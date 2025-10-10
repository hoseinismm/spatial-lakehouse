package ir.smh.spatialbricks.converttospatial;

import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.Envelope;
import org.apache.spark.sql.catalyst.expressions.GenericRowWithSchema;
import org.apache.spark.sql.types.StructType;
import java.util.Map;
import ch.hsr.geohash.GeoHash;

public class GeometryResult {
    public Geometry geometry; // شیء JTS
    public Map<String, Object> geomMap; // ساختار Map برای UDF

    public GeometryResult() {
    }

    public GeometryResult(Geometry geometry, Map<String, Object> geomMap) {
        this.geometry = geometry;
        this.geomMap = geomMap;
    }

    /**
     * محاسبه مساحت
     */
    public double computeArea() {
        if (geometry != null) return geometry.getArea();
        return 0.0;
    }

    /**
     * محاسبه جعبه محدودکننده در قالب StructType
     */
    public GenericRowWithSchema computeBBoxRow(StructType bboxType) {
        if (geometry != null) {
            Envelope env = geometry.getEnvelopeInternal();
            return new GenericRowWithSchema(
                    new Object[]{env.getMinX(), env.getMinY(), env.getMaxX(), env.getMaxY()},
                    bboxType
            );
        }
        return null;
    }

    /**
     * محاسبه مرکز (centroid) در قالب StructType x/y
     */
    public GenericRowWithSchema computeCenterRow(StructType centerType) {
        if (geometry != null) {
            Point centroid = geometry.getCentroid();
            return new GenericRowWithSchema(
                    new Object[]{centroid.getX(), centroid.getY()},
                    centerType
            );
        }
        return null;
    }

    /**
     * محاسبه نقطه شروع (اولین Coordinate)
     */
    public GenericRowWithSchema computeStartPointRow(StructType coordType) {
        if (geometry != null && !geometry.isEmpty()) {
            Coordinate start = geometry.getCoordinates()[0];
            return new GenericRowWithSchema(
                    new Object[]{start.getX(), start.getY()},
                    coordType
            );
        }
        return null;
    }

    /**
     * محاسبه نقطه پایان (آخرین Coordinate)
     */
    public GenericRowWithSchema computeEndPointRow(StructType coordType) {
        if (geometry != null && !geometry.isEmpty()) {
            Coordinate[] coords = geometry.getCoordinates();
            Coordinate end = coords[coords.length - 1];
            return new GenericRowWithSchema(
                    new Object[]{end.getX(), end.getY()},
                    coordType
            );
        }
        return null;
    }



    public String computeGeoHash() {
        if (geometry != null) {
            Point centroid = geometry.getCentroid();
            double lat = centroid.getY();
            double lon = centroid.getX();
            GeoHash hash = GeoHash.withCharacterPrecision(lat, lon, 6);
            return hash.toBase32();
        }
        return null;
    }

}