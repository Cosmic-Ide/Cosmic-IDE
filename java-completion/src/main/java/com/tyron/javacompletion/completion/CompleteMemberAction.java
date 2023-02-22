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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.sun.source.tree.ExpressionTree;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Optional;
import java.util.stream.Collectors;

import com.tyron.javacompletion.logging.JLogger;
import com.tyron.javacompletion.model.ClassEntity;
import com.tyron.javacompletion.model.Entity;
import com.tyron.javacompletion.model.EntityWithContext;
import com.tyron.javacompletion.project.PositionContext;
import com.tyron.javacompletion.typesolver.ExpressionSolver;
import com.tyron.javacompletion.typesolver.TypeSolver;

/**
 * An action to get completion candidates for member selection.
 */
class CompleteMemberAction implements CompletionAction {
    private static final JLogger logger = JLogger.createForEnclosingClass();

    private static final ClassMemberCompletor.Options MEMBER_SELECT_OPTIONS =
            ClassMemberCompletor.Options.builder()
                    .allowedKinds(Sets.immutableEnumSet(EnumSet.allOf(Entity.Kind.class)))
                    .addBothInstanceAndStaticMembers(false)
                    .includeAllMethodOverloads(true)
                    .build();
    private static final ClassMemberCompletor.Options METHOD_REFERENCE_OPTIONS =
            ClassMemberCompletor.Options.builder()
                    .allowedKinds(Sets.immutableEnumSet(Entity.Kind.METHOD))
                    .addBothInstanceAndStaticMembers(false)
                    .includeAllMethodOverloads(false)
                    .build();
    private static final ClassMemberCompletor.Options IMPORT_OPTIONS =
            ClassMemberCompletor.Options.builder()
                    .allowedKinds(
                            new ImmutableSet.Builder<Entity.Kind>()
                                    .addAll(ClassEntity.ALLOWED_KINDS)
                                    .add(Entity.Kind.QUALIFIER)
                                    .build())
                    .addBothInstanceAndStaticMembers(false)
                    .includeAllMethodOverloads(false)
                    .build();
    private static final ClassMemberCompletor.Options IMPORT_STATIC_OPTIONS =
            ClassMemberCompletor.Options.builder()
                    .allowedKinds(Sets.immutableEnumSet(EnumSet.allOf(Entity.Kind.class)))
                    .addBothInstanceAndStaticMembers(false)
                    .includeAllMethodOverloads(false)
                    .build();
    private final ExpressionTree parentExpression;
    private final TypeSolver typeSolver;
    private final ExpressionSolver expressionSolver;
    private final ClassMemberCompletor.Options options;

    private CompleteMemberAction(
            ExpressionTree parentExpression,
            TypeSolver typeSolver,
            ExpressionSolver expressionSolver,
            ClassMemberCompletor.Options options) {
        this.parentExpression = parentExpression;
        this.typeSolver = typeSolver;
        this.expressionSolver = expressionSolver;
        this.options = options;
    }

    static CompleteMemberAction forMemberSelect(
            ExpressionTree parentExpression, TypeSolver typeSolver, ExpressionSolver expressionSolver) {
        return new CompleteMemberAction(
                parentExpression, typeSolver, expressionSolver, MEMBER_SELECT_OPTIONS);
    }

    static CompleteMemberAction forMethodReference(
            ExpressionTree parentExpression, TypeSolver typeSolver, ExpressionSolver expressionSolver) {
        return new CompleteMemberAction(
                parentExpression, typeSolver, expressionSolver, METHOD_REFERENCE_OPTIONS);
    }

    static CompleteMemberAction forImport(
            ExpressionTree parentExpression, TypeSolver typeSolver, ExpressionSolver expressionSolver) {
        return new CompleteMemberAction(parentExpression, typeSolver, expressionSolver, IMPORT_OPTIONS);
    }

    static CompleteMemberAction forImportStatic(
            ExpressionTree parentExpression, TypeSolver typeSolver, ExpressionSolver expressionSolver) {
        return new CompleteMemberAction(
                parentExpression, typeSolver, expressionSolver, IMPORT_STATIC_OPTIONS);
    }

    @Override
    public ImmutableList<CompletionCandidate> getCompletionCandidates(
            PositionContext positionContext, String completionPrefix) {
        Optional<EntityWithContext> solvedParent =
                expressionSolver.solve(
                        parentExpression,
                        positionContext.getModule(),
                        positionContext.getScopeAtPosition(),
                        positionContext.getPosition());
        logger.fine("Solved parent expression: %s", solvedParent);
        if (!solvedParent.isPresent()) {
            return ImmutableList.of();
        }

        if (solvedParent.get().getArrayLevel() > 0) {
            return ImmutableList.of(new CompletionCandidate() {
                @Override
                public String getName() {
                    return "length";
                }

                @Override
                public Kind getKind() {
                    return Kind.FIELD;
                }

                @Override
                public Optional<String> getDetail() {
                    return Optional.of("int");
                }
            });
        }

        if (solvedParent.get().getEntity() instanceof ClassEntity) {
            return new ClassMemberCompletor(typeSolver, expressionSolver)
                    .getClassMembers(
                            solvedParent.get(), positionContext.getModule(), completionPrefix, options);
        }

        // Parent is a package.
        return completePackageMembers(
                solvedParent.get().getEntity().getScope().getMemberEntities().values(), completionPrefix);
    }

    private ImmutableList<CompletionCandidate> completePackageMembers(
            Collection<Entity> entities, String completionPrefix) {
        return entities.stream()
                .filter((entity) -> options.allowedKinds().contains(entity.getKind())
                        && CompletionPrefixMatcher.matches(entity.getSimpleName(), completionPrefix))
                .map((entity) ->
                        new EntityCompletionCandidate(
                                entity, CompletionCandidate.SortCategory.DIRECT_MEMBER))
                .collect(Collectors.collectingAndThen(Collectors.toList(), ImmutableList::copyOf));
    }
}