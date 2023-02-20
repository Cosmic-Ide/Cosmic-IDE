package com.tyron.javacompletion.parser.classfile;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.tyron.javacompletion.model.TypeParameter;
import com.tyron.javacompletion.model.TypeReference;

/** Parsed signature of a method. */
@AutoValue
public abstract class MethodSignature {
    public abstract ImmutableList<TypeParameter> getTypeParameters();

    public abstract ImmutableList<TypeReference> getParameters();

    public abstract TypeReference getResult();

    public abstract ImmutableList<TypeReference> getThrowsSignatures();

    public static Builder builder() {
        return new AutoValue_MethodSignature.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder setTypeParameters(ImmutableList<TypeParameter> typeParameters);

        protected abstract ImmutableList.Builder<TypeReference> parametersBuilder();

        public Builder addParameter(TypeReference parameter) {
            parametersBuilder().add(parameter);
            return this;
        }

        public abstract Builder setResult(TypeReference result);

        protected abstract ImmutableList.Builder<TypeReference> throwsSignaturesBuilder();

        public Builder addThrowsSignature(TypeReference throwsSignature) {
            throwsSignaturesBuilder().add(throwsSignature);
            return this;
        }

        public abstract MethodSignature build();
    }
}