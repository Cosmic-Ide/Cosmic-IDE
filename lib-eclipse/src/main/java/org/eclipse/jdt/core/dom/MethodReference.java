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

import java.util.List;

/**
 * Abstract base class of all AST node types that represent a method reference
 * expression (added in JLS8 API).
 *
 * <pre>
 * MethodReference:
 *    CreationReference
 *    ExpressionMethodReference
 *    SuperMethodReference
 *    TypeMethodReference
 * </pre>
 * <p>
 * A method reference that is represented by a simple or qualified name,
 * followed by <code>::</code>, followed by a simple name can be represented
 * as {@link ExpressionMethodReference} or as {@link TypeMethodReference}.
 * The ASTParser currently prefers the first form.
 * </p>
 *
 * @see CreationReference
 * @see ExpressionMethodReference
 * @see SuperMethodReference
 * @see TypeMethodReference
 * @since 3.10
 */
@SuppressWarnings({"rawtypes"})
public abstract class MethodReference extends Expression {

	/**
	 * The type arguments (element type: {@link Type}).
	 * Defaults to an empty list (see constructor).
	 */
	ASTNode.NodeList typeArguments;

	/**
	 * Creates and returns a structural property descriptor for the "typeArguments"
	 * property declared on the given concrete node type (element type: {@link Type}).
	 *
	 * @return the property descriptor
	 */
	static final ChildListPropertyDescriptor internalTypeArgumentsFactory(Class nodeClass) {
		return new ChildListPropertyDescriptor(nodeClass, "typeArguments", Type.class, NO_CYCLE_RISK); //$NON-NLS-1$
	}

	/**
	 * Returns the structural property descriptor for the "typeArguments" property
	 * of this node (element type: {@link Type}).
	 *
	 * @return the property descriptor
	 */
	abstract ChildListPropertyDescriptor internalTypeArgumentsProperty();

	/**
	 * Returns the structural property descriptor for the "typeArguments" property
	 * of this node (element type: {@link Type}).
	 *
	 * @return the property descriptor
	 */
	public final ChildListPropertyDescriptor getTypeArgumentsProperty() {
		return internalTypeArgumentsProperty();
	}

	/**
	 * Creates a new AST node for a method reference owned by the given AST.
	 * <p>
	 * N.B. This constructor is package-private.
	 * </p>
	 *
	 * @param ast the AST that is to own this node
	 */
	MethodReference(AST ast) {
		super(ast);
		this.typeArguments = new ASTNode.NodeList(getTypeArgumentsProperty());
	}

	/**
	 * Returns the live ordered list of type arguments of this method reference.
	 *
	 * @return the live list of type arguments
	 *    (element type: {@link Type})
	 */
	public List typeArguments() {
		return this.typeArguments;
	}

	/**
	 * Resolves and returns the binding for the method referenced by this
	 * method reference expression.
	 * <p>
	 * Note that bindings are generally unavailable unless requested when the
	 * AST is being built.
	 * </p>
	 *
	 * @return the method binding, or <code>null</code> if the binding cannot
	 * be resolved
	 */
	public IMethodBinding resolveMethodBinding() {
		return this.ast.getBindingResolver().resolveMethod(this);
	}
}
