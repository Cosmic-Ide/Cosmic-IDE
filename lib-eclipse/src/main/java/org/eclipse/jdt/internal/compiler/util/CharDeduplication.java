/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
 *     Joerg Kubitz    - threadlocal refactoring, all ASCII chars
 *                     - (copied content from PublicScanner.java / Scanner.java)
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.util;

import java.lang.ref.SoftReference;
import java.util.Arrays;
import java.util.function.Supplier;

public class CharDeduplication {

	// ----- immutable static part (thread safe): ----

	static final char[] ASCII_CHARS[] = new char[128][];
	static {
		for (int i = 0; i < ASCII_CHARS.length; i++) {
			ASCII_CHARS[i] = new char[] { (char) i };
		}
	}
	public static final int TABLE_SIZE = 30; // XXX thats not a prime -> bad for hashing, nor a power of 2 -> expensive
												// modulo computation
	public static final int INTERNAL_TABLE_SIZE = 6; // 30*6 =180 entries

	public static final int OPTIMIZED_LENGTH = 6;

	private final static char[] CHAR_ARRAY0 = new char[0];

	/** avoid OOME by additional CharDeduplication memory **/
	static final class CacheReference<T> {
		private SoftReference<T> reference;
		private final Supplier<? extends T> supplier;

		CacheReference(Supplier<? extends T> supplier) {
			this.supplier = supplier;
			this.reference = new SoftReference<>(supplier.get());
		}

		T get() {
			T referent = this.reference.get();
			if (referent == null) {
				referent = this.supplier.get();
				this.reference = new SoftReference<>(referent);
			}
			return referent;
		}
	}

	private final static ThreadLocal<CacheReference<CharDeduplication>> mutableCache = ThreadLocal.withInitial(()->new CacheReference<>(CharDeduplication::new));

	private static final char[] optimizedCurrentTokenSource1(char[] source, int startPosition) {
		// optimization at no speed cost of 99.5 % of the singleCharIdentifier
		char charOne = source[startPosition];
		if (charOne < ASCII_CHARS.length) {
			return ASCII_CHARS[charOne];
		}
		return new char[] { charOne };
	}

	/** @return an instance that is *not* thread safe. To be used in a single thread only. **/
	public static CharDeduplication getThreadLocalInstance() {
		return mutableCache.get().get();
	}

	// ----- mutable non-static part (not thread safe!): ----

	/** single threaded only **/
	public final char[][][][] charArray_length = new char[OPTIMIZED_LENGTH - 1][TABLE_SIZE][INTERNAL_TABLE_SIZE][];

	int newEntry2 = 0;
	int newEntry3 = 0;
	int newEntry4 = 0;
	int newEntry5 = 0;
	int newEntry6 = 0;

	private CharDeduplication() {
		init();
	}

	private void init() {
		for (int i = 0; i < OPTIMIZED_LENGTH - 1; i++) {
			final char[] initCharArray = new char[i + 2];
			for (int j = 0; j < TABLE_SIZE; j++) {
				for (int k = 0; k < INTERNAL_TABLE_SIZE; k++) {
					this.charArray_length[i][j][k] = initCharArray;
				}
			}
		}
	}

	/** public for test purpose only **/
	@Deprecated
	public void reset() {
		init();
	}

	/**
	 * like Arrays.copyOfRange(source, from, to) but returns a cached instance of the former result if
	 * available
	 * 
	 * @param from
	 *                 start index (inclusive)
	 * @param to
	 *                 end index (exclusive)
	 * @return source[from..to-1]
	 * @see java.util.Arrays#copyOfRange(char[], int, int)
	 **/
	public char[] sharedCopyOfRange(char[] source, int from, int to) {
		int length = to - from;
		switch (length) { // see OptimizedLength
			case 1:
				return optimizedCurrentTokenSource1(source, from);
			case 2:
				return optimizedCurrentTokenSource2(source, from);
			case 3:
				return optimizedCurrentTokenSource3(source, from);
			case 4:
				return optimizedCurrentTokenSource4(source, from);
			case 5:
				return optimizedCurrentTokenSource5(source, from);
			case 6:
				return optimizedCurrentTokenSource6(source, from);
			case 0:
				return CHAR_ARRAY0;
		}
		return Arrays.copyOfRange(source, from, to);
	}

	private final char[] optimizedCurrentTokenSource2(char[] source, int startPosition) {

		char[] src = source;
		int start = startPosition;
		char c0, c1;
		int hash = (((c0 = src[start]) << 6) + (c1 = src[start + 1])) % TABLE_SIZE;
		char[][] table = this.charArray_length[0][hash];
		int i = this.newEntry2;
		while (++i < INTERNAL_TABLE_SIZE) {
			char[] charArray = table[i];
			if ((c0 == charArray[0]) && (c1 == charArray[1]))
				return charArray;
		}
		// ---------other side---------
		i = -1;
		int max = this.newEntry2;
		while (++i <= max) {
			char[] charArray = table[i];
			if ((c0 == charArray[0]) && (c1 == charArray[1]))
				return charArray;
		}
		// --------add the entry-------
		if (++max >= INTERNAL_TABLE_SIZE)
			max = 0;
		char[] r;
		System.arraycopy(src, start, r = new char[2], 0, 2);
		return table[this.newEntry2 = max] = r;
	}

