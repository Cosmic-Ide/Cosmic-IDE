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
 * Assignment expression AST node type.
 *
 * <pre>
 * Assignment:
 *    Expression AssignmentOperator Expression
 * </pre>
 *
 * @since 2.0
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class Assignment extends Expression {

	/**
 	 * Assignment operators (typesafe enumeration).
	 * <pre>
	 * AssignmentOperator:<code>
	 *    <b>=</b> ASSIGN
	 *    <b>+=</b> PLUS_ASSIGN
	 *    <b>-=</b> MINUS_ASSIGN
	 *    <b>*=</b> TIMES_ASSIGN
	 *    <b>/=</b> DIVIDE_ASSIGN
	 *    <b>&amp;=</b> BIT_AND_ASSIGN
	 *    <b>|=</b> BIT_OR_ASSIGN
	 *    <b>^=</b> BIT_XOR_ASSIGN
	 *    <b>%=</b> REMAINDER_ASSIGN
	 *    <b>&lt;&lt;=</b> LEFT_SHIFT_ASSIGN
	 *    <b>&gt;&gt;=</b> RIGHT_SHIFT_SIGNED_ASSIGN
	 *    <b>&gt;&gt;&gt;=</b> RIGHT_SHIFT_UNSIGNED_ASSIGN</code>
	 * </pre>
	 */
	public static class Operator {

		/**
		 * The name of the operator
		 */
		private String op;

		/**
		 * Creates a new assignment operator with the given name.
		 * <p>
		 * Note: this constructor is private. The only instances
		 * ever created are the ones for the standard operators.
		 * </p>
		 *
		 * @param op the character sequence for the operator
		 */
		private Operator(String op) {
			this.op = op;
		}

		/**
		 * Returns the character sequence for the operator.
		 *
		 * @return the character sequence for the operator
		 */
		@Override
		public String toString() {
			return this.op;
		}

		/** = operator. */
		public static final Operator ASSIGN = new Operator("=");//$NON-NLS-1$
		/** += operator. */
		public static final Operator PLUS_ASSIGN = new Operator("+=");//$NON-NLS-1$
		/** -= operator. */
		public static final Operator MINUS_ASSIGN = new Operator("-=");//$NON-NLS-1$
		/** *= operator. */
		public static final Operator TIMES_ASSIGN = new Operator("*=");//$NON-NLS-1$
		/** /= operator. */
		public static final Operator DIVIDE_ASSIGN = new Operator("/=");//$NON-NLS-1$
		/** &amp;= operator. */
		public static final Operator BIT_AND_ASSIGN = new Operator("&=");//$NON-NLS-1$
		/** |= operator. */
		public static final Operator BIT_OR_ASSIGN = new Operator("|=");//$NON-NLS-1$
		/** ^= operator. */
		public static final Operator BIT_XOR_ASSIGN = new Operator("^=");//$NON-NLS-1$
		/** %= operator. */
		public static final Operator REMAINDER_ASSIGN = new Operator("%=");//$NON-NLS-1$
		/** &lt;&lt;== operator. */
		public static final Operator LEFT_SHIFT_ASSIGN =
			new Operator("<<=");//$NON-NLS-1$
		/** &gt;&gt;= operator. */
		public static final Operator RIGHT_SHIFT_SIGNED_ASSIGN =
			new Operator(">>=");//$NON-NLS-1$
		/** &gt;&gt;&gt;= operator. */
		public static final Operator RIGHT_SHIFT_UNSIGNED_ASSIGN =
			new Operator(">>>=");//$NON-NLS-1$

		/**
		 * Returns the assignment operator corresponding to the given string,
		 * or <code>null</code> if none.
		 * <p>
		 * <code>toOperator</code> is the converse of <code>toString</code>:
		 * that is, <code>Operator.toOperator(op.toString()) == op</code> for all
		 * operators <code>op</code>.
		 * </p>
		 *
		 * @param token the character sequence for the operator
		 * @return the assignment operator, or <code>null</code> if none
		 */
		public static Operator toOperator(String token) {
			return (Operator) CODES.get(token);
		}

		/**
		 * Map from token to operator (key type: <code>String</code>;
		 * value type: <code>Operator</code>).
		 */
		private static final Map CODES;
		static {
			CODES = new HashMap(20);
			Operator[] ops = {
					ASSIGN,
					PLUS_ASSIGN,
					MINUS_ASSIGN,
					TIMES_ASSIGN,
					DIVIDE_ASSIGN,
					BIT_AND_ASSIGN,
					BIT_OR_ASSIGN,
					BIT_XOR_ASSIGN,
					REMAINDER_ASSIGN,
					LEFT_SHIFT_ASSIGN,
					RIGHT_SHIFT_SIGNED_ASSIGN,
					RIGHT_SHIFT_UNSIGNED_ASSIGN
				};
			for (int i = 0; i < ops.length; i++) {
				CODES.put(ops[i].toString(), ops[i]);
			}
		}
	}

	/**
	 * The "leftHandSide" structural property of this node type (child type: {@link Expression}).
	 * @since 3.0
	 */
	public static final ChildPropertyDescriptor LEFT_HAND_SIDE_PROPERTY =
		new ChildPropertyDescriptor(Assignment.class, "leftHandSide", Expression.class, MANDATORY, CYCLE_RISK); //$NON-NLS-1$

	/**
	 * The "operator" structural property of this node type (type: {@link Assignment.Operator}).
	 * @since 3.0
	 */
	public static final SimplePropertyDescriptor OPERATOR_PROPERTY =
		new SimplePropertyDescriptor(Assignment.class, "operator", Assignment.Operator.class, MANDATORY); //$NON-NLS-1$

	/**
	 * The "rightHandSide" structural property of this node type (child type: {@link Expression}).
	 * @since 3.0
	 */
	public static final ChildPropertyDescriptor RIGHT_HAND_SIDE_PROPERTY =
		new ChildPropertyDescriptor(Assignment.class, "rightHandSide", Expression.class, MANDATORY, CYCLE_RISK); //$NON-NLS-1$

	/**
	 * A list of property descriptors (element type:
	 * {@link StructuralPropertyDescriptor}),
	 * or null if uninitialized.
	 */
	private static final List PROPERTY_DESCRIPTORS;

	static {
		List properyList = new ArrayList(4);
		createPropertyList(Assignment.class, properyList);
		addProperty(LEFT_HAND_SIDE_PROPERTY, properyList);
		addProperty(OPERATOR_PROPERTY, properyList);
		addProperty(RIGHT_HAND_SIDE_PROPERTY, properyList);
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
	 * The assignment operator; defaults to Assignment.Operator.ASSIGN
	 */
	private Assignment.Operator assignmentOperator = Assignment.Operator.ASSIGN;

	/**
	 * The left hand side; lazily initialized; defaults to an unspecified,
	 * but legal, simple name.
	 */
	private Expression leftHandSide = null;

	/**
	 * The right hand side; lazily initialized; defaults to an unspecified,
	 * but legal, simple name.
	 */
	private Expression rightHandSide = null;

	/**
	 * Creates a new AST node for an assignment expression owned by the given
	 * AST. By default, the node has an assignment operator, and unspecified
	 * left and right hand sides.
	 *
	 * @param ast the AST that is to own this node
	 */
	Assignment(AST ast) {
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
		if (property == LEFT_HAND_SIDE_PROPERTY) {
			if (get) {
				return getLeftHandSide();
			} else {
				setLeftHandSide((Expression) child);
				return null;
			}
		}
		if (property == RIGHT_HAND_SIDE_PROPERTY) {
			if (get) {
				return getRightHandSide();
			} else {
				setRightHandSide((Expression) child);
				return null;
			}
		}
		// allow default implementation to flag the error
		return super.internalGetSetChildProperty(property, get, child);
	}

	@Override
	final int getNodeType0() {
		return ASSIGNMENT;
	}

	@Override
	ASTNode clone0(AST target) {
		Assignment result = new Assignment(target);
		result.setSourceRange(getStartPosition(), getLength());
		result.setOperator(getOperator());
		result.setLeftHandSide((Expression) getLeftHandSide().clone(target));
		result.setRightHandSide((Expression) getRightHandSide().clone(target));
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
			acceptChild(visitor, getLeftHandSide());
			acceptChild(visitor, getRightHandSide());
		}
		visitor.endVisit(this);
	}

	/**
	 * Returns the operator of this assignment expression.
	 *
	 * @return the assignment operator
	 */
	public Assignment.Operator getOperator() {
		return this.assignmentOperator;
	}

	/**
	 * Sets the operator of this assignment expression.
	 *
	 * @param assignmentOperator the assignment operator
	 * @exception IllegalArgumentException if the argument is incorrect
	 */
	public void setOperator(Assignment.Operator assignmentOperator) {
		if (assignmentOperator == null) {
			throw new IllegalArgumentException();
		}
		preValueChange(OPERATOR_PROPERTY);
		this.assignmentOperator = assignmentOperator;
		postValueChange(OPERATOR_PROPERTY);
	}

	/**
	 * Returns the left hand side of this assignment expression.
	 *
	 * @return the left hand side node
	 */
	public Expression getLeftHandSide() {
		if (this.leftHandSide  == null) {
			// lazy init must be thread-safe for readers
			synchronized (this) {
				if (this.leftHandSide == null) {
					preLazyInit();
					this.leftHandSide= new SimpleName(this.ast);
					postLazyInit(this.leftHandSide, LEFT_HAND_SIDE_PROPERTY);
				}
			}
		}
		return this.leftHandSide;
	}

	/**
	 * Sets the left hand side of this assignment expression.
	 *
	 * @param expression the left hand side node
	 * @exception IllegalArgumentException if:
	 * <ul>
	 * <li>the node belongs to a different AST</li>
	 * <li>the node already has a parent</li>
	 * <li>a cycle in would be created</li>
	 * </ul>
	 */
	public void setLeftHandSide(Expression expression) {
		if (expression == null) {
			throw new IllegalArgumentException();
		}
		// an Assignment may occur inside a Expression - must check cycles
		ASTNode oldChild = this.leftHandSide;
		preReplaceChild(oldChild, expression, LEFT_HAND_SIDE_PROPERTY);
		this.leftHandSide = expression;
		postReplaceChild(oldChild, expression, LEFT_HAND_SIDE_PROPERTY);
	}

	/**
	 * Returns the right hand side of this assignment expression.
	 *
	 * @return the right hand side node
	 */
	public Expression getRightHandSide() {
		if (this.rightHandSide  == null) {
			// lazy init must be thread-safe for readers
			synchronized (this) {
				if (this.rightHandSide == null) {
					preLazyInit();
					this.rightHandSide= new SimpleName(this.ast);
					postLazyInit(this.rightHandSide, RIGHT_HAND_SIDE_PROPERTY);
				}
			}
		}
		return this.rightHandSide;
	}

	/**
	 * Sets the right hand side of this assignment expression.
	 *
	 * @param expression the right hand side node
	 * @exception IllegalArgumentException if:
	 * <ul>
	 * <li>the node belongs to a different AST</li>
	 * <li>the node already has a parent</li>
	 * <li>a cycle in would be created</li>
	 * </ul>
	 */
	public void setRightHandSide(Expression expression) {
		if (expression == null) {
			throw new IllegalArgumentException();
		}
		// an Assignment may occur inside a Expression - must check cycles
		ASTNode oldChild = this.rightHandSide;
		preReplaceChild(oldChild, expression, RIGHT_HAND_SIDE_PROPERTY);
		this.rightHandSide = expression;
		postReplaceChild(oldChild, expression, RIGHT_HAND_SIDE_PROPERTY);
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
			+ (this.leftHandSide == null ? 0 : getLeftHandSide().treeSize())
			+ (this.rightHandSide == null ? 0 : getRightHandSide().treeSize());
	}
}

