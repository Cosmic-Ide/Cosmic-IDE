/*******************************************************************************
 * Copyright (c) 2013, 2020 GK Software AG and others.
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
 *     Till Brychcy - Contributions for
 *                              Bug 467482 - TYPE_USE null annotations: Incorrect "Redundant null check"-warning
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.ast;

import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.internal.compiler.flow.FlowContext;
import org.eclipse.jdt.internal.compiler.flow.FlowInfo;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.lookup.AnnotationBinding;
import org.eclipse.jdt.internal.compiler.lookup.ArrayBinding;
import org.eclipse.jdt.internal.compiler.lookup.BinaryTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.BinaryTypeBinding.ExternalAnnotationStatus;
import org.eclipse.jdt.internal.compiler.lookup.Binding;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.CaptureBinding;
import org.eclipse.jdt.internal.compiler.lookup.InvocationSite;
import org.eclipse.jdt.internal.compiler.lookup.LookupEnvironment;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.ParameterizedTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.ProblemMethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.ProblemReasons;
import org.eclipse.jdt.internal.compiler.lookup.RawTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.Scope;
import org.eclipse.jdt.internal.compiler.lookup.Substitution;
import org.eclipse.jdt.internal.compiler.lookup.TagBits;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeBindingVisitor;
import org.eclipse.jdt.internal.compiler.lookup.TypeIds;
import org.eclipse.jdt.internal.compiler.lookup.TypeVariableBinding;
import org.eclipse.jdt.internal.compiler.lookup.VariableBinding;
import org.eclipse.jdt.internal.compiler.lookup.WildcardBinding;
import org.eclipse.jdt.internal.compiler.problem.ProblemSeverities;

/**
 * Performs matching of null type annotations.
 * Instances are used to encode result from this analysis.
 * @since 3.10
 */
public class NullAnnotationMatching {

	public static final NullAnnotationMatching NULL_ANNOTATIONS_OK = new NullAnnotationMatching(Severity.OK, FlowInfo.UNKNOWN, null);
	public static final NullAnnotationMatching NULL_ANNOTATIONS_OK_NONNULL = new NullAnnotationMatching(Severity.OK, FlowInfo.NON_NULL, null);
	public static final NullAnnotationMatching NULL_ANNOTATIONS_UNCHECKED = new NullAnnotationMatching(Severity.UNCHECKED, FlowInfo.UNKNOWN, null);
	public static final NullAnnotationMatching NULL_ANNOTATIONS_MISMATCH = new NullAnnotationMatching(Severity.MISMATCH, FlowInfo.UNKNOWN, null);

	public enum CheckMode {
		/** in this mode we check normal assignment compatibility. */
		COMPATIBLE {
			@Override boolean requiredNullableMatchesAll() {
				return true;
			}
		},
		/** in this mode we check similar to isTypeArgumentContained. */
		EXACT,
		/** in this mode we check compatibility of a type argument against the corresponding type parameter. */
		BOUND_CHECK,
		/** similar to COMPATIBLE, but for type variables we look for instantiations, rather than treating them as "free type variables". */
		BOUND_SUPER_CHECK,
		/** allow covariant return types, but no other deviations. */
		OVERRIDE_RETURN {
			@Override CheckMode toDetail() {
				return OVERRIDE;
			}
		},
		/** in this mode we do not tolerate incompatibly missing annotations on type parameters (for overriding analysis) */
		OVERRIDE {
			@Override boolean requiredNullableMatchesAll() {
				return true;
			}
			@Override CheckMode toDetail() {
				return OVERRIDE;
			}
		};

		boolean requiredNullableMatchesAll() {
			return false;
		}
		CheckMode toDetail() {
			return CheckMode.EXACT;
		}
	}

	private enum Severity {
		/** No problem detected. */
		OK,
		/** No real problem, but could issue an {@link IProblem#NonNullTypeVariableFromLegacyMethod} or similar. */
		LEGACY_WARNING,
		/** Need unchecked conversion from unannotated to annotated. */
		UNCHECKED,
		/** Need unchecked conversion to pass type with annotated type arguments into unannotated code. */
		UNCHECKED_TO_UNANNOTATED,
		/** Definite nullity mismatch. */
		MISMATCH;

		public Severity max(Severity severity) {
			if (compareTo(severity) < 0)
				return severity;
			return this;
		}

		public boolean isAnyMismatch() {
			return compareTo(LEGACY_WARNING) > 0;
		}
	}

	private final Severity severity;

	/** If non-null this field holds the supertype of the provided type which was used for direct matching. */
	public final TypeBinding superTypeHint;
	public final int nullStatus;

	NullAnnotationMatching(Severity severity, int nullStatus, TypeBinding superTypeHint) {
		this.severity = severity;
		this.superTypeHint = superTypeHint;
		this.nullStatus = nullStatus;
	}

	/**
 	 * For creating updated status during *FlowContext.complainOnDeferred*Checks() once the actual nullStatus is known
	 */
	public NullAnnotationMatching withNullStatus(int updatedNullStatus) {
		return updatedNullStatus == this.nullStatus ? this
				: new NullAnnotationMatching(this.severity, updatedNullStatus, this.superTypeHint);
	}

