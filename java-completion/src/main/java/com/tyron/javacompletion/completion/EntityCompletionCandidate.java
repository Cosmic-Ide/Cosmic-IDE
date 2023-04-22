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
package com.tyron.javacompletion.completion;

import androidx.annotation.NonNull;

import com.tyron.javacompletion.model.ClassEntity;
import com.tyron.javacompletion.model.Entity;
import com.tyron.javacompletion.model.MethodEntity;
import com.tyron.javacompletion.model.TypeParameter;
import com.tyron.javacompletion.model.TypeReference;
import com.tyron.javacompletion.model.VariableEntity;

import java.util.List;
import java.util.Optional;

/**
 * A {@link CompletionCandidate} backed by {@link Entity}.
 */
class EntityCompletionCandidate extends EntityBasedCompletionCandidate {
    private final SortCategory sortCategory;

    EntityCompletionCandidate(Entity entity, SortCategory sortCategory) {
        super(entity);
        this.sortCategory = sortCategory;
    }

    public static Kind toCandidateKind(Entity.Kind entityKind) {
        return switch (entityKind) {
            case CLASS -> Kind.CLASS;
            case ANNOTATION, INTERFACE -> Kind.INTERFACE;
            case ENUM -> Kind.ENUM;
            case METHOD -> Kind.METHOD;
            case VARIABLE, PRIMITIVE -> Kind.VARIABLE;
            case FIELD -> Kind.FIELD;
            case QUALIFIER -> Kind.PACKAGE;
            default -> Kind.UNKNOWN;
        };
    }

    private static void appendTypeParameters(StringBuilder sb, List<TypeParameter> typeParameters) {
        sb.append("<");
        boolean firstParam = true;
        for (TypeParameter typeParameter : typeParameters) {
            if (firstParam) {
                firstParam = false;
            } else {
                sb.append(", ");
            }
            sb.append(typeParameter.toDisplayString());
        }
        sb.append(">");
    }

    @Override
    public String getName() {
        return getEntity().getSimpleName();
    }

    @Override
    public Kind getKind() {
        return toCandidateKind(getEntity().getKind());
    }

    @Override
    public Optional<String> getDetail() {
        Entity entity = getEntity();
        switch (entity.getKind()) {
            case METHOD -> {
                StringBuilder sb = new StringBuilder();
                MethodEntity method = (MethodEntity) entity;
                if (!method.getTypeParameters().isEmpty()) {
                    appendTypeParameters(sb, method.getTypeParameters());
                    sb.append(" ");
                }
                sb.append("(");
                boolean firstParam = true;
                for (VariableEntity param : method.getParameters()) {
                    if (firstParam) {
                        firstParam = false;
                    } else {
                        sb.append(", ");
                    }
                    sb.append(param.getType().toDisplayString());
                    sb.append(" ");
                    sb.append(param.getSimpleName());
                }
                sb.append("): ");
                sb.append(method.getReturnType().toDisplayString());
                return Optional.of(sb.toString());
            }
            case CLASS, INTERFACE -> {
                ClassEntity classEntity = (ClassEntity) entity;
                if (classEntity.getTypeParameters().isEmpty()
                        && !classEntity.getSuperClass().isPresent()
                        && classEntity.getInterfaces().isEmpty()) {
                    return Optional.empty();
                }
                StringBuilder sb = new StringBuilder();
                if (!classEntity.getTypeParameters().isEmpty()) {
                    appendTypeParameters(sb, classEntity.getTypeParameters());
                }
                TypeReference superClassOrOnlyInterface = null;
                if (classEntity.getSuperClass().isPresent()) {
                    superClassOrOnlyInterface = classEntity.getSuperClass().get();
                } else if (classEntity.getInterfaces().size() == 1) {
                    superClassOrOnlyInterface = classEntity.getInterfaces().get(0);
                }

                if (superClassOrOnlyInterface != null) {
                    sb.append(": ");
                    sb.append(superClassOrOnlyInterface.getSimpleName());
                }
                return Optional.of(sb.toString());
            }
            case VARIABLE, FIELD -> {
                VariableEntity variable = (VariableEntity) entity;
                return Optional.of(variable.getType().toDisplayString());
            }
            default -> {
                return Optional.empty();
            }
        }
    }

    @Override
    public SortCategory getSortCategory() {
        return sortCategory;
    }

    @NonNull
    @Override
    public String toString() {
        return getKind().name() + " " + getName();
    }
}