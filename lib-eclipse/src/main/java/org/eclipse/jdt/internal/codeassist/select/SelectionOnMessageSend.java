/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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
 * reduce a message send containing the cursor.
 * e.g.
 *
 *	class X {
 *    void foo() {
 *      this.[start]bar[end](1, 2)
 *    }
 *  }
 *
 *	---> class X {
 *         void foo() {
 *           <SelectOnMessageSend:this.bar(1, 2)>
 *         }
 *       }
 *
 */

import org.eclipse.jdt.internal.compiler.ast.MessageSend;
import org.eclipse.jdt.internal.compiler.lookup.Binding;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.ProblemReasons;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;

public class SelectionOnMessageSend extends MessageSend {

	/*
	 * Cannot answer default abstract match, iterate in superinterfaces of declaring class
	 * for a better match (default abstract match came from scope lookups).
	 */
	private MethodBinding findNonDefaultAbstractMethod(MethodBinding methodBinding) {

		ReferenceBinding[] itsInterfaces = methodBinding.declaringClass.superInterfaces();
		if (itsInterfaces != Binding.NO_SUPERINTERFACES) {
			ReferenceBinding[] interfacesToVisit = itsInterfaces;
			int nextPosition = interfacesToVisit.length;

			for (int i = 0; i < nextPosition; i++) {
				ReferenceBinding currentType = interfacesToVisit[i];
				MethodBinding[] methods = currentType.getMethods(methodBinding.selector);
				if(methods != null) {
					for (int k = 0; k < methods.length; k++) {
						if(methodBinding.areParametersEqual(methods[k]))
							return methods[k];
					}
				}

				if ((itsInterfaces = currentType.superInterfaces()) != Binding.NO_SUPERINTERFACES) {
					int itsLength = itsInterfaces.length;
					if (nextPosition + itsLength >= interfacesToVisit.length)
						System.arraycopy(interfacesToVisit, 0, interfacesToVisit = new ReferenceBinding[nextPosition + itsLength + 5], 0, nextPosition);
					nextInterface : for (int a = 0; a < itsLength; a++) {
						ReferenceBinding next = itsInterfaces[a];
						for (int b = 0; b < nextPosition; b++)
							if (TypeBinding.equalsEquals(next, interfacesToVisit[b])) continue nextInterface;
						interfacesToVisit[nextPosition++] = next;
					}
				}
			}
		}
		return methodBinding;
	}

	@Override
	public StringBuffer printExpression(int indent, StringBuffer output) {

		output.append("<SelectOnMessageSend:"); //$NON-NLS-1$
		if (!this.receiver.isImplicitThis()) this.receiver.printExpression(0, output).append('.');
		output.append(this.selector).append('(');
		if (this.arguments != null) {
			for (int i = 0; i < this.arguments.length; i++) {
				if (i > 0) output.append(", "); //$NON-NLS-1$
				this.arguments[i].printExpression(0, output);
			}
		}
		return output.append(")>"); //$NON-NLS-1$
	}

	@Override
	public TypeBinding resolveType(BlockScope scope) {

		TypeBinding type = super.resolveType(scope);
		if (type != null && type.isPolyType())
			return type; // wait for more inference/resolution

		// tolerate some error cases
		if(this.binding == null ||
					!(this.binding.isValidBinding() ||
						this.binding.problemId() == ProblemReasons.NotVisible
						|| this.binding.problemId() == ProblemReasons.InheritedNameHidesEnclosingName
						|| this.binding.problemId() == ProblemReasons.NonStaticReferenceInConstructorInvocation
						|| this.binding.problemId() == ProblemReasons.NonStaticReferenceInStaticContext)) {
			throw new SelectionNodeFound();
		} else {
			if(this.binding.isDefaultAbstract()) {
				throw new SelectionNodeFound(findNonDefaultAbstractMethod(this.binding)); // 23594
			} else {
				throw new SelectionNodeFound(this.binding);
			}
		}
	}
}
