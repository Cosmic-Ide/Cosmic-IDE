/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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

import org.eclipse.jdt.core.util.IConstantPool;
import org.eclipse.jdt.core.util.IConstantPoolConstant;
import org.eclipse.jdt.core.util.IConstantPoolEntry;

/**
 * Default implementation of IConstantPool.
 */
public class ConstantPool extends ClassFileStruct implements IConstantPool {

	private int constantPoolCount;
	private int[] constantPoolOffset;
	private byte[] classFileBytes;

	ConstantPool(byte[] reference, int[] constantPoolOffset) {
		this.constantPoolCount = constantPoolOffset.length;
		this.constantPoolOffset = constantPoolOffset;
		this.classFileBytes = reference;
	}

	/**
	 * @see IConstantPool#decodeEntry(int)
	 */
	@Override
	public IConstantPoolEntry decodeEntry(int index) {
		ConstantPoolEntry constantPoolEntry = null;
		int kind = getEntryKind(index);
		switch(kind) {
			case IConstantPoolConstant.CONSTANT_Class :
				constantPoolEntry = new ConstantPoolEntry();
				constantPoolEntry.reset();
				constantPoolEntry.setKind(kind);
				constantPoolEntry.setClassInfoNameIndex(u2At(this.classFileBytes,  1, this.constantPoolOffset[index]));
				constantPoolEntry.setClassInfoName(getUtf8ValueAt(constantPoolEntry.getClassInfoNameIndex()));
				break;
			case IConstantPoolConstant.CONSTANT_Double :
				constantPoolEntry = new ConstantPoolEntry();
				constantPoolEntry.reset();
				constantPoolEntry.setKind(kind);
				constantPoolEntry.setDoubleValue(doubleAt(this.classFileBytes, 1, this.constantPoolOffset[index]));
				break;
			case IConstantPoolConstant.CONSTANT_Fieldref :
				constantPoolEntry = new ConstantPoolEntry();
				constantPoolEntry.reset();
				constantPoolEntry.setKind(kind);
				constantPoolEntry.reset();
				constantPoolEntry.setKind(kind);
				constantPoolEntry.setClassIndex(u2At(this.classFileBytes,  1, this.constantPoolOffset[index]));
				int declaringClassIndex = u2At(this.classFileBytes,  1, this.constantPoolOffset[constantPoolEntry.getClassIndex()]);
				constantPoolEntry.setClassName(getUtf8ValueAt(declaringClassIndex));
				constantPoolEntry.setNameAndTypeIndex(u2At(this.classFileBytes,  3, this.constantPoolOffset[index]));
				int fieldNameIndex = u2At(this.classFileBytes,  1, this.constantPoolOffset[constantPoolEntry.getNameAndTypeIndex()]);
				int fieldDescriptorIndex = u2At(this.classFileBytes,  3, this.constantPoolOffset[constantPoolEntry.getNameAndTypeIndex()]);
				constantPoolEntry.setFieldName(getUtf8ValueAt(fieldNameIndex));
				constantPoolEntry.setFieldDescriptor(getUtf8ValueAt(fieldDescriptorIndex));
				break;
			case IConstantPoolConstant.CONSTANT_Methodref :
			case IConstantPoolConstant.CONSTANT_InterfaceMethodref :
				constantPoolEntry = new ConstantPoolEntry();
				constantPoolEntry.reset();
				constantPoolEntry.setKind(kind);
				constantPoolEntry.reset();
				constantPoolEntry.setKind(kind);
				constantPoolEntry.setClassIndex(u2At(this.classFileBytes,  1, this.constantPoolOffset[index]));
				declaringClassIndex = u2At(this.classFileBytes,  1, this.constantPoolOffset[constantPoolEntry.getClassIndex()]);
				constantPoolEntry.setClassName(getUtf8ValueAt(declaringClassIndex));
				constantPoolEntry.setNameAndTypeIndex(u2At(this.classFileBytes,  3, this.constantPoolOffset[index]));
				int methodNameIndex = u2At(this.classFileBytes,  1, this.constantPoolOffset[constantPoolEntry.getNameAndTypeIndex()]);
				int methodDescriptorIndex = u2At(this.classFileBytes,  3, this.constantPoolOffset[constantPoolEntry.getNameAndTypeIndex()]);
				constantPoolEntry.setMethodName(getUtf8ValueAt(methodNameIndex));
				constantPoolEntry.setMethodDescriptor(getUtf8ValueAt(methodDescriptorIndex));
				break;
			case IConstantPoolConstant.CONSTANT_Float :
				constantPoolEntry = new ConstantPoolEntry();
				constantPoolEntry.reset();
				constantPoolEntry.setKind(kind);
				constantPoolEntry.reset();
				constantPoolEntry.setKind(kind);
				constantPoolEntry.setFloatValue(floatAt(this.classFileBytes, 1, this.constantPoolOffset[index]));
				break;
			case IConstantPoolConstant.CONSTANT_Integer :
				constantPoolEntry = new ConstantPoolEntry();
				constantPoolEntry.reset();
				constantPoolEntry.setKind(kind);
				constantPoolEntry.setIntegerValue(i4At(this.classFileBytes, 1, this.constantPoolOffset[index]));
				break;
			case IConstantPoolConstant.CONSTANT_Long :
				constantPoolEntry = new ConstantPoolEntry();
				constantPoolEntry.reset();
				constantPoolEntry.setKind(kind);
				constantPoolEntry.setLongValue(i8At(this.classFileBytes, 1, this.constantPoolOffset[index]));
				break;
			case IConstantPoolConstant.CONSTANT_NameAndType :
				constantPoolEntry = new ConstantPoolEntry();
				constantPoolEntry.reset();
				constantPoolEntry.setKind(kind);
				constantPoolEntry.setNameAndTypeNameIndex(u2At(this.classFileBytes,  1, this.constantPoolOffset[index]));
				constantPoolEntry.setNameAndTypeDescriptorIndex(u2At(this.classFileBytes,  3, this.constantPoolOffset[index]));
				break;
			case IConstantPoolConstant.CONSTANT_String :
				constantPoolEntry = new ConstantPoolEntry();
				constantPoolEntry.reset();
				constantPoolEntry.setKind(kind);
				constantPoolEntry.setStringIndex(u2At(this.classFileBytes,  1, this.constantPoolOffset[index]));
				constantPoolEntry.setStringValue(getUtf8ValueAt(constantPoolEntry.getStringIndex()));
				break;
			case IConstantPoolConstant.CONSTANT_Utf8 :
				constantPoolEntry = new ConstantPoolEntry();
				constantPoolEntry.reset();
				constantPoolEntry.setKind(kind);
				constantPoolEntry.setUtf8Length(u2At(this.classFileBytes,  1, this.constantPoolOffset[index]));
				constantPoolEntry.setUtf8Value(getUtf8ValueAt(index));
				break;
			case IConstantPoolConstant.CONSTANT_MethodHandle :
				ConstantPoolEntry2 constantPoolEntry2 = new ConstantPoolEntry2();
				constantPoolEntry2.reset();
				constantPoolEntry2.setKind(kind);
				constantPoolEntry2.setReferenceKind(u1At(this.classFileBytes,  1, this.constantPoolOffset[index]));
				constantPoolEntry2.setReferenceIndex(u2At(this.classFileBytes,  2, this.constantPoolOffset[index]));
				constantPoolEntry = constantPoolEntry2;
				break;
			case IConstantPoolConstant.CONSTANT_MethodType :
				constantPoolEntry2 = new ConstantPoolEntry2();
				constantPoolEntry2.reset();
				constantPoolEntry2.setKind(kind);
				methodDescriptorIndex = u2At(this.classFileBytes,  1, this.constantPoolOffset[index]);
				constantPoolEntry2.setDescriptorIndex(methodDescriptorIndex);
				constantPoolEntry2.setMethodDescriptor(getUtf8ValueAt(methodDescriptorIndex));
				constantPoolEntry = constantPoolEntry2;
				break;
			case IConstantPoolConstant.CONSTANT_InvokeDynamic :
				constantPoolEntry2 = new ConstantPoolEntry2();
				constantPoolEntry2.reset();
				constantPoolEntry2.setKind(kind);
				constantPoolEntry2.setBootstrapMethodAttributeIndex(u2At(this.classFileBytes,  1, this.constantPoolOffset[index]));
				int nameAndTypeIndex = u2At(this.classFileBytes,  3, this.constantPoolOffset[index]);
				constantPoolEntry2.setNameAndTypeIndex(nameAndTypeIndex);
				methodNameIndex = u2At(this.classFileBytes,  1, this.constantPoolOffset[nameAndTypeIndex]);
				methodDescriptorIndex = u2At(this.classFileBytes,  3, this.constantPoolOffset[nameAndTypeIndex]);
				constantPoolEntry2.setMethodName(getUtf8ValueAt(methodNameIndex));
				constantPoolEntry2.setMethodDescriptor(getUtf8ValueAt(methodDescriptorIndex));
				constantPoolEntry = constantPoolEntry2;
				break;
			case IConstantPoolConstant.CONSTANT_Dynamic :
				constantPoolEntry2 = new ConstantPoolEntry2();
				constantPoolEntry2.reset();
				constantPoolEntry2.setKind(kind);
				constantPoolEntry2.setBootstrapMethodAttributeIndex(u2At(this.classFileBytes,  1, this.constantPoolOffset[index]));
				int nameAndTypeIndex2 = u2At(this.classFileBytes,  3, this.constantPoolOffset[index]);
				constantPoolEntry2.setNameAndTypeIndex(nameAndTypeIndex2);
				fieldNameIndex = u2At(this.classFileBytes,  1, this.constantPoolOffset[nameAndTypeIndex2]);
				fieldDescriptorIndex = u2At(this.classFileBytes,  3, this.constantPoolOffset[nameAndTypeIndex2]);
				constantPoolEntry2.setFieldName(getUtf8ValueAt(fieldNameIndex));
				constantPoolEntry2.setFieldDescriptor(getUtf8ValueAt(fieldDescriptorIndex));
				constantPoolEntry = constantPoolEntry2;
				break;
			case IConstantPoolConstant.CONSTANT_Module :
				constantPoolEntry2 = new ConstantPoolEntry2();
				constantPoolEntry2.reset();
				constantPoolEntry2.setKind(kind);
				int moduleIndex = u2At(this.classFileBytes,  1, this.constantPoolOffset[index]);
				constantPoolEntry2.setModuleIndex(moduleIndex);
				constantPoolEntry2.setModuleName(getUtf8ValueAt(moduleIndex));
				constantPoolEntry = constantPoolEntry2;
				break;
			case IConstantPoolConstant.CONSTANT_Package :
				constantPoolEntry2 = new ConstantPoolEntry2();
				constantPoolEntry2.reset();
				constantPoolEntry2.setKind(kind);
				int packageIndex = u2At(this.classFileBytes,  1, this.constantPoolOffset[index]);
				constantPoolEntry2.setPackageIndex(packageIndex);
				constantPoolEntry2.setPackageName(getUtf8ValueAt(packageIndex));
				constantPoolEntry = constantPoolEntry2;
				break;
		}
		return constantPoolEntry;
	}

	/**
	 * @see IConstantPool#getConstantPoolCount()
	 */
	@Override
	public int getConstantPoolCount() {
		return this.constantPoolCount;
	}

	/**
	 * @see IConstantPool#getEntryKind(int)
	 */
	@Override
	public int getEntryKind(int index) {
		return u1At(this.classFileBytes, 0, this.constantPoolOffset[index]);
	}

	private char[] getUtf8ValueAt(int utf8Index) {
		int utf8Offset = this.constantPoolOffset[utf8Index];
		return utf8At(this.classFileBytes, 0, utf8Offset + 3, u2At(this.classFileBytes, 0, utf8Offset + 1));
	}
}
