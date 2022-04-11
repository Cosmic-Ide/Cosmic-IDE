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

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.eclipse.jdt.core.jdom.*;
import org.eclipse.jdt.internal.compiler.parser.Scanner;
import org.eclipse.jdt.internal.compiler.parser.TerminalTokens;
import org.eclipse.jdt.internal.core.util.CharArrayBuffer;
import org.eclipse.jdt.internal.core.util.Messages;
/**
 * DOMType provides an implementation of IDOMType.
 *
 * @see IDOMType
 * @see DOMNode
 * @deprecated The JDOM was made obsolete by the addition in 2.0 of the more
 * powerful, fine-grained DOM/AST API found in the
 * org.eclipse.jdt.core.dom package.
 */
@SuppressWarnings("rawtypes")
// TODO (jerome) - add implementation support for 1.5 features
/* package */ class DOMType extends DOMMember implements IDOMType {
	/**
	 * The 'class' or 'interface' keyword if altered
	 * from the documents contents, otherwise <code>null</code>.
	 */
	protected String fTypeKeyword;

	/**
	 * The original inclusive source range of the 'class'
	 * or 'interface' keyword in the document.
	 */
	protected int[]	 fTypeRange;

	/**
	 * The superclass name for the class declaration
	 * if altered from the document's contents, otherwise
	 * <code>null</code>. Also <code>null</code> when this
	 * type represents an interface.
	 */
	protected String fSuperclass;

	/**
	 * The original inclusive source range of the superclass
	 * name in the document, or -1's of no superclass was
	 * specified in the document.
	 */
	protected int[]  fSuperclassRange;


	/**
	 * The original inclusive souce range of the 'extends' keyword
	 * in the document, including surrounding whitespace, or -1's if
	 * the keyword was not present in the document.
	 */
	protected int[]	 fExtendsRange;

	/**
	 * The original inclusive souce range of the 'implements' keyword
	 * in the document, including surrounding whitespace, or -1's if
	 * the keyword was not present in the document.
	 */
	protected int[]	 fImplementsRange;

	/**
	 * The comma delimited list of interfaces this type implements
	 * or extends, if altered from the document's contents, otherwise
	 * <code>null</code>. Also <code>null</code> if this type does
	 * not implement or extend any interfaces.
	 */
	protected char[] fInterfaces;

	/**
	 * The original inclusive source range of the list of interfaces this
	 * type implements or extends, not including any surrouding whitespace.
	 * If the document did not specify interfaces, this array contains -1's.
	 */
	protected int[]  fInterfacesRange;



	/**
	 * The original source range of the first character following the
	 * type name superclass name, or interface list, up to and including
	 * the first character before the first type member.
	 */
	protected int[]  fOpenBodyRange;

	/**
	 * The original source range of the first new line or non whitespace
	 * character preceding the close brace of the type's body, up to the
	 * and including the first character before the next node (if there are
	 * no following nodes, the range ends at the position of the last
	 * character in the document).
	 */
	protected int[]  fCloseBodyRange;

	/**
	 * A list of interfaces this type extends or implements.
	 * <code>null</code> when this type does not extend
	 * or implement any interfaces.
	 */
	protected String[] fSuperInterfaces= CharOperation.NO_STRINGS;

	/**
	 * The formal type parameters.
	 * @since 3.0
	 */
	protected String[] fTypeParameters = CharOperation.NO_STRINGS;

	/**
	 * Indicates this type is an enum class.
	 * @since 3.0
	 */
	protected boolean fIsEnum= false;

	/**
	 * Indicates this type is an annotatation type (interface).
	 * @since 3.0
	 */
	protected boolean fIsAnnotation= false;

	/**
	 * This position is the position of the end of the last line separator before the closing brace starting
	 * position of the receiver.
	 */
//	protected int fInsertionPosition;

/**
 * Constructs an empty type node.
 */
DOMType() {
	// Constructs an empty type node
}
/**
 * Creates a new detailed TYPE document fragment on the given range of the document.
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
 * @param typeRange - a two element array describing the location of the 'class'
 *		or 'interface' keyword in the type declaration - first and last character
 *		positions.
 * @param superclassRange - a two element array describing the location of the
 *		superclass name in the type declaration - first and last character
 *		positions or two -1's if a superclass is not present in the document.
 * @param extendsRange - a two element array describing the location of the
 *		'extends' keyword in the type declaration, including any surrounding
 *		whitespace, or -1's if the 'extends' keyword is not present in the document.
 * @param implementsList - an array of names of the interfaces this type implements
 *		or extends, or <code>null</code> if this type does not implement or extend
 *		any interfaces.
 * @param implementsRange - a two element array describing the location of the
 *		comment delimited list of interfaces this type implements or extends,
 *		not including any surrounding whitespace, or -1's if no interface list
 *		is present in the document.
 * @param implementsKeywordRange - a two element array describing the location of the
 *		'implements' keyword, including any surrounding whitespace, or -1's if no
 * 		'implements' keyword is present in the document.
 * @param openBodyRange - a two element array describing the location of the
 *      open brace of the type's body and whitespace following the type declaration
 *		and preceeding the first member in the type.
 * @param closeBodyRange - a two element array describing the source range of the
 *		first new line or non whitespace character preceeding the close brace of the
 *		type's body, up to the close brace
 * @param isClass - true is the type is a class, false if it is an interface
 */
DOMType(char[] document, int[] sourceRange, String name, int[] nameRange, int[] commentRange, int flags, int[] modifierRange, int[] typeRange, int[] superclassRange, int[] extendsRange, String[] implementsList, int[] implementsRange, int[] implementsKeywordRange, int[] openBodyRange, int[] closeBodyRange, boolean isClass) {
	super(document, sourceRange, name, nameRange, commentRange, flags, modifierRange);

	this.fTypeRange= typeRange;
	setMask(MASK_TYPE_IS_CLASS, isClass);

	this.fExtendsRange= extendsRange;
	this.fImplementsRange= implementsKeywordRange;
	this.fSuperclassRange= superclassRange;
	this.fInterfacesRange= implementsRange;
	this.fCloseBodyRange= closeBodyRange;
	setMask(MASK_TYPE_HAS_SUPERCLASS, superclassRange[0] > 0);
	setMask(MASK_TYPE_HAS_INTERFACES, implementsList != null);
	this.fSuperInterfaces= implementsList;
	this.fOpenBodyRange= openBodyRange;
	this.fCloseBodyRange= closeBodyRange;
	setMask(MASK_DETAILED_SOURCE_INDEXES, true);

}
/**
 * Creates a new simple TYPE document fragment on the given range of the document.
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
 * @param flags - an integer representing the modifiers for this member. The
 *		integer can be analyzed with org.eclipse.jdt.core.Flags
 * @param implementsList - an array of names of the interfaces this type implements
 *		or extends, or <code>null</code> if this type does not implement or extend
 *		any interfaces.
 * @param isClass - true is the type is a class, false if it is an interface
 */
DOMType(char[] document, int[] sourceRange, String name, int[] nameRange, int flags, String[] implementsList, boolean isClass) {
	this(document, sourceRange, name, nameRange, new int[] {-1, -1}, flags,
		new int[] {-1, -1}, new int[] {-1, -1}, new int[] {-1, -1}, new int[] {-1, -1},
		implementsList, new int[] {-1, -1}, new int[] {-1, -1}, new int[] {-1, -1}, new int[] {sourceRange[1], sourceRange[1]}, isClass);
	setMask(MASK_DETAILED_SOURCE_INDEXES, false);
}
/**
 * @see IDOMType#addSuperInterface(String)
 */
@Override
public void addSuperInterface(String name) throws IllegalArgumentException {
	if (name == null) {
		throw new IllegalArgumentException(Messages.dom_addNullInterface);
	}
	if (this.fSuperInterfaces == null) {
		this.fSuperInterfaces= new String[1];
		this.fSuperInterfaces[0]= name;
	} else {
		this.fSuperInterfaces= appendString(this.fSuperInterfaces, name);
	}
	setSuperInterfaces(this.fSuperInterfaces);
}
/**
 * @see DOMMember#appendMemberBodyContents(CharArrayBuffer)
 */
@Override
protected void appendMemberBodyContents(CharArrayBuffer buffer) {
	buffer.append(this.fDocument, this.fOpenBodyRange[0], this.fOpenBodyRange[1] + 1 - this.fOpenBodyRange[0]);
	appendContentsOfChildren(buffer);
	buffer.append(this.fDocument, this.fCloseBodyRange[0], this.fCloseBodyRange[1] + 1 - this.fCloseBodyRange[0]);
	buffer.append(this.fDocument, this.fCloseBodyRange[1] + 1, this.fSourceRange[1] - this.fCloseBodyRange[1]);
}
/**
 * @see DOMMember#appendMemberDeclarationContents(CharArrayBuffer )
 */
@Override
protected void appendMemberDeclarationContents(CharArrayBuffer  buffer) {

	if (this.fTypeKeyword != null) {
		buffer.append(this.fTypeKeyword);
		buffer.append(this.fDocument, this.fTypeRange[1], this.fNameRange[0] - this.fTypeRange[1] );
	} else {
		buffer.append(this.fDocument, this.fTypeRange[0], this.fTypeRange[1] + 1 - this.fTypeRange[0]);
	}

	buffer.append(getName());

	if (isClass()) {
		boolean hasInterfaces = false;
		if (getMask(MASK_TYPE_HAS_SUPERCLASS)) {
			if (this.fExtendsRange[0] < 0) {
				buffer.append(" extends "); //$NON-NLS-1$
			} else {
				buffer.append(this.fDocument, this.fExtendsRange[0], this.fExtendsRange[1] + 1 - this.fExtendsRange[0]);
			}
			if (this.fSuperclass != null) {
				buffer.append(this.fSuperclass);
			} else {
				buffer.append(this.fDocument, this.fSuperclassRange[0], this.fSuperclassRange[1] + 1 - this.fSuperclassRange[0]);
			}
		}
		if (getMask(MASK_TYPE_HAS_INTERFACES)) {
			hasInterfaces = true;
			if (this.fImplementsRange[0] < 0) {
				buffer.append(" implements "); //$NON-NLS-1$
			} else {
				buffer.append(this.fDocument, this.fImplementsRange[0], this.fImplementsRange[1] + 1 - this.fImplementsRange[0]);
			}
			if (this.fInterfaces != null) {
				buffer.append(this.fInterfaces);
			} else {
				buffer.append(this.fDocument, this.fInterfacesRange[0], this.fInterfacesRange[1] + 1 - this.fInterfacesRange[0]);
			}
		}
		if (hasInterfaces) {
			if (this.fImplementsRange[0] < 0) {
				buffer.append(' ');
			} else {
				buffer.append(this.fDocument, this.fInterfacesRange[1] + 1, this.fOpenBodyRange[0] - this.fInterfacesRange[1] - 1);
			}
		} else {
			if (this.fSuperclassRange[0] < 0) {
				buffer.append(' ');
			} else if (this.fImplementsRange[0] > 0) {
				buffer.append(this.fDocument, this.fSuperclassRange[1] + 1, this.fImplementsRange[0] - this.fSuperclassRange[1] - 1);
				buffer.append(this.fDocument, this.fInterfacesRange[1] + 1, this.fOpenBodyRange[0] - this.fInterfacesRange[1] - 1);
			} else {
				buffer.append(this.fDocument, this.fSuperclassRange[1] + 1, this.fOpenBodyRange[0] - this.fSuperclassRange[1] - 1);
			}
		}
	} else {
		if (getMask(MASK_TYPE_HAS_INTERFACES)) {
			if (this.fExtendsRange[0] < 0) {
				buffer.append(" extends "); //$NON-NLS-1$
			} else {
				buffer.append(this.fDocument, this.fExtendsRange[0], this.fExtendsRange[1] + 1 - this.fExtendsRange[0]);
			}
			if (this.fInterfaces != null) {
				buffer.append(this.fInterfaces);
				buffer.append(' ');
			} else {
				buffer.append(this.fDocument, this.fInterfacesRange[0], this.fInterfacesRange[1] + 1 - this.fInterfacesRange[0]);
			}
		} else {
			if (this.fImplementsRange[0] < 0) {
				buffer.append(' ');
			} else {
				buffer.append(this.fDocument, this.fNameRange[1] + 1, this.fOpenBodyRange[0] - this.fNameRange[1] - 1);
			}
		}
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


	// append everything after my name and before my first child
	buffer.append(this.fDocument, this.fNameRange[1] + 1, this.fOpenBodyRange[1] - this.fNameRange[1]);
	// append my children
	appendContentsOfChildren(buffer);
	// append from my last child to my end
	buffer.append(this.fDocument, this.fCloseBodyRange[0], this.fSourceRange[1] - this.fCloseBodyRange[0] + 1);


}
/**
 * @see IDOMNode#canHaveChildren()
 */
@Override
public boolean canHaveChildren() {
	return true;
}
/**
 * Returns the position of the closing brace for the body of this type.
 * This value this method returns is only valid before the type has
 * been normalized and is present only for normalization.
 */
int getCloseBodyPosition() {
	return this.fCloseBodyRange[0];
}
/**
 * @see DOMNode#getDetailedNode()
 */
@Override
protected DOMNode getDetailedNode() {
	return (DOMNode)getFactory().createType(getContents());
}
/**
 * @see DOMNode#getInsertionPosition()
 */
@Override
public int getInsertionPosition() {
	// this should return the position of the end of the last line separator before the closing brace of the type
	// See PR 1GELSDQ: ITPJUI:WINNT - JDOM: IType.createMethod does not insert nicely for inner types
	return this.fInsertionPosition;
}
/**
 * @see IDOMNode#getJavaElement
 */
@Override
public IJavaElement getJavaElement(IJavaElement parent) throws IllegalArgumentException {
	switch (parent.getElementType()) {
		case IJavaElement.COMPILATION_UNIT:
			return ((ICompilationUnit)parent).getType(getName());
		case IJavaElement.TYPE:
			return ((IType)parent).getType(getName());
		// Note: creating local/anonymous type is not supported
		default:
			throw new IllegalArgumentException(Messages.element_illegalParent);
	}
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
	return IDOMNode.TYPE;
}
/**
 * Answers the open body range end position.
 */
int getOpenBodyEnd() {
	return this.fOpenBodyRange[1];
}
/**
 * @see IDOMType#getSuperclass()
 */
@Override
public String getSuperclass() {
	becomeDetailed();
	if (getMask(MASK_TYPE_HAS_SUPERCLASS)) {
		if (this.fSuperclass != null) {
			return this.fSuperclass;
		} else {
			return new String(this.fDocument, this.fSuperclassRange[0], this.fSuperclassRange[1] + 1 - this.fSuperclassRange[0]);
		}
	} else {
		return null;
	}
}
/**
 * @see IDOMType#getSuperInterfaces()
 */
@Override
public String[] getSuperInterfaces() {
	return this.fSuperInterfaces;
}
/**
 * @see IDOMNode
 */
@Override
public boolean isAllowableChild(IDOMNode node) {
	if (node != null) {
		int type= node.getNodeType();
		return type == IDOMNode.TYPE || type == IDOMNode.FIELD|| type == IDOMNode.METHOD ||
			type == IDOMNode.INITIALIZER;
	} else {
		return false;
	}

}
/**
 * @see IDOMType#isClass()
 */
@Override
public boolean isClass() {
	return getMask(MASK_TYPE_IS_CLASS);
}
/**
 * @see DOMNode
 */
@Override
protected DOMNode newDOMNode() {
	return new DOMType();
}
/**
 * Normalizes this <code>DOMNode</code>'s source positions to include whitespace preceeding
 * the node on the line on which the node starts, and all whitespace after the node up to
 * the next node's start
 */
@Override
void normalize(ILineStartFinder finder) {
	// perform final changes to the open and close body ranges
	int openBodyEnd, openBodyStart, closeBodyStart, closeBodyEnd;
	DOMNode first = (DOMNode) getFirstChild();
	DOMNode lastNode = null;
	// look for the open body
	Scanner scanner = new Scanner();
	scanner.setSource(this.fDocument);
	scanner.resetTo(this.fNameRange[1] + 1, this.fDocument.length);

	try {
		int currentToken = scanner.getNextToken();
		while(currentToken != TerminalTokens.TokenNameLBRACE &&
				currentToken != TerminalTokens.TokenNameEOF) {
			currentToken = scanner.getNextToken();
		}
		if(currentToken == TerminalTokens.TokenNameLBRACE) {
			openBodyEnd = scanner.currentPosition - 1;
			openBodyStart = scanner.startPosition;
		} else {
			openBodyEnd = this.fDocument.length;
			openBodyStart = this.fDocument.length;
		}
	} catch(InvalidInputException e) {
		openBodyEnd = this.fDocument.length;
		openBodyStart = this.fDocument.length;
	}
	if (first != null) {
		int lineStart = finder.getLineStart(first.getStartPosition());
		if (lineStart > openBodyEnd) {
			openBodyEnd = lineStart - 1;
		} else {
			openBodyEnd = first.getStartPosition() - 1;
		}
		lastNode = (DOMNode) first.getNextNode();
		if (lastNode == null) {
			lastNode = first;
		} else {
			while (lastNode.getNextNode() != null) {
				lastNode = (DOMNode) lastNode.getNextNode();
			}
		}
		scanner.setSource(this.fDocument);
		scanner.resetTo(lastNode.getEndPosition() + 1, this.fDocument.length);
		try {
			int currentToken = scanner.getNextToken();
			while(currentToken != TerminalTokens.TokenNameRBRACE &&
					currentToken != TerminalTokens.TokenNameEOF) {
				currentToken = scanner.getNextToken();
			}
			if(currentToken == TerminalTokens.TokenNameRBRACE) {
				closeBodyStart = scanner.startPosition;
				closeBodyEnd = scanner.currentPosition - 1;
			} else {
				closeBodyStart = this.fDocument.length;
				closeBodyEnd = this.fDocument.length;
			}
		} catch(InvalidInputException e) {
			closeBodyStart = this.fDocument.length;
			closeBodyEnd = this.fDocument.length;
		}
	} else {
		scanner.resetTo(openBodyEnd, this.fDocument.length);
		try {
			int currentToken = scanner.getNextToken();
			while(currentToken != TerminalTokens.TokenNameRBRACE &&
					currentToken != TerminalTokens.TokenNameEOF) {
				currentToken = scanner.getNextToken();
			}
			if(currentToken == TerminalTokens.TokenNameRBRACE) {
				closeBodyStart = scanner.startPosition;
				closeBodyEnd = scanner.currentPosition - 1;
			} else {
				closeBodyStart = this.fDocument.length;
				closeBodyEnd = this.fDocument.length;
			}
		} catch(InvalidInputException e) {
			closeBodyStart = this.fDocument.length;
			closeBodyEnd = this.fDocument.length;
		}
		openBodyEnd = closeBodyEnd - 1;
	}
	setOpenBodyRangeEnd(openBodyEnd);
	setOpenBodyRangeStart(openBodyStart);
	setCloseBodyRangeStart(closeBodyStart);
	setCloseBodyRangeEnd(closeBodyEnd);
	this.fInsertionPosition = finder.getLineStart(closeBodyStart);
	if (lastNode != null && this.fInsertionPosition < lastNode.getEndPosition()) {
		this.fInsertionPosition = getCloseBodyPosition();
	}
	if (this.fInsertionPosition <= openBodyEnd) {
		this.fInsertionPosition = getCloseBodyPosition();
	}
	super.normalize(finder);
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
			setSourceRangeEnd(((DOMType)parent).getCloseBodyPosition() - 1);
		}
	} else {
		// this node's end position is just before the start of the next node
		next.normalizeStartPosition(getEndPosition(), finder);
		setSourceRangeEnd(next.getStartPosition() - 1);
	}
}

