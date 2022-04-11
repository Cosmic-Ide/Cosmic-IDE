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
 * Super method reference AST node type (added in JLS8 API).
 *
 * <pre>
 * SuperMethodReference:
 *     [ ClassName <b>.</b> ] <b>super</b> <b>::</b>
 *         [ <b>&lt;</b> Type { <b>,</b> Type } <b>&gt;</b> ]
 *         Identifier
 * </pre>
 *
 * @since 3.10
 * @noinstantiate This class is not intended to be instantiated by clients
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class SuperMethodReference extends MethodReference {

	/**
	 * The "qualifier" structural property of this node type (child type: {@link Name}).
	 */
	public static final ChildPropertyDescriptor QUALIFIER_PROPERTY =
		new ChildPropertyDescriptor(SuperMethodReference.class, "qualifier", Name.class, OPTIONAL, NO_CYCLE_RISK); //$NON-NLS-1$

	/**
	 * The "typeArguments" structural property of this node type (element type: {@link Type}).
	 */
	public static final ChildListPropertyDescriptor TYPE_ARGUMENTS_PROPERTY =
		internalTypeArgumentsFactory(SuperMethodReference.class);

	/**
	 * The "name" structural property of this node type (child type: {@link SimpleName}).
	 */
	public static final ChildPropertyDescriptor NAME_PROPERTY =
		new ChildPropertyDescriptor(SuperMethodReference.class, "name", SimpleName.class, MANDATORY, NO_CYCLE_RISK); //$NON-NLS-1$

	/**
	 * A list of property descriptors (element type:
	 * {@link StructuralPropertyDescriptor}),
	 * or null if uninitialized.
	 */
	private static final List PROPERTY_DESCRIPTORS_8_0;

	static {
		List propertyList = new ArrayList(4);
		createPropertyList(SuperMethodReference.class, propertyList);
		addProperty(QUALIFIER_PROPERTY, propertyList);
		addProperty(TYPE_ARGUMENTS_PROPERTY, propertyList);
		addProperty(NAME_PROPERTY, propertyList);
		PROPERTY_DESCRIPTORS_8_0 = reapPropertyList(propertyList);
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
		return PROPERTY_DESCRIPTORS_8_0;
	}

	/**
	 * The optional qualifier; <code>null</code> for none; defaults to none.
	 */
	private Name optionalQualifier = null;

	/**
	 * The method name; lazily initialized; defaults to a unspecified,
	 * legal Java method name.
	 */
	private SimpleName methodName = null;
	/**
	 * Creates a new AST  node for a super method reference owned
	 * by the given AST. By default, there is no qualifier.
	 * <p>
	 * N.B. This constructor is package-private; all subclasses must be
	 * declared in the same package; clients are unable to declare
	 * additional subclasses.
	 * </p>
	 *
	 * @param ast the AST that is to own this node
	 */
	SuperMethodReference(AST ast) {
		super(ast);
		unsupportedIn2_3_4();
	}

	@Override
	final ChildListPropertyDescriptor internalTypeArgumentsProperty() {
		return TYPE_ARGUMENTS_PROPERTY;
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
	final List internalGetChildListProperty(ChildListPropertyDescriptor property) {
		if (property == TYPE_ARGUMENTS_PROPERTY) {
			return typeArguments();
		}
		// allow default implementation to flag the error
		return super.internalGetChildListProperty(property);
	}

	@Override
	final int getNodeType0() {
		return SUPER_METHOD_REFERENCE;
	}

	@Override
	ASTNode clone0(AST target) {
		SuperMethodReference result = new SuperMethodReference(target);
		result.setSourceRange(getStartPosition(), getLength());
		result.setName((SimpleName) getName().clone(target));
		result.setQualifier((Name) ASTNode.copySubtree(target, getQualifier()));
		result.typeArguments().addAll(ASTNode.copySubtrees(target, typeArguments()));
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
			acceptChildren(visitor, this.typeArguments);
			acceptChild(visitor, getName());
		}
		visitor.endVisit(this);
	}

	/**
	 * Returns the qualifier of this "super" method reference, or
	 * <code>null</code> if there is none.
	 *
	 * @return the qualifier name node, or <code>null</code> if there is none
	 */
	public Name getQualifier() {
		return this.optionalQualifier;
	}

	/**
	 * Sets the qualifier of this "super" method reference expression.
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
	 * Returns the live ordered list of type arguments of this super method reference.
	 *
	 * @return the live list of type arguments
	 *    (element type: {@link Type})
	 */
	@Override
	public List typeArguments() {
		return this.typeArguments;
	}

	/**
	 * Returns the name of the method referenced in this expression.
	 *
	 * @return the method name node
	 */
	public SimpleName getName() {
		if (this.methodName == null) {
			// lazy init must be thread-safe for readers
			synchronized (this) {
				if (this.methodName == null) {
					preLazyInit();
					this.methodName = new SimpleName(this.ast);
					postLazyInit(this.methodName, NAME_PROPERTY);
				}
			}
		}
		return this.methodName;
	}

	/**
	 * Sets the name of the method referenced in this expression to the
	 * given name.
	 *
	 * @param name the new method name
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
		ASTNode oldChild = this.methodName;
		preReplaceChild(oldChild, name, NAME_PROPERTY);
		this.methodName = name;
		postReplaceChild(oldChild, name, NAME_PROPERTY);
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
			+ (this.optionalQualifier == null ? 0 : getQualifier().treeSize())
			+ (this.typeArguments == null ? 0 : this.typeArguments.listSize())
			+ (this.methodName == null ? 0 : getName().treeSize());
	}
}