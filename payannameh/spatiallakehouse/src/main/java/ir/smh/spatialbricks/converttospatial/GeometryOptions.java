package ir.smh.spatialbricks.converttospatial;

import java.util.Set;

public class GeometryOptions {
    private final Set<String> features;

    public GeometryOptions(Set<String> features) {
        this.features = features;
    }

    public boolean has(String feature) {
        return features.contains(feature);
    }

    public static GeometryOptions of(String... features) {
        return new GeometryOptions(Set.of(features));
    }
}
