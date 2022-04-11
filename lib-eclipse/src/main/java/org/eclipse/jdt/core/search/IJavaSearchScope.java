/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
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
package org.eclipse.jdt.core.search;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;

/**
 * An <code>IJavaSearchScope</code> defines where search result should be found by a
 * <code>SearchEngine</code>. Clients must pass an instance of this interface
 * to the <code>search(...)</code> methods. Such an instance can be created
 * using the following factory methods: {@link SearchEngine#createHierarchyScope(IType)},
 * {@link SearchEngine#createJavaSearchScope(IJavaElement[])}, {@link SearchEngine#createWorkspaceScope()} or clients
 * may choose to implement this interface.
 */
public interface IJavaSearchScope {
/**
 * This constant defines the separator of the resourcePath string of the <code>encloses(String)</code>
 * method. If present in the string, it separates the path to the jar file from the path
 * to the .class file in the jar.
 */
String JAR_FILE_ENTRY_SEPARATOR = "|"; //$NON-NLS-1$
/**
 * Include type constant (bit mask) indicating that source folders should be considered in the search scope.
 * @since 3.0
 */
int SOURCES = 1;
/**
 * Include type constant (bit mask) indicating that application libraries should be considered in the search scope.
 * @since 3.0
 */
int APPLICATION_LIBRARIES = 2;
/**
 * Include type constant (bit mask) indicating that system libraries should be considered in the search scope.
 * @since 3.0
 */
int SYSTEM_LIBRARIES = 4;
/**
 * Include type constant (bit mask) indicating that referenced projects should be considered in the search scope.
 * @since 3.0
 */
int REFERENCED_PROJECTS = 8;
/**
 * Checks whether the resource at the given path is enclosed by this scope.
 *
 * @param resourcePath if the resource is contained in
 * a JAR file, the path is composed of 2 paths separated
 * by <code>JAR_FILE_ENTRY_SEPARATOR</code>: the first path is the full OS path
 * to the JAR (if it is an external JAR), or the workspace relative <code>IPath</code>
 * to the JAR (if it is an internal JAR),
 * the second path is the path to the resource inside the JAR.
 * @return whether the resource is enclosed by this scope
 */
public boolean encloses(String resourcePath);
/**
 * Checks whether this scope encloses the given element.
 *
 * @param element the given element
 * @return <code>true</code> if the element is in this scope
 */
public boolean encloses(IJavaElement element);
/**
 * Returns the paths to the enclosing projects and JARs for this search scope.
 * <ul>
 * <li> If the path is a project path, this is the full path of the project
 *       (see <code>IResource.getFullPath()</code>).
 *        For example, /MyProject
 * </li>
 * <li> If the path is a JAR path and this JAR is internal to the workspace,
 *        this is the full path of the JAR file (see <code>IResource.getFullPath()</code>).
 *        For example, /MyProject/mylib.jar
 * </li>
 * <li> If the path is a JAR path and this JAR is external to the workspace,
 *        this is the full OS path to the JAR file on the file system.
 *        For example, d:\libs\mylib.jar
 * </li>
 * </ul>
 *
 * @return an array of paths to the enclosing projects and JARS.
 */
IPath[] enclosingProjectsAndJars();
/**
 * Returns whether this scope contains any <code>.class</code> files (either
 * in folders or within JARs).
 *
 * @return whether this scope contains any <code>.class</code> files
 * @deprecated Use
 * {@link org.eclipse.jdt.core.search.SearchEngine#createJavaSearchScope(IJavaElement[])}
 * with the package fragment roots that correspond to the binaries instead.
 */
boolean includesBinaries();
/**
 * Returns whether this scope includes classpaths defined by
 * the projects of the resources of this search scope.
 *
 * @return whether this scope includes classpaths
 * @deprecated Use
 * {@link org.eclipse.jdt.core.search.SearchEngine#createJavaSearchScope(IJavaElement[])}
 * with a Java project instead.
 */
boolean includesClasspaths();
/**
 * Sets whether this scope contains any <code>.class</code> files (either
 * in folders or within JARs).
 *
 * @param includesBinaries whether this scope contains any <code>.class</code> files
 * @deprecated Use
 * {@link org.eclipse.jdt.core.search.SearchEngine#createJavaSearchScope(IJavaElement[])}
 * with the package fragment roots that correspond to the binaries instead.
 */
public void setIncludesBinaries(boolean includesBinaries);
/**
 * Sets whether this scope includes the classpaths defined by
 * the projects of the resources of this search scope.
 *
 * @param includesClasspaths whether this scope includes classpaths
 * @deprecated Use
 * {@link org.eclipse.jdt.core.search.SearchEngine#createJavaSearchScope(IJavaElement[])}
 * with a Java project instead.
 */
public void setIncludesClasspaths(boolean includesClasspaths);
}
