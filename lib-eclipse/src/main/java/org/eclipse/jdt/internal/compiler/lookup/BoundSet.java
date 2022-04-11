/*******************************************************************************
 * Copyright (c) 2013, 2020 GK Software AG.
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
 *     IBM Corporation - Bug fixes
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.lookup;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.jdt.internal.compiler.ast.Wildcard;

/**
 * Implementation of 18.1.3 in JLS8.
 * This class is also responsible for incorporation as defined in 18.3.
 */
class BoundSet {

	/**
	 * Set the identically-named system property to false to disable the
	 * optimization implemented to fix Bug 543480.
	 */
	public static boolean enableOptimizationForBug543480 = true;
	static {
		String enableOptimizationForBug543480Property = System.getProperty("enableOptimizationForBug543480"); //$NON-NLS-1$
		if(enableOptimizationForBug543480Property != null) {
			enableOptimizationForBug543480 = enableOptimizationForBug543480Property.equalsIgnoreCase("true"); //$NON-NLS-1$
		}
	}

	static final BoundSet TRUE = new BoundSet();	// empty set of bounds
	static final BoundSet FALSE = new BoundSet();	// pseudo bounds

	/**
	 * For a given inference variable this structure holds all type bounds
	 * with a relation in { SUPERTYPE, SAME, SUBTYPE }.
	 * These are internally stored in three sets, one for each of the relations.
	 */
	private static class ThreeSets {
		Set<TypeBound> superBounds;
		Set<TypeBound> sameBounds;
		Set<TypeBound> subBounds;
		TypeBinding	instantiation;
		Map<InferenceVariable,TypeBound> inverseBounds; // from right inference variable to bound
		Set<InferenceVariable> dependencies;
		public ThreeSets() {
			// empty, the sets are lazily initialized
		}
		/** Add a type bound to the appropriate set. */
		public boolean addBound(TypeBound bound) {
			boolean result = addBound1(bound);
			if(result) {
				Set<InferenceVariable> set = (this.dependencies == null ? new LinkedHashSet<>() : this.dependencies);
				bound.right.collectInferenceVariables(set);
				if (this.dependencies == null && set.size() > 0) {
					this.dependencies = set;
				}
			}
			return result;
		}
		private boolean addBound1(TypeBound bound) {
			switch (bound.relation) {
				case ReductionResult.SUPERTYPE:
					if (this.superBounds == null) this.superBounds = new LinkedHashSet<>();
					return this.superBounds.add(bound);
				case ReductionResult.SAME:
					if (this.sameBounds == null) this.sameBounds = new LinkedHashSet<>();
					return this.sameBounds.add(bound);
				case ReductionResult.SUBTYPE:
					if (this.subBounds == null) this.subBounds = new LinkedHashSet<>();
					return this.subBounds.add(bound);
				default:
					throw new IllegalArgumentException("Unexpected bound relation in : " + bound); //$NON-NLS-1$
			}
		}
		// pre: this.superBounds != null
		public TypeBinding[] lowerBounds(boolean onlyProper, InferenceVariable variable) {
			TypeBinding[] boundTypes = new TypeBinding[this.superBounds.size()];
			Iterator<TypeBound> it = this.superBounds.iterator();
			long nullHints = variable.nullHints;
			int i = 0;
			while(it.hasNext()) {
				TypeBound current = it.next();
				TypeBinding boundType = current.right;
				if (!onlyProper || boundType.isProperType(true)) {
					boundTypes[i++] = boundType;
					nullHints |= current.nullHints;
				}
			}
			if (i == 0)
				return Binding.NO_TYPES;
			if (i < boundTypes.length)
				System.arraycopy(boundTypes, 0, boundTypes=new TypeBinding[i], 0, i);
			useNullHints(nullHints, boundTypes, variable.environment);
			InferenceContext18.sortTypes(boundTypes);
			return boundTypes;
		}
		// pre: this.subBounds != null
		public TypeBinding[] upperBounds(boolean onlyProper, InferenceVariable variable) {
			TypeBinding[] rights = new TypeBinding[this.subBounds.size()];
			TypeBinding simpleUpper = null;
			Iterator<TypeBound> it = this.subBounds.iterator();
			long nullHints = variable.nullHints;
			int i = 0;
			while(it.hasNext()) {
				TypeBinding right=it.next().right;
				if (!onlyProper || right.isProperType(true)) {
					if (right instanceof ReferenceBinding) {
						rights[i++] = right;
						nullHints |= right.tagBits & TagBits.AnnotationNullMASK;
					} else {
						if (simpleUpper != null)
							return Binding.NO_TYPES; // shouldn't
						simpleUpper = right;
					}
				}
			}
			if (i == 0)
				return simpleUpper != null ? new TypeBinding[] { simpleUpper } : Binding.NO_TYPES;
			if (i == 1 && simpleUpper != null)
				return new TypeBinding[] { simpleUpper }; // no nullHints since not a reference type
			if (i < rights.length)
				System.arraycopy(rights, 0, rights=new TypeBinding[i], 0, i);
			useNullHints(nullHints, rights, variable.environment);
			InferenceContext18.sortTypes(rights);
			return rights;
		}
		// pre: beta is a prototype
		public boolean hasDependency(InferenceVariable beta) {
			if(this.dependencies != null && this.dependencies.contains(beta))
				return true;
			if (this.inverseBounds != null) {
				if (this.inverseBounds.containsKey(beta)) {
					// TODO: not yet observed in tests
					return true;
				}
			}
			return false;
		}
		/** Total number of type bounds in this container. */
		public int size() {
			int size = 0;
			if (this.superBounds != null)
				size += this.superBounds.size();
			if (this.sameBounds != null)
				size += this.sameBounds.size();
			if (this.subBounds != null)
				size += this.subBounds.size();
			return size;
		}
		public int flattenInto(TypeBound[] collected, int idx) {
			if (this.superBounds != null) {
				int len = this.superBounds.size();
				System.arraycopy(this.superBounds.toArray(), 0, collected, idx, len);
				idx += len;
			}
			if (this.sameBounds != null) {
				int len = this.sameBounds.size();
				System.arraycopy(this.sameBounds.toArray(), 0, collected, idx, len);
				idx += len;
			}
			if (this.subBounds != null) {
				int len = this.subBounds.size();
				System.arraycopy(this.subBounds.toArray(), 0, collected, idx, len);
				idx += len;
			}
			return idx;
		}
		public ThreeSets copy() {
			ThreeSets copy = new ThreeSets();
			if (this.superBounds != null)
				copy.superBounds = new LinkedHashSet<>(this.superBounds);
			if (this.sameBounds != null)
				copy.sameBounds = new LinkedHashSet<>(this.sameBounds);
			if (this.subBounds != null)
				copy.subBounds = new LinkedHashSet<>(this.subBounds);
			copy.instantiation = this.instantiation;
			if (this.dependencies != null) {
				copy.dependencies = new LinkedHashSet<>(this.dependencies);
			}
			return copy;
		}
		public TypeBinding findSingleWrapperType() {
			if (this.instantiation != null) {
				if (this.instantiation.isProperType(true)) {
					switch (this.instantiation.id) {
						case TypeIds.T_JavaLangByte:
						case TypeIds.T_JavaLangShort:
						case TypeIds.T_JavaLangCharacter:
						case TypeIds.T_JavaLangInteger:
						case TypeIds.T_JavaLangLong:
						case TypeIds.T_JavaLangFloat:
						case TypeIds.T_JavaLangDouble:
						case TypeIds.T_JavaLangBoolean:
							return this.instantiation;
					}
				}
			}
			if (this.subBounds != null) {
				Iterator<TypeBound> it = this.subBounds.iterator();
				while(it.hasNext()) {
					TypeBinding boundType = it.next().right;
					if ((boundType).isProperType(true)) {
						switch (boundType.id) {
							case TypeIds.T_JavaLangByte:
							case TypeIds.T_JavaLangShort:
							case TypeIds.T_JavaLangCharacter:
							case TypeIds.T_JavaLangInteger:
							case TypeIds.T_JavaLangLong:
							case TypeIds.T_JavaLangFloat:
							case TypeIds.T_JavaLangDouble:
							case TypeIds.T_JavaLangBoolean:
								return boundType;
						}
					}
				}
			}
			if (this.superBounds != null) {
				Iterator<TypeBound> it = this.superBounds.iterator();
				while(it.hasNext()) {
					TypeBinding boundType = it.next().right;
					if ((boundType).isProperType(true)) {
						switch (boundType.id) {
							case TypeIds.T_JavaLangByte:
							case TypeIds.T_JavaLangShort:
							case TypeIds.T_JavaLangCharacter:
							case TypeIds.T_JavaLangInteger:
							case TypeIds.T_JavaLangLong:
							case TypeIds.T_JavaLangFloat:
							case TypeIds.T_JavaLangDouble:
							case TypeIds.T_JavaLangBoolean:
								return boundType;
						}
					}
				}
			}
			return null;
		}
		/**
		 * Not per JLS: enhance the given type bounds using the nullHints, if useful.
		 * Will only ever be effective if any TypeBounds carry nullHints,
		 * which only happens if any TypeBindings have non-zero (tagBits & AnnotationNullMASK),
		 * which only happens if null annotations are enabled in the first place.
		 */
		private void useNullHints(long nullHints, TypeBinding[] boundTypes, LookupEnvironment environment) {
			if (nullHints == TagBits.AnnotationNullMASK) {
				// on contradiction remove null type annotations
				for (int i = 0; i < boundTypes.length; i++)
					boundTypes[i] = boundTypes[i].withoutToplevelNullAnnotation();
			} else {
				AnnotationBinding[] annot = environment.nullAnnotationsFromTagBits(nullHints);
				if (annot != null) {
					// only get here if exactly one of @NonNull or @Nullable was hinted; now apply this hint:
					for (int i = 0; i < boundTypes.length; i++)
						boundTypes[i] = environment.createAnnotatedType(boundTypes[i], annot);
				}
			}
		}
		TypeBinding combineAndUseNullHints(TypeBinding type, long nullHints, LookupEnvironment environment) {
			// precondition: only called when null annotations are enabled.
			// TODO(optimization): may want to collect all nullHints in the ThreeSets, which, however,
			// needs a reference TypeBound->ThreeSets to propagate the bits as they are added.
			if (this.sameBounds != null) {
				Iterator<TypeBound> it = this.sameBounds.iterator();
				while(it.hasNext())
					nullHints |= it.next().nullHints;
			}
			if (this.superBounds != null) {
				Iterator<TypeBound> it = this.superBounds.iterator();
				while(it.hasNext())
					nullHints |= it.next().nullHints;
			}
			if (this.subBounds != null) {
				Iterator<TypeBound> it = this.subBounds.iterator();
				while(it.hasNext())
					nullHints |= it.next().nullHints;
			}
			if (nullHints == TagBits.AnnotationNullMASK) // on contradiction remove null type annotations
				return type.withoutToplevelNullAnnotation();
			AnnotationBinding[] annot = environment.nullAnnotationsFromTagBits(nullHints);
			if (annot != null)
				// only get here if exactly one of @NonNull or @Nullable was hinted; now apply this hint:
				return environment.createAnnotatedType(type, annot);
			return type;
		}
		public void setInstantiation(TypeBinding type, InferenceVariable variable, LookupEnvironment environment) {
			if (environment.globalOptions.isAnnotationBasedNullAnalysisEnabled) {
				long variableBits = variable.tagBits & TagBits.AnnotationNullMASK;
				long allBits = type.tagBits | variableBits;
				if (this.instantiation != null)
					allBits |= this.instantiation.tagBits;
				allBits &= TagBits.AnnotationNullMASK;
				if (allBits == TagBits.AnnotationNullMASK) { // contradiction
					allBits = variableBits;
				}
				if (allBits != (type.tagBits & TagBits.AnnotationNullMASK)) {
					AnnotationBinding[] annot = environment.nullAnnotationsFromTagBits(allBits);
					if (annot != null)
						type = environment.createAnnotatedType(type.withoutToplevelNullAnnotation(), annot);
					else if (type.hasNullTypeAnnotations())
						type = type.withoutToplevelNullAnnotation();
				}
			}
			this.instantiation = type;
		}
	}
	// main storage of type bounds:
	HashMap<InferenceVariable, ThreeSets> boundsPerVariable = new LinkedHashMap<>();

