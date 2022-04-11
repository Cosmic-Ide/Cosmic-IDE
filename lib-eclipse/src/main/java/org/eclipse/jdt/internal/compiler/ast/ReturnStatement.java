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
 *     							bug 360328 - [compiler][null] detect null problems in nested code (local class inside a loop)
 *								bug 186342 - [compiler][null] Using annotations for null checking
 *								bug 365835 - [compiler][null] inconsistent error reporting.
 *								bug 365519 - editorial cleanup after bug 186342 and bug 365387
 *								bug 358903 - Filter practically unimportant resource leak warnings
 *								bug 368546 - [compiler][resource] Avoid remaining false positives found when compiling the Eclipse SDK
 *								bug 370639 - [compiler][resource] restore the default for resource leak warnings
 *								bug 365859 - [compiler][null] distinguish warnings based on flow analysis vs. null annotations
 *								bug 345305 - [compiler][null] Compiler misidentifies a case of "variable can only be null"
 *								bug 388996 - [compiler][resource] Incorrect 'potential resource leak'
 *								bug 394768 - [compiler][resource] Incorrect resource leak warning when creating stream in conditional
 *								bug 383368 - [compiler][null] syntactic null analysis for field references
 *								bug 400761 - [compiler][null] null may be return as boolean without a diagnostic
 *								bug 401030 - [1.8][null] Null analysis support for lambda methods.
 *								Bug 392099 - [1.8][compiler][null] Apply null annotation on types for null analysis
 *								Bug 415043 - [1.8][null] Follow-up re null type annotations after bug 392099
 *								Bug 416307 - [1.8][compiler][null] subclass with type parameter substitution confuses null checking
 *								Bug 417758 - [1.8][null] Null safety compromise during array creation.
 *								Bug 427438 - [1.8][compiler] NPE at org.eclipse.jdt.internal.compiler.ast.ConditionalExpression.generateCode(ConditionalExpression.java:280)
 *								Bug 430150 - [1.8][null] stricter checking against type variables
 *								Bug 435805 - [1.8][compiler][null] Java 8 compiler does not recognize declaration style null annotations
 *								Bug 452788 - [1.8][compiler] Type not correctly inferred in lambda expression
 *								Bug 453483 - [compiler][null][loop] Improve null analysis for loops
 *								Bug 455723 - Nonnull argument not correctly inferred in loop
 *     Jesper S Moller - Contributions for
 *								bug 382701 - [1.8][compiler] Implement semantic analysis of Lambda expressions & Reference expression
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.ast;

import static org.eclipse.jdt.internal.compiler.ast.ExpressionContext.ASSIGNMENT_CONTEXT;

import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.codegen.*;
import org.eclipse.jdt.internal.compiler.flow.*;
import org.eclipse.jdt.internal.compiler.impl.Constant;
import org.eclipse.jdt.internal.compiler.lookup.*;

public class ReturnStatement extends Statement {

