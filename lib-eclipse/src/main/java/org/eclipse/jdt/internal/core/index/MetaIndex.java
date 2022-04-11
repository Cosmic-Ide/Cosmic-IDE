/*******************************************************************************
 * Copyright (c) 2021 Gayan Perera and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Gayan Perera - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.core.index;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.internal.compiler.util.SimpleLookupTable;
import org.eclipse.jdt.internal.core.search.indexing.ReadWriteMonitor;

public class MetaIndex {
	private Index decoratee;
	private Set<String> indexesNotInMeta = null;

	public MetaIndex(Index decoratee) {
		this.decoratee = decoratee;
	}

	public IndexLocation getIndexLocation() {
		return this.decoratee.getIndexLocation();
	}

	public ReadWriteMonitor getMonitor() {
		return this.decoratee.monitor;
	}

	public void remove(String indexName) {
		this.decoratee.remove(indexName);
		Optional.ofNullable(this.indexesNotInMeta).ifPresent(i -> i.add(indexName));
	}

	public void addIndexEntry(char[] metaCategory, char[] qualifier, String name) {
		this.decoratee.addIndexEntry(metaCategory, qualifier, name);
		Optional.ofNullable(this.indexesNotInMeta).ifPresent(i -> i.remove(name));
	}

	public Index getIndex() {
		return this.decoratee;
	}

	public void startQuery() {
		this.decoratee.startQuery();
	}

	public EntryResult[] query(char[][] categories, char[] indexQualifier, int matchRule) throws IOException {
		return this.decoratee.query(categories, indexQualifier, matchRule);
	}

	/**
	 * Returns the index names out of passed in indexes which are not part of the meta index.
	 *
	 * @param indexes all indexes as a {@link SimpleLookupTable} where key is {@link IndexLocation} and value is {@link Index}
	 * @return index names which are not part of or empty.
	 * @throws IOException
	 */
	public Set<String> getIndexesNotInMeta(SimpleLookupTable indexes) throws IOException {
		// this method is accessed in a single thread
		// in the context of meta index usage we don't get index changes because while a search is running the indexing
		// thread is disabled. Therefore we can safely store the calculated values until the meta index is closed.
		if(this.indexesNotInMeta == null) {
			String[] documentNames = this.decoratee.queryDocumentNames(null);
			Set<String> names = new HashSet<>(Arrays.asList(documentNames == null ? new String[0] : documentNames));
			this.indexesNotInMeta = Stream.of(indexes.keyTable).filter(Objects::nonNull)
					.map(IndexLocation.class::cast).map(IndexLocation::fileName).filter(n -> !names.contains(n)).collect(Collectors.toSet());
		}
		return this.indexesNotInMeta;

	}

	// this method is accessed in a single thread
	public void stopQuery() {
		this.decoratee.stopQuery();
		if(this.decoratee.diskIndex.getCacheUserCount() < 0) {
			this.indexesNotInMeta = null;
		}
	}

	public boolean hasChanged() {
		return this.decoratee.hasChanged();
	}

	public void save() throws IOException {
		this.decoratee.save();
	}
}
