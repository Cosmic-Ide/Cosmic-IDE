/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModelStatus;
import org.eclipse.jdt.core.IJavaModelStatusConstants;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.util.Messages;

/**
 * @see IJavaModelStatus
 */

public class JavaModelStatus extends Status implements IJavaModelStatus, IJavaModelStatusConstants {

	/**
	 * The elements related to the failure, or <code>null</code>
	 * if no elements are involved.
	 */
	protected IJavaElement[] elements = new IJavaElement[0];
	/**
	 * The path related to the failure, or <code>null</code>
	 * if no path is involved.
	 */
	protected IPath path;
	/**
	 * The <code>String</code> related to the failure, or <code>null</code>
	 * if no <code>String</code> is involved.
	 */
	protected String string;
	/**
	 * Empty children
	 */
	protected final static IStatus[] NO_CHILDREN = new IStatus[] {};
	protected IStatus[] children= NO_CHILDREN;

	/**
	 * Singleton OK object
	 */
	public static final IJavaModelStatus VERIFIED_OK = new JavaModelStatus(OK, OK, Messages.status_OK);

	/**
	 * Constructs an Java model status with no corresponding elements.
	 */
	public JavaModelStatus() {
		// no code for an multi-status
		super(ERROR, JavaCore.PLUGIN_ID, 0, "JavaModelStatus", null); //$NON-NLS-1$
	}
	/**
	 * Constructs an Java model status with no corresponding elements.
	 */
	public JavaModelStatus(int code) {
		super(ERROR, JavaCore.PLUGIN_ID, code, "JavaModelStatus", null); //$NON-NLS-1$
		this.elements= JavaElement.NO_ELEMENTS;
	}
	/**
	 * Constructs an Java model status with the given corresponding
	 * elements.
	 */
	public JavaModelStatus(int code, IJavaElement[] elements) {
		super(ERROR, JavaCore.PLUGIN_ID, code, "JavaModelStatus", null); //$NON-NLS-1$
		this.elements= elements;
		this.path= null;
	}
	/**
	 * Constructs an Java model status with no corresponding elements.
	 */
	public JavaModelStatus(int code, String string) {
		this(ERROR, code, string);
	}
	/**
	 * Constructs an Java model status with no corresponding elements.
	 */
	public JavaModelStatus(int severity, int code, String string) {
		super(severity, JavaCore.PLUGIN_ID, code, "JavaModelStatus", null); //$NON-NLS-1$
		this.elements= JavaElement.NO_ELEMENTS;
		this.path= null;
		this.string = string;
	}
	/**
	 * Constructs an Java model status with no corresponding elements.
	 */
	public JavaModelStatus(int code, Throwable throwable) {
		super(ERROR, JavaCore.PLUGIN_ID, code, "JavaModelStatus", throwable); //$NON-NLS-1$
		this.elements= JavaElement.NO_ELEMENTS;
	}
	/**
	 * Constructs an Java model status with no corresponding elements.
	 */
	public JavaModelStatus(int code, IPath path) {
		super(ERROR, JavaCore.PLUGIN_ID, code, "JavaModelStatus", null); //$NON-NLS-1$
		this.elements= JavaElement.NO_ELEMENTS;
		this.path= path;
	}
	/**
	 * Constructs an Java model status with the given corresponding
	 * element.
	 */
	public JavaModelStatus(int code, IJavaElement element) {
		this(code, new IJavaElement[]{element});
	}
	/**
	 * Constructs an Java model status with the given corresponding
	 * element and string
	 */
	public JavaModelStatus(int code, IJavaElement element, String string) {
		this(code, new IJavaElement[]{element});
		this.string = string;
	}

	/**
	 * Constructs an Java model status with the given corresponding
	 * element and path
	 */
	public JavaModelStatus(int code, IJavaElement element, IPath path) {
		this(code, new IJavaElement[]{element});
		this.path = path;
	}

	/**
	 * Constructs an Java model status with the given corresponding
	 * element, path and string
	 */
	public JavaModelStatus(int code, IJavaElement element, IPath path, String string) {
		this(code, new IJavaElement[]{element});
		this.path = path;
		this.string = string;
	}

	/**
     * Constructs an Java model status with the given corresponding
     * element and path
     */
    public JavaModelStatus(int severity, int code, IJavaElement element, IPath path, String msg) {
    	super(severity, JavaCore.PLUGIN_ID, code, "JavaModelStatus", null); //$NON-NLS-1$
    	this.elements= new IJavaElement[]{element};
    	this.path = path;
    	this.string = msg;
    }

