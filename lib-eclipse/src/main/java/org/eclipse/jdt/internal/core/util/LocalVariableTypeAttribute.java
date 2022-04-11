/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
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
import org.eclipse.jdt.core.util.ILocalVariableTypeTableAttribute;
import org.eclipse.jdt.core.util.ILocalVariableTypeTableEntry;

/**
 * Default implementation of ILocalVariableTypeAttribute.
 */
public class LocalVariableTypeAttribute
	extends ClassFileAttribute
	implements ILocalVariableTypeTableAttribute {

	private static final ILocalVariableTypeTableEntry[] NO_ENTRIES = new ILocalVariableTypeTableEntry[0];
	private int localVariableTypeTableLength;
	private ILocalVariableTypeTableEntry[] localVariableTypeTableEntries;

	/**
	 * Constructor for LocalVariableTypeAttribute.
	 * @param classFileBytes
	 * @param constantPool
	 * @param offset
	 * @throws ClassFormatException
	 */
	public LocalVariableTypeAttribute(
		byte[] classFileBytes,
		IConstantPool constantPool,
		int offset)
		throws ClassFormatException {
		super(classFileBytes, constantPool, offset);
		final int length = u2At(classFileBytes, 6, offset);
		this.localVariableTypeTableLength = length;
		if (length != 0) {
			int readOffset = 8;
			this.localVariableTypeTableEntries = new ILocalVariableTypeTableEntry[length];
			for (int i = 0; i < length; i++) {
				this.localVariableTypeTableEntries[i] = new LocalVariableTypeTableEntry(classFileBytes, constantPool, offset + readOffset);
				readOffset += 10;
			}
		} else {
			this.localVariableTypeTableEntries = NO_ENTRIES;
		}
	}
	/**
	 * @see ILocalVariableTypeTableAttribute#getLocalVariableTypeTable()
	 */
	@Override
	public ILocalVariableTypeTableEntry[] getLocalVariableTypeTable() {
		return this.localVariableTypeTableEntries;
	}

	/**
	 * @see ILocalVariableTypeTableAttribute#getLocalVariableTypeTableLength()
	 */
	@Override
	public int getLocalVariableTypeTableLength() {
		return this.localVariableTypeTableLength;
	}
}
