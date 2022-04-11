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
package org.eclipse.jdt.internal.compiler.env;

public interface ISourceImport {

/**
 * Answer the source end position of the import declaration.
 */

int getDeclarationSourceEnd();
/**
 * Answer the source start position of the import declaration.
 */

int getDeclarationSourceStart();

/**
 * Answer an int whose bits are set according the access constants
 * defined by the VM spec.
 * Since Java 1.5, static imports can be defined.
 */
int getModifiers();
}
