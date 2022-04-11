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
 *     Stephan Herrmann - Contribution for
 *								Bug 429813 - [1.8][dom ast] IMethodBinding#getJavaElement() should return IMethod for lambda
 *******************************************************************************/

package org.eclipse.jdt.core.dom;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.lookup.ExtraCompilerModifiers;
import org.eclipse.jdt.internal.compiler.lookup.LookupEnvironment;
import org.eclipse.jdt.internal.compiler.lookup.ParameterizedGenericMethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.RawTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.SyntheticMethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.TagBits;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeVariableBinding;
import org.eclipse.jdt.internal.compiler.problem.AbortCompilation;
import org.eclipse.jdt.internal.core.JavaElement;
import org.eclipse.jdt.internal.core.util.Util;

/**
 * Internal implementation of method bindings.
 */
class MethodBinding implements IMethodBinding {

	private static final int VALID_MODIFIERS = Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE |
		Modifier.ABSTRACT | Modifier.STATIC | Modifier.FINAL | Modifier.SYNCHRONIZED | Modifier.NATIVE |
		Modifier.STRICTFP | Modifier.DEFAULT;
	private static final ITypeBinding[] NO_TYPE_BINDINGS = new ITypeBinding[0];
	static final IVariableBinding[] NO_VARIABLE_BINDINGS = new IVariableBinding[0];
	protected org.eclipse.jdt.internal.compiler.lookup.MethodBinding binding;
	protected BindingResolver resolver;
	private volatile ITypeBinding[] parameterTypes;
	private volatile ITypeBinding[] exceptionTypes;
	private volatile String name;
	private volatile ITypeBinding declaringClass;
	private volatile ITypeBinding returnType;
	private volatile String key;
	private volatile ITypeBinding[] typeParameters;
	private volatile ITypeBinding[] typeArguments;
	private volatile IAnnotationBinding[] annotations;
	private volatile IAnnotationBinding[][] parameterAnnotations;

	MethodBinding(BindingResolver resolver, org.eclipse.jdt.internal.compiler.lookup.MethodBinding binding) {
		this.resolver = resolver;
		this.binding = binding;
	}

	@Override
	public boolean isAnnotationMember() {
		return getDeclaringClass().isAnnotation();
	}

	/**
	 * @see IMethodBinding#isConstructor()
	 */
	@Override
	public boolean isConstructor() {
		return this.binding.isConstructor();
	}

	/**
	 * @see IMethodBinding#isCompactConstructor()
	 */
	@Override
	public boolean isCompactConstructor() {
		return this.binding.isCompactConstructor();
	}

	/**
	 * @see IMethodBinding#isCanonicalConstructor()
	 */
	@Override
	public boolean isCanonicalConstructor() {
		return this.binding.isCanonicalConstructor();
	}

	/**
	 * @see IMethodBinding#isDefaultConstructor()
	 * @since 3.0
	 */
	@Override
	public boolean isDefaultConstructor() {
		final ReferenceBinding declaringClassBinding = this.binding.declaringClass;
		if (declaringClassBinding.isRawType()) {
			RawTypeBinding rawTypeBinding = (RawTypeBinding) declaringClassBinding;
			if (rawTypeBinding.genericType().isBinaryBinding()) {
				return false;
			}
			return (this.binding.modifiers & ExtraCompilerModifiers.AccIsDefaultConstructor) != 0;
		}
		if (declaringClassBinding.isBinaryBinding()) {
			return false;
		}
		return (this.binding.modifiers & ExtraCompilerModifiers.AccIsDefaultConstructor) != 0;
	}

	/**
	 * @see IBinding#getName()
	 */
	@Override
	public String getName() {
		if (this.name == null) {
			if (this.binding.isConstructor()) {
				this.name = getDeclaringClass().getName();
			} else {
				this.name = new String(this.binding.selector);
			}
		}
		return this.name;
	}

	@Override
	public IAnnotationBinding[] getAnnotations() {
		if (this.annotations != null) {
			return this.annotations;
		}
		org.eclipse.jdt.internal.compiler.lookup.AnnotationBinding[] internalAnnotations = this.binding.getAnnotations();
		return this.annotations = filterTypeAnnotations(internalAnnotations);
	}

