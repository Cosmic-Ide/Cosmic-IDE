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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Range;
import java.util.Map;
import java.util.Optional;

/** Represents primitive types. */
public class PrimitiveEntity extends Entity {
    public static final PrimitiveEntity BYTE = new PrimitiveEntity("byte");
    public static final PrimitiveEntity SHORT = new PrimitiveEntity("short");
    public static final PrimitiveEntity INT = new PrimitiveEntity("int");
    public static final PrimitiveEntity LONG = new PrimitiveEntity("long");
    public static final PrimitiveEntity FLOAT = new PrimitiveEntity("float");
    public static final PrimitiveEntity DOUBLE = new PrimitiveEntity("double");
    public static final PrimitiveEntity CHAR = new PrimitiveEntity("char");
    public static final PrimitiveEntity BOOLEAN = new PrimitiveEntity("boolean");
    public static final PrimitiveEntity VOID = new PrimitiveEntity("void");

    private static final Map<String, PrimitiveEntity> TYPE_MAP =
            new ImmutableMap.Builder<String, PrimitiveEntity>()
                    .put(BYTE.getSimpleName(), BYTE)
                    .put(SHORT.getSimpleName(), SHORT)
                    .put(INT.getSimpleName(), INT)
                    .put(LONG.getSimpleName(), LONG)
                    .put(FLOAT.getSimpleName(), FLOAT)
                    .put(DOUBLE.getSimpleName(), DOUBLE)
                    .put(CHAR.getSimpleName(), CHAR)
                    .put(BOOLEAN.getSimpleName(), BOOLEAN)
                    .put(VOID.getSimpleName(), VOID)
                    .build();

    private static final Map<Class<?>, PrimitiveEntity> CLASS_MAP =
            new ImmutableMap.Builder<Class<?>, PrimitiveEntity>()
                    .put(Byte.class, BYTE)
                    .put(Short.class, SHORT)
                    .put(Integer.class, INT)
                    .put(Long.class, LONG)
                    .put(Float.class, FLOAT)
                    .put(Double.class, DOUBLE)
                    .put(Character.class, CHAR)
                    .put(Boolean.class, BOOLEAN)
                    .put(Void.class, VOID)
                    .build();

    public static boolean isPrimitive(String typeName) {
        return TYPE_MAP.containsKey(typeName);
    }

    public static PrimitiveEntity get(String simpleName) {
        if (!TYPE_MAP.containsKey(simpleName)) {
            // How can this happen?
            return new PrimitiveEntity(simpleName);
        }
        return TYPE_MAP.get(simpleName);
    }

    public static Optional<PrimitiveEntity> get(Class<?> valueClass) {
        return Optional.ofNullable(CLASS_MAP.get(valueClass));
    }

    private PrimitiveEntity(String simpleName) {
        super(
                simpleName,
                Entity.Kind.PRIMITIVE,
                ImmutableList.of() /* qualifiers */,
                true /* isStatic */,
                Optional.empty() /* javadoc */,
                Range.closedOpen(0, 0));
    }

    @Override
    public EmptyScope getScope() {
        return EmptyScope.INSTANCE;
    }

    @Override
    public Optional<EntityScope> getParentScope() {
        return Optional.empty();
    }

    @Override
    public String toString() {
        return "PrimitiveEntity: " + getSimpleName();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof PrimitiveEntity)) {
            return false;
        }
        PrimitiveEntity other = (PrimitiveEntity) o;

        return getSimpleName().equals(other.getSimpleName());
    }
}