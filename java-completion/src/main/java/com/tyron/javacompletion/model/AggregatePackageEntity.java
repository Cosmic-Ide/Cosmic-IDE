package com.tyron.javacompletion.model;

import java.util.List;
import java.util.Optional;

/** A {@link PackageEntity} that associates to {@link AggregatePackageScope}. */
public class AggregatePackageEntity extends PackageEntity {
    public AggregatePackageEntity(String simpleName, List<String> qualifiers) {
        super(simpleName, qualifiers, new AggregatePackageScope());
    }

    @Override
    public AggregatePackageScope getScope() {
        return (AggregatePackageScope) super.getScope();
    }

    @Override
    public Optional<EntityScope> getParentScope() {
        return Optional.empty();
    }
}