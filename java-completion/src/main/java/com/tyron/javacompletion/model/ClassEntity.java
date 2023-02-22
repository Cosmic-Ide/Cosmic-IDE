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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Range;
import com.tyron.javacompletion.model.util.QualifiedNames;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Represents a class, interface, enum, or annotation.
 */
public class ClassEntity extends Entity implements EntityScope {
    public static final Set<Entity.Kind> ALLOWED_KINDS =
            EnumSet.of(
                    Entity.Kind.CLASS, Entity.Kind.INTERFACE, Entity.Kind.ANNOTATION, Entity.Kind.ENUM);

    // Map of simple names -> fields.
    private final Map<String, VariableEntity> fields;
    // Map of simple names -> methods.
    private final Multimap<String, MethodEntity> methods;
    private final List<MethodEntity> constructors;
    private final EntityScope parentScope;
    private final Optional<TypeReference> superClass;
    private final ImmutableList<TypeReference> interfaces;
    private final Map<String, ClassEntity> innerClasses;
    private final ImmutableList<TypeParameter> typeParameters;
    private final List<EntityScope> childScopes;
    private final Range<Integer> definitionRange;

    public ClassEntity(
            String simpleName,
            Entity.Kind kind,
            List<String> qualifiers,
            boolean isStatic,
            EntityScope parentScope,
            Optional<TypeReference> superClass,
            List<TypeReference> interfaces,
            List<TypeParameter> typeParameters,
            Optional<String> javadoc,
            Range<Integer> classNameRage,
            Range<Integer> definitionRange) {
        super(simpleName, kind, qualifiers, isStatic, javadoc, classNameRage);
        checkArgument(
                ALLOWED_KINDS.contains(kind),
                "Invalid entity kind %s, allowed kinds are %s",
                kind,
                ALLOWED_KINDS);
        this.fields = new HashMap<>();
        this.methods = HashMultimap.create();
        this.constructors = new ArrayList<>();
        this.parentScope = parentScope;
        this.superClass = superClass;
        this.interfaces = ImmutableList.copyOf(interfaces);
        this.typeParameters = ImmutableList.copyOf(typeParameters);
        this.innerClasses = new HashMap<>();
        this.childScopes = new ArrayList<>();
        this.definitionRange = definitionRange;
    }

    @Override
    public boolean isInstanceMember() {
        // Inner classes can be in non-instance context of enclosing classes, regardless wether
        // it's static or not.
        return false;
    }

    @Override
    public ClassEntity getScope() {
        return this;
    }

    @Override
    public List<EntityScope> getChildScopes() {
        return childScopes;
    }

    @Override
    public Multimap<String, Entity> getMemberEntities() {
        ImmutableMultimap.Builder<String, Entity> builder = new ImmutableMultimap.Builder<>();
        return builder
                .putAll(fields.entrySet())
                .putAll(methods)
                .putAll(innerClasses.entrySet())
                .build();
    }

    @Override
    public void addEntity(Entity entity) {
        childScopes.add(entity.getScope());
        if (entity instanceof ClassEntity) {
            innerClasses.put(entity.getSimpleName(), (ClassEntity) entity);
        } else if (entity instanceof MethodEntity) {
            MethodEntity methodEntity = (MethodEntity) entity;
            if (methodEntity.isConstructor()) {
                constructors.add(methodEntity);
            } else {
                methods.put(entity.getSimpleName(), methodEntity);
            }
        } else {
            fields.put(entity.getSimpleName(), (VariableEntity) entity);
        }
    }

    @Override
    public void addChildScope(EntityScope childScope) {
        checkArgument(
                !childScope.getDefiningEntity().isPresent(),
                "Should call addEntity for adding entity %s",
                childScope.getClass().getSimpleName());
        childScopes.add(childScope);
    }

    @Override
    public Optional<Entity> getDefiningEntity() {
        return Optional.of(this);
    }

    @Override
    public Range<Integer> getDefinitionRange() {
        return definitionRange;
    }

    public List<MethodEntity> getMethodsWithName(String simpleName) {
        return ImmutableList.copyOf(methods.get(simpleName));
    }

    public Optional<VariableEntity> getFieldWithName(String simpleName) {
        return Optional.ofNullable(fields.get(simpleName));
    }

    public List<MethodEntity> getConstructors() {
        return ImmutableList.copyOf(constructors);
    }

    public ImmutableList<TypeReference> getInterfaces() {
        return interfaces;
    }

    public Optional<TypeReference> getSuperClass() {
        return superClass;
    }

    @Override
    public Optional<EntityScope> getParentScope() {
        return Optional.of(parentScope);
    }

    public Map<String, ClassEntity> getInnerClasses() {
        return ImmutableMap.copyOf(innerClasses);
    }

    public ImmutableList<TypeParameter> getTypeParameters() {
        return typeParameters;
    }

    public ImmutableList<MethodEntity> getMethods() {
        return ImmutableList.copyOf(methods.values());
    }
    @Override
    public String toString() {
        return "ClassEntity<"
                + QualifiedNames.formatQualifiedName(getQualifiers(), getSimpleName())
                + "<"
                + typeParameters
                + ">>";
    }
}