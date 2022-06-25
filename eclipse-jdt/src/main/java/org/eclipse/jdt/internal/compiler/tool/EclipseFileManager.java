/*******************************************************************************
 * Copyright (c) 2006, 2019 IBM Corporation and others.
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
package org.eclipse.jdt.internal.compiler.tool;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.zip.ZipException;

import javax.lang.model.SourceVersion;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;

import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.internal.compiler.DefaultErrorHandlingPolicies;
import org.eclipse.jdt.internal.compiler.batch.FileSystem;
import org.eclipse.jdt.internal.compiler.batch.FileSystem.Classpath;
import org.eclipse.jdt.internal.compiler.batch.Main;
import org.eclipse.jdt.internal.compiler.batch.Main.ResourceBundleFactory;
import org.eclipse.jdt.internal.compiler.batch.ModuleFinder;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.env.AccessRestriction;
import org.eclipse.jdt.internal.compiler.env.AccessRule;
import org.eclipse.jdt.internal.compiler.env.AccessRuleSet;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.parser.Parser;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblemFactory;
import org.eclipse.jdt.internal.compiler.problem.ProblemReporter;
import org.eclipse.jdt.internal.compiler.tool.JrtFileSystem.JrtFileObject;
import org.eclipse.jdt.internal.compiler.tool.ModuleLocationHandler.LocationContainer;
import org.eclipse.jdt.internal.compiler.tool.ModuleLocationHandler.LocationWrapper;
import org.eclipse.jdt.internal.compiler.tool.ModuleLocationHandler.ModuleLocationWrapper;
import org.eclipse.jdt.internal.compiler.util.Util;

/**
 * Implementation of the Standard Java File Manager
 */
public class EclipseFileManager implements StandardJavaFileManager {
	private static final String NO_EXTENSION = "";//$NON-NLS-1$
	static final int HAS_EXT_DIRS = 1;
	static final int HAS_BOOTCLASSPATH = 2;
	static final int HAS_ENDORSED_DIRS = 4;
	static final int HAS_PROCESSORPATH = 8;
	static final int HAS_PROC_MODULEPATH = 16;

	Map<File, Archive> archivesCache;
	Charset charset;
	Locale locale;
	ModuleLocationHandler locationHandler;
	final Map<Location, URLClassLoader> classloaders;
	int flags;
	boolean isOnJvm9;
	File jrtHome;
	JrtFileSystem jrtSystem;
	public ResourceBundle bundle;
	private String releaseVersion;

	public EclipseFileManager(Locale locale, Charset charset) {
		this.locale = locale == null ? Locale.getDefault() : locale;
		this.charset = charset == null ? Charset.defaultCharset() : charset;
		this.locationHandler = new ModuleLocationHandler();
		this.classloaders = new HashMap<>();
		this.archivesCache = new HashMap<>();
		this.isOnJvm9 = isRunningJvm9();
		try {
			initialize(Util.getJavaHome());
		} catch (IOException e) {
			e.printStackTrace();
			// ignore
		}
		try {
			this.bundle = ResourceBundleFactory.getBundle(this.locale);
		} catch(MissingResourceException e) {
			System.out.println("Missing resource : " + Main.bundleName.replace('.', '/') + ".properties for locale " + locale); //$NON-NLS-1$//$NON-NLS-2$
		}
	}
	protected void initialize(File javahome) throws IOException {
		if (this.isOnJvm9) {
			this.jrtSystem = new JrtFileSystem(javahome);
			try (Archive previous = this.archivesCache.put(javahome, this.jrtSystem)) {
				// nothing. Only theoretically autoclose the previous instance - which does not exist at this time
			}
			this.jrtHome = javahome;
			this.locationHandler.newSystemLocation(StandardLocation.SYSTEM_MODULES, this.jrtSystem);
		} else {
			this.setLocation(StandardLocation.PLATFORM_CLASS_PATH, getDefaultBootclasspath());
		}
		Iterable<? extends File> defaultClasspath = getDefaultClasspath();
		this.setLocation(StandardLocation.CLASS_PATH, defaultClasspath);
		// No annotation module path by default
		this.setLocation(StandardLocation.ANNOTATION_PROCESSOR_PATH, defaultClasspath);
	}
	/* (non-Javadoc)
	 * @see javax.tools.JavaFileManager#close()
	 */
	@Override
	public void close() throws IOException {
		this.locationHandler.close();
		for (Archive archive : this.archivesCache.values()) {
			archive.close();
		}
		this.archivesCache.clear();
		for (URLClassLoader cl : this.classloaders.values()) {
			cl.close();
		}
		this.classloaders.clear();
	}

	private void collectAllMatchingFiles(Location location, File file, String normalizedPackageName, Set<Kind> kinds, boolean recurse, ArrayList<JavaFileObject> collector) {
		if (file.equals(this.jrtHome)) {
			if (location instanceof ModuleLocationWrapper) {
				List<JrtFileObject> list = this.jrtSystem.list((ModuleLocationWrapper) location, normalizedPackageName, kinds, recurse, this.charset);
				for (JrtFileObject fo : list) {
					Kind kind = getKind(getExtension(fo.entryName));
					if (kinds.contains(kind)) {
						collector.add(fo);
					}
				}
			}
		} else if (isArchive(file)) {
			@SuppressWarnings("resource") // cached archive is closed in EclipseFileManager.close()
			Archive archive = this.getArchive(file);
			if (archive != Archive.UNKNOWN_ARCHIVE) {
				String key = normalizedPackageName;
				if (!normalizedPackageName.endsWith("/")) {//$NON-NLS-1$
					key += '/';
				}
				// we have an archive file
				if (recurse) {
					for (String packageName : archive.allPackages()) {
						if (packageName.startsWith(key)) {
							List<String[]> types = archive.getTypes(packageName);
							if (types != null) {
								for (String[] entry : types) {
									final Kind kind = getKind(getExtension(entry[0]));
									if (kinds.contains(kind)) {
										collector.add(archive.getArchiveFileObject(packageName + entry[0], entry[1],
												this.charset));
									}
								}
							}
						}
					}
				} else {
					List<String[]> types = archive.getTypes(key);
					if (types != null) {
						for (String[] entry : types) {
							final Kind kind = getKind(getExtension(entry[0]));
							if (kinds.contains(kind)) {
								collector.add(archive.getArchiveFileObject(key + entry[0], entry[1], this.charset));
							}
						}
					}
				}
			}
		} else {
			// we must have a directory
			File currentFile = new File(file, normalizedPackageName);
			if (!currentFile.exists()) return;
			String path;
			try {
				path = currentFile.getCanonicalPath();
			} catch (IOException e) {
				return;
			}
			if (File.separatorChar == '/') {
				if (!path.endsWith(normalizedPackageName)) return;
			} else if (!path.endsWith(normalizedPackageName.replace('/', File.separatorChar))) return;
			File[] files = currentFile.listFiles();
			if (files != null) {
				// this was a directory
				for (File f : files) {
					if (f.isDirectory() && recurse) {
						collectAllMatchingFiles(location, file, normalizedPackageName + '/' + f.getName(), kinds, recurse, collector);
					} else {
						final Kind kind = getKind(f);
						if (kinds.contains(kind)) {
							collector.add(new EclipseFileObject(normalizedPackageName + f.getName(), f.toURI(), kind, this.charset));
						}
					}
				}
			}
		}
	}

