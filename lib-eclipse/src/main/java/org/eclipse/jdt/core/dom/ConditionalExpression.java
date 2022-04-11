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
 * Conditional expression AST node type.
 *
 * <pre>
 * ConditionalExpression:
 *    Expression <b>?</b> Expression <b>:</b> Expression
 * </pre>
 *
 * @since 2.0
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
@SuppressWarnings("rawtypes")
public class ConditionalExpression extends Expression {

	/**
	 * The "expression" structural property of this node type (child type: {@link Expression}).
	 * @since 3.0
	 */
	public static final ChildPropertyDescriptor EXPRESSION_PROPERTY =
		new ChildPropertyDescriptor(ConditionalExpression.class, "expression", Expression.class, MANDATORY, CYCLE_RISK); //$NON-NLS-1$

	/**
	 * The "thenExpression" structural property of this node type (child type: {@link Expression}).
	 * @since 3.0
	 */
	public static final ChildPropertyDescriptor THEN_EXPRESSION_PROPERTY =
		new ChildPropertyDescriptor(ConditionalExpression.class, "thenExpression", Expression.class, MANDATORY, CYCLE_RISK); //$NON-NLS-1$

	/**
	 * The "elseExpression" structural property of this node type (child type: {@link Expression}).
	 * @since 3.0
	 */
	public static final ChildPropertyDescriptor ELSE_EXPRESSION_PROPERTY =
		new ChildPropertyDescriptor(ConditionalExpression.class, "elseExpression", Expression.class, MANDATORY, CYCLE_RISK); //$NON-NLS-1$

	/**
	 * A list of property descriptors (element type:
	 * {@link StructuralPropertyDescriptor}),
	 * or null if uninitialized.
	 */
	private static final List PROPERTY_DESCRIPTORS;

	static {
		List properyList = new ArrayList(4);
		createPropertyList(ConditionalExpression.class, properyList);
		addProperty(EXPRESSION_PROPERTY, properyList);
		addProperty(THEN_EXPRESSION_PROPERTY, properyList);
		addProperty(ELSE_EXPRESSION_PROPERTY, properyList);
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
	 * The condition expression; lazily initialized; defaults to an unspecified,
	 * but legal, expression.
	 */
	private Expression conditionExpression = null;

	/**
	 * The "then" expression; lazily initialized; defaults to an unspecified,
	 * but legal, expression.
	 */
	private Expression thenExpression = null;

	/**
	 * The "else" expression; lazily initialized; defaults to an unspecified,
	 * but legal, expression.
	 */
	private Expression elseExpression = null;

	/**
	 * Creates a new unparented conditional expression node owned by the given
	 * AST. By default, the condition, "then", and "else" expresssions are
	 * unspecified, but legal.
	 * <p>
	 * N.B. This constructor is package-private.
	 * </p>
	 *
	 * @param ast the AST that is to own this node
	 */
	ConditionalExpression(AST ast) {
		super(ast);
	}

	@Override
	final List internalStructuralPropertiesForType(int apiLevel) {
		return propertyDescriptors(apiLevel);
	}

	@Override
	final ASTNode internalGetSetChildProperty(ChildPropertyDescriptor property, boolean get, ASTNode child) {
		if (property == EXPRESSION_PROPERTY) {
			if (get) {
				return getExpression();
			} else {
				setExpression((Expression) child);
				return null;
			}
		}
		if (property == THEN_EXPRESSION_PROPERTY) {
			if (get) {
				return getThenExpression();
			} else {
				setThenExpression((Expression) child);
				return null;
			}
		}
		if (property == ELSE_EXPRESSION_PROPERTY) {
			if (get) {
				return getElseExpression();
			} else {
				setElseExpression((Expression) child);
				return null;
			}
		}
		// allow default implementation to flag the error
		return super.internalGetSetChildProperty(property, get, child);
	}

	@Override
	final int getNodeType0() {
		return CONDITIONAL_EXPRESSION;
	}

	@Override
	ASTNode clone0(AST target) {
		ConditionalExpression result = new ConditionalExpression(target);
		result.setSourceRange(getStartPosition(), getLength());
		result.setExpression((Expression) getExpression().clone(target));
		result.setThenExpression(
			(Expression) getThenExpression().clone(target));
		result.setElseExpression(
			(Expression) getElseExpression().clone(target));
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
			// visit children in normal left to right reading order
			acceptChild(visitor, getExpression());
			acceptChild(visitor, getThenExpression());
			acceptChild(visitor, getElseExpression());
		}
		visitor.endVisit(this);
	}

