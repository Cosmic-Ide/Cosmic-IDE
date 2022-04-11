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
package org.eclipse.jdt.core;

/**
 * Represents a member-value pair of an annotation.
 * The {@link #getValue() value} is represented by an {@link Object}. To get the exact
 * type of this object, use its {@link #getValueKind() value kind}. If this value is an array,
 * {@link #getValue()} returns an instance of {@link Object}[] and the value kind returns
 * the kind of the elements in this array.
 * <p>
 * This interface is not intended to be implemented or extended by clients.
 * </p>
 *
 * @since 3.4
 */
public interface IMemberValuePair {
	/**
	 * Constant indicating that the value kind is an <code>int</code> represented by
	 * an instance of {@link Integer}.
	 */
	int K_INT = 1;

	/**
	 * Constant indicating that the value kind is a <code>byte</code> represented by
	 * an instance of {@link Byte}.
	 */
	int K_BYTE = 2;

	/**
	 * Constant indicating that the value kind is a <code>short</code> represented by
	 * an instance of {@link Short}.
	 */
	int K_SHORT = 3;

	/**
	 * Constant indicating that the value kind is a <code>char</code> represented by
	 * an instance of {@link Character}.
	 */
	int K_CHAR = 4;

	/**
	 * Constant indicating that the value kind is a <code>float</code> represented by
	 * an instance of {@link Float}.
	 */
	int K_FLOAT = 5;

	/**
	 * Constant indicating that the value kind is a <code>double</code> represented by
	 * an instance of {@link Double}.
	 */
	int K_DOUBLE = 6;

	/**
	 * Constant indicating that the value kind is a <code>long</code> represented by
	 * an instance of {@link Long}.
	 */
	int K_LONG = 7;

	/**
	 * Constant indicating that the value kind is a <code>boolean</code> represented by
	 * an instance of {@link Boolean}.
	 */
	int K_BOOLEAN = 8;

	/**
	 * Constant indicating that the value kind is a {@link String} represented by
	 * the corresponding {@link String}.
	 */
	int K_STRING = 9;

	/**
	 * Constant indicating that the value kind is an annotation represented by
	 * an instance of {@link IAnnotation}.
	 */
	int K_ANNOTATION = 10;

	/**
	 * Constant indicating that the value kind is a {@link Class} represented by
	 * the name of the class (i.e. a {@link String}. If the member-value pair is coming from
	 * a compilation unit, this is either a simple name (e.g. for <code>MyType.class</code>,
	 * the name is "MyType"), or a qualified name  (e.g. for <code>x.y.MyType.MyNestedType.class</code>,
	 * the name is "x.y.MyType.MyNestedType"). If the member-value pair is coming from a class file, this is
	 * always a fully qualified name.
	 * <p>
	 * Note that one can use {@link IType#resolveType(String)} and e.g.
	 * {@link IJavaProject#findType(String, String, org.eclipse.core.runtime.IProgressMonitor)}
	 * to find the corresponding {@link IType}.
	 * </p>
	 */
	int K_CLASS = 11;

	/**
	 * Constant indicating that the value is a qualified name represented by a
	 * {@link String}. The qualified name refers to an enum constant or another
	 * compile-time constant if the code is correct (e.g. "MyEnum.FIRST").
	 */
	int K_QUALIFIED_NAME = 12;

	/**
	 * Constant indicating that the value is a simple name represented by a
	 * {@link String}. The simple name refers to an enum constant or another
	 * compile-time constant if the code is correct (e.g. "FIRST" when there is
	 * a static import for "MyEnum.FIRST").
	 */
	int K_SIMPLE_NAME = 13;

	/**
	 * Constant indicating that the value kind is unknown at this stage. The value is unknown in the
	 * following cases:
	 * <ul>
	 * <li>the value is an expression that would need to be further analyzed to determine its kind. For
	 * example, in <code>@MyAnnot(1 + 2.3)</code> the kind of the expression "1 + 2.3" is
	 * unknown</li>
	 * <li>the value is an array of size 0, e.g. <code>@MyAnnot({})</code></li>
	 * <li>the value is an array that contains at least one expression that would need to be further
	 *      analyzed to determine its kind. For example, in <code>@MyAnnot({3.4, 1 + 2.3})</code>,
	 *      the kind of the second element "1 + 2.3" is unknown.</li>
	 * <li>the value is an array that contains heterogeneous values, e.g.
	 *      <code>@MyAnnot({1, 2.3, "abc"})</code></li>
	 * </ul>
	 * If the value kind is unknown, the returned value is always either <code>null</code>, or an
	 * array containing {@link Object}s and/or <code>null</code>s for unknown elements.
	 */
	int K_UNKNOWN = 14;

	/**
	 * Returns the member's name of this member-value pair.
	 *
	 * @return the member's name of this member-value pair.
	 */
	String getMemberName();

	/**
	 * Returns the value of this member-value pair. The type of this value
	 * is function of this member-value pair's {@link #getValueKind() value kind}. It is an
	 * instance of {@link Object}[] if the value is an array.
	 * <p>
	 * If the value kind is {@link #K_UNKNOWN} and the value is not an array, then the
	 * value is <code>null</code>.
	 * If the value kind is {@link #K_UNKNOWN} and the value is an array, then the
	 * value is an array containing {@link Object}s and/or <code>null</code>s for
	 * unknown elements.
	 * See {@link #K_UNKNOWN} for more details.
	 * </p>
	 * @return the value of this member-value pair.
	 */
	Object getValue();

	/**
	 * Returns the value kind of this member-value pair. This indicates the instance of
	 * the returned {@link #getValue() value}, or the instance of the elements if the value
	 * is an array. The value kind is one of the following constants:
	 * {@link #K_ANNOTATION}, {@link #K_BOOLEAN}, {@link #K_BYTE}, {@link #K_CHAR},
	 * {@link #K_CLASS}, {@link #K_DOUBLE}, {@link #K_FLOAT}, {@link #K_INT}, {@link #K_LONG},
	 * {@link #K_QUALIFIED_NAME}, {@link #K_SIMPLE_NAME}, {@link #K_SHORT}, {@link #K_STRING},
	 * {@link #K_UNKNOWN}.
	 *
	 * @return the value kind of this member-value pair
	 */
	int getValueKind();

}
