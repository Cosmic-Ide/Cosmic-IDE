package com.tyron.javacompletion.model;

import java.util.Optional;
import javax.annotation.processing.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_WildcardTypeArgument extends WildcardTypeArgument {

  private final Optional<WildcardTypeArgument.Bound> bound;

  AutoValue_WildcardTypeArgument(
      Optional<WildcardTypeArgument.Bound> bound) {
    if (bound == null) {
      throw new NullPointerException("Null bound");
    }
    this.bound = bound;
  }

  @Override
  public Optional<WildcardTypeArgument.Bound> getBound() {
    return bound;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof WildcardTypeArgument) {
      WildcardTypeArgument that = (WildcardTypeArgument) o;
      return this.bound.equals(that.getBound());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= bound.hashCode();
    return h$;
  }

}
