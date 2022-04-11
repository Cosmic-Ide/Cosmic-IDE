/*******************************************************************************
 * Copyright (c) 2006, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    IBM Corporation - initial API and implementation
 *
 *******************************************************************************/

package org.eclipse.jdt.core.compiler;

import org.eclipse.core.resources.IFile;

/**
 * The context of a build event that is notified to interested compilation
 * participants when {@link CompilationParticipant#buildStarting(BuildContext[], boolean) a build is starting},
 * or to annotations processors when {@link CompilationParticipant#processAnnotations(BuildContext[]) a source file has annotations}.
 *
 * @since 3.2
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @noextend This class is not intended to be subclassed by clients.
 */
public class BuildContext {

/**
 * Returns the contents of the compilation unit.
 *
 * @return the contents of the compilation unit
 */
public char[] getContents() {
	return null; // default overridden by concrete implementation
}

/**
 * Returns the <code>IFile</code> representing the compilation unit.
 *
 * @return the <code>IFile</code> representing the compilation unit
 */
public IFile getFile() {
	return null; // default overridden by concrete implementation
}

/**
 * Returns whether the compilation unit contained any annotations when it was compiled.
 *
 * NOTE: This is only valid during {@link CompilationParticipant#processAnnotations(BuildContext[])}.
 *
 * @return whether the compilation unit contained any annotations when it was compiled
 */
public boolean hasAnnotations() {
	return false; // default overridden by concrete implementation
}

/**
 * Record the added/changed generated files that need to be compiled.
 *
 * @param addedGeneratedFiles the added/changed files
 */
public void recordAddedGeneratedFiles(IFile[] addedGeneratedFiles) {
	// default overridden by concrete implementation
}

/**
 * Record the generated files that need to be deleted.
 *
 * @param deletedGeneratedFiles the files that need to be deleted
 */
public void recordDeletedGeneratedFiles(IFile[] deletedGeneratedFiles) {
	// default overridden by concrete implementation
}

/**
 * Record the fully-qualified type names of any new dependencies, each name is of the form "p1.p2.A.B".
 *
 * @param typeNameDependencies the fully-qualified type names of new dependencies
 */
public void recordDependencies(String[] typeNameDependencies) {
	// default overridden by concrete implementation
}

/**
 * Record new problems to report against this compilationUnit.
 * Markers are persisted for these problems only for the declared managed marker type
 * (see the 'compilationParticipant' extension point).
 *
 * @param newProblems the problems to report
 */
public void recordNewProblems(CategorizedProblem[] newProblems) {
	// default overridden by concrete implementation
}

/**
 * @since 3.14
 */
public boolean isTestCode() {
	return false; // default overridden by concrete implementation
}
}
