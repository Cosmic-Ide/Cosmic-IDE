/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
package org.eclipse.jdt.internal.core.builder;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.batch.BasicModule;
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.jdt.internal.compiler.env.IModule;
import org.eclipse.jdt.internal.compiler.parser.Parser;
import org.eclipse.jdt.internal.core.util.Util;

public class ClasspathMultiDirectory extends ClasspathDirectory {

IContainer sourceFolder;
char[][] inclusionPatterns; // used by builders when walking source folders
char[][] exclusionPatterns; // used by builders when walking source folders
boolean hasIndependentOutputFolder; // if output folder is not equal to any of the source folders
public boolean ignoreOptionalProblems;

ClasspathMultiDirectory(IContainer sourceFolder, IContainer binaryFolder, char[][] inclusionPatterns, char[][] exclusionPatterns,
		boolean ignoreOptionalProblems, IPath externalAnnotationPath) {
	super(binaryFolder, true, null, externalAnnotationPath, false /* source never an automatic module*/);

	this.sourceFolder = sourceFolder;
	this.inclusionPatterns = inclusionPatterns;
	this.exclusionPatterns = exclusionPatterns;
	this.hasIndependentOutputFolder = false;
	this.ignoreOptionalProblems = ignoreOptionalProblems;

	// handle the case when a state rebuilds a source folder
	if (this.inclusionPatterns != null && this.inclusionPatterns.length == 0)
		this.inclusionPatterns = null;
	if (this.exclusionPatterns != null && this.exclusionPatterns.length == 0)
		this.exclusionPatterns = null;
}

@Override
public boolean equals(Object o) {
	if (this == o) return true;
	if (!(o instanceof ClasspathMultiDirectory)) return false;

	ClasspathMultiDirectory md = (ClasspathMultiDirectory) o;
	// TODO: revisit this - is this really required??
//	if (this.module != md.module)
//		if (this.module == null || !this.module.equals(md.module))
//			return false;
	return this.ignoreOptionalProblems == md.ignoreOptionalProblems
		&& this.sourceFolder.equals(md.sourceFolder) && this.binaryFolder.equals(md.binaryFolder)
		&& CharOperation.equals(this.inclusionPatterns, md.inclusionPatterns)
		&& CharOperation.equals(this.exclusionPatterns, md.exclusionPatterns);
}

@Override
protected boolean isExcluded(IResource resource) {
	if (this.exclusionPatterns != null || this.inclusionPatterns != null)
		if (this.sourceFolder.equals(this.binaryFolder))
			return Util.isExcluded(resource, this.inclusionPatterns, this.exclusionPatterns);
	return false;
}
@Override
String[] directoryList(String qualifiedPackageName) {
	String[] dirList = (String[]) this.directoryCache.get(qualifiedPackageName);
	if (dirList != null) return dirList;

	try {
		IResource container = this.binaryFolder.findMember(qualifiedPackageName); // this is a case-sensitive check
		if (container instanceof IContainer) {
			IResource[] members = ((IContainer) container).members();
			dirList = new String[members.length];
			int index = 0;
			boolean foundClass = false;
			if (members.length > 0) {
				for (int i = 0, l = members.length; i < l; i++) {
					IResource m = members[i];
					String name = m.getName();
					boolean isClass = m.getType() == IResource.FILE && org.eclipse.jdt.internal.compiler.util.Util.isClassFileName(name);
					if (m.getType() == IResource.FOLDER || isClass) {
						// add exclusion pattern check here if we want to hide .class files
						dirList[index++] = name;
						foundClass |= isClass;
					}
				}
			}
			if(!foundClass) {
				container = this.sourceFolder.findMember(qualifiedPackageName);
				if (container instanceof IContainer) {
					members = ((IContainer) container).members();
					if (members.length > 0) {
						dirList = new String[members.length];
						index = 0;
						for (int i = 0, l = members.length; i < l; i++) {
							IResource m = members[i];
							String name = m.getName();
							if (m.getType() == IResource.FOLDER
									|| (m.getType() == IResource.FILE && org.eclipse.jdt.internal.compiler.util.Util.isJavaFileName(name))) {
								// FIXME: check if .java file has any declarations?
								dirList[index++] = name;
							}
						}
					}
				}
			}
			if (index < dirList.length)
				System.arraycopy(dirList, 0, dirList = new String[index], 0, index);
			this.directoryCache.put(qualifiedPackageName, dirList);
			return dirList;
		}
	} catch(CoreException ignored) {
		// ignore
	}
	return null;
}

@Override
public String toString() {
	return "Source classpath directory " + this.sourceFolder.getFullPath().toString() + //$NON-NLS-1$
		" with " + super.toString(); //$NON-NLS-1$
}

public void acceptModuleInfo(ICompilationUnit cu, Parser parser) {
	CompilationResult compilationResult = new CompilationResult(cu, 0, 1, 10);
	CompilationUnitDeclaration unit = parser.parse(cu, compilationResult);
	// Request could also come in when module-info has changed or removed.
	if (unit.isModuleInfo() && unit.moduleDeclaration != null) {
		this.module = new BasicModule(unit.moduleDeclaration, null);
	}
}
@Override
public void setModule(IModule mod) {
	this.module = mod;
}

public IModule module() {
	return this.module;
}

}
