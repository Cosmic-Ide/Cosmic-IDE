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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.core.util.Util;

public class ModulePathContainerInitializer extends ClasspathContainerInitializer {

	@Override
	public void initialize(IPath containerPath, IJavaProject project) throws CoreException {
		//
		if (isModulePathContainer(containerPath)) {
			if (project instanceof JavaProject) {
				// TODO: should project compliance level be checked here?
				ModulePathContainer container = new ModulePathContainer(project);
				JavaCore.setClasspathContainer(containerPath, new IJavaProject[] { project },
						new IClasspathContainer[] { container }, null);
			}
		} else if (JavaModelManager.CP_RESOLVE_VERBOSE || JavaModelManager.CP_RESOLVE_VERBOSE_FAILURE) {
			verbose_not_a_module_project(project, containerPath);
		}
	}
	private boolean isModulePathContainer(IPath path) {
		return path != null && JavaCore.MODULE_PATH_CONTAINER_ID.equals(path.segment(0));
	}
	private void verbose_not_a_module_project(IJavaProject project, IPath containerPath) {
		Util.verbose(
			"Module path INIT - FAILED (not a module project)\n" + //$NON-NLS-1$
			"	project: " + project.getElementName() + '\n' + //$NON-NLS-1$
			"	container path: " + containerPath); //$NON-NLS-1$
	}
}
