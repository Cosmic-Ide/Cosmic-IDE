/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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

import org.eclipse.jdt.core.util.IConstantPoolEntry;

/**
 * Default implementation of IConstantPoolEntry
 *
 * @see ConstantPoolEntry2
 * @since 2.0
 */
public class ConstantPoolEntry implements IConstantPoolEntry {

	private int kind;
	private int classInfoNameIndex;
	private int classIndex;
	private int nameAndTypeIndex;
	private int stringIndex;
	private char[] stringValue;
	private int integerValue;
	private float floatValue;
	private double doubleValue;
	private long longValue;
	private int nameAndTypeDescriptorIndex;
	private int nameAndTypeNameIndex;
	private char[] className;
	private char[] fieldName;
	private char[] methodName;
	private char[] fieldDescriptor;
	private char[] methodDescriptor;
	private char[] utf8Value;
	private int utf8Length;
	private char[] classInfoName;

	public ConstantPoolEntry() {
		this.classInfoNameIndex = -1;
		this.classIndex = -1;
		this.nameAndTypeIndex = -1;
		this.stringIndex = -1;
		this.stringValue = null;
		this.integerValue = -1;
		this.floatValue = -0.0f;
		this.doubleValue = -0-0;
		this.longValue = -1;
		this.nameAndTypeDescriptorIndex = -1;
		this.nameAndTypeNameIndex = -1;
		this.className = null;
		this.fieldName = null;
		this.methodName = null;
		this.fieldDescriptor = null;
		this.methodDescriptor = null;
		this.utf8Value = null;
		this.utf8Length = -1;
		this.classInfoName = null;
	}

	/**
	 * @see IConstantPoolEntry#getKind()
	 */
	@Override
	public int getKind() {
		return this.kind;
	}

	/**
	 * Sets the kind.
	 * @param kind The kind to set
	 */
	public void setKind(int kind) {
		this.kind = kind;
	}

	/**
	 * @see IConstantPoolEntry#getClassInfoNameIndex()
	 */
	@Override
	public int getClassInfoNameIndex() {
		return this.classInfoNameIndex;
	}

	/**
	 * @see IConstantPoolEntry#getClassIndex()
	 */
	@Override
	public int getClassIndex() {
		return this.classIndex;
	}

	/**
	 * @see IConstantPoolEntry#getNameAndTypeIndex()
	 */
	@Override
	public int getNameAndTypeIndex() {
		return this.nameAndTypeIndex;
	}

	/**
	 * @see IConstantPoolEntry#getStringIndex()
	 */
	@Override
	public int getStringIndex() {
		return this.stringIndex;
	}

	/**
	 * @see IConstantPoolEntry#getStringValue()
	 */
	@Override
	public String getStringValue() {
		return new String(this.stringValue);
	}

	/**
	 * @see IConstantPoolEntry#getIntegerValue()
	 */
	@Override
	public int getIntegerValue() {
		return this.integerValue;
	}

	/**
	 * @see IConstantPoolEntry#getFloatValue()
	 */
	@Override
	public float getFloatValue() {
		return this.floatValue;
	}

	/**
	 * @see IConstantPoolEntry#getDoubleValue()
	 */
	@Override
	public double getDoubleValue() {
		return this.doubleValue;
	}

	/**
	 * @see IConstantPoolEntry#getLongValue()
	 */
	@Override
	public long getLongValue() {
		return this.longValue;
	}

	/**
	 * @see IConstantPoolEntry#getNameAndTypeInfoDescriptorIndex()
	 */
	@Override
	public int getNameAndTypeInfoDescriptorIndex() {
		return this.nameAndTypeDescriptorIndex;
	}

	/**
	 * @see IConstantPoolEntry#getNameAndTypeInfoNameIndex()
	 */
	@Override
	public int getNameAndTypeInfoNameIndex() {
		return this.nameAndTypeNameIndex;
	}

	/**
	 * @see IConstantPoolEntry#getClassName()
	 */
	@Override
	public char[] getClassName() {
		return this.className;
	}

	/**
	 * @see IConstantPoolEntry#getFieldName()
	 */
	@Override
	public char[] getFieldName() {
		return this.fieldName;
	}

	/**
	 * @see IConstantPoolEntry#getMethodName()
	 */
	@Override
	public char[] getMethodName() {
		return this.methodName;
	}

	/**
	 * @see IConstantPoolEntry#getFieldDescriptor()
	 */
	@Override
	public char[] getFieldDescriptor() {
		return this.fieldDescriptor;
	}

	/**
	 * @see IConstantPoolEntry#getMethodDescriptor()
	 */
	@Override
	public char[] getMethodDescriptor() {
		return this.methodDescriptor;
	}

	/**
	 * @see IConstantPoolEntry#getUtf8Value()
	 */
	@Override
	public char[] getUtf8Value() {
		return this.utf8Value;
	}

	/**
	 * @see IConstantPoolEntry#getClassInfoName()
	 */
	@Override
	public char[] getClassInfoName() {
		return this.classInfoName;
	}

	/**
	 * Sets the classInfoNameIndex.
	 * @param classInfoNameIndex The classInfoNameIndex to set
	 */
	public void setClassInfoNameIndex(int classInfoNameIndex) {
		this.classInfoNameIndex = classInfoNameIndex;
	}

