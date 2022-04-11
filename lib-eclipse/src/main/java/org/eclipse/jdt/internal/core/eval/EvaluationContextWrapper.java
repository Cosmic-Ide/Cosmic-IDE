/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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

import java.util.Locale;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.eval.ICodeSnippetRequestor;
import org.eclipse.jdt.core.eval.IEvaluationContext;
import org.eclipse.jdt.core.eval.IGlobalVariable;
import org.eclipse.jdt.internal.codeassist.impl.Keywords;
import org.eclipse.jdt.internal.compiler.IProblemFactory;
import org.eclipse.jdt.internal.compiler.env.INameEnvironment;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.core.*;
import org.eclipse.jdt.internal.core.builder.NameEnvironment;
import org.eclipse.jdt.internal.core.builder.ProblemFactory;
import org.eclipse.jdt.internal.eval.EvaluationContext;
import org.eclipse.jdt.internal.eval.GlobalVariable;
import org.eclipse.jdt.internal.eval.IRequestor;
import org.eclipse.jdt.internal.eval.InstallException;

/**
 * A wrapper around the infrastructure evaluation context.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class EvaluationContextWrapper implements IEvaluationContext {
	protected EvaluationContext context;
	protected JavaProject project;
/**
 * Creates a new wrapper around the given infrastructure evaluation context
 * and project.
 */
public EvaluationContextWrapper(EvaluationContext context, JavaProject project) {
	this.context = context;
	this.project = project;
}
/**
 * @see org.eclipse.jdt.core.eval.IEvaluationContext#allVariables()
 */
@Override
public IGlobalVariable[] allVariables() {
	GlobalVariable[] vars = this.context.allVariables();
	int length = vars.length;
	GlobalVariableWrapper[] result = new GlobalVariableWrapper[length];
	for (int i = 0; i < length; i++) {
		result[i] = new GlobalVariableWrapper(vars[i]);
	}
	return result;
}
/**
 * Checks to ensure that there is a previously built state.
 */
protected void checkBuilderState() {

	return;
}
/**
 * @see org.eclipse.jdt.core.eval.IEvaluationContext#codeComplete(String, int, ICompletionRequestor)
 * @deprecated
 */
@Override
public void codeComplete(String codeSnippet, int position, ICompletionRequestor requestor) throws JavaModelException {
	codeComplete(codeSnippet, position, requestor, DefaultWorkingCopyOwner.PRIMARY);
}
/**
 * @see org.eclipse.jdt.core.eval.IEvaluationContext#codeComplete(String, int, ICompletionRequestor, WorkingCopyOwner)
 * @deprecated
 */
@Override
public void codeComplete(String codeSnippet, int position, ICompletionRequestor requestor, WorkingCopyOwner owner) throws JavaModelException {
	if (requestor == null) {
		throw new IllegalArgumentException("Completion requestor cannot be null"); //$NON-NLS-1$
	}
	codeComplete(codeSnippet, position, new org.eclipse.jdt.internal.codeassist.CompletionRequestorWrapper(requestor), owner);
}
/**
 * @see org.eclipse.jdt.core.eval.IEvaluationContext#codeComplete(String, int, CompletionRequestor)
 */
@Override
public void codeComplete(String codeSnippet, int position, CompletionRequestor requestor) throws JavaModelException {
	codeComplete(codeSnippet, position, requestor, DefaultWorkingCopyOwner.PRIMARY);
}
/**
 * @see org.eclipse.jdt.core.eval.IEvaluationContext#codeComplete(String, int, CompletionRequestor, IProgressMonitor)
 */
@Override
public void codeComplete(String codeSnippet, int position, CompletionRequestor requestor, IProgressMonitor monitor) throws JavaModelException {
	codeComplete(codeSnippet, position, requestor, DefaultWorkingCopyOwner.PRIMARY, null);
}
/**
 * @see org.eclipse.jdt.core.eval.IEvaluationContext#codeComplete(String, int, CompletionRequestor, WorkingCopyOwner)
 */
@Override
public void codeComplete(String codeSnippet, int position, CompletionRequestor requestor, WorkingCopyOwner owner) throws JavaModelException {
	codeComplete(codeSnippet, position, requestor, owner, null);
}
/**
 * @see org.eclipse.jdt.core.eval.IEvaluationContext#codeComplete(String, int, CompletionRequestor, WorkingCopyOwner, IProgressMonitor)
 */
@Override
public void codeComplete(
		String codeSnippet,
		int position,
		CompletionRequestor requestor,
		WorkingCopyOwner owner,
		IProgressMonitor monitor) throws JavaModelException {
	SearchableEnvironment environment = this.project.newSearchableNameEnvironment(owner);
	this.context.complete(
		codeSnippet.toCharArray(),
		position,
		environment,
		requestor,
		this.project.getOptions(true),
		this.project,
		owner,
		monitor
	);
}
/**
 * @see org.eclipse.jdt.core.eval.IEvaluationContext#codeSelect(String, int, int)
 */
