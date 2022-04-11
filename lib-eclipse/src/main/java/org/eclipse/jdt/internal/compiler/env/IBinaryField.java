/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
 *     Andy Clement (GoPivotal, Inc) aclement@gopivotal.com - Contributions for
 *         Bug 407191 - [1.8] Binary access support for type annotations
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.env;

import org.eclipse.jdt.internal.compiler.impl.Constant;

public interface IBinaryField extends IGenericField {
/**
 * Answer the runtime visible and invisible annotations for this field or null if none.
 */
IBinaryAnnotation[] getAnnotations();

/**
 * Answer the runtime visible and invisible type annotations for this field or null if none.
 */
IBinaryTypeAnnotation[] getTypeAnnotations();

/**
 *
 * @return org.eclipse.jdt.internal.compiler.Constant
 */
Constant getConstant();

/**
 * Answer the receiver's FieldSignature, which describes the field's type
 * as specified in "4.7.9.1 Signatures" of the Java SE 8 VM spec.
 */
char[] getGenericSignature();

/**
 * Answer the name of the field.
 */
char[] getName();

/**
 * Answer the tagbits set according to the bits for annotations.
 */
long getTagBits();

/**
 * Answer the resolved name of the receiver's type in the
 * class file format as specified in section 4.3.2 of the Java 2 VM spec.
 *
 * For example:
 *   - java.lang.String is Ljava/lang/String;
 *   - an int is I
 *   - a 2 dimensional array of strings is [[Ljava/lang/String;
 *   - an array of floats is [F
 */
char[] getTypeName();
}
