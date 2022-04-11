/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
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
 *
 *******************************************************************************/
package org.eclipse.jdt.internal.codeassist.complete;

import org.eclipse.jdt.internal.compiler.ast.ExportsStatement;
import org.eclipse.jdt.internal.compiler.ast.ImportReference;

/**
 *
 * This class is independent of its parent class and is in fact a dummy ExportsStatement. Used to hook
 * into the existing module declaration type and is used as a placeholder for keyword completion. This can
 * be any module keyword completion and not necessarily related to exports statement.
 */
public class CompletionOnKeywordModuleInfo extends ExportsStatement implements CompletionOnKeyword {
	private char[] token;
	private char[][] possibleKeywords;

	public CompletionOnKeywordModuleInfo(char[] token, long pos, char[][] possibleKeywords) {
		super(new ImportReference(new char[][] {token}, new long[] {pos}, false, 0), null); // dummy
		this.token = token;
		this.possibleKeywords = possibleKeywords;
		this.sourceStart = (int) (pos>>>32)  ;
		this.sourceEnd = (int) (pos & 0x00000000FFFFFFFFL);
	}

	@Override
	public char[] getToken() {
		return this.token;
	}

	@Override
	public char[][] getPossibleKeywords() {
		return this.possibleKeywords;
	}
}
