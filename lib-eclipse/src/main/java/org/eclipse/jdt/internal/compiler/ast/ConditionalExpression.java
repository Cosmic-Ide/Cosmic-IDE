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
 *     Stephen Herrmann <stephan@cs.tu-berlin.de> -  Contributions for
 *     						bug 133125 - [compiler][null] need to report the null status of expressions and analyze them simultaneously
 *     						bug 292478 - Report potentially null across variable assignment
 * 							bug 324178 - [null] ConditionalExpression.nullStatus(..) doesn't take into account the analysis of condition itself
 * 							bug 354554 - [null] conditional with redundant condition yields weak error message
 *     						bug 349326 - [1.7] new warning for missing try-with-resources
 *							bug 345305 - [compiler][null] Compiler misidentifies a case of "variable can only be null"
 *							bug 383368 - [compiler][null] syntactic null analysis for field references
 *							bug 400761 - [compiler][null] null may be return as boolean without a diagnostic
 *							Bug 392099 - [1.8][compiler][null] Apply null annotation on types for null analysis
 *							Bug 415043 - [1.8][null] Follow-up re null type annotations after bug 392099
 *							Bug 417295 - [1.8[[null] Massage type annotated null analysis to gel well with deep encoded type bindings.
 *							Bug 400874 - [1.8][compiler] Inference infrastructure should evolve to meet JLS8 18.x (Part G of JSR335 spec)
 *							Bug 426078 - [1.8] VerifyError when conditional expression passed as an argument
 *							Bug 427438 - [1.8][compiler] NPE at org.eclipse.jdt.internal.compiler.ast.ConditionalExpression.generateCode(ConditionalExpression.java:280)
 *							Bug 418537 - [1.8][null] Fix null type annotation analysis for poly conditional expressions
 *							Bug 428352 - [1.8][compiler] Resolution errors don't always surface
 *							Bug 407414 - [compiler][null] Incorrect warning on a primitive type being null
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.ast;

import static org.eclipse.jdt.internal.compiler.ast.ExpressionContext.*;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.impl.*;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.codegen.*;
import org.eclipse.jdt.internal.compiler.flow.*;
import org.eclipse.jdt.internal.compiler.lookup.*;

public class ConditionalExpression extends OperatorExpression implements IPolyExpression {

	public Expression condition, valueIfTrue, valueIfFalse;
	public Constant optimizedBooleanConstant;
	public Constant optimizedIfTrueConstant;
	public Constant optimizedIfFalseConstant;

	// for local variables table attributes
	int trueInitStateIndex = -1;
	int falseInitStateIndex = -1;
	int mergedInitStateIndex = -1;

	// we compute and store the null status during analyseCode (https://bugs.eclipse.org/324178):
	private int nullStatus = FlowInfo.UNKNOWN;
	int ifFalseNullStatus;
	int ifTrueNullStatus;
	private TypeBinding expectedType;
	private ExpressionContext expressionContext = VANILLA_CONTEXT;
	private boolean isPolyExpression = false;
	private TypeBinding originalValueIfTrueType;
	private TypeBinding originalValueIfFalseType;
	private boolean use18specifics;

	public ConditionalExpression(Expression condition, Expression valueIfTrue, Expression valueIfFalse) {
		this.condition = condition;
		this.valueIfTrue = valueIfTrue;
		this.valueIfFalse = valueIfFalse;
		this.sourceStart = condition.sourceStart;
		this.sourceEnd = valueIfFalse.sourceEnd;
	}

@Override
public FlowInfo analyseCode(BlockScope currentScope, FlowContext flowContext,
			FlowInfo flowInfo) {
		int initialComplaintLevel = (flowInfo.reachMode() & FlowInfo.UNREACHABLE) != 0 ? Statement.COMPLAINED_FAKE_REACHABLE : Statement.NOT_COMPLAINED;
		Constant cst = this.condition.optimizedBooleanConstant();
		boolean isConditionOptimizedTrue = cst != Constant.NotAConstant && cst.booleanValue() == true;
		boolean isConditionOptimizedFalse = cst != Constant.NotAConstant && cst.booleanValue() == false;

		int mode = flowInfo.reachMode();
		flowInfo = this.condition.analyseCode(currentScope, flowContext, flowInfo, cst == Constant.NotAConstant);

		flowContext.conditionalLevel++;

		// process the if-true part
		FlowInfo trueFlowInfo = flowInfo.initsWhenTrue().copy();
		final CompilerOptions compilerOptions = currentScope.compilerOptions();
		if (isConditionOptimizedFalse) {
			if ((mode & FlowInfo.UNREACHABLE) == 0) {
				trueFlowInfo.setReachMode(FlowInfo.UNREACHABLE_OR_DEAD);
			}
			if (!isKnowDeadCodePattern(this.condition) || compilerOptions.reportDeadCodeInTrivialIfStatement) {
				this.valueIfTrue.complainIfUnreachable(trueFlowInfo, currentScope, initialComplaintLevel, false);
			}
		}
		this.trueInitStateIndex = currentScope.methodScope().recordInitializationStates(trueFlowInfo);
		this.condition.updateFlowOnBooleanResult(trueFlowInfo, true);
		trueFlowInfo = this.valueIfTrue.analyseCode(currentScope, flowContext, trueFlowInfo);
		this.valueIfTrue.checkNPEbyUnboxing(currentScope, flowContext, trueFlowInfo);

		// may need to fetch this null status before expireNullCheckedFieldInfo():
		this.ifTrueNullStatus = -1;
		if (compilerOptions.enableSyntacticNullAnalysisForFields) {
			this.ifTrueNullStatus = this.valueIfTrue.nullStatus(trueFlowInfo, flowContext);
			// wipe information that was meant only for valueIfTrue:
			flowContext.expireNullCheckedFieldInfo();
		}

		// process the if-false part
		FlowInfo falseFlowInfo = flowInfo.initsWhenFalse().copy();
		if (isConditionOptimizedTrue) {
			if ((mode & FlowInfo.UNREACHABLE) == 0) {
				falseFlowInfo.setReachMode(FlowInfo.UNREACHABLE_OR_DEAD);
			}
			if (!isKnowDeadCodePattern(this.condition) || compilerOptions.reportDeadCodeInTrivialIfStatement) {
				this.valueIfFalse.complainIfUnreachable(falseFlowInfo, currentScope, initialComplaintLevel, true);
			}
		}
		this.falseInitStateIndex = currentScope.methodScope().recordInitializationStates(falseFlowInfo);
		this.condition.updateFlowOnBooleanResult(falseFlowInfo, false);
		falseFlowInfo = this.valueIfFalse.analyseCode(currentScope, flowContext, falseFlowInfo);
		this.valueIfFalse.checkNPEbyUnboxing(currentScope, flowContext, falseFlowInfo);

		flowContext.conditionalLevel--;

		// merge if-true & if-false initializations
		FlowInfo mergedInfo;
		if (isConditionOptimizedTrue){
			mergedInfo = trueFlowInfo.addPotentialInitializationsFrom(falseFlowInfo);
			if (this.ifTrueNullStatus != -1) {
				this.nullStatus = this.ifTrueNullStatus;
			} else {
				this.nullStatus = this.valueIfTrue.nullStatus(trueFlowInfo, flowContext);
			}
		} else if (isConditionOptimizedFalse) {
			mergedInfo = falseFlowInfo.addPotentialInitializationsFrom(trueFlowInfo);
			this.nullStatus = this.valueIfFalse.nullStatus(falseFlowInfo, flowContext);
		} else {
			// this block must meet two conflicting requirements (see https://bugs.eclipse.org/324178):
			// (1) For null analysis of "Object o2 = (o1 != null) ? o1 : new Object();" we need to distinguish
			//     the paths *originating* from the evaluation of the condition to true/false respectively.
			//     This is used to determine the possible null status of the entire conditional expression.
			// (2) For definite assignment analysis (JLS 16.1.5) of boolean conditional expressions of the form
			//     "if (c1 ? expr1 : expr2) use(v);" we need to check whether any variable v will be definitely
			//     assigned whenever the entire conditional expression evaluates to true (to reach the then branch).
			//     I.e., we need to collect flowInfo *towards* the overall outcome true/false
			//     (regardless of the evaluation of the condition).

			// to support (1) use the infos of both branches originating from the condition for computing the nullStatus:
			computeNullStatus(trueFlowInfo, falseFlowInfo, flowContext);

			// to support (2) we split the true/false branches according to their inner structure. Consider this:
			// if (b ? false : (true && (v = false))) return v; -- ok
			// - expr1 ("false") has no path towards true (mark as unreachable)
			// - expr2 ("(true && (v = false))") has a branch towards true on which v is assigned.
			//   -> merging these two branches yields: v is assigned
			// - the paths towards false are irrelevant since the enclosing if has no else.
			cst = this.optimizedIfTrueConstant;
			boolean isValueIfTrueOptimizedTrue = cst != null && cst != Constant.NotAConstant && cst.booleanValue() == true;
			boolean isValueIfTrueOptimizedFalse = cst != null && cst != Constant.NotAConstant && cst.booleanValue() == false;

			cst = this.optimizedIfFalseConstant;
			boolean isValueIfFalseOptimizedTrue = cst != null && cst != Constant.NotAConstant && cst.booleanValue() == true;
			boolean isValueIfFalseOptimizedFalse = cst != null && cst != Constant.NotAConstant && cst.booleanValue() == false;

			UnconditionalFlowInfo trueFlowTowardsTrue = trueFlowInfo.initsWhenTrue().unconditionalCopy();
			UnconditionalFlowInfo falseFlowTowardsTrue = falseFlowInfo.initsWhenTrue().unconditionalCopy();
			UnconditionalFlowInfo trueFlowTowardsFalse = trueFlowInfo.initsWhenFalse().unconditionalInits();
			UnconditionalFlowInfo falseFlowTowardsFalse = falseFlowInfo.initsWhenFalse().unconditionalInits();
			if (isValueIfTrueOptimizedFalse) {
				trueFlowTowardsTrue.setReachMode(FlowInfo.UNREACHABLE_OR_DEAD);
			}
			if (isValueIfFalseOptimizedFalse) {
				falseFlowTowardsTrue.setReachMode(FlowInfo.UNREACHABLE_OR_DEAD);
			}
			if (isValueIfTrueOptimizedTrue) {
				trueFlowTowardsFalse.setReachMode(FlowInfo.UNREACHABLE_OR_DEAD);
			}
			if (isValueIfFalseOptimizedTrue) {
				falseFlowTowardsFalse.setReachMode(FlowInfo.UNREACHABLE_OR_DEAD);
			}
			mergedInfo =
				FlowInfo.conditional(
					trueFlowTowardsTrue.mergedWith(falseFlowTowardsTrue),
					trueFlowTowardsFalse.mergedWith(falseFlowTowardsFalse));
		}
		this.mergedInitStateIndex =
			currentScope.methodScope().recordInitializationStates(mergedInfo);
		mergedInfo.setReachMode(mode);

		return mergedInfo;
	}

	@Override
	public boolean checkNPE(BlockScope scope, FlowContext flowContext, FlowInfo flowInfo, int ttlForFieldCheck) {
		if ((this.nullStatus & FlowInfo.NULL) != 0)
			scope.problemReporter().expressionNullReference(this);
		else if ((this.nullStatus & FlowInfo.POTENTIALLY_NULL) != 0)
			scope.problemReporter().expressionPotentialNullReference(this);
		return true; // all checking done
	}

	private void computeNullStatus(FlowInfo trueBranchInfo, FlowInfo falseBranchInfo, FlowContext flowContext) {
		// given that the condition cannot be optimized to a constant
		// we now merge the nullStatus from both branches:
		if (this.ifTrueNullStatus == -1) { // has this status been pre-computed?
			this.ifTrueNullStatus = this.valueIfTrue.nullStatus(trueBranchInfo, flowContext);
		}
		this.ifFalseNullStatus = this.valueIfFalse.nullStatus(falseBranchInfo, flowContext);

		if (this.ifTrueNullStatus == this.ifFalseNullStatus) {
			this.nullStatus = this.ifTrueNullStatus;
			return;
		}
		if (trueBranchInfo.reachMode() != FlowInfo.REACHABLE) {
			this.nullStatus = this.ifFalseNullStatus;
			return;
		}
		if (falseBranchInfo.reachMode() != FlowInfo.REACHABLE) {
			this.nullStatus = this.ifTrueNullStatus;
			return;
		}

		// is there a chance of null (or non-null)? -> potentially null etc.
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=133125
		int combinedStatus = this.ifTrueNullStatus|this.ifFalseNullStatus;
		int status = Expression.computeNullStatus(0, combinedStatus);
		if (status > 0)
			this.nullStatus = status;
	}

	/**
	 * Code generation for the conditional operator ?:
	 *
	 * @param currentScope org.eclipse.jdt.internal.compiler.lookup.BlockScope
	 * @param codeStream org.eclipse.jdt.internal.compiler.codegen.CodeStream
	 * @param valueRequired boolean
	*/
	@Override
	public void generateCode(
		BlockScope currentScope,
		CodeStream codeStream,
		boolean valueRequired) {

		int pc = codeStream.position;
		BranchLabel endifLabel, falseLabel;
		if (this.constant != Constant.NotAConstant) {
			if (valueRequired)
				codeStream.generateConstant(this.constant, this.implicitConversion);
			codeStream.recordPositionsFrom(pc, this.sourceStart);
			return;
		}
		Constant cst = this.condition.optimizedBooleanConstant();
		if (cst == Constant.NotAConstant) {
			cst = this.condition.optimizedNullComparisonConstant();
		}
		boolean needTruePart = !(cst != Constant.NotAConstant && cst.booleanValue() == false);
		boolean needFalsePart = !(cst != Constant.NotAConstant && cst.booleanValue() == true);

		endifLabel = new BranchLabel(codeStream);

		// Generate code for the condition
		falseLabel = new BranchLabel(codeStream);
		falseLabel.tagBits |= BranchLabel.USED;
		this.condition.generateOptimizedBoolean(
			currentScope,
			codeStream,
			null,
			falseLabel,
			cst == Constant.NotAConstant);

		if (this.trueInitStateIndex != -1) {
			codeStream.removeNotDefinitelyAssignedVariables(
				currentScope,
				this.trueInitStateIndex);
			codeStream.addDefinitelyAssignedVariables(currentScope, this.trueInitStateIndex);
		}
		// Then code generation
		if (needTruePart) {
			this.valueIfTrue.generateCode(currentScope, codeStream, valueRequired);
			if (needFalsePart) {
				// Jump over the else part
				int position = codeStream.position;
				codeStream.goto_(endifLabel);
				codeStream.recordPositionsFrom(position, this.valueIfTrue.sourceEnd);
				// Tune codestream stack size
				if (valueRequired) {
					switch(this.resolvedType.id) {
						case TypeIds.T_long :
						case TypeIds.T_double :
							codeStream.decrStackSize(2);
							break;
						default :
							codeStream.decrStackSize(1);
							break;
					}
				}
			}
		}
		if (needFalsePart) {
			if (this.falseInitStateIndex != -1) {
				codeStream.removeNotDefinitelyAssignedVariables(
					currentScope,
					this.falseInitStateIndex);
				codeStream.addDefinitelyAssignedVariables(currentScope, this.falseInitStateIndex);
			}
			if (falseLabel.forwardReferenceCount() > 0) {
				falseLabel.place();
			}
			this.valueIfFalse.generateCode(currentScope, codeStream, valueRequired);
			if (valueRequired) {
				codeStream.recordExpressionType(this.resolvedType);
			}
			if (needTruePart) {
				// End of if statement
				endifLabel.place();
			}
		}
		// May loose some local variable initializations : affecting the local variable attributes
		if (this.mergedInitStateIndex != -1) {
			codeStream.removeNotDefinitelyAssignedVariables(
				currentScope,
				this.mergedInitStateIndex);
		}
		// implicit conversion
		if (valueRequired)
			codeStream.generateImplicitConversion(this.implicitConversion);
		codeStream.recordPositionsFrom(pc, this.sourceStart);
	}

	/**
	 * Optimized boolean code generation for the conditional operator ?:
	*/
	@Override
	public void generateOptimizedBoolean(
		BlockScope currentScope,
		CodeStream codeStream,
		BranchLabel trueLabel,
		BranchLabel falseLabel,
		boolean valueRequired) {

		int pc = codeStream.position;

		if ((this.constant != Constant.NotAConstant) && (this.constant.typeID() == T_boolean) // constant
			|| ((this.valueIfTrue.implicitConversion & IMPLICIT_CONVERSION_MASK) >> 4) != T_boolean
			|| ((this.valueIfFalse.implicitConversion & IMPLICIT_CONVERSION_MASK) >> 4) != T_boolean) { // non boolean values
			super.generateOptimizedBoolean(currentScope, codeStream, trueLabel, falseLabel, valueRequired);
			return;
		}
		Constant cst = this.condition.constant;
		Constant condCst = this.condition.optimizedBooleanConstant();
		boolean needTruePart =
			!(((cst != Constant.NotAConstant) && (cst.booleanValue() == false))
				|| ((condCst != Constant.NotAConstant) && (condCst.booleanValue() == false)));
		boolean needFalsePart =
			!(((cst != Constant.NotAConstant) && (cst.booleanValue() == true))
				|| ((condCst != Constant.NotAConstant) && (condCst.booleanValue() == true)));

		BranchLabel internalFalseLabel, endifLabel = new BranchLabel(codeStream);

		// Generate code for the condition
		boolean needConditionValue = (cst == Constant.NotAConstant) && (condCst == Constant.NotAConstant);
		this.condition.generateOptimizedBoolean(
				currentScope,
				codeStream,
				null,
				internalFalseLabel = new BranchLabel(codeStream),
				needConditionValue);

		if (this.trueInitStateIndex != -1) {
			codeStream.removeNotDefinitelyAssignedVariables(
				currentScope,
				this.trueInitStateIndex);
			codeStream.addDefinitelyAssignedVariables(currentScope, this.trueInitStateIndex);
		}
		// Then code generation
		if (needTruePart) {
			this.valueIfTrue.generateOptimizedBoolean(currentScope, codeStream, trueLabel, falseLabel, valueRequired);

			if (needFalsePart) {
				// Jump over the else part
				JumpEndif: {
					if (falseLabel == null) {
						if (trueLabel != null) {
							// implicit falling through the FALSE case
							cst = this.optimizedIfTrueConstant;
							boolean isValueIfTrueOptimizedTrue = cst != null && cst != Constant.NotAConstant && cst.booleanValue() == true;
							if (isValueIfTrueOptimizedTrue) break JumpEndif; // no need to jump over, since branched to true already
						}
					} else {
						// implicit falling through the TRUE case
						if (trueLabel == null) {
							cst = this.optimizedIfTrueConstant;
							boolean isValueIfTrueOptimizedFalse = cst != null && cst != Constant.NotAConstant && cst.booleanValue() == false;
							if (isValueIfTrueOptimizedFalse) break JumpEndif; // no need to jump over, since branched to false already
						} else {
							// no implicit fall through TRUE/FALSE --> should never occur
						}
					}
					int position = codeStream.position;
					codeStream.goto_(endifLabel);
					codeStream.recordPositionsFrom(position, this.valueIfTrue.sourceEnd);
				}
				// No need to decrement codestream stack size
				// since valueIfTrue was already consumed by branch bytecode
			}
		}
		if (needFalsePart) {
			internalFalseLabel.place();
			if (this.falseInitStateIndex != -1) {
				codeStream.removeNotDefinitelyAssignedVariables(currentScope, this.falseInitStateIndex);
				codeStream.addDefinitelyAssignedVariables(currentScope, this.falseInitStateIndex);
			}
			this.valueIfFalse.generateOptimizedBoolean(currentScope, codeStream, trueLabel, falseLabel, valueRequired);

			// End of if statement
			endifLabel.place();
		}
		// May loose some local variable initializations : affecting the local variable attributes
		if (this.mergedInitStateIndex != -1) {
			codeStream.removeNotDefinitelyAssignedVariables(currentScope, this.mergedInitStateIndex);
		}
		// no implicit conversion for boolean values
		codeStream.recordPositionsFrom(pc, this.sourceEnd);
	}

	@Override
	public int nullStatus(FlowInfo flowInfo, FlowContext flowContext) {
		if ((this.implicitConversion & TypeIds.BOXING) != 0)
			return FlowInfo.NON_NULL;
		return this.nullStatus;
	}

	@Override
	public Constant optimizedBooleanConstant() {

		return this.optimizedBooleanConstant == null ? this.constant : this.optimizedBooleanConstant;
	}

	@Override
	public StringBuffer printExpressionNoParenthesis(int indent, StringBuffer output) {

		this.condition.printExpression(indent, output).append(" ? "); //$NON-NLS-1$
		this.valueIfTrue.printExpression(0, output).append(" : "); //$NON-NLS-1$
		return this.valueIfFalse.printExpression(0, output);
	}

	@Override
	public void addPatternVariables(BlockScope scope, CodeStream codeStream) {
		this.condition.addPatternVariables(scope, codeStream);
		this.valueIfTrue.addPatternVariables(scope, codeStream);
		this.valueIfFalse.addPatternVariables(scope, codeStream);
	}
	@Override
	public void collectPatternVariablesToScope(LocalVariableBinding[] variables, BlockScope scope) {
		// Introduce preceding variables introduced into the scope if this expression is actually evaluated
		this.condition.collectPatternVariablesToScope(variables, scope);
		this.valueIfFalse.collectPatternVariablesToScope(variables, scope);
		this.valueIfTrue.collectPatternVariablesToScope(variables, scope);
		if (this.valueIfFalse.containsPatternVariable() && this.valueIfTrue.containsPatternVariable() ) {
			LocalVariableBinding[] first = this.valueIfTrue.patternVarsWhenTrue;
			LocalVariableBinding[] second = this.valueIfFalse.patternVarsWhenTrue;
			if (first != null && second != null) {
				for (LocalVariableBinding localVariableBinding : first) {
					char[] name = localVariableBinding.name;
					for (LocalVariableBinding localVariableBinding2 : second) {
						if (CharOperation.equals(name, localVariableBinding2.name)) {
							scope.problemReporter().illegalRedeclarationOfPatternVar(localVariableBinding2, localVariableBinding2.declaration);
						}
					}
				}
			}
			first = this.valueIfTrue.patternVarsWhenFalse;
			second = this.valueIfFalse.patternVarsWhenFalse;
			if (first != null && second != null) {
				for (LocalVariableBinding localVariableBinding : first) {
					char[] name = localVariableBinding.name;
					for (LocalVariableBinding localVariableBinding2 : second) {
						if (CharOperation.equals(name, localVariableBinding2.name)) {
							scope.problemReporter().illegalRedeclarationOfPatternVar(localVariableBinding2, localVariableBinding2.declaration);
						}
					}
				}
			}
		}

		if (!this.condition.containsPatternVariable()) {
			return;
		}
		if (this.condition.getPatternVariableIntroduced() != null) {
			char[] name = this.condition.getPatternVariableIntroduced().name;
			LocalDeclaration localVar = this.valueIfTrue.getPatternVariableIntroduced();
			if (localVar != null && CharOperation.equals(name, localVar.name)) {
					scope.problemReporter().illegalRedeclarationOfPatternVar(localVar.binding, localVar);
					return;
			}
			localVar = this.valueIfFalse.getPatternVariableIntroduced();
			if (localVar != null && CharOperation.equals(name, localVar.name)) {
				scope.problemReporter().illegalRedeclarationOfPatternVar(localVar.binding, localVar);
				return;
			}
		}
		this.condition.collectPatternVariablesToScope(this.patternVarsWhenTrue, scope);

		variables = this.condition.getPatternVariablesWhenTrue();
		this.valueIfTrue.addPatternVariablesWhenTrue(variables);
		this.valueIfFalse.addPatternVariablesWhenFalse(variables);
		this.valueIfTrue.collectPatternVariablesToScope(variables, scope);

		variables = this.condition.getPatternVariablesWhenFalse();
		this.valueIfTrue.addPatternVariablesWhenFalse(variables);
		this.valueIfFalse.addPatternVariablesWhenTrue(variables);
		this.valueIfFalse.collectPatternVariablesToScope(variables, scope);
	}
	@Override
	public TypeBinding resolveType(BlockScope scope) {
		// JLS3 15.25
		LookupEnvironment env = scope.environment();
		final long sourceLevel = scope.compilerOptions().sourceLevel;
		boolean use15specifics = sourceLevel >= ClassFileConstants.JDK1_5;
		this.use18specifics = sourceLevel >= ClassFileConstants.JDK1_8;

		if (this.use18specifics) {
			if (this.expressionContext == ASSIGNMENT_CONTEXT || this.expressionContext == INVOCATION_CONTEXT) {
				this.valueIfTrue.setExpressionContext(this.expressionContext);
				this.valueIfTrue.setExpectedType(this.expectedType);
				this.valueIfFalse.setExpressionContext(this.expressionContext);
				this.valueIfFalse.setExpectedType(this.expectedType);
			}
		}

		collectPatternVariablesToScope(null, scope);

		if (this.constant != Constant.NotAConstant) {
			this.constant = Constant.NotAConstant;
			TypeBinding conditionType = this.condition.resolveTypeExpecting(scope, TypeBinding.BOOLEAN);
			this.condition.computeConversion(scope, TypeBinding.BOOLEAN, conditionType);

			if (this.valueIfTrue instanceof CastExpression) this.valueIfTrue.bits |= DisableUnnecessaryCastCheck; // will check later on
			this.originalValueIfTrueType = this.valueIfTrue.resolveType(scope);

			if (this.valueIfFalse instanceof CastExpression) this.valueIfFalse.bits |= DisableUnnecessaryCastCheck; // will check later on
			this.originalValueIfFalseType = this.valueIfFalse.resolveType(scope);

			if (conditionType == null || this.originalValueIfTrueType == null || this.originalValueIfFalseType == null)
				return null;
		} else {
			if (this.originalValueIfTrueType.kind() == Binding.POLY_TYPE)
				this.originalValueIfTrueType = this.valueIfTrue.resolveType(scope);
			if (this.originalValueIfFalseType.kind() == Binding.POLY_TYPE)
				this.originalValueIfFalseType = this.valueIfFalse.resolveType(scope);

			if (this.originalValueIfTrueType == null || !this.originalValueIfTrueType.isValidBinding())
				return this.resolvedType = null;
			if (this.originalValueIfFalseType == null || !this.originalValueIfFalseType.isValidBinding())
				return this.resolvedType = null;
		}
		// Propagate the constant value from the valueIfTrue and valueIFFalse expression if it is possible
		Constant condConstant, trueConstant, falseConstant;
		if ((condConstant = this.condition.constant) != Constant.NotAConstant
			&& (trueConstant = this.valueIfTrue.constant) != Constant.NotAConstant
			&& (falseConstant = this.valueIfFalse.constant) != Constant.NotAConstant) {
			// all terms are constant expression so we can propagate the constant
			// from valueIFTrue or valueIfFalse to the receiver constant
			this.constant = condConstant.booleanValue() ? trueConstant : falseConstant;
		}
		if (isPolyExpression()) {
			if (this.expectedType == null || !this.expectedType.isProperType(true)) {
				// We will be back here in case of a PolyTypeBinding. So, to enable
				// further processing, set it back to default.
				this.constant = Constant.NotAConstant;
				return new PolyTypeBinding(this);
			}
			return this.resolvedType = computeConversions(scope, this.expectedType) ? this.expectedType : null;
		}

		TypeBinding valueIfTrueType = this.originalValueIfTrueType;
		TypeBinding valueIfFalseType = this.originalValueIfFalseType;
		if (use15specifics && TypeBinding.notEquals(valueIfTrueType, valueIfFalseType)) {
			if (valueIfTrueType.isBaseType()) {
				if (valueIfFalseType.isBaseType()) {
					// bool ? baseType : baseType
					if (valueIfTrueType == TypeBinding.NULL) {  // bool ? null : 12 --> Integer
						valueIfFalseType = env.computeBoxingType(valueIfFalseType); // boxing
					} else if (valueIfFalseType == TypeBinding.NULL) {  // bool ? 12 : null --> Integer
						valueIfTrueType = env.computeBoxingType(valueIfTrueType); // boxing
					}
				} else {
					// bool ? baseType : nonBaseType
					TypeBinding unboxedIfFalseType = valueIfFalseType.isBaseType() ? valueIfFalseType : env.computeBoxingType(valueIfFalseType);
					if (valueIfTrueType.isNumericType() && unboxedIfFalseType.isNumericType()) {
						valueIfFalseType = unboxedIfFalseType; // unboxing
					} else if (valueIfTrueType != TypeBinding.NULL) {  // bool ? 12 : new Integer(12) --> int
						valueIfFalseType = env.computeBoxingType(valueIfFalseType); // unboxing
					}
				}
			} else if (valueIfFalseType.isBaseType()) {
					// bool ? nonBaseType : baseType
					TypeBinding unboxedIfTrueType = valueIfTrueType.isBaseType() ? valueIfTrueType : env.computeBoxingType(valueIfTrueType);
					if (unboxedIfTrueType.isNumericType() && valueIfFalseType.isNumericType()) {
						valueIfTrueType = unboxedIfTrueType; // unboxing
					} else if (valueIfFalseType != TypeBinding.NULL) {  // bool ? new Integer(12) : 12 --> int
						valueIfTrueType = env.computeBoxingType(valueIfTrueType); // unboxing
					}
			} else {
					// bool ? nonBaseType : nonBaseType
					TypeBinding unboxedIfTrueType = env.computeBoxingType(valueIfTrueType);
					TypeBinding unboxedIfFalseType = env.computeBoxingType(valueIfFalseType);
					if (unboxedIfTrueType.isNumericType() && unboxedIfFalseType.isNumericType()) {
						valueIfTrueType = unboxedIfTrueType;
						valueIfFalseType = unboxedIfFalseType;
					}
			}
		}
		if (TypeBinding.equalsEquals(valueIfTrueType, valueIfFalseType)) { // harmed the implicit conversion
			this.valueIfTrue.computeConversion(scope, valueIfTrueType, this.originalValueIfTrueType);
			this.valueIfFalse.computeConversion(scope, valueIfFalseType, this.originalValueIfFalseType);
			if (TypeBinding.equalsEquals(valueIfTrueType, TypeBinding.BOOLEAN)) {
				this.optimizedIfTrueConstant = this.valueIfTrue.optimizedBooleanConstant();
				this.optimizedIfFalseConstant = this.valueIfFalse.optimizedBooleanConstant();
				if (this.optimizedIfTrueConstant != Constant.NotAConstant
						&& this.optimizedIfFalseConstant != Constant.NotAConstant
						&& this.optimizedIfTrueConstant.booleanValue() == this.optimizedIfFalseConstant.booleanValue()) {
					// a ? true : true  /   a ? false : false
					this.optimizedBooleanConstant = this.optimizedIfTrueConstant;
				} else if ((condConstant = this.condition.optimizedBooleanConstant()) != Constant.NotAConstant) { // Propagate the optimized boolean constant if possible
					this.optimizedBooleanConstant = condConstant.booleanValue()
						? this.optimizedIfTrueConstant
						: this.optimizedIfFalseConstant;
				}
			}
			return this.resolvedType = NullAnnotationMatching.moreDangerousType(valueIfTrueType, valueIfFalseType);
		}
		// Determine the return type depending on argument types
		// Numeric types
		if (valueIfTrueType.isNumericType() && valueIfFalseType.isNumericType()) {
			// (Short x Byte) or (Byte x Short)"
			if ((TypeBinding.equalsEquals(valueIfTrueType, TypeBinding.BYTE) && TypeBinding.equalsEquals(valueIfFalseType, TypeBinding.SHORT))
				|| (TypeBinding.equalsEquals(valueIfTrueType, TypeBinding.SHORT) && TypeBinding.equalsEquals(valueIfFalseType, TypeBinding.BYTE))) {
				this.valueIfTrue.computeConversion(scope, TypeBinding.SHORT, this.originalValueIfTrueType);
				this.valueIfFalse.computeConversion(scope, TypeBinding.SHORT, this.originalValueIfFalseType);
				return this.resolvedType = TypeBinding.SHORT;
			}
			// <Byte|Short|Char> x constant(Int)  ---> <Byte|Short|Char>   and reciprocally
			if ((TypeBinding.equalsEquals(valueIfTrueType, TypeBinding.BYTE) || TypeBinding.equalsEquals(valueIfTrueType, TypeBinding.SHORT) || TypeBinding.equalsEquals(valueIfTrueType, TypeBinding.CHAR))
					&& (TypeBinding.equalsEquals(valueIfFalseType, TypeBinding.INT)
						&& this.valueIfFalse.isConstantValueOfTypeAssignableToType(valueIfFalseType, valueIfTrueType))) {
				this.valueIfTrue.computeConversion(scope, valueIfTrueType, this.originalValueIfTrueType);
				this.valueIfFalse.computeConversion(scope, valueIfTrueType, this.originalValueIfFalseType);
				return this.resolvedType = valueIfTrueType;
			}
			if ((TypeBinding.equalsEquals(valueIfFalseType, TypeBinding.BYTE)
					|| TypeBinding.equalsEquals(valueIfFalseType, TypeBinding.SHORT)
					|| TypeBinding.equalsEquals(valueIfFalseType, TypeBinding.CHAR))
					&& (TypeBinding.equalsEquals(valueIfTrueType, TypeBinding.INT)
						&& this.valueIfTrue.isConstantValueOfTypeAssignableToType(valueIfTrueType, valueIfFalseType))) {
				this.valueIfTrue.computeConversion(scope, valueIfFalseType, this.originalValueIfTrueType);
				this.valueIfFalse.computeConversion(scope, valueIfFalseType, this.originalValueIfFalseType);
				return this.resolvedType = valueIfFalseType;
			}
			// Manual binary numeric promotion
			// int
			if (BaseTypeBinding.isNarrowing(valueIfTrueType.id, T_int)
					&& BaseTypeBinding.isNarrowing(valueIfFalseType.id, T_int)) {
				this.valueIfTrue.computeConversion(scope, TypeBinding.INT, this.originalValueIfTrueType);
				this.valueIfFalse.computeConversion(scope, TypeBinding.INT, this.originalValueIfFalseType);
				return this.resolvedType = TypeBinding.INT;
			}
			// long
			if (BaseTypeBinding.isNarrowing(valueIfTrueType.id, T_long)
					&& BaseTypeBinding.isNarrowing(valueIfFalseType.id, T_long)) {
				this.valueIfTrue.computeConversion(scope, TypeBinding.LONG, this.originalValueIfTrueType);
				this.valueIfFalse.computeConversion(scope, TypeBinding.LONG, this.originalValueIfFalseType);
				return this.resolvedType = TypeBinding.LONG;
			}
			// float
			if (BaseTypeBinding.isNarrowing(valueIfTrueType.id, T_float)
					&& BaseTypeBinding.isNarrowing(valueIfFalseType.id, T_float)) {
				this.valueIfTrue.computeConversion(scope, TypeBinding.FLOAT, this.originalValueIfTrueType);
				this.valueIfFalse.computeConversion(scope, TypeBinding.FLOAT, this.originalValueIfFalseType);
				return this.resolvedType = TypeBinding.FLOAT;
			}
			// double
			this.valueIfTrue.computeConversion(scope, TypeBinding.DOUBLE, this.originalValueIfTrueType);
			this.valueIfFalse.computeConversion(scope, TypeBinding.DOUBLE, this.originalValueIfFalseType);
			return this.resolvedType = TypeBinding.DOUBLE;
		}
		// Type references (null null is already tested)
		if (valueIfTrueType.isBaseType() && valueIfTrueType != TypeBinding.NULL) {
			if (use15specifics) {
				valueIfTrueType = env.computeBoxingType(valueIfTrueType);
			} else {
				scope.problemReporter().conditionalArgumentsIncompatibleTypes(this, valueIfTrueType, valueIfFalseType);
				return null;
			}
		}
		if (valueIfFalseType.isBaseType() && valueIfFalseType != TypeBinding.NULL) {
			if (use15specifics) {
				valueIfFalseType = env.computeBoxingType(valueIfFalseType);
			} else {
				scope.problemReporter().conditionalArgumentsIncompatibleTypes(this, valueIfTrueType, valueIfFalseType);
				return null;
			}
		}
		if (use15specifics) {
			// >= 1.5 : LUB(operand types) must exist
			TypeBinding commonType = null;
			if (valueIfTrueType == TypeBinding.NULL) {
				commonType = valueIfFalseType;
			} else if (valueIfFalseType == TypeBinding.NULL) {
				commonType = valueIfTrueType;
			} else {
				commonType = scope.lowerUpperBound(new TypeBinding[] { valueIfTrueType, valueIfFalseType });
			}
			if (commonType != null) {
				this.valueIfTrue.computeConversion(scope, commonType, this.originalValueIfTrueType);
				this.valueIfFalse.computeConversion(scope, commonType, this.originalValueIfFalseType);
				return this.resolvedType = commonType.capture(scope, this.sourceStart, this.sourceEnd);
			}
		} else {
			// < 1.5 : one operand must be convertible to the other
			if (valueIfFalseType.isCompatibleWith(valueIfTrueType)) {
				this.valueIfTrue.computeConversion(scope, valueIfTrueType, this.originalValueIfTrueType);
				this.valueIfFalse.computeConversion(scope, valueIfTrueType, this.originalValueIfFalseType);
				return this.resolvedType = valueIfTrueType;
			} else if (valueIfTrueType.isCompatibleWith(valueIfFalseType)) {
				this.valueIfTrue.computeConversion(scope, valueIfFalseType, this.originalValueIfTrueType);
				this.valueIfFalse.computeConversion(scope, valueIfFalseType, this.originalValueIfFalseType);
				return this.resolvedType = valueIfFalseType;
			}
		}
		scope.problemReporter().conditionalArgumentsIncompatibleTypes(
			this,
			valueIfTrueType,
			valueIfFalseType);
		return null;
	}

	protected boolean computeConversions(BlockScope scope, TypeBinding targetType) {
		boolean ok = true;
		if (this.originalValueIfTrueType != null && this.originalValueIfTrueType.isValidBinding()) {
			if (this.valueIfTrue.isConstantValueOfTypeAssignableToType(this.originalValueIfTrueType, targetType)
					|| this.originalValueIfTrueType.isCompatibleWith(targetType)) {

				this.valueIfTrue.computeConversion(scope, targetType, this.originalValueIfTrueType);
				if (this.originalValueIfTrueType.needsUncheckedConversion(targetType)) {
					scope.problemReporter().unsafeTypeConversion(this.valueIfTrue, this.originalValueIfTrueType, targetType);
				}
				if (this.valueIfTrue instanceof CastExpression
						&& (this.valueIfTrue.bits & (ASTNode.UnnecessaryCast|ASTNode.DisableUnnecessaryCastCheck)) == 0) {
					CastExpression.checkNeedForAssignedCast(scope, targetType, (CastExpression) this.valueIfTrue);
				}
			} else if (isBoxingCompatible(this.originalValueIfTrueType, targetType, this.valueIfTrue, scope)) {
				this.valueIfTrue.computeConversion(scope, targetType, this.originalValueIfTrueType);
				if (this.valueIfTrue instanceof CastExpression
						&& (this.valueIfTrue.bits & (ASTNode.UnnecessaryCast|ASTNode.DisableUnnecessaryCastCheck)) == 0) {
					CastExpression.checkNeedForAssignedCast(scope, targetType, (CastExpression) this.valueIfTrue);
				}
			} else {
				scope.problemReporter().typeMismatchError(this.originalValueIfTrueType, targetType, this.valueIfTrue, null);
				ok = false;
			}
		}
		if (this.originalValueIfFalseType != null && this.originalValueIfFalseType.isValidBinding()) {
			if (this.valueIfFalse.isConstantValueOfTypeAssignableToType(this.originalValueIfFalseType, targetType)
					|| this.originalValueIfFalseType.isCompatibleWith(targetType)) {

				this.valueIfFalse.computeConversion(scope, targetType, this.originalValueIfFalseType);
				if (this.originalValueIfFalseType.needsUncheckedConversion(targetType)) {
					scope.problemReporter().unsafeTypeConversion(this.valueIfFalse, this.originalValueIfFalseType, targetType);
				}
				if (this.valueIfFalse instanceof CastExpression
						&& (this.valueIfFalse.bits & (ASTNode.UnnecessaryCast|ASTNode.DisableUnnecessaryCastCheck)) == 0) {
					CastExpression.checkNeedForAssignedCast(scope, targetType, (CastExpression) this.valueIfFalse);
				}
			} else if (isBoxingCompatible(this.originalValueIfFalseType, targetType, this.valueIfFalse, scope)) {
				this.valueIfFalse.computeConversion(scope, targetType, this.originalValueIfFalseType);
				if (this.valueIfFalse instanceof CastExpression
						&& (this.valueIfFalse.bits & (ASTNode.UnnecessaryCast|ASTNode.DisableUnnecessaryCastCheck)) == 0) {
					CastExpression.checkNeedForAssignedCast(scope, targetType, (CastExpression) this.valueIfFalse);
				}
			} else {
				scope.problemReporter().typeMismatchError(this.originalValueIfFalseType, targetType, this.valueIfFalse, null);
				ok = false;
			}
		}
		return ok;
	}

	@Override
	public void setExpectedType(TypeBinding expectedType) {
		this.expectedType = expectedType;
	}

	@Override
	public void setExpressionContext(ExpressionContext context) {
		this.expressionContext = context;
	}

	@Override
	public ExpressionContext getExpressionContext() {
		return this.expressionContext;
	}

	@Override
	public Expression[] getPolyExpressions() {
		Expression [] truePolys = this.valueIfTrue.getPolyExpressions();
		Expression [] falsePolys = this.valueIfFalse.getPolyExpressions();
		if (truePolys.length == 0)
			return falsePolys;
		if (falsePolys.length == 0)
			return truePolys;
		Expression [] allPolys = new Expression [truePolys.length + falsePolys.length];
		System.arraycopy(truePolys, 0, allPolys, 0, truePolys.length);
		System.arraycopy(falsePolys, 0, allPolys, truePolys.length, falsePolys.length);
		return allPolys;
	}

	@Override
	public boolean isPertinentToApplicability(TypeBinding targetType, MethodBinding method) {
		return this.valueIfTrue.isPertinentToApplicability(targetType, method)
				&& this.valueIfFalse.isPertinentToApplicability(targetType, method);
	}

	@Override
	public boolean isPotentiallyCompatibleWith(TypeBinding targetType, Scope scope) {
		return this.valueIfTrue.isPotentiallyCompatibleWith(targetType, scope)
				&& this.valueIfFalse.isPotentiallyCompatibleWith(targetType, scope);
	}

	@Override
	public boolean isFunctionalType() {
		return this.valueIfTrue.isFunctionalType() || this.valueIfFalse.isFunctionalType(); // Even if only one arm is functional type, this will require a functional interface target
	}

	@Override
	public boolean isPolyExpression() throws UnsupportedOperationException {

		if (!this.use18specifics)
			return false;

		if (this.isPolyExpression)
			return true;

		if (this.expressionContext != ASSIGNMENT_CONTEXT && this.expressionContext != INVOCATION_CONTEXT)
			return false;

		if (this.originalValueIfTrueType == null || this.originalValueIfFalseType == null) // resolution error.
			return false;

		if (this.valueIfTrue.isPolyExpression() || this.valueIfFalse.isPolyExpression())
			return true;

		// "... unless both operands produce primitives (or boxed primitives)":
		if (this.originalValueIfTrueType.isBaseType() || (this.originalValueIfTrueType.id >= TypeIds.T_JavaLangByte && this.originalValueIfTrueType.id <= TypeIds.T_JavaLangBoolean)) {
			if (this.originalValueIfFalseType.isBaseType() || (this.originalValueIfFalseType.id >= TypeIds.T_JavaLangByte && this.originalValueIfFalseType.id <= TypeIds.T_JavaLangBoolean))
				return false;
		}

		// clause around generic method's return type prior to instantiation needs double check.
		return this.isPolyExpression = true;
	}

	@Override
	public boolean isCompatibleWith(TypeBinding left, Scope scope) {
		return isPolyExpression() ? this.valueIfTrue.isCompatibleWith(left, scope) && this.valueIfFalse.isCompatibleWith(left, scope) :
			super.isCompatibleWith(left, scope);
	}

	@Override
	public boolean isBoxingCompatibleWith(TypeBinding targetType, Scope scope) {
		// Note: compatibility check may have failed in just one arm and we may have reached here.
		return isPolyExpression() ? (this.valueIfTrue.isCompatibleWith(targetType, scope) ||
				                     this.valueIfTrue.isBoxingCompatibleWith(targetType, scope)) &&
				                    (this.valueIfFalse.isCompatibleWith(targetType, scope) ||
				                     this.valueIfFalse.isBoxingCompatibleWith(targetType, scope)) :
			super.isBoxingCompatibleWith(targetType, scope);
	}

	@Override
	public boolean sIsMoreSpecific(TypeBinding s, TypeBinding t, Scope scope) {
		if (super.sIsMoreSpecific(s, t, scope))
			return true;
		return isPolyExpression() ?
				this.valueIfTrue.sIsMoreSpecific(s, t, scope) && this.valueIfFalse.sIsMoreSpecific(s, t, scope):
				false;
	}

	@Override
	public void traverse(ASTVisitor visitor, BlockScope scope) {
		if (visitor.visit(this, scope)) {
			this.condition.traverse(visitor, scope);
			this.valueIfTrue.traverse(visitor, scope);
			this.valueIfFalse.traverse(visitor, scope);
		}
		visitor.endVisit(this, scope);
	}
}

