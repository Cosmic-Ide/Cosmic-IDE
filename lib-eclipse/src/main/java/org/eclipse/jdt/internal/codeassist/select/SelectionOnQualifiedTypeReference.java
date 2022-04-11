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
 * reduce a type reference containing the completion identifier as part
 * of a qualified name.
 * e.g.
 *
 *	class X extends java.lang.[start]Object[end]
 *
 *	---> class X extends <SelectOnType:java.lang.Object>
 *
 */

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ast.QualifiedTypeReference;
import org.eclipse.jdt.internal.compiler.lookup.Binding;
import org.eclipse.jdt.internal.compiler.lookup.PackageBinding;
import org.eclipse.jdt.internal.compiler.lookup.ProblemReasons;
import org.eclipse.jdt.internal.compiler.lookup.ProblemReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.Scope;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;

public class SelectionOnQualifiedTypeReference extends QualifiedTypeReference {
public SelectionOnQualifiedTypeReference(char[][] previousIdentifiers, char[] selectionIdentifier, long[] positions) {
	super(
		CharOperation.arrayConcat(previousIdentifiers, selectionIdentifier),
		positions);
}
@Override
public void aboutToResolve(Scope scope) {
	getTypeBinding(scope.parent); // step up from the ClassScope
}
@Override
protected TypeBinding getTypeBinding(Scope scope) {
	// it can be a package, type or member type
	Binding binding = scope.getTypeOrPackage(this.tokens);
	if (!binding.isValidBinding()) {
		// tolerate some error cases
		if (binding.problemId() == ProblemReasons.NotVisible){
			throw new SelectionNodeFound(binding);
		}

		if (binding instanceof TypeBinding) {
			scope.problemReporter().invalidType(this, (TypeBinding) binding);
		} else if (binding instanceof PackageBinding) {
			ProblemReferenceBinding problemBinding = new ProblemReferenceBinding(((PackageBinding)binding).compoundName, null, binding.problemId());
			scope.problemReporter().invalidType(this, problemBinding);
		}

		throw new SelectionNodeFound();
	}

	throw new SelectionNodeFound(binding);
}
@Override
public StringBuffer printExpression(int indent, StringBuffer output) {

	output.append("<SelectOnType:"); //$NON-NLS-1$
	for (int i = 0, length = this.tokens.length; i < length; i++) {
		if (i > 0) output.append('.');
		output.append(this.tokens[i]);
	}
	return output.append('>');
}
}
