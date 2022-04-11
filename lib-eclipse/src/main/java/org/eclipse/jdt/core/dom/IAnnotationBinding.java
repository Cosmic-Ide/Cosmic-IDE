/*******************************************************************************
 * Copyright (c) 2005, 2008 BEA Systems, Inc.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    tyeung@bea.com - initial API and implementation
 *    IBM Corporation - changed interface to extend IBinding
 *    IBM Corporation - renamed from IResolvedAnnotation to IAnnotationBinding
 *******************************************************************************/
package org.eclipse.jdt.core.dom;

/**
 * Represents a resolved annotation. Resolved annotations are computed along with other
 * bindings; they correspond to {@link Annotation} nodes.
 *
 * @since 3.2
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IAnnotationBinding extends IBinding {

	/**
	 * Returns the complete list of member value pairs for this annotation, including
	 * ones explicitly listed in the annotation as well as entries for
	 * annotation type members with default values that are implied.
	 *
	 * @return a possibly empty list of resolved member value pairs
	 */
	IMemberValuePairBinding[] getAllMemberValuePairs();

	/**
	 * Returns the type of the annotation. The resulting type binding will always
	 * return <code>true</code>	to <code>ITypeBinding.isAnnotation()</code>.
	 *
	 * @return the type of the annotation
	 */
	ITypeBinding getAnnotationType();

	/**
	 * Returns the list of declared member value pairs for this annotation.
	 * Returns an empty list for a {@link MarkerAnnotation}, a one element
	 * list for a {@link SingleMemberAnnotation}, and one entry for each
	 * of the explicitly listed values in a {@link NormalAnnotation}.
	 * <p>
	 * Note that the list only includes entries for annotation type members that are
	 * explicitly mentioned in the annotation. The list does not include any
	 * annotation type members with default values that are merely implied.
	 * Use {@link #getAllMemberValuePairs()} to get those as well.
	 * </p>
	 *
	 * @return a possibly empty list of resolved member value pairs
	 */
	IMemberValuePairBinding[] getDeclaredMemberValuePairs();

	/**
	 * Returns the name of the annotation type.
	 *
	 * @return the name of the annotation type
	 */
	@Override
	public String getName();

}
