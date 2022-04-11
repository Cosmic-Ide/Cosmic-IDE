/*******************************************************************************
 * Copyright (c) 2013, 2014 GK Software AG.
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
package org.eclipse.jdt.internal.compiler.ast;

import org.eclipse.jdt.internal.compiler.lookup.InferenceContext18;
import org.eclipse.jdt.internal.compiler.lookup.InvocationSite;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.ParameterizedGenericMethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.ParameterizedMethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;

/**
 * Abstraction for invocation AST nodes that can trigger overload resolution possibly involving type inference
*/
public interface Invocation extends InvocationSite {

	Expression[] arguments();

	/** Answer the resolved method binding of this invocation */
	MethodBinding binding();

	/**
	 * Register the given inference context, which produced the given method as its intermediate result.
	 * Later when the same method is selected as the most specific method, the inference context
	 * for this pair (Invocation x MethodBinding) can be looked up using {@link #getInferenceContext(ParameterizedMethodBinding)}
	 * to continue the type inference.
	 */
	void registerInferenceContext(ParameterizedGenericMethodBinding method, InferenceContext18 infCtx18);

	/**
	 * Retrieve an inference context for the given method.
	 * @param method an intermediate resolved candidate for this invocation
	 * return the associated inference context.
	 */
	InferenceContext18 getInferenceContext(ParameterizedMethodBinding method);

	/** Discard any state from type inference when compilation is done. */
	void cleanUpInferenceContexts();

	/** Record result against target type */
	void registerResult(TypeBinding targetType, MethodBinding method);

}