	public boolean isAnyMismatch()      		{ return this.severity.isAnyMismatch(); }
	public boolean isUnchecked()        		{ return this.severity == Severity.UNCHECKED || this.severity == Severity.UNCHECKED_TO_UNANNOTATED; }
	public boolean isAnnotatedToUnannotated() 	{ return this.severity == Severity.UNCHECKED_TO_UNANNOTATED; }
	public boolean isDefiniteMismatch() 		{ return this.severity == Severity.MISMATCH; }
	public boolean wantToReport() 				{ return this.severity == Severity.LEGACY_WARNING; }

	public boolean isPotentiallyNullMismatch() {
		return !isDefiniteMismatch() && this.nullStatus != -1 && (this.nullStatus & FlowInfo.POTENTIALLY_NULL) != 0;
	}

	public String superTypeHintName(CompilerOptions options, boolean shortNames) {
		return String.valueOf(this.superTypeHint.nullAnnotatedReadableName(options, shortNames));
	}

	/** Check null-ness of 'var' against a possible null annotation */
	public static int checkAssignment(BlockScope currentScope, FlowContext flowContext,
									   VariableBinding var, FlowInfo flowInfo, int nullStatus, Expression expression, TypeBinding providedType)
	{
		if (providedType == null) return FlowInfo.UNKNOWN; // assume we already reported an error
		long lhsTagBits = 0L;
		boolean hasReported = false;
		boolean usesNullTypeAnnotations = currentScope.environment().usesNullTypeAnnotations();
		if (!usesNullTypeAnnotations) {
			lhsTagBits = var.tagBits & TagBits.AnnotationNullMASK;
		} else {
			if (expression instanceof ConditionalExpression && expression.isPolyExpression()) {
				// drill into both branches:
				ConditionalExpression ce = ((ConditionalExpression) expression);
				int status1 = NullAnnotationMatching.checkAssignment(currentScope, flowContext, var, flowInfo, ce.ifTrueNullStatus, ce.valueIfTrue, ce.valueIfTrue.resolvedType);
				int status2 = NullAnnotationMatching.checkAssignment(currentScope, flowContext, var, flowInfo, ce.ifFalseNullStatus, ce.valueIfFalse, ce.valueIfFalse.resolvedType);
				if (status1 == status2)
					return status1;
				return nullStatus; // if both branches disagree use the precomputed & merged nullStatus
			} else if (expression instanceof SwitchExpression && expression.isPolyExpression()) {
				// drill into all the branches:
				SwitchExpression se = ((SwitchExpression) expression);
				Expression[] resExprs = se.resultExpressions.toArray(new Expression[0]);
				Expression re = resExprs[0];
				int status0 = NullAnnotationMatching.checkAssignment(currentScope, flowContext, var, flowInfo, re.nullStatus(flowInfo, flowContext), re, re.resolvedType);
				boolean identicalStatus = true;
				for (int i = 1, l = resExprs.length; i < l; ++i) {
					re = resExprs[i];
					int otherStatus = NullAnnotationMatching.checkAssignment(currentScope, flowContext, var, flowInfo,  re.nullStatus(flowInfo, flowContext), re, re.resolvedType);
					identicalStatus &= status0 == otherStatus;
				}
				return identicalStatus ? status0 : nullStatus; // if not all branches agree use the precomputed & merged nullStatus
			}
			lhsTagBits = var.type.tagBits & TagBits.AnnotationNullMASK;
			NullAnnotationMatching annotationStatus = analyse(var.type, providedType, null, null, nullStatus, expression, CheckMode.COMPATIBLE);
			if (annotationStatus.isAnyMismatch()) {
				flowContext.recordNullityMismatch(currentScope, expression, providedType, var.type, flowInfo, nullStatus, annotationStatus);
				hasReported = true;
			} else {
				if (annotationStatus.wantToReport())
					annotationStatus.report(currentScope);
				if (annotationStatus.nullStatus != FlowInfo.UNKNOWN) {
					return annotationStatus.nullStatus;
				}
			}
		}
		if (lhsTagBits == TagBits.AnnotationNonNull && nullStatus != FlowInfo.NON_NULL) {
			if (!hasReported)
				flowContext.recordNullityMismatch(currentScope, expression, providedType, var.type, flowInfo, nullStatus, null);
			return FlowInfo.NON_NULL;
		} else if (lhsTagBits == TagBits.AnnotationNullable && nullStatus == FlowInfo.UNKNOWN) {	// provided a legacy type?
			if (usesNullTypeAnnotations && providedType.isTypeVariable() && (providedType.tagBits & TagBits.AnnotationNullMASK) == 0)
				return FlowInfo.POTENTIALLY_NULL | FlowInfo.POTENTIALLY_NON_NULL;		// -> free type variable can mean either nullable or nonnull
			return FlowInfo.POTENTIALLY_NULL | FlowInfo.POTENTIALLY_UNKNOWN;			// -> combine info from lhs & rhs
		}
		return nullStatus;
	}

