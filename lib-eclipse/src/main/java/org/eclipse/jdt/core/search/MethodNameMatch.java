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

import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IMethod;

/**
 * A match collected while {@link SearchEngine searching} for
 * all type names methods using a {@link MethodNameRequestor requestor}.
 * <p>
 * The method of this match is available from {@link #getMethod()}.
 * </p>
 *
 * @noextend This class is not intended to be subclassed by clients.
 *
 * @see MethodNameMatchRequestor
 * @see SearchEngine#searchAllMethodNames(char[], int, char[], int, char[], int, char[], int, IJavaSearchScope, MethodNameMatchRequestor, int, org.eclipse.core.runtime.IProgressMonitor)
 * @since 3.12
 */
public abstract class MethodNameMatch {

	/**
	 * Returns the accessibility of the declaring type of the method name match
	 *
	 * @see IAccessRule
	 *
	 * @return the accessibility of the declaring type of the method name which may be
	 * 		{@link IAccessRule#K_ACCESSIBLE}, {@link IAccessRule#K_DISCOURAGED}
	 * 		or {@link IAccessRule#K_NON_ACCESSIBLE}.
	 * 		The default returned value is {@link IAccessRule#K_ACCESSIBLE}.
	 *
	 */
	public abstract int getAccessibility();

	/**
	 * Returns the modifiers of the matched method.
	 * <p>
	 * This is a handle-only method as neither Java Model nor classpath
	 * initialization is done while calling this method.
	 *
	 * @return the type modifiers
	 */
	public abstract int getModifiers();

	/**
	 * Returns a java model method handle.
	 * This handle may exist or not, but is not supposed to be <code>null</code>.
	 * <p>
	 * This is a handle-only method as neither Java Model nor classpath
	 * initializations are done while calling this method.
	 *
	 * @see IMethod
	 * @return the non-null handle on matched java model method.
	 */
	public abstract IMethod getMethod();

}