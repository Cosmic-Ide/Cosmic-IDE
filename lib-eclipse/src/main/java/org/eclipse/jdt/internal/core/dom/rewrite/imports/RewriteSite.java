/*******************************************************************************
 * Copyright (c) 2015 Google Inc and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     John Glassmyer <jogl@google.com> - import group sorting is broken - https://bugs.eclipse.org/430303
 *******************************************************************************/
package org.eclipse.jdt.internal.core.dom.rewrite.imports;

import org.eclipse.jface.text.IRegion;

/**
 * Describes the location in the compilation unit to be occupied by import declarations.
 */
class RewriteSite {
	/**
	 * The region where import declarations, their associated comments, and any adjacent whitespace
	 * should be placed.
	 * <p>
	 * Extends from the end of the AST node preceding import declarations and their comments (or the
	 * start of the compilation unit, if no such node exists) to the start of the AST node
	 * succeeding import declarations and their comments (or the end of the compilation unit, if no
	 * such node exists).
	 */
	final IRegion surroundingRegion;

	/**
	 * The region occupied by import declarations and their associated comments prior to the
	 * rewrite, or null if the compilation unit does not contain any import declarations.
	 * <p>
	 * If not null, this region is contained within surroundingRegion.
	 */
	final IRegion importsRegion;

	/**
	 * True if the compilation unit prior to the rewrite contains any top-level AST nodes (package
	 * declaration and/or comments) preceding the start of surroundingRegion.
	 */
	final boolean hasPrecedingElements;

	/**
	 * True if the compilation unit prior to the rewrite contains any top-level AST nodes (type
	 * declarations and/or comments) following the end of surroundingRegion.
	 */
	final boolean hasSucceedingElements;

	RewriteSite(
			IRegion surroundingRegion,
			IRegion importsRegion,
			boolean hasPrecedingElements,
			boolean hasSucceedingElements) {
		this.surroundingRegion = surroundingRegion;
		this.importsRegion = importsRegion;
		this.hasPrecedingElements = hasPrecedingElements;
		this.hasSucceedingElements = hasSucceedingElements;
	}
}