@Override
public IJavaElement[] codeSelect(String codeSnippet, int offset, int length) throws JavaModelException {
	return codeSelect(codeSnippet, offset, length, DefaultWorkingCopyOwner.PRIMARY);
}
/**
 * @see org.eclipse.jdt.core.eval.IEvaluationContext#codeSelect(String, int, int, WorkingCopyOwner)
 */
@Override
public IJavaElement[] codeSelect(String codeSnippet, int offset, int length, WorkingCopyOwner owner) throws JavaModelException {
	SearchableEnvironment environment = this.project.newSearchableNameEnvironment(owner);
	SelectionRequestor requestor= new SelectionRequestor(environment.nameLookup, null); // null because there is no need to look inside the code snippet itself
	this.context.select(
		codeSnippet.toCharArray(),
		offset,
		offset + length - 1,
		environment,
		requestor,
		this.project.getOptions(true),
		owner
	);
	return requestor.getElements();
}
/**
 * @see org.eclipse.jdt.core.eval.IEvaluationContext#deleteVariable(IGlobalVariable)
 */
@Override
public void deleteVariable(IGlobalVariable variable) {
	if (variable instanceof GlobalVariableWrapper) {
		GlobalVariableWrapper wrapper = (GlobalVariableWrapper)variable;
		this.context.deleteVariable(wrapper.variable);
	} else {
		throw new Error("Unknown implementation of IGlobalVariable"); //$NON-NLS-1$
	}
}
/**
 * @see IEvaluationContext#evaluateCodeSnippet(String, String[], String[], int[], IType, boolean, boolean, ICodeSnippetRequestor, IProgressMonitor)
 */
@Override
public void evaluateCodeSnippet(
	String codeSnippet,
	String[] localVariableTypeNames,
	String[] localVariableNames,
	int[] localVariableModifiers,
	IType declaringType,
	boolean isStatic,
	boolean isConstructorCall,
	ICodeSnippetRequestor requestor,
	IProgressMonitor progressMonitor) throws org.eclipse.jdt.core.JavaModelException {

	checkBuilderState();

	int length = localVariableTypeNames.length;
	char[][] varTypeNames = new char[length][];
	for (int i = 0; i < length; i++){
		varTypeNames[i] = localVariableTypeNames[i].toCharArray();
	}

	length = localVariableNames.length;
	char[][] varNames = new char[length][];
	for (int i = 0; i < length; i++){
		varNames[i] = localVariableNames[i].toCharArray();
	}

	Map options = this.project.getOptions(true);
	// transfer the imports of the IType to the evaluation context
	if (declaringType != null) {
		// retrieves the package statement
		this.context.setPackageName(declaringType.getPackageFragment().getElementName().toCharArray());
		ICompilationUnit compilationUnit = declaringType.getCompilationUnit();
		if (compilationUnit != null) {
			// retrieves the import statement
			IImportDeclaration[] imports = compilationUnit.getImports();
			int importsLength = imports.length;
			if (importsLength != 0) {
				char[][] importsNames = new char[importsLength][];
				for (int i = 0; i < importsLength; i++) {
					IImportDeclaration importDecl = imports[i];
					if(Flags.isStatic(importDecl.getFlags())) {
						importsNames[i] = CharOperation.concatAll(Keywords.STATIC, importDecl.getElementName().toCharArray(), ' ');
					} else {
						importsNames[i] = importDecl.getElementName().toCharArray();
					}
				}
				this.context.setImports(importsNames);
				// turn off import complaints for implicitly added ones
				options.put(CompilerOptions.OPTION_ReportUnusedImport, CompilerOptions.IGNORE);
			}
		} else {
			// try to retrieve imports from the source
			SourceMapper sourceMapper = ((AbstractClassFile) declaringType.getClassFile()).getSourceMapper();
			if (sourceMapper != null) {
				char[][] imports = sourceMapper.getImports((BinaryType) declaringType);
				if (imports != null) {
					this.context.setImports(imports);
					// turn off import complaints for implicitly added ones
					options.put(CompilerOptions.OPTION_ReportUnusedImport, CompilerOptions.IGNORE);
				}
			}
		}
	}
	INameEnvironment environment = null;
	try {
		String fullyQualifiedName = declaringType == null? null : declaringType.getFullyQualifiedName('.');
		this.context.evaluate(
			codeSnippet.toCharArray(),
			varTypeNames,
			varNames,
			localVariableModifiers,
			fullyQualifiedName == null? null : fullyQualifiedName.toCharArray(),
			isStatic,
			isConstructorCall,
			environment = getBuildNameEnvironment(),
			options,
			getInfrastructureEvaluationRequestor(requestor),
			getProblemFactory());
	} catch (InstallException e) {
		handleInstallException(e);
	} finally {
		if (environment != null) environment.cleanup();
	}
}
/**
 * @see IEvaluationContext#evaluateCodeSnippet(String, ICodeSnippetRequestor, IProgressMonitor)
 */
