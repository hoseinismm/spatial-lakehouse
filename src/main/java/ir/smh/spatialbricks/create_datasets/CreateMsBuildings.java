package ir.smh.spatialbricks.create_datasets;

import ir.smh.spatialbricks.encoder.converttogeometry.GeoJsonGeometricalAdapter;
import ir.smh.spatialbricks.udf.*;
import ir.smh.spatialbricks.utilities.PowerPlanUtil;
import ir.smh.spatialbricks.core.PipelineExecutor;
import ir.smh.spatialbricks.core.TableSpec;
import ir.smh.spatialbricks.config.SparkConfigLocal;
import ir.smh.spatialbricks.encoder.converttogeometry.GeometryReader;
import org.apache.sedona.spark.SedonaContext;
import org.apache.spark.sql.catalyst.analysis.NoSuchTableException;

import java.io.IOException;

public class CreateMsBuildings {

    public static void main(String[] args)
            throws NoSuchTableException, IOException, InterruptedException {

        PowerPlanUtil.setPowerPlan(PowerPlanUtil.SPARK_TEST);

        try {

            String folderpath = "../datasets/msbuildings";

            var spark = SparkConfigLocal.createSession(folderpath);


            try {

                spark.sparkContext().setLogLevel("ERROR");

                SedonaContext.create(spark);

                GeometryReader<?> geoJsonFile = new GeoJsonGeometricalAdapter();

                PipelineExecutor wkbWriting =
                        new PipelineExecutor(spark, geoJsonFile, new WKB(spark));

                PipelineExecutor spatialWriting =
                        new PipelineExecutor(spark, geoJsonFile, new SP(spark));

                PipelineExecutor flattenSpatialWriting =
                        new PipelineExecutor(spark, geoJsonFile, new FSP(spark));

                PipelineExecutor NFSPWriting =
                        new PipelineExecutor(spark, geoJsonFile, new NFSP(spark));

                PipelineExecutor GeoLakeWriting =
                        new PipelineExecutor(spark, geoJsonFile, new GeoLake(spark));

                String path = "../datasets/msbuildings/MSBuildingsndjson.geojson";

                TableSpec wkbUnindexed =
                        new TableSpec("wkbUnindexed", "msbuildings", folderpath);

                TableSpec wkbIndexed =
                        new TableSpec("wkbIndexed", "msbuildings", folderpath);

                TableSpec silverIndexed =
                        new TableSpec("silverIndexed", "msbuildings", folderpath);

                TableSpec silverUnindexed =
                        new TableSpec("silverUnindexed", "msbuildings", folderpath);

                TableSpec flattenSilverUnindexed =
                        new TableSpec("flattenSilverUnindexed", "msbuildings", folderpath);

                TableSpec flattenSilverIndexed =
                        new TableSpec("flattenSilverIndexed", "msbuildings", folderpath);

                TableSpec NFSPUnindexed =
                        new TableSpec("NFSPUnindexed", "msbuildings", folderpath);

                TableSpec NFSPIndexed =
                        new TableSpec("NFSPIndexed", "msbuildings", folderpath);

                TableSpec GeoLakeUnindexed =
                        new TableSpec("GeoLakeUnindexed", "msbuildings", folderpath);

                TableSpec GeoLakeIndexed =
                        new TableSpec("GeoLakeIndexed", "msbuildings", folderpath);

                long startTime = System.currentTimeMillis();

//                wkbWriting.silverLayerWithoutBboxIndexing(wkbUnindexed, path);

//                wkbWriting.silverLayerWithBboxIndexing(wkbIndexed, path, 150000L, 1048576L);

//                spatialWriting.silverLayerWithoutBboxIndexing(silverUnindexed, path);

//                spatialWriting.AddDataWithIndexing(silverIndexed, path, 150000L, 1048576L);
//
//                flattenSpatialWriting.AddDataWithoutIndexing(flattenSilverUnindexed, path);

//                flattenSpatialWriting.AddDataWithIndexing(flattenSilverIndexed, path, 150000L, 1048576L);

//                NFSPWriting.AddDataWithoutIndexing(NFSPUnindexed, path);

//                NFSPWriting.AddDataWithIndexing(NFSPIndexed, path, 150000L, 1048576L);

//                GeoLakeWriting.AddDataWithoutIndexing(GeoLakeUnindexed, path);

                GeoLakeWriting.AddDataWithIndexing(GeoLakeIndexed, path, 150000L, 1048576L);

                long duration = System.currentTimeMillis() - startTime;

                System.out.println("Time of writing : " + duration);

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

