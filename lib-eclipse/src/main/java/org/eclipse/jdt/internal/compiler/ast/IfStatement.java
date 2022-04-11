/*******************************************************************************
 * Copyright (c) 2000, 2021 IBM Corporation and others.
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
 *     Stephan Herrmann - Contributions for
 *     							bug 319201 - [null] no warning when unboxing SingleNameReference causes NPE
 *     							bug 349326 - [1.7] new warning for missing try-with-resources
 *								bug 345305 - [compiler][null] Compiler misidentifies a case of "variable can only be null"
 *								bug 383368 - [compiler][null] syntactic null analysis for field references
 *								bug 403147 - [compiler][null] FUP of bug 400761: consolidate interaction between unboxing, NPE, and deferred checking
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.ast;

import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.impl.*;
import org.eclipse.jdt.internal.compiler.codegen.*;
import org.eclipse.jdt.internal.compiler.flow.*;
import org.eclipse.jdt.internal.compiler.lookup.*;

public class IfStatement extends Statement {

	//this class represents the case of only one statement in
	//either else and/or then branches.

	public Expression condition;
	public Statement thenStatement;
	public Statement elseStatement;

	// for local variables table attributes
	int thenInitStateIndex = -1;
	int elseInitStateIndex = -1;
	int mergedInitStateIndex = -1;

public IfStatement(Expression condition, Statement thenStatement, int sourceStart, int sourceEnd) {
	this.condition = condition;
	this.thenStatement = thenStatement;
	// remember useful empty statement
	if (thenStatement instanceof EmptyStatement) thenStatement.bits |= IsUsefulEmptyStatement;
	this.sourceStart = sourceStart;
	this.sourceEnd = sourceEnd;
}

public IfStatement(Expression condition, Statement thenStatement, Statement elseStatement, int sourceStart, int sourceEnd) {
	this.condition = condition;
	this.thenStatement = thenStatement;
	// remember useful empty statement
	if (thenStatement instanceof EmptyStatement) thenStatement.bits |= IsUsefulEmptyStatement;
	this.elseStatement = elseStatement;
	if (elseStatement instanceof IfStatement) elseStatement.bits |= IsElseIfStatement;
	if (elseStatement instanceof EmptyStatement) elseStatement.bits |= IsUsefulEmptyStatement;
	this.sourceStart = sourceStart;
	this.sourceEnd = sourceEnd;
}

@Override
public FlowInfo analyseCode(BlockScope currentScope, FlowContext flowContext, FlowInfo flowInfo) {
	// process the condition
	FlowInfo conditionFlowInfo = this.condition.analyseCode(currentScope, flowContext, flowInfo);
	int initialComplaintLevel = (flowInfo.reachMode() & FlowInfo.UNREACHABLE) != 0 ? Statement.COMPLAINED_FAKE_REACHABLE : Statement.NOT_COMPLAINED;

	Constant cst = this.condition.optimizedBooleanConstant();
	this.condition.checkNPEbyUnboxing(currentScope, flowContext, flowInfo);
	boolean isConditionOptimizedTrue = cst != Constant.NotAConstant && cst.booleanValue() == true;
	boolean isConditionOptimizedFalse = cst != Constant.NotAConstant && cst.booleanValue() == false;

	flowContext.conditionalLevel++;

	// process the THEN part
	FlowInfo thenFlowInfo = conditionFlowInfo.safeInitsWhenTrue();
	if (isConditionOptimizedFalse) {
		thenFlowInfo.setReachMode(FlowInfo.UNREACHABLE_OR_DEAD);
	}
	FlowInfo elseFlowInfo = conditionFlowInfo.initsWhenFalse().copy();
	if (isConditionOptimizedTrue) {
		elseFlowInfo.setReachMode(FlowInfo.UNREACHABLE_OR_DEAD);
	}
	if (((flowInfo.tagBits & FlowInfo.UNREACHABLE) == 0) &&
			((thenFlowInfo.tagBits & FlowInfo.UNREACHABLE) != 0)) {
		// Mark then block as unreachable
		// No need if the whole if-else construct itself lies in unreachable code
		this.bits |= ASTNode.IsThenStatementUnreachable;
	} else if (((flowInfo.tagBits & FlowInfo.UNREACHABLE) == 0) &&
			((elseFlowInfo.tagBits & FlowInfo.UNREACHABLE) != 0)) {
		// Mark else block as unreachable
		// No need if the whole if-else construct itself lies in unreachable code
		this.bits |= ASTNode.IsElseStatementUnreachable;
	}
	boolean reportDeadCodeForKnownPattern = !isKnowDeadCodePattern(this.condition) || currentScope.compilerOptions().reportDeadCodeInTrivialIfStatement;
	if (this.thenStatement != null) {
		// Save info for code gen
		this.thenInitStateIndex = currentScope.methodScope().recordInitializationStates(thenFlowInfo);
		if (isConditionOptimizedFalse || ((this.bits & ASTNode.IsThenStatementUnreachable) != 0)) {
			if (reportDeadCodeForKnownPattern) {
				this.thenStatement.complainIfUnreachable(thenFlowInfo, currentScope, initialComplaintLevel, false);
			} else {
				// its a known coding pattern which should be tolerated by dead code analysis
				// according to isKnowDeadCodePattern()
				this.bits &= ~ASTNode.IsThenStatementUnreachable;
			}
		}
		this.condition.updateFlowOnBooleanResult(thenFlowInfo, true);
		thenFlowInfo = this.thenStatement.analyseCode(currentScope, flowContext, thenFlowInfo);
		if (!(this.thenStatement instanceof Block))
			flowContext.expireNullCheckedFieldInfo();
	}
	// any null check from the condition is now expired
	flowContext.expireNullCheckedFieldInfo();
	// code gen: optimizing the jump around the ELSE part
	if ((thenFlowInfo.tagBits & FlowInfo.UNREACHABLE_OR_DEAD) != 0) {
		this.bits |= ASTNode.ThenExit;
	}

	// process the ELSE part
	if (this.elseStatement != null) {
		// signal else clause unnecessarily nested, tolerate else-if code pattern
		if (thenFlowInfo == FlowInfo.DEAD_END
				&& (this.bits & IsElseIfStatement) == 0 	// else of an else-if
				&& !(this.elseStatement instanceof IfStatement)) {
			currentScope.problemReporter().unnecessaryElse(this.elseStatement);
		}
		// Save info for code gen
		this.elseInitStateIndex = currentScope.methodScope().recordInitializationStates(elseFlowInfo);
		if (isConditionOptimizedTrue || ((this.bits & ASTNode.IsElseStatementUnreachable) != 0)) {
			if (reportDeadCodeForKnownPattern) {
				this.elseStatement.complainIfUnreachable(elseFlowInfo, currentScope, initialComplaintLevel, false);
			} else {
				// its a known coding pattern which should be tolerated by dead code analysis
				// according to isKnowDeadCodePattern()
				this.bits &= ~ASTNode.IsElseStatementUnreachable;
			}
		}
		this.condition.updateFlowOnBooleanResult(elseFlowInfo, false);
		elseFlowInfo = this.elseStatement.analyseCode(currentScope, flowContext, elseFlowInfo);
		if (!(this.elseStatement instanceof Block))
			flowContext.expireNullCheckedFieldInfo();
	}
	// process AutoCloseable resources closed in only one branch:
	currentScope.correlateTrackingVarsIfElse(thenFlowInfo, elseFlowInfo);
	// merge THEN & ELSE initializations
	FlowInfo mergedInfo = FlowInfo.mergedOptimizedBranchesIfElse(
		thenFlowInfo,
		isConditionOptimizedTrue,
		elseFlowInfo,
		isConditionOptimizedFalse,
		true /*if(true){ return; }  fake-reachable(); */,
		flowInfo,
		this,
		reportDeadCodeForKnownPattern);
	this.mergedInitStateIndex = currentScope.methodScope().recordInitializationStates(mergedInfo);
	flowContext.conditionalLevel--;
	return mergedInfo;
}
/**
 * If code generation
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
	BranchLabel endifLabel = new BranchLabel(codeStream);

	// optimizing the then/else part code gen
	Constant cst;
	boolean hasThenPart =
		!(((cst = this.condition.optimizedBooleanConstant()) != Constant.NotAConstant
				&& cst.booleanValue() == false)
			|| this.thenStatement == null
			|| this.thenStatement.isEmptyBlock());
	boolean hasElsePart =
		!((cst != Constant.NotAConstant && cst.booleanValue() == true)
			|| this.elseStatement == null
			|| this.elseStatement.isEmptyBlock());
	if (hasThenPart) {
		BranchLabel falseLabel = null;
		// generate boolean condition only if needed
		if (cst != Constant.NotAConstant && cst.booleanValue() == true) {
			this.condition.generateCode(currentScope, codeStream, false);
		} else {
			this.condition.generateOptimizedBoolean(
				currentScope,
				codeStream,
				null,
				hasElsePart ? (falseLabel = new BranchLabel(codeStream)) : endifLabel,
				true/*cst == Constant.NotAConstant*/);
		}
		// May loose some local variable initializations : affecting the local variable attributes
		if (this.thenInitStateIndex != -1) {
			codeStream.removeNotDefinitelyAssignedVariables(currentScope, this.thenInitStateIndex);
			codeStream.addDefinitelyAssignedVariables(currentScope, this.thenInitStateIndex);
		}
		// generate then statement
		this.thenStatement.generateCode(currentScope, codeStream);
		// jump around the else statement
		if (hasElsePart) {
			if ((this.bits & ASTNode.ThenExit) == 0) {
				this.thenStatement.branchChainTo(endifLabel);
				int position = codeStream.position;
				codeStream.goto_(endifLabel);
				//goto is pointing to the last line of the thenStatement
				codeStream.recordPositionsFrom(position, this.thenStatement.sourceEnd);
				// generate else statement
			}
			// May loose some local variable initializations : affecting the local variable attributes
			if (this.elseInitStateIndex != -1) {
				codeStream.removeNotDefinitelyAssignedVariables(
						currentScope,
					this.elseInitStateIndex);
				codeStream.addDefinitelyAssignedVariables(currentScope, this.elseInitStateIndex);
			}
			if (falseLabel != null) falseLabel.place();
			this.elseStatement.generateCode(currentScope, codeStream);
		}
	} else if (hasElsePart) {
		// generate boolean condition only if needed
		if (cst != Constant.NotAConstant && cst.booleanValue() == false) {
			this.condition.generateCode(currentScope, codeStream, false);
		} else {
			this.condition.generateOptimizedBoolean(
				currentScope,
				codeStream,
				endifLabel,
				null,
				true/*cst == Constant.NotAConstant*/);
		}
		// generate else statement
		// May loose some local variable initializations : affecting the local variable attributes
		if (this.elseInitStateIndex != -1) {
			codeStream.removeNotDefinitelyAssignedVariables(
				currentScope,
				this.elseInitStateIndex);
			codeStream.addDefinitelyAssignedVariables(currentScope, this.elseInitStateIndex);
		}
		this.elseStatement.generateCode(currentScope, codeStream);
	} else {
		// generate condition side-effects
		if (this.condition.containsPatternVariable()) {
			this.condition.generateOptimizedBoolean(
				currentScope,
				codeStream,
				endifLabel,
				null,
				cst == Constant.NotAConstant);
		} else {
			this.condition.generateCode(currentScope, codeStream, false);
		}
		codeStream.recordPositionsFrom(pc, this.sourceStart);
	}
	// May loose some local variable initializations : affecting the local variable attributes
	if (this.mergedInitStateIndex != -1) {
		codeStream.removeNotDefinitelyAssignedVariables(
			currentScope,
			this.mergedInitStateIndex);
		codeStream.addDefinitelyAssignedVariables(currentScope, this.mergedInitStateIndex);
	}
	endifLabel.place();
	codeStream.recordPositionsFrom(pc, this.sourceStart);
}



