/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
package org.eclipse.jdt.internal.core.search;

import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.search.*;
import org.eclipse.jdt.internal.compiler.lookup.TypeConstants;
import org.eclipse.jdt.internal.core.index.IndexLocation;
import org.eclipse.jdt.internal.core.search.indexing.BinaryIndexer;
import org.eclipse.jdt.internal.core.search.indexing.ManifestIndexer;
import org.eclipse.jdt.internal.core.search.indexing.SourceIndexer;
import org.eclipse.jdt.internal.core.search.matching.MatchLocator;

/**
 * A search participant describes a particular extension to a generic search mechanism, allowing thus to
 * perform combined search actions which will involve all required participants
 *
 * A search scope defines which participants are involved.
 *
 * A search participant is responsible for holding index files, and selecting the appropriate ones to feed to
 * index queries. It also can map a document path to an actual document (note that documents could live outside
 * the workspace or no exist yet, and thus aren't just resources).
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class JavaSearchParticipant extends SearchParticipant implements IParallelizable {

	private final ThreadLocal indexSelector = new ThreadLocal();

	/**
	 * The only reason this field exist is the unfortunate idea to share created source indexer
	 * between three calls to this search participant in IndexManager.scheduleDocumentIndexing().
	 * <p>
	 * The field is supposed to be set in indexDocument() and potentially reused
	 * in later calls to resolveDocument() and indexResolvedDocument(), all in the same thread.
	 * <p>
	 * This is the only purpose of this field, and allows us not to manage it via ThreadLocal.
	 * <p>
	 * See org.eclipse.jdt.internal.core.search.indexing.IndexManager.scheduleDocumentIndexing()
	 */
	private SourceIndexer sourceIndexer;

	@Override
	public void beginSearching() {
		super.beginSearching();
		this.indexSelector.remove();
	}

	@Override
	public void doneSearching() {
		this.indexSelector.remove();
		super.doneSearching();
	}

	@Override
	public String getDescription() {
		return "Java"; //$NON-NLS-1$
	}

	@Override
	public SearchDocument getDocument(String documentPath) {
		return new JavaSearchDocument(documentPath, this);
	}

	@Override
	public void indexDocument(SearchDocument document, IPath indexPath) {
		// TODO must verify that the document + indexPath match, when this is not called from scheduleDocumentIndexing
		document.removeAllIndexEntries(); // in case the document was already indexed

		String documentPath = document.getPath();
		if (org.eclipse.jdt.internal.core.util.Util.isJavaLikeFileName(documentPath)) {
			SourceIndexer indexer = new SourceIndexer(document);
			indexer.indexDocument();

			// if the indexer should index resolved document too, remember it for later
			// See org.eclipse.jdt.internal.core.search.indexing.IndexManager.scheduleDocumentIndexing()
			if(document.shouldIndexResolvedDocument()) {
				this.sourceIndexer = indexer;
			}
		} else if (org.eclipse.jdt.internal.compiler.util.Util.isClassFileName(documentPath)) {
			new BinaryIndexer(document).indexDocument();
		} else if (documentPath.endsWith(TypeConstants.AUTOMATIC_MODULE_NAME)) {
			new ManifestIndexer(document).indexDocument();
		}
	}

	@Override
	public void indexResolvedDocument(SearchDocument document, IPath indexPath) {
		String documentPath = document.getPath();
		if (org.eclipse.jdt.internal.core.util.Util.isJavaLikeFileName(documentPath)) {
			SourceIndexer indexer = this.sourceIndexer;
			if (indexer != null) {
				indexer.indexResolvedDocument();
				// Cleanup reference, it is not more needed by the IndexManager
				// See org.eclipse.jdt.internal.core.search.indexing.IndexManager.scheduleDocumentIndexing()
				this.sourceIndexer = null;
			}
		}
	}

	@Override
	public void resolveDocument(SearchDocument document) {
		String documentPath = document.getPath();
		if (org.eclipse.jdt.internal.core.util.Util.isJavaLikeFileName(documentPath)) {
			SourceIndexer indexer = this.sourceIndexer;
			if (indexer != null)
				indexer.resolveDocument();
		}
	}

	@Override
	public void locateMatches(SearchDocument[] indexMatches, SearchPattern pattern,
			IJavaSearchScope scope, SearchRequestor requestor, IProgressMonitor monitor) throws CoreException {

		MatchLocator matchLocator =
			new MatchLocator(
				pattern,
				requestor,
				scope,
				monitor
		);

		/* eliminating false matches and locating them */
		if (monitor != null && monitor.isCanceled()) throw new OperationCanceledException();
		matchLocator.locateMatches(indexMatches);
	}

	@Override
	public IPath[] selectIndexes(SearchPattern pattern, IJavaSearchScope scope) {
		IndexSelector selector = getIndexSelector(pattern, scope);
		IndexLocation[] urls = selector.getIndexLocations();
		IPath[] paths = new IPath[urls.length];
		for (int i = 0; i < urls.length; i++) {
			paths[i] = new Path(urls[i].getIndexFile().getPath());
		}
		return paths;
	}

	private IndexSelector getIndexSelector(SearchPattern pattern, IJavaSearchScope scope) {
		IndexSelector selector = (IndexSelector) this.indexSelector.get();
		if (selector == null) {
			selector = new IndexSelector(scope, pattern);
			this.indexSelector.set(selector);
		}
		return selector;
	}

	public IndexLocation[] selectIndexURLs(SearchPattern pattern, IJavaSearchScope scope) {
		IndexSelector selector = getIndexSelector(pattern, scope);
		return selector.getIndexLocations();
	}

	@Override
	public boolean isParallelSearchSupported() {
		return true;
	}

}
