/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
 *								bug 392384 - [1.8][compiler][null] Restore nullness info from type annotations in class files
 *								Bug 416174 - [1.8][compiler][null] Bogus name clash error with null annotations
 *								Bug 416176 - [1.8][compiler][null] null type annotations cause grief on type variables
 *								Bug 423504 - [1.8] Implement "18.5.3 Functional Interface Parameterization Inference"
 *								Bug 425783 - An internal error occurred during: "Requesting Java AST from selection". java.lang.StackOverflowError
 *								Bug 438458 - [1.8][null] clean up handling of null type annotations wrt type variables
 *								Bug 441693 - [1.8][null] Bogus warning for type argument annotated with @NonNull
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.lookup;

import java.util.Set;

import org.eclipse.jdt.core.compiler.CharOperation;

/**
 * Denote a raw type, i.e. a generic type referenced without any type arguments.
 * e.g. X<T extends Exception> can be used a raw type 'X', in which case it
 * 	will behave as X<Exception>
 */
public class RawTypeBinding extends ParameterizedTypeBinding {

    /**
     * Raw type arguments are erasure of respective parameter bounds. But we may not have resolved
     * these bounds yet if creating raw types while supertype hierarchies are being connected.
     * Therefore, use 'null' instead, and access these in a lazy way later on (when substituting).
     */
	public RawTypeBinding(ReferenceBinding type, ReferenceBinding enclosingType, LookupEnvironment environment){
		super(type, null, enclosingType, environment);
		this.tagBits &= ~TagBits.HasMissingType;
		if ((type.tagBits & TagBits.HasMissingType) != 0) {
			if (type instanceof MissingTypeBinding) {
				this.tagBits |= TagBits.HasMissingType;
			} else if (type instanceof ParameterizedTypeBinding) {
				ParameterizedTypeBinding parameterizedTypeBinding = (ParameterizedTypeBinding) type;
				if (parameterizedTypeBinding.genericType() instanceof MissingTypeBinding) {
					this.tagBits |= TagBits.HasMissingType;
				}
			}
		}
		if (enclosingType != null && (enclosingType.tagBits & TagBits.HasMissingType) != 0) {
			if (enclosingType instanceof MissingTypeBinding) {
				this.tagBits |= TagBits.HasMissingType;
			} else if (enclosingType instanceof ParameterizedTypeBinding) {
				ParameterizedTypeBinding parameterizedTypeBinding = (ParameterizedTypeBinding) enclosingType;
				if (parameterizedTypeBinding.genericType() instanceof MissingTypeBinding) {
					this.tagBits |= TagBits.HasMissingType;
				}
			}
		}
		if (enclosingType == null || !this.hasEnclosingInstanceContext() || (enclosingType.modifiers & ExtraCompilerModifiers.AccGenericSignature) == 0) {
			this.modifiers &= ~ExtraCompilerModifiers.AccGenericSignature; // only need signature if enclosing needs one
		}
	}

	@Override
	public char[] computeUniqueKey(boolean isLeaf) {
	    StringBuilder sig = new StringBuilder(10);
		if (isMemberType() && (enclosingType().isParameterizedType() || enclosingType().isRawType())) {
			if (!hasEnclosingInstanceContext()) {
			    char[] typeSig = enclosingType().signature(); // don't consider generics from enclosing of static member
			    sig.append(typeSig, 0, typeSig.length-1); // copy all but trailing semicolon
			    sig.append('$'); // for consistency with keys prior to https://bugs.eclipse.org/460491
			} else {
				char[] typeSig = enclosingType().computeUniqueKey(false/*not a leaf*/);
				sig.append(typeSig, 0, typeSig.length-1); // copy all but trailing semicolon
				sig.append('.');
			}
			sig.append(sourceName());
		    if (genericType().typeVariables() != Binding.NO_TYPE_VARIABLES) // don't show <> if raw only due to a generic enclosing
		    	sig.append('<').append('>');
		    sig.append(';');
		} else {
		     sig.append(genericType().computeUniqueKey(false/*not a leaf*/));
		     sig.insert(sig.length()-1, "<>"); //$NON-NLS-1$
		}

		int sigLength = sig.length();
		char[] uniqueKey = new char[sigLength];
		sig.getChars(0, sigLength, uniqueKey, 0);
		return uniqueKey;
   	}

	@Override
	public TypeBinding clone(TypeBinding outerType) {
		return new RawTypeBinding(this.actualType(), (ReferenceBinding) outerType, this.environment);
	}

