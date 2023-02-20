package com.tyron.javacompletion.model;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Range;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Scope of entities in a unnamed block of statements, such as if, while, or switch. */
public class BlockScope implements EntityScope {
    // Map of simple names -> entities.
    private final Multimap<String, Entity> entities;
    private final EntityScope parentScope;
    private final ArrayList<EntityScope> childScopes = new ArrayList<>();
    private final Range<Integer> definitionRange;

    public BlockScope(EntityScope parentScope, Range<Integer> definitionRange) {
        this.entities = HashMultimap.create();
        this.parentScope = parentScope;
        this.definitionRange = definitionRange;
    }

    @Override
    public Multimap<String, Entity> getMemberEntities() {
        return ImmutableMultimap.copyOf(entities);
    }

    @Override
    public void addEntity(Entity entity) {
        entities.put(entity.getSimpleName(), entity);
        childScopes.add(entity.getScope());
    }

    @Override
    public void addChildScope(EntityScope entityScope) {
        childScopes.add(entityScope);
    }

    @Override
    public Optional<EntityScope> getParentScope() {
        return Optional.of(parentScope);
    }

    @Override
    public Optional<Entity> getDefiningEntity() {
        return Optional.empty();
    }

    @Override
    public Range<Integer> getDefinitionRange() {
        return definitionRange;
    }

    @Override
    public List<EntityScope> getChildScopes() {
        return ImmutableList.copyOf(childScopes);
    }
}