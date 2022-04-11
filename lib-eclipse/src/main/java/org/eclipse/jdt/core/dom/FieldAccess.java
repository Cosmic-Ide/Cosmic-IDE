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
 * Field access expression AST node type.
 *
 * <pre>
 * FieldAccess:
 * 		Expression <b>.</b> Identifier
 * </pre>
 *
 * <p>
 * Note that there are several kinds of expressions that resemble field access
 * expressions: qualified names, this expressions, and super field access
 * expressions. The following guidelines help with correct usage:
 * </p>
 * <ul>
 *   <li>An expression like "foo.this" can only be represented as a this
 *   expression (<code>ThisExpression</code>) containing a simple name.
 *   "this" is a keyword, and therefore invalid as an identifier.</li>
 *   <li>An expression like "this.foo" can only be represented as a field
 *   access expression (<code>FieldAccess</code>) containing a this expression
 *   and a simple name. Again, this is because "this" is a keyword, and
 *   therefore invalid as an identifier.</li>
 *   <li>An expression with "super" can only be represented as a super field
 *   access expression (<code>SuperFieldAccess</code>). "super" is a also
 *   keyword, and therefore invalid as an identifier.</li>
 *   <li>An expression like "foo.bar" can be represented either as a
 *   qualified name (<code>QualifiedName</code>) or as a field access
 *   expression (<code>FieldAccess</code>) containing simple names. Either
 *   is acceptable, and there is no way to choose between them without
 *   information about what the names resolve to
 *   (<code>ASTParser</code> may return either).</li>
 *   <li>Other expressions ending in an identifier, such as "foo().bar" can
 *   only be represented as field access expressions
 *   (<code>FieldAccess</code>).</li>
 * </ul>
 *
 * @see QualifiedName
 * @see ThisExpression
 * @see SuperFieldAccess
 * @since 2.0
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
@SuppressWarnings("rawtypes")
public class FieldAccess extends Expression {

	/**
	 * The "expression" structural property of this node type (child type: {@link Expression}).
	 * @since 3.0
	 */
	public static final ChildPropertyDescriptor EXPRESSION_PROPERTY =
		new ChildPropertyDescriptor(FieldAccess.class, "expression", Expression.class, MANDATORY, CYCLE_RISK); //$NON-NLS-1$

	/**
	 * The "name" structural property of this node type (child type: {@link SimpleName}).
	 * @since 3.0
	 */
	public static final ChildPropertyDescriptor NAME_PROPERTY =
		new ChildPropertyDescriptor(FieldAccess.class, "name", SimpleName.class, MANDATORY, NO_CYCLE_RISK); //$NON-NLS-1$

	/**
	 * A list of property descriptors (element type:
	 * {@link StructuralPropertyDescriptor}),
	 * or null if uninitialized.
	 */
	private static final List PROPERTY_DESCRIPTORS;

	static {
		List properyList = new ArrayList(3);
		createPropertyList(FieldAccess.class, properyList);
		addProperty(EXPRESSION_PROPERTY, properyList);
		addProperty(NAME_PROPERTY, properyList);
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
	 * The expression; lazily initialized; defaults to an unspecified,
	 * but legal, simple name.
	 */
	private Expression expression = null;

	/**
	 * The field; lazily initialized; defaults to an unspecified,
	 * but legal, simple field name.
	 */
	private SimpleName fieldName = null;

	/**
	 * Creates a new unparented node for a field access expression owned by the
	 * given AST. By default, the expression and field are both unspecified,
	 * but legal, names.
	 * <p>
	 * N.B. This constructor is package-private.
	 * </p>
	 *
	 * @param ast the AST that is to own this node
	 */
	FieldAccess(AST ast) {
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
		if (property == NAME_PROPERTY) {
			if (get) {
				return getName();
			} else {
				setName((SimpleName) child);
				return null;
			}
		}
		// allow default implementation to flag the error
		return super.internalGetSetChildProperty(property, get, child);
	}

	@Override
	final int getNodeType0() {
		return FIELD_ACCESS;
	}

	@Override
	ASTNode clone0(AST target) {
		FieldAccess result = new FieldAccess(target);
		result.setSourceRange(getStartPosition(), getLength());
		result.setExpression((Expression) getExpression().clone(target));
		result.setName((SimpleName) getName().clone(target));
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
			acceptChild(visitor, getName());
		}
		visitor.endVisit(this);
	}

	/**
	 * Returns the expression of this field access expression.
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
	 * Sets the expression of this field access expression.
	 *
	 * @param expression the new expression
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
	 * Returns the name of the field accessed in this field access expression.
	 *
	 * @return the field name
	 */
	public SimpleName getName() {
		if (this.fieldName == null) {
			// lazy init must be thread-safe for readers
			synchronized (this) {
				if (this.fieldName == null) {
					preLazyInit();
					this.fieldName = new SimpleName(this.ast);
					postLazyInit(this.fieldName, NAME_PROPERTY);
				}
			}
		}
		return this.fieldName;
	}

	/**
	 * Sets the name of the field accessed in this field access expression.
	 *
	 * @param fieldName the field name
	 * @exception IllegalArgumentException if:
	 * <ul>
	 * <li>the node belongs to a different AST</li>
	 * <li>the node already has a parent</li>
	 * </ul>
	 */
	public void setName(SimpleName fieldName) {
		if (fieldName == null) {
			throw new IllegalArgumentException();
		}
		ASTNode oldChild = this.fieldName;
		preReplaceChild(oldChild, fieldName, NAME_PROPERTY);
		this.fieldName = fieldName;
		postReplaceChild(oldChild, fieldName, NAME_PROPERTY);
	}

	@Override
	int memSize() {
		// treat Code as free
		return BASE_NODE_SIZE + 2 * 4;
	}

	/**
	 * Resolves and returns the binding for the field accessed by this
	 * expression.
	 * <p>
	 * Note that bindings are generally unavailable unless requested when the
	 * AST is being built.
	 * </p>
	 *
	 * @return the variable binding, or <code>null</code> if the binding cannot
	 * be resolved
	 * @since 3.0
	 */
	public IVariableBinding resolveFieldBinding() {
		return this.ast.getBindingResolver().resolveField(this);
	}

	@Override
	int treeSize() {
		return
			memSize()
			+ (this.expression == null ? 0 : getExpression().treeSize())
			+ (this.fieldName == null ? 0 : getName().treeSize());
	}
}

