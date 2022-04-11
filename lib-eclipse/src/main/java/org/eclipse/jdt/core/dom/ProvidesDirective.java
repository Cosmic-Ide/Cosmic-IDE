/*******************************************************************************
 * Copyright (c) 2016, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.core.dom;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides directive AST node type (added in JLS9 API).
 * <pre>
 * ProvidesDirective:
 *     <b>provides</b> Name <b>with</b> Name {<b>,</b> Name } <b>;</b>
 * </pre>
 *
 * @since 3.14
 *
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class ProvidesDirective extends ModuleDirective {

	/**
	 * The "name" structural property of this node type (child type: {@link Name}).
	 */
	public static final ChildPropertyDescriptor NAME_PROPERTY =
		new ChildPropertyDescriptor(ProvidesDirective.class, "name", Name.class, MANDATORY, NO_CYCLE_RISK); //$NON-NLS-1$

	/**
	 * The "implementations" structural property of this node type (element type: {@link Name}).
	 */
	public static final ChildListPropertyDescriptor IMPLEMENTATIONS_PROPERTY =
			new ChildListPropertyDescriptor(ProvidesDirective.class, "implementations", Name.class, NO_CYCLE_RISK); //$NON-NLS-1$

	/**
	 * A list of property descriptors (element type:
	 * {@link StructuralPropertyDescriptor}),
	 * or null if uninitialized.
	 */
	private static final List PROPERTY_DESCRIPTORS_9_0;

	static {
		List properyList = new ArrayList(3);
		createPropertyList(ProvidesDirective.class, properyList);
		addProperty(NAME_PROPERTY, properyList);
		addProperty(IMPLEMENTATIONS_PROPERTY, properyList);
		PROPERTY_DESCRIPTORS_9_0 = reapPropertyList(properyList);
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
		return PROPERTY_DESCRIPTORS_9_0;
	}

	/**
	 * The interface name; lazily initialized; defaults to a unspecified,
	 * legal Java identifier.
	 */
	private Name name = null;

	/**
	 * The implementations names
	 * (element type: {@link Name}).
	 * Defaults to an empty list.
	 */
	private ASTNode.NodeList implementations =
		new ASTNode.NodeList(IMPLEMENTATIONS_PROPERTY);

	/**
	 * Creates a new AST node for an provides directive owned by the
	 * given AST. The provides directive initially is
	 * for an unspecified, but legal, Java type name.
	 * <p>
	 * N.B. This constructor is package-private; all subclasses must be
	 * declared in the same package; clients are unable to declare
	 * additional subclasses.
	 * </p>
	 *
	 * @param ast the AST that is to own this node
	 */
	ProvidesDirective(AST ast) {
		super(ast);
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
				setName((Name) child);
				return null;
			}
		}

		// allow default implementation to flag the error
		return super.internalGetSetChildProperty(property, get, child);
	}

	@Override
	final List internalGetChildListProperty(ChildListPropertyDescriptor property) {
		if (property == IMPLEMENTATIONS_PROPERTY) {
			return implementations();
		}
		// allow default implementation to flag the error
		return super.internalGetChildListProperty(property);
	}

	@Override
	final int getNodeType0() {
		return PROVIDES_DIRECTIVE;
	}

	@Override
	ASTNode clone0(AST target) {
		ProvidesDirective result = new ProvidesDirective(target);
		result.setSourceRange(getStartPosition(), getLength());
		result.setName((Name) getName().clone(target));
		result.implementations().addAll(ASTNode.copySubtrees(target, implementations()));
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
			acceptChild(visitor, getName());
			acceptChildren(visitor, this.implementations);
		}
		visitor.endVisit(this);
	}


	/**
	 * Returns the name of the service in this directive.
	 *
	 * @return the services name
	 */
	public Name getName()  {
		if (this.name == null) {
			// lazy init must be thread-safe for readers
			synchronized (this) {
				if (this.name == null) {
					preLazyInit();
					this.name = this.ast.newQualifiedName(
							new SimpleName(this.ast), new SimpleName(this.ast));
					postLazyInit(this.name, NAME_PROPERTY);
				}
			}
		}
		return this.name;
	}

	/**
	 * Sets the name of the service.
	 *
	 * @param name the new service name
	 * @exception IllegalArgumentException if:
	 * <ul>
	 * <li>the node belongs to a different AST</li>
	 * <li>the node already has a parent</li>
	 * </ul>
	 */
	public void setName(Name name) {
		if (name == null) {
			throw new IllegalArgumentException();
		}
		ASTNode oldChild = this.name;
		preReplaceChild(oldChild, name, NAME_PROPERTY);
		this.name = name;
		postReplaceChild(oldChild, name, NAME_PROPERTY);
	}

	/**
	 * Returns the live ordered list of implementations for the interface in this provides directive.
	 *
	 * @return the live list of implementations for the interface
	 *    (element type: {@link Name})
	 */
	public List implementations() {
		return this.implementations;
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
			+ this.implementations.listSize();
	}
}