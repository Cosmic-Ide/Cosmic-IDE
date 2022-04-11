/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
package org.eclipse.jdt.internal.core.jdom;

import java.util.ArrayList;
import java.util.Map;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.jdom.*;
import org.eclipse.jdt.internal.compiler.DocumentElementParser;
import org.eclipse.jdt.internal.compiler.IDocumentElementRequestor;
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblemFactory;
/**
 * The DOMBuilder constructs each type of JDOM document fragment,
 * for the DOMFactory. The DOMBuilder has been separated from the
 * DOMFactory to hide the implmentation of node creation and the
 * public Requestor API methods.
 *
 * @deprecated The JDOM was made obsolete by the addition in 2.0 of the more
 * powerful, fine-grained DOM/AST API found in the
 * org.eclipse.jdt.core.dom package.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class DOMBuilder extends AbstractDOMBuilder implements IDocumentElementRequestor {

	/**
	 * True when parsing a single member - ignore any problems
	 * encountered after the member.
	 */
	protected boolean fBuildingSingleMember= false;

	/**
	 * True when the single member being built has been
	 * exited.
	 */
	protected boolean fFinishedSingleMember = false;

	/**
	 * Collection of multiple fields in one declaration
	 */
	protected ArrayList fFields;

	Map options = JavaCore.getOptions();

/**
 * Creates a new DOMBuilder
 */
public DOMBuilder() {
	// Creates a new DOMBuilder
}
/**
 * @see IDocumentElementRequestor#acceptImport(int, int, int[], char[], int, boolean, int)
 */
@Override
public void acceptImport(int declarationStart, int declarationEnd, int[] javaDocPositions, char[] name,
	int nameStart, boolean onDemand, int modifiers) {
	int[] sourceRange = {declarationStart, declarationEnd};
	int[] nameRange = {nameStart, declarationEnd - 1};

	/* See 1FVII1P */
	String importName = new String(this.fDocument, nameRange[0], nameRange[1] + 1 - nameRange[0]);

	this.fNode= new DOMImport(this.fDocument, sourceRange, importName, nameRange, onDemand, modifiers);
	addChild(this.fNode);
	if (this.fBuildingSingleMember) {
		this.fFinishedSingleMember= true;
	}
}
/**
 * @see IDocumentElementRequestor#acceptInitializer(int declarationStart, int declarationEnd, int[] javaDocPositions, int modifiers, int modifiersStart, int bodyStart, int bodyEnd)
 */
@Override
public void acceptInitializer(int declarationStart, int declarationEnd, int[] javaDocPositions, int modifiers,
	int modifiersStart, int bodyStart, int bodyEnd) {
	int[] sourceRange = {declarationStart, declarationEnd};
	int[] commentRange = {-1, -1};
	if (javaDocPositions != null) {
		int length = javaDocPositions.length;
		commentRange[0] = javaDocPositions[length - 2];
		commentRange[1] = javaDocPositions[length - 1];
	}

	int[] modifiersRange = {-1, -1};
	if (modifiersStart >= declarationStart) {
		modifiersRange[0] = modifiersStart;
		modifiersRange[1] = bodyStart - 1;
	}
	this.fNode = new DOMInitializer(this.fDocument, sourceRange, commentRange, modifiers,
		modifiersRange, bodyStart);
	addChild(this.fNode);
	if (this.fBuildingSingleMember) {
		this.fFinishedSingleMember= true;
	}
}
/**
 * @see IDocumentElementRequestor#acceptPackage(int declarationStart, int declarationEnd, int[] javaDocPositions, char[] name, int nameStartPosition)
 */
