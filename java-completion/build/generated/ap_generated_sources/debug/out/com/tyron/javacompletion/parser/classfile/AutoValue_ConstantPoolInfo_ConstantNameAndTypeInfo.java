package com.tyron.javacompletion.parser.classfile;

import javax.annotation.processing.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_ConstantPoolInfo_ConstantNameAndTypeInfo extends ConstantPoolInfo.ConstantNameAndTypeInfo {

  private final int nameIndex;

  private final int descriptorIndex;

  AutoValue_ConstantPoolInfo_ConstantNameAndTypeInfo(
      int nameIndex,
      int descriptorIndex) {
    this.nameIndex = nameIndex;
    this.descriptorIndex = descriptorIndex;
  }

  @Override
  public int getNameIndex() {
    return nameIndex;
  }

  @Override
  public int getDescriptorIndex() {
    return descriptorIndex;
  }

  @Override
  public String toString() {
    return "ConstantNameAndTypeInfo{"
        + "nameIndex=" + nameIndex + ", "
        + "descriptorIndex=" + descriptorIndex
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof ConstantPoolInfo.ConstantNameAndTypeInfo) {
      ConstantPoolInfo.ConstantNameAndTypeInfo that = (ConstantPoolInfo.ConstantNameAndTypeInfo) o;
      return this.nameIndex == that.getNameIndex()
          && this.descriptorIndex == that.getDescriptorIndex();
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= nameIndex;
    h$ *= 1000003;
    h$ ^= descriptorIndex;
    return h$;
  }

}
