package ir.smh.spatialbricks.decoder;

import org.apache.spark.sql.Row;
import org.locationtech.jts.geom.*;

import java.util.ArrayList;
import java.util.List;

public class FlattenSpatialParquetDecoder4 {

    private static final GeometryFactory GF =
            new GeometryFactory();

    private static final CoordinateSequenceFactory CSF =
            GF.getCoordinateSequenceFactory();


    // =========================================================
    // ENTRY POINT
    // =========================================================

    public static Geometry geometryToJTS(Row row) {

        int ARRAY_THRESHOLD = 1;

        if (row == null) {
            return null;
        }

        int type = row.getInt(0);

        List<Double> x = row.getList(1);
        List<Double> y = row.getList(2);
        List<Integer> parts = row.getList(3);

        if (type == 1) {
            return decodePoint(x, y);
        }

        int vertexCount =
                x == null ? 0 : x.size();

        if (vertexCount < ARRAY_THRESHOLD) {

            return decode(
                    type,
                    new ListDecodeContext(
                            x,
                            y,
                            parts
                    )
            );
        }

        return decode(
                type,
                new DecodeContext(
                        toDoubleArray(x),
                        toDoubleArray(y),
                        parts
                )
        );
    }

    private static Geometry decode(
            int type,
            DecodeContext context) {

        return switch (type) {
            case 2 -> decodeLineString(context);
            case 3 -> decodePolygon(context);
            case 4 -> decodeMultiPoint(context);
            case 5 -> decodeMultiLineString(context);
            case 6 -> decodeMultiPolygon(context);
            default -> null;
        };
    }

