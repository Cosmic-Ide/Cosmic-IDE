/*******************************************************************************
 * Copyright (c) 2002, 2009 IBM Corporation and others.
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
package org.eclipse.jdt.internal.core.util;

import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.internal.compiler.CompilationResult;

/**
 * Use to keep track of recorded information during the parsing like comment positions,
 * line ends or problems.
 */
public class RecordedParsingInformation {
	public CategorizedProblem[] problems;
	public int problemsCount;
	public int[] lineEnds;
	public int[][] commentPositions;

	public RecordedParsingInformation(CategorizedProblem[] problems, int[] lineEnds, int[][] commentPositions) {
		this.problems = problems;
		this.lineEnds = lineEnds;
		this.commentPositions = commentPositions;
		this.problemsCount = problems != null ? problems.length : 0;
	}

	void updateRecordedParsingInformation(CompilationResult compilationResult) {
		if (compilationResult.problems != null) {
			this.problems = compilationResult.problems;
			this.problemsCount = this.problems.length;
		}
	}
}
