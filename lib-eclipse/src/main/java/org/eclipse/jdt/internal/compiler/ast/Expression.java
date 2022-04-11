/*******************************************************************************
 * Copyright (c) 2000, 2021 IBM Corporation and others.
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
 *     Stephan Herrmann - Contributions for
 *								bug 292478 - Report potentially null across variable assignment
 *								bug 345305 - [compiler][null] Compiler misidentifies a case of "variable can only be null"
 *								bug 392862 - [1.8][compiler][null] Evaluate null annotations on array types
 *								bug 331649 - [compiler][null] consider null annotations for fields
 *								bug 383368 - [compiler][null] syntactic null analysis for field references
 *								bug 400761 - [compiler][null] null may be return as boolean without a diagnostic
 *								bug 402993 - [null] Follow up of bug 401088: Missing warning about redundant null check
 *								bug 403147 - [compiler][null] FUP of bug 400761: consolidate interaction between unboxing, NPE, and deferred checking
 *								Bug 392099 - [1.8][compiler][null] Apply null annotation on types for null analysis
 *								Bug 417295 - [1.8[[null] Massage type annotated null analysis to gel well with deep encoded type bindings.
 *								Bug 400874 - [1.8][compiler] Inference infrastructure should evolve to meet JLS8 18.x (Part G of JSR335 spec)
 *								Bug 426792 - [1.8][inference][impl] generify new type inference engine
 *								Bug 423505 - [1.8] Implement "18.5.4 More Specific Method Inference"
 *								Bug 427438 - [1.8][compiler] NPE at org.eclipse.jdt.internal.compiler.ast.ConditionalExpression.generateCode(ConditionalExpression.java:280)
 *								Bug 426996 - [1.8][inference] try to avoid method Expression.unresolve()?
 *								Bug 428274 - [1.8] [compiler] Cannot cast from Number to double
 *								Bug 428352 - [1.8][compiler] Resolution errors don't always surface
 *								Bug 452788 - [1.8][compiler] Type not correctly inferred in lambda expression
 *     Lars Vogel <Lars.Vogel@vogella.com> - Contributions for
 *     						Bug 473178
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.ast;

import java.util.ArrayList;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.codegen.BranchLabel;
import org.eclipse.jdt.internal.compiler.codegen.CodeStream;
import org.eclipse.jdt.internal.compiler.flow.FlowContext;
import org.eclipse.jdt.internal.compiler.flow.FlowInfo;
import org.eclipse.jdt.internal.compiler.impl.Constant;
import org.eclipse.jdt.internal.compiler.impl.ReferenceContext;
import org.eclipse.jdt.internal.compiler.lookup.ArrayBinding;
import org.eclipse.jdt.internal.compiler.lookup.BaseTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.Binding;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.FieldBinding;
import org.eclipse.jdt.internal.compiler.lookup.InferenceContext18;
import org.eclipse.jdt.internal.compiler.lookup.LocalVariableBinding;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.Scope;
import org.eclipse.jdt.internal.compiler.lookup.TagBits;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeIds;
import org.eclipse.jdt.internal.compiler.lookup.TypeVariableBinding;
import org.eclipse.jdt.internal.compiler.lookup.VariableBinding;
import org.eclipse.jdt.internal.compiler.lookup.WildcardBinding;
import org.eclipse.jdt.internal.compiler.problem.ShouldNotImplement;
import org.eclipse.jdt.internal.compiler.util.Messages;

public abstract class Expression extends Statement {

	public Constant constant;

	public int statementEnd = -1;

	//Some expression may not be used - from a java semantic point
	//of view only - as statements. Other may. In order to avoid the creation
	//of wrappers around expression in order to tune them as expression
	//Expression is a subclass of Statement. See the message isValidJavaStatement()

	public int implicitConversion;
	public TypeBinding resolvedType;

	public static Expression [] NO_EXPRESSIONS = new Expression[0];


public static final boolean isConstantValueRepresentable(Constant constant, int constantTypeID, int targetTypeID) {
	//true if there is no loss of precision while casting.
	// constantTypeID == constant.typeID
	if (targetTypeID == constantTypeID)
		return true;
	switch (targetTypeID) {
		case T_char :
			switch (constantTypeID) {
				case T_char :
					return true;
				case T_double :
					return constant.doubleValue() == constant.charValue();
				case T_float :
					return constant.floatValue() == constant.charValue();
				case T_int :
					return constant.intValue() == constant.charValue();
				case T_short :
					return constant.shortValue() == constant.charValue();
				case T_byte :
					return constant.byteValue() == constant.charValue();
				case T_long :
					return constant.longValue() == constant.charValue();
				default :
					return false;//boolean
			}

		case T_float :
			switch (constantTypeID) {
				case T_char :
					return constant.charValue() == constant.floatValue();
				case T_double :
					return constant.doubleValue() == constant.floatValue();
				case T_float :
					return true;
				case T_int :
					return constant.intValue() == constant.floatValue();
				case T_short :
					return constant.shortValue() == constant.floatValue();
				case T_byte :
					return constant.byteValue() == constant.floatValue();
				case T_long :
					return constant.longValue() == constant.floatValue();
				default :
					return false;//boolean
			}

		case T_double :
			switch (constantTypeID) {
				case T_char :
					return constant.charValue() == constant.doubleValue();
				case T_double :
					return true;
				case T_float :
					return constant.floatValue() == constant.doubleValue();
				case T_int :
					return constant.intValue() == constant.doubleValue();
				case T_short :
					return constant.shortValue() == constant.doubleValue();
				case T_byte :
					return constant.byteValue() == constant.doubleValue();
				case T_long :
					return constant.longValue() == constant.doubleValue();
				default :
					return false; //boolean
			}

		case T_byte :
			switch (constantTypeID) {
				case T_char :
					return constant.charValue() == constant.byteValue();
				case T_double :
					return constant.doubleValue() == constant.byteValue();
				case T_float :
					return constant.floatValue() == constant.byteValue();
				case T_int :
					return constant.intValue() == constant.byteValue();
				case T_short :
					return constant.shortValue() == constant.byteValue();
				case T_byte :
					return true;
				case T_long :
					return constant.longValue() == constant.byteValue();
				default :
					return false; //boolean
			}

		case T_short :
			switch (constantTypeID) {
				case T_char :
					return constant.charValue() == constant.shortValue();
				case T_double :
					return constant.doubleValue() == constant.shortValue();
				case T_float :
					return constant.floatValue() == constant.shortValue();
				case T_int :
					return constant.intValue() == constant.shortValue();
				case T_short :
					return true;
				case T_byte :
					return constant.byteValue() == constant.shortValue();
				case T_long :
					return constant.longValue() == constant.shortValue();
				default :
					return false; //boolean
			}

		case T_int :
			switch (constantTypeID) {
				case T_char :
					return constant.charValue() == constant.intValue();
				case T_double :
					return constant.doubleValue() == constant.intValue();
				case T_float :
					return constant.floatValue() == constant.intValue();
				case T_int :
					return true;
				case T_short :
					return constant.shortValue() == constant.intValue();
				case T_byte :
					return constant.byteValue() == constant.intValue();
				case T_long :
					return constant.longValue() == constant.intValue();
				default :
					return false; //boolean
			}

		case T_long :
			switch (constantTypeID) {
				case T_char :
					return constant.charValue() == constant.longValue();
				case T_double :
					return constant.doubleValue() == constant.longValue();
				case T_float :
					return constant.floatValue() == constant.longValue();
				case T_int :
					return constant.intValue() == constant.longValue();
				case T_short :
					return constant.shortValue() == constant.longValue();
				case T_byte :
					return constant.byteValue() == constant.longValue();
				case T_long :
					return true;
				default :
					return false; //boolean
			}

		default :
			return false; //boolean
	}
}

public Expression() {
	super();
}

@Override
public FlowInfo analyseCode(BlockScope currentScope, FlowContext flowContext, FlowInfo flowInfo) {
	return flowInfo;
}

/**
 * More sophisticated for of the flow analysis used for analyzing expressions, and be able to optimize out
 * portions of expressions where no actual value is required.
 *
 * @param currentScope
 * @param flowContext
 * @param flowInfo
 * @param valueRequired
 * @return The state of initialization after the analysis of the current expression
 */