	/**
	 * Find any mismatches between the two given types, which are caused by null type annotations.
	 * @param requiredType
	 * @param providedType
	 * @param nullStatus we are only interested in NULL or NON_NULL, -1 indicates that we are in a recursion, where flow info is ignored
	 * @return a status object representing the severity of mismatching plus optionally a supertype hint
	 */
	public static NullAnnotationMatching analyse(TypeBinding requiredType, TypeBinding providedType, int nullStatus) {
		return analyse(requiredType, providedType, null, null, nullStatus, null, CheckMode.COMPATIBLE);
	}
	/**
	 * Find any mismatches between the two given types, which are caused by null type annotations.
	 * @param requiredType
	 * @param providedType
	 * @param providedSubstitute in inheritance situations this maps the providedType into the realm of the subclass, needed for TVB identity checks.
	 * 		Pass null if not interested in these added checks.
	 * @param substitution TODO
	 * @param nullStatus we are only interested in NULL or NON_NULL, -1 indicates that we are in a recursion, where flow info is ignored
	 * @param providedExpression optionally holds the provided expression of type 'providedType'
	 * @param mode controls the kind of check performed (see {@link CheckMode}).
	 * @return a status object representing the severity of mismatching plus optionally a supertype hint
	 */
	public static NullAnnotationMatching analyse(TypeBinding requiredType, TypeBinding providedType, TypeBinding providedSubstitute, Substitution substitution,
			int nullStatus, Expression providedExpression, CheckMode mode)
	{
		if (!requiredType.enterRecursiveFunction())
			return NullAnnotationMatching.NULL_ANNOTATIONS_OK;
		try {
			Severity severity = Severity.OK;
			TypeBinding superTypeHint = null;
			TypeBinding originalRequiredType = requiredType;
			NullAnnotationMatching okStatus = NullAnnotationMatching.NULL_ANNOTATIONS_OK;
			if (areSameTypes(requiredType, providedType, providedSubstitute)) {
				if ((requiredType.tagBits & TagBits.AnnotationNonNull) != 0)
					return okNonNullStatus(providedExpression);
				return okStatus;
			}
			if (requiredType instanceof TypeVariableBinding && substitution != null && (mode == CheckMode.EXACT || mode == CheckMode.COMPATIBLE || mode == CheckMode.BOUND_SUPER_CHECK)) {
				requiredType.exitRecursiveFunction();
				requiredType = Scope.substitute(substitution, requiredType);
				if (!requiredType.enterRecursiveFunction())
					return NullAnnotationMatching.NULL_ANNOTATIONS_OK;
				if (areSameTypes(requiredType, providedType, providedSubstitute)) {
					if ((requiredType.tagBits & TagBits.AnnotationNonNull) != 0)
						return okNonNullStatus(providedExpression);
					return okStatus;
				}
			}
			if (mode == CheckMode.BOUND_CHECK && requiredType instanceof TypeVariableBinding) {
				boolean passedBoundCheck = (substitution instanceof ParameterizedTypeBinding) && (((ParameterizedTypeBinding) substitution).tagBits & TagBits.PassedBoundCheck) != 0;
				if (!passedBoundCheck) {
					// during bound check against a type variable check the provided type against all upper bounds:
					TypeBinding superClass = requiredType.superclass();
					if (superClass != null && (superClass.hasNullTypeAnnotations() || substitution != null)) { // annotations may enter when substituting a nested type variable
						NullAnnotationMatching status = analyse(superClass, providedType, null, substitution, nullStatus, providedExpression, CheckMode.BOUND_SUPER_CHECK);
						severity = severity.max(status.severity);
						if (severity == Severity.MISMATCH)
							return new NullAnnotationMatching(severity, nullStatus, superTypeHint);
					}
					TypeBinding[] superInterfaces = requiredType.superInterfaces();
					if (superInterfaces != null) {
						for (int i = 0; i < superInterfaces.length; i++) {
							if (superInterfaces[i].hasNullTypeAnnotations() || substitution != null) { // annotations may enter when substituting a nested type variable
								NullAnnotationMatching status = analyse(superInterfaces[i], providedType, null, substitution, nullStatus, providedExpression, CheckMode.BOUND_SUPER_CHECK);
								severity = severity.max(status.severity);
								if (severity == Severity.MISMATCH)
									return new NullAnnotationMatching(severity, nullStatus, superTypeHint);
							}
						}
					}
				}
			}
			if (requiredType instanceof ArrayBinding) {
				long[] requiredDimsTagBits = ((ArrayBinding)requiredType).nullTagBitsPerDimension;
				if (requiredDimsTagBits != null) {
					int dims = requiredType.dimensions();
					if (requiredType.dimensions() == providedType.dimensions()) {
						long[] providedDimsTagBits = ((ArrayBinding)providedType).nullTagBitsPerDimension;
						if (providedDimsTagBits == null)
							providedDimsTagBits = new long[dims+1]; // set to unspec'd at all dimensions
						int currentNullStatus = nullStatus;
						for (int i=0; i<=dims; i++) {
							long requiredBits = validNullTagBits(requiredDimsTagBits[i]);
							long providedBits = validNullTagBits(providedDimsTagBits[i]);
							if (i == 0 && requiredBits == TagBits.AnnotationNullable && nullStatus != -1 && mode.requiredNullableMatchesAll()) {
								// toplevel nullable array: no need to check
								if (nullStatus == FlowInfo.NULL)
									break; // null value has no details
							} else {
								if (i > 0)
									currentNullStatus = -1; // don't use beyond the outermost dimension
								Severity dimSeverity = computeNullProblemSeverity(requiredBits, providedBits, currentNullStatus, i == 0 ? mode : mode.toDetail(), null);
								if (i > 0 && dimSeverity == Severity.UNCHECKED
										&& providedExpression instanceof ArrayAllocationExpression
										&& providedBits == 0 && requiredBits != 0)
								{
									Expression[] dimensions = ((ArrayAllocationExpression) providedExpression).dimensions;
									Expression previousDim = dimensions[i-1];
									if (previousDim instanceof IntLiteral && previousDim.constant.intValue() == 0) {
										dimSeverity = Severity.OK; // element of empty dimension matches anything
										nullStatus = -1;
										break;
									}
								}
								severity = severity.max(dimSeverity);
								if (severity == Severity.MISMATCH) {
									if (nullStatus == FlowInfo.NULL)
										return new NullAnnotationMatching(severity, nullStatus, null);
									return NullAnnotationMatching.NULL_ANNOTATIONS_MISMATCH;
								}
							}
							if (severity == Severity.OK)
								nullStatus = -1;
						}
					} else if (providedType.id == TypeIds.T_null) {
						if (dims > 0 && requiredDimsTagBits[0] == TagBits.AnnotationNonNull)
							return NullAnnotationMatching.NULL_ANNOTATIONS_MISMATCH;
					}
				}
			} else if (requiredType.hasNullTypeAnnotations() || providedType.hasNullTypeAnnotations() || requiredType.isTypeVariable()) {
				long requiredBits = requiredNullTagBits(requiredType, mode);
				if (requiredBits == TagBits.AnnotationNullable && nullStatus != -1 && mode.requiredNullableMatchesAll()) {
					// at toplevel (having a nullStatus) nullable matches all
				} else {
					long providedBits = providedNullTagBits(providedType);
					Severity s = computeNullProblemSeverity(requiredBits, providedBits, nullStatus, mode, originalRequiredType);
					if (s.isAnyMismatch() && requiredType.isWildcard() && requiredBits != 0) {
						if (((WildcardBinding) requiredType).determineNullBitsFromDeclaration(null, null) == 0) {
							TypeVariableBinding typeVariable = ((WildcardBinding) requiredType).typeVariable();
							if ((typeVariable.tagBits & TagBits.AnnotationNullMASK) != 0) {
								// wildcard has its nullBits from the type variable
								s = Severity.OK; // is already reported as illegal substitution
							}
						}
					}
					severity = severity.max(s);
					if (!severity.isAnyMismatch() && (providedBits & TagBits.AnnotationNullMASK) == TagBits.AnnotationNonNull)
						okStatus = okNonNullStatus(providedExpression);
				}
				if (severity != Severity.MISMATCH && nullStatus != FlowInfo.NULL) {  // null value has no details
					TypeBinding providedSuper = providedType.findSuperTypeOriginatingFrom(requiredType);
					TypeBinding providedSubstituteSuper = providedSubstitute != null ? providedSubstitute.findSuperTypeOriginatingFrom(requiredType) : null;
					if (severity == Severity.UNCHECKED && requiredType.isTypeVariable() && providedType.isTypeVariable() && (providedSuper == requiredType || providedSubstituteSuper == requiredType)) { //$IDENTITY-COMPARISON$
						severity = Severity.OK;
					}
					if (providedSuper != providedType) //$IDENTITY-COMPARISON$
						superTypeHint = providedSuper;
					if (requiredType.isParameterizedType()  && providedSuper instanceof ParameterizedTypeBinding) { // TODO(stephan): handle providedType.isRaw()
						TypeBinding[] requiredArguments = ((ParameterizedTypeBinding) requiredType).arguments;
						TypeBinding[] providedArguments = ((ParameterizedTypeBinding) providedSuper).arguments;
						TypeBinding[] providedSubstitutes = (providedSubstituteSuper instanceof ParameterizedTypeBinding) ? ((ParameterizedTypeBinding)providedSubstituteSuper).arguments : null;
						if (requiredArguments != null && providedArguments != null && requiredArguments.length == providedArguments.length) {
							for (int i = 0; i < requiredArguments.length; i++) {
								TypeBinding providedArgSubstitute = providedSubstitutes != null ? providedSubstitutes[i] : null;
								NullAnnotationMatching status = analyse(requiredArguments[i], providedArguments[i], providedArgSubstitute, substitution, -1, providedExpression, mode.toDetail());
								severity = severity.max(status.severity);
								if (severity == Severity.MISMATCH)
									return new NullAnnotationMatching(severity, nullStatus, superTypeHint);
							}
						}
					}
					TypeBinding requiredEnclosing = requiredType.enclosingType();
					TypeBinding providedEnclosing = providedType.enclosingType();
					if (requiredEnclosing != null && providedEnclosing != null) {
						TypeBinding providedEnclSubstitute = providedSubstitute != null ? providedSubstitute.enclosingType() : null;
						NullAnnotationMatching status = analyse(requiredEnclosing, providedEnclosing, providedEnclSubstitute, substitution, -1, providedExpression, mode);
						severity = severity.max(status.severity);
					}
				}
			}
			if (!severity.isAnyMismatch())
				return okStatus;
			return new NullAnnotationMatching(severity, nullStatus, superTypeHint);
		} finally {
			requiredType.exitRecursiveFunction();
		}
	}
	public void report(Scope scope) {
		// nop
	}
	public static NullAnnotationMatching okNonNullStatus(final Expression providedExpression) {
		if (providedExpression instanceof MessageSend) {
			final MethodBinding method = ((MessageSend) providedExpression).binding;
			if (method != null && method.isValidBinding()) {
				MethodBinding originalMethod = method.original();
				TypeBinding originalDeclaringClass = originalMethod.declaringClass;
				if (originalDeclaringClass instanceof BinaryTypeBinding
						&& ((BinaryTypeBinding) originalDeclaringClass).externalAnnotationStatus.isPotentiallyUnannotatedLib()
						&& originalMethod.returnType.isTypeVariable()
						&& (originalMethod.returnType.tagBits & TagBits.AnnotationNullMASK) == 0)
				{
					final int severity = ((BinaryTypeBinding) originalDeclaringClass).externalAnnotationStatus == ExternalAnnotationStatus.NO_EEA_FILE
												? ProblemSeverities.Warning : ProblemSeverities.Info; // reduce severity if not configured to for external annotations
					return new NullAnnotationMatching(Severity.LEGACY_WARNING, FlowInfo.UNKNOWN, null) {
						@Override
						public void report(Scope scope) {
							scope.problemReporter().nonNullTypeVariableInUnannotatedBinary(scope.environment(), method, providedExpression, severity);
						}
					};
				}
			}
		}
		return NullAnnotationMatching.NULL_ANNOTATIONS_OK_NONNULL;
	}

