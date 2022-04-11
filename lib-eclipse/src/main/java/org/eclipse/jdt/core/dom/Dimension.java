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
 * AST node for an array dimension (added in JLS8 API).
 * <p>
 * A dimension, represented as <b>[]</b>, can have type annotations. The dimension node is used for:
 * </p>
 * <ul>
 * <li>the dimensions of an {@link ArrayType}</li>
 * <li>extra dimension in the following node types:
 * {@link SingleVariableDeclaration}, {@link VariableDeclarationFragment}, {@link MethodDeclaration}</li>
 * </ul>
 *
 * <pre>
 * Dimension:
 * 	{ Annotation } <b>[]</b>
 * </pre>
 *
 * @since 3.10
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class Dimension extends ASTNode {

	/**
	 * The "annotations" structural property of this node type (element type: {@link Annotation}).
	 */
	public static final ChildListPropertyDescriptor ANNOTATIONS_PROPERTY =
		new ChildListPropertyDescriptor(Dimension.class, "annotations", Annotation.class, NO_CYCLE_RISK); //$NON-NLS-1$

	/**
	 * A list of property descriptors (element type:
	 * {@link StructuralPropertyDescriptor}),
	 * or null if uninitialized.
	 */
	private static final List PROPERTY_DESCRIPTORS_8_0;

	static {
		List propertyList = new ArrayList(2);
		createPropertyList(Dimension.class, propertyList);
		addProperty(ANNOTATIONS_PROPERTY, propertyList);
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
		return PROPERTY_DESCRIPTORS_8_0;
	}

	/**
	 * The list of annotations for this dimension (element type: {@link Annotation}).
	 * Defaults to an empty list.
	 */
	private ASTNode.NodeList annotations = new ASTNode.NodeList(ANNOTATIONS_PROPERTY);

	/**
	 * Creates a new dimension node (supported only in level JLS8 or above).
	 * <p>
	 * N.B. This constructor is package-private.
	 * </p>
	 *
	 * @param ast the AST that is to own this node
	 * @exception UnsupportedOperationException if this operation is used
	 *            in a JLS2, JLS3 or JLS4 AST
	 */
	Dimension(AST ast) {
		super(ast);
		unsupportedIn2_3_4();
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
	final int getNodeType0() {
		return DIMENSION;
	}

	@Override
	ASTNode clone0(AST target) {
		Dimension result = new Dimension(target);
		result.annotations().addAll(
				ASTNode.copySubtrees(target, annotations()));
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
			acceptChildren(visitor, this.annotations);
		}
		visitor.endVisit(this);
	}

	/**
	 * Returns the live ordered list of annotations for this dimension.
	 *
	 * @return the live list of annotations (element type: {@link Annotation})
	 */
	public List annotations() {
		return this.annotations;
	}

	@Override
	int memSize() {
		return BASE_NODE_SIZE + 1 * 4;
	}

	@Override
	int treeSize() {
		return
			memSize()
			+ this.annotations.listSize();
	}
}
