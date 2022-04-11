/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.core.util;

/**
 * Description of a component info as described in the JVM
 * specifications.
 *
 * This interface may be implemented by clients.
 *
 * @since 3.22
 */
public interface IComponentInfo {


	/**
	 * Answer back the name of this component info. The name is returned as
	 * specified in the JVM specifications.
	 *
	 * @return the name of this component info. The name is returned as
	 * specified in the JVM specifications
	 */
	char[] getName();

	/**
	 * Answer back the name index of this component info.
	 *
	 * @return the name index of this component info
	 */
	int getNameIndex();

	/**
	 * Answer back the descriptor of this component info. The descriptor is returned as
	 * specified in the JVM specifications.
	 *
	 * @return the descriptor of this component info. The descriptor is returned as
	 * specified in the JVM specifications
	 */
	char[] getDescriptor();

	/**
	 * Answer back the descriptor index of this component info.
	 *
	 * @return the descriptor index of this component info
	 */
	int getDescriptorIndex();

	/**
	 * Answer back the attribute number of the component info.
	 *
	 * @return the attribute number of the component info
	 */
	int getAttributeCount();


	/**
	 * Answer back the collection of all attributes of the component info. It
	 * includes SyntheticAttribute, ConstantValueAttributes, etc.
	 * Returns an empty collection if none.
	 *
	 * @return the collection of all attributes of the component info.
	 * Returns an empty collection if none
	 */
	IClassFileAttribute[] getAttributes();

	/**
	 * Answer back the total size
	 * @return the size of the component info
	 */
	public int sizeInBytes();
}