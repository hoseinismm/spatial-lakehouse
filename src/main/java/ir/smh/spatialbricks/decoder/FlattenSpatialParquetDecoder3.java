package ir.smh.spatialbricks.decoder;

import org.apache.spark.sql.Row;
import org.locationtech.jts.geom.*;

import java.util.ArrayList;
import java.util.List;

public class FlattenSpatialParquetDecoder3 {

    private static final GeometryFactory GF =
            new GeometryFactory();

    // =========================================================
    // ENTRY POINT
    // =========================================================

    public static Geometry geometryToJTS(Row row) {

        if (row == null) {
            return null;
        }

        int type = row.getInt(0);

        double[] x =
                toDoubleArray(row.getList(1));

        double[] y =
                toDoubleArray(row.getList(2));

        int[] parts =
                toIntArray(row.getList(3));

        CoordinateSequenceFactory csf =
                GF.getCoordinateSequenceFactory();

        return switch (type) {

            case 1 -> decodePoint(x, y, csf);

            case 2 -> decodeLineString(x, y, csf);

            case 3 -> decodePolygon(
                    parts,
                    x,
                    y,
                    csf
            );

            case 4 -> decodeMultiPoint(
                    x,
                    y,
                    csf
            );

            case 5 -> decodeMultiLineString(
                    parts,
                    x,
                    y,
                    csf
            );

            case 6 -> decodeMultiPolygon(
                    parts,
                    x,
                    y,
                    csf
            );

            default -> null;
        };
    }


    // =========================================================
    // POINT
    // =========================================================

    private static Point decodePoint(
            double[] x,
            double[] y,
            CoordinateSequenceFactory csf) {

        if (x.length == 0) {
            return GF.createPoint();
        }

        CoordinateSequence seq =
                csf.create(1, 2);

        seq.setOrdinate(0, 0, x[0]);
        seq.setOrdinate(0, 1, y[0]);

        return GF.createPoint(seq);
    }


    // =========================================================
    // LINESTRING
    // =========================================================

    private static LineString decodeLineString(
            double[] x,
            double[] y,
            CoordinateSequenceFactory csf) {

        if (x.length == 0) {
            return GF.createLineString();
        }

        CoordinateSequence seq =
                createSequence(
                        csf,
                        x,
                        y,
                        0,
                        x.length
                );

        return GF.createLineString(seq);
    }


    // =========================================================
    // MULTIPOINT
    // =========================================================

    private static MultiPoint decodeMultiPoint(
            double[] x,
            double[] y,
            CoordinateSequenceFactory csf) {

        int size = x.length;

        if (size == 0) {
            return GF.createMultiPoint();
        }

        List<Point> points =
                new ArrayList<>(size);

        for (int i = 0; i < size; i++) {

            CoordinateSequence seq =
                    csf.create(1, 2);

            seq.setOrdinate(
                    0,
                    0,
                    x[i]
            );

            seq.setOrdinate(
                    0,
                    1,
                    y[i]
            );

            points.add(
                    GF.createPoint(seq)
            );
        }

        /*
         * فقط اینجا List -> Array
         */
        return GF.createMultiPoint(
                points.toArray(new Point[0])
        );
    }


    // =========================================================
    // MULTILINESTRING
    // =========================================================

    private static MultiLineString decodeMultiLineString(
            int[] parts,
            double[] x,
            double[] y,
            CoordinateSequenceFactory csf) {

        int count = parts.length;

        if (count == 0) {
            return GF.createMultiLineString();
        }

        List<LineString> lines =
                new ArrayList<>(count);

        for (int i = 0; i < count; i++) {

            int start =
                    Math.abs(parts[i]);

            int end =
                    (i + 1 < count)
                            ? Math.abs(parts[i + 1])
                            : x.length;

            CoordinateSequence seq =
                    createSequence(
                            csf,
                            x,
                            y,
                            start,
                            end
                    );

            lines.add(
                    GF.createLineString(seq)
            );
        }

        /*
         * فقط هنگام ساخت Geometry
         */
        return GF.createMultiLineString(
                lines.toArray(new LineString[0])
        );
    }


    // =========================================================
    // POLYGON
    // =========================================================

