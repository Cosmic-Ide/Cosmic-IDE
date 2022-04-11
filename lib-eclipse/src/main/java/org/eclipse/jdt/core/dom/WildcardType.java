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
 * Type node for a wildcard type (added in JLS3 API).
 * <pre>
 * WildcardType:
 *    { Annotation } <b>?</b> [ ( <b>extends</b> | <b>super</b>) Type ]
 * </pre>
 * <p>
 * Not all node arrangements will represent legal Java constructs. In particular,
 * it is nonsense if a wildcard type node appears anywhere other than as an
 * argument of a <code>ParameterizedType</code> node.
 * </p>
 *
 * @since 3.1
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
@SuppressWarnings("rawtypes")
public class WildcardType extends AnnotatableType {

	/**
	 * The "annotations" structural property of this node type (element type: {@link Annotation}).
	 * @since 3.10
	 */
	public static final ChildListPropertyDescriptor ANNOTATIONS_PROPERTY =
			internalAnnotationsPropertyFactory(WildcardType.class);

	/**
	 * The "bound" structural property of this node type (child type: {@link Type}).
	 */
	public static final ChildPropertyDescriptor BOUND_PROPERTY =
		new ChildPropertyDescriptor(WildcardType.class, "bound", Type.class, OPTIONAL, CYCLE_RISK); //$NON-NLS-1$

	/**
	 * The "upperBound" structural property of this node type (type: {@link Boolean}).
	 */
	public static final SimplePropertyDescriptor UPPER_BOUND_PROPERTY =
		new SimplePropertyDescriptor(WildcardType.class, "upperBound", boolean.class, MANDATORY); //$NON-NLS-1$

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
		createPropertyList(WildcardType.class, propertyList);
		addProperty(BOUND_PROPERTY, propertyList);
		addProperty(UPPER_BOUND_PROPERTY, propertyList);
		PROPERTY_DESCRIPTORS = reapPropertyList(propertyList);

		propertyList = new ArrayList(4);
		createPropertyList(WildcardType.class, propertyList);
		addProperty(ANNOTATIONS_PROPERTY, propertyList);
		addProperty(BOUND_PROPERTY, propertyList);
		addProperty(UPPER_BOUND_PROPERTY, propertyList);
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
	 * The optional type bound node; <code>null</code> if none;
	 * defaults to none.
	 */
	private Type optionalBound = null;

	/**
	 * Indicates whether the wildcard bound is an upper bound
	 * ("extends") as opposed to a lower bound ("super").
	 * Defaults to <code>true</code> initially.
	 */
	private boolean isUpperBound = true;

	/**
	 * Creates a new unparented node for a wildcard type owned by the
	 * given AST. By default, no upper bound.
	 * <p>
	 * N.B. This constructor is package-private.
	 * </p>
	 *
	 * @param ast the AST that is to own this node
	 */
	WildcardType(AST ast) {
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
	final boolean internalGetSetBooleanProperty(SimplePropertyDescriptor property, boolean get, boolean value) {
		if (property == UPPER_BOUND_PROPERTY) {
			if (get) {
				return isUpperBound();
			} else {
				setUpperBound(value);
				return false;
			}
		}
		// allow default implementation to flag the error
		return super.internalGetSetBooleanProperty(property, get, value);
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
		if (property == BOUND_PROPERTY) {
			if (get) {
				return getBound();
			} else {
				setBound((Type) child);
				return null;
			}
		}
		// allow default implementation to flag the error
		return super.internalGetSetChildProperty(property, get, child);
	}

	@Override
	final int getNodeType0() {
		return WILDCARD_TYPE;
	}

	@Override
	ASTNode clone0(AST target) {
		WildcardType result = new WildcardType(target);
		result.setSourceRange(getStartPosition(), getLength());
		if (this.ast.apiLevel >= AST.JLS8_INTERNAL) {
			result.annotations().addAll(
					ASTNode.copySubtrees(target, annotations()));
		}
		result.setBound((Type) ASTNode.copySubtree(target, getBound()), isUpperBound());
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
			acceptChild(visitor, getBound());
		}
		visitor.endVisit(this);
	}

	/**
	 * Returns whether this wildcard type is an upper bound
	 * ("extends") as opposed to a lower bound ("super").
	 * <p>
	 * Note that this property is irrelevant for wildcards
	 * that do not have a bound.
	 * </p>
	 *
	 * @return <code>true</code> if an upper bound,
	 *    and <code>false</code> if a lower bound
	 * @see #setBound(Type)
	 */
	public boolean isUpperBound() {
		return this.isUpperBound;
	}

	/**
	 * Returns the bound of this wildcard type if it has one.
	 * If {@link #isUpperBound isUpperBound} returns true, this
	 * is an upper bound ("? extends B"); if it returns false, this
	 * is a lower bound ("? super B").
	 *
	 * @return the bound of this wildcard type, or <code>null</code>
	 * if none
	 * @see #setBound(Type)
	 */
	public Type getBound() {
		return this.optionalBound;
	}

	/**
	 * Sets the bound of this wildcard type to the given type and
	 * marks it as an upper or a lower bound. The method is
	 * equivalent to calling <code>setBound(type); setUpperBound(isUpperBound)</code>.
	 *
	 * @param type the new bound of this wildcard type, or <code>null</code>
	 * if none
	 * @param isUpperBound <code>true</code> for an upper bound ("? extends B"),
	 * and <code>false</code> for a lower bound ("? super B")
	 * @exception IllegalArgumentException if:
	 * <ul>
	 * <li>the node belongs to a different AST</li>
	 * <li>the node already has a parent</li>
	 * </ul>
	 * @see #getBound()
	 * @see #isUpperBound()
	 */
	public void setBound(Type type, boolean isUpperBound) {
		setBound(type);
		setUpperBound(isUpperBound);
	}

	/**
	 * Sets the bound of this wildcard type to the given type.
	 *
	 * @param type the new bound of this wildcard type, or <code>null</code>
	 * if none
	 * @exception IllegalArgumentException if:
	 * <ul>
	 * <li>the node belongs to a different AST</li>
	 * <li>the node already has a parent</li>
	 * </ul>
	 * @see #getBound()
	 */
	public void setBound(Type type) {
		ASTNode oldChild = this.optionalBound;
		preReplaceChild(oldChild, type, BOUND_PROPERTY);
		this.optionalBound = type;
		postReplaceChild(oldChild, type, BOUND_PROPERTY);
	}

	/**
	 * Sets whether this wildcard type is an upper bound
	 * ("extends") as opposed to a lower bound ("super").
	 *
	 * @param isUpperBound <code>true</code> if an upper bound,
	 *    and <code>false</code> if a lower bound
	 * @see #isUpperBound()
	 */
	public void setUpperBound(boolean isUpperBound) {
		preValueChange(UPPER_BOUND_PROPERTY);
		this.isUpperBound = isUpperBound;
		postValueChange(UPPER_BOUND_PROPERTY);
	}

	@Override
	int memSize() {
		return BASE_NODE_SIZE + 3 * 4;
	}

	@Override
	int treeSize() {
		return
		memSize()
		+ (this.annotations == null ? 0 : this.annotations.listSize())
		+ (this.optionalBound == null ? 0 : getBound().treeSize());
	}
}

