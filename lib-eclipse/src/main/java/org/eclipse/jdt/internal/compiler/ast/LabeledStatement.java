/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
import org.eclipse.jdt.internal.compiler.codegen.*;
import org.eclipse.jdt.internal.compiler.flow.*;
import org.eclipse.jdt.internal.compiler.lookup.*;

public class LabeledStatement extends Statement {

	public Statement statement;
	public char[] label;
	public BranchLabel targetLabel;
	public int labelEnd;

	// for local variables table attributes
	int mergedInitStateIndex = -1;

	/**
	 * LabeledStatement constructor comment.
	 */
	public LabeledStatement(char[] label, Statement statement, long labelPosition, int sourceEnd) {

		this.statement = statement;
		// remember useful empty statement
		if (statement instanceof EmptyStatement) statement.bits |= IsUsefulEmptyStatement;
		this.label = label;
		this.sourceStart = (int)(labelPosition >>> 32);
		this.labelEnd = (int) labelPosition;
		this.sourceEnd = sourceEnd;
	}

	@Override
	public FlowInfo analyseCode(
		BlockScope currentScope,
		FlowContext flowContext,
		FlowInfo flowInfo) {

		// need to stack a context to store explicit label, answer inits in case of normal completion merged
		// with those relative to the exit path from break statement occurring inside the labeled statement.
		if (this.statement == null) {
			return flowInfo;
		} else {
			LabelFlowContext labelContext;
			FlowInfo statementInfo, mergedInfo;
			statementInfo = this.statement.analyseCode(
				currentScope,
				(labelContext =
					new LabelFlowContext(
						flowContext,
						this,
						this.label,
						(this.targetLabel = new BranchLabel()),
						currentScope)),
				flowInfo);
			boolean reinjectNullInfo = (statementInfo.tagBits & FlowInfo.UNREACHABLE) != 0 &&
				(labelContext.initsOnBreak.tagBits & FlowInfo.UNREACHABLE) == 0;
			mergedInfo = statementInfo.mergedWith(labelContext.initsOnBreak);
			if (reinjectNullInfo) {
				// an embedded loop has had no chance to reinject forgotten null info
				((UnconditionalFlowInfo)mergedInfo).addNullInfoFrom(flowInfo.unconditionalFieldLessCopy()).
					addNullInfoFrom(labelContext.initsOnBreak.unconditionalFieldLessCopy());
			}
			this.mergedInitStateIndex =
				currentScope.methodScope().recordInitializationStates(mergedInfo);
			if ((this.bits & ASTNode.LabelUsed) == 0) {
				currentScope.problemReporter().unusedLabel(this);
			}
			return mergedInfo;
		}
	}

	@Override
	public ASTNode concreteStatement() {

		// return statement.concreteStatement(); // for supporting nested labels:   a:b:c: someStatement (see 21912)
		return this.statement;
	}

	/**
	 * Code generation for labeled statement
	 *
	 * may not need actual source positions recording
	 *
	 * @param currentScope org.eclipse.jdt.internal.compiler.lookup.BlockScope
	 * @param codeStream org.eclipse.jdt.internal.compiler.codegen.CodeStream
	 */
	@Override
	public void generateCode(BlockScope currentScope, CodeStream codeStream) {

		if ((this.bits & IsReachable) == 0) {
			return;
		}
		int pc = codeStream.position;
		if (this.targetLabel != null) {
			this.targetLabel.initialize(codeStream);
			if (this.statement != null) {
				this.statement.generateCode(currentScope, codeStream);
			}
			this.targetLabel.place();
		}
		// May loose some local variable initializations : affecting the local variable attributes
		if (this.mergedInitStateIndex != -1) {
			codeStream.removeNotDefinitelyAssignedVariables(currentScope, this.mergedInitStateIndex);
			codeStream.addDefinitelyAssignedVariables(currentScope, this.mergedInitStateIndex);
		}
		codeStream.recordPositionsFrom(pc, this.sourceStart);
	}

	@Override
	public StringBuffer printStatement(int tab, StringBuffer output) {

		printIndent(tab, output).append(this.label).append(": "); //$NON-NLS-1$
		if (this.statement == null)
			output.append(';');
		else
			this.statement.printStatement(0, output);
		return output;
	}

	@Override
	public void resolve(BlockScope scope) {

		if (this.statement != null) {
			this.statement.resolve(scope);
		}
	}


	@Override
	public void traverse(
		ASTVisitor visitor,
		BlockScope blockScope) {

		if (visitor.visit(this, blockScope)) {
			if (this.statement != null) this.statement.traverse(visitor, blockScope);
		}
		visitor.endVisit(this, blockScope);
	}

	@Override
	public boolean doesNotCompleteNormally() {
		if (this.statement.breaksOut(this.label))
			return false;
		return this.statement.doesNotCompleteNormally();
	}

	@Override
	public boolean completesByContinue() {
		return this.statement instanceof ContinueStatement; // NOT this.statement.continuesAtOuterLabel
	}

	@Override
	public boolean canCompleteNormally() {
		if (this.statement.canCompleteNormally())
			return true;
		return this.statement.breaksOut(this.label);
	}

	@Override
	public boolean continueCompletes() {
		return this.statement instanceof ContinueStatement; // NOT this.statement.continuesAtOuterLabel
	}

}
