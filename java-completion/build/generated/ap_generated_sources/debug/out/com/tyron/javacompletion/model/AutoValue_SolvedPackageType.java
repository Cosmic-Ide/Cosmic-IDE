package com.tyron.javacompletion.model;

import javax.annotation.processing.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_SolvedPackageType extends SolvedPackageType {

  private final PackageEntity entity;

  AutoValue_SolvedPackageType(
      PackageEntity entity) {
    if (entity == null) {
      throw new NullPointerException("Null entity");
    }
    this.entity = entity;
  }

  @Override
  public PackageEntity getEntity() {
    return entity;
  }

  @Override
  public String toString() {
    return "SolvedPackageType{"
        + "entity=" + entity
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof SolvedPackageType) {
      SolvedPackageType that = (SolvedPackageType) o;
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
