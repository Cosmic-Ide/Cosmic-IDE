/*******************************************************************************
 * Copyright (c) 2005, 2017 IBM Corporation and others.
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
 *     Stephan Herrmann - Contribution for
 *     							bug 349326 - [1.7] new warning for missing try-with-resources
 *     							bug 359362 - FUP of bug 349326: Resource leak on non-Closeable resource
 *								bug 358903 - Filter practically unimportant resource leak warnings
 *								Bug 417295 - [1.8[[null] Massage type annotated null analysis to gel well with deep encoded type bindings.
 *								Bug 400874 - [1.8][compiler] Inference infrastructure should evolve to meet JLS8 18.x (Part G of JSR335 spec)
 *								Bug 423504 - [1.8] Implement "18.5.3 Functional Interface Parameterization Inference"
 *								Bug 426676 - [1.8][compiler] Wrong generic method type inferred from lambda expression
 *								Bug 427411 - [1.8][generics] JDT reports type mismatch when using method that returns generic type
 *								Bug 428019 - [1.8][compiler] Type inference failure with nested generic invocation.
 *								Bug 435962 - [RC2] StackOverFlowError when building
 *								Bug 438458 - [1.8][null] clean up handling of null type annotations wrt type variables
 *								Bug 440759 - [1.8][null] @NonNullByDefault should never affect wildcards and uses of a type variable
 *								Bug 441693 - [1.8][null] Bogus warning for type argument annotated with @NonNull
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.lookup;

import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.Wildcard;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;

/*
 * A wildcard acts as an argument for parameterized types, allowing to
 * abstract parameterized types, e.g. List<String> is not compatible with List<Object>,
 * but compatible with List<?>.
 */
public class WildcardBinding extends ReferenceBinding {

	public ReferenceBinding genericType;
	public int rank;
    public TypeBinding bound; // when unbound denotes the corresponding type variable (so as to retrieve its bound lazily)
    public TypeBinding[] otherBounds; // only positionned by lub computations (if so, #bound is also set) and associated to EXTENDS mode
	char[] genericSignature;
	public int boundKind;
	ReferenceBinding superclass;
	ReferenceBinding[] superInterfaces;
	TypeVariableBinding typeVariable; // corresponding variable
	LookupEnvironment environment;

	/**
	 * When unbound, the bound denotes the corresponding type variable (so as to retrieve its bound lazily)
	 */
	public WildcardBinding(ReferenceBinding genericType, int rank, TypeBinding bound, TypeBinding[] otherBounds, int boundKind, LookupEnvironment environment) {
		this.rank = rank;
	    this.boundKind = boundKind;
		this.modifiers = ClassFileConstants.AccPublic | ExtraCompilerModifiers.AccGenericSignature; // treat wildcard as public
		this.environment = environment;
		initialize(genericType, bound, otherBounds);
		if (genericType instanceof UnresolvedReferenceBinding)
			((UnresolvedReferenceBinding) genericType).addWrapper(this, environment);
		if (bound instanceof UnresolvedReferenceBinding)
			((UnresolvedReferenceBinding) bound).addWrapper(this, environment);
		this.tagBits |=  TagBits.HasUnresolvedTypeVariables; // cleared in resolve()
		this.typeBits = TypeIds.BitUninitialized;
	}

	@Override
	TypeBinding bound() {
		return this.bound;
	}

	@Override
	int boundKind() {
		return this.boundKind;
	}

	public TypeBinding allBounds() {
		if (this.otherBounds == null || this.otherBounds.length == 0)
			return this.bound;
		ReferenceBinding[] allBounds = new ReferenceBinding[this.otherBounds.length+1];
		try {
			allBounds[0] = (ReferenceBinding) this.bound;
			System.arraycopy(this.otherBounds, 0, allBounds, 1, this.otherBounds.length);
		} catch (ClassCastException | ArrayStoreException ase) {
			return this.bound;
		}
		return this.environment.createIntersectionType18(allBounds);
	}


	@Override
	public void setTypeAnnotations(AnnotationBinding[] annotations, boolean evalNullAnnotations) {
		this.tagBits |= TagBits.HasTypeAnnotations;
		if (annotations != null && annotations.length != 0) {
			this.typeAnnotations = annotations;
		}
		if (evalNullAnnotations) {
			evaluateNullAnnotations(null, null);
		}
	}

	/**
	 * evaluate null type annotations and check / copy nullTagBits from bound and typevariable.
	 * may be invoked repeatedly.
	 * @param scope (may be null, if wildcard is null)
	 * @param wildcard (may be null. if non-null, errors are reported and type annotations are dropped from this.bound in case of conflicts.)
	 */
	public void evaluateNullAnnotations(Scope scope, Wildcard wildcard) {
		long nullTagBits = determineNullBitsFromDeclaration(scope, wildcard);
		if (nullTagBits == 0L) {
			TypeVariableBinding typeVariable2 = typeVariable();
			if (typeVariable2 != null) {
				long typeVariableNullTagBits = typeVariable2.tagBits & TagBits.AnnotationNullMASK;
				if (typeVariableNullTagBits != 0L) {
					nullTagBits = typeVariableNullTagBits;
				}
			}
		}
		if (nullTagBits != 0)
			this.tagBits = (this.tagBits & ~TagBits.AnnotationNullMASK) | nullTagBits | TagBits.HasNullTypeAnnotation;
	}