	/**
	 * Sets the classIndex.
	 * @param classIndex The classIndex to set
	 */
	public void setClassIndex(int classIndex) {
		this.classIndex = classIndex;
	}

	/**
	 * Sets the nameAndTypeIndex.
	 * @param nameAndTypeIndex The nameAndTypeIndex to set
	 */
	public void setNameAndTypeIndex(int nameAndTypeIndex) {
		this.nameAndTypeIndex = nameAndTypeIndex;
	}

	/**
	 * Sets the stringIndex.
	 * @param stringIndex The stringIndex to set
	 */
	public void setStringIndex(int stringIndex) {
		this.stringIndex = stringIndex;
	}

	/**
	 * Sets the stringValue.
	 * @param stringValue The stringValue to set
	 */
	public void setStringValue(char[] stringValue) {
		this.stringValue = stringValue;
	}

	/**
	 * Sets the integerValue.
	 * @param integerValue The integerValue to set
	 */
	public void setIntegerValue(int integerValue) {
		this.integerValue = integerValue;
	}

	/**
	 * Sets the floatValue.
	 * @param floatValue The floatValue to set
	 */
	public void setFloatValue(float floatValue) {
		this.floatValue = floatValue;
	}

	/**
	 * Sets the doubleValue.
	 * @param doubleValue The doubleValue to set
	 */
	public void setDoubleValue(double doubleValue) {
		this.doubleValue = doubleValue;
	}

	/**
	 * Sets the longValue.
	 * @param longValue The longValue to set
	 */
	public void setLongValue(long longValue) {
		this.longValue = longValue;
	}

	/**
	 * Gets the nameAndTypeDescriptorIndex.
	 * @return Returns a int
	 */
	public int getNameAndTypeDescriptorIndex() {
		return this.nameAndTypeDescriptorIndex;
	}

	/**
	 * Sets the nameAndTypeDescriptorIndex.
	 * @param nameAndTypeDescriptorIndex The nameAndTypeDescriptorIndex to set
	 */
	public void setNameAndTypeDescriptorIndex(int nameAndTypeDescriptorIndex) {
		this.nameAndTypeDescriptorIndex = nameAndTypeDescriptorIndex;
	}

	/**
	 * Gets the nameAndTypeNameIndex.
	 * @return Returns a int
	 */
	public int getNameAndTypeNameIndex() {
		return this.nameAndTypeNameIndex;
	}

	/**
	 * Sets the nameAndTypeNameIndex.
	 * @param nameAndTypeNameIndex The nameAndTypeNameIndex to set
	 */
	public void setNameAndTypeNameIndex(int nameAndTypeNameIndex) {
		this.nameAndTypeNameIndex = nameAndTypeNameIndex;
	}

	/**
	 * Sets the className.
	 * @param className The className to set
	 */
	public void setClassName(char[] className) {
		this.className = className;
	}

	/**
	 * Sets the fieldName.
	 * @param fieldName The fieldName to set
	 */
	public void setFieldName(char[] fieldName) {
		this.fieldName = fieldName;
	}

	/**
	 * Sets the methodName.
	 * @param methodName The methodName to set
	 */
	public void setMethodName(char[] methodName) {
		this.methodName = methodName;
	}

	/**
	 * Sets the fieldDescriptor.
	 * @param fieldDescriptor The fieldDescriptor to set
	 */
	public void setFieldDescriptor(char[] fieldDescriptor) {
		this.fieldDescriptor = fieldDescriptor;
	}

	/**
	 * Sets the methodDescriptor.
	 *
	 * @param methodDescriptor The methodDescriptor to set
	 */
	public void setMethodDescriptor(char[] methodDescriptor) {
		this.methodDescriptor = methodDescriptor;
	}

	/**
	 * Sets the utf8Value.
	 * @param utf8Value The utf8Value to set
	 */
	public void setUtf8Value(char[] utf8Value) {
		this.utf8Value = utf8Value;
	}

	/**
	 * Sets the classInfoName.
	 * @param classInfoName The classInfoName to set
	 */
	public void setClassInfoName(char[] classInfoName) {
		this.classInfoName = classInfoName;
	}

	/**
	 * @see IConstantPoolEntry#getUtf8Length()
	 */
	@Override
	public int getUtf8Length() {
		return this.utf8Length;
	}

	/**
	 * Sets the utf8Length.
	 * @param utf8Length The utf8Length to set
	 */
	public void setUtf8Length(int utf8Length) {
		this.utf8Length = utf8Length;
	}

	public void reset() {
		this.kind = 0;
		this.classInfoNameIndex = 0;
		this.classIndex = 0;
		this.nameAndTypeIndex = 0;
		this.stringIndex = 0;
		this.stringValue = null;
		this.integerValue = 0;
		this.floatValue = 0.0f;
		this.doubleValue = 0.0;
		this.longValue = 0L;
		this.nameAndTypeDescriptorIndex = 0;
		this.nameAndTypeNameIndex = 0;
		this.className = null;
		this.fieldName = null;
		this.methodName = null;
		this.fieldDescriptor = null;
		this.methodDescriptor = null;
		this.utf8Value = null;
		this.utf8Length = 0;
		this.classInfoName = null;
	}
}