	/** Are both types identical wrt the unannotated type and any null type annotations? Only unstructured types and captures are considered. */
	protected static boolean areSameTypes(TypeBinding requiredType, TypeBinding providedType, TypeBinding providedSubstitute) {
		if (requiredType == providedType)  //$IDENTITY-COMPARISON$ // short cut for really-really-same types
			return true;
		if (requiredType.isParameterizedType() || requiredType.isArrayType())
			return false; // not analysing details here
		if (TypeBinding.notEquals(requiredType, providedType)) {
			if (requiredType instanceof CaptureBinding) {
				// when providing exactly the lower bound of the required type we're definitely fine:
				TypeBinding lowerBound = ((CaptureBinding)requiredType).lowerBound;
				if (lowerBound != null && areSameTypes(lowerBound, providedType, providedSubstitute))
					return (requiredType.tagBits & TagBits.AnnotationNullMASK) == (providedType.tagBits & TagBits.AnnotationNullMASK);
			} else if (requiredType.kind() == Binding.TYPE_PARAMETER && requiredType == providedSubstitute) { //$IDENTITY-COMPARISON$
				return true;
			} else if (providedType instanceof CaptureBinding) {
				// when requiring exactly the upper bound of the provided type we're fine, too:
				TypeBinding upperBound = ((CaptureBinding)providedType).upperBound();
				if (upperBound != null && areSameTypes(requiredType, upperBound, providedSubstitute))
					return (requiredType.tagBits & TagBits.AnnotationNullMASK) == (providedType.tagBits & TagBits.AnnotationNullMASK);
			}
			return false;
		}
		return (requiredType.tagBits & TagBits.AnnotationNullMASK) == (providedType.tagBits & TagBits.AnnotationNullMASK);
	}

