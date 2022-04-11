/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IJavaModelStatus;
import org.eclipse.jdt.core.IJavaModelStatusConstants;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.util.Messages;
import org.eclipse.jdt.internal.core.util.Util;

/**
 * <p>This operation creates a compilation unit (CU).
 * If the CU doesn't exist yet, a new compilation unit will be created with the content provided.
 * Otherwise the operation will override the contents of an existing CU with the new content.
 *
 * <p>Note: It is possible to create a CU automatically when creating a
 * class or interface. Thus, the preferred method of creating a CU is
 * to perform a create type operation rather than
 * first creating a CU and secondly creating a type inside the CU.
 *
 * <p>Required Attributes:<ul>
 *  <li>The package fragment in which to create the compilation unit.
 *  <li>The name of the compilation unit.
 *      Do not include the <code>".java"</code> suffix (ex. <code>"Object"</code> -
 * 		the <code>".java"</code> will be added for the name of the compilation unit.)
 *  <li>
  * </ul>
 */
public class CreateCompilationUnitOperation extends JavaModelOperation {

	/**
	 * The name of the compilation unit being created.
	 */
	protected String name;
	/**
	 * The source code to use when creating the element.
	 */
	protected String source= null;
/**
 * When executed, this operation will create a compilation unit with the given name.
 * The name should have the ".java" suffix.
 */
public CreateCompilationUnitOperation(IPackageFragment parentElement, String name, String source, boolean force) {
	super(null, new IJavaElement[] {parentElement}, force);
	this.name = name;
	this.source = source;
}
/**
 * Creates a compilation unit.
 *
 * @exception JavaModelException if unable to create the compilation unit.
 */
@Override
protected void executeOperation() throws JavaModelException {
	try {
		beginTask(Messages.operation_createUnitProgress, 2);
		JavaElementDelta delta = newJavaElementDelta();
		ICompilationUnit unit = getCompilationUnit();
		IPackageFragment pkg = (IPackageFragment) getParentElement();
		IContainer folder = (IContainer) pkg.getResource();
		worked(1);
		IFile compilationUnitFile = folder.getFile(new Path(this.name));
		if (compilationUnitFile.exists()) {
			// update the contents of the existing unit if fForce is true
			if (this.force) {
				IBuffer buffer = unit.getBuffer();
				if (buffer == null) return;
				buffer.setContents(this.source);
				unit.save(new NullProgressMonitor(), false);
				this.resultElements = new IJavaElement[] {unit};
				if (!Util.isExcluded(unit)
						&& unit.getParent().exists()) {
					for (int i = 0; i < this.resultElements.length; i++) {
						delta.changed(this.resultElements[i], IJavaElementDelta.F_CONTENT);
					}
					addDelta(delta);
				}
			} else {
				throw new JavaModelException(new JavaModelStatus(
					IJavaModelStatusConstants.NAME_COLLISION,
					Messages.bind(Messages.status_nameCollision, compilationUnitFile.getFullPath().toString())));
			}
		} else {
			try {
				String encoding = null;
				try {
					encoding = folder.getDefaultCharset(); // get folder encoding as file is not accessible
				}
				catch (CoreException ce) {
					// use no encoding
				}
				InputStream stream = new ByteArrayInputStream(encoding == null ? this.source.getBytes() : this.source.getBytes(encoding));
				createFile(folder, unit.getElementName(), stream, this.force);
				this.resultElements = new IJavaElement[] {unit};
				if (!Util.isExcluded(unit)
						&& unit.getParent().exists()) {
					for (int i = 0; i < this.resultElements.length; i++) {
						delta.added(this.resultElements[i]);
					}
					addDelta(delta);
				}
			} catch (IOException e) {
				throw new JavaModelException(e, IJavaModelStatusConstants.IO_EXCEPTION);
			}
		}
		worked(1);
	} finally {
		done();
	}
}
/**
 * @see CreateElementInCUOperation#getCompilationUnit()
 */
protected ICompilationUnit getCompilationUnit() {
	return ((IPackageFragment)getParentElement()).getCompilationUnit(this.name);
}
@Override
protected ISchedulingRule getSchedulingRule() {
	IResource resource  = getCompilationUnit().getResource();
	IWorkspace workspace = resource.getWorkspace();
	if (resource.exists()) {
		return workspace.getRuleFactory().modifyRule(resource);
	} else {
		return workspace.getRuleFactory().createRule(resource);
	}
}
/**
 * Possible failures: <ul>
 *  <li>NO_ELEMENTS_TO_PROCESS - the package fragment supplied to the operation is
 * 		<code>null</code>.
 *	<li>INVALID_NAME - the compilation unit name provided to the operation
 * 		is <code>null</code> or has an invalid syntax
 *  <li>INVALID_CONTENTS - the source specified for the compiliation unit is null
 * </ul>
 */
@Override
public IJavaModelStatus verify() {
	if (getParentElement() == null) {
		return new JavaModelStatus(IJavaModelStatusConstants.NO_ELEMENTS_TO_PROCESS);
	}
	IJavaProject project = getParentElement().getJavaProject();
	if (JavaConventions.validateCompilationUnitName(this.name, project.getOption(JavaCore.COMPILER_SOURCE, true), project.getOption(JavaCore.COMPILER_COMPLIANCE, true)).getSeverity() == IStatus.ERROR) {
		return new JavaModelStatus(IJavaModelStatusConstants.INVALID_NAME, this.name);
	}
	if (this.source == null) {
		return new JavaModelStatus(IJavaModelStatusConstants.INVALID_CONTENTS);
	}
	return JavaModelStatus.VERIFIED_OK;
}
}
