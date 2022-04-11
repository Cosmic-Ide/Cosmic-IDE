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
 *******************************************************************************/
package org.eclipse.jdt.internal.core.search.indexing;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.parser.Scanner;
import org.eclipse.jdt.internal.compiler.parser.TerminalTokens;
import org.eclipse.jdt.internal.compiler.util.SuffixConstants;

public abstract class BinaryContainer extends IndexRequest {

	Scanner scanner;
	public BinaryContainer(IPath containerPath, IndexManager manager) {
		super(containerPath, manager);
	}

	private boolean isIdentifier() throws InvalidInputException {
		switch(this.scanner.scanIdentifier()) {
			// assert and enum will not be recognized as java identifiers
			// in 1.7 mode, which are in 1.3.
			case TerminalTokens.TokenNameIdentifier:
			case TerminalTokens.TokenNameassert:
			case TerminalTokens.TokenNameenum:
				return true;
			default:
				return false;
		}
	}
	protected boolean isValidPackageNameForClassOrisModule(String className) {
		if (className.substring(0, className.length() - (SuffixConstants.SUFFIX_CLASS.length)).equals(new String(IIndexConstants.MODULE_INFO)))
			return true;
		char[] classNameArray = className.toCharArray();
		// use 1.7 as the source level as there are more valid identifiers in 1.7 mode
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=376673
		if (this.scanner == null)
			this.scanner = new Scanner(false /* comment */, true /* whitespace */, false /* nls */,
					ClassFileConstants.JDK1_7/* sourceLevel */, null/* taskTag */, null/* taskPriorities */, true /* taskCaseSensitive */);

		this.scanner.setSource(classNameArray);
		this.scanner.eofPosition = classNameArray.length - SuffixConstants.SUFFIX_CLASS.length;
		try {
			if (isIdentifier()) {
				while (this.scanner.eofPosition > this.scanner.currentPosition) {
					if (this.scanner.getNextChar() != '/' || this.scanner.eofPosition <= this.scanner.currentPosition) {
						return false;
					}
					if (!isIdentifier()) return false;
				}
				return true;
			}
		} catch (InvalidInputException e) {
			// invalid class name
		}
		return false;
	}
}
