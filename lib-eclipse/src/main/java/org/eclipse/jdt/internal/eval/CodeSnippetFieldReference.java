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
 *     Stephan Herrmann - Contributions for
 *								Bug 185682 - Increment/decrement operators mark local variables as read
 *								Bug 458396 - NPE in CodeStream.invoke()
 *******************************************************************************/
package org.eclipse.jdt.internal.eval;

import org.eclipse.jdt.internal.compiler.ast.Assignment;
import org.eclipse.jdt.internal.compiler.ast.CompoundAssignment;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.FieldReference;
import org.eclipse.jdt.internal.compiler.ast.IntLiteral;
import org.eclipse.jdt.internal.compiler.codegen.CodeStream;
import org.eclipse.jdt.internal.compiler.codegen.Opcodes;
import org.eclipse.jdt.internal.compiler.flow.FlowInfo;
import org.eclipse.jdt.internal.compiler.impl.Constant;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.FieldBinding;
import org.eclipse.jdt.internal.compiler.lookup.ProblemFieldBinding;
import org.eclipse.jdt.internal.compiler.lookup.ProblemReasons;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeIds;

public class CodeSnippetFieldReference extends FieldReference implements ProblemReasons, EvaluationConstants {

	EvaluationContext evaluationContext;
	FieldBinding delegateThis;
/**
 * CodeSnippetFieldReference constructor comment.
 * @param source char[]
 * @param pos long
 */
public CodeSnippetFieldReference(char[] source, long pos, EvaluationContext evaluationContext) {
	super(source, pos);
	this.evaluationContext = evaluationContext;
}
@Override
public void generateAssignment(BlockScope currentScope, CodeStream codeStream, Assignment assignment, boolean valueRequired) {
	FieldBinding codegenBinding = this.binding.original();
	if (codegenBinding.canBeSeenBy(this.actualReceiverType, this, currentScope)) {
		this.receiver.generateCode(currentScope, codeStream, !codegenBinding.isStatic());
		assignment.expression.generateCode(currentScope, codeStream, true);
		fieldStore(currentScope, codeStream, codegenBinding, null, this.actualReceiverType, this.receiver.isImplicitThis(), valueRequired);
	} else {
		codeStream.generateEmulationForField(codegenBinding);
		this.receiver.generateCode(currentScope, codeStream, !codegenBinding.isStatic());
		if (codegenBinding.isStatic()) { // need a receiver?
			codeStream.aconst_null();
		}
		assignment.expression.generateCode(currentScope, codeStream, true);
		if (valueRequired) {
			switch (codegenBinding.type.id) {
				case TypeIds.T_long :
				case TypeIds.T_double :
					codeStream.dup2_x2();
					break;
				default :
					codeStream.dup_x2();
					break;
			}
		}
		codeStream.generateEmulatedWriteAccessForField(codegenBinding);
	}
	if (valueRequired){
		codeStream.generateImplicitConversion(assignment.implicitConversion);
	}
}
/**
 * Field reference code generation
 *
 * @param currentScope org.eclipse.jdt.internal.compiler.lookup.BlockScope
 * @param codeStream org.eclipse.jdt.internal.compiler.codegen.CodeStream
 * @param valueRequired boolean
 */
@Override
public void generateCode(BlockScope currentScope, CodeStream codeStream, boolean valueRequired) {
	int pc = codeStream.position;
	if (this.constant != Constant.NotAConstant) {
		if (valueRequired) {
			codeStream.generateConstant(this.constant, this.implicitConversion);
		}
	} else {
		FieldBinding codegenBinding = this.binding.original();
		boolean isStatic = codegenBinding.isStatic();
		this.receiver.generateCode(currentScope, codeStream, !isStatic);
		if (valueRequired) {
			Constant fieldConstant = codegenBinding.constant();
			if (fieldConstant == Constant.NotAConstant) {
				if (codegenBinding.declaringClass == null) { // array length
					codeStream.arraylength();
				} else {
					if (codegenBinding.canBeSeenBy(this.actualReceiverType, this, currentScope)) {
						TypeBinding constantPoolDeclaringClass = CodeStream.getConstantPoolDeclaringClass(currentScope, codegenBinding, this.actualReceiverType, this.receiver.isImplicitThis());
						if (isStatic) {
							codeStream.fieldAccess(Opcodes.OPC_getstatic , codegenBinding, constantPoolDeclaringClass);
						} else {
							codeStream.fieldAccess(Opcodes.OPC_getfield, codegenBinding, constantPoolDeclaringClass);
						}
					} else {
						if (isStatic) {
							// we need a null on the stack to use the reflect emulation
							codeStream.aconst_null();
						}
						codeStream.generateEmulatedReadAccessForField(codegenBinding);
					}
				}
				codeStream.generateImplicitConversion(this.implicitConversion);
			} else {
				if (!isStatic) {
					codeStream.invokeObjectGetClass(); // perform null check
					codeStream.pop();
				}
				codeStream.generateConstant(fieldConstant, this.implicitConversion);
			}
		} else {
			if (!isStatic){
				codeStream.invokeObjectGetClass(); // perform null check
				codeStream.pop();
			}
		}
	}
	codeStream.recordPositionsFrom(pc, this.sourceStart);
}

@Override
public void generateCompoundAssignment(BlockScope currentScope, CodeStream codeStream, Expression expression, int operator, int assignmentImplicitConversion, boolean valueRequired) {
	boolean isStatic;
	FieldBinding codegenBinding = this.binding.original();
	if (codegenBinding.canBeSeenBy(this.actualReceiverType, this, currentScope)) {
		this.receiver.generateCode(currentScope, codeStream, !(isStatic = codegenBinding.isStatic()));
		TypeBinding constantPoolDeclaringClass = CodeStream.getConstantPoolDeclaringClass(currentScope, codegenBinding, this.actualReceiverType, this.receiver.isImplicitThis());
		if (isStatic) {
			codeStream.fieldAccess(Opcodes.OPC_getstatic, codegenBinding, constantPoolDeclaringClass);
		} else {
			codeStream.dup();
			codeStream.fieldAccess(Opcodes.OPC_getfield, codegenBinding, constantPoolDeclaringClass);
		}
		int operationTypeID;
		switch(operationTypeID = (this.implicitConversion & IMPLICIT_CONVERSION_MASK) >> 4) {
			case T_JavaLangString :
			case T_JavaLangObject :
			case T_undefined :
				codeStream.generateStringConcatenationAppend(currentScope, null, expression);
				break;
			default :
				// promote the array reference to the suitable operation type
				codeStream.generateImplicitConversion(this.implicitConversion);
				// generate the increment value (will by itself  be promoted to the operation value)
				if (expression == IntLiteral.One){ // prefix operation
					codeStream.generateConstant(expression.constant, this.implicitConversion);
				} else {
					expression.generateCode(currentScope, codeStream, true);
				}
				// perform the operation
				codeStream.sendOperator(operator, operationTypeID);
				// cast the value back to the array reference type
				codeStream.generateImplicitConversion(assignmentImplicitConversion);
		}
		fieldStore(currentScope, codeStream, codegenBinding, null, this.actualReceiverType, this.receiver.isImplicitThis(), valueRequired);
	} else {
		this.receiver.generateCode(currentScope, codeStream, !(isStatic = codegenBinding.isStatic()));
		if (isStatic) {
			// used to store the value
			codeStream.generateEmulationForField(codegenBinding);
			codeStream.aconst_null();

			// used to retrieve the actual value
			codeStream.aconst_null();
			codeStream.generateEmulatedReadAccessForField(codegenBinding);
		} else {
			// used to store the value
			codeStream.generateEmulationForField(this.binding);
			this.receiver.generateCode(currentScope, codeStream, !isStatic);

			// used to retrieve the actual value
			codeStream.dup();
			codeStream.generateEmulatedReadAccessForField(codegenBinding);
		}
		int operationTypeID;
		if ((operationTypeID = (this.implicitConversion & IMPLICIT_CONVERSION_MASK) >> 4) == T_JavaLangString) {
			codeStream.generateStringConcatenationAppend(currentScope, null, expression);
		} else {
			// promote the array reference to the suitable operation type
			codeStream.generateImplicitConversion(this.implicitConversion);
			// generate the increment value (will by itself  be promoted to the operation value)
			if (expression == IntLiteral.One){ // prefix operation
				codeStream.generateConstant(expression.constant, this.implicitConversion);
			} else {
				expression.generateCode(currentScope, codeStream, true);
			}
			// perform the operation
			codeStream.sendOperator(operator, operationTypeID);
			// cast the value back to the array reference type
			codeStream.generateImplicitConversion(assignmentImplicitConversion);
		}
		// current stack is:
		// field receiver value
		if (valueRequired) {
			if ((TypeBinding.equalsEquals(codegenBinding.type, TypeBinding.LONG)) || (TypeBinding.equalsEquals(codegenBinding.type, TypeBinding.DOUBLE))) {
				codeStream.dup2_x2();
			} else {
				codeStream.dup_x2();
			}
		}
		// current stack is:
		// value field receiver value
		codeStream.generateEmulatedWriteAccessForField(codegenBinding);
	}
}
@Override
public void generatePostIncrement(BlockScope currentScope, CodeStream codeStream, CompoundAssignment postIncrement, boolean valueRequired) {
	boolean isStatic;
	FieldBinding codegenBinding = this.binding.original();
	if (codegenBinding.canBeSeenBy(this.actualReceiverType, this, currentScope)) {
		super.generatePostIncrement(currentScope, codeStream, postIncrement, valueRequired);
	} else {
		this.receiver.generateCode(currentScope, codeStream, !(isStatic = codegenBinding.isStatic()));
		if (isStatic) {
			codeStream.aconst_null();
		}
		// the actual stack is: receiver
		codeStream.dup();
		// the actual stack is: receiver receiver
		codeStream.generateEmulatedReadAccessForField(codegenBinding);
		// the actual stack is: receiver value
		// receiver value
		// value receiver value 							dup_x1 or dup2_x1 if value required
		// value value receiver value					dup_x1 or dup2_x1
		// value value receiver							pop or pop2
		// value value receiver field						generateEmulationForField
		// value value field receiver 					swap
		// value field receiver value field receiver 	dup2_x1 or dup2_x2
		// value field receiver value 				 	pop2
		// value field receiver newvalue 				generate constant + op
		// value 												store
		int typeID;
		switch (typeID = codegenBinding.type.id) {
			case TypeIds.T_long :
			case TypeIds.T_double :
				if (valueRequired) {
					codeStream.dup2_x1();
				}
				codeStream.dup2_x1();
				codeStream.pop2();
				break;
			default :
				if (valueRequired) {
					codeStream.dup_x1();
				}
				codeStream.dup_x1();
				codeStream.pop();
				break;
		}
		codeStream.generateEmulationForField(codegenBinding);
		codeStream.swap();
		switch (typeID) {
			case TypeIds.T_long :
			case TypeIds.T_double :
				codeStream.dup2_x2();
				break;
			default :
				codeStream.dup2_x1();
				break;
		}
		codeStream.pop2();

		codeStream.generateConstant(postIncrement.expression.constant, this.implicitConversion);
		codeStream.sendOperator(postIncrement.operator, codegenBinding.type.id);
		codeStream.generateImplicitConversion(postIncrement.preAssignImplicitConversion);
		codeStream.generateEmulatedWriteAccessForField(codegenBinding);
	}
}
/*
 * No need to emulate access to protected fields since not implicitly accessed
 */
@Override
public void manageSyntheticAccessIfNecessary(BlockScope currentScope, FlowInfo flowInfo, boolean isReadAccess){
	// The private access will be managed through the code generation

	if ((flowInfo.tagBits & FlowInfo.UNREACHABLE_OR_DEAD) != 0) return;
}
@Override
public TypeBinding resolveType(BlockScope scope) {
	// Answer the signature type of the field.
	// constants are propaged when the field is final
	// and initialized with a (compile time) constant

	// regular receiver reference
	this.actualReceiverType = this.receiver.resolveType(scope);
	if (this.actualReceiverType == null){
		this.constant = Constant.NotAConstant;
		return null;
	}
	// the case receiverType.isArrayType and token = 'length' is handled by the scope API
	this.binding = scope.getField(this.actualReceiverType, this.token, this);
	FieldBinding firstAttempt = this.binding;
	boolean isNotVisible = false;
	if (!this.binding.isValidBinding()) {
		if (this.binding instanceof ProblemFieldBinding
			&& ((ProblemFieldBinding) this.binding).problemId() == NotVisible) {
				isNotVisible = true;
				if (this.evaluationContext.declaringTypeName != null) {
					this.delegateThis = scope.getField(scope.enclosingSourceType(), DELEGATE_THIS, this);
					if (this.delegateThis == null){  // if not found then internal error, field should have been found
						this.constant = Constant.NotAConstant;
						scope.problemReporter().invalidField(this, this.actualReceiverType);
						return null;
					}
					this.actualReceiverType = this.delegateThis.type;
				} else {
					this.constant = Constant.NotAConstant;
					scope.problemReporter().invalidField(this, this.actualReceiverType);
					return null;
				}
			CodeSnippetScope localScope = new CodeSnippetScope(scope);
			this.binding = localScope.getFieldForCodeSnippet(this.delegateThis.type, this.token, this);
		}
	}

	if (!this.binding.isValidBinding()) {
		this.constant = Constant.NotAConstant;
		if (isNotVisible) {
			this.binding = firstAttempt;
		}
		scope.problemReporter().invalidField(this, this.actualReceiverType);
		return null;
	}

	if (isFieldUseDeprecated(this.binding, scope, this.bits)) {
		scope.problemReporter().deprecatedField(this.binding, this);
	}
	// check for this.x in static is done in the resolution of the receiver
	this.constant = this.receiver.isImplicitThis() ? this.binding.constant(scope) : Constant.NotAConstant;
	if (!this.receiver.isThis()) { // TODO need to check if shouldn't be isImplicitThis check (and then removed)
		this.constant = Constant.NotAConstant;
	}
	return this.resolvedType = this.binding.type;
}
}
