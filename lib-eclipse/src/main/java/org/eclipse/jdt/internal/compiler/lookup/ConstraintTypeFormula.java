/*******************************************************************************
 * Copyright (c) 2013, 2018 GK Software AG, and others
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
import java.util.List;

import org.eclipse.jdt.internal.compiler.ast.Invocation;
import org.eclipse.jdt.internal.compiler.ast.Wildcard;

/**
 * Implementation of 18.1.2 in JLS8, cases:
 * <ul>
 * <li>S -> T <em>compatible</em></li>
 * <li>S <: T <em>subtype</em></li>
 * <li>S = T  <em>equality</em></li>
 * <li>S <= T <em>type argument containment</em></li>
 * </ul>
 */
class ConstraintTypeFormula extends ConstraintFormula {

	TypeBinding left;

	// this flag contributes to the workaround controlled by InferenceContext18.ARGUMENT_CONSTRAINTS_ARE_SOFT:
	boolean isSoft;

	public static ConstraintTypeFormula create(TypeBinding exprType, TypeBinding right, int relation) {
		if (exprType == null || right == null)
			return FALSE;
		return new ConstraintTypeFormula(exprType, right, relation, false);
	}

	public static ConstraintTypeFormula create(TypeBinding exprType, TypeBinding right, int relation, boolean isSoft) {
		if (exprType == null || right == null)
			return FALSE;
		return new ConstraintTypeFormula(exprType, right, relation, isSoft);
	}

	// DON'T USE, use factory methods above instead.
	private ConstraintTypeFormula(TypeBinding exprType, TypeBinding right, int relation, boolean isSoft) {
		this.left = exprType;
		this.right = right;
		this.relation = relation;
		this.isSoft = isSoft;
	}

	// for constants TRUE & FALSE, only:
	ConstraintTypeFormula() { }

