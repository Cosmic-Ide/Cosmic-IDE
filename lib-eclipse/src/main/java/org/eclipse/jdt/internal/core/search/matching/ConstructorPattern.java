/*******************************************************************************
 * Copyright (c) 2000, 2014 IBM Corporation and others.
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

import org.eclipse.jdt.core.BindingKey;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.internal.compiler.ExtraFlags;
import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.core.index.EntryResult;
import org.eclipse.jdt.internal.core.index.Index;
import org.eclipse.jdt.internal.core.util.Util;

public class ConstructorPattern extends JavaSearchPattern {

protected boolean findDeclarations = true;
protected boolean findReferences = true;

public char[] declaringQualification;
public char[] declaringSimpleName;

public char[][] parameterQualifications;
public char[][] parameterSimpleNames;
public int parameterCount;
public boolean varargs = false;

// Signatures and arguments for generic search
char[][][] parametersTypeSignatures;
char[][][][] parametersTypeArguments;
boolean constructorParameters = false;
char[][] constructorArguments;

protected static char[][] REF_CATEGORIES = { CONSTRUCTOR_REF };
protected static char[][] REF_AND_DECL_CATEGORIES = { CONSTRUCTOR_REF, CONSTRUCTOR_DECL };
protected static char[][] DECL_CATEGORIES = { CONSTRUCTOR_DECL };

public final static int FINE_GRAIN_MASK =
	IJavaSearchConstants.SUPER_REFERENCE |
	IJavaSearchConstants.QUALIFIED_REFERENCE |
	IJavaSearchConstants.THIS_REFERENCE |
	IJavaSearchConstants.IMPLICIT_THIS_REFERENCE |
	IJavaSearchConstants.METHOD_REFERENCE_EXPRESSION;


/**
 * Constructor entries are encoded as described
 *
 * Binary constructor for class
 * TypeName '/' Arity '/' TypeModifers '/' PackageName '/' Signature '/' ParameterNamesopt '/' Modifiers
 * Source constructor for class
 * TypeName '/' Arity '/' TypeModifers '/' PackageName '/' ParameterTypes '/' ParameterNamesopt '/' Modifiers
 * Constructor with 0 arity for class
 * TypeName '/' 0 '/' TypeModifers '/' PackageName '/' Modifiers
 * Constructor for enum, interface (annotation) and class with default constructor
 * TypeName '/' # '/' TypeModifers '/' PackageName
 * Constructor for member type
 * TypeName '/' Arity '/' TypeModifers
 *
 * TypeModifiers contains some encoded extra information
 * 		{@link ExtraFlags#IsMemberType}
 * 		{@link ExtraFlags#HasNonPrivateStaticMemberTypes}
 * 		{@link ExtraFlags#ParameterTypesStoredAsSignature}
 */
