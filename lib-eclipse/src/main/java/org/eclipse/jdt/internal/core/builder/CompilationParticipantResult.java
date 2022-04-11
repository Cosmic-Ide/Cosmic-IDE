/*******************************************************************************
 * Copyright (c) 2005, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    IBM - rewrote spec
 *
 *******************************************************************************/

package org.eclipse.jdt.internal.core.builder;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.compiler.*;

public class CompilationParticipantResult extends BuildContext {
	protected SourceFile sourceFile;
	protected boolean hasAnnotations; // only set during processAnnotations
	protected IFile[] addedFiles; // added/changed generated source files that need to be compiled
	protected IFile[] deletedFiles; // previously generated source files that should be deleted
	protected CategorizedProblem[] problems; // new problems to report against this compilationUnit
	protected String[] dependencies; // fully-qualified type names of any new dependencies, each name is of the form 'p1.p2.A.B'
	private boolean isTestCode;

protected CompilationParticipantResult(SourceFile sourceFile, boolean isTestCode) {
	this.sourceFile = sourceFile;
	this.isTestCode = isTestCode;
	this.hasAnnotations = false;
	this.addedFiles = null;
	this.deletedFiles = null;
	this.problems = null;
	this.dependencies = null;
}

/**
 * Returns the contents of the compilation unit.
 *
 * @return the contents of the compilation unit
 */
@Override
public char[] getContents() {
	return this.sourceFile.getContents();
}

/**
 * Returns the <code>IFile</code> representing the compilation unit.
 *
 * @return the <code>IFile</code> representing the compilation unit
 */
@Override
public IFile getFile() {
	return this.sourceFile.resource;
}

/**
 * Returns whether the compilation unit contained any annotations when it was compiled.
 *
 * NOTE: This is only valid during {@link CompilationParticipant#processAnnotations(BuildContext[])}.
 *
 * @return whether the compilation unit contained any annotations when it was compiled
 */
@Override
public boolean hasAnnotations() {
	return this.hasAnnotations; // only set during processAnnotations
}

/**
 * Record the added/changed generated files that need to be compiled.
 *
 * @param addedGeneratedFiles the added/changed files
 */
@Override
public void recordAddedGeneratedFiles(IFile[] addedGeneratedFiles) {
	int length2 = addedGeneratedFiles.length;
	if (length2 == 0) return;

	int length1 = this.addedFiles == null ? 0 : this.addedFiles.length;
	IFile[] merged = new IFile[length1 + length2];
	if (length1 > 0) // always make a copy even if currently empty
		System.arraycopy(this.addedFiles, 0, merged, 0, length1);
	System.arraycopy(addedGeneratedFiles, 0, merged, length1, length2);
	this.addedFiles = merged;
}

/**
 * Record the generated files that need to be deleted.
 *
 * @param deletedGeneratedFiles the files that need to be deleted
 */
@Override
public void recordDeletedGeneratedFiles(IFile[] deletedGeneratedFiles) {
	int length2 = deletedGeneratedFiles.length;
	if (length2 == 0) return;

	int length1 = this.deletedFiles == null ? 0 : this.deletedFiles.length;
	IFile[] merged = new IFile[length1 + length2];
	if (length1 > 0) // always make a copy even if currently empty
		System.arraycopy(this.deletedFiles, 0, merged, 0, length1);
	System.arraycopy(deletedGeneratedFiles, 0, merged, length1, length2);
	this.deletedFiles = merged;
}

/**
 * Record the fully-qualified type names of any new dependencies, each name is of the form "p1.p2.A.B".
 *
 * @param typeNameDependencies the fully-qualified type names of new dependencies
 */
@Override
public void recordDependencies(String[] typeNameDependencies) {
	int length2 = typeNameDependencies.length;
	if (length2 == 0) return;

	int length1 = this.dependencies == null ? 0 : this.dependencies.length;
	String[] merged = new String[length1 + length2];
	if (length1 > 0) // always make a copy even if currently empty
		System.arraycopy(this.dependencies, 0, merged, 0, length1);
	System.arraycopy(typeNameDependencies, 0, merged, length1, length2);
	this.dependencies = merged;
}

/**
 * Record new problems to report against this compilationUnit.
 * Markers are persisted for these problems only for the declared managed marker type
 * (see the 'compilationParticipant' extension point).
 *
 * @param newProblems the problems to report
 */
@Override
public void recordNewProblems(CategorizedProblem[] newProblems) {
	int length2 = newProblems.length;
	if (length2 == 0) return;

	int length1 = this.problems == null ? 0 : this.problems.length;
	CategorizedProblem[] merged = new CategorizedProblem[length1 + length2];
	if (length1 > 0) // always make a copy even if currently empty
		System.arraycopy(this.problems, 0, merged, 0, length1);
	System.arraycopy(newProblems, 0, merged, length1, length2);
	this.problems = merged;
}

void reset(boolean detectedAnnotations) {
	// called prior to processAnnotations
	this.hasAnnotations = detectedAnnotations;
	this.addedFiles = null;
	this.deletedFiles = null;
	this.problems = null;
	this.dependencies = null;
}

@Override
public String toString() {
	return this.sourceFile.toString();
}

@Override
public boolean isTestCode() {
	return this.isTestCode;
}
}
