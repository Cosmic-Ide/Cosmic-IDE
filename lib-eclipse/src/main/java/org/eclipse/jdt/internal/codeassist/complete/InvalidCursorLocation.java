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
package org.eclipse.jdt.internal.codeassist.complete;

/**
 * Thrown whenever cursor location is not inside a consistent token
 * for example: inside a string, number, unicode, comments etc...
 */
public class InvalidCursorLocation extends RuntimeException {

	public String irritant;

	/* Possible irritants */
	public static final String NO_COMPLETION_INSIDE_UNICODE = "No Completion Inside Unicode"; //$NON-NLS-1$
	public static final String NO_COMPLETION_INSIDE_COMMENT = "No Completion Inside Comment";      //$NON-NLS-1$
	public static final String NO_COMPLETION_INSIDE_STRING = "No Completion Inside String";        //$NON-NLS-1$
	public static final String NO_COMPLETION_INSIDE_NUMBER = "No Completion Inside Number";        //$NON-NLS-1$

	private static final long serialVersionUID = -3443160725735779590L; // backward compatible

public InvalidCursorLocation(String irritant){
	this.irritant = irritant;
}
}
