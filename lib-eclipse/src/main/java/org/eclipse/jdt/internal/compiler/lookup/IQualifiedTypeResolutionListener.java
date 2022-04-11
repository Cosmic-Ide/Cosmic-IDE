/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
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

import org.eclipse.jdt.internal.compiler.ast.QualifiedTypeReference;

/**
 * A listener, which gets notified when a type binding has been discovered.
 * <p>
 * This interface may be implemented by clients.
 * </p>
 */
public interface IQualifiedTypeResolutionListener {

	/**
	 * Notifies that the given resolution has been found for the given type reference. Some of the bindings are
	 * intermediate types i.e. qualifying types.
	 *
	 * @param typeReference
	 *            the type reference
	 * @param resolution
	 *            the resolution found
	 */
	public void recordResolution(QualifiedTypeReference typeReference, TypeBinding resolution);
}