	/**
	 * compute the nullTagBits from type annotations and bound.
	 * @param scope (may be null, if wildcard is null)
	 * @param wildcard (may be null. if non-null, errors are reported and type annotations are dropped from this.bound in case of conflicts.)
	 */
	public long determineNullBitsFromDeclaration(Scope scope, Wildcard wildcard) {
		long nullTagBits = 0L;
		AnnotationBinding [] annotations = this.typeAnnotations;
		if (annotations != null) {
			for (int i = 0, length = annotations.length; i < length; i++) {
				AnnotationBinding annotation = annotations[i];
				if (annotation != null) {
					if (annotation.type.hasNullBit(TypeIds.BitNullableAnnotation)) {
						if ((nullTagBits & TagBits.AnnotationNonNull) == 0) {
							nullTagBits |= TagBits.AnnotationNullable;
						} else {
							if (wildcard != null) {
								Annotation annotation1 = wildcard.findAnnotation(TagBits.AnnotationNullable);
								if (annotation1 != null)
									scope.problemReporter().contradictoryNullAnnotations(annotation1);
							}
						}
					} else if (annotation.type.hasNullBit(TypeIds.BitNonNullAnnotation)) {
						if ((nullTagBits & TagBits.AnnotationNullable) == 0) {
							nullTagBits |= TagBits.AnnotationNonNull;
						} else {
							if (wildcard != null) {
								Annotation annotation1 = wildcard.findAnnotation(TagBits.AnnotationNonNull);
								if (annotation1 != null)
									scope.problemReporter().contradictoryNullAnnotations(annotation1);
							}
						}
					}
				}
			}
		}
		if (this.bound != null && this.bound.isValidBinding()) {
			long boundNullTagBits = this.bound.tagBits & TagBits.AnnotationNullMASK;
			if (boundNullTagBits != 0L) {
				if (this.boundKind == Wildcard.SUPER) {
					if ((boundNullTagBits & TagBits.AnnotationNullable) != 0) {
						if (nullTagBits == 0L) {
							nullTagBits = TagBits.AnnotationNullable;
						} else if (wildcard != null && (nullTagBits & TagBits.AnnotationNonNull) != 0) {
							Annotation annotation = wildcard.bound.findAnnotation(boundNullTagBits);
							if (annotation == null) { // false alarm, implicit annotation is no conflict, but should be removed:
								// may not be reachable, how could we have an implicit @Nullable (not via @NonNullByDefault)?
								TypeBinding newBound = this.bound.withoutToplevelNullAnnotation();
								this.bound = newBound;
								wildcard.bound.resolvedType = newBound;
							} else {
								scope.problemReporter().contradictoryNullAnnotationsOnBounds(annotation, nullTagBits);
							}
						}
					}
				} else {
					if ((boundNullTagBits & TagBits.AnnotationNonNull) != 0) {
						if (nullTagBits == 0L) {
							nullTagBits = TagBits.AnnotationNonNull;
						} else if (wildcard != null && (nullTagBits & TagBits.AnnotationNullable) != 0) {
							Annotation annotation = wildcard.bound.findAnnotation(boundNullTagBits);
							if (annotation == null) { // false alarm, implicit annotation is no conflict, but should be removed:
								TypeBinding newBound = this.bound.withoutToplevelNullAnnotation();
								this.bound = newBound;
								wildcard.bound.resolvedType = newBound;
							} else {
								scope.problemReporter().contradictoryNullAnnotationsOnBounds(annotation, nullTagBits);
							}
						}
					}
					if (nullTagBits == 0L && this.otherBounds != null) {
						for (int i = 0, length = this.otherBounds.length; i < length; i++) {
							if ((this.otherBounds[i].tagBits & TagBits.AnnotationNonNull) != 0) { // can this happen?
								nullTagBits = TagBits.AnnotationNonNull;
								break;
							}
						}
					}
				}
			}
		}
		return nullTagBits;
	}


	@Override
	public ReferenceBinding actualType() {
		return this.genericType;
	}

	@Override
	TypeBinding[] additionalBounds() {
		return this.otherBounds;
	}

	@Override
	public int kind() {
		return this.otherBounds == null ? Binding.WILDCARD_TYPE : Binding.INTERSECTION_TYPE;
	}

	/**
	 * Returns true if the argument type satisfies the wildcard bound(s)
	 */
	public boolean boundCheck(TypeBinding argumentType) {
	    switch (this.boundKind) {
	        case Wildcard.UNBOUND :
	            return true;
	        case Wildcard.EXTENDS :
	            if (!argumentType.isCompatibleWith(this.bound)) return false;
	            // check other bounds (lub scenario)
            	for (int i = 0, length = this.otherBounds == null ? 0 : this.otherBounds.length; i < length; i++) {
            		if (!argumentType.isCompatibleWith(this.otherBounds[i])) return false;
            	}
            	return true;
	        default: // SUPER
	        	// ? super Exception   ok for:  IOException, since it would be ok for (Exception)ioException
	            return argumentType.isCompatibleWith(this.bound);
	    }
    }
	/**
	 * @see org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding#canBeInstantiated()
	 */
	@Override
	public boolean canBeInstantiated() {
		// cannot be asked per construction
		return false;
	}

	/**
	 * @see org.eclipse.jdt.internal.compiler.lookup.TypeBinding#collectMissingTypes(java.util.List)
	 */
	@Override
	public List<TypeBinding> collectMissingTypes(List<TypeBinding> missingTypes) {
		if ((this.tagBits & TagBits.HasMissingType) != 0) {
			missingTypes = this.bound.collectMissingTypes(missingTypes);
		}
		return missingTypes;
	}

