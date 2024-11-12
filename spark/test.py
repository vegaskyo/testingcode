# test.py
from pyspark.sql import SparkSession

# Khởi tạo Spark với cấu hình cần thiết
spark = SparkSession.builder \
  .appName("TestDeltaLineage") \
  .config("spark.sql.extensions", "io.delta.sql.DeltaSparkSessionExtension") \
  .config("spark.datahub.rest.server", "http://localhost:8933") \
  .config("spark.extraListeners", "datahub.spark.DatahubSparkListener") \
  .config("spark.datahub.rest.token", "eyJhbGciOiJIUzI1NiJ9.eyJhY3RvclR5cGUiOiJVU0VSIiwiYWN0b3JJZCI6ImRhdGFodWIiLCJ0eXBlIjoiUEVSU09OQUwiLCJ2ZXJzaW9uIjoiMiIsImp0aSI6IjUxMDI4YmRhLTM2NWQtNGU5ZC1iNDVjLTcyNmE1ZTQzMmY2MyIsInN1YiI6ImRhdGFodWIiLCJpc3MiOiJkYXRhaHViLW1ldGFkYXRhLXNlcnZpY2UifQ.gipnE5zWB_8X2JJzFJqoCx-iq0IPJZTOWXlFxcpzXUA") \
  .getOrCreate()

# Test case 1: saveAsTable
df = spark.createDataFrame([(1, "a")], ["id", "value"])
df.write.format("delta").mode("overwrite").saveAsTable("test_table")

# Test case 2: SQL CREATE TABLE
spark.sql("""
  CREATE TABLE test_sql_table
  USING delta
  AS SELECT * FROM test_table
""")