	// return: ReductionResult or ConstraintFormula[]
	@Override
	public Object reduce(InferenceContext18 inferenceContext) {
		switch (this.relation) {
		case COMPATIBLE:
			// 18.2.2:
			if (this.left.isProperType(true) && this.right.isProperType(true)) {
				return this.left.isCompatibleWith(this.right, inferenceContext.scope) || this.left.isBoxingCompatibleWith(this.right, inferenceContext.scope) ? TRUE : FALSE;
			}
			if (this.left.isPrimitiveType()) {
				TypeBinding sPrime = inferenceContext.environment.computeBoxingType(this.left);
				return ConstraintTypeFormula.create(sPrime, this.right, COMPATIBLE, this.isSoft);
			}
			if (this.right.isPrimitiveType()) {
				TypeBinding tPrime = inferenceContext.environment.computeBoxingType(this.right);
				return ConstraintTypeFormula.create(this.left, tPrime, SAME, this.isSoft);
			}
			switch (this.right.kind()) {
			case Binding.ARRAY_TYPE:
				if (this.right.leafComponentType().kind() != Binding.PARAMETERIZED_TYPE)
					break;
				//$FALL-THROUGH$ array of parameterized is handled below:
			case Binding.PARAMETERIZED_TYPE:
				{
					//															  this.right = G<T1,T2,...> or G<T1,T2,...>[]k
					TypeBinding gs = this.left.findSuperTypeOriginatingFrom(this.right);	// G<S1,S2,...> or G<S1,S2,...>[]k
					if (gs != null && gs.leafComponentType().isRawType()) {
						inferenceContext.recordUncheckedConversion(this);
						return TRUE;
					}
					break;
				}
			}
			return ConstraintTypeFormula.create(this.left, this.right, SUBTYPE, this.isSoft);
		case SUBTYPE:
			// 18.2.3:
			return reduceSubType(inferenceContext.scope, this.left, this.right);
		case SUPERTYPE:
			// 18.2.3:
			return reduceSubType(inferenceContext.scope, this.right, this.left);
		case SAME:
			if (inferenceContext.environment.globalOptions.isAnnotationBasedNullAnalysisEnabled)
				if (!checkIVFreeTVmatch(this.left, this.right))
					checkIVFreeTVmatch(this.right, this.left);
			// 18.2.4:
			return reduceTypeEquality(inferenceContext.object, inferenceContext);
		case TYPE_ARGUMENT_CONTAINED:
			// 18.2.3:
			if (this.right.kind() != Binding.WILDCARD_TYPE) { // "If T is a type" ... all alternatives require "wildcard"
				if (this.left.kind() != Binding.WILDCARD_TYPE) {
					return ConstraintTypeFormula.create(this.left, this.right, SAME, this.isSoft);
				} else {
					// TODO: speculative addition:
					if (this.right instanceof InferenceVariable)
						return new TypeBound((InferenceVariable) this.right, this.left, SAME, this.isSoft);
					return FALSE;
				}
			} else {
				WildcardBinding t = (WildcardBinding) this.right;
				if (t.boundKind == Wildcard.UNBOUND)
					return TRUE;
				if (t.boundKind == Wildcard.EXTENDS) {
					if (this.left.kind() != Binding.WILDCARD_TYPE) {
						return ConstraintTypeFormula.create(this.left, t.bound, SUBTYPE, this.isSoft);
					} else {
						WildcardBinding s = (WildcardBinding) this.left;
						switch (s.boundKind) {
							case Wildcard.UNBOUND:
								return ConstraintTypeFormula.create(inferenceContext.object, t.bound, SUBTYPE, this.isSoft);
							case Wildcard.EXTENDS:
								return ConstraintTypeFormula.create(s.bound, t.bound, SUBTYPE, this.isSoft);
							case Wildcard.SUPER:
								return ConstraintTypeFormula.create(inferenceContext.object, t.bound, SAME, this.isSoft);
							default:
								throw new IllegalArgumentException("Unexpected boundKind "+s.boundKind);  //$NON-NLS-1$
						}
					}
				} else { // SUPER
					if (this.left.kind() != Binding.WILDCARD_TYPE) {
						return ConstraintTypeFormula.create(t.bound, this.left, SUBTYPE, this.isSoft);
					} else {
						WildcardBinding s = (WildcardBinding) this.left;
						if (s.boundKind == Wildcard.SUPER) {
							return ConstraintTypeFormula.create(t.bound, s.bound, SUBTYPE, this.isSoft);
						} else {
							return FALSE;
						}
					}
				}
			}
		default: throw new IllegalStateException("Unexpected relation kind "+this.relation); //$NON-NLS-1$
		}
	}

	/** Detect when we are equating an inference variable against a free type variable. */
	boolean checkIVFreeTVmatch(TypeBinding one, TypeBinding two) {
		if (one instanceof InferenceVariable && two.isTypeVariable() && (two.tagBits & TagBits.AnnotationNullMASK) == 0) {
			// found match => avoid inferring any null annotation (by marking as contradiction):
			((InferenceVariable)one).nullHints = TagBits.AnnotationNullMASK;
			return true;
		}
		return false;
	}

