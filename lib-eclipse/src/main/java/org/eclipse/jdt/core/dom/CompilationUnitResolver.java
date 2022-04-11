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
 *     Stephan Herrmann - Contribution for
 *								bug 363858 - [dom] early throwing of AbortCompilation causes NPE in CompilationUnitResolver
 *								Bug 466279 - [hovering] IAE on hover when annotation-based null analysis is enabled
 *******************************************************************************/
package org.eclipse.jdt.core.dom;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.WorkingCopyOwner;
import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.Compiler;
import org.eclipse.jdt.internal.compiler.DefaultErrorHandlingPolicies;
import org.eclipse.jdt.internal.compiler.ICompilerRequestor;
import org.eclipse.jdt.internal.compiler.IErrorHandlingPolicy;
import org.eclipse.jdt.internal.compiler.IProblemFactory;
import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.batch.FileSystem.Classpath;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.env.AccessRestriction;
import org.eclipse.jdt.internal.compiler.env.INameEnvironment;
import org.eclipse.jdt.internal.compiler.env.ISourceType;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.lookup.AnnotationBinding;
import org.eclipse.jdt.internal.compiler.lookup.Binding;
import org.eclipse.jdt.internal.compiler.lookup.ExtraCompilerModifiers;
import org.eclipse.jdt.internal.compiler.lookup.PackageBinding;
import org.eclipse.jdt.internal.compiler.parser.Parser;
import org.eclipse.jdt.internal.compiler.problem.AbortCompilation;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblemFactory;
import org.eclipse.jdt.internal.compiler.problem.ProblemReporter;
import org.eclipse.jdt.internal.compiler.util.HashtableOfObject;
import org.eclipse.jdt.internal.compiler.util.HashtableOfObjectToInt;
import org.eclipse.jdt.internal.compiler.util.Messages;
import org.eclipse.jdt.internal.compiler.util.Util;
import org.eclipse.jdt.internal.core.BinaryMember;
import org.eclipse.jdt.internal.core.BinaryModule;
import org.eclipse.jdt.internal.core.CancelableNameEnvironment;
import org.eclipse.jdt.internal.core.CancelableProblemFactory;
import org.eclipse.jdt.internal.core.INameEnvironmentWithProgress;
import org.eclipse.jdt.internal.core.JavaProject;
import org.eclipse.jdt.internal.core.LocalVariable;
import org.eclipse.jdt.internal.core.NameLookup;
import org.eclipse.jdt.internal.core.SourceRefElement;
import org.eclipse.jdt.internal.core.SourceTypeElementInfo;
import org.eclipse.jdt.internal.core.util.BindingKeyResolver;
import org.eclipse.jdt.internal.core.util.CommentRecorderParser;
import org.eclipse.jdt.internal.core.util.DOMFinder;

@SuppressWarnings({ "rawtypes", "unchecked" })
class CompilationUnitResolver extends Compiler {
	public static final int RESOLVE_BINDING = 0x1;
	public static final int PARTIAL = 0x2;
	public static final int STATEMENT_RECOVERY = 0x4;
	public static final int IGNORE_METHOD_BODIES = 0x8;
	public static final int BINDING_RECOVERY = 0x10;
	public static final int INCLUDE_RUNNING_VM_BOOTCLASSPATH = 0x20;

	/* A list of int */
	static class IntArrayList {
		public int[] list = new int[5];
		public int length = 0;
		public void add(int i) {
			if (this.list.length == this.length) {
				System.arraycopy(this.list, 0, this.list = new int[this.length*2], 0, this.length);
			}
				this.list[this.length++] = i;
			}
		}

	/*
	 * The sources that were requested.
	 * Map from file name (char[]) to org.eclipse.jdt.internal.compiler.env.ICompilationUnit.
	 */
	HashtableOfObject requestedSources;

	/*
	 * The binding keys that were requested.
	 * Map from file name (char[]) to BindingKey (or ArrayList if multiple keys in the same file).
	 */
	HashtableOfObject requestedKeys;

	DefaultBindingResolver.BindingTables bindingTables;

	boolean hasCompilationAborted;
	CategorizedProblem abortProblem;

	private IProgressMonitor monitor;

	/**
	 * Set to <code>true</code> if the receiver was initialized using a java project name environment
	 */
	boolean fromJavaProject;

	/**
	 * Answer a new CompilationUnitVisitor using the given name environment and compiler options.
	 * The environment and options will be in effect for the lifetime of the compiler.
	 * When the compiler is run, compilation results are sent to the given requestor.
	 *
	 *  @param environment org.eclipse.jdt.internal.compiler.api.env.INameEnvironment
	 *      Environment used by the compiler in order to resolve type and package
	 *      names. The name environment implements the actual connection of the compiler
	 *      to the outside world (for example, in batch mode the name environment is performing
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
	public CompilationUnitResolver(
		INameEnvironment environment,
		IErrorHandlingPolicy policy,
		CompilerOptions compilerOptions,
		ICompilerRequestor requestor,
		IProblemFactory problemFactory,
		IProgressMonitor monitor,
		boolean fromJavaProject) {

		super(environment, policy, compilerOptions, requestor, problemFactory);
		this.hasCompilationAborted = false;
		this.monitor =monitor;
		this.fromJavaProject = fromJavaProject;
	}

	/*
	 * Add additional source types
	 */
	@Override
	public void accept(ISourceType[] sourceTypes, PackageBinding packageBinding, AccessRestriction accessRestriction) {
		// Need to reparse the entire source of the compilation unit so as to get source positions
		// (case of processing a source that was not known by beginToCompile (e.g. when asking to createBinding))
		SourceTypeElementInfo sourceType = (SourceTypeElementInfo) sourceTypes[0];
		accept((org.eclipse.jdt.internal.compiler.env.ICompilationUnit) sourceType.getHandle().getCompilationUnit(), accessRestriction);
	}

	@Override
	public synchronized void accept(org.eclipse.jdt.internal.compiler.env.ICompilationUnit sourceUnit, AccessRestriction accessRestriction) {
		super.accept(sourceUnit, accessRestriction);
	}

