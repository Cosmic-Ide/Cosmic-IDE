package com.tyron.javacompletion.parser.classfile;

import com.google.common.collect.ImmutableList;
import com.tyron.javacompletion.model.TypeParameter;
import com.tyron.javacompletion.model.TypeReference;
import javax.annotation.processing.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_ClassSignature extends ClassSignature {

  private final ImmutableList<TypeParameter> typeParameters;

  private final TypeReference superClass;

  private final ImmutableList<TypeReference> interfaces;

  private AutoValue_ClassSignature(
      ImmutableList<TypeParameter> typeParameters,
      TypeReference superClass,
      ImmutableList<TypeReference> interfaces) {
    this.typeParameters = typeParameters;
    this.superClass = superClass;
    this.interfaces = interfaces;
  }

  @Override
  public ImmutableList<TypeParameter> getTypeParameters() {
    return typeParameters;
  }

  @Override
  public TypeReference getSuperClass() {
    return superClass;
  }

  @Override
  public ImmutableList<TypeReference> getInterfaces() {
    return interfaces;
  }

  @Override
  public String toString() {
    return "ClassSignature{"
        + "typeParameters=" + typeParameters + ", "
        + "superClass=" + superClass + ", "
        + "interfaces=" + interfaces
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof ClassSignature) {
      ClassSignature that = (ClassSignature) o;
      return this.typeParameters.equals(that.getTypeParameters())
          && this.superClass.equals(that.getSuperClass())
          && this.interfaces.equals(that.getInterfaces());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= typeParameters.hashCode();
    h$ *= 1000003;
    h$ ^= superClass.hashCode();
    h$ *= 1000003;
    h$ ^= interfaces.hashCode();
    return h$;
  }

  static final class Builder extends ClassSignature.Builder {
    private ImmutableList.Builder<TypeParameter> typeParametersBuilder$;
    private ImmutableList<TypeParameter> typeParameters;
    private TypeReference superClass;
    private ImmutableList.Builder<TypeReference> interfacesBuilder$;
    private ImmutableList<TypeReference> interfaces;
    Builder() {
    }
    @Override
    public ClassSignature.Builder setTypeParameters(ImmutableList<TypeParameter> typeParameters) {
      if (typeParameters == null) {
        throw new NullPointerException("Null typeParameters");
      }
      if (typeParametersBuilder$ != null) {
        throw new IllegalStateException("Cannot set typeParameters after calling typeParametersBuilder()");
      }
      this.typeParameters = typeParameters;
      return this;
    }
    @Override
    protected ImmutableList.Builder<TypeParameter> typeParametersBuilder() {
      if (typeParametersBuilder$ == null) {
        if (typeParameters == null) {
          typeParametersBuilder$ = ImmutableList.builder();
        } else {
          typeParametersBuilder$ = ImmutableList.builder();
          typeParametersBuilder$.addAll(typeParameters);
          typeParameters = null;
        }
      }
      return typeParametersBuilder$;
    }
    @Override
    public ClassSignature.Builder setSuperClass(TypeReference superClass) {
      if (superClass == null) {
        throw new NullPointerException("Null superClass");
      }
      this.superClass = superClass;
      return this;
    }
    @Override
    protected ImmutableList.Builder<TypeReference> interfacesBuilder() {
      if (interfacesBuilder$ == null) {
        interfacesBuilder$ = ImmutableList.builder();
      }
      return interfacesBuilder$;
    }
    @Override
    public ClassSignature build() {
      if (typeParametersBuilder$ != null) {
        this.typeParameters = typeParametersBuilder$.build();
      } else if (this.typeParameters == null) {
        this.typeParameters = ImmutableList.of();
      }
      if (interfacesBuilder$ != null) {
        this.interfaces = interfacesBuilder$.build();
      } else if (this.interfaces == null) {
        this.interfaces = ImmutableList.of();
      }
      if (this.superClass == null) {
        String missing = " superClass";
        throw new IllegalStateException("Missing required properties:" + missing);
      }
      return new AutoValue_ClassSignature(
          this.typeParameters,
          this.superClass,
          this.interfaces);
    }
  }

}
