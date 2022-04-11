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

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.Assignment;
import org.eclipse.jdt.internal.compiler.ast.CompoundAssignment;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.IntLiteral;
import org.eclipse.jdt.internal.compiler.ast.QualifiedNameReference;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.codegen.CodeStream;
import org.eclipse.jdt.internal.compiler.codegen.Opcodes;
import org.eclipse.jdt.internal.compiler.flow.FlowInfo;
import org.eclipse.jdt.internal.compiler.impl.Constant;
import org.eclipse.jdt.internal.compiler.lookup.Binding;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.FieldBinding;
import org.eclipse.jdt.internal.compiler.lookup.LocalVariableBinding;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.MethodScope;
import org.eclipse.jdt.internal.compiler.lookup.ProblemBinding;
import org.eclipse.jdt.internal.compiler.lookup.ProblemFieldBinding;
import org.eclipse.jdt.internal.compiler.lookup.ProblemReasons;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeIds;
import org.eclipse.jdt.internal.compiler.lookup.VariableBinding;

public class CodeSnippetQualifiedNameReference extends QualifiedNameReference implements EvaluationConstants, ProblemReasons {

	EvaluationContext evaluationContext;
	FieldBinding delegateThis;
/**
 * CodeSnippetQualifiedNameReference constructor comment.
 * @param sources char[][]
 * @param sourceStart int
 * @param sourceEnd int
 */
public CodeSnippetQualifiedNameReference(char[][] sources, long[] positions, int sourceStart, int sourceEnd, EvaluationContext evaluationContext) {
	super(sources, positions, sourceStart, sourceEnd);
	this.evaluationContext = evaluationContext;
}

/**
 * Check and/or redirect the field access to the delegate receiver if any
 */
public TypeBinding checkFieldAccess(BlockScope scope) {
	FieldBinding fieldBinding = (FieldBinding) this.binding;
	MethodScope methodScope = scope.methodScope();
	TypeBinding declaringClass = fieldBinding.original().declaringClass;
	// check for forward references
	if ((this.indexOfFirstFieldBinding == 1 || declaringClass.isEnum())
			&& TypeBinding.equalsEquals(methodScope.enclosingSourceType(), declaringClass)
			&& methodScope.lastVisibleFieldID >= 0
			&& fieldBinding.id >= methodScope.lastVisibleFieldID
			&& (!fieldBinding.isStatic() || methodScope.isStatic)) {
		scope.problemReporter().forwardReference(this, this.indexOfFirstFieldBinding-1, fieldBinding);
	}
	this.bits &= ~ASTNode.RestrictiveFlagMASK; // clear bits
	this.bits |= Binding.FIELD;
	return getOtherFieldBindings(scope);
}

@Override
public void generateCode(BlockScope currentScope, CodeStream codeStream, boolean valueRequired) {
	int pc = codeStream.position;
	if ((this.bits & Binding.VARIABLE) == 0) { // nothing to do if type ref
		codeStream.recordPositionsFrom(pc, this.sourceStart);
		return;
	}
	FieldBinding lastFieldBinding = this.otherBindings == null ? (FieldBinding) this.binding : this.otherBindings[this.otherBindings.length-1];
	if (lastFieldBinding.canBeSeenBy(getFinalReceiverType(), this, currentScope)) {
		super.generateCode(currentScope, codeStream, valueRequired);
		return;
	}
	lastFieldBinding = generateReadSequence(currentScope, codeStream);
	if (lastFieldBinding != null) {
		boolean isStatic = lastFieldBinding.isStatic();
		Constant fieldConstant = lastFieldBinding.constant();
		if (fieldConstant != Constant.NotAConstant) {
			if (!isStatic){
				codeStream.invokeObjectGetClass();
				codeStream.pop();
			}
			if (valueRequired) { // inline the last field constant
				codeStream.generateConstant(fieldConstant, this.implicitConversion);
			}
		} else {
			boolean isFirst = lastFieldBinding == this.binding
											&& (this.indexOfFirstFieldBinding == 1 || TypeBinding.equalsEquals(lastFieldBinding.declaringClass, currentScope.enclosingReceiverType()))
											&& this.otherBindings == null; // could be dup: next.next.next
			TypeBinding requiredGenericCast = getGenericCast(this.otherBindings == null ? 0 : this.otherBindings.length);
			if (valueRequired
					|| (!isFirst && currentScope.compilerOptions().complianceLevel >= ClassFileConstants.JDK1_4)
					|| ((this.implicitConversion & TypeIds.UNBOXING) != 0)
					|| requiredGenericCast != null) {
				int lastFieldPc = codeStream.position;
				if (lastFieldBinding.declaringClass == null) { // array length
					codeStream.arraylength();
					if (valueRequired) {
						codeStream.generateImplicitConversion(this.implicitConversion);
					} else {
						// could occur if !valueRequired but compliance >= 1.4
						codeStream.pop();
					}
				} else {
					codeStream.generateEmulatedReadAccessForField(lastFieldBinding);
					if (requiredGenericCast != null) codeStream.checkcast(requiredGenericCast);
					if (valueRequired) {
						codeStream.generateImplicitConversion(this.implicitConversion);
					} else {
						boolean isUnboxing = (this.implicitConversion & TypeIds.UNBOXING) != 0;
						// conversion only generated if unboxing
						if (isUnboxing) codeStream.generateImplicitConversion(this.implicitConversion);
						switch (isUnboxing ? postConversionType(currentScope).id : lastFieldBinding.type.id) {
							case T_long :
							case T_double :
								codeStream.pop2();
								break;
							default :
								codeStream.pop();
						}
					}
				}

				int fieldPosition = (int) (this.sourcePositions[this.sourcePositions.length - 1] >>> 32);
				codeStream.recordPositionsFrom(lastFieldPc, fieldPosition);
			} else {
				if (!isStatic){
					codeStream.invokeObjectGetClass(); // perform null check
					codeStream.pop();
				}
			}
		}
	}
	codeStream.recordPositionsFrom(pc, this.sourceStart);
}
/**
 * Check and/or redirect the field access to the delegate receiver if any
 */
@Override
public void generateAssignment(BlockScope currentScope, CodeStream codeStream, Assignment assignment, boolean valueRequired) {
    FieldBinding lastFieldBinding = this.otherBindings == null ? (FieldBinding) this.binding : this.otherBindings[this.otherBindings.length-1];
	if (lastFieldBinding.canBeSeenBy(getFinalReceiverType(), this, currentScope)) {
		super.generateAssignment(currentScope, codeStream, assignment, valueRequired);
		return;
	}
	lastFieldBinding = generateReadSequence(currentScope, codeStream);
	codeStream.generateEmulationForField(lastFieldBinding);
	codeStream.swap();
	assignment.expression.generateCode(currentScope, codeStream, true);
	if (valueRequired) {
		switch (lastFieldBinding.type.id) {
			case TypeIds.T_long :
			case TypeIds.T_double :
				codeStream.dup2_x2();
				break;
			default :
				codeStream.dup_x2();
			break;
		}
	}
	codeStream.generateEmulatedWriteAccessForField(lastFieldBinding);
	if (valueRequired) {
		codeStream.generateImplicitConversion(assignment.implicitConversion);
	}
}

@Override
public void generateCompoundAssignment(BlockScope currentScope, CodeStream codeStream, Expression expression, int operator, int assignmentImplicitConversion, boolean valueRequired) {
    FieldBinding lastFieldBinding = this.otherBindings == null ? (FieldBinding) this.binding : this.otherBindings[this.otherBindings.length-1];
	if (lastFieldBinding.canBeSeenBy(getFinalReceiverType(), this, currentScope)) {
		super.generateCompoundAssignment(currentScope, codeStream, expression, operator, assignmentImplicitConversion, valueRequired);
		return;
	}
	lastFieldBinding = generateReadSequence(currentScope, codeStream);
	if (lastFieldBinding.isStatic()){
		codeStream.generateEmulationForField(lastFieldBinding);
		codeStream.swap();
		codeStream.aconst_null();
		codeStream.swap();
		codeStream.generateEmulatedReadAccessForField(lastFieldBinding);
	} else {
		codeStream.generateEmulationForField(lastFieldBinding);
		codeStream.swap();
		codeStream.dup();

		codeStream.generateEmulatedReadAccessForField(lastFieldBinding);
	}
	// the last field access is a write access
	// perform the actual compound operation
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
	// actual assignment

	// current stack is:
	// field receiver value
	if (valueRequired) {
		switch (lastFieldBinding.type.id) {
			case TypeIds.T_long :
			case TypeIds.T_double :
				codeStream.dup2_x2();
				break;
			default :
				codeStream.dup_x2();
			break;
		}
	}
	// current stack is:
	// value field receiver value
	codeStream.generateEmulatedWriteAccessForField(lastFieldBinding);
}
@Override
public void generatePostIncrement(BlockScope currentScope, CodeStream codeStream, CompoundAssignment postIncrement, boolean valueRequired) {
    FieldBinding lastFieldBinding = this.otherBindings == null ? (FieldBinding) this.binding : this.otherBindings[this.otherBindings.length-1];
	if (lastFieldBinding.canBeSeenBy(getFinalReceiverType(), this, currentScope)) {
		super.generatePostIncrement(currentScope, codeStream, postIncrement, valueRequired);
		return;
	}
	lastFieldBinding = generateReadSequence(currentScope, codeStream);
	codeStream.generateEmulatedReadAccessForField(lastFieldBinding);
	if (valueRequired) {
		switch (lastFieldBinding.type.id) {
			case TypeIds.T_long :
			case TypeIds.T_double :
				codeStream.dup2();
				break;
			default :
				codeStream.dup();
			break;
		}
	}
	codeStream.generateEmulationForField(lastFieldBinding);
	if ((TypeBinding.equalsEquals(lastFieldBinding.type, TypeBinding.LONG)) || (TypeBinding.equalsEquals(lastFieldBinding.type, TypeBinding.DOUBLE))) {
		codeStream.dup_x2();
		codeStream.pop();
		if (lastFieldBinding.isStatic()) {
			codeStream.aconst_null();
		} else {
			generateReadSequence(currentScope, codeStream);
		}
		codeStream.dup_x2();
		codeStream.pop();
	} else {
		codeStream.dup_x1();
		codeStream.pop();
		if (lastFieldBinding.isStatic()) {
			codeStream.aconst_null();
		} else {
			generateReadSequence(currentScope, codeStream);
		}
		codeStream.dup_x1();
		codeStream.pop();
	}
	codeStream.generateConstant(postIncrement.expression.constant, this.implicitConversion);
	codeStream.sendOperator(postIncrement.operator, lastFieldBinding.type.id);
	codeStream.generateImplicitConversion(postIncrement.preAssignImplicitConversion);
	codeStream.generateEmulatedWriteAccessForField(lastFieldBinding);
}

/*
 * Generate code for all bindings (local and fields) excluding the last one, which may then be generated code
 * for a read or write access.
 */
@Override
public FieldBinding generateReadSequence(BlockScope currentScope, CodeStream codeStream) {
	// determine the rank until which we now we do not need any actual value for the field access
	int otherBindingsCount = this.otherBindings == null ? 0 : this.otherBindings.length;
	boolean needValue = otherBindingsCount == 0 || !this.otherBindings[0].isStatic();
	FieldBinding lastFieldBinding;
	TypeBinding lastGenericCast;
	TypeBinding lastReceiverType;
	boolean complyTo14 = currentScope.compilerOptions().complianceLevel >= ClassFileConstants.JDK1_4;

	switch (this.bits & RestrictiveFlagMASK) {
		case Binding.FIELD :
			lastFieldBinding = ((FieldBinding) this.binding).original();
			lastGenericCast = this.genericCast;
			lastReceiverType = this.actualReceiverType;
			// if first field is actually constant, we can inline it
			if (lastFieldBinding.constant() != Constant.NotAConstant) {
				break;
			}
			if (needValue) {
				if (lastFieldBinding.canBeSeenBy(this.actualReceiverType, this, currentScope)) {
					if (!lastFieldBinding.isStatic()) {
						if ((this.bits & DepthMASK) != 0) {
							ReferenceBinding targetType = currentScope.enclosingSourceType().enclosingTypeAt((this.bits & DepthMASK) >> DepthSHIFT);
							Object[] emulationPath = currentScope.getEmulationPath(targetType, true /*only exact match*/, false/*consider enclosing arg*/);
							codeStream.generateOuterAccess(emulationPath, this, targetType, currentScope);
						} else {
							generateReceiver(codeStream);
						}
					}
				} else {
					if (!lastFieldBinding.isStatic()) {
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
				}
			}
			break;
		case Binding.LOCAL : // reading the first local variable
			lastFieldBinding = null;
			lastGenericCast = null;
			LocalVariableBinding localBinding = (LocalVariableBinding) this.binding;
			lastReceiverType = localBinding.type;
			if (!needValue) break; // no value needed
			// regular local variable read
			Constant localConstant = localBinding.constant();
			if (localConstant != Constant.NotAConstant) {
				codeStream.generateConstant(localConstant, 0);
				// no implicit conversion
			} else {
				// checkEffectiveFinality() returns if it's outer local
				if (checkEffectiveFinality(localBinding, currentScope)) {
					// outer local can be reached either through a synthetic arg or a synthetic field
					VariableBinding[] path = currentScope.getEmulationPath(localBinding);
					codeStream.generateOuterAccess(path, this, localBinding, currentScope);
				} else {
					codeStream.load(localBinding);
				}
			}
			break;
		default : // should not occur
			return null;
	}
	// all intermediate field accesses are read accesses
	// only the last field binding is a write access
	int positionsLength = this.sourcePositions.length;
	FieldBinding initialFieldBinding = lastFieldBinding; // can be null if initial was a local binding
	if (this.otherBindings != null) {
		for (int i = 0; i < otherBindingsCount; i++) {
			int pc = codeStream.position;
			FieldBinding nextField = this.otherBindings[i].original();
			TypeBinding nextGenericCast = this.otherGenericCasts == null ? null : this.otherGenericCasts[i];
			if (lastFieldBinding != null) {
				needValue = !nextField.isStatic();
				Constant fieldConstant = lastFieldBinding.constant();
				if (fieldConstant != Constant.NotAConstant) {
					if (i > 0 && !lastFieldBinding.isStatic()) {
						codeStream.invokeObjectGetClass(); // perform null check
						codeStream.pop();
					}
					if (needValue) {
						codeStream.generateConstant(fieldConstant, 0);
					}
				} else {
					if (needValue || (i > 0 && complyTo14) || lastGenericCast != null) {
						if (lastFieldBinding.canBeSeenBy(lastReceiverType, this, currentScope)) {
							MethodBinding accessor = this.syntheticReadAccessors == null ? null : this.syntheticReadAccessors[i];
							if (accessor == null) {
								TypeBinding constantPoolDeclaringClass = CodeStream.getConstantPoolDeclaringClass(currentScope, lastFieldBinding, lastReceiverType, i == 0 && this.indexOfFirstFieldBinding == 1);
								if (lastFieldBinding.isStatic()) {
									codeStream.fieldAccess(Opcodes.OPC_getstatic, lastFieldBinding, constantPoolDeclaringClass);
								} else {
									codeStream.fieldAccess(Opcodes.OPC_getfield, lastFieldBinding, constantPoolDeclaringClass);
								}
							} else {
								codeStream.invoke(Opcodes.OPC_invokestatic, accessor, null /* default declaringClass */);
							}
						} else {
							codeStream.generateEmulatedReadAccessForField(lastFieldBinding);
						}
						if (lastGenericCast != null) {
							codeStream.checkcast(lastGenericCast);
							lastReceiverType = lastGenericCast;
						} else {
							lastReceiverType = lastFieldBinding.type;
						}
						if (!needValue) codeStream.pop();
					} else {
						if (lastFieldBinding == initialFieldBinding) {
							if (lastFieldBinding.isStatic()){
								// if no valueRequired, still need possible side-effects of <clinit> invocation, if field belongs to different class
								if (TypeBinding.notEquals(initialFieldBinding.declaringClass, this.actualReceiverType.erasure())) {
									if (lastFieldBinding.canBeSeenBy(lastReceiverType, this, currentScope)) {
										MethodBinding accessor = this.syntheticReadAccessors == null ? null : this.syntheticReadAccessors[i];
										if (accessor == null) {
											TypeBinding constantPoolDeclaringClass = CodeStream.getConstantPoolDeclaringClass(currentScope, lastFieldBinding, lastReceiverType, i == 0 && this.indexOfFirstFieldBinding == 1);
											codeStream.fieldAccess(Opcodes.OPC_getstatic, lastFieldBinding, constantPoolDeclaringClass);
										} else {
											codeStream.invoke(Opcodes.OPC_invokestatic, accessor, null /* default declaringClass */);
										}
									} else {
										codeStream.generateEmulatedReadAccessForField(lastFieldBinding);
									}
									codeStream.pop();
								}
							}
						} else if (!lastFieldBinding.isStatic()){
							codeStream.invokeObjectGetClass(); // perform null check
							codeStream.pop();
						}
						lastReceiverType = lastFieldBinding.type;
					}
					if ((positionsLength - otherBindingsCount + i - 1) >= 0) {
						int fieldPosition = (int) (this.sourcePositions[positionsLength - otherBindingsCount + i - 1] >>>32);
						codeStream.recordPositionsFrom(pc, fieldPosition);
					}
				}
			}
			lastFieldBinding = nextField;
			lastGenericCast = nextGenericCast;
			if (lastFieldBinding != null && !lastFieldBinding.canBeSeenBy(lastReceiverType, this, currentScope)) {
				if (lastFieldBinding.isStatic()) {
					codeStream.aconst_null();
				}
			}
		}
	}
	return lastFieldBinding;
}


@Override
public void generateReceiver(CodeStream codeStream) {
	codeStream.aload_0();
	if (this.delegateThis != null) {
		codeStream.fieldAccess(Opcodes.OPC_getfield, this.delegateThis, null /* default declaringClass */); // delegated field access
	}
}

@Override
public TypeBinding getOtherFieldBindings(BlockScope scope) {
	// At this point restrictiveFlag may ONLY have two potential value : FIELD LOCAL (i.e cast <<(VariableBinding) binding>> is valid)
	int length = this.tokens.length;
	if ((this.bits & Binding.FIELD) != 0) {
		if (!((FieldBinding) this.binding).isStatic()) { //must check for the static status....
			if (this.indexOfFirstFieldBinding == 1) {
				//the field is the first token of the qualified reference....
				if (scope.methodScope().isStatic) {
					scope.problemReporter().staticFieldAccessToNonStaticVariable(this, (FieldBinding) this.binding);
					return null;
				}
			} else { //accessing to a field using a type as "receiver" is allowed only with static field
				scope.problemReporter().staticFieldAccessToNonStaticVariable(this, (FieldBinding) this.binding);
				return null;
			}
		}
		// only last field is actually a write access if any
		if (isFieldUseDeprecated((FieldBinding) this.binding, scope, this.indexOfFirstFieldBinding == length ? this.bits : 0)) {
			scope.problemReporter().deprecatedField((FieldBinding) this.binding, this);
		}
	}

	TypeBinding type = ((VariableBinding) this.binding).type;
	int index = this.indexOfFirstFieldBinding;
	if (index == length) { //	restrictiveFlag == FIELD
		this.constant = ((FieldBinding) this.binding).constant(scope);
		return type;
	}

	// allocation of the fieldBindings array	and its respective constants
	int otherBindingsLength = length - index;
	this.otherBindings = new FieldBinding[otherBindingsLength];

	// fill the first constant (the one of the binding)
	this.constant =((VariableBinding) this.binding).constant(scope);

	// iteration on each field
	while (index < length) {
		char[] token = this.tokens[index];
		if (type == null) return null; // could not resolve type prior to this point
		FieldBinding field = scope.getField(type, token, this);
		int place = index - this.indexOfFirstFieldBinding;
		this.otherBindings[place] = field;
		if (!field.isValidBinding()) {
			// try to retrieve the field as private field
			CodeSnippetScope localScope = new CodeSnippetScope(scope);
			if (this.delegateThis == null) {
				if (this.evaluationContext.declaringTypeName != null) {
					this.delegateThis = scope.getField(scope.enclosingSourceType(), DELEGATE_THIS, this);
					if (this.delegateThis == null){  // if not found then internal error, field should have been found
						return super.reportError(scope);
					}
					this.actualReceiverType = this.delegateThis.type;
				} else {
					this.constant = Constant.NotAConstant; //don't fill other constants slots...
					scope.problemReporter().invalidField(this, field, index, type);
					return null;
				}
			}
			field = localScope.getFieldForCodeSnippet(this.delegateThis.type, token, this);
			this.otherBindings[place] = field;
		}
		if (field.isValidBinding()) {
			// only last field is actually a write access if any
			if (isFieldUseDeprecated(field, scope, index+1 == length ? this.bits : 0)) {
				scope.problemReporter().deprecatedField(field, this);
			}
			// constant propagation can only be performed as long as the previous one is a constant too.
			if (this.constant != Constant.NotAConstant){
				this.constant = field.constant(scope);
			}
			type = field.type;
			index++;
		} else {
			this.constant = Constant.NotAConstant; //don't fill other constants slots...
			scope.problemReporter().invalidField(this, field, index, type);
			return null;
		}
	}
	return (this.otherBindings[otherBindingsLength - 1]).type;
}

/**
 * index is <0 to denote write access emulation
 */
@Override
public void manageSyntheticAccessIfNecessary(BlockScope currentScope, FieldBinding fieldBinding, int index, FlowInfo flowInfo) {
	// do nothing
}

/**
 * Normal field binding did not work, try to bind to a field of the delegate receiver.
 */
@Override
public TypeBinding reportError(BlockScope scope) {
	if (this.evaluationContext.declaringTypeName != null) {
		this.delegateThis = scope.getField(scope.enclosingSourceType(), DELEGATE_THIS, this);
		if (this.delegateThis == null){  // if not found then internal error, field should have been found
			return super.reportError(scope);
		}
		this.actualReceiverType = this.delegateThis.type;
	} else {
		return super.reportError(scope);
	}

	if ((this.binding instanceof ProblemFieldBinding && ((ProblemFieldBinding) this.binding).problemId() == NotFound)
		|| (this.binding instanceof ProblemBinding && ((ProblemBinding) this.binding).problemId() == NotFound)){
		// will not support innerclass emulation inside delegate
		FieldBinding fieldBinding = scope.getField(this.delegateThis.type, this.tokens[0], this);
		if (!fieldBinding.isValidBinding()) {
			if (((ProblemFieldBinding) fieldBinding).problemId() == NotVisible) {
				// manage the access to a private field of the enclosing type
				CodeSnippetScope localScope = new CodeSnippetScope(scope);
				this.binding = localScope.getFieldForCodeSnippet(this.delegateThis.type, this.tokens[0], this);
				if (this.binding.isValidBinding()) {
					return checkFieldAccess(scope);
				} else {
					return super.reportError(scope);
				}
			} else {
				return super.reportError(scope);
			}
		}
		this.binding = fieldBinding;
		return checkFieldAccess(scope);
	}

	TypeBinding result;
	if (this.binding instanceof ProblemFieldBinding && ((ProblemFieldBinding) this.binding).problemId() == NotVisible) {
		// field and/or local are done before type lookups
		// the only available value for the restrictiveFlag BEFORE
		// the TC is Flag_Type Flag_LocalField and Flag_TypeLocalField
		CodeSnippetScope localScope = new CodeSnippetScope(scope);
		if ((this.binding = localScope.getBinding(this.tokens, this.bits & RestrictiveFlagMASK, this, (ReferenceBinding) this.delegateThis.type)).isValidBinding()) {
			this.bits &= ~RestrictiveFlagMASK; // clear bits
			this.bits |= Binding.FIELD;
			result = getOtherFieldBindings(scope);
		} else {
			return super.reportError(scope);
		}
		if (result != null && result.isValidBinding()) {
			return result;
		}
	}
	return super.reportError(scope);
}
}