	/**
	 * Add the initial set of compilation units into the loop
	 *  ->  build compilation unit declarations, their bindings and record their results.
	 */
	protected void beginToCompile(org.eclipse.jdt.internal.compiler.env.ICompilationUnit[] sourceUnits, String[] bindingKeys) {
		int sourceLength = sourceUnits.length;
		int keyLength = bindingKeys.length;
		int maxUnits = sourceLength + keyLength;
		this.totalUnits = 0;
		this.unitsToProcess = new CompilationUnitDeclaration[maxUnits];
		int index = 0;

		// walks the source units
		this.requestedSources = new HashtableOfObject();
		for (int i = 0; i < sourceLength; i++) {
			org.eclipse.jdt.internal.compiler.env.ICompilationUnit sourceUnit = sourceUnits[i];
			CompilationUnitDeclaration parsedUnit;
			CompilationResult unitResult =
				new CompilationResult(sourceUnit, index++, maxUnits, this.options.maxProblemsPerUnit);
			try {
				if (this.options.verbose) {
					this.out.println(
						Messages.bind(Messages.compilation_request,
						new String[] {
							String.valueOf(index++ + 1),
							String.valueOf(maxUnits),
							new String(sourceUnit.getFileName())
						}));
				}
				if (this.parser instanceof CommentRecorderParser) {
					((CommentRecorderParser) this.parser).resetComments();
				}
				// diet parsing for large collection of units
				if (this.totalUnits < this.parseThreshold) {
					parsedUnit = this.parser.parse(sourceUnit, unitResult);
				} else {
					parsedUnit = this.parser.dietParse(sourceUnit, unitResult);
				}
				// initial type binding creation
				this.lookupEnvironment.buildTypeBindings(parsedUnit, null /*no access restriction*/);
				addCompilationUnit(sourceUnit, parsedUnit);
				this.requestedSources.put(unitResult.getFileName(), sourceUnit);
				worked(1);
			} finally {
				sourceUnits[i] = null; // no longer hold onto the unit
			}
		}

		// walk the binding keys
		this.requestedKeys = new HashtableOfObject();
		for (int i = 0; i < keyLength; i++) {
			BindingKeyResolver resolver = new BindingKeyResolver(bindingKeys[i], this, this.lookupEnvironment);
			resolver.parse(true/*pause after fully qualified name*/);
			// If it doesn't have a type name, then it is either an array type, package or base type, which will definitely not have a compilation unit.
			// Skipping it will speed up performance because the call will open jars. (theodora)
			CompilationUnitDeclaration parsedUnit = resolver.hasTypeName() ? resolver.getCompilationUnitDeclaration() : null;
			if (parsedUnit != null) {
				char[] fileName = parsedUnit.compilationResult.getFileName();
				Object existing = this.requestedKeys.get(fileName);
				if (existing == null)
					this.requestedKeys.put(fileName, resolver);
				else if (existing instanceof ArrayList)
					((ArrayList) existing).add(resolver);
				else {
					ArrayList list = new ArrayList();
					list.add(existing);
					list.add(resolver);
					this.requestedKeys.put(fileName, list);
				}

			} else {
				char[] key = resolver.hasTypeName()
					? resolver.getKey().toCharArray() // binary binding
					: resolver.hasModuleName()
					    ? resolver.moduleName()
					    : CharOperation.concatWith(resolver.compoundName(), '.'); // package binding or base type binding
				this.requestedKeys.put(key, resolver);
			}
			worked(1);
		}

		// binding resolution
		this.lookupEnvironment.completeTypeBindings();
	}

	IBinding createBinding(String key) {
		if (this.bindingTables == null)
			throw new RuntimeException("Cannot be called outside ASTParser#createASTs(...)"); //$NON-NLS-1$
		BindingKeyResolver keyResolver = new BindingKeyResolver(key, this, this.lookupEnvironment);
		Binding compilerBinding = keyResolver.getCompilerBinding();
		if (compilerBinding == null) return null;
		DefaultBindingResolver resolver = new DefaultBindingResolver(this.lookupEnvironment, null/*no owner*/, this.bindingTables, false, this.fromJavaProject);
		return resolver.getBinding(compilerBinding);
	}

	public static CompilationUnit convert(
			CompilationUnitDeclaration compilationUnitDeclaration,
			char[] source,
			int apiLevel,
			Map options,
			boolean needToResolveBindings,
			WorkingCopyOwner owner,
			DefaultBindingResolver.BindingTables bindingTables,
			int flags,
			IProgressMonitor monitor,
			boolean fromJavaProject) {
		BindingResolver resolver = null;
		AST ast = AST.newAST(apiLevel, JavaCore.ENABLED.equals(options.get(JavaCore.COMPILER_PB_ENABLE_PREVIEW_FEATURES)));
		String sourceModeSetting = (String) options.get(JavaCore.COMPILER_SOURCE);
		long sourceLevel = CompilerOptions.versionToJdkLevel(sourceModeSetting);
		if (sourceLevel == 0) {
			// unknown sourceModeSetting
			sourceLevel = ClassFileConstants.JDK1_3;
		}
		ast.scanner.sourceLevel = sourceLevel;
		String compliance = (String) options.get(JavaCore.COMPILER_COMPLIANCE);
		long complianceLevel = CompilerOptions.versionToJdkLevel(compliance);
		if (complianceLevel == 0) {
			// unknown sourceModeSetting
			complianceLevel = sourceLevel;
		}
		ast.scanner.complianceLevel = complianceLevel;
		ast.setDefaultNodeFlag(ASTNode.ORIGINAL);
		CompilationUnit compilationUnit = null;
		ASTConverter converter = new ASTConverter(options, needToResolveBindings, monitor);
		if (needToResolveBindings) {
			resolver = new DefaultBindingResolver(compilationUnitDeclaration.scope, owner, bindingTables, (flags & ICompilationUnit.ENABLE_BINDINGS_RECOVERY) != 0, fromJavaProject);
			ast.setFlag(flags | AST.RESOLVED_BINDINGS);
		} else {
			resolver = new BindingResolver();
			ast.setFlag(flags);
		}
		ast.setBindingResolver(resolver);
		converter.setAST(ast);
		compilationUnit = converter.convert(compilationUnitDeclaration, source);
		compilationUnit.setLineEndTable(compilationUnitDeclaration.compilationResult.getLineSeparatorPositions());
		ast.setDefaultNodeFlag(0);
		ast.setOriginalModificationCount(ast.modificationCount());
		return compilationUnit;
	}

	protected static CompilerOptions getCompilerOptions(Map options, boolean statementsRecovery) {
		CompilerOptions compilerOptions = new CompilerOptions(options);
		compilerOptions.performMethodsFullRecovery = statementsRecovery;
		compilerOptions.performStatementsRecovery = statementsRecovery;
		compilerOptions.parseLiteralExpressionsAsConstants = false;
		compilerOptions.storeAnnotations = true /*store annotations in the bindings*/;
		compilerOptions.ignoreSourceFolderWarningOption = true;
		return compilerOptions;
	}
	/*
	 *  Low-level API performing the actual compilation
	 */
	protected static IErrorHandlingPolicy getHandlingPolicy() {

		// passes the initial set of files to the batch oracle (to avoid finding more than once the same units when case insensitive match)
		return new IErrorHandlingPolicy() {
			@Override
			public boolean stopOnFirstError() {
				return false;
			}
			@Override
			public boolean proceedOnErrors() {
				return false; // stop if there are some errors
			}
			@Override
			public boolean ignoreAllErrors() {
				return false;
			}
		};
	}

	/*
	 * Answer the component to which will be handed back compilation results from the compiler
	 */
	protected static ICompilerRequestor getRequestor() {
		return new ICompilerRequestor() {
			@Override
			public void acceptResult(CompilationResult compilationResult) {
				// do nothing
			}
		};
	}

