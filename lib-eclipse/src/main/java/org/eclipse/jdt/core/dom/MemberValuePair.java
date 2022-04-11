/*******************************************************************************
 * Copyright (c) 2004, 2019 IBM Corporation and others.
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
 * Member value pair node (added in JLS3 API). Member value pairs appear in annotations.
 * <pre>
 * MemberValuePair:
 *   SimpleName <b>=</b> Expression
 * </pre>
 * <p>
 * Within annotations, only certain kinds of expressions are meaningful,
 * including other annotations.
 * </p>
 *
 * @see NormalAnnotation
 * @since 3.1
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
@SuppressWarnings("rawtypes")
public class MemberValuePair extends ASTNode {

	/**
	 * The "name" structural property of this node type (child type: {@link SimpleName}).
	 */
	public static final ChildPropertyDescriptor NAME_PROPERTY =
		new ChildPropertyDescriptor(MemberValuePair.class, "name", SimpleName.class, MANDATORY, NO_CYCLE_RISK); //$NON-NLS-1$

	/**
	 * The "value" structural property of this node type (child type: {@link Expression}).
	 */
	public static final ChildPropertyDescriptor VALUE_PROPERTY =
		new ChildPropertyDescriptor(MemberValuePair.class, "value", Expression.class, MANDATORY, CYCLE_RISK); //$NON-NLS-1$

	/**
	 * A list of property descriptors (element type:
	 * {@link StructuralPropertyDescriptor}),
	 * or null if uninitialized.
	 */
	private static final List PROPERTY_DESCRIPTORS;

	static {
		List propertyList = new ArrayList(3);
		createPropertyList(MemberValuePair.class, propertyList);
		addProperty(NAME_PROPERTY, propertyList);
		addProperty(VALUE_PROPERTY, propertyList);
		PROPERTY_DESCRIPTORS = reapPropertyList(propertyList);
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
		return PROPERTY_DESCRIPTORS;
	}

	/**
	 * The member name; lazily initialized; defaults to a unspecified,
	 * legal name.
	 */
	private SimpleName name = null;

	/**
	 * The value; lazily initialized; defaults to a unspecified,
	 * legal expression.
	 */
	private Expression value = null;

	/**
	 * Creates a new AST node for a member value pair owned by the given
	 * AST. By default, the node has an unspecified (but legal) member
	 * name and value.
	 * <p>
	 * N.B. This constructor is package-private.
	 * </p>
	 *
	 * @param ast the AST that is to own this node
	 */
	MemberValuePair(AST ast) {
		super(ast);
	    unsupportedIn2();
	}

	@Override
	final List internalStructuralPropertiesForType(int apiLevel) {
		return propertyDescriptors(apiLevel);
	}

	@Override
	final ASTNode internalGetSetChildProperty(ChildPropertyDescriptor property, boolean get, ASTNode child) {
		if (property == NAME_PROPERTY) {
			if (get) {
				return getName();
			} else {
				setName((SimpleName) child);
				return null;
			}
		}
		if (property == VALUE_PROPERTY) {
			if (get) {
				return getValue();
			} else {
				setValue((Expression) child);
				return null;
			}
		}
		// allow default implementation to flag the error
		return super.internalGetSetChildProperty(property, get, child);
	}

	@Override
	final int getNodeType0() {
		return MEMBER_VALUE_PAIR;
	}

	@Override
	ASTNode clone0(AST target) {
		MemberValuePair result = new MemberValuePair(target);
		result.setSourceRange(getStartPosition(), getLength());
		result.setName((SimpleName) ASTNode.copySubtree(target, getName()));
		result.setValue((Expression) ASTNode.copySubtree(target, getValue()));
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
			acceptChild(visitor, getName());
			acceptChild(visitor, getValue());
		}
		visitor.endVisit(this);
	}

	/**
	 * Returns the member name.
	 *
	 * @return the member name node
	 */
	public SimpleName getName() {
		if (this.name == null) {
			// lazy init must be thread-safe for readers
			synchronized (this) {
				if (this.name == null) {
					preLazyInit();
					this.name = new SimpleName(this.ast);
					postLazyInit(this.name, NAME_PROPERTY);
				}
			}
		}
		return this.name;
	}

	/**
	 * Resolves and returns the member value pair binding for this member value pair.
	 * <p>
	 * Note that bindings are generally unavailable unless requested when the
	 * AST is being built.
	 * </p>
	 *
	 * @return the binding, or <code>null</code> if the binding cannot be
	 *    resolved
	 * @since 3.2
	 */
	public final IMemberValuePairBinding resolveMemberValuePairBinding() {
		return this.ast.getBindingResolver().resolveMemberValuePair(this);
	}

	/**
	 * Sets the member name.
	 *
	 * @param name the member name node
	 * @exception IllegalArgumentException if:
	 * <ul>
	 * <li>the node belongs to a different AST</li>
	 * <li>the node already has a parent</li>
	 * </ul>
	 */
	public void setName(SimpleName name) {
		if (name == null) {
			throw new IllegalArgumentException();
		}
		ASTNode oldChild = this.name;
		preReplaceChild(oldChild, name, NAME_PROPERTY);
		this.name = name;
		postReplaceChild(oldChild, name, NAME_PROPERTY);
	}

	/**
	 * Returns the value expression.
	 *
	 * @return the value expression
	 */
	public Expression getValue() {
		if (this.value == null) {
			// lazy init must be thread-safe for readers
			synchronized (this) {
				if (this.value == null) {
					preLazyInit();
					this.value= new SimpleName(this.ast);
					postLazyInit(this.value, VALUE_PROPERTY);
				}
			}
		}
		return this.value;
	}

	/**
	 * Sets the value of this pair.
	 *
	 * @param value the new value
	 * @exception IllegalArgumentException if:
	 * <ul>
	 * <li>the node belongs to a different AST</li>
	 * <li>the node already has a parent</li>
	 * <li>a cycle in would be created</li>
	 * </ul>
	 */
	public void setValue(Expression value) {
		if (value == null) {
			throw new IllegalArgumentException();
		}
		ASTNode oldChild = this.value;
		preReplaceChild(oldChild, value, VALUE_PROPERTY);
		this.value = value;
		postReplaceChild(oldChild, value, VALUE_PROPERTY);
	}

	@Override
	int memSize() {
		return BASE_NODE_SIZE + 2 * 4;
	}

	@Override
	int treeSize() {
		return
			memSize()
			+ (this.name == null ? 0 : getName().treeSize())
			+ (this.value == null ? 0 : getValue().treeSize());
	}
}
