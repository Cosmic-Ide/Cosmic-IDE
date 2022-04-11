/*******************************************************************************
 * Copyright (c) 2016, 2021 IBM Corporation and others.
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
import java.nio.file.FileSystem;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.function.Predicate;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFormatException;
import org.eclipse.jdt.internal.compiler.env.AccessRuleSet;
import org.eclipse.jdt.internal.compiler.env.IBinaryType;
import org.eclipse.jdt.internal.compiler.env.IModule;
import org.eclipse.jdt.internal.compiler.env.NameEnvironmentAnswer;
import org.eclipse.jdt.internal.compiler.util.CtSym;
import org.eclipse.jdt.internal.compiler.util.JRTUtil;
import org.eclipse.jdt.internal.compiler.util.SimpleSet;
import org.eclipse.jdt.internal.compiler.util.SuffixConstants;
import org.eclipse.jdt.internal.core.util.Util;

public class ClasspathJrtWithReleaseOption extends ClasspathJrt {

	static String MODULE_INFO = "module-info.sig"; //$NON-NLS-1$

	final String release;
	String releaseCode;
	/**
	 * Null for releases without ct.sym file or for releases matching current one
	 */
	private FileSystem fs;
	protected Path releasePath;
	protected Path modulePath;
	private String modPathString;
	CtSym ctSym;



	public ClasspathJrtWithReleaseOption(String zipFilename, AccessRuleSet accessRuleSet, IPath externalAnnotationPath,
			String release) throws CoreException {
		super();
		if (release == null || release.equals("")) { //$NON-NLS-1$
			throw new IllegalArgumentException("--release argument can not be null"); //$NON-NLS-1$
		}
		setZipFile(zipFilename);
		this.accessRuleSet = accessRuleSet;
		if (externalAnnotationPath != null) {
			this.externalAnnotationPath = externalAnnotationPath.toString();
		}
		this.release = getReleaseOptionFromCompliance(release);
		try {
			this.ctSym = JRTUtil.getCtSym(Paths.get(this.zipFilename).getParent().getParent());
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, ClasspathJrtWithReleaseOption.class,
					"Failed to init ct.sym for " + this.zipFilename, e)); //$NON-NLS-1$
		}
		initialize();
		loadModules();
	}
	/*
	 * JDK 11 doesn't contain release 5. Hence
	 * if the compliance is below 6, we simply return the lowest supported
	 * release, which is 6.
	 */
	private String getReleaseOptionFromCompliance(String comp) {
		if (JavaCore.compareJavaVersions(comp, JavaCore.VERSION_1_5) <= 0) {
			return "6"; //$NON-NLS-1$
		}
		int index = comp.indexOf("1."); //$NON-NLS-1$
		if (index != -1) {
			return comp.substring(index + 2, comp.length());
		} else {
			return comp;
		}
	}

	/**
	 * Set up the paths where modules and regular classes need to be read. We need to deal with two different kind of
	 * formats of cy.sym, see {@link CtSym} javadoc.
	 *
	 * @see CtSym
	 */
	protected void initialize() throws CoreException {
		this.releaseCode = CtSym.getReleaseCode(this.release);
		this.fs = this.ctSym.getFs();
		this.releasePath = this.ctSym.getRoot();
		Path modPath = this.fs.getPath(this.releaseCode + (this.ctSym.isJRE12Plus() ? "" : "-modules")); //$NON-NLS-1$ //$NON-NLS-2$
		if (Files.exists(modPath)) {
			this.modulePath = modPath;
			this.modPathString = this.zipFilename + "|"+ modPath.toString(); //$NON-NLS-1$
		}

		if (!Files.exists(this.releasePath.resolve(this.releaseCode))) {
			Exception e = new IllegalArgumentException("release " + this.release + " is not found in the system"); //$NON-NLS-1$//$NON-NLS-2$
			throw new CoreException(new Status(IStatus.ERROR, JavaCore.PLUGIN_ID, e.getMessage(), e));
		}
		if (Files.exists(this.fs.getPath(this.releaseCode, "system-modules"))) { //$NON-NLS-1$
			this.fs = null;  // Fallback to default version, all classes are on jrt fs, not here.
		}
	}

	HashMap<String, SimpleSet> findPackagesInModules() {
		// In JDK 11 and before, classes are not listed under their respective modules
		// Hence, we simply go to the default module system for package-module mapping
		if (this.fs == null || !this.ctSym.isJRE12Plus()) {
			return ClasspathJrt.findPackagesInModules(this);
		}
		HashMap<String, SimpleSet> cache = PackageCache.get(this.modPathString);
		if (cache != null) {
			return cache;
		}
		final HashMap<String, SimpleSet> packagesInModule = new HashMap<>();
		PackageCache.put(this.modPathString, packagesInModule);
		try {
			JRTUtil.walkModuleImage(this.jrtFile, this.release, new JRTUtil.JrtFileVisitor<Path>() {
						SimpleSet packageSet = null;

						@Override
						public FileVisitResult visitPackage(Path dir, Path mod, BasicFileAttributes attrs)
								throws IOException {
							ClasspathJar.addToPackageSet(this.packageSet, dir.toString(), true);
							return FileVisitResult.CONTINUE;
						}

						@Override
						public FileVisitResult visitFile(Path file, Path mod, BasicFileAttributes attrs)
								throws IOException {
							return FileVisitResult.CONTINUE;
						}

						@Override
						public FileVisitResult visitModule(Path path, String name) throws IOException {
							this.packageSet = new SimpleSet(41);
							this.packageSet.add(""); //$NON-NLS-1$
							if (name.endsWith("/")) { //$NON-NLS-1$
								name = name.substring(0, name.length() - 1);
							}
							packagesInModule.put(name, this.packageSet);
							return FileVisitResult.CONTINUE;
						}
					}, JRTUtil.NOTIFY_PACKAGES | JRTUtil.NOTIFY_MODULES);
		} catch (IOException e) {
			// return empty handed
		}
		return packagesInModule;
	}

	public void loadModules() {
		if (this.fs == null || !this.ctSym.isJRE12Plus()) {
			ClasspathJrt.loadModules(this);
			return;
		}
		if (this.modPathString == null) {
			return;
		}
		HashMap<String, IModule> cache = ModulesCache.get(this.modPathString);
		if (cache == null) {
			List<Path> releaseRoots = this.ctSym.releaseRoots(this.releaseCode);
			for (Path root : releaseRoots) {
				try {
					Files.walkFileTree(root, Collections.EMPTY_SET, 2, new FileVisitor<java.nio.file.Path>() {
						@Override
						public FileVisitResult preVisitDirectory(java.nio.file.Path dir, BasicFileAttributes attrs)
								throws IOException {
							return FileVisitResult.CONTINUE;
						}

						@Override
						public FileVisitResult visitFile(java.nio.file.Path f, BasicFileAttributes attrs)
								throws IOException {
							if (attrs.isDirectory() || f.getNameCount() < 3) {
								return FileVisitResult.CONTINUE;
							}
							if (f.getFileName().toString().equals(MODULE_INFO)) {
								byte[] content = ClasspathJrtWithReleaseOption.this.ctSym.getFileBytes(f);
								if (content == null) {
									return FileVisitResult.CONTINUE;
								}
								ClasspathJrtWithReleaseOption.this.acceptModule(content, f.getParent().getFileName().toString());
							}
							return FileVisitResult.SKIP_SIBLINGS;
						}

						@Override
						public FileVisitResult visitFileFailed(java.nio.file.Path f, IOException exc)
								throws IOException {
							return FileVisitResult.CONTINUE;
						}

						@Override
						public FileVisitResult postVisitDirectory(java.nio.file.Path dir, IOException exc)
								throws IOException {
							return FileVisitResult.CONTINUE;
						}
					});
				} catch (IOException e) {
					// Nothing much to do
				}
			}
		}
	}


	@Override
	public NameEnvironmentAnswer findClass(String binaryFileName, String qualifiedPackageName, String moduleName,
			String qualifiedBinaryFileName, boolean asBinaryOnly, Predicate<String> moduleNameFilter) {

		if (this.fs == null) {
			return super.findClass(binaryFileName, qualifiedPackageName, moduleName, qualifiedBinaryFileName,
					asBinaryOnly, moduleNameFilter);
		}
		if (!isPackage(qualifiedPackageName, moduleName)) {
			return null; // most common case
		}
		List<Path> releaseRoots = this.ctSym.releaseRoots(this.releaseCode);
		try {
			IBinaryType reader = null;
			byte[] content = null;
			String fileNameWithoutExtension = qualifiedBinaryFileName.substring(0,
												qualifiedBinaryFileName.length() - SuffixConstants.SUFFIX_CLASS.length);
			if (!releaseRoots.isEmpty()) {
				qualifiedBinaryFileName = qualifiedBinaryFileName.replace(".class", ".sig"); //$NON-NLS-1$ //$NON-NLS-2$
				Path fullPath = this.ctSym.getFullPath(this.releaseCode, qualifiedBinaryFileName, moduleName);
				// If file is known, read it from ct.sym
				if (fullPath != null) {
					content = this.ctSym.getFileBytes(fullPath);
					if (content != null) {
						reader = new ClassFileReader(content, qualifiedBinaryFileName.toCharArray());
						if (moduleName != null) {
							((ClassFileReader) reader).moduleName = moduleName.toCharArray();
						} else {
							if (this.ctSym.isJRE12Plus()) {
								moduleName = this.ctSym.getModuleInJre12plus(this.releaseCode, qualifiedBinaryFileName);
								if (moduleName != null) {
									((ClassFileReader) reader).moduleName = moduleName.toCharArray();
								}
							}
						}
					}
				}
			} else {
				// Read the file in a "classic" way from the JDK itself
				reader = ClassFileReader.readFromModule(this.jrtFile, moduleName, qualifiedBinaryFileName,
						moduleNameFilter);
			}
			if (reader != null)
				return createAnswer(fileNameWithoutExtension, reader, reader.getModule());
		} catch (ClassFormatException | IOException e) {
			// treat as if class file is missing
		}
		return null;
	}

	@Override
	public Collection<String> getModuleNames(Collection<String> limitModules) {
		HashMap<String, SimpleSet> cache = findPackagesInModules();
		if (cache != null)
			return selectModules(cache.keySet(), limitModules);
		return Collections.emptyList();
	}

	@Override
	public void cleanup() {
		try {
			super.cleanup();
		} finally {
			// The same file system is also used in JRTUtil, so don't close it here.
			this.fs = null;
			this.ctSym = null;
		}
	}

	@Override
	public boolean hasModule() {
		return this.fs == null ? super.hasModule() : this.modPathString != null;
	}

	@Override
	protected String getKey() {
		return this.fs == null ? super.getKey() : this.modPathString;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof ClasspathJrtWithReleaseOption))
			return false;
		ClasspathJrtWithReleaseOption jar = (ClasspathJrtWithReleaseOption) o;
		if (!Util.equalOrNull(this.release, jar.release)) {
			return false;
		}
		return super.equals(o);
	}

	@Override
	public int hashCode() {
		int hash = this.zipFilename == null ? super.hashCode() : this.zipFilename.hashCode();
		return Util.combineHashCodes(hash, this.release.hashCode());
	}

	@Override
	public String toString() {
		String start = "Classpath jrt file " + this.zipFilename + " with --release option " + this.release; //$NON-NLS-1$ //$NON-NLS-2$
		return start;
	}

}
