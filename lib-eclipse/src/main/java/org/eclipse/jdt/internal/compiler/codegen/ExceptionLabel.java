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
 *        Andy Clement (GoPivotal, Inc) aclement@gopivotal.com - Contributions for
 *                          Bug 383624 - [1.8][compiler] Revive code generation support for type annotations (from Olivier's work)
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.codegen;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;

public class ExceptionLabel extends Label {

	public int ranges[] = {POS_NOT_SET,POS_NOT_SET};
	private int count = 0; // incremented each time placeStart or placeEnd is called
	public TypeBinding exceptionType;
	public TypeReference exceptionTypeReference;
	public Annotation [] se7Annotations;

public ExceptionLabel(CodeStream codeStream, TypeBinding exceptionType, TypeReference exceptionTypeReference, Annotation [] se7Annotations) {
	super(codeStream);
	this.exceptionType = exceptionType;
	this.exceptionTypeReference = exceptionTypeReference;
	this.se7Annotations = se7Annotations;
}

public ExceptionLabel(CodeStream codeStream, TypeBinding exceptionType) {
	super(codeStream);
	this.exceptionType = exceptionType;
}
public int getCount() {
	return this.count;
}
@Override
public void place() {
	// register the handler inside the codeStream then normal place
	this.codeStream.registerExceptionHandler(this);
	this.position = this.codeStream.getPosition();
}

public void placeEnd() {
	int endPosition = this.codeStream.position;
	if (this.ranges[this.count-1] == endPosition) { // start == end ?
		// discard empty exception handler
		this.count--;
	} else {
		this.ranges[this.count++] = endPosition;
	}
}

public void placeStart() {
	int startPosition = this.codeStream.position;
	if (this.count > 0 && this.ranges[this.count-1] == startPosition) { // start == previous end ?
		// reopen current handler
		this.count--;
		return;
	}
	// only need to grow on even additions (i.e. placeStart only)
	int length;
	if (this.count == (length = this.ranges.length)) {
		System.arraycopy(this.ranges, 0, this.ranges = new int[length*2], 0, length);
	}
	this.ranges[this.count++] = startPosition;
}
@Override
public String toString() {
	String basic = getClass().getName();
	basic = basic.substring(basic.lastIndexOf('.')+1);
	StringBuilder buffer = new StringBuilder(basic);
	buffer.append('@').append(Integer.toHexString(hashCode()));
	buffer.append("(type=").append(this.exceptionType == null ? CharOperation.NO_CHAR : this.exceptionType.readableName()); //$NON-NLS-1$
	buffer.append(", position=").append(this.position); //$NON-NLS-1$
	buffer.append(", ranges = "); //$NON-NLS-1$
	if (this.count == 0) {
		buffer.append("[]"); //$NON-NLS-1$
	} else {
		for (int i = 0; i < this.count; i++) {
			if ((i & 1) == 0) {
				buffer.append("[").append(this.ranges[i]); //$NON-NLS-1$
			} else {
				buffer.append(",").append(this.ranges[i]).append("]"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		if ((this.count & 1) == 1) {
			buffer.append(",?]"); //$NON-NLS-1$
		}
	}
	buffer.append(')');
	return buffer.toString();
}
}
