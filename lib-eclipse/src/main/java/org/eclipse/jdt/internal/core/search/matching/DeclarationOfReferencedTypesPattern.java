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

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.internal.compiler.util.SimpleSet;

public class DeclarationOfReferencedTypesPattern extends TypeReferencePattern {

protected SimpleSet knownTypes;
protected IJavaElement enclosingElement;

public DeclarationOfReferencedTypesPattern(IJavaElement enclosingElement) {
	super(null, null, R_PATTERN_MATCH);

	this.enclosingElement = enclosingElement;
	this.knownTypes = new SimpleSet();
	this.mustResolve = true;
}
}
