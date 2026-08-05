# SpatialBricks Implementation & Usage Guide
Comprehensive Architecture & Integration Guide for Large-Scale Spatial Processing

---

## 🏛️ System Architecture Overview

The **SpatialBricks** framework is designed to optimize spatial data processing, management, and indexing performance across distributed computing platforms (e.g., Apache Spark and Apache Iceberg) . The overall architecture consists of API Entry Points, Pipeline Executors, Format Converters, and Reference Bucket Managers.

| Component Class | Role / Type | Description                                                                                                                                                                 |
| :--- | :---: |:----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **`SpatialBricks`** | `API Core` | The primary entry-point API exposing high-level methods for users to perform ingestion, indexing, and data writing operations.                                              |
| **`PipeLineExecutor`** | `Executor` | Orchestrates execution logic for various ingestion modes and pipeline operations requested by the user.                                                                     |
| **`GeometryReader`** | `Interface` | Contains subclasses equipped with readers for spatial formats (WKT, WKB, GeoJSON) and passes constructed objects to execution pipelines.                                    |
| **`UdfRegistry`** | `Interface` | Provides serialization, formatting, and UDF decoding mechanisms according to user-selected spatial formats.                                                                 |
| **`AddOrUpdateIndex`** | `Indexing` | Responsible for indexing unindexed tables previously written by PipeLineExecutor or re-indexing datasets for higher spatial granularity.                                    |
| **`SpatialReader`** | `Reader` | Reads input paths, locates spatial columns, parses geometry strings into JTS Geometry objects, and outputs a Spark Dataset.                                                 |
| **`GeometryTransformer`** | `Transformer` | Converts spatial data into WKB, SP, or FSP representations using the Adapter pattern. Formats are stored in the `geometry` column alongside `bbox_partitioning` sub-fields. |
| **`BucketManager`** | `Core Storage` | Manages reference buckets, snapshot loading, auto-generation of global buckets, estimation of sub-buckets, and boundary calculations (`computeBucketBorders`).              |
| **`BboxIndexing`** | `Distributed Index` | Broadcasts reference bucket states to cluster nodes and utilizes custom `FindBucket` UDFs to evaluate `bbox_partitioning` values independently across nodes.                |
| **`BucketService`** | `Service` | Corrects and adjusts bucket configurations based on existing table partition metadata to prevent error accumulation.                                                        |
| **`TableWriter`** | `Writer` | Validates table existence and schema alignment, then performs atomic writes for both table data and isolated reference bucket metadata files.                               |

> 📌 **Important Note on Caching:**  
> If spatial indexing is requested, the Dataset is automatically cached to prevent redundant transformation steps across multiple Spark Action operations.

---

## 🚀 Getting Started & Environment Setup

### Prerequisites & Tested Versions
SpatialBricks has been developed and verified using the following stack:

* **Java JDK:** `17` 
* **Apache Spark:** `3.5.6` 
* **Scala:** `2.13` 
* **Apache Iceberg:** `1.9.2` 
* **Apache Sedona:** `1.7.2` 
* **Apache Maven:** `3.9+` 

> ⚠️ **Warning:** Using different dependency versions may result in API incompatibilities or runtime errors [cite: 4].

### Installation & Maven Integration

1. Clone the repository from GitHub :
   ```bash
   git clone https://github.com/hoseinismm/spatialbricks.git
   ```
2. Open the project in IntelliJ IDEA (or preferred IDE) and import the `pom.xml` file [cite: 4].
3. In the Maven Lifecycle tab, run `clean` followed by `install` [cite: 4].
4. To consume SpatialBricks in another project, include the following dependency in your `pom.xml` [cite: 4]:
   ```xml
   <dependency>
       <groupId>ir.smh</groupId>
       <artifactId>spatialbricks</artifactId>
       <version>1.0-SNAPSHOT</version>
   </dependency>
   ```

> ⚙️ **Mandatory Java 17 VM Options:**  
> Due to JDK 17 strong encapsulation rules, the following VM flags must be configured in your run configuration:
> ```text
> --add-opens java.base/java.io=ALL-UNNAMED 
> --add-opens java.base/java.lang=ALL-UNNAMED
> --add-opens java.base/java.nio=ALL-UNNAMED 
> --add-opens java.base/java.net=ALL-UNNAMED
> --add-opens java.base/java.util=ALL-UNNAMED
> --add-exports java.base/sun.nio.ch=ALL-UNNAMED 
> --add-exports java.base/sun.security.action=ALL-UNNAMED
> ```

---

## 💻 Code Examples & Integration Snippets

### 1. SparkSession Configuration
Recommended SparkSession setup configured with Iceberg and Sedona extensions:

