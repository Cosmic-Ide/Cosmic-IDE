/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Scope that aggregates all members of individual {@link PackageScope}s.
 */
public class AggregatePackageScope extends PackageScope {
    private final Set<PackageScope> packageScopes;

    private static final String notSupported = "not supported.";

    public AggregatePackageScope() {
        packageScopes = new HashSet<>();
    }

    @Override
    public Multimap<String, Entity> getMemberEntities() {
        Multimap<String, Entity> members = HashMultimap.create();
        Multimap<String, PackageEntity> packageEntityMembers = HashMultimap.create();

        for (PackageScope packageScope : packageScopes) {
            for (Map.Entry<String, Entity> entry : packageScope.getMemberEntities().entries()) {
                String key = entry.getKey();
                Entity entity = entry.getValue();
                if (entity instanceof PackageEntity packageEntity) {
                    packageEntityMembers.put(key, packageEntity);
                } else {
                    members.put(key, entity);
                }
            }
        }

        for (String name : packageEntityMembers.keySet()) {
            Collection<PackageEntity> entities = packageEntityMembers.get(name);
            if (entities.size() == 1) {
                members.putAll(name, entities);
                continue;
            }

            AggregatePackageEntity aggregatePackageEntity =
                    new AggregatePackageEntity(name, Iterables.getFirst(entities, null).getQualifiers());
            AggregatePackageScope aggregatePackageScope = aggregatePackageEntity.getScope();
            members.put(name, aggregatePackageEntity);

            // Aggregate all packages into one.
            for (PackageEntity packageEntity : entities) {
                aggregatePackageScope.addPackageScope(packageEntity.getScope());
            }
        }
        return members;
    }

    @Override
    public void addEntity(Entity entity) {
        throw new UnsupportedOperationException(notSupported);
    }

    @Override
    public void removePackage(PackageEntity entity) {
        throw new UnsupportedOperationException(notSupported);
    }

    @Override
    public void addFile(FileScope fileScope) {
        throw new UnsupportedOperationException(notSupported);
    }

    @Override
    public void removeFile(FileScope fileScope) {
        throw new UnsupportedOperationException(notSupported);
    }

    @Override
    public Optional<EntityScope> getParentScope() {
        return Optional.empty();
    }

    public void addPackageScope(PackageScope packageScope) {
        packageScopes.add(packageScope);
    }
}