	/**
	 * Collect the substitutes into a map for certain type variables inside the receiver type
	 * e.g.   Collection<T>.collectSubstitutes(Collection<List<X>>, Map), will populate Map with: T --> List<X>
	 * Constraints:
	 *   A << F   corresponds to:   F.collectSubstitutes(..., A, ..., CONSTRAINT_EXTENDS (1))
	 *   A = F   corresponds to:      F.collectSubstitutes(..., A, ..., CONSTRAINT_EQUAL (0))
	 *   A >> F   corresponds to:   F.collectSubstitutes(..., A, ..., CONSTRAINT_SUPER (2))
	 */
	@Override
	public void collectSubstitutes(Scope scope, TypeBinding actualType, InferenceContext inferenceContext, int constraint) {

		if ((this.tagBits & TagBits.HasTypeVariable) == 0) return;
		if (actualType == TypeBinding.NULL || actualType.kind() == POLY_TYPE) return;

		if (actualType.isCapture()) {
			CaptureBinding capture = (CaptureBinding) actualType;
			actualType = capture.wildcard;
			// this method should only be called in 1.7- inference, hence we don't expect to see CaptureBinding18 here.
		}

		switch (constraint) {
			case TypeConstants.CONSTRAINT_EXTENDS : // A << F
				switch (this.boundKind) {
					case Wildcard.UNBOUND: // F={?}
//						switch (actualType.kind()) {
//						case Binding.WILDCARD_TYPE :
//							WildcardBinding actualWildcard = (WildcardBinding) actualType;
//							switch(actualWildcard.kind) {
//								case Wildcard.UNBOUND: // A={?} << F={?}  --> 0
//									break;
//								case Wildcard.EXTENDS: // A={? extends V} << F={?} ---> 0
//									break;
//								case Wildcard.SUPER: // A={? super V} << F={?} ---> 0
//									break;
//							}
//							break;
//						case Binding.INTERSECTION_TYPE :// A={? extends V1&...&Vn} << F={?} ---> 0
//							break;
//						default :// A=V << F={?} ---> 0
//							break;
//						}
						break;
					case Wildcard.EXTENDS: // F={? extends U}
						switch(actualType.kind()) {
							case Binding.WILDCARD_TYPE :
								WildcardBinding actualWildcard = (WildcardBinding) actualType;
								switch(actualWildcard.boundKind) {
									case Wildcard.UNBOUND: // A={?} << F={? extends U}  --> 0
										break;
									case Wildcard.EXTENDS: // A={? extends V} << F={? extends U} ---> V << U
										this.bound.collectSubstitutes(scope, actualWildcard.bound, inferenceContext, TypeConstants.CONSTRAINT_EXTENDS);
										break;
									case Wildcard.SUPER: // A={? super V} << F={? extends U} ---> 0
										break;
								}
								break;
							case Binding.INTERSECTION_TYPE : // A={? extends V1&...&Vn} << F={? extends U} ---> V1 << U, ..., Vn << U
								WildcardBinding actualIntersection = (WildcardBinding) actualType;
								this.bound.collectSubstitutes(scope, actualIntersection.bound, inferenceContext, TypeConstants.CONSTRAINT_EXTENDS);
					        	for (int i = 0, length = actualIntersection.otherBounds.length; i < length; i++) {
									this.bound.collectSubstitutes(scope, actualIntersection.otherBounds[i], inferenceContext, TypeConstants.CONSTRAINT_EXTENDS);
					        	}
								break;
							default : // A=V << F={? extends U} ---> V << U
								this.bound.collectSubstitutes(scope, actualType, inferenceContext, TypeConstants.CONSTRAINT_EXTENDS);
								break;
						}
						break;
					case Wildcard.SUPER: // F={? super U}
						switch (actualType.kind()) {
							case Binding.WILDCARD_TYPE :
								WildcardBinding actualWildcard = (WildcardBinding) actualType;
								switch(actualWildcard.boundKind) {
									case Wildcard.UNBOUND: // A={?} << F={? super U}  --> 0
										break;
									case Wildcard.EXTENDS: // A={? extends V} << F={? super U} ---> 0
										break;
									case Wildcard.SUPER: // A={? super V} << F={? super U} ---> 0
										this.bound.collectSubstitutes(scope, actualWildcard.bound, inferenceContext, TypeConstants.CONSTRAINT_SUPER);
							        	for (int i = 0, length = actualWildcard.otherBounds == null ? 0 : actualWildcard.otherBounds.length; i < length; i++) {
											this.bound.collectSubstitutes(scope, actualWildcard.otherBounds[i], inferenceContext, TypeConstants.CONSTRAINT_SUPER);
							        	}
										break;
								}
								break;
							case Binding.INTERSECTION_TYPE : // A={? extends V1&...&Vn} << F={? super U} ---> 0
								break;
							default :// A=V << F={? super U} ---> V >> U
								this.bound.collectSubstitutes(scope, actualType, inferenceContext, TypeConstants.CONSTRAINT_SUPER);
								break;
						}
						break;
				}
				break;
			case TypeConstants.CONSTRAINT_EQUAL : // A == F
				switch (this.boundKind) {
					case Wildcard.UNBOUND: // F={?}
//						switch (actualType.kind()) {
//						case Binding.WILDCARD_TYPE :
//							WildcardBinding actualWildcard = (WildcardBinding) actualType;
//							switch(actualWildcard.kind) {
//								case Wildcard.UNBOUND: // A={?} == F={?}  --> 0
//									break;
//								case Wildcard.EXTENDS: // A={? extends V} == F={?} ---> 0
//									break;
//								case Wildcard.SUPER: // A={? super V} == F={?} ---> 0
//									break;
//							}
//							break;
//						case Binding.INTERSECTION_TYPE :// A={? extends V1&...&Vn} == F={?} ---> 0
//							break;
//						default :// A=V == F={?} ---> 0
//							break;
//						}
						break;
					case Wildcard.EXTENDS: // F={? extends U}
						switch (actualType.kind()) {
							case Binding.WILDCARD_TYPE :
								WildcardBinding actualWildcard = (WildcardBinding) actualType;
								switch(actualWildcard.boundKind) {
									case Wildcard.UNBOUND: // A={?} == F={? extends U}  --> 0
										break;
									case Wildcard.EXTENDS: // A={? extends V} == F={? extends U} ---> V == U
										this.bound.collectSubstitutes(scope, actualWildcard.bound, inferenceContext, TypeConstants.CONSTRAINT_EQUAL);
							        	for (int i = 0, length = actualWildcard.otherBounds == null ? 0 : actualWildcard.otherBounds.length; i < length; i++) {
											this.bound.collectSubstitutes(scope, actualWildcard.otherBounds[i], inferenceContext, TypeConstants.CONSTRAINT_EQUAL);
							        	}
										break;
									case Wildcard.SUPER: // A={? super V} == F={? extends U} ---> 0
										break;
								}
								break;
							case Binding.INTERSECTION_TYPE : // A={? extends V1&...&Vn} == F={? extends U} ---> V1 == U, ..., Vn == U
								WildcardBinding actuaIntersection = (WildcardBinding) actualType;
								this.bound.collectSubstitutes(scope, actuaIntersection.bound, inferenceContext, TypeConstants.CONSTRAINT_EQUAL);
					        	for (int i = 0, length = actuaIntersection.otherBounds == null ? 0 : actuaIntersection.otherBounds.length; i < length; i++) {
									this.bound.collectSubstitutes(scope, actuaIntersection.otherBounds[i], inferenceContext, TypeConstants.CONSTRAINT_EQUAL);
					        	}
								break;
							default : // A=V == F={? extends U} ---> 0
								break;
						}
						break;
					case Wildcard.SUPER: // F={? super U}
						switch (actualType.kind()) {
							case Binding.WILDCARD_TYPE :
								WildcardBinding actualWildcard = (WildcardBinding) actualType;
								switch(actualWildcard.boundKind) {
									case Wildcard.UNBOUND: // A={?} == F={? super U}  --> 0
										break;
									case Wildcard.EXTENDS: // A={? extends V} == F={? super U} ---> 0
										break;
									case Wildcard.SUPER: // A={? super V} == F={? super U} ---> 0
										this.bound.collectSubstitutes(scope, actualWildcard.bound, inferenceContext, TypeConstants.CONSTRAINT_EQUAL);
							        	for (int i = 0, length = actualWildcard.otherBounds == null ? 0 : actualWildcard.otherBounds.length; i < length; i++) {
											this.bound.collectSubstitutes(scope, actualWildcard.otherBounds[i], inferenceContext, TypeConstants.CONSTRAINT_EQUAL);
							        	}
							        	break;
								}
								break;
							case Binding.INTERSECTION_TYPE :  // A={? extends V1&...&Vn} == F={? super U} ---> 0
								break;
							default : // A=V == F={? super U} ---> 0
								break;
						}
						break;
				}
				break;
			case TypeConstants.CONSTRAINT_SUPER : // A >> F
				switch (this.boundKind) {
					case Wildcard.UNBOUND: // F={?}
//						switch (actualType.kind()) {
//						case Binding.WILDCARD_TYPE :
//							WildcardBinding actualWildcard = (WildcardBinding) actualType;
//							switch(actualWildcard.kind) {
//								case Wildcard.UNBOUND: // A={?} >> F={?}  --> 0
//									break;
//								case Wildcard.EXTENDS: // A={? extends V} >> F={?} ---> 0
//									break;
//								case Wildcard.SUPER: // A={? super V} >> F={?} ---> 0
//									break;
//							}
//							break;
//						case Binding.INTERSECTION_TYPE :// A={? extends V1&...&Vn} >> F={?} ---> 0
//							break;
//						default :// A=V >> F={?} ---> 0
//							break;
//						}
						break;
					case Wildcard.EXTENDS: // F={? extends U}
						switch (actualType.kind()) {
							case Binding.WILDCARD_TYPE :
								WildcardBinding actualWildcard = (WildcardBinding) actualType;
								switch(actualWildcard.boundKind) {
									case Wildcard.UNBOUND: // A={?} >> F={? extends U}  --> 0
										break;
									case Wildcard.EXTENDS: // A={? extends V} >> F={? extends U} ---> V >> U
										this.bound.collectSubstitutes(scope, actualWildcard.bound, inferenceContext, TypeConstants.CONSTRAINT_SUPER);
							        	for (int i = 0, length = actualWildcard.otherBounds == null ? 0 : actualWildcard.otherBounds.length; i < length; i++) {
											this.bound.collectSubstitutes(scope, actualWildcard.otherBounds[i], inferenceContext, TypeConstants.CONSTRAINT_SUPER);
							        	}
										break;
									case Wildcard.SUPER: // A={? super V} >> F={? extends U} ---> 0
										break;
								}
								break;
							case Binding.INTERSECTION_TYPE : // A={? extends V1&...&Vn} >> F={? extends U} ---> V1 >> U, ..., Vn >> U
								WildcardBinding actualIntersection = (WildcardBinding) actualType;
								this.bound.collectSubstitutes(scope, actualIntersection.bound, inferenceContext, TypeConstants.CONSTRAINT_SUPER);
					        	for (int i = 0, length = actualIntersection.otherBounds == null ? 0 : actualIntersection.otherBounds.length; i < length; i++) {
									this.bound.collectSubstitutes(scope, actualIntersection.otherBounds[i], inferenceContext, TypeConstants.CONSTRAINT_SUPER);
					        	}
								break;
							default : // A=V == F={? extends U} ---> 0
								break;
						}
						break;
					case Wildcard.SUPER: // F={? super U}
						switch (actualType.kind()) {
							case Binding.WILDCARD_TYPE :
								WildcardBinding actualWildcard = (WildcardBinding) actualType;
								switch(actualWildcard.boundKind) {
									case Wildcard.UNBOUND: // A={?} >> F={? super U}  --> 0
										break;
									case Wildcard.EXTENDS: // A={? extends V} >> F={? super U} ---> 0
										break;
									case Wildcard.SUPER: // A={? super V} >> F={? super U} ---> V >> U
										this.bound.collectSubstitutes(scope, actualWildcard.bound, inferenceContext, TypeConstants.CONSTRAINT_SUPER);
							        	for (int i = 0, length = actualWildcard.otherBounds == null ? 0 : actualWildcard.otherBounds.length; i < length; i++) {
											this.bound.collectSubstitutes(scope, actualWildcard.otherBounds[i], inferenceContext, TypeConstants.CONSTRAINT_SUPER);
							        	}
							        	break;
								}
								break;
							case Binding.INTERSECTION_TYPE :  // A={? extends V1&...&Vn} >> F={? super U} ---> 0
								break;
							default : // A=V >> F={? super U} ---> 0
								break;
						}
						break;
				}
				break;
		}
	}

