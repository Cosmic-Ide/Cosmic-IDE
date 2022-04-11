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

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.core.builder.ClasspathJMod;
import org.eclipse.jdt.internal.core.util.HashtableOfArrayToObject;

/**
 * A package fragment root that corresponds to a JMod file.
 *
 * <p>NOTE: The only visible entries from a Jmod package fragment root
 * are .class files. The sub folder "classes" where the .class files are nested under
 * is hidden from clients. THe package fragments appear to be directly under the
 * package fragment roots.
 * <p>NOTE: A JMod package fragment root may or may not have an associated resource.
 *
 * @see org.eclipse.jdt.core.IPackageFragmentRoot
 * @see org.eclipse.jdt.internal.core.JarPackageFragmentRootInfo
 */
public class JModPackageFragmentRoot extends JarPackageFragmentRoot {

	/**
	 * Constructs a package fragment root which is the root of the Java package directory hierarchy
	 * based on a JMOD file that is not contained in a <code>IJavaProject</code> and
	 * does not have an associated <code>IResource</code>.
	 */
	protected JModPackageFragmentRoot(IPath externalPath, JavaProject project, IClasspathAttribute[] extraAttributes) {
		super(null, externalPath, project, extraAttributes);
	}

	/**
	 * @see PackageFragmentRoot#getClassFilePath(String)
	 */
	@Override
	public String getClassFilePath(String entryName) {
		char[] name = CharOperation.append(ClasspathJMod.CLASSES_FOLDER, entryName.toCharArray());
		return new String(name);
	}
	@Override
	protected void initRawPackageInfo(HashtableOfArrayToObject rawPackageInfo, String entryName, boolean isDirectory, String compliance) {
		char[] name = entryName.toCharArray();
		if (CharOperation.prefixEquals(ClasspathJMod.CLASSES_FOLDER, name)) {
			name = CharOperation.subarray(name, ClasspathJMod.CLASSES_FOLDER.length, name.length);
		}
		super.initRawPackageInfo(rawPackageInfo, new String(name), isDirectory, compliance);
	}
}
