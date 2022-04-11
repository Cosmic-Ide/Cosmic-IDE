/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
package org.eclipse.jdt.internal.core.jdom;

import java.util.Enumeration;

import org.eclipse.jdt.core.jdom.*;

/**
 * SiblingEnumeration provides an enumeration on a linked list
 * of sibling DOM nodes.
 *
 * @see java.util.Enumeration
 * @deprecated The JDOM was made obsolete by the addition in 2.0 of the more
 * powerful, fine-grained DOM/AST API found in the
 * org.eclipse.jdt.core.dom package.
 */
@SuppressWarnings("rawtypes")
/* package */ class SiblingEnumeration implements Enumeration {

	/**
	 * The current location in the linked list
	 * of DOM nodes.
	 */
	protected IDOMNode fCurrentElement;
/**
 * Creates an enumeration of silbings starting at the given node.
 * If the given node is <code>null</code> the enumeration is empty.
 */
SiblingEnumeration(IDOMNode child) {
	this.fCurrentElement= child;
}
/**
 * @see java.util.Enumeration#hasMoreElements()
 */
@Override
public boolean hasMoreElements() {
	return this.fCurrentElement != null;
}
/**
 * @see java.util.Enumeration#nextElement()
 */
@Override
public Object nextElement() {
	IDOMNode curr=  this.fCurrentElement;
	if (curr != null) {
		this.fCurrentElement= this.fCurrentElement.getNextNode();
	}
	return curr;
}
}
