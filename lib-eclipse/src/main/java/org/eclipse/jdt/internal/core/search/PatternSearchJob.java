/*******************************************************************************
 * Copyright (c) 2000, 2021 IBM Corporation and others.
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
 *     Microsoft Corporation - contribution for bug 575562 - improve completion search performance
 *******************************************************************************/
package org.eclipse.jdt.internal.core.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.IParallelizable;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.internal.compiler.env.AccessRuleSet;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.jdt.internal.core.index.FileIndexLocation;
import org.eclipse.jdt.internal.core.index.Index;
import org.eclipse.jdt.internal.core.index.IndexLocation;
import org.eclipse.jdt.internal.core.search.indexing.ReadWriteMonitor;
import org.eclipse.jdt.internal.core.search.matching.MatchLocator;
import org.eclipse.jdt.internal.core.search.processing.IJob;
import org.eclipse.jdt.internal.core.search.processing.JobManager;
import org.eclipse.jdt.internal.core.util.Util;

public class PatternSearchJob implements IJob {

protected SearchPattern pattern;
protected IJavaSearchScope scope;
protected SearchParticipant participant;
protected IndexQueryRequestor requestor;
protected boolean resolveDocumentForJar;
protected boolean resolveDocumentForSourceFiles;
protected boolean areIndexesReady;
protected AtomicLong executionTime;
boolean parallel;

public static final String ENABLE_PARALLEL_SEARCH = "enableParallelJavaIndexSearch";//$NON-NLS-1$
public static final boolean ENABLE_PARALLEL_SEARCH_DEFAULT = true;

public PatternSearchJob(SearchPattern pattern, SearchParticipant participant, IJavaSearchScope scope, IndexQueryRequestor requestor) {
	this(pattern, participant, scope, true, true, requestor);
}

/**
 * Create a search job with the specified search pattern.
 *
 * @param resolveDocumentForJar whether to resolve the document name of a result entry
 *                              if it comes to a JAR library.
 * @param resolveDocumentForSourceFiles whether to resolve the document name of a result entry
 *                                      if it comes from a project's source files.
 */
public PatternSearchJob(SearchPattern pattern, SearchParticipant participant, IJavaSearchScope scope, final boolean resolveDocumentForJar, final boolean resolveDocumentForSourceFiles, IndexQueryRequestor requestor) {
	this.executionTime = new AtomicLong(0);
	this.pattern = pattern;
	this.participant = participant;
	this.scope = scope;
	this.requestor = requestor;
	this.resolveDocumentForJar = resolveDocumentForJar;
	this.resolveDocumentForSourceFiles = resolveDocumentForSourceFiles;
}

@Override
public boolean belongsTo(String jobFamily) {
	return true;
}
@Override
public void cancel() {
	// search job is cancelled through progress
}
@Override
public void ensureReadyToRun() {
	if (!this.areIndexesReady)
		getIndexes(null/*progress*/); // may trigger some index recreation
}
@Override
public boolean execute(IProgressMonitor progressMonitor) {
	SubMonitor subMonitor = SubMonitor.convert(progressMonitor, 3);

	boolean isComplete = COMPLETE;
	this.executionTime.set(0);
	long startTime = System.currentTimeMillis();

	Index[] indexes = getIndexes(subMonitor.split(1));
	try {
		int max = indexes.length;
		SubMonitor loopMonitor = subMonitor.split(2).setWorkRemaining(max);
		this.parallel = canRunInParallel();
		if(this.parallel) {
			isComplete = performParallelSearch(indexes, loopMonitor);
		} else {
			for (int i = 0; i < max; i++) {
				isComplete &= search(indexes[i], this.requestor, loopMonitor.split(1));
			}
		}

		if (JobManager.VERBOSE) {
			if (this.parallel) {
				long wallClockTime = System.currentTimeMillis() - startTime;
				Util.verbose("-> execution time: " + wallClockTime + "ms - " + this);//$NON-NLS-1$//$NON-NLS-2$
				Util.verbose("-> cumulative execution time (" + ForkJoinPool.getCommonPoolParallelism() + "): " //$NON-NLS-1$//$NON-NLS-2$
						+ this.executionTime.get() + "ms - " + this);//$NON-NLS-1$
			} else {
				Util.verbose("-> execution time: " + this.executionTime.get() + "ms - " + this);//$NON-NLS-1$//$NON-NLS-2$
			}
		}
		return isComplete;
	} finally {
		SubMonitor.done(progressMonitor);
	}
}
private boolean performParallelSearch(Index[] indexes, SubMonitor loopMonitor) {
	boolean isComplete = true;
	List<Future<IndexResult>> futures = new ArrayList<>(indexes.length);
	ForkJoinPool commonPool = ForkJoinPool.commonPool();
	ParallelSearchMonitor monitor = new ParallelSearchMonitor(loopMonitor);

	try {
		if (this.scope instanceof IParallelizable) {
			((IParallelizable) this.scope).initBeforeSearch(monitor);
		}
		for (Index index : indexes) {
			futures.add(commonPool.submit(() -> search(index, monitor)));
		}

		for (Future<IndexResult> future : futures) {
			loopMonitor.split(1);
			try {
				IndexResult result = future.get();
				isComplete &= result.complete;
				result.matches.forEach(m -> {
					boolean continueSearch = this.requestor.acceptIndexMatch(m.documentPath, m.indexRecord, this.participant, m.access);
					if(!continueSearch) {
						throw new OperationCanceledException();
					}
				});
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new OperationCanceledException();
			} catch (ExecutionException e) {
				if(e.getCause() instanceof RuntimeException) {
					throw (RuntimeException) e.getCause();
				}
				throw new RuntimeException(e);
			}
		}
	} catch (JavaModelException e) {
		monitor.setCanceled(true);
		throw new RuntimeException("Error initializing scope: " + this.scope, e); //$NON-NLS-1$
	} catch (Exception e) {
		monitor.setCanceled(true);
		throw e;
	}
	return isComplete;
}
public Index[] getIndexes(IProgressMonitor progressMonitor) {
	// acquire the in-memory indexes on the fly
	IndexLocation[] indexLocations;
	int length;
	if (this.participant instanceof JavaSearchParticipant) {
		indexLocations = ((JavaSearchParticipant)this.participant).selectIndexURLs(this.pattern, this.scope);
		length = indexLocations.length;
	} else {
		IPath[] paths = this.participant.selectIndexes(this.pattern, this.scope);
		length = paths.length;
		indexLocations = new IndexLocation[paths.length];
		for (int i = 0, len = paths.length; i < len; i++) {
			indexLocations[i] = new FileIndexLocation(paths[i].toFile(), true);
		}
	}
	Index[] indexes = JavaModelManager.getIndexManager().getIndexes(indexLocations, progressMonitor);
	this.areIndexesReady = indexes.length == length;
	return indexes;
}

@Override
public boolean waitNeeded() {
	return true;
}
@Override
public String getJobFamily() {
	return ""; //$NON-NLS-1$
}

private IndexResult search(Index index, IProgressMonitor progressMonitor) {
	List<IndexMatch> matches = new ArrayList<>();
	boolean complete = search(index,
			collectTo(matches, progressMonitor), progressMonitor);
	return new IndexResult(complete, matches);
}


public boolean search(Index index, IndexQueryRequestor queryRequestor, IProgressMonitor progressMonitor) {
	if (index == null) return COMPLETE;
	if (progressMonitor != null && progressMonitor.isCanceled()) throw new OperationCanceledException();
	ReadWriteMonitor monitor = index.monitor;
	if (monitor == null) return COMPLETE; // index got deleted since acquired
	try {
		monitor.enterRead(); // ask permission to read
		long start = System.currentTimeMillis();
		SearchPattern searchPattern = this.pattern;
		IJavaSearchScope searchScope = this.scope;
		if(this.parallel) {
			searchPattern = clone(searchPattern);
			searchScope = clone(searchScope);
		}

		boolean isFromJar = index.isIndexForJar();
		boolean resolveDocumentName = (isFromJar && this.resolveDocumentForJar)
			|| (!isFromJar && this.resolveDocumentForSourceFiles);
		if (resolveDocumentName) {
			// fall back to the default behavior in case some pattern implementation doesn't adapt to the new index search API.
			MatchLocator.findIndexMatches(searchPattern, index, queryRequestor, this.participant, searchScope, progressMonitor);
		} else {
			MatchLocator.findIndexMatches(searchPattern, index, queryRequestor, this.participant, searchScope, false, progressMonitor);
		}
		this.executionTime.addAndGet(System.currentTimeMillis() - start);
		return COMPLETE;
	} catch (IOException e) {
		if (e instanceof java.io.EOFException) {
			e.printStackTrace();
		} else {
			Throwable cause = e.getCause();
			if (cause != null) {
				Util.log(e, "Search failed for index " + index); //$NON-NLS-1$
			}
		}
		return FAILED;
	} finally {
		monitor.exitRead(); // finished reading
	}
}

private static IJavaSearchScope clone(IJavaSearchScope searchScope) {
	if (searchScope instanceof AbstractSearchScope) {
		try {
			searchScope = ((AbstractSearchScope)searchScope).clone();
		} catch (CloneNotSupportedException e) {
			Util.log(new Status(IStatus.WARNING, JavaCore.PLUGIN_ID,
					"PatternSearchJob could not clone " + searchScope, e));//$NON-NLS-1$
		}
	}
	return searchScope;
}

private static SearchPattern clone(SearchPattern searchPattern) {
	if(searchPattern instanceof Cloneable) {
		try {
			searchPattern = searchPattern.clone();
		} catch (CloneNotSupportedException e) {
			Util.log(new Status(IStatus.WARNING, JavaCore.PLUGIN_ID,
					"PatternSearchJob could not clone " + searchPattern, e));//$NON-NLS-1$
		}
	}
	return searchPattern;
}

@Override
public String toString() {
	return "searching " + this.pattern.toString(); //$NON-NLS-1$
}

private boolean canRunInParallel() {
	return isParallelSearchEnabled() && IParallelizable.isParallelSearchSupported(this.scope)
			&& IParallelizable.isParallelSearchSupported(this.participant)
			&& IParallelizable.isParallelSearchSupported(this.pattern);
}

private boolean isParallelSearchEnabled() {
	IPreferencesService preferenceService = Platform.getPreferencesService();
	if (preferenceService == null) {
		return true;
	}
	return preferenceService.getBoolean(JavaCore.PLUGIN_ID, ENABLE_PARALLEL_SEARCH, ENABLE_PARALLEL_SEARCH_DEFAULT,
			null);
}

private static IndexQueryRequestor collectTo(final List<IndexMatch> collectTo, final IProgressMonitor monitor) {
	return new IndexQueryRequestor() {

		@Override
		public boolean acceptIndexMatch(String documentPath, SearchPattern indexRecord, SearchParticipant participant,
				AccessRuleSet access) {
			collectTo.add(new IndexMatch(documentPath, indexRecord, access));
			return !monitor.isCanceled();
		}
	};
}

static class IndexResult {
	final boolean complete;
	final List<IndexMatch> matches;

	IndexResult(boolean complete, List<IndexMatch> matches) {
		this.complete = complete;
		this.matches = matches;
	}
}

static class IndexMatch {
	final String documentPath;
	final SearchPattern indexRecord;
	final AccessRuleSet access;

	IndexMatch(String documentPath, SearchPattern indexRecord, AccessRuleSet access) {
		this.documentPath = documentPath;
		this.indexRecord = indexRecord;
		this.access = access;
	}
}

static class ParallelSearchMonitor extends NullProgressMonitor {
	private volatile boolean canceled;
	private IProgressMonitor original;

	public ParallelSearchMonitor(IProgressMonitor original) {
		this.original = original;
	}

	@Override
	public boolean isCanceled() {
		return this.canceled || this.original.isCanceled();
	}

	@Override
	public void setCanceled(boolean canceled) {
		this.canceled = canceled;
	}
}

}
