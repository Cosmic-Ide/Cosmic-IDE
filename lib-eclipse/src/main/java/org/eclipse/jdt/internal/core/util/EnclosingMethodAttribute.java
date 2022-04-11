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
import org.eclipse.jdt.core.util.IEnclosingMethodAttribute;

/**
 * Default implementation of EnclosingMethodAttribute.
 *
 * @since 3.0
 */
public class EnclosingMethodAttribute extends ClassFileAttribute implements IEnclosingMethodAttribute {

	private int enclosingClassIndex;
	private char[] enclosingClassName;
	private int methodDescriptorIndex;
	private char[] methodDescriptor;
	private int methodNameIndex;
	private char[] methodName;
	private int methodNameAndTypeIndex;

	EnclosingMethodAttribute(byte[] classFileBytes, IConstantPool constantPool, int offset) throws ClassFormatException {
		super(classFileBytes, constantPool, offset);
		int index = u2At(classFileBytes, 6, offset);
		this.enclosingClassIndex = index;
		IConstantPoolEntry constantPoolEntry = constantPool.decodeEntry(index);
		if (constantPoolEntry.getKind() != IConstantPoolConstant.CONSTANT_Class) {
			throw new ClassFormatException(ClassFormatException.INVALID_CONSTANT_POOL_ENTRY);
		}
		this.enclosingClassName = constantPoolEntry.getClassInfoName();
		this.methodNameAndTypeIndex = u2At(classFileBytes, 8, offset);
		if (this.methodNameAndTypeIndex != 0) {
			constantPoolEntry = constantPool.decodeEntry(this.methodNameAndTypeIndex);
			if (constantPoolEntry.getKind() != IConstantPoolConstant.CONSTANT_NameAndType) {
				throw new ClassFormatException(ClassFormatException.INVALID_CONSTANT_POOL_ENTRY);
			}
			this.methodDescriptorIndex = constantPoolEntry.getNameAndTypeInfoDescriptorIndex();
			this.methodNameIndex = constantPoolEntry.getNameAndTypeInfoNameIndex();
			constantPoolEntry = constantPool.decodeEntry(this.methodDescriptorIndex);
			if (constantPoolEntry.getKind() != IConstantPoolConstant.CONSTANT_Utf8) {
				throw new ClassFormatException(ClassFormatException.INVALID_CONSTANT_POOL_ENTRY);
			}
			this.methodDescriptor = constantPoolEntry.getUtf8Value();
			constantPoolEntry = constantPool.decodeEntry(this.methodNameIndex);
			if (constantPoolEntry.getKind() != IConstantPoolConstant.CONSTANT_Utf8) {
				throw new ClassFormatException(ClassFormatException.INVALID_CONSTANT_POOL_ENTRY);
			}
			this.methodName = constantPoolEntry.getUtf8Value();
		}
	}

	@Override
	public char[] getEnclosingClass() {
		return this.enclosingClassName;
	}

	@Override
	public int getEnclosingClassIndex() {
		return this.enclosingClassIndex;
	}

	@Override
	public char[] getMethodDescriptor() {
		return this.methodDescriptor;
	}

	@Override
	public int getMethodDescriptorIndex() {
		return this.methodDescriptorIndex;
	}

	@Override
	public char[] getMethodName() {
		return this.methodName;
	}

	@Override
	public int getMethodNameIndex() {
		return this.methodNameIndex;
	}

	@Override
	public int getMethodNameAndTypeIndex() {
		return this.methodNameAndTypeIndex;
	}
}
