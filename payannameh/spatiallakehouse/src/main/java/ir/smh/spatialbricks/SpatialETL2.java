package ir.smh.spatialbricks;

import ir.smh.spatialbricks.converttospatial.GeometryOptions;
import ir.smh.spatialbricks.converttospatial.GeometryReader;
import ir.smh.spatialbricks.converttospatial.udf.GeohashToLongUdfRegistry;
import ir.smh.spatialbricks.converttospatial.udf.MinInBucketUdfRegistry;
import ir.smh.spatialbricks.converttospatial.udf.UDFRegistry;
import ir.smh.spatialbricks.createsql.IcebergTableCreator;
import ir.smh.spatialbricks.createsql.IcebergTableCreatorWithPartitioning;
import org.apache.sedona.core.formatMapper.GeoJsonReader;
import org.apache.sedona.sql.utils.Adapter;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.broadcast.Broadcast;
import org.apache.spark.sql.*;
import org.apache.spark.sql.api.java.UDF1;
import org.apache.spark.sql.catalyst.analysis.NoSuchTableException;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import scala.reflect.ClassTag$;

import static org.apache.spark.sql.functions.col;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.spark.sql.functions.*;

public class SpatialETL2 implements Serializable {

    private final SparkSession spark;
    private final GeometryOptions options;
    private final GeometryReader<?> adapter;
    private List<Double> splitList;

    public SpatialETL2(SparkSession spark, GeometryOptions options, GeometryReader<?> adapter) {
        this.spark = spark;
        this.options = options;
        this.adapter = adapter;
    }

    public void processFile(TableSpec bronze, TableSpec silver, String inputPath) throws NoSuchTableException {


        JavaSparkContext jsc = JavaSparkContext.fromSparkContext(spark.sparkContext());

        Dataset<Row> df = null;

        // ایجاد جدول برنزی

        if ((inputPath.toLowerCase().endsWith(".json"))||inputPath.toLowerCase().endsWith(".geojson")) {
            df = Adapter.toDf(GeoJsonReader.readToGeometryRDD(jsc, inputPath), spark);
            Dataset<Row> dfGeoJson = df.withColumn(
                    "geometry",
                    expr("ST_AsGeoJSON(geometry)")
            );
            //IcebergTableCreator.createIcebergTableFromSchema(spark, dfGeoJson.schema(), bronze.database(), bronze.table());
            dfGeoJson.writeTo(bronze.database() + "." + bronze.table()).append();

        } else if (inputPath.toLowerCase().endsWith(".parquet")) {
            df = spark.read().parquet(inputPath);
            //IcebergTableCreator.createIcebergTableFromSchema(spark, df.schema(), bronze.database(), bronze.table());
            df.writeTo(bronze.database() + "." + bronze.table()).append();

        } else if (inputPath.toLowerCase().endsWith(".csv")) {
            df = spark.read().csv(inputPath);
            df.writeTo(bronze.database() + "." + bronze.table()).append();

        } else {
            throw new IllegalArgumentException("Unsupported file format: " + inputPath);
        }

        // ثبت UDF و تبدیل
        UDFRegistry.registerAll(spark, options, adapter);

        // پیدا کردن نام واقعی ستون با ignore case
        String geomCol = Arrays.stream(df.columns())
                .filter(c -> c.equalsIgnoreCase("geometry"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No geometry column found"));

        Dataset<Row> transformed = df.withColumn(
                "geometry",
                callUDF("stringOrGeomToGeometry", df.col(geomCol))
        );


        transformed = transformed.filter(col("geometry").isNotNull());

        //long total = transformed.count();


        GeohashToLongUdfRegistry.registerAll(spark);



        MinInBucketUdfRegistry.registerAll(spark);

        transformed =transformed.withColumn(
                "geohash_numeric",
                functions.callUDF(
                        "geohashToLong",
                        transformed.col("geometry")
                )
        );
        Dataset<Row> silverTableDf = spark.table(silver.database() + "." + silver.table());

        Dataset<Row> metaDf = silverTableDf
                .groupBy(col("bucket_min"))
                .count()   // تعداد رکورد در هر bucket_min
                .withColumnRenamed("count", "record_count");
        metaDf.show();


        List<Long> bucketminList = metaDf.select("bucket_min").as(Encoders.LONG()).collectAsList();
        Collections.sort(bucketminList);
        //Broadcast<List<Long>> categoryIds = spark.sparkContext().broadcast(bucketminList, ClassTag$.MODULE$.apply(List.class));

        UDF1<Long, Long> nearestLower = (Long value) -> {
            Long result = bucketminList.get(0);
            for (Long x : bucketminList) {
                if (x <= value) {
                    result = x;
                } else {
                    break; // چون لیست مرتب است
                }
            }
            return result;
        };

        // ثبت UDF
        spark.udf().register("nearestLower", nearestLower, DataTypes.LongType);

        // اضافه کردن ستون جدید
        transformed = transformed.withColumn("bucket_min", functions.callUDF("nearestLower", col("geohash_numeric")));

        transformed.writeTo(silver.database() + "." + silver.table()).append();
        //spark.sql(String.format("ALTER TABLE %s.%s WRITE ORDERED BY (geometry.center.x ASC)", silver.database(), silver.table()
        //));

        metaDf = silverTableDf
                .groupBy(col("bucket_min"))
                .count()   // تعداد رکورد در هر bucket_min
                .withColumnRenamed("count", "record_count");
        metaDf.show();

        Dataset<Row> filteredDf = metaDf.filter(metaDf.col("record_count").gt(128));
        while (!filteredDf.isEmpty()) {

            silverTableDf = spark.table(silver.database() + "." + silver.table());

            for (Row partition : filteredDf.collectAsList()) {
                Long bucketMin = partition.getAs("bucket_min");


                Dataset<Row> partitionData = silverTableDf.filter(col("bucket_min").equalTo(bucketMin));

                System.out.println("split:" + bucketMin);


                // محاسبه میانه
                double[] quantiles = partitionData.stat().approxQuantile("geohash_numeric", new double[]{0.5}, 0.001);
                double median = quantiles[0];

                // رکوردهای نیمه پایین: bucket_min همان قبلی
                Dataset<Row> lowerHalf = partitionData.filter(partitionData.col("geohash_numeric").lt(median));

                // رکوردهای نیمه بالا: bucket_min = median
                Dataset<Row> upperHalf = partitionData.filter(partitionData.col("geohash_numeric").geq(median))
                        .withColumn("bucket_min", functions.lit(median));

                // **حذف پارتیشن قدیمی از جدول و جایگزینی با دو پارتیشن جدید**
                lowerHalf.writeTo(silver.database() + "." + silver.table())
                        .overwritePartitions();  // فقط پارتیشن bucketMin فعلی overwrite می‌شود

                upperHalf.writeTo(silver.database() + "." + silver.table())
                        .append();  // پارتیشن جدید با bucket_min = median اضافه می‌شود

            }
            metaDf = silverTableDf
                    .groupBy(col("bucket_min"))
                    .count()   // تعداد رکورد در هر bucket_min
                    .withColumnRenamed("count", "record_count");
            filteredDf = metaDf.filter(metaDf.col("record_count").gt(128));
        }



        //Dataset<Row> df2 = transformed.withColumn("center_x", col("geometry.center.x"));



        //Dataset<Row> df2 = transformed.withColumn("center_x", col("geometry.center.x"));






        // حالا dfWithBucket شامل ستون bucket_min است
        transformed.show();
    }
}