	/**
	 * 18.1.3 bullet 4: G<α1, ..., αn> = capture(G<A1, ..., An>)
	 * On both sides we only enter types with nonnull arguments.
	 */
	HashMap<ParameterizedTypeBinding,ParameterizedTypeBinding> captures = new LinkedHashMap<>();
	/** 18.1.3 bullet 5: throws α */
	Set<InferenceVariable> inThrows = new LinkedHashSet<>();

	private TypeBound[] incorporatedBounds = Binding.NO_TYPE_BOUNDS;
	private TypeBound[] unincorporatedBounds = new TypeBound[8];
	private int unincorporatedBoundsCount = 0;
	private TypeBound[] mostRecentBounds = new TypeBound[4]; // for quick & dirty duplicate elimination

	public BoundSet() {}

	// pre: typeParameters != null, variables[i].typeParameter == typeParameters[i]
	public void addBoundsFromTypeParameters(InferenceContext18 context, TypeVariableBinding[] typeParameters, InferenceVariable[] variables) {
		int length = typeParameters.length;
		for (int i = 0; i < length; i++) {
			TypeVariableBinding typeParameter = typeParameters[i];
			InferenceVariable variable = variables[i];
			TypeBound[] someBounds = typeParameter.getTypeBounds(variable, new InferenceSubstitution(context));
			boolean hasProperBound = false;
			if (someBounds.length > 0)
				hasProperBound = addBounds(someBounds, context.environment);
			if (!hasProperBound)
				addBound(new TypeBound(variable, context.object, ReductionResult.SUBTYPE), context.environment);
		}
	}

	/** Answer a flat representation of this BoundSet. */
	public TypeBound[] flatten() {
		int size = 0;
		Iterator<ThreeSets> outerIt = this.boundsPerVariable.values().iterator();
		while (outerIt.hasNext())
			size += outerIt.next().size();
		if (size == 0) return Binding.NO_TYPE_BOUNDS;
		TypeBound[] collected = new TypeBound[size];
		outerIt = this.boundsPerVariable.values().iterator();
		int idx = 0;
		while (outerIt.hasNext())
			idx = outerIt.next().flattenInto(collected, idx);
		return collected;
	}

