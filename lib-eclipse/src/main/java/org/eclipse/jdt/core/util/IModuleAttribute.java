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
 * Description of a module's attributes as described in the JVM specifications.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 3.14
 */
public interface IModuleAttribute extends IClassFileAttribute {

	/**
	 * Answer back the module name index.
	 *
	 * @return the module name index
	 */
	int getModuleNameIndex();

	/**
	 * Answer back the module name.
	 *
	 * @return the module name
	 */
	char[] getModuleName();

	/**
	 * Answer back the module flags.
	 *
	 * @return the module flags
	 */
	int getModuleFlags();

	/**
	 * Answer back the module version index.
	 *
	 * @return the module version index
	 */
	int getModuleVersionIndex();

	/**
	 * Answer back the module version string.
	 *
	 * @return the module version string
	 */
	public char[] getModuleVersionValue();

	/**
	 * Answer back the requires count.
	 *
	 * @return the requires counts
	 */
	int getRequiresCount();

	/**
	 * Answer back the array of requires infos of the .class file,
	 * an empty array if none.
	 *
	 * @return the array of requires infos of the .class file, an empty array if none
	 */
	IRequiresInfo[] getRequiresInfo();

	/**
	 * Answer back the exports count.
	 *
	 * @return the exports counts
	 */
	int getExportsCount();

	/**
	 * Answer back the array of exports infos of the .class file,
	 * an empty array if none.
	 *
	 * @return the array of exports infos of the .class file, an empty array if none
	 */
	IPackageVisibilityInfo[] getExportsInfo();

	/**
	 * Answer back the opens count.
	 *
	 * @return the opens counts
	 */
	int getOpensCount();

	/**
	 * Answer back the array of opens infos of the .class file,
	 * an empty array if none.
	 *
	 * @return the array of opens infos of the .class file, an empty array if none
	 */
	IPackageVisibilityInfo[] getOpensInfo();

	/**
	 * Answer back the uses count.
	 *
	 * @return the uses counts
	 */
	int getUsesCount();

	/**
	 * Answer back the array of uses indices of the .class file,
	 * an empty array if none.
	 *
	 * @return the array of uses indices of the .class file, an empty array if none
	 */
	int[] getUsesIndices();

	/**
	 * Answer back the array of uses class names of the .class file,
	 * an empty array if none.
	 *
	 * @return the array of uses class names of the .class file, an empty array if none
	 */
	char[][] getUsesClassNames();

	/**
	 * Answer back the provides count.
	 *
	 * @return the provides counts
	 */
	int getProvidesCount();

	/**
	 * Answer back the array of provides infos of the .class file,
	 * an empty array if none.
	 *
	 * @return the array of provides infos of the .class file, an empty array if none
	 */
	IProvidesInfo[] getProvidesInfo();
}