@Override
public void evaluateCodeSnippet(String codeSnippet, ICodeSnippetRequestor requestor, IProgressMonitor progressMonitor) throws JavaModelException {

	checkBuilderState();
	INameEnvironment environment = null;
	try {
		this.context.evaluate(
			codeSnippet.toCharArray(),
			environment = getBuildNameEnvironment(),
			this.project.getOptions(true),
			getInfrastructureEvaluationRequestor(requestor),
			getProblemFactory());
	} catch (InstallException e) {
		handleInstallException(e);
	} finally {
		if (environment != null) environment.cleanup();
	}
}
/**
 * @see IEvaluationContext#evaluateVariable(IGlobalVariable, ICodeSnippetRequestor, IProgressMonitor)
 */
@Override
public void evaluateVariable(IGlobalVariable variable, ICodeSnippetRequestor requestor, IProgressMonitor progressMonitor) throws JavaModelException {

	checkBuilderState();
	INameEnvironment environment = null;
	try {
		this.context.evaluateVariable(
			((GlobalVariableWrapper)variable).variable,
			environment = getBuildNameEnvironment(),
			this.project.getOptions(true),
			getInfrastructureEvaluationRequestor(requestor),
			getProblemFactory());
	} catch (InstallException e) {
		handleInstallException(e);
	} finally {
		if (environment != null) environment.cleanup();
	}
}
/**
 * Returns a name environment for the last built state.
 */
protected INameEnvironment getBuildNameEnvironment() {
	return new NameEnvironment(getProject(), CompilationGroup.TEST);
}
public char[] getVarClassName() {
	return this.context.getVarClassName();
}

/**
 * @see org.eclipse.jdt.core.eval.IEvaluationContext#getImports()
 */
@Override
public String[] getImports() {
	char[][] imports = this.context.getImports();
	int length = imports.length;
	String[] result = new String[length];
	for (int i = 0; i < length; i++) {
		result[i] = new String(imports[i]);
	}
	return result;
}
/**
 * Returns the infrastructure evaluation context.
 */
public EvaluationContext getInfrastructureEvaluationContext() {
	return this.context;
}
/**
 * Returns a new infrastructure evaluation requestor instance.
 */
protected IRequestor getInfrastructureEvaluationRequestor(ICodeSnippetRequestor requestor) {
	return new RequestorWrapper(requestor);
}
/**
 * @see org.eclipse.jdt.core.eval.IEvaluationContext#getPackageName()
 */
@Override
public String getPackageName() {
	return new String(this.context.getPackageName());
}
/**
 * Returns the problem factory to be used during evaluation.
 */
protected IProblemFactory getProblemFactory() {
	return ProblemFactory.getProblemFactory(Locale.getDefault());
}
/**
 * @see org.eclipse.jdt.core.eval.IEvaluationContext#getProject()
 */
@Override
public IJavaProject getProject() {
	return this.project;
}
/**
 * Handles an install exception by throwing a Java Model exception.
 */
protected void handleInstallException(InstallException e) throws JavaModelException {
	throw new JavaModelException(new JavaModelStatus(IJavaModelStatusConstants.EVALUATION_ERROR, e.toString()));
}
/**
 * @see org.eclipse.jdt.core.eval.IEvaluationContext#newVariable(String, String, String)
 */
@Override
public IGlobalVariable newVariable(String typeName, String name, String initializer) {
	GlobalVariable newVar =
		this.context.newVariable(
			typeName.toCharArray(),
			name.toCharArray(),
			(initializer == null) ?
				null :
				initializer.toCharArray());
	return new GlobalVariableWrapper(newVar);
}
/**
 * @see org.eclipse.jdt.core.eval.IEvaluationContext#setImports(String[])
 */
@Override
public void setImports(String[] imports) {
	int length = imports.length;
	char[][] result = new char[length][];
	for (int i = 0; i < length; i++) {
		result[i] = imports[i].toCharArray();
	}
	this.context.setImports(result);
}
/**
 * @see org.eclipse.jdt.core.eval.IEvaluationContext#setPackageName(String)
 */
