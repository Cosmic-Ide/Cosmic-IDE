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
import org.eclipse.jdt.internal.core.util.CharArrayBuffer;
import org.eclipse.jdt.internal.core.util.Messages;

/**
 * DOMNode provides an implementation for <code>IDOMNode</code>.
 *
 * <p>A node represents a document fragment. When a node is created, its
 * contents are located in a contiguous range of a shared document. A shared
 * document is a char array, and is shared in the sense that the contents of other
 * document fragments may also be contained in the array.
 *
 * <p>A node maintains indicies of relevant portions of its contents
 * in the shared document. Thus the original document and indicies create a
 * form from which to generate the contents of the document fragment. As attributes
 * of a node are changed, the node attempts to maintain the original formatting
 * by only replacing relevant portions of the shared document with the value
 * of new attributes (that is, filling in the form with replacement values).
 *
 * <p>When a node is first created, it is considered unfragmented. When any
 * attribute of the node is altered, the node is then considered fragmented
 * from that point on. A node is also considered fragmented if any of its
 * descendants are fragmented. When a node is unfragmented, the contents of the
 * node can be efficiently generated from the original shared document. When
 * a node is fragmented, the contents of the node must be created using the
 * original document and indicies as a form, filling in replacement values
 * as required.
 *
 * <p>Generally, a node's contents consists of complete lines in a shared document.
 * The contents of the node are normalized on creation to include any whitespace
 * preceding the node on the line where the node begins, and to include and trailing
 * whitespace up to the line where the next node begins. Any trailing // comments
 * that begin on the line where the current node ends, are considered part of that
 * node.
 *
 * @see IDOMNode
 * @deprecated The JDOM was made obsolete by the addition in 2.0 of the more
 * powerful, fine-grained DOM/AST API found in the
 * org.eclipse.jdt.core.dom package.
 */
@SuppressWarnings("rawtypes")
public abstract class DOMNode implements IDOMNode {

	/**
	 * The first child of this node - <code>null</code>
	 * when this node has no children. (Children of a node
	 * are implemented as a doubly linked list).
	 */
	protected DOMNode fFirstChild= null;

	/**
	 * The last child of this node - <code>null</code>
	 * when this node has no children. Used for efficient
	 * access to the last child when adding new children
	 * at the end of the linked list of children.
	 */
	protected DOMNode fLastChild= null;

	/**
	 * The sibling node following this node - <code>null</code>
	 * for the last node in the sibling list.
	 */
	protected DOMNode fNextNode= null;

	/**
	 * The parent of this node. A <code>null</code>
	 * parent indicates that this node is a root
	 * node of a document fragment.
	 */
	protected DOMNode fParent= null;

	/**
	 * The sibling node preceding this node - <code>null</code>
	 * for the first node in the sibling list.
	 */
	protected DOMNode fPreviousNode= null;

	/**
	 * True when this node has attributes that have
	 * been altered from their original state in the
	 * shared document, or when the attributes of a
	 * descendant have been altered. False when the
	 * contents of this node and all descendants are
	 * consistent with the content of the shared
	 * document.
	 */
	protected boolean fIsFragmented= false;

	/**
	 * The name of this node. For efficiency, the
	 * name of a node is duplicated in this variable
	 * on creation, rather than always having to fetch
	 * the name from the shared document.
	 */
	protected String  fName= null;

	/**
	 * The original inclusive indicies of this node's name in
	 * the shared document. Values of -1 indiciate the name
	 * does not exist in the document.
	 */
	protected int[]	  fNameRange;

	/**
	 * The shared document that the contents for this node
	 * are contained in. Attribute indicies are positions
	 * in this character array.
	 */
	protected char[]  fDocument= null;

	/**
	 * The original entire inclusive range of this node's contents
	 * within its document. Values of -1 indicate the contents
	 * of this node do not exist in the document.
	 */
	protected int[] fSourceRange;

