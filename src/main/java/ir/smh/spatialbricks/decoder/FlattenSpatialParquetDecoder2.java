package ir.smh.spatialbricks.decoder;

import org.apache.spark.sql.Row;
import org.locationtech.jts.geom.*;

import java.util.List;

public class FlattenSpatialParquetDecoder2 {

    private static final GeometryFactory GF =
            new GeometryFactory();

    private static final CoordinateSequenceFactory CSF =
            GF.getCoordinateSequenceFactory();

    // =========================================================
    // ENTRY POINT
    // =========================================================

    public static Geometry geometryToJTS(Row row) {

        if (row == null) {
            return null;
        }

        int type = row.getInt(0);

        double[] x = toDoubleArray(row.getList(1));
        double[] y = toDoubleArray(row.getList(2));
        int[] parts = toIntArray(row.getList(3));

        return switch (type) {

            case 1 -> decodePoint(x, y);

            case 2 -> decodeLineString(x, y);

            case 3 -> decodePolygon(parts, x, y);

            case 4 -> decodeMultiPoint(x, y);

            case 5 -> decodeMultiLineString(parts, x, y);

            case 6 -> decodeMultiPolygon(parts, x, y);

            default -> null;
        };
    }

    // =========================================================
    // POINT
    // =========================================================

    private static Point decodePoint(
            double[] x,
            double[] y) {

        if (x.length == 0 || y.length == 0) {
            return GF.createPoint();
        }

        CoordinateSequence seq =
                CSF.create(1, 2);

        seq.setOrdinate(0, 0, x[0]);
        seq.setOrdinate(0, 1, y[0]);

        return GF.createPoint(seq);
    }

    // =========================================================
    // LINESTRING
    // =========================================================

