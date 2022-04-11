/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
package org.eclipse.jdt.internal.core.builder;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;

import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.internal.compiler.problem.AbortCompilation;
import org.eclipse.jdt.internal.core.util.Messages;

public class BuildNotifier {

protected IProgressMonitor monitor;
protected boolean cancelling;
protected float percentComplete;
protected float progressPerCompilationUnit;
protected int newErrorCount;
protected int fixedErrorCount;
protected int newWarningCount;
protected int fixedWarningCount;
protected int workDone;
protected int totalWork;
protected String previousSubtask;

public static int NewErrorCount = 0;
public static int FixedErrorCount = 0;
public static int NewWarningCount = 0;
public static int FixedWarningCount = 0;

public static void resetProblemCounters() {
	NewErrorCount = 0;
	FixedErrorCount = 0;
	NewWarningCount = 0;
	FixedWarningCount = 0;
}

public BuildNotifier(IProgressMonitor monitor, IProject project) {
	this.monitor = monitor;
	this.cancelling = false;
	this.newErrorCount = NewErrorCount;
	this.fixedErrorCount = FixedErrorCount;
	this.newWarningCount = NewWarningCount;
	this.fixedWarningCount = FixedWarningCount;
	this.workDone = 0;
	this.totalWork = 1000000;
}

/**
 * Notification before a compile that a unit is about to be compiled.
 */
public void aboutToCompile(SourceFile unit) {
	String message = Messages.bind(Messages.build_compiling, unit.resource.getFullPath().removeLastSegments(1).makeRelative().toString());
	subTask(message);
}

public void begin() {
	if (this.monitor != null)
		this.monitor.beginTask("", this.totalWork); //$NON-NLS-1$
	this.previousSubtask = null;
}

/**
 * Check whether the build has been canceled.
 */
public void checkCancel() {
	if (this.monitor != null && this.monitor.isCanceled())
		throw new OperationCanceledException();
}

/**
 * Check whether the build has been canceled.
 * Must use this call instead of checkCancel() when within the compiler.
 */
public void checkCancelWithinCompiler() {
	if (this.monitor != null && this.monitor.isCanceled() && !this.cancelling) {
		// Once the compiler has been canceled, don't check again.
		setCancelling(true);
		// Only AbortCompilation can stop the compiler cleanly.
		// We check cancelation again following the call to compile.
		throw new AbortCompilation(true, null);
	}
}

/**
 * Notification while within a compile that a unit has finished being compiled.
 */
public void compiled(SourceFile unit) {
	String message = Messages.bind(Messages.build_compiling, unit.resource.getFullPath().removeLastSegments(1).makeRelative().toString());
	subTask(message);
	updateProgressDelta(this.progressPerCompilationUnit);
	checkCancelWithinCompiler();
}

public void done() {
	NewErrorCount = this.newErrorCount;
	FixedErrorCount = this.fixedErrorCount;
	NewWarningCount = this.newWarningCount;
	FixedWarningCount = this.fixedWarningCount;

	updateProgress(1.0f);
	subTask(Messages.build_done);
	if (this.monitor != null)
		this.monitor.done();
	this.previousSubtask = null;
}

/**
 * Returns a string describing the problems.
 */
protected String problemsMessage() {
	int numNew = this.newErrorCount + this.newWarningCount;
	int numFixed = this.fixedErrorCount + this.fixedWarningCount;
	if (numNew == 0 && numFixed == 0) return ""; //$NON-NLS-1$

	boolean displayBoth = numNew > 0 && numFixed > 0;
	StringBuilder buffer = new StringBuilder();
	buffer.append('(');
	if (numNew > 0) {
		// (Found x errors + y warnings)
		buffer.append(Messages.build_foundHeader);
		buffer.append(' ');
		if (displayBoth || this.newErrorCount > 0) {
			if (this.newErrorCount == 1)
				buffer.append(Messages.build_oneError);
			else
				buffer.append(Messages.bind(Messages.build_multipleErrors, String.valueOf(this.newErrorCount)));
			if (displayBoth || this.newWarningCount > 0)
				buffer.append(" + "); //$NON-NLS-1$
		}
		if (displayBoth || this.newWarningCount > 0) {
			if (this.newWarningCount == 1)
				buffer.append(Messages.build_oneWarning);
			else
				buffer.append(Messages.bind(Messages.build_multipleWarnings, String.valueOf(this.newWarningCount)));
		}
		if (numFixed > 0)
			buffer.append(", "); //$NON-NLS-1$
	}
	if (numFixed > 0) {
		// (Fixed x errors + y warnings) or (Found x errors + y warnings, Fixed x + y)
		buffer.append(Messages.build_fixedHeader);
		buffer.append(' ');
		if (displayBoth) {
			buffer.append(String.valueOf(this.fixedErrorCount));
			buffer.append(" + "); //$NON-NLS-1$
			buffer.append(String.valueOf(this.fixedWarningCount));
		} else {
			if (this.fixedErrorCount > 0) {
				if (this.fixedErrorCount == 1)
					buffer.append(Messages.build_oneError);
				else
					buffer.append(Messages.bind(Messages.build_multipleErrors, String.valueOf(this.fixedErrorCount)));
				if (this.fixedWarningCount > 0)
					buffer.append(" + "); //$NON-NLS-1$
			}
			if (this.fixedWarningCount > 0) {
				if (this.fixedWarningCount == 1)
					buffer.append(Messages.build_oneWarning);
				else
					buffer.append(Messages.bind(Messages.build_multipleWarnings, String.valueOf(this.fixedWarningCount)));
			}
		}
	}
	buffer.append(')');
	return buffer.toString();
}

/**
 * Sets the cancelling flag, which indicates we are in the middle
 * of being cancelled.  Certain places (those callable indirectly from the compiler)
 * should not check cancel again while this is true, to avoid OperationCanceledException
 * being thrown at an inopportune time.
 */
public void setCancelling(boolean cancelling) {
	this.cancelling = cancelling;
}

/**
 * Sets the amount of progress to report for compiling each compilation unit.
 */
public void setProgressPerCompilationUnit(float progress) {
	this.progressPerCompilationUnit = progress;
}

public void subTask(String message) {
	String pm = problemsMessage();
	String msg = pm.length() == 0 ? message : pm + " " + message; //$NON-NLS-1$

	if (msg.equals(this.previousSubtask)) return; // avoid refreshing with same one
	//if (JavaBuilder.DEBUG) System.out.println(msg);
	if (this.monitor != null)
		this.monitor.subTask(msg);

	this.previousSubtask = msg;
}

protected void updateProblemCounts(CategorizedProblem[] newProblems) {
	for (int i = 0, l = newProblems.length; i < l; i++)
		if (newProblems[i].isError()) this.newErrorCount++; else this.newWarningCount++;
}

/**
 * Update the problem counts from one compilation result given the old and new problems,
 * either of which may be null.
 */
protected void updateProblemCounts(IMarker[] oldProblems, CategorizedProblem[] newProblems) {
	if (newProblems != null) {
		next : for (int i = 0, l = newProblems.length; i < l; i++) {
			CategorizedProblem newProblem = newProblems[i];
			if (newProblem.getID() == IProblem.Task) continue; // skip task
			boolean isError = newProblem.isError();
			String message = newProblem.getMessage();

			if (oldProblems != null) {
				for (int j = 0, m = oldProblems.length; j < m; j++) {
					IMarker pb = oldProblems[j];
					if (pb == null) continue; // already matched up with a new problem
					boolean wasError = IMarker.SEVERITY_ERROR
						== pb.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
					if (isError == wasError && message.equals(pb.getAttribute(IMarker.MESSAGE, ""))) { //$NON-NLS-1$
						oldProblems[j] = null;
						continue next;
					}
				}
			}
			if (isError) this.newErrorCount++; else this.newWarningCount++;
		}
	}
	if (oldProblems != null) {
		next : for (int i = 0, l = oldProblems.length; i < l; i++) {
			IMarker oldProblem = oldProblems[i];
			if (oldProblem == null) continue next; // already matched up with a new problem
			boolean wasError = IMarker.SEVERITY_ERROR
				== oldProblem.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
			String message = oldProblem.getAttribute(IMarker.MESSAGE, ""); //$NON-NLS-1$

			if (newProblems != null) {
				for (int j = 0, m = newProblems.length; j < m; j++) {
					CategorizedProblem pb = newProblems[j];
					if (pb.getID() == IProblem.Task) continue; // skip task
					if (wasError == pb.isError() && message.equals(pb.getMessage()))
						continue next;
				}
			}
			if (wasError) this.fixedErrorCount++; else this.fixedWarningCount++;
		}
	}
}

public void updateProgress(float newPercentComplete) {
	if (newPercentComplete > this.percentComplete) {
		this.percentComplete = Math.min(newPercentComplete, 1.0f);
		int work = Math.round(this.percentComplete * this.totalWork);
		if (work > this.workDone) {
			if (this.monitor != null)
				this.monitor.worked(work - this.workDone);
			//if (JavaBuilder.DEBUG)
				//System.out.println(java.text.NumberFormat.getPercentInstance().format(this.percentComplete));
			this.workDone = work;
		}
	}
}

public void updateProgressDelta(float percentWorked) {
	updateProgress(this.percentComplete + percentWorked);
}
}
