/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation and others.
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;

/**
 * The location of the index files are represented as {@link IndexLocation}
 *
 * This is an abstract class to allow different implementation for a jar entry and a file
 * on the disk. Some of these functions could mean different for a jar entry or a file
 *
 */
public abstract class IndexLocation {

	public static IndexLocation createIndexLocation(URL url) {
		URL localUrl;
		try {
			localUrl = FileLocator.resolve(url);
		} catch (IOException e) {
			return null;
		}
		if (localUrl.getProtocol().equals("file")) { //$NON-NLS-1$
			File localFile = null;
			try {
				URI localFileURI = new URI(localUrl.toExternalForm());
				localFile = new File(localFileURI);
			}
			catch(Exception ex) {
				localFile = new File(localUrl.getPath());
			}
			return new FileIndexLocation(url, localFile);
		}
		return new JarIndexLocation(url, localUrl);
	}

	private final URL url; // url of the given index location

	/**
	 * Set to true if this index location is of an index file specified
	 * by a participant through
	 * {@link org.eclipse.jdt.core.search.SearchParticipant#scheduleDocumentIndexing}
	 */
	protected boolean participantIndex;

	protected IndexLocation(File file) {
		URL tempUrl = null;
		try {
			tempUrl = file.toURI().toURL();
		} catch (MalformedURLException e) {
			// should not happen
		}
		this.url = tempUrl;
	}

	public IndexLocation(URL url) {
		this.url = url;
	}

	/**
	 * Closes any open streams.
	 */
	public void close() {
		// default nothing to do
	}

	/**
	 * Creates a new file for the given index location
	 * @return true if the file is created
	 * @throws IOException
	 */
	public abstract boolean createNewFile() throws IOException;

	public abstract boolean delete();

	public abstract boolean exists();

	public abstract String fileName();

	/**
	 * @return the canonical file path if the location is a file or null otherwise
	 */
	public abstract String getCanonicalFilePath();

	public abstract File getIndexFile();

	abstract InputStream getInputStream() throws IOException;

	public URL getUrl() {
		return this.url;
	}

	@Override
	public int hashCode() {
		return this.url.hashCode();
	}

	public boolean isParticipantIndex() {
		return this.participantIndex;
	}

	/**
	 * @return the last modified time if the location is a file or -1 otherwise
	 */
	public abstract long lastModified();

	/**
	 * @return the length of the file if the location is a file or -1 otherwise
	 */
	public abstract long length();

	public abstract boolean startsWith(IPath path);

	@Override
	public String toString() {
		return this.url.toString();
	}
}
