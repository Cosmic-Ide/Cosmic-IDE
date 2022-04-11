/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
package org.eclipse.jdt.internal.compiler.batch;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.env.AccessRestriction;
import org.eclipse.jdt.internal.compiler.env.AccessRuleSet;
import org.eclipse.jdt.internal.compiler.env.IModule;
import org.eclipse.jdt.internal.compiler.env.IModule.IModuleReference;
import org.eclipse.jdt.internal.compiler.lookup.ModuleBinding;
import org.eclipse.jdt.internal.compiler.util.SuffixConstants;

public abstract class ClasspathLocation implements FileSystem.Classpath,
		SuffixConstants {

	public static final int SOURCE = 1;
	public static final int BINARY = 2;

	String path;
	char[] normalizedPath;
	public AccessRuleSet accessRuleSet;
	IModule module;

	public String destinationPath;
		// destination path for compilation units that are reached through this
		// classpath location; the coding is consistent with the one of
		// Main.destinationPath:
		// == null: unspecified, use whatever value is set by the enclosing
		//          context, id est Main;
		// == Main.NONE: absorbent element, do not output class files;
		// else: use as the path of the directory into which class files must
		//       be written.
		// potentially carried by any entry that contains to be compiled files

	protected ClasspathLocation(AccessRuleSet accessRuleSet,
			String destinationPath) {
		this.accessRuleSet = accessRuleSet;
		this.destinationPath = destinationPath;
	}

	/**
	 * Return the first access rule which is violated when accessing a given
	 * type, or null if no 'non accessible' access rule applies.
	 *
	 * @param qualifiedBinaryFileName
	 *            tested type specification, formed as:
	 *            "org/eclipse/jdt/core/JavaCore.class"; on systems that
	 *            use \ as File.separator, the
	 *            "org\eclipse\jdt\core\JavaCore.class" is accepted as well
	 * @return the first access rule which is violated when accessing a given
	 *         type, or null if none applies
	 */
	protected AccessRestriction fetchAccessRestriction(String qualifiedBinaryFileName) {
		if (this.accessRuleSet == null)
			return null;
		char [] qualifiedTypeName = qualifiedBinaryFileName.
			substring(0, qualifiedBinaryFileName.length() - SUFFIX_CLASS.length)
			.toCharArray();
		if (File.separatorChar == '\\') {
			CharOperation.replace(qualifiedTypeName, File.separatorChar, '/');
		}
		return this.accessRuleSet.getViolatedRestriction(qualifiedTypeName);
	}

	public int getMode() {
		return SOURCE | BINARY;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + this.getMode();
		result = prime * result + ((this.path == null) ? 0 : this.path.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ClasspathLocation other = (ClasspathLocation) obj;
		String localPath = this.getPath();
		String otherPath = other.getPath();
		if (localPath == null) {
			if (otherPath != null)
				return false;
		} else if (!localPath.equals(otherPath))
			return false;
		if (this.getMode() != other.getMode())
			return false;
		return true;
	}
	@Override
	public String getPath() {
		return this.path;
	}
	@Override
	public String getDestinationPath() {
		return this.destinationPath;
	}

	@Override
	public void acceptModule(IModule mod) {
		this.module = mod;
	}
	@Override
	public boolean isAutomaticModule() {
		return this.module == null ? false : this.module.isAutomatic();
	}
	@Override
	public Collection<String> getModuleNames(Collection<String> limitModules) {
		return getModuleNames(limitModules, m -> getModule(m.toCharArray()));
	}
	@Override
	public Collection<String> getModuleNames(Collection<String> limitModules, Function<String,IModule> getModule) {
		if (this.module != null) {
			String name = String.valueOf(this.module.name());
			return selectModules(Collections.singleton(name), limitModules, getModule);
		}
		return Collections.emptyList();
	}
	protected Collection<String> selectModules(Set<String> modules, Collection<String> limitModules, Function<String,IModule> getModule) {
		Collection<String> rootModules;
		if (limitModules != null) {
			Set<String> result = new HashSet<>(modules);
			result.retainAll(limitModules);
			rootModules = result;
		} else {
			rootModules = allModules(modules, s -> s, m -> getModule(m.toCharArray()));
		}
		Set<String> allModules = new HashSet<>(rootModules);
		for (String mod : rootModules)
			addRequired(mod, allModules, getModule);
		return allModules;
	}

	private void addRequired(String mod, Set<String> allModules, Function<String,IModule> getModule) {
		IModule iMod = getModule(mod.toCharArray());
		if (iMod != null) {
			for (IModuleReference requiredRef : iMod.requires()) {
				IModule reqMod = getModule.apply(new String(requiredRef.name()));
				if (reqMod != null) {
					String reqModName = String.valueOf(reqMod.name());
					if (allModules.add(reqModName))
						addRequired(reqModName, allModules, getModule);
				}
			}
		}
	}
	protected <T> List<String> allModules(Iterable<T> allSystemModules, Function<T,String> getModuleName, Function<T,IModule> getModule) {
		List<String> result = new ArrayList<>();
		for (T mod : allSystemModules) {
			String moduleName = getModuleName.apply(mod);
			result.add(moduleName);
		}
		return result;
	}

	@Override
	public boolean isPackage(String qualifiedPackageName, String moduleName) {
		return getModulesDeclaringPackage(qualifiedPackageName, moduleName) != null;
	}

	protected char[][] singletonModuleNameIf(boolean condition) {
		if (!condition)
			return null;
		if (this.module != null)
			return new char[][] { this.module.name() };
		return new char[][] { ModuleBinding.UNNAMED };
	}

	@Override
	public void reset() {
		this.module = null;
	}
}
