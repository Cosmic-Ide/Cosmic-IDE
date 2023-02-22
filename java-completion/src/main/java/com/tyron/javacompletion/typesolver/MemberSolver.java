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
package com.tyron.javacompletion.typesolver;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import com.tyron.javacompletion.logging.JLogger;
import com.tyron.javacompletion.model.ClassEntity;
import com.tyron.javacompletion.model.Entity;
import com.tyron.javacompletion.model.EntityWithContext;
import com.tyron.javacompletion.model.MethodEntity;
import com.tyron.javacompletion.model.Module;
import com.tyron.javacompletion.model.PrimitiveEntity;
import com.tyron.javacompletion.model.TypeArgument;
import com.tyron.javacompletion.model.VariableEntity;

/** Logic for finding the entity that defines the member of a class. */
public class MemberSolver {
    private static final JLogger logger = JLogger.createForEnclosingClass();

    private static final String IDENT_THIS = "this";
    private static final String IDENT_LENGTH = "length";
    private static final Set<Entity.Kind> ALLOWED_KINDS_NON_METHOD =
            new ImmutableSet.Builder<Entity.Kind>()
                    .addAll(ClassEntity.ALLOWED_KINDS)
                    .addAll(VariableEntity.ALLOWED_KINDS)
                    .add(Entity.Kind.QUALIFIER)
                    .build();

    private final TypeSolver typeSolver;
    private final OverloadSolver overloadSolver;

    public MemberSolver(TypeSolver typeSolver, OverloadSolver overloadSolver) {
        this.typeSolver = typeSolver;
        this.overloadSolver = overloadSolver;
    }

    public Optional<EntityWithContext> findNonMethodMember(
            String identifier, EntityWithContext baseEntity, Module module) {
        return findNonMethodMember(identifier, baseEntity, module, ALLOWED_KINDS_NON_METHOD);
    }

    public Optional<EntityWithContext> findNonMethodMember(
            String identifier,
            EntityWithContext baseEntity,
            Module module,
            Set<Entity.Kind> allowedKinds) {
        ////////
        // Array
        if (baseEntity.getArrayLevel() > 0) {
            if (IDENT_LENGTH.equals(identifier)) {
                return Optional.of(EntityWithContext.ofStaticEntity(PrimitiveEntity.INT));
            }
            return Optional.empty();
        }

        ///////
        // OuterClass.this
        if (baseEntity.getEntity() instanceof ClassEntity
                && !baseEntity.isInstanceContext()
                && IDENT_THIS.equals(identifier)) {
            return Optional.of(
                    baseEntity
                            .toBuilder()
                            .setInstanceContext(true)
                            .setSolvedTypeParameters(
                                    typeSolver.solveTypeParameters(
                                            ((ClassEntity) baseEntity.getEntity()).getTypeParameters(),
                                            ImmutableList.of(),
                                            baseEntity.getSolvedTypeParameters(),
                                            baseEntity.getEntity().getScope(),
                                            module))
                            .build());
        }

        ////////
        //  foo.bar
        return typeSolver.findEntityMember(identifier, baseEntity, module, allowedKinds);
    }

    /** @return a list of {@link MethodEntity} instances. */
    public List<EntityWithContext> findMethodMembers(
            String identifier, EntityWithContext baseEntity, Module module) {
        // Methods must be defined in classes.
        if (!(baseEntity.getEntity() instanceof ClassEntity)) {
            logger.warning(new Throwable(), "Cannot find method of non-class entities %s", baseEntity);
            return ImmutableList.of();
        }

        return typeSolver.findClassMethods(identifier, baseEntity, module);
    }
}