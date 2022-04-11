/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation.
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

import org.eclipse.jdt.core.compiler.CharOperation;

public class ModuleReferenceImpl implements IModule.IModuleReference {
	public char[] name;
	public int modifiers;
	@Override
	public char[] name() {
		return this.name;
	}
	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof IModule.IModuleReference))
			return false;
		IModule.IModuleReference mod = (IModule.IModuleReference) o;
		if (this.modifiers != mod.getModifiers())
			return false;
		return CharOperation.equals(this.name, mod.name());
	}
	@Override
	public int hashCode() {
		return CharOperation.hashCode(this.name);
	}
	@Override
	public int getModifiers() {
		return this.modifiers;
	}
}