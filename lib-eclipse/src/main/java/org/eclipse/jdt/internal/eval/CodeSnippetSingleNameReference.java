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
 *								Bug 185682 - Increment/decrement operators mark local variables as read
 *								Bug 458396 - NPE in CodeStream.invoke()
 *******************************************************************************/
package org.eclipse.jdt.internal.eval;

import org.eclipse.jdt.internal.compiler.ast.Assignment;
import org.eclipse.jdt.internal.compiler.ast.BinaryExpression;
import org.eclipse.jdt.internal.compiler.ast.CompoundAssignment;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.IntLiteral;
import org.eclipse.jdt.internal.compiler.ast.SingleNameReference;
import org.eclipse.jdt.internal.compiler.codegen.CodeStream;
import org.eclipse.jdt.internal.compiler.codegen.Opcodes;
import org.eclipse.jdt.internal.compiler.flow.FlowInfo;
import org.eclipse.jdt.internal.compiler.impl.Constant;
import org.eclipse.jdt.internal.compiler.lookup.Binding;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.FieldBinding;
import org.eclipse.jdt.internal.compiler.lookup.LocalVariableBinding;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.ParameterizedFieldBinding;
import org.eclipse.jdt.internal.compiler.lookup.ProblemBinding;
import org.eclipse.jdt.internal.compiler.lookup.ProblemFieldBinding;
import org.eclipse.jdt.internal.compiler.lookup.ProblemReasons;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.Scope;
import org.eclipse.jdt.internal.compiler.lookup.TagBits;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeIds;
import org.eclipse.jdt.internal.compiler.lookup.VariableBinding;
import org.eclipse.jdt.internal.compiler.problem.AbortMethod;

/**
 * A single name reference inside a code snippet can denote a field of a remote
 * receiver object (that is, the receiver of the context in the stack frame).
 */
public class CodeSnippetSingleNameReference extends SingleNameReference implements EvaluationConstants, ProblemReasons {

