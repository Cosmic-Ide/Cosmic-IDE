/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
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
 *							Bug 452788 - [1.8][compiler] Type not correctly inferred in lambda expression
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.ast;

import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.InferenceContext18;
import org.eclipse.jdt.internal.compiler.lookup.InvocationSite;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.PolyTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.Scope;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;

/**
 	Contract to be implemented by all poly expressions and potential poly expressions for uniform integration into overload resolution and type inference.
	Additional contracts may be imposed by {@link Invocation} and {@link InvocationSite}. For most contracts "default" implementations are furnished by
	{@link Expression} or {@link Statement} or by {@link ASTNode} and the poly expression should suitably override where required.

	@see PolyTypeBinding
	@see ExpressionContext
*/
public interface IPolyExpression {

	// Expression context manipulation
	public void setExpressionContext(ExpressionContext context);
	public ExpressionContext getExpressionContext();

	// Target type injection.
	public void setExpectedType(TypeBinding targetType);
	public TypeBinding invocationTargetType();

	// Compatibility checks.
	public boolean isPotentiallyCompatibleWith(TypeBinding targetType, Scope scope);
	public boolean isCompatibleWith(TypeBinding targetType, final Scope scope);
	public boolean isBoxingCompatibleWith(TypeBinding targetType, Scope scope);
	public boolean sIsMoreSpecific(TypeBinding s, TypeBinding t, Scope skope);

	// Pertinence checks.
	public boolean isPertinentToApplicability(TypeBinding targetType, MethodBinding method);

	// Polyness checks
	public boolean isPolyExpression(MethodBinding candidate);
	public boolean isPolyExpression();
	public boolean isFunctionalType();
	public Expression[] getPolyExpressions();


	/* Resolution: A poly expression must be prepared to be resolved multiple times and should manage matters in a side effect free fashion.
	   Typically, in invocation contexts, there is an initial resolution, multiple tentative resolutions and then a final resolution against
	   the ultimate target type.
	*/
	public TypeBinding resolveType(BlockScope blockScope);
	// Resolve expression tentatively - should have no lingering side-effects that may impact final resolution !
	public Expression resolveExpressionExpecting(TypeBinding targetType, Scope scope, InferenceContext18 inferenceContext);

}