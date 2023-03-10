package com.tyron.javacompletion.model;

import javax.annotation.processing.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_SolvedReferenceType extends SolvedReferenceType {

  private final ClassEntity entity;

  private final SolvedTypeParameters typeParameters;

  AutoValue_SolvedReferenceType(
      ClassEntity entity,
      SolvedTypeParameters typeParameters) {
    if (entity == null) {
      throw new NullPointerException("Null entity");
    }
    this.entity = entity;
    if (typeParameters == null) {
      throw new NullPointerException("Null typeParameters");
    }
    this.typeParameters = typeParameters;
  }

  @Override
  public ClassEntity getEntity() {
    return entity;
  }

  @Override
  public SolvedTypeParameters getTypeParameters() {
    return typeParameters;
  }

  @Override
  public String toString() {
    return "SolvedReferenceType{"
        + "entity=" + entity + ", "
        + "typeParameters=" + typeParameters
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof SolvedReferenceType) {
      SolvedReferenceType that = (SolvedReferenceType) o;
      return this.entity.equals(that.getEntity())
          && this.typeParameters.equals(that.getTypeParameters());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= entity.hashCode();
    h$ *= 1000003;
    h$ ^= typeParameters.hashCode();
    return h$;
  }

}
