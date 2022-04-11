/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
package org.eclipse.jdt.internal.core.search.matching;

import java.util.Iterator;
import java.util.Map;
import java.util.function.Predicate;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.compiler.env.NameEnvironmentAnswer;
import org.eclipse.jdt.internal.compiler.env.IModulePathEntry;
import org.eclipse.jdt.internal.compiler.util.SimpleLookupTable;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.jdt.internal.core.builder.ClasspathLocation;
import org.eclipse.jdt.internal.core.util.ResourceCompilationUnit;
import org.eclipse.jdt.internal.core.util.Util;

public class ClasspathSourceDirectory extends ClasspathLocation implements IModulePathEntry {

	IContainer sourceFolder;
	SimpleLookupTable directoryCache;
	SimpleLookupTable missingPackageHolder = new SimpleLookupTable();
	char[][] fullExclusionPatternChars;
	char[][] fulInclusionPatternChars;

ClasspathSourceDirectory(IContainer sourceFolder, char[][] fullExclusionPatternChars, char[][] fulInclusionPatternChars) {
	this.sourceFolder = sourceFolder;
	this.directoryCache = new SimpleLookupTable(5);
	this.fullExclusionPatternChars = fullExclusionPatternChars;
	this.fulInclusionPatternChars = fulInclusionPatternChars;
}

@Override
public void cleanup() {
	this.directoryCache = null;
}

SimpleLookupTable directoryTable(String qualifiedPackageName) {
	SimpleLookupTable dirTable = (SimpleLookupTable) this.directoryCache.get(qualifiedPackageName);
	if (dirTable == this.missingPackageHolder) return null; // package exists in another classpath directory or jar
	if (dirTable != null) return dirTable;

	try {
		IResource container = this.sourceFolder.findMember(qualifiedPackageName); // this is a case-sensitive check
		if (container instanceof IContainer) {
			IResource[] members = ((IContainer) container).members();
			dirTable = new SimpleLookupTable();
			for (int i = 0, l = members.length; i < l; i++) {
				IResource m = members[i];
				String name;
				if (m.getType() == IResource.FILE) {
					int index = Util.indexOfJavaLikeExtension(name = m.getName());
					if (index >= 0) {
						String fullPath = m.getFullPath().toString();
						if (!org.eclipse.jdt.internal.compiler.util.Util.isExcluded(fullPath.toCharArray(), this.fulInclusionPatternChars, this.fullExclusionPatternChars, false/*not a folder path*/)) {
							dirTable.put(name.substring(0, index), m);
						}
					}
				}
			}
			// look for secondary types, see https://bugs.eclipse.org/bugs/show_bug.cgi?id=382778
			IJavaProject project = JavaCore.create(container.getProject());
			Map<String, Map<String, IType>> secondaryTypePaths = JavaModelManager.getJavaModelManager().secondaryTypes(project, false, null);
			if (secondaryTypePaths.size() > 0) {
				Map<String, IType> typesInPackage = secondaryTypePaths.get(qualifiedPackageName.replace('/', '.'));
				if (typesInPackage != null && typesInPackage.size() > 0) {
					for (Iterator<String> j = typesInPackage.keySet().iterator(); j.hasNext();) {
						String secondaryTypeName = j.next();
						IType secondaryType = typesInPackage.get(secondaryTypeName);
						IJavaElement parent = secondaryType.getParent();
						String fullPath = parent.getResource().getFullPath().toString();
						if (!org.eclipse.jdt.internal.compiler.util.Util.isExcluded(fullPath.toCharArray(), this.fulInclusionPatternChars, this.fullExclusionPatternChars, false/*not a folder path*/)) {
							dirTable.put(secondaryTypeName, parent.getResource());
						}
					}
				}
			}
			this.directoryCache.put(qualifiedPackageName, dirTable);
			return dirTable;
		}
	} catch(CoreException ignored) {
		// treat as if missing
	}
	this.directoryCache.put(qualifiedPackageName, this.missingPackageHolder);
	return null;
}

@Override
public boolean equals(Object o) {
	if (this == o) return true;
	if (!(o instanceof ClasspathSourceDirectory)) return false;

	return this.sourceFolder.equals(((ClasspathSourceDirectory) o).sourceFolder);
}

@Override
public NameEnvironmentAnswer findClass(String typeName, String qualifiedPackageName, String moduleName, String qualifiedBinaryFileName, boolean asBinaryOnly, Predicate<String> moduleNameFilter) {
	return findClass(typeName, qualifiedPackageName, moduleName, qualifiedBinaryFileName);
}
@Override
public NameEnvironmentAnswer findClass(String sourceFileWithoutExtension, String qualifiedPackageName, String moduleName, String qualifiedSourceFileWithoutExtension) {
	SimpleLookupTable dirTable = directoryTable(qualifiedPackageName);
	if (dirTable != null && dirTable.elementSize > 0) {
		IFile file = (IFile) dirTable.get(sourceFileWithoutExtension);
		if (file != null) {
			return new NameEnvironmentAnswer(new ResourceCompilationUnit(file,
					this.module == null ? null : this.module.name()), null /* no access restriction */);
		}
	}
	return null;
}

@Override
public IPath getProjectRelativePath() {
	return this.sourceFolder.getProjectRelativePath();
}

@Override
public int hashCode() {
	return this.sourceFolder == null ? super.hashCode() : this.sourceFolder.hashCode();
}

@Override
public boolean isPackage(String qualifiedPackageName, String moduleName) {
	if (moduleName != null) {
		if (this.module == null || !moduleName.equals(String.valueOf(this.module.name())))
			return false;
	}
	return directoryTable(qualifiedPackageName) != null;
}
@Override
public boolean hasCompilationUnit(String qualifiedPackageName, String moduleName) {
	SimpleLookupTable dirTable = directoryTable(qualifiedPackageName);
	if (dirTable != null && dirTable.elementSize > 0)
		return true;
	return false;
}

@Override
public void reset() {
	this.directoryCache = new SimpleLookupTable(5);
}

@Override
public String toString() {
	return "Source classpath directory " + this.sourceFolder.getFullPath().toString(); //$NON-NLS-1$
}

@Override
public String debugPathString() {
	return this.sourceFolder.getFullPath().toString();
}
}