	// interpreting 'type' as a required type, compute the required null bits
	// we inspect the main type plus bounds of type variables and wildcards
	static long requiredNullTagBits(TypeBinding type, CheckMode mode) {

		long tagBits = type.tagBits & TagBits.AnnotationNullMASK;
		if (tagBits != 0)
			return validNullTagBits(tagBits);

		if (type.isWildcard()) {
			WildcardBinding wildcardBinding = (WildcardBinding) type;
			TypeBinding bound = wildcardBinding.bound;
			tagBits = bound != null ? bound.tagBits & TagBits.AnnotationNullMASK : 0;
			switch (wildcardBinding.boundKind) {
				case Wildcard.SUPER:
					if (tagBits == TagBits.AnnotationNullable)
						return TagBits.AnnotationNullable; // type cannot require @NonNull
					break;
				case Wildcard.EXTENDS:
					if (tagBits == TagBits.AnnotationNonNull)
						return tagBits;
					break;
			}
			return TagBits.AnnotationNullMASK;
		}

		if (type.isTypeVariable()) {
			// assume we must require @NonNull, unless lower @Nullable bound
			// (annotation directly on the TV has already been checked above)
			if (type.isCapture()) {
				TypeBinding lowerBound = ((CaptureBinding) type).lowerBound;
				if (lowerBound != null) {
					tagBits = lowerBound.tagBits & TagBits.AnnotationNullMASK;
					if (tagBits == TagBits.AnnotationNullable)
						return TagBits.AnnotationNullable; // type cannot require @NonNull
				}
			}
			switch (mode) {
				case BOUND_CHECK: // no pessimistic checks during boundcheck (we *have* the instantiation)
				case BOUND_SUPER_CHECK:
				case OVERRIDE: 	  // no pessimistic checks during override check (comparing two *declarations*)
				case OVERRIDE_RETURN:
					break;
				default:
					return TagBits.AnnotationNonNull; // instantiation could require @NonNull
			}
		}

		return 0;
	}

