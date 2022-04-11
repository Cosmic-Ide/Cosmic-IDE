/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
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

import org.eclipse.jdt.core.*;

/**
 * A match collected while {@link SearchEngine searching} for
 * all type names methods using a {@link TypeNameRequestor requestor}.
 * <p>
 * The type of this match is available from {@link #getType()}.
 * </p>
 *
 * @noextend This class is not intended to be subclassed by clients.
 *
 * @see TypeNameMatchRequestor
 * @see SearchEngine#searchAllTypeNames(char[], int, char[], int, int, IJavaSearchScope, TypeNameMatchRequestor, int, org.eclipse.core.runtime.IProgressMonitor)
 * @see SearchEngine#searchAllTypeNames(char[][], char[][], IJavaSearchScope, TypeNameMatchRequestor, int, org.eclipse.core.runtime.IProgressMonitor)
 * @since 3.3
 */
public abstract class TypeNameMatch {

/**
 * Returns the accessibility of the type name match
 *
 * @see IAccessRule
 *
 * @return the accessibility of the type name which may be
 * 		{@link IAccessRule#K_ACCESSIBLE}, {@link IAccessRule#K_DISCOURAGED}
 * 		or {@link IAccessRule#K_NON_ACCESSIBLE}.
 * 		The default returned value is {@link IAccessRule#K_ACCESSIBLE}.
 *
 * @since 3.6
 */
public abstract int getAccessibility();

/**
 * Returns the matched type's fully qualified name using '.' character
 * as separator (e.g. package name + '.' enclosing type names + '.' simple name).
 *
 * @see #getType()
 * @see IType#getFullyQualifiedName(char)
 *
 * @throws NullPointerException if matched type is <code> null</code>
 * @return Fully qualified type name of the type
 */
public String getFullyQualifiedName() {
	return getType().getFullyQualifiedName('.');
}

/**
 * Returns the modifiers of the matched type.
 * <p>
 * This is a handle-only method as neither Java Model nor classpath
 * initialization is done while calling this method.
 *
 * @return the type modifiers
 */
public abstract int getModifiers();

/**
 * Returns the package fragment root of the stored type.
 * Package fragment root cannot be null and <strong>does</strong> exist.
 *
 * @see #getType()
 * @see IJavaElement#getAncestor(int)
 *
 * @throws NullPointerException if matched type is <code> null</code>
 * @return the existing java model package fragment root (i.e. cannot be <code>null</code>
 * 	and will return <code>true</code> to <code>exists()</code> message).
 */
public IPackageFragmentRoot getPackageFragmentRoot() {
	return (IPackageFragmentRoot) getType().getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
}

/**
 * Returns the package name of the stored type.
 *
 * @see #getType()
 * @see IType#getPackageFragment()
 *
 * @throws NullPointerException if matched type is <code> null</code>
 * @return the package name
 */
public String getPackageName() {
	return getType().getPackageFragment().getElementName();
}

/**
 * Returns the name of the stored type.
 *
 * @see #getType()
 * @see IJavaElement#getElementName()
 *
 * @throws NullPointerException if matched type is <code> null</code>
 * @return the type name
 */
public String getSimpleTypeName() {
	return getType().getElementName();
}

/**
 * Returns a java model type handle.
 * This handle may exist or not, but is not supposed to be <code>null</code>.
 * <p>
 * This is a handle-only method as neither Java Model nor classpath
 * initializations are done while calling this method.
 *
 * @see IType
 * @return the non-null handle on matched java model type.
 */
public abstract IType getType();

/**
 * Name of the type container using '.' character
 * as separator (e.g. package name + '.' + enclosing type names).
 *
 * @see #getType()
 * @see IMember#getDeclaringType()
 *
 * @throws NullPointerException if matched type is <code> null</code>
 * @return name of the type container
 */
public String getTypeContainerName() {
	IType outerType = getType().getDeclaringType();
	if (outerType != null) {
		return outerType.getFullyQualifiedName('.');
	} else {
		return getType().getPackageFragment().getElementName();
	}
}

/**
 * Returns the matched type's type qualified name using '.' character
 * as separator (e.g. enclosing type names + '.' + simple name).
 *
 * @see #getType()
 * @see IType#getTypeQualifiedName(char)
 *
 * @throws NullPointerException if matched type is <code> null</code>
 * @return fully qualified type name of the type
 */
public String getTypeQualifiedName() {
	return getType().getTypeQualifiedName('.');
}
}
