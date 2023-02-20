package com.tyron.javacompletion.completion;

import com.google.auto.value.AutoValue;
import java.util.Comparator;

/** A wrapper of {@link CompletionCandidate} and {@link CompletionPrefixMatcher#MatchLevel}. */
@AutoValue
public abstract class CompletionCandidateWithMatchLevel
        implements Comparable<CompletionCandidateWithMatchLevel> {
    private static final Comparator<CompletionCandidateWithMatchLevel> COMPARATOR =
            Comparator.comparing(
                            (CompletionCandidateWithMatchLevel candidateWithLevel) ->
                                    candidateWithLevel.getCompletionCandidate().getSortCategory().ordinal())
                    .thenComparing(
                            candidateWithLevel -> candidateWithLevel.getMatchLevel().ordinal(),
                            Comparator.reverseOrder())
                    .thenComparing(
                            candidateWithLevel -> candidateWithLevel.getCompletionCandidate().getName());

    public abstract CompletionCandidate getCompletionCandidate();

    public abstract CompletionPrefixMatcher.MatchLevel getMatchLevel();

    public static CompletionCandidateWithMatchLevel create(
            CompletionCandidate completionCandidate, CompletionPrefixMatcher.MatchLevel matchLevel) {
        return new AutoValue_CompletionCandidateWithMatchLevel(completionCandidate, matchLevel);
    }

    @Override
    public int compareTo(CompletionCandidateWithMatchLevel other) {
        return COMPARATOR.compare(this, other);
    }
}