	@Override
	public void initializeParser() {
		this.parser = new CommentRecorderParser(this.problemReporter, false);
	}
	@Override
	public void process(CompilationUnitDeclaration unit, int i) {
		// don't resolve a second time the same unit (this would create the same binding twice)
		char[] fileName = unit.compilationResult.getFileName();
		if (this.requestedKeys.get(fileName) == null && this.requestedSources.get(fileName) == null)
			super.process(unit, i);
	}
	/*
	 * Compiler crash recovery in case of unexpected runtime exceptions
	 */
	@Override
	protected void handleInternalException(
			Throwable internalException,
			CompilationUnitDeclaration unit,
			CompilationResult result) {
		super.handleInternalException(internalException, unit, result);
		if (unit != null) {
			removeUnresolvedBindings(unit);
		}
	}

	/*
	 * Compiler recovery in case of internal AbortCompilation event
	 */
	@Override
	protected void handleInternalException(
			AbortCompilation abortException,
			CompilationUnitDeclaration unit) {
		super.handleInternalException(abortException, unit);
		if (unit != null) {
			removeUnresolvedBindings(unit);
		}
		this.hasCompilationAborted = true;
		this.abortProblem = abortException.problem;
	}

	public static void parse(ICompilationUnit[] compilationUnits, ASTRequestor astRequestor, int apiLevel, Map options, int flags, IProgressMonitor monitor) {
		CompilerOptions compilerOptions = new CompilerOptions(options);
		compilerOptions.ignoreMethodBodies = (flags & ICompilationUnit.IGNORE_METHOD_BODIES) != 0;
		Parser parser = new CommentRecorderParser(
			new ProblemReporter(
					DefaultErrorHandlingPolicies.proceedWithAllProblems(),
					compilerOptions,
					new DefaultProblemFactory()),
			false);
		int unitLength = compilationUnits.length;
		SubMonitor subMonitor = SubMonitor.convert(monitor);
		for (int i = 0; i < unitLength; i++) {
			subMonitor.setWorkRemaining(unitLength - i);
			org.eclipse.jdt.internal.compiler.env.ICompilationUnit sourceUnit = (org.eclipse.jdt.internal.compiler.env.ICompilationUnit) compilationUnits[i];
			CompilationResult compilationResult = new CompilationResult(sourceUnit, 0, 0,
					compilerOptions.maxProblemsPerUnit);
			CompilationUnitDeclaration compilationUnitDeclaration = parser.dietParse(sourceUnit, compilationResult);

			if (compilationUnitDeclaration.ignoreMethodBodies) {
				compilationUnitDeclaration.ignoreFurtherInvestigation = true;
				// if initial diet parse did not work, no need to dig into method bodies.
				continue;
			}

			//fill the methods bodies in order for the code to be generated
			//real parse of the method....
			org.eclipse.jdt.internal.compiler.ast.TypeDeclaration[] types = compilationUnitDeclaration.types;
			if (types != null) {
				for (int j = 0, typeLength = types.length; j < typeLength; j++) {
					types[j].parseMethods(parser, compilationUnitDeclaration);
				}
			}

			// convert AST
			CompilationUnit node = convert(compilationUnitDeclaration, parser.scanner.getSource(), apiLevel, options,
					false/* don't resolve binding */, null/* no owner needed */, null/* no binding table needed */,
					flags /* flags */, subMonitor.split(1), true);
			node.setTypeRoot(compilationUnits[i]);

			// accept AST
			astRequestor.acceptAST(compilationUnits[i], node);
		}
	}
	public static void parse(
			String[] sourceUnits,
			String[] encodings,
			FileASTRequestor astRequestor,
			int apiLevel,
			Map options,
			int flags,
			IProgressMonitor monitor) {
		CompilerOptions compilerOptions = new CompilerOptions(options);
		compilerOptions.ignoreMethodBodies = (flags & ICompilationUnit.IGNORE_METHOD_BODIES) != 0;
		Parser parser = new CommentRecorderParser(
			new ProblemReporter(
					DefaultErrorHandlingPolicies.proceedWithAllProblems(),
					compilerOptions,
					new DefaultProblemFactory()),
			false);
		int unitLength = sourceUnits.length;
		SubMonitor subMonitor = SubMonitor.convert(monitor, unitLength);
		for (int i = 0; i < unitLength; i++) {
			SubMonitor iterationMonitor = subMonitor.split(1);
			char[] contents = null;
			String encoding = encodings != null ? encodings[i] : null;
			try {
				contents = Util.getFileCharContent(new File(sourceUnits[i]), encoding);
			} catch(IOException e) {
				// go to the next unit
				continue;
			}
			if (contents == null) {
				// go to the next unit
				continue;
			}
			org.eclipse.jdt.internal.compiler.batch.CompilationUnit compilationUnit = new org.eclipse.jdt.internal.compiler.batch.CompilationUnit(contents, sourceUnits[i], encoding);
			org.eclipse.jdt.internal.compiler.env.ICompilationUnit sourceUnit = compilationUnit;
			CompilationResult compilationResult = new CompilationResult(sourceUnit, 0, 0, compilerOptions.maxProblemsPerUnit);
			CompilationUnitDeclaration compilationUnitDeclaration = parser.dietParse(sourceUnit, compilationResult);

			if (compilationUnitDeclaration.ignoreMethodBodies) {
				compilationUnitDeclaration.ignoreFurtherInvestigation = true;
				// if initial diet parse did not work, no need to dig into method bodies.
				continue;
			}

			//fill the methods bodies in order for the code to be generated
			//real parse of the method....
			org.eclipse.jdt.internal.compiler.ast.TypeDeclaration[] types = compilationUnitDeclaration.types;
			if (types != null) {
				for (int j = 0, typeLength = types.length; j < typeLength; j++) {
					types[j].parseMethods(parser, compilationUnitDeclaration);
				}
			}

			// convert AST
			CompilationUnit node = convert(compilationUnitDeclaration, parser.scanner.getSource(), apiLevel, options,
					false/* don't resolve binding */, null/* no owner needed */, null/* no binding table needed */,
					flags /* flags */, iterationMonitor, true);
			node.setTypeRoot(null);

			// accept AST
			astRequestor.acceptAST(sourceUnits[i], node);
		}
	}
	public static CompilationUnitDeclaration parse(
			org.eclipse.jdt.internal.compiler.env.ICompilationUnit sourceUnit,
			NodeSearcher nodeSearcher,
			Map settings,
			int flags) {
		if (sourceUnit == null) {
			throw new IllegalStateException();
		}
		CompilerOptions compilerOptions = new CompilerOptions(settings);
		boolean statementsRecovery = (flags & ICompilationUnit.ENABLE_STATEMENTS_RECOVERY) != 0;
		compilerOptions.performMethodsFullRecovery = statementsRecovery;
		compilerOptions.performStatementsRecovery = statementsRecovery;
		compilerOptions.ignoreMethodBodies = (flags & ICompilationUnit.IGNORE_METHOD_BODIES) != 0;
		Parser parser = new CommentRecorderParser(
			new ProblemReporter(
					DefaultErrorHandlingPolicies.proceedWithAllProblems(),
					compilerOptions,
					new DefaultProblemFactory()),
			false);
		CompilationResult compilationResult = new CompilationResult(sourceUnit, 0, 0, compilerOptions.maxProblemsPerUnit);
		CompilationUnitDeclaration compilationUnitDeclaration = parser.dietParse(sourceUnit, compilationResult);

		if (compilationUnitDeclaration.ignoreMethodBodies) {
			compilationUnitDeclaration.ignoreFurtherInvestigation = true;
			// if initial diet parse did not work, no need to dig into method bodies.
			return compilationUnitDeclaration;
		}

		if (nodeSearcher != null) {
			char[] source = parser.scanner.getSource();
			int searchPosition = nodeSearcher.position;
			if (searchPosition < 0 || searchPosition > source.length) {
				// the position is out of range. There is no need to search for a node.
				return compilationUnitDeclaration;
			}

			compilationUnitDeclaration.traverse(nodeSearcher, compilationUnitDeclaration.scope);

			org.eclipse.jdt.internal.compiler.ast.ASTNode node = nodeSearcher.found;
			if (node == null) {
				return compilationUnitDeclaration;
			}

			org.eclipse.jdt.internal.compiler.ast.TypeDeclaration enclosingTypeDeclaration = nodeSearcher.enclosingType;

			if (node instanceof AbstractMethodDeclaration) {
				((AbstractMethodDeclaration)node).parseStatements(parser, compilationUnitDeclaration);
			} else if (enclosingTypeDeclaration != null) {
				if (node instanceof org.eclipse.jdt.internal.compiler.ast.Initializer) {
					((org.eclipse.jdt.internal.compiler.ast.Initializer) node).parseStatements(parser, enclosingTypeDeclaration, compilationUnitDeclaration);
				} else if (node instanceof org.eclipse.jdt.internal.compiler.ast.TypeDeclaration) {
					((org.eclipse.jdt.internal.compiler.ast.TypeDeclaration)node).parseMethods(parser, compilationUnitDeclaration);
				}
			}
		} else {
				//fill the methods bodies in order for the code to be generated
				//real parse of the method....
				org.eclipse.jdt.internal.compiler.ast.TypeDeclaration[] types = compilationUnitDeclaration.types;
				if (types != null) {
					for (int j = 0, typeLength = types.length; j < typeLength; j++) {
						types[j].parseMethods(parser, compilationUnitDeclaration);
					}
				}
		}
		return compilationUnitDeclaration;
	}

