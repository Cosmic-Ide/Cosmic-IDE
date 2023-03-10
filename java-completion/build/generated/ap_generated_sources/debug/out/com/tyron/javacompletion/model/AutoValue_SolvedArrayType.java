package com.tyron.javacompletion.model;

import javax.annotation.processing.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_SolvedArrayType extends SolvedArrayType {

  private final SolvedType baseType;

  AutoValue_SolvedArrayType(
      SolvedType baseType) {
    if (baseType == null) {
      throw new NullPointerException("Null baseType");
    }
    this.baseType = baseType;
  }

  @Override
  public SolvedType getBaseType() {
    return baseType;
  }

  @Override
  public String toString() {
    return "SolvedArrayType{"
        + "baseType=" + baseType
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof SolvedArrayType) {
      SolvedArrayType that = (SolvedArrayType) o;
      return this.baseType.equals(that.getBaseType());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= baseType.hashCode();
    return h$;
  }

}
