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
package org.eclipse.jdt.internal.core.builder;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.jdt.internal.compiler.env.IModule;
import org.eclipse.jdt.internal.compiler.problem.AbortCompilation;
import org.eclipse.jdt.internal.core.util.Util;

public class SourceFile implements ICompilationUnit {

public IFile resource;
ClasspathMultiDirectory sourceLocation;
String initialTypeName;
boolean updateClassFile;

public SourceFile(IFile resource, ClasspathMultiDirectory sourceLocation) {
	this.resource = resource;
	this.sourceLocation = sourceLocation;
	this.initialTypeName = extractTypeName();
	this.updateClassFile = false;
}

public SourceFile(IFile resource, ClasspathMultiDirectory sourceLocation, boolean updateClassFile) {
	this(resource, sourceLocation);

	this.updateClassFile = updateClassFile;
}

@Override
public boolean equals(Object o) {
	if (this == o) return true;
	if (!(o instanceof SourceFile)) return false;

	SourceFile f = (SourceFile) o;
	return this.sourceLocation == f.sourceLocation && this.resource.getFullPath().equals(f.resource.getFullPath());
}

String extractTypeName() {
	// answer a String with the qualified type name for the source file in the form: 'p1/p2/A'
	IPath fullPath = this.resource.getFullPath();
	int resourceSegmentCount = fullPath.segmentCount();
	int sourceFolderSegmentCount = this.sourceLocation.sourceFolder.getFullPath().segmentCount();
	int charCount = (resourceSegmentCount - sourceFolderSegmentCount - 1);
	resourceSegmentCount--; // deal with the last segment separately
	for (int i = sourceFolderSegmentCount; i < resourceSegmentCount; i++)
		charCount += fullPath.segment(i).length();
	String lastSegment = fullPath.segment(resourceSegmentCount);
	int extensionIndex = Util.indexOfJavaLikeExtension(lastSegment);
	charCount += extensionIndex;

	char[] result = new char[charCount];
	int offset = 0;
	for (int i = sourceFolderSegmentCount; i < resourceSegmentCount; i++) {
		String segment = fullPath.segment(i);
		int size = segment.length();
		segment.getChars(0, size, result, offset);
		offset += size;
		result[offset++] = '/';
	}
	lastSegment.getChars(0, extensionIndex, result, offset);
	return new String(result);
}

@Override
public char[] getContents() {

	try {
		return Util.getResourceContentsAsCharArray(this.resource);
	} catch (CoreException e) {
		throw new AbortCompilation(true, new MissingSourceFileException(this.resource.getFullPath().toString()));
	}
}

/**
 * @see org.eclipse.jdt.internal.compiler.env.IDependent#getFileName()
 */
@Override
public char[] getFileName() {
	return this.resource.getFullPath().toString().toCharArray(); // do not know what you want to return here
}

@Override
public char[] getMainTypeName() {
	char[] typeName = this.initialTypeName.toCharArray();
	int lastIndex = CharOperation.lastIndexOf('/', typeName);
	return CharOperation.subarray(typeName, lastIndex + 1, -1);
}

@Override
public char[][] getPackageName() {
	char[] typeName = this.initialTypeName.toCharArray();
	int lastIndex = CharOperation.lastIndexOf('/', typeName);
	return CharOperation.splitOn('/', typeName, 0, lastIndex);
}
@Override
public int hashCode() {
	return this.initialTypeName.hashCode();
}
@Override
public boolean ignoreOptionalProblems() {
	return this.sourceLocation.ignoreOptionalProblems;
}
@Override
public String getExternalAnnotationPath(String qualifiedTypeName) {
	return this.sourceLocation.externalAnnotationPath;
}
String typeLocator() {
	return this.resource.getProjectRelativePath().toString();
}

@Override
public String toString() {
	return "SourceFile[" //$NON-NLS-1$
		+ this.resource.getFullPath() + "]";  //$NON-NLS-1$
}

@Override
public char[] getModuleName() {
	IModule mod = this.sourceLocation.module();
	return mod == null ? null : mod.name();
}
}
