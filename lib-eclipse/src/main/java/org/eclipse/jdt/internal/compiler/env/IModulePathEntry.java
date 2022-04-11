/*******************************************************************************
 * Copyright (c) 2016, 2018 IBM Corporation and others.
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
package org.eclipse.jdt.internal.compiler.env;

import org.eclipse.jdt.core.compiler.CharOperation;

/**
 * Represents an entry on the module path of a project. Could be a single module or a collection of
 * modules (like a jimage or an exploded module directory structure)
 *
 */
public interface IModulePathEntry {

	/**
	 * Get the module that this entry contributes. May be null, for instance when this entry does not
	 * represent a single module
	 *
	 * @return The module that this entry contributes or null
	 */
	default IModule getModule() {
		return null;
	}

	/**
	 * Get the module named name from this entry. May be null
	 *
	 * @param name - The name of the module to look up
	 *
	 * @return The module named name or null
	 */
	default IModule getModule(char[] name) {
		IModule mod = getModule();
		if (mod != null && CharOperation.equals(name, mod.name()))
			return mod;
		return null;
	}

	/**
	 * Indicates whether this entry knows the module named name and can answer queries regarding the module
	 *
	 * @param name The name of the module
	 *
	 * @return True if this entry knows the module, false otherwise
	 */
	default boolean servesModule(char[] name) {
		return getModule(name) != null;
	}

	/**
	 * Answer the relevant modules that declare the given package.
	 * If moduleName is ModuleBinding.ANY then all packages are relevant,
	 * if moduleName is ModuleBinding.UNNAMED, then only packages in the unnamed module are relevant,
	 * otherwise consider only packages in the module identified by moduleName.
	 */
	char[][] getModulesDeclaringPackage(String qualifiedPackageName, /*@Nullable*/String moduleName);

	/**
	 * Answer whether the given package has any compilation unit (.java or .class) in the given module.
	 * For entries representing a single module, the module name should be checked before invoking this method.
	 * @param qualifiedPackageName '/'-separated package name
	 * @param moduleName if non-null only CUs attached to the given module should be considered
	 * @return true iff a .java or .class file could be found in the given module / package.
	 */
	boolean hasCompilationUnit(String qualifiedPackageName, String moduleName);

	/**
	 * Lists all packages in this modulepath entry.
	 * @return array of flat, dot-separated package names
	 */
	default char[][] listPackages() {
		return CharOperation.NO_CHAR_CHAR;
	}

	/**
	 * Specifies whether this entry represents an automatic module.
	 *
	 * @return true if this is an automatic module, false otherwise
	 */
	public default boolean isAutomaticModule() {
		return false;
	}
}