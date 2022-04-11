package org.eclipse.jdt.internal.core;
import org.eclipse.core.runtime.IProgressMonitor;

/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
@SuppressWarnings("rawtypes")
public class BatchInitializationMonitor implements IProgressMonitor {

	public ThreadLocal initializeAfterLoadMonitor = new ThreadLocal();

	public String subTaskName = ""; //$NON-NLS-1$
	public int worked = 0;

	private IProgressMonitor getMonitor() {
		return (IProgressMonitor) this.initializeAfterLoadMonitor.get();
	}

	@Override
	public void beginTask(String name, int totalWork) {
		IProgressMonitor monitor = getMonitor();
		if (monitor != null)
			monitor.beginTask(name, totalWork);
	}

	@Override
	public void done() {
		IProgressMonitor monitor = getMonitor();
		if (monitor != null)
			monitor.done();
		this.worked = 0;
		this.subTaskName = ""; //$NON-NLS-1$
	}

	@Override
	public void internalWorked(double work) {
		IProgressMonitor monitor = getMonitor();
		if (monitor != null)
			monitor.internalWorked(work);
	}

	@Override
	public boolean isCanceled() {
		IProgressMonitor monitor = getMonitor();
		if (monitor != null)
			return monitor.isCanceled();
		return false;
	}

	@Override
	public void setCanceled(boolean value) {
		IProgressMonitor monitor = getMonitor();
		if (monitor != null)
			monitor.setCanceled(value);
	}

	@Override
	public void setTaskName(String name) {
		IProgressMonitor monitor = getMonitor();
		if (monitor != null)
			monitor.setTaskName(name);
	}

	@Override
	public void subTask(String name) {
		IProgressMonitor monitor = getMonitor();
		if (monitor != null)
			monitor.subTask(name);
		this.subTaskName = name;
	}

	@Override
	public void worked(int work) {
		IProgressMonitor monitor = getMonitor();
		if (monitor != null)
			monitor.worked(work);
		synchronized(this) {
			this.worked += work;
		}
	}

	public synchronized int getWorked() {
		int result = this.worked;
		this.worked = 0;
		return result;
	}
}
