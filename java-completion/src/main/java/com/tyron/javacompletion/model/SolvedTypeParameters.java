package com.tyron.javacompletion.model;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Optional;

@AutoValue
public abstract class SolvedTypeParameters {
    public static final SolvedTypeParameters EMPTY = SolvedTypeParameters.builder().build();

    public abstract ImmutableMap<String, SolvedType> getTypeVariableMap();

    public Optional<SolvedType> getTypeParameter(String name) {
        return Optional.ofNullable(getTypeVariableMap().get(name));
    }


    public abstract Builder toBuilder();

    public static Builder builder() {
        return new AutoValue_SolvedTypeParameters.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        protected abstract ImmutableMap.Builder<String, SolvedType> typeVariableMapBuilder();

        public Builder putTypeParameter(String name, SolvedType solvedTypeParameter) {
            typeVariableMapBuilder().put(name, solvedTypeParameter);
            return this;
        }

        public Builder putTypeParameters(Map<String, SolvedType> solvedTypeParameters) {
            typeVariableMapBuilder().putAll(solvedTypeParameters);
            return this;
        }

        public abstract SolvedTypeParameters build();
    }
}