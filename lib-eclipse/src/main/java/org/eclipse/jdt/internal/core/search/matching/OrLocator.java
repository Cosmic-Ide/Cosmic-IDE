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
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.ConstructorDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ImportReference;
import org.eclipse.jdt.internal.compiler.ast.LambdaExpression;
import org.eclipse.jdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.jdt.internal.compiler.ast.MemberValuePair;
import org.eclipse.jdt.internal.compiler.ast.MessageSend;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.QualifiedTypeReference;
import org.eclipse.jdt.internal.compiler.ast.Reference;
import org.eclipse.jdt.internal.compiler.ast.ReferenceExpression;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeParameter;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.eclipse.jdt.internal.compiler.lookup.Binding;
import org.eclipse.jdt.internal.compiler.lookup.FieldBinding;
import org.eclipse.jdt.internal.compiler.lookup.MemberTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;

public class OrLocator extends PatternLocator {

protected PatternLocator[] patternLocators;

public OrLocator(OrPattern pattern) {
	super(pattern);

	SearchPattern[] patterns = pattern.patterns;
	int length = patterns.length;
	this.patternLocators = new PatternLocator[length];
	for (int i = 0; i < length; i++)
		this.patternLocators[i] = PatternLocator.patternLocator(patterns[i]);
}
@Override
public void initializePolymorphicSearch(MatchLocator locator) {
	for (int i = 0, length = this.patternLocators.length; i < length; i++)
		this.patternLocators[i].initializePolymorphicSearch(locator);
}
@Override
public int match(Annotation node, MatchingNodeSet nodeSet) {
	int level = IMPOSSIBLE_MATCH;
	for (int i = 0, length = this.patternLocators.length; i < length; i++) {
		int newLevel = this.patternLocators[i].match(node, nodeSet);
		if (newLevel > level) {
			if (newLevel == ACCURATE_MATCH) return ACCURATE_MATCH;
			level = newLevel;
		}
	}
	return level;
}
@Override
public int match(ASTNode node, MatchingNodeSet nodeSet) {
	int level = IMPOSSIBLE_MATCH;
	for (int i = 0, length = this.patternLocators.length; i < length; i++) {
		int newLevel = this.patternLocators[i].match(node, nodeSet);
		if (newLevel > level) {
			if (newLevel == ACCURATE_MATCH) return ACCURATE_MATCH;
			level = newLevel;
		}
	}
	return level;
}
@Override
public int match(ConstructorDeclaration node, MatchingNodeSet nodeSet) {
	int level = IMPOSSIBLE_MATCH;
	for (int i = 0, length = this.patternLocators.length; i < length; i++) {
		int newLevel = this.patternLocators[i].match(node, nodeSet);
		if (newLevel > level) {
			if (newLevel == ACCURATE_MATCH) return ACCURATE_MATCH;
			level = newLevel;
		}
	}
	return level;
}
@Override
public int match(Expression node, MatchingNodeSet nodeSet) {
	int level = IMPOSSIBLE_MATCH;
	for (int i = 0, length = this.patternLocators.length; i < length; i++) {
		int newLevel = this.patternLocators[i].match(node, nodeSet);
		if (newLevel > level) {
			if (newLevel == ACCURATE_MATCH) return ACCURATE_MATCH;
			level = newLevel;
		}
	}
	return level;
}
@Override
public int match(FieldDeclaration node, MatchingNodeSet nodeSet) {
	int level = IMPOSSIBLE_MATCH;
	for (int i = 0, length = this.patternLocators.length; i < length; i++) {
		int newLevel = this.patternLocators[i].match(node, nodeSet);
		if (newLevel > level) {
			if (newLevel == ACCURATE_MATCH) return ACCURATE_MATCH;
			level = newLevel;
		}
	}
	return level;
}
@Override
public int match(LambdaExpression node, MatchingNodeSet nodeSet) {
	int level = IMPOSSIBLE_MATCH;
	for (int i = 0, length = this.patternLocators.length; i < length; i++) {
		int newLevel = this.patternLocators[i].match(node, nodeSet);
		if (newLevel > level) {
			if (newLevel == ACCURATE_MATCH) return ACCURATE_MATCH;
			level = newLevel;
		}
	}
	return level;
}
@Override
public int match(LocalDeclaration node, MatchingNodeSet nodeSet) {
	int level = IMPOSSIBLE_MATCH;
	for (int i = 0, length = this.patternLocators.length; i < length; i++) {
		int newLevel = this.patternLocators[i].match(node, nodeSet);
		if (newLevel > level) {
			if (newLevel == ACCURATE_MATCH) return ACCURATE_MATCH;
			level = newLevel;
		}
	}
	return level;
}
@Override
public int match(MethodDeclaration node, MatchingNodeSet nodeSet) {
	int level = IMPOSSIBLE_MATCH;
	for (int i = 0, length = this.patternLocators.length; i < length; i++) {
		int newLevel = this.patternLocators[i].match(node, nodeSet);
		if (newLevel > level) {
			if (newLevel == ACCURATE_MATCH) return ACCURATE_MATCH;
			level = newLevel;
		}
	}
	return level;
}
@Override
public int match(MemberValuePair node, MatchingNodeSet nodeSet) {
	int level = IMPOSSIBLE_MATCH;
	for (int i = 0, length = this.patternLocators.length; i < length; i++) {
		int newLevel = this.patternLocators[i].match(node, nodeSet);
		if (newLevel > level) {
			if (newLevel == ACCURATE_MATCH) return ACCURATE_MATCH;
			level = newLevel;
		}
	}
	return level;
}
@Override
public int match(MessageSend node, MatchingNodeSet nodeSet) {
	int level = IMPOSSIBLE_MATCH;
	for (int i = 0, length = this.patternLocators.length; i < length; i++) {
		int newLevel = this.patternLocators[i].match(node, nodeSet);
		if (newLevel > level) {
			if (newLevel == ACCURATE_MATCH) return ACCURATE_MATCH;
			level = newLevel;
		}
	}
	return level;
}
@Override
public int match(Reference node, MatchingNodeSet nodeSet) {
	int level = IMPOSSIBLE_MATCH;
	for (int i = 0, length = this.patternLocators.length; i < length; i++) {
		int newLevel = this.patternLocators[i].match(node, nodeSet);
		if (newLevel > level) {
			if (newLevel == ACCURATE_MATCH) return ACCURATE_MATCH;
			level = newLevel;
		}
	}
	return level;
}
@Override
public int match(ReferenceExpression node, MatchingNodeSet nodeSet) {
	int level = IMPOSSIBLE_MATCH;
	for (int i = 0, length = this.patternLocators.length; i < length; i++) {
		int newLevel = this.patternLocators[i].match(node, nodeSet);
		if (newLevel > level) {
			if (newLevel == ACCURATE_MATCH) return ACCURATE_MATCH;
			level = newLevel;
		}
	}
	return level;
}
@Override
public int match(TypeDeclaration node, MatchingNodeSet nodeSet) {
	int level = IMPOSSIBLE_MATCH;
	for (int i = 0, length = this.patternLocators.length; i < length; i++) {
		int newLevel = this.patternLocators[i].match(node, nodeSet);
		if (newLevel > level) {
			if (newLevel == ACCURATE_MATCH) return ACCURATE_MATCH;
			level = newLevel;
		}
	}
	return level;
}
@Override
public int match(TypeParameter node, MatchingNodeSet nodeSet) {
	int level = IMPOSSIBLE_MATCH;
	for (int i = 0, length = this.patternLocators.length; i < length; i++) {
		int newLevel = this.patternLocators[i].match(node, nodeSet);
		if (newLevel > level) {
			if (newLevel == ACCURATE_MATCH) return ACCURATE_MATCH;
			level = newLevel;
		}
	}
	return level;
}
@Override
public int match(TypeReference node, MatchingNodeSet nodeSet) {
	int level = IMPOSSIBLE_MATCH;
	for (int i = 0, length = this.patternLocators.length; i < length; i++) {
		int newLevel = this.patternLocators[i].match(node, nodeSet);
		if (newLevel > level) {
			if (newLevel == ACCURATE_MATCH) return ACCURATE_MATCH;
			level = newLevel;
		}
	}
	return level;
}
@Override
protected int matchContainer() {
	int result = 0;
	for (int i = 0, length = this.patternLocators.length; i < length; i++)
		result |= this.patternLocators[i].matchContainer();
	return result;
}
@Override
protected void matchLevelAndReportImportRef(ImportReference importRef, Binding binding, MatchLocator locator) throws CoreException {

	// for static import, binding can be a field binding or a member type binding
	// verify that in this case binding is static and use declaring class for fields
	Binding refBinding = binding;
	if (importRef.isStatic()) {
		if (binding instanceof FieldBinding) {
			FieldBinding fieldBinding = (FieldBinding) binding;
			if (!fieldBinding.isStatic()) return;
			refBinding = fieldBinding.declaringClass;
		} else if (binding instanceof MethodBinding) {
			MethodBinding methodBinding = (MethodBinding) binding;
			if (!methodBinding.isStatic()) return;
			refBinding = methodBinding.declaringClass;
		} else if (binding instanceof MemberTypeBinding) {
			MemberTypeBinding memberBinding = (MemberTypeBinding) binding;
			if (!memberBinding.isStatic()) return;
		}
	}

	// Look for closest pattern
	PatternLocator closestPattern = null;
	int level = IMPOSSIBLE_MATCH;
	for (int i = 0, length = this.patternLocators.length; i < length; i++) {
		PatternLocator patternLocator = this.patternLocators[i];
		int newLevel = patternLocator.referenceType() == 0 ? IMPOSSIBLE_MATCH : patternLocator.resolveLevel(refBinding);
		if (newLevel > level) {
			closestPattern = patternLocator;
			if (newLevel == ACCURATE_MATCH) break;
			level = newLevel;
		}
	}
	if (closestPattern != null) {
		closestPattern.matchLevelAndReportImportRef(importRef, binding, locator);
	}
}
@Override
protected void matchReportImportRef(ImportReference importRef, Binding binding, IJavaElement element, int accuracy, MatchLocator locator) throws CoreException {
	PatternLocator closestPattern = null;
	int level = IMPOSSIBLE_MATCH;
	for (int i = 0, length = this.patternLocators.length; i < length; i++) {
		int newLevel = this.patternLocators[i].matchLevel(importRef);
		if (newLevel > level) {
			closestPattern = this.patternLocators[i];
			if (newLevel == ACCURATE_MATCH) break;
			level = newLevel;
		}
	}
	if (closestPattern != null)
		closestPattern.matchReportImportRef(importRef, binding, element, accuracy, locator);
}
@Override
protected void matchReportReference(ASTNode reference, IJavaElement element, IJavaElement localElement, IJavaElement[] otherElements, Binding elementBinding, int accuracy, MatchLocator locator) throws CoreException {
	PatternLocator closestPattern = null;
	int level = IMPOSSIBLE_MATCH;
	for (int i = 0, length = this.patternLocators.length; i < length; i++) {
		PatternLocator patternLocator = this.patternLocators[i];
		int newLevel = patternLocator.referenceType() == 0 ? IMPOSSIBLE_MATCH : patternLocator.resolveLevel(reference);
		if (newLevel > level) {
			closestPattern = patternLocator;
			if (newLevel == ACCURATE_MATCH) break;
			level = newLevel;
		}
	}
	if (closestPattern != null)
		closestPattern.matchReportReference(reference, element, localElement, otherElements, elementBinding, accuracy, locator);
}
@Override
protected void matchReportReference(ASTNode reference, IJavaElement element, Binding elementBinding, int accuracy, MatchLocator locator) throws CoreException {
	matchReportReference(reference, element, null, null, elementBinding, accuracy, locator);
}
@Override
public SearchMatch newDeclarationMatch(ASTNode reference, IJavaElement element, Binding elementBinding, int accuracy, int length, MatchLocator locator) {
	PatternLocator closestPattern = null;
	int level = IMPOSSIBLE_MATCH;
	for (int i = 0, pl = this.patternLocators.length; i < pl; i++) {
		PatternLocator patternLocator = this.patternLocators[i];
		int newLevel = patternLocator.referenceType() == 0 ? IMPOSSIBLE_MATCH : patternLocator.resolveLevel(reference);
		if (newLevel > level) {
			closestPattern = patternLocator;
			if (newLevel == ACCURATE_MATCH) break;
			level = newLevel;
		}
	}
	if (closestPattern != null) {
	    return closestPattern.newDeclarationMatch(reference, element, elementBinding, accuracy, length, locator);
	}
	// super implementation...
    return locator.newDeclarationMatch(element, elementBinding, accuracy, reference.sourceStart, length);
}
@Override
public int resolveLevel(ASTNode node) {
	int level = IMPOSSIBLE_MATCH;
	for (int i = 0, length = this.patternLocators.length; i < length; i++) {
		int newLevel = this.patternLocators[i].resolveLevel(node);
		if (newLevel > level) {
			if (newLevel == ACCURATE_MATCH) return ACCURATE_MATCH;
			level = newLevel; // want to answer the stronger match
		}
	}
	return level;
}
@Override
public int resolveLevel(Binding binding) {
	int level = IMPOSSIBLE_MATCH;
	for (int i = 0, length = this.patternLocators.length; i < length; i++) {
		int newLevel = this.patternLocators[i].resolveLevel(binding);
		if (newLevel > level) {
			if (newLevel == ACCURATE_MATCH) return ACCURATE_MATCH;
			level = newLevel; // want to answer the stronger match
		}
	}
	return level;
}

@Override
void setFlavors(int flavors) {
	for (int i = 0, length = this.patternLocators.length; i < length; i++) {
		this.patternLocators[i].setFlavors(flavors);
	}
}
@Override
public void recordResolution(QualifiedTypeReference typeReference, TypeBinding resolution) {
	for (int i = 0, length = this.patternLocators.length; i < length; i++) {
		this.patternLocators[i].recordResolution(typeReference, resolution);
	}
}
}
