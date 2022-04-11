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
 *******************************************************************************/
package org.eclipse.jdt.internal.core;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.internal.compiler.*;
import org.eclipse.jdt.internal.compiler.Compiler;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.env.AccessRestriction;
import org.eclipse.jdt.internal.compiler.env.IModule;
import org.eclipse.jdt.internal.compiler.env.INameEnvironment;
import org.eclipse.jdt.internal.compiler.env.ISourceType;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.lookup.LookupEnvironment;
import org.eclipse.jdt.internal.compiler.lookup.PackageBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeConstants;
import org.eclipse.jdt.internal.compiler.parser.SourceTypeConverter;
import org.eclipse.jdt.internal.compiler.problem.AbortCompilation;
import org.eclipse.jdt.internal.core.util.CommentRecorderParser;
import org.eclipse.jdt.internal.core.util.Util;

/**
 * Responsible for resolving types inside a compilation unit being reconciled,
 * reporting the discovered problems to a given IProblemRequestor.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class CompilationUnitProblemFinder extends Compiler {

	/**
	 * Answer a new CompilationUnitVisitor using the given name environment and compiler options.
	 * The environment and options will be in effect for the lifetime of the compiler.
	 * When the compiler is run, compilation results are sent to the given requestor.
	 *
	 *  @param environment org.eclipse.jdt.internal.compiler.api.env.INameEnvironment
	 *      Environment used by the compiler in order to resolve type and package
	 *      names. The name environment implements the actual connection of the compiler
	 *      to the outside world (e.g. in batch mode the name environment is performing
	 *      pure file accesses, reuse previous build state or connection to repositories).
	 *      Note: the name environment is responsible for implementing the actual classpath
	 *            rules.
	 *
	 *  @param policy org.eclipse.jdt.internal.compiler.api.problem.IErrorHandlingPolicy
	 *      Configurable part for problem handling, allowing the compiler client to
	 *      specify the rules for handling problems (stop on first error or accumulate
	 *      them all) and at the same time perform some actions such as opening a dialog
	 *      in UI when compiling interactively.
	 *      @see org.eclipse.jdt.internal.compiler.DefaultErrorHandlingPolicies
	 *
	 *	@param compilerOptions The compiler options to use for the resolution.
	 *
	 *  @param requestor org.eclipse.jdt.internal.compiler.api.ICompilerRequestor
	 *      Component which will receive and persist all compilation results and is intended
	 *      to consume them as they are produced. Typically, in a batch compiler, it is
	 *      responsible for writing out the actual .class files to the file system.
	 *      @see org.eclipse.jdt.internal.compiler.CompilationResult
	 *
	 *  @param problemFactory org.eclipse.jdt.internal.compiler.api.problem.IProblemFactory
	 *      Factory used inside the compiler to create problem descriptors. It allows the
	 *      compiler client to supply its own representation of compilation problems in
	 *      order to avoid object conversions. Note that the factory is not supposed
	 *      to accumulate the created problems, the compiler will gather them all and hand
	 *      them back as part of the compilation unit result.
	 */
	protected CompilationUnitProblemFinder(
		INameEnvironment environment,
		IErrorHandlingPolicy policy,
		CompilerOptions compilerOptions,
		ICompilerRequestor requestor,
		IProblemFactory problemFactory) {

		super(environment,
			policy,
			compilerOptions,
			requestor,
			problemFactory
		);
	}

	/**
	 * Add additional source types
	 */
	@Override
	public void accept(ISourceType[] sourceTypes, PackageBinding packageBinding, AccessRestriction accessRestriction) {
		// ensure to jump back to toplevel type for first one (could be a member)
		while (sourceTypes[0].getEnclosingType() != null) {
			sourceTypes[0] = sourceTypes[0].getEnclosingType();
		}

		CompilationResult result =
			new CompilationResult(sourceTypes[0].getFileName(), 1, 1, this.options.maxProblemsPerUnit);

		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=305259, build the compilation unit in its own sand box.
		final long savedComplianceLevel = this.options.complianceLevel;
		final long savedSourceLevel = this.options.sourceLevel;

		LookupEnvironment environment = packageBinding.environment;
		if (environment == null)
			environment = this.lookupEnvironment;

		try {
			IJavaProject project = ((SourceTypeElementInfo) sourceTypes[0]).getHandle().getJavaProject();
			this.options.complianceLevel = CompilerOptions.versionToJdkLevel(project.getOption(JavaCore.COMPILER_COMPLIANCE, true));
			this.options.sourceLevel = CompilerOptions.versionToJdkLevel(project.getOption(JavaCore.COMPILER_SOURCE, true));

			// need to hold onto this
			CompilationUnitDeclaration unit =
				SourceTypeConverter.buildCompilationUnit(
						sourceTypes,//sourceTypes[0] is always toplevel here
						SourceTypeConverter.FIELD_AND_METHOD // need field and methods
						| SourceTypeConverter.MEMBER_TYPE // need member types
						| SourceTypeConverter.FIELD_INITIALIZATION, // need field initialization
						environment.problemReporter,
						result);

			if (unit != null) {
				environment.buildTypeBindings(unit, accessRestriction);
				CompilationUnitDeclaration previousUnitBeingCompleted = this.lookupEnvironment.unitBeingCompleted;
				environment.completeTypeBindings(unit);
				this.lookupEnvironment.unitBeingCompleted = previousUnitBeingCompleted;
			}
		} finally {
			this.options.complianceLevel = savedComplianceLevel;
			this.options.sourceLevel = savedSourceLevel;
		}
	}

	@Override
	public void accept(IModule module, LookupEnvironment environment) {
		IModuleDescription handle = null;
		if (module instanceof ModuleDescriptionInfo) {
			handle = ((ModuleDescriptionInfo) module).getHandle();
		}
		if (handle == null) {
			super.accept(module, environment);
			return;
		}
		CompilationResult result =
				new CompilationResult(TypeConstants.MODULE_INFO_FILE_NAME, 1, 1, this.options.maxProblemsPerUnit);

		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=305259, build the compilation unit in its own sand box.
		final long savedComplianceLevel = this.options.complianceLevel;
		final long savedSourceLevel = this.options.sourceLevel;

		if (environment == null)
			environment = this.lookupEnvironment;

		try {
			IJavaProject project = handle.getJavaProject();
			this.options.complianceLevel = CompilerOptions.versionToJdkLevel(project.getOption(JavaCore.COMPILER_COMPLIANCE, true));
			this.options.sourceLevel = CompilerOptions.versionToJdkLevel(project.getOption(JavaCore.COMPILER_SOURCE, true));

			// need to hold onto this
			CompilationUnitDeclaration unit =
				SourceTypeConverter.buildModularCompilationUnit(
						module,
						environment.problemReporter,
						result);

			if (unit != null) {
				environment.buildTypeBindings(unit, null);
				environment.completeTypeBindings(unit);
			}
		} finally {
			this.options.complianceLevel = savedComplianceLevel;
			this.options.sourceLevel = savedSourceLevel;
		}
	}

	protected static CompilerOptions getCompilerOptions(Map settings, boolean creatingAST, boolean statementsRecovery) {
		CompilerOptions compilerOptions = new CompilerOptions(settings);
		compilerOptions.performMethodsFullRecovery = statementsRecovery;
		compilerOptions.performStatementsRecovery = statementsRecovery;
		compilerOptions.parseLiteralExpressionsAsConstants = !creatingAST; /*parse literal expressions as constants only if not creating a DOM AST*/
		if (creatingAST)
			compilerOptions.storeAnnotations = true; /* store annotations in the bindings if creating a DOM AST */
		return compilerOptions;
	}

	/*
	 *  Low-level API performing the actual compilation
	 */
	protected static IErrorHandlingPolicy getHandlingPolicy() {
		return DefaultErrorHandlingPolicies.proceedWithAllProblems();
	}

	/*
	 * Answer the component to which will be handed back compilation results from the compiler
	 */
	protected static ICompilerRequestor getRequestor() {
		return new ICompilerRequestor() {
			@Override
			public void acceptResult(CompilationResult compilationResult) {
				// default requestor doesn't handle compilation results back
			}
		};
	}

	private static boolean isTestSource(IJavaProject project, ICompilationUnit cu) {
		try {
			IClasspathEntry[] resolvedClasspath = project.getResolvedClasspath(true);
			final IPath resourcePath = cu.getResource().getFullPath();
			for (IClasspathEntry e : resolvedClasspath) {
				if (e.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
					if (e.isTest()) {
						if (e.getPath().isPrefixOf(resourcePath)) {
							return true;
						}
					}
				}
			}
		} catch (JavaModelException e) {
			Util.log(e, "Exception while determining if compilation unit \"" + cu.getElementName() //$NON-NLS-1$
					+ "\" is test source"); //$NON-NLS-1$
		}
		return false;
	}

	/*
	 * Can return null if the process was aborted or canceled
	 */
	public static CompilationUnitDeclaration process(
			CompilationUnit unitElement,
			SourceElementParser parser,
			WorkingCopyOwner workingCopyOwner,
			HashMap problems,
			boolean creatingAST,
			int reconcileFlags,
			IProgressMonitor monitor)
		throws JavaModelException {

		JavaProject project = unitElement.getJavaProject();
		CancelableNameEnvironment environment = null;
		CancelableProblemFactory problemFactory = null;
		CompilationUnitProblemFinder problemFinder = null;
		CompilationUnitDeclaration unit = null;
		try {
			environment = new CancelableNameEnvironment(project, workingCopyOwner, monitor, !isTestSource(unitElement.getJavaProject(), unitElement));
			problemFactory = new CancelableProblemFactory(monitor);
			CompilerOptions compilerOptions = getCompilerOptions(project.getOptions(true), creatingAST, ((reconcileFlags & ICompilationUnit.ENABLE_STATEMENTS_RECOVERY) != 0));
			boolean ignoreMethodBodies = (reconcileFlags & ICompilationUnit.IGNORE_METHOD_BODIES) != 0;
			compilerOptions.ignoreMethodBodies = ignoreMethodBodies;
			problemFinder = new CompilationUnitProblemFinder(
				environment,
				getHandlingPolicy(),
				compilerOptions,
				getRequestor(),
				problemFactory);
			boolean analyzeAndGenerateCode = true;
			if (ignoreMethodBodies) {
				analyzeAndGenerateCode = false;
			}
			try {
				if (parser != null) {
					problemFinder.parser = parser;
					unit = parser.parseCompilationUnit(unitElement, true/*full parse*/, monitor);
					problemFinder.resolve(
						unit,
						unitElement,
						true, // verify methods
						analyzeAndGenerateCode, // analyze code
						analyzeAndGenerateCode); // generate code
				} else {
					unit =
						problemFinder.resolve(
							unitElement,
							true, // verify methods
							analyzeAndGenerateCode, // analyze code
							analyzeAndGenerateCode); // generate code
				}
			} catch (AbortCompilation e) {
				problemFinder.handleInternalException(e, unit);
			}
			if (unit != null) {
				CompilationResult unitResult = unit.compilationResult;
				CategorizedProblem[] unitProblems = unitResult.getCUProblems();
				int length = unitProblems == null ? 0 : unitProblems.length;
				if (length > 0) {
					CategorizedProblem[] categorizedProblems = new CategorizedProblem[length];
					System.arraycopy(unitProblems, 0, categorizedProblems, 0, length);
					problems.put(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER, categorizedProblems);
				}
				unitProblems = unitResult.getTasks();
				length = unitProblems == null ? 0 : unitProblems.length;
				if (length > 0) {
					CategorizedProblem[] categorizedProblems = new CategorizedProblem[length];
					System.arraycopy(unitProblems, 0, categorizedProblems, 0, length);
					problems.put(IJavaModelMarker.TASK_MARKER, categorizedProblems);
				}
				if (NameLookup.VERBOSE) {
					System.out.println(Thread.currentThread() + " TIME SPENT in NameLoopkup#seekTypesInSourcePackage: " + environment.nameLookup.timeSpentInSeekTypesInSourcePackage + "ms");  //$NON-NLS-1$ //$NON-NLS-2$
					System.out.println(Thread.currentThread() + " TIME SPENT in NameLoopkup#seekTypesInBinaryPackage: " + environment.nameLookup.timeSpentInSeekTypesInBinaryPackage + "ms");  //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
		} catch (OperationCanceledException e) {
			// catch this exception so as to not enter the catch(RuntimeException e) below
			throw e;
		} catch(RuntimeException e) {
			// avoid breaking other tools due to internal compiler failure (40334)
			String lineDelimiter = unitElement.findRecommendedLineSeparator();
			StringBuilder message = new StringBuilder("Exception occurred during problem detection:");  //$NON-NLS-1$
			message.append(lineDelimiter);
			message.append("----------------------------------- SOURCE BEGIN -------------------------------------"); //$NON-NLS-1$
			message.append(lineDelimiter);
			message.append(unitElement.getSource());
			message.append(lineDelimiter);
			message.append("----------------------------------- SOURCE END -------------------------------------"); //$NON-NLS-1$
			Util.log(e, message.toString());
			throw new JavaModelException(e, IJavaModelStatusConstants.COMPILER_FAILURE);
		} finally {
			if (environment != null)
				environment.setMonitor(null); // don't hold a reference to this external object
			if (problemFactory != null)
				problemFactory.monitor = null; // don't hold a reference to this external object
			// NB: unit.cleanUp() is done by caller
			if (problemFinder != null && !creatingAST)
				problemFinder.lookupEnvironment.reset();
		}
		return unit;
	}

	public static CompilationUnitDeclaration process(
			CompilationUnit unitElement,
			WorkingCopyOwner workingCopyOwner,
			HashMap problems,
			boolean creatingAST,
			int reconcileFlags,
			IProgressMonitor monitor)
			throws JavaModelException {

		return process(unitElement, null/*use default Parser*/, workingCopyOwner, problems, creatingAST, reconcileFlags, monitor);
	}

	/* (non-Javadoc)
	 * Fix for bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=60689.
	 * @see org.eclipse.jdt.internal.compiler.Compiler#initializeParser()
	 */
	@Override
	public void initializeParser() {
		this.parser = new CommentRecorderParser(this.problemReporter, this.options.parseLiteralExpressionsAsConstants);
	}
}

