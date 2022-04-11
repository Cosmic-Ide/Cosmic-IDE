/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
package org.eclipse.jdt.internal.compiler.batch;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.zip.ZipEntry;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.batch.FileSystem.Classpath;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFormatException;
import org.eclipse.jdt.internal.compiler.classfmt.ExternalAnnotationDecorator;
import org.eclipse.jdt.internal.compiler.classfmt.ExternalAnnotationProvider;
import org.eclipse.jdt.internal.compiler.env.AccessRuleSet;
import org.eclipse.jdt.internal.compiler.env.IModule;
import org.eclipse.jdt.internal.compiler.env.NameEnvironmentAnswer;
import org.eclipse.jdt.internal.compiler.env.IBinaryType;
import org.eclipse.jdt.internal.compiler.lookup.BinaryTypeBinding.ExternalAnnotationStatus;
import org.eclipse.jdt.internal.compiler.util.SuffixConstants;
import org.eclipse.jdt.internal.compiler.util.Util;

public class ClasspathJmod extends ClasspathJar {

	public static char[] CLASSES = "classes".toCharArray(); //$NON-NLS-1$
	public static char[] CLASSES_FOLDER = "classes/".toCharArray(); //$NON-NLS-1$

public ClasspathJmod(File file, boolean closeZipFileAtEnd,
		AccessRuleSet accessRuleSet, String destinationPath) {
	super(file, closeZipFileAtEnd, accessRuleSet, destinationPath);
}

@Override
public List<Classpath> fetchLinkedJars(FileSystem.ClasspathSectionProblemReporter problemReporter) {
	// don't do anything
	return null;
}
@Override
public NameEnvironmentAnswer findClass(char[] typeName, String qualifiedPackageName, String moduleName, String qualifiedBinaryFileName, boolean asBinaryOnly) {
	if (!isPackage(qualifiedPackageName, moduleName))
		return null; // most common case

	try {
		qualifiedBinaryFileName = new String(CharOperation.append(CLASSES_FOLDER, qualifiedBinaryFileName.toCharArray()));
		IBinaryType reader = ClassFileReader.read(this.zipFile, qualifiedBinaryFileName);
		if (reader != null) {
			char[] modName = this.module == null ? null : this.module.name();
			if (reader instanceof ClassFileReader) {
				ClassFileReader classReader = (ClassFileReader) reader;
				if (classReader.moduleName == null)
					classReader.moduleName = modName;
				else
					modName = classReader.moduleName;
			}
			searchPaths:
			if (this.annotationPaths != null) {
				String qualifiedClassName = qualifiedBinaryFileName.substring(0, qualifiedBinaryFileName.length()-SuffixConstants.EXTENSION_CLASS.length()-1);
				for (String annotationPath : this.annotationPaths) {
					try {
						if (this.annotationZipFile == null) {
							this.annotationZipFile = ExternalAnnotationDecorator.getAnnotationZipFile(annotationPath, null);
						}
						reader = ExternalAnnotationDecorator.create(reader, annotationPath, qualifiedClassName, this.annotationZipFile);

						if (reader.getExternalAnnotationStatus() == ExternalAnnotationStatus.TYPE_IS_ANNOTATED) {
							break searchPaths;
						}
					} catch (IOException e) {
						// don't let error on annotations fail class reading
					}
				}
				// location is configured for external annotations, but no .eea found, decorate in order to answer NO_EEA_FILE:
				reader = new ExternalAnnotationDecorator(reader, null);
			}
			return new NameEnvironmentAnswer(reader, fetchAccessRestriction(qualifiedBinaryFileName), modName);
		}
	} catch (ClassFormatException | IOException e) {
		// treat as if class file is missing
	}
	return null;
}
@Override
public boolean hasAnnotationFileFor(String qualifiedTypeName) {
	qualifiedTypeName = new String(CharOperation.append(CLASSES_FOLDER, qualifiedTypeName.toCharArray()));
	return this.zipFile.getEntry(qualifiedTypeName+ExternalAnnotationProvider.ANNOTATION_FILE_SUFFIX) != null;
}
@SuppressWarnings({ "rawtypes", "unchecked" })
@Override
public char[][][] findTypeNames(final String qualifiedPackageName, String moduleName) {
	if (!isPackage(qualifiedPackageName, moduleName))
		return null; // most common case
	final char[] packageArray = qualifiedPackageName.toCharArray();
	final ArrayList answers = new ArrayList();
	nextEntry : for (Enumeration e = this.zipFile.entries(); e.hasMoreElements(); ) {
		String fileName = ((ZipEntry) e.nextElement()).getName();

		// add the package name & all of its parent packages
		int first = CharOperation.indexOf(CLASSES_FOLDER, fileName.toCharArray(), false);
		int last = fileName.lastIndexOf('/');
		if (last > 0) {
			// extract the package name
			String packageName = fileName.substring(first + 1, last);
			if (!qualifiedPackageName.equals(packageName))
				continue nextEntry;
			int indexOfDot = fileName.lastIndexOf('.');
			if (indexOfDot != -1) {
				String typeName = fileName.substring(last + 1, indexOfDot);
				answers.add(
					CharOperation.arrayConcat(
						CharOperation.splitOn('/', packageArray),
						typeName.toCharArray()));
			}
		}
	}
	int size = answers.size();
	if (size != 0) {
		char[][][] result = new char[size][][];
		answers.toArray(result);
		return result;
	}
	return null;
}
@Override
public synchronized char[][] getModulesDeclaringPackage(String qualifiedPackageName, String moduleName) {
	if (this.packageCache != null)
		return singletonModuleNameIf(this.packageCache.contains(qualifiedPackageName));

	this.packageCache = new HashSet<>(41);
	this.packageCache.add(Util.EMPTY_STRING);

	for (Enumeration<? extends ZipEntry> e = this.zipFile.entries(); e.hasMoreElements(); ) {
		char[] entryName = e.nextElement().getName().toCharArray();
		int index = CharOperation.indexOf('/', entryName);
		if (index != -1) {
			char[] folder = CharOperation.subarray(entryName, 0, index);
			if (CharOperation.equals(CLASSES, folder)) {
				char[] fileName = CharOperation.subarray(entryName, index + 1, entryName.length);
				addToPackageCache(new String(fileName), false);
			}
		}
	}
	return singletonModuleNameIf(this.packageCache.contains(qualifiedPackageName));
}
@Override
public boolean hasCompilationUnit(String qualifiedPackageName, String moduleName) {
	qualifiedPackageName += '/';
	for (Enumeration<? extends ZipEntry> e = this.zipFile.entries(); e.hasMoreElements(); ) {
		char[] entryName = e.nextElement().getName().toCharArray();
		int index = CharOperation.indexOf('/', entryName);
		if (index != -1) {
			char[] folder = CharOperation.subarray(entryName, 0, index);
			if (CharOperation.equals(CLASSES, folder)) {
				String fileName = new String(CharOperation.subarray(entryName, index + 1, entryName.length));
				if (fileName.startsWith(qualifiedPackageName) && fileName.length() > qualifiedPackageName.length()) {
					String tail = fileName.substring(qualifiedPackageName.length());
					if (tail.indexOf('/') != -1)
						continue;
					if (tail.toLowerCase().endsWith(SUFFIX_STRING_class))
						return true;
				}
			}
		}
	}
	return false;
}
@Override
public String toString() {
	return "Classpath for JMod file " + this.file.getPath(); //$NON-NLS-1$
}
@Override
public IModule getModule() {
	return this.module;
}
}
