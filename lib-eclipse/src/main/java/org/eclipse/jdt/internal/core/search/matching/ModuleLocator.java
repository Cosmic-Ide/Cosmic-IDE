package org.eclipse.jdt.internal.core.search.matching;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.ModuleDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ModuleReference;
import org.eclipse.jdt.internal.compiler.lookup.Binding;
import org.eclipse.jdt.internal.compiler.lookup.ModuleBinding;

/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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
 *
 *******************************************************************************/
public class ModuleLocator extends PatternLocator {

	private ModulePattern pattern;
	/* package */ boolean target = false;

	public ModuleLocator(ModulePattern pattern) {
		super(pattern);
		this.pattern = pattern;
	}
	@Override
	public int match(ModuleDeclaration node, MatchingNodeSet nodeSet) {
		if (!this.pattern.findDeclarations) return IMPOSSIBLE_MATCH;
		if (!matchesName(this.pattern.name, node.moduleName)) return IMPOSSIBLE_MATCH;
		nodeSet.mustResolve = true;
		return nodeSet.addMatch(node, POSSIBLE_MATCH);
	}
	@Override
	protected int match(ModuleReference node, MatchingNodeSet nodeSet) {
		if (!this.pattern.findReferences) return IMPOSSIBLE_MATCH;
		if (!matchesName(this.pattern.name, node.moduleName)) return IMPOSSIBLE_MATCH;
		if (this.target) {
			return nodeSet.addMatch(node, ACCURATE_MATCH);
		}
		nodeSet.mustResolve = true;
		return nodeSet.addMatch(node, POSSIBLE_MATCH);
	}
	@Override
	protected int matchContainer() {
		return COMPILATION_UNIT_CONTAINER;
	}
	@Override
	public int resolveLevel(ASTNode possibleMatchingNode) {
		if (this.pattern.findDeclarations && possibleMatchingNode instanceof ModuleDeclaration) {
			return resolveLevel(((ModuleDeclaration) possibleMatchingNode).binding);
		}
		if (this.pattern.findReferences && possibleMatchingNode instanceof ModuleReference) {
			return resolveLevel(((ModuleReference) possibleMatchingNode).resolve(null));
		}
		return IMPOSSIBLE_MATCH;
	}
	@Override
	protected void matchReportReference(ASTNode reference, IJavaElement element, Binding elementBinding, int accuracy, MatchLocator locator) throws CoreException {
		super.matchReportReference(reference, element, elementBinding, accuracy, locator);
	}
	@Override
	protected void matchReportReference(ASTNode reference, IJavaElement element, IJavaElement localElement, IJavaElement[] otherElements, Binding elementBinding, int accuracy, MatchLocator locator) throws CoreException {
		matchReportReference(reference, element, elementBinding, accuracy, locator);
	}

	@Override
	public SearchMatch newDeclarationMatch(ASTNode node, IJavaElement element, Binding elementBinding, int accuracy, int length, MatchLocator locator) {
		return super.newDeclarationMatch(node, element, elementBinding, accuracy, length, locator);
	}
	@Override
	protected int referenceType() {
		return IJavaElement.JAVA_MODULE;
	}
	@Override
	public int resolveLevel(Binding binding) {
		if (binding == null) return INACCURATE_MATCH;
		if (!(binding instanceof ModuleBinding)) return IMPOSSIBLE_MATCH;
		return (matchesName(this.pattern.name, binding.readableName())) ? ACCURATE_MATCH : IMPOSSIBLE_MATCH;
	}
}
