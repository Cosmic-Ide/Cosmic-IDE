/*******************************************************************************
 * Copyright (c) 2017, 2019 IBM Corporation and others.
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
import java.util.Arrays;
import java.util.List;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IModuleDescription;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.env.IModule;
import org.eclipse.jdt.internal.compiler.env.IModule.IModuleReference;
import org.eclipse.jdt.internal.compiler.env.IModule.IPackageExport;
import org.eclipse.jdt.internal.compiler.env.IModule.IService;

public interface AbstractModule extends IModuleDescription {

	/**
	 * Handle for an automatic module.
	 *
	 * <p>Note, that by definition this is mostly a fake, only {@link #getElementName()} provides a useful value.</p>
	 */
	static class AutoModule extends NamedMember implements AbstractModule {

		private boolean nameFromManifest;

		public AutoModule(JavaElement parent, String name, boolean nameFromManifest) {
			super(parent, name);
			this.nameFromManifest = nameFromManifest;
		}
		@Override
		public IJavaElement[] getChildren() throws JavaModelException {
			return JavaElement.NO_ELEMENTS; // may later answer computed details
		}
		@Override
		public int getFlags() throws JavaModelException {
			return 0;
		}
		@Override
		public boolean isAutoModule() {
			return true;
		}
		public boolean isAutoNameFromManifest() {
			return this.nameFromManifest;
		}
		@Override
		public char getHandleMementoDelimiter() {
			return JavaElement.JEM_MODULE;
		}
		@Override
		public ITypeRoot getTypeRoot() {
			return null; // has no real CompilationUnit nor ClassFile
		}
		@Override
		public IModuleReference[] getRequiredModules() throws JavaModelException {
			return ModuleDescriptionInfo.NO_REQUIRES;
		}
		@Override
		public void toStringContent(StringBuffer buffer, String lineDelimiter) throws JavaModelException {
			buffer.append("automatic module "); //$NON-NLS-1$
			buffer.append(this.name);
		}
	}

	// "forward declaration" for a method from JavaElement:
	abstract Object getElementInfo() throws JavaModelException;

	default IModule getModuleInfo() throws JavaModelException {
		return (IModule) getElementInfo();
	}
	@Override
	default String[] getExportedPackageNames(IModuleDescription targetModule) throws JavaModelException {
		IModule info = getModuleInfo();
		if (info != null) {
			List<String> result = new ArrayList<>();
			for (IPackageExport packageExport : info.exports()) {
				if (targetModule == null || !packageExport.isQualified()
						|| CharOperation.containsEqual(packageExport.targets(), targetModule.getElementName().toCharArray()))
				{
					result.add(new String(packageExport.name()));
				}
			}
			return result.toArray(new String[result.size()]);
		}
		return new String[0];
	}
	@Override
	default String[] getOpenedPackageNames(IModuleDescription targetModule) throws JavaModelException {
		IModule info = getModuleInfo();
		if (info != null) {
			List<String> result = new ArrayList<>();
			for (IPackageExport packageOpen : info.opens()) {
				if (targetModule == null || !packageOpen.isQualified()
						|| CharOperation.containsEqual(packageOpen.targets(), targetModule.getElementName().toCharArray()))
				{
					result.add(new String(packageOpen.name()));
				}
			}
			return result.toArray(new String[result.size()]);
		}
		return new String[0];
	}
	default IModuleReference[] getRequiredModules() throws JavaModelException {
		return getModuleInfo().requires();
	}
	default IPackageExport[] getExportedPackages() throws JavaModelException {
		return getModuleInfo().exports();
	}
	default IService[] getProvidedServices() throws JavaModelException {
		return getModuleInfo().provides();
	}
	@Override
	default String[] getProvidedServiceNames() throws JavaModelException {
		ArrayList<String> results = new ArrayList<>();
		IService[] services = getProvidedServices();
		for (IService service : services) {
			results.add(new String(service.name()));
		}
		return results.toArray(new String[0]);

	}
	default char[][] getUsedServices() throws JavaModelException {
		return getModuleInfo().uses();
	}
	@Override
	default String[] getUsedServiceNames() throws JavaModelException {
		ArrayList<String> results = new ArrayList<>();
		char[][] services = getUsedServices();
		for (int i = 0; i < services.length; ++i) {
			char[] service = services[i];
			results.add(new String(service));
		}
		return results.toArray(new String[0]);
	}
	default IPackageExport[] getOpenedPackages() throws JavaModelException {
		return getModuleInfo().opens();
	}
	@Override
	default String[] getRequiredModuleNames() throws JavaModelException {
		IModuleReference[] references = getRequiredModules();
		return Arrays.stream(references).map(ref -> String.valueOf(ref.name())).toArray(String[]::new);
	}

	default String toString(String lineDelimiter) {
		StringBuffer buffer = new StringBuffer();
		try {
			toStringContent(buffer, lineDelimiter);
		} catch (JavaModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return buffer.toString();
	}
	default void toStringContent(StringBuffer buffer, String lineDelimiter) throws JavaModelException {
		IPackageExport[] exports = getExportedPackages();
		IModuleReference[] requires = getRequiredModules();
		buffer.append("module "); //$NON-NLS-1$
		buffer.append(getElementName()).append(' ');
		buffer.append('{').append(lineDelimiter);
		if (exports != null) {
			for(int i = 0; i < exports.length; i++) {
				buffer.append("\texports "); //$NON-NLS-1$
				buffer.append(exports[i].toString());
				buffer.append(lineDelimiter);
			}
		}
		buffer.append(lineDelimiter);
		if (requires != null) {
			for(int i = 0; i < requires.length; i++) {
				buffer.append("\trequires "); //$NON-NLS-1$
				if (requires[i].isTransitive()) {
					buffer.append(" public "); //$NON-NLS-1$
				}
				buffer.append(requires[i].name());
				buffer.append(';').append(lineDelimiter);
			}
		}
		buffer.append(lineDelimiter).append('}').toString();
	}

	@Override
	default int getElementType() {
		return JAVA_MODULE;
	}
}
