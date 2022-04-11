/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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

import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.internal.core.index.EntryResult;
import org.eclipse.jdt.internal.core.index.Index;
import org.eclipse.jdt.internal.core.search.indexing.IIndexConstants;

public class SecondaryTypeDeclarationPattern extends TypeDeclarationPattern {

	private final static char[] SECONDARY_PATTERN_KEY = "*/S".toCharArray(); //$NON-NLS-1$

public SecondaryTypeDeclarationPattern() {
	super(null, null, null, IIndexConstants.SECONDARY_SUFFIX, R_EXACT_MATCH | R_CASE_SENSITIVE);
}

public SecondaryTypeDeclarationPattern(int matchRule) {
	super(matchRule);
}

@Override
public SearchPattern getBlankPattern() {
	return new SecondaryTypeDeclarationPattern(R_EXACT_MATCH | R_CASE_SENSITIVE);
}
@Override
protected StringBuffer print(StringBuffer output) {
	output.append("Secondary"); //$NON-NLS-1$
	return super.print(output);
}

@Override
public EntryResult[] queryIn(Index index) throws IOException {
	return index.query(CATEGORIES, SECONDARY_PATTERN_KEY, R_PATTERN_MATCH | R_CASE_SENSITIVE);
}

}