/**
 * Offsets all the source indexes in this node by the given amount.
 */
@Override
protected void offset(int offset) {
	super.offset(offset);
	offsetRange(this.fCloseBodyRange, offset);
	offsetRange(this.fExtendsRange, offset);
	offsetRange(this.fImplementsRange, offset);
	offsetRange(this.fInterfacesRange, offset);
	offsetRange(this.fOpenBodyRange, offset);
	offsetRange(this.fSuperclassRange, offset);
	offsetRange(this.fTypeRange, offset);
}
/**
 * @see IDOMType#setClass(boolean)
 */
@Override
public void setClass(boolean b) {
	becomeDetailed();
	fragment();
	setMask(MASK_TYPE_IS_CLASS, b);
	if (b) {
		this.fTypeKeyword= "class"; //$NON-NLS-1$
	} else {
		this.fTypeKeyword= "interface"; //$NON-NLS-1$
		setSuperclass(null);
	}
}
/**
 * Sets the end of the close body range
 */
void setCloseBodyRangeEnd(int end) {
	this.fCloseBodyRange[1] = end;
}
/**
 * Sets the start of the close body range
 */
void setCloseBodyRangeStart(int start) {
	this.fCloseBodyRange[0] = start;
}
/**
 * Sets the name of this node.
 *
 * <p>When the name of a type is set, all of its constructors must be marked
 * as fragmented, since the names of the constructors must reflect the name
 * of this type.
 *
 * @see IDOMNode#setName(String)
 */