public FlowInfo analyseCode(BlockScope currentScope, FlowContext flowContext, FlowInfo flowInfo, boolean valueRequired) {
	return analyseCode(currentScope, flowContext, flowInfo);
}

/**
 * Back-propagation of flow info: before analysing a branch where a given condition is known to hold true/false respectively,
 * ask the condition to contribute its information to the given flowInfo.
 * @param flowInfo the info to be used for analysing the branch
 * @param result condition result that would cause entering the branch
 */
protected void updateFlowOnBooleanResult(FlowInfo flowInfo, boolean result) {
	// nop
}

/**
 * Returns false if cast is not legal.
 */
public final boolean checkCastTypesCompatibility(Scope scope, TypeBinding castType, TypeBinding expressionType, Expression expression, boolean useAutoBoxing) {
	// see specifications 5.5
	// handle errors and process constant when needed

	// if either one of the type is null ==>
	// some error has been already reported some where ==>
	// we then do not report an obvious-cascade-error.

	if (castType == null || expressionType == null) return true;

	// identity conversion cannot be performed upfront, due to side-effects
	// like constant propagation
	boolean use15specifics = scope.compilerOptions().sourceLevel >= ClassFileConstants.JDK1_5;
	boolean use17specifics = scope.compilerOptions().sourceLevel >= ClassFileConstants.JDK1_7;
	useAutoBoxing &= use15specifics;
	if (castType.isBaseType()) {
		if (expressionType.isBaseType()) {
			if (TypeBinding.equalsEquals(expressionType, castType)) {
				if (expression != null) {
					this.constant = expression.constant; //use the same constant
				}
				tagAsUnnecessaryCast(scope, castType);
				return true;
			}
			boolean necessary = false;
			if (expressionType.isCompatibleWith(castType)
					|| (necessary = BaseTypeBinding.isNarrowing(castType.id, expressionType.id))) {
				if (expression != null) {
					expression.implicitConversion = (castType.id << 4) + expressionType.id;
					if (expression.constant != Constant.NotAConstant) {
						this.constant = expression.constant.castTo(expression.implicitConversion);
					}
				}
				if (!necessary) tagAsUnnecessaryCast(scope, castType);
				return true;

			}
		} else if (useAutoBoxing && use17specifics && castType.isPrimitiveType() && expressionType instanceof ReferenceBinding &&
				!expressionType.isBoxedPrimitiveType() && checkCastTypesCompatibility(scope, scope.boxing(castType), expressionType, expression, useAutoBoxing)) {
			// cast from any reference type (other than boxing types) to base type allowed from 1.7, see JLS $5.5
			// by our own interpretation (in accordance with javac) we reject arays, though.
			return true;
		} else if (useAutoBoxing
							&& scope.environment().computeBoxingType(expressionType).isCompatibleWith(castType)) { // unboxing - only widening match is allowed
			tagAsUnnecessaryCast(scope, castType);
			return true;
		}
		return false;
	} else if (useAutoBoxing
						&& expressionType.isBaseType()
						&& scope.environment().computeBoxingType(expressionType).isCompatibleWith(castType)) { // boxing - only widening match is allowed
		tagAsUnnecessaryCast(scope, castType);
		return true;
	}

	if (castType.isIntersectionType18()) {
		ReferenceBinding [] intersectingTypes = castType.getIntersectingTypes();
		for (int i = 0, length = intersectingTypes.length; i < length; i++) {
			if (!checkCastTypesCompatibility(scope, intersectingTypes[i], expressionType, expression, useAutoBoxing))
				return false;
		}
		return true;
	}

	switch(expressionType.kind()) {
		case Binding.BASE_TYPE :
			//-----------cast to something which is NOT a base type--------------------------
			if (expressionType == TypeBinding.NULL) {
				tagAsUnnecessaryCast(scope, castType);
				return true; //null is compatible with every thing
			}
			return false;

		case Binding.ARRAY_TYPE :
			if (TypeBinding.equalsEquals(castType, expressionType)) {
				tagAsUnnecessaryCast(scope, castType);
				return true; // identity conversion
			}
			switch (castType.kind()) {
				case Binding.ARRAY_TYPE :
					// ( ARRAY ) ARRAY
					TypeBinding castElementType = ((ArrayBinding) castType).elementsType();
					TypeBinding exprElementType = ((ArrayBinding) expressionType).elementsType();
					if (exprElementType.isBaseType() || castElementType.isBaseType()) {
						if (TypeBinding.equalsEquals(castElementType, exprElementType)) {
							tagAsNeedCheckCast();
							return true;
						}
						return false;
					}
					// recurse on array type elements
					return checkCastTypesCompatibility(scope, castElementType, exprElementType, expression, useAutoBoxing);

				case Binding.TYPE_PARAMETER :
					// ( TYPE_PARAMETER ) ARRAY
					TypeBinding match = expressionType.findSuperTypeOriginatingFrom(castType);
					if (match == null) {
						checkUnsafeCast(scope, castType, expressionType, null /*no match*/, true);
					}
					for (TypeBinding bound : ((TypeVariableBinding) castType).allUpperBounds()) {
						if (!checkCastTypesCompatibility(scope, bound, expressionType, expression, useAutoBoxing))
							return false;
					}
					return true;

				default:
					// ( CLASS/INTERFACE ) ARRAY
					switch (castType.id) {
						case T_JavaLangCloneable :
						case T_JavaIoSerializable :
							tagAsNeedCheckCast();
							return true;
						case T_JavaLangObject :
							tagAsUnnecessaryCast(scope, castType);
							return true;
						default :
							return false;
					}
			}

		case Binding.TYPE_PARAMETER :
			TypeBinding match = expressionType.findSuperTypeOriginatingFrom(castType);
			if (match == null) {
				// recursively on the type variable upper bounds
				if (castType instanceof TypeVariableBinding) {
					// prefer iterating over required types, not provides
					for (TypeBinding bound : ((TypeVariableBinding)castType).allUpperBounds()) {
						if (!checkCastTypesCompatibility(scope, bound, expressionType, expression, useAutoBoxing))
							return false;
					}
				} else {
					for (TypeBinding bound : ((TypeVariableBinding)expressionType).allUpperBounds()) {
						if (!checkCastTypesCompatibility(scope, castType, bound, expression, useAutoBoxing))
							return false;
					}
				}
			}
			// if no incompatibility found:
			return checkUnsafeCast(scope, castType, expressionType, match, match == null);

		case Binding.WILDCARD_TYPE :
		case Binding.INTERSECTION_TYPE :
			match = expressionType.findSuperTypeOriginatingFrom(castType);
			if (match != null) {
				return checkUnsafeCast(scope, castType, expressionType, match, false);
			}
			TypeBinding bound = ((WildcardBinding)expressionType).bound;
			if (bound == null) bound = scope.getJavaLangObject();
			// recursively on the type variable upper bound
			return checkCastTypesCompatibility(scope, castType, bound, expression, useAutoBoxing);
		case Binding.INTERSECTION_TYPE18:
			ReferenceBinding [] intersectingTypes = expressionType.getIntersectingTypes();
			for (int i = 0, length = intersectingTypes.length; i < length; i++) {
				if (checkCastTypesCompatibility(scope, castType, intersectingTypes[i], expression, useAutoBoxing))
					return true;
			}
			return false;
		default:
			if (expressionType.isInterface()) {
				switch (castType.kind()) {
					case Binding.ARRAY_TYPE :
						// ( ARRAY ) INTERFACE
						switch (expressionType.id) {
							case T_JavaLangCloneable :
							case T_JavaIoSerializable :
								tagAsNeedCheckCast();
								return true;
							default :
								return false;
						}

					case Binding.TYPE_PARAMETER :
						// ( INTERFACE ) TYPE_PARAMETER
						match = expressionType.findSuperTypeOriginatingFrom(castType);
						if (match == null) {
							checkUnsafeCast(scope, castType, expressionType, null /*no match*/, true);
						}
						// recursively on the type variable upper bounds
						for (TypeBinding upperBound : ((TypeVariableBinding)castType).allUpperBounds()) {
							if (!checkCastTypesCompatibility(scope, upperBound, expressionType, expression, useAutoBoxing))
								return false;
						}
						return true;

					default :
						if (castType.isInterface()) {
							// ( INTERFACE ) INTERFACE
							ReferenceBinding interfaceType = (ReferenceBinding) expressionType;
							match = interfaceType.findSuperTypeOriginatingFrom(castType);
							if (match != null) {
								return checkUnsafeCast(scope, castType, interfaceType, match, false);
							}
							tagAsNeedCheckCast();
							match = castType.findSuperTypeOriginatingFrom(interfaceType);
							if (match != null) {
								return checkUnsafeCast(scope, castType, interfaceType, match, true);
							}
							if (use15specifics) {
								checkUnsafeCast(scope, castType, expressionType, null /*no match*/, true);
								// ensure there is no collision between both interfaces: i.e. I1 extends List<String>, I2 extends List<Object>
								if (scope.compilerOptions().complianceLevel < ClassFileConstants.JDK1_7) {
									if (interfaceType.hasIncompatibleSuperType((ReferenceBinding) castType)) {
										return false;
									}
								} else if (!castType.isRawType() && interfaceType.hasIncompatibleSuperType((ReferenceBinding) castType)) {
									return false;
								}
							} else {
								// pre1.5 semantics - no covariance allowed (even if 1.5 compliant, but 1.4 source)
								// look at original methods rather than the parameterized variants at 1.4 to detect
								// covariance. Otherwise when confronted with one raw type and one parameterized type,
								// we could mistakenly detect covariance and scream foul. See https://bugs.eclipse.org/bugs/show_bug.cgi?id=332744
								MethodBinding[] castTypeMethods = getAllOriginalInheritedMethods((ReferenceBinding) castType);
								MethodBinding[] expressionTypeMethods = getAllOriginalInheritedMethods((ReferenceBinding) expressionType);
								int exprMethodsLength = expressionTypeMethods.length;
								for (int i = 0, castMethodsLength = castTypeMethods.length; i < castMethodsLength; i++) {
									for (int j = 0; j < exprMethodsLength; j++) {
										if ((TypeBinding.notEquals(castTypeMethods[i].returnType, expressionTypeMethods[j].returnType))
												&& (CharOperation.equals(castTypeMethods[i].selector, expressionTypeMethods[j].selector))
												&& castTypeMethods[i].areParametersEqual(expressionTypeMethods[j])) {
											return false;

										}
									}
								}
							}
							return true;
						} else {
							// ( CLASS ) INTERFACE
							if (castType.id == TypeIds.T_JavaLangObject) { // no runtime error
								tagAsUnnecessaryCast(scope, castType);
								return true;
							}
							// can only be a downcast
							tagAsNeedCheckCast();
							match = castType.findSuperTypeOriginatingFrom(expressionType);
							if (match != null) {
								return checkUnsafeCast(scope, castType, expressionType, match, true);
							}
							if (((ReferenceBinding) castType).isFinal()) {
								// no subclass for castType, thus compile-time check is invalid
								return false;
							}
							if (use15specifics) {
								checkUnsafeCast(scope, castType, expressionType, null /*no match*/, true);
								// ensure there is no collision between both interfaces: i.e. I1 extends List<String>, I2 extends List<Object>
								if (scope.compilerOptions().complianceLevel < ClassFileConstants.JDK1_7) {
									if (((ReferenceBinding)castType).hasIncompatibleSuperType((ReferenceBinding) expressionType)) {
										return false;
									}
								} else if (!castType.isRawType() && ((ReferenceBinding)castType).hasIncompatibleSuperType((ReferenceBinding) expressionType)) {
									return false;
								}
							}
							return true;
						}
				}
			} else {
				switch (castType.kind()) {
					case Binding.ARRAY_TYPE :
						// ( ARRAY ) CLASS
						if (expressionType.id == TypeIds.T_JavaLangObject) { // potential runtime error
							if (use15specifics) checkUnsafeCast(scope, castType, expressionType, expressionType, true);
							tagAsNeedCheckCast();
							return true;
						}
						return false;

					case Binding.TYPE_PARAMETER :
						// ( TYPE_PARAMETER ) CLASS
						match = expressionType.findSuperTypeOriginatingFrom(castType);
						if (match == null) {
							checkUnsafeCast(scope, castType, expressionType, null, true);
						}
						// recursively on the type variable upper bounds
						for (TypeBinding upperBound : ((TypeVariableBinding)castType).allUpperBounds()) {
							if (!checkCastTypesCompatibility(scope, upperBound, expressionType, expression, useAutoBoxing))
								return false;
						}
						return true;

					default :
						if (castType.isInterface()) {
							// ( INTERFACE ) CLASS
							ReferenceBinding refExprType = (ReferenceBinding) expressionType;
							match = refExprType.findSuperTypeOriginatingFrom(castType);
							if (match != null) {
								return checkUnsafeCast(scope, castType, expressionType, match, false);
							}
							// unless final a subclass may implement the interface ==> no check at compile time
							if (refExprType.isFinal()) {
								return false;
							}
							tagAsNeedCheckCast();
							match = castType.findSuperTypeOriginatingFrom(expressionType);
							if (match != null) {
								return checkUnsafeCast(scope, castType, expressionType, match, true);
							}
							if (use15specifics) {
								checkUnsafeCast(scope, castType, expressionType, null /*no match*/, true);
								// ensure there is no collision between both interfaces: i.e. I1 extends List<String>, I2 extends List<Object>
								if (scope.compilerOptions().complianceLevel < ClassFileConstants.JDK1_7) {
									if (refExprType.hasIncompatibleSuperType((ReferenceBinding) castType)) {
										return false;
									}
								} else if (!castType.isRawType() && refExprType.hasIncompatibleSuperType((ReferenceBinding) castType)) {
									return false;
								}
							}
							return true;
						} else {
							// ( CLASS ) CLASS
							match = expressionType.findSuperTypeOriginatingFrom(castType);
							if (match != null) {
								if (expression != null && castType.id == TypeIds.T_JavaLangString) this.constant = expression.constant; // (String) cst is still a constant
								return checkUnsafeCast(scope, castType, expressionType, match, false);
							}
							match = castType.findSuperTypeOriginatingFrom(expressionType);
							if (match != null) {
								tagAsNeedCheckCast();
								return checkUnsafeCast(scope, castType, expressionType, match, true);
							}
							return false;
						}
				}
			}
	}
}