	private Iterable<? extends File> concatFiles(Iterable<? extends File> iterable, Iterable<? extends File> iterable2) {
		ArrayList<File> list = new ArrayList<>();
		if (iterable2 == null) return iterable;
		for (Iterator<? extends File> iterator = iterable.iterator(); iterator.hasNext(); ) {
			list.add(iterator.next());
		}
		for (Iterator<? extends File> iterator = iterable2.iterator(); iterator.hasNext(); ) {
			list.add(iterator.next());
		}
		return list;
	}

	/* (non-Javadoc)
	 * @see javax.tools.JavaFileManager#flush()
	 */
	@Override
	public void flush() throws IOException {
		for (Archive archive : this.archivesCache.values()) {
			archive.flush();
		}
	}

	JrtFileSystem getJrtFileSystem(File f){
		return (JrtFileSystem) getArchive(f);
	}

	private Archive getArchive(File f) {
		// check the archive (jar/zip) cache
		Archive existing = this.archivesCache.get(f);
		if (existing != null) {
			return existing;
		}
		Archive archive = Archive.UNKNOWN_ARCHIVE;
		// create a new archive
		if (f.exists()) {
			try {
				if (isJrt(f)) {
					archive = new JrtFileSystem(f);
				} else {
					archive = new Archive(f);
				}
			} catch (ZipException e) {
				// ignore
			} catch (IOException e) {
				// ignore
			}
		}
		try (Archive previous = this.archivesCache.put(f, archive)) {
			// Nothing but closing previous instance - which should not exist at this time
		}
		return archive;
	}

	/* (non-Javadoc)
	 * @see javax.tools.JavaFileManager#getClassLoader(javax.tools.JavaFileManager.Location)
	 */
	@Override
	public ClassLoader getClassLoader(Location location) {
		validateNonModuleLocation(location);
		Iterable<? extends File> files = getLocation(location);
		if (files == null) {
			// location is unknown
			return null;
		}
		URLClassLoader cl = this.classloaders.get(location);
		if (cl == null) {
			ArrayList<URL> allURLs = new ArrayList<>();
			for (File f : files) {
				try {
					allURLs.add(f.toURI().toURL());
				} catch (MalformedURLException e) {
					// the url is malformed - this should not happen
					throw new RuntimeException(e);
				}
			}
			URL[] result = new URL[allURLs.size()];
			cl = new URLClassLoader(allURLs.toArray(result), getClass().getClassLoader());
			try (URLClassLoader previous = this.classloaders.put(location, cl)) {
				// Nothing but closing previous instance - which should not exist at this time
			} catch (IOException e) {
				//ignore
			}
		}
		return cl;
	}

	private Iterable<? extends File> getPathsFrom(String path) {
		ArrayList<FileSystem.Classpath> paths = new ArrayList<>();
		ArrayList<File> files = new ArrayList<>();
		try {
			this.processPathEntries(Main.DEFAULT_SIZE_CLASSPATH, paths, path, this.charset.name(), false, false);
		} catch (IllegalArgumentException e) {
			return null;
		}
		for (FileSystem.Classpath classpath : paths) {
			files.add(new File(classpath.getPath()));
		}
		return files;
	}

	Iterable<? extends File> getDefaultBootclasspath() {
		List<File> files = new ArrayList<>();
		String javaversion = System.getProperty("java.version");//$NON-NLS-1$
		if(javaversion.length() > 3)
			javaversion = javaversion.substring(0, 3);
		long jdkLevel = CompilerOptions.versionToJdkLevel(javaversion);
		if (jdkLevel < ClassFileConstants.JDK1_6) {
			// wrong jdk - 1.6 or above is required
			return null;
		}

		for (FileSystem.Classpath classpath : org.eclipse.jdt.internal.compiler.util.Util.collectFilesNames()) {
			files.add(new File(classpath.getPath()));
		}
		return files;
	}

	Iterable<? extends File> getDefaultClasspath() {
		// default classpath
		ArrayList<File> files = new ArrayList<>();
		String classProp = System.getProperty("java.class.path"); //$NON-NLS-1$
		if ((classProp == null) || (classProp.length() == 0)) {
			return null;
		} else {
			StringTokenizer tokenizer = new StringTokenizer(classProp, File.pathSeparator);
			String token;
			while (tokenizer.hasMoreTokens()) {
				token = tokenizer.nextToken();
				File file = new File(token);
				if (file.exists()) {
					files.add(file);
				}
			}
		}
		return files;
	}

	private Iterable<? extends File> getEndorsedDirsFrom(String path) {
		ArrayList<FileSystem.Classpath> paths = new ArrayList<>();
		ArrayList<File> files = new ArrayList<>();
		try {
			this.processPathEntries(Main.DEFAULT_SIZE_CLASSPATH, paths, path, this.charset.name(), false, false);
		} catch (IllegalArgumentException e) {
			return null;
		}
		for (FileSystem.Classpath classpath : paths) {
			files.add(new File(classpath.getPath()));
		}
		return files;
	}

	private Iterable<? extends File> getExtdirsFrom(String path) {
		ArrayList<FileSystem.Classpath> paths = new ArrayList<>();
		ArrayList<File> files = new ArrayList<>();
		try {
			this.processPathEntries(Main.DEFAULT_SIZE_CLASSPATH, paths, path, this.charset.name(), false, false);
		} catch (IllegalArgumentException e) {
			return null;
		}
		for (FileSystem.Classpath classpath : paths) {
			files.add(new File(classpath.getPath()));
		}
		return files;
	}

	private String getExtension(File file) {
		String name = file.getName();
		return getExtension(name);
	}
	private String getExtension(String name) {
		int index = name.lastIndexOf('.');
		if (index == -1) {
			return EclipseFileManager.NO_EXTENSION;
		}
		return name.substring(index);
	}

	/* (non-Javadoc)
	 * @see javax.tools.JavaFileManager#getFileForInput(javax.tools.JavaFileManager.Location, java.lang.String, java.lang.String)
	 */
	@Override
	public FileObject getFileForInput(Location location, String packageName, String relativeName) throws IOException {
		validateNonModuleLocation(location);
		Iterable<? extends File> files = getLocation(location);
		if (files == null) {
			throw new IllegalArgumentException("Unknown location : " + location);//$NON-NLS-1$
		}
		String normalizedFileName = normalized(packageName) + '/' + relativeName.replace('\\', '/');
		for (File file : files) {
			if (file.isDirectory()) {
				// handle directory
				File f = new File(file, normalizedFileName);
				if (f.exists()) {
					return new EclipseFileObject(packageName + File.separator + relativeName, f.toURI(), getKind(f), this.charset);
				} else {
					continue; // go to next entry in the location
				}
			} else if (isArchive(file)) {
				// handle archive file
				ArchiveFileObject fileObject = getFileObject(file, normalizedFileName);
				if (fileObject!=null) {
					return fileObject;
				}
			}
		}
		return null;
	}

