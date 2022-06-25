/*******************************************************************************
 * Copyright (c) 2006, 2015 IBM Corporation and others.
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

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/**
 * Used as a zip file cache.
 */
public class Archive implements Closeable {

	public static final Archive UNKNOWN_ARCHIVE = new Archive();

	ZipFile zipFile;
	File file;

	protected Hashtable<String, ArrayList<String[]>> packagesCache;

	protected Archive() {
		// used to construct UNKNOWN_ARCHIVE
	}

	public Archive(File file) throws ZipException, IOException {
		this.file = file;
		this.zipFile = new ZipFile(file);
		initialize();
	}

	private void initialize() {
		// initialize packages
		this.packagesCache = new Hashtable<>();
		for (Enumeration<? extends ZipEntry> e = this.zipFile.entries(); e.hasMoreElements(); ) {
			String fileName = ((ZipEntry) e.nextElement()).getName();

			// add the package name & all of its parent packages
			int last = fileName.lastIndexOf('/');
			// extract the package name
			String packageName = fileName.substring(0, last + 1);
			String typeName = fileName.substring(last + 1);
			// might be empty if this is a directory entry
			if (typeName.length() == 0) {
				continue;
			}
			cacheTypes(packageName, typeName);
		}
	}

	protected void cacheTypes(String packageName, String typeName) {
		ArrayList<String[]> types = this.packagesCache.get(packageName);
		if (typeName == null) return;
		if (types == null) {

			types = new ArrayList<>();
			types.add(new String[]{typeName, null});
			this.packagesCache.put(packageName, types);
		} else {
			types.add(new String[]{typeName, null});
		}
	}

	public ArchiveFileObject getArchiveFileObject(String fileName, String module, Charset charset) {
		return new ArchiveFileObject(this.file, fileName, charset);
	}

	public boolean contains(String entryName) {
		return this.zipFile.getEntry(entryName) != null;
	}

	public Set<String> allPackages() {
		if (this.packagesCache == null) {
			this.initialize();
		}
		return this.packagesCache.keySet();
	}

	/**
	 * Returns an array of String - the array contains exactly two elements. The first element
	 * is the name of the type and the second being the module that contains the type. For a regular
	 * Jar archive, the module element will be null. This is applicable only to Jimage files
	 * where types are contained by multiple modules.
	 */
	public List<String[]> getTypes(String packageName) {
		// package name is expected to ends with '/'
		if (this.packagesCache == null) {
			try {
				this.zipFile = new ZipFile(this.file);
			} catch(IOException e) {
				return Collections.<String[]>emptyList();
			}
			this.initialize();
		}
		return this.packagesCache.get(packageName);
	}

	public void flush() {
		this.packagesCache = null;
	}

	@Override
	public void close() {
		this.packagesCache = null;
		try {
			if (this.zipFile != null) {
				this.zipFile.close();
			}
		} catch (IOException e) {
			// ignore
		}
	}

	@Override
	public String toString() {
		return "Archive: " + (this.file == null ? "UNKNOWN_ARCHIVE" : this.file.getAbsolutePath()); //$NON-NLS-1$ //$NON-NLS-2$
	}
}