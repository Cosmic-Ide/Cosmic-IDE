package com.pranav.javacompletion.completion;

import com.google.common.collect.ImmutableList;
import com.pranav.javacompletion.project.PositionContext;

/** Action to perform the requested completion. */
interface CompletionAction {
    ImmutableList<CompletionCandidate> getCompletionCandidates(
            PositionContext positionContext, String prefix);
}
