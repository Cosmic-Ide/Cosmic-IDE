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
package org.eclipse.jdt.internal.compiler.util;

import org.eclipse.jdt.core.compiler.CharOperation;

/**
 * Hashtable of {char[] --> Object }
 */
public final class HashtableOfObject implements Cloneable {

	/** Max array size accepted by JVM */
	public static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 2;

	// to avoid using Enumerations, walk the individual tables skipping nulls
	public char[] keyTable[];
	public Object valueTable[];

	public int elementSize; // number of elements in the table
	int threshold;

	public HashtableOfObject() {
		this(13);
	}

	/**
	 * @param size preferred table size
	 * @throws NegativeArraySizeException if size is negative
	 * @throws OutOfMemoryError if size exceeds {@link #MAX_ARRAY_SIZE}
	 */
	public HashtableOfObject(int size) {
		if (size < 0) {
			throw new NegativeArraySizeException("Bad attempt to create table with " + size + " elements"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		this.elementSize = 0;
		this.threshold = size; // size represents the expected number of elements
		int extraRoom = (int) (size * 1.75f);
		if (this.threshold == extraRoom) {
			extraRoom++;
		}
		// Check for integer overflow & max array size limit
		if (extraRoom < 1 || extraRoom > MAX_ARRAY_SIZE) {
			extraRoom = calculateNewSize(size);
		}
		this.keyTable = new char[extraRoom][];
		this.valueTable = new Object[extraRoom];
	}

	public void clear() {
		for (int i = this.keyTable.length; --i >= 0;) {
			this.keyTable[i] = null;
			this.valueTable[i] = null;
		}
		this.elementSize = 0;
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		HashtableOfObject result = (HashtableOfObject) super.clone();
		result.elementSize = this.elementSize;
		result.threshold = this.threshold;

		int length = this.keyTable.length;
		result.keyTable = new char[length][];
		System.arraycopy(this.keyTable, 0, result.keyTable, 0, length);

		length = this.valueTable.length;
		result.valueTable = new Object[length];
		System.arraycopy(this.valueTable, 0, result.valueTable, 0, length);
		return result;
	}

	public boolean containsKey(char[] key) {
		int length = this.keyTable.length,
			index = CharOperation.hashCode(key) % length;
		int keyLength = key.length;
		char[] currentKey;
		while ((currentKey = this.keyTable[index]) != null) {
			if (currentKey.length == keyLength && CharOperation.equals(currentKey, key))
				return true;
			if (++index == length) {
				index = 0;
			}
		}
		return false;
	}

	public Object get(char[] key) {
		int length = this.keyTable.length,
			index = CharOperation.hashCode(key) % length;
		int keyLength = key.length;
		char[] currentKey;
		while ((currentKey = this.keyTable[index]) != null) {
			if (currentKey.length == keyLength && CharOperation.equals(currentKey, key))
				return this.valueTable[index];
			if (++index == length) {
				index = 0;
			}
		}
		return null;
	}

	public Object put(char[] key, Object value) {
		int length = this.keyTable.length,
			index = CharOperation.hashCode(key) % length;
		int keyLength = key.length;
		char[] currentKey;
		while ((currentKey = this.keyTable[index]) != null) {
			if (currentKey.length == keyLength && CharOperation.equals(currentKey, key))
				return this.valueTable[index] = value;
			if (++index == length) {
				index = 0;
			}
		}
		this.keyTable[index] = key;
		this.valueTable[index] = value;

		// assumes the threshold is never equal to the size of the table
		if (++this.elementSize > this.threshold)
			rehash();
		return value;
	}

	/**
	 * Put a value at the index of the given using the local hash code computation.
	 * <p>
	 * Note that this is an unsafe put as there's no prior verification whether
	 * the given key already exists in the table or not.
	 * </p>
	 * @param key The key of the table entry
	 * @param value The value of the table entry
	 */
	public void putUnsafely(char[] key, Object value) {
		int length = this.keyTable.length,
			index = CharOperation.hashCode(key) % length;
		while (this.keyTable[index] != null) {
			if (++index == length) {
				index = 0;
			}
		}
		this.keyTable[index] = key;
		this.valueTable[index] = value;

		// assumes the threshold is never equal to the size of the table
		if (++this.elementSize > this.threshold) {
			rehash();
		}
	}

	public Object removeKey(char[] key) {
		int length = this.keyTable.length,
			index = CharOperation.hashCode(key) % length;
		int keyLength = key.length;
		char[] currentKey;
		while ((currentKey = this.keyTable[index]) != null) {
			if (currentKey.length == keyLength && CharOperation.equals(currentKey, key)) {
				Object value = this.valueTable[index];
				this.elementSize--;
				this.keyTable[index] = null;
				this.valueTable[index] = null;
				rehash();
				return value;
			}
			if (++index == length) {
				index = 0;
			}
		}
		return null;
	}

	private void rehash() {
		int newSize = calculateNewSize(this.elementSize);
		HashtableOfObject newHashtable = new HashtableOfObject(newSize);
		char[] currentKey;
		for (int i = this.keyTable.length; --i >= 0;)
			if ((currentKey = this.keyTable[i]) != null)
				newHashtable.putUnsafely(currentKey, this.valueTable[i]);

		this.keyTable = newHashtable.keyTable;
		this.valueTable = newHashtable.valueTable;
		this.threshold = newHashtable.threshold;
	}

	/**
	 * Tries to double the number of given elements but returns {@link #MAX_ARRAY_SIZE} - 2 in case new value would
	 * overflow
	 *
	 * @return new map size that fits to JVM limits or throws an error
	 */
	public static int calculateNewSize(int currentSize) {
		if(currentSize == 0) {
			return 1;
		}
		if(currentSize < 0) {
			throw new NegativeArraySizeException("Bad attempt to calculate table size with " + currentSize + " elements"); //$NON-NLS-1$ //$NON-NLS-2$
		}

		// double the number of expected elements
		int newSize = currentSize * 2;

		// int overflow or JVM limit hit
		if (newSize + 1 < 1 || newSize + 1 >= MAX_ARRAY_SIZE) {
			// add half of space between current size and max array size
			newSize = currentSize + (MAX_ARRAY_SIZE - currentSize) / 2;
			if (newSize + 1 < 1 || newSize + 1 >= MAX_ARRAY_SIZE) {
				// use max possible value
				newSize = MAX_ARRAY_SIZE - 2;
				if (newSize <= currentSize) {
					throw new OutOfMemoryError("Unable to increase table size over " + currentSize + " elements"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
		}
		return newSize;
	}

	public int storageSize() {
		return this.keyTable.length;
	}

	public int size() {
		return this.elementSize;
	}

	@Override
	public String toString() {
		String s = ""; //$NON-NLS-1$
		Object object;
		for (int i = 0, length = this.valueTable.length; i < length; i++)
			if ((object = this.valueTable[i]) != null)
				s += new String(this.keyTable[i]) + " -> " + object.toString() + "\n"; 	//$NON-NLS-2$ //$NON-NLS-1$
		return s;
	}
}