	/**
	 * The current state of bit masks defined by this node.
	 * Initially all bit flags are turned off. All bit masks
	 * are defined by this class to avoid overlap, although
	 * bit masks are node type specific.
	 *
	 * @see #setMask
	 * @see #getMask
	 */
	protected int fStateMask= 0;

	/**
	 * This position is the position of the end of the last line separator before the closing brace starting
	 * position of the receiver.
	 */
	protected int fInsertionPosition;

	/**
	 * A bit mask indicating this field has an initializer
	 * expression
	 */
	protected static final int MASK_FIELD_HAS_INITIALIZER= 0x00000001;

	/**
	 * A bit mask indicating this field is a secondary variable
	 * declarator for a previous field declaration.
	 */
	protected static final int MASK_FIELD_IS_VARIABLE_DECLARATOR= 0x00000002;

	/**
	 * A bit mask indicating this field's type has been
	 * altered from its original contents in the document.
	 */
	protected static final int MASK_FIELD_TYPE_ALTERED= 0x00000004;

	/**
	 * A bit mask indicating this node's name has been
	 * altered from its original contents in the document.
	 */
	protected static final int MASK_NAME_ALTERED= 0x00000008;

	/**
	 * A bit mask indicating this node currently has a
	 * body.
	 */
	protected static final int MASK_HAS_BODY= 0x00000010;

	/**
	 * A bit mask indicating this node currently has a
	 * preceding comment.
	 */
	protected static final int MASK_HAS_COMMENT= 0x00000020;

	/**
	 * A bit mask indicating this method is a constructor.
	 */
	protected static final int MASK_IS_CONSTRUCTOR= 0x00000040;

	/**
	 * A bit mask indicating this type is a class.
	 */
	protected static final int MASK_TYPE_IS_CLASS= 0x00000080;

	/**
	 * A bit mask indicating this type has a superclass
	 * (requires or has an 'extends' clause).
	 */
	protected static final int MASK_TYPE_HAS_SUPERCLASS= 0x00000100;

	/**
	 * A bit mask indicating this type implements
	 * or extends some interfaces
	 */
	protected static final int MASK_TYPE_HAS_INTERFACES= 0x00000200;

	/**
	 * A bit mask indicating this return type of this method has
	 * been altered from the original contents.
	 */
	protected static final int MASK_RETURN_TYPE_ALTERED= 0x00000400;

