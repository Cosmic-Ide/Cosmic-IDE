/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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
package org.eclipse.jdt.internal.core.index;

import org.eclipse.jdt.core.search.*;
import org.eclipse.jdt.internal.core.util.*;
import org.eclipse.jdt.internal.compiler.util.HashtableOfObject;
import org.eclipse.jdt.internal.compiler.util.SimpleLookupTable;
import org.eclipse.jdt.internal.compiler.util.SimpleSet;

public class MemoryIndex {

public int NUM_CHANGES = 100; // number of separate document changes... used to decide when to merge

SimpleLookupTable docsToReferences; // document paths -> HashtableOfObject(category names -> set of words)
SimpleWordSet allWords; // save space by locally interning the referenced words, since an indexer can generate numerous duplicates
String lastDocumentName;
HashtableOfObject lastReferenceTable;

MemoryIndex() {
	this.docsToReferences = new SimpleLookupTable(7);
	this.allWords = new SimpleWordSet(7);
}
void addDocumentNames(String substring, SimpleSet results) {
	// assumed the disk index already skipped over documents which have been added/changed/deleted
	Object[] paths = this.docsToReferences.keyTable;
	Object[] referenceTables = this.docsToReferences.valueTable;
	if (substring == null) { // add all new/changed documents
		for (int i = 0, l = referenceTables.length; i < l; i++)
			if (referenceTables[i] != null)
				results.add(paths[i]);
	} else {
		for (int i = 0, l = referenceTables.length; i < l; i++)
			if (referenceTables[i] != null && ((String) paths[i]).startsWith(substring, 0))
				results.add(paths[i]);
	}
}
void addIndexEntry(char[] category, char[] key, String documentName) {
	HashtableOfObject referenceTable;
	if (documentName.equals(this.lastDocumentName))
		referenceTable = this.lastReferenceTable;
	else {
		// assumed a document was removed before its reindexed
		referenceTable = (HashtableOfObject) this.docsToReferences.get(documentName);
		if (referenceTable == null)
			this.docsToReferences.put(documentName, referenceTable = new HashtableOfObject(3));
		this.lastDocumentName = documentName;
		this.lastReferenceTable = referenceTable;
	}

	SimpleWordSet existingWords = (SimpleWordSet) referenceTable.get(category);
	if (existingWords == null)
		referenceTable.put(category, existingWords = new SimpleWordSet(1));

	existingWords.add(this.allWords.add(key));
}
HashtableOfObject addQueryResults(char[][] categories, char[] key, int matchRule, HashtableOfObject results) {
	// assumed the disk index already skipped over documents which have been added/changed/deleted
	// results maps a word -> EntryResult
	Object[] paths = this.docsToReferences.keyTable;
	Object[] referenceTables = this.docsToReferences.valueTable;
	if (matchRule == (SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE) && key != null) {
		nextPath : for (int i = 0, l = referenceTables.length; i < l; i++) {
			HashtableOfObject categoryToWords = (HashtableOfObject) referenceTables[i];
			if (categoryToWords != null) {
				for (int j = 0, m = categories.length; j < m; j++) {
					SimpleWordSet wordSet = (SimpleWordSet) categoryToWords.get(categories[j]);
					if (wordSet != null && wordSet.includes(key)) {
						if (results == null)
							results = new HashtableOfObject(13);
						EntryResult result = (EntryResult) results.get(key);
						if (result == null)
							results.put(key, result = new EntryResult(key, null));
						result.addDocumentName((String) paths[i]);
						continue nextPath;
					}
				}
			}
		}
	} else {
		for (int i = 0, l = referenceTables.length; i < l; i++) {
			HashtableOfObject categoryToWords = (HashtableOfObject) referenceTables[i];
			if (categoryToWords != null) {
				for (int j = 0, m = categories.length; j < m; j++) {
					SimpleWordSet wordSet = (SimpleWordSet) categoryToWords.get(categories[j]);
					if (wordSet != null) {
						char[][] words = wordSet.words;
						for (int k = 0, n = words.length; k < n; k++) {
							char[] word = words[k];
							if (word != null && Index.isMatch(key, word, matchRule)) {
								if (results == null)
									results = new HashtableOfObject(13);
								EntryResult result = (EntryResult) results.get(word);
								if (result == null)
									results.put(word, result = new EntryResult(word, null));
								result.addDocumentName((String) paths[i]);
							}
						}
					}
				}
			}
		}
	}
	return results;
}
boolean hasChanged() {
	return this.docsToReferences.elementSize > 0;
}
void remove(String documentName) {
	if (documentName.equals(this.lastDocumentName)) {
		this.lastDocumentName = null;
		this.lastReferenceTable = null;
	}
	this.docsToReferences.put(documentName, null);
}
boolean shouldMerge() {
	return this.docsToReferences.elementSize >= this.NUM_CHANGES;
}
}
