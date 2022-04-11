/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
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
package org.eclipse.jdt.internal.compiler.parser;

import org.eclipse.jdt.internal.compiler.ast.ProvidesStatement;
import org.eclipse.jdt.internal.compiler.ast.SingleTypeReference;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;

public class RecoveredProvidesStatement extends RecoveredModuleStatement {
	SingleTypeReference impl;

	public RecoveredProvidesStatement(ProvidesStatement providesStatement, RecoveredElement parent, int bracketBalance) {
		super(providesStatement, parent, bracketBalance);
	}
	public RecoveredElement add(SingleTypeReference impl1,  int bracketBalance1) {
		this.impl = impl1;
		return this;
	}

	@Override
	public String toString(int tab) {
		return tabString(tab) + "Recovered Provides: " + super.toString(); //$NON-NLS-1$
	}
	public ProvidesStatement updatedProvidesStatement(){
		ProvidesStatement providesStatement = (ProvidesStatement) this.moduleStatement;
		if (providesStatement.implementations == null) { // only for with - actual impl by normal parse
			providesStatement.implementations = this.impl != null ? new TypeReference[] {this.impl} : new TypeReference[0]; // dummy for completion
		}
		return providesStatement;
	}
	@Override
	public void updateParseTree(){
		updatedProvidesStatement();
	}
}
