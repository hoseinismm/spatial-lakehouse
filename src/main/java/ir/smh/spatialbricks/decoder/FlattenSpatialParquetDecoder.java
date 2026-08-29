package ir.smh.spatialbricks.decoder;

import org.apache.spark.sql.Row;
import org.locationtech.jts.geom.*;

import java.util.List;

public class FlattenSpatialParquetDecoder {

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

        if (type == 1) {
            return decodePoint(row);
        }

        double[] x =
                toDoubleArray(row.getList(1));

        double[] y =
                toDoubleArray(row.getList(2));

        List<Integer> parts =
                row.getList(3);

        if (x.length != y.length) {
            throw new IllegalArgumentException(
                    "X and Y arrays have different lengths: "
                            + x.length
                            + " != "
                            + y.length
            );
        }

        DecodeContext context =
                new DecodeContext(
                        x,
                        y,
                        parts
                );

        return switch (type) {

            case 2 -> decodeLineString(context);
            case 3 -> decodePolygon(context);
            case 4 -> decodeMultiPoint(context);
            case 5 -> decodeMultiLineString(context);
            case 6 -> decodeMultiPolygon(context);

            default -> null;
        };
    }


    // =========================================================
    // DECODE CONTEXT
    // =========================================================

    private static class DecodeContext {

        final double[] x;
        final double[] y;
        final List<Integer> parts;

        DecodeContext(
                double[] x,
                double[] y,
                List<Integer> parts) {

            this.x = x;
            this.y = y;
            this.parts = parts;
        }
    }


    // =========================================================
    // POINT
    // =========================================================

    private static Point decodePoint(Row row) {

        List<Double> x =
                row.getList(1);

        List<Double> y =
                row.getList(2);

        if (x == null || y == null ||
                x.isEmpty() || y.isEmpty()) {

            return GF.createPoint();
        }

        return GF.createPoint(
                new Coordinate(
                        x.get(0),
                        y.get(0)
                )
        );
    }


    // =========================================================
    // LINESTRING
    // =========================================================

    private static LineString decodeLineString(
            DecodeContext context) {

        if (context.x.length == 0) {
            return GF.createLineString();
        }

        CoordinateSequence seq =
                createSequence(
                        context,
                        0,
                        context.x.length
                );

        return GF.createLineString(seq);
    }


    // =========================================================
    // MULTIPOINT
    // =========================================================

    private static MultiPoint decodeMultiPoint(
            DecodeContext context) {

        int size =
                context.x.length;

        if (size == 0) {
            return GF.createMultiPoint();
        }

        Point[] points =
                new Point[size];

        for (int i = 0; i < size; i++) {

            CoordinateSequence seq =
                    createSequence(
                            context,
                            i,
                            i + 1
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
            DecodeContext context) {

        int count =
                context.parts.size();

        if (count == 0) {
            return GF.createMultiLineString();
        }

        LineString[] lines =
                new LineString[count];

        int coordinateCount =
                context.x.length;

        for (int i = 0; i < count; i++) {

            int part =
                    context.parts.get(i);

            int start =
                    Math.abs(part);

            int end =
                    (i + 1 < count)
                            ? Math.abs(
                            context.parts.get(i + 1)
                    )
                            : coordinateCount;

            CoordinateSequence seq =
                    createSequence(
                            context,
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
            DecodeContext context) {

        int partsCount =
                context.parts.size();

        if (partsCount == 0) {
            return GF.createPolygon();
        }
        int start =0;

        LinearRing shell = null;

        LinearRing[] holes = null;

        if (partsCount > 1) {
            holes = new LinearRing[partsCount-1];
        }

        for (int i = 0; i < partsCount; i++) {

            int end;

            if (i + 1 < partsCount) {
                end = Math.abs(context.parts.get(i + 1));
            } else {
                end = context.x.length;
            }

            LinearRing ring =
                    GF.createLinearRing(
                            createSequence(
                                    context,
                                    start,
                                    end
                            )
                    );


            if (i==0) {

                shell = ring;
                if (partsCount ==1) {
                    return GF.createPolygon(shell);
                }


            } else {

                holes[i-1] =
                        ring;
            }
            start = end;
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
            DecodeContext context) {

        int partsCount =
                context.parts.size();

        if (partsCount == 0) {
            return GF.createMultiPolygon();
        }

        // Number of shells = number of polygons
        int polygonCount = 0;

        for (int part : context.parts) {

            if (part >= 0) {
                polygonCount++;
            }
        }

        Polygon[] polygons =
                new Polygon[polygonCount];

        int polygonIndex = 0;

        int holeCount = 0;

        LinearRing currentShell = null;

        LinearRing[] holes = null;

        int holeIndex = 0;

        int coordinateCount =
                context.x.length;

        int end = 0;

        for (int i = 0;
             i < partsCount;
             i++) {

            /*
             * The start of the current ring is the
             * signed end of the previous ring.
             */
            int start = end;

            /*
             * Keep the sign of the next part because
             * it identifies shell/hole in the next iteration.
             */
            end =
                    (i + 1 < partsCount)
                            ? context.parts.get(i + 1)
                            : coordinateCount;

            LinearRing ring =
                    GF.createLinearRing(
                            createSequence(
                                    context,
                                    Math.abs(start),
                                    Math.abs(end)
                            )
                    );

            // -----------------------------------------------------
            // SHELL
            // -----------------------------------------------------

            if (start >= 0) {

                currentShell = ring;

                holeCount = 0;

                /*
                 * Count holes belonging to this shell.
                 */
                for (int j = i + 1;
                     j < partsCount;
                     j++) {

                    if (context.parts.get(j) >= 0) {
                        break;
                    }

                    holeCount++;
                }

                if (holeCount == 0) {

                    polygons[polygonIndex++] =
                            GF.createPolygon(
                                    currentShell
                            );

                } else {

                    holes =
                            new LinearRing[holeCount];

                    holeIndex = 0;
                }

            }

            // -----------------------------------------------------
            // HOLE
            // -----------------------------------------------------

            else {

                if (holes != null) {

                    holes[holeIndex++] =
                            ring;

                    if (holeIndex == holeCount) {

                        polygons[polygonIndex++] =
                                GF.createPolygon(
                                        currentShell,
                                        holes
                                );

                        holes = null;
                        holeIndex = 0;
                        holeCount = 0;
                    }
                }
            }
        }

        return GF.createMultiPolygon(polygons);
    }


    // =========================================================
    // CREATE COORDINATE SEQUENCE
    // =========================================================

    private static CoordinateSequence createSequence(
            DecodeContext context,
            int start,
            int end) {

        int size =
                end - start;

        CoordinateSequence seq =
                CSF.create(
                        size,
                        2
                );

        for (int i = 0;
             i < size;
             i++) {

            int index =
                    start + i;

            seq.setOrdinate(
                    i,
                    0,
                    context.x[index]
            );

            seq.setOrdinate(
                    i,
                    1,
                    context.y[index]
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

        double[] array =
                new double[list.size()];

        for (int i = 0;
             i < array.length;
             i++) {

            Double value =
                    list.get(i);

            if (value == null) {

                throw new IllegalArgumentException(
                        "X/Y array contains null at index "
                                + i
                );
            }

            array[i] = value;
        }

        return array;
    }
}