/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.classfmt;

import org.eclipse.jdt.internal.compiler.env.IBinaryTypeAnnotation;

public final class ComponentInfoWithTypeAnnotation extends ComponentInfoWithAnnotation {
	private TypeAnnotationInfo[] typeAnnotations;

ComponentInfoWithTypeAnnotation(RecordComponentInfo info, AnnotationInfo[] annos, TypeAnnotationInfo[] typeAnnos) {
	super(info, annos);
	this.typeAnnotations = typeAnnos;
}
@Override
public IBinaryTypeAnnotation[] getTypeAnnotations() {
	return this.typeAnnotations;
}
@Override
protected void initialize() {
	for (int i = 0, max = this.typeAnnotations.length; i < max; i++)
		this.typeAnnotations[i].initialize();
	super.initialize();
}
@Override
protected void reset() {
	if (this.typeAnnotations != null)
		for (int i = 0, max = this.typeAnnotations.length; i < max; i++)
			this.typeAnnotations[i].reset();
	super.reset();
}
@Override
public String toString() {
	StringBuffer buffer = new StringBuffer(getClass().getName());
	if (this.typeAnnotations != null) {
		buffer.append('\n');
		buffer.append("type annotations:"); //$NON-NLS-1$
		for (int i = 0; i < this.typeAnnotations.length; i++) {
			buffer.append(this.typeAnnotations[i]);
			buffer.append('\n');
		}
	}
	toStringContent(buffer);
	return buffer.toString();
}
}