@Override
public void acceptPackage(int declarationStart, int declarationEnd, int[] javaDocPositions, char[] name,
	int nameStartPosition) {
	int[] sourceRange = null;
	if (javaDocPositions != null) {
		int length = javaDocPositions.length;
		// get last javadoc comment (see bug 68772)
		sourceRange = new int[] {javaDocPositions[length - 2], declarationEnd};
	} else {
		sourceRange = new int[] {declarationStart, declarationEnd};
	}
	int[] nameRange = {nameStartPosition, declarationEnd - 1};
	this.fNode= new DOMPackage(this.fDocument, sourceRange, CharOperation.charToString(name), nameRange);
	addChild(this.fNode);
	if (this.fBuildingSingleMember) {
		this.fFinishedSingleMember= true;
	}
}
/**
 * Sets the abort flag to true. The parser has encountered an error
 * in the current document. If we are only building a single member, and
 * we are done with the member - don't worry about the error.
 *
 * @see IDocumentElementRequestor
 */
@Override
public void acceptProblem(CategorizedProblem problem){
	if (this.fBuildingSingleMember && this.fFinishedSingleMember) {
		return;
	}
	this.fAbort= true;
}
/**
 * Adds the given node to the current enclosing scope, building the JDOM
 * tree. Nodes are only added to an enclosing scope when a compilation unit or type
 * is being built (since those are the only nodes that have children).
 *
 * <p>NOTE: nodes are added to the JDOM via the method #basicAddChild such that
 * the nodes in the newly created JDOM are not fragmented.
 */
@Override
protected void addChild(IDOMNode child) {
	super.addChild(child);
	if (this.fStack.isEmpty() && this.fFields != null) {
		this.fFields.add(child);
	}
}
/**
 * @see IDOMFactory#createCompilationUnit()
 */
public IDOMCompilationUnit createCompilationUnit() {
	return new DOMCompilationUnit();
}
/**
 * @see IDOMFactory#createCompilationUnit(String, String)
 */
@Override
public IDOMCompilationUnit createCompilationUnit(ICompilationUnit compilationUnit) {
	initializeBuild(compilationUnit.getContents(), true, true, false);
	getParser(this.options).parseCompilationUnit(compilationUnit);
	return super.createCompilationUnit(compilationUnit);
}
/**
 * @see IDOMFactory#createField(String)
 */
public IDOMField createField(char[] sourceCode) {
	initializeBuild(sourceCode, false, false, true);
	getParser(this.options).parseField(sourceCode);
	if (this.fAbort || this.fNode == null) {
		return null;
	}

	// we only accept field declarations with one field
	if (this.fFieldCount > 1) {
		return null;
	}

	this.fNode.normalize(this);
	return (IDOMField)this.fNode;
}
/**
 *
 */
public IDOMField[] createFields(char[] sourceCode) {
	initializeBuild(sourceCode, false, false, false);
	this.fFields= new ArrayList();
	getParser(this.options).parseField(sourceCode);
	if (this.fAbort) {
		return null;
	}
	IDOMField[] fields= new IDOMField[this.fFields.size()];
	this.fFields.toArray(fields);
	for (int i= 0; i < fields.length; i++) {
		DOMNode node= (DOMNode)fields[i];
		if (i < (fields.length - 1)) {
			DOMNode next= (DOMNode)fields[i + 1];
			node.fNextNode= next;
			next.fPreviousNode= node;
		}
		((DOMNode)fields[i]).normalize(this);
	}
	return fields;
}
/**
 * @see IDOMFactory#createImport()
 */
public IDOMImport createImport() {
	return new DOMImport();
}
/**
 * @see IDOMFactory#createImport(String)
 */
public IDOMImport createImport(char[] sourceCode) {
	initializeBuild(sourceCode, false, false, true);
	getParser(this.options).parseImport(sourceCode);
	if (this.fAbort || this.fNode == null) {
		return null;
	}
	this.fNode.normalize(this);
	return (IDOMImport)this.fNode;
}
/**
 * Creates an INITIALIZER document fragment from the given source.
 *
 * @see IDOMFactory#createInitializer(String)
 */
