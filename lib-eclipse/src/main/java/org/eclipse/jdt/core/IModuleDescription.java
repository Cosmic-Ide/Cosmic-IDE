/*******************************************************************************
 * Copyright (c) 2017, 2019 IBM Corporation.
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
package org.eclipse.jdt.core;

/**
 * Represents a Java module descriptor. The module description could either come from source or binary.
 * A simple module looks like the following:
 * <pre>
 * module my.module {
 * 		exports my.pack1;
 * 		exports my.pack2;
 * 		requires java.sql;
 * }
 * </pre>
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 3.14
 */
public interface IModuleDescription extends IMember, IAnnotatable {

	/**
	 * Answer the names of all modules directly required from this module.
	 *
	 * @return a non-null array of module names
	 * @since 3.14
	 */
	String[] getRequiredModuleNames() throws JavaModelException;

	/**
	 * Get provided service names for this module.
	 *
	 * @return a non-null array of provided service names
	 * @throws JavaModelException
	 * @since 3.18
	 */
	String[] getProvidedServiceNames() throws JavaModelException;

	/**
	 * Get used service names for this module.
	 *
	 * @return a non-null array of used service names
	 * @throws JavaModelException
	 * @since 3.18
	 */
	String[] getUsedServiceNames() throws JavaModelException;

	/**
	 * Get names of exported packages.
	 *
	 * @param targetModule filter the result to include only packages exported to the given module, unless {@code null}.
	 * @return a non-null array of exported package names
	 * @throws JavaModelException
	 * @since 3.18
	 */
	String[] getExportedPackageNames(IModuleDescription targetModule) throws JavaModelException;

	/**
	 * Get names of opened packages.
	 *
	 * @param targetModule filter the result to include only packages opened to the given module, unless {@code null}.
	 * @return a non-null array of opened package names
	 * @throws JavaModelException
	 * @since 3.18
	 */
	String[] getOpenedPackageNames(IModuleDescription targetModule) throws JavaModelException;

	/**
	 *
	 * @return true if automatic module, else false
	 * @since 3.14
	 */
	default boolean isAutoModule() {
		return false;
	}

	/**
	 * @return true if this module is a system module, else false
	 * @since 3.20
	 */
	default boolean isSystemModule() {
		return false;
	}
}
