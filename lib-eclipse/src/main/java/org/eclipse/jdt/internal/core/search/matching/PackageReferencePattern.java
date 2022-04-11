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
package org.eclipse.jdt.internal.core.search.matching;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.search.SearchPattern;

public class PackageReferencePattern extends IntersectingPattern {

protected char[] pkgName;

protected char[][] segments;
protected int currentSegment;

protected static char[][] CATEGORIES = { REF };

public PackageReferencePattern(char[] pkgName, int matchRule) {
	this(matchRule);

	if (pkgName == null || pkgName.length == 0) {
		this.pkgName = null;
		this.segments = new char[][] {CharOperation.NO_CHAR};
		this.mustResolve = false;
	} else {
		this.pkgName = (this.isCaseSensitive || this.isCamelCase) ? pkgName : CharOperation.toLowerCase(pkgName);
		this.segments = CharOperation.splitOn('.', this.pkgName);
		this.mustResolve = true;
	}
}
PackageReferencePattern(int matchRule) {
	super(PKG_REF_PATTERN, matchRule);
}
@Override
public void decodeIndexKey(char[] key) {
	// Package reference keys are encoded as 'name' (where 'name' is the last segment of the package name)
	this.pkgName = key;
}
@Override
public SearchPattern getBlankPattern() {
	return new PackageReferencePattern(R_EXACT_MATCH | R_CASE_SENSITIVE);
}
@Override
public char[] getIndexKey() {
	// Package reference keys are encoded as 'name' (where 'name' is the last segment of the package name)
	if (this.currentSegment >= 0)
		return this.segments[this.currentSegment];
	return null;
}
@Override
public char[][] getIndexCategories() {
	return CATEGORIES;
}
@Override
protected boolean hasNextQuery() {
	// if package has at least 4 segments, don't look at the first 2 since they are mostly
	// redundant (e.g. in 'org.eclipse.jdt.core.*' 'org.eclipse' is used all the time)
	return --this.currentSegment >= (this.segments.length >= 4 ? 2 : 0);
}
@Override
public boolean matchesDecodedKey(SearchPattern decodedPattern) {
	return true; // index key is not encoded so query results all match
}
@Override
protected void resetQuery() {
	/* walk the segments from end to start as it will find less potential references using 'lang' than 'java' */
	this.currentSegment = this.segments.length - 1;
}
@Override
protected StringBuffer print(StringBuffer output) {
	output.append("PackageReferencePattern: <"); //$NON-NLS-1$
	if (this.pkgName != null)
		output.append(this.pkgName);
	else
		output.append("*"); //$NON-NLS-1$
	output.append(">"); //$NON-NLS-1$
	return super.print(output);
}
}
