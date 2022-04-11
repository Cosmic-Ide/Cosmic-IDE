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
package org.eclipse.jdt.internal.compiler.lookup;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ast.ImportReference;

public class ImportConflictBinding extends ImportBinding {
public ReferenceBinding conflictingTypeBinding; // must ensure the import is resolved

public ImportConflictBinding(char[][] compoundName, Binding methodBinding, ReferenceBinding conflictingTypeBinding, ImportReference reference) {
	super(compoundName, false, methodBinding, reference);
	this.conflictingTypeBinding = conflictingTypeBinding;
}
@Override
public char[] readableName() {
	return CharOperation.concatWith(this.compoundName, '.');
}
@Override
public String toString() {
	return "method import : " + new String(readableName()); //$NON-NLS-1$
}
}
