/*******************************************************************************
 * Copyright (c) 2018, 2021 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.batch;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.batch.FileSystem.Classpath;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFormatException;
import org.eclipse.jdt.internal.compiler.env.AccessRuleSet;
import org.eclipse.jdt.internal.compiler.env.IModule;
import org.eclipse.jdt.internal.compiler.env.NameEnvironmentAnswer;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.util.CtSym;
import org.eclipse.jdt.internal.compiler.util.JRTUtil;
import org.eclipse.jdt.internal.compiler.util.Util;

public class ClasspathJep247 extends ClasspathJrt {

	protected java.nio.file.FileSystem fs = null;
	protected String compliance = null;
	protected long jdklevel;
	protected String releaseInHex = null;
	protected String[] subReleases = null;
	protected Path releasePath = null;
	protected Set<String> packageCache;
	protected File jdkHome;
	protected String modulePath = null;

	public ClasspathJep247(File jdkHome, String release, AccessRuleSet accessRuleSet) {
		super(jdkHome, false, accessRuleSet, null);
		this.compliance = release;
		this.jdklevel = CompilerOptions.releaseToJDKLevel(this.compliance);
		this.jdkHome = jdkHome;
		this.file = new File(new File(jdkHome, "lib"), "jrt-fs.jar"); //$NON-NLS-1$ //$NON-NLS-2$
	}
	@Override
	public List<Classpath> fetchLinkedJars(FileSystem.ClasspathSectionProblemReporter problemReporter) {
		 return null;
	}
	@Override
	public NameEnvironmentAnswer findClass(char[] typeName, String qualifiedPackageName, String moduleName, String qualifiedBinaryFileName) {
		return findClass(typeName, qualifiedPackageName, moduleName, qualifiedBinaryFileName, false);
	}
	@Override
	public NameEnvironmentAnswer findClass(char[] typeName, String qualifiedPackageName, String moduleName, String qualifiedBinaryFileName, boolean asBinaryOnly) {
		if (!isPackage(qualifiedPackageName, moduleName))
			return null; // most common case

		try {
			//TODO: Check if any conversion needed for path separator
			ClassFileReader reader = null;
			byte[] content = null;
			qualifiedBinaryFileName = qualifiedBinaryFileName.replace(".class", ".sig"); //$NON-NLS-1$ //$NON-NLS-2$
			if (this.subReleases != null && this.subReleases.length > 0) {
				for (String rel : this.subReleases) {
					Path p = this.fs.getPath(rel, qualifiedBinaryFileName);
					if (Files.exists(p)) {
						content = JRTUtil.safeReadBytes(p);
						if (content != null)
							break;
					}
				}
			} else {
				content = JRTUtil.safeReadBytes(this.fs.getPath(this.releaseInHex, qualifiedBinaryFileName));
			}
			if (content != null) {
				reader = new ClassFileReader(content, qualifiedBinaryFileName.toCharArray());
				char[] modName = moduleName != null ? moduleName.toCharArray() : null;
				return new NameEnvironmentAnswer(reader, fetchAccessRestriction(qualifiedBinaryFileName), modName);
			}
		} catch (ClassFormatException | IOException e) {
			// continue
		}
		return null;
	}

	@Override
	public void initialize() throws IOException {
		if (this.compliance == null) {
			return;
		}
		this.releaseInHex = CtSym.getReleaseCode(this.compliance);
		Path filePath = this.jdkHome.toPath().resolve("lib").resolve("ct.sym"); //$NON-NLS-1$ //$NON-NLS-2$
		URI t = filePath.toUri();
		if (!Files.exists(filePath)) {
			return;
		}
		URI uri = URI.create("jar:file:" + t.getRawPath()); //$NON-NLS-1$
		try {
			this.fs = FileSystems.getFileSystem(uri);
		} catch(FileSystemNotFoundException fne) {
			// Ignore and move on
		}
		if (this.fs == null) {
			HashMap<String, ?> env = new HashMap<>();
			try {
				this.fs = FileSystems.newFileSystem(uri, env);
			} catch (FileSystemAlreadyExistsException e) {
				this.fs = FileSystems.getFileSystem(uri);
			}
		}
		this.releasePath = this.fs.getPath("/"); //$NON-NLS-1$
		if (!Files.exists(this.fs.getPath(this.releaseInHex))) {
			throw new IllegalArgumentException("release " + this.compliance + " is not found in the system");  //$NON-NLS-1$//$NON-NLS-2$
		}
		super.initialize();
	}
	@Override
	public void loadModules() {
		// Modules below level 9 are not dealt with here. Leave it to ClasspathJrt
		if (this.jdklevel <= ClassFileConstants.JDK1_8) {
			super.loadModules();
			return;
		}
		final Path modPath = this.fs.getPath(this.releaseInHex + "-modules"); //$NON-NLS-1$
		if (!Files.exists(modPath)) {
			throw new IllegalArgumentException("release " + this.compliance + " is not found in the system");  //$NON-NLS-1$//$NON-NLS-2$
		}
		this.modulePath = this.file.getPath() + "|" + modPath.toString(); //$NON-NLS-1$
		Map<String, IModule> cache = ModulesCache.get(this.modulePath);
		if (cache == null) {
			try (DirectoryStream<java.nio.file.Path> stream = Files.newDirectoryStream(modPath)) {
				HashMap<String,IModule> newCache = new HashMap<>();
				for (final java.nio.file.Path subdir: stream) {
						Files.walkFileTree(subdir, new FileVisitor<java.nio.file.Path>() {

							@Override
							public FileVisitResult preVisitDirectory(java.nio.file.Path dir, BasicFileAttributes attrs)
									throws IOException {
								return FileVisitResult.CONTINUE;
							}

							@Override
							public FileVisitResult visitFile(java.nio.file.Path f, BasicFileAttributes attrs) throws IOException {
								byte[] content = null;
								if (Files.exists(f)) {
									content = JRTUtil.safeReadBytes(f);
									if (content == null)
										return FileVisitResult.CONTINUE;
									ClasspathJep247.this.acceptModule(content, newCache);
									ClasspathJep247.this.moduleNamesCache.add(JRTUtil.sanitizedFileName(f));
								}
								return FileVisitResult.CONTINUE;
							}

							@Override
							public FileVisitResult visitFileFailed(java.nio.file.Path f, IOException exc) throws IOException {
								return FileVisitResult.CONTINUE;
							}

							@Override
							public FileVisitResult postVisitDirectory(java.nio.file.Path dir, IOException exc) throws IOException {
								return FileVisitResult.CONTINUE;
							}
						});
				}
				synchronized(ModulesCache) {
					if (ModulesCache.get(this.modulePath) == null) {
						ModulesCache.put(this.modulePath, Collections.unmodifiableMap(newCache));
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			this.moduleNamesCache.addAll(cache.keySet());
		}
	}
	@Override
	void acceptModule(ClassFileReader reader, Map<String, IModule> cache) {
		// Modules below level 9 are not dealt with here. Leave it to ClasspathJrt
		if (this.jdklevel <= ClassFileConstants.JDK1_8) {
			super.acceptModule(reader, cache);
			return;
		}
		if (reader != null) {
			IModule moduleDecl = reader.getModuleDeclaration();
			if (moduleDecl != null) {
				cache.put(String.valueOf(moduleDecl.name()), moduleDecl);
			}
		}
	}
	protected void addToPackageCache(String packageName, boolean endsWithSep) {
		if (this.packageCache.contains(packageName))
			return;
		this.packageCache.add(packageName);
	}
	@Override
	public synchronized char[][] getModulesDeclaringPackage(String qualifiedPackageName, String moduleName) {
		if (this.packageCache == null) {
			this.packageCache = new HashSet<>(41);
			this.packageCache.add(Util.EMPTY_STRING);
			List<String> sub = new ArrayList<>();
			try (DirectoryStream<java.nio.file.Path> stream = Files.newDirectoryStream(this.releasePath)) {
				for (final java.nio.file.Path subdir: stream) {
					String rel = JRTUtil.sanitizedFileName(subdir);
					if (rel.contains(this.releaseInHex)) {
						sub.add(rel);
					} else {
						continue;
					}
					Files.walkFileTree(subdir, new FileVisitor<java.nio.file.Path>() {
						@Override
						public FileVisitResult preVisitDirectory(java.nio.file.Path dir, BasicFileAttributes attrs) throws IOException {
							if (dir.getNameCount() <= 1)
								return FileVisitResult.CONTINUE;
							Path relative = dir.subpath(1, dir.getNameCount());
							addToPackageCache(relative.toString(), false);
							return FileVisitResult.CONTINUE;
						}

						@Override
						public FileVisitResult visitFile(java.nio.file.Path f, BasicFileAttributes attrs) throws IOException {
							return FileVisitResult.CONTINUE;
						}

						@Override
						public FileVisitResult visitFileFailed(java.nio.file.Path f, IOException exc) throws IOException {
							return FileVisitResult.CONTINUE;
						}

						@Override
						public FileVisitResult postVisitDirectory(java.nio.file.Path dir, IOException exc) throws IOException {
							return FileVisitResult.CONTINUE;
						}
					});
				}
			} catch (IOException e) {
				e.printStackTrace();
				// Rethrow
			}
			this.subReleases = sub.toArray(new String[sub.size()]);
		}
		if (moduleName == null) {
			// Delegate to the boss, even if it means inaccurate error reporting at times
			List<String> mods = JRTUtil.getModulesDeclaringPackage(this.file, qualifiedPackageName, moduleName);
			return CharOperation.toCharArrays(mods);
		}
		return singletonModuleNameIf(this.packageCache.contains(qualifiedPackageName));
	}

	@Override
	public String toString() {
		return "Classpath for JEP 247 for JDK " + this.file.getPath(); //$NON-NLS-1$
	}
	@Override
	public char[] normalizedPath() {
		if (this.normalizedPath == null) {
			String path2 = this.getPath();
			char[] rawName = path2.toCharArray();
			if (File.separatorChar == '\\') {
				CharOperation.replace(rawName, '\\', '/');
			}
			this.normalizedPath = CharOperation.subarray(rawName, 0, CharOperation.lastIndexOf('.', rawName));
		}
		return this.normalizedPath;
	}
	@Override
	public String getPath() {
		if (this.path == null) {
			try {
				this.path = this.file.getCanonicalPath();
			} catch (IOException e) {
				// in case of error, simply return the absolute path
				this.path = this.file.getAbsolutePath();
			}
		}
		return this.path;
	}
	@Override
	public int getMode() {
		return BINARY;
	}

}
