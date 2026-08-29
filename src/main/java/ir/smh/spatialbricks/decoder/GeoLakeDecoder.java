package ir.smh.spatialbricks.decoder;

import org.apache.spark.sql.Row;
import org.locationtech.jts.geom.*;

import java.util.ArrayList;
import java.util.List;

public class GeoLakeDecoder {

    private static final GeometryFactory GF =
            new GeometryFactory();

    // =========================================================
    // ENTRY POINT
    // =========================================================

    public static Geometry geometryToJTS(Row row) {

        if (row == null) {
            return null;
        }

        int type = row.getAs("type");

        List<Double> x =
                row.getList(row.fieldIndex("x"));

        List<Double> y =
                row.getList(row.fieldIndex("y"));

        List<Integer> coordinateRanges =
                row.getList(row.fieldIndex("coordinateRanges"));

        List<Integer> lineRanges =
                row.getList(row.fieldIndex("lineRanges"));

        List<Integer> geometryRanges =
                row.getList(row.fieldIndex("geometryRanges"));

        List<Integer> geometryTypes =
                row.getList(row.fieldIndex("geometryTypes"));

        if (x == null) x = new ArrayList<>();
        if (y == null) y = new ArrayList<>();
        if (coordinateRanges == null)
            coordinateRanges = new ArrayList<>();
        if (lineRanges == null)
            lineRanges = new ArrayList<>();
        if (geometryRanges == null)
            geometryRanges = new ArrayList<>();
        if (geometryTypes == null)
            geometryTypes = new ArrayList<>();

        if (x.size() != y.size()) {
            throw new IllegalArgumentException(
                    "x/y size mismatch: x="
                            + x.size()
                            + ", y="
                            + y.size()
            );
        }

        DecodeContext ctx =
                new DecodeContext(
                        x,
                        y,
                        coordinateRanges,
                        lineRanges,
                        geometryRanges,
                        geometryTypes
                );

        return decodeGeometry(type, ctx);
    }


    // =========================================================
    // CONTEXT
    // =========================================================

    private static class DecodeContext {

        final List<Double> x;
        final List<Double> y;

        final List<Integer> coordinateRanges;
        final List<Integer> lineRanges;

        final List<Integer> geometryRanges;
        final List<Integer> geometryTypes;

        DecodeContext(
                List<Double> x,
                List<Double> y,
                List<Integer> coordinateRanges,
                List<Integer> lineRanges,
                List<Integer> geometryRanges,
                List<Integer> geometryTypes) {

            this.x = x;
            this.y = y;
            this.coordinateRanges = coordinateRanges;
            this.lineRanges = lineRanges;
            this.geometryRanges = geometryRanges;
            this.geometryTypes = geometryTypes;
        }
    }


    // =========================================================
    // GEOMETRY DISPATCH
    // =========================================================

    private static Geometry decodeGeometry(
            int type,
            DecodeContext ctx) {

        switch (type) {

            case 1:
                return decodePoint(ctx);

            case 2:
                return decodeLineString(ctx);

            case 3:
                return decodePolygon(ctx);

            case 4:
                return decodeMultiPoint(ctx);

            case 5:
                return decodeMultiLineString(ctx);

            case 6:
                return decodeMultiPolygon(ctx);

            case 7:
                return decodeGeometryCollection(ctx);

            default:
                throw new IllegalArgumentException(
                        "Unsupported GeoLake geometry type: "
                                + type
                );
        }
    }


    // =========================================================
    // POINT
    // =========================================================

    private static Point decodePoint(
            DecodeContext ctx) {

        if (ctx.x.isEmpty()) {
            return GF.createPoint();
        }

        Coordinate coordinate =
                new Coordinate(
                        ctx.x.get(0),
                        ctx.y.get(0)
                );

        return GF.createPoint(coordinate);
    }


    // =========================================================
    // MULTIPOINT
    // =========================================================

    private static MultiPoint decodeMultiPoint(
            DecodeContext ctx) {

        Point[] points =
                new Point[ctx.x.size()];

        for (int i = 0; i < ctx.x.size(); i++) {

            points[i] =
                    GF.createPoint(
                            new Coordinate(
                                    ctx.x.get(i),
                                    ctx.y.get(i)
                            )
                    );
        }

        return GF.createMultiPoint(points);
    }


    // =========================================================
    // LINESTRING
    // =========================================================

    private static LineString decodeLineString(
            DecodeContext ctx) {

        if (ctx.x.isEmpty()) {
            return GF.createLineString();
        }

        Coordinate[] coordinates =
                coordinates(
                        ctx,
                        0,
                        ctx.x.size()
                );

        return GF.createLineString(coordinates);
    }


    // =========================================================
    // MULTILINESTRING
    // =========================================================

    private static MultiLineString decodeMultiLineString(
            DecodeContext ctx) {

        List<LineString> lines =
                new ArrayList<>();

        int start = 0;

        for (Integer range :
                ctx.coordinateRanges) {

            int end = range;

            validateRange(
                    start,
                    end,
                    ctx.x.size()
            );

            Coordinate[] coordinates =
                    coordinates(
                            ctx,
                            start,
                            end
                    );

            lines.add(
                    GF.createLineString(coordinates)
            );

            start = end;
        }

        return GF.createMultiLineString(
                lines.toArray(new LineString[0])
        );
    }


