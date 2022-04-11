/*******************************************************************************
 * Copyright (c) 2013, 2019 GK Software AG.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Stephan Herrmann - initial API and implementation
 *     Lars Vogel <Lars.Vogel@vogella.com> - Contributions for
 *     						Bug 473178
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.lookup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.internal.compiler.ast.Argument;
import org.eclipse.jdt.internal.compiler.ast.ConditionalExpression;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.ExpressionContext;
import org.eclipse.jdt.internal.compiler.ast.Invocation;
import org.eclipse.jdt.internal.compiler.ast.LambdaExpression;
import org.eclipse.jdt.internal.compiler.ast.MessageSend;
import org.eclipse.jdt.internal.compiler.ast.ReferenceExpression;
import org.eclipse.jdt.internal.compiler.ast.SwitchExpression;
import org.eclipse.jdt.internal.compiler.ast.Wildcard;
import org.eclipse.jdt.internal.compiler.lookup.InferenceContext18.SuspendedInferenceRecord;

/**
 * Implementation of 18.1.2 in JLS8, case:
 * <ul>
 * <li>Expression -> T</li>
 * </ul>
 */
class ConstraintExpressionFormula extends ConstraintFormula {
	Expression left;

	// this flag contributes to the workaround controlled by InferenceContext18.ARGUMENT_CONSTRAINTS_ARE_SOFT:
	boolean isSoft;

	ConstraintExpressionFormula(Expression expression, TypeBinding type, int relation) {
		this.left = expression;
		this.right = type;
		this.relation = relation;
	}

	ConstraintExpressionFormula(Expression expression, TypeBinding type, int relation, boolean isSoft) {
		this(expression, type, relation);
		this.isSoft = isSoft;
	}

