/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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
package org.eclipse.jdt.internal.core.util;

/**
 * Hashtable of {Object[] --> Object }
 */
public final class HashtableOfArrayToObject implements Cloneable {

	// to avoid using Enumerations, walk the individual tables skipping nulls
	public Object[][] keyTable;
	public Object[] valueTable;

	public int elementSize; // number of elements in the table
	int threshold;

	public HashtableOfArrayToObject() {
		this(13);
	}

	public HashtableOfArrayToObject(int size) {

		this.elementSize = 0;
		this.threshold = size; // size represents the expected number of elements
		int extraRoom = (int) (size * 1.75f);
		if (this.threshold == extraRoom)
			extraRoom++;
		this.keyTable = new Object[extraRoom][];
		this.valueTable = new Object[extraRoom];
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		HashtableOfArrayToObject result = (HashtableOfArrayToObject) super.clone();
		result.elementSize = this.elementSize;
		result.threshold = this.threshold;

		int length = this.keyTable.length;
		result.keyTable = new Object[length][];
		System.arraycopy(this.keyTable, 0, result.keyTable, 0, length);

		length = this.valueTable.length;
		result.valueTable = new Object[length];
		System.arraycopy(this.valueTable, 0, result.valueTable, 0, length);
		return result;
	}

	public boolean containsKey(Object[] key) {
		int length = this.keyTable.length;
		int index = hashCode(key) % length;
		int keyLength = key.length;
		Object[] currentKey;
		while ((currentKey = this.keyTable[index]) != null) {
			if (currentKey.length == keyLength && Util.equalArraysOrNull(currentKey, key))
				return true;
			if (++index == length) {
				index = 0;
			}
		}
		return false;
	}

	public Object get(Object[] key) {
		int length = this.keyTable.length;
		int index = hashCode(key) % length;
		int keyLength = key.length;
		Object[] currentKey;
		while ((currentKey = this.keyTable[index]) != null) {
			if (currentKey.length == keyLength && Util.equalArraysOrNull(currentKey, key))
				return this.valueTable[index];
			if (++index == length) {
				index = 0;
			}
		}
		return null;
	}

	public int getIndex(Object[] key) {
		int length = this.keyTable.length;
		int index = hashCode(key) % length;
		int keyLength = key.length;
		Object[] currentKey;
		while ((currentKey = this.keyTable[index]) != null) {
			if (currentKey.length == keyLength && Util.equalArraysOrNull(currentKey, key))
				return index;
			if (++index == length) {
				index = 0;
			}
		}
		return -1;
	}

	public Object[] getKey(Object[] key, int keyLength) {
		int length = this.keyTable.length;
		int index = hashCode(key, keyLength) % length;
		Object[] currentKey;
		while ((currentKey = this.keyTable[index]) != null) {
			if (currentKey.length == keyLength && Util.equalArrays(currentKey, key, keyLength))
				return currentKey;
			if (++index == length) {
				index = 0;
			}
		}
		return null;
	}

	private int hashCode(Object[] element) {
		return hashCode(element, element.length);
	}

	private int hashCode(Object[] element, int length) {
		int hash = 0;
		for (int i = length-1; i >= 0; i--)
			hash = Util.combineHashCodes(hash, element[i].hashCode());
		return hash & 0x7FFFFFFF;
	}

	public Object put(Object[] key, Object value) {
		int length = this.keyTable.length;
		int index = hashCode(key) % length;
		int keyLength = key.length;
		Object[] currentKey;
		while ((currentKey = this.keyTable[index]) != null) {
			if (currentKey.length == keyLength && Util.equalArraysOrNull(currentKey, key))
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

	public Object removeKey(Object[] key) {
		int length = this.keyTable.length;
		int index = hashCode(key) % length;
		int keyLength = key.length;
		Object[] currentKey;
		while ((currentKey = this.keyTable[index]) != null) {
			if (currentKey.length == keyLength && Util.equalArraysOrNull(currentKey, key)) {
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

		HashtableOfArrayToObject newHashtable = new HashtableOfArrayToObject(this.elementSize * 2);		// double the number of expected elements
		Object[] currentKey;
		for (int i = this.keyTable.length; --i >= 0;)
			if ((currentKey = this.keyTable[i]) != null)
				newHashtable.put(currentKey, this.valueTable[i]);

		this.keyTable = newHashtable.keyTable;
		this.valueTable = newHashtable.valueTable;
		this.threshold = newHashtable.threshold;
	}

	public int size() {
		return this.elementSize;
	}

	@Override
	public String toString() {
		StringBuilder buffer = new StringBuilder();
		Object[] element;
		for (int i = 0, length = this.keyTable.length; i < length; i++)
			if ((element = this.keyTable[i]) != null) {
				buffer.append('{');
				for (int j = 0, length2 = element.length; j < length2; j++) {
					buffer.append(element[j]);
					if (j != length2-1)
						buffer.append(", "); //$NON-NLS-1$
				}
				buffer.append("} -> ");  //$NON-NLS-1$
				buffer.append(this.valueTable[i]);
				if (i != length-1)
					buffer.append('\n');
			}
		return buffer.toString();
	}
}
