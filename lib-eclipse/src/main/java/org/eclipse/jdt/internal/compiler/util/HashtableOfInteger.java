/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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

/**
  *	Hashtable for Integer keys.
  */

public final class HashtableOfInteger {

	// to avoid using Enumerations, walk the individual tables skipping nulls
	public Integer keyTable[];
	public Object valueTable[];

	public int elementSize; // number of elements in the table
	int threshold;

	public HashtableOfInteger() {
		this(13);
	}

	public HashtableOfInteger(int size) {

		this.elementSize = 0;
		this.threshold = size; // size represents the expected number of elements
		int extraRoom = (int) (size * 1.75f);
		if (this.threshold == extraRoom)
			extraRoom++;
		this.keyTable = new Integer[extraRoom];
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
		HashtableOfInteger result = (HashtableOfInteger) super.clone();
		result.elementSize = this.elementSize;
		result.threshold = this.threshold;

		int length = this.keyTable.length;
		result.keyTable = new Integer[length];
		System.arraycopy(this.keyTable, 0, result.keyTable, 0, length);

		length = this.valueTable.length;
		result.valueTable = new Object[length];
		System.arraycopy(this.valueTable, 0, result.valueTable, 0, length);
		return result;
	}

	public boolean containsKey(int key) {
		Integer intKey = Integer.valueOf(key);
		int length = this.keyTable.length,
			index = intKey.hashCode() % length;
		Integer currentKey;
		while ((currentKey = this.keyTable[index]) != null) {
			if (currentKey.equals(intKey))
				return true;
			if (++index == length) {
				index = 0;
			}
		}
		return false;
	}

	public Object get(int key) {
		Integer intKey = Integer.valueOf(key);
		int length = this.keyTable.length,
			index = intKey.hashCode() % length;
		Integer currentKey;
		while ((currentKey = this.keyTable[index]) != null) {
			if (currentKey.equals(intKey))
				return this.valueTable[index];
			if (++index == length) {
				index = 0;
			}
		}
		return null;
	}

	public Object put(int key, Object value) {
		Integer intKey = Integer.valueOf(key);
		int length = this.keyTable.length,
			index = intKey.hashCode() % length;
		Integer currentKey;
		while ((currentKey = this.keyTable[index]) != null) {
			if (currentKey.equals(intKey))
				return this.valueTable[index] = value;
			if (++index == length) {
				index = 0;
			}
		}
		this.keyTable[index] = intKey;
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
	public void putUnsafely(int key, Object value) {
		Integer intKey = Integer.valueOf(key);
		int length = this.keyTable.length,
			index = intKey.hashCode() % length;
		while (this.keyTable[index] != null) {
			if (++index == length) {
				index = 0;
			}
		}
		this.keyTable[index] = intKey;
		this.valueTable[index] = value;

		// assumes the threshold is never equal to the size of the table
		if (++this.elementSize > this.threshold) {
			rehash();
		}
	}

	public Object removeKey(int key) {
		Integer intKey = Integer.valueOf(key);
		int length = this.keyTable.length,
			index = intKey.hashCode() % length;
		Integer currentKey;
		while ((currentKey = this.keyTable[index]) != null) {
			if (currentKey.equals(intKey)) {
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

		HashtableOfInteger newHashtable = new HashtableOfInteger(this.elementSize * 2);		// double the number of expected elements
		Integer currentKey;
		for (int i = this.keyTable.length; --i >= 0;)
			if ((currentKey = this.keyTable[i]) != null)
				newHashtable.putUnsafely(currentKey, this.valueTable[i]);

		this.keyTable = newHashtable.keyTable;
		this.valueTable = newHashtable.valueTable;
		this.threshold = newHashtable.threshold;
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
				s += this.keyTable[i] + " -> " + object.toString() + "\n"; 	//$NON-NLS-2$ //$NON-NLS-1$
		return s;
	}
}
