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

/**
 *Element info for IMember elements.
 */
/* package */ abstract class MemberElementInfo extends SourceRefElementInfo {
	/**
	 * The modifiers associated with this member.
	 *
	 * @see org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants
	 */
	protected int flags;

	/**
	 * @see org.eclipse.jdt.internal.compiler.env.ISourceType#getNameSourceEnd()
	 * @see org.eclipse.jdt.internal.compiler.env.ISourceMethod#getNameSourceEnd()
	 * @see org.eclipse.jdt.internal.compiler.env.ISourceField#getNameSourceEnd()
	 */
	public int getNameSourceEnd() {
		return -1;
	}
	/**
	 * @see org.eclipse.jdt.internal.compiler.env.ISourceType#getNameSourceStart()
	 * @see org.eclipse.jdt.internal.compiler.env.ISourceMethod#getNameSourceStart()
	 * @see org.eclipse.jdt.internal.compiler.env.ISourceField#getNameSourceStart()
	 */
	public int getNameSourceStart() {
		return -1;
	}

	/**
	 * @see org.eclipse.jdt.internal.compiler.env.IGenericType#getModifiers()
	 * @see org.eclipse.jdt.internal.compiler.env.IGenericMethod#getModifiers()
	 * @see org.eclipse.jdt.internal.compiler.env.IGenericField#getModifiers()
	 */
	public int getModifiers() {
		return this.flags;
	}
	protected void setFlags(int flags) {
		this.flags = flags;
	}
}