/**
 * Check this expression against potential NPEs, which may occur:
 * <ul>
 * <li>if the expression is the receiver in a field access, qualified allocation, array reference or message send
 * 		incl. implicit message sends like it happens for the collection in a foreach statement.</li>
 * <li>if the expression is subject to unboxing</li>
 * <li>if the expression is the exception in a throw statement</li>
 * </ul>
 * If a risk of NPE is detected report it to the context.
 * If the expression denotes a local variable, mark it as checked, which affects the flow info.
 * @param scope the scope of the analysis
 * @param flowContext the current flow context
 * @param flowInfo the upstream flow info; caveat: may get modified
 * @param ttlForFieldCheck if this is a reference to a field we will mark that field as nonnull for the specified timeToLive
 * @return could this expression be checked by the current implementation?
 */
public boolean checkNPE(BlockScope scope, FlowContext flowContext, FlowInfo flowInfo, int ttlForFieldCheck) {
	boolean isNullable = false;
	if (this.resolvedType != null) {
		// 1. priority: @NonNull
		if ((this.resolvedType.tagBits & TagBits.AnnotationNonNull) != 0) {
			return true; // no danger
		} else if ((this.resolvedType.tagBits & TagBits.AnnotationNullable) != 0) {
			isNullable = true;
		}
	}
	LocalVariableBinding local = localVariableBinding();
	if (local != null &&
			(local.type.tagBits & TagBits.IsBaseType) == 0) {
		// 2. priority: local with flow analysis (via the FlowContext)
		if ((this.bits & ASTNode.IsNonNull) == 0) {
			flowContext.recordUsingNullReference(scope, local, this,
					FlowContext.MAY_NULL, flowInfo);
			// account for possible NPE:
			if (!flowInfo.isDefinitelyNonNull(local)) {
				flowContext.recordAbruptExit();
			}
		}
		flowInfo.markAsComparedEqualToNonNull(local);
			// from thereon it is set
		flowContext.markFinallyNullStatus(local, FlowInfo.NON_NULL);
		return true;
	} else if (isNullable) {
		// 3. priority: @Nullable without a local
		scope.problemReporter().dereferencingNullableExpression(this);
		return true;
	}
	return false; // not checked
}
public boolean checkNPE(BlockScope scope, FlowContext flowContext, FlowInfo flowInfo) {
	return checkNPE(scope, flowContext, flowInfo, 0); // default: don't mark field references as checked for null
}

