/*******************************************************************************
 * Copyright (c) 2015 Google Inc and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     John Glassmyer <jogl@google.com> - import group sorting is broken - https://bugs.eclipse.org/430303
 *******************************************************************************/
package org.eclipse.jdt.internal.core.dom.rewrite.imports;

import java.util.Comparator;

import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.internal.core.JavaProject;

/**
 * Sorts imports according to a lexicographic comparison of their containing package names.
 * <p>
 * This requires use of the JavaProject to look up packages and/or types by name in order to
 * distinguish segments of the import's container name as containing package name vs. containing
 * type name.
 * <p>
 * The alternative is {@link PackageAndContainingTypeImportComparator}. See
 * https://bugs.eclipse.org/194358.
 */
final class PackageImportComparator implements Comparator<ImportName> {
	private final JavaProject javaProject;

	PackageImportComparator(JavaProject javaProject) {
		this.javaProject = javaProject;
	}

	@Override
	public int compare(ImportName o1, ImportName o2) {
		return determinePackageName(o1).compareTo(determinePackageName(o2));
	}

	private String determinePackageName(ImportName importName) {
		String containerName = importName.containerName;

		try {
			// Loop from longest to shortest prefix (of dot-separated name segments) of the
			// container name until a package name is found.
			String containerNamePrefix = containerName;
			while (true) {
				// Try to find a package named with this prefix.
				if (this.javaProject.findPackageFragment(containerNamePrefix) != null) {
					return containerNamePrefix;
				}

				int lastSegmentStart = containerNamePrefix.lastIndexOf(Signature.C_DOT) + 1;

				// Use the heuristic that a prefix whose last segment starts with a lowercase letter
				// is a package name, if we don't recognize the prefix as the name of a type.
				if (this.javaProject.findType(containerNamePrefix) == null) {
					if (Character.isLowerCase(containerNamePrefix.charAt(lastSegmentStart))) {
						return containerNamePrefix;
					}
				}

				if (lastSegmentStart == 0) {
					// No prefix of containerName could be resolved to a package name.
					break;
				}

				containerNamePrefix = containerNamePrefix.substring(0, lastSegmentStart - 1);
			}
		} catch (JavaModelException e) {
			// An error occurred when we asked the JavaProject to resolve a name,
			// so there is no point in proceeding with the loop.
		}

		return containerName;
	}
}
