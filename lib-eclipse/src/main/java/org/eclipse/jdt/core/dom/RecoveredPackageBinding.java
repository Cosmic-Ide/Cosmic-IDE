/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
package org.eclipse.jdt.core.dom;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.lookup.ModuleBinding;
import org.eclipse.jdt.internal.compiler.lookup.PackageBinding;
import org.eclipse.jdt.internal.compiler.util.Util;

/**
 * This class represents the recovered binding for a package
 */
class RecoveredPackageBinding implements IPackageBinding {

	private static final String[] NO_NAME_COMPONENTS = CharOperation.NO_STRINGS;
	private static final String UNNAMED = Util.EMPTY_STRING;
	private static final char PACKAGE_NAME_SEPARATOR = '.';

	private PackageBinding binding;
	private BindingResolver resolver;
	private String name = null;
	private String[] components = null;

	RecoveredPackageBinding(org.eclipse.jdt.internal.compiler.lookup.PackageBinding binding, BindingResolver resolver) {
		this.binding = binding;
		this.resolver = resolver;
	}

	@Override
	public IAnnotationBinding[] getAnnotations() {
		return AnnotationBinding.NoAnnotations;
	}

	@Override
	public int getKind() {
		return IBinding.PACKAGE;
	}

	@Override
	public int getModifiers() {
		return Modifier.NONE;
	}

	@Override
	public boolean isDeprecated() {
		return false;
	}

	@Override
	public boolean isRecovered() {
		return true;
	}

	@Override
	public boolean isSynthetic() {
		return false;
	}

	@Override
	public IJavaElement getJavaElement() {
		return null;
	}

	@Override
	public String getKey() {
		StringBuilder buffer = new StringBuilder();
		buffer.append("Recovered#"); //$NON-NLS-1$
		buffer.append(this.binding.computeUniqueKey());
		return buffer.toString();
	}

	@Override
	public boolean isEqualTo(IBinding other) {
		if (!other.isRecovered() || other.getKind() != IBinding.PACKAGE) return false;
		return getKey().equals(other.getKey());
	}

	@Override
	public String getName() {
		if (this.name == null) {
			computeNameAndComponents();
		}
		return this.name;
	}

	@Override
	public boolean isUnnamed() {
		return false;
	}

	@Override
	public String[] getNameComponents() {
		if (this.components == null) {
			computeNameAndComponents();
		}
		return this.components;
	}
	@Override
	public IModuleBinding getModule() {
		ModuleBinding moduleBinding = this.binding.enclosingModule;
		return moduleBinding != null ? this.resolver.getModuleBinding(moduleBinding) : null;
	}
	private void computeNameAndComponents() {
		char[][] compoundName = this.binding.compoundName;
		if (compoundName == CharOperation.NO_CHAR_CHAR || compoundName == null) {
			this.name = UNNAMED;
			this.components = NO_NAME_COMPONENTS;
		} else {
			int length = compoundName.length;
			this.components = new String[length];
			StringBuilder buffer = new StringBuilder();
			for (int i = 0; i < length - 1; i++) {
				this.components[i] = new String(compoundName[i]);
				buffer.append(compoundName[i]).append(PACKAGE_NAME_SEPARATOR);
			}
			this.components[length - 1] = new String(compoundName[length - 1]);
			buffer.append(compoundName[length - 1]);
			this.name = buffer.toString();
		}
	}

}
