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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Range;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.tyron.javacompletion.model.util.QualifiedNames;
import javax.lang.model.element.Modifier;

/** Represents a method. */
public class MethodEntity extends Entity implements EntityScope {
    private static final String CONSTRUCTOR_NAME = "<init>";

    private final TypeReference returnType;
    private final ImmutableList<TypeParameter> typeParameters;
    // Map of simple names -> entities.
    private final Multimap<String, Entity> entities;
    private final ClassEntity classEntity;
    private final List<EntityScope> childScopes;
    private final Range<Integer> definitionRange;
    private ImmutableList<VariableEntity> parameters;

    public MethodEntity(
            String simpleName,
            List<String> qualifiers,
            boolean isStatic,
            TypeReference returnType,
            List<VariableEntity> parameters,
            List<TypeParameter> typeParameters,
            ClassEntity classEntity,
            Optional<String> javadoc,
            Range<Integer> methodNamelRange,
            Range<Integer> definitionRange) {
        super(
                getRealSimpleName(simpleName, classEntity),
                Entity.Kind.METHOD,
                qualifiers,
                isStatic,
                javadoc,
                methodNamelRange);
        this.returnType = returnType;
        this.parameters = ImmutableList.copyOf(parameters);
        this.typeParameters = ImmutableList.copyOf(typeParameters);
        this.entities = HashMultimap.create();
        this.classEntity = classEntity;
        this.childScopes = new ArrayList<>();
        this.definitionRange = definitionRange;
    }

    private static String getRealSimpleName(String simpleName, ClassEntity classEntity) {
        if (CONSTRUCTOR_NAME.equals(simpleName)) {
            return classEntity.getSimpleName();
        } else {
            return simpleName;
        }
    }

    /////////////// Entity methods ////////////////

    @Override
    public MethodEntity getScope() {
        return this;
    }

    /////////////// EntityScope methods ///////////////

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
    public Optional<Entity> getDefiningEntity() {
        return Optional.of(this);
    }

    @Override
    public List<EntityScope> getChildScopes() {
        return ImmutableList.copyOf(childScopes);
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
        return Optional.of(classEntity);
    }

    @Override
    public Range<Integer> getDefinitionRange() {
        return definitionRange;
    }

    /////////////// Other methods ////////////////

    public void setParameters(List<VariableEntity> parameters) {
        this.parameters = ImmutableList.copyOf(parameters);
    }

    /** gets paramters */
    public ImmutableList<VariableEntity> getParameters() {
        return parameters;
    }

    public ImmutableList<TypeParameter> getTypeParameters() {
        return typeParameters;
    }

    public TypeReference getReturnType() {
        return returnType;
    }

    public ClassEntity getParentClass() {
        return classEntity;
    }

    public boolean isConstructor() {
        return getSimpleName().equals(classEntity.getSimpleName());
    }

    private Set<Modifier> modifiers;

    public void setModifiers(Set<Modifier> modifiers) {
        this.modifiers = modifiers;
    }

    public boolean isDefault() {
        return modifiers.contains(Modifier.DEFAULT);
    }

    public boolean isPublic() {
        return modifiers.contains(Modifier.PUBLIC);
    }

    @Override
    public String toString() {
        return "MethodEntity<<"
                + getTypeParameters()
                + "> "
                + QualifiedNames.formatQualifiedName(getQualifiers(), getSimpleName())
                + ">";
    }
}