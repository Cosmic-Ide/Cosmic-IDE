/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.IJavaElement;

/**
 * A <code>IJavaSearchResultCollector</code> collects search results from a <code>search</code>
 * query to a <code>SearchEngine</code>. Clients must implement this interface and pass
 * an instance to the <code>search(...)</code> methods. When a search starts, the <code>aboutToStart()</code>
 * method is called, then 0 or more call to <code>accept(...)</code> are done, finally the
 * <code>done()</code> method is called.
 * <p>
 * Results provided to this collector may be accurate - in this case they have an <code>EXACT_MATCH</code> accuracy -
 * or they might be potential matches only - they have a <code>POTENTIAL_MATCH</code> accuracy. This last
 * case can occur when a problem prevented the <code>SearchEngine</code> from resolving the match.
 * </p>
 * <p>
 * The order of the results is unspecified. Clients must not rely on this order to display results,
 * but they should sort these results (for example, in syntactical order).
 * <p>
 * The <code>IJavaSearchResultCollector</code> is also used to provide a progress monitor to the
 * <code>SearchEngine</code>.
 * </p>
 * <p>
 * Clients may implement this interface.
 * </p>
 *
 * @see SearchEngine
 * @deprecated Since 3.0, the class
 * {@link org.eclipse.jdt.core.search.SearchRequestor} replaces this interface.
 */
public interface IJavaSearchResultCollector {
	/**
	 * The search result corresponds exactly to the search pattern.
	 *
     * @deprecated Use {@link SearchMatch#A_ACCURATE} instead.
	 */
	int EXACT_MATCH = 0;

	/**
	 * The search result is potentially a match for the search pattern,
	 * but a problem prevented the search engine from being more accurate
	 * (typically because of the classpath was not correctly set).
	 *
     * @deprecated Use {@link SearchMatch#A_INACCURATE} instead.
	 */
	 int POTENTIAL_MATCH = 1;

/**
 * Called before the actual search starts.
 *
 * @deprecated Replaced by {@link SearchRequestor#beginReporting()}.
 */
public void aboutToStart();
/**
 * Accepts the given search result.
 *
 * @param resource the resource in which the match has been found
 * @param start the start position of the match, -1 if it is unknown
 * @param end the end position of the match, -1 if it is unknown;
 *  the ending offset is exclusive, meaning that the actual range of characters
 *  covered is <code>[start, end]</code>
 * @param enclosingElement the Java element that contains the character range
 *	<code>[start, end]</code>; the value can be <code>null</code> indicating that
 *	no enclosing Java element has been found
 * @param accuracy the level of accuracy the search result has; either
 *  <code>EXACT_MATCH</code> or <code>POTENTIAL_MATCH</code>
 * @exception CoreException if this collector had a problem accepting the search result
 * @deprecated Replaced by {@link SearchRequestor#acceptSearchMatch(SearchMatch)}.
 */
public void accept(
	IResource resource,
	int start,
	int end,
	IJavaElement enclosingElement,
	int accuracy)
	throws CoreException;
/**
 * Called when the search has ended.
 *
 * @deprecated Replaced by {@link SearchRequestor#endReporting()}.
 */
public void done();
/**
 * Returns the progress monitor used to report progress.
 *
 * @return a progress monitor or null if no progress monitor is provided
 */
public IProgressMonitor getProgressMonitor();
}