	private Object reduceTypeEquality(TypeBinding object, InferenceContext18 inferenceContext) {
		// 18.2.4
		if (this.left.kind() == Binding.WILDCARD_TYPE) {
			if (this.right.kind() == Binding.WILDCARD_TYPE) {
				// left and right are wildcards ("type arguments")
				WildcardBinding leftWC = (WildcardBinding)this.left;
				WildcardBinding rightWC = (WildcardBinding)this.right;
				if (leftWC.boundKind == Wildcard.UNBOUND && rightWC.boundKind == Wildcard.UNBOUND)
					return TRUE;
				if (leftWC.boundKind == Wildcard.UNBOUND && rightWC.boundKind == Wildcard.EXTENDS)
					return ConstraintTypeFormula.create(object, rightWC.bound, SAME, this.isSoft);
				if (leftWC.boundKind == Wildcard.EXTENDS && rightWC.boundKind == Wildcard.UNBOUND)
					return ConstraintTypeFormula.create(leftWC.bound, object, SAME, this.isSoft);
				if ((leftWC.boundKind == Wildcard.EXTENDS && rightWC.boundKind == Wildcard.EXTENDS)
					||(leftWC.boundKind == Wildcard.SUPER && rightWC.boundKind == Wildcard.SUPER))
				{
					return ConstraintTypeFormula.create(leftWC.bound, rightWC.bound, SAME, this.isSoft);
				}
			}
		} else {
			if (this.right.kind() != Binding.WILDCARD_TYPE) {
				// left and right are types (vs. wildcards)
				if (this.left.isProperType(true) && this.right.isProperType(true)) {
					if (TypeBinding.equalsEquals(this.left, this.right))
						return TRUE;
					return FALSE;
				}
				if (this.left.id == TypeIds.T_null || this.right.id== TypeIds.T_null) {
					return FALSE;
				}
				if (this.left instanceof InferenceVariable && !this.right.isPrimitiveType()) {
					return new TypeBound((InferenceVariable) this.left, this.right, SAME, this.isSoft);
				}
				if (this.right instanceof InferenceVariable && !this.left.isPrimitiveType()) {
					return new TypeBound((InferenceVariable) this.right, this.left, SAME, this.isSoft);
				}
				if ((this.left.isClass() || this.left.isInterface())
						&& (this.right.isClass() || this.right.isInterface())
						&& TypeBinding.equalsEquals(this.left.erasure(), this.right.erasure()))
				{
					TypeBinding[] leftParams = this.left.typeArguments();
					TypeBinding[] rightParams = this.right.typeArguments();
					if (leftParams == null || rightParams == null)
						return leftParams == rightParams ? TRUE : FALSE;
					if (leftParams.length != rightParams.length)
						return FALSE;
					int len = leftParams.length;
					ConstraintFormula[] constraints = new ConstraintFormula[len];
					for (int i = 0; i < len; i++) {
						constraints[i] = ConstraintTypeFormula.create(leftParams[i], rightParams[i], SAME, this.isSoft);
					}
					return constraints;
				}
				if (this.left.isArrayType() && this.right.isArrayType()) {
					if (this.left.dimensions() == this.right.dimensions()) {
						// checking dimensions in one step is an optimization over reducing one dim at a time
						return ConstraintTypeFormula.create(this.left.leafComponentType(), this.right.leafComponentType(), SAME, this.isSoft);
					} else if (this.left.dimensions() > 0 && this.right.dimensions() > 0) {
						TypeBinding leftPrime = peelOneDimension(this.left, inferenceContext.environment);
						TypeBinding rightPrime = peelOneDimension(this.right, inferenceContext.environment);
						return ConstraintTypeFormula.create(leftPrime, rightPrime, SAME, this.isSoft);
					}
				}
			}
		}
		return FALSE;
	}

	private TypeBinding peelOneDimension(TypeBinding arrayType, LookupEnvironment env) {
		if (arrayType.dimensions() == 1)
			return arrayType.leafComponentType();
		return env.createArrayType(arrayType.leafComponentType(), arrayType.dimensions()-1);
	}

