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
import java.util.List;

import org.eclipse.jdt.internal.core.dom.util.DOMASTUtil;

/**
 * GuardedPattern pattern AST node type.
 *
 * <pre>
 * GuardedPattern:
 *      Pattern && Expression
 * </pre>
 *
 * @since 3.27
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @noreference This class is not intended to be referenced by clients.
 */

@SuppressWarnings("rawtypes")
public class GuardedPattern extends Pattern{

	GuardedPattern(AST ast) {
		super(ast);
		supportedOnlyIn17();
		unsupportedWithoutPreviewError();
	}

	/**
	 * The "pattern" structural property of this node type (child type: {@link Pattern}). (added in JEP 406).
	 */
	public static final ChildPropertyDescriptor PATTERN_PROPERTY  = internalPatternPropertyFactory(GuardedPattern.class);

	/**
	 * The "expression" structural property of this node type (child type: {@link Expression}). (added in JEP 406).
	 */
	public static final ChildPropertyDescriptor EXPRESSION_PROPERTY  =
			new ChildPropertyDescriptor(GuardedPattern.class, "expression", Expression.class, MANDATORY,  CYCLE_RISK); //$NON-NLS-1$);


	/**
	 * A list of property descriptors (element type:
	 * {@link StructuralPropertyDescriptor}),
	 * or null if uninitialized.
	 */
	private static final List PROPERTY_DESCRIPTORS;

	static {
		List propertyList = new ArrayList(3);
		createPropertyList(GuardedPattern.class, propertyList);
		addProperty(PATTERN_PROPERTY, propertyList);
		addProperty(EXPRESSION_PROPERTY, propertyList);
		PROPERTY_DESCRIPTORS = reapPropertyList(propertyList);
	}

	/**
	 * The pattern; <code>null</code> for none
	 */
	private Pattern pattern = null;

	/**
	 * The expression; <code>null</code> for none; lazily initialized (but
	 * does <b>not</b> default to none).
	 */
	private Expression conditonalExpression = null;



	@Override
	List internalStructuralPropertiesForType(int apiLevel) {
		return propertyDescriptors(apiLevel);
	}

	@Override
	final List internalStructuralPropertiesForType(int apiLevel, boolean previewEnabled) {
		return propertyDescriptors(apiLevel, previewEnabled);
	}
	@Override
	final ASTNode internalGetSetChildProperty(ChildPropertyDescriptor property, boolean get, ASTNode child) {
		if (property == EXPRESSION_PROPERTY) {
			if (get) {
				return getExpression();
			} else {
				setExpression((Expression) child);
				return null;
			}
		} else if (property == PATTERN_PROPERTY) {
			if (get) {
				return getPattern();
			} else {
				setPattern((Pattern)child);
				return null;
			}
		}
		// allow default implementation to flag the error
		return super.internalGetSetChildProperty(property, get, child);
	}

	@Override
	int getNodeType0() {
		return GUARDED_PATTERN;
	}

	@Override
	boolean subtreeMatch0(ASTMatcher matcher, Object other) {
		return matcher.match(this, other);
	}

	@Override
	ASTNode clone0(AST target) {
		GuardedPattern result = new GuardedPattern(target);
		result.setSourceRange(getStartPosition(), getLength());
		result.setPattern((Pattern) getPattern().clone(target));
		result.setExpression((Expression) getExpression().clone(target));
		return result;
	}

	@Override
	void accept0(ASTVisitor visitor) {
		boolean visitChildren = visitor.visit(this);
		if (visitChildren) {
			// visit children in normal left to right reading order
			acceptChild(visitor, getPattern());
			acceptChild(visitor, getExpression());
		}
		visitor.endVisit(this);

	}

	@Override
	int memSize() {
		return BASE_NODE_SIZE + 2 * 4;
	}

	@Override
	int treeSize() {
		return
				memSize()
			+ (this.pattern == null ? 0 : getPattern().treeSize())
			+ (this.conditonalExpression == null ? 0 : getExpression().treeSize());
	}

	@Override
	public List<SingleVariableDeclaration> patternVariables() {
		return null;
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

	/**
	 * Returns the conditional expression of this pattern, or
	 * <code>null</code> if there is none (the "default:" case).
	 *
	 * @return the expression node, or <code>null</code> if there is none
	 */
	public Expression getExpression() {
		supportedOnlyIn17();
		unsupportedWithoutPreviewError();
		return this.conditonalExpression;
	}

	/**
	 * Returns the pattern of this Guarded Pattern, or
	 * <code>empty</code> if there is none.
	 * @return the pattern node
	 * 			(element type: {@link Pattern})
	 * @exception UnsupportedOperationException if this operation is used other than JLS17
	 * @exception UnsupportedOperationException if this expression is used with previewEnabled flag as false
	 * @noreference This method is not intended to be referenced by clients as it is a part of Java preview feature.
	 */
	public Pattern getPattern() {
		supportedOnlyIn17();
		unsupportedWithoutPreviewError();
		return this.pattern;
	}

	/**
	 * Sets the conditional expression of this pattern, or clears it (turns it into
	 * the  "default:" case).
	 *
	 * @param expression the expression node, or <code>null</code> to
	 *    turn it into the  "default:" case
	 * @exception IllegalArgumentException if:
	 * <ul>
	 * <li>the node belongs to a different AST</li>
	 * <li>the node already has a parent</li>
	 * <li>a cycle in would be created</li>
	 * </ul>
	 */
	public void setExpression(Expression expression) {
		supportedOnlyIn17();
		unsupportedWithoutPreviewError();
		ASTNode oldChild = this.conditonalExpression;
		preReplaceChild(oldChild, expression, EXPRESSION_PROPERTY);
		this.conditonalExpression = expression;
		postReplaceChild(oldChild, expression, EXPRESSION_PROPERTY);
	}

	/**
	 * Sets the pattern of this switch case.
	 * @param pattern
	 * @noreference This method is not intended to be referenced by clients.
	 * @exception UnsupportedOperationException if this operation is used not for JLS17
	 * @exception UnsupportedOperationException if this operation is used without previewEnabled
	 */
	public void setPattern(Pattern pattern) {
		supportedOnlyIn17();
		unsupportedWithoutPreviewError();
		ASTNode oldChild = this.pattern;
		preReplaceChild(oldChild, pattern, PATTERN_PROPERTY);
		this.pattern = pattern;
		postReplaceChild(oldChild, pattern, PATTERN_PROPERTY);
	}

}
