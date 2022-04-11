/*******************************************************************************
 * Copyright (c) 2004, 2008 IBM Corporation and others.
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

import org.eclipse.jdt.core.IMemberValuePair;

/*
 * Element info for annotation method from source.
 */
public class SourceAnnotationMethodInfo extends SourceMethodInfo {

	/*
	 * The positions of a default member value of an annotation method.
	 * These are {-1, -1} if the method is an annotation method with no default value.
	 * Otherwise these are the start and end (inclusive) of the expression representing the default value.
	 */
	public int defaultValueStart = -1;
	public int defaultValueEnd = -1;

	public IMemberValuePair defaultValue;

	@Override
	public boolean isAnnotationMethod() {
		return true;
	}

}
