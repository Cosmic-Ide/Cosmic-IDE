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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Prefix expression AST node type.
 *
 * <pre>
 * PrefixExpression:
 *    PrefixOperator Expression
 * </pre>
 *
 * @since 2.0
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class PrefixExpression extends Expression {

	/**
 	 * Prefix operators (typesafe enumeration).
	 * <pre>
	 * PrefixOperator:
	 *    <b><code>++</code></b>  <code>INCREMENT</code>
	 *    <b><code>--</code></b>  <code>DECREMENT</code>
	 *    <b><code>+</code></b>  <code>PLUS</code>
	 *    <b><code>-</code></b>  <code>MINUS</code>
	 *    <b><code>~</code></b>  <code>COMPLEMENT</code>
	 *    <b><code>!</code></b>  <code>NOT</code>
	 * </pre>
	 */
	public static class Operator {

		/**
		 * The token for the operator.
		 */
		private String token;

		/**
		 * Creates a new prefix operator with the given token.
		 * <p>
		 * Note: this constructor is private. The only instances
		 * ever created are the ones for the standard operators.
		 * </p>
		 *
		 * @param token the character sequence for the operator
		 */
		private Operator(String token) {
			this.token = token;
		}

		/**
		 * Returns the character sequence for the operator.
		 *
		 * @return the character sequence for the operator
		 */
		@Override
		public String toString() {
			return this.token;
		}

		/** Prefix increment "++" operator. */
		public static final Operator INCREMENT = new Operator("++");//$NON-NLS-1$
		/** Prefix decrement "--" operator. */
		public static final Operator DECREMENT = new Operator("--");//$NON-NLS-1$
		/** Unary plus "+" operator. */
		public static final Operator PLUS = new Operator("+");//$NON-NLS-1$
		/** Unary minus "-" operator. */
		public static final Operator MINUS = new Operator("-");//$NON-NLS-1$
		/** Bitwise complement "~" operator. */
		public static final Operator COMPLEMENT = new Operator("~");//$NON-NLS-1$
		/** Logical complement "!" operator. */
		public static final Operator NOT = new Operator("!");//$NON-NLS-1$

		/**
		 * Map from token to operator (key type: <code>String</code>;
		 * value type: <code>Operator</code>).
		 */
		private static final Map CODES;
		static {
			CODES = new HashMap(20);
			Operator[] ops = {
					INCREMENT,
					DECREMENT,
					PLUS,
					MINUS,
					COMPLEMENT,
					NOT,
				};
			for (int i = 0; i < ops.length; i++) {
				CODES.put(ops[i].toString(), ops[i]);
			}
		}

		/**
		 * Returns the prefix operator corresponding to the given string,
		 * or <code>null</code> if none.
		 * <p>
		 * <code>toOperator</code> is the converse of <code>toString</code>:
		 * that is, <code>Operator.toOperator(op.toString()) == op</code> for
		 * all operators <code>op</code>.
		 * </p>
		 *
		 * @param token the character sequence for the operator
		 * @return the prefix operator, or <code>null</code> if none
		 */
		public static Operator toOperator(String token) {
			return (Operator) CODES.get(token);
		}
	}

	/**
	 * The "operator" structural property of this node type (type: {@link PrefixExpression.Operator}).
	 * @since 3.0
	 */
	public static final SimplePropertyDescriptor OPERATOR_PROPERTY =
		new SimplePropertyDescriptor(PrefixExpression.class, "operator", PrefixExpression.Operator.class, MANDATORY); //$NON-NLS-1$

	/**
	 * The "operand" structural property of this node type (child type: {@link Expression}).
	 * @since 3.0
	 */
	public static final ChildPropertyDescriptor OPERAND_PROPERTY =
		new ChildPropertyDescriptor(PrefixExpression.class, "operand", Expression.class, MANDATORY, CYCLE_RISK); //$NON-NLS-1$

	/**
	 * A list of property descriptors (element type:
	 * {@link StructuralPropertyDescriptor}),
	 * or null if uninitialized.
	 */
	private static final List PROPERTY_DESCRIPTORS;

	static {
		List propertyList = new ArrayList(3);
		createPropertyList(PrefixExpression.class, propertyList);
		addProperty(OPERATOR_PROPERTY, propertyList);
		addProperty(OPERAND_PROPERTY, propertyList);
		PROPERTY_DESCRIPTORS = reapPropertyList(propertyList);
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
	 * The operator; defaults to an unspecified prefix operator.
	 */
	private PrefixExpression.Operator operator =
		PrefixExpression.Operator.PLUS;

	/**
	 * The operand; lazily initialized; defaults to an unspecified,
	 * but legal, simple name.
	 */
	private Expression operand = null;

	/**
	 * Creates a new AST node for an prefix expression owned by the given
	 * AST. By default, the node has unspecified (but legal) operator and
	 * operand.
	 *
	 * @param ast the AST that is to own this node
	 */
	PrefixExpression(AST ast) {
		super(ast);
	}

	@Override
	final List internalStructuralPropertiesForType(int apiLevel) {
		return propertyDescriptors(apiLevel);
	}

	@Override
	final Object internalGetSetObjectProperty(SimplePropertyDescriptor property, boolean get, Object value) {
		if (property == OPERATOR_PROPERTY) {
			if (get) {
				return getOperator();
			} else {
				setOperator((Operator) value);
				return null;
			}
		}
		// allow default implementation to flag the error
		return super.internalGetSetObjectProperty(property, get, value);
	}

	@Override
	final ASTNode internalGetSetChildProperty(ChildPropertyDescriptor property, boolean get, ASTNode child) {
		if (property == OPERAND_PROPERTY) {
			if (get) {
				return getOperand();
			} else {
				setOperand((Expression) child);
				return null;
			}
		}
		// allow default implementation to flag the error
		return super.internalGetSetChildProperty(property, get, child);
	}

	@Override
	final int getNodeType0() {
		return PREFIX_EXPRESSION;
	}

	@Override
	ASTNode clone0(AST target) {
		PrefixExpression result = new PrefixExpression(target);
		result.setSourceRange(getStartPosition(), getLength());
		result.setOperator(getOperator());
		result.setOperand((Expression) getOperand().clone(target));
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
			acceptChild(visitor, getOperand());
		}
		visitor.endVisit(this);
	}

	/**
	 * Returns the operator of this prefix expression.
	 *
	 * @return the operator
	 */
	public PrefixExpression.Operator getOperator() {
		return this.operator;
	}

	/**
	 * Sets the operator of this prefix expression.
	 *
	 * @param operator the operator
	 * @exception IllegalArgumentException if the argument is incorrect
	 */
	public void setOperator(PrefixExpression.Operator operator) {
		if (operator == null) {
			throw new IllegalArgumentException();
		}
		preValueChange(OPERATOR_PROPERTY);
		this.operator = operator;
		postValueChange(OPERATOR_PROPERTY);
	}

	/**
	 * Returns the operand of this prefix expression.
	 *
	 * @return the operand expression node
	 */
	public Expression getOperand() {
		if (this.operand  == null) {
			// lazy init must be thread-safe for readers
			synchronized (this) {
				if (this.operand == null) {
					preLazyInit();
					this.operand= new SimpleName(this.ast);
					postLazyInit(this.operand, OPERAND_PROPERTY);
				}
			}
		}
		return this.operand;
	}

	/**
	 * Sets the operand of this prefix expression.
	 *
	 * @param expression the operand expression node
	 * @exception IllegalArgumentException if:
	 * <ul>
	 * <li>the node belongs to a different AST</li>
	 * <li>the node already has a parent</li>
	 * <li>a cycle in would be created</li>
	 * </ul>
	 */
	public void setOperand(Expression expression) {
		if (expression == null) {
			throw new IllegalArgumentException();
		}
		ASTNode oldChild = this.operand;
		preReplaceChild(oldChild, expression, OPERAND_PROPERTY);
		this.operand = expression;
		postReplaceChild(oldChild, expression, OPERAND_PROPERTY);
	}

	@Override
	int memSize() {
		// treat Operator as free
		return BASE_NODE_SIZE + 2 * 4;
	}

	@Override
	int treeSize() {
		return
			memSize()
			+ (this.operand == null ? 0 : getOperand().treeSize());
	}
}

