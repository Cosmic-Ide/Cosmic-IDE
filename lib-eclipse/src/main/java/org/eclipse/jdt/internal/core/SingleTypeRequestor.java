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
package org.eclipse.jdt.internal.core;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IInitializer;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IModuleDescription;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;

/**
 * The SingleTypeRequestor is an IJavaElementRequestor that
 * only accepts one result element and then cancels.
 */
/* package */ class SingleTypeRequestor implements IJavaElementRequestor {
	/**
	 * The single accepted element
	 */
	protected IType element= null;
/**
 * @see IJavaElementRequestor
 */
@Override
public void acceptField(IField field) {
	// implements interface method
}
/**
 * @see IJavaElementRequestor
 */
@Override
public void acceptInitializer(IInitializer initializer) {
	// implements interface method
}
/**
 * @see IJavaElementRequestor
 */
@Override
public void acceptMemberType(IType type) {
	this.element= type;
}
/**
 * @see IJavaElementRequestor
 */
@Override
public void acceptMethod(IMethod method) {
	// implements interface method
}
/**
 * @see IJavaElementRequestor
 */
@Override
public void acceptModule(IModuleDescription module) {
	// implements interface method
}
/**
 * @see IJavaElementRequestor
 */
@Override
public void acceptPackageFragment(IPackageFragment packageFragment) {
	// implements interface method
}
/**
 * @see IJavaElementRequestor
 */
@Override
public void acceptType(IType type) {
	this.element= type;
}
/**
 * Returns the type accepted by this requestor, or <code>null</code>
 * if no type has been accepted.
 */
public IType getType() {
	return this.element;
}
/**
 * @see IJavaElementRequestor
 */
@Override
public boolean isCanceled() {
	return this.element != null;
}
/**
 * Reset the state of this requestor
 */
public void reset() {
	this.element= null;
}
}
