/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
package org.eclipse.jdt.core.search;

/**
 * A search pattern defines how search results are found. Use <code>SearchEngine.createSearchPattern</code>
 * to create a search pattern.
 *
 * @see SearchEngine#createSearchPattern(org.eclipse.jdt.core.IJavaElement, int)
 * @see SearchEngine#createSearchPattern(String, int, int, boolean)
 * @deprecated Since 3.0, the class
 * {@link org.eclipse.jdt.core.search.SearchPattern} replaces this interface.
 */
public interface ISearchPattern {
	// used as a marker interface: contains no methods
}
