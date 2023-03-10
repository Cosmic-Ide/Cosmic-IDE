package com.tyron.javacompletion.parser.classfile;

import javax.annotation.processing.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_ConstantPoolInfo_ConstantClassInfo extends ConstantPoolInfo.ConstantClassInfo {

  private final int nameIndex;

  AutoValue_ConstantPoolInfo_ConstantClassInfo(
      int nameIndex) {
    this.nameIndex = nameIndex;
  }

  @Override
  public int getNameIndex() {
    return nameIndex;
  }

  @Override
  public String toString() {
    return "ConstantClassInfo{"
        + "nameIndex=" + nameIndex
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof ConstantPoolInfo.ConstantClassInfo) {
      ConstantPoolInfo.ConstantClassInfo that = (ConstantPoolInfo.ConstantClassInfo) o;
      return this.nameIndex == that.getNameIndex();
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= nameIndex;
    return h$;
  }

}
