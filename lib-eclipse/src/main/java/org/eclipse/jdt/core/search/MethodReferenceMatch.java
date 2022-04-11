/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
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
 * A Java search match that represents a method reference.
 * The element is the inner-most enclosing member that references this method.
 * <p>
 * This class is intended to be instantiated and subclassed by clients.
 * </p>
 *
 * @since 3.0
 */
public class MethodReferenceMatch extends ReferenceMatch {
	private boolean constructor;
	private boolean synthetic;
	private boolean superInvocation;

/**
 * Creates a new method reference match.
 *
 * @param enclosingElement the inner-most enclosing member that references this method
 * @param accuracy one of {@link #A_ACCURATE} or {@link #A_INACCURATE}
 * @param offset the offset the match starts at, or -1 if unknown
 * @param length the length of the match, or -1 if unknown
 * @param insideDocComment <code>true</code> if this search match is inside a doc
 * 		comment, and <code>false</code> otherwise
 * @param participant the search participant that created the match
 * @param resource the resource of the element
 */
public MethodReferenceMatch(IJavaElement enclosingElement, int accuracy, int offset, int length, boolean insideDocComment, SearchParticipant participant, IResource resource) {
	super(enclosingElement, accuracy, offset, length, insideDocComment, participant, resource);
}

/**
 * Creates a new method reference match.
 *
 * @param enclosingElement the inner-most enclosing member that references this method
 * @param accuracy one of {@link #A_ACCURATE} or {@link #A_INACCURATE}
 * @param offset the offset the match starts at, or -1 if unknown
 * @param length the length of the match, or -1 if unknown
 * @param constructor <code>true</code> if this search match a constructor
 * 		<code>false</code> otherwise
 * @param synthetic <code>true</code> if this search match a synthetic element
 * 		<code>false</code> otherwise
 * @param insideDocComment <code>true</code> if this search match is inside a doc
 * comment, and <code>false</code> otherwise
 * @param participant the search participant that created the match
 * @param resource the resource of the element
 * @since 3.1
 */
public MethodReferenceMatch(IJavaElement enclosingElement, int accuracy, int offset, int length, boolean constructor, boolean synthetic, boolean insideDocComment, SearchParticipant participant, IResource resource) {
	this(enclosingElement, accuracy, offset, length, insideDocComment, participant, resource);
	this.constructor = constructor;
	this.synthetic = synthetic;
}

/**
 * Creates a new method reference match.
 *
 * @param enclosingElement the inner-most enclosing member that references this method
 * @param accuracy one of {@link #A_ACCURATE} or {@link #A_INACCURATE}
 * @param offset the offset the match starts at, or -1 if unknown
 * @param length the length of the match, or -1 if unknown
 * @param constructor <code>true</code> if this search matches a constructor
 * 		<code>false</code> otherwise
 * @param synthetic <code>true</code> if this search matches a synthetic element
 * 		<code>false</code> otherwise
 * @param superInvocation <code>true</code> if this search matches a super-type invocation
 * 		element <code>false</code> otherwise
 * @param insideDocComment <code>true</code> if this search match is inside a doc
 * 		comment, and <code>false</code> otherwise
 * @param participant the search participant that created the match
 * @param resource the resource of the element
 * @since 3.3
 */
public MethodReferenceMatch(IJavaElement enclosingElement, int accuracy, int offset, int length, boolean constructor, boolean synthetic, boolean superInvocation, boolean insideDocComment, SearchParticipant participant, IResource resource) {
	this(enclosingElement, accuracy, offset, length, constructor, synthetic, insideDocComment, participant, resource);
	this.superInvocation = superInvocation;
}

/**
 * Returns whether the reference is on a constructor.
 *
 * @return Returns whether the reference is on a constructor or not.
 * @since 3.1
 */
public final boolean isConstructor() {
	return this.constructor;
}

/**
 * Returns whether the reference is on a synthetic element.
 * Note that this field is only used for constructor reference. This happens when default constructor
 * declaration is used or implicit super constructor is called.
 *
 * @return whether the reference is synthetic or not.
 * @since 3.1
 */
public final boolean isSynthetic() {
	return this.synthetic;
}

/**
 * Returns whether the reference is on a message sent from a type
 * which is a super type of the searched method declaring type.
 * If <code>true</code>, the method called at run-time may or may not be
 * the search target, depending on the run-time type of the receiver object.
 *
 * @return <code>true</code> if the reference is on a message sent from
 * a super-type of the searched method declaring class, <code>false</code> otherwise
 * @since 3.3
 */
public boolean isSuperInvocation() {
	return this.superInvocation;
}
}
