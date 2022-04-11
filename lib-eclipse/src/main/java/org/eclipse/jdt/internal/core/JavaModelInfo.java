/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;

/**
 * Implementation of IJavaModel. A Java Model is specific to a
 * workspace.
 *
 * @see org.eclipse.jdt.core.IJavaModel
 */
public class JavaModelInfo extends OpenableElementInfo {

/**
 * Compute the non-java resources contained in this java project.
 */
private Object[] computeNonJavaResources() {
	IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
	int length = projects.length;
	Object[] resources = null;
	int index = 0;
	for (int i = 0; i < length; i++) {
		IProject project = projects[i];
		if (!JavaProject.hasJavaNature(project)) {
			if (resources == null) {
				resources = new Object[length];
			}
			resources[index++] = project;
		}
	}
	if (index == 0) return NO_NON_JAVA_RESOURCES;
	if (index < length) {
		System.arraycopy(resources, 0, resources = new Object[index], 0, index);
	}
	return resources;
}

/**
 * Returns an array of non-java resources contained in the receiver.
 */
Object[] getNonJavaResources() {
	Object[] resources = this.nonJavaResources;
	if (resources == null) {
		resources = computeNonJavaResources();
		this.nonJavaResources = resources;
	}
	return resources;
}
}
