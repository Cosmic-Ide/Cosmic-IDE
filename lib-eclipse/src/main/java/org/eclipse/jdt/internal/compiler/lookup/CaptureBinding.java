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
 *     Stephan Herrmann - Contribution for
 *								Bug 400874 - [1.8][compiler] Inference infrastructure should evolve to meet JLS8 18.x (Part G of JSR335 spec)
 *								Bug 429384 - [1.8][null] implement conformance rules for null-annotated lower / upper type bounds
 *								Bug 441797 - [1.8] synchronize type annotations on capture and its wildcard
 *								Bug 456497 - [1.8][null] during inference nullness from target type is lost against weaker hint from applicability analysis
 *								Bug 456924 - StackOverflowError during compilation
 *								Bug 462790 - [null] NPE in Expression.computeConversion()
 *     Jesper S Møller - Contributions for bug 381345 : [1.8] Take care of the Java 8 major version
 *								Bug 527554 - [18.3] Compiler support for JEP 286 Local-Variable Type
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.lookup;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.Wildcard;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;

public class CaptureBinding extends TypeVariableBinding {

	public TypeBinding lowerBound;
	public WildcardBinding wildcard;
	public int captureID;

	/* information to compute unique binding key */
	public ReferenceBinding sourceType;
	public int start;
	public int end;
	public ASTNode cud; // to facilitate recaptures.

	TypeBinding pendingSubstitute; // for substitution of recursive captures, see https://bugs.eclipse.org/456924

	public CaptureBinding(WildcardBinding wildcard, ReferenceBinding sourceType, int start, int end, ASTNode cud, int captureID) {
		super(TypeConstants.WILDCARD_CAPTURE_NAME_PREFIX, wildcard.environment);
		this.wildcard = wildcard;
		this.modifiers = ClassFileConstants.AccPublic | ExtraCompilerModifiers.AccGenericSignature; // treat capture as public
		this.fPackage = wildcard.fPackage;
		this.sourceType = sourceType;
		this.start = start;
		this.end = end;
		this.captureID = captureID;
		this.tagBits |= TagBits.HasCapturedWildcard;
		this.cud = cud;
		if (wildcard.hasTypeAnnotations()) {
			// register an unannoted version before adding the annotated wildcard:
			CaptureBinding unannotated = (CaptureBinding) clone(null);
			unannotated.wildcard = (WildcardBinding) this.wildcard.unannotated();
			this.environment.getUnannotatedType(unannotated);
			this.id = unannotated.id; // transfer fresh id
			// now register this annotated type:
			this.environment.typeSystem.cacheDerivedType(this, unannotated, this);
			// propagate from wildcard to capture - use super version, because our own method propagates type annotations in the opposite direction:
			super.setTypeAnnotations(wildcard.getTypeAnnotations(), wildcard.environment.globalOptions.isAnnotationBasedNullAnalysisEnabled);
			if (wildcard.hasNullTypeAnnotations())
				this.tagBits |= TagBits.HasNullTypeAnnotation;
		} else {
			computeId(this.environment);
			if(wildcard.hasNullTypeAnnotations()) {
				this.tagBits |= (wildcard.tagBits & TagBits.AnnotationNullMASK) | TagBits.HasNullTypeAnnotation;
			}
		}
	}

	// for subclass CaptureBinding18
	protected CaptureBinding(ReferenceBinding sourceType, char[] sourceName, int start, int end, int captureID, LookupEnvironment environment) {
		super(sourceName, null, 0, environment);
		this.modifiers = ClassFileConstants.AccPublic | ExtraCompilerModifiers.AccGenericSignature; // treat capture as public
		this.sourceType = sourceType;
		this.start = start;
		this.end = end;
		this.captureID = captureID;
	}

	public CaptureBinding(CaptureBinding prototype) {
		super(prototype);
		this.wildcard = prototype.wildcard;
		this.sourceType = prototype.sourceType;
		this.start = prototype.start;
		this.end = prototype.end;
		this.captureID = prototype.captureID;
		this.lowerBound = prototype.lowerBound;
		this.tagBits |= (prototype.tagBits & TagBits.HasCapturedWildcard);
		this.cud = prototype.cud;
	}

	// Captures may get cloned and annotated during type inference.
	@Override
	public TypeBinding clone(TypeBinding enclosingType) {
		return new CaptureBinding(this);
	}

