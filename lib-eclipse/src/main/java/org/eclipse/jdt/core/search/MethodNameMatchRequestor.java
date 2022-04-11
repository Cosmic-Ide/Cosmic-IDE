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
package org.eclipse.jdt.core.search;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * A <code>MethodNameMatchRequestor</code> collects matches from a <code>searchAllMethodNames</code>
 * query to a <code>SearchEngine</code>. Clients must subclass this abstract class and pass an instance to the
 * {@link SearchEngine#searchAllMethodNames(
 * char[] packageName,
 * int pkgMatchRule,
 * char[] declaringQualification,
 * int declQualificationMatchRule,
 * char[] delcaringSimpleName,
 * int declSimpleNameMatchRule,
 * char[] methodName,
 * int methodMatchRule,
 * IJavaSearchScope scope,
 * MethodNameMatchRequestor methodRequestor,
 * int waitingPolicy,
 * IProgressMonitor progressMonitor)} method.
 * <p>
 * While {@link MethodNameRequestor} only reports method names information (e.g. package, enclosing types, method name, modifiers, etc.),
 * this class reports {@link MethodNameMatch} objects instead, which store this information and can return
 * an {@link org.eclipse.jdt.core.IMethod} handle.
 * </p>
 * <p>
 * This class may be subclassed by clients.
 * </p>
 * @see MethodNameMatch
 * @see MethodNameRequestor
 *
 * @since 3.12
 */
public abstract class MethodNameMatchRequestor {

	/**
	 * Accepts a method name match ({@link MethodNameMatch}) which contains a method
	 * information as package name, enclosing types names, method name, modifiers, etc.
	 *
	 * @param match the match which contains all method information
	 */
	public abstract void acceptMethodNameMatch(MethodNameMatch match);

}
