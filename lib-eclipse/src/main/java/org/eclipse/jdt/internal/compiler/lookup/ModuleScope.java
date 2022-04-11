/*******************************************************************************
 * Copyright (c) 2019, 2020 SAP SE and others
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.lookup;

import org.eclipse.jdt.internal.compiler.ast.ModuleDeclaration;
import org.eclipse.jdt.internal.compiler.problem.ProblemReporter;

public class ModuleScope extends BlockScope {
	public ModuleDeclaration referenceContext;

	public ModuleScope(Scope parent, ModuleDeclaration referenceContext) {
		super(Scope.MODULE_SCOPE, parent);
		this.referenceContext = referenceContext;
	}

	/**
	 * Answer the problem reporter to use for raising new problems.
	 *
	 * Note that as a side-effect, this updates the current reference context
	 * (here: module declaration) in case the problem handler decides it is necessary
	 * to abort.
	 */
	@Override
	public ProblemReporter problemReporter() {
		ProblemReporter problemReporter = referenceCompilationUnit().problemReporter;
		problemReporter.referenceContext = this.referenceContext;
		return problemReporter;
	}
}
