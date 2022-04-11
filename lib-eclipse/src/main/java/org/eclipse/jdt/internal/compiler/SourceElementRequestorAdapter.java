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
package org.eclipse.jdt.internal.compiler;

import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.ImportReference;

public class SourceElementRequestorAdapter implements ISourceElementRequestor {

	/**
	 * @see ISourceElementRequestor#acceptAnnotationTypeReference(char[][], int, int)
	 */
	@Override
	public void acceptAnnotationTypeReference(
		char[][] typeName,
		int sourceStart,
		int sourceEnd) {
		// default implementation: do nothing
	}

	/**
	 * @see ISourceElementRequestor#acceptAnnotationTypeReference(char[], int)
	 */
	@Override
	public void acceptAnnotationTypeReference(char[] typeName, int sourcePosition) {
		// default implementation: do nothing
	}

	/**
	 * @see ISourceElementRequestor#acceptConstructorReference(char[], int, int)
	 */
	@Override
	public void acceptConstructorReference(
		char[] typeName,
		int argCount,
		int sourcePosition) {
		// default implementation: do nothing
	}

	/**
	 * @see ISourceElementRequestor#acceptFieldReference(char[], int)
	 */
	@Override
	public void acceptFieldReference(char[] fieldName, int sourcePosition) {
		// default implementation: do nothing
	}

	/**
	 * @see ISourceElementRequestor#acceptImport(int, int, int, int, char[][], boolean, int)
	 */
	@Override
	public void acceptImport(
		int declarationStart,
		int declarationEnd,
		int nameStart,
		int nameEnd,
		char[][] tokens,
		boolean onDemand,
		int modifiers) {
		// default implementation: do nothing
	}

	/**
	 * @see ISourceElementRequestor#acceptLineSeparatorPositions(int[])
	 */
	@Override
	public void acceptLineSeparatorPositions(int[] positions) {
		// default implementation: do nothing
	}

	/**
	 * @see ISourceElementRequestor#acceptMethodReference(char[], int, int)
	 */
	@Override
	public void acceptMethodReference(
		char[] methodName,
		int argCount,
		int sourcePosition) {
		// default implementation: do nothing
	}

	/**
	 * @see ISourceElementRequestor#acceptPackage(ImportReference)
	 */
	@Override
	public void acceptPackage(ImportReference importReference) {
		// default implementation: do nothing
	}

	/**
	 * @see ISourceElementRequestor#acceptProblem(CategorizedProblem)
	 */
	@Override
	public void acceptProblem(CategorizedProblem problem) {
		// default implementation: do nothing
	}

	/**
	 * @see ISourceElementRequestor#acceptTypeReference(char[][], int, int)
	 */
	@Override
	public void acceptTypeReference(
		char[][] typeName,
		int sourceStart,
		int sourceEnd) {
		// default implementation: do nothing
	}

	/**
	 * @see ISourceElementRequestor#acceptTypeReference(char[], int)
	 */
	@Override
	public void acceptTypeReference(char[] typeName, int sourcePosition) {
		// default implementation: do nothing
	}

	/**
	 * @see ISourceElementRequestor#acceptUnknownReference(char[][], int, int)
	 */
	@Override
	public void acceptUnknownReference(
		char[][] name,
		int sourceStart,
		int sourceEnd) {
		// default implementation: do nothing
	}

	/**
	 * @see ISourceElementRequestor#acceptUnknownReference(char[], int)
	 */
	@Override
	public void acceptUnknownReference(char[] name, int sourcePosition) {
		// default implementation: do nothing
	}

	/**
	 * @see ISourceElementRequestor#enterCompilationUnit()
	 */
	@Override
	public void enterCompilationUnit() {
		// default implementation: do nothing
	}

	@Override
	public void enterConstructor(MethodInfo methodInfo) {
		// default implementation: do nothing
	}

	/**
	 * @see ISourceElementRequestor#enterField(ISourceElementRequestor.FieldInfo)
	 */
	@Override
	public void enterField(FieldInfo fieldInfo) {
		// default implementation: do nothing
	}

	/**
	 * @see ISourceElementRequestor#enterInitializer(int, int)
	 */
	@Override
	public void enterInitializer(int declarationStart, int modifiers) {
		// default implementation: do nothing
	}

	@Override
	public void enterMethod(MethodInfo methodInfo) {
		// default implementation: do nothing
	}

	@Override
	public void enterType(TypeInfo typeInfo) {
		// default implementation: do nothing
	}

	/**
	 * @see ISourceElementRequestor#exitCompilationUnit(int)
	 */
	@Override
	public void exitCompilationUnit(int declarationEnd) {
		// default implementation: do nothing
	}

	/**
	 * @see ISourceElementRequestor#exitConstructor(int)
	 */
	@Override
	public void exitConstructor(int declarationEnd) {
		// default implementation: do nothing
	}

	/**
	 * @see ISourceElementRequestor#exitField(int, int, int)
	 */
	@Override
	public void exitField(int initializationStart, int declarationEnd, int declarationSourceEnd) {
		// default implementation: do nothing
	}

	/**
	 * @see ISourceElementRequestor#exitInitializer(int)
	 */
	@Override
	public void exitInitializer(int declarationEnd) {
		// default implementation: do nothing
	}

	/**
	 * @see ISourceElementRequestor#exitMethod(int, Expression)
	 */
	@Override
	public void exitMethod(int declarationEnd, Expression defaultValue) {
		// default implementation: do nothing
	}

	/**
	 * @see ISourceElementRequestor#exitType(int)
	 */
	@Override
	public void exitType(int declarationEnd) {
		// default implementation: do nothing
	}

	@Override
	public void exitRecordComponent(int declarationEnd, int declarationSourceEnd) {
		// default implementation: do nothing
	}

}

