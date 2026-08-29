package ir.smh.spatialbricks.udf;

import ir.smh.spatialbricks.core.BucketManager;
import ir.smh.spatialbricks.decoder.GeoLakeDecoder;
import ir.smh.spatialbricks.encoder.GeometryResult;
import ir.smh.spatialbricks.encoder.converttogeometry.GeoJsonGeometricalAdapter;
import ir.smh.spatialbricks.encoder.converttogeometry.GeometryReader;
import ir.smh.spatialbricks.encoder.converttogeometry.WKBReaderAdapter;
import ir.smh.spatialbricks.encoder.converttogeometry.WKTReaderAdapter;

import org.apache.spark.broadcast.Broadcast;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.api.java.UDF1;
import org.apache.spark.sql.catalyst.expressions.GenericRowWithSchema;
import org.apache.spark.sql.sedona_sql.UDT.GeometryUDT$;
import org.apache.spark.sql.types.DataType;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import org.locationtech.jts.geom.Geometry;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import static org.apache.spark.sql.functions.*;

public class GeoLake
        implements UDFRegistry<Geometry, Map<String, Object>>,
        Serializable {

    private final SparkSession spark;

    public GeoLake(SparkSession spark) {
        this.spark = spark;
    }


    // =========================================================
    // SCHEMAS
    // =========================================================

    private static final StructType BBOX_SCHEMA =
            DataTypes.createStructType(
                    new StructField[]{
                            DataTypes.createStructField(
                                    "min_x",
                                    DataTypes.DoubleType,
                                    true
                            ),

                            DataTypes.createStructField(
                                    "min_y",
                                    DataTypes.DoubleType,
                                    true
                            ),

                            DataTypes.createStructField(
                                    "max_x",
                                    DataTypes.DoubleType,
                                    true
                            ),

                            DataTypes.createStructField(
                                    "max_y",
                                    DataTypes.DoubleType,
                                    true
                            )
                    }
            );


    private static final StructType BUCKET_SCHEMA =
            DataTypes.createStructType(
                    new StructField[]{
                            DataTypes.createStructField(
                                    "min_x",
                                    DataTypes.DoubleType,
                                    true
                            ),

                            DataTypes.createStructField(
                                    "min_y",
                                    DataTypes.DoubleType,
                                    true
                            ),

                            DataTypes.createStructField(
                                    "max_x",
                                    DataTypes.DoubleType,
                                    true
                            ),

                            DataTypes.createStructField(
                                    "max_y",
                                    DataTypes.DoubleType,
                                    true
                            ),

                            DataTypes.createStructField(
                                    "region_code",
                                    DataTypes.LongType,
                                    true
                            )
                    }
            );


    /*
     * =========================================================
     * GEO LAKE GEOMETRY SCHEMA
     *
     * type:
     *
     * 1 = Point
     * 2 = LineString
     * 3 = Polygon
     * 4 = MultiPoint
     * 5 = MultiLineString
     * 6 = MultiPolygon
     * 7 = GeometryCollection
     *
     * coordinateRanges:
     *
     * End-exclusive coordinate offsets.
     *
     * lineRanges:
     *
     * 1-based start/end indexes into coordinateRanges.
     *
     * geometryRanges:
     *
     * 1-based coordinate start positions for
     * GeometryCollection children.
     *
     * geometryTypes:
     *
     * Geometry type of each GeometryCollection child.
     * =========================================================
     */

    private static final StructType GEOMETRY_TYPE =
            DataTypes.createStructType(
                    new StructField[]{

                            DataTypes.createStructField(
                                    "type",
                                    DataTypes.IntegerType,
                                    false
                            ),

                            DataTypes.createStructField(
                                    "x",
                                    DataTypes.createArrayType(
                                            DataTypes.DoubleType
                                    ),
                                    false
                            ),

                            DataTypes.createStructField(
                                    "y",
                                    DataTypes.createArrayType(
                                            DataTypes.DoubleType
                                    ),
                                    false
                            ),

                            DataTypes.createStructField(
                                    "coordinateRanges",
                                    DataTypes.createArrayType(
                                            DataTypes.IntegerType
                                    ),
                                    false
                            ),

                            DataTypes.createStructField(
                                    "lineRanges",
                                    DataTypes.createArrayType(
                                            DataTypes.IntegerType
                                    ),
                                    false
                            ),

                            DataTypes.createStructField(
                                    "geometryRanges",
                                    DataTypes.createArrayType(
                                            DataTypes.IntegerType
                                    ),
                                    false
                            ),

                            DataTypes.createStructField(
                                    "geometryTypes",
                                    DataTypes.createArrayType(
                                            DataTypes.IntegerType
                                    ),
                                    false
                            ),

                            DataTypes.createStructField(
                                    "bbox_partitioning",
                                    BUCKET_SCHEMA,
                                    true
                            )
                    }
            );


    // =========================================================
    // PARSE
    // =========================================================

    public Map<String, Object> parse(
            Geometry geometry) {

        return ParseGeometryForGeoLake.parseGeometry(
                geometry
        );
    }


    // =========================================================
    // 1) GEOMETRY ENCODER UDF
    // =========================================================

    public DataType getGeometryType() {
        return GEOMETRY_TYPE;
    }


    public void registerGeometryUdf(
            GeometryReader<?> adapter) {

        UDF1<Object, Row> udf =
                (Object input) -> {

                    if (input == null) {
                        return null;
                    }

                    try {

                        Geometry geometry;

                        if (input instanceof byte[]
                                && adapter instanceof WKBReaderAdapter) {

                            geometry =
                                    ((WKBReaderAdapter) adapter)
                                            .inputToGeometry(
                                                    (byte[]) input
                                            );

                        } else if (input instanceof String
                                && adapter instanceof WKTReaderAdapter) {

                            geometry =
                                    ((WKTReaderAdapter) adapter)
                                            .inputToGeometry(
                                                    (String) input
                                            );

                        } else if (input instanceof Geometry
                                && adapter instanceof GeoJsonGeometricalAdapter) {

                            geometry =
                                    ((GeoJsonGeometricalAdapter) adapter)
                                            .inputToGeometry(
                                                    (Geometry) input
                                            );

                        } else {

                            throw new IllegalArgumentException(
                                    "Unsupported input: "
                                            + input.getClass()
                            );
                        }

                        return geometryToRow(geometry);

                    } catch (Exception e) {

                        System.err.println(
                                "GeoLake Geometry UDF error: "
                                        + e.getMessage()
                        );

                        return null;
                    }
                };

        spark.udf().register(
                "encodeGeometry",
                udf,
                GEOMETRY_TYPE
        );
    }


    // =========================================================
    // GEOMETRY -> SPARK ROW
    // =========================================================

    public Row geometryToRow(
            Geometry geometry) {

        Map<String, Object> geom =
                ParseGeometryForGeoLake.parseGeometry(
                        geometry
                );

        return new GenericRowWithSchema(
                new Object[]{

                        geom.get("type"),

                        geom.get("x"),

                        geom.get("y"),

                        geom.get("coordinateRanges"),

                        geom.get("lineRanges"),

                        geom.get("geometryRanges"),

                        geom.get("geometryTypes"),

                        null
                },

                GEOMETRY_TYPE
        );
    }


    // =========================================================
    // 2) BBOX UDF
    // =========================================================

    public void registerBboxUdf() {

        spark.udf().register(
                "calculateBbox",

                (Row geometry) -> {

                    double[] bbox =
                            calculateBounds(geometry);

                    if (bbox == null) {

                        return RowFactory.create(
                                null,
                                null,
                                null,
                                null
                        );
                    }

                    return RowFactory.create(
                            bbox[0],
                            bbox[1],
                            bbox[2],
                            bbox[3]
                    );
                },

                BBOX_SCHEMA
        );
    }


    // =========================================================
    // 3) BUCKET UDF
    // =========================================================

    public void registerBucketUdf(
            Broadcast<BucketManager.Bucket>
                    broadcastRootBuckets) {

        BucketManager.Bucket root =
                broadcastRootBuckets.value();

        spark.udf().register(
                "findBucket",

                (Row geometry) -> {

                    double[] bbox =
                            calculateBounds(geometry);

                    if (bbox == null) {

                        return RowFactory.create(
                                null,
                                null,
                                null,
                                null,
                                null
                        );
                    }

                    BucketManager.Bucket bucket =
                            findBucket(
                                    root,
                                    bbox[0],
                                    bbox[1],
                                    bbox[2],
                                    bbox[3]
                            );

                    return RowFactory.create(
                            bucket.xmin,
                            bucket.ymin,
                            bucket.xmax,
                            bucket.ymax,
                            bucket.code
                    );
                },

                BUCKET_SCHEMA
        );
    }


    // =========================================================
    // 4) DECODE UDF
    // =========================================================

    public Geometry geometryToJTS(
            Row geoRow) {

        return GeoLakeDecoder.geometryToJTS(
                geoRow
        );
    }


    public void registerDecode() {

        spark.udf().register(
                "decodeGeometry",

                (Row geoRow) ->
                        GeoLakeDecoder.geometryToJTS(
                                geoRow
                        ),

                GeometryUDT$.MODULE$
        );
    }


    // =========================================================
    // 5) GEOHASH UDF
    // =========================================================

    public void registerAddGeohash() {

        spark.udf().register(
                "addgeohash",

                (Row geoRow) -> {

                    List<Double> x =
                            geoRow.getList(
                                    geoRow.fieldIndex("x")
                            );

                    List<Double> y =
                            geoRow.getList(
                                    geoRow.fieldIndex("y")
                            );

                    if (x == null
                            || y == null
                            || x.isEmpty()
                            || y.isEmpty()) {

                        return null;
                    }

                    return GeometryResult.computeGeoHash(
                            x.get(0),
                            y.get(0)
                    );
                },

                DataTypes.StringType
        );
    }


    // =========================================================
    // CORE LOGIC
    // =========================================================

    private double[] calculateBounds(
            Row geometry) {

        if (geometry == null) {
            return null;
        }

        List<Double> x =
                geometry.getList(
                        geometry.fieldIndex("x")
                );

        List<Double> y =
                geometry.getList(
                        geometry.fieldIndex("y")
                );

        if (x == null
                || y == null
                || x.isEmpty()
                || y.isEmpty()) {

            return null;
        }

        if (x.size() != y.size()) {

            throw new IllegalArgumentException(
                    "X and Y arrays have different lengths: "
                            + x.size()
                            + " != "
                            + y.size()
            );
        }

        double minX =
                Double.POSITIVE_INFINITY;

        double minY =
                Double.POSITIVE_INFINITY;

        double maxX =
                Double.NEGATIVE_INFINITY;

        double maxY =
                Double.NEGATIVE_INFINITY;

        for (int i = 0;
             i < x.size();
             i++) {

            double xi = x.get(i);
            double yi = y.get(i);

            if (xi < minX) {
                minX = xi;
            }

            if (yi < minY) {
                minY = yi;
            }

            if (xi > maxX) {
                maxX = xi;
            }

            if (yi > maxY) {
                maxY = yi;
            }
        }

        return new double[]{
                minX,
                minY,
                maxX,
                maxY
        };
    }


    // =========================================================
    // FIND BUCKET
    // =========================================================

    private BucketManager.Bucket findBucket(
            BucketManager.Bucket bucket,
            double minX,
            double minY,
            double maxX,
            double maxY) {

        while (bucket.hasChildren) {

            if (maxX <= bucket.xmid
                    && minY >= bucket.ymid) {

                bucket =
                        bucket.topleft;

            } else if (minX >= bucket.xmid
                    && minY >= bucket.ymid) {

                bucket =
                        bucket.topright;

            } else if (maxX <= bucket.xmid
                    && maxY <= bucket.ymid) {

                bucket =
                        bucket.bottomleft;

            } else if (minX >= bucket.xmid
                    && maxY <= bucket.ymid) {

                bucket =
                        bucket.bottomright;

            } else {

                /*
                 * Geometry crosses bucket boundaries.
                 * Stop descending.
                 */
                break;
            }
        }

        return bucket;
    }


    // =========================================================
    // POINT GEOMETRY COLUMN
    // =========================================================

    public Dataset<Row> addPointGeometryColumn(
            Dataset<Row> df,
            String xColumn,
            String yColumn,
            String geometryColumnName) {

        StructType bboxType =
                new StructType()
                        .add(
                                "min_x",
                                DataTypes.DoubleType,
                                false
                        )
                        .add(
                                "min_y",
                                DataTypes.DoubleType,
                                false
                        )
                        .add(
                                "max_x",
                                DataTypes.DoubleType,
                                false
                        )
                        .add(
                                "max_y",
                                DataTypes.DoubleType,
                                false
                        )
                        .add(
                                "region_code",
                                DataTypes.LongType,
                                false
                        );

        return df
                .withColumn(
                        geometryColumnName,

                        struct(

                                /*
                                 * Point
                                 */
                                lit(1)
                                        .alias("type"),

                                /*
                                 * x
                                 */
                                array(
                                        col(xColumn)
                                )
                                        .alias("x"),

                                /*
                                 * y
                                 */
                                array(
                                        col(yColumn)
                                )
                                        .alias("y"),

                                /*
                                 * Point does not use
                                 * coordinateRanges.
                                 */
                                array()
                                        .cast(
                                                DataTypes.createArrayType(
                                                        DataTypes.IntegerType
                                                )
                                        )
                                        .alias(
                                                "coordinateRanges"
                                        ),

                                /*
                                 * Point does not use
                                 * lineRanges.
                                 */
                                array()
                                        .cast(
                                                DataTypes.createArrayType(
                                                        DataTypes.IntegerType
                                                )
                                        )
                                        .alias(
                                                "lineRanges"
                                        ),

                                /*
                                 * Point does not use
                                 * geometryRanges.
                                 */
                                array()
                                        .cast(
                                                DataTypes.createArrayType(
                                                        DataTypes.IntegerType
                                                )
                                        )
                                        .alias(
                                                "geometryRanges"
                                        ),

                                /*
                                 * Point does not use
                                 * geometryTypes.
                                 */
                                array()
                                        .cast(
                                                DataTypes.createArrayType(
                                                        DataTypes.IntegerType
                                                )
                                        )
                                        .alias(
                                                "geometryTypes"
                                        ),

                                /*
                                 * bbox_partitioning
                                 */
                                lit(null)
                                        .cast(bboxType)
                                        .alias(
                                                "bbox_partitioning"
                                        )
                        )
                )
                .select(
                        geometryColumnName
                );
    }
}

