package ir.smh.spatialbricks.test_queries;


import ir.smh.spatialbricks.config.SparkConfig;
import ir.smh.spatialbricks.config.SparkConfigLocal;
import ir.smh.spatialbricks.decoder.FlattenSpatialParquetDecoder4;
import ir.smh.spatialbricks.udf.*;
import ir.smh.spatialbricks.utilities.PowerPlanUtil;
import ir.smh.spatialbricks.core.TableSpec;
import org.apache.sedona.spark.SedonaContext;
import org.apache.sedona.sql.utils.SedonaSQLRegistrator;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.TaskContext;
import org.apache.spark.sql.Dataset;
import org.apache.spark.api.java.function.MapPartitionsFunction;
import org.apache.spark.sql.Encoders;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.spark.sql.Row;

import java.util.Collections;
import java.util.List;

import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class internatandvoicecoverage {

    private  static final SparkSession spark =  SparkConfig.createSession("../datasets/internet_and_voice_coverage");


    private static final UDFRegistry<?, ?> wkbRegistry =
            new WKB(spark);

    private static final UDFRegistry<?, ?> spatialRegistry =
            new SP(spark);

    private static final UDFRegistry<?, ?> flattenRegistry =
            new FSP(spark);

    private static final UDFRegistry<?, ?> NFSPRegistry =
            new NFSP(spark);

    private static final UDFRegistry<?, ?> GeoLakeRegistry =
            new GeoLake(spark);


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

    private static final TableSpec GeoLakeUnindexed =
            new TableSpec("GeoLakeUnindexed", "internet_and_voice_coverage", "");

    private static final TableSpec GeoLakeIndexed =
            new TableSpec("GeoLakeIndexed", "internet_and_voice_coverage", "");

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

        long[][] results = new long[15][runs];

        for (int i = 0; i < runs; i++) {

                System.out.println("Run " + (i + 1));


                results[0][i] = testQuery(wkbUnindexed, wkbRegistry, false);

                results[1][i] =0;// testQuery(wkbIndexed, wkbRegistry, true);
//
                results[2][i] =0;// testQuery(silverUnindexed, spatialRegistry, false);
//
                results[3][i] =0;// testQuery(silverIndexed, spatialRegistry, true);
//
                results[4][i] =0;// testQuery(flattenSilverUnindexed, flattenRegistry, false);

                results[5][i] =0;// testQuery(flattenSilverIndexed, flattenRegistry, true);
//
                results[6][i] =0;// testQuery(NFSPUnindexed, NFSPRegistry, false);
//
                results[7][i] =0;// testQuery(NFSPIndexed, NFSPRegistry, true);
//
                results[8][i] =0;//  testQuery(GeoLakeUnindexed, GeoLakeRegistry, false);
//
                results[9][i] =0;// testQuery(GeoLakeIndexed, GeoLakeRegistry, true);
//
                results[10][i] = testDecode(wkbUnindexed, wkbRegistry);

                results[11][i] =0;// testDecode(silverUnindexed, spatialRegistry);

                results[12][i] =0;// testDecode(flattenSilverUnindexed, flattenRegistry);
//
                results[13][i] =0;// testDecode(NFSPUnindexed, NFSPRegistry);

                results[14][i] =0;// testDecode(GeoLakeUnindexed, GeoLakeRegistry);
//

//

                
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
                "GeoLake Unindexed",
                "GeoLakeIndexed",
                "WKB Unindexed",
                "Spatial Unindexed",
                "Flatten Unindexed",
                "NFSP Unindexed",
                "GeoLake Unindexed",

        };

        try (PrintWriter out = new PrintWriter("benchmark_FSP15_internet_and_voice_coverage6.csv")) {

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
    WITH decoded AS (
        SELECT
            decodeGeometry(geometry) AS geom
        FROM %s
        WHERE 1 = 1
            %s
    )
    SELECT COUNT(*)
    FROM decoded
    WHERE
        ST_XMin(geom) > -161
    
    """.formatted(fullName, bboxFilter);

        spark.sql(sql).show(false);

        long duration = System.currentTimeMillis() - start;

        System.out.println("Querying from " + fullName + " = " + duration);

        return duration;
    }
    public static void diagnoseDecode(
            TableSpec silver,
            boolean indexed) {

        String fullName =
                silver.database() + "." + silver.table();

        String bboxFilter =
                indexed
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
    SELECT geometry
    FROM %s
    WHERE 1 = 1
        %s
    """.formatted(
                fullName,
                bboxFilter
        );

        Dataset<Row> df =
                spark.sql(sql);

        Dataset<String> debug =
                df.mapPartitions(
                        (MapPartitionsFunction<Row, String>) iterator -> {

                            // =====================================================
                            // TASK INFORMATION
                            // =====================================================

                            org.apache.spark.TaskContext taskContext =
                                    org.apache.spark.TaskContext.get();

                            int partitionId =
                                    taskContext.partitionId();

                            long taskAttemptId =
                                    taskContext.taskAttemptId();


                            // =====================================================
                            // INPUT METRICS
                            // =====================================================

                            long bytesReadBefore = 0;

                            if (taskContext.taskMetrics() != null &&
                                    taskContext.taskMetrics().inputMetrics() != null) {

                                bytesReadBefore =
                                        taskContext
                                                .taskMetrics()
                                                .inputMetrics()
                                                .bytesRead();
                            }


                            // =====================================================
                            // GEOMETRY STATISTICS
                            // =====================================================

                            long rows = 0;

                            long vertices = 0;
                            long parts = 0;

                            long multiPolygons = 0;
                            long multiPolygonVertices = 0;
                            long multiPolygonParts = 0;

                            long maxVertices = 0;
                            long maxParts = 0;


                            // =====================================================
                            // PROCESS PARTITION
                            // =====================================================

                            while (iterator.hasNext()) {

                                Row row =
                                        iterator.next();

                                rows++;

                                if (row == null ||
                                        row.isNullAt(0)) {

                                    continue;
                                }

                                Row geometry =
                                        row.getStruct(0);

                                if (geometry == null) {
                                    continue;
                                }

                                int type =
                                        geometry.getInt(0);

                                List<Double> x =
                                        geometry.getList(1);

                                List<Integer> p =
                                        geometry.getList(3);


                                int vertexCount =
                                        x == null
                                                ? 0
                                                : x.size();

                                int partsCount =
                                        p == null
                                                ? 0
                                                : p.size();


                                // =================================================
                                // TOTALS
                                // =================================================

                                vertices += vertexCount;
                                parts += partsCount;


                                // =================================================
                                // MAXIMUM SINGLE GEOMETRY
                                // =================================================

                                maxVertices =
                                        Math.max(
                                                maxVertices,
                                                vertexCount
                                        );

                                maxParts =
                                        Math.max(
                                                maxParts,
                                                partsCount
                                        );


                                // =================================================
                                // MULTIPOLYGON
                                // =================================================

                                if (type == 6) {

                                    multiPolygons++;

                                    multiPolygonVertices +=
                                            vertexCount;

                                    multiPolygonParts +=
                                            partsCount;
                                }
                            }


                            // =====================================================
                            // INPUT METRICS AFTER READING
                            // =====================================================

                            long bytesRead = 0;
                            long recordsRead = 0;

                            if (taskContext.taskMetrics() != null &&
                                    taskContext.taskMetrics().inputMetrics() != null) {

                                bytesRead =
                                        taskContext
                                                .taskMetrics()
                                                .inputMetrics()
                                                .bytesRead();

                                recordsRead =
                                        taskContext
                                                .taskMetrics()
                                                .inputMetrics()
                                                .recordsRead();
                            }


                            // =====================================================
                            // RESULT
                            // =====================================================

                            String result =
                                    "TASK"
                                            + " | partition=" + partitionId
                                            + " | attempt=" + taskAttemptId

                                            + " | bytesRead="
                                            + bytesRead
                                            + " (" + formatMB(bytesRead) + ")"

                                            + " | recordsRead="
                                            + recordsRead

                                            + " | rows="
                                            + rows

                                            + " | vertices="
                                            + vertices

                                            + " | parts="
                                            + parts

                                            + " | multiPolygons="
                                            + multiPolygons

                                            + " | multiPolygonVertices="
                                            + multiPolygonVertices

                                            + " | multiPolygonParts="
                                            + multiPolygonParts

                                            + " | maxVertices="
                                            + maxVertices

                                            + " | maxParts="
                                            + maxParts;


                            System.out.println(result);


                            return Collections.singletonList(
                                    result
                            ).iterator();
                        },
                        Encoders.STRING()
                );


        // =========================================================
        // EXECUTE
        // =========================================================

        List<String> stats =
                debug.collectAsList();


        // =========================================================
        // PRINT FINAL STATISTICS
        // =========================================================

        System.out.println(
                "\n========== PARTITION STATISTICS =========="
        );

        for (String stat : stats) {

            System.out.println(stat);
        }

        System.out.println(
                "==========================================\n"
        );
    }


// =============================================================
// FORMAT BYTES
// =============================================================

    private static String formatMB(long bytes) {

        return String.format(
                "%.2f MB",
                bytes / 1024.0 / 1024.0
        );
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

