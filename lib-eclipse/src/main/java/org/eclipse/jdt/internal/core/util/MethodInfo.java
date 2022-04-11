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
 *        Jesper Steen Moeller - Contribution for
 *                          Bug 406973 - [compiler] Parse MethodParameters attribute
 *******************************************************************************/
package org.eclipse.jdt.internal.core.util;

import org.eclipse.jdt.core.util.ClassFormatException;
import org.eclipse.jdt.core.util.IAttributeNamesConstants;
import org.eclipse.jdt.core.util.IClassFileAttribute;
import org.eclipse.jdt.core.util.IClassFileReader;
import org.eclipse.jdt.core.util.ICodeAttribute;
import org.eclipse.jdt.core.util.IConstantPool;
import org.eclipse.jdt.core.util.IConstantPoolConstant;
import org.eclipse.jdt.core.util.IConstantPoolEntry;
import org.eclipse.jdt.core.util.IExceptionAttribute;
import org.eclipse.jdt.core.util.IMethodInfo;
import org.eclipse.jdt.core.util.IModifierConstants;

/**
 * Default implementation of IMethodInfo.
 */
public class MethodInfo extends ClassFileStruct implements IMethodInfo {
	private int accessFlags;
	private int attributeBytes;
	private IClassFileAttribute[] attributes;
	private int attributesCount;
	private ICodeAttribute codeAttribute;
	private char[] descriptor;
	private int descriptorIndex;
	private IExceptionAttribute exceptionAttribute;
	private boolean isDeprecated;
	private boolean isSynthetic;
	private char[] name;
	private int nameIndex;

