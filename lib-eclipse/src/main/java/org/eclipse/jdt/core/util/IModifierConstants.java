/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
 *     Jesper Steen Moeller - Contribution for bug 406973 - [compiler] Parse MethodParameters attribute
 *******************************************************************************/
package org.eclipse.jdt.core.util;

/**
 * Definition of the modifier constants as specified in the JVM specifications.
 *
 * @since 2.0
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IModifierConstants {

	int ACC_PUBLIC       = 0x0001;
	int ACC_PRIVATE      = 0x0002;
	int ACC_PROTECTED    = 0x0004;
	int ACC_STATIC       = 0x0008;
	int ACC_FINAL        = 0x0010;
	int ACC_SUPER        = 0x0020;
	int ACC_SYNCHRONIZED = 0x0020;
	int ACC_VOLATILE     = 0x0040;

	/**
	 * Indicates a bridge method (added in J2SE 1.5).
	 * @since 3.0
	 */
	int ACC_BRIDGE       = 0x0040;
	int ACC_TRANSIENT    = 0x0080;

	/**
	 * Indicates a variable arity method (added in J2SE 1.5).
	 * @since 3.0
	 */
	int ACC_VARARGS      = 0x0080;
	int ACC_NATIVE       = 0x0100;
	int ACC_INTERFACE    = 0x0200;
	int ACC_ABSTRACT     = 0x0400;
	int ACC_STRICT       = 0x0800;
	/**
	 * Indicates a synthetic member or method parameter.
	 * @since 3.0
	 */
	int ACC_SYNTHETIC    = 0x1000;

	/**
	 * Indicates an annotation (added in J2SE 1.5).
	 * @since 3.0
	 */
	int ACC_ANNOTATION   = 0x2000;

	/**
	 * Indicates an enum (added in J2SE 1.5).
	 * @since 3.0
	 */
	int ACC_ENUM         = 0x4000;

	/**
	 * Indicates a module (added in Java SE 9).
	 * @since 3.14
	 */
	int ACC_MODULE       = 0x8000;

	/**
	 * Indicates a mandated parameter, such as this$1 (added in Java SE 8).
	 * @since 3.10
	 */
	int ACC_MANDATED     = 0x8000;
	/**
	 * Indicates an open module in module-info file (added in Java SE 9).
	 * @since 3.14
	 */
	int ACC_OPEN			= 0x0020;

	/**
	 * Indicates a transitive requires in module-info file (added in Java SE 9).
	 * @since 3.14
	 */
	int ACC_TRANSITIVE     = 0x0020;

	/**
	 * Indicates a static requires in module-info file (added in Java SE 9).
	 * @since 3.14
	 */
	int ACC_STATIC_PHASE	= 0x0040;
}
