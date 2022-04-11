/*******************************************************************************
 * Copyright (c) 2020, 2021 Andrey Loskutov and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Andrey Loskutov <loskutov@gmx.de> - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.ProviderNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Abstraction to the ct.sym file access (see https://openjdk.java.net/jeps/247). The ct.sym file is required to
 * implement JEP 247 feature (compile with "--release" option against class stubs for older releases) and is currently
 * (Java 15) a jar file with undocumented internal structure, currently existing in at least two different format
 * versions (pre Java 12 and Java 12 and later).
 * <p>
 * The only documentation known seem to be the current implementation of
 * com.sun.tools.javac.platform.JDKPlatformProvider and probably some JDK build tools that construct ct.sym file. Root
 * directories inside the file are somehow related to the Java release number, encoded as single digit or letter (single
 * digits for releases 7 to 9, capital letters for 10 and higher).
 * <p>
 * If a release directory contains "system-modules" file, it is a flag that this release files are not inside ct.sym
 * file because it is the current release, and jrt file system should be used instead.
 * <p>
 * All other release directories contain encoded signature (*.sig) files with class stubs for classes in the release.
 * <p>
 * Some directories contain files that are shared between different releases, exact logic how they are distributed is
 * not known.
 * <p>
 * Known format versions of ct.sym:
 * <p>
 * Pre JDK 12:
 *
 * <pre>
 * ct.sym -> 9 -> java/ -> lang/
 * ct.sym -> 9-modules -> java.base -> module-info.sig
 * </pre>
 *
 * From JDK 12 onward:
 *
 * <pre>
 * ct.sym -> 9 -> java.base -> java/ -> lang/
 * ct.sym -> 9 -> java.base -> module-info.sig
 * </pre>
 *
 * Notably,
 * <ol>
 * <li>in JDK 12 modules classes and ordinary classes are located in the same location
 * <li>in JDK 12, ordinary classes are found inside their respective modules
 * </ol>
 * <p>
 *
 * Searching a file for a given release in ct.sym means finding & traversing all possible release related directories
 * and searching for matching path.
 */
public class CtSym {

	/**
	 * 'B' is code for Java 11, see {@link #getReleaseCode(String)}.
	 */
	private static final char JAVA_11 = 'B';

	public static final boolean DISABLE_CACHE = Boolean.getBoolean("org.eclipse.jdt.disable_CTSYM_cache"); //$NON-NLS-1$

	static boolean VERBOSE = false;

	/**
	 * Map from path (release) inside ct.sym file to all class signatures loaded
	 */
	private final Map<Path, Optional<byte[]>> fileCache = new ConcurrentHashMap<>(10007);

	private final Path jdkHome;

	private final Path ctSymFile;

	private FileSystem fs;

	Path root;

	private boolean isJRE12Plus;

	/**
	 * Paths of all root directories, per release (versions encoded). e.g. in JDK 11, Java 10 mapping looks like A -> [A,
	 * A-modules, A789, A9] but to have more fun, in JDK 14, same mapping looks like A -> [A, AB, ABC, ABCD]
	 */
	private final Map<String, List<Path>> releaseRootPaths = new ConcurrentHashMap<>();

	/**
	 * All paths that exist in all release root directories, per release (versions encoded). The first key is release
	 * code. The second key is the "full qualified binary name" of the class (without module name and
	 * with .sig suffix). The value is the full path of the corresponding signature file in the ct.sym file.
	 */
	private final Map<String, Map<String, Path>> allReleasesPaths = new ConcurrentHashMap<>();

	CtSym(Path jdkHome) throws IOException {
		this.jdkHome = jdkHome;
		this.ctSymFile = jdkHome.resolve("lib/ct.sym"); //$NON-NLS-1$
		init();
	}

	private void init() throws IOException {
		boolean exists = Files.exists(this.ctSymFile);
		if (!exists) {
			throw new FileNotFoundException("File " + this.ctSymFile + " does not exist"); //$NON-NLS-1$//$NON-NLS-2$
		}
		FileSystem fst = null;
		URI uri = URI.create("jar:file:" + this.ctSymFile.toUri().getRawPath()); //$NON-NLS-1$
		try {
			fst = FileSystems.getFileSystem(uri);
		} catch (Exception fne) {
			// Ignore and move on
		}
		if (fst == null) {
			try {
				fst = FileSystems.newFileSystem(uri, new HashMap<>(), ClassLoader.getSystemClassLoader());
			} catch (FileSystemAlreadyExistsException e) {
				fst = FileSystems.getFileSystem(uri);
			} catch (ProviderNotFoundException e) {
				throw new IOException("Failed to create ct.sym file system for " + this.ctSymFile, e); //$NON-NLS-1$
			}
		}
		this.fs = fst;
		if (fst == null) {
			throw new IOException("Failed to create ct.sym file system for " + this.ctSymFile); //$NON-NLS-1$
		} else {
			this.root = fst.getPath("/"); //$NON-NLS-1$
			this.isJRE12Plus = isCurrentRelease12plus();
		}
	}

