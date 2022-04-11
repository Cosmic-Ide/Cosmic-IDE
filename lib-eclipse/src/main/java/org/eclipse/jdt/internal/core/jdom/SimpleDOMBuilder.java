/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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

import java.util.Map;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.jdom.*;
import org.eclipse.jdt.internal.compiler.ISourceElementRequestor;
import org.eclipse.jdt.internal.compiler.SourceElementParser;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.ImportReference;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblemFactory;
/**
 * A DOM builder that uses the SourceElementParser
 * @deprecated The JDOM was made obsolete by the addition in 2.0 of the more
 * powerful, fine-grained DOM/AST API found in the
 * org.eclipse.jdt.core.dom package.
 */
public class SimpleDOMBuilder extends AbstractDOMBuilder implements ISourceElementRequestor {

/**
 * Does nothing.
 */
@Override
public void acceptProblem(CategorizedProblem problem) {
	// nothing to do
}

@Override
public void acceptImport(int declarationStart, int declarationEnd, int nameStart, int nameEnd, char[][] tokens, boolean onDemand, int modifiers) {
	int[] sourceRange = {declarationStart, declarationEnd};
	String importName = new String(CharOperation.concatWith(tokens, '.'));
	/** name is set to contain the '*' */
	if (onDemand) {
		importName+=".*"; //$NON-NLS-1$
	}
	this.fNode= new DOMImport(this.fDocument, sourceRange, importName, onDemand, modifiers);
	addChild(this.fNode);
}
@Override
public void acceptPackage(ImportReference importReference) {
	int[] sourceRange= new int[] {importReference.declarationSourceStart, importReference.declarationSourceEnd};
	char[] name = CharOperation.concatWith(importReference.getImportName(), '.');
	this.fNode= new DOMPackage(this.fDocument, sourceRange, new String(name));
	addChild(this.fNode);
}
/**
 * @see IDOMFactory#createCompilationUnit(String, String)
 */
public IDOMCompilationUnit createCompilationUnit(String sourceCode, String name) {
	return createCompilationUnit(sourceCode.toCharArray(), name.toCharArray());
}
/**
 * @see IDOMFactory#createCompilationUnit(String, String)
 */
@Override
public IDOMCompilationUnit createCompilationUnit(ICompilationUnit compilationUnit) {
	initializeBuild(compilationUnit.getContents(), true, true);
	getParser(JavaCore.getOptions()).parseCompilationUnit(compilationUnit, false/*diet parse*/, null/*no progress*/);
	return super.createCompilationUnit(compilationUnit);
}
/**
 * Creates a new DOMMethod and inizializes.
 */
protected void enterAbstractMethod(MethodInfo methodInfo) {

	int[] sourceRange = {methodInfo.declarationStart, -1}; // will be fixed up on exit
	int[] nameRange = {methodInfo.nameSourceStart, methodInfo.nameSourceEnd};
	this.fNode = new DOMMethod(this.fDocument, sourceRange, CharOperation.charToString(methodInfo.name), nameRange, methodInfo.modifiers,
		methodInfo.isConstructor, CharOperation.charToString(methodInfo.returnType),
		CharOperation.charArrayToStringArray(methodInfo.parameterTypes),
		CharOperation.charArrayToStringArray(methodInfo.parameterNames),
		CharOperation.charArrayToStringArray(methodInfo.exceptionTypes));
	addChild(this.fNode);
	this.fStack.push(this.fNode);

	// type parameters not supported by JDOM
}
/**
 */
@Override
public void enterConstructor(MethodInfo methodInfo) {
	/* see 1FVIIQZ */
	String nameString = new String(this.fDocument, methodInfo.nameSourceStart, methodInfo.nameSourceEnd - methodInfo.nameSourceStart);
	int openParenPosition = nameString.indexOf('(');
	if (openParenPosition > -1)
		methodInfo.nameSourceEnd = methodInfo.nameSourceStart + openParenPosition - 1;

	enterAbstractMethod(methodInfo);
}
/**
 */
@Override
public void enterField(FieldInfo fieldInfo) {

	int[] sourceRange = {fieldInfo.declarationStart, -1};
	int[] nameRange = {fieldInfo.nameSourceStart, fieldInfo.nameSourceEnd};
	boolean isSecondary= false;
	if (this.fNode instanceof DOMField) {
		isSecondary = fieldInfo.declarationStart == this.fNode.fSourceRange[0];
	}
	this.fNode = new DOMField(this.fDocument, sourceRange, CharOperation.charToString(fieldInfo.name), nameRange,
		fieldInfo.modifiers, CharOperation.charToString(fieldInfo.type), isSecondary);
	addChild(this.fNode);
	this.fStack.push(this.fNode);
}
/**

 */
@Override
public void enterInitializer(int declarationSourceStart, int modifiers) {
	int[] sourceRange = {declarationSourceStart, -1};
	this.fNode = new DOMInitializer(this.fDocument, sourceRange, modifiers);
	addChild(this.fNode);
	this.fStack.push(this.fNode);
}
/**
 */
@Override
public void enterMethod(MethodInfo methodInfo) {
	enterAbstractMethod(methodInfo);
}
/**
 */
@Override
public void enterType(TypeInfo typeInfo) {
	if (this.fBuildingType) {
		int[] sourceRange = {typeInfo.declarationStart, -1}; // will be fixed in the exit
		int[] nameRange = new int[] {typeInfo.nameSourceStart, typeInfo.nameSourceEnd};
		this.fNode = new DOMType(this.fDocument, sourceRange, new String(typeInfo.name), nameRange,
			typeInfo.modifiers, CharOperation.charArrayToStringArray(typeInfo.superinterfaces), TypeDeclaration.kind(typeInfo.modifiers) == TypeDeclaration.CLASS_DECL); // TODO (jerome) should pass in kind
		addChild(this.fNode);
		this.fStack.push(this.fNode);

		// type parameters not supported by JDOM
	}
}
/**
 * Finishes the configuration of the method DOM object which
 * was created by a previous enterConstructor call.
 *
 * @see ISourceElementRequestor#exitConstructor(int)
 */
@Override
public void exitConstructor(int declarationEnd) {
	exitMember(declarationEnd);
}
/**
 */
@Override
public void exitField(int initializationStart, int declarationEnd, int declarationSourceEnd) {
	exitMember(declarationEnd);
}
/**
 */
@Override
public void exitInitializer(int declarationEnd) {
	exitMember(declarationEnd);
}
/**
 * Finishes the configuration of the member.
 *
 * @param declarationEnd - a source position corresponding to the end of the method
 *		declaration.  This can include whitespace and comments following the closing bracket.
 */
protected void exitMember(int declarationEnd) {
	DOMMember m= (DOMMember) this.fStack.pop();
	m.setSourceRangeEnd(declarationEnd);
	this.fNode = m;
}
/**
 */
@Override
public void exitMethod(int declarationEnd, Expression defaultValue) {
	exitMember(declarationEnd);
}
/**
 * @see AbstractDOMBuilder#exitType
 *
 * @param declarationEnd - a source position corresponding to the end of the class
 *		declaration.  This can include whitespace and comments following the closing bracket.
 */
@Override
public void exitType(int declarationEnd) {
	exitType(declarationEnd, declarationEnd);
}
/**
 * Creates a new parser.
 */
protected SourceElementParser getParser(Map<String, String> settings) {
	return new SourceElementParser(this, new DefaultProblemFactory(), new CompilerOptions(settings), false/*don't report local declarations*/, true/*optimize string literals*/);
}

@Override
public void exitRecordComponent(int declarationEnd, int declarationSourceEnd) {
	// TODO Auto-generated method stub
}
}
