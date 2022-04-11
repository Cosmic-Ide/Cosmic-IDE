/*******************************************************************************
 * Copyright (c) 2000, 2021 IBM Corporation and others.
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

import java.util.Iterator;
import java.util.List;

import org.eclipse.jdt.internal.core.dom.util.DOMASTUtil;

/**
 * Concrete superclass and default implementation of an AST subtree matcher.
 * <p>
 * For example, to compute whether two ASTs subtrees are structurally
 * isomorphic, use <code>n1.subtreeMatch(new ASTMatcher(), n2)</code> where
 * <code>n1</code> and <code>n2</code> are the AST root nodes of the subtrees.
 * </p>
 * <p>
 * For each different concrete AST node type <i>T</i> there is a
 * <code>public boolean match(<i>T</i> node, Object other)</code> method
 * that matches the given node against another object (typically another
 * AST node, although this is not essential). The default implementations
 * provided by this class tests whether the other object is a node of the
 * same type with structurally isomorphic child subtrees. For nodes with
 * list-valued properties, the child nodes within the list are compared in
 * order. For nodes with multiple properties, the child nodes are compared
 * in the order that most closely corresponds to the lexical reading order
 * of the source program. For instance, for a type declaration node, the
 * child ordering is: name, superclass, superinterfaces, and body
 * declarations.
 * </p>
 * <p>
 * Subclasses may override (extend or reimplement) some or all of the
 * <code>match</code> methods in order to define more specialized subtree
 * matchers.
 * </p>
 *
 * @see org.eclipse.jdt.core.dom.ASTNode#subtreeMatch(ASTMatcher, Object)
 * @since 2.0
 */
@SuppressWarnings("rawtypes")
public class ASTMatcher {

	/**
	 * Indicates whether doc tags should be matched.
	 * @since 3.0
	 */
	private boolean matchDocTags;

	/**
	 * Creates a new AST matcher instance.
	 * <p>
	 * For backwards compatibility, the matcher ignores tag
	 * elements below doc comments by default. Use
	 * {@link #ASTMatcher(boolean) ASTMatcher(true)}
	 * for a matcher that compares doc tags by default.
	 * </p>
	 */
	public ASTMatcher() {
		this(false);
	}

	/**
	 * Creates a new AST matcher instance.
	 *
	 * @param matchDocTags <code>true</code> if doc comment tags are
	 * to be compared by default, and <code>false</code> otherwise
	 * @see #match(Javadoc,Object)
	 * @since 3.0
	 */
	public ASTMatcher(boolean matchDocTags) {
		this.matchDocTags = matchDocTags;
	}

