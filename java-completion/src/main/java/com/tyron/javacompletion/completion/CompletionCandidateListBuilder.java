package com.tyron.javacompletion.completion;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import androidx.annotation.Nullable;
import com.tyron.javacompletion.model.Entity;
import com.tyron.javacompletion.typesolver.EntityShadowingListBuilder;

/**
 * A builder for bulding a {@link List} of {@link CompletionCandidate} instances that dedups the
 * candidates with the same name using {@link EntityShadowingListBuilder}.
 */
public class CompletionCandidateListBuilder {
    private static final GetElementFunction GET_ELEMENT_FUNCTION = new GetElementFunction();

    private final Map<String, EntityShadowingListBuilder<CompletionCandidateWithMatchLevel>>
            candidateMap;
    private final String completionPrefix;

    public CompletionCandidateListBuilder(String completionPrefix) {
        candidateMap = new HashMap<>();
        this.completionPrefix = completionPrefix;
    }

    public boolean hasCandidateWithName(String name) {
        return candidateMap.containsKey(name);
    }

    public CompletionCandidateListBuilder addEntities(
            Multimap<String, Entity> entities, CompletionCandidate.SortCategory sortCategory) {
        for (Entity entity : entities.values()) {
            addEntity(entity, sortCategory);
        }
        return this;
    }

    public CompletionCandidateListBuilder addCandidates(Collection<CompletionCandidate> candidates) {
        for (CompletionCandidate candidate : candidates) {
            addCandidate(candidate);
        }
        return this;
    }

    public CompletionCandidateListBuilder addEntity(
            Entity entity, CompletionCandidate.SortCategory sortCategory) {
        return this.addCandidate(new EntityCompletionCandidate(entity, sortCategory));
    }

    public CompletionCandidateListBuilder addCandidate(CompletionCandidate candidate) {
        String name = candidate.getName();
        CompletionPrefixMatcher.MatchLevel matchLevel =
                CompletionPrefixMatcher.computeMatchLevel(name, completionPrefix);
        if (matchLevel == CompletionPrefixMatcher.MatchLevel.NOT_MATCH) {
            return this;
        }

        if (!candidateMap.containsKey(name)) {
            candidateMap.put(name, new EntityShadowingListBuilder<>(GET_ELEMENT_FUNCTION));
        }
        candidateMap.get(name).add(CompletionCandidateWithMatchLevel.create(candidate, matchLevel));
        return this;
    }

    public ImmutableList<CompletionCandidate> build() {
        return candidateMap.values().stream()
                .flatMap(EntityShadowingListBuilder::stream)
                .sorted()
                .map(CompletionCandidateWithMatchLevel::getCompletionCandidate)
                .collect(Collectors.collectingAndThen(Collectors.toList(), ImmutableList::copyOf));
    }

    private static class GetElementFunction
            implements Function<CompletionCandidateWithMatchLevel, Entity> {
        @Override
        @Nullable
        public Entity apply(CompletionCandidateWithMatchLevel candidateWithMatchLevel) {
            CompletionCandidate candidate = candidateWithMatchLevel.getCompletionCandidate();
            if (candidate instanceof EntityBasedCompletionCandidate) {
                return ((EntityBasedCompletionCandidate) candidate).getEntity();
            } else {
                return null;
            }
        }
    }
}