/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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

/*
 * Selection node build by the parser in any case it was intending to
 * reduce a qualified name reference containing the assist identifier.
 * e.g.
 *
 *	class X {
 *    Y y;
 *    void foo() {
 *      y.fred.[start]ba[end]
 *    }
 *  }
 *
 *	---> class X {
 *         Y y;
 *         void foo() {
 *           <SelectOnName:y.fred.ba>
 *         }
 *       }
 *
 */

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ast.QualifiedNameReference;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.FieldBinding;
import org.eclipse.jdt.internal.compiler.lookup.MissingTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.ProblemFieldBinding;
import org.eclipse.jdt.internal.compiler.lookup.ProblemReasons;
import org.eclipse.jdt.internal.compiler.lookup.ProblemReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;

public class SelectionOnQualifiedNameReference extends QualifiedNameReference {

public SelectionOnQualifiedNameReference(char[][] previousIdentifiers, char[] selectionIdentifier, long[] positions) {
	super(
		CharOperation.arrayConcat(previousIdentifiers, selectionIdentifier),
		positions,
		(int) (positions[0] >>> 32),
		(int) positions[positions.length - 1]);
}
@Override
public StringBuffer printExpression(int indent, StringBuffer output) {

	output.append("<SelectOnName:"); //$NON-NLS-1$
	for (int i = 0, length = this.tokens.length; i < length; i++) {
		if (i > 0) output.append('.');
		output.append(this.tokens[i]);
	}
	return output.append('>');
}
@Override
public TypeBinding resolveType(BlockScope scope) {
	// it can be a package, type, member type, local variable or field
	this.binding = scope.getBinding(this.tokens, this);
	if (!this.binding.isValidBinding()) {
		if (this.binding instanceof ProblemFieldBinding) {
			// tolerate some error cases
			if (this.binding.problemId() == ProblemReasons.NotVisible
					|| this.binding.problemId() == ProblemReasons.InheritedNameHidesEnclosingName
					|| this.binding.problemId() == ProblemReasons.NonStaticReferenceInConstructorInvocation
					|| this.binding.problemId() == ProblemReasons.NonStaticReferenceInStaticContext) {
				throw new SelectionNodeFound(this.binding);
			}
			scope.problemReporter().invalidField(this, (FieldBinding) this.binding);
		} else if (this.binding instanceof ProblemReferenceBinding || this.binding instanceof MissingTypeBinding) {
			// tolerate some error cases
			if (this.binding.problemId() == ProblemReasons.NotVisible){
				throw new SelectionNodeFound(this.binding);
			}
			scope.problemReporter().invalidType(this, (TypeBinding) this.binding);
		} else {
			scope.problemReporter().unresolvableReference(this, this.binding);
		}
		throw new SelectionNodeFound();
	}
	throw new SelectionNodeFound(this.binding);
}
}
