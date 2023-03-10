package com.tyron.javacompletion.parser.classfile;

import java.util.EnumSet;
import javax.annotation.processing.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_InnerClassEntry extends InnerClassEntry {

  private final String outerClassName;

  private final String innerName;

  private final EnumSet<ClassAccessFlag> accessFlags;

  AutoValue_InnerClassEntry(
      String outerClassName,
      String innerName,
      EnumSet<ClassAccessFlag> accessFlags) {
    if (outerClassName == null) {
      throw new NullPointerException("Null outerClassName");
    }
    this.outerClassName = outerClassName;
    if (innerName == null) {
      throw new NullPointerException("Null innerName");
    }
    this.innerName = innerName;
    if (accessFlags == null) {
      throw new NullPointerException("Null accessFlags");
    }
    this.accessFlags = accessFlags;
  }

  @Override
  public String getOuterClassName() {
    return outerClassName;
  }

  @Override
  public String getInnerName() {
    return innerName;
  }

  @Override
  public EnumSet<ClassAccessFlag> getAccessFlags() {
    return accessFlags;
  }

  @Override
  public String toString() {
    return "InnerClassEntry{"
        + "outerClassName=" + outerClassName + ", "
        + "innerName=" + innerName + ", "
        + "accessFlags=" + accessFlags
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof InnerClassEntry) {
      InnerClassEntry that = (InnerClassEntry) o;
      return this.outerClassName.equals(that.getOuterClassName())
          && this.innerName.equals(that.getInnerName())
          && this.accessFlags.equals(that.getAccessFlags());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= outerClassName.hashCode();
    h$ *= 1000003;
    h$ ^= innerName.hashCode();
    h$ *= 1000003;
    h$ ^= accessFlags.hashCode();
    return h$;
  }

}
