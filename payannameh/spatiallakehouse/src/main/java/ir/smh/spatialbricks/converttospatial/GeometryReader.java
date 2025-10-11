package ir.smh.spatialbricks.converttospatial;

import org.locationtech.jts.geom.Geometry;

import java.io.Serializable;

public interface GeometryReader<T> {
    Geometry inputToGeometry(T input) throws Exception;
}

