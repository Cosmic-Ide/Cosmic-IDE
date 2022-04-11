/*******************************************************************************
 * Copyright (c) 2004, 2019 IBM Corporation and others.
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
 * Abstract base class for all AST nodes that represent comments.
 * There are exactly three kinds of comment:
 * line comments ({@link LineComment}),
 * block comments ({@link BlockComment}), and
 * doc comments ({@link Javadoc}).
 * <pre>
 * Comment:
 *     LineComment
 *     BlockComment
 *     Javadoc
 * </pre>
 *
 * @since 3.0
 */
public abstract class Comment extends ASTNode {

	/**
	 * Alternate root node, or <code>null</code> if none.
	 * Initially <code>null</code>.
	 */
	private ASTNode alternateRoot = null;

	/**
	 * Creates a new AST node for a comment owned by the given AST.
	 * <p>
	 * N.B. This constructor is package-private.
	 * </p>
	 *
	 * @param ast the AST that is to own this node
	 */
	Comment(AST ast) {
		super(ast);
	}

	/**
	 * Returns whether this comment is a block comment
	 * (<code>BlockComment</code>).
	 *
	 * @return <code>true</code> if this is a block comment, and
	 *    <code>false</code> otherwise
	 */
	public final boolean isBlockComment() {
		return (this instanceof BlockComment);
	}

	/**
	 * Returns whether this comment is a line comment
	 * (<code>LineComment</code>).
	 *
	 * @return <code>true</code> if this is a line comment, and
	 *    <code>false</code> otherwise
	 */
	public final boolean isLineComment() {
		return (this instanceof LineComment);
	}

	/**
	 * Returns whether this comment is a doc comment
	 * (<code>Javadoc</code>).
	 *
	 * @return <code>true</code> if this is a doc comment, and
	 *    <code>false</code> otherwise
	 */
	public final boolean isDocComment() {
		return (this instanceof Javadoc);
	}

	/**
	 * Returns the root AST node that this comment occurs
	 * within, or <code>null</code> if none (or not recorded).
	 * <p>
	 * Typically, the comment nodes created while parsing a compilation
	 * unit are not considered descendents of the normal AST
	 * root, namely an {@link CompilationUnit}. Instead, these
	 * comment nodes exist outside the normal AST and each is
	 * a root in its own right. This optional property provides
	 * a well-known way to navigate from the comment to the
	 * compilation unit in such cases. Note that the alternate root
	 * property is not one of the comment node's children. It is simply a
	 * reference to a node.
	 * </p>
	 *
	 * @return the alternate root node, or <code>null</code>
	 * if none
	 * @see #setAlternateRoot(ASTNode)
	 */
	public final ASTNode getAlternateRoot() {
		return this.alternateRoot;
	}

	/**
	 * Returns the root AST node that this comment occurs
	 * within, or <code>null</code> if none (or not recorded).
	 *
	 * @param root the alternate root node, or <code>null</code>
	 * if none
	 * @see #getAlternateRoot()
	 */
	public final void setAlternateRoot(ASTNode root) {
		// alternate root is *not* considered a structural property
		// but we protect them nevertheless
		checkModifiable();
		this.alternateRoot = root;
	}

	@Override
	int memSize() {
		return BASE_NODE_SIZE + 1 * 4;
	}
}
