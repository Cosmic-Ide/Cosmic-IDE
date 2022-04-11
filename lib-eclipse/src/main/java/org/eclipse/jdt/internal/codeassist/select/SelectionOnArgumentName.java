/*******************************************************************************
 * Copyright (c) 2000, 2014 IBM Corporation and others.
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
 *     Stephan Herrmann - Contribution for
 *								Bug 392099 - [1.8][compiler][null] Apply null annotation on types for null analysis
 *******************************************************************************/
package org.eclipse.jdt.internal.codeassist.select;

import org.eclipse.jdt.internal.compiler.ast.Argument;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.MethodScope;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;

public class SelectionOnArgumentName extends Argument {

	public SelectionOnArgumentName(char[] name , long posNom , TypeReference tr , int modifiers){

		super(name, posNom, tr, modifiers);
	}

	public SelectionOnArgumentName(char[] name , long posNom , TypeReference tr , int modifiers, boolean typeElided){
		super(name, posNom, tr, modifiers, typeElided);
	}
	@Override
	public TypeBinding bind(MethodScope scope, TypeBinding typeBinding, boolean used) {

		super.bind(scope, typeBinding, used);
		throw new SelectionNodeFound(this.binding);
	}

	@Override
	public StringBuffer print(int indent, StringBuffer output) {

		printIndent(indent, output);
		output.append("<SelectionOnArgumentName:"); //$NON-NLS-1$
		if (this.type != null) this.type.print(0, output).append(' ');
		output.append(this.name);
		if (this.initialization != null) {
			output.append(" = ");//$NON-NLS-1$
			this.initialization.printExpression(0, output);
		}
		return output.append('>');
	}

	@Override
	public void resolve(BlockScope scope) {

		super.resolve(scope);
		throw new SelectionNodeFound(this.binding);
	}

	@Override
	public TypeBinding resolveForCatch(BlockScope scope) {
		super.resolveForCatch(scope);
		throw new SelectionNodeFound(this.binding);
	}
}
