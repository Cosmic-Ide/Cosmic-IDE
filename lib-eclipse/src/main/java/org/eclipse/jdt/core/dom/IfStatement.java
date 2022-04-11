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
 * If statement AST node type.
 * <pre>
 * IfStatement:
 *    <b>if</b> <b>(</b> Expression <b>)</b> Statement [ <b>else</b> Statement]
 * </pre>
 *
 * @since 2.0
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
@SuppressWarnings("rawtypes")
public class IfStatement extends Statement {

	/**
	 * The "expression" structural property of this node type (child type: {@link Expression}).
	 * @since 3.0
	 */
	public static final ChildPropertyDescriptor EXPRESSION_PROPERTY =
		new ChildPropertyDescriptor(IfStatement.class, "expression", Expression.class, MANDATORY, CYCLE_RISK); //$NON-NLS-1$

	/**
	 * The "thenStatement" structural property of this node type (child type: {@link Statement}).
	 * @since 3.0
	 */
	public static final ChildPropertyDescriptor THEN_STATEMENT_PROPERTY =
		new ChildPropertyDescriptor(IfStatement.class, "thenStatement", Statement.class, MANDATORY, CYCLE_RISK); //$NON-NLS-1$

	/**
	 * The "elseStatement" structural property of this node type (child type: {@link Statement}).
	 * @since 3.0
	 */
	public static final ChildPropertyDescriptor ELSE_STATEMENT_PROPERTY =
		new ChildPropertyDescriptor(IfStatement.class, "elseStatement", Statement.class, OPTIONAL, CYCLE_RISK); //$NON-NLS-1$

	/**
	 * A list of property descriptors (element type:
	 * {@link StructuralPropertyDescriptor}),
	 * or null if uninitialized.
	 */
	private static final List PROPERTY_DESCRIPTORS;

	static {
		List properyList = new ArrayList(4);
		createPropertyList(IfStatement.class, properyList);
		addProperty(EXPRESSION_PROPERTY, properyList);
		addProperty(THEN_STATEMENT_PROPERTY, properyList);
		addProperty(ELSE_STATEMENT_PROPERTY, properyList);
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
	 * The expression; lazily initialized; defaults to an unspecified, but
	 * legal, expression.
	 */
	private Expression expression = null;

	/**
	 * The then statement; lazily initialized; defaults to an unspecified, but
	 * legal, statement.
	 */
	private Statement thenStatement = null;

	/**
	 * The else statement; <code>null</code> for none; defaults to none.
	 */
	private Statement optionalElseStatement = null;

	/**
	 * Creates a new unparented if statement node owned by the given
	 * AST. By default, the expresssion is unspecified,
	 * but legal, the then statement is an empty block, and there is no else
	 * statement.
	 * <p>
	 * N.B. This constructor is package-private.
	 * </p>
	 *
	 * @param ast the AST that is to own this node
	 */
	IfStatement(AST ast) {
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
		if (property == THEN_STATEMENT_PROPERTY) {
			if (get) {
				return getThenStatement();
			} else {
				setThenStatement((Statement) child);
				return null;
			}
		}
		if (property == ELSE_STATEMENT_PROPERTY) {
			if (get) {
				return getElseStatement();
			} else {
				setElseStatement((Statement) child);
				return null;
			}
		}
		// allow default implementation to flag the error
		return super.internalGetSetChildProperty(property, get, child);
	}

	@Override
	final int getNodeType0() {
		return IF_STATEMENT;
	}

	@Override
	ASTNode clone0(AST target) {
		IfStatement result = new IfStatement(target);
		result.setSourceRange(getStartPosition(), getLength());
		result.copyLeadingComment(this);
		result.setExpression((Expression) getExpression().clone(target));
		result.setThenStatement(
			(Statement) getThenStatement().clone(target));
		result.setElseStatement(
			(Statement) ASTNode.copySubtree(target, getElseStatement()));
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
			acceptChild(visitor, getThenStatement());
			acceptChild(visitor, getElseStatement());
		}
		visitor.endVisit(this);
	}

	/**
	 * Returns the expression of this if statement.
	 *
	 * @return the expression node
	 */
	public Expression getExpression() {
		if (this.expression == null) {
			// lazy init must be thread-safe for readers
			synchronized (this) {
				if (this.expression == null) {
					preLazyInit();
					this.expression = new SimpleName(this.ast);
					postLazyInit(this.expression, EXPRESSION_PROPERTY);
				}
			}
		}
		return this.expression;
	}

