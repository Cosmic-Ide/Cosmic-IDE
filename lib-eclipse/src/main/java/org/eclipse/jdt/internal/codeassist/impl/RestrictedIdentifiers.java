/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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
package org.eclipse.jdt.internal.codeassist.impl;

public interface RestrictedIdentifiers {
	int COUNT = 4;

	char[] RECORD = "record".toCharArray();//$NON-NLS-1$
	char[] SEALED = "sealed".toCharArray();//$NON-NLS-1$
	char[] NON_SEALED = "non-sealed".toCharArray();//$NON-NLS-1$
	char[] PERMITS = "permits".toCharArray();//$NON-NLS-1$

}