	private final char[] optimizedCurrentTokenSource3(char[] source, int startPosition) {
		char[] src = source;
		int start = startPosition;
		char c0, c1 = src[start + 1], c2;
		int hash = (((c0 = src[start]) << 6) + (c2 = src[start + 2])) % TABLE_SIZE;
		char[][] table = this.charArray_length[1][hash];
		int i = this.newEntry3;
		while (++i < INTERNAL_TABLE_SIZE) {
			char[] charArray = table[i];
			if ((c0 == charArray[0]) && (c1 == charArray[1]) && (c2 == charArray[2]))
				return charArray;
		}
		// ---------other side---------
		i = -1;
		int max = this.newEntry3;
		while (++i <= max) {
			char[] charArray = table[i];
			if ((c0 == charArray[0]) && (c1 == charArray[1]) && (c2 == charArray[2]))
				return charArray;
		}
		// --------add the entry-------
		if (++max >= INTERNAL_TABLE_SIZE)
			max = 0;
		char[] r;
		System.arraycopy(src, start, r = new char[3], 0, 3);
		return table[this.newEntry3 = max] = r;
	}

	private final char[] optimizedCurrentTokenSource4(char[] source, int startPosition) {
		char[] src = source;
		int start = startPosition;
		char c0, c1 = src[start + 1], c2, c3 = src[start + 3];
		int hash = (((c0 = src[start]) << 6) + (c2 = src[start + 2])) % TABLE_SIZE;
		char[][] table = this.charArray_length[2][hash];
		int i = this.newEntry4;
		while (++i < INTERNAL_TABLE_SIZE) {
			char[] charArray = table[i];
			if ((c0 == charArray[0]) && (c1 == charArray[1]) && (c2 == charArray[2]) && (c3 == charArray[3]))
				return charArray;
		}
		// ---------other side---------
		i = -1;
		int max = this.newEntry4;
		while (++i <= max) {
			char[] charArray = table[i];
			if ((c0 == charArray[0]) && (c1 == charArray[1]) && (c2 == charArray[2]) && (c3 == charArray[3]))
				return charArray;
		}
		// --------add the entry-------
		if (++max >= INTERNAL_TABLE_SIZE)
			max = 0;
		char[] r;
		System.arraycopy(src, start, r = new char[4], 0, 4);
		return table[this.newEntry4 = max] = r;
	}

	private final char[] optimizedCurrentTokenSource5(char[] source, int startPosition) {
		char[] src = source;
		int start = startPosition;
		char c0, c1 = src[start + 1], c2, c3 = src[start + 3], c4;
		int hash = (((c0 = src[start]) << 12) + ((c2 = src[start + 2]) << 6) + (c4 = src[start + 4])) % TABLE_SIZE;
		char[][] table = this.charArray_length[3][hash];
		int i = this.newEntry5;
		while (++i < INTERNAL_TABLE_SIZE) {
			char[] charArray = table[i];
			if ((c0 == charArray[0]) && (c1 == charArray[1]) && (c2 == charArray[2]) && (c3 == charArray[3])
					&& (c4 == charArray[4]))
				return charArray;
		}
		// ---------other side---------
		i = -1;
		int max = this.newEntry5;
		while (++i <= max) {
			char[] charArray = table[i];
			if ((c0 == charArray[0]) && (c1 == charArray[1]) && (c2 == charArray[2]) && (c3 == charArray[3])
					&& (c4 == charArray[4]))
				return charArray;
		}
		// --------add the entry-------
		if (++max >= INTERNAL_TABLE_SIZE)
			max = 0;
		char[] r;
		System.arraycopy(src, start, r = new char[5], 0, 5);
		return table[this.newEntry5 = max] = r;
	}

	private final char[] optimizedCurrentTokenSource6(char[] source, int startPosition) {
		char[] src = source;
		int start = startPosition;
		char c0, c1 = src[start + 1], c2, c3 = src[start + 3], c4, c5 = src[start + 5];
		int hash = (((c0 = src[start]) << 12) + ((c2 = src[start + 2]) << 6) + (c4 = src[start + 4])) % TABLE_SIZE;
		char[][] table = this.charArray_length[4][hash];
		int i = this.newEntry6;
		while (++i < INTERNAL_TABLE_SIZE) {
			char[] charArray = table[i];
			if ((c0 == charArray[0]) && (c1 == charArray[1]) && (c2 == charArray[2]) && (c3 == charArray[3])
					&& (c4 == charArray[4]) && (c5 == charArray[5]))
				return charArray;
		}
		// ---------other side---------
		i = -1;
		int max = this.newEntry6;
		while (++i <= max) {
			char[] charArray = table[i];
			if ((c0 == charArray[0]) && (c1 == charArray[1]) && (c2 == charArray[2]) && (c3 == charArray[3])
					&& (c4 == charArray[4]) && (c5 == charArray[5]))
				return charArray;
		}
		// --------add the entry-------
		if (++max >= INTERNAL_TABLE_SIZE)
			max = 0;
		char[] r;
		System.arraycopy(src, start, r = new char[6], 0, 6);
		return table[this.newEntry6 = max] = r;
	}
}
