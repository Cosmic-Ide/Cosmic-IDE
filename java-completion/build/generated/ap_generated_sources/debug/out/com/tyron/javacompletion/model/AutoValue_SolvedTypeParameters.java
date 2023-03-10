package com.tyron.javacompletion.model;

import com.google.common.collect.ImmutableMap;
import javax.annotation.processing.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_SolvedTypeParameters extends SolvedTypeParameters {

  private final ImmutableMap<String, SolvedType> typeVariableMap;

  private AutoValue_SolvedTypeParameters(
      ImmutableMap<String, SolvedType> typeVariableMap) {
    this.typeVariableMap = typeVariableMap;
  }

  @Override
  public ImmutableMap<String, SolvedType> getTypeVariableMap() {
    return typeVariableMap;
  }

  @Override
  public String toString() {
    return "SolvedTypeParameters{"
        + "typeVariableMap=" + typeVariableMap
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof SolvedTypeParameters) {
      SolvedTypeParameters that = (SolvedTypeParameters) o;
      return this.typeVariableMap.equals(that.getTypeVariableMap());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= typeVariableMap.hashCode();
    return h$;
  }

  @Override
  public SolvedTypeParameters.Builder toBuilder() {
    return new Builder(this);
  }

  static final class Builder extends SolvedTypeParameters.Builder {
    private ImmutableMap.Builder<String, SolvedType> typeVariableMapBuilder$;
    private ImmutableMap<String, SolvedType> typeVariableMap;
    Builder() {
    }
    private Builder(SolvedTypeParameters source) {
      this.typeVariableMap = source.getTypeVariableMap();
    }
    @Override
    protected ImmutableMap.Builder<String, SolvedType> typeVariableMapBuilder() {
      if (typeVariableMapBuilder$ == null) {
        if (typeVariableMap == null) {
          typeVariableMapBuilder$ = ImmutableMap.builder();
        } else {
          typeVariableMapBuilder$ = ImmutableMap.builder();
          typeVariableMapBuilder$.putAll(typeVariableMap);
          typeVariableMap = null;
        }
      }
      return typeVariableMapBuilder$;
    }
    @Override
    public SolvedTypeParameters build() {
      if (typeVariableMapBuilder$ != null) {
        this.typeVariableMap = typeVariableMapBuilder$.buildOrThrow();
      } else if (this.typeVariableMap == null) {
        this.typeVariableMap = ImmutableMap.of();
      }
      return new AutoValue_SolvedTypeParameters(
          this.typeVariableMap);
    }
  }

}
