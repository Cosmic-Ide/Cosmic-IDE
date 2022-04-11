/*******************************************************************************
 * Copyright (c) 2014, 2017 GK Software AG.
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
package org.eclipse.jdt.internal.compiler.lookup;

import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;

/**
 * Encodes a synthetic &lt;factory&gt; method used for resolving a diamond constructor.
 */
public class SyntheticFactoryMethodBinding extends MethodBinding {

	private MethodBinding staticFactoryFor;
	private LookupEnvironment environment;
	private ReferenceBinding enclosingType;

	public SyntheticFactoryMethodBinding(MethodBinding method, LookupEnvironment environment, ReferenceBinding enclosingType) {
		super(method.modifiers | ClassFileConstants.AccStatic, TypeConstants.SYNTHETIC_STATIC_FACTORY,
				null, null, null, method.declaringClass);
		this.environment = environment;
		this.staticFactoryFor = method;
		this.enclosingType = enclosingType;
	}

	public MethodBinding getConstructor() {
		return this.staticFactoryFor;
	}

	/** Apply the given type arguments on the (declaring class of the) actual constructor being represented by this factory method and
	    if method type arguments is not empty materialize the parameterized generic constructor
	 * @param targetType
	*/
	public ParameterizedMethodBinding applyTypeArgumentsOnConstructor(TypeBinding[] typeArguments, TypeBinding[] constructorTypeArguments, boolean inferredWithUncheckedConversion, TypeBinding targetType) {
		ReferenceBinding parameterizedType = typeArguments == null
				? this.environment.createRawType(this.declaringClass, this.enclosingType)
				: this.environment.createParameterizedType(this.declaringClass, typeArguments, this.enclosingType);
		for (MethodBinding parameterizedMethod : parameterizedType.methods()) {
			if (parameterizedMethod.original() == this.staticFactoryFor)
				return (constructorTypeArguments.length > 0 || inferredWithUncheckedConversion)
						? this.environment.createParameterizedGenericMethod(parameterizedMethod, constructorTypeArguments, inferredWithUncheckedConversion, false, targetType)
						: (ParameterizedMethodBinding) parameterizedMethod;
			if (parameterizedMethod instanceof ProblemMethodBinding) {
				MethodBinding closestMatch = ((ProblemMethodBinding)parameterizedMethod).closestMatch;
				if (closestMatch instanceof ParameterizedMethodBinding && closestMatch.original() == this.staticFactoryFor)
					return (ParameterizedMethodBinding) closestMatch;
			}
		}
		throw new IllegalArgumentException("Type doesn't have its own method?"); //$NON-NLS-1$
	}
}
