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
package org.eclipse.jdt.core.util;

/**
 * Description of a method info as described in the JVM
 * specifications.
 *
 * This interface may be implemented by clients.
 *
 * @since 2.0
 */
public interface IMethodInfo {

	/**
	 * Answer back the method descriptor of this method info as specified
	 * in the JVM specifications.
	 *
	 * @return the method descriptor of this method info as specified
	 * in the JVM specifications
	 */
	char[] getDescriptor();

	/**
	 * Answer back the descriptor index of this method info.
	 *
	 * @return the descriptor index of this method info
	 */
	int getDescriptorIndex();

	/**
	 * Answer back the access flags of this method info as specified
	 * in the JVM specifications.
	 *
	 * @return the access flags of this method info as specified
	 * in the JVM specifications
	 */
	int getAccessFlags();

	/**
	 * Answer back the name of this method info as specified
	 * in the JVM specifications.
	 *
	 * @return the name of this method info as specified
	 * in the JVM specifications
	 */
	char[] getName();

	/**
	 * Answer back the name index of this method info.
	 *
	 * @return the name index of this method info
	 */
	int getNameIndex();

	/**
	 * Answer true if this method info represents a &lt;clinit&gt; method,
	 * false otherwise.
	 *
	 * @return true if this method info represents a &lt;clinit&gt; method,
	 * false otherwise
	 */
	boolean isClinit();

	/**
	 * Answer true if this method info represents a constructor,
	 * false otherwise.
	 *
	 * @return true if this method info represents a constructor,
	 * false otherwise
	 */
	boolean isConstructor();

	/**
	 * Return true if the method info is synthetic according to the JVM specification, false otherwise.
	 * <p>Note that prior to JDK 1.5, synthetic fields were always marked using
	 * an attribute; with 1.5, synthetic fields can also be marked using
	 * the {@link IModifierConstants#ACC_SYNTHETIC} flag.
	 * </p>
	 *
	 * @return true if the method info is synthetic according to the JVM specification, false otherwise
	 */
	boolean isSynthetic();

	/**
	 * Answer true if this method info has a deprecated attribute,
	 * false otherwise.
	 *
	 * @return true if this method info has a deprecated attribute,
	 * false otherwise
	 */
	boolean isDeprecated();

	/**
	 * Answer the code attribute of this method info, null if none or if the decoding
	 * flag doesn't include METHOD_BODIES.
	 *
	 * @return the code attribute of this method info, null if none or if the decoding
	 * flag doesn't include METHOD_BODIES
	 */
	ICodeAttribute getCodeAttribute();

	/**
	 * Answer the exception attribute of this method info, null is none.
	 *
	 * 	@return the exception attribute of this method info, null is none
	 */
	IExceptionAttribute getExceptionAttribute();

	/**
	 * Answer back the attribute number of the method info. It includes the CodeAttribute
	 * if any even if the decoding flags doesn't include METHOD_BODIES.
	 *
	 * @return the attribute number of the method info. It includes the CodeAttribute
	 * if any even if the decoding flags doesn't include METHOD_BODIES
	 */
	int getAttributeCount();

	/**
	 * Answer back the collection of all attributes of the method info. It
	 * includes SyntheticAttribute, CodeAttributes, etc. It doesn't include the
	 * CodeAttribute if the decoding flags doesn't include METHOD_BODIES.
	 * Returns an empty collection if none.
	 *
	 * @return the collection of all attributes of the method info. It
	 * includes SyntheticAttribute, CodeAttributes, etc. It doesn't include the
	 * CodeAttribute if the decoding flags doesn't include METHOD_BODIES.
	 * Returns an empty collection if none
	 */
	IClassFileAttribute[] getAttributes();
}
