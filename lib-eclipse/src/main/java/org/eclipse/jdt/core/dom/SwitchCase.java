/*******************************************************************************
 * Copyright (c) 2000, 2021 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.core.dom;

import java.util.ArrayList;
import java.util.List;

/**
 * Switch case AST node type. A switch case is a special kind of node used only
 * in switch statements. It is a <code>Statement</code> in name only.
 * <pre>
 * SwitchCase:
 * 		<b>case</b> Expression  <b>:</b>
 * 		<b>default</b> <b>:</b>
 *
 * Switch case allows multiple expressions and '->' as part of Java 12 preview feature (JEP 325)
 *		<b>case</b> [ Expression { <b>,</b> Expression } ]  <b>{ : | ->}</b>
 *		<b>default</b> <b>{ : | ->}</b>
 * </pre>
 *
 * @since 2.0
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
@SuppressWarnings("rawtypes")
public class SwitchCase extends Statement {

	/**
	 * The "expression" structural property of this node type (child type: {@link Expression}).
	 * @since 3.0
	 * @deprecated In the JLS 12 15.28.1 API, this property is replaced by {@link #EXPRESSIONS2_PROPERTY }.
	 */
	public static final ChildPropertyDescriptor EXPRESSION_PROPERTY =
		new ChildPropertyDescriptor(SwitchCase.class, "expression", Expression.class, OPTIONAL, CYCLE_RISK); //$NON-NLS-1$

	/**
	 * The "expression" structural property of this node type (child type: {@link Expression}). (added in JEP 325).
	 * @since 3.18
	 */
	public static final ChildListPropertyDescriptor EXPRESSIONS2_PROPERTY  =
			new ChildListPropertyDescriptor(SwitchCase.class, "expression", Expression.class, CYCLE_RISK); //$NON-NLS-1$);

	/**
	 * The "switchLabeledRule" structural property of this node type (type: {@link Boolean}).
	 * @since 3.18
	 */
	public static final SimplePropertyDescriptor SWITCH_LABELED_RULE_PROPERTY =
		new SimplePropertyDescriptor(SwitchCase.class, "switchLabeledRule", boolean.class, MANDATORY); //$NON-NLS-1$

	/**
	 * A list of property descriptors (element type:
	 * {@link StructuralPropertyDescriptor}),
	 * or null if uninitialized.
	 */
	private static final List PROPERTY_DESCRIPTORS;

	/**
	 * A list of property descriptors (element type:
	 * {@link StructuralPropertyDescriptor}),
	 * or null if uninitialized.
	 */
	private static final List PROPERTY_DESCRIPTORS_13;

	static {
		List propertyList = new ArrayList(2);
		createPropertyList(SwitchCase.class, propertyList);
		addProperty(EXPRESSION_PROPERTY, propertyList);
		PROPERTY_DESCRIPTORS = reapPropertyList(propertyList);

		propertyList = new ArrayList(2);
		createPropertyList(SwitchCase.class, propertyList);
		addProperty(EXPRESSIONS2_PROPERTY , propertyList);
		addProperty(SWITCH_LABELED_RULE_PROPERTY, propertyList);
		PROPERTY_DESCRIPTORS_13 = reapPropertyList(propertyList);
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
		if (apiLevel >= AST.JLS14_INTERNAL) {
			return PROPERTY_DESCRIPTORS_13;
		}
		return PROPERTY_DESCRIPTORS;
	}

	/**
	 * The expression; <code>null</code> for none; lazily initialized (but
	 * does <b>not</b> default to none).
	 * @see #expressionInitialized
	 */
	private Expression optionalExpression = null;

	/**
	 * <code>true</code> indicates "->" and <code>false</code> indicates ":".
	 */
	private boolean switchLabeledRule = false;


	/**
	 * The expression list; <code>empty</code> for none;
	 */
	private ASTNode.NodeList expressions = null;

	/**
	 * Indicates whether <code>optionalExpression</code> has been initialized.
	 */
	private boolean expressionInitialized = false;

	/**
	 * Creates a new AST node for a switch case pseudo-statement owned by the
	 * given AST. By default, there is no expression, but legal, and switchLabeledRule
	 * is false which indicates ":".
	 *
	 * @param ast the AST that is to own this node
	 */
	SwitchCase(AST ast) {
		super(ast);
		if (ast.apiLevel >= AST.JLS14_INTERNAL) {
			this.expressions = new ASTNode.NodeList(EXPRESSIONS2_PROPERTY );
		}
	}

	@Override
	final List internalStructuralPropertiesForType(int apiLevel) {
		return propertyDescriptors(apiLevel);
	}

	@Override
	final boolean internalGetSetBooleanProperty(SimplePropertyDescriptor property, boolean get, boolean value) {
		if (property == SWITCH_LABELED_RULE_PROPERTY) {
			if (get) {
				return isSwitchLabeledRule();
			} else {
				setSwitchLabeledRule(value);
				return false;
			}
		}
		// allow default implementation to flag the error
		return super.internalGetSetBooleanProperty(property, get, value);
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
		// allow default implementation to flag the error
		return super.internalGetSetChildProperty(property, get, child);
	}

	@Override
	final List internalGetChildListProperty(ChildListPropertyDescriptor property) {
		if (property == EXPRESSIONS2_PROPERTY ) {
			return expressions();
		}
		// allow default implementation to flag the error
		return super.internalGetChildListProperty(property);
	}

	@Override
	final int getNodeType0() {
		return SWITCH_CASE;
	}

	@SuppressWarnings("unchecked")
	@Override
	ASTNode clone0(AST target) {
		SwitchCase result = new SwitchCase(target);
		result.setSourceRange(getStartPosition(), getLength());
		result.copyLeadingComment(this);
		if (this.ast.apiLevel >= AST.JLS14_INTERNAL) {
			result.expressions().addAll(
				ASTNode.copySubtrees(target, expressions()));
		} else {
			result.setExpression(
					(Expression) ASTNode.copySubtree(target, getExpression()));
		}
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
			if (this.ast.apiLevel >= AST.JLS14_INTERNAL) {
				acceptChildren(visitor, this.expressions);
			} else {
				acceptChild(visitor, getExpression());
			}
		}
		visitor.endVisit(this);
	}

	/**
	 * Returns the expression of this switch case, or
	 * <code>null</code> if there is none (the "default:" case).
	 *
	 * @return the expression node, or <code>null</code> if there is none
	 * @deprecated use expressions() (see JLS 12)
	 */
	public Expression getExpression() {
		if (!this.expressionInitialized) {
			// lazy init must be thread-safe for readers
			synchronized (this) {
				if (!this.expressionInitialized) {
					preLazyInit();
					this.optionalExpression = new SimpleName(this.ast);
					this.expressionInitialized = true;
					postLazyInit(this.optionalExpression, EXPRESSION_PROPERTY);
				}
			}
		}
		return this.optionalExpression;
	}

	/**
	 * Returns the list of expressions of this switch case, or
	 * <code>empty</code> if there is none (the "default:" case).
	 *
	 *  @return the list of expression nodes
	 *    (element type: {@link Expression})
	 * @exception UnsupportedOperationException if this operation is used below JLS14
	 * @since 3.22
	 */
	public List expressions() {
		if (this.expressions == null) {
			unsupportedBelow14();
		}
		return this.expressions;
	}

	/**
	 * Sets the expression of this switch case, or clears it (turns it into
	 * the  "default:" case).
	 *
	 * @param expression the expression node, or <code>null</code> to
	 *    turn it into the  "default:" case
	 * @exception IllegalArgumentException if:
	 * <ul>
	 * <li>the node belongs to a different AST</li>
	 * <li>the node already has a parent</li>
	 * <li>a cycle in would be created</li>
	 * </ul>
	 * @deprecated see JLS 12
	 */
	public void setExpression(Expression expression) {
		ASTNode oldChild = this.optionalExpression;
		preReplaceChild(oldChild, expression, EXPRESSION_PROPERTY);
		this.optionalExpression = expression;
		this.expressionInitialized = true;
		postReplaceChild(oldChild, expression, EXPRESSION_PROPERTY);
	}

	/**
	 * Sets the switchLabeledRule of this switch case as <code>true</code> or <code>false</code>.
	 * <code>true</code> indicates "->" and <code>false</code> indicates ":".

	 * @param switchLabeledRule <code>true</code> or <code>false</code>
	 * @exception UnsupportedOperationException if this operation is used below JLS14
	 * @since 3.22
	 */
	public void setSwitchLabeledRule(boolean switchLabeledRule) {
		unsupportedBelow14();
		preValueChange(SWITCH_LABELED_RULE_PROPERTY);
		this.switchLabeledRule = switchLabeledRule;
		postValueChange(SWITCH_LABELED_RULE_PROPERTY);
	}

	/**
	 * Gets the switchLabeledRule of this switch case as <code>true</code> or <code>false</code>.
	 *<code>true</code> indicates "->" and <code>false</code> indicates ":".
	 *
	 * @return switchLabeledRule <code>true</code> or <code>false</code>
	 * @exception UnsupportedOperationException if this operation is used below JLS14
	 * @since 3.22
	 */
	public boolean isSwitchLabeledRule() {
		unsupportedBelow14();
		return this.switchLabeledRule;
	}

	/**
	 * Returns whether this switch case represents the "default:" case.
	 * <p>
	 * This convenience method is equivalent to
	 * <code>getExpression() == null</code> or <code>expressions().isEmpty()</code>.
	 * </p>
	 *
	 * @return <code>true</code> if this is the default switch case, and
	 *    <code>false</code> if this is a non-default switch case
	 */
	public boolean isDefault()  {
		if (this.ast.apiLevel >= AST.JLS14_INTERNAL) {
			return expressions().isEmpty();
		}
		return getExpression() == null;
	}

	@Override
	int memSize() {
		return super.memSize() + 2 * 4;
	}

	@Override
	int treeSize() {
		return
			memSize()
			+ (this.optionalExpression == null ? 0 : this.optionalExpression.treeSize());
	}


}
