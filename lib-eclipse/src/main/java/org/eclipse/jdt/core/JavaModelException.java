/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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

import java.io.PrintStream;
import java.io.PrintWriter;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.jdt.internal.core.JavaModelStatus;

/**
 * A checked exception representing a failure in the Java model.
 * Java model exceptions contain a Java-specific status object describing the
 * cause of the exception.
 * <p>
 * Instances of this class are automatically created by the Java model
 * when problems arise, so there is generally no need for clients to create
 * instances.
 * </p>
 *
 * @see IJavaModelStatus
 * @see IJavaModelStatusConstants
 *
 * @noextend This class is not intended to be subclassed by clients.
 */
public class JavaModelException extends CoreException {

	private static final long serialVersionUID = -760398656505871287L; // backward compatible

	CoreException nestedCoreException;
/**
 * Creates a Java model exception that wrappers the given <code>Throwable</code>.
 * The exception contains a Java-specific status object with severity
 * <code>IStatus.ERROR</code> and the given status code.
 *
 * @param e the <code>Throwable</code>
 * @param code one of the Java-specific status codes declared in
 *   <code>IJavaModelStatusConstants</code>
 * @see IJavaModelStatusConstants
 * @see org.eclipse.core.runtime.IStatus#ERROR
 */
public JavaModelException(Throwable e, int code) {
	this(new JavaModelStatus(code, e));
}
/**
 * Creates a Java model exception for the given <code>CoreException</code>.
 * Equivalent to
 * <code>JavaModelException(exception,IJavaModelStatusConstants.CORE_EXCEPTION</code>.
 *
 * @param exception the <code>CoreException</code>
 */
public JavaModelException(CoreException exception) {
	super(exception.getStatus());
	this.nestedCoreException = exception;
}
/**
 * Creates a Java model exception for the given Java-specific status object.
 *
 * @param status the Java-specific status object
 */
public JavaModelException(IJavaModelStatus status) {
	super(status);
}

/**
 * Creates a Java model exception for the given status object.
 *
 * @since 3.14
 */
public JavaModelException(IStatus status) {
	super(status);
}
/**
 * Returns the underlying <code>Throwable</code> that caused the failure.
 *
 * @return the wrapped <code>Throwable</code>, or <code>null</code> if the
 *   direct case of the failure was at the Java model layer
 */
public Throwable getException() {
	if (this.nestedCoreException == null) {
		return getStatus().getException();
	} else {
		return this.nestedCoreException;
	}
}
/**
 * Returns the Java model status object for this exception.
 * Equivalent to <code>(IJavaModelStatus) getStatus()</code>.
 *
 * @return a status object
 */
public IJavaModelStatus getJavaModelStatus() {
	IStatus status = getStatus();
	if (status instanceof IJavaModelStatus) {
		return (IJavaModelStatus)status;
	} else {
		// A regular IStatus is created only in the case of a CoreException.
		// See bug 13492 Should handle JavaModelExceptions that contains CoreException more gracefully
		return new JavaModelStatus(this.nestedCoreException);
	}
}
/**
 * Returns whether this exception indicates that a Java model element does not
 * exist. Such exceptions have a status with a code of
 * <code>IJavaModelStatusConstants.ELEMENT_DOES_NOT_EXIST</code> or
 * <code>IJavaModelStatusConstants.ELEMENT_NOT_ON_CLASSPATH</code>.
 * This is a convenience method.
 *
 * @return <code>true</code> if this exception indicates that a Java model
 *   element does not exist
 * @see IJavaModelStatus#isDoesNotExist()
 * @see IJavaModelStatusConstants#ELEMENT_DOES_NOT_EXIST
 * @see IJavaModelStatusConstants#ELEMENT_NOT_ON_CLASSPATH
 */
public boolean isDoesNotExist() {
	IJavaModelStatus javaModelStatus = getJavaModelStatus();
	return javaModelStatus != null && javaModelStatus.isDoesNotExist();
}

/**
 * Prints this exception's stack trace to the given print stream.
 *
 * @param output the print stream
 * @since 3.0
 */
@Override
public void printStackTrace(PrintStream output) {
	synchronized(output) {
		super.printStackTrace(output);
		Throwable throwable = getException();
		if (throwable != null) {
			output.print("Caused by: "); //$NON-NLS-1$
			throwable.printStackTrace(output);
		}
	}
}

/**
 * Prints this exception's stack trace to the given print writer.
 *
 * @param output the print writer
 * @since 3.0
 */
@Override
public void printStackTrace(PrintWriter output) {
	synchronized(output) {
		super.printStackTrace(output);
		Throwable throwable = getException();
		if (throwable != null) {
			output.print("Caused by: "); //$NON-NLS-1$
			throwable.printStackTrace(output);
		}
	}
}
/*
 * Returns a printable representation of this exception suitable for debugging
 * purposes only.
 */
@Override
public String toString() {
	StringBuilder buffer= new StringBuilder();
	buffer.append("Java Model Exception: "); //$NON-NLS-1$
	if (getException() != null) {
		if (getException() instanceof CoreException) {
			CoreException c= (CoreException)getException();
			buffer.append("Core Exception [code "); //$NON-NLS-1$
			buffer.append(c.getStatus().getCode());
			buffer.append("] "); //$NON-NLS-1$
			buffer.append(c.getStatus().getMessage());
		} else {
			buffer.append(getException().toString());
		}
	} else {
		buffer.append(getStatus().toString());
	}
	return buffer.toString();
}
}
