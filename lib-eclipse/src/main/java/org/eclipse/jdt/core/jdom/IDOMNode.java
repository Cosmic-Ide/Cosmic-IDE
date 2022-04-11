/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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

import java.util.Enumeration;

import org.eclipse.jdt.core.IJavaElement;

/**
 * Nodes represent structural fragments of a Java source file, also known as document fragments. Their implementation
 * is known as a DOM (Document Object Model) -  in this case a JDOM (Java DOM). A root node (node
 * with no parent or siblings) represents the root of a document fragment (DF). A complete Java document is
 * represented by a compilation unit node (<code>IDOMCompilationUnit</code>). In this way, a DF is
 * comprised of DFs, and a document itself (compilation unit) is also a DF.
 * <p>
 * A DF may be created empty and programmatically filled, or it may be created from
 * a source code string. The <code>IDOMFactory</code> allows the creation of all kinds
 * of nodes from source code strings. Manipulations performed on a DF are immediately
 * reflected in the DF's contents.
 * </p>
 * <p>
 * Children fragments are represented as a linked list of nodes. Children are inserted via their parent node, and
 * are automatically linked up with previous and next nodes.
 * </p>
 * <p>
 * The contents of any node (DF) may be retrieved at any time. In this way it is possible to retrieve
 * source code representing fragments of the compilation unit (for example, a type or a method), since
 * the contents of any node (not just the root node) may be obtained.
 * </p>
 * <p>
 * The following manipulations on DFs are distinct:
 * <ul>
 * <li>clone - this creates a stand-alone copy of the DF that is in no way dependent on the DF that it was cloned from</li>
 * <li>remove - this orphans a DF from its host DF. The removed DF may still be dependent on its previous host
 *    (perhaps to generate its contents), and hanging onto the fragment means that its previous host is also
 *    retained in memory.</li>
 * <li>add/insert - this splices an un-parented DF (one that has been cloned, removed, or created stand-alone),
 *    into an existing DF such that the newly inserted DF is only dependent on its new host.</li>
 * </ul>
 * <p>
 * Wherever types are specified in DOM APIs, type names must be specified as they would appear
 * in source code. The DOM does not have a notion of type signatures, only raw text. Example type
 * names are <code>"Object"</code>, <code>"java.io.File"</code>, and <code>"int[]"</code>.
 * </p>
 *
 * @deprecated The JDOM was made obsolete by the addition in 2.0 of the more
 * powerful, fine-grained DOM/AST API found in the
 * org.eclipse.jdt.core.dom package.
 * @noimplement This interface is not intended to be implemented by clients.
 */
@SuppressWarnings("rawtypes")
public interface IDOMNode extends Cloneable  {

	/**
	 * Node type constant indicating a compilation unit.
	 * Nodes of this type maybe by safely cast to <code>IDOMCompilationUnit</code>.
	 * @see #getNodeType()
	 */
	public static int COMPILATION_UNIT= 1;

	/**
	 * Node type constant indicating a package declaration.
	 * Nodes of this type maybe by safely cast to <code>IDOMPackage</code>.
	* @see #getNodeType()
	 */
	public static int PACKAGE= 2;

	/**
	 * Node type constant indicating an import declaration.
	 * Nodes of this type maybe by safely cast to <code>IDOMImport</code>.
	 * @see #getNodeType()
	 */
	public static int IMPORT= 3;

	/**
	 * Node type constant indicating a type declaration.
	 * Nodes of this type maybe by safely cast to <code>IDOMType</code>.
	 * @see #getNodeType()
	 */
	public static int TYPE= 4;

	/**
	 * Node type constant indicating a field declaration.
	 * Nodes of this type maybe by safely cast to <code>IDOMField</code>.
	 * @see #getNodeType()
	 */
	public static int FIELD= 5;

	/**
	 * Node type constant indicating a method (or constructor) declaration.
	 * Nodes of this type maybe by safely cast to <code>IDOMMethod</code>.
	 * @see #getNodeType()
	 */
	public static int METHOD= 6;

