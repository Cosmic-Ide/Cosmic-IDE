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

import java.util.stream.Stream;

import org.eclipse.jdt.core.search.IParallelizable;
import org.eclipse.jdt.core.search.SearchPattern;

public class AndPattern extends IntersectingPattern {
protected SearchPattern[] patterns;
int current;

private static int combinedMatchRule(int matchRule, int matchRule2) {
	int combined = matchRule & matchRule2;
	int compatibility = combined & MATCH_COMPATIBILITY_MASK;
	if (compatibility == 0) {
		if ((matchRule & MATCH_COMPATIBILITY_MASK) == R_FULL_MATCH) {
			compatibility = matchRule2;
		} else if ((matchRule2 & MATCH_COMPATIBILITY_MASK) == R_FULL_MATCH) {
			compatibility = matchRule;
		} else {
			compatibility = Math.min(matchRule & MATCH_COMPATIBILITY_MASK, matchRule2 & MATCH_COMPATIBILITY_MASK);
		}
	}
	return (combined & (R_EXACT_MATCH | R_PREFIX_MATCH | R_PATTERN_MATCH | R_REGEXP_MATCH))
		| (combined & R_CASE_SENSITIVE)
		| compatibility
		| (combined & (R_CAMELCASE_MATCH | R_CAMELCASE_SAME_PART_COUNT_MATCH));
}

public AndPattern(SearchPattern leftPattern, SearchPattern rightPattern) {
	super(AND_PATTERN, combinedMatchRule(leftPattern.getMatchRule(), rightPattern.getMatchRule()));
	this.mustResolve = leftPattern.mustResolve || rightPattern.mustResolve;

	SearchPattern[] leftPatterns = leftPattern instanceof AndPattern ? ((AndPattern) leftPattern).patterns : null;
	SearchPattern[] rightPatterns = rightPattern instanceof AndPattern ? ((AndPattern) rightPattern).patterns : null;
	int leftSize = leftPatterns == null ? 1 : leftPatterns.length;
	int rightSize = rightPatterns == null ? 1 : rightPatterns.length;
	this.patterns = new SearchPattern[leftSize + rightSize];

	if (leftPatterns == null)
		this.patterns[0] = leftPattern;
	else
		System.arraycopy(leftPatterns, 0, this.patterns, 0, leftSize);
	if (rightPatterns == null)
		this.patterns[leftSize] = rightPattern;
	else
		System.arraycopy(rightPatterns, 0, this.patterns, leftSize, rightSize);

	// Store erasure match
	this.matchCompatibility = getMatchRule() & MATCH_COMPATIBILITY_MASK;

	this.current = 0;
}

@Override
public SearchPattern currentPattern() {
	return this.patterns[this.current++];
}

@Override
protected boolean hasNextQuery() {
	return this.current < (this.patterns.length-1);
}

@Override
protected void resetQuery() {
	this.current = 0;
}

@Override
public boolean isParallelSearchSupported() {
	return Stream.of(this.patterns).allMatch(IParallelizable::isParallelSearchSupported);
}

@Override
public SearchPattern clone() throws CloneNotSupportedException {
	AndPattern pattern = (AndPattern) super.clone();
	pattern.patterns = this.patterns.clone();
	for (int i = 0; i < this.patterns.length; i++) {
		 pattern.patterns[i] =  this.patterns[i].clone();
	}
	return pattern;
}

}
