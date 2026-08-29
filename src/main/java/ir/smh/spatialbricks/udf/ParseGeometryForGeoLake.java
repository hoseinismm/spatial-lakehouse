package ir.smh.spatialbricks.udf;

import org.locationtech.jts.geom.*;

import java.util.*;

public class ParseGeometryForGeoLake {

    public static Map<String, Object> parseGeometry(Geometry geometry) {

        if (geometry == null) {
            return null;
        }

        List<Double> xList = new ArrayList<>();
        List<Double> yList = new ArrayList<>();

        /*
         * coordinateRanges:
         * End offset of each Point / LineString / LinearRing.
         *
         * Example:
         * LineString (4 coordinates)
         * coordinateRanges = [4]
         *
         * MultiLineString with 2 and 3 coordinates:
         * coordinateRanges = [2, 5]
         */
        List<Integer> coordinateRanges = new ArrayList<>();

        /*
         * lineRanges:
         *
         * Used for Polygon / MultiPolygon.
         *
         * It identifies which coordinateRanges belong to
         * each Polygon.
         *
         * Example:
         * MultiPolygon with two polygons:
         * lineRanges = [1, 2]
         */
        List<Integer> lineRanges = new ArrayList<>();

        /*
         * geometryRanges / geometryTypes:
         *
         * Used only for GeometryCollection.
         */
        List<Integer> geometryRanges = new ArrayList<>();
        List<Integer> geometryTypes = new ArrayList<>();

        parseGeometryRecursive(
                geometry,
                xList,
                yList,
                coordinateRanges,
                lineRanges,
                geometryRanges,
                geometryTypes
        );

        Map<String, Object> geomMap = new HashMap<>();

        /*
         * GeoLake:
         *
         * 1 = Point
         * 2 = LineString
         * 3 = Polygon
         * 4 = MultiPoint
         * 5 = MultiLineString
         * 6 = MultiPolygon
         * 7 = GeometryCollection
         */
        geomMap.put(
                "type",
                geometryTypeToInt(geometry)
        );

        geomMap.put(
                "x",
                xList.stream()
                        .mapToDouble(Double::doubleValue)
                        .toArray()
        );

        geomMap.put(
                "y",
                yList.stream()
                        .mapToDouble(Double::doubleValue)
                        .toArray()
        );

        geomMap.put(
                "coordinateRanges",
                coordinateRanges.toArray(new Integer[0])
        );

        geomMap.put(
                "lineRanges",
                lineRanges.toArray(new Integer[0])
        );

        geomMap.put(
                "geometryRanges",
                geometryRanges.toArray(new Integer[0])
        );

        geomMap.put(
                "geometryTypes",
                geometryTypes.toArray(new Integer[0])
        );

        return geomMap;
    }