	/*
	 * genericTypeKey {rank}*|+|- [boundKey]
	 * p.X<T> { X<?> ... } --> Lp/X<TT;>;{0}*
	 */
	@Override
	public char[] computeUniqueKey(boolean isLeaf) {
		char[] genericTypeKey = this.genericType.computeUniqueKey(false/*not a leaf*/);
		char[] wildCardKey;
		// We now encode the rank also in the binding key - https://bugs.eclipse.org/bugs/show_bug.cgi?id=234609
		char[] rankComponent = ('{' + String.valueOf(this.rank) + '}').toCharArray();
        switch (this.boundKind) {
            case Wildcard.UNBOUND :
                wildCardKey = TypeConstants.WILDCARD_STAR;
                break;
            case Wildcard.EXTENDS :
                wildCardKey = CharOperation.concat(TypeConstants.WILDCARD_PLUS, this.bound.computeUniqueKey(false/*not a leaf*/));
                break;
			default: // SUPER
			    wildCardKey = CharOperation.concat(TypeConstants.WILDCARD_MINUS, this.bound.computeUniqueKey(false/*not a leaf*/));
				break;
        }
		return CharOperation.concat(genericTypeKey, rankComponent, wildCardKey);
    }



	/**
	 * @see org.eclipse.jdt.internal.compiler.lookup.TypeBinding#constantPoolName()
	 */
	@Override
	public char[] constantPoolName() {
		return erasure().constantPoolName();
	}

