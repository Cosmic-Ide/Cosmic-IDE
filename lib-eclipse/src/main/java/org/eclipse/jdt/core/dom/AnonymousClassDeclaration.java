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
 * Anonymous class declaration AST node type. For JLS2, this type of node appears
 * only as a child on a class instance creation expression.
 * For JLS3, this type of node appears may also appear as the child of
 * an enum constant declaration.
 *
 * <pre>
 * AnonymousClassDeclaration:
 *        <b>{</b> ClassBodyDeclaration <b>}</b>
 * </pre>
 *
 * @see ClassInstanceCreation
 * @see EnumConstantDeclaration
 * @since 2.0
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class AnonymousClassDeclaration extends ASTNode {

	/**
	 * The "bodyDeclarations" structural property of this node type (element type: {@link BodyDeclaration}).
	 * @since 3.0
	 */
	public static final ChildListPropertyDescriptor BODY_DECLARATIONS_PROPERTY =
		new ChildListPropertyDescriptor(AnonymousClassDeclaration.class, "bodyDeclarations", BodyDeclaration.class, CYCLE_RISK); //$NON-NLS-1$

	/**
	 * A list of property descriptors (element type:
	 * {@link StructuralPropertyDescriptor}),
	 * or null if uninitialized.
	 */
	private static final List PROPERTY_DESCRIPTORS;

	static {
		List properyList = new ArrayList(2);
		createPropertyList(AnonymousClassDeclaration.class, properyList);
		addProperty(BODY_DECLARATIONS_PROPERTY, properyList);
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
	 * The body declarations (element type: {@link BodyDeclaration}).
	 * Defaults to none.
	 */
	private ASTNode.NodeList bodyDeclarations =
		new ASTNode.NodeList(BODY_DECLARATIONS_PROPERTY);

	/**
	 * Creates a new AST node for an anonymous class declaration owned
	 * by the given AST. By default, the list of body declarations is empty.
	 * <p>
	 * N.B. This constructor is package-private; all subclasses must be
	 * declared in the same package; clients are unable to declare
	 * additional subclasses.
	 * </p>
	 *
	 * @param ast the AST that is to own this node
	 */
	AnonymousClassDeclaration(AST ast) {
		super(ast);
	}

	@Override
	final List internalStructuralPropertiesForType(int apiLevel) {
		return propertyDescriptors(apiLevel);
	}

	@Override
	final List internalGetChildListProperty(ChildListPropertyDescriptor property) {
		if (property == BODY_DECLARATIONS_PROPERTY) {
			return bodyDeclarations();
		}
		// allow default implementation to flag the error
		return super.internalGetChildListProperty(property);
	}

	@Override
	final int getNodeType0() {
		return ANONYMOUS_CLASS_DECLARATION;
	}

	@Override
	ASTNode clone0(AST target) {
		AnonymousClassDeclaration result = new AnonymousClassDeclaration(target);
		result.setSourceRange(getStartPosition(), getLength());
		result.bodyDeclarations().addAll(
			ASTNode.copySubtrees(target, bodyDeclarations()));
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
			acceptChildren(visitor, this.bodyDeclarations);
		}
		visitor.endVisit(this);
	}

	/**
	 * Returns the live ordered list of body declarations of this
	 * anonymous class declaration.
	 *
	 * @return the live list of body declarations
	 *    (element type: {@link BodyDeclaration})
	 */
	public List bodyDeclarations() {
		return this.bodyDeclarations;
	}

	/**
	 * Resolves and returns the binding for the anonymous class declared in
	 * this declaration.
	 * <p>
	 * Note that bindings are generally unavailable unless requested when the
	 * AST is being built.
	 * </p>
	 *
	 * @return the binding, or <code>null</code> if the binding cannot be
	 *    resolved
	 */
	public ITypeBinding resolveBinding() {
		return this.ast.getBindingResolver().resolveType(this);
	}

	@Override
	int memSize() {
		// treat Code as free
		return BASE_NODE_SIZE + 4;
	}

	@Override
	int treeSize() {
		return
			memSize()
			+ this.bodyDeclarations.listSize();
	}
}
