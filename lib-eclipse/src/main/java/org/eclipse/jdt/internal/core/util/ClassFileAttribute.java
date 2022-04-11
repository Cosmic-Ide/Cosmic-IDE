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
import org.eclipse.jdt.core.util.IClassFileAttribute;
import org.eclipse.jdt.core.util.IConstantPool;
import org.eclipse.jdt.core.util.IConstantPoolConstant;
import org.eclipse.jdt.core.util.IConstantPoolEntry;

/**
 * Default implementation of IClassFileAttribute
 */
public class ClassFileAttribute extends ClassFileStruct implements IClassFileAttribute {
	public static final IClassFileAttribute[] NO_ATTRIBUTES = new IClassFileAttribute[0];
	private long attributeLength;
	private int attributeNameIndex;
	private char[] attributeName;

	public ClassFileAttribute(byte[] classFileBytes, IConstantPool constantPool, int offset) throws ClassFormatException {
		this.attributeNameIndex = u2At(classFileBytes, 0, offset);
		this.attributeLength = u4At(classFileBytes, 2, offset);
		IConstantPoolEntry constantPoolEntry = constantPool.decodeEntry(this.attributeNameIndex);
		if (constantPoolEntry.getKind() != IConstantPoolConstant.CONSTANT_Utf8) {
			throw new ClassFormatException(ClassFormatException.INVALID_CONSTANT_POOL_ENTRY);
		}
		this.attributeName = constantPoolEntry.getUtf8Value();
	}

	@Override
	public int getAttributeNameIndex() {
		return this.attributeNameIndex;
	}

	/**
	 * @see IClassFileAttribute#getAttributeName()
	 */
	@Override
	public char[] getAttributeName() {
		return this.attributeName;
	}

	/**
	 * @see IClassFileAttribute#getAttributeLength()
	 */
	@Override
	public long getAttributeLength() {
		return this.attributeLength;
	}

}
