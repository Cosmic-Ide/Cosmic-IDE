/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
 *								bug 403086 - [compiler][null] include the effect of 'assert' in syntactic null analysis for fields
 *								bug 403147 - [compiler][null] FUP of bug 400761: consolidate interaction between unboxing, NPE, and deferred checking
 *								Bug 422796 - [compiler][null] boxed boolean reported as potentially null after null test in lazy disjunction
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.ast;

import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.impl.*;
import org.eclipse.jdt.internal.compiler.codegen.*;
import org.eclipse.jdt.internal.compiler.flow.*;
import org.eclipse.jdt.internal.compiler.lookup.*;

//dedicated treatment for the &&
public class AND_AND_Expression extends BinaryExpression {

	int rightInitStateIndex = -1;
	int mergedInitStateIndex = -1;

	public AND_AND_Expression(Expression left, Expression right, int operator) {
		super(left, right, operator);
	}

	@Override
	public FlowInfo analyseCode(BlockScope currentScope, FlowContext flowContext, FlowInfo flowInfo) {

		Constant cst = this.left.optimizedBooleanConstant();
		boolean isLeftOptimizedTrue = cst != Constant.NotAConstant && cst.booleanValue() == true;
		boolean isLeftOptimizedFalse = cst != Constant.NotAConstant && cst.booleanValue() == false;

		if (isLeftOptimizedTrue) {
			// TRUE && anything
			// need to be careful of scenario:
			//  (x && y) && !z, if passing the left info to the right, it would
			// be swapped by the !
			FlowInfo mergedInfo = this.left.analyseCode(currentScope, flowContext, flowInfo)
					.unconditionalInits();
			mergedInfo = this.right.analyseCode(currentScope, flowContext, mergedInfo);
			this.mergedInitStateIndex = currentScope.methodScope()
					.recordInitializationStates(mergedInfo);
			return mergedInfo;
		}

		FlowInfo leftInfo = this.left.analyseCode(currentScope, flowContext, flowInfo);
		if ((flowContext.tagBits & FlowContext.INSIDE_NEGATION) != 0)
			flowContext.expireNullCheckedFieldInfo();
		// need to be careful of scenario:
		//  (x && y) && !z, if passing the left info to the right, it would be
		// swapped by the !
		FlowInfo rightInfo = leftInfo.initsWhenTrue().unconditionalCopy();
		this.rightInitStateIndex = currentScope.methodScope().recordInitializationStates(rightInfo);

		int previousMode = rightInfo.reachMode();
		if (isLeftOptimizedFalse) {
			if ((rightInfo.reachMode() & FlowInfo.UNREACHABLE) == 0) {
				currentScope.problemReporter().fakeReachable(this.right);
				rightInfo.setReachMode(FlowInfo.UNREACHABLE_OR_DEAD);
			}
		}
		this.left.updateFlowOnBooleanResult(rightInfo, true);
		rightInfo = this.right.analyseCode(currentScope, flowContext, rightInfo);
		if ((flowContext.tagBits & FlowContext.INSIDE_NEGATION) != 0)
			flowContext.expireNullCheckedFieldInfo();
		this.left.checkNPEbyUnboxing(currentScope, flowContext, flowInfo);
		this.right.checkNPEbyUnboxing(currentScope, flowContext, leftInfo.initsWhenTrue());
		FlowInfo mergedInfo = FlowInfo.conditional(
				rightInfo.safeInitsWhenTrue(),
				leftInfo.initsWhenFalse().unconditionalInits().mergedWith(
						rightInfo.initsWhenFalse().setReachMode(previousMode).unconditionalInits()));
		// reset after trueMergedInfo got extracted
		this.mergedInitStateIndex = currentScope.methodScope().recordInitializationStates(mergedInfo);
		return mergedInfo;
	}