	/**
	 * Sets the condition of this if statement.
	 *
	 * @param expression the expression node
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
		ASTNode oldChild = this.expression;
		preReplaceChild(oldChild, expression, EXPRESSION_PROPERTY);
		this.expression = expression;
		postReplaceChild(oldChild, expression, EXPRESSION_PROPERTY);
	}

	/**
	 * Returns the "then" part of this if statement.
	 *
	 * @return the "then" statement node
	 */
	public Statement getThenStatement() {
		if (this.thenStatement == null) {
			// lazy init must be thread-safe for readers
			synchronized (this) {
				if (this.thenStatement == null) {
					preLazyInit();
					this.thenStatement = new Block(this.ast);
					postLazyInit(this.thenStatement, THEN_STATEMENT_PROPERTY);
				}
			}
		}
		return this.thenStatement;
	}

	/**
	 * Sets the "then" part of this if statement.
	 * <p>
	 * Special note: The Java language does not allow a local variable declaration
	 * to appear as the "then" part of an if statement (they may only appear within a
	 * block). However, the AST will allow a <code>VariableDeclarationStatement</code>
	 * as the thenStatement of a <code>IfStatement</code>. To get something that will
	 * compile, be sure to embed the <code>VariableDeclarationStatement</code>
	 * inside a <code>Block</code>.
	 * </p>
	 *
	 * @param statement the "then" statement node
	 * @exception IllegalArgumentException if:
	 * <ul>
	 * <li>the node belongs to a different AST</li>
	 * <li>the node already has a parent</li>
	 * <li>a cycle in would be created</li>
	 * </ul>
	 */
	public void setThenStatement(Statement statement) {
		if (statement == null) {
			throw new IllegalArgumentException();
		}
		ASTNode oldChild = this.thenStatement;
		preReplaceChild(oldChild, statement, THEN_STATEMENT_PROPERTY);
		this.thenStatement = statement;
		postReplaceChild(oldChild, statement, THEN_STATEMENT_PROPERTY);
	}

	/**
	 * Returns the "else" part of this if statement, or <code>null</code> if
	 * this if statement has <b>no</b> "else" part.
	 * <p>
	 * Note that there is a subtle difference between having no else
	 * statement and having an empty statement ("{}") or null statement (";").
	 * </p>
	 *
	 * @return the "else" statement node, or <code>null</code> if none
	 */
	public Statement getElseStatement() {
		return this.optionalElseStatement;
	}

	/**
	 * Sets or clears the "else" part of this if statement.
	 * <p>
	 * Note that there is a subtle difference between having no else part
	 * (as in <code>"if(true){}"</code>) and having an empty block (as in
	 * "if(true){}else{}") or null statement (as in "if(true){}else;").
	 * </p>
	 * <p>
	 * Special note: The Java language does not allow a local variable declaration
	 * to appear as the "else" part of an if statement (they may only appear within a
	 * block). However, the AST will allow a <code>VariableDeclarationStatement</code>
	 * as the elseStatement of a <code>IfStatement</code>. To get something that will
	 * compile, be sure to embed the <code>VariableDeclarationStatement</code>
	 * inside a <code>Block</code>.
	 * </p>
	 *
	 * @param statement the "else" statement node, or <code>null</code> if
	 *    there is none
	 * @exception IllegalArgumentException if:
	 * <ul>
	 * <li>the node belongs to a different AST</li>
	 * <li>the node already has a parent</li>
	 * <li>a cycle in would be created</li>
	 * </ul>
	 */
	public void setElseStatement(Statement statement) {
		ASTNode oldChild = this.optionalElseStatement;
		preReplaceChild(oldChild, statement, ELSE_STATEMENT_PROPERTY);
		this.optionalElseStatement = statement;
		postReplaceChild(oldChild, statement, ELSE_STATEMENT_PROPERTY);
	}

	@Override
	int memSize() {
		return super.memSize() + 3 * 4;
	}

	@Override
	int treeSize() {
		return
			memSize()
			+ (this.expression == null ? 0 : getExpression().treeSize())
			+ (this.thenStatement == null ? 0 : getThenStatement().treeSize())
			+ (this.optionalElseStatement == null ? 0 : getElseStatement().treeSize());
	}
}

