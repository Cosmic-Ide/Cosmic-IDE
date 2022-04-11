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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * Indicates that one or more single imports ({@code reducibleImports}) are unnecessary or could be
 * rendered unnecessary by the presence of an on-demand (.*) import ({@code containerOnDemand}) of
 * their containing type or package.
 * <p>
 * This "reduction" can be applied by removing all declarations of {@code reducibleImports}
 * from the compilation unit and adding a declaration of {@code containerOnDemand} to the
 * compilation unit if one is not already present.
 */
class OnDemandReduction {
	final ImportName containerOnDemand;
	final Collection<ImportName> reducibleImports;

	OnDemandReduction(ImportName containerName, Collection<ImportName> reducibleImports) {
		this.containerOnDemand = containerName;
		this.reducibleImports = Collections.unmodifiableCollection(new ArrayList<ImportName>(reducibleImports));
	}

	@Override
	public String toString() {
		return String.format("{%s: %s}", this.containerOnDemand.containerName, this.reducibleImports); //$NON-NLS-1$
	}
}