/** If this expression requires unboxing check if that operation can throw NPE. */
protected void checkNPEbyUnboxing(BlockScope scope, FlowContext flowContext, FlowInfo flowInfo) {
	int status;
	if ((this.implicitConversion & UNBOXING) != 0
			&& (this.bits & ASTNode.IsNonNull) == 0
			&& (status = nullStatus(flowInfo, flowContext)) != FlowInfo.NON_NULL)
	{
		flowContext.recordUnboxing(scope, this, status, flowInfo);
	}
}

public boolean checkUnsafeCast(Scope scope, TypeBinding castType, TypeBinding expressionType, TypeBinding match, boolean isNarrowing) {
	if (TypeBinding.equalsEquals(match, castType)) {
		if (!isNarrowing) tagAsUnnecessaryCast(scope, castType);
		return true;
	}
	if (match != null && (!castType.isReifiable() || !expressionType.isReifiable())) {
		if(isNarrowing
				? match.isProvablyDistinct(expressionType)
				: castType.isProvablyDistinct(match)) {
			return false;
		}
	}
	if (!isNarrowing) tagAsUnnecessaryCast(scope, castType);
	return true;
}

/**
 * Base types need that the widening is explicitly done by the compiler using some bytecode like i2f.
 * Also check unsafe type operations.
 */
