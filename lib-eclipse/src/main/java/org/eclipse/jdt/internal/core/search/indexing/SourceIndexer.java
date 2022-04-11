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
package org.eclipse.jdt.internal.core.search.indexing;

import java.util.Collections;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.search.SearchDocument;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.DefaultErrorHandlingPolicies;
import org.eclipse.jdt.internal.compiler.ISourceElementRequestor;
import org.eclipse.jdt.internal.compiler.SourceElementParser;
import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.FunctionalExpression;
import org.eclipse.jdt.internal.compiler.ast.LambdaExpression;
import org.eclipse.jdt.internal.compiler.ast.ReferenceExpression;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.env.AccessRestriction;
import org.eclipse.jdt.internal.compiler.env.IBinaryType;
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.jdt.internal.compiler.env.INameEnvironment;
import org.eclipse.jdt.internal.compiler.env.ISourceType;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.impl.ITypeRequestor;
import org.eclipse.jdt.internal.compiler.lookup.LookupEnvironment;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.PackageBinding;
import org.eclipse.jdt.internal.compiler.parser.Parser;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblemFactory;
import org.eclipse.jdt.internal.compiler.problem.ProblemReporter;
import org.eclipse.jdt.internal.compiler.util.SuffixConstants;
import org.eclipse.jdt.internal.core.DefaultWorkingCopyOwner;
import org.eclipse.jdt.internal.core.JavaModel;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.jdt.internal.core.JavaProject;
import org.eclipse.jdt.internal.core.SourceTypeElementInfo;
import org.eclipse.jdt.internal.core.jdom.CompilationUnit;
import org.eclipse.jdt.internal.core.search.matching.IndexBasedJavaSearchEnvironment;
import org.eclipse.jdt.internal.core.search.matching.MethodPattern;
import org.eclipse.jdt.internal.core.search.processing.JobManager;

/**
 * A SourceIndexer indexes java files using a java parser. The following items are indexed:
 * Declarations of:<br>
 * - Classes<br>
 * - Interfaces;<br>
 * - Methods;<br>
 * - Fields;<br>
 * - Lambda expressions;<br>
 * References to:<br>
 * - Methods (with number of arguments); <br>
 * - Fields;<br>
 * - Types;<br>
 * - Constructors.
 */
public class SourceIndexer extends AbstractIndexer implements ITypeRequestor, SuffixConstants {

	private LookupEnvironment lookupEnvironment;
	private CompilerOptions options;
	public ISourceElementRequestor requestor;
	private Parser basicParser;
	private CompilationUnit compilationUnit;
	private CompilationUnitDeclaration cud;
	private static final boolean DEBUG = false;

