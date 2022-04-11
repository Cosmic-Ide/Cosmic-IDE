/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
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
 * A <code>IRestrictedAccessConstructorRequestor</code> collects search results from a <code>searchAllConstructorDeclarations</code>
 * query to a <code>SearchBasicEngine</code> providing restricted access information of declaring type when a constructor is accepted.
 */
public interface IRestrictedAccessConstructorRequestor {

	public void acceptConstructor(
			int modifiers,
			char[] simpleTypeName,
			int parameterCount,
			char[] signature,
			char[][] parameterTypes,
			char[][] parameterNames,
			int typeModifiers,
			char[] packageName,
			int extraFlags,
			String path,
			AccessRestriction access);

}
