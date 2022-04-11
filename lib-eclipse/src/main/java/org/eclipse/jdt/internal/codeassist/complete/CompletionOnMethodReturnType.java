/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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
package org.eclipse.jdt.internal.codeassist.complete;

import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;

public class CompletionOnMethodReturnType extends MethodDeclaration {
	public CompletionOnMethodReturnType(TypeReference returnType, CompilationResult compilationResult){
		super(compilationResult);
		this.returnType = returnType;
		this.sourceStart = returnType.sourceStart;
		this.sourceEnd = returnType.sourceEnd;
	}

	@Override
	public void resolveStatements() {
			throw new CompletionNodeFound(this, this.scope);
	}

	@Override
	public StringBuffer print(int tab, StringBuffer output) {
		return this.returnType.print(tab, output);
	}

}
