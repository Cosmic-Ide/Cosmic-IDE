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
package org.eclipse.jdt.internal.compiler.impl;

/*
 * Implementors are valid compilation contexts from which we can
 * escape in case of error:
 * For example: method, type, compilation unit or a lambda expression.
 */

import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;

public interface ReferenceContext {

	void abort(int abortLevel, CategorizedProblem problem);

	CompilationResult compilationResult();

	CompilationUnitDeclaration getCompilationUnitDeclaration();

	boolean hasErrors();

	void tagAsHavingErrors();

	void tagAsHavingIgnoredMandatoryErrors(int problemId);

}
