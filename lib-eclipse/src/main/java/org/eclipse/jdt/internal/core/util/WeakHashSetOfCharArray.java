/*******************************************************************************
 * Copyright (c) 2004, 2013 IBM Corporation and others.
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
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

import org.eclipse.jdt.core.compiler.CharOperation;

/**
 * A hashset of char[] whose values can be garbage collected.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class WeakHashSetOfCharArray {

	public static class HashableWeakReference extends WeakReference {
		public int hashCode;
		public HashableWeakReference(char[] referent, ReferenceQueue queue) {
			super(referent, queue);
			this.hashCode = CharOperation.hashCode(referent);
		}
		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof HashableWeakReference)) return false;
			char[] referent = (char[]) get();
			char[] other = (char[]) ((HashableWeakReference) obj).get();
			if (referent == null) return other == null;
			return CharOperation.equals(referent, other);
		}
		@Override
		public int hashCode() {
			return this.hashCode;
		}
		@Override
		public String toString() {
			char[] referent = (char[]) get();
			if (referent == null) return "[hashCode=" + this.hashCode + "] <referent was garbage collected>"; //$NON-NLS-1$  //$NON-NLS-2$
			return "[hashCode=" + this.hashCode + "] \"" + new String(referent) + '\"'; //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	HashableWeakReference[] values;
	public int elementSize; // number of elements in the table
	int threshold;
	ReferenceQueue referenceQueue = new ReferenceQueue();

	public WeakHashSetOfCharArray() {
		this(5);
	}

	public WeakHashSetOfCharArray(int size) {
		this.elementSize = 0;
		this.threshold = size; // size represents the expected number of elements
		int extraRoom = (int) (size * 1.75f);
		if (this.threshold == extraRoom)
			extraRoom++;
		this.values = new HashableWeakReference[extraRoom];
	}

	/*
	 * Adds the given char array to this set.
	 * If a char array that is equals to the given char array already exists, do nothing.
	 * Returns the existing char array or the new char array if not found.
	 */
	public char[] add(char[] array) {
		cleanupGarbageCollectedValues();
		int valuesLength = this.values.length,
			index = (CharOperation.hashCode(array) & 0x7FFFFFFF) % valuesLength;
		HashableWeakReference currentValue;
		while ((currentValue = this.values[index]) != null) {
			char[] referent;
			if (CharOperation.equals(array, referent = (char[]) currentValue.get())) {
				return referent;
			}
			if (++index == valuesLength) {
				index = 0;
			}
		}
		this.values[index] = new HashableWeakReference(array, this.referenceQueue);

		// assumes the threshold is never equal to the size of the table
		if (++this.elementSize > this.threshold)
			rehash();

		return array;
	}

	private void addValue(HashableWeakReference value) {
		char[] array = (char[]) value.get();
		if (array == null) return;
		int valuesLength = this.values.length;
		int index = (value.hashCode & 0x7FFFFFFF) % valuesLength;
		HashableWeakReference currentValue;
		while ((currentValue = this.values[index]) != null) {
			if (CharOperation.equals(array, (char[]) currentValue.get())) {
				return;
			}
			if (++index == valuesLength) {
				index = 0;
			}
		}
		this.values[index] = value;

		// assumes the threshold is never equal to the size of the table
		if (++this.elementSize > this.threshold)
			rehash();
	}

	private void cleanupGarbageCollectedValues() {
		HashableWeakReference toBeRemoved;
		while ((toBeRemoved = (HashableWeakReference) this.referenceQueue.poll()) != null) {
			int hashCode = toBeRemoved.hashCode;
			int valuesLength = this.values.length;
			int index = (hashCode & 0x7FFFFFFF) % valuesLength;
			HashableWeakReference currentValue;
			while ((currentValue = this.values[index]) != null) {
				if (currentValue == toBeRemoved) {
					// replace the value at index with the last value with the same hash
					int sameHash = index;
					int current;
					while ((currentValue = this.values[current = (sameHash + 1) % valuesLength]) != null && currentValue.hashCode == hashCode)
						sameHash = current;
					this.values[index] = this.values[sameHash];
					this.values[sameHash] = null;
					this.elementSize--;
					break;
				}
				if (++index == valuesLength) {
					index = 0;
				}
			}
		}
	}

	public boolean contains(char[] array) {
		return get(array) != null;
	}

	/*
	 * Return the char array that is in this set and that is equals to the given char array.
	 * Return null if not found.
	 */
	public char[] get(char[] array) {
		cleanupGarbageCollectedValues();
		int valuesLength = this.values.length;
		int index = (CharOperation.hashCode(array) & 0x7FFFFFFF) % valuesLength;
		HashableWeakReference currentValue;
		while ((currentValue = this.values[index]) != null) {
			char[] referent;
			if (CharOperation.equals(array, referent = (char[]) currentValue.get())) {
				return referent;
			}
			if (++index == valuesLength) {
				index = 0;
			}
		}
		return null;
	}

	private void rehash() {
		WeakHashSetOfCharArray newHashSet = new WeakHashSetOfCharArray(this.elementSize * 2);		// double the number of expected elements
		newHashSet.referenceQueue = this.referenceQueue;
		HashableWeakReference currentValue;
		for (int i = 0, length = this.values.length; i < length; i++)
			if ((currentValue = this.values[i]) != null)
				newHashSet.addValue(currentValue);

		this.values = newHashSet.values;
		this.threshold = newHashSet.threshold;
		this.elementSize = newHashSet.elementSize;
	}

	/*
	 * Removes the char array that is in this set and that is equals to the given char array.
	 * Return the char array that was in the set, or null if not found.
	 */
	public char[] remove(char[] array) {
		cleanupGarbageCollectedValues();
		int valuesLength = this.values.length;
		int index = (CharOperation.hashCode(array) & 0x7FFFFFFF) % valuesLength;
		HashableWeakReference currentValue;
		while ((currentValue = this.values[index]) != null) {
			char[] referent;
			if (CharOperation.equals(array, referent = (char[]) currentValue.get())) {
				this.elementSize--;
				this.values[index] = null;
				rehash();
				return referent;
			}
			if (++index == valuesLength) {
				index = 0;
			}
		}
		return null;
	}

	public int size() {
		return this.elementSize;
	}

	@Override
	public String toString() {
		StringBuilder buffer = new StringBuilder("{"); //$NON-NLS-1$
		for (int i = 0, length = this.values.length; i < length; i++) {
			HashableWeakReference value = this.values[i];
			if (value != null) {
				char[] ref = (char[]) value.get();
				if (ref != null) {
					buffer.append('\"');
					buffer.append(ref);
					buffer.append("\", "); //$NON-NLS-1$
				}
			}
		}
		buffer.append("}"); //$NON-NLS-1$
		return buffer.toString();
	}
}
