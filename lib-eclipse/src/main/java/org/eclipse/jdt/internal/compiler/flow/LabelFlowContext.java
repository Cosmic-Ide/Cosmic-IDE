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
 *     Stephan Herrmann - Contribution for
 *								bug 345305 - [compiler][null] Compiler misidentifies a case of "variable can only be null"
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.flow;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.codegen.BranchLabel;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;

/**
 * Reflects the context of code analysis, keeping track of enclosing
 *	try statements, exception handlers, etc...
 */
public class LabelFlowContext extends SwitchFlowContext {

	public char[] labelName;

public LabelFlowContext(FlowContext parent, ASTNode associatedNode, char[] labelName, BranchLabel breakLabel, BlockScope scope) {
	super(parent, associatedNode, breakLabel, false, true);
	this.labelName = labelName;
	checkLabelValidity(scope);
}

void checkLabelValidity(BlockScope scope) {
	// check if label was already defined above
	FlowContext current = this.getLocalParent();
	while (current != null) {
		char[] currentLabelName;
		if (((currentLabelName = current.labelName()) != null)
			&& CharOperation.equals(currentLabelName, this.labelName)) {
			scope.problemReporter().alreadyDefinedLabel(this.labelName, this.associatedNode);
		}
		current = current.getLocalParent();
	}
}

@Override
public String individualToString() {
	return "Label flow context [label:" + String.valueOf(this.labelName) + "]"; //$NON-NLS-2$ //$NON-NLS-1$
}

@Override
public char[] labelName() {
	return this.labelName;
}
}
