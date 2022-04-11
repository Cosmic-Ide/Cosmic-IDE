/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.CRC32;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchDocument;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.internal.compiler.ISourceElementRequestor;
import org.eclipse.jdt.internal.compiler.SourceElementParser;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblemFactory;
import org.eclipse.jdt.internal.compiler.util.JRTUtil;
import org.eclipse.jdt.internal.compiler.util.SimpleLookupTable;
import org.eclipse.jdt.internal.compiler.util.SimpleSet;
import org.eclipse.jdt.internal.core.ClasspathEntry;
import org.eclipse.jdt.internal.core.JavaModel;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.jdt.internal.core.JavaProject;
import org.eclipse.jdt.internal.core.index.DiskIndex;
import org.eclipse.jdt.internal.core.index.EntryResult;
import org.eclipse.jdt.internal.core.index.FileIndexLocation;
import org.eclipse.jdt.internal.core.index.Index;
import org.eclipse.jdt.internal.core.index.IndexLocation;
import org.eclipse.jdt.internal.core.index.IndexQualifier;
import org.eclipse.jdt.internal.core.index.MetaIndex;
import org.eclipse.jdt.internal.core.search.BasicSearchEngine;
import org.eclipse.jdt.internal.core.search.PatternSearchJob;
import org.eclipse.jdt.internal.core.search.indexing.QualifierQuery.QueryCategory;
import org.eclipse.jdt.internal.core.search.processing.IJob;
import org.eclipse.jdt.internal.core.search.processing.JobManager;
import org.eclipse.jdt.internal.core.util.Messages;
import org.eclipse.jdt.internal.core.util.Util;

@SuppressWarnings({"rawtypes", "unchecked"})
public class IndexManager extends JobManager implements IIndexConstants {
	/**
	 * Bug 178816:  In case the meta index is causing issue and needs to disable,
	 * specify VM property: {@code -Dorg.eclipse.jdt.disableMetaIndex=true}
	 */
	// For upcoming M builds we make sure meta index is enabled by default, but if this cause problems we will make this
	// disabled by default for final release.
	// disabled by default until Bug574171 is fixed.
	private static final boolean DISABLE_META_INDEX = Boolean.parseBoolean(System.getProperty("org.eclipse.jdt.disableMetaIndex", "false")); //$NON-NLS-1$ //$NON-NLS-2$

	// key = containerPath, value = indexLocation path
	// indexLocation path is created by appending an index file name to the getJavaPluginWorkingLocation() path
	public SimpleLookupTable indexLocations = new SimpleLookupTable();
	// key = indexLocation path, value = an index
	private SimpleLookupTable indexes = new SimpleLookupTable();

	/**
	 * The new indexer is disabled, see bug 544898
	 */
//	private Indexer indexer = Indexer.getInstance();

	/* need to save ? */
	private boolean needToSave = false;
	private IPath javaPluginLocation = null;

	/* can only replace a current state if its less than the new one */
	// key = indexLocation path, value = index state integer
	private SimpleLookupTable indexStates = null;
	private File indexNamesMapFile = new File(getSavedIndexesDirectory(), "indexNamesMap.txt"); //$NON-NLS-1$
	private File participantIndexNamesFile = new File(getSavedIndexesDirectory(), "participantsIndexNames.txt"); //$NON-NLS-1$
	private boolean javaLikeNamesChanged = true;
	public static final Integer SAVED_STATE = 0;
	public static final Integer UPDATING_STATE = 1;
	public static final Integer UNKNOWN_STATE = 2;
	public static final Integer REBUILDING_STATE = 3;
	public static final Integer REUSE_STATE = 4;

	private final IndexNamesRegistry nameRegistry = new IndexNamesRegistry(new File(getSavedIndexesDirectory(),
			"savedIndexNames.txt"), getJavaPluginWorkingLocation()); //$NON-NLS-1$
	// search participants who register indexes with the index manager
	private SimpleLookupTable participantsContainers = null;
	private boolean participantUpdated = false;
	private volatile MetaIndex metaIndex;

	// should JDT manage (update, delete as needed) pre-built indexes?
	public static final String MANAGE_PRODUCT_INDEXES_PROPERTY = "jdt.core.manageProductIndexes"; //$NON-NLS-1$
	private static final boolean IS_MANAGING_PRODUCT_INDEXES_PROPERTY = Boolean.getBoolean(MANAGE_PRODUCT_INDEXES_PROPERTY);

	// save indices after idling for time equal to this property (in ms)
	public static final String INDEX_MANAGER_NOTIFY_IDLE_WAIT_PROPERTY = "jdt.core.indexManager.notifyIdleWait"; //$NON-NLS-1$
	private static final long INDEX_MANAGER_NOTIFY_IDLE_WAIT = getNotifyIdleWait();

	// Debug
	public static boolean DEBUG = false;

	private static final String INDEX_META_CONTAINER = "meta_index"; //$NON-NLS-1$
	LinkedHashSet<Index> metaIndexUpdates;

	public IndexManager() {
		this.metaIndexUpdates = new LinkedHashSet<>();
	}

