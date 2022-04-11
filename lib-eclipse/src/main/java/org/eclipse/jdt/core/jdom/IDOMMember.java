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
 * An <code>IDOMMember</code> defines functionality common to nodes, which
 * can be members of types.
 *
 * @see IDOMType
 * @see IDOMMethod
 * @see IDOMField
 * @see IDOMInitializer
 * @deprecated The JDOM was made obsolete by the addition in 2.0 of the more
 * powerful, fine-grained DOM/AST API found in the
 * org.eclipse.jdt.core.dom package.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IDOMMember extends IDOMNode {
/**
 * Returns the comment associated with this member (including comment delimiters).
 *
 * @return the comment, or <code>null</code> if this member has no associated
 *   comment
 */
public String getComment();
/**
 * Returns the flags for this member. The flags can be examined using the
 * <code>Flags</code> class.
 *
 * @return the flags
 * @see org.eclipse.jdt.core.Flags
 */
public int getFlags();
/**
 * Sets the comment associated with this member. The comment will appear
 * before the member in the source. The comment must be properly formatted, including
 * delimiters. A <code>null</code> comment indicates no comment. This member's
 * deprecated flag is automatically set to reflect the deprecated tag in the
 * comment.
 *
 * @param comment the comment, including comment delimiters, or
 *   <code>null</code> indicating this member should have no associated comment
 * @see #setFlags(int)
 */
public void setComment(String comment);
/**
 * Sets the flags for this member. The flags can be examined using the
 * <code>Flags</code> class. The deprecated flag passed in is ignored.
 *
 * @param flags the flags
 * @see org.eclipse.jdt.core.Flags
 */
public void setFlags(int flags);
}
