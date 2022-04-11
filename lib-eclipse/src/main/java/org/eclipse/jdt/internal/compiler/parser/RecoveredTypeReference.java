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
import org.eclipse.jdt.internal.compiler.ast.TypeReference;

public class RecoveredTypeReference extends RecoveredElement {
	public TypeReference typeReference;

	public RecoveredTypeReference(TypeReference typeReference, RecoveredElement parent, int bracketBalance) {
		super(parent, bracketBalance);
		this.typeReference = typeReference;
	}

	/*
	 * Answer the associated parsed structure
	 */
	@Override
	public ASTNode parseTree(){
		return this.typeReference;
	}
	public TypeReference updateTypeReference() {
		return this.typeReference;
	}
	/*
	 * Answer the very source end of the corresponding parse node
	 */
	@Override
	public String toString(int tab) {
		return tabString(tab) + "Recovered typereference: " + this.typeReference.toString(); //$NON-NLS-1$
	}
	public TypeReference updatedImportReference(){
		return this.typeReference;
	}
	@Override
	public void updateParseTree(){
		updatedImportReference();
	}
}