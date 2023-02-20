package com.tyron.javacompletion.model;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Scope that aggregates all members of individual {@link PackageScope}s. */
public class AggregatePackageScope extends PackageScope {
    private final Set<PackageScope> packageScopes;

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
                if (entity instanceof PackageEntity) {
                    packageEntityMembers.put(key, (PackageEntity) entity);
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

            AggregatePackageScope aggregatePackageScope = null;
            AggregatePackageEntity aggregatePackageEntity =
                    new AggregatePackageEntity(name, Iterables.getFirst(entities, null).getQualifiers());
            aggregatePackageScope = aggregatePackageEntity.getScope();
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
        throw new UnsupportedOperationException("not supported.");
    }

    @Override
    public void removePackage(PackageEntity entity) {
        throw new UnsupportedOperationException("not supported.");
    }

    @Override
    public void addFile(FileScope fileScope) {
        throw new UnsupportedOperationException("not supported.");
    }

    @Override
    public void removeFile(FileScope fileScope) {
        throw new UnsupportedOperationException("not supported.");
    }

    @Override
    public Optional<EntityScope> getParentScope() {
        return Optional.empty();
    }

    public void addPackageScope(PackageScope packageScope) {
        packageScopes.add(packageScope);
    }
}