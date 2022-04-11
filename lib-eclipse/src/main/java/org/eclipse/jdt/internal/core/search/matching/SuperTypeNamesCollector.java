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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.search.*;
import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.ast.*;
import org.eclipse.jdt.internal.compiler.ast.Initializer;
import org.eclipse.jdt.internal.compiler.env.AccessRuleSet;
import org.eclipse.jdt.internal.compiler.lookup.*;
import org.eclipse.jdt.internal.compiler.problem.AbortCompilation;
import org.eclipse.jdt.internal.core.*;
import org.eclipse.jdt.internal.core.search.*;
import org.eclipse.jdt.internal.core.search.indexing.IIndexConstants;
import org.eclipse.jdt.internal.core.search.indexing.IndexManager;
import org.eclipse.jdt.internal.core.util.ASTNodeFinder;
import org.eclipse.jdt.internal.core.util.Util;

/**
 * Collects the super type names of a given declaring type.
 * Returns NOT_FOUND_DECLARING_TYPE if the declaring type was not found.
 * Returns null if the declaring type pattern doesn't require an exact match.
 */
public class SuperTypeNamesCollector {

	/**
	 * An ast visitor that visits type declarations and member type declarations
	 * collecting their super type names.
	 */
	public class TypeDeclarationVisitor extends ASTVisitor {
		@Override
		public boolean visit(TypeDeclaration typeDeclaration, BlockScope scope) {
			ReferenceBinding binding = typeDeclaration.binding;
			if (SuperTypeNamesCollector.this.matches(binding))
				collectSuperTypeNames(binding, binding.compoundName);
			return true;
		}
		@Override
		public boolean visit(TypeDeclaration typeDeclaration, CompilationUnitScope scope) {
			ReferenceBinding binding = typeDeclaration.binding;
			if (SuperTypeNamesCollector.this.matches(binding))
				collectSuperTypeNames(binding, binding.compoundName);
			return true;
		}
		@Override
		public boolean visit(TypeDeclaration memberTypeDeclaration, ClassScope scope) {
			ReferenceBinding binding = memberTypeDeclaration.binding;
			if (SuperTypeNamesCollector.this.matches(binding))
				collectSuperTypeNames(binding, binding.compoundName);
			return true;
		}
		@Override
		public boolean visit(FieldDeclaration fieldDeclaration, MethodScope scope) {
			return false; // don't visit field declarations
		}
		@Override
		public boolean visit(Initializer initializer, MethodScope scope) {
			return false; // don't visit initializers
		}
		@Override
		public boolean visit(ConstructorDeclaration constructorDeclaration, ClassScope scope) {
			return false; // don't visit constructor declarations
		}
		@Override
		public boolean visit(MethodDeclaration methodDeclaration, ClassScope scope) {
			return false; // don't visit method declarations
		}
	}
SearchPattern pattern;
char[] typeSimpleName;
char[] typeQualification;
MatchLocator locator;
IType type;
IProgressMonitor progressMonitor;
char[][][] result;
int resultIndex;

char[][][] samePackageSuperTypeName; // set only if focus is null
int samePackageIndex;

public SuperTypeNamesCollector(
	SearchPattern pattern,
	char[] typeSimpleName,
	char[] typeQualification,
	MatchLocator locator,
	IType type,
	IProgressMonitor progressMonitor) {

	this.pattern = pattern;
	this.typeSimpleName = typeSimpleName;
	this.typeQualification = typeQualification;
	this.locator = locator;
	this.type = type;
	this.progressMonitor = progressMonitor;
}

private boolean addIfSamePackage(char[][] compoundName, char[][] path) {
	if (compoundName.length != path.length) return false;
	int resultLength = this.samePackageSuperTypeName.length;
	for (int i = 0; i < resultLength; i++)
		if (CharOperation.equals(this.samePackageSuperTypeName[i], compoundName)) return false; // already known

	for (int i = 0, length = compoundName.length - 1; i < length; i ++) {
		if (!CharOperation.equals(compoundName[i], path[i])) return false;
	}
	if (resultLength == this.samePackageIndex)
		System.arraycopy(this.samePackageSuperTypeName, 0, this.samePackageSuperTypeName = new char[resultLength*2][][], 0, resultLength);
	this.samePackageSuperTypeName[this.samePackageIndex++] = compoundName;
	return true;
}

protected void addToResult(char[][] compoundName) {
	int resultLength = this.result.length;
	for (int i = 0; i < resultLength; i++)
		if (CharOperation.equals(this.result[i], compoundName)) return; // already known

	if (resultLength == this.resultIndex)
		System.arraycopy(this.result, 0, this.result = new char[resultLength*2][][], 0, resultLength);
	this.result[this.resultIndex++] = compoundName;
}

/*
 * Parse the given compilation unit and build its type bindings.
 */
protected CompilationUnitDeclaration buildBindings(ICompilationUnit compilationUnit, boolean isTopLevelOrMember) throws JavaModelException {
	// source unit
	org.eclipse.jdt.internal.compiler.env.ICompilationUnit sourceUnit = (org.eclipse.jdt.internal.compiler.env.ICompilationUnit) compilationUnit;

	CompilationResult compilationResult = new CompilationResult(sourceUnit, 1, 1, 0);
	CompilationUnitDeclaration unit =
		isTopLevelOrMember ?
			this.locator.basicParser().dietParse(sourceUnit, compilationResult) :
			this.locator.basicParser().parse(sourceUnit, compilationResult);
	if (unit != null) {
		this.locator.lookupEnvironment.buildTypeBindings(unit, null /*no access restriction*/);
		this.locator.lookupEnvironment.completeTypeBindings(unit, !isTopLevelOrMember);
		if (!isTopLevelOrMember) {
			if (unit.scope != null)
				unit.scope.faultInTypes(); // fault in fields & methods
			unit.resolve();
		}
	}
	return unit;
}
public char[][][] collect() throws JavaModelException {
	if (this.type != null) {
		// Collect the paths of the cus that are in the hierarchy of the given type
		this.result = new char[1][][];
		this.resultIndex = 0;
		JavaProject javaProject = (JavaProject) this.type.getJavaProject();
		this.locator.initialize(javaProject, 0);
		try {
			if (this.type.isBinary()) {
				BinaryTypeBinding binding = this.locator.cacheBinaryType(this.type, null);
				if (binding != null)
					collectSuperTypeNames(binding, null);
			} else {
				ICompilationUnit unit = this.type.getCompilationUnit();
				SourceType sourceType = (SourceType) this.type;
				boolean isTopLevelOrMember = sourceType.getOuterMostLocalContext() == null;
				CompilationUnitDeclaration parsedUnit = buildBindings(unit, isTopLevelOrMember);
				if (parsedUnit != null) {
					TypeDeclaration typeDecl = new ASTNodeFinder(parsedUnit).findType(this.type);
					if (typeDecl != null && typeDecl.binding != null)
						collectSuperTypeNames(typeDecl.binding, null);
				}
			}
		} catch (AbortCompilation e) {
			// problem with classpath: report inaccurate matches
			return null;
		}
		if (this.result.length > this.resultIndex)
			System.arraycopy(this.result, 0, this.result = new char[this.resultIndex][][], 0, this.resultIndex);
		return this.result;
	}

	// Collect the paths of the cus that declare a type which matches declaringQualification + declaringSimpleName
	String[] paths = getPathsOfDeclaringType();
	if (paths == null) return null;

	// Create bindings from source types and binary types and collect super type names of the type declaration
	// that match the given declaring type
	Util.sort(paths); // sort by projects
	JavaProject previousProject = null;
	this.result = new char[1][][];
	this.samePackageSuperTypeName = new char[1][][];
	this.resultIndex = 0;
	for (int i = 0, length = paths.length; i < length; i++) {
		try {
			Openable openable = this.locator.handleFactory.createOpenable(paths[i], this.locator.scope);
			if (openable == null) continue; // outside classpath

			IJavaProject project = openable.getJavaProject();
			if (!project.equals(previousProject)) {
				previousProject = (JavaProject) project;
				this.locator.initialize(previousProject, 0);
			}
			if (openable instanceof ICompilationUnit) {
				ICompilationUnit unit = (ICompilationUnit) openable;
				CompilationUnitDeclaration parsedUnit = buildBindings(unit, true /*only top level and member types are visible to the focus type*/);
				if (parsedUnit != null)
					parsedUnit.traverse(new TypeDeclarationVisitor(), parsedUnit.scope);
			} else if (openable instanceof IOrdinaryClassFile) {
				IOrdinaryClassFile classFile = (IOrdinaryClassFile) openable;
				BinaryTypeBinding binding = this.locator.cacheBinaryType(classFile.getType(), null);
				if (matches(binding))
					collectSuperTypeNames(binding, binding.compoundName);
			}
		} catch (AbortCompilation | JavaModelException e) {
			// ignore: continue with next element
		}
	}
	if (this.result.length > this.resultIndex)
		System.arraycopy(this.result, 0, this.result = new char[this.resultIndex][][], 0, this.resultIndex);
	return this.result;
}
/**
 * Collects the names of all the supertypes of the given type.
 */
protected void collectSuperTypeNames(ReferenceBinding binding, char[][] path) {
	ReferenceBinding superclass = binding.superclass();
	if (path != null && superclass != null) {
		boolean samePackage = addIfSamePackage(superclass.compoundName, path);
		if (!samePackage) path = null;
	}
	if (superclass != null) {
		addToResult(superclass.compoundName);
		collectSuperTypeNames(superclass, path);
	}

	ReferenceBinding[] interfaces = binding.superInterfaces();
	if (interfaces != null) {
		for (int i = 0; i < interfaces.length; i++) {
			ReferenceBinding interfaceBinding = interfaces[i];
			addToResult(interfaceBinding.compoundName);
			collectSuperTypeNames(interfaceBinding, path);
		}
	}
}
protected String[] getPathsOfDeclaringType() {
	if (this.typeQualification == null && this.typeSimpleName == null) return null;

	final PathCollector pathCollector = new PathCollector();
	IJavaSearchScope scope = SearchEngine.createWorkspaceScope();
	IndexManager indexManager = JavaModelManager.getIndexManager();
	SearchPattern searchPattern = new TypeDeclarationPattern(
		this.typeSimpleName != null ? null : this.typeQualification, // use the qualification only if no simple name
		null, // do find member types
		this.typeSimpleName,
		IIndexConstants.TYPE_SUFFIX,
		this.pattern.getMatchRule());
	IndexQueryRequestor searchRequestor = new IndexQueryRequestor(){
		@Override
		public boolean acceptIndexMatch(String documentPath, SearchPattern indexRecord, SearchParticipant participant, AccessRuleSet access) {
			TypeDeclarationPattern record = (TypeDeclarationPattern)indexRecord;
			if (record.enclosingTypeNames != IIndexConstants.ONE_ZERO_CHAR) {  // filter out local and anonymous classes
				pathCollector.acceptIndexMatch(documentPath, indexRecord, participant, access);
			}
			return true;
		}
	};
	SubMonitor subMonitor = SubMonitor.convert(this.progressMonitor, 100);
	indexManager.performConcurrentJob(
		new PatternSearchJob(
			searchPattern,
			new JavaSearchParticipant(),
			scope,
			searchRequestor),
		IJavaSearchConstants.WAIT_UNTIL_READY_TO_SEARCH, subMonitor.split(100));
	return pathCollector.getPaths();
}
public char[][][] getSamePackageSuperTypeNames() {
	return this.samePackageSuperTypeName;
}
protected boolean matches(char[][] compoundName) {
	int length = compoundName.length;
	if (length == 0) return false;
	char[] simpleName = compoundName[length-1];
	int last = length - 1;
	if (this.typeSimpleName == null || this.pattern.matchesName(simpleName, this.typeSimpleName)) {
		// most frequent case: simple name equals last segment of compoundName
		char[][] qualification = new char[last][];
		System.arraycopy(compoundName, 0, qualification, 0, last);
		return this.pattern.matchesName(this.typeQualification, CharOperation.concatWith(qualification, '.'));
	}

	if (!CharOperation.endsWith(simpleName, this.typeSimpleName)) return false;

	// member type -> transform A.B.C$D into A.B.C.D
	System.arraycopy(compoundName, 0, compoundName = new char[length+1][], 0, last);
	int dollar = CharOperation.indexOf('$', simpleName);
	if (dollar == -1) return false;
	compoundName[last] = CharOperation.subarray(simpleName, 0, dollar);
	compoundName[length] = CharOperation.subarray(simpleName, dollar+1, simpleName.length);
	return this.matches(compoundName);
}
protected boolean matches(ReferenceBinding binding) {
	return binding != null && binding.compoundName != null && this.matches(binding.compoundName);
}
}
