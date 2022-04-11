/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others.
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
package org.eclipse.jdt.internal.codeassist.complete;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;

public class AssistNodeParentAnnotationArrayInitializer extends ASTNode {
	public final TypeReference type;
	public final char[] name;
	public AssistNodeParentAnnotationArrayInitializer(TypeReference type, char[] name) {
		this.type = type;
		this.name = name;
	}

	@Override
	public StringBuffer print(int indent, StringBuffer output) {
		output.append("<AssistNodeParentAnnotationArrayInitializer:"); //$NON-NLS-1$
		output.append('@');
		this.type.printExpression(0, output);
		output.append('(');
		output.append(this.name);
		output.append(')');
		output.append('>');

		return output;
	}
}