	/**
	 * @see IMethodBinding#getDeclaringClass()
	 */
	@Override
	public ITypeBinding getDeclaringClass() {
		if (this.declaringClass == null) {
			this.declaringClass = this.resolver.getTypeBinding(this.binding.declaringClass);
		}
		return this.declaringClass;
	}

	@Override
	public IBinding getDeclaringMember() {
		return null;
	}

	@Override
	public IAnnotationBinding[] getParameterAnnotations(int index) {
		if (getParameterTypes() == NO_TYPE_BINDINGS) {
			return AnnotationBinding.NoAnnotations;
		}
		if (this.parameterAnnotations != null) {
			return this.parameterAnnotations[index];
		}
		org.eclipse.jdt.internal.compiler.lookup.AnnotationBinding[][] bindingAnnotations = this.binding.getParameterAnnotations();
		if (bindingAnnotations == null) return AnnotationBinding.NoAnnotations;

		int length = bindingAnnotations.length;
		IAnnotationBinding[][] domAnnotations = new IAnnotationBinding[length][];
		for (int i = 0; i < length; i++) {
			org.eclipse.jdt.internal.compiler.lookup.AnnotationBinding[] paramBindingAnnotations = bindingAnnotations[i];
			int pLength = paramBindingAnnotations.length;
			domAnnotations[i] = new AnnotationBinding[pLength];
			for (int j=0; j<pLength; j++) {
				IAnnotationBinding domAnnotation = this.resolver.getAnnotationInstance(paramBindingAnnotations[j]);
				if (domAnnotation == null) {
					domAnnotations[i] = AnnotationBinding.NoAnnotations;
					break;
				}
				domAnnotations[i][j] = domAnnotation;
			}
		}
		this.parameterAnnotations = domAnnotations;

		return this.parameterAnnotations[index];
	}

	/**
	 * @see IMethodBinding#getParameterTypes()
	 */
	@Override
	public ITypeBinding[] getParameterTypes() {
		if (this.parameterTypes != null) {
			return this.parameterTypes;
		}
		org.eclipse.jdt.internal.compiler.lookup.TypeBinding[] parameters = this.binding.parameters;
		int length = parameters == null ? 0 : parameters.length;
		if (length == 0) {
			return this.parameterTypes = NO_TYPE_BINDINGS;
		} else {
			ITypeBinding[] paramTypes = new ITypeBinding[length];
			for (int i = 0; i < length; i++) {
				final TypeBinding parameterBinding = parameters[i];
				if (parameterBinding != null) {
					ITypeBinding typeBinding = this.resolver.getTypeBinding(parameterBinding);
					if (typeBinding == null) {
						return this.parameterTypes = NO_TYPE_BINDINGS;
					}
					paramTypes[i] = typeBinding;
				} else {
					// log error
					StringBuilder message = new StringBuilder("Report method binding where a parameter is null:\n");  //$NON-NLS-1$
					message.append(toString());
					Util.log(new IllegalArgumentException(), message.toString());
					// report no binding since one or more parameter has no binding
					return this.parameterTypes = NO_TYPE_BINDINGS;
				}
			}
			return this.parameterTypes = paramTypes;
		}
	}

	/**
	 * @see IMethodBinding#getDeclaredReceiverType()
	 */
	@Override
	public ITypeBinding getDeclaredReceiverType() {
		return this.resolver.getTypeBinding(this.binding.receiver);
	}
	/**
	 * @see IMethodBinding#getReturnType()
	 */
	@Override
	public ITypeBinding getReturnType() {
		if (this.returnType == null) {
			this.returnType = this.resolver.getTypeBinding(this.binding.returnType);
		}
		return this.returnType;
	}

