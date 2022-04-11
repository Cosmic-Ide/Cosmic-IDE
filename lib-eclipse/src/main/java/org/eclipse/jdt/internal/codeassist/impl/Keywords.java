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
 *      Jesper Steen Møller - Contributions for
 *                               bug 529552 - [18.3] Add 'var' in completions
 *******************************************************************************/
package org.eclipse.jdt.internal.codeassist.impl;

public interface Keywords {
	int COUNT = 50;

	char[] ABSTRACT = "abstract".toCharArray(); //$NON-NLS-1$
	char[] ASSERT = "assert".toCharArray(); //$NON-NLS-1$
	char[] BREAK = "break".toCharArray(); //$NON-NLS-1$
	char[] CASE = "case".toCharArray(); //$NON-NLS-1$
	char[] YIELD = "yield".toCharArray(); //$NON-NLS-1$
	char[] CATCH = "catch".toCharArray(); //$NON-NLS-1$
	char[] CLASS = "class".toCharArray(); //$NON-NLS-1$
	char[] CONTINUE = "continue".toCharArray(); //$NON-NLS-1$
	char[] DEFAULT = "default".toCharArray(); //$NON-NLS-1$
	char[] DO = "do".toCharArray(); //$NON-NLS-1$
	char[] ELSE = "else".toCharArray(); //$NON-NLS-1$
	char[] ENUM = "enum".toCharArray(); //$NON-NLS-1$
	char[] EXTENDS = "extends".toCharArray(); //$NON-NLS-1$
	char[] EXPORTS = "exports".toCharArray(); //$NON-NLS-1$
	char[] FINAL = "final".toCharArray(); //$NON-NLS-1$
	char[] FINALLY = "finally".toCharArray(); //$NON-NLS-1$
	char[] FOR = "for".toCharArray(); //$NON-NLS-1$
	char[] IF = "if".toCharArray(); //$NON-NLS-1$
	char[] IMPLEMENTS = "implements".toCharArray(); //$NON-NLS-1$
	char[] IMPORT = "import".toCharArray(); //$NON-NLS-1$
	char[] MODULE = "module".toCharArray(); //$NON-NLS-1$
	char[] INSTANCEOF = "instanceof".toCharArray(); //$NON-NLS-1$
	char[] INTERFACE = "interface".toCharArray(); //$NON-NLS-1$
	char[] NATIVE = "native".toCharArray(); //$NON-NLS-1$
	char[] NEW = "new".toCharArray(); //$NON-NLS-1$
	char[] OPENS= "opens".toCharArray(); //$NON-NLS-1$
	char[] PACKAGE = "package".toCharArray(); //$NON-NLS-1$
	char[] PRIVATE = "private".toCharArray(); //$NON-NLS-1$
	char[] PROTECTED = "protected".toCharArray(); //$NON-NLS-1$
	char[] PROVIDES = "provides".toCharArray(); //$NON-NLS-1$
	char[] PUBLIC = "public".toCharArray(); //$NON-NLS-1$
	char[] REQUIRES = "requires".toCharArray(); //$NON-NLS-1$
	char[] RETURN = "return".toCharArray(); //$NON-NLS-1$
	char[] STATIC = "static".toCharArray(); //$NON-NLS-1$
	char[] STRICTFP = "strictfp".toCharArray(); //$NON-NLS-1$
	char[] SUPER = "super".toCharArray(); //$NON-NLS-1$
	char[] SWITCH = "switch".toCharArray(); //$NON-NLS-1$
	char[] SYNCHRONIZED = "synchronized".toCharArray(); //$NON-NLS-1$
	char[] THIS = "this".toCharArray(); //$NON-NLS-1$
	char[] THROW = "throw".toCharArray(); //$NON-NLS-1$
	char[] THROWS = "throws".toCharArray(); //$NON-NLS-1$
	char[] TO = "to".toCharArray(); //$NON-NLS-1$
	char[] TRANSIENT = "transient".toCharArray(); //$NON-NLS-1$
	char[] TRY = "try".toCharArray(); //$NON-NLS-1$
	char[] USES = "uses".toCharArray(); //$NON-NLS-1$
	char[] VOLATILE = "volatile".toCharArray(); //$NON-NLS-1$
	char[] WHILE = "while".toCharArray(); //$NON-NLS-1$
	char[] WITH = "with".toCharArray(); //$NON-NLS-1$
	char[] TRUE = "true".toCharArray(); //$NON-NLS-1$
	char[] FALSE = "false".toCharArray(); //$NON-NLS-1$
	char[] NULL = "null".toCharArray(); //$NON-NLS-1$
	char[] VAR = "var".toCharArray(); //$NON-NLS-1$ // Admittedly not a full blown keyword, just "reserved"

}
