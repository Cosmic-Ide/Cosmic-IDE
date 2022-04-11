/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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
 * Class instance creation expression AST node type.
 * <pre>
 * ClassInstanceCreation:
 *        [ Expression <b>.</b> ]
 *            <b>new</b> [ <b>&lt;</b> Type { <b>,</b> Type } <b>&gt;</b> ]
 *            Type <b>(</b> [ Expression { <b>,</b> Expression } ] <b>)</b>
 *            [ AnonymousClassDeclaration ]
 * </pre>
 * <p>
 * Not all node arrangements will represent legal Java constructs. In particular,
 * it is nonsense if the type is a primitive type or an array type (primitive
 * types cannot be instantiated, and array creations must be represented with
 * <code>ArrayCreation</code> nodes). The normal use is when the type is a
 * simple, qualified, or parameterized type.
 * </p>
 * <p>
 * {@link QualifiedType} discusses typical representations of qualified type references.
 * </p>
 *
 * @since 2.0
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class ClassInstanceCreation extends Expression {

	/**
	 * The "typeArguments" structural property of this node type (element type: {@link Type}) (added in JLS3 API).
	 * @since 3.1
	 */
	public static final ChildListPropertyDescriptor TYPE_ARGUMENTS_PROPERTY =
		new ChildListPropertyDescriptor(ClassInstanceCreation.class, "typeArguments", Type.class, NO_CYCLE_RISK); //$NON-NLS-1$

	/**
	 * The "expression" structural property of this node type (child type: {@link Expression}).
	 * @since 3.0
	 */
	public static final ChildPropertyDescriptor EXPRESSION_PROPERTY =
		new ChildPropertyDescriptor(ClassInstanceCreation.class, "expression", Expression.class, OPTIONAL, CYCLE_RISK); //$NON-NLS-1$

	/**
	 * The "name" structural property of this node type (child type: {@link Name}) (JLS2 API only).
	 * @since 3.0
	 * @deprecated In the JLS3 API, this property is replaced by {@link #TYPE_PROPERTY}.
	 */
	public static final ChildPropertyDescriptor NAME_PROPERTY =
		new ChildPropertyDescriptor(ClassInstanceCreation.class, "name", Name.class, MANDATORY, NO_CYCLE_RISK); //$NON-NLS-1$

	/**
	 * The "type" structural property of this node type (child type: {@link Type}) (added in JLS3 API).
	 * @since 3.1
	 */
	public static final ChildPropertyDescriptor TYPE_PROPERTY =
		new ChildPropertyDescriptor(ClassInstanceCreation.class, "type", Type.class, MANDATORY, NO_CYCLE_RISK); //$NON-NLS-1$

	/**
	 * The "arguments" structural property of this node type (element type: {@link Expression}).
	 * @since 3.0
	 */
	public static final ChildListPropertyDescriptor ARGUMENTS_PROPERTY =
		new ChildListPropertyDescriptor(ClassInstanceCreation.class, "arguments", Expression.class, CYCLE_RISK); //$NON-NLS-1$

	/**
	 * The "anonymousClassDeclaration" structural property of this node type (child type: {@link AnonymousClassDeclaration}).
	 * @since 3.0
	 */
	public static final ChildPropertyDescriptor ANONYMOUS_CLASS_DECLARATION_PROPERTY =
		new ChildPropertyDescriptor(ClassInstanceCreation.class, "anonymousClassDeclaration", AnonymousClassDeclaration.class, OPTIONAL, CYCLE_RISK); //$NON-NLS-1$

	/**
	 * A list of property descriptors (element type:
	 * {@link StructuralPropertyDescriptor}),
	 * or null if uninitialized.
	 * @since 3.0
	 */
	private static final List PROPERTY_DESCRIPTORS_2_0;

	/**
	 * A list of property descriptors (element type:
	 * {@link StructuralPropertyDescriptor}),
	 * or null if uninitialized.
	 * @since 3.1
	 */
	private static final List PROPERTY_DESCRIPTORS_3_0;

	static {
		List properyList = new ArrayList(5);
		createPropertyList(ClassInstanceCreation.class, properyList);
		addProperty(EXPRESSION_PROPERTY, properyList);
		addProperty(NAME_PROPERTY, properyList);
		addProperty(ARGUMENTS_PROPERTY, properyList);
		addProperty(ANONYMOUS_CLASS_DECLARATION_PROPERTY, properyList);
		PROPERTY_DESCRIPTORS_2_0 = reapPropertyList(properyList);

		properyList = new ArrayList(6);
		createPropertyList(ClassInstanceCreation.class, properyList);
		addProperty(EXPRESSION_PROPERTY, properyList);
		addProperty(TYPE_ARGUMENTS_PROPERTY, properyList);
		addProperty(TYPE_PROPERTY, properyList);
		addProperty(ARGUMENTS_PROPERTY, properyList);
		addProperty(ANONYMOUS_CLASS_DECLARATION_PROPERTY, properyList);
		PROPERTY_DESCRIPTORS_3_0 = reapPropertyList(properyList);
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
		if (apiLevel == AST.JLS2_INTERNAL) {
			return PROPERTY_DESCRIPTORS_2_0;
		} else {
			return PROPERTY_DESCRIPTORS_3_0;
		}
	}

	/**
	 * The optional expression; <code>null</code> for none; defaults to none.
	 */
	private Expression optionalExpression = null;

	/**
	 * The type arguments (element type: {@link Type}).
	 * Null in JLS2. Added in JLS3; defaults to an empty list
	 * (see constructor).
	 * @since 3.1
	 */
	private ASTNode.NodeList typeArguments = null;

	/**
	 * The type name; lazily initialized; defaults to a unspecified,
	 * legal type name. Not used in JLS3.
	 */
	private Name typeName = null;

	/**
	 * The type; lazily initialized; defaults to a unspecified type.
	 * @since 3.0
	 */
	private Type type = null;

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
	 * Creates a new AST node for a class instance creation expression owned
	 * by the given AST. By default, there is no qualifying expression,
	 * an empty list of type parameters, an unspecified type, an empty
     * list of arguments, and does not declare an anonymous class.
	 * <p>
	 * N.B. This constructor is package-private; all subclasses must be
	 * declared in the same package; clients are unable to declare
	 * additional subclasses.
	 * </p>
	 *
	 * @param ast the AST that is to own this node
	 */
	ClassInstanceCreation (AST ast) {
		super(ast);
		if (ast.apiLevel >= AST.JLS3_INTERNAL) {
			this.typeArguments = new ASTNode.NodeList(TYPE_ARGUMENTS_PROPERTY);
		}
	}

	/* (omit javadoc for this method)
	 * Method declared on ASTNode.
	 * @since 3.0
	 */
	@Override
	final List internalStructuralPropertiesForType(int apiLevel) {
		return propertyDescriptors(apiLevel);
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
		}
		if (property == NAME_PROPERTY) {
			if (get) {
				return getName();
			} else {
				setName((Name) child);
				return null;
			}
		}
		if (property == TYPE_PROPERTY) {
			if (get) {
				return getType();
			} else {
				setType((Type) child);
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
		if (property == ARGUMENTS_PROPERTY) {
			return arguments();
		}
		if (property == TYPE_ARGUMENTS_PROPERTY) {
			return typeArguments();
		}
		// allow default implementation to flag the error
		return super.internalGetChildListProperty(property);
	}

	@Override
	final int getNodeType0() {
		return CLASS_INSTANCE_CREATION;
	}

	@Override
	ASTNode clone0(AST target) {
		ClassInstanceCreation result = new ClassInstanceCreation(target);
		result.setSourceRange(getStartPosition(), getLength());
		result.setExpression(
			(Expression) ASTNode.copySubtree(target, getExpression()));
		if (this.ast.apiLevel == AST.JLS2_INTERNAL) {
			result.setName((Name) getName().clone(target));
		}
		if (this.ast.apiLevel >= AST.JLS3_INTERNAL) {
			result.typeArguments().addAll(ASTNode.copySubtrees(target, typeArguments()));
			result.setType((Type) getType().clone(target));
		}
		result.arguments().addAll(ASTNode.copySubtrees(target, arguments()));
		result.setAnonymousClassDeclaration(
			(AnonymousClassDeclaration)
			   ASTNode.copySubtree(target, getAnonymousClassDeclaration()));
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
			acceptChild(visitor, getExpression());
			if (this.ast.apiLevel == AST.JLS2_INTERNAL) {
				acceptChild(visitor, getName());
			}
			if (this.ast.apiLevel >= AST.JLS3_INTERNAL) {
				acceptChildren(visitor, this.typeArguments);
				acceptChild(visitor, getType());
			}
			acceptChildren(visitor, this.arguments);
			acceptChild(visitor, getAnonymousClassDeclaration());
		}
		visitor.endVisit(this);
	}

	/**
	 * Returns the expression of this class instance creation expression, or
	 * <code>null</code> if there is none.
	 *
	 * @return the expression node, or <code>null</code> if there is none
	 */
	public Expression getExpression() {
		return this.optionalExpression;
	}

	/**
	 * Sets or clears the expression of this class instance creation expression.
	 *
	 * @param expression the expression node, or <code>null</code> if
	 *    there is none
	 * @exception IllegalArgumentException if:
	 * <ul>
	 * <li>the node belongs to a different AST</li>
	 * <li>the node already has a parent</li>
	 * <li>a cycle in would be created</li>
	 * </ul>
	 */
	public void setExpression(Expression expression) {
		// a ClassInstanceCreation may occur inside an Expression
		// must check cycles
		ASTNode oldChild = this.optionalExpression;
		preReplaceChild(oldChild, expression, EXPRESSION_PROPERTY);
		this.optionalExpression = expression;
		postReplaceChild(oldChild, expression, EXPRESSION_PROPERTY);
	}

	/**
	 * Returns the live ordered list of type arguments of this class
	 * instance creation (added in JLS3 API).
	 *
	 * @return the live list of type arguments
	 *    (element type: {@link Type})
	 * @exception UnsupportedOperationException if this operation is used in
	 * a JLS2 AST
	 * @since 3.1
	 */
	public List typeArguments() {
		// more efficient than just calling unsupportedIn2() to check
		if (this.typeArguments == null) {
			unsupportedIn2();
		}
		return this.typeArguments;
	}

    /**
	 * Returns the name of the type instantiated in this class instance
	 * creation expression (JLS2 API only).
	 *
	 * @return the type name node
	 * @exception UnsupportedOperationException if this operation is used in
	 * an AST later than JLS2
	 * @deprecated In the JLS3 API, this method is replaced by
	 * {@link #getType()}, which returns a <code>Type</code> instead of a
	 * <code>Name</code>.
	 */
	public Name getName() {
		return internalGetName();
	}

	/**
	 * Internal synonym for deprecated method. Used to avoid
	 * deprecation warnings.
	 * @since 3.1
	 */
	/*package*/ Name internalGetName() {
	    supportedOnlyIn2();
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
	 * Sets the name of the type instantiated in this class instance
	 * creation expression (JLS2 API only).
	 *
	 * @param name the new type name
	 * @exception IllegalArgumentException if:
	 * <ul>
	 * <li>the node belongs to a different AST</li>
	 * <li>the node already has a parent</li>
	 * </ul>
	 * @exception UnsupportedOperationException if this operation is used in
	 * an AST later than JLS2
	 * @deprecated In the JLS3 API, this method is replaced by
	 * {@link #setType(Type)}, which expects a <code>Type</code> instead of
	 * a <code>Name</code>.
	 */
	public void setName(Name name) {
		internalSetName(name);
	}

	/**
	 * Internal synonym for deprecated method. Used to avoid
	 * deprecation warnings.
	 * @since 3.1
	 */
	/*package*/ void internalSetName(Name name) {
	    supportedOnlyIn2();
		if (name == null) {
			throw new IllegalArgumentException();
		}
		ASTNode oldChild = this.typeName;
		preReplaceChild(oldChild, name, NAME_PROPERTY);
		this.typeName = name;
		postReplaceChild(oldChild, name, NAME_PROPERTY);
	}

	/**
	 * Returns the type instantiated in this class instance creation
	 * expression (added in JLS3 API).
	 *
	 * @return the type node
	 * @exception UnsupportedOperationException if this operation is used in
	 * a JLS2 AST
	 * @since 3.1
	 */
	public Type getType() {
	    unsupportedIn2();
		if (this.type == null) {
			// lazy init must be thread-safe for readers
			synchronized (this) {
				if (this.type == null) {
					preLazyInit();
					this.type = new SimpleType(this.ast);
					postLazyInit(this.type, TYPE_PROPERTY);
				}
			}
		}
		return this.type;
	}

	/**
	 * Sets the type instantiated in this class instance creation
	 * expression (added in JLS3 API).
	 *
	 * @param type the new type
	 * @exception IllegalArgumentException if:
	 * <ul>
	 * <li>the node belongs to a different AST</li>
	 * <li>the node already has a parent</li>
	 * </ul>
	 * @exception UnsupportedOperationException if this operation is used in
	 * a JLS2 AST
	 * @since 3.1
	 */
	public void setType(Type type) {
	    unsupportedIn2();
		if (type == null) {
			throw new IllegalArgumentException();
		}
		ASTNode oldChild = this.type;
		preReplaceChild(oldChild, type, TYPE_PROPERTY);
		this.type = type;
		postReplaceChild(oldChild, type, TYPE_PROPERTY);
	}

	/**
	 * Returns the live ordered list of argument expressions in this class
	 * instance creation expression.
	 *
	 * @return the live list of argument expressions (possibly empty)
	 *    (element type: {@link Expression})
	 */
	public List arguments() {
		return this.arguments;
	}

	/**
	 * Returns the anonymous class declaration introduced by this
	 * class instance creation expression, if it has one.
	 *
	 * @return the anonymous class declaration, or <code>null</code> if none
	 */
	public AnonymousClassDeclaration getAnonymousClassDeclaration() {
		return this.optionalAnonymousClassDeclaration;
	}

	/**
	 * Sets whether this class instance creation expression declares
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
	 * expression. For anonymous classes, the binding is that of the anonymous
	 * constructor.
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
	 * Returns <code>true</code> if the resolved class type has been inferred
	 * from the assignment context (JLS4 15.12.2.8), <code>false</code> otherwise.
	 * <p>
	 * This information is available only when bindings are requested when the AST is being built.
	 *
	 * @return <code>true</code> if the resolved class type has been inferred
	 * 	from the assignment context (JLS3 15.12.2.8), <code>false</code> otherwise
	 * @since 3.7.1
	 */
	public boolean isResolvedTypeInferredFromExpectedType() {
		return this.ast.getBindingResolver().isResolvedTypeInferredFromExpectedType(this);
	}

	@Override
	int memSize() {
		// treat Code as free
		return BASE_NODE_SIZE + 6 * 4;
	}

	@Override
	int treeSize() {
		// n.b. type == null for ast.API_LEVEL == JLS2
		// n.b. typeArguments == null for ast.API_LEVEL == JLS2
		// n.b. typeName == null for ast.API_LEVEL >= JLS3
		return
			memSize()
			+ (this.typeName == null ? 0 : getName().treeSize())
			+ (this.type == null ? 0 : getType().treeSize())
			+ (this.optionalExpression == null ? 0 : getExpression().treeSize())
			+ (this.typeArguments == null ? 0 : this.typeArguments.listSize())
			+ (this.arguments == null ? 0 : this.arguments.listSize())
			+ (this.optionalAnonymousClassDeclaration == null ? 0 : getAnonymousClassDeclaration().treeSize());
	}
}

