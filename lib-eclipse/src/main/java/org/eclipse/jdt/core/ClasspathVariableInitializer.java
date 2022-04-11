/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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

/**
 * Abstract base implementation of all classpath variable initializers.
 * Classpath variable initializers are used in conjunction with the
 * "org.eclipse.jdt.core.classpathVariableInitializer" extension point.
 * <p>
 * Clients should subclass this class to implement a specific classpath
 * variable initializer. The subclass must have a public 0-argument
 * constructor and a concrete implementation of <code>initialize</code>.
 *
 * @see IClasspathEntry
 * @since 2.0
 */
public abstract class ClasspathVariableInitializer {

    /**
     * Creates a new classpath variable initializer.
     */
    public ClasspathVariableInitializer() {
    	// a classpath variable initializer must have a public 0-argument constructor
    }

    /**
     * Binds a value to the workspace classpath variable with the given name,
     * or fails silently if this cannot be done.
     * <p>
     * A variable initializer is automatically activated whenever a variable value
     * is needed and none has been recorded so far. The implementation of
     * the initializer can set the corresponding variable using
     * <code>JavaCore#setClasspathVariable</code>.
     *
     * @param variable the name of the workspace classpath variable
     *    that requires a binding
     *
     * @see JavaCore#getClasspathVariable(String)
     * @see JavaCore#setClasspathVariable(String, org.eclipse.core.runtime.IPath, org.eclipse.core.runtime.IProgressMonitor)
     * @see JavaCore#setClasspathVariables(String[], org.eclipse.core.runtime.IPath[], org.eclipse.core.runtime.IProgressMonitor)
     */
    public abstract void initialize(String variable);
}
