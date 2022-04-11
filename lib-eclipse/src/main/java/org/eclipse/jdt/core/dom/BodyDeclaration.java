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

import java.util.Iterator;
import java.util.List;

/**
 * Abstract base class of all AST nodes that represent body declarations
 * that may appear in the body of some kind of class or interface declaration,
 * including anonymous class declarations, enumeration declarations, and
 * enumeration constant declarations.
 * <pre>
 * BodyDeclaration:
 * 		{@link AbstractTypeDeclaration}
 * 			{@link AnnotationTypeDeclaration}
 * 			{@link EnumDeclaration}
 * 			{@link TypeDeclaration} (for classes and interfaces)
 *		{@link AnnotationTypeMemberDeclaration}
 *		{@link EnumConstantDeclaration}
 * 		{@link FieldDeclaration}
 * 		{@link Initializer}
 *		{@link MethodDeclaration} (for methods and constructors)
 * </pre>
 * <p>
 * All types of body declarations carry modifiers (and annotations), although they differ in
 * which modifiers are allowed. Most types of body declarations can carry a
 * doc comment; Initializer is the only ones that does not. The source range
 * for body declarations always includes the doc comment if present.
 * </p>
 *
 * @since 2.0
 */
@SuppressWarnings("rawtypes")
public abstract class BodyDeclaration extends ASTNode {

	/**
	 * The doc comment, or <code>null</code> if none.
	 * Defaults to none.
	 */
	Javadoc optionalDocComment = null;

	/**
	 * The modifier flags; bit-wise or of Modifier flags.
	 * Defaults to none. Not used in 3.0.
	 * @since 3.0 - field was moved up from subclasses
	 */
	private int modifierFlags = Modifier.NONE;

	/**
	 * The extended modifiers (element type: {@link IExtendedModifier}).
	 * Null in JLS2. Added in JLS3; defaults to an empty list
	 * (see constructor).
	 *
	 * @since 3.0
	 */
	ASTNode.NodeList modifiers = null;

	/**
	 * Returns structural property descriptor for the "modifiers" property
	 * of this node as used in JLS2 (type: {@link Integer}).
	 *
	 * @return the property descriptor
	 * @deprecated In the JLS3 API, this method is replaced by {@link #internalModifiers2Property()}.
	 */
	abstract SimplePropertyDescriptor internalModifiersProperty();

	/**
	 * Returns structural property descriptor for the "modifiers" property
	 * of this node as used in JLS3 (element type: {@link IExtendedModifier}).
	 *
	 * @return the property descriptor
	 */
	abstract ChildListPropertyDescriptor internalModifiers2Property();

	/**
	 * Returns structural property descriptor for the "modifiers" property
	 * of this node as used in JLS3 (element type: {@link IExtendedModifier}).
	 *
	 * @return the property descriptor
	 * @since 3.1
	 */
	public final ChildListPropertyDescriptor getModifiersProperty() {
		// important: return property for AST.JLS3
		return internalModifiers2Property();
	}

	/**
	 * Returns structural property descriptor for the "javadoc" property
	 * of this node (child type: {@link Javadoc}).
	 *
	 * @return the property descriptor
	 */
	abstract ChildPropertyDescriptor internalJavadocProperty();

	/**
	 * Returns structural property descriptor for the "javadoc" property
	 * of this node (child type: {@link Javadoc}).
	 *
	 * @return the property descriptor
	 * @since 3.1
	 */
	public final ChildPropertyDescriptor getJavadocProperty() {
		return internalJavadocProperty();
	}

	/**
	 * Creates and returns a structural property descriptor for the
	 * "javadoc" property declared on the given concrete node type (child type: {@link Javadoc}).
	 *
	 * @return the property descriptor
	 */
	static final ChildPropertyDescriptor internalJavadocPropertyFactory(Class nodeClass) {
		return new ChildPropertyDescriptor(nodeClass, "javadoc", Javadoc.class, OPTIONAL, NO_CYCLE_RISK); //$NON-NLS-1$
	}

	/**
	 * Creates and returns a structural property descriptor for the
	 * "modifiers" property declared on the given concrete node type (type: {@link Integer}).
	 *
	 * @return the property descriptor
	 * @deprecated In the JLS3 API, this method is replaced by {@link #internalModifiers2PropertyFactory(Class)}.
	 */
	static final SimplePropertyDescriptor internalModifiersPropertyFactory(Class nodeClass) {
		return new SimplePropertyDescriptor(nodeClass, "modifiers", int.class, MANDATORY); //$NON-NLS-1$
	}

