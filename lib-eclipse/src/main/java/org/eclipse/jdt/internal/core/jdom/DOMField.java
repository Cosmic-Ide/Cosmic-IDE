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

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.jdom.*;
import org.eclipse.jdt.internal.core.util.Messages;
import org.eclipse.jdt.internal.core.util.CharArrayBuffer;
import org.eclipse.jdt.internal.core.util.Util;
/**
 * DOMField provides an implementation of IDOMField.
 *
 * @see IDOMField
 * @see DOMNode
 * @deprecated The JDOM was made obsolete by the addition in 2.0 of the more
 * powerful, fine-grained DOM/AST API found in the
 * org.eclipse.jdt.core.dom package.
 */
@SuppressWarnings("rawtypes")
class DOMField extends DOMMember implements IDOMField {

	/**
	 * Contains the type of the field when the type
	 * has been altered from the contents in the
	 * document, otherwise <code>null</code>.
	 */
	protected String fType;

	/**
	 * The original inclusive source range of the
	 * field's type in the document.
	 */
	protected int[] fTypeRange;

	/**
	 * The contents of the initializer when the
	 * initializer has been altered from the
	 * original state in the document, otherwise
	 * <code>null</code>.
	 */
	protected String fInitializer;

	/**
	 * The original inclusive source range of the
	 * initializer in the document.
	 */
	protected int[] fInitializerRange;

/**
 * Constructs an empty field node.
 */
DOMField() {
	// Constructs an empty field node
}
/**
 * Creates a new detailed FIELD document fragment on the given range of the document.
 *
 * @param document - the document containing this node's original contents
 * @param sourceRange - a two element array of integers describing the
 *		entire inclusive source range of this node within its document.
 * 		Contents start on and include the character at the first position.
 *		Contents end on and include the character at the last position.
 *		An array of -1's indicates this node's contents do not exist
 *		in the document.
 * @param name - the identifier portion of the name of this field,
 * 		corresponding to VariableDeclaratorId (JLS 8.3).
 * @param nameRange - a two element array of integers describing the
 *		entire inclusive source range of this node's name within its document,
 *		including any array qualifiers that might follow the name.
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
 * @param typeRange - a two element array describing the location of the
 *		typeName in the document - the positions of the first and last characters
 *		of the typeName.
 * @param type - the type of the field, in normalized form, as defined in
 *      Type in Field Declaration (JLS 8.3)
 * @param hasInitializer - true if this field declaration includes an initializer,
 *		otherwise false
 * @param initRange - a two element array describing the location of the initializer
 *		in the declaration. The first integer is the position of the character
 *		following the equals sign, and the position of the last character is the last
 *		in the initializer. If this field has no initializer, this array contains
 *		two -1's.
 * @param isVariableDeclarator - true if the field is a seconday variable declarator
 * 		for a previous field declaration.
 */
DOMField(char[] document, int[] sourceRange, String name, int[] nameRange, int[] commentRange, int flags, int[] modifierRange, int[] typeRange, String type, boolean hasInitializer, int[] initRange, boolean isVariableDeclarator) {
	super(document, sourceRange, name, nameRange, commentRange, flags, modifierRange);

	this.fType= type;
	this.fTypeRange= typeRange;
	setHasInitializer(hasInitializer);
	this.fInitializerRange= initRange;
	setIsVariableDeclarator(isVariableDeclarator);
	setMask(MASK_DETAILED_SOURCE_INDEXES, true);

}
/**
 * Creates a new simple FIELD document fragment on the given range of the document.
 *
 * @param document - the document containing this node's original contents
 * @param sourceRange - a two element array of integers describing the
 *		entire inclusive source range of this node within its document.
 * 		Contents start on and include the character at the first position.
 *		Contents end on and include the character at the last position.
 *		An array of -1's indicates this node's contents do not exist
 *		in the document.
 * @param name - the identifier portion of the name of this field,
 * 		corresponding to VariableDeclaratorId (JLS 8.3).
 * @param nameRange - a two element array of integers describing the
 *		entire inclusive source range of this node's name within its document,
 *		including any array qualifiers that might follow the name.
 * @param flags - an integer representing the modifiers for this member. The
 *		integer can be analyzed with org.eclipse.jdt.core.Flags
 * @param type - the type of the field, in normalized form, as defined in
 *      Type in Field Declaration (JLS 8.3)
 * @param isVariableDeclarator - true if the field is a seconday variable declarator
 * 		for a previous field declaration.
 */
DOMField(char[] document, int[] sourceRange, String name, int[] nameRange, int flags, String type, boolean isVariableDeclarator) {
	this(document, sourceRange, name, nameRange, new int[] {-1, -1}, flags, new int[] {-1, -1}, new int[] {-1, -1}, type, false, new int[] {-1, -1}, isVariableDeclarator);
	setMask(MASK_DETAILED_SOURCE_INDEXES, false);
}
/**
 * Appends this member's body contents to the given CharArrayBuffer.
 * Body contents include the member body and any trailing whitespace.
 *
 * <p>A field does not have a body.
 *
 * @see DOMMember#appendMemberBodyContents(CharArrayBuffer)
 */
@Override
protected void appendMemberBodyContents(CharArrayBuffer buffer) {
	// nothing to do
}
/**
 * @see DOMMember#appendMemberDeclarationContents(CharArrayBuffer)
 */
@Override
protected void appendMemberDeclarationContents(CharArrayBuffer buffer) {

	if (isVariableDeclarator()) {
		buffer.append(this.fDocument, this.fSourceRange[0], this.fNameRange[0] - this.fSourceRange[0]);
	} else {
		buffer
			.append(getTypeContents())
			.append(this.fDocument, this.fTypeRange[1] + 1, this.fNameRange[0] - this.fTypeRange[1] - 1);
	}

	buffer.append(getNameContents());
	if (hasInitializer()) {
		if (this.fInitializerRange[0] < 0) {
			buffer
				.append('=')
				.append(this.fInitializer)
				.append(this.fDocument, this.fNameRange[1] + 1, this.fSourceRange[1] - this.fNameRange[1]);
		} else {
			buffer
				.append(this.fDocument, this.fNameRange[1] + 1, this.fInitializerRange[0] - this.fNameRange[1] - 1)
				.append(getInitializer())
				.append(this.fDocument, this.fInitializerRange[1] + 1, this.fSourceRange[1] - this.fInitializerRange[1]);
		}
	} else {
		if (this.fInitializerRange[0] < 0) {
			buffer.append(this.fDocument, this.fNameRange[1] + 1, this.fSourceRange[1] - this.fNameRange[1]);
		} else {
			buffer.append(this.fDocument, this.fInitializerRange[1] + 1, this.fSourceRange[1] - this.fInitializerRange[1]);
		}
	}

}
/**
 * Appends this member's header contents to the given CharArrayBuffer.
 * Header contents include any preceding comments and modifiers.
 *
 * <p>If this field is a secondary variable declarator, there is no header.
 *
 * @see DOMMember#appendMemberHeaderFragment(CharArrayBuffer)
 */
@Override
protected void appendMemberHeaderFragment(CharArrayBuffer buffer) {

	if (isVariableDeclarator()) {
		return;
	} else {
		super.appendMemberHeaderFragment(buffer);
	}

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
 * Generates detailed source indexes for this node if possible.
 *
 * @exception DOMException if unable to generate detailed source indexes
 * 	for this node
 */
@Override
protected void becomeDetailed() throws DOMException {
	if (!isDetailed()) {
		if (isVariableDeclarator() || hasMultipleVariableDeclarators()) {
			DOMNode first = getFirstFieldDeclaration();
			DOMNode last = getLastFieldDeclaration();
			DOMNode node= first;
			String source= first.getContents();
			while (node != last) {
				node= node.fNextNode;
				source+=node.getContents();
			}
			DOMBuilder builder = new DOMBuilder();
			IDOMField[] details= builder.createFields(source.toCharArray());
			if (details.length == 0) {
				throw new DOMException(Messages.dom_cannotDetail);
			} else {
				node= this;
				for (int i= 0; i < details.length; i++) {
					node.shareContents((DOMNode)details[i]);
					node= node.fNextNode;
				}
			}
		} else {
			super.becomeDetailed();
		}

	}
}
/**
 * @see IDOMNode#clone()
 */
@Override
public Object clone() {
	if (isVariableDeclarator() || hasMultipleVariableDeclarators()) {
		return getFactory().createField(new String(getSingleVariableDeclaratorContents()));
	} else {
		return super.clone();
	}
}
/**
 * Expands all variable declarators in this field declaration into
 * stand-alone field declarations.
 */
protected void expand() {
	if (isVariableDeclarator() || hasMultipleVariableDeclarators()) {
		Enumeration siblings= new SiblingEnumeration(getFirstFieldDeclaration());
		DOMField field= (DOMField)siblings.nextElement();
		DOMNode next= field.fNextNode;
		while (siblings.hasMoreElements() && (next instanceof DOMField) && (((DOMField)next).isVariableDeclarator())) {
			field.localizeContents();
			if (field.fParent != null) {
				field.fParent.fragment();
			}
			field= (DOMField)siblings.nextElement();
			next= field.fNextNode;
		}
		field.localizeContents();
	}
}
/**
 * @see DOMNode#getDetailedNode()
 */
@Override
protected DOMNode getDetailedNode() {
	if (isVariableDeclarator() || hasMultipleVariableDeclarators()) {
		return (DOMNode)getFactory().createField(new String(getSingleVariableDeclaratorContents()));
	} else {
		return (DOMNode)getFactory().createField(getContents());
	}
}
/**
 * Returns the first field document fragment that defines
 * the type for this variable declarator.
 */
protected DOMField getFirstFieldDeclaration() {
	if (isVariableDeclarator()) {
		return ((DOMField)this.fPreviousNode).getFirstFieldDeclaration();
	} else {
		return this;
	}
}
/**
 * @see IDOMField#getInitializer()
 */
@Override
public String getInitializer() {
	becomeDetailed();
	if (hasInitializer()) {
		if (this.fInitializer != null) {
			return this.fInitializer;
		} else {
			return new String(this.fDocument, this.fInitializerRange[0], this.fInitializerRange[1] + 1 - this.fInitializerRange[0]);
		}
	} else {
		return null;
	}
}
/**
 * @see IDOMNode#getJavaElement
 */
@Override
public IJavaElement getJavaElement(IJavaElement parent) throws IllegalArgumentException {
	if (parent.getElementType() == IJavaElement.TYPE) {
		return ((IType)parent).getField(getName());
	} else {
		throw new IllegalArgumentException(Messages.element_illegalParent);
	}
}
/**
 * Returns the last field document fragment in this muli-declarator statement.
 */
protected DOMField getLastFieldDeclaration() {
	DOMField field = this;
	while (field.isVariableDeclarator() || field.hasMultipleVariableDeclarators()) {
		if (field.fNextNode instanceof DOMField && ((DOMField)field.fNextNode).isVariableDeclarator()) {
			field= (DOMField)field.fNextNode;
		} else {
			break;
		}
	}
	return field;
}
/**
 * @see DOMMember#getMemberDeclarationStartPosition()
 */
@Override
protected int getMemberDeclarationStartPosition() {
	return this.fTypeRange[0];
}
/**
 * @see IDOMNode#getNodeType()
 */
@Override
public int getNodeType() {
	return IDOMNode.FIELD;
}
/**
 * Returns a String representing this field declaration as a field
 * declaration with one variable declarator.
 */
protected char[] getSingleVariableDeclaratorContents() {


	CharArrayBuffer buffer= new CharArrayBuffer();
	DOMField first= getFirstFieldDeclaration();
	if (first.isDetailed()) {
		first.appendMemberHeaderFragment(buffer);
		buffer.append(getType());
		if (isVariableDeclarator()) {
			buffer.append(' ');
		} else {
			buffer.append(this.fDocument, this.fTypeRange[1] + 1, this.fNameRange[0] - this.fTypeRange[1] - 1);
		}
	} else {
		buffer.append(first.fDocument, first.fSourceRange[0], first.fNameRange[0] - first.fSourceRange[0]);
	}

	buffer.append(getName());
	if (hasInitializer()) {
		if (this.fInitializerRange[0] < 0) {
			buffer
				.append('=')
				.append(this.fInitializer)
				.append(';')
				.append(Util.getLineSeparator(buffer.toString(), null));
		} else {
			buffer
				.append(this.fDocument, this.fNameRange[1] + 1, this.fInitializerRange[0] - this.fNameRange[1] - 1)
				.append(getInitializer())
				.append(';')
				.append(Util.getLineSeparator(buffer.toString(), null));
		}
	} else {
		buffer.append(';').append(Util.getLineSeparator(buffer.toString(), null));
	}
	return buffer.getContents();
}
/**
 * @see IDOMField#getType()
 */
@Override
public String getType() {
	return this.fType;
}
/**
 * Returns the souce code to be used for this
 * field's type.
 */
protected char[] getTypeContents() {
	if (isTypeAltered()) {
		return this.fType.toCharArray();
	} else {
		return CharOperation.subarray(this.fDocument, this.fTypeRange[0], this.fTypeRange[1] + 1);
	}
}
/**
 * Returns true if this field has an initializer expression,
 * otherwise false.
 */
protected boolean hasInitializer() {
	return getMask(MASK_FIELD_HAS_INITIALIZER);
}
/**
 * Returns true is this field declarations has more than one
 * variable declarator, otherwise false;
 */
protected boolean hasMultipleVariableDeclarators() {
	return this.fNextNode != null && (this.fNextNode instanceof DOMField) &&
		((DOMField)this.fNextNode).isVariableDeclarator();
}
/**
 * Inserts the given un-parented node as a sibling of this node, immediately before
 * this node. Once inserted, the sibling is only dependent on this document fragment.
 *
 * <p>When a sibling is inserted before a variable declarator, it must first
 * be expanded.
 *
 * @see IDOMNode#insertSibling(IDOMNode)
 */
@Override
public void insertSibling(IDOMNode sibling) throws IllegalArgumentException, DOMException {
	if (isVariableDeclarator()) {
		expand();
	}
	super.insertSibling(sibling);
}
/**
 * Returns true if this field's type has been altered
 * from the original document contents.
 */
protected boolean isTypeAltered() {
	return getMask(MASK_FIELD_TYPE_ALTERED);
}
/**
 * Returns true if this field is declared as a secondary variable
 * declarator for a previous field declaration.
 */
protected boolean isVariableDeclarator() {
	return getMask(MASK_FIELD_IS_VARIABLE_DECLARATOR);
}
/**
 * @see DOMNode
 */
@Override
protected DOMNode newDOMNode() {
	return new DOMField();
}
/**
 * Normalizes this <code>DOMNode</code>'s end position.
 */
@Override
void normalizeEndPosition(ILineStartFinder finder, DOMNode next) {
	if (next == null) {
		// this node's end position includes all of the characters up
		// to the end of the enclosing node
		DOMNode parent = (DOMNode) getParent();
		if (parent == null || parent instanceof DOMCompilationUnit) {
			setSourceRangeEnd(this.fDocument.length - 1);
		} else {
			// parent is a type
			int temp = ((DOMType)parent).getCloseBodyPosition() - 1;
			setSourceRangeEnd(temp);
			this.fInsertionPosition = Math.max(finder.getLineStart(temp + 1), getEndPosition());
		}
	} else {
		// this node's end position is just before the start of the next node
		// unless the next node is a field that is declared along with this one
		int temp = next.getStartPosition() - 1;
		this.fInsertionPosition = Math.max(finder.getLineStart(temp + 1), getEndPosition());

		next.normalizeStartPosition(getEndPosition(), finder);
		if (next instanceof DOMField) {
			DOMField field = (DOMField) next;
			if (field.isVariableDeclarator() && this.fTypeRange[0] == field.fTypeRange[0])
				return;
		}
		setSourceRangeEnd(next.getStartPosition() - 1);
	}
}
/**
 * Normalizes this <code>DOMNode</code>'s start position.
 */
@Override
void normalizeStartPosition(int endPosition, ILineStartFinder finder) {
	if (isVariableDeclarator()) {
		// start position is end of last element
		setStartPosition(this.fPreviousNode.getEndPosition() + 1);
	} else {
		super.normalizeStartPosition(endPosition, finder);
	}
}
/**
 * Offsets all the source indexes in this node by the given amount.
 */
@Override
protected void offset(int offset) {
	super.offset(offset);
	offsetRange(this.fInitializerRange, offset);
	offsetRange(this.fTypeRange, offset);
}
/**
 * Separates this node from its parent and siblings, maintaining any ties that
 * this node has to the underlying document fragment.
 *
 * <p>When a field with multiple declarators is removed, its declaration
 * must first be expanded.
 *
 * @see IDOMNode#remove()
 */
@Override
public void remove() {
	expand();
	super.remove();
}
/**
 * @see IDOMMember#setComment(String)
 */
@Override
public void setComment(String comment) {
	expand();
	super.setComment(comment);
}
/**
 * @see IDOMMember#setFlags(int)
 */
@Override
public void setFlags(int flags) {
	expand();
	super.setFlags(flags);
}
/**
 * Sets the state of this field declaration as having
 * an initializer expression.
 */
protected void setHasInitializer(boolean hasInitializer) {
	setMask(MASK_FIELD_HAS_INITIALIZER, hasInitializer);
}
/**
 * @see IDOMField#setInitializer(String)
 */
@Override
public void setInitializer(String initializer) {
	becomeDetailed();
	fragment();
	setHasInitializer(initializer != null);
	this.fInitializer= initializer;
}
/**
 * Sets the initializer range.
 */
void setInitializerRange(int start, int end) {
	this.fInitializerRange[0] = start;
	this.fInitializerRange[1] = end;
}
/**
 * Sets the state of this field declaration as being a
 * secondary variable declarator for a previous field
 * declaration.
 */
protected void setIsVariableDeclarator(boolean isVariableDeclarator) {
	setMask(MASK_FIELD_IS_VARIABLE_DECLARATOR, isVariableDeclarator);
}
/**
 * @see IDOMField#setName(String)
 */
@Override
public void setName(String name) throws IllegalArgumentException {
	if (name == null) {
		throw new IllegalArgumentException(Messages.element_nullName);
	} else {
		super.setName(name);
		setTypeAltered(true);
	}
}
/**
 * @see IDOMField#setType(String)
 */
@Override
public void setType(String typeName) throws IllegalArgumentException {
	if (typeName == null) {
		throw new IllegalArgumentException(Messages.element_nullType);
	}
	becomeDetailed();
	expand();
	fragment();
	setTypeAltered(true);
	setNameAltered(true);
	this.fType= typeName;
}
/**
 * Sets the state of this field declaration as having
 * the field type altered from the original document.
 */
protected void setTypeAltered(boolean typeAltered) {
	setMask(MASK_FIELD_TYPE_ALTERED, typeAltered);
}
/**
 * @see DOMNode#shareContents(DOMNode)
 */
@Override
protected void shareContents(DOMNode node) {
	super.shareContents(node);
	DOMField field= (DOMField)node;
	this.fInitializer= field.fInitializer;
	this.fInitializerRange= rangeCopy(field.fInitializerRange);
	this.fType= field.fType;
	this.fTypeRange= rangeCopy(field.fTypeRange);
}
/**
 * @see IDOMNode#toString()
 */
@Override
public String toString() {
	return "FIELD: " + getName(); //$NON-NLS-1$
}
}