public IDOMInitializer createInitializer(char[] sourceCode) {
	initializeBuild(sourceCode, false, false, true);
	getParser(this.options).parseInitializer(sourceCode);
	if (this.fAbort || this.fNode == null || !(this.fNode instanceof IDOMInitializer)) {
		return null;
	}
	this.fNode.normalize(this);
	return (IDOMInitializer)this.fNode;
}
/**
 * @see IDOMFactory#createMethod(String)
 */
public IDOMMethod createMethod(char[] sourceCode) {
	initializeBuild(sourceCode, false, false, true);
	getParser(this.options).parseMethod(sourceCode);
	if (this.fAbort || this.fNode == null) {
		return null;
	}
	this.fNode.normalize(this);
	return (IDOMMethod)this.fNode;
}
/**
 * @see IDOMFactory#createPackage()
 */
public IDOMPackage createPackage() {
	return new DOMPackage();
}
/**
 * @see IDOMFactory#createPackage(String)
 */
public IDOMPackage createPackage(char[] sourceCode) {
	initializeBuild(sourceCode, false, false, true);
	getParser(this.options).parsePackage(sourceCode);
	if (this.fAbort || this.fNode == null) {
		return null;
	}
	this.fNode.normalize(this);
	return (IDOMPackage)this.fNode;
}
/**
 * @see IDOMFactory#createType(String)
 */
public IDOMType createType(char[] sourceCode) {
	initializeBuild(sourceCode, false, true, false);
	getParser(this.options).parseType(sourceCode);
	if (this.fAbort) {
		return null;
	}
	if (this.fNode != null) this.fNode.normalize(this);
	if (this.fNode instanceof IDOMType) {
		return (IDOMType) this.fNode;
	}
	return null;
}
/**
 * Creates a new DOMMethod and inizializes.
 *
 * @param declarationStart - a source position corresponding to the first character
 *		of this constructor declaration
 * @param modifiers - the modifiers for this constructor converted to a flag
 * @param modifiersStart - a source position corresponding to the first character of the
 *		textual modifiers
 * @param returnType - the name of the return type
 * @param returnTypeStart - a source position corresponding to the first character
 *		of the return type
 * @param returnTypeEnd - a source position corresponding to the last character
 *		of the return type
 * @param returnTypeDimensionCount - the array dimension count as supplied on the
 *		return type (for instance, 'public int[] foo() {}')
 * @param name - the name of this constructor
 * @param nameStart - a source position corresponding to the first character of the name
 * @param nameEnd - a source position corresponding to the last character of the name
 * @param parameterTypes - a list of parameter type names
 * @param parameterTypeStarts - a list of source positions corresponding to the
 *		first character of each parameter type name
 * @param parameterTypeEnds - a list of source positions corresponding to the
 *		last character of each parameter type name
 * @param parameterNames - a list of the names of the parameters
 * @param parametersEnd - a source position corresponding to the last character of the
 *		parameter list
 * @extendedReturnTypeDimensionCount - the array dimension count as supplied on the
 *		end of the parameter list (for instance, 'public int foo()[] {}')
 * @extendedReturnTypeDimensionEnd - a source position corresponding to the last character
 *		of the extended return type dimension
 * @param exceptionTypes - a list of the exception types
 * @param exceptionTypeStarts - a list of source positions corresponding to the first
 *		character of the respective exception types
 * @param exceptionTypeEnds - a list of source positions corresponding to the last
 *		character of the respective exception types
 * @param bodyStart - a source position corresponding to the start of this
 *		constructor's body
 */
