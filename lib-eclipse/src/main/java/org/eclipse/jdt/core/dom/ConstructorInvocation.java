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
 * Alternate constructor invocation statement AST node type.
 * <pre>
 * ConstructorInvocation:
 *    [ <b>&lt;</b> Type { <b>,</b> Type } <b>&gt;</b> ]
 *            <b>this</b> <b>(</b> [ Expression { <b>,</b> Expression } ] <b>)</b> <b>;</b>
 * </pre>
 *
 * @since 2.0
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class ConstructorInvocation extends Statement {

	/**
	 * The "typeArguments" structural property of this node type (element type: {@link Type}) (added in JLS3 API).
	 * @since 3.1
	 */
	public static final ChildListPropertyDescriptor TYPE_ARGUMENTS_PROPERTY =
		new ChildListPropertyDescriptor(ConstructorInvocation.class, "typeArguments", Type.class, NO_CYCLE_RISK); //$NON-NLS-1$

	/**
	 * The "arguments" structural property of this node type (element type: {@link Expression}).
	 * @since 3.0
	 */
	public static final ChildListPropertyDescriptor ARGUMENTS_PROPERTY =
		new ChildListPropertyDescriptor(ConstructorInvocation.class, "arguments", Expression.class, CYCLE_RISK); //$NON-NLS-1$

	/**
	 * A list of property descriptors (element type:
	 * {@link StructuralPropertyDescriptor}),
	 * or null if uninitialized.
	 * @since 3.0
	 */
	private static final List PROPERTY_DESCRIPTORS_2_0;

	/**
	 * A list of property descriptors (element type:
	 * {@link StructuralPropertyDescriptor}),
	 * or null if uninitialized.
	 * @since 3.1
	 */
	private static final List PROPERTY_DESCRIPTORS_3_0;

	static {
		List properyList = new ArrayList(2);
		createPropertyList(ConstructorInvocation.class, properyList);
		addProperty(ARGUMENTS_PROPERTY, properyList);
		PROPERTY_DESCRIPTORS_2_0 = reapPropertyList(properyList);

		properyList = new ArrayList(3);
		createPropertyList(ConstructorInvocation.class, properyList);
		addProperty(TYPE_ARGUMENTS_PROPERTY, properyList);
		addProperty(ARGUMENTS_PROPERTY, properyList);
		PROPERTY_DESCRIPTORS_3_0 = reapPropertyList(properyList);
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
		if (apiLevel == AST.JLS2_INTERNAL) {
			return PROPERTY_DESCRIPTORS_2_0;
		} else {
			return PROPERTY_DESCRIPTORS_3_0;
		}
	}

	/**
	 * The type arguments (element type: {@link Type}).
	 * Null in JLS2. Added in JLS3; defaults to an empty list
	 * (see constructor).
	 * @since 3.1
	 */
	private ASTNode.NodeList typeArguments = null;

	/**
	 * The list of argument expressions (element type:
	 * {@link Expression}). Defaults to an empty list.
	 */
	private ASTNode.NodeList arguments =
		new ASTNode.NodeList(ARGUMENTS_PROPERTY);

	/**
	 * Creates a new AST node for an alternate constructor invocation statement
	 * owned by the given AST. By default, an empty list of arguments.
	 *
	 * @param ast the AST that is to own this node
	 */
	ConstructorInvocation(AST ast) {
		super(ast);
		if (ast.apiLevel >= AST.JLS3_INTERNAL) {
			this.typeArguments = new ASTNode.NodeList(TYPE_ARGUMENTS_PROPERTY);
		}
	}

	@Override
	final List internalStructuralPropertiesForType(int apiLevel) {
		return propertyDescriptors(apiLevel);
	}

	@Override
	final List internalGetChildListProperty(ChildListPropertyDescriptor property) {
		if (property == ARGUMENTS_PROPERTY) {
			return arguments();
		}
		if (property == TYPE_ARGUMENTS_PROPERTY) {
			return typeArguments();
		}
		// allow default implementation to flag the error
		return super.internalGetChildListProperty(property);
	}

	@Override
	final int getNodeType0() {
		return CONSTRUCTOR_INVOCATION;
	}

	@Override
	ASTNode clone0(AST target) {
		ConstructorInvocation result = new ConstructorInvocation(target);
		result.setSourceRange(getStartPosition(), getLength());
		result.copyLeadingComment(this);
		if (this.ast.apiLevel >= AST.JLS3_INTERNAL) {
			result.typeArguments().addAll(ASTNode.copySubtrees(target, typeArguments()));
		}
		result.arguments().addAll(ASTNode.copySubtrees(target, arguments()));
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
			if (this.ast.apiLevel >= AST.JLS3_INTERNAL) {
				acceptChildren(visitor, this.typeArguments);
			}
			acceptChildren(visitor, this.arguments);
		}
		visitor.endVisit(this);
	}

	/**
	 * Returns the live ordered list of type arguments of this constructor
	 * invocation (added in JLS3 API).
	 *
	 * @return the live list of type arguments
	 *    (element type: {@link Type})
	 * @exception UnsupportedOperationException if this operation is used in
	 * a JLS2 AST
	 * @since 3.1
	 */
	public List typeArguments() {
		// more efficient than just calling unsupportedIn2() to check
		if (this.typeArguments == null) {
			unsupportedIn2();
		}
		return this.typeArguments;
	}

	/**
	 * Returns the live ordered list of argument expressions in this alternate
	 * constructor invocation statement.
	 *
	 * @return the live list of argument expressions
	 *    (element type: {@link Expression})
	 */
	public List arguments() {
		return this.arguments;
	}

	/**
	 * Resolves and returns the binding for the constructor invoked by this
	 * expression.
	 * <p>
	 * Note that bindings are generally unavailable unless requested when the
	 * AST is being built.
	 * </p>
	 *
	 * @return the constructor binding, or <code>null</code> if the binding
	 *    cannot be resolved
	 */
	public IMethodBinding resolveConstructorBinding() {
		return this.ast.getBindingResolver().resolveConstructor(this);
	}

	@Override
	int memSize() {
		// treat Code as free
		return BASE_NODE_SIZE + 2 * 4;
	}

	@Override
	int treeSize() {
		return
			memSize()
			+ (this.typeArguments == null ? 0 : this.typeArguments.listSize())
			+ (this.arguments == null ? 0 : this.arguments.listSize());
	}
}

