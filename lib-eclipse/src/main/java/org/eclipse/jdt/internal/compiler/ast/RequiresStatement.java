/*******************************************************************************
 * Copyright (c) 2016, 2018 IBM Corporation and others.
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

import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.lookup.ModuleBinding;
import org.eclipse.jdt.internal.compiler.lookup.Scope;

public class RequiresStatement extends ModuleStatement {

	public ModuleReference module;
	public ModuleBinding resolvedBinding;
	public int modifiers = ClassFileConstants.AccDefault;
	public int modifiersSourceStart;

	public RequiresStatement(ModuleReference module) {
		this.module = module;
	}
	public boolean isTransitive() {
		return (this.modifiers & ClassFileConstants.ACC_TRANSITIVE) != 0;
	}
	public boolean isStatic() {
		return (this.modifiers & ClassFileConstants.ACC_STATIC_PHASE) != 0;
	}
	@Override
	public StringBuffer print(int indent, StringBuffer output) {
		output.append("requires "); //$NON-NLS-1$
		if (isTransitive())
			output.append("transitive "); //$NON-NLS-1$
		if (isStatic())
			output.append("static "); //$NON-NLS-1$
		this.module.print(indent, output);
		output.append(";"); //$NON-NLS-1$
	return output;
	}
	public ModuleBinding resolve(Scope scope) {
		if (this.resolvedBinding != null)
			return this.resolvedBinding;
		this.resolvedBinding = this.module.resolve(scope);
		if (scope != null) {
			if (this.resolvedBinding == null) {
				scope.problemReporter().invalidModule(this.module);
			} else if (this.resolvedBinding.hasUnstableAutoName()) {
				scope.problemReporter().autoModuleWithUnstableName(this.module);
			}
		}
		return this.resolvedBinding;
	}

}
