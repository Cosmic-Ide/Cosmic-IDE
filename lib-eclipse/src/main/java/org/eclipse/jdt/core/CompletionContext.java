/*******************************************************************************
 * Copyright (c) 2005, 2020 IBM Corporation and others.
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
 * Completion context.
 *
 * Represent the context in which the completion occurs.
 *
 * @see CompletionRequestor#acceptContext(CompletionContext)
 * @since 3.1
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public class CompletionContext {

	/**
	 * The completed token is the first token of a member declaration.<br>
	 * e.g.
	 * <pre>
	 * public class X {
	 *   Foo| // completion occurs at |
	 * }
	 * </pre>
	 *
	 * @see #getTokenLocation()
	 *
	 * @since 3.4
	 */
	public static final int TL_MEMBER_START = 1;

	/**
	 * The completed token is the first token of a statement.<br>
	 * e.g.
	 * <pre>
	 * public class X {
	 *   public void bar() {
	 *     Foo| // completion occurs at |
	 *   }
	 * }
	 * </pre>
	 *
	 * @see #getTokenLocation()
	 *
	 * @since 3.4
	 */
	public static final int TL_STATEMENT_START = 2;

	/**
	 * The completed token is the first token of a constructor
	 * invocation expression.<br>
	 * e.g.
	 * <pre>
	 * public class X {
	 *   public void bar() {
	 *     new Foo| // completion occurs at |
	 *   }
	 * }
	 * </pre>
	 *
	 * @see #getTokenLocation()
	 *
	 * @since 3.9
	 */
	public static final int TL_CONSTRUCTOR_START = 4;

	/**
	 * The completed token is part of an import statement<br>
	 * e.g.
	 * <pre>
	 * import java.util| // completion occurs at |
	 * </pre>
	 *
	 * @see #getTokenLocation()
	 *
	 * @since 3.21
	 */
	public static final int TL_IN_IMPORT = 8;

	/**
	 * The completion token is unknown.
	 * @since 3.2
	 */
	public static final int TOKEN_KIND_UNKNOWN = 0;

	/**
	 * The completion token is a name.
	 * @since 3.2
	 */
	public static final int TOKEN_KIND_NAME = 1;
	/**
	 * The completion token is a string literal.
	 * The string literal ends quote can be not present the source.
	 * <code>"foo"</code> or <code>"foo</code>.
	 * @since 3.2
	 */

	public static final int TOKEN_KIND_STRING_LITERAL = 2;
	/**
	 * Tell user whether completion takes place in a javadoc comment or not.
	 *
	 * @return boolean true if completion takes place in a javadoc comment, false otherwise.
	 * @since 3.2
	 */
	public boolean isInJavadoc() {
		return false; // default overridden by concrete implementation
	}

	/**
	 * Tell user whether completion takes place in text area of a javadoc comment or not.
	 *
	 * @return boolean true if completion takes place in a text area of a javadoc comment, false otherwise.
	 * @since 3.2
	 */
	public  boolean isInJavadocText() {
		return false; // default overridden by concrete implementation
	}

	/**
	 * Tell user whether completion takes place in a formal reference of a javadoc tag or not.
	 * Tags with formal reference are:
	 * <ul>
	 * 	<li>&#64;see</li>
	 * 	<li>&#64;throws</li>
	 * 	<li>&#64;exception</li>
	 * 	<li>{&#64;link Object}</li>
	 * 	<li>{&#64;linkplain Object}</li>
	 * 	<li>{&#64;value} when compiler compliance is set at leats to 1.5</li>
	 * </ul>
	 *
	 * @return boolean true if completion takes place in formal reference of a javadoc tag, false otherwise.
	 * @since 3.2
	 */
	public boolean isInJavadocFormalReference() {
		return false; // default overridden by concrete implementation
	}

	/**
	 * Returns whether this completion context is an extended context.
	 * Some methods of this context can be used only if this context is an extended context but an extended context consumes more memory.
	 *
	 * @return <code>true</code> if this completion context is an extended context.
	 *
	 * @since 3.4
	 */
	public boolean isExtended() {
		return false; // default overridden by concrete implementation
	}

	/**
	 * Return signatures of expected types of a potential completion proposal at the completion position.
	 *
	 * It's not mandatory to a completion proposal to respect this expectation.
	 *
	 * @return signatures expected types of a potential completion proposal at the completion position or
	 * <code>null</code> if there is no expected types.
	 *
	 * @see Signature
	 */
	public char[][] getExpectedTypesSignatures() {
		return null; // default overridden by concrete implementation
	}

	/**
	 * Return keys of expected types of a potential completion proposal at the completion position.
	 *
	 * It's not mandatory to a completion proposal to respect this expectation.
	 *
	 * @return keys of expected types of a potential completion proposal at the completion position or
	 * <code>null</code> if there is no expected types.
	 *
	 * @see org.eclipse.jdt.core.dom.ASTParser#createASTs(ICompilationUnit[], String[], org.eclipse.jdt.core.dom.ASTRequestor, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public char[][] getExpectedTypesKeys() {
		return null; // default overridden by concrete implementation
	}

	/**
	 * Returns the completed token.
	 * This token is either the identifier or Java language keyword
	 * or the string literal under, immediately preceding,
	 * the original request offset. If the original request offset
	 * is not within or immediately after an identifier or keyword or
	 * a string literal then the returned value is <code>null</code>.
	 *
	 * @return completed token or <code>null</code>
	 * @since 3.2
	 */
	public char[] getToken() {
		return null; // default overridden by concrete implementation
	}

	/**
	 * Returns the kind of completion token being proposed.
	 * <p>
	 * The set of different kinds of completion token is
	 * expected to change over time. It is strongly recommended
	 * that clients do <b>not</b> assume that the kind is one of the
	 * ones they know about, and code defensively for the
	 * possibility of unexpected future growth.
	 * </p>
	 *
	 * @return the kind; one of the kind constants declared on
	 * this class whose name starts with <code>TOKEN_KIND</code>,
	 * or possibly a kind unknown to the caller
	 * @since 3.2
	 */
	public int getTokenKind() {
		return -1; // default overridden by concrete implementation
	}

	/**
	 * Returns the location of completion token being proposed.
	 * The returned location is a bit mask which can contain some values
	 * of the constants declared on this class whose name starts with <code>TL</code>,
	 * or possibly values unknown to the caller.
	 *
	 * <p>
	 * The set of different location values is expected to change over time.
	 * It is strongly recommended that clients do <b>not</b> assume that
	 * the location contains only known value, and code defensively for
	 * the possibility of unexpected future growth.
	 * </p>
	 *
	 * @return the location
	 *
	 * @since 3.4
	 */
	public int getTokenLocation() {
		return -1; // default overridden by concrete implementation
	}

	/**
	 * Returns the character index of the start of the
	 * subrange in the source file buffer containing the
	 * relevant token being completed. This
	 * token is either the identifier or Java language keyword
	 * under, or immediately preceding, the original request
	 * offset. If the original request offset is not within
	 * or immediately after an identifier or keyword, then the
	 * position returned is original request offset and the
	 * token range is empty.
	 *
	 * @return character index of token start position (inclusive)
	 * @since 3.2
	 */
	public int getTokenStart() {
		return -1; // default overridden by concrete implementation
	}

	/**
	 * Returns the character index of the end (exclusive) of the subrange
	 * in the source file buffer containing the
	 * relevant token. When there is no relevant token, the
	 * range is empty
	 * (<code>getTokenEnd() == getTokenStart() - 1</code>).
	 *
	 * @return character index of token end position (exclusive)
	 * @since 3.2
	 */
	// TODO (david) https://bugs.eclipse.org/bugs/show_bug.cgi?id=132558
	public int getTokenEnd() {
		return -1; // default overridden by concrete implementation
	}

	/**
	 * Returns the offset position in the source file buffer
	 * after which code assist is requested.
	 *
	 * @return offset position in the source file buffer
	 * @since 3.2
	 */
	public int getOffset() {
		return -1; // default overridden by concrete implementation
	}

	/**
	 * Returns the innermost enclosing Java element which contains the completion location or <code>null</code> if this element cannot be computed.
	 * The returned Java element and all Java elements in the same compilation unit which can be navigated to from the returned Java element are special Java elements:
	 * <ul>
	 * <li>they are based on the current content of the compilation unit's buffer, they are not the result of a reconcile operation</li>
	 * <li>they are not updated if the buffer changes.</li>
	 * <li>they do not contain local types which are not visible from the completion location.</li>
	 * <li>they do not give information about categories. {@link IMember#getCategories()} will return an empty array</li>
	 * </ul>
	 *
	 * Reasons for returning <code>null</code> include:
	 * <ul>
	 * <li>the compilation unit no longer exists</li>
	 * <li>the completion occurred in a binary type. However this restriction might be relaxed in the future.</li>
	 * </ul>
	 *
	 * @return the innermost enclosing Java element which contains the completion location or <code>null</code> if this element cannot be computed.
	 *
	 * @exception UnsupportedOperationException if the context is not an extended context
	 *
	 * @since 3.4
	 */
	public IJavaElement getEnclosingElement() {
		return null; // default overridden by concrete implementation
	}

	/**
	 * Return the elements which are visible from the completion location and which can be assigned to the given type.
	 * An element is assignable if its type can be assigned to a variable
	 * of the given type, as specified in section 5.2 of <em>The Java Language
	 * Specification, Third Edition</em> (JLS3).
	 * A visible element is either:
	 * <ul>
	 * <li>a {@link ILocalVariable} - the element type is {@link ILocalVariable#getTypeSignature()}</li>
	 * <li>a {@link IField} - the element type is {@link IField#getTypeSignature()}</li>
	 * <li>a {@link IMethod} - the element type is {@link IMethod#getReturnType()}</li>
	 * </ul>
	 *
	 * Returned elements defined in the completed compilation unit are special Java elements:
	 * <ul>
	 * <li>they are based on the current content of the compilation unit's buffer, they are not the result of a reconcile operation</li>
	 * <li>they are not updated if the buffer changes.</li>
	 * <li>they do not contain local types which are not visible from the completion location.</li>
	 * <li>they do not give information about categories. {@link IMember#getCategories()} will return an empty array</li>
	 * </ul>
	 *
	 * Note the array can be empty if:
	 * <ul>
	 * <li>the compilation unit no longer exists</li>
	 * <li>the completion occurred in a binary type. However this restriction might be relaxed in the future.</li>
	 * </ul>
	 *
	 * @param typeSignature elements which can be assigned to this type are returned.
	 * 		If <code>null</code> there is no constraint on the type of the returned elements.
	 *
	 * @return elements which are visible from the completion location and which can be assigned to the given type.
	 *
	 * @exception UnsupportedOperationException if the context is not an extended context
	 *
	 * @see #isExtended()
	 *
	 * @since 3.4
	 */
	public IJavaElement[] getVisibleElements(String typeSignature) {
		return null; // default overridden by concrete implementation
	}

}
