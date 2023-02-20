package com.tyron.javacompletion.completion;

import static com.google.common.base.Preconditions.checkState;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.HashSet;
import java.util.Set;
import com.tyron.javacompletion.completion.CompletionCandidate.SortCategory;
import com.tyron.javacompletion.model.ClassEntity;
import com.tyron.javacompletion.model.Entity;
import com.tyron.javacompletion.model.EntityWithContext;
import com.tyron.javacompletion.model.Module;
import com.tyron.javacompletion.typesolver.ExpressionSolver;
import com.tyron.javacompletion.typesolver.TypeSolver;

class ClassMemberCompletor {
    private final TypeSolver typeSolver;
    private final ExpressionSolver expressionSolver;

    ClassMemberCompletor(TypeSolver typeSolver, ExpressionSolver expressionSolver) {
        this.typeSolver = typeSolver;
        this.expressionSolver = expressionSolver;
    }

    ImmutableList<CompletionCandidate> getClassMembers(
            EntityWithContext actualClass, Module module, String prefix, Options options) {
        CompletionCandidateListBuilder builder = new CompletionCandidateListBuilder(prefix);
        boolean directMembers = true;
        Set<String> addedMethodNames = options.includeAllMethodOverloads() ? null : new HashSet<>();
        for (EntityWithContext classInHierachy : typeSolver.classHierarchy(actualClass, module)) {
            checkState(
                    classInHierachy.getEntity() instanceof ClassEntity,
                    "classHierarchy() returns non class entity %s for %s",
                    classInHierachy,
                    actualClass);
            for (Entity member :
                    ((ClassEntity) classInHierachy.getEntity()).getMemberEntities().values()) {
                member = typeSolver.applyTypeParameters(member, classInHierachy.getSolvedTypeParameters());
                if (!options.allowedKinds().contains(member.getKind())) {
                    continue;
                }
                if (!options.addBothInstanceAndStaticMembers()
                        && actualClass.isInstanceContext() != member.isInstanceMember()) {
                    continue;
                }
                if (!options.includeAllMethodOverloads() && member.getKind() == Entity.Kind.METHOD) {
                    if (addedMethodNames.contains(member.getSimpleName())) {
                        continue;
                    }
                    addedMethodNames.add(member.getSimpleName());
                }
                builder.addEntity(
                        member, directMembers ? SortCategory.DIRECT_MEMBER : SortCategory.ACCESSIBLE_SYMBOL);
            }
            directMembers = false;
        }
        return builder.build();
    }

    @AutoValue
    abstract static class Options {
        /**
         * If false, methods with the same name are merged together and represented as one candidate.
         * Otherwise each method is a separate candidate.
         */
        abstract boolean includeAllMethodOverloads();

        /**
         * If false, instance members are returned only if the parent is an instance, and static members
         * are returned only if the parent is a class itself.
         */
        abstract boolean addBothInstanceAndStaticMembers();

        abstract ImmutableSet<Entity.Kind> allowedKinds();

        static Builder builder() {
            return new AutoValue_ClassMemberCompletor_Options.Builder();
        }

        @AutoValue.Builder
        abstract static class Builder {
            abstract Builder includeAllMethodOverloads(boolean value);

            abstract Builder addBothInstanceAndStaticMembers(boolean value);

            abstract Builder allowedKinds(ImmutableSet<Entity.Kind> value);

            abstract Options build();
        }
    }
}