protected void enterAbstractMethod(int declarationStart, int[] javaDocPositions, int modifiers, int modifiersStart,
	char[] returnType, int returnTypeStart, int returnTypeEnd, int returnTypeDimensionCount,
	char[] name, int nameStart, int nameEnd, char[][] parameterTypes, int[] parameterTypeStarts,
	int[] parameterTypeEnds, char[][] parameterNames, int[] parameterNameStarts,
	int[] parameterNameEnds, int parametersEnd, int extendedReturnTypeDimensionCount,
	int extendedReturnTypeDimensionEnd, char[][] exceptionTypes, int[] exceptionTypeStarts,
	int[] exceptionTypeEnds, int bodyStart, boolean isConstructor) {
	int[] sourceRange = {declarationStart, -1}; // will be fixed up on exit
	int[] nameRange = {nameStart, nameEnd};
	int[] commentRange = {-1, -1};
	if (javaDocPositions != null) {
		int length = javaDocPositions.length;
		commentRange[0] = javaDocPositions[length - 2]; // get last javadoc comment (see bug 68772)
		commentRange[1] = javaDocPositions[length - 1];
	}
	int[] modifiersRange = {-1, -1};
	if (modifiersStart > -1) {
		modifiersRange[0] = modifiersStart;
		if (isConstructor) {
			modifiersRange[1] = nameStart - 1;
		} else {
			modifiersRange[1] = returnTypeStart - 1;
		}
	}
	int[] returnTypeRange = null;

	if (extendedReturnTypeDimensionCount > 0)
		returnTypeRange = new int[] {returnTypeStart, returnTypeEnd,
			parametersEnd + 1, extendedReturnTypeDimensionEnd};
	else
		returnTypeRange = new int[] {returnTypeStart, returnTypeEnd};
	int[] parameterRange = {nameEnd + 1, parametersEnd};
	int[] exceptionRange = {-1, -1};
	if (exceptionTypes != null && exceptionTypes.length > 0) {
		int exceptionCount = exceptionTypes.length;
		exceptionRange[0] = exceptionTypeStarts[0];
		exceptionRange[1] = exceptionTypeEnds[exceptionCount - 1];
	}
	int[] bodyRange = null;
	if (exceptionRange[1] > -1) {
		bodyRange = new int[] {exceptionRange[1] + 1, -1}; // will be fixed up on exit
	} else {
		bodyRange = new int[] {parametersEnd + 1, -1};
	}
	this.fNode = new DOMMethod(this.fDocument, sourceRange, CharOperation.charToString(name), nameRange, commentRange, modifiers,
		modifiersRange, isConstructor, CharOperation.charToString(returnType), returnTypeRange,
		CharOperation.charArrayToStringArray(parameterTypes),
		CharOperation.charArrayToStringArray(parameterNames),
		parameterRange, CharOperation.charArrayToStringArray(exceptionTypes), exceptionRange, bodyRange);
	addChild(this.fNode);
	this.fStack.push(this.fNode);
}
/**
 * @see IDocumentElementRequestor#enterClass(
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
	int bodyStart)
 */
@Override
public void enterClass(int declarationStart, int[] javaDocPositions, int modifiers, int modifiersStart, int keywordStart,
	char[] name, int nameStart, int nameEnd, char[] superclass, int superclassStart,
	int superclassEnd, char[][] superinterfaces, int[] superinterfaceStarts,
	int[] superinterfaceEnds, int bodyStart) {

	enterType(declarationStart, javaDocPositions, modifiers, modifiersStart, keywordStart,
		name, nameStart, nameEnd, superclass, superclassStart,
		superclassEnd, superinterfaces, superinterfaceStarts,
		superinterfaceEnds, bodyStart, true);
}
/**
 * @see IDocumentElementRequestor#enterConstructor(
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
	int bodyStart)
 */
