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
 * Simple or qualified "super" field access expression AST node type.
 *
 * <pre>
 * SuperFieldAccess:
 *     [ ClassName <b>.</b> ] <b>super</b> <b>.</b> Identifier
 * </pre>
 *
 * <p>
 * See <code>FieldAccess</code> for guidelines on handling other expressions
 * that resemble qualified names.
 * </p>
 *
 * @see FieldAccess
 * @since 2.0
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
@SuppressWarnings("rawtypes")
public class SuperFieldAccess extends Expression {

	/**
	 * The "qualifier" structural property of this node type (child type: {@link Name}).
	 * @since 3.0
	 */
	public static final ChildPropertyDescriptor QUALIFIER_PROPERTY =
		new ChildPropertyDescriptor(SuperFieldAccess.class, "qualifier", Name.class, OPTIONAL, NO_CYCLE_RISK); //$NON-NLS-1$

	/**
	 * The "name" structural property of this node type (child type: {@link SimpleName}).
	 * @since 3.0
	 */
	public static final ChildPropertyDescriptor NAME_PROPERTY =
		new ChildPropertyDescriptor(SuperFieldAccess.class, "name", SimpleName.class, MANDATORY, NO_CYCLE_RISK); //$NON-NLS-1$

	/**
	 * A list of property descriptors (element type:
	 * {@link StructuralPropertyDescriptor}),
	 * or null if uninitialized.
	 */
	private static final List PROPERTY_DESCRIPTORS;

	static {
		List propertyList = new ArrayList(3);
		createPropertyList(SuperFieldAccess.class, propertyList);
		addProperty(QUALIFIER_PROPERTY, propertyList);
		addProperty(NAME_PROPERTY, propertyList);
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
	 * The optional qualifier; <code>null</code> for none; defaults to none.
	 */
	private Name optionalQualifier = null;

	/**
	 * The field; lazily initialized; defaults to an unspecified,
	 * but legal, simple field name.
	 */
	private SimpleName fieldName = null;

	/**
	 * Creates a new unparented node for a super field access expression owned
	 * by the given AST. By default, field name is an unspecified, but legal,
	 * name, and there is no qualifier.
	 * <p>
	 * N.B. This constructor is package-private.
	 * </p>
	 *
	 * @param ast the AST that is to own this node
	 */
	SuperFieldAccess(AST ast) {
		super(ast);
	}

	@Override
	final List internalStructuralPropertiesForType(int apiLevel) {
		return propertyDescriptors(apiLevel);
	}

	@Override
	final ASTNode internalGetSetChildProperty(ChildPropertyDescriptor property, boolean get, ASTNode child) {
		if (property == QUALIFIER_PROPERTY) {
			if (get) {
				return getQualifier();
			} else {
				setQualifier((Name) child);
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
		return SUPER_FIELD_ACCESS;
	}

	@Override
	ASTNode clone0(AST target) {
		SuperFieldAccess result = new SuperFieldAccess(target);
		result.setSourceRange(getStartPosition(), getLength());
		result.setName((SimpleName) ASTNode.copySubtree(target, getName()));
		result.setQualifier((Name) ASTNode.copySubtree(target, getQualifier()));
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
			acceptChild(visitor, getQualifier());
			acceptChild(visitor, getName());
		}
		visitor.endVisit(this);
	}

	/**
	 * Returns the qualifier of this "super" field access expression, or
	 * <code>null</code> if there is none.
	 *
	 * @return the qualifier name node, or <code>null</code> if there is none
	 */
	public Name getQualifier() {
		return this.optionalQualifier;
	}

	/**
	 * Sets or clears the qualifier of this "super" field access expression.
	 *
	 * @param name the qualifier name node, or <code>null</code> if
	 *    there is none
	 * @exception IllegalArgumentException if:
	 * <ul>
	 * <li>the node belongs to a different AST</li>
	 * <li>the node already has a parent</li>
	 * </ul>
	 */
	public void setQualifier(Name name) {
		ASTNode oldChild = this.optionalQualifier;
		preReplaceChild(oldChild, name, QUALIFIER_PROPERTY);
		this.optionalQualifier = name;
		postReplaceChild(oldChild, name, QUALIFIER_PROPERTY);
	}

	/**
	 * Returns the name of the field accessed in this "super" field access
	 * expression.
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

	/**
	 * Sets the name of the field accessed in this "super" field access
	 * expression.
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

	@Override
	int treeSize() {
		return
			memSize()
			+ (this.optionalQualifier == null ? 0 : getQualifier().treeSize())
			+ (this.fieldName == null ? 0 : getName().treeSize());
	}
}

