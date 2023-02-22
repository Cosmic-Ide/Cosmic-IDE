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

import static com.google.common.base.Preconditions.checkState;

import com.google.auto.value.AutoValue;

/**
 * Reference to an {@link Entity} with additional contextual information for helping type solving.
 */
@AutoValue
public abstract class EntityWithContext {
    public abstract Entity getEntity();

    public abstract SolvedTypeParameters getSolvedTypeParameters();

    public abstract int getArrayLevel();

    public abstract boolean isInstanceContext();

    public abstract Builder toBuilder();

    public static Builder builder() {
        return new AutoValue_EntityWithContext.Builder();
    }

    public static Builder simpleBuilder() {
        return builder()
                .setArrayLevel(0)
                .setSolvedTypeParameters(SolvedTypeParameters.EMPTY)
                .setInstanceContext(true);
    }

    public static EntityWithContext ofEntity(Entity entity) {
        return simpleBuilder().setEntity(entity).build();
    }

    public static EntityWithContext ofStaticEntity(Entity entity) {
        return simpleBuilder().setEntity(entity).setInstanceContext(false).build();
    }

    public static Builder from(SolvedType solvedType) {
        if (solvedType instanceof SolvedArrayType) {
            return from(((SolvedArrayType) solvedType).getBaseType()).incrementArrayLevel();
        }
        if (solvedType instanceof SolvedEntityType) {
            SolvedEntityType solvedEntityType = (SolvedEntityType) solvedType;
            Builder builder =
                    builder()
                            .setArrayLevel(0)
                            .setEntity(solvedEntityType.getEntity())
                            .setInstanceContext(false);
            if (solvedType instanceof SolvedReferenceType) {
                builder.setSolvedTypeParameters(((SolvedReferenceType) solvedType).getTypeParameters());
            } else {
                builder.setSolvedTypeParameters(SolvedTypeParameters.EMPTY);
            }
            return builder;
        }

        throw new RuntimeException(
                "Cannot convert unsupported SolvedType to EntityWithContext: " + solvedType);
    }

    public SolvedType toSolvedType() {
        Entity entity = getEntity();
        SolvedType solvedType = null;
        if (entity instanceof PrimitiveEntity) {
            solvedType = SolvedPrimitiveType.create((PrimitiveEntity) entity);
        } else if (entity instanceof NullEntity) {
            solvedType = SolvedNullType.INSTANCE;
        } else if (entity instanceof ClassEntity) {
            solvedType = SolvedReferenceType.create((ClassEntity) entity, getSolvedTypeParameters());
        } else if (entity instanceof PackageEntity) {
            solvedType = SolvedPackageType.create((PackageEntity) entity);
        } else {
            throw new RuntimeException(
                    "Unsupported entity type " + entity + " for converting to SolvedType");
        }

        for (int i = 0; i < getArrayLevel(); i++) {
            solvedType = SolvedArrayType.create(solvedType);
        }

        return solvedType;
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder setEntity(Entity value);

        public abstract Builder setSolvedTypeParameters(SolvedTypeParameters value);

        public abstract Builder setArrayLevel(int value);

        public abstract Builder setInstanceContext(boolean value);

        public abstract int getArrayLevel();

        public Builder incrementArrayLevel() {
            return setArrayLevel(getArrayLevel() + 1);
        }

        public Builder decrementArrayLevel() {
            int currentArrayLevel = getArrayLevel();
            checkState(currentArrayLevel > 0, "Cannot decrement array level when it's already zero");
            return setArrayLevel(currentArrayLevel - 1);
        }

        public abstract EntityWithContext build();
    }
}