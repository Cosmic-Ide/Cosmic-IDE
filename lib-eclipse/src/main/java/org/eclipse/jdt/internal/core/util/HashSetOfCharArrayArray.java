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

import org.eclipse.jdt.core.compiler.CharOperation;

/**
 * HashSet of char[][]
 */
public final class HashSetOfCharArrayArray implements Cloneable {

	// to avoid using Enumerations, walk the individual tables skipping nulls
	public char[][][] set;

	public int elementSize; // number of elements in the table
	int threshold;

	public HashSetOfCharArrayArray() {
		this(13);
	}

	public HashSetOfCharArrayArray(int size) {

		this.elementSize = 0;
		this.threshold = size; // size represents the expected number of elements
		int extraRoom = (int) (size * 1.75f);
		if (this.threshold == extraRoom)
			extraRoom++;
		this.set = new char[extraRoom][][];
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		HashSetOfCharArrayArray result = (HashSetOfCharArrayArray) super.clone();
		result.elementSize = this.elementSize;
		result.threshold = this.threshold;

		int length = this.set.length;
		result.set = new char[length][][];
		System.arraycopy(this.set, 0, result.set, 0, length);

		return result;
	}

	public boolean contains(char[][] array) {
		int length = this.set.length;
		int index = hashCode(array) % length;
		int arrayLength = array.length;
		char[][] currentArray;
		while ((currentArray = this.set[index]) != null) {
			if (currentArray.length == arrayLength && CharOperation.equals(currentArray, array))
				return true;
			if (++index == length) {
				index = 0;
			}
		}
		return false;
	}

	private int hashCode(char[][] element) {
		return hashCode(element, element.length);
	}

	private int hashCode(char[][] element, int length) {
		int hash = 0;
		for (int i = length-1; i >= 0; i--)
			hash = Util.combineHashCodes(hash, CharOperation.hashCode(element[i]));
		return hash & 0x7FFFFFFF;
	}

	public char[][] add(char[][] array) {
		int length = this.set.length;
		int index = hashCode(array) % length;
		int arrayLength = array.length;
		char[][] currentArray;
		while ((currentArray = this.set[index]) != null) {
			if (currentArray.length == arrayLength && CharOperation.equals(currentArray, array))
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

	public char[][] remove(char[][] array) {
		int length = this.set.length;
		int index = hashCode(array) % length;
		int arrayLength = array.length;
		char[][] currentArray;
		while ((currentArray = this.set[index]) != null) {
			if (currentArray.length == arrayLength && CharOperation.equals(currentArray, array)) {
				char[][] existing = this.set[index];
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
		HashSetOfCharArrayArray newHashSet = new HashSetOfCharArrayArray(this.elementSize * 2);		// double the number of expected elements
		char[][] currentArray;
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
		for (int i = 0, length = this.set.length; i < length; i++) {
			char[][] arrayArray = this.set[i];
			if (arrayArray != null) {
				buffer.append("{"); //$NON-NLS-1$
				for (int j = 0, length2 = arrayArray.length; j < length2; j++) {
					char[] array = arrayArray[j];
					buffer.append('{');
					for (int k = 0, length3 = array.length; k < length3; k++) {
						buffer.append('\'');
						buffer.append(array[k]);
						buffer.append('\'');
						if (k != length3-1)
							buffer.append(", "); //$NON-NLS-1$
					}
					buffer.append('}');
					if (j != length2-1)
						buffer.append(", "); //$NON-NLS-1$
				}
				buffer.append("}");  //$NON-NLS-1$
				if (i != length-1)
					buffer.append('\n');
			}
		}
		return buffer.toString();
	}
}