public static char[] createDeclarationIndexKey(
		char[] typeName,
		int argCount,
		char[] signature,
		char[][] parameterTypes,
		char[][] parameterNames,
		int modifiers,
		char[] packageName,
		int typeModifiers,
		int extraFlags) {

	char[] countChars;
	char[] parameterTypesChars = null;
	char[] parameterNamesChars = null;

	if (argCount < 0) {
		countChars = DEFAULT_CONSTRUCTOR;
	} else {
		countChars = argCount < 10
		? COUNTS[argCount]
		: ("/" + String.valueOf(argCount)).toCharArray(); //$NON-NLS-1$

		if (argCount > 0) {
			if (signature == null) {
				if (parameterTypes != null && parameterTypes.length == argCount) {
					char[][] parameterTypeErasures = new char[argCount][];
					for (int i = 0; i < parameterTypes.length; i++) {
						parameterTypeErasures[i] = getTypeErasure(parameterTypes[i]);
					}
					parameterTypesChars = CharOperation.concatWith(parameterTypeErasures, PARAMETER_SEPARATOR);
				}
			} else {
				extraFlags |= ExtraFlags.ParameterTypesStoredAsSignature;
			}

			if (parameterNames != null && parameterNames.length == argCount) {
				parameterNamesChars = CharOperation.concatWith(parameterNames, PARAMETER_SEPARATOR);
			}
		}
	}

	boolean isMemberType = (extraFlags & ExtraFlags.IsMemberType) != 0;

	int typeNameLength = typeName == null ? 0 : typeName.length;
	int packageNameLength = packageName == null ? 0 : packageName.length;
	int countCharsLength = countChars.length;
	int parameterTypesLength = signature == null ? (parameterTypesChars == null ? 0 : parameterTypesChars.length): signature.length;
	int parameterNamesLength = parameterNamesChars == null ? 0 : parameterNamesChars.length;

	int resultLength = typeNameLength + countCharsLength + 3; // SEPARATOR=1 + TypeModifers=2
	if (!isMemberType) {
		resultLength += packageNameLength + 1; // SEPARATOR=1
		if (argCount >= 0) {
			resultLength += 3; // SEPARATOR=1 + Modifiers=2
		}

		if (argCount > 0) {
			resultLength += parameterTypesLength + parameterNamesLength + 2; //SEPARATOR=1 + SEPARATOR=1
		}
	}

	char[] result = new char[resultLength];

	int pos = 0;
	if (typeNameLength > 0) {
		System.arraycopy(typeName, 0, result, pos, typeNameLength);
		pos += typeNameLength;
	}

	if (countCharsLength > 0) {
		System.arraycopy(countChars, 0, result, pos, countCharsLength);
		pos += countCharsLength;
	}

	int typeModifiersWithExtraFlags = typeModifiers | encodeExtraFlags(extraFlags);
	result[pos++] = SEPARATOR;
	result[pos++] = (char) typeModifiersWithExtraFlags;
	result[pos++] = (char) (typeModifiersWithExtraFlags>>16);

	if (!isMemberType) {
		result[pos++] = SEPARATOR;
		if (packageNameLength > 0) {
			System.arraycopy(packageName, 0, result, pos, packageNameLength);
			pos += packageNameLength;
		}

		if (argCount == 0) {
			result[pos++] = SEPARATOR;
			result[pos++] = (char) modifiers;
			result[pos++] = (char) (modifiers>>16);
		} else if (argCount > 0) {
			result[pos++] = SEPARATOR;
			if (parameterTypesLength > 0) {
				if (signature == null) {
					System.arraycopy(parameterTypesChars, 0, result, pos, parameterTypesLength);
				} else {
					System.arraycopy(CharOperation.replaceOnCopy(signature, SEPARATOR, '\\'), 0, result, pos, parameterTypesLength);
				}
				pos += parameterTypesLength;
			}

			result[pos++] = SEPARATOR;
			if (parameterNamesLength > 0) {
				System.arraycopy(parameterNamesChars, 0, result, pos, parameterNamesLength);
				pos += parameterNamesLength;
			}

			result[pos++] = SEPARATOR;
			result[pos++] = (char) modifiers;
			result[pos++] = (char) (modifiers>>16);
		}

	}

	return result;
}
public static char[] createDefaultDeclarationIndexKey(
		char[] typeName,
		char[] packageName,
		int typeModifiers,
		int extraFlags) {
	return createDeclarationIndexKey(
			typeName,
			-1, // used to identify default constructor
			null,
			null,
			null,
			0, //
			packageName,
			typeModifiers,
			extraFlags);
}

/**
 * Constructor entries are encoded as TypeName '/' Arity:
 * e.g. 'X/0'
 */
