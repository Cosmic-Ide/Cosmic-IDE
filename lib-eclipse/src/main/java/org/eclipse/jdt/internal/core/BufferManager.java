/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
package org.eclipse.jdt.internal.core;

import java.text.NumberFormat;
import java.util.Enumeration;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.IOpenable;

/**
 * The buffer manager manages the set of open buffers.
 * It implements an LRU cache of buffers.
 */
public class BufferManager {

	protected static BufferManager DEFAULT_BUFFER_MANAGER;
	protected static boolean VERBOSE;

	/**
	 * LRU cache of buffers. The key and value for an entry
	 * in the table is the identical buffer.
	 */
	private BufferCache<IOpenable> openBuffers = new BufferCache<>(60);

	/**
	 * @deprecated
	 */
	protected org.eclipse.jdt.core.IBufferFactory defaultBufferFactory = new org.eclipse.jdt.core.IBufferFactory() {
	    /**
	     * @deprecated
	     */
		@Override
		public IBuffer createBuffer(IOpenable owner) {
			return BufferManager.createBuffer(owner);
		}
	};

/**
 * Adds a buffer to the table of open buffers.
 */
protected void addBuffer(IBuffer buffer) {
	if (VERBOSE) {
		String owner = ((Openable)buffer.getOwner()).toStringWithAncestors();
		System.out.println("Adding buffer for " + owner); //$NON-NLS-1$
	}
	synchronized (this.openBuffers) {
		this.openBuffers.put(buffer.getOwner(), buffer);
	}
	// close buffers that were removed from the cache if space was needed
	this.openBuffers.closeBuffers();
	if (VERBOSE) {
		System.out.println("-> Buffer cache filling ratio = " + NumberFormat.getInstance().format(this.openBuffers.fillingRatio()) + "%"); //$NON-NLS-1$//$NON-NLS-2$
	}
}
public static IBuffer createBuffer(IOpenable owner) {
	JavaElement element = (JavaElement) owner;
	IResource resource = element.resource();
	return
		new Buffer(
			resource instanceof IFile ? (IFile)resource : null,
			owner,
			element.isReadOnly());
}
public static IBuffer createNullBuffer(IOpenable owner) {
	JavaElement element = (JavaElement) owner;
	IResource resource = element.resource();
	return
		new NullBuffer(
			resource instanceof IFile ? (IFile)resource : null,
			owner,
			element.isReadOnly());
}
/**
 * Returns the open buffer associated with the given owner,
 * or <code>null</code> if the owner does not have an open
 * buffer associated with it.
 */
public IBuffer getBuffer(IOpenable owner) {
	synchronized (this.openBuffers) {
		return this.openBuffers.get(owner);
	}
}
/**
 * Returns the default buffer manager.
 */
public synchronized static BufferManager getDefaultBufferManager() {
	if (DEFAULT_BUFFER_MANAGER == null) {
		DEFAULT_BUFFER_MANAGER = new BufferManager();
	}
	return DEFAULT_BUFFER_MANAGER;
}
/**
 * Returns the default buffer factory.
 * @deprecated
 */
public org.eclipse.jdt.core.IBufferFactory getDefaultBufferFactory() {
	return this.defaultBufferFactory;
}
/**
 * Returns an enumeration of all open buffers.
 * <p>
 * The <code>Enumeration</code> answered is thread safe.
 *
 * @see OverflowingLRUCache
 * @return Enumeration of IBuffer
 */
public Enumeration<IBuffer> getOpenBuffers() {
	Enumeration<IBuffer> result;
	synchronized (this.openBuffers) {
		this.openBuffers.shrink();
		result = this.openBuffers.elements();
	}
	// close buffers that were removed from the cache if space was needed
	this.openBuffers.closeBuffers();
	return result;
}

/**
 * Removes a buffer from the table of open buffers.
 */
protected void removeBuffer(IBuffer buffer) {
	if (VERBOSE) {
		String owner = ((Openable)buffer.getOwner()).toStringWithAncestors();
		System.out.println("Removing buffer for " + owner); //$NON-NLS-1$
	}
	synchronized (this.openBuffers) {
		this.openBuffers.remove(buffer.getOwner());
	}
	// close buffers that were removed from the cache (should be only one)
	this.openBuffers.closeBuffers();
	if (VERBOSE) {
		System.out.println("-> Buffer cache filling ratio = " + NumberFormat.getInstance().format(this.openBuffers.fillingRatio()) + "%"); //$NON-NLS-1$//$NON-NLS-2$
	}
}
}