	@Override
	public TypeBinding clone(TypeBinding immaterial) {
		return new WildcardBinding(this.genericType, this.rank, this.bound, this.otherBounds, this.boundKind, this.environment);
	}

	@Override
	public String annotatedDebugName() {
		StringBuilder buffer = new StringBuilder(16);
		AnnotationBinding [] annotations = getTypeAnnotations();
		for (int i = 0, length = annotations == null ? 0 : annotations.length; i < length; i++) {
			buffer.append(annotations[i]);
			buffer.append(' ');
		}
		switch (this.boundKind) {
            case Wildcard.UNBOUND :
                return buffer.append(TypeConstants.WILDCARD_NAME).toString();
            case Wildcard.EXTENDS :
            	if (this.otherBounds == null)
                	return buffer.append(CharOperation.concat(TypeConstants.WILDCARD_NAME, TypeConstants.WILDCARD_EXTENDS, this.bound.annotatedDebugName().toCharArray())).toString();
            	buffer.append(this.bound.annotatedDebugName());
            	for (int i = 0, length = this.otherBounds.length; i < length; i++) {
            		buffer.append(" & ").append(this.otherBounds[i].annotatedDebugName()); //$NON-NLS-1$
            	}
            	return buffer.toString();
			default: // SUPER
			    return buffer.append(CharOperation.concat(TypeConstants.WILDCARD_NAME, TypeConstants.WILDCARD_SUPER, this.bound.annotatedDebugName().toCharArray())).toString();
        }
	}
	/**
	 * @see org.eclipse.jdt.internal.compiler.lookup.TypeBinding#debugName()
	 */
	@Override
	public String debugName() {
	    return toString();
	}

    @Override
	public TypeBinding erasure() {
    	if (this.otherBounds == null) {
	    	if (this.boundKind == Wildcard.EXTENDS)
		        return this.bound.erasure();
			TypeVariableBinding var = typeVariable();
			if (var != null)
				return var.erasure();
		    return this.genericType; // if typeVariable() == null, then its inconsistent & return this.genericType to avoid NPE case
    	}
    	// intersection type
    	return this.bound.id == TypeIds.T_JavaLangObject
    		? this.otherBounds[0].erasure()  // use first explicit bound to improve stackmap
    		: this.bound.erasure();
    }

    @Override
	public char[] genericTypeSignature() {
        if (this.genericSignature == null) {
            switch (this.boundKind) {
                case Wildcard.UNBOUND :
                    this.genericSignature = TypeConstants.WILDCARD_STAR;
                    break;
                case Wildcard.EXTENDS :
                    this.genericSignature = CharOperation.concat(TypeConstants.WILDCARD_PLUS, this.bound.genericTypeSignature());
					break;
				default: // SUPER
				    this.genericSignature = CharOperation.concat(TypeConstants.WILDCARD_MINUS, this.bound.genericTypeSignature());
            }
        }
        return this.genericSignature;
    }

	@Override
	public int hashCode() {
		return this.genericType.hashCode();
	}

	@Override
	public boolean hasTypeBit(int bit) {
		if (this.typeBits == TypeIds.BitUninitialized) {
			// initialize from upper bounds
			this.typeBits = 0;
			if (this.superclass != null && this.superclass.hasTypeBit(~TypeIds.BitUninitialized))
				this.typeBits |= (this.superclass.typeBits & TypeIds.InheritableBits);
			if (this.superInterfaces != null)
				for (int i = 0, l = this.superInterfaces.length; i < l; i++)
					if (this.superInterfaces[i].hasTypeBit(~TypeIds.BitUninitialized))
						this.typeBits |= (this.superInterfaces[i].typeBits & TypeIds.InheritableBits);
		}
		return (this.typeBits & bit) != 0;
	}