    // =========================================================
    // POLYGON
    // =========================================================

    private static Polygon decodePolygon(
            DecodeContext ctx) {

        if (ctx.x.isEmpty()) {
            return GF.createPolygon();
        }

        if (ctx.lineRanges.size() < 2) {
            throw new IllegalArgumentException(
                    "Invalid Polygon lineRanges"
            );
        }

        int startRange =
                ctx.lineRanges.get(0);

        int endRange =
                ctx.lineRanges.get(1);

        return buildPolygon(
                ctx,
                startRange,
                endRange
        );
    }


    // =========================================================
    // MULTIPOLYGON
    // =========================================================

    private static MultiPolygon decodeMultiPolygon(
            DecodeContext ctx) {

        if (ctx.lineRanges.isEmpty()) {
            return GF.createMultiPolygon();
        }

        if (ctx.lineRanges.size() % 2 != 0) {
            throw new IllegalArgumentException(
                    "Invalid MultiPolygon lineRanges: "
                            + ctx.lineRanges
            );
        }

        List<Polygon> polygons =
                new ArrayList<>();

        for (int i = 0;
             i < ctx.lineRanges.size();
             i += 2) {

            int startRange =
                    ctx.lineRanges.get(i);

            int endRange =
                    ctx.lineRanges.get(i + 1);

            polygons.add(
                    buildPolygon(
                            ctx,
                            startRange,
                            endRange
                    )
            );
        }

        return GF.createMultiPolygon(
                polygons.toArray(new Polygon[0])
        );
    }


    // =========================================================
    // BUILD POLYGON
    // =========================================================

    private static Polygon buildPolygon(
            DecodeContext ctx,
            int startRange,
            int endRange) {

        /*
         * GeoLake ranges are 1-based.
         *
         * coordinateRanges contains cumulative
         * coordinate endpoints.
         */

        if (startRange < 1 ||
                endRange < startRange ||
                endRange > ctx.coordinateRanges.size()) {

            throw new IllegalArgumentException(
                    "Invalid polygon range: ["
                            + startRange
                            + ", "
                            + endRange
                            + "]"
            );
        }

        LinearRing shell =
                buildRing(
                        ctx,
                        startRange - 1
                );

        LinearRing[] holes =
                new LinearRing[
                        endRange - startRange
                        ];

        for (int i = startRange;
             i < endRange;
             i++) {

            holes[i - startRange] =
                    buildRing(
                            ctx,
                            i
                    );
        }

        return GF.createPolygon(
                shell,
                holes
        );
    }


    // =========================================================
    // LINEAR RING
    // =========================================================

    private static LinearRing buildRing(
            DecodeContext ctx,
            int rangeIndex) {

        int start =
                rangeIndex == 0
                        ? 0
                        : ctx.coordinateRanges
                        .get(rangeIndex - 1);

        int end =
                ctx.coordinateRanges
                        .get(rangeIndex);

        validateRange(
                start,
                end,
                ctx.x.size()
        );

        Coordinate[] coordinates =
                coordinates(
                        ctx,
                        start,
                        end
                );

        return GF.createLinearRing(
                coordinates
        );
    }


    // =========================================================
    // GEOMETRY COLLECTION
    // =========================================================

    private static GeometryCollection
    decodeGeometryCollection(
            DecodeContext ctx) {

        if (ctx.geometryTypes.size() !=
                ctx.geometryRanges.size()) {

            throw new IllegalArgumentException(
                    "geometryTypes and geometryRanges "
                            + "size mismatch: types="
                            + ctx.geometryTypes.size()
                            + ", ranges="
                            + ctx.geometryRanges.size()
            );
        }

        List<Geometry> geometries =
                new ArrayList<>();

        /*
         * geometryRanges contains the starting
         * coordinate position of each child geometry.
         *
         * The ranges are 1-based.
         */

        for (int i = 0;
             i < ctx.geometryTypes.size();
             i++) {

            int type =
                    ctx.geometryTypes.get(i);

            int start =
                    ctx.geometryRanges.get(i) - 1;

            int end;

            if (i + 1 <
                    ctx.geometryRanges.size()) {

                end =
                        ctx.geometryRanges
                                .get(i + 1) - 1;

            } else {

                end =
                        ctx.x.size();
            }

            /*
             * Decode child using its own
             * coordinate section.
             *
             * GeometryCollection children may be
             * different geometry types.
             */

            DecodeContext childCtx =
                    new DecodeContext(
                            ctx.x.subList(start, end),
                            ctx.y.subList(start, end),
                            new ArrayList<>(),
                            new ArrayList<>(),
                            new ArrayList<>(),
                            new ArrayList<>()
                    );

            /*
             * For Point and MultiPoint the above
             * coordinate slicing is sufficient.
             *
             * For LineString / Polygon / Multi*
             * we need to reconstruct ranges locally.
             */

            Geometry child =
                    decodeCollectionChild(
                            type,
                            ctx,
                            i
                    );

            geometries.add(child);
        }

        return GF.createGeometryCollection(
                geometries.toArray(new Geometry[0])
        );
    }


