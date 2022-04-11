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
package org.eclipse.jdt.internal.core.jdom;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.jdom.*;
import org.eclipse.jdt.internal.core.util.Messages;
import org.eclipse.jdt.internal.core.util.CharArrayBuffer;
import org.eclipse.jdt.internal.core.util.Util;

/**
 * DOMPackage provides an implementation of IDOMPackage.
 *
 * @see IDOMPackage
 * @see DOMNode
 * @deprecated The JDOM was made obsolete by the addition in 2.0 of the more
 * powerful, fine-grained DOM/AST API found in the
 * org.eclipse.jdt.core.dom package.
 */
class DOMPackage extends DOMNode implements IDOMPackage {

/**
 * Creates an empty PACKAGE node.
 */
DOMPackage() {
	setMask(MASK_DETAILED_SOURCE_INDEXES, true);
}
/**
 * Creates a new simple PACKAGE document fragment on the given range of the document.
 *
 * @param document - the document containing this node's original contents
 * @param sourceRange - a two element array of integers describing the
 *		entire inclusive source range of this node within its document.
 * 		Contents start on and include the character at the first position.
 *		Contents end on and include the character at the last position.
 *		An array of -1's indicates this node's contents do not exist
 *		in the document.
 * @param name - the identifier portion of the name of this node, or
 *		<code>null</code> if this node does not have a name
 */
DOMPackage(char[] document, int[] sourceRange, String name) {
	super(document, sourceRange, name, new int[] {-1, -1});
	setMask(MASK_DETAILED_SOURCE_INDEXES, false);
}
/**
 * Creates a new detailed PACKAGE document fragment on the given range of the document.
 *
 * @param document - the document containing this node's original contents
 * @param sourceRange - a two element array of integers describing the
 *		entire inclusive source range of this node within its document.
 * 		Contents start on and include the character at the first position.
 *		Contents end on and include the character at the last position.
 *		An array of -1's indicates this node's contents do not exist
 *		in the document.
 * @param name - the identifier portion of the name of this node, or
 *		<code>null</code> if this node does not have a name
 * @param nameRange - a two element array of integers describing the
 *		entire inclusive source range of this node's name within its document,
 *		including any array qualifiers that might immediately follow the name
 *		or -1's if this node does not have a name.
 */
DOMPackage(char[] document, int[] sourceRange, String name, int[] nameRange) {
	super(document, sourceRange, name, nameRange);
	setMask(MASK_DETAILED_SOURCE_INDEXES, true);
}
/**
 * @see DOMNode#appendFragmentedContents(CharArrayBuffer)
 */
@Override
protected void appendFragmentedContents(CharArrayBuffer buffer) {
	if (this.fNameRange[0] < 0) {
		String lineSeparator = Util.getLineSeparator(buffer.toString(), null);
		buffer
			.append("package ") //$NON-NLS-1$
			.append(this.fName)
			.append(';')
			.append(lineSeparator)
			.append(lineSeparator);
	} else {
		buffer
			.append(this.fDocument, this.fSourceRange[0], this.fNameRange[0] - this.fSourceRange[0])
			.append(this.fName)
			.append(this.fDocument, this.fNameRange[1] + 1, this.fSourceRange[1] - this.fNameRange[1]);
	}
}
/**
 * @see IDOMNode#getContents()
 */
@Override
public String getContents() {
	if (this.fName == null) {
		return null;
	} else {
		return super.getContents();
	}
}
/**
 * @see DOMNode#getDetailedNode()
 */
@Override
protected DOMNode getDetailedNode() {
	return (DOMNode)getFactory().createPackage(getContents());
}
/**
 * @see IDOMNode#getJavaElement
 */
@Override
public IJavaElement getJavaElement(IJavaElement parent) throws IllegalArgumentException {
	if (parent.getElementType() == IJavaElement.COMPILATION_UNIT) {
		return ((ICompilationUnit)parent).getPackageDeclaration(getName());
	} else {
		throw new IllegalArgumentException(Messages.element_illegalParent);
	}
}
/**
 * @see IDOMNode#getNodeType()
 */
@Override
public int getNodeType() {
	return IDOMNode.PACKAGE;
}
/**
 * @see DOMNode
 */
@Override
protected DOMNode newDOMNode() {
	return new DOMPackage();
}
/**
 * @see IDOMNode#setName
 */
@Override
public void setName(String name) {
	becomeDetailed();
	super.setName(name);
}
/**
 * @see IDOMNode#toString()
 */
@Override
public String toString() {
	return "PACKAGE: " + getName(); //$NON-NLS-1$
}
}