	/**
	 * Waits until all indexing jobs are done or given monitor is cancelled.
	 *
	 * @param enableIndexer
	 *            {@code true} if this method should temporarily enable index manager and disable it again at the end
	 *            (if the index manager was disabled before calling this method). If {@code false} is given and index
	 *            manager is disabled, this method will block until index manager will be enabled again.
	 *
	 * @param monitor
	 *            for cancellation
	 * @return {@code Status#CANCEL_STATUS} if the waiting was cancelled, {@code Status#OK_STATUS} otherwise
	 */
	public IStatus waitForIndex(final boolean enableIndexer, IProgressMonitor monitor) {
		// => need to know whether the indexing is finished or not
		boolean indexing = awaitingJobsCount() > 0;
		if (!indexing) {
			return Status.OK_STATUS;
		}
		monitor = monitor == null ? new NullProgressMonitor() : monitor;
		int enableCount = 0;
		// Wait for the end of indexing or a cancel
		try {
			while (enableIndexer && !isEnabled()) {
				enableCount ++;
				enable();
			}
			performConcurrentJob(new IJob() {
				@Override
				public boolean belongsTo(String jobFamily) {
					return false;
				}

				@Override
				public void cancel() {
					// job is cancelled through progress
				}

				@Override
				public void ensureReadyToRun() {
					// always ready
				}

				@Override
				public boolean execute(IProgressMonitor progress) {
					return progress == null || !progress.isCanceled();
				}

				@Override
				public String getJobFamily() {
					return ""; //$NON-NLS-1$
				}

			}, IJob.WaitUntilReady, monitor);
			return monitor.isCanceled()? Status.CANCEL_STATUS : Status.OK_STATUS;
		} catch (OperationCanceledException oce) {
			return Status.CANCEL_STATUS;
		} finally {
			while (enableCount > 0) {
				enableCount --;
				disable();
			}
		}
	}

public synchronized void aboutToUpdateIndex(IPath containerPath, Integer newIndexState) {
	// newIndexState is either UPDATING_STATE or REBUILDING_STATE
	// must tag the index as inconsistent, in case we exit before the update job is started
	IndexLocation indexLocation = computeIndexLocation(containerPath);
	Object state = getIndexStates().get(indexLocation);
	Integer currentIndexState = state == null ? UNKNOWN_STATE : (Integer) state;
	if (currentIndexState.compareTo(REBUILDING_STATE) >= 0) return; // already rebuilding the index

	int compare = newIndexState.compareTo(currentIndexState);
	if (compare > 0) {
		// so UPDATING_STATE replaces SAVED_STATE and REBUILDING_STATE replaces everything
		updateIndexState(indexLocation, newIndexState);
	} else if (compare < 0 && this.indexes.get(indexLocation) == null) {
		// if already cached index then there is nothing more to do
		rebuildIndex(indexLocation, containerPath);
	}
}
/**
 * Trigger addition of a resource to an index
 * Note: the actual operation is performed in background
 */
public void addBinary(IFile resource, IPath containerPath) {
	if (JavaCore.getPlugin() == null) return;
	SearchParticipant participant = SearchEngine.getDefaultSearchParticipant();
	SearchDocument document = participant.getDocument(resource.getFullPath().toString());
	IndexLocation indexLocation = computeIndexLocation(containerPath);
	scheduleDocumentIndexing(document, containerPath, indexLocation, participant);
}
/**
 * Trigger addition of a resource to an index
 * Note: the actual operation is performed in background
 */
public void addSource(IFile resource, IPath containerPath, SourceElementParser parser) {
	if (JavaCore.getPlugin() == null) return;
	SearchParticipant participant = SearchEngine.getDefaultSearchParticipant();
	SearchDocument document = participant.getDocument(resource.getFullPath().toString());
	document.setParser(parser);
	IndexLocation indexLocation = computeIndexLocation(containerPath);
	scheduleDocumentIndexing(document, containerPath, indexLocation, participant);
}
/*
 * Removes unused indexes from disk.
 */
public void cleanUpIndexes() {
	SimpleSet knownPaths = new SimpleSet();
	if(!DISABLE_META_INDEX) {
		knownPaths.add(computeIndexLocation(new Path(INDEX_META_CONTAINER)));
	}
	IJavaSearchScope scope = BasicSearchEngine.createWorkspaceScope();
	PatternSearchJob job = new PatternSearchJob(null, SearchEngine.getDefaultSearchParticipant(), scope, null);
	Index[] selectedIndexes = job.getIndexes(null);
	for (int i = 0, l = selectedIndexes.length; i < l; i++) {
		IndexLocation IndexLocation = selectedIndexes[i].getIndexLocation();
		knownPaths.add(IndexLocation);
	}

	if (this.indexStates != null) {
		Object[] keys = this.indexStates.keyTable;
		IndexLocation[] locations = new IndexLocation[this.indexStates.elementSize];
		int count = 0;
		for (int i = 0, l = keys.length; i < l; i++) {
			IndexLocation key = (IndexLocation) keys[i];
			if (key != null && !knownPaths.includes(key))
				locations[count++] = key;
		}
		if (count > 0)
			removeIndexesState(locations);
	}
	deleteIndexFiles(knownPaths, null);
}
/**
 * Compute the pre-built index location for a specified URL
 */
public synchronized IndexLocation computeIndexLocation(IPath containerPath, final URL newIndexURL) {
	IndexLocation indexLocation = (IndexLocation) this.indexLocations.get(containerPath);
	if (indexLocation == null) {
		if(newIndexURL != null) {
			indexLocation = IndexLocation.createIndexLocation(newIndexURL);
			// update caches
			indexLocation = (IndexLocation) getIndexStates().getKey(indexLocation);
			this.indexLocations.put(containerPath, indexLocation);
		}
	}
	else {
		// an existing index location exists - make sure it has not changed (i.e. the URL has not changed)
		URL existingURL = indexLocation.getUrl();
		if (newIndexURL != null) {
			// if either URL is different then the index location has been updated so rebuild.
			if(!newIndexURL.equals(existingURL)) {
				// URL has changed so remove the old index and create a new one
				this.removeIndex(containerPath);
				// create a new one
				indexLocation = IndexLocation.createIndexLocation(newIndexURL);
				// update caches
				indexLocation = (IndexLocation) getIndexStates().getKey(indexLocation);
				this.indexLocations.put(containerPath, indexLocation);
			}
		}
	}
	return indexLocation;
}
public synchronized IndexLocation computeIndexLocation(IPath containerPath) {
	IndexLocation indexLocation = (IndexLocation) this.indexLocations.get(containerPath);
	if (indexLocation == null) {
		String pathString = containerPath.toOSString();
		CRC32 checksumCalculator = new CRC32();
		checksumCalculator.update(pathString.getBytes());
		String fileName = Long.toString(checksumCalculator.getValue()) + ".index"; //$NON-NLS-1$
		if (VERBOSE)
			Util.verbose("-> index name for " + pathString + " is " + fileName); //$NON-NLS-1$ //$NON-NLS-2$
		// to share the indexLocation between the indexLocations and indexStates tables, get the key from the indexStates table
		indexLocation = (IndexLocation) getIndexStates().getKey(new FileIndexLocation(new File(getSavedIndexesDirectory(), fileName)));
		this.indexLocations.put(containerPath, indexLocation);
	}
	return indexLocation;
}
/**
 * Use {@link #deleteIndexFiles(IProgressMonitor)}
 */
public final void deleteIndexFiles() {
	deleteIndexFiles(null);
}
public void deleteIndexFiles(IProgressMonitor monitor) {
	if (DEBUG)
		Util.verbose("Deleting index files"); //$NON-NLS-1$
	this.nameRegistry.delete(); // forget saved indexes & delete each index file
	deleteIndexFiles(null, monitor);
	synchronized (this) {
		this.metaIndex = null;
	}
}
private void deleteIndexFiles(SimpleSet pathsToKeep, IProgressMonitor monitor) {
	File[] indexesFiles = getSavedIndexesDirectory().listFiles();
	if (indexesFiles == null) return;

	SubMonitor subMonitor = SubMonitor.convert(monitor, indexesFiles.length);
	for (int i = 0, l = indexesFiles.length; i < l; i++) {
		subMonitor.split(1);
		String fileName = indexesFiles[i].getAbsolutePath();
		if (pathsToKeep != null && pathsToKeep.includes(new FileIndexLocation(indexesFiles[i]))) continue;
		String suffix = ".index"; //$NON-NLS-1$
		if (fileName.regionMatches(true, fileName.length() - suffix.length(), suffix, 0, suffix.length())) {
			if (VERBOSE || DEBUG)
				Util.verbose("Deleting index file " + indexesFiles[i]); //$NON-NLS-1$
			indexesFiles[i].delete();
		}
	}
}
/*
 * Creates an empty index at the given location, for the given container path, if none exist.
 */
public synchronized void ensureIndexExists(IndexLocation indexLocation, IPath containerPath) {
	// New index is disabled, see bug 544898
    // this.indexer.makeWorkspacePathDirty(containerPath);
	SimpleLookupTable states = getIndexStates();
	Object state = states.get(indexLocation);
	if (state == null) {
		updateIndexState(indexLocation, REBUILDING_STATE);
		getIndex(containerPath, indexLocation, true, true);
	}
}
public SourceElementParser getSourceElementParser(IJavaProject project, ISourceElementRequestor requestor) {
	// disable task tags to speed up parsing
	Map options = project.getOptions(true);
	options.put(JavaCore.COMPILER_TASK_TAGS, ""); //$NON-NLS-1$

	SourceElementParser parser = new IndexingParser(
		requestor,
		new DefaultProblemFactory(Locale.getDefault()),
		new CompilerOptions(options),
		true, // index local declarations
		true, // optimize string literals
		false); // do not use source javadoc parser to speed up parsing
	parser.reportOnlyOneSyntaxError = true;

	// Always check javadoc while indexing
	parser.javadocParser.checkDocComment = true;
	parser.javadocParser.reportProblems = false;

	return parser;
}
/**
 * Returns the index for a given index location
 *
 * @param indexLocation The path of the index file
 * @return The corresponding index or <code>null</code> if not found
 */
public synchronized Index getIndex(IndexLocation indexLocation) {
	return (Index) this.indexes.get(indexLocation); // is null if unknown, call if the containerPath must be computed
}
/**
 * Returns the index for a given project, according to the following algorithm:
 * - if index is already in memory: answers this one back
 * - if (reuseExistingFile) then read it and return this index and record it in memory
 * - if (createIfMissing) then create a new empty index and record it in memory
 *
 * Warning: Does not check whether index is consistent (not being used)
 */
public synchronized Index getIndex(IPath containerPath, boolean reuseExistingFile, boolean createIfMissing) {
	IndexLocation indexLocation = computeIndexLocation(containerPath);
	return getIndex(containerPath, indexLocation, reuseExistingFile, createIfMissing);
}
/**
 * Returns the index for a given project, according to the following algorithm:
 * - if index is already in memory: answers this one back
 * - if (reuseExistingFile) then read it and return this index and record it in memory
 * - if (createIfMissing) then create a new empty index and record it in memory
 *
 * Warning: Does not check whether index is consistent (not being used)
 */
public synchronized Index getIndex(IPath containerPath, IndexLocation indexLocation, boolean reuseExistingFile, boolean createIfMissing) {
	// Path is already canonical per construction
	Index index = getIndex(indexLocation);
	if (index == null) {
		Object state = getIndexStates().get(indexLocation);
		Integer currentIndexState = state == null ? UNKNOWN_STATE : (Integer) state;
		if (currentIndexState == UNKNOWN_STATE) {
			// should only be reachable for query jobs
			// IF you put an index in the cache, then AddJarFileToIndex fails because it thinks there is nothing to do
			rebuildIndex(indexLocation, containerPath);
			return null;
		}

		// index isn't cached, consider reusing an existing index file
		String containerPathString = containerPath.getDevice() == null ? containerPath.toString() : containerPath.toOSString();
		if (reuseExistingFile) {
			if (indexLocation.exists()) { // check before creating index so as to avoid creating a new empty index if file is missing
				try {
					index = new Index(indexLocation, containerPathString, true /*reuse index file*/);
					this.indexes.put(indexLocation, index);
					return index;
				} catch (IOException e) {
					// failed to read the existing file or its no longer compatible
					if (currentIndexState != REBUILDING_STATE && currentIndexState != REUSE_STATE) { // rebuild index if existing file is corrupt, unless the index is already being rebuilt
						if (VERBOSE)
							Util.verbose("-> cannot reuse existing index: "+indexLocation+" path: "+containerPathString); //$NON-NLS-1$ //$NON-NLS-2$
						rebuildIndex(indexLocation, containerPath);
						return null;
					}
					/*index = null;*/ // will fall thru to createIfMissing & create a empty index for the rebuild all job to populate
				}
			}
			if (currentIndexState == SAVED_STATE) { // rebuild index if existing file is missing
				rebuildIndex(indexLocation, containerPath);
				return null;
			}
			if (currentIndexState == REUSE_STATE) {
				// supposed to be in reuse state but error in the index file, so reindex.
				if (VERBOSE)
					Util.verbose("-> cannot reuse given index: "+indexLocation+" path: "+containerPathString); //$NON-NLS-1$ //$NON-NLS-2$
				if(!IS_MANAGING_PRODUCT_INDEXES_PROPERTY) {
					this.indexLocations.put(containerPath, null);
					indexLocation = computeIndexLocation(containerPath);
					rebuildIndex(indexLocation, containerPath);
				}
				else {
					rebuildIndex(indexLocation, containerPath, true);
				}
				return null;
			}
		}
		// index wasn't found on disk, consider creating an empty new one
		if (createIfMissing) {
			try {
				if (VERBOSE)
					Util.verbose("-> create empty index: "+indexLocation+" path: "+containerPathString); //$NON-NLS-1$ //$NON-NLS-2$
				index = new Index(indexLocation, containerPathString, false /*do not reuse index file*/);
				this.indexes.put(indexLocation, index);
				return index;
			} catch (IOException e) {
				if (VERBOSE)
					Util.verbose("-> unable to create empty index: "+indexLocation+" path: "+containerPathString); //$NON-NLS-1$ //$NON-NLS-2$
				// The file could not be created. Possible reason: the project has been deleted.
				return null;
			}
		}
	}
	//System.out.println(" index name: " + path.toOSString() + " <----> " + index.getIndexFile().getName());
	return index;
}
/**
 * Returns all the existing indexes for a list of index locations.
 * Note that this may trigger some indexes recreation work
 *
 * @param locations The list of of the index files path
 * @return The corresponding indexes list.
 */
public Index[] getIndexes(IndexLocation[] locations, IProgressMonitor progressMonitor) {
	// acquire the in-memory indexes on the fly
	int length = locations.length;
	Index[] locatedIndexes = new Index[length];
	int count = 0;
	if (this.javaLikeNamesChanged) {
		this.javaLikeNamesChanged = hasJavaLikeNamesChanged();
	}
	for (int i = 0; i < length; i++) {
		if (progressMonitor != null && progressMonitor.isCanceled()) {
			throw new OperationCanceledException();
		}
		// may trigger some index recreation work
		IndexLocation indexLocation = locations[i];
		Index index = getIndex(indexLocation);
		if (index == null) {
			// only need containerPath if the index must be built
			IPath containerPath = (IPath) this.indexLocations.keyForValue(indexLocation);
			if (containerPath != null) {// sanity check
				index = getIndex(containerPath, indexLocation, true /*reuse index file*/, false /*do not create if none*/);
				if (index != null && this.javaLikeNamesChanged && !index.isIndexForJar()) {
					// When a change in java like names extension has been detected, all
					// non jar files indexes (i.e. containing sources) need to be rebuilt.
					// see bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=286379
					File indexFile = index.getIndexFile();
					if (indexFile.exists()) {
						if (DEBUG)
							Util.verbose("Change in javaLikeNames - removing index file for " + containerPath ); //$NON-NLS-1$
						indexFile.delete();
					}
					synchronized (this) {
						this.indexes.put(indexLocation, null);
					}
					rebuildIndex(indexLocation, containerPath);
					index = null;
				}
			} else {
				if (indexLocation.isParticipantIndex() && indexLocation.exists()) { // the index belongs to non-jdt search participant
					try {
						IPath container = getParticipantsContainer(indexLocation);
						if (container != null) {
							index = new Index(indexLocation, container.toOSString(), true /*reuse index file*/);
							synchronized (this) {
								this.indexes.put(indexLocation, index);
							}
						}
					} catch (IOException e) {
						// ignore
					}
				}
			}
		}
		if (index != null)
			locatedIndexes[count++] = index; // only consider indexes which are ready
	}
	if (this.javaLikeNamesChanged) {
		writeJavaLikeNamesFile();
		this.javaLikeNamesChanged = false;
	}
	if (count < length) {
		System.arraycopy(locatedIndexes, 0, locatedIndexes=new Index[count], 0, count);
	}
	return locatedIndexes;
}
public synchronized Index getIndexForUpdate(IPath containerPath, boolean reuseExistingFile, boolean createIfMissing) {
	IndexLocation indexLocation = computeIndexLocation(containerPath);
	if (getIndexStates().get(indexLocation) == REBUILDING_STATE)
		return getIndex(containerPath, indexLocation, reuseExistingFile, createIfMissing);

	return null; // abort the job since the index has been removed from the REBUILDING_STATE
}
SimpleLookupTable getIndexStates() {
	if (this.indexStates != null) return this.indexStates;

	this.indexStates = new SimpleLookupTable();
	File indexesDirectoryPath = getSavedIndexesDirectory();
	char[][] savedNames = this.nameRegistry.read(null);
	if (savedNames != null) {
		for (int i = 1, l = savedNames.length; i < l; i++) { // first name is saved signature, see readIndexState()
			char[] savedName = savedNames[i];
			if (savedName.length > 0) {
				IndexLocation indexLocation = new FileIndexLocation(new File(indexesDirectoryPath, String.valueOf(savedName))); // shares indexesDirectoryPath's segments
				if (VERBOSE)
					Util.verbose("Reading saved index file " + indexLocation); //$NON-NLS-1$
				this.indexStates.put(indexLocation, SAVED_STATE);
			}
		}
	} else {
		// All the index files are getting deleted and hence there is no need to
		// further check for change in javaLikeNames.
		writeJavaLikeNamesFile();
		this.javaLikeNamesChanged = false;
		deleteIndexFiles();
	}
	readIndexMap();
	return this.indexStates;
}
private IPath getParticipantsContainer(IndexLocation indexLocation) {
	if (this.participantsContainers == null) {
		readParticipantsIndexNamesFile();
	}
	return (IPath)this.participantsContainers.get(indexLocation);
}
private IPath getJavaPluginWorkingLocation() {
	if (this.javaPluginLocation != null) return this.javaPluginLocation;

	IPath stateLocation = JavaCore.getPlugin().getStateLocation();
	return this.javaPluginLocation = stateLocation;
}
private File getSavedIndexesDirectory() {
	return new File(getJavaPluginWorkingLocation().toOSString());
}
/*
 * see https://bugs.eclipse.org/bugs/show_bug.cgi?id=286379
 * Returns true if there is a change in javaLikeNames since it
 * has been last stored.
 * The javaLikeNames stored in the file javaLikeNames.txt
 * is compared with the current javaLikeNames and if there is a change, this
 * function returns true. If the file javaLikeNames.txt doesn't exist and there
 * is only one javaLikeName (.java), then this returns false so that no-reindexing
 * happens.
 */
private boolean hasJavaLikeNamesChanged() {
	char[][] currentNames = Util.getJavaLikeExtensions();
	int current = currentNames.length;
	char[][] prevNames = readJavaLikeNamesFile();
	if (prevNames == null) {
		if (VERBOSE && current != 1)
			Util.verbose("No Java like names found and there is atleast one non-default javaLikeName", System.err); //$NON-NLS-1$
		return (current != 1); //Ignore if only java
	}
	int prev = prevNames.length;
	if (current != prev) {
		if (VERBOSE)
			Util.verbose("Java like names have changed", System.err); //$NON-NLS-1$
		return true;
	}
	if (current > 1) {
		// Sort the current java like names.
		// Copy the array to avoid modifying the Util static variable
		System.arraycopy(currentNames, 0, currentNames = new char[current][], 0, current);
		Util.sort(currentNames);
	}

	// The JavaLikeNames would have been sorted before getting stored in the file,
	// hence just do a direct compare.
	for (int i = 0; i < current; i++) {
		if (!CharOperation.equals(currentNames[i],prevNames[i])) {
			if (VERBOSE)
				Util.verbose("Java like names have changed", System.err); //$NON-NLS-1$
			return true;
		}
	}
	return false;
}
public void indexDocument(SearchDocument searchDocument, SearchParticipant searchParticipant, Index index, IPath indexLocation) {
	try {
		searchDocument.setIndex(index);
		searchParticipant.indexDocument(searchDocument, indexLocation);
	} finally {
		searchDocument.setIndex(null);
	}
}
public void indexResolvedDocument(SearchDocument searchDocument, SearchParticipant searchParticipant, Index index, IPath indexLocation) {
	searchParticipant.resolveDocument(searchDocument);
	ReadWriteMonitor monitor = index.monitor;
	if (monitor == null)
		return; // index got deleted since acquired
	try {
		monitor.enterWrite(); // ask permission to write
		searchDocument.setIndex(index);
		searchParticipant.indexResolvedDocument(searchDocument, indexLocation);
	} finally {
		searchDocument.setIndex(null);
		monitor.exitWrite();
	}
}
/**
 * Trigger addition of the entire content of a project
 * Note: the actual operation is performed in background
 */
public void indexAll(IProject project) {
	// New index is disabled, see bug 544898
	// this.indexer.makeDirty(project);
	if (JavaCore.getPlugin() == null) return;

	try {
		// Disable index manager to avoid synchronization lock contention when adding new index requests to the queue.
		disable();

		// Also request indexing of binaries on the classpath
		// determine the new children
		try {
			JavaModel model = JavaModelManager.getJavaModelManager().getJavaModel();
			JavaProject javaProject = (JavaProject) model.getJavaProject(project);
			// only consider immediate libraries - each project will do the same
			// NOTE: force to resolve CP variables before calling indexer - 19303, so that initializers
			// will be run in the current thread.
			IClasspathEntry[] entries = javaProject.getResolvedClasspath();
			for (int i = 0; i < entries.length; i++) {
				IClasspathEntry entry= entries[i];
				if (entry.getEntryKind() == IClasspathEntry.CPE_LIBRARY)
					indexLibrary(entry.getPath(), project, ((ClasspathEntry)entry).getLibraryIndexLocation());
			}
		} catch(JavaModelException e){ // cannot retrieve classpath info
		}

		// check if the same request is not already in the queue
		IndexRequest request = new IndexAllProject(project, this);
		requestIfNotWaiting(request);
	} finally {
		// Enable index manager after adding all new index requests to the queue.
		enable();
	}
}
public void indexLibrary(IPath path, IProject requestingProject, URL indexURL) {
	this.indexLibrary(path, requestingProject, indexURL, false);
}

private IndexRequest getRequest(Object target, IPath jPath, IndexLocation indexFile, IndexManager manager, boolean updateIndex) {
	return isJrt(((File) target).getName()) ? new AddJrtToIndex(jPath, indexFile, this, updateIndex) :
		new AddJarFileToIndex(jPath, indexFile, this, updateIndex);
}

private boolean isJrt(String fileName) {
	return fileName != null && fileName.endsWith(JRTUtil.JRT_FS_JAR);
}
/**
 * Trigger addition of a library to an index
 * Note: the actual operation is performed in background
 */
public void indexLibrary(IPath path, IProject requestingProject, URL indexURL, final boolean updateIndex) {
	// New index is disabled, see bug 544898
	// this.indexer.makeWorkspacePathDirty(path);
	// requestingProject is no longer used to cancel jobs but leave it here just in case
	IndexLocation indexFile = null;
	boolean forceIndexUpdate = false;
	if(indexURL != null) {
		if(IS_MANAGING_PRODUCT_INDEXES_PROPERTY) {
			indexFile = computeIndexLocation(path, indexURL);
			if(!updateIndex && !indexFile.exists()) {
				forceIndexUpdate = true;
			}
			else {
				forceIndexUpdate = updateIndex;
			}
		}
		else {
			indexFile = IndexLocation.createIndexLocation(indexURL);
		}
	}
	if (JavaCore.getPlugin() == null) return;
	IndexRequest request = null;
	Object target = JavaModel.getTarget(path, true);
	if (target instanceof IFile) {
		request = isJrt(((IFile) target).getFullPath().toOSString()) ?
				new AddJrtToIndex((IFile) target, indexFile, this, forceIndexUpdate) :
					new AddJarFileToIndex((IFile) target, indexFile, this, forceIndexUpdate);
	} else if (target instanceof File) {
		request = getRequest(target, path, indexFile, this, forceIndexUpdate);
	} else if (target instanceof IContainer) {
		request = new IndexBinaryFolder((IContainer) target, this);
	} else {
		return;
	}

	// check if the same request is not already in the queue
	requestIfNotWaiting(request);
}

synchronized boolean addIndex(IPath containerPath, IndexLocation indexFile) {
	getIndexStates().put(indexFile, REUSE_STATE);
	this.indexLocations.put(containerPath, indexFile);
	Index index = getIndex(containerPath, indexFile, true, false);
	if (index == null) {
		indexFile.close();
		this.indexLocations.put(containerPath, null);
		return false;
	}
	writeIndexMapFile();
	return true;
}

/**
 * Index the content of the given source folder.
 */
public void indexSourceFolder(JavaProject javaProject, IPath sourceFolder, char[][] inclusionPatterns, char[][] exclusionPatterns) {
	IProject project = javaProject.getProject();
	if (this.awaitingJobs.size() > 1) {
		// skip it if a job to index the project is already in the queue
		IndexRequest request = new IndexAllProject(project, this);
		if (isJobWaiting(request)) return;
	}

	request(new AddFolderToIndex(sourceFolder, project, inclusionPatterns, exclusionPatterns, this));
}
public synchronized void jobWasCancelled(IPath containerPath) {
	IndexLocation indexLocation = computeIndexLocation(containerPath);
	Index index = getIndex(indexLocation);
	if (index != null) {
		index.monitor = null;
		this.indexes.removeKey(indexLocation);
	}
	updateIndexState(indexLocation, UNKNOWN_STATE);
}
/**
 * Advance to the next available job, once the current one has been completed.
 * Note: clients awaiting until the job count is zero are still waiting at this point.
 */
@Override
protected synchronized void moveToNextJob() {
	// remember that one job was executed, and we will need to save indexes at some point
	this.needToSave = true;
	super.moveToNextJob();
}
/**
 * No more job awaiting.
 */
@Override
protected void notifyIdle(long idlingTime){
	if (idlingTime > INDEX_MANAGER_NOTIFY_IDLE_WAIT && this.needToSave) saveIndexes();
}
/**
 * Name of the background process
 */
@Override
public String processName(){
	return Messages.process_name;
}
private char[][] readJavaLikeNamesFile() {
	try {
		String pathName = getJavaPluginWorkingLocation().toOSString();
		File javaLikeNamesFile = new File(pathName, "javaLikeNames.txt"); //$NON-NLS-1$
		if (!javaLikeNamesFile.exists())
			return null;
		char[] javaLikeNames = org.eclipse.jdt.internal.compiler.util.Util.getFileCharContent(javaLikeNamesFile, null);
		if (javaLikeNames.length > 0) {
			char[][] names = CharOperation.splitOn('\n', javaLikeNames);
			return names;
		}
	} catch (IOException ignored) {
		if (VERBOSE)
			Util.verbose("Failed to read javaLikeNames file"); //$NON-NLS-1$
	}
	return null;
}
private void rebuildIndex(IndexLocation indexLocation, IPath containerPath) {
	rebuildIndex(indexLocation, containerPath, false);
}
private void rebuildIndex(IndexLocation indexLocation, IPath containerPath, final boolean updateIndex) {
	// New index is disabled, see bug 544898
	// this.indexer.makeWorkspacePathDirty(containerPath);
	Object target = JavaModel.getTarget(containerPath, true);
	if (target == null) return;

	if (VERBOSE)
		Util.verbose("-> request to rebuild index: "+indexLocation+" path: "+containerPath); //$NON-NLS-1$ //$NON-NLS-2$

	updateIndexState(indexLocation, REBUILDING_STATE);
	IndexRequest request = null;
	if (target instanceof IProject) {
		IProject p = (IProject) target;
		if (JavaProject.hasJavaNature(p))
			request = new IndexAllProject(p, this);
	} else if (target instanceof IFolder) {
		request = new IndexBinaryFolder((IFolder) target, this);
	} else if (target instanceof IFile) {
		request = isJrt(((IFile) target).getFullPath().toOSString()) ?
				new AddJrtToIndex((IFile) target, null, this, updateIndex) :
					new AddJarFileToIndex((IFile) target, null, this, updateIndex);
	} else if (target instanceof File) {
		request = getRequest(target, containerPath, null, this, updateIndex);
	}
	if (request != null)
		request(request);
}
/**
 * Recreates the index for a given path, keeping the same read-write monitor.
 * Returns the new empty index or null if it didn't exist before.
 * Warning: Does not check whether index is consistent (not being used)
 */
public synchronized Index recreateIndex(IPath containerPath) {
	// only called to over write an existing cached index...
	String containerPathString = containerPath.getDevice() == null ? containerPath.toString() : containerPath.toOSString();
	try {
		// Path is already canonical
		IndexLocation indexLocation = computeIndexLocation(containerPath);
		Index index = getIndex(indexLocation);
		ReadWriteMonitor monitor = index == null ? null : index.monitor;

		if (VERBOSE)
			Util.verbose("-> recreating index: "+indexLocation+" for path: "+containerPathString); //$NON-NLS-1$ //$NON-NLS-2$
		index = new Index(indexLocation, containerPathString, false /*do not reuse index file*/);
		this.indexes.put(indexLocation, index);
		index.monitor = monitor;
		return index;
	} catch (IOException e) {
		// The file could not be created. Possible reason: the project has been deleted.
		if (VERBOSE) {
			Util.verbose("-> failed to recreate index for path: "+containerPathString); //$NON-NLS-1$
			e.printStackTrace();
		}
		return null;
	}
}
/**
 * Trigger removal of a resource to an index
 * Note: the actual operation is performed in background
 */
public void remove(String containerRelativePath, IPath indexedContainer){
	// New index is disabled, see bug 544898
	// this.indexer.makeWorkspacePathDirty(indexedContainer);
	request(new RemoveFromIndex(containerRelativePath, indexedContainer, this));
}
/**
 * Removes the index for a given path.
 * This is a no-op if the index did not exist.
 */
public void removeIndex(IPath containerPath) {
	File indexFile = null;
	Index index = null;
	synchronized (this) {
		if (VERBOSE || DEBUG)
			Util.verbose("removing index " + containerPath); //$NON-NLS-1$
		// New index is disabled, see bug 544898
		// this.indexer.makeWorkspacePathDirty(containerPath);
		IndexLocation indexLocation = computeIndexLocation(containerPath);
		index = getIndex(indexLocation);
		if (index != null) {
			index.monitor = null;
			indexFile = index.getIndexFile();
		}
		if (indexFile == null)
			indexFile = indexLocation.getIndexFile(); // index is not cached yet, but still want to delete the file
		if (this.indexStates.get(indexLocation) == REUSE_STATE) {
			indexLocation.close();
			this.indexLocations.put(containerPath, null);
		} else if (indexFile != null && indexFile.exists()) {
			if (DEBUG)
				Util.verbose("removing index file " + indexFile); //$NON-NLS-1$
			indexFile.delete();
		}
		this.indexes.removeKey(indexLocation);
		if (IS_MANAGING_PRODUCT_INDEXES_PROPERTY) {
			this.indexLocations.removeKey(containerPath);
		}
		updateIndexState(indexLocation, null);
	}

	removeFromMetaIndex(index, indexFile, containerPath);
}

void removeFromMetaIndex(Index index, File indexFile, IPath containerPath) {
	if(DISABLE_META_INDEX) {
		return;
	}
	synchronized(this.metaIndexUpdates) {
		if(index != null) {
			this.metaIndexUpdates.remove(index);
		}
	}
	if (indexFile != null) {
		updateMetaIndex(indexFile.getName(), Collections.emptyList());
	} else {
		if (VERBOSE) {
			Util.verbose(
					String.format("Unable to update meta index for container path %s because index file is null", //$NON-NLS-1$
							containerPath));
		}
	}
}
/**
 * Removes all indexes whose paths start with (or are equal to) the given path.
 */
public void removeIndexPath(IPath path) {
	List<String> affectedIndexes = new ArrayList<>();

	synchronized (this) {
		if (VERBOSE || DEBUG)
			Util.verbose("removing index path " + path); //$NON-NLS-1$
		// New index is disabled, see bug 544898
		// this.indexer.makeWorkspacePathDirty(path);
		Object[] keyTable = this.indexes.keyTable;
		Object[] valueTable = this.indexes.valueTable;
		IndexLocation[] locations = null;
		int max = this.indexes.elementSize;
		int count = 0;
		for (int i = 0, l = keyTable.length; i < l; i++) {
			IndexLocation indexLocation = (IndexLocation) keyTable[i];
			if (indexLocation == null)
				continue;
			if (indexLocation.startsWith(path)) {
				Index index = (Index) valueTable[i];
				affectedIndexes.add(indexLocation.fileName());
				if (!DISABLE_META_INDEX) {
					synchronized (this.metaIndexUpdates) {
						this.metaIndexUpdates.remove(index);
					}
				}
				index.monitor = null;
				if (locations == null)
					locations = new IndexLocation[max];
				locations[count++] = indexLocation;
				if (this.indexStates.get(indexLocation) == REUSE_STATE) {
					indexLocation.close();
				} else {
					if (DEBUG)
						Util.verbose("removing index file " + indexLocation); //$NON-NLS-1$
					indexLocation.delete();
				}
			} else {
				max--;
			}
		}
		if (locations != null) {
			for (int i = 0; i < count; i++)
				this.indexes.removeKey(locations[i]);
			removeIndexesState(locations);
			if (this.participantsContainers != null) {
				boolean update = false;
				for (int i = 0; i < count; i++) {
					if (this.participantsContainers.get(locations[i]) != null) {
						update = true;
						this.participantsContainers.removeKey(locations[i]);
					}
				}
				if (update)
					writeParticipantsIndexNamesFile();
			}
		}
	}
	affectedIndexes.forEach(in -> updateMetaIndex(in, Collections.emptyList()));
}
/**
 * Removes all indexes whose paths start with (or are equal to) the given path.
 */
public void removeIndexFamily(IPath path) {
	// New index is disabled, see bug 544898
	// this.indexer.makeWorkspacePathDirty(path);
	// only finds cached index files... shutdown removes all non-cached index files
	ArrayList toRemove = null;
	synchronized (this) {
		Object[] containerPaths = this.indexLocations.keyTable;
		for (int i = 0, length = containerPaths.length; i < length; i++) {
			IPath containerPath = (IPath) containerPaths[i];
			if (containerPath == null)
				continue;
			if (path.isPrefixOf(containerPath)) {
				if (toRemove == null)
					toRemove = new ArrayList();
				toRemove.add(containerPath);
			}
		}
	}
	if (toRemove != null)
		for (int i = 0, length = toRemove.size(); i < length; i++)
			removeIndex((IPath) toRemove.get(i));
}
/**
 * Remove the content of the given source folder from the index.
 */
public void removeSourceFolderFromIndex(JavaProject javaProject, IPath sourceFolder, char[][] inclusionPatterns, char[][] exclusionPatterns) {
	IProject project = javaProject.getProject();
	if (this.awaitingJobs.size() > 1) {
		// skip it if a job to index the project is already in the queue
		IndexRequest request = new IndexAllProject(project, this);
		if (isJobWaiting(request)) return;
	}

	request(new RemoveFolderFromIndex(sourceFolder, inclusionPatterns, exclusionPatterns, project, this));
}
/**
 * Flush current state
 */
@Override
public void reset() {
	super.reset();
	synchronized (this) {
		if (this.indexes != null) {
			this.indexes = new SimpleLookupTable();
			this.indexStates = null;
		}
		this.indexLocations = new SimpleLookupTable();
		this.javaPluginLocation = null;
		synchronized (this.metaIndexUpdates) {
			this.metaIndexUpdates.clear();
		}
	}
}
/**
 * Resets the index for a given path.
 * Returns true if the index was reset, false otherwise.
 */
public synchronized boolean resetIndex(IPath containerPath) {
	// only called to over write an existing cached index...
	String containerPathString = containerPath.getDevice() == null ? containerPath.toString() : containerPath.toOSString();
	try {
		// Path is already canonical
		IndexLocation indexLocation = computeIndexLocation(containerPath);
		Index index = getIndex(indexLocation);
		if (VERBOSE) {
			Util.verbose("-> reseting index: "+indexLocation+" for path: "+containerPathString); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (index == null) {
			// the index does not exist, try to recreate it
			return recreateIndex(containerPath) != null;
		}
		index.reset();
		return true;
	} catch (IOException e) {
		// The file could not be created. Possible reason: the project has been deleted.
		if (VERBOSE) {
			Util.verbose("-> failed to reset index for path: "+containerPathString); //$NON-NLS-1$
			e.printStackTrace();
		}
		return false;
	}
}
/**
 * {@link #saveIndex(Index)} will only update the state if there are no other jobs running against the same
 * underlying resource for this index.  Pre-built indexes must be in a {@link #REUSE_STATE} state even if
 * there is another job to run against it as the subsequent job will find the index and not save it in the
 * right state.
 * Refer to https://bugs.eclipse.org/bugs/show_bug.cgi?id=405932
 */
public void savePreBuiltIndex(Index index) throws IOException {
	if (index.hasChanged()) {
		if (VERBOSE)
			Util.verbose("-> saving pre-build index " + index.getIndexLocation()); //$NON-NLS-1$
		index.save();
		updateMetaIndex(index);
	}
	synchronized (this) {
		updateIndexState(index.getIndexLocation(), REUSE_STATE);
	}
}
public void saveIndex(Index index) throws IOException {
	ReadWriteMonitor monitor = index.monitor;
	if (monitor == null) return; // index got deleted since acquired

	// must have permission to write from the write monitor
	if (index.hasChanged()) {
		if (VERBOSE)
			Util.verbose("-> saving index " + index.getIndexLocation()); //$NON-NLS-1$
		if (index.save()) {
			updateMetaIndex(index);
		} else {
			if (VERBOSE)
				Util.verbose("-> saving index cancelled " + index.getIndexLocation()); //$NON-NLS-1$
			return;
		}
	}
	synchronized (this) {
		IPath containerPath = new Path(index.containerPath);
		if (this.awaitingJobs.size() > 1) {
			// Start at the end and go backwards
			ListIterator<IJob> iterator = this.awaitingJobs.listIterator(this.awaitingJobs.size());
			IJob first = this.awaitingJobs.get(0);
			while (iterator.hasPrevious()) {
				IJob job = iterator.previous();
				// don't check first job, as it may have already started
				if(job == first) {
					break;
				}
				if (job instanceof IndexRequest) {
					if (((IndexRequest) job).containerPath.equals(containerPath)) {
						return;
					}
				}
			}
		}
		IndexLocation indexLocation = computeIndexLocation(containerPath);
		updateIndexState(indexLocation, SAVED_STATE);
	}
}
/**
 * Commit all index memory changes to disk
 */
public void saveIndexes() {
	// only save cached indexes... the rest were not modified
	ArrayList toSave = new ArrayList();
	synchronized(this) {
		Object[] valueTable = this.indexes.valueTable;
		for (int i = 0, l = valueTable.length; i < l; i++) {
			Index index = (Index) valueTable[i];
			if (index != null)
				toSave.add(index);
		}
	}

	boolean allSaved = true;
	for (int i = 0, length = toSave.size(); i < length; i++) {
		Index index = (Index) toSave.get(i);
		ReadWriteMonitor monitor = index.monitor;
		if (monitor == null) continue; // index got deleted since acquired
		try {
			// take read lock before checking if index has changed
			// don't take write lock yet since it can cause a deadlock (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=50571)
			monitor.enterRead();
			if (index.hasChanged()) {
				if (monitor.exitReadEnterWrite()) {
					try {
						saveIndex(index);
					} catch(IOException | NegativeArraySizeException | OutOfMemoryError e) {
						if(e instanceof FileNotFoundException && index.monitor == null) {
							// index got deleted since acquired
						} else {
							Util.log(e, "Failed to save JDT index: " + index.toString()); //$NON-NLS-1$
							allSaved = false;
						}
					} finally {
						monitor.exitWriteEnterRead();
					}
				} else {
					allSaved = false;
				}
			}
		} finally {
			monitor.exitRead();
		}
	}
	if (this.participantsContainers != null && this.participantUpdated) {
		writeParticipantsIndexNamesFile();
		this.participantUpdated = false;
	}
	allSaved &= saveMetaIndex();

	this.needToSave = !allSaved;
}

private boolean saveMetaIndex() {
	MetaIndex mindex = this.metaIndex;
	if(mindex == null) {
		return true;
	}
	Integer indexState = IndexManager.UNKNOWN_STATE;
	IndexLocation metaIndexLocation;
	synchronized (this) {
		metaIndexLocation = mindex.getIndexLocation();
		indexState = (Integer) this.getIndexStates().get(metaIndexLocation);
	}

	boolean saved = false;
	ReadWriteMonitor monitor = null;
	try {
		monitor = mindex.getMonitor();
		if(monitor != null) {
			monitor.enterWrite();
			if (mindex.hasChanged()) {
				mindex.save();
			}

			if(!IndexManager.SAVED_STATE.equals(indexState)) {
				indexState = IndexManager.SAVED_STATE;
			}
			saved = true;
		}
	} catch (IOException e) {
		JavaCore.getJavaCore().getLog().error("Failed to update qualified index.", e); //$NON-NLS-1$
	} finally {
		if(monitor != null) {
			monitor.exitWrite();
		}
	}

	if(saved) {
		// perform this call outside the lock.
		this.updateIndexState(metaIndexLocation, indexState);
	}
	return saved;
}

public void scheduleDocumentIndexing(final SearchDocument searchDocument, IPath container, final IndexLocation indexLocation, final SearchParticipant searchParticipant) {
	// New index is disabled, see bug 544898
//	IPath targetLocation = JavaIndex.getLocationForPath(new Path(searchDocument.getPath()));
//	if (targetLocation != null) {
//		 this.indexer.makeDirty(targetLocation);
//	}
	request(new IndexRequest(container, this) {
		@Override
		public boolean execute(IProgressMonitor progressMonitor) {
			if (this.isCancelled || progressMonitor != null && progressMonitor.isCanceled()) return true;

			/* ensure no concurrent write access to index */
			Index index = getIndex(this.containerPath, indexLocation, true, /*reuse index file*/ true /*create if none*/);
			if (index == null) return true;
			ReadWriteMonitor monitor = index.monitor;
			if (monitor == null) return true; // index got deleted since acquired
			final Path indexPath = new Path(indexLocation.getCanonicalFilePath());
			try {
				monitor.enterWrite(); // ask permission to write
				indexDocument(searchDocument, searchParticipant, index, indexPath);
			} finally {
				monitor.exitWrite(); // free write lock
			}
			if (searchDocument.shouldIndexResolvedDocument()) {
				indexResolvedDocument(searchDocument, searchParticipant, index, indexPath);
			}
			updateMetaIndex(index);
			return true;
		}
		@Override
		public String toString() {
			return "indexing " + searchDocument.getPath(); //$NON-NLS-1$
		}
		@Override
		public boolean waitNeeded() {
			return false;
		}
	});
}

@Override
public String toString() {
	StringBuilder buffer = new StringBuilder(10);
	buffer.append(super.toString());
	buffer.append("In-memory indexes:\n"); //$NON-NLS-1$
	int count = 0;
	Object[] valueTable = this.indexes.valueTable;
	for (int i = 0, l = valueTable.length; i < l; i++) {
		Index index = (Index) valueTable[i];
		if (index != null)
			buffer.append(++count).append(" - ").append(index.toString()).append('\n'); //$NON-NLS-1$
	}
	return buffer.toString();
}

private void readIndexMap() {
	try {
		char[] indexMaps = org.eclipse.jdt.internal.compiler.util.Util.getFileCharContent(this.indexNamesMapFile, null);
		char[][] names = CharOperation.splitOn('\n', indexMaps);
		if (names.length >= 3) {
			// First line is DiskIndex signature (see writeIndexMapFile())
			String savedSignature = DiskIndex.SIGNATURE;
			if (savedSignature.equals(new String(names[0]))) {
				for (int i = 1, l = names.length-1 ; i < l ; i+=2) {
					IndexLocation indexPath = IndexLocation.createIndexLocation(new URL(new String(names[i])));
					if (indexPath == null) continue;
					this.indexLocations.put(new Path(new String(names[i+1])), indexPath );
					this.indexStates.put(indexPath, REUSE_STATE);
				}
			}
		}
	} catch (IOException ignored) {
		if (VERBOSE)
			Util.verbose("Failed to read saved index file names"); //$NON-NLS-1$
	}
	return;
}
private void readParticipantsIndexNamesFile() {
	SimpleLookupTable containers = new SimpleLookupTable(3);
	try {
		char[] participantIndexNames = org.eclipse.jdt.internal.compiler.util.Util.getFileCharContent(this.participantIndexNamesFile, null);
		if (participantIndexNames.length > 0) {
			char[][] names = CharOperation.splitOn('\n', participantIndexNames);
			if (names.length >= 3) {
				// First line is DiskIndex signature  (see writeParticipantsIndexNamesFile())
				if (DiskIndex.SIGNATURE.equals(new String(names[0]))) {
					for (int i = 1, l = names.length-1 ; i < l ; i+=2) {
						IndexLocation indexLocation = new FileIndexLocation(new File(new String(names[i])), true);
						containers.put(indexLocation, new Path(new String(names[i+1])));
					}
				}
			}
		}
	} catch (IOException ignored) {
		if (VERBOSE)
			Util.verbose("Failed to read participant index file names"); //$NON-NLS-1$
	}
	this.participantsContainers = containers;
	return;
}
private synchronized void removeIndexesState(IndexLocation[] locations) {
	getIndexStates(); // ensure the states are initialized
	int length = locations.length;
	boolean changed = false;
	for (int i=0; i<length; i++) {
		if (locations[i] == null) continue;
		if ((this.indexStates.removeKey(locations[i]) != null)) {
			changed = true;
			if (VERBOSE) {
				Util.verbose("-> index state updated to: ? for: "+locations[i]); //$NON-NLS-1$
			}
		}
	}
	if (!changed) return;

	writeSavedIndexNamesFile();
	writeIndexMapFile();
}
synchronized void updateIndexState(IndexLocation indexLocation, Integer indexState) {
	if (indexLocation == null)
		throw new IllegalArgumentException();

	getIndexStates(); // ensure the states are initialized
	if (indexState != null) {
		if (indexState.equals(this.indexStates.get(indexLocation))) return; // not changed
		this.indexStates.put(indexLocation, indexState);
	} else {
		if (!this.indexStates.containsKey(indexLocation)) return; // did not exist anyway
		this.indexStates.removeKey(indexLocation);
	}

	writeSavedIndexNamesFile();

	if (VERBOSE) {
		if (indexState == null) {
			Util.verbose("-> index state removed for: "+indexLocation); //$NON-NLS-1$
		} else {
			String state = "?"; //$NON-NLS-1$
			if (indexState == SAVED_STATE) state = "SAVED"; //$NON-NLS-1$
			else if (indexState == UPDATING_STATE) state = "UPDATING"; //$NON-NLS-1$
			else if (indexState == UNKNOWN_STATE) state = "UNKNOWN"; //$NON-NLS-1$
			else if (indexState == REBUILDING_STATE) state = "REBUILDING"; //$NON-NLS-1$
			else if (indexState == REUSE_STATE) state = "REUSE"; //$NON-NLS-1$
			Util.verbose("-> index state updated to: " + state + " for: "+indexLocation); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

}
public void updateParticipant(IPath indexPath, IPath containerPath) {
	if (this.participantsContainers == null) {
		readParticipantsIndexNamesFile();
	}
	IndexLocation indexLocation = new FileIndexLocation(indexPath.toFile(), true);
	if (this.participantsContainers.get(indexLocation) == null) {
		this.participantsContainers.put(indexLocation, containerPath);
		this.participantUpdated  = true;
	}
}
private void writeJavaLikeNamesFile() {
	BufferedWriter writer = null;
	String pathName = getJavaPluginWorkingLocation().toOSString();
	try {
		char[][] currentNames = Util.getJavaLikeExtensions();
		int length = currentNames.length;
		if (length > 1) {
			// Sort the current java like names.
			// Copy the array to avoid modifying the Util static variable
			System.arraycopy(currentNames, 0, currentNames=new char[length][], 0, length);
			Util.sort(currentNames);
		}
		File javaLikeNamesFile = new File(pathName, "javaLikeNames.txt"); //$NON-NLS-1$
		writer = new BufferedWriter(new FileWriter(javaLikeNamesFile));
		for (int i = 0; i < length-1; i++) {
			writer.write(currentNames[i]);
			writer.write('\n');
		}
		if (length > 0)
			writer.write(currentNames[length-1]);

	} catch (IOException ignored) {
		if (VERBOSE)
			Util.verbose("Failed to write javaLikeNames file", System.err); //$NON-NLS-1$
	} finally {
		if (writer != null) {
			try {
				writer.close();
			} catch (IOException e) {
				// ignore
			}
		}
	}
}
private void writeIndexMapFile() {
	BufferedWriter writer = null;
	try {
		writer = new BufferedWriter(new FileWriter(this.indexNamesMapFile));
		writer.write(DiskIndex.SIGNATURE);
		writer.write('\n');
		Object[] keys = this.indexStates.keyTable;
		Object[] states = this.indexStates.valueTable;
		for (int i = 0, l = states.length; i < l; i++) {
			IndexLocation location = (IndexLocation)keys[i];
			if (location != null && states[i] == REUSE_STATE) {
				IPath container = (IPath)this.indexLocations.keyForValue(location);
				if (container != null) {
					writer.write(location.toString());
					writer.write('\n');
					writer.write(container.toOSString());
					writer.write('\n');
				}
			}
		}
	} catch (IOException ignored) {
		if (VERBOSE)
			Util.verbose("Failed to write saved index file names", System.err); //$NON-NLS-1$
	} finally {
		if (writer != null) {
			try {
				writer.close();
			} catch (IOException e) {
				// ignore
			}
		}
	}
}
private void writeParticipantsIndexNamesFile() {
	BufferedWriter writer = null;
	try {
		writer = new BufferedWriter(new FileWriter(this.participantIndexNamesFile));
		writer.write(DiskIndex.SIGNATURE);
		writer.write('\n');
		Object[] indexFiles = this.participantsContainers.keyTable;
		Object[] containers = this.participantsContainers.valueTable;
		for (int i = 0, l = indexFiles.length; i < l; i++) {
			IndexLocation indexFile = (IndexLocation)indexFiles[i];
			if (indexFile != null) {
				writer.write(indexFile.getIndexFile().getPath());
				writer.write('\n');
				writer.write(((IPath)containers[i]).toOSString());
				writer.write('\n');
			}
		}
	} catch (IOException ignored) {
		if (VERBOSE)
			Util.verbose("Failed to write participant index file names", System.err); //$NON-NLS-1$
	} finally {
		if (writer != null) {
			try {
				writer.close();
			} catch (IOException e) {
				// ignore
			}
		}
	}
}
private void writeSavedIndexNamesFile() {
	Object[] keys = this.indexStates.keyTable;
	Object[] states = this.indexStates.valueTable;
	int numToSave = 0;
	for (int i = 0, l = states.length; i < l; i++) {
		IndexLocation key = (IndexLocation) keys[i];
		if (key != null && states[i] == SAVED_STATE) {
			numToSave++;
		}
	}
	char[][] arrays = new char[numToSave][];
	int idx = 0;
	for (int i = 0, l = states.length; i < l; i++) {
		IndexLocation key = (IndexLocation) keys[i];
		if (key != null && states[i] == SAVED_STATE) {
			arrays[idx++] = key.fileName().toCharArray();
		}
	}
	this.nameRegistry.write(arrays);
}

private static long getNotifyIdleWait() {
	long idleWait = 1000;
	String idleWaitPropertyValue = System.getProperty(INDEX_MANAGER_NOTIFY_IDLE_WAIT_PROPERTY);
	if (idleWaitPropertyValue != null) {
		try {
			idleWait = Long.parseLong(idleWaitPropertyValue);
		} catch (NumberFormatException e) {
			Util.log(e, "Failed to parse value of property \"" + INDEX_MANAGER_NOTIFY_IDLE_WAIT_PROPERTY + "\": " + idleWaitPropertyValue); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
	return idleWait;
}

public Optional<Set<String>> findMatchingIndexNames(QualifierQuery query) {
	if(DISABLE_META_INDEX) {
		return Optional.empty();
	}
	MetaIndex mindex = null;
	long startTime = System.currentTimeMillis();
	try {
		mindex = loadMetaIndexIfNeeded();
		if(mindex == null) {
			return Optional.empty();
		}
		ReadWriteMonitor monitor = mindex.getMonitor();
		if(monitor == null) {
			return Optional.empty();
		}
		monitor.enterRead();
		mindex.startQuery();

		try {
			List<char[]> qualifiedCategories = new ArrayList<>(2);
			List<char[]> simpleCategories = new ArrayList<>(2);

			for (QueryCategory cat : query.getCategories()) {
				if(cat == QueryCategory.REF) {
					qualifiedCategories.add(META_INDEX_QUALIFIED_TYPE_QUALIFIER_REF);
					simpleCategories.add(META_INDEX_SIMPLE_TYPE_QUALIFIER_REF);
				} else if(cat == QueryCategory.SUPER) {
					qualifiedCategories.add(META_INDEX_QUALIFIED_SUPER_TYPE_QUALIFIER_REF);
					simpleCategories.add(META_INDEX_SIMPLE_SUPER_TYPE_QUALIFIER_REF);
				}
			}

			List<EntryResult> results = new ArrayList<>();
			if(query.getQualifiedKey().length > 0) {
				results.addAll(runQuery(mindex, qualifiedCategories.toArray(new char[0][]), query.getQualifiedKey()));
			}
			results.addAll(runQuery(mindex, simpleCategories.toArray(new char[0][]), query.getSimpleKey()));

			Set<String> indexesNotInMeta;
			synchronized (this) {
				indexesNotInMeta = mindex.getIndexesNotInMeta(this.indexes);
			}
			if (VERBOSE) {
				Util.verbose("-> not in meta-index: " + indexesNotInMeta.size() + ", in: "+results.size() + " for query " + query); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
			final Index i = mindex.getIndex();
			return Optional.of(Stream.concat(indexesNotInMeta.stream(), results.stream().flatMap(r -> {
				try {
					return Stream.of(r.getDocumentNames(i));
				} catch (IOException e) {
					return Stream.empty();
				}
			})).collect(Collectors.toSet()));
		} finally {
			mindex.stopQuery();
			monitor.exitRead();
		}
	} catch (IOException e) {
		JavaCore.getJavaCore().getLog().error("Error filtering index locations based on qualifier.", e); //$NON-NLS-1$
		return Optional.empty();
	} finally {
		if (VERBOSE) {
			long wallClockTime = System.currentTimeMillis() - startTime;
			Util.verbose("-> execution time: " + wallClockTime + "ms - IndexManager#findMatchingIndexNames");//$NON-NLS-1$//$NON-NLS-2$
		}
	}
}
private List<EntryResult> runQuery(MetaIndex index, char[][] categories, char[] key) throws IOException {
	EntryResult[] result = index.query(categories, key,
			SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE);
	if(result != null) {
		return Arrays.asList(result);
	}
	return Collections.emptyList();
}

private MetaIndex loadMetaIndexIfNeeded() throws IOException {
	MetaIndex mindex = this.metaIndex;
	if(mindex != null) {
		return mindex;
	}
	synchronized (this) {
		if(this.metaIndex == null) {
			IndexLocation indexLocation = computeIndexLocation(new Path(INDEX_META_CONTAINER));
			Object state = getIndexStates().get(indexLocation);
			Integer currentIndexState = state == null ? UNKNOWN_STATE : (Integer) state;
			if(UNKNOWN_STATE.equals(currentIndexState)) {
				if (VERBOSE) {
					Util.verbose("-> create empty meta-index: "+indexLocation+" path: "+INDEX_META_CONTAINER); //$NON-NLS-1$ //$NON-NLS-2$
				}
				this.metaIndex = new MetaIndex(new Index(indexLocation, INDEX_META_CONTAINER, false));
				updateIndexState(indexLocation, REUSE_STATE);
			} else if(indexLocation.exists()) {
				if (VERBOSE) {
					Util.verbose("-> load existing meta-index: "+indexLocation+" path: "+INDEX_META_CONTAINER); //$NON-NLS-1$ //$NON-NLS-2$
				}
				this.metaIndex = new MetaIndex(new Index(indexLocation, INDEX_META_CONTAINER, true));
			} else {
				getIndexStates().put(indexLocation, UNKNOWN_STATE);
				loadMetaIndexIfNeeded();
			}
		}
		return this.metaIndex;
	}
}

void updateMetaIndex(Index index) {
	if (DISABLE_META_INDEX) {
		return;
	}
	scheduleForMetaIndexUpdate(index);
}

void updateMetaIndex(String indexFileName, List<IndexQualifier> qualifications) {
	if(DISABLE_META_INDEX) {
		return;
	}
	if(indexFileName == null) {
		return;
	}
	ReadWriteMonitor monitor = null;
	try {
		MetaIndex mindex = loadMetaIndexIfNeeded();
		IndexLocation mindexLocation = mindex.getIndexLocation();

		monitor = mindex.getMonitor();
		if(monitor == null) {
			updateIndexState(mindexLocation, UNKNOWN_STATE);
			return;
		}
		if (VERBOSE) {
			int qsize = qualifications.size();
			Util.verbose("-> updating meta-index with " + qsize + " elements for " + indexFileName); //$NON-NLS-1$ //$NON-NLS-2$
		}
		monitor.enterWrite();
		// clean existing entries for current document
		mindex.remove(indexFileName);

		for (IndexQualifier qualifier : qualifications) {
			mindex.addIndexEntry(qualifier.getCategory(), qualifier.getKey(), indexFileName);
		}
		if (VERBOSE) {
			Util.verbose("-> meta-index updated for " + indexFileName); //$NON-NLS-1$
		}
	} catch (IOException e) {
		if (JobManager.VERBOSE) {
			Util.verbose("-> failed to update meta index for index " + indexFileName + " because of the following exception:"); //$NON-NLS-1$ //$NON-NLS-2$
			e.printStackTrace();
		}
	} finally {
		if(monitor != null) {
			monitor.exitWrite();
		}
	}
}

public Optional<MetaIndex> getMetaIndex() {
	if(DISABLE_META_INDEX) {
		return Optional.empty();
	}
	try {
		MetaIndex index = loadMetaIndexIfNeeded();
		return Optional.ofNullable(index);
	} catch (IOException e) {
		JavaCore.getJavaCore().getLog().error(e.getMessage(), e);
		return Optional.empty();
	}
}

void scheduleForMetaIndexUpdate(Index index) {
	synchronized(this.metaIndexUpdates){
		if (this.metaIndexUpdates.contains(index)) {
			if (VERBOSE) {
				Util.verbose("-> already waiting for meta-index update for " + index); //$NON-NLS-1$
			}
			return;
		}
		this.metaIndexUpdates.add(index);
	}
	requestIfNotWaiting(new MetaIndexUpdateRequest());
}

class MetaIndexUpdateRequest implements IJob {
	volatile boolean isCancelled;

	@Override
	public boolean belongsTo(String jobFamily) {
		return false;
	}

	@Override
	public void cancel() {
		this.isCancelled = true;
	}

	@Override
	public void ensureReadyToRun() {
		// no op
	}

	@Override
	public boolean execute(IProgressMonitor progress) {
		while((progress == null || !progress.isCanceled()) && !this.isCancelled) {
			Index index = null;
			synchronized(IndexManager.this.metaIndexUpdates){
				Iterator<Index> iterator = IndexManager.this.metaIndexUpdates.iterator();
				if (iterator.hasNext()) {
					index = iterator.next();
					iterator.remove();
				}
			}
			if (index == null) {
				return true;
			}
			if (index.monitor == null) {
				// index got deleted since acquired
				continue;
			}
			File indexFile = index.getIndexFile();
			if (indexFile == null) {
				continue;
			}
			if (VERBOSE) {
				Util.verbose("-> meta-index update from queue with size " + IndexManager.this.metaIndexUpdates.size()); //$NON-NLS-1$
			}
			try {
				updateMetaIndex(indexFile.getName(), index.getMetaIndexQualifications());
			} catch (IOException e) {
				Throwable cause = e.getCause();
				if (cause != null) {
					Util.log(e, "Failed to update meta index"); //$NON-NLS-1$
				} else {
					if (JobManager.VERBOSE) {
						Util.verbose("-> failed to update meta index for index " + indexFile.getName() //$NON-NLS-1$
								+ " because of the following exception:"); //$NON-NLS-1$
						e.printStackTrace();
					}
				}
			}
		}
		return true;
	}

	@Override
	public String getJobFamily() {
		return ""; //$NON-NLS-1$
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof MetaIndexUpdateRequest)) {
			return false;
		}
		MetaIndexUpdateRequest other = (MetaIndexUpdateRequest) obj;
		if (this.isCancelled != other.isCancelled) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}

}
}
