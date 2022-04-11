/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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

import org.eclipse.jdt.internal.compiler.ast.SingleNameReference;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;

public class CompletionOnKeyword3 extends SingleNameReference implements CompletionOnKeyword {
	private char[][] possibleKeywords;
	private boolean tryOrCatch;
	public CompletionOnKeyword3(char[] token, long pos, char[] possibleKeyword) {
		this(token, pos, new char[][]{possibleKeyword}, false);
	}
	public CompletionOnKeyword3(char[] token, long pos, char[][] possibleKeywords, boolean afterTryOrCatch) {
		super(token, pos);
		this.token = token;
		this.possibleKeywords = possibleKeywords;
		this.tryOrCatch = afterTryOrCatch;
	}
	@Override
	public char[] getToken() {
		return this.token;
	}
	@Override
	public char[][] getPossibleKeywords() {
		return this.possibleKeywords;
	}
	public boolean afterTryOrCatch() {
		return this.tryOrCatch;
	}
	@Override
	public StringBuffer printExpression(int indent, StringBuffer output) {

		return output.append("<CompleteOnKeyword:").append(this.token).append('>'); //$NON-NLS-1$
	}
	@Override
	public TypeBinding resolveType(BlockScope scope) {
		throw new CompletionNodeFound(this, scope);
	}
}
