from pyspark.sql import SparkSession
from delta.tables import DeltaTable
import shutil
import os

# Create Spark session with DataHub lineage configuration
spark = SparkSession.builder \
  .appName("delta-lineage-example") \
  .config("spark.jars.packages", 
          "io.acryl:acryl-spark-lineage:0.2.16,io.delta:delta-spark_2.12:3.2.0,io.delta:delta-storage_2.12:3.2.0") \
  .config("spark.sql.extensions", "io.delta.sql.DeltaSparkSessionExtension") \
  .config("spark.sql.catalog.spark_catalog", 
          "org.apache.spark.sql.delta.catalog.DeltaCatalog") \
  .config("spark.extraListeners", "datahub.spark.DatahubSparkListener") \
  .config("spark.datahub.rest.server", "http://localhost:8379") \
  .config("spark.datahub.rest.token", "eyJhbGciOiJIUzI1NiJ9.eyJhY3RvclR5cGUiOiJVU0VSIiwiYWN0b3JJZCI6ImRhdGFodWIiLCJ0eXBlIjoiUEVSU09OQUwiLCJ2ZXJzaW9uIjoiMiIsImp0aSI6IjYzYmU0MDE2LTVjY2ItNDE1NS05OTkxLWRlNzdhNmNiMmZkZiIsInN1YiI6ImRhdGFodWIiLCJleHAiOjE3Mzk4MTQxNDcsImlzcyI6ImRhdGFodWItbWV0YWRhdGEtc2VydmljZSJ9.20f8k7mzUJp8-9w6U7X-mAYOJei2e1JwLGOzJ5mf2U0") \
  .enableHiveSupport() \
  .getOrCreate()


# Enhanced cleanup function with better error handling
def cleanup_tables():
  try:
      # Drop tables from metastore
      tables = ["derived_table_sql", "derived_table", "source_table"]
      for table in tables:
          print(f"Dropping table if exists: {table}")
          spark.sql(f"DROP TABLE IF EXISTS {table}")
      
      # Clean up physical locations
      warehouse_dir = spark.conf.get("spark.sql.warehouse.dir").replace("file:///", "")
      for table in tables:
          table_path = os.path.join(warehouse_dir, table)
          if os.path.exists(table_path):
              print(f"Cleaning up directory: {table_path}")
              shutil.rmtree(table_path)
              
  except Exception as e:
      print(f"Error during cleanup: {str(e)}")

# Run cleanup
cleanup_tables()

# Create source Delta table
spark.sql("""
  CREATE TABLE IF NOT EXISTS source_table (
      id INT,
      name STRING,
      value DOUBLE
  ) USING DELTA
""")

# Insert sample data
spark.sql("""
  INSERT INTO source_table VALUES 
  (1, 'John', 100.0),
  (2, 'Jane', 200.0),
  (3, 'Bob', 300.0)
""")

# Create derived table using DataFrame API and saveAsTable
source_df = spark.sql("SELECT * FROM source_table")
derived_df = source_df.selectExpr("id", "name", "value * 2 as doubled_value")
derived_df.write.format("delta").mode("overwrite").saveAsTable("derived_table")

# Alternative method using pure SQL
spark.sql("""
  CREATE TABLE derived_table_sql USING DELTA AS
  SELECT id, name, value * 2 as doubled_value
  FROM source_table
""")

spark.stop()