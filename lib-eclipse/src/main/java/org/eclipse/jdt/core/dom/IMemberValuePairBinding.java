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
 *    IBM Corporation - renamed from IResolvedMemberValuePair to IMemberValuePairBinding
 *******************************************************************************/
package org.eclipse.jdt.core.dom;

/**
 * Represents a resolved instance of an annotation's member value pair.
 * Resolved annotation are computed along with other bindings; these objects
 * correspond to {@link MemberValuePair} nodes.
 *
 * @since 3.2
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IMemberValuePairBinding extends IBinding {
/**
 * Returns the name of the annotation type member.
 *
 * @return the name of the member
 */
@Override
public String getName();

/**
 * Returns the method binding corresponding to the named annotation type member.
 *
 * @return the method binding for the annotation type member
 */
public IMethodBinding getMethodBinding();

/**
 * Returns the resolved value. Resolved values are represented as follows:
 * <ul>
 * <li>Primitive type - the equivalent boxed object</li>
 * <li>java.lang.Class - the <code>ITypeBinding</code> for the class object</li>
 * <li>java.lang.String - the string value itself</li>
 * <li>enum type - the <code>IVariableBinding</code> for the enum constant</li>
 * <li>annotation type - an <code>IAnnotationBinding</code></li>
 * <li>array type - an <code>Object[]</code> whose elements are as per above
 * (the language only allows single dimensional arrays in annotations)</li>
 * </ul>
 *
 * @return the resolved value, or <code>null</code> if none exists
 */
public Object getValue();

/**
 * @return <code>true</code> iff this member value pair's value is the default value.
 *         Returns <code>false</code> otherwise.
 */
public boolean isDefault();
}
