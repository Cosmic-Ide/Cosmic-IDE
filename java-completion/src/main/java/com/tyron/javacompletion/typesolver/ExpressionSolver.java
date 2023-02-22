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

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.PrimitiveTypeTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.tree.JCTree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import androidx.annotation.Nullable;
import com.tyron.javacompletion.logging.JLogger;
import com.tyron.javacompletion.model.ClassEntity;
import com.tyron.javacompletion.model.Entity;
import com.tyron.javacompletion.model.EntityScope;
import com.tyron.javacompletion.model.EntityWithContext;
import com.tyron.javacompletion.model.MethodEntity;
import com.tyron.javacompletion.model.Module;
import com.tyron.javacompletion.model.NullEntity;
import com.tyron.javacompletion.model.PrimitiveEntity;
import com.tyron.javacompletion.model.SolvedReferenceType;
import com.tyron.javacompletion.model.SolvedType;
import com.tyron.javacompletion.model.SolvedTypeParameters;
import com.tyron.javacompletion.model.TypeArgument;
import com.tyron.javacompletion.model.TypeParameter;
import com.tyron.javacompletion.model.TypeReference;
import com.tyron.javacompletion.model.VariableEntity;
import com.tyron.javacompletion.parser.TypeArgumentScanner;
import com.tyron.javacompletion.parser.TypeReferenceScanner;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;

/**
 * Logic for solving the result type of an expression.
 */
public class ExpressionSolver {
    private static final JLogger logger = JLogger.createForEnclosingClass();

    private static final Set<Entity.Kind> ALL_ENTITY_KINDS = EnumSet.allOf(Entity.Kind.class);
    private static final Set<Entity.Kind> ALLOWED_KINDS_METHOD = ImmutableSet.of(Entity.Kind.METHOD);
    private static final Set<Entity.Kind> CLASS_LIKE_OR_PACKAGE_KINDS =
            new ImmutableSet.Builder<Entity.Kind>()
                    .addAll(ClassEntity.ALLOWED_KINDS)
                    .add(Entity.Kind.QUALIFIER)
                    .build();
    private static final String IDENT_THIS = "this";
    private static final String IDENT_SUPER = "super";
    private static final String IDENT_LENGTH = "length";

    private final TypeSolver typeSolver;
    private final OverloadSolver overloadSolver;
    private final MemberSolver memberSolver;
    private final ExpressionDefinitionScanner expressionDefinitionScanner;
    private final TypeArgumentScanner typeArgumentScanner;

    public ExpressionSolver(
            TypeSolver typeSolver, OverloadSolver overloadSolver, MemberSolver memberSolver) {
        this.typeSolver = typeSolver;
        this.overloadSolver = overloadSolver;
        this.memberSolver = memberSolver;
        this.expressionDefinitionScanner = new ExpressionDefinitionScanner();
        this.typeArgumentScanner = new TypeArgumentScanner();
    }

    /**
     * @param position the position in the file that the expression is being solved. It's useful for
     *                 filtering out variables defined after the position. It's ignored if set to negative value.
     */
    public Optional<EntityWithContext> solve(
            ExpressionTree expression, Module module, EntityScope baseScope, int position) {
        List<EntityWithContext> definitions =
                solveDefinitions(expression, module, baseScope, position, ALL_ENTITY_KINDS);
        return Optional.ofNullable(solveEntityType(definitions, module));
    }

    /**
     * Solve all entities that defines the given expression.
     *
     * <p>For methods, all overloads are returned. The best matched method is the first element.
     *
     * @param position the position in the file that the expression is being solved. It's useful for
     *                 filtering out variables defined after the position. It's ignored if set to negative value.
     */
    public List<EntityWithContext> solveDefinitions(
            ExpressionTree expression,
            Module module,
            EntityScope baseScope,
            int position,
            Set<Entity.Kind> allowedKinds) {
        ExpressionDefinitionScannerParams params =
                ExpressionDefinitionScannerParams.builder()
                        .module(module)
                        .baseScope(baseScope)
                        .position(position)
                        .allowedEntityKinds(ImmutableSet.copyOf(allowedKinds))
                        .contextTypeParameters(typeSolver.solveTypeParametersFromScope(baseScope, module))
                        .build();
        List<EntityWithContext> entities = expressionDefinitionScanner.scan(expression, params);
        if (entities == null) {
            logger.warning(
                    new Throwable(),
                    "Unsupported expression: (%s) %s",
                    expression.getClass().getSimpleName(),
                    expression);
            return ImmutableList.of();
        }

        logger.fine("Found definitions for %s: %s", expression, entities);
        return entities.stream()
                .filter(entityWithContext -> allowedKinds.contains(entityWithContext.getEntity().getKind()))
                .collect(collectingAndThen(toList(), ImmutableList::copyOf));
    }

