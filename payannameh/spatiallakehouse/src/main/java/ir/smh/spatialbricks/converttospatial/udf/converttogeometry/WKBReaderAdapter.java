package ir.smh.spatialbricks.converttospatial.udf.converttogeometry;

import ir.smh.spatialbricks.converttospatial.GeometryReader;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKBReader;

public class WKBReaderAdapter implements GeometryReader<byte[]> {

    @Override
    public Geometry inputToGeometry(byte[] input) throws Exception {
        WKBReader reader = new WKBReader();
        return reader.read(input);
    }
}