	public SourceIndexer(SearchDocument document) {
		super(document);
		this.requestor = new SourceIndexerRequestor(this);
	}
	@Override
	public void indexDocument() {
		// Create a new Parser
		String documentPath = this.document.getPath();
		SourceElementParser parser = this.document.getParser();
		if (parser == null) {
			IPath path = new Path(documentPath);
			IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(path.segment(0));
			parser = JavaModelManager.getJavaModelManager().indexManager.getSourceElementParser(JavaCore.create(project), this.requestor);
		} else {
			parser.setRequestor(this.requestor);
		}

		// Launch the parser
		char[] source = null;
		char[] name = null;
		try {
			source = this.document.getCharContents();
			name = documentPath.toCharArray();
		} catch(Exception e){
			// ignore
		}
		if (source == null || name == null) return; // could not retrieve document info (e.g. resource was discarded)
		this.compilationUnit = new CompilationUnit(source, name);
		try {
			if (parser.parseCompilationUnit(this.compilationUnit, true, null).hasFunctionalTypes())
				this.document.requireIndexingResolvedDocument();
		} catch (Exception e) {
			if (JobManager.VERBOSE) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void accept(IBinaryType binaryType, PackageBinding packageBinding, AccessRestriction accessRestriction) {
		this.lookupEnvironment.createBinaryTypeFrom(binaryType, packageBinding, accessRestriction);
	}

	@Override
	public void accept(ICompilationUnit unit, AccessRestriction accessRestriction) {
		CompilationResult unitResult = new CompilationResult(unit, 1, 1, this.options.maxProblemsPerUnit);
		CompilationUnitDeclaration parsedUnit = this.basicParser.dietParse(unit, unitResult);
		this.lookupEnvironment.buildTypeBindings(parsedUnit, accessRestriction);
		this.lookupEnvironment.completeTypeBindings(parsedUnit, true);
	}

	@Override
	public void accept(ISourceType[] sourceTypes, PackageBinding packageBinding, AccessRestriction accessRestriction) {
		ISourceType sourceType = sourceTypes[0];
		while (sourceType.getEnclosingType() != null)
			sourceType = sourceType.getEnclosingType();
		SourceTypeElementInfo elementInfo = (SourceTypeElementInfo) sourceType;
		IType type = elementInfo.getHandle();
		ICompilationUnit sourceUnit = (ICompilationUnit) type.getCompilationUnit();
		accept(sourceUnit, accessRestriction);
	}

	public void resolveDocument() {
		try {
			IPath path = new Path(this.document.getPath());
			IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(path.segment(0));
			JavaModel model = JavaModelManager.getJavaModelManager().getJavaModel();
			JavaProject javaProject = (JavaProject) model.getJavaProject(project);

			this.options = new CompilerOptions(javaProject.getOptions(true));
			ProblemReporter problemReporter =
					new ProblemReporter(
							DefaultErrorHandlingPolicies.proceedWithAllProblems(),
							this.options,
							new DefaultProblemFactory());

			// Re-parse using normal parser, IndexingParser swallows several nodes, see comment above class.
			this.basicParser = new Parser(problemReporter, false);
			this.basicParser.reportOnlyOneSyntaxError = true;
			this.basicParser.scanner.taskTags = null;
			this.cud = this.basicParser.parse(this.compilationUnit, new CompilationResult(this.compilationUnit, 0, 0, this.options.maxProblemsPerUnit));
			JavaModelManager.getJavaModelManager().cacheZipFiles(this); // use model only for caching
			// Use a non model name environment to avoid locks, monitors and such.
			INameEnvironment nameEnvironment = IndexBasedJavaSearchEnvironment.create(Collections.singletonList((IJavaProject)javaProject), JavaModelManager.getJavaModelManager().getWorkingCopies(DefaultWorkingCopyOwner.PRIMARY, true/*add primary WCs*/));
			this.lookupEnvironment = new LookupEnvironment(this, this.options, problemReporter, nameEnvironment);
			reduceParseTree(this.cud);
			this.lookupEnvironment.buildTypeBindings(this.cud, null);
			this.lookupEnvironment.completeTypeBindings();
			this.cud.scope.faultInTypes();
			this.cud.resolve();
		} catch (Exception e) {
			if (JobManager.VERBOSE) {
				e.printStackTrace();
			}
		} finally {
			JavaModelManager.getJavaModelManager().flushZipFiles(this);
		}
	}

	/**
	 * Called prior to the unit being resolved. Reduce the parse tree where possible.
	 */
	private void reduceParseTree(CompilationUnitDeclaration unit) {
		// remove statements from methods that have no functional interface types.
		TypeDeclaration[] types = unit.types;
		for (int i = 0, l = types == null ? 0 : types.length; i < l; i++)
			purgeMethodStatements(types[i]);
	}

	private void purgeMethodStatements(TypeDeclaration type) {
		AbstractMethodDeclaration[] methods = type.methods;
		for (int j = 0, length = methods == null ? 0 : methods.length; j < length; j++) {
			AbstractMethodDeclaration method = methods[j];
			if (method != null && (method.bits & ASTNode.HasFunctionalInterfaceTypes) == 0) {
				method.statements = null;
				method.javadoc = null;
			}
		}

		TypeDeclaration[] memberTypes = type.memberTypes;
		if (memberTypes != null)
			for (int i = 0, l = memberTypes.length; i < l; i++)
				purgeMethodStatements(memberTypes[i]);
	}

	@Override
	public void indexResolvedDocument() {
		try {
			if (DEBUG) System.out.println(new String(this.cud.compilationResult.fileName) + ':');
			for (int i = 0, length = this.cud.functionalExpressionsCount; i < length; i++) {
				FunctionalExpression expression = this.cud.functionalExpressions[i];
				if (expression instanceof LambdaExpression) {
					LambdaExpression lambdaExpression = (LambdaExpression) expression;
					if (lambdaExpression.binding != null && lambdaExpression.binding.isValidBinding()) {
						final char[] superinterface = lambdaExpression.resolvedType.sourceName();
						if (DEBUG) {
							System.out.println('\t' + new String(superinterface) + '.' +
									new String(lambdaExpression.descriptor.selector) + "-> {}"); //$NON-NLS-1$
						}
						SourceIndexer.this.addIndexEntry(IIndexConstants.METHOD_DECL, MethodPattern.createIndexKey(lambdaExpression.descriptor.selector, lambdaExpression.descriptor.parameters.length));

						addClassDeclaration(0,  // most entries are blank, that is fine, since lambda type/method cannot be searched.
								CharOperation.NO_CHAR, // package name
								ONE_ZERO,
								ONE_ZERO_CHAR, // enclosing types.
								CharOperation.NO_CHAR, // super class
								new char[][] { superinterface },
								CharOperation.NO_CHAR_CHAR,
								true); // not primary.

					} else {
						if (DEBUG) System.out.println("\tnull/bad binding in lambda"); //$NON-NLS-1$
					}
				} else {
					ReferenceExpression referenceExpression = (ReferenceExpression) expression;
					if (referenceExpression.isArrayConstructorReference())
						continue;
					MethodBinding binding = referenceExpression.getMethodBinding();
					if (binding != null && binding.isValidBinding()) {
						if (DEBUG) {
							System.out.println('\t' + new String(referenceExpression.resolvedType.sourceName()) + "::"  //$NON-NLS-1$
									+ new String(referenceExpression.descriptor.selector) + " == " + new String(binding.declaringClass.sourceName()) + '.' + //$NON-NLS-1$
									new String(binding.selector));
						}
						if (referenceExpression.isMethodReference())
							SourceIndexer.this.addMethodReference(binding.selector, binding.parameters.length);
						else
							SourceIndexer.this.addConstructorReference(binding.declaringClass.sourceName(), binding.parameters.length);
					} else {
						if (DEBUG) System.out.println("\tnull/bad binding in reference expression"); //$NON-NLS-1$
					}
				}
			}
		} catch (Exception e) {
			if (JobManager.VERBOSE) {
				e.printStackTrace();
			}
		}
	}
}
