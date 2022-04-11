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
 *								bug 331649 - [compiler][null] consider null annotations for fields
 *								Bug 400874 - [1.8][compiler] Inference infrastructure should evolve to meet JLS8 18.x (Part G of JSR335 spec)
 *								Bug 426996 - [1.8][inference] try to avoid method Expression.unresolve()?
 *     Jesper S Moller - Contributions for
 *							bug 382721 - [1.8][compiler] Effectively final variables needs special treatment
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.ast;

import java.util.function.Predicate;

import org.eclipse.jdt.internal.compiler.lookup.*;
import org.eclipse.jdt.internal.compiler.problem.AbortMethod;

public abstract class NameReference extends Reference implements InvocationSite {

	public Binding binding; //may be aTypeBinding-aFieldBinding-aLocalVariableBinding

	public TypeBinding actualReceiverType;	// modified receiver type - actual one according to namelookup

	//the error printing
	//some name reference are build as name reference but
	//only used as type reference. When it happens, instead of
	//creating a new object (aTypeReference) we just flag a boolean
	//This concesion is valuable while there are cases when the NameReference
	//will be a TypeReference (static message sends.....) and there is
	//no changeClass in java.
public NameReference() {
	this.bits |= Binding.TYPE | Binding.VARIABLE; // restrictiveFlag
}

/**
 * Use this method only when sure that the current reference is <strong>not</strong>
 * a chain of several fields (QualifiedNameReference with more than one field).
 * Otherwise use {@link #lastFieldBinding()}.
 */
@Override
public FieldBinding fieldBinding() {
	//this method should be sent ONLY after a check against isFieldReference()
	//check its use doing senders.........
	return (FieldBinding) this.binding ;
}

@Override
public FieldBinding lastFieldBinding() {
	if ((this.bits & ASTNode.RestrictiveFlagMASK) == Binding.FIELD)
		return fieldBinding(); // most subclasses only refer to one field anyway
	return null;
}

@Override
public InferenceContext18 freshInferenceContext(Scope scope) {
	return null;
}

@Override
public boolean isSuperAccess() {
	return false;
}

@Override
public boolean isTypeAccess() {
	// null is acceptable when we are resolving the first part of a reference
	return this.binding == null || (this.binding.kind() & Binding.TYPE) != 0;
}

@Override
public boolean isTypeReference() {
	return this.binding instanceof ReferenceBinding;
}

@Override
public void setActualReceiverType(ReferenceBinding receiverType) {
	if (receiverType == null) return; // error scenario only
	this.actualReceiverType = receiverType;
}

@Override
public void setDepth(int depth) {
	this.bits &= ~DepthMASK; // flush previous depth if any
	if (depth > 0) {
		this.bits |= (depth & 0xFF) << DepthSHIFT; // encoded on 8 bits
	}
}

@Override
public void setFieldIndex(int index){
	// ignored
}

public abstract String unboundReferenceErrorName();

public abstract char[][] getName();

/* Called during code generation to ensure that outer locals's effectively finality is guaranteed.
   Aborts if constraints are violated. Due to various complexities, this check is not conveniently
   implementable in resolve/analyze phases.
   Another quirk here is this method tells the clients whether the below condition is true
     (this.bits & ASTNode.IsCapturedOuterLocal) != 0
*/
public boolean checkEffectiveFinality(VariableBinding localBinding, Scope scope) {
	Predicate<VariableBinding> test = (local) -> {
		return (!localBinding.isFinal() && !localBinding.isEffectivelyFinal());
	};
	if ((this.bits & ASTNode.IsCapturedOuterLocal) != 0) {
		if (test.test(localBinding)) {
			scope.problemReporter().cannotReferToNonEffectivelyFinalOuterLocal(localBinding, this);
			throw new AbortMethod(scope.referenceCompilationUnit().compilationResult, null);
		}
		return true;
	} else if ((this.bits & ASTNode.IsUsedInPatternGuard) != 0) {
		if (test.test(localBinding)) {
			scope.problemReporter().cannotReferToNonFinalLocalInGuard(localBinding, this);
			throw new AbortMethod(scope.referenceCompilationUnit().compilationResult, null);
		}
	}
	return false;
}

@Override
public boolean isType() {
	return (this.bits & Binding.TYPE) != 0;
}
}
