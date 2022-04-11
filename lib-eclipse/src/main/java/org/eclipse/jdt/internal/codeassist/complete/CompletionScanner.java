/*******************************************************************************
 * Copyright (c) 2000, 2021 IBM Corporation and others.
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

/*
 * Scanner aware of a cursor location so as to discard trailing portions of identifiers
 * containing the cursor location.
 *
 * Cursor location denotes the position of the last character behind which completion
 * got requested:
 *  -1 means completion at the very beginning of the source
 *	0  means completion behind the first character
 *  n  means completion behind the n-th character
 */
import org.eclipse.jdt.core.compiler.*;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.parser.Scanner;
import org.eclipse.jdt.internal.compiler.parser.ScannerHelper;
import org.eclipse.jdt.internal.compiler.parser.TerminalTokens;

public class CompletionScanner extends Scanner {

	public char[] completionIdentifier;
	public int cursorLocation;
	public int endOfEmptyToken = -1;

	/* Source positions of the completedIdentifier
	 * if inside actual identifier, end goes to the actual identifier
	 * end, in other words, beyond cursor location
	 */
	public int completedIdentifierStart = 0;
	public int completedIdentifierEnd = -1;
	public int unicodeCharSize;

	public static final char[] EmptyCompletionIdentifier = {};

public CompletionScanner(long sourceLevel) {
	this(
		sourceLevel,
		false /* previewEnabled */);
}
public CompletionScanner(long sourceLevel, boolean previewEnabled) {
	super(
		false /*comment*/,
		false /*whitespace*/,
		false /*nls*/,
		sourceLevel,
		null /*taskTags*/,
		null/*taskPriorities*/,
		true/*taskCaseSensitive*/,
		previewEnabled);
}
@Override
protected boolean isAtAssistIdentifier() {
	if (this.cursorLocation < this.startPosition && this.currentPosition == this.startPosition) { // fake empty identifier got issued
		return true;
	}
	if (this.cursorLocation+1 >= this.startPosition && this.cursorLocation < this.currentPosition) {
		return true;
	}
	return false;
}
/*
 * Truncate the current identifier if it is containing the cursor location. Since completion is performed
 * on an identifier prefix.
 *
 */
@Override
public char[] getCurrentIdentifierSource() {

	if (this.completionIdentifier == null){
		if (this.cursorLocation < this.startPosition && this.currentPosition == this.startPosition){ // fake empty identifier got issued
			// remember actual identifier positions
			this.completedIdentifierStart = this.startPosition;
			this.completedIdentifierEnd = this.completedIdentifierStart - 1;
			return this.completionIdentifier = EmptyCompletionIdentifier;
		}
		if (this.cursorLocation+1 >= this.startPosition && this.cursorLocation < this.currentPosition){
			// remember actual identifier positions
			this.completedIdentifierStart = this.startPosition;
			this.completedIdentifierEnd = this.currentPosition - 1;
			if (this.withoutUnicodePtr != 0){			// check unicode scenario
				int length = this.cursorLocation + 1 - this.startPosition - this.unicodeCharSize;
				System.arraycopy(this.withoutUnicodeBuffer, 1, this.completionIdentifier = new char[length], 0, length);
			} else {
				// no char[] sharing around completionIdentifier, we want it to be unique so as to use identity checks
				int length = this.cursorLocation + 1 - this.startPosition;
				System.arraycopy(this.source, this.startPosition, (this.completionIdentifier = new char[length]), 0, length);
			}
			return this.completionIdentifier;
		}
	}
	return super.getCurrentIdentifierSource();
}

@Override
public char[] getCurrentTokenSourceString() {
	if (this.completionIdentifier == null){
		if (this.cursorLocation+1 >= this.startPosition && this.cursorLocation < this.currentPosition){
			// remember actual identifier positions
			this.completedIdentifierStart = this.startPosition;
			this.completedIdentifierEnd = this.currentPosition - 1;
			if (this.withoutUnicodePtr != 0){			// check unicode scenario
				int length = this.cursorLocation - this.startPosition - this.unicodeCharSize;
				System.arraycopy(this.withoutUnicodeBuffer, 2, this.completionIdentifier = new char[length], 0, length);
			} else {
				// no char[] sharing around completionIdentifier, we want it to be unique so as to use identity checks
				int length = this.cursorLocation - this.startPosition;
				System.arraycopy(this.source, this.startPosition + 1, (this.completionIdentifier = new char[length]), 0, length);
			}
			return this.completionIdentifier;
		}
	}
	return super.getCurrentTokenSourceString();
}
@Override
protected int getNextToken0() throws InvalidInputException {

	this.wasAcr = false;
	this.unicodeCharSize = 0;
	if (this.diet) {
		jumpOverMethodBody();
		this.diet = false;
		return this.currentPosition > this.eofPosition ? TokenNameEOF : TokenNameRBRACE;
	}
	int whiteStart = 0;
	try {
		while (true) { //loop for jumping over comments
			this.withoutUnicodePtr = 0;
			//start with a new token (even comment written with unicode )

			// ---------Consume white space and handles start position---------
			whiteStart = this.currentPosition;
			boolean isWhiteSpace, hasWhiteSpaces = false;
			int offset = 0;
			do {
				this.startPosition = this.currentPosition;
				boolean checkIfUnicode = false;
				try {
					checkIfUnicode = ((this.currentCharacter = this.source[this.currentPosition++]) == '\\')
						&& (this.source[this.currentPosition] == 'u');
				} catch(IndexOutOfBoundsException e) {
					if (this.tokenizeWhiteSpace && (whiteStart != this.currentPosition - 1)) {
						// reposition scanner in case we are interested by spaces as tokens
						this.currentPosition--;
						this.startPosition = whiteStart;
						return TokenNameWHITESPACE;
					}
					if (this.currentPosition > this.eofPosition) {
						/* might be completing at eof (e.g. behind a dot) */
						if (this.completionIdentifier == null &&
							this.startPosition == this.cursorLocation + 1){
							this.currentPosition = this.startPosition; // for being detected as empty free identifier
							return TokenNameIdentifier;
						}
						return TokenNameEOF;
					}
				}
				if (checkIfUnicode) {
					isWhiteSpace = jumpOverUnicodeWhiteSpace();
					offset = 6;
				} else {
					offset = 1;
					if ((this.currentCharacter == '\r') || (this.currentCharacter == '\n')) {
						//checkNonExternalizedString();
						if (this.recordLineSeparator) {
							pushLineSeparator();
						}
					}
					isWhiteSpace =
						(this.currentCharacter == ' ') || CharOperation.isWhitespace(this.currentCharacter);
				}
				if (isWhiteSpace) {
					hasWhiteSpaces = true;
				}
				/* completion requesting strictly inside blanks */
				if ((whiteStart != this.currentPosition)
					//&& (previousToken == TokenNameDOT)
					&& (this.completionIdentifier == null)
					&& (whiteStart <= this.cursorLocation+1)
					&& (this.cursorLocation < this.startPosition)
					&& !ScannerHelper.isJavaIdentifierStart(this.complianceLevel, this.currentCharacter)){
					this.currentPosition = this.startPosition; // for next token read
					return TokenNameIdentifier;
				}
			} while (isWhiteSpace);
			if (this.tokenizeWhiteSpace && hasWhiteSpaces) {
				// reposition scanner in case we are interested by spaces as tokens
				this.currentPosition-=offset;
				this.startPosition = whiteStart;
				return TokenNameWHITESPACE;
			}
			//little trick to get out in the middle of a source computation
			if (this.currentPosition > this.eofPosition){
				/* might be completing at eof (e.g. behind a dot) */
				if (this.completionIdentifier == null &&
					this.startPosition == this.cursorLocation + 1){
					// compute end of empty identifier.
					// if the empty identifier is at the start of a next token the end of
					// empty identifier is the end of the next token (e.g. "<empty token>next").
					int temp = this.eofPosition;
					this.eofPosition = this.source.length;
				 	while(getNextCharAsJavaIdentifierPart()){/*empty*/}
				 	this.eofPosition = temp;
				 	this.endOfEmptyToken = this.currentPosition - 1;
					this.currentPosition = this.startPosition; // for being detected as empty free identifier
					return TokenNameIdentifier;
				}
				this.currentPosition = this.startPosition; // fake EOF should not drown the real next token.
				return TokenNameEOF;
			}

			// ---------Identify the next token-------------

			switch (this.currentCharacter) {
				case '@' :
					return TokenNameAT;
				case '(' :
					return TokenNameLPAREN;
				case ')' :
					return TokenNameRPAREN;
				case '{' :
					return TokenNameLBRACE;
				case '}' :
					return TokenNameRBRACE;
				case '[' :
					return TokenNameLBRACKET;
				case ']' :
					return TokenNameRBRACKET;
				case ';' :
					return TokenNameSEMICOLON;
				case ',' :
					return TokenNameCOMMA;
				case '.' :
					if (this.startPosition <= this.cursorLocation
							&& this.cursorLocation < this.currentPosition){
						return TokenNameDOT; // completion inside .<|>12
					}
					if (getNextCharAsDigit()) {
						return scanNumber(true);
					}
					int temp = this.currentPosition;
					if (getNextChar('.')) {
						if (getNextChar('.')) {
							return TokenNameELLIPSIS;
						} else {
							this.currentPosition = temp;
							return TokenNameDOT;
						}
					} else {
						this.currentPosition = temp;
						return TokenNameDOT;
					}
				case '+' :
					{
						int test;
						if ((test = getNextChar('+', '=')) == 0)
							return TokenNamePLUS_PLUS;
						if (test > 0)
							return TokenNamePLUS_EQUAL;
						return TokenNamePLUS;
					}
				case '-' :
					{
						int test;
						if ((test = getNextChar('-', '=')) == 0)
							return TokenNameMINUS_MINUS;
						if (test > 0)
							return TokenNameMINUS_EQUAL;
						if (getNextChar('>'))
							return TokenNameARROW;
						return TokenNameMINUS;
					}
				case '~' :
					return TokenNameTWIDDLE;
				case '!' :
					if (getNextChar('='))
						return TokenNameNOT_EQUAL;
					return TokenNameNOT;
				case '*' :
					if (getNextChar('='))
						return TokenNameMULTIPLY_EQUAL;
					return TokenNameMULTIPLY;
				case '%' :
					if (getNextChar('='))
						return TokenNameREMAINDER_EQUAL;
					return TokenNameREMAINDER;
				case '<' :
					{
						int test;
						if ((test = getNextChar('=', '<')) == 0)
							return TokenNameLESS_EQUAL;
						if (test > 0) {
							if (getNextChar('='))
								return TokenNameLEFT_SHIFT_EQUAL;
							return TokenNameLEFT_SHIFT;
						}
						return TokenNameLESS;
					}
				case '>' :
					{
						int test;
						if (this.returnOnlyGreater) {
							return TokenNameGREATER;
						}
						if ((test = getNextChar('=', '>')) == 0)
							return TokenNameGREATER_EQUAL;
						if (test > 0) {
							if ((test = getNextChar('=', '>')) == 0)
								return TokenNameRIGHT_SHIFT_EQUAL;
							if (test > 0) {
								if (getNextChar('='))
									return TokenNameUNSIGNED_RIGHT_SHIFT_EQUAL;
								return TokenNameUNSIGNED_RIGHT_SHIFT;
							}
							return TokenNameRIGHT_SHIFT;
						}
						return TokenNameGREATER;
					}
				case '=' :
					if (getNextChar('='))
						return TokenNameEQUAL_EQUAL;
					return TokenNameEQUAL;
				case '&' :
					{
						int test;
						if ((test = getNextChar('&', '=')) == 0)
							return TokenNameAND_AND;
						if (test > 0)
							return TokenNameAND_EQUAL;
						return TokenNameAND;
					}
				case '|' :
					{
						int test;
						if ((test = getNextChar('|', '=')) == 0)
							return TokenNameOR_OR;
						if (test > 0)
							return TokenNameOR_EQUAL;
						return TokenNameOR;
					}
				case '^' :
					if (getNextChar('='))
						return TokenNameXOR_EQUAL;
					return TokenNameXOR;
				case '?' :
					return TokenNameQUESTION;
				case ':' :
					if (getNextChar(':'))
						return TokenNameCOLON_COLON;
					return TokenNameCOLON;
				case '\'' :
					{
						int test;
						if ((test = getNextChar('\n', '\r')) == 0) {
							throw new InvalidInputException(INVALID_CHARACTER_CONSTANT);
						}
						if (test > 0) {
							// relocate if finding another quote fairly close: thus unicode '/u000D' will be fully consumed
							for (int lookAhead = 0; lookAhead < 3; lookAhead++) {
								if (this.currentPosition + lookAhead == this.eofPosition)
									break;
								if (this.source[this.currentPosition + lookAhead] == '\n')
									break;
								if (this.source[this.currentPosition + lookAhead] == '\'') {
									this.currentPosition += lookAhead + 1;
									break;
								}
							}
							throw new InvalidInputException(INVALID_CHARACTER_CONSTANT);
						}
					}
					if (getNextChar('\'')) {
						// relocate if finding another quote fairly close: thus unicode '/u000D' will be fully consumed
						for (int lookAhead = 0; lookAhead < 3; lookAhead++) {
							if (this.currentPosition + lookAhead == this.eofPosition)
								break;
							if (this.source[this.currentPosition + lookAhead] == '\n')
								break;
							if (this.source[this.currentPosition + lookAhead] == '\'') {
								this.currentPosition += lookAhead + 1;
								break;
							}
						}
						throw new InvalidInputException(INVALID_CHARACTER_CONSTANT);
					}
					if (getNextChar('\\')) {
						if (this.unicodeAsBackSlash) {
							// consume next character
							this.unicodeAsBackSlash = false;
							if (((this.currentCharacter = this.source[this.currentPosition++]) == '\\') && (this.source[this.currentPosition] == 'u')) {
								getNextUnicodeChar();
							} else {
								if (this.withoutUnicodePtr != 0) {
									unicodeStore();
								}
							}
						} else {
							this.currentCharacter = this.source[this.currentPosition++];
						}
						scanEscapeCharacter();
					} else { // consume next character
						this.unicodeAsBackSlash = false;
						boolean checkIfUnicode = false;
						try {
							checkIfUnicode = ((this.currentCharacter = this.source[this.currentPosition++]) == '\\')
							&& (this.source[this.currentPosition] == 'u');
						} catch(IndexOutOfBoundsException e) {
							this.currentPosition--;
							throw new InvalidInputException(INVALID_CHARACTER_CONSTANT);
						}
						if (checkIfUnicode) {
							getNextUnicodeChar();
						} else {
							if (this.withoutUnicodePtr != 0) {
							    unicodeStore();
							}
						}
					}
					if (getNextChar('\''))
						return TokenNameCharacterLiteral;
					// relocate if finding another quote fairly close: thus unicode '/u000D' will be fully consumed
					for (int lookAhead = 0; lookAhead < 20; lookAhead++) {
						if (this.currentPosition + lookAhead == this.eofPosition)
							break;
						if (this.source[this.currentPosition + lookAhead] == '\n')
							break;
						if (this.source[this.currentPosition + lookAhead] == '\'') {
							this.currentPosition += lookAhead + 1;
							break;
						}
					}
					throw new InvalidInputException(INVALID_CHARACTER_CONSTANT);
				case '"' :
					boolean isTextBlock = scanForTextBlockBeginning();
					if (isTextBlock) {
						return scanForTextBlock();
					}
					try {
						// consume next character
						this.unicodeAsBackSlash = false;
						boolean isUnicode = false;
						if (((this.currentCharacter = this.source[this.currentPosition++]) == '\\')
							&& (this.source[this.currentPosition] == 'u')) {
							getNextUnicodeChar();
							isUnicode = true;
						} else {
							if (this.withoutUnicodePtr != 0) {
							    unicodeStore();
							}
						}

						while (this.currentCharacter != '"') {
							/**** \r and \n are not valid in string literals ****/
							if ((this.currentCharacter == '\n') || (this.currentCharacter == '\r')) {
								if (isUnicode) {
									int start = this.currentPosition - 5;
									while(this.source[start] != '\\') {
										start--;
									}
									if(this.startPosition <= this.cursorLocation
											&& this.cursorLocation <= this.currentPosition-1) {
										this.currentPosition = start;
										// complete inside a string literal
										return TokenNameStringLiteral;
									}
									start = this.currentPosition;
									for (int lookAhead = 0; lookAhead < 50; lookAhead++) {
										if (this.currentPosition >= this.eofPosition) {
											this.currentPosition = start;
											break;
										}
										if (((this.currentCharacter = this.source[this.currentPosition++]) == '\\') && (this.source[this.currentPosition] == 'u')) {
											isUnicode = true;
											getNextUnicodeChar();
										} else {
											isUnicode = false;
										}
										if (!isUnicode && this.currentCharacter == '\n') {
											this.currentPosition--; // set current position on new line character
											break;
										}
										if (this.currentCharacter == '\"') {
											throw new InvalidInputException(INVALID_CHAR_IN_STRING);
										}
									}
								} else {
									this.currentPosition--; // set current position on new line character
									if(this.startPosition <= this.cursorLocation
											&& this.cursorLocation <= this.currentPosition-1) {
										// complete inside a string literal
										return TokenNameStringLiteral;
									}
								}
								throw new InvalidInputException(INVALID_CHAR_IN_STRING);
							}
							if (this.currentCharacter == '\\') {
								if (this.unicodeAsBackSlash) {
									this.withoutUnicodePtr--;
									// consume next character
									this.unicodeAsBackSlash = false;
									if (((this.currentCharacter = this.source[this.currentPosition++]) == '\\') && (this.source[this.currentPosition] == 'u')) {
										getNextUnicodeChar();
										isUnicode = true;
										this.withoutUnicodePtr--;
									} else {
										isUnicode = false;
									}
								} else {
									if (this.withoutUnicodePtr == 0) {
										unicodeInitializeBuffer(this.currentPosition - this.startPosition);
									}
									this.withoutUnicodePtr --;
									this.currentCharacter = this.source[this.currentPosition++];
								}
								// we need to compute the escape character in a separate buffer
								scanEscapeCharacter();
								if (this.withoutUnicodePtr != 0) {
									unicodeStore();
								}
							}
							// consume next character
							this.unicodeAsBackSlash = false;
							if (((this.currentCharacter = this.source[this.currentPosition++]) == '\\')
								&& (this.source[this.currentPosition] == 'u')) {
								getNextUnicodeChar();
								isUnicode = true;
							} else {
								isUnicode = false;
								if (this.withoutUnicodePtr != 0) {
								    unicodeStore();
								}
							}

						}
					} catch (IndexOutOfBoundsException e) {
						this.currentPosition--;
						if(this.startPosition <= this.cursorLocation
							&& this.cursorLocation < this.currentPosition) {
							// complete inside a string literal
							return TokenNameStringLiteral;
						}
						throw new InvalidInputException(UNTERMINATED_STRING);
					} catch (InvalidInputException e) {
						if (e.getMessage().equals(INVALID_ESCAPE)) {
							// relocate if finding another quote fairly close: thus unicode '/u000D' will be fully consumed
							for (int lookAhead = 0; lookAhead < 50; lookAhead++) {
								if (this.currentPosition + lookAhead == this.eofPosition)
									break;
								if (this.source[this.currentPosition + lookAhead] == '\n')
									break;
								if (this.source[this.currentPosition + lookAhead] == '\"') {
									this.currentPosition += lookAhead + 1;
									break;
								}
							}

						}
						throw e; // rethrow
					}
					return TokenNameStringLiteral;
				case '/' :
					{
						int test;
						if ((test = getNextChar('/', '*')) == 0) { //line comment
							this.lastCommentLinePosition = this.currentPosition;
							try { //get the next char
								if (((this.currentCharacter = this.source[this.currentPosition++]) == '\\')
									&& (this.source[this.currentPosition] == 'u')) {
									//-------------unicode traitement ------------
									int c1 = 0, c2 = 0, c3 = 0, c4 = 0;
									this.currentPosition++;
									while (this.source[this.currentPosition] == 'u') {
										this.currentPosition++;
									}
									if ((c1 = ScannerHelper.getHexadecimalValue(this.source[this.currentPosition++])) > 15
										|| c1 < 0
										|| (c2 = ScannerHelper.getHexadecimalValue(this.source[this.currentPosition++])) > 15
										|| c2 < 0
										|| (c3 = ScannerHelper.getHexadecimalValue(this.source[this.currentPosition++])) > 15
										|| c3 < 0
										|| (c4 = ScannerHelper.getHexadecimalValue(this.source[this.currentPosition++])) > 15
										|| c4 < 0) {
										throw new InvalidInputException(INVALID_UNICODE_ESCAPE);
									} else {
										this.currentCharacter = (char) (((c1 * 16 + c2) * 16 + c3) * 16 + c4);
									}
								}

								//handle the \\u case manually into comment
								if (this.currentCharacter == '\\') {
									if (this.source[this.currentPosition] == '\\')
										this.currentPosition++;
								} //jump over the \\
								boolean isUnicode = false;
								while (this.currentCharacter != '\r' && this.currentCharacter != '\n') {
									this.lastCommentLinePosition = this.currentPosition;
									//get the next char
									isUnicode = false;
									if (((this.currentCharacter = this.source[this.currentPosition++]) == '\\')
										&& (this.source[this.currentPosition] == 'u')) {
										isUnicode = true;
										//-------------unicode traitement ------------
										int c1 = 0, c2 = 0, c3 = 0, c4 = 0;
										this.currentPosition++;
										while (this.source[this.currentPosition] == 'u') {
											this.currentPosition++;
										}
										if ((c1 = ScannerHelper.getHexadecimalValue(this.source[this.currentPosition++])) > 15
											|| c1 < 0
											|| (c2 = ScannerHelper.getHexadecimalValue(this.source[this.currentPosition++])) > 15
											|| c2 < 0
											|| (c3 = ScannerHelper.getHexadecimalValue(this.source[this.currentPosition++])) > 15
											|| c3 < 0
											|| (c4 = ScannerHelper.getHexadecimalValue(this.source[this.currentPosition++])) > 15
											|| c4 < 0) {
											throw new InvalidInputException(INVALID_UNICODE_ESCAPE);
										} else {
											this.currentCharacter = (char) (((c1 * 16 + c2) * 16 + c3) * 16 + c4);
										}
									}
									//handle the \\u case manually into comment
									if (this.currentCharacter == '\\') {
										if (this.source[this.currentPosition] == '\\')
											this.currentPosition++;
									} //jump over the \\
								}
								/*
								 * We need to completely consume the line break
								 */
								if (this.currentCharacter == '\r'
								   && this.eofPosition > this.currentPosition) {
								   	if (this.source[this.currentPosition] == '\n') {
										this.currentPosition++;
										this.currentCharacter = '\n';
								   	} else if ((this.source[this.currentPosition] == '\\')
										&& (this.source[this.currentPosition + 1] == 'u')) {
										isUnicode = true;
										char unicodeChar;
										int index = this.currentPosition + 1;
										index++;
										while (this.source[index] == 'u') {
											index++;
										}
										//-------------unicode traitement ------------
										int c1 = 0, c2 = 0, c3 = 0, c4 = 0;
										if ((c1 = ScannerHelper.getHexadecimalValue(this.source[index++])) > 15
											|| c1 < 0
											|| (c2 = ScannerHelper.getHexadecimalValue(this.source[index++])) > 15
											|| c2 < 0
											|| (c3 = ScannerHelper.getHexadecimalValue(this.source[index++])) > 15
											|| c3 < 0
											|| (c4 = ScannerHelper.getHexadecimalValue(this.source[index++])) > 15
											|| c4 < 0) {
											this.currentPosition = index;
											throw new InvalidInputException(INVALID_UNICODE_ESCAPE);
										} else {
											unicodeChar = (char) (((c1 * 16 + c2) * 16 + c3) * 16 + c4);
										}
										if (unicodeChar == '\n') {
											this.currentPosition = index;
											this.currentCharacter = '\n';
										}
									}
							   	}
								recordComment(TokenNameCOMMENT_LINE);
								if (this.startPosition <= this.cursorLocation && this.cursorLocation < this.currentPosition-1){
									throw new InvalidCursorLocation(InvalidCursorLocation.NO_COMPLETION_INSIDE_COMMENT);
								}
								if (this.taskTags != null) checkTaskTag(this.startPosition, this.currentPosition);
								if ((this.currentCharacter == '\r') || (this.currentCharacter == '\n')) {
									//checkNonExternalizedString();
									if (this.recordLineSeparator) {
										if (isUnicode) {
											pushUnicodeLineSeparator();
										} else {
											pushLineSeparator();
										}
									}
								}
								if (this.tokenizeComments) {
									return TokenNameCOMMENT_LINE;
								}
							} catch (IndexOutOfBoundsException e) {
								this.currentPosition--;
								recordComment(TokenNameCOMMENT_LINE);
								if (this.taskTags != null) checkTaskTag(this.startPosition, this.currentPosition);
								if (this.tokenizeComments) {
									return TokenNameCOMMENT_LINE;
								} else {
									this.currentPosition++;
								}
							}
							break;
						}
						if (test > 0) { //traditional and javadoc comment
							try { //get the next char
								boolean isJavadoc = false, star = false;
								boolean isUnicode = false;
								int previous;
								// consume next character
								this.unicodeAsBackSlash = false;
								if (((this.currentCharacter = this.source[this.currentPosition++]) == '\\')
									&& (this.source[this.currentPosition] == 'u')) {
									getNextUnicodeChar();
									isUnicode = true;
								} else {
									isUnicode = false;
									if (this.withoutUnicodePtr != 0) {
									    unicodeStore();
									}
								}

								if (this.currentCharacter == '*') {
									isJavadoc = true;
									star = true;
								}
								if ((this.currentCharacter == '\r') || (this.currentCharacter == '\n')) {
									//checkNonExternalizedString();
									if (this.recordLineSeparator) {
										if (!isUnicode) {
											pushLineSeparator();
										}
									}
								}
								isUnicode = false;
								previous = this.currentPosition;
								if (((this.currentCharacter = this.source[this.currentPosition++]) == '\\')
									&& (this.source[this.currentPosition] == 'u')) {
									//-------------unicode traitement ------------
									getNextUnicodeChar();
									isUnicode = true;
								} else {
									isUnicode = false;
								}
								//handle the \\u case manually into comment
								if (this.currentCharacter == '\\') {
									if (this.source[this.currentPosition] == '\\')
										this.currentPosition++;
								} //jump over the \\
								// empty comment is not a javadoc /**/
								if (this.currentCharacter == '/') {
									isJavadoc = false;
								}
								//loop until end of comment */
								int firstTag = 0;
								while ((this.currentCharacter != '/') || (!star)) {
									if ((this.currentCharacter == '\r') || (this.currentCharacter == '\n')) {
										//checkNonExternalizedString();
										if (this.recordLineSeparator) {
											if (!isUnicode) {
												pushLineSeparator();
											}
										}
									}
									switch (this.currentCharacter) {
										case '*':
											star = true;
											break;
										case '@':
											if (firstTag == 0 && this.isFirstTag()) {
												firstTag = previous;
											}
											//$FALL-THROUGH$ default case to set star to false
										default:
											star = false;
									}
 									//get next char
									previous = this.currentPosition;
									if (((this.currentCharacter = this.source[this.currentPosition++]) == '\\')
										&& (this.source[this.currentPosition] == 'u')) {
										//-------------unicode traitement ------------
										getNextUnicodeChar();
										isUnicode = true;
									} else {
										isUnicode = false;
									}
									//handle the \\u case manually into comment
									if (this.currentCharacter == '\\') {
										if (this.source[this.currentPosition] == '\\')
											this.currentPosition++;
									} //jump over the \\
								}
								int token = isJavadoc ? TokenNameCOMMENT_JAVADOC : TokenNameCOMMENT_BLOCK;
								recordComment(token);
								this.commentTagStarts[this.commentPtr] = firstTag;
								if (!isJavadoc && this.startPosition <= this.cursorLocation && this.cursorLocation < this.currentPosition-1){
									throw new InvalidCursorLocation(InvalidCursorLocation.NO_COMPLETION_INSIDE_COMMENT);
								}
								if (this.taskTags != null) checkTaskTag(this.startPosition, this.currentPosition);
								if (this.tokenizeComments) {
									/*
									if (isJavadoc)
										return TokenNameCOMMENT_JAVADOC;
									return TokenNameCOMMENT_BLOCK;
									*/
									return token;
								}
							} catch (IndexOutOfBoundsException e) {
								this.currentPosition--;
								throw new InvalidInputException(UNTERMINATED_COMMENT);
							}
							break;
						}
						if (getNextChar('='))
							return TokenNameDIVIDE_EQUAL;
						return TokenNameDIVIDE;
					}
				case '\u001a' :
					if (atEnd())
						return TokenNameEOF;
					//the atEnd may not be <this.currentPosition == this.source.length> if source is only some part of a real (external) stream
					throw new InvalidInputException("Ctrl-Z"); //$NON-NLS-1$

				default :
					char c = this.currentCharacter;
					if (c < ScannerHelper.MAX_OBVIOUS) {
						if ((ScannerHelper.OBVIOUS_IDENT_CHAR_NATURES[c] & ScannerHelper.C_IDENT_START) != 0) {
							return scanIdentifierOrKeyword();
						} else if ((ScannerHelper.OBVIOUS_IDENT_CHAR_NATURES[c] & ScannerHelper.C_DIGIT) != 0) {
								return scanNumber(false);
						} else {
							return TokenNameERROR;
						}
					}
					boolean isJavaIdStart;
					if (c >= HIGH_SURROGATE_MIN_VALUE && c <= HIGH_SURROGATE_MAX_VALUE) {
						if (this.complianceLevel < ClassFileConstants.JDK1_5) {
							throw new InvalidInputException(INVALID_UNICODE_ESCAPE);
						}
						// Unicode 4 detection
						char low = (char) getNextChar();
						if (low < LOW_SURROGATE_MIN_VALUE || low > LOW_SURROGATE_MAX_VALUE) {
							// illegal low surrogate
							throw new InvalidInputException(INVALID_LOW_SURROGATE);
						}
						isJavaIdStart = ScannerHelper.isJavaIdentifierStart(this.complianceLevel, c, low);
					}
					else if (c >= LOW_SURROGATE_MIN_VALUE && c <= LOW_SURROGATE_MAX_VALUE) {
						if (this.complianceLevel < ClassFileConstants.JDK1_5) {
							throw new InvalidInputException(INVALID_UNICODE_ESCAPE);
						}
						throw new InvalidInputException(INVALID_HIGH_SURROGATE);
					} else {
						// optimized case already checked
						isJavaIdStart = Character.isJavaIdentifierStart(c);
					}
					if (isJavaIdStart)
						return scanIdentifierOrKeyword();
					if (ScannerHelper.isDigit(this.currentCharacter)) {
						return scanNumber(false);
					}
					return TokenNameERROR;
			}
		}
	} //-----------------end switch while try--------------------
	catch (IndexOutOfBoundsException e) {
		if (this.tokenizeWhiteSpace && (whiteStart != this.currentPosition - 1)) {
			// reposition scanner in case we are interested by spaces as tokens
			this.currentPosition--;
			this.startPosition = whiteStart;
			return TokenNameWHITESPACE;
		}
	}
	/* might be completing at very end of file (e.g. behind a dot) */
	if (this.completionIdentifier == null &&
		this.startPosition == this.cursorLocation + 1){
		this.endOfEmptyToken = this.currentPosition - 1;
		this.currentPosition = this.startPosition; // for being detected as empty free identifier
		return TokenNameIdentifier;
	}
	return TokenNameEOF;
}
@Override
protected int getNextNotFakedToken() throws InvalidInputException {
	int token;
	boolean fromUnget = false;
	if (this.nextToken != TokenNameNotAToken) {
		token = this.nextToken;
		this.nextToken = TokenNameNotAToken;
		fromUnget = true;
	} else {
		token = getNextToken();
	}
	if (this.currentPosition == this.startPosition) {
		if (!fromUnget)
			this.currentPosition++; // on fake completion identifier
		return -1;
	}
	return token;
}
@Override
protected int scanForTextBlock() throws InvalidInputException {
	int lastQuotePos = 0;
	try {
		this.rawStart = this.currentPosition - this.startPosition;
		while (this.currentPosition <= this.eofPosition) {
			// The following few lines is the only difference between this method
			// and the scanForTextBlock() in Scanner()
			//======
			int start = this.currentPosition;
			if(this.startPosition <= this.cursorLocation
					&& this.cursorLocation <= this.currentPosition-1) {
				this.currentPosition = start;
				// complete inside a string literal
				return TokenNameStringLiteral;
			}
			//=====
			start = this.currentPosition;
			if (this.currentCharacter == '"') {
				lastQuotePos = this.currentPosition;
				// look for text block delimiter
				if (scanForTextBlockClose()) {
					this.currentPosition += 2;
					return TerminalTokens.TokenNameStringLiteral;
				}
				if (this.withoutUnicodePtr != 0) {
					unicodeStore();
				}
			} else {
				if ((this.currentCharacter == '\r') || (this.currentCharacter == '\n')) {
					if (this.recordLineSeparator) {
						pushLineSeparator();
					}
				}
			}
			outer: if (this.currentCharacter == '\\') {
				switch(this.source[this.currentPosition]) {
					case 'n' :
					case 'r' :
					case 'f' :
						break outer;
					case '\n' :
					case '\r' :
						this.currentCharacter = '\\';
						this.currentPosition++;
						break;
					case '\\' :
						this.currentPosition++;
						break;
					default :
						if (this.unicodeAsBackSlash) {
							this.withoutUnicodePtr--;
							// consume next character
							if (this.currentPosition >= this.eofPosition) {
								break;
							}
							this.unicodeAsBackSlash = false;
							if (((this.currentCharacter = this.source[this.currentPosition++]) == '\\')
									&& (this.source[this.currentPosition] == 'u')) {
								getNextUnicodeChar();
								this.withoutUnicodePtr--;
							}
						} else {
							if (this.withoutUnicodePtr == 0) {
								unicodeInitializeBuffer(this.currentPosition - this.startPosition);
							}
							this.withoutUnicodePtr --;
							this.currentCharacter = this.source[this.currentPosition++];
						}
						int oldPos = this.currentPosition - 1;
						scanEscapeCharacter();
						if (ScannerHelper.isWhitespace(this.currentCharacter)) {
							if (this.withoutUnicodePtr == 0) {
								unicodeInitializeBuffer(this.currentPosition - this.startPosition);
							}
							unicodeStore('\\');
							this.currentPosition = oldPos;
							this.currentCharacter = this.source[this.currentPosition];
							break outer;
						}
				}
				if (this.withoutUnicodePtr != 0) {
					unicodeStore();
				}
			}
			// consume next character
			this.unicodeAsBackSlash = false;
			if (((this.currentCharacter = this.source[this.currentPosition++]) == '\\')
					&& (this.source[this.currentPosition] == 'u')) {
				getNextUnicodeChar();
			} else {
				if (this.currentCharacter == '"'/* || skipWhitespace*/)
					continue;
				if (this.withoutUnicodePtr != 0) {
					unicodeStore();
				}
			}
		}
		if (lastQuotePos > 0)
			this.currentPosition = lastQuotePos;
		this.currentPosition = (lastQuotePos > 0) ? lastQuotePos : this.startPosition + this.rawStart;
		throw new InvalidInputException(UNTERMINATED_TEXT_BLOCK);
	} catch (IndexOutOfBoundsException e) {
		this.currentPosition = (lastQuotePos > 0) ? lastQuotePos : this.startPosition + this.rawStart;
		if(this.startPosition <= this.cursorLocation
				&& this.cursorLocation < this.currentPosition) {
				// complete inside a string literal
			return TokenNameStringLiteral;
		}
		throw new InvalidInputException(UNTERMINATED_TEXT_BLOCK);
	}
}

@Override
public final void getNextUnicodeChar() throws InvalidInputException {
	int temp = this.currentPosition; // the \ is already read
	super.getNextUnicodeChar();
	if(this.cursorLocation > temp) {
		this.unicodeCharSize += (this.currentPosition - temp);
	}
	if (temp < this.cursorLocation && this.cursorLocation < this.currentPosition-1){
		throw new InvalidCursorLocation(InvalidCursorLocation.NO_COMPLETION_INSIDE_UNICODE);
	}
}
@Override
protected boolean isFirstTag() {
	return
		getNextChar('d') &&
		getNextChar('e') &&
		getNextChar('p') &&
		getNextChar('r') &&
		getNextChar('e') &&
		getNextChar('c') &&
		getNextChar('a') &&
		getNextChar('t') &&
		getNextChar('e') &&
		getNextChar('d');
}
public final void jumpOverBlock() {
	jumpOverMethodBody();
}
///*
// * In case we actually read a keyword, but the cursor is located inside,
// * we pretend we read an identifier.
// */
@Override
public int scanIdentifierOrKeyword() {

	int id = super.scanIdentifierOrKeyword();

	if (this.startPosition <= this.cursorLocation+1
			&& this.cursorLocation < this.currentPosition){

		// extends the end of the completion token even if the end is after eofPosition
		if (this.cursorLocation+1 == this.eofPosition) {
			int temp = this.eofPosition;
			this.eofPosition = this.source.length;
		 	while(getNextCharAsJavaIdentifierPart()){/*empty*/}
			this.eofPosition = temp;
		}
		// convert completed keyword into an identifier
		return TokenNameIdentifier;
	}
	return id;
}

@Override
public int scanNumber(boolean dotPrefix) throws InvalidInputException {

	int token = super.scanNumber(dotPrefix);

	// consider completion just before a number to be ok, will insert before it
	if (this.startPosition <= this.cursorLocation && this.cursorLocation < this.currentPosition){
		throw new InvalidCursorLocation(InvalidCursorLocation.NO_COMPLETION_INSIDE_NUMBER);
	}
	return token;
}
}
