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
 *     Stephan Herrmann - Contributions for bug 215139 and bug 295894
 *******************************************************************************/
package org.eclipse.jdt.internal.core.search;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.internal.compiler.util.SuffixConstants;
import org.eclipse.jdt.internal.core.*;
import org.eclipse.jdt.internal.core.hierarchy.TypeHierarchy;

/**
 * Scope limited to the subtype and supertype hierarchy of a given type.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class HierarchyScope extends AbstractSearchScope implements SuffixConstants {

	public IType focusType;
	private String focusPath;
	private WorkingCopyOwner owner;

	private ITypeHierarchy hierarchy;
	private HashSet resourcePaths;
	private IPath[] enclosingProjectsAndJars;

	protected Set<String> elements;
	protected int elementCount;

	public boolean needsRefresh;

	private HashSet subTypes = null; // null means: don't filter for subTypes
	private IJavaProject javaProject = null; // null means: don't constrain the search to a project
	private boolean allowMemberAndEnclosingTypes = true;
	private boolean includeFocusType = true;

	/* (non-Javadoc)
	 * Adds the given resource to this search scope.
	 */
	public void add(IResource element) {
		this.elements.add(element.getFullPath().toString());
	}

	/**
	 * Creates a new hierarchy scope for the given type with the given configuration options.
	 * @param project      constrain the search result to this project,
	 *                     or <code>null</code> if search should consider all types in the workspace
	 * @param type         the focus type of the hierarchy
	 * @param owner 	   the owner of working copies that take precedence over original compilation units,
	 *                     or <code>null</code> if the primary working copy owner should be used
	 * @param onlySubtypes if true search only subtypes of 'type'
	 * @param noMembersOrEnclosingTypes if true the hierarchy is strict,
	 * 					   i.e., no additional member types or enclosing types of types spanning the hierarchy are included,
	 * 					   otherwise all member and enclosing types of types in the hierarchy are included.
	 * @param includeFocusType if true the focus type <code>type</code> is included in the resulting scope, otherwise it is excluded
	 */
	public HierarchyScope(IJavaProject project, IType type, WorkingCopyOwner owner, boolean onlySubtypes, boolean noMembersOrEnclosingTypes, boolean includeFocusType) throws JavaModelException {
		this(type, owner);
		this.javaProject = project;
		if (onlySubtypes) {
			this.subTypes = new HashSet();
		}
		this.includeFocusType = includeFocusType;
		this.allowMemberAndEnclosingTypes = !noMembersOrEnclosingTypes;
	}

	/* (non-Javadoc)
	 * Creates a new hiearchy scope for the given type.
	 */
	public HierarchyScope(IType type, WorkingCopyOwner owner) throws JavaModelException {
		this.focusType = type;
		this.owner = owner;

		this.enclosingProjectsAndJars = computeProjectsAndJars(type);

		// resource path
		IPackageFragmentRoot root = (IPackageFragmentRoot)type.getPackageFragment().getParent();
		if (root.isArchive()) {
			IPath jarPath = root.getPath();
			Object target = JavaModel.getTarget(jarPath, true);
			String zipFileName;
			if (target instanceof IFile) {
				// internal jar
				zipFileName = jarPath.toString();
			} else if (target instanceof File) {
				// external jar
				zipFileName = ((File)target).getPath();
			} else {
				return; // unknown target
			}
			IModuleDescription md = root.getModuleDescription();
			if(md != null) {
				String module = md.getElementName();
				this.focusPath =
						zipFileName
						+ JAR_FILE_ENTRY_SEPARATOR
						+ module
						+ JAR_FILE_ENTRY_SEPARATOR
						+ type.getFullyQualifiedName().replace('.', '/')
						+ SUFFIX_STRING_class;
			} else {
				this.focusPath =
					zipFileName
						+ JAR_FILE_ENTRY_SEPARATOR
						+ type.getFullyQualifiedName().replace('.', '/')
						+ SUFFIX_STRING_class;
			}
		} else {
			this.focusPath = type.getPath().toString();
		}

		this.needsRefresh = true;

		//disabled for now as this could be expensive
		//JavaModelManager.getJavaModelManager().rememberScope(this);
	}
	private void buildResourceVector() {
		HashMap paths = new HashMap();
		IType[] types = null;
		if (this.subTypes != null) {
			types = this.hierarchy.getAllSubtypes(this.focusType);
			if (this.includeFocusType) {
				int len = types.length;
				System.arraycopy(types, 0, types=new IType[len+1], 0, len);
				types[len] = this.focusType;
			}
		} else {
			types = this.hierarchy.getAllTypes();
		}
		for (int i = 0; i < types.length; i++) {
			IType type = types[i];
			if (this.subTypes != null) {
				// remember subtypes for later use in encloses()
				this.subTypes.add(type);
			}
			IResource resource = ((JavaElement)type).resource();
			if (resource != null) {
				add(resource);
			}
			IPackageFragmentRoot root =
				(IPackageFragmentRoot) type.getPackageFragment().getParent();
			if (root instanceof JarPackageFragmentRoot) {
				// type in a jar
				JarPackageFragmentRoot jar = (JarPackageFragmentRoot) root;
				IPath jarPath = jar.getPath();
				Object target = JavaModel.getTarget(jarPath, true);
				String zipFileName;
				if (target instanceof IFile) {
					// internal jar
					zipFileName = jarPath.toString();
				} else if (target instanceof File) {
					// external jar
					zipFileName = ((File)target).getPath();
				} else {
					continue; // unknown target
				}
				String resourcePath;
				IModuleDescription md = root.getModuleDescription();
				if(md != null) {
					String module = md.getElementName();
					resourcePath =
							zipFileName
							+ JAR_FILE_ENTRY_SEPARATOR
							+ module
							+ JAR_FILE_ENTRY_SEPARATOR
							+ type.getFullyQualifiedName().replace('.', '/')
							+ SUFFIX_STRING_class;
				} else {
					resourcePath =
						zipFileName
						+ JAR_FILE_ENTRY_SEPARATOR
						+ type.getFullyQualifiedName().replace('.', '/')
						+ SUFFIX_STRING_class;
				}
				this.resourcePaths.add(resourcePath);
				paths.put(jarPath, type);
			} else {
				// type is a project
				paths.put(type.getJavaProject().getProject().getFullPath(), type);
			}
		}
		this.enclosingProjectsAndJars = new IPath[paths.size()];
		int i = 0;
		for (Iterator iter = paths.keySet().iterator(); iter.hasNext();) {
			this.enclosingProjectsAndJars[i++] = (IPath) iter.next();
		}
	}
	/*
	 * Computes the paths of projects and jars that the hierarchy on the given type could contain.
	 * This is a super set of the project and jar paths once the hierarchy is computed.
	 */
	private IPath[] computeProjectsAndJars(IType type) throws JavaModelException {
		HashSet set = new HashSet();
		IPackageFragmentRoot root = (IPackageFragmentRoot)type.getPackageFragment().getParent();
		if (root.isArchive()) {
			// add the root
			set.add(root.getPath());
			// add all projects that reference this archive and their dependents
			IPath rootPath = root.getPath();
			IJavaModel model = JavaModelManager.getJavaModelManager().getJavaModel();
			IJavaProject[] projects = model.getJavaProjects();
			HashSet visited = new HashSet();
			for (int i = 0; i < projects.length; i++) {
				JavaProject project = (JavaProject) projects[i];
				IClasspathEntry entry = project.getClasspathEntryFor(rootPath);
				if (entry != null) {
					// add the project and its binary pkg fragment roots
					IPackageFragmentRoot[] roots = project.getAllPackageFragmentRoots();
					set.add(project.getPath());
					for (int k = 0; k < roots.length; k++) {
						IPackageFragmentRoot pkgFragmentRoot = roots[k];
						if (pkgFragmentRoot.getKind() == IPackageFragmentRoot.K_BINARY) {
							set.add(pkgFragmentRoot.getPath());
						}
					}
					// add the dependent projects
					computeDependents(project, set, visited);
				}
			}
		} else {
			// add all the project's pkg fragment roots
			IJavaProject project = (IJavaProject)root.getParent();
			IPackageFragmentRoot[] roots = project.getAllPackageFragmentRoots();
			for (int i = 0; i < roots.length; i++) {
				IPackageFragmentRoot pkgFragmentRoot = roots[i];
				if (pkgFragmentRoot.getKind() == IPackageFragmentRoot.K_BINARY) {
					set.add(pkgFragmentRoot.getPath());
				} else {
					set.add(pkgFragmentRoot.getParent().getPath());
				}
			}
			// add the dependent projects
			computeDependents(project, set, new HashSet());
		}
		IPath[] result = new IPath[set.size()];
		set.toArray(result);
		return result;
	}
	private void computeDependents(IJavaProject project, HashSet set, HashSet visited) {
		if (visited.contains(project)) return;
		visited.add(project);
		IProject[] dependents = project.getProject().getReferencingProjects();
		for (int i = 0; i < dependents.length; i++) {
			try {
				IJavaProject dependent = JavaCore.create(dependents[i]);
				IPackageFragmentRoot[] roots = dependent.getPackageFragmentRoots();
				set.add(dependent.getPath());
				for (int j = 0; j < roots.length; j++) {
					IPackageFragmentRoot pkgFragmentRoot = roots[j];
					if (pkgFragmentRoot.isArchive()) {
						set.add(pkgFragmentRoot.getPath());
					}
				}
				computeDependents(dependent, set, visited);
			} catch (JavaModelException e) {
				// project is not a java project
			}
		}
	}

	@Override
	public boolean encloses(String resourcePath) {
		return encloses(resourcePath, null);
	}
	public boolean encloses(String resourcePath, IProgressMonitor progressMonitor) {
		if (this.hierarchy == null) {
			if (resourcePath.equals(this.focusPath)) {
				return true;
			} else {
				if (this.needsRefresh) {
					try {
						initialize(progressMonitor);
					} catch (JavaModelException e) {
						return false;
					}
				} else {
					// the scope is used only to find enclosing projects and jars
					// clients is responsible for filtering out elements not in the hierarchy (see SearchEngine)
					return true;
				}
			}
		}
		if (this.needsRefresh) {
			try {
				refresh(progressMonitor);
			} catch(JavaModelException e) {
				return false;
			}
		}
		int separatorIndex = resourcePath.indexOf(JAR_FILE_ENTRY_SEPARATOR);
		if (separatorIndex != -1) {
			return this.resourcePaths.contains(resourcePath);
		} else {
			return this.elements.contains(resourcePath);
		}
	}
	/**
	 * Optionally perform additional checks after element has already passed matching based on index/documents.
	 *
	 * @param element the given element
	 * @return <code>true</code> if the element is enclosed or if no fine grained checking
	 *         (regarding subtypes and members) is requested
	 */
	public boolean enclosesFineGrained(IJavaElement element) {
		if ((this.subTypes == null) && this.allowMemberAndEnclosingTypes)
			return true; // no fine grained checking requested
		return encloses(element, null);
	}

	@Override
	public boolean encloses(IJavaElement element) {
		return encloses(element, null);
	}
	public boolean encloses(IJavaElement element, IProgressMonitor progressMonitor) {
		if (this.hierarchy == null) {
			if (this.includeFocusType && this.focusType.equals(element.getAncestor(IJavaElement.TYPE))) {
				return true;
			} else {
				if (this.needsRefresh) {
					try {
						initialize(progressMonitor);
					} catch (JavaModelException e) {
						return false;
					}
				} else {
					// the scope is used only to find enclosing projects and jars
					// clients is responsible for filtering out elements not in the hierarchy (see SearchEngine)
					return true;
				}
			}
		}
		if (this.needsRefresh) {
			try {
				refresh(progressMonitor);
			} catch(JavaModelException e) {
				return false;
			}
		}
		IType type = null;
		if (element instanceof IType) {
			type = (IType) element;
		} else if (element instanceof IMember) {
			type = ((IMember) element).getDeclaringType();
		}
		if (type != null) {
			if (this.focusType.equals(type))
				return this.includeFocusType;
			// potentially allow travelling in:
			if (enclosesType(type, this.allowMemberAndEnclosingTypes)) {
				return true;
			}
			if (this.allowMemberAndEnclosingTypes) {
				// travel out: queried type is enclosed in this scope if its (indirect) declaring type is:
				IType enclosing = type.getDeclaringType();
				while (enclosing != null) {
					// don't allow travelling in again:
					if (enclosesType(enclosing, false)) {
						return true;
					}
					enclosing = enclosing.getDeclaringType();
				}
			}
		}
		return false;
	}
	private boolean enclosesType(IType type, boolean recurse) {
		if (this.subTypes != null) {
			// searching subtypes
			if (this.subTypes.contains(type)) {
				return true;
			}
			// be flexible: look at original element (see bug 14106 and below)
			IType original = type.isBinary() ? null : (IType)type.getPrimaryElement();
			if (original != type && this.subTypes.contains(original)) {
				return true;
			}
		} else {
			if (this.hierarchy.contains(type)) {
				return true;
			} else {
				// be flexible: look at original element (see bug 14106 Declarations in Hierarchy does not find declarations in hierarchy)
				IType original;
				if (!type.isBinary()
						&& (original = (IType)type.getPrimaryElement()) != null) {
					if (this.hierarchy.contains(original)) {
						return true;
					}
				}
			}
		}
		if (recurse) {
			// queried type is enclosed in this scope if one of its members is:
			try {
				IType[] memberTypes = type.getTypes();
				for (int i = 0; i < memberTypes.length; i++) {
					if (enclosesType(memberTypes[i], recurse)) {
						return true;
					}
				}
			} catch (JavaModelException e) {
				return false;
			}
		}
		return false;
	}
	/* (non-Javadoc)
	 * @see IJavaSearchScope#enclosingProjectsAndJars()
	 * @deprecated
	 */
	@Override
	public IPath[] enclosingProjectsAndJars() {
		if (this.needsRefresh) {
			try {
				refresh(null);
			} catch(JavaModelException e) {
				return new IPath[0];
			}
		}
		return this.enclosingProjectsAndJars;
	}
	protected void initialize() throws JavaModelException {
		initialize(null);
	}
	protected void initialize(IProgressMonitor progressMonitor) throws JavaModelException {
		this.resourcePaths = new HashSet();
		this.elements = new HashSet<>();
		this.elementCount = 0;
		this.needsRefresh = false;
		if (this.hierarchy == null) {
			if (this.javaProject != null) {
				this.hierarchy = this.focusType.newTypeHierarchy(this.javaProject, this.owner, progressMonitor);
			} else {
				this.hierarchy = this.focusType.newTypeHierarchy(this.owner, progressMonitor);
			}
		} else {
			this.hierarchy.refresh(progressMonitor);
		}
		buildResourceVector();
	}

	@Override
	public void processDelta(IJavaElementDelta delta, int eventType) {
		if (this.needsRefresh) return;
		this.needsRefresh = this.hierarchy == null ? false : ((TypeHierarchy)this.hierarchy).isAffected(delta, eventType);
	}
	protected void refresh() throws JavaModelException {
		refresh(null);
	}
	protected void refresh(IProgressMonitor progressMonitor) throws JavaModelException {
		if (this.hierarchy != null) {
			initialize(progressMonitor);
		}
	}
	@Override
	public String toString() {
		return "HierarchyScope on " + ((JavaElement)this.focusType).toStringWithAncestors(); //$NON-NLS-1$
	}

	@Override
	public boolean isParallelSearchSupported() {
		return true;
	}

	@Override
	public void initBeforeSearch(IProgressMonitor monitor) throws JavaModelException {
		if (this.needsRefresh) {
			initialize(monitor);
		}
	}
}
