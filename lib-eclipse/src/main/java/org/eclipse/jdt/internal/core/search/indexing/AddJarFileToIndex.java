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
 *******************************************************************************/
package org.eclipse.jdt.internal.core.search.indexing;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipError;
import java.util.zip.ZipFile;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.internal.compiler.env.AutomaticModuleNaming;
import org.eclipse.jdt.internal.compiler.lookup.TypeConstants;
import org.eclipse.jdt.internal.compiler.util.SimpleLookupTable;
import org.eclipse.jdt.internal.compiler.util.Util;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.jdt.internal.core.index.Index;
import org.eclipse.jdt.internal.core.index.IndexLocation;
import org.eclipse.jdt.internal.core.search.JavaSearchDocument;
import org.eclipse.jdt.internal.core.search.processing.JobManager;

@SuppressWarnings("rawtypes")
class AddJarFileToIndex extends BinaryContainer {

	private static final char JAR_SEPARATOR = IJavaSearchScope.JAR_FILE_ENTRY_SEPARATOR.charAt(0);
	IFile resource;
	private IndexLocation indexFileURL;
	private final boolean forceIndexUpdate;

	public AddJarFileToIndex(IFile resource, IndexLocation indexFile, IndexManager manager) {
		this(resource, indexFile, manager, false);
	}
	public AddJarFileToIndex(IFile resource, IndexLocation indexFile, IndexManager manager, final boolean updateIndex) {
		super(resource.getFullPath(), manager);
		this.resource = resource;
		this.indexFileURL = indexFile;
		this.forceIndexUpdate = updateIndex;
	}
	public AddJarFileToIndex(IPath jarPath, IndexLocation indexFile, IndexManager manager) {
		this(jarPath, indexFile, manager, false);
	}
	public AddJarFileToIndex(IPath jarPath, IndexLocation indexFile, IndexManager manager, final boolean updateIndex) {
		// external JAR scenario - no resource
		super(jarPath, manager);
		this.indexFileURL = indexFile;
		this.forceIndexUpdate = updateIndex;
	}
	@Override
	public boolean equals(Object o) {
		if (o instanceof AddJarFileToIndex) {
			if (this.resource != null)
				return this.resource.equals(((AddJarFileToIndex) o).resource);
			if (this.containerPath != null)
				return this.containerPath.equals(((AddJarFileToIndex) o).containerPath);
		}
		return false;
	}
	@Override
	public int hashCode() {
		if (this.resource != null)
			return this.resource.hashCode();
		if (this.containerPath != null)
			return this.containerPath.hashCode();
		return -1;
	}
	@Override
	public boolean execute(IProgressMonitor progressMonitor) {

		if (this.isCancelled || progressMonitor != null && progressMonitor.isCanceled()) return true;

		if (hasPreBuiltIndex()) {
			boolean added = this.manager.addIndex(this.containerPath, this.indexFileURL);
			if (added) return true;
			this.indexFileURL = null;
		}

		try {
			// if index is already cached, then do not perform any check
			// MUST reset the IndexManager if a jar file is changed
			Index index = this.manager.getIndexForUpdate(this.containerPath, false, /*do not reuse index file*/ false /*do not create if none*/);
			if (index != null) {
				if (JobManager.VERBOSE)
					org.eclipse.jdt.internal.core.util.Util.verbose("-> no indexing required (index already exists) for " + this.containerPath); //$NON-NLS-1$
				return true;
			}

			index = this.manager.getIndexForUpdate(this.containerPath, true, /*reuse index file*/ true /*create if none*/);
			if (index == null) {
				if (JobManager.VERBOSE)
					org.eclipse.jdt.internal.core.util.Util.verbose("-> index could not be created for " + this.containerPath); //$NON-NLS-1$
				return true;
			}
			ReadWriteMonitor monitor = index.monitor;
			if (monitor == null) {
				if (JobManager.VERBOSE)
					org.eclipse.jdt.internal.core.util.Util.verbose("-> index for " + this.containerPath + " just got deleted"); //$NON-NLS-1$//$NON-NLS-2$
				return true; // index got deleted since acquired
			}
			index.separator = JAR_SEPARATOR;
			ZipFile zip = null;
			try {
				// this path will be a relative path to the workspace in case the zipfile in the workspace otherwise it will be a path in the
				// local file system
				Path zipFilePath = null;

				monitor.enterWrite(); // ask permission to write
				if (this.resource != null) {
					URI location = this.resource.getLocationURI();
					if (location == null) return false;
					if (JavaModelManager.ZIP_ACCESS_VERBOSE)
						System.out.println("(" + Thread.currentThread() + ") [AddJarFileToIndex.execute()] Creating ZipFile on " + location.getPath()); //$NON-NLS-1$	//$NON-NLS-2$
					File file = null;
					try {
						file = org.eclipse.jdt.internal.core.util.Util.toLocalFile(location, progressMonitor);
					} catch (CoreException e) {
						if (JobManager.VERBOSE) {
							org.eclipse.jdt.internal.core.util.Util.verbose("-> failed to index " + location.getPath() + " because of the following exception:"); //$NON-NLS-1$ //$NON-NLS-2$
							e.printStackTrace();
						}
					}
					if (file == null) {
						if (JobManager.VERBOSE)
							org.eclipse.jdt.internal.core.util.Util.verbose("-> failed to index " + location.getPath() + " because the file could not be fetched"); //$NON-NLS-1$ //$NON-NLS-2$
						return false;
					}
					if (JavaModelManager.ZIP_ACCESS_VERBOSE)
						System.out.println("(" + Thread.currentThread() + ") [AddJarFileToIndex.execute()] Creating ZipFile on " + this.containerPath); //$NON-NLS-1$	//$NON-NLS-2$
					zip = new ZipFile(file);
					zipFilePath = (Path) this.resource.getFullPath().makeRelative();
					// absolute path relative to the workspace
				} else {
					if (JavaModelManager.ZIP_ACCESS_VERBOSE)
						System.out.println("(" + Thread.currentThread() + ") [AddJarFileToIndex.execute()] Creating ZipFile on " + this.containerPath); //$NON-NLS-1$	//$NON-NLS-2$
					// external file -> it is ok to use toFile()
					zip = new ZipFile(this.containerPath.toFile());
					zipFilePath = (Path) this.containerPath;
					// path is already canonical since coming from a library classpath entry
				}

				if (this.isCancelled) {
					if (JobManager.VERBOSE)
						org.eclipse.jdt.internal.core.util.Util.verbose("-> indexing of " + zip.getName() + " has been cancelled"); //$NON-NLS-1$ //$NON-NLS-2$
					return false;
				}

				if (JobManager.VERBOSE)
					org.eclipse.jdt.internal.core.util.Util.verbose("-> indexing " + zip.getName()); //$NON-NLS-1$
				long initialTime = System.currentTimeMillis();

				String[] paths = index.queryDocumentNames(""); // all file names //$NON-NLS-1$
				if (paths != null) {
					int max = paths.length;
					/* check integrity of the existing index file
					 * if the length is equal to 0, we want to index the whole jar again
					 * If not, then we want to check that there is no missing entry, if
					 * one entry is missing then we recreate the index
					 */
					String EXISTS = "OK"; //$NON-NLS-1$
					String DELETED = "DELETED"; //$NON-NLS-1$
					SimpleLookupTable indexedFileNames = new SimpleLookupTable(max == 0 ? 33 : max + 11);
					for (int i = 0; i < max; i++)
						indexedFileNames.put(paths[i], DELETED);
					for (Enumeration e = zip.entries(); e.hasMoreElements();) {
						// iterate each entry to index it
						ZipEntry ze = (ZipEntry) e.nextElement();
						String zipEntryName = ze.getName();
						if (Util.isClassFileName(zipEntryName) && isValidPackageNameForClassOrisModule(zipEntryName))
								// the class file may not be there if the package name is not valid
							indexedFileNames.put(zipEntryName, EXISTS);
					}
					boolean needToReindex = indexedFileNames.elementSize != max; // a new file was added
					if (!needToReindex) {
						Object[] valueTable = indexedFileNames.valueTable;
						for (int i = 0, l = valueTable.length; i < l; i++) {
							if (valueTable[i] == DELETED) {
								needToReindex = true; // a file was deleted so re-index
								break;
							}
						}
						if (!needToReindex) {
							if (JobManager.VERBOSE)
								org.eclipse.jdt.internal.core.util.Util.verbose("-> no indexing required (index is consistent with library) for " //$NON-NLS-1$
								+ zip.getName() + " (" //$NON-NLS-1$
								+ (System.currentTimeMillis() - initialTime) + "ms)"); //$NON-NLS-1$
							this.manager.saveIndex(index); // to ensure its placed into the saved state
							return true;
						}
					}
				}

				// Index the jar for the first time or reindex the jar in case the previous index file has been corrupted
				// index already existed: recreate it so that we forget about previous entries
				SearchParticipant participant = SearchEngine.getDefaultSearchParticipant();
				if (!this.manager.resetIndex(this.containerPath)) {
					// failed to recreate index, see 73330
					this.manager.removeIndex(this.containerPath);
					return false;
				}
				index.separator = JAR_SEPARATOR;
				IPath indexPath = null;
				IndexLocation indexLocation;
				if ((indexLocation = index.getIndexLocation()) != null) {
					indexPath = new Path(indexLocation.getCanonicalFilePath());
				}
				boolean hasModuleInfoClass = false;
				for (Enumeration e = zip.entries(); e.hasMoreElements();) {
					if (this.isCancelled) {
						if (JobManager.VERBOSE)
							org.eclipse.jdt.internal.core.util.Util.verbose("-> indexing of " + zip.getName() + " has been cancelled"); //$NON-NLS-1$ //$NON-NLS-2$
						return false;
					}

					// iterate each entry to index it
					ZipEntry ze = (ZipEntry) e.nextElement();
					String zipEntryName = ze.getName();
					if (Util.isClassFileName(zipEntryName) &&
							isValidPackageNameForClassOrisModule(zipEntryName)) {
						hasModuleInfoClass |= zipEntryName.contains(TypeConstants.MODULE_INFO_NAME_STRING);
						// index only classes coming from valid packages - https://bugs.eclipse.org/bugs/show_bug.cgi?id=293861
						final byte[] classFileBytes = org.eclipse.jdt.internal.compiler.util.Util.getZipEntryByteContent(ze, zip);
						JavaSearchDocument entryDocument = new JavaSearchDocument(ze, zipFilePath, classFileBytes, participant);
						this.manager.indexDocument(entryDocument, participant, index, indexPath);
					}
				}
				if (!hasModuleInfoClass) {
					String s;
					try {
						s = this.resource == null ? this.containerPath.toOSString() :
							JavaModelManager.getLocalFile(this.resource.getFullPath()).toPath().toAbsolutePath().toString();
						char[] autoModuleName = AutomaticModuleNaming.determineAutomaticModuleName(s);
						final char[] contents = CharOperation.append(CharOperation.append(TypeConstants.AUTOMATIC_MODULE_NAME.toCharArray(), ':'), autoModuleName);
						// adding only the automatic module entry here - can be extended in the future to include other fields.
						ZipEntry ze = new ZipEntry(TypeConstants.AUTOMATIC_MODULE_NAME);
						JavaSearchDocument entryDocument = new JavaSearchDocument(ze, zipFilePath, new String(contents).getBytes(Charset.defaultCharset()), participant);
						this.manager.indexDocument(entryDocument, participant, index, indexPath);
					} catch (CoreException e) {
						// TODO Auto-generated catch block
//						e.printStackTrace();
					}
				}
				if(this.forceIndexUpdate) {
					this.manager.savePreBuiltIndex(index);
				}
				else {
					this.manager.saveIndex(index);
				}
				if (JobManager.VERBOSE)
					org.eclipse.jdt.internal.core.util.Util.verbose("-> done indexing of " //$NON-NLS-1$
						+ zip.getName() + " (" //$NON-NLS-1$
						+ (System.currentTimeMillis() - initialTime) + "ms)"); //$NON-NLS-1$
			} finally {
				if (zip != null) {
					if (JavaModelManager.ZIP_ACCESS_VERBOSE)
						System.out.println("(" + Thread.currentThread() + ") [AddJarFileToIndex.execute()] Closing ZipFile " + this.containerPath); //$NON-NLS-1$	//$NON-NLS-2$
					zip.close();
				}
				monitor.exitWrite(); // free write lock
			}
		} catch (IOException | ZipError e) {
			org.eclipse.jdt.internal.core.util.Util.log(e, "Failed to index " + this.containerPath); //$NON-NLS-1$
			this.manager.removeIndex(this.containerPath);
			return false;
		}
		return true;
	}
	@Override
	public String getJobFamily() {
		if (this.resource != null)
			return super.getJobFamily();
		return this.containerPath.toOSString(); // external jar
	}
	@Override
	protected Integer updatedIndexState() {

		Integer updateState = null;
		if(hasPreBuiltIndex()) {
			updateState = IndexManager.REUSE_STATE;
		}
		else {
			updateState = IndexManager.REBUILDING_STATE;
		}
		return updateState;
	}
	@Override
	public String toString() {
		return "indexing " + this.containerPath.toString(); //$NON-NLS-1$
	}

	protected boolean hasPreBuiltIndex() {
		return !this.forceIndexUpdate && (this.indexFileURL != null && this.indexFileURL.exists());
	}
}