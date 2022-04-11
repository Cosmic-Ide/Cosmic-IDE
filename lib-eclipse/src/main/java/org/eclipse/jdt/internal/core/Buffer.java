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
 *     Tim Hanson (thanson@bea.com) - patch for https://bugs.eclipse.org/bugs/show_bug.cgi?id=126673
 *******************************************************************************/
package org.eclipse.jdt.internal.core;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.internal.core.util.Util;

/**
 * @see IBuffer
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class Buffer implements IBuffer {
	protected IFile file;
	protected int flags;
	protected char[] contents;
	protected ListenerList<IBufferChangedListener> changeListeners;
	protected IOpenable owner;
	protected int gapStart = -1;
	protected int gapEnd = -1;

	protected Object lock = new Object();

	protected static final int F_HAS_UNSAVED_CHANGES = 1;
	protected static final int F_IS_READ_ONLY = 2;
	protected static final int F_IS_CLOSED = 4;

/**
 * Creates a new buffer on an underlying resource.
 */
protected Buffer(IFile file, IOpenable owner, boolean readOnly) {
	this.file = file;
	this.owner = owner;
	if (file == null) {
		setReadOnly(readOnly);
	}
}
/**
 * @see IBuffer
 */
@Override
public synchronized void addBufferChangedListener(IBufferChangedListener listener) {
	if (this.changeListeners == null) {
		this.changeListeners = new ListenerList();
	}
	this.changeListeners.add(listener);
}
/**
 * Append the <code>text</code> to the actual content, the gap is moved
 * to the end of the <code>text</code>.
 */
@Override
public void append(char[] text) {
	if (!isReadOnly()) {
		if (text == null || text.length == 0) {
			return;
		}
		int length = getLength();
		synchronized(this.lock) {
			if (this.contents == null) return;
			moveAndResizeGap(length, text.length);
			System.arraycopy(text, 0, this.contents, length, text.length);
			this.gapStart += text.length;
			this.flags |= F_HAS_UNSAVED_CHANGES;
		}
		notifyChanged(new BufferChangedEvent(this, length, 0, new String(text)));
	}
}
/**
 * Append the <code>text</code> to the actual content, the gap is moved
 * to the end of the <code>text</code>.
 */
@Override
public void append(String text) {
	if (text == null) {
		return;
	}
	this.append(text.toCharArray());
}
/**
 * @see IBuffer
 */
@Override
public void close() {
	BufferChangedEvent event = null;
	synchronized (this.lock) {
		if (isClosed())
			return;
		event = new BufferChangedEvent(this, 0, 0, null);
		this.contents = null;
		this.flags |= F_IS_CLOSED;
	}
	notifyChanged(event); // notify outside of synchronized block
	synchronized(this) { // ensure that no other thread is adding/removing a listener at the same time (https://bugs.eclipse.org/bugs/show_bug.cgi?id=126673)
		this.changeListeners = null;
	}
}
/**
 * @see IBuffer
 */
@Override
public char getChar(int position) {
	synchronized (this.lock) {
	    if (this.contents == null) return Character.MIN_VALUE;
		if (position < this.gapStart) {
			return this.contents[position];
		}
		int gapLength = this.gapEnd - this.gapStart;
		return this.contents[position + gapLength];
	}
}
/**
 * @see IBuffer
 */
@Override
public char[] getCharacters() {
	synchronized (this.lock) {
		if (this.contents == null) return null;
		if (this.gapStart < 0) {
			return this.contents;
		}
		int length = this.contents.length;
		char[] newContents = new char[length - this.gapEnd + this.gapStart];
		System.arraycopy(this.contents, 0, newContents, 0, this.gapStart);
		System.arraycopy(this.contents, this.gapEnd, newContents, this.gapStart, length - this.gapEnd);
		return newContents;
	}
}
/**
 * @see IBuffer
 */
@Override
public String getContents() {
	char[] chars = getCharacters();
	if (chars == null) return null;
	return new String(chars);
}
/**
 * @see IBuffer
 */
@Override
public int getLength() {
	synchronized (this.lock) {
		if (this.contents == null) return -1;
		int length = this.gapEnd - this.gapStart;
		return (this.contents.length - length);
	}
}
/**
 * @see IBuffer
 */
@Override
public IOpenable getOwner() {
	return this.owner;
}
/**
 * @see IBuffer
 */
@Override
public String getText(int offset, int length) {
	synchronized (this.lock) {
		if (this.contents == null) return ""; //$NON-NLS-1$
		if (offset + length < this.gapStart)
			return new String(this.contents, offset, length);
		if (this.gapStart < offset) {
			int gapLength = this.gapEnd - this.gapStart;
			return new String(this.contents, offset + gapLength, length);
		}
		StringBuilder buf = new StringBuilder();
		buf.append(this.contents, offset, this.gapStart - offset);
		buf.append(this.contents, this.gapEnd, offset + length - this.gapStart);
		return buf.toString();
	}
}
/**
 * @see IBuffer
 */
