package ir.smh.spatialbricks.test_queries;

import ir.smh.spatialbricks.udf.*;
import ir.smh.spatialbricks.utilities.PowerPlanUtil;
import ir.smh.spatialbricks.core.TableSpec;
import ir.smh.spatialbricks.config.SparkConfigLocal;
import org.apache.sedona.spark.SedonaContext;
import org.apache.sedona.sql.utils.SedonaSQLRegistrator;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;

import static org.apache.spark.sql.functions.expr;

public class msbuildings {


    private  static final SparkSession spark =  SparkConfigLocal.createSession("../datasets/msbuildings");

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
            new TableSpec("wkbUnindexed", "msbuildings", "");

    private static final TableSpec wkbIndexed =
            new TableSpec("wkbIndexed", "msbuildings", "");

    private static final TableSpec silverUnindexed =
            new TableSpec("silverUnindexed", "msbuildings", "");

    private static final TableSpec silverIndexed =
            new TableSpec("silverIndexed", "msbuildings", "");

    private static final TableSpec flattenSilverUnindexed =
            new TableSpec("flattenSilverUnindexed", "msbuildings", "");

    private static final TableSpec flattenSilverIndexed =
            new TableSpec("flattenSilverIndexed", "msbuildings", "");

    private static final TableSpec NFSPIndexed =
            new TableSpec("NFSPIndexed", "msbuildings", "");

    private static final TableSpec NFSPUnindexed =
            new TableSpec("NFSPUnindexed", "msbuildings", "");

    private static final TableSpec GeoLakeIndexed =
            new TableSpec("GeoLakeIndexed", "msbuildings", "");

    private static final TableSpec GeoLakeUnindexed =
            new TableSpec("GeoLakeUnindexed", "msbuildings", "");

    public static void main(String[] args) throws Exception {

        try {

            PowerPlanUtil.setPowerPlan(PowerPlanUtil.SPARK_TEST);
            final int runs = 2;

            try {

                SedonaContext.create(spark);
                SedonaSQLRegistrator.registerAll(spark);

                long[][] results = runBenchmarks( runs );

                writeResults(results, runs);

            }   finally {
                spark.stop();
            }

        } finally {
            PowerPlanUtil.setPowerPlan(PowerPlanUtil.BALANCED);
        }
    }

    private static long[][] runBenchmarks( int runs ) throws Exception {

        long[][] results = new long[15][runs];

        for (int i = 0; i < runs; i++) {

            System.out.println("Run " + (i + 1));

            results[0][i] = testQuery(wkbUnindexed,false,wkbRegistry);
            results[1][i] = testQuery( wkbIndexed, true, wkbRegistry);
            results[2][i] = testQuery( silverUnindexed,false,spatialRegistry);
            results[3][i] = testQuery( silverIndexed, true, spatialRegistry);
            results[4][i] = testQuery( flattenSilverUnindexed,false, flattenRegistry);
            results[5][i] = testQuery( flattenSilverIndexed, true, flattenRegistry);
            results[6][i] = testQuery( NFSPUnindexed,false, NFSPRegistry);
            results[7][i] = testQuery( NFSPIndexed, true, NFSPRegistry);
            results[8][i] = testQuery( GeoLakeUnindexed,false, GeoLakeRegistry);
            results[9][i] = testQuery( GeoLakeIndexed, true, GeoLakeRegistry);
            results[10][i] = testDecode( wkbUnindexed, wkbRegistry);
            results[11][i] = testDecode( silverUnindexed, spatialRegistry);
            results[12][i] = testDecode( flattenSilverUnindexed, flattenRegistry);
            results[13][i] = testDecode( NFSPUnindexed, NFSPRegistry);
            results[14][i] = testDecode( GeoLakeUnindexed, GeoLakeRegistry);

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
                "GeoLake Indexed",
                "WKB Unindexed",
                "Spatial Unindexed",
                "Flatten Unindexed",
                "NFSP Unindexed",
                "GeoLake Unindexed"

        };

        try (PrintWriter out = new PrintWriter("../datasets/msbuildings/benchmark2_msbuildings.csv")) {

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
            TableSpec table,
            boolean indexed, UDFRegistry<?,?> udfRegistry) throws IOException {

        udfRegistry.registerDecode();

        String bboxFilter = indexed
                ? """
                 WHERE geometry.bbox_partitioning.min_x < -73.70
                       AND geometry.bbox_partitioning.min_y < 40.91
                       AND geometry.bbox_partitioning.max_x > -74.26
                       AND geometry.bbox_partitioning.max_y > 40.47
              """
                : "";

        String sql = """
        SELECT
        SUM(ST_AreaSpheroid(geom)) AS total_area
                FROM (
                    SELECT
                        decodeGeometry(geometry) AS geom
                     FROM  %s
        %s
                ) t
                WHERE ST_Contains(
                    ST_PolygonFromEnvelope(
                        -74.26,
                        40.47,
                        -73.70,
                        40.91
                    ),
                    geom
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


    public static long testDecode(TableSpec table, UDFRegistry<?,?> udfregistry) throws Exception {

        String fullName= table.database() + "." + table.table();

        udfregistry.registerDecode();

        Dataset<Row> t = spark.read()
                .format("iceberg")
                .load(fullName).withColumn(
                        "geom",
                        expr("decodeGeometry(geometry)")
                );

        long start = System.currentTimeMillis();

        t.selectExpr(
                        "ST_Area(geom) as area"
                )
                .agg(expr("avg(area)"))
                .show();

        long duration = System.currentTimeMillis() - start;

        System.out.println(
                fullName+ " decode time = "
                        + duration);

        return duration;
    }
}