public void computeConversion(Scope scope, TypeBinding runtimeType, TypeBinding compileTimeType) {
	if (runtimeType == null || compileTimeType == null)
		return;
	if (this.implicitConversion != 0) return; // already set independently

	// it is possible for a Byte to be unboxed to a byte & then converted to an int
	// but it is not possible for a byte to become Byte & then assigned to an Integer,
	// or to become an int before boxed into an Integer
	if (runtimeType != TypeBinding.NULL && runtimeType.isBaseType()) {
		if (!compileTimeType.isBaseType()) {
			TypeBinding unboxedType = scope.environment().computeBoxingType(compileTimeType);
			this.implicitConversion = TypeIds.UNBOXING;
			scope.problemReporter().autoboxing(this, compileTimeType, runtimeType);
			compileTimeType = unboxedType;
		}
	} else if (compileTimeType != TypeBinding.NULL && compileTimeType.isBaseType()) {
		TypeBinding boxedType = scope.environment().computeBoxingType(runtimeType);
		if (TypeBinding.equalsEquals(boxedType, runtimeType)) // Object o = 12;
			boxedType = compileTimeType;
		if (boxedType.id > TypeIds.T_JavaLangBoolean) {       // (Comparable & Serializable) 0
			boxedType = compileTimeType;
		}
		this.implicitConversion = TypeIds.BOXING | (boxedType.id << 4) + compileTimeType.id;
		scope.problemReporter().autoboxing(this, compileTimeType, scope.environment().computeBoxingType(boxedType));
		return;
	} else if (this.constant != Constant.NotAConstant && this.constant.typeID() != TypeIds.T_JavaLangString) {
		this.implicitConversion = TypeIds.BOXING;
		return;
	}
	int compileTimeTypeID, runtimeTypeID;
	if ((compileTimeTypeID = compileTimeType.id) >= TypeIds.T_LastWellKnownTypeId) { // e.g. ? extends String  ==> String (103227); >= TypeIds.T_LastWellKnownTypeId implies TypeIds.NoId
		compileTimeTypeID = compileTimeType.erasure().id == TypeIds.T_JavaLangString ? TypeIds.T_JavaLangString : TypeIds.T_JavaLangObject;
	} else if (runtimeType.isPrimitiveType() && compileTimeType instanceof ReferenceBinding && !compileTimeType.isBoxedPrimitiveType()) {
		compileTimeTypeID = TypeIds.T_JavaLangObject; // treatment is the same as for jlO.
	}

	switch (runtimeTypeID = runtimeType.id) {
		case T_byte :
		case T_short :
		case T_char :
			if (compileTimeTypeID == TypeIds.T_JavaLangObject) {
				this.implicitConversion |= (runtimeTypeID << 4) + compileTimeTypeID;
			} else {
				this.implicitConversion |= (TypeIds.T_int << 4) + compileTimeTypeID;
			}
			break;
		case T_JavaLangString :
		case T_float :
		case T_boolean :
		case T_double :
		case T_int : //implicitConversion may result in i2i which will result in NO code gen
		case T_long :
			this.implicitConversion |= (runtimeTypeID << 4) + compileTimeTypeID;
			break;
		default : // regular object ref
//				if (compileTimeType.isRawType() && runtimeTimeType.isBoundParameterizedType()) {
//				    scope.problemReporter().unsafeRawExpression(this, compileTimeType, runtimeTimeType);
//				}
	}
}

public static int computeNullStatus(int status, int combinedStatus) {
	if ((combinedStatus & (FlowInfo.NULL|FlowInfo.POTENTIALLY_NULL)) != 0)
		status |= FlowInfo.POTENTIALLY_NULL;
	if ((combinedStatus & (FlowInfo.NON_NULL|FlowInfo.POTENTIALLY_NON_NULL)) != 0)
		status |= FlowInfo.POTENTIALLY_NON_NULL;
	if ((combinedStatus & (FlowInfo.UNKNOWN|FlowInfo.POTENTIALLY_UNKNOWN)) != 0)
		status |= FlowInfo.POTENTIALLY_UNKNOWN;
	return status;
}
/**
 * Expression statements are plain expressions, however they generate like
 * normal expressions with no value required.
 *
 * @param currentScope org.eclipse.jdt.internal.compiler.lookup.BlockScope
 * @param codeStream org.eclipse.jdt.internal.compiler.codegen.CodeStream
 */
@Override
public void generateCode(BlockScope currentScope, CodeStream codeStream) {
	if ((this.bits & ASTNode.IsReachable) == 0) {
		return;
	}
	generateCode(currentScope, codeStream, false);
}

/**
 * Every expression is responsible for generating its implicit conversion when necessary.
 *
 * @param currentScope org.eclipse.jdt.internal.compiler.lookup.BlockScope
 * @param codeStream org.eclipse.jdt.internal.compiler.codegen.CodeStream
 * @param valueRequired boolean
 */
public void generateCode(BlockScope currentScope, CodeStream codeStream, boolean valueRequired) {
	if (this.constant != Constant.NotAConstant) {
		// generate a constant expression
		int pc = codeStream.position;
		codeStream.generateConstant(this.constant, this.implicitConversion);
		codeStream.recordPositionsFrom(pc, this.sourceStart);
	} else {
		// actual non-constant code generation
		throw new ShouldNotImplement(Messages.ast_missingCode);
	}
}
public void addPatternVariables(BlockScope scope, CodeStream codeStream) {
	// Nothing by default
}
public LocalDeclaration getPatternVariableIntroduced() {
	return null;
}
public void collectPatternVariablesToScope(LocalVariableBinding[] variables, BlockScope scope) {
	new ASTVisitor() {
		LocalVariableBinding[] patternVariablesInScope;
		@Override
		public boolean visit(Argument argument, BlockScope skope) {
			// Most likely to be a lambda parameter
			argument.addPatternVariablesWhenTrue(this.patternVariablesInScope);
			return true;
		}
		@Override
		public boolean visit(
				QualifiedNameReference nameReference,
				BlockScope skope) {
			nameReference.addPatternVariablesWhenTrue(this.patternVariablesInScope);
			return true;
		}
		@Override
		public boolean visit(
				SingleNameReference nameReference,
				BlockScope skope) {
			nameReference.addPatternVariablesWhenTrue(this.patternVariablesInScope);
			return true;
		}

		public void propagatePatternVariablesInScope(LocalVariableBinding[] vars, BlockScope skope) {
			this.patternVariablesInScope = vars;
			Expression.this.traverse(this, skope);
		}
	}.propagatePatternVariablesInScope(variables, scope);
}

