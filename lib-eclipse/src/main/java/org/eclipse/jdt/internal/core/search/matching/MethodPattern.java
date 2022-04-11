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

import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.internal.core.index.*;
import org.eclipse.jdt.internal.core.util.Util;

public class MethodPattern extends JavaSearchPattern {

protected boolean findDeclarations = true;
protected boolean findReferences = true;

public char[] selector;

public char[] declaringQualification;
public char[] declaringSimpleName;
public char[] declaringPackageName; //set only when focus is not null

public char[] returnQualification;
public char[] returnSimpleName;

public char[][] parameterQualifications;
public char[][] parameterSimpleNames;
public int parameterCount;
public boolean varargs = false;

// extra reference info
protected IType declaringType;

// Signatures and arguments for generic search
char[][] returnTypeSignatures;
char[][][] parametersTypeSignatures;
char[][][][] parametersTypeArguments;
boolean methodParameters = false;
char[][] methodArguments;

protected static char[][] REF_CATEGORIES = { METHOD_REF };
protected static char[][] REF_AND_DECL_CATEGORIES = { METHOD_REF, METHOD_DECL };
protected static char[][] DECL_CATEGORIES = { METHOD_DECL };

public final static int FINE_GRAIN_MASK =
	IJavaSearchConstants.SUPER_REFERENCE |
	IJavaSearchConstants.QUALIFIED_REFERENCE |
	IJavaSearchConstants.THIS_REFERENCE |
	IJavaSearchConstants.IMPLICIT_THIS_REFERENCE |
	IJavaSearchConstants.METHOD_REFERENCE_EXPRESSION;

/**
 * Method entries are encoded as selector '/' Arity:
 * e.g. 'foo/0'
 */
public static char[] createIndexKey(char[] selector, int argCount) {
	char[] countChars = argCount < 10
		? COUNTS[argCount]
		: ("/" + String.valueOf(argCount)).toCharArray(); //$NON-NLS-1$
	return CharOperation.concat(selector, countChars);
}

MethodPattern(int matchRule) {
	super(METHOD_PATTERN, matchRule);
}
public MethodPattern(
	char[] selector,
	char[] declaringQualification,
	char[] declaringSimpleName,
	char[] returnQualification,
	char[] returnSimpleName,
	char[][] parameterQualifications,
	char[][] parameterSimpleNames,
	IType declaringType,
	int limitTo,
	int matchRule) {

	this(matchRule);

	this.fineGrain = limitTo & FINE_GRAIN_MASK;
    if (this.fineGrain == 0) {
		switch (limitTo & 0xF) {
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

	this.selector = (this.isCaseSensitive || this.isCamelCase) ? selector : CharOperation.toLowerCase(selector);
	this.declaringQualification = this.isCaseSensitive ? declaringQualification : CharOperation.toLowerCase(declaringQualification);
	this.declaringSimpleName = this.isCaseSensitive ? declaringSimpleName : CharOperation.toLowerCase(declaringSimpleName);
	this.returnQualification = this.isCaseSensitive ? returnQualification : CharOperation.toLowerCase(returnQualification);
	this.returnSimpleName = this.isCaseSensitive ? returnSimpleName : CharOperation.toLowerCase(returnSimpleName);
	if (parameterSimpleNames != null) {
		this.parameterCount = parameterSimpleNames.length;
		this.parameterQualifications = new char[this.parameterCount][];
		this.parameterSimpleNames = new char[this.parameterCount][];
		for (int i = 0; i < this.parameterCount; i++) {
			this.parameterQualifications[i] = this.isCaseSensitive ? parameterQualifications[i] : CharOperation.toLowerCase(parameterQualifications[i]);
			this.parameterSimpleNames[i] = this.isCaseSensitive ? parameterSimpleNames[i] : CharOperation.toLowerCase(parameterSimpleNames[i]);
		}
	} else {
		this.parameterCount = -1;
	}
	this.declaringType = declaringType;
	if (this.declaringType !=  null) {
		this.declaringPackageName = this.declaringType.getPackageFragment().getElementName().toCharArray();
	}
	this.mustResolve = mustResolve();
}
/*
 * Instanciate a method pattern with signatures for generics search
 */
public MethodPattern(
	char[] selector,
	char[] declaringQualification,
	char[] declaringSimpleName,
	char[] returnQualification,
	char[] returnSimpleName,
	String returnSignature,
	char[][] parameterQualifications,
	char[][] parameterSimpleNames,
	String[] parameterSignatures,
	IMethod method,
	int limitTo,
	int matchRule) {

	this(selector,
		declaringQualification,
		declaringSimpleName,
		returnQualification,
		returnSimpleName,
		parameterQualifications,
		parameterSimpleNames,
		method.getDeclaringType(),
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
		this.methodParameters = true;
		storeTypeSignaturesAndArguments(this.declaringType);
	}

	// Store type signatures and arguments for return type
	if (returnSignature != null) {
		this.returnTypeSignatures = Util.splitTypeLevelsSignature(returnSignature);
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
	this.methodArguments = extractMethodArguments(method);
	if (hasMethodArguments())  this.mustResolve = true;
}
/*
 * Instanciate a method pattern with signatures for generics search
 */
public MethodPattern(
	char[] selector,
	char[] declaringQualification,
	char[] declaringSimpleName,
	String declaringSignature,
	char[] returnQualification,
	char[] returnSimpleName,
	String returnSignature,
	char[][] parameterQualifications,
	char[][] parameterSimpleNames,
	String[] parameterSignatures,
	char[][] arguments,
	int limitTo,
	int matchRule) {

	this(selector,
		declaringQualification,
		declaringSimpleName,
		returnQualification,
		returnSimpleName,
		parameterQualifications,
		parameterSimpleNames,
		null,
		limitTo,
		matchRule);

	// Store type signature and arguments for declaring type
	if (declaringSignature != null) {
		this.typeSignatures = Util.splitTypeLevelsSignature(declaringSignature);
		setTypeArguments(Util.getAllTypeArguments(this.typeSignatures));
	}

	// Store type signatures and arguments for return type
	if (returnSignature != null) {
		this.returnTypeSignatures = Util.splitTypeLevelsSignature(returnSignature);
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
	this.methodArguments = arguments;
	if (hasMethodArguments())  this.mustResolve = true;
}
@Override
public void decodeIndexKey(char[] key) {
	int last = key.length - 1;
	this.parameterCount = 0;
	this.selector = null;
	int power = 1;
	for (int i=last; i>=0; i--) {
		if (key[i] == SEPARATOR) {
			System.arraycopy(key, 0, this.selector = new char[i], 0, i);
			break;
		}
		if (i == last) {
			this.parameterCount = key[i] - '0';
		} else {
			power *= 10;
			this.parameterCount += power * (key[i] - '0');
		}
	}
}
@Override
public SearchPattern getBlankPattern() {
	return new MethodPattern(R_EXACT_MATCH | R_CASE_SENSITIVE);
}
@Override
public char[][] getIndexCategories() {
	if (this.findReferences)
		return this.findDeclarations ? REF_AND_DECL_CATEGORIES : REF_CATEGORIES;
	if (this.findDeclarations)
		return DECL_CATEGORIES;
	return CharOperation.NO_CHAR_CHAR;
}
boolean hasMethodArguments() {
	return this.methodArguments != null && this.methodArguments.length > 0;
}
boolean hasMethodParameters() {
	return this.methodParameters;
}
@Override
public boolean isPolymorphicSearch() {
	return this.findReferences;
}
@Override
public boolean matchesDecodedKey(SearchPattern decodedPattern) {
	MethodPattern pattern = (MethodPattern) decodedPattern;

	return (this.parameterCount == pattern.parameterCount || this.parameterCount == -1 || this.varargs)
		&& matchesName(this.selector, pattern.selector);
}
/**
 * Returns whether a method declaration or message send must be resolved to
 * find out if this method pattern matches it.
 */
protected boolean mustResolve() {
	// declaring type
	// If declaring type is specified - even with simple name - always resolves
	if (this.declaringSimpleName != null || this.declaringQualification != null) return true;

	// return type
	// If return type is specified - even with simple name - always resolves
	if (this.returnSimpleName != null || this.returnQualification != null) return true;

	// parameter types
	if (this.parameterSimpleNames != null)
		for (int i = 0, max = this.parameterSimpleNames.length; i < max; i++)
			if (this.parameterQualifications[i] != null) return true;
	return false;
}
@Override
public EntryResult[] queryIn(Index index) throws IOException {
	char[] key = this.selector; // can be null
	int matchRule = getMatchRule();

	switch(getMatchMode()) {
		case R_EXACT_MATCH :
			if (this.selector != null && this.parameterCount >= 0 && !this.varargs)
				key = createIndexKey(this.selector, this.parameterCount);
			else { // do a prefix query with the selector
				matchRule &= ~R_EXACT_MATCH;
				matchRule |= R_PREFIX_MATCH;
			}
			break;
		case R_PREFIX_MATCH :
			// do a prefix query with the selector
			break;
		case R_PATTERN_MATCH :
			if (this.parameterCount >= 0 && !this.varargs)
				key = createIndexKey(this.selector == null ? ONE_STAR : this.selector, this.parameterCount);
			else if (this.selector != null && this.selector[this.selector.length - 1] != '*')
				key = CharOperation.concat(this.selector, ONE_STAR, SEPARATOR);
			// else do a pattern query with just the selector
			break;
		case R_REGEXP_MATCH :
			// TODO (frederic) implement regular expression match
			break;
		case R_CAMELCASE_MATCH:
		case R_CAMELCASE_SAME_PART_COUNT_MATCH:
			// do a prefix query with the selector
			break;
	}

	return index.query(getIndexCategories(), key, matchRule); // match rule is irrelevant when the key is null
}
@Override
protected StringBuffer print(StringBuffer output) {
	if (this.findDeclarations) {
		output.append(this.findReferences
			? "MethodCombinedPattern: " //$NON-NLS-1$
			: "MethodDeclarationPattern: "); //$NON-NLS-1$
	} else {
		output.append("MethodReferencePattern: "); //$NON-NLS-1$
	}
	if (this.declaringQualification != null)
		output.append(this.declaringQualification).append('.');
	if (this.declaringSimpleName != null)
		output.append(this.declaringSimpleName).append('.');
	else if (this.declaringQualification != null)
		output.append("*."); //$NON-NLS-1$

	if (this.selector != null)
		output.append(this.selector);
	else
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
	if (this.returnQualification != null)
		output.append(" --> ").append(this.returnQualification).append('.'); //$NON-NLS-1$
	else if (this.returnSimpleName != null)
		output.append(" --> "); //$NON-NLS-1$
	if (this.returnSimpleName != null)
		output.append(this.returnSimpleName);
	else if (this.returnQualification != null)
		output.append("*"); //$NON-NLS-1$
	return super.print(output);
}
}
