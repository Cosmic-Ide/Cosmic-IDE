/*******************************************************************************
 * Copyright (c) 2016, 2017 IBM Corporation and others.
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
package org.eclipse.jdt.internal.core.builder;


import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.env.IModule;
import org.eclipse.jdt.internal.compiler.env.IModulePathEntry;
import org.eclipse.jdt.internal.compiler.env.IMultiModuleEntry;

/**
 * Represents a project on the module path.
 */
public class ModulePathEntry implements IModulePathEntry {

	private IPath path;
	/*private*/ ClasspathLocation[] locations;
	IModule module;
	boolean isAutomaticModule;

	ModulePathEntry(IPath path, IModule module, ClasspathLocation[] locations) {
		this.path = path;
		this.locations = locations;
		this.module = module;
		this.isAutomaticModule = module.isAutomatic();
		initializeModule();
	}
	public ModulePathEntry(IPath path, ClasspathLocation location) {
		this.path = path;
		initModule(location);
		this.locations = new ClasspathLocation[] {location};
	}
	public IPath getPath() {
		return this.path;
	}
	public ClasspathLocation[] getClasspathLocations() {
		return this.locations;
	}

	@Override
	public IModule getModule() {
		//
		return this.module;
	}

	@Override
	public boolean isAutomaticModule() {
		return this.isAutomaticModule;
	}
	public static IModule getAutomaticModule(ClasspathLocation location) {
		if (location instanceof ClasspathJar) {
			ClasspathJar classpathJar = (ClasspathJar) location;
			return IModule.createAutomatic(classpathJar.zipFilename, true, classpathJar.getManifest());
		}
		if (location instanceof ClasspathDirectory) {
			return IModule.createAutomatic(((ClasspathDirectory) location).binaryFolder.getName(), false, null);
		}
		return null;
	}
	private void initModule(ClasspathLocation location) {
		IModule mod = null;
		if (location instanceof ClasspathJar) {
			mod = ((ClasspathJar) location).initializeModule();
		} else if (location instanceof ClasspathDirectory){
			mod = ((ClasspathDirectory) location).initializeModule();
		}
		if (mod != null) {
			this.module = mod;
			this.isAutomaticModule = false;
		} else {
			this.module = getAutomaticModule(location);
			this.isAutomaticModule = true;
		}
		location.setModule(this.module);
	}

	// TODO: This is only needed because SourceFile.module() uses the module set on the location
	// Once we have a mechanism to map a folder to a module path entry, this should no longer be
	// needed
	private void initializeModule() {
		for (int i = 0; i < this.locations.length; i++) {
			this.locations[i].setModule(this.module);
		}
	}
	@Override
	public char[][] getModulesDeclaringPackage(String qualifiedPackageName, String moduleName) {
		if (moduleName != null && ((this.module == null) || !moduleName.equals(String.valueOf(this.module.name()))))
			return null;
		// search all locations
		char[][] names = CharOperation.NO_CHAR_CHAR;
		for (ClasspathLocation cp : this.locations) {
			char[][] declaringModules = cp.getModulesDeclaringPackage(qualifiedPackageName, moduleName);
			if (declaringModules != null)
				names = CharOperation.arrayConcat(names, declaringModules);
		}
		return names == CharOperation.NO_CHAR_CHAR ? null : names;
	}
	@Override
	public boolean hasCompilationUnit(String qualifiedPackageName, String moduleName) {
		for (ClasspathLocation cp : this.locations) {
			if (cp.hasCompilationUnit(qualifiedPackageName, moduleName))
				return true;
		}
		return false;
	}

	@Override
	public char[][] listPackages() {
		char[][] packages = CharOperation.NO_CHAR_CHAR;
		if (this.isAutomaticModule) {
			for (ClasspathLocation cp : this.locations) {
				packages = CharOperation.arrayConcat(packages, cp.listPackages());
			}
			return packages;
		}
		return packages;
	}

	/**
	 * Combines an IMultiModuleEntry with further locations in order to support patch-module.
	 * Implemented by adding IMultiModuleEntry functionality to ModulePathEntry.
	 */
	static public class Multi extends ModulePathEntry implements IMultiModuleEntry {

		Multi(IPath path, IModule module, ClasspathLocation[] locations) {
			super(path, module, locations);
		}

		void addPatchLocation(ClasspathLocation location) {
			this.locations = Arrays.copyOf(this.locations, this.locations.length+1);
			this.locations[this.locations.length-1] = location;
			location.setModule(this.module);
		}

		@Override
		public IModule getModule(char[] name) {
			for (ClasspathLocation loc : this.locations) {
				if (loc instanceof IMultiModuleEntry) {
					IModule mod = ((IMultiModuleEntry) loc).getModule(name);
					if (mod != null)
						return mod;
				} else {
					IModule mod = loc.getModule();
					if (CharOperation.equals(mod.name(), name))
						return mod;
				}
			}
			return null;
		}

		@Override
		public Collection<String> getModuleNames(Collection<String> limitModules) {
			Set<String> result = new HashSet<>();
			for (ClasspathLocation loc : this.locations) {
				if (loc instanceof IMultiModuleEntry)
					result.addAll(((IMultiModuleEntry) loc).getModuleNames(limitModules));
				else
					result.add(String.valueOf(loc.getModule().name()));
			}
			return result;
		}
	}
}