	/**
	 * Returns whether the given lists of AST nodes match pair wise according
	 * to <code>ASTNode.subtreeMatch</code>.
	 * <p>
	 * Note that this is a convenience method, useful for writing recursive
	 * subtree matchers.
	 * </p>
	 *
	 * @param list1 the first list of AST nodes
	 *    (element type: {@link ASTNode})
	 * @param list2 the second list of AST nodes
	 *    (element type: {@link ASTNode})
	 * @return <code>true</code> if the lists have the same number of elements
	 *    and match pair-wise according to {@link ASTNode#subtreeMatch(ASTMatcher, Object) ASTNode.subtreeMatch}
	 * @see ASTNode#subtreeMatch(ASTMatcher matcher, Object other)
	 */
	public final boolean safeSubtreeListMatch(List list1, List list2) {
		int size1 = list1.size();
		int size2 = list2.size();
		if (size1 != size2) {
			return false;
		}
		for (Iterator it1 = list1.iterator(), it2 = list2.iterator(); it1.hasNext();) {
			ASTNode n1 = (ASTNode) it1.next();
			ASTNode n2 = (ASTNode) it2.next();
			if (!n1.subtreeMatch(this, n2)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Returns whether the given nodes match according to
	 * <code>AST.subtreeMatch</code>. Returns <code>false</code> if one or
	 * the other of the nodes are <code>null</code>. Returns <code>true</code>
	 * if both nodes are <code>null</code>.
	 * <p>
	 * Note that this is a convenience method, useful for writing recursive
	 * subtree matchers.
	 * </p>
	 *
	 * @param node1 the first AST node, or <code>null</code>; must be an
	 *    instance of <code>ASTNode</code>
	 * @param node2 the second AST node, or <code>null</code>; must be an
	 *    instance of <code>ASTNode</code>
	 * @return <code>true</code> if the nodes match according
	 *    to <code>AST.subtreeMatch</code> or both are <code>null</code>, and
	 *    <code>false</code> otherwise
	 * @see ASTNode#subtreeMatch(ASTMatcher, Object)
	 */
	public final boolean safeSubtreeMatch(Object node1, Object node2) {
		if (node1 == null && node2 == null) {
			return true;
		}
		if (node1 == null || node2 == null) {
			return false;
		}
		// N.B. call subtreeMatch even node1==node2!=null
		return ((ASTNode) node1).subtreeMatch(this, node2);
	}

	/**
	 * Returns whether the given objects are equal according to
	 * <code>equals</code>. Returns <code>false</code> if either
	 * node is <code>null</code>.
	 *
	 * @param o1 the first object, or <code>null</code>
	 * @param o2 the second object, or <code>null</code>
	 * @return <code>true</code> if the nodes are equal according to
	 *    <code>equals</code> or both <code>null</code>, and
	 *    <code>false</code> otherwise
	 */
	public static boolean safeEquals(Object o1, Object o2) {
		if (o1 == o2) {
			return true;
		}
		if (o1 == null || o2 == null) {
			return false;
		}
		return o1.equals(o2);
	}

	/**
	 * @deprecated
	 */
	private Type componentType(ArrayType array) {
		return array.getComponentType();
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 * @since 3.1
	 */
	public boolean match(AnnotationTypeDeclaration node, Object other) {
		if (!(other instanceof AnnotationTypeDeclaration)) {
			return false;
		}
		AnnotationTypeDeclaration o = (AnnotationTypeDeclaration) other;
		// node type added in JLS3 - ignore old JLS2-style modifiers
		return (safeSubtreeMatch(node.getJavadoc(), o.getJavadoc())
				&& safeSubtreeListMatch(node.modifiers(), o.modifiers())
				&& safeSubtreeMatch(node.getName(), o.getName())
				&& safeSubtreeListMatch(node.bodyDeclarations(), o.bodyDeclarations()));
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 * @since 3.1
	 */
	public boolean match(AnnotationTypeMemberDeclaration node, Object other) {
		if (!(other instanceof AnnotationTypeMemberDeclaration)) {
			return false;
		}
		AnnotationTypeMemberDeclaration o = (AnnotationTypeMemberDeclaration) other;
		// node type added in JLS3 - ignore old JLS2-style modifiers
		return (safeSubtreeMatch(node.getJavadoc(), o.getJavadoc())
				&& safeSubtreeListMatch(node.modifiers(), o.modifiers())
				&& safeSubtreeMatch(node.getType(), o.getType())
				&& safeSubtreeMatch(node.getName(), o.getName())
				&& safeSubtreeMatch(node.getDefault(), o.getDefault()));
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(AnonymousClassDeclaration node, Object other) {
		if (!(other instanceof AnonymousClassDeclaration)) {
			return false;
		}
		AnonymousClassDeclaration o = (AnonymousClassDeclaration) other;
		return safeSubtreeListMatch(node.bodyDeclarations(), o.bodyDeclarations());
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(ArrayAccess node, Object other) {
		if (!(other instanceof ArrayAccess)) {
			return false;
		}
		ArrayAccess o = (ArrayAccess) other;
		return (
			safeSubtreeMatch(node.getArray(), o.getArray())
				&& safeSubtreeMatch(node.getIndex(), o.getIndex()));
	}

	/**
	 * Returns whether the given node and the other object object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(ArrayCreation node, Object other) {
		if (!(other instanceof ArrayCreation)) {
			return false;
		}
		ArrayCreation o = (ArrayCreation) other;
		return (
			safeSubtreeMatch(node.getType(), o.getType())
				&& safeSubtreeListMatch(node.dimensions(), o.dimensions())
				&& safeSubtreeMatch(node.getInitializer(), o.getInitializer()));
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(ArrayInitializer node, Object other) {
		if (!(other instanceof ArrayInitializer)) {
			return false;
		}
		ArrayInitializer o = (ArrayInitializer) other;
		return safeSubtreeListMatch(node.expressions(), o.expressions());
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(ArrayType node, Object other) {
		if (!(other instanceof ArrayType)) {
			return false;
		}
		ArrayType o = (ArrayType) other;
		int level = node.getAST().apiLevel;
		if (level < AST.JLS8_INTERNAL) {
			return safeSubtreeMatch(componentType(node), componentType(o));
		}
		return safeSubtreeMatch(node.getElementType(), o.getElementType())
				&& safeSubtreeListMatch(node.dimensions(), o.dimensions());
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(AssertStatement node, Object other) {
		if (!(other instanceof AssertStatement)) {
			return false;
		}
		AssertStatement o = (AssertStatement) other;
		return (
			safeSubtreeMatch(node.getExpression(), o.getExpression())
				&& safeSubtreeMatch(node.getMessage(), o.getMessage()));
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(Assignment node, Object other) {
		if (!(other instanceof Assignment)) {
			return false;
		}
		Assignment o = (Assignment) other;
		return (
			node.getOperator().equals(o.getOperator())
				&& safeSubtreeMatch(node.getLeftHandSide(), o.getLeftHandSide())
				&& safeSubtreeMatch(node.getRightHandSide(), o.getRightHandSide()));
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(Block node, Object other) {
		if (!(other instanceof Block)) {
			return false;
		}
		Block o = (Block) other;
		return safeSubtreeListMatch(node.statements(), o.statements());
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type. Subclasses may override
	 * this method as needed.
	 * </p>
	 * <p>Note: {@link LineComment} and {@link BlockComment} nodes are
	 * not considered part of main structure of the AST. This method will
	 * only be called if a client goes out of their way to visit this
	 * kind of node explicitly.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 * @since 3.0
	 */
	public boolean match(BlockComment node, Object other) {
		if (!(other instanceof BlockComment)) {
			return false;
		}
		return true;
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(BooleanLiteral node, Object other) {
		if (!(other instanceof BooleanLiteral)) {
			return false;
		}
		BooleanLiteral o = (BooleanLiteral) other;
		return node.booleanValue() == o.booleanValue();
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(BreakStatement node, Object other) {
		if (!(other instanceof BreakStatement)) {
			return false;
		}
		BreakStatement o = (BreakStatement) other;
		return (safeSubtreeMatch(node.getLabel(), o.getLabel()));
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 *  @since 3.28
	 */
	public boolean match(CaseDefaultExpression node, Object other) {
		if (!(other instanceof CaseDefaultExpression)) {
			return false;
		}
		return true;
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(CastExpression node, Object other) {
		if (!(other instanceof CastExpression)) {
			return false;
		}
		CastExpression o = (CastExpression) other;
		return (
			safeSubtreeMatch(node.getType(), o.getType())
				&& safeSubtreeMatch(node.getExpression(), o.getExpression()));
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(CatchClause node, Object other) {
		if (!(other instanceof CatchClause)) {
			return false;
		}
		CatchClause o = (CatchClause) other;
		return (
			safeSubtreeMatch(node.getException(), o.getException())
				&& safeSubtreeMatch(node.getBody(), o.getBody()));
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(CharacterLiteral node, Object other) {
		if (!(other instanceof CharacterLiteral)) {
			return false;
		}
		CharacterLiteral o = (CharacterLiteral) other;
		return safeEquals(node.getEscapedValue(), o.getEscapedValue());
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(ClassInstanceCreation node, Object other) {
		if (!(other instanceof ClassInstanceCreation)) {
			return false;
		}
		ClassInstanceCreation o = (ClassInstanceCreation) other;
		int level = node.getAST().apiLevel;
		if (level == AST.JLS2_INTERNAL) {
			if (!safeSubtreeMatch(node.internalGetName(), o.internalGetName())) {
				return false;
			}
		}
		if (level >= AST.JLS3_INTERNAL) {
			if (!safeSubtreeListMatch(node.typeArguments(), o.typeArguments())) {
				return false;
			}
			if (!safeSubtreeMatch(node.getType(), o.getType())) {
				return false;
			}
		}
		return
			safeSubtreeMatch(node.getExpression(), o.getExpression())
				&& safeSubtreeListMatch(node.arguments(), o.arguments())
				&& safeSubtreeMatch(
					node.getAnonymousClassDeclaration(),
					o.getAnonymousClassDeclaration());
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(CompilationUnit node, Object other) {
		if (!(other instanceof CompilationUnit)) {
			return false;
		}
		CompilationUnit o = (CompilationUnit) other;
		return (
			(node.getAST().apiLevel >= AST.JLS9_INTERNAL ? safeSubtreeMatch(node.getModule(), o.getModule()) : true)
				&& safeSubtreeMatch(node.getPackage(), o.getPackage())
				&& safeSubtreeListMatch(node.imports(), o.imports())
				&& safeSubtreeListMatch(node.types(), o.types()));
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(ConditionalExpression node, Object other) {
		if (!(other instanceof ConditionalExpression)) {
			return false;
		}
		ConditionalExpression o = (ConditionalExpression) other;
		return (
			safeSubtreeMatch(node.getExpression(), o.getExpression())
				&& safeSubtreeMatch(node.getThenExpression(), o.getThenExpression())
				&& safeSubtreeMatch(node.getElseExpression(), o.getElseExpression()));
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(ConstructorInvocation node, Object other) {
		if (!(other instanceof ConstructorInvocation)) {
			return false;
		}
		ConstructorInvocation o = (ConstructorInvocation) other;
		if (node.getAST().apiLevel >= AST.JLS3_INTERNAL) {
			if (!safeSubtreeListMatch(node.typeArguments(), o.typeArguments())) {
				return false;
			}
		}
		return safeSubtreeListMatch(node.arguments(), o.arguments());
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(ContinueStatement node, Object other) {
		if (!(other instanceof ContinueStatement)) {
			return false;
		}
		ContinueStatement o = (ContinueStatement) other;
		return safeSubtreeMatch(node.getLabel(), o.getLabel());
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 * @since 3.10
	 */
	public boolean match(CreationReference node, Object other) {
		if (!(other instanceof CreationReference)) {
			return false;
		}
		CreationReference o = (CreationReference) other;
		return (
			safeSubtreeMatch(node.getType(), o.getType())
				&& safeSubtreeListMatch(node.typeArguments(), o.typeArguments()));
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 * @since 3.10
	 */
	public boolean match(Dimension node, Object other) {
		if (!(other instanceof Dimension)) {
			return false;
		}
		Dimension o = (Dimension) other;
		return safeSubtreeListMatch(node.annotations(), o.annotations());
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(DoStatement node, Object other) {
		if (!(other instanceof DoStatement)) {
			return false;
		}
		DoStatement o = (DoStatement) other;
		return (
			safeSubtreeMatch(node.getExpression(), o.getExpression())
				&& safeSubtreeMatch(node.getBody(), o.getBody()));
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(EmptyStatement node, Object other) {
		if (!(other instanceof EmptyStatement)) {
			return false;
		}
		return true;
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 * @since 3.1
	 */
	public boolean match(EnhancedForStatement node, Object other) {
		if (!(other instanceof EnhancedForStatement)) {
			return false;
		}
		EnhancedForStatement o = (EnhancedForStatement) other;
		return (
			safeSubtreeMatch(node.getParameter(), o.getParameter())
				&& safeSubtreeMatch(node.getExpression(), o.getExpression())
				&& safeSubtreeMatch(node.getBody(), o.getBody()));
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 * @since 3.1
	 */
	public boolean match(EnumConstantDeclaration node, Object other) {
		if (!(other instanceof EnumConstantDeclaration)) {
			return false;
		}
		EnumConstantDeclaration o = (EnumConstantDeclaration) other;
		return (
			safeSubtreeMatch(node.getJavadoc(), o.getJavadoc())
				&& safeSubtreeListMatch(node.modifiers(), o.modifiers())
				&& safeSubtreeMatch(node.getName(), o.getName())
				&& safeSubtreeListMatch(node.arguments(), o.arguments())
				&& safeSubtreeMatch(
					node.getAnonymousClassDeclaration(),
					o.getAnonymousClassDeclaration()));
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 * @since 3.1
	 */
	public boolean match(EnumDeclaration node, Object other) {
		if (!(other instanceof EnumDeclaration)) {
			return false;
		}
		EnumDeclaration o = (EnumDeclaration) other;
		return (
			safeSubtreeMatch(node.getJavadoc(), o.getJavadoc())
				&& safeSubtreeListMatch(node.modifiers(), o.modifiers())
				&& safeSubtreeMatch(node.getName(), o.getName())
				&& safeSubtreeListMatch(node.superInterfaceTypes(), o.superInterfaceTypes())
				&& safeSubtreeListMatch(node.enumConstants(), o.enumConstants())
				&& safeSubtreeListMatch(
					node.bodyDeclarations(),
					o.bodyDeclarations()));
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 * @since 3.14
	 */
	public boolean match(ExportsDirective node, Object other) {
		if (!(other instanceof ExportsDirective)) {
			return false;
		}
		ExportsDirective o = (ExportsDirective) other;
		return (
			safeSubtreeMatch(node.getName(), o.getName())
				&& safeSubtreeListMatch(node.modules(), o.modules()));
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 * @since 3.10
	 */
	public boolean match(ExpressionMethodReference node, Object other) {
		if (!(other instanceof ExpressionMethodReference)) {
			return false;
		}
		ExpressionMethodReference o = (ExpressionMethodReference) other;
		return (
			safeSubtreeMatch(node.getExpression(), o.getExpression())
				&& safeSubtreeListMatch(node.typeArguments(), o.typeArguments())
				&& safeSubtreeMatch(node.getName(), o.getName()));
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(ExpressionStatement node, Object other) {
		if (!(other instanceof ExpressionStatement)) {
			return false;
		}
		ExpressionStatement o = (ExpressionStatement) other;
		return safeSubtreeMatch(node.getExpression(), o.getExpression());
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(FieldAccess node, Object other) {
		if (!(other instanceof FieldAccess)) {
			return false;
		}
		FieldAccess o = (FieldAccess) other;
		return (
			safeSubtreeMatch(node.getExpression(), o.getExpression())
				&& safeSubtreeMatch(node.getName(), o.getName()));
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(FieldDeclaration node, Object other) {
		if (!(other instanceof FieldDeclaration)) {
			return false;
		}
		FieldDeclaration o = (FieldDeclaration) other;
		int level = node.getAST().apiLevel;
		if (level == AST.JLS2_INTERNAL) {
			if (node.getModifiers() != o.getModifiers()) {
				return false;
			}
		}
		if (level >= AST.JLS3_INTERNAL) {
			if (!safeSubtreeListMatch(node.modifiers(), o.modifiers())) {
				return false;
			}
		}
		return
			safeSubtreeMatch(node.getJavadoc(), o.getJavadoc())
			&& safeSubtreeMatch(node.getType(), o.getType())
			&& safeSubtreeListMatch(node.fragments(), o.fragments());
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(ForStatement node, Object other) {
		if (!(other instanceof ForStatement)) {
			return false;
		}
		ForStatement o = (ForStatement) other;
		return (
			safeSubtreeListMatch(node.initializers(), o.initializers())
				&& safeSubtreeMatch(node.getExpression(), o.getExpression())
				&& safeSubtreeListMatch(node.updaters(), o.updaters())
				&& safeSubtreeMatch(node.getBody(), o.getBody()));
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 * @since 3.28
	 */
	public boolean match(GuardedPattern node, Object other) {
		if (!(other instanceof GuardedPattern)) {
			return false;
		}
		GuardedPattern o = (GuardedPattern) other;
		return  safeSubtreeMatch(node.getPattern(), o.getPattern())
				&& safeSubtreeMatch(node.getExpression(), o.getExpression());
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(IfStatement node, Object other) {
		if (!(other instanceof IfStatement)) {
			return false;
		}
		IfStatement o = (IfStatement) other;
		return (
			safeSubtreeMatch(node.getExpression(), o.getExpression())
				&& safeSubtreeMatch(node.getThenStatement(), o.getThenStatement())
				&& safeSubtreeMatch(node.getElseStatement(), o.getElseStatement()));
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(ImportDeclaration node, Object other) {
		if (!(other instanceof ImportDeclaration)) {
			return false;
		}
		ImportDeclaration o = (ImportDeclaration) other;
		if (node.getAST().apiLevel >= AST.JLS3_INTERNAL) {
			if (node.isStatic() != o.isStatic()) {
				return false;
			}
		}
		return (
			safeSubtreeMatch(node.getName(), o.getName())
				&& node.isOnDemand() == o.isOnDemand());
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(InfixExpression node, Object other) {
		if (!(other instanceof InfixExpression)) {
			return false;
		}
		InfixExpression o = (InfixExpression) other;
		// be careful not to trigger lazy creation of extended operand lists
		if (node.hasExtendedOperands() && o.hasExtendedOperands()) {
			if (!safeSubtreeListMatch(node.extendedOperands(), o.extendedOperands())) {
				return false;
			}
		}
		if (node.hasExtendedOperands() != o.hasExtendedOperands()) {
			return false;
		}
		return (
			node.getOperator().equals(o.getOperator())
				&& safeSubtreeMatch(node.getLeftOperand(), o.getLeftOperand())
				&& safeSubtreeMatch(node.getRightOperand(), o.getRightOperand()));
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(Initializer node, Object other) {
		if (!(other instanceof Initializer)) {
			return false;
		}
		Initializer o = (Initializer) other;
		int level = node.getAST().apiLevel;
		if (level == AST.JLS2_INTERNAL) {
			if (node.getModifiers() != o.getModifiers()) {
				return false;
			}
		}
		if (level >= AST.JLS3_INTERNAL) {
			if (!safeSubtreeListMatch(node.modifiers(), o.modifiers())) {
				return false;
			}
		}
		return (
				safeSubtreeMatch(node.getJavadoc(), o.getJavadoc())
				&& safeSubtreeMatch(node.getBody(), o.getBody()));
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(InstanceofExpression node, Object other) {
		if (!(other instanceof InstanceofExpression)) {
			return false;
		}
		InstanceofExpression o = (InstanceofExpression) other;
		return
			safeSubtreeMatch(node.getLeftOperand(), o.getLeftOperand())
			&& safeSubtreeMatch(node.getRightOperand(), o.getRightOperand());
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 * @since 3.10
	 */
	public boolean match(IntersectionType node, Object other) {
		if (!(other instanceof IntersectionType)) {
			return false;
		}
		IntersectionType o = (IntersectionType) other;
		return safeSubtreeListMatch(node.types(), o.types());
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * Unlike other node types, the behavior of the default
	 * implementation is controlled by a constructor-supplied
	 * parameter  {@link #ASTMatcher(boolean) ASTMatcher(boolean)}
	 * which is <code>false</code> if not specified.
	 * When this parameter is <code>true</code>, the implementation
	 * tests whether the other object is also a <code>Javadoc</code>
	 * with structurally isomorphic child subtrees; the comment string
	 * (<code>Javadoc.getComment()</code>) is ignored.
	 * Conversely, when the parameter is <code>false</code>, the
	 * implementation tests whether the other object is also a
	 * <code>Javadoc</code> with exactly the same comment string;
	 * the tag elements ({@link Javadoc#tags() Javadoc.tags} are
	 * ignored. Subclasses may reimplement.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 * @see #ASTMatcher()
	 * @see #ASTMatcher(boolean)
	 */
	public boolean match(Javadoc node, Object other) {
		if (!(other instanceof Javadoc)) {
			return false;
		}
		Javadoc o = (Javadoc) other;
		if (this.matchDocTags) {
			return safeSubtreeListMatch(node.tags(), o.tags());
		} else {
			return compareDeprecatedComment(node, o);
		}
	}

	/**
	 * Return whether the deprecated comment strings of the given java doc are equals.
	 * <p>
	 * Note the only purpose of this method is to hide deprecated warnings.
	 * @deprecated mark deprecated to hide deprecated usage
	 */
	private boolean compareDeprecatedComment(Javadoc first, Javadoc second) {
		if (first.getAST().apiLevel == AST.JLS2_INTERNAL) {
			return safeEquals(first.getComment(), second.getComment());
		} else {
			return true;
		}
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(LabeledStatement node, Object other) {
		if (!(other instanceof LabeledStatement)) {
			return false;
		}
		LabeledStatement o = (LabeledStatement) other;
		return (
			safeSubtreeMatch(node.getLabel(), o.getLabel())
				&& safeSubtreeMatch(node.getBody(), o.getBody()));
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 * @since 3.10
	 */
	public boolean match(LambdaExpression node, Object other) {
		if (!(other instanceof LambdaExpression)) {
			return false;
		}
		LambdaExpression o = (LambdaExpression) other;
		return	(node.hasParentheses() == o.hasParentheses())
				&& safeSubtreeListMatch(node.parameters(), o.parameters())
				&& safeSubtreeMatch(node.getBody(), o.getBody());
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type. Subclasses may override
	 * this method as needed.
	 * </p>
	 * <p>Note: {@link LineComment} and {@link BlockComment} nodes are
	 * not considered part of main structure of the AST. This method will
	 * only be called if a client goes out of their way to visit this
	 * kind of node explicitly.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 * @since 3.0
	 */
	public boolean match(LineComment node, Object other) {
		if (!(other instanceof LineComment)) {
			return false;
		}
		return true;
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 * @since 3.1
	 */
	public boolean match(MarkerAnnotation node, Object other) {
		if (!(other instanceof MarkerAnnotation)) {
			return false;
		}
		MarkerAnnotation o = (MarkerAnnotation) other;
		return safeSubtreeMatch(node.getTypeName(), o.getTypeName());
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 * @since 3.0
	 */
	public boolean match(MemberRef node, Object other) {
		if (!(other instanceof MemberRef)) {
			return false;
		}
		MemberRef o = (MemberRef) other;
		return (
				safeSubtreeMatch(node.getQualifier(), o.getQualifier())
				&& safeSubtreeMatch(node.getName(), o.getName()));
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 * @since 3.1
	 */
	public boolean match(MemberValuePair node, Object other) {
		if (!(other instanceof MemberValuePair)) {
			return false;
		}
		MemberValuePair o = (MemberValuePair) other;
		return (safeSubtreeMatch(node.getName(), o.getName())
				&& safeSubtreeMatch(node.getValue(), o.getValue()));
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 * @since 3.0
	 */
	public boolean match(MethodRef node, Object other) {
		if (!(other instanceof MethodRef)) {
			return false;
		}
		MethodRef o = (MethodRef) other;
		return (
				safeSubtreeMatch(node.getQualifier(), o.getQualifier())
				&& safeSubtreeMatch(node.getName(), o.getName())
		        && safeSubtreeListMatch(node.parameters(), o.parameters()));
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 * @since 3.0
	 */
	public boolean match(MethodRefParameter node, Object other) {
		if (!(other instanceof MethodRefParameter)) {
			return false;
		}
		MethodRefParameter o = (MethodRefParameter) other;
		int level = node.getAST().apiLevel;
		if (level >= AST.JLS3_INTERNAL) {
			if (node.isVarargs() != o.isVarargs()) {
				return false;
			}
		}
		return (
				safeSubtreeMatch(node.getType(), o.getType())
				&& safeSubtreeMatch(node.getName(), o.getName()));
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 * <p>
	 * Note that extra array dimensions are compared since they are an
	 * important part of the method declaration.
	 * </p>
	 * <p>
	 * Note that the method return types are compared even for constructor
	 * declarations.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(MethodDeclaration node, Object other) {
		if (!(other instanceof MethodDeclaration)) {
			return false;
		}
		MethodDeclaration o = (MethodDeclaration) other;
		int level = node.getAST().apiLevel;
		return node.isConstructor() == o.isConstructor()
				&& safeSubtreeMatch(node.getJavadoc(), o.getJavadoc())
				&& (level >= AST.JLS3_INTERNAL
						? safeSubtreeListMatch(node.modifiers(), o.modifiers())
								&& safeSubtreeListMatch(node.typeParameters(), o.typeParameters())
								// n.b. compare return type even for constructors
								&& safeSubtreeMatch(node.getReturnType2(), o.getReturnType2())
						: node.getModifiers() == o.getModifiers()
								// n.b. compare return type even for constructors
								&& safeSubtreeMatch(node.internalGetReturnType(), o.internalGetReturnType()))
				&& safeSubtreeMatch(node.getName(), o.getName())
				&& (level >= AST.JLS8_INTERNAL
						? safeSubtreeMatch(node.getReceiverType(), o.getReceiverType())
								&& safeSubtreeMatch(node.getReceiverQualifier(), o.getReceiverQualifier())
						: true)
				&& safeSubtreeListMatch(node.parameters(), o.parameters())
				&& (level >= AST.JLS8_INTERNAL
						? safeSubtreeListMatch(node.extraDimensions(), o.extraDimensions())
								&& safeSubtreeListMatch(node.thrownExceptionTypes(), o.thrownExceptionTypes())
						: node.getExtraDimensions() == o.getExtraDimensions()
								&& safeSubtreeListMatch(node.internalThrownExceptions(), o.internalThrownExceptions()))
				&& safeSubtreeMatch(node.getBody(), o.getBody())
				&& (DOMASTUtil.isRecordDeclarationSupported(node.getAST())
						? node.isCompactConstructor() == o.isCompactConstructor()
						: true);
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(MethodInvocation node, Object other) {
		if (!(other instanceof MethodInvocation)) {
			return false;
		}
		MethodInvocation o = (MethodInvocation) other;
		if (node.getAST().apiLevel >= AST.JLS3_INTERNAL) {
			if (!safeSubtreeListMatch(node.typeArguments(), o.typeArguments())) {
				return false;
			}
		}
		return (
			safeSubtreeMatch(node.getExpression(), o.getExpression())
				&& safeSubtreeMatch(node.getName(), o.getName())
				&& safeSubtreeListMatch(node.arguments(), o.arguments()));
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 * @since 3.1
	 */
	public boolean match(Modifier node, Object other) {
		if (!(other instanceof Modifier)) {
			return false;
		}
		Modifier o = (Modifier) other;
		return (node.getKeyword() == o.getKeyword());
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 * @since 3.14
	 */
	public boolean match(ModuleDeclaration node, Object other) {
		if (!(other instanceof ModuleDeclaration)) {
			return false;
		}
		ModuleDeclaration o = (ModuleDeclaration) other;
		return (safeSubtreeMatch(node.getJavadoc(), o.getJavadoc())
				&& safeSubtreeListMatch(node.annotations(), o.annotations())
				&& node.isOpen() == o.isOpen()
				&& safeSubtreeMatch(node.getName(), o.getName())
				&& safeSubtreeListMatch(node.moduleStatements(), o.moduleStatements()));
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 * @since 3.14
	 */
	public boolean match(ModuleModifier node, Object other) {
		if (!(other instanceof ModuleModifier)) {
			return false;
		}
		ModuleModifier o = (ModuleModifier) other;
		return (node.getKeyword() == o.getKeyword());
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 * @since 3.10
	 */
	public boolean match(NameQualifiedType node, Object other) {
		if (!(other instanceof NameQualifiedType)) {
			return false;
		}
		NameQualifiedType o = (NameQualifiedType) other;
		return safeSubtreeMatch(node.getQualifier(), o.getQualifier())
				&& safeSubtreeListMatch(node.annotations(), o.annotations())
				&& safeSubtreeMatch(node.getName(), o.getName());
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 * @since 3.1
	 */
	public boolean match(NormalAnnotation node, Object other) {
		if (!(other instanceof NormalAnnotation)) {
			return false;
		}
		NormalAnnotation o = (NormalAnnotation) other;
		return (safeSubtreeMatch(node.getTypeName(), o.getTypeName())
					&& safeSubtreeListMatch(node.values(), o.values()));
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(NullLiteral node, Object other) {
		if (!(other instanceof NullLiteral)) {
			return false;
		}
		return true;
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 *  @since 3.28
	 */
	public boolean match(NullPattern node, Object other) {
		if (!(other instanceof NullPattern)) {
			return false;
		}
		return true;
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(NumberLiteral node, Object other) {
		if (!(other instanceof NumberLiteral)) {
			return false;
		}
		NumberLiteral o = (NumberLiteral) other;
		return safeEquals(node.getToken(), o.getToken());
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 * @since 3.14
	 */
	public boolean match(OpensDirective node, Object other) {
		if (!(other instanceof OpensDirective)) {
			return false;
		}
		OpensDirective o = (OpensDirective) other;
		return (
			safeSubtreeMatch(node.getName(), o.getName())
				&& safeSubtreeListMatch(node.modules(), o.modules()));
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(PackageDeclaration node, Object other) {
		if (!(other instanceof PackageDeclaration)) {
			return false;
		}
		PackageDeclaration o = (PackageDeclaration) other;
		if (node.getAST().apiLevel >= AST.JLS3_INTERNAL) {
			if (!safeSubtreeMatch(node.getJavadoc(), o.getJavadoc())) {
				return false;
			}
			if (!safeSubtreeListMatch(node.annotations(), o.annotations())) {
				return false;
			}
		}
		return safeSubtreeMatch(node.getName(), o.getName());
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 * @since 3.1
	 */
	public boolean match(ParameterizedType node, Object other) {
		if (!(other instanceof ParameterizedType)) {
			return false;
		}
		ParameterizedType o = (ParameterizedType) other;
		return safeSubtreeMatch(node.getType(), o.getType())
				&& safeSubtreeListMatch(node.typeArguments(), o.typeArguments());
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(ParenthesizedExpression node, Object other) {
		if (!(other instanceof ParenthesizedExpression)) {
			return false;
		}
		ParenthesizedExpression o = (ParenthesizedExpression) other;
		return safeSubtreeMatch(node.getExpression(), o.getExpression());
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 * @since 3.26
	 */
	public boolean match(PatternInstanceofExpression node, Object other) {
		if (!(other instanceof PatternInstanceofExpression)) {
			return false;
		}
		PatternInstanceofExpression o = (PatternInstanceofExpression) other;
		return
			safeSubtreeMatch(node.getLeftOperand(), o.getLeftOperand())
			&& safeSubtreeMatch(node.getRightOperand(), o.getRightOperand());
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(PostfixExpression node, Object other) {
		if (!(other instanceof PostfixExpression)) {
			return false;
		}
		PostfixExpression o = (PostfixExpression) other;
		return (
			node.getOperator().equals(o.getOperator())
				&& safeSubtreeMatch(node.getOperand(), o.getOperand()));
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(PrefixExpression node, Object other) {
		if (!(other instanceof PrefixExpression)) {
			return false;
		}
		PrefixExpression o = (PrefixExpression) other;
		return (
			node.getOperator().equals(o.getOperator())
				&& safeSubtreeMatch(node.getOperand(), o.getOperand()));
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(PrimitiveType node, Object other) {
		if (!(other instanceof PrimitiveType)) {
			return false;
		}
		PrimitiveType o = (PrimitiveType) other;
		int level = node.getAST().apiLevel;
		return (level >= AST.JLS8_INTERNAL ? safeSubtreeListMatch(node.annotations(), o.annotations()) : true)
				&& node.getPrimitiveTypeCode() == o.getPrimitiveTypeCode();
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 * @since 3.14

	 */
	public boolean match(ProvidesDirective node, Object other) {
		if (!(other instanceof ProvidesDirective)) {
			return false;
		}
		ProvidesDirective o = (ProvidesDirective) other;
		return (
				safeSubtreeMatch(node.getName(), o.getName())
				&& safeSubtreeListMatch(node.implementations(), o.implementations()));
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(QualifiedName node, Object other) {
		if (!(other instanceof QualifiedName)) {
			return false;
		}
		QualifiedName o = (QualifiedName) other;
		return safeSubtreeMatch(node.getQualifier(), o.getQualifier())
				&& safeSubtreeMatch(node.getName(), o.getName());
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 * @noreference
	 */
	public boolean match(ModuleQualifiedName node, Object other) {
		if (!(other instanceof ModuleQualifiedName)) {
			return false;
		}
		ModuleQualifiedName o = (ModuleQualifiedName) other;
		return safeSubtreeMatch(node.getModuleQualifier(), o.getModuleQualifier())
				&& safeSubtreeMatch(node.getName(), o.getName());
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 * @since 3.1
	 */
	public boolean match(QualifiedType node, Object other) {
		if (!(other instanceof QualifiedType)) {
			return false;
		}
		QualifiedType o = (QualifiedType) other;
		int level = node.getAST().apiLevel;
		return safeSubtreeMatch(node.getQualifier(), o.getQualifier())
				&& (level >= AST.JLS8_INTERNAL ? safeSubtreeListMatch(node.annotations(), o.annotations()) : true)
				&& safeSubtreeMatch(node.getName(), o.getName());
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 * @since 3.22
	 */
	public boolean match(RecordDeclaration node, Object other) {
		if (!(other instanceof RecordDeclaration)) {
			return false;
		}
		RecordDeclaration o = (RecordDeclaration) other;
		return (
			safeSubtreeMatch(node.getJavadoc(), o.getJavadoc())
				&& safeSubtreeListMatch(node.modifiers(), o.modifiers())
				&& safeSubtreeMatch(node.getName(), o.getName())
				&& safeSubtreeListMatch(node.superInterfaceTypes(), o.superInterfaceTypes())
				&& safeSubtreeMatch(node.typeParameters(), o.typeParameters())
				&& safeSubtreeListMatch(node.bodyDeclarations(), o.bodyDeclarations())
				&& safeSubtreeMatch(node.recordComponents(), o.recordComponents()));
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 *
	 *   @since 3.14
	 */
	public boolean match(RequiresDirective node, Object other) {
		if (!(other instanceof RequiresDirective)) {
			return false;
		}
		RequiresDirective o = (RequiresDirective) other;
		return safeSubtreeListMatch(node.modifiers(), o.modifiers())
				&& safeSubtreeMatch(node.getName(), o.getName());
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(ReturnStatement node, Object other) {
		if (!(other instanceof ReturnStatement)) {
			return false;
		}
		ReturnStatement o = (ReturnStatement) other;
		return safeSubtreeMatch(node.getExpression(), o.getExpression());
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(SimpleName node, Object other) {
		if (!(other instanceof SimpleName)) {
			return false;
		}
		SimpleName o = (SimpleName) other;
		return node.getIdentifier().equals(o.getIdentifier());
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(SimpleType node, Object other) {
		if (!(other instanceof SimpleType)) {
			return false;
		}
		SimpleType o = (SimpleType) other;
		int level = node.getAST().apiLevel;
		return (level >= AST.JLS8_INTERNAL ? safeSubtreeListMatch(node.annotations(), o.annotations()) : true)
				&& safeSubtreeMatch(node.getName(), o.getName());
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 * @since 3.1
	 */
	public boolean match(SingleMemberAnnotation node, Object other) {
		if (!(other instanceof SingleMemberAnnotation)) {
			return false;
		}
		SingleMemberAnnotation o = (SingleMemberAnnotation) other;
		return (safeSubtreeMatch(node.getTypeName(), o.getTypeName())
				&& safeSubtreeMatch(node.getValue(), o.getValue()));
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 * <p>
	 * Note that extra array dimensions and the variable arity flag
	 * are compared since they are both important parts of the declaration.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(SingleVariableDeclaration node, Object other) {
		if (!(other instanceof SingleVariableDeclaration)) {
			return false;
		}
		SingleVariableDeclaration o = (SingleVariableDeclaration) other;
		int level = node.getAST().apiLevel;
		return (level >= AST.JLS3_INTERNAL
						? safeSubtreeListMatch(node.modifiers(), o.modifiers())
						: node.getModifiers() == o.getModifiers())
				&& safeSubtreeMatch(node.getType(), o.getType())
				&& (level >= AST.JLS8_INTERNAL && node.isVarargs()
						? safeSubtreeListMatch(node.varargsAnnotations(), o.varargsAnnotations())
						: true)
				&& (level >= AST.JLS3_INTERNAL
						? node.isVarargs() == o.isVarargs()
						: true)
				&& safeSubtreeMatch(node.getName(), o.getName())
				&& ((level >= AST.JLS8_INTERNAL)
						? safeSubtreeListMatch(node.extraDimensions(), o.extraDimensions())
						: node.getExtraDimensions() == o.getExtraDimensions())
				&& safeSubtreeMatch(node.getInitializer(), o.getInitializer());
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(StringLiteral node, Object other) {
		if (!(other instanceof StringLiteral)) {
			return false;
		}
		StringLiteral o = (StringLiteral) other;
		return safeEquals(node.getEscapedValue(), o.getEscapedValue());
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(SuperConstructorInvocation node, Object other) {
		if (!(other instanceof SuperConstructorInvocation)) {
			return false;
		}
		SuperConstructorInvocation o = (SuperConstructorInvocation) other;
		if (node.getAST().apiLevel >= AST.JLS3_INTERNAL) {
			if (!safeSubtreeListMatch(node.typeArguments(), o.typeArguments())) {
				return false;
			}
		}
		return (
			safeSubtreeMatch(node.getExpression(), o.getExpression())
				&& safeSubtreeListMatch(node.arguments(), o.arguments()));
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(SuperFieldAccess node, Object other) {
		if (!(other instanceof SuperFieldAccess)) {
			return false;
		}
		SuperFieldAccess o = (SuperFieldAccess) other;
		return (
			safeSubtreeMatch(node.getName(), o.getName())
				&& safeSubtreeMatch(node.getQualifier(), o.getQualifier()));
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(SuperMethodInvocation node, Object other) {
		if (!(other instanceof SuperMethodInvocation)) {
			return false;
		}
		SuperMethodInvocation o = (SuperMethodInvocation) other;
		if (node.getAST().apiLevel >= AST.JLS3_INTERNAL) {
			if (!safeSubtreeListMatch(node.typeArguments(), o.typeArguments())) {
				return false;
			}
		}
		return (
			safeSubtreeMatch(node.getQualifier(), o.getQualifier())
				&& safeSubtreeMatch(node.getName(), o.getName())
				&& safeSubtreeListMatch(node.arguments(), o.arguments()));
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 *
	 *   @since 3.10
	 */
	public boolean match(SuperMethodReference node, Object other) {
		if (!(other instanceof SuperMethodReference)) {
			return false;
		}
		SuperMethodReference o = (SuperMethodReference) other;
		return (safeSubtreeMatch(node.getQualifier(), o.getQualifier())
				&& safeSubtreeListMatch(node.typeArguments(), o.typeArguments())
				&& safeSubtreeMatch(node.getName(), o.getName()));
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(SwitchCase node, Object other) {
		if (!(other instanceof SwitchCase)) {
			return false;
		}
		SwitchCase o = (SwitchCase) other;
		return ( node.getAST().apiLevel >= AST.JLS14_INTERNAL
				? safeSubtreeListMatch(node.expressions(), o.expressions())
						: compareDeprecatedSwitchExpression(node, o));
	}

	/**
	 * Return whether the deprecated comment strings of the given java doc are equals.
	 * <p>
	 * Note the only purpose of this method is to hide deprecated warnings.
	 * @deprecated mark deprecated to hide deprecated usage
	 */
	private boolean compareDeprecatedSwitchExpression(SwitchCase first, SwitchCase second) {
		return safeSubtreeMatch(first.getExpression(), second.getExpression());
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 * @since 3.18
	 */
	public boolean match(SwitchExpression node, Object other) {
		if (!(other instanceof SwitchExpression)) {
			return false;
		}
		SwitchExpression o = (SwitchExpression) other;
		return	safeSubtreeMatch(node.getExpression(), o.getExpression())
				&& safeSubtreeListMatch(node.statements(), o.statements());
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(SwitchStatement node, Object other) {
		if (!(other instanceof SwitchStatement)) {
			return false;
		}
		SwitchStatement o = (SwitchStatement) other;
		return (
			safeSubtreeMatch(node.getExpression(), o.getExpression())
				&& safeSubtreeListMatch(node.statements(), o.statements()));
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(SynchronizedStatement node, Object other) {
		if (!(other instanceof SynchronizedStatement)) {
			return false;
		}
		SynchronizedStatement o = (SynchronizedStatement) other;
		return (
			safeSubtreeMatch(node.getExpression(), o.getExpression())
				&& safeSubtreeMatch(node.getBody(), o.getBody()));
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 * @since 3.0
	 */
	public boolean match(TagElement node, Object other) {
		if (!(other instanceof TagElement)) {
			return false;
		}
		TagElement o = (TagElement) other;
		return (
				safeEquals(node.getTagName(), o.getTagName())
				&& safeSubtreeListMatch(node.fragments(), o.fragments()));
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 * @noreference This method is not intended to be referenced by clients as it is a part of Java preview feature.
	 * @nooverride This method is not intended to be re-implemented or extended by clients as it is a part of Java preview feature.
	 * @since 3.20
	 */
	public boolean match(TextBlock node, Object other) {
		if (!(other instanceof TextBlock)) {
			return false;
		}
		TextBlock o = (TextBlock) other;
		return safeEquals(node.getEscapedValue(), o.getEscapedValue());
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 * @since 3.0
	 */
	public boolean match(TextElement node, Object other) {
		if (!(other instanceof TextElement)) {
			return false;
		}
		TextElement o = (TextElement) other;
		return safeEquals(node.getText(), o.getText());
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(ThisExpression node, Object other) {
		if (!(other instanceof ThisExpression)) {
			return false;
		}
		ThisExpression o = (ThisExpression) other;
		return safeSubtreeMatch(node.getQualifier(), o.getQualifier());
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(ThrowStatement node, Object other) {
		if (!(other instanceof ThrowStatement)) {
			return false;
		}
		ThrowStatement o = (ThrowStatement) other;
		return safeSubtreeMatch(node.getExpression(), o.getExpression());
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(TryStatement node, Object other) {
		if (!(other instanceof TryStatement)) {
			return false;
		}
		TryStatement o = (TryStatement) other;
		int level = node.getAST().apiLevel;
		return (level >= AST.JLS4_INTERNAL ? safeSubtreeListMatch(node.resources(), o.resources()) : true)
				&& safeSubtreeMatch(node.getBody(), o.getBody())
				&& safeSubtreeListMatch(node.catchClauses(), o.catchClauses())
				&& safeSubtreeMatch(node.getFinally(), o.getFinally());
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(TypeDeclaration node, Object other) {
		if (!(other instanceof TypeDeclaration)) {
			return false;
		}
		TypeDeclaration o = (TypeDeclaration) other;
		int level = node.getAST().apiLevel;
		if (level == AST.JLS2_INTERNAL) {
			if (node.getModifiers() != o.getModifiers()) {
				return false;
			}
			if (!safeSubtreeMatch(node.internalGetSuperclass(), o.internalGetSuperclass())) {
				return false;
			}
			if (!safeSubtreeListMatch(node.internalSuperInterfaces(), o.internalSuperInterfaces())) {
				return false;
			}
		}
		if (level >= AST.JLS3_INTERNAL) {
			if (!safeSubtreeListMatch(node.modifiers(), o.modifiers())) {
				return false;
			}
			if (!safeSubtreeListMatch(node.typeParameters(), o.typeParameters())) {
				return false;
			}
			if (!safeSubtreeMatch(node.getSuperclassType(), o.getSuperclassType())) {
				return false;
			}
			if (!safeSubtreeListMatch(node.superInterfaceTypes(), o.superInterfaceTypes())) {
				return false;
			}
		}
		if (DOMASTUtil.isFeatureSupportedinAST(node.getAST(), Modifier.SEALED)) {
			if (!safeSubtreeListMatch(node.permittedTypes(), o.permittedTypes())) {
				return false;
			}
		}
		return (
				(node.isInterface() == o.isInterface())
				&& safeSubtreeMatch(node.getJavadoc(), o.getJavadoc())
				&& safeSubtreeMatch(node.getName(), o.getName())
				&& safeSubtreeListMatch(node.bodyDeclarations(), o.bodyDeclarations()));
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(TypeDeclarationStatement node, Object other) {
		if (!(other instanceof TypeDeclarationStatement)) {
			return false;
		}
		TypeDeclarationStatement o = (TypeDeclarationStatement) other;
		return safeSubtreeMatch(node.getDeclaration(), o.getDeclaration());
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(TypeLiteral node, Object other) {
		if (!(other instanceof TypeLiteral)) {
			return false;
		}
		TypeLiteral o = (TypeLiteral) other;
		return safeSubtreeMatch(node.getType(), o.getType());
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 * @since 3.10
	 */
	public boolean match(TypeMethodReference node, Object other) {
		if (!(other instanceof TypeMethodReference)) {
			return false;
		}
		TypeMethodReference o = (TypeMethodReference) other;
		return (
			safeSubtreeMatch(node.getType(), o.getType())
				&& safeSubtreeListMatch(node.typeArguments(), o.typeArguments())
				&& safeSubtreeMatch(node.getName(), o.getName()));
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 * @since 3.1
	 */
	public boolean match(TypeParameter node, Object other) {
		if (!(other instanceof TypeParameter)) {
			return false;
		}
		TypeParameter o = (TypeParameter) other;
		int level = node.getAST().apiLevel;
		return (level >= AST.JLS8_INTERNAL ? safeSubtreeListMatch(node.modifiers(), o.modifiers()) : true)
				&& safeSubtreeMatch(node.getName(), o.getName())
				&& safeSubtreeListMatch(node.typeBounds(), o.typeBounds());
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 * @since 3.28
	 */
	public boolean match(TypePattern node, Object other) {
		if (!(other instanceof TypePattern)) {
			return false;
		}
		TypePattern o = (TypePattern) other;
		return safeSubtreeMatch(node.getPatternVariable(), o.getPatternVariable());
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 * @since 3.7.1
	 */
	public boolean match(UnionType node, Object other) {
		if (!(other instanceof UnionType)) {
			return false;
		}
		UnionType o = (UnionType) other;
		return safeSubtreeListMatch(node.types(),	o.types());
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 * @since 3.14
	 */
	public boolean match(UsesDirective node, Object other) {
		if (!(other instanceof UsesDirective)) {
			return false;
		}
		UsesDirective o = (UsesDirective) other;
		return safeSubtreeMatch(node.getName(), o.getName());
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(VariableDeclarationExpression node, Object other) {
		if (!(other instanceof VariableDeclarationExpression)) {
			return false;
		}
		VariableDeclarationExpression o = (VariableDeclarationExpression) other;
		int level = node.getAST().apiLevel;
		if (level == AST.JLS2_INTERNAL) {
			if (node.getModifiers() != o.getModifiers()) {
				return false;
			}
		}
		if (level >= AST.JLS3_INTERNAL) {
			if (!safeSubtreeListMatch(node.modifiers(), o.modifiers())) {
				return false;
			}
		}
		return safeSubtreeMatch(node.getType(), o.getType())
			&& safeSubtreeListMatch(node.fragments(), o.fragments());
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 * <p>
	 * Note that extra array dimensions are compared since they are an
	 * important part of the type of the variable.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(VariableDeclarationFragment node, Object other) {
		if (!(other instanceof VariableDeclarationFragment)) {
			return false;
		}
		VariableDeclarationFragment o = (VariableDeclarationFragment) other;
		int level = node.getAST().apiLevel;
		return safeSubtreeMatch(node.getName(), o.getName())
				&& (level >= AST.JLS8_INTERNAL
						? safeSubtreeListMatch(node.extraDimensions(), o.extraDimensions())
						: node.getExtraDimensions() == o.getExtraDimensions())
				&& safeSubtreeMatch(node.getInitializer(), o.getInitializer());
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(VariableDeclarationStatement node, Object other) {
		if (!(other instanceof VariableDeclarationStatement)) {
			return false;
		}
		VariableDeclarationStatement o = (VariableDeclarationStatement) other;
		int level = node.getAST().apiLevel;
		if (level == AST.JLS2_INTERNAL) {
			if (node.getModifiers() != o.getModifiers()) {
				return false;
			}
		}
		if (level >= AST.JLS3_INTERNAL) {
			if (!safeSubtreeListMatch(node.modifiers(), o.modifiers())) {
				return false;
			}
		}
		return safeSubtreeMatch(node.getType(), o.getType())
			&& safeSubtreeListMatch(node.fragments(), o.fragments());
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(WhileStatement node, Object other) {
		if (!(other instanceof WhileStatement)) {
			return false;
		}
		WhileStatement o = (WhileStatement) other;
		return (
			safeSubtreeMatch(node.getExpression(), o.getExpression())
				&& safeSubtreeMatch(node.getBody(), o.getBody()));
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 * @since 3.1
	 */
	public boolean match(WildcardType node, Object other) {
		if (!(other instanceof WildcardType)) {
			return false;
		}
		WildcardType o = (WildcardType) other;
		int level = node.getAST().apiLevel;
		return (level >= AST.JLS8_INTERNAL ? safeSubtreeListMatch(node.annotations(), o.annotations()) : true)
				&& node.isUpperBound() == o.isUpperBound()
				&& safeSubtreeMatch(node.getBound(), o.getBound());
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 * @noreference This method is not intended to be referenced by clients as it is a part of Java preview feature.
	 * @nooverride This method is not intended to be re-implemented or extended by clients as it is a part of Java preview feature.
	 * @since 3.20
	 */
	public boolean match(YieldStatement node, Object other) {
		if (!(other instanceof YieldStatement)) {
			return false;
		}
		YieldStatement o = (YieldStatement) other;
		return	safeSubtreeMatch(node.getExpression(), o.getExpression());
	}

}
