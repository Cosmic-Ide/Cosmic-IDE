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
 *                          Bug 409250 - [1.8][compiler] Various loose ends in 308 code generation
 *******************************************************************************/
package org.eclipse.jdt.core.util;

/**
 * Description of an extended annotation structure as described in the JVM specifications
 * (added in JavaSE-1.8).
 *
 * This interface may be implemented by clients.
 *
 * @since 3.10
 */
public interface IExtendedAnnotation extends IAnnotation {
	/**
	 * Answer back the target type as described in the JVM specifications.
	 *
	 * @return the target type
	 */
	int getTargetType();

	/**
	 * Answer back the offset.
	 *
	 * For a target_type value equals to:
	 * <table border="1">
	 * <tr>
	 * <th>target_type</th>
	 * <th>offset description</th>
	 * </tr>
	 * <tr>
	 * <td>0x43 (INSTANCE_OF), 0x44 (NEW), 0x45 (CONSTRUCTOR_REFERENCE), 0x46 (METHOD_REFERENCE)</td>
	 * <td>The offset within the bytecodes of the <code>instanceof</code> bytecode for INSTANCE_OF,
	 * the <code>new</code> bytecode for NEW and the implementing instruction for either a
	 * CONSTRUCTOR_REFERENCE or METHOD_REFERENCE.</td>
	 * </tr>
	 * <tr>
	 * <td>0x47 (CAST), 0x48 (CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT), 0x49 (METHOD_INVOCATION_TYPE_ARGUMENT),
	 * 0x4A (CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT), 0x4B (METHOD_REFERENCE_TYPE_ARGUMENT)</td>
	 * <td>The offset within the bytecodes of the <code>new</code> bytecode for constructor call, or the
	 * relevant bytecode for method invocation or method reference. For CAST the offset may
	 * point to the <code>checkcast</code> or another instruction as it is possible the cast
	 * may have been discarded by the compiler if it were a no-op.</td>
	 * </tr>
	 * </table>
	 *
	 *
	 * @return the offset
	 */
	int getOffset();

	/**
	 * Answer back the exception table index when the target_type is EXCEPTION_PARAMETER.
	 *
	 * @return the exception table index
	 */
	int getExceptionTableIndex();

	/**
	 * Answer back the local variable reference info table length of this entry as specified in
	 * the JVM specifications.
	 *
	 * <p>This is defined only for annotations related to a local variable.</p>
	 *
	 * @return the local variable reference info table length of this entry as specified in
	 * the JVM specifications
	 */
	int getLocalVariableRefenceInfoLength();

	/**
	 * Answer back the local variable reference info table of this entry as specified in
	 * the JVM specifications. Answer an empty array if none.
	 *
	 * <p>This is defined only for annotations related to a local variable.</p>
	 *
	 * @return the local variable reference info table of this entry as specified in
	 * the JVM specifications. Answer an empty array if none
	 */
	ILocalVariableReferenceInfo[] getLocalVariableTable();

	/**
	 * Answer back the method parameter index.
	 *
	 * <p>The index is 0-based.</p>
	 *
	 * @return the method parameter index
	 */
	int getParameterIndex();

	/**
	 * Answer back the index of the type parameter of the class or method
	 *
	 * <p>The index is 0-based.</p>
	 *
	 * @return the index of the type parameter of the class or method
	 */
	int getTypeParameterIndex();

	/**
	 * Answer back the index of the bound of the type parameter of the method or class
	 *
	 * <p>The index is 0-based.</p>
	 *
	 * @return the index of the bound of the type parameter of the method or class
	 */
	int getTypeParameterBoundIndex();

	/**
	 * Answer back the index in the given different situations.
	 *
	 * <p>The index is 0-based.</p>
	 *
	 * <table border="1">
	 * <tr>
	 * <th>target_type</th>
	 * <th>offset description</th>
	 * </tr>
	 * <tr>
	 * <td>0x10 (CLASS_EXTENDS)</td>
	 * <td>the index of the type in the clause: <code>-1 (65535)</code> is used if the annotation is on
	 * the superclass type, and the value <code>i</code> is used if the annotation is on the <code>i</code>th
	 * superinterface type (counting from zero).</td>
	 * </tr>
	 * <tr>
	 * <td>0x17 (THROWS)</td>
	 * <td>the index of the exception type in the clause: the value <code>i</code> denotes an annotation of the
	 * <code>i</code>th exception type (counting from zero).</td>
	 * </tr>
	 * <tr>
	 * <td>0x47 (CAST), 0x48 (CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT), 0x49 (METHOD_INVOCATION_TYPE_ARGUMENT),
	 * 0x4A (CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT), 0x4B (METHOD_REFERENCE_TYPE_ARGUMENT)</td>
	 * <td>the type argument index in the expression</td>
	 * </tr>
	 * </table>
	 * @return the index in the given different situations
	 */
	int getAnnotationTypeIndex();

	/**
	 * Answer back the locations of the annotated type as described in the JVM specifications.
	 *
	 * <p>This is used for parameterized and array types.</p>
	 *
	 * @return the locations of the annotated type
	 */
	int[][] getTypePath();

}
