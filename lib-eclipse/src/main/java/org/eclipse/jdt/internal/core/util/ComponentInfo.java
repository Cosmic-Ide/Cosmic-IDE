/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
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
import org.eclipse.jdt.core.util.IAttributeNamesConstants;
import org.eclipse.jdt.core.util.IClassFileAttribute;
import org.eclipse.jdt.core.util.IConstantPool;
import org.eclipse.jdt.core.util.IConstantPoolConstant;
import org.eclipse.jdt.core.util.IConstantPoolEntry;
import org.eclipse.jdt.core.util.IComponentInfo;

/**
 * Default implementation of IComponentInfo.
 */
public class ComponentInfo extends ClassFileStruct implements IComponentInfo {
	private int attributeBytes;
	private IClassFileAttribute[] attributes;
	private int attributesCount;
	private char[] descriptor;
	private int descriptorIndex;
	private char[] name;
	private int nameIndex;

	/**
	 * @param classFileBytes byte[]
	 * @param constantPool IConstantPool
	 * @param offset int
	 */
	public ComponentInfo(byte classFileBytes[], IConstantPool constantPool, int offset)
		throws ClassFormatException {
		this.nameIndex = u2At(classFileBytes, 0, offset);
		IConstantPoolEntry constantPoolEntry = constantPool.decodeEntry(this.nameIndex);
		if (constantPoolEntry.getKind() != IConstantPoolConstant.CONSTANT_Utf8) {
			throw new ClassFormatException(ClassFormatException.INVALID_CONSTANT_POOL_ENTRY);
		}
		this.name = constantPoolEntry.getUtf8Value();

		this.descriptorIndex = u2At(classFileBytes, 2, offset);
		constantPoolEntry = constantPool.decodeEntry(this.descriptorIndex);
		if (constantPoolEntry.getKind() != IConstantPoolConstant.CONSTANT_Utf8) {
			throw new ClassFormatException(ClassFormatException.INVALID_CONSTANT_POOL_ENTRY);
		}
		this.descriptor = constantPoolEntry.getUtf8Value();

		this.attributesCount = u2At(classFileBytes, 4, offset);
		this.attributes = ClassFileAttribute.NO_ATTRIBUTES;
		int readOffset = 6;
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
			if (equals(attributeName, IAttributeNamesConstants.SIGNATURE)) {
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
	 * @see IComponentInfo#getAttributeCount()
	 */
	@Override
	public int getAttributeCount() {
		return this.attributesCount;
	}

	/**
	 * @see IComponentInfo#getAttributes()
	 */
	@Override
	public IClassFileAttribute[] getAttributes() {
		return this.attributes;
	}

	/**
	 * @see IComponentInfo#getDescriptor()
	 */
	@Override
	public char[] getDescriptor() {
		return this.descriptor;
	}

	/**
	 * @see IComponentInfo#getDescriptorIndex()
	 */
	@Override
	public int getDescriptorIndex() {
		return this.descriptorIndex;
	}

	/**
	 * @see IComponentInfo#getName()
	 */
	@Override
	public char[] getName() {
		return this.name;
	}

	/**
	 * @see IComponentInfo#getNameIndex()
	 */
	@Override
	public int getNameIndex() {
		return this.nameIndex;
	}

	@Override
	public int sizeInBytes() {
		return this.attributeBytes;
	}
}