	public static void resolve(
		ICompilationUnit[] compilationUnits,
		String[] bindingKeys,
		ASTRequestor requestor,
		int apiLevel,
		Map options,
		IJavaProject javaProject,
		WorkingCopyOwner owner,
		int flags,
		IProgressMonitor monitor) {

		CancelableNameEnvironment environment = null;
		CancelableProblemFactory problemFactory = null;
		try {
			int amountOfWork = (compilationUnits.length + bindingKeys.length) * 2; // 1 for beginToCompile, 1 for resolve
			SubMonitor subMonitor = SubMonitor.convert(monitor, amountOfWork);
			environment = new CancelableNameEnvironment(((JavaProject) javaProject), owner, subMonitor);
			problemFactory = new CancelableProblemFactory(subMonitor);
			CompilerOptions compilerOptions = getCompilerOptions(options, (flags & ICompilationUnit.ENABLE_STATEMENTS_RECOVERY) != 0);
			compilerOptions.ignoreMethodBodies = (flags & ICompilationUnit.IGNORE_METHOD_BODIES) != 0;
			CompilationUnitResolver resolver =
				new CompilationUnitResolver(
					environment,
					getHandlingPolicy(),
					compilerOptions,
					getRequestor(),
					problemFactory,
					subMonitor,
					javaProject != null);
			resolver.resolve(compilationUnits, bindingKeys, requestor, apiLevel, options, owner, flags);
			if (NameLookup.VERBOSE) {
				environment.printTimeSpent();
			}
		} catch (JavaModelException e) {
			// project doesn't exist -> simple parse without resolving
			parse(compilationUnits, requestor, apiLevel, options, flags, monitor);
		} finally {
			if (environment != null) {
				environment.setMonitor(null); // don't hold a reference to this external object
			}
			if (problemFactory != null) {
				problemFactory.monitor = null; // don't hold a reference to this external object
			}
		}
	}
	public static void resolve(
			String[] sourceUnits,
			String[] encodings,
			String[] bindingKeys,
			FileASTRequestor requestor,
			int apiLevel,
			Map options,
			List classpaths,
			int flags,
			IProgressMonitor monitor) {

			INameEnvironmentWithProgress environment = null;
			CancelableProblemFactory problemFactory = null;
			try {
				int amountOfWork = (sourceUnits.length + bindingKeys.length) * 2; // 1 for beginToCompile, 1 for resolve
				SubMonitor subMonitor = SubMonitor.convert(monitor, amountOfWork);
				Classpath[] allEntries = new Classpath[classpaths.size()];
				classpaths.toArray(allEntries);
				environment = new NameEnvironmentWithProgress(allEntries, null, subMonitor);
				problemFactory = new CancelableProblemFactory(subMonitor);
				CompilerOptions compilerOptions = getCompilerOptions(options, (flags & ICompilationUnit.ENABLE_STATEMENTS_RECOVERY) != 0);
				compilerOptions.ignoreMethodBodies = (flags & ICompilationUnit.IGNORE_METHOD_BODIES) != 0;
				CompilationUnitResolver resolver =
					new CompilationUnitResolver(
						environment,
						getHandlingPolicy(),
						compilerOptions,
						getRequestor(),
						problemFactory,
						subMonitor,
						false);
				resolver.resolve(sourceUnits, encodings, bindingKeys, requestor, apiLevel, options, flags);
				if (NameLookup.VERBOSE && (environment instanceof CancelableNameEnvironment)) {
					CancelableNameEnvironment cancelableNameEnvironment = (CancelableNameEnvironment) environment;
					cancelableNameEnvironment.printTimeSpent();
				}
			} finally {
				if (environment != null) {
					environment.setMonitor(null); // don't hold a reference to this external object
				}
				if (problemFactory != null) {
					problemFactory.monitor = null; // don't hold a reference to this external object
				}
			}
		}
	public static CompilationUnitDeclaration resolve(
			org.eclipse.jdt.internal.compiler.env.ICompilationUnit sourceUnit,
			IJavaProject javaProject,
			List classpaths,
			NodeSearcher nodeSearcher,
			Map options,
			WorkingCopyOwner owner,
			int flags,
			IProgressMonitor monitor) throws JavaModelException {

		CompilationUnitDeclaration unit = null;
		INameEnvironmentWithProgress environment = null;
		CancelableProblemFactory problemFactory = null;
		CompilationUnitResolver resolver = null;
		try {
			if (javaProject == null) {
				Classpath[] allEntries = new Classpath[classpaths.size()];
				classpaths.toArray(allEntries);
				environment = new NameEnvironmentWithProgress(allEntries, null, monitor);
			} else {
				environment = new CancelableNameEnvironment((JavaProject) javaProject, owner, monitor);
			}
			problemFactory = new CancelableProblemFactory(monitor);
			CompilerOptions compilerOptions = getCompilerOptions(options, (flags & ICompilationUnit.ENABLE_STATEMENTS_RECOVERY) != 0);
			boolean ignoreMethodBodies = (flags & ICompilationUnit.IGNORE_METHOD_BODIES) != 0;
			compilerOptions.ignoreMethodBodies = ignoreMethodBodies;
			resolver =
				new CompilationUnitResolver(
					environment,
					getHandlingPolicy(),
					compilerOptions,
					getRequestor(),
					problemFactory,
					monitor,
					javaProject != null);
			boolean analyzeAndGenerateCode = !ignoreMethodBodies;
			unit =
				resolver.resolve(
					null, // no existing compilation unit declaration
					sourceUnit,
					nodeSearcher,
					true, // method verification
					analyzeAndGenerateCode, // analyze code
					analyzeAndGenerateCode); // generate code
			if (resolver.hasCompilationAborted) {
				// the bindings could not be resolved due to missing types in name environment
				// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=86541
				CompilationUnitDeclaration unitDeclaration = parse(sourceUnit, nodeSearcher, options, flags);
				if (unit != null) {
					final int problemCount = unit.compilationResult.problemCount;
					if (problemCount != 0) {
						unitDeclaration.compilationResult.problems = new CategorizedProblem[problemCount];
						System.arraycopy(unit.compilationResult.problems, 0, unitDeclaration.compilationResult.problems, 0, problemCount);
						unitDeclaration.compilationResult.problemCount = problemCount;
					}
				} else if (resolver.abortProblem != null) {
					unitDeclaration.compilationResult.problemCount = 1;
					unitDeclaration.compilationResult.problems = new CategorizedProblem[] { resolver.abortProblem };
				}
				return unitDeclaration;
			}
			if (NameLookup.VERBOSE && environment instanceof CancelableNameEnvironment) {
				CancelableNameEnvironment cancelableNameEnvironment = (CancelableNameEnvironment) environment;
				cancelableNameEnvironment.printTimeSpent();
			}
			return unit;
		} finally {
			if (environment != null) {
				// don't hold a reference to this external object
				environment.setMonitor(null);
			}
			if (problemFactory != null) {
				problemFactory.monitor = null; // don't hold a reference to this external object
			}
		}
	}
	public static IBinding[] resolve(
		final IJavaElement[] elements,
		int apiLevel,
		Map compilerOptions,
		IJavaProject javaProject,
		WorkingCopyOwner owner,
		int flags,
		IProgressMonitor monitor) {

		final int length = elements.length;
		final HashMap sourceElementPositions = new HashMap(); // a map from ICompilationUnit to int[] (positions in elements)
		int cuNumber = 0;
		final HashtableOfObjectToInt binaryElementPositions = new HashtableOfObjectToInt(); // a map from String (binding key) to int (position in elements)
		for (int i = 0; i < length; i++) {
			IJavaElement element = elements[i];
			if (!(element instanceof SourceRefElement))
				throw new IllegalStateException(element + " is not part of a compilation unit or class file"); //$NON-NLS-1$
			Object cu = element.getAncestor(IJavaElement.COMPILATION_UNIT);
			if (cu != null) {
				// source member
				IntArrayList intList = (IntArrayList) sourceElementPositions.get(cu);
				if (intList == null) {
					sourceElementPositions.put(cu, intList = new IntArrayList());
					cuNumber++;
				}
				intList.add(i);
			} else {
				// binary member or method argument
				try {
					String key;
					if (element instanceof BinaryMember)
						key = ((BinaryMember) element).getKey(true/*open to get resolved info*/);
					else if (element instanceof LocalVariable)
						key = ((LocalVariable) element).getKey(true/*open to get resolved info*/);
					else if (element instanceof org.eclipse.jdt.internal.core.TypeParameter)
						key = ((org.eclipse.jdt.internal.core.TypeParameter) element).getKey(true/*open to get resolved info*/);
					else if (element instanceof BinaryModule)
						key = ((BinaryModule) element).getKey(true);
					else
						throw new IllegalArgumentException(element + " has an unexpected type"); //$NON-NLS-1$
					binaryElementPositions.put(key, i);
				} catch (JavaModelException e) {
					throw new IllegalArgumentException(element + " does not exist", e); //$NON-NLS-1$
				}
			}
		}
		ICompilationUnit[] cus = new ICompilationUnit[cuNumber];
		sourceElementPositions.keySet().toArray(cus);

		int bindingKeyNumber = binaryElementPositions.size();
		String[] bindingKeys = new String[bindingKeyNumber];
		binaryElementPositions.keysToArray(bindingKeys);

		class Requestor extends ASTRequestor {
			IBinding[] bindings = new IBinding[length];
			@Override
			public void acceptAST(ICompilationUnit source, CompilationUnit ast) {
				// TODO (jerome) optimize to visit the AST only once
				IntArrayList intList = (IntArrayList) sourceElementPositions.get(source);
				for (int i = 0; i < intList.length; i++) {
					final int index = intList.list[i];
					SourceRefElement element = (SourceRefElement) elements[index];
					DOMFinder finder = new DOMFinder(ast, element, true/*resolve binding*/);
					try {
						finder.search();
					} catch (JavaModelException e) {
						throw new IllegalArgumentException(element + " does not exist", e); //$NON-NLS-1$
					}
					this.bindings[index] = finder.foundBinding;
				}
			}
			@Override
			public void acceptBinding(String bindingKey, IBinding binding) {
				int index = binaryElementPositions.get(bindingKey);
				this.bindings[index] = binding;
			}
		}
		Requestor requestor = new Requestor();
		resolve(cus, bindingKeys, requestor, apiLevel, compilerOptions, javaProject, owner, flags, monitor);
		return requestor.bindings;
	}
	/*
	 * When unit result is about to be accepted, removed back pointers
	 * to unresolved bindings
	 */
	public void removeUnresolvedBindings(CompilationUnitDeclaration compilationUnitDeclaration) {
		final org.eclipse.jdt.internal.compiler.ast.TypeDeclaration[] types = compilationUnitDeclaration.types;
		if (types != null) {
			for (int i = 0, max = types.length; i < max; i++) {
				removeUnresolvedBindings(types[i]);
			}
		}
	}
	private void removeUnresolvedBindings(org.eclipse.jdt.internal.compiler.ast.TypeDeclaration type) {
		final org.eclipse.jdt.internal.compiler.ast.TypeDeclaration[] memberTypes = type.memberTypes;
		if (memberTypes != null) {
			for (int i = 0, max = memberTypes.length; i < max; i++){
				removeUnresolvedBindings(memberTypes[i]);
			}
		}
		if (type.binding != null && (type.binding.modifiers & ExtraCompilerModifiers.AccUnresolved) != 0) {
			type.binding = null;
		}

		final org.eclipse.jdt.internal.compiler.ast.FieldDeclaration[] fields = type.fields;
		if (fields != null) {
			for (int i = 0, max = fields.length; i < max; i++){
				if (fields[i].binding != null && (fields[i].binding.modifiers & ExtraCompilerModifiers.AccUnresolved) != 0) {
					fields[i].binding = null;
				}
			}
		}

		final AbstractMethodDeclaration[] methods = type.methods;
		if (methods != null) {
			for (int i = 0, max = methods.length; i < max; i++){
				if (methods[i].binding !=  null && (methods[i].binding.modifiers & ExtraCompilerModifiers.AccUnresolved) != 0) {
					methods[i].binding = null;
				}
			}
		}
	}

