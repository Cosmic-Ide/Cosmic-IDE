/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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
package org.eclipse.jdt.internal.codeassist.select;

import org.eclipse.jdt.internal.compiler.lookup.*;

public class SelectionNodeFound extends RuntimeException {

	public Binding binding;
	public boolean isDeclaration;
	private static final long serialVersionUID = -7335444736618092295L; // backward compatible

public SelectionNodeFound() {
	this(null, false); // we found a problem in the selection node
}
public SelectionNodeFound(Binding binding) {
	this(binding, false);
}
public SelectionNodeFound(Binding binding, boolean isDeclaration) {
	this.binding = binding;
	this.isDeclaration = isDeclaration;
}
}
