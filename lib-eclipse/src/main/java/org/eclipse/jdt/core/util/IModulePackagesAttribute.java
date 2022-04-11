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
 * Description of a Module Packages as described in the JVMS9 4.7.26
 *
 * This interface may be implemented by clients.
 *
 * @since 3.14
 */
public interface IModulePackagesAttribute extends IClassFileAttribute {

	/**
	 * Answer back the number of packages exported or opened.
	 *
	 * @return the number of packages exported or opened
	 */
	int getPackagesCount();

	/**
	 * Answer back the array of package indices.
	 *
	 * @return the array of package indices.
	 */
	int[] getPackageIndices();

	/**
	 * Answer back the names of packages. Answers an empty collection if none.
	 *
	 * @return the names of packages
	 */
	char[][] getPackageNames();
}
