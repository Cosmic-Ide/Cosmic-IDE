/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others.
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

import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.ast.MemberValuePair;
import org.eclipse.jdt.internal.compiler.ast.NormalAnnotation;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;

public class CompletionOnAnnotationMemberValuePair extends NormalAnnotation {
	public MemberValuePair completedMemberValuePair;
	public CompletionOnAnnotationMemberValuePair(TypeReference type, int sourceStart, MemberValuePair[] memberValuePairs, MemberValuePair completedMemberValuePair) {
		super(type, sourceStart);
		this.memberValuePairs = memberValuePairs;
		this.completedMemberValuePair = completedMemberValuePair;
	}

	@Override
	public TypeBinding resolveType(BlockScope scope) {
		super.resolveType(scope);

		if (this.resolvedType == null || !this.resolvedType.isValidBinding()) {
			throw new CompletionNodeFound();
		} else {
			throw new CompletionNodeFound(this.completedMemberValuePair, scope);
		}
	}

	@Override
	public StringBuffer printExpression(int indent, StringBuffer output) {
		output.append('@');
		this.type.printExpression(0, output);
		output.append('(');
		if (this.memberValuePairs != null) {
			for (int i = 0, max = this.memberValuePairs.length; i < max; i++) {
				if (i > 0) {
					output.append(',');
				}
				this.memberValuePairs[i].print(indent, output);
			}
			output.append(',');
		}
		this.completedMemberValuePair.print(indent, output);
		output.append(')');

		return output;
	}

	@Override
	public void traverse(ASTVisitor visitor, ClassScope scope) {
		super.traverse(visitor, scope);
		this.completedMemberValuePair.traverse(visitor, scope);
	}

	@Override
	public void traverse(ASTVisitor visitor, BlockScope scope) {
		super.traverse(visitor, scope);
		this.completedMemberValuePair.traverse(visitor, scope);
	}
}
