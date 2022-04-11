/*******************************************************************************
 * Copyright (c) 2017, 2018 GK Software AG, and others.
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
package org.eclipse.jdt.internal.core;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaModelStatusConstants;
import org.eclipse.jdt.core.IModuleDescription;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.env.IModuleAwareNameEnvironment;
import org.eclipse.jdt.internal.compiler.env.IUpdatableModule;
import org.eclipse.jdt.internal.compiler.env.IUpdatableModule.UpdateKind;
import org.eclipse.jdt.internal.compiler.env.IUpdatableModule.UpdatesByKind;
import org.eclipse.jdt.internal.compiler.lookup.ModuleBinding;
import org.eclipse.jdt.internal.core.util.Util;

/**
 * An instance of this class collects <code>add-exports</code> and <code>add-reads</code> options from
 * a project's class path entries, and performs the corresponding updates when requested by the compiler.
 * <p>For <code>patch-module</code> and <code>limit-modules</code> see
 * org.eclipse.jdt.internal.core.builder.ModuleEntryProcessor.</p>
 */
public class ModuleUpdater {

	private JavaProject javaProoject;

	private Map<String,UpdatesByKind> moduleUpdates = new HashMap<>();

	public ModuleUpdater(JavaProject javaProject) {
		this.javaProoject = javaProject;
	}

	/**
	 * Detects any ADD_EXPORTS or ADD_READS classpath attributes, parses the value,
	 * and collects the resulting module updates.
	 * @param entry a classpath entry of the current project.
	 * @throws JavaModelException
	 */
	public void computeModuleUpdates(IClasspathEntry entry) throws JavaModelException {
		for (IClasspathAttribute attribute : entry.getExtraAttributes()) {
			String attributeName = attribute.getName();
			String values = attribute.getValue(); // the attributes considered here may have multiple values separated by ':'
			if (attributeName.equals(IClasspathAttribute.ADD_EXPORTS)) {
				for (String value : values.split(":")) { // format: <source-module>/<package>=<target-module>(,<target-module>)* //$NON-NLS-1$
					int slash = value.indexOf('/');
					int equals = value.indexOf('=');
					if (slash != -1 && equals != -1) {
						String modName = value.substring(0, slash);
						char[] packName = value.substring(slash+1, equals).toCharArray();
						char[][] targets = CharOperation.splitOn(',', value.substring(equals+1).toCharArray());
						addModuleUpdate(modName, new IUpdatableModule.AddExports(packName, targets), UpdateKind.PACKAGE);
					} else {
						Util.log(IStatus.WARNING, "Invalid argument to add-exports: "+value); //$NON-NLS-1$
					}
				}
			} else if (attributeName.equals(IClasspathAttribute.ADD_READS)) {
				for (String value : values.split(":")) { // format: <source-module>=<target-module> //$NON-NLS-1$
					int equals = value.indexOf('=');
					if (equals != -1) {
						String srcMod = value.substring(0, equals);
						char[] targetMod = value.substring(equals+1).toCharArray();
						addModuleUpdate(srcMod, new IUpdatableModule.AddReads(targetMod), UpdateKind.MODULE);
					} else {
						Util.log(IStatus.WARNING, "Invalid argument to add-reads: "+value); //$NON-NLS-1$
					}
				}
			} else if (attributeName.equals(IClasspathAttribute.MODULE_MAIN_CLASS)) {
				IModuleDescription module = this.javaProoject.getModuleDescription();
				if (module == null)
					throw new JavaModelException(new JavaModelStatus(IJavaModelStatusConstants.ELEMENT_DOES_NOT_EXIST));
				addModuleUpdate(module.getElementName(), m -> m.setMainClassName(values.toCharArray()), UpdateKind.MODULE);
			}
		}
	}

	public void addModuleUpdate(String moduleName, Consumer<IUpdatableModule> update, UpdateKind kind) {
		UpdatesByKind updates = this.moduleUpdates.get(moduleName);
		if (updates == null) {
			this.moduleUpdates.put(moduleName, updates = new UpdatesByKind());
		}
		updates.getList(kind, true).add(update);
	}

	/**
	 * @see IModuleAwareNameEnvironment#applyModuleUpdates(IUpdatableModule, UpdateKind)
	 */
	public void applyModuleUpdates(IUpdatableModule compilerModule, UpdateKind kind) {
		char[] name = compilerModule.name();
		if (name != ModuleBinding.UNNAMED) { // can't update the unnamed module
			UpdatesByKind updates = this.moduleUpdates.get(String.valueOf(name));
			if (updates != null) {
				for (Consumer<IUpdatableModule> update : updates.getList(kind, false))
					update.accept(compilerModule);
			}
		}
	}

	private static boolean containsNonModularDependency(IClasspathEntry[] entries) {
		for (IClasspathEntry e : entries) {
			if (e.getEntryKind() != IClasspathEntry.CPE_SOURCE && !((ClasspathEntry) e).isModular())
				return true;
		}
		return false;
	}

	// Bug 520713: allow test code to access code on the classpath
	public void addReadUnnamedForNonEmptyClasspath(JavaProject project, IClasspathEntry[] expandedClasspath)
			throws JavaModelException {
		for (String moduleName : determineModulesOfProjectsWithNonEmptyClasspath(project, expandedClasspath)) {
			addModuleUpdate(moduleName, new IUpdatableModule.AddReads(ModuleBinding.ALL_UNNAMED), UpdateKind.MODULE);
		}
	}

	public static Set<String> determineModulesOfProjectsWithNonEmptyClasspath(JavaProject project,
			IClasspathEntry[] expandedClasspath) throws JavaModelException {
		LinkedHashSet<String> list = new LinkedHashSet<>();
		if (containsNonModularDependency(expandedClasspath)) {
			IModuleDescription moduleDescription = project.getModuleDescription();
			if (moduleDescription != null) {
				list.add(moduleDescription.getElementName());
			}
		}
		for (IClasspathEntry e1 : expandedClasspath) {
			if (e1.getEntryKind() == IClasspathEntry.CPE_PROJECT) {
				Object target = JavaModel.getTarget(e1.getPath(), true);
				if (target instanceof IProject) {
					IProject prereqProject = (IProject) target;
					if (JavaProject.hasJavaNature(prereqProject)) {
						JavaProject prereqJavaProject = (JavaProject) JavaCore.create(prereqProject);
						if (containsNonModularDependency(prereqJavaProject.getResolvedClasspath())) {
							IModuleDescription prereqModuleDescription = prereqJavaProject.getModuleDescription();
							if (prereqModuleDescription != null) {
								list.add(prereqModuleDescription.getElementName());
							}
						}
					}
				}
			}
		}
		return list;
	}
	public UpdatesByKind getUpdates(String moduleName) {
		return this.moduleUpdates.get(moduleName);
	}
}
