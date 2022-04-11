/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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

import org.eclipse.jdt.internal.compiler.ast.JavadocSingleNameReference;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;

public class CompletionOnJavadocParamNameReference extends JavadocSingleNameReference implements CompletionOnJavadoc {
	public int completionFlags = JAVADOC;
	public char[][] missingParams;
	public char[][] missingTypeParams;

	public CompletionOnJavadocParamNameReference(char[] name, long pos, int start, int end) {
		super(name, pos, start, end);
	}

	public CompletionOnJavadocParamNameReference(JavadocSingleNameReference nameRef) {
		super(nameRef.token, (((long)nameRef.sourceStart)<<32)+nameRef.sourceEnd, nameRef.tagSourceStart, nameRef.tagSourceStart);
	}

	@Override
	public void addCompletionFlags(int flags) {
		this.completionFlags |= flags;
	}

	@Override
	public int getCompletionFlags() {
		return this.completionFlags;
	}

	@Override
	public StringBuffer printExpression(int indent, StringBuffer output) {
		output.append("<CompletionOnJavadocParamNameReference:"); //$NON-NLS-1$
		if (this.token != null) super.printExpression(indent, output);
		return output.append('>');
	}

	@Override
	public TypeBinding reportError(BlockScope scope) {
		return null;
	}
}
