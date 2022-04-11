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
 * Annotation type member declaration AST node type (added in JLS3 API).
 * <pre>
 * AnnotationTypeMemberDeclaration:
 *   [ Javadoc ] { ExtendedModifier }
 *       Type Identifier <b>(</b> <b>)</b> [ <b>default</b> Expression ] <b>;</b>
 * </pre>
 * <p>
 * Note that annotation type member declarations are only meaningful as
 * elements of {@link AnnotationTypeDeclaration#bodyDeclarations()}.
 * </p>
 * <p>
 * When a Javadoc comment is present, the source
 * range begins with the first character of the "/**" comment delimiter.
 * When there is no Javadoc comment, the source range begins with the first
 * character of the first modifier keyword (if modifiers),
 * or the first character of the member type (no modifiers).
 * The source range extends through the last character of the
 * ";" token.
 * </p>
 *
 * @since 3.1
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
@SuppressWarnings({"rawtypes"})
public class AnnotationTypeMemberDeclaration extends BodyDeclaration {

	/**
	 * The "javadoc" structural property of this node type (child type: {@link Javadoc}).
	 */
	public static final ChildPropertyDescriptor JAVADOC_PROPERTY =
		internalJavadocPropertyFactory(AnnotationTypeMemberDeclaration.class);

	/**
	 * The "modifiers" structural property of this node type (element type: {@link IExtendedModifier}).
	 */
	public static final ChildListPropertyDescriptor MODIFIERS2_PROPERTY =
		internalModifiers2PropertyFactory(AnnotationTypeMemberDeclaration.class);

	/**
	 * The "type" structural property of this node type (child type: {@link Type}).
	 */
	public static final ChildPropertyDescriptor TYPE_PROPERTY =
			new ChildPropertyDescriptor(AnnotationTypeMemberDeclaration.class, "type", Type.class, MANDATORY, NO_CYCLE_RISK); //$NON-NLS-1$

	/**
	 * The "name" structural property of this node type (child type: {@link SimpleName}).
	 */
	public static final ChildPropertyDescriptor NAME_PROPERTY =
		new ChildPropertyDescriptor(AnnotationTypeMemberDeclaration.class, "name", SimpleName.class, MANDATORY, NO_CYCLE_RISK); //$NON-NLS-1$

	/**
	 * The "default" structural property of this node type (child type: {@link Expression}).
	 */
	public static final ChildPropertyDescriptor DEFAULT_PROPERTY =
		new ChildPropertyDescriptor(AnnotationTypeMemberDeclaration.class, "default", Expression.class, OPTIONAL, CYCLE_RISK); //$NON-NLS-1$

	/**
	 * A list of property descriptors (element type:
	 * {@link StructuralPropertyDescriptor}),
	 * or null if uninitialized.
	 */
	private static final List PROPERTY_DESCRIPTORS;

	static {
		List properyList = new ArrayList(6);
		createPropertyList(AnnotationTypeMemberDeclaration.class, properyList);
		addProperty(JAVADOC_PROPERTY, properyList);
		addProperty(MODIFIERS2_PROPERTY, properyList);
		addProperty(TYPE_PROPERTY, properyList);
		addProperty(NAME_PROPERTY, properyList);
		addProperty(DEFAULT_PROPERTY, properyList);
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
	 * The member type; lazily initialized; defaults to int.
	 */
	private Type memberType = null;

	/**
	 * The member name; lazily initialized; defaults to an unspecified,
	 * legal Java identifier.
	 */
	private SimpleName memberName = null;

	/**
	 * The optional default expression; <code>null</code> for none; defaults to none.
	 */
	private Expression optionalDefaultValue = null;

	/**
	 * Creates a new AST node for an annotation type member declaration owned
	 * by the given AST. By default, the declaration is for a member of an
	 * unspecified, but legal, name; no modifiers; no javadoc;
	 * an unspecified value type; and no default value.
	 * <p>
	 * N.B. This constructor is package-private; all subclasses must be
	 * declared in the same package; clients are unable to declare
	 * additional subclasses.
	 * </p>
	 *
	 * @param ast the AST that is to own this node
	 */
	AnnotationTypeMemberDeclaration(AST ast) {
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
		if (property == TYPE_PROPERTY) {
			if (get) {
				return getType();
			} else {
				setType((Type) child);
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
		if (property == DEFAULT_PROPERTY) {
			if (get) {
				return getDefault();
			} else {
				setDefault((Expression) child);
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
	final int getNodeType0() {
		return ANNOTATION_TYPE_MEMBER_DECLARATION;
	}

	@Override
	ASTNode clone0(AST target) {
		AnnotationTypeMemberDeclaration result = new AnnotationTypeMemberDeclaration(target);
		result.setSourceRange(getStartPosition(), getLength());
		result.setJavadoc(
			(Javadoc) ASTNode.copySubtree(target, getJavadoc()));
		result.modifiers().addAll(ASTNode.copySubtrees(target, modifiers()));
		result.setType((Type) ASTNode.copySubtree(target, getType()));
		result.setName((SimpleName) getName().clone(target));
		result.setDefault((Expression) ASTNode.copySubtree(target, getDefault()));
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
			acceptChild(visitor, getType());
			acceptChild(visitor, getName());
			acceptChild(visitor, getDefault());
		}
		visitor.endVisit(this);
	}

	/**
	 * Returns the type of the annotation type member declared in this
	 * declaration.
	 *
	 * @return the type of the member
	 */
	public Type getType() {
		if (this.memberType == null) {
			// lazy init must be thread-safe for readers
			synchronized (this) {
				if (this.memberType == null) {
					preLazyInit();
					this.memberType = this.ast.newPrimitiveType(PrimitiveType.INT);
					postLazyInit(this.memberType, TYPE_PROPERTY);
				}
			}
		}
		return this.memberType;
	}

	/**
	 * Sets the type of the annotation type member declared in this declaration
	 * to the given type.
	 *
	 * @param type the new member type
	 * @exception IllegalArgumentException if:
	 * <ul>
	 * <li>the node belongs to a different AST</li>
	 * <li>the node already has a parent</li>
	 * </ul>
	 */
	public void setType(Type type) {
		if (type == null) {
			throw new IllegalArgumentException();
		}
		ASTNode oldChild = this.memberType;
		preReplaceChild(oldChild, type, TYPE_PROPERTY);
		this.memberType = type;
		postReplaceChild(oldChild, type, TYPE_PROPERTY);
	}

	/**
	 * Returns the name of the annotation type member declared in this declaration.
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
	 * Sets the name of the annotation type member declared in this declaration to the
	 * given name.
	 *
	 * @param memberName the new member name
	 * @exception IllegalArgumentException if:
	 * <ul>
	 * <li>the node belongs to a different AST</li>
	 * <li>the node already has a parent</li>
	 * </ul>
	 */
	public void setName(SimpleName memberName) {
		if (memberName == null) {
			throw new IllegalArgumentException();
		}
		ASTNode oldChild = this.memberName;
		preReplaceChild(oldChild, memberName, NAME_PROPERTY);
		this.memberName = memberName;
		postReplaceChild(oldChild, memberName, NAME_PROPERTY);
	}

	/**
	 * Returns the default value of this annotation type member, or
	 * <code>null</code> if there is none.
	 *
	 * @return the expression node, or <code>null</code> if there is none
	 */
	public Expression getDefault() {
		return this.optionalDefaultValue;
	}

	/**
	 * Sets or clears the default value of this annotation type member.
	 *
	 * @param defaultValue the expression node, or <code>null</code> if
	 *    there is none
	 * @exception IllegalArgumentException if:
	 * <ul>
	 * <li>the node belongs to a different AST</li>
	 * <li>the node already has a parent</li>
	 * <li>a cycle in would be created</li>
	 * </ul>
	 */
	public void setDefault(Expression defaultValue) {
		// a AnnotationTypeMemberDeclaration may occur inside an Expression - must check cycles
		ASTNode oldChild = this.optionalDefaultValue;
		preReplaceChild(oldChild, defaultValue, DEFAULT_PROPERTY);
		this.optionalDefaultValue = defaultValue;
		postReplaceChild(oldChild, defaultValue, DEFAULT_PROPERTY);
	}

	/**
	 * Resolves and returns the binding for the annotation type member declared
	 * in this declaration.
	 * <p>
	 * Note that bindings are generally unavailable unless requested when the
	 * AST is being built.
	 * </p>
	 *
	 * @return the binding, or <code>null</code> if the binding cannot be
	 *    resolved
	 */
	public IMethodBinding resolveBinding() {
		return this.ast.getBindingResolver().resolveMember(this);
	}

	@Override
	int memSize() {
		return super.memSize() + 3 * 4;
	}

	@Override
	int treeSize() {
		return
			memSize()
			+ (this.optionalDocComment == null ? 0 : getJavadoc().treeSize())
			+ this.modifiers.listSize()
			+ (this.memberName == null ? 0 : getName().treeSize())
			+ (this.memberType == null ? 0 : getType().treeSize())
			+ (this.optionalDefaultValue == null ? 0 : getDefault().treeSize());
	}
}