	// interpreting 'type' as a provided type, compute the provide null bits
	// we inspect the main type plus bounds of type variables and wildcards
	static long providedNullTagBits(TypeBinding type) {

		long tagBits = type.tagBits & TagBits.AnnotationNullMASK;
		if (tagBits != 0)
			return validNullTagBits(tagBits);

		if (type.isWildcard()) { // wildcard can be 'provided' during inheritance checks
			return TagBits.AnnotationNullMASK;
		}

		if (type.isTypeVariable()) { // incl. captures
			TypeVariableBinding typeVariable = (TypeVariableBinding)type;
			boolean haveNullBits = false;
			if (typeVariable.isCapture()) {
				TypeBinding lowerBound = ((CaptureBinding) typeVariable).lowerBound;
				if (lowerBound != null) {
					tagBits = lowerBound.tagBits & TagBits.AnnotationNullMASK;
					if (tagBits == TagBits.AnnotationNullable)
						return TagBits.AnnotationNullable; // cannot be @NonNull
					haveNullBits |= (tagBits != 0);
				}
			}
			if (typeVariable.firstBound != null) {
				long boundBits = typeVariable.firstBound.tagBits & TagBits.AnnotationNullMASK;
				if (boundBits == TagBits.AnnotationNonNull)
					return TagBits.AnnotationNonNull; // cannot be @Nullable
				haveNullBits |= (boundBits != 0);
			}
			if (haveNullBits)
				return TagBits.AnnotationNullMASK; // could be either, can only match to a wildcard accepting both
		}

		return 0;
	}

	/**
	 * Use only if no suitable flowInfo is available.
	 */
	public static int nullStatusFromExpressionType(TypeBinding type) {
		if (type.isFreeTypeVariable())
			return FlowInfo.FREE_TYPEVARIABLE;
		long bits = type.tagBits & TagBits.AnnotationNullMASK;
		if (bits == 0)
			return FlowInfo.UNKNOWN;
		if (bits == TagBits.AnnotationNonNull)
			return FlowInfo.NON_NULL;
		return FlowInfo.POTENTIALLY_NON_NULL | FlowInfo.POTENTIALLY_NULL;
	}

	public static long validNullTagBits(long bits) {
		bits &= TagBits.AnnotationNullMASK;
		return bits == TagBits.AnnotationNullMASK ? 0 : bits;
	}

	/** Provided that both types are {@link TypeBinding#equalsEquals}, return the one that is more likely to show null at runtime. */
	public static TypeBinding moreDangerousType(TypeBinding one, TypeBinding two) {
		if (one == null) return null;
		long oneNullBits = validNullTagBits(one.tagBits);
		long twoNullBits = validNullTagBits(two.tagBits);
		if (oneNullBits != twoNullBits) {
			if (oneNullBits == TagBits.AnnotationNullable)
				return one;			// nullable is dangerous
			if (twoNullBits == TagBits.AnnotationNullable)
				return two;			// nullable is dangerous
			// below this point we have unknown vs. nonnull, which is which?
			if (oneNullBits == 0)
				return one;			// unknown is more dangerous than nonnull
			return two;				// unknown is more dangerous than nonnull
		} else if (one != two) { //$IDENTITY-COMPARISON$
			if (analyse(one, two, -1).isAnyMismatch())
				return two;			// two doesn't snugly fit into one, so it must be more dangerous
		}
		return one;
	}

