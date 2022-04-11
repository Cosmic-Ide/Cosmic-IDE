/*******************************************************************************
 * Copyright (c) 2004, 2010 IBM Corporation and others.
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

public class TypeParameterElementInfo extends SourceRefElementInfo {

	/*
	 * The start position of this type parameter's name in the its
	 * openable's buffer.
	 */
	public int nameStart= -1;

	/*
	 * The last position of this type parameter name in the its
	 * openable's buffer.
	 */
	public int nameEnd= -1;

	/*
	 * The bounds names of this type parameter.
	 */
	public char[][] bounds;

	/*
	 * The bounds' signatures for this type parameter.
	 */
	public char[][] boundsSignatures;
}
