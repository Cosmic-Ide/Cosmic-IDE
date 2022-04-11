/*******************************************************************************
 * Copyright (c) 2017 GK Software AG, and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Stephan Herrmann - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.core.nd.java.model;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaModelStatusConstants;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFormatException;
import org.eclipse.jdt.internal.compiler.env.IBinaryModule;
import org.eclipse.jdt.internal.compiler.env.IDependent;
import org.eclipse.jdt.internal.compiler.lookup.TypeConstants;
import org.eclipse.jdt.internal.core.JarPackageFragmentRoot;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.jdt.internal.core.JrtPackageFragmentRoot;
import org.eclipse.jdt.internal.core.ModularClassFile;
import org.eclipse.jdt.internal.core.PackageFragmentRoot;

/**
 * <strong>FIXME:</strong> this class is a stub as of now, it does not support modules in the new index.
 */
public class BinaryModuleFactory {

	public static BinaryModuleDescriptor createDescriptor(ModularClassFile modularClassFile) {
		return createDescriptor(modularClassFile.getPackageFragmentRoot(), modularClassFile);
	}

	/**
	 * Returns a descriptor for the given class within the given package fragment, or null if the fragment doesn't have
	 * a location on the filesystem.
	 */
	private static BinaryModuleDescriptor createDescriptor(PackageFragmentRoot root, ModularClassFile classFile) {
		IPath location = BinaryTypeFactory.getLocationForElement(root);
		if (location == null) {
			return null;
		}
		String entryName = TypeConstants.MODULE_INFO_CLASS_NAME_STRING;
		IPath workspacePath = root.getPath();
		String indexPath;
		char[] moduleName = null;

		if (root instanceof JarPackageFragmentRoot) {
			entryName = ((JarPackageFragmentRoot) root).getClassFilePath(entryName);
			indexPath = root.getHandleIdentifier() + IDependent.JAR_FILE_ENTRY_SEPARATOR + entryName;
			// see additional comments in BinaryTypeFactor.createDescriptor()
			if (root instanceof JrtPackageFragmentRoot) {
				moduleName = root.getElementName().toCharArray();
			}
		} else {
			location = location.append(entryName);
			indexPath = workspacePath.append(entryName).toString();
			workspacePath = classFile.resource().getFullPath();
		}

		return new BinaryModuleDescriptor(location.toString().toCharArray(), moduleName, // TODO: module name only known for JRT
				workspacePath.toString().toCharArray(), indexPath.toCharArray());
	}

	/**
	 * Reads the given binary module. If the module can be found in the index with a fingerprint that exactly matches
	 * the file on disk, the type is read from the index. Otherwise the type is read from disk. Returns null if
	 * no such type exists.
	 * <strong>caveat</strong> modules are not yet supported in the index.
	 *
	 * @throws ClassFormatException
	 */
	public static IBinaryModule readModule(BinaryModuleDescriptor descriptor, IProgressMonitor monitor) throws JavaModelException, ClassFormatException {
// FIXME: support module in the new index
//		if (JavaIndex.isEnabled()) {
//			try {
//				return readFromIndex(JavaIndex.getIndex(), descriptor, monitor);
//			} catch (NotInIndexException e) {
//				// fall back to reading the zip file, below
//			}
//		}
		return rawReadModule(descriptor, true);
	}

	public static IBinaryModule rawReadModule(BinaryModuleDescriptor descriptor, boolean fullyInitialize) throws JavaModelException, ClassFormatException {
		try {
			return rawReadModuleTestForExists(descriptor, fullyInitialize, true);
		} catch (FileNotFoundException e) {
			throw new JavaModelException(e, IJavaModelStatusConstants.IO_EXCEPTION);
		}
	}

	/**
	 * Read the class file from disk, circumventing the index's cache. This should only be used by callers
	 * that need to read information from the class file which aren't present in the index (such as method bodies).
	 *
	 * @return the newly-created IBinaryModule or null if the given class file does not exist.
	 * @throws ClassFormatException if the class file existed but was corrupt
	 * @throws JavaModelException if unable to read the class file due to a transient failure
	 * @throws FileNotFoundException if the file does not exist
	 */
	public static IBinaryModule rawReadModuleTestForExists(BinaryModuleDescriptor descriptor, boolean fullyInitialize,
			boolean useInvalidArchiveCache) throws JavaModelException, ClassFormatException, FileNotFoundException {
		if (descriptor == null) {
			return null;
		}
		if (descriptor.isInJarFile()) {
			ZipFile zip = null;
			try {
				zip = JavaModelManager.getJavaModelManager().getZipFile(new Path(new String(descriptor.workspacePath)),
						useInvalidArchiveCache);
				String entryName = TypeConstants.MODULE_INFO_CLASS_NAME_STRING;
				ZipEntry ze = zip.getEntry(entryName);
				if (ze != null) {
					byte contents[];
					try {
						contents = org.eclipse.jdt.internal.compiler.util.Util.getZipEntryByteContent(ze, zip);
					} catch (IOException ioe) {
						throw new JavaModelException(ioe, IJavaModelStatusConstants.IO_EXCEPTION);
					}
					ClassFileReader classFileReader = new ClassFileReader(contents, descriptor.indexPath, fullyInitialize);
					return classFileReader.getModuleDeclaration();
				}
			} catch (CoreException e) {
				throw new JavaModelException(e);
			} finally {
				JavaModelManager.getJavaModelManager().closeZipFile(zip);
			}
		} else {
			IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(new String(descriptor.workspacePath)));
			byte[] contents;
			try (InputStream stream = file.getContents(true)) {
				contents = org.eclipse.jdt.internal.compiler.util.Util.getInputStreamAsByteArray(stream);
			} catch (CoreException e) {
				IStatus status = e.getStatus();
				if (status.getCode() == IResourceStatus.RESOURCE_NOT_FOUND) {
					throw new FileNotFoundException();
				}
				throw new JavaModelException(e);
			} catch (IOException e) {
				throw new JavaModelException(e, IJavaModelStatusConstants.IO_EXCEPTION);
			}
			ClassFileReader classFileReader = new ClassFileReader(contents, file.getFullPath().toString().toCharArray(), fullyInitialize);
			return classFileReader.getModuleDeclaration();
		}
		return null;
	}
}