	/*
	 * sourceTypeKey ! wildcardKey position semi-colon
	 * p.X { capture of ? } --> !*123; (Lp/X; in declaring type except if leaf)
	 * p.X { capture of ? extends p.Y } --> !+Lp/Y;123; (Lp/X; in declaring type except if leaf)
	 */
	@Override
	public char[] computeUniqueKey(boolean isLeaf) {
		StringBuilder buffer = new StringBuilder();
		if (isLeaf) {
			buffer.append(this.sourceType.computeUniqueKey(false/*not a leaf*/));
			buffer.append('&');
		}
		buffer.append(TypeConstants.WILDCARD_CAPTURE);
		buffer.append(this.wildcard.computeUniqueKey(false/*not a leaf*/));
		buffer.append(this.end);
		buffer.append(';');
		int length = buffer.length();
		char[] uniqueKey = new char[length];
		buffer.getChars(0, length, uniqueKey, 0);
		return uniqueKey;
	}

	@Override
	public String debugName() {

		if (this.wildcard != null) {
			StringBuilder buffer = new StringBuilder(10);
			AnnotationBinding [] annotations = getTypeAnnotations();
			for (int i = 0, length = annotations == null ? 0 : annotations.length; i < length; i++) {
				buffer.append(annotations[i]);
				buffer.append(' ');
			}
			buffer
				.append(TypeConstants.WILDCARD_CAPTURE_NAME_PREFIX)
				.append(this.captureID)
				.append(TypeConstants.WILDCARD_CAPTURE_NAME_SUFFIX)
				.append(this.wildcard.debugName());
			return buffer.toString();
		}
		return super.debugName();
	}

	@Override
	public char[] genericTypeSignature() {
		// captures have no signature per JVMS 4.7.9.1, approximate one by erasure:
		if (this.inRecursiveFunction) {
			// catch "capture#1 of X<capture#1 ...>":
			// prefer answering "Ljava.lang.Object;" instead of throwing StackOverflowError:
			return CharOperation.concat(new char[] {'L'}, CharOperation.concatWith(TypeConstants.JAVA_LANG_OBJECT, '.'), new char[] {';'});
		}
		this.inRecursiveFunction = true;
		try {
			return erasure().genericTypeSignature();
		} finally {
			this.inRecursiveFunction = false;
		}
	}

