/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
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
package org.eclipse.jdt.internal.core.search.matching;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.internal.compiler.ast.*;
import org.eclipse.jdt.internal.compiler.lookup.Binding;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeVariableBinding;

/**
 * Search engine locator for type parameters matches.
 */
public class TypeParameterLocator extends PatternLocator {

	protected TypeParameterPattern pattern;

	public TypeParameterLocator(TypeParameterPattern pattern) {
		super(pattern);
		this.pattern = pattern;
	}

	/*
	 * Verify whether a type reference matches name pattern.
	 * Type parameter references (i.e. type arguments) are compiler type reference nodes
	 */
	@Override
	public int match(TypeReference node, MatchingNodeSet nodeSet) {
		if (this.pattern.findReferences) {
			if (node instanceof SingleTypeReference) { // Type parameter cannot be qualified
				if (matchesName(this.pattern.name, ((SingleTypeReference) node).token)) {
					int level = this.pattern.mustResolve ? POSSIBLE_MATCH : ACCURATE_MATCH;
					return nodeSet.addMatch(node, level);
				}
			}
		}
		return IMPOSSIBLE_MATCH;
	}


	/*
	 * Verify whether a type parameter matches name pattern.
	 */
	@Override
	public int match(TypeParameter node, MatchingNodeSet nodeSet) {
		if (this.pattern.findDeclarations) {
			if (matchesName(this.pattern.name, node.name)) {
				int level = this.pattern.mustResolve ? POSSIBLE_MATCH : ACCURATE_MATCH;
				return nodeSet.addMatch(node, level);
			}
		}
		return IMPOSSIBLE_MATCH;
	}

	/*
	 * While searching for references, need to match all containers as we can have references in javadoc comments.
	 * Otherwise, only class or method container can declare type parameters.
	 */
	@Override
	protected int matchContainer() {
		if (this.pattern.findReferences) {
			return ALL_CONTAINER;
		}
		return CLASS_CONTAINER | METHOD_CONTAINER;
	}

	/*
	 * Verify that a type variable binding match pattern infos.
	 * For types, only look at declaring member name.
	 * For methods, also look at declaring class and parameters type names
	 */
	protected int matchTypeParameter(TypeVariableBinding variable, boolean matchName) {
		if (variable == null || variable.declaringElement == null) return INACCURATE_MATCH;
		if (variable.declaringElement instanceof ReferenceBinding) {
			ReferenceBinding refBinding  = (ReferenceBinding) variable.declaringElement;
			if (matchesName(refBinding.sourceName, this.pattern.declaringMemberName)) {
				return ACCURATE_MATCH;
			}
		} else if (variable.declaringElement instanceof MethodBinding) {
			MethodBinding methBinding  = (MethodBinding) variable.declaringElement;
			if (matchesName(methBinding.declaringClass.sourceName, this.pattern.methodDeclaringClassName) &&
				(methBinding.isConstructor() || matchesName(methBinding.selector, this.pattern.declaringMemberName))) {
				int length = this.pattern.methodArgumentTypes==null ? 0 : this.pattern.methodArgumentTypes.length;
				if (methBinding.parameters == null) {
					if (length == 0) return ACCURATE_MATCH;
				} else if (methBinding.parameters.length == length){
					for (int i=0; i<length; i++) {
						if (!matchesName(methBinding.parameters[i].shortReadableName(), this.pattern.methodArgumentTypes[i])) {
							return IMPOSSIBLE_MATCH;
						}
					}
					return ACCURATE_MATCH;
				}
			}
		}
		return IMPOSSIBLE_MATCH;
	}

	@Override
	protected int referenceType() {
		return IJavaElement.TYPE_PARAMETER;
	}

	/*
	 * Resolve level for a possible matching node.
	 * Only type references while searching references and type parameters
	 * while searching declarations are valid.
	 */
	@Override
	public int resolveLevel(ASTNode possibleMatchingNode) {
		if (this.pattern.findReferences) {
			if (possibleMatchingNode instanceof SingleTypeReference) {
				return resolveLevel(((SingleTypeReference) possibleMatchingNode).resolvedType);
			}
		}
		if (this.pattern.findDeclarations) {
			if (possibleMatchingNode instanceof TypeParameter) {
				return matchTypeParameter(((TypeParameter) possibleMatchingNode).binding, true);
			}
		}
		return IMPOSSIBLE_MATCH;
	}

	/*
	 * Resolve level for a binding.
	 * Only type variable bindings are valid.
	 */
	@Override
	public int resolveLevel(Binding binding) {
		if (binding == null) return INACCURATE_MATCH;
		if (!(binding instanceof TypeVariableBinding)) return IMPOSSIBLE_MATCH;

		return matchTypeParameter((TypeVariableBinding) binding, true);
	}

	@Override
	public String toString() {
		return "Locator for " + this.pattern.toString(); //$NON-NLS-1$
	}
}
