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
package org.eclipse.jdt.internal.core;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.compiler.env.IModulePathEntry;

public class ModulePathContainer implements IClasspathContainer{

	private IJavaProject project;

	public ModulePathContainer(IJavaProject project) {
		this.project = project;
	}
	@Override
	public IClasspathEntry[] getClasspathEntries() {
		//
		List<IClasspathEntry> entries = new ArrayList<>();
		ModuleSourcePathManager manager = JavaModelManager.getModulePathManager();
		try {
			AbstractModule module = (AbstractModule) ((JavaProject)this.project).getModuleDescription();
			if (module == null)
				return new IClasspathEntry[0];
			for (org.eclipse.jdt.internal.compiler.env.IModule.IModuleReference ref : module.getRequiredModules()) {
				IModulePathEntry entry = manager.getModuleRoot(new String(ref.name()));
				JavaProject refRoot = null;
				if (entry instanceof ProjectEntry) {
					refRoot = ((ProjectEntry) entry).project;
				}
				if (refRoot == null)
					continue;
				IPath path = refRoot.getPath();
				IClasspathAttribute moduleAttribute = new ClasspathAttribute(IClasspathAttribute.MODULE, "true"); //$NON-NLS-1$
				entries.add(JavaCore.newProjectEntry(path, ClasspathEntry.NO_ACCESS_RULES,
						false,
						new IClasspathAttribute[] {moduleAttribute}, ref.isTransitive()));
			}
		} catch (JavaModelException e) {
			// ignore
		}
		return entries.toArray(new IClasspathEntry[entries.size()]);
	}

	@Override
	public String getDescription() {
		//
		return "Module path"; //$NON-NLS-1$
	}

	@Override
	public int getKind() {
		//
		return K_APPLICATION;
	}

	@Override
	public IPath getPath() {
		//
		return new Path(JavaCore.MODULE_PATH_CONTAINER_ID);
	}

}
