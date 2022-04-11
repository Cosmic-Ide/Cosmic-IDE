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

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.jdom.*;
import org.eclipse.jdt.internal.core.util.Messages;
import org.eclipse.jdt.internal.core.util.CharArrayBuffer;
import org.eclipse.jdt.internal.core.util.Util;
/**
 * DOMInitializer provides an implementation of IDOMInitializer.
 *
 * @see IDOMInitializer
 * @see DOMNode
 * @deprecated The JDOM was made obsolete by the addition in 2.0 of the more
 * powerful, fine-grained DOM/AST API found in the
 * org.eclipse.jdt.core.dom package.
 */
class DOMInitializer extends DOMMember implements IDOMInitializer {

	/**
	 * The contents of the initializer's body when the
	 * body has been altered from the contents in the
	 * document, otherwise <code>null</code>.
	 */
	protected String fBody;

	/**
	 * The original inclusive source range of the
	 * body in the document.
	 */
	protected int[]  fBodyRange;

/**
 * Constructs an empty initializer node.
 */
DOMInitializer() {
	// Constructs an empty initializer node
}
/**
 * Creates a new detailed INITIALIZER document fragment on the given range of the document.
 *
 * @param document - the document containing this node's original contents
 * @param sourceRange - a two element array of integers describing the
 *		entire inclusive source range of this node within its document.
 * 		Contents start on and include the character at the first position.
 *		Contents end on and include the character at the last position.
 *		An array of -1's indicates this node's contents do not exist
 *		in the document.
 * @param commentRange - a two element array describing the comments that precede
 *		the member declaration. The first matches the start of this node's
 *		sourceRange, and the second is the new-line or first non-whitespace
 *		character following the last comment. If no comments are present,
 *		this array contains two -1's.
 * @param flags - an integer representing the modifiers for this member. The
 *		integer can be analyzed with org.eclipse.jdt.core.Flags
 * @param modifierRange - a two element array describing the location of
 *		modifiers for this member within its source range. The first integer
 *		is the first character of the first modifier for this member, and
 *		the second integer is the last whitespace character preceeding the
 *		next part of this member declaration. If there are no modifiers present
 *		in this node's source code (that is, package default visibility), this array
 *		contains two -1's.
 * @param bodyStartPosition - the position of the open brace of the body
 * 		of this initialzer.
 */
DOMInitializer(char[] document, int[] sourceRange, int[] commentRange, int flags, int[] modifierRange, int bodyStartPosition) {
	super(document, sourceRange, null, new int[]{-1, -1}, commentRange, flags, modifierRange);
	this.fBodyRange= new int[2];
	this.fBodyRange[0]= bodyStartPosition;
	this.fBodyRange[1]= sourceRange[1];
	setHasBody(true);
	setMask(MASK_DETAILED_SOURCE_INDEXES, true);
}
/**
 * Creates a new simple INITIALIZER document fragment on the given range of the document.
 *
 * @param document - the document containing this node's original contents
 * @param sourceRange - a two element array of integers describing the
 *		entire inclusive source range of this node within its document.
 * 		Contents start on and include the character at the first position.
 *		Contents end on and include the character at the last position.
 *		An array of -1's indicates this node's contents do not exist
 *		in the document.
 * @param flags - an integer representing the modifiers for this member. The
 *		integer can be analyzed with org.eclipse.jdt.core.Flags
 */
DOMInitializer(char[] document, int[] sourceRange, int flags) {
	this(document, sourceRange, new int[] {-1, -1}, flags, new int[] {-1, -1}, -1);
	setMask(MASK_DETAILED_SOURCE_INDEXES, false);

}
/**
 * @see DOMMember#appendMemberBodyContents(CharArrayBuffer)
 */
@Override
protected void appendMemberBodyContents(CharArrayBuffer buffer) {
	if (hasBody()) {
		buffer
			.append(getBody())
			.append(this.fDocument, this.fBodyRange[1] + 1, this.fSourceRange[1] - this.fBodyRange[1]);
	} else {
		buffer.append("{}").append(Util.getLineSeparator(buffer.toString(), null)); //$NON-NLS-1$
	}
}
/**
 * @see DOMMember#appendMemberDeclarationContents(CharArrayBuffer)
 */
@Override
protected void appendMemberDeclarationContents(CharArrayBuffer buffer) {
	// nothing to do
}
/**
 * @see DOMMember#appendSimpleContents(CharArrayBuffer)
 */
@Override
protected void appendSimpleContents(CharArrayBuffer buffer) {
	// append eveything before my name
	buffer.append(this.fDocument, this.fSourceRange[0], this.fNameRange[0] - this.fSourceRange[0]);
	// append my name
	buffer.append(this.fName);
	// append everything after my name
	buffer.append(this.fDocument, this.fNameRange[1] + 1, this.fSourceRange[1] - this.fNameRange[1]);
}
/**
 * @see IDOMInitializer#getBody()
 */
@Override
public String getBody() {
	becomeDetailed();
	if (hasBody()) {
		if (this.fBody != null) {
			return this.fBody;
		} else {
			return new String(this.fDocument, this.fBodyRange[0], this.fBodyRange[1] + 1 - this.fBodyRange[0]);
		}
	} else {
		return null;
	}
}
/**
 * @see DOMNode#getDetailedNode()
 */
@Override
protected DOMNode getDetailedNode() {
	return (DOMNode)getFactory().createInitializer(getContents());
}
/**
 * @see IDOMNode#getJavaElement
 */
@Override
public IJavaElement getJavaElement(IJavaElement parent) throws IllegalArgumentException {
	if (parent.getElementType() == IJavaElement.TYPE) {
		int count = 1;
		IDOMNode previousNode = getPreviousNode();
		while (previousNode != null) {
			if (previousNode instanceof DOMInitializer) {
				count++;
			}
			previousNode = previousNode.getPreviousNode();
		}
		return ((IType) parent).getInitializer(count);
	} else {
		throw new IllegalArgumentException(Messages.element_illegalParent);
	}
}
/**
 * @see DOMMember#getMemberDeclarationStartPosition()
 */
@Override
protected int getMemberDeclarationStartPosition() {
	return this.fBodyRange[0];
}
/**
 * @see IDOMNode#getNodeType()
 */
@Override
public int getNodeType() {
	return IDOMNode.INITIALIZER;
}
/**
 * @see IDOMNode#isSignatureEqual(IDOMNode)
 *
 * <p>This method always answers false since an initializer
 * does not have a signature.
 */
@Override
public boolean isSignatureEqual(IDOMNode node) {
	return false;
}
/**
 * @see DOMNode
 */
@Override
protected DOMNode newDOMNode() {
	return new DOMInitializer();
}
/**
 * Offsets all the source indexes in this node by the given amount.
 */
@Override
protected void offset(int offset) {
	super.offset(offset);
	offsetRange(this.fBodyRange, offset);
}
/**
 * @see IDOMInitializer#setBody(String)
 */
@Override
public void setBody(String body) {
	becomeDetailed();
	this.fBody= body;
	setHasBody(body != null);
	fragment();
}
/**
 * @see IDOMInitializer#setName(String)
 */
@Override
public void setName(String name) {
	// initializers have no name
}
/**
 * @see DOMNode#shareContents(DOMNode)
 */
@Override
protected void shareContents(DOMNode node) {
	super.shareContents(node);
	DOMInitializer init= (DOMInitializer)node;
	this.fBody= init.fBody;
	this.fBodyRange= rangeCopy(init.fBodyRange);
}
/**
 * @see IDOMNode#toString()
 */
@Override
public String toString() {
	return "INITIALIZER"; //$NON-NLS-1$
}
}
