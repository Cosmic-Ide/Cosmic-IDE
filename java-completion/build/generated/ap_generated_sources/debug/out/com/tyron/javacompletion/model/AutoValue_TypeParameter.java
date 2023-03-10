package com.tyron.javacompletion.model;

import com.google.common.collect.ImmutableList;
import javax.annotation.processing.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_TypeParameter extends TypeParameter {

  private final String name;

  private final ImmutableList<TypeReference> extendBounds;

  AutoValue_TypeParameter(
      String name,
      ImmutableList<TypeReference> extendBounds) {
    if (name == null) {
      throw new NullPointerException("Null name");
    }
    this.name = name;
    if (extendBounds == null) {
      throw new NullPointerException("Null extendBounds");
    }
    this.extendBounds = extendBounds;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public ImmutableList<TypeReference> getExtendBounds() {
    return extendBounds;
  }

  @Override
  public String toString() {
    return "TypeParameter{"
        + "name=" + name + ", "
        + "extendBounds=" + extendBounds
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof TypeParameter) {
      TypeParameter that = (TypeParameter) o;
      return this.name.equals(that.getName())
          && this.extendBounds.equals(that.getExtendBounds());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= name.hashCode();
    h$ *= 1000003;
    h$ ^= extendBounds.hashCode();
    return h$;
  }

}
