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
 *     IBM Corporation - added support for requesting updates of a particular
 *                       container for generic container operations.
 * 						 - canUpdateClasspathContainer(IPath, IJavaProject)
 * 						 - requestClasspathContainerUpdate(IPath, IJavaProject, IClasspathContainer)
 *     IBM Corporation - allow initializers to provide a readable description
 *                       of a container reference, ahead of actual resolution.
 * 						 - getDescription(IPath, IJavaProject)
 *******************************************************************************/
package org.eclipse.jdt.core;

import org.eclipse.core.runtime.*;
import org.eclipse.jdt.internal.core.JavaModelStatus;

/**
 * Abstract base implementation of all classpath container initializer.
 * Classpath variable containers are used in conjunction with the
 * "org.eclipse.jdt.core.classpathContainerInitializer" extension point.
 * <p>
 * Clients should subclass this class to implement a specific classpath
 * container initializer. The subclass must have a public 0-argument
 * constructor and a concrete implementation of {@link #initialize(IPath, IJavaProject)}.
 * <p>
 * Multiple classpath containers can be registered, each of them declares
 * the container ID they can handle, so as to narrow the set of containers they
 * can resolve, in other words, a container initializer is guaranteed to only be
 * activated to resolve containers which match the ID they registered onto.
 * <p>
 * In case multiple container initializers collide on the same container ID, the first
 * registered one will be invoked.
 *
 * @see IClasspathEntry
 * @see IClasspathContainer
 * @since 2.0
 */
public abstract class ClasspathContainerInitializer {

	/**
	 * Status code indicating that an attribute is not supported.
	 *
	 * @see #getAccessRulesStatus(IPath, IJavaProject)
	 * @see #getAttributeStatus(IPath, IJavaProject, String)
	 * @see #getSourceAttachmentStatus(IPath, IJavaProject)
	 *
	 * @since 3.3
	 */
	public static final int ATTRIBUTE_NOT_SUPPORTED = 1;

	/**
	 * Status code indicating that an attribute is not modifiable.
	 *
	 * @see #getAccessRulesStatus(IPath, IJavaProject)
	 * @see #getAttributeStatus(IPath, IJavaProject, String)
	 * @see #getSourceAttachmentStatus(IPath, IJavaProject)
	 *
	 * @since 3.3
	 */
	public static final int ATTRIBUTE_READ_ONLY = 2;

   /**
     * Creates a new classpath container initializer.
     */
    public ClasspathContainerInitializer() {
    	// a classpath container initializer must have a public 0-argument constructor
    }

    /**
     * Binds a classpath container to a <code>IClasspathContainer</code> for a given project,
     * or silently fails if unable to do so.
     * <p>
     * A container is identified by a container path, which must be formed of two segments.
     * The first segment is used as a unique identifier (which this initializer did register onto), and
     * the second segment can be used as an additional hint when performing the resolution.
     * <p>
     * The initializer is invoked if a container path needs to be resolved for a given project, and no
     * value for it was recorded so far. The implementation of the initializer would typically set the
     * corresponding container using <code>JavaCore#setClasspathContainer</code>.
     * <p>
     * A container initialization can be indirectly performed while attempting to resolve a project
     * classpath using <code>IJavaProject#getResolvedClasspath(</code>; or directly when using
     * <code>JavaCore#getClasspathContainer</code>. During the initialization process, any attempt
     * to further obtain the same container will simply return <code>null</code> so as to avoid an
     * infinite regression of initializations.
     * <p>
     * A container initialization may also occur indirectly when setting a project classpath, as the operation
     * needs to resolve the classpath for validation purpose. While the operation is in progress, a referenced
     * container initializer may be invoked. If the initializer further tries to access the referring project classpath,
     * it will not see the new assigned classpath until the operation has completed. Note that once the Java
     * change notification occurs (at the end of the operation), the model has been updated, and the project
     * classpath can be queried normally.
	 * <p>
	 * This method is called by the Java model to give the party that defined
	 * this particular kind of classpath container the chance to install
	 * classpath container objects that will be used to convert classpath
	 * container entries into simpler classpath entries. The method is typically
	 * called exactly once for a given Java project and classpath container
	 * entry. This method must not be called by other clients.
	 * <p>
	 * There are a wide variety of conditions under which this method may be
	 * invoked. To ensure that the implementation does not interfere with
	 * correct functioning of the Java model, the implementation should use
	 * only the following Java model APIs:
	 * <ul>
	 * <li>{@link JavaCore#setClasspathContainer(IPath, IJavaProject[], IClasspathContainer[], org.eclipse.core.runtime.IProgressMonitor)}</li>
	 * <li>{@link JavaCore#getClasspathContainer(IPath, IJavaProject)}</li>
	 * <li>{@link JavaCore#create(org.eclipse.core.resources.IWorkspaceRoot)}</li>
	 * <li>{@link JavaCore#create(org.eclipse.core.resources.IProject)}</li>
	 * <li>{@link IJavaModel#getJavaProjects()}</li>
	 * <li>Java element operations marked as "handle-only"</li>
	 * </ul>
	 * The effects of using other Java model APIs are unspecified.
	 *
     * @param containerPath a two-segment path (ID/hint) identifying the container that needs
     * 	to be resolved
     * @param project the Java project in which context the container is to be resolved.
     *    This allows generic containers to be bound with project specific values.
     * @throws CoreException if an exception occurs during the initialization
     *
     * @see JavaCore#getClasspathContainer(IPath, IJavaProject)
     * @see JavaCore#setClasspathContainer(IPath, IJavaProject[], IClasspathContainer[], org.eclipse.core.runtime.IProgressMonitor)
     * @see IClasspathContainer
     */
    public abstract void initialize(IPath containerPath, IJavaProject project) throws CoreException;

