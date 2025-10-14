package ir.smh.spatialbricks.converttospatial.udf;


import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.api.java.UDF1;
import org.apache.spark.sql.types.DataTypes;

import java.util.HashMap;
import java.util.Map;

public class GeohashToLongUdfRegistry {

    // متد برای ثبت UDF
    public static void registerAll(SparkSession spark) {
        // نگاشت کاراکترهای Geohash به مقادیر 0 تا 31
        Map<Character, Integer> geohashMap = new HashMap<>();
        String chars = "0123456789bcdefghjkmnpqrstuvwxyz";
        for (int i = 0; i < chars.length(); i++) {
            geohashMap.put(chars.charAt(i), i);
        }

        // تعریف UDF
        UDF1<org.apache.spark.sql.Row, Long> geohashToLong = (geometry) -> {
            String gh;
            Object geohash = geometry.getAs("geohash");
            if (geohash instanceof String) {
                gh = (String) geohash;
            } else return null;
            if (gh == null || gh.isEmpty()) return null;
            long value = 0;
            for (char c : gh.toCharArray()) {
                Integer v = geohashMap.get(c);
                if (v == null) {
                    throw new IllegalArgumentException("Invalid geohash character: " + c);
                }
                value = value * 32 + v;
            }
            return value;
        };

        // ثبت UDF در Spark
        spark.udf().register("geohashToLong", geohashToLong, DataTypes.LongType);
    }
}

