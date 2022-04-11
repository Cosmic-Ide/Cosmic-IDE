/*******************************************************************************
 * Copyright (c) 2006, 2013 IBM Corporation and others.
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
package org.eclipse.jdt.core.formatter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import org.eclipse.jdt.internal.compiler.parser.ScannerHelper;
import org.eclipse.jdt.internal.compiler.util.Util;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultLineTracker;
import org.eclipse.jface.text.ILineTracker;
import org.eclipse.jface.text.IRegion;

import org.eclipse.text.edits.ReplaceEdit;

/**
 * Helper class to provide String manipulation functions dealing with indentations.
 *
 * @since 3.2
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public final class IndentManipulation {

	private IndentManipulation() {
		// don't instantiate
	}

	/**
	 * Returns <code>true</code> if the given character is an indentation character. Indentation character are all whitespace characters
	 * except the line delimiter characters.
	 *
	 * @param ch the given character
	 * @return Returns <code>true</code> if this the character is a indent character, <code>false</code> otherwise
	 */
	public static boolean isIndentChar(char ch) {
		return ScannerHelper.isWhitespace(ch) && !isLineDelimiterChar(ch);
	}

	/**
	 * Returns <code>true</code> if the given character is a line delimiter character.
	 *
	 * @param ch the given character
	 * @return Returns <code>true</code> if this the character is a line delimiter character, <code>false</code> otherwise
	 */
	public static boolean isLineDelimiterChar(char ch) {
		return ch == '\n' || ch == '\r';
	}

	/**
	 * Returns the indentation of the given line in indentation units. Odd spaces are
	 * not counted. This method only analyzes the content of <code>line</code> up to the first
	 * non-whitespace character.
	 *
	 * @param line the string to measure the indent of
	 * @param tabWidth the width of one tab character in space equivalents
	 * @param indentWidth the width of one indentation unit in space equivalents
	 * @return the number of indentation units that line is indented by
	 * @exception IllegalArgumentException if:
	 * <ul>
	 * <li>the given <code>indentWidth</code> is lower than zero</li>
	 * <li>the given <code>tabWidth</code> is lower than zero</li>
	 * <li>the given <code>line</code> is null</li>
	 * </ul>
	 */
	public static int measureIndentUnits(CharSequence line, int tabWidth, int indentWidth) {
		if (indentWidth < 0 || tabWidth < 0 || line == null) {
			throw new IllegalArgumentException();
		}

		if (indentWidth == 0) return 0;
		int visualLength= measureIndentInSpaces(line, tabWidth);
		return visualLength / indentWidth;
	}

	/**
	 * Returns the indentation of the given line in space equivalents.
	 *
	 * <p>Tab characters are counted using the given <code>tabWidth</code> and every other indent
	 * character as one. This method analyzes the content of <code>line</code> up to the first
	 * non-whitespace character.</p>
	 *
	 * @param line the string to measure the indent of
	 * @param tabWidth the width of one tab in space equivalents
	 * @return the measured indent width in space equivalents
	 * @exception IllegalArgumentException if:
	 * <ul>
	 * <li>the given <code>line</code> is null</li>
	 * <li>the given <code>tabWidth</code> is lower than zero</li>
	 * </ul>
	 */
	public static int measureIndentInSpaces(CharSequence line, int tabWidth) {
		if (tabWidth < 0 || line == null) {
			throw new IllegalArgumentException();
		}

		int length= 0;
		int max= line.length();
		for (int i= 0; i < max; i++) {
			char ch= line.charAt(i);
			if (ch == '\t') {
				length = calculateSpaceEquivalents(tabWidth, length);
			} else if (isIndentChar(ch)) {
				length++;
			} else {
				return length;
			}
		}
		return length;
	}

	/**
	 * Returns the leading indentation string of the given line. Note that the returned string
	 * need not be equal to the leading whitespace as odd spaces are not considered part of the
	 * indentation.
	 *
	 * @param line the line to scan
	 * @param tabWidth the size of one tab in space equivalents
	 * @param indentWidth the width of one indentation unit in space equivalents
	 * @return the indent part of <code>line</code>, but no odd spaces
	 * @exception IllegalArgumentException if:
	 * <ul>
	 * <li>the given <code>indentWidth</code> is lower than zero</li>
	 * <li>the given <code>tabWidth</code> is lower than zero</li>
	 * <li>the given <code>line</code> is null</li>
	 * </ul>
	 */
	public static String extractIndentString(String line, int tabWidth, int indentWidth) {
		if (tabWidth < 0 || indentWidth < 0 || line == null) {
			throw new IllegalArgumentException();
		}

		int size = line.length();
		int end = 0;

		int spaceEquivs = 0;
		int characters = 0;
		for (int i = 0; i < size; i++) {
			char c = line.charAt(i);
			if (c == '\t') {
				spaceEquivs = calculateSpaceEquivalents(tabWidth, spaceEquivs);
				characters++;
			} else if (isIndentChar(c)) {
				spaceEquivs++;
				characters++;
			} else {
				break;
			}
			if (spaceEquivs >= indentWidth) {
				end += characters;
				characters = 0;
				if(indentWidth == 0) {
					spaceEquivs = 0;
				} else {
					spaceEquivs = spaceEquivs % indentWidth;
				}
			}
		}
		if (end == 0) {
			return Util.EMPTY_STRING;
		} else if (end == size) {
			return line;
		} else {
			return line.substring(0, end);
		}
	}


	/**
	 * Removes the given number of indentation units from a given line. If the line
	 * has less indent than the given indentUnitsToRemove, all the available indentation is removed.
	 * If <code>indentsToRemove <= 0 or indent == 0</code> the line is returned.
	 *
	 * @param line the line to trim
	 * @param tabWidth the width of one tab in space equivalents
	 * @param indentWidth the width of one indentation unit in space equivalents
	 * @return the trimmed string
	 * @exception IllegalArgumentException if:
	 * <ul>
	 * <li>the given <code>indentWidth</code> is lower than zero</li>
	 * <li>the given <code>tabWidth</code> is lower than zero</li>
	 * <li>the given <code>line</code> is null</li>
	 * </ul>
	 */
	public static String trimIndent(String line, int indentUnitsToRemove, int tabWidth, int indentWidth) {
		if (tabWidth < 0 || indentWidth < 0 || line == null) {
			throw new IllegalArgumentException();
		}

		if (indentUnitsToRemove <= 0 || indentWidth == 0) {
			return line;
		}
		final int spaceEquivalentsToRemove= indentUnitsToRemove * indentWidth;

		int start= 0;
		int spaceEquivalents= 0;
		int size= line.length();
		String prefix= null;
		for (int i= 0; i < size; i++) {
			char c= line.charAt(i);
			if (c == '\t') {
				spaceEquivalents = calculateSpaceEquivalents(tabWidth, spaceEquivalents);
			} else if (isIndentChar(c)) {
				spaceEquivalents++;
			} else {
				// Assert.isTrue(false, "Line does not have requested number of indents");
				start= i;
				break;
			}
			if (spaceEquivalents == spaceEquivalentsToRemove) {
				start= i + 1;
				break;
			}
			if (spaceEquivalents > spaceEquivalentsToRemove) {
				// can happen if tabSize > indentSize, e.g tabsize==8, indent==4, indentsToRemove==1, line prefixed with one tab
				// this implements the third option
				start= i + 1; // remove the tab
				// and add the missing spaces
				char[] missing= new char[spaceEquivalents - spaceEquivalentsToRemove];
				Arrays.fill(missing, ' ');
				prefix= new String(missing);
				break;
			}
		}
		String trimmed;
		if (start == size)
			trimmed= Util.EMPTY_STRING;
		else
			trimmed= line.substring(start);

		if (prefix == null)
			return trimmed;
		return prefix + trimmed;
	}

	/**
	 * Change the indent of a, possible multiple line, code string. The given number of indent units is removed,
	 * and a new indent string is added.
	 * <p>The first line of the code will not be changed (It is considered to have no indent as it might start in
	 * the middle of a line).</p>
	 *
	 * @param code the code to change the indent of
	 * @param indentUnitsToRemove the number of indent units to remove from each line (except the first) of the given code
	 * @param tabWidth the size of one tab in space equivalents
	 * @param indentWidth the width of one indentation unit in space equivalents
	 * @param newIndentString the new indent string to be added to all lines (except the first)
	 * @param lineDelim the new line delimiter to be used. The returned code will contain only this line delimiter.
	 * @return the newly indent code, containing only the given line delimiters.
	 * @exception IllegalArgumentException if:
	 * <ul>
	 * <li>the given <code>indentWidth</code> is lower than zero</li>
	 * <li>the given <code>tabWidth</code> is lower than zero</li>
	 * <li>the given <code>code</code> is null</li>
	 * <li>the given <code>indentUnitsToRemove</code> is lower than zero</li>
	 * <li>the given <code>newIndentString</code> is null</li>
	 * <li>the given <code>lineDelim</code> is null</li>
	 * </ul>
	 */
	public static String changeIndent(String code, int indentUnitsToRemove, int tabWidth, int indentWidth, String newIndentString, String lineDelim) {
		if (tabWidth < 0 || indentWidth < 0 || code == null || indentUnitsToRemove < 0 || newIndentString == null || lineDelim == null) {
			throw new IllegalArgumentException();
		}

		try {
			ILineTracker tracker= new DefaultLineTracker();
			tracker.set(code);
			int nLines= tracker.getNumberOfLines();
			if (nLines == 1) {
				return code;
			}

			StringBuilder buf= new StringBuilder();

			for (int i= 0; i < nLines; i++) {
				IRegion region= tracker.getLineInformation(i);
				int start= region.getOffset();
				int end= start + region.getLength();
				String line= code.substring(start, end);

				if (i == 0) {  // no indent for first line (contained in the formatted string)
					buf.append(line);
				} else { // no new line after last line
					buf.append(lineDelim);
					buf.append(newIndentString);
					if(indentWidth != 0) {
						buf.append(trimIndent(line, indentUnitsToRemove, tabWidth, indentWidth));
					} else {
						buf.append(line);
					}
				}
			}
			return buf.toString();
		} catch (BadLocationException e) {
			// can not happen
			return code;
		}
	}

	/**
	 * Returns the text edits retrieved after changing the indentation of a, possible multi-line, code string.
	 *
	 * <p>The given number of indent units is removed, and a new indent string is added.</p>
	 * <p>The first line of the code will not be changed (It is considered to have no indent as it might start in
	 * the middle of a line).</p>
	 *
	 * @param source The code to change the indent of
	 * @param indentUnitsToRemove the number of indent units to remove from each line (except the first) of the given code
	 * @param tabWidth the size of one tab in space equivalents
	 * @param indentWidth the width of one indentation unit in space equivalents
	 * @param newIndentString the new indent string to be added to all lines (except the first)
	 * @return returns the resulting text edits
	 * @exception IllegalArgumentException if:
	 * <ul>
	 * <li>the given <code>indentWidth</code> is lower than zero</li>
	 * <li>the given <code>tabWidth</code> is lower than zero</li>
	 * <li>the given <code>source</code> is null</li>
	 * <li>the given <code>indentUnitsToRemove</code> is lower than zero</li>
	 * <li>the given <code>newIndentString</code> is null</li>
	 * </ul>
	 */
	public static ReplaceEdit[] getChangeIndentEdits(String source, int indentUnitsToRemove, int tabWidth, int indentWidth, String newIndentString) {
		if (tabWidth < 0 || indentWidth < 0 || source == null || indentUnitsToRemove < 0 || newIndentString == null) {
			throw new IllegalArgumentException();
		}

		ArrayList result= new ArrayList();
		try {
			ILineTracker tracker= new DefaultLineTracker();
			tracker.set(source);
			int nLines= tracker.getNumberOfLines();
			if (nLines == 1)
				return (ReplaceEdit[])result.toArray(new ReplaceEdit[result.size()]);
			for (int i= 1; i < nLines; i++) {
				IRegion region= tracker.getLineInformation(i);
				int offset= region.getOffset();
				String line= source.substring(offset, offset + region.getLength());
				int length= indexOfIndent(line, indentUnitsToRemove, tabWidth, indentWidth);
				if (length >= 0) {
					result.add(new ReplaceEdit(offset, length, newIndentString));
				} else {
					length= measureIndentUnits(line, tabWidth, indentWidth);
					result.add(new ReplaceEdit(offset, length, "")); //$NON-NLS-1$
				}
			}
		} catch (BadLocationException cannotHappen) {
			// can not happen
		}
		return (ReplaceEdit[])result.toArray(new ReplaceEdit[result.size()]);
	}

	/*
	 * Returns the index where the indent of the given size ends.
	 * Returns <code>-1<code> if the line isn't prefixed with an indent of
	 * the given number of indents.
	 */
	private static int indexOfIndent(CharSequence line, int numberOfIndentUnits, int tabWidth, int indentWidth) {

		int spaceEquivalents= numberOfIndentUnits * indentWidth;

		int size= line.length();
		int result= -1;
		int blanks= 0;
		for (int i= 0; i < size && blanks < spaceEquivalents; i++) {
			char c= line.charAt(i);
			if (c == '\t') {
				blanks = calculateSpaceEquivalents(tabWidth, blanks);
			} else if (isIndentChar(c)) {
				blanks++;
			} else {
				break;
			}
			result= i;
		}
		if (blanks < spaceEquivalents)
			return -1;
		return result + 1;
	}

	/*
	 * Calculates space equivalents up to the next tab stop
	 */
	private static int calculateSpaceEquivalents(int tabWidth, int spaceEquivalents) {
		if (tabWidth == 0){
			return spaceEquivalents;
		}
		int remainder = spaceEquivalents % tabWidth;
		spaceEquivalents += tabWidth - remainder;
		return spaceEquivalents;
	}

	/**
	 * Returns the tab width as configured in the given map.
	 * <p>Use {@link org.eclipse.jdt.core.IJavaProject#getOptions(boolean)} to get the most current project options.</p>
	 *
	 * @param options the map to get the formatter settings from.
	 *
	 * @return the tab width
	 * @exception IllegalArgumentException if the given <code>options</code> is null
	 */
	public static int getTabWidth(Map options) {
		if (options == null) {
			throw new IllegalArgumentException();
		}
		return getIntValue(options, DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, 4);
	}

	/**
	 * Returns the tab width as configured in the given map.
	 * <p>Use {@link org.eclipse.jdt.core.IJavaProject#getOptions(boolean)} to get the most current project options.</p>
	 *
	 * @param options the map to get the formatter settings from
	 *
	 * @return the indent width
	 * @exception IllegalArgumentException if the given <code>options</code> is null
	 */
	public static int getIndentWidth(Map options) {
		if (options == null) {
			throw new IllegalArgumentException();
		}
		int tabWidth=getTabWidth(options);
		boolean isMixedMode= DefaultCodeFormatterConstants.MIXED.equals(options.get(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR));
		if (isMixedMode) {
			return getIntValue(options, DefaultCodeFormatterConstants.FORMATTER_INDENTATION_SIZE, tabWidth);
		}
		return tabWidth;
	}

	private static int getIntValue(Map options, String key, int def) {
		try {
			return Integer.parseInt((String) options.get(key));
		} catch (NumberFormatException e) {
			return def;
		}
	}
}

