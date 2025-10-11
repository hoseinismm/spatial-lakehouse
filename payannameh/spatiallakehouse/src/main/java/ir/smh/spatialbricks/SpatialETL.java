package ir.smh.spatialbricks;

import ir.smh.spatialbricks.converttospatial.GeometryOptions;
import ir.smh.spatialbricks.converttospatial.GeometryReader;
import ir.smh.spatialbricks.converttospatial.udf.UDFRegistry;
import ir.smh.spatialbricks.createsql.IcebergTableCreator;
import org.apache.sedona.core.formatMapper.GeoJsonReader;
import org.apache.sedona.sql.utils.Adapter;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.catalyst.analysis.NoSuchTableException;


import java.io.Serializable;

import static org.apache.spark.sql.functions.*;

public class SpatialETL implements Serializable {

    private final SparkSession spark;
    private final GeometryOptions options;
    private final GeometryReader<?> adapter;

    public SpatialETL(SparkSession spark, GeometryOptions options, GeometryReader<?> adapter) {
        this.spark = spark;
        this.options = options;
        this.adapter = adapter;
    }

    public void processFile(TableSpec bronze, TableSpec silver, String inputPath) throws NoSuchTableException {


        JavaSparkContext jsc = JavaSparkContext.fromSparkContext(spark.sparkContext());

        Dataset<Row> df = null;

        // ایجاد جدول برنزی

        if ((inputPath.endsWith(".json"))||inputPath.endsWith(".geojson")) {
            df = Adapter.toDf(GeoJsonReader.readToGeometryRDD(jsc, inputPath), spark);
            Dataset<Row> dfGeoJson = df.withColumn(
                    "geometry",
                    expr("ST_AsGeoJSON(geometry)")
            );

            IcebergTableCreator.createIcebergTableFromSchema(spark, dfGeoJson.schema(), bronze.database(), bronze.table());
            dfGeoJson.writeTo(bronze.database() + "." + bronze.table()).append();
        } else if (inputPath.endsWith(".parquet")) {
            df = spark.read().parquet(inputPath);
            IcebergTableCreator.createIcebergTableFromSchema(spark, df.schema(), bronze.database(), bronze.table());
            df.writeTo(bronze.database() + "." + bronze.table()).append();
        } else if (inputPath.endsWith(".csv")) {
            df = spark.read().csv(inputPath);
            df.writeTo(bronze.database() + "." + bronze.table()).append();
        } else {
            throw new IllegalArgumentException("Unsupported file format: " + inputPath);
        }

        // ثبت UDF و تبدیل
        UDFRegistry.registerAll(spark, options, adapter);
        Dataset<Row> transformed = df.withColumn("geometry", callUDF("stringOrGeomToGeometry", df.col("geometry")));

        // ایجاد جدول نقره‌ای
        IcebergTableCreator.createIcebergTableFromSchema(spark, transformed.schema(), silver.database(), silver.table());
        transformed = transformed.filter(col("geometry").isNotNull());
        transformed.writeTo(silver.database() + "." + silver.table()).append();
    }
}

