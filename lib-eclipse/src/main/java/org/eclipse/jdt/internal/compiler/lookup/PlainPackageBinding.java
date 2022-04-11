/*******************************************************************************
 * Copyright (c) 2019 GK Software SE, and others.
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
package org.eclipse.jdt.internal.compiler.lookup;

import org.eclipse.jdt.core.compiler.CharOperation;

/** A package binding that is known not to be a {@link SplitPackageBinding}. */
public class PlainPackageBinding extends PackageBinding {

	/** Create a toplevel package. */
	public PlainPackageBinding(char[] topLevelPackageName, LookupEnvironment environment, ModuleBinding enclosingModule) {
		this(new char[][] {topLevelPackageName}, null, environment, enclosingModule);
	}

	/** Create a default package. */
	public PlainPackageBinding(LookupEnvironment environment) {
		this(CharOperation.NO_CHAR_CHAR, null, environment, environment.module);
	}

	/** Create a normal package. */
	public PlainPackageBinding(char[][] compoundName, PackageBinding parent, LookupEnvironment environment, ModuleBinding enclosingModule) {
		super(compoundName, parent, environment, enclosingModule);
	}

	protected PlainPackageBinding(char[][] compoundName, LookupEnvironment environment) {
		// for problem bindings
		super(compoundName, environment);
	}

	@Override
	public PlainPackageBinding getIncarnation(ModuleBinding moduleBinding) {
		if (this.enclosingModule == moduleBinding)
			return this;
		return null;
	}
}