    /**
     * Returns <code>true</code> if this container initializer can be requested to perform updates
     * on its own container values. If so, then an update request will be performed using
     * {@link #requestClasspathContainerUpdate(IPath, IJavaProject, IClasspathContainer)}.
     * <p>
     * @param containerPath the path of the container which requires to be updated
     * @param project the project for which the container is to be updated
     * @return returns <code>true</code> if the container can be updated
     * @since 2.1
     */
    public boolean canUpdateClasspathContainer(IPath containerPath, IJavaProject project) {

		// By default, classpath container initializers do not accept updating containers
    	return false;
    }

	/**
	 * Request a registered container definition to be updated according to a container suggestion. The container suggestion
	 * only acts as a place-holder to pass along the information to update the matching container definition(s) held by the
	 * container initializer. In particular, it is not expected to store the container suggestion as is, but rather adjust
	 * the actual container definition based on suggested changes.
	 * <p>
	 * IMPORTANT: In reaction to receiving an update request, a container initializer will update the corresponding
	 * container definition (after reconciling changes) at its earliest convenience, using
	 * {@link JavaCore#setClasspathContainer(IPath, IJavaProject[], IClasspathContainer[], IProgressMonitor)}.
	 * Until it does so, the update will not be reflected in the Java Model.
	 * <p>
	 * In order to anticipate whether the container initializer allows to update its containers, the predicate
	 * {@link #canUpdateClasspathContainer(IPath, IJavaProject)} should be used.
	 * <p>
	 * @param containerPath the path of the container which requires to be updated
     * @param project the project for which the container is to be updated
	 * @param containerSuggestion a suggestion to update the corresponding container definition
	 * @throws CoreException when <code>JavaCore#setClasspathContainer</code> would throw any.
	 * @see JavaCore#setClasspathContainer(IPath, IJavaProject[], IClasspathContainer[], org.eclipse.core.runtime.IProgressMonitor)
	 * @see ClasspathContainerInitializer#canUpdateClasspathContainer(IPath, IJavaProject)
	 * @since 2.1
	 */

    public void requestClasspathContainerUpdate(IPath containerPath, IJavaProject project, IClasspathContainer containerSuggestion) throws CoreException {

		// By default, classpath container initializers do not accept updating containers
    }

	/**
	 * Returns a readable description for a container path. A readable description for a container path can be
	 * used for improving the display of references to container, without actually needing to resolve them.
	 * A good implementation should answer a description consistent with the description of the associated
	 * target container (see {@link IClasspathContainer#getDescription()}).
	 *
	 * @param containerPath the path of the container which requires a readable description
	 * @param project the project from which the container is referenced
	 * @return a string description of the container
	 * @since 2.1
	 */
    public String getDescription(IPath containerPath, IJavaProject project) {

    	// By default, a container path is the only available description
    	return containerPath.makeRelative().toString();
    }

    /**
     * Returns a classpath container that is used after this initializer failed to bind a classpath container
     * to a {@link IClasspathContainer} for the given project. A non-<code>null</code>
     * failure container indicates that there will be no more request to initialize the given container
     * for the given project.
     * <p>
     * By default a non-<code>null</code> failure container with no classpath entries is returned.
     * Clients wishing to get a chance to run the initializer again should override this method
     * and return <code>null</code>.
     * </p>
     *
 	 * @param containerPath the path of the container which failed to initialize
	 * @param project the project from which the container is referenced
	 * @return the default failure container, or <code>null</code> if wishing to run the initializer again
     * @since 3.3
     */
    public IClasspathContainer getFailureContainer(final IPath containerPath, IJavaProject project) {
    	final String description = getDescription(containerPath, project);
    	return
    		new IClasspathContainer() {
				@Override
				public IClasspathEntry[] getClasspathEntries() {
					return new IClasspathEntry[0];
				}
				@Override
				public String getDescription() {
					return description;
				}
				@Override
				public int getKind() {
					return 0;
				}
				@Override
				public IPath getPath() {
					return containerPath;
				}
				@Override
				public String toString() {
					return getDescription();
				}
			};
	}

