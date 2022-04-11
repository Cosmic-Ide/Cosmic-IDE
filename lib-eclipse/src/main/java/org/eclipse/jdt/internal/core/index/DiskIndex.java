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
package org.eclipse.jdt.internal.core.index;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.regex.Pattern;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.search.*;
import org.eclipse.jdt.internal.core.util.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.jdt.internal.compiler.util.HashtableOfIntValues;
import org.eclipse.jdt.internal.compiler.util.HashtableOfObject;
import org.eclipse.jdt.internal.compiler.util.SimpleLookupTable;
import org.eclipse.jdt.internal.compiler.util.SimpleSet;
import org.eclipse.jdt.internal.compiler.util.SimpleSetOfCharArray;

public class DiskIndex {

IndexLocation indexLocation;

private int headerInfoOffset;
private int numberOfChunks;
private int sizeOfLastChunk;
private int[] chunkOffsets;
private int documentReferenceSize; // 1, 2 or more bytes... depends on # of document names
private int startOfCategoryTables;
private HashtableOfIntValues categoryOffsets, categoryEnds;

private int cacheUserCount;
private String[][] cachedChunks; // decompressed chunks of document names
private HashtableOfObject categoryTables; // category name -> HashtableOfObject(words -> int[] of document #'s) or offset if not read yet
private char[] cachedCategoryName;

private static final int DEFAULT_BUFFER_SIZE = 2048;
private static int BUFFER_READ_SIZE = DEFAULT_BUFFER_SIZE;
private static final int BUFFER_WRITE_SIZE = DEFAULT_BUFFER_SIZE;
private byte[] streamBuffer;
private int bufferIndex, bufferEnd; // used when reading from the file into the streamBuffer
private int streamEnd; // used when writing data from the streamBuffer to the file
char separator = Index.DEFAULT_SEPARATOR;

public static final String SIGNATURE= "INDEX VERSION 1.131"; //$NON-NLS-1$
private static final char[] SIGNATURE_CHARS = SIGNATURE.toCharArray();
public static boolean DEBUG = false;

private static final int RE_INDEXED = -1;
private static final int DELETED = -2;

private static final int CHUNK_SIZE = 100;

private static final SimpleSetOfCharArray INTERNED_CATEGORY_NAMES = new SimpleSetOfCharArray(20);
private static final String TMP_EXT = ".tmp"; //$NON-NLS-1$

static class IntList {

int size;
int[] elements;

IntList(int[] elements) {
	this.elements = elements;
	this.size = elements.length;
}
void add(int newElement) {
	if (this.size == this.elements.length) {
		int newSize = this.size * 3;
		if (newSize < 7) newSize = 7;
		System.arraycopy(this.elements, 0, this.elements = new int[newSize], 0, this.size);
	}
	this.elements[this.size++] = newElement;
}
int[] asArray() {
	int[] result = new int[this.size];
	System.arraycopy(this.elements, 0, result, 0, this.size);
	return result;
}
}


DiskIndex() {
	this.headerInfoOffset = -1;
	this.numberOfChunks = -1;
	this.sizeOfLastChunk = -1;
	this.chunkOffsets = null;
	this.documentReferenceSize = -1;
	this.cacheUserCount = -1;
	this.cachedChunks = null;
	this.categoryTables = null;
	this.cachedCategoryName = null;
	this.categoryOffsets = null;
	this.categoryEnds = null;
}
DiskIndex(IndexLocation location) throws IOException {
	this();
	if (location == null) {
		throw new IllegalArgumentException();
	}
	this.indexLocation = location;
}
SimpleSet addDocumentNames(String substring, MemoryIndex memoryIndex) throws IOException {
	// must skip over documents which have been added/changed/deleted in the memory index
	String[] docNames = readAllDocumentNames();
	SimpleSet results = new SimpleSet(docNames.length);
	if (substring == null) {
		if (memoryIndex == null) {
			for (int i = 0, l = docNames.length; i < l; i++)
				results.add(docNames[i]);
		} else {
			SimpleLookupTable docsToRefs = memoryIndex.docsToReferences;
			for (int i = 0, l = docNames.length; i < l; i++) {
				String docName = docNames[i];
				if (!docsToRefs.containsKey(docName))
					results.add(docName);
			}
		}
	} else {
		if (memoryIndex == null) {
			for (int i = 0, l = docNames.length; i < l; i++)
				if (docNames[i].startsWith(substring, 0))
					results.add(docNames[i]);
		} else {
			SimpleLookupTable docsToRefs = memoryIndex.docsToReferences;
			for (int i = 0, l = docNames.length; i < l; i++) {
				String docName = docNames[i];
				if (docName.startsWith(substring, 0) && !docsToRefs.containsKey(docName))
					results.add(docName);
			}
		}
	}
	return results;
}
private HashtableOfObject addQueryResult(HashtableOfObject results, char[] word, Object docs, MemoryIndex memoryIndex, boolean prevResults) throws IOException {
	// must skip over documents which have been added/changed/deleted in the memory index
	if (results == null)
		results = new HashtableOfObject(13);
	EntryResult result = prevResults ? (EntryResult) results.get(word) : null;
	if (memoryIndex == null) {
		if (result == null)
			results.putUnsafely(word, new EntryResult(word, docs));
		else
			result.addDocumentTable(docs);
	} else {
		SimpleLookupTable docsToRefs = memoryIndex.docsToReferences;
		if (result == null) result = new EntryResult(word, null);
		int[] docNumbers = readDocumentNumbers(docs);
		for (int i = 0, l = docNumbers.length; i < l; i++) {
			String docName = readDocumentName(docNumbers[i]);
			if (!docsToRefs.containsKey(docName))
				result.addDocumentName(docName);
		}
		if (!result.isEmpty())
			results.put(word, result);
	}
	return results;
}
HashtableOfObject addQueryResults(char[][] categories, char[] key, int matchRule, MemoryIndex memoryIndex) throws IOException {
	// assumes sender has called startQuery() & will call stopQuery() when finished
	if (this.categoryOffsets == null) return null; // file is empty

	HashtableOfObject results = null; // initialized if needed

	// No need to check the results table for duplicates while processing the
	// first category table or if the first category tables doesn't have any results.
	boolean prevResults = false;
	if (key == null) {
		for (int i = 0, l = categories.length; i < l; i++) {
			HashtableOfObject wordsToDocNumbers = readCategoryTable(categories[i], true); // cache if key is null since its a definite match
			if (wordsToDocNumbers != null) {
				char[][] words = wordsToDocNumbers.keyTable;
				Object[] values = wordsToDocNumbers.valueTable;
				if (results == null)
					results = new HashtableOfObject(wordsToDocNumbers.elementSize);
				for (int j = 0, m = words.length; j < m; j++)
					if (words[j] != null)
						results = addQueryResult(results, words[j], values[j], memoryIndex, prevResults);
			}
			prevResults = results != null;
		}
		if (results != null && this.cachedChunks == null)
			cacheDocumentNames();
	} else {
		switch (matchRule) {
			case SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE:
				for (int i = 0, l = categories.length; i < l; i++) {
					HashtableOfObject wordsToDocNumbers = readCategoryTable(categories[i], false);
					Object value;
					if (wordsToDocNumbers != null && (value = wordsToDocNumbers.get(key)) != null)
						results = addQueryResult(results, key, value, memoryIndex, prevResults);
					prevResults = results != null;
				}
				break;
			case SearchPattern.R_PREFIX_MATCH | SearchPattern.R_CASE_SENSITIVE:
				for (int i = 0, l = categories.length; i < l; i++) {
					HashtableOfObject wordsToDocNumbers = readCategoryTable(categories[i], false);
					if (wordsToDocNumbers != null) {
						char[][] words = wordsToDocNumbers.keyTable;
						Object[] values = wordsToDocNumbers.valueTable;
						for (int j = 0, m = words.length; j < m; j++) {
							char[] word = words[j];
							if (word != null && key[0] == word[0] && CharOperation.prefixEquals(key, word))
								results = addQueryResult(results, word, values[j], memoryIndex, prevResults);
						}
					}
					prevResults = results != null;
				}
				break;
			case SearchPattern.R_REGEXP_MATCH:
				Pattern pattern = Pattern.compile(new String(key));
				for (int i = 0, l = categories.length; i < l; i++) {
					HashtableOfObject wordsToDocNumbers = readCategoryTable(categories[i], false);
					if (wordsToDocNumbers != null) {
						char[][] words = wordsToDocNumbers.keyTable;
						Object[] values = wordsToDocNumbers.valueTable;
						for (int j = 0, m = words.length; j < m; j++) {
							char[] word = words[j];
							if (word != null && pattern.matcher(new String(word)).matches())
								results = addQueryResult(results, word, values[j], memoryIndex, prevResults);
						}
					}
					prevResults = results != null;
				}
				break;
			default:
				for (int i = 0, l = categories.length; i < l; i++) {
					HashtableOfObject wordsToDocNumbers = readCategoryTable(categories[i], false);
					if (wordsToDocNumbers != null) {
						char[][] words = wordsToDocNumbers.keyTable;
						Object[] values = wordsToDocNumbers.valueTable;
						for (int j = 0, m = words.length; j < m; j++) {
							char[] word = words[j];
							if (word != null && Index.isMatch(key, word, matchRule))
								results = addQueryResult(results, word, values[j], memoryIndex, prevResults);
						}
					}
					prevResults = results != null;
				}
		}
	}

	return results;
}
private void cacheDocumentNames() throws IOException {
	// will need all document names so get them now
	this.cachedChunks = new String[this.numberOfChunks][];
	try (InputStream stream = this.indexLocation.getInputStream()) {
		if (this.numberOfChunks > 5) BUFFER_READ_SIZE <<= 1;
		int offset = this.chunkOffsets[0];
		stream.skip(offset);
		this.streamBuffer = new byte[BUFFER_READ_SIZE];
		this.bufferIndex = 0;
		this.bufferEnd = stream.read(this.streamBuffer, 0, this.streamBuffer.length);
		for (int i = 0; i < this.numberOfChunks; i++) {
			int size = i == this.numberOfChunks - 1 ? this.sizeOfLastChunk : CHUNK_SIZE;
			readChunk(this.cachedChunks[i] = new String[size], stream, 0, size);
		}
	} catch (IOException e) {
		this.cachedChunks = null;
		throw e;
	} finally {
		this.indexLocation.close();
		this.streamBuffer = null;
		BUFFER_READ_SIZE = DEFAULT_BUFFER_SIZE;
	}
}
private String[] computeDocumentNames(String[] onDiskNames, int[] positions, SimpleLookupTable indexedDocuments, MemoryIndex memoryIndex) {
	int onDiskLength = onDiskNames.length;
	Object[] docNames = memoryIndex.docsToReferences.keyTable;
	Object[] referenceTables = memoryIndex.docsToReferences.valueTable;
	if (onDiskLength == 0) {
		// disk index was empty, so add every indexed document
		for (int i = 0, l = referenceTables.length; i < l; i++)
			if (referenceTables[i] != null)
				indexedDocuments.put(docNames[i], null); // remember each new document

		String[] newDocNames = new String[indexedDocuments.elementSize];
		int count = 0;
		Object[] added = indexedDocuments.keyTable;
		for (int i = 0, l = added.length; i < l; i++)
			if (added[i] != null)
				newDocNames[count++] = (String) added[i];
		Util.sort(newDocNames);
		for (int i = 0, l = newDocNames.length; i < l; i++)
			indexedDocuments.put(newDocNames[i], Integer.valueOf(i));
		return newDocNames;
	}

	// initialize positions as if each document will remain in the same position
	for (int i = 0; i < onDiskLength; i++)
		positions[i] = i;

	// find out if the memory index has any new or deleted documents, if not then the names & positions are the same
	int numDeletedDocNames = 0;
	nextPath : for (int i = 0, l = docNames.length; i < l; i++) {
		String docName = (String) docNames[i];
		if (docName != null) {
			for (int j = 0; j < onDiskLength; j++) {
				if (docName.equals(onDiskNames[j])) {
					if (referenceTables[i] == null) {
						positions[j] = DELETED;
						numDeletedDocNames++;
					} else {
						positions[j] = RE_INDEXED;
					}
					continue nextPath;
				}
			}
			if (referenceTables[i] != null)
				indexedDocuments.put(docName, null); // remember each new document, skip deleted documents which were never saved
		}
	}

	String[] newDocNames = onDiskNames;
	if (numDeletedDocNames > 0 || indexedDocuments.elementSize > 0) {
		// some new documents have been added or some old ones deleted
		newDocNames = new String[onDiskLength + indexedDocuments.elementSize - numDeletedDocNames];
		int count = 0;
		for (int i = 0; i < onDiskLength; i++)
			if (positions[i] >= RE_INDEXED)
				newDocNames[count++] = onDiskNames[i]; // keep each unchanged document
		Object[] added = indexedDocuments.keyTable;
		for (int i = 0, l = added.length; i < l; i++)
			if (added[i] != null)
				newDocNames[count++] = (String) added[i]; // add each new document
		Util.sort(newDocNames);
		for (int i = 0, l = newDocNames.length; i < l; i++)
			if (indexedDocuments.containsKey(newDocNames[i]))
				indexedDocuments.put(newDocNames[i], Integer.valueOf(i)); // remember the position for each new document
	}

	// need to be able to look up an old position (ref# from a ref[]) and map it to its new position
	// if its old position == DELETED then its forgotton
	// if its old position == ReINDEXED then its also forgotten but its new position is needed to map references
	int count = -1;
	for (int i = 0; i < onDiskLength;) {
		switch(positions[i]) {
			case DELETED :
				i++; // skip over deleted... references are forgotten
				break;
			case RE_INDEXED :
				String newName = newDocNames[++count];
				if (newName.equals(onDiskNames[i])) {
					indexedDocuments.put(newName, Integer.valueOf(count)); // the reindexed docName that was at position i is now at position count
					i++;
				}
				break;
			default :
				if (newDocNames[++count].equals(onDiskNames[i]))
					positions[i++] = count; // the unchanged docName that was at position i is now at position count
		}
	}
	return newDocNames;
}
private void copyQueryResults(HashtableOfObject categoryToWords, int newPosition) {
	char[][] categoryNames = categoryToWords.keyTable;
	Object[] wordSets = categoryToWords.valueTable;
	for (int i = 0, l = categoryNames.length; i < l; i++) {
		char[] categoryName = categoryNames[i];
		if (categoryName != null) {
			SimpleWordSet wordSet = (SimpleWordSet) wordSets[i];
			HashtableOfObject wordsToDocs = (HashtableOfObject) this.categoryTables.get(categoryName);
			if (wordsToDocs == null)
				this.categoryTables.put(categoryName, wordsToDocs = new HashtableOfObject(wordSet.elementSize));

			char[][] words = wordSet.words;
			for (int j = 0, m = words.length; j < m; j++) {
				char[] word = words[j];
				if (word != null) {
					Object o = wordsToDocs.get(word);
					if (o == null) {
						wordsToDocs.putUnsafely(word, new int[] {newPosition});
					} else if (o instanceof IntList) {
						((IntList) o).add(newPosition);
					} else {
						IntList list = new IntList((int[]) o);
						list.add(newPosition);
						wordsToDocs.put(word, list);
					}
				}
			}
		}
	}
}
void initialize(boolean reuseExistingFile) throws IOException {
	if (this.indexLocation.exists()) {
		if (reuseExistingFile) {
			InputStream stream = this.indexLocation.getInputStream();
			if (stream == null) {
				throw new IOException("Failed to use the index file"); //$NON-NLS-1$
			}
			this.streamBuffer = new byte[BUFFER_READ_SIZE];
			this.bufferIndex = 0;
			this.bufferEnd = stream.read(this.streamBuffer, 0, 128);
			try {
				char[] signature = readStreamChars(stream);
				if (!CharOperation.equals(signature, SIGNATURE_CHARS)) {
					throw new IOException(Messages.exception_wrongFormat);
				}
				this.headerInfoOffset = readStreamInt(stream);
				if (this.headerInfoOffset > 0) { // file is empty if its not set
					stream.skip(this.headerInfoOffset - this.bufferEnd); // assume that the header info offset is over current buffer end
					this.bufferIndex = 0;
					this.bufferEnd = stream.read(this.streamBuffer, 0, this.streamBuffer.length);
					readHeaderInfo(stream);
				}
			} finally {
				stream.close();
				this.indexLocation.close();
			}
			return;
		}
		if (!this.indexLocation.delete()) {
			if (DEBUG)
				System.out.println("initialize - Failed to delete index " + this.indexLocation); //$NON-NLS-1$
			throw new IOException("Failed to delete index " + this.indexLocation); //$NON-NLS-1$
		}
	}
	if (this.indexLocation.createNewFile()) {
		FileOutputStream stream = new FileOutputStream(this.indexLocation.getIndexFile(), false);
		try (stream) {
			this.streamBuffer = new byte[BUFFER_READ_SIZE];
			this.bufferIndex = 0;
			writeStreamChars(stream, SIGNATURE_CHARS);
			writeStreamInt(stream, -1); // file is empty
			// write the buffer to the stream
			if (this.bufferIndex > 0) {
				stream.write(this.streamBuffer, 0, this.bufferIndex);
				this.bufferIndex = 0;
			}
		}
	} else {
		if (DEBUG)
			System.out.println("initialize - Failed to create new index " + this.indexLocation); //$NON-NLS-1$
		throw new IOException("Failed to create new index " + this.indexLocation); //$NON-NLS-1$
	}
}
private void initializeFrom(DiskIndex diskIndex, File newIndexFile) throws IOException {
	if (newIndexFile.exists() && !newIndexFile.delete()) { // delete the temporary index file
		if (DEBUG)
			System.out.println("initializeFrom - Failed to delete temp index " + this.indexLocation); //$NON-NLS-1$
	} else if (!newIndexFile.createNewFile()) {
		if (DEBUG)
			System.out.println("initializeFrom - Failed to create temp index " + this.indexLocation); //$NON-NLS-1$
		throw new IOException("Failed to create temp index " + this.indexLocation); //$NON-NLS-1$
	}

	int size = diskIndex.categoryOffsets == null ? 8 : diskIndex.categoryOffsets.elementSize;
	this.categoryOffsets = new HashtableOfIntValues(size);
	this.categoryEnds = new HashtableOfIntValues(size);
	this.categoryTables = new HashtableOfObject(size);
	this.separator = diskIndex.separator;
}
private void mergeCategories(DiskIndex onDisk, int[] positions, FileOutputStream stream) throws IOException {
	// at this point, this.categoryTables contains the names -> wordsToDocs added in copyQueryResults()
	char[][] oldNames = onDisk.categoryOffsets.keyTable;
	for (int i = 0, l = oldNames.length; i < l; i++) {
		char[] oldName = oldNames[i];
		if (oldName != null && !this.categoryTables.containsKey(oldName))
			this.categoryTables.put(oldName, null);
	}

	char[][] categoryNames = this.categoryTables.keyTable;
	for (int i = 0, l = categoryNames.length; i < l; i++)
		if (categoryNames[i] != null)
			mergeCategory(categoryNames[i], onDisk, positions, stream);
	this.categoryTables = null;
}
private void mergeCategory(char[] categoryName, DiskIndex onDisk, int[] positions, FileOutputStream stream) throws IOException {
	HashtableOfObject wordsToDocs = (HashtableOfObject) this.categoryTables.get(categoryName);
	if (wordsToDocs == null)
		wordsToDocs = new HashtableOfObject(3);

	HashtableOfObject oldWordsToDocs = onDisk.readCategoryTable(categoryName, true);
	if (oldWordsToDocs != null) {
		char[][] oldWords = oldWordsToDocs.keyTable;
		Object[] oldArrayOffsets = oldWordsToDocs.valueTable;
		nextWord: for (int i = 0, l = oldWords.length; i < l; i++) {
			char[] oldWord = oldWords[i];
			if (oldWord != null) {
				int[] oldDocNumbers = (int[]) oldArrayOffsets[i];
				int length = oldDocNumbers.length;
				int[] mappedNumbers = new int[length];
				int count = 0;
				for (int j = 0; j < length; j++) {
					int pos = positions[oldDocNumbers[j]];
					if (pos > RE_INDEXED) // forget any reference to a document which was deleted or re_indexed
						mappedNumbers[count++] = pos;
				}
				if (count < length) {
					if (count == 0) continue nextWord; // skip words which no longer have any references
					System.arraycopy(mappedNumbers, 0, mappedNumbers = new int[count], 0, count);
				}

				Object o = wordsToDocs.get(oldWord);
				if (o == null) {
					wordsToDocs.putUnsafely(oldWord, mappedNumbers);
				} else {
					IntList list = null;
					if (o instanceof IntList) {
						list = (IntList) o;
					} else {
						list = new IntList((int[]) o);
						wordsToDocs.put(oldWord, list);
					}
					for (int j = 0; j < count; j++)
						list.add(mappedNumbers[j]);
				}
			}
		}
		onDisk.categoryTables.put(categoryName, null); // flush cached table
	}
	writeCategoryTable(categoryName, wordsToDocs, stream);
}
DiskIndex mergeWith(MemoryIndex memoryIndex) throws IOException {
 	// assume write lock is held
	// compute & write out new docNames
	if (this.indexLocation == null) {
		throw new IOException("Pre-built index file not writeable");  //$NON-NLS-1$
	}
	String[] docNames = readAllDocumentNames();
	int previousLength = docNames.length;
	int[] positions = new int[previousLength]; // keeps track of the position of each document in the new sorted docNames
	SimpleLookupTable indexedDocuments = new SimpleLookupTable(3); // for each new/changed document in the memoryIndex
	docNames = computeDocumentNames(docNames, positions, indexedDocuments, memoryIndex);
	if (docNames.length == 0) {
		if (previousLength == 0) return this; // nothing to do... memory index contained deleted documents that had never been saved

		// index is now empty since all the saved documents were removed
		DiskIndex newDiskIndex = new DiskIndex(this.indexLocation);
		newDiskIndex.initialize(false);
		return newDiskIndex;
	}
	boolean usingTmp = false;
	File oldIndexFile = this.indexLocation.getIndexFile();
	String indexFilePath = oldIndexFile.getPath();
	if (indexFilePath.endsWith(TMP_EXT)) { // the tmp file could not be renamed last time
		indexFilePath = indexFilePath.substring(0, indexFilePath.length()-TMP_EXT.length());
		usingTmp = true;
	} else {
		indexFilePath += TMP_EXT;
	}
	DiskIndex newDiskIndex = new DiskIndex(new FileIndexLocation(new File(indexFilePath)));
	File newIndexFile = newDiskIndex.indexLocation.getIndexFile();
	try {
		newDiskIndex.initializeFrom(this, newIndexFile);
		FileOutputStream stream = new FileOutputStream(newIndexFile, false);
		int offsetToHeader = -1;
		try {
			newDiskIndex.writeAllDocumentNames(docNames, stream);
			docNames = null; // free up the space

			// add each new/changed doc to empty category tables using its new position #
			if (indexedDocuments.elementSize > 0) {
				Object[] names = indexedDocuments.keyTable;
				Object[] integerPositions = indexedDocuments.valueTable;
				for (int i = 0, l = names.length; i < l; i++)
					if (names[i] != null)
						newDiskIndex.copyQueryResults(
							(HashtableOfObject) memoryIndex.docsToReferences.get(names[i]), ((Integer) integerPositions[i]).intValue());
			}
			indexedDocuments = null; // free up the space

			// merge each category table with the new ones & write them out
			if (previousLength == 0)
				newDiskIndex.writeCategories(stream);
			else
				newDiskIndex.mergeCategories(this, positions, stream);
			offsetToHeader = newDiskIndex.streamEnd;
			newDiskIndex.writeHeaderInfo(stream);
			positions = null; // free up the space
		} finally {
			stream.close();
			this.streamBuffer = null;
		}
		newDiskIndex.writeOffsetToHeader(offsetToHeader);

		// rename file by deleting previous index file & renaming temp one
		try {
			Files.deleteIfExists(oldIndexFile.toPath());
		} catch (Exception e2) {
			throw new IOException("Failed to delete index file " + this.indexLocation, e2); //$NON-NLS-1$
		}
		if (!usingTmp) {
			try {
				Files.move(newIndexFile.toPath(), oldIndexFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException ioe) {
				// try again after waiting for two milli secs
				try {
					Thread.sleep(2);
				} catch (InterruptedException e) {
					//ignore
				}
				try {
					Files.move(newIndexFile.toPath(), oldIndexFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException e) {
					Util.log(ioe, "mergeWith - Failed to rename index " + this.indexLocation); //$NON-NLS-1$
					usingTmp = true;
				}
			}
		}
	} catch (IOException e) {
		try {
			Files.deleteIfExists(newIndexFile.toPath());
		} catch (Exception e2) {
			Util.log(e2, "mergeWith - Failed to delete temp index " + newDiskIndex.indexLocation); //$NON-NLS-1$
		}
		throw e;
	}

	if (!usingTmp) // rename done, use the new file
		newDiskIndex.indexLocation = this.indexLocation;
	return newDiskIndex;
}
private synchronized String[] readAllDocumentNames() throws IOException {
	if (this.numberOfChunks <= 0)
		return CharOperation.NO_STRINGS;

	InputStream stream = this.indexLocation.getInputStream();
	try (stream) {
		int offset = this.chunkOffsets[0];
		stream.skip(offset);
		this.streamBuffer = new byte[BUFFER_READ_SIZE];
		this.bufferIndex = 0;
		this.bufferEnd = stream.read(this.streamBuffer, 0, this.streamBuffer.length);
		int lastIndex = this.numberOfChunks - 1;
		String[] docNames = new String[lastIndex * CHUNK_SIZE + this.sizeOfLastChunk];
		for (int i = 0; i < this.numberOfChunks; i++)
			readChunk(docNames, stream, i * CHUNK_SIZE, i < lastIndex ? CHUNK_SIZE : this.sizeOfLastChunk);
		return docNames;
	} finally {
		this.indexLocation.close();
		this.streamBuffer = null;
	}
}
private synchronized HashtableOfObject readCategoryTable(char[] categoryName, boolean readDocNumbers) throws IOException {
	// result will be null if categoryName is unknown
	int offset = this.categoryOffsets.get(categoryName);
	if (offset == HashtableOfIntValues.NO_VALUE) {
		return null;
	}

	if (this.categoryTables == null) {
		this.categoryTables = new HashtableOfObject(3);
	} else {
		HashtableOfObject cachedTable = (HashtableOfObject) this.categoryTables.get(categoryName);
		if (cachedTable != null) {
			if (readDocNumbers) { // must cache remaining document number arrays
				Object[] arrayOffsets = cachedTable.valueTable;
				for (int i = 0, l = arrayOffsets.length; i < l; i++)
					if (arrayOffsets[i] instanceof Integer)
						arrayOffsets[i] = readDocumentNumbers(arrayOffsets[i]);
			}
			return cachedTable;
		}
	}

	InputStream stream = this.indexLocation.getInputStream();
	HashtableOfObject categoryTable = null;
	char[][] matchingWords = null;
	int count = 0;
	int firstOffset = -1;
	this.streamBuffer = new byte[BUFFER_READ_SIZE];
	try {
		stream.skip(offset);
		this.bufferIndex = 0;
		this.bufferEnd = stream.read(this.streamBuffer, 0, this.streamBuffer.length);
		int size = readStreamInt(stream);
		try {
			categoryTable = new HashtableOfObject(size);
		} catch (NegativeArraySizeException | OutOfMemoryError e) {
			String message = "Failed to read index data from " + this.indexLocation + " at offset " + offset //$NON-NLS-1$ //$NON-NLS-2$
					+ " and size " + size; //$NON-NLS-1$
			throw new IOException(message, e);
		}
		int largeArraySize = 256;
		for (int i = 0; i < size; i++) {
			char[] word = readStreamChars(stream);
			int arrayOffset = readStreamInt(stream);
			// if arrayOffset is:
			//		<= 0 then the array size == 1 with the value -> -arrayOffset
			//		> 1 & < 256 then the size of the array is > 1 & < 256, the document array follows immediately
			//		256 if the array size >= 256 followed by another int which is the offset to the array (written prior to the table)
			if (arrayOffset <= 0) {
				categoryTable.putUnsafely(word, new int[] {-arrayOffset}); // store 1 element array by negating documentNumber
			} else if (arrayOffset < largeArraySize) {
				categoryTable.putUnsafely(word, readStreamDocumentArray(stream, arrayOffset)); // read in-lined array providing size
			} else {
				arrayOffset = readStreamInt(stream); // read actual offset
				if (readDocNumbers) {
					if (matchingWords == null)
						matchingWords = new char[size][];
					if (count == 0)
						firstOffset = arrayOffset;
					matchingWords[count++] = word;
				}
				categoryTable.putUnsafely(word, Integer.valueOf(arrayOffset)); // offset to array in the file
			}
		}
		this.categoryTables.put(INTERNED_CATEGORY_NAMES.get(categoryName), categoryTable);
		// cache the table as long as its not too big
		// in practice, some tables can be greater than 500K when they contain more than 10K elements
		this.cachedCategoryName = categoryTable.elementSize < 20000 ? categoryName : null;
	} catch (IOException ioe) {
		this.streamBuffer = null;
		throw ioe;
	} finally {
		stream.close();
		this.indexLocation.close();
	}

	if (matchingWords != null && count > 0) {
		stream = this.indexLocation.getInputStream();
		try {
			stream.skip(firstOffset);
			this.bufferIndex = 0;
			this.bufferEnd = stream.read(this.streamBuffer, 0, this.streamBuffer.length);
			for (int i = 0; i < count; i++) { // each array follows the previous one
				categoryTable.put(matchingWords[i], readStreamDocumentArray(stream, readStreamInt(stream)));
			}
		} catch (IOException ioe) {
			this.streamBuffer = null;
			throw ioe;
		} finally {
			stream.close();
			this.indexLocation.close();
		}
	}
	this.streamBuffer = null;
	return categoryTable;
}
private void readChunk(String[] docNames, InputStream stream, int index, int size) throws IOException {
	String current = new String(readStreamChars(stream));
	docNames[index++] = current;
	for (int i = 1; i < size; i++) {
		if (stream != null && this.bufferIndex + 2 >= this.bufferEnd)
			readStreamBuffer(stream);
		// bug 566262: check if the index file was deleted in parallel, if so throw an IOException instead of risking to run into index OOB exceptions
		if (this.bufferEnd == -1 && stream.available() == 0) {
			throw new IOException(NLS.bind("Stream was closed for index location \"{0}\"", this.indexLocation)); //$NON-NLS-1$
		}
		int start = this.streamBuffer[this.bufferIndex++] & 0xFF;
		int end = this.streamBuffer[this.bufferIndex++] & 0xFF;
		String next  = new String(readStreamChars(stream));
		if (start > 0) {
			if (end > 0) {
				int length = current.length();
				next = current.substring(0, start) + next + current.substring(length - end, length);
			} else {
				next = current.substring(0, start) + next;
			}
		} else if (end > 0) {
			int length = current.length();
			next = next + current.substring(length - end, length);
		}
		docNames[index++] = next;
		current = next;
	}
}
synchronized String readDocumentName(int docNumber) throws IOException {
	if (this.cachedChunks == null)
		this.cachedChunks = new String[this.numberOfChunks][];

	int chunkNumber = docNumber / CHUNK_SIZE;
	String[] chunk = this.cachedChunks[chunkNumber];
	if (chunk == null) {
		boolean isLastChunk = chunkNumber == this.numberOfChunks - 1;
		int start = this.chunkOffsets[chunkNumber];
		int numberOfBytes = (isLastChunk ? this.startOfCategoryTables : this.chunkOffsets[chunkNumber + 1]) - start;
		if (numberOfBytes < 0)
			throw new IllegalArgumentException();
		this.streamBuffer = new byte[numberOfBytes];
		this.bufferIndex = 0;
		InputStream file = this.indexLocation.getInputStream();
		try (file) {
			file.skip(start);
			if (file.read(this.streamBuffer, 0, numberOfBytes) != numberOfBytes)
				throw new IOException();
		} catch (IOException ioe) {
			this.streamBuffer = null;
			throw ioe;
		} finally {
			this.indexLocation.close();
		}
		int numberOfNames = isLastChunk ? this.sizeOfLastChunk : CHUNK_SIZE;
		chunk = new String[numberOfNames];
		try {
			readChunk(chunk, null, 0, numberOfNames);
		} catch (IOException ioe) {
			this.streamBuffer = null;
			throw ioe;
		}
		this.cachedChunks[chunkNumber] = chunk;
	}
	this.streamBuffer = null;
	return chunk[docNumber - (chunkNumber * CHUNK_SIZE)];
}
synchronized int[] readDocumentNumbers(Object arrayOffset) throws IOException {
	// arrayOffset is either a cached array of docNumbers or an Integer offset in the file
	if (arrayOffset instanceof int[])
		return (int[]) arrayOffset;

	InputStream stream = this.indexLocation.getInputStream();
	try (stream) {
		int offset = ((Integer) arrayOffset).intValue();
		stream.skip(offset);
		this.streamBuffer = new byte[BUFFER_READ_SIZE];
		this.bufferIndex = 0;
		this.bufferEnd = stream.read(this.streamBuffer, 0, this.streamBuffer.length);
		return readStreamDocumentArray(stream, readStreamInt(stream));
	} finally {
		this.indexLocation.close();
		this.streamBuffer = null;
	}
}
private void readHeaderInfo(InputStream stream) throws IOException {

	// must be same order as writeHeaderInfo()
	this.numberOfChunks = readStreamInt(stream);
	this.sizeOfLastChunk = this.streamBuffer[this.bufferIndex++] & 0xFF;
	this.documentReferenceSize = this.streamBuffer[this.bufferIndex++] & 0xFF;
	this.separator = (char) (this.streamBuffer[this.bufferIndex++] & 0xFF);
	long length = this.indexLocation.length();
	if (length != -1 && this.numberOfChunks > length) {
		// not an accurate check, but good enough https://bugs.eclipse.org/bugs/show_bug.cgi?id=350612
		if (DEBUG)
			System.out.println("Index file is corrupted " + this.indexLocation); //$NON-NLS-1$
		throw new IOException("Index file is corrupted " + this.indexLocation); //$NON-NLS-1$
	}
	this.chunkOffsets = new int[this.numberOfChunks];
	for (int i = 0; i < this.numberOfChunks; i++)
		this.chunkOffsets[i] = readStreamInt(stream);

	this.startOfCategoryTables = readStreamInt(stream);

	int size = readStreamInt(stream);
	this.categoryOffsets = new HashtableOfIntValues(size);
	this.categoryEnds = new HashtableOfIntValues(size);
	if (length != -1 && size > length) {
		//  not an accurate check, but good enough  https://bugs.eclipse.org/bugs/show_bug.cgi?id=350612
		if (DEBUG)
			System.out.println("Index file is corrupted " + this.indexLocation); //$NON-NLS-1$
		throw new IOException("Index file is corrupted " + this.indexLocation); //$NON-NLS-1$
	}
	char[] previousCategory = null;
	int offset = -1;
	for (int i = 0; i < size; i++) {
		char[] categoryName = INTERNED_CATEGORY_NAMES.get(readStreamChars(stream));
		offset = readStreamInt(stream);
		this.categoryOffsets.put(categoryName, offset); // cache offset to category table
		if (previousCategory != null) {
			this.categoryEnds.put(previousCategory, offset); // cache end of the category table
		}
		previousCategory = categoryName;
	}
	if (previousCategory != null) {
		this.categoryEnds.put(previousCategory, this.headerInfoOffset); // cache end of the category table
	}
	this.categoryTables = new HashtableOfObject(3);
}
synchronized void startQuery() {
	this.cacheUserCount++;
}
synchronized void stopQuery() {
	if (--this.cacheUserCount < 0) {
		// clear cached items
		this.cacheUserCount = -1;
		this.cachedChunks = null;
		if (this.categoryTables != null) {
			if (this.cachedCategoryName == null) {
				this.categoryTables = null;
			} else if (this.categoryTables.elementSize > 1) {
				HashtableOfObject newTables = new HashtableOfObject(3);
				newTables.put(this.cachedCategoryName, this.categoryTables.get(this.cachedCategoryName));
				this.categoryTables = newTables;
			}
		}
	}
}
private void readStreamBuffer(InputStream stream) throws IOException {
	// if we're about to read a known amount at the end of the existing buffer, but it does not completely fit
	// so we need to shift the remaining bytes to be read, and fill the buffer from the stream
	if (this.bufferEnd < this.streamBuffer.length) {
		if (stream.available() == 0)
			return; // we're at the end of the stream - nothing left to read
	}

	int bytesInBuffer = this.bufferEnd - this.bufferIndex;
	if (bytesInBuffer > 0)
		System.arraycopy(this.streamBuffer, this.bufferIndex, this.streamBuffer, 0, bytesInBuffer);
	this.bufferEnd = bytesInBuffer + stream.read(this.streamBuffer, bytesInBuffer, this.bufferIndex);
	this.bufferIndex = 0;
}
/**
 * Reads in a string from the specified data input stream. The
 * string has been encoded using a modified UTF-8 format.
 * <p>
 * The first two bytes are read as an unsigned short.
 * This value gives the number of following bytes that are in the encoded string,
 * not the length of the resulting string. The following bytes are then
 * interpreted as bytes encoding characters in the UTF-8 format
 * and are converted into characters.
 * <p>
 * This method blocks until all the bytes are read, the end of the
 * stream is detected, or an exception is thrown.
 *
 * @param      stream   a data input stream.
 * @return     UTF decoded string as a char array
 * @exception  EOFException if this end of data input is reached while reading it.
 * @exception  IOException if an I/O error occurs while reading data input.
 * @exception  UTFDataFormatException  if the bytes do not represent a
 *               valid UTF-8 encoding of a Unicode string.
 */
private char[] readStreamChars(InputStream stream) throws IOException {
	// read chars array length
	if (stream != null && this.bufferIndex + 2 >= this.bufferEnd)
		readStreamBuffer(stream);
	int length = (this.streamBuffer[this.bufferIndex++] & 0xFF) << 8;
	length += this.streamBuffer[this.bufferIndex++] & 0xFF;

	// fill the chars from bytes buffer
	char[] word = new char[length];
	int i = 0;
	while (i < length) {
		// how many characters can be decoded without refilling the buffer?
		int charsInBuffer = i + ((this.bufferEnd - this.bufferIndex) / 3);
		// all the characters must already be in the buffer if we're at the end of the stream
		if (charsInBuffer > length || stream == null  || (this.bufferEnd != this.streamBuffer.length && stream.available() == 0))
			charsInBuffer = length;
		{ // optimization for the typical case of pure ASCII chars:
			byte b;
			while (i < charsInBuffer && (b = this.streamBuffer[this.bufferIndex]) >= 0) {
				word[i++] = (char) b;
				this.bufferIndex++;
			}
		}
		while (i < charsInBuffer) {
			byte b = this.streamBuffer[this.bufferIndex++];
			switch (b & 0xF0) {
				case 0x00 :
				case 0x10 :
				case 0x20 :
				case 0x30 :
				case 0x40 :
				case 0x50 :
				case 0x60 :
				case 0x70 :
					word[i++]= (char) b;
					break;
				case 0xC0 :
				case 0xD0 :
					char next = (char) this.streamBuffer[this.bufferIndex++];
					if ((next & 0xC0) != 0x80) {
						throw new UTFDataFormatException();
					}
					char ch = (char) ((b & 0x1F) << 6);
					ch |= next & 0x3F;
					word[i++] = ch;
					break;
				case 0xE0 :
					char first = (char) this.streamBuffer[this.bufferIndex++];
					char second = (char) this.streamBuffer[this.bufferIndex++];
					if ((first & second & 0xC0) != 0x80) {
						throw new UTFDataFormatException();
					}
					ch = (char) ((b & 0x0F) << 12);
					ch |= ((first& 0x3F) << 6);
					ch |= second & 0x3F;
					word[i++] = ch;
					break;
				default:
					throw new UTFDataFormatException();
			}
		}
		if (i < length && stream != null)
			readStreamBuffer(stream);
	}
	return word;
}
private int[] readStreamDocumentArray(InputStream stream, int arraySize) throws IOException {
	int[] indexes = new int[arraySize];
	if (arraySize == 0) return indexes;

	int i = 0;
	switch (this.documentReferenceSize) {
		case 1 :
			while (i < arraySize) {
				// how many bytes without refilling the buffer?
				int bytesInBuffer = i + this.bufferEnd - this.bufferIndex;
				if (bytesInBuffer > arraySize)
					bytesInBuffer = arraySize;
				while (i < bytesInBuffer) {
					indexes[i++] = this.streamBuffer[this.bufferIndex++] & 0xFF;
				}
				if (i < arraySize && stream != null)
					readStreamBuffer(stream);
			}
			break;
		case 2 :
			while (i < arraySize) {
				// how many shorts without refilling the buffer?
				int shortsInBuffer = i + ((this.bufferEnd - this.bufferIndex) / 2);
				if (shortsInBuffer > arraySize)
					shortsInBuffer = arraySize;
				while (i < shortsInBuffer) {
					int val = (this.streamBuffer[this.bufferIndex++] & 0xFF) << 8;
					indexes[i++] = val + (this.streamBuffer[this.bufferIndex++] & 0xFF);
				}
				if (i < arraySize && stream != null)
					readStreamBuffer(stream);
			}
			break;
		default :
			while (i < arraySize) {
				indexes[i++] = readStreamInt(stream);
			}
			break;
	}
	return indexes;
}
private int readStreamInt(InputStream stream) throws IOException {
	if (this.bufferIndex + 4 >= this.bufferEnd) {
		readStreamBuffer(stream);
	}
	int val = (this.streamBuffer[this.bufferIndex++] & 0xFF) << 24;
	val += (this.streamBuffer[this.bufferIndex++] & 0xFF) << 16;
	val += (this.streamBuffer[this.bufferIndex++] & 0xFF) << 8;
	return val + (this.streamBuffer[this.bufferIndex++] & 0xFF);
}
private void writeAllDocumentNames(String[] sortedDocNames, FileOutputStream stream) throws IOException {
	if (sortedDocNames.length == 0)
		throw new IllegalArgumentException();

	// assume the file was just created by initializeFrom()
	this.streamBuffer = new byte[BUFFER_WRITE_SIZE];
	this.bufferIndex = 0;
	this.streamEnd = 0;

	// in order, write: SIGNATURE & headerInfoOffset place holder, then each compressed chunk of document names
	writeStreamChars(stream, SIGNATURE_CHARS);
	this.headerInfoOffset = this.streamEnd;
	writeStreamInt(stream, -1); // will overwrite with correct value later

	int size = sortedDocNames.length;
	this.numberOfChunks = (size / CHUNK_SIZE) + 1;
	this.sizeOfLastChunk = size % CHUNK_SIZE;
	if (this.sizeOfLastChunk == 0) {
		this.numberOfChunks--;
		this.sizeOfLastChunk = CHUNK_SIZE;
	}
	this.documentReferenceSize = size <= 0x7F ? 1 : (size <= 0x7FFF ? 2 : 4); // number of bytes used to encode a reference

	this.chunkOffsets = new int[this.numberOfChunks];
	int lastIndex = this.numberOfChunks - 1;
	for (int i = 0; i < this.numberOfChunks; i++) {
		this.chunkOffsets[i] = this.streamEnd;

		int chunkSize = i == lastIndex ? this.sizeOfLastChunk : CHUNK_SIZE;
		int chunkIndex = i * CHUNK_SIZE;
		String current = sortedDocNames[chunkIndex];
		writeStreamChars(stream, current.toCharArray());
		for (int j = 1; j < chunkSize; j++) {
			String next = sortedDocNames[chunkIndex + j];
			int len1 = current.length();
			int len2 = next.length();
			int max = len1 < len2 ? len1 : len2;
			int start = 0; // number of identical characters at the beginning (also the index of first character that is different)
			while (current.charAt(start) == next.charAt(start)) {
				start++;
				if (max == start) break; // current is 'abba', next is 'abbab'
			}
			if (start > 255) start = 255;

			int end = 0; // number of identical characters at the end
			while (current.charAt(--len1) == next.charAt(--len2)) {
				end++;
				if (len2 == start) break; // current is 'abbba', next is 'abba'
				if (len1 == 0) break; // current is 'xabc', next is 'xyabc'
			}
			if (end > 255) end = 255;
			if ((this.bufferIndex + 2) >= BUFFER_WRITE_SIZE)  {
				stream.write(this.streamBuffer, 0, this.bufferIndex);
				this.bufferIndex = 0;
			}
			this.streamBuffer[this.bufferIndex++] = (byte) start;
			this.streamBuffer[this.bufferIndex++] = (byte) end;
			this.streamEnd += 2;

			int last = next.length() - end;
			writeStreamChars(stream, (start < last ? CharOperation.subarray(next.toCharArray(), start, last) : CharOperation.NO_CHAR));
			current = next;
		}
	}
	this.startOfCategoryTables = this.streamEnd + 1;
}
private void writeCategories(FileOutputStream stream) throws IOException {
	char[][] categoryNames = this.categoryTables.keyTable;
	Object[] tables = this.categoryTables.valueTable;
	for (int i = 0, l = categoryNames.length; i < l; i++)
		if (categoryNames[i] != null)
			writeCategoryTable(categoryNames[i], (HashtableOfObject) tables[i], stream);
	this.categoryTables = null;
}
private void writeCategoryTable(char[] categoryName, HashtableOfObject wordsToDocs, FileOutputStream stream) throws IOException {
	// the format of a category table is as follows:
	// any document number arrays with >= 256 elements are written before the table (the offset to each array is remembered)
	// then the number of word->int[] pairs in the table is written
	// for each word -> int[] pair, the word is written followed by:
	//		an int <= 0 if the array size == 1
	//		an int > 1 & < 256 for the size of the array if its > 1 & < 256, the document array follows immediately
	//		256 if the array size >= 256 followed by another int which is the offset to the array (written prior to the table)

	int largeArraySize = 256;
	Object[] values = wordsToDocs.valueTable;
	for (int i = 0, l = values.length; i < l; i++) {
		Object o = values[i];
		if (o != null) {
			if (o instanceof IntList)
				o = values[i] = ((IntList) values[i]).asArray();
			int[] documentNumbers = (int[]) o;
			if (documentNumbers.length >= largeArraySize) {
				values[i] = Integer.valueOf(this.streamEnd);
				writeDocumentNumbers(documentNumbers, stream);
			}
		}
	}

	this.categoryOffsets.put(categoryName, this.streamEnd); // remember the offset to the start of the table
	this.categoryTables.put(categoryName, null); // flush cached table
	writeStreamInt(stream, wordsToDocs.elementSize);
	char[][] words = wordsToDocs.keyTable;
	for (int i = 0, l = words.length; i < l; i++) {
		Object o = values[i];
		if (o != null) {
			writeStreamChars(stream, words[i]);
			if (o instanceof int[]) {
				int[] documentNumbers = (int[]) o;
				if (documentNumbers.length == 1)
					writeStreamInt(stream, -documentNumbers[0]); // store an array of 1 element by negating the documentNumber (can be zero)
				else
					writeDocumentNumbers(documentNumbers, stream);
			} else {
				writeStreamInt(stream, largeArraySize); // mark to identify that an offset follows
				writeStreamInt(stream, ((Integer) o).intValue()); // offset in the file of the array of document numbers
			}
		}
	}
}
private void writeDocumentNumbers(int[] documentNumbers, FileOutputStream stream) throws IOException {
	// must store length as a positive int to detect in-lined array of 1 element
	int length = documentNumbers.length;
	writeStreamInt(stream, length);
	Util.sort(documentNumbers);
	int start = 0;
	switch (this.documentReferenceSize) {
		case 1 :
			while ((this.bufferIndex + length - start) >= BUFFER_WRITE_SIZE) {
				// when documentNumbers is large, write BUFFER_WRITE_SIZE parts & fall thru to write the last part
				int bytesLeft = BUFFER_WRITE_SIZE - this.bufferIndex;
				for (int i=0; i < bytesLeft; i++) {
					this.streamBuffer[this.bufferIndex++] = (byte) documentNumbers[start++];
				}
				stream.write(this.streamBuffer, 0, this.bufferIndex);
				this.bufferIndex = 0;
			}
			while (start < length) {
				this.streamBuffer[this.bufferIndex++] = (byte) documentNumbers[start++];
			}
			this.streamEnd += length;
			break;
		case 2 :
			while ((this.bufferIndex + ((length - start) * 2)) >= BUFFER_WRITE_SIZE) {
				// when documentNumbers is large, write BUFFER_WRITE_SIZE parts & fall thru to write the last part
				int shortsLeft = (BUFFER_WRITE_SIZE - this.bufferIndex) / 2;
				for (int i=0; i < shortsLeft; i++) {
					this.streamBuffer[this.bufferIndex++] = (byte) (documentNumbers[start] >> 8);
					this.streamBuffer[this.bufferIndex++] = (byte) documentNumbers[start++];
				}
				stream.write(this.streamBuffer, 0, this.bufferIndex);
				this.bufferIndex = 0;
			}
			while (start < length) {
				this.streamBuffer[this.bufferIndex++] = (byte) (documentNumbers[start] >> 8);
				this.streamBuffer[this.bufferIndex++] = (byte) documentNumbers[start++];
			}
			this.streamEnd += length * 2;
			break;
		default :
			while (start < length) {
				writeStreamInt(stream, documentNumbers[start++]);
			}
			break;
	}
}
private void writeHeaderInfo(FileOutputStream stream) throws IOException {
	writeStreamInt(stream, this.numberOfChunks);
	if ((this.bufferIndex + 3) >= BUFFER_WRITE_SIZE)  {
		stream.write(this.streamBuffer, 0, this.bufferIndex);
		this.bufferIndex = 0;
	}
	this.streamBuffer[this.bufferIndex++] = (byte) this.sizeOfLastChunk;
	this.streamBuffer[this.bufferIndex++] = (byte) this.documentReferenceSize;
	this.streamBuffer[this.bufferIndex++] = (byte) this.separator;
	this.streamEnd += 3;

	// apend the file with chunk offsets
	for (int i = 0; i < this.numberOfChunks; i++) {
		writeStreamInt(stream, this.chunkOffsets[i]);
	}

	writeStreamInt(stream, this.startOfCategoryTables);

	// append the file with the category offsets... # of name -> offset pairs, followed by each name & an offset to its word->doc# table
	writeStreamInt(stream, this.categoryOffsets.elementSize);
	char[][] categoryNames = this.categoryOffsets.keyTable;
	int[] offsets = this.categoryOffsets.valueTable;
	for (int i = 0, l = categoryNames.length; i < l; i++) {
		if (categoryNames[i] != null) {
			writeStreamChars(stream, categoryNames[i]);
			writeStreamInt(stream, offsets[i]);
		}
	}
	// ensure buffer is written to the stream
	if (this.bufferIndex > 0) {
		stream.write(this.streamBuffer, 0, this.bufferIndex);
		this.bufferIndex = 0;
	}
}
private void writeOffsetToHeader(int offsetToHeader) throws IOException {
	if (offsetToHeader > 0) {
		RandomAccessFile file = new RandomAccessFile(this.indexLocation.getIndexFile(), "rw"); //$NON-NLS-1$
		try (file) {
			file.seek(this.headerInfoOffset); // offset to position in header
			file.writeInt(offsetToHeader);
			this.headerInfoOffset = offsetToHeader; // update to reflect the correct offset
		}
	}
}
/**
 * Writes a string to the given output stream using UTF-8
 * encoding in a machine-independent manner.
 * <p>
 * First, two bytes of the array are giving the number of bytes to
 * follow. This value is the number of bytes actually written out,
 * not the length of the string. Following the length, each character
 * of the string is put in the bytes array, in sequence, using the UTF-8
 * encoding for the character.
 * </p>
 * <p>
 * Then the entire byte array is written to the output stream
 * using {@link OutputStream#write(byte[], int, int)} method.
 * </p>
 *
 * @param array char array to be written.
 * @exception  IOException  if an I/O error occurs while writting
 * 	the bytes array to the stream.
 */
private void writeStreamChars(FileOutputStream stream, char[] array) throws IOException {
	if ((this.bufferIndex + 2) >= BUFFER_WRITE_SIZE)  {
		stream.write(this.streamBuffer, 0, this.bufferIndex);
		this.bufferIndex = 0;
	}
	int length = array.length;
	this.streamBuffer[this.bufferIndex++] = (byte) ((length >>> 8) & 0xFF); // store chars array length instead of bytes
	this.streamBuffer[this.bufferIndex++] = (byte) (length & 0xFF); // this will allow to read it faster
	this.streamEnd += 2;

	// we're assuming that very few char[] are so large that we need to flush the buffer more than once, if at all
	int totalBytesNeeded = length * 3;
	if (totalBytesNeeded <= BUFFER_WRITE_SIZE) {
		if (this.bufferIndex + totalBytesNeeded > BUFFER_WRITE_SIZE) {
			// flush the buffer now to make sure there is room for the array
			stream.write(this.streamBuffer, 0, this.bufferIndex);
			this.bufferIndex = 0;
		}
		writeStreamChars(stream, array, 0, length);
	} else {
		int charsPerWrite = BUFFER_WRITE_SIZE / 3;
		int start = 0;
		while (start < length) {
			stream.write(this.streamBuffer, 0, this.bufferIndex);
			this.bufferIndex = 0;
			int charsLeftToWrite = length - start;
			int end = start + (charsPerWrite < charsLeftToWrite ? charsPerWrite : charsLeftToWrite);
			writeStreamChars(stream, array, start, end);
			start = end;
		}
	}
}
private void writeStreamChars(FileOutputStream stream, char[] array, int start, int end) throws IOException {
	// start can NOT be == end
	// must have checked that there is enough room for end - start * 3 bytes in the buffer

	int oldIndex = this.bufferIndex;
	while (start < end) {
		int ch = array[start++];
		if ((ch & 0x007F) == ch) {
			this.streamBuffer[this.bufferIndex++] = (byte) ch;
		} else if ((ch & 0x07FF) == ch) {
			// first two bits are stored in first byte
			byte b = (byte) (ch >> 6);
			b &= 0x1F;
			b |= 0xC0;
			this.streamBuffer[this.bufferIndex++] = b;
			// last six bits are stored in second byte
			b = (byte) (ch & 0x3F);
			b |= 0x80;
			this.streamBuffer[this.bufferIndex++] = b;
		} else {
			// first four bits are stored in first byte
			byte b = (byte) (ch >> 12);
			b &= 0x0F;
			b |= 0xE0;
			this.streamBuffer[this.bufferIndex++] = b;
			// six following bits are stored in second byte
			b = (byte) (ch >> 6);
			b &= 0x3F;
			b |= 0x80;
			this.streamBuffer[this.bufferIndex++] = b;
			// last six bits are stored in third byte
			b = (byte) (ch & 0x3F);
			b |= 0x80;
			this.streamBuffer[this.bufferIndex++] = b;
		}
	}
	this.streamEnd += this.bufferIndex - oldIndex;
}
private void writeStreamInt(FileOutputStream stream, int val) throws IOException {
	if ((this.bufferIndex + 4) >= BUFFER_WRITE_SIZE)  {
		stream.write(this.streamBuffer, 0, this.bufferIndex);
		this.bufferIndex = 0;
	}
	this.streamBuffer[this.bufferIndex++] = (byte) (val >> 24);
	this.streamBuffer[this.bufferIndex++] = (byte) (val >> 16);
	this.streamBuffer[this.bufferIndex++] = (byte) (val >> 8);
	this.streamBuffer[this.bufferIndex++] = (byte) val;
	this.streamEnd += 4;
}

synchronized int getCacheUserCount() {
	return this.cacheUserCount;
}
}
