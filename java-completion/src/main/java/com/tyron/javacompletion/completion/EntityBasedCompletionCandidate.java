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
