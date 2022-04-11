/*******************************************************************************
 * Copyright (c) 2000, 2014 IBM Corporation and others.
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
package org.eclipse.jdt.internal.core;

import java.util.Arrays;
import java.util.HashMap;

import org.eclipse.jdt.core.IType;

public final class TypeVector {
	static int INITIAL_SIZE = 10;
	static int MIN_ELEMENTS_FOR_HASHSET = 8;

	public int size;
	int maxSize;
	IType[] elements;
	/**
	 * {@link HashMap} that contains the same elements as the {@link #elements}. Used to speed up {@link #contains}
	 * for large lists. It is lazily constructed the first time it is needed for vectors larger than
	 * {@link #MIN_ELEMENTS_FOR_HASHSET}. Set to null if not constructed yet. The keys and values are the same
	 */
	private HashMap<IType, IType> elementSet = null;

	public final static IType[] NoElements = new IType[0];

public TypeVector() {
	this.maxSize = INITIAL_SIZE;
	this.size = 0;
	this.elements = new IType[this.maxSize];
}
public TypeVector(IType[] types) {
	this.size = types.length;
	this.maxSize = this.size + 1; // when an element is added, it assumes that the length is > 0
	this.elements = new IType[this.maxSize];
	System.arraycopy(types, 0, this.elements, 0, this.size);
}
public TypeVector(IType type) {
	this.maxSize = INITIAL_SIZE;
	this.size = 1;
	this.elements = new IType[this.maxSize];
	this.elements[0] = type;
}
public void add(IType newElement) {
	if (this.size == this.maxSize)	// knows that size starts <= maxSize
		System.arraycopy(this.elements, 0, (this.elements = new IType[this.maxSize *= 2]), 0, this.size);
	this.elements[this.size++] = newElement;
	if (this.elementSet != null) {
		this.elementSet.put(newElement, newElement);
	}
}
public void addAll(IType[] newElements) {
	if (this.size + newElements.length >= this.maxSize) {
		this.maxSize = this.size + newElements.length;	// assume no more elements will be added
		System.arraycopy(this.elements, 0, (this.elements = new IType[this.maxSize]), 0, this.size);
	}
	System.arraycopy(newElements, 0, this.elements, this.size, newElements.length);
	this.size += newElements.length;
	if (this.elementSet != null) {
		for (IType next : newElements) {
			this.elementSet.put(next, next);
		}
	}
}
public boolean contains(IType element) {
	constructElementSetIfNecessary();

	if (this.elementSet != null) {
		return this.elementSet.containsKey(element);
	}

	for (int i = this.size; --i >= 0;)
		if (element.equals(this.elements[i]))
			return true;
	return false;
}
/**
 *
 */
private void constructElementSetIfNecessary() {
	if (this.elementSet == null && this.size >= MIN_ELEMENTS_FOR_HASHSET) {
		this.elementSet = new HashMap<>();
		for (IType next : this.elements) {
			this.elementSet.put(next, next);
		}
	}
}
public TypeVector copy() {
	TypeVector clone = new TypeVector();
	int length = this.elements.length;
	System.arraycopy(this.elements, 0, clone.elements = new IType[length], 0, length);
	clone.size = this.size;
	clone.maxSize = this.maxSize;
	return clone;
}
public IType elementAt(int index) {
	return this.elements[index];
}
public IType[] elements() {

	// do not resize to 0 if empty since may add more elements later
	if (this.size == 0) return NoElements;

	if (this.size < this.maxSize) {
		this.maxSize = this.size;
		System.arraycopy(this.elements, 0, (this.elements = new IType[this.maxSize]), 0, this.size);
	}
	return this.elements;
}

public IType remove(IType element) {
	if (this.elementSet != null) {
		IType value = this.elementSet.get(element);
		if (value == element) {
			this.elementSet.remove(element);
		} else {
			return null;
		}
	}
	// assumes only one occurrence of the element exists
	for (int i = this.size; --i >= 0;)
		if (element == this.elements[i]) {
			// shift the remaining elements down one spot
			System.arraycopy(this.elements, i + 1, this.elements, i, --this.size - i);
			this.elements[this.size] = null;
			return element;
		}
	return null;
}
public void removeAll() {
	Arrays.fill(this.elements, null);
	this.elementSet = null;
	this.size = 0;
}
@Override
public String toString() {
	StringBuilder buffer = new StringBuilder("["); //$NON-NLS-1$
	for (int i = 0; i < this.size; i++) {
		buffer.append("\n"); //$NON-NLS-1$
		buffer.append(this.elements[i]);
	}
	buffer.append("\n]"); //$NON-NLS-1$
	return buffer.toString();
}
}
