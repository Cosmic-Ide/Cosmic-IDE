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

import java.util.List;

/**
 * Abstract subclass for type declaration, enum declaration,
 * and annotation type declaration AST node types.
 * <pre>
 * AbstractTypeDeclaration:
 * 		TypeDeclaration
 * 		EnumDeclaration
 * 		AnnotationTypeDeclaration
 * </pre>
 *
 * @since 3.0
 */
@SuppressWarnings("rawtypes")
public abstract class AbstractTypeDeclaration extends BodyDeclaration {

	/**
	 * The type name; lazily initialized; defaults to a unspecified,
	 * legal Java class identifier.
	 * @since 2.0 (originally declared on {@link TypeDeclaration})
	 */
	SimpleName typeName = null;

	/**
	 * The body declarations (element type: {@link BodyDeclaration}).
	 * Defaults to an empty list.
	 * @since 2.0 (originally declared on {@link TypeDeclaration})
	 */
	ASTNode.NodeList bodyDeclarations;

	/**
	 * Returns structural property descriptor for the "bodyDeclarations" property
	 * of this node (element type: {@link BodyDeclaration}).
	 *
	 * @return the property descriptor
	 */
	abstract ChildListPropertyDescriptor internalBodyDeclarationsProperty();

	/**
	 * Returns structural property descriptor for the "bodyDeclarations" property
	 * of this node (element type: {@link BodyDeclaration}).
	 *
	 * @return the property descriptor
	 * @since 3.1
	 */
	public final ChildListPropertyDescriptor getBodyDeclarationsProperty() {
		return internalBodyDeclarationsProperty();
	}

	/**
	 * Returns structural property descriptor for the "name" property
	 * of this node (child type: {@link SimpleName}).
	 *
	 * @return the property descriptor
	 */
	abstract ChildPropertyDescriptor internalNameProperty();

	/**
	 * Returns structural property descriptor for the "name" property
	 * of this node (child type: {@link SimpleName}).
	 *
	 * @return the property descriptor
	 * @since 3.1
	 */
	public final ChildPropertyDescriptor getNameProperty() {
		return internalNameProperty();
	}

	/**
	 * Creates and returns a structural property descriptor for the
	 * "bodyDeclaration" property declared on the given concrete node type (element type: {@link BodyDeclaration}).
	 *
	 * @return the property descriptor
	 */
	static final ChildListPropertyDescriptor internalBodyDeclarationPropertyFactory(Class nodeClass) {
		return new ChildListPropertyDescriptor(nodeClass, "bodyDeclarations", BodyDeclaration.class, CYCLE_RISK); //$NON-NLS-1$
	}

	/**
	 * Creates and returns a structural property descriptor for the
	 * "name" property declared on the given concrete node type (child type: {@link SimpleName}).
	 *
	 * @return the property descriptor
	 */
	static final ChildPropertyDescriptor internalNamePropertyFactory(Class nodeClass) {
		return new ChildPropertyDescriptor(nodeClass, "name", SimpleName.class, MANDATORY, NO_CYCLE_RISK); //$NON-NLS-1$
	}

	/**
	 * Creates a new AST node for an abstract type declaration owned by the given
	 * AST.
	 * <p>
	 * N.B. This constructor is package-private; all subclasses must be
	 * declared in the same package; clients are unable to declare
	 * additional subclasses.
	 * </p>
	 *
	 * @param ast the AST that is to own this node
	 */
	AbstractTypeDeclaration(AST ast) {
		super(ast);
		this.bodyDeclarations = new ASTNode.NodeList(internalBodyDeclarationsProperty());
	}

	/**
	 * Returns the name of the type declared in this type declaration.
	 *
	 * @return the type name node
	 * @since 2.0 (originally declared on {@link TypeDeclaration})
	 */
	public SimpleName getName() {
		if (this.typeName == null) {
			// lazy init must be thread-safe for readers
			synchronized (this) {
				if (this.typeName == null) {
					preLazyInit();
					this.typeName = new SimpleName(this.ast);
					postLazyInit(this.typeName, internalNameProperty());
				}
			}
		}
		return this.typeName;
	}

