# راهنمای جامع پیاده‌سازی و استفاده از SpatialBricks
راهنمای معماری و یکپارچه‌سازی برای پردازش داده‌های مکانی در مقیاس بزرگ

---

## 🏛️ مرور کلی معماری سیستم

فریم‌ورک **SpatialBricks** جهت بهینه‌سازی پردازش، مدیریت و کارایی ایندکس‌گذاری داده‌های مکانی روی پلتفرم‌های محاسبات توزیع‌شده (نظیر Apache Spark و Apache Iceberg) طراحی شده است [cite: 4]. معماری کلی سیستم شامل نقاط ورودی API، اجراکننده‌های پایپ‌لاین، مبدل‌های فرمت و مدیران باکت‌های مرجع می‌باشد [cite: 4].

| کلاس کامپوننت | نقش / نوع | توضیحات |
| :--- | :---: | :--- |
| **`SpatialBricks`** | `هسته API` | اصلی‌ترین رابط برنامه‌نویسی (API) که متدهای سطح بالا را برای بارگذاری، ایندکس‌گذاری و ذخیره‌سازی داده‌ها در اختیار کاربر قرار می‌دهد [cite: 4]. |
| **`PipeLineExecutor`** | `اجراکننده` | مدیریت و اجرای منطق مربوط به حالت‌های مختلف بارگذاری و عملیات‌های پایپ‌لاین درخواستی کاربر را بر عهده دارد [cite: 4]. |
| **`GeometryReader`** | `اینترفیس` | دارای زیرکلاس‌هایی شامل خواننده‌های فرمت‌های مکانی (WKT, WKB, GeoJSON) جهت ارسال اشیاء ساخته‌شده به پایپ‌لاین‌های اجرایی [cite: 4]. |
| **`UdfRegistry`** | `اینترفیس` | مکانیسم‌های سریال‌سازی، فرمت‌دهی و رمزگشایی UDF را مطابق با فرمت مکانی انتخاب‌شده توسط کاربر ارائه می‌دهد [cite: 4]. |
| **`AddOrUpdateIndex`** | `ایندکس‌گذاری` | مسئول ایندکس‌گذاری جداول فاقد ایندکس (که قبلاً توسط PipeLineExecutor نوشته شده‌اند) یا ایندکس‌گذاری مجدد با دقت مکانی بالاتر [cite: 4]. |
| **`SpatialReader`** | `خواننده` | خواندن مسیرهای ورودی، شناسایی ستون‌های مکانی، پارس کردن رشته‌های هندسی به اشیاء JTS Geometry و خروجی دادن یک Spark Dataset [cite: 4]. |
| **`GeometryTransformer`** | `مبدل` | تبدیل داده‌های مکانی به فرمت‌های WKB، SP یا FSP با استفاده از الگوی طراحی Adapter. فرمت‌ها در ستون `geometry` همراه با زیرفیلدهای `bbox_partitioning` ذخیره می‌شوند [cite: 4]. |
| **`BucketManager`** | `مدیریت ذخیره‌سازی` | مدیریت باکت‌های مرجع، بارگذاری Snapshot، تولید خودکار باکت‌های سراسری، تخمین زیرباکت‌ها و محاسبه مرزها (`computeBucketBorders`) [cite: 4]. |
| **`BboxIndexing`** | `ایندکس توزیع‌شده` | انتشار وضعیت باکت‌های مرجع به نودهای کلاستر و استفاده از UDFهای سفارشی `FindBucket` جهت ارزیابی مستقل مقادیر `bbox_partitioning` در نودها [cite: 4]. |
| **`BucketService`** | `سرویس` | تصحیح و تنظیم پیکربندی باکت‌ها بر اساس متادیتای پارتیشن‌های موجود در جدول جهت جلوگیری از انباشت خطا [cite: 4]. |
| **`TableWriter`** | `نویسنده` | اعتبارسنجی وجود جدول و تطابق اسکیما، سپس انجام عملیات نوشتن اتمیک برای داده‌های جدول و فایل‌های متادیتای مجزای باکت‌های مرجع [cite: 4]. |

> 📌 **نکته مهم در مورد کشینگ (Caching):**  
> در صورتی که درخواست ایندکس‌گذاری مکانی داده شده باشد، جهت جلوگیری از تکرار مراحل تبدیل هندسی در طول اکشن‌های متعدد Spark، مجموعه داده (Dataset) به صورت خودکار کش (Cache) می‌شود [cite: 4].

---

## 🚀 راهنمای شروع و آماده‌سازی محیط

### پیش‌نیازها و نسخه‌های تست‌شده
فریم‌ورک SpatialBricks با پشته تکنولوژی (Stack) زیر توسعه یافته و راستی‌آزمایی شده است [cite: 4]:

* **Java JDK:** `17` [cite: 4]
* **Apache Spark:** `3.5.6` [cite: 4]
* **Scala:** `2.13` [cite: 4]
* **Apache Iceberg:** `1.9.2` [cite: 4]
* **Apache Sedona:** `1.7.2` [cite: 4]
* **Apache Maven:** `3.9+` [cite: 4]

> ⚠️ **هشدار:** استفاده از نسخه‌های متفاوت وابستگی‌ها ممکن است باعث عدم تطابق APIها یا خطاهای زمان اجرا شود [cite: 4].

