/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
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

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.IOpenable;

/**
 * This class represents a null buffer. This buffer is used to represent a buffer for a class file
 * that has no source attached.
 */
public class NullBuffer extends Buffer {
	/**
	 * Creates a new null buffer on an underlying resource.
	 */
	public NullBuffer(IFile file, IOpenable owner, boolean readOnly) {
		super(file, owner, readOnly);
	}
}
