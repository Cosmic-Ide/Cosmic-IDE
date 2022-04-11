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

import java.util.List;

/**
 * Abstract base class of AST nodes that represent patterns.
 * There are several kinds of patterns.
 * <pre>
 * Expression:
 *    {@link TypePattern}, {@link GuardedPattern} and {@link NullPattern}
 *

 * </pre>
 *
 * @since 3.27
 * @noreference This class is not intended to be referenced by clients.
 */
public abstract class Pattern extends Expression {

	/**
	 * Creates a new AST node for an expression owned by the given AST.
	 * <p>
	 * N.B. This constructor is package-private.
	 * </p>
	 *
	 * @param ast the AST that is to own this node
	 */
	Pattern(AST ast) {
		super(ast);
	}

	/**
	 * Creates and returns a structural property descriptor for the
	 * "pattern" property declared on the given concrete node type).
	 *
	 * @return the pattern property descriptor
	 */
	@SuppressWarnings("rawtypes")
	static final ChildPropertyDescriptor internalPatternPropertyFactory(Class nodeClass) {
		return new ChildPropertyDescriptor(nodeClass, "pattern", Javadoc.class, MANDATORY, NO_CYCLE_RISK); //$NON-NLS-1$
	}

	/**
	 * Returns the list of pattern variables
	 *
	 *  @return the list of pattern variables
	 *    (element type: {@link SingleVariableDeclaration})
	 * @exception UnsupportedOperationException if this operation is not used for JLS17
	 * @exception UnsupportedOperationException if this operation is not used with previewEnabled flag as true
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public abstract List<SingleVariableDeclaration> patternVariables();

}

