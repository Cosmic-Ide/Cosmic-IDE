package com.tyron.javacompletion.parser.classfile;

import com.google.common.collect.ImmutableList;
import com.tyron.javacompletion.model.TypeParameter;
import com.tyron.javacompletion.model.TypeReference;
import javax.annotation.processing.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_MethodSignature extends MethodSignature {

  private final ImmutableList<TypeParameter> typeParameters;

  private final ImmutableList<TypeReference> parameters;

  private final TypeReference result;

  private final ImmutableList<TypeReference> throwsSignatures;

  private AutoValue_MethodSignature(
      ImmutableList<TypeParameter> typeParameters,
      ImmutableList<TypeReference> parameters,
      TypeReference result,
      ImmutableList<TypeReference> throwsSignatures) {
    this.typeParameters = typeParameters;
    this.parameters = parameters;
    this.result = result;
    this.throwsSignatures = throwsSignatures;
  }

  @Override
  public ImmutableList<TypeParameter> getTypeParameters() {
    return typeParameters;
  }

  @Override
  public ImmutableList<TypeReference> getParameters() {
    return parameters;
  }

  @Override
  public TypeReference getResult() {
    return result;
  }

  @Override
  public ImmutableList<TypeReference> getThrowsSignatures() {
    return throwsSignatures;
  }

  @Override
  public String toString() {
    return "MethodSignature{"
        + "typeParameters=" + typeParameters + ", "
        + "parameters=" + parameters + ", "
        + "result=" + result + ", "
        + "throwsSignatures=" + throwsSignatures
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof MethodSignature) {
      MethodSignature that = (MethodSignature) o;
      return this.typeParameters.equals(that.getTypeParameters())
          && this.parameters.equals(that.getParameters())
          && this.result.equals(that.getResult())
          && this.throwsSignatures.equals(that.getThrowsSignatures());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= typeParameters.hashCode();
    h$ *= 1000003;
    h$ ^= parameters.hashCode();
    h$ *= 1000003;
    h$ ^= result.hashCode();
    h$ *= 1000003;
    h$ ^= throwsSignatures.hashCode();
    return h$;
  }

  static final class Builder extends MethodSignature.Builder {
    private ImmutableList<TypeParameter> typeParameters;
    private ImmutableList.Builder<TypeReference> parametersBuilder$;
    private ImmutableList<TypeReference> parameters;
    private TypeReference result;
    private ImmutableList.Builder<TypeReference> throwsSignaturesBuilder$;
    private ImmutableList<TypeReference> throwsSignatures;
    Builder() {
    }
    @Override
    public MethodSignature.Builder setTypeParameters(ImmutableList<TypeParameter> typeParameters) {
      if (typeParameters == null) {
        throw new NullPointerException("Null typeParameters");
      }
      this.typeParameters = typeParameters;
      return this;
    }
    @Override
    protected ImmutableList.Builder<TypeReference> parametersBuilder() {
      if (parametersBuilder$ == null) {
        parametersBuilder$ = ImmutableList.builder();
      }
      return parametersBuilder$;
    }
    @Override
    public MethodSignature.Builder setResult(TypeReference result) {
      if (result == null) {
        throw new NullPointerException("Null result");
      }
      this.result = result;
      return this;
    }
    @Override
    protected ImmutableList.Builder<TypeReference> throwsSignaturesBuilder() {
      if (throwsSignaturesBuilder$ == null) {
        throwsSignaturesBuilder$ = ImmutableList.builder();
      }
      return throwsSignaturesBuilder$;
    }
    @Override
    public MethodSignature build() {
      if (parametersBuilder$ != null) {
        this.parameters = parametersBuilder$.build();
      } else if (this.parameters == null) {
        this.parameters = ImmutableList.of();
      }
      if (throwsSignaturesBuilder$ != null) {
        this.throwsSignatures = throwsSignaturesBuilder$.build();
      } else if (this.throwsSignatures == null) {
        this.throwsSignatures = ImmutableList.of();
      }
      if (this.typeParameters == null
          || this.result == null) {
        StringBuilder missing = new StringBuilder();
        if (this.typeParameters == null) {
          missing.append(" typeParameters");
        }
        if (this.result == null) {
          missing.append(" result");
        }
        throw new IllegalStateException("Missing required properties:" + missing);
      }
      return new AutoValue_MethodSignature(
          this.typeParameters,
          this.parameters,
          this.result,
          this.throwsSignatures);
    }
  }

}
