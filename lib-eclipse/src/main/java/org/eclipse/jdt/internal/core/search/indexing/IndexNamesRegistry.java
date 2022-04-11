/*******************************************************************************
 * Copyright (c) 2017 Google, Inc and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Stefan Xenos (Google) - Initial implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.core.search.indexing;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.core.index.DiskIndex;
import org.eclipse.jdt.internal.core.search.processing.JobManager;
import org.eclipse.jdt.internal.core.util.Util;

public class IndexNamesRegistry {
	private final File savedIndexNamesFile;
	private final Job writeJob;
	private final IPath javaPluginWorkingLocation;

	private final Object queueMutex = new Object();
	/**
	 * Outstanding write that is waiting to be processed. Null if there are no outstanding writes. Synchronize on
	 * queueMutex before accessing.
	 */
	private char[][] pendingWrite;

	public IndexNamesRegistry(File savedIndexNamesFile, IPath javaPluginWorkingLocation) {
		super();
		this.savedIndexNamesFile = savedIndexNamesFile;
		this.writeJob = Job.create("Updating index names", this::save); //$NON-NLS-1$
		this.writeJob.setSystem(true);
		this.javaPluginWorkingLocation = javaPluginWorkingLocation;
	}

	public void write(char[][] newContents) {
		synchronized (this.queueMutex) {
			this.pendingWrite = newContents;
		}
		this.writeJob.schedule();
	}

	/**
	 * Returns the contents of the index names registry.
	 */
	public char[][] read(IProgressMonitor monitor) {
		// If there is currently a write in progress, return the contents that are about to be written to disk.
		char[][] newContents;
		synchronized (this.queueMutex) {
			newContents = this.pendingWrite;
		}

		if (newContents != null) {
			return newContents;
		}

		// Otherwise, read fresh contents from disk
		try {
			char[] savedIndexNames = org.eclipse.jdt.internal.compiler.util.Util
					.getFileCharContent(this.savedIndexNamesFile, null);
			if (savedIndexNames.length > 0) {
				char[][] names = CharOperation.splitOn('\n', savedIndexNames);
				if (names.length > 1) {
					// First line is DiskIndex signature + saved plugin working location (see writeSavedIndexNamesFile())
					String savedSignature = DiskIndex.SIGNATURE + "+" + this.javaPluginWorkingLocation.toOSString(); //$NON-NLS-1$
					if (savedSignature.equals(new String(names[0])))
						return names;
				}
			}
		} catch (IOException ignored) {
			if (JobManager.VERBOSE)
				Util.verbose("Failed to read saved index file names"); //$NON-NLS-1$
		}
		return null;
	}

	private void save(IProgressMonitor monitor) {
		SubMonitor subMonitor = SubMonitor.convert(monitor);
		char[][] newContents;
		synchronized (this.queueMutex) {
			newContents = this.pendingWrite;
		}

		if (newContents == null) {
			return;
		}

		subMonitor.setWorkRemaining(newContents.length);

		try (BufferedWriter writer = new BufferedWriter(new FileWriter(this.savedIndexNamesFile))) {
			writer.write(DiskIndex.SIGNATURE);
			writer.write('+');
			writer.write(this.javaPluginWorkingLocation.toOSString());
			writer.write('\n');
			for (char[] next : newContents) {
				subMonitor.split(1);
				writer.write(next);
				writer.write('\n');
			}
		} catch (IOException ignored) {
			if (JobManager.VERBOSE)
				Util.verbose("Failed to write saved index file names", System.err); //$NON-NLS-1$
		}

		synchronized (this.queueMutex) {
			if (this.pendingWrite == newContents) {
				this.pendingWrite = null;
			}
		}
	}

	public void delete() {
		synchronized (this.queueMutex) {
			this.pendingWrite = null;
		}
		this.writeJob.cancel();
		try {
			this.writeJob.join();
		} catch (InterruptedException e) {
			// Nothing to do
		}
		synchronized (this.queueMutex) {
			this.pendingWrite = null;
		}
		this.savedIndexNamesFile.delete();
	}
}
