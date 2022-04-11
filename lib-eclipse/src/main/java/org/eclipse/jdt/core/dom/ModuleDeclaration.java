/*******************************************************************************
 * Copyright (c) 2016, 2019 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.core.dom;

import java.util.ArrayList;
import java.util.List;

/**
 * Module declaration AST node type representing the module descriptor file (added in JLS9 API).
 *
 * <pre>
 * ModuleDeclaration:
 *  [ Javadoc ] { Annotation } [ <b>open</b> ] <b>module</b> Name <b>{</b>
 *        { RequiresDirective | ExportsDirective | OpensDirective | UsesDirective | ProvidesDirective }
 *  <b>}</b>
 * </pre>
 *
 * @since 3.14
 *
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
@SuppressWarnings("rawtypes")
public class ModuleDeclaration extends ASTNode {

	/**
	 * The "javadoc" structural property of this node type (child type: {@link Javadoc}).
	 */
	public static final ChildPropertyDescriptor JAVADOC_PROPERTY =
			new ChildPropertyDescriptor(ModuleDeclaration.class, "javadoc", Javadoc.class, OPTIONAL, NO_CYCLE_RISK); //$NON-NLS-1$

	/**
	 * The "annotations" structural property of this node type (element type: {@link Annotation}).
	 */
	public static final ChildListPropertyDescriptor ANNOTATIONS_PROPERTY =
			new ChildListPropertyDescriptor(ModuleDeclaration.class, "annotations", Annotation.class, NO_CYCLE_RISK); //$NON-NLS-1$

	/**
	 * The "open" structural property of this node type (type: {@link Boolean}).
	 */
	public static final SimplePropertyDescriptor OPEN_PROPERTY =
			new SimplePropertyDescriptor(ModuleDeclaration.class, "open", boolean.class, MANDATORY); //$NON-NLS-1$

	/**
	 * The "name" structural property of this node type (child type: {@link Name}).
	 */
	public static final ChildPropertyDescriptor NAME_PROPERTY =
			new ChildPropertyDescriptor(ModuleDeclaration.class, "name", Name.class, MANDATORY, NO_CYCLE_RISK); //$NON-NLS-1$

	/**
	 * The "moduleDirectives" structural property of this node type (element type: {@link ModuleDirective}).
	 */
	public static final ChildListPropertyDescriptor MODULE_DIRECTIVES_PROPERTY =
			new ChildListPropertyDescriptor(ModuleDeclaration.class, "moduleDirectives", ModuleDirective.class, NO_CYCLE_RISK); //$NON-NLS-1$

	/**
	 * A list of property descriptors (element type:
	 * {@link StructuralPropertyDescriptor}),
	 * or null if uninitialized.
	 */
	private static final List PROPERTY_DESCRIPTORS_9_0;

	static {
		List properyList = new ArrayList(6);
		createPropertyList(ModuleDeclaration.class, properyList);
		addProperty(JAVADOC_PROPERTY, properyList);
		addProperty(ANNOTATIONS_PROPERTY, properyList);
		addProperty(OPEN_PROPERTY, properyList);
		addProperty(NAME_PROPERTY, properyList);
		addProperty(MODULE_DIRECTIVES_PROPERTY, properyList);
		PROPERTY_DESCRIPTORS_9_0 = reapPropertyList(properyList);
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
		return PROPERTY_DESCRIPTORS_9_0;
	}

	/**
	 * The doc comment, or <code>null</code> if none.
	 * Defaults to none.
	 */
	private Javadoc optionalDocComment = null;

	/**
	 * The annotations (element type: {@link Annotation}).
	 * Defaults to an empty list.
	 *
	 */
	private ASTNode.NodeList annotations = new ASTNode.NodeList(ANNOTATIONS_PROPERTY);

	/**
	 * open versus normal; defaults to normal module.
	 */
	private boolean isOpen = false;

	/**
	 * The referenced module name; lazily initialized; defaults to a unspecified,
	 * legal Java identifier.
	 */
	private Name name = null;

	/**
	 * The list of statements (element type: {@link ModuleDirective}).
	 * Defaults to an empty list.
	 */
	private ASTNode.NodeList moduleStatements = new ASTNode.NodeList(MODULE_DIRECTIVES_PROPERTY);

	ModuleDeclaration(AST ast) {
		super(ast);
		unsupportedBelow9();
	}

	@Override
	final List internalStructuralPropertiesForType(int apiLevel) {
		return propertyDescriptors(apiLevel);
	}

	@Override
	final boolean internalGetSetBooleanProperty(SimplePropertyDescriptor property, boolean get, boolean value) {
		if (property == OPEN_PROPERTY) {
			if (get) {
				return isOpen();
			} else {
				setOpen(value);
				return false;
			}
		}
		// allow default implementation to flag the error
		return super.internalGetSetBooleanProperty(property, get, value);
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
				setName((Name) child);
				return null;
			}
		}
		// allow default implementation to flag the error
		return super.internalGetSetChildProperty(property, get, child);
	}

	@Override
	final List internalGetChildListProperty(ChildListPropertyDescriptor property) {
		if (property == ANNOTATIONS_PROPERTY) {
			return annotations();
		}
		if (property == MODULE_DIRECTIVES_PROPERTY) {
			return moduleStatements();
		}
		// allow default implementation to flag the error
		return super.internalGetChildListProperty(property);
	}

	@Override
	int getNodeType0() {
		return MODULE_DECLARATION;
	}

	@SuppressWarnings("unchecked")
	@Override
	ASTNode clone0(AST target) {
		ModuleDeclaration result = new ModuleDeclaration(target);
		result.setSourceRange(getStartPosition(), getLength());
		result.setJavadoc((Javadoc) ASTNode.copySubtree(target, getJavadoc()));
		result.setOpen(isOpen());
		result.annotations().addAll(ASTNode.copySubtrees(target, annotations()));
		result.setName((SimpleName) getName().clone(target));
		result.moduleStatements().addAll(ASTNode.copySubtrees(target, moduleStatements()));
		return result;
	}

	@Override
	boolean subtreeMatch0(ASTMatcher matcher, Object other) {
		// dispatch to correct overloaded match method
		return matcher.match(this, other);
	}

	@Override
	void accept0(ASTVisitor visitor) {
		boolean visitChildren = visitor.visit(this);
		if (visitChildren) {
			// visit children in normal left to right reading order
			acceptChild(visitor, getJavadoc());
			acceptChildren(visitor, this.annotations);
			acceptChild(visitor, getName());
			acceptChildren(visitor, this.moduleStatements);
		}
		visitor.endVisit(this);

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
		ChildPropertyDescriptor p = JAVADOC_PROPERTY;
		ASTNode oldChild = this.optionalDocComment;
		preReplaceChild(oldChild, docComment, p);
		this.optionalDocComment = docComment;
		postReplaceChild(oldChild, docComment, p);
	}

	/**
	 * Returns the live ordered list of annotations
	 * of this declaration.
	 *
	 * @return the live list of annotations
	 *    (element type: {@link Annotation})
	 */
	public List annotations() {
		return this.annotations;
	}

	/**
	 * Returns whether this module declaration is open or not.
	 *
	 * @return <code>true</code> if this is open, else
	 *    <code>false</code>
	 */
	public boolean isOpen() {
		return this.isOpen;
	}

	/**
	 * Sets whether this module declaration is open or not.
	 *
	 * @param isOpen <code>true</code> if this is an open module,
	 *    and <code>false</code> if not
	 */
	public void setOpen(boolean isOpen) {
		preValueChange(OPEN_PROPERTY);
		this.isOpen = isOpen;
		postValueChange(OPEN_PROPERTY);
	}

	/**
	 * Returns the name of this module declaration.
	 *
	 * @return the module name
	 */
	public Name getName()  {
		if (this.name == null) {
			// lazy init must be thread-safe for readers
			synchronized (this) {
				if (this.name == null) {
					preLazyInit();
					this.name =this.ast.newQualifiedName(
							new SimpleName(this.ast), new SimpleName(this.ast));
					postLazyInit(this.name, NAME_PROPERTY);
				}
			}
		}
		return this.name;
	}

	/**
	 * Sets the module name in to the given name.
	 *
	 * @param name the new module name
	 * @exception IllegalArgumentException if:
	 * <ul>
	 * <li>the node belongs to a different AST</li>
	 * <li>the node already has a parent</li>
	 * </ul>
	 */
	public void setName(Name name) {
		if (name == null) {
			throw new IllegalArgumentException();
		}
		ASTNode oldChild = this.name;
		preReplaceChild(oldChild, name, NAME_PROPERTY);
		this.name = name;
		postReplaceChild(oldChild, name, NAME_PROPERTY);
	}

	/**
	 * Returns the live list of statements in this module. Adding and
	 * removing nodes from this list affects this node dynamically.
	 * All nodes in this list must be <code>ModuleDirective</code>s;
	 * attempts to add any other type of node will trigger an
	 * exception.
	 *
	 * @return the live list of statements in this module declaration
	 *    (element type: {@link ModuleDirective})
	 */
	public List moduleStatements() {
		return this.moduleStatements;
	}

	/**
	 * Resolves and returns the binding for the module.
	 * <p>
	 * Note that bindings are generally unavailable unless requested when the
	 * AST is being built.
	 * </p>
	 *
	 * @return the binding, or <code>null</code> if the binding cannot be
	 *    resolved
	 */
	public IModuleBinding resolveBinding() {
		return this.ast.getBindingResolver().resolveModule(this);
	}

	@Override
	int memSize() {
		return BASE_NODE_SIZE + 5 * 4;
	}

	@Override
	int treeSize() {
		return	memSize()
			+ (this.optionalDocComment == null ? 0 : getJavadoc().treeSize())
			+ this.annotations.listSize()
			+ (this.name == null ? 0 : getName().treeSize())
			+ this.moduleStatements.listSize();
	}
}
