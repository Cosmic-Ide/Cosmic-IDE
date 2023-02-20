package com.tyron.javacompletion.parser.classfile;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.tyron.javacompletion.model.TypeParameter;
import com.tyron.javacompletion.model.TypeReference;

/** Parsed signature of a class. */
@AutoValue
public abstract class ClassSignature {
    public abstract ImmutableList<TypeParameter> getTypeParameters();

    public abstract TypeReference getSuperClass();

    public abstract ImmutableList<TypeReference> getInterfaces();

    public static Builder builder() {
        return new AutoValue_ClassSignature.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        protected abstract ImmutableList.Builder<TypeParameter> typeParametersBuilder();

        public abstract Builder setTypeParameters(ImmutableList<TypeParameter> typeParameters);

        public Builder addTypeParameter(TypeParameter typeParameter) {
            typeParametersBuilder().add(typeParameter);
            return this;
        }

        public abstract Builder setSuperClass(TypeReference superClass);

        protected abstract ImmutableList.Builder<TypeReference> interfacesBuilder();

        public Builder addInterface(TypeReference iface) {
            interfacesBuilder().add(iface);
            return this;
        }

        public abstract ClassSignature build();
    }
}