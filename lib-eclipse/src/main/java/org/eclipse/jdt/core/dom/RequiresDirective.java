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
import java.util.Iterator;
import java.util.List;

/**
 * Requires directive AST node type (added in JLS9 API).
 * <pre>
 * RequiresDirective:
 *     <b>requires</b> { ModuleModifier } Name <b>;</b>
 * </pre>
 *
 * @since 3.14
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @noextend This class is not intended to be subclassed by clients.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class RequiresDirective extends ModuleDirective {

	/**
	 * The "modifiers" structural property of this node type (element type: {@link ModuleModifier}).
	 */
	public static final ChildListPropertyDescriptor MODIFIERS_PROPERTY =
		new ChildListPropertyDescriptor(RequiresDirective.class, "modifiers", ModuleModifier.class, NO_CYCLE_RISK); //$NON-NLS-1$

	/**
	 * The module structural property of this node type (child type: {@link Name}).
	 */
	public static final ChildPropertyDescriptor NAME_PROPERTY =
		new ChildPropertyDescriptor(RequiresDirective.class, "name", Name.class, OPTIONAL, NO_CYCLE_RISK); //$NON-NLS-1$

	/**
	 * A list of property descriptors (element type:
	 * {@link StructuralPropertyDescriptor}),
	 * or null if uninitialized.
	 */
	private static final List PROPERTY_DESCRIPTORS_9_0;

	static {
		List propertyList = new ArrayList(3);
		createPropertyList(RequiresDirective.class, propertyList);
		addProperty(MODIFIERS_PROPERTY, propertyList);
		addProperty(NAME_PROPERTY, propertyList);
		PROPERTY_DESCRIPTORS_9_0 = reapPropertyList(propertyList);
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
	 * The extended modifiers (element type: {@link ModuleModifier}).
	 * defaults to an empty list
	 */
	private ASTNode.NodeList modifiers = new ASTNode.NodeList(MODIFIERS_PROPERTY);

	/**
	 * The referenced module name; lazily initialized; defaults to a unspecified,
	 * legal Java identifier.
	 */
	private Name name = null;

	/**
	 * Creates a new AST node for an requires directive owned by the
	 * given AST. The requires directive initially is a regular (no modifiers)
	 * requires for an unspecified, but legal, Java module name.
	 * <p>
	 * N.B. This constructor is package-private; all subclasses must be
	 * declared in the same package; clients are unable to declare
	 * additional subclasses.
	 * </p>
	 *
	 * @param ast the AST that is to own this node
	 */
	RequiresDirective(AST ast) {
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
		if (property == MODIFIERS_PROPERTY) {
			return modifiers();
		}

		// allow default implementation to flag the error
		return super.internalGetChildListProperty(property);
	}

	@Override
	final int getNodeType0() {
		return REQUIRES_DIRECTIVE;
	}

	@Override
	ASTNode clone0(AST target) {
		RequiresDirective result = new RequiresDirective(target);
		result.modifiers().addAll(ASTNode.copySubtrees(target, modifiers()));
		result.setName((Name) getName().clone(target));
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
			acceptChildren(visitor, this.modifiers);
			acceptChild(visitor, getName());
		}
		visitor.endVisit(this);
	}

	/**
	 * Returns the live ordered list of modifiers
	 * of this declaration.
	 * <p>
	 * Note that the not all modifiers are legal.
	 * </p>
	 *
	 * @return the live list of module modifiers
	 *    (element type: {@link ModuleModifier})
	 */
	public List modifiers() {
		return this.modifiers;
	}

	/**
	 * Returns the module modifiers explicitly specified on this declaration.
	 * <p>
	 * This method is a convenience method that
	 * computes these flags from <code>modifiers()</code>.
	 * </p>
	 *
	 * @return the bit-wise or of <code>ModuleModifier</code> constants
	 * @see ModuleModifier
	 */
	public int getModifiers() {
		// do not cache - performance could be improved by caching computed flags
		// but this would require tracking changes to this.modifiers
		int computedModifierFlags = ModuleModifier.NONE;
		for (Iterator it = modifiers().iterator(); it.hasNext(); ) {
			Object x = it.next();
			if (x instanceof ModuleModifier) {
				computedModifierFlags |= ((ModuleModifier) x).getKeyword().toFlagValue();
			}
		}
		return computedModifierFlags;
	}

	/**
	 * Returns the module name referenced by this declaration.
	 *
	 * @return the module referenced
	 */
	public Name getName()  {
		if (this.name == null) {
			// lazy init must be thread-safe for readers
			synchronized (this) {
				if (this.name == null) {
					preLazyInit();
					this.name =this.ast.newQualifiedName(
							new SimpleName(this.ast), new SimpleName(this.ast));
					postLazyInit(this.name, NAME_PROPERTY);
				}
			}
		}
		return this.name;
	}

	/**
	 * Sets the module name in requires directive to the given name.
	 *
	 * @param name the new module name
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

	@Override
	int memSize() {
		return BASE_NODE_SIZE + 2 * 4;
	}

	@Override
	int treeSize() {
		return
			memSize()
			+ (this.modifiers == null ? 0 : this.modifiers.listSize())
			+ (this.name == null ? 0 : getName().treeSize());
	}

}
