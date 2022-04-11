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
 *
 *******************************************************************************/

package org.eclipse.jdt.core.dom;

/**
 * A module binding represents a module (added in JLS9 API).
 *
 * @since 3.14
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IModuleBinding extends IBinding {

	@Override
	public default int getKind() {
		return IBinding.MODULE;
	}

	/**
	 * Returns whether this module is open or not.
	 *
	 * @return <code>true</code> if open, <code>false</code> otherwise
	 */
	public abstract boolean isOpen();

	/**
	 * Returns all required modules.
	 * <p>The resulting bindings are in no particular order.</p>
	 *
	 * @return all required modules
	 */
	public abstract IModuleBinding[] getRequiredModules();

	/**
	 * Returns all exported packages.
	 * <p>The resulting bindings are in no particular order.</p>
	 *
	 * @return array of exported package bindings
	 */
	public abstract IPackageBinding[] getExportedPackages();

	/**
	 * If this module exports the given package to specific modules, returns the array of names of
	 * modules, otherwise returns an empty array.
	 *
	 * @param packageBinding a package binding for which targeted modules are declared
	 * @return array of names of targeted modules
	 */
	public abstract String[] getExportedTo(IPackageBinding packageBinding);

	/**
	 * Returns all opened packages.
	 * <p>The resulting bindings are in no particular order.</p>
	 *
	 * @return array of package bindings
	 */
	public abstract IPackageBinding[] getOpenedPackages();

	/**
	 * If this module opens the given package to specific modules, returns the array of names of
	 * modules, otherwise returns an empty array.
	 * <p>The resulting bindings are in no particular order.</p>
	 *
	 * @param packageBinding a package binding for which targeted modules are declared
	 * @return array of names of targeted modules
	 */
	public abstract String[] getOpenedTo(IPackageBinding packageBinding);

	/**
	 * Returns the services used by this module.
	 * <p>The resulting bindings are in no particular order.</p>
	 *
	 * @return array of type bindings
	 */
	public abstract ITypeBinding[] getUses();

	/**
	 * Returns the services provided by this module.
	 * <p>The resulting services are in no particular order.</p>
	 *
	 * @return array of services
	 */
	public abstract ITypeBinding[] getServices();

	/**
	 * Returns the implementations that implement the given service in this module.
	 *
	 * @return array of implementation type bindings, in declaration order
	 */
	public abstract ITypeBinding[] getImplementations(ITypeBinding service);
}