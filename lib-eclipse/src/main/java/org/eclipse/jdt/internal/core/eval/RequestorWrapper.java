/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
package org.eclipse.jdt.internal.core.eval;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.core.eval.ICodeSnippetRequestor;
import org.eclipse.jdt.internal.compiler.ClassFile;
import org.eclipse.jdt.internal.core.builder.JavaBuilder;
import org.eclipse.jdt.internal.eval.IRequestor;

public class RequestorWrapper implements IRequestor {

	ICodeSnippetRequestor requestor;

public RequestorWrapper(ICodeSnippetRequestor requestor) {
	this.requestor = requestor;
}
/**
 * @see ICodeSnippetRequestor
 */
@Override
public boolean acceptClassFiles(ClassFile[] classFiles, char[] codeSnippetClassName) {
	int length = classFiles.length;
	byte[][] classFileBytes = new byte[length][];
	String[][] compoundNames = new String[length][];
	for (int i = 0; i < length; i++) {
		ClassFile classFile = classFiles[i];
		classFileBytes[i] = classFile.getBytes();
		char[][] classFileCompundName = classFile.getCompoundName();
		int length2 = classFileCompundName.length;
		String[] compoundName = new String[length2];
		for (int j = 0; j < length2; j++){
			compoundName[j] = new String(classFileCompundName[j]);
		}
		compoundNames[i] = compoundName;
	}
	return this.requestor.acceptClassFiles(classFileBytes, compoundNames, codeSnippetClassName == null ? null : new String(codeSnippetClassName));
}
/**
 * @see ICodeSnippetRequestor
 */
@Override
public void acceptProblem(CategorizedProblem problem, char[] fragmentSource, int fragmentKind) {
	try {
		Map<String, Object> attributes = new HashMap<>();
		attributes.put(IJavaModelMarker.ID, problem.getID());
		attributes.put(IMarker.CHAR_START, problem.getSourceStart());
		attributes.put(IMarker.CHAR_END, problem.getSourceEnd() + 1);
		attributes.put(IMarker.LINE_NUMBER, problem.getSourceLineNumber());
		attributes.put(IMarker.MESSAGE, problem.getMessage());
		attributes.put(IMarker.SEVERITY, (problem.isError() ? IMarker.SEVERITY_ERROR : problem.isWarning() ? IMarker.SEVERITY_WARNING : IMarker.SEVERITY_INFO));
		attributes.put(IMarker.SOURCE_ID, JavaBuilder.SOURCE_ID);

		IMarker marker = ResourcesPlugin.getWorkspace().getRoot().createMarker(IJavaModelMarker.TRANSIENT_PROBLEM, attributes);
		this.requestor.acceptProblem(marker, new String(fragmentSource), fragmentKind);
	} catch (CoreException e) {
		e.printStackTrace();
	}
}
}
