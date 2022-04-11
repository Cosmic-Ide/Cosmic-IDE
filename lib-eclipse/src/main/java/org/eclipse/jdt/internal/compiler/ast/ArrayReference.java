/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
 *								bug 392862 - [1.8][compiler][null] Evaluate null annotations on array types
 *								bug 383368 - [compiler][null] syntactic null analysis for field references
 *								bug 403147 - [compiler][null] FUP of bug 400761: consolidate interaction between unboxing, NPE, and deferred checking
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.ast;

import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.codegen.CodeStream;
import org.eclipse.jdt.internal.compiler.flow.FlowContext;
import org.eclipse.jdt.internal.compiler.flow.FlowInfo;
import org.eclipse.jdt.internal.compiler.impl.Constant;
import org.eclipse.jdt.internal.compiler.lookup.ArrayBinding;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.TagBits;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeIds;

public class ArrayReference extends Reference {

	public Expression receiver;
	public Expression position;

public ArrayReference(Expression rec, Expression pos) {
	this.receiver = rec;
	this.position = pos;
	this.sourceStart = rec.sourceStart;
}

@Override
public FlowInfo analyseAssignment(BlockScope currentScope, FlowContext flowContext, FlowInfo flowInfo, Assignment assignment, boolean compoundAssignment) {
	// TODO (maxime) optimization: unconditionalInits is applied to all existing calls
	// account for potential ArrayIndexOutOfBoundsException:
	flowContext.recordAbruptExit();
	if (assignment.expression == null) {
		return analyseCode(currentScope, flowContext, flowInfo);
	}
	flowInfo = assignment
		.expression
		.analyseCode(
			currentScope,
			flowContext,
			analyseCode(currentScope, flowContext, flowInfo).unconditionalInits());
	if (currentScope.environment().usesNullTypeAnnotations()) {
		checkAgainstNullTypeAnnotation(currentScope, this.resolvedType, assignment.expression, flowContext, flowInfo);
	}
	return flowInfo;
}

@Override
public FlowInfo analyseCode(BlockScope currentScope, FlowContext flowContext, FlowInfo flowInfo) {
	flowInfo = this.receiver.analyseCode(currentScope, flowContext, flowInfo);
	this.receiver.checkNPE(currentScope, flowContext, flowInfo, 1);
	flowInfo = this.position.analyseCode(currentScope, flowContext, flowInfo);
	this.position.checkNPEbyUnboxing(currentScope, flowContext, flowInfo);
	// account for potential ArrayIndexOutOfBoundsException:
	flowContext.recordAbruptExit();
	return flowInfo;
}

@Override
public boolean checkNPE(BlockScope scope, FlowContext flowContext, FlowInfo flowInfo, int ttlForFieldCheck) {
	if ((this.resolvedType.tagBits & TagBits.AnnotationNullable) != 0) {
		scope.problemReporter().arrayReferencePotentialNullReference(this);
		return true;
	} else {
		return super.checkNPE(scope, flowContext, flowInfo, ttlForFieldCheck);
	}
}

@Override
public void generateAssignment(BlockScope currentScope, CodeStream codeStream, Assignment assignment, boolean valueRequired) {
	int pc = codeStream.position;
	this.receiver.generateCode(currentScope, codeStream, true);
	if (this.receiver instanceof CastExpression	// ((type[])null)[0]
			&& ((CastExpression)this.receiver).innermostCastedExpression().resolvedType == TypeBinding.NULL){
		codeStream.checkcast(this.receiver.resolvedType);
	}
	codeStream.recordPositionsFrom(pc, this.sourceStart);
	this.position.generateCode(currentScope, codeStream, true);
	assignment.expression.generateCode(currentScope, codeStream, true);
	codeStream.arrayAtPut(this.resolvedType.id, valueRequired);
	if (valueRequired) {
		codeStream.generateImplicitConversion(assignment.implicitConversion);
	}
}

/**
 * Code generation for a array reference
 */
@Override
public void generateCode(BlockScope currentScope, CodeStream codeStream, boolean valueRequired) {
	int pc = codeStream.position;
	this.receiver.generateCode(currentScope, codeStream, true);
	if (this.receiver instanceof CastExpression	// ((type[])null)[0]
			&& ((CastExpression)this.receiver).innermostCastedExpression().resolvedType == TypeBinding.NULL){
		codeStream.checkcast(this.receiver.resolvedType);
	}
	this.position.generateCode(currentScope, codeStream, true);
	codeStream.arrayAt(this.resolvedType.id);
	// Generating code for the potential runtime type checking
	if (valueRequired) {
		codeStream.generateImplicitConversion(this.implicitConversion);
	} else {
		boolean isUnboxing = (this.implicitConversion & TypeIds.UNBOXING) != 0;
		// conversion only generated if unboxing
		if (isUnboxing) codeStream.generateImplicitConversion(this.implicitConversion);
		switch (isUnboxing ? postConversionType(currentScope).id : this.resolvedType.id) {
			case T_long :
			case T_double :
				codeStream.pop2();
				break;
			default :
				codeStream.pop();
		}
	}
	codeStream.recordPositionsFrom(pc, this.sourceStart);
}

@Override
public void generateCompoundAssignment(BlockScope currentScope, CodeStream codeStream, Expression expression, int operator, int assignmentImplicitConversion, boolean valueRequired) {
	this.receiver.generateCode(currentScope, codeStream, true);
	if (this.receiver instanceof CastExpression	// ((type[])null)[0]
			&& ((CastExpression)this.receiver).innermostCastedExpression().resolvedType == TypeBinding.NULL){
		codeStream.checkcast(this.receiver.resolvedType);
	}
	this.position.generateCode(currentScope, codeStream, true);
	codeStream.dup2();
	codeStream.arrayAt(this.resolvedType.id);
	int operationTypeID;
	switch(operationTypeID = (this.implicitConversion & TypeIds.IMPLICIT_CONVERSION_MASK) >> 4) {
		case T_JavaLangString :
		case T_JavaLangObject :
		case T_undefined :
			codeStream.generateStringConcatenationAppend(currentScope, null, expression);
			break;
		default :
			// promote the array reference to the suitable operation type
			codeStream.generateImplicitConversion(this.implicitConversion);
			// generate the increment value (will by itself  be promoted to the operation value)
			if (expression == IntLiteral.One) { // prefix operation
				codeStream.generateConstant(expression.constant, this.implicitConversion);
			} else {
				expression.generateCode(currentScope, codeStream, true);
			}
			// perform the operation
			codeStream.sendOperator(operator, operationTypeID);
			// cast the value back to the array reference type
			codeStream.generateImplicitConversion(assignmentImplicitConversion);
	}
	codeStream.arrayAtPut(this.resolvedType.id, valueRequired);
}

@Override
public void generatePostIncrement(BlockScope currentScope, CodeStream codeStream, CompoundAssignment postIncrement, boolean valueRequired) {
	this.receiver.generateCode(currentScope, codeStream, true);
	if (this.receiver instanceof CastExpression	// ((type[])null)[0]
			&& ((CastExpression)this.receiver).innermostCastedExpression().resolvedType == TypeBinding.NULL){
		codeStream.checkcast(this.receiver.resolvedType);
	}
	this.position.generateCode(currentScope, codeStream, true);
	codeStream.dup2();
	codeStream.arrayAt(this.resolvedType.id);
	if (valueRequired) {
		switch(this.resolvedType.id) {
			case TypeIds.T_long :
			case TypeIds.T_double :
				codeStream.dup2_x2();
				break;
			default :
				codeStream.dup_x2();
				break;
		}
	}
	codeStream.generateImplicitConversion(this.implicitConversion);
	codeStream.generateConstant(
		postIncrement.expression.constant,
		this.implicitConversion);
	codeStream.sendOperator(postIncrement.operator, this.implicitConversion & TypeIds.COMPILE_TYPE_MASK);
	codeStream.generateImplicitConversion(
		postIncrement.preAssignImplicitConversion);
	codeStream.arrayAtPut(this.resolvedType.id, false);
}

@Override
public StringBuffer printExpression(int indent, StringBuffer output) {
	this.receiver.printExpression(0, output).append('[');
	return this.position.printExpression(0, output).append(']');
}

@Override
public TypeBinding resolveType(BlockScope scope) {
	this.constant = Constant.NotAConstant;
	if (this.receiver instanceof CastExpression	// no cast check for ((type[])null)[0]
			&& ((CastExpression)this.receiver).innermostCastedExpression() instanceof NullLiteral) {
		this.receiver.bits |= ASTNode.DisableUnnecessaryCastCheck; // will check later on
	}
	TypeBinding arrayType = this.receiver.resolveType(scope);
	if (arrayType != null) {
		this.receiver.computeConversion(scope, arrayType, arrayType);
		if (arrayType.isArrayType()) {
			TypeBinding elementType = ((ArrayBinding) arrayType).elementsType();
			this.resolvedType = ((this.bits & ASTNode.IsStrictlyAssigned) == 0) ? elementType.capture(scope, this.sourceStart, this.sourceEnd) : elementType;
		} else {
			scope.problemReporter().referenceMustBeArrayTypeAt(arrayType, this);
		}
	}
	TypeBinding positionType = this.position.resolveTypeExpecting(scope, TypeBinding.INT);
	if (positionType != null) {
		this.position.computeConversion(scope, TypeBinding.INT, positionType);
	}
	return this.resolvedType;
}

@Override
public void traverse(ASTVisitor visitor, BlockScope scope) {
	if (visitor.visit(this, scope)) {
		this.receiver.traverse(visitor, scope);
		this.position.traverse(visitor, scope);
	}
	visitor.endVisit(this, scope);
}

@Override
public int nullStatus(FlowInfo flowInfo, FlowContext flowContext) {
	if (this.resolvedType != null && (this.resolvedType.tagBits & TagBits.AnnotationNullMASK) == 0L && this.resolvedType.isFreeTypeVariable()) {
		return FlowInfo.FREE_TYPEVARIABLE;
	}
	return super.nullStatus(flowInfo, flowContext);
}
}