```java
SparkSession spark = SparkSession.builder()
    .appName("Spatial-Lakehouse-Writer")
    // MEMORY CONFIGURATION
    .config("spark.driver.memory", "12g")
    .config("spark.driver.maxResultSize", "4g")
    .config("spark.executor.memory", "8g")
    .config("spark.memory.fraction", "0.8")
    .config("spark.memory.storageFraction", "0.3")
    // OFF-HEAP MEMORY
    .config("spark.memory.offHeap.enabled", "true")
    .config("spark.memory.offHeap.size", "2g")
    // PERFORMANCE TUNING
    .config("spark.sql.shuffle.partitions", "50")
    .config("spark.default.parallelism", "50")
    .config("spark.sql.autoBroadcastJoinThreshold", "-1")
    .config("spark.sql.files.maxPartitionBytes", "32m")
    .config("spark.sql.parquet.blockSize", "32m")
    // SEDONA + ICEBERG EXTENSIONS
    .config("spark.sql.extensions",
            "org.apache.iceberg.spark.extensions.IcebergSparkSessionExtensions," +
            "org.apache.sedona.sql.SedonaSqlExtensions")
    .config("spark.sql.catalog.spark_catalog",
            "org.apache.iceberg.spark.SparkSessionCatalog")
    .config("spark.sql.catalog.spark_catalog.type", "hadoop")
    .config("spark.sql.catalog.spark_catalog.warehouse", warehousePath)
    // DEPENDENCY PACKAGES
    .config("spark.jars.packages", String.join(",", new String[]{
            "org.apache.iceberg:iceberg-spark-runtime-3.5_2.13:1.9.2",
            "org.apache.sedona:sedona-spark-shaded-3.5_2.13:1.7.2"
    }))
    .master("local[4]")
    .getOrCreate();
```

### 2. Basic Initialization and Ingestion
```java
import ir.smh.spatialbricks.api.SpatialBricks;
import ir.smh.spatialbricks.api.InputFormat;
import ir.smh.spatialbricks.api.GeometryFormat;

// Initialize API Instance
SpatialBricks sb = new SpatialBricks(spark, InputFormat.GEOJSON, GeometryFormat.FSP);

// Simple write to Iceberg table: nyc.taxi
sb.write("nyc", "taxi", "datasets/taxi.geojson");
```

### 3. Writing with Spatial Indexing (Write With Index)
```java
sb.writeWithIndex(
    "nyc", 
    "taxi", 
    "datasets/taxi.geojson", 
    1500000,  // driverRows: Target row count per estimation driver step
    131072   // maxPartitionSize: Upper threshold for partition row limits
);
```

### 4. Additional Indexing Methods
```java
// Index newly appended rows with null partition keys
sb.addIndexToNewRows("nyc", "taxi", 1500000, 131072);

// Direct DataFrame/Dataset Ingestion
Dataset<Row> df = spark.read().parquet("datasets/taxi.geojson");
sb.writeWithIndex("nyc", "taxi", df, 1500000, 131072);

// Index from Parquet containing explicit Longitude / Latitude columns
sb.writeWithIndex("nyc", "taxi", "datasets/taxi.parquet", "Start_Lon", "Start_Lat", 1500000, 131072);

// Rebuild global spatial index from scratch
sb.rebuildIndex("nyc", "taxi", 1500000, 131072);
```

### 5. Geometry Decoding & Sedona Queries
```java
// Read from Iceberg and decode spatial column into JTS geometry
Dataset<Row> t = spark.read()
    .format("iceberg")
    .load(fullName)
    .withColumn("geom", expr("decodeGeometry(geometry)"));

// Manual UDF Registration (if SpatialBricks instance isn't present)
UDFRegistry<?,?> udfRegistry = new FlattenSpatialParquet(spark);
udfRegistry.registerDecode();
```

### 6. Query Optimization with Bounding Box Filtering
By attaching explicit `bbox_partitioning` filters to SQL queries, Spark leverages metadata pruning via Lazy Evaluation to bypass irrelevant partition files, drastically improving query latency:

```sql
SELECT SUM(ST_AreaSpheroid(geom)) AS total_area
FROM polygons, iran
WHERE ST_Within(geom, iran.geom)
  AND geometry.bbox_partitioning.min_x < 63.5
  AND geometry.bbox_partitioning.min_y < 40.8
  AND geometry.bbox_partitioning.max_x > 44.0
  AND geometry.bbox_partitioning.max_y > 25.0;
```

---

## 📁 Supported Input File Formats

SpatialBricks natively accepts input files formatted as :
* **CSV** [cite: 4]
* **Parquet** [cite: 4]
* **JSON** (Must strictly be `ndjson` / Line-delimited JSON format) 

> 💡 **Converting Standard GeoJSON to NDJSON:**  
> If you have a standard GeoJSON file array, use the utility class `ConvertGeoJsonStreaming` under the project's `utilities` package to stream and convert it into `ndjson` format.

---

*SpatialBricks Documentation — English Edition 🚀* 
