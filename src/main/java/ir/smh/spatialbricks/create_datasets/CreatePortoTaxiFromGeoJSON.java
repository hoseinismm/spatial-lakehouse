package ir.smh.spatialbricks.create_datasets;

import ir.smh.spatialbricks.encoder.converttogeometry.GeoJsonGeometricalAdapter;
import ir.smh.spatialbricks.udf.GeoLake;
import ir.smh.spatialbricks.udf.NFSP;
import ir.smh.spatialbricks.udf.WKB;
import ir.smh.spatialbricks.utilities.PowerPlanUtil;
import ir.smh.spatialbricks.core.PipelineExecutor;
import ir.smh.spatialbricks.core.TableSpec;
import ir.smh.spatialbricks.config.SparkConfigLocal;
import ir.smh.spatialbricks.encoder.converttogeometry.GeometryReader;
import ir.smh.spatialbricks.udf.FSP;

import org.apache.sedona.spark.SedonaContext;
import org.apache.spark.sql.catalyst.analysis.NoSuchTableException;

import java.io.IOException;


public class CreatePortoTaxiFromGeoJSON {

    public static void main(String[] args) throws NoSuchTableException, IOException, InterruptedException {
        PowerPlanUtil.setPowerPlan(PowerPlanUtil.SPARK_TEST);

        try {

            String folderpath = "../datasets/portotaxi2";

        var spark = SparkConfigLocal.createSession(folderpath);
            try {

        spark.sparkContext().setLogLevel("ERROR");

        SedonaContext.create(spark);

        GeometryReader<?>  geoJsonFile= new GeoJsonGeometricalAdapter();

        PipelineExecutor wkbSpatialWriting = new PipelineExecutor(spark,geoJsonFile, new WKB(spark) );

        PipelineExecutor spatialWriting = new PipelineExecutor(spark,geoJsonFile );

        PipelineExecutor flattenSpatialWriting = new PipelineExecutor(spark,geoJsonFile, new FSP(spark));

        PipelineExecutor NFSPWriting = new PipelineExecutor(spark, geoJsonFile, new NFSP(spark));

        PipelineExecutor GeoLakeWriting = new PipelineExecutor(spark, geoJsonFile, new GeoLake(spark));

        TableSpec wkbUnindexed = new TableSpec("wkbUnindexed", "portotaxi", folderpath);

        TableSpec wkbIndexed = new TableSpec("wkbIndexed", "portotaxi", folderpath);

        TableSpec silverUnindexed = new TableSpec("silverUnindexed", "portotaxi", folderpath);

        TableSpec silverIndexed = new TableSpec("silverIndexed", "portotaxi", folderpath);

        TableSpec flattenSilverUnindexed = new TableSpec("flattenSilverUnindexed", "portotaxi", folderpath);

        TableSpec flattenSilverIndexed = new TableSpec("flattenSilverIndexed", "portotaxi", folderpath);

        TableSpec NFSPUnindexed = new TableSpec("NFSPUnindexed", "portotaxi", folderpath);

        TableSpec NFSPIndexed =  new TableSpec("NFSPIndexed", "portotaxi", folderpath);

        TableSpec GeoLakeUnindexed = new TableSpec("GeoLakeUnindexed", "portotaxi", folderpath);

        TableSpec GeoLakeIndexed =  new TableSpec("GeoLakeIndexed", "portotaxi", folderpath);

        long start = System.currentTimeMillis();

            String path ="../datasets/portotaxi2/portotaxindjson.geojson";

//          wkbSpatialWriting.silverLayerWithoutBboxIndexing(wkbUnindexed, path );

//          wkbSpatialWriting.silverLayerWithBboxIndexing(wkbIndexed,path, 150000L, 131072L);

//          spatialWriting.AddDataWithoutIndexing(silverUnindexed, path );

//          spatialWriting.AddDataWithIndexing(silverIndexed,path, 150000L, 131072L);

//          flattenSpatialWriting.AddDataWithoutIndexing(flattenSilverUnindexed, path );

//          flattenSpatialWriting.AddDataWithIndexing(flattenSilverIndexed,path, 150000L, 131072L);

//          NFSPWriting.AddDataWithoutIndexing(NFSPUnindexed, path );

//          NFSPWriting.AddDataWithIndexing(NFSPIndexed,path, 150000L, 131072L);

            GeoLakeWriting.AddDataWithoutIndexing(GeoLakeUnindexed, path );

//          GeoLakeWriting.AddDataWithIndexing(GeoLakeIndexed,path, 150000L, 131072L);

        long duration = System.currentTimeMillis() - start;

        System.out.println("Time of writing :  = " + duration);

            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                spark.stop();
            }

        } finally {
            PowerPlanUtil.setPowerPlan(PowerPlanUtil.BALANCED);
        }

        Thread.sleep(3000);
    }
}