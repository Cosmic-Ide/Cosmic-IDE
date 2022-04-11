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

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.jdom.*;
import org.eclipse.jdt.internal.core.util.Messages;
import org.eclipse.jdt.internal.core.util.CharArrayBuffer;
import org.eclipse.jdt.internal.core.util.Util;

/**
 * DOMImport provides an implementation of IDOMImport.
 *
 * @see IDOMImport
 * @see DOMNode
 * @deprecated The JDOM was made obsolete by the addition in 2.0 of the more
 * powerful, fine-grained DOM/AST API found in the
 * org.eclipse.jdt.core.dom package.
 */
// TODO (jerome) - add implementation support for 1.5 features
class DOMImport extends DOMNode implements IDOMImport {

	/**
	 * Indicates if this import is an on demand type import
	 */
	protected boolean fOnDemand;

	/**
	 * Modifiers for this import.
	 * @since 3.0
	 */
	protected int fFlags = Flags.AccDefault;

/**
 * Creates a new empty IMPORT node.
 */
DOMImport() {
	this.fName = "java.lang.*"; //$NON-NLS-1$
	setMask(MASK_DETAILED_SOURCE_INDEXES, true);
}
/**
 * Creates a new detailed IMPORT document fragment on the given range of the document.
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
 * @param onDemand - indicates if this import is an on demand style import
 */
DOMImport(char[] document, int[] sourceRange, String name, int[] nameRange, boolean onDemand, int modifiers) {
	super(document, sourceRange, name, nameRange);
	this.fOnDemand = onDemand;
	this.fFlags = modifiers;
	setMask(MASK_DETAILED_SOURCE_INDEXES, true);
}
/**
 * Creates a new simple IMPORT document fragment on the given range of the document.
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
 * @param onDemand - indicates if this import is an on demand style import
 */
DOMImport(char[] document, int[] sourceRange, String name, boolean onDemand, int modifiers) {
	this(document, sourceRange, name, new int[] {-1, -1}, onDemand, modifiers);
	this.fOnDemand = onDemand;
	setMask(MASK_DETAILED_SOURCE_INDEXES, false);
}
/**
 * @see DOMNode#appendFragmentedContents(CharArrayBuffer)
 */
@Override
protected void appendFragmentedContents(CharArrayBuffer buffer) {
	if (this.fNameRange[0] < 0) {
		buffer
			.append("import ") //$NON-NLS-1$
			.append(this.fName)
			.append(';')
			.append(Util.getLineSeparator(buffer.toString(), null));
	} else {
		buffer.append(this.fDocument, this.fSourceRange[0], this.fNameRange[0] - this.fSourceRange[0]);
		//buffer.append(fDocument, fNameRange[0], fNameRange[1] - fNameRange[0] + 1);
		buffer.append(this.fName);
		buffer.append(this.fDocument, this.fNameRange[1] + 1, this.fSourceRange[1] - this.fNameRange[1]);
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
	return (DOMNode)getFactory().createImport(getContents());
}
/**
 * @see IDOMNode#getJavaElement
 */
@Override
public IJavaElement getJavaElement(IJavaElement parent) throws IllegalArgumentException {
	if (parent.getElementType() == IJavaElement.COMPILATION_UNIT) {
		return ((ICompilationUnit)parent).getImport(getName());
	} else {
		throw new IllegalArgumentException(Messages.element_illegalParent);
	}
}
/**
 * @see IDOMNode#getNodeType()
 */
@Override
public int getNodeType() {
	return IDOMNode.IMPORT;
}
/**
 * @see IDOMImport#isOnDemand()
 */
@Override
public boolean isOnDemand() {
	return this.fOnDemand;
}
/**
 * @see DOMNode
 */
@Override
protected DOMNode newDOMNode() {
	return new DOMImport();
}
/**
 * @see IDOMNode#setName(String)
 */
@Override
public void setName(String name) {
	if (name == null) {
		throw new IllegalArgumentException(Messages.element_nullName);
	}
	becomeDetailed();
	super.setName(name);
	this.fOnDemand = name.endsWith(".*"); //$NON-NLS-1$
}
/**
 * @see IDOMNode#toString()
 */
@Override
public String toString() {
	return "IMPORT: " + getName(); //$NON-NLS-1$
}

/**
 * @see IDOMImport#getFlags()
 * @since 3.0
 */
@Override
public int getFlags() {
	return this.fFlags;
}

/**
 * @see IDOMImport#setFlags(int)
 * @since 3.0
 */
@Override
public void setFlags(int flags) {
	this.fFlags = flags;
}
}
