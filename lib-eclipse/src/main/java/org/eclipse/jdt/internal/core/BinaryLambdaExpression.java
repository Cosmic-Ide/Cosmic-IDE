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
 *******************************************************************************/
package org.eclipse.jdt.internal.core;


public class BinaryLambdaExpression extends LambdaExpression {

	BinaryLambdaExpression(JavaElement parent, org.eclipse.jdt.internal.compiler.ast.LambdaExpression lambdaExpression) {
		super(parent, lambdaExpression);
	}

	BinaryLambdaExpression(JavaElement parent, String interphase, int sourceStart, int sourceEnd, int arrowPosition) {
		super(parent, interphase, sourceStart, sourceEnd, arrowPosition);
	}

	BinaryLambdaExpression(JavaElement parent, String interphase, int sourceStart, int sourceEnd, int arrowPosition, LambdaMethod lambdaMethod) {
		super(parent, interphase, sourceStart, sourceEnd, arrowPosition, lambdaMethod);
	}

	@Override
	public JavaElement getPrimaryElement(boolean checkOwner) {
		return this;
	}

	@Override
	public boolean isBinary() {
		return true;
	}
}
