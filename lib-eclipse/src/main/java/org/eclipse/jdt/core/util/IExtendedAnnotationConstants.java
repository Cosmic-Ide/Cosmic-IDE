/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
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
 *        Andy Clement (GoPivotal, Inc) aclement@gopivotal.com - Contributions for
 *                          Bug 383624 - [1.8][compiler] Revive code generation support for type annotations (from Olivier's work)
 *******************************************************************************/
package org.eclipse.jdt.core.util;

import org.eclipse.jdt.internal.compiler.codegen.AnnotationTargetTypeConstants;

/**
 * Description of an extended annotation target types constants as described in the JVM specifications
 * (added in JavaSE-1.8).
 *
 * @since 3.10
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface IExtendedAnnotationConstants {

	int CLASS_TYPE_PARAMETER = AnnotationTargetTypeConstants.CLASS_TYPE_PARAMETER;
	int METHOD_TYPE_PARAMETER = AnnotationTargetTypeConstants.METHOD_TYPE_PARAMETER;

	int CLASS_EXTENDS = AnnotationTargetTypeConstants.CLASS_EXTENDS;
	int CLASS_TYPE_PARAMETER_BOUND = AnnotationTargetTypeConstants.CLASS_TYPE_PARAMETER_BOUND;
	int METHOD_TYPE_PARAMETER_BOUND = AnnotationTargetTypeConstants.METHOD_TYPE_PARAMETER_BOUND;
	int FIELD = AnnotationTargetTypeConstants.FIELD;
	int METHOD_RETURN = AnnotationTargetTypeConstants.METHOD_RETURN;
	int METHOD_RECEIVER = AnnotationTargetTypeConstants.METHOD_RECEIVER;
	int METHOD_FORMAL_PARAMETER = AnnotationTargetTypeConstants.METHOD_FORMAL_PARAMETER;
	int THROWS = AnnotationTargetTypeConstants.THROWS;

	int LOCAL_VARIABLE = AnnotationTargetTypeConstants.LOCAL_VARIABLE;
	int RESOURCE_VARIABLE = AnnotationTargetTypeConstants.RESOURCE_VARIABLE;
	int EXCEPTION_PARAMETER = AnnotationTargetTypeConstants.EXCEPTION_PARAMETER;
	int INSTANCEOF = AnnotationTargetTypeConstants.INSTANCEOF;
	int NEW = AnnotationTargetTypeConstants.NEW;
	int CONSTRUCTOR_REFERENCE = AnnotationTargetTypeConstants.CONSTRUCTOR_REFERENCE;
	int METHOD_REFERENCE = AnnotationTargetTypeConstants.METHOD_REFERENCE;
	int CAST = AnnotationTargetTypeConstants.CAST;
	int CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT = AnnotationTargetTypeConstants.CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT;
	int METHOD_INVOCATION_TYPE_ARGUMENT = AnnotationTargetTypeConstants.METHOD_INVOCATION_TYPE_ARGUMENT;
	int CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT = AnnotationTargetTypeConstants.CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT;
	int METHOD_REFERENCE_TYPE_ARGUMENT = AnnotationTargetTypeConstants.METHOD_REFERENCE_TYPE_ARGUMENT;

	// Type path entry kinds
	int TYPE_PATH_DEEPER_IN_ARRAY = 0;
	int TYPE_PATH_DEEPER_IN_INNER_TYPE = 1;
	int TYPE_PATH_ANNOTATION_ON_WILDCARD_BOUND = 2;
	int TYPE_PATH_TYPE_ARGUMENT_INDEX = 3;
}
