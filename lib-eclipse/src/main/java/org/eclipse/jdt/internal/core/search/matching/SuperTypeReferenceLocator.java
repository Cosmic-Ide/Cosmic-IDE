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
 *******************************************************************************/
package org.eclipse.jdt.internal.core.search.matching;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.internal.compiler.ast.*;
import org.eclipse.jdt.internal.compiler.lookup.*;
import org.eclipse.jdt.internal.core.search.indexing.IIndexConstants;

public class SuperTypeReferenceLocator extends PatternLocator {

protected SuperTypeReferencePattern pattern;

public SuperTypeReferenceLocator(SuperTypeReferencePattern pattern) {
	super(pattern);

	this.pattern = pattern;
}
//public int match(ASTNode node, MatchingNodeSet nodeSet) - SKIP IT
//public int match(ConstructorDeclaration node, MatchingNodeSet nodeSet) - SKIP IT
//public int match(Expression node, MatchingNodeSet nodeSet) - SKIP IT
//public int match(FieldDeclaration node, MatchingNodeSet nodeSet) - SKIP IT

@Override
public int match(LambdaExpression node, MatchingNodeSet nodeSet) {
	if (this.pattern.superRefKind != SuperTypeReferencePattern.ONLY_SUPER_INTERFACES)
		return IMPOSSIBLE_MATCH;
	nodeSet.mustResolve = true;
	return nodeSet.addMatch(node, POSSIBLE_MATCH);
}
//public int match(MethodDeclaration node, MatchingNodeSet nodeSet) - SKIP IT
//public int match(MessageSend node, MatchingNodeSet nodeSet) - SKIP IT
//public int match(Reference node, MatchingNodeSet nodeSet) - SKIP IT
//public int match(TypeDeclaration node, MatchingNodeSet nodeSet) - SKIP IT
@Override
public int match(TypeReference node, MatchingNodeSet nodeSet) {
	if (this.flavors != SUPERTYPE_REF_FLAVOR) return IMPOSSIBLE_MATCH;
	if (this.pattern.superSimpleName == null)
		return nodeSet.addMatch(node, this.pattern.mustResolve ? POSSIBLE_MATCH : ACCURATE_MATCH);

	char[] typeRefSimpleName = null;
	if (node instanceof SingleTypeReference) {
		typeRefSimpleName = ((SingleTypeReference) node).token;
	} else { // QualifiedTypeReference
		char[][] tokens = ((QualifiedTypeReference) node).tokens;
		typeRefSimpleName = tokens[tokens.length-1];
	}
	if (matchesName(this.pattern.superSimpleName, typeRefSimpleName))
		return nodeSet.addMatch(node, this.pattern.mustResolve ? POSSIBLE_MATCH : ACCURATE_MATCH);

	return IMPOSSIBLE_MATCH;
}

@Override
protected int matchContainer() {
	return CLASS_CONTAINER;
}

@Override
protected void matchReportReference(ASTNode reference, IJavaElement element, Binding elementBinding, int accuracy, MatchLocator locator) throws CoreException {
	if (elementBinding instanceof ReferenceBinding) {
		ReferenceBinding referenceBinding = (ReferenceBinding) elementBinding;
		if (referenceBinding.isClass() && this.pattern.typeSuffix == IIndexConstants.INTERFACE_SUFFIX) {
			// do not report class if expected types are only interfaces
			return;
		}
		if (referenceBinding.isInterface() && this.pattern.typeSuffix == IIndexConstants.CLASS_SUFFIX) {
			// do not report interface if expected types are only classes
			return;
		}
	}
	super.matchReportReference(reference, element, elementBinding, accuracy, locator);
}
@Override
protected int referenceType() {
	return IJavaElement.TYPE;
}
@Override
public int resolveLevel(ASTNode node) {
	TypeBinding typeBinding = null;
	if (node instanceof LambdaExpression) {
		LambdaExpression lambda = (LambdaExpression) node;
		typeBinding = lambda.resolvedType;
	} else {
		if (!(node instanceof TypeReference)) return IMPOSSIBLE_MATCH;
		TypeReference typeRef = (TypeReference) node;
		typeBinding = typeRef.resolvedType;
	}

	if (typeBinding instanceof ArrayBinding)
		typeBinding = ((ArrayBinding) typeBinding).leafComponentType;
	if (typeBinding instanceof ProblemReferenceBinding)
		typeBinding = ((ProblemReferenceBinding) typeBinding).closestMatch();

	if (typeBinding == null || !typeBinding.isValidBinding()) return INACCURATE_MATCH;
	return resolveLevelForType(this.pattern.superSimpleName, this.pattern.superQualification, typeBinding);
}
@Override
public int resolveLevel(Binding binding) {
	if (binding == null) return INACCURATE_MATCH;
	if (!(binding instanceof ReferenceBinding)) return IMPOSSIBLE_MATCH;

	ReferenceBinding type = (ReferenceBinding) binding;
	int level = IMPOSSIBLE_MATCH;
	if (this.pattern.superRefKind != SuperTypeReferencePattern.ONLY_SUPER_INTERFACES) {
		level = resolveLevelForType(this.pattern.superSimpleName, this.pattern.superQualification, type.superclass());
		if (level == ACCURATE_MATCH) return ACCURATE_MATCH;
	}

	if (this.pattern.superRefKind != SuperTypeReferencePattern.ONLY_SUPER_CLASSES) {
		ReferenceBinding[] superInterfaces = type.superInterfaces();
		for (int i = 0, max = superInterfaces.length; i < max; i++) {
			int newLevel = resolveLevelForType(this.pattern.superSimpleName, this.pattern.superQualification, superInterfaces[i]);
			if (newLevel > level) {
				if (newLevel == ACCURATE_MATCH) return ACCURATE_MATCH;
				level = newLevel;
			}
		}
	}
	return level;
}
@Override
public String toString() {
	return "Locator for " + this.pattern.toString(); //$NON-NLS-1$
}
}
