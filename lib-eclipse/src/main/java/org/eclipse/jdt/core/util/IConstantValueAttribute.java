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
 * Description of a constant value attribute as described in the JVM
 * specifications.
 *
 * This interface may be implemented by clients.
 *
 * @since 2.0
 */
public interface IConstantValueAttribute extends IClassFileAttribute {

	/**
	 * Answer back the constant value index.
	 *
	 * @return the constant value index
	 */
	int getConstantValueIndex();

	/**
	 * Answer back the constant pool entry that represents the constant
	 * value of this attribute.
	 *
	 * @return the constant pool entry that represents the constant
	 * value of this attribute
	 */
	IConstantPoolEntry getConstantValue();
}
