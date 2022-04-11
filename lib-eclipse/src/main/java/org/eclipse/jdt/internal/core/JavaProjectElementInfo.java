/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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
package org.eclipse.jdt.internal.core;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.DeltaProcessor.RootInfo;
import org.eclipse.jdt.internal.core.util.HashSetOfArray;
import org.eclipse.jdt.internal.core.util.HashtableOfArrayToObject;
import org.eclipse.jdt.internal.core.util.Util;

/**
 * Info for IJavaProject.
 * <p>
 * Note: <code>getChildren()</code> returns all of the <code>IPackageFragmentRoots</code>
 * specified on the classpath for the project.  This can include roots external to the
 * project. See <code>JavaProject#getAllPackageFragmentRoots()</code> and
 * <code>JavaProject#getPackageFragmentRoots()</code>.  To get only the <code>IPackageFragmentRoots</code>
 * that are internal to the project, use <code>JavaProject#getChildren()</code>.
 */

/* package */
class JavaProjectElementInfo extends OpenableElementInfo {

	static final IPackageFragmentRoot[] NO_ROOTS = new IPackageFragmentRoot[0];

	static class ProjectCache {
		ProjectCache(IPackageFragmentRoot[] allPkgFragmentRootsCache, Map<IPackageFragmentRoot, IClasspathEntry> rootToResolvedEntries, Map<IPackageFragmentRoot, HashSetOfArray> pkgFragmentsCaches) {
			this.allPkgFragmentRootsCache = allPkgFragmentRootsCache;
			this.rootToResolvedEntries = rootToResolvedEntries;
			this.pkgFragmentsCaches = pkgFragmentsCaches;
		}

		/*
		 * A cache of all package fragment roots of this project.
		 */
		public IPackageFragmentRoot[] allPkgFragmentRootsCache;

		/*
		 * A cache of all package fragments in this project.
		 * (a map from String[] (the package name) to IPackageFragmentRoot[] (the package fragment roots that contain a package fragment with this name))
		 */
		public HashtableOfArrayToObject allPkgFragmentsCache;

		/*
		 * A cache of package fragments for each package fragment root of this project
		 * (a map from IPackageFragmentRoot to a set of String[] (the package name))
		 */
		public Map<IPackageFragmentRoot, HashSetOfArray>pkgFragmentsCaches;

		/*
		 * A cache of package fragment roots to corresponding resolved CP entry
		 * (so as to be able to figure inclusion/exclusion rules)
		 */
		public Map<IPackageFragmentRoot, IClasspathEntry> rootToResolvedEntries;
	}

	ProjectCache projectCache;
	ProjectCache mainProjectCache;

	/*
	 * Adds the given name and its super names to the given set
	 * (e.g. for {"a", "b", "c"}, adds {"a", "b", "c"}, {"a", "b"}, and {"a"})
	 */
	static void addSuperPackageNames(String[] pkgName, HashtableOfArrayToObject packageFragments) {
		for (int i = pkgName.length-1; i > 0; i--) {
			if (packageFragments.getKey(pkgName, i) == null) {
				System.arraycopy(pkgName, 0, pkgName = new String[i], 0, i);
				packageFragments.put(pkgName, NO_ROOTS);
			}
		}
	}

	/**
	 * Create and initialize a new instance of the receiver
	 */
	public JavaProjectElementInfo() {
		this.nonJavaResources = null;
	}

