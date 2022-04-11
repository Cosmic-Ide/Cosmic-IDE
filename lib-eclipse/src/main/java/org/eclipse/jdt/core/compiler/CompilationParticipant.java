/*******************************************************************************
 * Copyright (c) 2005, 2019 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    mkaufman@bea.com - initial API as ICompilationParticipant
 *    IBM - changed from interface ICompilationParticipant to abstract class CompilationParticipant
 *    IBM - rewrote specification
 *
 *******************************************************************************/

package org.eclipse.jdt.core.compiler;

import org.eclipse.jdt.core.IJavaProject;

/**
 * A compilation participant is notified of events occurring during the compilation process.
 * The compilation process not only involves generating .class files (i.e. building), it also involves
 * cleaning the output directory, reconciling a working copy, etc.
 * So the notified events are the result of a build action, a clean action, a reconcile operation
 * (for a working copy), etc.
 * <p>
 * Code that participates in the build should in general be implemented with a separate Builder,
 * rather than a CompilationParticipant. It is only necessary to use a CompilationParticipant if
 * the build step needs to interact with the Java build, for instance by creating additional
 * Java source files that must themselves in turn be compiled.
 * <p>
 * Clients wishing to participate in the compilation process must subclass this class, and implement
 * {@link #isActive(IJavaProject)}, {@link #aboutToBuild(IJavaProject)},
 * {@link #reconcile(ReconcileContext)}, etc.
* </p><p>
 * This class is intended to be subclassed by clients.
 * </p>
 * @since 3.2
 */
public abstract class CompilationParticipant {

	public final static int READY_FOR_BUILD = 1;
	public final static int NEEDS_FULL_BUILD = 2;

/**
 * Notifies this participant that a build is about to start and provides it the opportunity to
 * create missing source folders for generated source files. Additional source folders
 * should be marked as optional so the project can be built when the folders do not exist.
 * Only sent to participants interested in the project.
 * <p>
 * Default is to return <code>READY_FOR_BUILD</code>.
 * </p>
 * @see #buildFinished(IJavaProject project)
 * @param project the project about to build
 * @return READY_FOR_BUILD or NEEDS_FULL_BUILD
 */
public int aboutToBuild(IJavaProject project) {
	return READY_FOR_BUILD;
}

/**
 * Notifies this participant that a build has finished for the project.
 * This will be sent, even if buildStarting() was not sent when no source files needed to be compiled
 * or the build failed.
 * Only sent to participants interested in the project.
 * @param project the project about to build
 * @since 3.4
  */
public void buildFinished(IJavaProject project) {
	// do nothing by default
}

/**
 * Notifies this participant that a compile operation is about to start and provides it the opportunity to
 * generate source files based on the source files about to be compiled.
 * When isBatchBuild is true, then files contains all source files in the project.
 * Only sent to participants interested in the current build project.
 *
 * @param files is an array of BuildContext
 * @param isBatch identifies when the build is a batch build
  */
public void buildStarting(BuildContext[] files, boolean isBatch) {
	// do nothing by default
}

/**
 * Notifies this participant that a clean is about to start and provides it the opportunity to
 * delete generated source files.
 * Only sent to participants interested in the project.
 * @param project the project about to be cleaned
 */
public void cleanStarting(IJavaProject project) {
	// do nothing by default
}

/**
 * Returns whether this participant is active for a given project.
 * <p>
 * Default is to return <code>false</code>.
 * </p><p>
 * For efficiency, participants that are not interested in the
 * given project should return <code>false</code> for that project.
 * </p><p>
 * Note: In {@link org.eclipse.jdt.core.WorkingCopyOwner#newWorkingCopy(String, org.eclipse.jdt.core.IClasspathEntry[], org.eclipse.core.runtime.IProgressMonitor)
 * special cases}, the project may be closed and not exist. Participants typically return false when the
 * underlying project is closed. I.e. when the following check returns false:
 *  <pre>
 * 	javaProject.getProject().isOpen();
 * </pre>
 * @param project the project to participate in
 * @return whether this participant is active for a given project
 */
public boolean isActive(IJavaProject project) {
	return false;
}

/**
 * Returns whether this participant is interested in Annotations.
 * <p>
 * Returning <code>true</code> enables the callback {@link #processAnnotations(BuildContext[])}, where this
 * participant can influence build results.
 * </p>
 * <p>
 * Default is to return <code>false</code>.
 * </p>
 *
 * @return whether this participant is interested in Annotations
 */
public boolean isAnnotationProcessor() {
	return false;
}

/**
 * Notifies this participant that a compile operation has found source files using Annotations.
 * Only sent to participants interested in the current build project that answer true to {@link #isAnnotationProcessor()}.
 * Each BuildContext was informed whether its source file currently hasAnnotations().
 *
 * @param files is an array of BuildContext
  */
public void processAnnotations(BuildContext[] files) {
	// do nothing by default
}

/**
 * Notifies this participant that a reconcile operation is happening. The participant can act on this reconcile
 * operation by using the given context. Other participant can then see the result of this participation
 * on this context.
 * <p>
 * Note that a participant should not modify the buffer of the working copy that is being reconciled.
 * </p><p>
 * Default is to do nothing.
 * </p>
 * @param context the reconcile context to act on
  */
public void reconcile(ReconcileContext context) {
	// do nothing by default
}

}