	/**
	 * Evaluate problem severity from the given details:
	 * @param requiredBits null tagBits of the required type
	 * @param providedBits null tagBits of the provided type
	 * @param nullStatus -1 means: don't use, other values see constants in FlowInfo
	 * @param mode check mode (see {@link CheckMode})
	 * @param requiredType the required type, used, e.g., to check if it is a type variable (possibly: "free type variable")?
	 * @return see {@link #severity} for interpretation of values
	 */
	private static Severity computeNullProblemSeverity(long requiredBits, long providedBits, int nullStatus, CheckMode mode, TypeBinding requiredType) {
		if (requiredBits == providedBits)
			return Severity.OK;
		if (requiredBits == 0) {
			switch (mode) {
				case EXACT:
					if (providedBits == TagBits.AnnotationNonNull && !(requiredType instanceof TypeVariableBinding))
						return Severity.UNCHECKED_TO_UNANNOTATED;
					return Severity.OK;
				case COMPATIBLE:
				case BOUND_CHECK:
				case BOUND_SUPER_CHECK:
					return Severity.OK;
				case OVERRIDE_RETURN:
					if (providedBits == TagBits.AnnotationNonNull)
						return Severity.OK; // covariant redefinition to nonnull is good
					if (!(requiredType instanceof TypeVariableBinding))
						return Severity.OK; // refining an unconstrained non-TVB return to nullable is also legal
					return Severity.UNCHECKED;
				case OVERRIDE:
					return Severity.UNCHECKED; // warn about dropped annotation
			}
		} else if (requiredBits == TagBits.AnnotationNullMASK) {
			if (mode == CheckMode.EXACT && providedBits == TagBits.AnnotationNonNull) {
				if (requiredType instanceof WildcardBinding) {
					WildcardBinding wildcard = (WildcardBinding) requiredType;
					// passing '@NonNull X' into '? super Y' risks pollution with null
					if (wildcard.boundKind == Wildcard.SUPER && providedBits == TagBits.AnnotationNonNull) {
						TypeBinding bound = wildcard.bound;
						if (bound != null && (bound.tagBits & TagBits.AnnotationNullMASK) != 0)
							return Severity.OK; // when the wildcard is annotated via its bound, there is not annotated->unannotated conversion
						return Severity.UNCHECKED_TO_UNANNOTATED;
					}
				}
			}
			return Severity.OK; // OK since LHS accepts either
		} else if (requiredBits == TagBits.AnnotationNonNull) {
			switch (mode) {
				case COMPATIBLE:
					if (nullStatus == FlowInfo.NULL)
						return Severity.MISMATCH; // NOK by flow analysis
					//$FALL-THROUGH$
				case BOUND_SUPER_CHECK:
					if (nullStatus == FlowInfo.NON_NULL)
						return Severity.OK; // OK by flow analysis
					//$FALL-THROUGH$
				case BOUND_CHECK:
				case EXACT:
				case OVERRIDE_RETURN:
				case OVERRIDE:
					if (providedBits == 0)
						return Severity.UNCHECKED;
					return Severity.MISMATCH;
			}

		} else if (requiredBits == TagBits.AnnotationNullable) {
			switch (mode) {
				case COMPATIBLE:
				case OVERRIDE_RETURN:
				case BOUND_SUPER_CHECK:
					return Severity.OK; // in these modes everything is compatible to nullable
				case BOUND_CHECK:
				case EXACT:
					if (providedBits == 0)
						return Severity.UNCHECKED;
					return Severity.MISMATCH;
				case OVERRIDE:
					return Severity.MISMATCH;
			}
		}
		return Severity.OK; // shouldn't get here, requiredBits should be one of the listed cases
	}

	static class SearchContradictions extends TypeBindingVisitor {
		ReferenceBinding typeWithContradiction;
		@Override
		public boolean visit(ReferenceBinding referenceBinding) {
			if ((referenceBinding.tagBits & TagBits.AnnotationNullMASK) == TagBits.AnnotationNullMASK) {
				this.typeWithContradiction = referenceBinding;
				return false;
			}
			return true;
		}
		@Override
		public boolean visit(TypeVariableBinding typeVariable) {
			if (!visit((ReferenceBinding)typeVariable))
				return false;
			long allNullBits = typeVariable.tagBits & TagBits.AnnotationNullMASK;
			if (typeVariable.firstBound != null)
				allNullBits = typeVariable.firstBound.tagBits & TagBits.AnnotationNullMASK;
			for (TypeBinding otherBound : typeVariable.otherUpperBounds())
				allNullBits |= otherBound.tagBits & TagBits.AnnotationNullMASK;
			if (allNullBits == TagBits.AnnotationNullMASK) {
				this.typeWithContradiction = typeVariable;
				return false;
			}
			return true;
		}
		@Override
		public boolean visit(RawTypeBinding rawType) {
			return visit((ReferenceBinding)rawType);
		}
		@Override
		public boolean visit(WildcardBinding wildcardBinding) {
			long allNullBits = wildcardBinding.tagBits & TagBits.AnnotationNullMASK;
			switch (wildcardBinding.boundKind) {
				case Wildcard.EXTENDS:
					allNullBits |= wildcardBinding.bound.tagBits & TagBits.AnnotationNonNull;
					break;
				case Wildcard.SUPER:
					allNullBits |= wildcardBinding.bound.tagBits & TagBits.AnnotationNullable;
					break;
			}
			if (allNullBits == TagBits.AnnotationNullMASK) {
				this.typeWithContradiction = wildcardBinding;
				return false;
			}
			return true;
		}
		@Override
		public boolean visit(ParameterizedTypeBinding parameterizedTypeBinding) {
			if (!visit((ReferenceBinding) parameterizedTypeBinding))
				return false;
			return super.visit(parameterizedTypeBinding);
		}
	}

