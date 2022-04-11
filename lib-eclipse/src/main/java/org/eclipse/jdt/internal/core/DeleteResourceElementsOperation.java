/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
package org.eclipse.jdt.internal.core;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModelStatusConstants;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.util.Messages;

/**
 * This operation deletes a collection of resources and all of their children.
 * It does not delete resources which do not belong to the Java Model
 * (eg GIF files).
 */
public class DeleteResourceElementsOperation extends MultiOperation {
/**
 * When executed, this operation will delete the given elements. The elements
 * to delete cannot be <code>null</code> or empty, and must have a corresponding
 * resource.
 */
protected DeleteResourceElementsOperation(IJavaElement[] elementsToProcess, boolean force) {
	super(elementsToProcess, force);
}
/**
 * Deletes the direct children of <code>frag</code> corresponding to its kind
 * (K_SOURCE or K_BINARY), and deletes the corresponding folder if it is then
 * empty.
 */
private void deletePackageFragment(IPackageFragment frag)
	throws JavaModelException {
	IResource res = ((JavaElement) frag).resource();
	if (res != null) {
		// collect the children to remove
		IJavaElement[] childrenOfInterest = frag.getChildren();
		if (childrenOfInterest.length > 0) {
			IResource[] resources = new IResource[childrenOfInterest.length];
			// remove the children
			for (int i = 0; i < childrenOfInterest.length; i++) {
				resources[i] = ((JavaElement) childrenOfInterest[i]).resource();
			}
			deleteResources(resources, this.force);
		}

		// Discard non-java resources
		Object[] nonJavaResources = frag.getNonJavaResources();
		int actualResourceCount = 0;
		for (int i = 0, max = nonJavaResources.length; i < max; i++){
			if (nonJavaResources[i] instanceof IResource) actualResourceCount++;
		}
		IResource[] actualNonJavaResources = new IResource[actualResourceCount];
		for (int i = 0, max = nonJavaResources.length, index = 0; i < max; i++){
			if (nonJavaResources[i] instanceof IResource) actualNonJavaResources[index++] = (IResource)nonJavaResources[i];
		}
		deleteResources(actualNonJavaResources, this.force);

		// delete remaining files in this package (.class file in the case where Proj=src=bin)
		IResource[] remainingFiles;
		try {
			remainingFiles = ((IContainer) res).members();
		} catch (CoreException ce) {
			throw new JavaModelException(ce);
		}
		boolean isEmpty = true;
		for (int i = 0, length = remainingFiles.length; i < length; i++) {
			IResource file = remainingFiles[i];
			if (file instanceof IFile && org.eclipse.jdt.internal.compiler.util.Util.isClassFileName(file.getName())) {
				deleteResource(file, IResource.FORCE | IResource.KEEP_HISTORY);
			} else {
				isEmpty = false;
			}
		}
		if (isEmpty && !frag.isDefaultPackage()/*don't delete default package's folder: see https://bugs.eclipse.org/bugs/show_bug.cgi?id=38450*/) {
			// delete recursively empty folders
			IResource fragResource =  ((JavaElement) frag).resource();
			if (fragResource != null) {
				deleteEmptyPackageFragment(frag, false, fragResource.getParent());
			}
		}
	}
}
/**
 * @see MultiOperation
 */
@Override
protected String getMainTaskName() {
	return Messages.operation_deleteResourceProgress;
}
/**
 * @see MultiOperation This method delegate to <code>deleteResource</code> or
 * <code>deletePackageFragment</code> depending on the type of <code>element</code>.
 */
@Override
protected void processElement(IJavaElement element) throws JavaModelException {
	switch (element.getElementType()) {
		case IJavaElement.CLASS_FILE :
		case IJavaElement.COMPILATION_UNIT :
			deleteResource(element.getResource(), this.force ? IResource.FORCE | IResource.KEEP_HISTORY : IResource.KEEP_HISTORY);
			break;
		case IJavaElement.PACKAGE_FRAGMENT :
			deletePackageFragment((IPackageFragment) element);
			break;
		default :
			throw new JavaModelException(new JavaModelStatus(IJavaModelStatusConstants.INVALID_ELEMENT_TYPES, element));
	}
	// ensure the element is closed
	if (element instanceof IOpenable) {
		((IOpenable)element).close();
	}
}
/**
 * @see MultiOperation
 */
@Override
protected void verify(IJavaElement element) throws JavaModelException {
	if (element == null || !element.exists())
		error(IJavaModelStatusConstants.ELEMENT_DOES_NOT_EXIST, element);

	int type = element.getElementType();
	if (type <= IJavaElement.PACKAGE_FRAGMENT_ROOT || type > IJavaElement.COMPILATION_UNIT)
		error(IJavaModelStatusConstants.INVALID_ELEMENT_TYPES, element);
	else if (type == IJavaElement.PACKAGE_FRAGMENT && element instanceof JarPackageFragment)
		error(IJavaModelStatusConstants.INVALID_ELEMENT_TYPES, element);
	IResource resource = ((JavaElement) element).resource();
	if (resource instanceof IFolder) {
		if (resource.isLinked()) {
			error(IJavaModelStatusConstants.INVALID_RESOURCE, element);
		}
	}
}
}
