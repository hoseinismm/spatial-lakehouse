package ir.smh.spatialbricks;

import ir.smh.spatialbricks.converttospatial.GeometryOptions;
import ir.smh.spatialbricks.converttospatial.GeometryReader;
import ir.smh.spatialbricks.converttospatial.udf.UDFRegistry;
import  ir.smh.spatialbricks.createsql.IcebergTableCreator;
import org.apache.sedona.core.formatMapper.GeoJsonReader;
import org.apache.sedona.sql.utils.Adapter;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import static org.apache.spark.sql.functions.expr;
import org.apache.spark.sql.catalyst.analysis.NoSuchTableException;
import org.locationtech.jts.geom.Geometry;

import java.io.Serializable;

import static org.apache.spark.sql.functions.callUDF;

public class SpatialETL implements Serializable {

    private final SparkSession spark;
    private final GeometryOptions options;
    private final GeometryReader<Geometry> adapter;

    public SpatialETL(SparkSession spark, GeometryOptions options, GeometryReader<Geometry> adapter) {
        this.spark = spark;
        this.options = options;
        this.adapter = adapter;
    }

    public void processFile(TableSpec bronze, TableSpec silver, String inputPath) throws NoSuchTableException {


         JavaSparkContext jsc = JavaSparkContext.fromSparkContext(spark.sparkContext());

         Dataset<Row> df = Adapter.toDf(GeoJsonReader.readToGeometryRDD(jsc, inputPath),spark);

         Dataset<Row> dfGeoJson = df.withColumn("geometry_json", expr("ST_AsGeoJSON(geometry)")).drop("geometry");

        // ایجاد جدول برنزی
        IcebergTableCreator.createIcebergTableFromSchema(spark, dfGeoJson.schema(), bronze.database(), bronze.table());
        dfGeoJson.writeTo(bronze.database() + "." + bronze.table()).append();

        // ثبت UDF و تبدیل
        UDFRegistry.registerAll(spark, options, adapter);
        Dataset<Row> transformed = df.withColumn("spatiallakehouse", callUDF("stringToGeometry", df.col("geometry"))).drop("geometry");

        // ایجاد جدول نقره‌ای
        IcebergTableCreator.createIcebergTableFromSchema(spark, transformed.schema(), silver.database(), silver.table());
        transformed.writeTo(silver.database() + "." + silver.table()).append();
    }
}