    // =========================================================
    // GEOMETRY COLLECTION CHILD
    // =========================================================

    private static Geometry decodeCollectionChild(
            int type,
            DecodeContext ctx,
            int index) {

        int start =
                ctx.geometryRanges.get(index) - 1;

        int end =
                index + 1 <
                        ctx.geometryRanges.size()
                        ? ctx.geometryRanges
                        .get(index + 1) - 1
                        : ctx.x.size();

        /*
         * Point / MultiPoint do not use
         * coordinateRanges.
         */

        if (type == 1) {

            if (start >= end) {
                return GF.createPoint();
            }

            return GF.createPoint(
                    new Coordinate(
                            ctx.x.get(start),
                            ctx.y.get(start)
                    )
            );
        }

        if (type == 4) {

            Point[] points =
                    new Point[end - start];

            for (int i = start;
                 i < end;
                 i++) {

                points[i - start] =
                        GF.createPoint(
                                new Coordinate(
                                        ctx.x.get(i),
                                        ctx.y.get(i)
                                )
                        );
            }

            return GF.createMultiPoint(points);
        }

        /*
         * For complex geometries, determine the
         * coordinateRanges belonging to this child.
         */

        int firstCoordRange =
                findFirstCoordinateRange(
                        ctx.coordinateRanges,
                        start
                );

        int lastCoordRange =
                findLastCoordinateRange(
                        ctx.coordinateRanges,
                        end
                );

        List<Integer> localRanges =
                new ArrayList<>();

        int previous = start;

        for (int i = firstCoordRange;
             i < lastCoordRange;
             i++) {

            int globalEnd =
                    ctx.coordinateRanges.get(i);

            if (globalEnd <= end) {

                localRanges.add(
                        globalEnd - start
                );

                previous = globalEnd;
            }
        }

        DecodeContext childCtx =
                new DecodeContext(
                        ctx.x.subList(start, end),
                        ctx.y.subList(start, end),
                        localRanges,
                        new ArrayList<>(),
                        new ArrayList<>(),
                        new ArrayList<>()
                );

        /*
         * LineString
         */

        if (type == 2) {
            return decodeLineString(childCtx);
        }

        /*
         * MultiLineString
         */

        if (type == 5) {
            return decodeMultiLineString(childCtx);
        }

        /*
         * Polygon
         */

        if (type == 3) {

            if (localRanges.isEmpty()) {
                return GF.createPolygon();
            }

            childCtx.lineRanges.add(1);
            childCtx.lineRanges.add(
                    localRanges.size()
            );

            return decodePolygon(childCtx);
        }

        /*
         * MultiPolygon
         */

        if (type == 6) {

            /*
             * Without explicit polygon boundaries
             * inside geometryRanges, reconstruction
             * of MultiPolygon is ambiguous.
             *
             * GeoLake's geometryRanges/geometryTypes
             * representation therefore cannot safely
             * reconstruct arbitrary MultiPolygon
             * children inside GeometryCollection unless
             * polygon boundaries are represented.
             */

            throw new IllegalArgumentException(
                    "MultiPolygon inside GeometryCollection "
                            + "cannot be safely reconstructed "
                            + "from GeoLake geometryRanges/geometryTypes alone"
            );
        }

        throw new IllegalArgumentException(
                "Unsupported GeometryCollection child type: "
                        + type
        );
    }


    // =========================================================
    // COORDINATES
    // =========================================================

    private static Coordinate[] coordinates(
            DecodeContext ctx,
            int start,
            int end) {

        validateRange(
                start,
                end,
                ctx.x.size()
        );

        Coordinate[] coordinates =
                new Coordinate[end - start];

        for (int i = start;
             i < end;
             i++) {

            coordinates[i - start] =
                    new Coordinate(
                            ctx.x.get(i),
                            ctx.y.get(i)
                    );
        }

        return coordinates;
    }


    // =========================================================
    // RANGE VALIDATION
    // =========================================================

    private static void validateRange(
            int start,
            int end,
            int size) {

        if (start < 0 ||
                end < start ||
                end > size) {

            throw new IllegalArgumentException(
                    "Invalid coordinate range: ["
                            + start
                            + ", "
                            + end
                            + "] for size "
                            + size
            );
        }
    }


    // =========================================================
    // RANGE SEARCH
    // =========================================================

    private static int findFirstCoordinateRange(
            List<Integer> ranges,
            int start) {

        for (int i = 0; i < ranges.size(); i++) {

            int end = ranges.get(i);

            if (end > start) {
                return i;
            }
        }

        return ranges.size();
    }


    private static int findLastCoordinateRange(
            List<Integer> ranges,
            int end) {

        for (int i = 0; i < ranges.size(); i++) {

            if (ranges.get(i) >= end) {
                return i + 1;
            }
        }

        return ranges.size();
    }
}