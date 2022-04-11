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

public class QualifiedNameSet {

// to avoid using Enumerations, walk the individual values skipping nulls
public char[][][] qualifiedNames;
public int elementSize; // number of elements in the table
public int threshold;

public QualifiedNameSet(int size) {
	this.elementSize = 0;
	this.threshold = size; // size represents the expected number of elements
	int extraRoom = (int) (size * 1.5f);
	if (this.threshold == extraRoom)
		extraRoom++;
	this.qualifiedNames = new char[extraRoom][][];
}

public char[][] add(char[][] qualifiedName) {
	int qLength = qualifiedName.length;
	if (qLength == 0) return CharOperation.NO_CHAR_CHAR;

	int length = this.qualifiedNames.length;
	int index = CharOperation.hashCode(qualifiedName[qLength - 1]) % length;
	char[][] current;
	while ((current = this.qualifiedNames[index]) != null) {
		if (CharOperation.equals(current, qualifiedName)) return current;
		if (++index == length) index = 0;
	}
	this.qualifiedNames[index] = qualifiedName;

	// assumes the threshold is never equal to the size of the table
	if (++this.elementSize > this.threshold) rehash();
	return qualifiedName;
}

private void rehash() {
	QualifiedNameSet newSet = new QualifiedNameSet(this.elementSize * 2); // double the number of expected elements
	char[][] current;
	for (int i = this.qualifiedNames.length; --i >= 0;)
		if ((current = this.qualifiedNames[i]) != null)
			newSet.add(current);

	this.qualifiedNames = newSet.qualifiedNames;
	this.elementSize = newSet.elementSize;
	this.threshold = newSet.threshold;
}

@Override
public String toString() {
	String s = ""; //$NON-NLS-1$
	char[][] qualifiedName;
	for (int i = 0, l = this.qualifiedNames.length; i < l; i++)
		if ((qualifiedName = this.qualifiedNames[i]) != null)
			s += CharOperation.toString(qualifiedName) + "\n"; //$NON-NLS-1$
	return s;
}
}
