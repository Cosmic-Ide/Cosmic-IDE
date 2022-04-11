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
import java.io.UnsupportedEncodingException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModelStatus;
import org.eclipse.jdt.core.IJavaModelStatusConstants;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.util.Messages;
import org.eclipse.jdt.internal.core.util.Util;

/**
 * Commits the contents of a working copy compilation
 * unit to its original element and resource, bringing
 * the Java Model up-to-date with the current contents of the working
 * copy.
 *
 * <p>It is possible that the contents of the
 * original resource have changed since the working copy was created,
 * in which case there is an update conflict. This operation allows
 * for two settings to resolve conflict set by the <code>fForce</code> flag:<ul>
 * <li>force flag is <code>false</code> - in this case an <code>JavaModelException</code>
 * 	is thrown</li>
 * <li>force flag is <code>true</code> - in this case the contents of
 * 	the working copy are applied to the underlying resource even though
 * 	the working copy was created before a subsequent change in the
 * 	resource</li>
 * </ul>
 *
 * <p>The default conflict resolution setting is the force flag is <code>false</code>
 *
 * A JavaModelOperation exception is thrown either if the commit could not
 * be performed or if the new content of the compilation unit violates some Java Model
 * constraint (e.g. if the new package declaration doesn't match the name of the folder
 * containing the compilation unit).
 */
public class CommitWorkingCopyOperation extends JavaModelOperation {
	/**
	 * Constructs an operation to commit the contents of a working copy
	 * to its original compilation unit.
	 */
	public CommitWorkingCopyOperation(ICompilationUnit element, boolean force) {
		super(new IJavaElement[] {element}, force);
	}
	/**
	 * @exception JavaModelException if setting the source
	 * 	of the original compilation unit fails
	 */
	@Override
	protected void executeOperation() throws JavaModelException {
		try {
			beginTask(Messages.workingCopy_commit, 2);
			CompilationUnit workingCopy = getCompilationUnit();

			if (ExternalJavaProject.EXTERNAL_PROJECT_NAME.equals(workingCopy.getJavaProject().getElementName())) {
				// case of a working copy without a resource
				workingCopy.getBuffer().save(this.progressMonitor, this.force);
				return;
			}

			ICompilationUnit primary = workingCopy.getPrimary();
			boolean isPrimary = workingCopy.isPrimary();

			JavaElementDeltaBuilder deltaBuilder = null;
			PackageFragmentRoot root = (PackageFragmentRoot)workingCopy.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
			boolean isIncluded = !Util.isExcluded(workingCopy);
			IFile resource = (IFile)workingCopy.getResource();
			IJavaProject project = root.getJavaProject();
			if (isPrimary || (root.validateOnClasspath().isOK() && isIncluded && resource.isAccessible() && Util.isValidCompilationUnitName(workingCopy.getElementName(), project.getOption(JavaCore.COMPILER_SOURCE, true), project.getOption(JavaCore.COMPILER_COMPLIANCE, true)))) {

				// force opening so that the delta builder can get the old info
				if (!isPrimary && !primary.isOpen()) {
					primary.open(null);
				}

				// creates the delta builder (this remembers the content of the cu) if:
				// - it is not excluded
				// - and it is not a primary or it is a non-consistent primary
				if (isIncluded && (!isPrimary || !workingCopy.isConsistent())) {
					deltaBuilder = new JavaElementDeltaBuilder(primary);
				}

				// save the cu
				IBuffer primaryBuffer = primary.getBuffer();
				if (!isPrimary) {
					if (primaryBuffer == null) return;
					char[] primaryContents = primaryBuffer.getCharacters();
					boolean hasSaved = false;
					try {
						IBuffer workingCopyBuffer = workingCopy.getBuffer();
						if (workingCopyBuffer == null) return;
						primaryBuffer.setContents(workingCopyBuffer.getCharacters());
						primaryBuffer.save(this.progressMonitor, this.force);
						primary.makeConsistent(this);
						hasSaved = true;
					} finally {
						if (!hasSaved){
							// restore original buffer contents since something went wrong
							primaryBuffer.setContents(primaryContents);
						}
					}
				} else {
					// for a primary working copy no need to set the content of the buffer again
					primaryBuffer.save(this.progressMonitor, this.force);
					primary.makeConsistent(this);
				}
			} else {
				// working copy on cu outside classpath OR resource doesn't exist yet
				String encoding = null;
				try {
					encoding = resource.getCharset();
				}
				catch (CoreException ce) {
					// use no encoding
				}
				String contents = workingCopy.getSource();
				if (contents == null) return;
				try {
					byte[] bytes = encoding == null
						? contents.getBytes()
						: contents.getBytes(encoding);
					ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
					if (resource.exists()) {
						resource.setContents(
							stream,
							this.force ? IResource.FORCE | IResource.KEEP_HISTORY : IResource.KEEP_HISTORY,
							null);
					} else {
						resource.create(
							stream,
							this.force,
							this.progressMonitor);
					}
				} catch (CoreException e) {
					throw new JavaModelException(e);
				} catch (UnsupportedEncodingException e) {
					throw new JavaModelException(e, IJavaModelStatusConstants.IO_EXCEPTION);
				}

			}

			setAttribute(HAS_MODIFIED_RESOURCE_ATTR, TRUE);

			// make sure working copy is in sync
			workingCopy.updateTimeStamp((CompilationUnit)primary);
			workingCopy.makeConsistent(this);
			worked(1);

			// build the deltas
			if (deltaBuilder != null) {
				deltaBuilder.buildDeltas();

				// add the deltas to the list of deltas created during this operation
				if (deltaBuilder.delta != null) {
					addDelta(deltaBuilder.delta);
				}
			}
			worked(1);
		} finally {
			done();
		}
	}
	/**
	 * Returns the compilation unit this operation is working on.
	 */
	protected CompilationUnit getCompilationUnit() {
		return (CompilationUnit)getElementToProcess();
	}
	@Override
	protected ISchedulingRule getSchedulingRule() {
		IResource resource = getElementToProcess().getResource();
		if (resource == null) return null;
		IWorkspace workspace = resource.getWorkspace();
		if (resource.exists()) {
			return workspace.getRuleFactory().modifyRule(resource);
		} else {
			return workspace.getRuleFactory().createRule(resource);
		}
	}
	/**
	 * Possible failures: <ul>
	 *	<li>INVALID_ELEMENT_TYPES - the compilation unit supplied to this
	 *		operation is not a working copy
	 *  <li>ELEMENT_NOT_PRESENT - the compilation unit the working copy is
	 *		based on no longer exists.
	 *  <li>UPDATE_CONFLICT - the original compilation unit has changed since
	 *		the working copy was created and the operation specifies no force
	 *  <li>READ_ONLY - the original compilation unit is in read-only mode
	 *  </ul>
	 */
	@Override
	public IJavaModelStatus verify() {
		CompilationUnit cu = getCompilationUnit();
		if (!cu.isWorkingCopy()) {
			return new JavaModelStatus(IJavaModelStatusConstants.INVALID_ELEMENT_TYPES, cu);
		}
		if (cu.hasResourceChanged() && !this.force) {
			return new JavaModelStatus(IJavaModelStatusConstants.UPDATE_CONFLICT);
		}
		// no read-only check, since some repository adapters can change the flag on save
		// operation.
		return JavaModelStatus.VERIFIED_OK;
	}
}
