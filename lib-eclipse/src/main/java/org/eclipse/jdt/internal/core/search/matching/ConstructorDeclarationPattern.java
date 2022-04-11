/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
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

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.internal.compiler.ExtraFlags;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;

public class ConstructorDeclarationPattern extends ConstructorPattern {
	public int extraFlags;

	public int declaringTypeModifiers;
	public char[] declaringPackageName;

	public int modifiers;
	public char[] signature;
	public char[][] parameterTypes;
	public char[][] parameterNames;

public ConstructorDeclarationPattern(char[] declaringPackageName, char[] declaringSimpleName, int matchRule) {
	this(matchRule);
	this.declaringSimpleName = (this.isCaseSensitive || this.isCamelCase) ? declaringSimpleName : CharOperation.toLowerCase(declaringSimpleName);
	this.declaringPackageName = declaringPackageName;
	this.findDeclarations = true;
	this.findReferences = false;
	this.parameterCount = -1;
	this.mustResolve = false;
}

ConstructorDeclarationPattern(int matchRule) {
	super(matchRule);
}
@Override
public void decodeIndexKey(char[] key) {
	int last = key.length - 1;
	int slash = CharOperation.indexOf(SEPARATOR, key, 0);
	this.declaringSimpleName = CharOperation.subarray(key, 0, slash);

	int start = slash + 1;
	slash = CharOperation.indexOf(SEPARATOR, key, start);
	last = slash - 1;

	boolean isDefaultConstructor = key[last] == '#';
	if (isDefaultConstructor) {
		this.parameterCount = -1;
	} else {
		this.parameterCount = 0;
		int power = 1;
		for (int i = last; i >= start; i--) {
			if (i == last) {
				this.parameterCount = key[i] - '0';
			} else {
				power *= 10;
				this.parameterCount += power * (key[i] - '0');
			}
		}
	}

	slash = slash + 3;
	last = slash - 1;

	int typeModifiersWithExtraFlags = key[last-1] + (key[last]<<16);
	this.declaringTypeModifiers = decodeModifers(typeModifiersWithExtraFlags);
	this.extraFlags = decodeExtraFlags(typeModifiersWithExtraFlags);

	// initialize optional fields
	this.declaringPackageName = null;
	this.modifiers = 0;
	this.signature = null;
	this.parameterTypes = null;
	this.parameterNames = null;

	boolean isMemberType = (this.extraFlags & ExtraFlags.IsMemberType) != 0;

	if (!isMemberType) {
		start = slash + 1;
		if (this.parameterCount == -1) {
			slash = key.length;
			last = slash - 1;
		} else {
			slash = CharOperation.indexOf(SEPARATOR, key, start);
		}
		last = slash - 1;

		this.declaringPackageName = CharOperation.subarray(key, start, slash);

		start = slash + 1;
		if (this.parameterCount == 0) {
			slash = slash + 3;
			last = slash - 1;

			this.modifiers = key[last-1] + (key[last]<<16);
		} else if (this.parameterCount > 0){
			slash = CharOperation.indexOf(SEPARATOR, key, start);
			last = slash - 1;

			boolean hasParameterStoredAsSignature = (this.extraFlags & ExtraFlags.ParameterTypesStoredAsSignature) != 0;
			if (hasParameterStoredAsSignature) {
				this.signature  = CharOperation.subarray(key, start, slash);
				CharOperation.replace(this.signature , '\\', SEPARATOR);
			} else {
				this.parameterTypes = CharOperation.splitOn(PARAMETER_SEPARATOR, key, start, slash);
			}
			start = slash + 1;
			slash = CharOperation.indexOf(SEPARATOR, key, start);
			last = slash - 1;

			if (slash != start) {
				this.parameterNames = CharOperation.splitOn(PARAMETER_SEPARATOR, key, start, slash);
			}

			slash = slash + 3;
			last = slash - 1;

			this.modifiers = key[last-1] + (key[last]<<16);
		} else {
			this.modifiers = ClassFileConstants.AccPublic;
		}
	}

	removeInternalFlags(); // remove internal flags
}

@Override
public SearchPattern getBlankPattern() {
	return new ConstructorDeclarationPattern(R_EXACT_MATCH | R_CASE_SENSITIVE);
}
@Override
public char[][] getIndexCategories() {
	return DECL_CATEGORIES;
}
@Override
public boolean matchesDecodedKey(SearchPattern decodedPattern) {
	ConstructorDeclarationPattern pattern = (ConstructorDeclarationPattern) decodedPattern;

	// only top level types
	if ((pattern.extraFlags & ExtraFlags.IsMemberType) != 0) return false;

	// check package - exact match only
	if (this.declaringPackageName != null && !CharOperation.equals(this.declaringPackageName, pattern.declaringPackageName, true))
		return false;

	return (this.parameterCount == pattern.parameterCount || this.parameterCount == -1 || this.varargs)
		&& matchesName(this.declaringSimpleName, pattern.declaringSimpleName);
}
private void removeInternalFlags() {
	this.extraFlags = this.extraFlags & ~ExtraFlags.ParameterTypesStoredAsSignature; // ParameterTypesStoredAsSignature is an internal flags only used to decode key
}
}