	/**
	 * Compute the non-java resources contained in this java project.
	 */
	private Object[] computeNonJavaResources(JavaProject project) {

		// determine if src == project and/or if bin == project
		IPath projectPath = project.getProject().getFullPath();
		boolean srcIsProject = false;
		boolean binIsProject = false;
		char[][] inclusionPatterns = null;
		char[][] exclusionPatterns = null;
		IPath projectOutput = null;
		boolean isClasspathResolved = true;
		try {
			IClasspathEntry entry = project.getClasspathEntryFor(projectPath);
			if (entry != null) {
				srcIsProject = true;
				inclusionPatterns = ((ClasspathEntry)entry).fullInclusionPatternChars();
				exclusionPatterns = ((ClasspathEntry)entry).fullExclusionPatternChars();
			}
			projectOutput = project.getOutputLocation();
			binIsProject = projectPath.equals(projectOutput);
		} catch (JavaModelException e) {
			isClasspathResolved = false;
		}

		Object[] resources = new IResource[5];
		int resourcesCounter = 0;
		try {
			IResource[] members = ((IContainer) project.getResource()).members();
			int length = members.length;
			if (length > 0) {
				String sourceLevel = project.getOption(JavaCore.COMPILER_SOURCE, true);
				String complianceLevel = project.getOption(JavaCore.COMPILER_COMPLIANCE, true);
				IClasspathEntry[] classpath = project.getResolvedClasspath();
				for (int i = 0; i < length; i++) {
					IResource res = members[i];
					switch (res.getType()) {
						case IResource.FILE :
							IPath resFullPath = res.getFullPath();
							String resName = res.getName();

							// ignore a jar file on the classpath
							if (isClasspathResolved &&
									isClasspathEntryOrOutputLocation(resFullPath, res.getLocation()/* see https://bugs.eclipse.org/bugs/show_bug.cgi?id=244406 */, classpath, projectOutput)) {
								break;
							}
							// ignore .java file if src == project
							if (srcIsProject
									&& Util.isValidCompilationUnitName(resName, sourceLevel, complianceLevel)
									&& !Util.isExcluded(res, inclusionPatterns, exclusionPatterns)) {
								break;
							}
							// ignore .class file if bin == project
							if (binIsProject && Util.isValidClassFileName(resName, sourceLevel, complianceLevel)) {
								break;
							}
							// else add non java resource
							if (resources.length == resourcesCounter) {
								// resize
								System.arraycopy(
										resources,
										0,
										(resources = new IResource[resourcesCounter * 2]),
										0,
										resourcesCounter);
							}
							resources[resourcesCounter++] = res;
							break;
						case IResource.FOLDER :
							resFullPath = res.getFullPath();

							// ignore non-excluded folders on the classpath or that correspond to an output location
							if ((srcIsProject && !Util.isExcluded(res, inclusionPatterns, exclusionPatterns) && Util.isValidFolderNameForPackage(res.getName(), sourceLevel, complianceLevel))
									|| (isClasspathResolved && isClasspathEntryOrOutputLocation(resFullPath, res.getLocation(), classpath, projectOutput))) {
								break;
							}
							// else add non java resource
							if (resources.length == resourcesCounter) {
								// resize
								System.arraycopy(
										resources,
										0,
										(resources = new IResource[resourcesCounter * 2]),
										0,
										resourcesCounter);
							}
							resources[resourcesCounter++] = res;
					}
				}
			}
			if (resources.length != resourcesCounter) {
				System.arraycopy(
					resources,
					0,
					(resources = new IResource[resourcesCounter]),
					0,
					resourcesCounter);
			}
		} catch (CoreException e) {
			resources = NO_NON_JAVA_RESOURCES;
			resourcesCounter = 0;
		}
		return resources;
	}

	ProjectCache getProjectCache(JavaProject project, boolean excludeTestCode) {
		ProjectCache cache = excludeTestCode ? this.mainProjectCache : this.projectCache;
		if (cache != null) {
			for (IPackageFragmentRoot root : cache.allPkgFragmentRootsCache) {
				IJavaProject rootProject = root.getJavaProject();
				if (rootProject != project && !rootProject.exists()) {
					cache = null; // force rebuilding
					break;
				}
			}
		}
		if (cache == null) {
			IPackageFragmentRoot[] roots;
			Map<IPackageFragmentRoot, IClasspathEntry> reverseMap = new HashMap<>(3);
			try {
				roots = project.getAllPackageFragmentRoots(reverseMap, excludeTestCode);
			} catch (JavaModelException e) {
				// project does not exist: cannot happen since this is the info of the project
				roots = new IPackageFragmentRoot[0];
				reverseMap.clear();
			}

			Map<IPath, RootInfo> rootInfos = JavaModelManager.getJavaModelManager().deltaState.roots;
			HashMap<IPackageFragmentRoot, HashSetOfArray> pkgFragmentsCaches = new HashMap<>();
			int length = roots.length;
			JavaModelManager  manager = JavaModelManager.getJavaModelManager();
			for (int i = 0; i < length; i++) {
				IPackageFragmentRoot root = roots[i];
				DeltaProcessor.RootInfo rootInfo = rootInfos.get(root.getPath());
				if (rootInfo == null || rootInfo.project.equals(project)) {
					// ensure that an identical root is used (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=217059 )
					roots[i] = root = (IPackageFragmentRoot) manager.getExistingElement(root);
					// compute fragment cache
					HashSetOfArray fragmentsCache = new HashSetOfArray();
					initializePackageNames(root, fragmentsCache);
					pkgFragmentsCaches.put(root, fragmentsCache);
				}
			}

			cache = new ProjectCache(roots, reverseMap, pkgFragmentsCaches);
			if(excludeTestCode) {
				this.mainProjectCache = cache;
			} else {
				this.projectCache = cache;
			}
		}
		return cache;
	}

	/**
	 * Returns an array of non-java resources contained in the receiver.
	 */
	Object[] getNonJavaResources(JavaProject project) {
		Object[] resources = this.nonJavaResources;
		if (resources == null) {
			resources = computeNonJavaResources(project);
			this.nonJavaResources = resources;
		}
		return resources;
	}