@Override
public void enterConstructor(int declarationStart, int[] javaDocPositions, int modifiers, int modifiersStart,
	char[] name, int nameStart, int nameEnd, char[][] parameterTypes,
	int[] parameterTypeStarts, int[] parameterTypeEnds, char[][] parameterNames,
	int[] parameterNameStarts, int[] parameterNameEnds, int parametersEnd,
	char[][] exceptionTypes, int[] exceptionTypeStarts, int[] exceptionTypeEnds,
	int bodyStart) {

	/* see 1FVIIQZ */
	String nameString = new String(this.fDocument, nameStart, nameEnd - nameStart);
	int openParenPosition = nameString.indexOf('(');
	if (openParenPosition > -1)
		nameEnd = nameStart + openParenPosition - 1;

	enterAbstractMethod(declarationStart, javaDocPositions, modifiers, modifiersStart,
		null, -1, -1, 0,
		name, nameStart, nameEnd, parameterTypes, parameterTypeStarts,
		parameterTypeEnds, parameterNames, parameterNameStarts,
		parameterNameEnds, parametersEnd, 0,
		-1, exceptionTypes, exceptionTypeStarts,
		exceptionTypeEnds, bodyStart,true);
}
/**
 * @see IDocumentElementRequestor#enterField(
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
	int extendedTypeDimensionEnd)
 */
@Override
public void enterField(int declarationStart, int[] javaDocPositions, int modifiers, int modifiersStart,
	char[] type, int typeStart, int typeEnd, int typeDimensionCount, char[] name,
	int nameStart, int nameEnd, int extendedTypeDimensionCount,
	int extendedTypeDimensionEnd) {
	int[] sourceRange = {declarationStart,
		(extendedTypeDimensionEnd > nameEnd) ? extendedTypeDimensionEnd : nameEnd};
	int[] nameRange = {nameStart, nameEnd};
	int[] commentRange = {-1, -1};
	if (javaDocPositions != null) {
		int length = javaDocPositions.length;
		commentRange[0] = javaDocPositions[length - 2]; // get last javadoc comment (see bug 68772)
		commentRange[1] = javaDocPositions[length - 1];
	}
	int[] modifiersRange = {-1, -1};
	if (modifiersStart > -1) {
		modifiersRange[0] = modifiersStart;
		modifiersRange[1] = typeStart - 1;
	}
	int[] typeRange = {typeStart, typeEnd};
	boolean hasInitializer = false; // fixed on exitField
	int[] initializerRange = {-1, -1}; // fixed on exitField
	boolean isVariableDeclarator = false;
	if (this.fNode instanceof DOMField) {
		DOMField field = (DOMField)this.fNode;
		if (field.fTypeRange[0] == typeStart)
			isVariableDeclarator = true;
	}
	this.fNode = new DOMField(this.fDocument, sourceRange, CharOperation.charToString(name), nameRange, commentRange,
		modifiers, modifiersRange, typeRange, CharOperation.charToString(type), hasInitializer,
		initializerRange, isVariableDeclarator);
	addChild(this.fNode);
	this.fStack.push(this.fNode);
}
/**
 * @see IDocumentElementRequestor#enterInterface(
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
	int bodyStart)
 */
@Override
public void enterInterface(int declarationStart, int[] javaDocPositions, int modifiers, int modifiersStart, int keywordStart,
	char[] name, int nameStart, int nameEnd, char[][] superinterfaces,
	int[] superinterfaceStarts, int[] superinterfaceEnds, int bodyStart) {

	enterType(declarationStart, javaDocPositions, modifiers, modifiersStart, keywordStart,
		name, nameStart, nameEnd, null, -1, -1, superinterfaces,
		superinterfaceStarts, superinterfaceEnds, bodyStart, false);
}
/**
 * @see IDocumentElementRequestor#enterMethod(
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
	int bodyStart)
 */
