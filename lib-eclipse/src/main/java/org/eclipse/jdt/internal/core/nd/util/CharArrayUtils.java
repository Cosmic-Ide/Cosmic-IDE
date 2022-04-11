/*******************************************************************************
 * Copyright (c) 2004, 2016 IBM Corporation and others.
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
 *     Andrew Ferguson (Symbian)
 *     Markus Schorn (Wind River Systems)
 *     Sergey Prigogin (Google)
 *******************************************************************************/
package org.eclipse.jdt.internal.core.nd.util;

import java.util.Arrays;

/**
 * A static utility class for char arrays.
 */
public class CharArrayUtils {
	/** @since 5.4 */
	public static final char[] EMPTY_CHAR_ARRAY = {};
	public static final char[] EMPTY = EMPTY_CHAR_ARRAY;
	/** @since 5.7 */
	public static final char[][] EMPTY_ARRAY_OF_CHAR_ARRAYS = {};

	private CharArrayUtils() {}

	public static final int hash(char[] str, int start, int length) {
		int h = 0;
		int end = start + length;

		for (int curr = start; curr < end; ++curr) {
			h = 31 * h + str[curr];
		}

		return h;
	}

	public static final int hash(char[] str) {
		return hash(str, 0, str.length);
	}

	public static final boolean equals(char[] str1, char[] str2) {
		return Arrays.equals(str1, str2);
	}