    @Nullable
    private EntityWithContext solveEntityType(List<EntityWithContext> foundEntities, Module module) {
        if (foundEntities.isEmpty()) {
            return null;
        }

        EntityWithContext entityWithContext = foundEntities.get(0);
        SolvedTypeParameters solvedTypeParameters = entityWithContext.getSolvedTypeParameters();
        Entity entity = entityWithContext.getEntity();
        if (entity instanceof MethodEntity) {
            MethodEntity methodEntity = (MethodEntity) entity;
            if (methodEntity.isConstructor()) {
                return entityWithContext
                        .toBuilder()
                        .setEntity(methodEntity.getParentClass())
                        .setInstanceContext(true)
                        .build();
            } else {
                return typeSolver
                        .solve(methodEntity.getReturnType(), solvedTypeParameters, methodEntity, module)
                        .map(solvedType -> EntityWithContext.from(solvedType).setInstanceContext(true).build())
                        .orElse(null);
            }
        }
        if (entity instanceof VariableEntity) {
            VariableEntity variableEntity = (VariableEntity) entity;
            return typeSolver
                    .solve(variableEntity.getType(),
                            solvedTypeParameters,
                            variableEntity.getParentScope().get(),
                            module)
                    .map(solvedType -> EntityWithContext.from(solvedType).setInstanceContext(true).build())
                    .orElse(null);
        }
        return entityWithContext;
    }

