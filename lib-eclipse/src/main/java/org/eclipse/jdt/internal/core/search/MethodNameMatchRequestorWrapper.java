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
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.MethodNameMatchRequestor;
import org.eclipse.jdt.core.search.MethodNameRequestor;
import org.eclipse.jdt.internal.compiler.env.AccessRestriction;
/**
 * Wrapper used to link {@link IRestrictedAccessMethodRequestor} with {@link MethodNameRequestor}.
 * This wrapper specifically allows usage of internal method {@link BasicSearchEngine#searchAllMethodNames(
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
 * IProgressMonitor progressMonitor)} from  API method
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
 */

public class MethodNameMatchRequestorWrapper extends NameMatchRequestorWrapper implements IRestrictedAccessMethodRequestor {

	MethodNameMatchRequestor requestor;

	public MethodNameMatchRequestorWrapper(MethodNameMatchRequestor requestor, IJavaSearchScope scope) {
		super(scope);
		this.requestor = requestor;
	}

	@Override
	public void acceptMethod(char[] methodName, int parameterCount, char[] declaringQualifier,
			char[] simpleTypeName, int typeModifiers, char[] packageName, char[] signature, char[][] parameterTypes,
			char[][] parameterNames, char[] returnType, int modifiers, String path,
			AccessRestriction access, int methodIndex) {
		// Get the type
		char[][] enclosingTypeNames = declaringQualifier != null && declaringQualifier.length > 0 ? CharOperation.splitOn('.', declaringQualifier) : CharOperation.NO_CHAR_CHAR;
		IType type = getType(typeModifiers, packageName, simpleTypeName, enclosingTypeNames, path, access);
		if (type == null) return;
		if (!(!(this.scope instanceof HierarchyScope) || ((HierarchyScope) this.scope).enclosesFineGrained(type))) return;
		parameterTypes = parameterTypes == null ? CharOperation.NO_CHAR_CHAR : parameterTypes;
		String[] paramTypeSigs = CharOperation.NO_STRINGS;
		if (signature != null) {
			char[][] parTypes = Signature.getParameterTypes(signature);
			if (parTypes.length > 0) {
				for (int i = 0, l = parTypes.length; i < l; ++i) {
					CharOperation.replace(parTypes[i], '/', '.');
				}
			}
			paramTypeSigs = CharOperation.toStrings(parTypes);
		} else if (parameterTypes.length > 0) {
			int l = parameterTypes.length;
			paramTypeSigs = new String[l];
			for (int i = 0; i < l; ++i) {
				paramTypeSigs[i] = Signature.createTypeSignature(parameterTypes[i], false);
			}
		}
		IMethod method = type.getMethod(new String(methodName), paramTypeSigs);
		this.requestor.acceptMethodNameMatch(new JavaSearchMethodNameMatch(method, modifiers));
	}
}
