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
 *     Stephan Herrmann - Contribution for
 *								Bug 440687 - [compiler][batch][null] improve command line option for external annotations
 *     Lars Vogel <Lars.Vogel@vogella.com> - Contributions for
 *     						Bug 473178
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.batch;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.DefaultErrorHandlingPolicies;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFormatException;
import org.eclipse.jdt.internal.compiler.classfmt.ExternalAnnotationProvider;
import org.eclipse.jdt.internal.compiler.env.AccessRuleSet;
import org.eclipse.jdt.internal.compiler.env.IModule;
import org.eclipse.jdt.internal.compiler.env.NameEnvironmentAnswer;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.lookup.TypeConstants;
import org.eclipse.jdt.internal.compiler.parser.Parser;
import org.eclipse.jdt.internal.compiler.parser.ScannerHelper;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblemFactory;
import org.eclipse.jdt.internal.compiler.problem.ProblemReporter;
import org.eclipse.jdt.internal.compiler.util.Util;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

@SuppressWarnings({"rawtypes", "unchecked"})
public class ClasspathDirectory extends ClasspathLocation {

private Hashtable directoryCache;
private String[] missingPackageHolder = new String[1];
private int mode; // ability to only consider one kind of files (source vs. binaries), by default use both
private String encoding; // only useful if referenced in the source path
private Hashtable<String, Hashtable<String, String>> packageSecondaryTypes = null;
Map options;

ClasspathDirectory(File directory, String encoding, int mode,
		AccessRuleSet accessRuleSet, String destinationPath, Map options) {
	super(accessRuleSet, destinationPath);
	this.mode = mode;
	this.options = options;
	try {
		this.path = directory.getCanonicalPath();
	} catch (IOException e) {
		// should not happen as we know that the file exists
		this.path = directory.getAbsolutePath();
	}
	if (!this.path.endsWith(File.separator))
		this.path += File.separator;
	this.directoryCache = new Hashtable(11);
	this.encoding = encoding;
}
String[] directoryList(String qualifiedPackageName) {
	String[] dirList = (String[]) this.directoryCache.get(qualifiedPackageName);
	if (dirList == this.missingPackageHolder) return null; // package exists in another classpath directory or jar
	if (dirList != null) return dirList;

	File dir = new File(this.path + qualifiedPackageName);
	notFound : if (dir.isDirectory()) {
		// must protect against a case insensitive File call
		// walk the qualifiedPackageName backwards looking for an uppercase character before the '/'
		int index = qualifiedPackageName.length();
		int last = qualifiedPackageName.lastIndexOf(File.separatorChar);
		while (--index > last && !ScannerHelper.isUpperCase(qualifiedPackageName.charAt(index))){/*empty*/}
		if (index > last) {
			if (last == -1) {
				if (!doesFileExist(qualifiedPackageName, Util.EMPTY_STRING))
					break notFound;
			} else {
				String packageName = qualifiedPackageName.substring(last + 1);
				String parentPackage = qualifiedPackageName.substring(0, last);
				if (!doesFileExist(packageName, parentPackage))
					break notFound;
			}
		}
		if ((dirList = dir.list()) == null)
			dirList = CharOperation.NO_STRINGS;
		this.directoryCache.put(qualifiedPackageName, dirList);
		return dirList;
	}
	this.directoryCache.put(qualifiedPackageName, this.missingPackageHolder);
	return null;
}
boolean doesFileExist(String fileName, String qualifiedPackageName) {
	String[] dirList = directoryList(qualifiedPackageName);
	if (dirList == null) return false; // most common case

	for (int i = dirList.length; --i >= 0;)
		if (fileName.equals(dirList[i]))
			return true;
	return false;
}
@Override
public List fetchLinkedJars(FileSystem.ClasspathSectionProblemReporter problemReporter) {
	return null;
}
private NameEnvironmentAnswer findClassInternal(char[] typeName, String qualifiedPackageName, String qualifiedBinaryFileName, boolean asBinaryOnly) {
	if (!isPackage(qualifiedPackageName, null)) return null; // most common case TODO(SHMOD): use module name from this.module?
	String fileName = new String(typeName);
	boolean binaryExists = ((this.mode & BINARY) != 0) && doesFileExist(fileName + SUFFIX_STRING_class, qualifiedPackageName);
	boolean sourceExists = ((this.mode & SOURCE) != 0) && doesFileExist(fileName + SUFFIX_STRING_java, qualifiedPackageName);
	if (sourceExists && !asBinaryOnly) {
		String fullSourcePath = this.path + qualifiedBinaryFileName.substring(0, qualifiedBinaryFileName.length() - 6)  + SUFFIX_STRING_java;
		CompilationUnit unit = new CompilationUnit(null, fullSourcePath, this.encoding, this.destinationPath);
		unit.module = this.module == null ? null : this.module.name();
		if (!binaryExists)
			return new NameEnvironmentAnswer(unit,
					fetchAccessRestriction(qualifiedBinaryFileName));
		String fullBinaryPath = this.path + qualifiedBinaryFileName;
		long binaryModified = new File(fullBinaryPath).lastModified();
		long sourceModified = new File(fullSourcePath).lastModified();
		if (sourceModified > binaryModified)
			return new NameEnvironmentAnswer(unit,
					fetchAccessRestriction(qualifiedBinaryFileName));
	}
	if (binaryExists) {
		try {
			ClassFileReader reader = ClassFileReader.read(this.path + qualifiedBinaryFileName);
			// https://bugs.eclipse.org/bugs/show_bug.cgi?id=321115, package names are to be treated case sensitive.
			String typeSearched = qualifiedPackageName.length() > 0 ?
					qualifiedPackageName.replace(File.separatorChar, '/') + "/" + fileName //$NON-NLS-1$
					: fileName;
			if (!CharOperation.equals(reader.getName(), typeSearched.toCharArray())) {
				reader = null;
			}
			if (reader != null) {
				char[] modName = reader.moduleName != null ? reader.moduleName : this.module != null ? this.module.name() : null;
				return new NameEnvironmentAnswer(
						reader,
						fetchAccessRestriction(qualifiedBinaryFileName),
						modName);
			}
		} catch (IOException | ClassFormatException e) {
			// treat as if file is missing
		}
	}
	return null;
}
public NameEnvironmentAnswer findSecondaryInClass(char[] typeName, String qualifiedPackageName, String qualifiedBinaryFileName) {
	//"package-info" is a reserved class name and can never be a secondary type (it is much faster to stop the search here).
	if(CharOperation.equals(TypeConstants.PACKAGE_INFO_NAME, typeName)) {
		return null;
	}

	String typeNameString = new String(typeName);
	String moduleName = this.module != null ? String.valueOf(this.module.name()) : null; // TODO(SHMOD): test for ModuleBinding.ANY & UNNAMED
	boolean prereqs = this.options != null && isPackage(qualifiedPackageName, moduleName) && ((this.mode & SOURCE) != 0) && doesFileExist(typeNameString + SUFFIX_STRING_java, qualifiedPackageName);
	return prereqs ? null : findSourceSecondaryType(typeNameString, qualifiedPackageName, qualifiedBinaryFileName); /* only secondary types */
}

@Override
public boolean hasAnnotationFileFor(String qualifiedTypeName) {
	int pos = qualifiedTypeName.lastIndexOf('/');
	if (pos != -1 && (pos + 1 < qualifiedTypeName.length())) {
		String fileName = qualifiedTypeName.substring(pos + 1) + ExternalAnnotationProvider.ANNOTATION_FILE_SUFFIX;
		return doesFileExist(fileName, qualifiedTypeName.substring(0, pos));
	}
	return false;
}
@Override
public NameEnvironmentAnswer findClass(char[] typeName, String qualifiedPackageName, String moduleName, String qualifiedBinaryFileName) {
	return findClass(typeName, qualifiedPackageName, moduleName, qualifiedBinaryFileName, false);
}
@Override
public NameEnvironmentAnswer findClass(char[] typeName, String qualifiedPackageName, String moduleName, String qualifiedBinaryFileName, boolean asBinaryOnly) {
	if (File.separatorChar == '/')
      return findClassInternal(typeName, qualifiedPackageName, qualifiedBinaryFileName, asBinaryOnly);

	return findClassInternal(typeName, qualifiedPackageName.replace('/', File.separatorChar),
				qualifiedBinaryFileName.replace('/', File.separatorChar), asBinaryOnly);
}
/**
 *  Add all the secondary types in the package
 */
private Hashtable<String, String> getSecondaryTypes(String qualifiedPackageName) {
	Hashtable<String, String> packageEntry = new Hashtable<>();

	String[] dirList = (String[]) this.directoryCache.get(qualifiedPackageName);
	if (dirList == this.missingPackageHolder // package exists in another classpath directory or jar
			|| dirList == null)
		return packageEntry;

	File dir = new File(this.path + qualifiedPackageName);
	File[] listFiles = dir.isDirectory() ? dir.listFiles() : null;
	if (listFiles == null) return packageEntry;

	for (int i = 0, l = listFiles.length; i < l; ++i) {
		File f = listFiles[i];
		if (f.isDirectory()) continue;
		String s = f.getAbsolutePath();
		if (s == null) continue;
		if (!(s.endsWith(SUFFIX_STRING_java) || s.endsWith(SUFFIX_STRING_JAVA))) continue;
		CompilationUnit cu = new CompilationUnit(null, s, this.encoding, this.destinationPath);
		CompilationResult compilationResult = new CompilationResult(s.toCharArray(), 1, 1, 10);
		ProblemReporter problemReporter =
				new ProblemReporter(
					DefaultErrorHandlingPolicies.proceedWithAllProblems(),
					new CompilerOptions(this.options),
					new DefaultProblemFactory());
		Parser parser = new Parser(problemReporter, false);
		parser.reportSyntaxErrorIsRequired = false;

		CompilationUnitDeclaration unit = parser.parse(cu, compilationResult);
		org.eclipse.jdt.internal.compiler.ast.TypeDeclaration[] types = unit != null ? unit.types : null;
		if (types == null) continue;
		for (int j = 0, k = types.length; j < k; j++) {
			TypeDeclaration type = types[j];
			char[] name = type.isSecondary() ? type.name : null;  // add only secondary types
			if (name != null)
				packageEntry.put(new String(name), s);
		}
	}
	return packageEntry;
}
private NameEnvironmentAnswer findSourceSecondaryType(String typeName, String qualifiedPackageName, String qualifiedBinaryFileName) {

	if (this.packageSecondaryTypes == null) this.packageSecondaryTypes = new Hashtable<>();
	Hashtable<String, String> packageEntry = this.packageSecondaryTypes.get(qualifiedPackageName);
	if (packageEntry == null) {
		packageEntry = 	getSecondaryTypes(qualifiedPackageName);
		this.packageSecondaryTypes.put(qualifiedPackageName, packageEntry);
	}
	String fileName = packageEntry.get(typeName);
	return fileName != null ? new NameEnvironmentAnswer(new CompilationUnit(null,
			fileName, this.encoding, this.destinationPath),
			fetchAccessRestriction(qualifiedBinaryFileName)) : null;
}


@Override
public char[][][] findTypeNames(String qualifiedPackageName, String moduleName) {
	if (!isPackage(qualifiedPackageName, moduleName)) {
		return null; // most common case
	}
	File dir = new File(this.path + qualifiedPackageName);
	if (!dir.exists() || !dir.isDirectory()) {
		return null;
	}
	String[] listFiles = dir.list(new FilenameFilter() {
		@Override
		public boolean accept(File directory1, String name) {
			String fileName = name.toLowerCase();
			return fileName.endsWith(".class") || fileName.endsWith(".java"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	});
	int length;
	if (listFiles == null || (length = listFiles.length) == 0) {
		return null;
	}
	Set<String> secondary = getSecondaryTypes(qualifiedPackageName).keySet();
	char[][][] result = new char[length + secondary.size()][][];
	char[][] packageName = CharOperation.splitOn(File.separatorChar, qualifiedPackageName.toCharArray());
	for (int i = 0; i < length; i++) {
		String fileName = listFiles[i];
		int indexOfLastDot = fileName.indexOf('.');
		String typeName = indexOfLastDot > 0 ? fileName.substring(0, indexOfLastDot) : fileName;
		result[i] = CharOperation.arrayConcat(packageName, typeName.toCharArray());
	}
	if (secondary.size() > 0) {
		int idx = length;
		for (String type : secondary) {
			result[idx++] = CharOperation.arrayConcat(packageName, type.toCharArray());
		}
	}
	return result;
}
@Override
public void initialize() throws IOException {
	// nothing to do
}
@Override
public char[][] getModulesDeclaringPackage(String qualifiedPackageName, /*@Nullable*/String moduleName) {
	String qp2 = File.separatorChar == '/' ? qualifiedPackageName : qualifiedPackageName.replace('/', File.separatorChar);
	return singletonModuleNameIf(directoryList(qp2) != null);
}
@Override
public boolean hasCompilationUnit(String qualifiedPackageName, String moduleName) {
	String qp2 = File.separatorChar == '/' ? qualifiedPackageName : qualifiedPackageName.replace('/', File.separatorChar);
	String[] dirList = directoryList(qp2);
	if (dirList != null) {
		for (String entry : dirList) {
			String entryLC = entry.toLowerCase();
			if (entryLC.endsWith(SUFFIX_STRING_java) || entryLC.endsWith(SUFFIX_STRING_class))
				return true;
		}
	}
	return false;
}
@Override
public boolean hasCUDeclaringPackage(String qualifiedPackageName, Function<CompilationUnit, String> pkgNameExtractor) {
	String qp2 = File.separatorChar == '/' ? qualifiedPackageName : qualifiedPackageName.replace('/', File.separatorChar);
	String[] directoryList = directoryList(qp2);
	if(directoryList == null)
		return false;
	return Stream.of(directoryList).anyMatch(entry -> {
		String entryLC = entry.toLowerCase();
		boolean hasDeclaration = false;
		String fullPath = this.path + qp2 + "/" + entry; //$NON-NLS-1$
		String pkgName = null;
		if (entryLC.endsWith(SUFFIX_STRING_class)) {
			return true;
		} else if (entryLC.endsWith(SUFFIX_STRING_java)) {
			CompilationUnit cu = new CompilationUnit(null, fullPath, this.encoding);
			pkgName = pkgNameExtractor.apply(cu);
		}
		if (pkgName != null && pkgName.equals(qp2.replace(File.separatorChar, '.')))
			hasDeclaration = true;
		return hasDeclaration;
	});
}

@Override
public char[][] listPackages() {
	Set<String> packageNames = new HashSet<>();
	try {
		Path basePath = FileSystems.getDefault().getPath(this.path);
		Files.walkFileTree(basePath, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
				if (file.toString().toLowerCase().endsWith(SUFFIX_STRING_class)) {
					packageNames.add(file.getParent().relativize(basePath).toString().replace('/', '.'));
				}
				return FileVisitResult.CONTINUE;
			}
		});
	} catch (IOException e) {
		// treat as if files are missing
	}
	return packageNames.stream().map(String::toCharArray).toArray(char[][]::new);
}

@Override
public void reset() {
	super.reset();
	this.directoryCache = new Hashtable(11);
}
@Override
public String toString() {
	return "ClasspathDirectory " + this.path; //$NON-NLS-1$
}
@Override
public char[] normalizedPath() {
	if (this.normalizedPath == null) {
		this.normalizedPath = this.path.toCharArray();
		if (File.separatorChar == '\\') {
			CharOperation.replace(this.normalizedPath, '\\', '/');
		}
	}
	return this.normalizedPath;
}
@Override
public String getPath() {
	return this.path;
}
@Override
public int getMode() {
	return this.mode;
}
@Override
public IModule getModule() {
	return this.module;
}
}
