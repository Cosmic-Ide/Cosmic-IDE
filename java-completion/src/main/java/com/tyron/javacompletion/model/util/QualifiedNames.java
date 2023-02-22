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