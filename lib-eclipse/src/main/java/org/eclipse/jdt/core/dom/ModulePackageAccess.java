/*******************************************************************************
 * Copyright (c) 2016, 2017 IBM Corporation and others.
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

import java.util.List;

/**
 * Abstract base class of AST nodes that represent exports and opens directives (added in JLS9 API).
 *
 * <pre>
 * ModulePackageAccess:
 *    {@link ExportsDirective}
 *    {@link OpensDirective}
 * </pre>
 *
 * @noextend This class is not intended to be subclassed by clients.
 * @since 3.14
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public abstract class ModulePackageAccess extends ModuleDirective {

	/**
	 * The package name; lazily initialized; defaults to a unspecified,
	 * legal Java identifier.
	 */
	protected Name name = null;

	/**
	 * The target modules
	 * (element type: {@link Name}).
	 * Defaults to an empty list. (see constructor)
	 */
	protected ASTNode.NodeList modules = null;

	/**
	 * Returns structural property descriptor for the "modules" property
	 * of this node (element type: {@link Name}).
	 *
	 * @return the property descriptor
	 */
	abstract ChildListPropertyDescriptor internalModulesProperty();

	/**
	 * Returns structural property descriptor for the "name" property
	 * of this node (child type: {@link Name}).
	 *
	 * @return the property descriptor
	 */
	abstract ChildPropertyDescriptor internalNameProperty();

	/**
	 * Returns structural property descriptor for the "name" property
	 * of this node (child type: {@link Name}).
	 *
	 * @return the property descriptor
	 */
	public final ChildPropertyDescriptor getNameProperty() {
		return internalNameProperty();
	}

	/**
	 * Creates and returns a structural property descriptor for the
	 * "name" property declared on the given concrete node type (child type: {@link Name}).
	 *
	 * @return the property descriptor
	 */
	static final ChildPropertyDescriptor internalNamePropertyFactory(Class nodeClass) {
		return new ChildPropertyDescriptor(nodeClass, "name", Name.class, MANDATORY, NO_CYCLE_RISK); //$NON-NLS-1$
	}

	/**
	 * Creates and returns a structural property descriptor for the
	 * "modules" property declared on the given concrete node type (element type: {@link Name}).
	 *
	 * @return the property descriptor
	 */
	static final ChildListPropertyDescriptor internalModulesPropertyFactory(Class nodeClass) {
		return new ChildListPropertyDescriptor(nodeClass, "modules", Name.class, NO_CYCLE_RISK); //$NON-NLS-1$
	}

	/**
	 * <p>
	 * N.B. This constructor is package-private; all subclasses must be
	 * declared in the same package; clients are unable to declare
	 * additional subclasses.
	 * </p>
	 *
	 * @param ast the AST that is to own this node
	 */
	ModulePackageAccess(AST ast) {
		super(ast);
		this.modules = new ASTNode.NodeList(internalModulesProperty());
	}

	@Override
	final ASTNode internalGetSetChildProperty(ChildPropertyDescriptor property, boolean get, ASTNode child) {
		if (property == internalNameProperty()) {
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
		if (property == internalModulesProperty()) {
			return modules();
		}
		// allow default implementation to flag the error
		return super.internalGetChildListProperty(property);
	}

	/**
	 * Returns the name of the package.
	 *
	 * @return the package name node
	 */
	public Name getName()  {
		if (this.name == null) {
			// lazy init must be thread-safe for readers
			synchronized (this) {
				if (this.name == null) {
					preLazyInit();
					this.name =this.ast.newQualifiedName(
							new SimpleName(this.ast), new SimpleName(this.ast));
					ChildPropertyDescriptor p = internalNameProperty();
					postLazyInit(this.name, p);
				}
			}
		}
		return this.name;
	}

	/**
	 * Sets the name of the package to the given name.
	 *
	 * @param name the new  package name
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
		ChildPropertyDescriptor p = internalNameProperty();
		preReplaceChild(oldChild, name, p);
		this.name = name;
		postReplaceChild(oldChild, name, p);
	}

	/**
	 * Returns the live ordered list of target modules for this
	 * directive.
	 *
	 * @return the live list of target modules
	 *    (element type: {@link Name})
	 */
	public List modules() {
		return this.modules;
	}

	protected ASTNode cloneHelper(AST target, ModulePackageAccess result) {
		result.setSourceRange(getStartPosition(), getLength());
		result.setName((Name) getName().clone(target));
		result.modules().addAll(ASTNode.copySubtrees(target, modules()));
		return result;
	}

	protected void acceptVisitChildren(boolean visitChildren, ASTVisitor visitor) {
		if (visitChildren) {
			acceptChild(visitor, getName());
			acceptChildren(visitor, this.modules);
		}
	}

	@Override
	int memSize() {
		return BASE_NODE_SIZE + 2 * 4;
	}

	@Override
	int treeSize() {
		return
			memSize()
			+ (this.name == null ? 0 : getName().treeSize())
			+ this.modules.listSize();
	}
}
