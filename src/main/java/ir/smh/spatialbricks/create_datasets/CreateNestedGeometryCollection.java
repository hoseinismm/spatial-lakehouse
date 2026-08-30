package ir.smh.spatialbricks.create_datasets;

import ir.smh.spatialbricks.config.SparkConfig;
import ir.smh.spatialbricks.core.PipelineExecutor;
import ir.smh.spatialbricks.core.TableSpec;
import ir.smh.spatialbricks.encoder.converttogeometry.GeometryReader;
import ir.smh.spatialbricks.encoder.converttogeometry.WKBReaderAdapter;
import ir.smh.spatialbricks.udf.NFSP;
import ir.smh.spatialbricks.udf.WKB;
import ir.smh.spatialbricks.utilities.PowerPlanUtil;
import org.apache.sedona.spark.SedonaContext;
import org.apache.spark.sql.catalyst.analysis.NoSuchTableException;

import java.io.IOException;

public class CreateNestedGeometryCollection {

    public static void main(String[] args)
            throws NoSuchTableException, IOException, InterruptedException {

        PowerPlanUtil.setPowerPlan(PowerPlanUtil.SPARK_TEST);

        try {

            String folderpath = "../datasets/testgeometrycollection";

            var spark = SparkConfig.createSession(folderpath);

            try {

                spark.sparkContext().setLogLevel("ERROR");

                SedonaContext.create(spark);

                GeometryReader<?> wkbFile = new WKBReaderAdapter();

                PipelineExecutor NFSPWriting =
                        new PipelineExecutor(spark, wkbFile, new NFSP(spark));

                PipelineExecutor wkbWriting = new PipelineExecutor(spark,wkbFile , new WKB(spark));

                String path = "../datasets/testgeometrycollection/nfsp_geometry_collection_1000.parquet";

                TableSpec NFSPUnindexed =
                        new TableSpec("NFSPUnindexed", "testgeometrycollection", folderpath);

                TableSpec NFSPIndexed =
                        new TableSpec("NFSPIndexed", "testgeometrycollection", folderpath);

                TableSpec wkbUnindexed =
                        new TableSpec("wkbUnindexed", "aubuildings", folderpath);

                TableSpec wkbIndexed =
                        new TableSpec("wkbIndexed", "aubuildings", folderpath);

                long startTime = System.currentTimeMillis();

//                wkbWriting.AddDataWithIndexing(wkbIndexed, path, 10000L, 200L);

//                wkbWriting.AddDataWithoutIndexing(wkbUnindexed, path);

//                NFSPWriting.AddDataWithoutIndexing(NFSPUnindexed, path);

                NFSPWriting.AddDataWithIndexing(NFSPIndexed, path, 10000L, 200L);


                long duration = System.currentTimeMillis() - startTime;

                System.out.println("Time of writing : " + duration);

            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {

                System.out.println("Stopping Spark...");
//                spark.catalog().clearCache();

                System.in.read();

//
//                Thread.sleep(8000);

                spark.stop();
                System.out.println("Spark stopped.");
            }

        } finally {
            PowerPlanUtil.setPowerPlan(PowerPlanUtil.BALANCED);
        }
    }
}

