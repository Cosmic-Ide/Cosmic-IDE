/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
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
package org.eclipse.jdt.internal.core.index;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.util.SimpleSet;

public class EntryResult {

private char[] word;
private Object[] documentTables;
private SimpleSet documentNames;

public EntryResult(char[] word, Object table) {
	this.word = word;
	if (table != null)
		this.documentTables = new Object[] {table};
}
public void addDocumentName(String documentName) {
	if (this.documentNames == null)
		this.documentNames = new SimpleSet(3);
	this.documentNames.add(documentName);
}
public void addDocumentTable(Object table) {
	if (this.documentTables != null) {
		int length = this.documentTables.length;
		System.arraycopy(this.documentTables, 0, this.documentTables = new Object[length + 1], 0, length);
		this.documentTables[length] = table;
	} else {
		this.documentTables = new Object[] {table};
	}
}
public char[] getWord() {
	return this.word;
}
public String[] getDocumentNames(Index index) throws java.io.IOException {
	if (this.documentTables != null) {
		int length = this.documentTables.length;
		if (length == 1 && this.documentNames == null) { // have a single table
			Object offset = this.documentTables[0];
			int[] numbers = index.diskIndex.readDocumentNumbers(offset);
			String[] names = new String[numbers.length];
			for (int i = 0, l = numbers.length; i < l; i++)
				names[i] = index.diskIndex.readDocumentName(numbers[i]);
			return names;
		}

		for (int i = 0; i < length; i++) {
			Object offset = this.documentTables[i];
			int[] numbers = index.diskIndex.readDocumentNumbers(offset);
			for (int j = 0, k = numbers.length; j < k; j++)
				addDocumentName(index.diskIndex.readDocumentName(numbers[j]));
		}
	}

	if (this.documentNames == null)
		return CharOperation.NO_STRINGS;

	String[] names = new String[this.documentNames.elementSize];
	int count = 0;
	Object[] values = this.documentNames.values;
	for (int i = 0, l = values.length; i < l; i++)
		if (values[i] != null)
			names[count++] = (String) values[i];
	return names;
}
public boolean isEmpty() {
	return this.documentTables == null && this.documentNames == null;
}
}
