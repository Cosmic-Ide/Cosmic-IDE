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

import org.eclipse.jdt.core.util.ClassFormatException;
import org.eclipse.jdt.core.util.IConstantPool;
import org.eclipse.jdt.core.util.ILineNumberAttribute;

/**
 * Default implementation of ILineNumberAttribute.
 */
public class LineNumberAttribute
	extends ClassFileAttribute
	implements ILineNumberAttribute {

	private static final int[][] NO_ENTRIES = new int[0][0];
	private int lineNumberTableLength;
	private int[][] lineNumberTable;

	/**
	 * Constructor for LineNumberAttribute.
	 * @param classFileBytes
	 * @param constantPool
	 * @param offset
	 * @throws ClassFormatException
	 */
	public LineNumberAttribute(
		byte[] classFileBytes,
		IConstantPool constantPool,
		int offset)
		throws ClassFormatException {
		super(classFileBytes, constantPool, offset);

		final int length = u2At(classFileBytes, 6, offset);
		this.lineNumberTableLength = length;
		if (length != 0) {
			this.lineNumberTable = new int[length][2];
			int readOffset = 8;
			for (int i = 0; i < length; i++) {
				this.lineNumberTable[i][0] = u2At(classFileBytes, readOffset, offset);
				this.lineNumberTable[i][1] = u2At(classFileBytes, readOffset + 2, offset);
				readOffset += 4;
			}
		} else {
			this.lineNumberTable = NO_ENTRIES;
		}
	}
	/**
	 * @see ILineNumberAttribute#getLineNumberTable()
	 */
	@Override
	public int[][] getLineNumberTable() {
		return this.lineNumberTable;
	}

	/**
	 * @see ILineNumberAttribute#getLineNumberTableLength()
	 */
	@Override
	public int getLineNumberTableLength() {
		return this.lineNumberTableLength;
	}

}
