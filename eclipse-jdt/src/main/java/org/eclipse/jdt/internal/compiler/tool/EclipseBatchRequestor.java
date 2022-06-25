/*******************************************************************************
 * Copyright (c) 2014 Gauthier JACQUES, IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Gauthier JACQUES - Initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.tool;

import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;

import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.ICompilerRequestor;
import org.eclipse.jdt.internal.compiler.batch.Main;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblemFactory;

public class EclipseBatchRequestor implements ICompilerRequestor {

	private final Main compiler;
	private int lineDelta = 0;
	private final DiagnosticListener<? super JavaFileObject> diagnosticListener;
	private final DefaultProblemFactory problemFactory;

	public EclipseBatchRequestor(Main compiler,
			DiagnosticListener<? super JavaFileObject> diagnosticListener,
			DefaultProblemFactory problemFactory) {
		this.compiler = compiler;
		this.diagnosticListener = diagnosticListener;
		this.problemFactory = problemFactory;
	}

	@Override
	public void acceptResult(CompilationResult compilationResult) {
		if (compilationResult.lineSeparatorPositions != null) {
			int unitLineCount = compilationResult.lineSeparatorPositions.length;
			this.lineDelta += unitLineCount;
			if (this.compiler.showProgress && this.lineDelta > 2000) {
				// in -log mode, dump a dot every 2000 lines compiled
				this.compiler.logger.logProgress();
				this.lineDelta = 0;
			}
		}
		this.compiler.logger.startLoggingSource(compilationResult);
		if (compilationResult.hasProblems() || compilationResult.hasTasks()) {
			this.compiler.logger.logProblems(
											compilationResult.getAllProblems(),
											compilationResult.compilationUnit.getContents(),
											this.compiler);
			reportProblems(compilationResult);
		}
		this.compiler.outputClassFiles(compilationResult);
		this.compiler.logger.endLoggingSource();
	}

	private void reportProblems(CompilationResult result) {
		for (CategorizedProblem problem : result.getAllProblems()) {
			EclipseDiagnostic diagnostic = EclipseDiagnostic.newInstance(problem, this.problemFactory);
			this.diagnosticListener.report(diagnostic);
		}
	}
}
