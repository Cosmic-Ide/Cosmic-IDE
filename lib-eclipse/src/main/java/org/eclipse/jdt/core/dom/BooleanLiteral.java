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
 * Boolean literal node.
 *
 * <pre>
 * BooleanLiteral:
 * 		<b>true</b>
 * 		<b>false</b>
 * </pre>
 *
 * @since 2.0
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
@SuppressWarnings("rawtypes")
public class BooleanLiteral extends Expression {

	/**
	 * The "booleanValue" structural property of this node type (type: {@link Boolean}).
	 * @since 3.0
	 */
	public static final SimplePropertyDescriptor BOOLEAN_VALUE_PROPERTY =
		new SimplePropertyDescriptor(BooleanLiteral.class, "booleanValue", boolean.class, MANDATORY); //$NON-NLS-1$

	/**
	 * A list of property descriptors (element type:
	 * {@link StructuralPropertyDescriptor}),
	 * or null if uninitialized.
	 */
	private static final List PROPERTY_DESCRIPTORS;

	static {
		List properyList = new ArrayList(2);
		createPropertyList(BooleanLiteral.class, properyList);
		addProperty(BOOLEAN_VALUE_PROPERTY, properyList);
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
	 * The boolean; defaults to the literal for <code>false</code>.
	 */
	private boolean value = false;

	/**
	 * Creates a new unparented boolean literal node owned by the given AST.
	 * <p>
	 * N.B. This constructor is package-private.
	 * </p>
	 *
	 * @param ast the AST that is to own this node
	 */
	BooleanLiteral(AST ast) {
		super(ast);
	}

	@Override
	final List internalStructuralPropertiesForType(int apiLevel) {
		return propertyDescriptors(apiLevel);
	}

	@Override
	final boolean internalGetSetBooleanProperty(SimplePropertyDescriptor property, boolean get, boolean newValue) {
		if (property == BOOLEAN_VALUE_PROPERTY) {
			if (get) {
				return booleanValue();
			} else {
				setBooleanValue(newValue);
				return false;
			}
		}
		// allow default implementation to flag the error
		return super.internalGetSetBooleanProperty(property, get, newValue);
	}

	@Override
	final int getNodeType0() {
		return BOOLEAN_LITERAL;
	}

	@Override
	ASTNode clone0(AST target) {
		BooleanLiteral result = new BooleanLiteral(target);
		result.setSourceRange(getStartPosition(), getLength());
		result.setBooleanValue(booleanValue());
		return result;
	}

	@Override
	final boolean subtreeMatch0(ASTMatcher matcher, Object other) {
		// dispatch to correct overloaded match method
		return matcher.match(this, other);
	}

	@Override
	void accept0(ASTVisitor visitor) {
		visitor.visit(this);
		visitor.endVisit(this);
	}

	/**
	 * Returns the boolean value of this boolean literal node.
	 *
	 * @return <code>true</code> for the boolean literal spelled
	 *    <code>"true"</code>, and <code>false</code> for the boolean literal
	 *    spelled <code>"false"</code>.
	 */
	public boolean booleanValue() {
		return this.value;
	}

	/**
	 * Sets the boolean value of this boolean literal node.
	 *
	 * @param value <code>true</code> for the boolean literal spelled
	 *    <code>"true"</code>, and <code>false</code> for the boolean literal
	 *    spelled <code>"false"</code>.
	 */
	public void setBooleanValue(boolean value) {
		preValueChange(BOOLEAN_VALUE_PROPERTY);
		this.value = value;
		postValueChange(BOOLEAN_VALUE_PROPERTY);
	}

	@Override
	int memSize() {
		return BASE_NODE_SIZE + 1 * 4;
	}

	@Override
	int treeSize() {
		return memSize();
	}
}

