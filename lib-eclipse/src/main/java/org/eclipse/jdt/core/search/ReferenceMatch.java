/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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
 * An abstract Java search match that represents a reference.
 *
 * @since 3.4
 */
public abstract class ReferenceMatch extends SearchMatch {

	IJavaElement localElement;

/**
 * Creates a new reference match.
 *
 * @param enclosingElement the inner-most enclosing member that references this java element
 * @param accuracy one of {@link #A_ACCURATE} or {@link #A_INACCURATE}
 * @param offset the offset the match starts at, or -1 if unknown
 * @param length the length of the match, or -1 if unknown
 * @param insideDocComment <code>true</code> if this search match is inside a doc
 * 		comment, and <code>false</code> otherwise
 * @param participant the search participant that created the match
 * @param resource the resource of the element
 */
public ReferenceMatch(IJavaElement enclosingElement, int accuracy, int offset, int length, boolean insideDocComment, SearchParticipant participant, IResource resource) {
	super(enclosingElement, accuracy, offset, length, participant, resource);
	setInsideDocComment(insideDocComment);
}

/**
 * Returns the local element of this search match, or <code>null</code> if none.
 * A local element is the inner-most element that contains the reference and that is
 * not reachable by navigating from the root of the {@link IJavaModel} using
 * {@link IParent#getChildren()}.
 * <p>
 * Known element types for local elements are {@link IJavaElement#ANNOTATION},
 * {@link IJavaElement#LOCAL_VARIABLE} and {@link IJavaElement#TYPE_PARAMETER}.<br>
 * However clients should not assume that this set of element types is closed as
 * other types of elements may be returned in the future, e.g. if new types
 * of elements are added in the Java model. Clients can only assume that the
 * {@link IJavaElement#getParent() parent} chain of this local element eventually
 * leads to the element from {@link #getElement()}.
 * </p><p>
 * The local element being an {@link IAnnotation} is the most usual case. For example,
 * <ul>
 * 	<li>searching for the references to the method <code>Annot.clazz()</code> in
 * 			<pre>
 *             public class Test {
 *                 void method() {
 *                     &#0064;Annot(clazz=Test.class) int x;
 *                 }
 *             }</pre>
 * 			will return one {@link MethodReferenceMatch} match whose local element
 * 			is the {@link IAnnotation} '<code>Annot</code>'.<br><br>
 * 	</li>
 *		<li>searching for the references to the type <code>Deprecated</code> in
 *				<pre>
 *             public class Test {
 *                &#0064;Deprecated void method() {}
 *             }</pre>
 * 			will return one {@link TypeReferenceMatch} match whose local element
 * 			is the {@link IAnnotation} '<code>Deprecated</code>'.<br><br>
 * 	</li>
 * 	<li>searching for the references to the field <code>CONST</code> in
 * 			<pre>
 *              &#0064;Num(number= Num.CONST)
 *              &#0064;interface Num {
 *                  public static final int CONST= 42;
 *                  int number();
 *              }</pre>
 * 			will return one {@link FieldReferenceMatch} match whose local element
 * 			is the {@link IAnnotation} '<code>Num</code>'.<br><br>
 * 	</li>
 * </ul>
 * <p>
 * A local element may also be a {@link ILocalVariable} whose type is the referenced
 * type. For example,
 * <ul>
 * 	<li>searching for the references to the type <code>Test</code> in
 * 		<pre>
 *         public class Test {
 *             void foo() {
 *                Test local;
 *             }
 *         }</pre>
 * 		will return one {@link TypeReferenceMatch} match whose local element
 * 		is the {@link ILocalVariable} '<code>local</code>'.<br><br>
 * 	</li>
 * </ul>
 * Or a local element may be an {@link ITypeParameter} that extends the referenced
 * type. For example,
 * <ul>
 * 	<li>searching for the references to the type <code>Test</code> in
 * 		<pre>
 *         public class X&lt; T extends Test&gt; {
 *         }</pre>
 * 		will return one {@link TypeReferenceMatch} match whose local element
 * 		is the {@link ITypeParameter} '<code>T</code>'.<br><br>
 * </ul>
 *
 * @return the local element of this search match, or <code>null</code> if none.
 *
 * @since 3.4
 */
final public IJavaElement getLocalElement() {
	return this.localElement;
}

/**
 * Store the local element in the match.
 *
 * @param element The local element to be stored
 *
 * @since 3.5
 */
final public void setLocalElement(IJavaElement element) {
	this.localElement = element;
}
}
