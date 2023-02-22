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

import com.google.common.collect.ImmutableMap;
import com.tyron.javacompletion.protocol.textdocument.CompletionItem.ResolveAction;
import com.tyron.javacompletion.protocol.textdocument.CompletionItem.ResolveActionParams;

import java.util.Map;
import java.util.Optional;

public interface CompletionCandidate {
    enum Kind {
        UNKNOWN,
        CLASS,
        INTERFACE,
        ENUM,
        METHOD,
        VARIABLE,
        FIELD,
        PACKAGE,
        KEYWORD,
    }

    /**
     * The category a candidate belongs to for sorting purposes.
     *
     * <p>Candidates are sorted by SortCategory first, then by their other characteristics such as the
     * label.
     *
     * <p>SortCategory values are compared using their ordinal values. Do not change the order of the
     * values unless there is a good reason.
     */
    enum SortCategory {
        /** A member name defined by the class of the instance. */
        DIRECT_MEMBER,
        /** A symbol name that's visible in the given scope. */
        ACCESSIBLE_SYMBOL,
        /** Other names in undefined categories that have the normal rank. */
        UNKNOWN,
        /** All Java keywords. */
        KEYWORD,
        /** Entity names that are not visible in the given scope. They need to be imported. */
        TO_IMPORT,
    }

    String getName();

    Kind getKind();

    Optional<String> getDetail();

    default Optional<String> getInsertPlainText(TextEditOptions textEditOptions) {
        return Optional.empty();
    }

    default Optional<String> getInsertSnippet(TextEditOptions textEditOptions) {
        return Optional.empty();
    }

    default SortCategory getSortCategory() {
        return SortCategory.UNKNOWN;
    }

    default Map<ResolveAction, ResolveActionParams> getResolveActions() {
        return ImmutableMap.of();
    }
}