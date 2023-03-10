package com.tyron.javacompletion.parser.classfile;

import javax.annotation.processing.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_ConstantPoolInfo_ConstantMethodrefInfo extends ConstantPoolInfo.ConstantMethodrefInfo {

  private final int classIndex;

  private final int nameAndTypeIndex;

  AutoValue_ConstantPoolInfo_ConstantMethodrefInfo(
      int classIndex,
      int nameAndTypeIndex) {
    this.classIndex = classIndex;
    this.nameAndTypeIndex = nameAndTypeIndex;
  }

  @Override
  public int getClassIndex() {
    return classIndex;
  }

  @Override
  public int getNameAndTypeIndex() {
    return nameAndTypeIndex;
  }

  @Override
  public String toString() {
    return "ConstantMethodrefInfo{"
        + "classIndex=" + classIndex + ", "
        + "nameAndTypeIndex=" + nameAndTypeIndex
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof ConstantPoolInfo.ConstantMethodrefInfo) {
      ConstantPoolInfo.ConstantMethodrefInfo that = (ConstantPoolInfo.ConstantMethodrefInfo) o;
      return this.classIndex == that.getClassIndex()
          && this.nameAndTypeIndex == that.getNameAndTypeIndex();
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= classIndex;
    h$ *= 1000003;
    h$ ^= nameAndTypeIndex;
    return h$;
  }

}
