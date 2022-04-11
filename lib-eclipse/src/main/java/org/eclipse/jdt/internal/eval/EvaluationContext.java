/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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

import java.util.Locale;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.CompletionRequestor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.WorkingCopyOwner;
import org.eclipse.jdt.core.compiler.*;
import org.eclipse.jdt.internal.codeassist.CompletionEngine;
import org.eclipse.jdt.internal.codeassist.ISelectionRequestor;
import org.eclipse.jdt.internal.codeassist.SelectionEngine;
import org.eclipse.jdt.internal.compiler.ClassFile;
import org.eclipse.jdt.internal.compiler.IProblemFactory;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFormatException;
import org.eclipse.jdt.internal.compiler.env.IBinaryType;
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.jdt.internal.compiler.env.INameEnvironment;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblemFactory;
import org.eclipse.jdt.internal.compiler.problem.ProblemSeverities;
import org.eclipse.jdt.internal.compiler.util.SuffixConstants;
import org.eclipse.jdt.internal.core.SearchableEnvironment;
import org.eclipse.jdt.internal.core.util.Util;

/**
 * @see org.eclipse.jdt.core.eval.IEvaluationContext
 */
public class EvaluationContext implements EvaluationConstants, SuffixConstants {
	/**
	 * Global counters so that several evaluation context can deploy on the same runtime.
	 */
	static int VAR_CLASS_COUNTER = 0;
	static int CODE_SNIPPET_COUNTER = 0;

	GlobalVariable[] variables;
	int variableCount;
	char[][] imports;
	char[] packageName;
	boolean varsChanged;
	VariablesInfo installedVars;
	IBinaryType codeSnippetBinary;
	String lineSeparator;

	/* do names implicitly refer to a given type */
	char[] declaringTypeName;
	int[] localVariableModifiers;
	char[][] localVariableTypeNames;
	char[][] localVariableNames;

