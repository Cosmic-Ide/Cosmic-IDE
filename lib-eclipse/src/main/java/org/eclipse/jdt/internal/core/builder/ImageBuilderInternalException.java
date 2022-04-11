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
package org.eclipse.jdt.internal.core.builder;

import org.eclipse.core.runtime.*;

/**
 * Exception thrown when there is an internal error in the image builder.
 * May wrapper another exception.
 */
public class ImageBuilderInternalException extends RuntimeException {

private static final long serialVersionUID = 28252254530437336L; // backward compatible
protected CoreException coreException;

public ImageBuilderInternalException(CoreException e) {
	this.coreException = e;
}

public CoreException getThrowable() {
	return this.coreException;
}

@Override
public void printStackTrace() {
	if (this.coreException != null) {
		System.err.println(this);
		System.err.println("Stack trace of embedded core exception:"); //$NON-NLS-1$
		this.coreException.printStackTrace();
	} else {
		super.printStackTrace();
	}
}
}
