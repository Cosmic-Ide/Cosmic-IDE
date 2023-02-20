package com.tyron.javacompletion.model;

import com.google.common.collect.Multimap;
import com.google.common.collect.Range;

import java.util.List;
import java.util.Optional;

public interface EntityScope {
    Multimap<String, Entity> getMemberEntities();

    Optional<EntityScope> getParentScope();

    List<EntityScope> getChildScopes();

    /** Returns the entity that defines this scope. */
    Optional<Entity> getDefiningEntity();

    /**
     * The range that defines this scope.
     *
     * <p>The range includes the whole defining entity if present. For example for a method it starts
     * from the first modifier of the method and ends with the closing bracket.
     */
    Range<Integer> getDefinitionRange();

    void addEntity(Entity entity);

    void addChildScope(EntityScope entityScope);
}