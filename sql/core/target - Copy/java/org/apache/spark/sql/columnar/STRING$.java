package org.apache.spark.sql.columnar;
// no position
  class STRING$ extends org.apache.spark.sql.columnar.NativeColumnType<org.apache.spark.sql.types.StringType$> {
  /**
   * Static reference to the singleton instance of this Scala object.
   */
  public static final STRING$ MODULE$ = null;
  public   STRING$ () { throw new RuntimeException(); }
  public  int actualSize (org.apache.spark.sql.catalyst.InternalRow row, int ordinal) { throw new RuntimeException(); }
  public  void append (org.apache.spark.unsafe.types.UTF8String v, java.nio.ByteBuffer buffer) { throw new RuntimeException(); }
  public  org.apache.spark.unsafe.types.UTF8String extract (java.nio.ByteBuffer buffer) { throw new RuntimeException(); }
  public  void setField (org.apache.spark.sql.catalyst.expressions.MutableRow row, int ordinal, org.apache.spark.unsafe.types.UTF8String value) { throw new RuntimeException(); }
  public  org.apache.spark.unsafe.types.UTF8String getField (org.apache.spark.sql.catalyst.InternalRow row, int ordinal) { throw new RuntimeException(); }
  public  void copyField (org.apache.spark.sql.catalyst.InternalRow from, int fromOrdinal, org.apache.spark.sql.catalyst.expressions.MutableRow to, int toOrdinal) { throw new RuntimeException(); }
  public  org.apache.spark.unsafe.types.UTF8String clone (org.apache.spark.unsafe.types.UTF8String v) { throw new RuntimeException(); }
}
