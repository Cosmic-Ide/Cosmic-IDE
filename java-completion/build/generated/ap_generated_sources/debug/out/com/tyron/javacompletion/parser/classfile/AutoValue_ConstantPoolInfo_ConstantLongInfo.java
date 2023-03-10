package com.tyron.javacompletion.parser.classfile;

import javax.annotation.processing.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_ConstantPoolInfo_ConstantLongInfo extends ConstantPoolInfo.ConstantLongInfo {

  private final long value;

  AutoValue_ConstantPoolInfo_ConstantLongInfo(
      long value) {
    this.value = value;
  }

  @Override
  public long getValue() {
    return value;
  }

  @Override
  public String toString() {
    return "ConstantLongInfo{"
        + "value=" + value
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof ConstantPoolInfo.ConstantLongInfo) {
      ConstantPoolInfo.ConstantLongInfo that = (ConstantPoolInfo.ConstantLongInfo) o;
      return this.value == that.getValue();
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= (int) ((value >>> 32) ^ value);
    return h$;
  }

}
