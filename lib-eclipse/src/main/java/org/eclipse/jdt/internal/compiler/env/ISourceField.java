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
package org.eclipse.jdt.internal.compiler.env;

public interface ISourceField extends IGenericField {
/**
 * Answer the source end position of the field's declaration.
 */
int getDeclarationSourceEnd();

/**
 * Answer the source start position of the field's declaration.
 */
int getDeclarationSourceStart();

/**
 * Answer the initialization source for this constant field.
 * Answer null if the field is not a constant or if it has no initialization.
 */
char[] getInitializationSource();

/**
 * Answer the source end position of the field's name.
 */
int getNameSourceEnd();

/**
 * Answer the source start position of the field's name.
 */
int getNameSourceStart();

/**
 * Answer the type name of the field.
 *
 * The name is a simple name or a qualified, dot separated name.
 * For example, Hashtable or java.util.Hashtable.
 */
char[] getTypeName();
}
