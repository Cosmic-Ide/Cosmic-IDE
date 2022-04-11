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
 * Enumeration constant declaration AST node type (added in JLS3 API).
 *
 * <pre>
 * EnumConstantDeclaration:
 *     [ Javadoc ] { ExtendedModifier } Identifier
 *         [ <b>(</b> [ Expression { <b>,</b> Expression } ] <b>)</b> ]
 *         [ AnonymousClassDeclaration ]
 * </pre>
 * <p>
 * When a Javadoc comment is present, the source
 * range begins with the first character of the "/**" comment delimiter.
 * When there is no Javadoc comment, the source range begins with the first
 * character of the identifier. If there are class body declarations, the
 * source range extends through the last character of the last character of
 * the "}" token following the body declarations. If there are arguments but
 * no class body declarations, the source range extends through the last
 * character of the ")" token following the arguments. If there are no
 * arguments and no class body declarations, the source range extends through
 * the last character of the identifier.
 * </p>
 *
 * @since 3.1
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class EnumConstantDeclaration extends BodyDeclaration {

	/**
	 * The "javadoc" structural property of this node type (child type: {@link Javadoc}).
	 */
	public static final ChildPropertyDescriptor JAVADOC_PROPERTY =
		internalJavadocPropertyFactory(EnumConstantDeclaration.class);

	/**
	 * The "modifiers" structural property of this node type (element type: {@link IExtendedModifier}).
	 */
	public static final ChildListPropertyDescriptor MODIFIERS2_PROPERTY =
		internalModifiers2PropertyFactory(EnumConstantDeclaration.class);

	/**
	 * The "name" structural property of this node type (child type: {@link SimpleName}).
	 */
	public static final ChildPropertyDescriptor NAME_PROPERTY =
		new ChildPropertyDescriptor(EnumConstantDeclaration.class, "name", SimpleName.class, MANDATORY, NO_CYCLE_RISK); //$NON-NLS-1$

	/**
	 * The "arguments" structural property of this node type (element type: {@link Expression}).
	 */
	public static final ChildListPropertyDescriptor ARGUMENTS_PROPERTY =
		new ChildListPropertyDescriptor(EnumConstantDeclaration.class, "arguments", Expression.class, NO_CYCLE_RISK); //$NON-NLS-1$

	/**
	 * The "anonymousClassDeclaration" structural property of this node type (child type: {@link AnonymousClassDeclaration}).
	 */
	public static final ChildPropertyDescriptor ANONYMOUS_CLASS_DECLARATION_PROPERTY =
		new ChildPropertyDescriptor(EnumConstantDeclaration.class, "anonymousClassDeclaration", AnonymousClassDeclaration.class, OPTIONAL, CYCLE_RISK); //$NON-NLS-1$

	/**
	 * A list of property descriptors (element type:
	 * {@link StructuralPropertyDescriptor}),
	 * or null if uninitialized.
	 */
	private static final List PROPERTY_DESCRIPTORS;

	static {
		List properyList = new ArrayList(6);
		createPropertyList(EnumConstantDeclaration.class, properyList);
		addProperty(JAVADOC_PROPERTY, properyList);
		addProperty(MODIFIERS2_PROPERTY, properyList);
		addProperty(NAME_PROPERTY, properyList);
		addProperty(ARGUMENTS_PROPERTY, properyList);
		addProperty(ANONYMOUS_CLASS_DECLARATION_PROPERTY, properyList);
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
	 */
	public static List propertyDescriptors(int apiLevel) {
		return PROPERTY_DESCRIPTORS;
	}

	/**
	 * The constant name; lazily initialized; defaults to a unspecified,
	 * legal Java class identifier.
	 */
	private SimpleName constantName = null;

	/**
	 * The list of argument expressions (element type:
	 * {@link Expression}). Defaults to an empty list.
	 */
	private ASTNode.NodeList arguments =
		new ASTNode.NodeList(ARGUMENTS_PROPERTY);

	/**
	 * The optional anonymous class declaration; <code>null</code> for none;
	 * defaults to none.
	 */
	private AnonymousClassDeclaration optionalAnonymousClassDeclaration = null;

	/**
	 * Creates a new AST node for an enumeration constants declaration owned by
	 * the given AST. By default, the enumeration constant has an unspecified,
	 * but legal, name; no javadoc; an empty list of modifiers and annotations;
	 * an empty list of arguments; and does not declare an anonymous class.
	 * <p>
	 * N.B. This constructor is package-private; all subclasses must be
	 * declared in the same package; clients are unable to declare
	 * additional subclasses.
	 * </p>
	 *
	 * @param ast the AST that is to own this node
	 */
	EnumConstantDeclaration(AST ast) {
		super(ast);
	    unsupportedIn2();
	}

	@Override
	final List internalStructuralPropertiesForType(int apiLevel) {
		return propertyDescriptors(apiLevel);
	}

	@Override
	final ASTNode internalGetSetChildProperty(ChildPropertyDescriptor property, boolean get, ASTNode child) {
		if (property == JAVADOC_PROPERTY) {
			if (get) {
				return getJavadoc();
			} else {
				setJavadoc((Javadoc) child);
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
		if (property == ANONYMOUS_CLASS_DECLARATION_PROPERTY) {
			if (get) {
				return getAnonymousClassDeclaration();
			} else {
				setAnonymousClassDeclaration((AnonymousClassDeclaration) child);
				return null;
			}
		}
		// allow default implementation to flag the error
		return super.internalGetSetChildProperty(property, get, child);
	}

	@Override
	final List internalGetChildListProperty(ChildListPropertyDescriptor property) {
		if (property == MODIFIERS2_PROPERTY) {
			return modifiers();
		}
		if (property == ARGUMENTS_PROPERTY) {
			return arguments();
		}
		// allow default implementation to flag the error
		return super.internalGetChildListProperty(property);
	}

	@Override
	final ChildPropertyDescriptor internalJavadocProperty() {
		return JAVADOC_PROPERTY;
	}

	@Override
	final ChildListPropertyDescriptor internalModifiers2Property() {
		return MODIFIERS2_PROPERTY;
	}

	@Override
	final SimplePropertyDescriptor internalModifiersProperty() {
		// this property will not be asked for (node type did not exist in JLS2)
		return null;
	}

	@Override
	final int getNodeType0() {
		return ENUM_CONSTANT_DECLARATION;
	}

	@Override
	ASTNode clone0(AST target) {
		EnumConstantDeclaration result = new EnumConstantDeclaration(target);
		result.setSourceRange(getStartPosition(), getLength());
		result.setJavadoc(
			(Javadoc) ASTNode.copySubtree(target, getJavadoc()));
		result.modifiers().addAll(ASTNode.copySubtrees(target, modifiers()));
		result.setName((SimpleName) getName().clone(target));
		result.arguments().addAll(ASTNode.copySubtrees(target, arguments()));
		result.setAnonymousClassDeclaration(
				(AnonymousClassDeclaration) ASTNode.copySubtree(target, getAnonymousClassDeclaration()));
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
			acceptChild(visitor, getJavadoc());
			acceptChildren(visitor, this.modifiers);
			acceptChild(visitor, getName());
			acceptChildren(visitor, this.arguments);
			acceptChild(visitor, getAnonymousClassDeclaration());
		}
		visitor.endVisit(this);
	}

	/**
	 * Returns the name of the constant declared in this enum declaration.
	 *
	 * @return the constant name node
	 */
	public SimpleName getName() {
		if (this.constantName == null) {
			// lazy init must be thread-safe for readers
			synchronized (this) {
				if (this.constantName == null) {
					preLazyInit();
					this.constantName = new SimpleName(this.ast);
					postLazyInit(this.constantName, NAME_PROPERTY);
				}
			}
		}
		return this.constantName;
	}

	/**
	 * Sets the name of the constant declared in this enum declaration to the
	 * given name.
	 *
	 * @param constantName the new constant name
	 * @exception IllegalArgumentException if:
	 * <ul>
	 * <li>the node belongs to a different AST</li>
	 * <li>the node already has a parent</li>
	 * </ul>
	 */
	public void setName(SimpleName constantName) {
		if (constantName == null) {
			throw new IllegalArgumentException();
		}
		ASTNode oldChild = this.constantName;
		preReplaceChild(oldChild, constantName, NAME_PROPERTY);
		this.constantName = constantName;
		postReplaceChild(oldChild, constantName, NAME_PROPERTY);
	}

	/**
	 * Returns the live ordered list of argument expressions in this enumeration
	 * constant declaration. Note that an empty list of arguments is equivalent
	 * to not explicitly specifying arguments.
	 *
	 * @return the live list of argument expressions
	 *    (element type: {@link Expression})
	 */
	public List arguments() {
		return this.arguments;
	}

	/**
	 * Returns the anonymous class declaration introduced by this
	 * enum constant declaration, if it has one.
	 *
	 * @return the anonymous class declaration, or <code>null</code> if none
	 */
	public AnonymousClassDeclaration getAnonymousClassDeclaration() {
		return this.optionalAnonymousClassDeclaration;
	}

	/**
	 * Sets whether this enum constant declaration declares
	 * an anonymous class (that is, has class body declarations).
	 *
	 * @param decl the anonymous class declaration, or <code>null</code>
	 *    if none
	 */
	public void setAnonymousClassDeclaration(AnonymousClassDeclaration decl) {
		ASTNode oldChild = this.optionalAnonymousClassDeclaration;
		preReplaceChild(oldChild, decl, ANONYMOUS_CLASS_DECLARATION_PROPERTY);
		this.optionalAnonymousClassDeclaration = decl;
		postReplaceChild(oldChild, decl, ANONYMOUS_CLASS_DECLARATION_PROPERTY);
	}

	/**
	 * Resolves and returns the binding for the constructor invoked by this
	 * enum constant.
	 * <p>
	 * Note that bindings are generally unavailable unless requested when the
	 * AST is being built.
	 * </p>
	 *
	 * @return the constructor binding, or <code>null</code> if the binding
	 *    cannot be resolved
	 */
	public IMethodBinding resolveConstructorBinding() {
		return this.ast.getBindingResolver().resolveConstructor(this);
	}

	/**
	 * Resolves and returns the field binding for this enum constant.
	 * <p>
	 * Note that bindings are generally unavailable unless requested when the
	 * AST is being built.
	 * </p>
	 *
	 * @return the binding, or <code>null</code> if the binding cannot be
	 *    resolved
	 */
	public IVariableBinding resolveVariable() {
		return this.ast.getBindingResolver().resolveVariable(this);
	}

	@Override
	int memSize() {
		return super.memSize() + 3 * 4;
	}

	@Override
	int treeSize() {
		return
			memSize()
			+ (this.optionalDocComment == null ? 0 : getJavadoc().treeSize())
			+ this.modifiers.listSize()
			+ (this.constantName == null ? 0 : getName().treeSize())
			+ this.arguments.listSize()
			+ (this.optionalAnonymousClassDeclaration == null ? 0 : getAnonymousClassDeclaration().treeSize());
	}
}

