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

import org.eclipse.jdt.core.IJavaElement;

public class SourceMethodWithChildrenInfo extends SourceMethodInfo {

	protected IJavaElement[] children;

	public SourceMethodWithChildrenInfo(IJavaElement[] children) {
		this.children = children;
	}

	@Override
	public IJavaElement[] getChildren() {
		return this.children;
	}

}
