package com.tyron.javacompletion.model;

/** A solved type that is linked to an {@link Entity}. */
public abstract class SolvedEntityType implements SolvedType {
    public abstract Entity getEntity();
}