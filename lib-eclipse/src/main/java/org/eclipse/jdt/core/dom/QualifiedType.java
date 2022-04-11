/*******************************************************************************
 * Copyright (c) 2003, 2014 IBM Corporation and others.
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
 * Type node for a qualified type (added in JLS3 API).
 *
 * <pre>
 * QualifiedType:
 *    Type <b>.</b> { Annotation } SimpleName
 * </pre>
 * <p>
 * Not all node arrangements will represent legal Java constructs. In particular,
 * it is nonsense if the type is an array type or primitive type. The normal use
 * is when the type is a ParameterizedType, an annotated QualifiedType, or a
 * NameQualifiedType.
 * </p>
 * <p>
 * A "."-separated type like "A.B" can be represented in three ways:
 * <pre>
 * 1.    SimpleType       | 2. NameQualifiedType   | 3.  QualifiedType
 *     QualifiedName      | SimpleName  SimpleName | SimpleType  SimpleName
 * SimpleName  SimpleName |     "A"         "B"    | SimpleName      "B"
 *     "A"         "B"    |                        |     "A"
 * </pre>
 * <p>
 * The ASTParser creates the SimpleType form (wrapping a name) if possible. The
 * SimpleType form doesn't support any embedded Annotations nor ParameterizedTypes.
 * The NameQualifiedType form is only available since JLS8 and the
 * QualifiedType form only since JLS3. The NameQualifiedType and QualifiedType forms
 * allow Annotations on the last SimpleName. The QualifiedType form cannot be used if
 * the qualifier represents a package name.
 * </p>
 * <p>
 * The part before the last "." is called the <em>qualifier</em> of a type. If
 * the name after the last "." has annotations or if the qualifier is not a
 * (possibly qualified) name, then the ASTParser creates either a
 * NameQualifiedType or a QualifiedType:
 * </p>
 * <ul>
 * <li>
 * If the qualifier is a (possibly qualified) name, then a NameQualifiedType is
 * created.
 * </li>
 * <li>
 * Otherwise, a QualifiedType is created and its qualifier is built using the
 * same rules.
 * </li>
 * </ul>
 *
 * @see SimpleType
 * @see NameQualifiedType
 *
 * @since 3.1
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
@SuppressWarnings("rawtypes")
public class QualifiedType extends AnnotatableType {
    /**
     * This index represents the position inside a parameterized qualified type.
     */
    int index;

	/**
	 * The "qualifier" structural property of this node type (child type: {@link Type}).
	 */
	public static final ChildPropertyDescriptor QUALIFIER_PROPERTY =
		new ChildPropertyDescriptor(QualifiedType.class, "qualifier", Type.class, MANDATORY, CYCLE_RISK); //$NON-NLS-1$

	/**
	 * The "annotations" structural property of this node type (element type: {@link Annotation}).
	 * @since 3.10
	 */
	public static final ChildListPropertyDescriptor ANNOTATIONS_PROPERTY =
			internalAnnotationsPropertyFactory(QualifiedType.class);

	/**
	 * The "name" structural property of this node type (child type: {@link SimpleName}).
	 */
	public static final ChildPropertyDescriptor NAME_PROPERTY =
		new ChildPropertyDescriptor(QualifiedType.class, "name", SimpleName.class, MANDATORY, NO_CYCLE_RISK); //$NON-NLS-1$

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
		createPropertyList(QualifiedType.class, propertyList);
		addProperty(QUALIFIER_PROPERTY, propertyList);
		addProperty(NAME_PROPERTY, propertyList);
		PROPERTY_DESCRIPTORS = reapPropertyList(propertyList);

		propertyList = new ArrayList(4);
		createPropertyList(QualifiedType.class, propertyList);
		addProperty(QUALIFIER_PROPERTY, propertyList);
		addProperty(ANNOTATIONS_PROPERTY, propertyList);
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
	 * The type node; lazily initialized; defaults to a type with
	 * an unspecified, but legal, simple name.
	 */
	private Type qualifier = null;

	/**
	 * The name being qualified; lazily initialized; defaults to a unspecified,
	 * legal Java identifier.
	 */
	private SimpleName name = null;

	/**
	 * Creates a new unparented node for a qualified type owned by the
	 * given AST. By default, an unspecified, but legal, qualifier and name.
	 * <p>
	 * N.B. This constructor is package-private.
	 * </p>
	 *
	 * @param ast the AST that is to own this node
	 */
	QualifiedType(AST ast) {
		super(ast);
	    unsupportedIn2();
	}

	/* (omit javadoc for this method)
	 * Method declared on AnnotatableType.
	 * @since 3.10
	 */
	@Override
	final ChildListPropertyDescriptor internalAnnotationsProperty() {
		return ANNOTATIONS_PROPERTY;
	}

	@Override
	final List internalStructuralPropertiesForType(int apiLevel) {
		return propertyDescriptors(apiLevel);
	}

	@Override
	final List internalGetChildListProperty(ChildListPropertyDescriptor property) {
		if (property == ANNOTATIONS_PROPERTY) {
			return annotations();
		}
		// allow default implementation to flag the error
		return super.internalGetChildListProperty(property);
	}

	@Override
	final ASTNode internalGetSetChildProperty(ChildPropertyDescriptor property, boolean get, ASTNode child) {
		if (property == QUALIFIER_PROPERTY) {
			if (get) {
				return getQualifier();
			} else {
				setQualifier((Type) child);
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
		return QUALIFIED_TYPE;
	}

	@Override
	ASTNode clone0(AST target) {
		QualifiedType result = new QualifiedType(target);
		result.setSourceRange(getStartPosition(), getLength());
		result.setQualifier((Type) ((ASTNode) getQualifier()).clone(target));
		if (this.ast.apiLevel >= AST.JLS8_INTERNAL) {
			result.annotations().addAll(
					ASTNode.copySubtrees(target, annotations()));
		}
		result.setName((SimpleName) ((ASTNode) getName()).clone(target));
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
			if (this.ast.apiLevel >= AST.JLS8_INTERNAL) {
				acceptChildren(visitor, this.annotations);
			}
			acceptChild(visitor, getName());
		}
		visitor.endVisit(this);
	}

	/**
	 * Returns the qualifier of this qualified type.
	 *
	 * @return the qualifier of this qualified type
	 */
	public Type getQualifier() {
		if (this.qualifier == null) {
			// lazy init must be thread-safe for readers
			synchronized (this) {
				if (this.qualifier == null) {
					preLazyInit();
					this.qualifier = new SimpleType(this.ast);
					postLazyInit(this.qualifier, QUALIFIER_PROPERTY);
				}
			}
		}
		return this.qualifier;
	}

	/**
	 * Sets the qualifier of this qualified type to the given type.
	 *
	 * @param type the new qualifier of this qualified type
	 * @exception IllegalArgumentException if:
	 * <ul>
	 * <li>the node belongs to a different AST</li>
	 * <li>the node already has a parent</li>
	 * </ul>
	 */
	public void setQualifier(Type type) {
		if (type == null) {
			throw new IllegalArgumentException();
		}
		ASTNode oldChild = this.qualifier;
		preReplaceChild(oldChild, type, QUALIFIER_PROPERTY);
		this.qualifier = type;
		postReplaceChild(oldChild, type, QUALIFIER_PROPERTY);
	}

	/**
	 * Returns the name part of this qualified type.
	 *
	 * @return the name being qualified
	 */
	public SimpleName getName() {
		if (this.name == null) {
			// lazy init must be thread-safe for readers
			synchronized (this) {
				if (this.name == null) {
					preLazyInit();
					this.name = new SimpleName(this.ast);
					postLazyInit(this.name, NAME_PROPERTY);
				}
			}
		}
		return this.name;
	}

	/**
	 * Sets the name part of this qualified type to the given simple name.
	 *
	 * @param name the identifier of this qualified name
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
		ASTNode oldChild = this.name;
		preReplaceChild(oldChild, name, NAME_PROPERTY);
		this.name = name;
		postReplaceChild(oldChild, name, NAME_PROPERTY);
	}

	@Override
	int memSize() {
		// treat Code as free
		return BASE_NODE_SIZE + 4 * 4;
	}

	@Override
	int treeSize() {
		return
			memSize()
			+ (this.qualifier == null ? 0 : getQualifier().treeSize())
			+ (this.annotations == null ? 0 : this.annotations.listSize())
			+ (this.name == null ? 0 : getName().treeSize());
	}
}

