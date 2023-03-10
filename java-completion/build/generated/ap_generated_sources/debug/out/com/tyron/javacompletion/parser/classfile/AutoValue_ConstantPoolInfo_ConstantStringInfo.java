package com.tyron.javacompletion.parser.classfile;

import javax.annotation.processing.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_ConstantPoolInfo_ConstantStringInfo extends ConstantPoolInfo.ConstantStringInfo {

  private final int stringIndex;

  AutoValue_ConstantPoolInfo_ConstantStringInfo(
      int stringIndex) {
    this.stringIndex = stringIndex;
  }

  @Override
  public int getStringIndex() {
    return stringIndex;
  }

  @Override
  public String toString() {
    return "ConstantStringInfo{"
        + "stringIndex=" + stringIndex
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof ConstantPoolInfo.ConstantStringInfo) {
      ConstantPoolInfo.ConstantStringInfo that = (ConstantPoolInfo.ConstantStringInfo) o;
      return this.stringIndex == that.getStringIndex();
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= stringIndex;
    return h$;
  }

}
