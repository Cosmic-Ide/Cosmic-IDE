/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
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
package org.eclipse.jdt.internal.compiler.ast;

import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.flow.*;
import org.eclipse.jdt.internal.compiler.lookup.*;

public class ContinueStatement extends BranchStatement {

public ContinueStatement(char[] label, int sourceStart, int sourceEnd) {
	super(label, sourceStart, sourceEnd);
}

@Override
public FlowInfo analyseCode(BlockScope currentScope, FlowContext flowContext, FlowInfo flowInfo) {

	// here requires to generate a sequence of finally blocks invocations depending corresponding
	// to each of the traversed try statements, so that execution will terminate properly.

	// lookup the label, this should answer the returnContext
	FlowContext targetContext = (this.label == null)
			? flowContext.getTargetContextForDefaultContinue()
			: flowContext.getTargetContextForContinueLabel(this.label);

	if (targetContext == null) {
		if (this.label == null) {
			currentScope.problemReporter().invalidContinue(this);
		} else {
			currentScope.problemReporter().undefinedLabel(this);
		}
		return flowInfo; // pretend it did not continue since no actual target
	}

	targetContext.recordAbruptExit();
	targetContext.expireNullCheckedFieldInfo();

	if (targetContext == FlowContext.NotContinuableContext) {
		currentScope.problemReporter().invalidContinue(this);
		return flowInfo; // pretend it did not continue since no actual target
	}
	this.initStateIndex =
		currentScope.methodScope().recordInitializationStates(flowInfo);

	this.targetLabel = targetContext.continueLabel();
	FlowContext traversedContext = flowContext;
	int subCount = 0;
	this.subroutines = new SubRoutineStatement[5];

	do {
		SubRoutineStatement sub;
		if ((sub = traversedContext.subroutine()) != null) {
			if (subCount == this.subroutines.length) {
				System.arraycopy(this.subroutines, 0, this.subroutines = new SubRoutineStatement[subCount*2], 0, subCount); // grow
			}
			this.subroutines[subCount++] = sub;
			if (sub.isSubRoutineEscaping()) {
				break;
			}
		}
		traversedContext.recordReturnFrom(flowInfo.unconditionalInits());

		if (traversedContext instanceof InsideSubRoutineFlowContext) {
			ASTNode node = traversedContext.associatedNode;
			if (node instanceof TryStatement) {
				TryStatement tryStatement = (TryStatement) node;
				flowInfo.addInitializationsFrom(tryStatement.subRoutineInits); // collect inits
			}
		} else if (traversedContext == targetContext) {
			// only record continue info once accumulated through subroutines, and only against target context
			targetContext.recordContinueFrom(flowContext, flowInfo);
			break;
		}
	} while ((traversedContext = traversedContext.getLocalParent()) != null);

	// resize subroutines
	if (subCount != this.subroutines.length) {
		System.arraycopy(this.subroutines, 0, this.subroutines = new SubRoutineStatement[subCount], 0, subCount);
	}
	return FlowInfo.DEAD_END;
}

@Override
public StringBuffer printStatement(int tab, StringBuffer output) {
	printIndent(tab, output).append("continue "); //$NON-NLS-1$
	if (this.label != null) output.append(this.label);
	return output.append(';');
}

@Override
public void traverse(ASTVisitor visitor, 	BlockScope blockScope) {
	visitor.visit(this, blockScope);
	visitor.endVisit(this, blockScope);
}
@Override
public boolean doesNotCompleteNormally() {
	return true;
}

@Override
public boolean completesByContinue() {
	return true;
}

@Override
public boolean canCompleteNormally() {
	return false;
}

@Override
public boolean continueCompletes() {
	return true;
}
}