    private class ExpressionDefinitionScanner
            extends TreeScanner<List<EntityWithContext>, ExpressionDefinitionScannerParams> {

        @Override
        public List<EntityWithContext> visitMethodInvocation(
                MethodInvocationTree node, ExpressionDefinitionScannerParams params) {
            if (!params.allowedEntityKinds().contains(Entity.Kind.METHOD)) {
                return ImmutableList.of();
            }
            List<Optional<SolvedType>> methodArgs = new ArrayList<>(node.getArguments().size());
            for (ExpressionTree arg : node.getArguments()) {
                methodArgs.add(
                        solve(arg, params.module(), params.baseScope(), ((JCTree) arg).getStartPosition())
                                .map(EntityWithContext::toSolvedType));
            }
            // We only need to solve model entities that matches the model invocation expression.
            ExpressionDefinitionScannerParams methodOnlyParams =
                    params.toBuilder().allowedEntityKinds(ALLOWED_KINDS_METHOD).build();
            List<EntityWithContext> methods = scan(node.getMethodSelect(), methodOnlyParams);

            methods = overloadSolver.prioritizeMatchedMethod(methods, methodArgs, params.module());
            return applyTypeArguments(methods, node.getTypeArguments(), params);
        }

        @Override
        public List<EntityWithContext> visitPrimitiveType(
                PrimitiveTypeTree node, ExpressionDefinitionScannerParams params) {
            return ImmutableList.of(
                    EntityWithContext.ofStaticEntity(
                            PrimitiveEntity.get(node.getPrimitiveTypeKind().name().toLowerCase())));
        }

        @Override
        public List<EntityWithContext> visitNewClass(
                NewClassTree node, ExpressionDefinitionScannerParams params) {
            ExpressionDefinitionScannerParams baseClassParams;
            if (node.getEnclosingExpression() != null) {
                // <EnclosingExpression>.new <identifier>(...).
                EntityWithContext enclosingClass =
                        solveEntityType(
                                scan(node.getEnclosingExpression(), params.copyWithAllEntityKindsAllowed()),
                                params.module());
                if (enclosingClass == null || !(enclosingClass.getEntity() instanceof ClassEntity)) {
                    return ImmutableList.of();
                }
                baseClassParams =
                        ExpressionDefinitionScannerParams.builder()
                                .module(params.module())
                                .baseScope(enclosingClass.getEntity().getScope())
                                .position(-1) /* position is useless for solving classes. */
                                .contextTypeParameters(params.contextTypeParameters())
                                .allowedEntityKinds(ClassEntity.ALLOWED_KINDS)
                                .build();
            } else {
                baseClassParams = params.toBuilder().allowedEntityKinds(ClassEntity.ALLOWED_KINDS).build();
            }

            List<EntityWithContext> baseClassEntities = scan(node.getIdentifier(), baseClassParams);

            if (baseClassEntities.isEmpty()) {
                return ImmutableList.of();
            }
            EntityWithContext entityWithContext = baseClassEntities.get(0);
            if (!(entityWithContext.getEntity() instanceof ClassEntity)) {
                logger.warning(
                        "Resolved entity for new class %s is not an entity: %s.",
                        node, entityWithContext.getEntity());
                return ImmutableList.of();
            }

            // Get constructors from the class entity and sort them based on the parameters.

            List<EntityWithContext> constructors =
                    ((ClassEntity) entityWithContext.getEntity())
                            .getConstructors().stream()
                            .map(
                                    methodEntity ->
                                            EntityWithContext.simpleBuilder()
                                                    .setEntity(methodEntity)
                                                    .setSolvedTypeParameters(entityWithContext.getSolvedTypeParameters())
                                                    .build())
                            .collect(Collectors.toList());
            if (constructors.isEmpty()) {
                // No constructors defined. Fallback to the class.
                return applyTypeArguments(
                        baseClassEntities.stream()
                                .map(baseClass -> baseClass.toBuilder().setInstanceContext(true).build())
                                .collect(Collectors.toList()),
                        node.getTypeArguments(),
                        params);
            }
            List<Optional<SolvedType>> arguments =
                    node.getArguments().stream()
                            .map(arg -> solve(arg,
                                    params.module(),
                                    params.baseScope(),
                                    ((JCTree) arg).getStartPosition())
                                    .map(EntityWithContext::toSolvedType))
                            .collect(Collectors.toList());

            constructors =
                    overloadSolver.prioritizeMatchedMethod(constructors, arguments, params.module());
            return applyTypeArguments(constructors, node.getTypeArguments(), params);
        }

        @Override
        public List<EntityWithContext> visitParameterizedType(
                ParameterizedTypeTree node, ExpressionDefinitionScannerParams params) {
            // TODO: handle diamond operator.
            List<EntityWithContext> entities = scan(node.getType(), params);
            return applyTypeArguments(entities, node.getTypeArguments(), params);
        }

        @Override
        public List<EntityWithContext> visitMemberSelect(
                MemberSelectTree node, ExpressionDefinitionScannerParams params) {
            List<EntityWithContext> expressionEntities =
                    scan(node.getExpression(), params.copyWithAllEntityKindsAllowed());
            logger.severe(
                    "[DEBUG] expression for %s with allowed %s are %s",
                    node, params.allowedEntityKinds(), expressionEntities);
            EntityWithContext expressionType = solveEntityType(expressionEntities, params.module());
            if (expressionType == null) {
                return ImmutableList.of();
            }

            String identifier = node.getIdentifier().toString();

            // When the member select expression is for a method invocation, this method is called by
            // visitMethodInvocation(), which passes the params with ALLOWED_KINDS_METHOD.
            if (params.allowedEntityKinds().equals(ALLOWED_KINDS_METHOD)) {
                return ImmutableList.copyOf(
                        memberSolver.findMethodMembers(identifier, expressionType, params.module()));
            } else {
                // Not called from visitMethodInvocation(), so we are not looking for methods.
                return toList(
                        memberSolver.findNonMethodMember(
                                identifier, expressionType, params.module(), params.allowedEntityKinds()));
            }
        }

        @Override
        public List<EntityWithContext> visitArrayAccess(
                ArrayAccessTree node, ExpressionDefinitionScannerParams params) {
            EntityWithContext expType =
                    solveEntityType(scan(node.getExpression(), params), params.module());
            if (expType == null || expType.getArrayLevel() == 0) {
                return ImmutableList.of();
            }

            return ImmutableList.of(expType.toBuilder().decrementArrayLevel().build());
        }

        @Override
        public List<EntityWithContext> visitIdentifier(
                IdentifierTree node, ExpressionDefinitionScannerParams params) {
            String identifier = node.getName().toString();

            if (IDENT_THIS.equals(identifier)) {
                ClassEntity enclosingClass = findEnclosingClass(params.baseScope());
                return toList(
                        enclosingClass,
                        true /* isInstanceContext */,
                        typeSolver.solveTypeParameters(
                                enclosingClass.getTypeParameters(),
                                ImmutableList.of(),
                                params.contextTypeParameters(),
                                enclosingClass,
                                params.module()));
            }

            if (IDENT_SUPER.equals(identifier)) {
                ClassEntity enclosingClass = findEnclosingClass(params.baseScope());
                if (enclosingClass != null && enclosingClass.getSuperClass().isPresent()) {
                    return toList(
                            typeSolver
                                    .solve(
                                            enclosingClass.getSuperClass().get(),
                                            params.contextTypeParameters(),
                                            enclosingClass.getParentScope().get(),
                                            params.module())
                                    .filter(solvedType -> solvedType instanceof SolvedReferenceType)
                                    .map(solvedType -> {
                                        SolvedReferenceType superClass = (SolvedReferenceType) solvedType;
                                        return EntityWithContext.builder()
                                                .setEntity(superClass.getEntity())
                                                .setSolvedTypeParameters(superClass.getTypeParameters())
                                                .setArrayLevel(0)
                                                .setInstanceContext(true)
                                                .build();
                                    }));
                }
            }

            if (params.contextTypeParameters().getTypeParameter(identifier).isPresent()) {
                return ImmutableList.of(
                        EntityWithContext.from(
                                        params.contextTypeParameters().getTypeParameter(identifier).get())
                                .build());
            }

            List<EntityWithContext> entities =
                    typeSolver.findEntitiesFromScope(
                            node.getName().toString(),
                            params.baseScope(),
                            params.module(),
                            params.position(),
                            params.allowedEntityKinds());

            if (!entities.isEmpty()) {
                return entities;
            }

            // We don't find anything from enclosing scopes, now try resolving the identifier as a
            // toplevel package or class name.

            if (Sets.intersection(params.allowedEntityKinds(), CLASS_LIKE_OR_PACKAGE_KINDS).isEmpty()) {
                return ImmutableList.of();
            }

            return toList(
                    typeSolver.findClassOrPackageInModule(
                            ImmutableList.of(node.getName().toString()), params.module()),
                    false /* isInstanceContext */,
                    params.contextTypeParameters());
        }

        @Override
        public List<EntityWithContext> visitLiteral(
                LiteralTree node, ExpressionDefinitionScannerParams params) {
            Object value = node.getValue();
            EntityWithContext.Builder builder = EntityWithContext.simpleBuilder();

            if (value == null) {
                return ImmutableList.of(builder.setEntity(NullEntity.INSTANCE).build());
            }

            if (value instanceof String) {
                return toList(
                        typeSolver.findClassInModule(TypeSolver.JAVA_LANG_STRING_QUALIFIERS, params.module()),
                        true /* isInstanceContext */,
                        SolvedTypeParameters.EMPTY);
            }

            Optional<PrimitiveEntity> primitiveEntity = PrimitiveEntity.get(value.getClass());
            if (primitiveEntity.isPresent()) {
                return ImmutableList.of(builder.setEntity(primitiveEntity.get()).build());
            }

            logger.warning("Unknown literal type: %s", value);
            return ImmutableList.of();
        }

        @Override
        public List<EntityWithContext> visitLambdaExpression(
                LambdaExpressionTree node, ExpressionDefinitionScannerParams params) {
            // TODO: implement this.
            return ImmutableList.of();
        }

        @Override
        public List<EntityWithContext> visitTypeCast(
                TypeCastTree node, ExpressionDefinitionScannerParams params) {
            TypeReference typeReference = new TypeReferenceScanner().getTypeReference(node.getType());
            Optional<SolvedType> solvedType =
                    typeSolver.solve(
                            typeReference, params.contextTypeParameters(), params.baseScope(), params.module());
            return toList(
                    solvedType.map(t ->
                            EntityWithContext.from(t)
                                    .setInstanceContext(!(t instanceof PrimitiveEntity))
                                    .build()));
        }

        private List<EntityWithContext> applyTypeArguments(
                List<EntityWithContext> entities,
                List<? extends Tree> typeArguments,
                ExpressionDefinitionScannerParams params) {
            if (typeArguments.isEmpty()) {
                return entities;
            }
            ImmutableList<TypeArgument> parsedTypeArguments =
                    typeArguments.stream()
                            .map(typeArgumentScanner::getTypeArgument)
                            .collect(collectingAndThen(Collectors.toList(), ImmutableList::copyOf));
            ImmutableList.Builder<EntityWithContext> builder = new ImmutableList.Builder<>();
            for (EntityWithContext entityWithContext : entities) {
                ImmutableList<TypeParameter> typeParameters =
                        getTypeParameters(entityWithContext.getEntity());
                if (typeParameters.isEmpty()) {
                    builder.add(entityWithContext);
                    continue;
                }

                SolvedTypeParameters newSolvedTypeParameters =
                        typeSolver.solveTypeParameters(
                                typeParameters,
                                parsedTypeArguments,
                                entityWithContext.getSolvedTypeParameters(),
                                params.baseScope(),
                                params.module());
                builder.add(
                        entityWithContext.toBuilder().setSolvedTypeParameters(newSolvedTypeParameters).build());
            }
            return builder.build();
        }

        private ImmutableList<TypeParameter> getTypeParameters(Entity entity) {
            if (entity instanceof ClassEntity) {
                return ((ClassEntity) entity).getTypeParameters();
            }
            if (entity instanceof MethodEntity) {
                MethodEntity method = (MethodEntity) entity;
                if (method.isConstructor()) {
                    // Constructor inherits type parameters from the class.
                    return method.getParentClass().getTypeParameters();
                } else {
                    return method.getTypeParameters();
                }
            }
            return ImmutableList.of();
        }

        @Nullable
        private ClassEntity findEnclosingClass(EntityScope baseScope) {
            for (; baseScope != null; baseScope = baseScope.getParentScope().orElse(null)) {
                if (baseScope instanceof ClassEntity) {
                    return (ClassEntity) baseScope;
                }
            }
            return null;
        }

        private List<EntityWithContext> toList(Optional<EntityWithContext> optionalEntityWithContext) {
            if (optionalEntityWithContext.isPresent()) {
                return ImmutableList.of(optionalEntityWithContext.get());
            }
            return ImmutableList.of();
        }

        private List<EntityWithContext> toList(
                Optional<? extends Entity> optionalEntity,
                boolean isInstanceContext,
                SolvedTypeParameters solvedTypeParameters) {
            return toList(optionalEntity.orElse(null), isInstanceContext, solvedTypeParameters);
        }

        private List<EntityWithContext> toList(
                @Nullable Entity entity,
                boolean isInstanceContext,
                SolvedTypeParameters solvedTypeParameters) {
            if (entity == null) {
                return ImmutableList.of();
            }
            return ImmutableList.of(
                    EntityWithContext.builder()
                            .setArrayLevel(0)
                            .setEntity(entity)
                            .setInstanceContext(isInstanceContext)
                            .setSolvedTypeParameters(solvedTypeParameters)
                            .build());
        }
    }

