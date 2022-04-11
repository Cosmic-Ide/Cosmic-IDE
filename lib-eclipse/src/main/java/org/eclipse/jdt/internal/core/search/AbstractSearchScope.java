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

import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.IParallelizable;

public abstract class AbstractSearchScope implements IJavaSearchScope, IParallelizable, Cloneable {

/**
 * @see IJavaSearchScope#includesBinaries()
 * @deprecated
 */
@Override
public boolean includesBinaries() {
	return true;
}

/**
 * @see IJavaSearchScope#includesClasspaths()
 * @deprecated
 */
@Override
public boolean includesClasspaths() {
	return true;
}

/* (non-Javadoc)
 * Process the given delta and refresh its internal state if needed.
 * Returns whether the internal state was refreshed.
 */
public abstract void processDelta(IJavaElementDelta delta, int eventType);

/**
 * @see IJavaSearchScope#setIncludesBinaries(boolean)
 * @deprecated
 */
@Override
public void setIncludesBinaries(boolean includesBinaries) {
	// implements interface method
}

/**
 * @see IJavaSearchScope#setIncludesClasspaths(boolean)
 * @deprecated
 */
@Override
public void setIncludesClasspaths(boolean includesClasspaths) {
	// implements interface method
}

@Override
public boolean isParallelSearchSupported() {
	return false;
}

@Override
public AbstractSearchScope clone() throws CloneNotSupportedException {
	return (AbstractSearchScope) super.clone();
}
}
