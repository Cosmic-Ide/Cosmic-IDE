/*******************************************************************************
 * Copyright (c) 2005, 2016 BEA Systems, Inc.
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

public class AnnotationMethodInfoWithAnnotations extends AnnotationMethodInfo {
	private AnnotationInfo[] annotations;

AnnotationMethodInfoWithAnnotations(MethodInfo methodInfo, Object defaultValue, AnnotationInfo[] annotations) {
	super(methodInfo, defaultValue);
	this.annotations = annotations;
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