	/**
	 * Code generation for a binary operation
	 */
	@Override
	public void generateCode(BlockScope currentScope, CodeStream codeStream, boolean valueRequired) {

		int pc = codeStream.position;
		if (this.constant != Constant.NotAConstant) {
			// inlined value
			if (valueRequired)
				codeStream.generateConstant(this.constant, this.implicitConversion);
			codeStream.recordPositionsFrom(pc, this.sourceStart);
			return;
		}
		Constant cst = this.right.constant;
		if (cst != Constant.NotAConstant) {
			// <expr> && true --> <expr>
			if (cst.booleanValue() == true) {
				this.left.generateCode(currentScope, codeStream, valueRequired);
			} else {
				// <expr> && false --> false
				this.left.generateCode(currentScope, codeStream, false);
				if (valueRequired) codeStream.iconst_0();
			}
			if (this.mergedInitStateIndex != -1) {
				codeStream.removeNotDefinitelyAssignedVariables(currentScope, this.mergedInitStateIndex);
			}
			codeStream.generateImplicitConversion(this.implicitConversion);
			codeStream.recordPositionsFrom(pc, this.sourceStart);
			return;
		}

		BranchLabel falseLabel = new BranchLabel(codeStream), endLabel;
		cst = this.left.optimizedBooleanConstant();
		boolean leftIsConst = cst != Constant.NotAConstant;
		boolean leftIsTrue = leftIsConst && cst.booleanValue() == true;

		cst = this.right.optimizedBooleanConstant();
		boolean rightIsConst = cst != Constant.NotAConstant;
		boolean rightIsTrue = rightIsConst && cst.booleanValue() == true;

		generateOperands : {
			if (leftIsConst) {
				this.left.generateCode(currentScope, codeStream, false);
				if (!leftIsTrue) {
					break generateOperands; // no need to generate right operand
				}
			} else {
				this.left.generateOptimizedBoolean(currentScope, codeStream, null, falseLabel, true);
				// need value, e.g. if (a == 1 && ((b = 2) > 0)) {} -> shouldn't initialize 'b' if a!=1
			}
			if (this.rightInitStateIndex != -1) {
				codeStream.addDefinitelyAssignedVariables(currentScope, this.rightInitStateIndex);
			}
			if (rightIsConst) {
				this.right.generateCode(currentScope, codeStream, false);
			} else {
				this.right.generateOptimizedBoolean(currentScope, codeStream, null, falseLabel, valueRequired);
			}
		}
		if (this.mergedInitStateIndex != -1) {
			codeStream.removeNotDefinitelyAssignedVariables(currentScope, this.mergedInitStateIndex);
		}
		/*
		 * improving code gen for such a case: boolean b = i < 0 && false since
		 * the label has never been used, we have the inlined value on the
		 * stack.
		 */
		if (valueRequired) {
			if (leftIsConst && !leftIsTrue) {
				codeStream.iconst_0();
			} else {
				if (rightIsConst && !rightIsTrue) {
					codeStream.iconst_0();
				} else {
					codeStream.iconst_1();
				}
				if (falseLabel.forwardReferenceCount() > 0) {
					if ((this.bits & IsReturnedValue) != 0) {
						codeStream.generateImplicitConversion(this.implicitConversion);
						codeStream.generateReturnBytecode(this);
						falseLabel.place();
						codeStream.iconst_0();
					} else {
						codeStream.goto_(endLabel = new BranchLabel(codeStream));
						codeStream.decrStackSize(1);
						falseLabel.place();
						codeStream.iconst_0();
						endLabel.place();
					}
				} else {
					falseLabel.place();
				}
			}
			codeStream.generateImplicitConversion(this.implicitConversion);
			codeStream.recordPositionsFrom(codeStream.position, this.sourceEnd);
		} else {
			falseLabel.place();
		}
	}

