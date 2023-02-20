package com.tyron.javacompletion.model.util;

import java.util.List;

/** Utilities for handling qualified names. */
public final class QualifiedNames {
    public static final String QUALIFIER_SEPARATOR = ".";

    private QualifiedNames() {}

    public static String formatQualifiedName(List<String> qualifiers, String simpleName) {
        if (qualifiers.isEmpty()) {
            return simpleName;
        }

        StringBuilder sb = new StringBuilder();
        for (String qualifier : qualifiers) {
            sb.append(qualifier).append(QUALIFIER_SEPARATOR);
        }
        sb.append(simpleName);
        return sb.toString();
    }
}