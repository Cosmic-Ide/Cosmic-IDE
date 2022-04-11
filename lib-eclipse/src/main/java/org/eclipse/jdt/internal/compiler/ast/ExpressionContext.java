/*******************************************************************************
 * Copyright (c) 2013, 2014 IBM Corporation and others.
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
 *								Bug 427438 - [1.8][compiler] NPE at org.eclipse.jdt.internal.compiler.ast.ConditionalExpression.generateCode(ConditionalExpression.java:280)
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.ast;

public enum ExpressionContext {

	/** Assignment context: potential poly-expressions are: method invocations, lambdas, reference expressions,
	   conditional expressions and allocation expressions. This is the only Java 7 context where target type
	   influenced evaluation of some expression.

	   Context induced by: ReturnStatement, ArrayInitializer, Assignment, FieldDeclaration and LocalDeclaration.
	*/
	ASSIGNMENT_CONTEXT {
		@Override
		public String toString() {
			return "assignment context"; //$NON-NLS-1$
		}
		@Override
		public boolean definesTargetType() {
			return true;
		}
	},

	/** Invocation context: potential poly-expressions are: method invocations, lambdas, reference expressions,
	   conditional expressions and allocation expressions. At this point, we don't distinguish between strict
	   and loose invocation contexts - we may have to cross the bridge some day.

	   Context induced by: AllocationExpression, QualifiedAllocationExpression, ExplicitConstructorCall, MessageSend
	   CodeSnippetAllocationExpression and CodeSnippetMessageSend.
	*/
	INVOCATION_CONTEXT {
		@Override
		public String toString() {
			return "invocation context"; //$NON-NLS-1$
		}
		@Override
		public boolean definesTargetType() {
			return true;
		}
	},

	/** Casting context: potential poly-expressions are: lambdas and reference expressions
	   Context induced by: CastExpression.
	*/
	CASTING_CONTEXT {
		@Override
		public String toString() {
			return "casting context"; //$NON-NLS-1$
		}
		@Override
		public boolean definesTargetType() {
			return false;
		}
	},

	/** Vanilla context (string, numeric): potential poly-expressions are: NONE. This is the nonpoly context in which
	   expressions get evaluated, unless they feature in one of the above contexts.
	*/
	VANILLA_CONTEXT {
		@Override
		public String toString() {
			return "vanilla context"; //$NON-NLS-1$
		}
		@Override
		public boolean definesTargetType() {
			return false;
		}
	};

	public abstract boolean definesTargetType();
}
