/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
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

import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;

public class CompletionOnAnnotationOfType extends TypeDeclaration {
	public ASTNode potentialAnnotatedNode;
	// During recovery a parameter can be parsed as a FieldDeclaration instead of Argument.
	// 'isParameter' is set to true in this case.
	public boolean isParameter;

	public CompletionOnAnnotationOfType(char[] typeName, CompilationResult compilationResult, Annotation annotation){
		super(compilationResult);
		this.sourceEnd = annotation.sourceEnd;
		this.sourceStart = annotation.sourceEnd;
		this.name = typeName;
		this.annotations = new Annotation[]{annotation};
	}

	@Override
	public StringBuffer print(int indent, StringBuffer output) {
		return this.annotations[0].print(indent, output);
	}
}