    private static void parseGeometryRecursive(
            Geometry geometry,
            List<Double> xList,
            List<Double> yList,
            List<Integer> coordinateRanges,
            List<Integer> lineRanges,
            List<Integer> geometryRanges,
            List<Integer> geometryTypes) {

        int type = geometryTypeToInt(geometry);

        if (type == -1) {
            throw new IllegalArgumentException(
                    "Unsupported geometry type: "
                            + geometry.getGeometryType()
            );
        }


        /*
         * -----------------------------------------------------
         * Point
         * -----------------------------------------------------
         */
        if (geometry instanceof Point p) {

            CoordinateSequence seq =
                    p.getCoordinateSequence();

            /*
             * Empty Point
             *
             * GeoLake has no coordinate range for an empty
             * Point because there is no coordinate value.
             */
            if (seq.size() == 0) {
                return;
            }

            xList.add(seq.getX(0));
            yList.add(seq.getY(0));

            /*
             * Point does not require coordinateRanges
             * according to GeoLake.
             */
            return;
        }


        /*
         * -----------------------------------------------------
         * MultiPoint
         * -----------------------------------------------------
         */
        if (geometry instanceof MultiPoint mp) {

            /*
             * MultiPoint does not require coordinateRanges.
             *
             * Each Point contains exactly one coordinate,
             * so the position of each point can be inferred
             * directly from x/y.
             */
            for (int i = 0;
                 i < mp.getNumGeometries();
                 i++) {

                Point p =
                        (Point) mp.getGeometryN(i);

                CoordinateSequence seq =
                        p.getCoordinateSequence();

                if (seq.size() == 0) {
                    throw new IllegalArgumentException(
                            "Empty Point inside MultiPoint"
                    );
                }

                xList.add(seq.getX(0));
                yList.add(seq.getY(0));
            }

            return;
        }


        /*
         * -----------------------------------------------------
         * LineString
         * -----------------------------------------------------
         */
        if (geometry instanceof LineString ls) {

            CoordinateSequence seq =
                    ls.getCoordinateSequence();

            for (int i = 0;
                 i < seq.size();
                 i++) {

                xList.add(seq.getX(i));
                yList.add(seq.getY(i));
            }

            /*
             * GeoLake uses 1-based coordinate offsets.
             *
             * Example:
             *
             * LINESTRING (1 1, 2 3, 3 1, 4 2)
             *
             * coordinateRanges = [4]
             */
            if (seq.size() > 0) {
                coordinateRanges.add(
                        xList.size()
                );
            }

            return;
        }


        /*
         * -----------------------------------------------------
         * MultiLineString
         * -----------------------------------------------------
         */
        if (geometry instanceof MultiLineString ml) {

            for (int i = 0;
                 i < ml.getNumGeometries();
                 i++) {

                LineString line =
                        (LineString) ml.getGeometryN(i);

                CoordinateSequence seq =
                        line.getCoordinateSequence();

                if (seq.size() == 0) {
                    throw new IllegalArgumentException(
                            "Empty LineString inside MultiLineString"
                    );
                }

                for (int j = 0;
                     j < seq.size();
                     j++) {

                    xList.add(seq.getX(j));
                    yList.add(seq.getY(j));
                }

                /*
                 * Endpoint of this LineString.
                 */
                coordinateRanges.add(
                        xList.size()
                );
            }

            return;
        }


        /*
         * -----------------------------------------------------
         * Polygon
         * -----------------------------------------------------
         */
        if (geometry instanceof Polygon poly) {

            if (poly.isEmpty()) {
                return;
            }

            /*
             * A Polygon consists of:
             *
             * 1 exterior LinearRing
             * 0..n interior LinearRings
             *
             * Each LinearRing gets one coordinateRange.
             */

            int firstLineRange =
                    coordinateRanges.size() + 1;


            /*
             * Exterior ring
             */
            LinearRing exterior =
                    poly.getExteriorRing();

            addRing(
                    exterior,
                    xList,
                    yList,
                    coordinateRanges
            );


            /*
             * Interior rings / holes
             */
            for (int i = 0;
                 i < poly.getNumInteriorRing();
                 i++) {

                LinearRing hole =
                        poly.getInteriorRingN(i);

                addRing(
                        hole,
                        xList,
                        yList,
                        coordinateRanges
                );
            }


            /*
             * Polygon occupies all coordinateRanges
             * added since firstLineRange.
             *
             * GeoLake lineRanges use 1-based offsets.
             */
            int lastLineRange =
                    coordinateRanges.size();

            /*
             * For a single Polygon, lineRanges represents
             * the range of its LinearRings.
             *
             * Example:
             *
             * Polygon with exterior + hole:
             *
             * coordinateRanges = [6, 10]
             * lineRanges       = [1, 2]
             */
            lineRanges.add(firstLineRange);
            lineRanges.add(lastLineRange);

            return;
        }


        /*
         * -----------------------------------------------------
         * MultiPolygon
         * -----------------------------------------------------
         */
        if (geometry instanceof MultiPolygon mp) {

            if (mp.getNumGeometries() == 0) {
                return;
            }

            /*
             * Each Polygon contributes one or more
             * LinearRings to coordinateRanges.
             *
             * lineRanges identifies the start/end range
             * of LinearRings for each Polygon.
             */
            for (int i = 0;
                 i < mp.getNumGeometries();
                 i++) {

                Polygon poly =
                        (Polygon) mp.getGeometryN(i);

                if (poly.isEmpty()) {
                    throw new IllegalArgumentException(
                            "Empty Polygon inside MultiPolygon"
                    );
                }

                int polygonStart =
                        coordinateRanges.size() + 1;


                /*
                 * Exterior ring
                 */
                addRing(
                        poly.getExteriorRing(),
                        xList,
                        yList,
                        coordinateRanges
                );


                /*
                 * Holes
                 */
                for (int j = 0;
                     j < poly.getNumInteriorRing();
                     j++) {

                    addRing(
                            poly.getInteriorRingN(j),
                            xList,
                            yList,
                            coordinateRanges
                    );
                }


                int polygonEnd =
                        coordinateRanges.size();

                /*
                 * Store the LinearRing range of this Polygon.
                 */
                lineRanges.add(polygonStart);
                lineRanges.add(polygonEnd);
            }

            return;
        }


        /*
         * -----------------------------------------------------
         * GeometryCollection
         * -----------------------------------------------------
         */
        if (geometry instanceof GeometryCollection gc) {

            /*
             * GeoLake supports GeometryCollection but NOT
             * nested GeometryCollection.
             *
             * Therefore, children must be simple geometries.
             */

            for (int i = 0;
                 i < gc.getNumGeometries();
                 i++) {

                Geometry child =
                        gc.getGeometryN(i);

                if (child instanceof GeometryCollection) {
                    throw new IllegalArgumentException(
                            "Nested GeometryCollection is not supported by GeoLake"
                    );
                }

                /*
                 * Save the position of this geometry
                 * in the x/y coordinate arrays.
                 *
                 * GeoLake geometryRanges are 1-based.
                 */
                geometryRanges.add(
                        xList.size() + 1
                );

                geometryTypes.add(
                        geometryTypeToInt(child)
                );


                /*
                 * Encode child geometry.
                 */
                parseGeometryRecursive(
                        child,
                        xList,
                        yList,
                        coordinateRanges,
                        lineRanges,
                        geometryRanges,
                        geometryTypes
                );
            }

            return;
        }


        throw new IllegalArgumentException(
                "Unsupported geometry: "
                        + geometry.getGeometryType()
        );
    }


