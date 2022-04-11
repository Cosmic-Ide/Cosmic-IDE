/*******************************************************************************
 * Copyright (c) 2004, 2006 IBM Corporation and others.
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
package org.eclipse.jdt.internal.codeassist;

/**
 * @deprecated Use {@link org.eclipse.jdt.core.CompletionRequestor} instead
 */
//TODO remove this class once no more clients
public interface IExtendedCompletionRequestor extends org.eclipse.jdt.core.ICompletionRequestor {
	void acceptPotentialMethodDeclaration(
			char[] declaringTypePackageName,
			char[] declaringTypeName,
			char[] selector,
			int completionStart,
			int completionEnd,
			int relevance);
}