	/**
	 * Creates and returns a structural property descriptor for the
	 * "modifiers" property declared on the given concrete node type (element type: {@link IExtendedModifier}).
	 *
	 * @return the property descriptor
	 */
	static final ChildListPropertyDescriptor internalModifiers2PropertyFactory(Class nodeClass) {
		return new ChildListPropertyDescriptor(nodeClass, "modifiers", IExtendedModifier.class, CYCLE_RISK); //$NON-NLS-1$
	}

	/**
	 * Creates a new AST node for a body declaration node owned by the
	 * given AST.
	 * <p>
	 * N.B. This constructor is package-private.
	 * </p>
	 *
	 * @param ast the AST that is to own this node
	 */
	BodyDeclaration(AST ast) {
		super(ast);
		if (ast.apiLevel >= AST.JLS3_INTERNAL) {
			this.modifiers = new ASTNode.NodeList(internalModifiers2Property());
		}
	}

	/**
	 * Returns the doc comment node.
	 *
	 * @return the doc comment node, or <code>null</code> if none
	 */
	public Javadoc getJavadoc() {
		return this.optionalDocComment;
	}

	/**
	 * Sets or clears the doc comment node.
	 *
	 * @param docComment the doc comment node, or <code>null</code> if none
	 * @exception IllegalArgumentException if the doc comment string is invalid
	 */
	public void setJavadoc(Javadoc docComment) {
		ChildPropertyDescriptor p = internalJavadocProperty();
		ASTNode oldChild = this.optionalDocComment;
		preReplaceChild(oldChild, docComment, p);
		this.optionalDocComment = docComment;
		postReplaceChild(oldChild, docComment, p);
	}

	/**
	 * Returns the modifiers explicitly specified on this declaration.
	 * <p>
	 * In the JLS3 API, this method is a convenience method that
	 * computes these flags from {@link #modifiers()}.
	 * </p>
	 *
	 * @return the bit-wise "or" of <code>Modifier</code> constants
	 * @see Modifier
	 */
	public int getModifiers() {
		// more efficient than checking getAST().API_LEVEL
		if (this.modifiers == null) {
			// JLS2 behavior - bona fide property
			return this.modifierFlags;
		} else {
			// JLS3 behavior - convenience method
			// performance could be improved by caching computed flags
			// but this would require tracking changes to this.modifiers
			int computedmodifierFlags = Modifier.NONE;
			for (Iterator it = modifiers().iterator(); it.hasNext(); ) {
				Object x = it.next();
				if (x instanceof Modifier) {
					computedmodifierFlags |= ((Modifier) x).getKeyword().toFlagValue();
				}
			}
			return computedmodifierFlags;
		}
	}

	/**
	 * Sets the modifiers explicitly specified on this declaration (JLS2 API only).
	 *
	 * @param modifiers the given modifiers (bit-wise "or" of {@link Modifier} constants)
	 * @exception UnsupportedOperationException if this operation is used in
	 * an AST later than JLS2
	 * @see Modifier
	 * @deprecated In the JLS3 API, this method is replaced by
	 * {@link #modifiers()}, which contains a list of {@link Modifier} nodes.
	 */
	public void setModifiers(int modifiers) {
		internalSetModifiers(modifiers);
	}

	/**
	 * Internal synonym for deprecated method. Used to avoid
	 * deprecation warnings.
	 * @since 3.1
	 */
	/*package*/ final void internalSetModifiers(int pmodifiers) {
		// more efficient than just calling supportedOnlyIn2() to check
		if (this.modifiers != null) {
			supportedOnlyIn2();
		}
		SimplePropertyDescriptor p = internalModifiersProperty();
		preValueChange(p);
		this.modifierFlags = pmodifiers;
		postValueChange(p);
	}

	/**
	 * Returns the live ordered list of modifiers and annotations
	 * of this declaration (added in JLS3 API).
	 *
	 * @return the live list of modifiers and annotations
	 *    (element type: {@link IExtendedModifier})
	 * @exception UnsupportedOperationException if this operation is used in
	 * a JLS2 AST
	 * @since 3.1
	 */
	public List modifiers() {
		// more efficient than just calling unsupportedIn2() to check
		if (this.modifiers == null) {
			unsupportedIn2();
		}
		return this.modifiers;
	}

	@Override
	int memSize() {
		return BASE_NODE_SIZE + 3 * 4;
	}
}

