/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.lookup;

import org.eclipse.jdt.core.compiler.CharOperation;

public final class MemberTypeBinding extends NestedTypeBinding {

public MemberTypeBinding(char[][] compoundName, ClassScope scope, SourceTypeBinding enclosingType) {
	super(compoundName, scope, enclosingType);
	this.tagBits |= TagBits.MemberTypeMask;
}

public MemberTypeBinding(MemberTypeBinding prototype) {
	super(prototype);
}

void checkSyntheticArgsAndFields() {
	if (!isPrototype()) throw new IllegalStateException();
	if (isStatic()) return;
	if (isInterface()) return;
	if (!isPrototype()) {
		((MemberTypeBinding) this.prototype).checkSyntheticArgsAndFields();
		return;
	}
	this.addSyntheticArgumentAndField(this.enclosingType);
}
/* Answer the receiver's constant pool name.
*
* NOTE: This method should only be used during/after code gen.
*/

@Override
public char[] constantPoolName() /* java/lang/Object */ {

	if (this.constantPoolName != null)
		return this.constantPoolName;

	if (!isPrototype()) {
		return this.prototype.constantPoolName();
	}

	return this.constantPoolName = CharOperation.concat(enclosingType().constantPoolName(), this.sourceName, '$');
}

@Override
public TypeBinding clone(TypeBinding outerType) {
	MemberTypeBinding copy = new MemberTypeBinding(this);
	copy.enclosingType = (SourceTypeBinding) outerType;
	return copy;
}

/**
 * @see org.eclipse.jdt.internal.compiler.lookup.Binding#initializeDeprecatedAnnotationTagBits()
 */
@Override
public void initializeDeprecatedAnnotationTagBits() {
	if (!isPrototype()) {
		this.prototype.initializeDeprecatedAnnotationTagBits();
		return;
	}
	if ((this.tagBits & TagBits.DeprecatedAnnotationResolved) == 0) {
		super.initializeDeprecatedAnnotationTagBits();
		if ((this.tagBits & TagBits.AnnotationDeprecated) == 0) {
			// check enclosing type
			ReferenceBinding enclosing;
			if (((enclosing = enclosingType()).tagBits & TagBits.DeprecatedAnnotationResolved) == 0) {
				enclosing.initializeDeprecatedAnnotationTagBits();
			}
			if (enclosing.isViewedAsDeprecated()) {
				this.modifiers |= ExtraCompilerModifiers.AccDeprecatedImplicitly;
				this.tagBits |= (enclosing.tagBits & TagBits.AnnotationTerminallyDeprecated);
			}
		}
	}
}
@Override
public String toString() {
	if (this.hasTypeAnnotations()) {
		return annotatedDebugName();
    } else {
    	return "Member type : " + new String(sourceName()) + " " + super.toString(); //$NON-NLS-2$ //$NON-NLS-1$
    }
}
@Override
public ModuleBinding module() {
	return this.enclosingType.module();
}
}