	/**
	 * For resolution we work with a copy of the bound set, to enable retrying.
	 * @return the new bound set.
	 */
	public BoundSet copy() {
		BoundSet copy = new BoundSet();
		if (!this.boundsPerVariable.isEmpty()) {
			Iterator<Entry<InferenceVariable, ThreeSets>> setsIterator = this.boundsPerVariable.entrySet().iterator();
			while (setsIterator.hasNext()) {
				Entry<InferenceVariable, ThreeSets> entry = setsIterator.next();
				copy.boundsPerVariable.put(entry.getKey(), entry.getValue().copy());
			}
		}
		copy.inThrows.addAll(this.inThrows);
		copy.captures.putAll(this.captures);
		if (this.incorporatedBounds.length > 0)
			System.arraycopy(this.incorporatedBounds, 0, copy.incorporatedBounds = new TypeBound[this.incorporatedBounds.length], 0, this.incorporatedBounds.length);
		if (this.unincorporatedBoundsCount > 0)
			System.arraycopy(this.unincorporatedBounds, 0, copy.unincorporatedBounds = new TypeBound[this.unincorporatedBounds.length], 0, this.unincorporatedBounds.length);
		copy.unincorporatedBoundsCount = this.unincorporatedBoundsCount;
		return copy;
	}

	public void addBound(TypeBound bound, LookupEnvironment environment) {

		if (bound.relation == ReductionResult.SUBTYPE && bound.right.id == TypeIds.T_JavaLangObject)
			return;
		if (bound.left == bound.right) //$IDENTITY-COMPARISON$
			return;
		for (int recent = 0; recent < 4; recent++) {
			if (bound.equals(this.mostRecentBounds[recent])) {
				if (environment.globalOptions.isAnnotationBasedNullAnalysisEnabled) {
					TypeBound existing = this.mostRecentBounds[recent];
					long boundNullBits = bound.right.tagBits & TagBits.AnnotationNullMASK;
					long existingNullBits = existing.right.tagBits & TagBits.AnnotationNullMASK;
					if (boundNullBits != existingNullBits) {
						if (existingNullBits == 0)
							existing.right = bound.right;
						else if (boundNullBits != 0) // combine bits from both sources, even if this creates a contradiction
							existing.right = environment.createAnnotatedType(existing.right, environment.nullAnnotationsFromTagBits(boundNullBits));
					}
				}
				return;
			}
		}

		this.mostRecentBounds[3] = this.mostRecentBounds[2];
		this.mostRecentBounds[2] = this.mostRecentBounds[1];
		this.mostRecentBounds[1] = this.mostRecentBounds[0];
		this.mostRecentBounds[0] = bound;

		InferenceVariable variable = bound.left.prototype();
		ThreeSets three = this.boundsPerVariable.get(variable);
		if (three == null)
			this.boundsPerVariable.put(variable, (three = new ThreeSets()));
		if (three.addBound(bound)) {
			int unincorporatedBoundsLength = this.unincorporatedBounds.length;
			if (this.unincorporatedBoundsCount >= unincorporatedBoundsLength)
				System.arraycopy(this.unincorporatedBounds, 0, this.unincorporatedBounds = new TypeBound[unincorporatedBoundsLength * 2], 0, unincorporatedBoundsLength);
			this.unincorporatedBounds[this.unincorporatedBoundsCount ++] = bound;
			// check if this makes the inference variable instantiated:
			TypeBinding typeBinding = bound.right;
			if (bound.relation == ReductionResult.SAME && typeBinding.isProperType(true))
				three.setInstantiation(typeBinding, variable, environment);
			if (bound.right instanceof InferenceVariable) {
				// for a dependency between two IVs make a note about the inverse bound.
				// this should be needed to determine IV dependencies independent of direction.
				// TODO: so far no test could be identified which actually needs it ...
				InferenceVariable rightIV = (InferenceVariable) bound.right.prototype();
				three = this.boundsPerVariable.get(rightIV);
				if (three == null)
					this.boundsPerVariable.put(rightIV, (three = new ThreeSets()));
				if (three.inverseBounds == null)
					three.inverseBounds = new HashMap<>();
				three.inverseBounds.put(rightIV, bound);
			}
		}
	}

	private boolean addBounds(TypeBound[] newBounds, LookupEnvironment environment) {
		boolean hasProperBound = false;
		for (int i = 0; i < newBounds.length; i++) {
			addBound(newBounds[i], environment);
			hasProperBound |= newBounds[i].isBound();
		}
		return hasProperBound;
	}

	public void addBounds(BoundSet that, LookupEnvironment environment) {
		if (that == null || environment == null)
			return;
		addBounds(that.flatten(), environment);
	}

	public boolean isInstantiated(InferenceVariable inferenceVariable) {
		ThreeSets three = this.boundsPerVariable.get(inferenceVariable.prototype());
		if (three != null)
			return three.instantiation != null;
		return false;
	}

	public TypeBinding getInstantiation(InferenceVariable inferenceVariable, LookupEnvironment environment) {
		ThreeSets three = this.boundsPerVariable.get(inferenceVariable.prototype());
		if (three != null) {
			TypeBinding instantiation = three.instantiation;
			if (environment != null && environment.globalOptions.isAnnotationBasedNullAnalysisEnabled
					&& instantiation != null && (instantiation.tagBits & TagBits.AnnotationNullMASK) == 0)
				return three.combineAndUseNullHints(instantiation, inferenceVariable.nullHints, environment);
			return instantiation;
		}
		return null;
	}

	public int numUninstantiatedVariables(InferenceVariable[] variables) {
		int num = 0;
		for (int i = 0; i < variables.length; i++) {
			if (!isInstantiated(variables[i]))
				num++;
		}
		return num;
	}