@Override
public IResource getUnderlyingResource() {
	return this.file;
}
/**
 * @see IBuffer
 */
@Override
public boolean hasUnsavedChanges() {
	return (this.flags & F_HAS_UNSAVED_CHANGES) != 0;
}
/**
 * @see IBuffer
 */
@Override
public boolean isClosed() {
	return (this.flags & F_IS_CLOSED) != 0;
}
/**
 * @see IBuffer
 */
@Override
public boolean isReadOnly() {
	return (this.flags & F_IS_READ_ONLY) != 0;
}
/**
 * Moves the gap to location and adjust its size to the
 * anticipated change size. The size represents the expected
 * range of the gap that will be filled after the gap has been moved.
 * Thus the gap is resized to actual size + the specified size and
 * moved to the given position.
 */
protected void moveAndResizeGap(int position, int size) {
	char[] content = null;
	int oldSize = this.gapEnd - this.gapStart;
	if (size < 0) {
		if (oldSize > 0) {
			content = new char[this.contents.length - oldSize];
			System.arraycopy(this.contents, 0, content, 0, this.gapStart);
			System.arraycopy(this.contents, this.gapEnd, content, this.gapStart, content.length - this.gapStart);
			this.contents = content;
		}
		this.gapStart = this.gapEnd = position;
		return;
	}
	content = new char[this.contents.length + (size - oldSize)];
	int newGapStart = position;
	int newGapEnd = newGapStart + size;
	if (oldSize == 0) {
		System.arraycopy(this.contents, 0, content, 0, newGapStart);
		System.arraycopy(this.contents, newGapStart, content, newGapEnd, content.length - newGapEnd);
	} else
		if (newGapStart < this.gapStart) {
			int delta = this.gapStart - newGapStart;
			System.arraycopy(this.contents, 0, content, 0, newGapStart);
			System.arraycopy(this.contents, newGapStart, content, newGapEnd, delta);
			System.arraycopy(this.contents, this.gapEnd, content, newGapEnd + delta, this.contents.length - this.gapEnd);
		} else {
			int delta = newGapStart - this.gapStart;
			System.arraycopy(this.contents, 0, content, 0, this.gapStart);
			System.arraycopy(this.contents, this.gapEnd, content, this.gapStart, delta);
			System.arraycopy(this.contents, this.gapEnd + delta, content, newGapEnd, content.length - newGapEnd);
		}
	this.contents = content;
	this.gapStart = newGapStart;
	this.gapEnd = newGapEnd;
}
/**
 * Notify the listeners that this buffer has changed.
 * To avoid deadlock, this should not be called in a synchronized block.
 */
protected void notifyChanged(final BufferChangedEvent event) {
	ListenerList<IBufferChangedListener> listeners = this.changeListeners;
	if (listeners != null) {
		Iterator<IBufferChangedListener> iterator = listeners.iterator();
		while (iterator.hasNext()) {
			final IBufferChangedListener listener = iterator.next();
			SafeRunner.run(new ISafeRunnable() {
				@Override
				public void handleException(Throwable exception) {
					Util.log(exception, "Exception occurred in listener of buffer change notification"); //$NON-NLS-1$
				}
				@Override
				public void run() throws Exception {
					listener.bufferChanged(event);
				}
			});
		}
	}
}
/**
 * @see IBuffer
 */
@Override
public synchronized void removeBufferChangedListener(IBufferChangedListener listener) {
	if (this.changeListeners != null) {
		this.changeListeners.remove(listener);
		if (this.changeListeners.size() == 0) {
			this.changeListeners = null;
		}
	}
}
/**
 * Replaces <code>length</code> characters starting from <code>position</code> with <code>text<code>.
 * After that operation, the gap is placed at the end of the
 * inserted <code>text</code>.
 */
@Override
public void replace(int position, int length, char[] text) {
	if (!isReadOnly()) {
		int textLength = text == null ? 0 : text.length;
		synchronized (this.lock) {
			if (this.contents == null) return;

			// move gap
			moveAndResizeGap(position + length, textLength - length);

			// overwrite
			int min = Math.min(textLength, length);
			if (min > 0) {
				System.arraycopy(text, 0, this.contents, position, min);
			}
			if (length > textLength) {
				// enlarge the gap
				this.gapStart -= length - textLength;
			} else if (textLength > length) {
				// shrink gap
				this.gapStart += textLength - length;
				System.arraycopy(text, 0, this.contents, position, textLength);
			}
			this.flags |= F_HAS_UNSAVED_CHANGES;
		}
		String string = null;
		if (textLength > 0) {
			string = new String(text);
		}
		notifyChanged(new BufferChangedEvent(this, position, length, string));
	}
}
/**
 * Replaces <code>length</code> characters starting from <code>position</code> with <code>text<code>.
 * After that operation, the gap is placed at the end of the
 * inserted <code>text</code>.
 */
