package com.tyron.javacompletion.model;

import javax.annotation.processing.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_SolvedPrimitiveType extends SolvedPrimitiveType {

  private final PrimitiveEntity entity;

  AutoValue_SolvedPrimitiveType(
      PrimitiveEntity entity) {
    if (entity == null) {
      throw new NullPointerException("Null entity");
    }
    this.entity = entity;
  }

  @Override
  public PrimitiveEntity getEntity() {
    return entity;
  }

  @Override
  public String toString() {
    return "SolvedPrimitiveType{"
        + "entity=" + entity
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof SolvedPrimitiveType) {
      SolvedPrimitiveType that = (SolvedPrimitiveType) o;
      return this.entity.equals(that.getEntity());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= entity.hashCode();
    return h$;
  }

}
