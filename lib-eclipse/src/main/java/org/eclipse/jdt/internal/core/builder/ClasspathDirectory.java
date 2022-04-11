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

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFormatException;
import org.eclipse.jdt.internal.compiler.classfmt.ExternalAnnotationDecorator;
import org.eclipse.jdt.internal.compiler.classfmt.ExternalAnnotationProvider;
import org.eclipse.jdt.internal.compiler.env.AccessRuleSet;
import org.eclipse.jdt.internal.compiler.env.IBinaryType;
import org.eclipse.jdt.internal.compiler.env.IModule;
import org.eclipse.jdt.internal.compiler.env.NameEnvironmentAnswer;
import org.eclipse.jdt.internal.compiler.util.SimpleLookupTable;
import org.eclipse.jdt.internal.compiler.util.SuffixConstants;
import org.eclipse.jdt.internal.core.util.Util;


public class ClasspathDirectory extends ClasspathLocation {

final boolean isOnModulePath;
IContainer binaryFolder; // includes .class files for a single directory
boolean isOutputFolder;
SimpleLookupTable directoryCache;
String[] missingPackageHolder = new String[1];

ClasspathDirectory(IContainer binaryFolder, boolean isOutputFolder, AccessRuleSet accessRuleSet,
		IPath externalAnnotationPath, boolean isOnModulePath)
{
	this.binaryFolder = binaryFolder;
	this.isOutputFolder = isOutputFolder || binaryFolder.getProjectRelativePath().isEmpty(); // if binaryFolder == project, then treat it as an outputFolder
	this.directoryCache = new SimpleLookupTable(5);
	this.accessRuleSet = accessRuleSet;
	if (externalAnnotationPath != null)
		this.externalAnnotationPath = externalAnnotationPath.toOSString();
	this.isOnModulePath = isOnModulePath;
}

@Override
public void cleanup() {
	if (this.annotationZipFile != null) {
		try {
			this.annotationZipFile.close();
		} catch(IOException e) { // ignore it
		}
		this.annotationZipFile = null;
	}
	this.directoryCache = null;
}

IModule initializeModule() {
	IResource[] members = null;
	try {
		members = this.binaryFolder.members();
		if (members != null) {
			for (int i = 0, l = members.length; i < l; i++) {
				IResource m = members[i];
				String name = m.getName();
				// Note: Look only inside the default package.
				if (m.getType() == IResource.FILE && org.eclipse.jdt.internal.compiler.util.Util.isClassFileName(name)) {
					if (name.equalsIgnoreCase(IModule.MODULE_INFO_CLASS)) {
						try {
							ClassFileReader cfr = Util.newClassFileReader(m);
							return cfr.getModuleDeclaration();
						} catch (ClassFormatException | IOException e) {
							// TODO Java 9 Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			}
		}
	} catch (CoreException e1) {
		e1.printStackTrace();
	}
	return null;
}
/** Lists all java-like files and also sub-directories (for recursive tests). */
String[] directoryList(String qualifiedPackageName) {
	String[] dirList = (String[]) this.directoryCache.get(qualifiedPackageName);
	if (dirList == this.missingPackageHolder) return null; // package exists in another classpath directory or jar
	if (dirList != null) return dirList;

	try {
		IResource container = this.binaryFolder.findMember(qualifiedPackageName); // this is a case-sensitive check
		if (container instanceof IContainer) {
			IResource[] members = ((IContainer) container).members();
			dirList = new String[members.length];
			int index = 0;
			for (int i = 0, l = members.length; i < l; i++) {
				IResource m = members[i];
				String name = m.getName();
				if (m.getType() == IResource.FOLDER || // include folders so we recognize empty parent packages
						(m.getType() == IResource.FILE && org.eclipse.jdt.internal.compiler.util.Util.isClassFileName(name))) {
					// add exclusion pattern check here if we want to hide .class files
					dirList[index++] = name;
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
	this.directoryCache.put(qualifiedPackageName, this.missingPackageHolder);
	return null;
}
boolean doesFileExist(String fileName, String qualifiedPackageName, String qualifiedFullName) {
	String[] dirList = directoryList(qualifiedPackageName);
	if (dirList == null) return false; // most common case

	for (int i = dirList.length; --i >= 0;)
		if (fileName.equals(dirList[i]))
			return true;
	return false;
}

@Override
public boolean equals(Object o) {
	if (this == o) return true;
	if (!(o instanceof ClasspathDirectory)) return false;

	ClasspathDirectory dir = (ClasspathDirectory) o;
	if (this.accessRuleSet != dir.accessRuleSet)
		if (this.accessRuleSet == null || !this.accessRuleSet.equals(dir.accessRuleSet))
			return false;
	if (this.isOnModulePath != dir.isOnModulePath)
		return false;

	return this.binaryFolder.equals(dir.binaryFolder) && areAllModuleOptionsEqual(dir);
}
@Override
public NameEnvironmentAnswer findClass(String binaryFileName, String qualifiedPackageName, String moduleName, String qualifiedBinaryFileName, boolean asBinaryOnly, Predicate<String> moduleNameFilter) {
	if (!doesFileExist(binaryFileName, qualifiedPackageName, qualifiedBinaryFileName)) return null; // most common case

	IBinaryType reader = null;
	try {
		reader = Util.newClassFileReader(this.binaryFolder.getFile(new Path(qualifiedBinaryFileName)));
	} catch (CoreException | ClassFormatException | IOException e) {
		return null;
	}
	if (reader != null) {
		char[] modName = this.module == null ? null : this.module.name();
		if (reader instanceof ClassFileReader) {
			ClassFileReader cfReader = (ClassFileReader) reader;
			if (cfReader.moduleName == null)
				cfReader.moduleName = modName;
			else
				modName = cfReader.moduleName;
		}
		String fileNameWithoutExtension = qualifiedBinaryFileName.substring(0, qualifiedBinaryFileName.length() - SuffixConstants.SUFFIX_CLASS.length);
		return createAnswer(fileNameWithoutExtension, reader, modName);
	}
	return null;
}

@Override
public IPath getProjectRelativePath() {
	return this.binaryFolder.getProjectRelativePath();
}

@Override
public int hashCode() {
	return this.binaryFolder == null ? super.hashCode() : this.binaryFolder.hashCode();
}

protected boolean isExcluded(IResource resource) {
	return false;
}

@Override
public boolean isOutputFolder() {
	return this.isOutputFolder;
}

@Override
public boolean isPackage(String qualifiedPackageName, String moduleName) {
	if (moduleName != null) {
		if (this.module == null || !moduleName.equals(String.valueOf(this.module.name())))
			return false;
	}
	String[] list = directoryList(qualifiedPackageName);
	if (list != null) {
		// 1. search files here:
		for (String entry : list) {
			String entryLC = entry.toLowerCase();
			if (entryLC.endsWith(SuffixConstants.SUFFIX_STRING_class) || entryLC.endsWith(SuffixConstants.SUFFIX_STRING_java))
				return true;
		}
		// 2. recurse into sub directories
		for (String entry : list) {
			if (entry.indexOf('.') == -1) { // no plain files without '.' are returned by directoryList()
				if (isPackage(qualifiedPackageName+'/'+entry, null/*already checked*/))
					return true;
			}
		}
	}
	return false;
}
@Override
public boolean hasCompilationUnit(String qualifiedPackageName, String moduleName) {
	String[] dirList = directoryList(qualifiedPackageName);
	if (dirList != null) {
		for (String entry : dirList) {
			String entryLC = entry.toLowerCase();
			if (entryLC.endsWith(SuffixConstants.SUFFIX_STRING_class) || entryLC.endsWith(SuffixConstants.SUFFIX_STRING_java))
				return true;
		}
	}
	return false;
}

@Override
public void reset() {
	this.directoryCache = new SimpleLookupTable(5);
}

@Override
public String toString() {
	String start = "Binary classpath directory " + this.binaryFolder.getFullPath().toString(); //$NON-NLS-1$
	if (this.accessRuleSet == null)
		return start;
	return start + " with " + this.accessRuleSet; //$NON-NLS-1$
}

@Override
public String debugPathString() {
	return this.binaryFolder.getFullPath().toString();
}

@Override
public NameEnvironmentAnswer findClass(String typeName, String qualifiedPackageName, String moduleName, String qualifiedBinaryFileName) {
	//
	return findClass(typeName, qualifiedPackageName, moduleName, qualifiedBinaryFileName, false, null);
}

@Override
public char[][] listPackages() {
	Set<String> packageNames = new HashSet<>();
	IPath basePath = this.binaryFolder.getFullPath();
	try {
		this.binaryFolder.accept(r -> {
			String extension = r.getFileExtension();
			if (r instanceof IFile && extension != null && SuffixConstants.EXTENSION_class.equals(extension.toLowerCase())) {
				packageNames.add(r.getParent().getFullPath().makeRelativeTo(basePath).toString().replace('/', '.'));
			}
			return true;
		});
	} catch (CoreException e) {
		Util.log(e, "Failed to scan packages of "+this.binaryFolder); //$NON-NLS-1$
	}
	return packageNames.stream().map(String::toCharArray).toArray(char[][]::new);
}
@Override
protected IBinaryType decorateWithExternalAnnotations(IBinaryType reader, String fileNameWithoutExtension) {
	String qualifiedFileName = fileNameWithoutExtension + ExternalAnnotationProvider.ANNOTATION_FILE_SUFFIX;
	IFile file = this.binaryFolder.getFile(new Path(qualifiedFileName));
	if (file.exists()) {
		try {
			ExternalAnnotationProvider provider = new ExternalAnnotationProvider(file.getContents(), fileNameWithoutExtension);
			return new ExternalAnnotationDecorator(reader, provider);
		} catch (IOException|CoreException e) {
			// ignore
		}
	}
	return reader; // undecorated
}
}
