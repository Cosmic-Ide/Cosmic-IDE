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
package org.eclipse.jdt.core.util;

/**
 * Description of an annotation component value as described in the JVM specifications
 * (added in J2SE 1.5).
 *
 * This interface may be implemented by clients.
 *
 * @since 3.1
 */
public interface IAnnotationComponentValue {

	/**
	 * Tag value for a constant of type <code>byte</code>
	 * @since 3.1
	 */
	int BYTE_TAG = 'B';
	/**
	 * Tag value for a constant of type <code>char</code>
	 * @since 3.1
	 */
	int CHAR_TAG = 'C';
	/**
	 * Tag value for a constant of type <code>double</code>
	 * @since 3.1
	 */
	int DOUBLE_TAG = 'D';
	/**
	 * Tag value for a constant of type <code>float</code>
	 * @since 3.1
	 */
	int FLOAT_TAG = 'F';
	/**
	 * Tag value for a constant of type <code>int</code>
	 * @since 3.1
	 */
	int INTEGER_TAG = 'I';
	/**
	 * Tag value for a constant of type <code>long</code>
	 * @since 3.1
	 */
	int LONG_TAG = 'J';
	/**
	 * Tag value for a constant of type <code>short</code>
	 * @since 3.1
	 */
	int SHORT_TAG = 'S';
	/**
	 * Tag value for a constant of type <code>boolean</code>
	 * @since 3.1
	 */
	int BOOLEAN_TAG = 'Z';
	/**
	 * Tag value for a constant of type <code>java.lang.String</code>
	 * @since 3.1
	 */
	int STRING_TAG = 's';
	/**
	 * Tag value for a value that represents an enum constant
	 * @since 3.1
	 */
	int ENUM_TAG = 'e';
	/**
	 * Tag value for a value that represents a class
	 * @since 3.1
	 */
	int CLASS_TAG = 'c';
	/**
	 * Tag value for a value that represents an annotation
	 * @since 3.1
	 */
	int ANNOTATION_TAG = '@';
	/**
	 * Tag value for a value that represents an array
	 * @since 3.1
	 */
	int ARRAY_TAG = '[';

	/**
	 * Returns the annotation component values as described in the JVM specifications
	 * if the tag item is '['.
	 * Returns null otherwise.
	 *
	 * @return the annotation component values
	 */
	IAnnotationComponentValue[] getAnnotationComponentValues();

	/**
	 * Returns the annotation value as described in the JVM specifications
	 * if the tag item is '&#064;'.
	 * Returns null otherwise.
	 *
	 * @return the attribute value
	 * @since 3.1
	 */
	IAnnotation getAnnotationValue();

	/**
	 * Returns the class info as described in the JVM specifications
	 * if the tag item is 'c'.
	 * Returns null otherwise.
	 *
	 * @return the class info
	 */
	IConstantPoolEntry getClassInfo();

	/**
	 * Returns the class info index as described in the JVM specifications
	 * if the tag item is 'c'.
	 * Returns null otherwise.
	 *
	 * @return the class info index
	 */
	int getClassInfoIndex();

	/**
	 * Returns the constant value as described in the JVM specifications
	 * if the tag item is one of 'B', 'C', 'D', 'F', 'I', 'J', 'S', 'Z', or 's'.
	 * Returns null otherwise.
	 *
	 * @return the constant value
	 */
	IConstantPoolEntry getConstantValue();

	/**
	 * Returns the constant value index as described in the JVM specifications
	 * if the tag item is one of 'B', 'C', 'D', 'F', 'I', 'J', 'S', 'Z', or 's'.
	 * The value is unspecified otherwise.
	 *
	 * @return the constant value index
	 */
	int getConstantValueIndex();

	/**
	 * Returns the simple name of the enum constant represented
	 * by this annotation component value as described in the JVM specifications
	 * if the tag item is 'e'.
	 * Returns null otherwise.
	 *
	 * @return the enum constant
	 * @since 3.1
	 */
	char[] getEnumConstantName();

	/**
	 * Returns the utf8 constant index as described in the JVM specifications
	 * if the tag item is 'e'.
	 * The value is unspecified otherwise.
	 *
	 * @return the enum constant index
	 * @since 3.1
	 */
	int getEnumConstantNameIndex();

	/**
	 * Returns the binary name of the type of the enum constant represented
	 * by this annotation component value as described in the JVM specifications
	 * if the tag item is 'e'.
	 * Returns null otherwise.
	 *
	 * @return the enum constant
	 * @since 3.1
	 */
	char[] getEnumConstantTypeName();

	/**
	 * Returns the utf8 constant index as described in the JVM specifications
	 * if the tag item is 'e'.
	 * The value is unspecified otherwise.
	 *
	 * @return the enum constant index
	 * @since 3.1
	 */
	int getEnumConstantTypeNameIndex();

	/**
	 * Returns the tag as described in the JVM specifications.
	 *
	 * @return the tag
	 */
	int getTag();

	/**
	 * Returns the number of values as described in the JVM specifications
	 * if the tag item is '['.
	 * The value is unspecified otherwise.
	 *
	 * @return the number of values
	 */
	int getValuesNumber();
}