	void initialize(ReferenceBinding someGenericType, TypeBinding someBound, TypeBinding[] someOtherBounds) {
		this.genericType = someGenericType;
		this.bound = someBound;
		this.otherBounds = someOtherBounds;
		if (someGenericType != null) {
			this.fPackage = someGenericType.getPackage();
		}
		if (someBound != null) {
			this.tagBits |= someBound.tagBits & (TagBits.HasTypeVariable | TagBits.HasMissingType | TagBits.ContainsNestedTypeReferences |
					TagBits.HasNullTypeAnnotation | TagBits.HasCapturedWildcard);
		}
		if (someOtherBounds != null) {
			for (int i = 0, max = someOtherBounds.length; i < max; i++) {
				TypeBinding someOtherBound = someOtherBounds[i];
				this.tagBits |= someOtherBound.tagBits & (TagBits.ContainsNestedTypeReferences | TagBits.HasNullTypeAnnotation | TagBits.HasCapturedWildcard);
			}
		}
	}

	/**
     * @see org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding#isSuperclassOf(org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding)
     */
    @Override
	public boolean isSuperclassOf(ReferenceBinding otherType) {
        if (this.boundKind == Wildcard.SUPER) {
            if (this.bound instanceof ReferenceBinding) {
                return ((ReferenceBinding) this.bound).isSuperclassOf(otherType);
            } else { // array bound
                return otherType.id == TypeIds.T_JavaLangObject;
            }
        }
        return false;
    }

    @Override
	public boolean isIntersectionType() {
    	return this.otherBounds != null;
    }

    @Override
    public ReferenceBinding[] getIntersectingTypes() {
    	if (isIntersectionType()) {
    		ReferenceBinding[] allBounds = new ReferenceBinding[this.otherBounds.length+1];
    		try {
    			allBounds[0] = (ReferenceBinding) this.bound;
    			System.arraycopy(this.otherBounds, 0, allBounds, 1, this.otherBounds.length);
    		} catch (ClassCastException | ArrayStoreException ase) {
    			return null;
    		}
    		return allBounds;
    	}
    	return null;
    }

	@Override
	public boolean isHierarchyConnected() {
		return this.superclass != null && this.superInterfaces != null;
	}

	// to prevent infinite recursion when inspecting recursive generics:
	boolean inRecursiveFunction = false;

	@Override
	public boolean enterRecursiveFunction() {
		if (this.inRecursiveFunction)
			return false;
		this.inRecursiveFunction = true;
		return true;
	}
	@Override
	public void exitRecursiveFunction() {
		this.inRecursiveFunction = false;
	}

	@Override
	public boolean isProperType(boolean admitCapture18) {
		if (this.inRecursiveFunction)
			return true;
		this.inRecursiveFunction = true;
		try {
			if (this.bound != null && !this.bound.isProperType(admitCapture18))
				return false;
			if (this.superclass != null && !this.superclass.isProperType(admitCapture18))
				return false;
			if (this.superInterfaces != null)
				for (int i = 0, l = this.superInterfaces.length; i < l; i++)
					if (!this.superInterfaces[i].isProperType(admitCapture18))
						return false;
			return true;
		} finally {
			this.inRecursiveFunction = false;
		}
	}

	@Override
	TypeBinding substituteInferenceVariable(InferenceVariable var, TypeBinding substituteType) {
		boolean haveSubstitution = false;
		TypeBinding currentBound = this.bound;
		if (currentBound != null) {
			currentBound = currentBound.substituteInferenceVariable(var, substituteType);
			haveSubstitution |= TypeBinding.notEquals(currentBound, this.bound);
		}
		TypeBinding[] currentOtherBounds = null;
		if (this.otherBounds != null) {
			int length = this.otherBounds.length;
			if (haveSubstitution)
				System.arraycopy(this.otherBounds, 0, currentOtherBounds=new ReferenceBinding[length], 0, length);
			for (int i = 0; i < length; i++) {
				TypeBinding currentOtherBound = this.otherBounds[i];
				if (currentOtherBound != null) {
					currentOtherBound = currentOtherBound.substituteInferenceVariable(var, substituteType);
					if (TypeBinding.notEquals(currentOtherBound, this.otherBounds[i])) {
						if (currentOtherBounds == null)
							System.arraycopy(this.otherBounds, 0, currentOtherBounds=new ReferenceBinding[length], 0, length);
						currentOtherBounds[i] = currentOtherBound;
					}
				}
			}
		}
		haveSubstitution |= currentOtherBounds != null;
		if (haveSubstitution) {
			return this.environment.createWildcard(this.genericType, this.rank, currentBound, currentOtherBounds, this.boundKind);
		}
		return this;
	}

	@Override
	public boolean isUnboundWildcard() {
	    return this.boundKind == Wildcard.UNBOUND;
	}

	@Override
	public boolean isWildcard() {
	    return true;
	}

	@Override
	int rank() {
		return this.rank;
	}

    @Override
	public char[] readableName() {
        switch (this.boundKind) {
            case Wildcard.UNBOUND :
                return TypeConstants.WILDCARD_NAME;
            case Wildcard.EXTENDS :
            	if (this.otherBounds == null)
	                return CharOperation.concat(TypeConstants.WILDCARD_NAME, TypeConstants.WILDCARD_EXTENDS, this.bound.readableName());
            	StringBuilder buffer = new StringBuilder(10);
            	buffer.append(this.bound.readableName());
            	for (int i = 0, length = this.otherBounds.length; i < length; i++) {
            		buffer.append('&').append(this.otherBounds[i].readableName());
            	}
            	int length;
				char[] result = new char[length = buffer.length()];
				buffer.getChars(0, length, result, 0);
				return result;
			default: // SUPER
			    return CharOperation.concat(TypeConstants.WILDCARD_NAME, TypeConstants.WILDCARD_SUPER, this.bound.readableName());
        }
    }

