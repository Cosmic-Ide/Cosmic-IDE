/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
package org.eclipse.jdt.internal.codeassist.select;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.ForeachStatement;
import org.eclipse.jdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;

public class SelectionOnLocalName extends LocalDeclaration{

	public SelectionOnLocalName(char[] name,	int sourceStart, int sourceEnd) {

		super(name, sourceStart, sourceEnd);
	}

	@Override
	public void resolve(BlockScope scope) {

		super.resolve(scope);
		if (isTypeNameVar(scope)) {
			if ((this.bits & ASTNode.IsForeachElementVariable) != 0 && scope.blockStatement instanceof ForeachStatement) {
				// small version extracted from ForeachStatement.resolve():

				ForeachStatement stat = (ForeachStatement) scope.blockStatement;
				TypeBinding collectionType = stat.collection == null ? null : stat.collection.resolveType((BlockScope) scope.parent);

				// Patch the resolved type
				if (!TypeBinding.equalsEquals(TypeBinding.NULL, collectionType)
						&& !TypeBinding.equalsEquals(TypeBinding.VOID, collectionType)) {
					TypeBinding elementType = ForeachStatement.getCollectionElementType(scope, collectionType);
					if (elementType != null) {
						this.patchType(elementType);
					}
				}
			}
		}
		throw new SelectionNodeFound(this.binding);
	}

	@Override
	public StringBuffer printAsExpression(int indent, StringBuffer output) {
		printIndent(indent, output);
		output.append("<SelectionOnLocalName:"); //$NON-NLS-1$
		printModifiers(this.modifiers, output);
		 this.type.print(0, output).append(' ').append(this.name);
		if (this.initialization != null) {
			output.append(" = "); //$NON-NLS-1$
			this.initialization.printExpression(0, output);
		}
		return output.append('>');
	}

	@Override
	public StringBuffer printStatement(int indent, StringBuffer output) {
		printAsExpression(indent, output);
		return output.append(';');
	}
}
