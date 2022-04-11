/*******************************************************************************
 * Copyright (c) 2000, 2021 IBM Corporation and others.
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
 *     IBM Corporation - added J2SE 1.5 support
 *******************************************************************************/
package org.eclipse.jdt.core;

/**
 * Represents a field declared in a type.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IField extends IMember, IAnnotatable {

/**
 * Returns the constant value associated with this field
 * or <code>null</code> if this field has none. To have a constant value, the field needs to be
 * final and initialized with a compile-time constant expression.
 * <p>
 * For types from source, this currently only works if the field initializer is a literal (returns
 * <code>null</code> for more complex constant expressions).
 * </p>
 * <p>
 * For primitive types, returns the boxed value.
 * </p>
 *
 * @return  the constant value associated with this field, or <code>null</code> if not available
 * @exception JavaModelException if this element does not exist or if an
 *      exception occurs while accessing its corresponding resource
 */
public Object getConstant() throws JavaModelException;
/**
 * Returns the simple name of this field.
 * @return the simple name of this field.
 */
@Override
String getElementName();
/**
 * Returns the binding key for this field only if the given field is {@link #isResolved() resolved}.
 * A binding key is a key that uniquely identifies this field. It allows access to generic info
 * for parameterized fields.
 *
 * <p>If the given field is not resolved, the returned key is simply the java element's key.
 * </p>
 * @return the binding key for this field
 * @see org.eclipse.jdt.core.dom.IBinding#getKey()
 * @see BindingKey
 * @see #isResolved()
 * @since 3.1
 */
String getKey();
/**
 * Returns the type signature of this field. For enum constants,
 * this returns the signature of the declaring enum class.
 * <p>
 * The type signature may be either unresolved (for source types)
 * or resolved (for binary types), and either basic (for basic types)
 * or rich (for parameterized types). See {@link Signature} for details.
 * </p>
 *
 * @return the type signature of this field
 * @exception JavaModelException if this element does not exist or if an
 *      exception occurs while accessing its corresponding resource
 * @see Signature
 */
String getTypeSignature() throws JavaModelException;
/**
 * Returns whether this field represents an enum constant.
 *
 * @return whether this field represents an enum constant
 * @exception JavaModelException if this element does not exist or if an
 *      exception occurs while accessing its corresponding resource
 * @since 3.1
 */
boolean isEnumConstant() throws JavaModelException;
/**
 * Returns whether this field represents a resolved field.
 * If a field is resolved, its key contains resolved information.
 *
 * @return whether this field represents a resolved field.
 * @since 3.1
 */
boolean isResolved();
/**
 * Returns whether this field represents a record component.
 *
 * @return whether this field represents a record component.
 * @throws JavaModelException
 * @since 3.26
 */
boolean isRecordComponent() throws JavaModelException;

}
