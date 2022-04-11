/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
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

package org.eclipse.jdt.internal.core;

public class BinaryLambdaMethod extends LambdaMethod {

	BinaryLambdaMethod(JavaElement parent, String name, String key, int sourceStart, String [] parameterTypes, String [] parameterNames, String returnType, SourceMethodElementInfo elementInfo) {
		super(parent, name, key, sourceStart, parameterTypes, parameterNames, returnType, elementInfo);
	}

	@Override
	public JavaElement getPrimaryElement(boolean checkOwner) {
		return this;
	}

	@Override
	public boolean isBinary() {
		return true;
	}
}
