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

/**
 * The name environment provides a callback API that the compiler
 * can use to look up types, compilation units, and packages in the
 * current environment.  The name environment is passed to the compiler
 * on creation.
 *
 * This name environment adds a method to switch on/off the search for secondary types.
 * Refer {@link #findType(char[], char[][], boolean)}.
 *
 */
public interface INameEnvironmentExtension extends INameEnvironment {
	/**
	 * Find a type named <typeName> in the package <packageName>.
	 * Answer the binary form of the type if it is known to be consistent.
	 * Otherwise, answer the compilation unit which defines the type
	 * or null if the type does not exist.
	 * The default package is indicated by char[0][].
	 *
	 * It is known that the package containing the type exists.
	 *
	 * NOTE: This method can be used to find a member type using its
	 * internal name A$B, but the source file for A is answered if the binary
	 * file is inconsistent.
	 *
	 * The flag <searchWithSecondaryTypes> can be used to switch on/off the search for secondary types.
	 * This is useful because the search for secondary types may by very expensive regarding the performance
	 * and in many cases it isn't necessary to search for secondary types.
	 *
	 * @param typeName type to find
	 * @param packageName package of the searched type
	 * @param searchWithSecondaryTypes flag to switch on/off the search for secondary types
	 * @param moduleName the name of the module to search in, or one of ModuleBinding.ANY, ModuleBinding.UNNAMED
	 * @return {@link NameEnvironmentAnswer}
	 */
	NameEnvironmentAnswer findType(char[] typeName, char[][] packageName, boolean searchWithSecondaryTypes, char[] moduleName);

	default NameEnvironmentAnswer findType(char[] typeName, char[][] packageName, boolean searchWithSecondaryTypes) {
		return findType(typeName, packageName, searchWithSecondaryTypes, null);
	}

}
