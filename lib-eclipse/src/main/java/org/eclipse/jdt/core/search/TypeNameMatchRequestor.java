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
package org.eclipse.jdt.core.search;

/**
 * A <code>TypeNameMatchRequestor</code> collects matches from a <code>searchAllTypeNames</code>
 * query to a <code>SearchEngine</code>. Clients must subclass this abstract class and pass an instance to the
 * {@link SearchEngine#searchAllTypeNames(
 *		char[] packageName,
 *		int packageMatchRule,
 *		char[] typeName,
 *		int typeMatchRule,
 *		int searchFor,
 *		IJavaSearchScope scope,
 *		TypeNameMatchRequestor nameMatchRequestor,
 *		int waitingPolicy,
 * 	org.eclipse.core.runtime.IProgressMonitor monitor)} method.
 * Only top-level and member types are reported. Local types are not reported.
 * <p>
 * While {@link TypeNameRequestor} only reports type names information (e.g. package, enclosing types, simple name, modifiers, etc.),
 * this class reports {@link TypeNameMatch} objects instead, which store this information and can return
 * an {@link org.eclipse.jdt.core.IType} handle.
 * </p>
 * <p>
 * This class may be subclassed by clients.
 * </p>
 * @see TypeNameMatch
 * @see TypeNameRequestor
 *
 * @since 3.3
 */
public abstract class TypeNameMatchRequestor {
	/**
	 * Accepts a type name match ({@link TypeNameMatch}) which contains top-level or a member type
	 * information as package name, enclosing types names, simple type name, modifiers, etc.
	 *
	 * @param match the match which contains all type information
	 */
	public abstract void acceptTypeNameMatch(TypeNameMatch match);
}