	// Driver for the real workhorse - Implements generational incorporation a la generational garbage collector.
	boolean incorporate(InferenceContext18 context) throws InferenceFailureException {
		if (this.unincorporatedBoundsCount == 0 && this.captures.isEmpty())
			return true;

		do {
			TypeBound [] freshBounds;
			System.arraycopy(this.unincorporatedBounds, 0, freshBounds = new TypeBound[this.unincorporatedBoundsCount], 0, this.unincorporatedBoundsCount);
			this.unincorporatedBoundsCount = 0;

			// Pairwise bidirectional compare all bounds from previous generation with the fresh set.
			if (!incorporate(context, this.incorporatedBounds, freshBounds))
				return false;
			// Pairwise bidirectional compare all fresh bounds.
			if (!incorporate(context, freshBounds, freshBounds))
				return false;

			// Merge the bounds into one incorporated generation.
			final int incorporatedLength = this.incorporatedBounds.length;
			final int unincorporatedLength = freshBounds.length;
			TypeBound [] aggregate = new TypeBound[incorporatedLength + unincorporatedLength];
			System.arraycopy(this.incorporatedBounds, 0, aggregate, 0, incorporatedLength);
			System.arraycopy(freshBounds, 0, aggregate, incorporatedLength, unincorporatedLength);
			this.incorporatedBounds = aggregate;

		} while (this.unincorporatedBoundsCount > 0);

		return true;
	}
	/**
	 * <b>JLS 18.3:</b> Try to infer new constraints from pairs of existing type bounds.
	 * Each new constraint is first reduced and checked for TRUE or FALSE, which will
	 * abort the processing.
	 * @param context the context that manages our inference variables
	 * @return false if any constraint resolved to false, true otherwise
	 * @throws InferenceFailureException a compile error has been detected during inference
	 */
	boolean incorporate(InferenceContext18 context, TypeBound [] first, TypeBound [] next) throws InferenceFailureException {
		boolean analyzeNull = context.environment.globalOptions.isAnnotationBasedNullAnalysisEnabled;
		ConstraintTypeFormula [] mostRecentFormulas = new ConstraintTypeFormula[4]; // poor man's cache to toss out duplicates, in pathological cases there are a good quarter million of them.
		// check each pair, in each way.
		for (int i = 0, iLength = first.length; i < iLength; i++) {
			TypeBound boundI = first[i];
			for (int j = 0, jLength = next.length; j < jLength; j++) {
				TypeBound boundJ = next[j];
				if (boundI == boundJ)
					continue;
				int iteration = 1;
				do {
					ConstraintTypeFormula newConstraint = null;
					boolean deriveTypeArgumentConstraints = false;
					if (iteration == 2) {
						TypeBound boundX = boundI;
						boundI = boundJ;
						boundJ = boundX;
					}
					switch (boundI.relation) {
						case ReductionResult.SAME:
							switch (boundJ.relation) {
								case ReductionResult.SAME:
									newConstraint = combineSameSame(boundI, boundJ, first, next);
									break;
								case ReductionResult.SUBTYPE:
								case ReductionResult.SUPERTYPE:
									newConstraint = combineSameSubSuper(boundI, boundJ, first, next);
									break;
							}
							break;
						case ReductionResult.SUBTYPE:
							switch (boundJ.relation) {
								case ReductionResult.SAME:
									newConstraint = combineSameSubSuper(boundJ, boundI, first, next);
									break;
								case ReductionResult.SUPERTYPE:
									newConstraint = combineSuperAndSub(boundJ, boundI);
									break;
								case ReductionResult.SUBTYPE:
									newConstraint = combineEqualSupers(boundI, boundJ);
									deriveTypeArgumentConstraints = TypeBinding.equalsEquals(boundI.left, boundJ.left);
									break;
							}
							break;
						case ReductionResult.SUPERTYPE:
							switch (boundJ.relation) {
								case ReductionResult.SAME:
									newConstraint = combineSameSubSuper(boundJ, boundI, first, next);
									break;
								case ReductionResult.SUBTYPE:
									newConstraint = combineSuperAndSub(boundI, boundJ);
									break;
								case ReductionResult.SUPERTYPE:
									newConstraint = combineEqualSupers(boundI, boundJ);
									break;
							}
					}
					if (newConstraint != null) {
						if (newConstraint.left == newConstraint.right) { //$IDENTITY-COMPARISON$
							newConstraint = null;
						} else 	if (newConstraint.equalsEquals(mostRecentFormulas[0]) || newConstraint.equalsEquals(mostRecentFormulas[1]) ||
									newConstraint.equalsEquals(mostRecentFormulas[2]) || newConstraint.equalsEquals(mostRecentFormulas[3])) {
							newConstraint = null;
						}
					}
					if (newConstraint != null) {
						// bubble formulas around the cache.
						mostRecentFormulas[3] = mostRecentFormulas[2];
						mostRecentFormulas[2] = mostRecentFormulas[1];
						mostRecentFormulas[1] = mostRecentFormulas[0];
						mostRecentFormulas[0] = newConstraint;

						if (!reduceOneConstraint(context, newConstraint))
							return false;

						if (analyzeNull) {
							// not per JLS: if the new constraint relates types where at least one has a null annotations,
							// record all null tagBits as hints for the final inference solution.
							long nullHints = (newConstraint.left.tagBits | newConstraint.right.tagBits) & TagBits.AnnotationNullMASK;
							if (nullHints != 0) {
								if (TypeBinding.equalsEquals(boundI.left, boundJ.left)
										|| (boundI.relation == ReductionResult.SAME	&& TypeBinding.equalsEquals(boundI.right, boundJ.left))
										|| (boundJ.relation == ReductionResult.SAME	&& TypeBinding.equalsEquals(boundI.left, boundJ.right))) {
									boundI.nullHints |= nullHints;
									boundJ.nullHints |= nullHints;
								}
							}
						}
					}
					ConstraintFormula[] typeArgumentConstraints = deriveTypeArgumentConstraints ? deriveTypeArgumentConstraints(boundI, boundJ) : null;
					if (typeArgumentConstraints != null) {
						for (int k = 0, length = typeArgumentConstraints.length; k < length; k++) {
							if (!reduceOneConstraint(context, typeArgumentConstraints[k]))
								return false;
						}
					}
					if (iteration == 2) {
						TypeBound boundX = boundI;
						boundI = boundJ;
						boundJ = boundX;
					}
				} while (first != next && ++iteration <= 2);
			}
		}
		/* TODO: are we sure this will always terminate? Cf. e.g. (Discussion in 18.3):
		 *
		 *    "The assertion that incorporation reaches a fixed point oversimplifies the matter slightly. ..."
		 */
		Iterator<Entry<ParameterizedTypeBinding, ParameterizedTypeBinding>> captIter = this.captures.entrySet().iterator();
		while (captIter.hasNext()) {
			Entry<ParameterizedTypeBinding, ParameterizedTypeBinding> capt = captIter.next();
			ParameterizedTypeBinding gAlpha = capt.getKey();
			ParameterizedTypeBinding gA = capt.getValue();
			ReferenceBinding g = (ReferenceBinding) gA.original();
			final TypeVariableBinding[] parameters = g.typeVariables();
			// construct theta = [P1:=alpha1,...]
			final InferenceVariable[] alphas = new InferenceVariable[gAlpha.arguments.length];
			System.arraycopy(gAlpha.arguments, 0, alphas, 0, alphas.length);
			InferenceSubstitution theta = new InferenceSubstitution(context.environment, alphas, context.currentInvocation) {
				@Override
				protected TypeBinding getP(int i) {
					return parameters[i];
				}
			};
			for (int i = 0, length = parameters.length; i < length; i++) {
				// A set of bounds on α1, ..., αn, constructed from the declared bounds of P1, ..., Pn as described in 18.1.3, is immediately implied.
				TypeVariableBinding pi = parameters[i];
				InferenceVariable alpha = (InferenceVariable) gAlpha.arguments[i];
				addBounds(pi.getTypeBounds(alpha, theta), context.environment);

				TypeBinding ai = gA.arguments[i];
				if (ai instanceof WildcardBinding) {
					WildcardBinding wildcardBinding = (WildcardBinding)ai;
					TypeBinding t = wildcardBinding.bound;
					ThreeSets three = this.boundsPerVariable.get(alpha.prototype());
					if (three != null) {
						Iterator<TypeBound> it;
						if (three.sameBounds != null) {
							//  α = R implies false
							it = three.sameBounds.iterator();
							while (it.hasNext()) {
								TypeBound bound = it.next();
								if (!(bound.right instanceof InferenceVariable))
									return false;
							}
						}
						if (three.subBounds != null) {
							TypeBinding bi1 = pi.firstBound;
							if (bi1 == null) {
								bi1 = context.object; // implicit bound
							}
							// If Bi is Object, α <: R implies ⟨T <: R⟩	(extends wildcard)
							// α <: R implies ⟨θ Bi <: R⟩				(else)
							it = three.subBounds.iterator();
							while (it.hasNext()) {
								TypeBound bound = it.next();
								if (!(bound.right instanceof InferenceVariable)) {
									TypeBinding r = bound.right;
									ReferenceBinding[] otherBounds = pi.superInterfaces;
									TypeBinding bi;
									if (otherBounds == Binding.NO_SUPERINTERFACES) {
										bi = bi1;
									} else {
										int n = otherBounds.length+1;
										ReferenceBinding[] allBounds = new ReferenceBinding[n];
										allBounds[0] = (ReferenceBinding) bi1; // TODO is this safe?
										System.arraycopy(otherBounds, 0, allBounds, 1, n-1);
										bi = context.environment.createIntersectionType18(allBounds);
									}
									addTypeBoundsFromWildcardBound(context, theta, wildcardBinding.boundKind, t, r, bi);
								}
							}
						}
						if (three.superBounds != null) {
							//  R <: α implies ⟨R <: T⟩  (super wildcard)
							//  R <: α implies false	 (else)
							it = three.superBounds.iterator();
							while (it.hasNext()) {
								TypeBound bound = it.next();
								if (!(bound.right instanceof InferenceVariable)) {
									if (wildcardBinding.boundKind == Wildcard.SUPER)
										reduceOneConstraint(context, ConstraintTypeFormula.create(bound.right, t, ReductionResult.SUBTYPE));
									else
										return false;
								}
							}
						}
					}
				} else {
					addBound(new TypeBound(alpha, ai, ReductionResult.SAME), context.environment);
				}
			}
		}
		this.captures.clear();
		return true;
	}

