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
import com.google.common.collect.Range;
import com.tyron.javacompletion.model.util.QualifiedNames;

import java.util.List;
import java.util.Optional;

/**
 * A Java entity declared in the source code.
 *
 * <p>A entity can be a leaf node (e.g variables) or a scope with more entities within it (e.g.
 * class, method).
 */
public abstract class Entity {

    public enum Kind {
        CLASS,
        INTERFACE,
        ANNOTATION,
        ENUM,
        METHOD,
        VARIABLE,
        FIELD,
        // Each part of a pacakage qualifier
        // e.g org.javacomp has 2 qualifiers: org and javacomp
        QUALIFIER,
        // A psuedo entity kind. Represents the a reference to a entity by name. May be resolved to its
        // referencing entity.
        REFERENCE,
        // A premitive type.
        PRIMITIVE,
        NULL,
    }

    private final String simpleName;
    private final List<String> qualifiers;
    private final Kind kind;
    private final boolean isStatic;
    private final Range<Integer> symbolRange;
    private final Optional<String> javadoc;

    protected Entity(
            String simpleName,
            Kind kind,
            List<String> qualifiers,
            boolean isStatic,
            Optional<String> javadoc,
            Range<Integer> symbolRange) {
        this.simpleName = simpleName;
        this.kind = kind;
        this.qualifiers = ImmutableList.copyOf(qualifiers);
        this.isStatic = isStatic;
        this.javadoc = javadoc;
        this.symbolRange = symbolRange;
    }

    /**
     * Gets the name of a entity without qualifiers. For example the simple name of foo.bar.ClassName
     * is ClassName.
     */
    public String getSimpleName() {
        return simpleName;
    }

    /** Gets the qualifiers of the name of the entity. */
    public List<String> getQualifiers() {
        return qualifiers;
    }

    /** Gets the qualified name in the form of qualifier1.qualifier2.simpleName */
    public String getQualifiedName() {
        return QualifiedNames.formatQualifiedName(qualifiers, simpleName);
    }

    public Kind getKind() {
        return kind;
    }

    public Optional<String> getJavadoc() {
        return javadoc;
    }

    public Range<Integer> getSymbolRange() {
        return symbolRange;
    }

    public boolean isStatic() {
        return isStatic;
    }

    /**
     * @return {@code true} if the entity is a member of a class instance, which can only be access in
     *     an instance context of that class.
     */
    public boolean isInstanceMember() {
        return !isStatic();
    }

    /** @return a {@link EntityScope} that this entity defines. */
    public abstract EntityScope getScope();

    /** @return a {@link EntityScope} that defines this entity. */
    public abstract Optional<EntityScope> getParentScope();
}