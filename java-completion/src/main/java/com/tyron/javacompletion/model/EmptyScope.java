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


import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Range;
import java.util.List;
import java.util.Optional;

/** An scope containing no entity. */
public class EmptyScope implements EntityScope {
    public static final EmptyScope INSTANCE = new EmptyScope();

    private EmptyScope() {}

    @Override
    public Multimap<String, Entity> getMemberEntities() {
        return ImmutableMultimap.of();
    }

    @Override
    public List<EntityScope> getChildScopes() {
        return ImmutableList.of();
    }

    @Override
    public void addEntity(Entity entity) {
        throw new UnsupportedOperationException("No entity is allowed to be added to a EmptyScope.");
    }

    @Override
    public void addChildScope(EntityScope childScope) {
        throw new UnsupportedOperationException("No scope is allowed to be added to a EmptyScope.");
    }

    @Override
    public Optional<EntityScope> getParentScope() {
        return Optional.empty();
    }

    @Override
    public Optional<Entity> getDefiningEntity() {
        return Optional.empty();
    }

    @Override
    public Range<Integer> getDefinitionRange() {
        return Range.closed(0, 0);
    }
}