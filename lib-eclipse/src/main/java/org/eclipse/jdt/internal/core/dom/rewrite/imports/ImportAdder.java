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

import java.util.Collection;
import java.util.List;

interface ImportAdder {
	/**
	 * Returns a new list containing the elements of {@code existingImports} and also containing
	 * each element of {@code importsToAdd} for which {@code existingImports} does not contain an
	 * equal element.
	 */
	List<ImportName> addImports(Collection<ImportName> existingImports, Collection<ImportName> importsToAdd);
}
