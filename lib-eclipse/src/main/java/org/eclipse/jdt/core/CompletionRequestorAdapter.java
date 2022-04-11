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
package org.eclipse.jdt.core;

import org.eclipse.jdt.core.compiler.IProblem;

/**
 * Adapter of the requestor interface <code>ICompletionRequestor</code>.
 * <p>
 * This class is intended to be instantiated and subclassed by clients.
 * </p>
 *
 * @see ICompletionRequestor
 * @since 2.0
 * @deprecated Subclass {@link CompletionRequestor} instead.
 */
public class CompletionRequestorAdapter implements ICompletionRequestor {

	@Override
	public void acceptAnonymousType(
		char[] superTypePackageName,
		char[] superTypeName,
		char[][] parameterPackageNames,
		char[][] parameterTypeNames,
		char[][] parameterNames,
		char[] completionName,
		int modifiers,
		int completionStart,
		int completionEnd,
		int relevance) {
			// default behavior is to ignore
	}

	@Override
	public void acceptClass(
		char[] packageName,
		char[] className,
		char[] completionName,
		int modifiers,
		int completionStart,
		int completionEnd,
		int relevance) {
			// default behavior is to ignore
	}

	@Override
	public void acceptError(IProblem error) {
		// default behavior is to ignore
	}

	@Override
	public void acceptField(
		char[] declaringTypePackageName,
		char[] declaringTypeName,
		char[] name,
		char[] typePackageName,
		char[] typeName,
		char[] completionName,
		int modifiers,
		int completionStart,
		int completionEnd,
		int relevance) {
			// default behavior is to ignore
	}

	@Override
	public void acceptInterface(
		char[] packageName,
		char[] interfaceName,
		char[] completionName,
		int modifiers,
		int completionStart,
		int completionEnd,
		int relevance) {
			// default behavior is to ignore
	}

	@Override
	public void acceptKeyword(
		char[] keywordName,
		int completionStart,
		int completionEnd,
		int relevance) {
			// default behavior is to ignore
	}

	@Override
	public void acceptLabel(
		char[] labelName,
		int completionStart,
		int completionEnd,
		int relevance) {
			// default behavior is to ignore
	}

	@Override
	public void acceptLocalVariable(
		char[] name,
		char[] typePackageName,
		char[] typeName,
		int modifiers,
		int completionStart,
		int completionEnd,
		int relevance) {
			// default behavior is to ignore
	}

	@Override
	public void acceptMethod(
		char[] declaringTypePackageName,
		char[] declaringTypeName,
		char[] selector,
		char[][] parameterPackageNames,
		char[][] parameterTypeNames,
		char[][] parameterNames,
		char[] returnTypePackageName,
		char[] returnTypeName,
		char[] completionName,
		int modifiers,
		int completionStart,
		int completionEnd,
		int relevance) {
			// default behavior is to ignore
	}

	@Override
	public void acceptMethodDeclaration(
		char[] declaringTypePackageName,
		char[] declaringTypeName,
		char[] selector,
		char[][] parameterPackageNames,
		char[][] parameterTypeNames,
		char[][] parameterNames,
		char[] returnTypePackageName,
		char[] returnTypeName,
		char[] completionName,
		int modifiers,
		int completionStart,
		int completionEnd,
		int relevance) {
			// default behavior is to ignore
	}

	@Override
	public void acceptModifier(
		char[] modifierName,
		int completionStart,
		int completionEnd,
		int relevance) {
			// default behavior is to ignore
	}

	@Override
	public void acceptPackage(
		char[] packageName,
		char[] completionName,
		int completionStart,
		int completionEnd,
		int relevance) {
			// default behavior is to ignore
	}

	@Override
	public void acceptType(
		char[] packageName,
		char[] typeName,
		char[] completionName,
		int completionStart,
		int completionEnd,
		int relevance) {
			// default behavior is to ignore
	}

	@Override
	public void acceptVariableName(
		char[] typePackageName,
		char[] typeName,
		char[] name,
		char[] completionName,
		int completionStart,
		int completionEnd,
		int relevance) {
			// default behavior is to ignore
	}
}