	/**
	 * Returns the condition of this conditional expression.
	 *
	 * @return the condition node
	 */
	public Expression getExpression() {
		if (this.conditionExpression == null) {
			// lazy init must be thread-safe for readers
			synchronized (this) {
				if (this.conditionExpression == null) {
					preLazyInit();
					this.conditionExpression = new SimpleName(this.ast);
					postLazyInit(this.conditionExpression, EXPRESSION_PROPERTY);
				}
			}
		}
		return this.conditionExpression;
	}

	/**
	 * Sets the condition of this conditional expression.
	 *
	 * @param expression the condition node
	 * @exception IllegalArgumentException if:
	 * <ul>
	 * <li>the node belongs to a different AST</li>
	 * <li>the node already has a parent</li>
	 * <li>a cycle in would be created</li>
	 * </ul>
	 */
	public void setExpression(Expression expression) {
		if (expression == null) {
			throw new IllegalArgumentException();
		}
		ASTNode oldChild = this.conditionExpression;
		preReplaceChild(oldChild, expression, EXPRESSION_PROPERTY);
		this.conditionExpression = expression;
		postReplaceChild(oldChild, expression, EXPRESSION_PROPERTY);
	}

	/**
	 * Returns the "then" part of this conditional expression.
	 *
	 * @return the "then" expression node
	 */
	public Expression getThenExpression() {
		if (this.thenExpression == null) {
			// lazy init must be thread-safe for readers
			synchronized (this) {
				if (this.thenExpression == null) {
					preLazyInit();
					this.thenExpression = new SimpleName(this.ast);
					postLazyInit(this.thenExpression, THEN_EXPRESSION_PROPERTY);
				}
			}
		}
		return this.thenExpression;
	}

	/**
	 * Sets the "then" part of this conditional expression.
	 *
	 * @param expression the "then" expression node
	 * @exception IllegalArgumentException if:
	 * <ul>
	 * <li>the node belongs to a different AST</li>
	 * <li>the node already has a parent</li>
	 * <li>a cycle in would be created</li>
	 * </ul>
	 */
	public void setThenExpression(Expression expression) {
		if (expression == null) {
			throw new IllegalArgumentException();
		}
		ASTNode oldChild = this.thenExpression;
		preReplaceChild(oldChild, expression, THEN_EXPRESSION_PROPERTY);
		this.thenExpression = expression;
		postReplaceChild(oldChild, expression, THEN_EXPRESSION_PROPERTY);
	}

	/**
	 * Returns the "else" part of this conditional expression.
	 *
	 * @return the "else" expression node
	 */
	public Expression getElseExpression() {
		if (this.elseExpression == null) {
			// lazy init must be thread-safe for readers
			synchronized (this) {
				if (this.elseExpression == null) {
					preLazyInit();
					this.elseExpression = new SimpleName(this.ast);
					postLazyInit(this.elseExpression, ELSE_EXPRESSION_PROPERTY);
				}
			}
		}
		return this.elseExpression;
	}

	/**
	 * Sets the "else" part of this conditional expression.
	 *
	 * @param expression the "else" expression node
	 * @exception IllegalArgumentException if:
	 * <ul>
	 * <li>the node belongs to a different AST</li>
	 * <li>the node already has a parent</li>
	 * <li>a cycle in would be created</li>
	 * </ul>
	 */
	public void setElseExpression(Expression expression) {
		if (expression == null) {
			throw new IllegalArgumentException();
		}
		ASTNode oldChild = this.elseExpression;
		preReplaceChild(oldChild, expression, ELSE_EXPRESSION_PROPERTY);
		this.elseExpression = expression;
		postReplaceChild(oldChild, expression, ELSE_EXPRESSION_PROPERTY);
	}

	@Override
	int memSize() {
		// treat Code as free
		return BASE_NODE_SIZE + 3 * 4;
	}

	@Override
	int treeSize() {
		return
			memSize()
			+ (this.conditionExpression == null ? 0 : getExpression().treeSize())
			+ (this.thenExpression == null ? 0 : getThenExpression().treeSize())
			+ (this.elseExpression == null ? 0 : getElseExpression().treeSize());
	}
}
