/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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
 *								bug 392862 - [1.8][compiler][null] Evaluate null annotations on array types
 *								bug 331649 - [compiler][null] consider null annotations for fields
 *								bug 383368 - [compiler][null] syntactic null analysis for field references
 *								bug 392384 - [1.8][compiler][null] Restore nullness info from type annotations in class files
 *								Bug 392099 - [1.8][compiler][null] Apply null annotation on types for null analysis
 *								Bug 411964 - [1.8][null] leverage null type annotation in foreach statement
 *								Bug 407414 - [compiler][null] Incorrect warning on a primitive type being null
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.ast;

import org.eclipse.jdt.internal.compiler.codegen.CodeStream;
import org.eclipse.jdt.internal.compiler.codegen.Opcodes;
import org.eclipse.jdt.internal.compiler.flow.FlowContext;
import org.eclipse.jdt.internal.compiler.flow.FlowInfo;
import org.eclipse.jdt.internal.compiler.impl.Constant;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ExtraCompilerModifiers;
import org.eclipse.jdt.internal.compiler.lookup.FieldBinding;
import org.eclipse.jdt.internal.compiler.lookup.LocalVariableBinding;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.MethodScope;
import org.eclipse.jdt.internal.compiler.lookup.Scope;
import org.eclipse.jdt.internal.compiler.lookup.TagBits;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeIds;