/**
 * Default generation of a boolean value
 * @param currentScope
 * @param codeStream
 * @param trueLabel
 * @param falseLabel
 * @param valueRequired
 */
public void generateOptimizedBoolean(BlockScope currentScope, CodeStream codeStream, BranchLabel trueLabel, BranchLabel falseLabel, boolean valueRequired) {
	// a label valued to nil means: by default we fall through the case...
	// both nil means we leave the value on the stack

	Constant cst = optimizedBooleanConstant();
	generateCode(currentScope, codeStream, valueRequired && cst == Constant.NotAConstant);
	if ((cst != Constant.NotAConstant) && (cst.typeID() == TypeIds.T_boolean)) {
		int pc = codeStream.position;
		if (cst.booleanValue() == true) {
			// constant == true
			if (valueRequired) {
				if (falseLabel == null) {
					// implicit falling through the FALSE case
					if (trueLabel != null) {
						codeStream.goto_(trueLabel);
					}
				}
			}
		} else {
			if (valueRequired) {
				if (falseLabel != null) {
					// implicit falling through the TRUE case
					if (trueLabel == null) {
						codeStream.goto_(falseLabel);
					}
				}
			}
		}
		codeStream.recordPositionsFrom(pc, this.sourceStart);
		return;
	}
	// branching
	int position = codeStream.position;
	if (valueRequired) {
		if (falseLabel == null) {
			if (trueLabel != null) {
				// Implicit falling through the FALSE case
				codeStream.ifne(trueLabel);
			}
		} else {
			if (trueLabel == null) {
				// Implicit falling through the TRUE case
				codeStream.ifeq(falseLabel);
			} else {
				// No implicit fall through TRUE/FALSE --> should never occur
			}
		}
	}
	codeStream.recordPositionsFrom(position, this.sourceEnd);
}

/* Optimized (java) code generation for string concatenations that involve StringBuffer
 * creation: going through this path means that there is no need for a new StringBuffer
 * creation, further operands should rather be only appended to the current one.
 * By default: no optimization.
 */
public void generateOptimizedStringConcatenation(BlockScope blockScope, CodeStream codeStream, int typeID) {
	if (typeID == TypeIds.T_JavaLangString && this.constant != Constant.NotAConstant && this.constant.stringValue().length() == 0) {
		return; // optimize str + ""
	}
	generateCode(blockScope, codeStream, true);
	codeStream.invokeStringConcatenationAppendForType(typeID);
}

/* Optimized (java) code generation for string concatenations that involve StringBuffer
 * creation: going through this path means that there is no need for a new StringBuffer
 * creation, further operands should rather be only appended to the current one.
 */
public void generateOptimizedStringConcatenationCreation(BlockScope blockScope, CodeStream codeStream, int typeID) {
	codeStream.newStringContatenation();
	codeStream.dup();
	switch (typeID) {
		case T_JavaLangObject :
		case T_undefined :
			// in the case the runtime value of valueOf(Object) returns null, we have to use append(Object) instead of directly valueOf(Object)
			// append(Object) returns append(valueOf(Object)), which means that the null case is handled by the next case.
			codeStream.invokeStringConcatenationDefaultConstructor();
			generateCode(blockScope, codeStream, true);
			codeStream.invokeStringConcatenationAppendForType(TypeIds.T_JavaLangObject);
			return;
		case T_JavaLangString :
		case T_null :
			if (this.constant != Constant.NotAConstant) {
				String stringValue = this.constant.stringValue();
				if (stringValue.length() == 0) {  // optimize ""+<str>
					codeStream.invokeStringConcatenationDefaultConstructor();
					return;
				}
				codeStream.ldc(stringValue);
			} else {
				// null case is not a constant
				generateCode(blockScope, codeStream, true);
				codeStream.invokeStringValueOf(TypeIds.T_JavaLangObject);
			}
			break;
		default :
			generateCode(blockScope, codeStream, true);
			codeStream.invokeStringValueOf(typeID);
	}
	codeStream.invokeStringConcatenationStringConstructor();
}

private MethodBinding[] getAllOriginalInheritedMethods(ReferenceBinding binding) {
	ArrayList<MethodBinding> collector = new ArrayList<>();
	getAllInheritedMethods0(binding, collector);
	for (int i = 0, len = collector.size(); i < len; i++) {
		collector.set(i, collector.get(i).original());
	}
	return collector.toArray(new MethodBinding[collector.size()]);
}

private void getAllInheritedMethods0(ReferenceBinding binding, ArrayList<MethodBinding> collector) {
	if (!binding.isInterface()) return;
	MethodBinding[] methodBindings = binding.methods();
	for (int i = 0, max = methodBindings.length; i < max; i++) {
		collector.add(methodBindings[i]);
	}
	ReferenceBinding[] superInterfaces = binding.superInterfaces();
	for (int i = 0, max = superInterfaces.length; i < max; i++) {
		getAllInheritedMethods0(superInterfaces[i], collector);
	}
}

public static Binding getDirectBinding(Expression someExpression) {
	if ((someExpression.bits & ASTNode.IgnoreNoEffectAssignCheck) != 0) {
		return null;
	}
	if (someExpression instanceof SingleNameReference) {
		return ((SingleNameReference)someExpression).binding;
	} else if (someExpression instanceof FieldReference) {
		FieldReference fieldRef = (FieldReference)someExpression;
		if (fieldRef.receiver.isThis() && !(fieldRef.receiver instanceof QualifiedThisReference)) {
			return fieldRef.binding;
		}
	} else if (someExpression instanceof Assignment) {
		Expression lhs = ((Assignment)someExpression).lhs;
		if ((lhs.bits & ASTNode.IsStrictlyAssigned) != 0) {
			// i = i = ...; // eq to int i = ...;
			return getDirectBinding (((Assignment)someExpression).lhs);
		} else if (someExpression instanceof PrefixExpression) {
			// i = i++; // eq to ++i;
			return getDirectBinding (((Assignment)someExpression).lhs);
		}
	} else if (someExpression instanceof QualifiedNameReference) {
		QualifiedNameReference qualifiedNameReference = (QualifiedNameReference) someExpression;
		if (qualifiedNameReference.indexOfFirstFieldBinding != 1
				&& qualifiedNameReference.otherBindings == null) {
			// case where a static field is retrieved using ClassName.fieldname
			return qualifiedNameReference.binding;
		}
	} else if (someExpression.isThis()) { // https://bugs.eclipse.org/bugs/show_bug.cgi?id=276741
		return someExpression.resolvedType;
	}
//		} else if (someExpression instanceof PostfixExpression) { // recurse for postfix: i++ --> i
//			// note: "b = b++" is equivalent to doing nothing, not to "b++"
//			return getDirectBinding(((PostfixExpression) someExpression).lhs);
	return null;
}

