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
 *								bug 319201 - [null] no warning when unboxing SingleNameReference causes NPE
 *								bug 345305 - [compiler][null] Compiler misidentifies a case of "variable can only be null"
 *								bug 403147 - [compiler][null] FUP of bug 400761: consolidate interaction between unboxing, NPE, and deferred checking
 *								Bug 415790 - [compiler][resource]Incorrect potential resource leak warning in for loop with close in try/catch
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.ast;

import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.impl.*;
import org.eclipse.jdt.internal.compiler.codegen.*;
import org.eclipse.jdt.internal.compiler.flow.*;
import org.eclipse.jdt.internal.compiler.lookup.*;

public class DoStatement extends Statement {

	public Expression condition;
	public Statement action;

	private BranchLabel breakLabel, continueLabel;

	// for local variables table attributes
	int mergedInitStateIndex = -1;
	int preConditionInitStateIndex = -1;

public DoStatement(Expression condition, Statement action, int sourceStart, int sourceEnd) {

	this.sourceStart = sourceStart;
	this.sourceEnd = sourceEnd;
	this.condition = condition;
	this.action = action;
	// remember useful empty statement
	if (action instanceof EmptyStatement) action.bits |= ASTNode.IsUsefulEmptyStatement;
}

@Override
public FlowInfo analyseCode(BlockScope currentScope, FlowContext flowContext, FlowInfo flowInfo) {
	this.breakLabel = new BranchLabel();
	this.continueLabel = new BranchLabel();
	LoopingFlowContext loopingContext =
		new LoopingFlowContext(
			flowContext,
			flowInfo,
			this,
			this.breakLabel,
			this.continueLabel,
			currentScope,
			false);

	Constant cst = this.condition.constant;
	boolean isConditionTrue = cst != Constant.NotAConstant && cst.booleanValue() == true;
	cst = this.condition.optimizedBooleanConstant();
	boolean isConditionOptimizedTrue = cst != Constant.NotAConstant && cst.booleanValue() == true;
	boolean isConditionOptimizedFalse = cst != Constant.NotAConstant && cst.booleanValue() == false;

	int previousMode = flowInfo.reachMode();

	FlowInfo initsOnCondition = flowInfo;
	UnconditionalFlowInfo actionInfo = flowInfo.nullInfoLessUnconditionalCopy();
	// we need to collect the contribution to nulls of the coming paths through the
	// loop, be they falling through normally or branched to break, continue labels
	// or catch blocks
	if ((this.action != null) && !this.action.isEmptyBlock()) {
		actionInfo = this.action.
			analyseCode(currentScope, loopingContext, actionInfo).
			unconditionalInits();

		// code generation can be optimized when no need to continue in the loop
		if ((actionInfo.tagBits &
				loopingContext.initsOnContinue.tagBits &
				FlowInfo.UNREACHABLE_OR_DEAD) != 0) {
			this.continueLabel = null;
		}
		if ((this.condition.implicitConversion & TypeIds.UNBOXING) != 0) {
			initsOnCondition = flowInfo.unconditionalInits().
									addInitializationsFrom(
										actionInfo.mergedWith(loopingContext.initsOnContinue));
		}
	}
	this.condition.checkNPEbyUnboxing(currentScope, flowContext, initsOnCondition);
	/* Reset reach mode, to address following scenario.
	 *   final blank;
	 *   do { if (true) break; else blank = 0; } while(false);
	 *   blank = 1; // may be initialized already
	 */
	actionInfo.setReachMode(previousMode);

	LoopingFlowContext condLoopContext;
	FlowInfo condInfo =
		this.condition.analyseCode(
			currentScope,
			(condLoopContext =
				new LoopingFlowContext(flowContext,	flowInfo, this, null,
					null, currentScope, true)),
			(this.action == null
				? actionInfo
				: (actionInfo.mergedWith(loopingContext.initsOnContinue))).copy());
	/* https://bugs.eclipse.org/bugs/show_bug.cgi?id=367023, we reach the condition at the bottom via two arcs,
	   one by free fall and another by continuing... Merge initializations propagated through the two pathways,
	   cf, while and for loops.
	*/
	this.preConditionInitStateIndex = currentScope.methodScope().recordInitializationStates(actionInfo.mergedWith(loopingContext.initsOnContinue));
	if (!isConditionOptimizedFalse && this.continueLabel != null) {
		loopingContext.complainOnDeferredFinalChecks(currentScope, condInfo);
		condLoopContext.complainOnDeferredFinalChecks(currentScope, condInfo);
		loopingContext.complainOnDeferredNullChecks(currentScope,
				flowInfo.unconditionalCopy().addPotentialNullInfoFrom(
					  condInfo.initsWhenTrue().unconditionalInits()));
		condLoopContext.complainOnDeferredNullChecks(currentScope,
				actionInfo.addPotentialNullInfoFrom(
				  condInfo.initsWhenTrue().unconditionalInits()));
	} else {
		loopingContext.complainOnDeferredNullChecks(currentScope,
				flowInfo.unconditionalCopy().addPotentialNullInfoFrom(
					  condInfo.initsWhenTrue().unconditionalInits()), false);
		condLoopContext.complainOnDeferredNullChecks(currentScope,
				actionInfo.addPotentialNullInfoFrom(
				  condInfo.initsWhenTrue().unconditionalInits()), false);
	}
	if (loopingContext.hasEscapingExceptions()) { // https://bugs.eclipse.org/bugs/show_bug.cgi?id=321926
		FlowInfo loopbackFlowInfo = flowInfo.copy();
		// loopback | (loopback + action + condition):
		loopbackFlowInfo = loopbackFlowInfo.mergedWith(loopbackFlowInfo.unconditionalCopy().addNullInfoFrom(condInfo.initsWhenTrue()).unconditionalInits());
		loopingContext.simulateThrowAfterLoopBack(loopbackFlowInfo);
	}
	// end of loop
	FlowInfo mergedInfo =
		FlowInfo.mergedOptimizedBranches(
						(loopingContext.initsOnBreak.tagBits & FlowInfo.UNREACHABLE) != 0
								? loopingContext.initsOnBreak
								: flowInfo.unconditionalCopy().addInitializationsFrom(loopingContext.initsOnBreak),
								// recover upstream null info
						isConditionOptimizedTrue,
						(condInfo.tagBits & FlowInfo.UNREACHABLE) == 0
								? flowInfo.copy().addInitializationsFrom(condInfo.initsWhenFalse()) // https://bugs.eclipse.org/bugs/show_bug.cgi?id=380927
								: condInfo,
							// recover null inits from before condition analysis
						false, // never consider opt false case for DO loop, since break can always occur (47776)
						!isConditionTrue /*do{}while(true); unreachable(); */);
	this.mergedInitStateIndex = currentScope.methodScope().recordInitializationStates(mergedInfo);
	return mergedInfo;
}

/**
 * Do statement code generation
 *
 */
@Override
public void generateCode(BlockScope currentScope, CodeStream codeStream) {
	if ((this.bits & ASTNode.IsReachable) == 0) {
		return;
	}
	int pc = codeStream.position;

	// labels management
	BranchLabel actionLabel = new BranchLabel(codeStream);
	if (this.action != null) actionLabel.tagBits |= BranchLabel.USED;
	actionLabel.place();
	this.breakLabel.initialize(codeStream);
	boolean hasContinueLabel = this.continueLabel != null;
	if (hasContinueLabel) {
		this.continueLabel.initialize(codeStream);
	}

	// generate action
	if (this.action != null) {
		this.action.generateCode(currentScope, codeStream);
	}
	// continue label (135602)
	if (hasContinueLabel) {
		this.continueLabel.place();
		// May loose some local variable initializations : affecting the local variable attributes
		if (this.preConditionInitStateIndex != -1) {
			codeStream.removeNotDefinitelyAssignedVariables(currentScope, this.preConditionInitStateIndex);
			codeStream.addDefinitelyAssignedVariables(currentScope, this.preConditionInitStateIndex);
		}
		// generate condition
		Constant cst = this.condition.optimizedBooleanConstant();
		boolean isConditionOptimizedFalse = cst != Constant.NotAConstant && cst.booleanValue() == false;
		if (isConditionOptimizedFalse){
			this.condition.generateCode(currentScope, codeStream, false);
		} else {
			this.condition.generateOptimizedBoolean(
				currentScope,
				codeStream,
				actionLabel,
				null,
				true);
		}
	}
	// May loose some local variable initializations : affecting the local variable attributes
	if (this.mergedInitStateIndex != -1) {
		codeStream.removeNotDefinitelyAssignedVariables(currentScope, this.mergedInitStateIndex);
		codeStream.addDefinitelyAssignedVariables(currentScope, this.mergedInitStateIndex);
	}
	if (this.breakLabel.forwardReferenceCount() > 0) {
		this.breakLabel.place();
	}

	codeStream.recordPositionsFrom(pc, this.sourceStart);
}

@Override
public StringBuffer printStatement(int indent, StringBuffer output) {
	printIndent(indent, output).append("do"); //$NON-NLS-1$
	if (this.action == null)
		output.append(" ;\n"); //$NON-NLS-1$
	else {
		output.append('\n');
		this.action.printStatement(indent + 1, output).append('\n');
	}
	output.append("while ("); //$NON-NLS-1$
	return this.condition.printExpression(0, output).append(");"); //$NON-NLS-1$
}

@Override
public void resolve(BlockScope scope) {
	if (containsPatternVariable()) {
		this.condition.collectPatternVariablesToScope(null, scope);
		LocalVariableBinding[] patternVariablesInFalseScope = this.condition.getPatternVariablesWhenFalse();
		TypeBinding type = this.condition.resolveTypeExpecting(scope, TypeBinding.BOOLEAN);
		this.condition.computeConversion(scope, type, type);
		if (this.action != null) {
			this.action.resolve(scope);
			this.action.promotePatternVariablesIfApplicable(patternVariablesInFalseScope,
					() -> !this.action.breaksOut(null));
		}
	} else {
		TypeBinding type = this.condition.resolveTypeExpecting(scope, TypeBinding.BOOLEAN);
		this.condition.computeConversion(scope, type, type);
		if (this.action != null)
			this.action.resolve(scope);
	}
}

@Override
public boolean containsPatternVariable() {
	return this.condition.containsPatternVariable();
}
@Override
public void traverse(ASTVisitor visitor, BlockScope scope) {
	if (visitor.visit(this, scope)) {
		if (this.action != null) {
			this.action.traverse(visitor, scope);
		}
		this.condition.traverse(visitor, scope);
	}
	visitor.endVisit(this, scope);
}

@Override
public boolean doesNotCompleteNormally() {
	Constant cst = this.condition.constant;
	boolean isConditionTrue = cst == null || cst != Constant.NotAConstant && cst.booleanValue() == true;
	cst = this.condition.optimizedBooleanConstant();
	boolean isConditionOptimizedTrue = cst == null ? true : cst != Constant.NotAConstant && cst.booleanValue() == true;

	if (isConditionTrue || isConditionOptimizedTrue)
		return this.action == null || !this.action.breaksOut(null);
	if (this.action == null || this.action.breaksOut(null))
		return false;
	return this.action.doesNotCompleteNormally() && !this.action.completesByContinue();
}

@Override
public boolean completesByContinue() {
	return this.action.continuesAtOuterLabel();
}

@Override
public boolean canCompleteNormally() {
	Constant cst = this.condition.constant;
	boolean isConditionTrue = cst == null || cst != Constant.NotAConstant && cst.booleanValue() == true;
	cst = this.condition.optimizedBooleanConstant();
	boolean isConditionOptimizedTrue = cst == null ? true : cst != Constant.NotAConstant && cst.booleanValue() == true;

	if (!(isConditionTrue || isConditionOptimizedTrue)) {
		if (this.action == null || this.action.canCompleteNormally())
			return true;
		if (this.action != null && this.action.continueCompletes())
			return true;
	}
	if (this.action != null && this.action.breaksOut(null))
		return true;

	return false;
}
@Override
public boolean continueCompletes() {
	return this.action.continuesAtOuterLabel();
}

}
