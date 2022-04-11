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

import org.eclipse.core.resources.IMarker;
import org.eclipse.jdt.internal.eval.EvaluationConstants;

/**
 * A code snippet requestor implements a callback interface for installing
 * the class files for a code snippet on the target and running it.
 * In addition, it receives compilation problems detected during code snippet
 * compilation.
 * <p>
 * Clients may implement this interface to provide a bridge a running Java VM.
 * </p>
 *
 * @see IEvaluationContext#evaluateCodeSnippet(String, ICodeSnippetRequestor, org.eclipse.core.runtime.IProgressMonitor)
 * @see IEvaluationContext#evaluateCodeSnippet(String, String[], String[], int[], org.eclipse.jdt.core.IType, boolean, boolean, ICodeSnippetRequestor, org.eclipse.core.runtime.IProgressMonitor)
 */
public interface ICodeSnippetRequestor {

	/**
	 * The prefix of fields that represent the local variables in a snippet
	 * class.
	 */
	public static final String LOCAL_VAR_PREFIX = new String(EvaluationConstants.LOCAL_VAR_PREFIX);

	/**
	 * The name of the field that represent 'this' in a snippet class
	 * instance.
	 */
	public static final String DELEGATE_THIS = new String(EvaluationConstants.DELEGATE_THIS);

	/**
	 * The name of the instance method in the snippet class that runs the code
	 * snippet.
	 */
	public static final String RUN_METHOD = EvaluationConstants.RUN_METHOD;

	/**
	 * The name of the field (of type <code>java.lang.Object</code>) on the code
	 * snippet instance that contains the returned value.
	 */
	public static final String RESULT_VALUE_FIELD = EvaluationConstants.RESULT_VALUE_FIELD;

	/**
	 * The field of type java.lang.Class on the code snippet instance that contains the type of the returned value.
	 * The name of the field (of type <code>java.lang.Class</code>) on the code
	 * snippet instance that contains the runtime type of the returned value.
	 */
	public static final String RESULT_TYPE_FIELD = EvaluationConstants.RESULT_TYPE_FIELD;

	/*
	 * REPORTING A PROBLEM OF COMPILATION IN THE CODE SNIPPET
	 */

	/**
	 * Indicates a compilation problem related to a global variable.
	 * <p>
	 * Note: if the problem is on the type of the variable, the marker
	 * source line number is -1; if the name of the variable, line number is 0;
	 * otherwise, the marker source line number is relative to the initializer
	 * code.
	 * </p>
	 *
	 * @see #acceptProblem(IMarker, String, int)
	 */
	public static final int VARIABLE = 1;

	/**
	 * Indicates a compilation problem related to a code snippet.
	 *
	 * @see #acceptProblem(IMarker, String, int)
	 */
	public static final int CODE_SNIPPET = 2;

	/**
	 * Indicates a compilation problem related to an import declaration.
	 *
	 * @see #acceptProblem(IMarker, String, int)
	 */
	public static final int IMPORT = 3;

	/**
	 * Indicates a compilation problem related to a package declaration.
	 *
	 * @see #acceptProblem(IMarker, String, int)
	 */
	public static final int PACKAGE = 4;

	/**
	 * Indicates an internal problem.
	 *
	 * @see #acceptProblem(IMarker, String, int)
	 */
	public static final int INTERNAL = 5;
/**
 * Sends the given class files to the target and loads them. If the given
 * class name is not <code>null</code>, run the code snippet with this class
 * name. Returns whether the code snippet could be deployed. Note it must
 * return <code>true</code> even if running the code snippet threw an exception.
 * <p>
 * The details of sending and loading the class files are left up to
 * implementations.
 * </p>
 * <p>
 * To run a code snippet, an implementation should create a new instance of
 * the given code snippet class and call (directly or using another means) its
 * <code>RUN_METHOD</code>.
 * </p>
 * <p>
 * Also before the call, the implementation should copy the values of the local
 * variables (if any) into the corresponding fields of the code snippet instance.
 * A field name is formed of <code>LOCAL_VAR_PREFIX</code>
 * preceded the name of the local variable. For example, the field name for
 * local variable <code>"myLocal"</code> is <code>"val$myLocal"</code> (assuming the
 * value of <code>LOCAL_VAR_PREFIX</code> is "val$"). In the
 * same way, the implementation should copy the value of the 'this' object into the
 * field called <code>DELEGATE_THIS</code>.
 * </p>
 * <p>
 * After calling the <code>RUN_METHOD</code>, the values of the local
 * variables may have been modified. The implementation must copy the
 * values of the fields back into the local variables.
 * </p>
 * <p>
 * Finally, the overall value returned by the code snippet can be retrieved
 * from the special field <code>RESULT_VALUE_FIELD</code>
 * on the code snippet instance.
 * The <code>Class</code> that is the runtime type of the returned value can be
 * retrieved from the special field <code>RESULT_TYPE_FIELD</code>.
 * </p>
 *
 * @param classFileBytes the list of class file bytes
 * @param classFileCompoundNames the corresponding list of class file type
 *   compound names (example of a compound name: {"java", "lang", "Object"})
 * @param codeSnippetClassName name of the actual class to instantiate and run,
 *   or <code>null</code> if none
 * @return <code>true</code> if the code snippet was successfully deployed
 */
public boolean acceptClassFiles(byte[][] classFileBytes, String[][] classFileCompoundNames, String codeSnippetClassName);
/**
 * Notifies of an evaluation problem.
 * Problems can arise for source of the following kinds:
 * <ul>
 *   <li>global variable (<code>VARIABLE</code>) - fragment source is name of
 *     variable</li>
 *   <li>code snippet (<code>CODE_SNIPPET</code>) - fragment source is code
 *     snippet</li>
 *   <li>import declaration (<code>IMPORT</code>) - fragment source is
 *     import</li>
 *   <li>package declaration (<code>PACKAGE</code>) - fragment source is
 *     package declaration</li>
 *   <li>other (<code>INTERNAL</code>) - no fragment source is involved, internal error occurred.</li>
 * </ul>
 * @param problemMarker the problem marker (cannot be null)
 * @param fragmentSource the fragment source
 * @param fragmentKind the kind of source fragment; one of:
 *   <code>VARIABLE</code>, <code>CODE_SNIPPET</code>, <code>IMPORT</code>,
 *   <code>PACKAGE</code>, or <code>INTERNAL</code>
 */
public void acceptProblem(IMarker problemMarker, String fragmentSource, int fragmentKind);
}
