/*******************************************************************************
 * Copyright (c) 2000, 2021 IBM Corporation and others.
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
 *     IBM Corporation - added J2SE 1.5 support
 *******************************************************************************/
package org.eclipse.jdt.core;

import java.io.InputStream;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Represents either a source type in a compilation unit (either a top-level
 * type, a member type, a local type, an anonymous type or a lambda expression)
 * or a binary type in a class file. Enumeration classes and annotation
 * types are subkinds of classes and interfaces, respectively.
 * <p>
 * Note that the element name of an anonymous source type and lambda expressions
 * is always empty. Types representing lambda expressions are pseudo-elements
 * and not included in the children of their parent. Lambda expressions are created
 * as the result of a <code>ICodeAssist.codeSelect(...)</code>. For more information
 * on such pseudo-elements, see <code>ILocalVariable</code>.
 * </p><p>
 * If a binary type cannot be parsed, its structure remains unknown.
 * Use <code>IJavaElement.isStructureKnown</code> to determine whether this
 * is the case.
 * </p>
 * <p>
 * The children are of type <code>IMember</code>, which includes <code>IField</code>,
 * <code>IMethod</code>, <code>IInitializer</code> and <code>IType</code>.
 * The children are listed in the order in which they appear in the source or class file.
 * </p>
 * <p>
 * Caveat: The {@link #getChildren() children} of a {@link #isBinary() binary} type include
 * nested types. However, the {@link #getParent() parent} of such a nested binary type is
 * <em>not</em> the enclosing type, but that nested type's {@link IClassFile}!
 * </p>
 *
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IType extends IMember, IAnnotatable {
	/**
	 * Do code completion inside a code snippet in the context of the current type.
	 *
	 * If the type has access to its source code and the insertion position is valid,
	 * then completion is performed against the source. Otherwise the completion is performed
	 * against the type structure and the given locals variables.
	 *
	 * @param snippet the code snippet
	 * @param insertion the position with in source where the snippet
	 * is inserted. This position must not be in comments.
	 * A possible value is -1, if the position is not known.
	 * @param position the position within snippet where the user
	 * is performing code assist.
	 * @param localVariableTypeNames an array (possibly empty) of fully qualified
	 * type names of local variables visible at the current scope
	 * @param localVariableNames an array (possibly empty) of local variable names
	 * that are visible at the current scope
	 * @param localVariableModifiers an array (possible empty) of modifiers for
	 * local variables
	 * @param isStatic whether the current scope is in a static context
	 * @param requestor the completion requestor
	 * @exception JavaModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource.
	 * @since 2.0
	 * @deprecated Use {@link #codeComplete(char[],int,int,char[][],char[][],int[],boolean,CompletionRequestor)} instead.
	 */
	void codeComplete(
		char[] snippet,
		int insertion,
		int position,
		char[][] localVariableTypeNames,
		char[][] localVariableNames,
		int[] localVariableModifiers,
		boolean isStatic,
		ICompletionRequestor requestor)
		throws JavaModelException;

	/**
	 * Do code completion inside a code snippet in the context of the current type.
	 * It considers types in the working copies with the given owner first. In other words,
	 * the owner's working copies will take precedence over their original compilation units
	 * in the workspace.
	 * <p>
	 * Note that if a working copy is empty, it will be as if the original compilation
	 * unit had been deleted.
	 * </p><p>
	 * If the type has access to its source code and the insertion position is valid,
	 * then completion is performed against the source. Otherwise the completion is performed
	 * against the type structure and the given locals variables.
	 * </p>
	 *
	 * @param snippet the code snippet
	 * @param insertion the position with in source where the snippet
	 * is inserted. This position must not be in comments.
	 * A possible value is -1, if the position is not known.
	 * @param position the position with in snippet where the user
	 * is performing code assist.
	 * @param localVariableTypeNames an array (possibly empty) of fully qualified
	 * type names of local variables visible at the current scope
	 * @param localVariableNames an array (possibly empty) of local variable names
	 * that are visible at the current scope
	 * @param localVariableModifiers an array (possible empty) of modifiers for
	 * local variables
	 * @param isStatic whether the current scope is in a static context
	 * @param requestor the completion requestor
	 * @param owner the owner of working copies that take precedence over their original compilation units
	 * @exception JavaModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource.
	 * @since 3.0
	 * @deprecated Use {@link #codeComplete(char[],int,int,char[][],char[][],int[],boolean,CompletionRequestor,WorkingCopyOwner)} instead.
	 */
	void codeComplete(
		char[] snippet,
		int insertion,
		int position,
		char[][] localVariableTypeNames,
		char[][] localVariableNames,
		int[] localVariableModifiers,
		boolean isStatic,
		ICompletionRequestor requestor,
		WorkingCopyOwner owner)
		throws JavaModelException;

	/**
	 * Do code completion inside a code snippet in the context of the current type.
	 *
	 * If the type has access to its source code and the insertion position is valid,
	 * then completion is performed against the source. Otherwise the completion is performed
	 * against the type structure and the given locals variables.
	 *
	 * @param snippet the code snippet
	 * @param insertion the position with in source where the snippet
	 * is inserted. This position must not be in comments.
	 * A possible value is -1, if the position is not known.
	 * @param position the position within snippet where the user
	 * is performing code assist.
	 * @param localVariableTypeNames an array (possibly empty) of fully qualified
	 * type names of local variables visible at the current scope
	 * @param localVariableNames an array (possibly empty) of local variable names
	 * that are visible at the current scope
	 * @param localVariableModifiers an array (possible empty) of modifiers for
	 * local variables
	 * @param isStatic whether the current scope is in a static context
	 * @param requestor the completion requestor
	 * @exception JavaModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource.
	 * @since 3.1
	 */
	void codeComplete(
		char[] snippet,
		int insertion,
		int position,
		char[][] localVariableTypeNames,
		char[][] localVariableNames,
		int[] localVariableModifiers,
		boolean isStatic,
		CompletionRequestor requestor)
		throws JavaModelException;

	/**
	 * Do code completion inside a code snippet in the context of the current type.
	 *
	 * If the type has access to its source code and the insertion position is valid,
	 * then completion is performed against the source. Otherwise the completion is performed
	 * against the type structure and the given locals variables.
	 * <p>
	 * If {@link IProgressMonitor} is not <code>null</code> then some proposals which
	 * can be very long to compute are proposed. To avoid that the code assist operation
	 * take too much time a {@link IProgressMonitor} which automatically cancel the code
	 * assist operation when a specified amount of time is reached could be used.
	 *
	 * <pre>
	 * new IProgressMonitor() {
	 *     private final static int TIMEOUT = 500; //ms
	 *     private long endTime;
	 *     public void beginTask(String name, int totalWork) {
	 *         fEndTime= System.currentTimeMillis() + TIMEOUT;
	 *     }
	 *     public boolean isCanceled() {
	 *         return endTime <= System.currentTimeMillis();
	 *     }
	 *     ...
	 * };
	 * </pre>
	 * <p>
	 *
	 * @param snippet the code snippet
	 * @param insertion the position with in source where the snippet
	 * is inserted. This position must not be in comments.
	 * A possible value is -1, if the position is not known.
	 * @param position the position within snippet where the user
	 * is performing code assist.
	 * @param localVariableTypeNames an array (possibly empty) of fully qualified
	 * type names of local variables visible at the current scope
	 * @param localVariableNames an array (possibly empty) of local variable names
	 * that are visible at the current scope
	 * @param localVariableModifiers an array (possible empty) of modifiers for
	 * local variables
	 * @param isStatic whether the current scope is in a static context
	 * @param requestor the completion requestor
	 * @param monitor the progress monitor used to report progress
	 * @exception JavaModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource.
	 * @since 3.5
	 */
	void codeComplete(
		char[] snippet,
		int insertion,
		int position,
		char[][] localVariableTypeNames,
		char[][] localVariableNames,
		int[] localVariableModifiers,
		boolean isStatic,
		CompletionRequestor requestor,
		IProgressMonitor monitor)
		throws JavaModelException;

	/**
	 * Do code completion inside a code snippet in the context of the current type.
	 * It considers types in the working copies with the given owner first. In other words,
	 * the owner's working copies will take precedence over their original compilation units
	 * in the workspace.
	 * <p>
	 * Note that if a working copy is empty, it will be as if the original compilation
	 * unit had been deleted.
	 * </p><p>
	 * If the type has access to its source code and the insertion position is valid,
	 * then completion is performed against the source. Otherwise the completion is performed
	 * against the type structure and the given locals variables.
	 * </p>
	 *
	 * @param snippet the code snippet
	 * @param insertion the position with in source where the snippet
	 * is inserted. This position must not be in comments.
	 * A possible value is -1, if the position is not known.
	 * @param position the position with in snippet where the user
	 * is performing code assist.
	 * @param localVariableTypeNames an array (possibly empty) of fully qualified
	 * type names of local variables visible at the current scope
	 * @param localVariableNames an array (possibly empty) of local variable names
	 * that are visible at the current scope
	 * @param localVariableModifiers an array (possible empty) of modifiers for
	 * local variables
	 * @param isStatic whether the current scope is in a static context
	 * @param requestor the completion requestor
	 * @param owner the owner of working copies that take precedence over their original compilation units
	 * @exception JavaModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource.
	 * @since 3.1
	 */
	void codeComplete(
		char[] snippet,
		int insertion,
		int position,
		char[][] localVariableTypeNames,
		char[][] localVariableNames,
		int[] localVariableModifiers,
		boolean isStatic,
		CompletionRequestor requestor,
		WorkingCopyOwner owner)
		throws JavaModelException;

	/**
	 * Do code completion inside a code snippet in the context of the current type.
	 * It considers types in the working copies with the given owner first. In other words,
	 * the owner's working copies will take precedence over their original compilation units
	 * in the workspace.
	 * <p>
	 * Note that if a working copy is empty, it will be as if the original compilation
	 * unit had been deleted.
	 * </p><p>
	 * If the type has access to its source code and the insertion position is valid,
	 * then completion is performed against the source. Otherwise the completion is performed
	 * against the type structure and the given locals variables.
	 * </p>
	 * <p>
	 * If {@link IProgressMonitor} is not <code>null</code> then some proposals which
	 * can be very long to compute are proposed. To avoid that the code assist operation
	 * take too much time a {@link IProgressMonitor} which automatically cancel the code
	 * assist operation when a specified amount of time is reached could be used.
	 *
	 * <pre>
	 * new IProgressMonitor() {
	 *     private final static int TIMEOUT = 500; //ms
	 *     private long endTime;
	 *     public void beginTask(String name, int totalWork) {
	 *         endTime= System.currentTimeMillis() + TIMEOUT;
	 *     }
	 *     public boolean isCanceled() {
	 *         return endTime <= System.currentTimeMillis();
	 *     }
	 *     ...
	 * };
	 * </pre>
	 * <p>
	 *
	 * @param snippet the code snippet
	 * @param insertion the position with in source where the snippet
	 * is inserted. This position must not be in comments.
	 * A possible value is -1, if the position is not known.
	 * @param position the position with in snippet where the user
	 * is performing code assist.
	 * @param localVariableTypeNames an array (possibly empty) of fully qualified
	 * type names of local variables visible at the current scope
	 * @param localVariableNames an array (possibly empty) of local variable names
	 * that are visible at the current scope
	 * @param localVariableModifiers an array (possible empty) of modifiers for
	 * local variables
	 * @param isStatic whether the current scope is in a static context
	 * @param requestor the completion requestor
	 * @param owner the owner of working copies that take precedence over their original compilation units
	 * @param monitor the progress monitor used to report progress
	 * @exception JavaModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource.
	 * @since 3.5
	 */
	void codeComplete(
		char[] snippet,
		int insertion,
		int position,
		char[][] localVariableTypeNames,
		char[][] localVariableNames,
		int[] localVariableModifiers,
		boolean isStatic,
		CompletionRequestor requestor,
		WorkingCopyOwner owner,
		IProgressMonitor monitor)
		throws JavaModelException;


	/**
	 * Creates and returns a field in this type with the
	 * given contents.
	 * <p>
	 * Optionally, the new element can be positioned before the specified
	 * sibling. If no sibling is specified, the element will be inserted
	 * as the last field declaration in this type.</p>
	 *
	 * <p>It is possible that a field with the same name already exists in this type.
	 * The value of the <code>force</code> parameter affects the resolution of
	 * such a conflict:
	 * <ul>
	 * <li> <code>true</code> - in this case the field is created with the new contents</li>
	 * <li> <code>false</code> - in this case a <code>JavaModelException</code> is thrown</li>
	 * </ul>
	 *
	 * @param contents the given contents
	 * @param sibling the given sibling
	 * @param force a flag in case the same name already exists in this type
	 * @param monitor the given progress monitor
	 * @exception JavaModelException if the element could not be created. Reasons include:
	 * <ul>
	 * <li> This Java element does not exist (ELEMENT_DOES_NOT_EXIST)</li>
	 * <li> A <code>CoreException</code> occurred while updating an underlying resource
	 * <li> The specified sibling is not a child of this type (INVALID_SIBLING)
	 * <li> The contents could not be recognized as a field declaration (INVALID_CONTENTS)
	 * <li> This type is read-only (binary) (READ_ONLY)
	 * <li> There was a naming collision with an existing field (NAME_COLLISION)
	 * </ul>
	 * @return a field in this type with the given contents
	 */
	IField createField(String contents, IJavaElement sibling, boolean force, IProgressMonitor monitor)
		throws JavaModelException;

	/**
	 * Creates and returns a static initializer in this type with the
	 * given contents.
	 * <p>
	 * Optionally, the new element can be positioned before the specified
	 * sibling. If no sibling is specified, the new initializer is positioned
	 * after the last existing initializer declaration, or as the first member
	 * in the type if there are no initializers.</p>
	 *
	 * @param contents the given contents
	 * @param sibling the given sibling
	 * @param monitor the given progress monitor
	 * @exception JavaModelException if the element could not be created. Reasons include:
	 * <ul>
	 * <li> This element does not exist
	 * <li> A <code>CoreException</code> occurred while updating an underlying resource
	 * <li> The specified sibling is not a child of this type (INVALID_SIBLING)
	 * <li> The contents could not be recognized as an initializer declaration (INVALID_CONTENTS)
	 * <li> This type is read-only (binary) (READ_ONLY)
	 * </ul>
	 * @return a static initializer in this type with the given contents
	 */
	IInitializer createInitializer(String contents, IJavaElement sibling, IProgressMonitor monitor)
		throws JavaModelException;

	/**
	 * Creates and returns a method or constructor in this type with the
	 * given contents.
	 * <p>
	 * Optionally, the new element can be positioned before the specified
	 * sibling. If no sibling is specified, the element will be appended
	 * to this type.
	 *
	 * <p>It is possible that a method with the same signature already exists in this type.
	 * The value of the <code>force</code> parameter affects the resolution of
	 * such a conflict:
	 * <ul>
	 * <li> <code>true</code> - in this case the method is created with the new contents</li>
	 * <li> <code>false</code> - in this case a <code>JavaModelException</code> is thrown</li>
	 * </ul>
	 *
	 * @param contents the given contents
	 * @param sibling the given sibling
	 * @param force a flag in case the same name already exists in this type
	 * @param monitor the given progress monitor
	 * @exception JavaModelException if the element could not be created. Reasons include:
	 * <ul>
	 * <li> This Java element does not exist (ELEMENT_DOES_NOT_EXIST)</li>
	 * <li> A <code>CoreException</code> occurred while updating an underlying resource
	 * <li> The specified sibling is not a child of this type (INVALID_SIBLING)
	 * <li> The contents could not be recognized as a method or constructor
	 *		declaration (INVALID_CONTENTS)
	 * <li> This type is read-only (binary) (READ_ONLY)
	 * <li> There was a naming collision with an existing method (NAME_COLLISION)
	 * </ul>
	 * @return a method or constructor in this type with the given contents
	 */
	IMethod createMethod(String contents, IJavaElement sibling, boolean force, IProgressMonitor monitor)
		throws JavaModelException;

	/**
	 * Creates and returns a type in this type with the
	 * given contents.
	 * <p>
	 * Optionally, the new type can be positioned before the specified
	 * sibling. If no sibling is specified, the type will be appended
	 * to this type.</p>
	 *
	 * <p>It is possible that a type with the same name already exists in this type.
	 * The value of the <code>force</code> parameter affects the resolution of
	 * such a conflict:
	 * <ul>
	 * <li> <code>true</code> - in this case the type is created with the new contents</li>
	 * <li> <code>false</code> - in this case a <code>JavaModelException</code> is thrown</li>
	 * </ul>
	 *
	 * @param contents the given contents
	 * @param sibling the given sibling
	 * @param force a flag in case the same name already exists in this type
	 * @param monitor the given progress monitor
	 * @exception JavaModelException if the element could not be created. Reasons include:
	 * <ul>
	 * <li> This Java element does not exist (ELEMENT_DOES_NOT_EXIST)</li>
	 * <li> A <code>CoreException</code> occurred while updating an underlying resource
	 * <li> The specified sibling is not a child of this type (INVALID_SIBLING)
	 * <li> The contents could not be recognized as a type declaration (INVALID_CONTENTS)
	 * <li> This type is read-only (binary) (READ_ONLY)
	 * <li> There was a naming collision with an existing field (NAME_COLLISION)
	 * </ul>
	 * @return a type in this type with the given contents
	 */
	IType createType(String contents, IJavaElement sibling, boolean force, IProgressMonitor monitor)
		throws JavaModelException;

	/**
	 * Finds the methods in this type that correspond to
	 * the given method.
	 * A method m1 corresponds to another method m2 if:
	 * <ul>
	 * <li>m1 has the same element name as m2.
	 * <li>m1 has the same number of arguments as m2 and
	 *     the simple names of the argument types must be equals.
	 * <li>m1 exists.
	 * </ul>
	 * @param method the given method
	 * @return the found method or <code>null</code> if no such methods can be found.
	 *
	 * @since 2.0
	 */
	IMethod[] findMethods(IMethod method);

	/**
	 * Returns the children of this type that have the given category as a <code>@category</code> tag.
	 * Returns an empty array if no children with this category exist.
	 *
	 * <p>
	 * The results are listed in the order in which they appear in the source or class file.
	 * </p>
	 *
	 * @return the children for the given category.
	 * @exception JavaModelException if this element does not exist or if an
	 *      exception occurs while accessing its corresponding resource.
	 *  @since 3.2
	 */
	IJavaElement[] getChildrenForCategory(String category) throws JavaModelException;

	/**
	 * Returns the simple name of this type, unqualified by package or enclosing type.
	 * This is a handle-only method.
	 *
	 * Note that the element name of an anonymous source type and lambda expressions
	 * is always empty.
	 *
	 * @return the simple name of this type
	 */
	@Override
	String getElementName();

	/**
	 * Returns the field with the specified name
	 * in this type (for example, <code>"bar"</code>).
	 * This is a handle-only method.  The field may or may not exist.
	 *
	 * @param name the given name
	 * @return the field with the specified name in this type
	 */
	IField getField(String name);

	/**
	 * Returns the fields declared by this type in the order in which they appear
	 * in the source or class file. For binary types, this includes synthetic fields.
	 * This does not include the implicit fields representing record components.
	 *
	 * @exception JavaModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource.
	 * @return the fields declared by this type
	 */
	IField[] getFields() throws JavaModelException;

	/**
	 * Returns the fully qualified name of this type,
	 * including qualification for any containing types and packages.
	 * This is the name of the package, followed by <code>'.'</code>,
	 * followed by the type-qualified name.
	 * <p>
	 * <b>Note</b>: The enclosing type separator used in the type-qualified
	 * name is <code>'$'</code>, not <code>'.'</code>.
	 * </p>
	 * This method is fully equivalent to <code>getFullyQualifiedName('$')</code>.
	 * This is a handle-only method.
	 *
	 * @see IType#getTypeQualifiedName()
	 * @see IType#getFullyQualifiedName(char)
	 * @return the fully qualified name of this type
	 */
	String getFullyQualifiedName();

	/**
	 * Returns the fully qualified name of this type,
	 * including qualification for any containing types and packages.
	 * This is the name of the package, followed by <code>'.'</code>,
	 * followed by the type-qualified name using the <code>enclosingTypeSeparator</code>.
	 *
	 * For example:
	 * <ul>
	 * <li>the fully qualified name of a class B defined as a member of a class A in a compilation unit A.java
	 *     in a package x.y using the '.' separator is "x.y.A.B"</li>
	 * <li>the fully qualified name of a class B defined as a member of a class A in a compilation unit A.java
	 *     in a package x.y using the '$' separator is "x.y.A$B"</li>
	 * <li>the fully qualified name of a binary type whose class file is x/y/A$B.class
	 *     using the '.' separator is "x.y.A.B"</li>
	 * <li>the fully qualified name of a binary type whose class file is x/y/A$B.class
	 *     using the '$' separator is "x.y.A$B"</li>
	 * <li>the fully qualified name of an anonymous binary type whose class file is x/y/A$1.class
	 *     using the '.' separator is "x.y.A.1"</li>
	 * </ul>
	 *
	 * This is a handle-only method.
	 *
	 * @param enclosingTypeSeparator the given enclosing type separator
	 * @return the fully qualified name of this type, including qualification for any containing types and packages
	 * @see IType#getTypeQualifiedName(char)
	 * @since 2.0
	 */
	String getFullyQualifiedName(char enclosingTypeSeparator);

	/**
	 * Returns this type's fully qualified name using a '.' enclosing type separator
	 * followed by its type parameters between angle brackets if it is a generic type.
	 * For example, "p.X&lt;T&gt;", "java.util.Map&lt;java.lang.String, p.X&gt;"
	 *
	 * @exception JavaModelException if this element does not exist or if an
	 *      exception occurs while accessing its corresponding resource.
	 * @return the fully qualified parameterized representation of this type
	 * @since 3.1
	 */
	String getFullyQualifiedParameterizedName() throws JavaModelException;

	/**
	 * Returns the initializer with the specified position relative to
	 * the order they are defined in the source.
	 * Numbering starts at 1 (thus the first occurrence is occurrence 1, not occurrence 0).
	 * This is a handle-only method.  The initializer may or may not be present.
	 *
	 * @param occurrenceCount the specified position
	 * @return the initializer with the specified position relative to the order they are defined in the source
	 */
	IInitializer getInitializer(int occurrenceCount);

	/**
	 * Returns the initializers declared by this type. For binary types this is an
	 * empty collection. For source types, the results are listed in the order in
	 * which they appear in the source.
	 *
	 * @exception JavaModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource.
	 * @return the initializers declared by this type
	 */
	IInitializer[] getInitializers() throws JavaModelException;

	/**
	 * Returns the binding key for this type only if the given type is {@link #isResolved() resolved}.
	 * A binding key is a key that uniquely identifies this type. It allows access
	 * to generic info for parameterized types.
	 *
	 * <p>If the given type is not resolved, the returned key is simply the java element's key.
	 * </p>
	 * @return the binding key for this type
	 * @see org.eclipse.jdt.core.dom.IBinding#getKey()
	 * @see BindingKey
	 * @since 3.1
	 * @see #isResolved()
	 */
	String getKey();

	/**
	 * Returns the method with the specified name and parameter types
	 * in this type (for example, <code>"foo", {"I", "QString;"}</code>).
	 * To get the handle for a constructor, the name specified must be the
	 * simple name of the enclosing type.
	 * This is a handle-only method.  The method may or may not be present.
	 * <p>
	 * The type signatures may be either unresolved (for source types)
	 * or resolved (for binary types), and either basic (for basic types)
	 * or rich (for parameterized types). See {@link Signature} for details.
	 * Note that the parameter type signatures for binary methods are expected
	 * to be dot-based.
	 * </p>
	 *
	 * @param name the given name
	 * @param parameterTypeSignatures the given parameter types
	 * @return the method with the specified name and parameter types in this type
	 */
	IMethod getMethod(String name, String[] parameterTypeSignatures);

	/**
	 * Returns the methods and constructors declared by this type.
	 * For binary types, this may include the special <code>&lt;clinit&gt;</code> method
	 * and synthetic methods.
	 * <p>
	 * The results are listed in the order in which they appear in the source or class file.
	 * </p>
	 *
	 * @exception JavaModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource.
	 * @return the methods and constructors declared by this type
	 */
	IMethod[] getMethods() throws JavaModelException;

	/**
	 * Returns the package fragment in which this element is defined.
	 * This is a handle-only method.
	 *
	 * @return the package fragment in which this element is defined
	 */
	IPackageFragment getPackageFragment();

	/**
	 * Returns the name of this type's superclass, or <code>null</code>
	 * for source types that do not specify a superclass.
	 * <p>
	 * For interfaces, the superclass name is always <code>"java.lang.Object"</code>.
	 * For source types, the name as declared is returned, for binary types,
	 * the resolved, qualified name is returned.
	 * For anonymous types, the superclass name is the name appearing after the 'new' keyword'.
	 * If the superclass is a parameterized type, the string
	 * may include its type arguments enclosed in "&lt;&gt;".
	 * If the returned string is needed for anything other than display
	 * purposes, use {@link #getSuperclassTypeSignature()} which returns
	 * a structured type signature string containing more precise information.
	 * </p>
	 *
	 * @exception JavaModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource.
	 * @return the name of this type's superclass, or <code>null</code> for source types that do not specify a superclass
	 */
	String getSuperclassName() throws JavaModelException;

	/**
	 * Returns the type signature of this type's superclass, or
	 * <code>null</code> if none.
	 * <p>
	 * The type signature may be either unresolved (for source types)
	 * or resolved (for binary types), and either basic (for basic types)
	 * or rich (for parameterized types). See {@link Signature} for details.
	 * </p>
	 *
	 * @exception JavaModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource.
	 * @return the type signature of this type's superclass, or
	 * <code>null</code> if none
	 * @since 3.0
	 */
	String getSuperclassTypeSignature() throws JavaModelException;

	/**
	 * Returns the type signatures of the interfaces that this type
	 * implements or extends, in the order in which they are listed in the
	 * source.
	 * <p>
	 * For classes and enum types, this gives the interfaces that this
	 * class implements. For interfaces and annotation types,
	 * this gives the interfaces that this interface extends.
	 * An empty collection is returned if this type does not implement or
	 * extend any interfaces. For anonymous types, an empty collection is
	 * always returned.
	 * </p>
	 * <p>
	 * The type signatures may be either unresolved (for source types)
	 * or resolved (for binary types), and either basic (for basic types)
	 * or rich (for parameterized types). See {@link Signature} for details.
	 * </p>
	 *
	 * @exception JavaModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource.
	 * @return  the type signatures of interfaces that this type implements
	 * or extends, in the order in which they are listed in the source,
	 * an empty collection if none
	 * @since 3.0
	 */
	String[] getSuperInterfaceTypeSignatures() throws JavaModelException;

	/**
	 * Returns the names of interfaces that this type implements or extends,
	 * in the order in which they are listed in the source.
	 * <p>
	 * For classes, this gives the interfaces that this class implements.
	 * For interfaces, this gives the interfaces that this interface extends.
	 * An empty collection is returned if this type does not implement or
	 * extend any interfaces. For source types, simple names are returned,
	 * for binary types, qualified names are returned.
	 * For anonymous types, an empty collection is always returned.
	 * If the list of supertypes includes parameterized types,
	 * the string may include type arguments enclosed in "&lt;&gt;".
	 * If the result is needed for anything other than display
	 * purposes, use {@link #getSuperInterfaceTypeSignatures()} which returns
	 * structured signature strings containing more precise information.
	 * </p>
	 *
	 * @exception JavaModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource.
	 * @return  the names of interfaces that this type implements or extends, in the order in which they are listed in the source,
	 * an empty collection if none
	 */
	String[] getSuperInterfaceNames() throws JavaModelException;

	/**
	 * Returns the names of types that this sealed type permits to be its sub types.
	 * For a non sealed type, an empty array is returned.
	 * If type declares an explicit permits clause, then the permitted sub-types
	 * are returned in the declared order. If a sealed type does not explicitly
	 * declare permitted sub types, then the implicit permitted types, that is,
	 * the types in the same compilation unit that are sub types of this sealed type
	 * are returned in the order they appear within the compilation unit.
	 *
	 * @exception JavaModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource.
	 * @return names of types that this type permits to be its sub types
	 * @since 3.28
	 */
	String[] getPermittedSubtypeNames() throws JavaModelException;

	/**
	 * Returns the formal type parameter signatures for this type.
	 * Returns an empty array if this type has no formal type parameters.
	 * <p>
	 * The formal type parameter signatures may be either unresolved (for source
	 * types) or resolved (for binary types). See {@link Signature} for details.
	 * </p>
	 *
	 * @exception JavaModelException if this element does not exist or if an
	 *      exception occurs while accessing its corresponding resource.
	 * @return the formal type parameter signatures of this type,
	 * in the order declared in the source, an empty array if none
	 * @see Signature
	 * @since 3.0
	 */
	String[] getTypeParameterSignatures() throws JavaModelException;

	/**
	 * Returns the formal type parameters for this type.
	 * Returns an empty array if this type has no formal type parameters.
	 *
	 * @exception JavaModelException if this element does not exist or if an
	 *      exception occurs while accessing its corresponding resource.
	 * @return the formal type parameters of this type,
	 * in the order declared in the source, an empty array if none
	 * @since 3.1
	 */
	ITypeParameter[] getTypeParameters() throws JavaModelException;

	/**
	 * Returns the member type declared in this type with the given simple name.
	 * This is a handle-only method. The type may or may not exist.
	 *
	 * @param name the given simple name
	 * @return the member type declared in this type with the given simple name
	 */
	IType getType(String name);

	/**
	 * Returns the type parameter declared in this type with the given name.
	 * This is a handle-only method. The type parameter may or may not exist.
	 *
	 * @param name the given simple name
	 * @return the type parameter declared in this type with the given name
	 * @since 3.1
	 */
	ITypeParameter getTypeParameter(String name);

	/**
	 * Returns the type-qualified name of this type,
	 * including qualification for any enclosing types,
	 * but not including package qualification.
	 * For source types, this consists of the simple names of any enclosing types,
	 * separated by <code>'$'</code>, followed by the simple name of this type
	 * or the occurrence count of this type if it is anonymous.
	 * For binary types, this is the name of the class file without the ".class" suffix.
	 * This method is fully equivalent to <code>getTypeQualifiedName('$')</code>.
	 * This is a handle-only method.
	 *
	 * @see #getTypeQualifiedName(char)
	 * @return the type-qualified name of this type
	 */
	String getTypeQualifiedName();

	/**
	 * Returns the type-qualified name of this type,
	 * including qualification for any enclosing types,
	 * but not including package qualification.
	 * For source types, this consists of the simple names of any enclosing types,
	 * separated by <code>enclosingTypeSeparator</code>, followed by the
	 * simple name of this type  or the occurrence count of this type if it is anonymous.
	 * For binary types, this is the name of the class file without the ".class" suffix,
	 * and - since 3.4 - the '$' characters in the class file name are replaced with the
	 * <code>enclosingTypeSeparator</code> character.
	 *
	 * For example:
	 * <ul>
	 * <li>the type qualified name of a class B defined as a member of a class A
	 *     using the '.' separator is "A.B"</li>
	 * <li>the type qualified name of a class B defined as a member of a class A
	 *     using the '$' separator is "A$B"</li>
	 * <li>the type qualified name of a binary type whose class file is A$B.class
	 *     using the '.' separator is "A.B"</li>
	 * <li>the type qualified name of a binary type whose class file is A$B.class
	 *     using the '$' separator is "A$B"</li>
	 * <li>the type qualified name of an anonymous binary type whose class file is A$1.class
	 *     using the '.' separator is "A.1"</li>
	 * </ul>
	 *
	 * This is a handle-only method.
	 *
	 * @param enclosingTypeSeparator the specified enclosing type separator
	 * @return the type-qualified name of this type
	 * @since 2.0
	 */
	String getTypeQualifiedName(char enclosingTypeSeparator);

	/**
	 * Returns the immediate member types declared by this type.
	 * The results are listed in the order in which they appear in the source or class file.
	 *
	 * @exception JavaModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource.
	 * @return the immediate member types declared by this type
	 */
	IType[] getTypes() throws JavaModelException;

	/**
	 * Returns whether this type represents an anonymous type.
	 *
	 * @exception JavaModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource.
	 * @return true if this type represents an anonymous type, false otherwise
	 * @since 2.0
	 */
	boolean isAnonymous() throws JavaModelException;

	/**
	 * Returns whether this type represents a class.
	 * <p>
	 * Note that a class can neither be an interface, an enumeration class, nor an annotation type.
	 * </p>
	 *
	 * @exception JavaModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource.
	 * @return true if this type represents a class, false otherwise
	 */
	boolean isClass() throws JavaModelException;

	/**
	 * Returns whether this type represents an enumeration class.
	 * <p>
	 * Note that an enumeration class can neither be a class, an interface, nor an annotation type.
	 * </p>
	 *
	 * @exception JavaModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource.
	 * @return true if this type represents an enumeration class,
	 * false otherwise
	 * @since 3.0
	 */
	boolean isEnum() throws JavaModelException;

	/**
	 * Returns whether this type represents a record class.
	 * <p>
	 * Note that a record class can neither be an enumeration, an interface, nor an annotation type.
	 * </p>
	 *
	 * @exception JavaModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource.
	 * @return true if this type represents a record class,
	 * false otherwise
	 * @since 3.26
	 */
	boolean isRecord() throws JavaModelException;
	/**
	 * Returns whether this type is a sealed type.
	 *
	 * @exception JavaModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource.
	 * @return true if this type this type is a sealed type, false otherwise
	 * @since 3.28
	 */
	boolean isSealed() throws JavaModelException;

	/**
	 * Returns the record components declared by this record class, or an empty
	 * array if this is not a record.
	 *
	 * @exception JavaModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource.
	 * @return record components declared by this record class
	 * @since 3.26
	 */
	default IField[] getRecordComponents() throws JavaModelException {
		return new IField[0];
	}
	/**
	 * Returns the record component with the specified name
	 * in this type (for example, <code>"bar"</code>).
	 * This is a handle-only method. The record component may or may not exist.
	 *
	 * @param name the given name
	 * @return the record component with the specified name in this record
	 * @since 3.26
	 */
	IField getRecordComponent(String name);

	/**
	 * Returns whether this type represents an interface.
	 * <p>
	 * Note that an interface can also be an annotation type, but it can neither be a class nor an enumeration class.
	 * </p>
	 *
	 * @exception JavaModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource.
	 * @return true if this type represents an interface, false otherwise
	 */
	boolean isInterface() throws JavaModelException;

	/**
	 * Returns whether this type represents an annotation type.
	 * <p>
	 * Note that an annotation type is also an interface, but it can neither be a class nor an enumeration class.
	 * </p>
	 *
	 * @exception JavaModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource.
	 * @return true if this type represents an annotation type,
	 * false otherwise
	 * @since 3.0
	 */
	boolean isAnnotation() throws JavaModelException;

	/**
	 * Returns whether this type represents a local type. For an anonymous type,
	 * this method returns true.
	 * <p>
	 * Note: This deviates from JLS3 14.3, which states that anonymous types are
	 * not local types since they do not have a name.
	 * </p>
	 *
	 * @exception JavaModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource.
	 * @return true if this type represents a local type, false otherwise
	 * @see org.eclipse.jdt.core.dom.ITypeBinding#isLocal()
	 * @since 2.0
	 */
	boolean isLocal() throws JavaModelException;

	/**
	 * Returns whether this type represents a member type.
	 *
	 * @exception JavaModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource.
	 * @return true if this type represents a member type, false otherwise
	 * @since 2.0
	 */
	boolean isMember() throws JavaModelException;
	/**
	 * Returns whether this type represents a resolved type.
	 * If a type is resolved, its key contains resolved information.
	 *
	 * @return whether this type represents a resolved type.
	 * @since 3.1
	 */
	boolean isResolved();
	/**
	 * Loads a previously saved ITypeHierarchy from an input stream. A type hierarchy can
	 * be stored using ITypeHierachy#store(OutputStream).
	 *
	 * Only hierarchies originally created by the following methods can be loaded:
	 * <ul>
	 * <li>IType#newSupertypeHierarchy(IProgressMonitor)</li>
	 * <li>IType#newTypeHierarchy(IJavaProject, IProgressMonitor)</li>
	 * <li>IType#newTypeHierarchy(IProgressMonitor)</li>
	 * </ul>
	 *
	 * @param input stream where hierarchy will be read
	 * @param monitor the given progress monitor
	 * @return the stored hierarchy
	 * @exception JavaModelException if the hierarchy could not be restored, reasons include:
	 *      - type is not the focus of the hierarchy or
	 *		- unable to read the input stream (wrong format, IOException during reading, ...)
	 * @see ITypeHierarchy#store(java.io.OutputStream, IProgressMonitor)
	 * @since 2.1
	 */
	ITypeHierarchy loadTypeHierachy(InputStream input, IProgressMonitor monitor) throws JavaModelException;
	/**
	 * Creates and returns a type hierarchy for this type containing
	 * this type and all of its supertypes.
	 *
	 * @param monitor the given progress monitor
	 * @exception JavaModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource.
	 * @return a type hierarchy for this type containing this type and all of its supertypes
	 */
	ITypeHierarchy newSupertypeHierarchy(IProgressMonitor monitor) throws JavaModelException;

	/**
	 * Creates and returns a type hierarchy for this type containing
	 * this type and all of its supertypes, considering types in the given
	 * working copies. In other words, the list of working copies will take
	 * precedence over their original compilation units in the workspace.
	 * <p>
	 * Note that passing an empty working copy will be as if the original compilation
	 * unit had been deleted.
	 * </p>
	 *
	 * @param workingCopies the working copies that take precedence over their original compilation units
	 * @param monitor the given progress monitor
	 * @return a type hierarchy for this type containing this type and all of its supertypes
	 * @exception JavaModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource.
	 * @since 3.0
	 */
	ITypeHierarchy newSupertypeHierarchy(ICompilationUnit[] workingCopies, IProgressMonitor monitor)
		throws JavaModelException;

	/**
	 * Creates and returns a type hierarchy for this type containing
	 * this type and all of its supertypes, considering types in the given
	 * working copies. In other words, the list of working copies will take
	 * precedence over their original compilation units in the workspace.
	 * <p>
	 * Note that passing an empty working copy will be as if the original compilation
	 * unit had been deleted.
	 * </p>
	 *
	 * @param workingCopies the working copies that take precedence over their original compilation units
	 * @param monitor the given progress monitor
	 * @return a type hierarchy for this type containing this type and all of its supertypes
	 * @exception JavaModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource.
	 * @since 2.0
	 * @deprecated Use {@link #newSupertypeHierarchy(ICompilationUnit[], IProgressMonitor)} instead
	 */
	ITypeHierarchy newSupertypeHierarchy(IWorkingCopy[] workingCopies, IProgressMonitor monitor)
		throws JavaModelException;

	/**
	 * Creates and returns a type hierarchy for this type containing
	 * this type and all of its supertypes, considering types in the
	 * working copies with the given owner.
	 * In other words, the owner's working copies will take
	 * precedence over their original compilation units in the workspace.
	 * <p>
	 * Note that if a working copy is empty, it will be as if the original compilation
	 * unit had been deleted.
	 * <p>
	 *
	 * @param owner the owner of working copies that take precedence over their original compilation units
	 * @param monitor the given progress monitor
	 * @return a type hierarchy for this type containing this type and all of its supertypes
	 * @exception JavaModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource.
	 * @since 3.0
	 */
	ITypeHierarchy newSupertypeHierarchy(WorkingCopyOwner owner, IProgressMonitor monitor)
		throws JavaModelException;

	/**
	 * Creates and returns a type hierarchy for this type containing
	 * this type, all of its supertypes, and all its subtypes
	 * in the context of the given project.
	 *
	 * @param project the given project
	 * @param monitor the given progress monitor
	 * @exception JavaModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource.
	 * @return a type hierarchy for this type containing
	 * this type, all of its supertypes, and all its subtypes
	 * in the context of the given project
	 */
	ITypeHierarchy newTypeHierarchy(IJavaProject project, IProgressMonitor monitor) throws JavaModelException;

	/**
	 * Creates and returns a type hierarchy for this type containing
	 * this type, all of its supertypes, and all its subtypes
	 * in the context of the given project, considering types in the
	 * working copies with the given owner.
	 * In other words, the owner's working copies will take
	 * precedence over their original compilation units in the workspace.
	 * <p>
	 * Note that if a working copy is empty, it will be as if the original compilation
	 * unit had been deleted.
	 * <p>
	 *
	 * @param project the given project
	 * @param owner the owner of working copies that take precedence over their original compilation units
	 * @param monitor the given progress monitor
	 * @exception JavaModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource.
	 * @return a type hierarchy for this type containing
	 * this type, all of its supertypes, and all its subtypes
	 * in the context of the given project
	 * @since 3.0
	 */
	ITypeHierarchy newTypeHierarchy(IJavaProject project, WorkingCopyOwner owner, IProgressMonitor monitor) throws JavaModelException;

	/**
	 * Creates and returns a type hierarchy for this type containing
	 * this type, all of its supertypes, and all its subtypes in the workspace.
	 *
	 * @param monitor the given progress monitor
	 * @exception JavaModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource.
	 * @return a type hierarchy for this type containing
	 * this type, all of its supertypes, and all its subtypes in the workspace
	 */
	ITypeHierarchy newTypeHierarchy(IProgressMonitor monitor) throws JavaModelException;

	/**
	 * Creates and returns a type hierarchy for this type containing
	 * this type, all of its supertypes, and all its subtypes in the workspace,
	 * considering types in the given working copies. In other words, the list of working
	 * copies that will take precedence over their original compilation units in the workspace.
	 * <p>
	 * Note that passing an empty working copy will be as if the original compilation
	 * unit had been deleted.
	 *
	 * @param workingCopies the working copies that take precedence over their original compilation units
	 * @param monitor the given progress monitor
	 * @return a type hierarchy for this type containing
	 * this type, all of its supertypes, and all its subtypes in the workspace
	 * @exception JavaModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource.
	 * @since 3.0
	 */
	ITypeHierarchy newTypeHierarchy(ICompilationUnit[] workingCopies, IProgressMonitor monitor) throws JavaModelException;

	/**
	 * Creates and returns a type hierarchy for this type containing
	 * this type, all of its supertypes, and all its subtypes in the workspace,
	 * considering types in the given working copies. In other words, the list of working
	 * copies that will take precedence over their original compilation units in the workspace.
	 * <p>
	 * Note that passing an empty working copy will be as if the original compilation
	 * unit had been deleted.
	 *
	 * @param workingCopies the working copies that take precedence over their original compilation units
	 * @param monitor the given progress monitor
	 * @return a type hierarchy for this type containing
	 * this type, all of its supertypes, and all its subtypes in the workspace
	 * @exception JavaModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource.
	 * @since 2.0
	 * @deprecated Use {@link #newTypeHierarchy(ICompilationUnit[], IProgressMonitor)} instead
	 */
	ITypeHierarchy newTypeHierarchy(IWorkingCopy[] workingCopies, IProgressMonitor monitor) throws JavaModelException;

	/**
	 * Creates and returns a type hierarchy for this type containing
	 * this type, all of its supertypes, and all its subtypes in the workspace,
	 * considering types in the working copies with the given owner.
	 * In other words, the owner's working copies will take
	 * precedence over their original compilation units in the workspace.
	 * <p>
	 * Note that if a working copy is empty, it will be as if the original compilation
	 * unit had been deleted.
	 * <p>
	 *
	 * @param owner the owner of working copies that take precedence over their original compilation units
	 * @param monitor the given progress monitor
	 * @return a type hierarchy for this type containing
	 * this type, all of its supertypes, and all its subtypes in the workspace
	 * @exception JavaModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource.
	 * @since 3.0
	 */
	ITypeHierarchy newTypeHierarchy(WorkingCopyOwner owner, IProgressMonitor monitor) throws JavaModelException;

	/**
	 * Resolves the given type name within the context of this type (depending on the type hierarchy
	 * and its imports).
	 * <p>
	 * Multiple answers might be found in case there are ambiguous matches.
	 * </p>
	 * <p>
	 * Each matching type name is decomposed as an array of two strings, the first denoting the package
	 * name (dot-separated) and the second being the type name. The package name is empty if it is the
	 * default package. The type name is the type qualified name using a '.' enclosing type separator.
	 * </p>
	 * <p>
	 * Returns <code>null</code> if unable to find any matching type.
	 * </p>
	 *<p>
	 * For example, resolution of <code>"Object"</code> would typically return
	 * <code>{{"java.lang", "Object"}}</code>. Another resolution that returns
	 * <code>{{"", "X.Inner"}}</code> represents the inner type Inner defined in type X in the
	 * default package.
	 * </p>
	 *
	 * @param typeName the given type name
	 * @exception JavaModelException if code resolve could not be performed.
	 * @return the resolved type names or <code>null</code> if unable to find any matching type
	 * @see #getTypeQualifiedName(char)
	 */
	String[][] resolveType(String typeName) throws JavaModelException;

	/**
	 * Resolves the given type name within the context of this type (depending on the type hierarchy
	 * and its imports) and using the given owner's working copies, considering types in the
	 * working copies with the given owner. In other words, the owner's working copies will take
	 * precedence over their original compilation units in the workspace.
	 * <p>
	 * Note that if a working copy is empty, it will be as if the original compilation
	 * unit had been deleted.
	 * </p>
	 * <p>Multiple answers might be found in case there are ambiguous matches.
	 * </p>
	 * <p>
	 * Each matching type name is decomposed as an array of two strings, the first denoting the package
	 * name (dot-separated) and the second being the type name. The package name is empty if it is the
	 * default package. The type name is the type qualified name using a '.' enclosing type separator.
	 * </p>
	 * <p>
	 * Returns <code>null</code> if unable to find any matching type.
	 *</p>
	 *<p>
	 * For example, resolution of <code>"Object"</code> would typically return
	 * <code>{{"java.lang", "Object"}}</code>. Another resolution that returns
	 * <code>{{"", "X.Inner"}}</code> represents the inner type Inner defined in type X in the
	 * default package.
	 * </p>
	 *
	 * @param typeName the given type name
	 * @param owner the owner of working copies that take precedence over their original compilation units
	 * @exception JavaModelException if code resolve could not be performed.
	 * @return the resolved type names or <code>null</code> if unable to find any matching type
	 * @see #getTypeQualifiedName(char)
	 * @since 3.0
	 */
	String[][] resolveType(String typeName, WorkingCopyOwner owner) throws JavaModelException;

	/**
	 * Returns whether this type represents a lambda expression.
	 *
	 * @return true if this type represents a lambda expression, false otherwise
	 * @since 3.10
	 */
	public boolean isLambda();

	/**
	 * Strengthen the contract of the inherited method to signal that the returned class file
	 * is always an {@link IOrdinaryClassFile}.
	 * @since 3.14
	 */
	@Override
	IOrdinaryClassFile getClassFile();
}
