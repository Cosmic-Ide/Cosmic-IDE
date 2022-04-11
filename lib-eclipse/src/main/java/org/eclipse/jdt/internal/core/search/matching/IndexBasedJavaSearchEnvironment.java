/*******************************************************************************
 * Copyright (c) 2015, 2018 Google, Inc and others.
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
package org.eclipse.jdt.internal.core.search.matching;

import java.util.Iterator;
import java.util.List;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.internal.compiler.env.INameEnvironment;
import org.eclipse.jdt.internal.core.JavaProject;

public class IndexBasedJavaSearchEnvironment {

	public static INameEnvironment create(List<IJavaProject> javaProjects, org.eclipse.jdt.core.ICompilationUnit[] copies) {
		Iterator<IJavaProject> next = javaProjects.iterator();
		JavaSearchNameEnvironment result = new JavaSearchNameEnvironment(next.next(), copies);

		while (next.hasNext()) {
			result.addProjectClassPath((JavaProject)next.next());
		}
		return result;
	}
}
