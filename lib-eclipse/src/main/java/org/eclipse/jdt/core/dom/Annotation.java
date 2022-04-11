/*******************************************************************************
 * Copyright (c) 2004, 2019 IBM Corporation and others.
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
 * Abstract base class of AST nodes that represent annotations.
 * <pre>
 * Annotation:
 *		NormalAnnotation
 *		MarkerAnnotation
 *		SingleMemberAnnotation
 * </pre>
 * @since 3.1
 */
@SuppressWarnings("rawtypes")
public abstract class Annotation extends Expression implements IExtendedModifier {

	/**
	 * Returns structural property descriptor for the "typeName" property
	 * of this node (child type: {@link Name}).
	 *
	 * @return the property descriptor
	 */
	abstract ChildPropertyDescriptor internalTypeNameProperty();

	/**
	 * Returns structural property descriptor for the "typeName" property
	 * of this node (child type: {@link Name}).
	 *
	 * @return the property descriptor
	 */
	public final ChildPropertyDescriptor getTypeNameProperty() {
		return internalTypeNameProperty();
	}

	/**
	 * Creates and returns a structural property descriptor for the
	 * "typeName" property declared on the given concrete node type (child type: {@link Name}).
	 *
	 * @return the property descriptor
	 */
	static final ChildPropertyDescriptor internalTypeNamePropertyFactory(Class nodeClass) {
		return new ChildPropertyDescriptor(nodeClass, "typeName", Name.class, MANDATORY, NO_CYCLE_RISK); //$NON-NLS-1$
	}

	/**
	 * The annotation type name; lazily initialized; defaults to an unspecified,
	 * legal Java identifier.
	 */
	Name typeName = null;

	/**
	 * Creates a new AST node for an annotation node owned by the
	 * given AST.
	 * <p>
	 * N.B. This constructor is package-private.
	 * </p>
	 *
	 * @param ast the AST that is to own this node
	 */
	Annotation(AST ast) {
		super(ast);
	}

	/**
	 * @see IExtendedModifier#isModifier()
	 */
	@Override
	public boolean isModifier() {
		return false;
	}

	/**
	 * @see IExtendedModifier#isAnnotation()
	 */
	@Override
	public boolean isAnnotation() {
		return true;
	}

	/**
	 * Returns the annotation type name of this annotation.
	 *
	 * @return the annotation type name
	 */
	public Name getTypeName() {
		if (this.typeName == null) {
			// lazy init must be thread-safe for readers
			synchronized (this) {
				if (this.typeName == null) {
					preLazyInit();
					this.typeName = new SimpleName(this.ast);
					postLazyInit(this.typeName, internalTypeNameProperty());
				}
			}
		}
		return this.typeName;
	}

	/**
	 * Sets the annotation type name of this annotation.
	 *
	 * @param typeName the annotation type name
	 * @exception IllegalArgumentException if:
	 * <ul>
	 * <li>the node belongs to a different AST</li>
	 * <li>the node already has a parent</li>
	 * </ul>
	 */
	public void setTypeName(Name typeName) {
		if (typeName == null) {
			throw new IllegalArgumentException();
		}
		ChildPropertyDescriptor p = internalTypeNameProperty();
		ASTNode oldChild = this.typeName;
		preReplaceChild(oldChild, typeName, p);
		this.typeName = typeName;
		postReplaceChild(oldChild, typeName, p);
	}

	/**
	 * Returns whether this is a normal annotation
	 * ({@link NormalAnnotation}).
	 *
	 * @return <code>true</code> if this is a normal annotation,
	 *    and <code>false</code> otherwise
	 */
	public boolean isNormalAnnotation() {
		return (this instanceof NormalAnnotation);
	}

	/**
	 * Returns whether this is a marker annotation
	 * ({@link MarkerAnnotation}).
	 *
	 * @return <code>true</code> if this is a marker annotation,
	 *    and <code>false</code> otherwise
	 */
	public boolean isMarkerAnnotation() {
		return (this instanceof MarkerAnnotation);
	}

	/**
	 * Returns whether this is a single member annotation.
	 * ({@link SingleMemberAnnotation}).
	 *
	 * @return <code>true</code> if this is a single member annotation,
	 *    and <code>false</code> otherwise
	 */
	public boolean isSingleMemberAnnotation() {
		return (this instanceof SingleMemberAnnotation);
	}

	@Override
	int memSize() {
		return BASE_NODE_SIZE + 1 * 4;
	}

	/**
	 * Resolves and returns the resolved annotation for this annotation.
	 * <p>
	 * Note that bindings (which includes resolved annotations) are generally unavailable unless
	 * requested when the AST is being built.
	 * </p>
	 *
	 * @return the resolved annotation, or <code>null</code> if the annotation cannot be resolved
	 * @since 3.2
	 */
	public IAnnotationBinding resolveAnnotationBinding() {
	    return this.ast.getBindingResolver().resolveAnnotation(this);
	}
}
