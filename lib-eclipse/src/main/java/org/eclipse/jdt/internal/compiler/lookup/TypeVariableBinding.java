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
 *     Stephan Herrmann <stephan@cs.tu-berlin.de> - Contributions for
 *     							bug 282152 - [1.5][compiler] Generics code rejected by Eclipse but accepted by javac
 *     							bug 349326 - [1.7] new warning for missing try-with-resources
 *     							bug 359362 - FUP of bug 349326: Resource leak on non-Closeable resource
 *								bug 358903 - Filter practically unimportant resource leak warnings
 *								bug 395002 - Self bound generic class doesn't resolve bounds properly for wildcards for certain parametrisation.
 *								bug 392384 - [1.8][compiler][null] Restore nullness info from type annotations in class files
 *								Bug 415043 - [1.8][null] Follow-up re null type annotations after bug 392099
 *								Bug 417295 - [1.8[[null] Massage type annotated null analysis to gel well with deep encoded type bindings.
 *								Bug 400874 - [1.8][compiler] Inference infrastructure should evolve to meet JLS8 18.x (Part G of JSR335 spec)
 *								Bug 426792 - [1.8][inference][impl] generify new type inference engine
 *								Bug 428019 - [1.8][compiler] Type inference failure with nested generic invocation.
 *								Bug 429384 - [1.8][null] implement conformance rules for null-annotated lower / upper type bounds
 *								Bug 431269 - [1.8][compiler][null] StackOverflow in nullAnnotatedReadableName
 *								Bug 431408 - Java 8 (1.8) generics bug
 *								Bug 435962 - [RC2] StackOverFlowError when building
 *								Bug 438458 - [1.8][null] clean up handling of null type annotations wrt type variables
 *								Bug 438250 - [1.8][null] NPE trying to report bogus null annotation conflict
 *								Bug 438179 - [1.8][null] 'Contradictory null annotations' error on type variable with explicit null-annotation.
 *								Bug 440143 - [1.8][null] one more case of contradictory null annotations regarding type variables
 *								Bug 440759 - [1.8][null] @NonNullByDefault should never affect wildcards and uses of a type variable
 *								Bug 441693 - [1.8][null] Bogus warning for type argument annotated with @NonNull
 *								Bug 456497 - [1.8][null] during inference nullness from target type is lost against weaker hint from applicability analysis
 *								Bug 456459 - Discrepancy between Eclipse compiler and javac - Enums, interfaces, and generics
 *								Bug 456487 - [1.8][null] @Nullable type variant of @NonNull-constrained type parameter causes grief
 *								Bug 462790 - [null] NPE in Expression.computeConversion()
 *								Bug 456532 - [1.8][null] ReferenceBinding.appendNullAnnotation() includes phantom annotations in error messages
 *     Jesper S Møller <jesper@selskabet.org>  - Contributions for bug 381345 : [1.8] Take care of the Java 8 major version
 *								Bug 527554 - [18.3] Compiler support for JEP 286 Local-Variable Type
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.lookup;

import java.util.Arrays;
import java.util.Set;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.NullAnnotationMatching;
import org.eclipse.jdt.internal.compiler.ast.NullAnnotationMatching.CheckMode;
import org.eclipse.jdt.internal.compiler.ast.TypeParameter;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.eclipse.jdt.internal.compiler.ast.Wildcard;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.lookup.TypeConstants.BoundCheckStatus;

/**
 * Binding for a type parameter, held by source/binary type or method.
 */
public class TypeVariableBinding extends ReferenceBinding {

	public Binding declaringElement; // binding of declaring type or method
	public int rank; // declaration rank, can be used to match variable in parameterized type

	/**
	 * Denote the first explicit (binding) bound amongst the supertypes (from declaration in source)
	 * If no superclass was specified, then it denotes the first superinterface, or null if none was specified.
	 */
	public TypeBinding firstBound;             // MUST NOT be modified directly, use setter !

	// actual resolved variable supertypes (if no superclass bound, then associated to Object)
	public ReferenceBinding superclass;        // MUST NOT be modified directly, use setter !
	public ReferenceBinding[] superInterfaces; // MUST NOT be modified directly, use setter !
	public char[] genericTypeSignature;
	LookupEnvironment environment;

	public TypeVariableBinding(char[] sourceName, Binding declaringElement, int rank, LookupEnvironment environment) {
		this.sourceName = sourceName;
		this.declaringElement = declaringElement;
		this.rank = rank;
		this.modifiers = ClassFileConstants.AccPublic | ExtraCompilerModifiers.AccGenericSignature; // treat type var as public
		this.tagBits |= TagBits.HasTypeVariable;
		this.environment = environment;
		this.typeBits = TypeIds.BitUninitialized;
		computeId(environment);
	}

	// for subclass CaptureBinding
	protected TypeVariableBinding(char[] sourceName, LookupEnvironment environment) {
		this.sourceName = sourceName;
		this.modifiers = ClassFileConstants.AccPublic | ExtraCompilerModifiers.AccGenericSignature; // treat type var as public
		this.tagBits |= TagBits.HasTypeVariable;
		this.environment = environment;
		this.typeBits = TypeIds.BitUninitialized;
		// don't yet compute the ID!
	}

	public TypeVariableBinding(TypeVariableBinding prototype) {
		super(prototype);
		this.declaringElement = prototype.declaringElement;
		this.rank = prototype.rank;
		this.firstBound = prototype.firstBound;
		this.superclass = prototype.superclass;
		if (prototype.superInterfaces != null) {
			int len = prototype.superInterfaces.length;
			if (len > 0)
				System.arraycopy(prototype.superInterfaces, 0, this.superInterfaces = new ReferenceBinding[len], 0, len);
			else
				this.superInterfaces = Binding.NO_SUPERINTERFACES;
		}
		this.genericTypeSignature = prototype.genericTypeSignature;
		this.environment = prototype.environment;
		prototype.tagBits |= TagBits.HasAnnotatedVariants;
		this.tagBits &= ~TagBits.HasAnnotatedVariants;
	}

	/**
	 * Returns true if the argument type satisfies all bounds of the type parameter
	 * @param location if non-null this may be used for reporting errors relating to null type annotations (if enabled)
	 */
	public TypeConstants.BoundCheckStatus boundCheck(Substitution substitution, TypeBinding argumentType, Scope scope, ASTNode location) {
		TypeConstants.BoundCheckStatus code = internalBoundCheck(substitution, argumentType, scope, location);
		if (code == BoundCheckStatus.MISMATCH) {
			if (argumentType instanceof TypeVariableBinding && scope != null) {
				TypeBinding bound = ((TypeVariableBinding)argumentType).firstBound;
				if (bound instanceof ParameterizedTypeBinding) {
					BoundCheckStatus code2 = boundCheck(substitution, bound.capture(scope, -1, -1), scope, location); // no capture position needed as this capture will never escape this context
					return code.betterOf(code2);
				}
			}
		}
		return code;
	}
	private TypeConstants.BoundCheckStatus internalBoundCheck(Substitution substitution, TypeBinding argumentType, Scope scope, ASTNode location) {
		if (argumentType == TypeBinding.NULL || TypeBinding.equalsEquals(argumentType, this)) {
			return BoundCheckStatus.OK;
		}
		boolean hasSubstitution = substitution != null;
		if (!(argumentType instanceof ReferenceBinding || argumentType.isArrayType()))
			return BoundCheckStatus.MISMATCH;
		// special case for re-entrant source types (selection, code assist, etc)...
		// can request additional types during hierarchy walk that are found as source types that also 'need' to connect their hierarchy
		if (this.superclass == null)
			return BoundCheckStatus.OK;

		BoundCheckStatus nullStatus = BoundCheckStatus.OK;
		boolean checkNullAnnotations = scope.environment().usesNullTypeAnnotations() && (location == null || (location.bits & ASTNode.InsideJavadoc) == 0);

		if (argumentType.kind() == Binding.WILDCARD_TYPE) {
			WildcardBinding wildcard = (WildcardBinding) argumentType;
			switch(wildcard.boundKind) {
				case Wildcard.EXTENDS :
					boolean checkedAsOK = false;
					TypeBinding wildcardBound = wildcard.bound;
					if (TypeBinding.equalsEquals(wildcardBound, this))
						checkedAsOK = true; // OK per JLS, but may require null checking below
					boolean isArrayBound = wildcardBound.isArrayType();
					if (!wildcardBound.isInterface()) {
						TypeBinding substitutedSuperType = hasSubstitution ? Scope.substitute(substitution, this.superclass) : this.superclass;
						if (!checkedAsOK) {
							if (substitutedSuperType.id != TypeIds.T_JavaLangObject) {
								if (isArrayBound) {
									if (!wildcardBound.isCompatibleWith(substitutedSuperType, scope))
										return BoundCheckStatus.MISMATCH;
								} else {
									TypeBinding match = wildcardBound.findSuperTypeOriginatingFrom(substitutedSuperType);
									if (match != null) {
										if (substitutedSuperType.isProvablyDistinct(match)) {
											return BoundCheckStatus.MISMATCH;
										}
									} else {
										match =  substitutedSuperType.findSuperTypeOriginatingFrom(wildcardBound);
										if (match != null) {
											if (match.isProvablyDistinct(wildcardBound)) {
												return BoundCheckStatus.MISMATCH;
											}
										} else {
											if (denotesRelevantSuperClass(wildcardBound) && denotesRelevantSuperClass(substitutedSuperType)) {
												// non-object real superclass should have produced a valid 'match' above
												return BoundCheckStatus.MISMATCH;
											}
											// not fully spec-ed in JLS, but based on email communication (2017-09-13):
											// (a) bound check should apply capture
											// (b) capture applies glb
											// (c) and then the glb should be checked for well-formedness (see Scope.isMalformedPair() - this part missing in JLS).
											// Since we don't do (a), nor (b) for this case, we just directly proceed to (b) here.
											// For (a) see ParameterizedTypeBinding.boundCheck() - comment added as of this commit
											// for (b) see CaptureBinding.initializeBounds()  - comment added as of this commit
											if (Scope.greaterLowerBound(new TypeBinding[] {substitutedSuperType, wildcardBound}, scope, this.environment) == null) {
												return BoundCheckStatus.MISMATCH;
											}
										}
									}
								}
							}
						}
						if (checkNullAnnotations && argumentType.hasNullTypeAnnotations()) {
							nullStatus = nullBoundCheck(scope, argumentType, substitutedSuperType, substitution, location, nullStatus);
						}
					}
					boolean mustImplement = isArrayBound || ((ReferenceBinding)wildcardBound).isFinal();
					for (int i = 0, length = this.superInterfaces.length; i < length; i++) {
						TypeBinding substitutedSuperType = hasSubstitution ? Scope.substitute(substitution, this.superInterfaces[i]) : this.superInterfaces[i];
						if (!checkedAsOK) {
							if (isArrayBound) {
								if (!wildcardBound.isCompatibleWith(substitutedSuperType, scope))
										return BoundCheckStatus.MISMATCH;
							} else {
								TypeBinding match = wildcardBound.findSuperTypeOriginatingFrom(substitutedSuperType);
								if (match != null) {
									if (substitutedSuperType.isProvablyDistinct(match)) {
										return BoundCheckStatus.MISMATCH;
									}
								} else if (mustImplement) {
										return BoundCheckStatus.MISMATCH; // cannot be extended further to satisfy missing bounds
								}
							}
						}
						if (checkNullAnnotations && argumentType.hasNullTypeAnnotations()) {
							nullStatus = nullBoundCheck(scope, argumentType, substitutedSuperType, substitution, location, nullStatus);
						}
					}
					if (nullStatus != null)
						return nullStatus;
					break;

				case Wildcard.SUPER :
					// if the wildcard is lower-bounded by a type variable that has no relevant upper bound there's nothing to check here (bug 282152):
					if (wildcard.bound.isTypeVariable() && ((TypeVariableBinding)wildcard.bound).superclass.id == TypeIds.T_JavaLangObject) {
						return nullBoundCheck(scope, argumentType, null, substitution, location, nullStatus);
					} else {
						TypeBinding bound = wildcard.bound;
						if (checkNullAnnotations && this.environment.containsNullTypeAnnotation(wildcard.typeAnnotations))
							bound = this.environment.createAnnotatedType(bound.withoutToplevelNullAnnotation(), wildcard.getTypeAnnotations());
						BoundCheckStatus status = boundCheck(substitution, bound, scope, null); // do not report null-errors against the tweaked bound ...
						if (status == BoundCheckStatus.NULL_PROBLEM && location != null)
							scope.problemReporter().nullityMismatchTypeArgument(this, wildcard, location); // ... but against the wildcard
						return status;
					}
				case Wildcard.UNBOUND :
					if (checkNullAnnotations && argumentType.hasNullTypeAnnotations()) {
						return nullBoundCheck(scope, argumentType, null, substitution, location, nullStatus);
					}
					break;
			}
			return BoundCheckStatus.OK;
		}
		boolean unchecked = false;
		if (this.superclass.id != TypeIds.T_JavaLangObject) {
			TypeBinding substitutedSuperType = hasSubstitution ? Scope.substitute(substitution, this.superclass) : this.superclass;
	    	if (TypeBinding.notEquals(substitutedSuperType, argumentType)) {
				if (!argumentType.isCompatibleWith(substitutedSuperType, scope)) {
				    return BoundCheckStatus.MISMATCH;
				}
				TypeBinding match = argumentType.findSuperTypeOriginatingFrom(substitutedSuperType);
				if (match != null){
					// Enum#RAW is not a substitute for <E extends Enum<E>> (86838)
					if (match.isRawType() && substitutedSuperType.isBoundParameterizedType())
						unchecked = true;
				}
	    	}
			if (checkNullAnnotations) {
				nullStatus = nullBoundCheck(scope, argumentType, substitutedSuperType, substitution, location, nullStatus);
			}
		}
	    for (int i = 0, length = this.superInterfaces.length; i < length; i++) {
			TypeBinding substitutedSuperType = hasSubstitution ? Scope.substitute(substitution, this.superInterfaces[i]) : this.superInterfaces[i];
	    	if (TypeBinding.notEquals(substitutedSuperType, argumentType)) {
				if (!argumentType.isCompatibleWith(substitutedSuperType, scope)) {
				    return BoundCheckStatus.MISMATCH;
				}
				TypeBinding match = argumentType.findSuperTypeOriginatingFrom(substitutedSuperType);
				if (match != null){
					// Enum#RAW is not a substitute for <E extends Enum<E>> (86838)
					if (match.isRawType() && substitutedSuperType.isBoundParameterizedType())
						unchecked = true;
				}
	    	}
			if (checkNullAnnotations) {
				nullStatus = nullBoundCheck(scope, argumentType, substitutedSuperType, substitution, location, nullStatus);
			}
	    }
	    if (checkNullAnnotations && nullStatus != BoundCheckStatus.NULL_PROBLEM) {
	    	long nullBits = this.tagBits & TagBits.AnnotationNullMASK;
	    	if (nullBits != 0 && nullBits != (argumentType.tagBits & TagBits.AnnotationNullMASK)) {
				if (location != null)
					scope.problemReporter().nullityMismatchTypeArgument(this, argumentType, location);
				nullStatus = BoundCheckStatus.NULL_PROBLEM;
			}
	    }
	    return unchecked ? BoundCheckStatus.UNCHECKED : nullStatus != null ? nullStatus : BoundCheckStatus.OK;
	}

	private BoundCheckStatus nullBoundCheck(Scope scope, TypeBinding argumentType, TypeBinding substitutedSuperType, Substitution substitution, ASTNode location, BoundCheckStatus previousStatus) {
		NullAnnotationMatching status = NullAnnotationMatching.analyse(this, argumentType, substitutedSuperType, substitution, -1, null, CheckMode.BOUND_CHECK);
		if (status.isAnyMismatch() && !status.isAnnotatedToUnannotated()) {
			if (location != null)
				scope.problemReporter().nullityMismatchTypeArgument(this, argumentType, location);
			return BoundCheckStatus.NULL_PROBLEM;
		}
		return previousStatus;
	}

	boolean denotesRelevantSuperClass(TypeBinding type) {
		if (!type.isTypeVariable() && !type.isInterface() && type.id != TypeIds.T_JavaLangObject)
			return true;
		ReferenceBinding aSuperClass = type.superclass();
		return aSuperClass != null && aSuperClass.id != TypeIds.T_JavaLangObject && !aSuperClass.isTypeVariable();
	}

	public int boundsCount() {
		if (this.firstBound == null)
			return 0;
		if (this.firstBound.isInterface())
			return this.superInterfaces.length; // only interface bounds
		return this.superInterfaces.length + 1; // class or array type isn't contained in superInterfaces
	}

	/**
	 * @see org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding#canBeInstantiated()
	 */
	@Override
	public boolean canBeInstantiated() {
		return false;
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

		//	only infer for type params of the generic method
		if (this.declaringElement != inferenceContext.genericMethod) return;

		// cannot infer anything from a null type
		switch (actualType.kind()) {
			case Binding.BASE_TYPE :
				if (actualType == TypeBinding.NULL) return;
				TypeBinding boxedType = scope.environment().computeBoxingType(actualType);
				if (boxedType == actualType) return; //$IDENTITY-COMPARISON$
				actualType = boxedType;
				break;
			case Binding.POLY_TYPE: // cannot steer inference, only learn from it.
			case Binding.WILDCARD_TYPE :
				return; // wildcards are not true type expressions (JLS 15.12.2.7, p.453 2nd discussion)
		}

		// reverse constraint, to reflect variable on rhs:   A << T --> T >: A
		int variableConstraint;
		switch(constraint) {
			case TypeConstants.CONSTRAINT_EQUAL :
				variableConstraint = TypeConstants.CONSTRAINT_EQUAL;
				break;
			case TypeConstants.CONSTRAINT_EXTENDS :
				variableConstraint = TypeConstants.CONSTRAINT_SUPER;
				break;
			default:
			//case CONSTRAINT_SUPER :
				variableConstraint =TypeConstants.CONSTRAINT_EXTENDS;
				break;
		}
		inferenceContext.recordSubstitute(this, actualType, variableConstraint);
	}

	/*
	 * declaringUniqueKey : genericTypeSignature
	 * p.X<T> { ... } --> Lp/X;:TT;
	 * p.X { <T> void foo() {...} } --> Lp/X;.foo()V:TT;
	 */
	@Override
	public char[] computeUniqueKey(boolean isLeaf) {
		StringBuilder buffer = new StringBuilder();
		Binding declaring = this.declaringElement;
		if (!isLeaf && declaring.kind() == Binding.METHOD) { // see https://bugs.eclipse.org/bugs/show_bug.cgi?id=97902
			MethodBinding methodBinding = (MethodBinding) declaring;
			ReferenceBinding declaringClass = methodBinding.declaringClass;
			buffer.append(declaringClass.computeUniqueKey(false/*not a leaf*/));
			buffer.append(':');
			MethodBinding[] methods = declaringClass.methods();
			if (methods != null)
				for (int i = 0, length = methods.length; i < length; i++) {
					MethodBinding binding = methods[i];
					if (binding == methodBinding) {
						buffer.append(i);
						break;
					}
				}
		} else {
			buffer.append(declaring.computeUniqueKey(false/*not a leaf*/));
			buffer.append(':');
		}
		buffer.append(genericTypeSignature());
		int length = buffer.length();
		char[] uniqueKey = new char[length];
		buffer.getChars(0, length, uniqueKey, 0);
		return uniqueKey;
	}
	@Override
	public char[] constantPoolName() { /* java/lang/Object */
	    if (this.firstBound != null) {
			return this.firstBound.constantPoolName();
	    }
	    return this.superclass.constantPoolName(); // java/lang/Object
	}

	@Override
	public TypeBinding clone(TypeBinding enclosingType) {
		return new TypeVariableBinding(this);
	}
	@Override
	public String annotatedDebugName() {
		StringBuilder buffer = new StringBuilder(10);
		buffer.append(super.annotatedDebugName());
		if (!this.inRecursiveFunction) {
			this.inRecursiveFunction = true;
			try {
				if (this.superclass != null && TypeBinding.equalsEquals(this.firstBound, this.superclass)) {
					buffer.append(" extends ").append(this.superclass.annotatedDebugName()); //$NON-NLS-1$
				}
				if (this.superInterfaces != null && this.superInterfaces != Binding.NO_SUPERINTERFACES) {
					if (TypeBinding.notEquals(this.firstBound, this.superclass)) {
						buffer.append(" extends "); //$NON-NLS-1$
					}
					for (int i = 0, length = this.superInterfaces.length; i < length; i++) {
						if (i > 0 || TypeBinding.equalsEquals(this.firstBound, this.superclass)) {
							buffer.append(" & "); //$NON-NLS-1$
						}
						buffer.append(this.superInterfaces[i].annotatedDebugName());
					}
				}
			} finally {
				this.inRecursiveFunction = false;
			}
		}
		return buffer.toString();
	}
	/**
	 * @see org.eclipse.jdt.internal.compiler.lookup.TypeBinding#debugName()
	 */
	@Override
	public String debugName() {
		if (this.hasTypeAnnotations())
			return super.annotatedDebugName();
	    return new String(this.sourceName);
	}
	@Override
	public TypeBinding erasure() {
	    if (this.firstBound != null) {
			return this.firstBound.erasure();
	    }
	    return this.superclass; // java/lang/Object
	}
	/**
	 * T::Ljava/util/Map;:Ljava/io/Serializable;
	 * T:LY<TT;>
	 */
	public char[] genericSignature() {
	    StringBuilder sig = new StringBuilder(10);
	    sig.append(this.sourceName).append(':');
	   	int interfaceLength = this.superInterfaces == null ? 0 : this.superInterfaces.length;
	    if (interfaceLength == 0 || TypeBinding.equalsEquals(this.firstBound, this.superclass)) {
	    	if (this.superclass != null)
		        sig.append(this.superclass.genericTypeSignature());
	    }
		for (int i = 0; i < interfaceLength; i++) {
		    sig.append(':').append(this.superInterfaces[i].genericTypeSignature());
		}
		int sigLength = sig.length();
		char[] genericSignature = new char[sigLength];
		sig.getChars(0, sigLength, genericSignature, 0);
		return genericSignature;
	}
	/**
	 * T::Ljava/util/Map;:Ljava/io/Serializable;
	 * T:LY<TT;>
	 */
	@Override
	public char[] genericTypeSignature() {
	    if (this.genericTypeSignature != null) return this.genericTypeSignature;
		return this.genericTypeSignature = CharOperation.concat('T', this.sourceName, ';');
	}

	/**
	 * Compute the initial type bounds for one inference variable as per JLS8 sect 18.1.3.
	 */
	TypeBound[] getTypeBounds(InferenceVariable variable, InferenceSubstitution theta) {
		int n = boundsCount();
        if (n == 0)
        	return NO_TYPE_BOUNDS;
        TypeBound[] bounds = new TypeBound[n];
        int idx = 0;
        if (!this.firstBound.isInterface())
        	bounds[idx++] = TypeBound.createBoundOrDependency(theta, this.firstBound, variable);
        for (int i = 0; i < this.superInterfaces.length; i++)
			bounds[idx++] = TypeBound.createBoundOrDependency(theta, this.superInterfaces[i], variable);
        return bounds;
	}

	boolean hasOnlyRawBounds() {
		if (this.superclass != null && TypeBinding.equalsEquals(this.firstBound, this.superclass))
			if (!this.superclass.isRawType())
				return false;

		if (this.superInterfaces != null)
			for (int i = 0, l = this.superInterfaces.length; i < l; i++)
		   		if (!this.superInterfaces[i].isRawType())
		   			return false;

		return true;
	}

	@Override
	public boolean hasTypeBit(int bit) {
		if (this.typeBits == TypeIds.BitUninitialized) {
			// initialize from bounds
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

	/**
	 * Returns true if the type variable is directly bound to a given type
	 */
	public boolean isErasureBoundTo(TypeBinding type) {
		if (TypeBinding.equalsEquals(this.superclass.erasure(), type))
			return true;
		for (int i = 0, length = this.superInterfaces.length; i < length; i++) {
			if (TypeBinding.equalsEquals(this.superInterfaces[i].erasure(), type))
				return true;
		}
		return false;
	}

	@Override
	public boolean isHierarchyConnected() {
		return (this.modifiers & ExtraCompilerModifiers.AccUnresolved) == 0;
	}

	/**
	 * Returns true if the 2 variables are playing exact same role: they have
	 * the same bounds, providing one is substituted with the other: <T1 extends
	 * List<T1>> is interchangeable with <T2 extends List<T2>>.
	 */
	public boolean isInterchangeableWith(TypeVariableBinding otherVariable, Substitution substitute) {
		if (TypeBinding.equalsEquals(this, otherVariable))
			return true;
		int length = this.superInterfaces.length;
		if (length != otherVariable.superInterfaces.length)
			return false;

		if (TypeBinding.notEquals(this.superclass, Scope.substitute(substitute, otherVariable.superclass)))
			return false;

		next : for (int i = 0; i < length; i++) {
			TypeBinding superType = Scope.substitute(substitute, otherVariable.superInterfaces[i]);
			for (int j = 0; j < length; j++)
				if (TypeBinding.equalsEquals(superType, this.superInterfaces[j]))
					continue next;
			return false; // not a match
		}
		return true;
	}

	@Override
	public boolean isSubtypeOf(TypeBinding other, boolean simulatingBugJDK8026527) {
		if (isSubTypeOfRTL(other))
			return true;
		if (this.firstBound != null && this.firstBound.isSubtypeOf(other, simulatingBugJDK8026527))
			return true;
		if (this.superclass != null && this.superclass.isSubtypeOf(other, simulatingBugJDK8026527))
			return true;
		if (this.superInterfaces != null)
			for (int i = 0, l = this.superInterfaces.length; i < l; i++)
		   		if (this.superInterfaces[i].isSubtypeOf(other, false))
					return true;
		return other.id == TypeIds.T_JavaLangObject;
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

	// to prevent infinite recursion when inspecting recursive generics:
	boolean inRecursiveProjectionFunction = false;

	public boolean enterRecursiveProjectionFunction() {
		if (this.inRecursiveProjectionFunction)
			return false;
		this.inRecursiveProjectionFunction = true;
		return true;
	}
	public void exitRecursiveProjectionFunction() {
		this.inRecursiveProjectionFunction = false;
	}

	@Override
	public boolean isProperType(boolean admitCapture18) {
		// handle recursive calls:
		if (this.inRecursiveFunction) // be optimistic, since this node is not an inference variable
			return true;

		this.inRecursiveFunction = true;
		try {
			if (this.superclass != null && !this.superclass.isProperType(admitCapture18)) {
				return false;
			}
			if (this.superInterfaces != null)
				for (int i = 0, l = this.superInterfaces.length; i < l; i++)
			   		if (!this.superInterfaces[i].isProperType(admitCapture18)) {
						return false;
					}
			return true;
		} finally {
			this.inRecursiveFunction = false;
		}
	}

	@Override
	TypeBinding substituteInferenceVariable(InferenceVariable var, TypeBinding substituteType) {
		if (this.inRecursiveFunction) return this;
		this.inRecursiveFunction = true;
		try {
			boolean haveSubstitution = false;
			ReferenceBinding currentSuperclass = this.superclass;
			if (currentSuperclass != null) {
				currentSuperclass = (ReferenceBinding) currentSuperclass.substituteInferenceVariable(var, substituteType);
				haveSubstitution |= TypeBinding.notEquals(currentSuperclass, this.superclass);
			}
			ReferenceBinding[] currentSuperInterfaces = null;
			if (this.superInterfaces != null) {
				int length = this.superInterfaces.length;
				if (haveSubstitution)
					System.arraycopy(this.superInterfaces, 0, currentSuperInterfaces=new ReferenceBinding[length], 0, length);
				for (int i = 0; i < length; i++) {
					ReferenceBinding currentSuperInterface = this.superInterfaces[i];
					if (currentSuperInterface != null) {
						currentSuperInterface = (ReferenceBinding) currentSuperInterface.substituteInferenceVariable(var, substituteType);
						if (TypeBinding.notEquals(currentSuperInterface, this.superInterfaces[i])) {
							if (currentSuperInterfaces == null)
								System.arraycopy(this.superInterfaces, 0, currentSuperInterfaces=new ReferenceBinding[length], 0, length);
							currentSuperInterfaces[i] = currentSuperInterface;
							haveSubstitution = true;
						}
					}
				}
			}
			if (haveSubstitution) {
				TypeVariableBinding newVar = new TypeVariableBinding(this.sourceName, this.declaringElement, this.rank, this.environment);
				newVar.superclass = currentSuperclass;
				newVar.superInterfaces = currentSuperInterfaces;
				newVar.tagBits = this.tagBits;
				return newVar;
			}
			return this;
		} finally {
			this.inRecursiveFunction = false;
		}
	}

	/**
	 * Returns true if the type was declared as a type variable
	 */
	@Override
	public boolean isTypeVariable() {
	    return true;
	}

//	/**
//	 * Returns the original type variable for a given variable.
//	 * Only different from receiver for type variables of generic methods of parameterized types
//	 * e.g. X<U> {   <V1 extends U> U foo(V1)   } --> X<String> { <V2 extends String> String foo(V2)  }
//	 *         and V2.original() --> V1
//	 */
//	public TypeVariableBinding original() {
//		if (this.declaringElement.kind() == Binding.METHOD) {
//			MethodBinding originalMethod = ((MethodBinding)this.declaringElement).original();
//			if (originalMethod != this.declaringElement) {
//				return originalMethod.typeVariables[this.rank];
//			}
//		} else {
//			ReferenceBinding originalType = (ReferenceBinding)((ReferenceBinding)this.declaringElement).erasure();
//			if (originalType != this.declaringElement) {
//				return originalType.typeVariables()[this.rank];
//			}
//		}
//		return this;
//	}

	@Override
	public int kind() {
		return Binding.TYPE_PARAMETER;
	}

	@Override
	public boolean mentionsAny(TypeBinding[] parameters, int idx) {
		if (this.inRecursiveFunction)
			return false; // nothing seen
		this.inRecursiveFunction = true;
		try {
			if (super.mentionsAny(parameters, idx))
				return true;
			if (this.superclass != null && this.superclass.mentionsAny(parameters, idx))
				return true;
			if (this.superInterfaces != null)
				for (int j = 0; j < this.superInterfaces.length; j++) {
					if (this.superInterfaces[j].mentionsAny(parameters, idx))
						return true;
			}
			return false;
		} finally {
			this.inRecursiveFunction = false;
		}
	}

	@Override
	void collectInferenceVariables(Set<InferenceVariable> variables) {
		if (this.inRecursiveFunction)
			return; // nothing seen
		this.inRecursiveFunction = true;
		try {
			if (this.superclass != null)
				this.superclass.collectInferenceVariables(variables);
			if (this.superInterfaces != null)
				for (int j = 0; j < this.superInterfaces.length; j++) {
					this.superInterfaces[j].collectInferenceVariables(variables);
			}
		} finally {
			this.inRecursiveFunction = false;
		}
	}

	public TypeBinding[] otherUpperBounds() {
		if (this.firstBound == null)
			return Binding.NO_TYPES;
		if (TypeBinding.equalsEquals(this.firstBound, this.superclass))
			return this.superInterfaces;
		int otherLength = this.superInterfaces.length - 1;
		if (otherLength > 0) {
			TypeBinding[] otherBounds;
			System.arraycopy(this.superInterfaces, 1, otherBounds = new TypeBinding[otherLength], 0, otherLength);
			return otherBounds;
		}
		return Binding.NO_TYPES;
	}

	/**
     * @see org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding#readableName()
     */
    @Override
	public char[] readableName() {
        return this.sourceName;
    }
	ReferenceBinding resolve() {
		if ((this.modifiers & ExtraCompilerModifiers.AccUnresolved) == 0)
			return this;

		long nullTagBits = this.tagBits & TagBits.AnnotationNullMASK;

		TypeBinding oldSuperclass = this.superclass, oldFirstInterface = null;
		if (this.superclass != null) {
			ReferenceBinding resolveType = (ReferenceBinding) BinaryTypeBinding.resolveType(this.superclass, this.environment, true /* raw conversion */);
			this.tagBits |= resolveType.tagBits & TagBits.ContainsNestedTypeReferences;
			long superNullTagBits = resolveType.tagBits & TagBits.AnnotationNullMASK;
			if (superNullTagBits != 0L) {
				if (nullTagBits == 0L) {
					if ((superNullTagBits & TagBits.AnnotationNonNull) != 0) {
						nullTagBits = superNullTagBits;
					}
				} else {
//					System.err.println("TODO(stephan): report proper error: conflict binary TypeVariable vs. first bound");
				}
			}
			this.setSuperClass(resolveType);
		}
		ReferenceBinding[] interfaces = this.superInterfaces;
		int length;
		if ((length = interfaces.length) != 0) {
			oldFirstInterface = interfaces[0];
			for (int i = length; --i >= 0;) {
				ReferenceBinding resolveType = (ReferenceBinding) BinaryTypeBinding.resolveType(interfaces[i], this.environment, true /* raw conversion */);
				this.tagBits |= resolveType.tagBits & TagBits.ContainsNestedTypeReferences;
				long superNullTagBits = resolveType.tagBits & TagBits.AnnotationNullMASK;
				if (superNullTagBits != 0L) {
					if (nullTagBits == 0L) {
						if ((superNullTagBits & TagBits.AnnotationNonNull) != 0) {
							nullTagBits = superNullTagBits;
						}
					} else {
//						System.err.println("TODO(stephan): report proper error: conflict binary TypeVariable vs. bound "+i);
					}
				}
				interfaces[i] = resolveType;
			}
		}
		if (nullTagBits != 0)
			this.tagBits |= nullTagBits | TagBits.HasNullTypeAnnotation;

		// refresh the firstBound in case it changed
		if (this.firstBound != null) {
			if (TypeBinding.equalsEquals(this.firstBound, oldSuperclass)) {
				this.setFirstBound(this.superclass);
			} else if (TypeBinding.equalsEquals(this.firstBound, oldFirstInterface)) {
				this.setFirstBound(interfaces[0]);
			}
		}
		this.modifiers &= ~ExtraCompilerModifiers.AccUnresolved;
		return this;
	}

	@Override
	public void setTypeAnnotations(AnnotationBinding[] annotations, boolean evalNullAnnotations) {
		if (getClass() == TypeVariableBinding.class) {
			// TVB only: if the declaration itself carries type annotations,
			// make sure TypeSystem will still have an unannotated variant at position 0, to answer getUnannotated()
			// (in this case the unannotated type is never explicit in source code, that's why we need this charade).
			this.environment.typeSystem.forceRegisterAsDerived(this);
		} else {
			this.environment.getUnannotatedType(this); // exposes original TVB/capture to type system for id stamping purposes.
		}
		super.setTypeAnnotations(annotations, evalNullAnnotations);
	}
	/**
     * @see org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding#shortReadableName()
     */
    @Override
	public char[] shortReadableName() {
        return readableName();
    }
	@Override
	public ReferenceBinding superclass() {
		return this.superclass;
	}

	@Override
	public ReferenceBinding[] superInterfaces() {
		return this.superInterfaces;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		if (this.hasTypeAnnotations())
			return annotatedDebugName();
		StringBuilder buffer = new StringBuilder(10);
		buffer.append('<').append(this.sourceName);//.append('[').append(this.rank).append(']');
		if (this.superclass != null && TypeBinding.equalsEquals(this.firstBound, this.superclass)) {
		    buffer.append(" extends ").append(this.superclass.debugName()); //$NON-NLS-1$
		}
		if (this.superInterfaces != null && this.superInterfaces != Binding.NO_SUPERINTERFACES) {
		   if (TypeBinding.notEquals(this.firstBound, this.superclass)) {
		        buffer.append(" extends "); //$NON-NLS-1$
	        }
		    for (int i = 0, length = this.superInterfaces.length; i < length; i++) {
		        if (i > 0 || TypeBinding.equalsEquals(this.firstBound, this.superclass)) {
		            buffer.append(" & "); //$NON-NLS-1$
		        }
				buffer.append(this.superInterfaces[i].debugName());
			}
		}
		buffer.append('>');
		return buffer.toString();
	}

	@Override
	public char[] nullAnnotatedReadableName(CompilerOptions options, boolean shortNames) {
	    StringBuffer nameBuffer = new StringBuffer(10);
		appendNullAnnotation(nameBuffer, options);
		nameBuffer.append(this.sourceName());
		if (!this.inRecursiveFunction) {
			this.inRecursiveFunction = true;
			try {
				if (this.superclass != null && TypeBinding.equalsEquals(this.firstBound, this.superclass)) {
					nameBuffer.append(" extends ").append(this.superclass.nullAnnotatedReadableName(options, shortNames)); //$NON-NLS-1$
				}
				if (this.superInterfaces != null && this.superInterfaces != Binding.NO_SUPERINTERFACES) {
					if (TypeBinding.notEquals(this.firstBound, this.superclass)) {
						nameBuffer.append(" extends "); //$NON-NLS-1$
					}
					for (int i = 0, length = this.superInterfaces.length; i < length; i++) {
						if (i > 0 || TypeBinding.equalsEquals(this.firstBound, this.superclass)) {
							nameBuffer.append(" & "); //$NON-NLS-1$
						}
						nameBuffer.append(this.superInterfaces[i].nullAnnotatedReadableName(options, shortNames));
					}
				}
			} finally {
				this.inRecursiveFunction = false;
			}
		}
		int nameLength = nameBuffer.length();
		char[] readableName = new char[nameLength];
		nameBuffer.getChars(0, nameLength, readableName, 0);
	    return readableName;
	}

	@Override
	protected void appendNullAnnotation(StringBuffer nameBuffer, CompilerOptions options) {
		int oldSize = nameBuffer.length();
		super.appendNullAnnotation(nameBuffer, options);
		if (oldSize == nameBuffer.length()) { // nothing appended in super.appendNullAnnotation()?
			if (hasNullTypeAnnotations()) {
				// see if the prototype has null type annotations:
				TypeVariableBinding[] typeVariables = null;
				if (this.declaringElement instanceof ReferenceBinding) {
					typeVariables = ((ReferenceBinding) this.declaringElement).typeVariables();
				} else if (this.declaringElement instanceof MethodBinding) {
					typeVariables = ((MethodBinding) this.declaringElement).typeVariables();
				}
				if (typeVariables != null && typeVariables.length > this.rank) {
					TypeVariableBinding prototype = typeVariables[this.rank];
					if (prototype != this)//$IDENTITY-COMPARISON$
						prototype.appendNullAnnotation(nameBuffer, options);
				}
			}
		}
	}

	@Override
	public TypeBinding unannotated() {
		return this.hasTypeAnnotations() ? this.environment.getUnannotatedType(this) : this;
	}

	@Override
	public TypeBinding withoutToplevelNullAnnotation() {
		if (!hasNullTypeAnnotations())
			return this;
		TypeBinding unannotated = this.environment.getUnannotatedType(this);
		AnnotationBinding[] newAnnotations = this.environment.filterNullTypeAnnotations(this.typeAnnotations);
		if (newAnnotations.length > 0)
			return this.environment.createAnnotatedType(unannotated, newAnnotations);
		return unannotated;
	}
	/**
	 * Upper bound doesn't perform erasure
	 */
	public TypeBinding upperBound() {
		if (this.firstBound != null) {
			return this.firstBound;
		}
		return this.superclass; // java/lang/Object
	}

	public TypeBinding[] allUpperBounds() {
		if (this.superclass == null)
			return this.superInterfaces;
		if (this.superInterfaces == null || this.superInterfaces.length == 0)
			return new TypeBinding[] { this.superclass };
		int nInterfaces = this.superInterfaces.length;
		TypeBinding[] all = Arrays.copyOf(this.superInterfaces, nInterfaces+1);
		all[nInterfaces] = this.superclass;
		return all;
	}

	public void evaluateNullAnnotations(Scope scope, TypeParameter parameter) {
		long nullTagBits = NullAnnotationMatching.validNullTagBits(this.tagBits);
		if (this.firstBound != null && this.firstBound.isValidBinding()) {
			long superNullTagBits = NullAnnotationMatching.validNullTagBits(this.firstBound.tagBits);
			if (superNullTagBits != 0L) {
				if (nullTagBits == 0L) {
					if ((superNullTagBits & TagBits.AnnotationNonNull) != 0) {
						nullTagBits = superNullTagBits;
					}
				} else if (superNullTagBits != nullTagBits) {
					if(parameter != null)
						this.firstBound = nullMismatchOnBound(parameter, this.firstBound, superNullTagBits, nullTagBits, scope);
				}
			}
		}
		ReferenceBinding[] interfaces = this.superInterfaces;
		int length;
		if (interfaces != null && (length = interfaces.length) != 0) {
			for (int i = length; --i >= 0;) {
				ReferenceBinding resolveType = interfaces[i];
				long superNullTagBits = NullAnnotationMatching.validNullTagBits(resolveType.tagBits);
				if (superNullTagBits != 0L) {
					if (nullTagBits == 0L) {
						if ((superNullTagBits & TagBits.AnnotationNonNull) != 0) {
							nullTagBits = superNullTagBits;
						}
					} else if (superNullTagBits != nullTagBits) {
						if(parameter != null)
							interfaces[i] = (ReferenceBinding) nullMismatchOnBound(parameter, resolveType, superNullTagBits, nullTagBits, scope);
					}
				}
			}
		}
		if (nullTagBits != 0)
			this.tagBits |= nullTagBits | TagBits.HasNullTypeAnnotation;
	}
	private TypeBinding nullMismatchOnBound(TypeParameter parameter, TypeBinding boundType, long superNullTagBits, long nullTagBits, Scope scope) {
		// not finding bound should be considered a compiler bug
		TypeReference bound = findBound(boundType, parameter);
		Annotation ann = bound.findAnnotation(superNullTagBits);
		if (ann != null) {
			// explicit annotation: error
			scope.problemReporter().contradictoryNullAnnotationsOnBounds(ann, nullTagBits);
			this.tagBits &= ~TagBits.AnnotationNullMASK;
		} else {
			// implicit annotation: let the new one override
			return boundType.withoutToplevelNullAnnotation();
		}
		return boundType;
	}
	private TypeReference findBound(TypeBinding bound, TypeParameter parameter) {
		if (parameter.type != null && TypeBinding.equalsEquals(parameter.type.resolvedType, bound))
			return parameter.type;
		TypeReference[] bounds = parameter.bounds;
		if (bounds != null) {
			for (int i = 0; i < bounds.length; i++) {
				if (TypeBinding.equalsEquals(bounds[i].resolvedType, bound))
					return bounds[i];
			}
		}
		return null;
	}

	/* An annotated type variable use differs from its declaration exactly in its annotations and in nothing else.
	   Propagate writes to all annotated variants so the clones evolve along.
	*/
	public TypeBinding setFirstBound(TypeBinding firstBound) {
		this.firstBound = firstBound;
		if ((this.tagBits & TagBits.HasAnnotatedVariants) != 0) {
			TypeBinding [] annotatedTypes = getDerivedTypesForDeferredInitialization();
			for (int i = 0, length = annotatedTypes == null ? 0 : annotatedTypes.length; i < length; i++) {
				TypeVariableBinding annotatedType = (TypeVariableBinding) annotatedTypes[i];
				annotatedType.firstBound = firstBound;
			}
		}
		if (firstBound != null && firstBound.hasNullTypeAnnotations())
			this.tagBits |= TagBits.HasNullTypeAnnotation;
		return firstBound;
	}
	/* An annotated type variable use differs from its declaration exactly in its annotations and in nothing else.
	   Propagate writes to all annotated variants so the clones evolve along.
	*/
	public ReferenceBinding setSuperClass(ReferenceBinding superclass) {
		this.superclass = superclass;
		if ((this.tagBits & TagBits.HasAnnotatedVariants) != 0) {
			TypeBinding [] annotatedTypes = getDerivedTypesForDeferredInitialization();
			for (int i = 0, length = annotatedTypes == null ? 0 : annotatedTypes.length; i < length; i++) {
				TypeVariableBinding annotatedType = (TypeVariableBinding) annotatedTypes[i];
				annotatedType.superclass = superclass;
			}
		}
		return superclass;
	}
	/* An annotated type variable use differs from its declaration exactly in its annotations and in nothing else.
	   Propagate writes to all annotated variants so the clones evolve along.
	*/
	public ReferenceBinding [] setSuperInterfaces(ReferenceBinding[] superInterfaces) {
		this.superInterfaces = superInterfaces;
		if ((this.tagBits & TagBits.HasAnnotatedVariants) != 0) {
			TypeBinding [] annotatedTypes = getDerivedTypesForDeferredInitialization();
			for (int i = 0, length = annotatedTypes == null ? 0 : annotatedTypes.length; i < length; i++) {
				TypeVariableBinding annotatedType = (TypeVariableBinding) annotatedTypes[i];
				annotatedType.superInterfaces = superInterfaces;
			}
		}
		return superInterfaces;
	}

	protected TypeBinding[] getDerivedTypesForDeferredInitialization() {
		return this.environment.getAnnotatedTypes(this);
	}

	public TypeBinding combineTypeAnnotations(TypeBinding substitute) {
		if (hasTypeAnnotations()) {
			// may need to merge annotations from the original variable and from substitution:
			if (hasRelevantTypeUseNullAnnotations()) {
				// explicit type use null annotation overrides any annots on type parameter and concrete type arguments
				substitute = substitute.withoutToplevelNullAnnotation();
			}
			if (this.typeAnnotations != Binding.NO_ANNOTATIONS)
				return this.environment.createAnnotatedType(substitute, this.typeAnnotations);
			// annots on originalVariable not relevant, and substitute has annots, keep substitute unmodified:
		}
		return substitute;
	}

	private boolean hasRelevantTypeUseNullAnnotations() {
		TypeVariableBinding[] parameters;
		if (this.declaringElement instanceof ReferenceBinding) {
			parameters = ((ReferenceBinding)this.declaringElement).original().typeVariables();
		} else if (this.declaringElement instanceof MethodBinding) {
			parameters = ((MethodBinding)this.declaringElement).original().typeVariables;
		} else {
			throw new IllegalStateException("Unexpected declaring element:"+String.valueOf(this.declaringElement.readableName())); //$NON-NLS-1$
		}
		TypeVariableBinding parameter = parameters[this.rank];
		// recognize explicit annots by their effect on null tag bits, if there's no effect, then the annot is not considered relevant
		long currentNullBits = this.tagBits & TagBits.AnnotationNullMASK;
		long declarationNullBits = parameter.tagBits & TagBits.AnnotationNullMASK;
		return (currentNullBits & ~declarationNullBits) != 0;
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
				if (this.superclass != null)
					this.tagBits |= this.superclass.updateTagBits();
				if (this.superInterfaces != null)
					for (TypeBinding superIfc : this.superInterfaces)
						this.tagBits |= superIfc.updateTagBits();
			} finally {
				this.inRecursiveFunction = false;
			}
		}
		return super.updateTagBits();
	}

	@Override
	public boolean isFreeTypeVariable() {
		return this.environment.usesNullTypeAnnotations()
				&& this.environment.globalOptions.pessimisticNullAnalysisForFreeTypeVariablesEnabled
				&& (this.tagBits & TagBits.AnnotationNullMASK) == 0;
	}

	@Override
	public ReferenceBinding upwardsProjection(Scope scope, TypeBinding[] mentionedTypeVariables) {
		return this;
	}

	@Override
	public ReferenceBinding downwardsProjection(Scope scope, TypeBinding[] mentionedTypeVariables) {
		return this;
	}

}
