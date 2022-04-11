/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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

import org.eclipse.jdt.core.compiler.CategorizedProblem;

/**
 * A selection requestor accepts results from the selection engine.
 */
public interface ISelectionRequestor {
	/**
	 * Code assist notification of a enum selection.
	 * @param packageName char[]
	 * 		Declaring package name of the type.
	 *
	 * @param annotationName char[]
	 * 		Name of the type.
	 *
	 * @param isDeclaration boolean
	 *  	Answer if the selected type is a declaration
	 *
	 * @param genericTypeSignature
	 *  	genric type signature of the selected type if it is a
	 *  	parameterized type
	 *
	 * @param start
	 *  	Start of the selection
	 *
	 * @param end
	 *  	End of the selection
	 *
	 * NOTE - All package and type names are presented in their readable form:
	 *    Package names are in the form "a.b.c".
	 *    Nested type names are in the qualified form "A.M".
	 *    The default package is represented by an empty array.
	 */
	void acceptType(
		char[] packageName,
		char[] annotationName,
		int modifiers,
		boolean isDeclaration,
		char[] genericTypeSignature,
		int start,
		int end);

	/**
	 * Code assist notification of a module selection.
	 *
	 * @param moduleName name of the module
	 * @param uniqueKey unique key of this module
	 * @param start Start of the selection
	 * @param end End of the selection
	 */
	void acceptModule(
			char[] moduleName,
			char[] uniqueKey,
			int start,
			int end);

	/**
	 * Code assist notification of a compilation error detected during selection.
	 *  @param error CategorizedProblem
	 *      Only problems which are categorized as errors are notified to the requestor,
	 *		warnings are silently ignored.
	 *		In case an error got signaled, no other completions might be available,
	 *		therefore the problem message should be presented to the user.
	 *		The source positions of the problem are related to the source where it was
	 *		detected (might be in another compilation unit, if it was indirectly requested
	 *		during the code assist process).
	 *      Note: the problem knows its originating file name.
	 */
	void acceptError(CategorizedProblem error);

	/**
	 * Code assist notification of a field selection.
	 * @param declaringTypePackageName char[]
	 * 		Name of the package in which the type that contains this field is declared.
	 *
	 * @param declaringTypeName char[]
	 * 		Name of the type declaring this new field.
	 *
	 * @param name char[]
	 * 		Name of the field.
	 *
	 * @param isDeclaration boolean
	 *  	Answer if the selected field is a declaration
	 *
	 * @param uniqueKey
	 *  	unique key of this field
	 *
	 * @param start
	 *  	Start of the selection
	 *
	 * @param end
	 *  	End of the selection
	 *
	 * NOTE - All package and type names are presented in their readable form:
	 *    Package names are in the form "a.b.c".
	 *    Nested type names are in the qualified form "A.M".
	 *    The default package is represented by an empty array.
	 */
	void acceptField(
		char[] declaringTypePackageName,
		char[] declaringTypeName,
		char[] name,
		boolean isDeclaration,
		char[] uniqueKey,
		int start,
		int end);