	@Override
	public TypeBinding withoutToplevelNullAnnotation() {
		if (!hasNullTypeAnnotations())
			return this;
		ReferenceBinding unannotatedGenericType = (ReferenceBinding) this.environment.getUnannotatedType(this.genericType());
		AnnotationBinding[] newAnnotations = this.environment.filterNullTypeAnnotations(this.typeAnnotations);
		return this.environment.createRawType(unannotatedGenericType, this.enclosingType(), newAnnotations);
	}

	/**
	 * @see org.eclipse.jdt.internal.compiler.lookup.ParameterizedTypeBinding#createParameterizedMethod(org.eclipse.jdt.internal.compiler.lookup.MethodBinding)
	 */
	@Override
	public ParameterizedMethodBinding createParameterizedMethod(MethodBinding originalMethod) {
		if (originalMethod.typeVariables == Binding.NO_TYPE_VARIABLES || originalMethod.isStatic()) {
			return super.createParameterizedMethod(originalMethod);
		}
		return this.environment.createParameterizedGenericMethod(originalMethod, this);
	}

	@Override
	public boolean isParameterizedType() {
		return false;
	}

	@Override
	public int kind() {
		return RAW_TYPE;
	}

	/**
	 * @see org.eclipse.jdt.internal.compiler.lookup.TypeBinding#debugName()
	 */
	@Override
	public String debugName() {
		if (this.hasTypeAnnotations())
			return annotatedDebugName();
		StringBuilder nameBuffer = new StringBuilder(10);
		nameBuffer.append(actualType().sourceName()).append("#RAW"); //$NON-NLS-1$
	    return nameBuffer.toString();
	}
	@Override
	public String annotatedDebugName() {
		StringBuilder buffer = new StringBuilder(super.annotatedDebugName());
		buffer.append("#RAW"); //$NON-NLS-1$
		return buffer.toString();
	}
	/**
	 * Ltype<param1 ... paramN>;
	 * LY<TT;>;
	 */
	@Override
	public char[] genericTypeSignature() {
		if (this.genericTypeSignature == null) {
			if ((this.modifiers & ExtraCompilerModifiers.AccGenericSignature) == 0) {
		    	this.genericTypeSignature = genericType().signature();
			} else {
			    StringBuilder sig = new StringBuilder(10);
			    if (isMemberType() && hasEnclosingInstanceContext()) {
			    	ReferenceBinding enclosing = enclosingType();
					char[] typeSig = enclosing.genericTypeSignature();
					sig.append(typeSig, 0, typeSig.length-1);// copy all but trailing semicolon
			    	if ((enclosing.modifiers & ExtraCompilerModifiers.AccGenericSignature) != 0) {
			    		sig.append('.');
			    	} else {
			    		sig.append('$');
			    	}
			    	sig.append(sourceName());
			    } else {
			    	char[] typeSig = genericType().signature();
					sig.append(typeSig, 0, typeSig.length-1);// copy all but trailing semicolon
		    	}
				sig.append(';');
				int sigLength = sig.length();
				this.genericTypeSignature = new char[sigLength];
				sig.getChars(0, sigLength, this.genericTypeSignature, 0);
			}
		}
		return this.genericTypeSignature;
	}

    @Override
	public boolean isEquivalentTo(TypeBinding otherType) {
		if (equalsEquals(this, otherType) || equalsEquals(erasure(), otherType))
		    return true;
	    if (otherType == null)
	        return false;
	    switch(otherType.kind()) {

	    	case Binding.WILDCARD_TYPE :
			case Binding.INTERSECTION_TYPE:
	        	return ((WildcardBinding) otherType).boundCheck(this);

	    	case Binding.GENERIC_TYPE :
	    	case Binding.PARAMETERIZED_TYPE :
	    	case Binding.RAW_TYPE :
	            return TypeBinding.equalsEquals(erasure(), otherType.erasure());
	    }
        return false;
	}

    @Override
	public boolean isProvablyDistinct(TypeBinding otherType) {
		if (TypeBinding.equalsEquals(this, otherType) || TypeBinding.equalsEquals(erasure(), otherType)) // https://bugs.eclipse.org/bugs/show_bug.cgi?id=329588
		    return false;
	    if (otherType == null)
	        return true;
	    switch(otherType.kind()) {

	    	case Binding.GENERIC_TYPE :
	    	case Binding.PARAMETERIZED_TYPE :
	    	case Binding.RAW_TYPE :
	            return TypeBinding.notEquals(erasure(), otherType.erasure());
	    }
        return true;
	}
    @Override
	public boolean isSubtypeOf(TypeBinding right, boolean simulatingBugJDK8026527) {
    	if (simulatingBugJDK8026527) {
    		right = this.environment.convertToRawType(right.erasure(), false);
    	}
    	return super.isSubtypeOf(right, simulatingBugJDK8026527);
    }