@Override
public void setPackageName(String packageName) {
	this.context.setPackageName(packageName.toCharArray());
}
/**
 * @see IEvaluationContext#validateImports(ICodeSnippetRequestor)
 */
@Override
public void validateImports(ICodeSnippetRequestor requestor) {

	checkBuilderState();
	INameEnvironment environment = null;
	try {
		this.context.evaluateImports(
			environment = getBuildNameEnvironment(),
			getInfrastructureEvaluationRequestor(requestor),
			getProblemFactory());
	} finally {
		if (environment != null) environment.cleanup();
	}
}
/**
 * @see IEvaluationContext#codeComplete(String, int, ICodeCompletionRequestor)
 * @deprecated - use codeComplete(String, int, ICompletionRequestor) instead
 */
@Override
public void codeComplete(String codeSnippet, int position, final org.eclipse.jdt.core.ICodeCompletionRequestor requestor) throws JavaModelException {

	if (requestor == null){
		codeComplete(codeSnippet, position, (ICompletionRequestor)null);
		return;
	}
	codeComplete(
		codeSnippet,
		position,
		new ICompletionRequestor(){
			@Override
			public void acceptAnonymousType(char[] superTypePackageName,char[] superTypeName,char[][] parameterPackageNames,char[][] parameterTypeNames,char[][] parameterNames,char[] completionName,int modifiers,int completionStart,int completionEnd, int relevance) {
				// implements interface method
			}
			@Override
			public void acceptClass(char[] packageName, char[] className, char[] completionName, int modifiers, int completionStart, int completionEnd, int relevance) {
				requestor.acceptClass(packageName, className, completionName, modifiers, completionStart, completionEnd);
			}
			@Override
			public void acceptError(IProblem error) {
				// was disabled in 1.0
			}

			@Override
			public void acceptField(char[] declaringTypePackageName, char[] declaringTypeName, char[] name, char[] typePackageName, char[] typeName, char[] completionName, int modifiers, int completionStart, int completionEnd, int relevance) {
				requestor.acceptField(declaringTypePackageName, declaringTypeName, name, typePackageName, typeName, completionName, modifiers, completionStart, completionEnd);
			}
			@Override
			public void acceptInterface(char[] packageName,char[] interfaceName,char[] completionName,int modifiers,int completionStart,int completionEnd, int relevance) {
				requestor.acceptInterface(packageName, interfaceName, completionName, modifiers, completionStart, completionEnd);
			}
			@Override
			public void acceptKeyword(char[] keywordName,int completionStart,int completionEnd, int relevance){
				requestor.acceptKeyword(keywordName, completionStart, completionEnd);
			}
			@Override
			public void acceptLabel(char[] labelName,int completionStart,int completionEnd, int relevance){
				requestor.acceptLabel(labelName, completionStart, completionEnd);
			}
			@Override
			public void acceptLocalVariable(char[] name,char[] typePackageName,char[] typeName,int modifiers,int completionStart,int completionEnd, int relevance){
				// ignore
			}
			@Override
			public void acceptMethod(char[] declaringTypePackageName,char[] declaringTypeName,char[] selector,char[][] parameterPackageNames,char[][] parameterTypeNames,char[][] parameterNames,char[] returnTypePackageName,char[] returnTypeName,char[] completionName,int modifiers,int completionStart,int completionEnd, int relevance){
				// skip parameter names
				requestor.acceptMethod(declaringTypePackageName, declaringTypeName, selector, parameterPackageNames, parameterTypeNames, returnTypePackageName, returnTypeName, completionName, modifiers, completionStart, completionEnd);
			}
			@Override
			public void acceptMethodDeclaration(char[] declaringTypePackageName,char[] declaringTypeName,char[] selector,char[][] parameterPackageNames,char[][] parameterTypeNames,char[][] parameterNames,char[] returnTypePackageName,char[] returnTypeName,char[] completionName,int modifiers,int completionStart,int completionEnd, int relevance){
				// ignore
			}
			@Override
			public void acceptModifier(char[] modifierName,int completionStart,int completionEnd, int relevance){
				requestor.acceptModifier(modifierName, completionStart, completionEnd);
			}
			@Override
			public void acceptPackage(char[] packageName,char[] completionName,int completionStart,int completionEnd, int relevance){
				requestor.acceptPackage(packageName, completionName, completionStart, completionEnd);
			}
			@Override
			public void acceptType(char[] packageName,char[] typeName,char[] completionName,int completionStart,int completionEnd, int relevance){
				requestor.acceptType(packageName, typeName, completionName, completionStart, completionEnd);
			}
			@Override
			public void acceptVariableName(char[] typePackageName,char[] typeName,char[] name,char[] completionName,int completionStart,int completionEnd, int relevance){
				// ignore
			}
		});
}
}