	public static final boolean equals(char[][] strarr1, char[][] strarr2) {
		if (strarr1 == strarr2) {
			return true;
		}
		if (strarr1 == null || strarr2 == null) {
			return false;
		}
		if (strarr1.length != strarr2.length) {
			return false;
		}
		for (int i = 0; i < strarr2.length; i++) {
			if (!Arrays.equals(strarr1[i], strarr2[i])) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Returns {@code true} if the contents of a character array are the same as contents
	 * of a string.
	 * @since 5.4
	 */
	public static final boolean equals(char[] str1, String str2) {
        int length = str1.length;
        if (str2.length() != length)
            return false;

        for (int i = 0; i < length; i++) {
            if (str1[i] != str2.charAt(i))
                return false;
        }
        return true;
	}

	/**
	 * Returns true iff the given array contains the given char at the given position
	 */
	public static final boolean hasCharAt(char toLookFor, int position, char[] toSearch) {
		if (toSearch.length <= position) {
			return false;
		}

		return toSearch[position] == toLookFor;
	}

	/**
	 * Returns {@code true} if the contents of a section of a character array are the same as contents of a string.
	 *
	 * @since 5.5
	 */
	public static final boolean equals(char[] str1, int start1, int length1, String str2) {
		if (length1 != str2.length() || str1.length < length1 + start1)
			return false;
		for (int i = 0; i < length1; ++i) {
			if (str1[start1++] != str2.charAt(i))
				return false;
		}
		return true;
	}

	/**
	 * Returns {@code true} if a prefix of the character array is the same as contents
	 * of a string.
	 * @since 5.4
	 */
	public static final boolean startsWith(char[] str1, String str2) {
		int len = str2.length();
		if (str1.length < len)
			return false;
		for (int i = 0; i < len; i++) {
            if (str1[i] != str2.charAt(i)) {
                return false;
            }
        }
        return true;
	}

	/**
	 * Implements a lexicographical comparator for char arrays. Comparison is done
	 * on a per char basis, not a code-point basis.
	 *
	 * @param str1 the first of the two char arrays to compare
	 * @param str2 the second of the two char arrays to compare
	 * @return  0 if str1==str2, -1 if str1 &lt; str2 and 1 if str1 &gt; str2
	 */
	/*
	 * aftodo - we should think about using the Character codepoint static methods
	 * if we move to Java 5
	 */
	public static final int compare(char[] str1, char[] str2) {
		if (str1 == str2)
			return 0;

		int end= Math.min(str1.length, str2.length);
		for (int i = 0; i < end; ++i) {
			int diff= str1[i] - str2[i];
			if (diff != 0)
				return diff;
		}

		return str1.length - str2.length;
	}

	/**
	 * Returns {@code true} if the contents of a section of a character array are the same as
	 * contents of another character array.
	 */
	public static final boolean equals(char[] str1, int start1, int length1, char[] str2) {
		if (length1 != str2.length || str1.length < length1 + start1)
			return false;
		if (str1 == str2 && start1 == 0)
		    return true;
		for (int i = 0; i < length1; ++i) {
			if (str1[start1++] != str2[i])
				return false;
		}

		return true;
	}

	public static final boolean equals(char[] str1, int start1, int length1, char[] str2, boolean ignoreCase) {
	    if (!ignoreCase)
	        return equals(str1, start1, length1, str2);

		if (length1 != str2.length || str1.length < start1 + length1)
			return false;

		for (int i = 0; i < length1; ++i) {
			if (Character.toLowerCase(str1[start1++]) != Character.toLowerCase(str2[i]))
				return false;
		}
		return true;
	}

	public static final char[] extract(char[] str, int start, int length) {
		if (start == 0 && length == str.length)
			return str;

		char[] copy = new char[length];
		System.arraycopy(str, start, copy, 0, length);
		return copy;
	}

	public static final char[] concat(char[] first, char[] second) {
		if (first == null)
			return second;
		if (second == null)
			return first;

		int length1 = first.length;
		int length2 = second.length;
		char[] result = new char[length1 + length2];
		System.arraycopy(first, 0, result, 0, length1);
		System.arraycopy(second, 0, result, length1, length2);
		return result;
	}

	public static final char[] concat(char[] first, char[] second, char[] third) {
		if (first == null)
			return concat(second, third);
		if (second == null)
			return concat(first, third);
		if (third == null)
			return concat(first, second);

		int length1 = first.length;
		int length2 = second.length;
		int length3 = third.length;
		char[] result = new char[length1 + length2 + length3];
		System.arraycopy(first, 0, result, 0, length1);
		System.arraycopy(second, 0, result, length1, length2);
		System.arraycopy(third, 0, result, length1 + length2, length3);
		return result;
	}

	public static final char[] concat(char[] first, char[] second, char[] third, char[] fourth) {
		if (first == null)
			return concat(second, third, fourth);
		if (second == null)
			return concat(first, third, fourth);
		if (third == null)
			return concat(first, second, fourth);
		if (fourth == null)
			return concat(first, second, third);

		int length1 = first.length;
		int length2 = second.length;
		int length3 = third.length;
		int length4 = fourth.length;
		char[] result = new char[length1 + length2 + length3 + length4];
		System.arraycopy(first, 0, result, 0, length1);
		System.arraycopy(second, 0, result, length1, length2);
		System.arraycopy(third, 0, result, length1 + length2, length3);
		System.arraycopy(fourth, 0, result, length1 + length2 + length3, length4);
		return result;
	}

	/**
	 * Answers a new array which is the concatenation of all the given arrays.
	 *
	 * @param toCatenate
	 * @since 3.12
	 */
	public static char[] concat(char[]... toCatenate) {
		int totalSize = 0;
		for (char[] next: toCatenate) {
			totalSize += next.length;
		}

		char[] result = new char[totalSize];
		int writeIndex = 0;
		for (char[] next: toCatenate) {
			if (next == null) {
				continue;
			}
			System.arraycopy(next, 0, result, writeIndex, next.length);
			writeIndex += next.length;
		}
		return result;
	}

	public static final char[] replace(char[] array, char[] toBeReplaced, char[] replacementChars) {
		int max = array.length;
		int replacedLength = toBeReplaced.length;
		int replacementLength = replacementChars.length;

		int[] starts = new int[5];
		int occurrenceCount = 0;

		if (!equals(toBeReplaced, replacementChars)) {
			next: for (int i = 0; i < max; i++) {
				int j = 0;
				while (j < replacedLength) {
					if (i + j == max)
						continue next;
					if (array[i + j] != toBeReplaced[j++])
						continue next;
				}
				if (occurrenceCount == starts.length) {
					System.arraycopy(starts, 0, starts = new int[occurrenceCount * 2], 0,
							occurrenceCount);
				}
				starts[occurrenceCount++] = i;
			}
		}
		if (occurrenceCount == 0)
			return array;
		char[] result = new char[max + occurrenceCount * (replacementLength - replacedLength)];
		int inStart = 0, outStart = 0;
		for (int i = 0; i < occurrenceCount; i++) {
			int offset = starts[i] - inStart;
			System.arraycopy(array, inStart, result, outStart, offset);
			inStart += offset;
			outStart += offset;
			System.arraycopy(
				replacementChars,
				0,
				result,
				outStart,
				replacementLength);
			inStart += replacedLength;
			outStart += replacementLength;
		}
		System.arraycopy(array, inStart, result, outStart, max - inStart);
		return result;
	}

	public static final char[][] subarray(char[][] array, int start, int end) {
		if (end == -1)
			end = array.length;
		if (start > end)
			return null;
		if (start < 0)
			return null;
		if (end > array.length)
			return null;

		char[][] result = new char[end - start][];
		System.arraycopy(array, start, result, 0, end - start);
		return result;
	}

	public static final char[] subarray(char[] array, int start, int end) {
		if (end == -1)
			end = array.length;
		if (start > end)
			return null;
		if (start < 0)
			return null;
		if (end > array.length)
			return null;

		char[] result = new char[end - start];
		System.arraycopy(array, start, result, 0, end - start);
		return result;
	}

	public static final int indexOf(char toBeFound, char[] array) {
		for (int i = 0; i < array.length; i++) {
			if (toBeFound == array[i])
				return i;
		}
		return -1;
	}

    public static int indexOf(char toBeFound, char[] buffer, int start, int end) {
        if (start < 0 || start > buffer.length || end > buffer.length)
            return -1;

        for (int i = start; i < end; i++) {
			if (toBeFound == buffer[i])
				return i;
        }
		return -1;
    }

	public static final int indexOf(char[] toBeFound, char[] array) {
	    if (toBeFound.length > array.length)
	        return -1;

	    int j = 0;
	    for (int i = 0; i < array.length; i++) {
	        if (toBeFound[j] == array[i]) {
	            if (++j == toBeFound.length)
	                return i - j + 1;
	        } else {
	        	j = 0;
	        }
	    }
	    return -1;
	}

	public static final int lastIndexOf(char[] toBeFound, char[] array) {
		return lastIndexOf(toBeFound, array, 0);
	}

	/**
	 * @since 5.11
	 */
	public static int lastIndexOf(char toBeFound, char[] array) {
		return lastIndexOf(toBeFound, array, 0);
	}

	/**
	 * @since 5.11
	 */
	public static int lastIndexOf(char toBeFound, char[] array, int fromIndex) {
		for (int i = array.length; --i >= fromIndex;) {
			if (array[i] == toBeFound) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * @since 5.11
	 */
	public static int lastIndexOf(char[] toBeFound, char[] array, int fromIndex) {
		int i = array.length;
		int j = toBeFound.length;
		while (true) {
			if (--j < 0)
				return i;
			if (--i < fromIndex)
				return -1;
			if (toBeFound[j] != array[i]) {
				i += toBeFound.length - j - 1;
				j = toBeFound.length;
			}
		}
	}

	static final public char[] trim(char[] chars) {
		if (chars == null)
			return null;

		int length = chars.length;
		int start = 0;
		while (start < length && chars[start] == ' ') {
			start++;
		}
		if (start == length)
			return EMPTY_CHAR_ARRAY;

		int end = length;
		while (--end > start && chars[end] == ' ') {
			// Nothing to do
		}
		end++;
		if (start == 0 && end == length)
			return chars;
		return subarray(chars, start, end);
	}

	static final public char[] lastSegment(char[] array, char[] separator) {
		int pos = lastIndexOf(separator, array);
		if (pos < 0)
			return array;
		return subarray(array, pos + separator.length, array.length);
	}

    /**
     * @param buff
     * @param i
     * @param charImage
     */
    public static void overWrite(char[] buff, int i, char[] charImage) {
        if (buff.length < i + charImage.length)
            return;
		System.arraycopy(charImage, 0, buff, i, charImage.length);
    }

    /**
     * Finds an array of chars in an array of arrays of chars.
     *
     * @return offset where the array was found or {@code -1}
     */
    public static int indexOf(final char[] searchFor, final char[][] searchIn) {
		for (int i = 0; i < searchIn.length; i++) {
			if (equals(searchIn[i], searchFor)) {
				return i;
			}
		}
		return -1;
    }

    /**
     * Converts a {@link StringBuilder} to a character array.
     * @since 5.5
     */
	public static char[] extractChars(StringBuilder buf) {
		final int len = buf.length();
		if (len == 0)
			return EMPTY_CHAR_ARRAY;
		char[] result= new char[len];
		buf.getChars(0, len, result, 0);
		return result;
	}

	public static char[] subarray(char[] inputString, int index) {
		if (inputString.length <= index) {
			return EMPTY_CHAR_ARRAY;
		}

		char[] result = new char[inputString.length - index];
		System.arraycopy(inputString, index, result, 0, result.length);
		return result;
	}

	public static boolean startsWith(char[] fieldDescriptor, char c) {
		return fieldDescriptor.length > 0 && fieldDescriptor[0] == c;
	}

	/**
	 * If the given array is null, returns the empty array. Otherwise, returns the argument.
	 */
	public static char[] notNull(char[] contents) {
		if (contents == null) {
			return EMPTY_CHAR_ARRAY;
		}
		return contents;
	}

	public static boolean endsWith(char[] fieldDescriptor, char c) {
		if (fieldDescriptor.length == 0) {
			return false;
		}
		return fieldDescriptor[fieldDescriptor.length - 1] == c;
	}
}
