/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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

import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;

/**
 * Type node for a named class type, a named interface type, or a type variable.
 * <pre>
 * SimpleType:
 *    { Annotation } TypeName
 * </pre>
 * <p>
 * This kind of node is used to convert a name ({@link Name}) into a type
 * ({@link Type}) by wrapping it.
 * </p>
 *
 * In JLS8 and later, the SimpleType may have optional annotations.
 * If annotations are present, then the name must be a {@link SimpleName}.
 * Annotated qualified names are represented as {@link QualifiedType} or {@link NameQualifiedType}.
 *
 * @see QualifiedType
 * @see NameQualifiedType
 *
 * @since 2.0
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
@SuppressWarnings("rawtypes")
public class SimpleType extends AnnotatableType {

	/**
	 * The "annotations" structural property of this node type (element type: {@link Annotation}).
	 * @since 3.10
	 */
	public static final ChildListPropertyDescriptor ANNOTATIONS_PROPERTY =
			internalAnnotationsPropertyFactory(SimpleType.class);

	/**
	 * The "name" structural property of this node type (child type: {@link Name}).
	 * @since 3.0
	 */
	public static final ChildPropertyDescriptor NAME_PROPERTY =
		new ChildPropertyDescriptor(SimpleType.class, "name", Name.class, MANDATORY, NO_CYCLE_RISK); //$NON-NLS-1$

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
		List propertyList = new ArrayList(2);
		createPropertyList(SimpleType.class, propertyList);
		addProperty(NAME_PROPERTY, propertyList);
		PROPERTY_DESCRIPTORS = reapPropertyList(propertyList);

		propertyList = new ArrayList(3);
		createPropertyList(SimpleType.class, propertyList);
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
	 * @since 3.0
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
	 * The type name node; lazily initialized; defaults to a type with
	 * an unspecified, but legal, name.
	 */
	private Name typeName = null;

	/**
	 * Creates a new unparented node for a simple type owned by the given AST.
	 * By default, an unspecified, but legal, name.
	 * <p>
	 * N.B. This constructor is package-private.
	 * </p>
	 *
	 * @param ast the AST that is to own this node
	 */
	SimpleType(AST ast) {
		super(ast);
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
	final int getNodeType0() {
		return SIMPLE_TYPE;
	}

	@Override
	ASTNode clone0(AST target) {
		SimpleType result = new SimpleType(target);
		result.setSourceRange(getStartPosition(), getLength());
		if (this.ast.apiLevel >= AST.JLS8_INTERNAL) {
			result.annotations().addAll(
					ASTNode.copySubtrees(target, annotations()));
		}
		result.setName((Name) (getName()).clone(target));
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
				acceptChildren(visitor, this.annotations);
			}
			acceptChild(visitor, getName());
		}
		visitor.endVisit(this);
	}

	/**
	 * Returns the name of this simple type.
	 *
	 * @return the name of this simple type
	 */
	public Name getName() {
		if (this.typeName == null) {
			// lazy init must be thread-safe for readers
			synchronized (this) {
				if (this.typeName == null) {
					preLazyInit();
					this.typeName = new SimpleName(this.ast);
					postLazyInit(this.typeName, NAME_PROPERTY);
				}
			}
		}
		return this.typeName;
	}

	/**
	 * Sets the name of this simple type to the given name.
	 *
	 * @param typeName the new name of this simple type
	 * @exception IllegalArgumentException if:
	 * <ul>
	 * <li>the node belongs to a different AST</li>
	 * <li>the node already has a parent</li>
	 * </ul>
	 */
	public void setName(Name typeName) {
		if (typeName == null) {
			throw new IllegalArgumentException();
		}
		ASTNode oldChild = this.typeName;
		preReplaceChild(oldChild, typeName, NAME_PROPERTY);
		this.typeName = typeName;
		postReplaceChild(oldChild, typeName, NAME_PROPERTY);
	}

	/**
	 * @exception UnsupportedOperationException if this operation is used below JLS10
	 * @since 3.14
	 */
	@Override
	public boolean isVar() {
		unsupportedBelow10();
		if (Long.compare(this.ast.scanner.complianceLevel, ClassFileConstants.JDK10) < 0)
			return false;
		if (this.typeName == null) getName();
		String qName = this.typeName.getFullyQualifiedName();
		return qName != null && qName.equals("var"); //$NON-NLS-1$
	}
	/* (omit javadoc for this method)
	 * Method declared on ASTNode.
	 */
	@Override
	int memSize() {
		// treat Code as free
		return BASE_NODE_SIZE + 2 * 4;
	}

	@Override
	int treeSize() {
		return
			memSize()
			+ (this.annotations == null ? 0 : this.annotations.listSize())
			+ (this.typeName == null ? 0 : getName().treeSize());
	}
}

