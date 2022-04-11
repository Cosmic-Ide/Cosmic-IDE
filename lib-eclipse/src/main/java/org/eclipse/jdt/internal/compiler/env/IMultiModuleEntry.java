/*******************************************************************************
 * Copyright (c) 2016, 2017 IBM Corporation and others.
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

import java.util.Collection;

/**
 * Represents a module path entry that represents a collection of modules
 * like a jimage or an exploded module directory structure
 *
 */
public interface IMultiModuleEntry extends IModulePathEntry {

	/**
	 * Get the module named name that this entry contributes to the module path
	 */

	@Override
	IModule getModule(char[] name);

	/**
	 * Get the names of all modules served by this entry.
	 * @param limitModules if non-null, only modules with names in this set and their
	 * 	transitive closure will be reported.
	 */
	Collection<String> getModuleNames(Collection<String> limitModules);

}