public abstract class Reference extends Expression  {
/**
 * BaseLevelReference constructor comment.
 */
public Reference() {
	super();
}
public abstract FlowInfo analyseAssignment(BlockScope currentScope, FlowContext flowContext, FlowInfo flowInfo, Assignment assignment, boolean isCompound);

@Override
public FlowInfo analyseCode(BlockScope currentScope, FlowContext flowContext, FlowInfo flowInfo) {
	return flowInfo;
}

@Override
public boolean checkNPE(BlockScope scope, FlowContext flowContext, FlowInfo flowInfo, int ttlForFieldCheck) {
	if (flowContext.isNullcheckedFieldAccess(this)) {
		return true; // enough seen
	}
	return super.checkNPE(scope, flowContext, flowInfo, ttlForFieldCheck);
}

protected boolean checkNullableFieldDereference(Scope scope, FieldBinding field, long sourcePosition, FlowContext flowContext, int ttlForFieldCheck) {
	if (field != null) {
		if (ttlForFieldCheck > 0 && scope.compilerOptions().enableSyntacticNullAnalysisForFields)
			flowContext.recordNullCheckedFieldReference(this, ttlForFieldCheck);
		// preference to type annotations if we have any
		if ((field.type.tagBits & TagBits.AnnotationNullable) != 0) {
			scope.problemReporter().dereferencingNullableExpression(sourcePosition, scope.environment());
			return true;
		}
		if (field.type.isFreeTypeVariable()) {
			scope.problemReporter().fieldFreeTypeVariableReference(field, sourcePosition);
			return true;
		}
		if ((field.tagBits & TagBits.AnnotationNullable) != 0) {
			scope.problemReporter().nullableFieldDereference(field, sourcePosition);
			return true;
		}
	}
	return false;
}

public FieldBinding fieldBinding() {
	//this method should be sent one FIELD-tagged references
	//  (ref.bits & BindingIds.FIELD != 0)()
	return null ;
}

public void fieldStore(Scope currentScope, CodeStream codeStream, FieldBinding fieldBinding, MethodBinding syntheticWriteAccessor, TypeBinding receiverType, boolean isImplicitThisReceiver, boolean valueRequired) {
	int pc = codeStream.position;
	if (fieldBinding.isStatic()) {
		if (valueRequired) {
			switch (fieldBinding.type.id) {
				case TypeIds.T_long :
				case TypeIds.T_double :
					codeStream.dup2();
					break;
				default :
					codeStream.dup();
					break;
			}
		}
		if (syntheticWriteAccessor == null) {
			TypeBinding constantPoolDeclaringClass = CodeStream.getConstantPoolDeclaringClass(currentScope, fieldBinding, receiverType, isImplicitThisReceiver);
			codeStream.fieldAccess(Opcodes.OPC_putstatic, fieldBinding, constantPoolDeclaringClass);
		} else {
			codeStream.invoke(Opcodes.OPC_invokestatic, syntheticWriteAccessor, null /* default declaringClass */);
		}
	} else { // Stack:  [owner][new field value]  ---> [new field value][owner][new field value]
		if (valueRequired) {
			switch (fieldBinding.type.id) {
				case TypeIds.T_long :
				case TypeIds.T_double :
					codeStream.dup2_x1();
					break;
				default :
					codeStream.dup_x1();
					break;
			}
		}
		if (syntheticWriteAccessor == null) {
			TypeBinding constantPoolDeclaringClass = CodeStream.getConstantPoolDeclaringClass(currentScope, fieldBinding, receiverType, isImplicitThisReceiver);
			codeStream.fieldAccess(Opcodes.OPC_putfield, fieldBinding, constantPoolDeclaringClass);
		} else {
			codeStream.invoke(Opcodes.OPC_invokestatic, syntheticWriteAccessor, null /* default declaringClass */);
		}
	}
	codeStream.recordPositionsFrom(pc, this.sourceStart);
}

public abstract void generateAssignment(BlockScope currentScope, CodeStream codeStream, Assignment assignment, boolean valueRequired);

public abstract void generateCompoundAssignment(BlockScope currentScope, CodeStream codeStream, Expression expression, int operator, int assignmentImplicitConversion, boolean valueRequired);

public abstract void generatePostIncrement(BlockScope currentScope, CodeStream codeStream, CompoundAssignment postIncrement, boolean valueRequired);

/**
 * Is the given reference equivalent to the receiver,
 * meaning that both denote the same path of field reads?
 * Used from {@link FlowContext#isNullcheckedFieldAccess(Reference)}.
 */
public boolean isEquivalent(Reference reference) {
	return false;
}

public FieldBinding lastFieldBinding() {
	// override to answer the field designated by the entire reference
	// (as opposed to fieldBinding() which answers the first field in a QNR)
	return null;
}

@Override
public int nullStatus(FlowInfo flowInfo, FlowContext flowContext) {
	if ((this.implicitConversion & TypeIds.BOXING) != 0)
		return FlowInfo.NON_NULL;
	FieldBinding fieldBinding = lastFieldBinding();
	if (fieldBinding != null) {
		if (fieldBinding.isFinal() && fieldBinding.constant() != Constant.NotAConstant)
			return FlowInfo.NON_NULL;
		if (fieldBinding.isNonNull() || flowContext.isNullcheckedFieldAccess(this)) {
			return FlowInfo.NON_NULL;
		} else if (fieldBinding.isNullable()) {
			return FlowInfo.POTENTIALLY_NULL;
		} else if (fieldBinding.type.isFreeTypeVariable()) {
			return FlowInfo.FREE_TYPEVARIABLE;
		}
	}
	if (this.resolvedType != null) {
		return FlowInfo.tagBitsToNullStatus(this.resolvedType.tagBits);
	}
	return FlowInfo.UNKNOWN;
}

/* report if a private field is only read from a 'special operator',
 * i.e., in a postIncrement expression or a compound assignment,
 * where the information is never flowing out off the field. */
void reportOnlyUselesslyReadPrivateField(BlockScope currentScope, FieldBinding fieldBinding, boolean valueRequired) {
	if (valueRequired) {
		// access is relevant, turn compound use into real use:
		fieldBinding.compoundUseFlag = 0;
		fieldBinding.modifiers |= ExtraCompilerModifiers.AccLocallyUsed;
	} else {
		if (fieldBinding.isUsedOnlyInCompound()) {
			fieldBinding.compoundUseFlag--; // consume one
			if (fieldBinding.compoundUseFlag == 0					// report only the last usage
					&& fieldBinding.isOrEnclosedByPrivateType()
					&& (this.implicitConversion & TypeIds.UNBOXING) == 0) // don't report if unboxing is involved (might cause NPE)
			{
				// compoundAssignment/postIncrement is the only usage of this field
				currentScope.problemReporter().unusedPrivateField(fieldBinding.sourceField());
			}
		}
	}
}
/* report a local/arg that is only read from a 'special operator',
 * i.e., in a postIncrement expression or a compound assignment,
 * where the information is never flowing out off the local/arg. */
static void reportOnlyUselesslyReadLocal(BlockScope currentScope, LocalVariableBinding localBinding, boolean valueRequired) {
	if (localBinding.declaration == null)
		return;  // secret local
	if ((localBinding.declaration.bits & ASTNode.IsLocalDeclarationReachable) == 0)
		return;  // declaration is unreachable
	if (localBinding.useFlag >= LocalVariableBinding.USED)
		return;  // we're only interested in cases with only compound access (negative count)

	if (valueRequired) {
		// access is relevant
		localBinding.useFlag = LocalVariableBinding.USED;
		return;
	} else {
		localBinding.useFlag++;
		if (localBinding.useFlag != LocalVariableBinding.UNUSED) // have all negative counts been consumed?
			return; // still waiting to see more usages of this kind
	}
	// at this point we know we have something to report
	if (localBinding.declaration instanceof Argument) {
		// check compiler options to report against unused arguments
		MethodScope methodScope = currentScope.methodScope();
		if (methodScope != null && !methodScope.isLambdaScope()) { // lambda must be congruent with the descriptor.
			MethodBinding method = ((AbstractMethodDeclaration)methodScope.referenceContext()).binding;

			boolean shouldReport = !method.isMain();
			if (method.isImplementing()) {
				shouldReport &= currentScope.compilerOptions().reportUnusedParameterWhenImplementingAbstract;
			} else if (method.isOverriding()) {
				shouldReport &= currentScope.compilerOptions().reportUnusedParameterWhenOverridingConcrete;
			}

			if (shouldReport) {
				// report the case of an argument that is unread except through a special operator
				currentScope.problemReporter().unusedArgument(localBinding.declaration);
			}
		}
	} else {
		// report the case of a local variable that is unread except for a special operator
		currentScope.problemReporter().unusedLocalVariable(localBinding.declaration);
	}
}
}
