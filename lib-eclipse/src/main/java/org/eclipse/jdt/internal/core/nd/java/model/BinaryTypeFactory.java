/*******************************************************************************
 * Copyright (c) 2015, 2016 Google, Inc and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Stefan Xenos (Google) - Initial implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.core.nd.java.model;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModelStatusConstants;
import org.eclipse.jdt.core.IOrdinaryClassFile;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFormatException;
import org.eclipse.jdt.internal.compiler.env.IBinaryType;
import org.eclipse.jdt.internal.compiler.env.IDependent;
import org.eclipse.jdt.internal.compiler.util.SuffixConstants;
import org.eclipse.jdt.internal.core.ClassFile;
import org.eclipse.jdt.internal.core.JarPackageFragmentRoot;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.jdt.internal.core.PackageFragment;
import org.eclipse.jdt.internal.core.PackageFragmentRoot;
import org.eclipse.jdt.internal.core.nd.util.CharArrayUtils;
import org.eclipse.jdt.internal.core.util.Util;

public class BinaryTypeFactory {
	public static final class NotInIndexException extends Exception {
		private static final long serialVersionUID = 2859848007651528256L;

		public NotInIndexException() {
		}
	}

	/**
	 * Returns a descriptor for the given class within the given package fragment, or null if the fragment doesn't have
	 * a location on the filesystem.
	 */
	private static BinaryTypeDescriptor createDescriptor(PackageFragment pkg, ClassFile classFile) {
		PackageFragmentRoot root = (PackageFragmentRoot) pkg.getParent();
		IPath location = getLocationForElement(root);
		if (location == null) {
			return null;
		}
		String entryName = Util.concatWith(pkg.names, classFile.getElementName(), '/');
		String name = Util.concatWith(pkg.names, classFile.getName(), '/');
		String overridePath = root.getClassFilePath(entryName);
		if (overridePath != entryName) {
			entryName = overridePath;
			String versionPath = overridePath.substring(0, overridePath.indexOf(entryName));
			name = versionPath + name;
		}
		char[] fieldDescriptor = CharArrayUtils.concat(new char[] { 'L' },
				name.toCharArray(), new char[] { ';' });
		IPath workspacePath = root.getPath();
		String indexPath;

		if (root instanceof JarPackageFragmentRoot) {
			// The old version returned this, but it doesn't conform to the spec on IBinaryType.getFileName():
			indexPath = root.getHandleIdentifier() + IDependent.JAR_FILE_ENTRY_SEPARATOR + entryName;
			// Version that conforms to the JavaDoc spec on IBinaryType.getFileName() -- note that this breaks
			// InlineMethodTests in the JDT UI project. Need to investigate why before using it.
			//indexPath = workspacePath.toString() + IDependent.JAR_FILE_ENTRY_SEPARATOR + entryName;
		} else {
			location = location.append(entryName);
			indexPath = workspacePath.append(entryName).toString();
			workspacePath = classFile.resource().getFullPath();
		}

		return new BinaryTypeDescriptor(location.toString().toCharArray(), fieldDescriptor,
				workspacePath.toString().toCharArray(), indexPath.toCharArray());
	}

	public static BinaryTypeDescriptor createDescriptor(IClassFile classFile) {
		ClassFile concreteClass = (ClassFile)classFile;
		PackageFragment parent = (PackageFragment) classFile.getParent();

		return createDescriptor(parent, concreteClass);
	}

	public static IBinaryType create(IOrdinaryClassFile classFile, IProgressMonitor monitor) throws JavaModelException, ClassFormatException {
		BinaryTypeDescriptor descriptor = createDescriptor(classFile);
		return readType(descriptor, monitor);
	}

	/**
	 * Reads the given binary type. If the type can be found in the index with a fingerprint that exactly matches
	 * the file on disk, the type is read from the index. Otherwise the type is read from disk. Returns null if
	 * no such type exists.
	 * @throws ClassFormatException
	 */
	public static IBinaryType readType(BinaryTypeDescriptor descriptor, IProgressMonitor monitor) throws JavaModelException, ClassFormatException {
		return rawReadType(descriptor, true);
	}

	public static ClassFileReader rawReadType(BinaryTypeDescriptor descriptor, boolean fullyInitialize) throws JavaModelException, ClassFormatException {
		try {
			return rawReadTypeTestForExists(descriptor, fullyInitialize, true);
		} catch (FileNotFoundException e) {
			throw new JavaModelException(e, IJavaModelStatusConstants.IO_EXCEPTION);
		}
	}

	/**
	 * Read the class file from disk, circumventing the index's cache. This should only be used by callers
	 * that need to read information from the class file which aren't present in the index (such as method bodies).
	 *
	 * @return the newly-created ClassFileReader or null if the given class file does not exist.
	 * @throws ClassFormatException if the class file existed but was corrupt
	 * @throws JavaModelException if unable to read the class file due to a transient failure
	 * @throws FileNotFoundException if the file does not exist
	 */
	public static ClassFileReader rawReadTypeTestForExists(BinaryTypeDescriptor descriptor, boolean fullyInitialize,
			boolean useInvalidArchiveCache) throws JavaModelException, ClassFormatException, FileNotFoundException {
		if (descriptor == null) {
			return null;
		}
		if (descriptor.isInJarFile()) {
			if (CharOperation.indexOf("jrt-fs.jar".toCharArray(), descriptor.location, false) == -1) { //$NON-NLS-1$
				ZipFile zip = null;
				try {
					zip = JavaModelManager.getJavaModelManager().getZipFile(new Path(new String(descriptor.workspacePath)),
							useInvalidArchiveCache);
					char[] entryNameCharArray = CharArrayUtils.concat(
							fieldDescriptorToBinaryName(descriptor.fieldDescriptor), SuffixConstants.SUFFIX_class);
					String entryName = new String(entryNameCharArray);
					ZipEntry ze = zip.getEntry(entryName);
					if (ze != null) {
						byte contents[];
						try {
							contents = org.eclipse.jdt.internal.compiler.util.Util.getZipEntryByteContent(ze, zip);
						} catch (IOException ioe) {
							throw new JavaModelException(ioe, IJavaModelStatusConstants.IO_EXCEPTION);
						}
						return new ClassFileReader(contents, descriptor.indexPath, fullyInitialize);
					}
				} catch (CoreException e) {
					throw new JavaModelException(e);
				} finally {
					JavaModelManager.getJavaModelManager().closeZipFile(zip);
				}
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
			return new ClassFileReader(contents, file.getFullPath().toString().toCharArray(), fullyInitialize);
		}
		return null;
	}

	/**
	 * Returns the absolute filesystem location of the given element or the empty path if none
	 * <p>
	 * The logic used in {@link #getLocationForElement(IJavaElement)} and
	 * {@link JavaModelManager#getLocalFile(IPath)} should be equivalent.
	 */
	public static IPath getLocationForElement(IJavaElement next) {
		IResource resource = next.getResource();

		if (resource != null) {
			return resource.getLocation() == null ? Path.EMPTY : resource.getLocation();
		}

		return next.getPath();
	}

	/**
	 * Given a field descriptor, if the field descriptor points to a class this returns the binary name of the class. If
	 * the field descriptor points to any other type, this returns the empty string. The field descriptor may optionally
	 * contain a trailing ';'.
	 *
	 * @param fieldDescriptor
	 * @return ""
	 */
	public static char[] fieldDescriptorToBinaryName(char[] fieldDescriptor) {
		if (CharArrayUtils.startsWith(fieldDescriptor, 'L')) {
			int end = fieldDescriptor.length - 1;
			return CharArrayUtils.subarray(fieldDescriptor, 1, end);
		}
		return CharArrayUtils.EMPTY_CHAR_ARRAY;
	}

}
