/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

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

import com.google.auto.value.AutoValue;

import java.util.Comparator;

/**
 * A wrapper of {@link CompletionCandidate} and {@link CompletionPrefixMatcher#computeMatchLevel(String, String)}.
 */
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

    public static CompletionCandidateWithMatchLevel create(
            CompletionCandidate completionCandidate, CompletionPrefixMatcher.MatchLevel matchLevel) {
        return new AutoValue_CompletionCandidateWithMatchLevel(completionCandidate, matchLevel);
    }

    public abstract CompletionCandidate getCompletionCandidate();

    public abstract CompletionPrefixMatcher.MatchLevel getMatchLevel();

    @Override
    public int compareTo(CompletionCandidateWithMatchLevel other) {
        return COMPARATOR.compare(this, other);
    }
}