	/**
	 * Boolean operator code generation Optimized operations are: &&
	 */
	@Override
	public void generateOptimizedBoolean(BlockScope currentScope, CodeStream codeStream, BranchLabel trueLabel, BranchLabel falseLabel, boolean valueRequired) {

		if (this.constant != Constant.NotAConstant) {
			super.generateOptimizedBoolean(currentScope, codeStream, trueLabel, falseLabel,
					valueRequired);
			return;
		}

		// <expr> && true --> <expr>
		Constant cst = this.right.constant;
		if (cst != Constant.NotAConstant && cst.booleanValue() == true) {
			int pc = codeStream.position;
			this.left.generateOptimizedBoolean(currentScope, codeStream, trueLabel, falseLabel, valueRequired);
			if (this.mergedInitStateIndex != -1) {
				codeStream.removeNotDefinitelyAssignedVariables(currentScope, this.mergedInitStateIndex);
			}
			codeStream.recordPositionsFrom(pc, this.sourceStart);
			return;
		}
		cst = this.left.optimizedBooleanConstant();
		boolean leftIsConst = cst != Constant.NotAConstant;
		boolean leftIsTrue = leftIsConst && cst.booleanValue() == true;

		cst = this.right.optimizedBooleanConstant();
		boolean rightIsConst = cst != Constant.NotAConstant;
		boolean rightIsTrue = rightIsConst && cst.booleanValue() == true;

		// default case
		generateOperands : {
			if (falseLabel == null) {
				if (trueLabel != null) {
					// implicit falling through the FALSE case
					BranchLabel internalFalseLabel = new BranchLabel(codeStream);
					this.left.generateOptimizedBoolean(currentScope, codeStream, null, internalFalseLabel, !leftIsConst);
					// need value, e.g. if (a == 1 && ((b = 2) > 0)) {} -> shouldn't initialize 'b' if a!=1
					if (leftIsConst && !leftIsTrue) {
						internalFalseLabel.place();
						break generateOperands; // no need to generate right operand
					}
					if (this.rightInitStateIndex != -1) {
						codeStream
								.addDefinitelyAssignedVariables(currentScope, this.rightInitStateIndex);
					}
					this.right.generateOptimizedBoolean(currentScope, codeStream, trueLabel, null,
							valueRequired && !rightIsConst);
					if (valueRequired && rightIsConst && rightIsTrue) {
						codeStream.goto_(trueLabel);
						codeStream.recordPositionsFrom(codeStream.position, this.sourceEnd);
					}
					internalFalseLabel.place();
				}
			} else {
				// implicit falling through the TRUE case
				if (trueLabel == null) {
					this.left.generateOptimizedBoolean(currentScope, codeStream, null, falseLabel, !leftIsConst);
					// need value, e.g. if (a == 1 && ((b = 2) > 0)) {} -> shouldn't initialize 'b' if a!=1
					int pc = codeStream.position;
					if (leftIsConst && !leftIsTrue) {
						if (valueRequired) {
							codeStream.goto_(falseLabel);
						}
						codeStream.recordPositionsFrom(pc, this.sourceEnd);
						break generateOperands; // no need to generate right operand
					}
					if (this.rightInitStateIndex != -1) {
						codeStream
								.addDefinitelyAssignedVariables(currentScope, this.rightInitStateIndex);
					}
					this.right.generateOptimizedBoolean(currentScope, codeStream, null, falseLabel, valueRequired && !rightIsConst);
					if (valueRequired && rightIsConst && !rightIsTrue) {
						codeStream.goto_(falseLabel);
						codeStream.recordPositionsFrom(pc, this.sourceEnd);
					}
				} else {
					// no implicit fall through TRUE/FALSE --> should never occur
				}
			}
		}
		if (this.mergedInitStateIndex != -1) {
			codeStream.removeNotDefinitelyAssignedVariables(currentScope, this.mergedInitStateIndex);
		}
	}
	@Override
	public void collectPatternVariablesToScope(LocalVariableBinding[] variables, BlockScope scope) {
		this.addPatternVariablesWhenTrue(variables);
		// If upper level already supplied positive new vars for us, also make those available to the left expr
		this.left.addPatternVariablesWhenTrue(this.patternVarsWhenTrue);

		this.left.collectPatternVariablesToScope(this.patternVarsWhenTrue, scope);

		variables = this.left.getPatternVariablesWhenTrue();
		this.addPatternVariablesWhenTrue(variables);
		this.right.addPatternVariablesWhenTrue(variables);

		variables = this.left.getPatternVariablesWhenFalse();
		this.right.addPatternVariablesWhenFalse(variables);

		this.right.collectPatternVariablesToScope(this.patternVarsWhenTrue, scope);
		variables = this.right.getPatternVariablesWhenTrue();
		this.addPatternVariablesWhenTrue(variables);
	}
	@Override
	public boolean isCompactableOperation() {
		return false;
	}

	/**
	 * @see org.eclipse.jdt.internal.compiler.ast.BinaryExpression#resolveType(org.eclipse.jdt.internal.compiler.lookup.BlockScope)
	 */
	@Override
	public TypeBinding resolveType(BlockScope scope) {
		TypeBinding result = super.resolveType(scope);
		// check whether comparing identical expressions
		Binding leftDirect = Expression.getDirectBinding(this.left);
		if (leftDirect != null && leftDirect == Expression.getDirectBinding(this.right)) {
			if (!(this.right instanceof Assignment))
				scope.problemReporter().comparingIdenticalExpressions(this);
		}
		return result;
	}

	@Override
	public void traverse(ASTVisitor visitor, BlockScope scope) {
		if (visitor.visit(this, scope)) {
			this.left.traverse(visitor, scope);
			this.right.traverse(visitor, scope);
		}
		visitor.endVisit(this, scope);
	}
}
