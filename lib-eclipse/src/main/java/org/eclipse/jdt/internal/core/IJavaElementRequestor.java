/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
 * This interface is used by IRequestorNameLookup. As results
 * are found by IRequestorNameLookup, they are reported to this
 * interface. An IJavaElementRequestor is able to cancel
 * at any time (that is, stop receiving results), by responding
 * <code>true</code> to <code>#isCancelled</code>.
 */
public interface IJavaElementRequestor {
public void acceptField(IField field);
public void acceptInitializer(IInitializer initializer);
public void acceptMemberType(IType type);
public void acceptMethod(IMethod method);
public void acceptPackageFragment(IPackageFragment packageFragment);
public void acceptType(IType type);
public void acceptModule(IModuleDescription module);
/**
 * Returns <code>true</code> if this IJavaElementRequestor does
 * not want to receive any more results.
 */
boolean isCanceled();
}
