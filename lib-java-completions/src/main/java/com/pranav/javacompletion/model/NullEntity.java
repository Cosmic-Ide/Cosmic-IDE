package com.pranav.javacompletion.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;

import java.util.Optional;

/** Represents null. */
public class NullEntity extends Entity {
    public static final NullEntity INSTANCE = new NullEntity();

    private NullEntity() {
        super(
                "null",
                Entity.Kind.NULL,
                ImmutableList.<String>of() /* qualifiers */,
                true /* isStatic */,
                Optional.empty() /* javadoc */,
                Range.closedOpen(0, 0));
    }

    @Override
    public Optional<EntityScope> getParentScope() {
        return Optional.empty();
    }

    @Override
    public EmptyScope getScope() {
        return EmptyScope.INSTANCE;
    }

    @Override
    public String toString() {
        return "NullEntity";
    }
}
