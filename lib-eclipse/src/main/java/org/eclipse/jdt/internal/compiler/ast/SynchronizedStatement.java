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
 *     Carmi Grushko - Bug 465048 - Binding is null for class literals in synchronized blocks
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.ast;

import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.codegen.*;
import org.eclipse.jdt.internal.compiler.flow.*;
import org.eclipse.jdt.internal.compiler.impl.Constant;
import org.eclipse.jdt.internal.compiler.lookup.*;

public class SynchronizedStatement extends SubRoutineStatement {

	public Expression expression;
	public Block block;
	public BlockScope scope;
	public LocalVariableBinding synchroVariable;
	static final char[] SecretLocalDeclarationName = " syncValue".toCharArray(); //$NON-NLS-1$

	// for local variables table attributes
	int preSynchronizedInitStateIndex = -1;
	int mergedSynchronizedInitStateIndex = -1;

public SynchronizedStatement(
	Expression expression,
	Block statement,
	int s,
	int e) {

	this.expression = expression;
	this.block = statement;
	this.sourceEnd = e;
	this.sourceStart = s;
}

@Override
public FlowInfo analyseCode(
	BlockScope currentScope,
	FlowContext flowContext,
	FlowInfo flowInfo) {

	this.preSynchronizedInitStateIndex =
		currentScope.methodScope().recordInitializationStates(flowInfo);
    // TODO (philippe) shouldn't it be protected by a check whether reachable statement ?

	// mark the synthetic variable as being used
	this.synchroVariable.useFlag = LocalVariableBinding.USED;

	// simple propagation to subnodes
	FlowInfo expressionFlowInfo = this.expression.analyseCode(this.scope, flowContext, flowInfo);

	this.expression.checkNPE(currentScope, flowContext, expressionFlowInfo, 1);

	flowInfo =
		this.block.analyseCode(
			this.scope,
			new InsideSubRoutineFlowContext(flowContext, this),
			expressionFlowInfo);

	this.mergedSynchronizedInitStateIndex =
		currentScope.methodScope().recordInitializationStates(flowInfo);

	// optimizing code gen
	if ((flowInfo.tagBits & FlowInfo.UNREACHABLE_OR_DEAD) != 0) {
		this.bits |= ASTNode.BlockExit;
	}

	return flowInfo;
}

@Override
public boolean isSubRoutineEscaping() {
	return false;
}

/**
 * Synchronized statement code generation
 *
 * @param currentScope org.eclipse.jdt.internal.compiler.lookup.BlockScope
 * @param codeStream org.eclipse.jdt.internal.compiler.codegen.CodeStream
 */
@Override
public void generateCode(BlockScope currentScope, CodeStream codeStream) {
	if ((this.bits & IsReachable) == 0) {
		return;
	}
	// in case the labels needs to be reinitialized
	// when the code generation is restarted in wide mode
	this.anyExceptionLabel = null;

	int pc = codeStream.position;

	// generate the synchronization expression
	this.expression.generateCode(this.scope, codeStream, true);
	if (this.block.isEmptyBlock()) {
		switch(this.synchroVariable.type.id) {
			case TypeIds.T_long :
			case TypeIds.T_double :
				codeStream.dup2();
				break;
			default :
				codeStream.dup();
				break;
		}
		// only take the lock
		codeStream.monitorenter();
		codeStream.monitorexit();
		if (this.scope != currentScope) {
			codeStream.exitUserScope(this.scope);
		}
	} else {
		// enter the monitor
		codeStream.store(this.synchroVariable, true);
		codeStream.addVariable(this.synchroVariable);
		codeStream.monitorenter();

		// generate  the body of the synchronized block
		enterAnyExceptionHandler(codeStream);
		this.block.generateCode(this.scope, codeStream);
		if (this.scope != currentScope) {
			// close all locals defined in the synchronized block except the secret local
			codeStream.exitUserScope(this.scope, this.synchroVariable);
		}

		BranchLabel endLabel = new BranchLabel(codeStream);
		if ((this.bits & ASTNode.BlockExit) == 0) {
			codeStream.load(this.synchroVariable);
			codeStream.monitorexit();
			exitAnyExceptionHandler();
			codeStream.goto_(endLabel);
			enterAnyExceptionHandler(codeStream);
		}
		// generate the body of the exception handler
		codeStream.pushExceptionOnStack(this.scope.getJavaLangThrowable());
		if (this.preSynchronizedInitStateIndex != -1) {
			codeStream.removeNotDefinitelyAssignedVariables(currentScope, this.preSynchronizedInitStateIndex);
		}
		placeAllAnyExceptionHandler();
		codeStream.load(this.synchroVariable);
		codeStream.monitorexit();
		exitAnyExceptionHandler();
		codeStream.athrow();
		// May loose some local variable initializations : affecting the local variable attributes
		if (this.mergedSynchronizedInitStateIndex != -1) {
			codeStream.removeNotDefinitelyAssignedVariables(currentScope, this.mergedSynchronizedInitStateIndex);
			codeStream.addDefinitelyAssignedVariables(currentScope, this.mergedSynchronizedInitStateIndex);
		}
		if (this.scope != currentScope) {
			codeStream.removeVariable(this.synchroVariable);
		}
		if ((this.bits & ASTNode.BlockExit) == 0) {
			endLabel.place();
		}
	}
	codeStream.recordPositionsFrom(pc, this.sourceStart);
}

/**
 * @see SubRoutineStatement#generateSubRoutineInvocation(BlockScope, CodeStream, Object, int, LocalVariableBinding)
 */
@Override
public boolean generateSubRoutineInvocation(BlockScope currentScope, CodeStream codeStream, Object targetLocation, int stateIndex, LocalVariableBinding secretLocal) {
	codeStream.load(this.synchroVariable);
	codeStream.monitorexit();
	exitAnyExceptionHandler();
	return false;
}

@Override
public void resolve(BlockScope upperScope) {
	// special scope for secret locals optimization.
	this.scope = new BlockScope(upperScope);
	TypeBinding type = this.expression.resolveType(this.scope);
	if (type != null) {
		switch (type.id) {
			case T_boolean :
			case T_char :
			case T_float :
			case T_double :
			case T_byte :
			case T_short :
			case T_int :
			case T_long :
				this.scope.problemReporter().invalidTypeToSynchronize(this.expression, type);
				break;
			case T_void :
				this.scope.problemReporter().illegalVoidExpression(this.expression);
				break;
			case T_null :
				this.scope.problemReporter().invalidNullToSynchronize(this.expression);
				break;
			default :
				if (type.hasValueBasedTypeAnnotation()) {
					this.scope.problemReporter().discouragedValueBasedTypeToSynchronize(this.expression, type);
				}
			}
			//continue even on errors in order to have the TC done into the statements
			this.synchroVariable = new LocalVariableBinding(SecretLocalDeclarationName, type, ClassFileConstants.AccDefault, false);
			this.scope.addLocalVariable(this.synchroVariable);
			this.synchroVariable.setConstant(Constant.NotAConstant); // not inlinable
			this.expression.computeConversion(this.scope, type, type);
	}
	this.block.resolveUsing(this.scope);
}

@Override
public StringBuffer printStatement(int indent, StringBuffer output) {
	printIndent(indent, output);
	output.append("synchronized ("); //$NON-NLS-1$
	this.expression.printExpression(0, output).append(')');
	output.append('\n');
	return this.block.printStatement(indent + 1, output);
}

@Override
public void traverse(ASTVisitor visitor, BlockScope blockScope) {
	if (visitor.visit(this, blockScope)) {
		this.expression.traverse(visitor, this.scope);
		this.block.traverse(visitor, this.scope);
	}
	visitor.endVisit(this, blockScope);
}

@Override
public boolean doesNotCompleteNormally() {
	return this.block.doesNotCompleteNormally();
}
@Override

public boolean completesByContinue() {
	return this.block.completesByContinue();
}

@Override
public boolean canCompleteNormally() {
	return this.block.canCompleteNormally();
}

@Override
public boolean continueCompletes() {
	return this.block.continueCompletes();
}
}