    /**
     * Adds one LinearRing to the coordinate arrays
     * and records its endpoint in coordinateRanges.
     */
    private static void addRing(
            LinearRing ring,
            List<Double> xList,
            List<Double> yList,
            List<Integer> coordinateRanges) {

        CoordinateSequence seq =
                ring.getCoordinateSequence();

        if (seq.size() == 0) {
            throw new IllegalArgumentException(
                    "Empty LinearRing"
            );
        }

        for (int i = 0;
             i < seq.size();
             i++) {

            xList.add(seq.getX(i));
            yList.add(seq.getY(i));
        }

        /*
         * 1-based endpoint.
         */
        coordinateRanges.add(
                xList.size()
        );
    }


    /**
     * Converts JTS geometry type to GeoLake integer type.

     * 1 = Point
     * 2 = LineString
     * 3 = Polygon
     * 4 = MultiPoint
     * 5 = MultiLineString
     * 6 = MultiPolygon
     * 7 = GeometryCollection
     */
    private static int geometryTypeToInt(
            Geometry geometry) {

        if (geometry instanceof Point) {
            return 1;
        }

        if (geometry instanceof LineString) {
            return 2;
        }

        if (geometry instanceof Polygon) {
            return 3;
        }

        if (geometry instanceof MultiPoint) {
            return 4;
        }

        if (geometry instanceof MultiLineString) {
            return 5;
        }

        if (geometry instanceof MultiPolygon) {
            return 6;
        }

        if (geometry instanceof GeometryCollection) {
            return 7;
        }

        return -1;
    }
}