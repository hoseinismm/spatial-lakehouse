package ir.smh.spatialbricks.converttospatial.udf.converttogeometry;

import ir.smh.spatialbricks.converttospatial.GeometryReader;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.geojson.GeoJsonReader;

public class geoJsonReaderAdapter implements GeometryReader<String> {
    private static final GeometryFactory geometryFactory = new GeometryFactory(
            new PrecisionModel(PrecisionModel.FLOATING_SINGLE), 4326
    );

    @Override
    public Geometry inputToGeometry(String geoJson) throws Exception {
        GeoJsonReader reader = new GeoJsonReader(geometryFactory);
        return reader.read(geoJson);
    }
}

