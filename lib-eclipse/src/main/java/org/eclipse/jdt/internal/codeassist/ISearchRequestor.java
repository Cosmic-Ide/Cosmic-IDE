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
package org.eclipse.jdt.internal.codeassist;

import org.eclipse.jdt.internal.compiler.env.AccessRestriction;

/**
 * This is the internal requestor passed to the searchable name environment
 * so as to process the multiple search results as they are discovered.
 *
 * It is used to allow the code assist engine to add some more information
 * to the raw name environment results before answering them to the UI.
 */
public interface ISearchRequestor {
	public void acceptConstructor(
						int modifiers,
						char[] simpleTypeName,
						int parameterCount,
						char[] signature,
						char[][] parameterTypes,
						char[][] parameterNames,
						int typeModifiers,
						char[] packageName,
						int extraFlags,
						String path,
						AccessRestriction access);
	/**
	 * One result of the search consists of a new type.
	 *
	 * NOTE - All package and type names are presented in their readable form:
	 *    Package names are in the form "a.b.c".
	 *    Nested type names are in the qualified form "A.I".
	 *    The default package is represented by an empty array.
	 */
	public void acceptType(char[] packageName, char[] typeName, char[][] enclosingTypeNames, int modifiers, AccessRestriction accessRestriction);

//	/**
//	 * One result of the search consists of a new annotation.
//	 *
//	 * NOTE - All package and type names are presented in their readable form:
//	 *    Package names are in the form "a.b.c".
//	 *    Nested type names are in the qualified form "A.I".
//	 *    The default package is represented by an empty array.
//	 */
//	public void acceptAnnotation(char[] packageName, char[] typeName, int modifiers, AccessRestriction accessRestriction);
//
//	/**
//	 * One result of the search consists of a new class.
//	 *
//	 * NOTE - All package and type names are presented in their readable form:
//	 *    Package names are in the form "a.b.c".
//	 *    Nested type names are in the qualified form "A.M".
//	 *    The default package is represented by an empty array.
//	 */
//	public void acceptClass(char[] packageName, char[] typeName, int modifiers, AccessRestriction accessRestriction);
//
//	/**
//	 * One result of the search consists of a new enum.
//	 *
//	 * NOTE - All package and type names are presented in their readable form:
//	 *    Package names are in the form "a.b.c".
//	 *    Nested type names are in the qualified form "A.I".
//	 *    The default package is represented by an empty array.
//	 */
//	public void acceptEnum(char[] packageName, char[] typeName, int modifiers, AccessRestriction accessRestriction);
//
//	/**
//	 * One result of the search consists of a new interface.
//	 *
//	 * NOTE - All package and type names are presented in their readable form:
//	 *    Package names are in the form "a.b.c".
//	 *    Nested type names are in the qualified form "A.I".
//	 *    The default package is represented by an empty array.
//	 */
//	public void acceptInterface(char[] packageName, char[] typeName, int modifiers, AccessRestriction accessRestriction);

	/**
	 * One result of the search consists of a new package.
	 *
	 * NOTE - All package names are presented in their readable form:
	 *    Package names are in the form "a.b.c".
	 *    The default package is represented by an empty array.
	 */
	public void acceptPackage(char[] packageName);

	public void acceptModule(char[] moduleName);
}