public static char[] createIndexKey(char[] typeName, int argCount) {
	char[] countChars = argCount < 10
		? COUNTS[argCount]
		: ("/" + String.valueOf(argCount)).toCharArray(); //$NON-NLS-1$
	return CharOperation.concat(typeName, countChars);
}
static int decodeExtraFlags(int modifiersWithExtraFlags) {
	int extraFlags = 0;

	if ((modifiersWithExtraFlags & ASTNode.Bit28) != 0) {
		extraFlags |= ExtraFlags.ParameterTypesStoredAsSignature;
	}

	if ((modifiersWithExtraFlags & ASTNode.Bit29) != 0) {
		extraFlags |= ExtraFlags.IsLocalType;
	}

	if ((modifiersWithExtraFlags & ASTNode.Bit30) != 0) {
		extraFlags |= ExtraFlags.IsMemberType;
	}

	if ((modifiersWithExtraFlags & ASTNode.Bit31) != 0) {
		extraFlags |= ExtraFlags.HasNonPrivateStaticMemberTypes;
	}

	return extraFlags;
}
static int decodeModifers(int modifiersWithExtraFlags) {
	return modifiersWithExtraFlags & ~(ASTNode.Bit31 | ASTNode.Bit30 | ASTNode.Bit29 | ASTNode.Bit28);
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
ConstructorPattern(int matchRule) {
	super(CONSTRUCTOR_PATTERN, matchRule);
}
public ConstructorPattern(
	char[] declaringSimpleName,
	char[] declaringQualification,
	char[][] parameterQualifications,
	char[][] parameterSimpleNames,
	int limitTo,
	int matchRule) {

	this(matchRule);

	this.fineGrain = limitTo & FINE_GRAIN_MASK;
    if (this.fineGrain == 0) {
		switch (limitTo) {
			case IJavaSearchConstants.DECLARATIONS :
				this.findReferences = false;
				break;
			case IJavaSearchConstants.REFERENCES :
				this.findDeclarations = false;
				break;
			case IJavaSearchConstants.ALL_OCCURRENCES :
				break;
		}
    } else {
		this.findDeclarations = false;
    }

	this.declaringQualification = this.isCaseSensitive ? declaringQualification : CharOperation.toLowerCase(declaringQualification);
	this.declaringSimpleName = (this.isCaseSensitive || this.isCamelCase) ? declaringSimpleName : CharOperation.toLowerCase(declaringSimpleName);
	if (parameterSimpleNames != null) {
		this.parameterCount = parameterSimpleNames.length;
		boolean synthetic = this.parameterCount>0 && declaringQualification != null && CharOperation.equals(CharOperation.concat(parameterQualifications[0], parameterSimpleNames[0], '.'), declaringQualification);
		int offset = 0;
		if (synthetic) {
			// skip first synthetic parameter
			this.parameterCount--;
			offset++;
		}
		this.parameterQualifications = new char[this.parameterCount][];
		this.parameterSimpleNames = new char[this.parameterCount][];
		for (int i = 0; i < this.parameterCount; i++) {
			this.parameterQualifications[i] = this.isCaseSensitive ? parameterQualifications[i+offset] : CharOperation.toLowerCase(parameterQualifications[i+offset]);
			this.parameterSimpleNames[i] = this.isCaseSensitive ? parameterSimpleNames[i+offset] : CharOperation.toLowerCase(parameterSimpleNames[i+offset]);
		}
	} else {
		this.parameterCount = -1;
	}
	this.mustResolve = mustResolve();
}
/*
 * Instantiate a method pattern with signatures for generics search
 */
public ConstructorPattern(
	char[] declaringSimpleName,
	char[] declaringQualification,
	char[][] parameterQualifications,
	char[][] parameterSimpleNames,
	String[] parameterSignatures,
	IMethod method,
	int limitTo,
	int matchRule) {

	this(declaringSimpleName,
		declaringQualification,
		parameterQualifications,
		parameterSimpleNames,
		limitTo,
		matchRule);

	// Set flags
	try {
		this.varargs = (method.getFlags() & Flags.AccVarargs) != 0;
	} catch (JavaModelException e) {
		// do nothing
	}

	// Get unique key for parameterized constructors
	String genericDeclaringTypeSignature = null;
	if (method.isResolved()) {
		String key = method.getKey();
		BindingKey bindingKey = new BindingKey(key);
		if (bindingKey.isParameterizedType()) {
			genericDeclaringTypeSignature = Util.getDeclaringTypeSignature(key);
			// Store type signature and arguments for declaring type
			if (genericDeclaringTypeSignature != null) {
					this.typeSignatures = Util.splitTypeLevelsSignature(genericDeclaringTypeSignature);
					setTypeArguments(Util.getAllTypeArguments(this.typeSignatures));
			}
		}
	} else {
		this.constructorParameters = true;
		storeTypeSignaturesAndArguments(method.getDeclaringType());
	}

	// store type signatures and arguments for method parameters type
	if (parameterSignatures != null) {
		int length = parameterSignatures.length;
		if (length > 0) {
			this.parametersTypeSignatures = new char[length][][];
			this.parametersTypeArguments = new char[length][][][];
			for (int i=0; i<length; i++) {
				this.parametersTypeSignatures[i] = Util.splitTypeLevelsSignature(parameterSignatures[i]);
				this.parametersTypeArguments[i] = Util.getAllTypeArguments(this.parametersTypeSignatures[i]);
			}
		}
	}

	// Store type signatures and arguments for method
	this.constructorArguments = extractMethodArguments(method);
	if (hasConstructorArguments())  this.mustResolve = true;
}
/*
 * Instantiate a method pattern with signatures for generics search
 */
public ConstructorPattern(
	char[] declaringSimpleName,
	char[] declaringQualification,
	String declaringSignature,
	char[][] parameterQualifications,
	char[][] parameterSimpleNames,
	String[] parameterSignatures,
	char[][] arguments,
	int limitTo,
	int matchRule) {

	this(declaringSimpleName,
		declaringQualification,
		parameterQualifications,
		parameterSimpleNames,
		limitTo,
		matchRule);

	// Store type signature and arguments for declaring type
	if (declaringSignature != null) {
		this.typeSignatures = Util.splitTypeLevelsSignature(declaringSignature);
		setTypeArguments(Util.getAllTypeArguments(this.typeSignatures));
	}

	// Store type signatures and arguments for method parameters type
	if (parameterSignatures != null) {
		int length = parameterSignatures.length;
		if (length > 0) {
			this.parametersTypeSignatures = new char[length][][];
			this.parametersTypeArguments = new char[length][][][];
			for (int i=0; i<length; i++) {
				this.parametersTypeSignatures[i] = Util.splitTypeLevelsSignature(parameterSignatures[i]);
				this.parametersTypeArguments[i] = Util.getAllTypeArguments(this.parametersTypeSignatures[i]);
			}
		}
	}

	// Store type signatures and arguments for method
	this.constructorArguments = arguments;
	if (arguments  == null || arguments.length == 0) {
		if (getTypeArguments() != null && getTypeArguments().length > 0) {
			this.constructorArguments = getTypeArguments()[0];
		}
	}
	if (hasConstructorArguments())  this.mustResolve = true;
}

@Override
public void decodeIndexKey(char[] key) {
	int last = key.length - 1;
	int slash = CharOperation.indexOf(SEPARATOR, key, 0);
	this.declaringSimpleName = CharOperation.subarray(key, 0, slash);

	int start = slash + 1;
	slash = CharOperation.indexOf(SEPARATOR, key, start);
	if (slash != -1) {
		last = slash - 1;
	}

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
}
@Override
public SearchPattern getBlankPattern() {
	return new ConstructorPattern(R_EXACT_MATCH | R_CASE_SENSITIVE);
}
@Override
public char[][] getIndexCategories() {
	if (this.findReferences)
		return this.findDeclarations ? REF_AND_DECL_CATEGORIES : REF_CATEGORIES;
	if (this.findDeclarations)
		return DECL_CATEGORIES;
	return CharOperation.NO_CHAR_CHAR;
}
boolean hasConstructorArguments() {
	return this.constructorArguments != null && this.constructorArguments.length > 0;
}
boolean hasConstructorParameters() {
	return this.constructorParameters;
}
@Override
public boolean matchesDecodedKey(SearchPattern decodedPattern) {
	ConstructorPattern pattern = (ConstructorPattern) decodedPattern;

	return pattern.parameterCount != -1
		&& (this.parameterCount == pattern.parameterCount || this.parameterCount == -1 || this.varargs)
		&& matchesName(this.declaringSimpleName, pattern.declaringSimpleName);
}
protected boolean mustResolve() {
	if (this.declaringQualification != null) return true;

	// parameter types
	if (this.parameterSimpleNames != null)
		for (int i = 0, max = this.parameterSimpleNames.length; i < max; i++)
			if (this.parameterQualifications[i] != null) return true;
	return this.findReferences; // need to check resolved default constructors and explicit constructor calls
}
@Override
public EntryResult[] queryIn(Index index) throws IOException {
	char[] key = this.declaringSimpleName; // can be null
	int matchRule = getMatchRule();

	switch(getMatchMode()) {
		case R_EXACT_MATCH :
			if (this.declaringSimpleName != null && this.parameterCount >= 0 && !this.varargs) {
				key = createIndexKey(this.declaringSimpleName, this.parameterCount);
			}
			matchRule &= ~R_EXACT_MATCH;
			matchRule |= R_PREFIX_MATCH;
			break;
		case R_PREFIX_MATCH :
			// do a prefix query with the declaringSimpleName
			break;
		case R_PATTERN_MATCH :
			if (this.parameterCount >= 0 && !this.varargs) {
				key = CharOperation.concat(createIndexKey(this.declaringSimpleName == null ? ONE_STAR : this.declaringSimpleName, this.parameterCount), ONE_STAR);
			} else if (this.declaringSimpleName != null && this.declaringSimpleName[this.declaringSimpleName.length - 1] != '*') {
				key = CharOperation.concat(this.declaringSimpleName, ONE_STAR, SEPARATOR);
			} else if (key != null){
				key = CharOperation.concat(key, ONE_STAR);
			}
			// else do a pattern query with just the declaringSimpleName
			break;
		case R_REGEXP_MATCH :
			// TODO (frederic) implement regular expression match
			break;
		case R_CAMELCASE_MATCH:
		case R_CAMELCASE_SAME_PART_COUNT_MATCH:
			// do a prefix query with the declaringSimpleName
			break;
	}

	return index.query(getIndexCategories(), key, matchRule); // match rule is irrelevant when the key is null
}
@Override
protected StringBuffer print(StringBuffer output) {
	if (this.findDeclarations) {
		output.append(this.findReferences
			? "ConstructorCombinedPattern: " //$NON-NLS-1$
			: "ConstructorDeclarationPattern: "); //$NON-NLS-1$
	} else {
		output.append("ConstructorReferencePattern: "); //$NON-NLS-1$
	}
	if (this.declaringQualification != null)
		output.append(this.declaringQualification).append('.');
	if (this.declaringSimpleName != null)
		output.append(this.declaringSimpleName);
	else if (this.declaringQualification != null)
		output.append("*"); //$NON-NLS-1$

	output.append('(');
	if (this.parameterSimpleNames == null) {
		output.append("..."); //$NON-NLS-1$
	} else {
		for (int i = 0, max = this.parameterSimpleNames.length; i < max; i++) {
			if (i > 0) output.append(", "); //$NON-NLS-1$
			if (this.parameterQualifications[i] != null) output.append(this.parameterQualifications[i]).append('.');
			if (this.parameterSimpleNames[i] == null) output.append('*'); else output.append(this.parameterSimpleNames[i]);
		}
	}
	output.append(')');
	return super.print(output);
}
}
