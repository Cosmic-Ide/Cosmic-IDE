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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.text.IRegion;

/**
 * Represents an import declaration that originally occurred in the compilation unit.
 */
class OriginalImportEntry extends ImportEntry {
	/**
	 * The comments associated with (either preceding, embedded within, or following) this import
	 * declaration.
	 */
	final List<ImportComment> comments;

	/**
	 * The difference between the line number of the start of the import declaration (or the start
	 * of its first leading comment, if any) and the line number of the end of the preceding import
	 * declaration (or the end of that import's trailing comment, if any). Zero for the first import
	 * in the compilation unit.
	 */
	final int precedingLineDelimiters;

	/**
	 * The region of the compilation unit occupied by the whitespace (e.g. line delimiters) between
	 * the previous import (or its last trailing comment, if any) and this import declaration (or
	 * its first leading comment, if any).
	 */
	final IRegion leadingDelimiter;

	/**
	 * The region of the compilation unit occupied by the import declaration itself, its associated
	 * comments, and any whitespace between the import declaration and its comments.
	 */
	final IRegion declarationAndComments;

	OriginalImportEntry(
			ImportName importName,
			Collection<ImportComment> comments,
			int precedingLeadingDelimiters,
			IRegion leadingWhitespace,
			IRegion declarationAndComments) {
		super(importName);

		this.comments = Collections.unmodifiableList(new ArrayList<ImportComment>(comments));
		this.precedingLineDelimiters = precedingLeadingDelimiters;
		this.leadingDelimiter = leadingWhitespace;
		this.declarationAndComments = declarationAndComments;
	}

	@Override
	public String toString() {
		return String.format("OriginalImportEntry(%s)", this.importName); //$NON-NLS-1$
	}

	@Override
	boolean isOriginal() {
		return true;
	}

	@Override
	OriginalImportEntry asOriginalImportEntry() {
		return this;
	}
}