	void addTypeBoundsFromWildcardBound(InferenceContext18 context, InferenceSubstitution theta, int boundKind, TypeBinding t,
			TypeBinding r, TypeBinding bi) throws InferenceFailureException {
		ConstraintFormula formula = null;
		if (boundKind == Wildcard.EXTENDS) {
			if (bi.id == TypeIds.T_JavaLangObject)
				formula = ConstraintTypeFormula.create(t, r, ReductionResult.SUBTYPE);
			if (t.id == TypeIds.T_JavaLangObject)
				formula = ConstraintTypeFormula.create(theta.substitute(theta, bi), r, ReductionResult.SUBTYPE);
		} else {
			formula = ConstraintTypeFormula.create(theta.substitute(theta, bi), r, ReductionResult.SUBTYPE);
		}
		if (formula != null)
			reduceOneConstraint(context, formula);
	}

	private ConstraintTypeFormula combineSameSame(TypeBound boundS, TypeBound boundT, TypeBound[] firstBounds, TypeBound[] nextBounds) {

		// α = S and α = T imply ⟨S = T⟩
		if (TypeBinding.equalsEquals(boundS.left, boundT.left))
			return ConstraintTypeFormula.create(boundS.right, boundT.right, ReductionResult.SAME, boundS.isSoft||boundT.isSoft);

		// match against more shapes:
		ConstraintTypeFormula newConstraint;
		newConstraint = combineSameSameWithProperType(boundS, boundT, firstBounds, nextBounds);
		if (newConstraint != null)
			return newConstraint;
		newConstraint = combineSameSameWithProperType(boundT, boundS, firstBounds, nextBounds);
		if (newConstraint != null)
			return newConstraint;
		return null;
	}

	// pre: boundLeft.left != boundRight.left
	private ConstraintTypeFormula combineSameSameWithProperType(TypeBound boundLeft, TypeBound boundRight,
			TypeBound[] firstBounds, TypeBound[] nextBounds) {
		//  α = U and S = T imply ⟨S[α:=U] = T[α:=U]⟩
		TypeBinding u = boundLeft.right;
		if (enableOptimizationForBug543480 && isParameterizedDependency(boundRight)) {
			// Performance optimization: do not incorporate arguments one by one, which yielt 2^n new bounds (n=number of type arguments) in the past.
			// Instead, all arguments of a parameterized dependency are incorporated at once - but only when they are available.
			return incorporateIntoParameterizedDependencyIfAllArgumentsAreProperTypes(boundRight,
					firstBounds, nextBounds);
		}
		if (u.isProperType(true)) {
			InferenceVariable alpha = boundLeft.left;
			TypeBinding left = boundRight.left; // no substitution since S inference variable and (S != α) per precondition
			TypeBinding right = boundRight.right.substituteInferenceVariable(alpha, u);
			return ConstraintTypeFormula.create(left, right, ReductionResult.SAME, boundLeft.isSoft||boundRight.isSoft);
		}
		return null;
	}

	private ConstraintTypeFormula combineSameSubSuper(TypeBound boundS, TypeBound boundT, TypeBound[] firstBounds, TypeBound[] nextBounds) {
		//  α = S and α <: T imply ⟨S <: T⟩
		//  α = S and T <: α imply ⟨T <: S⟩
		InferenceVariable alpha = boundS.left;
		TypeBinding s = boundS.right;
		if (TypeBinding.equalsEquals(alpha, boundT.left)) {
			TypeBinding t = boundT.right;
			return ConstraintTypeFormula.create(s, t, boundT.relation, boundT.isSoft||boundS.isSoft);
		}
		if (TypeBinding.equalsEquals(alpha, boundT.right)) {
			TypeBinding t = boundT.left;
			return ConstraintTypeFormula.create(t, s, boundT.relation, boundT.isSoft||boundS.isSoft);
		}

		if (boundS.right instanceof InferenceVariable) {
			// reverse:
			alpha = (InferenceVariable) boundS.right;
			s = boundS.left;
			if (TypeBinding.equalsEquals(alpha, boundT.left)) {
				TypeBinding t = boundT.right;
				return ConstraintTypeFormula.create(s, t, boundT.relation, boundT.isSoft||boundS.isSoft);
			}
			if (TypeBinding.equalsEquals(alpha, boundT.right)) {
				TypeBinding t = boundT.left;
				return ConstraintTypeFormula.create(t, s, boundT.relation, boundT.isSoft||boundS.isSoft);
			}
		}
		return combineSameSubSuperWithProperType(boundS, boundT, alpha, firstBounds, nextBounds);
	}

	// pre: boundLeft.left != boundRight.left
	// pre: boundLeft.left != boundRight.right
	private ConstraintTypeFormula combineSameSubSuperWithProperType(TypeBound boundLeft, TypeBound boundRight,
			InferenceVariable alpha, TypeBound[] firstBounds, TypeBound[] nextBounds) {
		//  α = U and S <: T imply ⟨S[α:=U] <: T[α:=U]⟩
		TypeBinding u = boundLeft.right;

		if (enableOptimizationForBug543480 && isParameterizedDependency(boundRight)) {
			// Performance optimization: do not incorporate arguments one by one, which yielt 2^n new bounds (n=number of type arguments) in the past.
			// Instead, all arguments of a parameterized dependency are incorporated at once - but only when they are available.
			return incorporateIntoParameterizedDependencyIfAllArgumentsAreProperTypes(boundRight,
								firstBounds, nextBounds);
		}
		if (u.isProperType(true)) {
			boolean substitute = TypeBinding.equalsEquals(alpha, boundRight.left);
			TypeBinding left = substitute ? u : boundRight.left;
			TypeBinding right = boundRight.right.substituteInferenceVariable(alpha, u);
			substitute |= TypeBinding.notEquals(right, boundRight.right);
			if (substitute) // avoid redundant constraint
				return ConstraintTypeFormula.create(left, right, boundRight.relation, boundRight.isSoft||boundLeft.isSoft);
		}
		return null;
	}

