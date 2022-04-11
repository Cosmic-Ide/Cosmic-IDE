/*******************************************************************************
 * Copyright (c) 2004, 2013 IBM Corporation and others.
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
 * AST node for a member reference within a doc comment
 * ({@link Javadoc}). The principal uses of these are in "@see" and "@link"
 * tag elements, for references to field members (and occasionally to method
 * and constructor members).
 * <pre>
 * MemberRef:
 * 		[ Name ] <b>#</b> Identifier
 * </pre>
 *
 * @see Javadoc
 * @since 3.0
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
@SuppressWarnings("rawtypes")
public class MemberRef extends ASTNode implements IDocElement {

	/**
	 * The "qualifier" structural property of this node type (child type: {@link Name}).
	 * @since 3.0
	 */
	public static final ChildPropertyDescriptor QUALIFIER_PROPERTY =
		new ChildPropertyDescriptor(MemberRef.class, "qualifier", Name.class, OPTIONAL, NO_CYCLE_RISK); //$NON-NLS-1$

	/**
	 * The "name" structural property of this node type (child type: {@link SimpleName}).
	 * @since 3.0
	 */
	public static final ChildPropertyDescriptor NAME_PROPERTY =
		new ChildPropertyDescriptor(MemberRef.class, "name", SimpleName.class, MANDATORY, NO_CYCLE_RISK); //$NON-NLS-1$

	/**
	 * A list of property descriptors (element type:
	 * {@link StructuralPropertyDescriptor}),
	 * or null if uninitialized.
	 */
	private static final List PROPERTY_DESCRIPTORS;

	static {
		List propertyList = new ArrayList(3);
		createPropertyList(MemberRef.class, propertyList);
		addProperty(QUALIFIER_PROPERTY, propertyList);
		addProperty(NAME_PROPERTY, propertyList);
		PROPERTY_DESCRIPTORS = reapPropertyList(propertyList);
	}

	/**
	 * Returns a list of structural property descriptors for this node type.
	 * Clients must not modify the result.
	 *
	 * @param apiLevel the API level; one of the AST.JLS* constants
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
	 * The member name; lazily initialized; defaults to a unspecified,
	 * legal Java method name.
	 */
	private SimpleName memberName = null;

	/**
	 * Creates a new AST node for a member reference owned by the given
	 * AST. By default, the method reference is for a member with an
	 * unspecified, but legal, name; and no qualifier.
	 * <p>
	 * N.B. This constructor is package-private; all subclasses must be
	 * declared in the same package; clients are unable to declare
	 * additional subclasses.
	 * </p>
	 *
	 * @param ast the AST that is to own this node
	 */
	MemberRef(AST ast) {
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
		return MEMBER_REF;
	}

	@Override
	ASTNode clone0(AST target) {
		MemberRef result = new MemberRef(target);
		result.setSourceRange(getStartPosition(), getLength());
		result.setQualifier((Name) ASTNode.copySubtree(target, getQualifier()));
		result.setName((SimpleName) ASTNode.copySubtree(target, getName()));
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
	 * Returns the qualifier of this member reference, or
	 * <code>null</code> if there is none.
	 *
	 * @return the qualifier name node, or <code>null</code> if there is none
	 */
	public Name getQualifier() {
		return this.optionalQualifier;
	}

	/**
	 * Sets or clears the qualifier of this member reference.
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
	 * Returns the name of the referenced member.
	 *
	 * @return the member name node
	 */
	public SimpleName getName() {
		if (this.memberName == null) {
			// lazy init must be thread-safe for readers
			synchronized (this) {
				if (this.memberName == null) {
					preLazyInit();
					this.memberName = new SimpleName(this.ast);
					postLazyInit(this.memberName, NAME_PROPERTY);
				}
			}
		}
		return this.memberName;
	}

	/**
	 * Sets the name of the referenced member to the given name.
	 *
	 * @param name the new member name node
	 * @exception IllegalArgumentException if:
	 * <ul>
	 * <li>the name is <code>null</code></li>
	 * <li>the node belongs to a different AST</li>
	 * <li>the node already has a parent</li>
	 * </ul>
	 */
	public void setName(SimpleName name) {
		if (name == null) {
			throw new IllegalArgumentException();
		}
		ASTNode oldChild = this.memberName;
		preReplaceChild(oldChild, name, NAME_PROPERTY);
		this.memberName = name;
		postReplaceChild(oldChild, name, NAME_PROPERTY);
	}

	/**
	 * Resolves and returns the binding for the entity referred to by
	 * this member reference.
	 * <p>
	 * Note that bindings are generally unavailable unless requested when the
	 * AST is being built.
	 * </p>
	 *
	 * @return the binding, or <code>null</code> if the binding cannot be
	 *    resolved
	 */
	public final IBinding resolveBinding() {
		return this.ast.getBindingResolver().resolveReference(this);
	}

	@Override
	int memSize() {
		return BASE_NODE_SIZE + 2 * 4;
	}

	@Override
	int treeSize() {
		return
			memSize()
			+ (this.optionalQualifier == null ? 0 : getQualifier().treeSize())
			+ (this.memberName == null ? 0 : getName().treeSize());
	}
}

