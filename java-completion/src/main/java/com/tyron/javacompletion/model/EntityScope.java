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