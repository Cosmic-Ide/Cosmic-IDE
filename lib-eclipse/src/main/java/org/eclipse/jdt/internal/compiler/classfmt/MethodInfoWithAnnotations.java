/*******************************************************************************
 * Copyright (c) 2005, 2018 BEA Systems, Inc and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    tyeung@bea.com - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.classfmt;

import org.eclipse.jdt.internal.compiler.env.IBinaryAnnotation;

public class MethodInfoWithAnnotations extends MethodInfo {
	protected AnnotationInfo[] annotations;

MethodInfoWithAnnotations(MethodInfo methodInfo, AnnotationInfo[] annotations) {
	super(methodInfo.reference, methodInfo.constantPoolOffsets, methodInfo.structOffset, methodInfo.version);
	this.annotations = annotations;

	this.accessFlags = methodInfo.accessFlags;
	this.attributeBytes = methodInfo.attributeBytes;
	this.descriptor = methodInfo.descriptor;
	this.exceptionNames = methodInfo.exceptionNames;
	this.name = methodInfo.name;
	this.signature = methodInfo.signature;
	this.signatureUtf8Offset = methodInfo.signatureUtf8Offset;
	this.tagBits = methodInfo.tagBits;
}
@Override
public IBinaryAnnotation[] getAnnotations() {
	return this.annotations;
}
@Override
protected void initialize() {
	for (int i = 0, l = this.annotations == null ? 0 : this.annotations.length; i < l; i++)
		if (this.annotations[i] != null)
			this.annotations[i].initialize();
	super.initialize();
}
@Override
protected void reset() {
	for (int i = 0, l = this.annotations == null ? 0 : this.annotations.length; i < l; i++)
		if (this.annotations[i] != null)
			this.annotations[i].reset();
	super.reset();
}
}