	/**
	 * @param classFileBytes byte[]
	 * @param constantPool IConstantPool
	 * @param offset int
	 * @param decodingFlags int
	 */
	public MethodInfo(byte classFileBytes[], IConstantPool constantPool, int offset, int decodingFlags)
		throws ClassFormatException {

		boolean no_code_attribute = (decodingFlags & IClassFileReader.METHOD_BODIES) == 0;
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
		if (this.attributesCount != 0) {
			if (no_code_attribute && !isAbstract() && !isNative()) {
				if (this.attributesCount != 1) {
					this.attributes = new IClassFileAttribute[this.attributesCount - 1];
				}
			} else {
				this.attributes = new IClassFileAttribute[this.attributesCount];
			}
		}
		int attributesIndex = 0;
		int readOffset = 8;
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
			} else if (equals(attributeName, IAttributeNamesConstants.CODE)) {
				if (!no_code_attribute) {
					this.codeAttribute = new CodeAttribute(classFileBytes, constantPool, offset + readOffset);
					this.attributes[attributesIndex++] = this.codeAttribute;
				}
			} else if (equals(attributeName, IAttributeNamesConstants.EXCEPTIONS)) {
				this.exceptionAttribute = new ExceptionAttribute(classFileBytes, constantPool, offset + readOffset);
				this.attributes[attributesIndex++] = this.exceptionAttribute;
			} else if (equals(attributeName, IAttributeNamesConstants.SIGNATURE)) {
				this.attributes[attributesIndex++] = new SignatureAttribute(classFileBytes, constantPool, offset + readOffset);
			} else if (equals(attributeName, IAttributeNamesConstants.RUNTIME_VISIBLE_ANNOTATIONS)) {
				this.attributes[attributesIndex++] = new RuntimeVisibleAnnotationsAttribute(classFileBytes, constantPool, offset + readOffset);
			} else if (equals(attributeName, IAttributeNamesConstants.RUNTIME_INVISIBLE_ANNOTATIONS)) {
				this.attributes[attributesIndex++] = new RuntimeInvisibleAnnotationsAttribute(classFileBytes, constantPool, offset + readOffset);
			} else if (equals(attributeName, IAttributeNamesConstants.RUNTIME_VISIBLE_PARAMETER_ANNOTATIONS)) {
				this.attributes[attributesIndex++] = new RuntimeVisibleParameterAnnotationsAttribute(classFileBytes, constantPool, offset + readOffset);
			} else if (equals(attributeName, IAttributeNamesConstants.RUNTIME_INVISIBLE_PARAMETER_ANNOTATIONS)) {
				this.attributes[attributesIndex++] = new RuntimeInvisibleParameterAnnotationsAttribute(classFileBytes, constantPool, offset + readOffset);
			} else if (equals(attributeName, IAttributeNamesConstants.ANNOTATION_DEFAULT)) {
				this.attributes[attributesIndex++] = new AnnotationDefaultAttribute(classFileBytes, constantPool, offset + readOffset);
			} else if (equals(attributeName, IAttributeNamesConstants.RUNTIME_VISIBLE_TYPE_ANNOTATIONS)) {
				this.attributes[attributesIndex++] = new RuntimeVisibleTypeAnnotationsAttribute(classFileBytes, constantPool, offset + readOffset);
			} else if (equals(attributeName, IAttributeNamesConstants.RUNTIME_INVISIBLE_TYPE_ANNOTATIONS)) {
				this.attributes[attributesIndex++] = new RuntimeInvisibleTypeAnnotationsAttribute(classFileBytes, constantPool, offset + readOffset);
			} else if (equals(attributeName, IAttributeNamesConstants.METHOD_PARAMETERS)) {
				this.attributes[attributesIndex++] = new MethodParametersAttribute(classFileBytes, constantPool, offset + readOffset);
			} else {
				this.attributes[attributesIndex++] = new ClassFileAttribute(classFileBytes, constantPool, offset + readOffset);
			}
			readOffset += (6 + u4At(classFileBytes, readOffset + 2, offset));
		}
		this.attributeBytes = readOffset;
	}
	/**
	 * @see IMethodInfo#getAccessFlags()
	 */
	@Override
	public int getAccessFlags() {
		return this.accessFlags;
	}

	/**
	 * @see IMethodInfo#getAttributeCount()
	 */
	@Override
	public int getAttributeCount() {
		return this.attributesCount;
	}
	/**
	 * @see IMethodInfo#getAttributes()
	 */
	@Override
	public IClassFileAttribute[] getAttributes() {
		return this.attributes;
	}

	/**
	 * @see IMethodInfo#getCodeAttribute()
	 */
	@Override
	public ICodeAttribute getCodeAttribute() {
		return this.codeAttribute;
	}

	/**
	 * @see IMethodInfo#getDescriptor()
	 */
	@Override
	public char[] getDescriptor() {
		return this.descriptor;
	}

	/**
	 * @see IMethodInfo#getDescriptorIndex()
	 */
	@Override
	public int getDescriptorIndex() {
		return this.descriptorIndex;
	}

	/**
	 * @see IMethodInfo#getExceptionAttribute()
	 */
	@Override
	public IExceptionAttribute getExceptionAttribute() {
		return this.exceptionAttribute;
	}

	/**
	 * @see IMethodInfo#getName()
	 */
	@Override
	public char[] getName() {
		return this.name;
	}

	/**
	 * @see IMethodInfo#getNameIndex()
	 */
	@Override
	public int getNameIndex() {
		return this.nameIndex;
	}

	private boolean isAbstract() {
		return (this.accessFlags & IModifierConstants.ACC_ABSTRACT) != 0;
	}

	/**
	 * @see IMethodInfo#isClinit()
	 */
	@Override
	public boolean isClinit() {
		return this.name[0] == '<' && this.name.length == 8; // Can only match <clinit>
	}

	/**
	 * @see IMethodInfo#isConstructor()
	 */
	@Override
	public boolean isConstructor() {
		return this.name[0] == '<' && this.name.length == 6; // Can only match <init>
	}

	/**
	 * @see IMethodInfo#isDeprecated()
	 */
	@Override
	public boolean isDeprecated() {
		return this.isDeprecated;
	}

	private boolean isNative() {
		return (this.accessFlags & IModifierConstants.ACC_NATIVE) != 0;
	}

	/**
	 * @see IMethodInfo#isSynthetic()
	 */
	@Override
	public boolean isSynthetic() {
		return this.isSynthetic;
	}

	int sizeInBytes() {
		return this.attributeBytes;
	}
}
