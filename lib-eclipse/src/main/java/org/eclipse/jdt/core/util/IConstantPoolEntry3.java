/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 3.14
 */
public interface IConstantPoolEntry3 extends IConstantPoolEntry2 {
	/**
	 * Returns the index for a CONSTANT_Module type entry.
	 * The value is unspecified otherwise.
	 *
	 * @return the index for a CONSTANT_Module type entry
	 * @see IConstantPoolConstant#CONSTANT_Module
	 * @see #getModuleName()
	 */
	int getModuleIndex();

	/**
	 * Returns the name of a CONSTANT_Module type entry.
	 * Returns null otherwise.
	 *
	 * @return the name of a CONSTANT_Module type entry
	 * @see IConstantPoolConstant#CONSTANT_Module
	 * @see #getModuleIndex()
	 */
	char[] getModuleName();

	/**
	 * Returns the index for a CONSTANT_Package type entry.
	 * The value is unspecified otherwise.
	 *
	 * @return the index for a CONSTANT_Package type entry
	 * @see IConstantPoolConstant#CONSTANT_Package
	 * @see #getPackageName()
	 */
	int getPackageIndex();

	/**
	 * Returns the name of a CONSTANT_Package type entry.
	 * Returns null otherwise.
	 *
	 * @return the name of a CONSTANT_Package type entry
	 * @see IConstantPoolConstant#CONSTANT_Package
	 * @see #getPackageIndex()
	 */
	char[] getPackageName();
}