	/**
	 * A bit mask indicating this node has detailed source indexes
	 */
	protected static final int MASK_DETAILED_SOURCE_INDEXES = 0x00000800;

/**
 * Creates a new empty document fragment.
 */
DOMNode() {
	this.fName= null;
	this.fDocument= null;
	this.fSourceRange= new int[]{-1, -1};
	this.fNameRange= new int[]{-1, -1};
	fragment();
}
/**
 * Creates a new document fragment on the given range of the document.
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
DOMNode(char[] document, int[] sourceRange, String name, int[] nameRange) {
	super();
	this.fDocument= document;
	this.fSourceRange= sourceRange;
	this.fName= name;
	this.fNameRange= nameRange;

}
/**
 * Adds the given un-parented node (document fragment) as the last child of
 * this node.
 *
 * <p>When a child is added, this node must be considered fragmented such that
 * the contents of this node are properly generated.
 *
 * @see IDOMNode#addChild(IDOMNode)
 */
@Override
public void addChild(IDOMNode child) throws IllegalArgumentException, DOMException {
	basicAddChild(child);

	// if the node is a constructor, it must also be fragmented to update the constructor's name
	if (child.getNodeType() == IDOMNode.METHOD && ((IDOMMethod)child).isConstructor()) {
		((DOMNode)child).fragment();
	} else {
		fragment();
	}
}
/**
 * Appends the current contents of this document fragment
 * to the given <code>CharArrayBuffer</code>.
 *
 * <p>If this node is fragmented, contents must be generated by
 * using the original document and indicies as a form for the current
 * attribute values of this node. If this node not fragmented, the
 * contents can be obtained from the document.
 *
 */
protected void appendContents(CharArrayBuffer buffer) {
	if (isFragmented()) {
		appendFragmentedContents(buffer);
	} else {
		buffer.append(this.fDocument, this.fSourceRange[0], this.fSourceRange[1] + 1 - this.fSourceRange[0]);
	}
}
/**
 * Appends the contents of all children of this node to the
 * given <code>CharArrayBuffer</code>.
 *
 * <p>This algorithm used minimizes String generation by merging
 * adjacent unfragmented children into one substring operation.
 *
 */
protected void appendContentsOfChildren(CharArrayBuffer buffer) {
	DOMNode child= this.fFirstChild;
	DOMNode sibling;

	int start= 0, end= 0;
	if (child != null) {
		start= child.getStartPosition();
		end= child.getEndPosition();
	}
	while (child != null) {
		sibling= child.fNextNode;
		if (sibling != null) {
			if (sibling.isContentMergableWith(child)) {
				end= sibling.getEndPosition();
			} else {
				if (child.isFragmented()) {
					child.appendContents(buffer);
				} else {
					buffer.append(child.getDocument(), start, end + 1 - start);
				}
				start= sibling.getStartPosition();
				end= sibling.getEndPosition();
			}
		} else {
			if (child.isFragmented()) {
				child.appendContents(buffer);
			} else {
				buffer.append(child.getDocument(), start, end + 1 - start);
			}
		}
		child= sibling;
	}
}
/**
 * Appends the contents of this node to the given <code>CharArrayBufer</code>, using
 * the original document and indicies as a form for the current attribute
 * values of this node.
 */
protected abstract void appendFragmentedContents(CharArrayBuffer buffer);
/**
 * Adds the given un-parented node (document fragment) as the last child of
 * this node without setting this node's 'fragmented' flag. This
 * method is only used by the <code>DOMBuilder</code> when creating a new DOM such
 * that a new DOM is unfragmented.
 */
void basicAddChild(IDOMNode child) throws IllegalArgumentException, DOMException {
	// verify child may be added
	if (!canHaveChildren()) {
		throw new DOMException(Messages.dom_unableAddChild);
	}
	if (child == null) {
		throw new IllegalArgumentException(Messages.dom_addNullChild);
	}
	if (!isAllowableChild(child)) {
		throw new DOMException(Messages.dom_addIncompatibleChild);
	}
	if (child.getParent() != null) {
		throw new DOMException(Messages.dom_addChildWithParent);
	}
	/* NOTE: To test if the child is an ancestor of this node, we
	 * need only test if the root of this node is the child (the child
	 * is already a root since we have just guarenteed it has no parent).
	 */
	if (child == getRoot()) {
		throw new DOMException(Messages.dom_addAncestorAsChild);
	}

	DOMNode node= (DOMNode)child;

	// if the child is not already part of this document, localize its contents
	// before adding it to the tree
	if (node.getDocument() != getDocument()) {
		node.localizeContents();
	}

	// add the child last
	if (this.fFirstChild == null) {
		// this is the first and only child
		this.fFirstChild= node;
	} else {
		this.fLastChild.fNextNode= node;
		node.fPreviousNode= this.fLastChild;
	}
	this.fLastChild= node;
	node.fParent= this;
}
/**
 * Generates detailed source indexes for this node if possible.
 *
 * @exception DOMException if unable to generate detailed source indexes
 * 	for this node
 */
protected void becomeDetailed() throws DOMException {
	if (!isDetailed()) {
		DOMNode detailed= getDetailedNode();
		if (detailed == null) {
			throw new DOMException(Messages.dom_cannotDetail);
		}
		if (detailed != this) {
			shareContents(detailed);
		}
	}
}
/**
 * Returns true if this node is allowed to have children, otherwise false.
 *
 * <p>Default implementation of <code>IDOMNode</code> interface method returns false; this
 * method must be overridden by subclasses that implement nodes that allow
 * children.
 *
 * @see IDOMNode#canHaveChildren()
 */
@Override
public boolean canHaveChildren() {
	return false;
}
/**
 * @see IDOMNode#clone()
 */
@Override
public Object clone() {

	// create a new buffer with all my contents and children contents
	int length= 0;
	char[] buffer= null;
	int offset= this.fSourceRange[0];

	if (offset >= 0) {
		length= this.fSourceRange[1] - offset + 1;
		buffer= new char[length];
		System.arraycopy(this.fDocument, offset, buffer, 0, length);
	}
	DOMNode clone= newDOMNode();
	clone.shareContents(this);
	clone.fDocument = buffer;

	if (offset > 0) {
		clone.offset(0 - offset);
	}

	// clone my children
	if (canHaveChildren()) {
		Enumeration children= getChildren();
		while (children.hasMoreElements()) {
			DOMNode child= (DOMNode)children.nextElement();
			if (child.fDocument == this.fDocument) {
				DOMNode childClone= child.cloneSharingDocument(buffer, offset);
				clone.basicAddChild(childClone);
			} else {
				DOMNode childClone= (DOMNode)child.clone();
				clone.addChild(childClone);
			}

		}
	}

	return clone;
}
private DOMNode cloneSharingDocument(char[] document, int rootOffset) {

	DOMNode clone = newDOMNode();
	clone.shareContents(this);
	clone.fDocument = document;
	if (rootOffset > 0) {
		clone.offset(0 - rootOffset);
	}

	if (canHaveChildren()) {
		Enumeration children = getChildren();
		while (children.hasMoreElements()) {
			DOMNode child = (DOMNode) children.nextElement();
			if (child.fDocument == this.fDocument) {
				DOMNode childClone= child.cloneSharingDocument(document, rootOffset);
				clone.basicAddChild(childClone);
			} else {
				DOMNode childClone= (DOMNode)child.clone();
				clone.addChild(childClone);
			}
		}
	}
	return clone;
}
/**
 * Sets this node's fragmented flag and all ancestor fragmented flags
 * to <code>true<code>. This happens when an attribute of this node or a descendant
 * node has been altered. When a node is fragmented, its contents must
 * be generated from its attributes and original "form" rather than
 * from the original contents in the document.
 */
protected void fragment() {
	if (!isFragmented()) {
		this.fIsFragmented= true;
		if (this.fParent != null) {
			this.fParent.fragment();
		}
	}
}
/**
 * @see IDOMNode#getCharacters()
 */
@Override
public char[] getCharacters() {
	CharArrayBuffer buffer= new CharArrayBuffer();
	appendContents(buffer);
	return buffer.getContents();
}
/**
 * @see IDOMNode#getChild(String)
 */
@Override
public IDOMNode getChild(String name) {
	DOMNode child = this.fFirstChild;
	while (child != null) {
		String n = child.getName();
		if (name == null) {
			if (n == null) {
				return child;
			}
		} else {
			if (name.equals(n)) {
				return child;
			}
		}
		child = child.fNextNode;
	}
	return null;
}
/**
 * @see IDOMNode#getChildren()
 */
@Override
public Enumeration getChildren() {
	return new SiblingEnumeration(this.fFirstChild);
}
/**
 * Returns the current contents of this document fragment,
 * or <code>null</code> if this node has no contents.
 *
 * <p>If this node is fragmented, contents must be generated by
 * using the original document and indicies as a form for the current
 * attribute values of this node. If this node not fragmented, the
 * contents can be obtained from the document.
 *
 * @see IDOMNode#getContents()
 */
@Override
public String getContents() {
	CharArrayBuffer buffer= new CharArrayBuffer();
	appendContents(buffer);
	return buffer.toString();
}
/**
 * Returns a new document fragment representing this node with
 * detailed source indexes. Subclasses that provide a detailed
 * implementation must override this method.
 */
protected DOMNode getDetailedNode() {
	return this;
}
/**
 * Returns the document containing this node's original contents.
 * The document may be shared by other nodes.
 */
protected char[] getDocument() {
	return this.fDocument;
}
/**
 * Returns the original position of the last character of this
 * node's contents in its document.
 */
public int getEndPosition() {
	return this.fSourceRange[1];
}
/**
 * Returns a factory with which to create new document fragments.
 */
protected IDOMFactory getFactory() {
	return new DOMFactory();
}
/**
 * @see IDOMNode#getFirstChild()
 */
@Override
public IDOMNode getFirstChild() {
	return this.fFirstChild;
}
/**
 * Returns the position at which the first child of this node should be inserted.
 */
public int getInsertionPosition() {
	return this.fInsertionPosition;
}
/**
 * Returns <code>true</code> if the given mask of this node's state flag
 * is turned on, otherwise <code>false</code>.
 */
protected boolean getMask(int mask) {
	return (this.fStateMask & mask) > 0;
}
/**
 * @see IDOMNode#getName()
 */
@Override
public String getName() {
	return this.fName;
}
/**
 * Returns the source code to be used for this node's name.
 */
protected char[] getNameContents() {
	if (isNameAltered()) {
		return this.fName.toCharArray();
	} else {
		if (this.fName == null || this.fNameRange[0] < 0) {
			return null;
		} else {
			int length = this.fNameRange[1] + 1 - this.fNameRange[0];
			char[] result = new char[length];
			System.arraycopy(this.fDocument, this.fNameRange[0], result, 0, length);
			return result;
		}
	}
}
/**
 * @see IDOMNode#getNextNode()
 */
@Override
public IDOMNode getNextNode() {
	return this.fNextNode;
}
/**
 * @see IDOMNode#getParent()
 */
@Override
public IDOMNode getParent() {
	return this.fParent;
}
/**
 * Answers a source position which corresponds to the end of the parent
 * element's declaration.
 */
protected int getParentEndDeclaration() {
	IDOMNode parent = getParent();
	if (parent == null) {
		return 0;
	} else {
		if (parent instanceof IDOMCompilationUnit) {
			return 0;
		} else {
			return ((DOMType)parent).getOpenBodyEnd();
		}
	}
}
/**
 * @see IDOMNode#getPreviousNode()
 */
@Override
public IDOMNode getPreviousNode() {
	return this.fPreviousNode;
}
/**
 * Returns the root node of this document fragment.
 */
protected IDOMNode getRoot() {
	if (this.fParent == null) {
		return this;
	} else {
		return this.fParent.getRoot();
	}
}
/**
 * Returns the original position of the first character of this
 * node's contents in its document.
 */
public int getStartPosition() {
	return this.fSourceRange[0];
}
/**
 * @see IDOMNode#insertSibling(IDOMNode)
 */
@Override
public void insertSibling(IDOMNode sibling) throws IllegalArgumentException, DOMException {
	// verify sibling may be added
	if (sibling == null) {
		throw new IllegalArgumentException(Messages.dom_addNullSibling);
	}
	if (this.fParent == null) {
		throw new DOMException(Messages.dom_addSiblingBeforeRoot);
	}
	if (!this.fParent.isAllowableChild(sibling)) {
		throw new DOMException(Messages.dom_addIncompatibleSibling);
	}
	if (sibling.getParent() != null) {
		throw new DOMException(Messages.dom_addSiblingWithParent);
	}
	/* NOTE: To test if the sibling is an ancestor of this node, we
	 * need only test if the root of this node is the child (the sibling
	 * is already a root since we have just guaranteed it has no parent).
	 */
	if (sibling == getRoot()) {
		throw new DOMException(Messages.dom_addAncestorAsSibling);
	}

	DOMNode node= (DOMNode)sibling;

	// if the sibling is not already part of this document, localize its contents
	// before inserting it into the tree
	if (node.getDocument() != getDocument()) {
		node.localizeContents();
	}

	// insert the node
	if (this.fPreviousNode == null) {
		this.fParent.fFirstChild= node;
	} else {
		this.fPreviousNode.fNextNode= node;
	}
	node.fParent= this.fParent;
	node.fPreviousNode= this.fPreviousNode;
	node.fNextNode= this;
	this.fPreviousNode= node;

	// if the node is a constructor, it must also be fragmented to update the constructor's name
	if (node.getNodeType() == IDOMNode.METHOD && ((IDOMMethod)node).isConstructor()) {
		node.fragment();
	} else {
		this.fParent.fragment();
	}
}
/**
 * @see IDOMNode
 */
@Override
public boolean isAllowableChild(IDOMNode node) {
	return false;
}
/**
 * Returns <code>true</code> if the contents of this node are from the same document as
 * the given node, the contents of this node immediately follow the contents
 * of the given node, and neither this node or the given node are fragmented -
 * otherwise <code>false</code>.
 */
protected boolean isContentMergableWith(DOMNode node) {
	return !node.isFragmented() && !isFragmented() && node.getDocument() == getDocument() &&
		node.getEndPosition() + 1 == getStartPosition();
}
/**
 * Returns <code>true</code> if this node has detailed source index information,
 * or <code>false</code> if this node has limited source index information. To
 * perform some manipulations, detailed indexes are required.
 */
protected boolean isDetailed() {
	return getMask(MASK_DETAILED_SOURCE_INDEXES);
}
/**
 * Returns <code>true</code> if this node's or a descendant node's contents
 * have been altered since this node was created. This indicates
 * that the contents of this node are no longer consistent with
 * the contents of this node's document.
 */
protected boolean isFragmented() {
	return this.fIsFragmented;
}
/**
 * Returns <code>true</code> if this noed's name has been altered
 * from the original document contents.
 */
protected boolean isNameAltered() {
	return getMask(MASK_NAME_ALTERED);
}
/**
 * @see IDOMNode#isSignatureEqual(IDOMNode)
 *
 * <p>By default, the signatures of two nodes are equal if their
 * type and names are equal. Node types that have other requirements
 * for equality must override this method.
 */
@Override
public boolean isSignatureEqual(IDOMNode node) {
	return getNodeType() == node.getNodeType() && getName().equals(node.getName());
}
/**
 * Localizes the contents of this node and all descendant nodes,
 * such that this node is no longer dependent on its original
 * document in order to generate its contents. This node and all
 * descendant nodes become unfragmented and share a new
 * document.
 */
protected void localizeContents() {

	DOMNode clone= (DOMNode)clone();
	shareContents(clone);

}
/**
 * Returns a new empty <code>DOMNode</code> for this instance.
 */
protected abstract DOMNode newDOMNode();
/**
 * Normalizes this <code>DOMNode</code>'s source positions to include whitespace preceeding
 * the node on the line on which the node starts, and all whitespace after the node up to
 * the next node's start
 */
void normalize(ILineStartFinder finder) {
	if (getPreviousNode() == null)
		normalizeStartPosition(getParentEndDeclaration(), finder);

	// Set the children's position
	if (canHaveChildren()) {
		Enumeration children = getChildren();
		while(children.hasMoreElements())
			((DOMNode)children.nextElement()).normalize(finder);
	}

	normalizeEndPosition(finder, (DOMNode)getNextNode());
}
/**
 * Normalizes this <code>DOMNode</code>'s end position.
 */
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
		int temp = next.getStartPosition() - 1;
		this.fInsertionPosition = Math.max(finder.getLineStart(temp + 1), getEndPosition());
		next.normalizeStartPosition(getEndPosition(), finder);
		setSourceRangeEnd(next.getStartPosition() - 1);
	}
}
/**
 * Normalizes this <code>DOMNode</code>'s start position.
 */
