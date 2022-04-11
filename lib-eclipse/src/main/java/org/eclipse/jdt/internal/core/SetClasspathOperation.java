/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
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

import org.eclipse.core.resources.IResourceRuleFactory;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModelStatus;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.JavaModelManager.PerProjectInfo;

/**
 * This operation sets an <code>IJavaProject</code>'s classpath.
 *
 * @see IJavaProject
 */
public class SetClasspathOperation extends ChangeClasspathOperation {

	IClasspathEntry[] newRawClasspath;
	IClasspathEntry[] referencedEntries;
	IPath newOutputLocation;
	JavaProject project;

	public SetClasspathOperation(
		JavaProject project,
		IClasspathEntry[] newRawClasspath,
		IPath newOutputLocation,
		boolean canChangeResource) {

		this(project, newRawClasspath, null, newOutputLocation, canChangeResource);
	}

	/**
	 * When executed, this operation sets the raw classpath and output location of the given project.
	 */
	public SetClasspathOperation(
		JavaProject project,
		IClasspathEntry[] newRawClasspath,
		IClasspathEntry[] referencedEntries,
		IPath newOutputLocation,
		boolean canChangeResource) {

		super(new IJavaElement[] { project }, canChangeResource);
		this.project = project;
		this.newRawClasspath = newRawClasspath;
		this.referencedEntries = referencedEntries;
		this.newOutputLocation = newOutputLocation;
	}

	/**
	 * Sets the classpath of the pre-specified project.
	 */
	@Override
	protected void executeOperation() throws JavaModelException {
		checkCanceled();
		try {
			// set raw classpath and null out resolved info
			PerProjectInfo perProjectInfo = this.project.getPerProjectInfo();
			ClasspathChange classpathChange = perProjectInfo.setRawClasspath(this.newRawClasspath, this.referencedEntries, this.newOutputLocation, JavaModelStatus.VERIFIED_OK/*format is ok*/);

			// if needed, generate delta, update project ref, create markers, ...
			classpathChanged(classpathChange, true/*refresh if external linked folder already exists*/);

			// write .classpath file
			if (this.canChangeResources && perProjectInfo.writeAndCacheClasspath(this.project, this.newRawClasspath, this.newOutputLocation))
				setAttribute(HAS_MODIFIED_RESOURCE_ATTR, TRUE);
		} finally {
			done();
		}
	}

	@Override
	protected ISchedulingRule getSchedulingRule() {
		if (this.canChangeResources) {
			IResourceRuleFactory ruleFactory = ResourcesPlugin.getWorkspace().getRuleFactory();
			return new MultiRule(new ISchedulingRule[] {
				// use project modification rule as this is needed to create the .classpath file if it doesn't exist yet, or to update project references
				ruleFactory.modifyRule(this.project.getProject()),

				// and external project modification rule in case the external folders are modified
				ruleFactory.modifyRule(JavaModelManager.getExternalManager().getExternalFoldersProject())
			});
		}
		return super.getSchedulingRule();
	}

	@Override
	public String toString(){
		StringBuilder buffer = new StringBuilder(20);
		buffer.append("SetClasspathOperation\n"); //$NON-NLS-1$
		buffer.append(" - classpath : "); //$NON-NLS-1$
		buffer.append("{"); //$NON-NLS-1$
		for (int i = 0; i < this.newRawClasspath.length; i++) {
			if (i > 0) buffer.append(","); //$NON-NLS-1$
			IClasspathEntry element = this.newRawClasspath[i];
			buffer.append(" ").append(element.toString()); //$NON-NLS-1$
		}
		buffer.append("\n - output location : ");  //$NON-NLS-1$
		buffer.append(this.newOutputLocation.toString());
		return buffer.toString();
	}

	@Override
	public IJavaModelStatus verify() {
		IJavaModelStatus status = super.verify();
		if (!status.isOK())
			return status;
		this.project.flushClasspathProblemMarkers(false, false, true);
		return ClasspathEntry.validateClasspath(this.project, this.newRawClasspath, this.newOutputLocation);
	}

}
