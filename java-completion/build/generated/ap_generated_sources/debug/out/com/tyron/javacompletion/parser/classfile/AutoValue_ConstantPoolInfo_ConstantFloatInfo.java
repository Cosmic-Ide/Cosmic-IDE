package com.tyron.javacompletion.parser.classfile;

import javax.annotation.processing.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_ConstantPoolInfo_ConstantFloatInfo extends ConstantPoolInfo.ConstantFloatInfo {

  private final float value;

  AutoValue_ConstantPoolInfo_ConstantFloatInfo(
      float value) {
    this.value = value;
  }

  @Override
  public float getValue() {
    return value;
  }

  @Override
  public String toString() {
    return "ConstantFloatInfo{"
        + "value=" + value
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof ConstantPoolInfo.ConstantFloatInfo) {
      ConstantPoolInfo.ConstantFloatInfo that = (ConstantPoolInfo.ConstantFloatInfo) o;
      return Float.floatToIntBits(this.value) == Float.floatToIntBits(that.getValue());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= Float.floatToIntBits(value);
    return h$;
  }

}
