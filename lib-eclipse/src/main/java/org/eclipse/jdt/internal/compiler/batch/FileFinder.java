/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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
package org.eclipse.jdt.internal.compiler.batch;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.internal.compiler.env.IModule;

public class FileFinder {

/**
 * Although the file finder is meant to be generic for any file name patters,
 * at the moment it is used only for *.java files. This method handles the
 * module-info.java in a special way by always placing it as the first element
 * of the resulting array.
 */
public static String[] find(File f, String pattern) {
	List<String> files = new ArrayList<>();
	find0(f, pattern, files);
	String[] result = new String[files.size()];
	files.toArray(result);
	return result;
}
private static void find0(File f, String pattern, List<String> collector) {
	if (f.isDirectory()) {
		String[] files = f.list();
		if (files == null) return;
		for (int i = 0, max = files.length; i < max; i++) {
			File current = new File(f, files[i]);
			if (current.isDirectory()) {
				find0(current, pattern, collector);
			} else {
				String name = current.getName().toLowerCase();
				if (name.endsWith(pattern)) {
					// NOTE: This handles only the lower case name. Check with the spec about
					// Naming of the module descriptor before making this code code insensitive.
					if (name.endsWith(IModule.MODULE_INFO_JAVA)) {
						collector.add(0, current.getAbsolutePath());
					} else {
						collector.add(current.getAbsolutePath());
					}
				}
			}
		}
	}
}
}
