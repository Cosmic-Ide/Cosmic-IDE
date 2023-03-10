package com.tyron.javacompletion.parser.classfile;

import java.util.EnumSet;
import javax.annotation.processing.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_AttributeInfo_InnerClass_ClassInfo extends AttributeInfo.InnerClass.ClassInfo {

  private final int innerClassInfoIndex;

  private final int outerClassInfoIndex;

  private final int innerNameIndex;

  private final EnumSet<ClassAccessFlag> accessFlags;

  AutoValue_AttributeInfo_InnerClass_ClassInfo(
      int innerClassInfoIndex,
      int outerClassInfoIndex,
      int innerNameIndex,
      EnumSet<ClassAccessFlag> accessFlags) {
    this.innerClassInfoIndex = innerClassInfoIndex;
    this.outerClassInfoIndex = outerClassInfoIndex;
    this.innerNameIndex = innerNameIndex;
    if (accessFlags == null) {
      throw new NullPointerException("Null accessFlags");
    }
    this.accessFlags = accessFlags;
  }

  @Override
  public int getInnerClassInfoIndex() {
    return innerClassInfoIndex;
  }

  @Override
  public int getOuterClassInfoIndex() {
    return outerClassInfoIndex;
  }

  @Override
  public int getInnerNameIndex() {
    return innerNameIndex;
  }

  @Override
  public EnumSet<ClassAccessFlag> getAccessFlags() {
    return accessFlags;
  }

  @Override
  public String toString() {
    return "ClassInfo{"
        + "innerClassInfoIndex=" + innerClassInfoIndex + ", "
        + "outerClassInfoIndex=" + outerClassInfoIndex + ", "
        + "innerNameIndex=" + innerNameIndex + ", "
        + "accessFlags=" + accessFlags
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof AttributeInfo.InnerClass.ClassInfo) {
      AttributeInfo.InnerClass.ClassInfo that = (AttributeInfo.InnerClass.ClassInfo) o;
      return this.innerClassInfoIndex == that.getInnerClassInfoIndex()
          && this.outerClassInfoIndex == that.getOuterClassInfoIndex()
          && this.innerNameIndex == that.getInnerNameIndex()
          && this.accessFlags.equals(that.getAccessFlags());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= innerClassInfoIndex;
    h$ *= 1000003;
    h$ ^= outerClassInfoIndex;
    h$ *= 1000003;
    h$ ^= innerNameIndex;
    h$ *= 1000003;
    h$ ^= accessFlags.hashCode();
    return h$;
  }

}