	protected IAnnotationBinding[] filterTypeAnnotations(org.eclipse.jdt.internal.compiler.lookup.AnnotationBinding[] internalAnnotations) {
		int length = internalAnnotations == null ? 0 : internalAnnotations.length;
		if (length != 0) {
			IAnnotationBinding[] tempAnnotations = new IAnnotationBinding[length];
			int convertedAnnotationCount = 0;
			final boolean isConstructor = this.isConstructor();
			for (int i = 0; i < length; i++) {
				org.eclipse.jdt.internal.compiler.lookup.AnnotationBinding internalAnnotation = internalAnnotations[i];
				final ReferenceBinding annotationType = internalAnnotation.getAnnotationType();
				long metaTagBits = annotationType.getAnnotationTagBits();

				// Exclude all other targets including TYPE_USE, even though TYPE_USE is accepted.
				if (isConstructor && (metaTagBits & TagBits.AnnotationForConstructor) == 0 &&
						((metaTagBits & TagBits.AnnotationTargetMASK) != 0)) {
					continue;
				}

				final IAnnotationBinding annotationInstance = this.resolver.getAnnotationInstance(internalAnnotation);
				if (annotationInstance == null) {
					continue;
				}
				tempAnnotations[convertedAnnotationCount++] = annotationInstance;
			}
			if (convertedAnnotationCount == length) return tempAnnotations;
			if (convertedAnnotationCount == 0) return AnnotationBinding.NoAnnotations;

			System.arraycopy(tempAnnotations, 0, (tempAnnotations = new IAnnotationBinding[convertedAnnotationCount]), 0, convertedAnnotationCount);
			return tempAnnotations;
		}
		return AnnotationBinding.NoAnnotations;
	}

	@Override
	public Object getDefaultValue() {
		if (isAnnotationMember())
			return MemberValuePairBinding.buildDOMValue(this.binding.getDefaultValue(), this.resolver);
		return null;
	}

	/**
	 * @see IMethodBinding#getExceptionTypes()
	 */
	@Override
	public ITypeBinding[] getExceptionTypes() {
		if (this.exceptionTypes != null) {
			return this.exceptionTypes;
		}
		org.eclipse.jdt.internal.compiler.lookup.TypeBinding[] exceptions = this.binding.thrownExceptions;
		int length = exceptions == null ? 0 : exceptions.length;
		if (length == 0) {
			return this.exceptionTypes = NO_TYPE_BINDINGS;
		}
		ITypeBinding[] exTypes = new ITypeBinding[length];
		for (int i = 0; i < length; i++) {
			ITypeBinding typeBinding = this.resolver.getTypeBinding(exceptions[i]);
			if (typeBinding == null) {
				return this.exceptionTypes = NO_TYPE_BINDINGS;
			}
			exTypes[i] = typeBinding;
		}
		return this.exceptionTypes = exTypes;
	}

	@Override
	public IJavaElement getJavaElement() {
		JavaElement element = getUnresolvedJavaElement();
		if (element == null)
			return null;
		return element.resolved(this.binding);
	}

	private JavaElement getUnresolvedJavaElement() {
		if (JavaCore.getPlugin() == null) {
			return null;
		}
		if (!(this.resolver instanceof DefaultBindingResolver)) return null;

		DefaultBindingResolver defaultBindingResolver = (DefaultBindingResolver) this.resolver;
		if (!defaultBindingResolver.fromJavaProject) return null;
		return Util.getUnresolvedJavaElement(
				this.binding,
				defaultBindingResolver.workingCopyOwner,
				defaultBindingResolver.getBindingsToNodesMap());
	}

	/**
	 * @see IBinding#getKind()
	 */
	@Override
	public int getKind() {
		return IBinding.METHOD;
	}

	/**
	 * @see IBinding#getModifiers()
	 */
	@Override
	public int getModifiers() {
		return this.binding.getAccessFlags() & VALID_MODIFIERS;
	}

	/**
	 * @see IBinding#isDeprecated()
	 */
	@Override
	public boolean isDeprecated() {
		return this.binding.isDeprecated();
	}

	/**
	 * @see IBinding#isRecovered()
	 */
	@Override
	public boolean isRecovered() {
		return false;
	}

	/**
	 * @see IBinding#isSynthetic()
	 */
	@Override
	public boolean isSynthetic() {
		return this.binding.isSynthetic();
	}

	/**
	 * @see org.eclipse.jdt.core.dom.IMethodBinding#isVarargs()
	 * @since 3.1
	 */
	@Override
	public boolean isVarargs() {
		return this.binding.isVarargs();
	}