	/**
	 * @return never null
	 */
	public FileSystem getFs() {
		return this.fs;
	}

	/**
	 *
	 * @return true if this file is from Java 12+ JRE
	 */
	public boolean isJRE12Plus() {
		return this.isJRE12Plus;
	}

	/**
	 * @return never null
	 */
	public Path getRoot() {
		return this.root;
	}

	/**
	 * @param releaseCode
	 *            major JDK version segment as version code (8, 9, A, etc)
	 * @return set with all root paths related to given release in ct.sym file
	 */
	public List<Path> releaseRoots(String releaseCode) {
		List<Path> list = this.releaseRootPaths.computeIfAbsent(releaseCode, x -> {
			List<Path> rootDirs = new ArrayList<>();
			try (DirectoryStream<Path> stream = Files.newDirectoryStream(this.root)) {
				for (final Path subdir : stream) {
					String rel = subdir.getFileName().toString();
					if (rel.contains("-")) { //$NON-NLS-1$
						// Ignore META-INF etc. We are only interested in A-Z 0-9
						continue;
					}
					// com.sun.tools.javac.platform.JDKPlatformProvider.PlatformDescriptionImpl.getFileManager()
					// https://github.com/openjdk/jdk/blob/master/src/jdk.compiler/share/classes/com/sun/tools/javac/platform/JDKPlatformProvider.java
					if (rel.contains(releaseCode)) {
						rootDirs.add(subdir);
					} else {
						continue;
					}
				}
			} catch (IOException e) {
				return Collections.emptyList();
			}
			return Collections.unmodifiableList(rootDirs);
		});
		return list;
	}

