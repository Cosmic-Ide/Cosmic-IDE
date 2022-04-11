/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
 *     Stephan Herrmann - Contribution for bug 215139
 *******************************************************************************/
package org.eclipse.jdt.internal.core.search;

import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.TypeNameMatchRequestor;
import org.eclipse.jdt.core.search.TypeNameRequestor;
import org.eclipse.jdt.internal.compiler.env.AccessRestriction;

/**
 * Wrapper used to link {@link IRestrictedAccessTypeRequestor} with {@link TypeNameRequestor}.
 * This wrapper specifically allows usage of internal method {@link BasicSearchEngine#searchAllTypeNames(
 * 	char[] packageName,
 * 	int packageMatchRule,
 * 	char[] typeName,
 * 	int typeMatchRule,
 * 	int searchFor,
 * 	org.eclipse.jdt.core.search.IJavaSearchScope scope,
 * 	IRestrictedAccessTypeRequestor nameRequestor,
 * 	int waitingPolicy,
 * 	org.eclipse.core.runtime.IProgressMonitor monitor) }.
 * from  API method {@link org.eclipse.jdt.core.search.SearchEngine#searchAllTypeNames(
 * 	char[] packageName,
 * 	int packageMatchRule,
 * 	char[] typeName,
 * 	int matchRule,
 * 	int searchFor,
 * 	org.eclipse.jdt.core.search.IJavaSearchScope scope,
 * 	TypeNameRequestor nameRequestor,
 * 	int waitingPolicy,
 * 	org.eclipse.core.runtime.IProgressMonitor monitor) }.
 */
public class TypeNameMatchRequestorWrapper extends NameMatchRequestorWrapper  implements IRestrictedAccessTypeRequestor {
	TypeNameMatchRequestor requestor;

public TypeNameMatchRequestorWrapper(TypeNameMatchRequestor requestor, IJavaSearchScope scope) {
	super(scope);
	this.requestor = requestor;
}

@Override
public void acceptType(int modifiers, char[] packageName, char[] simpleTypeName, char[][] enclosingTypeNames, String path, AccessRestriction access) {

	// Get type
	IType type = getType(modifiers, packageName, simpleTypeName, enclosingTypeNames, path, access);
	// Accept match if the type has been found
	if (type != null) {
		// hierarchy scopes require one more check:
		if (!(this.scope instanceof HierarchyScope) || ((HierarchyScope)this.scope).enclosesFineGrained(type)) {

			// Create the match
			final JavaSearchTypeNameMatch match = new JavaSearchTypeNameMatch(type, modifiers);

			// Update match accessibility
			if(access != null) {
				switch (access.getProblemId()) {
					case IProblem.ForbiddenReference:
						match.setAccessibility(IAccessRule.K_NON_ACCESSIBLE);
						break;
					case IProblem.DiscouragedReference:
						match.setAccessibility(IAccessRule.K_DISCOURAGED);
						break;
				}
			}
			// Accept match
			this.requestor.acceptTypeNameMatch(match);
		}
	}
}
}
