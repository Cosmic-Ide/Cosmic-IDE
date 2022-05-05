package com.pranav.javacompletion.completion;

import com.google.common.collect.ImmutableMap;
import com.pranav.javacompletion.protocol.textdocument.CompletionItem.ResolveAction;
import com.pranav.javacompletion.protocol.textdocument.CompletionItem.ResolveActionParams;

import java.util.Map;
import java.util.Optional;

public interface CompletionCandidate {
    public enum Kind {
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
     * <p>Candidates are sorted by SortCategory first, then by their other charactistics such as the
     * label.
     *
     * <p>SortCategory values are compared using their ordianl values. Do not change the order of the
     * values unless there is a good reason.
     */
    public enum SortCategory {
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