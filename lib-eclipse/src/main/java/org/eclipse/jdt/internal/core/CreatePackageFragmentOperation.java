/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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

import java.util.ArrayList;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModelStatus;
import org.eclipse.jdt.core.IJavaModelStatusConstants;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.core.util.Messages;
import org.eclipse.jdt.internal.core.util.Util;

/**
 * This operation creates a new package fragment under a given package fragment root.
 * The following must be specified: <ul>
 * <li>the package fragment root
 * <li>the package name
 * </ul>
 * <p>Any needed folders/package fragments are created.
 * If the package fragment already exists, this operation has no effect.
 * The result elements include the <code>IPackageFragment</code> created and any side effect
 * package fragments that were created.
 *
 * <p>NOTE: A default package fragment exists by default for a given root.
 *
 * <p>Possible exception conditions: <ul>
 *  <li>Package fragment root is read-only
 *  <li>Package fragment's name is taken by a simple (non-folder) resource
 * </ul>
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class CreatePackageFragmentOperation extends JavaModelOperation {
	/**
	 * The fully qualified, dot-delimited, package name.
	 */
	protected String[] pkgName;
/**
 * When executed, this operation will create a package fragment with the given name
 * under the given package fragment root. The dot-separated name is broken into
 * segments. Intermediate folders are created as required for each segment.
 * If the folders already exist, this operation has no effect.
 */
public CreatePackageFragmentOperation(IPackageFragmentRoot parentElement, String packageName, boolean force) {
	super(null, new IJavaElement[]{parentElement}, force);
	this.pkgName = packageName == null ? null : Util.getTrimmedSimpleNames(packageName);
}
/**
 * Execute the operation - creates the new package fragment and any
 * side effect package fragments.
 *
 * @exception JavaModelException if the operation is unable to complete
 */
@Override
protected void executeOperation() throws JavaModelException {
	try {
		JavaElementDelta delta = null;
		PackageFragmentRoot root = (PackageFragmentRoot) getParentElement();
		beginTask(Messages.operation_createPackageFragmentProgress, this.pkgName.length);
		IContainer parentFolder = (IContainer) root.resource();
		String[] sideEffectPackageName = CharOperation.NO_STRINGS;
		ArrayList results = new ArrayList(this.pkgName.length);
		char[][] inclusionPatterns = root.fullInclusionPatternChars();
		char[][] exclusionPatterns = root.fullExclusionPatternChars();
		int i;
		for (i = 0; i < this.pkgName.length; i++) {
			String subFolderName = this.pkgName[i];
			sideEffectPackageName = Util.arrayConcat(sideEffectPackageName, subFolderName);
			IResource subFolder = parentFolder.findMember(subFolderName);
			if (subFolder == null) {
				createFolder(parentFolder, subFolderName, this.force);
				parentFolder = parentFolder.getFolder(new Path(subFolderName));
				IPackageFragment addedFrag = root.getPackageFragment(sideEffectPackageName);
				if (!Util.isExcluded(parentFolder, inclusionPatterns, exclusionPatterns)) {
					if (delta == null) {
						delta = newJavaElementDelta();
					}
					delta.added(addedFrag);
				}
				results.add(addedFrag);
			} else {
				parentFolder = (IContainer) subFolder;
			}
			worked(1);
		}
		if (results.size() > 0) {
			this.resultElements = new IJavaElement[results.size()];
			results.toArray(this.resultElements);
			if (delta != null) {
				addDelta(delta);
			}
		}
	} finally {
		done();
	}
}
@Override
protected ISchedulingRule getSchedulingRule() {
	if (this.pkgName.length == 0)
		return null; // no resource is going to be created
	IResource parentResource = ((JavaElement) getParentElement()).resource();
	IResource resource = ((IContainer) parentResource).getFolder(new Path(this.pkgName[0]));
	return resource.getWorkspace().getRuleFactory().createRule(resource);
}
/**
 * Possible failures: <ul>
 *  <li>NO_ELEMENTS_TO_PROCESS - the root supplied to the operation is
 * 		<code>null</code>.
 *	<li>INVALID_NAME - the name provided to the operation
 * 		is <code>null</code> or is not a valid package fragment name.
 *	<li>READ_ONLY - the root provided to this operation is read only.
 *	<li>NAME_COLLISION - there is a pre-existing resource (file)
 * 		with the same name as a folder in the package fragment's hierarchy.
 *	<li>ELEMENT_NOT_PRESENT - the underlying resource for the root is missing
 * </ul>
 * @see IJavaModelStatus
 * @see JavaConventions
 */
@Override
public IJavaModelStatus verify() {
	IJavaElement parentElement = getParentElement();
	if (parentElement == null) {
		return new JavaModelStatus(IJavaModelStatusConstants.NO_ELEMENTS_TO_PROCESS);
	}

	String packageName = this.pkgName == null ? null : Util.concatWith(this.pkgName, '.');
	IJavaProject project = parentElement.getJavaProject();
	if (this.pkgName == null || (this.pkgName.length > 0 && JavaConventions.validatePackageName(packageName, project.getOption(JavaCore.COMPILER_SOURCE, true), project.getOption(JavaCore.COMPILER_COMPLIANCE, true)).getSeverity() == IStatus.ERROR)) {
		return new JavaModelStatus(IJavaModelStatusConstants.INVALID_NAME, packageName);
	}
	IJavaElement root = getParentElement();
	if (root.isReadOnly()) {
		return new JavaModelStatus(IJavaModelStatusConstants.READ_ONLY, root);
	}
	IContainer parentFolder = (IContainer) ((JavaElement) root).resource();
	int i;
	for (i = 0; i < this.pkgName.length; i++) {
		IResource subFolder = parentFolder.findMember(this.pkgName[i]);
		if (subFolder != null) {
			if (subFolder.getType() != IResource.FOLDER) {
				return new JavaModelStatus(
					IJavaModelStatusConstants.NAME_COLLISION,
					Messages.bind(Messages.status_nameCollision, subFolder.getFullPath().toString()));
			}
			parentFolder = (IContainer) subFolder;
		}
	}
	return JavaModelStatus.VERIFIED_OK;
}
}