	private void resolve(
			ICompilationUnit[] compilationUnits,
			String[] bindingKeys,
			ASTRequestor astRequestor,
			int apiLevel,
			Map compilerOptions,
			WorkingCopyOwner owner,
			int flags) {

		// temporarily connect ourselves to the ASTResolver - must disconnect when done
		astRequestor.compilationUnitResolver = this;
		this.bindingTables = new DefaultBindingResolver.BindingTables();
		CompilationUnitDeclaration unit = null;
		try {
			int length = compilationUnits.length;
			org.eclipse.jdt.internal.compiler.env.ICompilationUnit[] sourceUnits = new org.eclipse.jdt.internal.compiler.env.ICompilationUnit[length];
			System.arraycopy(compilationUnits, 0, sourceUnits, 0, length);
			beginToCompile(sourceUnits, bindingKeys);
			// process all units (some more could be injected in the loop by the lookup environment)
			for (int i = 0; i < this.totalUnits; i++) {
				if (resolvedRequestedSourcesAndKeys(i)) {
					// no need to keep resolving if no more ASTs and no more binding keys are needed
					// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=114935
					// cleanup remaining units
					for (; i < this.totalUnits; i++) {
						this.unitsToProcess[i].cleanUp();
						this.unitsToProcess[i] = null;
					}
					break;
				}
				unit = this.unitsToProcess[i];
				try {
					super.process(unit, i); // this.process(...) is optimized to not process already known units

					// requested AST
					char[] fileName = unit.compilationResult.getFileName();
					ICompilationUnit source = (ICompilationUnit) this.requestedSources.get(fileName);
					if (source != null) {
						// convert AST
						CompilationResult compilationResult = unit.compilationResult;
						org.eclipse.jdt.internal.compiler.env.ICompilationUnit sourceUnit = compilationResult.compilationUnit;
						char[] contents = sourceUnit.getContents();
						AST ast = AST.newAST(apiLevel, JavaCore.ENABLED.equals(this.options.getMap().get(JavaCore.COMPILER_PB_ENABLE_PREVIEW_FEATURES)));
						ast.setFlag(flags | AST.RESOLVED_BINDINGS);
						ast.setDefaultNodeFlag(ASTNode.ORIGINAL);
						ASTConverter converter = new ASTConverter(compilerOptions, true/*need to resolve bindings*/, this.monitor);
						BindingResolver resolver = new DefaultBindingResolver(unit.scope, owner, this.bindingTables, (flags & ICompilationUnit.ENABLE_BINDINGS_RECOVERY) != 0, this.fromJavaProject);
						ast.setBindingResolver(resolver);
						converter.setAST(ast);
						CompilationUnit compilationUnit = converter.convert(unit, contents);
						compilationUnit.setTypeRoot(source);
						compilationUnit.setLineEndTable(compilationResult.getLineSeparatorPositions());
						ast.setDefaultNodeFlag(0);
						ast.setOriginalModificationCount(ast.modificationCount());

						// pass it to requestor
						astRequestor.acceptAST(source, compilationUnit);

						worked(1);

						// remove at the end so that we don't resolve twice if a source and a key for the same file name have been requested
						this.requestedSources.put(fileName, null); // mark it as removed
					}

					// requested binding
					Object key = this.requestedKeys.get(fileName);
					if (key != null) {
						if (key instanceof BindingKeyResolver) {
							reportBinding(key, astRequestor, owner, unit);
							worked(1);
						} else if (key instanceof ArrayList) {
							Iterator iterator = ((ArrayList) key).iterator();
							while (iterator.hasNext()) {
								reportBinding(iterator.next(), astRequestor, owner, unit);
								worked(1);
							}
						}

						// remove at the end so that we don't resolve twice if a source and a key for the same file name have been requested
						this.requestedKeys.put(fileName, null); // mark it as removed
					}
				} finally {
					// cleanup compilation unit result
					unit.cleanUp();
				}
				this.unitsToProcess[i] = null; // release reference to processed unit declaration
				this.requestor.acceptResult(unit.compilationResult.tagAsAccepted());
			}

			// remaining binding keys
			DefaultBindingResolver resolver = new DefaultBindingResolver(this.lookupEnvironment, owner, this.bindingTables, (flags & ICompilationUnit.ENABLE_BINDINGS_RECOVERY) != 0, true);
			Object[] keys = this.requestedKeys.valueTable;
			for (int j = 0, keysLength = keys.length; j < keysLength; j++) {
				BindingKeyResolver keyResolver = (BindingKeyResolver) keys[j];
				if (keyResolver == null) continue;
				Binding compilerBinding = keyResolver.getCompilerBinding();
				IBinding binding = compilerBinding == null ? null : resolver.getBinding(compilerBinding);
				// pass it to requestor
				astRequestor.acceptBinding(((BindingKeyResolver) this.requestedKeys.valueTable[j]).getKey(), binding);
				worked(1);
			}
		} catch (OperationCanceledException e) {
			throw e;
		} catch (AbortCompilation e) {
			this.handleInternalException(e, unit);
		} catch (Error | RuntimeException e) {
			this.handleInternalException(e, unit, null);
			throw e; // rethrow
		} finally {
			// disconnect ourselves from ast requestor
			astRequestor.compilationUnitResolver = null;
		}
	}