	/**
	 * After a method has substituted type parameters, check if this resulted in any contradictory null annotations.
	 * Problems are either reported directly (if scope != null) or by returning a ProblemMethodBinding.
	 */
	public static MethodBinding checkForContradictions(MethodBinding method, Object location, Scope scope) {

		int start = 0, end = 0;
		if (location instanceof InvocationSite) {
			start = ((InvocationSite) location).sourceStart();
			end = ((InvocationSite) location).sourceEnd();
		} else if (location instanceof ASTNode) {
			start = ((ASTNode) location).sourceStart;
			end = ((ASTNode) location).sourceEnd;
		}
		SearchContradictions searchContradiction = new SearchContradictions();
		TypeBindingVisitor.visit(searchContradiction, method.returnType);
		if (searchContradiction.typeWithContradiction != null) {
			if (scope == null)
				return new ProblemMethodBinding(method, method.selector, method.parameters, ProblemReasons.ContradictoryNullAnnotations);
			scope.problemReporter().contradictoryNullAnnotationsInferred(method, start, end, location instanceof FunctionalExpression);
			// note: if needed, we might want to update the method by removing the contradictory annotations??
			return method;
		}

		Expression[] arguments = null;
		if (location instanceof Invocation)
			arguments = ((Invocation)location).arguments();
		for (int i = 0; i < method.parameters.length; i++) {
			TypeBindingVisitor.visit(searchContradiction, method.parameters[i]);
			if (searchContradiction.typeWithContradiction != null) {
				if (scope == null)
					return new ProblemMethodBinding(method, method.selector, method.parameters, ProblemReasons.ContradictoryNullAnnotations);
				if (arguments != null && i < arguments.length)
					scope.problemReporter().contradictoryNullAnnotationsInferred(method, arguments[i]);
				else
					scope.problemReporter().contradictoryNullAnnotationsInferred(method, start, end, location instanceof FunctionalExpression);
				return method;
			}
		}
		return method;
	}

	public static boolean hasContradictions(TypeBinding type) {
		SearchContradictions searchContradiction = new SearchContradictions();
		TypeBindingVisitor.visit(searchContradiction, type);
		return searchContradiction.typeWithContradiction != null;
	}

	public static TypeBinding strongerType(TypeBinding type1, TypeBinding type2, LookupEnvironment environment) {
		if ((type1.tagBits & TagBits.AnnotationNonNull) != 0)
			return mergeTypeAnnotations(type1, type2, true, environment);
		return mergeTypeAnnotations(type2, type1, true, environment); // don't bother to distinguish unannotated vs. @Nullable, since both can accept null
	}

	public static TypeBinding[] weakerTypes(TypeBinding[] parameters1, TypeBinding[] parameters2, LookupEnvironment environment) {
		TypeBinding[] newParameters = new TypeBinding[parameters1.length];
		for (int i = 0; i < newParameters.length; i++) {
			long tagBits1 = parameters1[i].tagBits;
			long tagBits2 = parameters2[i].tagBits;
			if ((tagBits1 & TagBits.AnnotationNullable) != 0)
				newParameters[i] = mergeTypeAnnotations(parameters1[i], parameters2[i], true, environment);		// @Nullable must be preserved
			else if ((tagBits2 & TagBits.AnnotationNullable) != 0)
				newParameters[i] = mergeTypeAnnotations(parameters2[i], parameters1[i], true, environment);		// @Nullable must be preserved
			else if ((tagBits1 & TagBits.AnnotationNonNull) == 0)
				newParameters[i] = mergeTypeAnnotations(parameters1[i], parameters2[i], true, environment);		// unannotated must be preserved
			else
				newParameters[i] = mergeTypeAnnotations(parameters2[i], parameters1[i], true, environment);		// either unannotated, or both are @NonNull
		}
		return newParameters;
	}
	private static TypeBinding mergeTypeAnnotations(TypeBinding type, TypeBinding otherType, boolean top, LookupEnvironment environment) {
		TypeBinding mainType = type;
		if (!top) {
			// for all but the top level type superimpose other's type annotation onto type
			AnnotationBinding[] otherAnnotations = otherType.getTypeAnnotations();
			if (otherAnnotations != Binding.NO_ANNOTATIONS)
				mainType = environment.createAnnotatedType(type, otherAnnotations);
		}
		if (mainType.isParameterizedType() && otherType.isParameterizedType()) {
			ParameterizedTypeBinding ptb = (ParameterizedTypeBinding) type, otherPTB = (ParameterizedTypeBinding) otherType;
			TypeBinding[] typeArguments = ptb.arguments;
			TypeBinding[] otherTypeArguments = otherPTB.arguments;
			TypeBinding[] newTypeArguments = new TypeBinding[typeArguments.length];
			for (int i = 0; i < typeArguments.length; i++) {
				newTypeArguments[i] = mergeTypeAnnotations(typeArguments[i], otherTypeArguments[i], false, environment);
			}
			return environment.createParameterizedType(ptb.genericType(), newTypeArguments, ptb.enclosingType());
		}
		return mainType;
	}

	@Override
	public String toString() {
		if (this == NULL_ANNOTATIONS_OK) return "OK"; //$NON-NLS-1$
		if (this == NULL_ANNOTATIONS_MISMATCH) return "MISMATCH"; //$NON-NLS-1$
		if (this == NULL_ANNOTATIONS_OK_NONNULL) return "OK NonNull"; //$NON-NLS-1$
		if (this == NULL_ANNOTATIONS_UNCHECKED) return "UNCHECKED"; //$NON-NLS-1$
		StringBuilder buf = new StringBuilder();
		buf.append("Analysis result: severity="+this.severity); //$NON-NLS-1$
		buf.append(" nullStatus="+this.nullStatus); //$NON-NLS-1$
		return buf.toString();
	}
}
