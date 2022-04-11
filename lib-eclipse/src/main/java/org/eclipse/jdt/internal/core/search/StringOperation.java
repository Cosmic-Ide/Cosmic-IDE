/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
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
package org.eclipse.jdt.internal.core.search;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.parser.ScannerHelper;

/**
 * This class is a collection of helper methods to manipulate strings during search.
 */
public final class StringOperation {

	private final static int[] EMPTY_REGIONS = new int[0];

/**
 * Answers all the regions in a given name matching a given camel case pattern.
 * </p><p>
 * Each of these regions is made of its starting index and its length in the given
 * name. They are all concatenated in a single array of <code>int</code>
 * which therefore always has an even length.
 * </p><p>
 * Note that each region is disjointed from the following one.<br>
 * E.g. if the regions are <code>{ start1, length1, start2, length2 }</code>,
 * then <code>start1+length1</code> will always be smaller than
 * <code>start2</code>.
 * </p><p>
 * <pre>
 * Examples:
 * <ol><li>  pattern = "NPE"
 *  name = NullPointerException / NoPermissionException
 *  result:  { 0, 1, 4, 1, 11, 1 } / { 0, 1, 2, 1, 12, 1 } </li>
 * <li>  pattern = "NuPoEx"
 *  name = NullPointerException
 *  result:  { 0, 2, 4, 2, 11, 2 }</li>
 * <li>  pattern = "IPL3"
 *  name = "IPerspectiveListener3"
 *  result:  { 0, 2, 12, 1, 20, 1 }</li>
 * <li>  pattern = "HashME"
 *  name = "HashMapEntry"
 *  result:  { 0, 5, 7, 1 }</li>
 * </ol></pre>
 *</p>
 * @see CharOperation#camelCaseMatch(char[], int, int, char[], int, int, boolean)
 * 	for more details on the camel case behavior
 * @see CharOperation#match(char[], char[], boolean) for more details on the
 * 	pattern match behavior
 *
 * @param pattern the given pattern
 * @param patternStart the start index of the pattern, inclusive
 * @param patternEnd the end index of the pattern, exclusive
 * @param name the given name
 * @param nameStart the start index of the name, inclusive
 * @param nameEnd the end index of the name, exclusive
 * @param samePartCount flag telling whether the pattern and the name should
 * 	have the same count of parts or not.<br>
 * 	&nbsp;&nbsp;For example:
 * 	<ul>
 * 		<li>'HM' type string pattern will match 'HashMap' and 'HtmlMapper' types,
 * 				but not 'HashMapEntry'</li>
 * 		<li>'HMap' type string pattern will still match previous 'HashMap' and
 * 				'HtmlMapper' types, but not 'HighMagnitude'</li>
 * 	</ul>
 * @return an array of <code>int</code> having two slots per returned
 * 	regions (first one is the starting index of the region and the second
 * 	one the length of the region).<br>
 * 	Note that it may be <code>null</code> if the given name does not match
 * 	the pattern
 * @since 3.5
 */
public static final int[] getCamelCaseMatchingRegions(String pattern, int patternStart, int patternEnd, String name, int nameStart, int nameEnd, boolean samePartCount) {

	/* !!!!!!!!!! WARNING !!!!!!!!!!
	 * The algorithm used in this method has been fully inspired from
	 * CharOperation#camelCaseMatch(char[], int, int, char[], int, int, boolean).
	 *
	 * So, if any change needs to be applied in the algorithm, do NOT forget
	 * to backport it in the CharOperation method!
	 */

	if (name == null)
		return null; // null name cannot match
	if (pattern == null) {
		// null pattern cannot match any region
		// see bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=264816
		return EMPTY_REGIONS;
	}
	if (patternEnd < 0) 	patternEnd = pattern.length();
	if (nameEnd < 0) nameEnd = name.length();

	if (patternEnd <= patternStart) {
		return nameEnd <= nameStart
			? new int[] { patternStart, patternEnd-patternStart }
			: null;
	}
	if (nameEnd <= nameStart) return null;
	// check first pattern char
	if (name.charAt(nameStart) != pattern.charAt(patternStart)) {
		// first char must strictly match (upper/lower)
		return null;
	}

	char patternChar, nameChar;
	int iPattern = patternStart;
	int iName = nameStart;

	// init segments
	int parts = 1;
	for (int i=patternStart+1; i<patternEnd; i++) {
		final char ch = pattern.charAt(i);
		if (ch < ScannerHelper.MAX_OBVIOUS) {
			if ((ScannerHelper.OBVIOUS_IDENT_CHAR_NATURES[ch] & (ScannerHelper.C_UPPER_LETTER | ScannerHelper.C_DIGIT)) != 0) {
				parts++;
			}
		} else if (Character.isJavaIdentifierPart(ch) && (Character.isUpperCase(ch) || Character.isDigit(ch))) {
			parts++;
		}
	}
	int[] segments = null;
	int count = 0; // count

	// Main loop is on pattern characters
	int segmentStart = iName;
	while (true) {
		iPattern++;
		iName++;

		if (iPattern == patternEnd) { // we have exhausted pattern...
			// it's a match if the name can have additional parts (i.e. uppercase characters) or is also exhausted
			if (!samePartCount || iName == nameEnd) {
				if (segments == null) {
					segments = new int[2];
				}
				segments[count++] = segmentStart;
				segments[count++] = iName - segmentStart;
				if (count < segments.length) {
					System.arraycopy(segments, 0, segments = new int[count], 0, count);
				}
				return segments;
			}

			// otherwise it's a match only if the name has no more uppercase characters
			int segmentEnd = iName;
			while (true) {
				if (iName == nameEnd) {
					// we have exhausted the name, so it's a match
					if (segments == null) {
						segments = new int[2];
					}
					segments[count++] = segmentStart;
					segments[count++] = segmentEnd - segmentStart;
					if (count < segments.length) {
						System.arraycopy(segments, 0, segments = new int[count], 0, count);
					}
					return segments;
				}
				nameChar = name.charAt(iName);
				// test if the name character is uppercase
				if (nameChar < ScannerHelper.MAX_OBVIOUS) {
					if ((ScannerHelper.OBVIOUS_IDENT_CHAR_NATURES[nameChar] & ScannerHelper.C_UPPER_LETTER) != 0) {
						return null;
					}
				}
				else if (!Character.isJavaIdentifierPart(nameChar) || Character.isUpperCase(nameChar)) {
					return null;
				}
				iName++;
			}
		}

		if (iName == nameEnd){
			// We have exhausted the name (and not the pattern), so it's not a match
			return null;
		}

		// For as long as we're exactly matching, bring it on (even if it's a lower case character)
		if ((patternChar = pattern.charAt(iPattern)) == name.charAt(iName)) {
			continue;
		}
		int segmentEnd = iName;

		// If characters are not equals, then it's not a match if patternChar is lowercase
		if (patternChar < ScannerHelper.MAX_OBVIOUS) {
			if ((ScannerHelper.OBVIOUS_IDENT_CHAR_NATURES[patternChar] & (ScannerHelper.C_UPPER_LETTER | ScannerHelper.C_DIGIT)) == 0) {
				return null;
			}
		} else if (Character.isJavaIdentifierPart(patternChar) && !Character.isUpperCase(patternChar) && !Character.isDigit(patternChar)) {
			return null;
		}

		// patternChar is uppercase, so let's find the next uppercase in name
		while (true) {
			if (iName == nameEnd){
	            //	We have exhausted name (and not pattern), so it's not a match
				return null;
			}

			nameChar = name.charAt(iName);
			if (nameChar < ScannerHelper.MAX_OBVIOUS) {
				int charNature = ScannerHelper.OBVIOUS_IDENT_CHAR_NATURES[nameChar];
				if ((charNature & (ScannerHelper.C_LOWER_LETTER | ScannerHelper.C_SPECIAL)) != 0) {
					// nameChar is lowercase
					iName++;
				} else if ((charNature & ScannerHelper.C_DIGIT) != 0) {
					// nameChar is digit => break if the digit is current pattern character otherwise consume it
					if (patternChar == nameChar) break;
					iName++;
				// nameChar is uppercase...
				} else  if (patternChar != nameChar) {
					//.. and it does not match patternChar, so it's not a match
					return null;
				} else {
					//.. and it matched patternChar. Back to the big loop
					break;
				}
			}
			// Same tests for non-obvious characters
			else if (Character.isJavaIdentifierPart(nameChar) && !Character.isUpperCase(nameChar)) {
				iName++;
			} else if (Character.isDigit(nameChar)) {
				if (patternChar == nameChar) break;
				iName++;
			} else  if (patternChar != nameChar) {
				return null;
			} else {
				break;
			}
		}
		// At this point, either name has been exhausted, or it is at an uppercase letter.
		// Since pattern is also at an uppercase letter
		if (segments == null) {
			segments = new int[parts*2];
		}
		segments[count++] = segmentStart;
		segments[count++] = segmentEnd - segmentStart;
		segmentStart = iName;
	}
}

/**
 * Answers all the regions in a given name matching a given <i>pattern</i>
 * pattern (e.g. "H*M??").
 * </p><p>
 * Each of these regions is made of its starting index and its length in the given
 * name. They are all concatenated in a single array of <code>int</code>
 * which therefore always has an even length.
 * </p><p>
 * Note that each region is disjointed from the following one.<br>
 * E.g. if the regions are <code>{ start1, length1, start2, length2 }</code>,
 * then <code>start1+length1</code> will always be smaller than
 * <code>start2</code>.
 * </p><p>
 * <pre>
 * Examples:
 * <ol>
 * <li>  pattern = "N???Po*Ex?eption"
 *  name = NullPointerException
 *  result:  { 0, 1, 4, 2, 11, 2, 14, 6 }</li>
 * <li>  pattern = "Ha*M*ent*"
 *  name = "HashMapEntry"
 *  result:  { 0, 2, 4, 1, 7, 3 }</li>
 * </ol></pre>
 *</p>
 * @see CharOperation#match(char[], char[], boolean) for more details on the
 * 	pattern match behavior
 *
 * @param pattern the given pattern
 * @param patternStart the given pattern start
 * @param patternEnd the given pattern end
 * @param name the given name
 * @param nameStart the given name start
 * @param nameEnd the given name end
 * @param isCaseSensitive flag to know if the matching should be case sensitive
 * @return an array of <code>int</code> having two slots per returned
 * 	regions (first one is the starting index of the region and the second
 * 	one the length of the region).<br>
 * 	Note that it may be <code>null</code> if the given name does not match
 * 	the pattern
 * @since 3.5
 */
public static final int[] getPatternMatchingRegions(
	String pattern,
	int patternStart,
	int patternEnd,
	String name,
	int nameStart,
	int nameEnd,
	boolean isCaseSensitive) {

	/* !!!!!!!!!! WARNING !!!!!!!!!!
	 * The algorithm used in this method has been fully inspired from
	 * CharOperation#match(char[], int, int, char[], int, int, boolean).
	 *
	 * So, if any change needs to be applied in the algorithm, do NOT forget
	 * to backport it in the CharOperation method!
	 */

	if (name == null) return null; // null name cannot match
	if (pattern == null) {
		// null pattern cannot match any region
		// see bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=264816
		return EMPTY_REGIONS;
	}
	int iPattern = patternStart;
	int iName = nameStart;

	// init segments parts
	if (patternEnd < 0)
		patternEnd = pattern.length();
	if (nameEnd < 0)
		nameEnd = name.length();
	int questions = 0;
	int parts = 0;
	char previous = 0;
	for (int i=patternStart; i<patternEnd; i++) {
		char ch = pattern.charAt(i);
		switch (ch) {
			case '?':
				questions++;
				break;
			case '*':
				break;
			default:
				switch (previous) {
					case 0 :
					case '?':
					case '*':
						parts++;
						break;
				}
		}
		previous = ch;
	}
	if (parts == 0) {
		if (questions <= (nameEnd - nameStart)) return EMPTY_REGIONS;
		return null;
	}
	int[] segments = new int[parts*2];

	/* check first segment */
	int count = 0;
	int start = iName;
	char patternChar = 0;
	previous = 0;
	while ((iPattern < patternEnd)
		&& (patternChar = pattern.charAt(iPattern)) != '*') {
		if (iName == nameEnd)
			return null;
		if (patternChar == '?') {
			switch (previous) {
				case 0:
				case '?':
					break;
				default:
					segments[count++] = start;
					segments[count++] = iPattern-start;
					break;
			}
		} else {
			if (isCaseSensitive) {
				if (patternChar != name.charAt(iName)) {
					return null;
				}
			} else if (ScannerHelper.toLowerCase(patternChar) != ScannerHelper.toLowerCase(name.charAt(iName))) {
				return null;
			}
			switch (previous) {
				case 0:
				case '?':
					start = iPattern;
					break;
			}
		}
		iName++;
		iPattern++;
		previous = patternChar;
	}
	/* check sequence of star+segment */
	int segmentStart;
	if (patternChar == '*') {
		if (iPattern > 0 && previous != '?') {
			segments[count++] = start;
			segments[count++] = iName-start;
			start = iName;
		}
		segmentStart = ++iPattern; // skip star
	} else {
		if (iName == nameEnd) {
			if (count == (parts*2)) return segments;
			int end = patternEnd;
			if (previous == '?') { // last char was a '?' => purge all trailing '?'
				while (pattern.charAt(--end-1) == '?') {
					if (end == start) {
						return new int[] { patternStart, patternEnd-patternStart };
					}
				}
			}
			return new int[] { start, end-start };
		}
		return null;
	}
	int prefixStart = iName;
	int previousCount = count;
	previous = patternChar;
	char previousSegment = patternChar;
	checkSegment : while (iName < nameEnd) {
		if (iPattern == patternEnd) {
			iPattern = segmentStart; // mismatch - restart current segment
			iName = ++prefixStart;
			previous = previousSegment;
			continue checkSegment;
		}
		/* segment is ending */
		if ((patternChar = pattern.charAt(iPattern)) == '*') {
			segmentStart = ++iPattern; // skip star
			if (segmentStart == patternEnd) {
				if (count < (parts*2)) {
					segments[count++] = start;
					segments[count++] = iName-start;
				}
				return segments;
			}
			switch (previous) {
				case '*':
				case '?':
					break;
				default:
					segments[count++] = start;
					segments[count++] = iName-start;
					break;
			}
			prefixStart = iName;
			start = prefixStart;
			previous = patternChar;
			previousSegment = patternChar;
			continue checkSegment;
		}
		/* check current name character */
		previousCount = count;
		if (patternChar == '?') {
			switch (previous) {
				case '*':
				case '?':
					break;
				default:
					segments[count++] = start;
					segments[count++] = iName-start;
					break;
			}
		} else {
			boolean mismatch;
			if (isCaseSensitive) {
				mismatch = name.charAt(iName) != patternChar;
			} else {
				mismatch = ScannerHelper.toLowerCase(name.charAt(iName)) != ScannerHelper.toLowerCase(patternChar);
			}
			if (mismatch) {
				iPattern = segmentStart; // mismatch - restart current segment
				iName = ++prefixStart;
				start = prefixStart;
				count = previousCount;
				previous = previousSegment;
				continue checkSegment;
			}
			switch (previous) {
				case '?':
					start = iName;
					break;
			}
		}
		iName++;
		iPattern++;
		previous = patternChar;
	}

	if ((segmentStart == patternEnd)
		|| (iName == nameEnd && iPattern == patternEnd)
		|| (iPattern == patternEnd - 1 && pattern.charAt(iPattern) == '*')) {
		if (count < (parts*2)) {
			segments[count++] = start;
			segments[count++] = iName-start;
		}
		return segments;
	}
	return null;
}
}
