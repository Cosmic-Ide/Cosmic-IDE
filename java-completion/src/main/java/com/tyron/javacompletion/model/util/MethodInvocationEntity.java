package com.tyron.javacompletion.model.util;

import com.google.common.collect.*;
import com.tyron.javacompletion.model.Entity;
import com.tyron.javacompletion.model.EntityScope;
import com.tyron.javacompletion.model.TypeParameter;
import com.tyron.javacompletion.model.VariableEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MethodInvocationEntity extends Entity implements EntityScope {

    private final Multimap<String, Entity> entities;
    private final List<TypeParameter> typeParameters;
    private final List<VariableEntity> parameters;
    private final List<EntityScope> childScopes;
    private final EntityScope parent;

    public MethodInvocationEntity(String simpleName,
                                  EntityScope parent,
                                  List<String> qualifiers,
                                  List<TypeParameter> typeParameters,
                                  List<VariableEntity> parameters,
                                  Range<Integer> symbolRange
                                  ) {
        super(simpleName, Kind.REFERENCE, qualifiers, false, Optional.empty(), symbolRange);
        this.parent = parent;
        this.typeParameters = typeParameters;
        this.parameters = parameters;
        this.entities = HashMultimap.create();
        this.childScopes = new ArrayList<>();
    }

    @Override
    public EntityScope getScope() {
        return this;
    }

    @Override
    public Multimap<String, Entity> getMemberEntities() {
        ImmutableMultimap.Builder<String, Entity> builder = new ImmutableMultimap.Builder<>();
        builder.putAll(entities);
        for (VariableEntity parameter : parameters) {
            builder.put(parameter.getSimpleName(), parameter);
        }
        return builder.build();
    }

    @Override
    public Optional<EntityScope> getParentScope() {
        return Optional.of(parent);
    }

    @Override
    public List<EntityScope> getChildScopes() {
        return ImmutableList.copyOf(childScopes);
    }

    @Override
    public Optional<Entity> getDefiningEntity() {
        return Optional.of(this);
    }

    @Override
    public Range<Integer> getDefinitionRange() {
        return Range.open(0, 0);
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
}
