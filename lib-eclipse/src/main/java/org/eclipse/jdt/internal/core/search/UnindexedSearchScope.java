/*******************************************************************************
 * Copyright (c) 2015, 2016 Google, Inc and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Stefan Xenos (Google) - Initial implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.core.search;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;

public class UnindexedSearchScope extends AbstractSearchScope {
	private IJavaSearchScope searchScope;

	private UnindexedSearchScope(IJavaSearchScope scope) {
		this.searchScope = scope;
	}

	public static IJavaSearchScope filterEntriesCoveredByTheNewIndex(IJavaSearchScope scope) {
		return new UnindexedSearchScope(scope);
	}

	@Override
	public boolean encloses(String resourcePathString) {
		int separatorIndex = resourcePathString.indexOf(JAR_FILE_ENTRY_SEPARATOR);
		if (separatorIndex != -1) {
			// Files within jar files would have been indexed
			return false;
		}

		// Jar files themselves would have been indexed
		if (isJarFile(resourcePathString)) {
			return false;
		}

		// Consult with the search scope
		return this.searchScope.encloses(resourcePathString);
	}

	private boolean isJarFile(String possibleJarFile) {
		if (possibleJarFile == null) {
			return false;
		}
		return (possibleJarFile.endsWith(".jar") || possibleJarFile.endsWith(".JAR")); //$NON-NLS-1$//$NON-NLS-2$
	}

	@Override
	public boolean encloses(IJavaElement element) {
		try {
			IResource underlyingResource = element.getUnderlyingResource();

			if (underlyingResource != null && isJarFile(underlyingResource.getName())) {
				return false;
			}
		} catch (JavaModelException e) {
			JavaCore.getPlugin().getLog().log(e.getStatus());
		}
		return this.searchScope.encloses(element);
	}

	@Override
	public IPath[] enclosingProjectsAndJars() {
		IPath[] unfiltered = this.searchScope.enclosingProjectsAndJars();

		List<IPath> result = new ArrayList<>();

		for (IPath next : unfiltered) {
			if (isJarFile(next.lastSegment())) {
				continue;
			}
			result.add(next);
		}
		return result.toArray(new IPath[result.size()]);
	}

	@Override
	public void processDelta(IJavaElementDelta delta, int eventType) {
		if (this.searchScope instanceof AbstractSearchScope) {
			AbstractSearchScope inner = (AbstractSearchScope) this.searchScope;

			inner.processDelta(delta, eventType);
		}
	}

	@Override
	public boolean isParallelSearchSupported() {
		return true;
	}

}