	private Object reduceSubType(Scope scope, TypeBinding subCandidate, TypeBinding superCandidate) {
		// 18.2.3 Subtyping Constraints
		if (subCandidate.isProperType(true) && superCandidate.isProperType(true)) {
			if (subCandidate.isSubtypeOf(superCandidate, InferenceContext18.SIMULATE_BUG_JDK_8026527))
				return TRUE;
			return FALSE;
		}
		if (subCandidate.id == TypeIds.T_null)
			return TRUE;
		if (superCandidate.id == TypeIds.T_null)
			return FALSE;
		if (subCandidate instanceof InferenceVariable)
			return new TypeBound((InferenceVariable)subCandidate, superCandidate, SUBTYPE, this.isSoft);
		if (superCandidate instanceof InferenceVariable)
			return new TypeBound((InferenceVariable)superCandidate, subCandidate, SUPERTYPE, this.isSoft); // normalize to have variable on LHS
		switch (superCandidate.kind()) {
			case Binding.GENERIC_TYPE:
			case Binding.TYPE:
			case Binding.RAW_TYPE:
				{
					if (subCandidate.isSubtypeOf(superCandidate, InferenceContext18.SIMULATE_BUG_JDK_8026527))
						return TRUE;
					return FALSE;
				}
			case Binding.PARAMETERIZED_TYPE:
				{
					List<ConstraintFormula> constraints = new ArrayList<>();
					boolean isFirst = true;
					while (superCandidate != null && superCandidate.kind() == Binding.PARAMETERIZED_TYPE && subCandidate != null)  {
						if (!addConstraintsFromTypeParameters(subCandidate, (ParameterizedTypeBinding) superCandidate, constraints))
							if (isFirst) return FALSE;
						isFirst = false;
						// travel to enclosing types to check if they have type parameters, too:
						superCandidate = superCandidate.enclosingType();
						subCandidate = subCandidate.enclosingType();
					}
					switch (constraints.size()) {
						case 0 : return TRUE;
						case 1 : return constraints.get(0);
						default: return constraints.toArray(new ConstraintFormula[constraints.size()]);
					}
				}
			case Binding.ARRAY_TYPE:
				TypeBinding tPrime = ((ArrayBinding)superCandidate).elementsType();
				// let S'[] be the most specific array type that is a supertype of S (or S itself)
				ArrayBinding sPrimeArray = null;
				switch(subCandidate.kind()) {
				case Binding.INTERSECTION_TYPE:
					{
						WildcardBinding intersection = (WildcardBinding) subCandidate;
						sPrimeArray = findMostSpecificSuperArray(intersection.bound, intersection.otherBounds, intersection);
						break;
					}
				case Binding.ARRAY_TYPE:
					sPrimeArray = (ArrayBinding) subCandidate;
					break;
				case Binding.TYPE_PARAMETER:
					{
						TypeVariableBinding subTVB = (TypeVariableBinding)subCandidate;
						sPrimeArray = findMostSpecificSuperArray(subTVB.firstBound, subTVB.otherUpperBounds(), subTVB);
						break;
					}
				default:
					return FALSE;
				}
				if (sPrimeArray == null)
					return FALSE;
				TypeBinding sPrime = sPrimeArray.elementsType();
				if (!tPrime.isPrimitiveType() && !sPrime.isPrimitiveType()) {
					return ConstraintTypeFormula.create(sPrime, tPrime, SUBTYPE, this.isSoft);
				}
				return TypeBinding.equalsEquals(tPrime, sPrime) ? TRUE : FALSE; // same primitive type?

			// "type variable" has two implementations in JDT:
			case Binding.WILDCARD_TYPE:
				if (subCandidate.kind() == Binding.INTERSECTION_TYPE) {
					ReferenceBinding[] intersectingTypes = subCandidate.getIntersectingTypes();
					if (intersectingTypes != null)
						for (int i = 0; i < intersectingTypes.length; i++)
							if (TypeBinding.equalsEquals(intersectingTypes[i], superCandidate))
								return true;
				}
				WildcardBinding variable = (WildcardBinding) superCandidate;
				if (variable.boundKind == Wildcard.SUPER)
					return ConstraintTypeFormula.create(subCandidate, variable.bound, SUBTYPE, this.isSoft);
				return FALSE;
			case Binding.TYPE_PARAMETER:
				// similar to wildcard, but different queries for lower bound
				if (subCandidate.kind() == Binding.INTERSECTION_TYPE) {
					ReferenceBinding[] intersectingTypes = subCandidate.getIntersectingTypes();
					if (intersectingTypes != null)
						for (int i = 0; i < intersectingTypes.length; i++)
							if (TypeBinding.equalsEquals(intersectingTypes[i], superCandidate))
								return true;
				}
				if (superCandidate instanceof CaptureBinding) {
					CaptureBinding capture = (CaptureBinding) superCandidate;
					if (capture.lowerBound != null)
						return ConstraintTypeFormula.create(subCandidate, capture.lowerBound, SUBTYPE, this.isSoft);
				}
				return FALSE;
			case Binding.INTERSECTION_TYPE:
				superCandidate = ((WildcardBinding) superCandidate).allBounds();
				//$FALL-THROUGH$
			case Binding.INTERSECTION_TYPE18:
				TypeBinding[] intersectingTypes = ((IntersectionTypeBinding18) superCandidate).intersectingTypes;
				ConstraintFormula[] result = new ConstraintFormula[intersectingTypes.length];
				for (int i = 0; i < intersectingTypes.length; i++) {
					result[i] = ConstraintTypeFormula.create(subCandidate, intersectingTypes[i], SUBTYPE, this.isSoft);
				}
				return result;
			case Binding.POLY_TYPE:
				PolyTypeBinding poly = (PolyTypeBinding) superCandidate;
				Invocation invocation = (Invocation) poly.expression;
				MethodBinding binding = invocation.binding();
				if (binding == null || !binding.isValidBinding())
					return FALSE;
				TypeBinding returnType = binding.isConstructor() ? binding.declaringClass : binding.returnType;
				return reduceSubType(scope, subCandidate, returnType.capture(scope, invocation.sourceStart(), invocation.sourceEnd()));
		}
		throw new IllegalStateException("Unexpected RHS "+superCandidate); //$NON-NLS-1$
	}

