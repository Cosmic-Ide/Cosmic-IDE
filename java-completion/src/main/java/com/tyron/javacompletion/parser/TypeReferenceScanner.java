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
package com.tyron.javacompletion.parser;

import com.sun.source.tree.ArrayTypeTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.PrimitiveTypeTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreeScanner;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import com.tyron.javacompletion.logging.JLogger;
import com.tyron.javacompletion.model.TypeArgument;
import com.tyron.javacompletion.model.TypeReference;

public class TypeReferenceScanner extends TreeScanner<Void, Void> {
    private static final JLogger logger = JLogger.createForEnclosingClass();

    private final Deque<String> names;
    private final List<TypeArgument> typeArguments;
    private final TypeArgumentScanner typeArgumentScanner;
    private boolean isPrimitive;
    private boolean isArray;

    public TypeReferenceScanner() {
        this(new TypeArgumentScanner());
    }

    public TypeReferenceScanner(TypeArgumentScanner typeArgumentScanner) {
        this.typeArgumentScanner = typeArgumentScanner;
        this.names = new ArrayDeque<>();
        this.typeArguments = new ArrayList<>();
        this.isPrimitive = false;
        this.isArray = false;
    }

    public TypeReference getTypeReference(Tree node) {
        names.clear();
        isPrimitive = false;
        isArray = false;
        typeArguments.clear();
        scan(node, null);
        if (names.isEmpty()) {
            // Malformed input, no type can be referenced
            logger.warning(new Throwable(), "Empty type name with %s", node);
            return TypeReference.EMPTY_TYPE;
        }
        return TypeReference.builder()
                .setFullName(names)
                .setPrimitive(isPrimitive)
                .setArray(isArray)
                .setTypeArguments(typeArguments)
                .build();
    }

    @Override
    public Void visitParameterizedType(ParameterizedTypeTree node, Void unused) {
        scan(node.getType(), unused);
        for (Tree typeArgument : node.getTypeArguments()) {
            typeArguments.add(typeArgumentScanner.getTypeArgument(typeArgument));
        }
        return null;
    }

    @Override
    public Void visitArrayType(ArrayTypeTree node, Void unused) {
        isArray = true;
        scan(node.getType(), unused);
        return null;
    }

    @Override
    public Void visitMemberSelect(MemberSelectTree node, Void unused) {
        names.addFirst(node.getIdentifier().toString());
        scan(node.getExpression(), unused);
        return null;
    }

    @Override
    public Void visitIdentifier(IdentifierTree node, Void unused) {
        names.addFirst(node.getName().toString());
        return null;
    }

    @Override
    public Void visitPrimitiveType(PrimitiveTypeTree node, Void unused) {
        isPrimitive = true;
        names.addFirst(node.getPrimitiveTypeKind().name().toLowerCase());
        return null;
    }
}