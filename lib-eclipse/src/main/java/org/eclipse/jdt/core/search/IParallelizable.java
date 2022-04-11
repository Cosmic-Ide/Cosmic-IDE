/*******************************************************************************
 * Copyright (c) 2020 Gayan Perera and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Gayan Perera - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.core.search;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.search.JavaSearchParticipant;

/**
 * This interface can be used by {@link IJavaSearchScope}, {@link JavaSearchParticipant} and {@link SearchPattern} to
 * mark implementors as eligible for parallel index search.
 *
 * @since 3.25
 */
public interface IParallelizable {
	/**
	 * Answers {@code true} if the current instance supports parallel index search
	 *
	 * @return Returns <code>true</code> if the implementation is safe to be used in a parallel search.
	 */
	boolean isParallelSearchSupported();

	/**
	 * Initialize all needed data before search is started
	 *
	 * @param monitor
	 *            non null progress callback
	 */
	default void initBeforeSearch(IProgressMonitor monitor) throws JavaModelException {
		// no op
	}

	/**
	 * Checks if the given object implements this interface and also returns <code>true</code> for
	 * {@link #isParallelSearchSupported()}.
	 *
	 * @param o
	 *            The object that needs to be checked. <code>null</code> value will result in returning
	 *            <code>false</code>.
	 * @return <code>true</code> if the given object can be used in parallel search.
	 */
	public static boolean isParallelSearchSupported(Object o) {
		return (o instanceof IParallelizable) && ((IParallelizable) o).isParallelSearchSupported();
	}

}
