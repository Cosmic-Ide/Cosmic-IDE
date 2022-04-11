/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
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
package org.eclipse.jdt.internal.core.search;

import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.search.MethodNameMatch;

/**
 * Java Search concrete class for a method name match.
 *
 */
public class JavaSearchMethodNameMatch extends MethodNameMatch {

	IMethod method = null;
	int modifiers = -1;

	private int accessibility = IAccessRule.K_ACCESSIBLE; // TODO: this pertains to class/type - need to revisit whether this is required in method name match
	public JavaSearchMethodNameMatch(IMethod method, int modifiers) {
		this.method = method;
		this.modifiers = modifiers;
	}

	@Override
	public int getAccessibility() {
		return this.accessibility;
	}

	@Override
	public int getModifiers() {
		return this.modifiers;
	}

	@Override
	public IMethod getMethod() {
		return this.method;
	}

	public void setAccessibility(int accessibility) {
		this.accessibility = accessibility;
	}

	@Override
	public String toString() {
		return this.method == null ? super.toString() : this.method.toString();
	}

}
