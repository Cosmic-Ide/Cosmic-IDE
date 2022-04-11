/*******************************************************************************
 * Copyright (c) 2016, 2018 IBM Corporation and others.
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
package org.eclipse.jdt.internal.core;

import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IModuleDescription;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.env.IModule;
import org.eclipse.jdt.internal.compiler.env.IModulePathEntry;

/**
 * Represents a project
 *
 */
public class ProjectEntry implements IModulePathEntry {

	static boolean representsProject(IModulePathEntry entry, IJavaProject otherProject) {
		if (entry instanceof ProjectEntry) {
			return ((ProjectEntry) entry).project.equals(otherProject);
		}
		return false;
	}

	JavaProject project;

	public ProjectEntry(JavaProject project) {
		//
		this.project = project;
	}
	@Override
	public IModule getModule() {
		try {
			IModuleDescription module = this.project.getModuleDescription();
			if (module != null) {
				return (IModule) ((JavaElement) module) .getElementInfo();
			}
		} catch (JavaModelException e) {
			// Proceed with null;
		}
		return null;
	}

	@Override
	public boolean isAutomaticModule() {
		return false;
	}

	@Override
	public char[][] getModulesDeclaringPackage(String qualifiedPackageName, String moduleName) {
		// TODO(SHMOD): verify (is unnamed handled correctly?)
		IModule mod = getModule();
		if (mod == null) {
			if (moduleName != null)
				return null;
		} else if (!String.valueOf(mod.name()).equals(moduleName)) {
			return null;
		}
		try {
			IJavaElement element = this.project.findElement(new Path(qualifiedPackageName.replace('.', '/')));
			if (element instanceof IPackageFragment)
				return mod != null ? new char[][] { mod.name() } : CharOperation.NO_CHAR_CHAR;
		} catch (JavaModelException e) {
			return null;
		}
		return null;
	}
	@Override
	public boolean hasCompilationUnit(String qualifiedPackageName, String moduleName) {
		try {
			for (IPackageFragmentRoot root : this.project.getPackageFragmentRoots()) {
				if (root instanceof PackageFragmentRoot && ((PackageFragmentRoot) root).hasCompilationUnit(qualifiedPackageName, moduleName))
					return true;
			}
		} catch (JavaModelException e) {
			// silent
		}
		return false;
	}
}