	@Override
	public Object reduce(InferenceContext18 inferenceContext) throws InferenceFailureException {

		if (this.relation == POTENTIALLY_COMPATIBLE) {
			/* 15.12.2.1: ... The definition of potential applicability goes beyond a basic arity check to also take into account the presence and "shape" of functional interface
			   target types. In some cases involving type argument inference, a lambda expression appearing as a method invocation argument cannot be properly typed until after
			   overload resolution. These rules allow the form of the lambda expression to still be taken into account, discarding obviously incorrect target types that might
			   otherwise cause ambiguity errors.
			*/

			return  this.left.isPotentiallyCompatibleWith(this.right, inferenceContext.scope) ? TRUE: FALSE;
		}

		// JLS 18.2.1
		if (this.right.isProperType(true)) {
			if (this.left.isCompatibleWith(this.right, inferenceContext.scope) || this.left.isBoxingCompatibleWith(this.right, inferenceContext.scope)) {
				if (this.left.resolvedType != null && this.left.resolvedType.needsUncheckedConversion(this.right)) {
					inferenceContext.usesUncheckedConversion = true;
				}
				return TRUE;
			}
			return FALSE;
		}
		if (!canBePolyExpression(this.left)) {
			TypeBinding exprType = this.left.resolvedType;
			if (exprType == null || !exprType.isValidBinding()) {
				if (this.left instanceof MessageSend && ((MessageSend)this.left).actualReceiverType instanceof InferenceVariable)
					return null; // nothing valuable to infer from this
				return FALSE;
			}
			return ConstraintTypeFormula.create(exprType, this.right, COMPATIBLE, this.isSoft);
		} else {
			// shapes of poly expressions (18.2.1)
			// - parenthesized expression : these are transparent in our AST
			if (this.left instanceof Invocation) {
				Invocation invocation = (Invocation) this.left;
				MethodBinding previousMethod = invocation.binding();
				if (previousMethod == null)  	// can happen, e.g., if inside a copied lambda with ignored errors
					return null; 				// -> proceed with no new constraints
				MethodBinding method = previousMethod;
				// ignore previous (inner) inference result and do a fresh start:
				// avoid original(), since we only want to discard one level of instantiation
				// (method type variables - not class type variables)!
				method = previousMethod.shallowOriginal();
				SuspendedInferenceRecord prevInvocation = inferenceContext.enterPolyInvocation(invocation, invocation.arguments());

				// Invocation Applicability Inference: 18.5.1 & Invocation Type Inference: 18.5.2
				InferenceContext18 innerCtx = null;
				try {
					Expression[] arguments = invocation.arguments();
					TypeBinding[] argumentTypes = arguments == null ? Binding.NO_PARAMETERS : new TypeBinding[arguments.length];
					for (int i = 0; i < argumentTypes.length; i++)
						argumentTypes[i] = arguments[i].resolvedType;
					if (previousMethod instanceof ParameterizedGenericMethodBinding) {
						// find the previous inner inference context to see what inference kind this invocation needs:
						innerCtx = invocation.getInferenceContext((ParameterizedGenericMethodBinding) previousMethod);
						if (innerCtx == null) {
							/* No inference context -> the method was likely manufactured by Scope.findExactMethod -> assume it wasn't really poly after all.
							   -> proceed as for non-poly expressions.
							*/
							TypeBinding exprType = this.left.resolvedType;
							if (exprType == null || !exprType.isValidBinding())
								return FALSE;
							return ConstraintTypeFormula.create(exprType, this.right, COMPATIBLE, this.isSoft);
						}
						if (innerCtx.stepCompleted >= InferenceContext18.APPLICABILITY_INFERRED) {
							inferenceContext.integrateInnerInferenceB2(innerCtx);
						} else {
							return FALSE; // should not reach here.
						}
						// b2 has been lifted, inferring poly invocation type amounts to lifting b3.
					} else {
						inferenceContext.inferenceKind = inferenceContext.getInferenceKind(previousMethod, argumentTypes);
						boolean isDiamond = method.isConstructor() && this.left.isPolyExpression(method);
						inferInvocationApplicability(inferenceContext, method, argumentTypes, isDiamond, inferenceContext.inferenceKind);
						// b2 has been lifted, inferring poly invocation type amounts to lifting b3.
					}
					if (!inferenceContext.computeB3(invocation, this.right, method))
						return FALSE;
					return null; // already incorporated
				} finally {
					inferenceContext.resumeSuspendedInference(prevInvocation, innerCtx);
				}
			} else if (this.left instanceof ConditionalExpression) {
				ConditionalExpression conditional = (ConditionalExpression) this.left;
				return new ConstraintFormula[] {
					new ConstraintExpressionFormula(conditional.valueIfTrue, this.right, this.relation, this.isSoft),
					new ConstraintExpressionFormula(conditional.valueIfFalse, this.right, this.relation, this.isSoft)
				};
			}  else if (this.left instanceof SwitchExpression) {
				SwitchExpression se = (SwitchExpression) this.left;
				ConstraintFormula[] cfs = new ConstraintFormula[se.resultExpressions.size()];
				int i = 0;
				for (Expression re : se.resultExpressions) {
					cfs[i++] = new ConstraintExpressionFormula(re, this.right, this.relation, this.isSoft);
				}
				return cfs;
			} else if (this.left instanceof LambdaExpression) {
				LambdaExpression lambda = (LambdaExpression) this.left;
				BlockScope scope = lambda.enclosingScope;
				if (this.right instanceof InferenceVariable)
					return TRUE; // assume inner inference will handle the fine print
				if (!this.right.isFunctionalInterface(scope))
					return FALSE;

				ReferenceBinding t = (ReferenceBinding) this.right;
				ParameterizedTypeBinding withWildCards = InferenceContext18.parameterizedWithWildcard(t);
				if (withWildCards != null) {
					t = findGroundTargetType(inferenceContext, scope, lambda, withWildCards);
				}
				if (t == null)
					return FALSE;
				MethodBinding functionType = t.getSingleAbstractMethod(scope, true);
				if (functionType == null)
					return FALSE;
				TypeBinding[] parameters = functionType.parameters;
				if (parameters.length != lambda.arguments().length)
					return FALSE;
				if (lambda.argumentsTypeElided())
					for (int i = 0; i < parameters.length; i++)
						if (!parameters[i].isProperType(true))
							return FALSE;
				lambda = lambda.resolveExpressionExpecting(t, inferenceContext.scope, inferenceContext);
				if (lambda == null)
					return FALSE; // not strictly unreduceable, but proceeding with TRUE would likely produce secondary errors
				if (functionType.returnType == TypeBinding.VOID) {
					if (!lambda.isVoidCompatible())
						return FALSE;
				} else {
					if (!lambda.isValueCompatible())
						return FALSE;
				}
				List<ConstraintFormula> result = new ArrayList<>();
				if (!lambda.argumentsTypeElided()) {
					Argument[] arguments = lambda.arguments();
					for (int i = 0; i < parameters.length; i++)
						result.add(ConstraintTypeFormula.create(parameters[i], arguments[i].type.resolvedType, SAME));
					// in addition, ⟨T' <: T⟩:
					if (lambda.resolvedType != null)
						result.add(ConstraintTypeFormula.create(lambda.resolvedType, this.right, SUBTYPE));
				}
				if (functionType.returnType != TypeBinding.VOID) {
					TypeBinding r = functionType.returnType;
					Expression[] exprs = lambda.resultExpressions();
					for (int i = 0, length = exprs == null ? 0 : exprs.length; i < length; i++) {
						Expression expr = exprs[i];
						if (r.isProperType(true) && expr.resolvedType != null) {
							TypeBinding exprType = expr.resolvedType;
							// "not compatible in an assignment context with R"?
							if (!(expr.isConstantValueOfTypeAssignableToType(exprType, r)
									|| exprType.isCompatibleWith(r) || expr.isBoxingCompatible(exprType, r, expr, scope)))
								return FALSE;
						} else {
							result.add(new ConstraintExpressionFormula(expr, r, COMPATIBLE, this.isSoft));
						}
					}
				}
				if (result.size() == 0)
					return TRUE;
				return result.toArray(new ConstraintFormula[result.size()]);
			} else if (this.left instanceof ReferenceExpression) {
				return reduceReferenceExpressionCompatibility((ReferenceExpression) this.left, inferenceContext);
			}
		}
		return FALSE;
	}

