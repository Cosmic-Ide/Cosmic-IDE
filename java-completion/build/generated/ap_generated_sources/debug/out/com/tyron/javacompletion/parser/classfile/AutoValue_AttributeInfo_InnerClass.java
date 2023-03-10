package com.tyron.javacompletion.parser.classfile;

import com.google.common.collect.ImmutableList;
import javax.annotation.processing.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_AttributeInfo_InnerClass extends AttributeInfo.InnerClass {

  private final ImmutableList<AttributeInfo.InnerClass.ClassInfo> classes;

  AutoValue_AttributeInfo_InnerClass(
      ImmutableList<AttributeInfo.InnerClass.ClassInfo> classes) {
    if (classes == null) {
      throw new NullPointerException("Null classes");
    }
    this.classes = classes;
  }

  @Override
  public ImmutableList<AttributeInfo.InnerClass.ClassInfo> getClasses() {
    return classes;
  }

  @Override
  public String toString() {
    return "InnerClass{"
        + "classes=" + classes
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof AttributeInfo.InnerClass) {
      AttributeInfo.InnerClass that = (AttributeInfo.InnerClass) o;
      return this.classes.equals(that.getClasses());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= classes.hashCode();
    return h$;
  }

}