	private void resolve(
			String[] sourceCompilationUnits,
			String[] encodings,
			String[] bindingKeys,
			FileASTRequestor astRequestor,
			int apiLevel,
			Map compilerOptions,
			int flags) {

		// temporarily connect ourselves to the ASTResolver - must disconnect when done
		astRequestor.compilationUnitResolver = this;
		this.bindingTables = new DefaultBindingResolver.BindingTables();
		CompilationUnitDeclaration unit = null;
		try {
			int length = sourceCompilationUnits.length;
			org.eclipse.jdt.internal.compiler.env.ICompilationUnit[] sourceUnits = new org.eclipse.jdt.internal.compiler.env.ICompilationUnit[length];
			int count = 0;
			for (int i = 0; i < length; i++) {
				char[] contents = null;
				String encoding = encodings != null ? encodings[i] : null;
				String sourceUnitPath = sourceCompilationUnits[i];
				try {
					contents = Util.getFileCharContent(new File(sourceUnitPath), encoding);
				} catch(IOException e) {
					// go to the next unit
					continue;
				}
				if (contents == null) {
					// go to the next unit
					continue;
				}
				sourceUnits[count++] = new org.eclipse.jdt.internal.compiler.batch.CompilationUnit(contents, sourceUnitPath, encoding);
			}
			if (count < length) {
				org.eclipse.jdt.internal.compiler.env.ICompilationUnit[] newArray = new org.eclipse.jdt.internal.compiler.env.ICompilationUnit[count];
				System.arraycopy(sourceUnits, 0, newArray, 0, count);
				sourceUnits = newArray;
			}
			beginToCompile(sourceUnits, bindingKeys);
			// process all units (some more could be injected in the loop by the lookup environment)
			for (int i = 0; i < this.totalUnits; i++) {
				if (resolvedRequestedSourcesAndKeys(i)) {
					// no need to keep resolving if no more ASTs and no more binding keys are needed
					// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=114935
					// cleanup remaining units
					for (; i < this.totalUnits; i++) {
						this.unitsToProcess[i].cleanUp();
						this.unitsToProcess[i] = null;
					}
					break;
				}
				unit = this.unitsToProcess[i];
				try {
					super.process(unit, i); // this.process(...) is optimized to not process already known units

					// requested AST
					char[] fileName = unit.compilationResult.getFileName();
					org.eclipse.jdt.internal.compiler.env.ICompilationUnit source = (org.eclipse.jdt.internal.compiler.env.ICompilationUnit) this.requestedSources.get(fileName);
					if (source != null) {
						// convert AST
						CompilationResult compilationResult = unit.compilationResult;
						org.eclipse.jdt.internal.compiler.env.ICompilationUnit sourceUnit = compilationResult.compilationUnit;
						char[] contents = sourceUnit.getContents();
						AST ast = AST.newAST(apiLevel, JavaCore.ENABLED.equals(compilerOptions.get(JavaCore.COMPILER_PB_ENABLE_PREVIEW_FEATURES)));
						ast.setFlag(flags | AST.RESOLVED_BINDINGS);
						ast.setDefaultNodeFlag(ASTNode.ORIGINAL);
						ASTConverter converter = new ASTConverter(compilerOptions, true/*need to resolve bindings*/, this.monitor);
						BindingResolver resolver = new DefaultBindingResolver(unit.scope, null, this.bindingTables, (flags & ICompilationUnit.ENABLE_BINDINGS_RECOVERY) != 0, this.fromJavaProject);
						ast.setBindingResolver(resolver);
						converter.setAST(ast);
						CompilationUnit compilationUnit = converter.convert(unit, contents);
						compilationUnit.setTypeRoot(null);
						compilationUnit.setLineEndTable(compilationResult.getLineSeparatorPositions());
						ast.setDefaultNodeFlag(0);
						ast.setOriginalModificationCount(ast.modificationCount());

						// pass it to requestor
						astRequestor.acceptAST(new String(source.getFileName()), compilationUnit);

						worked(1);

						// remove at the end so that we don't resolve twice if a source and a key for the same file name have been requested
						this.requestedSources.put(fileName, null); // mark it as removed
					}

					// requested binding
					Object key = this.requestedKeys.get(fileName);
					if (key != null) {
						if (key instanceof BindingKeyResolver) {
							reportBinding(key, astRequestor, unit);
							worked(1);
						} else if (key instanceof ArrayList) {
							Iterator iterator = ((ArrayList) key).iterator();
							while (iterator.hasNext()) {
								reportBinding(iterator.next(), astRequestor, unit);
								worked(1);
							}
						}

						// remove at the end so that we don't resolve twice if a source and a key for the same file name have been requested
						this.requestedKeys.put(fileName, null); // mark it as removed
					}
				} finally {
					// cleanup compilation unit result
					unit.cleanUp();
				}
				this.unitsToProcess[i] = null; // release reference to processed unit declaration
				this.requestor.acceptResult(unit.compilationResult.tagAsAccepted());
			}

			// remaining binding keys
			DefaultBindingResolver resolver = new DefaultBindingResolver(this.lookupEnvironment, null, this.bindingTables, (flags & ICompilationUnit.ENABLE_BINDINGS_RECOVERY) != 0, true);
			Object[] keys = this.requestedKeys.valueTable;
			for (int j = 0, keysLength = keys.length; j < keysLength; j++) {
				BindingKeyResolver keyResolver = (BindingKeyResolver) keys[j];
				if (keyResolver == null) continue;
				Binding compilerBinding = keyResolver.getCompilerBinding();
				IBinding binding = compilerBinding == null ? null : resolver.getBinding(compilerBinding);
				// pass it to requestor
				astRequestor.acceptBinding(((BindingKeyResolver) this.requestedKeys.valueTable[j]).getKey(), binding);
				worked(1);
			}
		} catch (OperationCanceledException e) {
			throw e;
		} catch (AbortCompilation e) {
			this.handleInternalException(e, unit);
		} catch (Error | RuntimeException e) {
			this.handleInternalException(e, unit, null);
			throw e; // rethrow
		} finally {
			// disconnect ourselves from ast requestor
			astRequestor.compilationUnitResolver = null;
		}
	}

