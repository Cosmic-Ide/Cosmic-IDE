package com.tyron.javacompletion.completion;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import com.tyron.javacompletion.model.Entity;
import com.tyron.javacompletion.protocol.textdocument.CompletionItem.ResolveAction;
import com.tyron.javacompletion.protocol.textdocument.CompletionItem.ResolveActionParams;
import com.tyron.javacompletion.protocol.textdocument.CompletionItem.ResolveFormatJavadocParams;

/** A completion candidate backed by a {@link Entity}. */
abstract class EntityBasedCompletionCandidate implements CompletionCandidate {
    private final Entity entity;

    EntityBasedCompletionCandidate(Entity entity) {
        this.entity = entity;
    }

    Entity getEntity() {
        return entity;
    }

    @Override
    public Map<ResolveAction, ResolveActionParams> getResolveActions() {
        if (entity.getJavadoc().isPresent()) {
            return ImmutableMap.of(
                    ResolveAction.FORMAT_JAVADOC, new ResolveFormatJavadocParams(entity.getJavadoc().get()));
        }
        return ImmutableMap.of();
    }
}