    private static Geometry decode(
            int type,
            ListDecodeContext context) {

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
    // DECODE AND SUBCLASSES
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


    private static class ListDecodeContext {

        final List<Double> x;
        final List<Double> y;
        final List<Integer> parts;

        ListDecodeContext(
                List<Double> x,
                List<Double> y,
                List<Integer> parts) {

            this.x = x;
            this.y = y;
            this.parts = parts;
        }
    }

    // =========================================================
    // POINT
    // =========================================================

    private static Point decodePoint(List<Double> x , List<Double> y) {

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

    private static LineString decodeLineString(
            ListDecodeContext context) {

        int size = context.x.size();

        if (size == 0) {
            return GF.createLineString();
        }

        CoordinateSequence seq =
                CSF.create(size, 2);

        for (int i = 0; i < size; i++) {

            seq.setOrdinate(
                    i,
                    0,
                    context.x.get(i)
            );

            seq.setOrdinate(
                    i,
                    1,
                    context.y.get(i)
            );
        }

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

            points[i] =
                    GF.createPoint(
                            new Coordinate(
                                    context.x[i],
                                    context.y[i]
                            )
                    );
        }

        return GF.createMultiPoint(points);
    }

    private static MultiPoint decodeMultiPoint(
            ListDecodeContext context) {

        int size =
                context.x.size();

        if (size == 0) {
            return GF.createMultiPoint();
        }

        Point[] points =
                new Point[size];

        for (int i = 0; i < size; i++) {

            points[i] =
                    GF.createPoint(
                            new Coordinate(
                                    context.x.get(i),
                                    context.y.get(i)
                            )
                    );
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

        int lineEnd = 0;

        for (int i = 0; i < count; i++) {

            int lineStart =
                    lineEnd;

            lineEnd =
                    (i + 1 < count)
                            ? Math.abs(
                            context.parts.get(i + 1)
                    )
                            : coordinateCount;

            CoordinateSequence seq =
                    createSequence(
                            context,
                            lineStart,
                            lineEnd
                    );

            lines[i] =
                    GF.createLineString(seq);
        }

        return GF.createMultiLineString(lines);
    }

    private static MultiLineString decodeMultiLineString(
            ListDecodeContext context) {

        int count =
                context.parts.size();

        if (count == 0) {
            return GF.createMultiLineString();
        }

        LineString[] lines =
                new LineString[count];

        int coordinateCount =
                context.x.size();

        int lineEnd = 0;

        for (int i = 0; i < count; i++) {

            int lineStart =
                    lineEnd;

            lineEnd =
                    (i + 1 < count)
                            ? Math.abs(
                            context.parts.get(i + 1)
                    )
                            : coordinateCount;

            CoordinateSequence seq =
                    CSF.create(
                            lineEnd - lineStart,
                            2
                    );

            for (int j = lineStart;
                 j < lineEnd;
                 j++) {

                int sequenceIndex =
                        j - lineStart;

                seq.setOrdinate(
                        sequenceIndex,
                        0,
                        context.x.get(j)
                );

                seq.setOrdinate(
                        sequenceIndex,
                        1,
                        context.y.get(j)
                );
            }

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

        LinearRing shell = null;

        LinearRing[] holes =
                partsCount > 1
                        ? new LinearRing[partsCount - 1]
                        : null;

        int ringStart = 0;

        for (int i = 0; i < partsCount; i++) {

            int ringEnd =
                    (i + 1 < partsCount)
                            ? Math.abs(
                            context.parts.get(i + 1)
                    )
                            : context.x.length;

            LinearRing ring =
                    GF.createLinearRing(
                            createSequence(
                                    context,
                                    ringStart,
                                    ringEnd
                            )
                    );

            if (i == 0) {

                shell = ring;

            } else {

                holes[i - 1] = ring;
            }

            ringStart = ringEnd;
        }

        return GF.createPolygon(
                shell,
                holes
        );
    }

    private static Polygon decodePolygon(
            ListDecodeContext context) {

        int partsCount =
                context.parts.size();

        if (partsCount == 0) {
            return GF.createPolygon();
        }

        LinearRing shell = null;

        LinearRing[] holes =
                partsCount > 1
                        ? new LinearRing[partsCount - 1]
                        : null;

        int ringStart = 0;

        for (int i = 0; i < partsCount; i++) {

            int ringEnd =
                    (i + 1 < partsCount)
                            ? Math.abs(
                            context.parts.get(i + 1)
                    )
                            : context.x.size();

            int size =
                    ringEnd - ringStart;

            CoordinateSequence seq =
                    CSF.create(size, 2);

            for (int j = 0; j < size; j++) {

                int coordinateIndex =
                        ringStart + j;

                seq.setOrdinate(
                        j,
                        0,
                        context.x.get(coordinateIndex)
                );

                seq.setOrdinate(
                        j,
                        1,
                        context.y.get(coordinateIndex)
                );
            }

            LinearRing ring =
                    GF.createLinearRing(seq);

            if (i == 0) {

                shell = ring;

            } else {

                holes[i - 1] = ring;
            }

            ringStart = ringEnd;
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

        int ringEnd = 0;

        for (int i = 0;
             i < partsCount;
             i++) {

            int ringStart =
                    ringEnd;

            ringEnd =
                    (i + 1 < partsCount)
                            ? context.parts.get(i + 1)
                            : coordinateCount;

            LinearRing ring =
                    GF.createLinearRing(
                            createSequence(
                                    context,
                                    Math.abs(ringStart),
                                    Math.abs(ringEnd)
                            )
                    );

            if (ringStart >= 0) {

                currentShell = ring;

                holeCount = 0;


                for (int j = i + 1; j < partsCount; j++) {

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

            } else {

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


        return GF.createMultiPolygon(polygons);
    }

    private static MultiPolygon decodeMultiPolygon(
            ListDecodeContext context) {

        int partsCount =
                context.parts.size();

        if (partsCount == 0) {
            return GF.createMultiPolygon();
        }

        List<Polygon> polygons =
                new ArrayList<>();

        LinearRing shell = null;

        List<LinearRing> holes = null;

        int coordinateCount =
                context.x.size();

        int ringEnd = 0;

        for (int i = 0;
             i < partsCount;
             i++) {

            int ringStart =
                    ringEnd;

            ringEnd =
                    (i + 1 < partsCount)
                            ? context.parts.get(i + 1)
                            : coordinateCount;

            CoordinateSequence seq =
                    createSequence(
                            CSF,
                            context.x,
                            context.y,
                            Math.abs(ringStart),
                            Math.abs(ringEnd)
                    );

            LinearRing ring =
                    GF.createLinearRing(seq);

            // =====================================================
            // SHELL
            // =====================================================

            if (ringStart >= 0) {

                // Finalize previous polygon
                if (shell != null) {

                    polygons.add(
                            GF.createPolygon(
                                    shell,
                                    holes == null
                                            ? null
                                            : holes.toArray(
                                            new LinearRing[0]
                                    )
                            )
                    );
                }

                shell = ring;
                holes = null;
            }

            // =====================================================
            // HOLE
            // =====================================================

            else {

                if (holes == null) {
                    holes = new ArrayList<>();
                }

                holes.add(ring);
            }
        }

        // =========================================================
        // FINAL POLYGON
        // =========================================================

        if (shell != null) {

            polygons.add(
                    GF.createPolygon(
                            shell,
                            holes == null
                                    ? null
                                    : holes.toArray(
                                    new LinearRing[0]
                            )
                    )
            );
        }

        return GF.createMultiPolygon(
                polygons.toArray(
                        new Polygon[0]
                )
        );
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


    private static CoordinateSequence createSequence(
            CoordinateSequenceFactory csf,
            List<Double> x,
            List<Double> y,
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
                    x.get(index)
            );

            seq.setOrdinate(
                    i,
                    1,
                    y.get(index)
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