	private void initializePackageNames(IPackageFragmentRoot root, HashSetOfArray fragmentsCache) {
		IJavaElement[] frags = null;
		try {
			if (!root.isOpen()) {
				PackageFragmentRootInfo info = root.isArchive() ? new JarPackageFragmentRootInfo() : new PackageFragmentRootInfo();
				((PackageFragmentRoot) root).computeChildren(info, ((JavaElement) root).resource());
				frags = info.children;
			} else
				frags = root.getChildren();
		} catch (JavaModelException e) {
			// root doesn't exist: ignore
			return;
		}
		for (int j = 0, length = frags.length; j < length; j++) {
			if (frags[j] instanceof PackageFragment) fragmentsCache.add(((PackageFragment) frags[j]).names);
		}
	}

	/*
	 * Returns whether the given path is a classpath entry or an output location.
	 */
	private boolean isClasspathEntryOrOutputLocation(IPath path, IPath location, IClasspathEntry[] resolvedClasspath, IPath projectOutput) {
		if (projectOutput.equals(path)) return true;
		for (int i = 0, length = resolvedClasspath.length; i < length; i++) {
			IClasspathEntry entry = resolvedClasspath[i];
			IPath entryPath;
			if ((entryPath = entry.getPath()).equals(path) || entryPath.equals(location)) {
				return true;
			}
			IPath output;
			if ((output = entry.getOutputLocation()) != null && output.equals(path)) {
				return true;
			}
		}
		return false;
	}

	/*
	 * Creates a new name lookup for this project info.
	 * The given project is assumed to be the handle of this info.
	 * This name lookup first looks in the given working copies.
	 */
	NameLookup newNameLookup(JavaProject project, ICompilationUnit[] workingCopies, boolean excludeTestCode) {
		ProjectCache cache = getProjectCache(project, excludeTestCode);
		HashtableOfArrayToObject allPkgFragmentsCache = cache.allPkgFragmentsCache;
		if (allPkgFragmentsCache == null) {
			Map<IPath, RootInfo> rootInfos = JavaModelManager.getJavaModelManager().deltaState.roots;
			IPackageFragmentRoot[] allRoots = cache.allPkgFragmentRootsCache;
			int length = allRoots.length;
			allPkgFragmentsCache = new HashtableOfArrayToObject();
			for (int i = 0; i < length; i++) {
				IPackageFragmentRoot root = allRoots[i];
				DeltaProcessor.RootInfo rootInfo = rootInfos.get(root.getPath());
				JavaProject rootProject = rootInfo == null ? project : rootInfo.project;
				HashSetOfArray fragmentsCache;
				if (rootProject.equals(project)) {
					// retrieve package fragments cache from this project
					fragmentsCache = cache.pkgFragmentsCaches.get(root);
				} else {
					// retrieve package fragments  cache from the root's project
					ProjectCache rootProjectCache;
					try {
						rootProjectCache = rootProject.getProjectCache(excludeTestCode);
					} catch (JavaModelException e) {
						// project doesn't exit
						continue;
					}
					fragmentsCache = rootProjectCache.pkgFragmentsCaches.get(root);
				}
				if (fragmentsCache == null) { // see https://bugs.eclipse.org/bugs/show_bug.cgi?id=183833
					fragmentsCache = new HashSetOfArray();
					initializePackageNames(root, fragmentsCache);
				}
				Object[][] set = fragmentsCache.set;
				for (int j = 0, length2 = set.length; j < length2; j++) {
					String[] pkgName = (String[]) set[j];
					if (pkgName == null)
						continue;
					Object existing = allPkgFragmentsCache.get(pkgName);
					if (existing == null || existing == NO_ROOTS) {
						allPkgFragmentsCache.put(pkgName, root);
						// ensure super packages (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=119161)
						// are also in the map
						addSuperPackageNames(pkgName, allPkgFragmentsCache);
					} else {
						if (existing instanceof PackageFragmentRoot) {
							allPkgFragmentsCache.put(pkgName, new IPackageFragmentRoot[] {(PackageFragmentRoot) existing, root});
						} else {
							IPackageFragmentRoot[] roots = (IPackageFragmentRoot[]) existing;
							int rootLength = roots.length;
							System.arraycopy(roots, 0, roots = new IPackageFragmentRoot[rootLength+1], 0, rootLength);
							roots[rootLength] = root;
							allPkgFragmentsCache.put(pkgName, roots);
						}
					}
				}
			}
			cache.allPkgFragmentsCache = allPkgFragmentsCache;
		}
		return new NameLookup(project, cache.allPkgFragmentRootsCache, cache.allPkgFragmentsCache, workingCopies, cache.rootToResolvedEntries);
	}

	/*
	 * Reset the package fragment roots and package fragment caches
	 */
	void resetCaches() {
		this.projectCache = null;
		this.mainProjectCache = null;
	}
}
