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

/*
 * Completion node build by the parser in any case it was intending to
 * reduce an type reference located as a potential return type for a class
 * member, containing the cursor location.
 * This node is only a fake-field wrapper of the actual completion node
 * which is accessible as the fake-field type.
 * e.g.
 *
 *	class X {
 *    Obj[cursor]
 *  }
 *
 *	---> class X {
 *         <CompleteOnType:Obj>;
 *       }
 *
 * The source range is always of length 0.
 * The arguments of the allocation expression are all the arguments defined
 * before the cursor.
 */

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ast.*;

public class CompletionOnFieldType extends FieldDeclaration {
	public boolean isLocalVariable;

public CompletionOnFieldType(TypeReference type, boolean isLocalVariable){
	super();
	this.sourceStart = type.sourceStart;
	this.sourceEnd = type.sourceEnd;
	this.type = type;
	this.name = CharOperation.NO_CHAR;
	this.isLocalVariable = isLocalVariable;
	if (type instanceof CompletionOnSingleTypeReference) {
	    ((CompletionOnSingleTypeReference) type).fieldTypeCompletionNode = this;
	}
}

@Override
public StringBuffer printStatement(int tab, StringBuffer output) {
	return this.type.print(tab, output).append(';');
}
}
