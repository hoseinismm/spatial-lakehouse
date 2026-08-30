package ir.smh.spatialbricks.create_datasets;

import ir.smh.spatialbricks.config.SparkConfig;
import ir.smh.spatialbricks.core.PipelineExecutor;
import ir.smh.spatialbricks.core.TableSpec;
import ir.smh.spatialbricks.encoder.converttogeometry.GeoJsonGeometricalAdapter;
import ir.smh.spatialbricks.encoder.converttogeometry.GeometryReader;
import ir.smh.spatialbricks.udf.GeoLake;
import ir.smh.spatialbricks.udf.NFSP;
import ir.smh.spatialbricks.udf.WKB;
import ir.smh.spatialbricks.utilities.PowerPlanUtil;
import org.apache.sedona.spark.SedonaContext;
import org.apache.spark.sql.catalyst.analysis.NoSuchTableException;

import java.io.IOException;

public class CreateGeometryCollection {

    public static void main(String[] args)
            throws NoSuchTableException, IOException, InterruptedException {

        PowerPlanUtil.setPowerPlan(PowerPlanUtil.SPARK_TEST);

        try {

            String folderpath = "../datasets/geometrycollection";

            var spark = SparkConfig.createSession(folderpath);

            try {

                spark.sparkContext().setLogLevel("ERROR");

                SedonaContext.create(spark);

                GeometryReader<?> geoJsonFile = new GeoJsonGeometricalAdapter();

                PipelineExecutor GeoLakeWriting =
                        new PipelineExecutor(spark, geoJsonFile, new GeoLake(spark));

                PipelineExecutor NFSPWriting =
                        new PipelineExecutor(spark, geoJsonFile, new NFSP(spark));

                PipelineExecutor wkbWriting =
                        new PipelineExecutor(spark, geoJsonFile, new WKB(spark));

                String path = "../datasets/geometrycollection/ndjson2.geojson";

                TableSpec GeoLakeUnindexed =
                        new TableSpec("GeoLakeUnindexed", "geometrycollection", folderpath);

                TableSpec GeoLakeIndexed =
                        new TableSpec("GeoLakeIndexed", "geometrycollection", folderpath);

                TableSpec NFSPUnindexed =
                        new TableSpec("NFSPUnindexed", "geometrycollection", folderpath);

                TableSpec NFSPIndexed =
                        new TableSpec("NFSPIndexed", "geometrycollection", folderpath);

                TableSpec wkbUnindexed =
                        new TableSpec("wkbUnindexed", "geometrycollection", folderpath);

                TableSpec wkbIndexed =
                        new TableSpec("wkbIndexed", "geometrycollection", folderpath);

                long startTime = System.currentTimeMillis();

//                wkbWriting.AddDataWithoutIndexing(wkbUnindexed, path);

                wkbWriting.AddDataWithIndexing(wkbIndexed, path, 150000L, 131072L);

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