	/**
	 * Returns an object which identifies a container for comparison purpose. This allows
	 * to eliminate redundant containers when accumulating classpath entries (e.g.
	 * runtime classpath computation). When requesting a container comparison ID, one
	 * should ensure using its corresponding container initializer. Indeed, a random container
	 * initializer cannot be held responsible for determining comparison IDs for arbitrary
	 * containers.
	 * <p>
	 * @param containerPath the path of the container which is being checked
	 * @param project the project for which the container is to being checked
	 * @return returns an Object identifying the container for comparison
	 * @since 3.0
	 */
	public Object getComparisonID(IPath containerPath, IJavaProject project) {

		// By default, containers are identical if they have the same containerPath first segment,
		// but this may be refined by other container initializer implementations.
		if (containerPath == null) {
			return null;
		} else {
			return containerPath.segment(0);
		}
	}

	/**
	 * Returns the access rules attribute status according to this initializer.
	 * <p>
	 * The returned {@link IStatus status} can have one of the following severities:
	 * <ul>
	 * <li>{@link IStatus#OK OK}: means that the attribute is supported
	 * 	<strong>and</strong> is modifiable</li>
	 * <li>{@link IStatus#ERROR ERROR}: means that either the attribute
	 * 	is not supported or is not modifiable.<br>
	 * 	In this case, the {@link IStatus#getCode() code}will have
	 * 	respectively the {@link #ATTRIBUTE_NOT_SUPPORTED} value
	 * 	or the {@link #ATTRIBUTE_READ_ONLY} value.</li>
	 * </ul>
	 * <p>
	 * The status message can contain more information.
	 * </p><p>
	 * If the subclass does not override this method, then the default behavior is
	 * to return {@link IStatus#OK OK} if and only if the classpath container can
	 * be updated (see {@link #canUpdateClasspathContainer(IPath, IJavaProject)}).
	 * </p>
	 *
	 * @param containerPath the path of the container which requires to be
	 * 	updated
	 * @param project the project for which the container is to be updated
	 * @return returns the access rules attribute status
	 *
	 * @since 3.3
	 */
	public IStatus getAccessRulesStatus(IPath containerPath, IJavaProject project) {

		if (canUpdateClasspathContainer(containerPath, project)) {
			return Status.OK_STATUS;
		}
		return new JavaModelStatus(ATTRIBUTE_READ_ONLY);
	}

	/**
	 * Returns the extra attribute status according to this initializer.
	 * <p>
	 * The returned {@link IStatus status} can have one of the following severities:
	 * <ul>
	 * <li>{@link IStatus#OK OK}: means that the attribute is supported
	 * 	<strong>and</strong> is modifiable</li>
	 * <li>{@link IStatus#ERROR ERROR}: means that either the attribute
	 * 	is not supported or is not modifiable.<br>
	 * 	In this case, the {@link IStatus#getCode() code}will have
	 * 	respectively the {@link #ATTRIBUTE_NOT_SUPPORTED} value
	 * 	or the {@link #ATTRIBUTE_READ_ONLY} value.</li>
	 * </ul>
	 * <p>
	 * The status message can contain more information.
	 * </p><p>
	 * If the subclass does not override this method, then the default behavior is
	 * to return {@link IStatus#OK OK} if and only if the classpath container can
	 * be updated (see {@link #canUpdateClasspathContainer(IPath, IJavaProject)}).
	 * </p>
	 *
	 * @param containerPath the path of the container which requires to be
	 * 	updated
	 * @param project the project for which the container is to be updated
	 * @param attributeKey the key of the extra attribute
	 * @return returns the extra attribute status
	 * @see IClasspathAttribute
	 *
	 * @since 3.3
	 */
	public IStatus getAttributeStatus(IPath containerPath, IJavaProject project, String attributeKey) {

		if (canUpdateClasspathContainer(containerPath, project)) {
			return Status.OK_STATUS;
		}
		return new JavaModelStatus(ATTRIBUTE_READ_ONLY);
	}

	/**
	 * Returns the source attachment attribute status according to this initializer.
	 * <p>
	 * The returned {@link IStatus status} can have one of the following severities:
	 * <ul>
	 * <li>{@link IStatus#OK OK}: means that the attribute is supported
	 * 	<strong>and</strong> is modifiable</li>
	 * <li>{@link IStatus#ERROR ERROR}: means that either the attribute
	 * 	is not supported or is not modifiable.<br>
	 * 	In this case, the {@link IStatus#getCode() code}will have
	 * 	respectively the {@link #ATTRIBUTE_NOT_SUPPORTED} value
	 * 	or the {@link #ATTRIBUTE_READ_ONLY} value.</li>
	 * </ul>
	 * <p>
	 * The status message can contain more information.
	 * </p><p>
	 * If the subclass does not override this method, then the default behavior is
	 * to return {@link IStatus#OK OK} if and only if the classpath container can
	 * be updated (see {@link #canUpdateClasspathContainer(IPath, IJavaProject)}).
	 * </p>
	 *
	 * @param containerPath the path of the container which requires to be
	 * 	updated
	 * @param project the project for which the container is to be updated
	 * @return returns the source attachment attribute status
	 *
	 * @since 3.3
	 */
	public IStatus getSourceAttachmentStatus(IPath containerPath, IJavaProject project) {

		if (canUpdateClasspathContainer(containerPath, project)) {
			return Status.OK_STATUS;
		}
		return new JavaModelStatus(ATTRIBUTE_READ_ONLY);
	}
}

