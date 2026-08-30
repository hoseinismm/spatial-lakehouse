package ir.smh.spatialbricks.test_queries;

import ir.smh.spatialbricks.config.SparkConfigLocal;
import ir.smh.spatialbricks.core.TableSpec;
import ir.smh.spatialbricks.udf.GeoLake;
import ir.smh.spatialbricks.udf.NFSP;
import ir.smh.spatialbricks.udf.UDFRegistry;
import ir.smh.spatialbricks.udf.WKB;
import ir.smh.spatialbricks.utilities.PowerPlanUtil;
import org.apache.sedona.spark.SedonaContext;
import org.apache.sedona.sql.utils.SedonaSQLRegistrator;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;

import static org.apache.spark.sql.functions.expr;

public class GeometryCollection {

    public static void main(String[] args) throws Exception {

        PowerPlanUtil.setPowerPlan(PowerPlanUtil.SPARK_TEST);

        try {

            final int runs = 10;

            SparkSession spark = createSpark();

            try {

                TableSpec wkbUnindexed = new TableSpec("wkbUnindexed", "geometrycollection", "");
                TableSpec wkbIndexed = new TableSpec("wkbIndexed", "geometrycollection", "");
                TableSpec NFSPUnindexed = new TableSpec("NFSPUnindexed", "geometrycollection", "");
                TableSpec NFSPIndexed = new TableSpec("NFSPIndexed", "geometrycollection", "");
                TableSpec GeoLakeUnindexed = new TableSpec("GeoLakeUnindexed", "geometrycollection", "");
                TableSpec GeoLakeIndexed = new TableSpec("GeoLakeIndexed", "geometrycollection", "");

                long[][] results = runBenchmarks(
                        spark,
                        runs,
                        wkbUnindexed,
                        wkbIndexed,
                        NFSPUnindexed,
                        NFSPIndexed,
                        GeoLakeUnindexed,
                        GeoLakeIndexed
                );

            writeResults(results, runs);

            }   finally {
                spark.stop();
            }

        } finally {
            PowerPlanUtil.setPowerPlan(PowerPlanUtil.BALANCED);
        }
    }

    private static SparkSession createSpark() {

        SparkSession spark =
                SparkConfigLocal.createSession("../datasets/geometrycollection");

        SedonaContext.create(spark);
        SedonaSQLRegistrator.registerAll(spark);

        return spark;
    }

    private static long[][] runBenchmarks(
            SparkSession spark,
            int runs,
            TableSpec wkbUnindexed,
            TableSpec wkbIndexed,
            TableSpec NFSPUnindexed,
            TableSpec NFSPIndexed,
            TableSpec GeoLakeUnindexed,
            TableSpec GeoLakeIndexed
            ) throws Exception {

        long[][] results = new long[9][runs];

        for (int i = 0; i < runs; i++) {

            System.out.println("Run " + (i + 1));

            results[0][i] = 0;//testQuery(spark, wkbUnindexed ,false,new WKBIndexedParquet(spark));
            results[1][i] = 0;//testQuery(spark, wkbIndexed,true,new WKBIndexedParquet(spark));
            results[2][i] = 0;//testQuery(spark, NFSPUnindexed,false, new NFSP(spark) );
            results[3][i] = 0;//testQuery(spark, NFSPIndexed, true, new NFSP(spark) );
            results[4][i] = 0;//testQuery(spark, GeoLakeUnindexed,false, new NFSP(spark) );
            results[5][i] = 0;//testQuery(spark, GeoLakeIndexed, true, new NFSP(spark) );
            results[6][i] = testDecode(spark, wkbUnindexed, new WKB(spark));
            results[7][i] = testDecode(spark, NFSPUnindexed, new NFSP(spark));
            results[8][i] = testDecode(spark, GeoLakeUnindexed, new GeoLake(spark));
        }

        return results;
    }

    private static void writeResults(long[][] results, int runs)
            throws FileNotFoundException {

        String[] names = {
                "WKB Unindexed",
                "WKB Indexed",
                "NFSP Unindexed",
                "NFSP Indexed",
                "GeoLake Unindexed",
                "GeoLake Indexed",
                "WKB Unindexed",
                "NFSP Unindexed",
                "GeoLake Unindexed"
        };

        try (PrintWriter out = new PrintWriter("benchmark_GeometryCollection2.csv")) {

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

    private static long testQuery(

            SparkSession spark,
            TableSpec table,
            boolean indexed, UDFRegistry udfRegistry) throws IOException {

        udfRegistry.registerDecode();


        String bboxFilter = indexed
                ? """
                 WHERE geometry.bbox_partitioning.max_x > 0
                       AND geometry.bbox_partitioning.max_y > 0
              """
                : "";

        String sql = """
                SELECT COUNT(*) AS count
                FROM (
                    SELECT
                        decodeGeometry(geometry) AS geom
                     FROM  %s
        %s
                ) t
                WHERE ST_Within(
                    geom,
                    ST_PolygonFromEnvelope(0, 0, 180, 90)
                );
        
        """.formatted(
                table.database() + "." + table.table(),
                bboxFilter
        );

        long t1 = System.currentTimeMillis();

        spark.sql(sql).show(false);

        long duration = System.currentTimeMillis() - t1;

        System.out.println("Querying from iceberg table time " + duration);

        return duration;
    }


    public static long testDecode(
            SparkSession spark,
            TableSpec table,
            UDFRegistry udfregistry
    ) throws Exception {

        String fullName =
                table.database() + "." + table.table();

        udfregistry.registerDecode();

        Dataset<Row> t = spark.read()
                .format("iceberg")
                .load(fullName);

        // --------------------------------------------------------
        // Warm-up
        // --------------------------------------------------------
        // یک اجرای اولیه برای کاهش اثر initialization / JIT
        // --------------------------------------------------------


        // --------------------------------------------------------
        // Benchmark
        // --------------------------------------------------------

        int iterations = 1;

        long start =
                System.currentTimeMillis();

        for (int i = 0; i < iterations; i++) {

            Dataset<Row> result =
                    t.withColumn(
                                    "geom",
                                    expr("decodeGeometry(geometry)")
                            )
                            .selectExpr(
                                    "ST_Area(ST_Envelope(geom)) AS envelope_area"
                            )
                            .agg(
                                    expr("avg(envelope_area)")
                            );

            // مهم:
            // action باید در هر iteration اجرا شود
            result.show(false);
        }

        long duration =
                System.currentTimeMillis() - start;

        double average =
                (double) duration / iterations;

        System.out.println(
                "=========================================="
        );

        System.out.println(
                fullName +
                        " decode benchmark"
        );

        System.out.println(
                "Iterations = " +
                        iterations
        );

        System.out.println(
                "Total time = " +
                        duration +
                        " ms"
        );

        System.out.println(
                "Average time = " +
                        String.format(
                                "%.2f",
                                average
                        ) +
                        " ms"
        );

        System.out.println(
                "=========================================="
        );

        return duration;
    }

}


