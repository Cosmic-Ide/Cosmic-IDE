/*******************************************************************************
 * Copyright (c) 2003, 2013 IBM Corporation and others.
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
 * Type node for a parameterized type (added in JLS3 API).
 * These nodes are used for type references (as opposed to
 * declarations of parameterized types.)
 * <pre>
 * ParameterizedType:
 *    Type <b>&lt;</b> Type { <b>,</b> Type } <b>&gt;</b>
 * </pre>
 * The first type may be a simple type or a qualified type;
 * other kinds of types are meaningless.
 *
 * @since 3.1
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class ParameterizedType extends Type {
    /**
     * This index represents the position inside a parameterized qualified type.
     */
    int index;

	/**
	 * The "type" structural property of this node type (child type: {@link Type}).
	 */
	public static final ChildPropertyDescriptor TYPE_PROPERTY =
		new ChildPropertyDescriptor(ParameterizedType.class, "type", Type.class, MANDATORY, CYCLE_RISK); //$NON-NLS-1$

	/**
	 * The "typeArguments" structural property of this node type (element type: {@link Type}).
	 */
	public static final ChildListPropertyDescriptor TYPE_ARGUMENTS_PROPERTY =
		new ChildListPropertyDescriptor(ParameterizedType.class, "typeArguments", Type.class, CYCLE_RISK); //$NON-NLS-1$

	/**
	 * A list of property descriptors (element type:
	 * {@link StructuralPropertyDescriptor}),
	 * or null if uninitialized.
	 */
	private static final List PROPERTY_DESCRIPTORS;

	static {
		List propertyList = new ArrayList(3);
		createPropertyList(ParameterizedType.class, propertyList);
		addProperty(TYPE_PROPERTY, propertyList);
		addProperty(TYPE_ARGUMENTS_PROPERTY, propertyList);
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
	 * The type node; lazily initialized; defaults to an unspecified, but legal,
	 * type.
	 */
	private Type type = null;

	/**
	 * The type arguments (element type: {@link Type}).
	 * Defaults to an empty list.
	 */
	private ASTNode.NodeList typeArguments =
		new ASTNode.NodeList(TYPE_ARGUMENTS_PROPERTY);

	/**
	 * Creates a new unparented node for a parameterized type owned by the
	 * given AST. By default, an unspecified, but legal, type, and no type
	 * arguments.
	 * <p>
	 * N.B. This constructor is package-private.
	 * </p>
	 *
	 * @param ast the AST that is to own this node
	 */
	ParameterizedType(AST ast) {
		super(ast);
	    unsupportedIn2();
	}

	@Override
	final List internalStructuralPropertiesForType(int apiLevel) {
		return propertyDescriptors(apiLevel);
	}

	@Override
	final ASTNode internalGetSetChildProperty(ChildPropertyDescriptor property, boolean get, ASTNode child) {
		if (property == TYPE_PROPERTY) {
			if (get) {
				return getType();
			} else {
				setType((Type) child);
				return null;
			}
		}
		// allow default implementation to flag the error
		return super.internalGetSetChildProperty(property, get, child);
	}

	@Override
	final List internalGetChildListProperty(ChildListPropertyDescriptor property) {
		if (property == TYPE_ARGUMENTS_PROPERTY) {
			return typeArguments();
		}
		// allow default implementation to flag the error
		return super.internalGetChildListProperty(property);
	}

	@Override
	final int getNodeType0() {
		return PARAMETERIZED_TYPE;
	}

	@Override
	ASTNode clone0(AST target) {
		ParameterizedType result = new ParameterizedType(target);
		result.setSourceRange(getStartPosition(), getLength());
		result.setType((Type) ((ASTNode) getType()).clone(target));
		result.typeArguments().addAll(
			ASTNode.copySubtrees(target, typeArguments()));
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
			acceptChild(visitor, getType());
			acceptChildren(visitor, this.typeArguments);
		}
		visitor.endVisit(this);
	}

	/**
	 * Returns the type of this parameterized type.
	 *
	 * @return the type of this parameterized type
	 */
	public Type getType() {
		if (this.type == null) {
			// lazy init must be thread-safe for readers
			synchronized (this) {
				if (this.type == null) {
					preLazyInit();
					this.type = new SimpleType(this.ast);
					postLazyInit(this.type, TYPE_PROPERTY);
				}
			}
		}
		return this.type;
	}

	/**
	 * Sets the type of this parameterized type.
	 *
	 * @param type the new type of this parameterized type
	 * @exception IllegalArgumentException if:
	 * <ul>
	 * <li>the node belongs to a different AST</li>
	 * <li>the node already has a parent</li>
	 * </ul>
	 */
	public void setType(Type type) {
		if (type == null) {
			throw new IllegalArgumentException();
		}
		ASTNode oldChild = this.type;
		preReplaceChild(oldChild, type, TYPE_PROPERTY);
		this.type = type;
		postReplaceChild(oldChild, type, TYPE_PROPERTY);
	}

	/**
	 * Returns the live ordered list of type arguments of this parameterized
	 * type. For the parameterized type to be plausible, the list should contain
	 * at least one element and not contain primitive or union types.
	 * <p>
	 * Since JLS4, the list can also be empty if this is the type of a
	 * {@link ClassInstanceCreation} (a so-called "diamond").
	 * </p>
	 *
	 * @return the live list of type arguments
	 *    (element type: {@link Type})
	 */
	public List typeArguments() {
		return this.typeArguments;
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
			+ (this.type == null ? 0 : getType().treeSize())
			+ this.typeArguments.listSize();
	}
}

