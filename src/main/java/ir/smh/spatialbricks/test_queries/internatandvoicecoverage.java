package ir.smh.spatialbricks.test_queries;

import ir.smh.spatialbricks.config.SparkConfigLocal;
import ir.smh.spatialbricks.udf.*;
import ir.smh.spatialbricks.utilities.PowerPlanUtil;
import ir.smh.spatialbricks.core.TableSpec;
import org.apache.sedona.spark.SedonaContext;
import org.apache.sedona.sql.utils.SedonaSQLRegistrator;
import org.apache.spark.sql.SparkSession;

import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class internatandvoicecoverage {

    private  static final SparkSession spark =  SparkConfigLocal.createSession("../datasets/internet_and_voice_coverage");

    private static final UDFRegistry<?, ?> wkbRegistry =
            new WKBIndexedParquet(spark);

    private static final UDFRegistry<?, ?> spatialRegistry =
            new SpatialParquet(spark);

    private static final UDFRegistry<?, ?> flattenRegistry =
            new FlattenSpatialParquet(spark);

    private static final UDFRegistry<?, ?> NFSPRegistry =
            new NFSP(spark);


    private static final TableSpec wkbUnindexed =
            new TableSpec("wkbUnindexed", "internet_and_voice_coverage", "");

    private static final TableSpec wkbIndexed =
            new TableSpec("wkbIndexed", "internet_and_voice_coverage", "");

    private static final TableSpec silverUnindexed =
            new TableSpec("silverUnindexed", "internet_and_voice_coverage", "");

    private static final TableSpec silverIndexed =
            new TableSpec("silverIndexed", "internet_and_voice_coverage", "");

    private static final TableSpec flattenSilverUnindexed =
            new TableSpec("flattenSilverUnindexed", "internet_and_voice_coverage", "");

    private static final TableSpec flattenSilverIndexed =
            new TableSpec("flattenSilverIndexed", "internet_and_voice_coverage", "");

    private static final TableSpec NFSPUnindexed =
            new TableSpec("NFSPUnindexed", "internet_and_voice_coverage", "");

    private static final TableSpec NFSPIndexed =
            new TableSpec("NFSPIndexed", "internet_and_voice_coverage", "");

    public static void main(String[] args) throws Exception {

        SedonaContext.create(spark);
        SedonaSQLRegistrator.registerAll(spark);

        try {

            PowerPlanUtil.setPowerPlan(PowerPlanUtil.SPARK_TEST);

            try {

                int runs = 10;

                long[][] results = runBenchmarks( runs );

                writeResults(results, runs);

            } finally {

                PowerPlanUtil.setPowerPlan(PowerPlanUtil.BALANCED);
            }

        } finally {
            spark.stop();
        }

    }

    private static long[][] runBenchmarks( int runs ) throws Exception {

        long[][] results = new long[12][runs];

        for (int i = 0; i < runs; i++) {

                System.out.println("Run " + (i + 1));


                results[0][i] =0;// testQuery(wkbUnindexed, wkbRegistry, false);
                spark.catalog().clearCache();
                System.gc();
                Thread.sleep(3000);
                results[1][i] =0;// testQuery(wkbIndexed, wkbRegistry, true);
                spark.catalog().clearCache();
                System.gc();
                Thread.sleep(3000);
                results[2][i] =0;// testQuery(silverUnindexed, spatialRegistry, false);
                spark.catalog().clearCache();
                System.gc();
                Thread.sleep(3000);
                results[3][i] =0;// testQuery(silverIndexed, spatialRegistry, true);
                spark.catalog().clearCache();
                System.gc();
                Thread.sleep(3000);
                results[4][i] =0;// testQuery(flattenSilverUnindexed, flattenRegistry, false);
                spark.catalog().clearCache();
                System.gc();
                Thread.sleep(3000);
                results[5][i] =0;// testQuery(flattenSilverIndexed, flattenRegistry, true);
                spark.catalog().clearCache();
                System.gc();
                results[6][i] = testQuery(NFSPUnindexed, NFSPRegistry, false);
                spark.catalog().clearCache();
                System.gc();
                results[7][i] = testQuery(NFSPIndexed, NFSPRegistry, true);
                spark.catalog().clearCache();
                System.gc();
                Thread.sleep(3000);
                results[8][i] =0;// testDecode(wkbUnindexed, wkbRegistry);
                spark.catalog().clearCache();
                System.gc();
                Thread.sleep(3000);
                results[9][i] =0;// testDecode(silverUnindexed, spatialRegistry);
                spark.catalog().clearCache();
                System.gc();
                Thread.sleep(3000);
                results[10][i] =0;// testDecode(flattenSilverUnindexed, flattenRegistry);
                spark.catalog().clearCache();
                System.gc();
                Thread.sleep(3000);
                results[11][i] = testDecode(NFSPUnindexed, NFSPRegistry);
                spark.catalog().clearCache();
                System.gc();
                Thread.sleep(3000);
        }

        return results;
    }

    private static void writeResults(long[][] results, int runs)
            throws FileNotFoundException {

        String[] names = {
                "WKB Unindexed",
                "WKB Indexed",
                "Spatial Unindexed",
                "Spatial Indexed",
                "Flatten Unindexed",
                "Flatten Indexed",
                "NFSP Unindexed",
                "NFSP Indexed",
                "WKB Unindexed",
                "Spatial Unindexed",
                "Flatten Unindexed",
                "NFSP Unindexed"

        };

        try (PrintWriter out = new PrintWriter("benchmarknfsp_internet_and_voice_coverage6.csv")) {

            out.print("Test");

            for (int i = 1; i <= runs; i++) {
                out.print(",Run" + i);
            }

            out.println();

            for (int t = 0; t < names.length; t++) {

                out.print(names[t]);

                for (int r = 0; r < runs; r++) {
                    out.print("," + results[t][r]);
                }

                out.println();
            }
        }
    }

    public static long testQuery(  TableSpec silver,UDFRegistry<?, ?> udfRegistry,boolean indexed)  {

        udfRegistry.registerDecode();

        String fullName = silver.database() + "." + silver.table();

        long start = System.currentTimeMillis();

        String bboxFilter = indexed
                ? """
                  AND (
                      geometry.bbox_partitioning.min_x < -159 AND
                      geometry.bbox_partitioning.max_x > -161 AND
                      geometry.bbox_partitioning.min_y < 61 AND
                      geometry.bbox_partitioning.max_y > 59
                  )
              """
                : "";

        String sql = """
            SELECT COUNT(*)
            FROM %s
            WHERE
                ST_XMin(decodeGeometry(geometry)) > -161
                AND ST_XMax(decodeGeometry(geometry)) < -159
                AND ST_YMin(decodeGeometry(geometry)) > 59
                AND ST_YMax(decodeGeometry(geometry)) < 61
                %s
            """.formatted(fullName, bboxFilter);

        spark.sql(sql).show(false);

        long duration = System.currentTimeMillis() - start;

        System.out.println("Querying from " + fullName + " = " + duration);

        return duration;
    }

    public static long testDecode( TableSpec silver, UDFRegistry<?,?> udfRegistry) {

        udfRegistry.registerDecode();

        String fullName = silver.database() + "." + silver.table();

        long start = System.currentTimeMillis();
        String sql = String.format("""
                SELECT SUM(ST_Area(decodeGeometry(geometry)))
                FROM %s
                """, fullName);
        spark.sql(sql).show(false);

        long duration = System.currentTimeMillis() - start;

        System.out.println("Iceberg"+fullName+" decode time = " + duration);

        return  duration;
    }

}

