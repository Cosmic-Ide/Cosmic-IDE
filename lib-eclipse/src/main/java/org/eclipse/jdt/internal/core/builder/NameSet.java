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

import org.eclipse.jdt.core.compiler.CharOperation;

public final class NameSet {

// to avoid using Enumerations, walk the individual values skipping nulls
public char[][] names;
public int elementSize; // number of elements in the table
public int threshold;

public NameSet(int size) {
	this.elementSize = 0;
	this.threshold = size; // size represents the expected number of elements
	int extraRoom = (int) (size * 1.5f);
	if (this.threshold == extraRoom)
		extraRoom++;
	this.names = new char[extraRoom][];
}

public char[] add(char[] name) {
	int length = this.names.length;
	int index = CharOperation.hashCode(name) % length;
	char[] current;
	while ((current = this.names[index]) != null) {
		if (CharOperation.equals(current, name)) return current;
		if (++index == length) index = 0;
	}
	this.names[index] = name;

	// assumes the threshold is never equal to the size of the table
	if (++this.elementSize > this.threshold) rehash();
	return name;
}

private void rehash() {
	NameSet newSet = new NameSet(this.elementSize * 2); // double the number of expected elements
	char[] current;
	for (int i = this.names.length; --i >= 0;)
		if ((current = this.names[i]) != null)
			newSet.add(current);

	this.names = newSet.names;
	this.elementSize = newSet.elementSize;
	this.threshold = newSet.threshold;
}

@Override
public String toString() {
	String s = ""; //$NON-NLS-1$
	char[] name;
	for (int i = 0, l = this.names.length; i < l; i++)
		if ((name = this.names[i]) != null)
			s += new String(name) + "\n"; //$NON-NLS-1$
	return s;
}
}
