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
 * Continue statement AST node type.
 *
 * <pre>
 * ContinueStatement:
 *    <b>continue</b> [ Identifier ] <b>;</b>
 * </pre>
 *
 * @since 2.0
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
@SuppressWarnings("rawtypes")
public class ContinueStatement extends Statement {

	/**
	 * The "label" structural property of this node type (child type: {@link SimpleName}).
	 * @since 3.0
	 */
	public static final ChildPropertyDescriptor LABEL_PROPERTY =
		new ChildPropertyDescriptor(ContinueStatement.class, "label", SimpleName.class, OPTIONAL, NO_CYCLE_RISK); //$NON-NLS-1$

	/**
	 * A list of property descriptors (element type:
	 * {@link StructuralPropertyDescriptor}),
	 * or null if uninitialized.
	 */
	private static final List PROPERTY_DESCRIPTORS;

	static {
		List properyList = new ArrayList(2);
		createPropertyList(ContinueStatement.class, properyList);
		addProperty(LABEL_PROPERTY, properyList);
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
	 * @since 3.0
	 */
	public static List propertyDescriptors(int apiLevel) {
		return PROPERTY_DESCRIPTORS;
	}

	/**
	 * The label, or <code>null</code> if none; none by default.
	 */
	private SimpleName optionalLabel = null;

	/**
	 * Creates a new unparented continue statement node owned by the given
	 * AST. By default, the continue statement has no label.
	 * <p>
	 * N.B. This constructor is package-private.
	 * </p>
	 *
	 * @param ast the AST that is to own this node
	 */
	ContinueStatement(AST ast) {
		super(ast);
	}

	@Override
	final List internalStructuralPropertiesForType(int apiLevel) {
		return propertyDescriptors(apiLevel);
	}

	@Override
	final ASTNode internalGetSetChildProperty(ChildPropertyDescriptor property, boolean get, ASTNode child) {
		if (property == LABEL_PROPERTY) {
			if (get) {
				return getLabel();
			} else {
				setLabel((SimpleName) child);
				return null;
			}
		}
		// allow default implementation to flag the error
		return super.internalGetSetChildProperty(property, get, child);
	}

	@Override
	final int getNodeType0() {
		return CONTINUE_STATEMENT;
	}

	@Override
	ASTNode clone0(AST target) {
		ContinueStatement result = new ContinueStatement(target);
		result.setSourceRange(getStartPosition(), getLength());
		result.copyLeadingComment(this);
		result.setLabel((SimpleName) ASTNode.copySubtree(target, getLabel()));
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
			acceptChild(visitor, getLabel());
		}
		visitor.endVisit(this);
	}

	/**
	 * Returns the label of this continue statement, or <code>null</code> if
	 * there is none.
	 *
	 * @return the label, or <code>null</code> if there is none
	 */
	public SimpleName getLabel() {
		return this.optionalLabel;
	}

	/**
	 * Sets or clears the label of this continue statement.
	 *
	 * @param label the label, or <code>null</code> if
	 *    there is none
	 * @exception IllegalArgumentException if:
	 * <ul>
	 * <li>the node belongs to a different AST</li>
	 * <li>the node already has a parent</li>
	 * </ul>
	 */
	public void setLabel(SimpleName label) {
		ASTNode oldChild = this.optionalLabel;
		preReplaceChild(oldChild, label, LABEL_PROPERTY);
		this.optionalLabel = label;
		postReplaceChild(oldChild, label, LABEL_PROPERTY);
	}

	@Override
	int memSize() {
		return super.memSize() + 1 * 4;
	}

	@Override
	int treeSize() {
		return
			memSize()
			+ (this.optionalLabel == null ? 0 : getLabel().treeSize());
	}
}

