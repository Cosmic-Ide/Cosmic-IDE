/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
import java.util.regex.Pattern;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.core.index.*;
import org.eclipse.jdt.internal.core.search.indexing.IIndexConstants;

public class TypeDeclarationPattern extends JavaSearchPattern {

public char[] simpleName;
public char[] pkg;
public char[][] enclosingTypeNames;
public char[][] moduleNames = null;
private boolean allowModuleRegex = false; // enable to try experimental Module Regex Match
/* package */ Pattern[] modulePatterns = null;
public boolean moduleGraph = false;
/* package */ char[][] moduleGraphElements = null;

// set to CLASS_SUFFIX for only matching classes
// set to INTERFACE_SUFFIX for only matching interfaces
// set to ENUM_SUFFIX for only matching enums
// set to ANNOTATION_TYPE_SUFFIX for only matching annotation types
// set to TYPE_SUFFIX for matching both classes and interfaces
public char typeSuffix;
public int modifiers;
public boolean secondary = false;

protected static char[][] CATEGORIES = { TYPE_DECL };

// want to save space by interning the package names for each match
static PackageNameSet internedPackageNames = new PackageNameSet(1001);
static class PackageNameSet {

public char[][] names;
public int elementSize; // number of elements in the table
public int threshold;

PackageNameSet(int size) {
	this.elementSize = 0;
	this.threshold = size; // size represents the expected number of elements
	int extraRoom = (int) (size * 1.5f);
	if (this.threshold == extraRoom)
		extraRoom++;
	this.names = new char[extraRoom][];
}

synchronized char[] add(char[] name) {
	int length = this.names.length;
	int index = CharOperation.hashCode(name) % length;
	char[] current;
	while ((current = this.names[index]) != null) {
		if (CharOperation.equals(current, name)) return current;
		if (++index == length) index = 0;
	}
	this.names[index] = name;

	// assumes the threshold is never equal to the size of the table
	if (++this.elementSize > this.threshold) rehash();
	return name;
}

synchronized void rehash() {
	PackageNameSet newSet = new PackageNameSet(this.elementSize * 2); // double the number of expected elements
	char[] current;
	for (int i = this.names.length; --i >= 0;)
		if ((current = this.names[i]) != null)
			newSet.add(current);

	this.names = newSet.names;
	this.elementSize = newSet.elementSize;
	this.threshold = newSet.threshold;
}
}

/*
 * Create index key for type declaration pattern:
 *		key = typeName / packageName / enclosingTypeName / modifiers
 * or for secondary types
 *		key = typeName / packageName / enclosingTypeName / modifiers / 'S'
 */
public static char[] createIndexKey(int modifiers, char[] typeName, char[] packageName, char[][] enclosingTypeNames, boolean secondary) { //, char typeSuffix) {
	int typeNameLength = typeName == null ? 0 : typeName.length;
	int packageLength = packageName == null ? 0 : packageName.length;
	int enclosingNamesLength = 0;
	if (enclosingTypeNames != null) {
		for (int i = 0, length = enclosingTypeNames.length; i < length;) {
			enclosingNamesLength += enclosingTypeNames[i].length;
			if (++i < length)
				enclosingNamesLength++; // for the '.' separator
		}
	}

	int resultLength = typeNameLength + packageLength + enclosingNamesLength + 5;
	if (secondary) resultLength += 2;
	char[] result = new char[resultLength];
	int pos = 0;
	if (typeNameLength > 0) {
		System.arraycopy(typeName, 0, result, pos, typeNameLength);
		pos += typeNameLength;
	}
	result[pos++] = SEPARATOR;
	if (packageLength > 0) {
		System.arraycopy(packageName, 0, result, pos, packageLength);
		pos += packageLength;
	}
	result[pos++] = SEPARATOR;
	if (enclosingTypeNames != null && enclosingNamesLength > 0) {
		for (int i = 0, length = enclosingTypeNames.length; i < length;) {
			char[] enclosingName = enclosingTypeNames[i];
			int itsLength = enclosingName.length;
			System.arraycopy(enclosingName, 0, result, pos, itsLength);
			pos += itsLength;
			if (++i < length)
				result[pos++] = '.';
		}
	}
	result[pos++] = SEPARATOR;
	result[pos++] = (char) modifiers;
	result[pos] = (char) (modifiers>>16);
	if (secondary) {
		result[++pos] = SEPARATOR;
		result[++pos] = 'S';
	}
	return result;
}

public TypeDeclarationPattern(
		char[] moduleNames,
		char[] pkg,
		char[][] enclosingTypeNames,
		char[] simpleName,
		char typeSuffix,
		int matchRule) {

		this(matchRule);
		addModuleNames(moduleNames);
		this.pkg = this.isCaseSensitive ? pkg : CharOperation.toLowerCase(pkg);
		if (this.isCaseSensitive || enclosingTypeNames == null) {
			this.enclosingTypeNames = enclosingTypeNames;
		} else {
			int length = enclosingTypeNames.length;
			this.enclosingTypeNames = new char[length][];
			for (int i = 0; i < length; i++)
				this.enclosingTypeNames[i] = CharOperation.toLowerCase(enclosingTypeNames[i]);
		}
		this.simpleName = (this.isCaseSensitive || this.isCamelCase) ? simpleName : CharOperation.toLowerCase(simpleName);
		this.typeSuffix = typeSuffix;

		this.mustResolve = (this.pkg != null && this.enclosingTypeNames != null) || typeSuffix != TYPE_SUFFIX;
	}

public TypeDeclarationPattern(
	char[] pkg,
	char[][] enclosingTypeNames,
	char[] simpleName,
	char typeSuffix,
	int matchRule) {

	this(null, pkg, enclosingTypeNames, simpleName, typeSuffix, matchRule);
}
TypeDeclarationPattern(int matchRule) {
	super(TYPE_DECL_PATTERN, matchRule);
}
protected void addModuleNames(char[] modNames) {
	if (modNames == null) {
		return;
	}
	final String explicit_unnamed = new String(IJavaSearchConstants.ALL_UNNAMED);
	String[] names = new String(modNames).split(String.valueOf(CharOperation.COMMA_SEPARATOR));
	int len = names.length;
	if (this.allowModuleRegex && len > 0 && names[0] != null && names[0].length() > 0
			&& names[0].charAt(0) == IIndexConstants.ZERO_CHAR) { //pattern
		names[0] = names[0].substring(1);
		this.modulePatterns = new Pattern[len];
		for (int i = 0; i < len; ++i) {
			this.modulePatterns[i] = Pattern.compile(names[i]);
		}
	} else { // 'normal' matching - flag if don't care conditions are passed
		for (int i = 0; i < len; ++i) {
			names[i] = names[i].trim();
			if (explicit_unnamed.equals(names[i]))
				names[i] = ""; //$NON-NLS-1$
		}
	}
	this.moduleNames = new char[len][];
	for (int i = 0; i < len; ++i) {
		String s = names[i];
		this.moduleNames[i] = s != null ? s.toCharArray() : CharOperation.NO_CHAR;
	}
}
/*
 * Type entries are encoded as:
 * 	simpleTypeName / packageName / enclosingTypeName / modifiers
 *			e.g. Object/java.lang//0
 * 		e.g. Cloneable/java.lang//512
 * 		e.g. LazyValue/javax.swing/UIDefaults/0
 * or for secondary types as:
 * 	simpleTypeName / packageName / enclosingTypeName / modifiers / S
 */
@Override
public void decodeIndexKey(char[] key) {
	int slash = CharOperation.indexOf(SEPARATOR, key, 0);
	this.simpleName = CharOperation.subarray(key, 0, slash);

	int start = ++slash;
	if (key[start] == SEPARATOR) {
		this.pkg = CharOperation.NO_CHAR;
	} else {
		slash = CharOperation.indexOf(SEPARATOR, key, start);
		this.pkg = internedPackageNames.add(CharOperation.subarray(key, start, slash));
	}

	// Continue key read by the end to decode modifiers
	int last = key.length-1;
	this.secondary = key[last] == 'S';
	if (this.secondary) {
		last -= 2;
	}
	this.modifiers = key[last-1] + (key[last]<<16);
	decodeModifiers();

	// Retrieve enclosing type names
	start = slash + 1;
	last -= 2; // position of ending slash
	if (start == last) {
		this.enclosingTypeNames = CharOperation.NO_CHAR_CHAR;
	} else {
		if (last == (start+1) && key[start] == ZERO_CHAR) {
			this.enclosingTypeNames = ONE_ZERO_CHAR;
		} else {
			this.enclosingTypeNames = CharOperation.splitOn('.', key, start, last);
		}
	}
}
protected void decodeModifiers() {

	// Extract suffix from modifiers instead of index key
	switch (this.modifiers & (ClassFileConstants.AccInterface|ClassFileConstants.AccEnum|ClassFileConstants.AccAnnotation)) {
		case ClassFileConstants.AccAnnotation:
		case ClassFileConstants.AccAnnotation+ClassFileConstants.AccInterface:
			this.typeSuffix = ANNOTATION_TYPE_SUFFIX;
			break;
		case ClassFileConstants.AccEnum:
			this.typeSuffix = ENUM_SUFFIX;
			break;
		case ClassFileConstants.AccInterface:
			this.typeSuffix = INTERFACE_SUFFIX;
			break;
		default:
			this.typeSuffix = CLASS_SUFFIX;
			break;
	}
}
@Override
public SearchPattern getBlankPattern() {
	return new TypeDeclarationPattern(R_EXACT_MATCH | R_CASE_SENSITIVE);
}
@Override
public char[][] getIndexCategories() {
	return CATEGORIES;
}
@Override
public boolean matchesDecodedKey(SearchPattern decodedPattern) {
	TypeDeclarationPattern pattern = (TypeDeclarationPattern) decodedPattern;

	// check type suffix
	if (this.typeSuffix != pattern.typeSuffix && this.typeSuffix != TYPE_SUFFIX) {
		if (!matchDifferentTypeSuffixes(this.typeSuffix, pattern.typeSuffix)) {
			return false;
		}
	}

	// check name
	if (!matchesName(this.simpleName, pattern.simpleName))
		return false;

	// check package - exact match only
	if (this.pkg != null && !CharOperation.equals(this.pkg, pattern.pkg, isCaseSensitive()))
		return false;

	// check enclosingTypeNames - exact match only
	if (this.enclosingTypeNames != null) {
		if (this.enclosingTypeNames.length == 0)
			return pattern.enclosingTypeNames.length == 0;
		if (this.enclosingTypeNames.length == 1 && pattern.enclosingTypeNames.length == 1)
			return CharOperation.equals(this.enclosingTypeNames[0], pattern.enclosingTypeNames[0], isCaseSensitive());
		if (pattern.enclosingTypeNames == ONE_ZERO_CHAR)
			return true; // is a local or anonymous type
		return CharOperation.equals(this.enclosingTypeNames, pattern.enclosingTypeNames, isCaseSensitive());
	}
	return true;
}
@Override
public EntryResult[] queryIn(Index index) throws IOException {
	char[] key = this.simpleName; // can be null
	int matchRule = getMatchRule();

	switch(getMatchMode()) {
		case R_PREFIX_MATCH :
			// do a prefix query with the simpleName
			break;
		case R_EXACT_MATCH :
			matchRule &= ~R_EXACT_MATCH;
			if (this.simpleName != null) {
				matchRule |= R_PREFIX_MATCH;
				key = this.pkg == null
					? CharOperation.append(this.simpleName, SEPARATOR)
					: CharOperation.concat(this.simpleName, SEPARATOR, this.pkg, SEPARATOR, CharOperation.NO_CHAR);
				break; // do a prefix query with the simpleName and possibly the pkg
			}
			matchRule |= R_PATTERN_MATCH;
			// $FALL-THROUGH$ - fall thru to encode the key and do a pattern query
		case R_PATTERN_MATCH :
			if (this.pkg == null) {
				if (this.simpleName == null) {
					switch(this.typeSuffix) {
						case CLASS_SUFFIX :
						case INTERFACE_SUFFIX :
						case ENUM_SUFFIX :
						case ANNOTATION_TYPE_SUFFIX :
						case CLASS_AND_INTERFACE_SUFFIX :
						case CLASS_AND_ENUM_SUFFIX :
						case INTERFACE_AND_ANNOTATION_SUFFIX :
							// null key already returns all types
							// key = new char[] {ONE_STAR[0],  SEPARATOR, ONE_STAR[0]};
							break;
					}
				} else if (this.simpleName[this.simpleName.length - 1] != '*') {
					key = CharOperation.concat(this.simpleName, ONE_STAR, SEPARATOR);
				}
				break; // do a pattern query with the current encoded key
			}
			// must decode to check enclosingTypeNames due to the encoding of local types
			key = CharOperation.concat(
				this.simpleName == null ? ONE_STAR : this.simpleName, SEPARATOR, this.pkg, SEPARATOR, ONE_STAR);
			break;
		case R_REGEXP_MATCH :
			// TODO (frederic) implement regular expression match
			break;
		case R_CAMELCASE_MATCH:
		case R_CAMELCASE_SAME_PART_COUNT_MATCH:
			// do a prefix query with the simpleName
			break;
	}

	return index.query(getIndexCategories(), key, matchRule); // match rule is irrelevant when the key is null
}
@Override
protected StringBuffer print(StringBuffer output) {
	switch (this.typeSuffix){
		case CLASS_SUFFIX :
			output.append("ClassDeclarationPattern: pkg<"); //$NON-NLS-1$
			break;
		case CLASS_AND_INTERFACE_SUFFIX:
			output.append("ClassAndInterfaceDeclarationPattern: pkg<"); //$NON-NLS-1$
			break;
		case CLASS_AND_ENUM_SUFFIX :
			output.append("ClassAndEnumDeclarationPattern: pkg<"); //$NON-NLS-1$
			break;
		case INTERFACE_SUFFIX :
			output.append("InterfaceDeclarationPattern: pkg<"); //$NON-NLS-1$
			break;
		case INTERFACE_AND_ANNOTATION_SUFFIX:
			output.append("InterfaceAndAnnotationDeclarationPattern: pkg<"); //$NON-NLS-1$
			break;
		case ENUM_SUFFIX :
			output.append("EnumDeclarationPattern: pkg<"); //$NON-NLS-1$
			break;
		case ANNOTATION_TYPE_SUFFIX :
			output.append("AnnotationTypeDeclarationPattern: pkg<"); //$NON-NLS-1$
			break;
		default :
			output.append("TypeDeclarationPattern: pkg<"); //$NON-NLS-1$
			break;
	}
	if (this.pkg != null)
		output.append(this.pkg);
	else
		output.append("*"); //$NON-NLS-1$
	output.append(">, enclosing<"); //$NON-NLS-1$
	if (this.enclosingTypeNames != null) {
		for (int i = 0; i < this.enclosingTypeNames.length; i++){
			output.append(this.enclosingTypeNames[i]);
			if (i < this.enclosingTypeNames.length - 1)
				output.append('.');
		}
	} else {
		output.append("*"); //$NON-NLS-1$
	}
	output.append(">, type<"); //$NON-NLS-1$
	if (this.simpleName != null)
		output.append(this.simpleName);
	else
		output.append("*"); //$NON-NLS-1$
	output.append(">"); //$NON-NLS-1$
	return super.print(output);
}
}
