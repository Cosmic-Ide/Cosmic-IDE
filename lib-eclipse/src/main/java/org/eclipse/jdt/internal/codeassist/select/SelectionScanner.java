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
package org.eclipse.jdt.internal.codeassist.select;

/*
 * Scanner aware of a selection range. If finding an identifier which source range is exactly
 * the same, then will record it so that the parser can make use of it.
 *
 * Source positions are zero-based and inclusive.
 */
import org.eclipse.jdt.internal.compiler.parser.Scanner;

public class SelectionScanner extends Scanner {

	public char[] selectionIdentifier;
	public int selectionStart, selectionEnd;
/*
 * Truncate the current identifier if it is containing the cursor location. Since completion is performed
 * on an identifier prefix.
 *
 */

public SelectionScanner(long sourceLevel, boolean previewEnabled) {
	super(false /*comment*/, false /*whitespace*/, false /*nls*/, sourceLevel, null /*taskTags*/, null/*taskPriorities*/, true/*taskCaseSensitive*/, previewEnabled);
}

@Override
protected boolean isAtAssistIdentifier() {
	return this.selectionStart == this.startPosition && this.selectionEnd == this.currentPosition - 1;
}

@Override
public char[] getCurrentIdentifierSource() {

	if (this.selectionIdentifier == null){
		if (this.selectionStart == this.startPosition && this.selectionEnd == this.currentPosition-1){
			if (this.withoutUnicodePtr != 0){			// check unicode scenario
				System.arraycopy(this.withoutUnicodeBuffer, 1, this.selectionIdentifier = new char[this.withoutUnicodePtr], 0, this.withoutUnicodePtr);
			} else {
				int length = this.currentPosition - this.startPosition;
				// no char[] sharing around completionIdentifier, we want it to be unique so as to use identity checks
				System.arraycopy(this.source, this.startPosition, (this.selectionIdentifier = new char[length]), 0, length);
			}
			return this.selectionIdentifier;
		}
	}
	return super.getCurrentIdentifierSource();
}
/*
 * In case we actually read a keyword which corresponds to the selected
 * range, we pretend we read an identifier.
 */
@Override
public int scanIdentifierOrKeyword() {

	int id = super.scanIdentifierOrKeyword();

	// convert completed keyword into an identifier
	if (id != TokenNameIdentifier
		&& this.startPosition == this.selectionStart
		&& this.currentPosition == this.selectionEnd+1){
		return TokenNameIdentifier;
	}
	return id;
}
}
