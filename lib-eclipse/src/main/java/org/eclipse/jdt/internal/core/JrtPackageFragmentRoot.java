/*******************************************************************************
 * Copyright (c) 2015, 2019 IBM Corporation and others.
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

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaModelStatusConstants;
import org.eclipse.jdt.core.IModuleDescription;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.env.IModule;
import org.eclipse.jdt.internal.compiler.env.IModulePathEntry;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.util.JRTUtil;
import org.eclipse.jdt.internal.core.util.HashtableOfArrayToObject;
import org.eclipse.jdt.internal.core.util.Util;

/**
 * A package fragment root that corresponds to a module in a JRT file system.
 *
 * @see org.eclipse.jdt.core.IPackageFragmentRoot
 * @see org.eclipse.jdt.internal.core.JarPackageFragmentRootInfo
 */
public class JrtPackageFragmentRoot extends JarPackageFragmentRoot implements IModulePathEntry {

	String moduleName;

	public static final ThreadLocal<Boolean> workingOnOldClasspath = new ThreadLocal<>();

	/**
	 * Constructs a package fragment root which represents a module
	 * contained in a JRT.
	 */
	protected JrtPackageFragmentRoot(IPath jrtPath, String moduleName, JavaProject project, IClasspathAttribute[] extraAttributes) {
		super(null, jrtPath, project, extraAttributes);
		this.moduleName = moduleName;
	}

	@Override
	protected boolean computeChildren(OpenableElementInfo info, IResource underlyingResource) throws JavaModelException {
		final HashtableOfArrayToObject rawPackageInfo = new HashtableOfArrayToObject();
		final String compliance = CompilerOptions.VERSION_1_8; // TODO: Java 9 Revisit

		// always create the default package
		rawPackageInfo.put(CharOperation.NO_STRINGS, new ArrayList[] { EMPTY_LIST, EMPTY_LIST });

		try {
			org.eclipse.jdt.internal.compiler.util.JRTUtil.walkModuleImage(this.jarPath.toFile(),
					new org.eclipse.jdt.internal.compiler.util.JRTUtil.JrtFileVisitor<Path>() {
				@Override
				public FileVisitResult visitPackage(Path dir, Path mod, BasicFileAttributes attrs) throws IOException {
					initRawPackageInfo(rawPackageInfo, dir.toString(), true, compliance);
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFile(Path path, Path mod, BasicFileAttributes attrs) throws IOException {
					initRawPackageInfo(rawPackageInfo, path.toString(), false, compliance);
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitModule(Path path, String name) throws IOException {
					if (!JrtPackageFragmentRoot.this.moduleName.equals(name)) {
						return FileVisitResult.SKIP_SUBTREE;
					}
					return FileVisitResult.CONTINUE;
				}
			}, JRTUtil.NOTIFY_ALL);
		} catch (IOException e) {
			Util.log(IStatus.ERROR, "Error reading modules" + toStringWithAncestors()); //$NON-NLS-1$
		}

		info.setChildren(createChildren(rawPackageInfo));
		((JarPackageFragmentRootInfo) info).rawPackageInfo = rawPackageInfo;
		return true;
	}
	@Override
	SourceMapper createSourceMapper(IPath sourcePath, IPath rootPath) throws JavaModelException {
		IClasspathEntry entry = ((JavaProject) getParent()).getClasspathEntryFor(getPath());
		String encoding = (entry== null) ? null : ((ClasspathEntry) entry).getSourceAttachmentEncoding();
		IModule mod = getModule();
		String modName = mod == null ? null : new String(mod.name());
		SourceMapper mapper = new SourceMapper(
			sourcePath,
			rootPath == null ? modName : rootPath.toOSString(),
			getJavaProject().getOptions(true),// cannot use workspace options if external jar is 1.5 jar and workspace options are 1.4 options
			encoding);
		return mapper;
	}
	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o instanceof JrtPackageFragmentRoot) {
			JrtPackageFragmentRoot other= (JrtPackageFragmentRoot) o;
			return this.moduleName.equals(other.moduleName) &&
					this.jarPath.equals(other.jarPath) &&
					Arrays.equals(this.extraAttributes, other.extraAttributes);
		}
		return false;
	}
	@Override
	public String getElementName() {
		return this.moduleName;
	}
	@Override
	public PackageFragment getPackageFragment(String[] pkgName) {
		// NOTE: Do we need a different kind of package fragment?
		return new JarPackageFragment(this, pkgName);
	}
	@Override
	public int hashCode() {
		int hash = 31;
		hash = Util.combineHashCodes(hash, this.jarPath.hashCode());
		hash = Util.combineHashCodes(hash, this.moduleName.hashCode());
		hash = Util.combineHashCodes(hash, Arrays.hashCode(this.extraAttributes));
		return hash;
	}
	@Override
	protected void toStringInfo(int tab, StringBuffer buffer, Object info, boolean showResolvedInfo) {
		buffer.append(tabString(tab));
		buffer.append("<module:").append(this.moduleName).append(">"); //$NON-NLS-1$ //$NON-NLS-2$
		if (info == null) {
			buffer.append(" (not open)"); //$NON-NLS-1$
		}
	}

	@Override
	public IModule getModule() {
		IModuleDescription desc = getModuleDescription();
		if (desc != null) {
			try {
				return (IModule)((JavaElement) desc).getElementInfo();
			} catch (JavaModelException e) {
				Util.log(e);
			}
		}
		return null;
	}

	@Override
	protected boolean isComplianceJava9OrHigher() {
		return true;
	}

	@Override
	public char[][] getModulesDeclaringPackage(String qualifiedPackageName, String requestedModuleName) {
		if (requestedModuleName != null && !requestedModuleName.equals(this.moduleName))
			return null;
		if (getPackageFragment(qualifiedPackageName).exists()) {
			return new char[][] { requestedModuleName.toCharArray() };
		}
		return null;
	}
	@Override
	protected boolean ignoreErrorStatus(IStatus status) {
		if (status.getCode() == IJavaModelStatusConstants.ELEMENT_NOT_ON_CLASSPATH
				&& workingOnOldClasspath.get() == Boolean.TRUE)
			return true;
		return false;
	}
}
