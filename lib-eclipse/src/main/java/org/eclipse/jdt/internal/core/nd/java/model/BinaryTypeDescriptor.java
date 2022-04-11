/*******************************************************************************
 * Copyright (c) 2015, 2016 Google, Inc and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Stefan Xenos (Google) - Initial implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.core.nd.java.model;

import org.eclipse.jdt.internal.compiler.env.IDependent;
import org.eclipse.jdt.internal.core.nd.util.CharArrayUtils;

/**
 * Holds a lightweight identifier for an IBinaryType, with sufficient information to either read it from
 * disk or read it from the index.
 */
public final class BinaryTypeDescriptor {
	public final char[] indexPath;
	public final char[] fieldDescriptor;
	public final char[] location;
	public final char[] workspacePath;

	/**
	 * Constructs a new descriptor
	 *
	 * @param location
	 *            location where the archive (.jar or .class) can be found in the local filesystem
	 * @param fieldDescriptor
	 *            field descriptor for the type (see the JVM specification)
	 * @param workspacePath
	 *            location where the archive (.jar or class) can be found in the workspace. If it is not in the
	 *            workspace, this is the path where it can be found on the local filesystem.
	 * @param indexPath
	 *            index path for the new type (workspace-or-local path to jar optionally followed by a | and a relative
	 *            path within the .jar)
	 */
	public BinaryTypeDescriptor(char[] location, char[] fieldDescriptor, char[] workspacePath, char[] indexPath) {
		super();
		this.location = location;
		this.fieldDescriptor = fieldDescriptor;
		this.indexPath = indexPath;
		this.workspacePath = workspacePath;
	}

	public boolean isInJarFile() {
		return CharArrayUtils.indexOf(IDependent.JAR_FILE_ENTRY_SEPARATOR, this.indexPath) != -1;
	}

	/**
	 * For debugging purposes only.
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(this.workspacePath);
		builder.append('|');
		builder.append(this.fieldDescriptor);
		return builder.toString();
	}
}