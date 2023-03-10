package com.tyron.javacompletion.model;

import javax.annotation.processing.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_WildcardTypeArgument_Bound extends WildcardTypeArgument.Bound {

  private final WildcardTypeArgument.Bound.Kind kind;

  private final TypeReference typeReference;

  AutoValue_WildcardTypeArgument_Bound(
      WildcardTypeArgument.Bound.Kind kind,
      TypeReference typeReference) {
    if (kind == null) {
      throw new NullPointerException("Null kind");
    }
    this.kind = kind;
    if (typeReference == null) {
      throw new NullPointerException("Null typeReference");
    }
    this.typeReference = typeReference;
  }

  @Override
  public WildcardTypeArgument.Bound.Kind getKind() {
    return kind;
  }

  @Override
  public TypeReference getTypeReference() {
    return typeReference;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof WildcardTypeArgument.Bound) {
      WildcardTypeArgument.Bound that = (WildcardTypeArgument.Bound) o;
      return this.kind.equals(that.getKind())
          && this.typeReference.equals(that.getTypeReference());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= kind.hashCode();
    h$ *= 1000003;
    h$ ^= typeReference.hashCode();
    return h$;
  }

}
