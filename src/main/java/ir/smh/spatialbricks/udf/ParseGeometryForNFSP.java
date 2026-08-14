package ir.smh.spatialbricks.udf;

import org.locationtech.jts.geom.*;

import java.util.*;

public class ParseGeometryForNFSP {

    public static Map<String, Object> parseGeometry(Geometry geometry) {

        if (geometry == null) {
            return null;
        }

        List<Double> xList = new ArrayList<>();
        List<Double> yList = new ArrayList<>();
        List<Integer> partsList = new ArrayList<>();
        List<Integer> geometriesList = new ArrayList<>();

        parseGeometryRecursive(
                geometry,
                xList,
                yList,
                partsList,
                geometriesList
        );

        Map<String, Object> geomMap = new HashMap<>();

        /*
         * type مربوط به ریشه است.
         *
         * برای GeometryCollection مقدار 0 خواهد بود.
         */
        geomMap.put("type", geometryTypeToInt(geometry));

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
                "parts",
                partsList.toArray(new Integer[0])
        );

        geomMap.put(
                "geometries",
                geometriesList.toArray(new Integer[0])
        );

        return geomMap;
    }


    /**
     * Recursively encodes a geometry into NFSP.
     *
     * geometries:
     *
     * 0 = start GeometryCollection
     * 1..6 = simple geometry
     * 7 = end GeometryCollection
     */
    private static void parseGeometryRecursive(
            Geometry geometry,
            List<Double> xList,
            List<Double> yList,
            List<Integer> partsList,
            List<Integer> geometriesList) {

        int type = geometryTypeToInt(geometry);

        if (type ==-1) {
            throw new IllegalArgumentException(
                    "Unsupported geometry type: "
                            + geometry.getGeometryType()
            );
        }

        if (type > 0 && type < 7)
            geometriesList.add(type);

        int startIndex = xList.size();


        /*
         * -----------------------------------------------------
         * Point
         * -----------------------------------------------------
         */
        if (geometry instanceof Point p) {

            CoordinateSequence seq =
                    p.getCoordinateSequence();

            if (seq.size() == 0) {
                partsList.add(0);
                partsList.add(-1);
                return;
            }

            xList.add(seq.getX(0));
            yList.add(seq.getY(0));

            partsList.add(0);
            partsList.add(0);

            return;
        }


        /*
         * -----------------------------------------------------
         * MultiPoint
         * -----------------------------------------------------
         */
        if (geometry instanceof MultiPoint mp) {

            int numberOfPoints = mp.getNumGeometries();

            for (int i = 0; i < numberOfPoints; i++) {

                Point p =
                        (Point) mp.getGeometryN(i);

                CoordinateSequence seq =
                        p.getCoordinateSequence();

                if (seq.size() == 0) {
                    throw new IllegalArgumentException(
                            "Empty Point inside MultiPoint");
                }

                xList.add(seq.getX(0));
                yList.add(seq.getY(0));
            }

            int endpoint =
                    xList.size() - startIndex - 1;

            partsList.add(0);
            partsList.add(endpoint);

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

            int size = seq.size();



            /*
             * LineString has no internal parts in FSP,
             * but NFSP needs its geometry boundary.
             *
             * Therefore:
             *
             * parts = [0, lastIndex]
             */
            partsList.add(0);

            for (int i = 0; i < size; i++) {

                xList.add(seq.getX(i));
                yList.add(seq.getY(i));
            }

            int endpoint =
                    xList.size() - startIndex - 1;

            partsList.add(endpoint);

            return;
        }


        /*
         * -----------------------------------------------------
         * MultiLineString
         * -----------------------------------------------------
         */
        if (geometry instanceof MultiLineString ml) {

            int numberOfLines = ml.getNumGeometries();

            if (numberOfLines == 0) {
                partsList.add(0);
            }


            for (int i = 0; i < numberOfLines; i++) {

                LineString line =
                        (LineString) ml.getGeometryN(i);

                CoordinateSequence seq =
                        line.getCoordinateSequence();

                int size = seq.size();

                if (size == 0) {
                    throw new IllegalArgumentException(
                            "Empty LineString inside MultiLineString");
                }

                int partStart =
                        xList.size() - startIndex;

                partsList.add(partStart);

                for (int j = 0; j < size; j++) {

                    xList.add(seq.getX(j));
                    yList.add(seq.getY(j));
                }
            }

            int endpoint =
                    xList.size() - startIndex - 1;

            partsList.add(endpoint);

            return;
        }


        /*
         * -----------------------------------------------------
         * Polygon
         * -----------------------------------------------------
         */
        if (geometry instanceof Polygon poly) {

            if (poly.isEmpty()) {
                partsList.add(0);
                partsList.add(-1);
                return;
            }

            /*
             * Exterior ring
             */
            LinearRing exterior =
                    poly.getExteriorRing();

            CoordinateSequence ext =
                    exterior.getCoordinateSequence();

            partsList.add(0);

            for (int i = 0; i < ext.size(); i++) {

                xList.add(ext.getX(i));
                yList.add(ext.getY(i));
            }


            /*
             * Interior rings / holes
             */
            for (int i = 0;
                 i < poly.getNumInteriorRing();
                 i++) {

                LinearRing hole =
                        poly.getInteriorRingN(i);

                CoordinateSequence seq =
                        hole.getCoordinateSequence();

                /*
                 * Negative offset for hole.
                 */
                int holeStart =
                        xList.size() - startIndex;

                partsList.add(-holeStart);

                for (int j = 0; j < seq.size(); j++) {

                    xList.add(seq.getX(j));
                    yList.add(seq.getY(j));
                }
            }

            /*
             * Endpoint of Polygon geometry.
             */
            int endpoint =
                    xList.size() - startIndex - 1;

            partsList.add(endpoint);

            return;
        }


        /*
         * -----------------------------------------------------
         * MultiPolygon
         * -----------------------------------------------------
         */
        if (geometry instanceof MultiPolygon mp) {

            int numberOfPolygons =
                    mp.getNumGeometries();

            if (numberOfPolygons == 0) {
                partsList.add(0);
                partsList.add(-1);
                return;
            }

            for (int i = 0;
                 i < numberOfPolygons;
                 i++) {



                Polygon poly =
                        (Polygon) mp.getGeometryN(i);

                if (poly.isEmpty()) {
                    throw new IllegalArgumentException(
                            "Empty Polygon inside MultiPolygon");
                }


                /*
                 * Exterior ring
                 */
                LinearRing exterior =
                        poly.getExteriorRing();

                CoordinateSequence ext =
                        exterior.getCoordinateSequence();

                int exteriorStart =
                        xList.size() - startIndex;

                partsList.add(exteriorStart);

                for (int j = 0;
                     j < ext.size();
                     j++) {

                    xList.add(ext.getX(j));
                    yList.add(ext.getY(j));
                }


                /*
                 * Holes
                 */
                for (int j = 0;
                     j < poly.getNumInteriorRing();
                     j++) {

                    LinearRing hole =
                            poly.getInteriorRingN(j);

                    CoordinateSequence seq =
                            hole.getCoordinateSequence();

                    int holeStart =
                            xList.size() - startIndex;

                    partsList.add(-holeStart);

                    for (int k = 0;
                         k < seq.size();
                         k++) {

                        xList.add(seq.getX(k));
                        yList.add(seq.getY(k));
                    }
                }
            }

            /*
             * Endpoint of entire MultiPolygon.
             */
            int endpoint =
                    xList.size() - startIndex - 1;

            partsList.add(endpoint);

            return;
        }

        /*
         * -----------------------------------------------------
         * GeometryCollection
         * -----------------------------------------------------
         */
        if (geometry instanceof GeometryCollection gc) {

            /*
             * Start GeometryCollection
             */
            geometriesList.add(0);

            /*
             * Process children in their original order.
             */
            for (int i = 0; i < gc.getNumGeometries(); i++) {

                Geometry child = gc.getGeometryN(i);

                parseGeometryRecursive(
                        child,
                        xList,
                        yList,
                        partsList,
                        geometriesList
                );
            }

            /*
             * End GeometryCollection
             */
            geometriesList.add(7);

            return;
        }


        throw new IllegalArgumentException(
                "Unsupported geometry: "
                        + geometry.getGeometryType());
    }


    /**
     * Converts JTS geometry type to NFSP integer type.
     */
    private static int geometryTypeToInt(Geometry geometry) {

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

        /*
         * GeometryCollection
         *
         * 0 is reserved for the start marker in geometries.
         */
        if (geometry instanceof GeometryCollection) {
            return 0;
        }

        return -1;
    }
}