@Override
public void setName(String name) throws IllegalArgumentException {
	if (name == null) {
		throw new IllegalArgumentException(Messages.element_nullName);
	}
	super.setName(name);
	Enumeration children= getChildren();
	while (children.hasMoreElements()) {
		IDOMNode child= (IDOMNode)children.nextElement();
		if (child.getNodeType() == IDOMNode.METHOD && ((IDOMMethod)child).isConstructor()) {
			((DOMNode)child).fragment();
		}
	}
}
/**
 * Sets the end of the open body range
 */
void setOpenBodyRangeEnd(int end) {
	this.fOpenBodyRange[1] = end;
}
/**
 * Sets the start of the open body range
 */
void setOpenBodyRangeStart(int start) {
	this.fOpenBodyRange[0] = start;
}
/**
 * @see IDOMType#setSuperclass(String)
 */
@Override
public void setSuperclass(String superclassName) {
	becomeDetailed();
	fragment();
	this.fSuperclass= superclassName;
	setMask(MASK_TYPE_HAS_SUPERCLASS, superclassName != null);
}
/**
 * @see IDOMType#setSuperInterfaces(String[])
 */
@Override
public void setSuperInterfaces(String[] names) {
	becomeDetailed();
	if (names == null) {
		throw new IllegalArgumentException(Messages.dom_nullInterfaces);
	}
	fragment();
	this.fSuperInterfaces= names;
	if (names.length == 0) {
		this.fInterfaces= null;
		this.fSuperInterfaces= CharOperation.NO_STRINGS;
		setMask(MASK_TYPE_HAS_INTERFACES, false);
	} else {
		setMask(MASK_TYPE_HAS_INTERFACES, true);
		CharArrayBuffer buffer = new CharArrayBuffer();
		for (int i = 0; i < names.length; i++) {
			if (i > 0) {
				buffer.append(", "); //$NON-NLS-1$
			}
			buffer.append(names[i]);
		}
		this.fInterfaces = buffer.getContents();
	}
}
/**
 * Sets the type keyword
 */
