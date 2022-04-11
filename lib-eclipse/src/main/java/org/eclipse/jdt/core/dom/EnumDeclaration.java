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
 * Enum declaration AST node type (added in JLS3 API).
 *
 * <pre>
 * EnumDeclaration:
 *     [ Javadoc ] { ExtendedModifier } <b>enum</b> Identifier
 *         [ <b>implements</b> Type { <b>,</b> Type } ]
 *         <b>{</b>
 *         [ EnumConstantDeclaration { <b>,</b> EnumConstantDeclaration } ] [ <b>,</b> ]
 *         [ <b>;</b> { ClassBodyDeclaration | <b>;</b> } ]
 *         <b>}</b>
 * </pre>
 * The {@link #enumConstants()} list holds the enum constant declarations,
 * while the {@link #bodyDeclarations()} list holds the class body declarations
 * that appear after the semicolon.
 * <p>
 * When a Javadoc comment is present, the source
 * range begins with the first character of the "/**" comment delimiter.
 * When there is no Javadoc comment, the source range begins with the first
 * character of the first modifier or annotation (if present), or the
 * first character of the "enum" keyword (if no
 * modifiers or annotations). The source range extends through the last
 * character of the "}" token following the body declarations.
 * </p>
 *
 * @since 3.1
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class EnumDeclaration extends AbstractTypeDeclaration {

	/**
	 * The "javadoc" structural property of this node type (child type: {@link Javadoc}).
	 */
	public static final ChildPropertyDescriptor JAVADOC_PROPERTY =
		internalJavadocPropertyFactory(EnumDeclaration.class);

	/**
	 * The "modifiers" structural property of this node type (element type: {@link IExtendedModifier}) (added in JLS3 API).
	 */
	public static final ChildListPropertyDescriptor MODIFIERS2_PROPERTY =
		internalModifiers2PropertyFactory(EnumDeclaration.class);

	/**
	 * The "name" structural property of this node type (child type: {@link SimpleName}).
	 */
	public static final ChildPropertyDescriptor NAME_PROPERTY =
		internalNamePropertyFactory(EnumDeclaration.class);

	/**
	 * The "superInterfaceTypes" structural property of this node type (element type: {@link Type}).
	 */
	public static final ChildListPropertyDescriptor SUPER_INTERFACE_TYPES_PROPERTY =
		new ChildListPropertyDescriptor(EnumDeclaration.class, "superInterfaceTypes", Type.class, NO_CYCLE_RISK); //$NON-NLS-1$

	/**
	 * The "enumConstants" structural property of this node type (element type: {@link EnumConstantDeclaration}).
	 */
	public static final ChildListPropertyDescriptor ENUM_CONSTANTS_PROPERTY =
		new ChildListPropertyDescriptor(EnumDeclaration.class, "enumConstants", EnumConstantDeclaration.class, CYCLE_RISK); //$NON-NLS-1$

	/**
	 * The "bodyDeclarations" structural property of this node type (element type: {@link BodyDeclaration}).
	 */
	public static final ChildListPropertyDescriptor BODY_DECLARATIONS_PROPERTY =
		internalBodyDeclarationPropertyFactory(EnumDeclaration.class);

	/**
	 * A list of property descriptors (element type:
	 * {@link StructuralPropertyDescriptor}),
	 * or null if uninitialized.
	 */
	private static final List PROPERTY_DESCRIPTORS;

	static {
		List properyList = new ArrayList(6);
		createPropertyList(EnumDeclaration.class, properyList);
		addProperty(JAVADOC_PROPERTY, properyList);
		addProperty(MODIFIERS2_PROPERTY, properyList);
		addProperty(NAME_PROPERTY, properyList);
		addProperty(SUPER_INTERFACE_TYPES_PROPERTY, properyList);
		addProperty(ENUM_CONSTANTS_PROPERTY, properyList);
		addProperty(BODY_DECLARATIONS_PROPERTY, properyList);
		PROPERTY_DESCRIPTORS = reapPropertyList(properyList);
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
	 * The superinterface types (element type: {@link Type}).
	 * Defaults to an empty list.
	 */
	private ASTNode.NodeList superInterfaceTypes =
		new ASTNode.NodeList(SUPER_INTERFACE_TYPES_PROPERTY);

	/**
	 * The enum constant declarations
	 * (element type: {@link EnumConstantDeclaration}).
	 * Defaults to an empty list.
	 */
	private ASTNode.NodeList enumConstants =
		new ASTNode.NodeList(ENUM_CONSTANTS_PROPERTY);

	/**
	 * Creates a new AST node for an enum declaration owned by the given
	 * AST. By default, the enum declaration has an unspecified, but legal,
	 * name; no modifiers; no javadoc; no superinterfaces;
	 * and empty lists of enum constants and body declarations.
	 * <p>
	 * N.B. This constructor is package-private; all subclasses must be
	 * declared in the same package; clients are unable to declare
	 * additional subclasses.
	 * </p>
	 *
	 * @param ast the AST that is to own this node
	 */
	EnumDeclaration(AST ast) {
		super(ast);
	    unsupportedIn2();
	}

	@Override
	final List internalStructuralPropertiesForType(int apiLevel) {
		return propertyDescriptors(apiLevel);
	}

	@Override
	final ASTNode internalGetSetChildProperty(ChildPropertyDescriptor property, boolean get, ASTNode child) {
		if (property == JAVADOC_PROPERTY) {
			if (get) {
				return getJavadoc();
			} else {
				setJavadoc((Javadoc) child);
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
		if (property == MODIFIERS2_PROPERTY) {
			return modifiers();
		}
		if (property == SUPER_INTERFACE_TYPES_PROPERTY) {
			return superInterfaceTypes();
		}
		if (property == ENUM_CONSTANTS_PROPERTY) {
			return enumConstants();
		}
		if (property == BODY_DECLARATIONS_PROPERTY) {
			return bodyDeclarations();
		}
		// allow default implementation to flag the error
		return super.internalGetChildListProperty(property);
	}

	@Override
	final ChildPropertyDescriptor internalJavadocProperty() {
		return JAVADOC_PROPERTY;
	}

	@Override
	final ChildListPropertyDescriptor internalModifiers2Property() {
		return MODIFIERS2_PROPERTY;
	}

	@Override
	final SimplePropertyDescriptor internalModifiersProperty() {
		// this property will not be asked for (node type did not exist in JLS2)
		return null;
	}

	@Override
	final ChildPropertyDescriptor internalNameProperty() {
		return NAME_PROPERTY;
	}

	@Override
	final ChildListPropertyDescriptor internalBodyDeclarationsProperty() {
		return BODY_DECLARATIONS_PROPERTY;
	}

	@Override
	final int getNodeType0() {
		return ENUM_DECLARATION;
	}

	@Override
	ASTNode clone0(AST target) {
		EnumDeclaration result = new EnumDeclaration(target);
		result.setSourceRange(getStartPosition(), getLength());
		result.setJavadoc(
			(Javadoc) ASTNode.copySubtree(target, getJavadoc()));
		result.modifiers().addAll(ASTNode.copySubtrees(target, modifiers()));
		result.setName((SimpleName) getName().clone(target));
		result.superInterfaceTypes().addAll(
			ASTNode.copySubtrees(target, superInterfaceTypes()));
		result.enumConstants().addAll(
				ASTNode.copySubtrees(target, enumConstants()));
		result.bodyDeclarations().addAll(
			ASTNode.copySubtrees(target, bodyDeclarations()));
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
			acceptChild(visitor, getJavadoc());
			acceptChildren(visitor, this.modifiers);
			acceptChild(visitor, getName());
			acceptChildren(visitor, this.superInterfaceTypes);
			acceptChildren(visitor, this.enumConstants);
			acceptChildren(visitor, this.bodyDeclarations);
		}
		visitor.endVisit(this);
	}

	/**
	 * Returns the live ordered list of superinterfaces of this enum
	 * declaration.
	 *
	 * @return the live list of super interface types
	 *    (element type: {@link Type})
	 */
	public List superInterfaceTypes() {
		return this.superInterfaceTypes;
	}

	/**
	 * Returns the live ordered list of enum constant declarations
	 * of this enum declaration.
	 *
	 * @return the live list of enum constant declarations
	 *    (element type: {@link EnumConstantDeclaration})
	 */
	public List enumConstants() {
		return this.enumConstants;
	}

	@Override
	ITypeBinding internalResolveBinding() {
		return this.ast.getBindingResolver().resolveType(this);
	}

	@Override
	int memSize() {
		return super.memSize() + 2 * 4;
	}

	@Override
	int treeSize() {
		return memSize()
			+ (this.optionalDocComment == null ? 0 : getJavadoc().treeSize())
			+ this.modifiers.listSize()
			+ (this.typeName == null ? 0 : getName().treeSize())
			+ this.superInterfaceTypes.listSize()
			+ this.enumConstants.listSize()
			+ this.bodyDeclarations.listSize();
	}
}

