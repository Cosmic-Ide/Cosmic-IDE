/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchDocument;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;
import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.lookup.*;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.jdt.internal.core.search.JavaSearchParticipant;

public class TypeDeclarationLocator extends PatternLocator {

protected TypeDeclarationPattern pattern; // can be a QualifiedTypeDeclarationPattern

public TypeDeclarationLocator(TypeDeclarationPattern pattern) {
	super(pattern);

	this.pattern = pattern;
}
//public int match(ASTNode node, MatchingNodeSet nodeSet) - SKIP IT
//public int match(ConstructorDeclaration node, MatchingNodeSet nodeSet) - SKIP IT
//public int match(Expression node, MatchingNodeSet nodeSet) - SKIP IT
//public int match(FieldDeclaration node, MatchingNodeSet nodeSet) - SKIP IT
//public int match(MethodDeclaration node, MatchingNodeSet nodeSet) - SKIP IT
//public int match(MessageSend node, MatchingNodeSet nodeSet) - SKIP IT
//public int match(Reference node, MatchingNodeSet nodeSet) - SKIP IT
@Override
public int match(TypeDeclaration node, MatchingNodeSet nodeSet) {
	if (this.pattern.simpleName == null || matchesName(this.pattern.simpleName, node.name))
		return nodeSet.addMatch(node, this.pattern.mustResolve ? POSSIBLE_MATCH : ACCURATE_MATCH);

	return IMPOSSIBLE_MATCH;
}
//public int match(TypeReference node, MatchingNodeSet nodeSet) - SKIP IT

@Override
public int resolveLevel(ASTNode node) {
	if (!(node instanceof TypeDeclaration)) return IMPOSSIBLE_MATCH;

	return resolveLevel(((TypeDeclaration) node).binding);
}
@Override
public int resolveLevel(Binding binding) {
	if (binding == null) return INACCURATE_MATCH;
	if (!(binding instanceof TypeBinding)) return IMPOSSIBLE_MATCH;

	TypeBinding type = (TypeBinding) binding;

	switch (this.pattern.typeSuffix) {
		case CLASS_SUFFIX:
			if (!type.isClass()) return IMPOSSIBLE_MATCH;
			break;
		case CLASS_AND_INTERFACE_SUFFIX:
			if (!(type.isClass() || (type.isInterface() && !type.isAnnotationType()))) return IMPOSSIBLE_MATCH;
			break;
		case CLASS_AND_ENUM_SUFFIX:
			if (!(type.isClass() || type.isEnum())) return IMPOSSIBLE_MATCH;
			break;
		case INTERFACE_SUFFIX:
			if (!type.isInterface() || type.isAnnotationType()) return IMPOSSIBLE_MATCH;
			break;
		case INTERFACE_AND_ANNOTATION_SUFFIX:
			if (!(type.isInterface() || type.isAnnotationType())) return IMPOSSIBLE_MATCH;
			break;
		case ENUM_SUFFIX:
			if (!type.isEnum()) return IMPOSSIBLE_MATCH;
			break;
		case ANNOTATION_TYPE_SUFFIX:
			if (!type.isAnnotationType()) return IMPOSSIBLE_MATCH;
			break;
		case TYPE_SUFFIX : // nothing
	}

	if (matchModule(this.pattern, type) == IMPOSSIBLE_MATCH) {
		return IMPOSSIBLE_MATCH;
	}
	// fully qualified name
	if (this.pattern instanceof QualifiedTypeDeclarationPattern) {
		QualifiedTypeDeclarationPattern qualifiedPattern = (QualifiedTypeDeclarationPattern) this.pattern;
		return resolveLevelForType(qualifiedPattern.simpleName, qualifiedPattern.qualification, type);
	} else {
		char[] enclosingTypeName = this.pattern.enclosingTypeNames == null ? null : CharOperation.concatWith(this.pattern.enclosingTypeNames, '.');
		return resolveLevelForType(this.pattern.simpleName, this.pattern.pkg, enclosingTypeName, type);
	}
}
/**
 * Returns whether the given type binding matches the given simple name pattern
 * qualification pattern and enclosing type name pattern.
 */
protected int resolveLevelForType(char[] simpleNamePattern, char[] qualificationPattern, char[] enclosingNamePattern, TypeBinding type) {
	if (enclosingNamePattern == null)
		return resolveLevelForType(simpleNamePattern, qualificationPattern, type);
	if (qualificationPattern == null)
		return resolveLevelForType(simpleNamePattern, enclosingNamePattern, type);

	// case of an import reference while searching for ALL_OCCURENCES of a type (see bug 37166)
	if (type instanceof ProblemReferenceBinding) return IMPOSSIBLE_MATCH;

	// pattern was created from a Java element: qualification is the package name.
	char[] fullQualificationPattern = CharOperation.concat(qualificationPattern, enclosingNamePattern, '.');
	if (CharOperation.equals(this.pattern.pkg, CharOperation.concatWith(type.getPackage().compoundName, '.')))
		return resolveLevelForType(simpleNamePattern, fullQualificationPattern, type);
	return IMPOSSIBLE_MATCH;
}
private HashSet<String> getModuleGraph(String mName, TypeDeclarationPattern typePattern, HashSet<String> mGraph) {
	mGraph.add(mName);
	SearchPattern modulePattern = SearchPattern.createPattern(mName,
			IJavaSearchConstants.MODULE, IJavaSearchConstants.DECLARATIONS, typePattern.getMatchRule());
	if (modulePattern == null) return mGraph;
	final HashSet<String> tmpGraph = new HashSet<>();
	final SearchParticipant participant = new JavaSearchParticipant() {
		@Override
		public void locateMatches(SearchDocument[] indexMatches, SearchPattern mPattern,
				IJavaSearchScope scope, SearchRequestor requestor, IProgressMonitor monitor) throws CoreException {
			MatchLocator matchLocator =	new MatchLocator(mPattern,	requestor,	scope,	monitor);
			/* eliminating false matches and locating them */
			if (monitor != null && monitor.isCanceled()) throw new OperationCanceledException();
			matchLocator.locateMatches(indexMatches);
			addRequiredModules(matchLocator);
		}
		private void addRequiredModules(MatchLocator matchLocator) {
			if (matchLocator.matchBinding == null) return;
			for (Binding b :matchLocator.matchBinding.values()) {
				if (b instanceof ModuleBinding &&  ((ModuleBinding) b).moduleName != null) {
					ModuleBinding m = (ModuleBinding) b;
					tmpGraph.add(new String(m.moduleName));
					for (ModuleBinding r : m.getAllRequiredModules()) {
						char[] name = r.moduleName;
						if (name == null || CharOperation.equals(name, CharOperation.NO_CHAR)) continue;
						tmpGraph.add(new String(name));
					}
				}
			}
		}
	};
	final SearchRequestor requestor = new SearchRequestor() {
		@Override
		public void acceptSearchMatch(SearchMatch searchMatch) throws CoreException {
			System.out.println(searchMatch.toString());
			// do nothing
		}
	};
	try {
		new SearchEngine().search(modulePattern, new SearchParticipant[] {participant},
				JavaModelManager.getJavaModelManager().getWorkspaceScope(),
				requestor,	null);
	} catch (CoreException e) {
		// do nothing
	}
	mGraph.addAll(tmpGraph);
	return mGraph;
}
private char[][] getModuleList(TypeDeclarationPattern typePattern) {
	if (!typePattern.moduleGraph)
		return typePattern.moduleNames;
	if (typePattern.moduleGraphElements != null) // already computed
		return typePattern.moduleGraphElements;
	typePattern.moduleGraphElements = CharOperation.NO_CHAR_CHAR; // signal processing done.
	// compute (lazy)
	List<String> moduleList = Arrays.asList(CharOperation.toStrings(typePattern.moduleNames));
	int sz = moduleList.size();
	HashSet<String> mGraph = new HashSet<>();
	for (int i = 0; i < sz; ++i) {
		mGraph = getModuleGraph(moduleList.get(i), typePattern, mGraph);
	}
	sz = mGraph.size();
	if (sz > 0) {
		String[] ar = mGraph.toArray(new String[0]);
		char[][] tmp = new char[sz][];
		for (int i = 0; i < sz; ++i) {
			tmp[i] = ar[i].toCharArray();
		}
		typePattern.moduleGraphElements = tmp;
	}
	return typePattern.moduleGraphElements;
}
private int matchModule(TypeDeclarationPattern typePattern, TypeBinding type) {
	if (!(type instanceof ReferenceBinding))
		return INACCURATE_MATCH; // a safety net, should not come here for error free code.
	ReferenceBinding reference = (ReferenceBinding) type;
	ModuleBinding module = reference.module();
	if (module == null || module.moduleName == null || typePattern.moduleNames == null)
		return POSSIBLE_MATCH; //can't determine, say possible to all.
	String bindModName = new String(module.moduleName);

	if (typePattern.modulePatterns == null) {// use 'normal' matching
		char[][] moduleList = getModuleList(typePattern);
		for (char[] m : moduleList) { // match any in the list
			int ret = matchNameValue(m, module.moduleName);
			if (ret != IMPOSSIBLE_MATCH) return ret;
		}
	} else {// use pattern matching
		for (Pattern p : typePattern.modulePatterns) {
			Matcher matcher = p.matcher(bindModName);
			if (matcher.matches()) return ACCURATE_MATCH;
		}
	}
	return IMPOSSIBLE_MATCH;
}
@Override
public String toString() {
	return "Locator for " + this.pattern.toString(); //$NON-NLS-1$
}
}
