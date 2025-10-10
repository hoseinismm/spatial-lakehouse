package ir.smh.spatialbricks;

import ir.smh.spatialbricks.config.SparkConfig;
import ir.smh.spatialbricks.converttospatial.GeometryOptions;
import ir.smh.spatialbricks.converttospatial.GeometryReader;
import ir.smh.spatialbricks.converttospatial.udf.UDFRegistry;
import ir.smh.spatialbricks.converttospatial.udf.converttogeometry.geoJsonGeometricalAdapter;
import  ir.smh.spatialbricks.createsql.IcebergTableCreator;
import org.apache.sedona.core.formatMapper.GeoJsonReader;
import org.apache.sedona.sql.utils.Adapter;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.sedona.spark.SedonaContext;
import static org.apache.spark.sql.functions.expr;
import org.apache.spark.sql.catalyst.analysis.NoSuchTableException;
import org.locationtech.jts.geom.Geometry;


import static org.apache.spark.sql.functions.callUDF;

public class Main {
    public static void main(String[] args) throws NoSuchTableException {
        String warehousePath = new java.io.File("../datasets/newyork").getAbsolutePath().replace("\\", "/");
        var spark = SparkConfig.createSession(warehousePath);
        JavaSparkContext jsc = JavaSparkContext.fromSparkContext(spark.sparkContext());
        SedonaContext.create(spark);

        Dataset<Row> df = Adapter.toDf(
            GeoJsonReader.readToGeometryRDD(jsc,
                    "../datasets/newyork/raw-files/FireStations_ndjson.json"),
            spark
        );


        // تبدیل Geometry به GeoJSON (String) برای Iceberg
        Dataset<Row> df_geojson = df.withColumn("geometry_json", expr("ST_AsGeoJSON(geometry)"))
                .drop("geometry");


        // تعریف دیتابیس و جدول
        String dbName_b = "bronzelayer";
        String tableName_b = "FireStations";
        spark.sql("CREATE DATABASE IF NOT EXISTS bronzelayer");
        spark.sql("CREATE DATABASE IF NOT EXISTS silverlayer");

        IcebergTableCreator.createIcebergTableFromSchema(spark,df_geojson.schema(),dbName_b,tableName_b);

        // ذخیره در جدول آیسبرگ
        df_geojson.writeTo( dbName_b + "." + tableName_b)
                .append();

        System.out.println("Table was created successfully!");

        df_geojson.show();
        df_geojson.printSchema();

        GeometryOptions options = GeometryOptions.of( "geohash");

        GeometryReader<Geometry> adapter = new geoJsonGeometricalAdapter();

        UDFRegistry.registerAll(spark, options, adapter);

        Dataset<Row> transformed = df.withColumn("spatiallakehouse",
                callUDF("stringToGeometry", df.col("geometry")));

        transformed = transformed
                .drop("geometry");

        transformed.printSchema();

        String dbName_s = "silverlayer";
        String tableName_s = "FireStations";

        IcebergTableCreator.createIcebergTableFromSchema(spark,transformed.schema(),dbName_s,tableName_s);

        transformed.writeTo("silverlayer.FireStations").append();

        System.out.println("Table was created successfully!");

        spark.stop();
    }
}

