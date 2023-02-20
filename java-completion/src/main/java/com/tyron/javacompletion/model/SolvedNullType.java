package com.tyron.javacompletion.model;

import com.google.auto.value.AutoValue;

/** A solved type that is null. */
@AutoValue
public abstract class SolvedNullType implements SolvedType {
    public static final SolvedNullType INSTANCE = new AutoValue_SolvedNullType();

    @Override
    public TypeReference toTypeReference() {
        throw new RuntimeException("Cannot convert null to type reference.");
    }
}