/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Gauthier JACQUES - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.compiler.tool;

import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;

import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.batch.BatchCompilerRequestor;
import org.eclipse.jdt.internal.compiler.batch.Main;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblemFactory;

public class EclipseCompilerRequestor extends BatchCompilerRequestor {

    private final DiagnosticListener<? super JavaFileObject> diagnosticListener;
    private final DefaultProblemFactory problemFactory;

    public EclipseCompilerRequestor(Main compiler, DiagnosticListener<? super JavaFileObject> diagnosticListener, DefaultProblemFactory problemFactory) {
        super(compiler);
        this.diagnosticListener = diagnosticListener;
        this.problemFactory = problemFactory;
    }

    @Override
	protected void reportProblems(CompilationResult result) {
    	if (this.diagnosticListener != null) {
    		for (CategorizedProblem problem : result.getAllProblems()) {
                EclipseDiagnostic diagnostic = EclipseDiagnostic.newInstance(problem, this.problemFactory);
                this.diagnosticListener.report(diagnostic);
            }
    	}
    }
}
