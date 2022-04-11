/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
 *     Stephan Herrmann <stephan@cs.tu-berlin.de> - Contributions for
 *								bug 185682 - Increment/decrement operators mark local variables as read
 *								bug 331649 - [compiler][null] consider null annotations for fields
 *								bug 383368 - [compiler][null] syntactic null analysis for field references
 *								Bug 412203 - [compiler] Internal compiler error: java.lang.IllegalArgumentException: info cannot be null
 *								Bug 400874 - [1.8][compiler] Inference infrastructure should evolve to meet JLS8 18.x (Part G of JSR335 spec)
 *								Bug 458396 - NPE in CodeStream.invoke()
 *     Jesper S Moller - Contributions for
 *								Bug 378674 - "The method can be declared as static" is wrong
 *     Robert Roth <robert.roth.off@gmail.com> - Contributions for
 *								Bug 361039 - NPE in FieldReference.optimizedBooleanConstant
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.ast;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.codegen.CodeStream;
import org.eclipse.jdt.internal.compiler.codegen.Opcodes;
import org.eclipse.jdt.internal.compiler.flow.FlowContext;
import org.eclipse.jdt.internal.compiler.flow.FlowInfo;
import org.eclipse.jdt.internal.compiler.impl.Constant;
import org.eclipse.jdt.internal.compiler.lookup.Binding;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.FieldBinding;
import org.eclipse.jdt.internal.compiler.lookup.InferenceContext18;
import org.eclipse.jdt.internal.compiler.lookup.InvocationSite;
import org.eclipse.jdt.internal.compiler.lookup.LocalVariableBinding;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.MethodScope;
import org.eclipse.jdt.internal.compiler.lookup.MissingTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.ProblemFieldBinding;
import org.eclipse.jdt.internal.compiler.lookup.ProblemReasons;
import org.eclipse.jdt.internal.compiler.lookup.ProblemReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.Scope;
import org.eclipse.jdt.internal.compiler.lookup.SourceTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.TagBits;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeIds;
import org.eclipse.jdt.internal.compiler.lookup.VariableBinding;

public class FieldReference extends Reference implements InvocationSite {

	public static final int READ = 0;
	public static final int WRITE = 1;
	public Expression receiver;
	public char[] token;
	public FieldBinding binding;															// exact binding resulting from lookup
	public MethodBinding[] syntheticAccessors; // [0]=read accessor [1]=write accessor