	private void reportBinding(Object key, ASTRequestor astRequestor, WorkingCopyOwner owner, CompilationUnitDeclaration unit) {
		BindingKeyResolver keyResolver = (BindingKeyResolver) key;
		Binding compilerBinding = keyResolver.getCompilerBinding();
		if (compilerBinding != null) {
			DefaultBindingResolver resolver = new DefaultBindingResolver(unit.scope, owner, this.bindingTables, false, this.fromJavaProject);
			AnnotationBinding annotationBinding = keyResolver.getAnnotationBinding();
			IBinding binding;
			if (annotationBinding != null) {
				binding = resolver.getAnnotationInstance(annotationBinding);
			} else {
				binding = resolver.getBinding(compilerBinding);
			}
			if (binding != null)
				astRequestor.acceptBinding(keyResolver.getKey(), binding);
		}
	}

	private void reportBinding(Object key, FileASTRequestor astRequestor, CompilationUnitDeclaration unit) {
		BindingKeyResolver keyResolver = (BindingKeyResolver) key;
		Binding compilerBinding = keyResolver.getCompilerBinding();
		if (compilerBinding != null) {
			DefaultBindingResolver resolver = new DefaultBindingResolver(unit.scope, null, this.bindingTables, false, this.fromJavaProject);
			AnnotationBinding annotationBinding = keyResolver.getAnnotationBinding();
			IBinding binding;
			if (annotationBinding != null) {
				binding = resolver.getAnnotationInstance(annotationBinding);
			} else {
				binding = resolver.getBinding(compilerBinding);
			}
			if (binding != null)
				astRequestor.acceptBinding(keyResolver.getKey(), binding);
		}
	}

