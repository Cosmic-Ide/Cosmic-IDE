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

import java.io.InputStream;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.jdt.core.IJarEntryResource;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.internal.core.util.Util;

public class NonJavaResource  extends PlatformObject implements IJarEntryResource {

	private static final IJarEntryResource[] NO_CHILDREN = new IJarEntryResource[0];
	protected Object parent;
	protected IResource resource;

	public NonJavaResource(Object parent, IResource resource) {
		this.parent = parent;
		this.resource = resource;
	}

	@Override
	public boolean equals(Object obj) {
		if (! (obj instanceof NonJavaResource))
			return false;
		NonJavaResource other = (NonJavaResource) obj;
		return this.parent.equals(other.parent) && this.resource.equals(other.resource);
	}

	@Override
	public IJarEntryResource[] getChildren() {
		if (this.resource instanceof IContainer) {
			IResource[] members;
			try {
				members = ((IContainer) this.resource).members();
			} catch (CoreException e) {
				Util.log(e, "Could not retrieve children of " + this.resource.getFullPath()); //$NON-NLS-1$
				return NO_CHILDREN;
			}
			int length = members.length;
			if (length == 0)
				return NO_CHILDREN;
			IJarEntryResource[] children = new IJarEntryResource[length];
			for (int i = 0; i < length; i++) {
				children[i] = new NonJavaResource(this, members[i]);
			}
			return children;
		}
		return NO_CHILDREN;
	}

	@Override
	public InputStream getContents() throws CoreException {
		if (this.resource instanceof IFile)
			return ((IFile) this.resource).getContents();
		return null;
	}

	protected String getEntryName() {
		String parentEntryName;
		if (this.parent instanceof IPackageFragment) {
			String elementName = ((IPackageFragment) this.parent).getElementName();
			parentEntryName = elementName.length() == 0 ? "" : elementName .replace('.', '/') + '/'; //$NON-NLS-1$
		} else if (this.parent instanceof IPackageFragmentRoot) {
			parentEntryName = ""; //$NON-NLS-1$
		} else {
			parentEntryName = ((NonJavaResource) this.parent).getEntryName() + '/';
		}
		return parentEntryName + getName();
	}

	@Override
	public IPath getFullPath() {
		return new Path(getEntryName()).makeAbsolute();
	}

	@Override
	public String getName() {
		return this.resource.getName();
	}

	@Override
	public IPackageFragmentRoot getPackageFragmentRoot() {
		if (this.parent instanceof IPackageFragment) {
			return (IPackageFragmentRoot) ((IPackageFragment) this.parent).getParent();
		} else if (this.parent instanceof IPackageFragmentRoot) {
			return (IPackageFragmentRoot) this.parent;
		} else {
			return ((NonJavaResource) this.parent).getPackageFragmentRoot();
		}
	}

	@Override
	public Object getParent() {
		return this.parent;
	}

	@Override
	public int hashCode() {
		return Util.combineHashCodes(this.resource.hashCode(), this.parent.hashCode());
	}

	@Override
	public boolean isFile() {
		return this.resource instanceof IFile;
	}

	@Override
	public boolean isReadOnly() {
		return true;
	}

	@Override
	public String toString() {
		return "NonJavaResource["+getEntryName()+"]"; //$NON-NLS-1$ //$NON-NLS-2$
	}
}
