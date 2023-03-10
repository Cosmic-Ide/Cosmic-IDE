package com.tyron.javacompletion.parser.classfile;

import javax.annotation.processing.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_ConstantPoolInfo_ConstantInvokeDynamicInfo extends ConstantPoolInfo.ConstantInvokeDynamicInfo {

  private final int bootstrapMethodAttrIndex;

  private final int nameAndTypeIndex;

  AutoValue_ConstantPoolInfo_ConstantInvokeDynamicInfo(
      int bootstrapMethodAttrIndex,
      int nameAndTypeIndex) {
    this.bootstrapMethodAttrIndex = bootstrapMethodAttrIndex;
    this.nameAndTypeIndex = nameAndTypeIndex;
  }

  @Override
  public int getBootstrapMethodAttrIndex() {
    return bootstrapMethodAttrIndex;
  }

  @Override
  public int getNameAndTypeIndex() {
    return nameAndTypeIndex;
  }

  @Override
  public String toString() {
    return "ConstantInvokeDynamicInfo{"
        + "bootstrapMethodAttrIndex=" + bootstrapMethodAttrIndex + ", "
        + "nameAndTypeIndex=" + nameAndTypeIndex
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof ConstantPoolInfo.ConstantInvokeDynamicInfo) {
      ConstantPoolInfo.ConstantInvokeDynamicInfo that = (ConstantPoolInfo.ConstantInvokeDynamicInfo) o;
      return this.bootstrapMethodAttrIndex == that.getBootstrapMethodAttrIndex()
          && this.nameAndTypeIndex == that.getNameAndTypeIndex();
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= bootstrapMethodAttrIndex;
    h$ *= 1000003;
    h$ ^= nameAndTypeIndex;
    return h$;
  }

}