    @Override
	public boolean isProperType(boolean admitCapture18) {
    	TypeBinding actualType = actualType();
    	return actualType != null && actualType.isProperType(admitCapture18);
    }

	@Override
	protected void initializeArguments() {
		TypeVariableBinding[] typeVariables = genericType().typeVariables();
		int length = typeVariables.length;
		TypeBinding[] typeArguments = new TypeBinding[length];
		for (int i = 0; i < length; i++) {
			// perform raw conversion on variable upper bound - could cause infinite regression if arguments were initialized lazily
		    typeArguments[i] = this.environment.convertToRawType(typeVariables[i].erasure(), false /*do not force conversion of enclosing types*/);
		}
		this.arguments = typeArguments;
	}

	@Override
	public ParameterizedTypeBinding capture(Scope scope, int start, int end) {
		return this;
	}

	@Override
	public TypeBinding uncapture(Scope scope) {
		return this;
	}

	@Override
	TypeBinding substituteInferenceVariable(InferenceVariable var, TypeBinding substituteType) {
		// NEVER substitute the type arguments of a raw type
		return this;
	}

	@Override
	public MethodBinding getSingleAbstractMethod(Scope scope, boolean replaceWildcards) {
		int index = replaceWildcards ? 0 : 1;
		if (this.singleAbstractMethod != null) {
			if (this.singleAbstractMethod[index] != null)
			return this.singleAbstractMethod[index];
		} else {
			this.singleAbstractMethod = new MethodBinding[2];
		}
		final ReferenceBinding genericType = genericType();
		MethodBinding theAbstractMethod = genericType.getSingleAbstractMethod(scope, replaceWildcards);
		if (theAbstractMethod == null || !theAbstractMethod.isValidBinding())
			return this.singleAbstractMethod[index] = theAbstractMethod;

		ReferenceBinding declaringType = (ReferenceBinding) scope.environment().convertToRawType(genericType, true);
		declaringType = (ReferenceBinding) declaringType.findSuperTypeOriginatingFrom(theAbstractMethod.declaringClass);
		MethodBinding [] choices = declaringType.getMethods(theAbstractMethod.selector);
		for (int i = 0, length = choices.length; i < length; i++) {
			MethodBinding method = choices[i];
			if (!method.isAbstract() || method.redeclaresPublicObjectMethod(scope)) continue; // (re)skip statics, defaults, public object methods ...
			this.singleAbstractMethod[index] = method;
			break;
		}
		return this.singleAbstractMethod[index];
	}
	@Override
	public boolean mentionsAny(TypeBinding[] parameters, int idx) {
		return false;
	}
	/**
	 * @see org.eclipse.jdt.internal.compiler.lookup.Binding#readableName()
	 */
	@Override
	public char[] readableName(boolean showGenerics) /*java.lang.Object,  p.X<T> */ {
	    char[] readableName;
		if (isMemberType()) {
			readableName = CharOperation.concat(enclosingType().readableName(showGenerics && hasEnclosingInstanceContext()), this.sourceName, '.');
		} else {
			readableName = CharOperation.concatWith(actualType().compoundName, '.');
		}
		return readableName;
	}

	/**
	 * @see org.eclipse.jdt.internal.compiler.lookup.Binding#shortReadableName()
	 */
	@Override
	public char[] shortReadableName(boolean showGenerics) /*Object*/ {
	    char[] shortReadableName;
		if (isMemberType()) {
			shortReadableName = CharOperation.concat(enclosingType().shortReadableName(showGenerics && hasEnclosingInstanceContext()), this.sourceName, '.');
		} else {
			shortReadableName = actualType().sourceName;
		}
		return shortReadableName;
	}

	@Override
	void collectInferenceVariables(Set<InferenceVariable> variables) {
		// nothing to collect for a raw type.
	}

	@Override
	public ReferenceBinding upwardsProjection(Scope scope, TypeBinding[] mentionedTypeVariables) {
		return this;
	}

	@Override
	public ReferenceBinding downwardsProjection(Scope scope, TypeBinding[] mentionedTypeVariables) {
		return this;
	}

	@Override
	public ReferenceBinding enclosingType() {
		// ParameterizedTypeBinding earlier always had a resolved enclosed type, but now it does on-demand resolving.
		// Behaviour for RawTypeBinding should be unchanged.
	    return this.enclosingType;
	}
}
