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
 * Represents an initializer. The corresponding syntactic
 * units are InstanceInitializer (JLS2 8.6) and StaticDeclaration (JLS2 8.7).
 * An initializer has no children and its parent is a type.
 *
 * @deprecated The JDOM was made obsolete by the addition in 2.0 of the more
 * powerful, fine-grained DOM/AST API found in the
 * org.eclipse.jdt.core.dom package.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IDOMInitializer extends IDOMMember {
/**
 * Returns the body of this initializer. The syntax for a body corresponds to
 * InstanceInitializer (JLS2 8.6) and StaticDeclaration (JLS2 8.7).
 *
 * @return an initializer body, including braces, or <code>null</code> if
 *   no body is present
 */
public String getBody();
/**
 * The <code>IDOMInitializer</code> refinement of this <code>IDOMNode</code>
 * method returns <code>null</code>. An initializer does not have a name.
 *
 * @return <code>null</code>
 */
@Override
public String getName();
/**
 * Sets the body of this initializer. The syntax for a body corresponds to
 * InstanceInitializer (JLS2 8.6) and StaticDeclaration (JLS2 8.7). No formatting
 * or syntax checking is performed on the body. Braces <b>must</b> be included.
 *
 * @param body an initializer body, including braces, or <code>null</code>
 *   indicating no body
 */
public void setBody(String body);
/**
 * The <code>IDOMInitializer</code> refinement of this <code>IDOMNode</code>
 * method does nothing.
 *
 * @param name the given name
 */
@Override
public void setName(String name);
}