@Override
public StringBuffer printStatement(int indent, StringBuffer output) {
	printIndent(indent, output).append("if ("); //$NON-NLS-1$
	this.condition.printExpression(0, output).append(")\n");	//$NON-NLS-1$
	this.thenStatement.printStatement(indent + 2, output);
	if (this.elseStatement != null) {
		output.append('\n');
		printIndent(indent, output);
		output.append("else\n"); //$NON-NLS-1$
		this.elseStatement.printStatement(indent + 2, output);
	}
	return output;
}
private void resolveIfStatement(BlockScope scope) {
	TypeBinding type = this.condition.resolveTypeExpecting(scope, TypeBinding.BOOLEAN);
	this.condition.computeConversion(scope, type, type);
	if (this.thenStatement != null)
		this.thenStatement.resolve(scope);
	if (this.elseStatement != null)
		this.elseStatement.resolve(scope);
}
@Override
public void resolve(BlockScope scope) {
	if (containsPatternVariable()) {
		this.condition.collectPatternVariablesToScope(null, scope);
		LocalVariableBinding[] patternVariablesInTrueScope = this.condition.getPatternVariablesWhenTrue();
		LocalVariableBinding[] patternVariablesInFalseScope = this.condition.getPatternVariablesWhenFalse();
		TypeBinding type = this.condition.resolveTypeExpecting(scope, TypeBinding.BOOLEAN);
		this.condition.computeConversion(scope, type, type);

		if (this.thenStatement != null) {
			this.thenStatement.resolveWithPatternVariablesInScope(patternVariablesInTrueScope, scope);
		}
		if (this.elseStatement != null) {
			this.elseStatement.resolveWithPatternVariablesInScope(patternVariablesInFalseScope, scope);
		}
		if (this.thenStatement != null)
			this.thenStatement.promotePatternVariablesIfApplicable(patternVariablesInFalseScope,
				this.thenStatement::doesNotCompleteNormally);
		if (this.elseStatement != null)
			this.elseStatement.promotePatternVariablesIfApplicable(patternVariablesInTrueScope,
					this.elseStatement::doesNotCompleteNormally);
	} else {
		resolveIfStatement(scope);
	}
}

