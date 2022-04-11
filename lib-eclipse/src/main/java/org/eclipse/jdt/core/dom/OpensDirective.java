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

import java.util.ArrayList;
import java.util.List;

/**
 * Opens directive AST node type (added in JLS9 API).
 * <pre>
 * OpensDirective:
 *     <b>opens</b> PackageName [ <b>to</b>  ModuleName {<b>,</b> ModuleName } ] <b>;</b>
 * </pre>
 *
 * @since 3.14
 *
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
@SuppressWarnings({"rawtypes"})
public class OpensDirective extends ModulePackageAccess {

	/**
	 * The "name" structural property of this node type (child type: {@link Name}).
	 */
	public static final ChildPropertyDescriptor NAME_PROPERTY =
		internalNamePropertyFactory(OpensDirective.class);
	/**
	 * The "modules" structural property of this node type (element type: {@link Name}).
	 */
	public static final ChildListPropertyDescriptor MODULES_PROPERTY =
			internalModulesPropertyFactory(OpensDirective.class);

	/**
	 * A list of property descriptors (element type:
	 * {@link StructuralPropertyDescriptor}),
	 * or null if uninitialized.
	 */
	private static final List PROPERTY_DESCRIPTORS_9_0;

	static {
		List properyList = new ArrayList(3);
		createPropertyList(OpensDirective.class, properyList);
		addProperty(NAME_PROPERTY, properyList);
		addProperty(MODULES_PROPERTY, properyList);
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
	 * Creates a new AST node for an opens directive owned by the
	 * given AST. The open directive initially is a regular (non-targetted)
	 * single package open for an unspecified, but legal, Java package name.
	 * <p>
	 * N.B. This constructor is package-private; all subclasses must be
	 * declared in the same package; clients are unable to declare
	 * additional subclasses.
	 * </p>
	 *
	 * @param ast the AST that is to own this node
	 */
	OpensDirective(AST ast) {
		super(ast);
	}

	@Override
	final List internalStructuralPropertiesForType(int apiLevel) {
		return propertyDescriptors(apiLevel);
	}

	@Override
	final ChildPropertyDescriptor internalNameProperty() {
		return NAME_PROPERTY;
	}

	@Override
	final ChildListPropertyDescriptor internalModulesProperty() {
		return MODULES_PROPERTY;
	}

	@Override
	final int getNodeType0() {
		return OPENS_DIRECTIVE;
	}

	@Override
	ASTNode clone0(AST target) {
		return cloneHelper(target, new OpensDirective(target));
	}

	@Override
	final boolean subtreeMatch0(ASTMatcher matcher, Object other) {
		// dispatch to correct overloaded match method
		return matcher.match(this, other);
	}

	@Override
	void accept0(ASTVisitor visitor) {
		boolean visitChildren = visitor.visit(this);
		acceptVisitChildren(visitChildren, visitor);
		visitor.endVisit(this);
	}
}