	private ConstraintTypeFormula combineSuperAndSub(TypeBound boundS, TypeBound boundT) {
		//  permutations of: S <: α and α <: T imply ⟨S <: T⟩
		InferenceVariable alpha = boundS.left;
		if (TypeBinding.equalsEquals(alpha, boundT.left))
			//  α >: S and α <: T imply ⟨S <: T⟩
			return ConstraintTypeFormula.create(boundS.right, boundT.right, ReductionResult.SUBTYPE, boundT.isSoft||boundS.isSoft);
		if (boundS.right instanceof InferenceVariable) {
			// try reverse:
			alpha = (InferenceVariable) boundS.right;
			if (TypeBinding.equalsEquals(alpha, boundT.right))
				// S :> α and T <: α  imply ⟨S :> T⟩
				return ConstraintTypeFormula.create(boundS.left, boundT.left, ReductionResult.SUPERTYPE, boundT.isSoft||boundS.isSoft);
		}
		return null;
	}

	private ConstraintTypeFormula combineEqualSupers(TypeBound boundS, TypeBound boundT) {
		//  more permutations of: S <: α and α <: T imply ⟨S <: T⟩
		if (TypeBinding.equalsEquals(boundS.left, boundT.right))
			// came in as: α REL S and T REL α imply ⟨T REL S⟩
			return ConstraintTypeFormula.create(boundT.left, boundS.right, boundS.relation, boundT.isSoft||boundS.isSoft);
		if (TypeBinding.equalsEquals(boundS.right, boundT.left))
			// came in as: S REL α and α REL T imply ⟨S REL T⟩
			return ConstraintTypeFormula.create(boundS.left, boundT.right, boundS.relation, boundT.isSoft||boundS.isSoft);
		return null;
	}

	private boolean isParameterizedDependency(TypeBound typeBound) {
		return typeBound.right.kind() == Binding.PARAMETERIZED_TYPE
				&& !typeBound.right.isProperType(true) /* is a dependency, not a type bound */
				&& typeBound.right.isParameterizedTypeWithActualArguments();
	}

	private ConstraintTypeFormula incorporateIntoParameterizedDependencyIfAllArgumentsAreProperTypes(TypeBound typeBound,
			TypeBound[] firstBounds, TypeBound[] nextBounds) {
		Collection<TypeBound> properTypesForAllInferenceVariables = getProperTypesForAllInferenceVariablesOrNull((ParameterizedTypeBinding)typeBound.right, firstBounds, nextBounds);
		if (null != properTypesForAllInferenceVariables) {
			return combineWithProperTypes(properTypesForAllInferenceVariables, typeBound);
		}
		return null;
	}

	private Collection<TypeBound> getProperTypesForAllInferenceVariablesOrNull(ParameterizedTypeBinding parameterizedType,
			TypeBound[] firstBounds, TypeBound[] nextBounds) {
		final Map<InferenceVariable,TypeBound> properTypesByInferenceVariable = properTypesByInferenceVariable(firstBounds, nextBounds);
		if(properTypesByInferenceVariable.size() == 0) {
			return null;
		}
		final Set<InferenceVariable> inferenceVariables = getInferenceVariables(parameterizedType);
		if(properTypesByInferenceVariable.keySet().containsAll(inferenceVariables)) {
			return properTypesByInferenceVariable.values();
		}
		return null;
	}

	private Map<InferenceVariable,TypeBound> properTypesByInferenceVariable(TypeBound[] firstBounds, TypeBound[] nextBounds) {
		return getBoundsStream(firstBounds, nextBounds)
				.filter(bound -> bound.relation == ReductionResult.SAME)
				.filter(bound -> bound.right.isProperType(true))
				.collect(toMap(bound -> bound.left, identity(),
						// If nextBounds and firstBounds have a bound for the IV, prefer the newer one from nextBounds.
						(boundFromNextBounds, boundFromFirstBounds) -> boundFromNextBounds));
	}

	private Stream<TypeBound> getBoundsStream(TypeBound[] firstBounds, TypeBound[] nextBounds) {
		if(firstBounds == nextBounds) {
			return Arrays.stream(firstBounds);
		}
		// The next bounds are considered initially because it seems more
		// likely that they contain the new bounds that enable successful
		// incorporation in this run in case no incorporation was possible
		// in previous runs.
		return Stream.concat(Arrays.stream(nextBounds), Arrays.stream(firstBounds));
	}

	private Set<InferenceVariable> getInferenceVariables(ParameterizedTypeBinding parameterizedType) {
		final Set<InferenceVariable> inferenceVariables = new LinkedHashSet<>();
		for(final TypeBinding argument: parameterizedType.arguments) {
			argument.collectInferenceVariables(inferenceVariables);
		}
		return inferenceVariables;
	}

	private ConstraintTypeFormula combineWithProperTypes(Collection<TypeBound> properTypesForAllInferenceVariables, TypeBound boundRight) {
		// either: α = U, β = V, ... and S =  T imply ⟨S[α:=U, β:=V, ...] =  T[α:=U, β:=V, ...]⟩
		// or:     α = U, β = V, ... and S <: T imply ⟨S[α:=U, β:=V, ...] <: T[α:=U, β:=V, ...]⟩
		if(properTypesForAllInferenceVariables.size() == 0) {
			return null;
		}
		boolean isAnyLeftSoft = false;
		InferenceVariable left = boundRight.left;
		TypeBinding right = boundRight.right;
		for(final TypeBound properTypeForInferenceVariable: properTypesForAllInferenceVariables) {
			final TypeBound boundLeft = properTypeForInferenceVariable;
			final InferenceVariable alpha = boundLeft.left;
			final TypeBinding u = boundLeft.right;
			isAnyLeftSoft |= boundLeft.isSoft;
			right = right.substituteInferenceVariable(alpha, u);
		}
		return ConstraintTypeFormula.create(left, right, boundRight.relation, isAnyLeftSoft||boundRight.isSoft);
	}

	private ConstraintTypeFormula[] deriveTypeArgumentConstraints(TypeBound boundS, TypeBound boundT) {
		/* From 18.4:
		 *  If two bounds have the form α <: S and α <: T, and if for some generic class or interface, G,
		 *  there exists a supertype (4.10) of S of the form G<S1, ..., Sn> and a supertype of T of the form G<T1, ..., Tn>,
		 *  then for all i, 1 ≤ i ≤ n, if Si and Ti are types (not wildcards), the constraint ⟨Si = Ti⟩ is implied.
		 */
		// callers must ensure both relations are <: and both lefts are equal
		TypeBinding[] supers = superTypesWithCommonGenericType(boundS.right, boundT.right);
		if (supers != null)
			return typeArgumentEqualityConstraints(supers[0], supers[1], boundS.isSoft || boundT.isSoft);
		return null;
	}

