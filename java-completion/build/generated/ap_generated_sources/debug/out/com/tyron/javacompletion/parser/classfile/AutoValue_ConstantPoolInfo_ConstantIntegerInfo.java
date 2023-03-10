package com.tyron.javacompletion.parser.classfile;

import javax.annotation.processing.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_ConstantPoolInfo_ConstantIntegerInfo extends ConstantPoolInfo.ConstantIntegerInfo {

  private final int value;

  AutoValue_ConstantPoolInfo_ConstantIntegerInfo(
      int value) {
    this.value = value;
  }

  @Override
  public int getValue() {
    return value;
  }

  @Override
  public String toString() {
    return "ConstantIntegerInfo{"
        + "value=" + value
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof ConstantPoolInfo.ConstantIntegerInfo) {
      ConstantPoolInfo.ConstantIntegerInfo that = (ConstantPoolInfo.ConstantIntegerInfo) o;
      return this.value == that.getValue();
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= value;
    return h$;
  }

}
