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
package org.eclipse.jdt.core;

import org.eclipse.core.resources.IMarker;

/**
 * A completion requestor accepts results as they are computed and is aware
 * of source positions to complete the various different results.
 * <p>
 * This interface may be implemented by clients.
 * </p>
 *
 * @see ICodeAssist
 * @deprecated Use {@link CompletionRequestor} instead.
 */
public interface ICodeCompletionRequestor {
/**
 * Code assist notification of a class completion.
 *
 * @param packageName Declaring package name of the class.
 * @param className Name of the class.
 * @param completionName The completion for the class.
 *   Can include ';' for imported classes.
 * @param modifiers The modifiers of the class.
 * @param completionStart The start position of insertion of the name of the class.
 * @param completionEnd The end position of insertion of the name of the class.
 *
 * NOTE - All package and type names are presented in their readable form:
 *    Package names are in the form "a.b.c".
 *    Nested type names are in the qualified form "A.M".
 *    The default package is represented by an empty array.
 */
void acceptClass(
	char[] packageName,
	char[] className,
	char[] completionName,
	int modifiers,
	int completionStart,
	int completionEnd);
/**
 * Code assist notification of a compilation error detected during completion.
 *  @param marker Only problems which are categorized as errors are notified to the requestor,
 *		warnings are silently ignored.
 *		In case an error got signaled, no other completions might be available,
 *		therefore the problem message should be presented to the user.
 *		The source positions of the problem are related to the source where it was
 *		detected (might be in another compilation unit, if it was indirectly requested
 *		during the code assist process).
 *      Note: the problem knows its originating file name.
 */
void acceptError(IMarker marker);
/**
 * Code assist notification of a field completion.
 *
 * @param declaringTypePackageName Name of the package in which the type that contains this field is declared.
 *
 * @param declaringTypeName Name of the type declaring this new field.
 *
 * @param name Name of the field.
 *
 * @param typePackageName Name of the package in which the type of this field is declared.
 *
 * @param typeName Name of the type of this field.
 *
 * @param completionName The completion for the field.
 *
 * @param modifiers The modifiers of this field.
 *
 * @param completionStart The start position of insertion of the name of this field.
 *
 * @param completionEnd The end position of insertion of the name of this field.
 *
 * NOTE - All package and type names are presented in their readable form:
 *    Package names are in the form "a.b.c".
 *    Base types are in the form "int" or "boolean".
 *    Array types are in the qualified form "M[]" or "int[]".
 *    Nested type names are in the qualified form "A.M".
 *    The default package is represented by an empty array.
 */
void acceptField(
	char[] declaringTypePackageName,
	char[] declaringTypeName,
	char[] name,
	char[] typePackageName,
	char[] typeName,
	char[] completionName,
	int modifiers,
	int completionStart,
	int completionEnd);
/**
 * Code assist notification of an interface completion.
 *
 * @param packageName Declaring package name of the interface.
 * @param interfaceName Name of the interface.
 * @param completionName The completion for the interface.
 *   Can include ';' for imported interfaces.
 * @param modifiers The modifiers of the interface.
 * @param completionStart The start position of insertion of the name of the interface.
 * @param completionEnd The end position of insertion of the name of the interface.
 *
 * NOTE - All package and type names are presented in their readable form:
 *    Package names are in the form "a.b.c".
 *    Nested type names are in the qualified form "A.M".
 *    The default package is represented by an empty array.
 */
void acceptInterface(
	char[] packageName,
	char[] interfaceName,
	char[] completionName,
	int modifiers,
	int completionStart,
	int completionEnd);
/**
 * Code assist notification of a keyword completion.
 *
 * @param keywordName The keyword source.
 * @param completionStart The start position of insertion of the name of this keyword.
 * @param completionEnd The end position of insertion of the name of this keyword.
 */
void acceptKeyword(char[] keywordName, int completionStart, int completionEnd);
/**
 * Code assist notification of a label completion.
 *
 * @param labelName The label source.
 * @param completionStart The start position of insertion of the name of this label.
 * @param completionEnd The end position of insertion of the name of this label.
 */
void acceptLabel(char[] labelName, int completionStart, int completionEnd);
/**
 * Code assist notification of a local variable completion.
 *
 * @param name Name of the new local variable.
 *
 * @param typePackageName Name of the package in which the type of this new local variable is declared.
 *
 * @param typeName Name of the type of this new local variable.
 *
 * @param modifiers The modifiers of this new local variable.
 *
 * @param completionStart The start position of insertion of the name of this new local variable.
 *
 * @param completionEnd The end position of insertion of the name of this new local variable.
 *
 * NOTE - All package and type names are presented in their readable form:
 *    Package names are in the form "a.b.c".
 *    Base types are in the form "int" or "boolean".
 *    Array types are in the qualified form "M[]" or "int[]".
 *    Nested type names are in the qualified form "A.M".
 *    The default package is represented by an empty array.
 */
void acceptLocalVariable(
	char[] name,
	char[] typePackageName,
	char[] typeName,
	int modifiers,
	int completionStart,
	int completionEnd);
/**
 * Code assist notification of a method completion.
 *
 * @param declaringTypePackageName Name of the package in which the type that contains this new method is declared.
 *
 * @param declaringTypeName Name of the type declaring this new method.
 *
 * @param selector Name of the new method.
 *
 * @param parameterPackageNames Names of the packages in which the parameter types are declared.
 *    	Should contain as many elements as parameterTypeNames.
 *
 * @param parameterTypeNames Names of the parameters types.
 *    	Should contain as many elements as parameterPackageNames.
 *
 * @param returnTypePackageName Name of the package in which the return type is declared.
 *
 * @param returnTypeName Name of the return type of this new method, should be <code>null</code> for a constructor.
 *
 * @param completionName The completion for the method.
 *   	Can include zero, one or two brackets. If the closing bracket is included, then the cursor should be placed before it.
 *
 * @param modifiers The modifiers of this new method.
 *
 * @param completionStart The start position of insertion of the name of this new method.
 *
 * @param completionEnd The end position of insertion of the name of this new method.
 *
 * NOTE - All package and type names are presented in their readable form:
 *    Package names are in the form "a.b.c".
 *    Base types are in the form "int" or "boolean".
 *    Array types are in the qualified form "M[]" or "int[]".
 *    Nested type names are in the qualified form "A.M".
 *    The default package is represented by an empty array.
 *
 * NOTE: parameter names can be retrieved from the source model after the user selects a specific method.
 */
void acceptMethod(
	char[] declaringTypePackageName,
	char[] declaringTypeName,
	char[] selector,
	char[][] parameterPackageNames,
	char[][] parameterTypeNames,
	char[] returnTypePackageName,
	char[] returnTypeName,
	char[] completionName,
	int modifiers,
	int completionStart,
	int completionEnd);
/**
 * Code assist notification of a modifier completion.
 *
 * @param modifierName The new modifier.
 * @param completionStart The start position of insertion of the name of this new modifier.
 * @param completionEnd The end position of insertion of the name of this new modifier.
 */
void acceptModifier(char[] modifierName, int completionStart, int completionEnd);
/**
 * Code assist notification of a package completion.
 *
 * @param packageName The package name.
 * @param completionName The completion for the package.
 *   Can include '.*;' for imports.
 * @param completionStart The start position of insertion of the name of this new package.
 * @param completionEnd The end position of insertion of the name of this new package.
 *
 * NOTE - All package names are presented in their readable form:
 *    Package names are in the form "a.b.c".
 *    The default package is represented by an empty array.
 */
void acceptPackage(
	char[] packageName,
	char[] completionName,
	int completionStart,
	int completionEnd);
/**
 * Code assist notification of a type completion.
 *
 * @param packageName Declaring package name of the type.
 * @param typeName Name of the type.
 * @param completionName The completion for the type.
 *   Can include ';' for imported types.
 * @param completionStart The start position of insertion of the name of the type.
 * @param completionEnd The end position of insertion of the name of the type.
 *
 * NOTE - All package and type names are presented in their readable form:
 *    Package names are in the form "a.b.c".
 *    Nested type names are in the qualified form "A.M".
 *    The default package is represented by an empty array.
 */
void acceptType(
	char[] packageName,
	char[] typeName,
	char[] completionName,
	int completionStart,
	int completionEnd);
}