	public long nameSourcePosition; //(start<<32)+end
	public TypeBinding actualReceiverType;
	public TypeBinding genericCast;

public FieldReference(char[] source, long pos) {
	this.token = source;
	this.nameSourcePosition = pos;
	//by default the position are the one of the field (not true for super access)
	this.sourceStart = (int) (pos >>> 32);
	this.sourceEnd = (int) (pos & 0x00000000FFFFFFFFL);
	this.bits |= Binding.FIELD;

}

@Override
public FlowInfo analyseAssignment(BlockScope currentScope, FlowContext flowContext, FlowInfo flowInfo, Assignment assignment, boolean isCompound) {
	// compound assignment extra work
	if (isCompound) { // check the variable part is initialized if blank final
		if (this.binding.isBlankFinal()
			&& this.receiver.isThis()
			&& currentScope.needBlankFinalFieldInitializationCheck(this.binding)) {
			FlowInfo fieldInits = flowContext.getInitsForFinalBlankInitializationCheck(this.binding.declaringClass.original(), flowInfo);
			if (!fieldInits.isDefinitelyAssigned(this.binding)) {
				currentScope.problemReporter().uninitializedBlankFinalField(this.binding, this);
				// we could improve error msg here telling "cannot use compound assignment on final blank field"
			}
		}
		manageSyntheticAccessIfNecessary(currentScope, flowInfo, true /*read-access*/);
	}
	flowInfo =
		this.receiver
			.analyseCode(currentScope, flowContext, flowInfo, !this.binding.isStatic())
			.unconditionalInits();

	this.receiver.checkNPE(currentScope, flowContext, flowInfo);

	if (assignment.expression != null) {
		flowInfo =
			assignment
				.expression
				.analyseCode(currentScope, flowContext, flowInfo)
				.unconditionalInits();
	}
	manageSyntheticAccessIfNecessary(currentScope, flowInfo, false /*write-access*/);

	// check if assigning a final field
	if (this.binding.isFinal()) {
		// in a context where it can be assigned?
		if (this.binding.isBlankFinal()
			&& !isCompound
			&& this.receiver.isThis()
			&& !(this.receiver instanceof QualifiedThisReference)
			&& ((this.receiver.bits & ASTNode.ParenthesizedMASK) == 0) // (this).x is forbidden
			&& currentScope.allowBlankFinalFieldAssignment(this.binding)
			&& !currentScope.methodScope().isCompactConstructorScope) {
			if (flowInfo.isPotentiallyAssigned(this.binding)) {
				currentScope.problemReporter().duplicateInitializationOfBlankFinalField(
					this.binding,
					this);
			} else {
				flowContext.recordSettingFinal(this.binding, this, flowInfo);
			}
			flowInfo.markAsDefinitelyAssigned(this.binding);
		} else {
			if (currentScope.methodScope().isCompactConstructorScope)
				currentScope.problemReporter().recordIllegalExplicitFinalFieldAssignInCompactConstructor(this.binding, this);
			else
			// assigning a final field outside an initializer or constructor or wrong reference
				currentScope.problemReporter().cannotAssignToFinalField(this.binding, this);
		}
	} else if (this.binding.isNonNull() || this.binding.type.isTypeVariable()) {
		// in a context where it can be assigned?
		if (   !isCompound
			&& this.receiver.isThis()
			&& !(this.receiver instanceof QualifiedThisReference)
			&& TypeBinding.equalsEquals(this.receiver.resolvedType, this.binding.declaringClass) // inherited fields are not tracked here
			&& ((this.receiver.bits & ASTNode.ParenthesizedMASK) == 0)) { // (this).x is forbidden
			flowInfo.markAsDefinitelyAssigned(this.binding);
		}
	}
	return flowInfo;
}

@Override
public FlowInfo analyseCode(BlockScope currentScope, FlowContext flowContext, FlowInfo flowInfo) {
	return analyseCode(currentScope, flowContext, flowInfo, true);
}

@Override
public FlowInfo analyseCode(BlockScope currentScope, FlowContext flowContext, FlowInfo flowInfo, boolean valueRequired) {
	boolean nonStatic = !this.binding.isStatic();
	this.receiver.analyseCode(currentScope, flowContext, flowInfo, nonStatic);
	if (nonStatic) {
		this.receiver.checkNPE(currentScope, flowContext, flowInfo, 1);
	}

	if (valueRequired || currentScope.compilerOptions().complianceLevel >= ClassFileConstants.JDK1_4) {
		manageSyntheticAccessIfNecessary(currentScope, flowInfo, true /*read-access*/);
	}
	if (currentScope.compilerOptions().complianceLevel >= ClassFileConstants.JDK1_7) {
		FieldBinding fieldBinding = this.binding;
		if (this.receiver.isThis() && fieldBinding.isBlankFinal() && currentScope.needBlankFinalFieldInitializationCheck(fieldBinding)) {
			FlowInfo fieldInits = flowContext.getInitsForFinalBlankInitializationCheck(fieldBinding.declaringClass.original(), flowInfo);
			if (!fieldInits.isDefinitelyAssigned(fieldBinding)) {
				currentScope.problemReporter().uninitializedBlankFinalField(fieldBinding, this);
			}
		}
	}
	return flowInfo;
}

@Override
public boolean checkNPE(BlockScope scope, FlowContext flowContext, FlowInfo flowInfo, int ttlForFieldCheck) {
	if (flowContext.isNullcheckedFieldAccess(this)) {
		return true; // enough seen
	}
	return checkNullableFieldDereference(scope, this.binding, this.nameSourcePosition, flowContext, ttlForFieldCheck);
}

/**
 * @see org.eclipse.jdt.internal.compiler.ast.Expression#computeConversion(org.eclipse.jdt.internal.compiler.lookup.Scope, org.eclipse.jdt.internal.compiler.lookup.TypeBinding, org.eclipse.jdt.internal.compiler.lookup.TypeBinding)
 */
@Override
public void computeConversion(Scope scope, TypeBinding runtimeTimeType, TypeBinding compileTimeType) {
	if (runtimeTimeType == null || compileTimeType == null)
		return;
	// set the generic cast after the fact, once the type expectation is fully known (no need for strict cast)
	if (this.binding != null && this.binding.isValidBinding()) {
		FieldBinding originalBinding = this.binding.original();
		TypeBinding originalType = originalBinding.type;
	    // extra cast needed if field type is type variable
		if (originalType.leafComponentType().isTypeVariable()) {
	    	TypeBinding targetType = (!compileTimeType.isBaseType() && runtimeTimeType.isBaseType())
	    		? compileTimeType  // unboxing: checkcast before conversion
	    		: runtimeTimeType;
	        this.genericCast = originalBinding.type.genericCast(targetType);
	        if (this.genericCast instanceof ReferenceBinding) {
				ReferenceBinding referenceCast = (ReferenceBinding) this.genericCast;
				if (!referenceCast.canBeSeenBy(scope)) {
		        	scope.problemReporter().invalidType(this,
		        			new ProblemReferenceBinding(
								CharOperation.splitOn('.', referenceCast.shortReadableName()),
								referenceCast,
								ProblemReasons.NotVisible));
				}
	        }
		}
	}
	super.computeConversion(scope, runtimeTimeType, compileTimeType);
}

@Override
public FieldBinding fieldBinding() {
	return this.binding;
}

@Override
public void generateAssignment(BlockScope currentScope, CodeStream codeStream, Assignment assignment, boolean valueRequired) {
	int pc = codeStream.position;
	FieldBinding codegenBinding = this.binding.original();
	this.receiver.generateCode(currentScope, codeStream, !codegenBinding.isStatic());
	codeStream.recordPositionsFrom(pc, this.sourceStart);
	assignment.expression.generateCode(currentScope, codeStream, true);
	fieldStore(currentScope, codeStream, codegenBinding, this.syntheticAccessors == null ? null : this.syntheticAccessors[FieldReference.WRITE], this.actualReceiverType, this.receiver.isImplicitThis(), valueRequired);
	if (valueRequired) {
		codeStream.generateImplicitConversion(assignment.implicitConversion);
	}
	// no need for generic cast as value got dupped
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
		codeStream.recordPositionsFrom(pc, this.sourceStart);
		return;
	}
	FieldBinding codegenBinding = this.binding.original();
	boolean isStatic = codegenBinding.isStatic();
	boolean isThisReceiver = this.receiver instanceof ThisReference;
	Constant fieldConstant = codegenBinding.constant();
	if (fieldConstant != Constant.NotAConstant) {
		if (!isThisReceiver) {
			this.receiver.generateCode(currentScope, codeStream, !isStatic);
			if (!isStatic){
				codeStream.invokeObjectGetClass();
				codeStream.pop();
			}
		}
		if (valueRequired) {
			codeStream.generateConstant(fieldConstant, this.implicitConversion);
		}
		codeStream.recordPositionsFrom(pc, this.sourceStart);
		return;
	}
	if (valueRequired
			|| (!isThisReceiver && currentScope.compilerOptions().complianceLevel >= ClassFileConstants.JDK1_4)
			|| ((this.implicitConversion & TypeIds.UNBOXING) != 0)
			|| (this.genericCast != null)) {
		this.receiver.generateCode(currentScope, codeStream, !isStatic);
		if ((this.bits & NeedReceiverGenericCast) != 0) {
			codeStream.checkcast(this.actualReceiverType);
		}
		pc = codeStream.position;
		if (codegenBinding.declaringClass == null) { // array length
			codeStream.arraylength();
			if (valueRequired) {
				codeStream.generateImplicitConversion(this.implicitConversion);
			} else {
				// could occur if !valueRequired but compliance >= 1.4
				codeStream.pop();
			}
		} else {
			if (this.syntheticAccessors == null || this.syntheticAccessors[FieldReference.READ] == null) {
				TypeBinding constantPoolDeclaringClass = CodeStream.getConstantPoolDeclaringClass(currentScope, codegenBinding, this.actualReceiverType, this.receiver.isImplicitThis());
				if (isStatic) {
					codeStream.fieldAccess(Opcodes.OPC_getstatic, codegenBinding, constantPoolDeclaringClass);
				} else {
					codeStream.fieldAccess(Opcodes.OPC_getfield, codegenBinding, constantPoolDeclaringClass);
				}
			} else {
				codeStream.invoke(Opcodes.OPC_invokestatic, this.syntheticAccessors[FieldReference.READ], null /* default declaringClass */);
			}
			// required cast must occur even if no value is required
			if (this.genericCast != null) codeStream.checkcast(this.genericCast);
			if (valueRequired) {
				codeStream.generateImplicitConversion(this.implicitConversion);
			} else {
				boolean isUnboxing = (this.implicitConversion & TypeIds.UNBOXING) != 0;
				// conversion only generated if unboxing
				if (isUnboxing) codeStream.generateImplicitConversion(this.implicitConversion);
				switch (isUnboxing ? postConversionType(currentScope).id : codegenBinding.type.id) {
					case T_long :
					case T_double :
						codeStream.pop2();
						break;
					default :
						codeStream.pop();
				}
			}
		}
	} else {
		if (isThisReceiver) {
			if (isStatic){
				// if no valueRequired, still need possible side-effects of <clinit> invocation, if field belongs to different class
				if (TypeBinding.notEquals(this.binding.original().declaringClass, this.actualReceiverType.erasure())) {
					MethodBinding accessor = this.syntheticAccessors == null ? null : this.syntheticAccessors[FieldReference.READ];
					if (accessor == null) {
						TypeBinding constantPoolDeclaringClass = CodeStream.getConstantPoolDeclaringClass(currentScope, codegenBinding, this.actualReceiverType, this.receiver.isImplicitThis());
						codeStream.fieldAccess(Opcodes.OPC_getstatic, codegenBinding, constantPoolDeclaringClass);
					} else {
						codeStream.invoke(Opcodes.OPC_invokestatic, accessor, null /* default declaringClass */);
					}
					switch (codegenBinding.type.id) {
						case T_long :
						case T_double :
							codeStream.pop2();
							break;
						default :
							codeStream.pop();
					}
				}
			}
		} else {
			this.receiver.generateCode(currentScope, codeStream, !isStatic);
			if (!isStatic){
				codeStream.invokeObjectGetClass(); // perform null check
				codeStream.pop();
			}
		}
	}
	codeStream.recordPositionsFrom(pc, this.sourceEnd);
}