	/**
	 * @see IBinding#getKey()
	 */
	@Override
	public String getKey() {
		if (this.key == null) {
			this.key = new String(this.binding.computeUniqueKey());
		}
		return this.key;
	}

	/**
	 * @see IBinding#isEqualTo(IBinding)
	 * @since 3.1
	 */
	@Override
	public boolean isEqualTo(IBinding other) {
		if (other == this) {
			// identical binding - equal (key or no key)
			return true;
		}
		if (other == null) {
			// other binding missing
			return false;
		}
		if (!(other instanceof MethodBinding)) {
			return false;
		}
		org.eclipse.jdt.internal.compiler.lookup.MethodBinding otherBinding = ((MethodBinding) other).binding;
		return BindingComparator.isEqual(this.binding, otherBinding);
	}

	/**
	 * @see org.eclipse.jdt.core.dom.IMethodBinding#getTypeParameters()
	 */
	@Override
	public ITypeBinding[] getTypeParameters() {
		if (this.typeParameters != null) {
			return this.typeParameters;
		}
		TypeVariableBinding[] typeVariableBindings = this.binding.typeVariables();
		int typeVariableBindingsLength = typeVariableBindings == null ? 0 : typeVariableBindings.length;
		if (typeVariableBindingsLength == 0) {
			return this.typeParameters = NO_TYPE_BINDINGS;
		}
		ITypeBinding[] tParameters = new ITypeBinding[typeVariableBindingsLength];
		for (int i = 0; i < typeVariableBindingsLength; i++) {
			ITypeBinding typeBinding = this.resolver.getTypeBinding(typeVariableBindings[i]);
			if (typeBinding == null) {
				return this.typeParameters = NO_TYPE_BINDINGS;
			}
			tParameters[i] = typeBinding;
		}
		return this.typeParameters = tParameters;
	}

	/**
	 * @see org.eclipse.jdt.core.dom.IMethodBinding#isGenericMethod()
	 * @since 3.1
	 */
	@Override
	public boolean isGenericMethod() {
		// equivalent to return getTypeParameters().length > 0;
		if (this.typeParameters != null) {
			return this.typeParameters.length > 0;
		}
		TypeVariableBinding[] typeVariableBindings = this.binding.typeVariables();
		return (typeVariableBindings != null && typeVariableBindings.length > 0);
	}

	/**
	 * @see org.eclipse.jdt.core.dom.IMethodBinding#getTypeArguments()
	 */
	@Override
	public ITypeBinding[] getTypeArguments() {
		if (this.typeArguments != null) {
			return this.typeArguments;
		}

		if (this.binding instanceof ParameterizedGenericMethodBinding) {
			ParameterizedGenericMethodBinding genericMethodBinding = (ParameterizedGenericMethodBinding) this.binding;
			org.eclipse.jdt.internal.compiler.lookup.TypeBinding[] typeArgumentsBindings = genericMethodBinding.typeArguments;
			int typeArgumentsLength = typeArgumentsBindings == null ? 0 : typeArgumentsBindings.length;
			if (typeArgumentsLength != 0) {
				ITypeBinding[] tArguments = new ITypeBinding[typeArgumentsLength];
				for (int i = 0; i < typeArgumentsLength; i++) {
					ITypeBinding typeBinding = this.resolver.getTypeBinding(typeArgumentsBindings[i]);
					if (typeBinding == null) {
						return this.typeArguments = NO_TYPE_BINDINGS;
					}
					tArguments[i] = typeBinding;
				}
				return this.typeArguments = tArguments;
			}
		}
		return this.typeArguments = NO_TYPE_BINDINGS;
	}

	/**
	 * @see org.eclipse.jdt.core.dom.IMethodBinding#isParameterizedMethod()
	 */
	@Override
	public boolean isParameterizedMethod() {
		return (this.binding instanceof ParameterizedGenericMethodBinding)
			&& !((ParameterizedGenericMethodBinding) this.binding).isRaw;
	}

