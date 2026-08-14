package ir.smh.spatialbricks.decoder;

import org.apache.spark.sql.Row;
import org.locationtech.jts.geom.*;

import java.util.ArrayList;
import java.util.List;

public class NFSPDecoder {

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

        int[] geometries =
                toIntArray(row.getList(4));

        if (x.length != y.length) {
            throw new IllegalArgumentException(
                    "X and Y arrays have different lengths: "
                            + x.length + " != " + y.length
            );
        }

        CoordinateSequenceFactory csf =
                GF.getCoordinateSequenceFactory();

        DecodeContext context =
                new DecodeContext(
                        x,
                        y,
                        parts,
                        csf
                );

        /*
         * =====================================================
         * NORMAL ROOT GEOMETRY
         * =====================================================
         */

        if (type != 0) {

            if (type < 1 || type > 6) {
                throw new IllegalArgumentException(
                        "Invalid root geometry type: " + type
                );
            }

            Geometry result =
                    decodeSimpleGeometry(
                            type,
                            context
                    );

            /*
             * A normal root geometry must consume
             * all coordinate and parts data.
             */
            if (context.xIndex != x.length) {
                throw new IllegalArgumentException(
                        "Unused coordinates remain after decoding root geometry: "
                                + (x.length - context.xIndex)
                );
            }

            if (context.partsIndex != parts.length) {
                throw new IllegalArgumentException(
                        "Unused parts remain after decoding root geometry: "
                                + (parts.length - context.partsIndex)
                );
            }

            return result;
        }

        /*
         * =====================================================
         * GEOMETRY COLLECTION ROOT
         * =====================================================
         */

        GeometryCursor geometryCursor =
                new GeometryCursor(geometries);

        GeometryCollection result =
                decodeGeometryCollection(
                        geometryCursor,
                        context
                );

        /*
         * All geometry markers must be consumed.
         */
        if (geometryCursor.hasNext()) {
            throw new IllegalArgumentException(
                    "Unused values remain in geometries array"
            );
        }

        /*
         * All coordinates must be consumed.
         */
        if (context.xIndex != x.length) {
            throw new IllegalArgumentException(
                    "Unused coordinates remain after decoding GeometryCollection: "
                            + (x.length - context.xIndex)
            );
        }

        /*
         * All parts must be consumed.
         */
        if (context.partsIndex != parts.length) {
            throw new IllegalArgumentException(
                    "Unused parts remain after decoding GeometryCollection: "
                            + (parts.length - context.partsIndex)
            );
        }

