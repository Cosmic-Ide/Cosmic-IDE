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

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.ExportsStatement;
import org.eclipse.jdt.internal.compiler.ast.ModuleReference;

public class RecoveredExport extends RecoveredElement {

	public ExportsStatement exportReference;
	RecoveredModuleReference[] targets;
	int targetCount = 0;

	public RecoveredExport(ExportsStatement exportReference, RecoveredElement parent, int bracketBalance) {
		super(parent, bracketBalance);
		this.exportReference = exportReference;
	}
	public RecoveredElement add(ModuleReference target,  int bracketBalance1) {

		if (this.targets == null) {
			this.targets = new RecoveredModuleReference[5];
			this.targetCount = 0;
		} else {
			if (this.targetCount == this.targets.length) {
				System.arraycopy(
					this.targets,
					0,
					(this.targets = new RecoveredModuleReference[2 * this.targetCount]),
					0,
					this.targetCount);
			}
		}
		RecoveredModuleReference element = new RecoveredModuleReference(target, this, bracketBalance1);
		this.targets[this.targetCount++] = element;

		/* if target not finished, then target becomes current */
		if (target.sourceEnd == 0) return element;
		return this;

	}
	/*
	 * Answer the associated parsed structure
	 */
	@Override
	public ASTNode parseTree(){
		return this.exportReference;
	}
	/*
	 * Answer the very source end of the corresponding parse node
	 */
	@Override
	public int sourceEnd(){
		return this.exportReference.declarationSourceEnd;
	}
	@Override
	public String toString(int tab) {
		return tabString(tab) + "Recovered export: " + this.exportReference.toString(); //$NON-NLS-1$
	}
	public ExportsStatement updatedExportReference(){
		if (this.targetCount > 0) {
			int existingCount = this.exportReference.targets != null ? this.exportReference.targets.length : 0, actualCount = 0;
			ModuleReference[] moduleRef1 = new ModuleReference[existingCount + this.targetCount];
			if (existingCount > 0) {
				System.arraycopy(this.exportReference.targets, 0, moduleRef1, 0, existingCount);
				actualCount = existingCount;
			}
			for (int i = 0, l = this.targetCount; i < l; ++i) {
				moduleRef1[actualCount++] = this.targets[i].updatedModuleReference();
			}
			this.exportReference.targets = moduleRef1;
		}
		return this.exportReference;
	}
	@Override
	public void updateParseTree(){
		updatedExportReference();
	}
	/*
	 * Update the declarationSourceEnd of the corresponding parse node
	 */
	@Override
	public void updateSourceEndIfNecessary(int bodyStart, int bodyEnd){
		if (this.exportReference.declarationSourceEnd == 0) {
			this.exportReference.declarationSourceEnd = bodyEnd;
			this.exportReference.declarationEnd = bodyEnd;
		}
	}

}
