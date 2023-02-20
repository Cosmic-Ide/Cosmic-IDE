package com.tyron.javacompletion.model;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** A mutable {@link SolvedTypeParameters}. */
public class MutableSolvedTypeParameters extends SolvedTypeParameters {
    public final HashMap<String, SolvedType> typeVariableMap;

    public MutableSolvedTypeParameters() {
        this.typeVariableMap = new HashMap<>();
    }

    @Override
    public ImmutableMap<String, SolvedType> getTypeVariableMap() {
        return ImmutableMap.copyOf(typeVariableMap);
    }

    @Override
    public Optional<SolvedType> getTypeParameter(String name) {
        return Optional.ofNullable(typeVariableMap.get(name));
    }

    @Override
    public SolvedTypeParameters.Builder toBuilder() {
        throw new UnsupportedOperationException();
    }

    public MutableSolvedTypeParameters putTypeParameter(String name, SolvedType solvedType) {
        typeVariableMap.put(name, solvedType);
        return this;
    }

    public MutableSolvedTypeParameters putAllTypeParameters(
            Map<String, SolvedType> allTypeParameters) {
        typeVariableMap.putAll(allTypeParameters);
        return this;
    }

    public MutableSolvedTypeParameters removeTypeParameter(String name) {
        typeVariableMap.remove(name);
        return this;
    }

    public SolvedTypeParameters toImmutable() {
        return SolvedTypeParameters.builder().putTypeParameters(typeVariableMap).build();
    }

    public static MutableSolvedTypeParameters copyOf(SolvedTypeParameters solvedTypeParameters) {
        MutableSolvedTypeParameters ret = new MutableSolvedTypeParameters();
        ret.putAllTypeParameters(solvedTypeParameters.getTypeVariableMap());
        return ret;
    }
}