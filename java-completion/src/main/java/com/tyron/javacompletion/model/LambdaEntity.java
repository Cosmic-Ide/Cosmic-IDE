package com.tyron.javacompletion.model;

import com.google.common.collect.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LambdaEntity extends Entity implements EntityScope {

    private final EntityScope parent;
    private final Multimap<String, Entity> entities;
    private final List<EntityScope> childScopes;
    private ImmutableList<VariableEntity> parameters;

    public LambdaEntity(EntityScope parent) {
        super("", Kind.METHOD, ImmutableList.of(), false, Optional.empty(), Range.open(0, 1));
        this.parent = parent;
        this.childScopes = new ArrayList<>();
        this.entities = HashMultimap.create();
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
        return null;
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

    public void setParameters(List<VariableEntity> parameters) {
        this.parameters = ImmutableList.copyOf(parameters);
    }

    public ImmutableList<VariableEntity> getParameters() {
        return parameters;
    }
}