	/**
	 * Initialize capture bounds using substituted supertypes
	 * e.g. given X<U, V extends X<U, V>>,     capture(X<E,?>) = X<E,capture>, where capture extends X<E,capture>
	 */
	public void initializeBounds(Scope scope, ParameterizedTypeBinding capturedParameterizedType) {
		boolean is18plus = scope.compilerOptions().complianceLevel >= ClassFileConstants.JDK1_8;
		TypeVariableBinding wildcardVariable = this.wildcard.typeVariable();
		if (wildcardVariable == null) {
			// error resilience when capturing Zork<?>
			// no substitution for wildcard bound (only formal bounds from type variables are to be substituted: 104082)
			TypeBinding originalWildcardBound = this.wildcard.bound;
			switch (this.wildcard.boundKind) {
				case Wildcard.EXTENDS :
					// still need to capture bound supertype as well so as not to expose wildcards to the outside (111208)
					TypeBinding capturedWildcardBound = is18plus
							? originalWildcardBound // as spec'd
							: originalWildcardBound.capture(scope, this.start, this.end); // for compatibility with old behavior at 1.7-
					if (originalWildcardBound.isInterface()) {
						this.setSuperClass(scope.getJavaLangObject());
						this.setSuperInterfaces(new ReferenceBinding[] { (ReferenceBinding) capturedWildcardBound });
					} else {
						// the wildcard bound should be a subtype of variable superclass
						// it may occur that the bound is less specific, then consider glb (202404)
						if (capturedWildcardBound.isArrayType() || TypeBinding.equalsEquals(capturedWildcardBound, this)) {
							this.setSuperClass(scope.getJavaLangObject());
						} else {
							this.setSuperClass((ReferenceBinding) capturedWildcardBound);
						}
						this.setSuperInterfaces(Binding.NO_SUPERINTERFACES);
					}
					this.setFirstBound(capturedWildcardBound);
					if ((capturedWildcardBound.tagBits & TagBits.HasTypeVariable) == 0)
						this.tagBits &= ~TagBits.HasTypeVariable;
					break;
				case Wildcard.UNBOUND :
					this.setSuperClass(scope.getJavaLangObject());
					this.setSuperInterfaces(Binding.NO_SUPERINTERFACES);
					this.tagBits &= ~TagBits.HasTypeVariable;
					break;
				case Wildcard.SUPER :
					this.setSuperClass(scope.getJavaLangObject());
					this.setSuperInterfaces(Binding.NO_SUPERINTERFACES);
					this.lowerBound = this.wildcard.bound;
					if ((originalWildcardBound.tagBits & TagBits.HasTypeVariable) == 0)
						this.tagBits &= ~TagBits.HasTypeVariable;
					break;
			}
			return;
		}
		ReferenceBinding originalVariableSuperclass = wildcardVariable.superclass;
		ReferenceBinding substitutedVariableSuperclass = (ReferenceBinding) Scope.substitute(capturedParameterizedType, originalVariableSuperclass);
		// prevent cyclic capture: given X<T>, capture(X<? extends T> could yield a circular type
		if (TypeBinding.equalsEquals(substitutedVariableSuperclass, this)) substitutedVariableSuperclass = originalVariableSuperclass;

		ReferenceBinding[] originalVariableInterfaces = wildcardVariable.superInterfaces();
		ReferenceBinding[] substitutedVariableInterfaces = Scope.substitute(capturedParameterizedType, originalVariableInterfaces);
		if (substitutedVariableInterfaces != originalVariableInterfaces) {
			// prevent cyclic capture: given X<T>, capture(X<? extends T> could yield a circular type
			for (int i = 0, length = substitutedVariableInterfaces.length; i < length; i++) {
				if (TypeBinding.equalsEquals(substitutedVariableInterfaces[i], this)) substitutedVariableInterfaces[i] = originalVariableInterfaces[i];
			}
		}
		// no substitution for wildcard bound (only formal bounds from type variables are to be substituted: 104082)
		TypeBinding originalWildcardBound = this.wildcard.bound;

		switch (this.wildcard.boundKind) {
			case Wildcard.EXTENDS :
				// still need to capture bound supertype as well so as not to expose wildcards to the outside (111208)
				TypeBinding capturedWildcardBound = is18plus
							? originalWildcardBound // as spec'd
							: originalWildcardBound.capture(scope, this.start, this.end); // for compatibility with old behavior at 1.7-
				if (originalWildcardBound.isInterface()) {
					this.setSuperClass(substitutedVariableSuperclass);
					// merge wildcard bound into variable superinterfaces using glb
					if (substitutedVariableInterfaces == Binding.NO_SUPERINTERFACES) {
						this.setSuperInterfaces(new ReferenceBinding[] { (ReferenceBinding) capturedWildcardBound });
					} else {
						int length = substitutedVariableInterfaces.length;
						System.arraycopy(substitutedVariableInterfaces, 0, substitutedVariableInterfaces = new ReferenceBinding[length+1], 1, length);
						substitutedVariableInterfaces[0] =  (ReferenceBinding) capturedWildcardBound;
						this.setSuperInterfaces(Scope.greaterLowerBound(substitutedVariableInterfaces));
					}
				} else {
					// the wildcard bound should be a subtype of variable superclass
					// it may occur that the bound is less specific, then consider glb (202404)
					if (capturedWildcardBound.isArrayType() || TypeBinding.equalsEquals(capturedWildcardBound, this)) {
						this.setSuperClass(substitutedVariableSuperclass);
					} else {
						this.setSuperClass((ReferenceBinding) capturedWildcardBound);
						if (this.superclass.isSuperclassOf(substitutedVariableSuperclass)) {
							this.setSuperClass(substitutedVariableSuperclass);
						}
						// TODO: there are cases were we need to compute glb(capturedWildcardBound, substitutedVariableSuperclass)
						//       but then when glb (perhaps triggered inside setFirstBound()) fails, how to report the error??
					}
					this.setSuperInterfaces(substitutedVariableInterfaces);
				}
				this.setFirstBound(capturedWildcardBound);
				if ((capturedWildcardBound.tagBits & TagBits.HasTypeVariable) == 0)
					this.tagBits &= ~TagBits.HasTypeVariable;
				break;
			case Wildcard.UNBOUND :
				this.setSuperClass(substitutedVariableSuperclass);
				this.setSuperInterfaces(substitutedVariableInterfaces);
				this.tagBits &= ~TagBits.HasTypeVariable;
				break;
			case Wildcard.SUPER :
				this.setSuperClass(substitutedVariableSuperclass);
				if (TypeBinding.equalsEquals(wildcardVariable.firstBound, substitutedVariableSuperclass) || TypeBinding.equalsEquals(originalWildcardBound, substitutedVariableSuperclass)) {
					this.setFirstBound(substitutedVariableSuperclass);
				}
				this.setSuperInterfaces(substitutedVariableInterfaces);
				this.lowerBound = originalWildcardBound;
				if ((originalWildcardBound.tagBits & TagBits.HasTypeVariable) == 0)
					this.tagBits &= ~TagBits.HasTypeVariable;
				break;
		}
		if(scope.environment().usesNullTypeAnnotations()) {
			evaluateNullAnnotations(scope, null);
		}
	}
	@Override
	public ReferenceBinding upwardsProjection(Scope scope, TypeBinding[] mentionedTypeVariables) {
		if (enterRecursiveProjectionFunction()) {
			try {
				for (int i = 0; i < mentionedTypeVariables.length; ++i) {
					if (TypeBinding.equalsEquals(this, mentionedTypeVariables[i])) {
						TypeBinding upperBoundForProjection = this.upperBoundForProjection();
						return ((ReferenceBinding)upperBoundForProjection).upwardsProjection(scope, mentionedTypeVariables);
					}
				}
				return this;
			} finally {
				exitRecursiveProjectionFunction();
			}
		} else {
			return scope.getJavaLangObject();
		}
	}
	public TypeBinding upperBoundForProjection() {
		TypeBinding upperBound = null;
		if (this.wildcard != null) {
			ReferenceBinding[] supers = this.superInterfaces();
			if (this.wildcard.boundKind == Wildcard.EXTENDS) {
				if (supers.length > 0) {
					ReferenceBinding[] allBounds = new ReferenceBinding[supers.length + 1];
					System.arraycopy(supers, 0, allBounds, 1, supers.length);
					allBounds[0] = this.superclass();
					ReferenceBinding[] glbs = Scope.greaterLowerBound(allBounds);
					if (glbs == null) {
						upperBound = new ProblemReferenceBinding(null, null, ProblemReasons.ParameterBoundMismatch);
					} else if (glbs.length == 1) {
						upperBound = glbs[0];
					} else {
						upperBound = this.environment.createIntersectionType18(glbs);
					}
				} else {
					upperBound = this.superclass;
				}
			} else {
				// ITB18.isCompatibleWith does not handle the presence of j.l.Object among intersecting types,
				// so it returns false when checking (I&J).isCompatibleWith(Object&I&J)
				// TODO see if this can be handled in ITB18.isCompatibleWith() itself
				boolean superClassIsObject = TypeBinding.equalsEquals(this.superclass(), this.environment.getResolvedJavaBaseType(TypeConstants.JAVA_LANG_OBJECT, null));
				if (supers.length == 0) {
					upperBound = this.superclass();
				} else if (supers.length == 1) {
					upperBound = superClassIsObject ? supers[0] : this.environment.createIntersectionType18(new ReferenceBinding[] {this.superclass(), supers[0]});
				} else {
					if (superClassIsObject) {
						upperBound = this.environment.createIntersectionType18(supers);
					} else {
						ReferenceBinding[] allBounds = new ReferenceBinding[supers.length + 1];
						System.arraycopy(supers, 0, allBounds, 1, supers.length);
						allBounds[0] = this.superclass();
						upperBound = this.environment.createIntersectionType18(allBounds);
					}
				}
			}
		} else {
			upperBound = super.upperBound();
		}
		return upperBound;
	}

