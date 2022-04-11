/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
package org.eclipse.jdt.internal.core.search;

import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.internal.compiler.env.AccessRuleSet;


public abstract class AbstractJavaSearchScope extends AbstractSearchScope {

/**
 * Get access rule set corresponding to a given path.
 * @param relativePath The path user want to have restriction access
 * @return The access rule set for given path or null if none is set for it.
 * 	Returns specific unit access rule set when scope does not enclose the given path.
 */
abstract public AccessRuleSet getAccessRuleSet(String relativePath, String containerPath);

/**
 * Returns the package fragment root corresponding to a given resource path.
 *
 * @param resourcePathString path of expected package fragment root.
 * @param jarSeparatorIndex the index of the jar separator in the resource path, or -1 if none
 * @param jarPath the already extracted jar path, or null if none
 * @return the {@link IPackageFragmentRoot package fragment root} which path
 * 	match the given one or <code>null</code> if none was found.
 */
abstract public IPackageFragmentRoot packageFragmentRoot(String resourcePathString, int jarSeparatorIndex, String jarPath);
}
