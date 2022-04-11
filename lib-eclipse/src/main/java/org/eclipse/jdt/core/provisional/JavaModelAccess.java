/*******************************************************************************
 * Copyright (c) 2017, 2019 GK Software SE, and others.
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
package org.eclipse.jdt.core.provisional;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IModuleDescription;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.JavaProject;
import org.eclipse.jdt.internal.core.JrtPackageFragmentRoot;
import org.eclipse.jdt.internal.core.PackageFragmentRoot;

/**
 * Provisional API for use by JDT/UI or JDT/Debug, which may possibly be removed in a future version.
 * See <a href="https://bugs.eclipse.org/522391">Bug 522391</a>.
 */
public class JavaModelAccess {
	/**
	 * Answer the names of all modules directly required from the given module.
	 * @param module the module whose "requires" directives are queried
	 * @return a non-null array of module names
	 * @deprecated this provisional API has been promoted to {@link IModuleDescription#getRequiredModuleNames()}
	 */
	@Deprecated
	public static String[] getRequiredModules(IModuleDescription module) throws JavaModelException {
		return module.getRequiredModuleNames();
	}

	/**
	 * Returns the <code>IModuleDescription</code> that the given java element contains
	 * when regarded as an automatic module. The element must be an <code>IPackageFragmentRoot</code>
	 * or an <code>IJavaProject</code>.
	 *
	 * <p>The returned module descriptor has a name (<code>getElementName()</code>) following
	 * the specification of <code>java.lang.module.ModuleFinder.of(Path...)</code>, but it
	 * contains no other useful information.</p>
	 *
	 * @return the <code>IModuleDescription</code> representing this java element as an automatic module,
	 * 		never <code>null</code>.
	 * @throws JavaModelException
	 * @throws IllegalArgumentException if the provided element is neither <code>IPackageFragmentRoot</code>
	 * 	nor <code>IJavaProject</code>
	 * @since 3.14
	 */
	public static IModuleDescription getAutomaticModuleDescription(IJavaElement element) throws JavaModelException, IllegalArgumentException {
		switch (element.getElementType()) {
			case IJavaElement.JAVA_PROJECT:
				return ((JavaProject) element).getAutomaticModuleDescription();
			case IJavaElement.PACKAGE_FRAGMENT_ROOT:
				return ((PackageFragmentRoot) element).getAutomaticModuleDescription();
			default:
				throw new IllegalArgumentException("Illegal kind of java element: "+element.getElementType()); //$NON-NLS-1$
		}
	}

	/**
	 * Answer whether the given module is a system module.
	 * <p><em>This provisional API may likely be changed into a direct method {@code IModuleDescription.isSystemModule()}.</em></p>
	 *
	 * @param module the module being queried about
	 * @return {@code true} iff the module is defined in the system library (also known as JRE).
	 * @since 3.18
	 * @deprecated please use {@link IModuleDescription#isSystemModule()}
	 */
	@Deprecated
	public static boolean isSystemModule(IModuleDescription module) {
		IPackageFragmentRoot pfr = (IPackageFragmentRoot) module.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
		return pfr instanceof JrtPackageFragmentRoot;
	}
}
