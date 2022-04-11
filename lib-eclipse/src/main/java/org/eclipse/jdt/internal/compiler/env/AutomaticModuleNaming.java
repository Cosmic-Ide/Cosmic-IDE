/*******************************************************************************
 * Copyright (c) 2017 Till Brychcy and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Till Brychcy - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.env;

import java.io.File;
import java.io.IOException;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class AutomaticModuleNaming {

	private static final String AUTOMATIC_MODULE_NAME = "Automatic-Module-Name"; //$NON-NLS-1$

	/**
	 * Determine the automatic module name of a given jar as specified in {@link <a href=
	 * "http://download.java.net/java/jdk9/docs/api/java/lang/module/ModuleFinder.html#of-java.nio.file.Path...-">
	 * ModuleFinder.of</a>}
	 */
	public static char[] determineAutomaticModuleName(final String jarFileName) {
		// "If the JAR file has the attribute "Automatic-Module-Name" in its main manifest then its value is the
		// module name."
		try (JarFile jar = new JarFile(jarFileName)) {
			Manifest manifest = jar.getManifest();
			if (manifest != null) {
				String automaticModuleName = manifest.getMainAttributes().getValue(AUTOMATIC_MODULE_NAME);
				if (automaticModuleName != null) {
					return automaticModuleName.toCharArray();
				}
			}
		} catch (IOException e) {
			// ignore
		}
		// The module name is otherwise derived from the name of the JAR file.
		return determineAutomaticModuleNameFromFileName(jarFileName, true, true);
	}

	/**
	 * Determine the automatic module name of a given jar or project as specified in {@link <a href=
	 * "http://download.java.net/java/jdk9/docs/api/java/lang/module/ModuleFinder.html#of-java.nio.file.Path...-">
	 * ModuleFinder.of</a>}
	 * @param fileName names either a jar file or a java project in the workspace
	 * @param isFile <code>true</code> indicates that fileName denotes a file, <code>false</code> must be used for projects
	 * @param manifest representation of the META-INF/MANIFEST.MF entry within the given source (jar or project), or <code>null</code>
	 * @return the derived module name or <code>null</code>
	 */
	public static char[] determineAutomaticModuleName(final String fileName, boolean isFile, Manifest manifest) {
		if (manifest != null) {
			String automaticModuleName = manifest.getMainAttributes().getValue(AUTOMATIC_MODULE_NAME);
			if (automaticModuleName != null) {
				return automaticModuleName.toCharArray();
			}
		}
		return determineAutomaticModuleNameFromFileName(fileName, true, isFile);
	}

	/**
	 * Determine the automatic module name of a given jar or project as defined by an Automatic-Module-Name
	 * header in its manifest.
	 * @param manifest representation of the META-INF/MANIFEST.MF entry within the given source (jar or project), or <code>null</code>
	 * @return the derived module name or <code>null</code>
	 */
	public static char[] determineAutomaticModuleNameFromManifest(Manifest manifest) {
		if (manifest != null) {
			String automaticModuleName = manifest.getMainAttributes().getValue(AUTOMATIC_MODULE_NAME);
			if (automaticModuleName != null) {
				return automaticModuleName.toCharArray();
			}
		}
		return null;
	}

	/**
	 * Determine the automatic module name if no "Automatic-Module-Name" was found in the Manifest, as specified in
	 * {@link <a href=
	 * "http://download.java.net/java/jdk9/docs/api/java/lang/module/ModuleFinder.html#of-java.nio.file.Path...-">ModuleFinder.of</a>}
	 *
	 * @param name
	 *            the filename (or directory name)
	 * @param skipDirectory
	 *            if true, parent directory names are skipped
	 * @param removeExtension
	 *            if true, the ".jar" extension is removed.
	 */
	public static char[] determineAutomaticModuleNameFromFileName(String name, boolean skipDirectory,
			boolean removeExtension) {
		int index;
		int start = 0;
		int end = name.length();
		if (skipDirectory) {
			index = name.lastIndexOf(File.separatorChar);
			start = index + 1;
		}

		// "The ".jar" suffix is removed"
		if (removeExtension) {
			if (name.endsWith(".jar") || name.endsWith(".JAR")) { //$NON-NLS-1$//$NON-NLS-2$
				end -= 4;
			}
		}

		// "If the name matches the regular expression "-(\\d+(\\.|$))" then the module name will be derived from the
		// subsequence preceding the hyphen of the first occurrence. [...]"
		dashLoop: for (index = start; index < end - 1; index++) {
			if (name.charAt(index) == '-' && name.charAt(index + 1) >= '0' && name.charAt(index + 1) <= '9') {
				for (int index2 = index + 2; index2 < end; index2++) {
					final char c = name.charAt(index2);
					if (c == '.') {
						break;
					}
					if (c < '0' || c > '9') {
						continue dashLoop;
					}
				}
				end = index;
				break;
			}
		}

		// "All non-alphanumeric characters ([^A-Za-z0-9]) in the module name are replaced with a dot ("."), all
		// repeating dots are replaced with one dot, and all leading and trailing dots are removed."
		StringBuilder sb = new StringBuilder(end - start);
		boolean needDot = false;
		for (int i = start; i < end; i++) {
			char c = name.charAt(i);
			if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
				if (needDot) {
					sb.append('.');
					needDot = false;
				}
				sb.append(c);
			} else {
				if (sb.length() > 0) {
					needDot = true;
				}
			}
		}
		return sb.toString().toCharArray();
	}

}
