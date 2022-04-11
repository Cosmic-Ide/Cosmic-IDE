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
package org.eclipse.jdt.internal.compiler.batch;

import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.ICompilerRequestor;

public class BatchCompilerRequestor implements ICompilerRequestor {

    private Main compiler;
    private int lineDelta = 0;

    public BatchCompilerRequestor(Main compiler) {
        this.compiler = compiler;
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
        	this.compiler.logger.logProblems(compilationResult.getAllProblems(), compilationResult.compilationUnit.getContents(), this.compiler);
            reportProblems(compilationResult);
        }
        this.compiler.outputClassFiles(compilationResult);
        this.compiler.logger.endLoggingSource();
    }

    protected void reportProblems(CompilationResult result) {
        // Nothing to do
    }
}
