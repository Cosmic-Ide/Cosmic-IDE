/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJarEntryResource;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModelStatusConstants;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.compiler.lookup.TypeConstants;
import org.eclipse.jdt.internal.core.util.Util;

/**
 * A package fragment that represents a package fragment found in a JAR.
 *
 * @see org.eclipse.jdt.core.IPackageFragment
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
class JarPackageFragment extends PackageFragment {
/**
 * Constructs a package fragment that is contained within a jar or a zip.
 */
protected JarPackageFragment(PackageFragmentRoot root, String[] names) {
	super(root, names);
}
/**
 * @see Openable
 */
@Override
protected boolean buildStructure(OpenableElementInfo info, IProgressMonitor pm, Map newElements, IResource underlyingResource) throws JavaModelException {
	JarPackageFragmentRoot root = (JarPackageFragmentRoot) getParent();
	JarPackageFragmentRootInfo parentInfo = (JarPackageFragmentRootInfo) root.getElementInfo();
	ArrayList[] entries = (ArrayList[]) parentInfo.rawPackageInfo.get(this.names);
	if (entries == null)
		throw newNotPresentException();
	JarPackageFragmentInfo fragInfo = (JarPackageFragmentInfo) info;

	// compute children
	fragInfo.setChildren(computeChildren(entries[0/*class files*/]));

	// compute non-Java resources
	fragInfo.setNonJavaResources(computeNonJavaResources(entries[1/*non Java resources*/]));

	newElements.put(this, fragInfo);
	return true;
}
/**
 * Compute the children of this package fragment. Children of jar package fragments
 * can only be IClassFile (representing .class files).
 */
private IJavaElement[] computeChildren(ArrayList namesWithoutExtension) {
	int size = namesWithoutExtension.size();
	if (size == 0)
		return NO_ELEMENTS;
	IJavaElement[] children = new IJavaElement[size];
	for (int i = 0; i < size; i++) {
		String nameWithoutExtension = (String) namesWithoutExtension.get(i);
		if (TypeConstants.MODULE_INFO_NAME_STRING.equals(nameWithoutExtension))
			children[i] = new ModularClassFile(this);
		else
			children[i] = new ClassFile(this, nameWithoutExtension);
	}
	return children;
}
/**
 * Compute all the non-java resources according to the given entry names.
 */
private Object[] computeNonJavaResources(ArrayList entryNames) {
	int length = entryNames.size();
	if (length == 0)
		return JavaElementInfo.NO_NON_JAVA_RESOURCES;
	HashMap jarEntries = new HashMap(); // map from IPath to IJarEntryResource
	HashMap childrenMap = new HashMap(); // map from IPath to ArrayList<IJarEntryResource>
	ArrayList topJarEntries = new ArrayList();
	for (int i = 0; i < length; i++) {
		String resName = (String) entryNames.get(i);
		// consider that a .java file is not a non-java resource (see bug 12246 Packages view shows .class and .java files when JAR has source)
		if (!Util.isJavaLikeFileName(resName)) {
			IPath filePath = new Path(resName);
			IPath childPath = filePath.removeFirstSegments(this.names.length);
			if (jarEntries.containsKey(childPath)) {
				// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=222665
				continue;
			}
			JarEntryFile file = new JarEntryFile(filePath.lastSegment());
			jarEntries.put(childPath, file);
			if (childPath.segmentCount() == 1) {
				file.setParent(this);
				topJarEntries.add(file);
			} else {
				IPath parentPath = childPath.removeLastSegments(1);
				while (parentPath.segmentCount() > 0) {
					ArrayList parentChildren = (ArrayList) childrenMap.get(parentPath);
					if (parentChildren == null) {
						Object dir = new JarEntryDirectory(parentPath.lastSegment());
						jarEntries.put(parentPath, dir);
						childrenMap.put(parentPath, parentChildren = new ArrayList());
						parentChildren.add(childPath);
						if (parentPath.segmentCount() == 1) {
							topJarEntries.add(dir);
							break;
						}
						childPath = parentPath;
						parentPath = childPath.removeLastSegments(1);
					} else {
						parentChildren.add(childPath);
						break; // all parents are already registered
					}
				}
			}
		}
	}
	Iterator entries = childrenMap.entrySet().iterator();
	while (entries.hasNext()) {
		Map.Entry entry = (Map.Entry) entries.next();
		IPath entryPath = (IPath) entry.getKey();
		ArrayList entryValue =  (ArrayList) entry.getValue();
		JarEntryDirectory jarEntryDirectory = (JarEntryDirectory) jarEntries.get(entryPath);
		int size = entryValue.size();
		IJarEntryResource[] children = new IJarEntryResource[size];
		for (int i = 0; i < size; i++) {
			JarEntryResource child = (JarEntryResource) jarEntries.get(entryValue.get(i));
			child.setParent(jarEntryDirectory);
			children[i] = child;
		}
		jarEntryDirectory.setChildren(children);
		if (entryPath.segmentCount() == 1) {
			jarEntryDirectory.setParent(this);
		}
	}
	return topJarEntries.toArray(new Object[topJarEntries.size()]);
}
/**
 * Returns true if this fragment contains at least one java resource.
 * Returns false otherwise.
 */
@Override
public boolean containsJavaResources() throws JavaModelException {
	return ((JarPackageFragmentInfo) getElementInfo()).containsJavaResources();
}
/**
 * @see org.eclipse.jdt.core.IPackageFragment
 */
@Override
public ICompilationUnit createCompilationUnit(String cuName, String contents, boolean force, IProgressMonitor monitor) throws JavaModelException {
	throw new JavaModelException(new JavaModelStatus(IJavaModelStatusConstants.READ_ONLY, this));
}
/**
 * @see JavaElement
 */
@Override
protected Object createElementInfo() {
	return new JarPackageFragmentInfo();
}
/**
 * @see org.eclipse.jdt.core.IPackageFragment
 */
@Override
public IClassFile[] getAllClassFiles() throws JavaModelException {
	ArrayList list = getChildrenOfType(CLASS_FILE);
	IClassFile[] array= new IClassFile[list.size()];
	list.toArray(array);
	return array;
}
/**
 * A jar package fragment never contains compilation units.
 * @see org.eclipse.jdt.core.IPackageFragment
 */
@Override
public ICompilationUnit[] getCompilationUnits() {
	return NO_COMPILATION_UNITS;
}
/**
 * A package fragment in a jar has no corresponding resource.
 *
 * @see IJavaElement
 */
@Override
public IResource getCorrespondingResource() {
	return null;
}
/**
 * Returns an array of non-java resources contained in the receiver.
 */
@Override
public Object[] getNonJavaResources() throws JavaModelException {
	if (isDefaultPackage()) {
		// We don't want to show non java resources of the default package (see PR #1G58NB8)
		return JavaElementInfo.NO_NON_JAVA_RESOURCES;
	} else {
		return storedNonJavaResources();
	}
}
@Override
protected boolean internalIsValidPackageName() {
	return true;
}
/**
 * Jars and jar entries are all read only
 */
@Override
public boolean isReadOnly() {
	return true;
}
protected Object[] storedNonJavaResources() throws JavaModelException {
	return ((JarPackageFragmentInfo) getElementInfo()).getNonJavaResources();
}
}