	/**
	 * Code assist notification of a method selection.
	 * @param declaringTypePackageName char[]
	 * 		Name of the package in which the type that contains this new method is declared.
	 *
	 * @param declaringTypeName char[]
	 * 		Name of the type declaring this new method.
	 *
	 * @param enclosingDeclaringTypeSignature String
	 *  	Type signature of the declaring type of the declaring type or <code>null</code>
	 *  	if declaring type is a top level type.
	 *
	 * @param selector char[]
	 * 		Name of the new method.
	 *
	 * @param parameterPackageNames char[][]
	 * 		Names of the packages in which the parameter types are declared.
	 *    	Should contain as many elements as parameterTypeNames.
	 *
	 * @param parameterTypeNames char[][]
	 * 		Names of the parameters types.
	 *    	Should contain as many elements as parameterPackageNames.
	 *
	 * @param parameterSignatures String[]
	 * 		Signature of the parameters types.
	 *    	Should contain as many elements as parameterPackageNames.
	 *
	 *  @param isConstructor boolean
	 * 		Answer if the method is a constructor.
	 *
	 * @param isDeclaration boolean
	 *  	Answer if the selected method is a declaration
	 *
	 * @param uniqueKey
	 *  	unique key of the method
	 *
	 * @param start
	 *  	Start of the selection
	 *
	 * @param end
	 *  	End of the selection
	 *
	 * NOTE - All package and type names are presented in their readable form:
	 *    Package names are in the form "a.b.c".
	 *    Base types are in the form "int" or "boolean".
	 *    Array types are in the qualified form "M[]" or "int[]".
	 *    Nested type names are in the qualified form "A.M".
	 *    The default package is represented by an empty array.
	 */
	// parameters 'isDeclaration', 'start' and 'end' are use to distinguish duplicate methods declarations
	void acceptMethod(
		char[] declaringTypePackageName,
		char[] declaringTypeName,
		String enclosingDeclaringTypeSignature,
		char[] selector,
		char[][] parameterPackageNames,
		char[][] parameterTypeNames,
		String[] parameterSignatures,
		char[][] typeParameterNames,
		char[][][] typeParameterBoundNames,
		boolean isConstructor,
		boolean isDeclaration,
		char[] uniqueKey,
		int start,
		int end);

	/**
	 * Code assist notification of a package selection.
	 * @param packageName char[]
	 * 		The package name.
	 *
	 * NOTE - All package names are presented in their readable form:
	 *    Package names are in the form "a.b.c".
	 *    The default package is represented by an empty array.
	 */
	void acceptPackage(char[] packageName);
	/**
	 * Code assist notification of a type parameter selection.
	 *
	 * @param declaringTypePackageName char[]
	 * 		Name of the package in which the type that contains this new method is declared.
	 *
	 * @param declaringTypeName char[]
	 * 		Name of the type declaring this new method.
	 *
	 * @param typeParameterName char[]
	 * 		Name of the type parameter.
	 *
	 * @param isDeclaration boolean
	 *  	Answer if the selected type parameter is a declaration
	 *
	 * @param start
	 *  	Start of the selection
	 *
	 * @param end
	 *  	End of the selection
	 *
	 * NOTE - All package and type names are presented in their readable form:
	 *    Package names are in the form "a.b.c".
	 *    Nested type names are in the qualified form "A.M".
	 *    The default package is represented by an empty array.
	 */
	void acceptTypeParameter(
		char[] declaringTypePackageName,
		char[] declaringTypeName,
		char[] typeParameterName,
		boolean isDeclaration,
		int start,
		int end);

	/**
	 * Code assist notification of a type parameter selection.
	 *
	 * @param declaringTypePackageName char[]
	 * 		Name of the package in which the type that contains this new method is declared.
	 *
	 * @param declaringTypeName char[]
	 * 		Name of the type declaring this new method.
	 *
	 * @param selector char[]
	 * 		Name of the declaring method.
	 *
	 * @param selectorStart int
	 * 		Start of the selector.
	 *
	 * @param selectorEnd int
	 * 		End of the selector.
	 *
	 * @param typeParameterName char[]
	 * 		Name of the type parameter.
	 *
	 * @param isDeclaration boolean
	 *  	Answer if the selected type parameter is a declaration
	 *
	 * @param start
	 *  	Start of the selection
	 *
	 * @param end
	 *  	End of the selection
	 *
	 * NOTE - All package and type names are presented in their readable form:
	 *    Package names are in the form "a.b.c".
	 *    Nested type names are in the qualified form "A.M".
	 *    The default package is represented by an empty array.
	 */
	void acceptMethodTypeParameter(
		char[] declaringTypePackageName,
		char[] declaringTypeName,
		char[] selector,
		int selectorStart,
		int selectorEnd,
		char[] typeParameterName,
		boolean isDeclaration,
		int start,
		int end);
}
