/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
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
package org.eclipse.jdt.internal.core.search;

import org.eclipse.jdt.internal.compiler.env.AccessRestriction;

/**
 * A <code>IRestrictedAccessMethodRequestor</code> collects search results from a <code>searchAllMethodDeclarations</code>
 * query to a <code>SearchBasicEngine</code> providing restricted access information of declaring type when a method is accepted.
 */
public interface IRestrictedAccessMethodRequestor {

	public void acceptMethod(
			char[] methodName,
			int parameterCount,
			char[] declaringQualification,
			char[] simpleTypeName,
			int typeModifiers,
			char[] packageName,
			char[] signature,
			char[][] parameterTypes,
			char[][] parameterNames,
			char[] returnType,
			int modifiers,
			String path,
			AccessRestriction access,
			int methodIndex);
}
