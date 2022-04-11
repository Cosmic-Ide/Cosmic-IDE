/*******************************************************************************
 * Copyright (c) 2016 GoPivotal, Inc.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *        Andy Clement (GoPivotal, Inc) aclement@gopivotal.com - Contributions for
 *            Bug 407191 - [1.8] Binary access support for type annotations
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.classfmt;

import org.eclipse.jdt.internal.compiler.env.IBinaryTypeAnnotation;

class MethodInfoWithTypeAnnotations extends MethodInfoWithParameterAnnotations {
	private TypeAnnotationInfo[] typeAnnotations;

MethodInfoWithTypeAnnotations(MethodInfo methodInfo, AnnotationInfo[] annotations, AnnotationInfo[][] parameterAnnotations, TypeAnnotationInfo[] typeAnnotations) {
	super(methodInfo, annotations, parameterAnnotations);
	this.typeAnnotations = typeAnnotations;
}
@Override
public IBinaryTypeAnnotation[] getTypeAnnotations() {
	return this.typeAnnotations;
}

@Override
protected void initialize() {
	for (int i = 0, l = this.typeAnnotations == null ? 0 : this.typeAnnotations.length; i < l; i++) {
		this.typeAnnotations[i].initialize();
	}
	super.initialize();
}
@Override
protected void reset() {
	for (int i = 0, l = this.typeAnnotations == null ? 0 : this.typeAnnotations.length; i < l; i++) {
		this.typeAnnotations[i].reset();
	}
	super.reset();
}
}
