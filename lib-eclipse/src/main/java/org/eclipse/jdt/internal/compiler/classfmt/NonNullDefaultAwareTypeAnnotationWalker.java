/*******************************************************************************
 * Copyright (c) 2014, 2018 GK Software AG.
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
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.classfmt;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.env.IBinaryAnnotation;
import org.eclipse.jdt.internal.compiler.env.IBinaryElementValuePair;
import org.eclipse.jdt.internal.compiler.env.IBinaryTypeAnnotation;
import org.eclipse.jdt.internal.compiler.env.ITypeAnnotationWalker;
import org.eclipse.jdt.internal.compiler.lookup.Binding;
import org.eclipse.jdt.internal.compiler.lookup.LookupEnvironment;
import org.eclipse.jdt.internal.compiler.lookup.TypeIds;

/**
 * A type annotation walker that adds missing NonNull annotations according to the current default.
 */
public class NonNullDefaultAwareTypeAnnotationWalker extends TypeAnnotationWalker {

	private final int defaultNullness;
	private final boolean atDefaultLocation;
	private final boolean atTypeBound;
	private final boolean currentArrayContentIsNonNull;
	private final boolean isEmpty;
	private final IBinaryAnnotation nonNullAnnotation;
	private final LookupEnvironment environment;

	private boolean nextIsDefaultLocation;
	private boolean nextIsTypeBound;
	private boolean nextArrayContentIsNonNull;


	/** Create initial walker with non-empty type annotations. */
	public NonNullDefaultAwareTypeAnnotationWalker(IBinaryTypeAnnotation[] typeAnnotations,
						int defaultNullness, LookupEnvironment environment) {
		super(typeAnnotations);
		this.nonNullAnnotation = getNonNullAnnotation(environment);
		this.defaultNullness = defaultNullness;
		this.environment = environment;
		this.atDefaultLocation = false;
		this.atTypeBound = false;
		this.isEmpty = false;
		this.currentArrayContentIsNonNull = false;
	}

	/** Create an initial walker without 'real' type annotations, but with a nonnull default. */
	public NonNullDefaultAwareTypeAnnotationWalker(int defaultNullness, LookupEnvironment environment) {
		this(defaultNullness, getNonNullAnnotation(environment), false, false, environment, false);
	}

	/** Get restricted walker, still with non-empty type annotations. */
	NonNullDefaultAwareTypeAnnotationWalker(IBinaryTypeAnnotation[] typeAnnotations, long newMatches, int newPathPtr,
						int defaultNullness, IBinaryAnnotation nonNullAnnotation, boolean atDefaultLocation, boolean atTypeBound,
						LookupEnvironment environment, boolean currentArrayContentIsNonNull) {
		super(typeAnnotations, newMatches, newPathPtr);
		this.defaultNullness = defaultNullness;
		this.nonNullAnnotation = nonNullAnnotation;
		this.atDefaultLocation = atDefaultLocation;
		this.atTypeBound = atTypeBound;
		this.environment = environment;
		this.currentArrayContentIsNonNull = this.nextArrayContentIsNonNull = currentArrayContentIsNonNull;
		this.isEmpty = false;
	}

	/** Create a restricted walker without 'real' type annotations, but with a nonnull default. */
	NonNullDefaultAwareTypeAnnotationWalker(int defaultNullness, IBinaryAnnotation nonNullAnnotation,
						boolean atDefaultLocation, boolean atTypeBound, LookupEnvironment environment, boolean currentArrayContentIsNonNull) {
		super(null, 0, 0);
		this.nonNullAnnotation = nonNullAnnotation;
		this.defaultNullness = defaultNullness;
		this.atDefaultLocation = atDefaultLocation;
		this.atTypeBound = atTypeBound;
		this.isEmpty = true;
		this.environment = environment;
		this.currentArrayContentIsNonNull = this.nextArrayContentIsNonNull = currentArrayContentIsNonNull;
	}

	private static IBinaryAnnotation getNonNullAnnotation(LookupEnvironment environment) {
		final char[] nonNullAnnotationName = CharOperation.concat(
						'L', CharOperation.concatWith(environment.getNonNullAnnotationName(), '/'), ';');
		// create the synthetic annotation:
		return new IBinaryAnnotation() {
			@Override
			public char[] getTypeName() {
				return nonNullAnnotationName;
			}
			@Override
			public IBinaryElementValuePair[] getElementValuePairs() {
				return null;
			}
		};
	}

