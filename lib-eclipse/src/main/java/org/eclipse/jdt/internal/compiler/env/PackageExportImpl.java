/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
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
package org.eclipse.jdt.internal.compiler.env;

public class PackageExportImpl implements IModule.IPackageExport {
	public char[] pack;
	public char[][] exportedTo;
	@Override
	public char[] name() {
		return this.pack;
	}

	@Override
	public char[][] targets() {
		return this.exportedTo;
	}
	@Override
	public String toString() {
		StringBuilder buffer = new StringBuilder();
		buffer.append(this.pack);
		buffer.append(" to "); //$NON-NLS-1$
		if (this.exportedTo != null) {
			for (int i = 0; i < this.exportedTo.length; i++) {
				if (i > 0) {
					buffer.append(", "); //$NON-NLS-1$
				}
				char[] cs = this.exportedTo[i];
				buffer.append(cs);
			}
		}
		buffer.append(';');
		return buffer.toString();
	}
}