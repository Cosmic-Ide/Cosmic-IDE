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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IModuleDescription;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.env.IModule;
import org.eclipse.jdt.internal.compiler.env.IModulePathEntry;

public class ModuleSourcePathManager {

	private Map<String, IModulePathEntry> knownModules = new HashMap<String, IModulePathEntry>(11);

	private IModulePathEntry getModuleRoot0(String name) {
		return this.knownModules.get(name);
	}
	public IModulePathEntry getModuleRoot(String name) {
		IModulePathEntry root = getModuleRoot0(name);
		if (root == null) {
			try {
				seekModule(name.toCharArray(),false, new JavaElementRequestor());
			} catch (JavaModelException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		root = this.knownModules.get(name);
		return root;
	}
	public void addEntry(IModuleDescription module, JavaProject project) throws JavaModelException {
		String moduleName = new String(module.getElementName().toCharArray());
		IModulePathEntry entry = getModuleRoot0(moduleName);
		if (entry != null) {
			// TODO : Should we signal error via JavaModelException
			return;
		}
		this.knownModules.put(moduleName, new ProjectEntry(project));
	}

	public void removeEntry(JavaProject javaProject) {
		Entry<String, IModulePathEntry> entry = this.knownModules.entrySet().stream()
			.filter(e -> ProjectEntry.representsProject(e.getValue(), javaProject))
			.findFirst()
			.orElse(null);

		String key = entry != null ? entry.getKey() : null;
		if (key != null) {
			this.knownModules.remove(key);
		}
	}
	interface IPrefixMatcherCharArray {
		boolean matches(char[] prefix, char[] name);
	}
	public void seekModule(char[] name, boolean prefixMatch, IJavaElementRequestor requestor) throws JavaModelException {
		if (name == null)
			return;
		IPrefixMatcherCharArray prefixMatcher = prefixMatch ? CharOperation.equals(name, CharOperation.ALL_PREFIX) ?
				(x, y) -> true : CharOperation::prefixEquals : CharOperation :: equals;
		IJavaProject[] projects = JavaModelManager.getJavaModelManager().getJavaModel().getJavaProjects();
		for (int i = 0; i < projects.length; i++) {
			IJavaProject project = projects[i];
			if (!project.getProject().isAccessible())
				continue;
			if (project instanceof JavaProject) {
				IModuleDescription module = ((JavaProject) project).getModuleDescription();
				if (module != null) {
					if (prefixMatcher.matches(name, module.getElementName().toCharArray())) {
						//addEntry(module, (JavaProject) project);
						requestor.acceptModule(module);
					}
				}
			}
		}
	}
	public IModule getModule(char[] name) {
		IModulePathEntry root = getModuleRoot0(CharOperation.charToString(name));
		if (root != null)
			try {
				return root.getModule();
			} catch (Exception e1) {
				//
				return null;
			}
		JavaElementRequestor requestor = new JavaElementRequestor();
		try {
			seekModule(name, false, requestor);
		} catch (JavaModelException e) {
			//
		}
		IModuleDescription[] modules = requestor.getModules();
		if (modules.length > 0) {
			IModuleDescription module = modules[0];
			try {
				return (IModule) ((JavaElement) module).getElementInfo();
			} catch (JavaModelException e) {
				e.printStackTrace();
			}
		}
		return null;
	}
//	public IModuleDeclaration[] getModules() {
//		if (this.knownModules.size() == 0) {
//			return new IModuleDeclaration[0];
//		}
//		List<IModuleDeclaration> modules = new ArrayList<IModuleDeclaration>();
//		for (IModulePathEntry val : this.knownModules.values()) {
//			try {
//				modules.add(val.getModule());
//			} catch (Exception e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
//		return modules.toArray(new IModuleDeclaration[modules.size()]);
//	}
}
