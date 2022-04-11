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
 * HashSet of Object[]
 */
public final class HashSetOfArray implements Cloneable {

	// to avoid using Enumerations, walk the individual tables skipping nulls
	public Object[][] set;

	public int elementSize; // number of elements in the table
	int threshold;

	public HashSetOfArray() {
		this(13);
	}

	public HashSetOfArray(int size) {

		this.elementSize = 0;
		this.threshold = size; // size represents the expected number of elements
		int extraRoom = (int) (size * 1.75f);
		if (this.threshold == extraRoom)
			extraRoom++;
		this.set = new Object[extraRoom][];
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		HashSetOfArray result = (HashSetOfArray) super.clone();
		result.elementSize = this.elementSize;
		result.threshold = this.threshold;

		int length = this.set.length;
		result.set = new Object[length][];
		System.arraycopy(this.set, 0, result.set, 0, length);

		return result;
	}

	public boolean contains(Object[] array) {
		int length = this.set.length;
		int index = hashCode(array) % length;
		int arrayLength = array.length;
		Object[] currentArray;
		while ((currentArray = this.set[index]) != null) {
			if (currentArray.length == arrayLength && Util.equalArraysOrNull(currentArray, array))
				return true;
			if (++index == length) {
				index = 0;
			}
		}
		return false;
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

	public Object add(Object[] array) {
		int length = this.set.length;
		int index = hashCode(array) % length;
		int arrayLength = array.length;
		Object[] currentArray;
		while ((currentArray = this.set[index]) != null) {
			if (currentArray.length == arrayLength && Util.equalArraysOrNull(currentArray, array))
				return this.set[index] = array;
			if (++index == length) {
				index = 0;
			}
		}
		this.set[index] = array;

		// assumes the threshold is never equal to the size of the table
		if (++this.elementSize > this.threshold)
			rehash();
		return array;
	}

	public Object remove(Object[] array) {
		int length = this.set.length;
		int index = hashCode(array) % length;
		int arrayLength = array.length;
		Object[] currentArray;
		while ((currentArray = this.set[index]) != null) {
			if (currentArray.length == arrayLength && Util.equalArraysOrNull(currentArray, array)) {
				Object existing = this.set[index];
				this.elementSize--;
				this.set[index] = null;
				rehash();
				return existing;
			}
			if (++index == length) {
				index = 0;
			}
		}
		return null;
	}

	private void rehash() {

		HashSetOfArray newHashSet = new HashSetOfArray(this.elementSize * 2);		// double the number of expected elements
		Object[] currentArray;
		for (int i = this.set.length; --i >= 0;)
			if ((currentArray = this.set[i]) != null)
				newHashSet.add(currentArray);

		this.set = newHashSet.set;
		this.threshold = newHashSet.threshold;
	}

	public int size() {
		return this.elementSize;
	}

	@Override
	public String toString() {
		StringBuilder buffer = new StringBuilder();
		Object[] element;
		for (int i = 0, length = this.set.length; i < length; i++)
			if ((element = this.set[i]) != null) {
				buffer.append('{');
				for (int j = 0, length2 = element.length; j < length2; j++) {
					buffer.append(element[j]);
					if (j != length2-1)
						buffer.append(", "); //$NON-NLS-1$
				}
				buffer.append("}");  //$NON-NLS-1$
				if (i != length-1)
					buffer.append('\n');
			}
		return buffer.toString();
	}
}
