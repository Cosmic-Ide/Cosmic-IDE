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

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.JavaModelException;

/**
 * Element info for PackageFragments.
 */
class PackageFragmentInfo extends OpenableElementInfo {

/**
 * Create and initialize a new instance of the receiver
 */
public PackageFragmentInfo() {
	this.nonJavaResources = null;
}
/**
 */
boolean containsJavaResources() {
	return this.children.length != 0;
}
/**
 * Returns an array of non-java resources contained in the receiver.
 */
Object[] getNonJavaResources(IResource underlyingResource, PackageFragmentRoot rootHandle) {
	Object[] resources = this.nonJavaResources;
	if (resources == null) {
		try {
			resources =
				PackageFragmentRootInfo.computeFolderNonJavaResources(
					rootHandle,
					(IContainer)underlyingResource,
					rootHandle.fullInclusionPatternChars(),
					rootHandle.fullExclusionPatternChars());

		} catch (JavaModelException e) {
			// root doesn't exist: consider package has no nonJavaResources
			resources = NO_NON_JAVA_RESOURCES;
		}
		this.nonJavaResources = resources;
	}
	return resources;
}
}
