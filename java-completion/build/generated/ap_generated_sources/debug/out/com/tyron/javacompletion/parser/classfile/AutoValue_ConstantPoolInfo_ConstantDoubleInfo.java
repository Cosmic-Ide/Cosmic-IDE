package com.tyron.javacompletion.parser.classfile;

import javax.annotation.processing.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_ConstantPoolInfo_ConstantDoubleInfo extends ConstantPoolInfo.ConstantDoubleInfo {

  private final double value;

  AutoValue_ConstantPoolInfo_ConstantDoubleInfo(
      double value) {
    this.value = value;
  }

  @Override
  public double getValue() {
    return value;
  }

  @Override
  public String toString() {
    return "ConstantDoubleInfo{"
        + "value=" + value
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof ConstantPoolInfo.ConstantDoubleInfo) {
      ConstantPoolInfo.ConstantDoubleInfo that = (ConstantPoolInfo.ConstantDoubleInfo) o;
      return Double.doubleToLongBits(this.value) == Double.doubleToLongBits(that.getValue());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= (int) ((Double.doubleToLongBits(value) >>> 32) ^ Double.doubleToLongBits(value));
    return h$;
  }

}
