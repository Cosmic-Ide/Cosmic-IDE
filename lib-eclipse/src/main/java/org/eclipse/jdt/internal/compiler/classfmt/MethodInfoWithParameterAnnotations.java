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
 *    tyeung@bea.com  - initial API and implementation
 *    IBM Corporation - fix for bug 342757
 *    Stephan Herrmann - Contribution for bug 186342 - [compiler][null] Using annotations for null checking
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.classfmt;

import org.eclipse.jdt.internal.compiler.env.IBinaryAnnotation;

class MethodInfoWithParameterAnnotations extends MethodInfoWithAnnotations {
	private AnnotationInfo[][] parameterAnnotations;

MethodInfoWithParameterAnnotations(MethodInfo methodInfo, AnnotationInfo[] annotations, AnnotationInfo[][] parameterAnnotations) {
	super(methodInfo, annotations);
	this.parameterAnnotations = parameterAnnotations;
}
@Override
public IBinaryAnnotation[] getParameterAnnotations(int index, char[] classFileName) {
	try {
		return this.parameterAnnotations == null ? null : this.parameterAnnotations[index];
	} catch (ArrayIndexOutOfBoundsException aioobe) {
		// detailed reporting to track down https://bugs.eclipse.org/474081
		StringBuilder message = new StringBuilder("Mismatching number of parameter annotations, "); //$NON-NLS-1$
		message.append(index);
		message.append('>');
		message.append(this.parameterAnnotations.length-1);
		message.append(" in "); //$NON-NLS-1$
		message.append(getSelector());
		char[] desc = getGenericSignature();
		if (desc != null)
			message.append(desc);
		else
			message.append(getMethodDescriptor());
		if (classFileName != null)
			message.append(" in ").append(classFileName); //$NON-NLS-1$
		throw new IllegalStateException(message.toString(), aioobe);
	}
}
@Override
public int getAnnotatedParametersCount() {
	return this.parameterAnnotations == null ? 0 : this.parameterAnnotations.length;
}
@Override
protected void initialize() {
	for (int i = 0, l = this.parameterAnnotations == null ? 0 : this.parameterAnnotations.length; i < l; i++) {
		AnnotationInfo[] infos = this.parameterAnnotations[i];
		for (int j = 0, k = infos == null ? 0 : infos.length; j < k; j++)
			infos[j].initialize();
	}
	super.initialize();
}
@Override
protected void reset() {
	for (int i = 0, l = this.parameterAnnotations == null ? 0 : this.parameterAnnotations.length; i < l; i++) {
		AnnotationInfo[] infos = this.parameterAnnotations[i];
		for (int j = 0, k = infos == null ? 0 : infos.length; j < k; j++)
			infos[j].reset();
	}
	super.reset();
}
}
