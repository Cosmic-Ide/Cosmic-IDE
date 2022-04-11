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
 *     Vladimir Piskarev <pisv@1c.ru> - Thread safety of OpenableElementInfo - https://bugs.eclipse.org/450490
 *******************************************************************************/
package org.eclipse.jdt.internal.core;

import java.util.Arrays;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IModuleDescription;



/** Element info for IOpenable elements. */
public class OpenableElementInfo extends JavaElementInfo {

	/**
	 * Collection of handles of immediate children of this
	 * object. This is an empty array if this element has
	 * no children.
	 */
	protected volatile IJavaElement[] children = JavaElement.NO_ELEMENTS;
	protected IModuleDescription module;
	/**
	 * Is the structure of this element known
	 * @see IJavaElement#isStructureKnown()
	 */
	protected boolean isStructureKnown = false;

	/**
	 * A array with all the non-java resources contained by this element
	 */
	protected Object[] nonJavaResources;

	public void addChild(IJavaElement child) {
		IJavaElement[] oldChildren = this.children;
		int length = oldChildren.length;
		if (length == 0) {
			this.children = new IJavaElement[] {child};
		} else {
			for (int i = 0; i < length; i++) {
				if (oldChildren[i].equals(child))
					return; // already included
			}
			IJavaElement[] newChildren = new IJavaElement[length+1];
			System.arraycopy(oldChildren, 0, newChildren, 0, length);
			newChildren[length] = child;
			this.children = newChildren;
		}
	}

	@Override
	public IJavaElement[] getChildren() {
		return this.children;
	}

	/**
	 * @see IJavaElement#isStructureKnown()
	 */
	public boolean isStructureKnown() {
		return this.isStructureKnown;
	}

	public void removeChild(IJavaElement child) {
		IJavaElement[] oldChildren = this.children;
		for (int i = 0, length = oldChildren.length; i < length; i++) {
			if (oldChildren[i].equals(child)) {
				if (length == 1) {
					this.children = JavaElement.NO_ELEMENTS;
				} else {
					IJavaElement[] newChildren = new IJavaElement[length-1];
					System.arraycopy(oldChildren, 0, newChildren , 0, i);
					if (i < length-1)
						System.arraycopy(oldChildren, i+1, newChildren, i, length-1-i);
					this.children = newChildren;
				}
				break;
			}
		}
	}

	public void setChildren(IJavaElement[] children) {
		this.children= (children.length > 0) ? children : JavaElement.NO_ELEMENTS;
	}

	public void setModule(IModuleDescription module) {
		this.module = module;
	}
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getClass().getSimpleName());
		sb.append(" ["); //$NON-NLS-1$
		sb.append("isStructureKnown="); //$NON-NLS-1$
		sb.append(this.isStructureKnown);
		sb.append(", "); //$NON-NLS-1$
		if (this.module != null) {
			sb.append("module="); //$NON-NLS-1$
			sb.append(this.module);
			sb.append(", "); //$NON-NLS-1$
		}
		if (this.children != null) {
			sb.append("children="); //$NON-NLS-1$
			sb.append(Arrays.toString(this.children));
			sb.append(", "); //$NON-NLS-1$
		}
		if (this.nonJavaResources != null) {
			sb.append("nonJavaResources="); //$NON-NLS-1$
			sb.append(Arrays.toString(this.nonJavaResources));
		}
		sb.append("]"); //$NON-NLS-1$
		return sb.toString();
	}

	public IModuleDescription getModule() {
		return this.module;
	}
	/**
	 * Sets whether the structure of this element known
	 * @see IJavaElement#isStructureKnown()
	 */
	public void setIsStructureKnown(boolean newIsStructureKnown) {
		this.isStructureKnown = newIsStructureKnown;
	}
	/**
	 * Sets the nonJavaResources
	 */
	void setNonJavaResources(Object[] resources) {
		this.nonJavaResources = resources;
	}
}
