package ir.smh.spatialbricks.converttospatial;

import org.locationtech.jts.geom.Geometry;

public interface GeometryReader<T> {
    Geometry inputToGeometry(T input) throws Exception;
}

