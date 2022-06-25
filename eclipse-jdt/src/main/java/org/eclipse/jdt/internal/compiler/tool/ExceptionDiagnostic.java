/*******************************************************************************
 * Copyright (c) 2007, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Frits Jalvingh  - fix for bug 533830.
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.tool;

import java.util.Locale;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

final class ExceptionDiagnostic implements Diagnostic<JavaFileObject> {
	private final Exception exception;

	ExceptionDiagnostic(Exception e) {
		this.exception = e;
	}

	@Override
	public String getCode() {
		return "exception"; //$NON-NLS-1$
	}

	@Override
	public long getColumnNumber() {
		return 0;
	}

	@Override
	public long getEndPosition() {
		return 0;
	}

	@Override
	public Kind getKind() {
		return Kind.ERROR;
	}

	@Override
	public long getLineNumber() {
		return 0;
	}

	@Override
	public String getMessage(Locale arg0) {
		return this.exception.toString();
	}

	@Override
	public long getPosition() {
		return 0;
	}

	@Override
	public JavaFileObject getSource() {
		return null;
	}

	@Override
	public long getStartPosition() {
		return 0;
	}
}