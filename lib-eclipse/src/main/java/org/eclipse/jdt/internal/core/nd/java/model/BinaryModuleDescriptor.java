/*******************************************************************************
 * Copyright (c) 2017 GK Software AG, and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Stephan Herrmann - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.core.nd.java.model;

import org.eclipse.jdt.internal.compiler.env.IDependent;
import org.eclipse.jdt.internal.core.nd.util.CharArrayUtils;

/**
 * Holds a lightweight identifier for an IBinaryModule, with sufficient information to either read it from
 * disk or read it from the index.
 */
public class BinaryModuleDescriptor {
	public final char[] indexPath;
	public final char[] moduleName; // TODO: not sure if this will be needed once wired to the index?
	public final char[] location;
	public final char[] workspacePath;

	/**
	 * Constructs a new descriptor
	 *
	 * @param location
	 *            location where the archive (.jar or .class) can be found in the local filesystem
	 * @param moduleName
	 *            name of the module
	 * @param workspacePath
	 *            location where the archive (.jar or class) can be found in the workspace. If it is not in the
	 *            workspace, this is the path where it can be found on the local filesystem.
	 * @param indexPath
	 *            index path for the new module (workspace-or-local path to jar optionally followed by a | and a relative
	 *            path within the .jar)
	 */
	public BinaryModuleDescriptor(char[] location, char[] moduleName, char[] workspacePath, char[] indexPath) {
		super();
		this.location = location;
		this.moduleName = moduleName;
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
		builder.append(this.moduleName);
		return builder.toString();
	}
}