	@SuppressWarnings("resource") // cached archive is closed in EclipseFileManager.close()
	private ArchiveFileObject getFileObject(File archiveFile, String normalizedFileName) {
		Archive archive = getArchive(archiveFile);
		if (archive == Archive.UNKNOWN_ARCHIVE) {
			return null;
		}
		if (archive.contains(normalizedFileName)) {
			return archive.getArchiveFileObject(normalizedFileName, null, this.charset);
		}
		return null;
	}

	@SuppressWarnings("resource") // cached archive is closed in EclipseFileManager.close()
	private Boolean containsFileObject(File archiveFile, String normalizedFileName) {
		Archive archive = getArchive(archiveFile);
		if (archive == Archive.UNKNOWN_ARCHIVE) {
			return null;
		}
		return archive.contains(normalizedFileName);
	}

	/* (non-Javadoc)
	 * @see javax.tools.JavaFileManager#getFileForOutput(javax.tools.JavaFileManager.Location, java.lang.String, java.lang.String, javax.tools.FileObject)
	 */
	@Override
	public FileObject getFileForOutput(Location location, String packageName, String relativeName, FileObject sibling)
			throws IOException {
		validateOutputLocation(location);
		Iterable<? extends File> files = getLocation(location);
		if (files == null) {
			throw new IllegalArgumentException("Unknown location : " + location);//$NON-NLS-1$
		}
		final Iterator<? extends File> iterator = files.iterator();
		if (iterator.hasNext()) {
			File file = iterator.next();
			String normalizedFileName = normalized(packageName) + '/' + relativeName.replace('\\', '/');
			File f = new File(file, normalizedFileName);
			return new EclipseFileObject(packageName + File.separator + relativeName, f.toURI(), getKind(f), this.charset);
		} else {
			throw new IllegalArgumentException("location is empty : " + location);//$NON-NLS-1$
		}
	}