	private CompilationUnitDeclaration resolve(
			CompilationUnitDeclaration unit,
			org.eclipse.jdt.internal.compiler.env.ICompilationUnit sourceUnit,
			NodeSearcher nodeSearcher,
			boolean verifyMethods,
			boolean analyzeCode,
			boolean generateCode) {

		try {

			if (unit == null) {
				// build and record parsed units
				this.parseThreshold = 0; // will request a full parse
				beginToCompile(new org.eclipse.jdt.internal.compiler.env.ICompilationUnit[] { sourceUnit });
				// find the right unit from what was injected via accept(ICompilationUnit,..):
				for (int i=0, max = this.totalUnits; i < max; i++) {
					CompilationUnitDeclaration currentCompilationUnitDeclaration = this.unitsToProcess[i];
					if (currentCompilationUnitDeclaration != null
							&& currentCompilationUnitDeclaration.compilationResult.compilationUnit == sourceUnit) {
						unit = currentCompilationUnitDeclaration;
						break;
					}
				}
				if (unit == null) {
					unit = this.unitsToProcess[0]; // fall back to old behavior
				}
			} else {
				// initial type binding creation
				this.lookupEnvironment.buildTypeBindings(unit, null /*no access restriction*/);

				// binding resolution
				this.lookupEnvironment.completeTypeBindings();
			}

			if (nodeSearcher == null) {
				this.parser.getMethodBodies(unit); // no-op if method bodies have already been parsed
			} else {
				int searchPosition = nodeSearcher.position;
				char[] source = sourceUnit.getContents();
				int length = source.length;
				if (searchPosition >= 0 && searchPosition <= length) {
					unit.traverse(nodeSearcher, unit.scope);

					org.eclipse.jdt.internal.compiler.ast.ASTNode node = nodeSearcher.found;

		 			if (node != null) {
						// save existing values to restore them at the end of the parsing process
						// see bug 47079 for more details
						int[] oldLineEnds = this.parser.scanner.lineEnds;
						int oldLinePtr = this.parser.scanner.linePtr;

						this.parser.scanner.setSource(source, unit.compilationResult);

						org.eclipse.jdt.internal.compiler.ast.TypeDeclaration enclosingTypeDeclaration = nodeSearcher.enclosingType;
						if (node instanceof AbstractMethodDeclaration) {
							((AbstractMethodDeclaration)node).parseStatements(this.parser, unit);
						} else if (enclosingTypeDeclaration != null) {
							if (node instanceof org.eclipse.jdt.internal.compiler.ast.Initializer) {
								((org.eclipse.jdt.internal.compiler.ast.Initializer) node).parseStatements(this.parser, enclosingTypeDeclaration, unit);
							} else if (node instanceof org.eclipse.jdt.internal.compiler.ast.TypeDeclaration) {
								((org.eclipse.jdt.internal.compiler.ast.TypeDeclaration)node).parseMethods(this.parser, unit);
							}
						}
						// this is done to prevent any side effects on the compilation unit result
						// line separator positions array.
						this.parser.scanner.lineEnds = oldLineEnds;
						this.parser.scanner.linePtr = oldLinePtr;
					}
				}
			}

			if (unit.scope != null) {
				CompilationUnitDeclaration previousUnit = this.lookupEnvironment.unitBeingCompleted;
				this.lookupEnvironment.unitBeingCompleted = unit;
				try {
					// fault in fields & methods
					unit.scope.faultInTypes();
					if (unit.scope != null && verifyMethods) {
						// http://dev.eclipse.org/bugs/show_bug.cgi?id=23117
	 					// verify inherited methods
						unit.scope.verifyMethods(this.lookupEnvironment.methodVerifier());
					}
					// type checking
					unit.resolve();

					// flow analysis
					if (analyzeCode) unit.analyseCode();

					// code generation
					if (generateCode) unit.generateCode();

					// finalize problems (suppressWarnings)
					unit.finalizeProblems();
				} finally {
					this.lookupEnvironment.unitBeingCompleted = previousUnit; // paranoia, always null in org.eclipse.jdt.core.tests.dom.RunAllTests
				}
			}
			if (this.unitsToProcess != null) this.unitsToProcess[0] = null; // release reference to processed unit declaration
			this.requestor.acceptResult(unit.compilationResult.tagAsAccepted());
			return unit;
		} catch (AbortCompilation e) {
			this.handleInternalException(e, unit);
			return unit == null ? this.unitsToProcess[0] : unit;
		} catch (Error | RuntimeException e) {
			this.handleInternalException(e, unit, null);
			throw e; // rethrow
		} finally {
			// No reset is performed there anymore since,
			// within the CodeAssist (or related tools),
			// the compiler may be called *after* a call
			// to this resolve(...) method. And such a call
			// needs to have a compiler with a non-empty
			// environment.
			// this.reset();
		}
	}
	/*
	 * Internal API used to resolve a given compilation unit. Can run a subset of the compilation process
	 */
	@Override
	public CompilationUnitDeclaration resolve(
			org.eclipse.jdt.internal.compiler.env.ICompilationUnit sourceUnit,
			boolean verifyMethods,
			boolean analyzeCode,
			boolean generateCode) {

		return resolve(
			null, /* no existing compilation unit declaration*/
			sourceUnit,
			null/*no node searcher*/,
			verifyMethods,
			analyzeCode,
			generateCode);
	}

	boolean resolvedRequestedSourcesAndKeys(int unitIndexToProcess) {
		if (unitIndexToProcess < this.requestedSources.size() && unitIndexToProcess < this.requestedKeys.size())
			return false; // must process at least this many units before checking to see if all are done

		Object[] sources = this.requestedSources.valueTable;
		for (int i = 0, l = sources.length; i < l; i++)
			if (sources[i] != null) return false;
		Object[] keys = this.requestedKeys.valueTable;
		for (int i = 0, l = keys.length; i < l; i++)
			if (keys[i] != null) return false;
		return true;
	}

	/*
	 * Internal API used to resolve a given compilation unit. Can run a subset of the compilation process
	 */
	@Override
	public CompilationUnitDeclaration resolve(
			CompilationUnitDeclaration unit,
			org.eclipse.jdt.internal.compiler.env.ICompilationUnit sourceUnit,
			boolean verifyMethods,
			boolean analyzeCode,
			boolean generateCode) {

		return resolve(
			unit,
			sourceUnit,
			null/*no node searcher*/,
			verifyMethods,
			analyzeCode,
			generateCode);
	}

	private void worked(int work) {
		if (this.monitor != null) {
			if (this.monitor.isCanceled())
				throw new OperationCanceledException();
			this.monitor.worked(work);
		}
	}
}
