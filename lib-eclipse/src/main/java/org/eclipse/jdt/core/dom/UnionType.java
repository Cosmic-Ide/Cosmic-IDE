/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation and others.
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
 * Type node for an union type (added in JLS4 API).
 * <pre>
 * UnionType:
 *    Type <b>|</b> Type { <b>|</b> Type }
 * </pre>
 * <p>
 * This kind of node is used inside a catch clause's formal parameter type.
 * </p>
 *
 * @since 3.7.1
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class UnionType extends Type {

	/**
	 * The "types" structural property of this node type (element type: {@link Type}).
	 */
	public static final ChildListPropertyDescriptor TYPES_PROPERTY =
		new ChildListPropertyDescriptor(UnionType.class, "types", Type.class, CYCLE_RISK); //$NON-NLS-1$

	/**
	 * A list of property descriptors (element type:
	 * {@link StructuralPropertyDescriptor}),
	 * or null if uninitialized.
	 */
	private static final List PROPERTY_DESCRIPTORS;

	static {
		List propertyList = new ArrayList(2);
		createPropertyList(UnionType.class, propertyList);
		addProperty(TYPES_PROPERTY, propertyList);
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
	 */
	public static List propertyDescriptors(int apiLevel) {
		return PROPERTY_DESCRIPTORS;
	}

	/**
	 * The list of types (element type: {@link Type}).  Defaults to an empty list.
	 */
	private ASTNode.NodeList types = new ASTNode.NodeList(TYPES_PROPERTY);

	/**
	 * Creates a new unparented node for an union type owned by the given AST.
	 * By default, it has no types.<p>
	 *
	 * N.B. This constructor is package-private.
	 * </p>
	 *
	 * @param ast the AST that is to own this node
	 */
	UnionType(AST ast) {
		super(ast);
		unsupportedIn2_3();
	}

	@Override
	final List internalStructuralPropertiesForType(int apiLevel) {
		return propertyDescriptors(apiLevel);
	}

	@Override
	final List internalGetChildListProperty(ChildListPropertyDescriptor property) {
		if (property == TYPES_PROPERTY) {
			return types();
		}
		// allow default implementation to flag the error
		return super.internalGetChildListProperty(property);
	}

	@Override
	final int getNodeType0() {
		return UNION_TYPE;
	}

	@Override
	ASTNode clone0(AST target) {
		UnionType result = new UnionType(target);
		result.setSourceRange(getStartPosition(), getLength());
		result.types().addAll(
				ASTNode.copySubtrees(target, types()));
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
			acceptChildren(visitor, this.types);
		}
		visitor.endVisit(this);
	}

	/**
	 * Returns the live ordered list of types in this union type.
	 * Adding and removing nodes from this list affects this node
	 * dynamically. All nodes in this list must be
	 * <code>Type</code>s; attempts to add any other
	 * type of node will trigger an exception.
	 *
	 * @return the live list of types in this union type (element type: {@link Type})
	 */
	public List types() {
		return this.types;
	}

	@Override
	int memSize() {
		return BASE_NODE_SIZE + 1 * 4;
	}

	@Override
	int treeSize() {
		return
			memSize()
			+ this.types.listSize();
	}
}