	private ConstraintTypeFormula[] typeArgumentEqualityConstraints(TypeBinding s, TypeBinding t, boolean isSoft) {
		if (s == null || s.kind() != Binding.PARAMETERIZED_TYPE || t == null || t.kind() != Binding.PARAMETERIZED_TYPE)
			return null;
		if (TypeBinding.equalsEquals(s, t)) // don't create useless constraints
			return null;
		TypeBinding[] sis = s.typeArguments();
		TypeBinding[] tis = t.typeArguments();
		if (sis == null || tis == null || sis.length != tis.length)
			return null;
		List<ConstraintTypeFormula> result = new ArrayList<>();
		for (int i = 0; i < sis.length; i++) {
			TypeBinding si = sis[i];
			TypeBinding ti = tis[i];
			if (si.isWildcard() || ti.isWildcard() || TypeBinding.equalsEquals(si, ti))
				continue;
			result.add(ConstraintTypeFormula.create(si, ti, ReductionResult.SAME, isSoft));
		}
		if (result.size() > 0)
			return result.toArray(new ConstraintTypeFormula[result.size()]);
		return null;
	}

	/**
	 * Try to reduce the one given constraint.
	 * If a constraint produces further constraints reduce those recursively.
	 * @throws InferenceFailureException a compile error has been detected during inference
	 */
	public boolean reduceOneConstraint(InferenceContext18 context, ConstraintFormula currentConstraint) throws InferenceFailureException {
		Object result = currentConstraint.reduce(context);
		if (result == ReductionResult.FALSE)
			return false;
		if (result == ReductionResult.TRUE)
			return true;
		if (result == currentConstraint) {
			// not reduceable
			throw new IllegalStateException("Failed to reduce constraint formula"); //$NON-NLS-1$
		}
		if (result != null) {
			if (result instanceof ConstraintFormula) {
				if (!reduceOneConstraint(context, (ConstraintFormula) result))
					return false;
			} else if (result instanceof ConstraintFormula[]) {
				ConstraintFormula[] resultArray = (ConstraintFormula[]) result;
				for (int i = 0; i < resultArray.length; i++)
					if (!reduceOneConstraint(context, resultArray[i]))
						return false;
			} else {
				addBound((TypeBound)result, context.environment);
			}
		}
		return true; // no FALSE encountered
	}

	/**
	 * Helper for resolution (18.4):
	 * Does this bound set define a direct dependency between the two given inference variables?
	 */
	public boolean dependsOnResolutionOf(InferenceVariable alpha, InferenceVariable beta) {
		alpha = alpha.prototype();
		beta = beta.prototype();
		if (TypeBinding.equalsEquals(alpha, beta))
			return true; // An inference variable α depends on the resolution of itself.
		Iterator<Map.Entry<ParameterizedTypeBinding, ParameterizedTypeBinding>> captureIter = this.captures.entrySet().iterator();
		boolean betaIsInCaptureLhs = false;
		while (captureIter.hasNext()) { // TODO: optimization: consider separate index structure (by IV)
			Entry<ParameterizedTypeBinding, ParameterizedTypeBinding> entry = captureIter.next();
			ParameterizedTypeBinding g = entry.getKey();
			for (int i = 0; i < g.arguments.length; i++) {
				if (TypeBinding.equalsEquals(g.arguments[i], alpha)) {
					// An inference variable α appearing on the left-hand side of a bound of the form G<..., α, ...> = capture(G<...>)
					// depends on the resolution of every other inference variable mentioned in this bound (on both sides of the = sign).
					ParameterizedTypeBinding captured = entry.getValue();
					if (captured.mentionsAny(new TypeBinding[]{beta}, -1/*don't care about index*/))
						return true;
					if (g.mentionsAny(new TypeBinding[]{beta}, i)) // exclude itself
						return true;
				} else if (TypeBinding.equalsEquals(g.arguments[i], beta)) {
					betaIsInCaptureLhs = true;
				}
			}
		}
		if (betaIsInCaptureLhs) { // swap α and β in the rule text to cover "then β depends on the resolution of α"
			ThreeSets sets = this.boundsPerVariable.get(beta);
			if (sets != null && sets.hasDependency(alpha))
				return true;
		} else {
			ThreeSets sets = this.boundsPerVariable.get(alpha);
			if (sets != null && sets.hasDependency(beta))
				return true;
		}
		return false;
	}

	List<Set<InferenceVariable>> computeConnectedComponents(InferenceVariable[] inferenceVariables) {
		// create all dependency edges (as bi-directional):
		Map<InferenceVariable, Set<InferenceVariable>> allEdges = new HashMap<>();
		for (int i = 0; i < inferenceVariables.length; i++) {
			InferenceVariable iv1 = inferenceVariables[i];
			Set<InferenceVariable> targetSet = new LinkedHashSet<InferenceVariable>();
			allEdges.put(iv1, targetSet); // eventually ensures: forall iv in inferenceVariables : allEdges.get(iv) != null
			for (int j = 0; j < i; j++) {
				InferenceVariable iv2 = inferenceVariables[j];
				if (dependsOnResolutionOf(iv1, iv2) || dependsOnResolutionOf(iv2, iv1)) {
					targetSet.add(iv2);
					allEdges.get(iv2).add(iv1);
				}
			}
		}
		// collect all connected IVs into one component:
		Set<InferenceVariable> visited = new LinkedHashSet<>();
		List<Set<InferenceVariable>> allComponents = new ArrayList<>();
		for (InferenceVariable inferenceVariable : inferenceVariables) {
			Set<InferenceVariable> component = new LinkedHashSet<>();
			addConnected(component, inferenceVariable, allEdges, visited);
			if (!component.isEmpty())
				allComponents.add(component);
		}
		return allComponents;
	}

	private void addConnected(Set<InferenceVariable> component, InferenceVariable seed,
			Map<InferenceVariable, Set<InferenceVariable>> allEdges, Set<InferenceVariable> visited)
	{
		if (visited.add(seed)) {
			// add all IVs starting from seed and reachable via any in allEdges:
			component.add(seed);
			for (InferenceVariable next : allEdges.get(seed))
				addConnected(component, next, allEdges, visited);
		}
	}

	// helper for 18.4
	public boolean hasCaptureBound(Set<InferenceVariable> variableSet) {
		Iterator<ParameterizedTypeBinding> captureIter = this.captures.keySet().iterator();
		while (captureIter.hasNext()) {
			ParameterizedTypeBinding g = captureIter.next();
			for (int i = 0; i < g.arguments.length; i++)
				if (variableSet.contains(g.arguments[i]))
					return true;
		}
		return false;
	}

	// helper for 18.4
	public boolean hasOnlyTrivialExceptionBounds(InferenceVariable variable, TypeBinding[] upperBounds) {
		if (upperBounds != null) {
			for (int i = 0; i < upperBounds.length; i++) {
				switch (upperBounds[i].id) {
					case TypeIds.T_JavaLangException:
					case TypeIds.T_JavaLangThrowable:
					case TypeIds.T_JavaLangObject:
						continue;
				}
				return false;
			}
		}
		return true;
	}

	/**
	 * JLS 18.1.3:
	 * Answer all upper bounds for the given inference variable as defined by any bounds in this set.
	 */
	public TypeBinding[] upperBounds(InferenceVariable variable, boolean onlyProper) {
		ThreeSets three = this.boundsPerVariable.get(variable.prototype());
		if (three == null || three.subBounds == null)
			return Binding.NO_TYPES;
		return three.upperBounds(onlyProper, variable);
		// TODO: if !onlyProper: should we also consider ThreeSets.inverseBounds,
		//        or is it safe to rely on incorporation to produce the required bounds?
	}

