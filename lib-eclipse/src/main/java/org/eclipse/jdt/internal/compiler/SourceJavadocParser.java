/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
package org.eclipse.jdt.internal.compiler;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.parser.JavadocParser;
import org.eclipse.jdt.internal.compiler.parser.Parser;
import org.eclipse.jdt.internal.compiler.parser.ScannerHelper;
import org.eclipse.jdt.internal.compiler.parser.TerminalTokens;

public class SourceJavadocParser extends JavadocParser {

	// Store categories identifiers parsed in javadoc
	int categoriesPtr = -1;
	char[][] categories = CharOperation.NO_CHAR_CHAR;

public SourceJavadocParser(Parser sourceParser) {
	super(sourceParser);
	this.kind = SOURCE_PARSER | TEXT_VERIF;
}

@Override
public boolean checkDeprecation(int commentPtr) {
	this.categoriesPtr = -1;
	boolean result = super.checkDeprecation(commentPtr);
	if (this.categoriesPtr > -1) {
		System.arraycopy(this.categories, 0, this.categories = new char[this.categoriesPtr+1][], 0, this.categoriesPtr+1);
	} else {
		this.categories = CharOperation.NO_CHAR_CHAR;
	}
	return result;
}

@Override
protected boolean parseIdentifierTag(boolean report) {
	int end = this.lineEnd+1;
	if (super.parseIdentifierTag(report) && this.index <= end) {
		if (this.tagValue == TAG_CATEGORY_VALUE) {
			// Store first category id
			int length = this.categories.length;
			if (++this.categoriesPtr >= length) {
				System.arraycopy(this.categories, 0, this.categories = new char[length+5][], 0, length);
				length += 5;
			}
			this.categories[this.categoriesPtr] = this.identifierStack[this.identifierPtr--];
			// Store optional additional category identifiers
			consumeToken();
			while (this.index < end) {
				if (readTokenSafely() == TerminalTokens.TokenNameIdentifier && (this.scanner.currentCharacter == ' ' || ScannerHelper.isWhitespace(this.scanner.currentCharacter))) {
					if (this.index > (this.lineEnd+1)) break;
					// valid additional identifier
					if (++this.categoriesPtr >= length) {
						System.arraycopy(this.categories, 0, this.categories = new char[length+5][], 0, length);
						length += 5;
					}
					this.categories[this.categoriesPtr] = this.scanner.getCurrentIdentifierSource();
					consumeToken();
				} else {
					// TODO (frederic) raise warning for invalid syntax when javadoc spec will be finalized...
					break;
				}
			}
			// Reset position to end of line
			this.index = end;
			this.scanner.currentPosition = end;
			consumeToken();
		}
		return true;
	}
	return false;
}

@Override
protected void parseSimpleTag() {

	// Read first char
	// readChar() code is inlined to balance additional method call in checkDeprectation(int)
	char first = this.source[this.index++];
	if (first == '\\' && this.source[this.index] == 'u') {
		int c1, c2, c3, c4;
		int pos = this.index;
		this.index++;
		while (this.source[this.index] == 'u')
			this.index++;
		if (!(((c1 = ScannerHelper.getHexadecimalValue(this.source[this.index++])) > 15 || c1 < 0)
				|| ((c2 = ScannerHelper.getHexadecimalValue(this.source[this.index++])) > 15 || c2 < 0)
				|| ((c3 = ScannerHelper.getHexadecimalValue(this.source[this.index++])) > 15 || c3 < 0)
				|| ((c4 = ScannerHelper.getHexadecimalValue(this.source[this.index++])) > 15 || c4 < 0))) {
			first = (char) (((c1 * 16 + c2) * 16 + c3) * 16 + c4);
		} else {
			this.index = pos;
		}
	}

	// switch on first tag char
	switch (first) {
		case 'd': // perhaps @deprecated tag?
	        if ((readChar() == 'e') &&
					(readChar() == 'p') && (readChar() == 'r') &&
					(readChar() == 'e') && (readChar() == 'c') &&
					(readChar() == 'a') && (readChar() == 't') &&
					(readChar() == 'e') && (readChar() == 'd')) {
				// ensure the tag is properly ended: either followed by a space, a tab, line end or asterisk.
				char c = readChar();
				if (ScannerHelper.isWhitespace(c) || c == '*') {
					this.tagValue = TAG_DEPRECATED_VALUE;
					this.deprecated = true;
				}
	        }
			break;
		case 'c': // perhaps @category tag?
	        if ((readChar() == 'a') &&
					(readChar() == 't') && (readChar() == 'e') &&
					(readChar() == 'g') && (readChar() == 'o') &&
					(readChar() == 'r') && (readChar() == 'y')) {
				// ensure the tag is properly ended: either followed by a space, a tab, line end or asterisk.
				char c = readChar();
				if (ScannerHelper.isWhitespace(c) || c == '*') {
					this.tagValue = TAG_CATEGORY_VALUE;
					if (this.scanner.source == null) {
						this.scanner.setSource(this.source);
					}
					this.scanner.resetTo(this.index, this.scanner.eofPosition);
					parseIdentifierTag(false); // Do not report missing identifier
				}
	        }
			break;
	}
}

}