public boolean isCompactableOperation() {
	return false;
}

//Return true if the conversion is done AUTOMATICALLY by the vm
//while the javaVM is an int based-machine, thus for example pushing
//a byte onto the stack , will automatically create an int on the stack
//(this request some work d be done by the VM on signed numbers)
public boolean isConstantValueOfTypeAssignableToType(TypeBinding constantType, TypeBinding targetType) {

	if (this.constant == Constant.NotAConstant)
		return false;
	if (TypeBinding.equalsEquals(constantType, targetType))
		return true;
	//No free assignment conversion from anything but to integral ones.
	if (BaseTypeBinding.isWidening(TypeIds.T_int, constantType.id)
			&& (BaseTypeBinding.isNarrowing(targetType.id, TypeIds.T_int))) {
		//use current explicit conversion in order to get some new value to compare with current one
		return isConstantValueRepresentable(this.constant, constantType.id, targetType.id);
	}
	return false;
}

public boolean isTypeReference() {
	return false;
}

/**
 * Returns the local variable referenced by this node. Can be a direct reference (SingleNameReference)
 * or thru a cast expression etc...
 */
public LocalVariableBinding localVariableBinding() {
	return null;
}

/**
 * Mark this expression as being non null, per a specific tag in the
 * source code.
 */
// this is no more called for now, waiting for inter procedural null reference analysis
public void markAsNonNull() {
	this.bits |= ASTNode.IsNonNull;
}

public int nullStatus(FlowInfo flowInfo, FlowContext flowContext) {
	// many kinds of expression need no analysis / are always non-null, make it the default:
	return FlowInfo.NON_NULL;
}

/**
 * Constant usable for bytecode pattern optimizations, but cannot be inlined
 * since it is not strictly equivalent to the definition of constant expressions.
 * In particular, some side-effects may be required to occur (only the end value
 * is known).
 * @return Constant known to be of boolean type
 */
public Constant optimizedBooleanConstant() {
	return this.constant;
}

public boolean isPertinentToApplicability(TypeBinding targetType, MethodBinding method) {
	return true;
}
/**
 * Returns the type of the expression after required implicit conversions. When expression type gets promoted
 * or inserted a generic cast, the converted type will differ from the resolved type (surface side-effects from
 * #computeConversion(...)).
 * @return the type after implicit conversion
 */
public TypeBinding postConversionType(Scope scope) {
	TypeBinding convertedType = this.resolvedType;
	int runtimeType = (this.implicitConversion & TypeIds.IMPLICIT_CONVERSION_MASK) >> 4;
	switch (runtimeType) {
		case T_boolean :
			convertedType = TypeBinding.BOOLEAN;
			break;
		case T_byte :
			convertedType = TypeBinding.BYTE;
			break;
		case T_short :
			convertedType = TypeBinding.SHORT;
			break;
		case T_char :
			convertedType = TypeBinding.CHAR;
			break;
		case T_int :
			convertedType = TypeBinding.INT;
			break;
		case T_float :
			convertedType = TypeBinding.FLOAT;
			break;
		case T_long :
			convertedType = TypeBinding.LONG;
			break;
		case T_double :
			convertedType = TypeBinding.DOUBLE;
			break;
		default :
	}
	if ((this.implicitConversion & TypeIds.BOXING) != 0) {
		convertedType = scope.environment().computeBoxingType(convertedType);
	}
	return convertedType;
}

@Override
public StringBuffer print(int indent, StringBuffer output) {
	printIndent(indent, output);
	return printExpression(indent, output);
}

public abstract StringBuffer printExpression(int indent, StringBuffer output);

@Override
public StringBuffer printStatement(int indent, StringBuffer output) {
	return print(indent, output).append(";"); //$NON-NLS-1$
}

@Override
public void resolve(BlockScope scope) {
	// drops the returning expression's type whatever the type is.
	this.resolveType(scope);
	return;
}
@Override
public TypeBinding resolveExpressionType(BlockScope scope) {
	return resolveType(scope);
}

/**
 * Resolve the type of this expression in the context of a blockScope
 *
 * @param scope
 * @return
 * 	Return the actual type of this expression after resolution
 */
public TypeBinding resolveType(BlockScope scope) {
	// by default... subclasses should implement a better TB if required.
	return null;
}

/**
 * Resolve the type of this expression in the context of a classScope
 *
 * @param scope
 * @return
 * 	Return the actual type of this expression after resolution
 */
public TypeBinding resolveType(ClassScope scope) {
	// by default... subclasses should implement a better TB if required.
	return null;
}

public TypeBinding resolveTypeExpecting(BlockScope scope, TypeBinding expectedType) {
	setExpectedType(expectedType); // needed in case of generic method invocation
	TypeBinding expressionType = this.resolveType(scope);
	if (expressionType == null) return null;
	if (TypeBinding.equalsEquals(expressionType, expectedType)) return expressionType;

	if (!expressionType.isCompatibleWith(expectedType)) {
		if (scope.isBoxingCompatibleWith(expressionType, expectedType)) {
			computeConversion(scope, expectedType, expressionType);
		} else {
			scope.problemReporter().typeMismatchError(expressionType, expectedType, this, null);
			return null;
		}
	}
	return expressionType;
}

public Expression resolveExpressionExpecting(TypeBinding targetType, Scope scope, InferenceContext18 context) {
	return this; // subclasses should implement for a better resolved expression if required.
}

/**
 * Returns true if the receiver is forced to be of raw type either to satisfy the contract imposed
 * by a super type or because it *is* raw and the current type has no control over it (i.e the rawness
 * originates from some other file.)
 */
