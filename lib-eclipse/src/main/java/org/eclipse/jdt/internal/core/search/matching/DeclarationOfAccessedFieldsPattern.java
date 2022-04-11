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

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.internal.compiler.util.SimpleSet;

public class DeclarationOfAccessedFieldsPattern extends FieldPattern {

protected IJavaElement enclosingElement;
protected SimpleSet knownFields;

public DeclarationOfAccessedFieldsPattern(IJavaElement enclosingElement) {
	super(null, null, null, null, null, IJavaSearchConstants.REFERENCES, R_PATTERN_MATCH);

	this.enclosingElement = enclosingElement;
	this.knownFields = new SimpleSet();
	this.mustResolve = true;
}
}