	/**
	 * Node type constant indicating an initializer declaration.
	 * Nodes of this type maybe by safely cast to <code>IDOMInitializer</code>.
	 * @see #getNodeType()
	 */
	public static int INITIALIZER= 7;

/**
 * Adds the given un-parented node (document fragment) as the last child of this node.
 *
 * @param child the new child node
 * @exception DOMException if any of the following conditions hold:<ul>
 * <li>this node is not allowed to have children,</li>
 * <li>the child is not of an allowable type</li>
 * <li>the child already has a parent</li>
 * <li>the child is an ancestor of this node</li>
 * </ul>
 * @exception IllegalArgumentException if the child is <code>null</code>
 *
 * @see #insertSibling(IDOMNode)
 * @see #remove()
 */
public void addChild(IDOMNode child) throws DOMException, IllegalArgumentException;
/**
 * Returns whether this node is allowed to have children.
 *
 * @return <code>true</code> if this node can have children
 */
public boolean canHaveChildren();
/**
 * Returns a stand-alone copy of the document fragment represented by this node that
 * is in no way dependent on the document this node is part of.
 *
 * @return a copy of type <code>IDOMNode</code>
 * @see #addChild(IDOMNode)
 * @see #insertSibling(IDOMNode)
 * @see #remove()
 */
public Object clone();
/**
 * Returns the current contents of this document fragment as a character array.
 * <p>
 * Note: To obtain complete source for the source file, ask a compilation unit
 * node for its contents.
 * </p>
 *
 * @return the contents, or <code>null</code> if this node has no contents
 */
public char[] getCharacters();
/**
 * Returns the first named child of this node with the given name.
 *
 * @param name the name
 * @return the child node, or <code>null</code> if no such child exists
 */
public IDOMNode getChild(String name);
/**
 * Returns an enumeration of children of this node. Returns an empty enumeration
 * if this node has no children (including nodes that cannot have children).
 * Children appear in the order in which they are declared in the source code.
 *
 * @return an enumeration of the children
 */
public Enumeration getChildren();
/**
 * Returns the current contents of this document fragment.
 * <p>
 * Note: To obtain complete source for the source file, ask a compilation unit
 * node for its contents.
 * </p>
 *
 * @return the contents, or <code>null</code> if this node has no contents
 */
public String getContents();
/**
 * Returns the first child of this node.
 * Children appear in the order in which they exist in the source code.
 *
 * @return the first child, or <code>null</code> if this node has no children
 * @see #getChildren()
 */
public IDOMNode getFirstChild();
/**
 * Returns a handle for the Java element associated with this
 * document fragment, based on the parent Java element.
 *
 * @param parent the parent Java element
 * @exception IllegalArgumentException if the parent element is not
 *   of a valid parent type for this node
 * @return a handle for the Java element associated with this
 *         document fragment, based on the parent Java element
 */
public IJavaElement getJavaElement(IJavaElement parent) throws IllegalArgumentException;
/**
 * Returns the name of this node.
 * More details are provided in each of the subtypes.
 *
 * @return the name, or <code>null</code> if it has no name
 */
public String getName();
/**
 * Returns the sibling node immediately following this node.
 *
 * @return the next node, or <code>null</code> if there is no following node
 */
public IDOMNode getNextNode();
/**
 * Returns the type of this node.
 *
 * @return one of the node type constants defined in <code>IDOMNode</code>
 */
public int getNodeType();
/**
 * Returns the parent of this node.
 *
 * @return the parent node, or <code>null</code> if this node does not have a
 *   parent
 */
public IDOMNode getParent();
/**
 * Returns the sibling node immediately preceding this node.
 *
 * @return the previous node, or <code>null</code> if there is no preceding node
 */
public IDOMNode getPreviousNode();
/**
 * Inserts the given un-parented node as a sibling of this node, immediately before
 * this node.
 *
 * @param sibling the new sibling node
 * @exception DOMException if any of the following conditions hold:<ul>
 * <li>this node is a document fragment root</li>
 * <li>the sibling is not of the correct type</li>
 * <li>the sibling already has a parent</li>
 * <li>this sibling is an ancestor of this node</li>
 * </ul>
 * @exception IllegalArgumentException if the sibling is <code>null</code>
 *
 * @see #addChild(IDOMNode)
 * @see #clone()
 * @see #remove()
 */
public void insertSibling(IDOMNode sibling) throws DOMException, IllegalArgumentException;
/**
 * Returns whether the given node is an allowable child for this node.
 *
 * @param node the potential child node
 * @return <code>true</code> if the given node is an allowable child
 */
public boolean isAllowableChild(IDOMNode node);
/**
 * Returns whether this node's signature is equivalent to the given
 * node's signature. In other words, if the nodes were siblings,
 * would the declarations collide because they represent the same declaration.
 *
 * @param node the other node
 * @return <code>true</code> if the nodes have equivalent signatures
 */
public boolean isSignatureEqual(IDOMNode node);
/**
 * Separates this node from its parent and siblings, maintaining any ties that this node
 * has to the underlying document fragment. A document fragment that is removed
 * from its host document may still be dependent on that host document until it is
 * inserted into a different document. Removing a root node has no effect.
 *
 * @see #addChild(IDOMNode)
 * @see #clone()
 * @see #insertSibling(IDOMNode)
 */
public void remove();
/**
 * Sets the name of this node. Name format depends on node type.
 * More details are provided in each of the subtypes.
 *
 * @param name the name, or <code>null</code> to clear the name
 */
public void setName(String name);
}
