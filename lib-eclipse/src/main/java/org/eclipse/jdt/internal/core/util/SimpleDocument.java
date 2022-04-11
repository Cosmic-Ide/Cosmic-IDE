/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
package org.eclipse.jdt.internal.core.util;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.IDocumentPartitioningListener;
import org.eclipse.jface.text.IPositionUpdater;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.Position;

/**
 * Minimal implementation of IDocument to apply text edit onto a string.
 */
public class SimpleDocument implements IDocument {

	private StringBuffer buffer;


	public SimpleDocument(String source) {
		this.buffer = new StringBuffer(source);
	}

	@Override
	public char getChar(int offset) {
		return this.buffer.charAt(offset);
	}

	@Override
	public int getLength() {
		return this.buffer.length();
	}

	@Override
	public String get() {
		return this.buffer.toString();
	}

	@Override
	public String get(int offset, int length) {
		return this.buffer.substring(offset, offset + length);
	}

	@Override
	public void set(String text) {
		// defining interface method
	}

	@Override
	public void replace(int offset, int length, String text) {

		this.buffer.replace(offset, offset + length, text);
	}

	@Override
	public void addDocumentListener(IDocumentListener listener) {
		// defining interface method
	}

	@Override
	public void removeDocumentListener(IDocumentListener listener) {
		// defining interface method
	}

	@Override
	public void addPrenotifiedDocumentListener(IDocumentListener documentAdapter) {
		// defining interface method
	}

	@Override
	public void removePrenotifiedDocumentListener(IDocumentListener documentAdapter) {
		// defining interface method
	}

	@Override
	public void addPositionCategory(String category) {
		// defining interface method
	}

	@Override
	public void removePositionCategory(String category) {
			// defining interface method
	}

	@Override
	public String[] getPositionCategories() {
		// defining interface method
		return null;
	}

	@Override
	public boolean containsPositionCategory(String category) {
		// defining interface method
		return false;
	}

	@Override
	public void addPosition(Position position) {
		// defining interface method
	}

	@Override
	public void removePosition(Position position) {
		// defining interface method
	}

	@Override
	public void addPosition(String category, Position position) {
		// defining interface method
	}

	@Override
	public void removePosition(String category, Position position) {
		// defining interface method
	}

	@Override
	public Position[] getPositions(String category) {
		// defining interface method
		return null;
	}

	@Override
	public boolean containsPosition(String category, int offset, int length) {
		// defining interface method
		return false;
	}

	@Override
	public int computeIndexInCategory(String category, int offset) {
		// defining interface method
		return 0;
	}

	@Override
	public void addPositionUpdater(IPositionUpdater updater) {
		// defining interface method
	}

	@Override
	public void removePositionUpdater(IPositionUpdater updater) {
		// defining interface method
	}

	@Override
	public void insertPositionUpdater(IPositionUpdater updater, int index) {
		// defining interface method
	}

	@Override
	public IPositionUpdater[] getPositionUpdaters() {
		// defining interface method
		return null;
	}

	@Override
	public String[] getLegalContentTypes() {
		// defining interface method
		return null;
	}

	@Override
	public String getContentType(int offset) {
		// defining interface method
		return null;
	}

	@Override
	public ITypedRegion getPartition(int offset) {
		// defining interface method
		return null;
	}

	@Override
	public ITypedRegion[] computePartitioning(int offset, int length) {
		// defining interface method
		return null;
	}

	@Override
	public void addDocumentPartitioningListener(IDocumentPartitioningListener listener) {
		// defining interface method
	}

	@Override
	public void removeDocumentPartitioningListener(IDocumentPartitioningListener listener) {
		// defining interface method
	}

	@Override
	public void setDocumentPartitioner(IDocumentPartitioner partitioner) {
		// defining interface method
	}

	@Override
	public IDocumentPartitioner getDocumentPartitioner() {
		// defining interface method
		return null;
	}

	@Override
	public int getLineLength(int line) {
		// defining interface method
		return 0;
	}

	@Override
	public int getLineOfOffset(int offset) {
		// defining interface method
		return 0;
	}

	@Override
	public int getLineOffset(int line) {
		// defining interface method
		return 0;
	}

	@Override
	public IRegion getLineInformation(int line) {
		// defining interface method
		return null;
	}

	@Override
	public IRegion getLineInformationOfOffset(int offset) {
		// defining interface method
		return null;
	}

	@Override
	public int getNumberOfLines() {
		// defining interface method
		return 0;
	}

	@Override
	public int getNumberOfLines(int offset, int length) {
		// defining interface method
		return 0;
	}

	@Override
	public int computeNumberOfLines(String text) {
		// defining interface method
		return 0;
	}

	@Override
	public String[] getLegalLineDelimiters() {
		// defining interface method
		return null;
	}

	@Override
	public String getLineDelimiter(int line) {
		// defining interface method
		return null;
	}

	/**
	 * @see org.eclipse.jface.text.IDocument#search(int, java.lang.String, boolean, boolean, boolean)
	 * @deprecated
	 */
	@Override
	public int search(
		int startOffset,
		String findString,
		boolean forwardSearch,
		boolean caseSensitive,
		boolean wholeWord) {
		// defining interface method
		return 0;
	}

}
