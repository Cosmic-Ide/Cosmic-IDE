/*******************************************************************************
 * Copyright (c) 2014 Google Inc and others.
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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.JavaModelException;

import java.util.Set;

interface ConflictingSimpleNameFinder {
	/**
	 * Finds duplicate declarations of the specified simple names within the specified on-demand and
	 * implicit import containers.
	 *
	 * @param simpleNames
	 *            simple names of single imports in the compilation unit
	 * @param onDemandAndImplicitContainerNames
	 *            names of on-demand and implicitly imported containers (e.g. "java.lang")
	 * @param monitor
	 *            a progress monitor used to track time spent searching for conflicts
	 */
	Set<String> findConflictingSimpleNames(
			Set<String> simpleNames,
			Set<String> onDemandAndImplicitContainerNames,
			IProgressMonitor monitor) throws JavaModelException;
}