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
package org.eclipse.jdt.internal.core.search.indexing;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.internal.core.search.processing.IJob;

public abstract class IndexRequest implements IJob {
	protected volatile boolean isCancelled;
	protected IPath containerPath;
	protected IndexManager manager;

	public IndexRequest(IPath containerPath, IndexManager manager) {
		this.containerPath = containerPath;
		this.manager = manager;
	}
	@Override
	public boolean belongsTo(String projectNameOrJarPath) {
		// used to remove pending jobs because the project was deleted... not to delete index files
		// can be found either by project name or JAR path name
		return projectNameOrJarPath.equals(this.containerPath.segment(0))
			|| projectNameOrJarPath.equals(this.containerPath.toString());
	}
	@Override
	public void cancel() {
		this.manager.jobWasCancelled(this.containerPath);
		this.isCancelled = true;
	}
	@Override
	public void ensureReadyToRun() {
		// tag the index as inconsistent
		this.manager.aboutToUpdateIndex(this.containerPath, updatedIndexState());
	}
	@Override
	public String getJobFamily() {
		return this.containerPath.toString();
	}
	protected Integer updatedIndexState() {
		return IndexManager.UPDATING_STATE;
	}
	@Override
	public boolean waitNeeded() {
		return true;
	}
}
