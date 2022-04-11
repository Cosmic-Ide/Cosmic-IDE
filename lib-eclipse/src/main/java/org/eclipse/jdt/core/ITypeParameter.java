/*******************************************************************************
 * Copyright (c) 2004, 2010 IBM Corporation and others.
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
package org.eclipse.jdt.core;


/**
 * Represents a type parameter defined by a type or a method
 * in a compilation unit or a class file.
 * <p>
 * Type parameters are obtained using {@link IType#getTypeParameter(String)} and
 * {@link IMethod#getTypeParameter(String)}.
 * </p><p>
 * Note that type parameters are not children of their declaring type or method. To get a list
 * of the type parameters use {@link IType#getTypeParameters()} for a type and use
 * {@link IMethod#getTypeParameters()} for a method.
 * </p>
 *
 * @since 3.1
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface ITypeParameter extends IJavaElement, ISourceReference {

	/**
	 * Returns the names of the class and interface bounds of this type parameter. Returns an empty
	 * array if this type parameter has no bounds. A bound name is the name as it appears in the
	 * source (without the <code>extends</code> keyword) if the type parameter comes from a
	 * compilation unit. It is the dot-separated fully qualified name of the bound if the type
	 * parameter comes from a class file.
	 *
	 * @return the names of the bounds
	 * @throws JavaModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource
	 */
	String[] getBounds() throws JavaModelException;

	/**
	 * Returns the signatures for this type parameter's bounds. The type parameter may have
	 * been declared as part of a type or a method. The signatures represent only the individual
	 * bounds and do not include the type variable name or the <code>extends</code> keyword.
	 * The signatures may be either unresolved (for source types) or resolved (for binary types).
	 * See {@link Signature} for details.
	 *
	 * @return the signatures for the bounds of this formal type parameter
	 * @throws JavaModelException
	 *             if this element does not exist or if an exception occurs while accessing its corresponding resource.
	 * @see Signature
	 * @since 3.6
	 */
	String[] getBoundsSignatures() throws JavaModelException;

	/**
	 * Returns the declaring member of this type parameter. This can be either an <code>IType</code>
	 * or an <code>IMethod</code>.
	 * <p>
	 * This is a handle-only method.
	 * </p>
	 *
	 * @return the declaring member of this type parameter.
	 */
	IMember getDeclaringMember();

	/**
	 * Returns the Java type root in which this type parameter is declared.
	 * <p>
	 * This is a handle-only method.
	 * </p>
	 *
	 * @return the Java type root in which this type parameter is declared
	 * @since 3.7
	 */
	ITypeRoot getTypeRoot();
}
