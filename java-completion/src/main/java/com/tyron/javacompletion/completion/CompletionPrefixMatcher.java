package com.tyron.javacompletion.completion;

/** Logic of matching a completion name with a given completion prefix. */
public class CompletionPrefixMatcher {

    /**
     * How well does the candidate name match the compleation prefix.
     *
     * <p>The ordinal values of the enum values imply the match level. The greater the ordinal value
     * is the better the candidate matches. It can be a key for sorting the matched candidates. New
     * value should be added to the right place to keep the ordinal value in order.
     */
    public enum MatchLevel {
        NOT_MATCH,
        CASE_INSENSITIVE_PREFIX,
        CASE_SENSITIVE_PREFIX,
        CASE_INSENSITIVE_EQUAL,
        CASE_SENSITIVE_EQUAL,
    }

    /** Returns how well does {@code candidateName} match {@code completionPrefix}. */
    public static MatchLevel computeMatchLevel(String candidateName, String completionPrefix) {
        if (candidateName.startsWith(completionPrefix)) {
            return candidateName.length() == completionPrefix.length()
                    ? MatchLevel.CASE_SENSITIVE_EQUAL
                    : MatchLevel.CASE_SENSITIVE_PREFIX;
        }

        if (candidateName.toLowerCase().startsWith(completionPrefix.toLowerCase())) {
            return candidateName.length() == completionPrefix.length()
                    ? MatchLevel.CASE_INSENSITIVE_EQUAL
                    : MatchLevel.CASE_INSENSITIVE_PREFIX;
        }

        return MatchLevel.NOT_MATCH;
    }

    /** Returns {@code true} if {@code candidateName} matches {@code completionPrefix}. */
    public static boolean matches(String candidateName, String completionPrefix) {
        return computeMatchLevel(candidateName, completionPrefix) != MatchLevel.NOT_MATCH;
    }
}