@Override
public void enterMethod(int declarationStart, int[] javaDocPositions, int modifiers, int modifiersStart,
	char[] returnType, int returnTypeStart, int returnTypeEnd, int returnTypeDimensionCount,
	char[] name, int nameStart, int nameEnd, char[][] parameterTypes, int[] parameterTypeStarts,
	int[] parameterTypeEnds, char[][] parameterNames, int[] parameterNameStarts,
	int[] parameterNameEnds, int parametersEnd, int extendedReturnTypeDimensionCount,
	int extendedReturnTypeDimensionEnd, char[][] exceptionTypes, int[] exceptionTypeStarts,
	int[] exceptionTypeEnds, int bodyStart) {
	enterAbstractMethod(declarationStart, javaDocPositions, modifiers, modifiersStart,
		returnType, returnTypeStart, returnTypeEnd, returnTypeDimensionCount,
		name, nameStart, nameEnd, parameterTypes, parameterTypeStarts,
		parameterTypeEnds, parameterNames, parameterNameStarts,
		parameterNameEnds, parametersEnd, extendedReturnTypeDimensionCount,
		extendedReturnTypeDimensionEnd, exceptionTypes, exceptionTypeStarts,
		exceptionTypeEnds, bodyStart,false);
}

protected void enterType(int declarationStart, int[] javaDocPositions,
	int modifiers, int modifiersStart, int keywordStart, char[] name,
	int nameStart, int nameEnd, char[] superclass, int superclassStart,
	int superclassEnd, char[][] superinterfaces, int[] superinterfaceStarts,
	int[] superinterfaceEnds, int bodyStart, boolean isClass) {
	if (this.fBuildingType) {
		int[] sourceRange = {declarationStart, -1}; // will be fixed in the exit
		int[] commentRange = {-1, -1};
		if (javaDocPositions != null) {
			int length = javaDocPositions.length;
			commentRange[0] = javaDocPositions[length - 2];  // get last javadoc comment (see bug 68772)
			commentRange[1] = javaDocPositions[length - 1];
		}
		int[] modifiersRange = {-1, -1};
		if (modifiersStart > -1) {
			modifiersRange[0] = modifiersStart;
			modifiersRange[1] = (modifiersStart > -1) ? keywordStart - 1 : -1;
		}
		int[] typeKeywordRange = {keywordStart, nameStart - 1};
		int[] nameRange = new int[] {nameStart, nameEnd};
		int[] extendsKeywordRange = {-1, -1};
		int[] superclassRange = {-1, -1};
		int[] implementsKeywordRange = {-1, -1};
		int[] interfacesRange = {-1, -1};
		if (isClass) {
			if (superclass != null) {
				extendsKeywordRange[0] = nameEnd + 1;
				extendsKeywordRange[1] = superclassStart - 1;
				superclassRange[0] = superclassStart;
				superclassRange[1] = superclassEnd;
			}
			if (superinterfaces != null && superinterfaces.length > 0) {
				superclassRange[1] = superclassEnd;
				if (superclassEnd > -1) {
					implementsKeywordRange[0] = superclassEnd + 1;
				} else {
					implementsKeywordRange[0] = nameEnd + 1;
				}
				implementsKeywordRange[1] = superinterfaceStarts[0] - 1;
				interfacesRange[0] = superinterfaceStarts[0];
				interfacesRange[1] = superinterfaceEnds[superinterfaces.length - 1];
			}
		} else {
			if (superinterfaces != null && superinterfaces.length > 0) {
				extendsKeywordRange[0] = nameEnd + 1;
				extendsKeywordRange[1] = superinterfaceStarts[0] - 1;
				interfacesRange[0] = superinterfaceStarts[0];
				interfacesRange[1] = superinterfaceEnds[superinterfaces.length - 1];
			}
		}
		int[] openBodyRange = {bodyStart, -1}; // fixed by setTypeRanges(DOMNode)
		int[] closeBodyRange = {-1, -1}; // will be fixed in exit
		this.fNode = new DOMType(this.fDocument, sourceRange, new String(name), nameRange, commentRange,
			modifiers, modifiersRange, typeKeywordRange, superclassRange, extendsKeywordRange,
			CharOperation.charArrayToStringArray(superinterfaces), interfacesRange,
			implementsKeywordRange, openBodyRange,
			closeBodyRange, isClass);
		addChild(this.fNode);
		this.fStack.push(this.fNode);
	}
}
/**
 * Finishes the configuration of the constructors and methods.
 *
 * @param bodyEnd - a source position corresponding to the closing bracket of the method
 * @param declarationEnd - a source position corresponding to the end of the method
 *		declaration.  This can include whitespace and comments following the closing bracket.
 */
