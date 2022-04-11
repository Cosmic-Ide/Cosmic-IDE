/*******************************************************************************
 * Copyright (c) 2013 GoPivotal, Inc.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *        Andy Clement (GoPivotal, Inc) aclement@gopivotal.com - Contributions for
 *          Bug 407191 - [1.8] Binary access support for type annotations
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.env;

import org.eclipse.jdt.internal.compiler.codegen.AnnotationTargetTypeConstants;

/**
 * This represents class file information about an annotation instance.
 */
public interface IBinaryTypeAnnotation {

	static int[] NO_TYPE_PATH = new int[0];

/**
 * @return the annotation
 */
IBinaryAnnotation getAnnotation();

/**
 * @return the target type, according to the constants in {@link AnnotationTargetTypeConstants}
 */
int getTargetType();

/**
 *
 * @return the raw type path data, each pair of ints is a type path entry
 */
int[] getTypePath();

/**
 * Applicable for target type 0x10(CLASS_EXTENDS)
 *
 * @return the supertype index. -1 (65535) for superclass, 0..n for superinterfaces
 */
int getSupertypeIndex();

/**
 * Applicable for target type 0x00(CLASS_TYPE_PARAMETER), 0x01(METHOD_TYPE_PARAMETER)
 *
 * @return the type parameter index (0 based)
 */
int getTypeParameterIndex();

/**
 * Applicable for target type 0x11(CLASS_TYPE_PARAMETER_BOUND), 0x12(METHOD_TYPE_PARAMETER_BOUND)
 *
 * @return the bound index
 */
int getBoundIndex();

/**
 * Applicable for target type 0x16(METHOD_FORMAL_PARAMETER)
 *
 * @return the method formal parameter index
 */
int getMethodFormalParameterIndex();

/**
 * Applicable for target type 0x17(THROWS)
 *
 * @return the throws type index
 */
int getThrowsTypeIndex();

}
