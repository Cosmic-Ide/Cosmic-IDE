package com.tyron.javacompletion.parser.classfile;

import javax.annotation.processing.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_ConstantPoolInfo_ConstantMethodHandleInfo extends ConstantPoolInfo.ConstantMethodHandleInfo {

  private final byte referenceKind;

  private final int referenceIndex;

  AutoValue_ConstantPoolInfo_ConstantMethodHandleInfo(
      byte referenceKind,
      int referenceIndex) {
    this.referenceKind = referenceKind;
    this.referenceIndex = referenceIndex;
  }

  @Override
  public byte getReferenceKind() {
    return referenceKind;
  }

  @Override
  public int getReferenceIndex() {
    return referenceIndex;
  }

  @Override
  public String toString() {
    return "ConstantMethodHandleInfo{"
        + "referenceKind=" + referenceKind + ", "
        + "referenceIndex=" + referenceIndex
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof ConstantPoolInfo.ConstantMethodHandleInfo) {
      ConstantPoolInfo.ConstantMethodHandleInfo that = (ConstantPoolInfo.ConstantMethodHandleInfo) o;
      return this.referenceKind == that.getReferenceKind()
          && this.referenceIndex == that.getReferenceIndex();
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= referenceKind;
    h$ *= 1000003;
    h$ ^= referenceIndex;
    return h$;
  }

}
