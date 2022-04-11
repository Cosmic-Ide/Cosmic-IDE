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
package org.eclipse.jdt.core.util;

/**
 * Description of a constant pool entry as described in the JVM specifications.
 * Its contents is initialized according to its kind.
 *
 * This interface may be implemented by clients. Because of that questionable choice,
 * clients may have to cast to {@link IConstantPoolEntry3} to get access to the relevant content.
 *
 * @see IConstantPoolEntry2
 * @see IConstantPoolEntry3
 * @since 2.0
 */
public interface IConstantPoolEntry {

	/**
	 * Returns the type of this entry.
	 *
	 * @return the type of this entry
	 */
	int getKind();

	/**
	 * Returns the name index for a CONSTANT_Class type entry.
	 * The value is unspecified otherwise.
	 *
	 * @return the name index for a CONSTANT_Class type entry
	 * @see IConstantPoolConstant#CONSTANT_Class
	 */
	int getClassInfoNameIndex();

	/**
	 * Returns the class index for a CONSTANT_Fieldref,
	 * CONSTANT_Methodref, CONSTANT_InterfaceMethodref type entry.
	 * The value is unspecified otherwise.
	 *
	 * @return the class index for a CONSTANT_Fieldref,
	 * CONSTANT_Methodref, CONSTANT_InterfaceMethodref type entry
	 * @see IConstantPoolConstant#CONSTANT_Fieldref
	 * @see IConstantPoolConstant#CONSTANT_Methodref
	 * @see IConstantPoolConstant#CONSTANT_InterfaceMethodref
	 */
	int getClassIndex();

	/**
	 * Returns the nameAndType index for a CONSTANT_Fieldref,
	 * CONSTANT_Methodref, CONSTANT_InterfaceMethodref,
	 * CONSTANT_InvokeDynamic type entry.
	 * The value is unspecified otherwise.
	 *
	 * @return the nameAndType index for a CONSTANT_Fieldref,
	 * CONSTANT_Methodref, CONSTANT_InterfaceMethodref,
	 * CONSTANT_InvokeDynamic type entry
	 * @see IConstantPoolConstant#CONSTANT_Fieldref
	 * @see IConstantPoolConstant#CONSTANT_Methodref
	 * @see IConstantPoolConstant#CONSTANT_InterfaceMethodref
	 * @see IConstantPoolConstant#CONSTANT_InvokeDynamic
	 * @see IConstantPoolConstant#CONSTANT_Dynamic
	 */
	int getNameAndTypeIndex();

	/**
	 * Returns the string index for a CONSTANT_String type entry.
	 * The value is unspecified otherwise.
	 *
	 * @return the string index for a CONSTANT_String type entry
	 * @see IConstantPoolConstant#CONSTANT_String
	 */
	int getStringIndex();

	/**
	 * Returns the string value for a CONSTANT_String type entry.
	 * Returns null otherwise.
	 *
	 * @return the string value for a CONSTANT_String type entry
	 * @see IConstantPoolConstant#CONSTANT_String
	 */
	String getStringValue();

	/**
	 * Returns the integer value for a CONSTANT_Integer type entry.
	 * The value is unspecified otherwise.
	 *
	 * @return the integer value for a CONSTANT_Integer type entry
	 * @see IConstantPoolConstant#CONSTANT_Integer
	 */
	int getIntegerValue();

	/**
	 * Returns the float value for a CONSTANT_Float type entry.
	 * The value is unspecified otherwise.
	 *
	 * @return the float value for a CONSTANT_Float type entry
	 * @see IConstantPoolConstant#CONSTANT_Float
	 */
	float getFloatValue();

	/**
	 * Returns the double value for a CONSTANT_Double type entry.
	 * The value is unspecified otherwise.
	 *
	 * @return the double value for a CONSTANT_Double type entry
	 * @see IConstantPoolConstant#CONSTANT_Double
	 */
	double getDoubleValue();

	/**
	 * Returns the long value for a CONSTANT_Long type entry.
	 * The value is unspecified otherwise.
	 *
	 * @return the long value for a CONSTANT_Long type entry
	 * @see IConstantPoolConstant#CONSTANT_Long
	 */
	long getLongValue();

	/**
	 * Returns the descriptor index for a CONSTANT_NameAndType type entry.
	 * The value is unspecified otherwise.
	 *
	 * @return the descriptor index for a CONSTANT_NameAndType type entry
	 * @see IConstantPoolConstant#CONSTANT_NameAndType
	 */
	int getNameAndTypeInfoDescriptorIndex();

