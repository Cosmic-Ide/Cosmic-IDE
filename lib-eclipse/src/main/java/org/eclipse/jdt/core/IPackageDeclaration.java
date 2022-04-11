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
package org.eclipse.jdt.core;


/**
 * Represents a package declaration in Java compilation unit.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IPackageDeclaration extends IJavaElement, ISourceReference, IAnnotatable {
/**
 * Returns the name of the package the statement refers to.
 * This is a handle-only method.
 *
 * @return the name of the package the statement
 */
@Override
String getElementName();
}