	public static ReferenceBinding findGroundTargetType(InferenceContext18 inferenceContext, BlockScope scope,
													LambdaExpression lambda, ParameterizedTypeBinding targetTypeWithWildCards)
	{
		if (lambda.argumentsTypeElided()) {
			return lambda.findGroundTargetTypeForElidedLambda(scope, targetTypeWithWildCards);
		} else {
			SuspendedInferenceRecord previous = inferenceContext.enterLambda(lambda);
			try {
				return inferenceContext.inferFunctionalInterfaceParameterization(lambda, scope, targetTypeWithWildCards);
			} finally {
				inferenceContext.resumeSuspendedInference(previous, null);
			}
		}
	}

	private boolean canBePolyExpression(Expression expr) {
		// when inferring compatibility against a right type, the check isPolyExpression
		// must assume that expr occurs in s.t. like an assignment context:
		ExpressionContext previousExpressionContext = expr.getExpressionContext();
		if (previousExpressionContext == ExpressionContext.VANILLA_CONTEXT)
			this.left.setExpressionContext(ExpressionContext.ASSIGNMENT_CONTEXT);
		try {
			return expr.isPolyExpression();
		} finally {
			expr.setExpressionContext(previousExpressionContext);
		}
	}

	private Object reduceReferenceExpressionCompatibility(ReferenceExpression reference, InferenceContext18 inferenceContext) {
		TypeBinding t = this.right;
		if (t.isProperType(true))
			throw new IllegalStateException("Should not reach here with T being a proper type"); //$NON-NLS-1$
		if (!t.isFunctionalInterface(inferenceContext.scope))
			return FALSE;
		MethodBinding functionType = t.getSingleAbstractMethod(inferenceContext.scope, true);
		if (functionType == null)
			return FALSE;
		// potentially-applicable method for the method reference when targeting T (15.13.1),
		reference = reference.resolveExpressionExpecting(t, inferenceContext.scope, inferenceContext);
		MethodBinding potentiallyApplicable = reference != null ? reference.binding : null;
		if (potentiallyApplicable == null)
			return FALSE;
		if (reference.isExactMethodReference()) {
			List<ConstraintFormula> newConstraints = new ArrayList<>();
			TypeBinding[] p = functionType.parameters;
			int n = p.length;
			TypeBinding[] pPrime = potentiallyApplicable.parameters;
			int k = pPrime.length;
			int offset = 0;
			if (n == k+1) {
				newConstraints.add(ConstraintTypeFormula.create(p[0], reference.lhs.resolvedType, COMPATIBLE));
				offset = 1;
			}
			for (int i = offset; i < n; i++)
				newConstraints.add(ConstraintTypeFormula.create(p[i], pPrime[i-offset], COMPATIBLE));
			TypeBinding r = functionType.returnType;
			if (r != TypeBinding.VOID) {
				TypeBinding rAppl = potentiallyApplicable.isConstructor() && !reference.isArrayConstructorReference() ? potentiallyApplicable.declaringClass : potentiallyApplicable.returnType;
				if (rAppl == TypeBinding.VOID)
					return FALSE;
				TypeBinding rPrime = rAppl.capture(inferenceContext.scope, reference.sourceStart, reference.sourceEnd);
				newConstraints.add(ConstraintTypeFormula.create(rPrime, r, COMPATIBLE));
			}
			return newConstraints.toArray(new ConstraintFormula[newConstraints.size()]);
		} else { // inexact
			int n = functionType.parameters.length;
			for (int i = 0; i < n; i++)
				if (!functionType.parameters[i].isProperType(true))
					return FALSE;
			// Otherwise, a search for a compile-time declaration is performed, as defined in 15.13.1....
			// Note: we currently don't distinguish search for a potentially-applicable method from searching the compiler-time declaration,
			// hence reusing the method binding from above
			MethodBinding compileTimeDecl = potentiallyApplicable;
			if (!compileTimeDecl.isValidBinding())
				return FALSE;
			TypeBinding r = functionType.isConstructor() ? functionType.declaringClass : functionType.returnType;
			if (r.id == TypeIds.T_void)
				return TRUE;
			// ignore parameterization of resolve result and do a fresh start:
			MethodBinding original = compileTimeDecl.shallowOriginal();
			if (needsInference(reference, original)) {
				TypeBinding[] argumentTypes;
				if (t.isParameterizedType()) {
					MethodBinding capturedFunctionType = ((ParameterizedTypeBinding)t).getSingleAbstractMethod(inferenceContext.scope, true, reference.sourceStart, reference.sourceEnd);
					argumentTypes = capturedFunctionType.parameters;
				} else {
					argumentTypes = functionType.parameters;
				}
				SuspendedInferenceRecord prevInvocation = inferenceContext.enterPolyInvocation(reference, reference.createPseudoExpressions(argumentTypes));

				// Invocation Applicability Inference: 18.5.1 & Invocation Type Inference: 18.5.2
				InferenceContext18 innerContext = null;
				try {
					innerContext = reference.getInferenceContext((ParameterizedMethodBinding) compileTimeDecl);
					if (innerContext != null)
						innerContext.pushBoundsTo(inferenceContext);
					int innerInferenceKind = determineInferenceKind(compileTimeDecl, argumentTypes, innerContext);
					inferInvocationApplicability(inferenceContext, original, argumentTypes, original.isConstructor()/*mimic a diamond?*/, innerInferenceKind);
					if (!inferenceContext.computeB3(reference, r, original))
						return FALSE;
					return null; // already incorporated
				} catch (InferenceFailureException e) {
					return FALSE;
				} finally {
					inferenceContext.resumeSuspendedInference(prevInvocation, innerContext);
				}
			}
			TypeBinding rPrime = compileTimeDecl.isConstructor() ? compileTimeDecl.declaringClass : compileTimeDecl.returnType.capture(inferenceContext.scope, reference.sourceStart(), reference.sourceEnd());
			if (rPrime.id == TypeIds.T_void)
				return FALSE;
			return ConstraintTypeFormula.create(rPrime, r, COMPATIBLE, this.isSoft);
		}
	}

