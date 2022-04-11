/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
 *     Nikolay Botev - Bug 348507
 *******************************************************************************/
package org.eclipse.jdt.internal.core.search;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.compiler.env.AccessRuleSet;
import org.eclipse.jdt.internal.core.DeltaProcessor;
import org.eclipse.jdt.internal.core.DeltaProcessor.RootInfo;
import org.eclipse.jdt.internal.core.ExternalFoldersManager;
import org.eclipse.jdt.internal.core.JavaModel;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.jdt.internal.core.JavaProject;
import org.eclipse.jdt.internal.core.util.Util;

/**
 * A Java-specific scope for searching the entire workspace.
 * The scope can be configured to not search binaries. By default, binaries
 * are included.
 */
public class JavaWorkspaceScope extends AbstractJavaSearchScope {

	private IPath[] enclosingPaths = null;

public JavaWorkspaceScope() {
	// As nothing is stored in the JavaWorkspaceScope now, no initialization is longer needed
}

@Override
public boolean encloses(IJavaElement element) {
	/*A workspace scope encloses all java elements (this assumes that the index selector
	 * and thus enclosingProjectAndJars() returns indexes on the classpath only and that these
	 * indexes are consistent.)
	 * NOTE: Returning true gains 20% of a hierarchy build on Object
	 */
	return true;
}
@Override
public boolean encloses(String resourcePathString) {
	/*A workspace scope encloses all resources (this assumes that the index selector
	 * and thus enclosingProjectAndJars() returns indexes on the classpath only and that these
	 * indexes are consistent.)
	 * NOTE: Returning true gains 20% of a hierarchy build on Object
	 */
	return true;
}

@Override
public IPath[] enclosingProjectsAndJars() {
	IPath[] result = this.enclosingPaths;
	if (result != null) {
		return result;
	}
	long start = BasicSearchEngine.VERBOSE ? System.currentTimeMillis() : -1;
	try {
		IJavaProject[] projects = JavaModelManager.getJavaModelManager().getJavaModel().getJavaProjects();
		// use a linked set to preserve the order during search: see bug 348507
		Set<IPath> paths = new LinkedHashSet<>(projects.length * 2);
		for (int i = 0, length = projects.length; i < length; i++) {
			JavaProject javaProject = (JavaProject) projects[i];

			// Add project full path
			IPath projectPath = javaProject.getProject().getFullPath();
			paths.add(projectPath);
		}

		// add the project source paths first in a separate loop above
		// to ensure source files always get higher precedence during search.
		// see bug 348507

		for (int i = 0, length = projects.length; i < length; i++) {
			JavaProject javaProject = (JavaProject) projects[i];

			// Add project libraries paths
			IClasspathEntry[] entries = javaProject.getResolvedClasspath();
			for (int j = 0, eLength = entries.length; j < eLength; j++) {
				IClasspathEntry entry = entries[j];
				if (entry.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
					IPath path = entry.getPath();
					Object target = JavaModel.getTarget(path, false/*don't check existence*/);
					if (target instanceof IFolder) // case of an external folder
						path = ((IFolder) target).getFullPath();
					paths.add(entry.getPath());
				}
			}
		}
		result = new IPath[paths.size()];
		paths.toArray(result);
		return this.enclosingPaths = result;
	} catch (JavaModelException e) {
		Util.log(e, "Exception while computing workspace scope's enclosing projects and jars"); //$NON-NLS-1$
		return new IPath[0];
	} finally {
		if (BasicSearchEngine.VERBOSE) {
			long time = System.currentTimeMillis() - start;
			int length = result == null ? 0 : result.length;
			Util.verbose("JavaWorkspaceScope.enclosingProjectsAndJars: "+length+" paths computed in "+time+"ms."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
	}
}

@Override
public boolean equals(Object o) {
  return o == this; // use the singleton pattern
}

@Override
public AccessRuleSet getAccessRuleSet(String relativePath, String containerPath) {
	// Do not consider access rules on workspace scope
	return null;
}

@Override
public int hashCode() {
	return JavaWorkspaceScope.class.hashCode();
}

/**
 * @see AbstractJavaSearchScope#packageFragmentRoot(String, int, String)
 */
@Override
public IPackageFragmentRoot packageFragmentRoot(String resourcePathString, int jarSeparatorIndex, String jarPath) {
	Map<IPath, RootInfo> rootInfos = JavaModelManager.getDeltaState().roots;
	DeltaProcessor.RootInfo rootInfo = null;
	if (jarPath != null) {
		IPath path = new Path(jarPath);
		rootInfo = rootInfos.get(path);
	} else {
		IPath path = new Path(resourcePathString);
		if (ExternalFoldersManager.isInternalPathForExternalFolder(path)) {
			IResource resource = JavaModel.getWorkspaceTarget(path.uptoSegment(2/*linked folders for external folders are always of size 2*/));
			if (resource != null)
				rootInfo = rootInfos.get(resource.getLocation());
		} else {
			rootInfo = rootInfos.get(path);
			while (rootInfo == null && path.segmentCount() > 0) {
				path = path.removeLastSegments(1);
				rootInfo = rootInfos.get(path);
			}
		}
	}
	if (rootInfo == null)
		return null;
	return rootInfo.getPackageFragmentRoot(null/*no resource hint*/);
}

@Override
public void processDelta(IJavaElementDelta delta, int eventType) {
	if (this.enclosingPaths == null) return;
	IJavaElement element = delta.getElement();
	switch (element.getElementType()) {
		case IJavaElement.JAVA_MODEL:
			IJavaElementDelta[] children = delta.getAffectedChildren();
			for (int i = 0, length = children.length; i < length; i++) {
				IJavaElementDelta child = children[i];
				processDelta(child, eventType);
			}
			break;
		case IJavaElement.JAVA_PROJECT:
			int kind = delta.getKind();
			switch (kind) {
				case IJavaElementDelta.ADDED:
				case IJavaElementDelta.REMOVED:
					this.enclosingPaths = null;
					break;
				case IJavaElementDelta.CHANGED:
					int flags = delta.getFlags();
					if ((flags & IJavaElementDelta.F_CLOSED) != 0
							|| (flags & IJavaElementDelta.F_OPENED) != 0) {
						this.enclosingPaths = null;
					} else {
						children = delta.getAffectedChildren();
						for (int i = 0, length = children.length; i < length; i++) {
							IJavaElementDelta child = children[i];
							processDelta(child, eventType);
						}
					}
					break;
			}
			break;
		case IJavaElement.PACKAGE_FRAGMENT_ROOT:
			kind = delta.getKind();
			switch (kind) {
				case IJavaElementDelta.ADDED:
				case IJavaElementDelta.REMOVED:
					this.enclosingPaths = null;
					break;
				case IJavaElementDelta.CHANGED:
					int flags = delta.getFlags();
					if ((flags & IJavaElementDelta.F_ADDED_TO_CLASSPATH) > 0
						|| (flags & IJavaElementDelta.F_REMOVED_FROM_CLASSPATH) > 0) {
						this.enclosingPaths = null;
					}
					break;
			}
			break;
	}
}


@Override
public String toString() {
	StringBuilder result = new StringBuilder("JavaWorkspaceScope on "); //$NON-NLS-1$
	IPath[] paths = enclosingProjectsAndJars();
	int length = paths == null ? 0 : paths.length;
	if (length == 0) {
		result.append("[empty scope]"); //$NON-NLS-1$
	} else {
		result.append("["); //$NON-NLS-1$
		for (int i = 0; i < length; i++) {
			result.append("\n\t"); //$NON-NLS-1$
			result.append(paths[i]);
		}
		result.append("\n]"); //$NON-NLS-1$
	}
	return result.toString();
}

@Override
public boolean isParallelSearchSupported() {
	return true;
}
}
