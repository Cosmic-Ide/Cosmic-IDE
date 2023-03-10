package com.tyron.javacompletion.parser.classfile;

import javax.annotation.processing.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_ConstantPoolInfo_ConstantMethodTypeInfo extends ConstantPoolInfo.ConstantMethodTypeInfo {

  private final int descriptorIndex;

  AutoValue_ConstantPoolInfo_ConstantMethodTypeInfo(
      int descriptorIndex) {
    this.descriptorIndex = descriptorIndex;
  }

  @Override
  public int getDescriptorIndex() {
    return descriptorIndex;
  }

  @Override
  public String toString() {
    return "ConstantMethodTypeInfo{"
        + "descriptorIndex=" + descriptorIndex
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof ConstantPoolInfo.ConstantMethodTypeInfo) {
      ConstantPoolInfo.ConstantMethodTypeInfo that = (ConstantPoolInfo.ConstantMethodTypeInfo) o;
      return this.descriptorIndex == that.getDescriptorIndex();
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= descriptorIndex;
    return h$;
  }

}
