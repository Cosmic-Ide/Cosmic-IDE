/*******************************************************************************
 * Copyright (c) 2000, 2014 IBM Corporation and others.
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
package org.eclipse.jdt.core.search;

import org.eclipse.jdt.internal.compiler.SourceElementParser;
import org.eclipse.jdt.internal.core.index.Index;

/**
 * A search document encapsulates a content to be either indexed or searched in.
 * A search participant creates a search document.
 * <p>
 * This class is intended to be subclassed by clients.
 * </p>
 *
 * @since 3.0
 */
public abstract class SearchDocument {
	private Index index;
	private String containerRelativePath;
	private SourceElementParser parser;
	private String documentPath;
	private SearchParticipant participant;
	private boolean shouldIndexResolvedDocument = false;

	/**
	 * Creates a new search document. The given document path is a string that uniquely identifies the document.
	 * Most of the time it is a workspace-relative path, but it can also be a file system path, or a path inside a zip file.
	 *
	 * @param documentPath the path to the document,
	 * or <code>null</code> if none
	 * @param participant the participant that creates the search document
	 */
	protected SearchDocument(String documentPath, SearchParticipant participant) {
		this.documentPath = documentPath;
		this.participant = participant;
	}

	/**
	 * Adds the given index entry (category and key) coming from this
	 * document to the index. This method must be called from
	 * {@link SearchParticipant#indexDocument(SearchDocument document, org.eclipse.core.runtime.IPath indexPath)}.
	 *
	 * @param category the category of the index entry
	 * @param key the key of the index entry
	 */
	public void addIndexEntry(char[] category, char[] key) {
		if (this.index != null)
			this.index.addIndexEntry(category, key, getContainerRelativePath());
	}

	/**
	 * Returns the contents of this document.
	 * Contents may be different from actual resource at corresponding document path,
	 * in case of preprocessing.
	 * <p>
	 * This method must be implemented in subclasses.
	 * </p><p>
	 * Note: some implementation may choose to cache the contents directly on the
	 * document for performance reason. However, this could induce scalability issues due
	 * to the fact that collections of documents are manipulated throughout the search
	 * operation, and cached contents would then consume lots of memory until they are
	 * all released at once in the end.
	 * </p>
	 *
	 * @return the contents of this document,
	 * or <code>null</code> if none
	 */
	public abstract byte[] getByteContents();

	/**
	 * Returns the contents of this document.
	 * Contents may be different from actual resource at corresponding document
	 * path due to preprocessing.
	 * <p>
	 * This method must be implemented in subclasses.
	 * </p><p>
	 * Note: some implementation may choose to cache the contents directly on the
	 * document for performance reason. However, this could induce scalability issues due
	 * to the fact that collections of documents are manipulated throughout the search
	 * operation, and cached contents would then consume lots of memory until they are
	 * all released at once in the end.
	 * </p>
	 *
	 * @return the contents of this document,
	 * or <code>null</code> if none
	 */
	public abstract char[] getCharContents();

	private String getContainerRelativePath() {
		if (this.containerRelativePath == null)
			this.containerRelativePath = this.index.containerRelativePath(getPath());
		return this.containerRelativePath;
	}

	/**
	 * Returns the encoding for this document.
	 * <p>
	 * This method must be implemented in subclasses.
	 * </p>
	 *
	 * @return the encoding for this document,
	 * or <code>null</code> if none
	 */
	public abstract String getEncoding();

	/**
	 * @nooverride This method is not intended to be re-implemented or extended by clients.
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public SourceElementParser getParser() {
		return this.parser;
	}

	/**
	 * Returns the participant that created this document.
	 *
	 * @return the participant that created this document
	 */
	public final SearchParticipant getParticipant() {
		return this.participant;
	}

	/**
	 * Returns the path to the original document to publicly mention in index
	 * or search results. This path is a string that uniquely identifies the document.
	 * Most of the time it is a workspace-relative path, but it can also be a file system path,
	 * or a path inside a zip file.
	 *
	 * @return the path to the document
	 */
	public final String getPath() {
		return this.documentPath;
	}
	/**
	 * Removes all index entries from the index for the given document.
	 * This method must be called from
	 * {@link SearchParticipant#indexDocument(SearchDocument document, org.eclipse.core.runtime.IPath indexPath)}.
	 */
	public void removeAllIndexEntries() {
		if (this.index != null)
			this.index.remove(getContainerRelativePath());
	}

	/**
	 * @nooverride This method is not intended to be re-implemented or extended by clients.
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public void setIndex(Index indexToSet) {
		this.index = indexToSet;
	}

	/**
	 * @nooverride This method is not intended to be re-implemented or extended by clients.
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public void setParser(SourceElementParser sourceElementParser) {
		this.parser = sourceElementParser;
	}

	/** Flags the document as requiring indexing after symbol and type resolution. A participant would be asked
	 *  to resolve the document via {@link SearchParticipant#resolveDocument} and to index the document adding
	 *  additional entries via {@link SearchParticipant#indexResolvedDocument}
	 *
	 * @since 3.10
	 */
	public void requireIndexingResolvedDocument() {
		this.shouldIndexResolvedDocument = true;
	}

	/**
	 * @nooverride This method is not intended to be re-implemented or extended by clients.
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public boolean shouldIndexResolvedDocument() {
		return this.shouldIndexResolvedDocument;
	}
}
