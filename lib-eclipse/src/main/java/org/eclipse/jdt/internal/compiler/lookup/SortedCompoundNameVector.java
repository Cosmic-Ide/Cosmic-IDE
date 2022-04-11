/*******************************************************************************
 * Copyright (c) 2019 Sebastian Zarnekow and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Sebastian Zarnekow - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.lookup;

import java.util.Arrays;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.util.SortedCharArrays;

/**
 * Sorted and simplified version of previously existed CompoundNameVector
 */
final class SortedCompoundNameVector {

	static int INITIAL_SIZE = 10;

	int size;
	char[][][] elements;

	public SortedCompoundNameVector() {
		this.size = 0;
		this.elements = new char[INITIAL_SIZE][][];
	}

	public boolean add(char[][] newElement) {
		int idx = Arrays.binarySearch(this.elements, 0, this.size, newElement, SortedCharArrays.CHAR_CHAR_ARR_COMPARATOR);
		if (idx < 0) {
			this.elements = SortedCharArrays.insertIntoArray(
					this.elements,
					this.size < this.elements.length ? this.elements : new char[this.elements.length * 2][][],
					newElement,
					-(idx + 1),
					this.size++);
			return true;
		}
		return false;
	}

	public char[][] elementAt(int index) {
		return this.elements[index];
	}

	@Override
	public String toString() {
		StringBuilder buffer = new StringBuilder();
		for (int i = 0; i < this.size; i++) {
			buffer.append(CharOperation.toString(this.elements[i])).append("\n"); //$NON-NLS-1$
		}
		return buffer.toString();
	}

}
