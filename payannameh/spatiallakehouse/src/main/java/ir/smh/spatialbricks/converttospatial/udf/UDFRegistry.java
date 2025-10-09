package ir.smh.spatialbricks.converttospatial.udf;

import ir.smh.spatialbricks.converttospatial.GeometryResult;
import ir.smh.spatialbricks.converttospatial.GeometryOptions;
import ir.smh.spatialbricks.converttospatial.GeometryReader;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.api.java.UDF1;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.catalyst.expressions.GenericRowWithSchema;
import org.apache.spark.sql.types.*;
import org.locationtech.jts.geom.Geometry;

import java.util.*;


public class UDFRegistry {

    public static void registerAll(SparkSession spark, GeometryOptions options, GeometryReader adapter) {

        StructType coordType = new StructType()
                .add("x", DataTypes.DoubleType, false)
                .add("y", DataTypes.DoubleType, false);

        StructType partType = new StructType()
                .add("coordinate", DataTypes.createArrayType(coordType));

        List<StructField> fields = new ArrayList<>();
        fields.add(DataTypes.createStructField("type", DataTypes.IntegerType, true));
        fields.add(DataTypes.createStructField("part", DataTypes.createArrayType(partType), true));

        StructType bboxType = new StructType()
                .add("minx", DataTypes.DoubleType)
                .add("miny", DataTypes.DoubleType)
                .add("maxx", DataTypes.DoubleType)
                .add("maxy", DataTypes.DoubleType);
        StructType centerType = new StructType()
                .add("x", DataTypes.DoubleType)
                .add("y", DataTypes.DoubleType);


        if (options.has("bbox"))
            fields.add(DataTypes.createStructField("bbox", bboxType, true));
        if (options.has("center"))
            fields.add(DataTypes.createStructField("center", centerType, true));
        if (options.has("area"))
            fields.add(DataTypes.createStructField("area", DataTypes.DoubleType, true));
        if (options.has("startpoint"))
            fields.add(DataTypes.createStructField("startpoint", coordType, true));
        if (options.has("endpoint"))
            fields.add(DataTypes.createStructField("endpoint", coordType, true));

        StructType geometryType = new StructType(fields.toArray(new StructField[0]));

        UDF1<String, Row> stringToGeometry = (String SpatialData) -> {
            try {
                Geometry geometry = adapter.inputToGeometry(SpatialData);
                GeometryResult result = ParseGeometry.parseGeometry(geometry);
                Map<String, Object> geom = result.geomMap;
                int type = (int) geom.get("type");
                @SuppressWarnings("unchecked")
                List<List<Map<String, Object>>> partsList = (List<List<Map<String, Object>>>) geom.get("part");

                List<Row> partRows = new ArrayList<>();


                for (List<Map<String, Object>> part : partsList) {
                    List<Row> coordRows = new ArrayList<>();
                    for (Map<String, Object> c : part) {
                        double x = (double) c.get("x");
                        double y = (double) c.get("y");

                        coordRows.add(new GenericRowWithSchema(new Object[]{x, y}, coordType));
                    }
                    partRows.add(new GenericRowWithSchema(new Object[]{coordRows}, partType));
                }

                List<Object> values = new ArrayList<>();
                values.add(type);
                values.add(partRows);

                if (options.has("bbox"))
                    values.add(result.computeBBoxRow(bboxType));
                if (options.has("center"))
                    values.add(result.computeCenterRow(centerType));
                if (options.has("area"))
                    values.add(result.computeArea());
                if (options.has("startpoint"))
                    values.add(result.computeStartPointRow(coordType));
                if (options.has("endpoint")) {
                    values.add(result.computeEndPointRow(coordType));
                }

                return new GenericRowWithSchema(values.toArray(), geometryType);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        };

        spark.udf().register("stringToGeometry", stringToGeometry, geometryType);
    }
}
