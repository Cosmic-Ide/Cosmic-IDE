/*******************************************************************************
 * Copyright (c) 2019, 2021 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.core.dom;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Record declaration AST node type (added in JLS16 API).
 *
 * <pre>
 * RecordDeclaration:
 *     [ Javadoc ] { ExtendedModifier } <b>record</b> Identifier
 *     		[ <b>&lt;</b> TypeParameter <b>&gt;</b> ]
 *     		<b>(</b>
 *          	[ FormalParameter { <b>,</b> FormalParameter } ]
 *        	<b>)</b> { Dimension }
 *     		[ <b>implements</b> Type { <b>,</b> Type } ]
 *         	[ <b>;</b> { RecordBodyDeclaration | <b>;</b> } ]
 * </pre>
 * The {@link #bodyDeclarations()} list holds the class body declarations
 * that appear after the semicolon.
 * <p>
 * When a Javadoc comment is present, the source
 * range begins with the first character of the "/**" comment delimiter.
 * When there is no Javadoc comment, the source range begins with the first
 * character of the first modifier or annotation (if present), or the
 * first character of the "record" keyword (if no
 * modifiers or annotations). The source range extends through the last
 * character of the "}" token following the body declarations.
 * </p>
 *
 * @since 3.26
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RecordDeclaration extends AbstractTypeDeclaration {

	/**
	 * The "javadoc" structural property of this node type (child type: {@link Javadoc}).
	 */
	public static final ChildPropertyDescriptor JAVADOC_PROPERTY =
		internalJavadocPropertyFactory(RecordDeclaration.class);

	/**
	 * The "modifiers" structural property of this node type (element type: {@link IExtendedModifier}).
	 */
	public static final ChildListPropertyDescriptor MODIFIERS2_PROPERTY =
		internalModifiers2PropertyFactory(RecordDeclaration.class);

	/**
	 * The "name" structural property of this node type (child type: {@link SimpleName}).
	 */
	public static final ChildPropertyDescriptor NAME_PROPERTY =
		internalNamePropertyFactory(RecordDeclaration.class);


	/**
	 * The "superInterfaceTypes" structural property of this node type (element type: {@link Type}).
	 */
	public static final ChildListPropertyDescriptor SUPER_INTERFACE_TYPES_PROPERTY =
		new ChildListPropertyDescriptor(RecordDeclaration.class, "superInterfaceTypes", Type.class, NO_CYCLE_RISK); //$NON-NLS-1$

	/**
	 * The "typeParameters" structural property of this node type (element type: {@link TypeParameter}).
	 */
	public static final ChildListPropertyDescriptor TYPE_PARAMETERS_PROPERTY =
		new ChildListPropertyDescriptor(RecordDeclaration.class, "typeParameters", TypeParameter.class, NO_CYCLE_RISK); //$NON-NLS-1$

	/**
	 * The "recordComponents" structural property of this node type (element type: {@link SingleVariableDeclaration}).
	 */
	public static final ChildListPropertyDescriptor RECORD_COMPONENTS_PROPERTY =
		new ChildListPropertyDescriptor(RecordDeclaration.class, "recordComponents", SingleVariableDeclaration.class, NO_CYCLE_RISK); //$NON-NLS-1$

	/**
	 * The "bodyDeclarations" structural property of this node type (element type: {@link BodyDeclaration}).
	 */
	public static final ChildListPropertyDescriptor BODY_DECLARATIONS_PROPERTY =
		internalBodyDeclarationPropertyFactory(RecordDeclaration.class);


	/**
	 * A character index into the original restricted identifier source string,
	 * or <code>-1</code> if no restricted identifier source position information is available
	 * for this node; <code>-1</code> by default.
	 */
	private int restrictedIdentifierStartPosition = -1;

	/**
	 * @param restrictedIdentifierStartPosition
	 * @since 3.26
	 */
	public void setRestrictedIdentifierStartPosition(int restrictedIdentifierStartPosition) {
		if (restrictedIdentifierStartPosition < 0) {
			throw new IllegalArgumentException();
		}
		// restrictedIdentifierStartPosition is not considered a structural property
		// but we protect it nevertheless
		checkModifiable();
		this.restrictedIdentifierStartPosition= restrictedIdentifierStartPosition;
	}

	/**
	 * @return restrictedIdentifierStartPosition
	 * @since 3.26
	 */
	public int getRestrictedIdentifierStartPosition() {
		return this.restrictedIdentifierStartPosition;
	}

	/**
	 * A list of property descriptors (element type:
	 * {@link StructuralPropertyDescriptor}),
	 * or null if uninitialized.
	 */
	private static final List PROPERTY_DESCRIPTORS;

	static {

		ArrayList propertyList = new ArrayList(8);
		createPropertyList(RecordDeclaration.class, propertyList);
		addProperty(JAVADOC_PROPERTY, propertyList);
		addProperty(MODIFIERS2_PROPERTY, propertyList);
		addProperty(NAME_PROPERTY, propertyList);
		addProperty(TYPE_PARAMETERS_PROPERTY, propertyList);
		addProperty(RECORD_COMPONENTS_PROPERTY, propertyList);
		addProperty(SUPER_INTERFACE_TYPES_PROPERTY, propertyList);
		addProperty(BODY_DECLARATIONS_PROPERTY, propertyList);

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
	 * @since 3.26
	 */
	public static List propertyDescriptors(int apiLevel) {
		return PROPERTY_DESCRIPTORS;
	}

	/**
	 * The type parameters (element type: {@link TypeParameter}).
	 * defaults to an empty list
	 */
	private ASTNode.NodeList typeParameters = new ASTNode.NodeList(TYPE_PARAMETERS_PROPERTY);


	/**
	 * The superinterface types (element type: {@link Type}).
	 * defaults to an empty list
	 * (see constructor).
	 */
	private ASTNode.NodeList superInterfaceTypes =  new ASTNode.NodeList(SUPER_INTERFACE_TYPES_PROPERTY);

	/**
	 * The parameters (element type: {@link SingleVariableDeclaration}).
	 * defaults to an empty list
	 * (see constructor).
	 */
	private ASTNode.NodeList recordComponents = new ASTNode.NodeList(RECORD_COMPONENTS_PROPERTY);


	/**
	 * Creates a new AST node for a type declaration owned by the given
	 * AST. By default, the type declaration is for a class of an
	 * unspecified, but legal, name; no modifiers; no javadoc;
	 * no type parameters; no superinterfaces; and an empty list
	 * of body declarations.
	 * <p>
	 * N.B. This constructor is package-private; all subclasses must be
	 * declared in the same package; clients are unable to declare
	 * additional subclasses.
	 * </p>
	 *
	 * @param ast the AST that is to own this node
	 * @exception UnsupportedOperationException if this operation is used below JLS16
	 */
	RecordDeclaration(AST ast) {
		super(ast);
		unsupportedBelow16();
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
		if (property == TYPE_PARAMETERS_PROPERTY) {
			return typeParameters();
		}
		if (property == RECORD_COMPONENTS_PROPERTY) {
			return recordComponents();
		}
		if (property == SUPER_INTERFACE_TYPES_PROPERTY) {
			return superInterfaceTypes();
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
	final ChildPropertyDescriptor internalNameProperty() {
		return NAME_PROPERTY;
	}

	@Override
	final ChildListPropertyDescriptor internalBodyDeclarationsProperty() {
		return BODY_DECLARATIONS_PROPERTY;
	}

	@Override
	final int getNodeType0() {
		return RECORD_DECLARATION;
	}

	@Override
	ASTNode clone0(AST target) {
		RecordDeclaration result = new RecordDeclaration(target);
		result.restrictedIdentifierStartPosition = getRestrictedIdentifierStartPosition();
		result.setSourceRange(getStartPosition(), getLength());
		result.setJavadoc(
			(Javadoc) ASTNode.copySubtree(target, getJavadoc()));
		result.setName((SimpleName) getName().clone(target));
		result.modifiers().addAll(ASTNode.copySubtrees(target, modifiers()));
		result.typeParameters().addAll(
				ASTNode.copySubtrees(target, typeParameters()));
		result.recordComponents().addAll(
				ASTNode.copySubtrees(target, recordComponents()));
		result.superInterfaceTypes().addAll(
				ASTNode.copySubtrees(target, superInterfaceTypes()));
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
			acceptChildren(visitor, this.typeParameters);
			acceptChildren(visitor, this.recordComponents);
			acceptChildren(visitor, this.superInterfaceTypes);
			acceptChildren(visitor, this.bodyDeclarations);
		}
		visitor.endVisit(this);
	}

	/**
	 * Returns the live ordered list of type parameters of this type
	 * declaration (added in JLS3 API). This list is non-empty for parameterized types.
	 *
	 * @return the live list of type parameters
	 *    (element type: {@link TypeParameter})
	 * @since 3.26
	 */
	public List typeParameters() {
		return this.typeParameters;
	}


	/**
	 * Returns the live ordered list of superinterfaces of this type
	 * declaration (added in JLS3 API). For a class declaration, these are the interfaces
	 * that this class implements; for an interface declaration,
	 * these are the interfaces that this interface extends.
	 *
	 * @return the live list of interface types
	 *    (element type: {@link Type})
	 * @since 3.26
	 */
	public List superInterfaceTypes() {
		return this.superInterfaceTypes;
	}

	/**
	 * Returns the live ordered list of recordComponents of record declaration.
	 *
	 * @return the live list of  recordComponents
	 *    (element type: {@link SingleVariableDeclaration})
	 * @since 3.26
	 */
	public List recordComponents() {
		return this.recordComponents;
	}

	/**
	 * Returns the ordered list of field declarations of this type
	 * declaration. For a class declaration, these are the
	 * field declarations; for an interface declaration, these are
	 * the constant declarations.
	 * <p>
	 * This convenience method returns this node's body declarations
	 * with non-fields filtered out. Unlike <code>bodyDeclarations</code>,
	 * this method does not return a live result.
	 * </p>
	 *
	 * @return the (possibly empty) list of field declarations
	 * @since 3.26
	 */
	public FieldDeclaration[] getFields() {
		List bd = bodyDeclarations();
		int fieldCount = 0;
		for (Iterator it = bd.listIterator(); it.hasNext(); ) {
			if (it.next() instanceof FieldDeclaration) {
				fieldCount++;
			}
		}
		FieldDeclaration[] fields = new FieldDeclaration[fieldCount];
		int next = 0;
		for (Iterator it = bd.listIterator(); it.hasNext(); ) {
			Object decl = it.next();
			if (decl instanceof FieldDeclaration) {
				fields[next++] = (FieldDeclaration) decl;
			}
		}
		return fields;
	}

	/**
	 * Returns the ordered list of method declarations of this type
	 * declaration.
	 * <p>
	 * This convenience method returns this node's body declarations
	 * with non-methods filtered out. Unlike <code>bodyDeclarations</code>,
	 * this method does not return a live result.
	 * </p>
	 *
	 * @return the (possibly empty) list of method (and constructor)
	 *    declarations
	 * @since 3.26
	 */
	public MethodDeclaration[] getMethods() {
		List bd = bodyDeclarations();
		int methodCount = 0;
		for (Iterator it = bd.listIterator(); it.hasNext(); ) {
			if (it.next() instanceof MethodDeclaration) {
				methodCount++;
			}
		}
		MethodDeclaration[] methods = new MethodDeclaration[methodCount];
		int next = 0;
		for (Iterator it = bd.listIterator(); it.hasNext(); ) {
			Object decl = it.next();
			if (decl instanceof MethodDeclaration) {
				methods[next++] = (MethodDeclaration) decl;
			}
		}
		return methods;
	}

	@Override
	ITypeBinding internalResolveBinding() {
		return this.ast.getBindingResolver().resolveType(this);
	}

	@Override
	int memSize() {
		return super.memSize() + 4 * 4;
	}

	@Override
	int treeSize() {
		return memSize()
			+ (this.optionalDocComment == null ? 0 : getJavadoc().treeSize())
			+ (this.modifiers == null ? 0 : this.modifiers.listSize())
			+ (this.typeName == null ? 0 : getName().treeSize())
			+ (this.typeParameters == null ? 0 : this.typeParameters.listSize())
			+ (this.superInterfaceTypes == null ? 0 : this.superInterfaceTypes.listSize())
			+ (this.recordComponents == null ? 0 : this.recordComponents.listSize())
			+ this.bodyDeclarations.listSize();
	}

	@Override
	SimplePropertyDescriptor internalModifiersProperty() {
		// node type does not exist before JLS 16
		return null;
	}

}

