/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
package org.eclipse.jdt.core.util;

/**
 * Description of constant pool constants as described in the JVM specifications.
 *
 * @since 2.0
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IConstantPoolConstant {

	int CONSTANT_Class = 7;
	int CONSTANT_Fieldref = 9;
	int CONSTANT_Methodref = 10;
	int CONSTANT_InterfaceMethodref = 11;
	int CONSTANT_String = 8;
	int CONSTANT_Integer = 3;
	int CONSTANT_Float = 4;
	int CONSTANT_Long = 5;
	int CONSTANT_Double = 6;
	int CONSTANT_NameAndType = 12;
	int CONSTANT_Utf8 = 1;
	/**
	 * @since 3.8
	 */
	int CONSTANT_MethodHandle = 15;
	/**
	 * @since 3.8
	 */
	int CONSTANT_MethodType = 16;
	/**
	 * @since 3.8
	 */
	int CONSTANT_InvokeDynamic = 18;
	/**
	 * proposed new name in JVMLS11
	 * @since 3.16
	 */
	int CONSTANT_DynamicCallSite = CONSTANT_InvokeDynamic;

	/**
	 * @since 3.14
	 */
	int CONSTANT_Module = 19;
	/**
	 * @since 3.14
	 */
	int CONSTANT_Package = 20;
	/**
	 * For the proposed section of JVMLS11 4.4.13 The CONSTANT_Dynamic_info Structure
	 * @since 3.16
	 */
	int CONSTANT_Dynamic = 17;

	int CONSTANT_Methodref_SIZE = 5;
	int CONSTANT_Class_SIZE = 3;
	int CONSTANT_Double_SIZE = 9;
	int CONSTANT_Fieldref_SIZE = 5;
	int CONSTANT_Float_SIZE = 5;
	int CONSTANT_Integer_SIZE = 5;
	int CONSTANT_InterfaceMethodref_SIZE = 5;
	int CONSTANT_Long_SIZE = 9;
	int CONSTANT_String_SIZE = 3;
	int CONSTANT_Utf8_SIZE = 3;
	int CONSTANT_NameAndType_SIZE = 5;
	/**
	 * @since 3.8
	 */
	int CONSTANT_MethodHandle_SIZE = 4;
	/**
	 * @since 3.8
	 */
	int CONSTANT_MethodType_SIZE = 3;
	/**
	 * @since 3.8
	 */
	int CONSTANT_InvokeDynamic_SIZE = 5;

	/**
	 * @since 3.16
	 */
	int CONSTANT_Dynamic_SIZE = 5;
	/**
	 * @since 3.14
	 */
	int CONSTANT_Module_SIZE = 3;

	/**
	 * @since 3.14
	 */
	int CONSTANT_Package_SIZE = 3;
	/**
	 * The constant is described at 5.4.3.5 in the Java 7 VM specification (part 3).
	 * @since 3.8
	 */
	int METHOD_TYPE_REF_GetField = 1;
	/**
	 * The constant is described at 5.4.3.5 in the Java 7 VM specification (part 3).
	 * @since 3.8
	 */
	int METHOD_TYPE_REF_GetStatic = 2;
	/**
	 * The constant is described at 5.4.3.5 in the Java 7 VM specification (part 3).
	 * @since 3.8
	 */
	int METHOD_TYPE_REF_PutField = 3;
	/**
	 * The constant is described at 5.4.3.5 in the Java 7 VM specification (part 3).
	 * @since 3.8
	 */
	int METHOD_TYPE_REF_PutStatic = 4;
	/**
	 * The constant is described at 5.4.3.5 in the Java 7 VM specification (part 3).
	 * @since 3.8
	 */
	int METHOD_TYPE_REF_InvokeVirtual = 5;
	/**
	 * The constant is described at 5.4.3.5 in the Java 7 VM specification (part 3).
	 * @since 3.8
	 */
	int METHOD_TYPE_REF_InvokeStatic = 6;
	/**
	 * The constant is described at 5.4.3.5 in the Java 7 VM specification (part 3).
	 * @since 3.8
	 */
	int METHOD_TYPE_REF_InvokeSpecial = 7;
	/**
	 * The constant is described at 5.4.3.5 in the Java 7 VM specification (part 3).
	 * @since 3.8
	 */
	int METHOD_TYPE_REF_NewInvokeSpecial = 8;
	/**
	 * The constant is described at 5.4.3.5 in the Java 7 VM specification (part 3).
	 * @since 3.8
	 */
	int METHOD_TYPE_REF_InvokeInterface = 9;
}