	/**
	 * Sets the name of the type declared in this type declaration to the
	 * given name.
	 *
	 * @param typeName the new type name
	 * @exception IllegalArgumentException if:
	 * <ul>
	 * <li>the node belongs to a different AST</li>
	 * <li>the node already has a parent</li>
	 * </ul>
	 * @since 2.0 (originally declared on {@link TypeDeclaration})
	 */
	public void setName(SimpleName typeName) {
		if (typeName == null) {
			throw new IllegalArgumentException();
		}
		ChildPropertyDescriptor p = internalNameProperty();
		ASTNode oldChild = this.typeName;
		preReplaceChild(oldChild, typeName, p);
		this.typeName = typeName;
		postReplaceChild(oldChild, typeName, p);
	}

	/**
	 * Returns the live ordered list of body declarations of this type
	 * declaration.
	 *
	 * @return the live list of body declarations
	 *    (element type: {@link BodyDeclaration})
	 * @since 2.0 (originally declared on {@link TypeDeclaration})
	 */
	public List bodyDeclarations() {
		return this.bodyDeclarations;
	}

	/**
	 * Returns whether this type declaration is a package member (that is,
	 * a top-level type).
	 * <p>
	 * Note that this is a convenience method that simply checks whether
	 * this node's parent is a compilation unit node.
	 * </p>
	 *
	 * @return <code>true</code> if this type declaration is a child of
	 *   a compilation unit node, and <code>false</code> otherwise
	 * @since 2.0 (originally declared on {@link TypeDeclaration})
	 */
	public boolean isPackageMemberTypeDeclaration() {
		ASTNode parent = getParent();
		return (parent instanceof CompilationUnit);
	}

	/**
	 * Returns whether this type declaration is a type member.
	 * <p>
	 * Note that this is a convenience method that simply checks whether
	 * this node's parent is a type declaration node or an anonymous
	 * class declaration.
	 * </p>
	 *
	 * @return <code>true</code> if this type declaration is a child of
	 *   a type declaration node or an anonymous class declaration node,
	 *   and <code>false</code> otherwise
	 * @since 2.0 (originally declared on {@link TypeDeclaration})
	 */
	public boolean isMemberTypeDeclaration() {
		ASTNode parent = getParent();
		return (parent instanceof AbstractTypeDeclaration)
			|| (parent instanceof AnonymousClassDeclaration);
	}

	/**
	 * Returns whether this type declaration is a local type.
	 * <p>
	 * Note that this is a convenience method that simply checks whether
	 * this node's parent is a type declaration statement node.
	 * </p>
	 *
	 * @return <code>true</code> if this type declaration is a child of
	 *   a type declaration statement node, and <code>false</code> otherwise
	 * @since 2.0 (originally declared on <code>TypeDeclaration</code>)
	 */
	public boolean isLocalTypeDeclaration() {
		ASTNode parent = getParent();
		return (parent instanceof TypeDeclarationStatement);
	}

	/**
	 * Resolves and returns the binding for the type declared in this type
	 * declaration.
	 * <p>
	 * Note that bindings are generally unavailable unless requested when the
	 * AST is being built.
	 * </p>
	 *
	 * @return the binding, or <code>null</code> if the binding cannot be
	 *    resolved
	 * @since 3.1 Declared in 3.0 on the individual subclasses.
	 */
	public final ITypeBinding resolveBinding() {
		return internalResolveBinding();
	}

	/**
	 * Resolves and returns the binding for the type declared in this type
	 * declaration. This method must be implemented by subclasses.
	 *
	 * @return the binding, or <code>null</code> if the binding cannot be
	 *    resolved
	 */
	abstract ITypeBinding internalResolveBinding();

	@Override
	int memSize() {
		return super.memSize() + 2 * 4;
	}

}