	private ArrayBinding findMostSpecificSuperArray(TypeBinding firstBound, TypeBinding[] otherUpperBounds, TypeBinding theType) {
		int numArrayBounds = 0;
		ArrayBinding result = null;
		if (firstBound != null && firstBound.isArrayType()) {
			result = (ArrayBinding) firstBound;
			numArrayBounds++;
		}
		for (int i = 0; i < otherUpperBounds.length; i++) {
			if (otherUpperBounds[i].isArrayType()) {
				result = (ArrayBinding) otherUpperBounds[i];
				numArrayBounds++;
			}
		}
		if (numArrayBounds == 0)
			return null;
		if (numArrayBounds == 1)
			return result;
		InferenceContext18.missingImplementation("Extracting array from intersection is not defined"); //$NON-NLS-1$
		return null;
	}

	boolean addConstraintsFromTypeParameters(TypeBinding subCandidate, ParameterizedTypeBinding ca, List<ConstraintFormula> constraints) {
		TypeBinding cb = subCandidate.findSuperTypeOriginatingFrom(ca);	// C<B1,B2,...>
		if (cb == null)
			return false; // nothing here means we failed
		if (TypeBinding.equalsEquals(ca, cb)) // incl C#RAW vs C#RAW
			return true;
		if (!(cb instanceof ParameterizedTypeBinding)) {
			// if C is parameterized with its own type variables, there're no more constraints to be created here, otherwise let's fail
			return ca.isParameterizedWithOwnVariables();
		}
		TypeBinding[] bi = ((ParameterizedTypeBinding) cb).arguments;
		TypeBinding[] ai = ca.arguments;								// C<A1,A2,...>
		if (ai == null)
			return true; // no arguments here means nothing to check
		if (cb.isRawType() || bi == null || bi.length == 0)
			return (this.isSoft && InferenceContext18.SIMULATE_BUG_JDK_8026527) ? true : false; // FALSE would conform to the spec
		for (int i = 0; i < ai.length; i++)
			constraints.add(ConstraintTypeFormula.create(bi[i], ai[i], TYPE_ARGUMENT_CONTAINED, this.isSoft));
		return true;
	}

	public boolean equalsEquals (ConstraintTypeFormula that) {
		return (that != null && this.relation == that.relation && this.isSoft == that.isSoft &&
					TypeBinding.equalsEquals(this.left, that.left) && TypeBinding.equalsEquals(this.right, that.right));
	}

	@Override
	public boolean applySubstitution(BoundSet solutionSet, InferenceVariable[] variables) {
		super.applySubstitution(solutionSet, variables);
		for (int i=0; i<variables.length; i++) {
			InferenceVariable variable = variables[i];
			TypeBinding instantiation = solutionSet.getInstantiation(variables[i], null);
			if (instantiation == null)
				return false;
			this.left = this.left.substituteInferenceVariable(variable, instantiation);
		}
		return true;
	}

	// debugging
	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer("Type Constraint:\n"); //$NON-NLS-1$
		buf.append('\t').append(LEFT_ANGLE_BRACKET);
		appendTypeName(buf, this.left);
		buf.append(relationToString(this.relation));
		appendTypeName(buf, this.right);
		buf.append(RIGHT_ANGLE_BRACKET);
		return buf.toString();
	}
}
