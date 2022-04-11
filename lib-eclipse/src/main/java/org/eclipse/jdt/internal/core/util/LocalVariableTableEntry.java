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
import org.eclipse.jdt.core.util.IConstantPoolConstant;
import org.eclipse.jdt.core.util.IConstantPoolEntry;
import org.eclipse.jdt.core.util.ILocalVariableTableEntry;

/**
 * Default implementation of ILocalVariableTableEntry
 */
public class LocalVariableTableEntry extends ClassFileStruct implements ILocalVariableTableEntry {

	private int startPC;
	private int length;
	private int nameIndex;
	private int descriptorIndex;
	private char[] name;
	private char[] descriptor;
	private int index;

	/**
	 * Constructor for LocalVariableTableEntry.
	 *
	 * @param classFileBytes
	 * @param constantPool
	 * @param offset
	 * @throws ClassFormatException
	 */
	public LocalVariableTableEntry(
		byte[] classFileBytes,
		IConstantPool constantPool,
		int offset) throws ClassFormatException {
			this.startPC = u2At(classFileBytes, 0, offset);
			this.length = u2At(classFileBytes, 2, offset);
			this.nameIndex = u2At(classFileBytes, 4, offset);
			this.descriptorIndex = u2At(classFileBytes, 6, offset);
			this.index = u2At(classFileBytes, 8, offset);
			IConstantPoolEntry constantPoolEntry = constantPool.decodeEntry(this.nameIndex);
			if (constantPoolEntry.getKind() != IConstantPoolConstant.CONSTANT_Utf8) {
				throw new ClassFormatException(ClassFormatException.INVALID_CONSTANT_POOL_ENTRY);
			}
			this.name = constantPoolEntry.getUtf8Value();
			constantPoolEntry = constantPool.decodeEntry(this.descriptorIndex);
			if (constantPoolEntry.getKind() != IConstantPoolConstant.CONSTANT_Utf8) {
				throw new ClassFormatException(ClassFormatException.INVALID_CONSTANT_POOL_ENTRY);
			}
			this.descriptor = constantPoolEntry.getUtf8Value();
		}

	/**
	 * @see ILocalVariableTableEntry#getStartPC()
	 */
	@Override
	public int getStartPC() {
		return this.startPC;
	}

	/**
	 * @see ILocalVariableTableEntry#getLength()
	 */
	@Override
	public int getLength() {
		return this.length;
	}

	/**
	 * @see ILocalVariableTableEntry#getNameIndex()
	 */
	@Override
	public int getNameIndex() {
		return this.nameIndex;
	}

	/**
	 * @see ILocalVariableTableEntry#getDescriptorIndex()
	 */
	@Override
	public int getDescriptorIndex() {
		return this.descriptorIndex;
	}

	/**
	 * @see ILocalVariableTableEntry#getIndex()
	 */
	@Override
	public int getIndex() {
		return this.index;
	}

	/**
	 * @see ILocalVariableTableEntry#getName()
	 */
	@Override
	public char[] getName() {
		return this.name;
	}

	/**
	 * @see ILocalVariableTableEntry#getDescriptor()
	 */
	@Override
	public char[] getDescriptor() {
		return this.descriptor;
	}

}
