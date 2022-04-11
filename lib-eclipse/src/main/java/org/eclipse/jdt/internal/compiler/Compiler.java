/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
 *     Stephan Herrmann - contributions for
 *     							bug 337868 - [compiler][model] incomplete support for package-info.java when using SearchableEnvironment
 *     							bug 186342 - [compiler][null] Using annotations for null checking
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler;

import org.eclipse.jdt.core.compiler.*;
import org.eclipse.jdt.internal.compiler.env.*;
import org.eclipse.jdt.internal.compiler.impl.*;
import org.eclipse.jdt.internal.compiler.ast.*;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.lookup.*;
import org.eclipse.jdt.internal.compiler.parser.*;
import org.eclipse.jdt.internal.compiler.problem.*;
import org.eclipse.jdt.internal.compiler.util.*;

import java.io.*;
import java.util.*;

@SuppressWarnings("rawtypes")
public class Compiler implements ITypeRequestor, ProblemSeverities {
	public Parser parser;
	public ICompilerRequestor requestor;
	public CompilerOptions options;
	public ProblemReporter problemReporter;
	protected PrintWriter out; // output for messages that are not sent to problemReporter
	public CompilerStats stats;
	public CompilationProgress progress;
	public int remainingIterations = 1;

	// management of unit to be processed
	//public CompilationUnitResult currentCompilationUnitResult;
	public CompilationUnitDeclaration[] unitsToProcess;
	public int totalUnits; // (totalUnits-1) gives the last unit in unitToProcess

	private Map<String, APTProblem[]> aptProblems;

	// name lookup
	public LookupEnvironment lookupEnvironment;

	// ONCE STABILIZED, THESE SHOULD RETURN TO A FINAL FIELD
	public static boolean DEBUG = false;
	public int parseThreshold = -1;

	public AbstractAnnotationProcessorManager annotationProcessorManager;
	public int annotationProcessorStartIndex = 0;
	public ReferenceBinding[] referenceBindings;
	public boolean useSingleThread = true; // by default the compiler will not use worker threads to read/process/write

	// number of initial units parsed at once (-1: none)

	/*
	 * Static requestor reserved to listening compilation results in debug mode,
	 * so as for example to monitor compiler activity independantly from a particular
	 * builder implementation. It is reset at the end of compilation, and should not
	 * persist any information after having been reset.
	 */
	public static IDebugRequestor DebugRequestor = null;

	/**
	 * Answer a new compiler using the given name environment and compiler options.
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
	 *  @param settings java.util.Map
	 *      The settings that control the compiler behavior.
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
	 *
	 *  @deprecated this constructor is kept to preserve 3.1 and 3.2M4 compatibility
	 */
	public Compiler(
			INameEnvironment environment,
			IErrorHandlingPolicy policy,
			Map<String, String> settings,
			final ICompilerRequestor requestor,
			IProblemFactory problemFactory) {
		this(environment, policy, new CompilerOptions(settings), requestor, problemFactory, null /* printwriter */, null /* progress */);
	}

	/**
	 * Answer a new compiler using the given name environment and compiler options.
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
	 *  @param settings java.util.Map
	 *      The settings that control the compiler behavior.
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
	 *
	 *  @param parseLiteralExpressionsAsConstants <code>boolean</code>
	 *		This parameter is used to optimize the literals or leave them as they are in the source.
	 * 		If you put true, "Hello" + " world" will be converted to "Hello world".
	 *
	 *  @deprecated this constructor is kept to preserve 3.1 and 3.2M4 compatibility
	 */
	public Compiler(
			INameEnvironment environment,
			IErrorHandlingPolicy policy,
			Map settings,
			final ICompilerRequestor requestor,
			IProblemFactory problemFactory,
			boolean parseLiteralExpressionsAsConstants) {
		this(environment, policy, new CompilerOptions(settings, parseLiteralExpressionsAsConstants), requestor, problemFactory, null /* printwriter */, null /* progress */);
	}

