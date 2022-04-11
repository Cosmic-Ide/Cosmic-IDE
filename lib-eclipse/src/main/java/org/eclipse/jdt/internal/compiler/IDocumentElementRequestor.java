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
package org.eclipse.jdt.internal.compiler;

import org.eclipse.jdt.core.compiler.CategorizedProblem;

/**
 * Part of the source element parser responsible for building the output.
 * It gets notified of structural information as they are detected, relying
 * on the requestor to assemble them together, based on the notifications it got.
 *
 * The structural investigation includes:
 * - package statement
 * - import statements
 * - top-level types: package member, member types (member types of member types...)
 * - fields
 * - methods
 *
 * If reference information is requested, then all source constructs are
 * investigated and type, field & method references are provided as well.
 *
 * Any (parsing) problem encountered is also provided.
 *
 * All positions are relative to the exact source fed to the parser.
 *
 * Elements which are complex are notified in two steps:
 * - enter<Element> : once the element header has been identified
 * - exit<Element> : once the element has been fully consumed
 *
 * other simpler elements (package, import) are read all at once:
 * - accept<Element>
 */

public interface IDocumentElementRequestor {
/**
 * @param declarationStart - a source position corresponding to the start of the package
 *  declaration
 * @param declarationEnd - a source position corresponding to the end of the package
 *  declaration
 * @param javaDocPositions - answer back an array of sourceStart/sourceEnd
 * positions of the available JavaDoc comments. The array is a flattened
 * structure: 2*n entries with consecutives start and end positions.
 * If no JavaDoc is available, then null is answered instead of an empty array.
 * e.g. { 10, 20, 25, 45 }  --> javadoc1 from 10 to 20, javadoc2 from 25 to 45
 * The array is equals to null if there are no javadoc comments
 * @param name - the name of the package
 * @param nameStartPosition - a source position corresponding to the first character of the
 *  name
 * @param onDemand - a boolean equals to true if the import is an import on demand
 */
void acceptImport(
	int declarationStart,
	int declarationEnd,
	int[] javaDocPositions,
	char[] name,
	int nameStartPosition,
	boolean onDemand,
	int modifiers);
/**
 * @param declarationStart - a source position corresponding to the start of the package
 *  declaration
 * @param declarationEnd - a source position corresponding to the end of the package
 *  declaration
 * @param javaDocPositions - answer back an array of sourceStart/sourceEnd
 * positions of the available JavaDoc comments. The array is a flattened
 * structure: 2*n entries with consecutives start and end positions.
 * If no JavaDoc is available, then null is answered instead of an empty array.
 * e.g. { 10, 20, 25, 45 }  --> javadoc1 from 10 to 20, javadoc2 from 25 to 45
 * The array is equals to null if there are no javadoc comments
 * @param modifiers - the modifiers for this initializer
 * @param modifiersStart - a source position corresponding to the start
 *  of the textual modifiers, is < 0 if there are no textual modifiers
 * @param bodyStart - the position of the '{'
 * @param bodyEnd - the position of the '}'
 */
void acceptInitializer(
	int declarationStart,
	int declarationEnd,
	int[] javaDocPositions,
	int modifiers,
	int modifiersStart,
	int bodyStart,
	int bodyEnd);
/*
 * Table of line separator position. This table is passed once at the end
 * of the parse action, so as to allow computation of normalized ranges.
 *
 * A line separator might corresponds to several characters in the source,
 *
 */
void acceptLineSeparatorPositions(int[] positions);
/**
 * @param declarationStart - a source position corresponding to the start of the package
 *  declaration
 * @param declarationEnd - a source position corresponding to the end of the package
 *  declaration
 * @param javaDocPositions - answer back an array of sourceStart/sourceEnd
 * positions of the available JavaDoc comments. The array is a flattened
 * structure: 2*n entries with consecutives start and end positions.
 * If no JavaDoc is available, then null is answered instead of an empty array.
 * e.g. { 10, 20, 25, 45 }  --> javadoc1 from 10 to 20, javadoc2 from 25 to 45
 * The array is equals to null if there are no javadoc comments
 * @param name - the name of the package
 * @param nameStartPosition - a source position corresponding to the first character of the
 *  name
 */
void acceptPackage(
	int declarationStart,
	int declarationEnd,
	int[] javaDocPositions,
	char[] name,
	int nameStartPosition);
/**
 * @param problem - Used to report a problem while running the JDOM
 */
void acceptProblem(CategorizedProblem problem);
/**
 * @param declarationStart - a source position corresponding to the start
 *  of this class.
 * @param javaDocPositions - answer back an array of sourceStart/sourceEnd
 * positions of the available JavaDoc comments. The array is a flattened
 * structure: 2*n entries with consecutives start and end positions.
 * If no JavaDoc is available, then null is answered instead of an empty array.
 * e.g. { 10, 20, 25, 45 }  --> javadoc1 from 10 to 20, javadoc2 from 25 to 45
 * The array is equals to null if there are no javadoc comments
 * @param modifiers - the modifiers for this class
 * @param modifiersStart - a source position corresponding to the start
 *  of the textual modifiers, is < 0 if there are no textual modifiers
 * @param classStart - a source position corresponding to the start
 *  of the keyword 'class'
 * @param name - the name of the class
 * @param nameStart - a source position corresponding to the start of the name
 * @param nameEnd - a source position corresponding to the end of the name
 * @param superclass - the name of the superclass
 * @param superclassStart - a source position corresponding to the start
 *  of the superclass name
 * @param superclassEnd - a source position corresponding to the end of the
 *  superclass name
 * @param superinterfaces - the name of the superinterfaces
 * @param superinterfaceStarts - an array of source positions corresponding
 *  to the start of their respective superinterface names
 * @param superinterfaceEnds - an array of source positions corresponding
 *  to the end of their respective superinterface names
 * @param bodyStart - a source position corresponding to the open bracket
 *  of the class body
 */
void enterClass(
	int declarationStart,
	int[] javaDocPositions,
	int modifiers,
	int modifiersStart,
	int classStart,
	char[] name,
	int nameStart,
	int nameEnd,
	char[] superclass,
	int superclassStart,
	int superclassEnd,
	char[][] superinterfaces,
	int[] superinterfaceStarts,
	int[] superinterfaceEnds,
	int bodyStart);
void enterCompilationUnit();
/**
 * @param declarationStart - a source position corresponding to the first character
 *  of this constructor declaration
 * @param javaDocPositions - answer back an array of sourceStart/sourceEnd
 * positions of the available JavaDoc comments. The array is a flattened
 * structure: 2*n entries with consecutives start and end positions.
 * If no JavaDoc is available, then null is answered instead of an empty array.
 * e.g. { 10, 20, 25, 45 }  --> javadoc1 from 10 to 20, javadoc2 from 25 to 45
 * The array is equals to null if there are no javadoc comments
 * @param modifiers - the modifiers for this constructor converted to a flag
 * @param modifiersStart - a source position corresponding to the first character of the
 *  textual modifiers
 * @param name - the name of this constructor
 * @param nameStart - a source position corresponding to the first character of the name
 * @param nameEnd - a source position corresponding to the last character of the name
 * @param parameterTypes - a list of parameter type names
 * @param parameterTypeStarts - a list of source positions corresponding to the
 *  first character of each parameter type name
 * @param parameterTypeEnds - a list of source positions corresponding to the
 *  last character of each parameter type name
 * @param parameterNames - a list of the names of the parameters
 * @param parametersEnd - a source position corresponding to the last character of the
 *  parameter list
 * @param exceptionTypes - a list of the exception types
 * @param exceptionTypeStarts - a list of source positions corresponding to the first
 *  character of the respective exception types
 * @param exceptionTypeEnds - a list of source positions corresponding to the last
 *  character of the respective exception types
 * @param bodyStart - a source position corresponding to the start of this
 *  constructor's body
 */
void enterConstructor(
	int declarationStart,
	int[] javaDocPositions,
	int modifiers,
	int modifiersStart,
	char[] name,
	int nameStart,
	int nameEnd,
	char[][] parameterTypes,
	int [] parameterTypeStarts,
	int [] parameterTypeEnds,
	char[][] parameterNames,
	int [] parameterNameStarts,
	int [] parameterNameEnds,
	int parametersEnd,
	char[][] exceptionTypes,
	int [] exceptionTypeStarts,
	int [] exceptionTypeEnds,
	int bodyStart);
/**
 * @param declarationStart - a source position corresponding to the first character
 *  of this field
 * @param javaDocPositions - answer back an array of sourceStart/sourceEnd
 * positions of the available JavaDoc comments. The array is a flattened
 * structure: 2*n entries with consecutives start and end positions.
 * If no JavaDoc is available, then null is answered instead of an empty array.
 * e.g. { 10, 20, 25, 45 }  --> javadoc1 from 10 to 20, javadoc2 from 25 to 45
 * The array is equals to null if there are no javadoc comments
 * @param modifiers - the modifiers for this field converted to a flag
 * @param modifiersStart - a source position corresponding to the first character of the
 *  textual modifiers
 * @param type - the name of the field type
 * @param typeStart - a source position corresponding to the start of the fields type
 * @param typeEnd - a source position corresponding to the end of the fields type
 * @param typeDimensionCount - the array dimension indicated on the type (for example, 'int[] v')
 * @param name - the name of this constructor
 * @param nameStart - a source position corresponding to the first character of the name
 * @param nameEnd - a source position corresponding to the last character of the name
 * @param extendedTypeDimensionCount - the array dimension indicated on the variable,
 *  (for example, 'int v[]')
 * @param extendedTypeDimensionEnd - a source position corresponding to the end of
 *  the extened type dimension. This position should be -1 in case there is no extended
 *  dimension for the type.
 */
void enterField(
	int declarationStart,
	int[] javaDocPositions,
	int modifiers,
	int modifiersStart,
	char[] type,
	int typeStart,
	int typeEnd,
 	int typeDimensionCount,
	char[] name,
	int nameStart,
	int nameEnd,
	int extendedTypeDimensionCount,
	int extendedTypeDimensionEnd);
/**
 * @param declarationStart - a source position corresponding to the start
 *  of this class.
 * @param javaDocPositions - answer back an array of sourceStart/sourceEnd
 * positions of the available JavaDoc comments. The array is a flattened
 * structure: 2*n entries with consecutives start and end positions.
 * If no JavaDoc is available, then null is answered instead of an empty array.
 * e.g. { 10, 20, 25, 45 }  --> javadoc1 from 10 to 20, javadoc2 from 25 to 45
 * The array is equals to null if there are no javadoc comments
 * @param modifiers - the modifiers for this class
 * @param modifiersStart - a source position corresponding to the start
 *  of the textual modifiers, is < 0 if there are no textual modifiers
 * @param interfaceStart - a source position corresponding to the start
 *  of the keyword 'interface'
 * @param name - the name of the class
 * @param nameStart - a source position corresponding to the start of the name
 * @param nameEnd - a source position corresponding to the end of the name
 * @param superinterfaces - the name of the superinterfaces
 * @param superinterfaceStarts - an array of source positions corresponding
 *  to the start of their respective superinterface names
 * @param superinterfaceEnds - an array of source positions corresponding
 *  to the end of their respective superinterface names
 * @param bodyStart - a source position corresponding to the open bracket
 *  of the class body
 */
void enterInterface(
	int declarationStart,
	int[] javaDocPositions,
	int modifiers,
	int modifiersStart,
	int interfaceStart,
	char[] name,
	int nameStart,
	int nameEnd,
	char[][] superinterfaces,
	int[] superinterfaceStarts,
	int[] superinterfaceEnds,
	int bodyStart);
/**
 * @param declarationStart - a source position corresponding to the first character
 *  of this constructor declaration
 * @param javaDocPositions - answer back an array of sourceStart/sourceEnd
 * positions of the available JavaDoc comments. The array is a flattened
 * structure: 2*n entries with consecutives start and end positions.
 * If no JavaDoc is available, then null is answered instead of an empty array.
 * e.g. { 10, 20, 25, 45 }  --> javadoc1 from 10 to 20, javadoc2 from 25 to 45
 * The array is equals to null if there are no javadoc comments
 * @param modifiers - the modifiers for this constructor converted to a flag
 * @param modifiersStart - a source position corresponding to the first character of the
 *  textual modifiers
 * @param returnType - the name of the return type
 * @param returnTypeStart - a source position corresponding to the first character
 *  of the return type
 * @param returnTypeEnd - a source position corresponding to the last character
 *  of the return type
 * @param returnTypeDimensionCount - the array dimension count as supplied on the
 *  return type (for example, 'public int[] foo() {}')
 * @param name - the name of this constructor
 * @param nameStart - a source position corresponding to the first character of the name
 * @param nameEnd - a source position corresponding to the last character of the name
 * @param parameterTypes - a list of parameter type names
 * @param parameterTypeStarts - a list of source positions corresponding to the
 *  first character of each parameter type name
 * @param parameterTypeEnds - a list of source positions corresponding to the
 *  last character of each parameter type name
 * @param parameterNames - a list of the names of the parameters
 * @param parametersEnd - a source position corresponding to the last character of the
 *  parameter list
 * @param extendedReturnTypeDimensionCount - the array dimension count as supplied on the
 *  end of the parameter list (for example, 'public int foo()[] {}')
 * @param extendedReturnTypeDimensionEnd - a source position corresponding to the last character
 *  of the extended return type dimension. This position should be -1 in case there is no extended
 *  dimension for the type.
 * @param exceptionTypes - a list of the exception types
 * @param exceptionTypeStarts - a list of source positions corresponding to the first
 *  character of the respective exception types
 * @param exceptionTypeEnds - a list of source positions corresponding to the last
 *  character of the respective exception types
 * @param bodyStart - a source position corresponding to the start of this
 *  method's body
 */
void enterMethod(
	int declarationStart,
	int[] javaDocPositions,
	int modifiers,
	int modifiersStart,
	char[] returnType,
	int returnTypeStart,
	int returnTypeEnd,
 	int returnTypeDimensionCount,
	char[] name,
	int nameStart,
	int nameEnd,
	char[][] parameterTypes,
	int [] parameterTypeStarts,
	int [] parameterTypeEnds,
	char[][] parameterNames,
	int [] parameterNameStarts,
	int [] parameterNameEnds,
	int parametersEnd,
	int extendedReturnTypeDimensionCount,
	int extendedReturnTypeDimensionEnd,
	char[][] exceptionTypes,
	int [] exceptionTypeStarts,
	int [] exceptionTypeEnds,
	int bodyStart);
/**
 * @param bodyEnd - a source position corresponding to the closing bracket of the class
 * @param declarationEnd - a source position corresponding to the end of the class
 *  declaration.  This can include whitespace and comments following the closing bracket.
 */
void exitClass(
	int bodyEnd,
	int declarationEnd);
/**
 * @param declarationEnd - a source position corresponding to the end of the compilation unit
 */
void exitCompilationUnit(
	int declarationEnd);
/**
 * @param bodyEnd - a source position corresponding to the closing bracket of the method
 * @param declarationEnd - a source position corresponding to the end of the method
 *  declaration.  This can include whitespace and comments following the closing bracket.
 */
void exitConstructor(
	int bodyEnd,
	int declarationEnd);
/**
 * @param bodyEnd - a source position corresponding to the end of the field.
 * @param declarationEnd - a source position corresponding to the end of the field.
 *  This can include whitespace and comments following the semi-colon.
 */
void exitField(
	int bodyEnd,
	int declarationEnd);
/**
 * @param bodyEnd - a source position corresponding to the closing bracket of the interface
 * @param declarationEnd - a source position corresponding to the end of the interface
 *  declaration.  This can include whitespace and comments following the closing bracket.
 */
void exitInterface(
	int bodyEnd,
	int declarationEnd);
/**
 * @param bodyEnd - a source position corresponding to the closing bracket of the method
 * @param declarationEnd - a source position corresponding to the end of the method
 *  declaration.  This can include whitespace and comments following the closing bracket.
 */
void exitMethod(
	int bodyEnd,
	int declarationEnd);
}
