package ir.smh.spatialbricks;

import ir.smh.spatialbricks.config.SparkConfig;
import ir.smh.spatialbricks.converttospatial.GeometryOptions;
import ir.smh.spatialbricks.converttospatial.GeometryReader;
import ir.smh.spatialbricks.converttospatial.udf.converttogeometry.geoJsonGeometricalAdapter;
import org.apache.sedona.spark.SedonaContext;
import org.apache.spark.sql.catalyst.analysis.NoSuchTableException;
import org.locationtech.jts.geom.Geometry;

public class Main {
    public static void main(String[] args) throws NoSuchTableException {

        var spark = SparkConfig.createSession("../datasets/newyork");
        SedonaContext.create(spark);

        GeometryOptions options = GeometryOptions.of("geohash");

        GeometryReader<?> adapter = new geoJsonGeometricalAdapter();

        SpatialETL etl = new SpatialETL(spark, options, adapter);

        TableSpec bronze = new TableSpec("bronzelayer", "FireStations", "");
        TableSpec silver = new TableSpec("silverlayer", "FireStations", "");

        etl.processFile(bronze, silver, "../datasets/newyork/raw-files/FireStations_ndjson.json");

        spark.stop();
    }
}



