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
package org.eclipse.jdt.internal.eval;

import java.util.Map;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.*;
import org.eclipse.jdt.internal.compiler.ClassFile;
import org.eclipse.jdt.internal.compiler.Compiler;
import org.eclipse.jdt.internal.compiler.DefaultErrorHandlingPolicies;
import org.eclipse.jdt.internal.compiler.ICompilerRequestor;
import org.eclipse.jdt.internal.compiler.IProblemFactory;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFormatException;
import org.eclipse.jdt.internal.compiler.env.IBinaryType;
import org.eclipse.jdt.internal.compiler.env.INameEnvironment;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;

/**
 * A code snippet evaluator compiles and returns class file for a code snippet.
 * Or it reports problems against the code snippet.
 */
public class CodeSnippetEvaluator extends Evaluator implements EvaluationConstants {
	/**
	 * Whether the code snippet support classes should be found in the provided name environment
	 * or on disk.
	 */
	static final boolean DEVELOPMENT_MODE = false;

	/**
	 * The code snippet to evaluate.
	 */
	char[] codeSnippet;

	/**
	 * The code snippet to generated compilation unit mapper
	 */
	CodeSnippetToCuMapper mapper;
/**
 * Creates a new code snippet evaluator.
 */
CodeSnippetEvaluator(char[] codeSnippet, EvaluationContext context, INameEnvironment environment, Map<String, String> options, IRequestor requestor, IProblemFactory problemFactory) {
	super(context, environment, options, requestor, problemFactory);
	this.codeSnippet = codeSnippet;
}
/**
 * @see org.eclipse.jdt.internal.eval.Evaluator
 */
@Override
protected void addEvaluationResultForCompilationProblem(Map<char[], EvaluationResult> resultsByIDs, CategorizedProblem problem, char[] cuSource) {
	CodeSnippetToCuMapper sourceMapper = getMapper();
	int pbLineNumber = problem.getSourceLineNumber();
	int evaluationType = sourceMapper.getEvaluationType(pbLineNumber);

	char[] evaluationID = null;
	switch(evaluationType) {
		case EvaluationResult.T_PACKAGE:
			evaluationID = this.context.packageName;

			// shift line number, source start and source end
			problem.setSourceLineNumber(1);
			problem.setSourceStart(0);
			problem.setSourceEnd(evaluationID.length - 1);
			break;

		case EvaluationResult.T_IMPORT:
			evaluationID = sourceMapper.getImport(pbLineNumber);

			// shift line number, source start and source end
			problem.setSourceLineNumber(1);
			problem.setSourceStart(0);
			problem.setSourceEnd(evaluationID.length - 1);
			break;

		case EvaluationResult.T_CODE_SNIPPET:
			evaluationID = this.codeSnippet;

			// shift line number, source start and source end
			problem.setSourceLineNumber(pbLineNumber - this.mapper.lineNumberOffset);
			problem.setSourceStart(problem.getSourceStart() - this.mapper.startPosOffset);
			problem.setSourceEnd(problem.getSourceEnd() - this.mapper.startPosOffset);
			break;

		case EvaluationResult.T_INTERNAL:
			evaluationID = cuSource;
			break;
	}

	EvaluationResult result = resultsByIDs.get(evaluationID);
	if (result == null) {
		resultsByIDs.put(evaluationID, new EvaluationResult(evaluationID, evaluationType, new CategorizedProblem[] {problem}));
	} else {
		result.addProblem(problem);
	}
}
/**
 * @see org.eclipse.jdt.internal.eval.Evaluator
 */
@Override
protected char[] getClassName() {
	return CharOperation.concat(CODE_SNIPPET_CLASS_NAME_PREFIX, Integer.toString(EvaluationContext.CODE_SNIPPET_COUNTER + 1).toCharArray());
}
/**
 * @see Evaluator
 */
@Override
Compiler getCompiler(ICompilerRequestor compilerRequestor) {
	Compiler compiler = null;
	if (!DEVELOPMENT_MODE) {
		// If we are not developping the code snippet support classes,
		// use a regular compiler and feed its lookup environment with
		// the code snippet support classes

		CompilerOptions compilerOptions = new CompilerOptions(this.options);
		compilerOptions.performMethodsFullRecovery = true;
		compilerOptions.performStatementsRecovery = true;
		compiler =
			new CodeSnippetCompiler(
				this.environment,
				DefaultErrorHandlingPolicies.exitAfterAllProblems(),
				compilerOptions,
				compilerRequestor,
				this.problemFactory,
				this.context,
				getMapper().startPosOffset,
				getMapper().startPosOffset + this.codeSnippet.length - 1);
		((CodeSnippetParser) compiler.parser).lineSeparatorLength = this.context.lineSeparator.length();
		// Initialize the compiler's lookup environment with the already compiled super classes
		IBinaryType binary = this.context.getRootCodeSnippetBinary();
		if (binary != null) {
			compiler.lookupEnvironment.cacheBinaryType(binary, null /*no access restriction*/);
		}
		VariablesInfo installedVars = this.context.installedVars;
		if (installedVars != null) {
			ClassFile[] globalClassFiles = installedVars.classFiles;
			for (int i = 0; i < globalClassFiles.length; i++) {
				ClassFileReader binaryType = null;
				try {
					binaryType = new ClassFileReader(globalClassFiles[i].getBytes(), null);
				} catch (ClassFormatException e) {
					e.printStackTrace(); // Should never happen since we compiled this type
				}
				compiler.lookupEnvironment.cacheBinaryType(binaryType, null /*no access restriction*/);
			}
		}
	} else {
		// If we are developping the code snippet support classes,
		// use a wrapped environment so that if the code snippet classes are not found
		// then a default implementation is provided.

		CompilerOptions compilerOptions = new CompilerOptions(this.options);
		compilerOptions.performMethodsFullRecovery = true;
		compilerOptions.performStatementsRecovery = true;
		compiler = new Compiler(
			getWrapperEnvironment(),
			DefaultErrorHandlingPolicies.exitAfterAllProblems(),
			compilerOptions,
			compilerRequestor,
			this.problemFactory);
	}
	return compiler;
}
private CodeSnippetToCuMapper getMapper() {
	if (this.mapper == null) {
		char[] varClassName = null;
		VariablesInfo installedVars = this.context.installedVars;
		if (installedVars != null) {
			char[] superPackageName = installedVars.packageName;
			if (superPackageName != null && superPackageName.length != 0) {
				varClassName = CharOperation.concat(superPackageName, installedVars.className, '.');
			} else {
				varClassName = installedVars.className;
			}

		}
		this.mapper = new CodeSnippetToCuMapper(
			this.codeSnippet,
			this.context.packageName,
			this.context.imports,
			getClassName(),
			varClassName,
			this.context.localVariableNames,
			this.context.localVariableTypeNames,
			this.context.localVariableModifiers,
			this.context.declaringTypeName,
			this.context.lineSeparator,
			CompilerOptions.versionToJdkLevel(this.options.get(JavaCore.COMPILER_COMPLIANCE))
		);

	}
	return this.mapper;
}
/**
 * @see org.eclipse.jdt.internal.eval.Evaluator
 */
@Override
protected char[] getSource() {
	return getMapper().cuSource;
}
/**
 * Returns an environment that wraps the client's name environment.
 * This wrapper always considers the wrapped environment then if the name is
 * not found, it search in the code snippet support. This includes the superclass
 * org.eclipse.jdt.internal.eval.target.CodeSnippet as well as the global variable classes.
 */
private INameEnvironment getWrapperEnvironment() {
	return new CodeSnippetEnvironment(this.environment, this.context);
}
}
