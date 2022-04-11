/*******************************************************************************
 * Copyright (c) 2003, 2013 IBM Corporation and others.
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

import org.eclipse.jdt.internal.compiler.ast.LocalDeclaration;

public class CatchParameterBinding extends LocalVariableBinding {

	TypeBinding [] preciseTypes = Binding.NO_EXCEPTIONS;  // the catch block can be entered with the parameters set to these types.

	public CatchParameterBinding(LocalDeclaration declaration, TypeBinding type, int modifiers, boolean isArgument) {
		super(declaration, type, modifiers, isArgument);
	}

	public TypeBinding [] getPreciseTypes() {
		return this.preciseTypes;
	}

	public void setPreciseType(TypeBinding raisedException) {
		int length = this.preciseTypes.length;
		for (int i = 0; i < length; ++i) {
			if (TypeBinding.equalsEquals(this.preciseTypes[i], raisedException))
				return;
		}
		System.arraycopy(this.preciseTypes, 0, this.preciseTypes = new TypeBinding [length + 1], 0, length);
		this.preciseTypes[length] = raisedException;
		return;
	}
	@Override
	public boolean isCatchParameter() {
		return true;
	}
}