	/**
	 * Returns the name index for a CONSTANT_NameAndType type entry.
	 * The value is unspecified otherwise.
	 *
	 * @return the name index for a CONSTANT_NameAndType type entry
	 * @see IConstantPoolConstant#CONSTANT_NameAndType
	 */
	int getNameAndTypeInfoNameIndex();

	/**
	 * Returns the class name for a CONSTANT_Class type entry.
	 * Returns null otherwise.
	 *
	 * @return the class name for a CONSTANT_Class type entry
	 * @see IConstantPoolConstant#CONSTANT_Class
	 */
	char[] getClassInfoName();

	/**
	 * Returns the class name for a CONSTANT_Fieldref,
	 * CONSTANT_Methodref, CONSTANT_InterfaceMethodref type entry.
	 * Returns null otherwise.
	 *
	 * @return the class name for a CONSTANT_Fieldref,
	 * CONSTANT_Methodref, CONSTANT_InterfaceMethodref type entry
	 * @see IConstantPoolConstant#CONSTANT_Fieldref
	 * @see IConstantPoolConstant#CONSTANT_Methodref
	 * @see IConstantPoolConstant#CONSTANT_InterfaceMethodref
	 */
	char[] getClassName();

	/**
	 * Returns the field name for a CONSTANT_Fieldref type entry.
	 * Returns null otherwise.
	 *
	 * @return the field name for a CONSTANT_Fieldref type entry
	 * @see IConstantPoolConstant#CONSTANT_Fieldref
	 */
	char[] getFieldName();

	/**
	 * Returns the method name for a CONSTANT_Methodref, CONSTANT_InterfaceMethodref
	 * or CONSTANT_InvokeDynamic type entry.
	 * Returns null otherwise.
	 *
	 * @return the method name for a CONSTANT_Methodref, CONSTANT_InterfaceMethodref
	 * or CONSTANT_InvokeDynamic type entry
	 * @see IConstantPoolConstant#CONSTANT_Methodref
	 * @see IConstantPoolConstant#CONSTANT_InterfaceMethodref
	 * @see IConstantPoolConstant#CONSTANT_InvokeDynamic
	 * @see IConstantPoolConstant#CONSTANT_Dynamic
	 */
	char[] getMethodName();

	/**
	 * Returns the field descriptor value for a CONSTANT_Fieldref type entry. This value
	 * is set only when decoding the CONSTANT_Fieldref entry.
	 * Returns null otherwise.
	 *
	 * @return the field descriptor value for a CONSTANT_Fieldref type entry. This value
	 * is set only when decoding the CONSTANT_Fieldref entry
	 * @see IConstantPoolConstant#CONSTANT_Fieldref
	 */
	char[] getFieldDescriptor();

	/**
	 * Returns the method descriptor value for a CONSTANT_Methodref or
	 * CONSTANT_InterfaceMethodref type entry. This value is set only when decoding the
	 * CONSTANT_Methodref, CONSTANT_InterfaceMethodref, CONSTANT_MethodType
	 * or CONSTANT_InvokeDynamic entry.
	 *
	 * Returns null otherwise.
	 *
	 * @return the method descriptor value for a CONSTANT_Methodref,
	 * CONSTANT_InterfaceMethodref type entry. This value is set only when decoding the
	 * CONSTANT_Methodref, CONSTANT_InterfaceMethodref, CONSTANT_MethodType
	 * or CONSTANT_InvokeDynamic entry
	 *
	 * @see IConstantPoolConstant#CONSTANT_Methodref
	 * @see IConstantPoolConstant#CONSTANT_InterfaceMethodref
	 * @see IConstantPoolConstant#CONSTANT_MethodType
	 * @see IConstantPoolConstant#CONSTANT_InvokeDynamic
	 * @see IConstantPoolConstant#CONSTANT_Dynamic
	 */
	char[] getMethodDescriptor();

	/**
	 * Returns the utf8 value for a CONSTANT_Utf8 type entry. This value is set only when
	 * decoding a UTF8 entry.
	 * Returns null otherwise.
	 *
	 * @return the utf8 value for a CONSTANT_Utf8 type entry. This value is set only when
	 * decoding a UTF8 entry
	 * @see IConstantPoolConstant#CONSTANT_Utf8
	 */
	char[] getUtf8Value();

	/**
	 * Returns the utf8 length for a CONSTANT_Utf8 type entry. This value is set only when
	 * decoding a UTF8 entry.
	 * Returns null otherwise.
	 *
	 * @return the utf8 length for a CONSTANT_Utf8 type entry. This value is set only when
	 * decoding a UTF8 entry
	 * @see IConstantPoolConstant#CONSTANT_Utf8
	 */
	int getUtf8Length();
}
