/*******************************************************************************
 * Copyright (c) 2016, 2020 IBM Corporation.
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
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.batch.FileSystem.Classpath;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFormatException;
import org.eclipse.jdt.internal.compiler.env.IModule;
import org.eclipse.jdt.internal.compiler.env.PackageExportImpl;
import org.eclipse.jdt.internal.compiler.env.IModule.IPackageExport;
import org.eclipse.jdt.internal.compiler.parser.Parser;
import org.eclipse.jdt.internal.compiler.util.Util;

public class ModuleFinder {

	public static List<FileSystem.Classpath> findModules(File f, String destinationPath, Parser parser, Map<String, String> options, boolean isModulepath, String release) {
		List<FileSystem.Classpath> collector = new ArrayList<>();
		scanForModules(destinationPath, parser, options, isModulepath, false, collector, f, release);
		return collector;
	}

	protected static FileSystem.Classpath findModule(final File file, String destinationPath, Parser parser,
			Map<String, String> options, boolean isModulepath, String release) {
		FileSystem.Classpath modulePath = FileSystem.getClasspath(file.getAbsolutePath(), null, !isModulepath, null,
				destinationPath == null ? null : (destinationPath + File.separator + file.getName()), options, release);
		if (modulePath != null) {
			scanForModule(modulePath, file, parser, isModulepath, release);
		}
		return modulePath;
	}
	protected static void scanForModules(String destinationPath, Parser parser, Map<String, String> options, boolean isModulepath,
			boolean thisAnAutomodule, List<FileSystem.Classpath> collector, final File file, String release) {
		FileSystem.Classpath entry = FileSystem.getClasspath(
				file.getAbsolutePath(),
				null,
				!isModulepath,
				null,
				destinationPath == null ? null : (destinationPath + File.separator + file.getName()),
				options,
				release);
		if (entry != null) {
			IModule module = scanForModule(entry, file, parser, thisAnAutomodule, release);
			if (module != null) {
				collector.add(entry);
			} else {
				if (file.isDirectory()) {
					File[] files = file.listFiles();
					for (File f : files) {
						scanForModules(destinationPath, parser, options, isModulepath, isModulepath, collector, f, release);
					}
				}
			}
		}
	}
	protected static IModule scanForModule(FileSystem.Classpath modulePath, final File file, Parser parser, boolean considerAutoModules, String release) {
		IModule module = null;
		if (file.isDirectory()) {
			String[] list = file.list(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					if (dir == file && (name.equalsIgnoreCase(IModule.MODULE_INFO_CLASS)
							|| name.equalsIgnoreCase(IModule.MODULE_INFO_JAVA))) {
						return true;
					}
					return false;
				}
			});
			if (list.length > 0) {
				String fileName = list[0];
				switch (fileName) {
					case IModule.MODULE_INFO_CLASS:
						module = ModuleFinder.extractModuleFromClass(new File(file, fileName), modulePath);
						break;
					case IModule.MODULE_INFO_JAVA:
						module = ModuleFinder.extractModuleFromSource(new File(file, fileName), parser, modulePath);
						if (module == null)
							return null;
						String modName = new String(module.name());
						if (!modName.equals(file.getName())) {
							throw new IllegalArgumentException("module name " + modName + " does not match expected name " + file.getName()); //$NON-NLS-1$ //$NON-NLS-2$
						}
						break;
				}
			}
		} else {
			String moduleDescPath = getModulePathForArchive(file);
			if (moduleDescPath != null) {
				module = extractModuleFromArchive(file, modulePath, moduleDescPath, release);
			}
		}
		if (considerAutoModules && module == null && !(modulePath instanceof ClasspathJrt)) {
			if (!file.isDirectory()) {
				String fileName = getFileName(file);
				if (!fileName.isEmpty())
					module = IModule.createAutomatic(fileName, file.isFile(), getManifest(file));
			}
		}
		if (module != null)
			modulePath.acceptModule(module);
		return module;
	}
	private static Manifest getManifest(File file) {
		if (getModulePathForArchive(file) == null)
			return null;
		try (JarFile jar = new JarFile(file)) {
			return jar.getManifest();
		} catch (IOException e) {
			return null;
		}
	}
	private static String getFileName(File file) {
		String name = file.getName();
		int index = name.lastIndexOf('.');
		if (index == -1)
			return name;
		return name.substring(0, index);
	}
	/**
	 * Extracts the single reads clause from the given
	 * command line option (--add-reads). The result is a String[] with two
	 * element, first being the source module and second being the target module.
	 * The expected format is:
	 *  --add-reads <source-module>=<target-module>
	 * @param option
	 * @return a String[] with source and target module of the "reads" clause.
	 */
	protected static String[] extractAddonRead(String option) {
		StringTokenizer tokenizer = new StringTokenizer(option, "="); //$NON-NLS-1$
		String source = null;
		String target = null;
		if (tokenizer.hasMoreTokens()) {
			source = tokenizer.nextToken();
		} else {
			// Handle error
			return null;
		}
		if (tokenizer.hasMoreTokens()) {
			target = tokenizer.nextToken();
		} else {
			// Handle error
			return null;
		}
 		return new String[]{source, target};
	}
	/**
	 * Simple structure representing one <code>--add-exports</code> value.
	 */
	static class AddExport {
		/** the name of the exporting module. */
		public final String sourceModuleName;
		/** the export structure */
		public final IModule.IPackageExport export;
		public AddExport(String moduleName, IPackageExport export) {
			this.sourceModuleName = moduleName;
			this.export = export;
		}
	}
	/**
	 * Parses the --add-exports command line option and returns the package export definitions.
	 *
	 * <p>
	 * The expected format is:
	 * </p>
	 * <p>
	 * {@code
	 *   --add-exports <source-module>/<package>=<target-module>(,<target-module>)*
	 * }
	 * </p>
	 * @param option the option to parse
	 * @return an {@link AddExport} structure.
	 */
	protected static AddExport extractAddonExport(String option) {
		StringTokenizer tokenizer = new StringTokenizer(option, "/"); //$NON-NLS-1$
		String source = null;
		String pack = null;
		List<String> targets = new ArrayList<>();
		if (tokenizer.hasMoreTokens()) {
			source = tokenizer.nextToken("/"); //$NON-NLS-1$
		} else {
			// Handle error
			return null;
		}
		if (tokenizer.hasMoreTokens()) {
			pack = tokenizer.nextToken("/="); //$NON-NLS-1$
		} else {
			// Handle error
			return null;
		}
		while (tokenizer.hasMoreTokens()) {
			targets.add(tokenizer.nextToken("=,")); //$NON-NLS-1$
		}
		PackageExportImpl export = new PackageExportImpl();
		export.pack = pack.toCharArray();
		export.exportedTo = new char[targets.size()][];
		for(int i = 0; i < export.exportedTo.length; i++) {
			export.exportedTo[i] = targets.get(i).toCharArray();
		}
		return new AddExport(source, export);
	}

	private static String getModulePathForArchive(File file) {
		int format = Util.archiveFormat(file.getAbsolutePath());
		if (format == Util.ZIP_FILE) {
			return IModule.MODULE_INFO_CLASS;
		} else if(format == Util.JMOD_FILE) {
			return "classes/" + IModule.MODULE_INFO_CLASS; //$NON-NLS-1$
		}
		return null;
	}
	private static IModule extractModuleFromArchive(File file, Classpath pathEntry, String path, String release) {
		ZipFile zipFile = null;
		try {
			zipFile = new ZipFile(file);
			if (release != null) {
				String releasePath = "META-INF/versions/" + release + "/" + path; //$NON-NLS-1$ //$NON-NLS-2$
				ZipEntry entry = zipFile.getEntry(releasePath);
				if (entry != null) {
					path = releasePath;
				}
			}
			ClassFileReader reader = ClassFileReader.read(zipFile, path);
			IModule module = getModule(reader);
			if (module != null) {
				return reader.getModuleDeclaration();
			}
			return null;
		} catch (ClassFormatException | IOException e) {
			// Nothing to be done here
		} finally {
			if (zipFile != null) {
				try {
					zipFile.close();
				} catch (IOException e) {
					// Nothing much to do here
				}
			}
		}
		return null;
	}
	private static IModule extractModuleFromClass(File classfilePath, Classpath pathEntry) {
		ClassFileReader reader;
		try {
			reader = ClassFileReader.read(classfilePath);
			IModule module =  getModule(reader);
			if (module != null) {
				return reader.getModuleDeclaration();
			}
			return null;
		} catch (ClassFormatException | IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	private static IModule getModule(ClassFileReader classfile) {
		if (classfile != null) {
			return classfile.getModuleDeclaration();
		}
		return null;
	}
	private static IModule extractModuleFromSource(File file, Parser parser, Classpath pathEntry) {
		CompilationUnit cu = new CompilationUnit(null, file.getAbsolutePath(), null, pathEntry.getDestinationPath());
		CompilationResult compilationResult = new CompilationResult(cu, 0, 1, 10);
		CompilationUnitDeclaration unit = parser.parse(cu, compilationResult);
		if (unit.isModuleInfo() && unit.moduleDeclaration != null) {
			cu.module = unit.moduleDeclaration.moduleName;
			return new BasicModule(unit.moduleDeclaration, pathEntry);
		}
		return null;
	}
}