	/**
	 * @see org.eclipse.jdt.internal.compiler.lookup.TypeBinding#isCapture()
	 */
	@Override
	public boolean isCapture() {
		return true;
	}

	/**
	 * @see TypeBinding#isEquivalentTo(TypeBinding)
	 */
	@Override
	public boolean isEquivalentTo(TypeBinding otherType) {
	    if (equalsEquals(this, otherType)) return true;
	    if (otherType == null) return false;
		// capture of ? extends X[]
		if (this.firstBound != null && this.firstBound.isArrayType()) {
			if (this.firstBound.isCompatibleWith(otherType))
				return true;
		}
		switch (otherType.kind()) {
			case Binding.WILDCARD_TYPE :
			case Binding.INTERSECTION_TYPE :
				return ((WildcardBinding) otherType).boundCheck(this);
		}
		return false;
	}

	@Override
	public boolean isProperType(boolean admitCapture18) {
		if (this.lowerBound != null && !this.lowerBound.isProperType(admitCapture18))
			return false;
		if (this.wildcard != null && !this.wildcard.isProperType(admitCapture18))
			return false;
		return super.isProperType(admitCapture18);
	}

	@Override
	public char[] readableName() {
		if (this.wildcard != null) {
			StringBuilder buffer = new StringBuilder(10);
			buffer
				.append(TypeConstants.WILDCARD_CAPTURE_NAME_PREFIX)
				.append(this.captureID)
				.append(TypeConstants.WILDCARD_CAPTURE_NAME_SUFFIX)
				.append(this.wildcard.readableName());
			int length = buffer.length();
			char[] name = new char[length];
			buffer.getChars(0, length, name, 0);
			return name;
		}
		return super.readableName();
	}

