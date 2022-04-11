/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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
package org.eclipse.jdt.internal.core.search.matching;

import java.io.IOException;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.internal.core.index.EntryResult;
import org.eclipse.jdt.internal.core.index.Index;

public class ModulePattern extends JavaSearchPattern {

	boolean findDeclarations = true; /* package visible */
	boolean findReferences = true; /* package visible */
	char[] name; /* package visible */

	protected static char[][] REF_CATEGORIES = { MODULE_REF };
	protected static char[][] REF_AND_DECL_CATEGORIES = { MODULE_REF, MODULE_DECL };
	protected static char[][] DECL_CATEGORIES = { MODULE_DECL };
	private static char[] regexPrefix = {'/','r',' '};

	public static char[] createIndexKey(char[] name) {
		return name; // until a need arises, let the name itself be the index key.
	}
	protected ModulePattern(int matchRule) {
		super(MODULE_PATTERN, matchRule);
	}
	public ModulePattern(char[] name, int limitTo, int matchRule) {
		this(ModulePattern.trapDoorRegexMatchRule(name, matchRule));
		this.name = trapDoorRegexExtractModuleName(name);
		switch (limitTo & 0xF) {
			case IJavaSearchConstants.DECLARATIONS :
				this.findReferences = false;
				break;
			case IJavaSearchConstants.REFERENCES :
				this.findDeclarations = false;
				break;
			case IJavaSearchConstants.ALL_OCCURRENCES :
				if ((getMatchRule() & SearchPattern.R_REGEXP_MATCH) != 0)
					this.findReferences = false; //regex implemented only for module declarations.
				break;
		}
		this.mustResolve = mustResolve();
	}
	/*
	 * Trap door method for implicitly triggering a regex search.
	 * if the module name starts with regex: (case insensitive),
	 * the rest of the characters are taken as a regular expression
	 * for the module name.
	 */
	private static char[] trapDoorRegexExtractModuleName(char[] name2) {
		int index = CharOperation.indexOf(regexPrefix, name2, false);
		return index >= 0 ? CharOperation.subarray(name2, index + regexPrefix.length, name2.length) : name2;
	}
	/*
	 * Sets the match rule to regular expression search as well.
	 */
	private static int trapDoorRegexMatchRule(char[] name2, int matchRule) {
		return CharOperation.indexOf(regexPrefix, name2, false) == 0 ?
				SearchPattern.R_REGEXP_MATCH : matchRule;
	}
	@Override
	public void decodeIndexKey(char[] key) {
		this.name = key;
	}
	@Override
	public SearchPattern getBlankPattern() {
		return new ModulePattern(R_EXACT_MATCH);
	}
	@Override
	public char[][] getIndexCategories() {
		if (this.findReferences)
			return this.findDeclarations ? REF_AND_DECL_CATEGORIES : REF_CATEGORIES;
		if (this.findDeclarations)
			return DECL_CATEGORIES;
		return CharOperation.NO_CHAR_CHAR;
	}
	@Override
	public boolean matchesDecodedKey(SearchPattern decodedPattern) {
		return matchesName(this.name, ((ModulePattern) decodedPattern).name);
	}
	@Override
	public EntryResult[] queryIn(Index index) throws IOException {
		char[] key = this.name; // can be null
		int matchRule = getMatchRule();

		switch(getMatchMode()) {
			case R_EXACT_MATCH :
				if (this.name != null) {
					key = createIndexKey(this.name);
				} else { // do a prefix query with the selector
					matchRule &= ~R_EXACT_MATCH;
					matchRule |= R_PREFIX_MATCH;
				}
				break;
			case R_PREFIX_MATCH :
				// do a prefix query with the selector
				break;
			case R_PATTERN_MATCH :
				if (this.name != null) {
					key = createIndexKey(this.name);
				}
				// else do a pattern query with just the selector
				break;
			case R_REGEXP_MATCH :
				// nothing to do here for the regex match
				break;
			case R_CAMELCASE_MATCH:
			case R_CAMELCASE_SAME_PART_COUNT_MATCH:
				// do a prefix query with the selector
				break;
		}

		return index.query(getIndexCategories(), key, matchRule); // match rule is irrelevant when the key is null
	}

	protected boolean mustResolve() {
		return true;
	}
	@Override
	protected StringBuffer print(StringBuffer output) {
		if (this.findDeclarations) {
			output.append(this.findReferences
				? "ModuleCombinedPattern: " //$NON-NLS-1$
				: "ModuleDeclarationPattern: "); //$NON-NLS-1$
		} else {
			output.append("ModuleReferencePattern: "); //$NON-NLS-1$
		}
		output.append("module "); //$NON-NLS-1$
		output.append(this.name);
		return super.print(output);
	}
}
