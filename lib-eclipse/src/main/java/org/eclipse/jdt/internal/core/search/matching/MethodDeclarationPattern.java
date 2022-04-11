/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
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
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.internal.compiler.ExtraFlags;
import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;

public class MethodDeclarationPattern extends MethodPattern {

	public int extraFlags;
	public int declaringTypeModifiers;

	public int modifiers;
	public char[] signature;
	public char[][] parameterTypes;
	public char[][] parameterNames;
	public char[] fusedDeclaringQualifier = null; // TODO: do we need this; cleanup?
	/**
	 * Method Declaration entries are encoded as described
	 *
	 * Binary Method Declaration for class
	 * MethodName '/' Arity '/' DeclaringQualifier '/' TypeName '/' TypeModifers '/' PackageName '/' Signature '/' ParameterNamesopt '/' Modifiers '/' returnType
	 * Source method for class
	 * MethodName '/' Arity '/' DeclaringQualifier '/' TypeName '/' TypeModifers '/' PackageName '/' ParameterTypes '/' ParameterNamesopt '/' Modifiers '/' returnType
	 * TypeModifiers contains some encoded extra information
	 * 		{@link ExtraFlags#IsMemberType}
	 * 		{@link ExtraFlags#HasNonPrivateStaticMemberTypes}
	 * 		{@link ExtraFlags#ParameterTypesStoredAsSignature}
	 */
	public static char[] createDeclarationIndexKey(
			char[] typeName,
			char[] declaringQualification,
			char[] methodName,
			int argCount,
			char[] signature,
			char[][] parameterTypes,
			char[][] parameterNames,
			char[] returnType,
			int modifiers,
			char[] packageName,
			int typeModifiers,
			int extraFlags) {

		char[] countChars;
		char[] parameterTypesChars = null;
		char[] parameterNamesChars = null;


		countChars = argCount < 10 ? new char[] {COUNTS[argCount][1]}:  String.valueOf(argCount).toCharArray();
		if (argCount > 0) {
			if (signature == null) {
				if (parameterTypes != null && parameterTypes.length == argCount) {
					parameterTypesChars = CharOperation.concatWith(parameterTypes, PARAMETER_SEPARATOR);
				}
			} else {
				extraFlags |= ExtraFlags.ParameterTypesStoredAsSignature;
			}
			if (parameterNames != null && parameterNames.length == argCount) {
				parameterNamesChars = CharOperation.concatWith(parameterNames, PARAMETER_SEPARATOR);
			}
		}

		char[] returnTypeChars = returnType == null ? CharOperation.NO_CHAR : getTypeErasure(returnType);
		int typeModifiersWithExtraFlags = typeModifiers | encodeExtraFlags(extraFlags);
		int entryIndex = 0;
		int numEntries = 10;
		char [][] tmp = new char[numEntries][];

		tmp[entryIndex++] = methodName != null ? methodName : CharOperation.NO_CHAR;
		tmp[entryIndex++] = countChars;
		tmp[entryIndex++] = declaringQualification != null ? declaringQualification : CharOperation.NO_CHAR;
		tmp[entryIndex++] = typeName != null ? typeName : CharOperation.NO_CHAR;
		tmp[entryIndex++] = new char[] {(char) typeModifiersWithExtraFlags, (char) (typeModifiersWithExtraFlags>>16)};
		tmp[entryIndex++] = packageName != null ? packageName : CharOperation.NO_CHAR;

		if (argCount == 0) {
			tmp[entryIndex++] = CharOperation.NO_CHAR;
			tmp[entryIndex++] = CharOperation.NO_CHAR;
		} else if (argCount > 0) {
			tmp[entryIndex++] = signature != null ? CharOperation.replaceOnCopy(signature, SEPARATOR, '\\') : parameterTypesChars != null ? parameterTypesChars  : CharOperation.NO_CHAR;
			tmp[entryIndex++] = parameterNamesChars != null ? parameterNamesChars : CharOperation.NO_CHAR;
		}
		tmp[entryIndex++] = new char[] {(char) modifiers, (char) (modifiers>>16)};
		tmp[entryIndex] = returnTypeChars;
		return CharOperation.concatWithAll(tmp, '/');
	}

