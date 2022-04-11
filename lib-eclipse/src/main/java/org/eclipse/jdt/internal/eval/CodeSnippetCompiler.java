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
package org.eclipse.jdt.internal.eval;

import org.eclipse.jdt.internal.compiler.Compiler;
import org.eclipse.jdt.internal.compiler.ICompilerRequestor;
import org.eclipse.jdt.internal.compiler.IErrorHandlingPolicy;
import org.eclipse.jdt.internal.compiler.IProblemFactory;
import org.eclipse.jdt.internal.compiler.env.INameEnvironment;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;

/**
 * A compiler that compiles code snippets.
 */
public class CodeSnippetCompiler extends Compiler {

	EvaluationContext evaluationContext;
	int codeSnippetStart;
	int codeSnippetEnd;

	/**
	 * Creates a new code snippet compiler initialized with a code snippet parser.
	 */
	public CodeSnippetCompiler(
    		INameEnvironment environment,
    		IErrorHandlingPolicy policy,
    		CompilerOptions compilerOptions,
    		ICompilerRequestor requestor,
    		IProblemFactory problemFactory,
    		EvaluationContext evaluationContext,
    		int codeSnippetStart,
    		int codeSnippetEnd) {
		super(environment, policy, compilerOptions, requestor, problemFactory);
		this.codeSnippetStart = codeSnippetStart;
		this.codeSnippetEnd = codeSnippetEnd;
		this.evaluationContext = evaluationContext;
		this.parser =
			new CodeSnippetParser(
				this.problemReporter,
				evaluationContext,
				this.options.parseLiteralExpressionsAsConstants,
				codeSnippetStart,
				codeSnippetEnd);
		this.parseThreshold = 1;
		// fully parse only the code snippet compilation unit
	}

	@Override
	public void initializeParser() {
		this.parser =
			new CodeSnippetParser(
				this.problemReporter,
				this.evaluationContext,
				this.options.parseLiteralExpressionsAsConstants,
				this.codeSnippetStart,
				this.codeSnippetEnd);
		}
}
