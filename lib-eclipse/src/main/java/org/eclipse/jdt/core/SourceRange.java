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
package org.eclipse.jdt.core;


/**
 * A source range defines an element's source coordinates relative to
 * its source buffer.
 *
 * @see ISourceRange
 * @since 3.6
 */
public final class SourceRange implements ISourceRange {

	/**
	 * Helper method that answers whether a valid source range is available
	 * in the given ISourceRange. When an element has no associated source
	 * code, Java Model APIs may return either <code>null</code> or a range of
	 * [-1, 0] to indicate an invalid range. This utility method can be used
	 * to detect that case.
	 *
	 * @param range a source range, can be <code>null</code>
	 * @return <code>true</code> iff range is not null and range.getOffset() is not -1
	 */
	public static boolean isAvailable(ISourceRange range) { // see https://bugs.eclipse.org/bugs/show_bug.cgi?id=130161
		return range != null && range.getOffset() != -1;
	}

	private int offset;
	private int length;

	/**
	 * Instantiate a new source range using the given offset and the given length.
	 *
	 * @param offset the given offset
	 * @param length the given length
	 */
	public SourceRange(int offset, int length) {
		this.offset = offset;
		this.length = length;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof ISourceRange))
			return false;
		ISourceRange sourceRange = (ISourceRange) obj;
		return sourceRange.getOffset() == this.offset && sourceRange.getLength() == this.length;
	}
	/**
	 * @see ISourceRange
	 */
	@Override
	public int getLength() {
		return this.length;
	}
	/**
	 * @see ISourceRange
	 */
	@Override
	public int getOffset() {
		return this.offset;
	}

	@Override
	public int hashCode() {
		return this.length ^ this.offset;
	}

	@Override
	public String toString() {
		StringBuilder buffer = new StringBuilder();
		buffer.append("[offset="); //$NON-NLS-1$
		buffer.append(this.offset);
		buffer.append(", length="); //$NON-NLS-1$
		buffer.append(this.length);
		buffer.append("]"); //$NON-NLS-1$
		return buffer.toString();
	}
}
