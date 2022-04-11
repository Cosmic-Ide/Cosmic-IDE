/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation.
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
import java.util.Enumeration;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFormatException;
import org.eclipse.jdt.internal.compiler.env.AccessRuleSet;
import org.eclipse.jdt.internal.compiler.env.IBinaryType;
import org.eclipse.jdt.internal.compiler.env.IModule;
import org.eclipse.jdt.internal.compiler.env.NameEnvironmentAnswer;
import org.eclipse.jdt.internal.compiler.util.SimpleSet;
import org.eclipse.jdt.internal.compiler.util.SuffixConstants;

public class ClasspathJMod extends ClasspathJar {

	public static char[] CLASSES = "classes".toCharArray(); //$NON-NLS-1$
	public static char[] CLASSES_FOLDER = "classes/".toCharArray(); //$NON-NLS-1$
	private static int MODULE_DESCRIPTOR_NAME_LENGTH = IModule.MODULE_INFO_CLASS.length();

	ClasspathJMod(String zipFilename, long lastModified, AccessRuleSet accessRuleSet, IPath externalAnnotationPath) {
		super(zipFilename, lastModified, accessRuleSet, externalAnnotationPath, true);
	}
	@Override
	IModule initializeModule() {
		IModule mod = null;
		ZipFile file = null;
		try {
			file = new ZipFile(this.zipFilename);
			String fileName = new String(CLASSES_FOLDER) + IModule.MODULE_INFO_CLASS;
			ClassFileReader classfile = ClassFileReader.read(file, fileName);
			if (classfile != null) {
				mod = classfile.getModuleDeclaration();
			}
		} catch (ClassFormatException | IOException e) {
			// do nothing
		} finally {
			try {
				if (file != null)
					file.close();
			} catch (IOException e) {
				// do nothing
			}
		}
		return mod;
	}

	@Override
	public NameEnvironmentAnswer findClass(String binaryFileName, String qualifiedPackageName, String moduleName, String qualifiedBinaryFileName,
											boolean asBinaryOnly, Predicate<String> moduleNameFilter) {
		if (!isPackage(qualifiedPackageName, moduleName)) return null; // most common case
		if (moduleNameFilter != null && this.module != null && !moduleNameFilter.test(String.valueOf(this.module.name())))
			return null;

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
				String fileNameWithoutExtension = qualifiedBinaryFileName.substring(0, qualifiedBinaryFileName.length() - SuffixConstants.SUFFIX_CLASS.length);
				return createAnswer(fileNameWithoutExtension, reader, modName);
			}
		} catch (IOException | ClassFormatException e) { // treat as if class file is missing
		}
		return null;
	}
	@Override
	protected String readJarContent(final SimpleSet packageSet) {
		String modInfo = null;
		for (Enumeration<? extends ZipEntry> e = this.zipFile.entries(); e.hasMoreElements(); ) {
			ZipEntry entry = e.nextElement();
			char[] entryName = entry.getName().toCharArray();
			int index = CharOperation.indexOf('/', entryName);
			if (index != -1) {
				char[] folder = CharOperation.subarray(entryName, 0, index);
				if (CharOperation.equals(CLASSES, folder)) {
					char[] fileName = CharOperation.subarray(entryName, index + 1, entryName.length);
					if (modInfo == null && fileName.length == MODULE_DESCRIPTOR_NAME_LENGTH) {
						if (CharOperation.equals(fileName, IModule.MODULE_INFO_CLASS.toCharArray())) {
							modInfo = new String(entryName);
						}
					}
					addToPackageSet(packageSet, new String(fileName), false);
				}
			}
		}
		return modInfo;
	}
}
