/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
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
package org.eclipse.jdt.core.index;

import java.io.IOException;

import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.internal.core.search.indexing.DefaultJavaIndexer;

/**
 * {@link JavaIndexer} provides functionality to generate index files which can be used by the JDT {@link SearchEngine}.
 * The generated index files can be used as a classpath attribute for the particular classpath entry.
 *
 * <p> The search engine indexes all the elements referred in the classpath entries of the project into
 * index files. These index files are used to search the elements faster. Indexing for bigger jars could
 * take some time. To avoid this time, one can generate the index file and specify it when the jar is added
 * to the classpath of the project. </p>
 *
 * @since 3.8
 */
public final class JavaIndexer {

	/**
	 * Generates the index file for the specified jar.
	 * @param pathToJar The full path to the jar that needs to be indexed
	 * @param pathToIndexFile The full path to the index file that needs to be generated
	 * @throws IOException if the jar is not found or could not write into the index file
	 * @since 3.8
	 */
	public static void generateIndexForJar(String pathToJar, String pathToIndexFile) throws IOException {
		new DefaultJavaIndexer().generateIndexForJar(pathToJar, pathToIndexFile);
	}

}
