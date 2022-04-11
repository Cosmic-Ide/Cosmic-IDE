/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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
package org.eclipse.jdt.core.search;

import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.IJavaElement;

/**
 * A Java search match that represents a module reference.
 * The element is the innermost enclosing member (mostly module declaration) that references this module reference.
 * <p>
 * This class is intended to be instantiated and subclassed by clients.
 * </p>
 * @since 3.14
 */
public class ModuleReferenceMatch extends ReferenceMatch {

	/**
	 * Creates a new module reference match.
	 *
	 * @param enclosingElement the inner-most enclosing member that references this module reference
	 * @param accuracy one of {@link #A_ACCURATE} or {@link #A_INACCURATE}
	 * @param offset the offset the match starts at, or -1 if unknown
	 * @param length the length of the match, or -1 if unknown
	 * @param insideDocComment <code>true</code> if this search match is inside a doc
	 * 				comment, and <code>false</code> otherwise
	 * @param participant the search participant that created the match
	 * @param resource the resource of the element
	 */
	public ModuleReferenceMatch(IJavaElement enclosingElement, int accuracy, int offset, int length,
			boolean insideDocComment, SearchParticipant participant, IResource resource) {
		super(enclosingElement, accuracy, offset, length, insideDocComment, participant, resource);
	}

}
