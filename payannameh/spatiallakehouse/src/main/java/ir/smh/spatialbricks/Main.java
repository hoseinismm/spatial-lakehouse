package ir.smh.spatialbricks;

import ir.smh.spatialbricks.config.SparkConfig;
import ir.smh.spatialbricks.converttospatial.GeometryOptions;
import ir.smh.spatialbricks.converttospatial.GeometryReader;
import ir.smh.spatialbricks.converttospatial.udf.UDFRegistry;
import ir.smh.spatialbricks.converttospatial.udf.converttogeometry.WKTReaderAdapter;
import org.apache.sedona.core.formatMapper.GeoJsonReader;
import org.apache.sedona.sql.utils.Adapter;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;


import static org.apache.spark.sql.functions.callUDF;

public class Main {
    public static void main(String[] args) {
        String warehousePath = new java.io.File("../dataset/newyork buildings and fire stations").getAbsolutePath().replace("\\", "/");
        var spark = SparkConfig.createSession(warehousePath);
        JavaSparkContext jsc = JavaSparkContext.fromSparkContext(spark.sparkContext());
        Dataset<Row> df = Adapter.toDf(
            GeoJsonReader.readToGeometryRDD(jsc,
                    "../dataset/newyork buildings and fire stations/raw-files/FireStations_-8963958910743951293.geojson"),
            spark
        );

        // تعریف دیتابیس و جدول
        String dbName = "bronze-layer";
        String tableName = "newyork-buildings";
        spark.sql("DROP TABLE IF EXISTS " + dbName + "." + tableName);
        spark.sql("CREATE DATABASE IF NOT EXISTS " + dbName);

        // ذخیره در جدول آیسبرگ
        df.writeTo(dbName + "." + tableName)
                .using("iceberg")
                .createOrReplace();

        System.out.println("Table was created successfully!");

        df.show();
        df.printSchema();

        GeometryOptions options = GeometryOptions.of("bbox", "center", "area", "startpoint","endpoint");

        GeometryReader adapter = new WKTReaderAdapter();

        UDFRegistry.registerAll(spark, options, adapter);

        Dataset<Row> df2 = spark.read().parquet("input/data.parquet");

        Dataset<Row> transformed = df.withColumn("geometry",
                callUDF("wktToRow", df.col("wkt")));

        transformed.write().mode("overwrite").parquet("output/spatial_parquet");

        spark.stop();
    }
}

