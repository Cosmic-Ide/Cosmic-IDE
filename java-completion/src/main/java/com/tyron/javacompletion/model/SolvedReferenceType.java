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

import com.google.auto.value.AutoValue;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

/** A solved type that is a simple reference type. */
@AutoValue
public abstract class SolvedReferenceType extends SolvedEntityType {
    @Override
    public abstract ClassEntity getEntity();

    public abstract SolvedTypeParameters getTypeParameters();

    @Override
    public TypeReference toTypeReference() {
        Deque<SimpleType> enclosingClasses = new ArrayDeque<>();
        Deque<String> packages = new ArrayDeque<>();
        Optional<Entity> parent =
                getEntity().getParentScope().map(scope -> scope.getDefiningEntity().orElse(null));
        ClassEntity lastClass = getEntity();
        SolvedTypeParameters typeParameters = getTypeParameters();
        while (parent.isPresent()) {
            if (parent.get() instanceof ClassEntity) {
                if (lastClass.isStatic()) {
                    typeParameters = SolvedTypeParameters.EMPTY;
                }
                lastClass = (ClassEntity) parent.get();
                enclosingClasses.addFirst(buildSimpleType(lastClass, typeParameters));
            } else if (parent.get() instanceof PackageEntity) {
                packages.addFirst(parent.get().getSimpleName());
            }
            parent = parent.get().getParentScope().map(scope -> scope.getDefiningEntity().orElse(null));
        }
        return TypeReference.formalizedBuilder()
                .setPrimitive(false)
                .setSimpleName(getEntity().getSimpleName())
                .setArray(false)
                .setEnclosingClasses(enclosingClasses)
                .setPackageName(packages)
                .setSimpleType(buildSimpleType(getEntity(), getTypeParameters()))
                .build();
    }

    private static SimpleType buildSimpleType(
            ClassEntity classEntity, SolvedTypeParameters solvedTypeParameters) {
        SimpleType.Builder builder =
                SimpleType.builder().setPrimitive(false).setSimpleName(classEntity.getSimpleName());
        for (TypeParameter typeParameter : classEntity.getTypeParameters()) {
            Optional<SolvedType> solvedTypeParameter =
                    solvedTypeParameters.getTypeParameter(typeParameter.getName());
            if (solvedTypeParameter.isPresent()) {
                builder.addTypeArgument(solvedTypeParameter.get().toTypeReference());
            } else {
                // TODO: Use bounds here.
                builder.addTypeArgument(WildcardTypeArgument.create(Optional.empty()));
            }
        }
        return builder.build();
    }

    public static SolvedReferenceType create(
            ClassEntity classEntity, SolvedTypeParameters typeParameters) {
        return new AutoValue_SolvedReferenceType(classEntity, typeParameters);
    }
}
