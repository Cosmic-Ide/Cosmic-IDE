/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Nick Teryaev - fix for bug (https://bugs.eclipse.org/bugs/show_bug.cgi?id=40752)
 *     Stephan Herrmann - Contributions for
 *								bug 319201 - [null] no warning when unboxing SingleNameReference causes NPE
 *								bug 345305 - [compiler][null] Compiler misidentifies a case of "variable can only be null"
 *								bug 395002 - Self bound generic class doesn't resolve bounds properly for wildcards for certain parametrisation.
 *								bug 383368 - [compiler][null] syntactic null analysis for field references
 *								bug 401017 - [compiler][null] casted reference to @Nullable field lacks a warning
 *								bug 400761 - [compiler][null] null may be return as boolean without a diagnostic
 *								Bug 392238 - [1.8][compiler][null] Detect semantically invalid null type annotations
 *								Bug 416307 - [1.8][compiler][null] subclass with type parameter substitution confuses null checking
 *								Bug 392099 - [1.8][compiler][null] Apply null annotation on types for null analysis
 *								Bug 400874 - [1.8][compiler] Inference infrastructure should evolve to meet JLS8 18.x (Part G of JSR335 spec)
 *								Bug 427438 - [1.8][compiler] NPE at org.eclipse.jdt.internal.compiler.ast.ConditionalExpression.generateCode(ConditionalExpression.java:280)
 *								Bug 430150 - [1.8][null] stricter checking against type variables
 *								Bug 435805 - [1.8][compiler][null] Java 8 compiler does not recognize declaration style null annotations
 *								Bug 407414 - [compiler][null] Incorrect warning on a primitive type being null
 *        Andy Clement (GoPivotal, Inc) aclement@gopivotal.com - Contributions for
 *                          Bug 415541 - [1.8][compiler] Type annotations in the body of static initializer get dropped
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.ast;

import static org.eclipse.jdt.internal.compiler.ast.ExpressionContext.CASTING_CONTEXT;

import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.codegen.CodeStream;
import org.eclipse.jdt.internal.compiler.flow.FlowContext;
import org.eclipse.jdt.internal.compiler.flow.FlowInfo;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.impl.Constant;
import org.eclipse.jdt.internal.compiler.impl.IrritantSet;
import org.eclipse.jdt.internal.compiler.lookup.ArrayBinding;
import org.eclipse.jdt.internal.compiler.lookup.Binding;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.InferenceContext18;
import org.eclipse.jdt.internal.compiler.lookup.InvocationSite;
import org.eclipse.jdt.internal.compiler.lookup.LocalVariableBinding;
import org.eclipse.jdt.internal.compiler.lookup.LookupEnvironment;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.ParameterizedGenericMethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.ParameterizedTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.PolymorphicMethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.Scope;
import org.eclipse.jdt.internal.compiler.lookup.TagBits;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeIds;
import org.eclipse.jdt.internal.compiler.problem.ProblemSeverities;

public class CastExpression extends Expression {

	public Expression expression;
	public TypeReference type;
	public TypeBinding expectedType; // when assignment conversion to a given expected type: String s = (String) t;
	public TypeBinding instanceofType; // set by InstanceofExpression to ensure we don't flag a necessary cast unnecessary
	public boolean isVarTypeDeclaration; // set by LocalDeclaration to indicate we are initializing a var type declaration

//expression.implicitConversion holds the cast for baseType casting
public CastExpression(Expression expression, TypeReference type) {
	this.expression = expression;
	this.type = type;
	type.bits |= ASTNode.IgnoreRawTypeCheck; // no need to worry about raw type usage
}

@Override
public FlowInfo analyseCode(BlockScope currentScope, FlowContext flowContext, FlowInfo flowInfo) {
	FlowInfo result = this.expression
		.analyseCode(currentScope, flowContext, flowInfo)
		.unconditionalInits();
	this.expression.checkNPEbyUnboxing(currentScope, flowContext, flowInfo);
	// account for pot. CCE:
	flowContext.recordAbruptExit();
	return result;
}

/**
 * Complain if assigned expression is cast, but not actually used as such, e.g. Object o = (List) object;
 */
public static void checkNeedForAssignedCast(BlockScope scope, TypeBinding expectedType, CastExpression rhs) {
	CompilerOptions compilerOptions = scope.compilerOptions();
	if (compilerOptions.getSeverity(CompilerOptions.UnnecessaryTypeCheck) == ProblemSeverities.Ignore) return;

	TypeBinding castedExpressionType = rhs.expression.resolvedType;
	//	int i = (byte) n; // cast still had side effect
	// double d = (float) n; // cast to float is unnecessary
	if (castedExpressionType == null || rhs.resolvedType.isBaseType()) return;
	//if (castedExpressionType.id == T_null) return; // tolerate null expression cast
	if (castedExpressionType.isCompatibleWith(expectedType, scope)) {
		if (scope.environment().usesNullTypeAnnotations()) {
			// are null annotations compatible, too?
			if (NullAnnotationMatching.analyse(expectedType, castedExpressionType, -1).isAnyMismatch())
				return; // already reported unchecked cast (nullness), say no more.
		}
		scope.problemReporter().unnecessaryCast(rhs);
	}
}


/**
 * Complain if cast expression is cast, but not actually needed, int i = (int)(Integer) 12;
 * Note that this (int) cast is however needed:   Integer i = 0;  char c = (char)((int) i);
 */
public static void checkNeedForCastCast(BlockScope scope, CastExpression enclosingCast) {
	if (scope.compilerOptions().getSeverity(CompilerOptions.UnnecessaryTypeCheck) == ProblemSeverities.Ignore) return;

	CastExpression nestedCast = (CastExpression) enclosingCast.expression;
	if ((nestedCast.bits & ASTNode.UnnecessaryCast) == 0) return;
	if (nestedCast.losesPrecision(scope)) return;
	// check if could cast directly to enclosing cast type, without intermediate type cast
	CastExpression alternateCast = new CastExpression(null, enclosingCast.type);
	alternateCast.resolvedType = enclosingCast.resolvedType;
	if (!alternateCast.checkCastTypesCompatibility(scope, enclosingCast.resolvedType, nestedCast.expression.resolvedType, null /* no expr to avoid side-effects*/, true)) return;
	scope.problemReporter().unnecessaryCast(nestedCast);
}

private boolean losesPrecision(Scope scope) {
	// implements the following from JLS §5.1.2:
	// "A widening primitive conversion from int to float, or from long to float, or from long to double, may result in loss of precision [...]"
	// (extended to boxed types)
	TypeBinding exprType = this.expression.resolvedType;
	if (exprType.isBoxedPrimitiveType())
		exprType = scope.environment().computeBoxingType(exprType);
	switch (this.resolvedType.id) {
		case TypeIds.T_JavaLangFloat:
		case TypeIds.T_float: 	// (float)myInt , (float)myLong need rounding
			return exprType.id == TypeIds.T_int || exprType.id == TypeIds.T_long;
		case TypeIds.T_JavaLangDouble:
		case TypeIds.T_double:	// (double)myLong needs rounding
			return exprType.id == TypeIds.T_long;
	}
	return false;
}

/**
 * Casting an enclosing instance will considered as useful if removing it would actually bind to a different type
 */
public static void checkNeedForEnclosingInstanceCast(BlockScope scope, Expression enclosingInstance, TypeBinding enclosingInstanceType, TypeBinding memberType) {
	if (scope.compilerOptions().getSeverity(CompilerOptions.UnnecessaryTypeCheck) == ProblemSeverities.Ignore) return;

	TypeBinding castedExpressionType = ((CastExpression)enclosingInstance).expression.resolvedType;
	if (castedExpressionType == null) return; // cannot do better
	// obvious identity cast
	if (TypeBinding.equalsEquals(castedExpressionType, enclosingInstanceType)) {
		scope.problemReporter().unnecessaryCast((CastExpression)enclosingInstance);
	} else if (castedExpressionType == TypeBinding.NULL){
		return; // tolerate null enclosing instance cast
	} else {
		TypeBinding alternateEnclosingInstanceType = castedExpressionType;
		if (castedExpressionType.isBaseType() || castedExpressionType.isArrayType()) return; // error case
		if (TypeBinding.equalsEquals(memberType, scope.getMemberType(memberType.sourceName(), (ReferenceBinding) alternateEnclosingInstanceType))) {
			scope.problemReporter().unnecessaryCast((CastExpression)enclosingInstance);
		}
	}
}

/**
 * Only complain for identity cast, since other type of casts may be useful: e.g.  ~((~(long) 0) << 32)  is different from: ~((~0) << 32)
 */
public static void checkNeedForArgumentCast(BlockScope scope, int operator, int operatorSignature, Expression expression, int expressionTypeId) {
	if (scope.compilerOptions().getSeverity(CompilerOptions.UnnecessaryTypeCheck) == ProblemSeverities.Ignore) return;

	// check need for left operand cast
	if ((expression.bits & ASTNode.UnnecessaryCast) == 0 && expression.resolvedType.isBaseType()) {
		// narrowing conversion on base type may change value, thus necessary
		return;
	} else {
		TypeBinding alternateLeftType = ((CastExpression)expression).expression.resolvedType;
		if (alternateLeftType == null) return; // cannot do better
		if (alternateLeftType.id == expressionTypeId) { // obvious identity cast
			scope.problemReporter().unnecessaryCast((CastExpression)expression);
			return;
		}
	}
}

/**
 * Cast expressions will considered as useful if removing them all would actually bind to a different method
 * (no fine grain analysis on per casted argument basis, simply separate widening cast from narrowing ones)
 */
public static void checkNeedForArgumentCasts(BlockScope scope, Expression receiver, TypeBinding receiverType, MethodBinding binding, Expression[] arguments, TypeBinding[] argumentTypes, final InvocationSite invocationSite) {
	if (scope.compilerOptions().getSeverity(CompilerOptions.UnnecessaryTypeCheck) == ProblemSeverities.Ignore) return;

	int length = argumentTypes.length;

	// iterate over arguments, and retrieve original argument types (before cast)
	TypeBinding[] rawArgumentTypes = argumentTypes;
	for (int i = 0; i < length; i++) {
		Expression argument = arguments[i];
		if (argument instanceof CastExpression) {
			// narrowing conversion on base type may change value, thus necessary
			if ((argument.bits & ASTNode.UnnecessaryCast) == 0 && argument.resolvedType.isBaseType()) {
				continue;
			}
			TypeBinding castedExpressionType = ((CastExpression)argument).expression.resolvedType;
			if (castedExpressionType == null) return; // cannot do better
			// obvious identity cast
			if (TypeBinding.equalsEquals(castedExpressionType, argumentTypes[i])) {
				scope.problemReporter().unnecessaryCast((CastExpression)argument);
			} else if (castedExpressionType == TypeBinding.NULL){
				continue; // tolerate null argument cast
			} else if ((argument.implicitConversion & TypeIds.BOXING) != 0) {
				continue; // boxing has a side effect: (int) char   is not boxed as simple char
			} else {
				if (rawArgumentTypes == argumentTypes) {
					System.arraycopy(rawArgumentTypes, 0, rawArgumentTypes = new TypeBinding[length], 0, length);
				}
				// retain original argument type
				rawArgumentTypes[i] = castedExpressionType;
			}
		}
	}
	// perform alternate lookup with original types
	if (rawArgumentTypes != argumentTypes) {
		checkAlternateBinding(scope, receiver, receiverType, binding, arguments, argumentTypes, rawArgumentTypes, invocationSite);
	}
}

/**
 * Check binary operator casted arguments
 */
public static void checkNeedForArgumentCasts(BlockScope scope, int operator, int operatorSignature, Expression left, int leftTypeId, boolean leftIsCast, Expression right, int rightTypeId, boolean rightIsCast) {
	if (scope.compilerOptions().getSeverity(CompilerOptions.UnnecessaryTypeCheck) == ProblemSeverities.Ignore) return;

	boolean useAutoBoxing = operator != OperatorIds.EQUAL_EQUAL && operator != OperatorIds.NOT_EQUAL;
	// check need for left operand cast
	int alternateLeftTypeId = leftTypeId;
	if (leftIsCast) {
		if ((left.bits & ASTNode.UnnecessaryCast) == 0 && left.resolvedType.isBaseType()) {
			// narrowing conversion on base type may change value, thus necessary
			leftIsCast = false;
		} else  {
			TypeBinding alternateLeftType = ((CastExpression)left).expression.resolvedType;
			if (alternateLeftType == null) return; // cannot do better
			if ((alternateLeftTypeId = alternateLeftType.id) == leftTypeId
					|| (useAutoBoxing
							? scope.environment().computeBoxingType(alternateLeftType).id == leftTypeId
							: TypeBinding.equalsEquals(alternateLeftType, left.resolvedType))) { // obvious identity cast
				scope.problemReporter().unnecessaryCast((CastExpression)left);
				leftIsCast = false;
			} else if (alternateLeftTypeId == TypeIds.T_null) {
				alternateLeftTypeId = leftTypeId;  // tolerate null argument cast
				leftIsCast = false;
			}
		}
	}
	// check need for right operand cast
	int alternateRightTypeId = rightTypeId;
	if (rightIsCast) {
		if ((right.bits & ASTNode.UnnecessaryCast) == 0 && right.resolvedType.isBaseType()) {
			// narrowing conversion on base type may change value, thus necessary
			rightIsCast = false;
		} else {
			TypeBinding alternateRightType = ((CastExpression)right).expression.resolvedType;
			if (alternateRightType == null) return; // cannot do better
			if ((alternateRightTypeId = alternateRightType.id) == rightTypeId
					|| (useAutoBoxing
							? scope.environment().computeBoxingType(alternateRightType).id == rightTypeId
							: TypeBinding.equalsEquals(alternateRightType, right.resolvedType))) { // obvious identity cast
				scope.problemReporter().unnecessaryCast((CastExpression)right);
				rightIsCast = false;
			} else if (alternateRightTypeId == TypeIds.T_null) {
				alternateRightTypeId = rightTypeId;  // tolerate null argument cast
				rightIsCast = false;
			}
		}
	}
	if (leftIsCast || rightIsCast) {
		if (alternateLeftTypeId > 15 || alternateRightTypeId > 15) { // must convert String + Object || Object + String
			if (alternateLeftTypeId == TypeIds.T_JavaLangString) {
				alternateRightTypeId = TypeIds.T_JavaLangObject;
			} else if (alternateRightTypeId == TypeIds.T_JavaLangString) {
				alternateLeftTypeId = TypeIds.T_JavaLangObject;
			} else {
				return; // invalid operator
			}
		}
		int alternateOperatorSignature = OperatorExpression.OperatorSignatures[operator][(alternateLeftTypeId << 4) + alternateRightTypeId];
		// (cast)  left   Op (cast)  right --> result
		//  1111   0000       1111   0000     1111
		//  <<16   <<12       <<8    <<4       <<0
		final int CompareMASK = (0xF<<16) + (0xF<<8) + 0xF; // mask hiding compile-time types
		if ((operatorSignature & CompareMASK) == (alternateOperatorSignature & CompareMASK)) { // same promotions and result
			if (leftIsCast) scope.problemReporter().unnecessaryCast((CastExpression)left);
			if (rightIsCast) scope.problemReporter().unnecessaryCast((CastExpression)right);
		}
	}
}

@Override
public boolean checkNPE(BlockScope scope, FlowContext flowContext, FlowInfo flowInfo, int ttlForFieldCheck) {
	if((this.resolvedType.tagBits & TagBits.AnnotationNonNull) != 0) {
		return true;
	}
	checkNPEbyUnboxing(scope, flowContext, flowInfo);
	return this.expression.checkNPE(scope, flowContext, flowInfo, ttlForFieldCheck);
}

private static void checkAlternateBinding(BlockScope scope, Expression receiver, TypeBinding receiverType, MethodBinding binding, Expression[] arguments, TypeBinding[] originalArgumentTypes, TypeBinding[] alternateArgumentTypes, final InvocationSite invocationSite) {
		InvocationSite fakeInvocationSite = new InvocationSite(){
			@Override
			public TypeBinding[] genericTypeArguments() { return null; }
			@Override
			public boolean isSuperAccess(){ return invocationSite.isSuperAccess(); }
			@Override
			public boolean isTypeAccess() { return invocationSite.isTypeAccess(); }
			@Override
			public void setActualReceiverType(ReferenceBinding actualReceiverType) { /* ignore */}
			@Override
			public void setDepth(int depth) { /* ignore */}
			@Override
			public void setFieldIndex(int depth){ /* ignore */}
			@Override
			public int sourceStart() { return 0; }
			@Override
			public int sourceEnd() { return 0; }
			@Override
			public TypeBinding invocationTargetType() { return invocationSite.invocationTargetType(); }
			@Override
			public boolean receiverIsImplicitThis() { return invocationSite.receiverIsImplicitThis();}
			@Override
			public InferenceContext18 freshInferenceContext(Scope someScope) { return invocationSite.freshInferenceContext(someScope); }
			@Override
			public ExpressionContext getExpressionContext() { return invocationSite.getExpressionContext(); }
			@Override
			public boolean isQualifiedSuper() { return invocationSite.isQualifiedSuper(); }
			@Override
			public boolean checkingPotentialCompatibility() { return false; }
			@Override
			public void acceptPotentiallyCompatibleMethods(MethodBinding[] methods) {/* ignore */}
		};
		MethodBinding bindingIfNoCast;
		if (binding.isConstructor()) {
			bindingIfNoCast = scope.getConstructor((ReferenceBinding)receiverType, alternateArgumentTypes, fakeInvocationSite);
		} else {
			bindingIfNoCast = receiver.isImplicitThis()
				? scope.getImplicitMethod(binding.selector, alternateArgumentTypes, fakeInvocationSite)
				: scope.getMethod(receiverType, binding.selector, alternateArgumentTypes, fakeInvocationSite);
		}
		if (bindingIfNoCast == binding) {
			int argumentLength = originalArgumentTypes.length;
			if (binding.isVarargs()) {
				int paramLength = binding.parameters.length;
				if (paramLength == argumentLength) {
					int varargsIndex = paramLength - 1;
					ArrayBinding varargsType = (ArrayBinding) binding.parameters[varargsIndex];
					TypeBinding lastArgType = alternateArgumentTypes[varargsIndex];
					// originalType may be compatible already, but cast mandated
					// to clarify between varargs/non-varargs call
					if (varargsType.dimensions != lastArgType.dimensions()) {
						return;
					}
					if (lastArgType.isCompatibleWith(varargsType.elementsType())
							&& lastArgType.isCompatibleWith(varargsType)) {
						return;
					}
				}
			}
			for (int i = 0; i < argumentLength; i++) {
				if (TypeBinding.notEquals(originalArgumentTypes[i], alternateArgumentTypes[i])
                       /*&& !originalArgumentTypes[i].needsUncheckedConversion(alternateArgumentTypes[i])*/) {
					if (!preventsUnlikelyTypeWarning(originalArgumentTypes[i], alternateArgumentTypes[i], receiverType, binding, scope))
						scope.problemReporter().unnecessaryCast((CastExpression)arguments[i]);
				}
			}
		}
}

private static boolean preventsUnlikelyTypeWarning(TypeBinding castedType, TypeBinding uncastedType, TypeBinding receiverType, MethodBinding binding, BlockScope scope) {
	if (!scope.compilerOptions().isAnyEnabled(IrritantSet.UNLIKELY_ARGUMENT_TYPE))
		return false;
	if (binding.isStatic() || binding.parameters.length != 1)
		return false;
	// would using the uncastedType be considered as dangerous?
	UnlikelyArgumentCheck argumentChecks = UnlikelyArgumentCheck.determineCheckForNonStaticSingleArgumentMethod(
			uncastedType, scope, binding.selector, receiverType, binding.parameters);
	if (argumentChecks != null && argumentChecks.isDangerous(scope)) {
		// does the cast help?
		argumentChecks = UnlikelyArgumentCheck.determineCheckForNonStaticSingleArgumentMethod(
				castedType, scope, binding.selector, receiverType, binding.parameters);
		if (argumentChecks == null || !argumentChecks.isDangerous(scope))
			return true;
	}
	return false;
}

@Override
public boolean checkUnsafeCast(Scope scope, TypeBinding castType, TypeBinding expressionType, TypeBinding match, boolean isNarrowing) {
	return CastExpression.checkUnsafeCast(this, scope, castType, expressionType, match, isNarrowing);
}
public static boolean checkUnsafeCast(Expression expression, Scope scope, TypeBinding castType, TypeBinding expressionType, TypeBinding match, boolean isNarrowing) {
	// In case of expression being a InstanceOfExpression, this.resolvedType is null
	// hence use the type of RHS of the instanceof operator
	TypeBinding resolvedType = expression.resolvedType != null ? expression.resolvedType : castType;
	if (TypeBinding.equalsEquals(match, castType)) {
		if (!isNarrowing && TypeBinding.equalsEquals(match, resolvedType.leafComponentType()) // do not tag as unnecessary when recursing through upper bounds
				&& !(expressionType.isParameterizedType() && expressionType.isProvablyDistinct(castType))) {
			expression.tagAsUnnecessaryCast(scope, castType);
		}
		return true;
	}
	if (match != null) {
		if (isNarrowing
				? match.isProvablyDistinct(expressionType)
				: castType.isProvablyDistinct(match)) {
			return false;
		}
	}
	switch (castType.kind()) {
		case Binding.PARAMETERIZED_TYPE :
			if (!castType.isReifiable()) {

				// [JLS 5.1.6.2] T <: S
				// [JLS 5.1.5] S <: T
				if (match == null) { // unrelated types
					expression.bits |= ASTNode.UnsafeCast;
					return true;
				}
				switch (match.kind()) {
					case Binding.PARAMETERIZED_TYPE :
						if (isNarrowing) {
							// [JLS 5.1.6.2] T <: S

							// [JLS 5.1.6.2] S has no subtype X other than T where the type arguments of X are not contained in the type arguments of T
							// S will have all the type arguments that were specified in S
							// If T2<T> extends T1<T> then a cast from T1<? extends ArrayList<?>> to T2<? extends List<?>> is checked while a cast from T1<? extends List<?>> to T2<? extends ArrayList<?>> is unchecked
							if (expressionType.isRawType() || !expressionType.isEquivalentTo(match)) {
								expression.bits |= ASTNode.UnsafeCast;
								return true;
							}

							// T will have all the type arguments that were introduced in subtypes of S so the type arguments of T need to be unbound wildcards since the type arguments couldn't have been specified by S
							// If T2<T,U> extends T1<T> then a cast from T1<? extends List<?>> to T2<? extends List<?>, ? extends List<?>> is unchecked
							ParameterizedTypeBinding paramCastType = (ParameterizedTypeBinding) castType;
							TypeBinding[] castArguments = paramCastType.arguments;
							int length = castArguments == null ? 0 : castArguments.length;
							if ((paramCastType.tagBits & (TagBits.HasDirectWildcard|TagBits.HasTypeVariable)) != 0) {
								// verify alternate cast type, substituting different type arguments
								for (int i = 0; i < length; i++) {
									if (castArguments[i].isUnboundWildcard())
										continue;
									TypeBinding[] alternateArguments;
									// need to clone for each iteration to avoid env paramtype cache interference
									System.arraycopy(paramCastType.arguments, 0, alternateArguments = new TypeBinding[length], 0, length);
									alternateArguments[i] = TypeBinding.equalsEquals(paramCastType.arguments[i], scope.getJavaLangObject()) ? scope.getJavaLangBoolean() : scope.getJavaLangObject();
									LookupEnvironment environment = scope.environment();
									ParameterizedTypeBinding alternateCastType = environment.createParameterizedType((ReferenceBinding)castType.erasure(), alternateArguments, castType.enclosingType());
									if (TypeBinding.equalsEquals(alternateCastType.findSuperTypeOriginatingFrom(expressionType), match)) {
										expression.bits |= ASTNode.UnsafeCast;
										break;
									}
								}
							}

							// Type arguments added by subtypes of S and removed by supertypes of T don't need to be checked since the type arguments aren't specified by either S or T
							return true;
						} else {
							// [JLS 5.1.5] S <: T
							if (!match.isEquivalentTo(castType)) {
								expression.bits |= ASTNode.UnsafeCast;
								return true;
							}
						}
						break;
					case Binding.RAW_TYPE :
						expression.bits |= ASTNode.UnsafeCast; // upcast since castType is known to be bound paramType
						return true;
					default :
						if (isNarrowing){
							// match is not parameterized or raw, then any other subtype of match will erase  to |T|
							expression.bits |= ASTNode.UnsafeCast;
							return true;
						}
						break;
				}
			}
			break;
		case Binding.ARRAY_TYPE :
			TypeBinding leafType = castType.leafComponentType();
			if (isNarrowing && (!leafType.isReifiable() || leafType.isTypeVariable())) {
				expression.bits |= ASTNode.UnsafeCast;
				return true;
			}
			break;
		case Binding.TYPE_PARAMETER :
			expression.bits |= ASTNode.UnsafeCast;
			return true;
//		(disabled) https://bugs.eclipse.org/bugs/show_bug.cgi?id=240807
//		case Binding.TYPE :
//			if (isNarrowing && match == null && expressionType.isParameterizedType()) {
//				this.bits |= ASTNode.UnsafeCast;
//				return true;
//			}
//			break;
	}
	if (!isNarrowing && TypeBinding.equalsEquals(match, resolvedType.leafComponentType())) { // do not tag as unnecessary when recursing through upper bounds
		expression.tagAsUnnecessaryCast(scope, castType);
	}
	return true;
}

/**
 * Cast expression code generation
 *
 * @param currentScope org.eclipse.jdt.internal.compiler.lookup.BlockScope
 * @param codeStream org.eclipse.jdt.internal.compiler.codegen.CodeStream
 * @param valueRequired boolean
 */
@Override
public void generateCode(BlockScope currentScope, CodeStream codeStream, boolean valueRequired) {
	int pc = codeStream.position;
	boolean annotatedCast = (this.type.bits & ASTNode.HasTypeAnnotations) != 0;
	boolean needRuntimeCheckcast = (this.bits & ASTNode.GenerateCheckcast) != 0;
	if (this.constant != Constant.NotAConstant) {
		if (valueRequired || needRuntimeCheckcast || annotatedCast) { // Added for: 1F1W9IG: IVJCOM:WINNT - Compiler omits casting check
			codeStream.generateConstant(this.constant, this.implicitConversion);
			if (needRuntimeCheckcast || annotatedCast) {
				codeStream.checkcast(this.type, this.resolvedType, pc);
			}
			if (!valueRequired) {
				// the resolveType cannot be double or long
				codeStream.pop();
			}
		}
		codeStream.recordPositionsFrom(pc, this.sourceStart);
		return;
	}
	this.expression.generateCode(currentScope, codeStream, annotatedCast || valueRequired || needRuntimeCheckcast);
	if (annotatedCast || (needRuntimeCheckcast && TypeBinding.notEquals(this.expression.postConversionType(currentScope), this.resolvedType.erasure()))) { // no need to issue a checkcast if already done as genericCast
		codeStream.checkcast(this.type, this.resolvedType, pc);
	}
	if (valueRequired) {
		codeStream.generateImplicitConversion(this.implicitConversion);
	} else if (annotatedCast || needRuntimeCheckcast) {
		switch (this.resolvedType.id) {
			case T_long :
			case T_double :
				codeStream.pop2();
				break;
			default :
				codeStream.pop();
				break;
		}
	}
	codeStream.recordPositionsFrom(pc, this.sourceStart);
}

public Expression innermostCastedExpression(){
	Expression current = this.expression;
	while (current instanceof CastExpression) {
		current = ((CastExpression) current).expression;
	}
	return current;
}

/**
 * @see org.eclipse.jdt.internal.compiler.ast.Expression#localVariableBinding()
 */
@Override
public LocalVariableBinding localVariableBinding() {
	return this.expression.localVariableBinding();
}

@Override
public int nullStatus(FlowInfo flowInfo, FlowContext flowContext) {
	if ((this.implicitConversion & TypeIds.BOXING) != 0)
		return FlowInfo.NON_NULL;
	return this.expression.nullStatus(flowInfo, flowContext);
}

/**
 * @see org.eclipse.jdt.internal.compiler.ast.Expression#optimizedBooleanConstant()
 */
@Override
public Constant optimizedBooleanConstant() {
	switch(this.resolvedType.id) {
		case T_boolean :
		case T_JavaLangBoolean :
			return this.expression.optimizedBooleanConstant();
	}
	return Constant.NotAConstant;
}

@Override
public StringBuffer printExpression(int indent, StringBuffer output) {
	int parenthesesCount = (this.bits & ASTNode.ParenthesizedMASK) >> ASTNode.ParenthesizedSHIFT;
	String suffix = ""; //$NON-NLS-1$
	for(int i = 0; i < parenthesesCount; i++) {
		output.append('(');
		suffix += ')';
	}
	output.append('(');
	this.type.print(0, output).append(") "); //$NON-NLS-1$
	return this.expression.printExpression(0, output).append(suffix);
}

@Override
public TypeBinding resolveType(BlockScope scope) {
	// compute a new constant if the cast is effective

	this.constant = Constant.NotAConstant;
	this.implicitConversion = TypeIds.T_undefined;

	boolean exprContainCast = false;

	TypeBinding castType = this.resolvedType = this.type.resolveType(scope);
	if (scope.compilerOptions().sourceLevel >= ClassFileConstants.JDK1_8) {
		this.expression.setExpressionContext(CASTING_CONTEXT);
		if (this.expression instanceof FunctionalExpression) {
			this.expression.setExpectedType(this.resolvedType);
			this.bits |= ASTNode.DisableUnnecessaryCastCheck;
		}
	}
	if (this.expression instanceof CastExpression) {
		this.expression.bits |= ASTNode.DisableUnnecessaryCastCheck;
		exprContainCast = true;
	}
	TypeBinding expressionType = this.expression.resolveType(scope);
	if (this.expression instanceof MessageSend) {
		MessageSend messageSend = (MessageSend) this.expression;
		MethodBinding methodBinding = messageSend.binding;
		if (methodBinding != null && methodBinding.isPolymorphic()) {
			messageSend.binding = scope.environment().updatePolymorphicMethodReturnType((PolymorphicMethodBinding) methodBinding, castType);
			if (TypeBinding.notEquals(expressionType, castType)) {
				expressionType = castType;
				this.bits |= ASTNode.DisableUnnecessaryCastCheck;
			}
		}
	}
	if (castType != null) {
		if (expressionType != null) {

			boolean nullAnnotationMismatch = scope.compilerOptions().isAnnotationBasedNullAnalysisEnabled
					&& NullAnnotationMatching.analyse(castType, expressionType, -1).isAnyMismatch();

			if (this.instanceofType != null && expressionType.isParameterizedType()
					&& expressionType.isProvablyDistinct(this.instanceofType)) {
				this.bits |= ASTNode.DisableUnnecessaryCastCheck;
			}
			if (this.isVarTypeDeclaration && TypeBinding.notEquals(expressionType, castType)) {
				this.bits |= ASTNode.DisableUnnecessaryCastCheck;
			}
			boolean isLegal = checkCastTypesCompatibility(scope, castType, expressionType, this.expression, true);
			if (isLegal) {
				this.expression.computeConversion(scope, castType, expressionType);
				if ((this.bits & ASTNode.UnsafeCast) != 0) { // unsafe cast
					if (scope.compilerOptions().reportUnavoidableGenericTypeProblems
							|| !(expressionType.isRawType() && this.expression.forcedToBeRaw(scope.referenceContext()))) {
						scope.problemReporter().unsafeCast(this, scope);
					}
				} else if (nullAnnotationMismatch) {
					// report null annotation issue at medium priority
					scope.problemReporter().unsafeNullnessCast(this, scope);
				} else {
					if (castType.isRawType() && scope.compilerOptions().getSeverity(CompilerOptions.RawTypeReference) != ProblemSeverities.Ignore){
						scope.problemReporter().rawTypeReference(this.type, castType);
					}
					if ((this.bits & (ASTNode.UnnecessaryCast|ASTNode.DisableUnnecessaryCastCheck)) == ASTNode.UnnecessaryCast) { // unnecessary cast
						if (!isIndirectlyUsed()) // used for generic type inference or boxing ?
							scope.problemReporter().unnecessaryCast(this);
					}
				}
			} else { // illegal cast
				if ((castType.tagBits & TagBits.HasMissingType) == 0) { // no complaint if secondary error
					scope.problemReporter().typeCastError(this, castType, expressionType);
				}
				this.bits |= ASTNode.DisableUnnecessaryCastCheck; // disable further secondary diagnosis
			}
		}
		this.resolvedType = castType.capture(scope, this.type.sourceStart, this.type.sourceEnd); // make it unique, a cast expression shares source end with the expression.
		if (exprContainCast) {
			checkNeedForCastCast(scope, this);
		}
	}
	return this.resolvedType;
}

/**
 * @see org.eclipse.jdt.internal.compiler.ast.Expression#setExpectedType(org.eclipse.jdt.internal.compiler.lookup.TypeBinding)
 */
@Override
public void setExpectedType(TypeBinding expectedType) {
	this.expectedType = expectedType;
}

/**
 * Determines whether apparent unnecessary cast wasn't actually used to
 * perform return type inference of generic method invocation or boxing.
 */
private boolean isIndirectlyUsed() {
	if (this.expression instanceof MessageSend) {
		MethodBinding method = ((MessageSend)this.expression).binding;
		if (method instanceof ParameterizedGenericMethodBinding
					&& ((ParameterizedGenericMethodBinding)method).inferredReturnType) {
			if (this.expectedType == null)
				return true;
			if (TypeBinding.notEquals(this.resolvedType, this.expectedType))
				return true;
		}
	}
	if (this.expectedType != null && this.resolvedType.isBaseType() && !this.resolvedType.isCompatibleWith(this.expectedType)) {
		// boxing: Short s = (short) _byte
		return true;
	}
	return false;
}

/**
 * @see org.eclipse.jdt.internal.compiler.ast.Expression#tagAsNeedCheckCast()
 */
@Override
public void tagAsNeedCheckCast() {
	this.bits |= ASTNode.GenerateCheckcast;
}

/**
 * @see org.eclipse.jdt.internal.compiler.ast.Expression#tagAsUnnecessaryCast(Scope, TypeBinding)
 */
@Override
public void tagAsUnnecessaryCast(Scope scope, TypeBinding castType) {
	this.bits |= ASTNode.UnnecessaryCast;
}

public void setInstanceofType(TypeBinding instanceofTypeBinding) {
	this.instanceofType = instanceofTypeBinding;
}

public void setVarTypeDeclaration(boolean value) {
	this.isVarTypeDeclaration = value;
}

@Override
public void traverse(ASTVisitor visitor, BlockScope blockScope) {
	if (visitor.visit(this, blockScope)) {
		this.type.traverse(visitor, blockScope);
		this.expression.traverse(visitor, blockScope);
	}
	visitor.endVisit(this, blockScope);
}
}
