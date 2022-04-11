/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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
package org.eclipse.jdt.core;

import org.eclipse.core.runtime.IPath;

/**
 * Interface of a classpath container.
 * A classpath container provides a way to indirectly reference a set of classpath entries through
 * a classpath entry of kind <code>CPE_CONTAINER</code>. Typically, a classpath container can
 * be used to describe a complex library composed of multiple JARs or projects, considering also
 * that containers can map to different set of entries on each project, in other words, several
 * projects can reference the same generic container path, but have each of them actually bound
 * to a different container object.
 * <p>
 * The set of entries associated with a classpath container may contain any of the following:
 * <ul>
 * <li> library entries (<code>CPE_LIBRARY</code>) </li>
 * <li> project entries (<code>CPE_PROJECT</code>) </li>
 * </ul>
 * In particular, a classpath container can neither reference further classpath containers or classpath variables.
 * <p>
 * A library entry can reference other libraries through the Class-Path section of the JAR's MANIFEST.MF file. If the
 * container wants such referenced entries to be part of the classpath, the container must explicitly add them to the
 * array returned from {@link #getClasspathEntries()}.
 * <p>
 * Classpath container values are persisted locally to the workspace, but are not preserved from a
 * session to another. It is thus highly recommended to register a <code>ClasspathContainerInitializer</code>
 * for each referenced container (through the extension point "org.eclipse.jdt.core.ClasspathContainerInitializer").
 * <p>
 * @see IClasspathEntry
 * @since 2.0
 */

public interface IClasspathContainer {

	/**
	 * Kind for a container mapping to an application library
	 */
	int K_APPLICATION = 1;

	/**
	 * Kind for a container mapping to a system library
	 */
	int K_SYSTEM = 2;

	/**
	 * Kind for a container mapping to a default system library, implicitly contributed by the runtime
	 */
	int K_DEFAULT_SYSTEM = 3;

	/**
	 * Answers the set of classpath entries this container is mapping to.
	 * <p>
	 * The set of entries associated with a classpath container may contain any of the following:
	 * <ul>
	 * <li> library entries (<code>CPE_LIBRARY</code>) </li>
	 * <li> project entries (<code>CPE_PROJECT</code>) </li>
	 * </ul>
	 * A classpath container can neither reference further classpath containers
	 * or classpath variables.
	 * <p>
	 * A library entry can reference other libraries through the Class-Path section of the JAR's MANIFEST.MF file. If
	 * the container wants such referenced entries to be part of the classpath, the container must explicitly add them
	 * to the result.
	 * <p>
	 * This method is called by the Java model when it needs to resolve this
	 * classpath container entry into a list of library and project entries.
	 * The method is typically called exactly once for a given Java project,
	 * and the resulting list of entries cached internally by the Java model.
	 * This method must not be called by other clients.
	 * <p>
	 * There are a wide variety of conditions under which this method may be
	 * invoked. To ensure that the implementation does not interfere with
	 * correct functioning of the Java model, the implementation should use
	 * only the following Java model APIs:
	 * <ul>
	 * <li>{@link JavaCore#newLibraryEntry(IPath, IPath, IPath, boolean)} and variants</li>
	 * <li>{@link JavaCore#newProjectEntry(IPath, boolean)} and variants</li>
	 * <li>{@link JavaCore#create(org.eclipse.core.resources.IWorkspaceRoot)}</li>
	 * <li>{@link JavaCore#create(org.eclipse.core.resources.IProject)}</li>
	 * <li>{@link JavaCore#getReferencedClasspathEntries(IClasspathEntry, IJavaProject)} with <code>null</code> as project</li>
	 * <li>{@link IJavaModel#getJavaProjects()}</li>
	 * <li>{@link IJavaProject#getRawClasspath()}</li>
	 * <li>{@link IJavaProject#readRawClasspath()}</li>
	 * <li>{@link IJavaProject#getOutputLocation()}</li>
	 * <li>{@link IJavaProject#readOutputLocation()}</li>
	 * <li>Java element operations marked as "handle-only"</li>
	 * </ul>
	 * <p>
	 * The effects of using other Java model APIs are unspecified.
	 * </p>
	 *
	 * @return IClasspathEntry[] - the classpath entries this container represents
	 * @see IClasspathEntry
	 */
    IClasspathEntry[] getClasspathEntries();

	/**
	 * Answers a readable description of this container
	 *
	 * @return String - a string description of the container
	 */
    String getDescription();

	/**
	 * Answers the kind of this container. Can be either:
	 * <ul>
	 * <li><code>K_APPLICATION</code> if this container maps to an application library</li>
	 * <li><code>K_SYSTEM</code> if this container maps to a system library</li>
	 * <li><code>K_DEFAULT_SYSTEM</code> if this container maps to a default system library (library
	 * 	implicitly contributed by the runtime).</li>
	 * </ul>
	 * Typically, system containers should be placed first on a build path.
	 * @return the kind of this container
	 */
    int getKind();

	/**
	 * Answers the container path identifying this container.
	 * A container path is formed by a first ID segment followed with extra segments, which
	 * can be used as additional hints for resolving to this container.
	 * <p>
	 * The container ID is also used to identify a<code>ClasspathContainerInitializer</code>
	 * registered on the extension point "org.eclipse.jdt.core.classpathContainerInitializer", which can
	 * be invoked if needing to resolve the container before it is explicitly set.
	 * <p>
	 * @return IPath - the container path that is associated with this container
	 */
    IPath getPath();
}