	EvaluationContext evaluationContext;
	FieldBinding delegateThis;

public CodeSnippetSingleNameReference(char[] source, long pos, EvaluationContext evaluationContext) {
	super(source, pos);
	this.evaluationContext = evaluationContext;
}
/**
 * Check and/or redirect the field access to the delegate receiver if any
 */
@Override
public TypeBinding checkFieldAccess(BlockScope scope) {

	if (this.delegateThis == null) {
		return super.checkFieldAccess(scope);
	}
	FieldBinding fieldBinding = (FieldBinding) this.binding;
	this.bits &= ~RestrictiveFlagMASK; // clear bits
	this.bits |= Binding.FIELD;
	if (!fieldBinding.isStatic()) {
		// must check for the static status....
		if (this.evaluationContext.isStatic) {
			scope.problemReporter().staticFieldAccessToNonStaticVariable(
				this,
				fieldBinding);
			this.constant = Constant.NotAConstant;
			return null;
		}
	}
	this.constant = fieldBinding.constant(scope);

	if (isFieldUseDeprecated(fieldBinding, scope, this.bits)) {
		scope.problemReporter().deprecatedField(fieldBinding, this);
	}
	return fieldBinding.type;

}
@Override
public void generateAssignment(BlockScope currentScope, CodeStream codeStream, Assignment assignment, boolean valueRequired) {
	// optimizing assignment like: i = i + 1 or i = 1 + i
	if (assignment.expression.isCompactableOperation()) {
		BinaryExpression operation = (BinaryExpression) assignment.expression;
		int operator = (operation.bits & OperatorMASK) >> OperatorSHIFT;
		SingleNameReference variableReference;
		if ((operation.left instanceof SingleNameReference) && ((variableReference = (SingleNameReference) operation.left).binding == this.binding)) {
			// i = i + value, then use the variable on the right hand side, since it has the correct implicit conversion
			variableReference.generateCompoundAssignment(currentScope, codeStream, this.syntheticAccessors == null ? null : this.syntheticAccessors[WRITE], operation.right, operator, operation.implicitConversion, valueRequired);
			if (valueRequired) {
				codeStream.generateImplicitConversion(assignment.implicitConversion);
			}
			return;
		}
		if ((operation.right instanceof SingleNameReference)
				&& ((operator == PLUS) || (operator == MULTIPLY)) // only commutative operations
				&& ((variableReference = (SingleNameReference) operation.right).binding == this.binding)
				&& (operation.left.constant != Constant.NotAConstant) // exclude non constant expressions, since could have side-effect
				&& (((operation.left.implicitConversion & IMPLICIT_CONVERSION_MASK) >> 4) != T_JavaLangString) // exclude string concatenation which would occur backwards
				&& (((operation.right.implicitConversion & IMPLICIT_CONVERSION_MASK) >> 4) != T_JavaLangString)) { // exclude string concatenation which would occur backwards
			// i = value + i, then use the variable on the right hand side, since it has the correct implicit conversion
			variableReference.generateCompoundAssignment(currentScope, codeStream, this.syntheticAccessors == null ? null : this.syntheticAccessors[WRITE], operation.left, operator, operation.implicitConversion, valueRequired);
			if (valueRequired) {
				codeStream.generateImplicitConversion(assignment.implicitConversion);
			}
			return;
		}
	}
	switch (this.bits & RestrictiveFlagMASK) {
		case Binding.FIELD : // assigning to a field
			FieldBinding codegenField = ((FieldBinding) this.binding).original();
			if (codegenField.canBeSeenBy(getReceiverType(currentScope), this, currentScope)) {
				if (!codegenField.isStatic()) { // need a receiver?
					if ((this.bits & DepthMASK) != 0) {
						ReferenceBinding targetType = currentScope.enclosingSourceType().enclosingTypeAt((this.bits & DepthMASK) >> DepthSHIFT);
						Object[] emulationPath = currentScope.getEmulationPath(targetType, true /*only exact match*/, false/*consider enclosing arg*/);
						codeStream.generateOuterAccess(emulationPath, this, targetType, currentScope);
					} else {
						generateReceiver(codeStream);
					}
				}
				assignment.expression.generateCode(currentScope, codeStream, true);
				fieldStore(currentScope, codeStream, codegenField, null, this.actualReceiverType, this.delegateThis == null /*implicit this*/, valueRequired);
				if (valueRequired) {
					codeStream.generateImplicitConversion(assignment.implicitConversion);
				}
			} else {
				codeStream.generateEmulationForField(codegenField);
				if (!codegenField.isStatic()) { // need a receiver?
					if ((this.bits & DepthMASK) != 0) {
						// internal error, per construction we should have found it
						// not yet supported
						currentScope.problemReporter().needImplementation(this);
					} else {
						generateReceiver(codeStream);
					}
				} else {
					codeStream.aconst_null();
				}
				assignment.expression.generateCode(currentScope, codeStream, true);
				if (valueRequired) {
					if ((TypeBinding.equalsEquals(codegenField.type, TypeBinding.LONG)) || (TypeBinding.equalsEquals(codegenField.type, TypeBinding.DOUBLE))) {
						codeStream.dup2_x2();
					} else {
						codeStream.dup_x2();
					}
				}
				codeStream.generateEmulatedWriteAccessForField(codegenField);
				if (valueRequired) {
					codeStream.generateImplicitConversion(assignment.implicitConversion);
				}
			}
			return;
		case Binding.LOCAL : // assigning to a local variable
			LocalVariableBinding localBinding = (LocalVariableBinding) this.binding;
			if (localBinding.resolvedPosition != -1) {
				assignment.expression.generateCode(currentScope, codeStream, true);
			} else {
				if (assignment.expression.constant != Constant.NotAConstant) {
					// assigning an unused local to a constant value = no actual assignment is necessary
					if (valueRequired) {
						codeStream.generateConstant(assignment.expression.constant, assignment.implicitConversion);
					}
				} else {
					assignment.expression.generateCode(currentScope, codeStream, true);
					/* Even though the value may not be required, we force it to be produced, and discard it later
					on if it was actually not necessary, so as to provide the same behavior as JDK1.2beta3.	*/
					if (valueRequired) {
						codeStream.generateImplicitConversion(assignment.implicitConversion); // implicit conversion
					} else {
						if ((TypeBinding.equalsEquals(localBinding.type, TypeBinding.LONG)) || (TypeBinding.equalsEquals(localBinding.type, TypeBinding.DOUBLE))) {
							codeStream.pop2();
						} else {
							codeStream.pop();
						}
					}
				}
				return;
			}
			// normal local assignment (since cannot store in outer local which are final locations)
			codeStream.store(localBinding, valueRequired);
			if ((this.bits & FirstAssignmentToLocal) != 0) { // for local variable debug attributes
				localBinding.recordInitializationStartPC(codeStream.position);
			}
			// implicit conversion
			if (valueRequired) {
				codeStream.generateImplicitConversion(assignment.implicitConversion);
			}
	}
}
@Override
public void generateCode(BlockScope currentScope, CodeStream codeStream, boolean valueRequired) {
	int pc = codeStream.position;
	if (this.constant != Constant.NotAConstant) {
		if (valueRequired) {
			codeStream.generateConstant(this.constant, this.implicitConversion);
		}
	} else {
		switch (this.bits & RestrictiveFlagMASK) {
			case Binding.FIELD : // reading a field
				if (!valueRequired)
					break;
				FieldBinding codegenField = ((FieldBinding) this.binding).original();
				Constant fieldConstant = codegenField.constant();
				if (fieldConstant == Constant.NotAConstant) { // directly use inlined value for constant fields
					if (codegenField.canBeSeenBy(getReceiverType(currentScope), this, currentScope)) {
						TypeBinding someReceiverType = this.delegateThis != null ? this.delegateThis.type : this.actualReceiverType;
						TypeBinding constantPoolDeclaringClass = CodeStream.getConstantPoolDeclaringClass(currentScope, codegenField, someReceiverType, true /* implicit this */);
						if (codegenField.isStatic()) {
							codeStream.fieldAccess(Opcodes.OPC_getstatic, codegenField, constantPoolDeclaringClass);
						} else {
							if ((this.bits & DepthMASK) != 0) {
								ReferenceBinding targetType = currentScope.enclosingSourceType().enclosingTypeAt((this.bits & DepthMASK) >> DepthSHIFT);
								Object[] emulationPath = currentScope.getEmulationPath(targetType, true /*only exact match*/, false/*consider enclosing arg*/);
								codeStream.generateOuterAccess(emulationPath, this, targetType, currentScope);
							} else {
								generateReceiver(codeStream);
							}
							codeStream.fieldAccess(Opcodes.OPC_getfield, codegenField, constantPoolDeclaringClass);
						}
					} else {
						// managing private access
						if (!codegenField.isStatic()) {
							if ((this.bits & DepthMASK) != 0) {
								ReferenceBinding targetType = currentScope.enclosingSourceType().enclosingTypeAt((this.bits & DepthMASK) >> DepthSHIFT);
								Object[] emulationPath = currentScope.getEmulationPath(targetType, true /*only exact match*/, false/*consider enclosing arg*/);
								codeStream.generateOuterAccess(emulationPath, this, targetType, currentScope);
							} else {
								generateReceiver(codeStream);
							}
						} else {
							codeStream.aconst_null();
						}
						codeStream.generateEmulatedReadAccessForField(codegenField);
					}
					if (this.genericCast != null) codeStream.checkcast(this.genericCast);
					codeStream.generateImplicitConversion(this.implicitConversion);
				} else { // directly use the inlined value
					codeStream.generateConstant(fieldConstant, this.implicitConversion);
				}
				break;
			case Binding.LOCAL : // reading a local
				LocalVariableBinding localBinding = (LocalVariableBinding) this.binding;
				if (localBinding.resolvedPosition == -1) {
					if (valueRequired) {
						// restart code gen
						localBinding.useFlag = LocalVariableBinding.USED;
						throw new AbortMethod(CodeStream.RESTART_CODE_GEN_FOR_UNUSED_LOCALS_MODE, null);
					}
					codeStream.recordPositionsFrom(pc, this.sourceStart);
					return;
				}
				if (!valueRequired)
					break;
				// checkEffectiveFinality() returns if it's outer local
				if (checkEffectiveFinality(localBinding, currentScope)) {
					// outer local can be reached either through a synthetic arg or a synthetic field
					VariableBinding[] path = currentScope.getEmulationPath(localBinding);
					codeStream.generateOuterAccess(path, this, localBinding, currentScope);
				} else {
					// regular local variable read
					codeStream.load(localBinding);
				}
				codeStream.generateImplicitConversion(this.implicitConversion);
				break;
		}
	}
	codeStream.recordPositionsFrom(pc, this.sourceStart);
}
/*
 * The APIs with an extra argument is used whenever there are two references to the same variable which
 * are optimized in one access: e.g "a = a + 1" optimized into "a++".
 */
@Override
public void generateCompoundAssignment(BlockScope currentScope, CodeStream codeStream, MethodBinding writeAccessor, Expression expression, int operator, int assignmentImplicitConversion, boolean valueRequired) {
	switch (this.bits & RestrictiveFlagMASK) {
		case Binding.FIELD : // assigning to a field
			FieldBinding codegenField = ((FieldBinding) this.binding).original();
			if (codegenField.isStatic()) {
				if (codegenField.canBeSeenBy(getReceiverType(currentScope), this, currentScope)) {
					TypeBinding someReceiverType = this.delegateThis != null ? this.delegateThis.type : this.actualReceiverType;
					TypeBinding constantPoolDeclaringClass = CodeStream.getConstantPoolDeclaringClass(currentScope, codegenField, someReceiverType, true /* implicit this */);
					codeStream.fieldAccess(Opcodes.OPC_getstatic, codegenField, constantPoolDeclaringClass);
				} else {
					// used to store the value
					codeStream.generateEmulationForField(codegenField);
					codeStream.aconst_null();

					// used to retrieve the actual value
					codeStream.aconst_null();
					codeStream.generateEmulatedReadAccessForField(codegenField);
				}
			} else {
				if (codegenField.canBeSeenBy(getReceiverType(currentScope), this, currentScope)) {
					if ((this.bits & DepthMASK) != 0) {
						ReferenceBinding targetType = currentScope.enclosingSourceType().enclosingTypeAt((this.bits & DepthMASK) >> DepthSHIFT);
						Object[] emulationPath = currentScope.getEmulationPath(targetType, true /*only exact match*/, false/*consider enclosing arg*/);
						codeStream.generateOuterAccess(emulationPath, this, targetType, currentScope);
					} else {
						generateReceiver(codeStream);
					}
					codeStream.dup();
					TypeBinding someReceiverType = this.delegateThis != null ? this.delegateThis.type : this.actualReceiverType;
					TypeBinding constantPoolDeclaringClass = CodeStream.getConstantPoolDeclaringClass(currentScope, codegenField, someReceiverType, true /* implicit this */);
					codeStream.fieldAccess(Opcodes.OPC_getfield, codegenField, constantPoolDeclaringClass);
				} else {
					if ((this.bits & DepthMASK) != 0) {
						// internal error, per construction we should have found it
						// not yet supported
						currentScope.problemReporter().needImplementation(this);
					}
					// used to store the value
					codeStream.generateEmulationForField(codegenField);
					generateReceiver(codeStream);

					// used to retrieve the actual value
					codeStream.dup();
					codeStream.generateEmulatedReadAccessForField(codegenField);
				}
			}
			break;
		case Binding.LOCAL : // assigning to a local variable (cannot assign to outer local)
			LocalVariableBinding localBinding = (LocalVariableBinding) this.binding;
			// using incr bytecode if possible
			Constant assignConstant;
			switch (localBinding.type.id) {
				case T_JavaLangString :
					codeStream.generateStringConcatenationAppend(currentScope, this, expression);
					if (valueRequired) {
						codeStream.dup();
					}
					codeStream.store(localBinding, false);
					return;
				case T_int :
					assignConstant = expression.constant;
					if (localBinding.resolvedPosition == -1) {
						if (valueRequired) {
							/*
							 * restart code gen because we either:
							 * - need the value
							 * - the constant can have potential side-effect
							 */
							localBinding.useFlag = LocalVariableBinding.USED;
							throw new AbortMethod(CodeStream.RESTART_CODE_GEN_FOR_UNUSED_LOCALS_MODE, null);
						} else if (assignConstant == Constant.NotAConstant) {
							// we only need to generate the value of the expression's constant if it is not a constant expression
							expression.generateCode(currentScope, codeStream, false);
						}
						return;
					}
					if ((assignConstant != Constant.NotAConstant)
							&& (assignConstant.typeID() != TypeIds.T_float) // only for integral types
							&& (assignConstant.typeID() != TypeIds.T_double)) { // TODO (philippe) is this test needed ?
						switch (operator) {
							case PLUS :
								int increment  = assignConstant.intValue();
								if (increment != (short) increment) break; // not representable as a 16-bits value
								codeStream.iinc(localBinding.resolvedPosition, increment);
								if (valueRequired) {
									codeStream.load(localBinding);
								}
								return;
							case MINUS :
								increment  = -assignConstant.intValue();
								if (increment != (short) increment) break; // not representable as a 16-bits value
								codeStream.iinc(localBinding.resolvedPosition, increment);
								if (valueRequired) {
									codeStream.load(localBinding);
								}
								return;
						}
					}
					//$FALL-THROUGH$
				default :
					if (localBinding.resolvedPosition == -1) {
						assignConstant = expression.constant;
						if (valueRequired) {
							/*
							 * restart code gen because we either:
							 * - need the value
							 * - the constant can have potential side-effect
							 */
							localBinding.useFlag = LocalVariableBinding.USED;
							throw new AbortMethod(CodeStream.RESTART_CODE_GEN_FOR_UNUSED_LOCALS_MODE, null);
						} else if (assignConstant == Constant.NotAConstant) {
							// we only need to generate the value of the expression's constant if it is not a constant expression
							expression.generateCode(currentScope, codeStream, false);
						}
						return;
					}
					codeStream.load(localBinding);
			}
	}
	// perform the actual compound operation
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
	// store the result back into the variable
	switch (this.bits & RestrictiveFlagMASK) {
		case Binding.FIELD : // assigning to a field
			FieldBinding codegenField = ((FieldBinding) this.binding).original();
			if (codegenField.canBeSeenBy(getReceiverType(currentScope), this, currentScope)) {
				fieldStore(currentScope, codeStream, codegenField, writeAccessor, this.actualReceiverType, this.delegateThis == null /* implicit this */, valueRequired);
			} else {
				// current stack is:
				// field receiver value
				if (valueRequired) {
					switch (codegenField.type.id) {
						case TypeIds.T_long :
						case TypeIds.T_double :
							codeStream.dup2_x2();
							break;
						default:
							codeStream.dup_x2();
							break;
					}
				}
				// current stack is:
				// value field receiver value
				codeStream.generateEmulatedWriteAccessForField(codegenField);
			}
			return;
		case Binding.LOCAL : // assigning to a local variable
			LocalVariableBinding localBinding = (LocalVariableBinding) this.binding;
			if (valueRequired) {
				switch (localBinding.type.id) {
					case TypeIds.T_long :
					case TypeIds.T_double :
						codeStream.dup2();
						break;
					default:
						codeStream.dup();
						break;
				}
			}
			codeStream.store(localBinding, false);
	}
}
@Override
public void generatePostIncrement(BlockScope currentScope, CodeStream codeStream, CompoundAssignment postIncrement, boolean valueRequired) {
	switch (this.bits & RestrictiveFlagMASK) {
		case Binding.FIELD : // assigning to a field
			FieldBinding codegenField = ((FieldBinding) this.binding).original();
			if (codegenField.canBeSeenBy(getReceiverType(currentScope), this, currentScope)) {
				super.generatePostIncrement(currentScope, codeStream, postIncrement, valueRequired);
			} else {
				if (codegenField.isStatic()) {
					codeStream.aconst_null();
				} else {
					if ((this.bits & DepthMASK) != 0) {
						// internal error, per construction we should have found it
						// not yet supported
						currentScope.problemReporter().needImplementation(this);
					} else {
						generateReceiver(codeStream);
					}
				}
				codeStream.generateEmulatedReadAccessForField(codegenField);
				if (valueRequired) {
					switch (codegenField.type.id) {
						case TypeIds.T_long :
						case TypeIds.T_double :
							codeStream.dup2();
							break;
						default:
							codeStream.dup();
							break;
					}
				}
				codeStream.generateEmulationForField(codegenField);
				switch (codegenField.type.id) {
					case TypeIds.T_long :
					case TypeIds.T_double :
						codeStream.dup_x2();
						codeStream.pop();
						if (codegenField.isStatic()) {
							codeStream.aconst_null();
						} else {
							generateReceiver(codeStream);
						}
						codeStream.dup_x2();
						codeStream.pop();
						break;
					default:
						codeStream.dup_x1();
					codeStream.pop();
					if (codegenField.isStatic()) {
						codeStream.aconst_null();
					} else {
						generateReceiver(codeStream);
					}
					codeStream.dup_x1();
					codeStream.pop();
						break;
				}
				codeStream.generateConstant(postIncrement.expression.constant, this.implicitConversion);
				codeStream.sendOperator(postIncrement.operator, codegenField.type.id);
				codeStream.generateImplicitConversion(postIncrement.preAssignImplicitConversion);
				codeStream.generateEmulatedWriteAccessForField(codegenField);
			}
			return;
		case Binding.LOCAL : // assigning to a local variable
			super.generatePostIncrement(currentScope, codeStream, postIncrement, valueRequired);
	}
}
@Override
public void generateReceiver(CodeStream codeStream) {
	codeStream.aload_0();
	if (this.delegateThis != null) {
		codeStream.fieldAccess(Opcodes.OPC_getfield, this.delegateThis, null /* default declaringClass */); // delegate field access
	}
}
/**
 * Check and/or redirect the field access to the delegate receiver if any
 */
public TypeBinding getReceiverType(BlockScope currentScope) {
	Scope scope = currentScope.parent;
	while (true) {
			switch (scope.kind) {
				case Scope.CLASS_SCOPE :
					return ((ClassScope) scope).referenceContext.binding;
				default:
					scope = scope.parent;
			}
	}
}
@Override
public void manageSyntheticAccessIfNecessary(BlockScope currentScope, FlowInfo flowInfo, boolean isReadAccess) {

	if (this.delegateThis == null) {
		super.manageSyntheticAccessIfNecessary(currentScope, flowInfo, isReadAccess);
		return;
	}

	if ((flowInfo.tagBits & FlowInfo.UNREACHABLE_OR_DEAD) != 0) return;
	//If inlinable field, forget the access emulation, the code gen will directly target it
	if (this.constant != Constant.NotAConstant)
		return;
	// if field from parameterized type got found, use the original field at codegen time
	if (this.binding instanceof ParameterizedFieldBinding) {
	    ParameterizedFieldBinding parameterizedField = (ParameterizedFieldBinding) this.binding;
	    FieldBinding codegenField = parameterizedField.originalField;
	    // extra cast needed if field type was type variable
	    if ((codegenField.type.tagBits & TagBits.HasTypeVariable) != 0) {
	        this.genericCast = codegenField.type.genericCast(currentScope.boxing(parameterizedField.type)); // runtimeType could be base type in boxing case
	    }
	}
}
/**
 * Normal field binding did not work, try to bind to a field of the delegate receiver.
 */
@Override
public TypeBinding reportError(BlockScope scope) {

	this.constant = Constant.NotAConstant;
	if (this.binding instanceof ProblemFieldBinding && ((ProblemFieldBinding) this.binding).problemId() == NotFound){
		if (this.evaluationContext.declaringTypeName != null) {
			this.delegateThis = scope.getField(scope.enclosingSourceType(), DELEGATE_THIS, this);
			if (this.delegateThis != null){  // if not found then internal error, field should have been found
				this.actualReceiverType = this.delegateThis.type;
				// will not support innerclass emulation inside delegate
				this.binding = scope.getField(this.delegateThis.type, this.token, this);
				if (!this.binding.isValidBinding()) {
					return super.reportError(scope);
				}
				return checkFieldAccess(scope);
			}
		}
	}
	if (this.binding instanceof ProblemBinding && ((ProblemBinding) this.binding).problemId() == NotFound){
		if (this.evaluationContext.declaringTypeName != null) {
			this.delegateThis = scope.getField(scope.enclosingSourceType(), DELEGATE_THIS, this);
			if (this.delegateThis != null){  // if not found then internal error, field should have been found
				this.actualReceiverType = this.delegateThis.type;
				// will not support innerclass emulation inside delegate
				FieldBinding fieldBinding = scope.getField(this.delegateThis.type, this.token, this);
				if (!fieldBinding.isValidBinding()) {
					if (((ProblemFieldBinding) fieldBinding).problemId() == NotVisible) {
						// manage the access to a private field of the enclosing type
						CodeSnippetScope localScope = new CodeSnippetScope(scope);
						this.binding = localScope.getFieldForCodeSnippet(this.delegateThis.type, this.token, this);
						return checkFieldAccess(scope);
					} else {
						return super.reportError(scope);
					}
				}
				this.binding = fieldBinding;
				return checkFieldAccess(scope);
			}
		}
	}
	return super.reportError(scope);
}
}
