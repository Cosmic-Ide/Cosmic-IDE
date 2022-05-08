package com.pranav.javacompletion.parser;

import com.pranav.javacompletion.logging.JLogger;
import com.pranav.javacompletion.model.TypeArgument;
import com.pranav.javacompletion.model.TypeReference;

import org.openjdk.source.tree.ArrayTypeTree;
import org.openjdk.source.tree.IdentifierTree;
import org.openjdk.source.tree.MemberSelectTree;
import org.openjdk.source.tree.ParameterizedTypeTree;
import org.openjdk.source.tree.PrimitiveTypeTree;
import org.openjdk.source.tree.Tree;
import org.openjdk.source.util.TreeScanner;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

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
