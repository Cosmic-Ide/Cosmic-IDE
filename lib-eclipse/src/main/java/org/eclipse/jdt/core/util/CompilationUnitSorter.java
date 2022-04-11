/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
 *     Alex Blewitt - alex_blewitt@yahoo.com https://bugs.eclipse.org/bugs/show_bug.cgi?id=171066
 *******************************************************************************/

package org.eclipse.jdt.core.util;

import java.util.Comparator;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.internal.core.SortElementsOperation;
import org.eclipse.jdt.internal.core.dom.util.DOMASTUtil;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;

/**
 * Operation for sorting members within a compilation unit.
 * <p>
 * This class provides all functionality via static members.
 * </p>
 *
 * @since 2.1
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
@SuppressWarnings("rawtypes")
public final class CompilationUnitSorter {

 	/**
 	 * Private constructor to prevent instantiation.
 	 */
	private CompilationUnitSorter() {
		// Not instantiable
	}

    /**
     * @deprecated marking deprecated as it is using deprecated code
     */
    private static void checkASTLevel(int level) {
        DOMASTUtil.checkASTLevel(level);
    }

	/**
	 * Name of auxillary property whose value can be used to determine the
	 * original relative order of two body declarations. This allows a
	 * comparator to preserve the relative positions of certain kinds of
	 * body declarations when required.
	 * <p>
	 * All body declarations passed to the comparator's <code>compare</code>
	 * method by <code>CompilationUnitSorter.sort</code> carry an
	 * Integer-valued property. The body declaration with the lower value
	 * comes before the one with the higher value. The exact numeric value
	 * of these properties is unspecified.
	 * </p>
	 * <p>
	 * Example usage:
	 * <pre>
	 * BodyDeclaration b1 = (BodyDeclaration) object1;
	 * BodyDeclaration b2 = (BodyDeclaration) object2;
	 * Integer i1 = (Integer) b1.getProperty(RELATIVE_ORDER);
	 * Integer i2 = (Integer) b2.getProperty(RELATIVE_ORDER);
	 * return i1.intValue() - i2.intValue(); // preserve original order
	 * </pre>
	 *
	 * @see #sort(ICompilationUnit, int[], Comparator, int, IProgressMonitor)
	 * @see org.eclipse.jdt.core.dom.BodyDeclaration
	 */
	public static final String RELATIVE_ORDER = "relativeOrder"; //$NON-NLS-1$

	/**
	 * Reorders the declarations in the given compilation unit according to
     * JLS2 rules. The caller is
	 * responsible for arranging in advance that the given compilation unit is
	 * a working copy, and for saving the changes afterwards.
	 * <p>
	 * <b>Note:</b> Reordering the members within a type declaration might be
	 * more than a cosmetic change and could have potentially serious
	 * repercussions. Firstly, the order in which the fields of a type are
	 * initialized is significant in the Java language; reordering fields
	 * and initializers may result in compilation errors or change the execution
	 * behavior of the code. Secondly, reordering a class's members may affect
	 * how its instances are serialized. This operation should therefore be used
	 * with caution and due concern for potential negative side effects.
	 * </p>
	 * <p>
	 * The optional <code>positions</code> array contains a non-decreasing
	 * ordered list of character-based source positions within the compilation
	 * unit's source code string. Upon return from this method, the positions in
	 * the array reflect the corresponding new locations in the modified source
	 * code string. Note that this operation modifies the given array in place.
	 * </p>
	 * <p>
	 * The <code>compare</code> method of the given comparator is passed pairs
	 * of JLS2 AST body declarations (subclasses of <code>BodyDeclaration</code>)
	 * representing body declarations at the same level. The comparator is
	 * called on body declarations of nested classes, including anonymous and
	 * local classes, but always at the same level. Clients need to provide
	 * a comparator implementation (there is no standard comparator). The
	 * <code>RELATIVE_ORDER</code> property attached to these AST nodes afforts
	 * the comparator a way to preserve the original relative order.
	 * </p>
	 * <p>
	 * The body declarations passed as parameters to the comparator
	 * always carry at least the following minimal signature information:
	 * <br>
	 * <table border="1">
	 *	  <tr>
	 *	    <td><code>TypeDeclaration</code></td>
	 *	    <td><code>modifiers, isInterface, name, superclass,
	 *	      superInterfaces<br>
     *		  RELATIVE_ORDER property</code></td>
	 *	  </tr>
	 *	  <tr>
	 *	    <td><code>FieldDeclaration</code></td>
	 *	    <td><code>modifiers, type, fragments
	 *        (VariableDeclarationFragments
	 *	      with name only)<br>
     *		  RELATIVE_ORDER property</code></td>
	 *	  </tr>
	 *	  <tr>
	 *	    <td><code>MethodDeclaration</code></td>
	 *	    <td><code>modifiers, isConstructor, returnType, name,
	 *		  parameters
	 *	      (SingleVariableDeclarations with name and type only),
	 *		  thrownExceptions<br>
     *		  RELATIVE_ORDER property</code></td>
	 *	  </tr>
	 *	  <tr>
	 *	    <td><code>Initializer</code></td>
	 *	    <td><code>modifiers<br>
     *		  RELATIVE_ORDER property</code></td>
	 *	  </tr>
	 * </table>
	 * <p>
	 * Clients should not rely on the AST nodes being properly parented or on
	 * having source range information. (Future releases may provide options
	 * for requesting additional information like source positions, full ASTs,
	 * non-recursive sorting, etc.)
	 * </p>
	 *
	 * @param compilationUnit the given compilation unit, which must be a
	 * working copy
	 * @param positions an array of source positions to map, or
	 * <code>null</code> if none. If supplied, the positions must
	 * character-based source positions within the original source code for
	 * the given compilation unit, arranged in non-decreasing order.
	 * The array is updated in place when this method returns to reflect the
	 * corresponding source positions in the permuted source code string
	 * (but not necessarily any longer in non-decreasing order).
	 * @param comparator the comparator capable of ordering
	 *   <code>BodyDeclaration</code>s; this comparator is passed AST nodes
     *   from a JLS2 AST
	 * @param options bitwise-or of option flags; <code>0</code> for default
	 * behavior (reserved for future growth)
	 * @param monitor the progress monitor to notify, or <code>null</code> if
	 * none
	 * @exception JavaModelException if the compilation unit could not be
	 * sorted. Reasons include:
	 * <ul>
	 * <li> The given compilation unit does not exist (ELEMENT_DOES_NOT_EXIST)</li>
	 * <li> The given compilation unit is not a working copy (INVALID_ELEMENT_TYPES)</li>
	 * <li> A <code>CoreException</code> occurred while accessing the underlying
	 * resource
	 * </ul>
	 * @exception IllegalArgumentException if the given compilation unit is null
	 * or if the given comparator is null.
	 * @see org.eclipse.jdt.core.dom.BodyDeclaration
	 * @see #RELATIVE_ORDER
     * @deprecated Clients should port their code to use the new JLS3 AST API and call
     *    {@link #sort(int, ICompilationUnit, int[], Comparator, int, IProgressMonitor)
     *    CompilationUnitSorter.sort(AST.JLS3, compilationUnit, positions, comparator, options, monitor)}
     *    instead of using this method.
	 */
	public static void sort(ICompilationUnit compilationUnit,
	        int[] positions,
	        Comparator comparator,
	        int options,
	        IProgressMonitor monitor) throws JavaModelException {
		sort(AST.JLS2, compilationUnit, positions, comparator, options, monitor);
	}

    /**
     * Reorders the declarations in the given compilation unit according to
     * the specified AST level. The caller is responsible for arranging in
     * advance that the given compilation unit is a working copy, and for
     * saving the changes afterwards.
     * <p>
     * <b>Note:</b> Reordering the members within a type declaration might be
     * more than a cosmetic change and could have potentially serious
     * repercussions. Firstly, the order in which the fields of a type are
     * initialized is significant in the Java language; reordering fields
     * and initializers may result in compilation errors or change the execution
     * behavior of the code. Secondly, reordering a class's members may affect
     * how its instances are serialized. This operation should therefore be used
     * with caution and due concern for potential negative side effects.
     * </p>
     * <p>
     * The optional <code>positions</code> array contains a non-decreasing
     * ordered list of character-based source positions within the compilation
     * unit's source code string. Upon return from this method, the positions in
     * the array reflect the corresponding new locations in the modified source
     * code string. Note that this operation modifies the given array in place.
     * </p>
     * <p>
     * The <code>compare</code> method of the given comparator is passed pairs
     * of body declarations (subclasses of <code>BodyDeclaration</code>)
     * representing body declarations at the same level. The nodes are from an
     * AST of the specified level
     * ({@link org.eclipse.jdt.core.dom.ASTParser#newParser(int)}. Clients
     * will generally specify the latest available <code>{@link AST}.JLS*</code> constant since that will
     * cover all constructs found in all version of Java source code.
     * The comparator is called on body declarations of nested classes, including
     * anonymous and local classes, but always at the same level. Clients need to provide
     * a comparator implementation (there is no standard comparator). The
     * <code>RELATIVE_ORDER</code> property attached to these AST nodes afforts
     * the comparator a way to preserve the original relative order.
     * </p>
     * <p>
     * The body declarations passed as parameters to the comparator
     * always carry at least the following minimal signature information:
     * <br>
     * <table border="1">
     *    <tr>
     *      <td><code>TypeDeclaration</code></td>
     *      <td><code>modifiers, isInterface, name, superclass,
     *        superInterfaces, typeParameters<br>
     *        RELATIVE_ORDER property</code></td>
     *    </tr>
     *    <tr>
     *      <td><code>FieldDeclaration</code></td>
     *      <td><code>modifiers, type, fragments
     *        (VariableDeclarationFragments
     *        with name only)<br>
     *        RELATIVE_ORDER property</code></td>
     *    </tr>
     *    <tr>
     *      <td><code>MethodDeclaration</code></td>
     *      <td><code>modifiers, isConstructor, returnType, name,
     *        typeParameters, parameters
     *        (SingleVariableDeclarations with name, type, and modifiers only),
     *        thrownExceptions<br>
     *        RELATIVE_ORDER property</code></td>
     *    </tr>
     *    <tr>
     *      <td><code>Initializer</code></td>
     *      <td><code>modifiers<br>
     *        RELATIVE_ORDER property</code></td>
     *    </tr>
     *    <tr>
     *      <td><code>AnnotationTypeDeclaration</code></td>
     *      <td><code>modifiers, name<br>
     *        RELATIVE_ORDER property</code></td>
     *    </tr>
     *    <tr>
     *      <td><code>AnnotationTypeMemberDeclaration</code></td>
     *      <td><code>modifiers, name, type, default<br>
     *        RELATIVE_ORDER property</code></td>
     *    </tr>
     *    <tr>
     *      <td><code>EnumDeclaration</code></td>
     *      <td><code>modifiers, name, superInterfaces<br>
     *        RELATIVE_ORDER property</code></td>
     *    </tr>
     *    <tr>
     *      <td><code>EnumConstantDeclaration</code></td>
     *      <td><code>modifiers, name, arguments<br>
     *        RELATIVE_ORDER property</code></td>
     *    </tr>
     * </table>
     * Clients should not rely on the AST nodes being properly parented or on
     * having source range information. (Future releases may provide options
     * for requesting additional information like source positions, full ASTs,
     * non-recursive sorting, etc.)
     *
     * @param level the AST level; one of the <code>{@link AST}.JLS*</code> constants
     * @param compilationUnit the given compilation unit, which must be a
     * working copy
     * @param positions an array of source positions to map, or
     * <code>null</code> if none. If supplied, the positions must
     * character-based source positions within the original source code for
     * the given compilation unit, arranged in non-decreasing order.
     * The array is updated in place when this method returns to reflect the
     * corresponding source positions in the permuted source code string
     * (but not necessarily any longer in non-decreasing order).
     * @param comparator the comparator capable of ordering
     *   <code>BodyDeclaration</code>s; this comparator is passed AST nodes
     *   from an AST of the specified AST level
     * @param options bitwise-or of option flags; <code>0</code> for default
     * behavior (reserved for future growth)
     * @param monitor the progress monitor to notify, or <code>null</code> if
     * none
     * @exception JavaModelException if the compilation unit could not be
     * sorted. Reasons include:
     * <ul>
     * <li> The given compilation unit does not exist (ELEMENT_DOES_NOT_EXIST)</li>
     * <li> The given compilation unit is not a working copy (INVALID_ELEMENT_TYPES)</li>
     * <li> A <code>CoreException</code> occurred while accessing the underlying
     * resource
     * </ul>
     * @exception IllegalArgumentException if the given compilation unit is null
     * or if the given comparator is null, or if <code>level</code> is not one of
     * the AST JLS level constants.
     * @see org.eclipse.jdt.core.dom.BodyDeclaration
     * @see #RELATIVE_ORDER
     * @since 3.1
     */
    public static void sort(int level, ICompilationUnit compilationUnit,
            int[] positions,
            Comparator comparator,
            int options,
            IProgressMonitor monitor) throws JavaModelException {
        if (compilationUnit == null || comparator == null) {
            throw new IllegalArgumentException();
        }
        checkASTLevel(level);
        ICompilationUnit[] compilationUnits = new ICompilationUnit[] { compilationUnit };
        SortElementsOperation operation = new SortElementsOperation(level, compilationUnits, positions, comparator);
        operation.runOperation(monitor);
    }

	/**
	 * Reorders the declarations in the given compilation unit according to the
	 * specified comparator. The caller is responsible for arranging in advance
	 * that the given compilation unit is a working copy, and for applying the
	 * returned TextEdit afterwards.
	 * <p>
	 * <b>Note:</b> Reordering the members within a type declaration might be
	 * more than a cosmetic change and could have potentially serious
	 * repercussions. Firstly, the order in which the fields of a type are
	 * initialized is significant in the Java language; reordering fields and
	 * initializers may result in compilation errors or change the execution
	 * behavior of the code. Secondly, reordering a class's members may affect
	 * how its instances are serialized. This operation should therefore be used
	 * with caution and due concern for potential negative side effects.
	 * </p>
	 * <p>
	 * The <code>compare</code> method of the given comparator is passed pairs
	 * of body declarations (subclasses of <code>BodyDeclaration</code>)
	 * representing body declarations at the same level.
	 * The comparator is called on body declarations of nested classes,
	 * including anonymous and local classes, but always at the same level.
	 * Clients need to provide a comparator implementation (there is no standard
	 * comparator). The <code>RELATIVE_ORDER</code> property attached to these
	 * AST nodes affords the comparator a way to preserve the original relative
	 * order.
	 * </p>
	 * <p>
	 * The body declarations passed as parameters to the comparator always carry
	 * at least the following minimal signature information: <br>
	 * <table border="1">
	 * <tr>
	 * <td><code>TypeDeclaration</code></td>
	 * <td><code>modifiers, isInterface, name, superclass,
	 *        superInterfaces, typeParameters<br>
	 *        RELATIVE_ORDER property</code></td>
	 * </tr>
	 * <tr>
	 * <td><code>FieldDeclaration</code></td>
	 * <td><code>modifiers, type, fragments
	 *        (VariableDeclarationFragments
	 *        with name only)<br>
	 *        RELATIVE_ORDER property</code></td>
	 * </tr>
	 * <tr>
	 * <td><code>MethodDeclaration</code></td>
	 * <td><code>modifiers, isConstructor, returnType, name,
	 *        typeParameters, parameters
	 *        (SingleVariableDeclarations with name, type, and modifiers only),
	 *        thrownExceptions<br>
	 *        RELATIVE_ORDER property</code></td>
	 * </tr>
	 * <tr>
	 * <td><code>Initializer</code></td>
	 * <td><code>modifiers<br>
	 *        RELATIVE_ORDER property</code></td>
	 * </tr>
	 * <tr>
	 * <td><code>AnnotationTypeDeclaration</code></td>
	 * <td><code>modifiers, name<br>
	 *        RELATIVE_ORDER property</code></td>
	 * </tr>
	 * <tr>
	 * <td><code>AnnotationTypeMemberDeclaration</code></td>
	 * <td><code>modifiers, name, type, default<br>
	 *        RELATIVE_ORDER property</code></td>
	 * </tr>
	 * <tr>
	 * <td><code>EnumDeclaration</code></td>
	 * <td><code>modifiers, name, superInterfaces<br>
	 *        RELATIVE_ORDER property</code></td>
	 * </tr>
	 * <tr>
	 * <td><code>EnumConstantDeclaration</code></td>
	 * <td><code>modifiers, name, arguments<br>
	 *        RELATIVE_ORDER property</code></td>
	 * </tr>
	 * </table>
	 *
	 * @param unit
	 *            the CompilationUnit to sort
	 * @param comparator
	 *            the comparator capable of ordering
	 *            <code>BodyDeclaration</code>s; this comparator is passed
	 *            AST nodes from an AST of the specified AST level
	 * @param options
	 *            bitwise-or of option flags; <code>0</code> for default
	 *            behavior (reserved for future growth)
	 * @param group
	 *            the text edit group to use when generating text edits, or <code>null</code>
	 * @param monitor
	 *            the progress monitor to notify, or <code>null</code> if none
	 * @return a TextEdit describing the required edits to do the sort, or <code>null</code>
	 *            if sorting is not required
	 * @exception JavaModelException
	 *                if the compilation unit could not be sorted. Reasons
	 *                include:
	 *                <ul>
	 *                <li> The given unit was not created from a ICompilationUnit (INVALID_ELEMENT_TYPES)</li>
	 *                </ul>
	 * @exception IllegalArgumentException
	 *                if the given compilation unit is null or if the given
	 *                comparator is null, or if <code>options</code> is not one
	 *                of the supported levels.
	 * @see org.eclipse.jdt.core.dom.BodyDeclaration
	 * @see #RELATIVE_ORDER
	 * @since 3.3
	 */
	public static TextEdit sort(CompilationUnit unit,
			Comparator comparator,
			int options,
			TextEditGroup group,
			IProgressMonitor monitor) throws JavaModelException {
		if (unit == null || comparator == null) {
			throw new IllegalArgumentException();
		}
		SortElementsOperation operation = new SortElementsOperation(unit.getAST().apiLevel(), new IJavaElement[] { unit.getJavaElement() }, null, comparator);
		return operation.calculateEdit(unit, group);
	}
}
