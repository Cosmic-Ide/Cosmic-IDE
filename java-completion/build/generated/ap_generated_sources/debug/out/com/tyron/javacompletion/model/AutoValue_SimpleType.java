package com.tyron.javacompletion.model;

import com.google.common.collect.ImmutableList;
import javax.annotation.processing.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_SimpleType extends SimpleType {

  private final boolean primitive;

  private final String simpleName;

  private final ImmutableList<TypeArgument> typeArguments;

  private AutoValue_SimpleType(
      boolean primitive,
      String simpleName,
      ImmutableList<TypeArgument> typeArguments) {
    this.primitive = primitive;
    this.simpleName = simpleName;
    this.typeArguments = typeArguments;
  }

  @Override
  public boolean isPrimitive() {
    return primitive;
  }

  @Override
  public String getSimpleName() {
    return simpleName;
  }

  @Override
  public ImmutableList<TypeArgument> getTypeArguments() {
    return typeArguments;
  }

  @Override
  public String toString() {
    return "SimpleType{"
        + "primitive=" + primitive + ", "
        + "simpleName=" + simpleName + ", "
        + "typeArguments=" + typeArguments
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof SimpleType) {
      SimpleType that = (SimpleType) o;
      return this.primitive == that.isPrimitive()
          && this.simpleName.equals(that.getSimpleName())
          && this.typeArguments.equals(that.getTypeArguments());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= primitive ? 1231 : 1237;
    h$ *= 1000003;
    h$ ^= simpleName.hashCode();
    h$ *= 1000003;
    h$ ^= typeArguments.hashCode();
    return h$;
  }

  static final class Builder extends SimpleType.Builder {
    private boolean primitive;
    private String simpleName;
    private ImmutableList.Builder<TypeArgument> typeArgumentsBuilder$;
    private ImmutableList<TypeArgument> typeArguments;
    private byte set$0;
    Builder() {
    }
    @Override
    public SimpleType.Builder setPrimitive(boolean primitive) {
      this.primitive = primitive;
      set$0 |= 1;
      return this;
    }
    @Override
    public SimpleType.Builder setSimpleName(String simpleName) {
      if (simpleName == null) {
        throw new NullPointerException("Null simpleName");
      }
      this.simpleName = simpleName;
      return this;
    }
    @Override
    public SimpleType.Builder setTypeArguments(ImmutableList<TypeArgument> typeArguments) {
      if (typeArguments == null) {
        throw new NullPointerException("Null typeArguments");
      }
      if (typeArgumentsBuilder$ != null) {
        throw new IllegalStateException("Cannot set typeArguments after calling typeArgumentsBuilder()");
      }
      this.typeArguments = typeArguments;
      return this;
    }
    @Override
    protected ImmutableList.Builder<TypeArgument> typeArgumentsBuilder() {
      if (typeArgumentsBuilder$ == null) {
        if (typeArguments == null) {
          typeArgumentsBuilder$ = ImmutableList.builder();
        } else {
          typeArgumentsBuilder$ = ImmutableList.builder();
          typeArgumentsBuilder$.addAll(typeArguments);
          typeArguments = null;
        }
      }
      return typeArgumentsBuilder$;
    }
    @Override
    public SimpleType build() {
      if (typeArgumentsBuilder$ != null) {
        this.typeArguments = typeArgumentsBuilder$.build();
      } else if (this.typeArguments == null) {
        this.typeArguments = ImmutableList.of();
      }
      if (set$0 != 1
          || this.simpleName == null) {
        StringBuilder missing = new StringBuilder();
        if ((set$0 & 1) == 0) {
          missing.append(" primitive");
        }
        if (this.simpleName == null) {
          missing.append(" simpleName");
        }
        throw new IllegalStateException("Missing required properties:" + missing);
      }
      return new AutoValue_SimpleType(
          this.primitive,
          this.simpleName,
          this.typeArguments);
    }
  }

}