    private static LineString decodeLineString(
            double[] x,
            double[] y) {

        if (x.length == 0) {
            return GF.createLineString();
        }

        CoordinateSequence seq =
                createSequence(
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
            double[] y) {

        int size = x.length;

        Point[] points =
                new Point[size];

        for (int i = 0; i < size; i++) {

            CoordinateSequence seq =
                    CSF.create(1, 2);

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

            points[i] =
                    GF.createPoint(seq);
        }

        return GF.createMultiPoint(points);
    }

    // =========================================================
    // MULTILINESTRING
    // =========================================================

    private static MultiLineString decodeMultiLineString(
            int[] parts,
            double[] x,
            double[] y) {

        int count = parts.length;

        LineString[] lines =
                new LineString[count];

        int coordinateCount = x.length;

        for (int i = 0; i < count; i++) {

            int part = parts[i];

            int start = Math.abs(part);

            int end =
                    (i + 1 < count)
                            ? Math.abs(parts[i + 1])
                            : coordinateCount;

            CoordinateSequence seq =
                    createSequence(
                            x,
                            y,
                            start,
                            end
                    );

            lines[i] =
                    GF.createLineString(seq);
        }

        return GF.createMultiLineString(lines);
    }

    // =========================================================
    // POLYGON
    // =========================================================

    private static Polygon decodePolygon(
            int[] parts,
            double[] x,
            double[] y) {

        LinearRing shell = null;

        int holeCount = 0;

        for (int part : parts) {

            if (part < 0) {
                holeCount++;
            }
        }

        LinearRing[] holes =
                holeCount == 0
                        ? null
                        : new LinearRing[holeCount];

        int holeIndex = 0;

        int coordinateCount = x.length;

        for (int i = 0; i < parts.length; i++) {

            int part = parts[i];

            int start =
                    Math.abs(part);

            int end =
                    (i + 1 < parts.length)
                            ? Math.abs(parts[i + 1])
                            : coordinateCount;

            CoordinateSequence seq =
                    createSequence(
                            x,
                            y,
                            start,
                            end
                    );

            LinearRing ring =
                    GF.createLinearRing(seq);

            if (part >= 0) {

                shell = ring;

            } else {

                holes[holeIndex++] = ring;
            }
        }

        if (shell == null) {
            return GF.createPolygon();
        }

        if (holeCount == 0) {
            return GF.createPolygon(shell);
        }

        return GF.createPolygon(
                shell,
                holes
        );
    }

    // =========================================================
    // MULTIPOLYGON
    // =========================================================

    private static MultiPolygon decodeMultiPolygon(
            int[] parts,
            double[] x,
            double[] y) {

        if (parts.length == 0) {
            return GF.createMultiPolygon();
        }

        /*
         * تعداد Polygonها = تعداد Shellها
         */
        int polygonCount = 0;

        for (int part : parts) {

            if (part >= 0) {
                polygonCount++;
            }
        }

        Polygon[] polygons =
                new Polygon[polygonCount];

        LinearRing currentShell = null;

        /*
         * فعلاً برای Holeها List نداریم.
         * تعداد Holeهای هر Polygon را قبل از ساخت
         * Polygon پیدا می‌کنیم.
         */
        LinearRing[] holes = null;

        int holeCount = 0;
        int polygonIndex = 0;

        int coordinateCount = x.length;

        for (int i = 0; i < parts.length; i++) {

            int part = parts[i];

            int start =
                    Math.abs(part);

            int end =
                    (i + 1 < parts.length)
                            ? Math.abs(parts[i + 1])
                            : coordinateCount;

            CoordinateSequence seq =
                    createSequence(
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
            if (part >= 0) {

                /*
                 * Polygon قبلی را ببند.
                 */
                if (currentShell != null) {

                    polygons[polygonIndex++] =
                            holeCount == 0
                                    ? GF.createPolygon(
                                    currentShell
                            )
                                    : GF.createPolygon(
                                    currentShell,
                                    holes
                            );
                }

                /*
                 * Shell جدید
                 */
                currentShell = ring;

                /*
                 * Holeهای Polygon جدید را
                 * بعداً ایجاد می‌کنیم.
                 */
                holes = null;
                holeCount = 0;

            } else {

                /*
                 * Hole
                 *
                 * اگر اولین Hole باشد، ابتدا تعداد
                 * Holeهای این Polygon را پیدا می‌کنیم.
                 */
                if (holeCount == 0) {

                    int count = 0;

                    for (int j = i;
                         j < parts.length;
                         j++) {

                        if (parts[j] < 0) {
                            count++;
                        } else {
                            break;
                        }
                    }

                    holes =
                            new LinearRing[count];
                }

                holes[holeCount++] = ring;
            }
        }

        /*
         * آخرین Polygon
         */
        if (currentShell != null) {

            polygons[polygonIndex++] =
                    holeCount == 0
                            ? GF.createPolygon(
                            currentShell
                    )
                            : GF.createPolygon(
                            currentShell,
                            holes
                    );
        }

        return GF.createMultiPolygon(polygons);
    }

    // =========================================================
    // COORDINATE SEQUENCE
    // =========================================================

    private static CoordinateSequence createSequence(
            double[] x,
            double[] y,
            int start,
            int end) {

        int size = end - start;

        CoordinateSequence seq =
                CSF.create(size, 2);

        for (int i = 0; i < size; i++) {

            seq.setOrdinate(
                    i,
                    0,
                    x[start + i]
            );

            seq.setOrdinate(
                    i,
                    1,
                    y[start + i]
            );
        }

        return seq;
    }

    // =========================================================
    // ARRAY CONVERSION
    // =========================================================

    private static double[] toDoubleArray(
            List<Double> list) {

        if (list == null || list.isEmpty()) {
            return new double[0];
        }

        double[] array =
                new double[list.size()];

        for (int i = 0; i < array.length; i++) {
            array[i] = list.get(i);
        }

        return array;
    }

    private static int[] toIntArray(
            List<Integer> list) {

        if (list == null || list.isEmpty()) {
            return new int[0];
        }

        int[] array =
                new int[list.size()];

        for (int i = 0; i < array.length; i++) {
            array[i] = list.get(i);
        }

        return array;
    }
}