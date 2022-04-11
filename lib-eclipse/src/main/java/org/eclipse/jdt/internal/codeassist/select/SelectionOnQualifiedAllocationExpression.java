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
 * reduce an allocation expression containing the cursor.
 * If the allocation expression is not qualified, the enclosingInstance field
 * is null.
 * e.g.
 *
 *	class X {
 *    void foo() {
 *      new [start]Bar[end](1, 2)
 *    }
 *  }
 *
 *	---> class X {
 *         void foo() {
 *           <SelectOnAllocationExpression:new Bar(1, 2)>
 *         }
 *       }
 *
 */

import org.eclipse.jdt.internal.compiler.ast.ConstructorDeclaration;
import org.eclipse.jdt.internal.compiler.ast.QualifiedAllocationExpression;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.lookup.Binding;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.LocalTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.ProblemReasons;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;

public class SelectionOnQualifiedAllocationExpression extends QualifiedAllocationExpression {

	public SelectionOnQualifiedAllocationExpression() {
		// constructor without argument
	}

	public SelectionOnQualifiedAllocationExpression(TypeDeclaration anonymous) {
		super(anonymous);
	}

	@Override
	public StringBuffer printExpression(int indent, StringBuffer output) {
		if (this.enclosingInstance == null)
			output.append("<SelectOnAllocationExpression:");  //$NON-NLS-1$
		else
			output.append("<SelectOnQualifiedAllocationExpression:"); //$NON-NLS-1$

		return super.printExpression(indent, output).append('>');
	}

	@Override
	public TypeBinding resolveType(BlockScope scope) {
		super.resolveType(scope);

		if (this.binding == null) {
			throw new SelectionNodeFound();
		}

		// tolerate some error cases
		if (!this.binding.isValidBinding()) {
			switch (this.binding.problemId()) {
				case ProblemReasons.NotVisible:
					// visibility is ignored
					break;
				case ProblemReasons.NotFound:
					if (this.resolvedType != null && this.resolvedType.isValidBinding()) {
						throw new SelectionNodeFound(this.resolvedType);
					}
					throw new SelectionNodeFound();
				default:
					throw new SelectionNodeFound();
			}
		}

		if (this.anonymousType == null)
			throw new SelectionNodeFound(this.binding);

		// if selecting a type for an anonymous type creation, we have to
		// find its target super constructor (if extending a class) or its target
		// super interface (if extending an interface)
		if (this.anonymousType.binding != null) {
			LocalTypeBinding localType = (LocalTypeBinding) this.anonymousType.binding;
			if (localType.superInterfaces == Binding.NO_SUPERINTERFACES) {
				// find the constructor binding inside the super constructor call
				ConstructorDeclaration constructor = (ConstructorDeclaration) this.anonymousType.declarationOf(this.binding.original());
				if (constructor != null) {
					throw new SelectionNodeFound(constructor.constructorCall.binding);
				}
				throw new SelectionNodeFound(this.binding);
			}
			// open on the only super interface
			throw new SelectionNodeFound(localType.superInterfaces[0]);
		} else {
			if (this.resolvedType.isInterface()) {
				throw new SelectionNodeFound(this.resolvedType);
			}
			throw new SelectionNodeFound(this.binding);
		}
	}
}
