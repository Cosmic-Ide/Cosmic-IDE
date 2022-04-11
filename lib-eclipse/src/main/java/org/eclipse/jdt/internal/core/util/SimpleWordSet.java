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

public final class SimpleWordSet {

// to avoid using Enumerations, walk the individual values skipping nulls
public char[][] words;
public int elementSize; // number of elements in the table
public int threshold;

public SimpleWordSet(int size) {
	this.elementSize = 0;
	this.threshold = size; // size represents the expected number of elements
	int extraRoom = (int) (size * 1.5f);
	if (this.threshold == extraRoom)
		extraRoom++;
	this.words = new char[extraRoom][];
}

public char[] add(char[] word) {
	int length = this.words.length;
	int index = CharOperation.hashCode(word) % length;
	char[] current;
	while ((current = this.words[index]) != null) {
		if (CharOperation.equals(current, word)) return current;
		if (++index == length) index = 0;
	}
	this.words[index] = word;

	// assumes the threshold is never equal to the size of the table
	if (++this.elementSize > this.threshold) rehash();
	return word;
}

public boolean includes(char[] word) {
	int length = this.words.length;
	int index = CharOperation.hashCode(word) % length;
	char[] current;
	while ((current = this.words[index]) != null) {
		if (CharOperation.equals(current, word)) return true;
		if (++index == length) index = 0;
	}
	return false;
}

private void rehash() {
	SimpleWordSet newSet = new SimpleWordSet(this.elementSize * 2); // double the number of expected elements
	char[] current;
	for (int i = this.words.length; --i >= 0;)
		if ((current = this.words[i]) != null)
			newSet.add(current);

	this.words = newSet.words;
	this.elementSize = newSet.elementSize;
	this.threshold = newSet.threshold;
}
}