@Override
public void replace(int position, int length, String text) {
	this.replace(position, length, text == null ? null : text.toCharArray());
}
/**
 * @see IBuffer
 */
@Override
public void save(IProgressMonitor progress, boolean force) throws JavaModelException {

	// determine if saving is required
	if (isReadOnly() || this.file == null) {
		return;
	}
	if (!hasUnsavedChanges())
		return;

	// use a platform operation to update the resource contents
	try {
		String stringContents = getContents();
		if (stringContents == null) return;

		// Get encoding
		String encoding = null;
		try {
			encoding = this.file.getCharset();
		}
		catch (CoreException ce) {
			// use no encoding
		}

		// Create bytes array
		byte[] bytes = encoding == null
			? stringContents.getBytes()
			: stringContents.getBytes(encoding);

		// Special case for UTF-8 BOM files
		// see bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=110576
		if (encoding != null && encoding.equals(org.eclipse.jdt.internal.compiler.util.Util.UTF_8)) {
			IContentDescription description;
			try {
				description = this.file.getContentDescription();
			} catch (CoreException e) {
				if (e.getStatus().getCode() != IResourceStatus.RESOURCE_NOT_FOUND)
					throw e;
				// file no longer exist (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=234307 )
				description = null;
			}
			if (description != null && description.getProperty(IContentDescription.BYTE_ORDER_MARK) != null) {
				int bomLength= IContentDescription.BOM_UTF_8.length;
				byte[] bytesWithBOM= new byte[bytes.length + bomLength];
				System.arraycopy(IContentDescription.BOM_UTF_8, 0, bytesWithBOM, 0, bomLength);
				System.arraycopy(bytes, 0, bytesWithBOM, bomLength, bytes.length);
				bytes= bytesWithBOM;
			}
		}

		// Set file contents
		ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
		if (this.file.exists()) {
			this.file.setContents(
				stream,
				force ? IResource.FORCE | IResource.KEEP_HISTORY : IResource.KEEP_HISTORY,
				null);
		} else {
			this.file.create(stream, force, null);
		}
	} catch (IOException e) {
		throw new JavaModelException(e, IJavaModelStatusConstants.IO_EXCEPTION);
	} catch (CoreException e) {
		throw new JavaModelException(e);
	}

	// the resource no longer has unsaved changes
	this.flags &= ~ (F_HAS_UNSAVED_CHANGES);
}
/**
 * @see IBuffer
 */
@Override
public void setContents(char[] newContents) {
	// allow special case for first initialization
	// after creation by buffer factory
	if (this.contents == null) {
		synchronized (this.lock) {
			this.contents = newContents;
			this.flags &= ~ (F_HAS_UNSAVED_CHANGES);
		}
		return;
	}

	if (!isReadOnly()) {
		String string = null;
		if (newContents != null) {
			string = new String(newContents);
		}
		synchronized (this.lock) {
			if (this.contents == null) return; // ignore if buffer is closed (as per spec)
			this.contents = newContents;
			this.flags |= F_HAS_UNSAVED_CHANGES;
			this.gapStart = -1;
			this.gapEnd = -1;
		}
		BufferChangedEvent event = new BufferChangedEvent(this, 0, getLength(), string);
		notifyChanged(event);
	}
}
/**
 * @see IBuffer
 */
@Override
public void setContents(String newContents) {
	this.setContents(newContents.toCharArray());
}
/**
 * Sets this <code>Buffer</code> to be read only.
 */
protected void setReadOnly(boolean readOnly) {
	if (readOnly) {
		this.flags |= F_IS_READ_ONLY;
	} else {
		this.flags &= ~(F_IS_READ_ONLY);
	}
}
@Override
public String toString() {
	StringBuilder buffer = new StringBuilder();
	buffer.append("Owner: " + ((JavaElement)this.owner).toStringWithAncestors()); //$NON-NLS-1$
	buffer.append("\nHas unsaved changes: " + hasUnsavedChanges()); //$NON-NLS-1$
	buffer.append("\nIs readonly: " + isReadOnly()); //$NON-NLS-1$
	buffer.append("\nIs closed: " + isClosed()); //$NON-NLS-1$
	buffer.append("\nContents:\n"); //$NON-NLS-1$
	char[] charContents = getCharacters();
	if (charContents == null) {
		buffer.append("<null>"); //$NON-NLS-1$
	} else {
		int length = charContents.length;
		for (int i = 0; i < length; i++) {
			char c = charContents[i];
			switch (c) {
				case '\n':
					buffer.append("\\n\n"); //$NON-NLS-1$
					break;
				case '\r':
					if (i < length-1 && this.contents[i+1] == '\n') {
						buffer.append("\\r\\n\n"); //$NON-NLS-1$
						i++;
					} else {
						buffer.append("\\r\n"); //$NON-NLS-1$
					}
					break;
				default:
					buffer.append(c);
					break;
			}
		}
	}
	return buffer.toString();
}
}
