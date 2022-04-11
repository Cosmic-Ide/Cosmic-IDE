/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
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
package org.eclipse.jdt.internal.core;

import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.SourceRange;
import org.eclipse.jdt.internal.compiler.env.ISourceImport;

/**
 * Element info for IImportDeclaration elements.
 * @see org.eclipse.jdt.core.IImportDeclaration
 */
public class ImportDeclarationElementInfo extends MemberElementInfo implements ISourceImport{

	/**
	 * The start position of this import declaration's name in the its
	 * openable's buffer.
	 */
	protected int nameStart= -1;

	/**
	 * The last position of this import declaration's name in the its
	 * openable's buffer.
	 */
	protected int nameEnd= -1;

	/**
	 * Sets the last position of this import declaration's name, relative
	 * to its openable's source buffer.
	 */
	protected void setNameSourceEnd(int end) {
		this.nameEnd= end;
	}
	/**
	 * Sets the start position of this import declaration's name, relative
	 * to its openable's source buffer.
	 */
	protected void setNameSourceStart(int start) {
		this.nameStart= start;
	}

	protected ISourceRange getNameRange() {
		return new SourceRange(this.nameStart, this.nameEnd - this.nameStart + 1);
	}
}