	public Expression expression;
	public SubRoutineStatement[] subroutines;
	public LocalVariableBinding saveValueVariable;
	public int initStateIndex = -1;
	private boolean implicitReturn;

public ReturnStatement(Expression expression, int sourceStart, int sourceEnd) {
	this(expression, sourceStart, sourceEnd, false);
}

public ReturnStatement(Expression expression, int sourceStart, int sourceEnd, boolean implicitReturn) {
	this.sourceStart = sourceStart;
	this.sourceEnd = sourceEnd;
	this.expression = expression;
	this.implicitReturn = implicitReturn;
}

@Override
public FlowInfo analyseCode(BlockScope currentScope, FlowContext flowContext, FlowInfo flowInfo) {	// here requires to generate a sequence of finally blocks invocations depending corresponding
	// to each of the traversed try statements, so that execution will terminate properly.

	// lookup the label, this should answer the returnContext

	if (this.expression instanceof FunctionalExpression) {
		if (this.expression.resolvedType == null || !this.expression.resolvedType.isValidBinding()) {
			/* Don't descend without proper target types. For lambda shape analysis, what is pertinent is value vs void return and the fact that
			   this constitutes an abrupt exit. The former is already gathered, the latter is handled here.
			*/
			flowContext.recordAbruptExit();
			return FlowInfo.DEAD_END;
		}
	}

	MethodScope methodScope = currentScope.methodScope();
	if (this.expression != null) {
		flowInfo = this.expression.analyseCode(currentScope, flowContext, flowInfo);
		this.expression.checkNPEbyUnboxing(currentScope, flowContext, flowInfo);
		if (flowInfo.reachMode() == FlowInfo.REACHABLE && currentScope.compilerOptions().isAnnotationBasedNullAnalysisEnabled)
			checkAgainstNullAnnotation(currentScope, flowContext, flowInfo, this.expression);
		if (currentScope.compilerOptions().analyseResourceLeaks) {
			FakedTrackingVariable trackingVariable = FakedTrackingVariable.getCloseTrackingVariable(this.expression, flowInfo, flowContext);
			if (trackingVariable != null) {
				if (methodScope != trackingVariable.methodScope)
					trackingVariable.markClosedInNestedMethod();
				// by returning the method passes the responsibility to the caller:
				flowInfo = FakedTrackingVariable.markPassedToOutside(currentScope, this.expression, flowInfo, flowContext, true);
			}
			// don't wait till after this statement, because then flowInfo would be DEAD_END & thus cannot serve nullStatus any more:
			FakedTrackingVariable.cleanUpUnassigned(currentScope, this.expression, flowInfo);
		}
	}
	this.initStateIndex =
		methodScope.recordInitializationStates(flowInfo);
	// compute the return sequence (running the finally blocks)
	FlowContext traversedContext = flowContext;
	int subCount = 0;
	boolean saveValueNeeded = false;
	boolean hasValueToSave = needValueStore();
	boolean noAutoCloseables = true;
	do {
		SubRoutineStatement sub;
		if ((sub = traversedContext.subroutine()) != null) {
			if (this.subroutines == null){
				this.subroutines = new SubRoutineStatement[5];
			}
			if (subCount == this.subroutines.length) {
				System.arraycopy(this.subroutines, 0, (this.subroutines = new SubRoutineStatement[subCount*2]), 0, subCount); // grow
			}
			this.subroutines[subCount++] = sub;
			if (sub.isSubRoutineEscaping()) {
				saveValueNeeded = false;
				this.bits |= ASTNode.IsAnySubRoutineEscaping;
				break;
			}
			if (sub instanceof TryStatement) {
				if (((TryStatement) sub).resources.length > 0) {
					noAutoCloseables = false;
				}
			}
		}
		traversedContext.recordReturnFrom(flowInfo.unconditionalInits());

		if (traversedContext instanceof InsideSubRoutineFlowContext) {
			ASTNode node = traversedContext.associatedNode;
			if (node instanceof SynchronizedStatement) {
				this.bits |= ASTNode.IsSynchronized;
			} else if (node instanceof TryStatement) {
				TryStatement tryStatement = (TryStatement) node;
				flowInfo.addInitializationsFrom(tryStatement.subRoutineInits); // collect inits
				if (hasValueToSave) {
					if (this.saveValueVariable == null){ // closest subroutine secret variable is used
						prepareSaveValueLocation(tryStatement);
					}
					saveValueNeeded = true;
					this.initStateIndex =
						methodScope.recordInitializationStates(flowInfo);
				}
			}
		} else if (traversedContext instanceof InitializationFlowContext) {
				currentScope.problemReporter().cannotReturnInInitializer(this);
				return FlowInfo.DEAD_END;
		}
	} while ((traversedContext = traversedContext.getLocalParent()) != null);

	// resize subroutines
	if ((this.subroutines != null) && (subCount != this.subroutines.length)) {
		System.arraycopy(this.subroutines, 0, (this.subroutines = new SubRoutineStatement[subCount]), 0, subCount);
	}

	// secret local variable for return value (note that this can only occur in a real method)
	if (saveValueNeeded) {
		if (this.saveValueVariable != null) {
			this.saveValueVariable.useFlag = LocalVariableBinding.USED;
		}
	} else {
		this.saveValueVariable = null;
		if (((this.bits & ASTNode.IsSynchronized) == 0) && this.expression != null && TypeBinding.equalsEquals(this.expression.resolvedType, TypeBinding.BOOLEAN)) {
			if (noAutoCloseables) { // can't abruptly return in the presence of autocloseables. See https://bugs.eclipse.org/bugs/show_bug.cgi?id=367566
				this.expression.bits |= ASTNode.IsReturnedValue;
			}
		}
	}
	currentScope.checkUnclosedCloseables(flowInfo, flowContext, this, currentScope);
	// inside conditional structure respect that a finally-block may conditionally be entered directly from here
	flowContext.recordAbruptExit();
	flowContext.expireNullCheckedFieldInfo();
	return FlowInfo.DEAD_END;
}
@Override
public boolean doesNotCompleteNormally() {
	return true;
}

@Override
public boolean canCompleteNormally() {
	return false;
}

/**
 * Retrun statement code generation
 *
 *   generate the finallyInvocationSequence.
 *
 * @param currentScope org.eclipse.jdt.internal.compiler.lookup.BlockScope
 * @param codeStream org.eclipse.jdt.internal.compiler.codegen.CodeStream
 */
@Override
public void generateCode(BlockScope currentScope, CodeStream codeStream) {
	if ((this.bits & ASTNode.IsReachable) == 0) {
		return;
	}
	int pc = codeStream.position;
	boolean alreadyGeneratedExpression = false;
	// generate the expression
	if (needValueStore()) {
		alreadyGeneratedExpression = true;
		this.expression.generateCode(currentScope, codeStream, needValue()); // no value needed if non-returning subroutine
		generateStoreSaveValueIfNecessary(currentScope, codeStream);
	}

	// generation of code responsible for invoking the finally blocks in sequence
	if (this.subroutines != null) {
		Object reusableJSRTarget = this.expression == null ? (Object)TypeBinding.VOID : this.expression.reusableJSRTarget();
		for (int i = 0, max = this.subroutines.length; i < max; i++) {
			SubRoutineStatement sub = this.subroutines[i];
			boolean didEscape = sub.generateSubRoutineInvocation(currentScope, codeStream, reusableJSRTarget, this.initStateIndex, this.saveValueVariable);
			if (didEscape) {
					codeStream.recordPositionsFrom(pc, this.sourceStart);
					SubRoutineStatement.reenterAllExceptionHandlers(this.subroutines, i, codeStream);
					return;
			}
		}
	}
	if (this.saveValueVariable != null) {
		codeStream.load(this.saveValueVariable);
	}
	if (this.expression != null && !alreadyGeneratedExpression) {
		this.expression.generateCode(currentScope, codeStream, true);
		// hook necessary for Code Snippet
		generateStoreSaveValueIfNecessary(currentScope, codeStream);
	}
	// output the suitable return bytecode or wrap the value inside a descriptor for doits
	generateReturnBytecode(codeStream);
	if (this.saveValueVariable != null) {
		codeStream.removeVariable(this.saveValueVariable);
	}
	if (this.initStateIndex != -1) {
		codeStream.removeNotDefinitelyAssignedVariables(currentScope, this.initStateIndex);
		codeStream.addDefinitelyAssignedVariables(currentScope, this.initStateIndex);
	}
	codeStream.recordPositionsFrom(pc, this.sourceStart);
	SubRoutineStatement.reenterAllExceptionHandlers(this.subroutines, -1, codeStream);
}

/**
 * Dump the suitable return bytecode for a return statement
 *
 */
public void generateReturnBytecode(CodeStream codeStream) {
	codeStream.generateReturnBytecode(this.expression);
}

public void generateStoreSaveValueIfNecessary(Scope scope, CodeStream codeStream){
	if (this.saveValueVariable != null) {
		codeStream.store(this.saveValueVariable, false);
		// the variable is visible as soon as the local is stored
		codeStream.addVariable(this.saveValueVariable);
	}
}

private boolean needValueStore() {
	return this.expression != null
					&& (this.expression.constant == Constant.NotAConstant || (this.expression.implicitConversion & TypeIds.BOXING)!= 0)
					&& !(this.expression instanceof NullLiteral);
}

public boolean needValue() {
	return this.saveValueVariable != null
					|| (this.bits & ASTNode.IsSynchronized) != 0
					|| ((this.bits & ASTNode.IsAnySubRoutineEscaping) == 0);
}

public void prepareSaveValueLocation(TryStatement targetTryStatement){
	this.saveValueVariable = targetTryStatement.secretReturnValue;
}

@Override
public StringBuffer printStatement(int tab, StringBuffer output){
	printIndent(tab, output).append("return "); //$NON-NLS-1$
	if (this.expression != null )
		this.expression.printExpression(0, output) ;
	return output.append(';');
}

@Override
public void resolve(BlockScope scope) {
	MethodScope methodScope = scope.methodScope();
	MethodBinding methodBinding = null;
	LambdaExpression lambda = methodScope.referenceContext instanceof LambdaExpression ? (LambdaExpression) methodScope.referenceContext : null;
	TypeBinding methodType =
		lambda != null ? lambda.expectedResultType() :
		(methodScope.referenceContext instanceof AbstractMethodDeclaration)
			? ((methodBinding = ((AbstractMethodDeclaration) methodScope.referenceContext).binding) == null
				? null
				: methodBinding.returnType)
			: TypeBinding.VOID;
	TypeBinding expressionType;

	if (methodBinding != null && methodBinding.isCompactConstructor())
		scope.problemReporter().recordCompactConstructorHasReturnStatement(this);

	if (this.expression != null) {
		this.expression.setExpressionContext(ASSIGNMENT_CONTEXT);
		this.expression.setExpectedType(methodType);
		if (lambda != null && lambda.argumentsTypeElided() && this.expression instanceof CastExpression) {
			this.expression.bits |= ASTNode.DisableUnnecessaryCastCheck;
		}
	}

	if (methodType == TypeBinding.VOID) {
		// the expression should be null, exceptions exist for lambda expressions.
		if (this.expression == null) {
			if (lambda != null)
				lambda.returnsExpression(null, TypeBinding.VOID);
			return;
		}
		expressionType = this.expression.resolveType(scope);
		if (lambda != null)
			lambda.returnsExpression(this.expression, expressionType);
		if (this.implicitReturn && (expressionType == TypeBinding.VOID || this.expression.statementExpression()))
			return;
		if (expressionType != null)
			scope.problemReporter().attemptToReturnNonVoidExpression(this, expressionType);
		return;
	}
	if (this.expression == null) {
		if (lambda != null)
			lambda.returnsExpression(null,  methodType);
		if (methodType != null) scope.problemReporter().shouldReturn(methodType, this);
		return;
	}

	expressionType = this.expression.resolveType(scope);
	if (lambda != null)
		lambda.returnsExpression(this.expression, expressionType);

	if (expressionType == null) return;
	if (expressionType == TypeBinding.VOID) {
		scope.problemReporter().attemptToReturnVoidValue(this);
		return;
	}
	if (methodType == null)
		return;

	if (methodType.isProperType(true) && lambda != null) {
		// ensure that type conversions don't leak a preliminary local type:
		if (lambda.updateLocalTypes())
			methodType = lambda.expectedResultType();
	}
	if (TypeBinding.notEquals(methodType, expressionType)) // must call before computeConversion() and typeMismatchError()
		scope.compilationUnitScope().recordTypeConversion(methodType, expressionType);
	if (this.expression.isConstantValueOfTypeAssignableToType(expressionType, methodType)
			|| expressionType.isCompatibleWith(methodType, scope)) {

		this.expression.computeConversion(scope, methodType, expressionType);
		if (expressionType.needsUncheckedConversion(methodType)) {
		    scope.problemReporter().unsafeTypeConversion(this.expression, expressionType, methodType);
		}
		if (this.expression instanceof CastExpression) {
			if ((this.expression.bits & (ASTNode.UnnecessaryCast|ASTNode.DisableUnnecessaryCastCheck)) == 0) {
				CastExpression.checkNeedForAssignedCast(scope, methodType, (CastExpression) this.expression);
			} else if (lambda != null && lambda.argumentsTypeElided() && (this.expression.bits & ASTNode.UnnecessaryCast) != 0) {
				if (TypeBinding.equalsEquals(((CastExpression)this.expression).expression.resolvedType, methodType)) {
					scope.problemReporter().unnecessaryCast((CastExpression)this.expression);
				}
			}
		}
		return;
	} else if (isBoxingCompatible(expressionType, methodType, this.expression, scope)) {
		this.expression.computeConversion(scope, methodType, expressionType);
		if (this.expression instanceof CastExpression
				&& (this.expression.bits & (ASTNode.UnnecessaryCast|ASTNode.DisableUnnecessaryCastCheck)) == 0) {
			CastExpression.checkNeedForAssignedCast(scope, methodType, (CastExpression) this.expression);
		}			return;
	}
	if ((methodType.tagBits & TagBits.HasMissingType) == 0) {
		// no need to complain if return type was missing (avoid secondary error : 220967)
		scope.problemReporter().typeMismatchError(expressionType, methodType, this.expression, this);
	}
}

@Override
public void traverse(ASTVisitor visitor, BlockScope scope) {
	if (visitor.visit(this, scope)) {
		if (this.expression != null)
			this.expression.traverse(visitor, scope);
	}
	visitor.endVisit(this, scope);
}
}
