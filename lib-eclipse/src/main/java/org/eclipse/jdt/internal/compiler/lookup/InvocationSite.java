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
 *								bug 384380 - False positive on a "Potential null pointer access" after a continue
 *								Bug 400874 - [1.8][compiler] Inference infrastructure should evolve to meet JLS8 18.x (Part G of JSR335 spec)
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.lookup;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.ExpressionContext;

public interface InvocationSite {

	TypeBinding[] genericTypeArguments();
	boolean isSuperAccess();
	boolean isQualifiedSuper();
	boolean isTypeAccess();
	// in case the receiver type does not match the actual receiver type
	// e.g. pkg.Type.C (receiver type of C is type of source context,
	//		but actual receiver type is pkg.Type)
	// e.g2. in presence of implicit access to enclosing type
	void setActualReceiverType(ReferenceBinding receiverType);
	void setDepth(int depth);
	void setFieldIndex(int depth);
	int sourceEnd();
	int sourceStart();
	default int nameSourceStart() { return sourceStart(); }
	default int nameSourceEnd() { return sourceEnd(); }
	TypeBinding invocationTargetType();
	boolean receiverIsImplicitThis();
	boolean checkingPotentialCompatibility();
	void acceptPotentiallyCompatibleMethods(MethodBinding [] methods);
	public default LocalVariableBinding[] getPatternVariablesWhenTrue() {
		return null;
	}

	/** When inference for this invocationSite starts, get a fresh inference context, initialized from this site. */
	InferenceContext18 freshInferenceContext(Scope scope);
	ExpressionContext getExpressionContext();

	static class EmptyWithAstNode implements InvocationSite {
		ASTNode node;
		public EmptyWithAstNode(ASTNode node) {
			this.node = node;
		}
		@Override
		public TypeBinding[] genericTypeArguments() { return null;}
		@Override
		public boolean isSuperAccess() {return false;}
		@Override
		public boolean isTypeAccess() {return false;}
		@Override
		public void setActualReceiverType(ReferenceBinding receiverType) {/* empty */}
		@Override
		public void setDepth(int depth) {/* empty */ }
		@Override
		public void setFieldIndex(int depth) {/* empty */ }
		@Override
		public int sourceEnd() {return this.node.sourceEnd; }
		@Override
		public int sourceStart() {return this.node.sourceStart; }
		@Override
		public TypeBinding invocationTargetType() { return null; }
		@Override
		public boolean receiverIsImplicitThis() { return false; }
		@Override
		public InferenceContext18 freshInferenceContext(Scope scope) { return null; }
		@Override
		public ExpressionContext getExpressionContext() { return ExpressionContext.VANILLA_CONTEXT; }
		@Override
		public boolean isQualifiedSuper() { return false; }
		@Override
		public boolean checkingPotentialCompatibility() { return false; }
		@Override
		public void acceptPotentiallyCompatibleMethods(MethodBinding[] methods) { /* ignore */ }
	}
}