void normalizeStartPosition(int previousEnd, ILineStartFinder finder) {
	int nodeStart = getStartPosition();
	int lineStart = finder.getLineStart(nodeStart);
	if (nodeStart > lineStart && (lineStart > previousEnd || (previousEnd == 0 && lineStart == 0)))
		setStartPosition(lineStart);
}
/**
 * Offsets all the source indexes in this node by the given amount.
 */
protected void offset(int offset) {
	offsetRange(this.fNameRange, offset);
	offsetRange(this.fSourceRange, offset);
}
/**
 * Offsets the source range by the given amount
 */
protected void offsetRange(int[] range, int offset) {
	for (int i= 0; i < range.length; i++) {
		range[i]+=offset;
		if (range[i] < 0) {
			range[i]= -1;
		}
	}
}
/**
 * Returns a copy of the given range.
 */
protected int[] rangeCopy(int[] range) {
	int[] copy= new int[range.length];
	System.arraycopy(range, 0, copy, 0, range.length);
	return copy;
}
/**
 * Separates this node from its parent and siblings, maintaining any ties that
 * this node has to the underlying document fragment.
 *
 * <p>When a child is removed, its parent is fragmented such that it properly
 * generates its contents.
 *
 * @see IDOMNode#remove()
 */
@Override
public void remove() {

	if (this.fParent != null) {
		this.fParent.fragment();
	}

	// link siblings
	if (this.fNextNode != null) {
		this.fNextNode.fPreviousNode= this.fPreviousNode;
	}
	if (this.fPreviousNode != null) {
		this.fPreviousNode.fNextNode= this.fNextNode;
	}
	// fix parent's pointers
	if (this.fParent != null) {
		if (this.fParent.fFirstChild == this) {
			this.fParent.fFirstChild= this.fNextNode;
		}
		if (this.fParent.fLastChild == this) {
			this.fParent.fLastChild= this.fPreviousNode;
		}
	}
	// remove myself
	this.fParent= null;
	this.fNextNode= null;
	this.fPreviousNode= null;
}
/**
 * Sets the specified mask of this node's state mask on or off
 * based on the boolean value - true -> on, false -> off.
 */
