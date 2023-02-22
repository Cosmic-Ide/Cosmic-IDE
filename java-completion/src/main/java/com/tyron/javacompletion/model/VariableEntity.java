/*
 *  This file is part of CodeAssist.
 *
 *  CodeAssist is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  CodeAssist is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *   along with CodeAssist.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.tyron.javacompletion.model;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Range;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Represents a variable. */
public class VariableEntity extends Entity implements EntityScope {
    public static final Set<Entity.Kind> ALLOWED_KINDS =
            EnumSet.of(Entity.Kind.VARIABLE, Entity.Kind.FIELD);

    private final TypeReference type;
    private final EntityScope parentScope;
    private final Range<Integer> definitionRange;

    public VariableEntity(
            String simpleName,
            Entity.Kind kind,
            List<String> qualifiers,
            boolean isStatic,
            TypeReference type,
            EntityScope parentScope,
            Optional<String> javadoc,
            Range<Integer> variableNameRange,
            Range<Integer> definitionRange) {
        super(simpleName, kind, qualifiers, isStatic, javadoc, variableNameRange);
        checkArgument(ALLOWED_KINDS.contains(kind), "Kind %s is not allowed for variables.", kind);
        this.type = type;
        this.parentScope = parentScope;
        this.definitionRange = definitionRange;
    }

    public TypeReference getType() {
        return type;
    }

    @Override
    public EntityScope getScope() {
        return this;
    }

    @Override
    public Optional<EntityScope> getParentScope() {
        return Optional.of(parentScope);
    }

    @Override
    public List<EntityScope> getChildScopes() {
        return ImmutableList.of();
    }

    @Override
    public Multimap<String, Entity> getMemberEntities() {
        return ImmutableMultimap.of();
    }

    @Override
    public String toString() {
        return "VariableEntity<" + getType().getSimpleName() + ' ' + getSimpleName() + ">";
    }

    @Override
    public void addChildScope(EntityScope entityScope) {
        throw new UnsupportedOperationException("Variables doen't have child scopes.");
    }

    @Override
    public void addEntity(Entity entity) {
        throw new UnsupportedOperationException("Variables cannot have child entities.");
    }

    @Override
    public Optional<Entity> getDefiningEntity() {
        return Optional.of(this);
    }

    @Override
    public Range<Integer> getDefinitionRange() {
        return definitionRange;
    }
}