	@Override
	protected TypeAnnotationWalker restrict(long newMatches, int newPathPtr) {
		// considers nextIsDefaultLocation as the new atDefaultLocation
		try {
			// do we have any change at all?
			if (this.matches == newMatches && this.pathPtr == newPathPtr
					&& this.atDefaultLocation == this.nextIsDefaultLocation && this.atTypeBound == this.nextIsTypeBound && this.currentArrayContentIsNonNull == this.nextArrayContentIsNonNull)
				return this;
			// are we running out of real type annotations?
			if (newMatches == 0 || this.typeAnnotations == null || this.typeAnnotations.length == 0)
				return new NonNullDefaultAwareTypeAnnotationWalker(this.defaultNullness, this.nonNullAnnotation,
												this.nextIsDefaultLocation, this.nextIsTypeBound, this.environment, this.nextArrayContentIsNonNull);
			// proceed as normal, but pass on our specific fields, too:
			return new NonNullDefaultAwareTypeAnnotationWalker(this.typeAnnotations, newMatches, newPathPtr,
												this.defaultNullness, this.nonNullAnnotation, this.nextIsDefaultLocation,
												this.nextIsTypeBound, this.environment, this.nextArrayContentIsNonNull);
		} finally {
			this.nextIsDefaultLocation = false; // expire
			this.nextIsTypeBound = false;
			this.nextArrayContentIsNonNull = this.currentArrayContentIsNonNull;
		}
	}

	@Override
	public ITypeAnnotationWalker toSupertype(short index, char[] superTypeSignature) {
		if (this.isEmpty) return restrict(this.matches, this.pathPtr);
		return super.toSupertype(index, superTypeSignature);
	}

	@Override
	public ITypeAnnotationWalker toMethodParameter(short index) {
		// don't set nextIsDefaultLocation, because signature-level nullness is handled by ImplicitNullAnnotationVerifier (triggered per invocation via MessageSend.resolveType() et al)
		if (this.isEmpty) return restrict(this.matches, this.pathPtr);
		return super.toMethodParameter(index);
	}

	@Override
	public ITypeAnnotationWalker toField() {
		// don't set nextIsDefaultLocation, because field-level nullness is handled by BinaryTypeBinding.scanFieldForNullAnnotation
		if (this.isEmpty) return restrict(this.matches, this.pathPtr);
		return super.toField();
	}

	@Override
	public ITypeAnnotationWalker toMethodReturn() {
		// don't set nextIsDefaultLocation, because signature-level nullness is handled by ImplicitNullAnnotationVerifier (triggered per invocation via MessageSend.resolveType() et al)
		if (this.isEmpty) return restrict(this.matches, this.pathPtr);
		return super.toMethodReturn();
	}

	@Override
	public ITypeAnnotationWalker toTypeBound(short boundIndex) {
		this.nextIsDefaultLocation = (this.defaultNullness & Binding.DefaultLocationTypeBound) != 0;
		this.nextIsTypeBound = true;
		this.nextArrayContentIsNonNull = false;
		if (this.isEmpty) return restrict(this.matches, this.pathPtr);
		return super.toTypeBound(boundIndex);
	}

	@Override
	public ITypeAnnotationWalker toWildcardBound() {
		this.nextIsDefaultLocation = (this.defaultNullness & Binding.DefaultLocationTypeBound) != 0;
		this.nextIsTypeBound = true;
		this.nextArrayContentIsNonNull = false;
		if (this.isEmpty) return restrict(this.matches, this.pathPtr);
		return super.toWildcardBound();
	}

	@Override
	public ITypeAnnotationWalker toTypeParameterBounds(boolean isClassTypeParameter, int parameterRank) {
		this.nextIsDefaultLocation = (this.defaultNullness & Binding.DefaultLocationTypeBound) != 0;
		this.nextIsTypeBound = true;
		this.nextArrayContentIsNonNull = false;
		if (this.isEmpty) return restrict(this.matches, this.pathPtr);
		return super.toTypeParameterBounds(isClassTypeParameter, parameterRank);
	}

	@Override
	public ITypeAnnotationWalker toTypeArgument(int rank) {
		this.nextIsDefaultLocation = (this.defaultNullness & Binding.DefaultLocationTypeArgument) != 0;
		this.nextIsTypeBound = false;
		this.nextArrayContentIsNonNull = false;
		if (this.isEmpty) return restrict(this.matches, this.pathPtr);
		return super.toTypeArgument(rank);
	}

	@Override
	public ITypeAnnotationWalker toTypeParameter(boolean isClassTypeParameter, int rank) {
		this.nextIsDefaultLocation = (this.defaultNullness & Binding.DefaultLocationTypeParameter) != 0;
		this.nextIsTypeBound = false;
		this.nextArrayContentIsNonNull = false;
		if (this.isEmpty) return restrict(this.matches, this.pathPtr);
		return super.toTypeParameter(isClassTypeParameter, rank);
	}