	/**
	 * JLS 18.1.3:
	 * Answer all lower bounds for the given inference variable as defined by any bounds in this set.
	 */
	TypeBinding[] lowerBounds(InferenceVariable variable, boolean onlyProper) {
		ThreeSets three = this.boundsPerVariable.get(variable.prototype());
		if (three == null || three.superBounds == null)
			return Binding.NO_TYPES;
		return three.lowerBounds(onlyProper, variable);
		// bounds where 'variable' appears at the RHS are not relevant because
		// we're only interested in bounds with a proper type, but if 'variable'
		// appears as RHS the bound is by construction an inference variable,too.
	}

	// debugging:
	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder("Type Bounds:\n"); //$NON-NLS-1$
		TypeBound[] flattened = flatten();
		for (int i = 0; i < flattened.length; i++) {
			buf.append('\t').append(flattened[i].toString()).append('\n');
		}
		buf.append("Capture Bounds:\n"); //$NON-NLS-1$
		Iterator<Map.Entry<ParameterizedTypeBinding,ParameterizedTypeBinding>> captIter = this.captures.entrySet().iterator();
		while (captIter.hasNext()) {
			Entry<ParameterizedTypeBinding, ParameterizedTypeBinding> capt = captIter.next();
			String lhs = String.valueOf(((TypeBinding)capt.getKey()).shortReadableName());
			String rhs = String.valueOf(((TypeBinding)capt.getValue()).shortReadableName());
			buf.append('\t').append(lhs).append(" = capt(").append(rhs).append(")\n"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return buf.toString();
	}

	public TypeBinding findWrapperTypeBound(InferenceVariable variable) {
		ThreeSets three = this.boundsPerVariable.get(variable.prototype());
		if (three == null) return null;
		return three.findSingleWrapperType();
	}
	// this condition is just way too complex to check it in-line:
	public boolean condition18_5_2_bullet_3_3_1(InferenceVariable alpha, TypeBinding targetType) {
		// T is a reference type, but is not a wildcard-parameterized type, and either
		// i) B2 contains a bound of one of the forms α = S or S <: α, where S is a wildcard-parameterized type, or ...
		if (targetType.isBaseType()) return false;
		if (InferenceContext18.parameterizedWithWildcard(targetType) != null) return false;
		ThreeSets ts = this.boundsPerVariable.get(alpha.prototype());
		if (ts == null)
			return false;
		if (ts.sameBounds != null) {
			Iterator<TypeBound> bounds = ts.sameBounds.iterator();
			while (bounds.hasNext()) {
				TypeBound bound = bounds.next();
				if (InferenceContext18.parameterizedWithWildcard(bound.right) != null)
					return true;
			}
		}
		if (ts.superBounds != null) {
			Iterator<TypeBound> bounds = ts.superBounds.iterator();
			while (bounds.hasNext()) {
				TypeBound bound = bounds.next();
				if (InferenceContext18.parameterizedWithWildcard(bound.right) != null)
					return true;
			}
		}
		// ii) B2 contains two bounds of the forms S1 <: α and S2 <: α, where
		//     S1 and S2 have supertypes (4.10) that are two different parameterizations of the same generic class or interface.
		if (ts.superBounds != null) {
			ArrayList<TypeBound> superBounds = new ArrayList<>(ts.superBounds);
			int len = superBounds.size();
			for (int i=0; i<len; i++) {
				TypeBinding s1 = superBounds.get(i).right;
				for (int j=i+1; j<len; j++) {
					TypeBinding s2 = superBounds.get(j).right;
					TypeBinding[] supers = superTypesWithCommonGenericType(s1, s2);
					if (supers != null) {
						/* HashMap<K#8,V#9> and HashMap<K#8,ArrayList<T>> with an instantiation for V9 = ArrayList<T> already in the
						   bound set should not be seen as two different parameterizations of the same generic class or interface.
						   See https://bugs.eclipse.org/bugs/show_bug.cgi?id=432626 for a test that triggers this condition.
						   See https://bugs.openjdk.java.net/browse/JDK-8056092: recommendation is to check for proper types.
						*/
						if (supers[0].isProperType(true) && supers[1].isProperType(true) && !TypeBinding.equalsEquals(supers[0], supers[1]))
							return true;
					}
				}
			}
		}
		return false;
	}

	public boolean condition18_5_2_bullet_3_3_2(InferenceVariable alpha, TypeBinding targetType, InferenceContext18 ctx18) {
		// T is a parameterization of a generic class or interface, G, and
		// B2 contains a bound of one of the forms α = S or S <: α,
		//   where there exists no type of the form G<...> that is a supertype of S, but the raw type G is a supertype of S.
		if (!targetType.isParameterizedType()) return false;
		TypeBinding g = targetType.original();
		ThreeSets ts = this.boundsPerVariable.get(alpha.prototype());
		if (ts == null)
			return false;
		Iterator<TypeBound> boundIterator;
		if (ts.sameBounds != null) {
			boundIterator = ts.sameBounds.iterator();
			while (boundIterator.hasNext()) {
				TypeBound b = boundIterator.next();
				if (superOnlyRaw(g, b.right, ctx18.environment))
					return true;
			}
		}
		if (ts.superBounds != null) {
			boundIterator = ts.superBounds.iterator();
			while (boundIterator.hasNext()) {
				TypeBound b = boundIterator.next();
				if (superOnlyRaw(g, b.right, ctx18.environment))
					return true;
			}
		}
		return false;
	}
	private boolean superOnlyRaw(TypeBinding g, TypeBinding s, LookupEnvironment env) {
		if (s instanceof InferenceVariable)
			return false; // inference has no super types
		final TypeBinding superType = s.findSuperTypeOriginatingFrom(g);
		if (superType != null && !superType.isParameterizedType())
			return s.isCompatibleWith(env.convertToRawType(g, false));
		return false;
	}

	protected TypeBinding[] superTypesWithCommonGenericType(TypeBinding s, TypeBinding t) {
		if (s == null || s.id == TypeIds.T_JavaLangObject || t == null || t.id == TypeIds.T_JavaLangObject)
			return null;
		if (TypeBinding.equalsEquals(s.original(), t.original())) {
			return new TypeBinding[] { s, t };
		}
		TypeBinding tSuper = t.findSuperTypeOriginatingFrom(s);
		if (tSuper != null) {
			return new TypeBinding[] {s, tSuper};
		}
		TypeBinding[] result = superTypesWithCommonGenericType(s.superclass(), t);
		if (result != null)
			return result;
		ReferenceBinding[] superInterfaces = s.superInterfaces();
		if (superInterfaces != null) {
			for (int i = 0; i < superInterfaces.length; i++) {
				result = superTypesWithCommonGenericType(superInterfaces[i], t);
				if (result != null)
					return result;
			}
		}
		return null;
	}

	public TypeBinding getEquivalentOuterVariable(InferenceVariable variable, InferenceVariable[] outerVariables) {
		ThreeSets three = this.boundsPerVariable.get(variable);
		if (three != null) {
			for (TypeBound bound : three.sameBounds) {
				for (InferenceVariable iv : outerVariables)
					if (TypeBinding.equalsEquals(bound.right, iv))
						return iv;
			}
		}
		for (InferenceVariable iv : outerVariables) {
			three = this.boundsPerVariable.get(iv);
			if (three != null && three.sameBounds != null) {
				for (TypeBound bound : three.sameBounds)
					if (TypeBinding.equalsEquals(bound.right, variable))
						return iv;
			}
		}
		return null;
	}
}
