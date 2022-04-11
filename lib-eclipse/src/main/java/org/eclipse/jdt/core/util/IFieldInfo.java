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
 * Description of a field info as described in the JVM
 * specifications.
 *
 * This interface may be implemented by clients.
 *
 * @since 2.0
 */
public interface IFieldInfo {

	/**
	 * Answer back the constant value attribute of this field info if specified,
	 * null otherwise.
	 *
	 * @return the constant value attribute of this field info if specified,
	 * null otherwise
	 */
	IConstantValueAttribute getConstantValueAttribute();

	/**
	 * Answer back the access flag of this field info.
	 *
	 * @return the access flag of this field info
	 */
	int getAccessFlags();

	/**
	 * Answer back the name of this field info. The name is returned as
	 * specified in the JVM specifications.
	 *
	 * @return the name of this field info. The name is returned as
	 * specified in the JVM specifications
	 */
	char[] getName();

	/**
	 * Answer back the name index of this field info.
	 *
	 * @return the name index of this field info
	 */
	int getNameIndex();

	/**
	 * Answer back the descriptor of this field info. The descriptor is returned as
	 * specified in the JVM specifications.
	 *
	 * @return the descriptor of this field info. The descriptor is returned as
	 * specified in the JVM specifications
	 */
	char[] getDescriptor();

	/**
	 * Answer back the descriptor index of this field info.
	 *
	 * @return the descriptor index of this field info
	 */
	int getDescriptorIndex();

	/**
	 * Return true if the field info has a constant value attribute, false otherwise.
	 *
	 * @return true if the field info has a constant value attribute, false otherwise
	 */
	boolean hasConstantValueAttribute();

	/**
	 * Return true if the field info is synthetic according to the JVM specification, false otherwise.
	 * <p>Note that prior to JDK 1.5, synthetic fields were always marked using
	 * an attribute; with 1.5, synthetic fields can also be marked using
	 * the {@link IModifierConstants#ACC_SYNTHETIC} flag.
	 * </p>
	 *
	 * @return true if the field info is synthetic according to the JVM specification, false otherwise
	 */
	boolean isSynthetic();

	/**
	 * Return true if the field info has a deprecated attribute, false otherwise.
	 *
	 * @return true if the field info has a deprecated attribute, false otherwise
	 */
	boolean isDeprecated();

	/**
	 * Answer back the attribute number of the field info.
	 *
	 * @return the attribute number of the field info
	 */
	int getAttributeCount();


	/**
	 * Answer back the collection of all attributes of the field info. It
	 * includes SyntheticAttribute, ConstantValueAttributes, etc.
	 * Returns an empty collection if none.
	 *
	 * @return the collection of all attributes of the field info. It
	 * includes SyntheticAttribute, ConstantValueAttributes, etc.
	 * Returns an empty collection if none
	 */
	IClassFileAttribute[] getAttributes();
}