	/**
	 * @see org.eclipse.jdt.core.dom.IMethodBinding#isRawMethod()
	 */
	@Override
	public boolean isRawMethod() {
		return (this.binding instanceof ParameterizedGenericMethodBinding)
			&& ((ParameterizedGenericMethodBinding) this.binding).isRaw;
	}

	@Override
	public boolean isSubsignature(IMethodBinding otherMethod) {
		try {
			LookupEnvironment lookupEnvironment = this.resolver.lookupEnvironment();
			return lookupEnvironment != null
				&& lookupEnvironment.methodVerifier().isMethodSubsignature(this.binding, ((MethodBinding) otherMethod).binding);
		} catch (AbortCompilation e) {
			// don't surface internal exception to clients
			// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=143013
			return false;
		}
	}

	/**
	 * @see org.eclipse.jdt.core.dom.IMethodBinding#getMethodDeclaration()
	 */
	@Override
	public IMethodBinding getMethodDeclaration() {
		return this.resolver.getMethodBinding(this.binding.original());
	}

	/**
	 * @see IMethodBinding#overrides(IMethodBinding)
	 */
	@Override
	public boolean overrides(IMethodBinding otherMethod) {
			LookupEnvironment lookupEnvironment = this.resolver.lookupEnvironment();
			return lookupEnvironment != null
				&& lookupEnvironment.methodVerifier().doesMethodOverride(this.binding, ((MethodBinding) otherMethod).binding);
	}

	/**
	 * For debugging purpose only.
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return this.binding.toString();
	}

	/*
	 * Method binding representing a lambda expression.
	 * Most properties are read from the SAM descriptor,
	 * but key, parameter types, and annotations are taken from the lambda implementation.
	 * Additionally we store the declaring member (see #getDeclaringMember()).
	 */
	static class LambdaMethod extends MethodBinding {

		private MethodBinding implementation;
		private IBinding declaringMember;
		private IVariableBinding[] syntheticOuterLocalVariables;

		public LambdaMethod(DefaultBindingResolver resolver,
							org.eclipse.jdt.internal.compiler.lookup.MethodBinding lambdaDescriptor,
							org.eclipse.jdt.internal.compiler.lookup.MethodBinding implementation,
							IBinding declaringMember)
		{
			super(resolver, lambdaDescriptor);
			this.implementation = new MethodBinding(resolver, implementation);
			this.declaringMember = declaringMember;
		}

		/**
		 * @see IBinding#getModifiers()
		 */
		@Override
		public int getModifiers() {
			return super.getModifiers() & ~ClassFileConstants.AccAbstract;
		}

		/**
		 * @see IBinding#getKey()
		 */
		@Override
		public String getKey() {
			return this.implementation.getKey();
		}

		@Override
		public ITypeBinding[] getParameterTypes() {
			return this.implementation.getParameterTypes();
		}

		@Override
		public IAnnotationBinding[] getParameterAnnotations(int paramIndex) {
			return this.implementation.getParameterAnnotations(paramIndex);
		}

		@Override
		public IAnnotationBinding[] getAnnotations() {
			return this.implementation.getAnnotations();
		}

		@Override
		public IBinding getDeclaringMember() {
			return this.declaringMember;
		}

		@Override
		public IMethodBinding getMethodDeclaration() {
			return this.resolver.getMethodBinding(this.binding);
		}

		@Override
		public String toString() {
			return super.toString().replace("public abstract ", "public ");  //$NON-NLS-1$//$NON-NLS-2$
		}

		@Override
		public IVariableBinding[] getSyntheticOuterLocals() {
			if (this.syntheticOuterLocalVariables != null) {
				return this.syntheticOuterLocalVariables;
			}
			return NO_VARIABLE_BINDINGS;
		}

		public  void setSyntheticOuterLocals(IVariableBinding[] syntheticOuterLocalVariables) {
			this.syntheticOuterLocalVariables = syntheticOuterLocalVariables;
		}
	}

	@Override
	public IVariableBinding[] getSyntheticOuterLocals() {
		return NO_VARIABLE_BINDINGS;
	}

	@Override
	public boolean isSyntheticRecordMethod() {
		return ((getDeclaringClass().isRecord()) &&
				(this.binding instanceof SyntheticMethodBinding));
	}
}