	private boolean needsInference(ReferenceExpression reference, MethodBinding original) {
		if (reference.typeArguments != null)
			return false;
		TypeBinding compileTimeReturn;
		if (original.isConstructor()) {
			// not checking r.mentionsAny for constructors, because A::new resolves to the raw type
			// whereas in fact the type of all expressions of this shape depend on their type variable (if any)
			if (original.declaringClass.typeVariables() != Binding.NO_TYPE_VARIABLES
					&& reference.receiverType.isRawType())
				return true; // diamond
			compileTimeReturn = original.declaringClass;
		} else {
			compileTimeReturn =  original.returnType;
		}
		return (original.typeVariables() != Binding.NO_TYPE_VARIABLES
				&& compileTimeReturn.mentionsAny(original.typeVariables(), -1));
	}

	private int determineInferenceKind(MethodBinding original, TypeBinding[] argumentTypes, InferenceContext18 innerContext) {
		if (innerContext != null)
			return innerContext.inferenceKind;
		if (original.isVarargs()) {
			int expectedLen = original.parameters.length;
			int providedLen = argumentTypes.length;
			if (expectedLen < providedLen) {
				return InferenceContext18.CHECK_VARARG;
			} else if (expectedLen == providedLen) {
				TypeBinding providedLast = argumentTypes[expectedLen-1];
				TypeBinding expectedLast = original.parameters[expectedLen-1];
				if (!providedLast.isCompatibleWith(expectedLast)) {
					if (expectedLast.isArrayType()) {
						expectedLast = expectedLast.leafComponentType();
						if (providedLast.isCompatibleWith(expectedLast))
							return InferenceContext18.CHECK_VARARG;
					}
				}
			}
		}
		return InferenceContext18.CHECK_STRICT;
	}