        return result;
    }


    // =========================================================
    // CONTEXT
    // =========================================================

    private static class DecodeContext {

        final double[] x;
        final double[] y;
        final int[] parts;
        final CoordinateSequenceFactory csf;

        int xIndex = 0;
        int partsIndex = 0;

        DecodeContext(
                double[] x,
                double[] y,
                int[] parts,
                CoordinateSequenceFactory csf) {

            this.x = x;
            this.y = y;
            this.parts = parts;
            this.csf = csf;
        }
    }


    // =========================================================
    // GEOMETRY CURSOR
    // =========================================================

    private static class GeometryCursor {

        final int[] geometries;
        int index = 0;

        GeometryCursor(int[] geometries) {
            this.geometries = geometries;
        }

        boolean hasNext() {
            return index < geometries.length;
        }

        int next() {

            if (!hasNext()) {
                throw new IllegalArgumentException(
                        "Unexpected end of geometries array"
                );
            }

            return geometries[index++];
        }
    }


    // =========================================================
    // GEOMETRY COLLECTION
    // =========================================================

    private static GeometryCollection decodeGeometryCollection(
            GeometryCursor geometryCursor,
            DecodeContext context) {

        int startMarker =
                geometryCursor.next();

        if (startMarker != 0) {
            throw new IllegalArgumentException(
                    "Expected GeometryCollection start marker 0"
            );
        }

        return decodeGeometryCollectionAfterStart(
                geometryCursor,
                context
        );
    }

    private static GeometryCollection decodeGeometryCollectionAfterStart(
            GeometryCursor geometryCursor,
            DecodeContext context) {

        List<Geometry> geometries =
                new ArrayList<>();

        while (geometryCursor.hasNext()) {

            int type =
                    geometryCursor.next();

            /*
             * 7 = end of current GeometryCollection
             */
            if (type == 7) {

                return GF.createGeometryCollection(
                        geometries.toArray(
                                new Geometry[0]
                        )
                );
            }

            /*
             * 0 = nested GeometryCollection
             *
             * The opening 0 has already been consumed.
             */
            if (type == 0) {

                GeometryCollection nested =
                        decodeGeometryCollectionAfterStart(
                                geometryCursor,
                                context
                        );

                geometries.add(nested);

                continue;
            }

            /*
             * 1..6 = normal geometry
             */
            if (type < 1 || type > 6) {

                throw new IllegalArgumentException(
                        "Invalid geometry type in geometries array: "
                                + type
                );
            }

            Geometry geometry =
                    decodeSimpleGeometry(
                            type,
                            context
                    );

            geometries.add(geometry);
        }

        throw new IllegalArgumentException(
                "GeometryCollection has no end marker 7"
        );
    }


    // =========================================================
    // SIMPLE GEOMETRY
    // =========================================================

    private static Geometry decodeSimpleGeometry(
            int type,
            DecodeContext context) {

        return switch (type) {

            case 1 ->
                    decodePoint(context);

            case 2 ->
                    decodeLineString(context);

            case 3 ->
                    decodePolygon(context);

            case 4 ->
                    decodeMultiPoint(context);

            case 5 ->
                    decodeMultiLineString(context);

            case 6 ->
                    decodeMultiPolygon(context);

            default ->
                    throw new IllegalArgumentException(
                            "Unsupported geometry type: " + type
                    );
        };
    }


    // =========================================================
    // POINT
    // =========================================================

    private static Point decodePoint(
            DecodeContext context) {

        int[] p =
                readFixedParts(context, 2);

        /*
         * [0,-1] = EMPTY POINT
         */
        if (p[0] == 0 && p[1] == -1) {
            return GF.createPoint();
        }

        if (p[0] != 0 || p[1] != 0) {
            throw new IllegalArgumentException(
                    "Invalid Point parts: ["
                            + p[0] + ", " + p[1] + "]"
            );
        }

        checkCoordinateRange(
                context,
                1
        );

        CoordinateSequence seq =
                createSequence(
                        context.csf,
                        context.x,
                        context.y,
                        context.xIndex,
                        context.xIndex + 1
                );

        context.xIndex++;

        return GF.createPoint(seq);
    }


    // =========================================================
    // MULTIPOINT
    // =========================================================

    private static MultiPoint decodeMultiPoint(
            DecodeContext context) {

        int[] p =
                readFixedParts(context, 2);

        if (p[0] != 0) {
            throw new IllegalArgumentException(
                    "Invalid MultiPoint start: " + p[0]
            );
        }

        /*
         * [0,-1] = EMPTY MULTIPOINT
         */
        if (p[1] == -1) {
            return GF.createMultiPoint();
        }

        if (p[1] < 0) {
            throw new IllegalArgumentException(
                    "Invalid MultiPoint endpoint: " + p[1]
            );
        }

        int numberOfPoints =
                p[1] + 1;

        checkCoordinateRange(
                context,
                numberOfPoints
        );

        Point[] points =
                new Point[numberOfPoints];

        for (int i = 0; i < numberOfPoints; i++) {

            CoordinateSequence seq =
                    context.csf.create(1, 2);

            seq.setOrdinate(
                    0,
                    0,
                    context.x[context.xIndex]
            );

            seq.setOrdinate(
                    0,
                    1,
                    context.y[context.xIndex]
            );

            context.xIndex++;

            points[i] =
                    GF.createPoint(seq);
        }

        return GF.createMultiPoint(points);
    }


    // =========================================================
    // LINESTRING
    // =========================================================

    private static LineString decodeLineString(
            DecodeContext context) {

        int[] p =
                readFixedParts(context, 2);

        if (p[0] != 0) {
            throw new IllegalArgumentException(
                    "Invalid LineString start: " + p[0]
            );
        }

        /*
         * [0,-1] = EMPTY LINESTRING
         */
        if (p[1] == -1) {
            return GF.createLineString();
        }

        if (p[1] < 0) {
            throw new IllegalArgumentException(
                    "Invalid LineString endpoint: " + p[1]
            );
        }

        int numberOfPoints =
                p[1] + 1;

        checkCoordinateRange(
                context,
                numberOfPoints
        );

        CoordinateSequence seq =
                createSequence(
                        context.csf,
                        context.x,
                        context.y,
                        context.xIndex,
                        context.xIndex + numberOfPoints
                );

        context.xIndex += numberOfPoints;

        return GF.createLineString(seq);
    }


    // =========================================================
    // MULTILINESTRING
    // =========================================================

    private static MultiLineString decodeMultiLineString(
            DecodeContext context) {

        int[] parts =
                readVariableParts(context);

        /*
         * [0,-1] = EMPTY MULTILINESTRING
         */
        if (isEmptyMarker(parts)) {
            return GF.createMultiLineString();
        }

        if (parts.length < 2) {
            throw new IllegalArgumentException(
                    "Invalid MultiLineString parts"
            );
        }

        int endpoint =
                parts[parts.length - 1];

        if (endpoint < 0) {
            throw new IllegalArgumentException(
                    "Invalid MultiLineString endpoint: "
                            + endpoint
            );
        }

        int numberOfLines =
                parts.length - 1;

        LineString[] lines =
                new LineString[numberOfLines];

        for (int i = 0; i < numberOfLines; i++) {

            int start =
                    parts[i];

            int end =
                    (i + 1 < numberOfLines)
                            ? parts[i + 1] - 1
                            : endpoint;

            if (start < 0 || end < start) {
                throw new IllegalArgumentException(
                        "Invalid MultiLineString part: start="
                                + start
                                + ", end="
                                + end
                );
            }

            int numberOfPoints =
                    end - start + 1;

            checkCoordinateRange(
                    context,
                    numberOfPoints
            );

            CoordinateSequence seq =
                    createSequence(
                            context.csf,
                            context.x,
                            context.y,
                            context.xIndex,
                            context.xIndex + numberOfPoints
                    );

            context.xIndex += numberOfPoints;

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

        int[] parts =
                readVariableParts(context);

        /*
         * [0,-1] = EMPTY POLYGON
         */
        if (isEmptyMarker(parts)) {
            return GF.createPolygon();
        }

        if (parts.length < 2) {
            throw new IllegalArgumentException(
                    "Invalid Polygon parts"
            );
        }

        int endpoint =
                parts[parts.length - 1];

        if (endpoint < 0) {
            throw new IllegalArgumentException(
                    "Invalid Polygon endpoint: "
                            + endpoint
            );
        }

        LinearRing shell = null;

        List<LinearRing> holes =
                new ArrayList<>();

        int numberOfParts =
                parts.length - 1;

        for (int i = 0; i < numberOfParts; i++) {

            int start =
                    Math.abs(parts[i]);

            int end =
                    (i + 1 < numberOfParts)
                            ? Math.abs(parts[i + 1]) - 1
                            : endpoint;

            if (end < start) {
                throw new IllegalArgumentException(
                        "Invalid Polygon part: start="
                                + start
                                + ", end="
                                + end
                );
            }

            int numberOfPoints =
                    end - start + 1;

            checkCoordinateRange(
                    context,
                    numberOfPoints
            );

            CoordinateSequence seq =
                    createSequence(
                            context.csf,
                            context.x,
                            context.y,
                            context.xIndex,
                            context.xIndex + numberOfPoints
                    );

            context.xIndex += numberOfPoints;

            LinearRing ring =
                    GF.createLinearRing(seq);

            /*
             * Positive part = exterior ring
             * Negative part = interior ring / hole
             */
            if (parts[i] >= 0) {

                if (shell != null) {
                    throw new IllegalArgumentException(
                            "Polygon contains multiple exterior rings"
                    );
                }

                shell = ring;

            } else {

                holes.add(ring);
            }
        }

        if (shell == null) {
            throw new IllegalArgumentException(
                    "Polygon has no exterior ring"
            );
        }

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
            DecodeContext context) {

        int[] parts =
                readVariableParts(context);

        /*
         * [0,-1] = EMPTY MULTIPOLYGON
         */
        if (isEmptyMarker(parts)) {
            return GF.createMultiPolygon();
        }

        if (parts.length < 2) {
            throw new IllegalArgumentException(
                    "Invalid MultiPolygon parts"
            );
        }

        int endpoint =
                parts[parts.length - 1];

        if (endpoint < 0) {
            throw new IllegalArgumentException(
                    "Invalid MultiPolygon endpoint: "
                            + endpoint
            );
        }

        List<Polygon> polygons =
                new ArrayList<>();

        LinearRing currentShell = null;

        List<LinearRing> currentHoles =
                new ArrayList<>();

        int numberOfParts =
                parts.length - 1;

        for (int i = 0; i < numberOfParts; i++) {

            int start =
                    Math.abs(parts[i]);

            int end =
                    (i + 1 < numberOfParts)
                            ? Math.abs(parts[i + 1]) - 1
                            : endpoint;

            if (end < start) {
                throw new IllegalArgumentException(
                        "Invalid MultiPolygon part: start="
                                + start
                                + ", end="
                                + end
                );
            }

            int numberOfPoints =
                    end - start + 1;

            checkCoordinateRange(
                    context,
                    numberOfPoints
            );

            CoordinateSequence seq =
                    createSequence(
                            context.csf,
                            context.x,
                            context.y,
                            context.xIndex,
                            context.xIndex + numberOfPoints
                    );

            context.xIndex += numberOfPoints;

            LinearRing ring =
                    GF.createLinearRing(seq);

            /*
             * Positive part = exterior ring.
             * A new exterior ring starts a new polygon.
             */
            if (parts[i] >= 0) {

                if (currentShell != null) {

                    polygons.add(
                            GF.createPolygon(
                                    currentShell,
                                    currentHoles.toArray(
                                            new LinearRing[0]
                                    )
                            )
                    );
                }

                currentShell = ring;

                currentHoles.clear();

            } else {

                /*
                 * Negative part = hole.
                 */
                if (currentShell == null) {
                    throw new IllegalArgumentException(
                            "Hole appears before exterior ring"
                    );
                }

                currentHoles.add(ring);
            }
        }

        /*
         * Add final polygon.
         */
        if (currentShell == null) {
            throw new IllegalArgumentException(
                    "MultiPolygon has no exterior ring"
            );
        }

        polygons.add(
                GF.createPolygon(
                        currentShell,
                        currentHoles.toArray(
                                new LinearRing[0]
                        )
                )
        );

        return GF.createMultiPolygon(
                polygons.toArray(
                        new Polygon[0]
                )
        );
    }


    // =========================================================
    // FIXED PARTS
    // =========================================================

    private static int[] readFixedParts(
            DecodeContext context,
            int count) {

        if (context.partsIndex + count >
                context.parts.length) {

            throw new IllegalArgumentException(
                    "Not enough parts values"
            );
        }

        int[] result =
                new int[count];

        for (int i = 0; i < count; i++) {

            result[i] =
                    context.parts[
                            context.partsIndex++
                            ];
        }

        return result;
    }


    // =========================================================
    // VARIABLE PARTS
    // =========================================================

    /**
     * Reads the parts belonging to exactly one variable-length
     * geometry.
     *
     * Each geometry starts with local offset 0.
     *
     * Examples:
     *
     *   Polygon:
     *       [0, -5, 10]
     *
     *   MultiLineString:
     *       [0, 5, 12]
     *
     *   Empty geometry:
     *       [0, -1]
     *
     * The difficult case is when the endpoint of a geometry
     * is zero and the next geometry consequently also starts
     * with zero.
     *
     * Example:
     *
     *       [0, 0, 0, 4, ...]
     *
     * The first two zeros belong to the current geometry:
     *
     *       [0, 0]
     *
     * and the third zero starts the next geometry.
     *
     * Therefore the first zero after the initial zero must
     * be consumed as the endpoint before searching for the
     * next geometry boundary.
     */
    private static int[] readVariableParts(
            DecodeContext context) {

        if (context.partsIndex >=
                context.parts.length) {

            throw new IllegalArgumentException(
                    "Missing parts for variable-length geometry"
            );
        }

        int start =
                context.partsIndex;

        /*
         * Every geometry must begin with local offset 0.
         */
        if (context.parts[start] != 0) {

            throw new IllegalArgumentException(
                    "Variable geometry parts must start with 0, found: "
                            + context.parts[start]
            );
        }

        /*
         * Consume the first 0.
         */
        context.partsIndex++;

        /*
         * -----------------------------------------------------
         * EMPTY GEOMETRY
         * -----------------------------------------------------
         *
         * [0,-1]
         */
        if (context.partsIndex < context.parts.length
                && context.parts[context.partsIndex] == -1) {

            context.partsIndex++;

            return new int[]{
                    0,
                    -1
            };
        }

        /*
         * -----------------------------------------------------
         * ZERO ENDPOINT
         * -----------------------------------------------------
         *
         * If the next value is zero, it is the endpoint of
         * the current geometry.
         *
         * Example:
         *
         *     [0,0,0,4,...]
         *
         *     current geometry = [0,0]
         *     next geometry    = [0,4,...]
         */
        if (context.partsIndex < context.parts.length
                && context.parts[context.partsIndex] == 0) {

            context.partsIndex++;
        }

        /*
         * -----------------------------------------------------
         * FIND NEXT GEOMETRY
         * -----------------------------------------------------
         *
         * All remaining non-zero values belong to the current
         * geometry. The next zero marks the beginning of the
         * next geometry.
         */
        while (context.partsIndex <
                context.parts.length) {

            if (context.parts[context.partsIndex] == 0) {
                break;
            }

            context.partsIndex++;
        }

        int end =
                context.partsIndex;

        int[] result =
                new int[end - start];

        System.arraycopy(
                context.parts,
                start,
                result,
                0,
                result.length
        );

        return result;
    }


    private static boolean isEmptyMarker(
            int[] parts) {

        return parts.length == 2
                && parts[0] == 0
                && parts[1] == -1;
    }


    // =========================================================
    // COORDINATE SEQUENCE
    // =========================================================

    private static CoordinateSequence createSequence(
            CoordinateSequenceFactory csf,
            double[] x,
            double[] y,
            int start,
            int end) {

        if (start < 0
                || end < start
                || end > x.length
                || end > y.length) {

            throw new IllegalArgumentException(
                    "Invalid coordinate range: "
                            + start + " - " + end
            );
        }

        int size =
                end - start;

        CoordinateSequence seq =
                csf.create(size, 2);

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


    private static void checkCoordinateRange(
            DecodeContext context,
            int numberOfPoints) {

        if (numberOfPoints < 0
                || context.xIndex + numberOfPoints
                > context.x.length
                || context.xIndex + numberOfPoints
                > context.y.length) {

            throw new IllegalArgumentException(
                    "Invalid coordinate range: index="
                            + context.xIndex
                            + ", count="
                            + numberOfPoints
                            + ", xLength="
                            + context.x.length
                            + ", yLength="
                            + context.y.length
            );
        }
    }


    // =========================================================
    // ARRAY CONVERSION
    // =========================================================

    private static double[] toDoubleArray(
            List<Double> list) {

        if (list == null || list.isEmpty()) {
            return new double[0];
        }

        double[] arr =
                new double[list.size()];

        for (int i = 0; i < arr.length; i++) {

            Double value =
                    list.get(i);

            if (value == null) {
                throw new IllegalArgumentException(
                        "X/Y array contains null at index " + i
                );
            }

            arr[i] = value;
        }

        return arr;
    }


    private static int[] toIntArray(
            List<Integer> list) {

        if (list == null || list.isEmpty()) {
            return new int[0];
        }

        int[] arr =
                new int[list.size()];

        for (int i = 0; i < arr.length; i++) {

            Integer value =
                    list.get(i);

            if (value == null) {
                throw new IllegalArgumentException(
                        "Integer array contains null at index " + i
                );
            }

            arr[i] = value;
        }

        return arr;
    }
}