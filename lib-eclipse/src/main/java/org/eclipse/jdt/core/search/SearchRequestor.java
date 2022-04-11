/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
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

import org.eclipse.core.runtime.CoreException;

/**
 * Collects the results from a search engine query.
 * Clients implement a subclass to pass to <code>SearchEngine.search</code>
 * and implement the {@link #acceptSearchMatch(SearchMatch)} method, and
 * possibly override other life cycle methods.
 * <p>
 * The search engine calls <code>beginReporting()</code> when a search starts,
 * then calls <code>acceptSearchMatch(...)</code> for each search result, and
 * finally calls <code>endReporting()</code>. The order of the search results
 * is unspecified and may vary from request to request; when displaying results,
 * clients should not rely on the order but should instead arrange the results
 * in an order that would be more meaningful to the user.
 * </p>
 *
 * @see SearchEngine
 * @since 3.0
 */
public abstract class SearchRequestor {

	/**
	 * Accepts the given search match.
	 *
	 * @param match the found match
	 * @throws CoreException
	 */
	public abstract void acceptSearchMatch(SearchMatch match) throws CoreException;

	/**
	 * Notification sent before starting the search action.
	 * Typically, this would tell a search requestor to clear previously
	 * recorded search results.
	 * <p>
	 * The default implementation of this method does nothing. Subclasses
	 * may override.
	 * </p>
	 */
	public void beginReporting() {
		// do nothing
	}

	/**
	 * Notification sent after having completed the search action.
	 * Typically, this would tell a search requestor collector that no more
	 * results will be forthcoming in this search.
	 * <p>
	 * The default implementation of this method does nothing. Subclasses
	 * may override.
	 * </p>
	 */
	public void endReporting() {
		// do nothing
	}

	/**
	 * Intermediate notification sent when the given participant starts to
	 * contribute.
	 * <p>
	 * The default implementation of this method does nothing. Subclasses
	 * may override.
	 * </p>
	 *
	 * @param participant the participant that is starting to contribute
	 */
	public void enterParticipant(SearchParticipant participant) {
		// do nothing
	}

	/**
	 * Intermediate notification sent when the given participant is finished
	 * contributing.
	 * <p>
	 * The default implementation of this method does nothing. Subclasses
	 * may override.
	 * </p>
	 *
	 * @param participant the participant that finished contributing
	 */
	public void exitParticipant(SearchParticipant participant) {
		// do nothing
	}
}
