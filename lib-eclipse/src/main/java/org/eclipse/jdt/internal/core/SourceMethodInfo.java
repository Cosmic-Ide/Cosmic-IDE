/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
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

/*
 * Element info for method from source.
 */
public class SourceMethodInfo extends SourceMethodElementInfo {

	/*
	 * Return type name for this method. The return type of
	 * constructors is equivalent to void.
	 */
	protected char[] returnType;

	@Override
	public boolean isAnnotationMethod() {
		return false;
	}

	@Override
	public boolean isConstructor() {
		return false;
	}

	@Override
	public char[] getReturnTypeName() {
		return this.returnType;
	}

	@Override
	protected void setReturnType(char[] type) {
		this.returnType = type;
	}

}
