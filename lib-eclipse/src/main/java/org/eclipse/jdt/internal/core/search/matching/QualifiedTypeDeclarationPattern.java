/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
import org.eclipse.jdt.core.search.*;

public class QualifiedTypeDeclarationPattern extends TypeDeclarationPattern {

public char[] qualification;
PackageDeclarationPattern packagePattern;
public int packageIndex = -1;

public QualifiedTypeDeclarationPattern(char[] moduleNames, char[] qualification, char[] simpleName, char typeSuffix, int matchRule) {
	this(matchRule);
	addModuleNames(moduleNames);
	this.qualification = this.isCaseSensitive ? qualification : CharOperation.toLowerCase(qualification);
	this.simpleName = (this.isCaseSensitive || this.isCamelCase) ? simpleName : CharOperation.toLowerCase(simpleName);
	this.typeSuffix = typeSuffix;

	this.mustResolve = this.qualification != null || typeSuffix != TYPE_SUFFIX || moduleNames != null;
}
public QualifiedTypeDeclarationPattern(char[] qualification, char[] simpleName, char typeSuffix, int matchRule) {
	this(null, qualification, simpleName, typeSuffix, matchRule);
}
public QualifiedTypeDeclarationPattern(char[] qualification, int qualificationMatchRule, char[] simpleName, char typeSuffix, int matchRule) {
	this(qualification, simpleName, typeSuffix, matchRule);
	this.packagePattern = new PackageDeclarationPattern(qualification, qualificationMatchRule);
}
QualifiedTypeDeclarationPattern(int matchRule) {
	super(matchRule);
}
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
	this.qualification = this.pkg;

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
		int length = this.qualification.length;
		int size = last - start;
		System.arraycopy(this.qualification, 0, this.qualification = new char[length+1+size], 0, length);
		this.qualification[length] = '.';
		if (last == (start+1) && key[start] == ZERO_CHAR) {
			this.enclosingTypeNames = ONE_ZERO_CHAR;
			this.qualification[length+1] = ZERO_CHAR;
		} else {
			this.enclosingTypeNames = CharOperation.splitOn('.', key, start, last);
			System.arraycopy(key, start, this.qualification, length+1, size);
		}
	}
}
@Override
public SearchPattern getBlankPattern() {
	return new QualifiedTypeDeclarationPattern(R_EXACT_MATCH | R_CASE_SENSITIVE);
}
@Override
public boolean matchesDecodedKey(SearchPattern decodedPattern) {
	QualifiedTypeDeclarationPattern pattern = (QualifiedTypeDeclarationPattern) decodedPattern;

	// check type suffix
	if (this.typeSuffix != pattern.typeSuffix && this.typeSuffix != TYPE_SUFFIX) {
		if (!matchDifferentTypeSuffixes(this.typeSuffix, pattern.typeSuffix)) {
			return false;
		}
	}

	// check name
	return matchesName(this.simpleName, pattern.simpleName) &&
		(this.qualification == null || this.packagePattern == null || this.packagePattern.matchesName(this.qualification, pattern.qualification));
}
@Override
protected StringBuffer print(StringBuffer output) {
	switch (this.typeSuffix){
		case CLASS_SUFFIX :
			output.append("ClassDeclarationPattern: qualification<"); //$NON-NLS-1$
			break;
		case CLASS_AND_INTERFACE_SUFFIX:
			output.append("ClassAndInterfaceDeclarationPattern: qualification<"); //$NON-NLS-1$
			break;
		case CLASS_AND_ENUM_SUFFIX :
			output.append("ClassAndEnumDeclarationPattern: qualification<"); //$NON-NLS-1$
			break;
		case INTERFACE_SUFFIX :
			output.append("InterfaceDeclarationPattern: qualification<"); //$NON-NLS-1$
			break;
		case INTERFACE_AND_ANNOTATION_SUFFIX:
			output.append("InterfaceAndAnnotationDeclarationPattern: qualification<"); //$NON-NLS-1$
			break;
		case ENUM_SUFFIX :
			output.append("EnumDeclarationPattern: qualification<"); //$NON-NLS-1$
			break;
		case ANNOTATION_TYPE_SUFFIX :
			output.append("AnnotationTypeDeclarationPattern: qualification<"); //$NON-NLS-1$
			break;
		default :
			output.append("TypeDeclarationPattern: qualification<"); //$NON-NLS-1$
			break;
	}
	if (this.qualification != null)
		output.append(this.qualification);
	else
		output.append("*"); //$NON-NLS-1$
	output.append(">, type<"); //$NON-NLS-1$
	if (this.simpleName != null)
		output.append(this.simpleName);
	else
		output.append("*"); //$NON-NLS-1$
	output.append("> "); //$NON-NLS-1$
	return super.print(output);
}
}
