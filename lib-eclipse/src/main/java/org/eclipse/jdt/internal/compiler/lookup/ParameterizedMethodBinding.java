/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
 *								bug 186342 - [compiler][null] Using annotations for null checking
 *								bug 392099 - [1.8][compiler][null] Apply null annotation on types for null analysis
 *								bug 392384 - [1.8][compiler][null] Restore nullness info from type annotations in class files
 *								Bug 415043 - [1.8][null] Follow-up re null type annotations after bug 392099
 *								Bug 417295 - [1.8[[null] Massage type annotated null analysis to gel well with deep encoded type bindings.
 *								Bug 425152 - [1.8] [compiler] Lambda Expression not resolved but flow analyzed leading to NPE.
 *								Bug 429958 - [1.8][null] evaluate new DefaultLocation attribute of @NonNullByDefault
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.lookup;

import org.eclipse.jdt.internal.compiler.ast.NullAnnotationMatching;
import org.eclipse.jdt.internal.compiler.ast.Wildcard;

/**
 * Binding denoting a method after type parameter substitutions got performed.
 * On parameterized type bindings, all methods got substituted, regardless whether
 * their signature did involve generics or not, so as to get the proper declaringClass for
 * these methods.
 */
public class ParameterizedMethodBinding extends MethodBinding {

	protected MethodBinding originalMethod;

	/**
	 * Create method of parameterized type, substituting original parameters/exception/return type with type arguments.
	 */
	public ParameterizedMethodBinding(final ParameterizedTypeBinding parameterizedDeclaringClass, MethodBinding originalMethod) {
		super(
				originalMethod.modifiers,
				originalMethod.selector,
				originalMethod.returnType,
				originalMethod.parameters,
				originalMethod.thrownExceptions,
				parameterizedDeclaringClass);
		this.originalMethod = originalMethod;
		/* missing type bit cannot be copied as is it might come from the return type or a parameter type that
		 * is substituted by a raw type.
		 */
		this.tagBits = originalMethod.tagBits & ~TagBits.HasMissingType;
		this.parameterNonNullness = originalMethod.parameterNonNullness;
		this.defaultNullness = originalMethod.defaultNullness;

		final TypeVariableBinding[] originalVariables = originalMethod.typeVariables;
		Substitution substitution = null;
		final int length = originalVariables.length;
		final boolean isStatic = originalMethod.isStatic();
		if (length == 0) {
			this.typeVariables = Binding.NO_TYPE_VARIABLES;
			if (!isStatic) substitution = parameterizedDeclaringClass;
		} else {
			// at least fix up the declaringElement binding + bound substitution if non static
			final TypeVariableBinding[] substitutedVariables = new TypeVariableBinding[length];
			for (int i = 0; i < length; i++) { // copy original type variable to relocate
				TypeVariableBinding originalVariable = originalVariables[i];
				substitutedVariables[i] = new TypeVariableBinding(originalVariable.sourceName, this, originalVariable.rank, parameterizedDeclaringClass.environment);
				substitutedVariables[i].tagBits |= (originalVariable.tagBits & (TagBits.AnnotationNullMASK|TagBits.HasNullTypeAnnotation));
			}
			this.typeVariables = substitutedVariables;

			// need to substitute old var refs with new ones (double substitution: declaringClass + new type variables)
			substitution = new Substitution() {
				@Override
				public LookupEnvironment environment() {
					return parameterizedDeclaringClass.environment;
				}
				@Override
				public boolean isRawSubstitution() {
					return !isStatic && parameterizedDeclaringClass.isRawSubstitution();
				}
				@Override
				public TypeBinding substitute(TypeVariableBinding typeVariable) {
					// check this variable can be substituted given copied variables
					if (typeVariable.rank < length && TypeBinding.equalsEquals(originalVariables[typeVariable.rank], typeVariable)) {
						TypeBinding substitute = substitutedVariables[typeVariable.rank];
						return typeVariable.hasTypeAnnotations() ? environment().createAnnotatedType(substitute, typeVariable.getTypeAnnotations()) : substitute;
					}
					if (!isStatic)
						return parameterizedDeclaringClass.substitute(typeVariable);
					return typeVariable;
				}
			};

			// initialize new variable bounds
			for (int i = 0; i < length; i++) {
				TypeVariableBinding originalVariable = originalVariables[i];
				TypeVariableBinding substitutedVariable = substitutedVariables[i];
				TypeBinding substitutedSuperclass = Scope.substitute(substitution, originalVariable.superclass);
				ReferenceBinding[] substitutedInterfaces = Scope.substitute(substitution, originalVariable.superInterfaces);
				if (originalVariable.firstBound != null) {
					TypeBinding firstBound;
					firstBound = TypeBinding.equalsEquals(originalVariable.firstBound, originalVariable.superclass)
						? substitutedSuperclass // could be array type or interface
						: substitutedInterfaces[0];
					substitutedVariable.setFirstBound(firstBound);
				}
				switch (substitutedSuperclass.kind()) {
					case Binding.ARRAY_TYPE :
						substitutedVariable.setSuperClass(parameterizedDeclaringClass.environment.getResolvedJavaBaseType(TypeConstants.JAVA_LANG_OBJECT, null));
						substitutedVariable.setSuperInterfaces(substitutedInterfaces);
						break;
					default:
						if (substitutedSuperclass.isInterface()) {
							substitutedVariable.setSuperClass(parameterizedDeclaringClass.environment.getResolvedType(TypeConstants.JAVA_LANG_OBJECT, null));
							int interfaceCount = substitutedInterfaces.length;
							System.arraycopy(substitutedInterfaces, 0, substitutedInterfaces = new ReferenceBinding[interfaceCount+1], 1, interfaceCount);
							substitutedInterfaces[0] = (ReferenceBinding) substitutedSuperclass;
							substitutedVariable.setSuperInterfaces(substitutedInterfaces);
						} else {
							substitutedVariable.setSuperClass((ReferenceBinding) substitutedSuperclass); // typeVar was extending other typeVar which got substituted with interface
							substitutedVariable.setSuperInterfaces(substitutedInterfaces);
						}
				}
			}
		}
		if (substitution != null) {
			this.returnType = Scope.substitute(substitution, this.returnType);
			this.parameters = Scope.substitute(substitution, this.parameters);
			this.thrownExceptions = Scope.substitute(substitution, this.thrownExceptions);
			// error case where exception type variable would have been substituted by a non-reference type (207573)
			if (this.thrownExceptions == null) this.thrownExceptions = Binding.NO_EXCEPTIONS;

			// after substitution transfer nullness information from type annotations:
			if (parameterizedDeclaringClass.environment.globalOptions.isAnnotationBasedNullAnalysisEnabled) {
				long returnNullBits = NullAnnotationMatching.validNullTagBits(this.returnType.tagBits);
				if (returnNullBits != 0L) {
					this.tagBits &= ~TagBits.AnnotationNullMASK;
					this.tagBits |= returnNullBits;
				}
				int parametersLen = this.parameters.length;
				for (int i=0; i<parametersLen; i++) {
					long paramTagBits = NullAnnotationMatching.validNullTagBits(this.parameters[i].tagBits);
					if (paramTagBits != 0) {
						if (this.parameterNonNullness == null)
							this.parameterNonNullness = new Boolean[parametersLen];
						this.parameterNonNullness[i] = Boolean.valueOf(paramTagBits == TagBits.AnnotationNonNull);
					}
				}
			}
		}
		checkMissingType: {
			if ((this.tagBits & TagBits.HasMissingType) != 0)
				break checkMissingType;
			if ((this.returnType.tagBits & TagBits.HasMissingType) != 0) {
				this.tagBits |=  TagBits.HasMissingType;
				break checkMissingType;
			}
			for (int i = 0, max = this.parameters.length; i < max; i++) {
				if ((this.parameters[i].tagBits & TagBits.HasMissingType) != 0) {
					this.tagBits |=  TagBits.HasMissingType;
					break checkMissingType;
				}
			}
			for (int i = 0, max = this.thrownExceptions.length; i < max; i++) {
				if ((this.thrownExceptions[i].tagBits & TagBits.HasMissingType) != 0) {
					this.tagBits |=  TagBits.HasMissingType;
					break checkMissingType;
				}
			}
		}
	}

	/**
	 * Create method of parameterized type, substituting original parameters/exception/return type with type arguments.
	 * This is a CODE ASSIST method ONLY.
	 */
	public ParameterizedMethodBinding(final ReferenceBinding declaringClass, MethodBinding originalMethod, char[][] alternateParamaterNames, final LookupEnvironment environment) {
		super(
				originalMethod.modifiers,
				originalMethod.selector,
				 originalMethod.returnType,
				originalMethod.parameters,
				originalMethod.thrownExceptions,
				declaringClass);
		this.originalMethod = originalMethod;
		/* missing type bit cannot be copied as is it might come from the return type or a parameter type that
		 * is substituted by a raw type.
		 */
		this.tagBits = originalMethod.tagBits & ~TagBits.HasMissingType;
		this.parameterNonNullness = originalMethod.parameterNonNullness;
		this.defaultNullness = originalMethod.defaultNullness;

		final TypeVariableBinding[] originalVariables = originalMethod.typeVariables;
		Substitution substitution = null;
		final int length = originalVariables.length;
		if (length == 0) {
			this.typeVariables = Binding.NO_TYPE_VARIABLES;
		} else {
			// at least fix up the declaringElement binding + bound substitution if non static
			final TypeVariableBinding[] substitutedVariables = new TypeVariableBinding[length];
			for (int i = 0; i < length; i++) { // copy original type variable to relocate
				TypeVariableBinding originalVariable = originalVariables[i];
				substitutedVariables[i] = new TypeVariableBinding(
						alternateParamaterNames == null ?
								originalVariable.sourceName :
								alternateParamaterNames[i],
							this,
							originalVariable.rank,
							environment);
				substitutedVariables[i].tagBits |= (originalVariable.tagBits & (TagBits.AnnotationNullMASK|TagBits.HasNullTypeAnnotation));
			}
			this.typeVariables = substitutedVariables;

			// need to substitute old var refs with new ones (double substitution: declaringClass + new type variables)
			substitution = new Substitution() {
				@Override
				public LookupEnvironment environment() {
					return environment;
				}
				@Override
				public boolean isRawSubstitution() {
					return false;
				}
				@Override
				public TypeBinding substitute(TypeVariableBinding typeVariable) {
			        // check this variable can be substituted given copied variables
			        if (typeVariable.rank < length && TypeBinding.equalsEquals(originalVariables[typeVariable.rank], typeVariable)) {
			        	TypeBinding substitute = substitutedVariables[typeVariable.rank];
						return typeVariable.hasTypeAnnotations() ? environment().createAnnotatedType(substitute, typeVariable.getTypeAnnotations()) : substitute;
			        }
			        return typeVariable;
				}
			};

			// initialize new variable bounds
			for (int i = 0; i < length; i++) {
				TypeVariableBinding originalVariable = originalVariables[i];
				TypeVariableBinding substitutedVariable = substitutedVariables[i];
				TypeBinding substitutedSuperclass = Scope.substitute(substitution, originalVariable.superclass);
				ReferenceBinding[] substitutedInterfaces = Scope.substitute(substitution, originalVariable.superInterfaces);
				if (originalVariable.firstBound != null) {
					TypeBinding firstBound;
					firstBound = TypeBinding.equalsEquals(originalVariable.firstBound, originalVariable.superclass)
						? substitutedSuperclass // could be array type or interface
						: substitutedInterfaces[0];
					substitutedVariable.setFirstBound(firstBound);
				}
				switch (substitutedSuperclass.kind()) {
					case Binding.ARRAY_TYPE :
						substitutedVariable.setSuperClass(environment.getResolvedJavaBaseType(TypeConstants.JAVA_LANG_OBJECT, null));
						substitutedVariable.setSuperInterfaces(substitutedInterfaces);
						break;
					default:
						if (substitutedSuperclass.isInterface()) {
							substitutedVariable.setSuperClass(environment.getResolvedType(TypeConstants.JAVA_LANG_OBJECT, null));
							int interfaceCount = substitutedInterfaces.length;
							System.arraycopy(substitutedInterfaces, 0, substitutedInterfaces = new ReferenceBinding[interfaceCount+1], 1, interfaceCount);
							substitutedInterfaces[0] = (ReferenceBinding) substitutedSuperclass;
							substitutedVariable.setSuperInterfaces(substitutedInterfaces);
						} else {
							substitutedVariable.setSuperClass((ReferenceBinding) substitutedSuperclass); // typeVar was extending other typeVar which got substituted with interface
							substitutedVariable.setSuperInterfaces(substitutedInterfaces);
						}
				}
			}
		}
		if (substitution != null) {
			this.returnType = Scope.substitute(substitution, this.returnType);
			this.parameters = Scope.substitute(substitution, this.parameters);
			this.thrownExceptions = Scope.substitute(substitution, this.thrownExceptions);
		    // error case where exception type variable would have been substituted by a non-reference type (207573)
		    if (this.thrownExceptions == null) this.thrownExceptions = Binding.NO_EXCEPTIONS;
		}
		checkMissingType: {
			if ((this.tagBits & TagBits.HasMissingType) != 0)
				break checkMissingType;
			if ((this.returnType.tagBits & TagBits.HasMissingType) != 0) {
				this.tagBits |=  TagBits.HasMissingType;
				break checkMissingType;
			}
			for (int i = 0, max = this.parameters.length; i < max; i++) {
				if ((this.parameters[i].tagBits & TagBits.HasMissingType) != 0) {
					this.tagBits |=  TagBits.HasMissingType;
					break checkMissingType;
				}
			}
			for (int i = 0, max = this.thrownExceptions.length; i < max; i++) {
				if ((this.thrownExceptions[i].tagBits & TagBits.HasMissingType) != 0) {
					this.tagBits |=  TagBits.HasMissingType;
					break checkMissingType;
				}
			}
		}
	}

	public ParameterizedMethodBinding() {
		// no init
	}

	/**
	 * The type of x.getClass() is substituted from 'Class<? extends Object>' into: 'Class<? extends raw(X)>
	 */
	public static ParameterizedMethodBinding instantiateGetClass(TypeBinding receiverType, MethodBinding originalMethod, Scope scope) {
		ParameterizedMethodBinding method = new ParameterizedMethodBinding();
		method.modifiers = originalMethod.modifiers;
		method.selector = originalMethod.selector;
		method.declaringClass = originalMethod.declaringClass;
		method.typeVariables = Binding.NO_TYPE_VARIABLES;
		method.originalMethod = originalMethod;
		method.parameters = originalMethod.parameters;
		method.thrownExceptions = originalMethod.thrownExceptions;
		method.tagBits = originalMethod.tagBits;
		ReferenceBinding genericClassType = scope.getJavaLangClass();
		LookupEnvironment environment = scope.environment();
		TypeBinding rawType = environment.convertToRawType(receiverType.erasure(), false /*do not force conversion of enclosing types*/);
		if (environment.usesNullTypeAnnotations())
			rawType = environment.createAnnotatedType(rawType, new AnnotationBinding[] { environment.getNonNullAnnotation() });
		method.returnType = environment.createParameterizedType(
			genericClassType,
			new TypeBinding[] {  environment.createWildcard(genericClassType, 0, rawType, null /*no extra bound*/, Wildcard.EXTENDS) },
			null);
		if (environment.globalOptions.isAnnotationBasedNullAnalysisEnabled) {
			if (environment.usesNullTypeAnnotations())
				method.returnType = environment.createAnnotatedType(method.returnType, new AnnotationBinding[] { environment.getNonNullAnnotation() });
			else
				method.tagBits |= TagBits.AnnotationNonNull;
		}
		if ((method.returnType.tagBits & TagBits.HasMissingType) != 0) {
			method.tagBits |=  TagBits.HasMissingType;
		}
		return method;
	}

	/**
	 * Returns true if some parameters got substituted.
	 */
	@Override
	public boolean hasSubstitutedParameters() {
		return this.parameters != this.originalMethod.parameters;
	}

	/**
	 * Returns true if the return type got substituted.
	 */
	@Override
	public boolean hasSubstitutedReturnType() {
		return this.returnType != this.originalMethod.returnType; //$IDENTITY-COMPARISON$
	}

	/**
	 * Returns the original method (as opposed to parameterized instances)
	 */
	@Override
	public MethodBinding original() {
		return this.originalMethod.original();
	}


	@Override
	public MethodBinding shallowOriginal() {
		return this.originalMethod;
	}
}