@Override
public boolean containsPatternVariable() {
	return this.condition.containsPatternVariable();
}

@Override
public void traverse(ASTVisitor visitor, BlockScope blockScope) {
	if (visitor.visit(this, blockScope)) {
		this.condition.traverse(visitor, blockScope);
		if (this.thenStatement != null)
			this.thenStatement.traverse(visitor, blockScope);
		if (this.elseStatement != null)
			this.elseStatement.traverse(visitor, blockScope);
	}
	visitor.endVisit(this, blockScope);
}

@Override
public boolean doesNotCompleteNormally() {
	return this.thenStatement != null && this.thenStatement.doesNotCompleteNormally() && this.elseStatement != null && this.elseStatement.doesNotCompleteNormally();
}
@Override
public boolean completesByContinue() {
	return this.thenStatement != null && this.thenStatement.completesByContinue() || this.elseStatement != null && this.elseStatement.completesByContinue();
}
@Override
public boolean canCompleteNormally() {
	return ((this.thenStatement == null || this.thenStatement.canCompleteNormally()) ||
		(this.elseStatement == null || this.elseStatement.canCompleteNormally()));
}
@Override
public boolean continueCompletes() {
	return this.thenStatement != null && this.thenStatement.continueCompletes() ||
			this.elseStatement != null && this.elseStatement.continueCompletes();
}
}
