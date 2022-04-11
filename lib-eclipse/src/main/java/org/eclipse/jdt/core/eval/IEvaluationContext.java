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
package org.eclipse.jdt.core.eval;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

/**
 * An evaluation context supports evaluating code snippets.
 * <p>
 * A code snippet is pretty much any valid piece of Java code that could be
 * pasted into the body of a method and compiled. However, there are two
 * areas where the rules are slightly more liberal.
 * </p>
 * <p>
 * First, a code snippet can return heterogeneous types. Inside the same code
 * snippet an <code>int</code> could be returned on one line, and a
 * <code>String</code> on the next, etc. For example, the following would be
 * considered a valid code snippet:
 * <pre>
 * <code>
 * char c = '3';
 * switch (c) {
 *   case '1': return 1;
 *   case '2': return '2';
 *   case '3': return "3";
 *   default: return null;
 * }
 * </code>
 * </pre>
 * <p>
 * Second, if the last statement is only an expression, the <code>return</code>
 * keyword is implied. For example, the following returns <code>false</code>:
 * <pre>
 * <code>
 * int i = 1;
 * i == 2
 * </code>
 * </pre>
 * <p>
 * Global variables are an additional feature of evaluation contexts. Within an
 * evaluation context, global variables maintain their value across evaluations.
 * These variables are particularly useful for storing the result of an
 * evaluation for use in subsequent evaluations.
 * </p>
 * <p>
 * The evaluation context remembers the name of the package in which code
 * snippets are run. The user can set this to any package, thereby gaining
 * access to types that are normally only visible within that package.
 * </p>
 * <p>
 * Finally, the evaluation context remembers a list of import declarations. The
 * user can import any packages and types so that the code snippets may refer
 * to types by their shorter simple names.
 * </p>
 * <p>
 * Example of use:
 * <pre>
 * <code>
 * IJavaProject project = getJavaProject();
 * IEvaluationContext context = project.newEvaluationContext();
 * String codeSnippet = "int i= 0; i++";
 * ICodeSnippetRequestor requestor = ...;
 * context.evaluateCodeSnippet(codeSnippet, requestor, progressMonitor);
 * </code>
 * </pre>
 * <p>
 * <code>IJavaProject.newEvaluationContext</code> can be used to obtain an
 * instance.
 * </p>
 *
 * @see IJavaProject#newEvaluationContext()
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IEvaluationContext {
	/**
	 * Returns the global variables declared in this evaluation context.
	 * The variables are maintained in the order they are created in.
	 *
	 * @return the list of global variables
	 */
	public IGlobalVariable[] allVariables();
	/**
	 * Performs a code completion at the given position in the given code snippet,
	 * reporting results to the given completion requestor.
	 * <p>
	 * Note that code completion does not involve evaluation.
	 * <p>
	 *
	 * @param codeSnippet the code snippet to complete in
	 * @param position the character position in the code snippet to complete at,
	 *   or -1 indicating the beginning of the snippet
	 * @param requestor the code completion requestor capable of accepting all
	 *    possible types of completions
	 * @exception JavaModelException if code completion could not be performed. Reasons include:
	 *  <ul>
	 *	  <li>The position specified is less than -1 or is greater than the snippet's
	 *	    length (INDEX_OUT_OF_BOUNDS)</li>
	 *  </ul>
	 * @since 2.0
	 * @deprecated Use {@link #codeComplete(String,int,CompletionRequestor)} instead.
	 */
	public void codeComplete(
		String codeSnippet,
		int position,
		ICompletionRequestor requestor)
		throws JavaModelException;
	/**
	 * Performs a code completion at the given position in the given code snippet,
	 * reporting results to the given completion requestor.
	 * It considers types in the working copies with the given owner first. In other words,
	 * the owner's working copies will take precedence over their original compilation units
	 * in the workspace.
	 * <p>
	 * Note that if a working copy is empty, it will be as if the original compilation
	 * unit had been deleted.
	 * </p>
	 * <p>
	 * Note that code completion does not involve evaluation.
	 * <p>
	 *
	 * @param codeSnippet the code snippet to complete in
	 * @param position the character position in the code snippet to complete at,
	 *   or -1 indicating the beginning of the snippet
	 * @param requestor the code completion requestor capable of accepting all
	 *    possible types of completions
	 * @param owner the owner of working copies that take precedence over their original compilation units
	 * @exception JavaModelException if code completion could not be performed. Reasons include:
	 *  <ul>
	 *	  <li>The position specified is less than -1 or is greater than the snippet's
	 *	    length (INDEX_OUT_OF_BOUNDS)</li>
	 *  </ul>
	 * @since 3.0
	 * @deprecated Use {@link #codeComplete(String,int,CompletionRequestor,WorkingCopyOwner)} instead.
	 */
	public void codeComplete(
		String codeSnippet,
		int position,
		ICompletionRequestor requestor,
		WorkingCopyOwner owner)
		throws JavaModelException;
	/**
	 * Performs a code completion at the given position in the given code snippet,
	 * reporting results to the given completion requestor.
	 * <p>
	 * Note that code completion does not involve evaluation.
	 * <p>
	 *
	 * @param codeSnippet the code snippet to complete in
	 * @param position the character position in the code snippet to complete at,
	 *   or -1 indicating the beginning of the snippet
	 * @param requestor the code completion requestor capable of accepting all
	 *    possible types of completions
	 * @exception JavaModelException if code completion could not be performed. Reasons include:
	 *  <ul>
	 *	  <li>The position specified is less than -1 or is greater than the snippet's
	 *	    length (INDEX_OUT_OF_BOUNDS)</li>
	 *  </ul>
	 * @since 3.1
	 */
	public void codeComplete(
		String codeSnippet,
		int position,
		CompletionRequestor requestor)
		throws JavaModelException;

	/**
	 * Performs a code completion at the given position in the given code snippet,
	 * reporting results to the given completion requestor.
	 * <p>
	 * Note that code completion does not involve evaluation.
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
	 * @param codeSnippet the code snippet to complete in
	 * @param position the character position in the code snippet to complete at,
	 *   or -1 indicating the beginning of the snippet
	 * @param requestor the code completion requestor capable of accepting all
	 *    possible types of completions
	 * @param monitor the progress monitor used to report progress
	 * @exception JavaModelException if code completion could not be performed. Reasons include:
	 *  <ul>
	 *	  <li>The position specified is less than -1 or is greater than the snippet's
	 *	    length (INDEX_OUT_OF_BOUNDS)</li>
	 *  </ul>
	 * @since 3.5
	 */
	public void codeComplete(
		String codeSnippet,
		int position,
		CompletionRequestor requestor,
		IProgressMonitor monitor)
		throws JavaModelException;

	/**
	 * Performs a code completion at the given position in the given code snippet,
	 * reporting results to the given completion requestor.
	 * It considers types in the working copies with the given owner first. In other words,
	 * the owner's working copies will take precedence over their original compilation units
	 * in the workspace.
	 * <p>
	 * Note that if a working copy is empty, it will be as if the original compilation
	 * unit had been deleted.
	 * </p>
	 * <p>
	 * Note that code completion does not involve evaluation.
	 * <p>
	 *
	 * @param codeSnippet the code snippet to complete in
	 * @param position the character position in the code snippet to complete at,
	 *   or -1 indicating the beginning of the snippet
	 * @param requestor the code completion requestor capable of accepting all
	 *    possible types of completions
	 * @param owner the owner of working copies that take precedence over their original compilation units
	 * @exception JavaModelException if code completion could not be performed. Reasons include:
	 *  <ul>
	 *	  <li>The position specified is less than -1 or is greater than the snippet's
	 *	    length (INDEX_OUT_OF_BOUNDS)</li>
	 *  </ul>
	 * @since 3.1
	 */
	public void codeComplete(
		String codeSnippet,
		int position,
		CompletionRequestor requestor,
		WorkingCopyOwner owner)
		throws JavaModelException;

	/**
	 * Performs a code completion at the given position in the given code snippet,
	 * reporting results to the given completion requestor.
	 * It considers types in the working copies with the given owner first. In other words,
	 * the owner's working copies will take precedence over their original compilation units
	 * in the workspace.
	 * <p>
	 * Note that if a working copy is empty, it will be as if the original compilation
	 * unit had been deleted.
	 * </p>
	 * <p>
	 * Note that code completion does not involve evaluation.
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
	 * @param codeSnippet the code snippet to complete in
	 * @param position the character position in the code snippet to complete at,
	 *   or -1 indicating the beginning of the snippet
	 * @param requestor the code completion requestor capable of accepting all
	 *    possible types of completions
	 * @param owner the owner of working copies that take precedence over their original compilation units
	 * @param monitor the progress monitor used to report progress
	 * @exception JavaModelException if code completion could not be performed. Reasons include:
	 *  <ul>
	 *	  <li>The position specified is less than -1 or is greater than the snippet's
	 *	    length (INDEX_OUT_OF_BOUNDS)</li>
	 *  </ul>
	 * @since 3.5
	 */
	public void codeComplete(
		String codeSnippet,
		int position,
		CompletionRequestor requestor,
		WorkingCopyOwner owner,
		IProgressMonitor monitor)
		throws JavaModelException;

	/**
	 * Resolves and returns a collection of Java elements corresponding to the source
	 * code at the given positions in the given code snippet.
	 * <p>
	 * Note that code select does not involve evaluation, and problems are never
	 * reported.
	 * <p>
	 *
	 * @param codeSnippet the code snippet to resolve in
	 * @param offset the position in the code snippet of the first character
	 *   of the code to resolve
	 * @param length the length of the selected code to resolve
	 * @return the (possibly empty) list of selection Java elements
	 * @exception JavaModelException if code resolve could not be performed.
	 *   Reasons include:
	 *   <ul>
	 *	   <li>The position specified is less than -1 or is greater than the snippet's
	 *	     length (INDEX_OUT_OF_BOUNDS)</li>
	 *   </ul>
	 */
	public IJavaElement[] codeSelect(String codeSnippet, int offset, int length)
		throws JavaModelException;
	/**
	 * Resolves and returns a collection of Java elements corresponding to the source
	 * code at the given positions in the given code snippet.
	 * It considers types in the working copies with the given owner first. In other words,
	 * the owner's working copies will take precedence over their original compilation units
	 * in the workspace.
	 * <p>
	 * Note that if a working copy is empty, it will be as if the original compilation
	 * unit had been deleted.
	 * </p>
	 * <p>
	 * Note that code select does not involve evaluation, and problems are never
	 * reported.
	 * <p>
	 *
	 * @param codeSnippet the code snippet to resolve in
	 * @param offset the position in the code snippet of the first character
	 *   of the code to resolve
	 * @param length the length of the selected code to resolve
	 * @param owner the owner of working copies that take precedence over their original compilation units
	 * @return the (possibly empty) list of selection Java elements
	 * @exception JavaModelException if code resolve could not be performed.
	 *   Reasons include:
	 *   <ul>
	 *	   <li>The position specified is less than -1 or is greater than the snippet's
	 *	     length (INDEX_OUT_OF_BOUNDS)</li>
	 *   </ul>
	 * @since 3.0
	 */
	public IJavaElement[] codeSelect(String codeSnippet, int offset, int length, WorkingCopyOwner owner)
		throws JavaModelException;
	/**
	 * Deletes the given variable from this evaluation context. Does nothing if
	 * the given variable has already been deleted.
	 *
	 * @param variable the global variable
	 */
	public void deleteVariable(IGlobalVariable variable);
	/**
	 * Evaluates the given code snippet in the context of a suspended thread.
	 * The code snippet is compiled along with this context's package declaration,
	 * imports, and global variables. The given requestor's
	 * <code>acceptProblem</code> method is called for each compilation problem that
	 * is detected. Then the resulting class files are handed to the given
	 * requestor's <code>acceptClassFiles</code> method to deploy and run.
	 * <p>
	 * The requestor is expected to:
	 * <ol>
	 *   <li>send the class files to the target VM,
	 *   <li>load them (starting with the code snippet class),
	 *   <li>create a new instance of the code snippet class,
	 *   <li>run the method <code>run()</code> of the code snippet,
	 *   <li>retrieve the values of the local variables,
	 *   <li>retrieve the returned value of the code snippet
	 * </ol>
	 * <p>
	 * This method is long-running; progress and cancellation are provided
	 * by the given progress monitor.
	 * </p>
	 *
	 * @param codeSnippet the code snippet
	 * @param localVariableTypeNames the dot-separated fully qualified names of the types of the local variables.
	 * @param localVariableNames the names of the local variables as they are declared in the user's code.
	 * @param localVariableModifiers the modifiers of the local variables (default modifier or final modifier).
	 * @param declaringType the type in which the code snippet is evaluated.
	 * @param isStatic whether the code snippet is evaluated in a static member of the declaring type.
	 * @param isConstructorCall whether the code snippet is evaluated in a constructor of the declaring type.
	 * @param requestor the code snippet requestor
	 * @param progressMonitor a progress monitor
	 * @exception JavaModelException if a runtime problem occurred or if this
	 *   context's project has no build state
	 */
	public void evaluateCodeSnippet(
		String codeSnippet,
		String[] localVariableTypeNames,
		String[] localVariableNames,
		int[] localVariableModifiers,
		IType declaringType,
		boolean isStatic,
		boolean isConstructorCall,
		ICodeSnippetRequestor requestor,
		IProgressMonitor progressMonitor)
		throws JavaModelException;
	/**
	 * Evaluates the given code snippet. The code snippet is
	 * compiled along with this context's package declaration, imports, and
	 * global variables. The given requestor's <code>acceptProblem</code> method
	 * is called for each compilation problem that is detected. Then the resulting
	 * class files are handed to the given requestor's <code>acceptClassFiles</code>
	 * method to deploy and run. The requestor is also responsible for getting the
	 * result back.
	 * <p>
	 * This method is long-running; progress and cancellation are provided
	 * by the given progress monitor.
	 * </p>
	 *
	 * @param codeSnippet the code snippet
	 * @param requestor the code snippet requestor
	 * @param progressMonitor a progress monitor
	 * @exception JavaModelException if a runtime problem occurred or if this
	 *   context's project has no build state
	 */
	public void evaluateCodeSnippet(
		String codeSnippet,
		ICodeSnippetRequestor requestor,
		IProgressMonitor progressMonitor)
		throws JavaModelException;
	/**
	 * Evaluates the given global variable. During this operation,
	 * this context's package declaration, imports, and <i>all</i> its declared
	 * variables are verified. The given requestor's <code>acceptProblem</code>
	 * method will be called for each problem that is detected.
	 * <p>
	 * This method is long-running; progress and cancellation are provided
	 * by the given progress monitor.
	 * </p>
	 *
	 * @param variable the global variable
	 * @param requestor the code snippet requestor
	 * @param progressMonitor a progress monitor
	 * @exception JavaModelException if a runtime problem occurred or if this
	 *   context's project has no build state
	 */
	public void evaluateVariable(
		IGlobalVariable variable,
		ICodeSnippetRequestor requestor,
		IProgressMonitor progressMonitor)
		throws JavaModelException;
	/**
	 * Returns the import declarations for this evaluation context. Returns and empty
	 * list if there are no imports (the default if the imports have never been set).
	 * The syntax for the import corresponds to a fully qualified type name, or to
	 * an on-demand package name as defined by ImportDeclaration (JLS2 7.5). For
	 * example, <code>"java.util.Hashtable"</code> or <code>"java.util.*"</code>.
	 *
	 * @return the list of import names
	 */
	public String[] getImports();
	/**
	 * Returns the name of the package in which code snippets are to be compiled and
	 * run. Returns an empty string for the default package (the default if the
	 * package name has never been set). For example, <code>"com.example.myapp"</code>.
	 *
	 * @return the dot-separated package name, or the empty string indicating the
	 *   default package
	 */
	public String getPackageName();
	/**
	 * Returns the Java project this evaluation context was created for.
	 *
	 * @return the Java project
	 */
	public IJavaProject getProject();
	/**
	 * Creates a new global variable with the given name, type, and initializer.
	 * <p>
	 * The <code>typeName</code> and <code>initializer</code> are interpreted in
	 * the context of this context's package and import declarations.
	 * </p>
	* <p>
	 * The syntax for a type name corresponds to Type in Field Declaration (JLS2 8.3).
	 * </p>
	 *
	 * @param typeName the type name
	 * @param name the name of the global variable
	 * @param initializer the initializer expression, or <code>null</code> if the
	 *   variable is not initialized
	 * @return a new global variable with the given name, type, and initializer
	 */
	public IGlobalVariable newVariable(
		String typeName,
		String name,
		String initializer);
	/**
	 * Sets the import declarations for this evaluation context. An empty
	 * list indicates there are no imports. The syntax for the import corresponds to a
	 * fully qualified type name, or to an on-demand package name as defined by
	 * ImportDeclaration (JLS2 7.5). For example, <code>"java.util.Hashtable"</code>
	 * or <code>"java.util.*"</code>.
	 *
	 * @param imports the list of import names
	 */
	public void setImports(String[] imports);
	/**
	 * Sets the dot-separated name of the package in which code snippets are
	 * to be compiled and run. For example, <code>"com.example.myapp"</code>.
	 *
	 * @param packageName the dot-separated package name, or the empty string
	 *   indicating the default package
	 */
	public void setPackageName(String packageName);
	/**
	 * Validates this evaluation context's import declarations. The given requestor's
	 * <code>acceptProblem</code> method is called for each problem that is detected.
	 *
	 * @param requestor the code snippet requestor
	 * @exception JavaModelException if this context's project has no build state
	 */
	public void validateImports(ICodeSnippetRequestor requestor)
		throws JavaModelException;

	/**
	 * Performs a code completion at the given position in the given code snippet,
	 * reporting results to the given completion requestor.
	 * <p>
	 * Note that code completion does not involve evaluation.
	 * <p>
	 *
	 * @param codeSnippet the code snippet to complete in
	 * @param position the character position in the code snippet to complete at,
	 *   or -1 indicating the beginning of the snippet
	 * @param requestor the code completion requestor capable of accepting all
	 *    possible types of completions
	 * @exception JavaModelException if code completion could not be performed. Reasons include:
	 *  <ul>
	 *	  <li>The position specified is less than -1 or is greater than the snippet's
	 *	    length (INDEX_OUT_OF_BOUNDS)</li>
	 *  </ul>
	 * @deprecated - use codeComplete(String, int, ICompletionRequestor) instead
	 */
	public void codeComplete(
		String codeSnippet,
		int position,
		org.eclipse.jdt.core.ICodeCompletionRequestor requestor)
		throws JavaModelException;

}
