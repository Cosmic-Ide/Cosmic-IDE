/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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

import org.eclipse.jdt.internal.compiler.ast.SingleTypeReference;
import org.eclipse.jdt.internal.compiler.lookup.Scope;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;

public class CompletionOnKeyword1 extends SingleTypeReference implements CompletionOnKeyword {
	private char[][] possibleKeywords;

	public CompletionOnKeyword1(char[] token, long pos, char[] possibleKeyword) {
		this(token, pos, new char[][]{possibleKeyword});
	}
	public CompletionOnKeyword1(char[] token, long pos, char[][] possibleKeywords) {
		super(token, pos);
		this.possibleKeywords = possibleKeywords;
	}
	@Override
	public char[] getToken() {
		return this.token;
	}
	@Override
	public char[][] getPossibleKeywords() {
		return this.possibleKeywords;
	}
	@Override
	public void aboutToResolve(Scope scope) {
		getTypeBinding(scope);
	}
	@Override
	protected TypeBinding getTypeBinding(Scope scope) {
		throw new CompletionNodeFound(this, scope);
	}
	@Override
	public StringBuffer printExpression(int indent, StringBuffer output){

		return output.append("<CompleteOnKeyword:").append(this.token).append('>');  //$NON-NLS-1$
	}
}
