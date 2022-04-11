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
 *								Bug 429958 - [1.8][null] evaluate new DefaultLocation attribute of @NonNullByDefault
 *******************************************************************************/
package org.eclipse.jdt.internal.codeassist.select;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ast.ParameterizedQualifiedTypeReference;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;

public class SelectionOnParameterizedQualifiedTypeReference extends ParameterizedQualifiedTypeReference {
	public SelectionOnParameterizedQualifiedTypeReference(char[][] previousIdentifiers, char[] selectionIdentifier, TypeReference[][] typeArguments, TypeReference[] assistTypeArguments, long[] positions) {
		super(
			CharOperation.arrayConcat(previousIdentifiers, selectionIdentifier),
			typeArguments,
			0,
			positions);
		int length =  this.typeArguments.length;
		System.arraycopy(this.typeArguments, 0, this.typeArguments = new TypeReference[length + 1][], 0, length);
		this.typeArguments[length] = assistTypeArguments;
	}

	@Override
	public TypeBinding resolveType(BlockScope scope, boolean checkBounds, int location) {
		super.resolveType(scope, checkBounds, location);
		//// removed unnecessary code to solve bug 94653
		//if(this.resolvedType != null && this.resolvedType.isRawType()) {
		//	ParameterizedTypeBinding parameterizedTypeBinding = scope.createParameterizedType(((RawTypeBinding)this.resolvedType).type, new TypeBinding[0], this.resolvedType.enclosingType());
		//	throw new SelectionNodeFound(parameterizedTypeBinding);
		//}
		throw new SelectionNodeFound(this.resolvedType);
	}

	@Override
	public TypeBinding resolveType(ClassScope scope, int location) {
		super.resolveType(scope, location);
		//// removed unnecessary code to solve bug 94653
		//if(this.resolvedType != null && this.resolvedType.isRawType()) {
		//	ParameterizedTypeBinding parameterizedTypeBinding = scope.createParameterizedType(((RawTypeBinding)this.resolvedType).type, new TypeBinding[0], this.resolvedType.enclosingType());
		//	throw new SelectionNodeFound(parameterizedTypeBinding);
		//}
		throw new SelectionNodeFound(this.resolvedType);
	}

	@Override
	public StringBuffer printExpression(int indent, StringBuffer output) {
		output.append("<SelectOnType:");//$NON-NLS-1$
		int length = this.tokens.length;
		for (int i = 0; i < length; i++) {
			if(i != 0) {
				output.append('.');
			}
			output.append(this.tokens[i]);
			TypeReference[] typeArgument = this.typeArguments[i];
			if (typeArgument != null) {
				output.append('<');
				int max = typeArgument.length - 1;
				for (int j = 0; j < max; j++) {
					typeArgument[j].print(0, output);
					output.append(", ");//$NON-NLS-1$
				}
				typeArgument[max].print(0, output);
				output.append('>');
			}

		}
		output.append('>');
		return output;
	}
}
