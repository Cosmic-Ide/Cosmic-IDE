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

package org.eclipse.jdt.core.dom;

import java.util.ArrayList;
import java.util.List;

/**
 * Array initializer AST node type.
 *
 * <pre>
 * ArrayInitializer:
 * 		<b>{</b> [ Expression { <b>,</b> Expression} [ <b>,</b> ]] <b>}</b>
 * </pre>
 *
 * @since 2.0
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class ArrayInitializer extends Expression {

	/**
	 * The "expressions" structural property of this node type (element type: {@link Expression}).
	 * @since 3.0
	 */
	public static final ChildListPropertyDescriptor EXPRESSIONS_PROPERTY =
		new ChildListPropertyDescriptor(ArrayInitializer.class, "expressions", Expression.class, CYCLE_RISK); //$NON-NLS-1$

	/**
	 * A list of property descriptors (element type:
	 * {@link StructuralPropertyDescriptor}),
	 * or null if uninitialized.
	 */
	private static final List PROPERTY_DESCRIPTORS;

	static {
		List properyList = new ArrayList(2);
		createPropertyList(ArrayInitializer.class, properyList);
		addProperty(EXPRESSIONS_PROPERTY, properyList);
		PROPERTY_DESCRIPTORS = reapPropertyList(properyList);
	}

	/**
	 * Returns a list of structural property descriptors for this node type.
	 * Clients must not modify the result.
	 *
	 * @param apiLevel the API level; one of the
	 * <code>AST.JLS*</code> constants

	 * @return a list of property descriptors (element type:
	 * {@link StructuralPropertyDescriptor})
	 * @since 3.0
	 */
	public static List propertyDescriptors(int apiLevel) {
		return PROPERTY_DESCRIPTORS;
	}

	/**
	 * The list of expressions (element type:
	 * {@link Expression}). Defaults to an empty list.
	 */
	private ASTNode.NodeList expressions =
		new ASTNode.NodeList(EXPRESSIONS_PROPERTY);

	/**
	 * Creates a new AST node for an array initializer owned by the
	 * given AST. By default, the list of expressions is empty.
	 *
	 * @param ast the AST that is to own this node
	 */
	ArrayInitializer(AST ast) {
		super(ast);
	}

	@Override
	final List internalStructuralPropertiesForType(int apiLevel) {
		return propertyDescriptors(apiLevel);
	}

	@Override
	final List internalGetChildListProperty(ChildListPropertyDescriptor property) {
		if (property == EXPRESSIONS_PROPERTY) {
			return expressions();
		}
		// allow default implementation to flag the error
		return super.internalGetChildListProperty(property);
	}

	@Override
	final int getNodeType0() {
		return ARRAY_INITIALIZER;
	}

	@Override
	ASTNode clone0(AST target) {
		ArrayInitializer result = new ArrayInitializer(target);
		result.setSourceRange(getStartPosition(), getLength());
		result.expressions().addAll(ASTNode.copySubtrees(target, expressions()));
		return result;
	}

	@Override
	final boolean subtreeMatch0(ASTMatcher matcher, Object other) {
		// dispatch to correct overloaded match method
		return matcher.match(this, other);
	}

	@Override
	void accept0(ASTVisitor visitor) {
		boolean visitChildren = visitor.visit(this);
		if (visitChildren) {
			acceptChildren(visitor, this.expressions);
		}
		visitor.endVisit(this);
	}

	/**
	 * Returns the live ordered list of expressions in this array initializer.
	 *
	 * @return the live list of expressions
	 *    (element type: {@link Expression})
	 */
	public List expressions() {
		return this.expressions;
	}

	@Override
	int memSize() {
		return BASE_NODE_SIZE + 1 * 4;
	}

	@Override
	int treeSize() {
		return memSize() + this.expressions.listSize();
	}
}

