/*******************************************************************************
 * Copyright (c) 2019 Sebastian Zarnekow and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Sebastian Zarnekow - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.util;

import java.util.Comparator;

/**
 * @since 3.18
 */
public class SortedCharArrays {

	// there may be better thresholds available for different scenarios
	public static final int BINARY_SEARCH_THRESHOLD = 16;

	/**
	 * @param target same as source array or new array with higher capacity
	 * @param idx position for new element
	 * @param currentCount the current number of elements in the source array
	 * @return given target array
	 */
	public static <T> T[] insertIntoArray(T[] src, T[] target, T entry, int idx, int currentCount) {
		if (src != target) {
			// src and target point to different instances
			// -> we need to copy the elements into the new result
			System.arraycopy(src, 0, target, 0, idx);
			System.arraycopy(src, idx, target, idx+1, currentCount - idx);
		} else if (idx != currentCount) {
			// src and target point to the same instance
			// -> we need to shift the elements one slot to the right
			System.arraycopy(src, idx, target, idx+1, currentCount - idx);
		}
		target[idx] = entry;
		return target;
	}

	/**
	 * Compares the two char arrays.
	 * Longer arrays are considered to be smaller than shorter arrays.
	 * Arrays with the same length are compared char by char lexicographically.
	 *
	 * @see Character#compare(char, char)
	 */
	public static int compareCharArray(char[] left, char[] right){
		if (left == right) {
			return 0;
		}
		int l = left.length;
		int diff = right.length - l;
		if (diff == 0) {
			for(int i = 0; i < l && (diff = left[i] - right[i]) == 0; i++) {
				// all logic is in the loop header
			}
		}
		return diff;
	}
	public static final Comparator<char[]> CHAR_ARR_COMPARATOR = SortedCharArrays::compareCharArray;

	/**
	 * Compares the two char-char arrays.
	 * Longer arrays are considered to be smaller than shorter arrays.
	 * Arrays with the same length are compared according to the logic in {@link #compareCharArray(char[], char[])}.
	 */
	public static int compareCharCharArray(char[][] left, char[][]right) {
		if (left == right) {
			return 0;
		}
		int l = left.length;
		int diff = right.length - l;
		if (diff == 0) {
			for(int i = 0; i < l && (diff = compareCharArray(left[i], right[i])) == 0; i++) {
				// all logic is in the loop header
			}
		}
		return diff;
	}
	public static final Comparator<char[][]> CHAR_CHAR_ARR_COMPARATOR = SortedCharArrays::compareCharCharArray;
}