### نصب و یکپارچه‌سازی با Maven

۱. کلون کردن مخزن از گیت‌هاب [cite: 4]:
   ```bash
   git clone https://github.com/hoseinismm/spatialbricks.git
   ```
۲. باز کردن پروژه در محیط IntelliJ IDEA (یا IDE مورد نظر) و ایمپورت فایل `pom.xml` [cite: 4].
۳. در تب Maven Lifecycle، دستور `clean` و سپس `install` را اجرا کنید [cite: 4].
۴. برای استفاده از SpatialBricks در پروژه‌های دیگر، وابستگی زیر را به `pom.xml` خود اضافه کنید [cite: 4]:
   ```xml
   <dependency>
       <groupId>ir.smh</groupId>
       <artifactId>spatialbricks</artifactId>
       <version>1.0-SNAPSHOT</version>
   </dependency>
   ```

> ⚙️ **تنظیمات ضروری Java 17 VM Options:**  
> به دلیل قوانین کپسوله‌سازی سخت‌گیرانه در JDK 17، پرچم‌های VM زیر باید در پیکربندی اجرای شما قرار گیرند [cite: 4]:
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

## 💻 نمونه کدها و قطعه‌کدهای یکپارچه‌سازی

### ۱. پیکربندی SparkSession
تنظیمات پیشنهادی SparkSession پیکربندی‌شده همراه با افزونه‌های Iceberg و Sedona [cite: 4]:

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

### ۲. راه‌اندازی اولیه و بارگذاری ساده
```java
import ir.smh.spatialbricks.api.SpatialBricks;
import ir.smh.spatialbricks.api.InputFormat;
import ir.smh.spatialbricks.api.GeometryFormat;

// راه‌اندازی نمونه API
SpatialBricks sb = new SpatialBricks(spark, InputFormat.GEOJSON, GeometryFormat.FSP);

// ذخیره ساده در جدول Iceberg با نام nyc.taxi
sb.write("nyc", "taxi", "datasets/taxi.geojson");
```

### ۳. ذخیره همراه با ایندکس‌گذاری مکانی (Write With Index)
```java
sb.writeWithIndex(
    "nyc", 
    "taxi", 
    "datasets/taxi.geojson", 
    1500000,  // driverRows: تعداد سطر هدف در هر گام تخمین توسط درایور
    131072   // maxPartitionSize: حد بالای تعداد سطرهای هر پارتیشن
);
```

### ۴. متدهای مکمل ایندکس‌گذاری
```java
// ایندکس‌گذاری سطرهای جدید افزوده‌شده که کلید پارتیشن آن‌ها Null است
sb.addIndexToNewRows("nyc", "taxi", 1500000, 131072);

// بارگذاری مستقیم از طریق DataFrame / Dataset
Dataset<Row> df = spark.read().parquet("datasets/taxi.geojson");
sb.writeWithIndex("nyc", "taxi", df, 1500000, 131072);

// ایندکس‌گذاری از فایل Parquet دارای ستون‌های مجزای طول و عرض جغرافیایی (Start_Lon, Start_Lat)
sb.writeWithIndex("nyc", "taxi", "datasets/taxi.parquet", "Start_Lon", "Start_Lat", 1500000, 131072);

// بازسازی کامل و مجدد ایندکس مکانی سراسری
sb.rebuildIndex("nyc", "taxi", 1500000, 131072);
```

### ۵. رمزگشایی هندسی و پرس‌وجوهای Sedona
```java
// خواندن از جدول Iceberg و رمزگشایی ستون مکانی به شیء هندسی JTS
Dataset<Row> t = spark.read()
    .format("iceberg")
    .load(fullName)
    .withColumn("geom", expr("decodeGeometry(geometry)"));

// ثبت دستی UDF (در صورت عدم استفاده مستقیم از SpatialBricks)
UDFRegistry<?,?> udfRegistry = new FlattenSpatialParquet(spark);
udfRegistry.registerDecode();
```

### ۶. بهینه‌سازی پرس‌وجو با فیلتر کردن Bounding Box
با افزودن فیلترهای صریح `bbox_partitioning` به پرس‌وجوهای SQL، موتور Spark از ارزیابی تنبل (Lazy Evaluation) و پروسه Metadata Pruning استفاده کرده و فایل‌های پارتیشن غیرمرتبط را کاملاً نادیده می‌گیرد که این امر باعث کاهش چشمگیر تاخیر اجرای کوئری می‌شود [cite: 4]:

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

## 📁 فرمت‌های ورودی پشتیبانی‌شده

فریم‌ورک SpatialBricks به صورت نیتیو (Native) فایل‌های ورودی زیر را می‌پذیرد [cite: 4]:
* **CSV** [cite: 4]
* **Parquet** [cite: 4]
* **JSON** (حتماً باید به صورت `ndjson` / JSON خطی باشد) [cite: 4]

> 💡 **تبدیل GeoJSON استاندارد به NDJSON:**  
> در صورتی که فایل GeoJSON استاندارد به صورت آرایه دارید، می‌توانید از کلاس کمکی `ConvertGeoJsonStreaming` در پکیج `utilities` برای استریم و تبدیل آن به فرمت `ndjson` استفاده کنید [cite: 4].

---

*SpatialBricks Documentation — Persian Edition 🚀* [cite: 4]
