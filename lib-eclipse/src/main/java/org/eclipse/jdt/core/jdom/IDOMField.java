/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
package org.eclipse.jdt.core.jdom;

/**
 * Represents a field declaration. The corresponding
 * syntactic units are FieldDeclaration (JLS2 8.3) and ConstantDeclaration
 * (JLS2 9.3) restricted to a single VariableDeclarator clause.
 * A field has no children. The parent of a field is a type.
 *
 * @deprecated The JDOM was made obsolete by the addition in 2.0 of the more
 * powerful, fine-grained DOM/AST API found in the
 * org.eclipse.jdt.core.dom package.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IDOMField extends IDOMMember {
/**
 * Returns the initializer expression for this field.
 * The syntax for an initializer corresponds to VariableInitializer (JLS2 8.3).
 * <p>
 * Note: The expression does not include a "<code>=</code>".
 * </p>
 *
 * @return the initializer expression, or <code>null</code> if this field does
 *    not have an initializer
 */
public String getInitializer();
/**
 * The <code>IDOMField</code> refinement of this <code>IDOMNode</code>
 * method returns the name of this field. The syntax for the name of a field
 * corresponds to VariableDeclaratorId (JLS2 8.3).
 *
 * @return the name of this field
 */
@Override
public String getName();
/**
 * Returns the type name of this field. The syntax for a type name of a field
 * corresponds to Type in Field Declaration (JLS2 8.3).
 *
 * @return the type name
 */
public String getType();
/**
 * Sets the initializer expression for this field.
 * The syntax for an initializer corresponds to VariableInitializer (JLS2 8.3).
 * <p>
 * Note: The expression does not include a "<code>=</code>".
 * </p>
 *
 * @param initializer the initializer expression, or <code>null</code> indicating
 *   the field does not have an initializer
 */
public void setInitializer(String initializer);
/**
 * The <code>IDOMField</code> refinement of this <code>IDOMNode</code>
 * method sets the name of this field. The syntax for the name of a field
 * corresponds to VariableDeclaratorId (JLS2 8.3).
 *
 * @param name the given name
 * @exception IllegalArgumentException if <code>null</code> is specified
 */
@Override
public void setName(String name) throws IllegalArgumentException;
/**
 * Sets the type name of this field. The syntax for a type name of a field
 * corresponds to Type in Field Declaration (JLS2 8.3). Type names must be
 * specified as they should appear in source code. For example:
 * <code>"String"</code>, <code>"int[]"</code>, or <code>"java.io.File"</code>.
 *
 * @param typeName the type name
 * @exception IllegalArgumentException if <code>null</code> is specified
 */
public void setType(String typeName) throws IllegalArgumentException;
}