    /**
	 * Constructs an Java model status with no corresponding elements.
	 */
	public JavaModelStatus(CoreException coreException) {
		super(ERROR, JavaCore.PLUGIN_ID, CORE_EXCEPTION, "JavaModelStatus", coreException); //$NON-NLS-1$
		this.elements= JavaElement.NO_ELEMENTS;
	}
	protected int getBits() {
		int severity = 1 << (getCode() % 100 / 33);
		int category = 1 << ((getCode() / 100) + 3);
		return severity | category;
	}
	/**
	 * @see IStatus
	 */
	@Override
	public IStatus[] getChildren() {
		return this.children;
	}
	/**
	 * @see IJavaModelStatus
	 */
	@Override
	public IJavaElement[] getElements() {
		return this.elements;
	}
	/**
	 * Returns the message that is relevant to the code of this status.
	 */
	@Override
	public String getMessage() {
		Throwable exception = getException();
		if (exception == null) {
			switch (getCode()) {
				case CORE_EXCEPTION :
					return Messages.status_coreException;

				case BUILDER_INITIALIZATION_ERROR:
					return Messages.build_initializationError;

				case BUILDER_SERIALIZATION_ERROR:
					return Messages.build_serializationError;

				case DEVICE_PATH:
					return Messages.bind(Messages.status_cannotUseDeviceOnPath, getPath().toString());

				case DOM_EXCEPTION:
					return Messages.status_JDOMError;

				case ELEMENT_DOES_NOT_EXIST:
					return Messages.bind(Messages.element_doesNotExist, ((JavaElement)this.elements[0]).toStringWithAncestors());

				case ELEMENT_NOT_ON_CLASSPATH:
					return Messages.bind(Messages.element_notOnClasspath, ((JavaElement)this.elements[0]).toStringWithAncestors());

				case EVALUATION_ERROR:
					return Messages.bind(Messages.status_evaluationError, this.string);

				case INDEX_OUT_OF_BOUNDS:
					return Messages.status_indexOutOfBounds;

				case INVALID_CONTENTS:
					return Messages.status_invalidContents;

				case INVALID_DESTINATION:
					return Messages.bind(Messages.status_invalidDestination, ((JavaElement)this.elements[0]).toStringWithAncestors());

				case INVALID_ELEMENT_TYPES:
					StringBuilder buff= new StringBuilder(Messages.operation_notSupported);
					for (int i= 0; i < this.elements.length; i++) {
						if (i > 0) {
							buff.append(", "); //$NON-NLS-1$
						}
						buff.append(((JavaElement)this.elements[i]).toStringWithAncestors());
					}
					return buff.toString();

				case INVALID_NAME:
					return Messages.bind(Messages.status_invalidName, this.string);

				case INVALID_PACKAGE:
					return Messages.bind(Messages.status_invalidPackage, this.string);

				case INVALID_PATH:
					if (this.string != null) {
						return this.string;
					} else {
						return Messages.bind(
							Messages.status_invalidPath,
							new String[] {getPath() == null ? "null" : getPath().toString()} //$NON-NLS-1$
						);
					}

				case INVALID_PROJECT:
					return Messages.bind(Messages.status_invalidProject, this.string);

				case INVALID_RESOURCE:
					return Messages.bind(Messages.status_invalidResource, this.string);

				case INVALID_RESOURCE_TYPE:
					return Messages.bind(Messages.status_invalidResourceType, this.string);

				case INVALID_SIBLING:
					if (this.string != null) {
						return Messages.bind(Messages.status_invalidSibling, this.string);
					} else {
						return Messages.bind(Messages.status_invalidSibling, ((JavaElement)this.elements[0]).toStringWithAncestors());
					}

				case IO_EXCEPTION:
					return Messages.status_IOException;

				case NAME_COLLISION:
					if (this.elements != null && this.elements.length > 0) {
						IJavaElement element = this.elements[0];
						if (element instanceof PackageFragment && ((PackageFragment) element).isDefaultPackage()) {
							return Messages.operation_cannotRenameDefaultPackage;
						}
					}
					if (this.string != null) {
						return this.string;
					} else {
						return Messages.bind(Messages.status_nameCollision, "");  //$NON-NLS-1$
					}
				case NO_ELEMENTS_TO_PROCESS:
					return Messages.operation_needElements;

				case NULL_NAME:
					return Messages.operation_needName;

				case NULL_PATH:
					return Messages.operation_needPath;

				case NULL_STRING:
					return Messages.operation_needString;

				case PATH_OUTSIDE_PROJECT:
					return Messages.bind(Messages.operation_pathOutsideProject, new String[] {this.string, ((JavaElement)this.elements[0]).toStringWithAncestors()});

				case READ_ONLY:
					IJavaElement element = this.elements[0];
					String name = element.getElementName();
					if (element instanceof IPackageFragment && name.equals(IPackageFragment.DEFAULT_PACKAGE_NAME)) {
						return Messages.status_defaultPackageReadOnly;
					}
					return Messages.bind(Messages.status_readOnly, name);

				case RELATIVE_PATH:
					return Messages.bind(Messages.operation_needAbsolutePath, getPath().toString());

				case TARGET_EXCEPTION:
					return Messages.status_targetException;

				case UPDATE_CONFLICT:
					return Messages.status_updateConflict;

				case NO_LOCAL_CONTENTS :
					return Messages.bind(Messages.status_noLocalContents, getPath().toString());

				case CP_CONTAINER_PATH_UNBOUND:
					IJavaProject javaProject = (IJavaProject)this.elements[0];
					ClasspathContainerInitializer initializer = JavaCore.getClasspathContainerInitializer(this.path.segment(0));
					String description = null;
					if (initializer != null) description = initializer.getDescription(this.path, javaProject);
					if (description == null) description = this.path.makeRelative().toString();
					return Messages.bind(Messages.classpath_unboundContainerPath, new String[] {description, javaProject.getElementName()});

				case INVALID_CP_CONTAINER_ENTRY:
					javaProject = (IJavaProject)this.elements[0];
					IClasspathContainer container = null;
					description = null;
					try {
						container = JavaCore.getClasspathContainer(this.path, javaProject);
					} catch(JavaModelException e){
						// project doesn't exist: ignore
					}
					if (container == null) {
						 initializer = JavaCore.getClasspathContainerInitializer(this.path.segment(0));
						if (initializer != null) description = initializer.getDescription(this.path, javaProject);
					} else {
						description = container.getDescription();
					}
					if (description == null) description = this.path.makeRelative().toString();
					return Messages.bind(Messages.classpath_invalidContainer, new String[] {description, javaProject.getElementName()});

				case CP_VARIABLE_PATH_UNBOUND:
					javaProject = (IJavaProject)this.elements[0];
					return Messages.bind(Messages.classpath_unboundVariablePath, new String[] {this.path.makeRelative().toString(), javaProject.getElementName()});

				case CLASSPATH_CYCLE:
					javaProject = (IJavaProject)this.elements[0];
					return Messages.bind(Messages.classpath_cycle, new String[] {javaProject.getElementName(), this.string});

				case DISABLED_CP_EXCLUSION_PATTERNS:
					javaProject = (IJavaProject)this.elements[0];
					String projectName = javaProject.getElementName();
					IPath newPath = this.path;
					if (this.path.segment(0).toString().equals(projectName)) {
						newPath = this.path.removeFirstSegments(1);
					}
					return Messages.bind(Messages.classpath_disabledInclusionExclusionPatterns, new String[] {newPath.makeRelative().toString(), projectName});

				case DISABLED_CP_MULTIPLE_OUTPUT_LOCATIONS:
					javaProject = (IJavaProject)this.elements[0];
					projectName = javaProject.getElementName();
					newPath = this.path;
					if (this.path.segment(0).toString().equals(projectName)) {
						newPath = this.path.removeFirstSegments(1);
					}
					return Messages.bind(Messages.classpath_disabledMultipleOutputLocations, new String[] {newPath.makeRelative().toString(), projectName});

				case CANNOT_RETRIEVE_ATTACHED_JAVADOC :
					if (this.elements != null && this.elements.length == 1) {
						if (this.string != null) {
							return Messages.bind(Messages.status_cannot_retrieve_attached_javadoc, ((JavaElement)this.elements[0]).toStringWithAncestors(), this.string);
						}
						return Messages.bind(Messages.status_cannot_retrieve_attached_javadoc, ((JavaElement)this.elements[0]).toStringWithAncestors(), ""); //$NON-NLS-1$
					}
					if (this.string != null) {
						return Messages.bind(Messages.status_cannot_retrieve_attached_javadoc, this.string, "");//$NON-NLS-1$
					}
					break;

				case CANNOT_RETRIEVE_ATTACHED_JAVADOC_TIMEOUT :
					if (this.elements != null && this.elements.length == 1) {
						if (this.string != null) {
							return Messages.bind(Messages.status_timeout_javadoc, ((JavaElement)this.elements[0]).toStringWithAncestors(), this.string);
						}
						return Messages.bind(Messages.status_timeout_javadoc, ((JavaElement)this.elements[0]).toStringWithAncestors(), ""); //$NON-NLS-1$
					}
					if (this.string != null) {
						return Messages.bind(Messages.status_timeout_javadoc, this.string, "");//$NON-NLS-1$
					}
					break;

				case UNKNOWN_JAVADOC_FORMAT :
					return Messages.bind(Messages.status_unknown_javadoc_format, ((JavaElement)this.elements[0]).toStringWithAncestors());

				case DEPRECATED_VARIABLE :
					javaProject = (IJavaProject)this.elements[0];
					return Messages.bind(Messages.classpath_deprecated_variable, new String[] {this.path.segment(0).toString(), javaProject.getElementName(), this.string});
				case TEST_SOURCE_REQUIRES_SEPARATE_OUTPUT_LOCATION:
					javaProject = (IJavaProject)this.elements[0];
					projectName = javaProject.getElementName();
					newPath = this.path;
					if (this.path.segment(0).toString().equals(projectName)) {
						newPath = this.path.removeFirstSegments(1);
					}
					return Messages.bind(Messages.classpath_testSourceRequiresSeparateOutputFolder, new String[] {newPath.makeRelative().toString(), projectName});
				case TEST_OUTPUT_FOLDER_MUST_BE_SEPARATE_FROM_MAIN_OUTPUT_FOLDERS:
					javaProject = (IJavaProject)this.elements[0];
					projectName = javaProject.getElementName();
					newPath = this.path;
					if (this.path.segment(0).toString().equals(projectName)) {
						newPath = this.path.removeFirstSegments(1);
					}
					return Messages.bind(Messages.classpath_testOutputFolderMustBeSeparateFromMainOutputFolders, new String[] {newPath.makeRelative().toString(), projectName});
			}
			if (this.string != null) {
				return this.string;
			} else {
				return ""; //$NON-NLS-1$
			}
		} else {
			String message = exception.getMessage();
			if (message != null) {
				return message;
			} else {
				return exception.toString();
			}
		}
	}
	/**
	 * @see IJavaModelStatus#getPath()
	 */
	@Override
	public IPath getPath() {
		return this.path;
	}
	/**
	 * @see IStatus#getSeverity()
	 */
	@Override
	public int getSeverity() {
		if (this.children == NO_CHILDREN) return super.getSeverity();
		int severity = -1;
		for (int i = 0, max = this.children.length; i < max; i++) {
			int childrenSeverity = this.children[i].getSeverity();
			if (childrenSeverity > severity) {
				severity = childrenSeverity;
			}
		}
		return severity;
	}
	/**
	 * @see IJavaModelStatus#getString()
	 * @deprecated
	 */
	@Override
	public String getString() {
		return this.string;
	}
	/**
	 * @see IJavaModelStatus#isDoesNotExist()
	 */
	@Override
	public boolean isDoesNotExist() {
		int code = getCode();
		return code == ELEMENT_DOES_NOT_EXIST || code == ELEMENT_NOT_ON_CLASSPATH;
	}
	/**
	 * @see IStatus#isMultiStatus()
	 */
	@Override
	public boolean isMultiStatus() {
		return this.children != NO_CHILDREN;
	}
	/**
	 * @see IStatus#isOK()
	 */
	@Override
	public boolean isOK() {
		return getCode() == OK;
	}
	/**
	 * @see IStatus#matches(int)
	 */
	@Override
	public boolean matches(int mask) {
		if (! isMultiStatus()) {
			return matches(this, mask);
		} else {
			for (int i = 0, max = this.children.length; i < max; i++) {
				if (matches((JavaModelStatus) this.children[i], mask))
					return true;
			}
			return false;
		}
	}
	/**
	 * Helper for matches(int).
	 */
	protected boolean matches(JavaModelStatus status, int mask) {
		int severityMask = mask & 0x7;
		int categoryMask = mask & ~0x7;
		int bits = status.getBits();
		return ((severityMask == 0) || (bits & severityMask) != 0) && ((categoryMask == 0) || (bits & categoryMask) != 0);
	}
	/**
	 * Creates and returns a new <code>IJavaModelStatus</code> that is a
	 * a multi-status status.
	 *
	 * @see IStatus#isMultiStatus()
	 */
	public static IJavaModelStatus newMultiStatus(IJavaModelStatus[] children) {
		JavaModelStatus jms = new JavaModelStatus();
		jms.children = children;
		return jms;
	}
	/**
	 * Returns a printable representation of this exception for debugging
	 * purposes.
	 */
	@Override
	public String toString() {
		if (this == VERIFIED_OK){
			return "JavaModelStatus[OK]"; //$NON-NLS-1$
		}
		StringBuilder buffer = new StringBuilder();
		buffer.append("Error in Java Model (code "); //$NON-NLS-1$
		buffer.append(this.getCode());
		buffer.append("): "); //$NON-NLS-1$
		buffer.append(getMessage());
		return buffer.toString();
	}
}