    @Override
	public char[] nullAnnotatedReadableName(CompilerOptions options, boolean shortNames) {
    	StringBuffer buffer = new StringBuffer(10);
    	appendNullAnnotation(buffer, options);
        switch (this.boundKind) {
            case Wildcard.UNBOUND :
                buffer.append(TypeConstants.WILDCARD_NAME);
                break;
            case Wildcard.EXTENDS :
            	if (this.otherBounds == null) {
            		buffer.append(TypeConstants.WILDCARD_NAME).append(TypeConstants.WILDCARD_EXTENDS);
            		buffer.append(this.bound.nullAnnotatedReadableName(options, shortNames));
            	} else {
	            	buffer.append(this.bound.nullAnnotatedReadableName(options, shortNames));
	            	for (int i = 0, length = this.otherBounds.length; i < length; i++) {
	            		buffer.append('&').append(this.otherBounds[i].nullAnnotatedReadableName(options, shortNames));
	            	}
            	}
            	break;
			default: // SUPER
			    buffer.append(TypeConstants.WILDCARD_NAME).append(TypeConstants.WILDCARD_SUPER).append(this.bound.nullAnnotatedReadableName(options, shortNames));
        }
        int length;
        char[] result = new char[length = buffer.length()];
        buffer.getChars(0, length, result, 0);
        return result;
    }

	ReferenceBinding resolve() {
		if ((this.tagBits & TagBits.HasUnresolvedTypeVariables) == 0)
			return this;

		this.tagBits &= ~TagBits.HasUnresolvedTypeVariables;
		BinaryTypeBinding.resolveType(this.genericType, this.environment, false /* no raw conversion */);

		switch(this.boundKind) {
			case Wildcard.EXTENDS :
				TypeBinding resolveType = BinaryTypeBinding.resolveType(this.bound, this.environment, true /* raw conversion */);
				this.bound = resolveType;
				this.tagBits |= resolveType.tagBits & TagBits.ContainsNestedTypeReferences | TagBits.HasCapturedWildcard;
				for (int i = 0, length = this.otherBounds == null ? 0 : this.otherBounds.length; i < length; i++) {
					resolveType = BinaryTypeBinding.resolveType(this.otherBounds[i], this.environment, true /* raw conversion */);
					this.otherBounds[i]= resolveType;
					this.tagBits |= resolveType.tagBits & TagBits.ContainsNestedTypeReferences | TagBits.HasCapturedWildcard;
				}
				break;
			case Wildcard.SUPER :
				resolveType = BinaryTypeBinding.resolveType(this.bound, this.environment, true /* raw conversion */);
				this.bound = resolveType;
				this.tagBits |= resolveType.tagBits & TagBits.ContainsNestedTypeReferences | TagBits.HasCapturedWildcard;
				break;
			case Wildcard.UNBOUND :
		}
		if (this.environment.usesNullTypeAnnotations()) {
			evaluateNullAnnotations(null, null);
		}
		return this;
	}

    @Override
	public char[] shortReadableName() {
        switch (this.boundKind) {
            case Wildcard.UNBOUND :
                return TypeConstants.WILDCARD_NAME;
            case Wildcard.EXTENDS :
            	if (this.otherBounds == null)
	                return CharOperation.concat(TypeConstants.WILDCARD_NAME, TypeConstants.WILDCARD_EXTENDS, this.bound.shortReadableName());
            	StringBuilder buffer = new StringBuilder(10);
            	buffer.append(this.bound.shortReadableName());
            	for (int i = 0, length = this.otherBounds.length; i < length; i++) {
            		buffer.append('&').append(this.otherBounds[i].shortReadableName());
            	}
            	int length;
				char[] result = new char[length = buffer.length()];
				buffer.getChars(0, length, result, 0);
				return result;
			default: // SUPER
			    return CharOperation.concat(TypeConstants.WILDCARD_NAME, TypeConstants.WILDCARD_SUPER, this.bound.shortReadableName());
        }
    }

    /**
     * @see org.eclipse.jdt.internal.compiler.lookup.TypeBinding#signature()
     */
    @Override
	public char[] signature() {
     	// should not be called directly on a wildcard; signature should only be asked on
    	// original methods or type erasures (which cannot denote wildcards at first level)
		if (this.signature == null) {
	        switch (this.boundKind) {
	            case Wildcard.EXTENDS :
	                return this.bound.signature();
				default: // SUPER | UNBOUND
				    return typeVariable().signature();
	        }
		}
		return this.signature;
    }

    @Override
	public char[] sourceName() {
        switch (this.boundKind) {
            case Wildcard.UNBOUND :
                return TypeConstants.WILDCARD_NAME;
            case Wildcard.EXTENDS :
                return CharOperation.concat(TypeConstants.WILDCARD_NAME, TypeConstants.WILDCARD_EXTENDS, this.bound.sourceName());
			default: // SUPER
			    return CharOperation.concat(TypeConstants.WILDCARD_NAME, TypeConstants.WILDCARD_SUPER, this.bound.sourceName());
        }
    }

    @Override
	public ReferenceBinding superclass() {
		if (this.superclass == null) {
			TypeBinding superType = null;
			if (this.boundKind == Wildcard.EXTENDS && !this.bound.isInterface()) {
				superType = this.bound;
			} else {
				TypeVariableBinding variable = typeVariable();
				if (variable != null) superType = variable.firstBound;
			}
			this.superclass = superType instanceof ReferenceBinding && !superType.isInterface()
				? (ReferenceBinding) superType
				: this.environment.getResolvedJavaBaseType(TypeConstants.JAVA_LANG_OBJECT, null);
		}

		return this.superclass;
    }

