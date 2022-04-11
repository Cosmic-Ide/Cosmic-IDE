/*******************************************************************************
 * Copyright (c) 2013, 2014 IBM Corporation and others.
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
 * Lambda expression AST node type (added in JLS8 API).
 * <pre>
 * LambdaExpression:
 *    Identifier <b>-></b> Body
 *    <b>(</b> [ Identifier { <b>,</b> Identifier } ] <b>)</b> <b>-></b> Body
 *    <b>(</b> [ FormalParameter { <b>,</b> FormalParameter } ] <b>)</b> <b>-></b> Body
 * </pre>
 *
 *<p>
 * The first two forms use {@link VariableDeclarationFragment} for the parameter or parameters,
 * while the third form uses {@link SingleVariableDeclaration}.</p>
 *<p>The Body can be either a {@link Block} or an {@link Expression}.</p>
 *
 * @since 3.10
 * @noinstantiate This class is not intended to be instantiated by clients
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class LambdaExpression extends Expression {

	/**
	 * The "parentheses" structural property of this node type (type: {@link Boolean}).
	 * <p>
	 * Note that parentheses are required unless {@link #parameters()} contains
	 * just a single {@link VariableDeclarationFragment}.
	 * ASTRewrite may ignore this property if necessary.
	 * </p>
	 */
	public static final SimplePropertyDescriptor PARENTHESES_PROPERTY =
		new SimplePropertyDescriptor(LambdaExpression.class, "parentheses", boolean.class, MANDATORY); //$NON-NLS-1$

	/**
	 * The "parameters" structural property of this node type (element type: {@link VariableDeclaration}).
	 * <p>
	 * Note that all elements must be of the same type, either all {@link SingleVariableDeclaration} or all {@link VariableDeclarationFragment}.
	 * </p>
	 */
	public static final ChildListPropertyDescriptor PARAMETERS_PROPERTY =
		new ChildListPropertyDescriptor(LambdaExpression.class, "parameters", VariableDeclaration.class, CYCLE_RISK); //$NON-NLS-1$

	/**
	 * The "body" structural property of this node type (child type: {@link ASTNode},
	 * must be either a {@link Block} or an {@link Expression}).
	 */
	public static final ChildPropertyDescriptor BODY_PROPERTY =
		new ChildPropertyDescriptor(LambdaExpression.class, "body", ASTNode.class, MANDATORY, CYCLE_RISK); //$NON-NLS-1$

	/**
	 * A list of property descriptors (element type:
	 * {@link StructuralPropertyDescriptor}),
	 * or null if uninitialized.
	 */
	private static final List PROPERTY_DESCRIPTORS_8_0;

	static {
		List propertyList = new ArrayList(4);
		createPropertyList(LambdaExpression.class, propertyList);
		addProperty(PARENTHESES_PROPERTY, propertyList);
		addProperty(PARAMETERS_PROPERTY, propertyList);
		addProperty(BODY_PROPERTY, propertyList);
		PROPERTY_DESCRIPTORS_8_0 = reapPropertyList(propertyList);
	}

	/**
	 * Returns a list of structural property descriptors for this node type.
	 * Clients must not modify the result.
	 *
	 * @param apiLevel the API level; one of the AST.JLS* constants
	 * @return a list of property descriptors (element type:
	 * {@link StructuralPropertyDescriptor})
	 */
	public static List propertyDescriptors(int apiLevel) {
		return PROPERTY_DESCRIPTORS_8_0;
	}

	/**
	 * Indicates whether parentheses are present or not.
	 * Defaults to <code>true</code>.
	 */
	private boolean hasParentheses = true;

	/**
	 * The parameter declarations
	 * (element type: {@link VariableDeclaration}).
	 * Defaults to an empty list.
	 */
	private ASTNode.NodeList parameters =
		new ASTNode.NodeList(PARAMETERS_PROPERTY);

	/**
	 * The method body.
	 * The method body; lazily initialized, defaults to an empty Block.
	 */
	private ASTNode body = null;

	/**
	 * Creates a new AST node for a LambdaExpression declaration owned
	 * by the given AST.
	 * <p>
	 * N.B. This constructor is package-private; all subclasses must be
	 * declared in the same package; clients are unable to declare
	 * additional subclasses.
	 * </p>
	 *
	 * @param ast the AST that is to own this node
	 */
	LambdaExpression(AST ast) {
		super(ast);
		unsupportedIn2_3_4();
	}

	@Override
	final List internalStructuralPropertiesForType(int apiLevel) {
		return propertyDescriptors(apiLevel);
	}

	@Override
	final boolean internalGetSetBooleanProperty(SimplePropertyDescriptor property, boolean get, boolean value) {
		if (property == PARENTHESES_PROPERTY) {
			if (get) {
				return hasParentheses();
			} else {
				setParentheses(value);
				return false;
			}
		}
		// allow default implementation to flag the error
		return super.internalGetSetBooleanProperty(property, get, value);
	}

	@Override
	final List internalGetChildListProperty(ChildListPropertyDescriptor property) {
		if (property == PARAMETERS_PROPERTY) {
			return parameters();
		}
		// allow default implementation to flag the error
		return super.internalGetChildListProperty(property);
	}

	@Override
	final ASTNode internalGetSetChildProperty(ChildPropertyDescriptor property, boolean get, ASTNode child) {
		if (property == BODY_PROPERTY) {
			if (get) {
				return getBody();
			} else {
				setBody( child);
				return null;
			}
		}
		// allow default implementation to flag the error
		return super.internalGetSetChildProperty(property, get, child);
	}

	@Override
	final int getNodeType0() {
		return LAMBDA_EXPRESSION;
	}

	@Override
	ASTNode clone0(AST target) {
		LambdaExpression result = new LambdaExpression(target);
		result.setSourceRange(getStartPosition(), getLength());
		result.setParentheses(hasParentheses());
		result.parameters().addAll(ASTNode.copySubtrees(target, parameters()));
		result.setBody(ASTNode.copySubtree(target, getBody()));
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
			acceptChildren(visitor, this.parameters);
			acceptChild(visitor, getBody());
		}
		visitor.endVisit(this);
	}

	/**
	 * Returns whether parentheses around the parameters are present or not.
	 * <p>
	 * Note that parentheses are required unless {@link #parameters()} contains
	 * just a single {@link VariableDeclarationFragment}.
	 * ASTRewrite may ignore this property if necessary.
	 * </p>
	 *
	 * @return <code>true</code> if this lambda expression has parentheses around
	 * its parameters and <code>false</code> otherwise
	 */
	public boolean hasParentheses() {
		return this.hasParentheses;
	}

	/**
	 * Sets whether this lambda expression has parentheses around its parameters or not.
	 * <p>
	 * Note that parentheses are required unless {@link #parameters()} contains
	 * just a single {@link VariableDeclarationFragment}.
	 * ASTRewrite may ignore this property if necessary.
	 * </p>
	 *
	 * @param hasParentheses <code>true</code> if this lambda expression has parentheses around its parameters
	 *  and <code>false</code> otherwise
	 */
	public void setParentheses(boolean hasParentheses) {
		preValueChange(PARENTHESES_PROPERTY);
		this.hasParentheses = hasParentheses;
		postValueChange(PARENTHESES_PROPERTY);
	}

	/**
	 * Returns the live ordered list of formal parameters of this lambda expression.
	 * Note that all elements must be of the same type, either
	 * <ul>
	 * <li>all {@link SingleVariableDeclaration} (explicit type), or</li>
	 * <li>all {@link VariableDeclarationFragment} (inferred type).</li>
	 * </ul>
	 *
	 * @return the live list of formal parameters of this lambda expression
	 *    (element type: {@link VariableDeclaration})
	 */
	public List parameters() {
		return this.parameters;
	}

	/**
	 * Returns the body of this lambda expression.
	 *
	 * @return the lambda expression body, which can be either a {@link Block} or an {@link Expression}
	 */
	public ASTNode getBody() {
		if (this.body == null) {
			// lazy init must be thread-safe for readers
			synchronized (this) {
				if (this.body == null) {
					preLazyInit();
					this.body = new Block(this.ast);
					postLazyInit(this.body, BODY_PROPERTY);
				}
			}
		}
		return this.body;
	}

	/**
	 * Sets the body of this lambda expression.
	 *
	 * @param body a block node or an expression node
	 * @exception IllegalArgumentException if:
	 * <ul>
	 * <li>the node belongs to a different AST</li>
	 * <li>the node already has a parent</li>
	 * <li>a cycle in would be created</li>
	 * <li>body is neither a {@link Block} nor an {@link Expression}</li>
	 * </ul>
	 */
	public void setBody(ASTNode body) {
		if (!(body instanceof Expression || body instanceof Block)) {
			throw new IllegalArgumentException();
		}
		ASTNode oldChild = this.body;
		preReplaceChild(oldChild, body, BODY_PROPERTY);
		this.body = body;
		postReplaceChild(oldChild, body, BODY_PROPERTY);
	}

	/**
	 * Resolves and returns the binding for the lambda expression
	 * <p>
	 * Note that bindings are generally unavailable unless requested when the
	 * AST is being built.
	 * </p>
	 *
	 * @return the binding, or <code>null</code> if the binding cannot be
	 *    resolved
	 */
	public IMethodBinding resolveMethodBinding() {
		return this.ast.getBindingResolver().resolveMethod(this);
	}

	@Override
	int memSize() {
		return BASE_NODE_SIZE + 3 * 4;
	}

	@Override
	int treeSize() {
		return
			memSize()
			+ this.parameters.listSize()
			+ (this.body == null ? 0 : getBody().treeSize());
	}
}