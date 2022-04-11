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
 * Description of an exports/opens info as described in the JVM specifications 4.7.25
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 3.14
 */
public interface IPackageVisibilityInfo {

	/**
	 * Answer back the exports/opens index.
	 *
	 * @return the exports/opens index
	 */
	int getIndex();

	/**
	 * Answer back the exports/opens package.
	 *
	 * @return the exports/opens package
	 */
	char[] getPackageName();

	/**
	 * Answer back the exports/opens flags.
	 *
	 * @return the exports/opens flags
	 */
	int getFlags();

	/**
	 * Answer back the number of targets, zero if none.
	 *
	 * @return the number of targets, zero if none.
	 */
	int getTargetsCount();

	/**
	 * Answer back the array of target module indices.
	 *
	 * @return the array of target module indices.
	 */
	int[] getTargetModuleIndices();

	/**
	 * Answer back the array of target module names.
	 *
	 * @return the array of target module names.
	 */
	char[][] getTargetModuleNames();
}
