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
 *     Stephan Herrmann - Contribution for
 *								bug 383368 - [compiler][null] syntactic null analysis for field references
 *								Bug 392238 - [1.8][compiler][null] Detect semantically invalid null type annotations
 *								Bug 416307 - [1.8][compiler][null] subclass with type parameter substitution confuses null checking
 *								Bug 392099 - [1.8][compiler][null] Apply null annotation on types for null analysis
 *								Bug 417295 - [1.8[[null] Massage type annotated null analysis to gel well with deep encoded type bindings.
 *								Bug 392238 - [1.8][compiler][null] Detect semantically invalid null type annotations
 *								Bug 435570 - [1.8][null] @NonNullByDefault illegally tries to affect "throws E"
 *								Bug 466713 - Null Annotations: NullPointerException using <int @Nullable []> as Type Param
 *        Andy Clement - Contributions for
 *                          Bug 383624 - [1.8][compiler] Revive code generation support for type annotations (from Olivier's work)
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.ast;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.ast.TypeReference.AnnotationPosition;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.codegen.*;
import org.eclipse.jdt.internal.compiler.flow.*;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.impl.Constant;
import org.eclipse.jdt.internal.compiler.lookup.*;

public class InstanceOfExpression extends OperatorExpression {

	public Expression expression;
	public TypeReference type;
	public LocalDeclaration elementVariable;
	static final char[] SECRET_INSTANCEOF_PATTERN_EXPRESSION_VALUE = " instanceOfPatternExpressionValue".toCharArray(); //$NON-NLS-1$

	public LocalVariableBinding secretInstanceOfPatternExpressionValue = null;

public InstanceOfExpression(Expression expression, TypeReference type) {
	this.expression = expression;
	this.type = type;
	type.bits |= IgnoreRawTypeCheck; // https://bugs.eclipse.org/bugs/show_bug.cgi?id=282141
	this.bits |= INSTANCEOF << OperatorSHIFT;
	this.sourceStart = expression.sourceStart;
	this.sourceEnd = type.sourceEnd;
}
public InstanceOfExpression(Expression expression, Pattern pattern) {
	this.expression = expression;
	// As of now, instanceof can only have a type pattern variable.
	// So, extract the local variable definition and ignore the pattern
	this.elementVariable = pattern.getPatternVariableIntroduced();
	this.type = this.elementVariable.type;
	this.bits |= INSTANCEOF << OperatorSHIFT;
	this.sourceStart = expression.sourceStart;
	this.sourceEnd = this.elementVariable.declarationSourceEnd;
}

@Override
public FlowInfo analyseCode(BlockScope currentScope, FlowContext flowContext, FlowInfo flowInfo) {
	LocalVariableBinding local = this.expression.localVariableBinding();
	FlowInfo initsWhenTrue = null;
	if (local != null && (local.type.tagBits & TagBits.IsBaseType) == 0) {
		flowInfo = this.expression.analyseCode(currentScope, flowContext, flowInfo).
			unconditionalInits();
		initsWhenTrue = flowInfo.copy();
		initsWhenTrue.markAsComparedEqualToNonNull(local);
		flowContext.recordUsingNullReference(currentScope, local,
				this.expression, FlowContext.CAN_ONLY_NULL | FlowContext.IN_INSTANCEOF, flowInfo);
		// no impact upon enclosing try context
		flowInfo =  FlowInfo.conditional(initsWhenTrue.copy(), flowInfo.copy());
	} else if (this.expression instanceof Reference) {
		if (currentScope.compilerOptions().enableSyntacticNullAnalysisForFields) {
			FieldBinding field = ((Reference)this.expression).lastFieldBinding();
			if (field != null && (field.type.tagBits & TagBits.IsBaseType) == 0) {
				flowContext.recordNullCheckedFieldReference((Reference) this.expression, 1);
			}
		}
	}
	if (initsWhenTrue == null) {
		flowInfo = this.expression.analyseCode(currentScope, flowContext, flowInfo).
				unconditionalInits();
		if (this.elementVariable != null) {
			initsWhenTrue = flowInfo.copy();
		}
	}
	if (this.elementVariable != null) {
		initsWhenTrue.markAsDefinitelyAssigned(this.elementVariable.binding);
	}
	return (initsWhenTrue == null) ? flowInfo :
			FlowInfo.conditional(initsWhenTrue, flowInfo.copy());
}
/**
 * Code generation for instanceOfExpression
 *
 * @param currentScope org.eclipse.jdt.internal.compiler.lookup.BlockScope
 * @param codeStream org.eclipse.jdt.internal.compiler.codegen.CodeStream
 * @param valueRequired boolean
*/
@Override
public void generateCode(BlockScope currentScope, CodeStream codeStream, boolean valueRequired) {
	if (this.elementVariable != null && this.elementVariable.binding != null) {
		this.elementVariable.binding.modifiers &= ~ExtraCompilerModifiers.AccPatternVariable;
	}
	addPatternVariables(currentScope, codeStream);

	int pc = codeStream.position;

	if (this.elementVariable != null) {
		addAssignment(currentScope, codeStream, this.secretInstanceOfPatternExpressionValue);
		codeStream.load(this.secretInstanceOfPatternExpressionValue);
	} else {
		this.expression.generateCode(currentScope, codeStream, true);
	}

	codeStream.instance_of(this.type, this.type.resolvedType);
	if (this.elementVariable != null) {
		BranchLabel actionLabel = new BranchLabel(codeStream);
		codeStream.dup();
		codeStream.ifeq(actionLabel);
		codeStream.load(this.secretInstanceOfPatternExpressionValue);
		codeStream.removeVariable(this.secretInstanceOfPatternExpressionValue);
		codeStream.checkcast(this.type, this.type.resolvedType, codeStream.position);
		this.elementVariable.binding.recordInitializationStartPC(codeStream.position);
		codeStream.store(this.elementVariable.binding, false);
		codeStream.removeVariable(this.elementVariable.binding);
		codeStream.recordPositionsFrom(codeStream.position, this.sourceEnd);
		actionLabel.place();
	}
	if (valueRequired) {
		codeStream.generateImplicitConversion(this.implicitConversion);
	} else {
		codeStream.pop();
	}
	codeStream.recordPositionsFrom(pc, this.sourceStart);
}
@Override
public void generateOptimizedBoolean(BlockScope currentScope, CodeStream codeStream, BranchLabel trueLabel, BranchLabel falseLabel, boolean valueRequired) {
	// a label valued to nil means: by default we fall through the case...
	// both nil means we leave the value on the stack

	if (this.elementVariable == null) {
		super.generateOptimizedBoolean(currentScope, codeStream, trueLabel, falseLabel, valueRequired);
		return;
	}
	Constant cst = optimizedBooleanConstant();
	addPatternVariables(currentScope, codeStream);

	int pc = codeStream.position;

	addAssignment(currentScope, codeStream, this.secretInstanceOfPatternExpressionValue);
	codeStream.load(this.secretInstanceOfPatternExpressionValue);

	BranchLabel nextSibling = falseLabel != null ? falseLabel : new BranchLabel(codeStream);
	codeStream.instance_of(this.type, this.type.resolvedType);
	if (this.elementVariable != null) {
		codeStream.ifeq(nextSibling);
		codeStream.load(this.secretInstanceOfPatternExpressionValue);
		codeStream.checkcast(this.type, this.type.resolvedType, codeStream.position);
		codeStream.dup();
		codeStream.store(this.elementVariable.binding, false);

		codeStream.load(this.secretInstanceOfPatternExpressionValue);
		codeStream.removeVariable(this.secretInstanceOfPatternExpressionValue);
		codeStream.checkcast(this.type, this.type.resolvedType, codeStream.position);
	}
	if (valueRequired && cst == Constant.NotAConstant) {
		codeStream.generateImplicitConversion(this.implicitConversion);
	} else {
		codeStream.pop();
	}
	codeStream.recordPositionsFrom(pc, this.sourceStart);


	if ((cst != Constant.NotAConstant) && (cst.typeID() == TypeIds.T_boolean)) {
		pc = codeStream.position;
		if (cst.booleanValue() == true) {
			// constant == true
			if (valueRequired) {
				if (falseLabel == null) {
					// implicit falling through the FALSE case
					if (trueLabel != null) {
						codeStream.goto_(trueLabel);
					}
				}
			}
		} else {
			if (valueRequired) {
				if (falseLabel != null) {
					// implicit falling through the TRUE case
					if (trueLabel == null) {
						codeStream.goto_(falseLabel);
					}
				}
			}
		}
		codeStream.recordPositionsFrom(pc, this.sourceStart);
	} else  {
		// branching
		int position = codeStream.position;
		if (valueRequired) {
			if (falseLabel == null) {
				if (trueLabel != null) {
					// Implicit falling through the FALSE case
					codeStream.if_acmpeq(trueLabel);
				}
			} else {
				if (trueLabel == null) {
					// Implicit falling through the TRUE case
					codeStream.if_acmpne(falseLabel);
				} else {
					// No implicit fall through TRUE/FALSE --> should never occur
				}
			}
		}
		codeStream.recordPositionsFrom(position, this.sourceEnd);
	}
	if (nextSibling != falseLabel)
		nextSibling.place();
}

private void addAssignment(BlockScope currentScope, CodeStream codeStream, LocalVariableBinding local) {
	assert local != null;
	SingleNameReference lhs = new SingleNameReference(local.name, 0);
	lhs.binding = local;
	lhs.bits &= ~ASTNode.RestrictiveFlagMASK; // clear bits
	lhs.bits |= Binding.LOCAL;
	lhs.bits |= ASTNode.IsSecretYieldValueUsage;
	((LocalVariableBinding) lhs.binding).markReferenced(); // TODO : Can be skipped?
	Assignment assignment = new Assignment(lhs, this.expression, 0);
	assignment.generateCode(currentScope, codeStream);
	codeStream.addVariable(this.secretInstanceOfPatternExpressionValue);
}

@Override
public StringBuffer printExpressionNoParenthesis(int indent, StringBuffer output) {
	this.expression.printExpression(indent, output).append(" instanceof "); //$NON-NLS-1$
	return this.elementVariable == null ? this.type.print(0, output) : this.elementVariable.printAsExpression(0, output);
}

@Override
public void addPatternVariables(BlockScope currentScope, CodeStream codeStream) {
	if (this.elementVariable != null) {
		codeStream.addVisibleLocalVariable(this.elementVariable.binding);
	}
}
public boolean resolvePatternVariable(BlockScope scope) {
	if (this.elementVariable == null) return false;
	if (this.elementVariable.binding == null) {
		this.elementVariable.modifiers |= ExtraCompilerModifiers.AccPatternVariable;
		this.elementVariable.resolve(scope, true);
		// Kludge - to remove the AccBlankFinal added by the LocalDeclaration#resolve() due to the
		// missing initializer
		this.elementVariable.modifiers &= ~ExtraCompilerModifiers.AccBlankFinal;
		this.elementVariable.binding.modifiers |= ExtraCompilerModifiers.AccPatternVariable;
		this.elementVariable.binding.useFlag = LocalVariableBinding.USED;
		// Why cant this be done in the constructor?
		this.type = this.elementVariable.type;
	}
	return true;
}
@Override
public void collectPatternVariablesToScope(LocalVariableBinding[] variables, BlockScope scope) {
	this.expression.collectPatternVariablesToScope(variables, scope);
	if (this.elementVariable != null) {
		if (this.elementVariable.binding == null) {
			resolvePatternVariable(scope);
			if (variables != null) {
				for (LocalVariableBinding variable : variables) {
					if (CharOperation.equals(this.elementVariable.name, variable.name)) {
						scope.problemReporter().redefineLocal(this.elementVariable);
					}
				}
			}
		}
		if (this.patternVarsWhenTrue == null) {
			this.patternVarsWhenTrue = new LocalVariableBinding[1];
			this.patternVarsWhenTrue[0] = this.elementVariable.binding;
		} else {
			this.addPatternVariablesWhenTrue(new LocalVariableBinding[] {this.elementVariable.binding});
		}
	}

}
@Override
public boolean containsPatternVariable() {
	return this.elementVariable != null;
}
@Override
public LocalDeclaration getPatternVariableIntroduced() {
	return this.elementVariable;
}
private void addSecretInstanceOfPatternExpressionValue(BlockScope scope1) {
	LocalVariableBinding local =
			new LocalVariableBinding(
				InstanceOfExpression.SECRET_INSTANCEOF_PATTERN_EXPRESSION_VALUE,
				TypeBinding.wellKnownType(scope1, T_JavaLangObject),
				ClassFileConstants.AccDefault,
				false);
	local.setConstant(Constant.NotAConstant);
	local.useFlag = LocalVariableBinding.USED;
	local.declaration = new LocalDeclaration(InstanceOfExpression.SECRET_INSTANCEOF_PATTERN_EXPRESSION_VALUE, 0, 0);
	scope1.addLocalVariable(local);
	this.secretInstanceOfPatternExpressionValue = local;
}

@Override
public TypeBinding resolveType(BlockScope scope) {
	this.constant = Constant.NotAConstant;
	if (this.elementVariable != null)
		addSecretInstanceOfPatternExpressionValue(scope);
	resolvePatternVariable(scope);
	TypeBinding checkedType = this.type.resolveType(scope, true /* check bounds*/);
	if (this.expression instanceof CastExpression) {
		((CastExpression) this.expression).setInstanceofType(checkedType); // for cast expression we need to know instanceof type to not tag unnecessary when needed
	}
	TypeBinding expressionType = this.expression.resolveType(scope);
	if (expressionType != null && checkedType != null && this.type.hasNullTypeAnnotation(AnnotationPosition.ANY)) {
		// don't complain if the entire operation is redundant anyway
		if (!expressionType.isCompatibleWith(checkedType) || NullAnnotationMatching.analyse(checkedType, expressionType, -1).isAnyMismatch())
			scope.problemReporter().nullAnnotationUnsupportedLocation(this.type);
	}
	if (expressionType == null || checkedType == null)
		return null;

	if (this.secretInstanceOfPatternExpressionValue != null && expressionType != TypeBinding.NULL)
		this.secretInstanceOfPatternExpressionValue.type = expressionType;

	if (!checkedType.isReifiable()) {
		CompilerOptions options = scope.compilerOptions();
		// Report same as before for older compliances
		if (options.complianceLevel < ClassFileConstants.JDK16) {
			scope.problemReporter().illegalInstanceOfGenericType(checkedType, this);
		} else {
			if (expressionType != TypeBinding.NULL) {
				boolean isLegal = checkCastTypesCompatibility(scope, checkedType, expressionType, this.expression, true);
				if (!isLegal || (this.bits & ASTNode.UnsafeCast) != 0) {
					scope.problemReporter().unsafeCastInInstanceof(this.expression, checkedType, expressionType);
				}
			}
		}
	} else if (checkedType.isValidBinding()) {
		// if not a valid binding, an error has already been reported for unresolved type
		if ((expressionType != TypeBinding.NULL && expressionType.isBaseType()) // disallow autoboxing
				|| checkedType.isBaseType()
				|| !checkCastTypesCompatibility(scope, checkedType, expressionType, null, true)) {
			scope.problemReporter().notCompatibleTypesError(this, expressionType, checkedType);
		}
	}
	// finally, the subtype check
	if (this.secretInstanceOfPatternExpressionValue != null &&
			expressionType.isSubtypeOf(checkedType, false)) {
		scope.problemReporter().patternCannotBeSubtypeOfExpression(this.elementVariable.binding, this);
	}
	return this.resolvedType = TypeBinding.BOOLEAN;
}
@Override
public boolean checkUnsafeCast(Scope scope, TypeBinding castType, TypeBinding expressionType, TypeBinding match, boolean isNarrowing) {
	if (!castType.isReifiable())
		return CastExpression.checkUnsafeCast(this, scope, castType, expressionType, match, isNarrowing);
	else
		return super.checkUnsafeCast(scope, castType, expressionType, match, isNarrowing);
}
/**
 * @see org.eclipse.jdt.internal.compiler.ast.Expression#tagAsUnnecessaryCast(Scope,TypeBinding)
 */

@Override
public void tagAsUnnecessaryCast(Scope scope, TypeBinding castType) {
	// null is not instanceof Type, recognize direct scenario
	if (this.expression.resolvedType != TypeBinding.NULL)
		scope.problemReporter().unnecessaryInstanceof(this, castType);
}

@Override
public void traverse(ASTVisitor visitor, BlockScope scope) {
	if (visitor.visit(this, scope)) {
		this.expression.traverse(visitor, scope);
		if (this.elementVariable != null) {
			this.elementVariable.traverse(visitor, scope);
		} else {
			this.type.traverse(visitor, scope);
		}
	}
	visitor.endVisit(this, scope);
}
}
