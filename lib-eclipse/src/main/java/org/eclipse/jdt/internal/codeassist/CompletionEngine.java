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
 *     Timo Kinnunen - Contributions for bug 377373 - [subwords] known limitations with JDT 3.8
 *     							Bug 420953 - [subwords] Constructors that don't match prefix not found
 *     IBM Corporation - initial API and implementation
 *     Stephan Herrmann - Contribution for
 *								Bug 400874 - [1.8][compiler] Inference infrastructure should evolve to meet JLS8 18.x (Part G of JSR335 spec)
 *     Gábor Kövesdán - Contribution for Bug 350000 - [content assist] Include non-prefix matches in auto-complete suggestions
 *     Microsoft Corporation - Contribution for bug 575562 - improve completion search performance
 *******************************************************************************/
package org.eclipse.jdt.internal.codeassist;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.CompletionContext;
import org.eclipse.jdt.core.CompletionFlags;
import org.eclipse.jdt.core.CompletionProposal;
import org.eclipse.jdt.core.CompletionRequestor;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IModuleDescription;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.WorkingCopyOwner;
import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;
import org.eclipse.jdt.internal.codeassist.complete.AssistNodeParentAnnotationArrayInitializer;
import org.eclipse.jdt.internal.codeassist.complete.CompletionJavadoc;
import org.eclipse.jdt.internal.codeassist.complete.CompletionNodeDetector;
import org.eclipse.jdt.internal.codeassist.complete.CompletionNodeFound;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnAnnotationOfType;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnArgumentName;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnBreakStatement;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnClassLiteralAccess;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnContinueStatement;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnExplicitConstructorCall;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnFieldName;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnFieldType;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnImportReference;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnJavadoc;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnJavadocAllocationExpression;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnJavadocFieldReference;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnJavadocMessageSend;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnJavadocModuleReference;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnJavadocParamNameReference;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnJavadocQualifiedTypeReference;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnJavadocSingleTypeReference;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnJavadocTag;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnJavadocTypeParamReference;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnKeyword;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnKeyword3;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnKeywordModuleDeclaration;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnKeywordModuleInfo;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnLocalName;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnMarkerAnnotationName;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnMemberAccess;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnMemberValueName;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnMessageSend;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnMessageSendName;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnMethodName;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnMethodReturnType;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnModuleDeclaration;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnModuleReference;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnPackageReference;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnPackageVisibilityReference;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnParameterizedQualifiedTypeReference;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnProvidesImplementationsQualifiedTypeReference;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnProvidesImplementationsSingleTypeReference;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnProvidesInterfacesQualifiedTypeReference;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnProvidesInterfacesSingleTypeReference;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnQualifiedAllocationExpression;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnQualifiedNameReference;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnQualifiedTypeReference;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnReferenceExpressionName;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnSingleNameReference;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnSingleTypeReference;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnStringLiteral;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnUsesQualifiedTypeReference;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnUsesSingleTypeReference;
import org.eclipse.jdt.internal.codeassist.complete.CompletionParser;
import org.eclipse.jdt.internal.codeassist.complete.CompletionScanner;
import org.eclipse.jdt.internal.codeassist.complete.InvalidCursorLocation;
import org.eclipse.jdt.internal.codeassist.impl.AssistParser;
import org.eclipse.jdt.internal.codeassist.impl.Engine;
import org.eclipse.jdt.internal.codeassist.impl.Keywords;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.DefaultErrorHandlingPolicies;
import org.eclipse.jdt.internal.compiler.ExtraFlags;
import org.eclipse.jdt.internal.compiler.ast.AND_AND_Expression;
import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.AbstractVariableDeclaration;
import org.eclipse.jdt.internal.compiler.ast.AllocationExpression;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.Argument;
import org.eclipse.jdt.internal.compiler.ast.ArrayInitializer;
import org.eclipse.jdt.internal.compiler.ast.ArrayReference;
import org.eclipse.jdt.internal.compiler.ast.AssertStatement;
import org.eclipse.jdt.internal.compiler.ast.Assignment;
import org.eclipse.jdt.internal.compiler.ast.BinaryExpression;
import org.eclipse.jdt.internal.compiler.ast.Block;
import org.eclipse.jdt.internal.compiler.ast.CaseStatement;
import org.eclipse.jdt.internal.compiler.ast.CastExpression;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ConditionalExpression;
import org.eclipse.jdt.internal.compiler.ast.ConstructorDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.ExpressionContext;
import org.eclipse.jdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.jdt.internal.compiler.ast.FieldReference;
import org.eclipse.jdt.internal.compiler.ast.ForStatement;
import org.eclipse.jdt.internal.compiler.ast.IfStatement;
import org.eclipse.jdt.internal.compiler.ast.ImportReference;
import org.eclipse.jdt.internal.compiler.ast.Initializer;
import org.eclipse.jdt.internal.compiler.ast.InstanceOfExpression;
import org.eclipse.jdt.internal.compiler.ast.Javadoc;
import org.eclipse.jdt.internal.compiler.ast.JavadocImplicitTypeReference;
import org.eclipse.jdt.internal.compiler.ast.JavadocModuleReference;
import org.eclipse.jdt.internal.compiler.ast.JavadocQualifiedTypeReference;
import org.eclipse.jdt.internal.compiler.ast.JavadocSingleTypeReference;
import org.eclipse.jdt.internal.compiler.ast.LambdaExpression;
import org.eclipse.jdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.jdt.internal.compiler.ast.MemberValuePair;
import org.eclipse.jdt.internal.compiler.ast.MessageSend;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ModuleDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ModuleReference;
import org.eclipse.jdt.internal.compiler.ast.NormalAnnotation;
import org.eclipse.jdt.internal.compiler.ast.OperatorExpression;
import org.eclipse.jdt.internal.compiler.ast.OperatorIds;
import org.eclipse.jdt.internal.compiler.ast.PackageVisibilityStatement;
import org.eclipse.jdt.internal.compiler.ast.ParameterizedQualifiedTypeReference;
import org.eclipse.jdt.internal.compiler.ast.ParameterizedSingleTypeReference;
import org.eclipse.jdt.internal.compiler.ast.ProvidesStatement;
import org.eclipse.jdt.internal.compiler.ast.QualifiedAllocationExpression;
import org.eclipse.jdt.internal.compiler.ast.QualifiedNameReference;
import org.eclipse.jdt.internal.compiler.ast.QualifiedTypeReference;
import org.eclipse.jdt.internal.compiler.ast.ReferenceExpression;
import org.eclipse.jdt.internal.compiler.ast.RequiresStatement;
import org.eclipse.jdt.internal.compiler.ast.ReturnStatement;
import org.eclipse.jdt.internal.compiler.ast.SingleNameReference;
import org.eclipse.jdt.internal.compiler.ast.SingleTypeReference;
import org.eclipse.jdt.internal.compiler.ast.Statement;
import org.eclipse.jdt.internal.compiler.ast.SuperReference;
import org.eclipse.jdt.internal.compiler.ast.SwitchStatement;
import org.eclipse.jdt.internal.compiler.ast.ThisReference;
import org.eclipse.jdt.internal.compiler.ast.TryStatement;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeParameter;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.eclipse.jdt.internal.compiler.ast.UnaryExpression;
import org.eclipse.jdt.internal.compiler.ast.UnionTypeReference;
import org.eclipse.jdt.internal.compiler.ast.UsesStatement;
import org.eclipse.jdt.internal.compiler.ast.WhileStatement;
import org.eclipse.jdt.internal.compiler.ast.Wildcard;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.env.AccessRestriction;
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.jdt.internal.compiler.env.INameEnvironment;
import org.eclipse.jdt.internal.compiler.env.ISourceType;
import org.eclipse.jdt.internal.compiler.env.NameEnvironmentAnswer;
import org.eclipse.jdt.internal.compiler.impl.ReferenceContext;
import org.eclipse.jdt.internal.compiler.lookup.ArrayBinding;
import org.eclipse.jdt.internal.compiler.lookup.BaseTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.Binding;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.CompilationUnitScope;
import org.eclipse.jdt.internal.compiler.lookup.ExtraCompilerModifiers;
import org.eclipse.jdt.internal.compiler.lookup.FieldBinding;
import org.eclipse.jdt.internal.compiler.lookup.ImportBinding;
import org.eclipse.jdt.internal.compiler.lookup.InferenceContext18;
import org.eclipse.jdt.internal.compiler.lookup.InvocationSite;
import org.eclipse.jdt.internal.compiler.lookup.LocalVariableBinding;
import org.eclipse.jdt.internal.compiler.lookup.LookupEnvironment;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.MethodScope;
import org.eclipse.jdt.internal.compiler.lookup.ModuleBinding;
import org.eclipse.jdt.internal.compiler.lookup.PackageBinding;
import org.eclipse.jdt.internal.compiler.lookup.ParameterizedMethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.ParameterizedTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.ProblemMethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.ProblemReasons;
import org.eclipse.jdt.internal.compiler.lookup.ProblemReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.Scope;
import org.eclipse.jdt.internal.compiler.lookup.SourceTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.TagBits;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeConstants;
import org.eclipse.jdt.internal.compiler.lookup.TypeIds;
import org.eclipse.jdt.internal.compiler.lookup.TypeVariableBinding;
import org.eclipse.jdt.internal.compiler.lookup.VariableBinding;
import org.eclipse.jdt.internal.compiler.lookup.WildcardBinding;
import org.eclipse.jdt.internal.compiler.parser.JavadocTagConstants;
import org.eclipse.jdt.internal.compiler.parser.ScannerHelper;
import org.eclipse.jdt.internal.compiler.parser.SourceTypeConverter;
import org.eclipse.jdt.internal.compiler.parser.TerminalTokens;
import org.eclipse.jdt.internal.compiler.problem.AbortCompilation;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblemFactory;
import org.eclipse.jdt.internal.compiler.problem.ProblemReporter;
import org.eclipse.jdt.internal.compiler.problem.ProblemSeverities;
import org.eclipse.jdt.internal.compiler.util.HashtableOfObject;
import org.eclipse.jdt.internal.compiler.util.ObjectVector;
import org.eclipse.jdt.internal.compiler.util.SimpleSetOfCharArray;
import org.eclipse.jdt.internal.compiler.util.SuffixConstants;
import org.eclipse.jdt.internal.core.BasicCompilationUnit;
import org.eclipse.jdt.internal.core.BinaryTypeConverter;
import org.eclipse.jdt.internal.core.INamingRequestor;
import org.eclipse.jdt.internal.core.InternalNamingConventions;
import org.eclipse.jdt.internal.core.JarPackageFragmentRoot;
import org.eclipse.jdt.internal.core.JavaElementRequestor;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.jdt.internal.core.ModuleSourcePathManager;
import org.eclipse.jdt.internal.core.SearchableEnvironment;
import org.eclipse.jdt.internal.core.SourceMethod;
import org.eclipse.jdt.internal.core.SourceMethodElementInfo;
import org.eclipse.jdt.internal.core.SourceType;
import org.eclipse.jdt.internal.core.SourceTypeElementInfo;
import org.eclipse.jdt.internal.core.search.BasicSearchEngine;
import org.eclipse.jdt.internal.core.search.matching.IndexBasedJavaSearchEnvironment;
import org.eclipse.jdt.internal.core.util.Messages;
import org.eclipse.jdt.internal.core.util.Util;

/**
 * This class is the entry point for source completions.
 * It contains two public APIs used to call CodeAssist on a given source with
 * a given environment, assisting position and storage (and possibly options).
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public final class CompletionEngine
	extends Engine
	implements ISearchRequestor, TypeConstants , TerminalTokens , RelevanceConstants, SuffixConstants {

	private static class AcceptedConstructor {
		public int modifiers;
		public char[] simpleTypeName;
		public int parameterCount;
		public char[] signature;
		public char[][] parameterTypes;
		public char[][] parameterNames;
		public int typeModifiers;
		public char[] packageName;
		public int extraFlags;
		public int accessibility;
		public boolean proposeType = false;
		public boolean proposeConstructor = false;
		public char[] fullyQualifiedName = null;

		public boolean mustBeQualified = false;

		public AcceptedConstructor(
				int modifiers,
				char[] simpleTypeName,
				int parameterCount,
				char[] signature,
				char[][] parameterTypes,
				char[][] parameterNames,
				int typeModifiers,
				char[] packageName,
				int extraFlags,
				int accessibility) {
			this.modifiers = modifiers;
			this.simpleTypeName = simpleTypeName;
			this.parameterCount = parameterCount;
			this.signature = signature;
			this.parameterTypes = parameterTypes;
			this.parameterNames = parameterNames;
			this.typeModifiers = typeModifiers;
			this.packageName = packageName;
			this.extraFlags = extraFlags;
			this.accessibility = accessibility;
		}

		@Override
		public String toString() {
			StringBuilder buffer = new StringBuilder();
			buffer.append('{');
			buffer.append(this.packageName);
			buffer.append(',');
			buffer.append(this.simpleTypeName);
			buffer.append('}');
			return buffer.toString();
		}
	}

	private static class AcceptedType {
		public char[] packageName;
		public char[] simpleTypeName;
		public char[][] enclosingTypeNames;
		public int modifiers;
		public int accessibility;
		public boolean mustBeQualified = false;

		public char[] fullyQualifiedName = null;
		public char[] qualifiedTypeName = null;
		public AcceptedType(
			char[] packageName,
			char[] simpleTypeName,
			char[][] enclosingTypeNames,
			int modifiers,
			int accessibility) {
			this.packageName = packageName;
			this.simpleTypeName = simpleTypeName;
			this.enclosingTypeNames = enclosingTypeNames;
			this.modifiers = modifiers;
			this.accessibility = accessibility;
		}

		@Override
		public String toString() {
			StringBuilder buffer = new StringBuilder();
			buffer.append('{');
			buffer.append(this.packageName);
			buffer.append(',');
			buffer.append(this.simpleTypeName);
			buffer.append(',');
			buffer.append(CharOperation.concatWith(this.enclosingTypeNames, '.'));
			buffer.append('}');
			return buffer.toString();
		}
	}

	public class CompletionProblemFactory extends DefaultProblemFactory {
		private int lastErrorStart;

		private boolean checkProblems = false;
		public boolean hasForbiddenProblems = false;
		public boolean hasAllowedProblems = false;

		public CompletionProblemFactory(Locale loc) {
			super(loc);
		}

		private CategorizedProblem checkProblem(CategorizedProblem pb,
			char[] originatingFileName,	int severity, int start) {
			int id = pb.getID();
			if (CompletionEngine.this.actualCompletionPosition > start
				&& this.lastErrorStart < start
				&& pb.isError()
				&& (id & IProblem.Syntax) == 0
				&& (CompletionEngine.this.fileName == null || CharOperation.equals(CompletionEngine.this.fileName, originatingFileName))) {

				CompletionEngine.this.problem = pb;
				this.lastErrorStart = start;
			}
			if (this.checkProblems && !this.hasForbiddenProblems) {
				switch (id) {
					case IProblem.UsingDeprecatedType:
						this.hasForbiddenProblems =
							CompletionEngine.this.options.checkDeprecation;
						break;
					case IProblem.NotVisibleType:
						this.hasForbiddenProblems =
							CompletionEngine.this.options.checkVisibility;
						break;
					case IProblem.ForbiddenReference:
						this.hasForbiddenProblems =
							CompletionEngine.this.options.checkForbiddenReference;
						break;
					case IProblem.DiscouragedReference:
						this.hasForbiddenProblems =
							CompletionEngine.this.options.checkDiscouragedReference;
						break;
					default:
						if ((severity & ProblemSeverities.Optional) != 0) {
							this.hasAllowedProblems = true;
						} else {
							this.hasForbiddenProblems = true;
						}

						break;
				}
			}

			return pb;
		}

		@Override
		public CategorizedProblem createProblem(
				char[] originatingFileName,
				int problemId,
				String[] problemArguments,
				int elaborationId,
				String[] messageArguments,
				int severity,
				int start,
				int end,
				int lineNumber,
				int columnNumber) {
				return checkProblem(
					super.createProblem(
						originatingFileName,
						problemId,
						problemArguments,
						elaborationId,
						messageArguments,
						severity,
						start,
						end,
						lineNumber,
						columnNumber), originatingFileName, severity, start);
		}

		@Override
		public CategorizedProblem createProblem(
				char[] originatingFileName,
				int problemId,
				String[] problemArguments,
				String[] messageArguments,
				int severity,
				int start,
				int end,
				int lineNumber,
				int columnNumber) {
				return checkProblem(
					super.createProblem(
						originatingFileName,
						problemId,
						problemArguments,
						messageArguments,
						severity,
						start,
						end,
						lineNumber,
						columnNumber), originatingFileName, severity, start);
		}

		public void startCheckingProblems() {
			this.checkProblems = true;
			this.hasForbiddenProblems = false;
			this.hasAllowedProblems = false;
		}

		public void stopCheckingProblems() {
			this.checkProblems = false;
		}
	}

	public static char[] createBindingKey(char[] packageName, char[] typeName) {
		char[] signature = createTypeSignature(packageName, typeName);
		CharOperation.replace(signature, '.', '/');
		return signature;
	}

	public static char[][] createDefaultParameterNames(int length) {
		char[][] parameters;
		switch (length) {
			case 0 :
				parameters = new char[length][];
				break;
			case 1 :
				parameters = ARGS1;
				break;
			case 2 :
				parameters = ARGS2;
				break;
			case 3 :
				parameters = ARGS3;
				break;
			case 4 :
				parameters = ARGS4;
				break;
			default :
				parameters = new char[length][];
				for (int i = 0; i < length; i++) {
					parameters[i] = CharOperation.concat(ARG, String.valueOf(i).toCharArray());
				}
				break;
		}
		return parameters;
	}
	public static char[] createMethodSignature(char[][] parameterPackageNames, char[][] parameterTypeNames, char[] returnTypeSignature) {
		char[][] parameterTypeSignature = new char[parameterTypeNames.length][];
		for (int i = 0; i < parameterTypeSignature.length; i++) {
			parameterTypeSignature[i] =
				Signature.createCharArrayTypeSignature(
						CharOperation.concat(
								parameterPackageNames[i],
								CharOperation.replaceOnCopy(parameterTypeNames[i], '.', '$'), '.'), true);
		}

		return Signature.createMethodSignature(
				parameterTypeSignature,
				returnTypeSignature);
	}

	public static char[] createMethodSignature(char[][] parameterPackageNames, char[][] parameterTypeNames, char[] returnPackagename, char[] returnTypeName) {
		char[] returnTypeSignature =
			returnTypeName == null || returnTypeName.length == 0
			? Signature.createCharArrayTypeSignature(VOID, true)
			: Signature.createCharArrayTypeSignature(
					CharOperation.concat(
							returnPackagename,
							CharOperation.replaceOnCopy(returnTypeName, '.', '$'), '.'), true);

		return createMethodSignature(
				parameterPackageNames,
				parameterTypeNames,
				returnTypeSignature);
	}
	public static char[] createNonGenericTypeSignature(char[] qualifiedPackageName, char[] qualifiedTypeName) {
		return Signature.createCharArrayTypeSignature(
				CharOperation.concat(
						qualifiedPackageName,
						CharOperation.replaceOnCopy(qualifiedTypeName, '.', '$'), '.'), true);
	}

	public static char[] createTypeSignature(char[] qualifiedPackageName, char[] qualifiedTypeName) {
		char[] name = new char[qualifiedTypeName.length];
		System.arraycopy(qualifiedTypeName, 0, name, 0, qualifiedTypeName.length);

		int depth = 0;
		int length = name.length;
		for (int i = length -1; i >= 0; i--) {
			switch (name[i]) {
				case '.':
					if (depth == 0 && name[i - 1] != '>') {
						name[i] = '$';
					}
					break;
				case '<':
					depth--;
					break;
				case '>':
					depth++;
					break;
			}
		}
		return Signature.createCharArrayTypeSignature(
				CharOperation.concat(
						qualifiedPackageName,
						name, '.'), true);
	}

	private static char[] getRequiredTypeSignature(TypeBinding typeBinding) {
		char[] result = null;
		StringBuilder sig = new StringBuilder(10);

		sig.append(typeBinding.signature());

		int sigLength = sig.length();
		result = new char[sigLength];
		sig.getChars(0, sigLength, result, 0);
		result = CharOperation.replaceOnCopy(result, '/', '.');
		return result;
	}

	private static char[] getTypeName(TypeReference typeReference) {
		char[] typeName = CharOperation.concatWith(typeReference.getTypeName(), '.');
		int dims = typeReference.dimensions();
		if (dims > 0) {
			int length = typeName.length;
			int newLength = length + (dims*2);
			System.arraycopy(typeName, 0, typeName = new char[newLength], 0, length);
			for (int k = length; k < newLength; k += 2) {
				typeName[k] = '[';
				typeName[k+1] = ']';
			}
		}

		return typeName;
	}

	private static boolean hasStaticMemberTypes(ReferenceBinding typeBinding, SourceTypeBinding invocationType, CompilationUnitScope unitScope) {
		ReferenceBinding[] memberTypes = typeBinding.memberTypes();
		int length = memberTypes == null ? 0 : memberTypes.length;
		next : for (int i = 0; i < length; i++) {
			ReferenceBinding memberType = memberTypes[i];
			if (invocationType != null && !memberType.canBeSeenBy(typeBinding, invocationType)) {
				continue next;
			} else if(invocationType == null && !memberType.canBeSeenBy(unitScope.fPackage)) {
				continue next;
			}

			if ((memberType.modifiers & ClassFileConstants.AccStatic) != 0) {
				return true;
			}
		}
		return false;
	}

	private static boolean hasMemberTypesInEnclosingScope(SourceTypeBinding typeBinding, Scope scope) {
		ReferenceBinding[] memberTypes = typeBinding.memberTypes();
		int length = memberTypes == null ? 0 : memberTypes.length;

		if (length > 0) {
			MethodScope methodScope = scope.methodScope();
			if (methodScope != null && !methodScope.isStatic) {
				ClassScope classScope = typeBinding.scope;
				Scope currentScope = scope;
				while (currentScope != null) {
					if (currentScope == classScope) {
						return true;
					}
					currentScope = currentScope.parent;
				}
			}
		}
		return false;
	}

	public HashtableOfObject typeCache;
	public int openedBinaryTypes; // used during InternalCompletionProposal#findConstructorParameterNames()

	public static boolean DEBUG = false;
	public static boolean PERF = false;

	private static final char[] KNOWN_TYPE_WITH_UNKNOWN_CONSTRUCTORS = new char[]{};
	private static final char[] KNOWN_TYPE_WITH_KNOWN_CONSTRUCTORS = new char[]{};

	private static final char[] ARG = "arg".toCharArray();  //$NON-NLS-1$
	private static final char[] ARG0 = "arg0".toCharArray();  //$NON-NLS-1$
	private static final char[] ARG1 = "arg1".toCharArray();  //$NON-NLS-1$
	private static final char[] ARG2 = "arg2".toCharArray();  //$NON-NLS-1$
	private static final char[] ARG3 = "arg3".toCharArray();  //$NON-NLS-1$
	private static final char[][] ARGS1 = new char[][]{ARG0};
	private static final char[][] ARGS2 = new char[][]{ARG0, ARG1};
	private static final char[][] ARGS3 = new char[][]{ARG0, ARG1, ARG2};
	private static final char[][] ARGS4 = new char[][]{ARG0, ARG1, ARG2, ARG3};

	private final static int CHECK_CANCEL_FREQUENCY = 50;

	// temporary constants to quickly disabled polish features if necessary
	public final static boolean NO_TYPE_COMPLETION_ON_EMPTY_TOKEN = false;

	private final static char[] ERROR_PATTERN = "*error*".toCharArray();  //$NON-NLS-1$
	private final static char[] EXCEPTION_PATTERN = "*exception*".toCharArray();  //$NON-NLS-1$
	private final static char[] SEMICOLON = new char[] { ';' };

	private final static char[] CLASS = "Class".toCharArray();  //$NON-NLS-1$
	private final static char[] VOID = "void".toCharArray();  //$NON-NLS-1$
	private final static char[] INT = "int".toCharArray();  //$NON-NLS-1$
	private final static char[] INT_SIGNATURE = new char[]{Signature.C_INT};
	private final static char[] VALUE = "value".toCharArray();  //$NON-NLS-1$
	private final static char[] EXTENDS = "extends".toCharArray();  //$NON-NLS-1$
	private final static char[] SUPER = "super".toCharArray();  //$NON-NLS-1$
	private final static char[] DEFAULT_CONSTRUCTOR_SIGNATURE = "()V".toCharArray();  //$NON-NLS-1$

	private final static char[] DOT = ".".toCharArray();  //$NON-NLS-1$

	private final static char[] VARARGS = "...".toCharArray();  //$NON-NLS-1$

	private final static char[] IMPORT = "import".toCharArray();  //$NON-NLS-1$
	private final static char[] STATIC = "static".toCharArray();  //$NON-NLS-1$
	private final static char[] ON_DEMAND = ".*".toCharArray();  //$NON-NLS-1$
	private final static char[] IMPORT_END = ";\n".toCharArray();  //$NON-NLS-1$
	private final static char[] LAMBDA = "->".toCharArray(); //$NON-NLS-1$

	private final static char[] JAVA_LANG_OBJECT_SIGNATURE =
		createTypeSignature(CharOperation.concatWith(JAVA_LANG, '.'), OBJECT);
	private final static char[] JAVA_LANG_NAME =
		CharOperation.concatWith(JAVA_LANG, '.');

	private final static int NONE = 0;
	private final static int SUPERTYPE = 1;
	private final static int SUBTYPE = 2;

	int expectedTypesPtr = -1;
	TypeBinding[] expectedTypes = new TypeBinding[1];
	int expectedTypesFilter;
	boolean hasJavaLangObjectAsExpectedType = false;
	boolean hasExpectedArrayTypes = false;
	boolean hasComputedExpectedArrayTypes = false;
	int uninterestingBindingsPtr = -1;
	Binding[] uninterestingBindings = new Binding[1];
	int forbbidenBindingsPtr = -1;
	Binding[] forbbidenBindings = new Binding[1];
	int uninterestingBindingsFilter;     // only set when completing on an exception type

	ImportBinding[] favoriteReferenceBindings;

	boolean assistNodeIsClass;
	boolean assistNodeIsEnum;
	boolean assistNodeIsException;
	boolean assistNodeIsInterface;
	boolean assistNodeIsAnnotation;
	boolean assistNodeIsConstructor;
	boolean assistNodeIsSuperType;
	boolean assistNodeIsExtendedType;
	boolean assistNodeIsInterfaceExcludingAnnotation; // https://bugs.eclipse.org/bugs/show_bug.cgi?id=310423
	int  assistNodeInJavadoc = 0;
	boolean assistNodeCanBeSingleMemberAnnotation = false;
	boolean assistNodeIsInsideCase = false; // https://bugs.eclipse.org/bugs/show_bug.cgi?id=195346
	boolean assistNodeIsString = false;	// https://bugs.eclipse.org/bugs/show_bug.cgi?id=343476

	long targetedElement;

	WorkingCopyOwner owner;
	IProgressMonitor monitor;
	IJavaProject javaProject;
	ITypeRoot typeRoot;
	CompletionParser parser;
	CompletionRequestor requestor;
	CompletionProblemFactory problemFactory;
	ProblemReporter problemReporter;
	private INameEnvironment noCacheNameEnvironment;
	char[] source;
	ModuleDeclaration moduleDeclaration;
	boolean skipDefaultPackage = false;
	char[] completionToken;

	char[] qualifiedCompletionToken;
	boolean resolvingImports = false;
	boolean resolvingStaticImports = false;
	boolean insideQualifiedReference = false;
	boolean noProposal = true;
	CategorizedProblem problem = null;
	char[] fileName = null;
	int startPosition, actualCompletionPosition, endPosition, offset;
	int tokenStart, tokenEnd;
	int javadocTagPosition; // Position of previous tag while completing in javadoc
	String sourceLevel;
	String complianceLevel;
	SimpleSetOfCharArray validPackageNames = new SimpleSetOfCharArray(10);
	SimpleSetOfCharArray invalidPackageNames = new SimpleSetOfCharArray(1);
	HashtableOfObject knownModules = new HashtableOfObject(10);
	HashtableOfObject knownPkgs = new HashtableOfObject(10);
	HashtableOfObject knownTypes = new HashtableOfObject(10);

 	/*
		static final char[][] mainDeclarations =
			new char[][] {
				"package".toCharArray(),
				"import".toCharArray(),
				"abstract".toCharArray(),
				"final".toCharArray(),
				"public".toCharArray(),
				"class".toCharArray(),
				"interface".toCharArray()};

		static final char[][] modifiers = // may want field, method, type & member type modifiers
			new char[][] {
				"abstract".toCharArray(),
				"final".toCharArray(),
				"native".toCharArray(),
				"public".toCharArray(),
				"protected".toCharArray(),
				"private".toCharArray(),
				"static".toCharArray(),
				"strictfp".toCharArray(),
				"synchronized".toCharArray(),
				"transient".toCharArray(),
				"volatile".toCharArray()};
	*/
	static final BaseTypeBinding[] BASE_TYPES = {
		TypeBinding.BOOLEAN,
		TypeBinding.BYTE,
		TypeBinding.CHAR,
		TypeBinding.DOUBLE,
		TypeBinding.FLOAT,
		TypeBinding.INT,
		TypeBinding.LONG,
		TypeBinding.SHORT,
		TypeBinding.VOID
	};
	static final int BASE_TYPES_LENGTH = BASE_TYPES.length;
	static final char[][] BASE_TYPE_NAMES = new char[BASE_TYPES_LENGTH][];
	static final int BASE_TYPES_WITHOUT_VOID_LENGTH = BASE_TYPES.length - 1;
	static final char[][] BASE_TYPE_NAMES_WITHOUT_VOID = new char[BASE_TYPES_WITHOUT_VOID_LENGTH][];
	static {
 		for (int i=0; i<BASE_TYPES_LENGTH; i++) {
 			BASE_TYPE_NAMES[i] = BASE_TYPES[i].simpleName;
 		}
		for (int i=0; i<BASE_TYPES_WITHOUT_VOID_LENGTH; i++) {
			BASE_TYPE_NAMES_WITHOUT_VOID[i] = BASE_TYPES[i].simpleName;
		}
 	}

	static final char[] classField = "class".toCharArray();  //$NON-NLS-1$
	static final char[] lengthField = "length".toCharArray();  //$NON-NLS-1$
	static final char[] cloneMethod = "clone".toCharArray();  //$NON-NLS-1$
	static final char[] THIS = "this".toCharArray();  //$NON-NLS-1$
	static final char[] THROWS = "throws".toCharArray();  //$NON-NLS-1$

	static InvocationSite FakeInvocationSite = new InvocationSite(){
		@Override
		public TypeBinding[] genericTypeArguments() { return null; }
		@Override
		public boolean isSuperAccess(){ return false; }
		@Override
		public boolean isTypeAccess(){ return false; }
		@Override
		public void setActualReceiverType(ReferenceBinding receiverType) {/* empty */}
		@Override
		public void setDepth(int depth){/* empty */}
		@Override
		public void setFieldIndex(int depth){/* empty */}
		@Override
		public int sourceEnd() { return 0; 	}
		@Override
		public int sourceStart() { return 0; 	}
		@Override
		public TypeBinding invocationTargetType() { return null; }
		@Override
		public boolean receiverIsImplicitThis() { return false; }
		@Override
		public InferenceContext18 freshInferenceContext(Scope scope) { return null; }
		@Override
		public ExpressionContext getExpressionContext() { return ExpressionContext.VANILLA_CONTEXT; }
		@Override
		public boolean isQualifiedSuper() { return false; }
		@Override
		public boolean checkingPotentialCompatibility() { return false; }
		@Override
		public void acceptPotentiallyCompatibleMethods(MethodBinding[] methods) {/* ignore */}
	};

	private int foundTypesCount;
	private ObjectVector acceptedTypes;

	private int foundConstructorsCount;
	private ObjectVector acceptedConstructors;
	/**
	 * The strictMatchForExtepectedType used to skip all elements found at <code>findVariablesAndMethods</code>
	 * which doesn't match the current expected types in the engine in <code>expectedTypes</code>. Since in this mode
	 * all elements found matches the expected type, the completion proposals will not contains the calculated expected type
	 * relevance. This is done to keep the overloaded method suggestions always on top in this mode as a fix for
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=575149
	 *
	 */
	private boolean strictMatchForExtepectedType = false;

	/**
	 * The CompletionEngine is responsible for computing source completions.
	 *
	 * It requires a searchable name environment, which supports some
	 * specific search APIs, and a requestor to feed back the results to a UI.
	 *
	 *  @param nameEnvironment org.eclipse.jdt.internal.codeassist.ISearchableNameEnvironment
	 *      used to resolve type/package references and search for types/packages
	 *      based on partial names.
	 *
	 *  @param requestor org.eclipse.jdt.internal.codeassist.ICompletionRequestor
	 *      since the engine might produce answers of various forms, the engine
	 *      is associated with a requestor able to accept all possible completions.
	 *
	 *  @param settings java.util.Map
	 *		set of options used to configure the code assist engine.
	 */
	public CompletionEngine(
			SearchableEnvironment nameEnvironment,
			CompletionRequestor requestor,
			Map settings,
			IJavaProject javaProject,
			WorkingCopyOwner owner,
			IProgressMonitor monitor) {
		super(settings);
		this.javaProject = javaProject;
		this.requestor = requestor;
		this.nameEnvironment = nameEnvironment;
		this.typeCache = new HashtableOfObject(5);
		this.openedBinaryTypes = 0;
		this.sourceLevel = javaProject.getOption(JavaCore.COMPILER_SOURCE, true);
		this.complianceLevel = javaProject.getOption(JavaCore.COMPILER_COMPLIANCE, true);

		this.problemFactory = new CompletionProblemFactory(Locale.getDefault());
		this.problemReporter = new ProblemReporter(
				DefaultErrorHandlingPolicies.proceedWithAllProblems(),
				this.compilerOptions,
				this.problemFactory);
		this.lookupEnvironment =
			new LookupEnvironment(this, this.compilerOptions, this.problemReporter, nameEnvironment);
		this.parser =
			new CompletionParser(this.problemReporter, this.requestor.isExtendedContextRequired(), monitor);
		this.owner = owner;
		this.monitor = monitor;
	}

	@Override
	public void accept(ICompilationUnit sourceUnit, AccessRestriction accessRestriction) {
		if (!CharOperation.equals(sourceUnit.getMainTypeName(), TypeConstants.PACKAGE_INFO_NAME)) {
			// do not accept package-info.java as a type for completion engine
			// because it contains no extra info that will help in completion
			// https://bugs.eclipse.org/bugs/show_bug.cgi?id=343865
			// Required after the fix for https://bugs.eclipse.org/bugs/show_bug.cgi?id=337868
			// because now we get a type corresponding to the package-info.java from the java model.
			super.accept(sourceUnit, accessRestriction);
		}
	}

	@Override
	public void acceptConstructor(
			int modifiers,
			char[] simpleTypeName,
			int parameterCount,
			char[] signature,
			char[][] parameterTypes,
			char[][] parameterNames,
			int typeModifiers,
			char[] packageName,
			int extraFlags,
			String path,
			AccessRestriction accessRestriction) {

		// does not check cancellation for every types to avoid performance loss
		if ((this.foundConstructorsCount % (CHECK_CANCEL_FREQUENCY)) == 0) checkCancel();
		this.foundConstructorsCount++;

		if ((typeModifiers & ClassFileConstants.AccEnum) != 0) return;

		if (this.options.checkDeprecation && (typeModifiers & ClassFileConstants.AccDeprecated) != 0) return;

		if (this.options.checkVisibility) {
			if((typeModifiers & ClassFileConstants.AccPublic) == 0) {
				if((typeModifiers & ClassFileConstants.AccPrivate) != 0) return;

				if (this.currentPackageName == null) {
					initializePackageCache();
				}

				if(!CharOperation.equals(packageName, this.currentPackageName)) return;
			}
		}

		int accessibility = IAccessRule.K_ACCESSIBLE;
		if(accessRestriction != null) {
			switch (accessRestriction.getProblemId()) {
				case IProblem.ForbiddenReference:
					if (this.options.checkForbiddenReference) {
						return;
					}
					accessibility = IAccessRule.K_NON_ACCESSIBLE;
					break;
				case IProblem.DiscouragedReference:
					if (this.options.checkDiscouragedReference) {
						return;
					}
					accessibility = IAccessRule.K_DISCOURAGED;
					break;
			}
		}

		if(this.acceptedConstructors == null) {
			this.acceptedConstructors = new ObjectVector();
		}
		this.acceptedConstructors.add(
				new AcceptedConstructor(
						modifiers,
						simpleTypeName,
						parameterCount,
						signature,
						parameterTypes,
						parameterNames,
						typeModifiers,
						packageName,
						extraFlags,
						accessibility));
	}

	private void acceptConstructors(Scope scope) {
		final boolean DEFER_QUALIFIED_PROPOSALS = false;

		this.checkCancel();

		if(this.acceptedConstructors == null) return;

		int length = this.acceptedConstructors.size();

		if(length == 0) return;

		HashtableOfObject onDemandFound = new HashtableOfObject();

		ArrayList deferredProposals = null;
		if (DEFER_QUALIFIED_PROPOSALS) {
			deferredProposals = new ArrayList();
		}

		try {
			next : for (int i = 0; i < length; i++) {

				// does not check cancellation for every types to avoid performance loss
				if ((i % CHECK_CANCEL_FREQUENCY) == 0) checkCancel();

				AcceptedConstructor acceptedConstructor = (AcceptedConstructor)this.acceptedConstructors.elementAt(i);
				final int typeModifiers = acceptedConstructor.typeModifiers;
				final char[] packageName = acceptedConstructor.packageName;
				final char[] simpleTypeName = acceptedConstructor.simpleTypeName;
				final int modifiers = acceptedConstructor.modifiers;
				final int parameterCount = acceptedConstructor.parameterCount;
				final char[] signature = acceptedConstructor.signature;
				final char[][] parameterTypes = acceptedConstructor.parameterTypes;
				final char[][] parameterNames = acceptedConstructor.parameterNames;
				final int extraFlags = acceptedConstructor.extraFlags;
				final int accessibility = acceptedConstructor.accessibility;

				boolean proposeType = hasArrayTypeAsExpectedSuperTypes() || (extraFlags & ExtraFlags.HasNonPrivateStaticMemberTypes) != 0;

				char[] fullyQualifiedName = CharOperation.concat(packageName, simpleTypeName, '.');

				Object knownTypeKind = this.knownTypes.get(fullyQualifiedName);
				if (knownTypeKind != null) {
					if (knownTypeKind == KNOWN_TYPE_WITH_KNOWN_CONSTRUCTORS) {
						// the type and its constructors are already accepted
						continue next;
					}
					// this type is already accepted
					proposeType = false;
				} else {
					this.knownTypes.put(fullyQualifiedName, KNOWN_TYPE_WITH_UNKNOWN_CONSTRUCTORS);
				}

				boolean proposeConstructor = true;

				if (this.options.checkVisibility) {
					if((modifiers & ClassFileConstants.AccPublic) == 0) {
						if((modifiers & ClassFileConstants.AccPrivate) != 0) {
							if (!proposeType) continue next;
							proposeConstructor = false;
						} else {
							if (this.currentPackageName == null) {
								initializePackageCache();
							}

							if(!CharOperation.equals(packageName, this.currentPackageName)) {

								if((typeModifiers & ClassFileConstants.AccAbstract) == 0 ||
										(modifiers & ClassFileConstants.AccProtected) == 0) {
									if (!proposeType) continue next;
									proposeConstructor = false;
								}
							}
						}
					}
				}

				acceptedConstructor.fullyQualifiedName = fullyQualifiedName;
				acceptedConstructor.proposeType = proposeType;
				acceptedConstructor.proposeConstructor = proposeConstructor;


				if(!this.importCachesInitialized) {
					initializeImportCaches();
				}

				for (int j = 0; j < this.importCacheCount; j++) {
					char[][] importName = this.importsCache[j];
					if(CharOperation.equals(simpleTypeName, importName[0])) {
						if (proposeType) {
							proposeType(
									packageName,
									simpleTypeName,
									typeModifiers,
									accessibility,
									simpleTypeName,
									fullyQualifiedName,
									!CharOperation.equals(fullyQualifiedName, importName[1]),
									scope);
						}

						if (proposeConstructor && !Flags.isEnum(typeModifiers)) {
							boolean isQualified = !CharOperation.equals(fullyQualifiedName, importName[1]);
							if (!isQualified) {
								proposeConstructor(
										simpleTypeName,
										parameterCount,
										signature,
										parameterTypes,
										parameterNames,
										modifiers,
										packageName,
										typeModifiers,
										accessibility,
										simpleTypeName,
										fullyQualifiedName,
										isQualified,
										scope,
										extraFlags);
							} else {
								acceptedConstructor.mustBeQualified = true;
								if (DEFER_QUALIFIED_PROPOSALS) {
									deferredProposals.add(acceptedConstructor);
								} else {
									proposeConstructor(acceptedConstructor, scope);
								}
							}
						}
						continue next;
					}
				}


				if (CharOperation.equals(this.currentPackageName, packageName)) {
					if (proposeType) {
						proposeType(
								packageName,
								simpleTypeName,
								typeModifiers,
								accessibility,
								simpleTypeName,
								fullyQualifiedName,
								false,
								scope);
					}

					if (proposeConstructor && !Flags.isEnum(typeModifiers)) {
						proposeConstructor(
								simpleTypeName,
								parameterCount,
								signature,
								parameterTypes,
								parameterNames,
								modifiers,
								packageName,
								typeModifiers,
								accessibility,
								simpleTypeName,
								fullyQualifiedName,
								false,
								scope,
								extraFlags);
					}
					continue next;
				} else {
					char[] fullyQualifiedEnclosingTypeOrPackageName = null;

					AcceptedConstructor foundConstructor = null;
					if((foundConstructor = (AcceptedConstructor)onDemandFound.get(simpleTypeName)) == null) {
						for (int j = 0; j < this.onDemandImportCacheCount; j++) {
							ImportBinding importBinding = this.onDemandImportsCache[j];

							char[][] importName = importBinding.compoundName;
							char[] importFlatName = CharOperation.concatWith(importName, '.');

							if(fullyQualifiedEnclosingTypeOrPackageName == null) {
								fullyQualifiedEnclosingTypeOrPackageName = packageName;
							}
							if(CharOperation.equals(fullyQualifiedEnclosingTypeOrPackageName, importFlatName)) {
								if(importBinding.isStatic()) {
									if((typeModifiers & ClassFileConstants.AccStatic) != 0) {
										onDemandFound.put(
												simpleTypeName,
												acceptedConstructor);
										continue next;
									}
								} else {
									onDemandFound.put(
											simpleTypeName,
											acceptedConstructor);
									continue next;
								}
							}
						}
					} else if(!foundConstructor.mustBeQualified){
						done : for (int j = 0; j < this.onDemandImportCacheCount; j++) {
							ImportBinding importBinding = this.onDemandImportsCache[j];

							char[][] importName = importBinding.compoundName;
							char[] importFlatName = CharOperation.concatWith(importName, '.');

							if(fullyQualifiedEnclosingTypeOrPackageName == null) {
								fullyQualifiedEnclosingTypeOrPackageName = packageName;
							}
							if(CharOperation.equals(fullyQualifiedEnclosingTypeOrPackageName, importFlatName)) {
								if(importBinding.isStatic()) {
									if((typeModifiers & ClassFileConstants.AccStatic) != 0) {
										foundConstructor.mustBeQualified = true;
										break done;
									}
								} else {
									foundConstructor.mustBeQualified = true;
									break done;
								}
							}
						}
					}
					if (proposeType) {
						proposeType(
								packageName,
								simpleTypeName,
								typeModifiers,
								accessibility,
								simpleTypeName,
								fullyQualifiedName,
								true,
								scope);
					}

					if (proposeConstructor && !Flags.isEnum(typeModifiers)) {
						acceptedConstructor.mustBeQualified = true;
						if (DEFER_QUALIFIED_PROPOSALS) {
							deferredProposals.add(acceptedConstructor);
						} else {
							proposeConstructor(acceptedConstructor, scope);
						}
					}
				}
			}

			char[][] keys = onDemandFound.keyTable;
			Object[] values = onDemandFound.valueTable;
			int max = keys.length;
			for (int i = 0; i < max; i++) {

				// does not check cancellation for every types to avoid performance loss
				if ((i % CHECK_CANCEL_FREQUENCY) == 0) checkCancel();

				if(keys[i] != null) {
					AcceptedConstructor value = (AcceptedConstructor) values[i];
					if(value != null) {
						char[] fullyQualifiedEnclosingTypeOrPackageName = null;
						done : for (int j = 0; j < this.onDemandImportCacheCount; j++) {
							ImportBinding importBinding = this.onDemandImportsCache[j];

							char[][] importName = importBinding.compoundName;
							char[] importFlatName = CharOperation.concatWith(importName, '.');

							if(fullyQualifiedEnclosingTypeOrPackageName == null) {
								fullyQualifiedEnclosingTypeOrPackageName = value.packageName;
							}
							if(CharOperation.equals(fullyQualifiedEnclosingTypeOrPackageName, importFlatName)) {
								if(importBinding.isStatic()) {
									if((value.modifiers & ClassFileConstants.AccStatic) != 0) {
										value.mustBeQualified = true;
										break done;
									}
								} else {
									value.mustBeQualified = true;
									break done;
								}
							}
						}
						if (value.proposeType) {
							proposeType(
									value.packageName,
									value.simpleTypeName,
									value.typeModifiers,
									value.accessibility,
									value.simpleTypeName,
									value.fullyQualifiedName,
									value.mustBeQualified,
									scope);
						}

						if (value.proposeConstructor && !Flags.isEnum(value.modifiers)) {
							if (!value.mustBeQualified) {
								proposeConstructor(
										value.simpleTypeName,
										value.parameterCount,
										value.signature,
										value.parameterTypes,
										value.parameterNames,
										value.modifiers,
										value.packageName,
										value.typeModifiers,
										value.accessibility,
										value.simpleTypeName,
										value.fullyQualifiedName,
										value.mustBeQualified,
										scope,
										value.extraFlags);
							} else {
								if (DEFER_QUALIFIED_PROPOSALS) {
									deferredProposals.add(value);
								} else {
									proposeConstructor(value, scope);
								}
							}
						}
					}
				}
			}

			if (DEFER_QUALIFIED_PROPOSALS) {
				int size = deferredProposals.size();
				for (int i = 0; i < size; i++) {

					// does not check cancellation for every types to avoid performance loss
					if ((i % CHECK_CANCEL_FREQUENCY) == 0) checkCancel();

					AcceptedConstructor deferredProposal = (AcceptedConstructor)deferredProposals.get(i);

					if (deferredProposal.proposeConstructor) {
						proposeConstructor(
								deferredProposal.simpleTypeName,
								deferredProposal.parameterCount,
								deferredProposal.signature,
								deferredProposal.parameterTypes,
								deferredProposal.parameterNames,
								deferredProposal.modifiers,
								deferredProposal.packageName,
								deferredProposal.typeModifiers,
								deferredProposal.accessibility,
								deferredProposal.simpleTypeName,
								deferredProposal.fullyQualifiedName,
								deferredProposal.mustBeQualified,
								scope,
								deferredProposal.extraFlags);
					}
				}
			}
		} finally {
			this.acceptedTypes = null; // reset
		}
	}

	/**
	 * One result of the search consists of a new module.
	 *
	 * NOTE - All module names are presented in their readable form:
	 *    Module names are in the form "a.b.c".
	 *    The default module is represented by an empty array.
	 */
	@Override
	public void acceptModule(char[] moduleName) {
		if (this.knownModules.containsKey(moduleName)) return;
		if (this.assistNodeInJavadoc == 0 && this.moduleDeclaration != null && CharOperation.equals(moduleName, this.moduleDeclaration.moduleName)) return;
		if (CharOperation.equals(moduleName, CharOperation.NO_CHAR)) return;
		this.knownModules.put(moduleName, this);
		char[] completion = moduleName;
		int relevance = computeBaseRelevance();
		relevance += computeRelevanceForResolution();
		relevance += computeRelevanceForInterestingProposal();
		relevance += computeRelevanceForCaseMatching(this.qualifiedCompletionToken == null ? this.completionToken : this.qualifiedCompletionToken, moduleName);
		relevance += computeRelevanceForQualification(true);
		relevance += computeRelevanceForRestrictions(IAccessRule.K_ACCESSIBLE);
		this.noProposal = false;
		if(!this.requestor.isIgnored(CompletionProposal.MODULE_REF)) {
			InternalCompletionProposal proposal = createProposal(CompletionProposal.MODULE_REF, this.actualCompletionPosition);
			if (this.assistNodeInJavadoc != 0) {
				completion = new char[moduleName.length + 1];
				System.arraycopy(moduleName, 0, completion, 0, moduleName.length);
				completion[moduleName.length] = '/';
			}
			proposal.setModuleName(moduleName);
			proposal.setDeclarationSignature(moduleName);
			proposal.setCompletion(completion);
			proposal.setReplaceRange(this.startPosition - this.offset, this.endPosition - this.offset);
			proposal.setTokenRange(this.tokenStart - this.offset, this.tokenEnd - this.offset);
			proposal.setRelevance(relevance);
			this.requestor.accept(proposal);
			if(DEBUG) {
				this.printDebug(proposal);
			}
		}

	}

	@Override
	public void acceptPackage(char[] packageName) {
		acceptPackage(packageName, null);
	}

	private void acceptPackage(char[] packageName, char[] moduleName) {

		if (this.knownPkgs.containsKey(packageName)) return;

		if (!isValidPackageName(packageName)) return;

		if (this.skipDefaultPackage &&
			CharOperation.equals(packageName, CharOperation.NO_CHAR))
			return;

		this.knownPkgs.put(packageName, this);

		char[] completion;
		if(this.resolvingImports) {
			if(this.resolvingStaticImports) {
				completion = CharOperation.concat(packageName, new char[] { '.' });
			} else {
				completion = CharOperation.concat(packageName, new char[] { '.', '*', ';' });
			}
		} else {
			completion = packageName;
		}

		int relevance = computeBaseRelevance();
		relevance += computeRelevanceForResolution();
		relevance += computeRelevanceForInterestingProposal();
		relevance += computeRelevanceForCaseMatching(this.qualifiedCompletionToken == null ? this.completionToken : this.qualifiedCompletionToken, packageName);
		if(!this.resolvingImports) {
			relevance += computeRelevanceForQualification(true);
		}
		relevance += computeRelevanceForRestrictions(IAccessRule.K_ACCESSIBLE);

		this.noProposal = false;
		if(!this.requestor.isIgnored(CompletionProposal.PACKAGE_REF)) {
			InternalCompletionProposal proposal = createProposal(CompletionProposal.PACKAGE_REF, this.actualCompletionPosition);
			char[] signature = packageName;
			if (moduleName != null) {
				signature = CharOperation.append(packageName, " - ".toCharArray()); //$NON-NLS-1$
				signature = CharOperation.append(signature, moduleName);
				signature = CharOperation.append(signature, '/');
				completion = CharOperation.append(moduleName, '/');
				completion = CharOperation.append(completion, packageName);
			}
			proposal.setDeclarationSignature(signature);
			proposal.setPackageName(packageName);
			proposal.setCompletion(completion);
			proposal.setReplaceRange(this.startPosition - this.offset, this.endPosition - this.offset);
			proposal.setTokenRange(this.tokenStart - this.offset, this.tokenEnd - this.offset);
			proposal.setRelevance(relevance);
			this.requestor.accept(proposal);
			if(DEBUG) {
				this.printDebug(proposal);
			}
		}
	}

	@Override
	public void acceptType(
		char[] packageName,
		char[] simpleTypeName,
		char[][] enclosingTypeNames,
		int modifiers,
		AccessRestriction accessRestriction) {

		// does not check cancellation for every types to avoid performance loss
		if ((this.foundTypesCount % CHECK_CANCEL_FREQUENCY) == 0) checkCancel();
		this.foundTypesCount++;

		if (this.options.checkDeprecation && (modifiers & ClassFileConstants.AccDeprecated) != 0) return;
		if (this.assistNodeIsExtendedType && (modifiers & ClassFileConstants.AccFinal) != 0) return;
		if (this.assistNodeIsExtendedType && (modifiers & ExtraCompilerModifiers.AccRecord) != 0) return;

		if (this.options.checkVisibility) {
			if((modifiers & ClassFileConstants.AccPublic) == 0) {
				if((modifiers & ClassFileConstants.AccPrivate) != 0) return;
				if (this.moduleDeclaration == null) {
					char[] currentPackage = CharOperation.concatWith(this.unitScope.fPackage.compoundName, '.');
					if(!CharOperation.equals(packageName, currentPackage)) return;
				}
			}
		}

		int accessibility = IAccessRule.K_ACCESSIBLE;
		if(accessRestriction != null) {
			switch (accessRestriction.getProblemId()) {
				case IProblem.ForbiddenReference:
					if (this.options.checkForbiddenReference) {
						return;
					}
					accessibility = IAccessRule.K_NON_ACCESSIBLE;
					break;
				case IProblem.DiscouragedReference:
					if (this.options.checkDiscouragedReference) {
						return;
					}
					accessibility = IAccessRule.K_DISCOURAGED;
					break;
			}
		}

		if (isForbidden(packageName, simpleTypeName, enclosingTypeNames)) {
			return;
		}

		if(this.acceptedTypes == null) {
			this.acceptedTypes = new ObjectVector();
		}
		this.acceptedTypes.add(new AcceptedType(packageName, simpleTypeName, enclosingTypeNames, modifiers, accessibility));
	}

	private void acceptTypes(Scope scope) {
		this.checkCancel();

		if(this.acceptedTypes == null) return;

		int length = this.acceptedTypes.size();

		if(length == 0) return;

		HashtableOfObject onDemandFound = new HashtableOfObject();

		try {
			next : for (int i = 0; i < length; i++) {

				// does not check cancellation for every types to avoid performance loss
				if ((i % CHECK_CANCEL_FREQUENCY) == 0) checkCancel();

				AcceptedType acceptedType = (AcceptedType)this.acceptedTypes.elementAt(i);
				char[] packageName = acceptedType.packageName;
				char[] simpleTypeName = acceptedType.simpleTypeName;
				char[][] enclosingTypeNames = acceptedType.enclosingTypeNames;
				int modifiers = acceptedType.modifiers;
				int accessibility = acceptedType.accessibility;

				char[] typeName;
				char[] flatEnclosingTypeNames;
				if(enclosingTypeNames == null || enclosingTypeNames.length == 0) {
					flatEnclosingTypeNames = null;
					typeName = simpleTypeName;
				} else {
					flatEnclosingTypeNames = CharOperation.concatWith(acceptedType.enclosingTypeNames, '.');
					typeName = CharOperation.concat(flatEnclosingTypeNames, simpleTypeName, '.');
				}
				char[] fullyQualifiedName = CharOperation.concat(packageName, typeName, '.');

				if (this.knownTypes.containsKey(fullyQualifiedName)) continue next;

				this.knownTypes.put(fullyQualifiedName, KNOWN_TYPE_WITH_UNKNOWN_CONSTRUCTORS);

				if (this.resolvingImports) {
					if(this.compilerOptions.complianceLevel >= ClassFileConstants.JDK1_4 && packageName.length == 0) {
						continue next; // import of default package is forbidden when compliance is 1.4 or higher
					}

					char[] completionName = this.insideQualifiedReference ? simpleTypeName : fullyQualifiedName;

					if(this.resolvingStaticImports) {
						if(enclosingTypeNames == null || enclosingTypeNames.length == 0) {
							completionName = CharOperation.concat(completionName, new char[] { '.' });
						} else if ((modifiers & ClassFileConstants.AccStatic) == 0) {
							continue next;
						} else {
							completionName = appendUnlessNextToken(completionName, new char[] { ';' }, TerminalTokens.TokenNameSEMICOLON);
						}
					} else {
						completionName = appendUnlessNextToken(completionName, new char[] {';'}, TerminalTokens.TokenNameSEMICOLON);
					}

					int relevance = computeBaseRelevance();
					relevance += computeRelevanceForResolution();
					relevance += computeRelevanceForInterestingProposal(packageName, fullyQualifiedName);
					relevance += computeRelevanceForRestrictions(accessibility);
					relevance += computeRelevanceForCaseMatching(this.completionToken, simpleTypeName);

					this.noProposal = false;
					if(!this.requestor.isIgnored(CompletionProposal.TYPE_REF)) {
						createTypeProposal(packageName, typeName, modifiers, accessibility, completionName, relevance);
					}
				} else {
					if(!this.importCachesInitialized) {
						initializeImportCaches();
					}

					for (int j = 0; j < this.importCacheCount; j++) {
						char[][] importName = this.importsCache[j];
						if(CharOperation.equals(typeName, importName[0])) {
							proposeType(
									packageName,
									simpleTypeName,
									modifiers,
									accessibility,
									typeName,
									fullyQualifiedName,
									!CharOperation.equals(fullyQualifiedName, importName[1]),
									scope);
							continue next;
						}
					}


					if ((enclosingTypeNames == null || enclosingTypeNames.length == 0 ) && CharOperation.equals(this.currentPackageName, packageName)) {
						proposeType(
								packageName,
								simpleTypeName,
								modifiers,
								accessibility,
								typeName,
								fullyQualifiedName,
								false,
								scope);
						continue next;
					} else {
						char[] fullyQualifiedEnclosingTypeOrPackageName = null;

						AcceptedType foundType = null;
						if((foundType = (AcceptedType)onDemandFound.get(simpleTypeName)) == null) {
							for (int j = 0; j < this.onDemandImportCacheCount; j++) {
								ImportBinding importBinding = this.onDemandImportsCache[j];

								char[][] importName = importBinding.compoundName;
								char[] importFlatName = CharOperation.concatWith(importName, '.');

								if(fullyQualifiedEnclosingTypeOrPackageName == null) {
									if(enclosingTypeNames != null && enclosingTypeNames.length != 0) {
										fullyQualifiedEnclosingTypeOrPackageName =
											CharOperation.concat(
													packageName,
													flatEnclosingTypeNames,
													'.');
									} else {
										fullyQualifiedEnclosingTypeOrPackageName =
											packageName;
									}
								}
								if(CharOperation.equals(fullyQualifiedEnclosingTypeOrPackageName, importFlatName)) {
									if(importBinding.isStatic()) {
										if((modifiers & ClassFileConstants.AccStatic) != 0) {
											acceptedType.qualifiedTypeName = typeName;
											acceptedType.fullyQualifiedName = fullyQualifiedName;
											onDemandFound.put(
													simpleTypeName,
													acceptedType);
											continue next;
										}
									} else {
										acceptedType.qualifiedTypeName = typeName;
										acceptedType.fullyQualifiedName = fullyQualifiedName;
										onDemandFound.put(
												simpleTypeName,
												acceptedType);
										continue next;
									}
								}
							}
						} else if(!foundType.mustBeQualified){
							done : for (int j = 0; j < this.onDemandImportCacheCount; j++) {
								ImportBinding importBinding = this.onDemandImportsCache[j];

								char[][] importName = importBinding.compoundName;
								char[] importFlatName = CharOperation.concatWith(importName, '.');

								if(fullyQualifiedEnclosingTypeOrPackageName == null) {
									if(enclosingTypeNames != null && enclosingTypeNames.length != 0) {
										fullyQualifiedEnclosingTypeOrPackageName =
											CharOperation.concat(
													packageName,
													flatEnclosingTypeNames,
													'.');
									} else {
										fullyQualifiedEnclosingTypeOrPackageName =
											packageName;
									}
								}
								if(CharOperation.equals(fullyQualifiedEnclosingTypeOrPackageName, importFlatName)) {
									if(importBinding.isStatic()) {
										if((modifiers & ClassFileConstants.AccStatic) != 0) {
											foundType.mustBeQualified = true;
											break done;
										}
									} else {
										foundType.mustBeQualified = true;
										break done;
									}
								}
							}
						}
						proposeType(
								packageName,
								simpleTypeName,
								modifiers,
								accessibility,
								typeName,
								fullyQualifiedName,
								true,
								scope);
					}
				}
			}

			char[][] keys = onDemandFound.keyTable;
			Object[] values = onDemandFound.valueTable;
			int max = keys.length;
			for (int i = 0; i < max; i++) {
				if ((i % CHECK_CANCEL_FREQUENCY) == 0) checkCancel();
				if(keys[i] != null) {
					AcceptedType value = (AcceptedType) values[i];
					if(value != null) {
						proposeType(
								value.packageName,
								value.simpleTypeName,
								value.modifiers,
								value.accessibility,
								value.qualifiedTypeName,
								value.fullyQualifiedName,
								value.mustBeQualified,
								scope);
					}
				}
			}
		} finally {
			this.acceptedTypes = null; // reset
		}
	}

	private char[] appendUnlessNextToken(char[] completionName, char[] suffix, int nextToken) {
		if (this.source == null)
			return CharOperation.concat(completionName, suffix);

		AssistParser assistParser = getParser();
		Object parserState = assistParser.becomeSimpleParser();

		assistParser.scanner.setSource(this.source);
		assistParser.scanner.resetTo(this.endPosition, Integer.MAX_VALUE);
		try {
			if (assistParser.scanner.getNextToken() != nextToken) {
				return CharOperation.concat(completionName, suffix);
			}
		} catch (InvalidInputException e) {
			// ignore
		} finally {
			assistParser.restoreAssistParser(parserState);
		}
		return completionName;
	}

	public void acceptUnresolvedName(char[] name) {
		int relevance = computeBaseRelevance();
		relevance += computeRelevanceForResolution(false);
		relevance += computeRelevanceForInterestingProposal();
		relevance += computeRelevanceForCaseMatching(this.completionToken, name);
		relevance += computeRelevanceForQualification(false);
		relevance += computeRelevanceForRestrictions(IAccessRule.K_ACCESSIBLE); // no access restriction for local variable
		CompletionEngine.this.noProposal = false;
		if(!CompletionEngine.this.requestor.isIgnored(CompletionProposal.LOCAL_VARIABLE_REF)) {
			InternalCompletionProposal proposal = CompletionEngine.this.createProposal(CompletionProposal.LOCAL_VARIABLE_REF, CompletionEngine.this.actualCompletionPosition);
			proposal.setSignature(JAVA_LANG_OBJECT_SIGNATURE);
			proposal.setPackageName(JAVA_LANG_NAME);
			proposal.setTypeName(OBJECT);
			proposal.setName(name);
			proposal.setCompletion(name);
			proposal.setFlags(Flags.AccDefault);
			proposal.setReplaceRange(this.startPosition - this.offset, this.endPosition - this.offset);
			proposal.setTokenRange(this.tokenStart - this.offset, this.tokenEnd - this.offset);
			proposal.setRelevance(relevance);
			CompletionEngine.this.requestor.accept(proposal);
			if(DEBUG) {
				CompletionEngine.this.printDebug(proposal);
			}
		}
	}
	private void addExpectedType(TypeBinding type, Scope scope){
		if (type == null || !type.isValidBinding() || type == TypeBinding.NULL || type == TypeBinding.VOID) return;

		// do not add twice the same type
		for (int i = 0; i <= this.expectedTypesPtr; i++) {
			if (TypeBinding.equalsEquals(this.expectedTypes[i], type)) return;
		}

		int length = this.expectedTypes.length;
		if (++this.expectedTypesPtr >= length)
			System.arraycopy(this.expectedTypes, 0, this.expectedTypes = new TypeBinding[length * 2], 0, length);
		this.expectedTypes[this.expectedTypesPtr] = type;

		if(TypeBinding.equalsEquals(type, scope.getJavaLangObject())) {
			this.hasJavaLangObjectAsExpectedType = true;
		}
	}

	private void addForbiddenBindings(Binding binding){
		if (binding == null) return;

		int length = this.forbbidenBindings.length;
		if (++this.forbbidenBindingsPtr >= length)
			System.arraycopy(this.forbbidenBindings, 0, this.forbbidenBindings = new Binding[length * 2], 0, length);
		this.forbbidenBindings[this.forbbidenBindingsPtr] = binding;
	}

	private void addUninterestingBindings(Binding binding){
		if (binding == null) return;

		int length = this.uninterestingBindings.length;
		if (++this.uninterestingBindingsPtr >= length)
			System.arraycopy(this.uninterestingBindings, 0, this.uninterestingBindings = new Binding[length * 2], 0, length);
		this.uninterestingBindings[this.uninterestingBindingsPtr] = binding;
	}

	// this code is derived from MethodBinding#areParametersCompatibleWith(TypeBinding[])
	private final boolean areParametersCompatibleWith(TypeBinding[] parameters, TypeBinding[] arguments, boolean isVarargs) {
		int paramLength = parameters.length;
		int argLength = arguments.length;
		int lastIndex = argLength;
		if (isVarargs) {
			lastIndex = paramLength - 1;
			if (paramLength == argLength) { // accept X[] but not X or X[][]
				TypeBinding varArgType = parameters[lastIndex]; // is an ArrayBinding by definition
				TypeBinding lastArgument = arguments[lastIndex];
				if (TypeBinding.notEquals(varArgType, lastArgument) && !lastArgument.isCompatibleWith(varArgType))
					return false;
			} else if (paramLength < argLength) { // all remainig argument types must be compatible with the elementsType of varArgType
				TypeBinding varArgType = ((ArrayBinding) parameters[lastIndex]).elementsType();
				for (int i = lastIndex; i < argLength; i++)
					if (TypeBinding.notEquals(varArgType, arguments[i]) && !arguments[i].isCompatibleWith(varArgType))
						return false;
			} else if (lastIndex != argLength) { // can call foo(int i, X ... x) with foo(1) but NOT foo();
				return false;
			}
			// now compare standard arguments from 0 to lastIndex
		} else {
			if(paramLength != argLength)
				return false;
		}
		for (int i = 0; i < lastIndex; i++)
			if (TypeBinding.notEquals(parameters[i], arguments[i]) && !arguments[i].isCompatibleWith(parameters[i]))
				return false;
		return true;
	}

	private void buildContext(
			ASTNode astNode,
			ASTNode astNodeParent,
			CompilationUnitDeclaration compilationUnitDeclaration,
			Binding qualifiedBinding,
			Scope scope) {
		InternalCompletionContext context = new InternalCompletionContext();
		if (this.requestor.isExtendedContextRequired()) {
			context.setExtendedData(
					this.typeRoot,
					compilationUnitDeclaration,
					this.lookupEnvironment,
					scope,
					astNode,
					astNodeParent,
					this.owner,
					this.parser);
		}

		// build expected types context
		if (this.expectedTypesPtr > -1) {
			int length = this.expectedTypesPtr + 1;
			char[][] expTypes = new char[length][];
			char[][] expKeys = new char[length][];
			for (int i = 0; i < length; i++) {
				expTypes[i] = getSignature(this.expectedTypes[i]);
				expKeys[i] = this.expectedTypes[i].computeUniqueKey();
			}
			context.setExpectedTypesSignatures(expTypes);
			context.setExpectedTypesKeys(expKeys);
		}

		context.setOffset(this.actualCompletionPosition + 1 - this.offset);

		// Set javadoc info
		if (astNode instanceof CompletionOnJavadoc) {
			this.assistNodeInJavadoc = ((CompletionOnJavadoc)astNode).getCompletionFlags();
			context.setJavadoc(this.assistNodeInJavadoc);
		}

		if (!(astNode instanceof CompletionOnJavadoc)) {
			CompletionScanner scanner = (CompletionScanner)this.parser.scanner;
			context.setToken(scanner.completionIdentifier);
			context.setTokenRange(
					scanner.completedIdentifierStart - this.offset,
					scanner.completedIdentifierEnd - this.offset,
					scanner.endOfEmptyToken - this.offset);
		} else if(astNode instanceof CompletionOnJavadocTag) {
			CompletionOnJavadocTag javadocTag = (CompletionOnJavadocTag) astNode;
			context.setToken(CharOperation.concat(new char[]{'@'}, javadocTag.token));
			context.setTokenRange(
					javadocTag.tagSourceStart - this.offset,
					javadocTag.tagSourceEnd - this.offset,
					((CompletionScanner)this.parser.javadocParser.scanner).endOfEmptyToken - this.offset);
		} else {
			CompletionScanner scanner = (CompletionScanner)this.parser.javadocParser.scanner;
			context.setToken(scanner.completionIdentifier);
			context.setTokenRange(
					scanner.completedIdentifierStart - this.offset,
					scanner.completedIdentifierEnd - this.offset,
					scanner.endOfEmptyToken - this.offset);
		}

		if(astNode instanceof CompletionOnStringLiteral) {
			context.setTokenKind(CompletionContext.TOKEN_KIND_STRING_LITERAL);
		} else {
			context.setTokenKind(CompletionContext.TOKEN_KIND_NAME);
		}

		buildTokenLocationContext(context, scope, astNode, astNodeParent);

		if(DEBUG) {
			System.out.println(context.toString());
		}
		this.requestor.acceptContext(context);
	}

	private void buildTokenLocationContext(InternalCompletionContext context, Scope scope, ASTNode astNode, ASTNode astNodeParent) {
		if (scope == null || context.isInJavadoc()) return;

		if (astNode instanceof CompletionOnFieldType) {
			CompletionOnFieldType field = (CompletionOnFieldType) astNode;
			if (!field.isLocalVariable &&
					field.modifiers == ClassFileConstants.AccDefault &&
					(field.annotations == null || field.annotations.length == 0)) {
				context.setTokenLocation(CompletionContext.TL_MEMBER_START);
			}
		} else if (astNode instanceof CompletionOnMethodReturnType) {
			CompletionOnMethodReturnType method = (CompletionOnMethodReturnType) astNode;
			if (method.modifiers == ClassFileConstants.AccDefault &&
					(method.annotations == null || method.annotations.length == 0)) {
				context.setTokenLocation(CompletionContext.TL_MEMBER_START);
			}
		} else if (astNode instanceof CompletionOnSingleTypeReference) {
			CompletionOnSingleTypeReference completionOnSingleTypeReference = (CompletionOnSingleTypeReference) astNode;
			if (completionOnSingleTypeReference.isConstructorType) {
				context.setTokenLocation(CompletionContext.TL_CONSTRUCTOR_START);
			} else if (astNodeParent instanceof LambdaExpression || astNodeParent instanceof LocalDeclaration) {
				context.setTokenLocation(CompletionContext.TL_STATEMENT_START);
			}
		} else if (astNode instanceof CompletionOnQualifiedTypeReference) {
			CompletionOnQualifiedTypeReference completionOnQualifiedTypeReference = (CompletionOnQualifiedTypeReference) astNode;
			if (completionOnQualifiedTypeReference.isConstructorType){
				context.setTokenLocation(CompletionContext.TL_CONSTRUCTOR_START);
			} else if (astNodeParent instanceof LambdaExpression) {
				context.setTokenLocation(CompletionContext.TL_STATEMENT_START);
			}
		} else if (astNode instanceof CompletionOnKeyword3 && ((CompletionOnKeyword3) astNode).afterTryOrCatch()) {
				context.setTokenLocation(CompletionContext.TL_STATEMENT_START);
		} else if (astNode instanceof CompletionOnImportReference) {
			context.setTokenLocation(CompletionContext.TL_IN_IMPORT);
		} else {
			ReferenceContext referenceContext = scope.referenceContext();
			if (referenceContext instanceof AbstractMethodDeclaration) {
				AbstractMethodDeclaration methodDeclaration = (AbstractMethodDeclaration)referenceContext;
				if (methodDeclaration.bodyStart <= astNode.sourceStart &&
						astNode.sourceEnd <= methodDeclaration.bodyEnd) {
					// completion is inside a method body
					if (astNodeParent == null &&
							astNode instanceof CompletionOnSingleNameReference &&
							!((CompletionOnSingleNameReference)astNode).isPrecededByModifiers) {
						context.setTokenLocation(CompletionContext.TL_STATEMENT_START);
					}
				}
			} else if (referenceContext instanceof LambdaExpression) {
				LambdaExpression expression = (LambdaExpression)referenceContext;
				if (expression.body().sourceStart <= astNode.sourceStart &&
						astNode.sourceEnd <= expression.body().sourceEnd) {
					// completion is inside a method body
					if ((astNodeParent == null || astNodeParent == expression) &&
							astNode instanceof CompletionOnSingleNameReference &&
							!((CompletionOnSingleNameReference)astNode).isPrecededByModifiers) {
						context.setTokenLocation(CompletionContext.TL_STATEMENT_START);
					}
				}
			} else if (referenceContext instanceof TypeDeclaration) {
				TypeDeclaration typeDeclaration = (TypeDeclaration) referenceContext;
				FieldDeclaration[] fields = typeDeclaration.fields;
				if (fields != null) {
					done : for (int i = 0; i < fields.length; i++) {
						if (fields[i] instanceof Initializer) {
							Initializer initializer = (Initializer) fields[i];
							if (initializer.block.sourceStart <= astNode.sourceStart &&
									astNode.sourceStart < initializer.bodyEnd) {
								// completion is inside an initializer
								if (astNodeParent == null &&
										astNode instanceof CompletionOnSingleNameReference &&
										!((CompletionOnSingleNameReference)astNode).isPrecededByModifiers) {
									context.setTokenLocation(CompletionContext.TL_STATEMENT_START);
								}
								break done;
							}
						}
					}
				}
			}
		}
	}

	void checkCancel() {
		if (this.monitor != null && this.monitor.isCanceled()) {
			throw new OperationCanceledException();
		}
	}

	private boolean complete(
			ASTNode astNode,
			ASTNode astNodeParent,
			ASTNode enclosingNode,
			CompilationUnitDeclaration compilationUnitDeclaration,
			Binding qualifiedBinding,
			Scope scope,
			boolean insideTypeAnnotation) {

		setSourceAndTokenRange(astNode.sourceStart, astNode.sourceEnd);

		scope = computeForbiddenBindings(astNode, astNodeParent, scope);
		computeUninterestingBindings(astNode, astNodeParent, scope);
		if(astNodeParent != null) {
			if(!isValidParent(astNodeParent, astNode, scope)) return false;
			computeExpectedTypes(astNodeParent, astNode, scope);
		}

		buildContext(astNode, astNodeParent, compilationUnitDeclaration, qualifiedBinding, scope);

		if (astNode instanceof CompletionOnMemberAccess && qualifiedBinding instanceof BaseTypeBinding) return true;
		if (astNode instanceof CompletionOnFieldType) {
			completionOnFieldType(astNode, scope);
		} else if (astNode instanceof CompletionOnMethodReturnType) {
			completionOnMethodReturnType(astNode, scope);
		} else if (astNode instanceof CompletionOnSingleNameReference) {
			completionOnSingleNameReference(astNode, astNodeParent, scope, insideTypeAnnotation);
		} else if (astNode instanceof CompletionOnProvidesInterfacesQualifiedTypeReference) {
			completionOnProvidesInterfacesQualifiedTypeReference(astNode, astNodeParent, qualifiedBinding, scope);
		} else if (astNode instanceof CompletionOnProvidesInterfacesSingleTypeReference) {
			completionOnProvidesInterfacesSingleTypeReference(astNode, astNodeParent, qualifiedBinding, scope);
		} else if (astNode instanceof CompletionOnProvidesImplementationsQualifiedTypeReference) {
			completionOnProvidesImplementationsQualifiedTypeReference(astNode, astNodeParent, qualifiedBinding, scope);
		} else if (astNode instanceof CompletionOnProvidesImplementationsSingleTypeReference) {
			completionOnProvidesImplementationsSingleTypeReference(astNode, astNodeParent, qualifiedBinding, scope);
		} else if (astNode instanceof CompletionOnSingleTypeReference) {
			completionOnSingleTypeReference(astNode, astNodeParent, qualifiedBinding, scope);
		} else if (astNode instanceof CompletionOnQualifiedNameReference) {
			completionOnQualifiedNameReference(astNode, enclosingNode, qualifiedBinding, scope, insideTypeAnnotation);
		} else if (astNode instanceof CompletionOnQualifiedTypeReference) {
			completionOnQualifiedTypeReference(astNode, astNodeParent, enclosingNode, qualifiedBinding, scope);
		} else if (astNode instanceof CompletionOnMemberAccess) {
			completionOnMemberAccess(astNode, enclosingNode, qualifiedBinding, scope, insideTypeAnnotation);
		} else if (astNode instanceof CompletionOnMessageSend) {
			completionOnMessageSend(astNode, qualifiedBinding, scope);
			// rebuild the context with newly found expected types so other completion computers can benifit from it.
			if(this.expectedTypesPtr > -1) {
				buildContext(astNode, astNodeParent, compilationUnitDeclaration, qualifiedBinding, scope);
			}
		} else if (astNode instanceof CompletionOnExplicitConstructorCall) {
			completionOnExplicitConstructorCall(astNode, qualifiedBinding, scope);
		} else if (astNode instanceof CompletionOnQualifiedAllocationExpression) {
			completionOnQualifiedAllocationExpression(astNode, qualifiedBinding, scope);
		} else if (astNode instanceof CompletionOnClassLiteralAccess) {
			completionOnClassLiteralAccess(astNode, qualifiedBinding, scope);
		} else if (astNode instanceof CompletionOnMethodName) {
			completionOnMethodName(astNode, scope);
		} else if (astNode instanceof CompletionOnFieldName) {
			completionOnFieldName(astNode, scope);
		} else if (astNode instanceof CompletionOnLocalName) {
			completionOnLocalOrArgumentName(astNode, scope);
		} else if (astNode instanceof CompletionOnArgumentName) {
			completionOnLocalOrArgumentName(astNode, scope);
		} else if (astNode instanceof CompletionOnKeyword) {
			completionOnKeyword(astNode);
		} else if (astNode instanceof CompletionOnParameterizedQualifiedTypeReference) {
			completionOnParameterizedQualifiedTypeReference(astNode, astNodeParent, qualifiedBinding, scope);
		} else if (astNode instanceof CompletionOnMarkerAnnotationName) {
			completionOnMarkerAnnotationName(astNode, qualifiedBinding, scope);
		} else if (astNode instanceof CompletionOnMemberValueName) {
			completionOnMemberValueName(astNode, astNodeParent, scope, insideTypeAnnotation);
		} else if(astNode instanceof CompletionOnBreakStatement) {
			completionOnBreakStatement((CompletionOnBreakStatement) astNode);
		} else if(astNode instanceof CompletionOnContinueStatement) {
			completionOnContinueStatement((CompletionOnContinueStatement) astNode);
		} else if(astNode instanceof CompletionOnMessageSendName) {
			completionOnMessageSendName(astNode, qualifiedBinding, scope);
		} else if (astNode instanceof CompletionOnReferenceExpressionName) {
			completionOnReferenceExpressionName(astNode, qualifiedBinding, scope);
		// Completion on Javadoc nodes
		} else if ((astNode.bits & ASTNode.InsideJavadoc) != 0) {
			if (astNode instanceof CompletionOnJavadocSingleTypeReference) {
				completionOnJavadocSingleTypeReference(astNode, scope);
			} else if (astNode instanceof CompletionOnJavadocQualifiedTypeReference) {
				completionOnJavadocQualifiedTypeReference(astNode, qualifiedBinding, scope);
			} else if (astNode instanceof CompletionOnJavadocModuleReference) {
				completionOnJavadocModuleReference(astNode, qualifiedBinding, scope);
			} else if (astNode instanceof CompletionOnJavadocFieldReference) {
				completionOnJavadocFieldReference(astNode, scope);
			} else if (astNode instanceof CompletionOnJavadocMessageSend) {
				completionOnJavadocMessageSend(astNode, qualifiedBinding, scope);
			} else if (astNode instanceof CompletionOnJavadocAllocationExpression) {
				completionOnJavadocAllocationExpression(astNode, qualifiedBinding, scope);
			} else if (astNode instanceof CompletionOnJavadocParamNameReference) {
				completionOnJavadocParamNameReference(astNode);
			} else if (astNode instanceof CompletionOnJavadocTypeParamReference) {
				completionOnJavadocTypeParamReference(astNode);
			} else if (astNode instanceof CompletionOnJavadocTag) {
				completionOnJavadocTag(astNode);
			}
		}
		return true;
	}

	/**
	 * Ask the engine to compute a completion at the specified position
	 * of the given compilation unit.
	 *
	 *  No return
	 *      completion results are answered through a requestor.
	 *
	 *  @param sourceUnit org.eclipse.jdt.internal.compiler.env.ICompilationUnit
	 *      the source of the current compilation unit.
	 *
	 *  @param completionPosition int
	 *      a position in the source where the completion is taking place.
	 *      This position is relative to the source provided.
	 */
	public void complete(ICompilationUnit sourceUnit, int completionPosition, int pos, ITypeRoot root) {

		if(DEBUG) {
			System.out.print("COMPLETION IN "); //$NON-NLS-1$
			System.out.print(sourceUnit.getFileName());
			System.out.print(" AT POSITION "); //$NON-NLS-1$
			System.out.println(completionPosition);
			System.out.println("COMPLETION - Source :"); //$NON-NLS-1$
			System.out.println(sourceUnit.getContents());
		}
		if (this.monitor != null) this.monitor.beginTask(Messages.engine_completing, IProgressMonitor.UNKNOWN);
		this.requestor.beginReporting();
		boolean contextAccepted = false;
		try {
			this.fileName = sourceUnit.getFileName();
			this.actualCompletionPosition = completionPosition - 1;
			this.offset = pos;
			this.typeRoot = root;
			this.source = sourceUnit.getContents();

			this.checkCancel();

			// for now until we can change the UI.
			CompilationResult result = new CompilationResult(sourceUnit, 1, 1, this.compilerOptions.maxProblemsPerUnit);
			CompilationUnitDeclaration parsedUnit = this.parser.dietParse(sourceUnit, result, this.actualCompletionPosition);

			//		boolean completionNodeFound = false;
			if (parsedUnit != null) {
				if(DEBUG) {
					System.out.println("COMPLETION - Diet AST :"); //$NON-NLS-1$
					System.out.println(parsedUnit.toString());
				}

				if (parsedUnit.isModuleInfo()) {
					this.moduleDeclaration = parsedUnit.moduleDeclaration;
					if (this.moduleDeclaration == null) return;
					if (this.moduleDeclaration instanceof CompletionOnModuleDeclaration) {
						contextAccepted = true;
						buildContext(parsedUnit.moduleDeclaration, null, parsedUnit, null, null);
						//this.requestor.setIgnored(CompletionProposal.MODULE_DECLARATION, false); //TODO: Hack until ui fixes this issue.
						if(!this.requestor.isIgnored(CompletionProposal.MODULE_DECLARATION)) {
							proposeModuleName(parsedUnit);
						}
						debugPrintf();
						return;
					}
					if (this.moduleDeclaration instanceof CompletionOnKeywordModuleDeclaration) {
						contextAccepted = true;
						processModuleKeywordCompletion(parsedUnit, this.moduleDeclaration, (CompletionOnKeyword) this.moduleDeclaration);
						return;
					}
					if (this.moduleDeclaration.exports != null) {
						contextAccepted = completeOnPackageVisibilityStatements(contextAccepted, parsedUnit, this.moduleDeclaration.exports);
						if (contextAccepted) return;
					}
					if (this.moduleDeclaration.opens != null) {
						contextAccepted = completeOnPackageVisibilityStatements(contextAccepted, parsedUnit, this.moduleDeclaration.opens);
						if (contextAccepted) return;
					}
					RequiresStatement[] moduleRefs = this.moduleDeclaration.requires;
					if (moduleRefs != null) {
						for (int i = 0, l = moduleRefs.length; i < l; ++i) {
							ModuleReference reference = moduleRefs[i].module;
							if (reference instanceof CompletionOnModuleReference) {
								contextAccepted = true;
								buildContext(reference, null, parsedUnit, null, null);
								if(!this.requestor.isIgnored(CompletionProposal.MODULE_REF)) {
									findModules((CompletionOnModuleReference) reference, false /* targetted */);
								}
								debugPrintf();
								return;
							}
						}
					}
					try {
						UsesStatement[] uses = this.moduleDeclaration.uses;
						if (uses != null) {
							for (int i = 0, l = uses.length; i < l; ++i) {
								UsesStatement usesStatement = uses[i];
								TypeReference usesReference = usesStatement.serviceInterface;
								if (usesReference instanceof CompletionOnUsesSingleTypeReference ||
										usesReference instanceof CompletionOnUsesQualifiedTypeReference) {
									contextAccepted = checkForCNF(usesReference, parsedUnit, true);
									return;
								}
							}
						}
						ProvidesStatement[] providesStmts = this.moduleDeclaration.services;
						for (int i = 0, l = providesStmts != null ? providesStmts.length : 0; i < l; ++i) {
							ProvidesStatement providesStmt = providesStmts[i];
							this.parser.enclosingNode = providesStmt;
							TypeReference pInterface = providesStmt.serviceInterface;
							if (pInterface instanceof CompletionOnProvidesInterfacesSingleTypeReference ||
									pInterface instanceof CompletionOnProvidesInterfacesQualifiedTypeReference) {
								contextAccepted = checkForCNF(pInterface, parsedUnit, true);
								return;
							}
							TypeReference[] implementations = providesStmt.implementations;
							for (int j = 0, k = implementations.length; j < k; ++j) {
								TypeReference implementation = implementations[j];
								if (implementation instanceof CompletionOnProvidesImplementationsSingleTypeReference ||
										implementation instanceof CompletionOnProvidesImplementationsQualifiedTypeReference) {
									this.skipDefaultPackage = true;
									contextAccepted = checkForCNF(implementation, parsedUnit, false);
									return;
								} else if (implementation instanceof CompletionOnKeyword) {
									contextAccepted = true;
									processModuleKeywordCompletion(parsedUnit, implementation, (CompletionOnKeyword) implementation);
								}
							}
						}
					} catch (CompletionNodeFound e) {
						//					completionNodeFound = true;
						if (e.astNode != null) {
							// if null then we found a problem in the completion node
							if(DEBUG) {
								System.out.print("COMPLETION - Completion node : "); //$NON-NLS-1$
								System.out.println(e.astNode.toString());
								if(this.parser.assistNodeParent != null) {
									System.out.print("COMPLETION - Parent Node : ");  //$NON-NLS-1$
									System.out.println(this.parser.assistNodeParent);
								}
							}
							this.lookupEnvironment.unitBeingCompleted = parsedUnit; // better resilient to further error reporting
							contextAccepted =
									complete(
											e.astNode,
											this.parser.assistNodeParent,
											this.parser.enclosingNode,
											parsedUnit,
											e.qualifiedBinding,
											e.scope,
											e.insideTypeAnnotation);
						}
					} finally {
						this.skipDefaultPackage = false;
					}
				}
				// scan the package & import statements first
				if (parsedUnit.currentPackage instanceof CompletionOnPackageReference) {
					contextAccepted = true;
					buildContext(parsedUnit.currentPackage, null, parsedUnit, null, null);
					if(!this.requestor.isIgnored(CompletionProposal.PACKAGE_REF)) {
						findPackages((CompletionOnPackageReference) parsedUnit.currentPackage);
					}
					debugPrintf();
					return;
				}

				ImportReference[] imports = parsedUnit.imports;
				if (imports != null) {
					for (int i = 0, length = imports.length; i < length; i++) {
						ImportReference importReference = imports[i];
						if (importReference instanceof CompletionOnImportReference) {
							this.lookupEnvironment.buildTypeBindings(parsedUnit, null /*no access restriction*/);
							if ((this.unitScope = parsedUnit.scope) != null) {
								contextAccepted = true;
								buildContext(importReference, null, parsedUnit, null, this.unitScope);

								long positions = importReference.sourcePositions[importReference.tokens.length - 1];
								setSourceAndTokenRange((int) (positions >>> 32), (int) positions);

								char[][] oldTokens = importReference.tokens;
								int tokenCount = oldTokens.length;
								if (tokenCount == 1) {
									findImports((CompletionOnImportReference)importReference, true);
								} else if(tokenCount > 1){
									this.insideQualifiedReference = true;

									char[] lastToken = oldTokens[tokenCount - 1];
									char[][] qualifierTokens = CharOperation.subarray(oldTokens, 0, tokenCount - 1);

									Binding binding = this.unitScope.getTypeOrPackage(qualifierTokens);
									if(binding != null) {
										if(binding instanceof PackageBinding) {
											findImports((CompletionOnImportReference)importReference, false);
										} else {
											ReferenceBinding ref = (ReferenceBinding) binding;

											if(!this.requestor.isIgnored(CompletionProposal.TYPE_REF)) {
												findImportsOfMemberTypes(lastToken, ref, importReference.isStatic());
											}
											if(importReference.isStatic()) {

												if(!this.requestor.isIgnored(CompletionProposal.FIELD_REF)) {
													findImportsOfStaticFields(lastToken, ref);
												}
												if(!this.requestor.isIgnored(CompletionProposal.METHOD_NAME_REFERENCE)) {
													findImportsOfStaticMethods(lastToken, ref);
												}
											}
										}
									}
								}

								debugPrintf();
							}
							return;
						} else if(importReference instanceof CompletionOnKeyword) {
							contextAccepted = true;
							buildContext(importReference, null, parsedUnit, null, null);
							if(!this.requestor.isIgnored(CompletionProposal.KEYWORD)) {
								setSourceAndTokenRange(importReference.sourceStart, importReference.sourceEnd);
								CompletionOnKeyword keyword = (CompletionOnKeyword)importReference;
								findKeywords(keyword.getToken(), keyword.getPossibleKeywords(), false, parsedUnit.currentPackage != null);
							}
							debugPrintf();
							return;
						}
					}
				}
				// javadoc tag completion in module-info file
				contextAccepted = completeJavadocTagInModuleInfo(parsedUnit);
				if (parsedUnit.types != null) {
					try {
						this.lookupEnvironment.buildTypeBindings(parsedUnit, null /*no access restriction*/);

						if ((this.unitScope = parsedUnit.scope) != null) {
							this.lookupEnvironment.completeTypeBindings(parsedUnit, true);
							parsedUnit.scope.faultInTypes();
							parseBlockStatements(parsedUnit, this.actualCompletionPosition);
							if(DEBUG) {
								System.out.println("COMPLETION - AST :"); //$NON-NLS-1$
								System.out.println(parsedUnit.toString());
							}
							parsedUnit.resolve();
						}
					} catch (CompletionNodeFound e) {
						//					completionNodeFound = true;
						if (e.astNode != null) {
							// if null then we found a problem in the completion node
							if(DEBUG) {
								System.out.print("COMPLETION - Completion node : "); //$NON-NLS-1$
								System.out.println(e.astNode.toString());
								if(this.parser.assistNodeParent != null) {
									System.out.print("COMPLETION - Parent Node : ");  //$NON-NLS-1$
									System.out.println(this.parser.assistNodeParent);
								}
							}
							this.lookupEnvironment.unitBeingCompleted = parsedUnit; // better resilient to further error reporting
							contextAccepted =
								complete(
									e.astNode,
									this.parser.assistNodeParent,
									this.parser.enclosingNode,
									parsedUnit,
									e.qualifiedBinding,
									e.scope,
									e.insideTypeAnnotation);
						}
					}
				}
			}

			if(this.noProposal && this.problem != null) {
				if(!contextAccepted) {
					contextAccepted = true;
					InternalCompletionContext context = new InternalCompletionContext();
					context.setOffset(completionPosition - this.offset);
					context.setTokenKind(CompletionContext.TOKEN_KIND_UNKNOWN);
					if (this.requestor.isExtendedContextRequired()) context.setExtended();
					this.requestor.acceptContext(context);
				}
				this.requestor.completionFailure(this.problem);
				if(DEBUG) {
					this.printDebug(this.problem);
				}
			}
			/* Ignore package, import, class & interface keywords for now...
					if (!completionNodeFound) {
						if (parsedUnit == null || parsedUnit.types == null) {
							// this is not good enough... can still be trying to define a second type
							CompletionScanner scanner = (CompletionScanner) this.parser.scanner;
							setSourceRange(scanner.completedIdentifierStart, scanner.completedIdentifierEnd);
							findKeywords(scanner.completionIdentifier, mainDeclarations, null);
						}
						// currently have no way to know if extends/implements are possible keywords
					}
			*/
		} catch (IndexOutOfBoundsException | InvalidCursorLocation | AbortCompilation | CompletionNodeFound e){ // internal failure - bugs 5618
			if(DEBUG) {
				System.out.println("Exception caught by CompletionEngine:"); //$NON-NLS-1$
				e.printStackTrace(System.out);
			}
		} finally {
			if(!contextAccepted) {
				contextAccepted = true;
				InternalCompletionContext context = new InternalCompletionContext();
				context.setTokenKind(CompletionContext.TOKEN_KIND_UNKNOWN);
				context.setOffset(completionPosition - this.offset);
				if (this.requestor.isExtendedContextRequired()) context.setExtended();
				this.requestor.acceptContext(context);
			}
			this.requestor.endReporting();
			if (this.monitor != null) this.monitor.done();
			reset();
		}
	}

	private boolean completeJavadocTagInModuleInfo(CompilationUnitDeclaration parsedUnit) {
		boolean contextAccepted = false;
		if (this.parser.assistNodeParent instanceof CompletionJavadoc && parsedUnit.isModuleInfo() ) {
			try {
				this.lookupEnvironment.buildTypeBindings(parsedUnit, null /*no access restriction*/);
				if(this.parser.assistNode instanceof CompletionOnJavadocTag) {
					((CompletionOnJavadocTag)this.parser.assistNode).filterPossibleTags(parsedUnit.scope);
				}
				throw new CompletionNodeFound(this.parser.assistNode, null, parsedUnit.scope);
			}
			catch (CompletionNodeFound e) {
				this.unitScope = parsedUnit.scope;
				if (e.astNode != null) {
					// if null then we found a problem in the completion node
					if(DEBUG) {
						System.out.print("COMPLETION - Completion node : "); //$NON-NLS-1$
						System.out.println(e.astNode.toString());
						if(this.parser.assistNodeParent != null) {
							System.out.print("COMPLETION - Parent Node : ");  //$NON-NLS-1$
							System.out.println(this.parser.assistNodeParent);
						}
					}
					this.lookupEnvironment.unitBeingCompleted = parsedUnit; // better resilient to further error reporting
					contextAccepted =
						complete(
							e.astNode,
							this.parser.assistNodeParent,
							this.parser.enclosingNode,
							parsedUnit,
							e.qualifiedBinding,
							e.scope,
							e.insideTypeAnnotation);
				}
			}
		}
		return contextAccepted;
	}

	private boolean checkForCNF(TypeReference ref, CompilationUnitDeclaration parsedUnit, boolean showAll) {
		this.lookupEnvironment.buildTypeBindings(parsedUnit, null);
		this.lookupEnvironment.completeTypeBindings(parsedUnit, true);
		parsedUnit.resolve();
		this.startPosition = ref.sourceStart;
		this.endPosition = ref.sourceEnd > ref.sourceStart ? ref.sourceEnd : ref.sourceStart;
		if ((this.unitScope = parsedUnit.scope) != null) {
			if (showAll) {
				char[][] tokens = ref.getTypeName();
				char[] typeName = CharOperation.concatWithAll(tokens, '.');
				if (typeName.length == 0) {
					buildContext(ref, null, parsedUnit, null, null);
					this.completionToken = new char[] {'*'};
					findTypesAndPackages(this.completionToken, this.unitScope, true, true, new ObjectVector());
					return true;
				}
			}
			parsedUnit.scope.faultInTypes();
		}
		return false; // should not come here - will throw exception
	}

	private boolean completeOnPackageVisibilityStatements(boolean contextAccepted,
			CompilationUnitDeclaration parsedUnit, PackageVisibilityStatement[] pvsStmts) {
		try {
			this.skipDefaultPackage = true;
			for (int i = 0, l = pvsStmts.length; i < l; ++i) {
				PackageVisibilityStatement pvs = pvsStmts[i];
				if (pvs instanceof CompletionOnKeywordModuleInfo) { // dummy pvs statement
					contextAccepted = true;
					processModuleKeywordCompletion(parsedUnit, pvs, (CompletionOnKeyword) pvs);
					return contextAccepted;
				}
				if (pvs.pkgRef instanceof CompletionOnPackageVisibilityReference) {
					contextAccepted = true;
					buildContext(pvs, null, parsedUnit, null, null);
					if(!this.requestor.isIgnored(CompletionProposal.PACKAGE_REF)) {
						findPackages((CompletionOnPackageVisibilityReference) pvs.pkgRef);
					}
					debugPrintf();
					return contextAccepted;
				}
				ModuleReference[] targets = pvs.targets;
				if (targets == null) continue;
				HashSet<String> skipSet = new HashSet<>();
				for (int j = 0, lj = targets.length; j < lj; j++) {
					ModuleReference target = targets[j];
					if (target == null) break;
					if (target instanceof CompletionOnModuleReference) {
						buildContext(target, null, parsedUnit, null, null);
						contextAccepted = true;
						if(!this.requestor.isIgnored(CompletionProposal.MODULE_REF)) {
							findTargettedModules((CompletionOnModuleReference) target, skipSet);
						}
						debugPrintf();
						return contextAccepted;
					} else if (target instanceof CompletionOnKeyword) {
						contextAccepted = true;
						processModuleKeywordCompletion(parsedUnit, target, (CompletionOnKeyword) target);
					} else {
					if (target.moduleName != null || target.moduleName == CharOperation.NO_CHAR)
							skipSet.add(new String(target.moduleName));
					}
				}
			}
		} finally {
			this.skipDefaultPackage = false;
		}
		return contextAccepted;
	}

	private void debugPrintf() {
		if(this.noProposal && this.problem != null) {
			this.requestor.completionFailure(this.problem);
			if(DEBUG) {
				this.printDebug(this.problem);
			}
		}
	}

	private void processModuleKeywordCompletion(CompilationUnitDeclaration parsedUnit, ASTNode node, CompletionOnKeyword keyword) {
		buildContext(node, null, parsedUnit, null, null);
		if(!this.requestor.isIgnored(CompletionProposal.KEYWORD)) {
			setSourceAndTokenRange(node.sourceStart, node.sourceEnd);
			findKeywords(keyword.getToken(), keyword.getPossibleKeywords(), false, parsedUnit.currentPackage != null);
		}
		debugPrintf();
	}

	public void complete(IType type, char[] snippet, int position, char[][] localVariableTypeNames, char[][] localVariableNames, int[] localVariableModifiers, boolean isStatic){
		if(this.requestor != null){
			this.requestor.beginReporting();
		}
		boolean contextAccepted = false;
		IType topLevelType = type;
		while(topLevelType.getDeclaringType() != null) {
			topLevelType = topLevelType.getDeclaringType();
		}

		this.fileName = topLevelType.getParent().getElementName().toCharArray();
		CompilationResult compilationResult = new CompilationResult(this.fileName, 1, 1, this.compilerOptions.maxProblemsPerUnit);

		CompilationUnitDeclaration compilationUnit = null;

		try {
			// TypeConverter is used instead of SourceTypeConverter because the type
			// to convert can be a binary type or a source type
			TypeDeclaration typeDeclaration = null;
			if (type instanceof SourceType) {
				SourceType sourceType = (SourceType) type;
				ISourceType info = (ISourceType) sourceType.getElementInfo();
				compilationUnit = SourceTypeConverter.buildCompilationUnit(
					new ISourceType[] {info},//sourceTypes[0] is always toplevel here
					SourceTypeConverter.FIELD_AND_METHOD // need field and methods
					| SourceTypeConverter.MEMBER_TYPE, // need member types
					// no need for field initialization
					this.problemReporter,
					compilationResult);
				if (compilationUnit != null && compilationUnit.types != null)
					typeDeclaration = compilationUnit.types[0];
			} else {
				compilationUnit = new CompilationUnitDeclaration(this.problemReporter, compilationResult, 0);
				typeDeclaration = new BinaryTypeConverter(this.parser.problemReporter(), compilationResult, null/*no need to remember type names*/).buildTypeDeclaration(type, compilationUnit);
			}

			if(typeDeclaration != null) {
				// build AST from snippet
				Initializer fakeInitializer = parseSnippeInitializer(snippet, position, localVariableTypeNames, localVariableNames, localVariableModifiers, isStatic);

				// merge AST
				FieldDeclaration[] oldFields = typeDeclaration.fields;
				FieldDeclaration[] newFields = null;
				if (oldFields != null) {
					newFields = new FieldDeclaration[oldFields.length + 1];
					System.arraycopy(oldFields, 0, newFields, 0, oldFields.length);
					newFields[oldFields.length] = fakeInitializer;
				} else {
					newFields = new FieldDeclaration[] {fakeInitializer};
				}
				typeDeclaration.fields = newFields;

				if(DEBUG) {
					System.out.println("SNIPPET COMPLETION AST :"); //$NON-NLS-1$
					System.out.println(compilationUnit.toString());
				}

				if (compilationUnit.types != null) {
					try {
						this.lookupEnvironment.buildTypeBindings(compilationUnit, null /*no access restriction*/);

						if ((this.unitScope = compilationUnit.scope) != null) {
							this.lookupEnvironment.completeTypeBindings(compilationUnit, true);
							compilationUnit.scope.faultInTypes();
							compilationUnit.resolve();
						}
					} catch (CompletionNodeFound e) {
						//					completionNodeFound = true;
						if (e.astNode != null) {
							// if null then we found a problem in the completion node
							contextAccepted =
								complete(
									e.astNode,
									this.parser.assistNodeParent,
									this.parser.enclosingNode,
									compilationUnit,
									e.qualifiedBinding,
									e.scope,
									e.insideTypeAnnotation);
						}
					}
				}
				if(this.noProposal && this.problem != null) {
					if(!contextAccepted) {
						contextAccepted = true;
						InternalCompletionContext context = new InternalCompletionContext();
						if (this.requestor.isExtendedContextRequired()) context.setExtended();
						this.requestor.acceptContext(context);
					}
					this.requestor.completionFailure(this.problem);
					if(DEBUG) {
						this.printDebug(this.problem);
					}
				}
			}
		}  catch (IndexOutOfBoundsException | InvalidCursorLocation | AbortCompilation | CompletionNodeFound e){ // internal failure - bugs 5618 (added with fix of 99629)
			if(DEBUG) {
				System.out.println("Exception caught by CompletionEngine:"); //$NON-NLS-1$
				e.printStackTrace(System.out);
			}
		} catch(JavaModelException e) {
			// Do nothing
		}
		if(!contextAccepted) {
			contextAccepted = true;
			InternalCompletionContext context = new InternalCompletionContext();
			if (this.requestor.isExtendedContextRequired()) context.setExtended();
			this.requestor.acceptContext(context);
		}
		if(this.requestor != null){
			this.requestor.endReporting();
		}
	}

	private void completionOnBreakStatement(CompletionOnBreakStatement astNode) {
		if (!this.requestor.isIgnored(CompletionProposal.LABEL_REF)) {
			this.completionToken = astNode.label;
			findLabels(this.completionToken, astNode.possibleLabels);
		}
	}

	private void completionOnContinueStatement(CompletionOnContinueStatement astNode) {
		if (!this.requestor.isIgnored(CompletionProposal.LABEL_REF)) {
			this.completionToken = astNode.label;
			findLabels(this.completionToken, astNode.possibleLabels);
		}
	}

	private void completionOnClassLiteralAccess(ASTNode astNode, Binding qualifiedBinding, Scope scope) {
		if (!this.requestor.isIgnored(CompletionProposal.FIELD_REF)) {
			CompletionOnClassLiteralAccess access = (CompletionOnClassLiteralAccess) astNode;
			setSourceAndTokenRange(access.classStart, access.sourceEnd);
			this.completionToken = access.completionIdentifier;
			findClassField(
					this.completionToken,
					(TypeBinding) qualifiedBinding,
					scope,
					null,
					null,
					null,
					false);
		}
	}

	private void completionOnExplicitConstructorCall(ASTNode astNode, Binding qualifiedBinding, Scope scope) {
		if (!this.requestor.isIgnored(CompletionProposal.METHOD_REF)) {
			setSourceAndTokenRange(astNode.sourceStart, astNode.sourceEnd, false);
			CompletionOnExplicitConstructorCall constructorCall = (CompletionOnExplicitConstructorCall) astNode;
			TypeBinding[] argTypes = computeTypes(constructorCall.arguments);
			findConstructors(
				(ReferenceBinding) qualifiedBinding,
				argTypes,
				scope,
				constructorCall,
				false,
				null,
				null,
				null,
				false);
		}
	}

	private void completionOnFieldName(ASTNode astNode, Scope scope) {
		if (!this.requestor.isIgnored(CompletionProposal.VARIABLE_DECLARATION)) {
			CompletionOnFieldName field = (CompletionOnFieldName) astNode;

			FieldBinding[] fields = scope.enclosingSourceType().fields();
			char[][] excludeNames = new char[fields.length][];
			for(int i = 0 ; i < fields.length ; i++){
				excludeNames[i] = fields[i].name;
			}

			this.completionToken = field.realName;


			int kind =
				 (field.modifiers & ClassFileConstants.AccStatic) == 0 ?
						InternalNamingConventions.VK_INSTANCE_FIELD :
							(field.modifiers & ClassFileConstants.AccFinal) == 0 ?
									InternalNamingConventions.VK_STATIC_FIELD :
										InternalNamingConventions.VK_STATIC_FINAL_FIELD;

			findVariableNames(field.realName, field.type, excludeNames, null, kind);
		}
	}

	private void completionOnFieldType(ASTNode astNode, Scope scope) {
		CompletionOnFieldType field = (CompletionOnFieldType) astNode;
		CompletionOnSingleTypeReference type = (CompletionOnSingleTypeReference) field.type;
		this.completionToken = type.token;
		setSourceAndTokenRange(type.sourceStart, type.sourceEnd);

		findTypesAndPackages(this.completionToken, scope, true, true, new ObjectVector());
		if (!this.requestor.isIgnored(CompletionProposal.KEYWORD)) {
			findKeywordsForMember(this.completionToken, field.modifiers, astNode);
		}

		if (!field.isLocalVariable && field.modifiers == ClassFileConstants.AccDefault) {
			SourceTypeBinding enclosingType = scope.enclosingSourceType();
			if (!enclosingType.isAnnotationType()) {
				if (!this.requestor.isIgnored(CompletionProposal.METHOD_DECLARATION)) {
					findMethodDeclarations(
							this.completionToken,
							enclosingType,
							scope,
							new ObjectVector(),
							null,
							null,
							null,
							false);
				}
				if (!this.requestor.isIgnored(CompletionProposal.POTENTIAL_METHOD_DECLARATION)) {
					proposeNewMethod(this.completionToken, enclosingType);
				}
			}
		}
	}
	//TODO
	private void completionOnJavadocAllocationExpression(ASTNode astNode, Binding qualifiedBinding, Scope scope) {
		// setSourceRange(astNode.sourceStart, astNode.sourceEnd, false);

		CompletionOnJavadocAllocationExpression allocExpression = (CompletionOnJavadocAllocationExpression) astNode;
		this.javadocTagPosition = allocExpression.tagSourceStart;
		int rangeStart = astNode.sourceStart;
		if (allocExpression.type.isThis()) {
			if (allocExpression.completeInText()) {
				rangeStart = allocExpression.separatorPosition;
			}
		} else if (allocExpression.completeInText()) {
			rangeStart = allocExpression.type.sourceStart;
		}
		setSourceAndTokenRange(rangeStart, astNode.sourceEnd, false);
		TypeBinding[] argTypes = computeTypes(allocExpression.arguments);

		ReferenceBinding ref = (ReferenceBinding) qualifiedBinding;
		if (!this.requestor.isIgnored(CompletionProposal.METHOD_REF) && ref.isClass()) {
			findConstructors(ref, argTypes, scope, allocExpression, false, null, null, null, false);
		}
	}
	//TODO
	private void completionOnJavadocFieldReference(ASTNode astNode, Scope scope) {
		this.insideQualifiedReference = true;
		CompletionOnJavadocFieldReference fieldRef = (CompletionOnJavadocFieldReference) astNode;
		this.completionToken = fieldRef.token;
		long completionPosition = fieldRef.nameSourcePosition;
		this.javadocTagPosition = fieldRef.tagSourceStart;

		if (fieldRef.actualReceiverType != null && fieldRef.actualReceiverType.isValidBinding()) {
				ReferenceBinding receiverType = (ReferenceBinding) fieldRef.actualReceiverType;
			int rangeStart = (int) (completionPosition >>> 32);
			if (fieldRef.receiver.isThis()) {
				if (fieldRef.completeInText()) {
					rangeStart = fieldRef.separatorPosition;
				}
			} else if (fieldRef.completeInText()) {
				rangeStart = fieldRef.receiver.sourceStart;
			}
			setSourceAndTokenRange(rangeStart, (int) completionPosition);

			if (!this.requestor.isIgnored(CompletionProposal.FIELD_REF)
					|| !this.requestor.isIgnored(CompletionProposal.JAVADOC_FIELD_REF)) {
				findFields(this.completionToken,
					receiverType,
					scope,
					new ObjectVector(),
					new ObjectVector(),
					false, /*not only static */
					fieldRef,
					scope,
					false,
					true,
					null,
					null,
					null,
					false,
					null,
					-1,
					-1);
			}

			if (!this.requestor.isIgnored(CompletionProposal.METHOD_REF)
					|| !this.requestor.isIgnored(CompletionProposal.JAVADOC_METHOD_REF)) {
				findMethods(
					this.completionToken,
					null,
					null,
					receiverType,
					scope,
					new ObjectVector(),
					false, /*not only static */
					false,
					fieldRef,
					scope,
					false,
					false,
					true,
					null,
					null,
					null,
					false,
					null,
					-1,
					-1);
				if (fieldRef.actualReceiverType instanceof ReferenceBinding) {
					ReferenceBinding refBinding = (ReferenceBinding)fieldRef.actualReceiverType;
					if (this.completionToken == null
							|| CharOperation.prefixEquals(this.completionToken, refBinding.sourceName)
							|| (this.options.camelCaseMatch && CharOperation.camelCaseMatch(this.completionToken, refBinding.sourceName))) {
						findConstructors(refBinding, null, scope, fieldRef, false, null, null, null, false);
					}
				}
			}
		}
	}
	//TODO
	private void completionOnJavadocMessageSend(ASTNode astNode, Binding qualifiedBinding, Scope scope) {
		CompletionOnJavadocMessageSend messageSend = (CompletionOnJavadocMessageSend) astNode;
		TypeBinding[] argTypes = null; //computeTypes(messageSend.arguments);
		this.completionToken = messageSend.selector;
		this.javadocTagPosition = messageSend.tagSourceStart;

		// Set source range
		int rangeStart = astNode.sourceStart;
		if (messageSend.receiver.isThis()) {
			if (messageSend.completeInText()) {
				rangeStart = messageSend.separatorPosition;
			}
		} else if (messageSend.completeInText()) {
			rangeStart = messageSend.receiver.sourceStart;
		}
		setSourceAndTokenRange(rangeStart, astNode.sourceEnd, false);

		if (qualifiedBinding == null) {
			if (!this.requestor.isIgnored(CompletionProposal.METHOD_REF)) {
				findImplicitMessageSends(this.completionToken, argTypes, scope, messageSend, scope, new ObjectVector());
			}
		} else if (!this.requestor.isIgnored(CompletionProposal.METHOD_REF)) {
			findMethods(
				this.completionToken,
				null,
				argTypes,
				(ReferenceBinding) ((ReferenceBinding) qualifiedBinding).capture(scope, messageSend.receiver.sourceStart, messageSend.receiver.sourceEnd),
				scope,
				new ObjectVector(),
				false,
				false/* prefix match */,
				messageSend,
				scope,
				false,
				messageSend.receiver instanceof SuperReference,
				true,
				null,
				null,
				null,
				false,
				null,
				-1,
				-1);
		}
	}
	//TODO
	private void completionOnJavadocParamNameReference(ASTNode astNode) {
		if (!this.requestor.isIgnored(CompletionProposal.JAVADOC_PARAM_REF)) {
			CompletionOnJavadocParamNameReference paramRef = (CompletionOnJavadocParamNameReference) astNode;
			setSourceAndTokenRange(paramRef.tagSourceStart, paramRef.tagSourceEnd);
			findJavadocParamNames(paramRef.token, paramRef.missingParams, false);
			findJavadocParamNames(paramRef.token, paramRef.missingTypeParams, true);
		}
	}
	private void completionOnJavadocQualifiedTypeReference(ASTNode astNode, Binding qualifiedBinding, Scope scope) {
		completionOnJavadocQualifiedTypeReference(astNode, qualifiedBinding, scope, true);
	}
	//TODO
	private void completionOnJavadocQualifiedTypeReference(ASTNode astNode, Binding qualifiedBinding, Scope scope, boolean findModules) {
		this.insideQualifiedReference = true;

		CompletionOnJavadocQualifiedTypeReference typeRef = (CompletionOnJavadocQualifiedTypeReference) astNode;
		this.completionToken = typeRef.completionIdentifier;
		long completionPosition = typeRef.sourcePositions[typeRef.tokens.length];
		this.javadocTagPosition = typeRef.tagSourceStart;

		// get the source positions of the completion identifier
		if (qualifiedBinding instanceof ReferenceBinding && !(qualifiedBinding instanceof TypeVariableBinding)) {
			if (!this.requestor.isIgnored(CompletionProposal.TYPE_REF) ||
					((this.assistNodeInJavadoc & CompletionOnJavadoc.TEXT) != 0 && !this.requestor.isIgnored(CompletionProposal.JAVADOC_TYPE_REF))) {
				int rangeStart = typeRef.completeInText() ? typeRef.sourceStart : (int) (completionPosition >>> 32);
				setSourceAndTokenRange(rangeStart, (int) completionPosition);
				findMemberTypes(
					this.completionToken,
					(ReferenceBinding) qualifiedBinding,
					scope,
					scope.enclosingSourceType(),
					false,
					false,
					new ObjectVector(),
					null,
					null,
					null,
					false);
			}
		} else if (qualifiedBinding instanceof PackageBinding) {

			setSourceRange(astNode.sourceStart, (int) completionPosition);
			int rangeStart = typeRef.completeInText() ? typeRef.sourceStart : (int) (completionPosition >>> 32);
			setTokenRange(rangeStart, (int) completionPosition);
			// replace to the end of the completion identifier
			findTypesAndSubpackages(this.completionToken, (PackageBinding) qualifiedBinding, scope);
		}
		if (findModules) {
			findModules(typeRef, false);
		}
	}
	//TODO
	private void completionOnJavadocSingleTypeReference(ASTNode astNode, Scope scope) {
		CompletionOnJavadocSingleTypeReference typeRef = (CompletionOnJavadocSingleTypeReference) astNode;
		this.completionToken = typeRef.token;
		this.javadocTagPosition = typeRef.tagSourceStart;
		setSourceAndTokenRange(typeRef.sourceStart, typeRef.sourceEnd);
		findTypesAndPackages(
				this.completionToken,
				scope,
				(this.assistNodeInJavadoc & CompletionOnJavadoc.BASE_TYPES) != 0,
				false,
				new ObjectVector());
		findModules(typeRef, false);
	}
	//TODO
	private void completionOnJavadocModuleReference(ASTNode astNode,  Binding qualifiedBinding, Scope scope) {
		CompletionOnJavadocModuleReference modRef = (CompletionOnJavadocModuleReference) astNode;
		this.completionToken = modRef.completionIdentifier;
		this.javadocTagPosition = modRef.tagSourceStart;
		setSourceAndTokenRange(modRef.sourceStart, modRef.sourceEnd);
		char[] completionName=  CharOperation.NO_CHAR;
		TypeReference tRef= modRef.getTypeReference();
		if (tRef instanceof CompletionOnJavadocSingleTypeReference) {
			CompletionOnJavadocSingleTypeReference typeRef = (CompletionOnJavadocSingleTypeReference) tRef;
			this.completionToken = typeRef.token;
			this.javadocTagPosition = typeRef.tagSourceStart;
			setSourceAndTokenRange(typeRef.sourceStart, typeRef.sourceEnd);
			completionName= this.completionToken;
			if (completionName.length == 1 && completionName[0] == '/') {
				completionName=  CharOperation.NO_CHAR;
			}
		} else if (tRef instanceof CompletionOnJavadocQualifiedTypeReference){
			completionOnJavadocQualifiedTypeReference(tRef, qualifiedBinding, scope, false);
			return;
		}
		boolean setIgnorePackageRef= true;
		try {
			if (this.requestor.isIgnored(CompletionProposal.PACKAGE_REF)) {
				setIgnorePackageRef = false;
				this.requestor.setIgnored(CompletionProposal.PACKAGE_REF, setIgnorePackageRef);
			}
			setSourceAndTokenRange(modRef.moduleReference.sourceStart, modRef.moduleReference.sourceEnd);
			findPackagesInModule(CharOperation.toLowerCase(completionName), modRef.moduleReference.binding, scope);
		} finally {
			if (!setIgnorePackageRef) {
				this.requestor.setIgnored(CompletionProposal.PACKAGE_REF, !setIgnorePackageRef);
			}
		}
	}
	//TODO
	private void completionOnJavadocTag(ASTNode astNode) {
		CompletionOnJavadocTag javadocTag = (CompletionOnJavadocTag) astNode;
		setSourceAndTokenRange(javadocTag.tagSourceStart, javadocTag.sourceEnd);
		findJavadocBlockTags(javadocTag);
		findJavadocInlineTags(javadocTag);
	}
	//TODO
	private void completionOnJavadocTypeParamReference(ASTNode astNode) {
		if (!this.requestor.isIgnored(CompletionProposal.JAVADOC_PARAM_REF)) {
			CompletionOnJavadocTypeParamReference paramRef = (CompletionOnJavadocTypeParamReference) astNode;
			setSourceAndTokenRange(paramRef.tagSourceStart, paramRef.tagSourceEnd);
			findJavadocParamNames(paramRef.token, paramRef.missingParams, true);
		}
	}

	private void completionOnKeyword(ASTNode astNode) {
		if (!this.requestor.isIgnored(CompletionProposal.KEYWORD)) {
			CompletionOnKeyword keyword = (CompletionOnKeyword)astNode;
			findKeywords(keyword.getToken(), keyword.getPossibleKeywords(), false, false);
		}
	}

	private void completionOnLocalOrArgumentName(ASTNode astNode, Scope scope) {
		if (!this.requestor.isIgnored(CompletionProposal.VARIABLE_DECLARATION)) {
			LocalDeclaration variable = (LocalDeclaration) astNode;

			int kind;
			if (variable instanceof CompletionOnLocalName){
				this.completionToken = ((CompletionOnLocalName) variable).realName;
				kind = InternalNamingConventions.VK_LOCAL;
			} else {
				CompletionOnArgumentName arg = (CompletionOnArgumentName) variable;
				this.completionToken = arg.realName;
				if ((variable.bits & ASTNode.IsTypeElided) != 0) {
					findKeywords(variable.name, new char[][] { Keywords.VAR }, false, false);
					return; // heuristics below need a type, which we don't have here
				}
				kind = arg.isCatchArgument ? InternalNamingConventions.VK_LOCAL : InternalNamingConventions.VK_PARAMETER;
			}

			char[][] alreadyDefinedName = computeAlreadyDefinedName((BlockScope)scope, variable);

			char[][] forbiddenNames = findVariableFromUnresolvedReference(variable, (BlockScope)scope, alreadyDefinedName);

			LocalVariableBinding[] locals = ((BlockScope)scope).locals;
			char[][] discouragedNames = new char[locals.length][];
			int localCount = 0;
			for(int i = 0 ; i < locals.length ; i++){
				if (locals[i] != null) {
					discouragedNames[localCount++] = locals[i].name;
				}
			}

			System.arraycopy(discouragedNames, 0, discouragedNames = new char[localCount][], 0, localCount);

			findVariableNames(this.completionToken, variable.type, discouragedNames, forbiddenNames, kind);
		}
	}

	private void completionOnMarkerAnnotationName(ASTNode astNode, Binding qualifiedBinding, Scope scope) {
		CompletionOnMarkerAnnotationName annot = (CompletionOnMarkerAnnotationName) astNode;

		// When completion is inside lambda body, the fake type cannot be attached to the lambda.
		ReferenceContext referenceContext = scope.parent.referenceContext();
		CompletionOnAnnotationOfType fakeType;
		if (referenceContext instanceof CompletionOnAnnotationOfType) {
			fakeType = (CompletionOnAnnotationOfType) referenceContext;
		} else {
			fakeType = new CompletionOnAnnotationOfType(CompletionParser.FAKE_TYPE_NAME, scope.referenceCompilationUnit().compilationResult, annot);
		}
		if (fakeType.annotations[0] == annot) {
			// When the completion is inside a method body the annotation cannot be accuratly attached to the correct node by completion recovery.
			// So 'targetedElement' is not computed in this case.
			if (scope.parent.parent == null || !(scope.parent.parent instanceof MethodScope)) {
				this.targetedElement = computeTargetedElement(fakeType);
			}

		}

		this.assistNodeIsAnnotation = true;
		if (annot.type instanceof CompletionOnSingleTypeReference) {
			CompletionOnSingleTypeReference type = (CompletionOnSingleTypeReference) annot.type;
			this.completionToken = type.token;
			setSourceAndTokenRange(type.sourceStart, type.sourceEnd);

			if (scope.parent.parent != null &&
					!(scope.parent.parent instanceof MethodScope) && !fakeType.isParameter) {

				if (this.completionToken.length <= Keywords.INTERFACE.length
					&& CharOperation.prefixEquals(this.completionToken, Keywords.INTERFACE, false /* ignore case */
				)){
					int relevance = computeBaseRelevance();
					relevance += computeRelevanceForResolution();
					relevance += computeRelevanceForInterestingProposal();
					relevance += computeRelevanceForCaseMatching(this.completionToken, Keywords.INTERFACE);
					relevance += computeRelevanceForRestrictions(IAccessRule.K_ACCESSIBLE); // no access restriction for keywords
					relevance += R_ANNOTATION; // this proposal is most relevant than annotation proposals

					this.noProposal = false;
					if(!this.requestor.isIgnored(CompletionProposal.KEYWORD)) {
						CompletionProposal proposal = createProposal(CompletionProposal.KEYWORD, this.actualCompletionPosition);
						proposal.setName(Keywords.INTERFACE);
						proposal.setCompletion(Keywords.INTERFACE);
						proposal.setReplaceRange(this.startPosition - this.offset, this.endPosition - this.offset);
						proposal.setTokenRange(this.tokenStart - this.offset, this.tokenEnd - this.offset);
						proposal.setRelevance(relevance);
						this.requestor.accept(proposal);
						if(DEBUG) {
							this.printDebug(proposal);
						}
					}
				}
			}

			findTypesAndPackages(this.completionToken, scope, false, false, new ObjectVector());
		} else if (annot.type instanceof CompletionOnQualifiedTypeReference) {
			this.insideQualifiedReference = true;

			CompletionOnQualifiedTypeReference type = (CompletionOnQualifiedTypeReference) annot.type;
			this.completionToken = type.completionIdentifier;
			long completionPosition = type.sourcePositions[type.tokens.length];
			if (qualifiedBinding instanceof PackageBinding) {

				setSourceRange(astNode.sourceStart, (int) completionPosition);
				setTokenRange((int) (completionPosition >>> 32), (int) completionPosition);
				// replace to the end of the completion identifier
				findTypesAndSubpackages(this.completionToken, (PackageBinding) qualifiedBinding, scope);
			} else {
				setSourceAndTokenRange((int) (completionPosition >>> 32), (int) completionPosition);

				findMemberTypes(
					this.completionToken,
					(ReferenceBinding) qualifiedBinding,
					scope,
					scope.enclosingSourceType(),
					false,
					false,
					new ObjectVector(),
					null,
					null,
					null,
					false);
			}
		}
	}

	private void completionOnMemberAccess(ASTNode astNode, ASTNode enclosingNode, Binding qualifiedBinding,
			Scope scope, boolean insideTypeAnnotation) {
		this.insideQualifiedReference = true;
		CompletionOnMemberAccess access = (CompletionOnMemberAccess) astNode;
		long completionPosition = access.nameSourcePosition;
		setSourceAndTokenRange((int) (completionPosition >>> 32), (int) completionPosition);

		this.completionToken = access.token;

		if (qualifiedBinding.problemId() == ProblemReasons.NotFound) {
			// complete method members with missing return type
			// class X {
			//   Missing f() {return null;}
			//   void foo() {
			//     f().|
			//   }
			// }
			if (this.assistNodeInJavadoc == 0 &&
					(this.requestor.isAllowingRequiredProposals(CompletionProposal.FIELD_REF, CompletionProposal.TYPE_REF) ||
							this.requestor.isAllowingRequiredProposals(CompletionProposal.METHOD_REF, CompletionProposal.TYPE_REF))) {
				ProblemMethodBinding problemMethodBinding = (ProblemMethodBinding) qualifiedBinding;
				findFieldsAndMethodsFromMissingReturnType(
						problemMethodBinding.selector,
						problemMethodBinding.parameters,
						scope,
						access,
						insideTypeAnnotation);
			}
		} else {
			if (!access.isInsideAnnotation) {
				if (!this.requestor.isIgnored(CompletionProposal.KEYWORD) && !access.isSuperAccess()) {
					findKeywords(this.completionToken, new char[][]{Keywords.NEW}, false, false);
				}

				ObjectVector fieldsFound = new ObjectVector();
				ObjectVector methodsFound = new ObjectVector();

				boolean superCall = access.receiver instanceof SuperReference;

				findFieldsAndMethods(
					this.completionToken,
					((TypeBinding) qualifiedBinding).capture(scope, access.receiver.sourceStart, access.receiver.sourceEnd),
					scope,
					fieldsFound,
					methodsFound,
					access,
					scope,
					false,
					superCall,
					null,
					null,
					null,
					false,
					null,
					-1,
					-1);

				if (!superCall) {

					checkCancel();

					findFieldsAndMethodsFromCastedReceiver(
							enclosingNode,
							qualifiedBinding,
							scope,
							fieldsFound,
							methodsFound,
							access,
							scope,
							access.receiver);
				}
			}
		}
	}

	private void completionOnMemberValueName(ASTNode astNode, ASTNode astNodeParent, Scope scope,
			boolean insideTypeAnnotation) {
		CompletionOnMemberValueName memberValuePair = (CompletionOnMemberValueName) astNode;
		Annotation annotation = (Annotation) astNodeParent;

		this.completionToken = memberValuePair.name;

		ReferenceBinding annotationType = (ReferenceBinding)annotation.resolvedType;

		if (annotationType != null && annotationType.isAnnotationType()) {
			if (!this.requestor.isIgnored(CompletionProposal.ANNOTATION_ATTRIBUTE_REF)) {
				findAnnotationAttributes(this.completionToken, annotation.memberValuePairs(), annotationType);
			}
			if (this.assistNodeCanBeSingleMemberAnnotation) {
				if (this.expectedTypesPtr > -1 && this.expectedTypes[0].isAnnotationType()) {
					findTypesAndPackages(this.completionToken, scope, false, false, new ObjectVector());
				} else {
					if (this.expectedTypesPtr > -1) {
						this.assistNodeIsEnum = true;
						done : for (int i = 0; i <= this.expectedTypesPtr; i++) {
							if (!this.expectedTypes[i].isEnum()) {
								this.assistNodeIsEnum = false;
								break done;
							}
						}

					}
					if (scope instanceof BlockScope && !this.requestor.isIgnored(CompletionProposal.LOCAL_VARIABLE_REF)) {
						char[][] alreadyDefinedName = computeAlreadyDefinedName((BlockScope)scope, FakeInvocationSite);

						findUnresolvedReference(
								memberValuePair.sourceStart,
								memberValuePair.sourceEnd,
								(BlockScope)scope,
								alreadyDefinedName);
					}
					findVariablesAndMethods(
						this.completionToken,
						scope,
						FakeInvocationSite,
						scope,
						insideTypeAnnotation,
						true);
					// can be the start of a qualified type name
					findTypesAndPackages(this.completionToken, scope, false, false, new ObjectVector());
				}
			}
		}
	}

	private void completionOnMessageSend(ASTNode astNode, Binding qualifiedBinding, Scope scope) {
		setSourceAndTokenRange(astNode.sourceStart, astNode.sourceEnd, false);

		CompletionOnMessageSend messageSend = (CompletionOnMessageSend) astNode;
		TypeBinding[] argTypes = computeTypes(messageSend.arguments);
		this.completionToken = messageSend.selector;
		ObjectVector methodsFound = new ObjectVector();
		if (qualifiedBinding == null) {
			if (!this.requestor.isIgnored(CompletionProposal.METHOD_REF)) {

				findImplicitMessageSends(this.completionToken, argTypes, scope, messageSend, scope, methodsFound);

				checkCancel();

				findLocalMethodsFromStaticImports(
						this.completionToken,
						scope,
						messageSend,
						scope,
						true,
						methodsFound,
						true);
			}
		} else  if (!this.requestor.isIgnored(CompletionProposal.METHOD_REF)) {
			findMethods(
				this.completionToken,
				null,
				argTypes,
				(ReferenceBinding)((ReferenceBinding) qualifiedBinding).capture(scope, messageSend.receiver.sourceStart, messageSend.receiver.sourceEnd),
				scope,
				methodsFound,
				false,
				true,
				messageSend,
				scope,
				false,
				messageSend.receiver instanceof SuperReference,
				false,
				null,
				null,
				null,
				false,
				null,
				-1,
				-1);
		}

		findCompletionsForArgumentPosition(methodsFound, argTypes != null ? argTypes.length : 0, scope);
	}

	private void findCompletionsForArgumentPosition(ObjectVector methodsFound, int completedArgumentLength, Scope scope) {
		if(methodsFound.size == 0) {
			return;
		}

		for(int i = 0; i < methodsFound.size; i++) {
			MethodBinding method = (MethodBinding) ((Object[])methodsFound.elementAt(i))[0];
			if(method.parameters.length <= completedArgumentLength) {
				continue;
			}

			TypeBinding paramType = method.parameters[completedArgumentLength];
			addExpectedType(paramType, scope);
		}
		this.strictMatchForExtepectedType = true;
		int filter = this.expectedTypesFilter;
		this.expectedTypesFilter = SUBTYPE;
		int start = this.startPosition, end = this.endPosition;
		int tStart = this.tokenStart, tEnd = this.tokenEnd;
		try {
			this.startPosition = this.endPosition = this.tokenStart = this.tokenEnd = this.actualCompletionPosition + 1;
			findVariablesAndMethods(CharOperation.NO_CHAR, scope, FakeInvocationSite, scope, false, false, false, methodsFound);
		} finally {
			this.startPosition = start;
			this.endPosition = end;
			this.tokenStart = tStart;
			this.tokenEnd = tEnd;
			this.strictMatchForExtepectedType = false;
			this.expectedTypesFilter = filter;
		}
	}

	private void completionOnMessageSendName(ASTNode astNode, Binding qualifiedBinding, Scope scope) {
		if (!this.requestor.isIgnored(CompletionProposal.METHOD_REF)) {
			CompletionOnMessageSendName messageSend = (CompletionOnMessageSendName) astNode;

			setTokenRange(messageSend.sourceStart, messageSend.sourceEnd);
			if (messageSend.statementEnd > messageSend.sourceStart)
				setSourceRange(messageSend.sourceStart, messageSend.statementEnd);

			this.completionToken = messageSend.selector;

			if (messageSend.nextIsCast) {
				// optionalPrefix|((String) s) was mistaken as a messageSend(?). Treat like beginning of statement.
				findVariablesAndMethods(this.completionToken, scope, messageSend, scope, false, false);
				findTypesAndPackages(this.completionToken, scope, true, false, new ObjectVector());
				return;
			}

			this.insideQualifiedReference = true;

			TypeBinding receiverType = (TypeBinding)qualifiedBinding;

			if(receiverType != null && receiverType instanceof ReferenceBinding) {
				TypeBinding[] typeArgTypes = computeTypesIfCorrect(messageSend.typeArguments);
				if(typeArgTypes != null) {
					findMethods(
							this.completionToken,
							typeArgTypes,
							null,
							(ReferenceBinding)receiverType.capture(scope, messageSend.receiver.sourceStart, messageSend.receiver.sourceEnd),
							scope,
							new ObjectVector(),
							messageSend.receiver.isType(),
							false,
							messageSend,
							scope,
							false,
							false,
							false,
							null,
							null,
							null,
							false,
							null,
							-1,
							-1);
				}
			}
		}
	}
	private void completionOnReferenceExpressionName(ASTNode astNode, Binding qualifiedBinding, Scope scope) {
		if (!this.requestor.isIgnored(CompletionProposal.METHOD_NAME_REFERENCE)) {
			CompletionOnReferenceExpressionName referenceExpression = (CompletionOnReferenceExpressionName) astNode;
			this.insideQualifiedReference = true;
			this.completionToken = referenceExpression.selector;
			boolean onlyStatic = false;

			TypeBinding receiverType = (TypeBinding) qualifiedBinding;
			if (receiverType != null && receiverType instanceof ReferenceBinding) {
				setSourceAndTokenRange(referenceExpression.nameSourceStart, referenceExpression.sourceEnd);
				if (!(receiverType.isInterface() || this.requestor.isIgnored(CompletionProposal.KEYWORD))) {
					this.assistNodeIsConstructor = true;
					findKeywords(this.completionToken, new char[][] { Keywords.NEW }, false, false);
				}
				findMethods(
						this.completionToken,
						referenceExpression.resolvedTypeArguments,
						null,
						(ReferenceBinding)receiverType.capture(scope, referenceExpression.sourceStart, referenceExpression.sourceEnd),
						scope,
						new ObjectVector(),
						onlyStatic,
						false,
						referenceExpression,
						scope,
						false,
						false,
						false,
						null,
						null,
						null,
						false,
						null,
						-1,
						-1);
			}
		}
	}


	private void completionOnMethodName(ASTNode astNode, Scope scope) {
		if (!this.requestor.isIgnored(CompletionProposal.VARIABLE_DECLARATION)) {
			CompletionOnMethodName method = (CompletionOnMethodName) astNode;

			setSourceAndTokenRange(method.sourceStart, method.selectorEnd);

			FieldBinding[] fields = scope.enclosingSourceType().fields();
			char[][] excludeNames = new char[fields.length][];
			for(int i = 0 ; i < fields.length ; i++){
				excludeNames[i] = fields[i].name;
			}

			this.completionToken = method.selector;


			int kind =
				 (method.modifiers & ClassFileConstants.AccStatic) == 0 ?
						InternalNamingConventions.VK_INSTANCE_FIELD :
							(method.modifiers & ClassFileConstants.AccFinal) == 0 ?
									InternalNamingConventions.VK_STATIC_FIELD :
										InternalNamingConventions.VK_STATIC_FINAL_FIELD;

			findVariableNames(this.completionToken, method.returnType, excludeNames, null, kind);
		}
	}

	private void completionOnMethodReturnType(ASTNode astNode, Scope scope) {
		CompletionOnMethodReturnType method = (CompletionOnMethodReturnType) astNode;
		SingleTypeReference type = (CompletionOnSingleTypeReference) method.returnType;
		this.completionToken = type.token;
		setSourceAndTokenRange(type.sourceStart, type.sourceEnd);
		findTypesAndPackages(this.completionToken, scope.parent, true, true, new ObjectVector());
		if (!this.requestor.isIgnored(CompletionProposal.KEYWORD)) {
			findKeywordsForMember(this.completionToken, method.modifiers, null);
		}

		if (method.modifiers == ClassFileConstants.AccDefault) {
			SourceTypeBinding enclosingType = scope.enclosingSourceType();
			if (!enclosingType.isAnnotationType()) {
				if (!this.requestor.isIgnored(CompletionProposal.METHOD_DECLARATION)) {
					findMethodDeclarations(
							this.completionToken,
							scope.enclosingSourceType(),
							scope,
							new ObjectVector(),
							null,
							null,
							null,
							false);
				}
				if (!this.requestor.isIgnored(CompletionProposal.POTENTIAL_METHOD_DECLARATION)) {
					proposeNewMethod(this.completionToken, scope.enclosingSourceType());
				}
			}
		}
	}

	private void completionOnParameterizedQualifiedTypeReference(ASTNode astNode, ASTNode astNodeParent, Binding qualifiedBinding, Scope scope) {
		if (!this.requestor.isIgnored(CompletionProposal.TYPE_REF)) {
			CompletionOnParameterizedQualifiedTypeReference ref = (CompletionOnParameterizedQualifiedTypeReference) astNode;

			this.insideQualifiedReference = true;

			this.assistNodeIsClass = ref.isClass();
			this.assistNodeIsException = ref.isException();
			this.assistNodeIsInterface = ref.isInterface();
			this.assistNodeIsSuperType = ref.isSuperType();
			this.assistNodeIsExtendedType = assistNodeIsExtendedType(astNode, astNodeParent);
			this.assistNodeIsInterfaceExcludingAnnotation = assistNodeIsInterfaceExcludingAnnotation(astNode, astNodeParent);

			this.completionToken = ref.completionIdentifier;
			long completionPosition = ref.sourcePositions[ref.tokens.length];
			setSourceAndTokenRange((int) (completionPosition >>> 32), (int) completionPosition);

			if (qualifiedBinding.problemId() == ProblemReasons.NotFound ||
					(((ReferenceBinding)qualifiedBinding).tagBits & TagBits.HasMissingType) != 0) {
				if (this.assistNodeInJavadoc == 0 &&
						(this.requestor.isAllowingRequiredProposals(CompletionProposal.TYPE_REF, CompletionProposal.TYPE_REF))) {
					if(ref.tokens.length == 1) {
						findMemberTypesFromMissingType(
								ref,
								ref.sourcePositions[0],
								scope);
					}
				}
			} else {
				ObjectVector typesFound = new ObjectVector();
				if (this.assistNodeIsException && astNodeParent instanceof TryStatement) {
					findExceptionFromTryStatement(
							this.completionToken,
							(ReferenceBinding)qualifiedBinding,
							scope.enclosingSourceType(),
							(BlockScope)scope,
							typesFound);
				}

				checkCancel();

				findMemberTypes(
					this.completionToken,
					(ReferenceBinding) qualifiedBinding,
					scope,
					scope.enclosingSourceType(),
					false,
					false,
					typesFound,
					null,
					null,
					null,
					false);
			}
		}
	}

	private boolean assistNodeIsExtendedType(ASTNode astNode, ASTNode astNodeParent) {
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=99399, don't propose final types for extension.
		if (astNodeParent == null)
			return false;
		if (astNodeParent instanceof TypeDeclaration) {
			TypeDeclaration typeDeclaration = (TypeDeclaration) astNodeParent;
			return (typeDeclaration.superclass == astNode);
		} else if (astNodeParent instanceof TypeParameter) {
			TypeParameter typeParameter = (TypeParameter) astNodeParent;
			return (typeParameter.type == astNode);
		} else if (astNodeParent instanceof Wildcard) {
			Wildcard wildcard = (Wildcard) astNodeParent;
			return (wildcard.bound == astNode && wildcard.kind == Wildcard.EXTENDS);
		}
		return false;
	}

	private boolean assistNodeIsInterfaceExcludingAnnotation(ASTNode astNode, ASTNode astNodeParent) {
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=310423, don't propose annotations for implements.
		if (astNodeParent == null)
			return false;
		if (astNodeParent instanceof TypeDeclaration) {
			TypeDeclaration typeDeclaration = (TypeDeclaration) astNodeParent;
			TypeReference [] superInterfaces = typeDeclaration.superInterfaces;
			int length = superInterfaces == null ? 0 : superInterfaces.length;
			for (int i = 0; i < length; i++) {
				if (superInterfaces[i] == astNode)
					return true;
			}
		}
		return false;
	}

	private boolean assistNodeIsInsideCase(ASTNode astNode, ASTNode astNodeParent) {
		// To find whether we're completing inside the case expression in a
		// switch case construct (https://bugs.eclipse.org/bugs/show_bug.cgi?id=195346)
		if (astNodeParent instanceof SwitchStatement) {
			CaseStatement[] cases = ((SwitchStatement) astNodeParent).cases;
			for (int i = 0, caseCount = ((SwitchStatement) astNodeParent).caseCount; i < caseCount; i++) {
				CompletionNodeDetector detector = new CompletionNodeDetector(astNode, cases[i]);
				if (detector.containsCompletionNode()) {
					return true;
				}
			}
		}
		return false;
	}

	private void completionOnQualifiedAllocationExpression(ASTNode astNode, Binding qualifiedBinding, Scope scope) {
		setSourceAndTokenRange(astNode.sourceStart, astNode.sourceEnd, false);

		CompletionOnQualifiedAllocationExpression allocExpression =
			(CompletionOnQualifiedAllocationExpression) astNode;
		TypeBinding[] argTypes = computeTypes(allocExpression.arguments);

		ReferenceBinding ref = (ReferenceBinding) qualifiedBinding;

		if (ref.problemId() == ProblemReasons.NotFound) {
			findConstructorsFromMissingType(
					allocExpression.type,
					argTypes,
					scope,
					allocExpression);
		} else {
			if (!this.requestor.isIgnored(CompletionProposal.METHOD_REF)
					&& ref.isClass()
					&& !ref.isAbstract()) {
					findConstructors(
						ref,
						argTypes,
						scope,
						allocExpression,
						false,
						null,
						null,
						null,
						false);
			}

			checkCancel();

			if (!this.requestor.isIgnored(CompletionProposal.ANONYMOUS_CLASS_DECLARATION)
					&& !ref.isFinal()
					&& !ref.isEnum()){
				findAnonymousType(
					ref,
					argTypes,
					scope,
					allocExpression,
					null,
					null,
					null,
					false);
			}
		}
	}

	private void completionOnQualifiedNameReference(ASTNode astNode, ASTNode enclosingNode, Binding qualifiedBinding,
			Scope scope, boolean insideTypeAnnotation) {
		this.insideQualifiedReference = true;
		CompletionOnQualifiedNameReference ref =
			(CompletionOnQualifiedNameReference) astNode;
		this.completionToken = ref.completionIdentifier;
		internalCompletionOnQualifiedReference(astNode, enclosingNode, qualifiedBinding, scope, insideTypeAnnotation,
				ref.isInsideAnnotationAttribute, ref, ref.tokens, ref.sourcePositions);
	}

	/** Unified handling for true QualifiedNameReference and misclassified QualifiedTypeReference. */
	private void internalCompletionOnQualifiedReference(ASTNode ref, ASTNode enclosingNode, Binding qualifiedBinding, Scope scope,
			boolean insideTypeAnnotation, boolean isInsideAnnotationAttribute, InvocationSite site, char[][] tokens, long[] sourcePositions)
	{
		long completionPosition = sourcePositions[sourcePositions.length - 1];
		if (qualifiedBinding.problemId() == ProblemReasons.NotFound) {
			setSourceAndTokenRange((int) (completionPosition >>> 32), (int) completionPosition);
			// complete field members with missing fields type
			// class X {
			//   Missing f;
			//   void foo() {
			//     f.|
			//   }
			// }
			if (this.assistNodeInJavadoc == 0 &&
					(this.requestor.isAllowingRequiredProposals(CompletionProposal.FIELD_REF, CompletionProposal.TYPE_REF) ||
							this.requestor.isAllowingRequiredProposals(CompletionProposal.METHOD_REF, CompletionProposal.TYPE_REF) ||
							this.requestor.isAllowingRequiredProposals(CompletionProposal.TYPE_REF, CompletionProposal.TYPE_REF))) {
				if(tokens.length == 1) {
					boolean foundSomeFields = findFieldsAndMethodsFromMissingFieldType(tokens[0], scope, site, insideTypeAnnotation);

					if (!foundSomeFields) {

						checkCancel();

						findMembersFromMissingType(
								tokens[0],
								sourcePositions[0],
								null,
								scope,
								site,
								isInsideAnnotationAttribute);
					}
				}
			}
		} else if (qualifiedBinding instanceof VariableBinding) {
			setSourceAndTokenRange((int) (completionPosition >>> 32), (int) completionPosition);
			TypeBinding receiverType = ((VariableBinding) qualifiedBinding).type;
			if (receiverType != null && (receiverType.tagBits & TagBits.HasMissingType) == 0) {
				ObjectVector fieldsFound = new ObjectVector();
				ObjectVector methodsFound = new ObjectVector();

				findFieldsAndMethods(
						this.completionToken,
						receiverType.capture(scope, ref.sourceStart, ref.sourceEnd),
						scope,
						fieldsFound,
						methodsFound,
						site,
						scope,
						false,
						false,
						null,
						null,
						null,
						false,
						null,
						-1,
						-1);

				checkCancel();

				if (ref instanceof Expression) {
					findFieldsAndMethodsFromCastedReceiver(
						enclosingNode,
						qualifiedBinding,
						scope,
						fieldsFound,
						methodsFound,
						site,
						scope,
						(Expression) ref);
				}

			} else if (this.assistNodeInJavadoc == 0 &&
					(this.requestor.isAllowingRequiredProposals(CompletionProposal.FIELD_REF, CompletionProposal.TYPE_REF) ||
							this.requestor.isAllowingRequiredProposals(CompletionProposal.METHOD_REF, CompletionProposal.TYPE_REF))) {
				boolean proposeField = !this.requestor.isIgnored(CompletionProposal.FIELD_REF);
				boolean proposeMethod = !this.requestor.isIgnored(CompletionProposal.METHOD_REF);
				if (proposeField || proposeMethod) {
					if(tokens.length == 1) {
						if (qualifiedBinding instanceof LocalVariableBinding) {
							// complete local variable members with missing variables type
							// class X {
							//   void foo() {
							//     Missing f;
							//     f.|
							//   }
							// }
							LocalVariableBinding localVariableBinding = (LocalVariableBinding) qualifiedBinding;
							findFieldsAndMethodsFromMissingType(
									localVariableBinding.declaration.type,
									localVariableBinding.declaringScope,
									site,
									scope);
						} else {
							// complete field members with missing fields type
							// class X {
							//   Missing f;
							//   void foo() {
							//     f.|
							//   }
							// }
							findFieldsAndMethodsFromMissingFieldType(tokens[0], scope, site, insideTypeAnnotation);
						}

					}
				}
			}

		} else if (qualifiedBinding instanceof ReferenceBinding && !(qualifiedBinding instanceof TypeVariableBinding)) {
			ReferenceBinding receiverType = (ReferenceBinding) qualifiedBinding;
			setSourceAndTokenRange((int) (completionPosition >>> 32), (int) completionPosition);

			findMembers(
					this.completionToken,
					receiverType,
					scope,
					site,
					isInsideAnnotationAttribute,
					null,
					null,
					null,
					false);

		} else if (qualifiedBinding instanceof PackageBinding) {

			setSourceRange(ref.sourceStart, (int) completionPosition);
			setTokenRange((int) (completionPosition >>> 32), (int) completionPosition);

			// replace to the end of the completion identifier
			findTypesAndSubpackages(this.completionToken, (PackageBinding) qualifiedBinding, scope);
		}
	}

	private void completionOnQualifiedTypeReference(ASTNode astNode, ASTNode astNodeParent, ASTNode enclosingNode, Binding qualifiedBinding,
			Scope scope) {
		this.insideQualifiedReference = true;

		CompletionOnQualifiedTypeReference ref =
			(CompletionOnQualifiedTypeReference) astNode;

		this.assistNodeIsClass = ref.isClass();
		this.assistNodeIsException = ref.isException();
		this.assistNodeIsInterface = ref.isInterface();
		this.assistNodeIsConstructor = ref.isConstructorType;
		this.assistNodeIsSuperType = ref.isSuperType();
		this.assistNodeIsExtendedType = assistNodeIsExtendedType(astNode, astNodeParent);
		this.assistNodeIsInterfaceExcludingAnnotation = assistNodeIsInterfaceExcludingAnnotation(astNode, astNodeParent);

		this.completionToken = ref.completionIdentifier;
		long completionPosition = ref.sourcePositions[ref.tokens.length];

		boolean haveTypeProposals = false;

		// get the source positions of the completion identifier
		if (qualifiedBinding.problemId() == ProblemReasons.NotFound) {
			setSourceAndTokenRange((int) (completionPosition >>> 32), (int) completionPosition);
			if (this.assistNodeInJavadoc == 0 &&
					(this.requestor.isAllowingRequiredProposals(CompletionProposal.TYPE_REF, CompletionProposal.TYPE_REF))) {
				if(ref.tokens.length == 1) {
					findMemberTypesFromMissingType(
							ref.tokens[0],
							ref.sourcePositions[0],
							scope);
				}
			}
		} else if (qualifiedBinding instanceof ReferenceBinding && !(qualifiedBinding instanceof TypeVariableBinding)) {
			ReferenceBinding receiverType = (ReferenceBinding) qualifiedBinding;
			if (!this.requestor.isIgnored(CompletionProposal.TYPE_REF)) {
				setSourceAndTokenRange((int) (completionPosition >>> 32), (int) completionPosition);

				ObjectVector typesFound = new ObjectVector();

				if (this.assistNodeIsException && astNodeParent instanceof TryStatement) {
					findExceptionFromTryStatement(
							this.completionToken,
							receiverType,
							scope.enclosingSourceType(),
							(BlockScope)scope,
							typesFound);
				}

				checkCancel();

				findMemberTypes(
					this.completionToken,
					receiverType,
					scope,
					scope.enclosingSourceType(),
					false,
					false,
					typesFound,
					null,
					null,
					null,
					false);
				haveTypeProposals = typesFound.size() > 0;
			}
		} else if (qualifiedBinding instanceof PackageBinding) {

			setSourceRange(astNode.sourceStart, (int) completionPosition);
			setTokenRange((int) (completionPosition >>> 32), (int) completionPosition);
			// replace to the end of the completion identifier
			findTypesAndSubpackages(this.completionToken, (PackageBinding) qualifiedBinding, scope);
		}
		// alternatively interpret tokens in a misclassified LocalDeclaration like a QualifiedNameReference:
		if (astNodeParent instanceof LocalDeclaration && enclosingNode != null) { // enclosingNode == null when called from completionOnProvidesInterfacesQualifiedTypeReference
			if (scope instanceof BlockScope) {
				// resolve tokens like it's done in CompletionOnQualifiedNameReference:
				qualifiedBinding = ((BlockScope) scope).getBinding(ref.tokens, FakeInvocationSite);
				boolean ignoreType = this.requestor.isIgnored(CompletionProposal.TYPE_REF);
				try {
					this.requestor.setIgnored(CompletionProposal.TYPE_REF, haveTypeProposals); // temp ignore types if already proposed above
					internalCompletionOnQualifiedReference(ref, enclosingNode, qualifiedBinding, scope, false, false, FakeInvocationSite, ref.tokens, ref.sourcePositions);
				} finally {
					this.requestor.setIgnored(CompletionProposal.TYPE_REF, ignoreType);
				}
			}
		}
	}

	private void completionOnProvidesInterfacesQualifiedTypeReference(ASTNode astNode, ASTNode astNodeParent, Binding qualifiedBinding, Scope scope) {
		// TODO: Filter the results wrt accessibility and add relevance to the results.
		completionOnQualifiedTypeReference(astNode, astNodeParent, null, qualifiedBinding, scope);
	}

	private void completionOnProvidesImplementationsQualifiedTypeReference(ASTNode astNode, ASTNode astNodeParent, Binding qualifiedBinding, Scope scope) {
		findImplementations((ProvidesStatement) this.parser.enclosingNode, (TypeReference) astNode);
	}

	private void completionOnSingleNameReference(ASTNode astNode, ASTNode astNodeParent, Scope scope,
			boolean insideTypeAnnotation) {
		CompletionOnSingleNameReference singleNameReference = (CompletionOnSingleNameReference) astNode;
		this.completionToken = singleNameReference.token;
		SwitchStatement switchStatement = astNodeParent instanceof SwitchStatement ? (SwitchStatement) astNodeParent : null;
		boolean isSwitchEnumOrType = false;
		if((switchStatement != null
				&& switchStatement.expression.resolvedType != null)) {
			TypeBinding resolvedType = switchStatement.expression.resolvedType;
			isSwitchEnumOrType = resolvedType.isEnum();
			if(!isSwitchEnumOrType) {
				if( this.compilerOptions.complianceLevel >= ClassFileConstants.JDK17)
					isSwitchEnumOrType = resolvedType.isClass() || resolvedType.isInterface() || resolvedType.isRecord();
			}

		}
		if (isSwitchEnumOrType) {
			TypeBinding resolvedType = switchStatement.expression.resolvedType;
			if(resolvedType.isEnum()) {
				if (!this.requestor.isIgnored(CompletionProposal.FIELD_REF)) {
					this.assistNodeIsEnum = true;
					findEnumConstantsFromSwithStatement(this.completionToken, (SwitchStatement) astNodeParent);
				}
			}
			else {
					// if switch with class/interface/record - J17 onwards
					char[][] keywords = new char[2][];
					int count = 0;
					if (switchStatement.defaultCase == null) {
						keywords[count++] = Keywords.DEFAULT;
					}
					if (switchStatement.nullCase == null) {
						keywords[count++] = Keywords.NULL;
					}
					System.arraycopy(keywords, 0, keywords = new char[count][], 0, count);
					findKeywords(this.completionToken, keywords, false, false);
				}

		} else if (this.expectedTypesPtr > -1 && this.expectedTypes[0].isAnnotationType()) {
			findTypesAndPackages(this.completionToken, scope, false, false, new ObjectVector());
			if (scope instanceof BlockScope && !this.requestor.isIgnored(CompletionProposal.LOCAL_VARIABLE_REF)) {
				findVariablesAndMethods(
					this.completionToken,
					scope,
					singleNameReference,
					scope,
					insideTypeAnnotation,
					singleNameReference.isInsideAnnotationAttribute);
			}
		} else {
			if (this.expectedTypesPtr > -1) {
				this.assistNodeIsEnum = true;
				done : for (int i = 0; i <= this.expectedTypesPtr; i++) {
					if (!this.expectedTypes[i].isEnum()) {
						this.assistNodeIsEnum = false;
						break done;
					}
				}

			}
			if (scope instanceof BlockScope && !this.requestor.isIgnored(CompletionProposal.LOCAL_VARIABLE_REF)) {
				char[][] alreadyDefinedName = computeAlreadyDefinedName((BlockScope)scope, singleNameReference);

				int end = Math.min(singleNameReference.sourceEnd, this.actualCompletionPosition);
				findUnresolvedReference(
						singleNameReference.sourceStart,
						end,
						(BlockScope)scope,
						alreadyDefinedName);
			}

			checkCancel();

			findVariablesAndMethods(
				this.completionToken,
				scope,
				singleNameReference,
				scope,
				insideTypeAnnotation,
				singleNameReference.isInsideAnnotationAttribute);

			checkCancel();

			// can be the start of a qualified type name
			findTypesAndPackages(this.completionToken, scope, true, false, new ObjectVector());
			if (!this.requestor.isIgnored(CompletionProposal.KEYWORD)) {
				if (this.completionToken != null && this.completionToken.length != 0) {
					findKeywords(this.completionToken, singleNameReference.possibleKeywords, false, false);
				} else {
					findTrueOrFalseKeywords(singleNameReference.possibleKeywords);
				}
			}
			if (singleNameReference.canBeExplicitConstructor && !this.requestor.isIgnored(CompletionProposal.METHOD_REF)){
				if (CharOperation.prefixEquals(this.completionToken, Keywords.THIS, false)) {
					ReferenceBinding ref = scope.enclosingSourceType();
					findExplicitConstructors(Keywords.THIS, ref, (MethodScope)scope, singleNameReference);
				} else if (CharOperation.prefixEquals(this.completionToken, Keywords.SUPER, false)) {
					ReferenceBinding ref = scope.enclosingSourceType();
					findExplicitConstructors(Keywords.SUPER, ref.superclass(), (MethodScope)scope, singleNameReference);
				}
			}
		}
	}

	private void completionOnSingleTypeReference(ASTNode astNode, ASTNode astNodeParent, Binding qualifiedBinding, Scope scope) {
		CompletionOnSingleTypeReference singleRef = (CompletionOnSingleTypeReference) astNode;

		this.completionToken = singleRef.token;

		this.assistNodeIsClass = singleRef.isClass();
		this.assistNodeIsException = singleRef.isException();
		this.assistNodeIsInterface = singleRef.isInterface();
		this.assistNodeIsConstructor = singleRef.isConstructorType;
		this.assistNodeIsSuperType = singleRef.isSuperType();
		this.assistNodeIsExtendedType = assistNodeIsExtendedType(astNode, astNodeParent);
		this.assistNodeIsInterfaceExcludingAnnotation = assistNodeIsInterfaceExcludingAnnotation(astNode, astNodeParent);

		// can be the start of a qualified type name
		if (qualifiedBinding == null) {
			if (this.completionToken.length == 0 &&
					(astNodeParent instanceof ParameterizedSingleTypeReference ||
							astNodeParent instanceof ParameterizedQualifiedTypeReference)) {
				this.setSourceAndTokenRange(astNode.sourceStart, astNode.sourceStart - 1, false);

				findParameterizedType((TypeReference)astNodeParent, scope);
			} else {
				ObjectVector typesFound = new ObjectVector();
				if (this.assistNodeIsException && astNodeParent instanceof TryStatement) {
					findExceptionFromTryStatement(
							this.completionToken,
							null,
							scope.enclosingSourceType(),
							(BlockScope)scope,
							typesFound);
				}
				else if ( astNodeParent instanceof InstanceOfExpression) {
					// propose final keyword
					if (!this.requestor.isIgnored(CompletionProposal.KEYWORD)) {
						findKeywordsForMember(this.completionToken, (~ClassFileConstants.AccFinal  & 0xFF), astNode);
					}
				}

				checkCancel();

				findTypesAndPackages(this.completionToken, scope, this.assistNodeIsConstructor, false, typesFound);
			}
		} else if (!this.requestor.isIgnored(CompletionProposal.TYPE_REF)) {
			findMemberTypes(
				this.completionToken,
				(ReferenceBinding) qualifiedBinding,
				scope,
				scope.enclosingSourceType(),
				false,
				false,
				false,
				false,
				!this.assistNodeIsConstructor,
				null,
				new ObjectVector(),
				null,
				null,
				null,
				false);
		}
		ASTNode parentNode = this.parser.assistNodeParent;
		if (parentNode instanceof LocalDeclaration && ((LocalDeclaration) parentNode).type == singleRef) {
			// additionally check if this identifier should be interpreted as the beginning of an expression rather than the type of a variable declaration.
			TypeBinding receiverType = scope.enclosingReceiverType();
			if (receiverType != null && receiverType.isValidBinding()) {
				findVariablesAndMethods(this.completionToken, scope, FakeInvocationSite, scope, false, false);
			}
			// ... or a keyword (possibly starting a new statement):
			if (!this.requestor.isIgnored(CompletionProposal.KEYWORD)) {
				if (this.completionToken != null && this.completionToken.length != 0) {
					findKeywords(this.completionToken, singleRef.possibleKeywords, false, false);
				} else {
					findTrueOrFalseKeywords(singleRef.possibleKeywords);
				}
			}
		}
	}

	private void completionOnProvidesInterfacesSingleTypeReference(ASTNode astNode, ASTNode astNodeParent, Binding qualifiedBinding, Scope scope) {
		completionOnSingleTypeReference(astNode, astNodeParent, qualifiedBinding, scope);
	}
	private void completionOnProvidesImplementationsSingleTypeReference(ASTNode astNode, ASTNode astNodeParent, Binding qualifiedBinding, Scope scope) {
		findImplementations((ProvidesStatement) this.parser.enclosingNode, (TypeReference) astNode);
		// TODO : filter the results - remove packs without a type in impl.
	}

	private char[][] computeAlreadyDefinedName(
			BlockScope scope,
			InvocationSite invocationSite) {
		ArrayList result = new ArrayList();

		boolean staticsOnly = false;

		Scope currentScope = scope;

		done1 : while (true) { // done when a COMPILATION_UNIT_SCOPE is found

			switch (currentScope.kind) {

				case Scope.METHOD_SCOPE :
					// handle the error case inside an explicit constructor call (see MethodScope>>findField)
					MethodScope methodScope = (MethodScope) currentScope;
					staticsOnly |= methodScope.isStatic | methodScope.isConstructorCall;

				//$FALL-THROUGH$
				case Scope.BLOCK_SCOPE :
					BlockScope blockScope = (BlockScope) currentScope;

					next : for (int i = 0, length = blockScope.locals.length; i < length; i++) {
						LocalVariableBinding local = blockScope.locals[i];

						if (local == null)
							break next;

						if (local.isSecret())
							continue next;

						result.add(local.name);
					}
					break;

				case Scope.CLASS_SCOPE :
					ClassScope classScope = (ClassScope) currentScope;
					SourceTypeBinding enclosingType = classScope.referenceContext.binding;
					computeAlreadyDefinedName(
							enclosingType,
							classScope,
							staticsOnly,
							invocationSite,
							result);
					staticsOnly |= enclosingType.isStatic();
					break;

				case Scope.COMPILATION_UNIT_SCOPE :
					break done1;
			}
			currentScope = currentScope.parent;
		}

		if (result.size() == 0) return CharOperation.NO_CHAR_CHAR;

		return (char[][])result.toArray(new char[result.size()][]);
	}

	private void computeAlreadyDefinedName(
			FieldBinding[] fields,
			Scope scope,
			boolean onlyStaticFields,
			ReferenceBinding receiverType,
			InvocationSite invocationSite,
			ArrayList result) {

		next : for (int f = fields.length; --f >= 0;) {
			FieldBinding field = fields[f];

			if (field.isSynthetic()) continue next;

			if (onlyStaticFields && !field.isStatic()) continue next;

			if (!field.canBeSeenBy(receiverType, invocationSite, scope)) continue next;

			result.add(field.name);
		}
	}

	private void computeAlreadyDefinedName(
			SourceTypeBinding receiverType,
			ClassScope scope,
			boolean onlyStaticFields,
			InvocationSite invocationSite,
			ArrayList result) {

		ReferenceBinding currentType = receiverType;
		ReferenceBinding[] interfacesToVisit = null;
		int nextPosition = 0;
		do {
			ReferenceBinding[] itsInterfaces = currentType.superInterfaces();
			if (itsInterfaces != Binding.NO_SUPERINTERFACES) {
				if (interfacesToVisit == null) {
					interfacesToVisit = itsInterfaces;
					nextPosition = interfacesToVisit.length;
				} else {
					int itsLength = itsInterfaces.length;
					if (nextPosition + itsLength >= interfacesToVisit.length)
						System.arraycopy(interfacesToVisit, 0, interfacesToVisit = new ReferenceBinding[nextPosition + itsLength + 5], 0, nextPosition);
					nextInterface : for (int a = 0; a < itsLength; a++) {
						ReferenceBinding next = itsInterfaces[a];
						for (int b = 0; b < nextPosition; b++)
							if (TypeBinding.equalsEquals(next, interfacesToVisit[b])) continue nextInterface;
						interfacesToVisit[nextPosition++] = next;
					}
				}
			}

			FieldBinding[] fields = currentType.availableFields();
			if(fields != null && fields.length > 0) {
				computeAlreadyDefinedName(
					fields,
					scope,
					onlyStaticFields,
					receiverType,
					invocationSite,
					result);
			}
			currentType = currentType.superclass();
		} while ( currentType != null);

		if (interfacesToVisit != null) {
			for (int i = 0; i < nextPosition; i++) {
				ReferenceBinding anInterface = interfacesToVisit[i];
				FieldBinding[] fields = anInterface.availableFields();
				if(fields !=  null) {
					computeAlreadyDefinedName(
						fields,
						scope,
						onlyStaticFields,
						receiverType,
						invocationSite,
						result);
				}

				ReferenceBinding[] itsInterfaces = anInterface.superInterfaces();
				if (itsInterfaces != Binding.NO_SUPERINTERFACES) {
					int itsLength = itsInterfaces.length;
					if (nextPosition + itsLength >= interfacesToVisit.length)
						System.arraycopy(interfacesToVisit, 0, interfacesToVisit = new ReferenceBinding[nextPosition + itsLength + 5], 0, nextPosition);
					nextInterface : for (int a = 0; a < itsLength; a++) {
						ReferenceBinding next = itsInterfaces[a];
						for (int b = 0; b < nextPosition; b++)
							if (TypeBinding.equalsEquals(next, interfacesToVisit[b])) continue nextInterface;
						interfacesToVisit[nextPosition++] = next;
					}
				}
			}
		}
	}

	int computeBaseRelevance(){
		return R_DEFAULT;
	}

	private void computeExpectedTypes(ASTNode parent, ASTNode node, Scope scope){

		// default filter
		this.expectedTypesFilter = SUBTYPE;
		this.hasJavaLangObjectAsExpectedType = false;

		// find types from parent
		if(parent instanceof AbstractVariableDeclaration && !(parent instanceof TypeParameter)) {
			AbstractVariableDeclaration variable = (AbstractVariableDeclaration)parent;
			TypeBinding binding = variable.type.resolvedType;
			if(binding != null) {
				if(!(variable.initialization instanceof ArrayInitializer)) {
					addExpectedType(binding, scope);
				} else { // https://bugs.eclipse.org/bugs/show_bug.cgi?id=310747
					// If the variable is of type X[], and we're in the initializer
					// we should have X as the expected type for the variable initializers.
					binding = binding.leafComponentType();
					addExpectedType(binding, scope);
				}
			}
		} else if(parent instanceof Assignment) {
			TypeBinding binding = ((Assignment)parent).lhs.resolvedType;
			if(binding != null) {
				addExpectedType(binding, scope);
			}
		} else if(parent instanceof ReturnStatement) {
			if(scope.methodScope().referenceContext instanceof AbstractMethodDeclaration) {
				MethodBinding methodBinding = ((AbstractMethodDeclaration) scope.methodScope().referenceContext).binding;
				TypeBinding binding = methodBinding  == null ? null : methodBinding.returnType;
				if(binding != null) {
					addExpectedType(binding, scope);
				}
			} else if (scope.methodScope().referenceContext instanceof LambdaExpression) {
				MethodBinding methodBinding = ((LambdaExpression) scope.methodScope().referenceContext).getMethodBinding();
				TypeBinding binding = methodBinding  == null ? null : methodBinding.returnType;
				if (binding != null) {
					addExpectedType(binding, scope);
				}
			}
		} else if (parent instanceof LambdaExpression) {
			LambdaExpression lambda = (LambdaExpression) parent;
			if (lambda.body == node) {
				if (lambda.binding != null) {
					TypeBinding returnType = lambda.binding.returnType;
					if (returnType != null) {
						addExpectedType(returnType, scope);
					}
				}
			}
		} else if(parent instanceof CastExpression) {
			TypeReference e = ((CastExpression)parent).type;
			TypeBinding binding = e.resolvedType;
			if(binding != null){
				addExpectedType(binding, scope);
				this.expectedTypesFilter = SUBTYPE | SUPERTYPE;
			}
		} else if(parent instanceof MessageSend) {
			MessageSend messageSend = (MessageSend) parent;

			if(messageSend.actualReceiverType instanceof ReferenceBinding) {
				ReferenceBinding binding = (ReferenceBinding)messageSend.actualReceiverType;
				boolean isStatic = messageSend.receiver.isTypeReference();

				while(binding != null) {
					computeExpectedTypesForMessageSend(
						binding,
						messageSend.selector,
						messageSend.arguments,
						(ReferenceBinding)messageSend.actualReceiverType,
						scope,
						messageSend,
						isStatic);
					computeExpectedTypesForMessageSendForInterface(
						binding,
						messageSend.selector,
						messageSend.arguments,
						(ReferenceBinding)messageSend.actualReceiverType,
						scope,
						messageSend,
						isStatic);
					binding = binding.superclass();
				}
			}
		} else if(parent instanceof AllocationExpression) {
			AllocationExpression allocationExpression = (AllocationExpression) parent;

			ReferenceBinding binding = (ReferenceBinding) allocationExpression.resolvedType;

			if(binding != null) {
				computeExpectedTypesForAllocationExpression(
					binding,
					allocationExpression.arguments,
					scope,
					allocationExpression);
			}
		} else if(parent instanceof OperatorExpression) {
			int operator = (parent.bits & ASTNode.OperatorMASK) >> ASTNode.OperatorSHIFT;
			if(parent instanceof ConditionalExpression) {
				// for future use
			} else if(parent instanceof InstanceOfExpression) {
				InstanceOfExpression e = (InstanceOfExpression) parent;
				TypeBinding binding = e.expression.resolvedType;
				if (binding == null) {
					if (scope instanceof BlockScope)
						binding = e.expression.resolveType((BlockScope) scope);
					else if (scope instanceof ClassScope)
						binding = e.expression.resolveType((ClassScope) scope);
				}
				if(binding != null){
					addExpectedType(binding, scope);
					this.expectedTypesFilter = SUBTYPE | SUPERTYPE;
				}
			} else if(parent instanceof BinaryExpression) {
				BinaryExpression binaryExpression = (BinaryExpression) parent;
				switch(operator) {
					case OperatorIds.EQUAL_EQUAL :
					case OperatorIds.NOT_EQUAL :
						// expected type is not relevant in this case
						TypeBinding binding = binaryExpression.left.resolvedType;
						if (binding != null) {
							addExpectedType(binding, scope);
							this.expectedTypesFilter = SUBTYPE | SUPERTYPE;
						}
						break;
					case OperatorIds.PLUS :
						addExpectedType(TypeBinding.SHORT, scope);
						addExpectedType(TypeBinding.INT, scope);
						addExpectedType(TypeBinding.LONG, scope);
						addExpectedType(TypeBinding.FLOAT, scope);
						addExpectedType(TypeBinding.DOUBLE, scope);
						addExpectedType(TypeBinding.CHAR, scope);
						addExpectedType(TypeBinding.BYTE, scope);
						addExpectedType(scope.getJavaLangString(), scope);
						break;
					case OperatorIds.AND_AND :
					case OperatorIds.OR_OR :
					case OperatorIds.XOR :
						addExpectedType(TypeBinding.BOOLEAN, scope);
						break;
					default :
						addExpectedType(TypeBinding.SHORT, scope);
						addExpectedType(TypeBinding.INT, scope);
						addExpectedType(TypeBinding.LONG, scope);
						addExpectedType(TypeBinding.FLOAT, scope);
						addExpectedType(TypeBinding.DOUBLE, scope);
						addExpectedType(TypeBinding.CHAR, scope);
						addExpectedType(TypeBinding.BYTE, scope);
						break;
				}
				if(operator == OperatorIds.LESS) {
					if(binaryExpression.left instanceof SingleNameReference){
						SingleNameReference name = (SingleNameReference) binaryExpression.left;
						Binding b = scope.getBinding(name.token, Binding.VARIABLE | Binding.TYPE, name, false);
						if(b instanceof ReferenceBinding) {
							TypeVariableBinding[] typeVariableBindings =((ReferenceBinding)b).typeVariables();
							if(typeVariableBindings != null && typeVariableBindings.length > 0) {
								addExpectedType(typeVariableBindings[0].firstBound, scope);
							}

						}
					}
				}
			} else if(parent instanceof UnaryExpression) {
				switch(operator) {
					case OperatorIds.NOT :
						addExpectedType(TypeBinding.BOOLEAN, scope);
						break;
					case OperatorIds.TWIDDLE :
						addExpectedType(TypeBinding.SHORT, scope);
						addExpectedType(TypeBinding.INT, scope);
						addExpectedType(TypeBinding.LONG, scope);
						addExpectedType(TypeBinding.CHAR, scope);
						addExpectedType(TypeBinding.BYTE, scope);
						break;
					case OperatorIds.PLUS :
					case OperatorIds.MINUS :
					case OperatorIds.PLUS_PLUS :
					case OperatorIds.MINUS_MINUS :
						addExpectedType(TypeBinding.SHORT, scope);
						addExpectedType(TypeBinding.INT, scope);
						addExpectedType(TypeBinding.LONG, scope);
						addExpectedType(TypeBinding.FLOAT, scope);
						addExpectedType(TypeBinding.DOUBLE, scope);
						addExpectedType(TypeBinding.CHAR, scope);
						addExpectedType(TypeBinding.BYTE, scope);
						break;
				}
			}
		} else if(parent instanceof ArrayReference) {
			addExpectedType(TypeBinding.SHORT, scope);
			addExpectedType(TypeBinding.INT, scope);
			addExpectedType(TypeBinding.LONG, scope);
		} else if(parent instanceof ParameterizedSingleTypeReference) {
			ParameterizedSingleTypeReference ref = (ParameterizedSingleTypeReference) parent;
			TypeBinding expected = null;
			if (this.parser.enclosingNode instanceof AbstractVariableDeclaration ||
					this.parser.enclosingNode instanceof ReturnStatement) {
				// completing inside the diamond
				if (this.parser.enclosingNode instanceof AbstractVariableDeclaration) {
					AbstractVariableDeclaration abstractVariableDeclaration = (AbstractVariableDeclaration) this.parser.enclosingNode;
					expected = abstractVariableDeclaration.initialization != null ? abstractVariableDeclaration.initialization.expectedType() : null;
				} else {
					ReturnStatement returnStatement = (ReturnStatement) this.parser.enclosingNode;
					if (returnStatement.expression != null) {
						expected = returnStatement.expression.expectedType();
					}
				}
				addExpectedType(expected, scope);
			} else {
				TypeVariableBinding[] typeVariables = ((ReferenceBinding)ref.resolvedType).typeVariables();
				int length = ref.typeArguments == null ? 0 : ref.typeArguments.length;
				if(typeVariables != null && typeVariables.length >= length) {
					int index = length - 1;
					while(index > -1 && ref.typeArguments[index] != node) index--;

					TypeBinding bound = typeVariables[index].firstBound;
					addExpectedType(bound == null ? scope.getJavaLangObject() : bound, scope);
				}
			}
		} else if(parent instanceof ParameterizedQualifiedTypeReference) {
			ParameterizedQualifiedTypeReference ref = (ParameterizedQualifiedTypeReference) parent;
			TypeReference[][] arguments = ref.typeArguments;
			TypeBinding expected = null;
			if (this.parser.enclosingNode instanceof AbstractVariableDeclaration ||
					this.parser.enclosingNode instanceof ReturnStatement) {
				// completing inside the diamond
				if (this.parser.enclosingNode instanceof AbstractVariableDeclaration) {
					AbstractVariableDeclaration abstractVariableDeclaration = (AbstractVariableDeclaration) this.parser.enclosingNode;
					expected = abstractVariableDeclaration.initialization != null ? abstractVariableDeclaration.initialization.expectedType() : null;
				} else {
					ReturnStatement returnStatement = (ReturnStatement) this.parser.enclosingNode;
					if (returnStatement.expression != null) {
						expected = returnStatement.expression.expectedType();
					}
				}
				addExpectedType(expected, scope);
			} else {
				TypeVariableBinding[] typeVariables = ((ReferenceBinding)ref.resolvedType).typeVariables();
				if(typeVariables != null) {
					int iLength = arguments == null ? 0 : arguments.length;
					done: for (int i = 0; i < iLength; i++) {
						int jLength = arguments[i] == null ? 0 : arguments[i].length;
						for (int j = 0; j < jLength; j++) {
							if(arguments[i][j] == node && typeVariables.length > j) {
								TypeBinding bound = typeVariables[j].firstBound;
								addExpectedType(bound == null ? scope.getJavaLangObject() : bound, scope);
								break done;
							}
						}
					}
				}
			}
		} else if(parent instanceof MemberValuePair) {
			MemberValuePair memberValuePair = (MemberValuePair) parent;
			if(memberValuePair.binding != null) {
				addExpectedType(memberValuePair.binding.returnType.leafComponentType(), scope);
			}
		} else if (parent instanceof NormalAnnotation) {
			NormalAnnotation annotation = (NormalAnnotation) parent;
			MemberValuePair[] memberValuePairs = annotation.memberValuePairs();
			if(memberValuePairs == null || memberValuePairs.length == 0) {
				if(annotation.resolvedType instanceof ReferenceBinding) {
					MethodBinding[] methodBindings =
						((ReferenceBinding)annotation.resolvedType).availableMethods();
					if (methodBindings != null &&
							methodBindings.length > 0 &&
							CharOperation.equals(methodBindings[0].selector, VALUE)) {
						boolean canBeSingleMemberAnnotation = true;
						done : for (int i = 1; i < methodBindings.length; i++) {
							if((methodBindings[i].modifiers & ClassFileConstants.AccAnnotationDefault) == 0) {
								canBeSingleMemberAnnotation = false;
								break done;
							}
						}
						if (canBeSingleMemberAnnotation) {
							this.assistNodeCanBeSingleMemberAnnotation = canBeSingleMemberAnnotation;
							addExpectedType(methodBindings[0].returnType.leafComponentType(), scope);
						}
					}
				}
			}
		} else if (parent instanceof AssistNodeParentAnnotationArrayInitializer) {
			AssistNodeParentAnnotationArrayInitializer parent1 = (AssistNodeParentAnnotationArrayInitializer) parent;
			if(parent1.type.resolvedType instanceof ReferenceBinding) {
				MethodBinding[] methodBindings =
					((ReferenceBinding)parent1.type.resolvedType).availableMethods();
				if (methodBindings != null) {
					for (MethodBinding methodBinding : methodBindings) {
						if(CharOperation.equals(methodBinding.selector, parent1.name)) {
							addExpectedType(methodBinding.returnType.leafComponentType(), scope);
							break;
						}
					}
				}
			}
		} else if (parent instanceof TryStatement) {
			boolean isException = false;
			if (node instanceof CompletionOnSingleTypeReference) {
				isException = ((CompletionOnSingleTypeReference)node).isException();
			} else if (node instanceof CompletionOnQualifiedTypeReference) {
				isException = ((CompletionOnQualifiedTypeReference)node).isException();
			} else if (node instanceof CompletionOnParameterizedQualifiedTypeReference) {
				isException = ((CompletionOnParameterizedQualifiedTypeReference)node).isException();
			}
			if (isException) {
				ThrownExceptionFinder thrownExceptionFinder = new ThrownExceptionFinder();
				thrownExceptionFinder.processThrownExceptions((TryStatement) parent, (BlockScope)scope);
				ReferenceBinding[] bindings = thrownExceptionFinder.getThrownUncaughtExceptions();
				ReferenceBinding[] alreadyCaughtExceptions = thrownExceptionFinder.getAlreadyCaughtExceptions();
				ReferenceBinding[] discouragedExceptions = thrownExceptionFinder.getDiscouragedExceptions();
				if (bindings != null && bindings.length > 0) {
					for (int i = 0; i < bindings.length; i++) {
						addExpectedType(bindings[i], scope);
					}
					this.expectedTypesFilter = SUPERTYPE;
				}
				if (alreadyCaughtExceptions != null && alreadyCaughtExceptions.length > 0) {
					for (int i = 0; i < alreadyCaughtExceptions.length; i++) {
						addForbiddenBindings(alreadyCaughtExceptions[i]);
						this.knownTypes.put(CharOperation.concat(alreadyCaughtExceptions[i].qualifiedPackageName(), alreadyCaughtExceptions[i].qualifiedSourceName(), '.'), KNOWN_TYPE_WITH_KNOWN_CONSTRUCTORS);
					}
				}
				if (discouragedExceptions != null && discouragedExceptions.length > 0) {
					for (int i = 0; i < discouragedExceptions.length; i++) {
						addUninterestingBindings(discouragedExceptions[i]);
						// do not insert into known types. We do need these types to come from
						// searchAllTypes(..) albeit with lower relevance
					}
				}
			}
		} else if (parent instanceof SwitchStatement) {
			SwitchStatement switchStatement = (SwitchStatement) parent;
			this.assistNodeIsInsideCase = assistNodeIsInsideCase(node, parent);
			if (switchStatement.expression != null &&
					switchStatement.expression.resolvedType != null) {
				if (this.assistNodeIsInsideCase &&
						switchStatement.expression.resolvedType.id == TypeIds.T_JavaLangString &&
						this.compilerOptions.complianceLevel >= ClassFileConstants.JDK1_7) {
					// set the field to true even though the expected types array will contain String as
					// expected type to avoid traversing the array in every case later on.
					// https://bugs.eclipse.org/bugs/show_bug.cgi?id=343476
					this.assistNodeIsString = true;
				}
				addExpectedType(switchStatement.expression.resolvedType, scope);
			}
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=253008, flag boolean as the expected
		// type if we are completing inside if(), for (; ;), while() and do while()
		} else if (parent instanceof WhileStatement) {  // covers both while and do-while loops
			addExpectedType(TypeBinding.BOOLEAN, scope);
		} else if (parent instanceof IfStatement) {
			addExpectedType(TypeBinding.BOOLEAN, scope);
		} else if (parent instanceof AssertStatement) {
			// https://bugs.eclipse.org/bugs/show_bug.cgi?id=274466
			// If the assertExpression is same as the node , then the assistNode is the conditional part of the assert statement
			AssertStatement assertStatement = (AssertStatement) parent;
			if (assertStatement.assertExpression == node) {
				addExpectedType(TypeBinding.BOOLEAN, scope);
			}
		} else if (parent instanceof ForStatement) {   // astNodeParent set to ForStatement only for the condition
			addExpectedType(TypeBinding.BOOLEAN, scope);

		// Expected types for javadoc
		} else if (parent instanceof Javadoc) {
			if (scope.kind == Scope.METHOD_SCOPE) {
				MethodScope methodScope = (MethodScope) scope;
				AbstractMethodDeclaration methodDecl = methodScope.referenceMethod();
				if (methodDecl != null && methodDecl.binding != null) {
					ReferenceBinding[] exceptions = methodDecl.binding.thrownExceptions;
					if (exceptions != null) {
						for (int i = 0; i < exceptions.length; i++) {
							addExpectedType(exceptions[i], scope);
						}
					}
				}
			}
		}

		// Guard it, otherwise we end up with a empty array which cause issues down the line
		if((this.expectedTypesPtr > -1) && ((this.expectedTypesPtr + 1) != this.expectedTypes.length)) {
			System.arraycopy(this.expectedTypes, 0, this.expectedTypes = new TypeBinding[this.expectedTypesPtr + 1], 0, this.expectedTypesPtr + 1);
		}
	}

	private void computeExpectedTypesForAllocationExpression(
		ReferenceBinding binding,
		Expression[] arguments,
		Scope scope,
		InvocationSite invocationSite) {

		if (arguments == null)
			return;

		MethodBinding[] methods = binding.availableMethods();
		nextMethod : for (int i = 0; i < methods.length; i++) {
			MethodBinding method = methods[i];

			if (!method.isConstructor()) continue nextMethod;

			if (method.isSynthetic()) continue nextMethod;

			if (this.options.checkVisibility && !method.canBeSeenBy(invocationSite, scope)) continue nextMethod;

			TypeBinding[] parameters = method.parameters;
			if(parameters.length < arguments.length)
				continue nextMethod;

			int length = arguments.length - 1;

			for (int j = 0; j < length; j++) {
				Expression argument = arguments[j];
				TypeBinding argType = argument.resolvedType;
				if(argType != null && !argType.isCompatibleWith(parameters[j]))
					continue nextMethod;
			}

			TypeBinding expectedType = method.parameters[arguments.length - 1];
			if(expectedType != null) {
				addExpectedType(expectedType, scope);
			}
		}
	}
	private void computeExpectedTypesForMessageSend(
		ReferenceBinding binding,
		char[] selector,
		Expression[] arguments,
		ReferenceBinding receiverType,
		Scope scope,
		InvocationSite invocationSite,
		boolean isStatic) {

		if (arguments == null)
			return;

		MethodBinding[] methods = binding.availableMethods();
		nextMethod : for (int i = 0; i < methods.length; i++) {
			MethodBinding method = methods[i];

			if (method.isSynthetic()) continue nextMethod;

			if (method.isDefaultAbstract())	continue nextMethod;

			if (method.isConstructor()) continue nextMethod;

			if (isStatic && !method.isStatic()) continue nextMethod;

			if (this.options.checkVisibility && !method.canBeSeenBy(receiverType, invocationSite, scope)) continue nextMethod;

			if(!CharOperation.equals(method.selector, selector)) continue nextMethod;

			TypeBinding[] parameters = method.parameters;
			if(parameters.length < arguments.length)
				continue nextMethod;

			int length = arguments.length - 1;
			int completionArgIndex = arguments.length - 1;

			for (int j = 0; j < length; j++) {
				Expression argument = arguments[j];
				TypeBinding argType = argument.resolvedType;
				if(argType != null && !argType.erasure().isCompatibleWith(parameters[j].erasure()))
					continue nextMethod;

				if((argument.sourceStart >= this.startPosition)
						&& (argument.sourceEnd <= this.endPosition)) {
					completionArgIndex = j;
				}
			}

			TypeBinding expectedType = method.parameters[completionArgIndex];
			if(expectedType != null) {
				addExpectedType(expectedType, scope);
			}
		}
	}
	private void computeExpectedTypesForMessageSendForInterface(
		ReferenceBinding binding,
		char[] selector,
		Expression[] arguments,
		ReferenceBinding receiverType,
		Scope scope,
		InvocationSite invocationSite,
		boolean isStatic) {

		ReferenceBinding[] itsInterfaces = binding.superInterfaces();
		if (itsInterfaces != Binding.NO_SUPERINTERFACES) {
			ReferenceBinding[] interfacesToVisit = itsInterfaces;
			int nextPosition = interfacesToVisit.length;

			for (int i = 0; i < nextPosition; i++) {
				ReferenceBinding currentType = interfacesToVisit[i];
				computeExpectedTypesForMessageSend(
					currentType,
					selector,
					arguments,
					receiverType,
					scope,
					invocationSite,
					isStatic);

				if ((itsInterfaces = currentType.superInterfaces()) != Binding.NO_SUPERINTERFACES) {
					int itsLength = itsInterfaces.length;
					if (nextPosition + itsLength >= interfacesToVisit.length)
						System.arraycopy(interfacesToVisit, 0, interfacesToVisit = new ReferenceBinding[nextPosition + itsLength + 5], 0, nextPosition);
					nextInterface : for (int a = 0; a < itsLength; a++) {
						ReferenceBinding next = itsInterfaces[a];
						for (int b = 0; b < nextPosition; b++)
							if (TypeBinding.equalsEquals(next, interfacesToVisit[b])) continue nextInterface;
						interfacesToVisit[nextPosition++] = next;
					}
				}
			}
		}
	}

	private Scope computeForbiddenBindings(ASTNode astNode, ASTNode astNodeParent, Scope scope) {
		if(scope instanceof ClassScope) {
			TypeDeclaration typeDeclaration = ((ClassScope)scope).referenceContext;
			if(typeDeclaration.superclass == astNode) {
				addForbiddenBindings(typeDeclaration.binding);
				addForbiddenBindingsForMemberTypes(typeDeclaration);
				return scope.parent;
			}
			TypeReference[] superInterfaces = typeDeclaration.superInterfaces;
			int length = superInterfaces == null ? 0 : superInterfaces.length;
			int astNodeIndex = -1;
			for (int i = 0; i < length; i++) {
				if(superInterfaces[i] == astNode) {
					addForbiddenBindings(typeDeclaration.binding);
					addForbiddenBindingsForMemberTypes(typeDeclaration);
					astNodeIndex = i;
					break;
				}
			}
			if (astNodeIndex >= 0) {
				// Need to loop only up to astNodeIndex as the rest will be undefined.
				for (int i = 0; i < astNodeIndex; i++) {
					addForbiddenBindings(superInterfaces[i].resolvedType);
				}
				return scope.parent;
			}
		}
//		else if(scope instanceof MethodScope) {
//			MethodScope methodScope = (MethodScope) scope;
//			if(methodScope.insideTypeAnnotation) {
//				return methodScope.parent.parent;
//			}
//		}
		return scope;
	}

	// https://bugs.eclipse.org/bugs/show_bug.cgi?id=270437
	private void addForbiddenBindingsForMemberTypes(TypeDeclaration typeDeclaration) {
		TypeDeclaration[] memberTypes = typeDeclaration.memberTypes;
		int memberTypesLen = memberTypes == null ? 0 : memberTypes.length;
		for (int i = 0; i < memberTypesLen; i++) {
			addForbiddenBindings(memberTypes[i].binding);
			addForbiddenBindingsForMemberTypes(memberTypes[i]);
		}
	}

	private char[] computePrefix(SourceTypeBinding declarationType, SourceTypeBinding invocationType, boolean isStatic){

		StringBuilder completion = new StringBuilder(10);

		if (isStatic) {
			completion.append(declarationType.sourceName());

		} else if (TypeBinding.equalsEquals(declarationType, invocationType)) {
			completion.append(THIS);

		} else {

			if (!declarationType.isNestedType()) {

				completion.append(declarationType.sourceName());
				completion.append('.');
				completion.append(THIS);

			} else if (!declarationType.isAnonymousType()) {

				completion.append(declarationType.sourceName());
				completion.append('.');
				completion.append(THIS);

			}
		}

		return completion.toString().toCharArray();
	}

	private int computeRelevanceForAnnotation(){
		if(this.assistNodeIsAnnotation) {
			return R_ANNOTATION;
		}
		return 0;
	}

	private int computeRelevanceForAnnotationTarget(TypeBinding typeBinding){
		if (this.assistNodeIsAnnotation &&
				(this.targetedElement & TagBits.AnnotationTargetMASK) != 0) {
			long target = typeBinding.getAnnotationTagBits() & TagBits.AnnotationTargetMASK;
			if(target == 0 || (target & this.targetedElement) != 0) {
				return R_TARGET;
			}
		}
		return 0;
	}
	int computeRelevanceForCaseMatching(char[] token, char[] proposalName){
		if(CharOperation.equals(token, proposalName, true)) {
			return R_EXACT_NAME + R_CASE;
		} else if(CharOperation.equals(token, proposalName, false)) {
			return R_EXACT_NAME;
		} else if (CharOperation.prefixEquals(token, proposalName, false)) {
			if (CharOperation.prefixEquals(token, proposalName, true))
				return R_CASE;
		} else if (this.options.camelCaseMatch && CharOperation.camelCaseMatch(token, proposalName)){
				return R_CAMEL_CASE;
		} else if (this.options.substringMatch && CharOperation.substringMatch(token, proposalName)) {
			return R_SUBSTRING;
		} else if (this.options.subwordMatch && CharOperation.subWordMatch(token, proposalName)) {
			return R_SUBWORD;
		}
		return 0;
	}

	private int computeRelevanceForClass(){
		if(this.assistNodeIsClass) {
			return R_CLASS;
		}
		return 0;
	}

	private int computeRelevanceForConstructor() {
		if (this.assistNodeIsConstructor) {
			return R_CONSTRUCTOR;
		}
		return 0;
	}

	private int computeRelevanceForEnum(){
		if(this.assistNodeIsEnum) {
			return R_ENUM;
		}
		return 0;
	}

	private int computeRelevanceForEnumConstant(TypeBinding proposalType){
		if(this.assistNodeIsEnum &&
				proposalType != null &&
				this.expectedTypes != null) {
			for (int i = 0; i <= this.expectedTypesPtr; i++) {
				if (proposalType.isEnum() &&
						TypeBinding.equalsEquals(proposalType, this.expectedTypes[i])) {
					return R_ENUM + R_ENUM_CONSTANT;
				}

			}
		}
		return 0;
	}

	private int computeRelevanceForException(){
		if (this.assistNodeIsException) {
			return R_EXCEPTION;
		}
		return 0;
	}

	private int computeRelevanceForException(char[] proposalName){

		if((this.assistNodeIsException || (this.assistNodeInJavadoc & CompletionOnJavadoc.EXCEPTION) != 0 )&&
			(CharOperation.match(EXCEPTION_PATTERN, proposalName, false) ||
			CharOperation.match(ERROR_PATTERN, proposalName, false))) {
			return R_EXCEPTION;
		}
		return 0;
	}

	private int computeRelevanceForExpectingType(char[] packageName, char[] typeName){
		if(this.expectedTypes != null) {
			for (int i = 0; i <= this.expectedTypesPtr; i++) {
				if(CharOperation.equals(this.expectedTypes[i].qualifiedPackageName(), packageName) &&
					CharOperation.equals(this.expectedTypes[i].qualifiedSourceName(), typeName)) {
					return R_EXACT_EXPECTED_TYPE;
				}
			}
			if(this.hasJavaLangObjectAsExpectedType) {
				return R_EXPECTED_TYPE;
			}
		}
		return 0;
	}

	private int computeRelevanceForExpectingType(TypeBinding proposalType){
		if(this.expectedTypes != null && proposalType != null) {
			int relevance = 0;
			// https://bugs.eclipse.org/bugs/show_bug.cgi?id=271296
			// If there is at least one expected type, then void proposal types attract a degraded relevance.
			if (proposalType == TypeBinding.VOID && this.expectedTypesPtr >=0) {
				return R_VOID;
			}
			for (int i = 0; i <= this.expectedTypesPtr; i++) {
				if((this.expectedTypesFilter & SUBTYPE) != 0
						&& (proposalType.erasure().isCompatibleWith(this.expectedTypes[i].erasure()))) {

					if(CharOperation.equals(this.expectedTypes[i].qualifiedPackageName(), proposalType.qualifiedPackageName()) &&
							CharOperation.equals(this.expectedTypes[i].qualifiedSourceName(), proposalType.qualifiedSourceName())) {
						return R_EXACT_EXPECTED_TYPE;
					}

					relevance = R_EXPECTED_TYPE;
				}
				if((this.expectedTypesFilter & SUPERTYPE) != 0
						&& this.expectedTypes[i].isCompatibleWith(proposalType)) {

					if(CharOperation.equals(this.expectedTypes[i].qualifiedPackageName(), proposalType.qualifiedPackageName()) &&
							CharOperation.equals(this.expectedTypes[i].qualifiedSourceName(), proposalType.qualifiedSourceName())) {
						return R_EXACT_EXPECTED_TYPE;
					}

					relevance = R_EXPECTED_TYPE;
				}
				// Bug 84720 - [1.5][assist] proposal ranking by return value should consider auto(un)boxing
				// Just ensuring that the unitScope is not null, even though it's an unlikely case.
				if (this.unitScope != null && this.unitScope.isBoxingCompatibleWith(proposalType, this.expectedTypes[i])) {
					relevance = R_EXPECTED_TYPE;
				}
			}
			return relevance;
		}
		return 0;
	}

	private int computeRelevanceForInheritance(ReferenceBinding receiverType, ReferenceBinding declaringClass) {
		if (TypeBinding.equalsEquals(receiverType, declaringClass)) return R_NON_INHERITED;
		return 0;
	}

	int computeRelevanceForInterestingProposal(){
		return computeRelevanceForInterestingProposal(null);
	}

	private int computeRelevanceForInterestingProposal(Binding binding){
		if(this.uninterestingBindings != null) {
			for (int i = 0; i <= this.uninterestingBindingsPtr; i++) {
				if(this.uninterestingBindings[i] == binding) {
					return 0;
				}
				if((this.uninterestingBindingsFilter & SUBTYPE) != 0) {
					if (binding instanceof TypeBinding &&
							this.uninterestingBindings[i] instanceof TypeBinding &&
							((TypeBinding)binding).isCompatibleWith((TypeBinding)this.uninterestingBindings[i])) {
						return 0;
					}
				}
				if ((this.uninterestingBindingsFilter & SUPERTYPE) != 0) {
					if (binding instanceof TypeBinding &&
							this.uninterestingBindings[i] instanceof TypeBinding &&
							((TypeBinding)this.uninterestingBindings[i]).isCompatibleWith((TypeBinding)binding)) {
						return 0;
					}
				}
			}
		}
		return R_INTERESTING;
	}

	private int computeRelevanceForInterestingProposal(char[] givenPkgName, char[] fullTypeName) {
		for (int i = 0; i <= this.uninterestingBindingsPtr; i++) {
			if (this.uninterestingBindings[i] instanceof TypeBinding) {
				TypeBinding typeBinding = (TypeBinding) this.uninterestingBindings[i];
				char[] currPkgName = typeBinding.qualifiedPackageName();
				if (CharOperation.equals(givenPkgName, currPkgName))	{
					char[] currTypeName = typeBinding.qualifiedSourceName();
					if (CharOperation.equals(fullTypeName, currTypeName)) {
						return 0;
					}
				}
			}
		}
		return R_INTERESTING;
	}

	private int computeRelevanceForInterface(){
		if(this.assistNodeIsInterface) {
			return R_INTERFACE;
		}
		return 0;
	}

	private int computeRelevanceForMissingElements(boolean hasProblems) {
		if (!hasProblems) {
			return R_NO_PROBLEMS;
		}
		return 0;
	}
	int computeRelevanceForQualification(boolean prefixRequired) {
		if(!prefixRequired && !this.insideQualifiedReference) {
			return R_UNQUALIFIED;
		}

		if(prefixRequired && this.insideQualifiedReference) {
			return R_QUALIFIED;
		}
		return 0;
	}

	int computeRelevanceForResolution(){
		return computeRelevanceForResolution(true);
	}

	int computeRelevanceForResolution(boolean isResolved){
		if (isResolved) {
			return R_RESOLVED;
		}
		return 0;
	}

	int computeRelevanceForRestrictions(int accessRuleKind) {
		if(accessRuleKind == IAccessRule.K_ACCESSIBLE) {
			return R_NON_RESTRICTED;
		}
		return 0;
	}

	private int computeRelevanceForStatic(boolean onlyStatic, boolean isStatic) {
		if(this.insideQualifiedReference && !onlyStatic && !isStatic) {
			return R_NON_STATIC;
		}
		return 0;
	}

	private int computeRelevanceForFinal(boolean onlyFinal, boolean isFinal) {
		if (onlyFinal && isFinal) {
			return R_FINAL;
		}
		return 0;
	}

	private int computeRelevanceForSuper(MethodBinding method, Scope scope, InvocationSite site) {
		if (site instanceof CompletionOnMemberAccess) {
			CompletionOnMemberAccess access = (CompletionOnMemberAccess) site;
			if (access.isSuperAccess() && this.parser.assistNodeParent == null) {
				ReferenceContext referenceContext = scope.referenceContext();
				if (referenceContext instanceof AbstractMethodDeclaration) {  // LE is anonymous.
					MethodBinding binding = ((AbstractMethodDeclaration) referenceContext).binding;
					if (binding != null) {
						if (CharOperation.equals(binding.selector, method.selector)) {
							if (binding.areParameterErasuresEqual(method)) {
								return R_EXACT_NAME + R_METHOD_OVERIDE;
							}
							return R_EXACT_NAME;
						}
					}
				}
			}
		}
		return 0;
	}

	private long computeTargetedElement(CompletionOnAnnotationOfType fakeNode) {
		ASTNode annotatedElement = fakeNode.potentialAnnotatedNode;

		if (annotatedElement instanceof TypeDeclaration) {
			TypeDeclaration annotatedTypeDeclaration = (TypeDeclaration) annotatedElement;
			if (TypeDeclaration.kind(annotatedTypeDeclaration.modifiers) == TypeDeclaration.ANNOTATION_TYPE_DECL) {
				return TagBits.AnnotationForAnnotationType | TagBits.AnnotationForType | TagBits.AnnotationForTypeUse;
			}
			return TagBits.AnnotationForType | TagBits.AnnotationForTypeUse;
		} else if (annotatedElement instanceof FieldDeclaration) {
			if (fakeNode.isParameter) {
				return TagBits.AnnotationForParameter;
			}
			return TagBits.AnnotationForField;
		} else if (annotatedElement instanceof MethodDeclaration) {
			return TagBits.AnnotationForMethod;
		} else if (annotatedElement instanceof Argument) {
			return TagBits.AnnotationForParameter;
		} else if (annotatedElement instanceof ConstructorDeclaration) {
			return TagBits.AnnotationForConstructor;
		} else if (annotatedElement instanceof LocalDeclaration) {
			return TagBits.AnnotationForLocalVariable;
		} else if (annotatedElement instanceof ImportReference) {
			return TagBits.AnnotationForPackage;
		}
		return 0;
	}
	private TypeBinding[] computeTypes(Expression[] arguments) {
		if (arguments == null) return null;
		int argsLength = arguments.length;
		TypeBinding[] argTypes = new TypeBinding[argsLength];
		for (int a = argsLength; --a >= 0;) {
			argTypes[a] = arguments[a].resolvedType;
		}
		return argTypes;
	}

	private TypeBinding[] computeTypesIfCorrect(Expression[] arguments) {
		if (arguments == null) return Binding.NO_TYPES;
		int argsLength = arguments.length;
		TypeBinding[] argTypes = new TypeBinding[argsLength];
		for (int a = argsLength; --a >= 0;) {
			TypeBinding typeBinding = arguments[a].resolvedType;
			if(typeBinding == null || !typeBinding.isValidBinding()) return null;
			argTypes[a] = typeBinding;
		}
		return argTypes;
	}

	private void computeUninterestingBindings(ASTNode astNode, ASTNode parent, Scope scope){
		this.uninterestingBindingsFilter = NONE;
		if(parent instanceof LocalDeclaration) {
			addUninterestingBindings(((LocalDeclaration)parent).binding);
		} else if (parent instanceof FieldDeclaration) {
			addUninterestingBindings(((FieldDeclaration)parent).binding);
		} else if (parent instanceof TryStatement) {
			boolean isException = false;
			if (astNode instanceof CompletionOnSingleTypeReference) {
				isException = ((CompletionOnSingleTypeReference)astNode).isException();
			} else if (astNode instanceof CompletionOnQualifiedTypeReference) {
				isException = ((CompletionOnQualifiedTypeReference)astNode).isException();
			} else if (astNode instanceof CompletionOnParameterizedQualifiedTypeReference) {
				isException = ((CompletionOnParameterizedQualifiedTypeReference)astNode).isException();
			}
			if (isException) {
				this.uninterestingBindingsFilter |= SUBTYPE;
				// super-types also need to be discouraged if we're in a union type (bug 350652)
				Argument[] args = ((TryStatement)parent).catchArguments;
				for (int i = 0; i < args.length; i++) {
					if (args[i].type instanceof UnionTypeReference) {
						CompletionNodeDetector detector = new CompletionNodeDetector(astNode, args[i]);
						if (detector.containsCompletionNode()) {
							this.uninterestingBindingsFilter |= SUPERTYPE;
							break;
						}
					}
				}

			}
		}
	}

	private char[] createImportCharArray(char[] importedElement, boolean isStatic, boolean onDemand) {
		char[] result = IMPORT;
		if (isStatic) {
			result = CharOperation.concat(result, STATIC, ' ');
		}
		result = CharOperation.concat(result, importedElement, ' ');
		if (onDemand) {
			result = CharOperation.concat(result, ON_DEMAND);
		}
		return CharOperation.concat(result, IMPORT_END);
	}

	private void createMethod(MethodBinding method, char[][] parameterPackageNames, char[][] parameterTypeNames, char[][] parameterNames, Scope scope, StringBuffer completion) {
		//// Modifiers
		// flush uninteresting modifiers
		int insertedModifiers = method.modifiers & ~(ClassFileConstants.AccNative | ClassFileConstants.AccAbstract);
		if(insertedModifiers != ClassFileConstants.AccDefault){
			ASTNode.printModifiers(insertedModifiers, completion);
		}

		//// Type parameters

		TypeVariableBinding[] typeVariableBindings = method.typeVariables;
		if(typeVariableBindings != null && typeVariableBindings.length != 0) {
			completion.append('<');
			for (int i = 0; i < typeVariableBindings.length; i++) {
				if(i != 0) {
					completion.append(',');
					completion.append(' ');
				}
				createTypeVariable(typeVariableBindings[i], scope, completion);
			}
			completion.append('>');
			completion.append(' ');
		}

		//// Return type
		createType(method.returnType, scope, completion);
		completion.append(' ');

		//// Selector
		completion.append(method.selector);

		completion.append('(');

		////Parameters
		TypeBinding[] parameterTypes = method.parameters;
		int length = parameterTypes.length;
		for (int i = 0; i < length; i++) {
			if(i != 0) {
				completion.append(',');
				completion.append(' ');
			}
			createType(parameterTypes[i], scope, completion);
			completion.append(' ');
			if(parameterNames != null){
				completion.append(parameterNames[i]);
			} else {
				completion.append('%');
			}
		}

		completion.append(')');

		//// Exceptions
		ReferenceBinding[] exceptions = method.thrownExceptions;

		if (exceptions != null && exceptions.length > 0){
			completion.append(' ');
			completion.append(THROWS);
			completion.append(' ');
			for(int i = 0; i < exceptions.length ; i++){
				if(i != 0) {
					completion.append(' ');
					completion.append(',');
				}
				createType(exceptions[i], scope, completion);
			}
		}
	}

	protected InternalCompletionProposal createProposal(int kind, int completionOffset) {
		InternalCompletionProposal proposal = (InternalCompletionProposal) CompletionProposal.create(kind, completionOffset - this.offset);
		proposal.nameLookup = this.nameEnvironment.nameLookup;
		proposal.completionEngine = this;
		return proposal;
	}

	private CompletionProposal createRequiredTypeProposal(Binding binding, int start, int end, int relevance) {
		InternalCompletionProposal proposal = null;
		if (binding instanceof ReferenceBinding) {
			ReferenceBinding typeBinding = (ReferenceBinding) binding;

			char[] packageName = typeBinding.qualifiedPackageName();
			char[] typeName = typeBinding.qualifiedSourceName();
			char[] fullyQualifiedName = CharOperation.concat(packageName, typeName, '.');

			proposal = createProposal(CompletionProposal.TYPE_REF, this.actualCompletionPosition);
			proposal.nameLookup = this.nameEnvironment.nameLookup;
			proposal.completionEngine = this;
			proposal.setDeclarationSignature(packageName);
			proposal.setSignature(getRequiredTypeSignature(typeBinding));
			proposal.setPackageName(packageName);
			proposal.setTypeName(typeName);
			proposal.setCompletion(fullyQualifiedName);
			proposal.setFlags(typeBinding.modifiers);
			proposal.setReplaceRange(start - this.offset, end - this.offset);
			proposal.setTokenRange(start - this.offset, end - this.offset);
			proposal.setRelevance(relevance);
		} else if (binding instanceof PackageBinding) {
			PackageBinding packageBinding = (PackageBinding) binding;

			char[] packageName = CharOperation.concatWith(packageBinding.compoundName, '.');

			proposal = createProposal(CompletionProposal.PACKAGE_REF, this.actualCompletionPosition);
			proposal.setDeclarationSignature(packageName);
			proposal.setPackageName(packageName);
			proposal.setCompletion(packageName);
			proposal.setReplaceRange(start - this.offset, end - this.offset);
			proposal.setTokenRange(start - this.offset, end - this.offset);
			proposal.setRelevance(relevance);
		}
		return proposal;
	}

	private void createType(TypeBinding type, Scope scope, StringBuffer completion) {
		switch (type.kind()) {
			case Binding.BASE_TYPE :
				completion.append(type.sourceName());
				break;
			case Binding.WILDCARD_TYPE :
			case Binding.INTERSECTION_TYPE : // TODO (david) need to handle intersection type specifically
				WildcardBinding wildcardBinding = (WildcardBinding) type;
				completion.append('?');
				switch (wildcardBinding.boundKind) {
					case Wildcard.EXTENDS:
						completion.append(' ');
						completion.append(EXTENDS);
						completion.append(' ');
						createType(wildcardBinding.bound, scope, completion);
						if(wildcardBinding.otherBounds != null) {

							int length = wildcardBinding.otherBounds.length;
							for (int i = 0; i < length; i++) {
								completion.append(' ');
								completion.append('&');
								completion.append(' ');
								createType(wildcardBinding.otherBounds[i], scope, completion);
							}
						}
						break;
					case Wildcard.SUPER:
						completion.append(' ');
						completion.append(SUPER);
						completion.append(' ');
						createType(wildcardBinding.bound, scope, completion);
						break;
				}
				break;
			case Binding.ARRAY_TYPE :
				createType(type.leafComponentType(), scope, completion);
				int dim = type.dimensions();
				for (int i = 0; i < dim; i++) {
					completion.append('[');
					completion.append(']');
				}
				break;
			case Binding.PARAMETERIZED_TYPE :
				ParameterizedTypeBinding parameterizedType = (ParameterizedTypeBinding) type;
				if (type.isMemberType()) {
					createType(parameterizedType.enclosingType(), scope, completion);
					completion.append('.');
					completion.append(parameterizedType.sourceName);
				} else {
					completion.append(CharOperation.concatWith(parameterizedType.genericType().compoundName, '.'));
				}
				if (parameterizedType.arguments != null) {
					completion.append('<');
				    for (int i = 0, length = parameterizedType.arguments.length; i < length; i++) {
				        if (i != 0) completion.append(',');
				        createType(parameterizedType.arguments[i], scope, completion);
				    }
				    completion.append('>');
				}
				break;
			default :
				char[] packageName = type.qualifiedPackageName();
			char[] typeName = type.qualifiedSourceName();
			if(mustQualifyType(
					(ReferenceBinding)type,
					packageName,
					scope)) {
				completion.append(CharOperation.concat(packageName, typeName,'.'));
			} else {
				completion.append(type.sourceName());
			}
			break;
		}
	}

	/*
	 * Create a completion proposal for a member type.
	 */
	private void createTypeParameterProposal(TypeParameter typeParameter, int relevance) {
		char[] completionName = typeParameter.name;

		// Create standard type proposal
		if(!this.requestor.isIgnored(CompletionProposal.TYPE_REF)) {
			InternalCompletionProposal proposal = (InternalCompletionProposal) CompletionProposal.create(CompletionProposal.TYPE_REF, this.actualCompletionPosition - this.offset);
			proposal.nameLookup = this.nameEnvironment.nameLookup;
			proposal.completionEngine = this;
			proposal.setSignature(getSignature(typeParameter.binding));
			proposal.setTypeName(completionName);
			proposal.setCompletion(completionName);
			proposal.setFlags(typeParameter.modifiers);
			proposal.setReplaceRange(this.startPosition - this.offset, this.endPosition - this.offset);
			proposal.setTokenRange(this.tokenStart - this.offset, this.tokenEnd - this.offset);
			proposal.setRelevance(relevance);
			this.requestor.accept(proposal);
			if(DEBUG) {
				this.printDebug(proposal);
			}
		}

		// Create javadoc text proposal if necessary
		if ((this.assistNodeInJavadoc & CompletionOnJavadoc.TEXT) != 0 && !this.requestor.isIgnored(CompletionProposal.JAVADOC_TYPE_REF)) {
			char[] javadocCompletion= inlineTagCompletion(completionName, JavadocTagConstants.TAG_LINK);
			InternalCompletionProposal proposal = (InternalCompletionProposal) CompletionProposal.create(CompletionProposal.JAVADOC_TYPE_REF, this.actualCompletionPosition - this.offset);
			proposal.nameLookup = this.nameEnvironment.nameLookup;
			proposal.completionEngine = this;
			proposal.setSignature(getSignature(typeParameter.binding));
			proposal.setTypeName(javadocCompletion);
			proposal.setCompletion(javadocCompletion);
			proposal.setFlags(typeParameter.modifiers);
			proposal.setReplaceRange(this.startPosition - this.offset, this.endPosition - this.offset);
			proposal.setTokenRange(this.tokenStart - this.offset, this.tokenEnd - this.offset);
			proposal.setRelevance(relevance+R_INLINE_TAG);
			this.requestor.accept(proposal);
			if(DEBUG) {
				this.printDebug(proposal);
			}
		}
	}

	/*
	 * Create a completion proposal for a type.
	 */
	private void createTypeProposal(char[] packageName, char[] typeName, int modifiers, int accessibility, char[] completionName, int relevance) {

		// Create standard type proposal
		if(!this.requestor.isIgnored(CompletionProposal.TYPE_REF) && (this.assistNodeInJavadoc & CompletionOnJavadoc.ONLY_INLINE_TAG) == 0) {
			InternalCompletionProposal proposal = (InternalCompletionProposal) CompletionProposal.create(CompletionProposal.TYPE_REF, this.actualCompletionPosition - this.offset);
			proposal.nameLookup = this.nameEnvironment.nameLookup;
			proposal.completionEngine = this;
			proposal.setDeclarationSignature(packageName);
			proposal.setSignature(createNonGenericTypeSignature(packageName, typeName));
			proposal.setPackageName(packageName);
			proposal.setTypeName(typeName);
			proposal.setCompletion(completionName);
			proposal.setFlags(modifiers);
			proposal.setReplaceRange(this.startPosition - this.offset, this.endPosition - this.offset);
			proposal.setTokenRange(this.tokenStart - this.offset, this.tokenEnd - this.offset);
			proposal.setRelevance(relevance);
			proposal.setAccessibility(accessibility);
			this.requestor.accept(proposal);
			if(DEBUG) {
				this.printDebug(proposal);
			}
		}

		// Create javadoc text proposal if necessary
		if ((this.assistNodeInJavadoc & CompletionOnJavadoc.TEXT) != 0 && !this.requestor.isIgnored(CompletionProposal.JAVADOC_TYPE_REF)) {
			char[] javadocCompletion= inlineTagCompletion(completionName, JavadocTagConstants.TAG_LINK);
			InternalCompletionProposal proposal = (InternalCompletionProposal) CompletionProposal.create(CompletionProposal.JAVADOC_TYPE_REF, this.actualCompletionPosition - this.offset);
			proposal.nameLookup = this.nameEnvironment.nameLookup;
			proposal.completionEngine = this;
			proposal.setDeclarationSignature(packageName);
			proposal.setSignature(createNonGenericTypeSignature(packageName, typeName));
			proposal.setPackageName(packageName);
			proposal.setTypeName(typeName);
			proposal.setCompletion(javadocCompletion);
			proposal.setFlags(modifiers);
			int start = (this.assistNodeInJavadoc & CompletionOnJavadoc.REPLACE_TAG) != 0 ? this.javadocTagPosition : this.startPosition;
			proposal.setReplaceRange(start - this.offset, this.endPosition - this.offset);
			proposal.setTokenRange(this.tokenStart - this.offset, this.tokenEnd - this.offset);
			proposal.setRelevance(relevance+R_INLINE_TAG);
			proposal.setAccessibility(accessibility);
			this.requestor.accept(proposal);
			if(DEBUG) {
				this.printDebug(proposal);
			}
		}
	}

	/*
	 * Create a completion proposal for a member type.
	 */
	private void createTypeProposal(
			ReferenceBinding refBinding,
			char[] typeName,
			int accessibility,
			char[] completionName,
			int relevance,
			Binding[] missingElements,
			int[] missingElementsStarts,
			int[] missingElementsEnds,
			boolean missingElementsHaveProblems) {

		// Create standard type proposal
		if(!this.isIgnored(CompletionProposal.TYPE_REF, missingElements != null) && (this.assistNodeInJavadoc & CompletionOnJavadoc.ONLY_INLINE_TAG) == 0) {
			InternalCompletionProposal proposal = (InternalCompletionProposal) CompletionProposal.create(CompletionProposal.TYPE_REF, this.actualCompletionPosition - this.offset);
			proposal.nameLookup = this.nameEnvironment.nameLookup;
			proposal.completionEngine = this;
			proposal.setDeclarationSignature(refBinding.qualifiedPackageName());
			proposal.setSignature(getCompletedTypeSignature(refBinding));
			proposal.setPackageName(refBinding.qualifiedPackageName());
			proposal.setTypeName(typeName);
			if (missingElements != null) {
				CompletionProposal[] subProposals = new CompletionProposal[missingElements.length];
				for (int i = 0; i < missingElements.length; i++) {
					subProposals[i] =
						createRequiredTypeProposal(
								missingElements[i],
								missingElementsStarts[i],
								missingElementsEnds[i],
								relevance);
				}
				proposal.setRequiredProposals(subProposals);
			}
			proposal.setCompletion(completionName);
			proposal.setFlags(refBinding.modifiers);
			proposal.setReplaceRange(this.startPosition - this.offset, this.endPosition - this.offset);
			proposal.setTokenRange(this.tokenStart - this.offset, this.tokenEnd - this.offset);
			proposal.setRelevance(relevance);
			this.requestor.accept(proposal);
			if(DEBUG) {
				this.printDebug(proposal);
			}
		}

		// Create javadoc text proposal if necessary
		if ((this.assistNodeInJavadoc & CompletionOnJavadoc.TEXT) != 0 && !this.requestor.isIgnored(CompletionProposal.JAVADOC_TYPE_REF)) {
			char[] javadocCompletion= inlineTagCompletion(completionName, JavadocTagConstants.TAG_LINK);
			InternalCompletionProposal proposal = (InternalCompletionProposal) CompletionProposal.create(CompletionProposal.JAVADOC_TYPE_REF, this.actualCompletionPosition - this.offset);
			proposal.nameLookup = this.nameEnvironment.nameLookup;
			proposal.completionEngine = this;
			proposal.setDeclarationSignature(refBinding.qualifiedPackageName());
			proposal.setSignature(getCompletedTypeSignature(refBinding));
			proposal.setPackageName(refBinding.qualifiedPackageName());
			proposal.setTypeName(typeName);
			proposal.setCompletion(javadocCompletion);
			proposal.setFlags(refBinding.modifiers);
			int start = (this.assistNodeInJavadoc & CompletionOnJavadoc.REPLACE_TAG) != 0 ? this.javadocTagPosition : this.startPosition;
			proposal.setReplaceRange(start - this.offset, this.endPosition - this.offset);
			proposal.setTokenRange(this.tokenStart - this.offset, this.tokenEnd - this.offset);
			proposal.setRelevance(relevance+R_INLINE_TAG);
			this.requestor.accept(proposal);
			if(DEBUG) {
				this.printDebug(proposal);
			}
		}
	}
	private void createTypeVariable(TypeVariableBinding typeVariable, Scope scope, StringBuffer completion) {
		completion.append(typeVariable.sourceName);

		if (typeVariable.superclass != null && TypeBinding.equalsEquals(typeVariable.firstBound, typeVariable.superclass)) {
		    completion.append(' ');
		    completion.append(EXTENDS);
		    completion.append(' ');
		    createType(typeVariable.superclass, scope, completion);
		}
		if (typeVariable.superInterfaces != null && typeVariable.superInterfaces != Binding.NO_SUPERINTERFACES) {
		   if (TypeBinding.notEquals(typeVariable.firstBound, typeVariable.superclass)) {
			   completion.append(' ');
			   completion.append(EXTENDS);
			   completion.append(' ');
		   }
		   for (int i = 0, length = typeVariable.superInterfaces.length; i < length; i++) {
			   if (i > 0 || TypeBinding.equalsEquals(typeVariable.firstBound, typeVariable.superclass)) {
				   completion.append(' ');
				   completion.append(EXTENDS);
				   completion.append(' ');
			   }
			   createType(typeVariable.superInterfaces[i], scope, completion);
		   }
		}
	}
	private void createVargsType(TypeBinding type, Scope scope, StringBuffer completion) {
		if (type.isArrayType()) {
			createType(type.leafComponentType(), scope, completion);
			int dim = type.dimensions() - 1;
			for (int i = 0; i < dim; i++) {
				completion.append('[');
				completion.append(']');
			}
			completion.append(VARARGS);
		} else {
			createType(type, scope, completion);
		}
	}
	private void findAnnotationAttributes(char[] token, MemberValuePair[] attributesFound, ReferenceBinding annotation) {
		MethodBinding[] methods = annotation.availableMethods();
		nextAttribute: for (int i = 0; i < methods.length; i++) {
			MethodBinding method = methods[i];

			if(isFailedMatch(token, method.selector)) continue nextAttribute;

			int length = attributesFound == null ? 0 : attributesFound.length;
			for (int j = 0; j < length; j++) {
				if(CharOperation.equals(method.selector, attributesFound[j].name, false)) continue nextAttribute;
			}

			int relevance = computeBaseRelevance();
			relevance += computeRelevanceForResolution();
			relevance += computeRelevanceForInterestingProposal(method);
			relevance += computeRelevanceForCaseMatching(token, method.selector);
			relevance += computeRelevanceForQualification(false);
			relevance += computeRelevanceForRestrictions(IAccessRule.K_ACCESSIBLE);

			this.noProposal = false;
			if(!this.requestor.isIgnored(CompletionProposal.ANNOTATION_ATTRIBUTE_REF)) {
				CompletionProposal proposal = createProposal(CompletionProposal.ANNOTATION_ATTRIBUTE_REF, this.actualCompletionPosition);
				proposal.setDeclarationSignature(getSignature(method.declaringClass));
				proposal.setSignature(getSignature(method.returnType));
				proposal.setName(method.selector);
				// add "=" to completion since it will always be needed
				char[] completion= method.selector;
				if (JavaCore.INSERT.equals(this.javaProject.getOption(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_ASSIGNMENT_OPERATOR, true))) {
					completion= CharOperation.concat(completion, new char[] {' '});
				}
				completion= CharOperation.concat(completion, new char[] {'='});
				if (JavaCore.INSERT.equals(this.javaProject.getOption(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_ASSIGNMENT_OPERATOR, true))) {
					completion= CharOperation.concat(completion, new char[] {' '});
				}
				proposal.setCompletion(completion);
				proposal.setFlags(method.modifiers);
				proposal.setReplaceRange(this.startPosition - this.offset, this.endPosition - this.offset);
				proposal.setTokenRange(this.tokenStart - this.offset, this.tokenEnd - this.offset);
				proposal.setRelevance(relevance);
				this.requestor.accept(proposal);
				if(DEBUG) {
					this.printDebug(proposal);
				}
			}
		}
	}
	void findAnonymousType(
			ReferenceBinding currentType,
			TypeBinding[] argTypes,
			Scope scope,
			InvocationSite invocationSite,
			Binding[] missingElements,
			int[] missingElementsStarts,
			int[] missingElementsEnds,
			boolean missingElementsHaveProblems) {

		int relevance = computeBaseRelevance();
		relevance += computeRelevanceForResolution();
		relevance += computeRelevanceForInterestingProposal(currentType);
		relevance += computeRelevanceForRestrictions(IAccessRule.K_ACCESSIBLE);

		if (missingElements != null) {
			relevance += computeRelevanceForMissingElements(missingElementsHaveProblems);
		}

		findAnonymousType(
				currentType,
				argTypes,
				scope,
				invocationSite,
				missingElements,
				missingElementsStarts,
				missingElementsEnds,
				missingElementsHaveProblems,
				true,
				false,
				relevance);
	}
	private void findAnonymousType(
		ReferenceBinding currentType,
		TypeBinding[] argTypes,
		Scope scope,
		InvocationSite invocationSite,
		Binding[] missingElements,
		int[] missingElementsStarts,
		int[] missingElementsEnds,
		boolean missingElementsHaveProblems,
		boolean exactMatch,
		boolean isQualified,
		int relevance) {

		if (currentType.isInterface()) {
			char[] completion = CharOperation.NO_CHAR;
			char[] typeCompletion = null;
			if (!exactMatch) {
				typeCompletion =
					isQualified ?
							CharOperation.concat(currentType.qualifiedPackageName(), currentType.qualifiedSourceName(), '.') :
								currentType.sourceName();
				if (this.source != null
							&& this.source.length > this.endPosition
							&& this.source[this.endPosition] == '(') {
					completion = CharOperation.NO_CHAR;
				} else {
					completion = new char[] { '(', ')' };
				}
			}

			this.noProposal = false;
			if (!exactMatch) {
				if(!isIgnored(CompletionProposal.ANONYMOUS_CLASS_CONSTRUCTOR_INVOCATION, CompletionProposal.TYPE_REF)) {
					char[] packageName = currentType.isLocalType() ? null : currentType.qualifiedPackageName();
					char[] typeName = currentType.qualifiedSourceName();

					InternalCompletionProposal proposal = createProposal(CompletionProposal.ANONYMOUS_CLASS_CONSTRUCTOR_INVOCATION, this.actualCompletionPosition);
					proposal.setDeclarationSignature(getSignature(currentType));
					proposal.setDeclarationKey(currentType.computeUniqueKey());
					proposal.setSignature(
							createMethodSignature(
									CharOperation.NO_CHAR_CHAR,
									CharOperation.NO_CHAR_CHAR,
									CharOperation.NO_CHAR,
									CharOperation.NO_CHAR));
					//proposal.setOriginalSignature(null);
					//proposal.setUniqueKey(null);
					proposal.setDeclarationPackageName(packageName);
					proposal.setDeclarationTypeName(typeName);
					//proposal.setParameterPackageNames(null);
					//proposal.setParameterTypeNames(null);
					//proposal.setPackageName(null);
					//proposal.setTypeName(null);
					proposal.setName(currentType.sourceName());

					InternalCompletionProposal typeProposal = createProposal(CompletionProposal.TYPE_REF, this.actualCompletionPosition);
					typeProposal.nameLookup = this.nameEnvironment.nameLookup;
					typeProposal.completionEngine = this;
					typeProposal.setDeclarationSignature(packageName);
					typeProposal.setSignature(getRequiredTypeSignature(currentType));
					typeProposal.setPackageName(packageName);
					typeProposal.setTypeName(typeName);
					typeProposal.setCompletion(typeCompletion);
					typeProposal.setFlags(currentType.modifiers);
					typeProposal.setReplaceRange(this.startPosition - this.offset, this.endPosition - this.offset);
					typeProposal.setTokenRange(this.startPosition - this.offset, this.endPosition - this.offset);
					typeProposal.setRelevance(relevance);
					proposal.setRequiredProposals( new CompletionProposal[]{typeProposal});

					proposal.setCompletion(completion);
					proposal.setFlags(Flags.AccPublic);
					proposal.setReplaceRange(this.endPosition - this.offset, this.endPosition - this.offset);
					proposal.setTokenRange(this.tokenStart - this.offset, this.tokenEnd - this.offset);
					proposal.setRelevance(relevance);
					this.requestor.accept(proposal);
					if(DEBUG) {
						this.printDebug(proposal);
					}
				}
			}  else {
				if(!isIgnored(CompletionProposal.ANONYMOUS_CLASS_DECLARATION, missingElements != null)) {
					InternalCompletionProposal proposal = createProposal(CompletionProposal.ANONYMOUS_CLASS_DECLARATION, this.actualCompletionPosition);
					proposal.setDeclarationSignature(getSignature(currentType));
					proposal.setDeclarationKey(currentType.computeUniqueKey());
					proposal.setSignature(
							createMethodSignature(
									CharOperation.NO_CHAR_CHAR,
									CharOperation.NO_CHAR_CHAR,
									CharOperation.NO_CHAR,
									CharOperation.NO_CHAR));
					//proposal.setOriginalSignature(null);
					//proposal.setUniqueKey(null);
					proposal.setDeclarationPackageName(currentType.qualifiedPackageName());
					proposal.setDeclarationTypeName(currentType.qualifiedSourceName());
					//proposal.setParameterPackageNames(null);
					//proposal.setParameterTypeNames(null);
					//proposal.setPackageName(null);
					//proposal.setTypeName(null);
					if (missingElements != null) {
						CompletionProposal[] subProposals = new CompletionProposal[missingElements.length];
						for (int i = 0; i < missingElements.length; i++) {
							subProposals[i] =
								createRequiredTypeProposal(
										missingElements[i],
										missingElementsStarts[i],
										missingElementsEnds[i],
										relevance);
						}
						proposal.setRequiredProposals(subProposals);
					}
					proposal.setCompletion(completion);
					proposal.setFlags(Flags.AccPublic);
					proposal.setReplaceRange(this.endPosition - this.offset, this.endPosition - this.offset);
					proposal.setTokenRange(this.tokenEnd - this.offset, this.tokenEnd - this.offset);
					proposal.setRelevance(relevance);
					this.requestor.accept(proposal);
					if(DEBUG) {
						this.printDebug(proposal);
					}
				}
			}
		} else {
			findConstructors(
				currentType,
				argTypes,
				scope,
				invocationSite,
				true,
				missingElements,
				missingElementsStarts,
				missingElementsEnds,
				missingElementsHaveProblems,
				exactMatch,
				isQualified,
				relevance);
		}
	}
	private void findClassField(
			char[] token,
			TypeBinding receiverType,
			Scope scope,
			Binding[] missingElements,
			int[] missingElementsStarts,
			int[] missingElementsEnds,
			boolean missingElementsHaveProblems) {

		if (token == null) return;

		if (token.length <= classField.length
			&& CharOperation.prefixEquals(token, classField, false /* ignore case */
		)) {
			int relevance = computeBaseRelevance();
			relevance += computeRelevanceForResolution();
			relevance += computeRelevanceForInterestingProposal();
			relevance += computeRelevanceForCaseMatching(token, classField);
			relevance += computeRelevanceForExpectingType(scope.getJavaLangClass());
			relevance += computeRelevanceForRestrictions(IAccessRule.K_ACCESSIBLE); //no access restriction for class field
			relevance += R_NON_INHERITED;

			if (missingElements != null) {
				relevance += computeRelevanceForMissingElements(missingElementsHaveProblems);
			}

			this.noProposal = false;
			if(!isIgnored(CompletionProposal.FIELD_REF, missingElements != null)) {
				InternalCompletionProposal proposal = createProposal(CompletionProposal.FIELD_REF, this.actualCompletionPosition);
				//proposal.setDeclarationSignature(null);
				char[] signature =
					createNonGenericTypeSignature(
						CharOperation.concatWith(JAVA_LANG, '.'),
						CLASS);
				if (this.compilerOptions.sourceLevel > ClassFileConstants.JDK1_4) {
					// add type argument
					char[] typeArgument = getTypeSignature(receiverType);
					int oldLength = signature.length;
					int argumentLength = typeArgument.length;
					int newLength = oldLength + argumentLength + 2;
					System.arraycopy(signature, 0, signature = new char[newLength], 0, oldLength - 1);
					signature[oldLength - 1] = '<';
					System.arraycopy(typeArgument, 0, signature, oldLength , argumentLength);
					signature[newLength - 2] = '>';
					signature[newLength - 1] = ';';
				}
				proposal.setSignature(signature);
				//proposal.setDeclarationPackageName(null);
				//proposal.setDeclarationTypeName(null);
				proposal.setPackageName(CharOperation.concatWith(JAVA_LANG, '.'));
				proposal.setTypeName(CLASS);
				proposal.setName(classField);
				if (missingElements != null) {
					CompletionProposal[] subProposals = new CompletionProposal[missingElements.length];
					for (int i = 0; i < missingElements.length; i++) {
						subProposals[i] =
							createRequiredTypeProposal(
									missingElements[i],
									missingElementsStarts[i],
									missingElementsEnds[i],
									relevance);
					}
					proposal.setRequiredProposals(subProposals);
				}
				proposal.setCompletion(classField);
				proposal.setFlags(Flags.AccStatic | Flags.AccPublic);
				proposal.setReplaceRange(this.startPosition - this.offset, this.endPosition - this.offset);
				proposal.setTokenRange(this.tokenStart - this.offset, this.tokenEnd - this.offset);
				proposal.setRelevance(relevance);
				this.requestor.accept(proposal);
				if(DEBUG) {
					this.printDebug(proposal);
				}
			}
		}
	}

	void findConstructors(
		ReferenceBinding currentType,
		TypeBinding[] argTypes,
		Scope scope,
		InvocationSite invocationSite,
		boolean forAnonymousType,
		Binding[] missingElements,
		int[] missingElementsStarts,
		int[] missingElementsEnds,
		boolean missingElementsHaveProblems) {

		int relevance = computeBaseRelevance();
		relevance += computeRelevanceForResolution();
		relevance += computeRelevanceForInterestingProposal();
		relevance += computeRelevanceForRestrictions(IAccessRule.K_ACCESSIBLE);

		if (missingElements != null) {
			relevance += computeRelevanceForMissingElements(missingElementsHaveProblems);
		}

		findConstructors(
				currentType,
				argTypes,
				scope,
				invocationSite,
				forAnonymousType,
				missingElements,
				missingElementsStarts,
				missingElementsEnds,
				missingElementsHaveProblems,
				true,
				false,
				relevance);
	}


	private void findConstructorsFromMissingType(
			TypeReference typeRef,
			final TypeBinding[] argTypes,
			final Scope scope,
			final InvocationSite invocationSite) {
		MissingTypesGuesser missingTypesConverter = new MissingTypesGuesser(this);
		MissingTypesGuesser.GuessedTypeRequestor substitutionRequestor =
			new MissingTypesGuesser.GuessedTypeRequestor() {
				@Override
				public void accept(
						TypeBinding guessedType,
						Binding[] missingElements,
						int[] missingElementsStarts,
						int[] missingElementsEnds,
						boolean hasProblems) {
					if (guessedType instanceof ReferenceBinding) {
						ReferenceBinding ref = (ReferenceBinding) guessedType;
						if (!isIgnored(CompletionProposal.METHOD_REF, missingElements != null)
								&& ref.isClass()
								&& !ref.isAbstract()) {
								findConstructors(
									ref,
									argTypes,
									scope,
									invocationSite,
									false,
									missingElements,
									missingElementsStarts,
									missingElementsEnds,
									hasProblems);
						}

						checkCancel();

						if (!isIgnored(CompletionProposal.ANONYMOUS_CLASS_DECLARATION, missingElements != null)
								&& !ref.isFinal()
								&& !ref.isEnum()){
							findAnonymousType(
								ref,
								argTypes,
								scope,
								invocationSite,
								missingElements,
								missingElementsStarts,
								missingElementsEnds,
								hasProblems);
						}
					}
				}
			};
		missingTypesConverter.guess(typeRef, scope, substitutionRequestor);
	}

	private void findConstructors(
		ReferenceBinding currentType,
		TypeBinding[] argTypes,
		Scope scope,
		InvocationSite invocationSite,
		boolean forAnonymousType,
		Binding[] missingElements,
		int[] missingElementsStarts,
		int[] missingElementsEnds,
		boolean missingElementsHaveProblems,
		boolean exactMatch,
		boolean isQualified,
		int relevance) {

		// No visibility checks can be performed without the scope & invocationSite
		MethodBinding[] methods = null;
		if (currentType instanceof ParameterizedTypeBinding && invocationSite instanceof CompletionOnQualifiedAllocationExpression) {
			CompletionOnQualifiedAllocationExpression alloc = (CompletionOnQualifiedAllocationExpression) invocationSite;
			if ((alloc.bits & ASTNode.IsDiamond) != 0) {
				// inference failed. So don't substitute type arguments. Just return the unsubstituted methods
				// and let the user decide what to substitute.
				ParameterizedTypeBinding binding = (ParameterizedTypeBinding) currentType;
				ReferenceBinding originalGenericType = binding.genericType();
				if (originalGenericType != null)
					methods = originalGenericType.methods();
			} else {
				methods = currentType.availableMethods();
			}
		} else {
			methods = currentType.availableMethods();
		}
		if(methods != null) {
			int minArgLength = argTypes == null ? 0 : argTypes.length;
			next : for (int f = methods.length; --f >= 0;) {
				MethodBinding constructor = methods[f];
				if (constructor.isConstructor()) {

					if (constructor.isSynthetic()) continue next;

					if (this.options.checkDeprecation &&
							constructor.isViewedAsDeprecated() &&
							!scope.isDefinedInSameUnit(constructor.declaringClass))
						continue next;

					if (this.options.checkVisibility
						&& !constructor.canBeSeenBy(invocationSite, scope)) {
						if(!forAnonymousType || !constructor.isProtected())
							continue next;
					}

					TypeBinding[] parameters = constructor.parameters;
					int paramLength = parameters.length;
					if (minArgLength > paramLength)
						continue next;
					for (int a = minArgLength; --a >= 0;)
						if (argTypes[a] != null) { // can be null if it could not be resolved properly
							if (!argTypes[a].isCompatibleWith(constructor.parameters[a]))
								continue next;
						}

					char[][] parameterPackageNames = new char[paramLength][];
					char[][] parameterTypeNames = new char[paramLength][];
					for (int i = 0; i < paramLength; i++) {
						TypeBinding type = parameters[i];
						parameterPackageNames[i] = type.qualifiedPackageName();
						parameterTypeNames[i] = type.qualifiedSourceName();
					}
					char[][] parameterNames = findMethodParameterNames(constructor,parameterTypeNames);

					char[] completion = CharOperation.NO_CHAR;

					if(forAnonymousType){
						char[] typeCompletion = null;
						if (!exactMatch) {
							typeCompletion =
								isQualified ?
										CharOperation.concat(currentType.qualifiedPackageName(), currentType.qualifiedSourceName(), '.') :
											currentType.sourceName();
							if (this.source != null
										&& this.source.length > this.endPosition
										&& this.source[this.endPosition] == '(') {
								completion = CharOperation.NO_CHAR;
							} else {
								completion = new char[] { '(', ')' };
							}
						}

						this.noProposal = false;
						if (!exactMatch) {
							if(!isIgnored(CompletionProposal.ANONYMOUS_CLASS_CONSTRUCTOR_INVOCATION, CompletionProposal.TYPE_REF)) {
								char[] packageName = currentType.isLocalType() ? null : currentType.qualifiedPackageName();
								char[] typeName = currentType.qualifiedSourceName();

								InternalCompletionProposal proposal = createProposal(CompletionProposal.ANONYMOUS_CLASS_CONSTRUCTOR_INVOCATION, this.actualCompletionPosition);
								proposal.setBinding(constructor);
								proposal.setDeclarationSignature(getSignature(currentType));
								proposal.setDeclarationKey(currentType.computeUniqueKey());
								proposal.setSignature(getSignature(constructor));
								MethodBinding original = constructor.original();
								if(original != constructor) {
									proposal.setOriginalSignature(getSignature(original));
								}
								proposal.setKey(constructor.computeUniqueKey());
								proposal.setDeclarationPackageName(packageName);
								proposal.setDeclarationTypeName(typeName);
								proposal.setParameterPackageNames(parameterPackageNames);
								proposal.setParameterTypeNames(parameterTypeNames);
								//proposal.setPackageName(null);
								//proposal.setTypeName(null);
								proposal.setName(currentType.sourceName());

								InternalCompletionProposal typeProposal = createProposal(CompletionProposal.TYPE_REF, this.actualCompletionPosition);
								typeProposal.nameLookup = this.nameEnvironment.nameLookup;
								typeProposal.completionEngine = this;
								typeProposal.setDeclarationSignature(packageName);
								typeProposal.setSignature(getRequiredTypeSignature(currentType));
								typeProposal.setPackageName(packageName);
								typeProposal.setTypeName(typeName);
								typeProposal.setCompletion(typeCompletion);
								typeProposal.setFlags(currentType.modifiers);
								typeProposal.setReplaceRange(this.startPosition - this.offset, this.endPosition - this.offset);
								typeProposal.setTokenRange(this.startPosition - this.offset, this.endPosition - this.offset);
								typeProposal.setRelevance(relevance);
								proposal.setRequiredProposals( new CompletionProposal[]{typeProposal});

								proposal.setCompletion(completion);
								proposal.setFlags(constructor.modifiers);
								proposal.setReplaceRange(this.endPosition - this.offset, this.endPosition - this.offset);
								proposal.setTokenRange(this.tokenStart - this.offset, this.tokenEnd - this.offset);
								proposal.setRelevance(relevance);
								if(parameterNames != null) proposal.setParameterNames(parameterNames);
								this.requestor.accept(proposal);
								if(DEBUG) {
									this.printDebug(proposal);
								}
							}
						} else {
							if(!isIgnored(CompletionProposal.ANONYMOUS_CLASS_DECLARATION, missingElements != null)) {
								InternalCompletionProposal proposal = createProposal(CompletionProposal.ANONYMOUS_CLASS_DECLARATION, this.actualCompletionPosition);
								proposal.setDeclarationSignature(getSignature(currentType));
								proposal.setDeclarationKey(currentType.computeUniqueKey());
								proposal.setSignature(getSignature(constructor));
								MethodBinding original = constructor.original();
								if(original != constructor) {
									proposal.setOriginalSignature(getSignature(original));
								}
								proposal.setKey(constructor.computeUniqueKey());
								proposal.setDeclarationPackageName(currentType.qualifiedPackageName());
								proposal.setDeclarationTypeName(currentType.qualifiedSourceName());
								proposal.setParameterPackageNames(parameterPackageNames);
								proposal.setParameterTypeNames(parameterTypeNames);
								//proposal.setPackageName(null);
								//proposal.setTypeName(null);
								if (missingElements != null) {
									CompletionProposal[] subProposals = new CompletionProposal[missingElements.length];
									for (int i = 0; i < missingElements.length; i++) {
										subProposals[i] =
											createRequiredTypeProposal(
													missingElements[i],
													missingElementsStarts[i],
													missingElementsEnds[i],
													relevance);
									}
									proposal.setRequiredProposals(subProposals);
								}
								proposal.setCompletion(completion);
								proposal.setFlags(constructor.modifiers);
								proposal.setReplaceRange(this.endPosition - this.offset, this.endPosition - this.offset);
								proposal.setTokenRange(this.tokenEnd - this.offset, this.tokenEnd - this.offset);
								proposal.setRelevance(relevance);
								if(parameterNames != null) proposal.setParameterNames(parameterNames);
								this.requestor.accept(proposal);
								if(DEBUG) {
									this.printDebug(proposal);
								}
							}
						}
					} else {
						char[] typeCompletion = null;
						// Special case for completion in javadoc
						if (this.assistNodeInJavadoc > 0) {
							Expression receiver = null;
							char[] selector = null;
							if (invocationSite instanceof CompletionOnJavadocAllocationExpression) {
								CompletionOnJavadocAllocationExpression alloc = (CompletionOnJavadocAllocationExpression) invocationSite;
								receiver = alloc.type;
							} else if (invocationSite instanceof CompletionOnJavadocFieldReference) {
								CompletionOnJavadocFieldReference fieldRef = (CompletionOnJavadocFieldReference) invocationSite;
								receiver = fieldRef.receiver;
							}
							if (receiver instanceof JavadocModuleReference) {
								JavadocModuleReference modRef = (JavadocModuleReference) receiver;
								receiver = modRef.typeReference;
							}
							if (receiver != null) {
								StringBuffer javadocCompletion = new StringBuffer();
								if (receiver.isThis()) {
									selector = (((JavadocImplicitTypeReference)receiver).token);
									if ((this.assistNodeInJavadoc & CompletionOnJavadoc.TEXT) != 0) {
										javadocCompletion.append('#');
									}
								} else if (receiver instanceof JavadocSingleTypeReference) {
									JavadocSingleTypeReference typeRef = (JavadocSingleTypeReference) receiver;
									selector = typeRef.token;
									if ((this.assistNodeInJavadoc & CompletionOnJavadoc.TEXT) != 0) {
										javadocCompletion.append(typeRef.token);
										javadocCompletion.append('#');
									}
								} else if (receiver instanceof JavadocQualifiedTypeReference) {
									JavadocQualifiedTypeReference typeRef = (JavadocQualifiedTypeReference) receiver;
									selector = typeRef.tokens[typeRef.tokens.length-1];
									if ((this.assistNodeInJavadoc & CompletionOnJavadoc.TEXT) != 0) {
										javadocCompletion.append(CharOperation.concatWith(typeRef.tokens, '.'));
										javadocCompletion.append('#');
									}
								}
								// Append parameters types
								javadocCompletion.append(selector);
								javadocCompletion.append('(');
								if (constructor.parameters != null) {
									boolean isVarargs = constructor.isVarargs();
									for (int p=0, ln=constructor.parameters.length; p<ln; p++) {
										if (p>0) javadocCompletion.append(", "); //$NON-NLS-1$
										TypeBinding argTypeBinding = constructor.parameters[p];
										if (isVarargs && p == ln - 1)  {
											createVargsType(argTypeBinding.erasure(), scope, javadocCompletion);
										} else {
											createType(argTypeBinding.erasure(), scope, javadocCompletion);
										}
									}
								}
								javadocCompletion.append(')');
								completion = javadocCompletion.toString().toCharArray();
							}
						} else {
							if (!exactMatch) {
								typeCompletion =
									isQualified ?
											CharOperation.concat(currentType.qualifiedPackageName(), currentType.qualifiedSourceName(), '.') :
												currentType.sourceName();

								if (this.source != null
											&& this.source.length > this.endPosition
											&& this.source[this.endPosition] == '(') {
									completion = CharOperation.NO_CHAR;
								} else {
									completion = new char[] { '(', ')' };
								}
							}
						}

						// Create standard proposal
						this.noProposal = false;
						if (!exactMatch) {
							if(!isIgnored(CompletionProposal.CONSTRUCTOR_INVOCATION, CompletionProposal.TYPE_REF)) {
								char[] packageName = currentType.isLocalType() ? null : currentType.qualifiedPackageName();
								char[] typeName = currentType.qualifiedSourceName();
								int constructorRelevance = relevance + computeRelevanceForConstructor();
								InternalCompletionProposal proposal =  createProposal(CompletionProposal.CONSTRUCTOR_INVOCATION, this.actualCompletionPosition);
								proposal.setBinding(constructor);
								proposal.setDeclarationSignature(getSignature(currentType));
								proposal.setSignature(getSignature(constructor));
								MethodBinding original = constructor.original();
								if(original != constructor) {
									proposal.setOriginalSignature(getSignature(original));
								}
								proposal.setDeclarationPackageName(packageName);
								proposal.setDeclarationTypeName(typeName);
								proposal.setParameterPackageNames(parameterPackageNames);
								proposal.setParameterTypeNames(parameterTypeNames);
								//proposal.setPackageName(null);
								//proposal.setTypeName(null);
								proposal.setName(currentType.sourceName());

								InternalCompletionProposal typeProposal = createProposal(CompletionProposal.TYPE_REF, this.actualCompletionPosition);
								typeProposal.nameLookup = this.nameEnvironment.nameLookup;
								typeProposal.completionEngine = this;
								typeProposal.setDeclarationSignature(packageName);
								typeProposal.setSignature(getRequiredTypeSignature(currentType));
								typeProposal.setPackageName(packageName);
								typeProposal.setTypeName(typeName);
								typeProposal.setCompletion(typeCompletion);
								typeProposal.setFlags(currentType.modifiers);
								typeProposal.setReplaceRange(this.startPosition - this.offset, this.endPosition - this.offset);
								typeProposal.setTokenRange(this.startPosition - this.offset, this.endPosition - this.offset);
								typeProposal.setRelevance(constructorRelevance);
								proposal.setRequiredProposals( new CompletionProposal[]{typeProposal});

								proposal.setIsContructor(true);
								proposal.setCompletion(completion);
								proposal.setFlags(constructor.modifiers);
								proposal.setReplaceRange(this.endPosition - this.offset, this.endPosition - this.offset);
								proposal.setTokenRange(this.tokenStart - this.offset, this.tokenEnd - this.offset);
								proposal.setRelevance(constructorRelevance);
								if(parameterNames != null) proposal.setParameterNames(parameterNames);
								this.requestor.accept(proposal);
								if(DEBUG) {
									this.printDebug(proposal);
								}
							}
						} else {
							if(!isIgnored(CompletionProposal.METHOD_REF, missingElements != null) && (this.assistNodeInJavadoc & CompletionOnJavadoc.ONLY_INLINE_TAG) == 0) {
								InternalCompletionProposal proposal =  createProposal(CompletionProposal.METHOD_REF, this.actualCompletionPosition);
								proposal.setBinding(constructor);
								proposal.setDeclarationSignature(getSignature(currentType));
								proposal.setSignature(getSignature(constructor));
								MethodBinding original = constructor.original();
								if(original != constructor) {
									proposal.setOriginalSignature(getSignature(original));
								}
								proposal.setDeclarationPackageName(currentType.qualifiedPackageName());
								proposal.setDeclarationTypeName(currentType.qualifiedSourceName());
								proposal.setParameterPackageNames(parameterPackageNames);
								proposal.setParameterTypeNames(parameterTypeNames);
								//proposal.setPackageName(null);
								//proposal.setTypeName(null);
								proposal.setName(currentType.sourceName());
								if (missingElements != null) {
									CompletionProposal[] subProposals = new CompletionProposal[missingElements.length];
									for (int i = 0; i < missingElements.length; i++) {
										subProposals[i] =
											createRequiredTypeProposal(
													missingElements[i],
													missingElementsStarts[i],
													missingElementsEnds[i],
													relevance);
									}
									proposal.setRequiredProposals(subProposals);
								}
								proposal.setIsContructor(true);
								proposal.setCompletion(completion);
								proposal.setFlags(constructor.modifiers);
								int start = (this.assistNodeInJavadoc > 0) ? this.startPosition : this.endPosition;
								proposal.setReplaceRange(start - this.offset, this.endPosition - this.offset);
								proposal.setTokenRange(this.tokenStart - this.offset, this.tokenEnd - this.offset);
								proposal.setRelevance(relevance);
								if(parameterNames != null) proposal.setParameterNames(parameterNames);
								this.requestor.accept(proposal);
								if(DEBUG) {
									this.printDebug(proposal);
								}
							}
							if ((this.assistNodeInJavadoc & CompletionOnJavadoc.TEXT) != 0 && !this.requestor.isIgnored(CompletionProposal.JAVADOC_METHOD_REF)) {
								char[] javadocCompletion = inlineTagCompletion(completion, JavadocTagConstants.TAG_LINK);
								InternalCompletionProposal proposal =  createProposal(CompletionProposal.JAVADOC_METHOD_REF, this.actualCompletionPosition);
								proposal.setBinding(constructor);
								proposal.setDeclarationSignature(getSignature(currentType));
								proposal.setSignature(getSignature(constructor));
								MethodBinding original = constructor.original();
								if(original != constructor) {
									proposal.setOriginalSignature(getSignature(original));
								}
								proposal.setDeclarationPackageName(currentType.qualifiedPackageName());
								proposal.setDeclarationTypeName(currentType.qualifiedSourceName());
								proposal.setParameterPackageNames(parameterPackageNames);
								proposal.setParameterTypeNames(parameterTypeNames);
								//proposal.setPackageName(null);
								//proposal.setTypeName(null);
								proposal.setName(currentType.sourceName());
								proposal.setIsContructor(true);
								proposal.setCompletion(javadocCompletion);
								proposal.setFlags(constructor.modifiers);
								int start = (this.assistNodeInJavadoc & CompletionOnJavadoc.REPLACE_TAG) != 0 ? this.javadocTagPosition : this.startPosition;
								proposal.setReplaceRange(start - this.offset, this.endPosition - this.offset);
								proposal.setTokenRange(this.tokenStart - this.offset, this.tokenEnd - this.offset);
								proposal.setRelevance(relevance+R_INLINE_TAG);
								if(parameterNames != null) proposal.setParameterNames(parameterNames);
								this.requestor.accept(proposal);
								if(DEBUG) {
									this.printDebug(proposal);
								}
							}
						}
					}
				}
			}
		}
	}

	private char[] getResolvedSignature(char[][] parameterTypes, char[] fullyQualifiedTypeName, int parameterCount, Scope scope) {
		char[][] cn = CharOperation.splitOn('.', fullyQualifiedTypeName);

		TypeReference ref;
		if (cn.length == 1) {
			ref = new SingleTypeReference(cn[0], 0);
		} else {
			ref = new QualifiedTypeReference(cn,new long[cn.length]);
		}

		TypeBinding guessedType = null;
		INameEnvironment oldNameEnvironment = this.lookupEnvironment.nameEnvironment;
		this.lookupEnvironment.nameEnvironment = getNoCacheNameEnvironment();
		try {
			switch (scope.kind) {
				case Scope.METHOD_SCOPE :
				case Scope.BLOCK_SCOPE :
					guessedType = ref.resolveType((BlockScope)scope);
					break;
				case Scope.CLASS_SCOPE :
					guessedType = ref.resolveType((ClassScope)scope);
					break;
			}


			if (guessedType != null && guessedType.isValidBinding()) {
				// the erasure must be used because guessedType can be a RawTypeBinding (https://bugs.eclipse.org/bugs/show_bug.cgi?id=276890)
				guessedType = guessedType.erasure();

				if (guessedType instanceof SourceTypeBinding) {
					SourceTypeBinding refBinding = (SourceTypeBinding) guessedType;

					if (refBinding.scope == null || refBinding.scope.referenceContext == null) return null;

					TypeDeclaration typeDeclaration = refBinding.scope.referenceContext;
					AbstractMethodDeclaration[] methods = typeDeclaration.methods;

					next : for (int i = 0; i < methods.length; i++) {
						AbstractMethodDeclaration method = methods[i];

						if (!method.isConstructor()) continue next;

						Argument[] arguments = method.arguments;
						int argumentsLength = arguments == null ? 0 : arguments.length;

						if (parameterCount != argumentsLength) continue next;

						for (int j = 0; j < argumentsLength; j++) {
							char[] argumentTypeName = getTypeName(arguments[j].type);

							if (!CharOperation.equals(argumentTypeName, parameterTypes[j])) {
								continue next;
							}
						}

						refBinding.resolveTypesFor(method.binding); // force resolution
						if (method.binding == null) continue next;
						return getSignature(method.binding);
					}
				}
			}
		} finally {
			this.lookupEnvironment.nameEnvironment = oldNameEnvironment;
		}

		return null;
	}

	private void findConstructorsOrAnonymousTypes(
			ReferenceBinding currentType,
			Scope scope,
			InvocationSite invocationSite,
			boolean isQualified,
			int relevance) {

		if (!isIgnored(CompletionProposal.CONSTRUCTOR_INVOCATION, CompletionProposal.TYPE_REF)
				&& currentType.isClass()
				&& !currentType.isAbstract()) {
				findConstructors(
					currentType,
					null,
					scope,
					invocationSite,
					false,
					null,
					null,
					null,
					false,
					false,
					isQualified,
					relevance);
		}

		// This code is disabled because there is too much proposals when constructors and anonymous are proposed
		if (!isIgnored(CompletionProposal.ANONYMOUS_CLASS_CONSTRUCTOR_INVOCATION, CompletionProposal.TYPE_REF)
				&& !currentType.isFinal()
				&& (currentType.isInterface() || (currentType.isClass() && currentType.isAbstract()))){
			findAnonymousType(
				currentType,
				null,
				scope,
				invocationSite,
				null,
				null,
				null,
				false,
				false,
				isQualified,
				relevance);
		}
	}
	private char[][] findEnclosingTypeNames(Scope scope){
		char[][] excludedNames = new char[10][];
		int excludedNameCount = 0;

		Scope currentScope = scope;
		while(currentScope != null) {
			switch (currentScope.kind) {
				case Scope.CLASS_SCOPE :
					ClassScope classScope = (ClassScope) currentScope;

					TypeDeclaration typeDeclaration = classScope.referenceContext;

					if(excludedNameCount == excludedNames.length) {
						System.arraycopy(excludedNames, 0, excludedNames = new char[excludedNameCount * 2][], 0, excludedNameCount);
					}
					excludedNames[excludedNameCount++] = typeDeclaration.name;

					TypeParameter[] classTypeParameters = typeDeclaration.typeParameters;
					if(classTypeParameters != null) {
						for (int i = 0; i < classTypeParameters.length; i++) {
							TypeParameter typeParameter = classTypeParameters[i];
							if(excludedNameCount == excludedNames.length) {
								System.arraycopy(excludedNames, 0, excludedNames = new char[excludedNameCount * 2][], 0, excludedNameCount);
							}
							excludedNames[excludedNameCount++] = typeParameter.name;
						}
					}
					break;
				case Scope.METHOD_SCOPE :
					MethodScope methodScope = (MethodScope) currentScope;
					if(methodScope.referenceContext instanceof AbstractMethodDeclaration) {
						TypeParameter[] methodTypeParameters = ((AbstractMethodDeclaration)methodScope.referenceContext).typeParameters();
						if(methodTypeParameters != null) {
							for (int i = 0; i < methodTypeParameters.length; i++) {
								TypeParameter typeParameter = methodTypeParameters[i];
								if(excludedNameCount == excludedNames.length) {
									System.arraycopy(excludedNames, 0, excludedNames = new char[excludedNameCount * 2][], 0, excludedNameCount);
								}
								excludedNames[excludedNameCount++] = typeParameter.name;
							}
						}
					}
					break;
			}

			currentScope = currentScope.parent;
		}

		if(excludedNameCount == 0) {
			return CharOperation.NO_CHAR_CHAR;
		}
		System.arraycopy(excludedNames, 0, excludedNames = new char[excludedNameCount][], 0, excludedNameCount);
		return excludedNames;
	}
	private void findEnumConstants(
			char[] enumConstantName,
			ReferenceBinding enumType,
			Scope invocationScope,
			ObjectVector fieldsFound,
			char[][] alreadyUsedConstants,
			int alreadyUsedConstantCount,
			boolean needQualification) {

		FieldBinding[] fields = enumType.fields();

		int enumConstantLength = enumConstantName.length;
		next : for (int f = fields.length; --f >= 0;) {
			FieldBinding field = fields[f];

			if (field.isSynthetic()) continue next;

			if ((field.modifiers & Flags.AccEnum) == 0) continue next;

			if (enumConstantLength > field.name.length) continue next;

			if (isFailedMatch(enumConstantName, field.name))	continue next;

			char[] fieldName = field.name;

			for (int i = 0; i < alreadyUsedConstantCount; i++) {
				if(CharOperation.equals(alreadyUsedConstants[i], fieldName)) continue next;
			}

			int relevance = computeBaseRelevance();
			relevance += computeRelevanceForResolution();
			relevance += computeRelevanceForInterestingProposal(field);
			relevance += computeRelevanceForCaseMatching(enumConstantName, field.name);
			int computeRelevanceForExpectingType = computeRelevanceForExpectingType(field.type);
			if(this.strictMatchForExtepectedType && computeRelevanceForExpectingType <= 0) {
				continue;
			} else if (!this.strictMatchForExtepectedType) {
				relevance += computeRelevanceForExpectingType;
			}

			relevance += computeRelevanceForEnumConstant(field.type);
			relevance += computeRelevanceForQualification(needQualification);
			relevance += computeRelevanceForRestrictions(IAccessRule.K_ACCESSIBLE);

			this.noProposal = false;
			if (!needQualification) {
				char[] completion = fieldName;

				if(!this.requestor.isIgnored(CompletionProposal.FIELD_REF)) {
					InternalCompletionProposal proposal = createProposal(CompletionProposal.FIELD_REF, this.actualCompletionPosition);
					proposal.setBinding(field);
					proposal.setDeclarationSignature(getSignature(field.declaringClass));
					proposal.setSignature(getSignature(field.type));
					proposal.setDeclarationPackageName(field.declaringClass.qualifiedPackageName());
					proposal.setDeclarationTypeName(field.declaringClass.qualifiedSourceName());
					proposal.setPackageName(field.type.qualifiedPackageName());
					proposal.setTypeName(field.type.qualifiedSourceName());
					proposal.setName(field.name);
					proposal.setCompletion(completion);
					proposal.setFlags(field.modifiers);
					proposal.setReplaceRange(this.startPosition - this.offset, this.endPosition - this.offset);
					proposal.setTokenRange(this.tokenStart - this.offset, this.tokenEnd - this.offset);
					proposal.setRelevance(relevance);
					this.requestor.accept(proposal);
					if(DEBUG) {
						this.printDebug(proposal);
					}
				}

			} else {
				TypeBinding visibleType = invocationScope.getType(field.type.sourceName());
				boolean needImport = visibleType == null || !visibleType.isValidBinding();

				char[] completion = CharOperation.concat(field.type.sourceName(), field.name, '.');

				if (!needImport) {
					if(!this.requestor.isIgnored(CompletionProposal.FIELD_REF)) {
						InternalCompletionProposal proposal = createProposal(CompletionProposal.FIELD_REF, this.actualCompletionPosition);
						proposal.setDeclarationSignature(getSignature(field.declaringClass));
						proposal.setSignature(getSignature(field.type));
						proposal.setDeclarationPackageName(field.declaringClass.qualifiedPackageName());
						proposal.setDeclarationTypeName(field.declaringClass.qualifiedSourceName());
						proposal.setPackageName(field.type.qualifiedPackageName());
						proposal.setTypeName(field.type.qualifiedSourceName());
						proposal.setName(field.name);
						proposal.setCompletion(completion);
						proposal.setFlags(field.modifiers);
						proposal.setReplaceRange(this.startPosition - this.offset, this.endPosition - this.offset);
						proposal.setTokenRange(this.tokenStart - this.offset, this.tokenEnd - this.offset);
						proposal.setRelevance(relevance);
						this.requestor.accept(proposal);
						if(DEBUG) {
							this.printDebug(proposal);
						}
					}
				} else {
					if (!this.isIgnored(CompletionProposal.FIELD_REF, CompletionProposal.TYPE_IMPORT)) {
						CompilationUnitDeclaration cu = this.unitScope.referenceContext;
						int importStart = cu.types[0].declarationSourceStart;
						int importEnd = importStart;

						ReferenceBinding fieldType = (ReferenceBinding)field.type;

						InternalCompletionProposal proposal = createProposal(CompletionProposal.FIELD_REF, this.actualCompletionPosition);
						proposal.setBinding(field);
						proposal.setDeclarationSignature(getSignature(field.declaringClass));
						proposal.setSignature(getSignature(field.type));
						proposal.setDeclarationPackageName(field.declaringClass.qualifiedPackageName());
						proposal.setDeclarationTypeName(field.declaringClass.qualifiedSourceName());
						proposal.setPackageName(field.type.qualifiedPackageName());
						proposal.setTypeName(field.type.qualifiedSourceName());
						proposal.setName(field.name);
						proposal.setCompletion(completion);
						proposal.setFlags(field.modifiers);
						proposal.setReplaceRange(this.startPosition - this.offset, this.endPosition - this.offset);
						proposal.setTokenRange(this.tokenStart - this.offset, this.tokenEnd - this.offset);
						proposal.setRelevance(relevance);

						char[] typeImportCompletion = createImportCharArray(CharOperation.concatWith(fieldType.compoundName, '.'), false, false);

						InternalCompletionProposal typeImportProposal = createProposal(CompletionProposal.TYPE_IMPORT, this.actualCompletionPosition);
						typeImportProposal.nameLookup = this.nameEnvironment.nameLookup;
						typeImportProposal.completionEngine = this;
						char[] packageName = fieldType.qualifiedPackageName();
						typeImportProposal.setDeclarationSignature(packageName);
						typeImportProposal.setSignature(getSignature(fieldType));
						typeImportProposal.setPackageName(packageName);
						typeImportProposal.setTypeName(fieldType.qualifiedSourceName());
						typeImportProposal.setCompletion(typeImportCompletion);
						typeImportProposal.setFlags(fieldType.modifiers);
						typeImportProposal.setAdditionalFlags(CompletionFlags.Default);
						typeImportProposal.setReplaceRange(importStart - this.offset, importEnd - this.offset);
						typeImportProposal.setTokenRange(importStart - this.offset, importEnd - this.offset);
						typeImportProposal.setRelevance(relevance);

						proposal.setRequiredProposals(new CompletionProposal[]{typeImportProposal});

						this.requestor.accept(proposal);
						if(DEBUG) {
							this.printDebug(proposal);
						}
					}
				}
			}
		}
	}
	private void findEnumConstantsFromExpectedTypes(
			char[] token,
			Scope invocationScope,
			ObjectVector fieldsFound) {
		int length = this.expectedTypesPtr + 1;
		for (int i = 0; i < length; i++) {
			if (this.expectedTypes[i].isEnum()) {
				findEnumConstants(
						token,
						(ReferenceBinding)this.expectedTypes[i],
						invocationScope,
						fieldsFound,
						CharOperation.NO_CHAR_CHAR,
						0,
						true);
			}
		}

	}
	private void findEnumConstantsFromSwithStatement(char[] enumConstantName, SwitchStatement switchStatement) {
		TypeBinding expressionType = switchStatement.expression.resolvedType;
		if(expressionType != null && expressionType.isEnum()) {
			ReferenceBinding enumType = (ReferenceBinding) expressionType;

			CaseStatement[] cases = switchStatement.cases;

			char[][] alreadyUsedConstants = new char[switchStatement.caseCount][];
			int alreadyUsedConstantCount = 0;
			for (int i = 0; i < switchStatement.caseCount; i++) {
				Expression[] caseExpressions = cases[i].constantExpressions;
				Expression caseExpression = caseExpressions != null && caseExpressions.length > 0 ?
						caseExpressions[0] : null;
				if((caseExpression instanceof SingleNameReference)
						&& (caseExpression.resolvedType != null && caseExpression.resolvedType.isEnum())) {
					alreadyUsedConstants[alreadyUsedConstantCount++] = ((SingleNameReference)caseExpression).token;
				}
			}

			findEnumConstants(
					enumConstantName,
					enumType,
					null /* doesn't need invocation scope */,
					new ObjectVector(),
					alreadyUsedConstants,
					alreadyUsedConstantCount,
					false);
		}
	}
	private void findExceptionFromTryStatement(
			char[] typeName,
			ReferenceBinding exceptionType,
			ReferenceBinding receiverType,
			SourceTypeBinding invocationType,
			BlockScope scope,
			ObjectVector typesFound,
			boolean searchSuperClasses) {

		if (searchSuperClasses) {
			ReferenceBinding javaLangThrowable = scope.getJavaLangThrowable();
			if (TypeBinding.notEquals(exceptionType, javaLangThrowable)) {
				ReferenceBinding superClass = exceptionType.superclass();
				while(superClass != null && TypeBinding.notEquals(superClass, javaLangThrowable)) {
					findExceptionFromTryStatement(typeName, superClass, receiverType, invocationType, scope, typesFound, false);
					superClass = superClass.superclass();
				}
			}
		}

		if (typeName.length > exceptionType.sourceName.length)
			return;

		if (isFailedMatch(typeName, exceptionType.sourceName))
			return;

		if (this.options.checkDeprecation &&
				exceptionType.isViewedAsDeprecated() &&
				!scope.isDefinedInSameUnit(exceptionType))
			return;

		if (this.options.checkVisibility) {
			if (invocationType != null) {
				if (receiverType != null) {
					if (!exceptionType.canBeSeenBy(receiverType, invocationType)) return;
				} else {
					if (!exceptionType.canBeSeenBy(exceptionType, invocationType)) return;
				}
			} else if(!exceptionType.canBeSeenBy(this.unitScope.fPackage)) {
				return;
			}
		}

		if (isForbidden(exceptionType)) return;

		for (int j = typesFound.size; --j >= 0;) {
			ReferenceBinding otherType = (ReferenceBinding) typesFound.elementAt(j);

			if (TypeBinding.equalsEquals(exceptionType, otherType))
				return;

			if (CharOperation.equals(exceptionType.sourceName, otherType.sourceName, true)) {

				if (exceptionType.enclosingType().isSuperclassOf(otherType.enclosingType()))
					return;

				if (otherType.enclosingType().isInterface())
					if (exceptionType.enclosingType()
						.implementsInterface(otherType.enclosingType(), true))
						return;

				if (exceptionType.enclosingType().isInterface())
					if (otherType.enclosingType()
						.implementsInterface(exceptionType.enclosingType(), true))
						return;
			}
		}

		typesFound.add(exceptionType);

		char[] completionName = exceptionType.sourceName();

		boolean isQualified = false;

		if(!this.insideQualifiedReference) {
			isQualified = true;

			char[] memberPackageName = exceptionType.qualifiedPackageName();
			char[] memberTypeName = exceptionType.sourceName();
			char[] memberEnclosingTypeNames = null;

			ReferenceBinding enclosingType = exceptionType.enclosingType();
			if (enclosingType != null) {
				memberEnclosingTypeNames = exceptionType.enclosingType().qualifiedSourceName();
			}

			Scope currentScope = scope;
			done : while (currentScope != null) { // done when a COMPILATION_UNIT_SCOPE is found

				switch (currentScope.kind) {

					case Scope.METHOD_SCOPE :
					case Scope.BLOCK_SCOPE :
						BlockScope blockScope = (BlockScope) currentScope;

						for (int j = 0, length = blockScope.subscopeCount; j < length; j++) {

							if (blockScope.subscopes[j] instanceof ClassScope) {
								SourceTypeBinding localType =
									((ClassScope) blockScope.subscopes[j]).referenceContext.binding;

								if (TypeBinding.equalsEquals(localType, exceptionType)) {
									isQualified = false;
									break done;
								}
							}
						}
						break;

					case Scope.CLASS_SCOPE :
						SourceTypeBinding type = ((ClassScope)currentScope).referenceContext.binding;
						ReferenceBinding[] memberTypes = type.memberTypes();
						if (memberTypes != null) {
							for (int j = 0; j < memberTypes.length; j++) {
								if (TypeBinding.equalsEquals(memberTypes[j], exceptionType)) {
									isQualified = false;
									break done;
								}
							}
						}


						break;

					case Scope.COMPILATION_UNIT_SCOPE :
						SourceTypeBinding[] types = ((CompilationUnitScope)currentScope).topLevelTypes;
						if (types != null) {
							for (int j = 0; j < types.length; j++) {
								if (TypeBinding.equalsEquals(types[j], exceptionType)) {
									isQualified = false;
									break done;
								}
							}
						}
						break done;
				}
				currentScope = currentScope.parent;
			}

			if (isQualified && mustQualifyType(memberPackageName, memberTypeName, memberEnclosingTypeNames, exceptionType.modifiers)) {
				if (memberPackageName == null || memberPackageName.length == 0)
					if (this.unitScope != null && this.unitScope.fPackage.compoundName != CharOperation.NO_CHAR_CHAR)
						return; // ignore types from the default package from outside it
			} else {
				isQualified = false;
			}

			if (isQualified) {
				completionName =
					CharOperation.concat(
							memberPackageName,
							CharOperation.concat(
									memberEnclosingTypeNames,
									memberTypeName,
									'.'),
							'.');
			}
		}

		int relevance = computeBaseRelevance();
		relevance += computeRelevanceForResolution();
		relevance += computeRelevanceForInterestingProposal(exceptionType);
		relevance += computeRelevanceForCaseMatching(typeName, exceptionType.sourceName);
		relevance += computeRelevanceForExpectingType(exceptionType);
		relevance += computeRelevanceForRestrictions(IAccessRule.K_ACCESSIBLE);
		if(!this.insideQualifiedReference) {
			relevance += computeRelevanceForQualification(isQualified);
		}
		relevance += computeRelevanceForClass();
		relevance += computeRelevanceForException();

		this.noProposal = false;
		if(!this.requestor.isIgnored(CompletionProposal.TYPE_REF)) {
			createTypeProposal(
					exceptionType,
					exceptionType.qualifiedSourceName(),
					IAccessRule.K_ACCESSIBLE,
					completionName,
					relevance,
					null,
					null,
					null,
					false);
		}
	}
	private void findExceptionFromTryStatement(
			char[] typeName,
			ReferenceBinding receiverType,
			SourceTypeBinding invocationType,
			BlockScope scope,
			ObjectVector typesFound) {

		for (int i = 0; i <= this.expectedTypesPtr; i++) {
			ReferenceBinding exceptionType = (ReferenceBinding)this.expectedTypes[i];

			findExceptionFromTryStatement(typeName, exceptionType, receiverType, invocationType, scope, typesFound, true);
		}
	}
	private void findExplicitConstructors(
		char[] name,
		ReferenceBinding currentType,
		MethodScope scope,
		InvocationSite invocationSite) {

		ConstructorDeclaration constructorDeclaration = (ConstructorDeclaration)scope.referenceContext;
		MethodBinding enclosingConstructor = constructorDeclaration.binding;

		// No visibility checks can be performed without the scope & invocationSite
		MethodBinding[] methods = currentType.availableMethods();
		if(methods != null) {
			next : for (int f = methods.length; --f >= 0;) {
				MethodBinding constructor = methods[f];
				if (constructor != enclosingConstructor && constructor.isConstructor()) {

					if (constructor.isSynthetic()) continue next;

					if (this.options.checkDeprecation &&
							constructor.isViewedAsDeprecated() &&
							!scope.isDefinedInSameUnit(constructor.declaringClass))
						continue next;

					if (this.options.checkVisibility
						&& !constructor.canBeSeenBy(invocationSite, scope))	continue next;

					TypeBinding[] parameters = constructor.parameters;
					int paramLength = parameters.length;

					char[][] parameterPackageNames = new char[paramLength][];
					char[][] parameterTypeNames = new char[paramLength][];
					for (int i = 0; i < paramLength; i++) {
						TypeBinding type = parameters[i];
						parameterPackageNames[i] = type.qualifiedPackageName();
						parameterTypeNames[i] = type.qualifiedSourceName();
					}
					char[][] parameterNames = findMethodParameterNames(constructor,parameterTypeNames);

					char[] completion = CharOperation.NO_CHAR;
					if (this.source != null
						&& this.source.length > this.endPosition
						&& this.source[this.endPosition] == '(')
						completion = name;
					else
						completion = CharOperation.concat(name, new char[] { '(', ')' });

					int relevance = computeBaseRelevance();
					relevance += computeRelevanceForResolution();
					relevance += computeRelevanceForInterestingProposal();
					relevance += computeRelevanceForCaseMatching(this.completionToken, name);
					relevance += computeRelevanceForRestrictions(IAccessRule.K_ACCESSIBLE);

					this.noProposal = false;
					if(!this.requestor.isIgnored(CompletionProposal.METHOD_REF)) {
						InternalCompletionProposal proposal =  createProposal(CompletionProposal.METHOD_REF, this.actualCompletionPosition);
						proposal.setBinding(constructor);
						proposal.setDeclarationSignature(getSignature(currentType));
						proposal.setSignature(getSignature(constructor));
						MethodBinding original = constructor.original();
						if(original != constructor) {
							proposal.setOriginalSignature(getSignature(original));
						}
						proposal.setDeclarationPackageName(currentType.qualifiedPackageName());
						proposal.setDeclarationTypeName(currentType.qualifiedSourceName());
						proposal.setParameterPackageNames(parameterPackageNames);
						proposal.setParameterTypeNames(parameterTypeNames);
						//proposal.setPackageName(null);
						//proposal.setTypeName(null);
						proposal.setName(name);
						proposal.setIsContructor(true);
						proposal.setCompletion(completion);
						proposal.setFlags(constructor.modifiers);
						proposal.setReplaceRange(this.startPosition - this.offset, this.endPosition - this.offset);
						proposal.setTokenRange(this.tokenStart - this.offset, this.tokenEnd - this.offset);
						proposal.setRelevance(relevance);
						if(parameterNames != null) proposal.setParameterNames(parameterNames);
						this.requestor.accept(proposal);
						if(DEBUG) {
							this.printDebug(proposal);
						}
					}
				}
			}
		}
	}
	// Helper method for findFields(char[], ReferenceBinding, Scope, ObjectVector, boolean)
	private void findFields(
		char[] fieldName,
		FieldBinding[] fields,
		Scope scope,
		ObjectVector fieldsFound,
		ObjectVector localsFound,
		boolean onlyStaticFields,
		ReferenceBinding receiverType,
		InvocationSite invocationSite,
		Scope invocationScope,
		boolean implicitCall,
		boolean canBePrefixed,
		Binding[] missingElements,
		int[] missingElementsStarts,
		int[] missingElementsEnds,
		boolean missingElementsHaveProblems,
		char[] castedReceiver,
		int receiverStart,
		int receiverEnd) {

		ObjectVector newFieldsFound = new ObjectVector();
		// if the proposal is being asked inside a field's initialization, we'll record its id
		int fieldBeingCompletedId = -1;
		boolean isFieldBeingCompletedStatic = false;
		for (int f = fields.length; --f >=0;) {
			FieldBinding field = fields[f];
			FieldDeclaration fieldDeclaration = field.sourceField();
			// We maybe asking for a proposal inside this field's initialization. So record its id
			ASTNode astNode = this.parser.assistNode;
			if (fieldDeclaration != null && fieldDeclaration.initialization != null && astNode != null) {
				if (CharOperation.equals(this.fileName, field.declaringClass.getFileName()) && fieldDeclaration.initialization.sourceEnd > 0) {
					if (fieldDeclaration.initialization.sourceStart <= astNode.sourceStart &&
						astNode.sourceEnd <= fieldDeclaration.initialization.sourceEnd) {
						// completion is inside a field initializer
						fieldBeingCompletedId = field.id;
						isFieldBeingCompletedStatic = field.isStatic();
						break;
					}
				} else { // The sourceEnd may not yet be set
					CompletionNodeDetector detector = new CompletionNodeDetector(astNode, fieldDeclaration.initialization);
					if (detector.containsCompletionNode()) {  // completion is inside a field initializer
						fieldBeingCompletedId = field.id;
						isFieldBeingCompletedStatic = field.isStatic();
						break;
					}
				}
			}
		}
		// Inherited fields which are hidden by subclasses are filtered out
		// No visibility checks can be performed without the scope & invocationSite

		int fieldLength = fieldName.length;
		next : for (int f = fields.length; --f >= 0;) {
			FieldBinding field = fields[f];

			// Content assist invoked inside some field's initialization.
			// bug 310427 and 325481
			if (fieldBeingCompletedId >= 0 && field.id >= fieldBeingCompletedId) {
				// Don't propose field which is being declared currently
				// Don't propose fields declared after the current field declaration statement
				// Though, if field is static or completion happens in Javadoc, then it can be still be proposed
				if (this.assistNodeInJavadoc == 0) {
					if (!field.isStatic()) {
						continue next;
					} else if (isFieldBeingCompletedStatic) {
						// static fields can't be proposed before they are actually declared if the
						// field currently being declared is also static
						continue next;
					}
				}
			}

			if (field.isSynthetic())	continue next;

			if (onlyStaticFields && !field.isStatic()) continue next;

			if (fieldLength > field.name.length) continue next;

			if (isFailedMatch(fieldName, field.name))	continue next;

			if (this.options.checkDeprecation &&
					field.isViewedAsDeprecated() &&
					!scope.isDefinedInSameUnit(field.declaringClass))
				continue next;

			if (this.options.checkVisibility
				&& !field.canBeSeenBy(receiverType, invocationSite, scope))	continue next;

			// don't propose non constant fields or strings (1.6 or below) in case expression
			// https://bugs.eclipse.org/bugs/show_bug.cgi?id=195346
			// https://bugs.eclipse.org/bugs/show_bug.cgi?id=343342
			if (this.assistNodeIsInsideCase) {
				if (field.isFinal()) {
					if (this.assistNodeIsString){
						if (field.type == null || field.type.id != TypeIds.T_JavaLangString)
							continue next;
					} else if (!(field.type instanceof BaseTypeBinding))
						continue next;
				} else {
					continue next; // non-constants not allowed in case.
				}
			}

			boolean prefixRequired = false;

			for (int i = fieldsFound.size; --i >= 0;) {
				Object[] other = (Object[])fieldsFound.elementAt(i);
				FieldBinding otherField = (FieldBinding) other[0];
				ReferenceBinding otherReceiverType = (ReferenceBinding) other[1];
				if (field == otherField && TypeBinding.equalsEquals(receiverType, otherReceiverType))
					continue next;
				if (CharOperation.equals(field.name, otherField.name, true)) {
					if (field.declaringClass.isSuperclassOf(otherField.declaringClass))
						continue next;
					if (otherField.declaringClass.isInterface()) {
						if (TypeBinding.equalsEquals(field.declaringClass, scope.getJavaLangObject()))
							continue next;
						if (field.declaringClass.implementsInterface(otherField.declaringClass, true))
							continue next;
					}
					if (field.declaringClass.isInterface())
						if (otherField.declaringClass.implementsInterface(field.declaringClass, true))
							continue next;
					if(canBePrefixed) {
						prefixRequired = true;
					} else {
						continue next;
					}
				}
			}

			for (int l = localsFound.size; --l >= 0;) {
				LocalVariableBinding local = (LocalVariableBinding) localsFound.elementAt(l);

				if (CharOperation.equals(field.name, local.name, true)) {
					SourceTypeBinding declarationType = scope.enclosingSourceType();
					if (declarationType.isAnonymousType() && TypeBinding.notEquals(declarationType, invocationScope.enclosingSourceType())) {
						continue next;
					}
					if(canBePrefixed) {
						prefixRequired = true;
					} else {
						continue next;
					}
					break;
				}
			}

			newFieldsFound.add(new Object[]{field, receiverType});

			char[] completion = field.name;

			if(prefixRequired || this.options.forceImplicitQualification){
				char[] prefix = computePrefix(scope.enclosingSourceType(), invocationScope.enclosingSourceType(), field.isStatic());
				completion = CharOperation.concat(prefix,completion,'.');
			}


			if (castedReceiver != null) {
				completion = CharOperation.concat(castedReceiver, completion);
			}

			// Special case for javadoc completion
			if (this.assistNodeInJavadoc > 0) {
				if (invocationSite instanceof CompletionOnJavadocFieldReference) {
					CompletionOnJavadocFieldReference fieldRef = (CompletionOnJavadocFieldReference) invocationSite;
					if (fieldRef.receiver.isThis()) {
						if (fieldRef.completeInText()) {
							completion = CharOperation.concat(new char[] { '#' }, field.name);
						}
					} else if (fieldRef.completeInText()) {
						if (fieldRef.receiver instanceof JavadocSingleTypeReference) {
							JavadocSingleTypeReference typeRef = (JavadocSingleTypeReference) fieldRef.receiver;
							completion = CharOperation.concat(typeRef.token, field.name, '#');
						} else if (fieldRef.receiver instanceof JavadocQualifiedTypeReference) {
							JavadocQualifiedTypeReference typeRef = (JavadocQualifiedTypeReference) fieldRef.receiver;
							completion = CharOperation.concat(CharOperation.concatWith(typeRef.tokens, '.'), field.name, '#');
						}
					}
				}
			}

			int relevance = computeBaseRelevance();
			relevance += computeRelevanceForResolution();
			relevance += computeRelevanceForInterestingProposal(field);
			relevance += computeRelevanceForCaseMatching(fieldName, field.name);
			int computeRelevanceForExpectingType = computeRelevanceForExpectingType(field.type);
			if(this.strictMatchForExtepectedType && computeRelevanceForExpectingType <= 0) {
				continue;
			} else if(!this.strictMatchForExtepectedType) {
				relevance += computeRelevanceForExpectingType;
			}

			relevance += computeRelevanceForEnumConstant(field.type);
			relevance += computeRelevanceForStatic(onlyStaticFields, field.isStatic());
			relevance += computeRelevanceForFinal(this.assistNodeIsInsideCase, field.isFinal());
			relevance += computeRelevanceForQualification(prefixRequired);
			relevance += computeRelevanceForRestrictions(IAccessRule.K_ACCESSIBLE);
			if (onlyStaticFields && this.insideQualifiedReference) {
				relevance += computeRelevanceForInheritance(receiverType, field.declaringClass);
			}
			if (missingElements != null) {
				relevance += computeRelevanceForMissingElements(missingElementsHaveProblems);
			}

			this.noProposal = false;
			if (castedReceiver == null) {
				// Standard proposal
				if (!this.isIgnored(CompletionProposal.FIELD_REF, missingElements != null) && (this.assistNodeInJavadoc & CompletionOnJavadoc.ONLY_INLINE_TAG) == 0) {
					InternalCompletionProposal proposal =  createProposal(CompletionProposal.FIELD_REF, this.actualCompletionPosition);
					proposal.setBinding(field);
					proposal.setDeclarationSignature(getSignature(field.declaringClass));
					proposal.setSignature(getSignature(field.type));
					proposal.setDeclarationPackageName(field.declaringClass.qualifiedPackageName());
					proposal.setDeclarationTypeName(field.declaringClass.qualifiedSourceName());
					proposal.setPackageName(field.type.qualifiedPackageName());
					proposal.setTypeName(field.type.qualifiedSourceName());
					proposal.setName(field.name);
					if (missingElements != null) {
						CompletionProposal[] subProposals = new CompletionProposal[missingElements.length];
						for (int i = 0; i < missingElements.length; i++) {
							subProposals[i] =
								createRequiredTypeProposal(
										missingElements[i],
										missingElementsStarts[i],
										missingElementsEnds[i],
										relevance);
						}
						proposal.setRequiredProposals(subProposals);
					}
					proposal.setCompletion(completion);
					proposal.setFlags(field.modifiers);
					proposal.setReplaceRange(this.startPosition - this.offset, this.endPosition - this.offset);
					proposal.setTokenRange(this.tokenStart - this.offset, this.tokenEnd - this.offset);
					proposal.setRelevance(relevance);
					this.requestor.accept(proposal);
					if(DEBUG) {
						this.printDebug(proposal);
					}
				}

				// Javadoc completions
				if ((this.assistNodeInJavadoc & CompletionOnJavadoc.TEXT) != 0 && !this.requestor.isIgnored(CompletionProposal.JAVADOC_FIELD_REF)) {
					char[] javadocCompletion = inlineTagCompletion(completion, JavadocTagConstants.TAG_LINK);
					InternalCompletionProposal proposal =  createProposal(CompletionProposal.JAVADOC_FIELD_REF, this.actualCompletionPosition);
					proposal.setBinding(field);
					proposal.setDeclarationSignature(getSignature(field.declaringClass));
					proposal.setSignature(getSignature(field.type));
					proposal.setDeclarationPackageName(field.declaringClass.qualifiedPackageName());
					proposal.setDeclarationTypeName(field.declaringClass.qualifiedSourceName());
					proposal.setPackageName(field.type.qualifiedPackageName());
					proposal.setTypeName(field.type.qualifiedSourceName());
					proposal.setName(field.name);
					proposal.setCompletion(javadocCompletion);
					proposal.setFlags(field.modifiers);
					int start = (this.assistNodeInJavadoc & CompletionOnJavadoc.REPLACE_TAG) != 0 ? this.javadocTagPosition : this.startPosition;
					proposal.setReplaceRange(start - this.offset, this.endPosition - this.offset);
					proposal.setTokenRange(this.tokenStart - this.offset, this.tokenEnd - this.offset);
					proposal.setRelevance(relevance+R_INLINE_TAG);
					this.requestor.accept(proposal);
					if(DEBUG) {
						this.printDebug(proposal);
					}
					// Javadoc value completion for static fields
					if (field.isStatic() && !this.requestor.isIgnored(CompletionProposal.JAVADOC_VALUE_REF)) {
						javadocCompletion = inlineTagCompletion(completion, JavadocTagConstants.TAG_VALUE);
						InternalCompletionProposal valueProposal = createProposal(CompletionProposal.JAVADOC_VALUE_REF, this.actualCompletionPosition);
						valueProposal.setDeclarationSignature(getSignature(field.declaringClass));
						valueProposal.setSignature(getSignature(field.type));
						valueProposal.setDeclarationPackageName(field.declaringClass.qualifiedPackageName());
						valueProposal.setDeclarationTypeName(field.declaringClass.qualifiedSourceName());
						valueProposal.setPackageName(field.type.qualifiedPackageName());
						valueProposal.setTypeName(field.type.qualifiedSourceName());
						valueProposal.setName(field.name);
						valueProposal.setCompletion(javadocCompletion);
						valueProposal.setFlags(field.modifiers);
						valueProposal.setReplaceRange(start - this.offset, this.endPosition - this.offset);
						valueProposal.setTokenRange(this.tokenStart - this.offset, this.tokenEnd - this.offset);
						valueProposal.setRelevance(relevance+R_VALUE_TAG);
						this.requestor.accept(valueProposal);
						if(DEBUG) {
							this.printDebug(valueProposal);
						}
					}
				}
			} else {
				if(!this.isIgnored(CompletionProposal.FIELD_REF_WITH_CASTED_RECEIVER, missingElements != null)) {
					InternalCompletionProposal proposal = createProposal(CompletionProposal.FIELD_REF_WITH_CASTED_RECEIVER, this.actualCompletionPosition);
					proposal.setBinding(field);
					proposal.setDeclarationSignature(getSignature(field.declaringClass));
					proposal.setSignature(getSignature(field.type));
					proposal.setReceiverSignature(getSignature(receiverType));
					proposal.setDeclarationPackageName(field.declaringClass.qualifiedPackageName());
					proposal.setDeclarationTypeName(field.declaringClass.qualifiedSourceName());
					proposal.setPackageName(field.type.qualifiedPackageName());
					proposal.setTypeName(field.type.qualifiedSourceName());
					proposal.setName(field.name);
					if (missingElements != null) {
						CompletionProposal[] subProposals = new CompletionProposal[missingElements.length];
						for (int i = 0; i < missingElements.length; i++) {
							subProposals[i] =
								createRequiredTypeProposal(
										missingElements[i],
										missingElementsStarts[i],
										missingElementsEnds[i],
										relevance);
						}
						proposal.setRequiredProposals(subProposals);
					}
					proposal.setCompletion(completion);
					proposal.setFlags(field.modifiers);
					proposal.setReplaceRange(this.startPosition - this.offset, this.endPosition - this.offset);
					proposal.setReceiverRange(receiverStart - this.offset, receiverEnd - this.offset);
					proposal.setTokenRange(this.tokenStart - this.offset, this.tokenEnd - this.offset);
					proposal.setRelevance(relevance);
					this.requestor.accept(proposal);
					if(DEBUG) {
						this.printDebug(proposal);
					}
				}
			}
		}

		fieldsFound.addAll(newFieldsFound);
	}
	private void findFields(
		char[] fieldName,
		ReferenceBinding receiverType,
		Scope scope,
		ObjectVector fieldsFound,
		ObjectVector localsFound,
		boolean onlyStaticFields,
		InvocationSite invocationSite,
		Scope invocationScope,
		boolean implicitCall,
		boolean canBePrefixed,
		Binding[] missingElements,
		int[] missingElementsStarts,
		int[] missingElementsEnds,
		boolean missingElementsHaveProblems,
		char[] castedReceiver,
		int receiverStart,
		int receiverEnd) {

		boolean notInJavadoc = this.assistNodeInJavadoc == 0;
		if (fieldName == null && notInJavadoc)
			return;

		ReferenceBinding currentType = receiverType;
		ReferenceBinding[] interfacesToVisit = null;
		int nextPosition = 0;
		do {
			ReferenceBinding[] itsInterfaces = currentType.superInterfaces();
			if (notInJavadoc && itsInterfaces != null && itsInterfaces != Binding.NO_SUPERINTERFACES) {
				if (interfacesToVisit == null) {
					interfacesToVisit = itsInterfaces;
					nextPosition = interfacesToVisit.length;
				} else {
					int itsLength = itsInterfaces.length;
					if (nextPosition + itsLength >= interfacesToVisit.length)
						System.arraycopy(interfacesToVisit, 0, interfacesToVisit = new ReferenceBinding[nextPosition + itsLength + 5], 0, nextPosition);
					nextInterface : for (int a = 0; a < itsLength; a++) {
						ReferenceBinding next = itsInterfaces[a];
						for (int b = 0; b < nextPosition; b++)
							if (TypeBinding.equalsEquals(next, interfacesToVisit[b])) continue nextInterface;
						interfacesToVisit[nextPosition++] = next;
					}
				}
			}

			FieldBinding[] fields = currentType.availableFields();
			if(fields != null && fields.length > 0) {
				findFields(
					fieldName,
					fields,
					scope,
					fieldsFound,
					localsFound,
					onlyStaticFields,
					receiverType,
					invocationSite,
					invocationScope,
					implicitCall,
					canBePrefixed,
					missingElements,
					missingElementsStarts,
					missingElementsEnds,
					missingElementsHaveProblems,
					castedReceiver,
					receiverStart,
					receiverEnd);
			}
			currentType = currentType.superclass();
		} while (notInJavadoc && currentType != null);

		if (notInJavadoc && interfacesToVisit != null) {
			for (int i = 0; i < nextPosition; i++) {
				ReferenceBinding anInterface = interfacesToVisit[i];
				FieldBinding[] fields = anInterface.availableFields();
				if(fields !=  null) {
					findFields(
						fieldName,
						fields,
						scope,
						fieldsFound,
						localsFound,
						onlyStaticFields,
						receiverType,
						invocationSite,
						invocationScope,
						implicitCall,
						canBePrefixed,
						missingElements,
						missingElementsStarts,
						missingElementsEnds,
						missingElementsHaveProblems,
						castedReceiver,
						receiverStart,
						receiverEnd);
				}

				ReferenceBinding[] itsInterfaces = anInterface.superInterfaces();
				if (itsInterfaces != Binding.NO_SUPERINTERFACES) {
					int itsLength = itsInterfaces.length;
					if (nextPosition + itsLength >= interfacesToVisit.length)
						System.arraycopy(interfacesToVisit, 0, interfacesToVisit = new ReferenceBinding[nextPosition + itsLength + 5], 0, nextPosition);
					nextInterface : for (int a = 0; a < itsLength; a++) {
						ReferenceBinding next = itsInterfaces[a];
						for (int b = 0; b < nextPosition; b++)
							if (TypeBinding.equalsEquals(next, interfacesToVisit[b])) continue nextInterface;
						interfacesToVisit[nextPosition++] = next;
					}
				}
			}
		}
	}

	protected void findFieldsAndMethods(
		char[] token,
		TypeBinding receiverType,
		Scope scope,
		ObjectVector fieldsFound,
		ObjectVector methodsFound,
		InvocationSite invocationSite,
		Scope invocationScope,
		boolean implicitCall,
		boolean superCall,
		Binding[] missingElements,
		int[] missingElementsStarts,
		int[] missingElementsEnds,
		boolean missingElementsHaveProblems,
		char[] castedReceiver,
		int receiverStart,
		int receiverEnd) {

		if (token == null)
			return;

		if (receiverType.isBaseType())
			return; // nothing else is possible with base types

		boolean proposeField =
			castedReceiver == null ?
					!this.isIgnored(CompletionProposal.FIELD_REF, missingElements != null) :
					!this.isIgnored(CompletionProposal.FIELD_REF_WITH_CASTED_RECEIVER, missingElements != null) ;
		boolean proposeMethod =
			castedReceiver == null ?
					!this.isIgnored(CompletionProposal.METHOD_REF, missingElements != null) :
					!this.isIgnored(CompletionProposal.METHOD_REF_WITH_CASTED_RECEIVER, missingElements != null);

		if (receiverType.isArrayType()) {
			if (proposeField
				&& token.length <= lengthField.length
				&& CharOperation.prefixEquals(token, lengthField, false /* ignore case */
			)) {

				int relevance = computeBaseRelevance();
				relevance += computeRelevanceForResolution();
				relevance += computeRelevanceForInterestingProposal();
				relevance += computeRelevanceForCaseMatching(token,lengthField);
				relevance += computeRelevanceForExpectingType(TypeBinding.INT);
				relevance += computeRelevanceForRestrictions(IAccessRule.K_ACCESSIBLE); // no access restriction for length field
				if (missingElements != null) {
					relevance += computeRelevanceForMissingElements(missingElementsHaveProblems);
				}
				this.noProposal = false;
				if (castedReceiver == null) {
					if(!isIgnored(CompletionProposal.FIELD_REF, missingElements != null)) {
						InternalCompletionProposal proposal =  createProposal(CompletionProposal.FIELD_REF, this.actualCompletionPosition);
						proposal.setDeclarationSignature(getSignature(receiverType));
						proposal.setSignature(INT_SIGNATURE);
						proposal.setTypeName(INT);
						proposal.setName(lengthField);
						if (missingElements != null) {
							CompletionProposal[] subProposals = new CompletionProposal[missingElements.length];
							for (int i = 0; i < missingElements.length; i++) {
								subProposals[i] =
									createRequiredTypeProposal(
											missingElements[i],
											missingElementsStarts[i],
											missingElementsEnds[i],
											relevance);
							}
							proposal.setRequiredProposals(subProposals);
						}
						proposal.setCompletion(lengthField);
						proposal.setFlags(Flags.AccPublic);
						proposal.setReplaceRange(this.startPosition - this.offset, this.endPosition - this.offset);
						proposal.setTokenRange(this.tokenStart - this.offset, this.tokenEnd - this.offset);
						proposal.setRelevance(relevance);
						this.requestor.accept(proposal);
						if(DEBUG) {
							this.printDebug(proposal);
						}
					}
				} else {
					char[] completion = CharOperation.concat(castedReceiver, lengthField);

					if(!this.isIgnored(CompletionProposal.FIELD_REF_WITH_CASTED_RECEIVER, missingElements != null)) {
						InternalCompletionProposal proposal =  createProposal(CompletionProposal.FIELD_REF_WITH_CASTED_RECEIVER, this.actualCompletionPosition);
						proposal.setDeclarationSignature(getSignature(receiverType));
						proposal.setSignature(INT_SIGNATURE);
						proposal.setReceiverSignature(getSignature(receiverType));
						proposal.setTypeName(INT);
						proposal.setName(lengthField);
						if (missingElements != null) {
							CompletionProposal[] subProposals = new CompletionProposal[missingElements.length];
							for (int i = 0; i < missingElements.length; i++) {
								subProposals[i] =
									createRequiredTypeProposal(
											missingElements[i],
											missingElementsStarts[i],
											missingElementsEnds[i],
											relevance);
							}
							proposal.setRequiredProposals(subProposals);
						}
						proposal.setCompletion(completion);
						proposal.setFlags(Flags.AccPublic);
						proposal.setReplaceRange(this.startPosition - this.offset, this.endPosition - this.offset);
						proposal.setReceiverRange(receiverStart - this.offset, receiverEnd - this.offset);
						proposal.setTokenRange(this.tokenStart - this.offset, this.tokenEnd - this.offset);
						proposal.setRelevance(relevance);
						this.requestor.accept(proposal);
						if(DEBUG) {
							this.printDebug(proposal);
						}
					}
				}
			}
			if (proposeMethod
				&& token.length <= cloneMethod.length
				&& CharOperation.prefixEquals(token, cloneMethod, false /* ignore case */)
			) {
				ReferenceBinding objectRef = scope.getJavaLangObject();

				int relevance = computeBaseRelevance();
				relevance += computeRelevanceForResolution();
				relevance += computeRelevanceForInterestingProposal();
				relevance += computeRelevanceForCaseMatching(token, cloneMethod);
				relevance += computeRelevanceForExpectingType(objectRef);
				relevance += computeRelevanceForStatic(false, false);
				relevance += computeRelevanceForQualification(false);
				relevance += computeRelevanceForRestrictions(IAccessRule.K_ACCESSIBLE); // no access restriction for clone() method
				if (missingElements != null) {
					relevance += computeRelevanceForMissingElements(missingElementsHaveProblems);
				}
				char[] completion;
				if (this.source != null
					&& this.source.length > this.endPosition
					&& this.source[this.endPosition] == '(') {
					completion = cloneMethod;
					} else {
					completion = CharOperation.concat(cloneMethod, new char[] { '(', ')' });
				}

				if (castedReceiver != null) {
					completion = CharOperation.concat(castedReceiver, completion);
				}

				this.noProposal = false;
				if (castedReceiver == null) {
					if (!this.isIgnored(CompletionProposal.METHOD_REF, missingElements != null)) {
						InternalCompletionProposal proposal =  createProposal(CompletionProposal.METHOD_REF, this.actualCompletionPosition);
						proposal.setDeclarationSignature(getSignature(receiverType));
						proposal.setSignature(
								this.compilerOptions.sourceLevel > ClassFileConstants.JDK1_4 && receiverType.isArrayType() ?
										createMethodSignature(
												CharOperation.NO_CHAR_CHAR,
												CharOperation.NO_CHAR_CHAR,
												getSignature(receiverType)) :
										createMethodSignature(
												CharOperation.NO_CHAR_CHAR,
												CharOperation.NO_CHAR_CHAR,
												CharOperation.concatWith(JAVA_LANG, '.'),
												OBJECT));
						//proposal.setOriginalSignature(null);
						//proposal.setDeclarationPackageName(null);
						//proposal.setDeclarationTypeName(null);
						//proposal.setParameterPackageNames(null);
						//proposal.setParameterTypeNames(null);
						proposal.setPackageName(CharOperation.concatWith(JAVA_LANG, '.'));
						proposal.setTypeName(OBJECT);
						proposal.setName(cloneMethod);
						if (missingElements != null) {
							CompletionProposal[] subProposals = new CompletionProposal[missingElements.length];
							for (int i = 0; i < missingElements.length; i++) {
								subProposals[i] =
									createRequiredTypeProposal(
											missingElements[i],
											missingElementsStarts[i],
											missingElementsEnds[i],
											relevance);
							}
							proposal.setRequiredProposals(subProposals);
						}
						proposal.setCompletion(completion);
						proposal.setFlags(Flags.AccPublic);
						proposal.setReplaceRange(this.startPosition - this.offset, this.endPosition - this.offset);
						proposal.setTokenRange(this.tokenStart - this.offset, this.tokenEnd - this.offset);
						proposal.setRelevance(relevance);
						this.requestor.accept(proposal);
						if(DEBUG) {
							this.printDebug(proposal);
						}
					}
					methodsFound.add(new Object[]{objectRef.getMethods(cloneMethod)[0], objectRef});
				} else {
					if(!this.isIgnored(CompletionProposal.METHOD_REF_WITH_CASTED_RECEIVER, missingElements != null)) {
						InternalCompletionProposal proposal =  createProposal(CompletionProposal.METHOD_REF_WITH_CASTED_RECEIVER, this.actualCompletionPosition);
						proposal.setDeclarationSignature(getSignature(receiverType));
						proposal.setSignature(
								this.compilerOptions.sourceLevel > ClassFileConstants.JDK1_4 && receiverType.isArrayType() ?
										createMethodSignature(
												CharOperation.NO_CHAR_CHAR,
												CharOperation.NO_CHAR_CHAR,
												getSignature(receiverType)) :
										createMethodSignature(
												CharOperation.NO_CHAR_CHAR,
												CharOperation.NO_CHAR_CHAR,
												CharOperation.concatWith(JAVA_LANG, '.'),
												OBJECT));
						proposal.setReceiverSignature(getSignature(receiverType));
						proposal.setPackageName(CharOperation.concatWith(JAVA_LANG, '.'));
						proposal.setTypeName(OBJECT);
						proposal.setName(cloneMethod);
						if (missingElements != null) {
							CompletionProposal[] subProposals = new CompletionProposal[missingElements.length];
							for (int i = 0; i < missingElements.length; i++) {
								subProposals[i] =
									createRequiredTypeProposal(
											missingElements[i],
											missingElementsStarts[i],
											missingElementsEnds[i],
											relevance);
							}
							proposal.setRequiredProposals(subProposals);
						}
						proposal.setCompletion(completion);
						proposal.setFlags(Flags.AccPublic);
						proposal.setReplaceRange(this.startPosition - this.offset, this.endPosition - this.offset);
						proposal.setReceiverRange(receiverStart - this.offset, receiverEnd - this.offset);
						proposal.setTokenRange(this.tokenStart - this.offset, this.tokenEnd - this.offset);
						proposal.setRelevance(relevance);
						this.requestor.accept(proposal);
						if(DEBUG) {
							this.printDebug(proposal);
						}
					}
				}
			}

			receiverType = scope.getJavaLangObject();
		}

		checkCancel();

		if(proposeField) {
			findFields(
				token,
				(ReferenceBinding) receiverType,
				scope,
				fieldsFound,
				new ObjectVector(),
				false,
				invocationSite,
				invocationScope,
				implicitCall,
				false,
				missingElements,
				missingElementsStarts,
				missingElementsEnds,
				missingElementsHaveProblems,
				castedReceiver,
				receiverStart,
				receiverEnd);
		}

		if(proposeMethod) {
			findMethods(
				token,
				null,
				null,
				(ReferenceBinding) receiverType,
				scope,
				methodsFound,
				false,
				false,
				invocationSite,
				invocationScope,
				implicitCall,
				superCall,
				false,
				missingElements,
				missingElementsStarts,
				missingElementsEnds,
				missingElementsHaveProblems,
				castedReceiver,
				receiverStart,
				receiverEnd);
		}
	}

	protected void findFieldsAndMethodsFromAnotherReceiver(
			char[] token,
			TypeReference receiverType,
			Scope scope,
			ObjectVector fieldsFound,
			ObjectVector methodsFound,
			InvocationSite invocationSite,
			Scope invocationScope,
			boolean implicitCall,
			boolean superCall,
			Binding[] missingElements,
			int[] missingElementsStarts,
			int[] missingElementsEnds,
			boolean missingElementsHaveProblems,
			char[][] receiverName,
			int receiverStart,
			int receiverEnd) {

		if (receiverType.resolvedType == null) return;

		TypeBinding receiverTypeBinding = receiverType.resolvedType;
		char[] castedReceiver = null;

		char[] castedTypeChars = CharOperation.concatWith(receiverType.getTypeName(), '.');
		if(this.source != null) {
			int memberRefStart = this.startPosition;

			char[] receiverChars = CharOperation.subarray(this.source, receiverStart, receiverEnd);
			char[] dotChars = CharOperation.subarray(this.source, receiverEnd, memberRefStart);

			castedReceiver =
				CharOperation.concat(
					CharOperation.concat(
						'(',
						CharOperation.concat(
							CharOperation.concat('(', castedTypeChars, ')'),
							receiverChars),
						')'),
					dotChars);
		} else {
			castedReceiver =
				CharOperation.concat(
					CharOperation.concat(
						'(',
						CharOperation.concat(
							CharOperation.concat('(', castedTypeChars, ')'),
							CharOperation.concatWith(receiverName, '.')),
						')'),
					DOT);
		}

		if (castedReceiver == null) return;

		int oldStartPosition = this.startPosition;
		this.startPosition = receiverStart;

		findFieldsAndMethods(
				token,
				receiverTypeBinding,
				scope,
				fieldsFound,
				methodsFound,
				invocationSite,
				invocationScope,
				implicitCall,
				superCall,
				missingElements,
				missingElementsStarts,
				missingElementsEnds,
				missingElementsHaveProblems,
				castedReceiver,
				receiverStart,
				receiverEnd);

		this.startPosition = oldStartPosition;
	}
	private void findFieldsAndMethodsFromCastedReceiver(
			ASTNode enclosingNode,
			Binding qualifiedBinding,
			Scope scope,
			ObjectVector fieldsFound,
			ObjectVector methodsFound,
			InvocationSite invocationSite,
			Scope invocationScope,
			Expression receiver) {

		if (enclosingNode == null) return;

		ASTNode currentEnclosing = enclosingNode;
		while (currentEnclosing != null) {
			InstanceOfExpression instanceOfExpression = findGuardingInstanceOf(currentEnclosing);
			if (instanceOfExpression == null) return;

			TypeReference instanceOfType = instanceOfExpression.type;

			if (instanceOfType.resolvedType == null) return;

			boolean findFromAnotherReceiver = false;

			char[][] receiverName = null;
			int receiverStart = -1;
			int receiverEnd = -1;

			if (receiver instanceof QualifiedNameReference) {
				QualifiedNameReference qualifiedNameReference = (QualifiedNameReference) receiver;

				receiverName = qualifiedNameReference.tokens;

				if (receiverName.length != 1) return;

				receiverStart = (int) (qualifiedNameReference.sourcePositions[0] >>> 32);
				receiverEnd = (int) qualifiedNameReference.sourcePositions[qualifiedNameReference.tokens.length - 1] + 1;

				// if (local instanceof X) local.|
				// if (field instanceof X) field.|
				if (instanceOfExpression.expression instanceof SingleNameReference &&
						((SingleNameReference)instanceOfExpression.expression).binding == qualifiedBinding &&
						(qualifiedBinding instanceof LocalVariableBinding || qualifiedBinding instanceof FieldBinding)) {
					findFromAnotherReceiver = true;
				}

				// if (this.field instanceof X) field.|
				if (instanceOfExpression.expression instanceof FieldReference) {
					FieldReference fieldReference = (FieldReference)instanceOfExpression.expression;

					if (fieldReference.receiver instanceof ThisReference &&
							qualifiedBinding instanceof FieldBinding &&
							fieldReference.binding == qualifiedBinding) {
								findFromAnotherReceiver = true;
					}
				}
			} else if (receiver instanceof FieldReference) {
				FieldReference fieldReference1 = (FieldReference) receiver;

				receiverStart = fieldReference1.sourceStart;
				receiverEnd = fieldReference1.sourceEnd + 1;

				if (fieldReference1.receiver instanceof ThisReference) {

					receiverName = new char[][] {THIS, fieldReference1.token};

					// if (field instanceof X) this.field.|
					if (instanceOfExpression.expression instanceof SingleNameReference &&
							((SingleNameReference)instanceOfExpression.expression).binding == fieldReference1.binding) {
						findFromAnotherReceiver = true;
					}

					// if (this.field instanceof X) this.field.|
					if (instanceOfExpression.expression instanceof FieldReference) {
						FieldReference fieldReference2 = (FieldReference)instanceOfExpression.expression;

						if (fieldReference2.receiver instanceof ThisReference &&
								fieldReference2.binding == fieldReference1.binding) {
									findFromAnotherReceiver = true;
						}
					}
				}
			}

			if (findFromAnotherReceiver) {
				TypeBinding receiverTypeBinding = instanceOfType.resolvedType;
				char[] castedReceiver = null;

				char[] castedTypeChars = CharOperation.concatWith(instanceOfType.getTypeName(), '.');
				if(this.source != null) {
					int memberRefStart = this.startPosition;

					char[] receiverChars = CharOperation.subarray(this.source, receiverStart, receiverEnd);
					char[] dotChars = CharOperation.subarray(this.source, receiverEnd, memberRefStart);

					castedReceiver =
						CharOperation.concat(
							CharOperation.concat(
								'(',
								CharOperation.concat(
									CharOperation.concat('(', castedTypeChars, ')'),
									receiverChars),
								')'),
							dotChars);
				} else {
					castedReceiver =
						CharOperation.concat(
							CharOperation.concat(
								'(',
								CharOperation.concat(
									CharOperation.concat('(', castedTypeChars, ')'),
									CharOperation.concatWith(receiverName, '.')),
								')'),
							DOT);
				}

				if (castedReceiver == null) return;

				int oldStartPosition = this.startPosition;
				this.startPosition = receiverStart;

				findFieldsAndMethods(
						this.completionToken,
						receiverTypeBinding,
						scope,
						fieldsFound,
						methodsFound,
						invocationSite,
						invocationScope,
						false,
						false,
						null,
						null,
						null,
						false,
						castedReceiver,
						receiverStart,
						receiverEnd);

				this.startPosition = oldStartPosition;
			}
			// traverse the enclosing node to find the instanceof expression corresponding
			// to the completion node (if any)
			// https://bugs.eclipse.org/bugs/show_bug.cgi?id=304006
			currentEnclosing = findGuardedInner(currentEnclosing);
		}
	}

	private InstanceOfExpression findGuardingInstanceOf(ASTNode node) {
		// CompletionNodeDetector selects the enclosing node such that it will contain the assist node only in the positive branch.
		// Thus we don't have to check here in which branch we are asking for completion.
		ASTNode left = null;
		if (node instanceof IfStatement) {
			left = ((IfStatement) node).condition;
		} else if (node instanceof AND_AND_Expression) {
			left = ((AND_AND_Expression) node).left;
		} else {
			return null;
		}
		if (left instanceof InstanceOfExpression) {
			return (InstanceOfExpression) left;
		}
		return null;
	}

	private ASTNode findGuardedInner(ASTNode currentEnclosing) {
		if (currentEnclosing instanceof IfStatement) {
			Statement thenStatement = ((IfStatement) currentEnclosing).thenStatement;
			if (thenStatement instanceof Block) {
				Block block = (Block) thenStatement;
				if (block.statements != null && block.statements.length > 0)
					return block.statements[0];
			}
			return thenStatement;
		} else if (currentEnclosing instanceof AND_AND_Expression) {
			return ((AND_AND_Expression) currentEnclosing).right;
		}
		return null;
	}

	private void findFieldsAndMethodsFromFavorites(
			char[] token,
			Scope scope,
			InvocationSite invocationSite,
			Scope invocationScope,
			ObjectVector localsFound,
			ObjectVector fieldsFound,
			ObjectVector methodsFound) {

		ObjectVector methodsFoundFromFavorites = new ObjectVector();

		ImportBinding[] favoriteBindings = getFavoriteReferenceBindings(invocationScope);

		if (favoriteBindings != null && favoriteBindings.length > 0) {
			for (int i = 0; i < favoriteBindings.length; i++) {
				ImportBinding favoriteBinding = favoriteBindings[i];
				switch (favoriteBinding.resolvedImport.kind()) {
					case Binding.FIELD:
						FieldBinding fieldBinding = (FieldBinding) favoriteBinding.resolvedImport;
						findFieldsFromFavorites(
								token,
								new FieldBinding[]{fieldBinding},
								scope,
								fieldsFound,
								localsFound,
								fieldBinding.declaringClass,
								invocationSite,
								invocationScope);
						break;
					case Binding.METHOD:
						MethodBinding methodBinding = (MethodBinding) favoriteBinding.resolvedImport;
						MethodBinding[] methods = methodBinding.declaringClass.availableMethods();
						long range;
						if ((range = ReferenceBinding.binarySearch(methodBinding.selector, methods)) >= 0) {
							int start = (int) range, end = (int) (range >> 32);
							int length = end - start + 1;
							System.arraycopy(methods, start, methods = new MethodBinding[length], 0, length);
						} else {
							methods = Binding.NO_METHODS;
						}
						findLocalMethodsFromFavorites(
								token,
								methods,
								scope,
								methodsFound,
								methodsFoundFromFavorites,
								methodBinding.declaringClass,
								invocationSite,
								invocationScope);
						break;
					case Binding.TYPE:
						ReferenceBinding referenceBinding = (ReferenceBinding) favoriteBinding.resolvedImport;
						if(favoriteBinding.onDemand) {
							findFieldsFromFavorites(
									token,
									referenceBinding.availableFields(),
									scope,
									fieldsFound,
									localsFound,
									referenceBinding,
									invocationSite,
									invocationScope);

							findLocalMethodsFromFavorites(
									token,
									referenceBinding.availableMethods(),
									scope,
									methodsFound,
									methodsFoundFromFavorites,
									referenceBinding,
									invocationSite,
									invocationScope);
						}
						break;
				}
			}
		}

		methodsFound.addAll(methodsFoundFromFavorites);
	}

	private boolean findFieldsAndMethodsFromMissingFieldType(
		char[] token,
		Scope scope,
		InvocationSite invocationSite,
		boolean insideTypeAnnotation) {

		boolean foundSomeFields = false;

		Scope currentScope = scope;

		done : while (true) { // done when a COMPILATION_UNIT_SCOPE is found

			switch (currentScope.kind) {

				case Scope.CLASS_SCOPE :
					ClassScope classScope = (ClassScope) currentScope;
					if(!insideTypeAnnotation) {

						FieldDeclaration[] fields = classScope.referenceContext.fields;

						int fieldsCount = fields == null ? 0 : fields.length;
						for (int i = 0; i < fieldsCount; i++) {
							FieldDeclaration fieldDeclaration = fields[i];
							if (CharOperation.equals(fieldDeclaration.name, token)) {
								FieldBinding fieldBinding = fieldDeclaration.binding;
								if (fieldBinding == null || fieldBinding.type == null  || (fieldBinding.type.tagBits & TagBits.HasMissingType) != 0) {
									foundSomeFields = true;
									findFieldsAndMethodsFromMissingType(
											fieldDeclaration.type,
											currentScope,
											invocationSite,
											scope);
								}
								break done;
							}
						}
					}
					insideTypeAnnotation = false;
					break;
				case Scope.COMPILATION_UNIT_SCOPE :
					break done;
			}
			currentScope = currentScope.parent;
		}
		return foundSomeFields;
	}

	private void findFieldsAndMethodsFromMissingReturnType(
		char[] token,
		TypeBinding[] arguments,
		Scope scope,
		InvocationSite invocationSite,
		boolean insideTypeAnnotation) {

		Scope currentScope = scope;

		done : while (true) { // done when a COMPILATION_UNIT_SCOPE is found

			switch (currentScope.kind) {

				case Scope.CLASS_SCOPE :
					ClassScope classScope = (ClassScope) currentScope;
					if(!insideTypeAnnotation) {

						AbstractMethodDeclaration[] methods = classScope.referenceContext.methods;

						int methodsCount = methods == null ? 0 : methods.length;
						for (int i = 0; i < methodsCount; i++) {
							AbstractMethodDeclaration methodDeclaration = methods[i];
							if (methodDeclaration instanceof MethodDeclaration &&
									CharOperation.equals(methodDeclaration.selector, token)) {
								MethodDeclaration method = (MethodDeclaration) methodDeclaration;
								MethodBinding methodBinding = method.binding;
								if (methodBinding == null || methodBinding.returnType == null  || (methodBinding.returnType.tagBits & TagBits.HasMissingType) != 0) {
									Argument[] parameters = method.arguments;
									int parametersLength = parameters == null ? 0 : parameters.length;
									int argumentsLength = arguments == null ? 0 : arguments.length;

									if (parametersLength == 0) {
										if (argumentsLength == 0) {
											findFieldsAndMethodsFromMissingType(
													method.returnType,
													currentScope,
													invocationSite,
													scope);
											break done;
										}
									} else {
										TypeBinding[] parametersBindings;
										if (methodBinding == null) { // since no binding, extra types from type references
											parametersBindings = new TypeBinding[parametersLength];
											for (int j = 0; j < parametersLength; j++) {
												TypeBinding parameterType = parameters[j].type.resolvedType;
												if (!parameterType.isValidBinding() && parameterType.closestMatch() != null) {
													parameterType = parameterType.closestMatch();
												}
												parametersBindings[j] = parameterType;
											}
										} else {
											parametersBindings = methodBinding.parameters;
										}
										if(areParametersCompatibleWith(parametersBindings, arguments, parameters[parametersLength - 1].isVarArgs())) {
											findFieldsAndMethodsFromMissingType(
													method.returnType,
													currentScope,
													invocationSite,
													scope);
											break done;
										}
									}
								}

							}
						}
					}
					insideTypeAnnotation = false;
					break;
				case Scope.COMPILATION_UNIT_SCOPE :
					break done;
			}
			currentScope = currentScope.parent;
		}
	}

	private void findFieldsAndMethodsFromMissingType(
			TypeReference typeRef,
			final Scope scope,
			final InvocationSite invocationSite,
			final Scope invocationScope) {
		MissingTypesGuesser missingTypesConverter = new MissingTypesGuesser(this);
		MissingTypesGuesser.GuessedTypeRequestor substitutionRequestor =
			new MissingTypesGuesser.GuessedTypeRequestor() {
				@Override
				public void accept(
						TypeBinding guessedType,
						Binding[] missingElements,
						int[] missingElementsStarts,
						int[] missingElementsEnds,
						boolean hasProblems) {
					findFieldsAndMethods(
						CompletionEngine.this.completionToken,
						guessedType,
						scope,
						new ObjectVector(),
						new ObjectVector(),
						invocationSite,
						invocationScope,
						false,
						false,
						missingElements,
						missingElementsStarts,
						missingElementsEnds,
						hasProblems,
						null,
						-1,
						-1);

				}
			};
		missingTypesConverter.guess(typeRef, scope, substitutionRequestor);
	}

	private void findFieldsAndMethodsFromStaticImports(
			char[] token,
			Scope scope,
			InvocationSite invocationSite,
			Scope invocationScope,
			boolean exactMatch,
			boolean insideAnnotationAttribute,
			ObjectVector localsFound,
			ObjectVector fieldsFound,
			ObjectVector methodsFound,
			boolean proposeField,
			boolean proposeMethod) {
		// search in static import
		ImportBinding[] importBindings = scope.compilationUnitScope().imports;
		for (int i = 0; i < importBindings.length; i++) {
			ImportBinding importBinding = importBindings[i];
			if(importBinding.isValidBinding() && importBinding.isStatic()) {
				Binding binding = importBinding.resolvedImport;
				if(binding != null && binding.isValidBinding()) {
					if(importBinding.onDemand) {
						if((binding.kind() & Binding.TYPE) != 0) {
							if(proposeField) {
								findFields(
									token,
									(ReferenceBinding)binding,
									scope,
									fieldsFound,
									localsFound,
									true,
									invocationSite,
									invocationScope,
									true,
									false,
									null,
									null,
									null,
									false,
									null,
									-1,
									-1);
							}
							if(proposeMethod && !insideAnnotationAttribute) {
								findMethods(
									token,
									null,
									null,
									(ReferenceBinding)binding,
									scope,
									methodsFound,
									true,
									exactMatch,
									invocationSite,
									invocationScope,
									true,
									false,
									false,
									null,
									null,
									null,
									false,
									null,
									-1,
									-1);
							}
						}
					} else {
						if ((binding.kind() & Binding.FIELD) != 0) {
							if(proposeField) {
									findFields(
											token,
											new FieldBinding[]{(FieldBinding)binding},
											scope,
											fieldsFound,
											localsFound,
											true,
											((FieldBinding)binding).declaringClass,
											invocationSite,
											invocationScope,
											true,
											false,
											null,
											null,
											null,
											false,
											null,
											-1,
											-1);
							}
						} else if ((binding.kind() & Binding.METHOD) != 0) {
							if(proposeMethod && !insideAnnotationAttribute) {
								MethodBinding methodBinding = (MethodBinding)binding;
								if ((exactMatch && CharOperation.equals(token, methodBinding.selector)) ||
										!exactMatch && CharOperation.prefixEquals(token, methodBinding.selector, false) ||
										(this.options.camelCaseMatch && CharOperation.camelCaseMatch(token, methodBinding.selector))) {
									findLocalMethodsFromStaticImports(
											token,
											methodBinding.declaringClass.getMethods(methodBinding.selector),
											scope,
											exactMatch,
											methodsFound,
											methodBinding.declaringClass,
											invocationSite);
								}
							}
						}
					}
				}
			}
		}
	}

	private void findFieldsFromFavorites(
			char[] fieldName,
			FieldBinding[] fields,
			Scope scope,
			ObjectVector fieldsFound,
			ObjectVector localsFound,
			ReferenceBinding receiverType,
			InvocationSite invocationSite,
			Scope invocationScope) {

		char[] typeName = CharOperation.concatWith(receiverType.compoundName, '.');

		int fieldLength = fieldName.length;
		next : for (int f = fields.length; --f >= 0;) {
			FieldBinding field = fields[f];

			if (field.isSynthetic())	continue next;

			// only static fields must be proposed
			if (!field.isStatic()) continue next;

			if (fieldLength > field.name.length) continue next;

			if (isFailedMatch(fieldName, field.name))	continue next;

			if (this.options.checkDeprecation &&
					field.isViewedAsDeprecated() &&
					!scope.isDefinedInSameUnit(field.declaringClass))
				continue next;

			if (this.options.checkVisibility
				&& !field.canBeSeenBy(receiverType, invocationSite, scope))	continue next;

			for (int i = fieldsFound.size; --i >= 0;) {
				Object[] other = (Object[])fieldsFound.elementAt(i);
				FieldBinding otherField = (FieldBinding) other[0];

				if (field == otherField) continue next;
			}

			fieldsFound.add(new Object[]{field, receiverType});

			int relevance = computeBaseRelevance();
			relevance += computeRelevanceForResolution();
			relevance += computeRelevanceForInterestingProposal(field);
			relevance += computeRelevanceForCaseMatching(fieldName, field.name);
			relevance += computeRelevanceForExpectingType(field.type);
			relevance += computeRelevanceForEnumConstant(field.type);
			relevance += computeRelevanceForStatic(true, true);
			relevance += computeRelevanceForRestrictions(IAccessRule.K_ACCESSIBLE);

			CompilationUnitDeclaration cu = this.unitScope.referenceContext;
			int importStart = cu.types[0].declarationSourceStart;
			int importEnd = importStart;

			this.noProposal = false;

			if (this.compilerOptions.complianceLevel < ClassFileConstants.JDK1_5 ||
					!this.options.suggestStaticImport) {
				if (!this.isIgnored(CompletionProposal.FIELD_REF, CompletionProposal.TYPE_IMPORT)) {
					char[] completion = CharOperation.concat(receiverType.sourceName, field.name, '.');

					InternalCompletionProposal proposal =  createProposal(CompletionProposal.FIELD_REF, this.actualCompletionPosition);
					proposal.setBinding(field);
					proposal.setDeclarationSignature(getSignature(field.declaringClass));
					proposal.setSignature(getSignature(field.type));
					proposal.setDeclarationPackageName(field.declaringClass.qualifiedPackageName());
					proposal.setDeclarationTypeName(field.declaringClass.qualifiedSourceName());
					proposal.setPackageName(field.type.qualifiedPackageName());
					proposal.setTypeName(field.type.qualifiedSourceName());
					proposal.setName(field.name);
					proposal.setCompletion(completion);
					proposal.setFlags(field.modifiers);
					proposal.setReplaceRange(this.startPosition - this.offset, this.endPosition - this.offset);
					proposal.setTokenRange(this.tokenStart - this.offset, this.tokenEnd - this.offset);
					proposal.setRelevance(relevance);

					char[] typeImportCompletion = createImportCharArray(typeName, false, false);

					InternalCompletionProposal typeImportProposal = createProposal(CompletionProposal.TYPE_IMPORT, this.actualCompletionPosition);
					typeImportProposal.nameLookup = this.nameEnvironment.nameLookup;
					typeImportProposal.completionEngine = this;
					char[] packageName = receiverType.qualifiedPackageName();
					typeImportProposal.setDeclarationSignature(packageName);
					typeImportProposal.setSignature(getSignature(receiverType));
					typeImportProposal.setPackageName(packageName);
					typeImportProposal.setTypeName(receiverType.qualifiedSourceName());
					typeImportProposal.setCompletion(typeImportCompletion);
					typeImportProposal.setFlags(receiverType.modifiers);
					typeImportProposal.setAdditionalFlags(CompletionFlags.Default);
					typeImportProposal.setReplaceRange(importStart - this.offset, importEnd - this.offset);
					typeImportProposal.setTokenRange(importStart - this.offset, importEnd - this.offset);
					typeImportProposal.setRelevance(relevance);

					proposal.setRequiredProposals(new CompletionProposal[]{typeImportProposal});

					this.requestor.accept(proposal);
					if(DEBUG) {
						this.printDebug(proposal);
					}
				}
			} else {
				if (!this.isIgnored(CompletionProposal.FIELD_REF, CompletionProposal.FIELD_IMPORT)) {
					char[] completion = field.name;

					InternalCompletionProposal proposal =  createProposal(CompletionProposal.FIELD_REF, this.actualCompletionPosition);
					proposal.setBinding(field);
					proposal.setDeclarationSignature(getSignature(field.declaringClass));
					proposal.setSignature(getSignature(field.type));
					proposal.setDeclarationPackageName(field.declaringClass.qualifiedPackageName());
					proposal.setDeclarationTypeName(field.declaringClass.qualifiedSourceName());
					proposal.setPackageName(field.type.qualifiedPackageName());
					proposal.setTypeName(field.type.qualifiedSourceName());
					proposal.setName(field.name);
					proposal.setCompletion(completion);
					proposal.setFlags(field.modifiers);
					proposal.setReplaceRange(this.startPosition - this.offset, this.endPosition - this.offset);
					proposal.setTokenRange(this.tokenStart - this.offset, this.tokenEnd - this.offset);
					proposal.setRelevance(relevance);

					char[] fieldImportCompletion = createImportCharArray(CharOperation.concat(typeName, field.name, '.'), true, false);

					InternalCompletionProposal fieldImportProposal = createProposal(CompletionProposal.FIELD_IMPORT, this.actualCompletionPosition);
					fieldImportProposal.setDeclarationSignature(getSignature(field.declaringClass));
					fieldImportProposal.setSignature(getSignature(field.type));
					fieldImportProposal.setDeclarationPackageName(field.declaringClass.qualifiedPackageName());
					fieldImportProposal.setDeclarationTypeName(field.declaringClass.qualifiedSourceName());
					fieldImportProposal.setPackageName(field.type.qualifiedPackageName());
					fieldImportProposal.setTypeName(field.type.qualifiedSourceName());
					fieldImportProposal.setName(field.name);
					fieldImportProposal.setCompletion(fieldImportCompletion);
					fieldImportProposal.setFlags(field.modifiers);
					fieldImportProposal.setAdditionalFlags(CompletionFlags.StaticImport);
					fieldImportProposal.setReplaceRange(importStart - this.offset, importEnd - this.offset);
					fieldImportProposal.setTokenRange(importStart - this.offset, importEnd - this.offset);
					fieldImportProposal.setRelevance(relevance);

					proposal.setRequiredProposals(new CompletionProposal[]{fieldImportProposal});

					this.requestor.accept(proposal);
					if(DEBUG) {
						this.printDebug(proposal);
					}
				}
			}
		}
	}
	private void findImplicitMessageSends(
		char[] token,
		TypeBinding[] argTypes,
		Scope scope,
		InvocationSite invocationSite,
		Scope invocationScope,
		ObjectVector methodsFound) {

		if (token == null)
			return;

		boolean staticsOnly = false;
		// need to know if we're in a static context (or inside a constructor)

		done : while (true) { // done when a COMPILATION_UNIT_SCOPE is found

			switch (scope.kind) {

				case Scope.METHOD_SCOPE :
					// handle the error case inside an explicit constructor call (see MethodScope>>findField)
					MethodScope methodScope = (MethodScope) scope;
					staticsOnly |= methodScope.isStatic | methodScope.isConstructorCall;
					break;

				case Scope.CLASS_SCOPE :
					ClassScope classScope = (ClassScope) scope;
					SourceTypeBinding enclosingType = classScope.referenceContext.binding;
					findMethods(
						token,
						null,
						argTypes,
						enclosingType,
						classScope,
						methodsFound,
						staticsOnly,
						true,
						invocationSite,
						invocationScope,
						true,
						false,
						true,
						null,
						null,
						null,
						false,
						null,
						-1,
						-1);
					staticsOnly |= enclosingType.isStatic();
					break;

				case Scope.COMPILATION_UNIT_SCOPE :
					break done;
			}
			scope = scope.parent;
		}
	}
	private void findImports(CompletionOnImportReference importReference, boolean findMembers) {
		char[][] tokens = importReference.tokens;

		char[] importName = CharOperation.concatWith(tokens, '.');

		if (importName.length == 0)
			return;

		char[] lastToken = tokens[tokens.length - 1];
		if(lastToken != null && lastToken.length == 0)
			importName = CharOperation.concat(importName, new char[]{'.'});

		this.resolvingImports = true;
		this.resolvingStaticImports = importReference.isStatic();

		this.completionToken =  lastToken;
		this.qualifiedCompletionToken = importName;

		// want to replace the existing .*;
		if(!this.requestor.isIgnored(CompletionProposal.PACKAGE_REF)) {
			int oldStart = this.startPosition;
			int oldEnd = this.endPosition;
			setSourceRange(
				importReference.sourceStart,
				importReference.declarationSourceEnd);
			try {
				this.nameEnvironment.findPackages(importName, this, this.javaProject.getAllPackageFragmentRoots(), true);
			} catch (JavaModelException e) {
				// silent
			}
			setSourceRange(
				oldStart,
				oldEnd - 1,
				false);
		}
		if(!this.requestor.isIgnored(CompletionProposal.TYPE_REF)) {
			this.foundTypesCount = 0;
			this.nameEnvironment.findTypes(
					importName,
					findMembers,
					getTypesMatchRule(),
					IJavaSearchConstants.TYPE,
					false,
					this,
					this.monitor);
			acceptTypes(null);
		}
	}

	private void findImportsOfMemberTypes(char[] typeName,	ReferenceBinding ref, boolean onlyStatic) {
		ReferenceBinding[] memberTypes = ref.memberTypes();

		int typeLength = typeName.length;
		next : for (int m = memberTypes.length; --m >= 0;) {
			ReferenceBinding memberType = memberTypes[m];
			//		if (!wantClasses && memberType.isClass()) continue next;
			//		if (!wantInterfaces && memberType.isInterface()) continue next;

			if (onlyStatic && !memberType.isStatic())
				continue next;

			if (typeLength > memberType.sourceName.length)
				continue next;

			if (isFailedMatch(typeName, memberType.sourceName))
				continue next;

			if (this.options.checkDeprecation && memberType.isViewedAsDeprecated()) continue next;

			if (this.options.checkVisibility
				&& !memberType.canBeSeenBy(this.unitScope.fPackage))
				continue next;

			char[] completionName = CharOperation.concat(memberType.sourceName, SEMICOLON);

			int relevance = computeBaseRelevance();
			relevance += computeRelevanceForResolution();
			relevance += computeRelevanceForInterestingProposal();
			relevance += computeRelevanceForCaseMatching(typeName, memberType.sourceName);
			relevance += computeRelevanceForRestrictions(IAccessRule.K_ACCESSIBLE);

			if (memberType.isClass()) {
				relevance += computeRelevanceForClass();
			} else if(memberType.isEnum()) {
				relevance += computeRelevanceForEnum();
			} else if (memberType.isInterface()) {
				relevance += computeRelevanceForInterface();
			}
			this.noProposal = false;
			if(!this.requestor.isIgnored(CompletionProposal.TYPE_REF)) {
				createTypeProposal(
						memberType,
						memberType.qualifiedSourceName(),
						IAccessRule.K_ACCESSIBLE,
						completionName,
						relevance,
						null,
						null,
						null,
						false);
			}
		}
	}

	private void findImportsOfStaticFields(char[] fieldName, ReferenceBinding ref) {
		FieldBinding[] fields = ref.availableFields();

		int fieldLength = fieldName.length;
		next : for (int m = fields.length; --m >= 0;) {
			FieldBinding field = fields[m];

			if (fieldLength > field.name.length)
				continue next;

			if (field.isSynthetic())
				continue next;

			if (!field.isStatic())
				continue next;

			if (isFailedMatch(fieldName, field.name))
				continue next;

			if (this.options.checkDeprecation && field.isViewedAsDeprecated()) continue next;

			if (this.options.checkVisibility
				&& !field.canBeSeenBy(this.unitScope.fPackage))
				continue next;

			char[] completionName = CharOperation.concat(field.name, SEMICOLON);

			int relevance = computeBaseRelevance();
			relevance += computeRelevanceForResolution();
			relevance += computeRelevanceForInterestingProposal();
			relevance += computeRelevanceForCaseMatching(fieldName, field.name);
			relevance += computeRelevanceForRestrictions(IAccessRule.K_ACCESSIBLE);

			this.noProposal = false;
			if(!this.requestor.isIgnored(CompletionProposal.FIELD_REF)) {
				InternalCompletionProposal proposal =  createProposal(CompletionProposal.FIELD_REF, this.actualCompletionPosition);
				proposal.setBinding(field);
				proposal.setDeclarationSignature(getSignature(field.declaringClass));
				proposal.setSignature(getSignature(field.type));
				proposal.setDeclarationPackageName(field.declaringClass.qualifiedPackageName());
				proposal.setDeclarationTypeName(field.declaringClass.qualifiedSourceName());
				proposal.setPackageName(field.type.qualifiedPackageName());
				proposal.setTypeName(field.type.qualifiedSourceName());
				proposal.setName(field.name);
				proposal.setCompletion(completionName);
				proposal.setFlags(field.modifiers);
				proposal.setReplaceRange(this.startPosition - this.offset, this.endPosition - this.offset);
				proposal.setTokenRange(this.tokenStart - this.offset, this.tokenEnd - this.offset);
				proposal.setRelevance(relevance);
				this.requestor.accept(proposal);
				if(DEBUG) {
					this.printDebug(proposal);
				}
			}
		}
	}

	private void findImportsOfStaticMethods(char[] methodName, ReferenceBinding ref) {
		MethodBinding[] methods = ref.availableMethods();

		int methodLength = methodName.length;
		next : for (int m = methods.length; --m >= 0;) {
			MethodBinding method = methods[m];

			if (method.isSynthetic()) continue next;

			if (method.isDefaultAbstract())	continue next;

			if (method.isConstructor()) continue next;

			if (!method.isStatic()) continue next;

			if (this.options.checkDeprecation && method.isViewedAsDeprecated()) continue next;

			if (this.options.checkVisibility
				&& !method.canBeSeenBy(this.unitScope.fPackage)) continue next;

			if (methodLength > method.selector.length)
				continue next;

			if (isFailedMatch(methodName, method.selector))
				continue next;

			int length = method.parameters.length;
			char[][] parameterPackageNames = new char[length][];
			char[][] parameterTypeNames = new char[length][];

			for (int i = 0; i < length; i++) {
				TypeBinding type = method.original().parameters[i];
				parameterPackageNames[i] = type.qualifiedPackageName();
				parameterTypeNames[i] = type.qualifiedSourceName();
			}
			char[][] parameterNames = findMethodParameterNames(method,parameterTypeNames);

			char[] completionName = CharOperation.concat(method.selector, SEMICOLON);

			int relevance = computeBaseRelevance();
			relevance += computeRelevanceForResolution();
			relevance += computeRelevanceForInterestingProposal();
			relevance += computeRelevanceForCaseMatching(methodName, method.selector);
			relevance += computeRelevanceForRestrictions(IAccessRule.K_ACCESSIBLE);

			this.noProposal = false;
			if(!this.requestor.isIgnored(CompletionProposal.METHOD_NAME_REFERENCE)) {
				InternalCompletionProposal proposal =  createProposal(CompletionProposal.METHOD_NAME_REFERENCE, this.actualCompletionPosition);
				proposal.setDeclarationSignature(getSignature(method.declaringClass));
				proposal.setSignature(getSignature(method));
				proposal.setDeclarationPackageName(method.declaringClass.qualifiedPackageName());
				proposal.setDeclarationTypeName(method.declaringClass.qualifiedSourceName());
				proposal.setParameterPackageNames(parameterPackageNames);
				proposal.setParameterTypeNames(parameterTypeNames);
				proposal.setPackageName(method.returnType.qualifiedPackageName());
				proposal.setTypeName(method.returnType.qualifiedSourceName());
				proposal.setName(method.selector);
				proposal.setCompletion(completionName);
				proposal.setFlags(method.modifiers);
				proposal.setReplaceRange(this.startPosition - this.offset, this.endPosition - this.offset);
				proposal.setTokenRange(this.tokenStart - this.offset, this.tokenEnd - this.offset);
				proposal.setRelevance(relevance);
				if(parameterNames != null) proposal.setParameterNames(parameterNames);
				this.requestor.accept(proposal);
				if(DEBUG) {
					this.printDebug(proposal);
				}
			}
		}
	}

	private void findInterfacesMethodDeclarations(
		char[] selector,
		ReferenceBinding receiverType,
		ReferenceBinding[] itsInterfaces,
		Scope scope,
		ObjectVector methodsFound,
		Binding[] missingElements,
		int[] missingElementssStarts,
		int[] missingElementsEnds,
		boolean missingElementsHaveProblems) {

		if (selector == null)
			return;

		if (itsInterfaces != Binding.NO_SUPERINTERFACES) {
			ReferenceBinding[] interfacesToVisit = itsInterfaces;
			int nextPosition = interfacesToVisit.length;

			for (int i = 0; i < nextPosition; i++) {
				ReferenceBinding currentType = interfacesToVisit[i];
				MethodBinding[] methods = currentType.availableMethods();
				if(methods != null) {
					findLocalMethodDeclarations(
						selector,
						methods,
						scope,
						methodsFound,
						false,
						receiverType);
				}

				itsInterfaces = currentType.superInterfaces();
				if (itsInterfaces != null && itsInterfaces != Binding.NO_SUPERINTERFACES) {
					int itsLength = itsInterfaces.length;
					if (nextPosition + itsLength >= interfacesToVisit.length)
						System.arraycopy(interfacesToVisit, 0, interfacesToVisit = new ReferenceBinding[nextPosition + itsLength + 5], 0, nextPosition);
					nextInterface : for (int a = 0; a < itsLength; a++) {
						ReferenceBinding next = itsInterfaces[a];
						for (int b = 0; b < nextPosition; b++)
							if (TypeBinding.equalsEquals(next, interfacesToVisit[b])) continue nextInterface;
						interfacesToVisit[nextPosition++] = next;
					}
				}
			}
		}
	}

	private void findInterfacesMethods(
		char[] selector,
		TypeBinding[] typeArgTypes,
		TypeBinding[] argTypes,
		ReferenceBinding receiverType,
		ReferenceBinding[] itsInterfaces,
		Scope scope,
		ObjectVector methodsFound,
		boolean onlyStaticMethods,
		boolean exactMatch,
		InvocationSite invocationSite,
		Scope invocationScope,
		boolean implicitCall,
		boolean superCall,
		boolean canBePrefixed,
		Binding[] missingElements,
		int[] missingElementssStarts,
		int[] missingElementsEnds,
		boolean missingElementsHaveProblems,
		char[] castedReceiver,
		int receiverStart,
		int receiverEnd) {

		if (selector == null)
			return;

		if (itsInterfaces != Binding.NO_SUPERINTERFACES) {
			ReferenceBinding[] interfacesToVisit = itsInterfaces;
			int nextPosition = interfacesToVisit.length;

			for (int i = 0; i < nextPosition; i++) {
				ReferenceBinding currentType = interfacesToVisit[i];
				MethodBinding[] methods = currentType.availableMethods();
				if(methods != null) {
					findLocalMethods(
						selector,
						typeArgTypes,
						argTypes,
						methods,
						scope,
						methodsFound,
						onlyStaticMethods,
						exactMatch,
						receiverType,
						invocationSite,
						invocationScope,
						implicitCall,
						superCall,
						canBePrefixed,
						missingElements,
						missingElementssStarts,
						missingElementsEnds,
						missingElementsHaveProblems,
						castedReceiver,
						receiverStart,
						receiverEnd);
				}

				itsInterfaces = currentType.superInterfaces();
				if (itsInterfaces != null && itsInterfaces != Binding.NO_SUPERINTERFACES) {
					int itsLength = itsInterfaces.length;
					if (nextPosition + itsLength >= interfacesToVisit.length)
						System.arraycopy(interfacesToVisit, 0, interfacesToVisit = new ReferenceBinding[nextPosition + itsLength + 5], 0, nextPosition);
					nextInterface : for (int a = 0; a < itsLength; a++) {
						ReferenceBinding next = itsInterfaces[a];
						for (int b = 0; b < nextPosition; b++)
							if (TypeBinding.equalsEquals(next, interfacesToVisit[b])) continue nextInterface;
						interfacesToVisit[nextPosition++] = next;
					}
				}
			}
		}
	}
	/*
	 * Find javadoc block tags for a given completion javadoc tag node
	 */
	private void findJavadocBlockTags(CompletionOnJavadocTag javadocTag) {
		char[][] possibleTags = javadocTag.getPossibleBlockTags();
		if (possibleTags == null) return;
		int length = possibleTags.length;
		for (int i=0; i<length; i++) {
			int relevance = computeBaseRelevance();
			relevance += computeRelevanceForInterestingProposal();
			relevance += computeRelevanceForRestrictions(IAccessRule.K_ACCESSIBLE); // no access restriction for keywors

			this.noProposal = false;
			if (!this.requestor.isIgnored(CompletionProposal.JAVADOC_BLOCK_TAG)) {
				char[] possibleTag = possibleTags[i];
				InternalCompletionProposal proposal =  createProposal(CompletionProposal.JAVADOC_BLOCK_TAG, this.actualCompletionPosition);
				proposal.setName(possibleTag);
				int tagLength = possibleTag.length;
				char[] completion = new char[1+tagLength];
				completion[0] = '@';
				System.arraycopy(possibleTag, 0, completion, 1, tagLength);
				proposal.setCompletion(completion);
				proposal.setReplaceRange(this.startPosition - this.offset, this.endPosition - this.offset);
				proposal.setTokenRange(this.tokenStart - this.offset, this.tokenEnd - this.offset);
				proposal.setRelevance(relevance);
				this.requestor.accept(proposal);
				if (DEBUG) {
					this.printDebug(proposal);
				}
			}
		}
	}

	/*
	 * Find javadoc inline tags for a given completion javadoc tag node
	 */
	private void findJavadocInlineTags(CompletionOnJavadocTag javadocTag) {
		char[][] possibleTags = javadocTag.getPossibleInlineTags();
		if (possibleTags == null) return;
		int length = possibleTags.length;
		for (int i=0; i<length; i++) {
			int relevance = computeBaseRelevance();
			relevance += computeRelevanceForInterestingProposal();
			relevance += computeRelevanceForRestrictions(IAccessRule.K_ACCESSIBLE); // no access restriction for keywors

			this.noProposal = false;
			if (!this.requestor.isIgnored(CompletionProposal.JAVADOC_INLINE_TAG)) {
				char[] possibleTag = possibleTags[i];
				InternalCompletionProposal proposal =  createProposal(CompletionProposal.JAVADOC_INLINE_TAG, this.actualCompletionPosition);
				proposal.setName(possibleTag);
				int tagLength = possibleTag.length;
//				boolean inlineTagStarted = javadocTag.completeInlineTagStarted();
				char[] completion = new char[2+tagLength+1];
				completion[0] = '{';
				completion[1] = '@';
				System.arraycopy(possibleTag, 0, completion, 2, tagLength);
				// do not add space at end of inline tag (see bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=121026)
				//completion[tagLength+2] = ' ';
				completion[tagLength+2] = '}';
				proposal.setCompletion(completion);
				proposal.setReplaceRange(this.startPosition - this.offset, this.endPosition - this.offset);
				proposal.setTokenRange(this.tokenStart - this.offset, this.tokenEnd - this.offset);
				proposal.setRelevance(relevance);
				this.requestor.accept(proposal);
				if (DEBUG) {
					this.printDebug(proposal);
				}
			}
		}
	}

	/*
	 * Find javadoc parameter names.
	 */
	private void findJavadocParamNames(char[] token, char[][] missingParams, boolean isTypeParam) {

		if (missingParams == null) return;

		// Get relevance
		int relevance = computeBaseRelevance();
		relevance += computeRelevanceForInterestingProposal();
		relevance += computeRelevanceForRestrictions(IAccessRule.K_ACCESSIBLE); // no access restriction for param name
		if (!isTypeParam) relevance += R_INTERESTING;

		// Propose missing param
		int length = missingParams.length;
		relevance += length;
		for (int i=0; i<length; i++) {
			char[] argName = missingParams[i];
			if (token == null || CharOperation.prefixEquals(token, argName)) {

				this.noProposal = false;
				if (!this.requestor.isIgnored(CompletionProposal.JAVADOC_PARAM_REF)) {
					InternalCompletionProposal proposal =  createProposal(CompletionProposal.JAVADOC_PARAM_REF, this.actualCompletionPosition);
					proposal.setName(argName);
					char[] completion = isTypeParam ? CharOperation.concat('<', argName, '>') : argName;
					proposal.setCompletion(completion);
					proposal.setReplaceRange(this.startPosition - this.offset, this.endPosition - this.offset);
					proposal.setTokenRange(this.tokenStart - this.offset, this.tokenEnd - this.offset);
					proposal.setRelevance(--relevance);
					this.requestor.accept(proposal);
					if (DEBUG) {
						this.printDebug(proposal);
					}
				}
			}
		}
	}

	// what about onDemand types? Ignore them since it does not happen!
	// import p1.p2.A.*;
	private void findKeywords(char[] keyword, char[][] choices, boolean staticFieldsAndMethodOnly, boolean ignorePackageKeyword) {
		if(choices == null || choices.length == 0) return;
		int length = keyword.length;
		for (int i = 0; i < choices.length; i++)
			if (length <= choices[i].length && (CharOperation.prefixEquals(keyword, choices[i], false /* ignore case */)
					|| (this.options.substringMatch && CharOperation.substringMatch(keyword, choices[i])))) {
				if (ignorePackageKeyword && CharOperation.equals(choices[i], Keywords.PACKAGE))
					continue;
				int relevance = computeBaseRelevance();
				relevance += computeRelevanceForResolution();
				relevance += computeRelevanceForInterestingProposal();
				relevance += computeRelevanceForCaseMatching(keyword, choices[i]);
				relevance += computeRelevanceForRestrictions(IAccessRule.K_ACCESSIBLE); // no access restriction for keywords
				relevance += computeRelevanceForFinalInIOE();
				if (staticFieldsAndMethodOnly && this.insideQualifiedReference) relevance += R_NON_INHERITED;

				if(CharOperation.equals(choices[i], Keywords.TRUE) || CharOperation.equals(choices[i], Keywords.FALSE)) {
					relevance += computeRelevanceForExpectingType(TypeBinding.BOOLEAN);
					relevance += computeRelevanceForQualification(false);
				}
				if (CharOperation.equals(choices[i], Keywords.NEW)) {
					relevance += computeRelevanceForConstructor();
				}
				this.noProposal = false;
				if(!this.requestor.isIgnored(CompletionProposal.KEYWORD)) {
					InternalCompletionProposal proposal =  createProposal(CompletionProposal.KEYWORD, this.actualCompletionPosition);
					proposal.setName(choices[i]);
					proposal.setCompletion(choices[i]);
					proposal.setReplaceRange((this.startPosition < 0) ? 0 : this.startPosition - this.offset, this.endPosition - this.offset);
					proposal.setTokenRange((this.tokenStart < 0) ? 0 : this.tokenStart - this.offset, this.tokenEnd - this.offset);
					proposal.setRelevance(relevance);
					this.requestor.accept(proposal);
					if(DEBUG) {
						this.printDebug(proposal);
					}
				}
			}
	}

	private int computeRelevanceForFinalInIOE() {
		// add expected type else final keyword will be buried inside multiple Fin* classes
		return this.parser.assistNodeParent instanceof InstanceOfExpression ? R_FINAL + R_EXPECTED_TYPE : 0;
	}

	private void findKeywordsForMember(char[] token, int modifiers, ASTNode astNode) {
		char[][] keywords = new char[Keywords.COUNT][];
		int count = 0;

		// visibility
		if((modifiers & ClassFileConstants.AccPrivate) == 0
			&& (modifiers & ClassFileConstants.AccProtected) == 0
			&& (modifiers & ClassFileConstants.AccPublic) == 0) {
			keywords[count++] = Keywords.PROTECTED;
			keywords[count++] = Keywords.PUBLIC;
			if((modifiers & ClassFileConstants.AccAbstract) == 0) {
				keywords[count++] = Keywords.PRIVATE;
			}
		}

		if (astNode instanceof CompletionOnFieldType &&
	        this.compilerOptions.sourceLevel >= ClassFileConstants.JDK1_8) {
	        FieldBinding astNodeBinding = ((CompletionOnFieldType) astNode).binding;
	        ReferenceBinding declaringClass = astNodeBinding != null ? astNodeBinding.declaringClass : null;
	        if (declaringClass != null && declaringClass.isInterface() && !declaringClass.isAnnotationType())
	            keywords[count++] = Keywords.DEFAULT;
	    }
		if((modifiers & ClassFileConstants.AccAbstract) == 0) {
			// abtract
			if((modifiers & ~(ExtraCompilerModifiers.AccVisibilityMASK | ClassFileConstants.AccStatic)) == 0) {
				keywords[count++] = Keywords.ABSTRACT;
			}

			// final
			if((modifiers & ClassFileConstants.AccFinal) == 0) {
				keywords[count++] = Keywords.FINAL;
			}

			// static
			if((modifiers & ClassFileConstants.AccStatic) == 0) {
				keywords[count++] = Keywords.STATIC;
			}

			boolean canBeField = true;
			boolean canBeMethod = true;
			boolean canBeType = true;
			if((modifiers & ClassFileConstants.AccNative) != 0
				|| (modifiers & ClassFileConstants.AccStrictfp) != 0
				|| (modifiers & ClassFileConstants.AccSynchronized) != 0) {
				canBeField = false;
				canBeType = false;
			}

			if((modifiers & ClassFileConstants.AccTransient) != 0
				|| (modifiers & ClassFileConstants.AccVolatile) != 0) {
				canBeMethod = false;
				canBeType = false;
			}

			if(canBeField) {
				// transient
				if((modifiers & ClassFileConstants.AccTransient) == 0) {
					keywords[count++] = Keywords.TRANSIENT;
				}

				// volatile
				if((modifiers & ClassFileConstants.AccVolatile) == 0) {
					keywords[count++] = Keywords.VOLATILE;
				}
			}

			if(canBeMethod) {
				// native
				if((modifiers & ClassFileConstants.AccNative) == 0) {
					keywords[count++] = Keywords.NATIVE;
				}

				// strictfp
				if((modifiers & ClassFileConstants.AccStrictfp) == 0) {
					keywords[count++] = Keywords.STRICTFP;
				}

				// synchronized
				if((modifiers & ClassFileConstants.AccSynchronized) == 0) {
					keywords[count++] = Keywords.SYNCHRONIZED;
				}
			}

			if(canBeType) {
				keywords[count++] = Keywords.CLASS;
				keywords[count++] = Keywords.INTERFACE;

				if((modifiers & ClassFileConstants.AccFinal) == 0) {
					keywords[count++] = Keywords.ENUM;
				}
			}
		} else {
			// class
			keywords[count++] = Keywords.CLASS;
			keywords[count++] = Keywords.INTERFACE;
		}
		System.arraycopy(keywords, 0, keywords = new char[count][], 0, count);

		findKeywords(token, keywords, false, false);
	}
	private void findLabels(char[] label, char[][] choices) {
		if(choices == null || choices.length == 0) return;

		int length = label.length;
		for (int i = 0; i < choices.length; i++) {
			if (length <= choices[i].length
				&& CharOperation.prefixEquals(label, choices[i], false /* ignore case */
			)){
				int relevance = computeBaseRelevance();
				relevance += computeRelevanceForResolution();
				relevance += computeRelevanceForInterestingProposal();
				relevance += computeRelevanceForCaseMatching(label, choices[i]);
				relevance += computeRelevanceForRestrictions(IAccessRule.K_ACCESSIBLE); // no access restriction for keywors

				this.noProposal = false;
				if(!this.requestor.isIgnored(CompletionProposal.LABEL_REF)) {
					InternalCompletionProposal proposal =  createProposal(CompletionProposal.LABEL_REF, this.actualCompletionPosition);
					proposal.setName(choices[i]);
					proposal.setCompletion(choices[i]);
					proposal.setReplaceRange(this.startPosition - this.offset, this.endPosition - this.offset);
					proposal.setTokenRange(this.tokenStart - this.offset, this.tokenEnd - this.offset);
					proposal.setRelevance(relevance);
					this.requestor.accept(proposal);
					if(DEBUG) {
						this.printDebug(proposal);
					}
				}
			}
		}
	}

	// Helper method for findMethods(char[], MethodBinding[], Scope, ObjectVector, boolean, boolean, boolean, TypeBinding)
	private void findLocalMethodDeclarations(
		char[] methodName,
		MethodBinding[] methods,
		Scope scope,
		ObjectVector methodsFound,
		//	boolean noVoidReturnType, how do you know?
		boolean exactMatch,
		ReferenceBinding receiverType) {

		ObjectVector newMethodsFound =  new ObjectVector();
		// Inherited methods which are hidden by subclasses are filtered out
		// No visibility checks can be performed without the scope & invocationSite
		int methodLength = methodName.length;
		next : for (int f = methods.length; --f >= 0;) {

			MethodBinding method = methods[f];
			if (method.isSynthetic())	continue next;

			if (method.isDefaultAbstract()) continue next;

			if (method.isConstructor()) continue next;

			if (method.isFinal()) {
                newMethodsFound.add(method);
                continue next;
            }

			if (this.options.checkDeprecation &&
					method.isViewedAsDeprecated() &&
					!scope.isDefinedInSameUnit(method.declaringClass))
				continue next;

			//		if (noVoidReturnType && method.returnType == BaseTypes.VoidBinding) continue next;
			if(method.isStatic()) continue next;

			if (!method.canBeSeenBy(receiverType, FakeInvocationSite , scope)) continue next;

			if (exactMatch) {
				if (!CharOperation.equals(methodName, method.selector, false /* ignore case */
					))
					continue next;

			} else {

				if (methodLength > method.selector.length)
					continue next;

				if (isFailedMatch(methodName, method.selector))
					continue next;
			}

			for (int i = methodsFound.size; --i >= 0;) {
				MethodBinding otherMethod = (MethodBinding) methodsFound.elementAt(i);
				if (method == otherMethod)
					continue next;

				if (CharOperation.equals(method.selector, otherMethod.selector, true)
						&& this.lookupEnvironment.methodVerifier().isMethodSubsignature(otherMethod, method)) {
					continue next;
				}
			}

			newMethodsFound.add(method);

			int length = method.parameters.length;
			char[][] parameterPackageNames = new char[length][];
			char[][] parameterFullTypeNames = new char[length][];

			for (int i = 0; i < length; i++) {
				TypeBinding type = method.parameters[i];
				parameterPackageNames[i] = type.qualifiedPackageName();
				parameterFullTypeNames[i] = type.qualifiedSourceName();
			}

			char[][] parameterNames = findMethodParameterNames(method, parameterFullTypeNames);

			if(method.typeVariables != null && method.typeVariables.length > 0) {
				char[][] excludedNames = findEnclosingTypeNames(scope);
				char[][] substituedParameterNames = substituteMethodTypeParameterNames(method.typeVariables, excludedNames);
				if(substituedParameterNames != null) {
					method = new ParameterizedMethodBinding(
								method.declaringClass,
								method,
								substituedParameterNames,
								scope.environment());
				}
			}

			StringBuffer completion = new StringBuffer(10);
			if (!exactMatch) {
				createMethod(method, parameterPackageNames, parameterFullTypeNames, parameterNames, scope, completion);
			}

			int relevance = computeBaseRelevance();
			relevance += computeRelevanceForResolution();
			relevance += computeRelevanceForInterestingProposal();
			relevance += computeRelevanceForCaseMatching(methodName, method.selector);
			relevance += R_METHOD_OVERIDE;
			if(method.isAbstract()) relevance += R_ABSTRACT_METHOD;
			relevance += computeRelevanceForRestrictions(IAccessRule.K_ACCESSIBLE);

			this.noProposal = false;
			if(!this.requestor.isIgnored(CompletionProposal.METHOD_DECLARATION)) {
				InternalCompletionProposal proposal =  createProposal(CompletionProposal.METHOD_DECLARATION, this.actualCompletionPosition);
				proposal.setBinding(method);
				proposal.setDeclarationSignature(getSignature(method.declaringClass));
				proposal.setDeclarationKey(method.declaringClass.computeUniqueKey());
				proposal.setSignature(getSignature(method));
				MethodBinding original = method.original();
				if(original != method) {
					proposal.setOriginalSignature(getSignature(original));
				}
				proposal.setKey(method.computeUniqueKey());
				proposal.setDeclarationPackageName(method.declaringClass.qualifiedPackageName());
				proposal.setDeclarationTypeName(method.declaringClass.qualifiedSourceName());
				proposal.setParameterPackageNames(parameterPackageNames);
				proposal.setParameterTypeNames(parameterFullTypeNames);
				proposal.setPackageName(method.returnType.qualifiedPackageName());
				proposal.setTypeName(method.returnType.qualifiedSourceName());
				proposal.setCompletion(completion.toString().toCharArray());
				proposal.setName(method.selector);
				proposal.setFlags(method.modifiers);
				proposal.setReplaceRange(this.startPosition - this.offset, this.endPosition - this.offset);
				proposal.setTokenRange(this.tokenStart - this.offset, this.tokenEnd - this.offset);
				proposal.setRelevance(relevance);
				if(parameterNames != null) proposal.setParameterNames(parameterNames);
				this.requestor.accept(proposal);
				if(DEBUG) {
					this.printDebug(proposal);
				}
			}
		}
		methodsFound.addAll(newMethodsFound);
	}

	// Helper method for findMethods(char[], TypeBinding[], ReferenceBinding, Scope, ObjectVector, boolean, boolean, boolean)
	private void findLocalMethods(
		char[] methodName,
		TypeBinding[] typeArgTypes,
		TypeBinding[] argTypes,
		MethodBinding[] methods,
		Scope scope,
		ObjectVector methodsFound,
		boolean onlyStaticMethods,
		boolean exactMatch,
		ReferenceBinding receiverType,
		InvocationSite invocationSite,
		Scope invocationScope,
		boolean implicitCall,
		boolean superCall,
		boolean canBePrefixed,
		Binding[] missingElements,
		int[] missingElementsStarts,
		int[] missingElementsEnds,
		boolean missingElementsHaveProblems,
		char[] castedReceiver,
		int receiverStart,
		int receiverEnd) {

		boolean completionOnReferenceExpressionName = invocationSite instanceof ReferenceExpression;
		ObjectVector newMethodsFound =  new ObjectVector();
		// Inherited methods which are hidden by subclasses are filtered out
		// No visibility checks can be performed without the scope & invocationSite

		int methodLength = methodName.length;
		int minTypeArgLength = typeArgTypes == null ? 0 : typeArgTypes.length;
		int minArgLength = argTypes == null ? 0 : argTypes.length;

		next : for (int f = methods.length; --f >= 0;) {
			MethodBinding method = methods[f];

			if (method.isSynthetic()) continue next;

			if (method.isDefaultAbstract())	continue next;

			if (method.isConstructor()) continue next;

			if (this.options.checkDeprecation &&
					method.isViewedAsDeprecated() &&
					!scope.isDefinedInSameUnit(method.declaringClass))
				continue next;

			//TODO (david) perhaps the relevance of a void method must be lesser than other methods
			//if (expectedTypesPtr > -1 && method.returnType == BaseTypes.VoidBinding) continue next;

			if (onlyStaticMethods && !method.isStatic()) continue next;

			if (this.options.checkVisibility
				&& !method.canBeSeenBy(receiverType, invocationSite, scope)) continue next;

			if(superCall && method.isAbstract()) {
				methodsFound.add(new Object[]{method, receiverType});
				continue next;
			}

			if (exactMatch) {
				if (!CharOperation.equals(methodName, method.selector, false /* ignore case */)) {
					continue next;
				}
			} else {
				if (methodLength > method.selector.length) continue next;
				if (isFailedMatch(methodName, method.selector)) {
					continue next;
				}
			}

			if (minTypeArgLength != 0 && minTypeArgLength != method.typeVariables.length)
				continue next;

			if (minTypeArgLength != 0) {
				method = scope.environment().createParameterizedGenericMethod(method, typeArgTypes);
			}

			if (minArgLength > method.parameters.length)
				continue next;

			for (int a = minArgLength; --a >= 0;){
				if (argTypes[a] != null) { // can be null if it could not be resolved properly
					if (!argTypes[a].isCompatibleWith(method.parameters[a])) {
						continue next;
					}
				}
			}

			boolean prefixRequired = false;

			for (int i = methodsFound.size; --i >= 0;) {
				Object[] other = (Object[]) methodsFound.elementAt(i);
				MethodBinding otherMethod = (MethodBinding) other[0];
				ReferenceBinding otherReceiverType = (ReferenceBinding) other[1];
				if (method == otherMethod && TypeBinding.equalsEquals(receiverType, otherReceiverType))
					continue next;

				if (CharOperation.equals(method.selector, otherMethod.selector, true)) {
					if (TypeBinding.equalsEquals(receiverType, otherReceiverType)) {
						if (this.lookupEnvironment.methodVerifier().isMethodSubsignature(otherMethod, method)) {
							if (!superCall || !otherMethod.declaringClass.isInterface()) {
								continue next;
							}
						}
					} else {
						if (this.lookupEnvironment.methodVerifier().isMethodSubsignature(otherMethod, method)) {
							if(receiverType.isAnonymousType()) continue next;

							if(!superCall) {
								if(!canBePrefixed) continue next;

								prefixRequired = true;
							}
						}
					}
				}
			}

			newMethodsFound.add(new Object[]{method, receiverType});

			ReferenceBinding superTypeWithSameErasure = (ReferenceBinding)receiverType.findSuperTypeOriginatingFrom(method.declaringClass);
			if (TypeBinding.notEquals(method.declaringClass, superTypeWithSameErasure)) {
				MethodBinding[] otherMethods = superTypeWithSameErasure.getMethods(method.selector);
				for (int i = 0; i < otherMethods.length; i++) {
					if(otherMethods[i].original() == method.original()) {
						method = otherMethods[i];
					}
				}
			}

			int length = method.parameters.length;
			char[][] parameterPackageNames = new char[length][];
			char[][] parameterTypeNames = new char[length][];

				for (int i = 0; i < length; i++) {
					TypeBinding type = method.original().parameters[i];
					parameterPackageNames[i] = type.qualifiedPackageName();
					parameterTypeNames[i] = type.qualifiedSourceName();
				}
			char[][] parameterNames = findMethodParameterNames(method,parameterTypeNames);

			char[] completion = CharOperation.NO_CHAR;

			int previousStartPosition = this.startPosition;
			int previousTokenStart = this.tokenStart;

			// Special case for completion in javadoc
			if (this.assistNodeInJavadoc > 0) {
				Expression receiver = null;
				if (invocationSite instanceof CompletionOnJavadocMessageSend) {
					CompletionOnJavadocMessageSend msg = (CompletionOnJavadocMessageSend) invocationSite;
					receiver = msg.receiver;
				} else if (invocationSite instanceof CompletionOnJavadocFieldReference) {
					CompletionOnJavadocFieldReference fieldRef = (CompletionOnJavadocFieldReference) invocationSite;
					receiver = fieldRef.receiver;
				}
				if (receiver != null) {
					StringBuffer javadocCompletion = new StringBuffer();
					if (receiver.isThis()) {
						if ((this.assistNodeInJavadoc & CompletionOnJavadoc.TEXT) != 0) {
							javadocCompletion.append('#');
						}
					} else if ((this.assistNodeInJavadoc & CompletionOnJavadoc.TEXT) != 0) {
						if (receiver instanceof JavadocSingleTypeReference) {
							JavadocSingleTypeReference typeRef = (JavadocSingleTypeReference) receiver;
							javadocCompletion.append(typeRef.token);
							javadocCompletion.append('#');
						} else if (receiver instanceof JavadocQualifiedTypeReference) {
							JavadocQualifiedTypeReference typeRef = (JavadocQualifiedTypeReference) receiver;
							completion = CharOperation.concat(CharOperation.concatWith(typeRef.tokens, '.'), method.selector, '#');
							for (int t=0,nt =typeRef.tokens.length; t<nt; t++) {
								if (t>0) javadocCompletion.append('.');
								javadocCompletion.append(typeRef.tokens[t]);
							}
							javadocCompletion.append('#');
						}
					}
					javadocCompletion.append(method.selector);
					// Append parameters types
					javadocCompletion.append('(');
					if (method.parameters != null) {
						boolean isVarargs = method.isVarargs();
						for (int p=0, ln=method.parameters.length; p<ln; p++) {
							if (p>0) javadocCompletion.append(", "); //$NON-NLS-1$
							TypeBinding argTypeBinding = method.parameters[p];
							if (isVarargs && p == ln - 1)  {
								createVargsType(argTypeBinding.erasure(), scope, javadocCompletion);
							} else {
								createType(argTypeBinding.erasure(), scope,javadocCompletion);
							}
						}
					}
					javadocCompletion.append(')');
					completion = javadocCompletion.toString().toCharArray();
				}
			} else {
				// nothing to insert - do not want to replace the existing selector & arguments
				if (!exactMatch) {
					if (completionOnReferenceExpressionName)
						completion = method.selector;
					else if (this.source != null
						&& this.source.length > this.endPosition
						&& this.source[this.endPosition] == '(')
						completion = method.selector;
					else
						completion = CharOperation.concat(method.selector, new char[] { '(', ')' });

					if (castedReceiver != null) {
						completion = CharOperation.concat(castedReceiver, completion);
					}
				} else {
					if(prefixRequired && (this.source != null)) {
						completion = CharOperation.subarray(this.source, this.startPosition, this.endPosition);
					} else {
						this.startPosition = this.endPosition;
					}
					this.tokenStart = this.tokenEnd;
				}

				if(prefixRequired || this.options.forceImplicitQualification){
					char[] prefix = computePrefix(scope.enclosingSourceType(), invocationScope.enclosingSourceType(), method.isStatic());
					completion = CharOperation.concat(prefix,completion,'.');
				}
			}

			int relevance = computeBaseRelevance();
			relevance += computeRelevanceForResolution();
			relevance += computeRelevanceForInterestingProposal();
			relevance += computeRelevanceForCaseMatching(methodName, method.selector);
			int computeRelevanceForExpectingType = computeRelevanceForExpectingType(method.returnType);
			if(this.strictMatchForExtepectedType && computeRelevanceForExpectingType <= 0) {
				continue;
			} else if(!this.strictMatchForExtepectedType) {
				relevance += computeRelevanceForExpectingType;
			}
			relevance += computeRelevanceForEnumConstant(method.returnType);
			relevance += computeRelevanceForStatic(onlyStaticMethods, method.isStatic());
			relevance += computeRelevanceForQualification(prefixRequired);
			relevance += computeRelevanceForRestrictions(IAccessRule.K_ACCESSIBLE);
			if (onlyStaticMethods && this.insideQualifiedReference) {
				relevance += computeRelevanceForInheritance(receiverType, method.declaringClass);
			}
			if (missingElements != null) {
				relevance += computeRelevanceForMissingElements(missingElementsHaveProblems);
			}
			relevance += computeRelevanceForSuper(method, scope, invocationSite);
			this.noProposal = false;

			if (castedReceiver == null) {
				// Standard proposal
				if(!this.isIgnored(CompletionProposal.METHOD_REF, missingElements != null) && (this.assistNodeInJavadoc & CompletionOnJavadoc.ONLY_INLINE_TAG) == 0) {
					InternalCompletionProposal proposal =  createProposal(completionOnReferenceExpressionName ? CompletionProposal.METHOD_NAME_REFERENCE : CompletionProposal.METHOD_REF, this.actualCompletionPosition);
					proposal.setBinding(method);
					proposal.setDeclarationSignature(getSignature(method.declaringClass));
					proposal.setSignature(getSignature(method));
					MethodBinding original = method.original();
					if(original != method) {
						proposal.setOriginalSignature(getSignature(original));
					}
					proposal.setDeclarationPackageName(method.declaringClass.qualifiedPackageName());
					proposal.setDeclarationTypeName(method.declaringClass.qualifiedSourceName());
					proposal.setParameterPackageNames(parameterPackageNames);
					proposal.setParameterTypeNames(parameterTypeNames);
					proposal.setPackageName(method.returnType.qualifiedPackageName());
					proposal.setTypeName(method.returnType.qualifiedSourceName());
					proposal.setName(method.selector);
					if (missingElements != null) {
						CompletionProposal[] subProposals = new CompletionProposal[missingElements.length];
						for (int i = 0; i < missingElements.length; i++) {
							subProposals[i] =
								createRequiredTypeProposal(
										missingElements[i],
										missingElementsStarts[i],
										missingElementsEnds[i],
										relevance);
						}
						proposal.setRequiredProposals(subProposals);
					}
					proposal.setCompletion(completion);
					proposal.setFlags(method.modifiers);
					proposal.setReplaceRange(this.startPosition - this.offset, this.endPosition - this.offset);
					proposal.setTokenRange(this.tokenStart - this.offset, this.tokenEnd - this.offset);
					proposal.setRelevance(relevance);
					if(parameterNames != null) proposal.setParameterNames(parameterNames);
					this.requestor.accept(proposal);
					if(DEBUG) {
						this.printDebug(proposal);
					}
				}

				// Javadoc proposal
				if ((this.assistNodeInJavadoc & CompletionOnJavadoc.TEXT) != 0 && !this.requestor.isIgnored(CompletionProposal.JAVADOC_METHOD_REF)) {
					char[] javadocCompletion = inlineTagCompletion(completion, JavadocTagConstants.TAG_LINK);
					InternalCompletionProposal proposal =  createProposal(CompletionProposal.JAVADOC_METHOD_REF, this.actualCompletionPosition);
					proposal.setBinding(method);
					proposal.setDeclarationSignature(getSignature(method.declaringClass));
					proposal.setSignature(getSignature(method));
					MethodBinding original = method.original();
					if(original != method) {
						proposal.setOriginalSignature(getSignature(original));
					}
					proposal.setDeclarationPackageName(method.declaringClass.qualifiedPackageName());
					proposal.setDeclarationTypeName(method.declaringClass.qualifiedSourceName());
					proposal.setParameterPackageNames(parameterPackageNames);
					proposal.setParameterTypeNames(parameterTypeNames);
					proposal.setPackageName(method.returnType.qualifiedPackageName());
					proposal.setTypeName(method.returnType.qualifiedSourceName());
					proposal.setName(method.selector);
					proposal.setCompletion(javadocCompletion);
					proposal.setFlags(method.modifiers);
					int start = (this.assistNodeInJavadoc & CompletionOnJavadoc.REPLACE_TAG) != 0 ? this.javadocTagPosition : this.startPosition;
					proposal.setReplaceRange(start - this.offset, this.endPosition - this.offset);
					proposal.setTokenRange(this.tokenStart - this.offset, this.tokenEnd - this.offset);
					proposal.setRelevance(relevance+R_INLINE_TAG);
					if(parameterNames != null) proposal.setParameterNames(parameterNames);
					this.requestor.accept(proposal);
					if(DEBUG) {
						this.printDebug(proposal);
					}
				}
			} else {
				if(!this.isIgnored(CompletionProposal.METHOD_REF_WITH_CASTED_RECEIVER, missingElements != null)) {
					InternalCompletionProposal proposal =  createProposal(CompletionProposal.METHOD_REF_WITH_CASTED_RECEIVER, this.actualCompletionPosition);
					proposal.setBinding(method);
					proposal.setDeclarationSignature(getSignature(method.declaringClass));
					proposal.setSignature(getSignature(method));
					MethodBinding original = method.original();
					if(original != method) {
						proposal.setOriginalSignature(getSignature(original));
					}
					proposal.setReceiverSignature(getSignature(receiverType));
					proposal.setDeclarationPackageName(method.declaringClass.qualifiedPackageName());
					proposal.setDeclarationTypeName(method.declaringClass.qualifiedSourceName());
					proposal.setParameterPackageNames(parameterPackageNames);
					proposal.setParameterTypeNames(parameterTypeNames);
					proposal.setPackageName(method.returnType.qualifiedPackageName());
					proposal.setTypeName(method.returnType.qualifiedSourceName());
					proposal.setName(method.selector);
					if (missingElements != null) {
						CompletionProposal[] subProposals = new CompletionProposal[missingElements.length];
						for (int i = 0; i < missingElements.length; i++) {
							subProposals[i] =
								createRequiredTypeProposal(
										missingElements[i],
										missingElementsStarts[i],
										missingElementsEnds[i],
										relevance);
						}
						proposal.setRequiredProposals(subProposals);
					}
					proposal.setCompletion(completion);
					proposal.setFlags(method.modifiers);
					proposal.setReplaceRange(this.startPosition - this.offset, this.endPosition - this.offset);
					proposal.setReceiverRange(receiverStart - this.offset, receiverEnd - this.offset);
					proposal.setTokenRange(this.tokenStart - this.offset, this.tokenEnd - this.offset);
					proposal.setRelevance(relevance);
					if(parameterNames != null) proposal.setParameterNames(parameterNames);
					this.requestor.accept(proposal);
					if(DEBUG) {
						this.printDebug(proposal);
					}
				}
			}
			this.startPosition = previousStartPosition;
			this.tokenStart = previousTokenStart;
		}

		methodsFound.addAll(newMethodsFound);
	}
	private void findLocalMethodsFromFavorites(
			char[] methodName,
			MethodBinding[] methods,
			Scope scope,
			ObjectVector methodsFound,
			ObjectVector methodsFoundFromFavorites,
			ReferenceBinding receiverType,
			InvocationSite invocationSite,
			Scope invocationScope) {

			char[] typeName = CharOperation.concatWith(receiverType.compoundName, '.');

			int methodLength = methodName.length;

			next : for (int f = methods.length; --f >= 0;) {
				MethodBinding method = methods[f];

				if (method.isSynthetic()) continue next;

				if (method.isDefaultAbstract())	continue next;

				if (method.isConstructor()) continue next;

				if (this.options.checkDeprecation &&
						method.isViewedAsDeprecated() &&
						!scope.isDefinedInSameUnit(method.declaringClass))
					continue next;

				if (!method.isStatic()) continue next;

				if (this.options.checkVisibility
					&& !method.canBeSeenBy(receiverType, invocationSite, scope)) continue next;

				if (methodLength > method.selector.length) continue next;

				if (isFailedMatch(methodName, method.selector)) {
					continue next;
				}

				for (int i = methodsFoundFromFavorites.size; --i >= 0;) {
					Object[] other = (Object[]) methodsFoundFromFavorites.elementAt(i);
					MethodBinding otherMethod = (MethodBinding) other[0];

					if (method == otherMethod) continue next;

					if (CharOperation.equals(method.selector, otherMethod.selector, true)) {
						if (TypeBinding.equalsEquals(otherMethod.declaringClass, method.declaringClass) &&
								this.lookupEnvironment.methodVerifier().isMethodSubsignature(otherMethod, method)) {
							continue next;
						}
					}
				}

				for (int i = methodsFound.size; --i >= 0;) {
					Object[] other = (Object[]) methodsFound.elementAt(i);
					MethodBinding otherMethod = (MethodBinding) other[0];

					if (method == otherMethod) continue next;

					if (CharOperation.equals(method.selector, otherMethod.selector, true)) {
						if (this.lookupEnvironment.methodVerifier().isMethodSubsignature(otherMethod, method)) {
							continue next;
						}
					}
				}

				boolean proposeStaticImport = !(this.compilerOptions.complianceLevel < ClassFileConstants.JDK1_5) &&
					this.options.suggestStaticImport;

				boolean isAlreadyImported = false;
				if (!proposeStaticImport) {
					if(!this.importCachesInitialized) {
						initializeImportCaches();
					}
					for (int j = 0; j < this.importCacheCount; j++) {
						char[][] importName = this.importsCache[j];
						if(CharOperation.equals(receiverType.sourceName, importName[0])) {
							if (!CharOperation.equals(typeName, importName[1])) {
								continue next;
							} else {
								isAlreadyImported = true;
							}
						}
					}
				}

				methodsFoundFromFavorites.add(new Object[]{method, receiverType});

				ReferenceBinding superTypeWithSameErasure = (ReferenceBinding)receiverType.findSuperTypeOriginatingFrom(method.declaringClass);
				if (TypeBinding.notEquals(method.declaringClass, superTypeWithSameErasure)) {
					MethodBinding[] otherMethods = superTypeWithSameErasure.getMethods(method.selector);
					for (int i = 0; i < otherMethods.length; i++) {
						if(otherMethods[i].original() == method.original()) {
							method = otherMethods[i];
						}
					}
				}

				int length = method.parameters.length;
				char[][] parameterPackageNames = new char[length][];
				char[][] parameterTypeNames = new char[length][];

				for (int i = 0; i < length; i++) {
					TypeBinding type = method.original().parameters[i];
					parameterPackageNames[i] = type.qualifiedPackageName();
					parameterTypeNames[i] = type.qualifiedSourceName();
				}
				char[][] parameterNames = findMethodParameterNames(method,parameterTypeNames);

				char[] completion = CharOperation.NO_CHAR;

				int previousStartPosition = this.startPosition;
				int previousTokenStart = this.tokenStart;

				if (this.source != null
					&& this.source.length > this.endPosition
					&& this.source[this.endPosition] == '(') {
					completion = method.selector;
				} else {
					completion = CharOperation.concat(method.selector, new char[] { '(', ')' });
				}

				int relevance = computeBaseRelevance();
				relevance += computeRelevanceForResolution();
				relevance += computeRelevanceForInterestingProposal();
				relevance += computeRelevanceForCaseMatching(methodName, method.selector);
				int computeRelevanceForExpectingType = computeRelevanceForExpectingType(method.returnType);
				if(this.strictMatchForExtepectedType && computeRelevanceForExpectingType <= 0) {
					continue;
				} else if(!this.strictMatchForExtepectedType) {
					relevance += computeRelevanceForExpectingType;
				}

				relevance += computeRelevanceForEnumConstant(method.returnType);
				relevance += computeRelevanceForStatic(true, method.isStatic());
				relevance += computeRelevanceForQualification(true);
				relevance += computeRelevanceForRestrictions(IAccessRule.K_ACCESSIBLE);

				CompilationUnitDeclaration cu = this.unitScope.referenceContext;
				int importStart = cu.types[0].declarationSourceStart;
				int importEnd = importStart;

				this.noProposal = false;

				if (!proposeStaticImport) {
					if (isAlreadyImported) {
						if (!isIgnored(CompletionProposal.METHOD_REF)) {
							completion = CharOperation.concat(receiverType.sourceName, completion, '.');

							InternalCompletionProposal proposal =  createProposal(CompletionProposal.METHOD_REF, this.actualCompletionPosition);
							proposal.setBinding(method);
							proposal.setDeclarationSignature(getSignature(method.declaringClass));
							proposal.setSignature(getSignature(method));
							MethodBinding original = method.original();
							if(original != method) {
								proposal.setOriginalSignature(getSignature(original));
							}
							proposal.setDeclarationPackageName(method.declaringClass.qualifiedPackageName());
							proposal.setDeclarationTypeName(method.declaringClass.qualifiedSourceName());
							proposal.setParameterPackageNames(parameterPackageNames);
							proposal.setParameterTypeNames(parameterTypeNames);
							proposal.setPackageName(method.returnType.qualifiedPackageName());
							proposal.setTypeName(method.returnType.qualifiedSourceName());
							proposal.setName(method.selector);
							proposal.setCompletion(completion);
							proposal.setFlags(method.modifiers);
							proposal.setReplaceRange(this.startPosition - this.offset, this.endPosition - this.offset);
							proposal.setTokenRange(this.tokenStart - this.offset, this.tokenEnd - this.offset);
							proposal.setRelevance(relevance);
							if(parameterNames != null) proposal.setParameterNames(parameterNames);

							this.requestor.accept(proposal);
							if(DEBUG) {
								this.printDebug(proposal);
							}
						}
					} else if (!this.isIgnored(CompletionProposal.METHOD_REF, CompletionProposal.TYPE_IMPORT)) {
						completion = CharOperation.concat(receiverType.sourceName, completion, '.');

						InternalCompletionProposal proposal =  createProposal(CompletionProposal.METHOD_REF, this.actualCompletionPosition);
						proposal.setBinding(method);
						proposal.setDeclarationSignature(getSignature(method.declaringClass));
						proposal.setSignature(getSignature(method));
						MethodBinding original = method.original();
						if(original != method) {
							proposal.setOriginalSignature(getSignature(original));
						}
						proposal.setDeclarationPackageName(method.declaringClass.qualifiedPackageName());
						proposal.setDeclarationTypeName(method.declaringClass.qualifiedSourceName());
						proposal.setParameterPackageNames(parameterPackageNames);
						proposal.setParameterTypeNames(parameterTypeNames);
						proposal.setPackageName(method.returnType.qualifiedPackageName());
						proposal.setTypeName(method.returnType.qualifiedSourceName());
						proposal.setName(method.selector);
						proposal.setCompletion(completion);
						proposal.setFlags(method.modifiers);
						proposal.setReplaceRange(this.startPosition - this.offset, this.endPosition - this.offset);
						proposal.setTokenRange(this.tokenStart - this.offset, this.tokenEnd - this.offset);
						proposal.setRelevance(relevance);
						if(parameterNames != null) proposal.setParameterNames(parameterNames);

						char[] typeImportCompletion = createImportCharArray(typeName, false, false);

						InternalCompletionProposal typeImportProposal = createProposal(CompletionProposal.TYPE_IMPORT, this.actualCompletionPosition);
						typeImportProposal.nameLookup = this.nameEnvironment.nameLookup;
						typeImportProposal.completionEngine = this;
						char[] packageName = receiverType.qualifiedPackageName();
						typeImportProposal.setDeclarationSignature(packageName);
						typeImportProposal.setSignature(getSignature(receiverType));
						typeImportProposal.setPackageName(packageName);
						typeImportProposal.setTypeName(receiverType.qualifiedSourceName());
						typeImportProposal.setCompletion(typeImportCompletion);
						typeImportProposal.setFlags(receiverType.modifiers);
						typeImportProposal.setAdditionalFlags(CompletionFlags.Default);
						typeImportProposal.setReplaceRange(importStart - this.offset, importEnd - this.offset);
						typeImportProposal.setTokenRange(importStart - this.offset, importEnd - this.offset);
						typeImportProposal.setRelevance(relevance);

						proposal.setRequiredProposals(new CompletionProposal[]{typeImportProposal});

						this.requestor.accept(proposal);
						if(DEBUG) {
							this.printDebug(proposal);
						}
					}
				} else {
					if (!this.isIgnored(CompletionProposal.METHOD_REF, CompletionProposal.METHOD_IMPORT)) {
						InternalCompletionProposal proposal =  createProposal(CompletionProposal.METHOD_REF, this.actualCompletionPosition);
						proposal.setBinding(method);
						proposal.setDeclarationSignature(getSignature(method.declaringClass));
						proposal.setSignature(getSignature(method));
						MethodBinding original = method.original();
						if(original != method) {
							proposal.setOriginalSignature(getSignature(original));
						}
						proposal.setDeclarationPackageName(method.declaringClass.qualifiedPackageName());
						proposal.setDeclarationTypeName(method.declaringClass.qualifiedSourceName());
						proposal.setParameterPackageNames(parameterPackageNames);
						proposal.setParameterTypeNames(parameterTypeNames);
						proposal.setPackageName(method.returnType.qualifiedPackageName());
						proposal.setTypeName(method.returnType.qualifiedSourceName());
						proposal.setName(method.selector);
						proposal.setCompletion(completion);
						proposal.setFlags(method.modifiers);
						proposal.setReplaceRange(this.startPosition - this.offset, this.endPosition - this.offset);
						proposal.setTokenRange(this.tokenStart - this.offset, this.tokenEnd - this.offset);
						proposal.setRelevance(relevance);
						if(parameterNames != null) proposal.setParameterNames(parameterNames);

						char[] methodImportCompletion = createImportCharArray(CharOperation.concat(typeName, method.selector, '.'), true, false);

						InternalCompletionProposal methodImportProposal = createProposal(CompletionProposal.METHOD_IMPORT, this.actualCompletionPosition);
						methodImportProposal.setDeclarationSignature(getSignature(method.declaringClass));
						methodImportProposal.setSignature(getSignature(method));
						if(original != method) {
							proposal.setOriginalSignature(getSignature(original));
						}
						methodImportProposal.setDeclarationPackageName(method.declaringClass.qualifiedPackageName());
						methodImportProposal.setDeclarationTypeName(method.declaringClass.qualifiedSourceName());
						methodImportProposal.setParameterPackageNames(parameterPackageNames);
						methodImportProposal.setParameterTypeNames(parameterTypeNames);
						methodImportProposal.setPackageName(method.returnType.qualifiedPackageName());
						methodImportProposal.setTypeName(method.returnType.qualifiedSourceName());
						methodImportProposal.setName(method.selector);
						methodImportProposal.setCompletion(methodImportCompletion);
						methodImportProposal.setFlags(method.modifiers);
						methodImportProposal.setAdditionalFlags(CompletionFlags.StaticImport);
						methodImportProposal.setReplaceRange(importStart - this.offset, importEnd - this.offset);
						methodImportProposal.setTokenRange(importStart - this.offset, importEnd - this.offset);
						methodImportProposal.setRelevance(relevance);
						if(parameterNames != null) methodImportProposal.setParameterNames(parameterNames);

						proposal.setRequiredProposals(new CompletionProposal[]{methodImportProposal});

						this.requestor.accept(proposal);
						if(DEBUG) {
							this.printDebug(proposal);
						}
					}
				}

				this.startPosition = previousStartPosition;
				this.tokenStart = previousTokenStart;
			}
		}

	/**
	 * Helper method for findMethods(char[], TypeBinding[], ReferenceBinding, Scope, ObjectVector, boolean, boolean, boolean)
	 * Note that the method doesn't do a comparison of the method names and expects the client to handle the same.
	 *
	 * @methodName method as entered by the user, the one to completed
	 * @param methods a resultant array of MethodBinding, whose names should match methodName. The calling client must ensure that this check is handled.
	 */
	private void findLocalMethodsFromStaticImports(
		char[] methodName,
		MethodBinding[] methods,
		Scope scope,
		boolean exactMatch,
		ObjectVector methodsFound,
		ReferenceBinding receiverType,
		InvocationSite invocationSite) {

		ObjectVector newMethodsFound =  new ObjectVector();

		next : for (int f = methods.length; --f >= 0;) {
			MethodBinding method = methods[f];

			if (method.isSynthetic()) continue next;

			if (method.isDefaultAbstract())	continue next;

			if (method.isConstructor()) continue next;

			if (!method.isStatic()) continue next;

			if (this.options.checkDeprecation &&
					method.isViewedAsDeprecated() &&
					!scope.isDefinedInSameUnit(method.declaringClass))
				continue next;

			if (this.options.checkVisibility
				&& !method.canBeSeenBy(receiverType, invocationSite, scope)) continue next;

			for (int i = methodsFound.size; --i >= 0;) {
				Object[] other = (Object[]) methodsFound.elementAt(i);
				MethodBinding otherMethod = (MethodBinding) other[0];
				ReferenceBinding otherReceiverType = (ReferenceBinding) other[1];
				if (method == otherMethod && TypeBinding.equalsEquals(receiverType, otherReceiverType))
					continue next;

				if (CharOperation.equals(method.selector, otherMethod.selector, true)) {
					if (this.lookupEnvironment.methodVerifier().isMethodSubsignature(otherMethod, method)) {
						continue next;
					}
				}
			}

			newMethodsFound.add(new Object[]{method, receiverType});

			int length = method.parameters.length;
			char[][] parameterPackageNames = new char[length][];
			char[][] parameterTypeNames = new char[length][];

			for (int i = 0; i < length; i++) {
				TypeBinding type = method.original().parameters[i];
				parameterPackageNames[i] = type.qualifiedPackageName();
				parameterTypeNames[i] = type.qualifiedSourceName();
			}
			char[][] parameterNames = findMethodParameterNames(method,parameterTypeNames);

			char[] completion = CharOperation.NO_CHAR;

			int previousStartPosition = this.startPosition;
			int previousTokenStart = this.tokenStart;

			if (!exactMatch) {
				if (this.source != null
					&& this.source.length > this.endPosition
					&& this.source[this.endPosition] == '(') {
					completion = method.selector;
				} else {
					completion = CharOperation.concat(method.selector, new char[] { '(', ')' });
				}
			} else {
				this.startPosition = this.endPosition;
				this.tokenStart = this.tokenEnd;
			}

			int relevance = computeBaseRelevance();
			relevance += computeRelevanceForResolution();
			relevance += computeRelevanceForInterestingProposal();
			relevance += computeRelevanceForCaseMatching(methodName, method.selector);
			int computeRelevanceForExpectingType = computeRelevanceForExpectingType(method.returnType);
			if(this.strictMatchForExtepectedType && computeRelevanceForExpectingType <= 0) {
				continue;
			} else if(!this.strictMatchForExtepectedType) {
				relevance += computeRelevanceForExpectingType;
			}

			int computeRelevanceForEnumConstant = computeRelevanceForEnumConstant(method.returnType);
			if(this.strictMatchForExtepectedType && computeRelevanceForEnumConstant <= 0) {
				continue;
			} else if(!this.strictMatchForExtepectedType) {
				relevance += computeRelevanceForEnumConstant;
			}

			relevance += computeRelevanceForStatic(true, method.isStatic());
			relevance += computeRelevanceForQualification(false);
			relevance += computeRelevanceForRestrictions(IAccessRule.K_ACCESSIBLE);

			this.noProposal = false;
			if(!this.requestor.isIgnored(CompletionProposal.METHOD_REF)) {
				InternalCompletionProposal proposal =  createProposal(CompletionProposal.METHOD_REF, this.actualCompletionPosition);
				proposal.setBinding(method);
				proposal.setDeclarationSignature(getSignature(method.declaringClass));
				proposal.setSignature(getSignature(method));
				MethodBinding original = method.original();
				if(original != method) {
					proposal.setOriginalSignature(getSignature(original));
				}
				proposal.setDeclarationPackageName(method.declaringClass.qualifiedPackageName());
				proposal.setDeclarationTypeName(method.declaringClass.qualifiedSourceName());
				proposal.setParameterPackageNames(parameterPackageNames);
				proposal.setParameterTypeNames(parameterTypeNames);
				proposal.setPackageName(method.returnType.qualifiedPackageName());
				proposal.setTypeName(method.returnType.qualifiedSourceName());
				proposal.setName(method.selector);
				proposal.setCompletion(completion);
				proposal.setFlags(method.modifiers);
				proposal.setReplaceRange(this.startPosition - this.offset, this.endPosition - this.offset);
				proposal.setTokenRange(this.tokenStart - this.offset, this.tokenEnd - this.offset);
				proposal.setRelevance(relevance);
				if(parameterNames != null) proposal.setParameterNames(parameterNames);
				this.requestor.accept(proposal);
				if(DEBUG) {
					this.printDebug(proposal);
				}
			}
			this.startPosition = previousStartPosition;
			this.tokenStart = previousTokenStart;
		}

		methodsFound.addAll(newMethodsFound);
	}

	private void findLocalMethodsFromStaticImports(
			char[] token,
			Scope scope,
			InvocationSite invocationSite,
			Scope invocationScope,
			boolean exactMatch,
			ObjectVector methodsFound,
			boolean proposeMethod) {
		findFieldsAndMethodsFromStaticImports(
				token,
				scope,
				invocationSite,
				invocationScope,
				exactMatch,
				false,
				new ObjectVector(),
				new ObjectVector(),
				methodsFound,
				false,
				proposeMethod);
	}
	protected void findMembers(
			char[] token,
			ReferenceBinding receiverType,
			Scope scope,
			InvocationSite invocationSite,
			boolean isInsideAnnotationAttribute,
			Binding[] missingElements,
			int[] missingElementsStarts,
			int[] missingElementsEnds,
			boolean missingElementsHaveProblems) {

		if (!this.requestor.isIgnored(CompletionProposal.TYPE_REF)) {
			findMemberTypes(
					token,
					receiverType,
					scope,
					scope.enclosingSourceType(),
					false,
					true,
					new ObjectVector(),
					missingElements,
					missingElementsStarts,
					missingElementsEnds,
					missingElementsHaveProblems);
		}
		if (!this.requestor.isIgnored(CompletionProposal.FIELD_REF)) {
			findClassField(
					token,
					receiverType,
					scope,
					missingElements,
					missingElementsStarts,
					missingElementsEnds,
					missingElementsHaveProblems);
		}

		MethodScope methodScope = null;
		if (!isInsideAnnotationAttribute &&
				!this.requestor.isIgnored(CompletionProposal.KEYWORD) &&
				((scope instanceof MethodScope && !((MethodScope)scope).isStatic)
				|| ((methodScope = scope.enclosingMethodScope()) != null && !methodScope.isStatic))) {
			if (token.length >= 0) {
				boolean isInterface = false;
				if (receiverType != null) {
					isInterface = receiverType.isInterface();
				}
				if (!isInterface) {
					if (hasCompatibleEnclosing(scope, receiverType)) {
						findKeywords(token, new char[][] { Keywords.THIS, Keywords.SUPER }, true, false);
					}
				} else {
					boolean isEqual = false;
					char[] enclosingSourceName = null;
					if(scope.enclosingSourceType() != null)
						enclosingSourceName = scope.enclosingSourceType().sourceName;
					char[] receiverSourceName = null;
					if (receiverType != null) {
						receiverSourceName = receiverType.sourceName;
					}
					if( enclosingSourceName !=null & receiverSourceName !=null)
						isEqual = Arrays.equals(enclosingSourceName, receiverSourceName);
					if(isEqual) {
						findKeywords(token, new char[][] { Keywords.THIS }, true, false);
					} else {
						// Check if the enclosing source implements this interface then show super
						if (scope.enclosingSourceType() != null) {
							SourceTypeBinding src = scope.enclosingSourceType();
							ReferenceBinding[] superInterfaces = src.superInterfaces();
							boolean implemented = false;
							for (ReferenceBinding referenceBinding : superInterfaces) {
								if (Arrays.equals(referenceBinding.sourceName, receiverSourceName)) {
									implemented = true;
									break;
								}
							}
							if (implemented) {
								findKeywords(token, new char[][] { Keywords.SUPER }, true, false);
							}
						}
					}
				}
			}
		}

		if (!this.requestor.isIgnored(CompletionProposal.FIELD_REF)) {
			findFields(
				token,
				receiverType,
				scope,
				new ObjectVector(),
				new ObjectVector(),
				true,
				invocationSite,
				scope,
				false,
				false,
				missingElements,
				missingElementsStarts,
				missingElementsEnds,
				missingElementsHaveProblems,
				null,
				-1,
				-1);
		}

		if (!isInsideAnnotationAttribute && !this.requestor.isIgnored(CompletionProposal.METHOD_REF)) {
			findMethods(
				token,
				null,
				null,
				receiverType,
				scope,
				new ObjectVector(),
				true,
				false,
				invocationSite,
				scope,
				false,
				false,
				false,
				missingElements,
				missingElementsStarts,
				missingElementsEnds,
				missingElementsHaveProblems,
				null,
				-1,
				-1);
		}
	}

	private boolean hasCompatibleEnclosing(Scope scope, ReferenceBinding receiverType) {
		ReferenceBinding enclosing = scope.enclosingSourceType();
		while (enclosing != null) {
			if (enclosing.isCompatibleWith(receiverType, scope))
				return true;
			enclosing = enclosing.enclosingType();
		}
		return false;
	}

	private void findMembersFromMissingType(
			final char[] token,
			final long pos,
			TypeBinding resolveType,
			final Scope scope,
			final InvocationSite invocationSite,
			final boolean isInsideAnnotationAttribute) {
		MissingTypesGuesser missingTypesConverter = new MissingTypesGuesser(this);
		MissingTypesGuesser.GuessedTypeRequestor substitutionRequestor =
			new MissingTypesGuesser.GuessedTypeRequestor() {
				@Override
				public void accept(
						TypeBinding guessedType,
						Binding[] missingElements,
						int[] missingElementsStarts,
						int[] missingElementsEnds,
						boolean hasProblems) {
					if (guessedType instanceof ReferenceBinding) {
						findMembers(
								CompletionEngine.this.completionToken,
								(ReferenceBinding)guessedType,
								scope,
								invocationSite,
								isInsideAnnotationAttribute,
								missingElements,
								missingElementsStarts,
								missingElementsEnds,
								hasProblems);
					}
				}
			};
		SingleTypeReference typeRef = new SingleTypeReference(token, pos);
		typeRef.resolvedType = new ProblemReferenceBinding(new char[][]{ token }, null, ProblemReasons.NotFound);
		missingTypesConverter.guess(typeRef, scope, substitutionRequestor);
	}

	private void findMemberTypes(
		char[] typeName,
		ReferenceBinding receiverType,
		Scope scope,
		SourceTypeBinding typeInvocation,
		boolean staticOnly,
		boolean staticFieldsAndMethodOnly,
		boolean fromStaticImport,
		boolean checkQualification,
		boolean proposeAllMemberTypes,
		SourceTypeBinding typeToIgnore,
		ObjectVector typesFound,
		Binding[] missingElements,
		int[] missingElementsStarts,
		int[] missingElementsEnds,
		boolean missingElementsHaveProblems) {

		ReferenceBinding currentType = receiverType;
		if (typeName == null)
			return;

		if (this.insideQualifiedReference
			|| typeName.length == 0) { // do not search up the hierarchy

			findMemberTypes(
				typeName,
				currentType.memberTypes(),
				typesFound,
				receiverType,
				typeInvocation,
				staticOnly,
				staticFieldsAndMethodOnly,
				fromStaticImport,
				checkQualification,
				scope,
				missingElements,
				missingElementsStarts,
				missingElementsEnds,
				missingElementsHaveProblems);
			return;
		}

		ReferenceBinding[] interfacesToVisit = null;
		int nextPosition = 0;

		do {
			ReferenceBinding[] itsInterfaces = currentType.superInterfaces();
			if (itsInterfaces != null && itsInterfaces != Binding.NO_SUPERINTERFACES) {
				if (interfacesToVisit == null) {
					interfacesToVisit = itsInterfaces;
					nextPosition = interfacesToVisit.length;
				} else {
					int itsLength = itsInterfaces.length;
					if (nextPosition + itsLength >= interfacesToVisit.length)
						System.arraycopy(interfacesToVisit, 0, interfacesToVisit = new ReferenceBinding[nextPosition + itsLength + 5], 0, nextPosition);
					nextInterface : for (int a = 0; a < itsLength; a++) {
						ReferenceBinding next = itsInterfaces[a];
						for (int b = 0; b < nextPosition; b++)
							if (TypeBinding.equalsEquals(next, interfacesToVisit[b])) continue nextInterface;
						interfacesToVisit[nextPosition++] = next;
					}
				}
			}

			findMemberTypes(
				typeName,
				currentType.memberTypes(),
				typesFound,
				receiverType,
				typeInvocation,
				staticOnly,
				staticFieldsAndMethodOnly,
				fromStaticImport,
				checkQualification,
				scope,
				missingElements,
				missingElementsStarts,
				missingElementsEnds,
				missingElementsHaveProblems);

			currentType = currentType.superclass();
		} while (currentType != null);

		if(proposeAllMemberTypes) {
			ReferenceBinding[] memberTypes = receiverType.memberTypes();
			for (int i = 0; i < memberTypes.length; i++) {
				if(TypeBinding.notEquals(memberTypes[i], typeToIgnore)) {
					findSubMemberTypes(
						typeName,
						memberTypes[i],
						scope,
						typeInvocation,
						staticOnly,
						staticFieldsAndMethodOnly,
						fromStaticImport,
						typesFound);
				}
			}
		}

		if (interfacesToVisit != null) {
			for (int i = 0; i < nextPosition; i++) {
				ReferenceBinding anInterface = interfacesToVisit[i];
				findMemberTypes(
					typeName,
					anInterface.memberTypes(),
					typesFound,
					receiverType,
					typeInvocation,
					staticOnly,
					staticFieldsAndMethodOnly,
					fromStaticImport,
					checkQualification,
					scope,
					missingElements,
					missingElementsStarts,
					missingElementsEnds,
					missingElementsHaveProblems);

				ReferenceBinding[] itsInterfaces = anInterface.superInterfaces();
				if (itsInterfaces != null && itsInterfaces != Binding.NO_SUPERINTERFACES) {
					int itsLength = itsInterfaces.length;
					if (nextPosition + itsLength >= interfacesToVisit.length)
						System.arraycopy(interfacesToVisit, 0, interfacesToVisit = new ReferenceBinding[nextPosition + itsLength + 5], 0, nextPosition);
					nextInterface : for (int a = 0; a < itsLength; a++) {
						ReferenceBinding next = itsInterfaces[a];
						for (int b = 0; b < nextPosition; b++)
							if (TypeBinding.equalsEquals(next, interfacesToVisit[b])) continue nextInterface;
						interfacesToVisit[nextPosition++] = next;
					}
				}
			}
		}
	}

	protected void findMemberTypes(
		char[] typeName,
		ReferenceBinding receiverType,
		Scope scope,
		SourceTypeBinding typeInvocation,
		boolean staticOnly,
		boolean staticFieldsAndMethodOnly,
		ObjectVector typesFound,
		Binding[] missingElements,
		int[] missingElementsStarts,
		int[] missingElementsEnds,
		boolean missingElementsHaveProblems)  {
		findMemberTypes(
				typeName,
				receiverType,
				scope,
				typeInvocation,
				staticOnly,
				staticFieldsAndMethodOnly,
				false,
				false,
				false,
				null,
				typesFound,
				missingElements,
				missingElementsStarts,
				missingElementsEnds,
				missingElementsHaveProblems);
	}
		// Helper method for findMemberTypes(char[], ReferenceBinding, Scope)
	private void findMemberTypes(
		char[] typeName,
		ReferenceBinding[] memberTypes,
		ObjectVector typesFound,
		ReferenceBinding receiverType,
		SourceTypeBinding invocationType,
		boolean staticOnly,
		boolean staticFieldsAndMethodOnly,
		boolean fromStaticImport,
		boolean checkQualification,
		Scope scope,
		Binding[] missingElements,
		int[] missingElementsStarts,
		int[] missingElementsEnds,
		boolean missingElementsHaveProblems) {

		// Inherited member types which are hidden by subclasses are filtered out
		// No visibility checks can be performed without the scope & invocationSite
		int typeLength = typeName.length;
		next : for (int m = memberTypes.length; --m >= 0;) {
			ReferenceBinding memberType = memberTypes[m];
			//		if (!wantClasses && memberType.isClass()) continue next;
			//		if (!wantInterfaces && memberType.isInterface()) continue next;

			if (staticOnly && !memberType.isStatic()) continue next;

			if (isForbidden(memberType)) continue next;

			if (typeLength > memberType.sourceName.length)
				continue next;

			if (isFailedMatch(typeName, memberType.sourceName))
				continue next;

			if (this.options.checkDeprecation &&
					memberType.isViewedAsDeprecated() &&
					!scope.isDefinedInSameUnit(memberType))
				continue next;

			if (this.options.checkVisibility) {
				if (invocationType != null && !memberType.canBeSeenBy(receiverType, invocationType)) {
					continue next;
				} else if(invocationType == null && !memberType.canBeSeenBy(this.unitScope.fPackage)) {
					continue next;
				}
			}

			if (this.insideQualifiedReference &&
					receiverType.isParameterizedType() &&
					memberType.isStatic()) {
				continue next;
			}

			for (int i = typesFound.size; --i >= 0;) {
				ReferenceBinding otherType = (ReferenceBinding) typesFound.elementAt(i);

				if (TypeBinding.equalsEquals(memberType, otherType))
					continue next;

				if (CharOperation.equals(memberType.sourceName, otherType.sourceName, true)
						&& otherType.isNestedType()) {

					if (memberType.enclosingType().isSuperclassOf(otherType.enclosingType()))
						continue next;

					if (otherType.enclosingType().isInterface())
						if (memberType.enclosingType()
							.implementsInterface(otherType.enclosingType(), true))
							continue next;

					if (memberType.enclosingType().isInterface())
						if (otherType.enclosingType()
							.implementsInterface(memberType.enclosingType(), true))
							continue next;
				}
			}

			typesFound.add(memberType);

			if (this.assistNodeIsExtendedType && memberType.isFinal()) continue next;
			if (this.assistNodeIsInterfaceExcludingAnnotation && memberType.isAnnotationType()) continue next;
			if(!this.insideQualifiedReference) {
				if(this.assistNodeIsClass || this.assistNodeIsException) {
					if(!memberType.isClass()) continue next;
				} else if(this.assistNodeIsInterface) {
					if(!memberType.isInterface() && !memberType.isAnnotationType()) continue next;
				} else if (this.assistNodeIsAnnotation) {
					if(!memberType.isAnnotationType()) continue next;
				}
			}

			char[] completionName = memberType.sourceName();

			boolean isQualified = false;
			if(checkQualification && !fromStaticImport) {
				char[] memberPackageName = memberType.qualifiedPackageName();
				char[] memberTypeName = memberType.sourceName();
				char[] memberEnclosingTypeNames = memberType.enclosingType().qualifiedSourceName();
				if (mustQualifyType(memberPackageName, memberTypeName, memberEnclosingTypeNames, memberType.modifiers)) {
					if (memberPackageName == null || memberPackageName.length == 0)
						if (this.unitScope != null && this.unitScope.fPackage.compoundName != CharOperation.NO_CHAR_CHAR)
							break next; // ignore types from the default package from outside it
					isQualified = true;
					completionName =
						CharOperation.concat(
								memberPackageName,
								CharOperation.concat(
										memberEnclosingTypeNames,
										memberTypeName,
										'.'),
								'.');
				}
			}

			int relevance = computeBaseRelevance();
			relevance += computeRelevanceForResolution();
			relevance += computeRelevanceForInterestingProposal(memberType);
			relevance += computeRelevanceForCaseMatching(typeName, memberType.sourceName);
			relevance += computeRelevanceForExpectingType(memberType);
			relevance += computeRelevanceForRestrictions(IAccessRule.K_ACCESSIBLE);
			if(!this.insideQualifiedReference) {
				relevance += computeRelevanceForQualification(isQualified);
			}
			if (staticFieldsAndMethodOnly && this.insideQualifiedReference) relevance += R_NON_INHERITED; // This criterion doesn't concern types and is added to be balanced with field and method relevance.

			if (memberType.isAnnotationType()) {
				relevance += computeRelevanceForAnnotation();
				relevance += computeRelevanceForAnnotationTarget(memberType);
			} else if (memberType.isClass()) {
				relevance += computeRelevanceForClass();
				relevance += computeRelevanceForException(memberType.sourceName);
			} else if(memberType.isEnum()) {
				relevance += computeRelevanceForEnum();
			} else if(memberType.isInterface()) {
				relevance += computeRelevanceForInterface();
			}

			if (missingElements != null) {
				relevance += computeRelevanceForMissingElements(missingElementsHaveProblems);
			}

			boolean allowingLongComputationProposals = isAllowingLongComputationProposals();

			this.noProposal = false;
			if (!this.assistNodeIsConstructor ||
					!allowingLongComputationProposals ||
					hasStaticMemberTypes(memberType, invocationType, this.unitScope) ||
					(memberType instanceof SourceTypeBinding && hasMemberTypesInEnclosingScope((SourceTypeBinding)memberType, scope)) ||
					hasArrayTypeAsExpectedSuperTypes()) {
				createTypeProposal(
						memberType,
						memberType.qualifiedSourceName(),
						IAccessRule.K_ACCESSIBLE,
						completionName,
						relevance,
						missingElements,
						missingElementsStarts,
						missingElementsEnds,
						missingElementsHaveProblems);
			}

			if (this.assistNodeIsConstructor && allowingLongComputationProposals) {
				findConstructorsOrAnonymousTypes(
						memberType,
						scope,
						FakeInvocationSite,
						isQualified,
						relevance);
			}
		}
	}
	private void findMemberTypesFromMissingType(
			char[] typeName,
			final long pos,
			final Scope scope)  {
		MissingTypesGuesser missingTypesConverter = new MissingTypesGuesser(this);
		MissingTypesGuesser.GuessedTypeRequestor substitutionRequestor =
			new MissingTypesGuesser.GuessedTypeRequestor() {
				@Override
				public void accept(
						TypeBinding guessedType,
						Binding[] missingElements,
						int[] missingElementsStarts,
						int[] missingElementsEnds,
						boolean hasProblems) {
					if (guessedType instanceof ReferenceBinding) {
						findMemberTypes(
								CompletionEngine.this.completionToken,
								(ReferenceBinding)guessedType,
								scope,
								scope.enclosingSourceType(),
								false,
								false,
								new ObjectVector(),
								missingElements,
								missingElementsStarts,
								missingElementsEnds,
								hasProblems);
					}
				}
			};
		SingleTypeReference typeRef = new SingleTypeReference(typeName, pos);
		typeRef.resolvedType = new ProblemReferenceBinding(new char[][]{ typeName }, null, ProblemReasons.NotFound);
		missingTypesConverter.guess(typeRef, scope, substitutionRequestor);
	}

	private void findMemberTypesFromMissingType(
			TypeReference typeRef,
			final long pos,
			final Scope scope)  {
		MissingTypesGuesser missingTypesConverter = new MissingTypesGuesser(this);
		MissingTypesGuesser.GuessedTypeRequestor substitutionRequestor =
			new MissingTypesGuesser.GuessedTypeRequestor() {
				@Override
				public void accept(
						TypeBinding guessedType,
						Binding[] missingElements,
						int[] missingElementsStarts,
						int[] missingElementsEnds,
						boolean hasProblems) {
					if (guessedType instanceof ReferenceBinding) {
						findMemberTypes(
								CompletionEngine.this.completionToken,
								(ReferenceBinding)guessedType,
								scope,
								scope.enclosingSourceType(),
								false,
								false,
								new ObjectVector(),
								missingElements,
								missingElementsStarts,
								missingElementsEnds,
								hasProblems);
					}
				}
			};
		missingTypesConverter.guess(typeRef, scope, substitutionRequestor);
	}

	private void findMethodDeclarations(
		char[] selector,
		ReferenceBinding receiverType,
		Scope scope,
		ObjectVector methodsFound,
		Binding[] missingElements,
		int[] missingElementsStarts,
		int[] missingElementsEnds,
		boolean missingElementsHaveProblems) {

		if (selector == null) {
			return;
		}

		MethodBinding[] receiverTypeMethods = receiverType.availableMethods();
		if (receiverTypeMethods != null){
			for (int i = 0; i < receiverTypeMethods.length; i++) {
				if(!receiverTypeMethods[i].isDefaultAbstract()) {
					methodsFound.add(receiverTypeMethods[i]);
				}
			}
		}

		ReferenceBinding currentType = receiverType;

		findInterfacesMethodDeclarations(
			selector,
			receiverType,
			currentType.superInterfaces(),
			scope,
			methodsFound,
			missingElements,
			missingElementsStarts,
			missingElementsEnds,
			missingElementsHaveProblems);

		if (receiverType.isInterface()) {
			currentType = scope.getJavaLangObject();
		} else {
			currentType = receiverType.superclass();
		}

		boolean hasPotentialDefaultAbstractMethods = true;
		boolean java8Plus = this.compilerOptions.sourceLevel >= ClassFileConstants.JDK1_8;
		while (currentType != null) {

			MethodBinding[] methods = currentType.availableMethods();
			if (methods != null) {
				findLocalMethodDeclarations(
					selector,
					methods,
					scope,
					methodsFound,
					false,
					receiverType);
			}

			if (hasPotentialDefaultAbstractMethods && (java8Plus ||
					(currentType.isAbstract() ||
							currentType.isTypeVariable() ||
							currentType.isIntersectionType() ||
							currentType.isEnum()))){

				ReferenceBinding[] superInterfaces = currentType.superInterfaces();

				findInterfacesMethodDeclarations(
					selector,
					receiverType,
					superInterfaces,
					scope,
					methodsFound,
					missingElements,
					missingElementsStarts,
					missingElementsEnds,
					missingElementsHaveProblems);
			} else {
				hasPotentialDefaultAbstractMethods = false;
			}
			currentType = currentType.superclass();
		}
	}

	private char[][] findMethodParameterNames(MethodBinding method, char[][] parameterTypeNames){
		TypeBinding erasure =  method.declaringClass.erasure();
		if(!(erasure instanceof ReferenceBinding)) return null;

		char[][] parameterNames = null;

		int length = parameterTypeNames.length;

		if (length == 0){
			return CharOperation.NO_CHAR_CHAR;
		}
		// look into the corresponding unit if it is available
		if (erasure instanceof SourceTypeBinding){
			SourceTypeBinding sourceType = (SourceTypeBinding) erasure;

			if (sourceType.scope != null){
				TypeDeclaration parsedType;

				if ((parsedType = sourceType.scope.referenceContext) != null){
					AbstractMethodDeclaration methodDecl = parsedType.declarationOf(method.original());

					if (methodDecl != null){
						Argument[] arguments = methodDecl.arguments;
						parameterNames = new char[length][];

						for(int i = 0 ; i < length ; i++){
							parameterNames[i] = arguments[i].name;
						}
					}
				}
			}
		}
		// look into the model
		if(parameterNames == null){

			ReferenceBinding bindingType = (ReferenceBinding)erasure;

			char[] compoundName = CharOperation.concatWith(bindingType.compoundName, '.');
			Object type = this.typeCache.get(compoundName);

			ISourceType sourceType = null;
			if(type != null) {
				if(type instanceof ISourceType) {
					sourceType = (ISourceType) type;
				}
			} else {
				NameEnvironmentAnswer answer = this.nameEnvironment.findTypeInModules(bindingType.compoundName, this.unitScope.module());
				if(answer != null && answer.isSourceType()) {
					sourceType = answer.getSourceTypes()[0];
					this.typeCache.put(compoundName, sourceType);
				}
			}

			if(sourceType != null) {
				IType typeHandle = ((SourceTypeElementInfo) sourceType).getHandle();

				String[] parameterTypeSignatures = new String[length];
				for (int i = 0; i < length; i++) {
					parameterTypeSignatures[i] = Signature.createTypeSignature(parameterTypeNames[i], false);
				}
				IMethod searchedMethod = typeHandle.getMethod(String.valueOf(method.selector), parameterTypeSignatures);
				IMethod[] foundMethods = typeHandle.findMethods(searchedMethod);

				if(foundMethods != null) {
					int len = foundMethods.length;
					if(len == 1) {
						try {
							SourceMethod sourceMethod = (SourceMethod) foundMethods[0];
							parameterNames = ((SourceMethodElementInfo) sourceMethod.getElementInfo()).getArgumentNames();
						} catch (JavaModelException e) {
							// method doesn't exist: ignore
						}
					}
				}
			}
		}
		return parameterNames;
	}

	private void findMethods(
		char[] selector,
		TypeBinding[] typeArgTypes,
		TypeBinding[] argTypes,
		ReferenceBinding receiverType,
		Scope scope,
		ObjectVector methodsFound,
		boolean onlyStaticMethods,
		boolean exactMatch,
		InvocationSite invocationSite,
		Scope invocationScope,
		boolean implicitCall,
		boolean superCall,
		boolean canBePrefixed,
		Binding[] missingElements,
		int[] missingElementsStarts,
		int[] missingElementsEnds,
		boolean missingElementsHaveProblems,
		char[] castedReceiver,
		int receiverStart,
		int receiverEnd) {

		boolean notInJavadoc = this.assistNodeInJavadoc == 0;
		if (selector == null && notInJavadoc) {
			return;
		}

		if (this.assistNodeIsInsideCase)
			return;		// no methods should be proposed inside case expression

		ReferenceBinding currentType = receiverType;
		if (notInJavadoc) {
			if (receiverType.isInterface()) {
				findInterfacesMethods(
					selector,
					typeArgTypes,
					argTypes,
					receiverType,
					new ReferenceBinding[]{currentType},
					scope,
					methodsFound,
					onlyStaticMethods,
					exactMatch,
					invocationSite,
					invocationScope,
					implicitCall,
					superCall,
					canBePrefixed,
					missingElements,
					missingElementsStarts,
					missingElementsEnds,
					missingElementsHaveProblems,
					castedReceiver,
					receiverStart,
					receiverEnd);

				currentType = scope.getJavaLangObject();
			}
		}
		boolean hasPotentialDefaultAbstractMethods = true;
		boolean java8Plus = this.compilerOptions.sourceLevel >= ClassFileConstants.JDK1_8;
		while (currentType != null) {

			MethodBinding[] methods = currentType.availableMethods();
			if (methods != null) {
				findLocalMethods(
					selector,
					typeArgTypes,
					argTypes,
					methods,
					scope,
					methodsFound,
					onlyStaticMethods,
					exactMatch,
					receiverType,
					invocationSite,
					invocationScope,
					implicitCall,
					superCall,
					canBePrefixed,
					missingElements,
					missingElementsStarts,
					missingElementsEnds,
					missingElementsHaveProblems,
					castedReceiver,
					receiverStart,
					receiverEnd);
			}

			/* Searching of superinterfaces for candidate proposal methods can be skipped if current type is concrete, but only for source levels below 1.8.
			   For 1.8 even a concrete type's superinterfaces should be searched as they could have default methods which are not implemented by the concrete
			   type.
			*/
			if (hasPotentialDefaultAbstractMethods &&
					(java8Plus || (currentType.isAbstract() || currentType.isTypeVariable() || currentType.isIntersectionType() || currentType.isEnum()))) {

				ReferenceBinding[] superInterfaces = currentType.superInterfaces();
				if (superInterfaces != null && currentType.isIntersectionType()) {
					for (int i = 0; i < superInterfaces.length; i++) {
						superInterfaces[i] = (ReferenceBinding)superInterfaces[i].capture(invocationScope, invocationSite.sourceStart(), invocationSite.sourceEnd());
					}
				}

				findInterfacesMethods(
					selector,
					typeArgTypes,
					argTypes,
					receiverType,
					superInterfaces,
					scope,
					methodsFound,
					onlyStaticMethods,
					exactMatch,
					invocationSite,
					invocationScope,
					implicitCall,
					superCall,
					canBePrefixed,
					missingElements,
					missingElementsStarts,
					missingElementsEnds,
					missingElementsHaveProblems,
					castedReceiver,
					receiverStart,
					receiverEnd);
			} else {
				if (!java8Plus)
					hasPotentialDefaultAbstractMethods = false;
			}
			currentType = currentType.superclass();
		}
	}

	private void findNestedTypes(
		char[] typeName,
		SourceTypeBinding currentType,
		Scope scope,
		boolean proposeAllMemberTypes,
		ObjectVector typesFound) {

		if (typeName == null)
			return;

		int typeLength = typeName.length;

		SourceTypeBinding nextTypeToIgnore = null;
		while (scope != null) { // done when a COMPILATION_UNIT_SCOPE is found

			switch (scope.kind) {

				case Scope.METHOD_SCOPE :
				case Scope.BLOCK_SCOPE :
					BlockScope blockScope = (BlockScope) scope;

					next : for (int i = 0, length = blockScope.subscopeCount; i < length; i++) {

						if (blockScope.subscopes[i] instanceof ClassScope) {
							SourceTypeBinding localType =
								((ClassScope) blockScope.subscopes[i]).referenceContext.binding;

							if (!localType.isAnonymousType()) {
								if (isForbidden(localType))
									continue next;

								if (typeLength > localType.sourceName.length)
									continue next;
								if (isFailedMatch(typeName, localType.sourceName))
									continue next;

								for (int j = typesFound.size; --j >= 0;) {
									ReferenceBinding otherType = (ReferenceBinding) typesFound.elementAt(j);

									if (TypeBinding.equalsEquals(localType, otherType))
										continue next;
								}

								if (this.assistNodeIsExtendedType && localType.isFinal()) continue next;
								if (this.assistNodeIsInterfaceExcludingAnnotation && localType.isAnnotationType()) continue next;
								if(this.assistNodeIsClass) {
									if(!localType.isClass()) continue next;
								} else if(this.assistNodeIsInterface) {
									if(!localType.isInterface() && !localType.isAnnotationType()) continue next;
								} else if (this.assistNodeIsAnnotation) {
									if(!localType.isAnnotationType()) continue next;
								}

								int relevance = computeBaseRelevance();
								relevance += computeRelevanceForResolution();
								relevance += computeRelevanceForInterestingProposal(localType);
								relevance += computeRelevanceForCaseMatching(typeName, localType.sourceName);
								relevance += computeRelevanceForExpectingType(localType);
								relevance += computeRelevanceForException(localType.sourceName);
								relevance += computeRelevanceForClass();
								relevance += computeRelevanceForQualification(false);
								relevance += computeRelevanceForRestrictions(IAccessRule.K_ACCESSIBLE); // no access restriction for nested type
								relevance += computeRelevanceForAnnotationTarget(localType);

								boolean allowingLongComputationProposals = isAllowingLongComputationProposals();
								if (!this.assistNodeIsConstructor || !allowingLongComputationProposals || hasArrayTypeAsExpectedSuperTypes()) {
									this.noProposal = false;
									if(!this.requestor.isIgnored(CompletionProposal.TYPE_REF)) {
										createTypeProposal(
												localType,
												localType.sourceName,
												IAccessRule.K_ACCESSIBLE,
												localType.sourceName,
												relevance,
												null,
												null,
												null,
												false);
									}
								}

								if (this.assistNodeIsConstructor && allowingLongComputationProposals) {
									findConstructorsOrAnonymousTypes(
											localType,
											blockScope,
											FakeInvocationSite,
											false,
											relevance);
								}
							}
						}
					}
					break;

				case Scope.CLASS_SCOPE :
					SourceTypeBinding enclosingSourceType = scope.enclosingSourceType();
					findMemberTypes(
							typeName,
							enclosingSourceType,
							scope,
							currentType,
							false,
							false,
							false,
							false,
							proposeAllMemberTypes,
							nextTypeToIgnore,
							typesFound,
							null,
							null,
							null,
							false);
					nextTypeToIgnore = enclosingSourceType;
					if (typeLength == 0)
						return; // do not search outside the class scope if no prefix was provided
					break;

				case Scope.COMPILATION_UNIT_SCOPE :
					return;
			}
			scope = scope.parent;
		}
	}

	private void proposeModuleName(CompilationUnitDeclaration parsedUnit) {
		String projectName = this.javaProject.getElementName();
		char[] moduleName = projectName.toCharArray();
		if (moduleName.length > 0) {// do not propose invalid names
			if (!Character.isJavaIdentifierStart(moduleName[0])) return;
			for (char c : moduleName) {
				if (!Character.isJavaIdentifierPart(c) && c != '.') return;
			}
		}
		this.completionToken = CharOperation.concatWith(this.moduleDeclaration.tokens, '.');
		setSourceRange(this.moduleDeclaration.sourceStart, this.moduleDeclaration.bodyStart);
		if (this.completionToken.length > 0 && !CharOperation.prefixEquals(this.completionToken, moduleName)) return;

		InternalCompletionProposal proposal =  createProposal(CompletionProposal.MODULE_DECLARATION, this.actualCompletionPosition);
		proposal.setName(moduleName);
		proposal.setDeclarationSignature(moduleName);
		proposal.setCompletion(moduleName);
		proposal.setReplaceRange((this.startPosition < 0) ? 0 : this.startPosition - this.offset, this.endPosition - this.offset);
		proposal.setTokenRange((this.tokenStart < 0) ? 0 : this.tokenStart - this.offset, this.tokenEnd - this.offset);
		proposal.setRelevance(R_MODULE_DECLARATION);
		this.requestor.accept(proposal);
		if(DEBUG) {
			this.printDebug(proposal);
		}
	}

	private HashSet<String> getAllJarModuleNames(IJavaProject javaProject2) {
		HashSet<String> modules = new HashSet<>();
		try {
			for (IPackageFragmentRoot root : javaProject2.getAllPackageFragmentRoots()) {
				if (root instanceof JarPackageFragmentRoot) {
					IModuleDescription desc = root.getModuleDescription();
					desc = desc == null ? ((JarPackageFragmentRoot) root).getAutomaticModuleDescription() : desc;
					String name = desc != null ? desc.getElementName() : null;
					if (name != null && name.length() > 0)
						modules.add(name);
				}
			}
		} catch (JavaModelException e) {
			// do nothing
		}
		return modules;
	}
	private void findTargettedModules(char[] prefix, HashSet<String> skipSet) {
		HashSet<String> probableModules = new HashSet<>();
		ModuleSourcePathManager mManager = JavaModelManager.getModulePathManager();
		JavaElementRequestor javaElementRequestor = new JavaElementRequestor();
		try {
			mManager.seekModule(this.completionToken, true, javaElementRequestor);
			IModuleDescription[] modules = javaElementRequestor.getModules();
			for (IModuleDescription module : modules) {
				String name = module.getElementName();
				if (name == null || name.equals("")) //$NON-NLS-1$
					continue;
				probableModules.add(name);
			}
		} catch (JavaModelException e) {
			// TODO ignore for now
		}
		probableModules.addAll(getAllJarModuleNames(this.javaProject));
		if (prefix != CharOperation.ALL_PREFIX && prefix != null && prefix.length > 0) {
			probableModules.removeIf(e -> isFailedMatch(prefix, e.toCharArray()));
		}
		for (String s : probableModules) {
			if (!skipSet.contains(s))
				this.acceptModule(s.toCharArray());
		}
	}
	private void findTargettedModules(CompletionOnModuleReference moduleReference, HashSet<String> skipSet) {
		setCompletionToken(moduleReference.tokens, moduleReference.sourceStart, moduleReference.sourceEnd, moduleReference.sourcePositions);
		findTargettedModules(CharOperation.toLowerCase(this.completionToken), skipSet);
	}
	private void findTargettedModules(CompletionOnJavadocQualifiedTypeReference typeReference, HashSet<String> skipSet) {
		setCompletionToken(typeReference.tokens, typeReference.sourceStart, typeReference.sourceEnd, typeReference.sourcePositions);
		findTargettedModules(CharOperation.toLowerCase(this.completionToken), skipSet);
	}

	private void setCompletionToken(char[][] tokens, int sourceStart, int sourceEnd, long[] sourcePositions, boolean without) {
		this.completionToken = without ? CharOperation.concatWith(tokens, '.') : CharOperation.concatWithAll(tokens, '.');
		if (this.completionToken.length == 0)
			this.completionToken = CharOperation.ALL_PREFIX;
		setSourceRange(sourceStart, sourceEnd);
		long completionPosition = sourcePositions[sourcePositions.length - 1];
		setTokenRange((int) (completionPosition >>> 32), (int) completionPosition);
	}
	private void setCompletionToken(char[][] tokens, int sourceStart, int sourceEnd, long[] sourcePositions) {
		setCompletionToken(tokens, sourceStart, sourceEnd, sourcePositions, tokens.length > 0 && tokens[tokens.length - 1].length > 0);
	}
	private void findModules(CompletionOnModuleReference moduleReference, boolean targetted) {
		setCompletionToken(moduleReference.tokens, moduleReference.sourceStart, moduleReference.sourceEnd, moduleReference.sourcePositions);
		findTargettedModules(moduleReference, new HashSet<>()); // empty skipSet passed
		this.nameEnvironment.findModules(CharOperation.toLowerCase(this.completionToken), this, targetted ? this.javaProject : null);
	}
	private void findModules(CompletionOnJavadocQualifiedTypeReference typeReference, boolean targetted) {
		if (JavaCore.compareJavaVersions(this.sourceLevel, JavaCore.VERSION_15) >= 0 ) {
			boolean isIgnoredModuleRef = false;
			try {
				if (isIgnored(CompletionProposal.MODULE_REF)) {
					this.requestor.setIgnored(CompletionProposal.MODULE_REF, false);
					isIgnoredModuleRef = true;
				}
				setCompletionToken(typeReference.tokens, typeReference.sourceStart, typeReference.sourceEnd, typeReference.sourcePositions);
				findTargettedModules(typeReference, new HashSet<>()); // empty skipSet passed
				this.nameEnvironment.findModules(CharOperation.toLowerCase(this.completionToken), this, targetted ? this.javaProject : null);
			} finally {
				this.requestor.setIgnored(CompletionProposal.MODULE_REF, isIgnoredModuleRef);
			}
		}
	}
	private void findModules(CompletionOnJavadocSingleTypeReference typeReference, boolean targetted) {
		if (JavaCore.compareJavaVersions(this.sourceLevel, JavaCore.VERSION_15) >= 0 ) {
			boolean isIgnoredModuleRef = false;
			try {
				if (isIgnored(CompletionProposal.MODULE_REF)) {
					this.requestor.setIgnored(CompletionProposal.MODULE_REF, false);
					isIgnoredModuleRef = true;
				}
				this.nameEnvironment.findModules(CharOperation.toLowerCase(typeReference.token), this, targetted ? this.javaProject : null);
			} finally {
				this.requestor.setIgnored(CompletionProposal.MODULE_REF, isIgnoredModuleRef);
			}
		}
	}
	private void findPackages(CompletionOnPackageVisibilityReference reference) {
		setCompletionToken(reference.tokens, reference.sourceStart, reference.sourceEnd, reference.sourcePositions, false);
		findPackagesInCurrentModule();
	}

	private void findPackagesInCurrentModule() {
		try {
			IPackageFragmentRoot[] moduleRoots = SearchableEnvironment.getOwnedPackageFragmentRoots(this.javaProject);
			this.nameEnvironment.findPackages(CharOperation.toLowerCase(this.completionToken), this, moduleRoots, false);
		} catch (JavaModelException e) {
			// silent
		}
	}
	private void findPackages(CompletionOnPackageReference packageStatement) {
		this.completionToken = CharOperation.concatWithAll(packageStatement.tokens, '.');
		if (this.completionToken.length == 0)
			return;
		setSourceRange(packageStatement.sourceStart, packageStatement.sourceEnd);
		long completionPosition = packageStatement.sourcePositions[packageStatement.sourcePositions.length - 1];
		setTokenRange((int) (completionPosition >>> 32), (int) completionPosition);
		try {
			this.nameEnvironment.findPackages(CharOperation.toLowerCase(this.completionToken), this, this.javaProject.getAllPackageFragmentRoots(), true);
		} catch (JavaModelException e) {
			// silent
		}
	}

	private void findParameterizedType(TypeReference ref, Scope scope) {
		ReferenceBinding refBinding = (ReferenceBinding) ref.resolvedType;
		if(refBinding != null) {
			if (this.options.checkDeprecation &&
					refBinding.isViewedAsDeprecated() &&
					!scope.isDefinedInSameUnit(refBinding))
				return;

			int accessibility = IAccessRule.K_ACCESSIBLE;
			if(refBinding.hasRestrictedAccess()) {
				AccessRestriction accessRestriction = this.lookupEnvironment.getAccessRestriction(refBinding);
				if(accessRestriction != null) {
					switch (accessRestriction.getProblemId()) {
						case IProblem.ForbiddenReference:
							if (this.options.checkForbiddenReference) {
								return;
							}
							accessibility = IAccessRule.K_NON_ACCESSIBLE;
							break;
						case IProblem.DiscouragedReference:
							if (this.options.checkDiscouragedReference) {
								return;
							}
							accessibility = IAccessRule.K_DISCOURAGED;
							break;
					}
				}
			}

			int relevance = computeBaseRelevance();
			relevance += computeRelevanceForResolution();
			relevance += computeRelevanceForInterestingProposal();
			relevance += computeRelevanceForCaseMatching(refBinding.sourceName, refBinding.sourceName);
			relevance += computeRelevanceForExpectingType(refBinding);
			relevance += computeRelevanceForQualification(false);
			relevance += computeRelevanceForRestrictions(accessibility); // no access restriction for type in the current unit

			if(!this.requestor.isIgnored(CompletionProposal.TYPE_REF)) {
				createTypeProposal(
						refBinding,
						refBinding.qualifiedSourceName(),
						IAccessRule.K_ACCESSIBLE,
						CharOperation.NO_CHAR,
						relevance,
						null,
						null,
						null,
						false);
			}
		}
	}

	private void findSubMemberTypes(
		char[] typeName,
		ReferenceBinding receiverType,
		Scope scope,
		SourceTypeBinding typeInvocation,
		boolean staticOnly,
		boolean staticFieldsAndMethodOnly,
		boolean fromStaticImport,
		ObjectVector typesFound) {

		ReferenceBinding currentType = receiverType;
		if (typeName == null)
			return;

		if (this.assistNodeIsSuperType && !this.insideQualifiedReference && isForbidden(currentType)) return; // we're trying to find a supertype

		findMemberTypes(
				typeName,
				currentType.memberTypes(),
				typesFound,
				receiverType,
				typeInvocation,
				staticOnly,
				staticFieldsAndMethodOnly,
				fromStaticImport,
				true,
				scope,
				null,
				null,
				null,
				false);

		ReferenceBinding[] memberTypes = receiverType.memberTypes();
		next : for (int i = 0; i < memberTypes.length; i++) {
			if (this.options.checkVisibility) {
				if (typeInvocation != null && !memberTypes[i].canBeSeenBy(receiverType, typeInvocation)) {
					continue next;
				} else if(typeInvocation == null && !memberTypes[i].canBeSeenBy(this.unitScope.fPackage)) {
					continue next;
				}
			}
			findSubMemberTypes(
				typeName,
				memberTypes[i],
				scope,
				typeInvocation,
				staticOnly,
				staticFieldsAndMethodOnly,
				fromStaticImport,
				typesFound);
		}
	}

	private void findTrueOrFalseKeywords(char[][] choices) {
		if(choices == null || choices.length == 0) return;

		if(this.expectedTypesPtr != 0 || TypeBinding.notEquals(this.expectedTypes[0], TypeBinding.BOOLEAN)) return;

		for (int i = 0; i < choices.length; i++) {
			if (CharOperation.equals(choices[i], Keywords.TRUE) ||
					CharOperation.equals(choices[i], Keywords.FALSE)
			){
				int relevance = computeBaseRelevance();
				relevance += computeRelevanceForResolution();
				relevance += computeRelevanceForInterestingProposal();
				relevance += computeRelevanceForCaseMatching(CharOperation.NO_CHAR, choices[i]);
				relevance += computeRelevanceForRestrictions(IAccessRule.K_ACCESSIBLE); // no access restriction for keywors
				relevance += computeRelevanceForExpectingType(TypeBinding.BOOLEAN);
				relevance += computeRelevanceForQualification(false);
				relevance += R_TRUE_OR_FALSE;

				this.noProposal = false;
				if(!this.requestor.isIgnored(CompletionProposal.KEYWORD)) {
					InternalCompletionProposal proposal =  createProposal(CompletionProposal.KEYWORD, this.actualCompletionPosition);
					proposal.setName(choices[i]);
					proposal.setCompletion(choices[i]);
					proposal.setReplaceRange(this.startPosition - this.offset, this.endPosition - this.offset);
					proposal.setTokenRange(this.tokenStart - this.offset, this.tokenEnd - this.offset);
					proposal.setRelevance(relevance);
					this.requestor.accept(proposal);
					if(DEBUG) {
						this.printDebug(proposal);
					}
				}
			}
		}
	}

	private void findTypeParameters(char[] token, Scope scope) {
		if (this.compilerOptions.sourceLevel < ClassFileConstants.JDK1_5) return;

		TypeParameter[] typeParameters = null;
		while (scope != null) { // done when a COMPILATION_UNIT_SCOPE is found
			typeParameters = null;
			switch (scope.kind) {
				case Scope.METHOD_SCOPE :
					MethodScope methodScope = (MethodScope) scope;
					if(methodScope.referenceContext instanceof MethodDeclaration) {
						MethodDeclaration methodDeclaration = (MethodDeclaration) methodScope.referenceContext;
						typeParameters = methodDeclaration.typeParameters;
					} else if(methodScope.referenceContext instanceof ConstructorDeclaration) {
						ConstructorDeclaration methodDeclaration = (ConstructorDeclaration) methodScope.referenceContext;
						typeParameters = methodDeclaration.typeParameters;
					}
					break;
				case Scope.CLASS_SCOPE :
					ClassScope classScope = (ClassScope) scope;
					typeParameters = classScope.referenceContext.typeParameters;
					break;
				case Scope.COMPILATION_UNIT_SCOPE :
					return;
			}
			if(typeParameters != null) {
				for (int i = 0; i < typeParameters.length; i++) {
					int typeLength = token.length;
					TypeParameter typeParameter = typeParameters[i];

					if(typeParameter.binding == null) continue;

					if (typeLength > typeParameter.name.length) continue;

					if (isFailedMatch(token, typeParameter.name)) continue;

					int relevance = computeBaseRelevance();
					relevance += computeRelevanceForResolution();
					relevance += computeRelevanceForInterestingProposal();
					relevance += computeRelevanceForCaseMatching(token, typeParameter.name);
					relevance += computeRelevanceForExpectingType(typeParameter.type == null ? null :typeParameter.type.resolvedType);
					relevance += computeRelevanceForQualification(false);
					relevance += computeRelevanceForException(typeParameter.name);
					relevance += computeRelevanceForRestrictions(IAccessRule.K_ACCESSIBLE); // no access restriction fot type parameter

					this.noProposal = false;
					if(!this.requestor.isIgnored(CompletionProposal.TYPE_REF)) {
						createTypeParameterProposal(typeParameter, relevance);
					}
				}
			}
			scope = scope.parent;
		}
	}

	private void findTypesAndPackages(char[] token, Scope scope, boolean proposeBaseTypes, boolean proposeVoidType, ObjectVector typesFound) {

		if (token == null)
			return;

		boolean allowingLongComputationProposals = isAllowingLongComputationProposals();

		boolean proposeType =
			!this.requestor.isIgnored(CompletionProposal.TYPE_REF) ||
			((this.assistNodeInJavadoc & CompletionOnJavadoc.TEXT) != 0 && !this.requestor.isIgnored(CompletionProposal.JAVADOC_TYPE_REF));

		boolean proposeAllMemberTypes = !this.assistNodeIsConstructor;

		boolean proposeConstructor =
			allowingLongComputationProposals &&
			this.assistNodeIsConstructor &&
			(!isIgnored(CompletionProposal.CONSTRUCTOR_INVOCATION, CompletionProposal.TYPE_REF) ||
					!isIgnored(CompletionProposal.ANONYMOUS_CLASS_CONSTRUCTOR_INVOCATION, CompletionProposal.TYPE_REF));


		if ((proposeType || proposeConstructor) && scope.enclosingSourceType() != null) {

			checkCancel();

			findNestedTypes(token, scope.enclosingSourceType(), scope, proposeAllMemberTypes, typesFound);
			if(!this.assistNodeIsInterface &&
					!this.assistNodeIsConstructor &&
					!this.assistNodeIsAnnotation &&
					this.assistNodeInJavadoc == 0) {

				checkCancel();

				// don't propose type parameters if the completion is a constructor ('new |')
				findTypeParameters(token, scope);
			}
		}

		boolean isEmptyPrefix = token.length == 0;

		if ((proposeType || proposeConstructor) && this.unitScope != null) {

			ReferenceBinding outerInvocationType = scope.enclosingSourceType();
			if(outerInvocationType != null) {
				ReferenceBinding temp = outerInvocationType.enclosingType();
				while(temp != null) {
					outerInvocationType = temp;
					temp = temp.enclosingType();
				}
			}

			int typeLength = token.length;
			SourceTypeBinding[] types = this.unitScope.topLevelTypes;

			next : for (int i = 0, length = types.length; i < length; i++) {

				checkCancel();

				SourceTypeBinding sourceType = types[i];

				if(isForbidden(sourceType)) continue next;

				if(proposeAllMemberTypes &&
					TypeBinding.notEquals(sourceType, outerInvocationType)) {
					findSubMemberTypes(
							token,
							sourceType,
							scope,
							scope.enclosingSourceType(),
							false,
							false,
							false,
							typesFound);
				}

				if (sourceType.sourceName == CompletionParser.FAKE_TYPE_NAME) continue next;
				if (sourceType.sourceName == TypeConstants.PACKAGE_INFO_NAME) continue next;

				if (typeLength > sourceType.sourceName.length) continue next;

				if (isFailedMatch(token, sourceType.sourceName)) continue next;

				if (this.assistNodeIsAnnotation && !hasPossibleAnnotationTarget(sourceType, scope)) {
					continue next;
				}

				for (int j = typesFound.size; --j >= 0;) {
					ReferenceBinding otherType = (ReferenceBinding) typesFound.elementAt(j);

					if (TypeBinding.equalsEquals(sourceType, otherType)) continue next;
				}

				typesFound.add(sourceType);

				if (this.assistNodeIsExtendedType && sourceType.isFinal()) continue next;
				if (this.assistNodeIsExtendedType && sourceType.isRecord()) continue next;
				if (this.assistNodeIsInterfaceExcludingAnnotation && sourceType.isAnnotationType()) continue next;
				if(this.assistNodeIsClass) {
					if(!sourceType.isClass()) continue next;
				} else if(this.assistNodeIsInterface) {
					if(!sourceType.isInterface() && !sourceType.isAnnotationType()) continue next;
				} else if (this.assistNodeIsAnnotation) {
					if(!sourceType.isAnnotationType()) continue next;
				} else if (this.assistNodeIsException) {
					 if (!sourceType.isClass()) continue next;
					 if (isEmptyPrefix) {
						 if (sourceType.findSuperTypeOriginatingFrom(TypeIds.T_JavaLangThrowable, true) == null) {
							 continue next;
					     }
					  }
				}

				int relevance = computeBaseRelevance();
				relevance += computeRelevanceForResolution();
				relevance += computeRelevanceForInterestingProposal(sourceType);
				relevance += computeRelevanceForCaseMatching(token, sourceType.sourceName);
				relevance += computeRelevanceForExpectingType(sourceType);
				relevance += computeRelevanceForQualification(false);
				relevance += computeRelevanceForRestrictions(IAccessRule.K_ACCESSIBLE); // no access restriction for type in the current unit

				if (sourceType.isAnnotationType()) {
					relevance += computeRelevanceForAnnotation();
					relevance += computeRelevanceForAnnotationTarget(sourceType);
				} else if (sourceType.isInterface()) {
					relevance += computeRelevanceForInterface();
				} else if(sourceType.isClass()){
					relevance += computeRelevanceForClass();
					relevance += computeRelevanceForException(sourceType.sourceName);
				}


				this.noProposal = false;
				if(proposeType &&
						(!this.assistNodeIsConstructor ||
								!allowingLongComputationProposals ||
								hasStaticMemberTypes(sourceType, null, this.unitScope) ||
								hasMemberTypesInEnclosingScope(sourceType, scope)) ||
								hasArrayTypeAsExpectedSuperTypes()) {
					char[] typeName = sourceType.sourceName();
					createTypeProposal(
								sourceType,
								typeName,
								IAccessRule.K_ACCESSIBLE,
								typeName,
								relevance,
								null,
								null,
								null,
								false);
				}

				if (proposeConstructor) {
					findConstructorsOrAnonymousTypes(
							sourceType,
							scope,
							FakeInvocationSite,
							false,
							relevance);
				}
			}
		}

		if (proposeConstructor && !isEmptyPrefix) {

			checkCancel();

			findTypesFromImports(token, scope, proposeType, typesFound);
		} else if(proposeType) {

			checkCancel();

			findTypesFromStaticImports(token, scope, proposeAllMemberTypes, typesFound);
		}

		if (proposeConstructor) {

			checkCancel();

			findTypesFromExpectedTypes(token, scope, typesFound, proposeType, proposeConstructor);
		}

		if (isEmptyPrefix && !this.assistNodeIsAnnotation) {
			if (!proposeConstructor) {
				findTypesFromExpectedTypes(token, scope, typesFound, proposeType, proposeConstructor);
			}
		} else {
			if(!isEmptyPrefix && !this.requestor.isIgnored(CompletionProposal.KEYWORD)) {
				if (this.assistNodeInJavadoc == 0 || (this.assistNodeInJavadoc & CompletionOnJavadoc.BASE_TYPES) != 0) {
					if (proposeBaseTypes) {
						if (proposeVoidType) {
							findKeywords(token, BASE_TYPE_NAMES, false, false);
						} else {
							findKeywords(token, BASE_TYPE_NAMES_WITHOUT_VOID, false, false);
						}
					}
				}
			}

			if (proposeConstructor) {
				int l = typesFound.size();
				for (int i = 0; i < l; i++) {
					ReferenceBinding typeFound = (ReferenceBinding) typesFound.elementAt(i);
					char[] fullyQualifiedTypeName =
						CharOperation.concat(
								typeFound.qualifiedPackageName(),
								typeFound.qualifiedSourceName(),
								'.');
					this.knownTypes.put(fullyQualifiedTypeName, KNOWN_TYPE_WITH_KNOWN_CONSTRUCTORS);
				}

				checkCancel();

				this.foundConstructorsCount = 0;
				this.nameEnvironment.findConstructorDeclarations(
						token,
						getTypesMatchRule(),
						false,
						this,
						this.monitor);
				acceptConstructors(scope);
			} else if (proposeType) {
				int l = typesFound.size();
				for (int i = 0; i < l; i++) {
					ReferenceBinding typeFound = (ReferenceBinding) typesFound.elementAt(i);
					char[] fullyQualifiedTypeName =
						CharOperation.concat(
								typeFound.qualifiedPackageName(),
								typeFound.qualifiedSourceName(),
								'.');
					this.knownTypes.put(fullyQualifiedTypeName, KNOWN_TYPE_WITH_KNOWN_CONSTRUCTORS);
				}
				int searchFor = IJavaSearchConstants.TYPE;
				if(this.assistNodeIsClass || this.assistNodeIsException) {
					searchFor = IJavaSearchConstants.CLASS;
				} else if (this.assistNodeIsInterfaceExcludingAnnotation) {
					searchFor = IJavaSearchConstants.INTERFACE;
				} else if(this.assistNodeIsInterface) {
					searchFor = IJavaSearchConstants.INTERFACE_AND_ANNOTATION;
				} else if(this.assistNodeIsEnum) {
					searchFor = IJavaSearchConstants.ENUM;
				} else if(this.assistNodeIsAnnotation) {
					searchFor = IJavaSearchConstants.ANNOTATION_TYPE;
				}

				checkCancel();

				this.foundTypesCount = 0;
				this.nameEnvironment.findTypes(
						token,
						proposeAllMemberTypes,
						getTypesMatchRule(),
						searchFor,
						false,
						this,
						this.monitor);
				acceptTypes(scope);
			}
			if(!isEmptyPrefix && !this.requestor.isIgnored(CompletionProposal.PACKAGE_REF)) {

				checkCancel();

				this.nameEnvironment.findPackages(token, this);
			}
		}
	}

	private int getTypesMatchRule() {
		int matchRule = SearchPattern.R_PREFIX_MATCH;
		if (this.options.camelCaseMatch)
			matchRule |= SearchPattern.R_CAMELCASE_MATCH;
		if (this.options.substringMatch)
			matchRule |= SearchPattern.R_SUBSTRING_MATCH;
		if (this.options.subwordMatch)
			matchRule |= SearchPattern.R_SUBWORD_MATCH;
		return matchRule;
	}

	private void findTypesAndSubpackages(
		char[] token,
		PackageBinding packageBinding,
		Scope scope) {

		boolean allowingLongComputationProposals = isAllowingLongComputationProposals();

		boolean proposeType =
			!this.requestor.isIgnored(CompletionProposal.TYPE_REF) ||
			((this.assistNodeInJavadoc & CompletionOnJavadoc.TEXT) != 0 && !this.requestor.isIgnored(CompletionProposal.JAVADOC_TYPE_REF));

		boolean proposeConstructor =
			allowingLongComputationProposals &&
			this.assistNodeIsConstructor &&
			(!isIgnored(CompletionProposal.CONSTRUCTOR_INVOCATION, CompletionProposal.TYPE_REF) ||
					!isIgnored(CompletionProposal.ANONYMOUS_CLASS_CONSTRUCTOR_INVOCATION, CompletionProposal.TYPE_REF));

		char[] qualifiedName =
			CharOperation.concatWith(packageBinding.compoundName, token, '.');

		if (token == null || token.length == 0) {
			int length = qualifiedName.length;
			System.arraycopy(
				qualifiedName,
				0,
				qualifiedName = new char[length + 1],
				0,
				length);
			qualifiedName[length] = '.';
		}

		this.qualifiedCompletionToken = qualifiedName;

		if ((proposeType || proposeConstructor) && this.unitScope != null) {
			int typeLength = qualifiedName.length;
			SourceTypeBinding[] types = this.unitScope.topLevelTypes;

			for (int i = 0, length = types.length; i < length; i++) {

				checkCancel();

				SourceTypeBinding sourceType = types[i];

				if (isForbidden(sourceType)) continue;
				if (this.assistNodeIsClass && sourceType.isInterface()) continue;
				if (this.assistNodeIsInterface && sourceType.isClass()) continue;

				char[] qualifiedSourceTypeName = CharOperation.concatWith(sourceType.compoundName, '.');

				if (sourceType.sourceName == CompletionParser.FAKE_TYPE_NAME) continue;
				if (sourceType.sourceName == TypeConstants.PACKAGE_INFO_NAME) continue;
				if (typeLength > qualifiedSourceTypeName.length) continue;
				if (!(packageBinding == sourceType.getPackage())) continue;

				if (!CharOperation.prefixEquals(qualifiedName, qualifiedSourceTypeName, false)
						&& !(this.options.camelCaseMatch && CharOperation.camelCaseMatch(token, sourceType.sourceName)))	continue;

				if (this.options.checkDeprecation &&
						sourceType.isViewedAsDeprecated() &&
						!scope.isDefinedInSameUnit(sourceType))
					continue;

			    if (this.assistNodeIsExtendedType && sourceType.isFinal()) continue;
			    if (this.assistNodeIsInterfaceExcludingAnnotation && sourceType.isAnnotationType()) continue;
				int accessibility = IAccessRule.K_ACCESSIBLE;
				if(sourceType.hasRestrictedAccess()) {
					AccessRestriction accessRestriction = this.lookupEnvironment.getAccessRestriction(sourceType);
					if(accessRestriction != null) {
						switch (accessRestriction.getProblemId()) {
							case IProblem.ForbiddenReference:
								if (this.options.checkForbiddenReference) {
									continue;
								}
								accessibility = IAccessRule.K_NON_ACCESSIBLE;
								break;
							case IProblem.DiscouragedReference:
								if (this.options.checkDiscouragedReference) {
									continue;
								}
								accessibility = IAccessRule.K_DISCOURAGED;
								break;
						}
					}
				}

				this.knownTypes.put(CharOperation.concat(sourceType.qualifiedPackageName(), sourceType.sourceName(), '.'), KNOWN_TYPE_WITH_KNOWN_CONSTRUCTORS);

				int relevance = computeBaseRelevance();
				relevance += computeRelevanceForResolution();
				relevance += computeRelevanceForInterestingProposal(sourceType);
				relevance += computeRelevanceForCaseMatching(qualifiedName, qualifiedSourceTypeName);
				relevance += computeRelevanceForExpectingType(sourceType);
				relevance += computeRelevanceForQualification(false);
				relevance += computeRelevanceForRestrictions(accessibility);

				if (sourceType.isAnnotationType()) {
					relevance += computeRelevanceForAnnotation();
				} else if (sourceType.isInterface()) {
					relevance += computeRelevanceForInterface();
				} else if (sourceType.isClass()) {
					relevance += computeRelevanceForClass();
					relevance += computeRelevanceForException(sourceType.sourceName);
				}

				this.noProposal = false;
				if(proposeType &&
						(!this.assistNodeIsConstructor ||
								!allowingLongComputationProposals ||
								hasStaticMemberTypes(sourceType, null, this.unitScope) ||
								hasMemberTypesInEnclosingScope(sourceType, scope)) ||
								hasArrayTypeAsExpectedSuperTypes()) {
					char[] typeName = sourceType.sourceName();
					createTypeProposal(
							sourceType,
							typeName,
							IAccessRule.K_ACCESSIBLE,
							typeName,
							relevance,
							null,
							null,
							null,
							false);
				}

				if (proposeConstructor) {
					findConstructorsOrAnonymousTypes(
							sourceType,
							scope,
							FakeInvocationSite,
							false,
							relevance);
				}
			}
		}

		if (proposeConstructor) {


			checkCancel();

			this.foundConstructorsCount = 0;
			this.nameEnvironment.findConstructorDeclarations(
					qualifiedName,
					getTypesMatchRule(),
					false,
					this,
					this.monitor);
			acceptConstructors(scope);
		} if(proposeType) {
			int searchFor = IJavaSearchConstants.TYPE;
			if(this.assistNodeIsClass) {
				searchFor = IJavaSearchConstants.CLASS;
			} else if (this.assistNodeIsInterfaceExcludingAnnotation) {
				searchFor = IJavaSearchConstants.INTERFACE;
			} else if(this.assistNodeIsInterface) {
				searchFor = IJavaSearchConstants.INTERFACE_AND_ANNOTATION;
			} else if(this.assistNodeIsEnum) {
				searchFor = IJavaSearchConstants.ENUM;
			} else if(this.assistNodeIsAnnotation) {
				searchFor = IJavaSearchConstants.ANNOTATION_TYPE;
			}

			checkCancel();

			this.foundTypesCount = 0;
			this.nameEnvironment.findTypes(
					qualifiedName,
					false,
					getTypesMatchRule(),
					searchFor,
					false,
					this,
					this.monitor);
			acceptTypes(scope);
		}

		if(!this.requestor.isIgnored(CompletionProposal.PACKAGE_REF)) {
			this.nameEnvironment.findPackages(qualifiedName, this);
		}
	}

	private void findTypesFromExpectedTypes(char[] token, Scope scope, ObjectVector typesFound, boolean proposeType, boolean proposeConstructor) {
		if(this.expectedTypesPtr > -1) {
			boolean allowingLongComputationProposals = isAllowingLongComputationProposals();

			int typeLength = token == null ? 0 : token.length;

			next : for (int i = 0; i <= this.expectedTypesPtr; i++) {

				checkCancel();

				if(this.expectedTypes[i] instanceof ReferenceBinding) {
					ReferenceBinding refBinding = (ReferenceBinding)this.expectedTypes[i];

					if (typeLength > 0) {
						if (typeLength > refBinding.sourceName.length) continue next;

						if (isFailedMatch(token, refBinding.sourceName)) continue next;
					}


					if(refBinding.isTypeVariable() && this.assistNodeIsConstructor) {
						// don't propose type variable if the completion is a constructor ('new |')
						continue next;
					}
					if (this.options.checkDeprecation &&
							refBinding.isViewedAsDeprecated() &&
							!scope.isDefinedInSameUnit(refBinding))
						continue next;

					int accessibility = IAccessRule.K_ACCESSIBLE;
					if(refBinding.hasRestrictedAccess()) {
						AccessRestriction accessRestriction = this.lookupEnvironment.getAccessRestriction(refBinding);
						if(accessRestriction != null) {
							switch (accessRestriction.getProblemId()) {
								case IProblem.ForbiddenReference:
									if (this.options.checkForbiddenReference) {
										continue next;
									}
									accessibility = IAccessRule.K_NON_ACCESSIBLE;
									break;
								case IProblem.DiscouragedReference:
									if (this.options.checkDiscouragedReference) {
										continue next;
									}
									accessibility = IAccessRule.K_DISCOURAGED;
									break;
							}
						}
					}
					if(isForbidden(refBinding)) continue next;

					for (int j = 0; j < typesFound.size(); j++) {
						ReferenceBinding typeFound = (ReferenceBinding)typesFound.elementAt(j);
						if (TypeBinding.equalsEquals(typeFound, refBinding.erasure())) {
							continue next;
						}
					}

					typesFound.add(refBinding);

					boolean inSameUnit = this.unitScope.isDefinedInSameUnit(refBinding);

					// top level types of the current unit are already proposed.
					if(!inSameUnit || (inSameUnit && refBinding.isMemberType())) {
						char[] packageName = refBinding.qualifiedPackageName();
						char[] typeName = refBinding.sourceName();
						char[] completionName = typeName;

						boolean isQualified = false;
						if (!this.insideQualifiedReference && !refBinding.isMemberType()) {
							if (mustQualifyType(packageName, typeName, null, refBinding.modifiers)) {
								if (packageName == null || packageName.length == 0)
									if (this.unitScope != null && this.unitScope.fPackage.compoundName != CharOperation.NO_CHAR_CHAR)
										continue next; // ignore types from the default package from outside it
								completionName = CharOperation.concat(packageName, typeName, '.');
								isQualified = true;
							}
						}

						if (this.assistNodeIsExtendedType && refBinding.isFinal()) continue next;
						if (this.assistNodeIsInterfaceExcludingAnnotation && refBinding.isAnnotationType()) continue next;
						if(this.assistNodeIsClass) {
							if(!refBinding.isClass()) continue next;
						} else if(this.assistNodeIsInterface) {
							if(!refBinding.isInterface() && !refBinding.isAnnotationType()) continue next;
						} else if (this.assistNodeIsAnnotation) {
							if(!refBinding.isAnnotationType()) continue next;
						}

						int relevance = computeBaseRelevance();
						relevance += computeRelevanceForResolution();
						relevance += computeRelevanceForInterestingProposal(refBinding);
						relevance += computeRelevanceForCaseMatching(token, typeName);
						relevance += computeRelevanceForExpectingType(refBinding);
						relevance += computeRelevanceForQualification(isQualified);
						relevance += computeRelevanceForRestrictions(accessibility);

						if(refBinding.isClass()) {
							relevance += computeRelevanceForClass();
							relevance += computeRelevanceForException(typeName);
						} else if(refBinding.isEnum()) {
							relevance += computeRelevanceForEnum();
						} else if(refBinding.isInterface()) {
							relevance += computeRelevanceForInterface();
						}

						if (proposeType &&
								(!this.assistNodeIsConstructor ||
										!allowingLongComputationProposals ||
										hasStaticMemberTypes(refBinding, scope.enclosingSourceType() ,this.unitScope)) ||
										hasArrayTypeAsExpectedSuperTypes()) {
							this.noProposal = false;
							if(!this.requestor.isIgnored(CompletionProposal.TYPE_REF)) {
								InternalCompletionProposal proposal =  createProposal(CompletionProposal.TYPE_REF, this.actualCompletionPosition);
								proposal.setDeclarationSignature(packageName);
								proposal.setSignature(getSignature(refBinding));
								proposal.setPackageName(packageName);
								proposal.setTypeName(typeName);
								proposal.setCompletion(completionName);
								proposal.setFlags(refBinding.modifiers);
								proposal.setReplaceRange(this.startPosition - this.offset, this.endPosition - this.offset);
								proposal.setTokenRange(this.tokenStart - this.offset, this.tokenEnd - this.offset);
								proposal.setRelevance(relevance);
								proposal.setAccessibility(accessibility);
								this.requestor.accept(proposal);
								if(DEBUG) {
									this.printDebug(proposal);
								}
							}
						}

						if (proposeConstructor) {
							findConstructorsOrAnonymousTypes(
									refBinding,
									scope,
									FakeInvocationSite,
									isQualified,
									relevance);
						}
					}
				}
			}
		}
	}

	private void findTypesFromImports(char[] token, Scope scope, boolean proposeType, ObjectVector typesFound) {
		ImportBinding[] importBindings = scope.compilationUnitScope().imports;
		next : for (int i = 0; i < importBindings.length; i++) {
			ImportBinding importBinding = importBindings[i];
			if(importBinding.isValidBinding()) {
				Binding binding = importBinding.resolvedImport;
				if(binding != null && binding.isValidBinding()) {
					if(importBinding.onDemand) {
						if (importBinding.isStatic()) {
							if((binding.kind() & Binding.TYPE) != 0) {
								this.findMemberTypes(
										token,
										(ReferenceBinding) binding,
										scope,
										scope.enclosingSourceType(),
										true,
										false,
										true,
										true,
										false,
										null,
										typesFound,
										null,
										null,
										null,
										false);
							}
						}
					} else {
						if ((binding.kind() & Binding.TYPE) != 0) {
							ReferenceBinding typeBinding = (ReferenceBinding) binding;
							int typeLength = token.length;

							if (!typeBinding.isStatic()) continue next;

							if (typeLength > typeBinding.sourceName.length)	continue next;

							if (isFailedMatch(token, typeBinding.sourceName)) continue next;

							int accessibility = IAccessRule.K_ACCESSIBLE;
							if(typeBinding.hasRestrictedAccess()) {
								AccessRestriction accessRestriction = this.lookupEnvironment.getAccessRestriction(typeBinding);
								if(accessRestriction != null) {
									switch (accessRestriction.getProblemId()) {
										case IProblem.ForbiddenReference:
											if (this.options.checkForbiddenReference) {
												continue next;
											}
											accessibility = IAccessRule.K_NON_ACCESSIBLE;
											break;
										case IProblem.DiscouragedReference:
											if (this.options.checkDiscouragedReference) {
												continue next;
											}
											accessibility = IAccessRule.K_DISCOURAGED;
											break;
									}
								}
							}

							if (typesFound.contains(typeBinding)) continue next;

							typesFound.add(typeBinding);

							if (this.assistNodeIsExtendedType && typeBinding.isFinal()) continue;
							if (this.assistNodeIsInterfaceExcludingAnnotation && typeBinding.isAnnotationType()) continue;
							if(this.assistNodeIsClass) {
								if(!typeBinding.isClass()) continue;
							} else if(this.assistNodeIsInterface) {
								if(!typeBinding.isInterface() && !typeBinding.isAnnotationType()) continue;
							} else if (this.assistNodeIsAnnotation) {
								if(!typeBinding.isAnnotationType()) continue;
							}

							int relevance = computeBaseRelevance();
							relevance += computeRelevanceForResolution();
							relevance += computeRelevanceForInterestingProposal(typeBinding);
							relevance += computeRelevanceForCaseMatching(token, typeBinding.sourceName);
							relevance += computeRelevanceForExpectingType(typeBinding);
							relevance += computeRelevanceForQualification(false);
							relevance += computeRelevanceForRestrictions(accessibility);

							if (typeBinding.isAnnotationType()) {
								relevance += computeRelevanceForAnnotation();
								relevance += computeRelevanceForAnnotationTarget(typeBinding);
							} else if (typeBinding.isInterface()) {
								relevance += computeRelevanceForInterface();
							} else if(typeBinding.isClass()){
								relevance += computeRelevanceForClass();
								relevance += computeRelevanceForException(typeBinding.sourceName);
							}

							if (proposeType &&
									(hasStaticMemberTypes(typeBinding, scope.enclosingSourceType(), this.unitScope) || hasArrayTypeAsExpectedSuperTypes())) {
								this.noProposal = false;
								if(!this.requestor.isIgnored(CompletionProposal.TYPE_REF)) {
									InternalCompletionProposal proposal =  createProposal(CompletionProposal.TYPE_REF, this.actualCompletionPosition);
									proposal.setDeclarationSignature(typeBinding.qualifiedPackageName());
									proposal.setSignature(getSignature(typeBinding));
									proposal.setPackageName(typeBinding.qualifiedPackageName());
									proposal.setTypeName(typeBinding.qualifiedSourceName());
									proposal.setCompletion(typeBinding.sourceName());
									proposal.setFlags(typeBinding.modifiers);
									proposal.setReplaceRange(this.startPosition - this.offset, this.endPosition - this.offset);
									proposal.setTokenRange(this.tokenStart - this.offset, this.tokenEnd - this.offset);
									proposal.setRelevance(relevance);
									this.requestor.accept(proposal);
									if(DEBUG) {
										this.printDebug(proposal);
									}
								}
							}

							findConstructorsOrAnonymousTypes(
									typeBinding,
									scope,
									FakeInvocationSite,
									false,
									relevance);
						}
					}
				}
			}
		}
	}

	private void findTypesFromStaticImports(char[] token, Scope scope, boolean proposeAllMemberTypes, ObjectVector typesFound) {
		ImportBinding[] importBindings = scope.compilationUnitScope().imports;
		if (importBindings == null) return;
		for (int i = 0; i < importBindings.length; i++) {
			ImportBinding importBinding = importBindings[i];
			if(importBinding.isValidBinding() && importBinding.isStatic()) {
				Binding binding = importBinding.resolvedImport;
				if(binding != null && binding.isValidBinding()) {
					if(importBinding.onDemand) {
						if((binding.kind() & Binding.TYPE) != 0) {
							this.findMemberTypes(
									token,
									(ReferenceBinding) binding,
									scope,
									scope.enclosingSourceType(),
									true,
									false,
									true,
									true,
									proposeAllMemberTypes,
									null,
									typesFound,
									null,
									null,
									null,
									false);
						}
					} else {
						if ((binding.kind() & Binding.TYPE) != 0) {
							ReferenceBinding typeBinding = (ReferenceBinding) binding;
							int typeLength = token.length;

							if (!typeBinding.isStatic()) continue;

							if (typeLength > typeBinding.sourceName.length)	continue;

							if (isFailedMatch(token, typeBinding.sourceName))	continue;

							if (typesFound.contains(typeBinding))  continue;

							typesFound.add(typeBinding);

							if (this.assistNodeIsExtendedType && typeBinding.isFinal()) continue;
							if (this.assistNodeIsInterfaceExcludingAnnotation && typeBinding.isAnnotationType()) continue;
							if(this.assistNodeIsClass || this.assistNodeIsException) {
								if(!typeBinding.isClass()) continue;
							} else if(this.assistNodeIsInterface) {
								if(!typeBinding.isInterface() && !typeBinding.isAnnotationType()) continue;
							} else if (this.assistNodeIsAnnotation) {
								if(!typeBinding.isAnnotationType()) continue;
							}

							int relevance = computeBaseRelevance();
							relevance += computeRelevanceForResolution();
							relevance += computeRelevanceForInterestingProposal(typeBinding);
							relevance += computeRelevanceForCaseMatching(token, typeBinding.sourceName);
							relevance += computeRelevanceForExpectingType(typeBinding);
							relevance += computeRelevanceForQualification(false);
							relevance += computeRelevanceForRestrictions(IAccessRule.K_ACCESSIBLE);

							if (typeBinding.isClass()) {
								relevance += computeRelevanceForClass();
								relevance += computeRelevanceForException(typeBinding.sourceName);
							} else if(typeBinding.isEnum()) {
								relevance += computeRelevanceForEnum();
							} else if(typeBinding.isInterface()) {
								relevance += computeRelevanceForInterface();
							}

							this.noProposal = false;
							if(!this.requestor.isIgnored(CompletionProposal.TYPE_REF)) {
								InternalCompletionProposal proposal =  createProposal(CompletionProposal.TYPE_REF, this.actualCompletionPosition);
								proposal.setDeclarationSignature(typeBinding.qualifiedPackageName());
								proposal.setSignature(getSignature(typeBinding));
								proposal.setPackageName(typeBinding.qualifiedPackageName());
								proposal.setTypeName(typeBinding.qualifiedSourceName());
								proposal.setCompletion(typeBinding.sourceName());
								proposal.setFlags(typeBinding.modifiers);
								proposal.setReplaceRange(this.startPosition - this.offset, this.endPosition - this.offset);
								proposal.setTokenRange(this.tokenStart - this.offset, this.tokenEnd - this.offset);
								proposal.setRelevance(relevance);
								this.requestor.accept(proposal);
								if(DEBUG) {
									this.printDebug(proposal);
								}
							}
						}
					}
				}
			}
		}
	}

	private void findUnresolvedReference(int completedNameStart, int completedNameEnd, BlockScope scope, char[][] discouragedNames) {
		char[][] foundNames = findUnresolvedReferenceBefore(completedNameStart - 1, completedNameEnd, scope, discouragedNames);
		if (foundNames != null && foundNames.length > 1) {
			int discouragedNamesLength = discouragedNames.length;
			int foundNamesLength = foundNames.length;
			int newLength = discouragedNamesLength + foundNamesLength;
			System.arraycopy(discouragedNames, 0, discouragedNames = new char[newLength][], 0, discouragedNamesLength);
			System.arraycopy(foundNames, 0, discouragedNames, discouragedNamesLength, foundNamesLength);
		}
		findUnresolvedReferenceAfter(completedNameEnd + 1, scope, discouragedNames);
	}

	private char[][] findUnresolvedReferenceAfter(int from, BlockScope scope, final char[][] discouragedNames) {
		final ArrayList proposedNames = new ArrayList();

		UnresolvedReferenceNameFinder.UnresolvedReferenceNameRequestor nameRequestor =
			new UnresolvedReferenceNameFinder.UnresolvedReferenceNameRequestor() {
				@Override
				public void acceptName(char[] name) {
					CompletionEngine.this.acceptUnresolvedName(name);
					proposedNames.add(name);
				}
			};

		ReferenceContext referenceContext = scope.referenceContext();
		if (referenceContext instanceof AbstractMethodDeclaration) {
			AbstractMethodDeclaration md = (AbstractMethodDeclaration)referenceContext;

			UnresolvedReferenceNameFinder nameFinder = new UnresolvedReferenceNameFinder(this);
			nameFinder.findAfter(
					this.completionToken,
					md.scope,
					md.scope.classScope(),
					from,
					md.bodyEnd,
					discouragedNames,
					nameRequestor);
		} else if (referenceContext instanceof LambdaExpression) {
			LambdaExpression expression = (LambdaExpression) referenceContext;
			UnresolvedReferenceNameFinder nameFinder = new UnresolvedReferenceNameFinder(this);
			nameFinder.findAfter(
					this.completionToken,
					expression.scope,
					expression.scope.classScope(),
					from,
					expression.body().sourceEnd,
					discouragedNames,
					nameRequestor);
		} else if (referenceContext instanceof TypeDeclaration) {
			TypeDeclaration typeDeclaration = (TypeDeclaration) referenceContext;
			FieldDeclaration[] fields = typeDeclaration.fields;
			if (fields != null) {
				done : for (int i = 0; i < fields.length; i++) {
					if (fields[i] instanceof Initializer) {
						Initializer initializer = (Initializer) fields[i];
						if (initializer.block.sourceStart <= from &&
								from < initializer.bodyEnd) {
							UnresolvedReferenceNameFinder nameFinder = new UnresolvedReferenceNameFinder(this);
							nameFinder.findAfter(
										this.completionToken,
										typeDeclaration.scope,
										typeDeclaration.scope,
										from,
										initializer.bodyEnd,
										discouragedNames,
										nameRequestor);
							break done;
						}
					}
				}
			}
		}

		int proposedNamesCount = proposedNames.size();
		if (proposedNamesCount > 0) {
			return (char[][])proposedNames.toArray(new char[proposedNamesCount][]);
		}

		return null;
	}

	private char[][] findUnresolvedReferenceBefore(int recordTo, int parseTo, BlockScope scope, final char[][] discouragedNames) {
		final ArrayList proposedNames = new ArrayList();

		UnresolvedReferenceNameFinder.UnresolvedReferenceNameRequestor nameRequestor =
			new UnresolvedReferenceNameFinder.UnresolvedReferenceNameRequestor() {
				@Override
				public void acceptName(char[] name) {
					CompletionEngine.this.acceptUnresolvedName(name);
					proposedNames.add(name);
				}
			};

		BlockScope upperScope = scope;
		while (upperScope.enclosingMethodScope() != null) {
			upperScope = upperScope.enclosingMethodScope();
		}

		ReferenceContext referenceContext = upperScope.referenceContext();
		if (referenceContext instanceof AbstractMethodDeclaration) {
			AbstractMethodDeclaration md = (AbstractMethodDeclaration)referenceContext;

			UnresolvedReferenceNameFinder nameFinder = new UnresolvedReferenceNameFinder(this);
			nameFinder.findBefore(
					this.completionToken,
					md.scope,
					md.scope.classScope(),
					md.bodyStart,
					recordTo,
					parseTo,
					discouragedNames,
					nameRequestor);
		} else if (referenceContext instanceof TypeDeclaration) {
			TypeDeclaration typeDeclaration = (TypeDeclaration) referenceContext;


			done : {
				FieldDeclaration[] fields = typeDeclaration.fields;
				if (fields != null) {
					for (int i = 0; i < fields.length; i++) {
						if (fields[i] instanceof Initializer) {
							Initializer initializer = (Initializer) fields[i];
							if (initializer.block.sourceStart <= recordTo &&
									recordTo < initializer.bodyEnd) {

								UnresolvedReferenceNameFinder nameFinder = new UnresolvedReferenceNameFinder(this);
								nameFinder.findBefore(
										this.completionToken,
										typeDeclaration.scope,
										typeDeclaration.scope,
										initializer.block.sourceStart,
										recordTo,
										parseTo,
										discouragedNames,
										nameRequestor);
								break done;
							}
						}
					}
				}
			}
		}

		int proposedNamesCount = proposedNames.size();
		if (proposedNamesCount > 0) {
			return (char[][])proposedNames.toArray(new char[proposedNamesCount][]);
		}

		return null;
	}

	private void findImplementations(ProvidesStatement providesStmt, TypeReference reference ) {
		char[][] tokens = reference.getTypeName();
		char[] typeName = CharOperation.concatWithAll(tokens, '.');

		if (typeName.length == 0) {
			this.completionToken = CharOperation.ALL_PREFIX;
		} else if (reference instanceof CompletionOnProvidesImplementationsQualifiedTypeReference) {
			CompletionOnQualifiedTypeReference qReference = (CompletionOnQualifiedTypeReference) reference;
			if (qReference.completionIdentifier != null) {
				this.completionToken = CharOperation.concatAll(typeName, qReference.completionIdentifier, '.');
			}
		} else {
			 char[] lastToken = tokens[tokens.length - 1];
			 this.completionToken = lastToken != null && lastToken.length == 0 ?
					 CharOperation.concat(typeName, new char[]{'.'}) :lastToken;
		}
		setSourceRange(reference.sourceStart, reference.sourceEnd);
		findImplementations(this.completionToken, this.unitScope, providesStmt, -1);
	}

	private void findImplementations(char[] token, Scope scope, ProvidesStatement providesStmt, int stmtIndex) {
		TypeReference theInterface = providesStmt.serviceInterface;
		if (token == null) return;
		char[][] theInterfaceType = null;
		if (theInterface.resolvedType != null && theInterface.resolvedType.isValidBinding()) {
			char[] readableName = theInterface.resolvedType.readableName();
			if (readableName != null)
				theInterfaceType = CharOperation.splitOn('.', readableName);
		}
		theInterfaceType = theInterfaceType == null ? theInterface.getTypeName() : theInterfaceType;
		if (theInterfaceType == null) return;
		SearchPattern pattern  = null;
		NameEnvironmentAnswer answer =  this.nameEnvironment.findTypeInModules(theInterfaceType, scope.module());
		if (answer != null ) {
			if (answer.isSourceType()) {
				IType typeHandle = ((SourceTypeElementInfo) answer.getSourceTypes()[0]).getHandle();
				try {
					ArrayList<IType> allTypes = new ArrayList<IType>();
					ITypeHierarchy newTypeHierarchy = typeHandle.newTypeHierarchy(this.javaProject, null);
					IType[] implementingClasses = newTypeHierarchy.getImplementingClasses(typeHandle);
					for (IType iClass : implementingClasses) {
						getAllTypesInHierarchy(newTypeHierarchy,iClass,allTypes);
					}
					for (IType iType : allTypes) {
						String pkg = iType.getPackageFragment().getElementName();
						String name = iType.getElementName();
						if ( CharOperation.ALL_PREFIX != this.completionToken) {
							if(!CharOperation.prefixEquals(this.completionToken, name.toCharArray(), false))
								if(!CharOperation.prefixEquals(this.completionToken, pkg.toCharArray(), false))
									continue;
						}
						this.acceptType(pkg.toCharArray(), name.toCharArray(), CharOperation.NO_CHAR_CHAR, iType.getFlags(), null);
						acceptTypes(scope);
					}
					if(!this.requestor.isIgnored(CompletionProposal.PACKAGE_REF)) {
						checkCancel();
						findPackagesInCurrentModule();
					}
					return;

				} catch (JavaModelException e) {
					//
				}
			} else if (answer.isBinaryType()) {
				String typeName = new String(CharOperation.replaceOnCopy(answer.getBinaryType().getName(), '/', '.'));
				pattern = SearchPattern.createPattern(typeName,
						IJavaSearchConstants.CLASS_AND_INTERFACE,
						IJavaSearchConstants.IMPLEMENTORS, SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE);
			}
		}
		if (pattern == null) return;
		IJavaSearchScope searchScope = BasicSearchEngine.createJavaSearchScope(new IJavaElement[] {this.javaProject});
		class ImplSearchRequestor extends SearchRequestor {
			String prefix;
			LinkedHashSet<String> filter;
			public List<IType> types = new ArrayList<>();
			public ImplSearchRequestor(char[] prefixToken, LinkedHashSet<String> filter) {
				this.prefix = (prefixToken == CharOperation.ALL_PREFIX) ? null : new String(prefixToken);
				this.filter = filter;
			}
			@Override
			public void acceptSearchMatch(SearchMatch match) throws CoreException {
			//	checkCancel();
				IJavaElement element = ((IJavaElement) match.getElement());
				if (element.getElementType() == IJavaElement.TYPE) {
					IType type = (IType) element;
					if (this.prefix != null) {
						String fullTypeName = type.getPackageFragment().getElementName();
						if (fullTypeName != null) {
							fullTypeName = fullTypeName.concat(".").concat(type.getElementName()); //$NON-NLS-1$
						} else {
							fullTypeName = type.getElementName();
						}
						if (this.filter.contains(fullTypeName)) return;
						if (!(fullTypeName.startsWith(this.prefix) || type.getElementName().startsWith(this.prefix)))
							return;
					}
					this.types.add(type);
				}
			}
		}
		try {
			LinkedHashSet<String> existingImpl = new LinkedHashSet<>();
			char[][] theInterfaceName = theInterface.getTypeName();
			// filter out existing implementations of the same interfaces
			for (int i = 0, l = this.moduleDeclaration.servicesCount; i < l; ++i) {
				if (i == stmtIndex) continue;
				ProvidesStatement prevProvides = this.moduleDeclaration.services[i];
				if (!CharOperation.equals(theInterfaceName, prevProvides.serviceInterface.getTypeName())) continue;
				TypeReference[] prevImpls = prevProvides.implementations;
				for (TypeReference prevImpl : prevImpls) {
					char[][] typeName = prevImpl.getTypeName();
					if (typeName == CharOperation.NO_CHAR_CHAR) continue;
					existingImpl.add(CharOperation.toString(typeName));
				}
			}
			// use search infrastructure - faster than using model
			ImplSearchRequestor searchRequestor = new ImplSearchRequestor(this.completionToken, existingImpl);
			new SearchEngine(this.owner == null ? null : JavaModelManager.getJavaModelManager().getWorkingCopies(this.owner, true/*add primary WCs*/)).search(
					pattern,
					new SearchParticipant[] {SearchEngine.getDefaultSearchParticipant()},
					searchScope,
					searchRequestor,
					null
					);
			for (IType type : searchRequestor.types) {
				String pkg = type.getPackageFragment().getElementName();
				String name = type.getElementName();
				this.acceptType(pkg.toCharArray(), name.toCharArray(), CharOperation.NO_CHAR_CHAR, type.getFlags(), null);
				acceptTypes(scope);
			}
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(!this.requestor.isIgnored(CompletionProposal.PACKAGE_REF)) {
			checkCancel();
			findPackagesInCurrentModule();
		}
	}

	private void getAllTypesInHierarchy(ITypeHierarchy newTypeHierarchy, IType iClass, ArrayList<IType> allTypes) {
			allTypes.add(iClass);
			IType[] subclasses = newTypeHierarchy.getSubclasses(iClass);
			for (IType iType2 : subclasses) {
				getAllTypesInHierarchy(newTypeHierarchy,iType2,allTypes);

			}

	}

	private char[][] findVariableFromUnresolvedReference(LocalDeclaration variable, BlockScope scope, final char[][] discouragedNames) {
		final TypeReference type = variable.type;
		if(type != null &&
				type.resolvedType != null &&
				type.resolvedType.problemId() == ProblemReasons.NoError){

			final ArrayList proposedNames = new ArrayList();

			UnresolvedReferenceNameFinder.UnresolvedReferenceNameRequestor nameRequestor =
				new UnresolvedReferenceNameFinder.UnresolvedReferenceNameRequestor() {
					@Override
					public void acceptName(char[] name) {
						int relevance = computeBaseRelevance();
						relevance += computeRelevanceForInterestingProposal();
						relevance += computeRelevanceForCaseMatching(CompletionEngine.this.completionToken, name);
						relevance += R_NAME_FIRST_PREFIX;
						relevance += R_NAME_FIRST_SUFFIX;
						relevance += R_NAME_LESS_NEW_CHARACTERS;
						relevance += computeRelevanceForRestrictions(IAccessRule.K_ACCESSIBLE); // no access restriction for variable name

						// accept result
						CompletionEngine.this.noProposal = false;
						if(!CompletionEngine.this.requestor.isIgnored(CompletionProposal.VARIABLE_DECLARATION)) {
							InternalCompletionProposal proposal =  CompletionEngine.this.createProposal(CompletionProposal.VARIABLE_DECLARATION, CompletionEngine.this.actualCompletionPosition);
							proposal.setSignature(getSignature(type.resolvedType));
							proposal.setPackageName(type.resolvedType.qualifiedPackageName());
							proposal.setTypeName(type.resolvedType.qualifiedSourceName());
							proposal.setName(name);
							proposal.setCompletion(name);
							//proposal.setFlags(Flags.AccDefault);
							proposal.setReplaceRange(CompletionEngine.this.startPosition - CompletionEngine.this.offset, CompletionEngine.this.endPosition - CompletionEngine.this.offset);
							proposal.setTokenRange(CompletionEngine.this.tokenStart - CompletionEngine.this.offset, CompletionEngine.this.tokenEnd - CompletionEngine.this.offset);
							proposal.setRelevance(relevance);
							CompletionEngine.this.requestor.accept(proposal);
							if(DEBUG) {
								CompletionEngine.this.printDebug(proposal);
							}
						}
						proposedNames.add(name);
					}
				};

			ReferenceContext referenceContext = scope.referenceContext();
			if (referenceContext instanceof AbstractMethodDeclaration) {
				AbstractMethodDeclaration md = (AbstractMethodDeclaration)referenceContext;

				UnresolvedReferenceNameFinder nameFinder = new UnresolvedReferenceNameFinder(this);
				nameFinder.find(
						this.completionToken,
						md,
						variable.declarationSourceEnd + 1,
						discouragedNames,
						nameRequestor);
			} else if (referenceContext instanceof TypeDeclaration) {
				TypeDeclaration typeDeclaration = (TypeDeclaration) referenceContext;
				FieldDeclaration[] fields = typeDeclaration.fields;
				if (fields != null) {
					done : for (int i = 0; i < fields.length; i++) {
						if (fields[i] instanceof Initializer) {
							Initializer initializer = (Initializer) fields[i];
							if (initializer.bodyStart <= variable.sourceStart &&
									variable.sourceStart < initializer.bodyEnd) {
								UnresolvedReferenceNameFinder nameFinder = new UnresolvedReferenceNameFinder(this);
								nameFinder.find(
										this.completionToken,
										initializer,
										typeDeclaration.scope,
										variable.declarationSourceEnd + 1,
										discouragedNames,
										nameRequestor);
								break done;
							}
						}
					}
				}
			}

			int proposedNamesCount = proposedNames.size();
			if (proposedNamesCount > 0) {
				return (char[][])proposedNames.toArray(new char[proposedNamesCount][]);
			}
		}

		return null;
	}

	private void findVariableName(
			char[] token,
			char[] qualifiedPackageName,
			char[] qualifiedSourceName,
			char[] sourceName,
			final TypeBinding typeBinding,
			char[][] discouragedNames,
			final char[][] forbiddenNames,
			boolean forCollection,
			int dim,
			int kind){

		if(sourceName == null || sourceName.length == 0)
			return;

		// compute variable name for non base type
		final char[] displayName;
		if (!forCollection) {
			if (dim > 0){
				int l = qualifiedSourceName.length;
				displayName = new char[l+(2*dim)];
				System.arraycopy(qualifiedSourceName, 0, displayName, 0, l);
				for(int i = 0; i < dim; i++){
					displayName[l+(i*2)] = '[';
					displayName[l+(i*2)+1] = ']';
				}
			} else {
				displayName = qualifiedSourceName;
			}
		} else {
			displayName = typeBinding.qualifiedSourceName();
		}

		final char[] t = token;
		final char[] q = qualifiedPackageName;
		INamingRequestor namingRequestor = new INamingRequestor() {
			void accept(char[] name, int prefixAndSuffixRelevance, int reusedCharacters){
				int l = forbiddenNames == null ? 0 : forbiddenNames.length;
				for (int i = 0; i < l; i++) {
					if (CharOperation.equals(forbiddenNames[i], name, false)) return;
				}

				if (CharOperation.prefixEquals(t, name, false)) {
					int relevance = computeBaseRelevance();
					relevance += computeRelevanceForInterestingProposal();
					relevance += computeRelevanceForCaseMatching(t, name);
					relevance += prefixAndSuffixRelevance;
					if(reusedCharacters > 0) relevance += R_NAME_LESS_NEW_CHARACTERS;
					relevance += computeRelevanceForRestrictions(IAccessRule.K_ACCESSIBLE); // no access restriction for variable name

					// accept result
					CompletionEngine.this.noProposal = false;
					if(!CompletionEngine.this.requestor.isIgnored(CompletionProposal.VARIABLE_DECLARATION)) {
						InternalCompletionProposal proposal =  CompletionEngine.this.createProposal(CompletionProposal.VARIABLE_DECLARATION, CompletionEngine.this.actualCompletionPosition);
						proposal.setSignature(getSignature(typeBinding));
						proposal.setPackageName(q);
						proposal.setTypeName(displayName);
						proposal.setName(name);
						proposal.setCompletion(name);
						//proposal.setFlags(Flags.AccDefault);
						proposal.setReplaceRange(CompletionEngine.this.startPosition - CompletionEngine.this.offset, CompletionEngine.this.endPosition - CompletionEngine.this.offset);
						proposal.setTokenRange(CompletionEngine.this.tokenStart - CompletionEngine.this.offset, CompletionEngine.this.tokenEnd - CompletionEngine.this.offset);
						proposal.setRelevance(relevance);
						CompletionEngine.this.requestor.accept(proposal);
						if(DEBUG) {
							CompletionEngine.this.printDebug(proposal);
						}
					}
				}
			}

			@Override
			public void acceptNameWithoutPrefixAndSuffix(char[] name,int reusedCharacters) {
				accept(name, 0, reusedCharacters);
			}

			@Override
			public void acceptNameWithPrefix(char[] name, boolean isFirstPrefix, int reusedCharacters) {
				accept(name, isFirstPrefix ? R_NAME_FIRST_PREFIX :  R_NAME_PREFIX, reusedCharacters);
			}

			@Override
			public void acceptNameWithPrefixAndSuffix(char[] name, boolean isFirstPrefix, boolean isFirstSuffix, int reusedCharacters) {
				accept(
						name,
						(isFirstPrefix ? R_NAME_FIRST_PREFIX : R_NAME_PREFIX) + (isFirstSuffix ? R_NAME_FIRST_SUFFIX : R_NAME_SUFFIX),
						reusedCharacters);
			}
			@Override
			public void acceptNameWithSuffix(char[] name, boolean isFirstSuffix, int reusedCharacters) {
				accept(name, isFirstSuffix ? R_NAME_FIRST_SUFFIX : R_NAME_SUFFIX, reusedCharacters);
			}
		};

		InternalNamingConventions.suggestVariableNames(
				kind,
				InternalNamingConventions.BK_SIMPLE_TYPE_NAME,
				qualifiedSourceName,
				this.javaProject,
				dim,
				token,
				discouragedNames,
				true,
				namingRequestor);
	}

	// Helper method for private void findVariableNames(char[] name, TypeReference type )
	private void findVariableName(
			char[] token,
			char[] qualifiedPackageName,
			char[] qualifiedSourceName,
			char[] sourceName,
			final TypeBinding typeBinding,
			char[][] discouragedNames,
			final char[][] forbiddenNames,
			int dim,
			int kind){
		findVariableName(
				token,
				qualifiedPackageName,
				qualifiedSourceName,
				sourceName,
				typeBinding,
				discouragedNames,
				forbiddenNames,
				false,
				dim,
				kind);
	}
	private void findVariableNameForCollection(
			char[] token,
			char[] qualifiedPackageName,
			char[] qualifiedSourceName,
			char[] sourceName,
			final TypeBinding typeBinding,
			char[][] discouragedNames,
			final char[][] forbiddenNames,
			int kind){

		findVariableName(
				token,
				qualifiedPackageName,
				qualifiedSourceName,
				sourceName,
				typeBinding,
				discouragedNames,
				forbiddenNames,
				false,
				1,
				kind);
	}
	private void findVariableNames(char[] name, TypeReference type , char[][] discouragedNames, char[][] forbiddenNames, int kind){
		if(type != null &&
			type.resolvedType != null) {
			TypeBinding tb = type.resolvedType;

			if (tb.problemId() == ProblemReasons.NoError &&
					TypeBinding.notEquals(tb, Scope.getBaseType(VOID))) {
				findVariableName(
					name,
					tb.leafComponentType().qualifiedPackageName(),
					tb.leafComponentType().qualifiedSourceName(),
					tb.leafComponentType().sourceName(),
					tb,
					discouragedNames,
					forbiddenNames,
					type.dimensions(),
					kind);

				if (tb.isParameterizedType() &&
						tb.findSuperTypeOriginatingFrom(TypeIds.T_JavaUtilCollection, false) != null) {
					ParameterizedTypeBinding ptb = ((ParameterizedTypeBinding) tb);
					TypeBinding[] arguments = ptb.arguments;
					if (arguments != null && arguments.length == 1) {
						TypeBinding argument = arguments[0];
						findVariableNameForCollection(
							name,
							argument.leafComponentType().qualifiedPackageName(),
							argument.leafComponentType().qualifiedSourceName(),
							argument.leafComponentType().sourceName(),
							tb,
							discouragedNames,
							forbiddenNames,
							kind);
					}
				}
			}
		}

	}
	private void findVariablesAndMethods(
		char[] token,
		Scope scope,
		InvocationSite invocationSite,
		Scope invocationScope,
		boolean insideTypeAnnotation,
		boolean insideAnnotationAttribute) {
		findVariablesAndMethods(token, scope, invocationSite, invocationScope, insideTypeAnnotation, insideAnnotationAttribute, true, new ObjectVector());
	}
	private void findVariablesAndMethods(
		char[] token,
		Scope scope,
		InvocationSite invocationSite,
		Scope invocationScope,
		boolean insideTypeAnnotation,
		boolean insideAnnotationAttribute,
		boolean canBePrefixed,
		ObjectVector methodsFound) {

		if (token == null)
			return;

		// Should local variables hide fields from the receiver type or any of its enclosing types?
		// we know its an implicit field/method access... see BlockScope getBinding/getImplicitMethod

		boolean staticsOnly = false;
		// need to know if we're in a static context (or inside a constructor)
		int tokenLength = token.length;

		ObjectVector localsFound = new ObjectVector();
		ObjectVector fieldsFound = new ObjectVector();

		Scope currentScope = scope;

		if (!this.requestor.isIgnored(CompletionProposal.LOCAL_VARIABLE_REF)) {
			done1 : while (true) { // done when a COMPILATION_UNIT_SCOPE is found

				switch (currentScope.kind) {

					case Scope.METHOD_SCOPE :
						// handle the error case inside an explicit constructor call (see MethodScope>>findField)
						MethodScope methodScope = (MethodScope) currentScope;
						staticsOnly |= methodScope.isStatic | methodScope.isConstructorCall;

					//$FALL-THROUGH$
					case Scope.BLOCK_SCOPE :
						BlockScope blockScope = (BlockScope) currentScope;

						next : for (int i = 0, length = blockScope.locals.length; i < length; i++) {
							LocalVariableBinding local = blockScope.locals[i];

							if (local == null)
								break next;

							if (tokenLength > local.name.length)
								continue next;

							if (isFailedMatch(token, local.name))
								continue next;

							if (local.isSecret())
								continue next;

							// https://bugs.eclipse.org/bugs/show_bug.cgi?id=328674
							if (local.declaration.initialization != null && !local.declaration.type.isTypeNameVar(null)) {
								// proposal being asked inside field's initialization. Don't propose this field.
								continue next;
							}

							// don't propose non constant variables or strings (1.6 or below) in case expression
							// https://bugs.eclipse.org/bugs/show_bug.cgi?id=195346
							// https://bugs.eclipse.org/bugs/show_bug.cgi?id=343342
							if (this.assistNodeIsInsideCase) {
								if (local.isFinal()) {
									if (this.assistNodeIsString){
										if (local.type == null || local.type.id != TypeIds.T_JavaLangString)
											continue next;
									} else if (!(local.type instanceof BaseTypeBinding))
										continue next;
								} else {
									continue next; // non-constants not allowed in case.
								}
							}

							int ptr = this.uninterestingBindingsPtr;
							// Cases where the binding is uninteresting eg. for completion occurring inside a local var
							// declaration, the local var binding is uninteresting and shouldn't be proposed.
							while (ptr >= 0) {
								if (this.uninterestingBindings[ptr] == local) {
									continue next;
								}
								ptr--;
							}

							for (int f = 0; f < localsFound.size; f++) {
								LocalVariableBinding otherLocal =
									(LocalVariableBinding) localsFound.elementAt(f);
								if (CharOperation.equals(otherLocal.name, local.name, true))
									continue next;
							}
							localsFound.add(local);

							int relevance = computeBaseRelevance();
							relevance += computeRelevanceForResolution();
							relevance += computeRelevanceForInterestingProposal(local);
							relevance += computeRelevanceForCaseMatching(token, local.name);
							int computeRelevanceForExpectingType = computeRelevanceForExpectingType(local.type);
							if(this.strictMatchForExtepectedType && computeRelevanceForExpectingType <= 0) {
								continue;
							} else if(!this.strictMatchForExtepectedType) {
								relevance += computeRelevanceForExpectingType;
							}

							relevance += computeRelevanceForEnumConstant(local.type);
							relevance += computeRelevanceForQualification(false);
							relevance += computeRelevanceForRestrictions(IAccessRule.K_ACCESSIBLE); // no access restriction for local variable
							relevance += computeRelevanceForFinal(this.assistNodeIsInsideCase, local.isFinal());
							this.noProposal = false;
							if(!this.requestor.isIgnored(CompletionProposal.LOCAL_VARIABLE_REF)) {
								InternalCompletionProposal proposal =  createProposal(CompletionProposal.LOCAL_VARIABLE_REF, this.actualCompletionPosition);
								proposal.setSignature(
									local.type == null
									? createTypeSignature(
											CharOperation.NO_CHAR,
											local.declaration.type.toString().toCharArray())
									: getSignature(local.type));
								if(local.type == null) {
									//proposal.setPackageName(null);
									proposal.setTypeName(local.declaration.type.toString().toCharArray());
								} else {
									proposal.setPackageName(local.type.qualifiedPackageName());
									proposal.setTypeName(local.type.qualifiedSourceName());
								}
								proposal.setName(local.name);
								proposal.setCompletion(local.name);
								proposal.setFlags(local.modifiers);
								proposal.setReplaceRange(this.startPosition - this.offset, this.endPosition - this.offset);
								proposal.setTokenRange(this.tokenStart - this.offset, this.tokenEnd - this.offset);
								proposal.setRelevance(relevance);
								this.requestor.accept(proposal);
								if(DEBUG) {
									this.printDebug(proposal);
								}
							}
						}
						break;

					case Scope.COMPILATION_UNIT_SCOPE :
						break done1;
				}
				currentScope = currentScope.parent;
			}
		}

		checkCancel();

		boolean proposeField = !this.requestor.isIgnored(CompletionProposal.FIELD_REF);
		boolean proposeMethod = !this.requestor.isIgnored(CompletionProposal.METHOD_REF);

		staticsOnly = false;
		currentScope = scope;

		if(proposeField || proposeMethod) {
			done2 : while (true) { // done when a COMPILATION_UNIT_SCOPE is found

				switch (currentScope.kind) {
					case Scope.METHOD_SCOPE :
						// handle the error case inside an explicit constructor call (see MethodScope>>findField)
						MethodScope methodScope = (MethodScope) currentScope;
						staticsOnly |= methodScope.isStatic | methodScope.isConstructorCall;
						break;
					case Scope.CLASS_SCOPE :
						ClassScope classScope = (ClassScope) currentScope;
						SourceTypeBinding enclosingType = classScope.referenceContext.binding;
						/*				if (tokenLength == 0) { // only search inside the type itself if no prefix was provided
											findFields(token, enclosingType.fields(), classScope, fieldsFound, staticsOnly);
											findMethods(token, enclosingType.methods(), classScope, methodsFound, staticsOnly, false);
											break done;
										} else { */
						if(!insideTypeAnnotation) {
							if(proposeField) {
								findFields(
									token,
									enclosingType,
									classScope,
									fieldsFound,
									localsFound,
									staticsOnly,
									invocationSite,
									invocationScope,
									true,
									canBePrefixed,
									null,
									null,
									null,
									false,
									null,
									-1,
									-1);
							}
							if(proposeMethod && !insideAnnotationAttribute) {
								findMethods(
									token,
									null,
									null,
									enclosingType,
									classScope,
									methodsFound,
									staticsOnly,
									false,
									invocationSite,
									invocationScope,
									true,
									false,
									canBePrefixed,
									null,
									null,
									null,
									false,
									null,
									-1,
									-1);
							}
						}
						staticsOnly |= enclosingType.isStatic();
						insideTypeAnnotation = false;
						//				}
						break;

					case Scope.COMPILATION_UNIT_SCOPE :
						break done2;
				}
				currentScope = currentScope.parent;
			}

			checkCancel();

			findFieldsAndMethodsFromStaticImports(
					token,
					scope,
					invocationSite,
					invocationScope,
					false,
					insideAnnotationAttribute,
					localsFound,
					fieldsFound,
					methodsFound,
					proposeField,
					proposeMethod);

			if (this.assistNodeInJavadoc == 0) {

				checkCancel();

				// search in favorites import
				findFieldsAndMethodsFromFavorites(
						token,
						scope,
						invocationSite,
						invocationScope,
						localsFound,
						fieldsFound,
						methodsFound);
			}

			checkCancel();

			findEnumConstantsFromExpectedTypes(
					token,
					invocationScope,
					fieldsFound);

			findLambdaExpressions(currentScope);
		}
	}

	private char[] getCompletedTypeSignature(ReferenceBinding referenceBinding) {
		char[] result = null;
		StringBuilder sig = new StringBuilder(10);
		if (!referenceBinding.isMemberType()) {
			char[] typeSig = referenceBinding.genericTypeSignature();
			sig.append(typeSig, 0, typeSig.length);
		} else if (!this.insideQualifiedReference) {
			if (referenceBinding.isStatic()) {
				char[] typeSig = referenceBinding.signature();
				sig.append(typeSig, 0, typeSig.length-1); // copy all but trailing semicolon

				TypeVariableBinding[] typeVariables = referenceBinding.typeVariables();
				if (typeVariables != Binding.NO_TYPE_VARIABLES) {
				    sig.append(Signature.C_GENERIC_START);
				    for (int i = 0, length = typeVariables.length; i < length; i++) {
				        sig.append(typeVariables[i].genericTypeSignature());
				    }
				    sig.append(Signature.C_GENERIC_END);
				}
				sig.append(Signature.C_SEMICOLON);
			} else {
				char[] typeSig = referenceBinding.genericTypeSignature();
				sig.append(typeSig, 0, typeSig.length);
			}
		} else {
			ReferenceBinding enclosingType = referenceBinding.enclosingType();
			if (enclosingType.isParameterizedType()) {
				char[] typeSig = referenceBinding.genericTypeSignature();
				sig.append(typeSig, 0, typeSig.length-1);

				TypeVariableBinding[] typeVariables = referenceBinding.typeVariables();
				if (typeVariables != Binding.NO_TYPE_VARIABLES) {
				    sig.append(Signature.C_GENERIC_START);
				    for (int i = 0, length = typeVariables.length; i < length; i++) {
				        sig.append(typeVariables[i].genericTypeSignature());
				    }
				    sig.append(Signature.C_GENERIC_END);
				}
			} else {
				char[] typeSig = referenceBinding.signature();
				sig.append(typeSig, 0, typeSig.length-1); // copy all but trailing semicolon

				if (referenceBinding.isStatic()) {
					TypeVariableBinding[] typeVariables = referenceBinding.typeVariables();
					if (typeVariables != Binding.NO_TYPE_VARIABLES) {
					    sig.append(Signature.C_GENERIC_START);
					    for (int i = 0, length = typeVariables.length; i < length; i++) {
					        sig.append(typeVariables[i].genericTypeSignature());
					    }
					    sig.append(Signature.C_GENERIC_END);
					}
				}
			}
			sig.append(Signature.C_SEMICOLON);
		}
		int sigLength = sig.length();
		result = new char[sigLength];
		sig.getChars(0, sigLength, result, 0);
		result = CharOperation.replaceOnCopy(result, '/', Signature.C_DOT);
		return result;
	}

	private ImportBinding[] getFavoriteReferenceBindings(Scope scope) {
		if (this.favoriteReferenceBindings != null) return this.favoriteReferenceBindings;

		String[] favoriteReferences = this.requestor.getFavoriteReferences();

		if (favoriteReferences == null || favoriteReferences.length == 0) return null;

		ImportBinding[] resolvedImports = new ImportBinding[favoriteReferences.length];

		int count = 0;
		next : for (int i = 0; i < favoriteReferences.length; i++) {
			String favoriteReference = favoriteReferences[i];

			int length;
			if (favoriteReference == null || (length = favoriteReference.length()) == 0) continue next;

			boolean onDemand = favoriteReference.charAt(length - 1) == '*';

			char[][] compoundName = CharOperation.splitOn('.', favoriteReference.toCharArray());
			if (onDemand) {
				compoundName = CharOperation.subarray(compoundName, 0, compoundName.length - 1);
			}

			// remove duplicate and conflicting
			for (int j = 0; j < count; j++) {
				ImportReference f = resolvedImports[j].reference;

				if (CharOperation.equals(f.tokens, compoundName)) continue next;

				if (!onDemand && ((f.bits & ASTNode.OnDemand) == 0)) {
					if (CharOperation.equals(f.tokens[f.tokens.length - 1], compoundName[compoundName.length - 1]))
						continue next;
				}
			}

			boolean isStatic = true;

			ImportReference importReference =
				new ImportReference(
						compoundName,
						new long[compoundName.length],
						onDemand,
						isStatic ? ClassFileConstants.AccStatic : ClassFileConstants.AccDefault);

			Binding importBinding = this.unitScope.findImport(compoundName, isStatic, onDemand);

			if (!importBinding.isValidBinding()) {
				continue next;
			}

			if (importBinding instanceof PackageBinding) {
				continue next;
			}

			resolvedImports[count++] =
				new ImportBinding(compoundName, onDemand, importBinding, importReference);
		}

		if (resolvedImports.length > count)
			System.arraycopy(resolvedImports, 0, resolvedImports = new ImportBinding[count], 0, count);

		return this.favoriteReferenceBindings = resolvedImports;
	}

	private INameEnvironment getNoCacheNameEnvironment() {
		if (this.noCacheNameEnvironment == null) {
			JavaModelManager.getJavaModelManager().cacheZipFiles(this);
			this.noCacheNameEnvironment = IndexBasedJavaSearchEnvironment.create(Collections.singletonList(this.javaProject), this.owner == null ? null : JavaModelManager.getJavaModelManager().getWorkingCopies(this.owner, true/*add primary WCs*/));
		}
		return this.noCacheNameEnvironment;
	}

	@Override
	public AssistParser getParser() {

		return this.parser;
	}
	protected boolean hasArrayTypeAsExpectedSuperTypes() {
		if ((this.expectedTypesFilter & ~SUBTYPE) != 0) return false;

		if (!this.hasComputedExpectedArrayTypes) {
			if(this.expectedTypes != null) {
				done : for (int i = 0; i <= this.expectedTypesPtr; i++) {
					if(this.expectedTypes[i].isArrayType()) {
						this.hasExpectedArrayTypes = true;
						break done;
					}
				}
			}

			this.hasComputedExpectedArrayTypes = true;
		}

		return this.hasExpectedArrayTypes;
	}
	protected boolean hasPossibleAnnotationTarget(TypeBinding typeBinding, Scope scope) {
		if (this.targetedElement == TagBits.AnnotationForPackage) {
			long target = typeBinding.getAnnotationTagBits() & TagBits.AnnotationTargetMASK;
			if(target != 0 && (target & TagBits.AnnotationForPackage) == 0) {
				return false;
			}
		} else if (this.targetedElement == TagBits.AnnotationForModule) {
			long target = typeBinding.getAnnotationTagBits() & TagBits.AnnotationTargetMASK;
			if(target != 0 && (target & TagBits.AnnotationForModule) == 0) {
				return false;
			}
		} else if ((this.targetedElement & (TagBits.AnnotationForType | TagBits.AnnotationForTypeUse)) != 0) {
			if (scope.parent != null &&
					scope.parent.parent != null &&
					scope.parent.referenceContext() instanceof CompletionOnAnnotationOfType &&
					scope.parent.parent instanceof CompilationUnitScope) {
				long target = typeBinding.getAnnotationTagBits() & TagBits.AnnotationTargetMASK;
				if ((this.targetedElement & TagBits.AnnotationForAnnotationType) != 0) {
					if(target != 0 && (target &(TagBits.AnnotationForType | TagBits.AnnotationForAnnotationType | TagBits.AnnotationForTypeUse)) == 0) {
						return false;
					}
				} else {
					if (target != 0 && (target & (TagBits.AnnotationForType | TagBits.AnnotationForTypeUse)) == 0) {
						return false;
					}
				}
			}
		}
		return true;
	}
	/**
	 * Returns completion string inserted inside a specified inline tag.
	 * @param completionName
	 * @return char[] Completion text inclunding specified inline tag
	 */
	private char[] inlineTagCompletion(char[] completionName, char[] inlineTag) {
		int tagLength= inlineTag.length;
		int completionLength = completionName.length;
		int inlineLength = 2+tagLength+1+completionLength+1;
		char[] inlineCompletion = new char[inlineLength];
		inlineCompletion[0] = '{';
		inlineCompletion[1] = '@';
		System.arraycopy(inlineTag, 0, inlineCompletion, 2, tagLength);
		inlineCompletion[tagLength+2] = ' ';
		System.arraycopy(completionName, 0, inlineCompletion, tagLength+3, completionLength);
		// do not add space at end of inline tag (see bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=121026)
		//inlineCompletion[inlineLength-2] = ' ';
		inlineCompletion[inlineLength-1] = '}';
		return inlineCompletion;
	}
	private boolean isAllowingLongComputationProposals() {
		return this.monitor != null;
	}

	/**
	 * Checks whether name matches the token according to the current
	 * code completion settings (substring match, camel case match etc.)
	 * and sets whether the current match is a suffix proposal.
	 *
	 * @param token the token that is tested
	 * @param name the name to match
	 * @return <code>true</code> if the token does not match,
	 * <code>false</code> otherwise
	 */
	private boolean isFailedMatch(char[] token, char[] name) {
		if ((this.options.substringMatch && CharOperation.substringMatch(token, name))
				|| (this.options.camelCaseMatch && CharOperation.camelCaseMatch(token, name))
				|| CharOperation.prefixEquals(token, name, false)
				|| (this.options.subwordMatch && CharOperation.subWordMatch(token, name))) {
			return false;
		}

		return true;
	}
	private boolean isForbidden(ReferenceBinding binding) {
		for (int i = 0; i <= this.forbbidenBindingsPtr; i++) {
			if(this.forbbidenBindings[i] == binding) {
				return true;
			}
		}
		if (!isValidPackageName(binding.qualifiedPackageName())) {
			return true;
		}
		return false;
	}

	private boolean isForbidden(char[] givenPkgName, char[] givenTypeName, char[][] enclosingTypeNames) {
		// CharOperation.concatWith() handles the cases where input args are null/empty
		char[] fullTypeName = CharOperation.concatWith(enclosingTypeNames, givenTypeName, '.');
		for (int i = 0; i <= this.forbbidenBindingsPtr; i++) {
			if (this.forbbidenBindings[i] instanceof TypeBinding) {
				TypeBinding typeBinding = (TypeBinding) this.forbbidenBindings[i];
				char[] currPkgName = typeBinding.qualifiedPackageName();
				if (CharOperation.equals(givenPkgName, currPkgName))	{
					char[] currTypeName = typeBinding.qualifiedSourceName();
					if (CharOperation.equals(fullTypeName, currTypeName)) {
						return true;
					}
				}
			}
		}

		if (!isValidPackageName(givenPkgName)) {
			return true;
		}

		return false;
	}

	private boolean isIgnored(int kind) {
		return this.requestor.isIgnored(kind);
	}
	boolean isIgnored(int kind, boolean missingTypes) {
		return this.requestor.isIgnored(kind) ||
			(missingTypes && !this.requestor.isAllowingRequiredProposals(kind, CompletionProposal.TYPE_REF));
	}

	private boolean isIgnored(int kind, int requiredProposalKind) {
		return this.requestor.isIgnored(kind) ||
			!this.requestor.isAllowingRequiredProposals(kind, requiredProposalKind);
	}

	private boolean isValidPackageName(char[] packageName) {
		if (this.validPackageNames.includes(packageName)) {
			return true;
		}

		if (this.invalidPackageNames.includes(packageName)) {
			return false;
		}

		char[][] names = CharOperation.splitOn('.', packageName);
		for (int i = 0, length = names.length; i < length; i++) {
			if (!Util.isValidFolderNameForPackage(new String(names[i]), this.sourceLevel, this.complianceLevel)) {
				this.invalidPackageNames.add(packageName);
				return false;
			}
		}

		this.validPackageNames.add(packageName);
		return true;
	}

	private boolean isValidParent(ASTNode parent, ASTNode node, Scope scope){

		if(parent instanceof ParameterizedSingleTypeReference) {
			ParameterizedSingleTypeReference ref = (ParameterizedSingleTypeReference) parent;
			TypeVariableBinding[] typeVariables = ((ReferenceBinding)ref.resolvedType).typeVariables();
			int length = ref.typeArguments == null ? 0 : ref.typeArguments.length;
			int nodeIndex = -1;
			for(int i = length - 1 ; i > -1 ; i--) {
				if(node == ref.typeArguments[i]) {
					nodeIndex = i;
					break;
				}
			}
			if(nodeIndex > -1 && (typeVariables == null || typeVariables.length < nodeIndex + 1)) {
				TypeBinding[] typeBindings = new TypeBinding[nodeIndex + 1];
				for(int i = 0; i < nodeIndex; i++) {
					typeBindings[i] = ref.typeArguments[i].resolvedType;
				}
				typeBindings[nodeIndex] = scope.getJavaLangObject();
				if(typeVariables == null || typeVariables.length == 0) {
					scope.problemReporter().nonGenericTypeCannotBeParameterized(0, ref, ref.resolvedType, typeBindings);
				} else {
					scope.problemReporter().incorrectArityForParameterizedType(ref, ref.resolvedType, typeBindings);
				}
				return false;
			}
		} else if(parent instanceof ParameterizedQualifiedTypeReference) {
			ParameterizedQualifiedTypeReference ref = (ParameterizedQualifiedTypeReference) parent;
			TypeVariableBinding[] typeVariables = ((ReferenceBinding)ref.resolvedType).typeVariables();
			TypeReference[][] arguments = ref.typeArguments;
			int iLength = arguments == null ? 0 : arguments.length;
			for (int i = 0; i < iLength; i++) {
				int jLength = arguments[i] == null ? 0 : arguments[i].length;
				for (int j = 0; j < jLength; j++) {
					if(arguments[i][j] == node && (typeVariables == null || typeVariables.length <= j)) {
						TypeBinding[] typeBindings = new TypeBinding[j + 1];
						for(int k = 0; k < j; k++) {
							typeBindings[k] = ref.typeArguments[i][k].resolvedType;
						}
						typeBindings[j] = scope.getJavaLangObject();
						if(typeVariables == null || typeVariables.length == 0) {
							scope.problemReporter().nonGenericTypeCannotBeParameterized(0, ref, ref.resolvedType, typeBindings);
						} else {
							scope.problemReporter().incorrectArityForParameterizedType(ref, ref.resolvedType, typeBindings);
						}
						return false;
					}
				}
			}
		} else if (parent instanceof QualifiedAllocationExpression) {
			Expression enclosingInstance = ((QualifiedAllocationExpression) parent).enclosingInstance();
			if (enclosingInstance instanceof AllocationExpression) {
				AllocationExpression enclosingAllocation = (AllocationExpression) enclosingInstance;
				if (enclosingAllocation.type != null && (enclosingAllocation.type.bits & ASTNode.IsDiamond) != 0) {
					if (enclosingAllocation.binding != null) {
						for (TypeBinding param : enclosingAllocation.binding.original().parameters) {
							if (param.kind() == Binding.TYPE_PARAMETER)
								return true; // this could possible help type inference, let's assume the best
						}
						scope.problemReporter().cannotInferElidedTypes(enclosingAllocation);
						return false; // will fail type inference, hence no useful proposals can be made
					} else if (enclosingAllocation.arguments == null) {
						scope.problemReporter().cannotInferElidedTypes(enclosingAllocation);
						return false; // will fail type inference, hence no useful proposals can be made
					}
				}
			}
		}
		return true;
	}
	private boolean mustQualifyType(ReferenceBinding type, char[] packageName, Scope scope) {
		if(!mustQualifyType(
				packageName,
				type.sourceName(),
				type.isMemberType() ? type.enclosingType().qualifiedSourceName() : null,
				type.modifiers)) {
			return false;
		}
		ReferenceBinding enclosingType = scope.enclosingSourceType();
		while (enclosingType != null) {
			ReferenceBinding currentType = enclosingType;
			while (currentType != null) {
				ReferenceBinding[] memberTypes = currentType.memberTypes();
				if(memberTypes != null) {
					for (int i = 0; i < memberTypes.length; i++) {
						if (CharOperation.equals(memberTypes[i].sourceName, type.sourceName()) &&
								memberTypes[i].canBeSeenBy(scope)) {
							return TypeBinding.notEquals(memberTypes[i], type);
						}
					}
				}
				currentType = currentType.superclass();
			}
			enclosingType = enclosingType.enclosingType();
		}
		return true;
	}
	private Initializer parseSnippeInitializer(char[] snippet, int position, char[][] localVariableTypeNames, char[][] localVariableNames, int[] localVariableModifiers, boolean isStatic){
		StringBuffer prefix = new StringBuffer();
		prefix.append("public class FakeType {\n "); //$NON-NLS-1$
		if(isStatic) {
			prefix.append("static "); //$NON-NLS-1$
		}
		prefix.append("{\n"); //$NON-NLS-1$
		for (int i = 0; i < localVariableTypeNames.length; i++) {
			ASTNode.printModifiers(localVariableModifiers[i], prefix);
			prefix.append(' ');
			prefix.append(localVariableTypeNames[i]);
			prefix.append(' ');
			prefix.append(localVariableNames[i]);
			prefix.append(';');
		}

		char[] fakeSource = CharOperation.concat(prefix.toString().toCharArray(), snippet, "}}".toCharArray());//$NON-NLS-1$
		this.offset = prefix.length();

		String encoding = this.compilerOptions.defaultEncoding;
		@SuppressWarnings("deprecation")
		BasicCompilationUnit fakeUnit = new BasicCompilationUnit(
			fakeSource,
			null,
			"FakeType.java", //$NON-NLS-1$
			encoding);

		this.actualCompletionPosition = prefix.length() + position - 1;

		CompilationResult fakeResult = new CompilationResult(fakeUnit, 1, 1, this.compilerOptions.maxProblemsPerUnit);
		CompilationUnitDeclaration fakeAST = this.parser.dietParse(fakeUnit, fakeResult, this.actualCompletionPosition);

		parseBlockStatements(fakeAST, this.actualCompletionPosition);

		return (Initializer)fakeAST.types[0].fields[0];
	}
	protected void printDebug(CategorizedProblem error) {
		if(CompletionEngine.DEBUG) {
			System.out.print("COMPLETION - completionFailure("); //$NON-NLS-1$
			System.out.print(error);
			System.out.println(")"); //$NON-NLS-1$
		}
	}
	protected void printDebug(CompletionProposal proposal){
		StringBuffer buffer = new StringBuffer();
		printDebug(proposal, 0, buffer);
		System.out.println(buffer.toString());
	}

	private void printDebug(CompletionProposal proposal, int tab, StringBuffer buffer){
		printDebugTab(tab, buffer);
		buffer.append("COMPLETION - "); //$NON-NLS-1$
		switch(proposal.getKind()) {
			case CompletionProposal.ANONYMOUS_CLASS_DECLARATION :
				buffer.append("ANONYMOUS_CLASS_DECLARATION"); //$NON-NLS-1$
				break;
			case CompletionProposal.FIELD_REF :
				buffer.append("FIELD_REF"); //$NON-NLS-1$
				break;
			case CompletionProposal.FIELD_REF_WITH_CASTED_RECEIVER :
				buffer.append("FIELD_REF_WITH_CASTED_RECEIVER"); //$NON-NLS-1$
				break;
			case CompletionProposal.KEYWORD :
				buffer.append("KEYWORD"); //$NON-NLS-1$
				break;
			case CompletionProposal.LABEL_REF :
				buffer.append("LABEL_REF"); //$NON-NLS-1$
				break;
			case CompletionProposal.LOCAL_VARIABLE_REF :
				buffer.append("LOCAL_VARIABLE_REF"); //$NON-NLS-1$
				break;
			case CompletionProposal.METHOD_DECLARATION :
				buffer.append("METHOD_DECLARATION"); //$NON-NLS-1$
				break;
			case CompletionProposal.METHOD_REF :
				buffer.append("METHOD_REF"); //$NON-NLS-1$
				break;
			case CompletionProposal.METHOD_REF_WITH_CASTED_RECEIVER :
				buffer.append("METHOD_REF_WITH_CASTED_RECEIVER"); //$NON-NLS-1$
				break;
			case CompletionProposal.MODULE_DECLARATION :
				buffer.append("MODULE_DECLARATION"); //$NON-NLS-1$
				break;
			case CompletionProposal.MODULE_REF :
				buffer.append("MODULE_REF"); //$NON-NLS-1$
				break;
			case CompletionProposal.PACKAGE_REF :
				buffer.append("PACKAGE_REF"); //$NON-NLS-1$
				break;
			case CompletionProposal.TYPE_REF :
				buffer.append("TYPE_REF"); //$NON-NLS-1$
				break;
			case CompletionProposal.VARIABLE_DECLARATION :
				buffer.append("VARIABLE_DECLARATION"); //$NON-NLS-1$
				break;
			case CompletionProposal.POTENTIAL_METHOD_DECLARATION :
				buffer.append("POTENTIAL_METHOD_DECLARATION"); //$NON-NLS-1$
				break;
			case CompletionProposal.METHOD_NAME_REFERENCE :
				buffer.append("METHOD_NAME_REFERENCE"); //$NON-NLS-1$
				break;
			case CompletionProposal.ANNOTATION_ATTRIBUTE_REF :
				buffer.append("ANNOTATION_ATTRIBUT_REF"); //$NON-NLS-1$
				break;
			case CompletionProposal.FIELD_IMPORT :
				buffer.append("FIELD_IMPORT"); //$NON-NLS-1$
				break;
			case CompletionProposal.METHOD_IMPORT :
				buffer.append("METHOD_IMPORT"); //$NON-NLS-1$
				break;
			case CompletionProposal.TYPE_IMPORT :
				buffer.append("TYPE_IMPORT"); //$NON-NLS-1$
				break;
			case CompletionProposal.CONSTRUCTOR_INVOCATION :
				buffer.append("CONSTRUCTOR_INVOCATION"); //$NON-NLS-1$
				break;
			case CompletionProposal.ANONYMOUS_CLASS_CONSTRUCTOR_INVOCATION :
				buffer.append("ANONYMOUS_CLASS_CONSTRUCTOR_INVOCATION"); //$NON-NLS-1$
				break;
			case CompletionProposal.LAMBDA_EXPRESSION :
				buffer.append("LAMBDA_EXPRESSION"); //$NON-NLS-1$
				break;
			default :
				buffer.append("PROPOSAL"); //$NON-NLS-1$
				break;

		}

		buffer.append("{\n");//$NON-NLS-1$
		printDebugTab(tab, buffer);
		buffer.append("\tCompletion[").append(proposal.getCompletion() == null ? "null".toCharArray() : proposal.getCompletion()).append("]\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		printDebugTab(tab, buffer);
		buffer.append("\tDeclarationSignature[").append(proposal.getDeclarationSignature() == null ? "null".toCharArray() : proposal.getDeclarationSignature()).append("]\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		printDebugTab(tab, buffer);
		buffer.append("\tDeclarationKey[").append(proposal.getDeclarationKey() == null ? "null".toCharArray() : proposal.getDeclarationKey()).append("]\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		printDebugTab(tab, buffer);
		buffer.append("\tSignature[").append(proposal.getSignature() == null ? "null".toCharArray() : proposal.getSignature()).append("]\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		printDebugTab(tab, buffer);
		buffer.append("\tKey[").append(proposal.getKey() == null ? "null".toCharArray() : proposal.getKey()).append("]\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		printDebugTab(tab, buffer);
		buffer.append("\tName[").append(proposal.getName() == null ? "null".toCharArray() : proposal.getName()).append("]\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		printDebugTab(tab, buffer);
		buffer.append("\tFlags[");//$NON-NLS-1$
		int flags = proposal.getFlags();
		buffer.append(Flags.toString(flags));
		if((flags & Flags.AccInterface) != 0) buffer.append("interface ");//$NON-NLS-1$
		if((flags & Flags.AccEnum) != 0) buffer.append("enum ");//$NON-NLS-1$
		buffer.append("]\n"); //$NON-NLS-1$

		CompletionProposal[] proposals = proposal.getRequiredProposals();
		if(proposals != null) {
			printDebugTab(tab, buffer);
			buffer.append("\tRequiredProposals[");//$NON-NLS-1$
			for (int i = 0; i < proposals.length; i++) {
				buffer.append("\n"); //$NON-NLS-1$
				printDebug(proposals[i], tab + 2, buffer);
			}
			printDebugTab(tab, buffer);
			buffer.append("\n\t]\n"); //$NON-NLS-1$
		}

		printDebugTab(tab, buffer);
		buffer.append("\tCompletionLocation[").append(proposal.getCompletionLocation()).append("]\n"); //$NON-NLS-1$ //$NON-NLS-2$
		int start = proposal.getReplaceStart();
		int end = proposal.getReplaceEnd();
		printDebugTab(tab, buffer);
		buffer.append("\tReplaceStart[").append(start).append("]"); //$NON-NLS-1$ //$NON-NLS-2$
		buffer.append("-ReplaceEnd[").append(end).append("]\n"); //$NON-NLS-1$ //$NON-NLS-2$
		start = proposal.getTokenStart();
		end = proposal.getTokenEnd();
		printDebugTab(tab, buffer);
		buffer.append("\tTokenStart[").append(start).append("]"); //$NON-NLS-1$ //$NON-NLS-2$
		buffer.append("-TokenEnd[").append(end).append("]\n"); //$NON-NLS-1$ //$NON-NLS-2$
		if (this.source != null) {
			printDebugTab(tab, buffer);
			buffer.append("\tReplacedText[").append(this.source, start, end-start).append("]\n"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		printDebugTab(tab, buffer);
		buffer.append("\tTokenStart[").append(proposal.getTokenStart()).append("]"); //$NON-NLS-1$ //$NON-NLS-2$
		buffer.append("-TokenEnd[").append(proposal.getTokenEnd()).append("]\n"); //$NON-NLS-1$ //$NON-NLS-2$
		printDebugTab(tab, buffer);
		buffer.append("\tRelevance[").append(proposal.getRelevance()).append("]\n"); //$NON-NLS-1$ //$NON-NLS-2$

		printDebugTab(tab, buffer);
		buffer.append("}\n");//$NON-NLS-1$
	}

	private void printDebugTab(int tab, StringBuffer buffer) {
		for (int i = 0; i < tab; i++) {
			buffer.append('\t');
		}
	}

	private void proposeConstructor(AcceptedConstructor deferredProposal, Scope scope) {
		if (deferredProposal.proposeConstructor) {
			proposeConstructor(
					deferredProposal.simpleTypeName,
					deferredProposal.parameterCount,
					deferredProposal.signature,
					deferredProposal.parameterTypes,
					deferredProposal.parameterNames,
					deferredProposal.modifiers,
					deferredProposal.packageName,
					deferredProposal.typeModifiers,
					deferredProposal.accessibility,
					deferredProposal.simpleTypeName,
					deferredProposal.fullyQualifiedName,
					deferredProposal.mustBeQualified,
					scope,
					deferredProposal.extraFlags);
		}
	}

	private void proposeConstructor(
			char[] simpleTypeName,
			int parameterCount,
			char[] signature,
			char[][] parameterTypes,
			char[][] parameterNames,
			int modifiers,
			char[] packageName,
			int typeModifiers,
			int accessibility,
			char[] typeName,
			char[] fullyQualifiedName,
			boolean isQualified,
			Scope scope,
			int extraFlags) {
		char[] typeCompletion = fullyQualifiedName;
		if(isQualified) {
			if (packageName == null || packageName.length == 0)
				if (this.unitScope != null && this.unitScope.fPackage.compoundName != CharOperation.NO_CHAR_CHAR)
					return; // ignore types from the default package from outside it
		} else {
			typeCompletion = simpleTypeName;
		}

		ReferenceBinding typeBinding = this.lookupEnvironment.getType(CharOperation.splitOn('.', fullyQualifiedName));

		int relevance = computeBaseRelevance();
		relevance += computeRelevanceForResolution();
		relevance += computeRelevanceForInterestingProposal();
		relevance += computeRelevanceForRestrictions(accessibility);
		relevance += computeRelevanceForCaseMatching(this.completionToken, simpleTypeName);
		relevance += computeRelevanceForExpectingType(typeBinding);
		relevance += computeRelevanceForQualification(isQualified);
		relevance += computeRelevanceForConstructor();

		boolean isInterface = false;
		int kind = typeModifiers & (ClassFileConstants.AccInterface | ClassFileConstants.AccEnum | ClassFileConstants.AccAnnotation);
		switch (kind) {
			case ClassFileConstants.AccAnnotation:
			case ClassFileConstants.AccAnnotation | ClassFileConstants.AccInterface:
				relevance += computeRelevanceForAnnotation();
				relevance += computeRelevanceForInterface();
				isInterface = true;
				break;
			case ClassFileConstants.AccEnum:
				relevance += computeRelevanceForEnum();
				break;
			case ClassFileConstants.AccInterface:
				relevance += computeRelevanceForInterface();
				isInterface = true;
				break;
			default:
				relevance += computeRelevanceForClass();
				relevance += computeRelevanceForException(simpleTypeName);
				break;
		}

		char[] completion;
		if (this.source != null
					&& this.source.length > this.endPosition
					&& this.source[this.endPosition] == '(') {
			completion = CharOperation.NO_CHAR;
		} else {
			completion = new char[] { '(', ')' };
		}

		InternalCompletionProposal typeProposal = createProposal(CompletionProposal.TYPE_REF, this.actualCompletionPosition);
		typeProposal.nameLookup = this.nameEnvironment.nameLookup;
		typeProposal.completionEngine = this;
		typeProposal.setDeclarationSignature(packageName);
		typeProposal.setSignature(createNonGenericTypeSignature(packageName, typeName));
		typeProposal.setPackageName(packageName);
		typeProposal.setTypeName(typeName);
		typeProposal.setCompletion(typeCompletion);
		typeProposal.setFlags(typeModifiers);
		typeProposal.setReplaceRange(this.startPosition - this.offset, this.endPosition - this.offset);
		typeProposal.setTokenRange(this.startPosition - this.offset, this.endPosition - this.offset);
		typeProposal.setRelevance(relevance);

		switch (parameterCount) {
			case -1: // default constructor
				int flags = Flags.AccPublic;
				if (Flags.isDeprecated(typeModifiers)) {
					flags |= Flags.AccDeprecated;
				}

				if (isInterface || (typeModifiers & ClassFileConstants.AccAbstract) != 0) {
					this.noProposal = false;
					if(!isIgnored(CompletionProposal.ANONYMOUS_CLASS_CONSTRUCTOR_INVOCATION, CompletionProposal.TYPE_REF)) {
						InternalCompletionProposal proposal = createProposal(CompletionProposal.ANONYMOUS_CLASS_CONSTRUCTOR_INVOCATION, this.actualCompletionPosition);
						proposal.setDeclarationSignature(createNonGenericTypeSignature(packageName, typeName));
						proposal.setDeclarationKey(createBindingKey(packageName, typeName));
						proposal.setSignature(DEFAULT_CONSTRUCTOR_SIGNATURE);
						proposal.setDeclarationPackageName(packageName);
						proposal.setDeclarationTypeName(typeName);
						proposal.setParameterPackageNames(CharOperation.NO_CHAR_CHAR);
						proposal.setParameterTypeNames(CharOperation.NO_CHAR_CHAR);
						proposal.setParameterNames(CharOperation.NO_CHAR_CHAR);
						proposal.setName(simpleTypeName);
						proposal.setRequiredProposals(new CompletionProposal[]{typeProposal});
						proposal.setIsContructor(true);
						proposal.setCompletion(completion);
						proposal.setFlags(flags);
						proposal.setReplaceRange(this.endPosition - this.offset, this.endPosition - this.offset);
						proposal.setTokenRange(this.tokenStart - this.offset, this.tokenEnd - this.offset);
						proposal.setRelevance(relevance);
						this.requestor.accept(proposal);
						if(DEBUG) {
							this.printDebug(proposal);
						}
					}
				} else {
					this.noProposal = false;
					if(!isIgnored(CompletionProposal.CONSTRUCTOR_INVOCATION, CompletionProposal.TYPE_REF)) {
						InternalCompletionProposal proposal =  createProposal(CompletionProposal.CONSTRUCTOR_INVOCATION, this.actualCompletionPosition);
						proposal.setDeclarationSignature(createNonGenericTypeSignature(packageName, typeName));
						proposal.setSignature(DEFAULT_CONSTRUCTOR_SIGNATURE);
						proposal.setDeclarationPackageName(packageName);
						proposal.setDeclarationTypeName(typeName);
						proposal.setParameterPackageNames(CharOperation.NO_CHAR_CHAR);
						proposal.setParameterTypeNames(CharOperation.NO_CHAR_CHAR);
						proposal.setParameterNames(CharOperation.NO_CHAR_CHAR);
						proposal.setName(simpleTypeName);
						proposal.setRequiredProposals(new CompletionProposal[]{typeProposal});
						proposal.setIsContructor(true);
						proposal.setCompletion(completion);
						proposal.setFlags(flags);
						proposal.setReplaceRange(this.endPosition - this.offset, this.endPosition - this.offset);
						proposal.setTokenRange(this.tokenStart - this.offset, this.tokenEnd - this.offset);
						proposal.setRelevance(relevance);
						this.requestor.accept(proposal);
						if(DEBUG) {
							this.printDebug(proposal);
						}
					}
				}
				break;
			case 0: // constructor with no parameter

				if ((typeModifiers & ClassFileConstants.AccAbstract) != 0) {
					this.noProposal = false;
					if(!isIgnored(CompletionProposal.ANONYMOUS_CLASS_CONSTRUCTOR_INVOCATION, CompletionProposal.TYPE_REF)) {
						InternalCompletionProposal proposal = createProposal(CompletionProposal.ANONYMOUS_CLASS_CONSTRUCTOR_INVOCATION, this.actualCompletionPosition);
						proposal.setDeclarationSignature(createNonGenericTypeSignature(packageName, typeName));
						proposal.setDeclarationKey(createBindingKey(packageName, typeName));
						proposal.setSignature(DEFAULT_CONSTRUCTOR_SIGNATURE);
						proposal.setDeclarationPackageName(packageName);
						proposal.setDeclarationTypeName(typeName);
						proposal.setParameterPackageNames(CharOperation.NO_CHAR_CHAR);
						proposal.setParameterTypeNames(CharOperation.NO_CHAR_CHAR);
						proposal.setParameterNames(CharOperation.NO_CHAR_CHAR);
						proposal.setName(simpleTypeName);
						proposal.setRequiredProposals(new CompletionProposal[]{typeProposal});
						proposal.setIsContructor(true);
						proposal.setCompletion(completion);
						proposal.setFlags(modifiers);
						proposal.setReplaceRange(this.endPosition - this.offset, this.endPosition - this.offset);
						proposal.setTokenRange(this.tokenStart - this.offset, this.tokenEnd - this.offset);
						proposal.setRelevance(relevance);
						this.requestor.accept(proposal);
						if(DEBUG) {
							this.printDebug(proposal);
						}
					}
				} else {
					this.noProposal = false;
					if(!isIgnored(CompletionProposal.CONSTRUCTOR_INVOCATION, CompletionProposal.TYPE_REF)) {
						InternalCompletionProposal proposal =  createProposal(CompletionProposal.CONSTRUCTOR_INVOCATION, this.actualCompletionPosition);
						proposal.setDeclarationSignature(createNonGenericTypeSignature(packageName, typeName));
						proposal.setSignature(DEFAULT_CONSTRUCTOR_SIGNATURE);
						proposal.setDeclarationPackageName(packageName);
						proposal.setDeclarationTypeName(typeName);
						proposal.setParameterPackageNames(CharOperation.NO_CHAR_CHAR);
						proposal.setParameterTypeNames(CharOperation.NO_CHAR_CHAR);
						proposal.setParameterNames(CharOperation.NO_CHAR_CHAR);
						proposal.setName(simpleTypeName);
						proposal.setRequiredProposals(new CompletionProposal[]{typeProposal});
						proposal.setIsContructor(true);
						proposal.setCompletion(completion);
						proposal.setFlags(modifiers);
						proposal.setReplaceRange(this.endPosition - this.offset, this.endPosition - this.offset);
						proposal.setTokenRange(this.tokenStart - this.offset, this.tokenEnd - this.offset);
						proposal.setRelevance(relevance);
						this.requestor.accept(proposal);
						if(DEBUG) {
							this.printDebug(proposal);
						}
					}
				}
				break;
			default: // constructor with parameter
				if (signature == null) {
					// resolve type to found parameter types
					signature = getResolvedSignature(parameterTypes, fullyQualifiedName, parameterCount, scope);
					if (signature == null) return;
				} else {
					signature = CharOperation.replaceOnCopy(signature, '/', '.');
				}

				int parameterNamesLength = parameterNames == null ? 0 : parameterNames.length;
				if (parameterCount != parameterNamesLength) {
					parameterNames = null;
				}

				if ((typeModifiers & ClassFileConstants.AccAbstract) != 0) {
					this.noProposal = false;
					if(!isIgnored(CompletionProposal.ANONYMOUS_CLASS_CONSTRUCTOR_INVOCATION, CompletionProposal.TYPE_REF)) {
						InternalCompletionProposal proposal = createProposal(CompletionProposal.ANONYMOUS_CLASS_CONSTRUCTOR_INVOCATION, this.actualCompletionPosition);
						proposal.setDeclarationSignature(createNonGenericTypeSignature(packageName, typeName));
						proposal.setDeclarationKey(createBindingKey(packageName, typeName));
						proposal.setSignature(signature);
						proposal.setDeclarationPackageName(packageName);
						proposal.setDeclarationTypeName(typeName);
						proposal.setParameterPackageNames(CharOperation.NO_CHAR_CHAR);
						proposal.setParameterTypeNames(CharOperation.NO_CHAR_CHAR);
						if (parameterNames != null) {
							proposal.setParameterNames(parameterNames);
						} else {
							proposal.setHasNoParameterNamesFromIndex(true);
						}
						proposal.setName(simpleTypeName);
						proposal.setRequiredProposals(new CompletionProposal[]{typeProposal});
						proposal.setIsContructor(true);
						proposal.setCompletion(completion);
						proposal.setFlags(modifiers);
						proposal.setReplaceRange(this.endPosition - this.offset, this.endPosition - this.offset);
						proposal.setTokenRange(this.tokenStart - this.offset, this.tokenEnd - this.offset);
						proposal.setRelevance(relevance);
						this.requestor.accept(proposal);
						if(DEBUG) {
							this.printDebug(proposal);
						}
					}
				} else {
					this.noProposal = false;
					if(!isIgnored(CompletionProposal.CONSTRUCTOR_INVOCATION, CompletionProposal.TYPE_REF)) {
						InternalCompletionProposal proposal =  createProposal(CompletionProposal.CONSTRUCTOR_INVOCATION, this.actualCompletionPosition);
						proposal.setDeclarationSignature(createNonGenericTypeSignature(packageName, typeName));
						proposal.setSignature(signature);
						proposal.setDeclarationPackageName(packageName);
						proposal.setDeclarationTypeName(typeName);
						proposal.setParameterPackageNames(CharOperation.NO_CHAR_CHAR);
						proposal.setParameterTypeNames(CharOperation.NO_CHAR_CHAR);
						if (parameterNames != null) {
							proposal.setParameterNames(parameterNames);
						} else {
							proposal.setHasNoParameterNamesFromIndex(true);
						}
						proposal.setName(simpleTypeName);
						proposal.setRequiredProposals(new CompletionProposal[]{typeProposal});
						proposal.setIsContructor(true);
						proposal.setCompletion(completion);
						proposal.setFlags(modifiers);
						proposal.setReplaceRange(this.endPosition - this.offset, this.endPosition - this.offset);
						proposal.setTokenRange(this.tokenStart - this.offset, this.tokenEnd - this.offset);
						proposal.setRelevance(relevance);

						this.requestor.accept(proposal);
						if(DEBUG) {
							this.printDebug(proposal);
						}
					}
				}
				break;
		}
	}

	private void proposeNewMethod(char[] token, ReferenceBinding reference) {
		if(!this.requestor.isIgnored(CompletionProposal.POTENTIAL_METHOD_DECLARATION)) {
			int relevance = computeBaseRelevance();
			relevance += computeRelevanceForResolution();
			relevance += computeRelevanceForInterestingProposal();
			relevance += computeRelevanceForRestrictions(IAccessRule.K_ACCESSIBLE); // no access restriction for new method

			InternalCompletionProposal proposal =  createProposal(CompletionProposal.POTENTIAL_METHOD_DECLARATION, this.actualCompletionPosition);
			proposal.setDeclarationSignature(getSignature(reference));
			proposal.setSignature(
					createMethodSignature(
							CharOperation.NO_CHAR_CHAR,
							CharOperation.NO_CHAR_CHAR,
							CharOperation.NO_CHAR,
							VOID));
			proposal.setDeclarationPackageName(reference.qualifiedPackageName());
			proposal.setDeclarationTypeName(reference.qualifiedSourceName());

			//proposal.setPackageName(null);
			proposal.setTypeName(VOID);
			proposal.setName(token);
			//proposal.setParameterPackageNames(null);
			//proposal.setParameterTypeNames(null);
			//proposal.setPackageName(null);
			proposal.setCompletion(token);
			proposal.setFlags(Flags.AccPublic);
			proposal.setReplaceRange(this.startPosition - this.offset, this.endPosition - this.offset);
			proposal.setTokenRange(this.tokenStart - this.offset, this.tokenEnd - this.offset);
			proposal.setRelevance(relevance);
			this.requestor.accept(proposal);
			if(DEBUG) {
				this.printDebug(proposal);
			}
		}
	}

	private void proposeType(
			char[] packageName,
			char[] simpleTypeName,
			int modifiers,
			int accessibility,
			char[] typeName,
			char[] fullyQualifiedName,
			boolean isQualified,
			Scope scope) {
		char[] completionName = fullyQualifiedName;
		if(isQualified) {
			if (packageName == null || packageName.length == 0)
				if (this.unitScope != null && this.unitScope.fPackage.compoundName != CharOperation.NO_CHAR_CHAR)
					return; // ignore types from the default package from outside it
		} else {
			completionName = simpleTypeName;
		}

		TypeBinding guessedType = null;
		if ((modifiers & ClassFileConstants.AccAnnotation) != 0 &&
				this.assistNodeIsAnnotation &&
				(this.targetedElement & TagBits.AnnotationTargetMASK) != 0) {
			char[][] cn = CharOperation.splitOn('.', fullyQualifiedName);

			TypeReference ref;
			if (cn.length == 1) {
				ref = new SingleTypeReference(simpleTypeName, 0);
			} else {
				ref = new QualifiedTypeReference(cn,new long[cn.length]);
			}

			switch (scope.kind) {
				case Scope.METHOD_SCOPE :
				case Scope.BLOCK_SCOPE :
					guessedType = ref.resolveType((BlockScope)scope);
					break;
				case Scope.CLASS_SCOPE :
					guessedType = ref.resolveType((ClassScope)scope);
					break;
			}

			if (guessedType == null || !guessedType.isValidBinding()) return;

			if (!hasPossibleAnnotationTarget(guessedType, scope)) return;
		}

		int relevance = computeBaseRelevance();
		relevance += computeRelevanceForResolution();
		relevance += computeRelevanceForInterestingProposal(packageName, fullyQualifiedName);
		relevance += computeRelevanceForRestrictions(accessibility);
		relevance += computeRelevanceForCaseMatching(this.completionToken, simpleTypeName);
		relevance += computeRelevanceForExpectingType(packageName, simpleTypeName);
		relevance += computeRelevanceForQualification(isQualified);

		int kind = modifiers & (ClassFileConstants.AccInterface | ClassFileConstants.AccEnum | ClassFileConstants.AccAnnotation);
		switch (kind) {
			case ClassFileConstants.AccAnnotation:
			case ClassFileConstants.AccAnnotation | ClassFileConstants.AccInterface:
				relevance += computeRelevanceForAnnotation();
				if (guessedType != null) relevance += computeRelevanceForAnnotationTarget(guessedType);
				relevance += computeRelevanceForInterface();
				break;
			case ClassFileConstants.AccEnum:
				relevance += computeRelevanceForEnum();
				break;
			case ClassFileConstants.AccInterface:
				relevance += computeRelevanceForInterface();
				break;
			default:
				relevance += computeRelevanceForClass();
				relevance += computeRelevanceForException(simpleTypeName);
				break;
		}

		this.noProposal = false;
		if(!this.requestor.isIgnored(CompletionProposal.TYPE_REF)) {
			createTypeProposal(packageName, typeName, modifiers, accessibility, completionName, relevance);
		}
	}

	protected void reset() {

		super.reset(false);
		this.validPackageNames = new SimpleSetOfCharArray(10);
		this.invalidPackageNames = new SimpleSetOfCharArray(1);
		this.knownModules = new HashtableOfObject(10);
		this.knownPkgs = new HashtableOfObject(10);
		this.knownTypes = new HashtableOfObject(10);
		if (this.noCacheNameEnvironment != null) {
			this.noCacheNameEnvironment.cleanup();
			this.noCacheNameEnvironment = null;
			JavaModelManager.getJavaModelManager().flushZipFiles(this);
		}
	}

	private void setSourceAndTokenRange(int start, int end) {
		this.setSourceAndTokenRange(start, end, true);
	}

	private void setSourceAndTokenRange(int start, int end, boolean emptyTokenAdjstment) {
		this.setSourceRange(start, end, emptyTokenAdjstment);
		this.setTokenRange(start, end, emptyTokenAdjstment);
	}

	private void setSourceRange(int start, int end) {
		this.setSourceRange(start, end, true);
	}

	private void setSourceRange(int start, int end, boolean emptyTokenAdjstment) {
		this.startPosition = start;
		if(emptyTokenAdjstment) {
			int endOfEmptyToken = ((CompletionScanner)this.parser.scanner).endOfEmptyToken;
			this.endPosition = endOfEmptyToken > end ? endOfEmptyToken + 1 : end + 1;
		} else {
			this.endPosition = end + 1;
		}
	}

	private void setTokenRange(int start, int end) {
		this.setTokenRange(start, end, true);
	}
	private void setTokenRange(int start, int end, boolean emptyTokenAdjstment) {
		this.tokenStart = start;
		if(emptyTokenAdjstment) {
			int endOfEmptyToken = ((CompletionScanner)this.parser.scanner).endOfEmptyToken;
			this.tokenEnd = endOfEmptyToken > end ? endOfEmptyToken + 1 : end + 1;
		} else {
			this.tokenEnd = end + 1;
		}
	}

	private char[] substituteMethodTypeParameterName(char firstName, char startChar, char endChar, char[][] excludedNames, char[][] otherParameterNames) {
		char name = firstName;
		next : while (true) {
			for (int i = 0 ; i < excludedNames.length ; i++){
				if(excludedNames[i].length == 1 && ScannerHelper.toLowerCase(excludedNames[i][0]) == ScannerHelper.toLowerCase(name)) {
					name++;
					if(name > endChar)
						name = startChar;
					if(name == firstName)
						return substituteMethodTypeParameterName(new char[]{firstName}, excludedNames, otherParameterNames);
					continue next;
				}
			}

			for (int i = 0; i < otherParameterNames.length; i++) {
				if(otherParameterNames[i].length == 1 && ScannerHelper.toLowerCase(otherParameterNames[i][0]) == ScannerHelper.toLowerCase(name)) {
					name++;
					if(name > endChar)
						name = startChar;
					if(name == firstName)
						return substituteMethodTypeParameterName(new char[]{firstName}, excludedNames, otherParameterNames);
					continue next;
				}
			}
			break next;
		}
		return new char[]{name};
	}

	private char[] substituteMethodTypeParameterName(char[] firstName, char[][] excludedNames, char[][] otherParameterNames) {
		char[] name = firstName;
		int count = 2;
		next : while(true) {
			for(int k = 0 ; k < excludedNames.length ; k++){
				if(CharOperation.equals(name, excludedNames[k], false)) {
					name = CharOperation.concat(firstName, String.valueOf(count++).toCharArray());
					continue next;
				}
			}
			for (int i = 0; i < otherParameterNames.length; i++) {
				if(CharOperation.equals(name, otherParameterNames[i], false)) {
					name = CharOperation.concat(firstName, String.valueOf(count++).toCharArray());
					continue next;
				}
			}
			break next;
		}
		return name;
	}

	private char[][] substituteMethodTypeParameterNames(TypeVariableBinding[] typeVariables, char[][] excludedNames) {
		char[][] substituedParameterNames = new char[typeVariables.length][];

		for (int i = 0; i < substituedParameterNames.length; i++) {
			substituedParameterNames[i] = typeVariables[i].sourceName;
		}

		boolean foundConflicts = false;

		nextTypeParameter : for (int i = 0; i < typeVariables.length; i++) {
			TypeVariableBinding typeVariableBinding = typeVariables[i];
			char[] methodParameterName = typeVariableBinding.sourceName;

			for (int j = 0; j < excludedNames.length; j++) {
				char[] typeParameterName = excludedNames[j];
				if(CharOperation.equals(typeParameterName, methodParameterName, false)) {
					char[] substitution;
					if(methodParameterName.length == 1) {
						if(ScannerHelper.isUpperCase(methodParameterName[0])) {
							substitution = substituteMethodTypeParameterName(methodParameterName[0], 'A', 'Z', excludedNames, substituedParameterNames);
						} else {
							substitution = substituteMethodTypeParameterName(methodParameterName[0], 'a', 'z', excludedNames, substituedParameterNames);
						}
					} else {
						substitution = substituteMethodTypeParameterName(methodParameterName, excludedNames, substituedParameterNames);
					}
					substituedParameterNames[i] = substitution;

					foundConflicts = true;
					continue nextTypeParameter;
				}
			}
		}

		if(foundConflicts) return substituedParameterNames;
		return null;
	}

	private void findLambdaExpressions(Scope scope) {
		if (this.requestor.isIgnored(CompletionProposal.LAMBDA_EXPRESSION) ||
				this.compilerOptions.sourceLevel < ClassFileConstants.JDK1_8) {
			return;
		}

		if (this.expectedTypesPtr > -1) {
			for (int i = 0; i <= this.expectedTypesPtr; i++) {
				TypeBinding type = this.expectedTypes[i];

				if (type.isFunctionalInterface(scope)) {
					int relevance = computeBaseRelevance();
					relevance += R_EXACT_EXPECTED_TYPE;
					relevance += R_ABSTRACT_METHOD;
					relevance += computeRelevanceForResolution();
					relevance += computeRelevanceForInterestingProposal();
					relevance += computeRelevanceForRestrictions(IAccessRule.K_ACCESSIBLE);

					// we are sure this is not null since we already check for eligibility
					MethodBinding method = type.getSingleAbstractMethod(scope, true);
					int length = method.parameters.length;
					char[][] parameterTypeNames = new char[length][];

					for (int j = 0; j < length; j++) {
						TypeBinding p = method.parameters[j];
						parameterTypeNames[j] = p.qualifiedSourceName();
					}
					char[][] parameterNames = findMethodParameterNames(method, parameterTypeNames);

					InternalCompletionProposal proposal = createProposal(CompletionProposal.LAMBDA_EXPRESSION,
							this.actualCompletionPosition);
					proposal.setBinding(method);
					proposal.setDeclarationSignature(getSignature(method.declaringClass));
					proposal.setSignature(getSignature(method));
					MethodBinding original = method.original();
					if(original != method) {
						proposal.setOriginalSignature(getSignature(original));
					}
					proposal.setReplaceRange(this.startPosition - this.offset, this.endPosition - this.offset);
					proposal.setTokenRange(this.tokenStart - this.offset, this.tokenEnd - this.offset);
					proposal.setRelevance(relevance);
					proposal.setCompletion(LAMBDA);
					proposal.setParameterTypeNames(parameterTypeNames);
					proposal.setFlags(method.modifiers);
					proposal.setDeclarationPackageName(method.declaringClass.qualifiedPackageName());
					proposal.setDeclarationTypeName(method.declaringClass.qualifiedSourceName());
					proposal.setName(method.selector);
					proposal.setTypeName(method.returnType.qualifiedSourceName());
					if (parameterNames != null) {
						proposal.setParameterNames(parameterNames);
					}

					this.requestor.accept(proposal);
					if (DEBUG) {
						this.printDebug(proposal);
					}
				}
			}
		}
	}

	public void findPackagesInModule(char[] prefix,  ModuleBinding module, Scope scope) {
		if (module != null ) {
			String name= new String(prefix);
			String[] splittedName = Util.splitOn('.', new String(name), 0, name.length());
			PackageBinding[] pkgBindings = module.getExports();
			for (PackageBinding pkgBinding : pkgBindings) {
				String[] pkgName = CharOperation.toStrings(pkgBinding.compoundName);
				if (pkgName != null &&
						( name.isEmpty() ||
								Util.startsWithIgnoreCase(pkgName, splittedName, true))) {
					acceptPackage(CharOperation.toString(pkgBinding.compoundName).toCharArray(), module.moduleName);
				}
			}
		}
	}

}
