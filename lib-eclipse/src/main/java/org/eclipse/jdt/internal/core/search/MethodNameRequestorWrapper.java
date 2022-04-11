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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.MethodNameRequestor;
import org.eclipse.jdt.internal.compiler.env.AccessRestriction;

/**
 * Wrapper used to linke {@link IRestrictedAccessMethodRequestor} with {@link MethodNameRequestor}.
 * This wrapper specifically allows the usage of internal method {@link BasicSearchEngine#searchAllMethodNames(
 * char[] packageName,
 * int pkgMatchRule,
 * char[] declaringQualification,
 * int declQualificationMatchRule,
 * char[] delcaringSimpleName,
 * int declSimpleNameMatchRule,
 * char[] methodName,
 * int methodMatchRule,
 * IJavaSearchScope scope,
 * IRestrictedAccessMethodRequestor methodRequestor,
 * int waitingPolicy,
 * IProgressMonitor progressMonitor)} from API method
 * {@link org.eclipse.jdt.core.search.SearchEngine#searchAllMethodNames(
 * char[] packageName,
 * int pkgMatchRule,
 * char[] declaringQualification,
 * int declQualificationMatchRule,
 * char[] delcaringSimpleName,
 * int declSimpleNameMatchRule,
 * char[] methodName,
 * int methodMatchRule,
 * IJavaSearchScope scope,
 * MethodNameRequestor methodRequestor,
 * int waitingPolicy,
 * IProgressMonitor progressMonitor)}.
 *
 */
public class MethodNameRequestorWrapper implements IRestrictedAccessMethodRequestor {

	MethodNameRequestor requestor;

	public MethodNameRequestorWrapper(MethodNameRequestor requestor) {
		this.requestor = requestor;
	}

	@Override
	public void acceptMethod(char[] methodName, int parameterCount, char[] declaringQualification,
			char[] simpleTypeName, int typeModifiers, char[] packageName, char[] signature, char[][] parameterTypes,
			char[][] parameterNames, char[] returnType, int modifiers, String path,
			AccessRestriction access, int methodIndex) {
		this.requestor.acceptMethod(methodName, parameterCount, declaringQualification, simpleTypeName, typeModifiers,
				packageName, signature, parameterTypes, parameterNames, returnType, modifiers, path, methodIndex);
	}
}