void setTypeKeyword(String keyword) {
	this.fTypeKeyword = keyword;
}
/**
 * @see DOMNode#shareContents(DOMNode)
 */
@Override
protected void shareContents(DOMNode node) {
	super.shareContents(node);
	DOMType type= (DOMType)node;
	this.fCloseBodyRange= rangeCopy(type.fCloseBodyRange);
	this.fExtendsRange= type.fExtendsRange;
	this.fImplementsRange= rangeCopy(type.fImplementsRange);
	this.fInterfaces= type.fInterfaces;
	this.fInterfacesRange= rangeCopy(type.fInterfacesRange);
	this.fOpenBodyRange= rangeCopy(type.fOpenBodyRange);
	this.fSuperclass= type.fSuperclass;
	this.fSuperclassRange= rangeCopy(type.fSuperclassRange);
	this.fSuperInterfaces= type.fSuperInterfaces;
	this.fTypeKeyword= type.fTypeKeyword;
	this.fTypeRange= rangeCopy(type.fTypeRange);
}
/**
 * @see IDOMNode#toString()
 */
@Override
public String toString() {
	return "TYPE: " + getName(); //$NON-NLS-1$
}

/**
 * @see IDOMType#getTypeParameters()
 * @since 3.0
 */
@Override
public String[] getTypeParameters() {
	return this.fTypeParameters;
}

/**
 * @see IDOMType#isEnum()
 * @since 3.0
 */
@Override
public boolean isEnum() {
	return this.fIsEnum;
}

/**
 * @see IDOMType#isAnnotation()
 * @since 3.0
 */
@Override
public boolean isAnnotation() {
	return this.fIsAnnotation;
}

/**
 * @see IDOMType#setEnum(boolean)
 * @since 3.0
 */
@Override
public void setEnum(boolean b) {
	this.fIsEnum = b;
	if (this.fIsEnum) {
		// enums are always classes with no superclass
		setClass(true);
		setSuperclass(null);
	}
}

/**
 * @see IDOMType#setAnnotation(boolean)
 * @since 3.0
 */
@Override
public void setAnnotation(boolean b) {
	this.fIsAnnotation= b;
	if (this.fIsAnnotation) {
		// annotation types are always interface with no superclass or superinterfaces
		setClass(false);
		setSuperclass(null);
		setSuperInterfaces(CharOperation.NO_STRINGS);
	}
}

/**
 * @see IDOMType#setTypeParameters(java.lang.String[])
 * @since 3.0
 */
@Override
public void setTypeParameters(String[] typeParameters) {
	this.fTypeParameters = typeParameters;
}
}
