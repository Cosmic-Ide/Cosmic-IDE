/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.codegen.BranchLabel;

/**
 * Reflects the context of code analysis, keeping track of enclosing
 *	try statements, exception handlers, etc...
 */
public class SwitchFlowContext extends FlowContext {

	public BranchLabel breakLabel;
	public UnconditionalFlowInfo initsOnBreak = FlowInfo.DEAD_END;
	public boolean isExpression = false;

public SwitchFlowContext(FlowContext parent, ASTNode associatedNode, BranchLabel breakLabel, boolean isPreTest, boolean inheritNullFieldChecks) {
	super(parent, associatedNode, inheritNullFieldChecks);
	this.breakLabel = breakLabel;
	if (isPreTest && parent.conditionalLevel > -1) {
		this.conditionalLevel++;
	}
}

@Override
public BranchLabel breakLabel() {
	return this.breakLabel;
}

@Override
public String individualToString() {
	StringBuilder buffer = new StringBuilder("Switch flow context"); //$NON-NLS-1$
	buffer.append("[initsOnBreak -").append(this.initsOnBreak.toString()).append(']'); //$NON-NLS-1$
	return buffer.toString();
}

@Override
public boolean isBreakable() {
	return true;
}

@Override
public void recordBreakFrom(FlowInfo flowInfo) {
	if ((this.initsOnBreak.tagBits & FlowInfo.UNREACHABLE_OR_DEAD) == 0) {
		this.initsOnBreak = this.initsOnBreak.mergedWith(flowInfo.unconditionalInits());
	}
	else {
		this.initsOnBreak = flowInfo.unconditionalCopy();
	}
}
}
