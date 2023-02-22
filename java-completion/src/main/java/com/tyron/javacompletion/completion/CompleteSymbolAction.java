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

import com.tyron.javacompletion.typesolver.ExpressionSolver;
import com.tyron.javacompletion.typesolver.TypeSolver;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import com.tyron.javacompletion.completion.CompletionCandidate.SortCategory;
import com.tyron.javacompletion.logging.JLogger;
import com.tyron.javacompletion.model.ClassEntity;
import com.tyron.javacompletion.model.Entity;
import com.tyron.javacompletion.model.EntityScope;
import com.tyron.javacompletion.model.EntityWithContext;
import com.tyron.javacompletion.model.FileScope;
import com.tyron.javacompletion.model.MethodEntity;
import com.tyron.javacompletion.model.Module;
import com.tyron.javacompletion.model.PackageScope;
import com.tyron.javacompletion.model.VariableEntity;
import com.tyron.javacompletion.project.PositionContext;
import com.tyron.javacompletion.typesolver.ExpressionSolver;
import com.tyron.javacompletion.typesolver.TypeSolver;

/** An action that returns any visible entities as completion candidates. */
class CompleteSymbolAction implements CompletionAction {
    private static final JLogger logger = JLogger.createForEnclosingClass();

    private static final List<String> JAVA_LANG_QUALIFIERS = ImmutableList.of("java", "lang");
    private static final Set<Entity.Kind> METHOD_VARIABLE_KINDS =
            new ImmutableSet.Builder<Entity.Kind>()
                    .addAll(VariableEntity.ALLOWED_KINDS)
                    .add(Entity.Kind.METHOD)
                    .build();
    private static final ClassMemberCompletor.Options CLASS_SCOPE_COMPLETE_OPTIONS =
            ClassMemberCompletor.Options.builder()
                    .allowedKinds(Sets.immutableEnumSet(EnumSet.allOf(Entity.Kind.class)))
                    // TODO: In static scope only static members are accessible.
                    .addBothInstanceAndStaticMembers(true)
                    .includeAllMethodOverloads(true)
                    .build();

    private final TypeSolver typeSolver;
    private final ClassMemberCompletor classMemberCompletor;

    CompleteSymbolAction(TypeSolver typeSolver, ExpressionSolver expressionSolver) {
        this.typeSolver = typeSolver;
        this.classMemberCompletor = new ClassMemberCompletor(typeSolver, expressionSolver);
    }

    @Override
    public ImmutableList<CompletionCandidate> getCompletionCandidates(
            PositionContext positionContext, String completionPrefix) {
        CompletionCandidateListBuilder builder = new CompletionCandidateListBuilder(completionPrefix);
        addKeywords(builder);
        for (EntityScope currentScope = positionContext.getScopeAtPosition();
             currentScope != null;
             currentScope = currentScope.getParentScope().orElse(null)) {
            logger.fine("Adding member entities in scope: %s", currentScope);
            if (currentScope instanceof ClassEntity) {
                builder.addCandidates(
                        classMemberCompletor.getClassMembers(
                                EntityWithContext.ofEntity((ClassEntity) currentScope),
                                positionContext.getModule(),
                                completionPrefix,
                                CLASS_SCOPE_COMPLETE_OPTIONS));
            } else if (currentScope instanceof FileScope) {
                FileScope fileScope = (FileScope) currentScope;
                builder.addEntities(
                        getPackageMembers((FileScope) currentScope, positionContext.getModule()),
                        SortCategory.ACCESSIBLE_SYMBOL);
                addImportedEntities(builder, fileScope, positionContext.getModule());
            } else {
                builder.addEntities(currentScope.getMemberEntities(), SortCategory.DIRECT_MEMBER);
            }
        }
        builder.addEntities(
                typeSolver.getAggregateRootPackageScope(positionContext.getModule()).getMemberEntities(),
                SortCategory.UNKNOWN);

        Optional<PackageScope> javaLangPackage =
                typeSolver.findPackageInModule(JAVA_LANG_QUALIFIERS, positionContext.getModule());
        if (javaLangPackage.isPresent()) {
            builder.addEntities(
                    javaLangPackage.get().getMemberEntities(), SortCategory.ACCESSIBLE_SYMBOL);
        }

        addClassesForImport(
                builder,
                positionContext.getModule(),
                completionPrefix,
                positionContext.getFileScope().getFilename());

        return builder.build();
    }

