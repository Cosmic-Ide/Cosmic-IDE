/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
 *        Andy Clement - Contributions for
 *                          Bug 383624 - [1.8][compiler] Revive code generation support for type annotations (from Olivier's work)
 *        Jesper Steen Moller - Contributions for
 *							Bug 406973 - [compiler] Parse MethodParameters attribute
 *******************************************************************************/
package org.eclipse.jdt.core.util;

/**
 * Description of attribute names as described in the JVM specifications.
 *
 * @since 2.0
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IAttributeNamesConstants {
	/**
	 * "Synthetic" attribute.
	 * <p>Note that prior to JDK 1.5, synthetic elements were always marked
	 * using an attribute; with 1.5, synthetic elements can also be marked
	 * using the {@link IModifierConstants#ACC_SYNTHETIC} flag.
	 * </p>
	 * @since 2.0
	 */
	char[] SYNTHETIC = "Synthetic".toCharArray(); //$NON-NLS-1$

	/**
	 * "ConstantValue" attribute.
	 * @since 2.0
	 */
	char[] CONSTANT_VALUE = "ConstantValue".toCharArray(); //$NON-NLS-1$

	/**
	 * "LineNumberTable" attribute.
	 * @since 2.0
	 */
	char[] LINE_NUMBER = "LineNumberTable".toCharArray(); //$NON-NLS-1$

	/**
	 * "LocalVariableTable" attribute.
	 * @since 2.0
	 */
	char[] LOCAL_VARIABLE = "LocalVariableTable".toCharArray(); //$NON-NLS-1$

	/**
	 * "InnerClasses" attribute.
	 * @since 2.0
	 */
	char[] INNER_CLASSES = "InnerClasses".toCharArray(); //$NON-NLS-1$

	/**
	 * "Code" attribute.
	 * @since 2.0
	 */
	char[] CODE = "Code".toCharArray(); //$NON-NLS-1$

	/**
	 * "Exceptions" attribute.
	 * @since 2.0
	 */
	char[] EXCEPTIONS = "Exceptions".toCharArray(); //$NON-NLS-1$

	/**
	 * "SourceFile" attribute.
	 * @since 2.0
	 */
	char[] SOURCE = "SourceFile".toCharArray(); //$NON-NLS-1$

	/**
	 * "Deprecated" attribute.
	 * @since 2.0
	 */
	char[] DEPRECATED = "Deprecated".toCharArray(); //$NON-NLS-1$

	/**
	 * "Signature" attribute (added in J2SE 1.5).
	 * Class file readers which support J2SE 1.5 return
	 * attributes with this name represented by objects
	 * implementing {@link ISignatureAttribute}.
	 * @since 3.0
	 */
	char[] SIGNATURE = "Signature".toCharArray(); //$NON-NLS-1$

	/**
	 * "EnclosingMethod" attribute (added in J2SE 1.5).
	 * Class file readers which support J2SE 1.5 return
	 * attributes with this name represented by objects
	 * implementing {@link IEnclosingMethodAttribute}.
	 * @since 3.0
	 */
	char[] ENCLOSING_METHOD = "EnclosingMethod".toCharArray(); //$NON-NLS-1$

	/**
	 * "LocalVariableTypeTable" attribute (added in J2SE 1.5).
	 * @since 3.0
	 */
	char[] LOCAL_VARIABLE_TYPE_TABLE = "LocalVariableTypeTable".toCharArray(); //$NON-NLS-1$

	/**
	 * "RuntimeVisibleAnnotations" attribute (added in J2SE 1.5).
	 * @since 3.0
	 */
	char[] RUNTIME_VISIBLE_ANNOTATIONS = "RuntimeVisibleAnnotations".toCharArray(); //$NON-NLS-1$

	/**
	 * "RuntimeInvisibleAnnotations" attribute (added in J2SE 1.5).
	 * @since 3.0
	 */
	char[] RUNTIME_INVISIBLE_ANNOTATIONS = "RuntimeInvisibleAnnotations".toCharArray(); //$NON-NLS-1$

	/**
	 * "RuntimeVisibleParameterAnnotations" attribute (added in J2SE 1.5).
	 * @since 3.0
	 */
	char[] RUNTIME_VISIBLE_PARAMETER_ANNOTATIONS = "RuntimeVisibleParameterAnnotations".toCharArray(); //$NON-NLS-1$

	/**
	 * "RuntimeInvisibleParameterAnnotations" attribute (added in J2SE 1.5).
	 * @since 3.0
	 */
	char[] RUNTIME_INVISIBLE_PARAMETER_ANNOTATIONS = "RuntimeInvisibleParameterAnnotations".toCharArray(); //$NON-NLS-1$

	/**
	 * "AnnotationDefault" attribute (added in J2SE 1.5).
	 * @since 3.0
	 */
	char[] ANNOTATION_DEFAULT = "AnnotationDefault".toCharArray(); //$NON-NLS-1$

	/**
	 * "StackMapTable" attribute (added in J2SE 1.6).
	 * @since 3.2
	 */
	char[] STACK_MAP_TABLE = "StackMapTable".toCharArray(); //$NON-NLS-1$

	/**
	 * "StackMap" attribute (added in cldc1.0).
	 * @since 3.2
	 */
	char[] STACK_MAP = "StackMap".toCharArray(); //$NON-NLS-1$

 	/**
	 * "RuntimeVisibleTypeAnnotations" attribute (added in jsr 308).
	 * @since 3.10
	 */
	char[] RUNTIME_VISIBLE_TYPE_ANNOTATIONS = "RuntimeVisibleTypeAnnotations".toCharArray(); //$NON-NLS-1$

	/**
	 * "RuntimeInvisibleTypeAnnotations" attribute (added in jsr 308).
	 * @since 3.10
	 */
	char[] RUNTIME_INVISIBLE_TYPE_ANNOTATIONS = "RuntimeInvisibleTypeAnnotations".toCharArray(); //$NON-NLS-1$


	/**
	 * "BootstrapMethods" attribute (added in cldc1.0).
	 * @since 3.8
	 */
	char[] BOOTSTRAP_METHODS = "BootstrapMethods".toCharArray(); //$NON-NLS-1$

	/**
	 * "MethodParameters" attribute (added in jep118).
	 * @since 3.10
	 */
	char[] METHOD_PARAMETERS = "MethodParameters".toCharArray(); //$NON-NLS-1$

	/**
	 * "Module" attribute (added in Java SE 9).
	 * @since 3.14
	 */
	char[] MODULE = "Module".toCharArray(); //$NON-NLS-1$

	/**
	 * "ModulePackages" attribute (added in Java SE 9).
	 * @since 3.14
	 */
	char[] MODULE_PACKAGES = "ModulePackages".toCharArray(); //$NON-NLS-1$

	/**
	 * "ModuleMainClass" attribute (added in Java SE 9).
	 * @since 3.14
	 */
	char[] MODULE_MAIN_CLASS = "ModuleMainClass".toCharArray(); //$NON-NLS-1$

	/**
	 * "NestHost" attribute (JVMS 11).
	 * @since 3.16
	 */
	char[] NEST_HOST = "NestHost".toCharArray(); //$NON-NLS-1$

	/**
	 * "NestMembers" attribute (JVMS 11).
	 * @since 3.16
	 */
	char[] NEST_MEMBERS = "NestMembers".toCharArray(); //$NON-NLS-1$
	/**
	 * "Record" attribute (JVMS 14 - preview).
	 * @since 3.22
	 */
	char[] RECORD = "Record".toCharArray(); //$NON-NLS-1$
	/**
	 * "PermittedSubclasses" attribute (JVMS 15).
	 * @since 3.24
	 */
	char[] PERMITTED_SUBCLASSES = "PermittedSubclasses".toCharArray(); //$NON-NLS-1$


}
