/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
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
package org.eclipse.jdt.internal.core.search.indexing;

import java.io.IOException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.internal.core.index.Index;
import org.eclipse.jdt.internal.core.search.processing.JobManager;
import org.eclipse.jdt.internal.core.util.Util;

class RemoveFolderFromIndex extends IndexRequest {
	IPath folderPath;
	char[][] inclusionPatterns;
	char[][] exclusionPatterns;

	public RemoveFolderFromIndex(IPath folderPath, char[][] inclusionPatterns, char[][] exclusionPatterns, IProject project, IndexManager manager) {
		super(project.getFullPath(), manager);
		this.folderPath = folderPath;
		this.inclusionPatterns = inclusionPatterns;
		this.exclusionPatterns = exclusionPatterns;
	}
	@Override
	public boolean execute(IProgressMonitor progressMonitor) {

		if (this.isCancelled || progressMonitor != null && progressMonitor.isCanceled()) return true;

		/* ensure no concurrent write access to index */
		Index index = this.manager.getIndex(this.containerPath, true, /*reuse index file*/ false /*create if none*/);
		if (index == null) return true;
		ReadWriteMonitor monitor = index.monitor;
		if (monitor == null) return true; // index got deleted since acquired

		try {
			monitor.enterRead(); // ask permission to read
			String containerRelativePath = Util.relativePath(this.folderPath, this.containerPath.segmentCount());
			String[] paths = index.queryDocumentNames(containerRelativePath);
			// all file names belonging to the folder or its subfolders and that are not excluded (see http://bugs.eclipse.org/bugs/show_bug.cgi?id=32607)
			if (paths != null) {
				if (this.exclusionPatterns == null && this.inclusionPatterns == null) {
					for (int i = 0, max = paths.length; i < max; i++) {
						this.manager.remove(paths[i], this.containerPath); // write lock will be acquired by the remove operation
					}
				} else {
					for (int i = 0, max = paths.length; i < max; i++) {
						String documentPath =  this.containerPath.toString() + '/' + paths[i];
						if (!Util.isExcluded(new Path(documentPath), this.inclusionPatterns, this.exclusionPatterns, false))
							this.manager.remove(paths[i], this.containerPath); // write lock will be acquired by the remove operation
					}
				}
			}
		} catch (IOException e) {
			if (JobManager.VERBOSE) {
				Util.verbose("-> failed to remove " + this.folderPath + " from index because of the following exception:", System.err); //$NON-NLS-1$ //$NON-NLS-2$
				e.printStackTrace();
			}
			return false;
		} finally {
			monitor.exitRead(); // free read lock
		}
		return true;
	}
	@Override
	public String toString() {
		return "removing " + this.folderPath + " from index " + this.containerPath; //$NON-NLS-1$ //$NON-NLS-2$
	}
}
