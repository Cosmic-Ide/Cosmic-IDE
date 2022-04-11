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
import org.eclipse.jdt.core.util.IInnerClassesAttributeEntry;

/**
 * Default implementation of IInnerClassesAttributeEntry
 */
public class InnerClassesAttributeEntry
	extends ClassFileStruct
	implements IInnerClassesAttributeEntry {

	private int innerClassNameIndex;
	private int outerClassNameIndex;
	private int innerNameIndex;
	private char[] innerClassName;
	private char[] outerClassName;
	private char[] innerName;
	private int accessFlags;

	public InnerClassesAttributeEntry(byte classFileBytes[], IConstantPool constantPool, int offset)
		throws ClassFormatException {
		this.innerClassNameIndex = u2At(classFileBytes, 0, offset);
		this.outerClassNameIndex = u2At(classFileBytes, 2, offset);
		this.innerNameIndex = u2At(classFileBytes, 4, offset);
		this.accessFlags = u2At(classFileBytes, 6, offset);
		IConstantPoolEntry constantPoolEntry;
		if (this.innerClassNameIndex != 0) {
			constantPoolEntry = constantPool.decodeEntry(this.innerClassNameIndex);
			if (constantPoolEntry.getKind() != IConstantPoolConstant.CONSTANT_Class) {
				throw new ClassFormatException(ClassFormatException.INVALID_CONSTANT_POOL_ENTRY);
			}
			this.innerClassName = constantPoolEntry.getClassInfoName();
		}
		if (this.outerClassNameIndex != 0) {
			constantPoolEntry = constantPool.decodeEntry(this.outerClassNameIndex);
			if (constantPoolEntry.getKind() != IConstantPoolConstant.CONSTANT_Class) {
				throw new ClassFormatException(ClassFormatException.INVALID_CONSTANT_POOL_ENTRY);
			}
			this.outerClassName = constantPoolEntry.getClassInfoName();
		}
		if (this.innerNameIndex != 0) {
			constantPoolEntry = constantPool.decodeEntry(this.innerNameIndex);
			if (constantPoolEntry.getKind() != IConstantPoolConstant.CONSTANT_Utf8) {
				throw new ClassFormatException(ClassFormatException.INVALID_CONSTANT_POOL_ENTRY);
			}
			this.innerName = constantPoolEntry.getUtf8Value();
		}
	}

	/**
	 * @see IInnerClassesAttributeEntry#getAccessFlags()
	 */
	@Override
	public int getAccessFlags() {
		return this.accessFlags;
	}

	/**
	 * @see IInnerClassesAttributeEntry#getInnerClassName()
	 */
	@Override
	public char[] getInnerClassName() {
		return this.innerClassName;
	}

	/**
	 * @see IInnerClassesAttributeEntry#getInnerClassNameIndex()
	 */
	@Override
	public int getInnerClassNameIndex() {
		return this.innerClassNameIndex;
	}

	/**
	 * @see IInnerClassesAttributeEntry#getInnerName()
	 */
	@Override
	public char[] getInnerName() {
		return this.innerName;
	}

	/**
	 * @see IInnerClassesAttributeEntry#getInnerNameIndex()
	 */
	@Override
	public int getInnerNameIndex() {
		return this.innerNameIndex;
	}

	/**
	 * @see IInnerClassesAttributeEntry#getOuterClassName()
	 */
	@Override
	public char[] getOuterClassName() {
		return this.outerClassName;
	}

	/**
	 * @see IInnerClassesAttributeEntry#getOuterClassNameIndex()
	 */
	@Override
	public int getOuterClassNameIndex() {
		return this.outerClassNameIndex;
	}
}