    private static Polygon decodePolygon(
            int[] parts,
            double[] x,
            double[] y,
            CoordinateSequenceFactory csf) {

        if (parts.length == 0) {
            return GF.createPolygon();
        }

        LinearRing shell = null;

        List<LinearRing> holes =
                new ArrayList<>();

        for (int i = 0;
             i < parts.length;
             i++) {

            int start =
                    Math.abs(parts[i]);

            int end =
                    (i + 1 < parts.length)
                            ? Math.abs(parts[i + 1])
                            : x.length;

            CoordinateSequence seq =
                    createSequence(
                            csf,
                            x,
                            y,
                            start,
                            end
                    );

            LinearRing ring =
                    GF.createLinearRing(seq);

            if (parts[i] >= 0) {

                shell = ring;

            } else {

                holes.add(ring);
            }
        }

        if (shell == null) {
            return GF.createPolygon();
        }

        if (holes.isEmpty()) {
            return GF.createPolygon(shell);
        }

        /*
         * فقط هنگام ساخت Polygon
         */
        return GF.createPolygon(
                shell,
                holes.toArray(
                        new LinearRing[0]
                )
        );
    }


    // =========================================================
    // MULTIPOLYGON
    // =========================================================

    private static MultiPolygon decodeMultiPolygon(
            int[] parts,
            double[] x,
            double[] y,
            CoordinateSequenceFactory csf) {

        if (parts.length == 0) {
            return GF.createMultiPolygon();
        }

        List<Polygon> polygons =
                new ArrayList<>();

        LinearRing currentShell = null;

        List<LinearRing> currentHoles =
                new ArrayList<>();

        for (int i = 0;
             i < parts.length;
             i++) {

            int start =
                    Math.abs(parts[i]);

            int end =
                    (i + 1 < parts.length)
                            ? Math.abs(parts[i + 1])
                            : x.length;

            CoordinateSequence seq =
                    createSequence(
                            csf,
                            x,
                            y,
                            start,
                            end
                    );

            LinearRing ring =
                    GF.createLinearRing(seq);

            /*
             * Shell
             */
            if (parts[i] >= 0) {

                /*
                 * Polygon قبلی را تمام کن
                 */
                if (currentShell != null) {

                    polygons.add(
                            createPolygon(
                                    currentShell,
                                    currentHoles
                            )
                    );
                }

                currentShell = ring;

                /*
                 * List جدید برای holeهای Polygon جدید
                 */
                currentHoles =
                        new ArrayList<>();
            }

            /*
             * Hole
             */
            else {

                currentHoles.add(ring);
            }
        }

        /*
         * آخرین Polygon
         */
        if (currentShell != null) {

            polygons.add(
                    createPolygon(
                            currentShell,
                            currentHoles
                    )
            );
        }

        /*
         * فقط هنگام ساخت MultiPolygon
         */
        return GF.createMultiPolygon(
                polygons.toArray(
                        new Polygon[0]
                )
        );
    }


    // =========================================================
    // CREATE POLYGON
    // =========================================================

    private static Polygon createPolygon(
            LinearRing shell,
            List<LinearRing> holes) {

        if (holes.isEmpty()) {
            return GF.createPolygon(shell);
        }

        /*
         * تبدیل List -> Array
         * دقیقاً در لحظه ساخت Geometry
         */
        return GF.createPolygon(
                shell,
                holes.toArray(
                        new LinearRing[0]
                )
        );
    }


    // =========================================================
    // CREATE COORDINATE SEQUENCE
    // =========================================================

    private static CoordinateSequence createSequence(
            CoordinateSequenceFactory csf,
            double[] x,
            double[] y,
            int start,
            int end) {

        int size =
                end - start;

        CoordinateSequence seq =
                csf.create(size, 2);

        for (int i = 0; i < size; i++) {

            int index =
                    start + i;

            seq.setOrdinate(
                    i,
                    0,
                    x[index]
            );

            seq.setOrdinate(
                    i,
                    1,
                    y[index]
            );
        }

        return seq;
    }


    // =========================================================
    // LIST -> DOUBLE ARRAY
    // =========================================================

    private static double[] toDoubleArray(
            List<Double> list) {

        if (list == null ||
                list.isEmpty()) {

            return new double[0];
        }

        double[] arr =
                new double[list.size()];

        for (int i = 0;
             i < arr.length;
             i++) {

            arr[i] =
                    list.get(i);
        }

        return arr;
    }


    // =========================================================
    // LIST -> INT ARRAY
    // =========================================================

    private static int[] toIntArray(
            List<Integer> list) {

        if (list == null ||
                list.isEmpty()) {

            return new int[0];
        }

        int[] arr =
                new int[list.size()];

        for (int i = 0;
             i < arr.length;
             i++) {

            arr[i] =
                    list.get(i);
        }

        return arr;
    }
}