    @AutoValue
    abstract static class ExpressionDefinitionScannerParams {
        abstract EntityScope baseScope();

        abstract Module module();

        abstract ImmutableSet<Entity.Kind> allowedEntityKinds();

        abstract int position();

        abstract SolvedTypeParameters contextTypeParameters();

        abstract Builder toBuilder();

        ExpressionDefinitionScannerParams copyWithAllEntityKindsAllowed() {
            // Use identity check here. The scanner may chain-call this method
            // multiple times when recursively scanning the expression, so identity
            // check works for most of the time.
            if (allowedEntityKinds() == ALL_ENTITY_KINDS) {
                return this;
            }
            return toBuilder().allowedEntityKinds(ALL_ENTITY_KINDS).build();
        }

        static Builder builder() {
            return new AutoValue_ExpressionSolver_ExpressionDefinitionScannerParams.Builder();
        }

        @AutoValue.Builder
        abstract static class Builder {
            abstract Builder baseScope(EntityScope value);

            abstract Builder module(Module value);

            abstract Builder allowedEntityKinds(ImmutableSet<Entity.Kind> value);

            Builder allowedEntityKinds(Collection<Entity.Kind> value) {
                return allowedEntityKinds(ImmutableSet.copyOf(value));
            }

            abstract Builder position(int value);

            abstract Builder contextTypeParameters(SolvedTypeParameters value);

            abstract ExpressionDefinitionScannerParams build();
        }
    }
}