    private Multimap<String, Entity> getPackageMembers(FileScope fileScope, Module module) {
        PackageScope packageScope = module.getPackageForFile(fileScope);
        return packageScope.getMemberEntities();
    }

    private void addImportedEntities(
            CompletionCandidateListBuilder builder, FileScope fileScope, Module module) {
        // import foo.Bar;
        for (List<String> fullClassName : fileScope.getAllImportedClasses()) {
            Optional<ClassEntity> importedEntity =
                    typeSolver.findClassInModule(fullClassName, module, true /* useCanonicalName */);
            if (importedEntity.isPresent()) {
                builder.addEntity(importedEntity.get(), SortCategory.ACCESSIBLE_SYMBOL);
            } else {
                String simpleName = fullClassName.get(fullClassName.size() - 1);
                if (!builder.hasCandidateWithName(simpleName)) {
                    builder.addCandidate(
                            SimpleCompletionCandidate.builder()
                                    .setName(simpleName)
                                    .setKind(CompletionCandidate.Kind.CLASS)
                                    .build());
                }
            }
        }

        // import static foo.Bar.BAZ;
        for (List<String> fullMemberName : fileScope.getAllImportedStaticMembers()) {
            ClassEntity enclosingClass =
                    typeSolver.solveClassOfStaticImport(fullMemberName, fileScope, module).orElse(null);
            if (enclosingClass == null) {
                continue;
            }
            String name = fullMemberName.get(fullMemberName.size() - 1);
            for (Entity member : enclosingClass.getMemberEntities().get(name)) {
                if (!member.isStatic()) {
                    continue;
                }

                if (member instanceof MethodEntity || member instanceof VariableEntity) {
                    builder.addEntity(member, CompletionCandidate.SortCategory.ACCESSIBLE_SYMBOL);
                }
            }
        }

        // import foo.Bar.*;
        addOnDemandImportedEntities(
                builder, fileScope.getOnDemandClassImportQualifiers(), module, ClassEntity.ALLOWED_KINDS);

        // import static foo.Bar.*;
        addOnDemandImportedEntities(
                builder, fileScope.getOnDemandStaticImportQualifiers(), module, METHOD_VARIABLE_KINDS);
    }

    private void addOnDemandImportedEntities(
            CompletionCandidateListBuilder builder,
            List<List<String>> importedQualifiers,
            Module module,
            Set<Entity.Kind> allowedKinds) {
        for (List<String> qualifiers : importedQualifiers) {
            Entity enclosingClassOrPackage =
                    typeSolver.findClassOrPackageInModule(qualifiers, module).orElse(null);
            if (enclosingClassOrPackage == null) {
                continue;
            }

            for (Entity member : enclosingClassOrPackage.getScope().getMemberEntities().values()) {
                if (member.isStatic() && allowedKinds.contains(member.getKind())) {
                    builder.addEntity(member, CompletionCandidate.SortCategory.ACCESSIBLE_SYMBOL);
                }
            }
        }
    }

    private void addKeywords(CompletionCandidateListBuilder builder) {
        // TODO: add only keywords that are available for the current context.
        for (KeywordCompletionCandidate keyword : KeywordCompletionCandidate.values()) {
            builder.addCandidate(keyword);
        }
    }

    private void addClassesForImport(
            CompletionCandidateListBuilder builder, Module module, String prefix, String filename) {
        List<ClassEntity> classes = new AllEntitiesCompletor().getAllClasses(module, prefix);
        for (ClassEntity classEntity : classes) {
            builder.addCandidate(new ClassForImportCandidate(classEntity, filename));
        }
    }
}