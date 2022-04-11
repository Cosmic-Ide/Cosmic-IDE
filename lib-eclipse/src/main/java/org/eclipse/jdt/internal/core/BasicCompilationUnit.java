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

import java.io.File;
import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IModularClassFile;
import org.eclipse.jdt.core.IModuleDescription;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.jdt.internal.compiler.util.Util;

/**
 * A basic implementation of <code>ICompilationUnit</code>
 * for use in the <code>SourceMapper</code>.
 * @see ICompilationUnit
 */
public class BasicCompilationUnit implements ICompilationUnit {
	protected char[] contents;

	// Note that if this compiler ICompilationUnit's content is known in advance, the fileName is not used to retrieve this content.
	// Instead it is used to keep enough information to recreate the IJavaElement corresponding to this compiler ICompilationUnit.
	// Thus the fileName can be a path to a .class file, or even a path in a .jar to a .class file.
	// (e.g. /P/lib/mylib.jar|org/eclipse/test/X.class)
	protected char[] fileName;

	protected char[][] packageName;
	protected char[] mainTypeName;
	protected char[] moduleName;
	protected String encoding;

private BasicCompilationUnit(char[] contents, char[][] packageName, String fileName) {
	this.contents = contents;
	this.fileName = fileName.toCharArray();
	this.packageName = packageName;
}

/**
 * @deprecated Should pass a javaElement via {@link BasicCompilationUnit#BasicCompilationUnit(char[], char[][], String, IJavaElement)}.
 */
@Deprecated
public BasicCompilationUnit(char[] contents, char[][] packageName, String fileName, String encoding) {
	this(contents, packageName, fileName);
	this.encoding = encoding;
}

public BasicCompilationUnit(char[] contents, char[][] packageName, String fileName, IJavaElement javaElement) {
	this(contents, packageName, fileName);
	initAttributes(javaElement);
}

/*
 * Initialize compilation unit encoding.
 * If we have a project, then get file name corresponding IFile and retrieve its encoding using
 * new API for encoding.
 * In case of a class file, then go through project in order to let the possibility to retrieve
 * a corresponding source file resource.
 * If we have a compilation unit, then get encoding from its resource directly...
 */
private void initAttributes(IJavaElement javaElement) {
	if (javaElement != null) {
		try {
				IModuleDescription module = null;

				search: while (javaElement != null) {
					switch (javaElement.getElementType()) {
						case IJavaElement.JAVA_PROJECT:
							module = ((IJavaProject) javaElement).getModuleDescription();
							break search;
						case IJavaElement.PACKAGE_FRAGMENT_ROOT:
							module = ((IPackageFragmentRoot) javaElement).getModuleDescription();
							break search;
						case IJavaElement.CLASS_FILE:
							if (javaElement instanceof IModularClassFile) {
								module = ((IModularClassFile) javaElement).getModule();
								break search;
							}
							break;
						case IJavaElement.COMPILATION_UNIT:
							IFile file = (IFile) javaElement.getResource();
							if (file != null) {
								this.encoding = file.getCharset();
							}
							module = ((org.eclipse.jdt.core.ICompilationUnit) javaElement).getModule();
							if (module != null)
								break search;
							break;
						default:
							break;
					}
					javaElement = javaElement.getParent();
				}

				if (module != null) {
					this.moduleName = module.getElementName().toCharArray();
				}
				if (this.encoding == null) {
					IProject project = javaElement.getJavaProject().getProject();
					if (project != null) {
						this.encoding = project.getDefaultCharset();
					}
				}
		} catch (CoreException e1) {
			this.encoding = null;
		}
	} else  {
		this.encoding = null;
	}
}

@Override
public char[] getContents() {
	if (this.contents != null)
		return this.contents;   // answer the cached source

	// otherwise retrieve it
	try {
		return Util.getFileCharContent(new File(new String(this.fileName)), this.encoding);
	} catch (IOException e) {
		// could not read file: returns an empty array
	}
	return CharOperation.NO_CHAR;
}
/**
 * @see org.eclipse.jdt.internal.compiler.env.IDependent#getFileName()
 */
@Override
public char[] getFileName() {
	return this.fileName;
}
@Override
public char[] getMainTypeName() {
	if (this.mainTypeName == null) {
		int start = CharOperation.lastIndexOf('/', this.fileName) + 1;
		if (start == 0 || start < CharOperation.lastIndexOf('\\', this.fileName))
			start = CharOperation.lastIndexOf('\\', this.fileName) + 1;
		int separator = CharOperation.lastIndexOf('|', this.fileName) + 1;
		if (separator > start) // case of a .class file in a default package in a jar
			start = separator;

		int end = CharOperation.lastIndexOf('$', this.fileName);
		if (end == -1 || !Util.isClassFileName(this.fileName)) {
			end = CharOperation.lastIndexOf('.', this.fileName);
			if (end == -1)
				end = this.fileName.length;
		}

		this.mainTypeName = CharOperation.subarray(this.fileName, start, end);
	}
	return this.mainTypeName;
}
@Override
public char[][] getPackageName() {
	return this.packageName;
}
@Override
public boolean ignoreOptionalProblems() {
	return false;
}
@Override
public String toString(){
	return "CompilationUnit: "+new String(this.fileName); //$NON-NLS-1$
}

@Override
public char[] getModuleName() {
	return this.moduleName;
}
}