@Override
public void generateCompoundAssignment(BlockScope currentScope, CodeStream codeStream, Expression expression, int operator, int assignmentImplicitConversion, boolean valueRequired) {
	boolean isStatic;
	// check if compound assignment is the only usage of a private field
	reportOnlyUselesslyReadPrivateField(currentScope, this.binding, valueRequired);
	FieldBinding codegenBinding = this.binding.original();
	this.receiver.generateCode(currentScope, codeStream, !(isStatic = codegenBinding.isStatic()));
	if (isStatic) {
		if (this.syntheticAccessors == null || this.syntheticAccessors[FieldReference.READ] == null) {
			TypeBinding constantPoolDeclaringClass = CodeStream.getConstantPoolDeclaringClass(currentScope, codegenBinding, this.actualReceiverType, this.receiver.isImplicitThis());
			codeStream.fieldAccess(Opcodes.OPC_getstatic, codegenBinding, constantPoolDeclaringClass);
		} else {
			codeStream.invoke(Opcodes.OPC_invokestatic, this.syntheticAccessors[FieldReference.READ], null /* default declaringClass */);
		}
	} else {
		codeStream.dup();
		if (this.syntheticAccessors == null || this.syntheticAccessors[FieldReference.READ] == null) {
			TypeBinding constantPoolDeclaringClass = CodeStream.getConstantPoolDeclaringClass(currentScope, codegenBinding, this.actualReceiverType, this.receiver.isImplicitThis());
			codeStream.fieldAccess(Opcodes.OPC_getfield, codegenBinding, constantPoolDeclaringClass);
		} else {
			codeStream.invoke(Opcodes.OPC_invokestatic, this.syntheticAccessors[FieldReference.READ], null /* default declaringClass */);
		}
	}
	int operationTypeID;
	switch(operationTypeID = (this.implicitConversion & TypeIds.IMPLICIT_CONVERSION_MASK) >> 4) {
		case T_JavaLangString :
		case T_JavaLangObject :
		case T_undefined :
			codeStream.generateStringConcatenationAppend(currentScope, null, expression);
			break;
		default :
			if (this.genericCast != null)
				codeStream.checkcast(this.genericCast);
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
	fieldStore(currentScope, codeStream, codegenBinding, this.syntheticAccessors == null ? null : this.syntheticAccessors[FieldReference.WRITE], this.actualReceiverType, this.receiver.isImplicitThis(), valueRequired);
	// no need for generic cast as value got dupped
}

@Override
public void generatePostIncrement(BlockScope currentScope, CodeStream codeStream, CompoundAssignment postIncrement, boolean valueRequired) {
	boolean isStatic;
	// check if postIncrement is the only usage of a private field
	reportOnlyUselesslyReadPrivateField(currentScope, this.binding, valueRequired);
	FieldBinding codegenBinding = this.binding.original();
	this.receiver.generateCode(currentScope, codeStream, !(isStatic = codegenBinding.isStatic()));
	if (isStatic) {
		if (this.syntheticAccessors == null || this.syntheticAccessors[FieldReference.READ] == null) {
			TypeBinding constantPoolDeclaringClass = CodeStream.getConstantPoolDeclaringClass(currentScope, codegenBinding, this.actualReceiverType, this.receiver.isImplicitThis());
			codeStream.fieldAccess(Opcodes.OPC_getstatic, codegenBinding, constantPoolDeclaringClass);
		} else {
			codeStream.invoke(Opcodes.OPC_invokestatic, this.syntheticAccessors[FieldReference.READ], null /* default declaringClass */);
		}
	} else {
		codeStream.dup();
		if (this.syntheticAccessors == null || this.syntheticAccessors[FieldReference.READ] == null) {
			TypeBinding constantPoolDeclaringClass = CodeStream.getConstantPoolDeclaringClass(currentScope, codegenBinding, this.actualReceiverType, this.receiver.isImplicitThis());
			codeStream.fieldAccess(Opcodes.OPC_getfield, codegenBinding, constantPoolDeclaringClass);
		} else {
			codeStream.invoke(Opcodes.OPC_invokestatic, this.syntheticAccessors[FieldReference.READ], null /* default declaringClass */);
		}
	}
	TypeBinding operandType;
	if (this.genericCast != null) {
		codeStream.checkcast(this.genericCast);
		operandType = this.genericCast;
	} else {
		operandType = codegenBinding.type;
	}
	if (valueRequired) {
		if (isStatic) {
			switch (operandType.id) {
				case TypeIds.T_long :
				case TypeIds.T_double :
					codeStream.dup2();
					break;
				default :
					codeStream.dup();
					break;
			}
		} else { // Stack:  [owner][old field value]  ---> [old field value][owner][old field value]
			switch (operandType.id) {
				case TypeIds.T_long :
				case TypeIds.T_double :
					codeStream.dup2_x1();
					break;
				default :
					codeStream.dup_x1();
					break;
			}
		}
	}
	codeStream.generateImplicitConversion(this.implicitConversion);
	codeStream.generateConstant(
		postIncrement.expression.constant,
		this.implicitConversion);
	codeStream.sendOperator(postIncrement.operator, this.implicitConversion & TypeIds.COMPILE_TYPE_MASK);
	codeStream.generateImplicitConversion(
		postIncrement.preAssignImplicitConversion);
	fieldStore(currentScope, codeStream, codegenBinding, this.syntheticAccessors == null ? null : this.syntheticAccessors[FieldReference.WRITE], this.actualReceiverType, this.receiver.isImplicitThis(), false);
}

/**
 * @see org.eclipse.jdt.internal.compiler.lookup.InvocationSite#genericTypeArguments()
 */
@Override
public TypeBinding[] genericTypeArguments() {
	return null;
}

@Override
public InferenceContext18 freshInferenceContext(Scope scope) {
	return null;
}

@Override
public boolean isEquivalent(Reference reference) {
	// only consider field references relative to "this":
	if (this.receiver.isThis() && !(this.receiver instanceof QualifiedThisReference)) {
		// current is a simple "this.f1"
		char[] otherToken = null;
		// matching 'reference' could be "f1" or "this.f1":
		if (reference instanceof SingleNameReference) {
			otherToken = ((SingleNameReference) reference).token;
		} else if (reference instanceof FieldReference) {
			FieldReference fr = (FieldReference) reference;
			if (fr.receiver.isThis() && !(fr.receiver instanceof QualifiedThisReference)) {
				otherToken = fr.token;
			}
		}
		return otherToken != null && CharOperation.equals(this.token, otherToken);
	} else {
		// search deeper for "this" inside:
		char[][] thisTokens = getThisFieldTokens(1);
		if (thisTokens == null) {
			return false;
		}
		// other can be "this.f1.f2", too, or "f1.f2":
		char[][] otherTokens = null;
		if (reference instanceof FieldReference) {
			otherTokens = ((FieldReference) reference).getThisFieldTokens(1);
		} else if (reference instanceof QualifiedNameReference) {
			if (((QualifiedNameReference)reference).binding instanceof LocalVariableBinding)
				return false; // initial variable mismatch: local (from f1.f2) vs. field (from this.f1.f2)
			otherTokens = ((QualifiedNameReference) reference).tokens;
		}
		return CharOperation.equals(thisTokens, otherTokens);
	}
}

private char[][] getThisFieldTokens(int nestingCount) {
	char[][] result = null;
	if (this.receiver.isThis() && ! (this.receiver instanceof QualifiedThisReference)) {
		// found an inner-most this-reference, start building the token array:
		result = new char[nestingCount][];
		// fill it front to tail while traveling back out:
		result[0] = this.token;
	} else if (this.receiver instanceof FieldReference) {
		result = ((FieldReference)this.receiver).getThisFieldTokens(nestingCount+1);
		if (result != null) {
			// front to tail: outermost is last:
			result[result.length-nestingCount] = this.token;
		}
	}
	return result;
}

@Override
public boolean isSuperAccess() {
	return this.receiver.isSuper();
}

@Override
public boolean isQualifiedSuper() {
	return this.receiver.isQualifiedSuper();
}

@Override
public boolean isTypeAccess() {
	return this.receiver != null && this.receiver.isTypeReference();
}

@Override
public FieldBinding lastFieldBinding() {
	return this.binding;
}

/*
 * No need to emulate access to protected fields since not implicitly accessed
 */
public void manageSyntheticAccessIfNecessary(BlockScope currentScope, FlowInfo flowInfo, boolean isReadAccess) {
	if ((flowInfo.tagBits & FlowInfo.UNREACHABLE_OR_DEAD) != 0)	return;

	// if field from parameterized type got found, use the original field at codegen time
	FieldBinding codegenBinding = this.binding.original();
	if (this.binding.isPrivate()) {
		if (!currentScope.enclosingSourceType().isNestmateOf(codegenBinding.declaringClass) &&
			(TypeBinding.notEquals(currentScope.enclosingSourceType(), codegenBinding.declaringClass))
				&& this.binding.constant(currentScope) == Constant.NotAConstant) {
			if (this.syntheticAccessors == null)
				this.syntheticAccessors = new MethodBinding[2];
			this.syntheticAccessors[isReadAccess ? FieldReference.READ : FieldReference.WRITE] =
				((SourceTypeBinding) codegenBinding.declaringClass).addSyntheticMethod(codegenBinding, isReadAccess, false /* not super ref in remote type*/);
			currentScope.problemReporter().needToEmulateFieldAccess(codegenBinding, this, isReadAccess);
			return;
		}
	} else if (this.receiver instanceof QualifiedSuperReference) { // qualified super
		// qualified super need emulation always
		SourceTypeBinding destinationType = (SourceTypeBinding) (((QualifiedSuperReference) this.receiver).currentCompatibleType);
		if (this.syntheticAccessors == null)
			this.syntheticAccessors = new MethodBinding[2];
		this.syntheticAccessors[isReadAccess ? FieldReference.READ : FieldReference.WRITE] = destinationType.addSyntheticMethod(codegenBinding, isReadAccess, isSuperAccess());
		currentScope.problemReporter().needToEmulateFieldAccess(codegenBinding, this, isReadAccess);
		return;

	} else if (this.binding.isProtected()) {
		SourceTypeBinding enclosingSourceType;
		if (((this.bits & ASTNode.DepthMASK) != 0)
			&& this.binding.declaringClass.getPackage()
				!= (enclosingSourceType = currentScope.enclosingSourceType()).getPackage()) {

			SourceTypeBinding currentCompatibleType =
				(SourceTypeBinding) enclosingSourceType.enclosingTypeAt(
					(this.bits & ASTNode.DepthMASK) >> ASTNode.DepthSHIFT);
			if (this.syntheticAccessors == null)
				this.syntheticAccessors = new MethodBinding[2];
			this.syntheticAccessors[isReadAccess ? FieldReference.READ : FieldReference.WRITE] = currentCompatibleType.addSyntheticMethod(codegenBinding, isReadAccess, isSuperAccess());
			currentScope.problemReporter().needToEmulateFieldAccess(codegenBinding, this, isReadAccess);
			return;
		}
	}
}

@Override
public Constant optimizedBooleanConstant() {
	if (this.resolvedType == null)
		return Constant.NotAConstant;
	switch (this.resolvedType.id) {
		case T_boolean :
		case T_JavaLangBoolean :
			return this.constant != Constant.NotAConstant ? this.constant : this.binding.constant();
		default :
			return Constant.NotAConstant;
	}
}

/**
 * @see org.eclipse.jdt.internal.compiler.ast.Expression#postConversionType(Scope)
 */
@Override
public TypeBinding postConversionType(Scope scope) {
	TypeBinding convertedType = this.resolvedType;
	if (this.genericCast != null)
		convertedType = this.genericCast;
	int runtimeType = (this.implicitConversion & TypeIds.IMPLICIT_CONVERSION_MASK) >> 4;
	switch (runtimeType) {
		case T_boolean :
			convertedType = TypeBinding.BOOLEAN;
			break;
		case T_byte :
			convertedType = TypeBinding.BYTE;
			break;
		case T_short :
			convertedType = TypeBinding.SHORT;
			break;
		case T_char :
			convertedType = TypeBinding.CHAR;
			break;
		case T_int :
			convertedType = TypeBinding.INT;
			break;
		case T_float :
			convertedType = TypeBinding.FLOAT;
			break;
		case T_long :
			convertedType = TypeBinding.LONG;
			break;
		case T_double :
			convertedType = TypeBinding.DOUBLE;
			break;
		default :
	}
	if ((this.implicitConversion & TypeIds.BOXING) != 0) {
		convertedType = scope.environment().computeBoxingType(convertedType);
	}
	return convertedType;
}

@Override
public StringBuffer printExpression(int indent, StringBuffer output) {
	return this.receiver.printExpression(0, output).append('.').append(this.token);
}

@Override
public TypeBinding resolveType(BlockScope scope) {
	// Answer the signature type of the field.
	// constants are propaged when the field is final
	// and initialized with a (compile time) constant

	//always ignore receiver cast, since may affect constant pool reference
	boolean receiverCast = false;
	if (this.receiver instanceof CastExpression) {
		this.receiver.bits |= ASTNode.DisableUnnecessaryCastCheck; // will check later on
		receiverCast = true;
	}
	this.actualReceiverType = this.receiver.resolveType(scope);
	if (this.actualReceiverType == null) {
		this.constant = Constant.NotAConstant;
		return null;
	}
	if (receiverCast) {
		 // due to change of declaring class with receiver type, only identity cast should be notified
		if (TypeBinding.equalsEquals(((CastExpression)this.receiver).expression.resolvedType, this.actualReceiverType)) {
				scope.problemReporter().unnecessaryCast((CastExpression)this.receiver);
		}
	}
	// the case receiverType.isArrayType and token = 'length' is handled by the scope API
	FieldBinding fieldBinding = this.binding = scope.getField(this.actualReceiverType, this.token, this);
	if (!fieldBinding.isValidBinding()) {
		this.constant = Constant.NotAConstant;
		if (this.receiver.resolvedType instanceof ProblemReferenceBinding) {
			// problem already got signaled on receiver, do not report secondary problem
			return null;
		}
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=245007 avoid secondary errors in case of
		// missing super type for anonymous classes ...
		ReferenceBinding declaringClass = fieldBinding.declaringClass;
		boolean avoidSecondary = declaringClass != null &&
								 declaringClass.isAnonymousType() &&
								 declaringClass.superclass() instanceof MissingTypeBinding;
		if (!avoidSecondary) {
			scope.problemReporter().invalidField(this, this.actualReceiverType);
		}
		if (fieldBinding instanceof ProblemFieldBinding) {
			ProblemFieldBinding problemFieldBinding = (ProblemFieldBinding) fieldBinding;
			FieldBinding closestMatch = problemFieldBinding.closestMatch;
			switch(problemFieldBinding.problemId()) {
				case ProblemReasons.InheritedNameHidesEnclosingName :
				case ProblemReasons.NotVisible :
				case ProblemReasons.NonStaticReferenceInConstructorInvocation :
				case ProblemReasons.NonStaticReferenceInStaticContext :
					if (closestMatch != null) {
						fieldBinding = closestMatch;
					}
			}
		}
		if (!fieldBinding.isValidBinding()) {
			return null;
		}
	}
	// handle indirect inheritance thru variable secondary bound
	// receiver may receive generic cast, as part of implicit conversion
	TypeBinding oldReceiverType = this.actualReceiverType;
	this.actualReceiverType = this.actualReceiverType.getErasureCompatibleType(fieldBinding.declaringClass);
	this.receiver.computeConversion(scope, this.actualReceiverType, this.actualReceiverType);
	if (TypeBinding.notEquals(this.actualReceiverType, oldReceiverType) && TypeBinding.notEquals(this.receiver.postConversionType(scope), this.actualReceiverType)) { // record need for explicit cast at codegen since receiver could not handle it
		this.bits |= NeedReceiverGenericCast;
	}
	if (isFieldUseDeprecated(fieldBinding, scope, this.bits)) {
		scope.problemReporter().deprecatedField(fieldBinding, this);
	}
	boolean isImplicitThisRcv = this.receiver.isImplicitThis();
	this.constant = isImplicitThisRcv ? fieldBinding.constant(scope) : Constant.NotAConstant;
	if (fieldBinding.isStatic()) {
		// static field accessed through receiver? legal but unoptimal (optional warning)
		if (!(isImplicitThisRcv
				|| (this.receiver instanceof NameReference
					&& (((NameReference) this.receiver).bits & Binding.TYPE) != 0))) {
			scope.problemReporter().nonStaticAccessToStaticField(this, fieldBinding);
		}
		ReferenceBinding declaringClass = this.binding.declaringClass;
		if (!isImplicitThisRcv
				&& TypeBinding.notEquals(declaringClass, this.actualReceiverType)
				&& declaringClass.canBeSeenBy(scope)) {
			scope.problemReporter().indirectAccessToStaticField(this, fieldBinding);
		}
		// check if accessing enum static field in initializer
		if (declaringClass.isEnum() && scope.kind != Scope.MODULE_SCOPE) {
			MethodScope methodScope = scope.methodScope();
			SourceTypeBinding sourceType = scope.enclosingSourceType();
			if (this.constant == Constant.NotAConstant
					&& !methodScope.isStatic
					&& (TypeBinding.equalsEquals(sourceType, declaringClass) || TypeBinding.equalsEquals(sourceType.superclass, declaringClass)) // enum constant body
					&& methodScope.isInsideInitializerOrConstructor()) {
				scope.problemReporter().enumStaticFieldUsedDuringInitialization(this.binding, this);
			}
		}
	}
	TypeBinding fieldType = fieldBinding.type;
	if (fieldType != null) {
		if ((this.bits & ASTNode.IsStrictlyAssigned) == 0) {
			fieldType = fieldType.capture(scope, this.sourceStart, this.sourceEnd);	// perform capture conversion if read access
		}
		this.resolvedType = fieldType;
		if ((fieldType.tagBits & TagBits.HasMissingType) != 0) {
			scope.problemReporter().invalidType(this, fieldType);
			return null;
		}
	}
	return fieldType;
}

@Override
public void setActualReceiverType(ReferenceBinding receiverType) {
	this.actualReceiverType = receiverType;
}

@Override
public void setDepth(int depth) {
	this.bits &= ~ASTNode.DepthMASK; // flush previous depth if any
	if (depth > 0) {
		this.bits |= (depth & 0xFF) << ASTNode.DepthSHIFT; // encoded on 8 bits
	}
}

@Override
public void setFieldIndex(int index) {
	// ignored
}

@Override
public void traverse(ASTVisitor visitor, BlockScope scope) {
	if (visitor.visit(this, scope)) {
		this.receiver.traverse(visitor, scope);
	}
	visitor.endVisit(this, scope);
}

@Override
public VariableBinding nullAnnotatedVariableBinding(boolean supportTypeAnnotations) {
	if (this.binding != null) {
		if (supportTypeAnnotations
				|| ((this.binding.tagBits & TagBits.AnnotationNullMASK) != 0)) {
			return this.binding;
		}
	}
	return null;
}
}
