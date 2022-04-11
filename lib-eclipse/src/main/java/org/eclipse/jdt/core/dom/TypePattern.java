/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
import java.util.Arrays;
import java.util.List;

import org.eclipse.jdt.internal.core.dom.util.DOMASTUtil;

/**
 * TypePattern pattern AST node type.
 *
 * <pre>
 * TypePattern:
 *      SingleVariableDeclaration
 * </pre>
 *
 * @since 3.27
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @noreference This class is not intended to be referenced by clients.
 */
@SuppressWarnings("rawtypes")
public class TypePattern extends Pattern {
	/**
	 * The "patternVariable" structural property of this node type (child type: {@link SingleVariableDeclaration}).
	 */
	public static final ChildPropertyDescriptor PATTERN_VARIABLE_PROPERTY =
			new ChildPropertyDescriptor(TypePattern.class, "patternVariable", SingleVariableDeclaration.class, MANDATORY, CYCLE_RISK); //$NON-NLS-1$

	/**
	 * A list of property descriptors (element type:
	 * {@link StructuralPropertyDescriptor}),
	 * or null if uninitialized.
	 */
	private static final List PROPERTY_DESCRIPTORS;

	static {
		List properyList = new ArrayList(3);
		createPropertyList(TypePattern.class, properyList);
		addProperty(PATTERN_VARIABLE_PROPERTY, properyList);
		PROPERTY_DESCRIPTORS = reapPropertyList(properyList);
	}

	@Override
	int getNodeType0() {
		return ASTNode.TYPE_PATTERN;
	}

	TypePattern(AST ast) {
		super(ast);
		supportedOnlyIn17();
		unsupportedWithoutPreviewError();
	}


	/**
	 * The pattern Variable list; <code>empty</code> for none;
	 */
	private SingleVariableDeclaration patternVariable = null;

	/**
	 * Returns a list of structural property descriptors for this node type.
	 * Clients must not modify the result.
	 *
	 * @param apiLevel the API level; one of the
	 * <code>AST.JLS*</code> constants
	 * @return a list of property descriptors (element type:
	 * {@link StructuralPropertyDescriptor})
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public static List propertyDescriptors(int apiLevel) {
		return null;
	}

	/**
	 * Returns a list of structural property descriptors for this node type.
	 * Clients must not modify the result.
	 *
	 * @param apiLevel the API level; one of the
	 * <code>AST.JLS*</code> constants
	 * @param previewEnabled the previewEnabled flag
	 * @return a list of property descriptors (element type:
	 * {@link StructuralPropertyDescriptor})
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public static List propertyDescriptors(int apiLevel, boolean previewEnabled) {
		if (DOMASTUtil.isPatternSupported(apiLevel, previewEnabled)) {
			return PROPERTY_DESCRIPTORS;
		}
		return null;
	}

	@Override
	final List internalStructuralPropertiesForType(int apiLevel) {
		return propertyDescriptors(apiLevel);
	}

	@Override
	final List internalStructuralPropertiesForType(int apiLevel, boolean previewEnabled) {
		return propertyDescriptors(apiLevel, previewEnabled);
	}

	@Override
	final ASTNode internalGetSetChildProperty(ChildPropertyDescriptor property, boolean get, ASTNode child) {
		if (property == PATTERN_VARIABLE_PROPERTY ) {
			return getPatternVariable();
		}
		// allow default implementation to flag the error
		return super.internalGetSetChildProperty(property, get, child);
	}

	@Override
	public List<SingleVariableDeclaration> patternVariables() {
		supportedOnlyIn17();
		unsupportedWithoutPreviewError();
		return new ArrayList<SingleVariableDeclaration>(Arrays.asList(getPatternVariable()));
	}

	/**
	 * Sets the pattern variable.
	 *
	 * @param patternVariable the right operand node
	 * @exception IllegalArgumentException if:
	 * <ul>
	 * <li>the node belongs to a different AST</li>
	 * <li>the node already has a parent</li>
	 * <li>a cycle in would be created</li>
	 * </ul>
	 * @exception UnsupportedOperationException if this operation is used other than JLS17
	 * @exception UnsupportedOperationException if this expression is used with previewEnabled flag as false
	 * @noreference This method is not intended to be referenced by clients as it is a part of Java preview feature.
	 */
	public void setPatternVariable(SingleVariableDeclaration patternVariable) {
		supportedOnlyIn17();
		unsupportedWithoutPreviewError();
		if (patternVariable == null) {
			throw new IllegalArgumentException();
		}
		ASTNode oldChild = this.patternVariable;
		preReplaceChild(oldChild, patternVariable, PATTERN_VARIABLE_PROPERTY);
		this.patternVariable = patternVariable;
		postReplaceChild(oldChild, patternVariable, PATTERN_VARIABLE_PROPERTY);
	}

	/**
	 * Returns the pattern variable of Types Pattern.
	 *
	 * @return the pattern variable
	 * @exception UnsupportedOperationException if this operation is used other than JLS17
	 * @exception UnsupportedOperationException if this expression is used with previewEnabled flag as false
	 * @noreference This method is not intended to be referenced by clients as it is a part of Java preview feature.
	 */
	public SingleVariableDeclaration getPatternVariable() {
		supportedOnlyIn17();
		unsupportedWithoutPreviewError();
		if (this.patternVariable  == null) {
			// lazy init must be thread-safe for readers
			synchronized (this) {
				if (this.patternVariable == null) {
					preLazyInit();
					this.patternVariable= new SingleVariableDeclaration(this.ast);
					postLazyInit(this.patternVariable, PATTERN_VARIABLE_PROPERTY);
				}
			}
		}
		return this.patternVariable;
	}

	@Override
	boolean subtreeMatch0(ASTMatcher matcher, Object other) {
		return matcher.match(this, other);
	}

	@Override
	ASTNode clone0(AST target) {
		TypePattern result = new TypePattern(target);
		result.setSourceRange(getStartPosition(), getLength());
		result.setPatternVariable((SingleVariableDeclaration) getPatternVariable().clone(target));
		return result;
	}

	@Override
	void accept0(ASTVisitor visitor) {
		boolean visitChildren = visitor.visit(this);
		if (visitChildren) {
			// visit children in normal left to right reading order
			acceptChild(visitor, getPatternVariable());
		}
		visitor.endVisit(this);

	}

	@Override
	int memSize() {
		return BASE_NODE_SIZE + 1 * 4;
	}

	@Override
	int treeSize() {
		return
				memSize()
				+ (this.patternVariable == null ? 0 : getPatternVariable().treeSize());
	}


}