	/**
	 * Answer a new compiler using the given name environment and compiler options.
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
	 *  @param options org.eclipse.jdt.internal.compiler.impl.CompilerOptions
	 *      The options that control the compiler behavior.
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
	public Compiler(
		INameEnvironment environment,
		IErrorHandlingPolicy policy,
		CompilerOptions options,
		final ICompilerRequestor requestor,
		IProblemFactory problemFactory) {
		this(environment, policy, options, requestor, problemFactory, null /* printwriter */, null /* progress */);
	}

	/**
	 * Answer a new compiler using the given name environment and compiler options.
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
	 *  @param options org.eclipse.jdt.internal.compiler.impl.CompilerOptions
	 *      The options that control the compiler behavior.
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
	 * @deprecated
	 */
	public Compiler(
			INameEnvironment environment,
			IErrorHandlingPolicy policy,
			CompilerOptions options,
			final ICompilerRequestor requestor,
			IProblemFactory problemFactory,
			PrintWriter out) {
		this(environment, policy, options, requestor, problemFactory, out, null /* progress */);
	}

	public Compiler(
			INameEnvironment environment,
			IErrorHandlingPolicy policy,
			CompilerOptions options,
			final ICompilerRequestor requestor,
			IProblemFactory problemFactory,
			PrintWriter out,
			CompilationProgress progress) {

		this.options = options;
		this.progress = progress;

		// wrap requestor in DebugRequestor if one is specified
		if(DebugRequestor == null) {
			this.requestor = requestor;
		} else {
			this.requestor = new ICompilerRequestor(){
				@Override
				public void acceptResult(CompilationResult result){
					if (DebugRequestor.isActive()){
						DebugRequestor.acceptDebugResult(result);
					}
					requestor.acceptResult(result);
				}
			};
		}
		this.problemReporter = new ProblemReporter(policy, this.options, problemFactory);
		this.lookupEnvironment = new LookupEnvironment(this, this.options, this.problemReporter, environment);
		this.out = out == null ? new PrintWriter(System.out, true) : out;
		this.stats = new CompilerStats();
		initializeParser();
	}

	/**
	 * Add an additional binary type
	 */
	@Override
	public void accept(IBinaryType binaryType, PackageBinding packageBinding, AccessRestriction accessRestriction) {
		if (this.options.verbose) {
			this.out.println(
				Messages.bind(Messages.compilation_loadBinary, new String(binaryType.getName())));
//			new Exception("TRACE BINARY").printStackTrace(System.out);
//		    System.out.println();
		}
		LookupEnvironment env = packageBinding.environment;
		env.createBinaryTypeFrom(binaryType, packageBinding, accessRestriction);
	}

	/**
	 * Add an additional compilation unit into the loop
	 *  ->  build compilation unit declarations, their bindings and record their results.
	 */
	@Override
	public void accept(ICompilationUnit sourceUnit, AccessRestriction accessRestriction) {
		// Switch the current policy and compilation result for this unit to the requested one.
		CompilationResult unitResult =
			new CompilationResult(sourceUnit, this.totalUnits, this.totalUnits, this.options.maxProblemsPerUnit);
		unitResult.checkSecondaryTypes = true;
		try {
			if (this.options.verbose) {
				String count = String.valueOf(this.totalUnits + 1);
				this.out.println(
					Messages.bind(Messages.compilation_request,
						new String[] {
							count,
							count,
							new String(sourceUnit.getFileName())
						}));
			}
			// diet parsing for large collection of unit
			CompilationUnitDeclaration parsedUnit;
			if (this.totalUnits < this.parseThreshold) {
				parsedUnit = this.parser.parse(sourceUnit, unitResult);
			} else {
				parsedUnit = this.parser.dietParse(sourceUnit, unitResult);
			}
			// initial type binding creation
			this.lookupEnvironment.buildTypeBindings(parsedUnit, accessRestriction);
			addCompilationUnit(sourceUnit, parsedUnit);

			// binding resolution
			this.lookupEnvironment.completeTypeBindings(parsedUnit);
		} catch (AbortCompilationUnit e) {
			// at this point, currentCompilationUnitResult may not be sourceUnit, but some other
			// one requested further along to resolve sourceUnit.
			if (unitResult.compilationUnit == sourceUnit) { // only report once
				this.requestor.acceptResult(unitResult.tagAsAccepted());
			} else {
				throw e; // want to abort enclosing request to compile
			}
		}
	}

	/**
	 * Add additional source types
	 */
	@Override
	public void accept(ISourceType[] sourceTypes, PackageBinding packageBinding, AccessRestriction accessRestriction) {
		this.problemReporter.abortDueToInternalError(
			Messages.bind(Messages.abort_againstSourceModel, new String[] { String.valueOf(sourceTypes[0].getName()), String.valueOf(sourceTypes[0].getFileName()) }));
	}

	protected synchronized void addCompilationUnit(
		ICompilationUnit sourceUnit,
		CompilationUnitDeclaration parsedUnit) {

		if (this.unitsToProcess == null)
			return; // not collecting units

		// append the unit to the list of ones to process later on
		int size = this.unitsToProcess.length;
		if (this.totalUnits == size)
			// when growing reposition units starting at position 0
			System.arraycopy(
				this.unitsToProcess,
				0,
				(this.unitsToProcess = new CompilationUnitDeclaration[size * 2]),
				0,
				this.totalUnits);
		this.unitsToProcess[this.totalUnits++] = parsedUnit;
	}

	/**
	 * Add the initial set of compilation units into the loop
	 *  ->  build compilation unit declarations, their bindings and record their results.
	 */
	protected void beginToCompile(ICompilationUnit[] sourceUnits) {
		int maxUnits = sourceUnits.length;
		this.totalUnits = 0;
		this.unitsToProcess = new CompilationUnitDeclaration[maxUnits];

		internalBeginToCompile(sourceUnits, maxUnits);
	}

	/**
	 * Checks whether the compilation has been canceled and reports the given progress to the compiler progress.
	 */
	protected void reportProgress(String taskDecription) {
		if (this.progress != null) {
			if (this.progress.isCanceled()) {
				// Only AbortCompilation can stop the compiler cleanly.
				// We check cancellation again following the call to compile.
				throw new AbortCompilation(true, null);
			}
			this.progress.setTaskName(taskDecription);
		}
	}

	/**
	 * Checks whether the compilation has been canceled and reports the given work increment to the compiler progress.
	 */
	protected void reportWorked(int workIncrement, int currentUnitIndex) {
		if (this.progress != null) {
			if (this.progress.isCanceled()) {
				// Only AbortCompilation can stop the compiler cleanly.
				// We check cancellation again following the call to compile.
				throw new AbortCompilation(true, null);
			}
			this.progress.worked(workIncrement, (this.totalUnits* this.remainingIterations) - currentUnitIndex - 1);
		}
	}

	public void compile(ICompilationUnit[] sourceUnits) {
		compile(sourceUnits, false);
	}
	/**
	 * General API
	 * -> compile each of supplied files
	 * -> recompile any required types for which we have an incomplete principle structure
	 */
	private void compile(ICompilationUnit[] sourceUnits, boolean lastRound) {
		this.stats.startTime = System.currentTimeMillis();
		try {
			// build and record parsed units
			reportProgress(Messages.compilation_beginningToCompile);

			if (this.options.complianceLevel >= ClassFileConstants.JDK9) {
				// in Java 9 the compiler must never ask the oracle for a module that is contained in the input units:
				sortModuleDeclarationsFirst(sourceUnits);
			}
			if (this.annotationProcessorManager == null) {
				beginToCompile(sourceUnits);
			} else {
				ICompilationUnit[] originalUnits = sourceUnits.clone(); // remember source units in case a source type collision occurs
				try {
					beginToCompile(sourceUnits);
					if (!lastRound) {
						processAnnotations();
					}
					if (!this.options.generateClassFiles) {
						// -proc:only was set on the command line
						return;
					}
				} catch (SourceTypeCollisionException e) {
					backupAptProblems();
					reset();
					// a generated type was referenced before it was created
					// the compiler either created a MissingType or found a BinaryType for it
					// so add the processor's generated files & start over,
					// but remember to only pass the generated files to the annotation processor
					int originalLength = originalUnits.length;
					int newProcessedLength = e.newAnnotationProcessorUnits.length;
					ICompilationUnit[] combinedUnits = new ICompilationUnit[originalLength + newProcessedLength];
					System.arraycopy(originalUnits, 0, combinedUnits, 0, originalLength);
					System.arraycopy(e.newAnnotationProcessorUnits, 0, combinedUnits, originalLength, newProcessedLength);
					this.annotationProcessorStartIndex  = originalLength;
					compile(combinedUnits, e.isLastRound);
					return;
				}
			}
			// Restore the problems before the results are processed and cleaned up.
			restoreAptProblems();
			processCompiledUnits(0, lastRound);
		} catch (AbortCompilation e) {
			this.handleInternalException(e, null);
		}
		if (this.options.verbose) {
			if (this.totalUnits > 1) {
				this.out.println(
					Messages.bind(Messages.compilation_units, String.valueOf(this.totalUnits)));
			} else {
				this.out.println(
					Messages.bind(Messages.compilation_unit, String.valueOf(this.totalUnits)));
			}
		}
	}

	private void sortModuleDeclarationsFirst(ICompilationUnit[] sourceUnits) {
		Arrays.sort(sourceUnits, (u1, u2) -> {
			char[] fn1 = u1.getFileName();
			char[] fn2 = u2.getFileName();
			boolean isMod1 = CharOperation.endsWith(fn1, TypeConstants.MODULE_INFO_FILE_NAME) || CharOperation.endsWith(fn1, TypeConstants.MODULE_INFO_CLASS_NAME);
			boolean isMod2 = CharOperation.endsWith(fn2, TypeConstants.MODULE_INFO_FILE_NAME) || CharOperation.endsWith(fn2, TypeConstants.MODULE_INFO_CLASS_NAME);
			if (isMod1 == isMod2)
				return 0;
			return isMod1 ? -1 : 1;
		});
	}

	static class APTProblem {
		CategorizedProblem problem;
		ReferenceContext context;
		APTProblem(CategorizedProblem problem, ReferenceContext context) {
			this.problem = problem;
			this.context = context;
		}
	}

	protected void backupAptProblems() {
		if (this.unitsToProcess == null) return;
		for (int i = 0; i < this.totalUnits; i++) {
			CompilationUnitDeclaration unitDecl = this.unitsToProcess[i];
			CompilationResult result = unitDecl.compilationResult;
			if (result != null && result.hasErrors()) {
				CategorizedProblem[] errors = result.getErrors();
				for (CategorizedProblem problem : errors) {
					if (problem.getCategoryID() == CategorizedProblem.CAT_UNSPECIFIED) {
						if (this.aptProblems == null) {
							this.aptProblems = new HashMap<>();
						}
						APTProblem[] problems = this.aptProblems.get(new String(unitDecl.getFileName()));
						if (problems == null) {
							this.aptProblems.put(
									new String(unitDecl.getFileName()),
									new APTProblem[] { new APTProblem(problem, result.getContext(problem)) });
						} else {
							APTProblem[] temp = new APTProblem[problems.length + 1];
							System.arraycopy(problems, 0, temp, 0, problems.length);
							temp[problems.length] = new APTProblem(problem, result.getContext(problem));
							this.aptProblems.put(new String(unitDecl.getFileName()), temp);
						}
					}
				}
			}
		}
	}

	protected void restoreAptProblems() {
		if (this.unitsToProcess != null && this.aptProblems!= null) {
			for (int i = 0; i < this.totalUnits; i++) {
				CompilationUnitDeclaration unitDecl = this.unitsToProcess[i];
				APTProblem[] problems = this.aptProblems.get(new String(unitDecl.getFileName()));
				if (problems != null) {
					for (APTProblem problem : problems) {
						unitDecl.compilationResult.record(problem.problem, problem.context);
					}
				}
			}
		}
		this.aptProblems = null; // No need for this.
	}

	protected void processCompiledUnits(int startingIndex, boolean lastRound) throws java.lang.Error {
		CompilationUnitDeclaration unit = null;
		ProcessTaskManager processingTask = null;
		try {
			if (this.useSingleThread) {
				// process all units (some more could be injected in the loop by the lookup environment)
				for (int i = startingIndex; i < this.totalUnits; i++) {
					unit = this.unitsToProcess[i];
					if (unit.compilationResult != null && unit.compilationResult.hasBeenAccepted)
						continue;
					reportProgress(Messages.bind(Messages.compilation_processing, new String(unit.getFileName())));
					try {
						if (this.options.verbose)
							this.out.println(
								Messages.bind(Messages.compilation_process,
								new String[] {
									String.valueOf(i + 1),
									String.valueOf(this.totalUnits),
									new String(this.unitsToProcess[i].getFileName())
								}));
						process(unit, i);
					} finally {
						// cleanup compilation unit result, but only if not annotation processed.
						if (this.annotationProcessorManager == null || shouldCleanup(i))
							unit.cleanUp();
					}
					if (this.annotationProcessorManager == null) {
						this.unitsToProcess[i] = null; // release reference to processed unit declaration
					}

					reportWorked(1, i);
					this.stats.lineCount += unit.compilationResult.lineSeparatorPositions.length;
					long acceptStart = System.currentTimeMillis();
					this.requestor.acceptResult(unit.compilationResult.tagAsAccepted());
					this.stats.generateTime += System.currentTimeMillis() - acceptStart; // record accept time as part of generation
					if (this.options.verbose)
						this.out.println(
							Messages.bind(Messages.compilation_done,
							new String[] {
								String.valueOf(i + 1),
								String.valueOf(this.totalUnits),
								new String(unit.getFileName())
							}));
				}
			} else {
				processingTask = new ProcessTaskManager(this, startingIndex);
				int acceptedCount = 0;
				// process all units (some more could be injected in the loop by the lookup environment)
				// the processTask can continue to process units until its fixed sized cache is full then it must wait
				// for this this thread to accept the units as they appear (it only waits if no units are available)
				while (true) {
					try {
						unit = processingTask.removeNextUnit(); // waits if no units are in the processed queue
					} catch (Error | RuntimeException e) {
						unit = processingTask.unitToProcess;
						throw e;
					}
					if (unit == null) break;
					reportWorked(1, acceptedCount++);
					this.stats.lineCount += unit.compilationResult.lineSeparatorPositions.length;
					this.requestor.acceptResult(unit.compilationResult.tagAsAccepted());
					if (this.options.verbose)
						this.out.println(
							Messages.bind(Messages.compilation_done,
							new String[] {
								String.valueOf(acceptedCount),
								String.valueOf(this.totalUnits),
								new String(unit.getFileName())
							}));
				}
			}
			if (!lastRound) {
				if (this.annotationProcessorManager != null && this.totalUnits > this.annotationProcessorStartIndex) {
					int backup = this.annotationProcessorStartIndex;
					int prevUnits = this.totalUnits;
					processAnnotations();
					// Clean up the units that were left out previously for annotation processing.
					for (int i = backup; i < prevUnits; i++) {
						this.unitsToProcess[i].cleanUp();
					}
					processCompiledUnits(backup, lastRound);
				}
			}
		} catch (AbortCompilation e) {
			this.handleInternalException(e, unit);
		} catch (Error | RuntimeException e) {
			this.handleInternalException(e, unit, null);
			throw e; // rethrow
		} finally {
			if (processingTask != null) {
				processingTask.shutdown();
				processingTask = null;
			}
			reset();
			this.annotationProcessorStartIndex  = 0;
			this.stats.endTime = System.currentTimeMillis();
			this.stats.overallTime += this.stats.endTime - this.stats.startTime;
		}
	}

	public synchronized CompilationUnitDeclaration getUnitToProcess(int next) {
		if (next < this.totalUnits) {
			CompilationUnitDeclaration unit = this.unitsToProcess[next];
			if (this.annotationProcessorManager == null || next < this.annotationProcessorStartIndex) {
				this.unitsToProcess[next] = null; // release reference to processed unit declaration
			}
			return unit;
		}
		return null;
	}

	/*
	 * Returns whether the compilation unit at the given index should be
	 * cleaned up after processing. This basically means whether or not
	 * the unit is still required for annotation processing.
	 */
	public boolean shouldCleanup(int index) {
		return index < this.annotationProcessorStartIndex;
	}

	public void setBinaryTypes(ReferenceBinding[] binaryTypes) {
		this.referenceBindings = binaryTypes;
	}
	/*
	 * Compiler crash recovery in case of unexpected runtime exceptions
	 */
	protected void handleInternalException(
		Throwable internalException,
		CompilationUnitDeclaration unit,
		CompilationResult result) {

		if (result == null && unit != null) {
			result = unit.compilationResult; // current unit being processed ?
		}
		// Lookup environment may be in middle of connecting types
		if (result == null && this.lookupEnvironment.unitBeingCompleted != null) {
		    result = this.lookupEnvironment.unitBeingCompleted.compilationResult;
		}
		if (result == null) {
			synchronized (this) {
				if (this.unitsToProcess != null && this.totalUnits > 0)
					result = this.unitsToProcess[this.totalUnits - 1].compilationResult;
			}
		}
		// last unit in beginToCompile ?

		boolean needToPrint = true;
		if (result != null) {
			/* create and record a compilation problem */
			// only keep leading portion of the trace
			String[] pbArguments = new String[] {
				Messages.bind(Messages.compilation_internalError, Util.getExceptionSummary(internalException)),
			};

			result
				.record(
					this.problemReporter
						.createProblem(
							result.getFileName(),
							IProblem.Unclassified,
							pbArguments,
							pbArguments,
							Error, // severity
							0, // source start
							0, // source end
							0, // line number
							0),// column number
					unit, true);

			/* hand back the compilation result */
			if (!result.hasBeenAccepted) {
				this.requestor.acceptResult(result.tagAsAccepted());
				needToPrint = false;
			}
		}
		if (needToPrint) {
			/* dump a stack trace to the console */
			internalException.printStackTrace();
		}
	}

	/*
	 * Compiler recovery in case of internal AbortCompilation event
	 */
	protected void handleInternalException(
		AbortCompilation abortException,
		CompilationUnitDeclaration unit) {

		/* special treatment for SilentAbort: silently cancelling the compilation process */
		if (abortException.isSilent) {
			if (abortException.silentException == null) {
				return;
			}
			throw abortException.silentException;
		}

		/* uncomment following line to see where the abort came from */
		// abortException.printStackTrace();

		// Exception may tell which compilation result it is related, and which problem caused it
		CompilationResult result = abortException.compilationResult;
		if (result == null && unit != null) {
			result = unit.compilationResult; // current unit being processed ?
		}
		// Lookup environment may be in middle of connecting types
		if (result == null && this.lookupEnvironment.unitBeingCompleted != null) {
		    result = this.lookupEnvironment.unitBeingCompleted.compilationResult;
		}
		if (result == null) {
			synchronized (this) {
				if (this.unitsToProcess != null && this.totalUnits > 0)
					result = this.unitsToProcess[this.totalUnits - 1].compilationResult;
			}
		}
		// last unit in beginToCompile ?
		if (result != null && !result.hasBeenAccepted) {
			/* distant problem which could not be reported back there? */
			if (abortException.problem != null) {
				recordDistantProblem: {
				CategorizedProblem distantProblem = abortException.problem;
				CategorizedProblem[] knownProblems = result.problems;
					for (int i = 0; i < result.problemCount; i++) {
						if (knownProblems[i] == distantProblem) { // already recorded
							break recordDistantProblem;
						}
					}
					if (distantProblem instanceof DefaultProblem) { // fixup filename TODO (philippe) should improve API to make this official
						((DefaultProblem) distantProblem).setOriginatingFileName(result.getFileName());
					}
					result.record(distantProblem, unit, true);
				}
			} else {
				/* distant internal exception which could not be reported back there */
				if (abortException.exception != null) {
					this.handleInternalException(abortException.exception, null, result);
					return;
				}
			}
			/* hand back the compilation result */
			if (!result.hasBeenAccepted) {
				this.requestor.acceptResult(result.tagAsAccepted());
			}
		} else {
			abortException.printStackTrace();
		}
	}

	public void initializeParser() {

		this.parser = new Parser(this.problemReporter, this.options.parseLiteralExpressionsAsConstants);
	}

	private  void abortIfPreviewNotAllowed(ICompilationUnit[] sourceUnits, int maxUnits) {
		if (!this.options.enablePreviewFeatures)
			return;
		try {
			if (this.options.sourceLevel != ClassFileConstants.getLatestJDKLevel()) {
				this.problemReporter.abortDueToPreviewEnablingNotAllowed(CompilerOptions.versionFromJdkLevel(this.options.sourceLevel), CompilerOptions.getLatestVersion());
			}
		} catch (AbortCompilation a) {
			// best effort to find a way for reporting this problem: report on the first source
			if (a.compilationResult == null) {
				a.compilationResult = new CompilationResult(sourceUnits[0], 0, maxUnits, this.options.maxProblemsPerUnit);
			}
			throw a;
		}
	}
	/**
	 * Add the initial set of compilation units into the loop
	 *  ->  build compilation unit declarations, their bindings and record their results.
	 */
	protected void internalBeginToCompile(ICompilationUnit[] sourceUnits, int maxUnits) {
		abortIfPreviewNotAllowed(sourceUnits,maxUnits);
		if (!this.useSingleThread && maxUnits >= ReadManager.THRESHOLD)
			this.parser.readManager = new ReadManager(sourceUnits, maxUnits);
		// Switch the current policy and compilation result for this unit to the requested one.
		for (int i = 0; i < maxUnits; i++) {
			CompilationResult unitResult = null;
			try {
				if (this.options.verbose) {
					this.out.println(
						Messages.bind(Messages.compilation_request,
						new String[] {
							String.valueOf(i + 1),
							String.valueOf(maxUnits),
							new String(sourceUnits[i].getFileName())
						}));
				}
				// diet parsing for large collection of units
				CompilationUnitDeclaration parsedUnit;
				unitResult = new CompilationResult(sourceUnits[i], i, maxUnits, this.options.maxProblemsPerUnit);
				long parseStart = System.currentTimeMillis();
				if (this.totalUnits < this.parseThreshold) {
					parsedUnit = this.parser.parse(sourceUnits[i], unitResult);
				} else {
					parsedUnit = this.parser.dietParse(sourceUnits[i], unitResult);
				}
				long resolveStart = System.currentTimeMillis();
				this.stats.parseTime += resolveStart - parseStart;
				// initial type binding creation
				this.lookupEnvironment.buildTypeBindings(parsedUnit, null /*no access restriction*/);
				this.stats.resolveTime += System.currentTimeMillis() - resolveStart;
				addCompilationUnit(sourceUnits[i], parsedUnit);
				ImportReference currentPackage = parsedUnit.currentPackage;
				if (currentPackage != null) {
					unitResult.recordPackageName(currentPackage.tokens);
				}
				//} catch (AbortCompilationUnit e) {
				//	requestor.acceptResult(unitResult.tagAsAccepted());
			} catch (AbortCompilation a) {
				// best effort to find a way for reporting this problem:
				if (a.compilationResult == null)
					a.compilationResult = unitResult;
				throw a;
			} finally {
				sourceUnits[i] = null; // no longer hold onto the unit
			}
		}
		if (this.parser.readManager != null) {
			this.parser.readManager.shutdown();
			this.parser.readManager = null;
		}
		// binding resolution
		this.lookupEnvironment.completeTypeBindings();
	}

	/**
	 * Process a compilation unit already parsed and build.
	 */
	public void process(CompilationUnitDeclaration unit, int i) {
		this.lookupEnvironment.unitBeingCompleted = unit;
		long parseStart = System.currentTimeMillis();

		this.parser.getMethodBodies(unit);

		long resolveStart = System.currentTimeMillis();
		this.stats.parseTime += resolveStart - parseStart;

		// fault in fields & methods
		if (unit.scope != null)
			unit.scope.faultInTypes();

		// verify inherited methods
		if (unit.scope != null)
			unit.scope.verifyMethods(this.lookupEnvironment.methodVerifier());

		// type checking
		unit.resolve();

		long analyzeStart = System.currentTimeMillis();
		this.stats.resolveTime += analyzeStart - resolveStart;

		//No need of analysis or generation of code if statements are not required
		if (!this.options.ignoreMethodBodies) unit.analyseCode(); // flow analysis

		long generateStart = System.currentTimeMillis();
		this.stats.analyzeTime += generateStart - analyzeStart;

		if (!this.options.ignoreMethodBodies) unit.generateCode(); // code generation

		// reference info
		if (this.options.produceReferenceInfo && unit.scope != null)
			unit.scope.storeDependencyInfo();

		// finalize problems (suppressWarnings)
		unit.finalizeProblems();

		this.stats.generateTime += System.currentTimeMillis() - generateStart;

		// refresh the total number of units known at this stage
		unit.compilationResult.totalUnitsKnown = this.totalUnits;

		this.lookupEnvironment.unitBeingCompleted = null;
	}

	protected void processAnnotations() {
		int newUnitSize = 0;
		int newClassFilesSize = 0;
		int bottom = this.annotationProcessorStartIndex;
		int top = this.totalUnits;
		ReferenceBinding[] binaryTypeBindingsTemp = this.referenceBindings;
		if (top == 0 && binaryTypeBindingsTemp == null) return;
		this.referenceBindings = null;
		do {
			// extract units to process
			int length = top - bottom;
			CompilationUnitDeclaration[] currentUnits = new CompilationUnitDeclaration[length];
			int index = 0;
			for (int i = bottom; i < top; i++) {
				CompilationUnitDeclaration currentUnit = this.unitsToProcess[i];
				currentUnits[index++] = currentUnit;
			}
			if (index != length) {
				System.arraycopy(currentUnits, 0, (currentUnits = new CompilationUnitDeclaration[index]), 0, index);
			}
			this.annotationProcessorManager.processAnnotations(currentUnits, binaryTypeBindingsTemp, false);
			// https://bugs.eclipse.org/bugs/show_bug.cgi?id=407841
			// It is possible that during the #processAnnotations() call, some units in the next batch would have been
			// brought forward and compiled already. If there are any such, process them for annotations then and there.
			// This would avoid the complications of marking some units as compiled but not-annotation-processed-yet.
			if (top < this.totalUnits) {
				length = this.totalUnits - top; // NOTE: Reuse the same variable, but make sure it's not used after this point
				CompilationUnitDeclaration[] addedUnits = new CompilationUnitDeclaration[length];
				System.arraycopy(this.unitsToProcess, top, addedUnits, 0, length);
				this.annotationProcessorManager.processAnnotations(addedUnits, binaryTypeBindingsTemp, false);
			}
			this.annotationProcessorStartIndex = top;
			ICompilationUnit[] newUnits = this.annotationProcessorManager.getNewUnits();
			newUnitSize = newUnits.length;
			ReferenceBinding[] newClassFiles = this.annotationProcessorManager.getNewClassFiles();
			binaryTypeBindingsTemp = newClassFiles;
			newClassFilesSize = newClassFiles.length;
			if (newUnitSize != 0) {
				ICompilationUnit[] newProcessedUnits = newUnits.clone(); // remember new units in case a source type collision occurs
				try {
					this.lookupEnvironment.isProcessingAnnotations = true;
					internalBeginToCompile(newUnits, newUnitSize);
				} catch (SourceTypeCollisionException e) {
					e.newAnnotationProcessorUnits = newProcessedUnits;
					throw e;
				} finally {
					this.lookupEnvironment.isProcessingAnnotations = false;
					this.annotationProcessorManager.reset();
				}
				bottom = top;
				top = this.totalUnits; // last unit added
				this.annotationProcessorStartIndex = top;
			} else {
				bottom = top;
				this.annotationProcessorManager.reset();
			}
		} while (newUnitSize != 0 || newClassFilesSize != 0);

		this.annotationProcessorManager.processAnnotations(null, null, true);
		// process potential units added in the final round see 329156
		ICompilationUnit[] newUnits = this.annotationProcessorManager.getNewUnits();
		newUnitSize = newUnits.length;
		try {
			if (newUnitSize != 0) {
				ICompilationUnit[] newProcessedUnits = newUnits.clone(); // remember new units in case a source type collision occurs
				try {
					this.lookupEnvironment.isProcessingAnnotations = true;
					internalBeginToCompile(newUnits, newUnitSize);
				} catch (SourceTypeCollisionException e) {
					e.isLastRound = true;
					e.newAnnotationProcessorUnits = newProcessedUnits;
					throw e;
				}
			}
		} finally {
			this.lookupEnvironment.isProcessingAnnotations = false;
			this.annotationProcessorManager.reset();
			this.annotationProcessorManager.cleanUp();
		}
		// Units added in final round don't get annotation processed
		this.annotationProcessorStartIndex = this.totalUnits;
	}

	public void reset() {
		this.lookupEnvironment.reset();
		this.parser.scanner.source = null;
		this.unitsToProcess = null;
		if (DebugRequestor != null) DebugRequestor.reset();
		this.problemReporter.reset();
	}

	/**
	 * Internal API used to resolve a given compilation unit. Can run a subset of the compilation process
	 */
	public CompilationUnitDeclaration resolve(
			CompilationUnitDeclaration unit,
			ICompilationUnit sourceUnit,
			boolean verifyMethods,
			boolean analyzeCode,
			boolean generateCode) {

		try {
			if (unit == null) {
				// build and record parsed units
				this.parseThreshold = 0; // will request a full parse
				beginToCompile(new ICompilationUnit[] { sourceUnit });
				// find the right unit from what was injected via accept(ICompilationUnit,..):
				for (int i=0; i<this.totalUnits; i++) {
					if (   this.unitsToProcess[i] != null
						&& this.unitsToProcess[i].compilationResult.compilationUnit == sourceUnit)
					{
						unit = this.unitsToProcess[i];
						break;
					}
				}
				if (unit == null)
					unit = this.unitsToProcess[0]; // fall back to old behavior

			} else {
				// initial type binding creation
				this.lookupEnvironment.buildTypeBindings(unit, null /*no access restriction*/);

				// binding resolution
				this.lookupEnvironment.completeTypeBindings();
			}
			this.lookupEnvironment.unitBeingCompleted = unit;
			this.parser.getMethodBodies(unit);
			if (unit.scope != null) {
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
			// leave this.lookupEnvironment.unitBeingCompleted set to the unit, until another unit is resolved
			// other calls to dom can cause classpath errors to be detected, resulting in AbortCompilation exceptions

			// No reset is performed there anymore since,
			// within the CodeAssist (or related tools),
			// the compiler may be called *after* a call
			// to this resolve(...) method. And such a call
			// needs to have a compiler with a non-empty
			// environment.
			// this.reset();
		}
	}
	/**
	 * Internal API used to resolve a given compilation unit. Can run a subset of the compilation process
	 */
	public CompilationUnitDeclaration resolve(
			ICompilationUnit sourceUnit,
			boolean verifyMethods,
			boolean analyzeCode,
			boolean generateCode) {

		return resolve(
			null,
			sourceUnit,
			verifyMethods,
			analyzeCode,
			generateCode);
	}
}