	static void inferInvocationApplicability(InferenceContext18 inferenceContext, MethodBinding method, TypeBinding[] arguments, boolean isDiamond, int checkType)
	{
		// 18.5.1
		TypeVariableBinding[] typeVariables = method.getAllTypeVariables(isDiamond);
		InferenceVariable[] inferenceVariables = inferenceContext.createInitialBoundSet(typeVariables); // creates initial bound set B

		// check if varargs need special treatment:
		int paramLength = method.parameters.length;
		TypeBinding varArgsType = null;
		if (method.isVarargs()) {
			int varArgPos = paramLength-1;
			varArgsType = method.parameters[varArgPos];
		}
		inferenceContext.createInitialConstraintsForParameters(method.parameters, checkType==InferenceContext18.CHECK_VARARG, varArgsType, method);
		inferenceContext.addThrowsContraints(typeVariables, inferenceVariables, method.thrownExceptions);
	}

	/** Perform steps from JLS 18.5.2. needed for computing the bound set B3. */
	static boolean inferPolyInvocationType(InferenceContext18 inferenceContext, InvocationSite invocationSite, TypeBinding targetType, MethodBinding method)
				throws InferenceFailureException
	{
		TypeBinding[] typeArguments = invocationSite.genericTypeArguments();
		if (typeArguments == null) {
			// invocation type inference (18.5.2):
			TypeBinding returnType = method.isConstructor() ? method.declaringClass : method.returnType;
			if (returnType == TypeBinding.VOID)
				throw new InferenceFailureException("expression has no value"); //$NON-NLS-1$

			if (inferenceContext.usesUncheckedConversion) {
				TypeBinding erasure = getRealErasure(returnType, inferenceContext.environment);
				ConstraintTypeFormula newConstraint = ConstraintTypeFormula.create(erasure, targetType, COMPATIBLE);
				return inferenceContext.reduceAndIncorporate(newConstraint);
			}
			TypeBinding rTheta = inferenceContext.substitute(returnType);
			ParameterizedTypeBinding parameterizedType = InferenceContext18.parameterizedWithWildcard(rTheta);
			if (parameterizedType != null && parameterizedType.arguments != null) {
				TypeBinding[] arguments = parameterizedType.arguments;
				InferenceVariable[] betas = inferenceContext.addTypeVariableSubstitutions(arguments);
				ParameterizedTypeBinding gbeta = inferenceContext.environment.createParameterizedType(
						parameterizedType.genericType(), betas, parameterizedType.enclosingType(), parameterizedType.getTypeAnnotations());
				inferenceContext.currentBounds.captures.put(gbeta, parameterizedType); // established: both types have nonnull arguments
				if (InferenceContext18.SHOULD_WORKAROUND_BUG_JDK_8054721) {
					for (int i = 0, length = arguments.length; i < length; i++) {
						if (arguments[i].isWildcard()) {
							WildcardBinding wc = (WildcardBinding) arguments[i];
							switch (wc.boundKind) {
								case Wildcard.EXTENDS:
									inferenceContext.currentBounds.addBound(new TypeBound(betas[i], wc.bound(), SUBTYPE), inferenceContext.environment);
									break;
								case Wildcard.SUPER:
									inferenceContext.currentBounds.addBound(new TypeBound(betas[i], wc.bound(), SUPERTYPE), inferenceContext.environment);
									break;
							}
						}
					}
				}
				ConstraintTypeFormula newConstraint = ConstraintTypeFormula.create(gbeta, targetType, COMPATIBLE);
				return inferenceContext.reduceAndIncorporate(newConstraint);
			}
			if (rTheta.leafComponentType() instanceof InferenceVariable) { // https://bugs.openjdk.java.net/browse/JDK-8062082
				InferenceVariable alpha = (InferenceVariable) rTheta.leafComponentType();
				TypeBinding targetLeafType = targetType.leafComponentType();
				boolean toResolve = false;
				if (inferenceContext.currentBounds.condition18_5_2_bullet_3_3_1(alpha, targetLeafType)) {
					toResolve = true;
				} else if (inferenceContext.currentBounds.condition18_5_2_bullet_3_3_2(alpha, targetLeafType, inferenceContext)) {
					toResolve = true;
				} else if (targetLeafType.isPrimitiveType()) {
					TypeBinding wrapper = inferenceContext.currentBounds.findWrapperTypeBound(alpha);
					if (wrapper != null)
						toResolve = true;
				}
				if (toResolve) {
					BoundSet solution = inferenceContext.solve(new InferenceVariable[]{alpha});
					if (solution == null)
						return false;
					TypeBinding u = solution.getInstantiation(alpha, null).capture(inferenceContext.scope, invocationSite.sourceStart(), invocationSite.sourceEnd());
					if (rTheta.dimensions() != 0) {
						u = inferenceContext.environment.createArrayType(u, rTheta.dimensions());
					}
					ConstraintTypeFormula newConstraint = ConstraintTypeFormula.create(u, targetType, COMPATIBLE);
					return inferenceContext.reduceAndIncorporate(newConstraint);
				}
			}
			ConstraintTypeFormula newConstraint = ConstraintTypeFormula.create(rTheta, targetType, COMPATIBLE);
			if (!inferenceContext.reduceAndIncorporate(newConstraint))
				return false;
		}
		return true;
	}

