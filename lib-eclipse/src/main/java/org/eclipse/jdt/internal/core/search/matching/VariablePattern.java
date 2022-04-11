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

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.search.IJavaSearchConstants;

public abstract class VariablePattern extends JavaSearchPattern {

protected boolean findDeclarations = false;
protected boolean findReferences = false;
protected boolean readAccess = false;
protected boolean writeAccess = false;

protected char[] name;

public final static int FINE_GRAIN_MASK =
	IJavaSearchConstants.SUPER_REFERENCE |
	IJavaSearchConstants.QUALIFIED_REFERENCE |
	IJavaSearchConstants.THIS_REFERENCE |
	IJavaSearchConstants.IMPLICIT_THIS_REFERENCE;

public VariablePattern(int patternKind, char[] name, int limitTo, int matchRule) {
	super(patternKind, matchRule);

    this.fineGrain = limitTo & FINE_GRAIN_MASK;
    if (this.fineGrain == 0) {
		switch (limitTo & 0xF) {
			case IJavaSearchConstants.DECLARATIONS :
				this.findDeclarations = true;
				break;
			case IJavaSearchConstants.REFERENCES :
				this.readAccess = true;
				this.writeAccess = true;
				break;
			case IJavaSearchConstants.READ_ACCESSES :
				this.readAccess = true;
				break;
			case IJavaSearchConstants.WRITE_ACCESSES :
				this.writeAccess = true;
				break;
			case IJavaSearchConstants.ALL_OCCURRENCES :
				this.findDeclarations = true;
				this.readAccess = true;
				this.writeAccess = true;
				break;
		}
		this.findReferences = this.readAccess || this.writeAccess;
    }

	this.name = (this.isCaseSensitive || this.isCamelCase) ? name : CharOperation.toLowerCase(name);
}
/*
 * Returns whether a method declaration or message send will need to be resolved to
 * find out if this method pattern matches it.
 */
protected boolean mustResolve() {
	// would like to change this so that we only do it if generic references are found
	return this.findReferences || this.fineGrain != 0; // always resolve (in case of a simple name reference being a potential match)
}
}