	/* (non-Javadoc)
	 * @see javax.tools.JavaFileManager#getJavaFileForInput(javax.tools.JavaFileManager.Location, java.lang.String, javax.tools.JavaFileObject.Kind)
	 */
	@Override
	public JavaFileObject getJavaFileForInput(Location location, String className, Kind kind) throws IOException {
		validateNonModuleLocation(location);
		if (kind != Kind.CLASS && kind != Kind.SOURCE) {
			throw new IllegalArgumentException("Invalid kind : " + kind);//$NON-NLS-1$
		}
		Iterable<? extends File> files = getLocation(location);
		if (files == null) {
			throw new IllegalArgumentException("Unknown location : " + location);//$NON-NLS-1$
		}
		String normalizedFileName = normalized(className);
		normalizedFileName += kind.extension;
		for (File file : files) {
			if (file.equals(this.jrtHome)) {
				String modName;
				if (location instanceof ModuleLocationWrapper) {
					modName = ((ModuleLocationWrapper) location).modName;
				} else {
					modName = ""; //$NON-NLS-1$
				}
				return this.jrtSystem.getArchiveFileObject(normalizedFileName, modName, this.charset);
			} else  if (file.isDirectory()) {
				// handle directory
				File f = new File(file, normalizedFileName);
				if (f.exists()) {
					return new EclipseFileObject(className, f.toURI(), kind, this.charset);
				} else {
					continue; // go to next entry in the location
				}
			} else if (isArchive(file)) {
				// handle archive file
				ArchiveFileObject fileObject = getFileObject(file, normalizedFileName);
				if (fileObject!=null) {
					return fileObject;
				}
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see javax.tools.JavaFileManager#getJavaFileForOutput(javax.tools.JavaFileManager.Location, java.lang.String, javax.tools.JavaFileObject.Kind, javax.tools.FileObject)
	 */
	@Override
	public JavaFileObject getJavaFileForOutput(Location location, String className, Kind kind, FileObject sibling)
			throws IOException {
		validateOutputLocation(location);
		if (kind != Kind.CLASS && kind != Kind.SOURCE) {
			throw new IllegalArgumentException("Invalid kind : " + kind);//$NON-NLS-1$
		}
		Iterable<? extends File> files = getLocation(location);
		if (files == null) {
			if (!location.equals(StandardLocation.CLASS_OUTPUT)
					&& !location.equals(StandardLocation.SOURCE_OUTPUT))
				throw new IllegalArgumentException("Unknown location : " + location);//$NON-NLS-1$
			// we will use either the sibling or user.dir
			if (sibling != null) {
				String normalizedFileName = normalized(className);
				int index = normalizedFileName.lastIndexOf('/');
				if (index != -1) {
					normalizedFileName = normalizedFileName.substring(index + 1);
				}
				normalizedFileName += kind.extension;
				URI uri = sibling.toUri();
				URI uri2 = null;
				try {
					String path = uri.getPath();
					index = path.lastIndexOf('/');
					if (index != -1) {
						path = path.substring(0, index + 1);
						path += normalizedFileName;
					}
					uri2 = new URI(uri.getScheme(), uri.getHost(), path, uri.getFragment());
				} catch (URISyntaxException e) {
					throw new IllegalArgumentException("invalid sibling", e);//$NON-NLS-1$
				}
				return new EclipseFileObject(className, uri2, kind, this.charset);
			} else {
				String normalizedFileName = normalized(className);
				normalizedFileName += kind.extension;
				File f = new File(System.getProperty("user.dir"), normalizedFileName);//$NON-NLS-1$
				return new EclipseFileObject(className, f.toURI(), kind, this.charset);
			}
		}
		final Iterator<? extends File> iterator = files.iterator();
		if (iterator.hasNext()) {
			File file = iterator.next();
			String normalizedFileName = normalized(className);
			normalizedFileName += kind.extension;
			File f = new File(file, normalizedFileName);
			return new EclipseFileObject(className, f.toURI(), kind, this.charset);
		} else {
			throw new IllegalArgumentException("location is empty : " + location);//$NON-NLS-1$
		}
	}

	/* (non-Javadoc)
	 * @see javax.tools.StandardJavaFileManager#getJavaFileObjects(java.io.File[])
	 */
	@Override
	public Iterable<? extends JavaFileObject> getJavaFileObjects(File... files) {
		return getJavaFileObjectsFromFiles(Arrays.asList(files));
	}

	/* (non-Javadoc)
	 * @see javax.tools.StandardJavaFileManager#getJavaFileObjects(java.lang.String[])
	 */
	@Override
	public Iterable<? extends JavaFileObject> getJavaFileObjects(String... names) {
		return getJavaFileObjectsFromStrings(Arrays.asList(names));
	}

	/* (non-Javadoc)
	 * @see javax.tools.StandardJavaFileManager#getJavaFileObjectsFromFiles(java.lang.Iterable)
	 */
	@Override
	public Iterable<? extends JavaFileObject> getJavaFileObjectsFromFiles(Iterable<? extends File> files) {
		ArrayList<JavaFileObject> javaFileArrayList = new ArrayList<>();
		for (File f : files) {
			if (f.isDirectory()) {
				throw new IllegalArgumentException("file : " + f.getAbsolutePath() + " is a directory"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			javaFileArrayList.add(new EclipseFileObject(f.getAbsolutePath(), f.toURI(), getKind(f), this.charset));
		}
		return javaFileArrayList;
	}

	/* (non-Javadoc)
	 * @see javax.tools.StandardJavaFileManager#getJavaFileObjectsFromStrings(java.lang.Iterable)
	 */
	@Override
	public Iterable<? extends JavaFileObject> getJavaFileObjectsFromStrings(Iterable<String> names) {
		ArrayList<File> files = new ArrayList<>();
		for (String name : names) {
			files.add(new File(name));
		}
		return getJavaFileObjectsFromFiles(files);
	}

	public Kind getKind(File f) {
		return getKind(getExtension(f));
	}

	private Kind getKind(String extension) {
		if (Kind.CLASS.extension.equals(extension)) {
			return Kind.CLASS;
		} else if (Kind.SOURCE.extension.equals(extension)) {
			return Kind.SOURCE;
		} else if (Kind.HTML.extension.equals(extension)) {
			return Kind.HTML;
		}
		return Kind.OTHER;
	}

	/* (non-Javadoc)
	 * @see javax.tools.StandardJavaFileManager#getLocation(javax.tools.JavaFileManager.Location)
	 */
	@Override
	public Iterable<? extends File> getLocation(Location location) {
		if (location instanceof LocationWrapper) {
			return getFiles(((LocationWrapper) location).paths);
		}
		LocationWrapper loc = this.locationHandler.getLocation(location);
		if (loc == null) {
			return null;
		}
		return getFiles(loc.getPaths());
	}

	private Iterable<? extends File> getOutputDir(String string) {
		if ("none".equals(string)) {//$NON-NLS-1$
			return null;
		}
		File file = new File(string);
		if (file.exists() && !file.isDirectory()) {
			throw new IllegalArgumentException("file : " + file.getAbsolutePath() + " is not a directory");//$NON-NLS-1$//$NON-NLS-2$
		}
		ArrayList<File> list = new ArrayList<>(1);
		list.add(file);
		return list;
	}

	/* (non-Javadoc)
	 * @see javax.tools.JavaFileManager#handleOption(java.lang.String, java.util.Iterator)
	 */
	@Override
	public boolean handleOption(String current, Iterator<String> remaining) {
		try {
			switch(current) {
				case "-bootclasspath": //$NON-NLS-1$
					if (remaining.hasNext()) {
						final Iterable<? extends File> bootclasspaths = getPathsFrom(remaining.next());
						if (bootclasspaths != null) {
							Iterable<? extends File> iterable = getLocation(StandardLocation.PLATFORM_CLASS_PATH);
							if ((this.flags & EclipseFileManager.HAS_ENDORSED_DIRS) == 0
									&& (this.flags & EclipseFileManager.HAS_EXT_DIRS) == 0) {
								// override default bootclasspath
								setLocation(StandardLocation.PLATFORM_CLASS_PATH, bootclasspaths);
							} else if ((this.flags & EclipseFileManager.HAS_ENDORSED_DIRS) != 0) {
								// endorseddirs have been processed first
								setLocation(StandardLocation.PLATFORM_CLASS_PATH,
										concatFiles(iterable, bootclasspaths));
							} else {
								// extdirs have been processed first
								setLocation(StandardLocation.PLATFORM_CLASS_PATH,
										prependFiles(iterable, bootclasspaths));
							}
						}
						this.flags |= EclipseFileManager.HAS_BOOTCLASSPATH;
						return true;
					} else {
						throw new IllegalArgumentException();
					}
				case "--system": //$NON-NLS-1$
					if (remaining.hasNext()) {
						final Iterable<? extends File> classpaths = getPathsFrom(remaining.next());
						if (classpaths != null) {
							Iterable<? extends File> iterable = getLocation(StandardLocation.SYSTEM_MODULES);
							if (iterable != null) {
								setLocation(StandardLocation.SYSTEM_MODULES, concatFiles(iterable, classpaths));
							} else {
								setLocation(StandardLocation.SYSTEM_MODULES, classpaths);
							}
						}
						return true;
					} else {
						throw new IllegalArgumentException();
					}
				case "--upgrade-module-path": //$NON-NLS-1$
					if (remaining.hasNext()) {
						final Iterable<? extends File> classpaths = getPathsFrom(remaining.next());
						if (classpaths != null) {
							Iterable<? extends File> iterable = getLocation(StandardLocation.UPGRADE_MODULE_PATH);
							if (iterable != null) {
								setLocation(StandardLocation.UPGRADE_MODULE_PATH,
									concatFiles(iterable, classpaths));
							} else {
								setLocation(StandardLocation.UPGRADE_MODULE_PATH, classpaths);
							}
						}
						return true;
					} else {
						throw new IllegalArgumentException();
					}
				case "-classpath": //$NON-NLS-1$
				case "-cp": //$NON-NLS-1$
					if (remaining.hasNext()) {
						final Iterable<? extends File> classpaths = getPathsFrom(remaining.next());
						if (classpaths != null) {
							Iterable<? extends File> iterable = getLocation(StandardLocation.CLASS_PATH);
							if (iterable != null) {
								setLocation(StandardLocation.CLASS_PATH,
									concatFiles(iterable, classpaths));
							} else {
								setLocation(StandardLocation.CLASS_PATH, classpaths);
							}
							if ((this.flags & EclipseFileManager.HAS_PROCESSORPATH) == 0) {
								setLocation(StandardLocation.ANNOTATION_PROCESSOR_PATH, classpaths);
							} else if ((this.flags & EclipseFileManager.HAS_PROC_MODULEPATH) == 0) {
								if (this.isOnJvm9)
									setLocation(StandardLocation.ANNOTATION_PROCESSOR_MODULE_PATH, classpaths);
							}
						}
						return true;
					} else {
						throw new IllegalArgumentException();
					}
				case "--module-path": //$NON-NLS-1$
				case "-p": //$NON-NLS-1$
					final Iterable<? extends File> classpaths = getPathsFrom(remaining.next());
					if (classpaths != null) {
						Iterable<? extends File> iterable = getLocation(StandardLocation.MODULE_PATH);
						if (iterable != null) {
							setLocation(StandardLocation.MODULE_PATH, concatFiles(iterable, classpaths));
						} else {
							setLocation(StandardLocation.MODULE_PATH, classpaths);
						}
						if ((this.flags & EclipseFileManager.HAS_PROCESSORPATH) == 0) {
							setLocation(StandardLocation.ANNOTATION_PROCESSOR_PATH, classpaths);
						} else if ((this.flags & EclipseFileManager.HAS_PROC_MODULEPATH) == 0) {
							if (this.isOnJvm9)
								setLocation(StandardLocation.ANNOTATION_PROCESSOR_MODULE_PATH, classpaths);
						}
					}
					return true;
				case "-encoding": //$NON-NLS-1$
					if (remaining.hasNext()) {
						this.charset = Charset.forName(remaining.next());
						return true;
					} else {
						throw new IllegalArgumentException();
					}
				case "-sourcepath": //$NON-NLS-1$
					if (remaining.hasNext()) {
						final Iterable<? extends File> sourcepaths = getPathsFrom(remaining.next());
						if (sourcepaths != null) setLocation(StandardLocation.SOURCE_PATH, sourcepaths);
						return true;
					} else {
						throw new IllegalArgumentException();
					}
				case "--module-source-path": //$NON-NLS-1$
					if (remaining.hasNext()) {
						final Iterable<? extends File> sourcepaths = getPathsFrom(remaining.next());
						if (sourcepaths != null && this.isOnJvm9)
							setLocation(StandardLocation.MODULE_SOURCE_PATH, sourcepaths);
						return true;
					} else {
						throw new IllegalArgumentException();
					}
				case "-extdirs": //$NON-NLS-1$
					if (this.isOnJvm9) {
						throw new IllegalArgumentException();
					}
					if (remaining.hasNext()) {
						Iterable<? extends File> iterable = getLocation(StandardLocation.PLATFORM_CLASS_PATH);
						setLocation(StandardLocation.PLATFORM_CLASS_PATH,
								concatFiles(iterable, getExtdirsFrom(remaining.next())));
						this.flags |= EclipseFileManager.HAS_EXT_DIRS;
						return true;
					} else {
						throw new IllegalArgumentException();
					}
				case "-endorseddirs": //$NON-NLS-1$
					if (remaining.hasNext()) {
						Iterable<? extends File> iterable = getLocation(StandardLocation.PLATFORM_CLASS_PATH);
						setLocation(StandardLocation.PLATFORM_CLASS_PATH,
								prependFiles(iterable, getEndorsedDirsFrom(remaining.next())));
						this.flags |= EclipseFileManager.HAS_ENDORSED_DIRS;
						return true;
					} else {
						throw new IllegalArgumentException();
					}
				case "-d": //$NON-NLS-1$
					if (remaining.hasNext()) {
						final Iterable<? extends File> outputDir = getOutputDir(remaining.next());
						if (outputDir != null) {
							setLocation(StandardLocation.CLASS_OUTPUT, outputDir);
						}
						return true;
					} else {
						throw new IllegalArgumentException();
					}
				case "-s": //$NON-NLS-1$
					if (remaining.hasNext()) {
						final Iterable<? extends File> outputDir = getOutputDir(remaining.next());
						if (outputDir != null) {
							setLocation(StandardLocation.SOURCE_OUTPUT, outputDir);
						}
						return true;
					} else {
						throw new IllegalArgumentException();
					}
				case "-processorpath": //$NON-NLS-1$
					if (remaining.hasNext()) {
						final Iterable<? extends File> processorpaths = getPathsFrom(remaining.next());
						if (processorpaths != null) {
							setLocation(StandardLocation.ANNOTATION_PROCESSOR_PATH, processorpaths);
						}
						this.flags |= EclipseFileManager.HAS_PROCESSORPATH;
						return true;
					} else {
						throw new IllegalArgumentException();
					}
				case "--processor-module-path": //$NON-NLS-1$
					if (remaining.hasNext()) {
						final Iterable<? extends File> processorpaths = getPathsFrom(remaining.next());
						if (processorpaths != null && this.isOnJvm9) {
							setLocation(StandardLocation.ANNOTATION_PROCESSOR_MODULE_PATH, processorpaths);
							this.flags |= EclipseFileManager.HAS_PROC_MODULEPATH;
						}
						return true;
					} else {
						throw new IllegalArgumentException();
					}
				case "--release": //$NON-NLS-1$
					if (remaining.hasNext()) {
						this.releaseVersion = remaining.next();
						return true;
					} else {
						throw new IllegalArgumentException();
					}
			}
		} catch (IOException e) {
			// ignore
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see javax.tools.JavaFileManager#hasLocation(javax.tools.JavaFileManager.Location)
	 */
	@Override
	public boolean hasLocation(Location location) {
		String mod = null;
		if (location instanceof ModuleLocationWrapper) {
			mod = ((ModuleLocationWrapper) location).modName;
		}
		LocationWrapper impl = null;
		if (mod == null) {
			impl =  this.locationHandler.getLocation(location);
		} else {
			impl = this.locationHandler.getLocation(location, mod);
		}
		return (impl != null);
	}

	/* (non-Javadoc)
	 * @see javax.tools.JavaFileManager#inferBinaryName(javax.tools.JavaFileManager.Location, javax.tools.JavaFileObject)
	 */
	@Override
	public String inferBinaryName(Location location, JavaFileObject file) {
		validateNonModuleLocation(location);
		Iterable<? extends Path> paths = getLocationAsPaths(location);
		if (paths == null) {
			return null;
		}
		if (file instanceof JrtFileObject) {
			Path filePath = ((JrtFileObject) file).path;
			filePath = filePath.subpath(2, filePath.getNameCount());
			String name = filePath.toString();
			int index = name.lastIndexOf('.');
			if (index != -1) {
				name = name.substring(0, index);
			}
			return name.replace('/', '.');
		}
		String name = file.getName();
		JavaFileObject javaFileObject = null;
		int index = name.lastIndexOf('.');
		if (index != -1) {
			name = name.substring(0, index);
		}
		try {
			javaFileObject = getJavaFileForInput(location, name, file.getKind());
		} catch (IOException e) {
			// ignore
		} catch (IllegalArgumentException iae) {
			return null; // Either unknown kind or location not present
		}
		if (javaFileObject == null) {
			return null;
		}
		return name.replace('/', '.');
	}

	private boolean isArchive(File f) {
		if (isJrt(f))
			return false;
		String extension = getExtension(f);
		return extension.equalsIgnoreCase(".jar") || extension.equalsIgnoreCase(".zip"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private boolean isJrt(File f) {
		return f.getName().toLowerCase().equals(JrtFileSystem.BOOT_MODULE);
	}

	/* (non-Javadoc)
	 * @see javax.tools.StandardJavaFileManager#isSameFile(javax.tools.FileObject, javax.tools.FileObject)
	 */
	@Override
	public boolean isSameFile(FileObject fileObject1, FileObject fileObject2) {
		// EclipseFileManager creates only EcliseFileObject
		if (!(fileObject1 instanceof EclipseFileObject)) throw new IllegalArgumentException("Unsupported file object class : " + fileObject1.getClass());//$NON-NLS-1$
		if (!(fileObject2 instanceof EclipseFileObject)) throw new IllegalArgumentException("Unsupported file object class : " + fileObject2.getClass());//$NON-NLS-1$
		return fileObject1.equals(fileObject2);
	}
	/* (non-Javadoc)
	 * @see javax.tools.OptionChecker#isSupportedOption(java.lang.String)
	 */
	@Override
	public int isSupportedOption(String option) {
		return Options.processOptionsFileManager(option);
	}

	/* (non-Javadoc)
	 * @see javax.tools.JavaFileManager#list(javax.tools.JavaFileManager.Location, java.lang.String, java.util.Set, boolean)
	 */
	@Override
	public Iterable<JavaFileObject> list(Location location, String packageName, Set<Kind> kinds, boolean recurse)
			throws IOException {
		validateNonModuleLocation(location);
		Iterable<? extends Path> allPaths = getLocationAsPaths(location);
		if (allPaths == null) {
			throw new IllegalArgumentException("Unknown location : " + location);//$NON-NLS-1$
		}

		ArrayList<JavaFileObject> collector = new ArrayList<>();
		String normalizedPackageName = normalized(packageName);
		for (Path file : allPaths) {
			collectAllMatchingFiles(location, file.toFile(), normalizedPackageName, kinds, recurse, collector);
		}
		return collector;
	}

	private String normalized(String className) {
		char[] classNameChars = className.toCharArray();
		for (int i = 0, max = classNameChars.length; i < max; i++) {
			switch(classNameChars[i]) {
				case '\\' :
					classNameChars[i] = '/';
					break;
				case '.' :
					classNameChars[i] = '/';
			}
		}
		return new String(classNameChars);
	}

	private Iterable<? extends File> prependFiles(Iterable<? extends File> iterable,
			Iterable<? extends File> iterable2) {
		if (iterable2 == null) return iterable;
		ArrayList<File> list = new ArrayList<>();
		for (Iterator<? extends File> iterator = iterable2.iterator(); iterator.hasNext(); ) {
			list.add(iterator.next());
		}
		if (iterable != null) {
			for (Iterator<? extends File> iterator = iterable.iterator(); iterator.hasNext(); ) {
				list.add(iterator.next());
			}
		}
		return list;
	}
	private boolean isRunningJvm9() {
		return (SourceVersion.latest().compareTo(SourceVersion.RELEASE_8) > 0);
	}
	/* (non-Javadoc)
	 * @see javax.tools.StandardJavaFileManager#setLocation(javax.tools.JavaFileManager.Location, java.lang.Iterable)
	 */
	@Override
	public void setLocation(Location location, Iterable<? extends File> files) throws IOException {
		if (location.isOutputLocation() && files != null) {
			// output location
			int count = 0;
			for (Iterator<? extends File> iterator = files.iterator(); iterator.hasNext(); ) {
				iterator.next();
				count++;
			}
			if (count != 1) {
				throw new IllegalArgumentException("output location can only have one path");//$NON-NLS-1$
			}
		}
		this.locationHandler.setLocation(location, getPaths(files));
	}

	public void setLocale(Locale locale) {
		this.locale = locale == null ? Locale.getDefault() : locale;
		try {
			this.bundle = ResourceBundleFactory.getBundle(this.locale);
		} catch(MissingResourceException e) {
			System.out.println("Missing resource : " + Main.bundleName.replace('.', '/') + ".properties for locale " + locale); //$NON-NLS-1$//$NON-NLS-2$
			throw e;
		}
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public void processPathEntries(final int defaultSize, final ArrayList paths,
			final String currentPath, String customEncoding, boolean isSourceOnly,
			boolean rejectDestinationPathOnJars) {

		String currentClasspathName = null;
		String currentDestinationPath = null;
		ArrayList currentRuleSpecs = new ArrayList(defaultSize);
		StringTokenizer tokenizer = new StringTokenizer(currentPath,
				File.pathSeparator + "[]", true); //$NON-NLS-1$
		ArrayList tokens = new ArrayList();
		while (tokenizer.hasMoreTokens()) {
			tokens.add(tokenizer.nextToken());
		}
		// state machine
		final int start = 0;
		final int readyToClose = 1;
		// 'path' 'path1[rule];path2'
		final int readyToCloseEndingWithRules = 2;
		// 'path[rule]' 'path1;path2[rule]'
		final int readyToCloseOrOtherEntry = 3;
		// 'path[rule];' 'path;' 'path1;path2;'
		final int rulesNeedAnotherRule = 4;
		// 'path[rule1;'
		final int rulesStart = 5;
		// 'path[' 'path1;path2['
		final int rulesReadyToClose = 6;
		// 'path[rule' 'path[rule1;rule2'
		final int destinationPathReadyToClose = 7;
		// 'path[-d bin'
		final int readyToCloseEndingWithDestinationPath = 8;
		// 'path[-d bin]' 'path[rule][-d bin]'
		final int destinationPathStart = 9;
		// 'path[rule]['
		final int bracketOpened = 10;
		// '.*[.*'
		final int bracketClosed = 11;
		// '.*([.*])+'

		final int error = 99;
		int state = start;
		String token = null;
		int cursor = 0, tokensNb = tokens.size(), bracket = -1;
		while (cursor < tokensNb && state != error) {
			token = (String) tokens.get(cursor++);
			if (token.equals(File.pathSeparator)) {
				switch (state) {
				case start:
				case readyToCloseOrOtherEntry:
				case bracketOpened:
					break;
				case readyToClose:
				case readyToCloseEndingWithRules:
				case readyToCloseEndingWithDestinationPath:
					state = readyToCloseOrOtherEntry;
					addNewEntry(paths, currentClasspathName, currentRuleSpecs,
							customEncoding, currentDestinationPath, isSourceOnly,
							rejectDestinationPathOnJars);
					currentRuleSpecs.clear();
					break;
				case rulesReadyToClose:
					state = rulesNeedAnotherRule;
					break;
				case destinationPathReadyToClose:
					throw new IllegalArgumentException(
							this.bind("configure.incorrectDestinationPathEntry", //$NON-NLS-1$
									currentPath));
				case bracketClosed:
					cursor = bracket + 1;
					state = rulesStart;
					break;
				default:
					state = error;
				}
			} else if (token.equals("[")) { //$NON-NLS-1$
				switch (state) {
				case start:
					currentClasspathName = ""; //$NON-NLS-1$
					//$FALL-THROUGH$
				case readyToClose:
					bracket = cursor - 1;
					//$FALL-THROUGH$
				case bracketClosed:
					state = bracketOpened;
					break;
				case readyToCloseEndingWithRules:
					state = destinationPathStart;
					break;
				case readyToCloseEndingWithDestinationPath:
					state = rulesStart;
					break;
				case bracketOpened:
				default:
					state = error;
				}
			} else if (token.equals("]")) { //$NON-NLS-1$
				switch (state) {
				case rulesReadyToClose:
					state = readyToCloseEndingWithRules;
					break;
				case destinationPathReadyToClose:
					state = readyToCloseEndingWithDestinationPath;
					break;
				case bracketOpened:
					state = bracketClosed;
					break;
				case bracketClosed:
				default:
					state = error;
				}
			} else {
				// regular word
				switch (state) {
				case start:
				case readyToCloseOrOtherEntry:
					state = readyToClose;
					currentClasspathName = token;
					break;
				case rulesStart:
					if (token.startsWith("-d ")) { //$NON-NLS-1$
						if (currentDestinationPath != null) {
							throw new IllegalArgumentException(
									this.bind("configure.duplicateDestinationPathEntry", //$NON-NLS-1$
											currentPath));
						}
						currentDestinationPath = token.substring(3).trim();
						state = destinationPathReadyToClose;
						break;
					} // else we proceed with a rule
					//$FALL-THROUGH$
				case rulesNeedAnotherRule:
					if (currentDestinationPath != null) {
						throw new IllegalArgumentException(
								this.bind("configure.accessRuleAfterDestinationPath", //$NON-NLS-1$
									currentPath));
					}
					state = rulesReadyToClose;
					currentRuleSpecs.add(token);
					break;
				case destinationPathStart:
					if (!token.startsWith("-d ")) { //$NON-NLS-1$
						state = error;
					} else {
						currentDestinationPath = token.substring(3).trim();
						state = destinationPathReadyToClose;
					}
					break;
				case bracketClosed:
					for (int i = bracket; i < cursor ; i++) {
						currentClasspathName += (String) tokens.get(i);
					}
					state = readyToClose;
					break;
				case bracketOpened:
					break;
				default:
					state = error;
				}
			}
			if (state == bracketClosed && cursor == tokensNb) {
				cursor = bracket + 1;
				state = rulesStart;
			}
		}
		switch(state) {
			case readyToCloseOrOtherEntry:
				break;
			case readyToClose:
			case readyToCloseEndingWithRules:
			case readyToCloseEndingWithDestinationPath:
				addNewEntry(paths, currentClasspathName, currentRuleSpecs,
					customEncoding, currentDestinationPath, isSourceOnly,
					rejectDestinationPathOnJars);
				break;
			case bracketOpened:
			case bracketClosed:
			default :
				// we go on anyway
		}
	}
	@SuppressWarnings({"rawtypes", "unchecked"})
	protected void addNewEntry(ArrayList paths, String currentClasspathName,
			ArrayList currentRuleSpecs, String customEncoding,
			String destPath, boolean isSourceOnly,
			boolean rejectDestinationPathOnJars) {

		int rulesSpecsSize = currentRuleSpecs.size();
		AccessRuleSet accessRuleSet = null;
		if (rulesSpecsSize != 0) {
			AccessRule[] accessRules = new AccessRule[currentRuleSpecs.size()];
			boolean rulesOK = true;
			Iterator i = currentRuleSpecs.iterator();
			int j = 0;
			while (i.hasNext()) {
				String ruleSpec = (String) i.next();
				char key = ruleSpec.charAt(0);
				String pattern = ruleSpec.substring(1);
				if (pattern.length() > 0) {
					switch (key) {
						case '+':
							accessRules[j++] = new AccessRule(pattern
									.toCharArray(), 0);
							break;
						case '~':
							accessRules[j++] = new AccessRule(pattern
									.toCharArray(),
									IProblem.DiscouragedReference);
							break;
						case '-':
							accessRules[j++] = new AccessRule(pattern
									.toCharArray(),
									IProblem.ForbiddenReference);
							break;
						case '?':
							accessRules[j++] = new AccessRule(pattern
									.toCharArray(),
									IProblem.ForbiddenReference, true/*keep looking for accessible type*/);
							break;
						default:
							rulesOK = false;
					}
				} else {
					rulesOK = false;
				}
			}
			if (rulesOK) {
	    		accessRuleSet = new AccessRuleSet(accessRules, AccessRestriction.COMMAND_LINE, currentClasspathName);
			} else {
				return;
			}
		}
		if (Main.NONE.equals(destPath)) {
			destPath = Main.NONE; // keep == comparison valid
		}
		if (rejectDestinationPathOnJars && destPath != null &&
				(currentClasspathName.endsWith(".jar") || //$NON-NLS-1$
					currentClasspathName.endsWith(".zip"))) { //$NON-NLS-1$
			throw new IllegalArgumentException(
					this.bind("configure.unexpectedDestinationPathEntryFile", //$NON-NLS-1$
								currentClasspathName));
			}
		FileSystem.Classpath currentClasspath = FileSystem.getClasspath(
				currentClasspathName,
				customEncoding,
				isSourceOnly,
				accessRuleSet,
				destPath,
				null,
				this.releaseVersion);
		if (currentClasspath != null) {
			paths.add(currentClasspath);
		}
	}
	/*
	 * Lookup the message with the given ID in this catalog and bind its
	 * substitution locations with the given string.
	 */
	private String bind(String id, String binding) {
		return bind(id, new String[] { binding });
	}

	/*
	 * Lookup the message with the given ID in this catalog and bind its
	 * substitution locations with the given string values.
	 */
	private String bind(String id, String[] arguments) {
		if (id == null)
			return "No message available"; //$NON-NLS-1$
		String message = null;
		try {
			message = this.bundle.getString(id);
		} catch (MissingResourceException e) {
			// If we got an exception looking for the message, fail gracefully by just returning
			// the id we were looking for.  In most cases this is semi-informative so is not too bad.
			return "Missing message: " + id + " in: " + Main.bundleName; //$NON-NLS-2$ //$NON-NLS-1$
		}
		return MessageFormat.format(message, (Object[]) arguments);
	}

	private Iterable<? extends File> getFiles(final Iterable<? extends Path> paths) {
		if (paths == null)
			return null;
		return () -> new Iterator<>() {
			Iterator<? extends Path> original = paths.iterator();
			@Override
			public boolean hasNext() {
				return this.original.hasNext();
			}
			@Override
			public File next() {
				return this.original.next().toFile();
			}
		};
	}
	private Iterable<? extends Path> getPaths(final Iterable<? extends File> files) {
		if (files == null)
			return null;
		return () -> new Iterator<>() {
			Iterator<? extends File> original = files.iterator();
			@Override
			public boolean hasNext() {
				return this.original.hasNext();
			}
			@Override
			public Path next() {
				return this.original.next().toPath();
			}
		};
	}

	private void validateFileObject(FileObject file) {
		// FIXME: fill-up
	}
	private void validateModuleLocation(Location location, String modName) {
		Objects.requireNonNull(location);
		if (modName == null) {
			throw new IllegalArgumentException("module must not be null"); //$NON-NLS-1$
		}
		if (this.isOnJvm9) {
			if (!location.isModuleOrientedLocation() && !location.isOutputLocation()) {
				throw new IllegalArgumentException("location is module related :" + location.getName()); //$NON-NLS-1$
			}
		}
	}
	private void validateNonModuleLocation(Location location) {
		Objects.requireNonNull(location);
		if (this.isOnJvm9) {
			if (location.isModuleOrientedLocation() && location.isOutputLocation()) {
				throw new IllegalArgumentException("location is module related :" + location.getName()); //$NON-NLS-1$
			}
		}
	}
	private void validateOutputLocation(Location location) {
		Objects.requireNonNull(location);
		if (!location.isOutputLocation()) {
			throw new IllegalArgumentException("location is not output location :" + location.getName()); //$NON-NLS-1$
		}
	}
	@Override
	public Iterable<? extends JavaFileObject> getJavaFileObjects(Path... paths) {
		return getJavaFileObjectsFromPaths(Arrays.asList(paths));
	}

	@Override
	public Iterable<? extends JavaFileObject> getJavaFileObjectsFromPaths(Iterable<? extends Path> paths) {
		return getJavaFileObjectsFromFiles(getFiles(paths));
	}

	@Override
	public Iterable<? extends Path> getLocationAsPaths(Location location) {
		if (location instanceof LocationWrapper) {
			return ((LocationWrapper) location).paths;
		}
		LocationWrapper loc = this.locationHandler.getLocation(location);
		if (loc == null) {
			return null;
		}
		return loc.getPaths();
	}

	@Override
	public void setLocationFromPaths(Location location, Collection<? extends Path> paths) throws IOException {
		setLocation(location, getFiles(paths));
		if (location == StandardLocation.MODULE_PATH || location == StandardLocation.MODULE_SOURCE_PATH) {
			// FIXME: same for module source path?
			Map<String, String> options = new HashMap<>();
			// FIXME: Find a way to get the options from the EclipseCompiler and pass it to the parser.
			String latest = CompilerOptions.getLatestVersion();
			options.put(CompilerOptions.OPTION_Compliance, latest);
			options.put(CompilerOptions.OPTION_Source, latest);
			options.put(CompilerOptions.OPTION_TargetPlatform, latest);
			CompilerOptions compilerOptions = new CompilerOptions(options);
			ProblemReporter problemReporter =
					new ProblemReporter(
						DefaultErrorHandlingPolicies.proceedWithAllProblems(),
						compilerOptions,
						new DefaultProblemFactory());
			for (Path path : paths) {
				List<Classpath> mp = ModuleFinder.findModules(path.toFile(), null,
						new Parser(problemReporter, true), null, true, this.releaseVersion);
				for (Classpath cp : mp) {
					Collection<String> moduleNames = cp.getModuleNames(null);
					for (String string : moduleNames) {
						Path p = Paths.get(cp.getPath());
						setLocationForModule(location, string,  Collections.singletonList(p));
					}
				}
			}
		}
	}

	@Override
	public boolean contains(Location location, FileObject fo) throws IOException {
		validateFileObject(fo);
		Iterable<? extends File> files = getLocation(location);
		if (files == null) {
			throw new IllegalArgumentException("Unknown location : " + location);//$NON-NLS-1$
		}
		for (File file : files) {
			if (file.isDirectory()) {
				if (fo instanceof EclipseFileObject) {
					Path filepath = ((EclipseFileObject) fo).f.toPath();
					if (filepath.startsWith(Paths.get(file.toURI()).toAbsolutePath())) {
						return true;
					}
				}
			} else if (isArchive(file)) {
				if (fo instanceof ArchiveFileObject) {
					Boolean contains = containsFileObject(file, ((ArchiveFileObject) fo).entryName);
					if (contains != null) {
						return contains;
					}
				}
			}
		}
		return false;
	}

	@Override
	public Location getLocationForModule(Location location, String moduleName) throws IOException {
		validateModuleLocation(location, moduleName);
		Location result = this.locationHandler.getLocation(location, moduleName);
		if (result == null && location == StandardLocation.CLASS_OUTPUT) {
			LocationWrapper wrapper = this.locationHandler.getLocation(StandardLocation.MODULE_SOURCE_PATH, moduleName);
			// There are cases where we don't have module source path in that case we need to create
			// classes in default location
			if (wrapper == null) {
				result = location;
			} else {
				deriveOutputLocationForModules(moduleName, wrapper.paths);
				result = getLocationForModule(location, moduleName);
			}
		} else if (result == null && location == StandardLocation.SOURCE_OUTPUT) {
			LocationWrapper wrapper = this.locationHandler.getLocation(StandardLocation.MODULE_SOURCE_PATH, moduleName);
			deriveSourceOutputLocationForModules(moduleName, wrapper.paths);
			result = getLocationForModule(location, moduleName);
		}
		return result;
	}

	@Override
	public Location getLocationForModule(Location location, JavaFileObject fo) {
		validateModuleLocation(location, ""); //$NON-NLS-1$
		Path path = null;
		if (fo instanceof ArchiveFileObject) {
			path = ((ArchiveFileObject) fo).file.toPath();
			return this.locationHandler.getLocation(location, path);
		} else if (fo instanceof EclipseFileObject) {
			path = ((EclipseFileObject) fo).f.toPath();
			try {
				path = path.toRealPath();
			} catch (IOException e) {
				e.printStackTrace();
			}
			LocationContainer container = this.locationHandler.getLocation(location);
			while (path != null) {
				Location loc = container.get(path);
				if (loc != null)
					return loc;
				path = path.getParent();
			}
		}
		return null;
	}

	@Override
	public <S> ServiceLoader<S> getServiceLoader(Location location, Class<S> service) throws IOException {
		// FIXME: Need special handling in case of module class loaders.
		return ServiceLoader.load(service, getClassLoader(location));
	}

	@Override
	public String inferModuleName(Location location) throws IOException {
		if (location instanceof ModuleLocationWrapper) {
			ModuleLocationWrapper wrapper = (ModuleLocationWrapper) location;
			return wrapper.modName;
		}
		return null;
	}

	@Override
	public Iterable<Set<Location>> listLocationsForModules(Location location) {
		validateModuleLocation(location, ""); //$NON-NLS-1$
		return this.locationHandler.listLocationsForModules(location);
	}

	@Override
	public Path asPath(FileObject file) {
		validateFileObject(file);
		EclipseFileObject eclFile = (EclipseFileObject) file;
		if (eclFile.f != null) {
			return eclFile.f.toPath();
		}
		return null;
	}
	private void deriveOutputLocationForModules(String moduleName, Collection<? extends Path> paths) {
		LocationWrapper wrapper = this.locationHandler.getLocation(StandardLocation.CLASS_OUTPUT, moduleName);
		if (wrapper == null) {
			// First get from our internally known location for legacy/unnamed location
			wrapper = this.locationHandler.getLocation(StandardLocation.CLASS_OUTPUT, ""); //$NON-NLS-1$
			if (wrapper == null) {
				wrapper = this.locationHandler.getLocation(StandardLocation.CLASS_OUTPUT);
			}
			if (wrapper != null) {
				Iterator<? extends Path> iterator = wrapper.paths.iterator();
				if (iterator.hasNext()) {
					try {
					// Per module output location is always a singleton list
					Path path = iterator.next().resolve(moduleName);
					this.locationHandler.setLocation(StandardLocation.CLASS_OUTPUT, moduleName, Collections.singletonList(path));
					} catch(Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	private void deriveSourceOutputLocationForModules(String moduleName, Collection<? extends Path> paths) {
		LocationWrapper wrapper = this.locationHandler.getLocation(StandardLocation.SOURCE_OUTPUT, moduleName);
		if (wrapper == null) {
			// First get from our internally known location for legacy/unnamed location
			wrapper = this.locationHandler.getLocation(StandardLocation.SOURCE_OUTPUT, ""); //$NON-NLS-1$
			if (wrapper == null) {
				wrapper = this.locationHandler.getLocation(StandardLocation.SOURCE_OUTPUT);
			}
			if (wrapper != null) {
				Iterator<? extends Path> iterator = wrapper.paths.iterator();
				if (iterator.hasNext()) {
					try {
					// Per module output location is always a singleton list
					Path path = iterator.next().resolve(moduleName);
					this.locationHandler.setLocation(StandardLocation.SOURCE_OUTPUT, moduleName, Collections.singletonList(path));
					} catch(Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	@Override
	public void setLocationForModule(Location location, String moduleName, Collection<? extends Path> paths) throws IOException {
		validateModuleLocation(location, moduleName);
		this.locationHandler.setLocation(location, moduleName, paths);
		if (location == StandardLocation.MODULE_SOURCE_PATH) {
			deriveOutputLocationForModules(moduleName, paths);
		}
	}
}