	@Override
	protected ITypeAnnotationWalker toNextDetail(int detailKind) {
		if (this.isEmpty) return restrict(this.matches, this.pathPtr);
		return super.toNextDetail(detailKind);
	}

	@Override
	public IBinaryAnnotation[] getAnnotationsAtCursor(int currentTypeId, boolean mayApplyArrayContentsDefaultNullness) {
		IBinaryAnnotation[] normalAnnotations = this.isEmpty ? NO_ANNOTATIONS : super.getAnnotationsAtCursor(currentTypeId, mayApplyArrayContentsDefaultNullness);
		if ((this.atDefaultLocation || (mayApplyArrayContentsDefaultNullness && this.currentArrayContentIsNonNull)) &&
				!(currentTypeId == -1) && // never apply default on type variable use or wildcard
				!(this.atTypeBound && currentTypeId == TypeIds.T_JavaLangObject)) // for CLIMB-to-top consider a j.l.Object type bound as no explicit type bound
		{
			if (normalAnnotations == null || normalAnnotations.length == 0)
				return new IBinaryAnnotation[] { this.nonNullAnnotation };
			if (this.environment.containsNullTypeAnnotation(normalAnnotations)) {
				// no default annotation if explicit annotation exists
				return normalAnnotations;
			} else {
				// merge:
				int len = normalAnnotations.length;
				IBinaryAnnotation[] newAnnots = new IBinaryAnnotation[len+1];
				System.arraycopy(normalAnnotations, 0, newAnnots, 0, len);
				newAnnots[len] = this.nonNullAnnotation;
				return newAnnots;
			}
		}
		return normalAnnotations;
	}

	@Override
	public ITypeAnnotationWalker toNextArrayDimension() {
		boolean hasNNBDForArrayContents = (this.defaultNullness & Binding.DefaultLocationArrayContents) != 0;
		if (hasNNBDForArrayContents) {
			this.nextArrayContentIsNonNull = true;
		}
		this.nextIsDefaultLocation = false;
		this.nextIsTypeBound = false;
		if (this.isEmpty)
			return restrict(this.matches, this.pathPtr);
		return super.toNextArrayDimension();
	}

	public static ITypeAnnotationWalker updateWalkerForParamNonNullDefault(ITypeAnnotationWalker walker,
			int defaultNullness, LookupEnvironment environment) {
		if (environment.globalOptions.isAnnotationBasedNullAnalysisEnabled) {
			if (defaultNullness != Binding.NO_NULL_DEFAULT) {
				if (defaultNullness == Binding.NULL_UNSPECIFIED_BY_DEFAULT) {
					if (walker instanceof NonNullDefaultAwareTypeAnnotationWalker) {
						NonNullDefaultAwareTypeAnnotationWalker nonNullDefaultAwareTypeAnnotationWalker = (NonNullDefaultAwareTypeAnnotationWalker) walker;
						return new TypeAnnotationWalker(nonNullDefaultAwareTypeAnnotationWalker.typeAnnotations,
								nonNullDefaultAwareTypeAnnotationWalker.matches,
								nonNullDefaultAwareTypeAnnotationWalker.pathPtr);
					} else {
						return walker;
					}
				} else {
					if (walker instanceof TypeAnnotationWalker) {
						TypeAnnotationWalker typeAnnotationWalker = (TypeAnnotationWalker) walker;

						IBinaryAnnotation nonNullAnnotation2;
						if (walker instanceof NonNullDefaultAwareTypeAnnotationWalker) {
							NonNullDefaultAwareTypeAnnotationWalker nonNullDefaultAwareTypeAnnotationWalker = (NonNullDefaultAwareTypeAnnotationWalker) walker;
							if(nonNullDefaultAwareTypeAnnotationWalker.isEmpty) {
								return new NonNullDefaultAwareTypeAnnotationWalker(defaultNullness, environment);
							}
							nonNullAnnotation2 = nonNullDefaultAwareTypeAnnotationWalker.nonNullAnnotation;
						} else {
							nonNullAnnotation2 = getNonNullAnnotation(environment);
						}
						return new NonNullDefaultAwareTypeAnnotationWalker(typeAnnotationWalker.typeAnnotations,
								typeAnnotationWalker.matches, typeAnnotationWalker.pathPtr, defaultNullness,
								nonNullAnnotation2, false, false, environment, false);
					} else {
						// empty or walker from ExternalAnnotationProvider
						return new NonNullDefaultAwareTypeAnnotationWalker(defaultNullness, environment);
					}
				}
			}
		}
		return walker;
	}
}
