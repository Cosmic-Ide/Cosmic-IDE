/*******************************************************************************
 * Copyright (c) 2013, 2015 GK Software AG.
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
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.lookup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.internal.compiler.ast.FunctionalExpression;
import org.eclipse.jdt.internal.compiler.ast.LambdaExpression;
import org.eclipse.jdt.internal.compiler.ast.ReferenceExpression;

/**
 * Constraint formula expressing that a given expression must have an exception type.
 * <ul>
 * <li>Expression contains<sub>throws</sub> T</li>
 * </ul>
 */
public class ConstraintExceptionFormula extends ConstraintFormula {

	FunctionalExpression left;

	public ConstraintExceptionFormula(FunctionalExpression left, TypeBinding type) {
		this.left = left;
		this.right = type;
		this.relation = EXCEPTIONS_CONTAINED;
	}

	@Override
	public Object reduce(InferenceContext18 inferenceContext) {
		// JLS 18.2.5
		Scope scope = inferenceContext.scope;
		if (!this.right.isFunctionalInterface(scope))
			return FALSE;
		MethodBinding sam = this.right.getSingleAbstractMethod(scope, true);
		if (sam == null)
			return FALSE;
		if (this.left instanceof LambdaExpression) {
			if (((LambdaExpression)this.left).argumentsTypeElided()) {
				int nParam = sam.parameters.length;
				for (int i = 0; i < nParam; i++)
					if (!sam.parameters[i].isProperType(true))
						return FALSE;
			}
			if (sam.returnType != TypeBinding.VOID && !sam.returnType.isProperType(true))
				return FALSE;
		} else { // reference expression
			if (!((ReferenceExpression)this.left).isExactMethodReference()) {
				int nParam = sam.parameters.length;
				for (int i = 0; i < nParam; i++)
					if (!sam.parameters[i].isProperType(true))
						return FALSE;
				if (sam.returnType != TypeBinding.VOID && !sam.returnType.isProperType(true))
					return FALSE;
			}
		}
		TypeBinding[] thrown = sam.thrownExceptions;
		InferenceVariable[] e = new InferenceVariable[thrown.length];
		int n = 0;
		for (int i = 0; i < thrown.length; i++)
			if (!thrown[i].isProperType(true))
				e[n++] = (InferenceVariable) thrown[i]; // thrown[i] is not a proper type, since it's an exception it must be an inferenceVariable, right?

		/* If throw specification does not encode any type parameters, there are no constraints to be gleaned/gathered from the throw sites.
		   See also that thrown exceptions are not allowed to influence compatibility and overload resolution.
		*/
		if (n == 0)
			return TRUE;

		TypeBinding[] ePrime = null;
		if (this.left instanceof LambdaExpression) {
			LambdaExpression lambda = ((LambdaExpression) this.left).resolveExpressionExpecting(this.right, inferenceContext.scope, inferenceContext);
			if (lambda == null)
				return TRUE; // cannot make use of this buggy constraint
			Set<TypeBinding> ePrimeSet = lambda.getThrownExceptions();
			ePrime = ePrimeSet.toArray(new TypeBinding[ePrimeSet.size()]);
		} else {
			ReferenceExpression referenceExpression = ((ReferenceExpression) this.left).resolveExpressionExpecting(this.right, scope, inferenceContext);
			MethodBinding method = referenceExpression != null ? referenceExpression.binding : null;
			if (method != null)
				ePrime = method.thrownExceptions;
		}
		if (ePrime == null)
			return TRUE;
		int m = ePrime.length;
		List<ConstraintFormula> result = new ArrayList<>();
		actual: for (int i = 0; i < m; i++) {
			if (ePrime[i].isUncheckedException(false))
				continue;
			for (int j = 0; j < thrown.length; j++)
				if (thrown[j].isProperType(true) && ePrime[i].isCompatibleWith(thrown[j]))
					continue actual;
			for (int j = 0; j < n; j++)
				result.add(ConstraintTypeFormula.create(ePrime[i], e[j], SUBTYPE));
		}
		for (int j = 0; j < n; j++)
			inferenceContext.currentBounds.inThrows.add(e[j].prototype());
		return result.toArray(new ConstraintFormula[result.size()]);
	}

	@Override
	Collection<InferenceVariable> inputVariables(final InferenceContext18 context) {
		// from 18.5.2.
		if (this.left instanceof LambdaExpression) {
			if (this.right instanceof InferenceVariable) {
				return Collections.singletonList((InferenceVariable)this.right);
			}
			if (this.right.isFunctionalInterface(context.scope)) {
				LambdaExpression lambda = (LambdaExpression) this.left;
				MethodBinding sam = this.right.getSingleAbstractMethod(context.scope, true); // TODO derive with target type?
				final Set<InferenceVariable> variables = new LinkedHashSet<>();
				if (lambda.argumentsTypeElided()) {
					// i)
					int len = sam.parameters.length;
					for (int i = 0; i < len; i++) {
						sam.parameters[i].collectInferenceVariables(variables);
					}
				}
				if (sam.returnType != TypeBinding.VOID) {
					// ii)
					sam.returnType.collectInferenceVariables(variables);
				}
				return variables;
			}
		} else if (this.left instanceof ReferenceExpression) {
			if (this.right instanceof InferenceVariable) {
				return Collections.singletonList((InferenceVariable)this.right);
			}
			if (this.right.isFunctionalInterface(context.scope)) { // TODO: && this.left is inexact
				MethodBinding sam = this.right.getSingleAbstractMethod(context.scope, true); // TODO derive with target type?
				final Set<InferenceVariable> variables = new LinkedHashSet<>();
				int len = sam.parameters.length;
				for (int i = 0; i < len; i++) {
					sam.parameters[i].collectInferenceVariables(variables);
				}
				sam.returnType.collectInferenceVariables(variables);
				return variables;
			}
		}
		return EMPTY_VARIABLE_LIST;
	}

	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer().append(LEFT_ANGLE_BRACKET);
		this.left.printExpression(4, buf);
		buf.append(" \u2286throws "); //$NON-NLS-1$
		appendTypeName(buf, this.right);
		buf.append(RIGHT_ANGLE_BRACKET);
		return buf.toString();
	}
}
