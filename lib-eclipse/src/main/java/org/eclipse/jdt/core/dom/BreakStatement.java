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

package org.eclipse.jdt.core.dom;

import java.util.ArrayList;
import java.util.List;

/**
 * Break statement AST node type.
 *
 * <pre>
 * BreakStatement:
 *    <b>break</b> [ Identifier ] <b>;</b>
 *
 *    Break statement allows expression as part of Java 12 preview feature (JEP 325)
 *		<b>break</b> <b>{ Identifier | Expression }</b>
 * </pre>
 *
 * @since 2.0
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
@SuppressWarnings("rawtypes")
public class BreakStatement extends Statement {

	/**
	 * The "label" structural property of this node type (child type: {@link SimpleName}).
	 * @since 3.0
	 */
	public static final ChildPropertyDescriptor LABEL_PROPERTY =
		new ChildPropertyDescriptor(BreakStatement.class, "label", SimpleName.class, OPTIONAL, NO_CYCLE_RISK); //$NON-NLS-1$

	/**
	 * The "expression" structural property of this node type (child type: {@link Expression}). (added in JEP 325).
	 * @noreference This property is not intended to be referenced by clients as it is a part of Java preview feature.
	 * @deprecated
	 * @since 3.18
	 */
	public static final ChildPropertyDescriptor EXPRESSION_PROPERTY =
			new ChildPropertyDescriptor(BreakStatement.class, "expression", Expression.class, OPTIONAL, NO_CYCLE_RISK); //$NON-NLS-1$);

	/**
	 * A list of property descriptors (element type:
	 * {@link StructuralPropertyDescriptor}),
	 * or null if uninitialized.
	 */
	private static final List PROPERTY_DESCRIPTORS;

	/**
	 * <code>true</code> indicates implicit and <code>false</code> indicates not implicit.
	 */
	private boolean isImplicit = false;

	static {
		List properyList = new ArrayList(2);
		createPropertyList(BreakStatement.class, properyList);
		addProperty(LABEL_PROPERTY, properyList);
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
	 * Returns a list of structural property descriptors for this node type.
	 * Clients must not modify the result.
	 *
	 * @param apiLevel the API level; one of the
	 * <code>AST.JLS*</code> constants
	 * @param previewEnabled the previewEnabled flag

	 * @return a list of property descriptors (element type:
	 * {@link StructuralPropertyDescriptor})
	 * @noreference This method is not intended to be referenced by clients as it is a part of Java preview feature.
	 * @deprecated
	 * @since 3.20
	 */
	public static List propertyDescriptors(int apiLevel, boolean previewEnabled) {
		return PROPERTY_DESCRIPTORS;
	}

	/**
	 * The label, or <code>null</code> if none; none by default.
	 */
	private SimpleName optionalLabel = null;

	/**
	 * The expression; <code>null</code> for none
	 */
	private Expression optionalExpression = null;

	/**
	 * Creates a new unparented break statement node owned by the given
	 * AST. By default, the break statement has no label/identifier/expression and is not implicit.
	 * <p>
	 * N.B. This constructor is package-private.
	 * </p>
	 *
	 * @param ast the AST that is to own this node
	 */
	BreakStatement(AST ast) {
		super(ast);
	}

	@Override
	final List internalStructuralPropertiesForType(int apiLevel) {
		return propertyDescriptors(apiLevel);
	}

	@Override
	final ASTNode internalGetSetChildProperty(ChildPropertyDescriptor property, boolean get, ASTNode child) {
		if (property == LABEL_PROPERTY) {
			if (get) {
				return getLabel();
			} else {
				setLabel((SimpleName) child);
				return null;
			}
		}
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
	final int getNodeType0() {
		return BREAK_STATEMENT;
	}

	@Override
	ASTNode clone0(AST target) {
		BreakStatement result = new BreakStatement(target);
		result.setSourceRange(getStartPosition(), getLength());
		result.copyLeadingComment(this);
		result.setLabel((SimpleName) ASTNode.copySubtree(target, getLabel()));
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
			acceptChild(visitor, getLabel());
		}
		visitor.endVisit(this);
	}

	/**
	 * Returns the label of this break statement, or <code>null</code> if
	 * there is none.
	 *
	 * @return the label, or <code>null</code> if there is none
	 */
	public SimpleName getLabel() {
		return this.optionalLabel;
	}

	/**
	 * Sets or clears the label of this break statement.
	 *
	 * @param label the label, or <code>null</code> if
	 *    there is none
	 * @exception IllegalArgumentException if:
	 * <ul>
	 * <li>the node belongs to a different AST</li>
	 * <li>the node already has a parent</li>
	 * </ul>
	 */
	public void setLabel(SimpleName label) {
		ASTNode oldChild = this.optionalLabel;
		preReplaceChild(oldChild, label, LABEL_PROPERTY);
		this.optionalLabel = label;
		postReplaceChild(oldChild, label, LABEL_PROPERTY);
	}

	/**
	 * Returns the expression of this break statement, or <code>null</code> if
	 * there is none.
	 *
	 * @return the expression, or <code>null</code> if there is none
	 * @exception UnsupportedOperationException if this operation is used other than JLS12
	 * @noreference This method is not intended to be referenced by clients as it is a part of Java preview feature.
	 * @nooverride This method is not intended to be re-implemented or extended by clients as it is a part of Java preview feature.
	 * @deprecated
	 * @since 3.18
	 */
	public Expression getExpression() {
		// optionalExpression can be null
		supportedOnlyIn12();
		return this.optionalExpression;
	}

	/**
	 * Sets or clears the expression of this break statement.
	 *
	 * @param expression the expression, or <code>null</code> if
	 *    there is none
	 * @exception IllegalArgumentException if:
	 * <ul>
	 * <li>the node belongs to a different AST</li>
	 * <li>the node already has a parent</li>
	 * </ul>
	 * @exception UnsupportedOperationException if this operation is used other than JLS12
	 * @noreference This method is not intended to be referenced by clients as it is a part of Java preview feature.
	 * @nooverride This method is not intended to be re-implemented or extended by clients as it is a part of Java preview feature.
	 * @deprecated
	 * @since 3.18
	 */
	public void setExpression(Expression expression) {
		supportedOnlyIn12();
		ASTNode oldChild = this.optionalExpression;
		preReplaceChild(oldChild, expression, EXPRESSION_PROPERTY);
		this.optionalExpression = expression;
		postReplaceChild(oldChild, expression, EXPRESSION_PROPERTY);
	}

	/**
	 * Gets the isImplicit of this break statement as <code>true</code> or <code>false</code>.
	 *<code>true</code> indicates implicit and <code>false</code> indicates not implicit.
	 *
	 * @return isImplicit <code>true</code> or <code>false</code>
	 * @exception UnsupportedOperationException if this operation is used other than JLS12
	 * @noreference This method is not intended to be referenced by clients as it is a part of Java preview feature.
	 * @nooverride This method is not intended to be re-implemented or extended by clients as it is a part of Java preview feature.
	 * @deprecated
	 * @since 3.18
	 */
	public boolean isImplicit() {
		supportedOnlyIn12();
		return this.isImplicit;
	}

	/**
	 * Sets the isImplicit of this break statement as <code>true</code> or <code>false</code>.
	 * <code>true</code> indicates implicit and <code>false</code> indicates not implicit. This flag is
	 * generated by compiler and is not expected to be set by client.

	 * @param isImplicit <code>true</code> or <code>false</code>
	 * @exception UnsupportedOperationException if this operation is used other than JLS12
	 * @deprecated
	 * @since 3.18
	 */
	void setImplicit(boolean isImplicit) {
		supportedOnlyIn12();
		this.isImplicit = isImplicit;
	}

	@Override
	int memSize() {
		return super.memSize() + 2 * 4;
	}

	@Override
	int treeSize() {
		return
			memSize()
			+ (this.optionalLabel == null ? 0 : getLabel().treeSize())
			+ (this.optionalExpression == null ? 0 : getExpression().treeSize());
	}
}

