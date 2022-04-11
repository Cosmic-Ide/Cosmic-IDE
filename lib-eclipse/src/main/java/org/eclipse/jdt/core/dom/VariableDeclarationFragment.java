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
 * Variable declaration fragment AST node type, used in field declarations,
 * local variable declarations, <code>ForStatement</code> initializers,
 * and <code>LambdaExpression</code> parameters.
 * In contrast to <code>SingleVariableDeclaration</code>, fragments are
 * missing the modifiers and the type; these are either located in the fragment's
 * parent node, or inferred (for lambda parameters).
 *
 * <pre>
 * VariableDeclarationFragment:
 *    Identifier { Dimension } [ <b>=</b> Expression ]
 * </pre>
 * @since 2.0
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
@SuppressWarnings("rawtypes")
public class VariableDeclarationFragment extends VariableDeclaration {

	/**
	 * The "name" structural property of this node type (child type: {@link SimpleName}).
	 * @since 3.0
	 */
	public static final ChildPropertyDescriptor NAME_PROPERTY =
			internalNamePropertyFactory(VariableDeclarationFragment.class);

	/**
	 * The "extraDimensions" structural property of this node type (type: {@link Integer}) (below JLS8 only).
	 *
	 * @since 3.0
	 * @deprecated in JLS8 and later, use {@link VariableDeclarationFragment#EXTRA_DIMENSIONS2_PROPERTY} instead.
	 */
	public static final SimplePropertyDescriptor EXTRA_DIMENSIONS_PROPERTY =
			internalExtraDimensionsPropertyFactory(VariableDeclarationFragment.class);

	/**
	 * The "extraDimensions2" structural property of this node type (element type: {@link Dimension}) (added in JLS8 API).
	 * @since 3.10
	 */
	public static final ChildListPropertyDescriptor EXTRA_DIMENSIONS2_PROPERTY =
			internalExtraDimensions2PropertyFactory(VariableDeclarationFragment.class);

	/**
	 * The "initializer" structural property of this node type (child type: {@link Expression}).
	 * @since 3.0
	 */
	public static final ChildPropertyDescriptor INITIALIZER_PROPERTY =
			internalInitializerPropertyFactory(VariableDeclarationFragment.class);

	/**
	 * A list of property descriptors (element type:
	 * {@link StructuralPropertyDescriptor}),
	 * or null if uninitialized.
	 * @since 3.0
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
		List propertyList = new ArrayList(4);
		createPropertyList(VariableDeclarationFragment.class, propertyList);
		addProperty(NAME_PROPERTY, propertyList);
		addProperty(EXTRA_DIMENSIONS_PROPERTY, propertyList);
		addProperty(INITIALIZER_PROPERTY, propertyList);
		PROPERTY_DESCRIPTORS = reapPropertyList(propertyList);

		propertyList = new ArrayList(4);
		createPropertyList(VariableDeclarationFragment.class, propertyList);
		addProperty(NAME_PROPERTY, propertyList);
		addProperty(EXTRA_DIMENSIONS2_PROPERTY, propertyList);
		addProperty(INITIALIZER_PROPERTY, propertyList);
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
	 * @since 3.0
	 */
	public static List propertyDescriptors(int apiLevel) {
		if (apiLevel >= AST.JLS8_INTERNAL) {
			return PROPERTY_DESCRIPTORS_8_0;
		} else {
			return PROPERTY_DESCRIPTORS;
		}
	}

	/**
	 * Creates a new AST node for a variable declaration fragment owned by the
	 * given AST. By default, the variable declaration has: an unspecified
	 * (but legal) variable name, no initializer, and no extra array dimensions.
	 * <p>
	 * N.B. This constructor is package-private.
	 * </p>
	 *
	 * @param ast the AST that is to own this node
	 */
	VariableDeclarationFragment(AST ast) {
		super(ast);
	}

	@Override
	final ChildPropertyDescriptor internalNameProperty() {
		return NAME_PROPERTY;
	}

	@Override
	final SimplePropertyDescriptor internalExtraDimensionsProperty() {
		return EXTRA_DIMENSIONS_PROPERTY;
	}

	@Override
	final ChildListPropertyDescriptor internalExtraDimensions2Property() {
		return EXTRA_DIMENSIONS2_PROPERTY;
	}

	@Override
	final ChildPropertyDescriptor internalInitializerProperty() {
		return INITIALIZER_PROPERTY;
	}

	@Override
	final List internalStructuralPropertiesForType(int apiLevel) {
		return propertyDescriptors(apiLevel);
	}

	@Override
	final int internalGetSetIntProperty(SimplePropertyDescriptor property, boolean get, int value) {
		if (property == EXTRA_DIMENSIONS_PROPERTY) {
			if (get) {
				return getExtraDimensions();
			} else {
				internalSetExtraDimensions(value);
				return 0;
			}
		}
		// allow default implementation to flag the error
		return super.internalGetSetIntProperty(property, get, value);
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
		if (property == INITIALIZER_PROPERTY) {
			if (get) {
				return getInitializer();
			} else {
				setInitializer((Expression) child);
				return null;
			}
		}
		// allow default implementation to flag the error
		return super.internalGetSetChildProperty(property, get, child);
	}

	@Override
	final List internalGetChildListProperty(ChildListPropertyDescriptor property) {
		if (property == EXTRA_DIMENSIONS2_PROPERTY) {
			return extraDimensions();
		}
		// allow default implementation to flag the error
		return super.internalGetChildListProperty(property);
	}

	@Override
	final int getNodeType0() {
		return VARIABLE_DECLARATION_FRAGMENT;
	}

	@Override
	ASTNode clone0(AST target) {
		VariableDeclarationFragment result = new VariableDeclarationFragment(target);
		result.setSourceRange(getStartPosition(), getLength());
		result.setName((SimpleName) getName().clone(target));
		if (this.ast.apiLevel >= AST.JLS8_INTERNAL) {
			result.extraDimensions().addAll(
					ASTNode.copySubtrees(target, extraDimensions()));
		} else {
			result.internalSetExtraDimensions(getExtraDimensions());
		}
		result.setInitializer(
			(Expression) ASTNode.copySubtree(target, getInitializer()));
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
			if (this.ast.apiLevel >= AST.JLS8_INTERNAL) {
				acceptChildren(visitor, this.extraDimensions);
			}
			acceptChild(visitor, getInitializer());
		}
		visitor.endVisit(this);
	}

	@Override
	int memSize() {
		// treat Operator as free
		return BASE_NODE_SIZE + 4 * 4;
	}

	@Override
	int treeSize() {
		return
			memSize()
			+ (this.variableName == null ? 0 : getName().treeSize())
			+ (this.extraDimensions == null ? 0 : this.extraDimensions.listSize())
			+ (this.optionalInitializer == null ? 0 : getInitializer().treeSize());
	}
}