public boolean forcedToBeRaw(ReferenceContext referenceContext) {
	if (this instanceof NameReference) {
		final Binding receiverBinding = ((NameReference) this).binding;
		if (receiverBinding.isParameter() && (((LocalVariableBinding) receiverBinding).tagBits & TagBits.ForcedToBeRawType) != 0) {
			return true;  // parameter is forced to be raw since super method uses raw types.
		} else if (receiverBinding instanceof FieldBinding) {
			FieldBinding field = (FieldBinding) receiverBinding;
			if (field.type.isRawType()) {
				if (referenceContext instanceof AbstractMethodDeclaration) {
					AbstractMethodDeclaration methodDecl = (AbstractMethodDeclaration) referenceContext;
					ReferenceBinding declaringClass = methodDecl.binding != null
							? methodDecl.binding.declaringClass
							: methodDecl.scope.enclosingReceiverType();
					if (TypeBinding.notEquals(field.declaringClass, declaringClass)) { // inherited raw field, see https://bugs.eclipse.org/bugs/show_bug.cgi?id=337962
						return true;
					}
				} else if (referenceContext instanceof TypeDeclaration) {
					TypeDeclaration type = (TypeDeclaration) referenceContext;
					if (TypeBinding.notEquals(field.declaringClass, type.binding)) { // inherited raw field, see https://bugs.eclipse.org/bugs/show_bug.cgi?id=337962
						return true;
					}
				}
			}
		}
	} else if (this instanceof MessageSend) {
		if (!CharOperation.equals(((MessageSend) this).binding.declaringClass.getFileName(),
				referenceContext.compilationResult().getFileName())) {  // problem is rooted elsewhere
			return true;
		}
	} else if (this instanceof FieldReference) {
		FieldBinding field = ((FieldReference) this).binding;
		if (!CharOperation.equals(field.declaringClass.getFileName(),
				referenceContext.compilationResult().getFileName())) { // problem is rooted elsewhere
			return true;
		}
		if (field.type.isRawType()) {
			if (referenceContext instanceof AbstractMethodDeclaration) {
				AbstractMethodDeclaration methodDecl = (AbstractMethodDeclaration) referenceContext;
				ReferenceBinding declaringClass = methodDecl.binding != null
						? methodDecl.binding.declaringClass
						: methodDecl.scope.enclosingReceiverType();
				if (TypeBinding.notEquals(field.declaringClass, declaringClass)) { // inherited raw field, see https://bugs.eclipse.org/bugs/show_bug.cgi?id=337962
					return true;
				}
			} else if (referenceContext instanceof TypeDeclaration) {
				TypeDeclaration type = (TypeDeclaration) referenceContext;
				if (TypeBinding.notEquals(field.declaringClass, type.binding)) { // inherited raw field, see https://bugs.eclipse.org/bugs/show_bug.cgi?id=337962
					return true;
				}
			}
		}
	} else if (this instanceof ConditionalExpression) { // https://bugs.eclipse.org/bugs/show_bug.cgi?id=337751
		ConditionalExpression ternary = (ConditionalExpression) this;
		if (ternary.valueIfTrue.forcedToBeRaw(referenceContext) || ternary.valueIfFalse.forcedToBeRaw(referenceContext)) {
			return true;
		}
	} else if (this instanceof SwitchExpression) {
		SwitchExpression se = (SwitchExpression) this;
		for (Expression e : se.resultExpressions) {
			if (e.forcedToBeRaw(referenceContext))
				return true;
		}
	}
	return false;
}

/**
 * Returns an object which can be used to identify identical JSR sequence targets
 * (see TryStatement subroutine codegen)
 * or <code>null</null> if not reusable
 */
public Object reusableJSRTarget() {
	if (this.constant != Constant.NotAConstant && (this.implicitConversion & TypeIds.BOXING) == 0) {
		return this.constant;
	}
	return null;
}

/**
 * Record the type expectation before this expression is typechecked.
 * e.g. String s = foo();, foo() will be tagged as being expected of type String
 * Used to trigger proper inference of generic method invocations.
 *
 * @param expectedType
 * 	The type denoting an expectation in the context of an assignment conversion
 */
public void setExpectedType(TypeBinding expectedType) {
    // do nothing by default
}

public void setExpressionContext(ExpressionContext context) {
	// don't care. Subclasses that are poly expressions in specific contexts should listen in and make note.
}

public boolean isCompatibleWith(TypeBinding left, Scope scope) {
	return this.resolvedType != null && this.resolvedType.isCompatibleWith(left,  scope);
}

public boolean isBoxingCompatibleWith(TypeBinding left, Scope scope) {
	return this.resolvedType != null && isBoxingCompatible(this.resolvedType, left, this, scope);
}

public boolean sIsMoreSpecific(TypeBinding s, TypeBinding t, Scope scope) {
	return s.isCompatibleWith(t, scope);
}

public boolean isExactMethodReference() {
	return false;
}

/* Answer if the receiver is a poly expression in the prevailing context. Caveat emptor: Some constructs (notably method calls)
   cannot answer this question until after resolution is over and may throw unsupported operation exception if queried ahead of
   resolution. Default implementation here returns false which is true for vast majority of AST nodes. The ones that are poly
   expressions under one or more contexts should override and return suitable value.
 */
public boolean isPolyExpression() throws UnsupportedOperationException {
	return false;
}
/** Variant of isPolyExpression() to be used during type inference, when a resolution candidate exists. */
public boolean isPolyExpression(MethodBinding method) {
	return false;
}


public void tagAsNeedCheckCast() {
    // do nothing by default
}

/**
 * Record the fact a cast expression got detected as being unnecessary.
 *
 * @param scope
 * @param castType
 */
public void tagAsUnnecessaryCast(Scope scope, TypeBinding castType) {
    // do nothing by default
}

public Expression toTypeReference() {
	//by default undefined

	//this method is meanly used by the parser in order to transform
	//an expression that is used as a type reference in a cast ....
	//--appreciate the fact that castExpression and ExpressionWithParenthesis
	//--starts with the same pattern.....

	return this;
}

/**
 * Traverse an expression in the context of a blockScope
 * @param visitor
 * @param scope
 */
@Override
public void traverse(ASTVisitor visitor, BlockScope scope) {
	// nothing to do
}

/**
 * Traverse an expression in the context of a classScope
 * @param visitor
 * @param scope
 */
public void traverse(ASTVisitor visitor, ClassScope scope) {
	// nothing to do
}
// return true if this expression can be a stand alone statement when terminated with a semicolon
public boolean statementExpression() {
	return false;
}
// for switch statement
public boolean isTrulyExpression() {
	return true;
}

/**
 * Used on the lhs of an assignment for detecting null spec violation.
 * If this expression represents a null-annotated variable return the variable binding,
 * otherwise null.
 * @param supportTypeAnnotations if true this causes any variable binding to be used
 *   independent of declaration annotations (for in-depth analysis of type annotations)
*/
public VariableBinding nullAnnotatedVariableBinding(boolean supportTypeAnnotations) {
	return null;
}

public boolean isFunctionalType() {
	return false;
}

/** Returns contained poly expressions, result could be 0, 1 or more (for conditional expression) */
public Expression [] getPolyExpressions() {
	return isPolyExpression() ? new Expression [] { this } : NO_EXPRESSIONS;
}

public boolean isPotentiallyCompatibleWith(TypeBinding targetType, Scope scope) {
	return isCompatibleWith(targetType, scope); // for all but functional expressions, potential compatibility is the same as compatibility.
}

protected Constant optimizedNullComparisonConstant() {
	return Constant.NotAConstant;
}
}