	/* can 'this' be used in this context */
	boolean isStatic;
	boolean isConstructorCall;
/**
 * Creates a new evaluation context.
 */
public EvaluationContext() {
	this.variables = new GlobalVariable[5];
	this.variableCount = 0;
	this.imports = CharOperation.NO_CHAR_CHAR;
	this.packageName = CharOperation.NO_CHAR;
	this.varsChanged = true;
	this.isStatic = true;
	this.isConstructorCall = false;
	this.lineSeparator = org.eclipse.jdt.internal.compiler.util.Util.LINE_SEPARATOR; // default value
}
/**
 * Returns the global variables of this evaluation context in the order they were created in.
 */
public GlobalVariable[] allVariables() {
	GlobalVariable[] result = new GlobalVariable[this.variableCount];
	System.arraycopy(this.variables, 0, result, 0, this.variableCount);
	return result;
}
/**
 * Computes a completion at the specified position of the given code snippet.
 * (Note that this evaluation context's VM doesn't need to be running.)
 *
 *  @param environment
 *      used to resolve type/package references and search for types/packages
 *      based on partial names.
 *
 *  @param requestor
 *      since the engine might produce answers of various forms, the engine
 *      is associated with a requestor able to accept all possible completions.
 *
 *  @param options
 *		set of options used to configure the code assist engine.
 *
 *  @param owner
 *  	the owner of working copies that take precedence over their original compilation units
 *
 *  @param monitor
 *  	the progress monitor used to report progress
 */
public void complete(
		char[] codeSnippet,
		int completionPosition,
		SearchableEnvironment environment,
		CompletionRequestor requestor,
		Map<String, String> options,
		final IJavaProject project,
		WorkingCopyOwner owner,
		IProgressMonitor monitor) {
	try {
		IRequestor variableRequestor = new IRequestor() {
			@Override
			public boolean acceptClassFiles(ClassFile[] classFiles, char[] codeSnippetClassName) {
				// Do nothing
				return true;
			}
			@Override
			public void acceptProblem(CategorizedProblem problem, char[] fragmentSource, int fragmentKind) {
				// Do nothing
			}
		};
		evaluateVariables(environment, options, variableRequestor, new DefaultProblemFactory(Locale.getDefault()));
	} catch (InstallException e) {
		// Do nothing
	}
	final char[] className = "CodeSnippetCompletion".toCharArray(); //$NON-NLS-1$
	final long complianceVersion = CompilerOptions.versionToJdkLevel(options.get(JavaCore.COMPILER_COMPLIANCE));
	final CodeSnippetToCuMapper mapper = new CodeSnippetToCuMapper(
		codeSnippet,
		this.packageName,
		this.imports,
		className,
		this.installedVars == null ? null : this.installedVars.className,
		this.localVariableNames,
		this.localVariableTypeNames,
		this.localVariableModifiers,
		this.declaringTypeName,
		this.lineSeparator,
		complianceVersion
	);
	ICompilationUnit sourceUnit = new ICompilationUnit() {
		@Override
		public char[] getFileName() {
			return CharOperation.concat(className, Util.defaultJavaExtension().toCharArray());
		}
		@Override
		public char[] getContents() {
			return mapper.getCUSource(EvaluationContext.this.lineSeparator);
		}
		@Override
		public char[] getMainTypeName() {
			return className;
		}
		@Override
		public char[][] getPackageName() {
			return null;
		}
		@Override
		public boolean ignoreOptionalProblems() {
			return false;
		}
		@Override
		public char[] getModuleName() {
			// TODO Java 9 Auto-generated method stub
			return null;
		}
	};

	CompletionEngine engine = new CompletionEngine(environment, mapper.getCompletionRequestor(requestor), options, project, owner, monitor);

	if (this.installedVars != null) {
		IBinaryType binaryType = getRootCodeSnippetBinary();
		if (binaryType != null) {
			engine.lookupEnvironment.cacheBinaryType(binaryType, null /*no access restriction*/);
		}

		ClassFile[] classFiles = this.installedVars.classFiles;
		for (int i = 0; i < classFiles.length; i++) {
			ClassFile classFile = classFiles[i];
			IBinaryType binary = null;
			try {
				binary = new ClassFileReader(classFile.getBytes(), null);
			} catch (ClassFormatException e) {
				e.printStackTrace(); // Should never happen since we compiled this type
			}
			engine.lookupEnvironment.cacheBinaryType(binary, null /*no access restriction*/);
		}
	}

	engine.complete(sourceUnit, mapper.startPosOffset + completionPosition, mapper.startPosOffset, null/*extended context isn't computed*/);
}
/**
 * Deletes the given variable from this evaluation context. This will take effect in the target VM only
 * the next time global variables are installed.
 */
public void deleteVariable(GlobalVariable variable) {
	GlobalVariable[] vars = this.variables;
	int index = -1;
	for (int i = 0; i < this.variableCount; i++) {
		if (vars[i].equals(variable)) {
			index = i;
			break;
		}
	}
	if (index == -1) {
		return;
	}
	int elementCount = this.variableCount--;
	int j = elementCount - index - 1;
	if (j > 0) {
	    System.arraycopy(vars, index + 1, vars, index, j);
	}
	vars[elementCount - 1] = null;
	this.varsChanged = true;
}
private void deployCodeSnippetClassIfNeeded(IRequestor requestor) throws InstallException {
	if (this.codeSnippetBinary == null) {
		// Deploy CodeSnippet class (only once)
		if (!requestor.acceptClassFiles(
			new ClassFile[] {
				new ClassFile() {
					@Override
					public byte[] getBytes() {
						return getCodeSnippetBytes();
					}
					@Override
					public char[][] getCompoundName() {
						return EvaluationConstants.ROOT_COMPOUND_NAME;
					}
				}
			},
			null))
				throw new InstallException();
	}
}
/**
 * @see org.eclipse.jdt.core.eval.IEvaluationContext
 * @exception org.eclipse.jdt.internal.eval.InstallException if the code snippet class files could not be deployed.
 */
public void evaluate(
	char[] codeSnippet,
	char[][] contextLocalVariableTypeNames,
	char[][] contextLocalVariableNames,
	int[] contextLocalVariableModifiers,
	char[] contextDeclaringTypeName,
	boolean contextIsStatic,
	boolean contextIsConstructorCall,
	INameEnvironment environment,
	Map<String, String> options,
	final IRequestor requestor,
	IProblemFactory problemFactory) throws InstallException {

	// Initialialize context
	this.localVariableTypeNames = contextLocalVariableTypeNames;
	this.localVariableNames = contextLocalVariableNames;
	this.localVariableModifiers = contextLocalVariableModifiers;
	this.declaringTypeName = contextDeclaringTypeName;
	this.isStatic = contextIsStatic;
	this.isConstructorCall = contextIsConstructorCall;

	deployCodeSnippetClassIfNeeded(requestor);

	try {
		// Install new variables if needed
		class ForwardingRequestor implements IRequestor {
			boolean hasErrors = false;
			@Override
			public boolean acceptClassFiles(ClassFile[] classFiles, char[] codeSnippetClassName) {
				return requestor.acceptClassFiles(classFiles, codeSnippetClassName);
			}
			@Override
			public void acceptProblem(CategorizedProblem problem, char[] fragmentSource, int fragmentKind) {
				requestor.acceptProblem(problem, fragmentSource, fragmentKind);
				if (problem.isError()) {
					this.hasErrors = true;
				}
			}
		}
		ForwardingRequestor forwardingRequestor = new ForwardingRequestor();
		if (this.varsChanged) {
			evaluateVariables(environment, options, forwardingRequestor, problemFactory);
		}

		// Compile code snippet if there was no errors while evaluating the variables
		if (!forwardingRequestor.hasErrors) {
			Evaluator evaluator =
				new CodeSnippetEvaluator(
					codeSnippet,
					this,
					environment,
					options,
					requestor,
					problemFactory);
			ClassFile[] classes = evaluator.getClasses();
			// Send code snippet on target
			if (classes != null && classes.length > 0) {
				char[] simpleClassName = evaluator.getClassName();
				char[] pkgName = getPackageName();
				char[] qualifiedClassName =
					pkgName.length == 0 ?
						simpleClassName :
						CharOperation.concat(pkgName, simpleClassName, '.');
				CODE_SNIPPET_COUNTER++;
				if (!requestor.acceptClassFiles(classes, qualifiedClassName))
					throw new InstallException();
			}
		}
	} finally {
		// Reinitialize context to default values
		this.localVariableTypeNames = null;
		this.localVariableNames = null;
		this.localVariableModifiers = null;
		this.declaringTypeName = null;
		this.isStatic = true;
		this.isConstructorCall = false;
	}
}
/**
 * @see org.eclipse.jdt.core.eval.IEvaluationContext
 * @exception org.eclipse.jdt.internal.eval.InstallException if the code snippet class files could not be deployed.
 */
public void evaluate(char[] codeSnippet, INameEnvironment environment, Map<String, String> options, final IRequestor requestor, IProblemFactory problemFactory) throws InstallException {
	this.evaluate(
		codeSnippet,
		null,
		null,
		null,
		null,
		true,
		false,
		environment,
		options,
		requestor,
		problemFactory);
}
/**
 * @see org.eclipse.jdt.core.eval.IEvaluationContext
 */
public void evaluateImports(INameEnvironment environment, IRequestor requestor, IProblemFactory problemFactory) {
	for (int i = 0; i < this.imports.length; i++) {
		CategorizedProblem[] problems = new CategorizedProblem[] {null};
		char[] importDeclaration = this.imports[i];
		char[][] splitDeclaration = CharOperation.splitOn('.', importDeclaration);
		int splitLength = splitDeclaration.length;
		if (splitLength > 0) {
			char[] pkgName = splitDeclaration[splitLength - 1];
			if (pkgName.length == 1 && pkgName[0] == '*') {
				char[][] parentName;
				switch (splitLength) {
					case 1:
						parentName = null;
						break;
					case 2:
						parentName = null;
						pkgName = splitDeclaration[splitLength - 2];
						break;
					default:
						parentName = CharOperation.subarray(splitDeclaration, 0, splitLength - 2);
						pkgName = splitDeclaration[splitLength - 2];
				}
				if (!environment.isPackage(parentName, pkgName)) {
					String[] arguments = new String[] {new String(importDeclaration)};
					problems[0] = problemFactory.createProblem(importDeclaration, IProblem.ImportNotFound, arguments, arguments, ProblemSeverities.Warning, 0, importDeclaration.length - 1, i, 0);
				}
			} else {
				if (environment.findType(splitDeclaration) == null) {
					String[] arguments = new String[] {new String(importDeclaration)};
					problems[0] = problemFactory.createProblem(importDeclaration, IProblem.ImportNotFound, arguments, arguments, ProblemSeverities.Warning, 0, importDeclaration.length - 1, i, 0);
				}
			}
		} else {
			String[] arguments = new String[] {new String(importDeclaration)};
			problems[0] = problemFactory.createProblem(importDeclaration, IProblem.ImportNotFound, arguments, arguments, ProblemSeverities.Warning, 0, importDeclaration.length - 1, i, 0);
		}
		if (problems[0] != null) {
			requestor.acceptProblem(problems[0], importDeclaration, EvaluationResult.T_IMPORT);
		}
	}
}
/**
 * @see org.eclipse.jdt.core.eval.IEvaluationContext
 * @exception org.eclipse.jdt.internal.eval.InstallException if the code snippet class files could not be deployed.
 * @exception java.lang.IllegalArgumentException if the global has not been installed yet.
 */
public void evaluateVariable(GlobalVariable variable, INameEnvironment environment, Map<String, String> options, IRequestor requestor, IProblemFactory problemFactory) throws InstallException {
	this.evaluate(variable.getName(), environment, options, requestor, problemFactory);
}
/**
 * @see org.eclipse.jdt.core.eval.IEvaluationContext
 * @exception org.eclipse.jdt.internal.eval.InstallException if the code snippet class files could not be deployed.
 */
public void evaluateVariables(INameEnvironment environment, Map<String, String> options, IRequestor requestor, IProblemFactory problemFactory) throws InstallException {
	deployCodeSnippetClassIfNeeded(requestor);
	VariablesEvaluator evaluator = new VariablesEvaluator(this, environment, options, requestor, problemFactory);
	ClassFile[] classes = evaluator.getClasses();
	if (classes != null) {
		if (classes.length > 0) {
			// Sort classes so that enclosing types are cached before nested types
			// otherwise an AbortCompilation is thrown in 1.5 mode since the enclosing type
			// is needed to resolve a nested type
			Util.sort(classes, new Util.Comparer() {
				@Override
				public int compare(Object a, Object b) {
					if (a == b) return 0;
					ClassFile enclosing = ((ClassFile) a).enclosingClassFile;
					while (enclosing != null) {
						if (enclosing == b)
							return 1;
						enclosing = enclosing.enclosingClassFile;
					}
					return -1;
				}
			});

			// Send classes
			if (!requestor.acceptClassFiles(classes, null)) {
				throw new InstallException();
			}

			// Remember that the variables have been installed
			int count = this.variableCount;
			GlobalVariable[] variablesCopy = new GlobalVariable[count];
			System.arraycopy(this.variables, 0, variablesCopy, 0, count);
			this.installedVars = new VariablesInfo(evaluator.getPackageName(), evaluator.getClassName(), classes, variablesCopy, count);
			VAR_CLASS_COUNTER++;
		}
		this.varsChanged = false;
	}
}
/**
 * Returns the bytes of the CodeSnippet class.
 * Generated using the following code snippet:
[java.io.BufferedWriter writer = new java.io.BufferedWriter(new java.io.FileWriter("d:/temp/CodeSnippet.java"));
writer.write(org.eclipse.jdt.internal.eval.EvaluationContext.getCodeSnippetSource());
writer.close();
org.eclipse.jdt.internal.compiler.batch.Main.compile(
	"d:/temp/CodeSnippet.java -d d:/temp -classpath d:/jdk1.2.2/jre/lib/rt.jar -verbose");
java.io.FileInputStream reader =  new java.io.FileInputStream("d:/temp/org/eclipse/jdt/internal/eval/target/CodeSnippet.class");
byte[] bytes = org.eclipse.jdt.internal.core.Util.readContentsAsBytes(reader);
reader.close();
StringBuffer buffer = new StringBuffer();
buffer.append("private byte[] getCodeSnippetBytes() {\n");
buffer.append("	return new byte[] {\n");
buffer.append("		");
for (int i = 0; i < bytes.length; i++) {
	buffer.append(bytes[i]);
	if (i == bytes.length - 1) {
		buffer.append("\n");
	} else {
		buffer.append(", ");
	}
}
buffer.append("	};\n");
buffer.append("}");
buffer.toString()
]
 */
byte[] getCodeSnippetBytes() {
	return new byte[] {
		-54, -2, -70, -66, 0, 3, 0, 45, 0, 35, 1, 0, 48, 111, 114, 103, 47, 101, 99, 108, 105, 112, 115, 101, 47, 106, 100, 116, 47, 105, 110, 116, 101, 114, 110, 97, 108, 47, 101, 118, 97, 108, 47, 116, 97, 114, 103, 101, 116, 47, 67, 111, 100, 101, 83, 110, 105, 112, 112, 101, 116, 7, 0, 1, 1, 0, 16, 106, 97, 118, 97, 47, 108, 97, 110, 103, 47, 79, 98, 106, 101, 99, 116, 7, 0, 3, 1, 0, 10, 114, 101, 115, 117, 108, 116, 84, 121, 112, 101, 1, 0, 17, 76, 106, 97, 118, 97, 47, 108, 97, 110, 103, 47, 67, 108, 97, 115, 115, 59, 1, 0, 11, 114, 101, 115, 117, 108, 116, 86, 97, 108, 117, 101, 1, 0, 18, 76, 106, 97, 118, 97, 47, 108, 97, 110, 103, 47, 79, 98, 106, 101, 99, 116, 59, 1, 0, 7, 99, 108, 97, 115, 115, 36, 48, 1, 0, 9, 83, 121, 110, 116, 104, 101, 116, 105, 99, 1, 0, 6, 60, 105, 110, 105, 116, 62, 1, 0, 3, 40, 41, 86, 1, 0, 4, 67, 111, 100, 101, 12, 0, 11, 0, 12, 10, 0, 4, 0, 14, 1, 0, 14, 106, 97, 118, 97, 47, 108, 97, 110, 103, 47, 86, 111, 105, 100, 7, 0, 16, 1, 0, 4, 84, 89, 80, 69, 12, 0, 18, 0, 6, 9, 0, 17, 0, 19, 12, 0, 5, 0, 6, 9, 0, 2, 0, 21, 12, 0, 7, 0, 8, 9, 0, 2, 0, 23, 1, 0, 15, 76, 105, 110, 101, 78, 117, 109, 98, 101, 114, 84, 97, 98, 108, 101, 1, 0, 13, 103, 101, 116, 82, 101, 115, 117, 108, 116, 84, 121, 112, 101, 1, 0, 19, 40, 41, 76, 106, 97, 118, 97, 47, 108, 97, 110, 103, 47, 67, 108, 97, 115, 115, 59, 1, 0, 14, 103, 101, 116, 82, 101, 115, 117, 108, 116, 86, 97, 108, 117, 101, 1, 0, 20, 40, 41, 76, 106, 97, 118, 97, 47, 108, 97, 110, 103, 47, 79, 98, 106, 101, 99, 116, 59, 1, 0, 3, 114, 117, 110, 1, 0, 9, 115, 101, 116, 82, 101, 115, 117, 108, 116, 1, 0, 38, 40, 76, 106, 97, 118, 97, 47, 108, 97, 110, 103, 47, 79, 98, 106, 101, 99, 116, 59, 76, 106, 97, 118, 97, 47, 108, 97, 110, 103, 47, 67, 108, 97, 115, 115, 59, 41, 86, 1, 0, 10, 83, 111, 117, 114, 99, 101, 70, 105, 108, 101, 1, 0, 16, 67, 111, 100, 101, 83, 110, 105, 112, 112, 101, 116, 46, 106, 97, 118, 97, 0, 33, 0, 2, 0, 4, 0, 0, 0, 3, 0, 2, 0, 5, 0, 6, 0, 0, 0, 2, 0, 7, 0, 8, 0, 0, 0, 8, 0, 9, 0, 6, 0, 1, 0, 10, 0, 0, 0, 0, 0, 5, 0, 1, 0, 11, 0, 12, 0, 1, 0, 13, 0, 0, 0, 53, 0, 2, 0, 1, 0, 0, 0, 17, 42, -73, 0, 15, 42, -78, 0, 20, -75, 0, 22, 42, 1, -75, 0, 24, -79, 0, 0, 0, 1, 0, 25, 0, 0, 0, 18, 0, 4, 0, 0, 0, 17, 0, 4, 0, 18, 0, 11, 0, 19, 0, 16, 0, 17, 0, 1, 0, 26, 0, 27, 0, 1, 0, 13, 0, 0, 0, 29, 0, 1, 0, 1, 0, 0, 0, 5, 42, -76, 0, 22, -80, 0, 0, 0, 1, 0, 25, 0, 0, 0, 6, 0, 1, 0, 0, 0, 24, 0, 1, 0, 28, 0, 29, 0, 1, 0, 13, 0, 0, 0, 29, 0, 1, 0, 1, 0, 0, 0, 5, 42, -76, 0, 24, -80, 0, 0, 0, 1, 0, 25, 0, 0, 0, 6, 0, 1, 0, 0, 0, 30, 0, 1, 0, 30, 0, 12, 0, 1, 0, 13, 0, 0, 0, 25, 0, 0, 0, 1, 0, 0, 0, 1, -79, 0, 0, 0, 1, 0, 25, 0, 0, 0, 6, 0, 1, 0, 0, 0, 36, 0, 1, 0, 31, 0, 32, 0, 1, 0, 13, 0, 0, 0, 43, 0, 2, 0, 3, 0, 0, 0, 11, 42, 43, -75, 0, 24, 42, 44, -75, 0, 22, -79, 0, 0, 0, 1, 0, 25, 0, 0, 0, 14, 0, 3, 0, 0, 0, 42, 0, 5, 0, 43, 0, 10, 0, 41, 0, 1, 0, 33, 0, 0, 0, 2, 0, 34
	};
}
/**
 * Returns the source of the CodeSnippet class.
 * This is used to generate the binary of the CodeSnippetClass
 */
public static String getCodeSnippetSource() {
	return
		"package org.eclipse.jdt.internal.eval.target;\n" + //$NON-NLS-1$
		"\n" + //$NON-NLS-1$
		"/*\n" + //$NON-NLS-1$
		" * (c) Copyright IBM Corp. 2000, 2001.\n" + //$NON-NLS-1$
		" * All Rights Reserved.\n" + //$NON-NLS-1$
		" */\n" + //$NON-NLS-1$
		"/**\n" + //$NON-NLS-1$
		" * The root of all code snippet classes. Code snippet classes\n" + //$NON-NLS-1$
		" * are supposed to overide the run() method.\n" + //$NON-NLS-1$
		" * <p>\n" + //$NON-NLS-1$
		" * IMPORTANT NOTE:\n" + //$NON-NLS-1$
		" * All methods in this class must be public since this class is going to be loaded by the\n" + //$NON-NLS-1$
		" * bootstrap class loader, and the other code snippet support classes might be loaded by \n" + //$NON-NLS-1$
		" * another class loader (so their runtime packages are going to be different).\n" + //$NON-NLS-1$
		" */\n" + //$NON-NLS-1$
		"public class CodeSnippet {\n" + //$NON-NLS-1$
		"	private Class resultType = void.class;\n" + //$NON-NLS-1$
		"	private Object resultValue = null;\n" + //$NON-NLS-1$
		"/**\n" + //$NON-NLS-1$
		" * Returns the result type of the code snippet evaluation.\n" + //$NON-NLS-1$
		" */\n" + //$NON-NLS-1$
		"public Class getResultType() {\n" + //$NON-NLS-1$
		"	return this.resultType;\n" + //$NON-NLS-1$
		"}\n" + //$NON-NLS-1$
		"/**\n" + //$NON-NLS-1$
		" * Returns the result value of the code snippet evaluation.\n" + //$NON-NLS-1$
		" */\n" + //$NON-NLS-1$
		"public Object getResultValue() {\n" + //$NON-NLS-1$
		"	return this.resultValue;\n" + //$NON-NLS-1$
		"}\n" + //$NON-NLS-1$
		"/**\n" + //$NON-NLS-1$
		" * The code snippet. Subclasses must override this method with a transformed code snippet\n" + //$NON-NLS-1$
		" * that stores the result using setResult(Class, Object).\n" + //$NON-NLS-1$
		" */\n" + //$NON-NLS-1$
		"public void run() {\n" + //$NON-NLS-1$
		"}\n" + //$NON-NLS-1$
		"/**\n" + //$NON-NLS-1$
		" * Stores the result type and value of the code snippet evaluation.\n" + //$NON-NLS-1$
		" */\n" + //$NON-NLS-1$
		"public void setResult(Object someResultValue, Class someResultType) {\n" + //$NON-NLS-1$
		"	this.resultValue = someResultValue;\n" + //$NON-NLS-1$
		"	this.resultType = someResultType;\n" + //$NON-NLS-1$
		"}\n" + //$NON-NLS-1$
		"}\n"; //$NON-NLS-1$
}
/**
 * Returns the imports of this evaluation context. An import is the name of a package
 * or the fully qualified name of a type as defined in the import statement of
 * a compilation unit.
 */
public char[][] getImports() {
	return this.imports;
}
/**
 * Returns the dot-separated name of the package code snippets are run into.
 * Returns an empty array for the default package. This is the default if
 * the package name has never been set.
 */
public char[] getPackageName() {
	return this.packageName;
}
/**
 * Return the binary for the root code snippet class (i.e. org.eclipse.jdt.internal.eval.target.CodeSnippet).
 */
IBinaryType getRootCodeSnippetBinary() {
	if (this.codeSnippetBinary == null) {
		this.codeSnippetBinary = new CodeSnippetSkeleton();
	}
	return this.codeSnippetBinary;
}
public char[] getVarClassName() {
	if (this.installedVars == null) return CharOperation.NO_CHAR;
	return CharOperation.concat(this.installedVars.packageName, this.installedVars.className, '.');
}
/**
 * Creates a new global variable with the given name, type and initializer.
 * If the variable is not initialized, the initializer can be null.
 * Note that this doesn't install it to this evaluation context's VM.
 *
 * @see GlobalVariable
 */
public GlobalVariable newVariable(char[] typeName, char[] name, char[] initializer) {
	GlobalVariable var = new GlobalVariable(typeName, name, initializer);
	if (this.variableCount >= this.variables.length) // assume variables is never empty
		System.arraycopy(this.variables, 0, this.variables = new GlobalVariable[this.variableCount * 2], 0, this.variableCount);
	this.variables[this.variableCount++] = var;
	this.varsChanged = true;
	return var;
}
/**
 * Computes the selection at the specified positions of the given code snippet.
 * (Note that this evaluation context's VM doesn't need to be running.)
 *  @param codeSnippet char[]
 * 		The code snipper source
 *
 *  @param selectionSourceStart int
 *
 *  @param selectionSourceEnd int
 *
 *  @param environment org.eclipse.jdt.internal.core.SearchableEnvironment
 *      used to resolve type/package references and search for types/packages
 *      based on partial names.
 *
 *  @param requestor org.eclipse.jdt.internal.codeassist.ISelectionRequestor
 *      since the engine might produce answers of various forms, the engine
 *      is associated with a requestor able to accept all possible selections.
 *
 *  @param options java.util.Map
 *		set of options used to configure the code assist engine.
 */
public void select(
	char[] codeSnippet,
	int selectionSourceStart,
	int selectionSourceEnd,
	SearchableEnvironment environment,
	ISelectionRequestor requestor,
	Map<String, String> options,
	WorkingCopyOwner owner) {

	final char[] className = "CodeSnippetSelection".toCharArray(); //$NON-NLS-1$
	final long complianceVersion = CompilerOptions.versionToJdkLevel(options.get(JavaCore.COMPILER_COMPLIANCE));
	final CodeSnippetToCuMapper mapper = new CodeSnippetToCuMapper(
		codeSnippet,
		this.packageName,
		this.imports,
		className,
		this.installedVars == null ? null : this.installedVars.className,
		this.localVariableNames,
		this.localVariableTypeNames,
		this.localVariableModifiers,
		this.declaringTypeName,
		this.lineSeparator,
		complianceVersion
	);
	ICompilationUnit sourceUnit = new ICompilationUnit() {
		@Override
		public char[] getFileName() {
			return CharOperation.concat(className, Util.defaultJavaExtension().toCharArray());
		}
		@Override
		public char[] getContents() {
			return mapper.getCUSource(EvaluationContext.this.lineSeparator);
		}
		@Override
		public char[] getMainTypeName() {
			return className;
		}
		@Override
		public char[][] getPackageName() {
			return null;
		}
		@Override
		public boolean ignoreOptionalProblems() {
			return false;
		}
		@Override
		public char[] getModuleName() {
			return null;
		}
	};
	SelectionEngine engine = new SelectionEngine(environment, mapper.getSelectionRequestor(requestor), options, owner);
	engine.select(sourceUnit, mapper.startPosOffset + selectionSourceStart, mapper.startPosOffset + selectionSourceEnd);
}
/**
 * Sets the imports of this evaluation context. An import is the name of a package
 * or the fully qualified name of a type as defined in the import statement of
 * a compilation unit (see the Java Language Specifications for more details).
 */
public void setImports(char[][] imports) {
	this.imports = imports;
	this.varsChanged = true; // this may change the visibility of the variable's types
}
/**
 * Sets the line separator used by this evaluation context.
 */
public void setLineSeparator(String lineSeparator) {
	this.lineSeparator = lineSeparator;
}
/**
 * Sets the dot-separated name of the package code snippets are ran into.
 * The default package name is an empty array.
 */
public void setPackageName(char[] packageName) {
	this.packageName = packageName;
	this.varsChanged = true; // this may change the visibility of the variable's types
}
}
