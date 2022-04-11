/*******************************************************************************
 * Copyright (c) 2016, 2017 IBM Corporation and others.
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
 *
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.ast;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.internal.compiler.lookup.ModuleBinding;
import org.eclipse.jdt.internal.compiler.lookup.PlainPackageBinding;
import org.eclipse.jdt.internal.compiler.lookup.Scope;
import org.eclipse.jdt.internal.compiler.problem.ProblemSeverities;
import org.eclipse.jdt.internal.compiler.util.HashtableOfObject;

public abstract class PackageVisibilityStatement extends ModuleStatement {
	public ImportReference pkgRef;
	public ModuleReference[] targets;
	public char[] pkgName;
	public PlainPackageBinding resolvedPackage;

	public PackageVisibilityStatement(ImportReference pkgRef, ModuleReference[] targets) {
		this.pkgRef = pkgRef;
		this.pkgName = CharOperation.concatWith(this.pkgRef.tokens, '.');
		this.targets = targets;
	}
	public boolean isQualified() {
		return this.targets != null && this.targets.length > 0;
	}

	public ModuleReference[] getTargetedModules() {
		return this.targets;
	}

	public boolean resolve(Scope scope) {
		boolean errorsExist = resolvePackageReference(scope) == null;
		if (this.isQualified()) {
			HashtableOfObject modules = new HashtableOfObject(this.targets.length);
			for (int i = 0; i < this.targets.length; i++) {
				ModuleReference ref = this.targets[i];
				// targets will be resolved later (during ModuleDeclaration.resolveModuleDirectives())
				if (modules.containsKey(ref.moduleName)) {
					scope.problemReporter().duplicateModuleReference(IProblem.DuplicateModuleRef, ref);
					errorsExist = true;
				} else {
					modules.put(ref.moduleName, ref);
				}
			}
		}
		return !errorsExist;
	}
	public int computeSeverity(int problemId) {
		return ProblemSeverities.Error;
	}
	protected PlainPackageBinding resolvePackageReference(Scope scope) {
		if (this.resolvedPackage != null)
			return this.resolvedPackage;
		ModuleDeclaration exportingModule = scope.compilationUnitScope().referenceContext.moduleDeclaration;
		ModuleBinding src = exportingModule.binding;
		this.resolvedPackage = src != null ? src.getOrCreateDeclaredPackage(this.pkgRef.tokens) : null;
		return this.resolvedPackage;
	}

	@Override
	public StringBuffer print(int indent, StringBuffer output) {
		this.pkgRef.print(indent, output);
		if (this.isQualified()) {
			output.append(" to "); //$NON-NLS-1$
			for (int i = 0; i < this.targets.length; i++) {
				if (i > 0) output.append(", "); //$NON-NLS-1$
				this.targets[i].print(0, output);
			}
		}
		return output;
	}
}