	private static TypeBinding getRealErasure(TypeBinding type, LookupEnvironment environment) {
		TypeBinding erasure = type.erasure();
		// could still be / contain a generic type that needs to be converted to raw:
		TypeBinding erasedLeaf = erasure.leafComponentType();
		if (erasedLeaf.isGenericType())
			erasedLeaf = environment.convertToRawType(erasedLeaf, false);
		if (erasure.isArrayType())
			return environment.createArrayType(erasedLeaf, erasure.dimensions());
		return erasedLeaf;
	}

	@Override
	Collection<InferenceVariable> inputVariables(final InferenceContext18 context) {
		// from 18.5.2.
		if (this.left instanceof LambdaExpression) {
			if (this.right instanceof InferenceVariable) {
				return Collections.singletonList((InferenceVariable)this.right);
			}
			if (this.right.isFunctionalInterface(context.scope)) {
				LambdaExpression lambda = (LambdaExpression) this.left;
				ReferenceBinding targetType = (ReferenceBinding) this.right;
				ParameterizedTypeBinding withWildCards = InferenceContext18.parameterizedWithWildcard(targetType);
				if (withWildCards != null) {
					targetType = ConstraintExpressionFormula.findGroundTargetType(context, lambda.enclosingScope, lambda, withWildCards);
				}
				if (targetType == null) {
					return EMPTY_VARIABLE_LIST;
				}
				MethodBinding sam = targetType.getSingleAbstractMethod(context.scope, true);
				final Set<InferenceVariable> variables = new LinkedHashSet<>();
				if (lambda.argumentsTypeElided()) {
					// i)
					int len = sam.parameters.length;
					for (int i = 0; i < len; i++) {
						sam.parameters[i].collectInferenceVariables(variables);
					}
				}
				if (sam.returnType != TypeBinding.VOID) {
					// ii)
					final TypeBinding r = sam.returnType;
					LambdaExpression resolved = lambda.resolveExpressionExpecting(this.right, context.scope, context);
					Expression[] resultExpressions = resolved != null ? resolved.resultExpressions() : null;
					for (int i = 0, length = resultExpressions == null ? 0 : resultExpressions.length; i < length; i++) {
						variables.addAll(new ConstraintExpressionFormula(resultExpressions[i], r, COMPATIBLE).inputVariables(context));
					}
				}
				return variables;
			}
		} else if (this.left instanceof ReferenceExpression) {
			if (this.right instanceof InferenceVariable) {
				return Collections.singletonList((InferenceVariable)this.right);
			}
			if (this.right.isFunctionalInterface(context.scope) && !this.left.isExactMethodReference()) {
				MethodBinding sam = this.right.getSingleAbstractMethod(context.scope, true);
				final Set<InferenceVariable> variables = new LinkedHashSet<>();
				int len = sam.parameters.length;
				for (int i = 0; i < len; i++) {
					sam.parameters[i].collectInferenceVariables(variables);
				}
				return variables;
			}
		} else if (this.left instanceof ConditionalExpression && this.left.isPolyExpression()) {
			ConditionalExpression expr = (ConditionalExpression) this.left;
			Set<InferenceVariable> variables = new LinkedHashSet<>();
			variables.addAll(new ConstraintExpressionFormula(expr.valueIfTrue, this.right, COMPATIBLE).inputVariables(context));
			variables.addAll(new ConstraintExpressionFormula(expr.valueIfFalse, this.right, COMPATIBLE).inputVariables(context));
			return variables;
		} else if (this.left instanceof SwitchExpression && this.left.isPolyExpression()) {
			SwitchExpression expr = (SwitchExpression) this.left;
			Set<InferenceVariable> variables = new LinkedHashSet<>();
			for (Expression re : expr.resultExpressions) {
				variables.addAll(new ConstraintExpressionFormula(re, this.right, COMPATIBLE).inputVariables(context));
			}
			return variables;
		}
		return EMPTY_VARIABLE_LIST;
	}

	// debugging:
	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer().append(LEFT_ANGLE_BRACKET);
		this.left.printExpression(4, buf);
		buf.append(relationToString(this.relation));
		appendTypeName(buf, this.right);
		buf.append(RIGHT_ANGLE_BRACKET);
		return buf.toString();
	}
}
