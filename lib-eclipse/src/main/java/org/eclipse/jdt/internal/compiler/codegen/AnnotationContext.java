/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
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
 *        Andy Clement (GoPivotal, Inc) aclement@gopivotal.com - Contributions for
 *                          Bug 383624 - [1.8][compiler] Revive code generation support for type annotations (from Olivier's work)
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.codegen;

import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.Wildcard;
import org.eclipse.jdt.internal.compiler.lookup.LocalVariableBinding;

public class AnnotationContext {
	public static final int VISIBLE = 0x1;
	public static final int INVISIBLE = 0x2;
	public Annotation annotation;
	public Expression typeReference;
	public int targetType;
	public int info;
	public int info2;
	public int visibility;
	public LocalVariableBinding variableBinding;
	public Wildcard wildcard;

	public AnnotationContext(
			Annotation annotation,
			Expression typeReference,
			int targetType,
			int visibility) {
		this.annotation = annotation;
		this.typeReference = typeReference;
		this.targetType = targetType;
		this.visibility = visibility;
	}

	@Override
	public String toString() {
		return "AnnotationContext [annotation=" //$NON-NLS-1$
				+ this.annotation
				+ ", typeReference=" //$NON-NLS-1$
				+ this.typeReference
				+ ", targetType=" //$NON-NLS-1$
				+ this.targetType
				+ ", info =" //$NON-NLS-1$
				+ this.info
				+ ", boundIndex=" //$NON-NLS-1$
				+ this.info2
				+ "]"; //$NON-NLS-1$
	}
}
