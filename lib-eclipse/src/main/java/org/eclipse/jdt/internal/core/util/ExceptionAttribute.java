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
import org.eclipse.jdt.core.util.ClassFormatException;
import org.eclipse.jdt.core.util.IConstantPool;
import org.eclipse.jdt.core.util.IConstantPoolConstant;
import org.eclipse.jdt.core.util.IConstantPoolEntry;
import org.eclipse.jdt.core.util.IExceptionAttribute;

/**
 * Default implementation of IExceptionAttribute.
 */
public class ExceptionAttribute extends ClassFileAttribute implements IExceptionAttribute {
	private int exceptionsNumber;
	private char[][] exceptionNames;
	private int[] exceptionIndexes;

	ExceptionAttribute(byte[] classFileBytes, IConstantPool constantPool, int offset) throws ClassFormatException {
		super(classFileBytes, constantPool, offset);
		this.exceptionsNumber = u2At(classFileBytes, 6, offset);
		int exceptionLength = this.exceptionsNumber;
		this.exceptionNames = CharOperation.NO_CHAR_CHAR;
		this.exceptionIndexes = org.eclipse.jdt.internal.compiler.util.Util.EMPTY_INT_ARRAY;
		if (exceptionLength != 0) {
			this.exceptionNames = new char[exceptionLength][];
			this.exceptionIndexes = new int[exceptionLength];
		}
		int readOffset = 8;
		IConstantPoolEntry constantPoolEntry;
		for (int i = 0; i < exceptionLength; i++) {
			this.exceptionIndexes[i] = u2At(classFileBytes, readOffset, offset);
			constantPoolEntry = constantPool.decodeEntry(this.exceptionIndexes[i]);
			if (constantPoolEntry.getKind() != IConstantPoolConstant.CONSTANT_Class) {
				throw new ClassFormatException(ClassFormatException.INVALID_CONSTANT_POOL_ENTRY);
			}
			this.exceptionNames[i] = constantPoolEntry.getClassInfoName();
			readOffset += 2;
		}
	}

	/**
	 * @see IExceptionAttribute#getExceptionIndexes()
	 */
	@Override
	public int[] getExceptionIndexes() {
		return this.exceptionIndexes;
	}

	/**
	 * @see IExceptionAttribute#getExceptionNames()
	 */
	@Override
	public char[][] getExceptionNames() {
		return this.exceptionNames;
	}

	/**
	 * @see IExceptionAttribute#getExceptionsNumber()
	 */
	@Override
	public int getExceptionsNumber() {
		return this.exceptionsNumber;
	}
}
