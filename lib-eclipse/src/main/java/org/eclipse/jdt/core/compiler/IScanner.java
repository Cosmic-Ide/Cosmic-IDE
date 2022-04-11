/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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

package org.eclipse.jdt.core.compiler;

 /**
  * <p>
  * <strong>Caveat:</strong> With the introduction of "restricted keywords" in Java 9
  * it is impossible to classify a token without the help of a parser.
  * For that reason, this interface is not suitable for scanning a modular compilation unit
  * ("module-info.java").
  * It is the client's responsibility to pass only source from ordinary compilation units.
  * For lack of a file name the scanner cannot check this.
  * </p>
  * Definition of a Java scanner, as returned by the <code>ToolFactory</code>.
  * The scanner is responsible for tokenizing a given source, providing information about
  * the nature of the token read, its positions and source equivalent.
  * <p>
  * When the scanner has finished tokenizing, it answers an EOF token (<code>
  * ITerminalSymbols#TokenNameEOF</code>.
  * </p><p>
  * When encountering lexical errors, an <code>InvalidInputException</code> is thrown.
  * </p>
  *
  * @see org.eclipse.jdt.core.ToolFactory
  * @see ITerminalSymbols
  * @since 2.0
 * @noimplement This interface is not intended to be implemented by clients.
  */
public interface IScanner {

	/**
	 * Answers the current identifier source, after unicode escape sequences have
	 * been translated into unicode characters.
	 * For example, if original source was <code>\\u0061bc</code> then it will answer <code>abc</code>.
	 *
	 * @return the current identifier source, after unicode escape sequences have
	 * been translated into unicode characters
	 */
	char[] getCurrentTokenSource();

	/**
	 * Answers the current identifier source, before unicode escape sequences have
	 * been translated into unicode characters.
	 * For example, if original source was <code>\\u0061bc</code> then it will answer <code>\\u0061bc</code>.
	 *
	 * @return the current identifier source, before unicode escape sequences have
	 * been translated into unicode characters
	 * @since 2.1
	 */
	char[] getRawTokenSource();

	/**
	 * Answers the starting position of the current token inside the original source.
	 * This position is zero-based and inclusive. It corresponds to the position of the first character
	 * which is part of this token. If this character was a unicode escape sequence, it points at the first
	 * character of this sequence.
	 *
	 * @return the starting position of the current token inside the original source
	 */
	int getCurrentTokenStartPosition();

	/**
	 * Answers the ending position of the current token inside the original source.
	 * This position is zero-based and inclusive. It corresponds to the position of the last character
	 * which is part of this token. If this character was a unicode escape sequence, it points at the last
	 * character of this sequence.
	 *
	 * @return the ending position of the current token inside the original source
	 */
	int getCurrentTokenEndPosition();

	/**
	 * Answers the starting position of a given line number. This line has to have been encountered
	 * already in the tokenization process (in other words, it cannot be used to compute positions of lines beyond
	 * current token). Once the entire source has been processed, it can be used without any limit.
	 * Line starting positions are zero-based, and start immediately after the previous line separator (if any).
	 *
	 * @param lineNumber the given line number
	 * @return the starting position of a given line number
	 */
	int getLineStart(int lineNumber);

	/**
	 * Answers the ending position of a given line number. This line has to have been encountered
	 * already in the tokenization process (in other words, it cannot be used to compute positions of lines beyond
	 * current token). Once the entire source has been processed, it can be used without any limit.
	 * Line ending positions are zero-based, and correspond to the last character of the line separator
	 * (in case multi-character line separators).
	 *
	 * @param lineNumber the given line number
	 * @return the ending position of a given line number
	 **/
	int getLineEnd(int lineNumber);

	/**
	 * Answers an array of the ending positions of the lines encountered so far. Line ending positions
	 * are zero-based, and correspond to the last character of the line separator (in case multi-character
	 * line separators).
	 *
	 * @return an array of the ending positions of the lines encountered so far
	 */
	int[] getLineEnds();

	/**
	 * Answers a 1-based line number using the lines which have been encountered so far. If the position
	 * is located beyond the current scanned line, then the last line number will be answered.
	 *
	 * @param charPosition the given character position
	 * @return a 1-based line number using the lines which have been encountered so far
	 */
	int getLineNumber(int charPosition);

	/**
	 * Read the next token in the source, and answers its ID as specified by <code>ITerminalSymbols</code>.
	 * Note that the actual token ID values are subject to change if new keywords were added to the language
	 * (for instance, 'assert' is a keyword in 1.4).
	 *
	 * @throws InvalidInputException in case a lexical error was detected while reading the current token
	 * @return the next token
	 */
	int getNextToken() throws InvalidInputException;

	/**
	 * Answers the original source being processed (not a copy of it).
	 *
	 * @return the original source being processed
	 */
	char[] getSource();

	/**
	 * Reposition the scanner on some portion of the original source. The given endPosition is the last valid position.
	 * Beyond this position, the scanner will answer EOF tokens (<code>ITerminalSymbols.TokenNameEOF</code>).
	 *
	 * @param startPosition the given start position
	 * @param endPosition the given end position
	 */
	void resetTo(int startPosition, int endPosition);

	/**
	 * Set the scanner source to process. By default, the scanner will consider starting at the beginning of the
	 * source until it reaches its end.
	 * If the given source is <code>null</code>, this clears the source.
	 *
	 * @param source the given source
	 */
	void setSource(char[] source);
}