protected void exitAbstractMethod(int bodyEnd, int declarationEnd) {
	DOMMethod method = (DOMMethod) this.fStack.pop();
	method.setSourceRangeEnd(declarationEnd);
	method.setBodyRangeEnd(bodyEnd + 1);
	this.fNode = method;
	if (this.fBuildingSingleMember) {
		this.fFinishedSingleMember= true;
	}
}
/**
 * Finishes the configuration of the class DOM object which
 * was created by a previous enterClass call.
 *
 * @see IDocumentElementRequestor#exitClass(int, int)
 */
@Override
public void exitClass(int bodyEnd, int declarationEnd) {
	exitType(bodyEnd, declarationEnd);
}
/**
 * Finishes the configuration of the method DOM object which
 * was created by a previous enterConstructor call.
 *
 * @see IDocumentElementRequestor#exitConstructor(int, int)
 */
@Override
public void exitConstructor(int bodyEnd, int declarationEnd) {
	exitAbstractMethod(bodyEnd, declarationEnd);
}
/**
 * Finishes the configuration of the field DOM object which
 * was created by a previous enterField call.
 *
 * @see IDocumentElementRequestor#exitField(int, int)
 */
@Override
public void exitField(int bodyEnd, int declarationEnd) {
	DOMField field = (DOMField)this.fStack.pop();
	if (field.getEndPosition() < declarationEnd) {
		field.setSourceRangeEnd(declarationEnd);
		int nameEnd = field.fNameRange[1];
		if (nameEnd < bodyEnd) {
			/* see 1FVIIV8 - obtain initializer range */
			String initializer = new String(this.fDocument, nameEnd + 1, bodyEnd - nameEnd);
			int index = initializer.indexOf('=');
			if (index > -1) {
				field.setHasInitializer(true);
				field.setInitializerRange(nameEnd + index + 2, bodyEnd);
			}
		}
	}
	this.fFieldCount++;
	this.fNode = field;
	if (this.fBuildingSingleMember) {
		this.fFinishedSingleMember= true;
	}
}
/**
 * Finishes the configuration of the interface DOM object which
 * was created by a previous enterInterface call.
 *
 * @see IDocumentElementRequestor#exitInterface(int, int)
 */
@Override
public void exitInterface(int bodyEnd, int declarationEnd) {
	exitType(bodyEnd, declarationEnd);
}
/**
 * Finishes the configuration of the method DOM object which
 * was created by a previous enterMethod call.
 *
 * @see IDocumentElementRequestor#exitMethod(int, int)
 */
@Override
public void exitMethod(int bodyEnd, int declarationEnd) {
	exitAbstractMethod(bodyEnd, declarationEnd);
}
/**
 * Creates a new parser.
 */
protected DocumentElementParser getParser(Map settings) {
	return new DocumentElementParser(this, new DefaultProblemFactory(), new CompilerOptions(settings));
}
/**
 * Initializes the builder to create a document fragment.
 *
 * @param sourceCode - the document containing the source code to be analyzed
 * @param buildingCompilationUnit - true if a the document is being analyzed to
 *		create a compilation unit, otherwise false
 * @param buildingType - true if the document is being analyzed to create a
 *		type or compilation unit
 * @param singleMember - true if building a single member
 */
protected void initializeBuild(char[] sourceCode, boolean buildingCompilationUnit, boolean buildingType, boolean singleMember) {
	super.initializeBuild(sourceCode, buildingCompilationUnit, buildingType);
	this.fBuildingSingleMember= singleMember;
	this.fFinishedSingleMember= false;

}
}