	@Override
	public char[] signableName() {
		if (this.wildcard != null) {
			StringBuilder buffer = new StringBuilder(10);
			buffer
				.append(TypeConstants.WILDCARD_CAPTURE_SIGNABLE_NAME_SUFFIX)
				.append(this.wildcard.readableName());
			int length = buffer.length();
			char[] name = new char[length];
			buffer.getChars(0, length, name, 0);
			return name;
		}
		return super.readableName();
	}

	@Override
	public char[] shortReadableName() {
		if (this.wildcard != null) {
			StringBuilder buffer = new StringBuilder(10);
			buffer
				.append(TypeConstants.WILDCARD_CAPTURE_NAME_PREFIX)
				.append(this.captureID)
				.append(TypeConstants.WILDCARD_CAPTURE_NAME_SUFFIX)
				.append(this.wildcard.shortReadableName());
			int length = buffer.length();
			char[] name = new char[length];
			buffer.getChars(0, length, name, 0);
			return name;
		}
		return super.shortReadableName();
	}

	@Override
	public char[] nullAnnotatedReadableName(CompilerOptions options, boolean shortNames) {
	    StringBuffer nameBuffer = new StringBuffer(10);
		appendNullAnnotation(nameBuffer, options);
		nameBuffer.append(this.sourceName());
		if (!this.inRecursiveFunction) { // CaptureBinding18 can be recursive indeed
			this.inRecursiveFunction = true;
			try {
				if (this.wildcard != null) {
					nameBuffer.append("of "); //$NON-NLS-1$
					nameBuffer.append(this.wildcard.withoutToplevelNullAnnotation().nullAnnotatedReadableName(options, shortNames));
				} else if (this.lowerBound != null) {
					nameBuffer.append(" super "); //$NON-NLS-1$
					nameBuffer.append(this.lowerBound.nullAnnotatedReadableName(options, shortNames));
				} else if (this.firstBound != null) {
					nameBuffer.append(" extends "); //$NON-NLS-1$
					nameBuffer.append(this.firstBound.nullAnnotatedReadableName(options, shortNames));
					TypeBinding[] otherUpperBounds = this.otherUpperBounds();
					if (otherUpperBounds != NO_TYPES)
						nameBuffer.append(" & ..."); //$NON-NLS-1$ // only hint at more bounds, we currently don't evaluate null annotations on otherUpperBounds
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
	public TypeBinding withoutToplevelNullAnnotation() {
		if (!hasNullTypeAnnotations())
			return this;
		if (this.wildcard != null && this.wildcard.hasNullTypeAnnotations()) {
			WildcardBinding newWildcard = (WildcardBinding) this.wildcard.withoutToplevelNullAnnotation();
			if (newWildcard != this.wildcard) { //$IDENTITY-COMPARISON$

				CaptureBinding newCapture = (CaptureBinding) this.environment.getUnannotatedType(this).clone(null);
				if (newWildcard.hasTypeAnnotations())
					newCapture.tagBits |= TagBits.HasTypeAnnotations;
				newCapture.wildcard = newWildcard;

				// manually transfer the following two, because we are not in a context where we can call initializeBounds():
				newCapture.superclass = this.superclass;
				newCapture.superInterfaces = this.superInterfaces;

				AnnotationBinding[] newAnnotations = this.environment.filterNullTypeAnnotations(this.typeAnnotations);
				return this.environment.createAnnotatedType(newCapture, newAnnotations);
			}
		}
		return super.withoutToplevelNullAnnotation();
	}

	@Override
	TypeBinding substituteInferenceVariable(InferenceVariable var, TypeBinding substituteType) {
		if (this.pendingSubstitute != null)
			return this.pendingSubstitute;
		try {
			TypeBinding substitutedWildcard = this.wildcard.substituteInferenceVariable(var, substituteType);
			if (substitutedWildcard != this.wildcard) {  //$IDENTITY-COMPARISON$
				CaptureBinding substitute = (CaptureBinding) clone(enclosingType());
			    substitute.wildcard = (WildcardBinding) substitutedWildcard;
			    this.pendingSubstitute = substitute;
			    if (this.lowerBound != null)
			    	substitute.lowerBound = this.lowerBound.substituteInferenceVariable(var, substituteType);
			    if (this.firstBound != null)
			    	substitute.firstBound = this.firstBound.substituteInferenceVariable(var, substituteType);
			    if (this.superclass != null)
			    	substitute.superclass = (ReferenceBinding) this.superclass.substituteInferenceVariable(var, substituteType);
			    if (this.superInterfaces != null) {
			    	int length = this.superInterfaces.length;
			    	substitute.superInterfaces = new ReferenceBinding[length];
			    	for (int i = 0; i < length; i++)
			    		substitute.superInterfaces[i] = (ReferenceBinding) this.superInterfaces[i].substituteInferenceVariable(var, substituteType);
			    }
			    return substitute;
			}
			return this;
		} finally {
			this.pendingSubstitute = null;
		}
	}

	@Override
	public void setTypeAnnotations(AnnotationBinding[] annotations, boolean evalNullAnnotations) {
		super.setTypeAnnotations(annotations, evalNullAnnotations);
		if (annotations != Binding.NO_ANNOTATIONS && this.wildcard != null) {
			// keep annotations in sync, propagate from capture to its wildcard:
			this.wildcard = (WildcardBinding) this.wildcard.environment.createAnnotatedType(this.wildcard, annotations);
		}
	}

	@Override
	public TypeBinding uncapture(Scope scope) {
		return this.wildcard.uncapture(scope);
	}

	@Override
	public ReferenceBinding downwardsProjection(Scope scope, TypeBinding[] mentionedTypeVariables) {
		ReferenceBinding result = null;
		if (enterRecursiveProjectionFunction()) {
			for (int i = 0; i < mentionedTypeVariables.length; ++i) {
				if (TypeBinding.equalsEquals(this, mentionedTypeVariables[i])) {
					if (this.lowerBound != null) {
						result = (ReferenceBinding) this.lowerBound.downwardsProjection(scope, mentionedTypeVariables);
					}
					break;
				}
			}
			exitRecursiveProjectionFunction();
		}
		return result;
	}

	/*
	 * CaptureBinding needs even more propagation, because we are creating a naked type
	 * (during CaptureBinding(WildcardBinding,ReferenceBinding,int,int,ASTNode,int)
	 * that has no firstBound / superclass / superInterfaces set.
	 */
	@Override
	protected TypeBinding[] getDerivedTypesForDeferredInitialization() {
		TypeBinding[] derived = this.environment.typeSystem.getDerivedTypes(this);
		if (derived.length > 0) {
			int count = 0;
			for (int i = 0; i < derived.length; i++) {
				if (derived[i] != null && derived[i].id == this.id)
					derived[count++] = derived[i];
			}
			if (count < derived.length)
				System.arraycopy(derived, 0, derived = new TypeBinding[count], 0, count);
		}
		return derived;
	}

	@Override
	public String toString() {
		if (this.wildcard != null) {
			StringBuilder buffer = new StringBuilder(10);
			AnnotationBinding [] annotations = getTypeAnnotations();
			for (int i = 0, length = annotations == null ? 0 : annotations.length; i < length; i++) {
				buffer.append(annotations[i]);
				buffer.append(' ');
			}
			buffer
				.append(TypeConstants.WILDCARD_CAPTURE_NAME_PREFIX)
				.append(this.captureID)
				.append(TypeConstants.WILDCARD_CAPTURE_NAME_SUFFIX)
				.append(this.wildcard);
			return buffer.toString();
		}
		return super.toString();
	}


	@Override
	public char[] signature() /* Ljava/lang/Object; */ {
		if (this.signature != null)
			return this.signature;

		if (this.firstBound instanceof ArrayBinding) {
			this.signature = constantPoolName();
		} else {
			this.signature = CharOperation.concat('L', constantPoolName(), ';');
		}
		return this.signature;

	}

}
