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
package org.eclipse.jdt.core.search;

import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.*;

/**
 * A Java search match that represents a type reference.
 * The element is the inner-most enclosing member that references this type.
 * <p>
 * This class is intended to be instantiated and subclassed by clients.
 * </p>
 *
 * @since 3.0
 */
public class TypeReferenceMatch extends ReferenceMatch {

	private IJavaElement[] otherElements;

/**
 * Creates a new type reference match.
 *
 * @param enclosingElement the inner-most enclosing member that references this type
 * @param accuracy one of {@link #A_ACCURATE} or {@link #A_INACCURATE}
 * @param offset the offset the match starts at, or -1 if unknown
 * @param length the length of the match, or -1 if unknown
 * @param insideDocComment <code>true</code> if this search match is inside a doc
 * 				comment, and <code>false</code> otherwise
 * @param participant the search participant that created the match
 * @param resource the resource of the element
 */
public TypeReferenceMatch(IJavaElement enclosingElement, int accuracy, int offset, int length, boolean insideDocComment, SearchParticipant participant, IResource resource) {
	super(enclosingElement, accuracy, offset, length, insideDocComment, participant, resource);
}

/**
 * Returns other elements also enclosing the type reference. This typically can
 * happen for multiple fields or local variable declarations.
 *<p>
 * For example,
 * <ul>
 * 	<li>searching for the references to the type <code>Test</code> in
 *         <pre>
 *         public class Test {
 *             Test test1, test2, test3;
 *             void method() {}
 *         }
 *         </pre>
 * 		will return one match whose other elements is an array of two fields:
 * 		{@link IField test2} and {@link IField test3}.
 * 		<br><br>
 * 	</li>
 * 	<li>searching for the references to the type <code>Test</code> in
 * 		<pre>
 *         public class Test {
 *             String str;
 *             void method() {
 *                 Test local1, local2, local3;
 *             }
 *         }
 *         </pre>
 * 		will return one match whose other elements is an array of two local
 * 		variables: {@link ILocalVariable local2} and {@link ILocalVariable local3}.
 * 		<br><br>
 * 	</li>
 * 	<li>since 3.6, searching for the references to the type
 * 		<code>Test</code> in
 * 		<pre>
 *         public class Test {
 *                 void testB(int testKind) {
 *                         &#064;Annot int test1, test2;
 *                 }
 *         }
 *         &#064;interface Annot {}
 *         </pre>
 * 		will return one match whose other elements is an array of one annotation:
 * 		{@link IAnnotation Annot} which parent is the local variable
 * 		{@link ILocalVariable test2}.
 * 	</li>
 * </ul>
 *
 * @return the other elements of the search match, or <code>null</code> if none
 * @since 3.2
 */
public final IJavaElement[] getOtherElements() {
	return this.otherElements;
}

/**
 * Sets the other elements of this search match.
 *
 * @see #getOtherElements()
 *
 * @param otherElements the other elements of the match,
 * 	or <code>null</code> if none
 * @since 3.2
 */
public final void setOtherElements(IJavaElement[] otherElements) {
	this.otherElements = otherElements;
}
}
