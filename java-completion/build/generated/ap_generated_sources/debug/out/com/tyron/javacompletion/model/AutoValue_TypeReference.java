package com.tyron.javacompletion.model;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import javax.annotation.processing.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_TypeReference extends TypeReference {

  private final ImmutableList<String> unformalizedFullName;

  private final SimpleType simpleType;

  private final Optional<ImmutableList<String>> packageName;

  private final Optional<ImmutableList<SimpleType>> enclosingClasses;

  private final boolean array;

  private AutoValue_TypeReference(
      ImmutableList<String> unformalizedFullName,
      SimpleType simpleType,
      Optional<ImmutableList<String>> packageName,
      Optional<ImmutableList<SimpleType>> enclosingClasses,
      boolean array) {
    this.unformalizedFullName = unformalizedFullName;
    this.simpleType = simpleType;
    this.packageName = packageName;
    this.enclosingClasses = enclosingClasses;
    this.array = array;
  }

  @Override
  protected ImmutableList<String> getUnformalizedFullName() {
    return unformalizedFullName;
  }

  @Override
  protected SimpleType getSimpleType() {
    return simpleType;
  }

  @Override
  public Optional<ImmutableList<String>> getPackageName() {
    return packageName;
  }

  @Override
  public Optional<ImmutableList<SimpleType>> getEnclosingClasses() {
    return enclosingClasses;
  }

  @Override
  public boolean isArray() {
    return array;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof TypeReference) {
      TypeReference that = (TypeReference) o;
      return this.unformalizedFullName.equals(that.getUnformalizedFullName())
          && this.simpleType.equals(that.getSimpleType())
          && this.packageName.equals(that.getPackageName())
          && this.enclosingClasses.equals(that.getEnclosingClasses())
          && this.array == that.isArray();
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= unformalizedFullName.hashCode();
    h$ *= 1000003;
    h$ ^= simpleType.hashCode();
    h$ *= 1000003;
    h$ ^= packageName.hashCode();
    h$ *= 1000003;
    h$ ^= enclosingClasses.hashCode();
    h$ *= 1000003;
    h$ ^= array ? 1231 : 1237;
    return h$;
  }

  @Override
  protected TypeReference.Builder autoToBuilder() {
    return new Builder(this);
  }

  static final class Builder extends TypeReference.Builder {
    private ImmutableList<String> unformalizedFullName;
    private SimpleType simpleType;
    private Optional<ImmutableList<String>> packageName = Optional.empty();
    private Optional<ImmutableList<SimpleType>> enclosingClasses = Optional.empty();
    private boolean array;
    private byte set$0;
    Builder() {
    }
    private Builder(TypeReference source) {
      this.unformalizedFullName = source.getUnformalizedFullName();
      this.simpleType = source.getSimpleType();
      this.packageName = source.getPackageName();
      this.enclosingClasses = source.getEnclosingClasses();
      this.array = source.isArray();
      set$0 = (byte) 1;
    }
    @Override
    protected TypeReference.Builder setUnformalizedFullName(ImmutableList<String> unformalizedFullName) {
      if (unformalizedFullName == null) {
        throw new NullPointerException("Null unformalizedFullName");
      }
      this.unformalizedFullName = unformalizedFullName;
      return this;
    }
    @Override
    public TypeReference.Builder setSimpleType(SimpleType simpleType) {
      if (simpleType == null) {
        throw new NullPointerException("Null simpleType");
      }
      this.simpleType = simpleType;
      return this;
    }
    @Override
    public TypeReference.Builder setPackageName(ImmutableList<String> packageName) {
      this.packageName = Optional.of(packageName);
      return this;
    }
    @Override
    public TypeReference.Builder setPackageName(Optional<ImmutableList<String>> packageName) {
      if (packageName == null) {
        throw new NullPointerException("Null packageName");
      }
      this.packageName = packageName;
      return this;
    }
    @Override
    public TypeReference.Builder setEnclosingClasses(ImmutableList<SimpleType> enclosingClasses) {
      this.enclosingClasses = Optional.of(enclosingClasses);
      return this;
    }
    @Override
    public TypeReference.Builder setEnclosingClasses(Optional<ImmutableList<SimpleType>> enclosingClasses) {
      if (enclosingClasses == null) {
        throw new NullPointerException("Null enclosingClasses");
      }
      this.enclosingClasses = enclosingClasses;
      return this;
    }
    @Override
    public TypeReference.Builder setArray(boolean array) {
      this.array = array;
      set$0 |= 1;
      return this;
    }
    @Override
    protected TypeReference autoBuild() {
      if (set$0 != 1
          || this.unformalizedFullName == null
          || this.simpleType == null) {
        StringBuilder missing = new StringBuilder();
        if (this.unformalizedFullName == null) {
          missing.append(" unformalizedFullName");
        }
        if (this.simpleType == null) {
          missing.append(" simpleType");
        }
        if ((set$0 & 1) == 0) {
          missing.append(" array");
        }
        throw new IllegalStateException("Missing required properties:" + missing);
      }
      return new AutoValue_TypeReference(
          this.unformalizedFullName,
          this.simpleType,
          this.packageName,
          this.enclosingClasses,
          this.array);
    }
  }

}
