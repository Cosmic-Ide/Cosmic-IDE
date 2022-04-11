/*******************************************************************************
 * Copyright (c) 2013, 2014 IBM Corporation and others.
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
 *								Bug 438458 - [1.8][null] clean up handling of null type annotations wrt type variables
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.lookup;

// Give it an identity of its own to discriminate the fact that this type is not annotatable and so is a singleton.
public class VoidTypeBinding extends BaseTypeBinding {

	VoidTypeBinding() {
		super(TypeIds.T_void, TypeConstants.VOID, new char[] { 'V' });
	}

	@Override
	public TypeBinding clone(TypeBinding enclosingType) {
		return this;  // enforce solitude.
	}

	@Override
	public void setTypeAnnotations(AnnotationBinding[] annotations, boolean evalNullAnnotations) {
		return; // reject misguided attempt.
	}

	@Override
	public TypeBinding unannotated() {
		return this;
	}
}