	/**
	 * Retrieves the full path in ct.sym file fro given signature file in given release
	 * <p>
	 * 12+: something like
	 * <p>
	 * java/io/Reader.sig -> /879/java.base/java/io/Reader.sig
	 * <p>
	 * before 12:
	 * <p>
	 * java/io/Reader.sig -> /8769/java/io/Reader.sig
	 *
	 * @param releaseCode release number encoded (7,8,9,A,B...)
	 * @param qualifiedSignatureFileName signature file name (without module)
	 * @param moduleName
	 * @return corresponding path in ct.sym file system or null if not found
	 */
	public Path getFullPath(String releaseCode, String qualifiedSignatureFileName, String moduleName) {
		String sep = this.fs.getSeparator();
		if (DISABLE_CACHE) {
			List<Path> releaseRoots = releaseRoots(releaseCode);
			for (Path rroot : releaseRoots) {
				// Calculate file path
				Path p = null;
				if (isJRE12Plus()) {
					if (moduleName == null) {
						moduleName = getModuleInJre12plus(releaseCode, qualifiedSignatureFileName);
					}
					p = rroot.resolve(moduleName + sep + qualifiedSignatureFileName);
				} else {
					p = rroot.resolve(qualifiedSignatureFileName);
				}

				// If file is known, read it from ct.sym
				if (Files.exists(p)) {
					if (VERBOSE) {
						System.out.println("found: " + qualifiedSignatureFileName + " in " + p + " for module " + moduleName  + "\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
					}
					return p;
				}
			}
			if (VERBOSE) {
				System.out.println("not found: " + qualifiedSignatureFileName + " for module " + moduleName); //$NON-NLS-1$ //$NON-NLS-2$
			}
			return null;
		}
		Map<String, Path> releasePaths = getCachedReleasePaths(releaseCode);
		Path path;
		if(moduleName != null) {
			// Without this, org.eclipse.jdt.core.tests.model.ModuleBuilderTests.testConvertToModule() fails on 12+ JRE
			path = releasePaths.get(moduleName + sep + qualifiedSignatureFileName);

			// Special handling of broken module schema in java 11 for compilation with --release 10
			if(path == null && !this.isJRE12Plus() && "A".equals(releaseCode)){ //$NON-NLS-1$
				path = releasePaths.get(qualifiedSignatureFileName);
			}
		} else {
			path = releasePaths.get(qualifiedSignatureFileName);
		}
		if (VERBOSE) {
			if (path != null) {
				System.out.println("found: " + qualifiedSignatureFileName + " in " + path + " for module " + moduleName +"\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			} else {
				System.out.println("not found: " + qualifiedSignatureFileName + " for module " + moduleName); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		return path;
	}

	public String getModuleInJre12plus(String releaseCode, String qualifiedSignatureFileName) {
		if (DISABLE_CACHE) {
			return findModuleForFileInJre12plus(releaseCode, qualifiedSignatureFileName);
		}
		Map<String, Path> releasePaths = getCachedReleasePaths(releaseCode);
		Path path = releasePaths.get(qualifiedSignatureFileName);
		if (path != null && path.getNameCount() > 2) {
			// First segment is release, second: module
			return path.getName(1).toString();
		}
		return null;
	}

	private String findModuleForFileInJre12plus(String releaseCode, String qualifiedSignatureFileName) {
		for (Path rroot : releaseRoots(releaseCode)) {
			try (DirectoryStream<Path> stream = Files.newDirectoryStream(rroot)) {
				for (final Path subdir : stream) {
					Path p = subdir.resolve(qualifiedSignatureFileName);
					if (Files.exists(p)) {
						if (subdir.getNameCount() == 2) {
							return subdir.getName(1).toString();
						}
					}
				}
			} catch (IOException e) {
				// not found...
			}
		}
		return null;
	}

	/**
	 * Populates {@link #allReleasesPaths} with the paths of all files within each matching release directory in ct.sym.
	 * This cache is an optimization to avoid excessive calls into the zip filesystem in
	 * {@code ClasspathJrtWithReleaseOption#findClass(String, String, String, String, boolean, Predicate)}.
	 * <p>
	 * 12+: something like
	 * <p>
	 * java.base/javax/net/ssl/SSLSocketFactory.sig -> /89ABC/java.base/javax/net/ssl/SSLSocketFactory.sig
	 * <p> or
	 * javax/net/ssl/SSLSocketFactory.sig -> /89ABC/java.base/javax/net/ssl/SSLSocketFactory.sig
	 * <p>
	 * before 12: javax/net/ssl/SSLSocketFactory.sig -> /89ABC/java.base/javax/net/ssl/SSLSocketFactory.sig
	 */
	private Map<String, Path> getCachedReleasePaths(String releaseCode) {
		Map<String, Path> result = this.allReleasesPaths.computeIfAbsent(releaseCode, x -> {
			List<Path> roots = releaseRoots(releaseCode);
			Map<String, Path> allReleaseFiles = new HashMap<>(4999);
			for (Path start : roots) {
				try {
					Files.walk(start).filter(Files::isRegularFile).forEach(p -> {
						if (isJRE12Plus()) {
							// Don't use module name as part of the key
							String binaryNameWithoutModule = p.subpath(2, p.getNameCount()).toString();
							allReleaseFiles.put(binaryNameWithoutModule, p);
							// Cache extra key with module added, see getFullPath().
							String binaryNameWithModule = p.subpath(1, p.getNameCount()).toString();
							allReleaseFiles.put(binaryNameWithModule, p);
						} else {
							String binaryNameWithoutModule = p.subpath(1, p.getNameCount()).toString();
							allReleaseFiles.put(binaryNameWithoutModule, p);
						}
					});
				} catch (IOException e) {
					// Not much do to if we can't list the dir; anything in there will be treated
					// as if it were missing.
				}
			}
			return Collections.unmodifiableMap(allReleaseFiles);
		});
		return result;
	}

	public byte[] getFileBytes(Path path) throws IOException {
		if (DISABLE_CACHE) {
			return JRTUtil.safeReadBytes(path);
		} else {
			Optional<byte[]> bytes = this.fileCache.computeIfAbsent(path, key -> {
				try {
					return Optional.ofNullable(JRTUtil.safeReadBytes(key));
				} catch (IOException e) {
					return Optional.empty();
				}
			});
			if (VERBOSE) {
				System.out.println("got bytes: " + path); //$NON-NLS-1$
			}
			return bytes.orElse(null);
		}
	}

	private boolean isCurrentRelease12plus() throws IOException {
		// ignore everything that is not one character (Java release code is one character plus separator)
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(this.root, p -> p.toString().length() == 2)) {
			for (final Path subdir : stream) {
				String rel = JRTUtil.sanitizedFileName(subdir);
				if (rel.length() != 1) {
					continue;
				}
				try {
					char releaseCode = rel.charAt(0);
					// If a release directory contains "system-modules" file, it is a flag
					// that this is the *current* release
					if (releaseCode > JAVA_11 && Files.exists(this.fs.getPath(rel, "system-modules"))) { //$NON-NLS-1$
						return true;
					}
				} catch (NumberFormatException e) {
					// META-INF, A-modules etc
					continue;
				}
			}
		}
		return false;
	}

	@Override
	public int hashCode() {
		return this.jdkHome.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof CtSym)) {
			return false;
		}
		CtSym other = (CtSym) obj;
		return this.jdkHome.equals(other.jdkHome);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("CtSym ["); //$NON-NLS-1$
		sb.append("file="); //$NON-NLS-1$
		sb.append(this.ctSymFile);
		sb.append("]"); //$NON-NLS-1$
		return sb.toString();
	}

	/**
	 * Tries to translate numeric Java version to the corresponding release "code".
	 * <ul>
	 * <li>7, 8 and 9 are just returned "as is"
	 * <li>versions up from 10 are returned as upper letters starting with "A", so 10 is "A", 11 is "B" and so on.
	 * </ul>
	 *
	 * @param release
	 *            release version as number (8, 9, 10, ...)
	 * @return the "code" used by ct.sym for given Java version
	 */
	public static String getReleaseCode(String release) {
		int numericVersion = Integer.parseInt(release);
		if(numericVersion < 10) {
			return String.valueOf(numericVersion);
		}
		return String.valueOf((char) ('A' + (numericVersion - 10)));
	}

}