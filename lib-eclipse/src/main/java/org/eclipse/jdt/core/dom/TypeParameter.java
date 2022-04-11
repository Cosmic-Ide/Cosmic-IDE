/*******************************************************************************
 * Copyright (c) 2003, 2019 IBM Corporation and others.
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
 * Type parameter declaration node (added in JLS3 API).
 *
 * <pre>
 * TypeParameter:
 *    { ExtendedModifier } Identifier [ <b>extends</b> Type { <b>&amp;</b> Type } ]
 * </pre>
 *
 * @since 3.1
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class TypeParameter extends ASTNode {

	/**
	 * The "modifiers" structural property of this node type (element type: {@link IExtendedModifier}) (added in JLS8 API).
	 * @since 3.10
	 */
	public static final ChildListPropertyDescriptor MODIFIERS_PROPERTY =
			new ChildListPropertyDescriptor(TypeParameter.class, "modifiers", IExtendedModifier.class, CYCLE_RISK); //$NON-NLS-1$

	/**
	 * The "name" structural property of this node type (child type: {@link SimpleName}).
	 */
	public static final ChildPropertyDescriptor NAME_PROPERTY =
		new ChildPropertyDescriptor(TypeParameter.class, "name", SimpleName.class, MANDATORY, NO_CYCLE_RISK); //$NON-NLS-1$

	/**
	 * The "typeBounds" structural property of this node type (element type: {@link Type}).
	 */
	public static final ChildListPropertyDescriptor TYPE_BOUNDS_PROPERTY =
		new ChildListPropertyDescriptor(TypeParameter.class, "typeBounds", Type.class, NO_CYCLE_RISK); //$NON-NLS-1$

	/**
	 * A list of property descriptors (element type:
	 * {@link StructuralPropertyDescriptor}),
	 * or null if uninitialized.
	 */
	private static final List PROPERTY_DESCRIPTORS;
	/**
	 * A list of property descriptors (element type:
	 * {@link StructuralPropertyDescriptor}),
	 * or null if uninitialized.
	 * @since 3.10
	 */
	private static final List PROPERTY_DESCRIPTORS_8_0;

	static {
		List propertyList = new ArrayList(3);
		createPropertyList(TypeParameter.class, propertyList);
		addProperty(NAME_PROPERTY, propertyList);
		addProperty(TYPE_BOUNDS_PROPERTY, propertyList);
		PROPERTY_DESCRIPTORS = reapPropertyList(propertyList);

		propertyList = new ArrayList(4);
		createPropertyList(TypeParameter.class, propertyList);
		addProperty(MODIFIERS_PROPERTY, propertyList);
		addProperty(NAME_PROPERTY, propertyList);
		addProperty(TYPE_BOUNDS_PROPERTY, propertyList);
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
		switch (apiLevel) {
			case AST.JLS2_INTERNAL :
			case AST.JLS3_INTERNAL :
			case AST.JLS4_INTERNAL:
				return PROPERTY_DESCRIPTORS;
			default :
				return PROPERTY_DESCRIPTORS_8_0;
		}
	}

	/**
	 * The type variable node; lazily initialized; defaults to an unspecified,
	 * but legal, name.
	 */
	private SimpleName typeVariableName = null;

	/**
	 * The type bounds (element type: {@link Type}).
	 * Defaults to an empty list.
	 */
	private ASTNode.NodeList typeBounds =
		new ASTNode.NodeList(TYPE_BOUNDS_PROPERTY);

	/**
	 * The modifiers (element type: {@link IExtendedModifier}).
	 * Null in JLS < 8. Added in JLS8; defaults to an empty list
	 * (see constructor).
	 */
	private ASTNode.NodeList modifiers = null;

	/**
	 * Creates a new unparented node for a parameterized type owned by the
	 * given AST. By default, an unspecified, but legal, type variable name,
	 * and no type bounds.
	 * <p>
	 * N.B. This constructor is package-private.
	 * </p>
	 *
	 * @param ast the AST that is to own this node
	 */
	TypeParameter(AST ast) {
		super(ast);
	    unsupportedIn2();
	    if (ast.apiLevel >= AST.JLS8_INTERNAL) {
			this.modifiers = new ASTNode.NodeList(MODIFIERS_PROPERTY);
		}
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
		// allow default implementation to flag the error
		return super.internalGetSetChildProperty(property, get, child);
	}

	@Override
	final List internalGetChildListProperty(ChildListPropertyDescriptor property) {
		if (property == MODIFIERS_PROPERTY) {
			return modifiers();
		}
		if (property == TYPE_BOUNDS_PROPERTY) {
			return typeBounds();
		}
		// allow default implementation to flag the error
		return super.internalGetChildListProperty(property);
	}

	@Override
	final int getNodeType0() {
		return TYPE_PARAMETER;
	}

	@Override
	ASTNode clone0(AST target) {
		TypeParameter result = new TypeParameter(target);
		result.setSourceRange(getStartPosition(), getLength());
		if (this.ast.apiLevel >= AST.JLS8_INTERNAL) {
			result.modifiers().addAll(
					ASTNode.copySubtrees(target, modifiers()));
		}
		result.setName((SimpleName) ((ASTNode) getName()).clone(target));
		result.typeBounds().addAll(
			ASTNode.copySubtrees(target, typeBounds()));
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
			if (this.ast.apiLevel >= AST.JLS8_INTERNAL) {
				acceptChildren(visitor, this.modifiers);
			}
			acceptChild(visitor, getName());
			acceptChildren(visitor, this.typeBounds);
		}
		visitor.endVisit(this);
	}

	/**
	 * Returns the name of the type variable declared in this type parameter.
	 *
	 * @return the name of the type variable
	 */
	public SimpleName getName() {
		if (this.typeVariableName == null) {
			// lazy init must be thread-safe for readers
			synchronized (this) {
				if (this.typeVariableName == null) {
					preLazyInit();
					this.typeVariableName = new SimpleName(this.ast);
					postLazyInit(this.typeVariableName, NAME_PROPERTY);
				}
			}
		}
		return this.typeVariableName;
	}

	/**
	 * Resolves and returns the binding for this type parameter.
	 * <p>
	 * Note that bindings are generally unavailable unless requested when the
	 * AST is being built.
	 * </p>
	 *
	 * @return the binding, or <code>null</code> if the binding cannot be
	 *    resolved
	 */
	public final ITypeBinding resolveBinding() {
		return this.ast.getBindingResolver().resolveTypeParameter(this);
	}

	/**
	 * Sets the name of the type variable of this type parameter to the given
	 * name.
	 *
	 * @param typeName the new name of this type parameter
	 * @exception IllegalArgumentException if:
	 * <ul>
	 * <li>the node belongs to a different AST</li>
	 * <li>the node already has a parent</li>
	 * </ul>
	 */
	public void setName(SimpleName typeName) {
		if (typeName == null) {
			throw new IllegalArgumentException();
		}
		ASTNode oldChild = this.typeVariableName;
		preReplaceChild(oldChild, typeName, NAME_PROPERTY);
		this.typeVariableName = typeName;
		postReplaceChild(oldChild, typeName, NAME_PROPERTY);
	}

	/**
	 * Returns the live ordered list of type bounds of this type parameter.
	 * For the type parameter to be plausible, there can be at most one
	 * class in the list, and it must be first, and the remaining ones must be
	 * interfaces; the list should not contain primitive types (but array types
	 * and parameterized types are allowed).
	 *
	 * @return the live list of type bounds
	 *    (element type: {@link Type})
	 */
	public List typeBounds() {
		return this.typeBounds;
	}

	/**
	 * Returns the live ordered list of modifiers for this TypeParameter node (added in JLS8 API).
	 *
	 * @return the live list of modifiers (element type: {@link IExtendedModifier})
	 * @exception UnsupportedOperationException if this operation is used
	 *            in a JLS2, JLS3 or JLS4 AST
	 * @since 3.10
	 */
	public List modifiers() {
		// more efficient than just calling unsupportedIn2_3_4() to check
		if (this.modifiers == null) {
			unsupportedIn2_3_4();
		}
		return this.modifiers;
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
			+ (this.modifiers == null ? 0 : this.modifiers.listSize())
			+ (this.typeVariableName == null ? 0 : getName().treeSize())
			+ this.typeBounds.listSize();
	}
}

