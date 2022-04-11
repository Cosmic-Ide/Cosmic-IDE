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
package org.eclipse.jdt.internal.compiler.env;

import org.eclipse.jdt.internal.compiler.lookup.LookupEnvironment;
import org.eclipse.jdt.internal.compiler.lookup.ModuleBinding;

/**
 * This interface denotes a compilation unit, providing its name and content.
 *
 * <p>
 * Note: This internal interface has been implemented illegally by the
 * org.apache.jasper.glassfish bundle from Orbit, see
 * <a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=500211">bug 500211</a>.
 * Avoid changing the API or supply default methods to avoid breaking the Eclipse Help system.
 * </p>
 */
public interface ICompilationUnit extends IDependent {
/**
 * Answer the contents of the compilation unit.
 *
 * In normal use, the contents are requested twice.
 * Once during the initial lite parsing step, then again for the
 * more detailed parsing step.
 * Implementors must never return null - return an empty char[] instead,
 * CharOperation.NO_CHAR being the candidate of choice.
 */
char[] getContents();
/**
 * Answer the name of the top level public type.
 * For example, {Hashtable}.
 */
char[] getMainTypeName();
/**
 * Answer the name of the package according to the directory structure
 * or null if package consistency checks should be ignored.
 * For example, {java, lang}.
 */
char[][] getPackageName();
/**
* Answer if optional problems should be ignored for this compilation unit.
* Implementors should return <code>false</code> if there is no preference.
*/
default boolean ignoreOptionalProblems() {
	return false;
}
/**
 * Returns the binding of the module that this compilation unit is associated with.
 *
 * @return the binding representing the module.
 */
default ModuleBinding module(LookupEnvironment environment) {
	return environment.getModule(getModuleName());
}
/**
 * Returns the name of the module to which this compilation unit is associated.
 * A return value of {@code null} signals the unnamed module.
 * @return module name or {@code null} for the unnamed module.
 */
default char[] getModuleName() {
	return null;
}
default String getDestinationPath() {
	return null;
}
/**
 * Answers a path for external annotations that has been configured for
 * the providing classpath entry, or <code>null</code>.
 */
default String getExternalAnnotationPath(String qualifiedTypeName) { return null; }

}
