package com.tyron.javacompletion.parser.classfile;

import javax.annotation.processing.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_ParsedClassFile_ParsedMethod extends ParsedClassFile.ParsedMethod {

  private final String simpleName;

  private final MethodSignature methodSignature;

  private final boolean static0;

  AutoValue_ParsedClassFile_ParsedMethod(
      String simpleName,
      MethodSignature methodSignature,
      boolean static0) {
    if (simpleName == null) {
      throw new NullPointerException("Null simpleName");
    }
    this.simpleName = simpleName;
    if (methodSignature == null) {
      throw new NullPointerException("Null methodSignature");
    }
    this.methodSignature = methodSignature;
    this.static0 = static0;
  }

  @Override
  public String getSimpleName() {
    return simpleName;
  }

  @Override
  public MethodSignature getMethodSignature() {
    return methodSignature;
  }

  @Override
  public boolean isStatic() {
    return static0;
  }

  @Override
  public String toString() {
    return "ParsedMethod{"
        + "simpleName=" + simpleName + ", "
        + "methodSignature=" + methodSignature + ", "
        + "static=" + static0
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof ParsedClassFile.ParsedMethod) {
      ParsedClassFile.ParsedMethod that = (ParsedClassFile.ParsedMethod) o;
      return this.simpleName.equals(that.getSimpleName())
          && this.methodSignature.equals(that.getMethodSignature())
          && this.static0 == that.isStatic();
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= simpleName.hashCode();
    h$ *= 1000003;
    h$ ^= methodSignature.hashCode();
    h$ *= 1000003;
    h$ ^= static0 ? 1231 : 1237;
    return h$;
  }

}
