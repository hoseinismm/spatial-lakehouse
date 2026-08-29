package ir.smh.spatialbricks.create_datasets;

import ir.smh.spatialbricks.config.SparkConfig;
import ir.smh.spatialbricks.core.PipelineExecutor;
import ir.smh.spatialbricks.core.TableSpec;
import ir.smh.spatialbricks.encoder.converttogeometry.GeometryReader;
import ir.smh.spatialbricks.encoder.converttogeometry.GeoJsonGeometricalAdapter;
import ir.smh.spatialbricks.udf.*;
import ir.smh.spatialbricks.utilities.PowerPlanUtil;
import org.apache.sedona.spark.SedonaContext;
import org.apache.spark.sql.catalyst.analysis.NoSuchTableException;

import java.io.IOException;

public class CreateAuBuildings {

    public static void main(String[] args)
            throws NoSuchTableException, IOException, InterruptedException {

        PowerPlanUtil.setPowerPlan(PowerPlanUtil.SPARK_TEST);

        try {

            String folderpath = "../datasets/aubuildings";

            var spark = SparkConfig.createSession(folderpath);

            try {

                spark.sparkContext().setLogLevel("ERROR");

                SedonaContext.create(spark);

                GeometryReader<?> geoJsonFile = new GeoJsonGeometricalAdapter();

                PipelineExecutor GeoLakeWriting =
                        new PipelineExecutor(spark, geoJsonFile, new GeoLake(spark));

                PipelineExecutor NFSPWriting =
                        new PipelineExecutor(spark, geoJsonFile, new NFSP(spark));

                PipelineExecutor spatialWriting =
                        new PipelineExecutor(spark, geoJsonFile, new SP(spark));

                PipelineExecutor flattenSpatialWriting =
                        new PipelineExecutor(spark, geoJsonFile, new FSP(spark));

                PipelineExecutor wkbWriting = new PipelineExecutor(spark, geoJsonFile, new WKB(spark));

                String path = "../datasets/aubuildings/AUBuildingsndjson.geojson";

                TableSpec GeoLakeUnindexed =
                        new TableSpec("GeoLakeUnindexed", "aubuildings", folderpath);

                TableSpec GeoLakeIndexed =
                        new TableSpec("GeoLakeIndexed", "aubuildings", folderpath);

                TableSpec NFSPUnindexed =
                        new TableSpec("NFSPUnindexed", "aubuildings", folderpath);

                TableSpec NFSPIndexed =
                        new TableSpec("NFSPIndexed", "aubuildings", folderpath);

                TableSpec silverIndexed =
                        new TableSpec("silverIndexed", "aubuildings", folderpath);

                TableSpec silverUnindexed =
                        new TableSpec("silverUnindexed", "aubuildings", folderpath);

                TableSpec flattenSilverUnindexed =
                        new TableSpec("flattenSilverUnindexed", "aubuildings", folderpath);

                TableSpec flattenSilverIndexed =
                        new TableSpec("flattenSilverIndexed", "aubuildings", folderpath);

                TableSpec wkbUnindexed =
                        new TableSpec("wkbUnindexed", "aubuildings", folderpath);

                TableSpec wkbIndexed =
                        new TableSpec("wkbIndexed", "aubuildings", folderpath);

                long startTime = System.currentTimeMillis();



//                wkbWriting.AddDataWithoutIndexing(wkbUnindexed, path);

//                wkbWriting.silverLayerWithBboxIndexing(wkbIndexed, path, 150000L, 131072L);

//                spatialWriting.AddDataWithoutIndexing(silverUnindexed, path);

//                spatialWriting.silverLayerWithBboxIndexing(silverIndexed, path, 150000L, 131072L);

//                flattenSpatialWriting.AddDataWithoutIndexing(flattenSilverUnindexed, path);

//                flattenSpatialWriting.silverLayerWithBboxIndexing(flattenSilverIndexed, path, 150000L, 131072L);

//                NFSPWriting.AddDataWithoutIndexing(NFSPUnindexed, path);

//                NFSPWriting.AddDataWithIndexing(NFSPIndexed, path, 150000L, 131072L);

//                GeoLakeWriting.AddDataWithoutIndexing(GeoLakeUnindexed, path);

//                GeoLakeWriting.AddDataWithIndexing(GeoLakeIndexed, path, 150000L, 131072L);

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