	private static int encodeExtraFlags(int extraFlags) {
		int encodedExtraFlags = 0;

		if ((extraFlags & ExtraFlags.ParameterTypesStoredAsSignature) != 0) {
			encodedExtraFlags |= ASTNode.Bit28;
		}

		if ((extraFlags & ExtraFlags.IsLocalType) != 0) {
			encodedExtraFlags |= ASTNode.Bit29;
		}

		if ((extraFlags & ExtraFlags.IsMemberType) != 0) {
			encodedExtraFlags |= ASTNode.Bit30;
		}
		if ((extraFlags & ExtraFlags.HasNonPrivateStaticMemberTypes) != 0) {
			encodedExtraFlags |= ASTNode.Bit31;
		}

		return encodedExtraFlags;
	}
	private static char[] getTypeErasure(char[] typeName) {
		int index;
		if ((index = CharOperation.indexOf('<', typeName)) == -1) return typeName;

		int length = typeName.length;
		char[] typeErasurename = new char[length - 2];

		System.arraycopy(typeName, 0, typeErasurename, 0, index);

		int depth = 1;
		for (int i = index + 1; i < length; i++) {
			switch (typeName[i]) {
				case '<':
					depth++;
					break;
				case '>':
					depth--;
					break;
				default:
					if (depth == 0) {
						typeErasurename[index++] = typeName[i];
					}
					break;
			}
		}

		System.arraycopy(typeErasurename, 0, typeErasurename = new char[index], 0, index);
		return typeErasurename;
	}

public MethodDeclarationPattern(
		char[] declaringPackageName,
		char[] declaringQualification,
		char[] declaringSimpleName,
		char[] methodName,
		int matchRule) {
	super(methodName, declaringQualification, declaringSimpleName,
			null, null, null, null, null,
			IJavaSearchConstants.DECLARATIONS, matchRule);
	this.declaringPackageName = declaringPackageName;
}

public MethodDeclarationPattern(
		char[] declaringQualifier,
		char[] methodName,
		int matchRule) {
	super(methodName, CharOperation.NO_CHAR, CharOperation.NO_CHAR,
			null, null, null, null, null,
			IJavaSearchConstants.DECLARATIONS, matchRule);
	this.fusedDeclaringQualifier = declaringQualifier;
}

public MethodDeclarationPattern(int matchRule) {
	super(matchRule);
}

@Override
public void decodeIndexKey(char[] key) {

	int start = 0;
	int slash = CharOperation.indexOf(SEPARATOR, key, start);
	this.selector = CharOperation.subarray(key, start, slash);

	start = slash + 1;
	slash = CharOperation.indexOf(SEPARATOR, key, start);
	int last = slash - 1;

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

	start = slash + 1;
	slash = CharOperation.indexOf(SEPARATOR, key, start);
	this.declaringQualification = CharOperation.subarray(key, start, slash);

	start = slash + 1;
	slash = CharOperation.indexOf(SEPARATOR, key, start);
	this.declaringSimpleName = CharOperation.subarray(key, start, slash);

	start = slash + 1;
	slash = CharOperation.indexOf(SEPARATOR, key, start);
	last = slash - 1;
	int typeModifiersWithExtraFlags = key[last-1] + (key[last]<<16);
	this.declaringTypeModifiers = ConstructorPattern.decodeModifers(typeModifiersWithExtraFlags);
	this.extraFlags = ConstructorPattern.decodeExtraFlags(typeModifiersWithExtraFlags);

	// initialize optional fields
	this.declaringPackageName = null;
	this.modifiers = 0;
	this.signature = null;
	this.parameterTypes = null;
	this.parameterNames = null;

	start = slash + 1;
	slash = CharOperation.indexOf(SEPARATOR, key, start);
	this.declaringPackageName = CharOperation.subarray(key, start, slash);

	start = slash + 1;
	slash = CharOperation.indexOf(SEPARATOR, key, start);
	if (this.parameterCount == 0) {
		start = slash + 1;
		slash = CharOperation.indexOf(SEPARATOR, key, start); // skip parameter type/signature

		start = slash + 1;
		slash = CharOperation.indexOf(SEPARATOR, key, start); //skip parameter names

		this.modifiers = key[last-1] + (key[last]<<16);
	} else if (this.parameterCount > 0){

		boolean hasParameterStoredAsSignature = (this.extraFlags & ExtraFlags.ParameterTypesStoredAsSignature) != 0;
		if (hasParameterStoredAsSignature) {
			this.signature  = CharOperation.subarray(key, start, slash);
			CharOperation.replace(this.signature , '\\', SEPARATOR);
		} else {
			this.parameterTypes = CharOperation.splitOnWithEnclosures(PARAMETER_SEPARATOR, '<', '>', key, start, slash);
		}
		start = slash + 1;
		slash = CharOperation.indexOf(SEPARATOR, key, start);

		if (slash != start) {
			this.parameterNames = CharOperation.splitOn(PARAMETER_SEPARATOR, key, start, slash);
		}

		start = slash + 1;
		slash = CharOperation.indexOf(SEPARATOR, key, start);
		last = slash - 1;

		this.modifiers = key[last-1] + (key[last]<<16);
	} else {
		this.modifiers = ClassFileConstants.AccPublic;
	}

	start = slash + 1;
	slash = CharOperation.indexOf(SEPARATOR, key, start);
	this.returnSimpleName = CharOperation.subarray(key, start, slash); //TODO : separate return qualified and simple names - currently stored together in simple name.

	removeInternalFlags(); // remove internal flags
}

	@Override
	public SearchPattern getBlankPattern() {
		return new MethodDeclarationPattern(R_EXACT_MATCH | R_CASE_SENSITIVE);
	}

	@Override
	public char[][] getIndexCategories() {
		return new char[][] { METHOD_DECL_PLUS };
	}

	private void removeInternalFlags() {
		this.extraFlags = this.extraFlags & ~ExtraFlags.ParameterTypesStoredAsSignature; // ParameterTypesStoredAsSignature is an internal flags only used to decode key
	}

}
