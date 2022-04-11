/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
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
package org.eclipse.jdt.core.dom;

/**
 * A node event handler is an internal mechanism for receiving
 * notification of changes to nodes in an AST.
 * <p>
 * The default implementation serves as the default event handler
 * that does nothing. Internal subclasses do all the real work.
 * </p>
 *
 * @see AST#getEventHandler()
 */
class NodeEventHandler {

	/**
	 * Creates a node event handler.
	 */
	NodeEventHandler() {
		// default implementation: do nothing
	}

	/**
	 * Reports that the given node is about to lose a child.
	 * The first half of an event pair. The default implementation does nothing.
	 *
	 * @param node the node about to be modified
	 * @param child the node about to be removed
	 * @param property the child or child list property descriptor
	 * @see #postRemoveChildEvent(ASTNode, ASTNode, StructuralPropertyDescriptor)
	 * @since 3.0
	 */
	void preRemoveChildEvent(ASTNode node, ASTNode child, StructuralPropertyDescriptor property) {
		// do nothing
		// System.out.println("DEL1 " + property);
	}

	/**
	 * Reports that the given node has just lose a child.
	 * The second half of an event pair. The default implementation does nothing.
	 *
	 * @param node the node that was modified
	 * @param child the child that was removed; note that this node is unparented
	 * @param property the child or child list property descriptor
	 * @see #preRemoveChildEvent(ASTNode, ASTNode, StructuralPropertyDescriptor)
	 * @since 3.0
	 */
	void postRemoveChildEvent(ASTNode node, ASTNode child, StructuralPropertyDescriptor property) {
		// do nothing
		// System.out.println("DEL2 " + property);
	}

	/**
	 * Reports that the given node is about to have a child replaced.
	 * The first half of an event pair.
	 * The default implementation does nothing.
	 *
	 * @param node the node about to be modified
	 * @param child the node about to be replaced
	 * @param newChild the replacement child; note that this node is unparented
	 * @param property the child or child list property descriptor
	 * @see #preReplaceChildEvent(ASTNode, ASTNode, ASTNode, StructuralPropertyDescriptor)
	 * @since 3.0
	 */
	void preReplaceChildEvent(ASTNode node, ASTNode child, ASTNode newChild, StructuralPropertyDescriptor property) {
		// do nothing
		// System.out.println("REP1 " + property);
	}

	/**
	 * Reports that the given node has had its child replaced. The second half
	 * of an event pair. The default implementation does nothing.
	 *
	 * @param node the node that was modified
	 * @param child the node that was replaced; note that this node is unparented
	 * @param newChild the replacement child
	 * @param property the child or child list property descriptor
	 * @see #postReplaceChildEvent(ASTNode, ASTNode, ASTNode, StructuralPropertyDescriptor)
	 * @since 3.0
	 */
	void postReplaceChildEvent(ASTNode node, ASTNode child, ASTNode newChild, StructuralPropertyDescriptor property) {
		// do nothing
		// System.out.println("REP2 " + property);
	}

	/**
	 * Reports that the given node is about to gain a child.
	 * The first half of an event pair. The default implementation does nothing.
	 *
	 * @param node the node that to be modified
	 * @param child the node that is to be added as a child; note that this
	 * node is unparented; in the case of a child list property, the exact
	 * location of insertion is not supplied (but is known on the
	 * corresponding <code>postAddChildEvent</code> to
	 * follow)
	 * @param property the child or child list property descriptor
	 * @see #postAddChildEvent(ASTNode, ASTNode, StructuralPropertyDescriptor)
	 * @since 3.0
	 */
	void preAddChildEvent(ASTNode node, ASTNode child, StructuralPropertyDescriptor property) {
		// do nothing
		// System.out.println("ADD1 " + property);
	}

	/**
	 * Reports that the given node has just gained a child.
	 * The second half of an event pair. The default implementation does nothing.
	 *
	 * @param node the node that was modified
	 * @param child the node that was added as a child
	 * @param property the child or child list property descriptor
	 * @see #preAddChildEvent(ASTNode, ASTNode, StructuralPropertyDescriptor)
	 * @since 3.0
	 */
	void postAddChildEvent(ASTNode node, ASTNode child, StructuralPropertyDescriptor property) {
		// do nothing
		// System.out.println("ADD2 " + property);
	}

	/**
	 * Reports that the given node is about to change the value of a
	 * non-child property. The first half of an event pair.
	 * The default implementation does nothing.
	 *
	 * @param node the node to be modified
	 * @param property the property descriptor
	 * @see #postValueChangeEvent(ASTNode, SimplePropertyDescriptor)
	 * @since 3.0
	 */
	void preValueChangeEvent(ASTNode node, SimplePropertyDescriptor property) {
		// do nothing
		// System.out.println("MOD1 " + property);
	}

	/**
	 * Reports that the given node has just changed the value of a
	 * non-child property. The second half of an event pair.
	 * The default implementation does nothing.
	 *
	 * @param node the node that was modified
	 * @param property the property descriptor
	 * @see #preValueChangeEvent(ASTNode, SimplePropertyDescriptor)
	 * @since 3.0
	 */
	void postValueChangeEvent(ASTNode node, SimplePropertyDescriptor property) {
		// do nothing
		// System.out.println("MOD2 " + property);
	}

	/**
	 * Reports that the given node is about to be cloned.
	 * The first half of an event pair.
	 * The default implementation does nothing.
	 *
	 * @param node the node to be modified
	 * @see #postCloneNodeEvent(ASTNode, ASTNode)
	 * @since 3.0
	 */
	void preCloneNodeEvent(ASTNode node) {
		// do nothing
		// System.out.println("CLONE1");
	}

	/**
	 * Reports that the given node has just been cloned.
	 * The second half of an event pair.
	 * The default implementation does nothing.
	 *
	 * @param node the node that was modified
	 * @param clone the clone of <code>node</code>
	 * @see #preCloneNodeEvent(ASTNode)
	 * @since 3.0
	 */
	void postCloneNodeEvent(ASTNode node, ASTNode clone) {
		// do nothing
		// System.out.println("CLONE2");
	}

}
