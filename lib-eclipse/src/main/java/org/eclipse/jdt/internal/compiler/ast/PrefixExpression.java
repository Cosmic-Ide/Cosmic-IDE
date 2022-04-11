/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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
package org.eclipse.jdt.internal.compiler.ast;

import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.lookup.*;

public class PrefixExpression extends CompoundAssignment {

/**
 * PrefixExpression constructor comment.
 * @param lhs org.eclipse.jdt.internal.compiler.ast.Expression
 * @param expression org.eclipse.jdt.internal.compiler.ast.Expression
 * @param operator int
 */
public PrefixExpression(Expression lhs, Expression expression, int operator, int pos) {
	super(lhs, expression, operator, lhs.sourceEnd);
	this.sourceStart = pos;
	this.sourceEnd = lhs.sourceEnd;
}
@Override
public boolean checkCastCompatibility() {
	return false;
}
@Override
public String operatorToString() {
	switch (this.operator) {
		case PLUS :
			return "++"; //$NON-NLS-1$
		case MINUS :
			return "--"; //$NON-NLS-1$
	}
	return "unknown operator"; //$NON-NLS-1$
}

@Override
public StringBuffer printExpressionNoParenthesis(int indent, StringBuffer output) {

	output.append(operatorToString()).append(' ');
	return this.lhs.printExpression(0, output);
}

@Override
public boolean restrainUsageToNumericTypes() {
	return true;
}

@Override
public void traverse(ASTVisitor visitor, BlockScope scope) {
	if (visitor.visit(this, scope)) {
		this.lhs.traverse(visitor, scope);
	}
	visitor.endVisit(this, scope);
}
}
