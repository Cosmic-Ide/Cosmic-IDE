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
package org.eclipse.jdt.internal.core.builder;

public class StringSet {

// to avoid using Enumerations, walk the individual values skipping nulls
public String[] values;
public int elementSize; // number of elements in the table
public int threshold;

public StringSet(int size) {
	this.elementSize = 0;
	this.threshold = size; // size represents the expected number of elements
	int extraRoom = (int) (size * 1.5f);
	if (this.threshold == extraRoom)
		extraRoom++;
	this.values = new String[extraRoom];
}

public boolean add(String value) {
	int length = this.values.length;
	int index = (value.hashCode() & 0x7FFFFFFF) % length;
	String current;
	while ((current = this.values[index]) != null) {
		if (value.equals(current)) return false; // did not add it since it already existed
		if (++index == length) index = 0;
	}
	this.values[index] = value;

	// assumes the threshold is never equal to the size of the table
	if (++this.elementSize > this.threshold) rehash();
	return true;
}

public void clear() {
	for (int i = this.values.length; --i >= 0;)
		this.values[i] = null;
	this.elementSize = 0;
}

public boolean includes(String value) {
	int length = this.values.length;
	int index = (value.hashCode() & 0x7FFFFFFF) % length;
	String current;
	while ((current = this.values[index]) != null) {
		if (value.equals(current)) return true;
		if (++index == length) index = 0;
	}
	return false;
}

private void rehash() {
	StringSet newSet = new StringSet(this.elementSize * 2); // double the number of expected elements
	String current;
	for (int i = this.values.length; --i >= 0;)
		if ((current = this.values[i]) != null)
			newSet.add(current);

	this.values = newSet.values;
	this.elementSize = newSet.elementSize;
	this.threshold = newSet.threshold;
}

@Override
public String toString() {
	String s = ""; //$NON-NLS-1$
	String value;
	for (int i = 0, l = this.values.length; i < l; i++)
		if ((value = this.values[i]) != null)
			s += value + "\n"; //$NON-NLS-1$
	return s;
}
}
