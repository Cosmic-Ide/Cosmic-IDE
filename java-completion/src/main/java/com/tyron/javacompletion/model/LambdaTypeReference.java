package com.tyron.javacompletion.model;

import com.google.common.collect.ImmutableList;

import java.util.Optional;

public class LambdaTypeReference extends TypeReference {

    private final String name;

    public LambdaTypeReference(String name) {
        this.name = name;
    }

    @Override
    public String getSimpleName() {
        return name;
    }

    @Override
    protected ImmutableList<String> getUnformalizedFullName() {
        return ImmutableList.of();
    }

    @Override
    protected SimpleType getSimpleType() {
        return null;
    }

    @Override
    public Optional<ImmutableList<String>> getPackageName() {
        return Optional.empty();
    }

    @Override
    public Optional<ImmutableList<SimpleType>> getEnclosingClasses() {
        return Optional.empty();
    }

    @Override
    public boolean isArray() {
        return false;
    }

    @Override
    protected Builder autoToBuilder() {
        return null;
    }

    @Override
    public ImmutableList<TypeArgument> getTypeArguments() {
        return ImmutableList.of();
    }
}
