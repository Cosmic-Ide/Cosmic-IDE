/*******************************************************************************
 * Copyright (c) 2013 Jesper Steen Moeller and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Jesper Steen Moeller - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.core.util;

import org.eclipse.jdt.core.util.ClassFormatException;
import org.eclipse.jdt.core.util.IConstantPool;
import org.eclipse.jdt.core.util.IConstantPoolConstant;
import org.eclipse.jdt.core.util.IConstantPoolEntry;
import org.eclipse.jdt.core.util.IMethodParametersAttribute;

/**
 * @since 3.10
 */
public class MethodParametersAttribute extends ClassFileAttribute implements IMethodParametersAttribute {

	private static final char[][] NO_NAMES = new char[0][];
	private static final short[] NO_ACCES_FLAGS = new short[0];

	private final int numberOfEntries;
	private final char[][] names;
	private final short[] accessFlags;


	MethodParametersAttribute(byte[] classFileBytes, IConstantPool constantPool, int offset) throws ClassFormatException {
		super(classFileBytes, constantPool, offset);

		final int length = u1At(classFileBytes, 6, offset);
		this.numberOfEntries = length;
		if (length != 0) {
			int readOffset = offset + 7;
			this.names = new char[length][];
			this.accessFlags = new short[length];
			for (int i = 0; i < length; i++) {
				int nameIndex = u2At(classFileBytes, 0, readOffset);
				int mask = u2At(classFileBytes, 2, readOffset);
				readOffset += 4;
				if (nameIndex != 0) {
					IConstantPoolEntry constantPoolEntry = constantPool.decodeEntry(nameIndex);
					if (constantPoolEntry.getKind() != IConstantPoolConstant.CONSTANT_Utf8) {
						throw new ClassFormatException(ClassFormatException.INVALID_CONSTANT_POOL_ENTRY);
					}
					this.names[i] = constantPoolEntry.getUtf8Value();
				} else {
					this.names[i] = null;
				}
				this.accessFlags[i] = (short) (mask & 0xFFFF);
			}
		} else {
			this.names = NO_NAMES;
			this.accessFlags = NO_ACCES_FLAGS;
		}
	}

	@Override
	public int getMethodParameterLength() {
		return this.numberOfEntries;
	}

	@Override
	public char[] getParameterName(int i) {
		return this.names[i];
	}

	@Override
	public short getAccessFlags(int i) {
		return this.accessFlags[i];
	}
}