protected void setMask(int mask, boolean on) {
	if (on) {
		this.fStateMask |= mask;
	} else {
		this.fStateMask &= ~mask;
	}
}
/**
 * @see IDOMNode#setName
 */
@Override
public void setName(String name) {
	this.fName= name;
	setNameAltered(true);
	fragment();
}
/**
 * Sets the state of this node as having
 * its name attribute altered from the original
 * document contents.
 */
protected void setNameAltered(boolean altered) {
	setMask(MASK_NAME_ALTERED, altered);
}
/**
 * Sets the original position of the last character of this node's contents
 * in its document. This method is only used during DOM creation while
 * normalizing the source range of each node.
 */
protected void setSourceRangeEnd(int end) {
	this.fSourceRange[1]= end;
}
/**
 * Sets the original position of the first character of this node's contents
 * in its document. This method is only used during DOM creation while
 * normalizing the source range of each node.
 */
protected void setStartPosition(int start) {
	this.fSourceRange[0]= start;
}
/**
 * Sets the contents of this node and descendant nodes to be the
 * (identical) contents of the given node and its descendants. This
 * does not effect this node's parent and sibling configuration,
 * only the contents of this node. This is used only to localize
 * the contents of this node.
 */
protected void shareContents(DOMNode node) {
	this.fDocument= node.fDocument;
	this.fIsFragmented= node.fIsFragmented;
	this.fName= node.fName;
	this.fNameRange= rangeCopy(node.fNameRange);
	this.fSourceRange= rangeCopy(node.fSourceRange);
	this.fStateMask= node.fStateMask;


	if (canHaveChildren()) {
		Enumeration myChildren= getChildren();
		Enumeration otherChildren= node.getChildren();
		DOMNode myChild, otherChild;
		while (myChildren.hasMoreElements()) {
			myChild= (DOMNode)myChildren.nextElement();
			otherChild= (DOMNode)otherChildren.nextElement();
			myChild.shareContents(otherChild);
		}
	}
}
/**
 * Returns a <code>String</code> representing this node - for Debug purposes only.
 */
@Override
public abstract String toString();
}
