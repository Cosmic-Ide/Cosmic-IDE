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

/**
 * Descriptor for a child list property of an AST node.
 * A child list property is one whose value is a list of
 * {@link ASTNode}.
 *
 * @see org.eclipse.jdt.core.dom.ASTNode#getStructuralProperty(StructuralPropertyDescriptor)
 * @since 3.0
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
@SuppressWarnings("rawtypes")
public final class ChildListPropertyDescriptor extends StructuralPropertyDescriptor {

	/**
	 * Element type. For example, for a node type like
	 * CompilationUnit, the "imports" property is ImportDeclaration.class.
	 * <p>
	 * Field is private, but marked package-visible for fast
	 * access from ASTNode.
	 * </p>
	 */
	final Class elementType;

	/**
	 * Indicates whether a cycle is possible.
	 * <p>
	 * Field is private, but marked package-visible for fast
	 * access from ASTNode.
	 * </p>
	 */
	final boolean cycleRisk;

	/**
	 * Creates a new child list property descriptor with the given property id.
	 * Note that this constructor is declared package-private so that
	 * property descriptors can only be created by the AST
	 * implementation.
	 *
	 * @param nodeClass concrete AST node type that owns this property
	 * @param propertyId the property id
	 * @param elementType the element type of this property
	 * @param cycleRisk <code>true</code> if this property is at
	 * risk of cycles, and <code>false</code> if there is no worry about cycles
	 */
	ChildListPropertyDescriptor(Class nodeClass, String propertyId, Class elementType, boolean cycleRisk) {
		super(nodeClass, propertyId);
		if (elementType == null) {
			throw new IllegalArgumentException();
		}
		this.elementType = elementType;
		this.cycleRisk = cycleRisk;
	}

	/**
	 * Returns the element type of this list property.
	 * <p>
	 * For example, for a node type like CompilationUnit,
	 * the "imports" property returns <code>ImportDeclaration.class</code>.
	 * </p>
	 *
	 * @return the element type of the property
	 */
	public final Class getElementType() {
		return this.elementType;
	}

	/**
	 * Returns whether this property is vulnerable to cycles.
	 * <p>
	 * A property is vulnerable to cycles if a node of the owning
	 * type (that is, the type that owns this property) could legally
	 * appear in the AST subtree below this property. For example,
	 * the body property of a
	 * {@link MethodDeclaration} node
	 * admits a body which might include statement that embeds
	 * another {@link MethodDeclaration} node.
	 * On the other hand, the name property of a
	 * MethodDeclaration node admits only names, and thereby excludes
	 * another MethodDeclaration node.
	 * </p>
	 *
	 * @return <code>true</code> if cycles are possible,
	 * and <code>false</code> if cycles are impossible
	 */
	public final boolean cycleRisk() {
		return this.cycleRisk;
	}
}
