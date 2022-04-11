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
 * Node for a name-qualified type (added in JLS8 API).
 *
 * <pre>
 * NameQualifiedType:
 *    Name <b>.</b> { Annotation } SimpleName
 * </pre>
 *
 * <p>
 * The qualifier can resolve to a type or to a package.
 * </p>
 * <p>
 * Note that if no annotation is present, then a name-qualified type can
 * also be represented by a SimpleType or a QualifiedType, see the discussion
 * in {@link QualifiedType}.
 * </p>
 *
 * @see SimpleType
 * @see QualifiedType
 *
 * @since 3.10
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
@SuppressWarnings({"rawtypes"})
public class NameQualifiedType extends AnnotatableType {

	/**
	 * The "qualifier" structural property of this node type (child type: {@link Name}).
	 */
	public static final ChildPropertyDescriptor QUALIFIER_PROPERTY =
		new ChildPropertyDescriptor(NameQualifiedType.class, "qualifier", Name.class, MANDATORY, NO_CYCLE_RISK); //$NON-NLS-1$

	/**
	 * The "annotations" structural property of this node type (element type: {@link Annotation}).
	 */
	public static final ChildListPropertyDescriptor ANNOTATIONS_PROPERTY =
			internalAnnotationsPropertyFactory(NameQualifiedType.class);

	/**
	 * The "name" structural property of this node type (child type: {@link SimpleName}).
	 */
	public static final ChildPropertyDescriptor NAME_PROPERTY =
		new ChildPropertyDescriptor(NameQualifiedType.class, "name", SimpleName.class, MANDATORY, NO_CYCLE_RISK); //$NON-NLS-1$

	/**
	 * A list of property descriptors (element type:
	 * {@link StructuralPropertyDescriptor}),
	 * or null if uninitialized.
	 */
	private static final List PROPERTY_DESCRIPTORS;

	static {
		List propertyList = new ArrayList(4);
		createPropertyList(NameQualifiedType.class, propertyList);
		addProperty(QUALIFIER_PROPERTY, propertyList);
		addProperty(ANNOTATIONS_PROPERTY, propertyList);
		addProperty(NAME_PROPERTY, propertyList);
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
	 */
	public static List propertyDescriptors(int apiLevel) {
		return PROPERTY_DESCRIPTORS;
	}

	/**
	 * The qualifier node; lazily initialized; defaults to
	 * an unspecified, but legal, simple name.
	 */
	private Name qualifier = null;

	/**
	 * The name being qualified; lazily initialized; defaults to a unspecified,
	 * legal Java identifier.
	 */
	private SimpleName name = null;

	/**
	 * Creates a new unparented node for a name-qualified type owned by the
	 * given AST. By default, an unspecified, but legal, qualifier and name.
	 * <p>
	 * N.B. This constructor is package-private.
	 * </p>
	 *
	 * @param ast the AST that is to own this node
	 */
	NameQualifiedType(AST ast) {
		super(ast);
	    unsupportedIn2_3_4();
	}

	@Override
	ChildListPropertyDescriptor internalAnnotationsProperty() {
		return ANNOTATIONS_PROPERTY;
	}

	@Override
	List internalStructuralPropertiesForType(int apiLevel) {
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
		return NAME_QUALIFIED_TYPE;
	}

	@Override
	final boolean subtreeMatch0(ASTMatcher matcher, Object other) {
		// dispatch to correct overloaded match method
		return matcher.match(this, other);
	}

	@Override
	ASTNode clone0(AST target) {
		NameQualifiedType result = new NameQualifiedType(target);
		result.setSourceRange(getStartPosition(), getLength());
		result.setQualifier((Name) ((ASTNode) getQualifier()).clone(target));
		result.annotations().addAll(ASTNode.copySubtrees(target, annotations()));
		result.setName((SimpleName) ((ASTNode) getName()).clone(target));
		return result;
	}

	@Override
	void accept0(ASTVisitor visitor) {
		boolean visitChildren = visitor.visit(this);
		if (visitChildren) {
			// visit children in normal left to right reading order
			acceptChild(visitor, getQualifier());
			acceptChildren(visitor, this.annotations);
			acceptChild(visitor, getName());
		}
		visitor.endVisit(this);
	}

	/**
	 * Returns the qualifier of this name-qualified type.
	 *
	 * @return the qualifier of this name-qualified type
	 */
	public Name getQualifier() {
		if (this.qualifier == null) {
			// lazy init must be thread-safe for readers
			synchronized (this) {
				if (this.qualifier == null) {
					preLazyInit();
					this.qualifier = new SimpleName(this.ast);
					postLazyInit(this.qualifier, QUALIFIER_PROPERTY);
				}
			}
		}
		return this.qualifier;
	}

	/**
	 * Sets the qualifier of this name-qualified type to the given name.
	 *
	 * @param name the new qualifier of this name-qualified type
	 * @exception IllegalArgumentException if:
	 * <ul>
	 * <li>the node belongs to a different AST</li>
	 * <li>the node already has a parent</li>
	 * </ul>
	 */
	public void setQualifier(Name name) {
		if (name == null) {
			throw new IllegalArgumentException();
		}
		ASTNode oldChild = this.qualifier;
		preReplaceChild(oldChild, name, QUALIFIER_PROPERTY);
		this.qualifier = name;
		postReplaceChild(oldChild, name, QUALIFIER_PROPERTY);
	}

	/**
	 * Returns the name part of this name-qualified type.
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
	 * Sets the name part of this name-qualified type to the given simple name.
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
		return BASE_NODE_SIZE + 2 * 4;
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
