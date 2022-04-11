/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
 *        Andy Clement - Contributions for
 *                          Bug 383624 - [1.8][compiler] Revive code generation support for type annotations (from Olivier's work)
 *******************************************************************************/
package org.eclipse.jdt.internal.core.util;

import org.eclipse.jdt.core.util.ClassFormatException;
import org.eclipse.jdt.core.util.IAttributeNamesConstants;
import org.eclipse.jdt.core.util.IClassFileAttribute;
import org.eclipse.jdt.core.util.IConstantPool;
import org.eclipse.jdt.core.util.IConstantPoolConstant;
import org.eclipse.jdt.core.util.IConstantPoolEntry;
import org.eclipse.jdt.core.util.IConstantValueAttribute;
import org.eclipse.jdt.core.util.IFieldInfo;
import org.eclipse.jdt.core.util.IModifierConstants;

/**
 * Default implementation of IFieldInfo.
 */
public class FieldInfo extends ClassFileStruct implements IFieldInfo {
	private int accessFlags;
	private int attributeBytes;
	private IClassFileAttribute[] attributes;
	private int attributesCount;
	private IConstantValueAttribute constantValueAttribute;
	private char[] descriptor;
	private int descriptorIndex;
	private boolean isDeprecated;
	private boolean isSynthetic;
	private char[] name;
	private int nameIndex;

	/**
	 * @param classFileBytes byte[]
	 * @param constantPool IConstantPool
	 * @param offset int
	 */
	public FieldInfo(byte classFileBytes[], IConstantPool constantPool, int offset)
		throws ClassFormatException {
		final int flags = u2At(classFileBytes, 0, offset);
		this.accessFlags = flags;
		if ((flags & IModifierConstants.ACC_SYNTHETIC) != 0) {
			this.isSynthetic = true;
		}
		this.nameIndex = u2At(classFileBytes, 2, offset);
		IConstantPoolEntry constantPoolEntry = constantPool.decodeEntry(this.nameIndex);
		if (constantPoolEntry.getKind() != IConstantPoolConstant.CONSTANT_Utf8) {
			throw new ClassFormatException(ClassFormatException.INVALID_CONSTANT_POOL_ENTRY);
		}
		this.name = constantPoolEntry.getUtf8Value();

		this.descriptorIndex = u2At(classFileBytes, 4, offset);
		constantPoolEntry = constantPool.decodeEntry(this.descriptorIndex);
		if (constantPoolEntry.getKind() != IConstantPoolConstant.CONSTANT_Utf8) {
			throw new ClassFormatException(ClassFormatException.INVALID_CONSTANT_POOL_ENTRY);
		}
		this.descriptor = constantPoolEntry.getUtf8Value();

		this.attributesCount = u2At(classFileBytes, 6, offset);
		this.attributes = ClassFileAttribute.NO_ATTRIBUTES;
		int readOffset = 8;
		if (this.attributesCount != 0) {
			this.attributes = new IClassFileAttribute[this.attributesCount];
		}
		int attributesIndex = 0;
		for (int i = 0; i < this.attributesCount; i++) {
			constantPoolEntry = constantPool.decodeEntry(u2At(classFileBytes, readOffset, offset));
			if (constantPoolEntry.getKind() != IConstantPoolConstant.CONSTANT_Utf8) {
				throw new ClassFormatException(ClassFormatException.INVALID_CONSTANT_POOL_ENTRY);
			}
			char[] attributeName = constantPoolEntry.getUtf8Value();
			if (equals(attributeName, IAttributeNamesConstants.DEPRECATED)) {
				this.isDeprecated = true;
				this.attributes[attributesIndex++] = new ClassFileAttribute(classFileBytes, constantPool, offset + readOffset);
			} else if (equals(attributeName, IAttributeNamesConstants.SYNTHETIC)) {
				this.isSynthetic = true;
				this.attributes[attributesIndex++] = new ClassFileAttribute(classFileBytes, constantPool, offset + readOffset);
			} else if (equals(attributeName, IAttributeNamesConstants.CONSTANT_VALUE)) {
				this.constantValueAttribute = new ConstantValueAttribute(classFileBytes, constantPool, offset + readOffset);
				this.attributes[attributesIndex++] = this.constantValueAttribute;
			} else if (equals(attributeName, IAttributeNamesConstants.SIGNATURE)) {
				this.attributes[attributesIndex++] = new SignatureAttribute(classFileBytes, constantPool, offset + readOffset);
			} else if (equals(attributeName, IAttributeNamesConstants.RUNTIME_VISIBLE_ANNOTATIONS)) {
				this.attributes[attributesIndex++] = new RuntimeVisibleAnnotationsAttribute(classFileBytes, constantPool, offset + readOffset);
			} else if (equals(attributeName, IAttributeNamesConstants.RUNTIME_INVISIBLE_ANNOTATIONS)) {
				this.attributes[attributesIndex++] = new RuntimeInvisibleAnnotationsAttribute(classFileBytes, constantPool, offset + readOffset);
			} else if (equals(attributeName, IAttributeNamesConstants.RUNTIME_VISIBLE_TYPE_ANNOTATIONS)) {
				this.attributes[attributesIndex++] = new RuntimeVisibleTypeAnnotationsAttribute(classFileBytes, constantPool, offset + readOffset);
			} else if (equals(attributeName, IAttributeNamesConstants.RUNTIME_INVISIBLE_TYPE_ANNOTATIONS)) {
				this.attributes[attributesIndex++] = new RuntimeInvisibleTypeAnnotationsAttribute(classFileBytes, constantPool, offset + readOffset);
			} else {
				this.attributes[attributesIndex++] = new ClassFileAttribute(classFileBytes, constantPool, offset + readOffset);
			}
			readOffset += (6 + u4At(classFileBytes, readOffset + 2, offset));
		}

		this.attributeBytes = readOffset;
	}
	/**
	 * @see IFieldInfo#getAccessFlags()
	 */
	@Override
	public int getAccessFlags() {
		return this.accessFlags;
	}
	/**
	 * @see IFieldInfo#getAttributeCount()
	 */
	@Override
	public int getAttributeCount() {
		return this.attributesCount;
	}

	/**
	 * @see IFieldInfo#getAttributes()
	 */
	@Override
	public IClassFileAttribute[] getAttributes() {
		return this.attributes;
	}

	/**
	 * @see IFieldInfo#getConstantValueAttribute()
	 */
	@Override
	public IConstantValueAttribute getConstantValueAttribute() {
		return this.constantValueAttribute;
	}

	/**
	 * @see IFieldInfo#getDescriptor()
	 */
	@Override
	public char[] getDescriptor() {
		return this.descriptor;
	}

	/**
	 * @see IFieldInfo#getDescriptorIndex()
	 */
	@Override
	public int getDescriptorIndex() {
		return this.descriptorIndex;
	}

	/**
	 * @see IFieldInfo#getName()
	 */
	@Override
	public char[] getName() {
		return this.name;
	}

	/**
	 * @see IFieldInfo#getNameIndex()
	 */
	@Override
	public int getNameIndex() {
		return this.nameIndex;
	}
	/**
	 * @see IFieldInfo#hasConstantValueAttribute()
	 */
	@Override
	public boolean hasConstantValueAttribute() {
		return this.constantValueAttribute != null;
	}

	/**
	 * @see IFieldInfo#isDeprecated()
	 */
	@Override
	public boolean isDeprecated() {
		return this.isDeprecated;
	}

	/**
	 * @see IFieldInfo#isSynthetic()
	 */
	@Override
	public boolean isSynthetic() {
		return this.isSynthetic;
	}

	int sizeInBytes() {
		return this.attributeBytes;
	}
}
