package com.tyron.javacompletion.model;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import java.util.Map;

/** A solved type that is primitive. */
@AutoValue
public abstract class SolvedPrimitiveType extends SolvedEntityType {

    private static final Map<PrimitiveEntity, TypeReference> TYPE_MAP =
            new ImmutableMap.Builder<PrimitiveEntity, TypeReference>()
                    .put(PrimitiveEntity.BYTE, TypeReference.BYTE_TYPE)
                    .put(PrimitiveEntity.SHORT, TypeReference.SHORT_TYPE)
                    .put(PrimitiveEntity.INT, TypeReference.INT_TYPE)
                    .put(PrimitiveEntity.LONG, TypeReference.LONG_TYPE)
                    .put(PrimitiveEntity.FLOAT, TypeReference.FLOAT_TYPE)
                    .put(PrimitiveEntity.DOUBLE, TypeReference.DOUBLE_TYPE)
                    .put(PrimitiveEntity.CHAR, TypeReference.CHAR_TYPE)
                    .put(PrimitiveEntity.BOOLEAN, TypeReference.BOOLEAN_TYPE)
                    .put(PrimitiveEntity.VOID, TypeReference.VOID_TYPE)
                    .build();

    @Override
    public abstract PrimitiveEntity getEntity();

    public static SolvedPrimitiveType create(PrimitiveEntity primitiveEntity) {
        return new AutoValue_SolvedPrimitiveType(primitiveEntity);
    }

    @Override
    public TypeReference toTypeReference() {
        return checkNotNull(
                TYPE_MAP.get(getEntity()),
                "Cannot convert primitive type %s to type reference.",
                getEntity());
    }
}