    @Override
	public ReferenceBinding[] superInterfaces() {
        if (this.superInterfaces == null) {
        	if (typeVariable() != null) {
        		this.superInterfaces = this.typeVariable.superInterfaces();
        	} else {
        		this.superInterfaces = Binding.NO_SUPERINTERFACES;
        	}
			if (this.boundKind == Wildcard.EXTENDS) {
				if (this.bound.isInterface()) {
					// augment super interfaces with the wildcard bound
					int length = this.superInterfaces.length;
					System.arraycopy(this.superInterfaces, 0, this.superInterfaces = new ReferenceBinding[length+1], 1, length);
					this.superInterfaces[0] = (ReferenceBinding) this.bound; // make bound first
				}
				if (this.otherBounds != null) {
					// augment super interfaces with the wildcard otherBounds (interfaces per construction)
					int length = this.superInterfaces.length;
					int otherLength = this.otherBounds.length;
					System.arraycopy(this.superInterfaces, 0, this.superInterfaces = new ReferenceBinding[length+otherLength], 0, length);
					for (int i = 0; i < otherLength; i++) {
						this.superInterfaces[length+i] = (ReferenceBinding) this.otherBounds[i];
					}
				}
			}
        }
        return this.superInterfaces;
    }

	@Override
	public void swapUnresolved(UnresolvedReferenceBinding unresolvedType, ReferenceBinding resolvedType, LookupEnvironment env) {
		boolean affected = false;
		if (this.genericType == unresolvedType) { //$IDENTITY-COMPARISON$
			this.genericType = resolvedType; // no raw conversion
			affected = true;
		}
		if (this.bound == unresolvedType) { //$IDENTITY-COMPARISON$
			this.bound = env.convertUnresolvedBinaryToRawType(resolvedType);
			affected = true;
		}
		if (this.otherBounds != null) {
			for (int i = 0, length = this.otherBounds.length; i < length; i++) {
				if (this.otherBounds[i] == unresolvedType) { //$IDENTITY-COMPARISON$
					this.otherBounds[i] = env.convertUnresolvedBinaryToRawType(resolvedType);
					affected = true;
				}
			}
		}
		if (affected)
			initialize(this.genericType, this.bound, this.otherBounds);
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		if (this.hasTypeAnnotations())
			return annotatedDebugName();
        switch (this.boundKind) {
            case Wildcard.UNBOUND :
                return new String(TypeConstants.WILDCARD_NAME);
            case Wildcard.EXTENDS :
            	if (this.otherBounds == null)
                	return new String(CharOperation.concat(TypeConstants.WILDCARD_NAME, TypeConstants.WILDCARD_EXTENDS, this.bound.debugName().toCharArray()));
            	StringBuilder buffer = new StringBuilder(this.bound.debugName());
            	for (int i = 0, length = this.otherBounds.length; i < length; i++) {
            		buffer.append('&').append(this.otherBounds[i].debugName());
            	}
            	return buffer.toString();
			default: // SUPER
			    return new String(CharOperation.concat(TypeConstants.WILDCARD_NAME, TypeConstants.WILDCARD_SUPER, this.bound.debugName().toCharArray()));
        }
	}
	/**
	 * Returns associated type variable, or null in case of inconsistency
	 */
	public TypeVariableBinding typeVariable() {
		if (this.typeVariable == null) {
			TypeVariableBinding[] typeVariables = this.genericType.typeVariables();
			if (this.rank < typeVariables.length)
				this.typeVariable = typeVariables[this.rank];
		}
		return this.typeVariable;
	}

	@Override
	public TypeBinding unannotated() {
		return this.hasTypeAnnotations() ? this.environment.getUnannotatedType(this) : this;
	}

	@Override
	public TypeBinding withoutToplevelNullAnnotation() {
		if (!hasNullTypeAnnotations())
			return this;
		AnnotationBinding[] newAnnotations = this.environment.filterNullTypeAnnotations(getTypeAnnotations());
		return this.environment.createWildcard(this.genericType, this.rank, this.bound, this.otherBounds, this.boundKind, newAnnotations);
	}
	@Override
	public TypeBinding uncapture(Scope scope) {
		if ((this.tagBits & TagBits.HasCapturedWildcard) == 0)
			return this;
		TypeBinding freeBound = this.bound != null ? this.bound.uncapture(scope) : null;
		int length = 0;
		TypeBinding [] freeOtherBounds = this.otherBounds == null ? null : new TypeBinding[length = this.otherBounds.length];
		for (int i = 0; i < length; i++) {
			freeOtherBounds[i] = this.otherBounds[i] == null ? null : this.otherBounds[i].uncapture(scope);
		}
		return scope.environment().createWildcard(this.genericType, this.rank, freeBound, freeOtherBounds, this.boundKind, getTypeAnnotations());
	}
	@Override
	void collectInferenceVariables(Set<InferenceVariable> variables) {
		if (this.bound != null)
			this.bound.collectInferenceVariables(variables);
		if (this.otherBounds != null)
			for (int i = 0, length = this.otherBounds.length; i < length; i++)
				this.otherBounds[i].collectInferenceVariables(variables);
	}
	@Override
	public boolean mentionsAny(TypeBinding[] parameters, int idx) {
		if (this.inRecursiveFunction)
			return false;
		this.inRecursiveFunction = true;
		try {
			if (super.mentionsAny(parameters, idx))
				return true;
			if (this.bound != null && 	this.bound.mentionsAny(parameters, -1))
				return true;
			if (this.otherBounds != null) {
				for (int i = 0, length = this.otherBounds.length; i < length; i++)
					if (this.otherBounds[i].mentionsAny(parameters, -1))
						return true;
			}
		} finally {
			this.inRecursiveFunction = false;
		}
		return false;
	}

	@Override
	public boolean acceptsNonNullDefault() {
		return false;
	}

	@Override
	public long updateTagBits() {
		if (!this.inRecursiveFunction) {
			this.inRecursiveFunction = true;
			try {
				if (this.bound != null)
					this.tagBits |= this.bound.updateTagBits();
				if (this.otherBounds != null) {
					for (int i = 0, length = this.otherBounds.length; i < length; i++)
						this.tagBits |= this.otherBounds[i].updateTagBits();
				}
			} finally {
				this.inRecursiveFunction = false;
			}
		}
		return super.updateTagBits();
	}
}
