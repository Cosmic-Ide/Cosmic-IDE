package com.tyron.javacompletion.model;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.stream.Collectors;

/** Type parameter for parameterized classe and method declarations. */
@AutoValue
public abstract class TypeParameter {
    public abstract String getName();

    public abstract ImmutableList<TypeReference> getExtendBounds();

    public static TypeParameter create(String name, List<TypeReference> extendBounds) {
        return new AutoValue_TypeParameter(name, ImmutableList.copyOf(extendBounds));
    }

    public String toDisplayString() {
        if (getExtendBounds().isEmpty()) {
            return getName();
        }

        return getName()
                + " extends "
                + getExtendBounds().stream()
                .map(b -> b.toDisplayString())
                .collect(Collectors.joining(", "));
    }
}