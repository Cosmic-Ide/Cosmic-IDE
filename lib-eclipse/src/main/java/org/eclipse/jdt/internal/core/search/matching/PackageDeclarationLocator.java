/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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

public class PackageDeclarationLocator extends PatternLocator {

protected PackageDeclarationPattern pattern;

public PackageDeclarationLocator(PackageDeclarationPattern pattern) {
	super(pattern);

	this.pattern = pattern;
}
@Override
protected int matchContainer() {
	return 0;
}
@Override
public String toString() {
	return "Locator for " + this.pattern.toString(); //$NON-NLS-1$
}
}
