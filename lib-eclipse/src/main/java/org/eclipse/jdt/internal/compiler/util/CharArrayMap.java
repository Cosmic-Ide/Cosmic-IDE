/*******************************************************************************
 * Copyright (c) 2021 jkubitz and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     jkubitz - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * This map avoids hashing. This is suitable for few elements or long char arrays because it uses vectorized equals.
 * Vectorized equals is several times faster then calculating hashCode in a loop. This class is not thread safe and
 * callers are responsible for thread safety.
 *
 * @author jkubitz
 */
public final class CharArrayMap<P> implements CharArrayMapper<P> {
	private char[] keyTable[];
	private P valueTable[];

	/**
	 * The number of key-value mappings contained in this map.
	 */
	private int size;

	public CharArrayMap() {
		this(0); // usually not very large
	}

	public CharArrayMap(int estimatedSize) {
		int capacity = estimatedSize > 0 ? estimatedSize : 0;
		this.size = 0;
		this.keyTable = new char[capacity][];
		@SuppressWarnings("unchecked")
		P[] x = (P[]) new Object[capacity];
		this.valueTable = x;
	}

	@Override
	public Collection<P> values() {
		return Arrays.stream(this.valueTable).filter(Objects::nonNull).collect(Collectors.toList());
	}

	@Override
	public Collection<char[]> keys() {
		return Arrays.stream(this.keyTable).filter(Objects::nonNull).collect(Collectors.toList());
	}

	@Override
	public boolean containsKey(char[] key) {
		for (int i = 0; i < this.size; i++) {
			if (Arrays.equals(this.keyTable[i], key)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public P get(char[] key) {
		for (int i = 0; i < this.size; i++) {
			if (Arrays.equals(this.keyTable[i], key)) {
				return this.valueTable[i];
			}
		}
		return null;
	}

	@Override
	public P put(char[] key, P value) {
		int i = 0;
		for (; i < this.size; i++) {
			if (Arrays.equals(this.keyTable[i], key)) {
				P previous = this.valueTable[i];
				this.valueTable[i] = value;
				return previous;
			}
		}

		if (i >= this.keyTable.length) {
			grow();
		}
		this.keyTable[i] = key;
		this.valueTable[i] = value;
		this.size++;
		// assumes the threshold is never equal to the size of the table
		return null;
	}

	void transferTo(CharArrayMapper<P> bigMap) {
		for (int i = 0; i < this.size; i++) {
			if (this.keyTable[i] != null) {
				bigMap.put(this.keyTable[i], this.valueTable[i]);
			}
		}
	}

	private void grow() {
		int capacity = this.keyTable.length > 1 ? this.keyTable.length : 1;
		int newCapacity = capacity * 2;
		this.keyTable = Arrays.copyOfRange(this.keyTable, 0, newCapacity);
		this.valueTable = Arrays.copyOfRange(this.valueTable, 0, newCapacity);
	}

	@Override
	public int size() {
		return this.size;
	}

	@Override
	public String toString() {
		return CharArrayMapper.toString(this);
	}
}
