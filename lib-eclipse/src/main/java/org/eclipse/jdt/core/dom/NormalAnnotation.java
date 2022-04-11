/*******************************************************************************
 * Copyright (c) 2004, 2019 IBM Corporation and others.
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
 * Normal annotation node (added in JLS3 API).
 * <pre>
 * NormalAnnotation:
 *   <b>@</b> TypeName <b>(</b> [ MemberValuePair { <b>,</b> MemberValuePair } ] <b>)</b>
 * </pre>
 *
 * @since 3.1
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public final class NormalAnnotation extends Annotation {

	/**
	 * The "typeName" structural property of this node type (child type: {@link Name}).
	 */
	public static final ChildPropertyDescriptor TYPE_NAME_PROPERTY =
		internalTypeNamePropertyFactory(NormalAnnotation.class);

	/**
	 * The "values" structural property of this node type (element type: {@link MemberValuePair}).
	 */
	public static final ChildListPropertyDescriptor VALUES_PROPERTY =
		new ChildListPropertyDescriptor(NormalAnnotation.class, "values", MemberValuePair.class, CYCLE_RISK); //$NON-NLS-1$

	/**
	 * A list of property descriptors (element type:
	 * {@link StructuralPropertyDescriptor}),
	 * or null if uninitialized.
	 */
	private static final List PROPERTY_DESCRIPTORS;

	static {
		List propertyList = new ArrayList(3);
		createPropertyList(NormalAnnotation.class, propertyList);
		addProperty(TYPE_NAME_PROPERTY, propertyList);
		addProperty(VALUES_PROPERTY, propertyList);
		PROPERTY_DESCRIPTORS = reapPropertyList(propertyList);
	}

	/**
	 * Returns a list of structural property descriptors for this node type.
	 * Clients must not modify the result.
	 *
	 * @param apiLevel the API level; one of the AST.JLS* constants
	 * @return a list of property descriptors (element type:
	 * {@link StructuralPropertyDescriptor})
	 */
	public static List propertyDescriptors(int apiLevel) {
		return PROPERTY_DESCRIPTORS;
	}

	/**
	 * The list of member value pairs (element type:
	 * {@link MemberValuePair}). Defaults to an empty list.
	 */
	private ASTNode.NodeList values =
		new ASTNode.NodeList(VALUES_PROPERTY);

	/**
	 * Creates a new unparented normal annotation node owned
	 * by the given AST.  By default, the annotation has an
	 * unspecified type name and an empty list of member value
	 * pairs.
	 * <p>
	 * N.B. This constructor is package-private.
	 * </p>
	 *
	 * @param ast the AST that is to own this node
	 */
	NormalAnnotation(AST ast) {
		super(ast);
	    unsupportedIn2();
	}

	@Override
	final List internalStructuralPropertiesForType(int apiLevel) {
		return propertyDescriptors(apiLevel);
	}

	@Override
	final ASTNode internalGetSetChildProperty(ChildPropertyDescriptor property, boolean get, ASTNode child) {
		if (property == TYPE_NAME_PROPERTY) {
			if (get) {
				return getTypeName();
			} else {
				setTypeName((Name) child);
				return null;
			}
		}
		// allow default implementation to flag the error
		return super.internalGetSetChildProperty(property, get, child);
	}

	@Override
	final List internalGetChildListProperty(ChildListPropertyDescriptor property) {
		if (property == VALUES_PROPERTY) {
			return values();
		}
		// allow default implementation to flag the error
		return super.internalGetChildListProperty(property);
	}

	@Override
	final ChildPropertyDescriptor internalTypeNameProperty() {
		return TYPE_NAME_PROPERTY;
	}

	@Override
	final int getNodeType0() {
		return NORMAL_ANNOTATION;
	}

	@Override
	ASTNode clone0(AST target) {
		NormalAnnotation result = new NormalAnnotation(target);
		result.setSourceRange(getStartPosition(), getLength());
		result.setTypeName((Name) ASTNode.copySubtree(target, getTypeName()));
		result.values().addAll(ASTNode.copySubtrees(target, values()));
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
			acceptChild(visitor, getTypeName());
			acceptChildren(visitor, this.values);
		}
		visitor.endVisit(this);
	}

	/**
	 * Returns the live list of member value pairs in this annotation.
	 * Adding and removing nodes from this list affects this node
	 * dynamically. All nodes in this list must be
	 * {@link MemberValuePair}s; attempts to add any other
	 * type of node will trigger an exception.
	 *
	 * @return the live list of member value pairs in this
	 *    annotation (element type: {@link MemberValuePair})
	 */
	public List values() {
		return this.values;
	}

	@Override
	int memSize() {
		return super.memSize() + 1 * 4;
	}

	@Override
	int treeSize() {
		return
			memSize()
			+ (this.typeName == null ? 0 : getTypeName().treeSize())
			+ this.values.listSize();
	}
}
