/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
package org.eclipse.jdt.internal.compiler.flow;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.SubRoutineStatement;

/**
 * Reflects the context of code analysis, keeping track of enclosing
 *	try statements, exception handlers, etc...
 */
public class InsideSubRoutineFlowContext extends TryFlowContext {

	public UnconditionalFlowInfo initsOnReturn;

public InsideSubRoutineFlowContext(
	FlowContext parent,
	ASTNode associatedNode) {
	super(parent, associatedNode);
	this.initsOnReturn = FlowInfo.DEAD_END;
}

@Override
public String individualToString() {
	StringBuilder buffer = new StringBuilder("Inside SubRoutine flow context"); //$NON-NLS-1$
	buffer.append("[initsOnReturn -").append(this.initsOnReturn.toString()).append(']'); //$NON-NLS-1$
	return buffer.toString();
}

@Override
public UnconditionalFlowInfo initsOnReturn(){
	return this.initsOnReturn;
}

@Override
public boolean isNonReturningContext() {
	return ((SubRoutineStatement) this.associatedNode).isSubRoutineEscaping();
}

@Override
public void recordReturnFrom(UnconditionalFlowInfo flowInfo) {
	if ((flowInfo.tagBits & FlowInfo.UNREACHABLE_OR_DEAD) == 0)	{
	if (this.initsOnReturn == FlowInfo.DEAD_END) {
		this.initsOnReturn = (UnconditionalFlowInfo) flowInfo.copy();
	} else {
		this.initsOnReturn = this.initsOnReturn.mergedWith(flowInfo);
	}
	}
}

@Override
public SubRoutineStatement subroutine() {
	return (SubRoutineStatement) this.associatedNode;
}
}
