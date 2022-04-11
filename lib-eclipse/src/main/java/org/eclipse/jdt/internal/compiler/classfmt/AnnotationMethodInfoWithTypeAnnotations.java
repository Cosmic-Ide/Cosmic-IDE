/*******************************************************************************
 * Copyright (c) 2016 Till Brychcy and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Till Brychcy - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.classfmt;

import org.eclipse.jdt.internal.compiler.env.IBinaryTypeAnnotation;

class AnnotationMethodInfoWithTypeAnnotations extends AnnotationMethodInfoWithAnnotations {
	private TypeAnnotationInfo[] typeAnnotations;

AnnotationMethodInfoWithTypeAnnotations(MethodInfo methodInfo, Object defaultValue, AnnotationInfo[] annotations, TypeAnnotationInfo[] typeAnnotations) {
	super(methodInfo, defaultValue, annotations);
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
