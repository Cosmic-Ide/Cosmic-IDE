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

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.internal.core.util.Util;

public class TypeReferencePattern extends IntersectingPattern {

	protected char[] qualification;
	protected char[] simpleName;

	protected char[] currentCategory;

	/* Optimization: case where simpleName == null */
	public int segmentsSize;
	protected char[][] segments;
	protected int currentSegment;

	private final static char[][]
		CATEGORIES = { REF, ANNOTATION_REF },
		CATEGORIES_ANNOT_REF = { ANNOTATION_REF };
	private char[][] categories;
	char typeSuffix = TYPE_SUFFIX;

	public TypeReferencePattern(char[] qualification, char[] simpleName, int matchRule) {
		this(matchRule);

		this.qualification = this.isCaseSensitive ? qualification : CharOperation.toLowerCase(qualification);
		this.simpleName = (this.isCaseSensitive || this.isCamelCase) ? simpleName : CharOperation.toLowerCase(simpleName);

		if (simpleName == null)
			this.segments = this.qualification == null ? ONE_STAR_CHAR : CharOperation.splitOn('.', this.qualification);
		else
			this.segments = null;

		if (this.segments == null)
			if (this.qualification == null)
				this.segmentsSize =  0;
			else
				this.segmentsSize =  CharOperation.occurencesOf('.', this.qualification) + 1;
		else
			this.segmentsSize = this.segments.length;

		this.mustResolve = true; // always resolve (in case of a simple name reference being a potential match)
	}
	/*
	 * Instantiate a type reference pattern with additional information for generics search
	 */
	public TypeReferencePattern(char[] qualification, char[] simpleName, String typeSignature, int matchRule) {
		this(qualification, simpleName, typeSignature, 0, TYPE_SUFFIX, matchRule);
	}
	/*
	 * Instantiate a type reference pattern with additional information for generics search and search elements nature
	 */
	public TypeReferencePattern(char[] qualification, char[] simpleName, String typeSignature, char typeSuffix, int matchRule) {
		this(qualification, simpleName, typeSignature, 0, typeSuffix, matchRule);
	}

	/*
	 * Instanciate a type reference pattern with additional information for generics search, search elements nature and fine grain information
	 */
	public TypeReferencePattern(char[] qualification, char[] simpleName, String typeSignature, int limitTo, char typeSuffix, int matchRule) {
		this(qualification, simpleName,matchRule);
		this.typeSuffix = typeSuffix;
		if (typeSignature != null) {
			// store type signatures and arguments
			this.typeSignatures = Util.splitTypeLevelsSignature(typeSignature);
			setTypeArguments(Util.getAllTypeArguments(this.typeSignatures));
			if (hasTypeArguments()) {
				this.segmentsSize = getTypeArguments().length + CharOperation.occurencesOf('/', this.typeSignatures[0]) - 1;
			}
		}
	    this.fineGrain = limitTo & 0xFFFFFFF0;
	    if (this.fineGrain == IJavaSearchConstants.ANNOTATION_TYPE_REFERENCE) {
	    	this.categories = CATEGORIES_ANNOT_REF;
	    }
	}

	/*
	 * Instantiate a type reference pattern with additional information for generics search
	 */
	public TypeReferencePattern(char[] qualification, char[] simpleName, IType type, int matchRule) {
		this(qualification, simpleName, type, 0, matchRule);
	}

	/*
	 * Instanciate a type reference pattern with additional information for generics search
	 */
	public TypeReferencePattern(char[] qualification, char[] simpleName, IType type, int limitTo, int matchRule) {
		this(qualification, simpleName,matchRule);
		storeTypeSignaturesAndArguments(type);
	    this.fineGrain = limitTo & 0xFFFFFFF0;
	}

	TypeReferencePattern(int matchRule) {
		super(TYPE_REF_PATTERN, matchRule);
		this.categories = CATEGORIES;
	}
	@Override
	public void decodeIndexKey(char[] key) {
		this.simpleName = key;
	}
	@Override
	public SearchPattern getBlankPattern() {
		return new TypeReferencePattern(R_EXACT_MATCH | R_CASE_SENSITIVE);
	}
	@Override
	public char[] getIndexKey() {
		if (this.simpleName != null)
			return this.simpleName;

		// Optimization, e.g. type reference is 'org.eclipse.jdt.core.*'
		if (this.currentSegment >= 0)
			return this.segments[this.currentSegment];
		return null;
	}
	@Override
	public char[][] getIndexCategories() {
		return this.categories;
	}
	@Override
	protected boolean hasNextQuery() {
		if (this.segments == null) return false;

		// Optimization, e.g. type reference is 'org.eclipse.jdt.core.*'
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
		if (this.segments != null)
			this.currentSegment = this.segments.length - 1;
	}
	@Override
	protected StringBuffer print(StringBuffer output) {
		String patternClassName = getClass().getName();
		output.append(patternClassName.substring(patternClassName.lastIndexOf('.')+1));
		output.append(": qualification<"); //$NON-NLS-1$
		if (this.qualification != null)
			output.append(this.qualification);
		else
			output.append("*"); //$NON-NLS-1$
		output.append(">, type<"); //$NON-NLS-1$
		if (this.simpleName != null)
			output.append(this.simpleName);
		else
			output.append("*"); //$NON-NLS-1$
		output.append(">"); //$NON-NLS-1$
		return super.print(output);
	}
}
