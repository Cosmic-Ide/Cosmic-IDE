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
 *     Jesper Steen Møller <jesper@selskabet.org> - contributions for:
 *         Bug 531046: [10] ICodeAssist#codeSelect support for 'var'
 *******************************************************************************/
package org.eclipse.jdt.internal.codeassist;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.IOrdinaryClassFile;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.WorkingCopyOwner;
import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.TypeNameMatch;
import org.eclipse.jdt.core.search.TypeNameMatchRequestor;
import org.eclipse.jdt.internal.codeassist.impl.AssistParser;
import org.eclipse.jdt.internal.codeassist.impl.Engine;
import org.eclipse.jdt.internal.codeassist.select.SelectionJavadocParser;
import org.eclipse.jdt.internal.codeassist.select.SelectionNodeFound;
import org.eclipse.jdt.internal.codeassist.select.SelectionOnPackageVisibilityReference;
import org.eclipse.jdt.internal.codeassist.select.SelectionOnImportReference;
import org.eclipse.jdt.internal.codeassist.select.SelectionOnLocalName;
import org.eclipse.jdt.internal.codeassist.select.SelectionOnMessageSend;
import org.eclipse.jdt.internal.codeassist.select.SelectionOnPackageReference;
import org.eclipse.jdt.internal.codeassist.select.SelectionOnQualifiedTypeReference;
import org.eclipse.jdt.internal.codeassist.select.SelectionOnSingleTypeReference;
import org.eclipse.jdt.internal.codeassist.select.SelectionParser;
import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.DefaultErrorHandlingPolicies;
import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Argument;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ConstructorDeclaration;
import org.eclipse.jdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ImportReference;
import org.eclipse.jdt.internal.compiler.ast.LambdaExpression;
import org.eclipse.jdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.jdt.internal.compiler.ast.MessageSend;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ModuleDeclaration;
import org.eclipse.jdt.internal.compiler.ast.PackageVisibilityStatement;
import org.eclipse.jdt.internal.compiler.ast.SingleTypeReference;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeParameter;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFormatException;
import org.eclipse.jdt.internal.compiler.env.AccessRestriction;
import org.eclipse.jdt.internal.compiler.env.IBinaryType;
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.jdt.internal.compiler.env.ISourceType;
import org.eclipse.jdt.internal.compiler.lookup.ArrayBinding;
import org.eclipse.jdt.internal.compiler.lookup.BaseTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.Binding;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.CompilationUnitScope;
import org.eclipse.jdt.internal.compiler.lookup.FieldBinding;
import org.eclipse.jdt.internal.compiler.lookup.IntersectionTypeBinding18;
import org.eclipse.jdt.internal.compiler.lookup.LocalTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.LocalVariableBinding;
import org.eclipse.jdt.internal.compiler.lookup.LookupEnvironment;
import org.eclipse.jdt.internal.compiler.lookup.MemberTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.MethodScope;
import org.eclipse.jdt.internal.compiler.lookup.ModuleBinding;
import org.eclipse.jdt.internal.compiler.lookup.PackageBinding;
import org.eclipse.jdt.internal.compiler.lookup.ParameterizedTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.ProblemFieldBinding;
import org.eclipse.jdt.internal.compiler.lookup.ProblemReasons;
import org.eclipse.jdt.internal.compiler.lookup.ProblemReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.Scope;
import org.eclipse.jdt.internal.compiler.lookup.SourceTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.SyntheticMethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeConstants;
import org.eclipse.jdt.internal.compiler.lookup.TypeVariableBinding;
import org.eclipse.jdt.internal.compiler.parser.Scanner;
import org.eclipse.jdt.internal.compiler.parser.ScannerHelper;
import org.eclipse.jdt.internal.compiler.parser.SourceTypeConverter;
import org.eclipse.jdt.internal.compiler.parser.TerminalTokens;
import org.eclipse.jdt.internal.compiler.problem.AbortCompilation;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblemFactory;
import org.eclipse.jdt.internal.compiler.problem.ProblemReporter;
import org.eclipse.jdt.internal.compiler.util.HashtableOfObject;
import org.eclipse.jdt.internal.compiler.util.ObjectVector;
import org.eclipse.jdt.internal.core.BinaryTypeConverter;
import org.eclipse.jdt.internal.core.ClassFile;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.jdt.internal.core.JrtPackageFragmentRoot;
import org.eclipse.jdt.internal.core.SearchableEnvironment;
import org.eclipse.jdt.internal.core.SelectionRequestor;
import org.eclipse.jdt.internal.core.SourceType;
import org.eclipse.jdt.internal.core.SourceTypeElementInfo;
import org.eclipse.jdt.internal.core.nd.java.model.BinaryTypeDescriptor;
import org.eclipse.jdt.internal.core.nd.java.model.BinaryTypeFactory;
import org.eclipse.jdt.internal.core.search.BasicSearchEngine;
import org.eclipse.jdt.internal.core.search.TypeNameMatchRequestorWrapper;
import org.eclipse.jdt.internal.core.util.ASTNodeFinder;
import org.eclipse.jdt.internal.core.util.HashSetOfCharArrayArray;

/**
 * The selection engine is intended to infer the nature of a selected name in some
 * source code. This name can be qualified.
 *
 * Selection is resolving context using a name environment (no need to search), assuming
 * the source where selection occurred is correct and will not perform any completion
 * attempt. If this was the desired behavior, a call to the CompletionEngine should be
 * performed instead.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public final class SelectionEngine extends Engine implements ISearchRequestor {

	private static class SelectionTypeNameMatchRequestorWrapper extends TypeNameMatchRequestorWrapper {

		static class AcceptedType {
			public int modifiers;
			public char[] packageName;
			public char[] simpleTypeName;
			public String path;
			public AccessRestriction access;

			public AcceptedType(int modifiers, char[] packageName, char[] simpleTypeName, String path, AccessRestriction access) {
				this.modifiers = modifiers;
				this.packageName = packageName;
				this.simpleTypeName = simpleTypeName;
				this.path = path;
				this.access = access;
			}
		}

		private ImportReference[] importReferences;

		private boolean importCachesNodeInitialized = false;
		private ImportReference[] onDemandImportsNodeCache;
		private int onDemandImportsNodeCacheCount;
		private char[][][] importsNodeCache;
		private int importsNodeCacheCount;

		private HashtableOfObject onDemandFound = new HashtableOfObject();
		private ObjectVector notImportedFound = new ObjectVector();

		public SelectionTypeNameMatchRequestorWrapper(TypeNameMatchRequestor requestor, IJavaSearchScope scope, ImportReference[] importReferences) {
			super(requestor, scope);
			this.importReferences = importReferences;
		}

		@Override
		public void acceptType(int modifiers, char[] packageName, char[] simpleTypeName, char[][] enclosingTypeNames, String path, AccessRestriction access) {
			if (enclosingTypeNames != null && enclosingTypeNames.length > 0) return;

			if (!this.importCachesNodeInitialized) initializeImportNodeCaches();

			char[] fullyQualifiedTypeName = CharOperation.concat(packageName, simpleTypeName, '.');

			for (int i = 0; i < this.importsNodeCacheCount; i++) {
				char[][] importName = this.importsNodeCache[i];
				if (CharOperation.equals(importName[0], simpleTypeName)) {

					if(CharOperation.equals(importName[1], fullyQualifiedTypeName)) {
						super.acceptType(modifiers, packageName, simpleTypeName, enclosingTypeNames, path, access);
					}
					return;
				}
			}

			for (int i = 0; i < this.onDemandImportsNodeCacheCount; i++) {
				char[][] importName = this.onDemandImportsNodeCache[i].tokens;
				char[] importFlatName = CharOperation.concatWith(importName, '.');

				if (CharOperation.equals(importFlatName, packageName)) {

					this.onDemandFound.put(simpleTypeName, simpleTypeName);
					super.acceptType(modifiers, packageName, simpleTypeName, enclosingTypeNames, path, access);
					return;
				}
			}


			this.notImportedFound.add(new AcceptedType(modifiers, packageName, simpleTypeName, path, access));
		}

		public void acceptNotImported() {
			int size = this.notImportedFound.size();
			for (int i = 0; i < size; i++) {
				AcceptedType acceptedType = (AcceptedType)this.notImportedFound.elementAt(i);

				if (this.onDemandFound.get(acceptedType.simpleTypeName) == null) {
					super.acceptType(
							acceptedType.modifiers,
							acceptedType.packageName,
							acceptedType.simpleTypeName,
							null,
							acceptedType.path,
							acceptedType.access);
				}
			}
		}

		public void initializeImportNodeCaches() {
			int length = this.importReferences == null ? 0 : this.importReferences.length;

			for (int i = 0; i < length; i++) {
				ImportReference importReference = this.importReferences[i];
				if((importReference.bits & ASTNode.OnDemand) != 0) {
					if(this.onDemandImportsNodeCache == null) {
						this.onDemandImportsNodeCache = new ImportReference[length - i];
					}
					this.onDemandImportsNodeCache[this.onDemandImportsNodeCacheCount++] =
						importReference;
				} else {
					if(this.importsNodeCache == null) {
						this.importsNodeCache = new char[length - i][][];
					}


					this.importsNodeCache[this.importsNodeCacheCount++] = new char[][]{
							importReference.tokens[importReference.tokens.length - 1],
							CharOperation.concatWith(importReference.tokens, '.')
						};
				}
			}

			this.importCachesNodeInitialized = true;
		}
	}

	public static boolean DEBUG = false;
	public static boolean PERF = false;

	SelectionParser parser;
	ISelectionRequestor requestor;
	WorkingCopyOwner owner;

	boolean acceptedAnswer;

	private int actualSelectionStart;
	private int actualSelectionEnd;
	private char[] selectedIdentifier;

	private char[][][] acceptedClasses;
	private int[] acceptedClassesModifiers;
	private char[][][] acceptedInterfaces;
	private int[] acceptedInterfacesModifiers;
	private char[][][] acceptedEnums;
	private int[] acceptedEnumsModifiers;
	private char[][][] acceptedAnnotations;
	private int[] acceptedAnnotationsModifiers;
	int acceptedClassesCount;
	int acceptedInterfacesCount;
	int acceptedEnumsCount;
	int acceptedAnnotationsCount;

	boolean noProposal = true;
	CategorizedProblem problem = null;

	/**
	 * The SelectionEngine is responsible for computing the selected object.
	 *
	 * It requires a searchable name environment, which supports some
	 * specific search APIs, and a requestor to feed back the results to a UI.
	 *
	 *  @param nameEnvironment org.eclipse.jdt.internal.core.SearchableEnvironment
	 *      used to resolve type/package references and search for types/packages
	 *      based on partial names.
	 *
	 *  @param requestor org.eclipse.jdt.internal.codeassist.ISelectionRequestor
	 *      since the engine might produce answers of various forms, the engine
	 *      is associated with a requestor able to accept all possible completions.
	 *
	 *  @param settings java.util.Map
	 *		set of options used to configure the code assist engine.
	 */
	public SelectionEngine(
		SearchableEnvironment nameEnvironment,
		ISelectionRequestor requestor,
		Map settings,
		WorkingCopyOwner owner) {

		super(settings);

		this.requestor = requestor;
		this.nameEnvironment = nameEnvironment;

		ProblemReporter problemReporter =
			new ProblemReporter(
				DefaultErrorHandlingPolicies.proceedWithAllProblems(),
				this.compilerOptions,
				new DefaultProblemFactory(Locale.getDefault())) {

			@Override
			public CategorizedProblem createProblem(
				char[] fileName,
				int problemId,
				String[] problemArguments,
				String[] messageArguments,
				int severity,
				int problemStartPosition,
				int problemEndPosition,
				int lineNumber,
				int columnNumber) {
				CategorizedProblem pb =  super.createProblem(
					fileName,
					problemId,
					problemArguments,
					messageArguments,
					severity,
					problemStartPosition,
					problemEndPosition,
					lineNumber,
					columnNumber);
					if(SelectionEngine.this.problem == null && pb.isError() && (pb.getID() & IProblem.Syntax) == 0) {
						SelectionEngine.this.problem = pb;
					}

					return pb;
			}
		};
		this.lookupEnvironment =
			new LookupEnvironment(this, this.compilerOptions, problemReporter, nameEnvironment);
		this.parser = new SelectionParser(problemReporter);
		this.owner = owner;
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
			AccessRestriction access) {
		// constructors aren't searched
	}
	@Override
	public void accept(ISourceType[] sourceTypes, PackageBinding packageBinding, AccessRestriction accessRestriction) {
		CompilationUnitDeclaration unit = this.lookupEnvironment.unitBeingCompleted;
		super.accept(sourceTypes, packageBinding, accessRestriction);
		this.lookupEnvironment.unitBeingCompleted = unit;
	}

	@Override
	public void acceptType(char[] packageName, char[] simpleTypeName, char[][] enclosingTypeNames, int modifiers, AccessRestriction accessRestriction) {
		char[] typeName = enclosingTypeNames == null ?
				simpleTypeName :
					CharOperation.concat(
						CharOperation.concatWith(enclosingTypeNames, '.'),
						simpleTypeName,
						'.');

		if (CharOperation.equals(simpleTypeName, this.selectedIdentifier)) {
			char[] flatEnclosingTypeNames =
				enclosingTypeNames == null || enclosingTypeNames.length == 0 ?
						null :
							CharOperation.concatWith(enclosingTypeNames, '.');
			if(mustQualifyType(packageName, simpleTypeName, flatEnclosingTypeNames, modifiers)) {
				int length = 0;
				int kind = modifiers & (ClassFileConstants.AccInterface | ClassFileConstants.AccEnum | ClassFileConstants.AccAnnotation);
				switch (kind) {
					case ClassFileConstants.AccAnnotation:
					case ClassFileConstants.AccAnnotation | ClassFileConstants.AccInterface:
						char[][] acceptedAnnotation = new char[2][];
						acceptedAnnotation[0] = packageName;
						acceptedAnnotation[1] = typeName;

						if(this.acceptedAnnotations == null) {
							this.acceptedAnnotations = new char[10][][];
							this.acceptedAnnotationsModifiers = new int[10];
							this.acceptedAnnotationsCount = 0;
						}
						length = this.acceptedAnnotations.length;
						if(length == this.acceptedAnnotationsCount) {
							int newLength = (length + 1)* 2;
							System.arraycopy(this.acceptedAnnotations, 0, this.acceptedAnnotations = new char[newLength][][], 0, length);
							System.arraycopy(this.acceptedAnnotationsModifiers, 0, this.acceptedAnnotationsModifiers = new int[newLength], 0, length);
						}
						this.acceptedAnnotationsModifiers[this.acceptedAnnotationsCount] = modifiers;
						this.acceptedAnnotations[this.acceptedAnnotationsCount++] = acceptedAnnotation;
						break;
					case ClassFileConstants.AccEnum:
						char[][] acceptedEnum = new char[2][];
						acceptedEnum[0] = packageName;
						acceptedEnum[1] = typeName;

						if(this.acceptedEnums == null) {
							this.acceptedEnums = new char[10][][];
							this.acceptedEnumsModifiers = new int[10];
							this.acceptedEnumsCount = 0;
						}
						length = this.acceptedEnums.length;
						if(length == this.acceptedEnumsCount) {
							int newLength = (length + 1)* 2;
							System.arraycopy(this.acceptedEnums, 0, this.acceptedEnums = new char[newLength][][], 0, length);
							System.arraycopy(this.acceptedEnumsModifiers, 0, this.acceptedEnumsModifiers = new int[newLength], 0, length);
						}
						this.acceptedEnumsModifiers[this.acceptedEnumsCount] = modifiers;
						this.acceptedEnums[this.acceptedEnumsCount++] = acceptedEnum;
						break;
					case ClassFileConstants.AccInterface:
						char[][] acceptedInterface= new char[2][];
						acceptedInterface[0] = packageName;
						acceptedInterface[1] = typeName;

						if(this.acceptedInterfaces == null) {
							this.acceptedInterfaces = new char[10][][];
							this.acceptedInterfacesModifiers = new int[10];
							this.acceptedInterfacesCount = 0;
						}
						length = this.acceptedInterfaces.length;
						if(length == this.acceptedInterfacesCount) {
							int newLength = (length + 1)* 2;
							System.arraycopy(this.acceptedInterfaces, 0, this.acceptedInterfaces = new char[newLength][][], 0, length);
							System.arraycopy(this.acceptedInterfacesModifiers, 0, this.acceptedInterfacesModifiers = new int[newLength], 0, length);
						}
						this.acceptedInterfacesModifiers[this.acceptedInterfacesCount] = modifiers;
						this.acceptedInterfaces[this.acceptedInterfacesCount++] = acceptedInterface;
						break;
					default:
						char[][] acceptedClass = new char[2][];
						acceptedClass[0] = packageName;
						acceptedClass[1] = typeName;

						if(this.acceptedClasses == null) {
							this.acceptedClasses = new char[10][][];
							this.acceptedClassesModifiers = new int[10];
							this.acceptedClassesCount = 0;
						}
						length = this.acceptedClasses.length;
						if(length == this.acceptedClassesCount) {
							int newLength = (length + 1)* 2;
							System.arraycopy(this.acceptedClasses, 0, this.acceptedClasses = new char[newLength][][], 0, length);
							System.arraycopy(this.acceptedClassesModifiers, 0, this.acceptedClassesModifiers = new int[newLength], 0, length);
						}
						this.acceptedClassesModifiers[this.acceptedClassesCount] = modifiers;
						this.acceptedClasses[this.acceptedClassesCount++] = acceptedClass;
						break;
				}
			} else {
				this.noProposal = false;
				this.requestor.acceptType(
					packageName,
					typeName,
					modifiers,
					false,
					null,
					this.actualSelectionStart,
					this.actualSelectionEnd);
				this.acceptedAnswer = true;
			}
		}
	}

	@Override
	public void acceptPackage(char[] packageName) {
		// implementation of interface method
	}

	private void acceptQualifiedTypes() {
		if(this.acceptedClasses != null){
			this.acceptedAnswer = true;
			for (int i = 0; i < this.acceptedClassesCount; i++) {
				this.noProposal = false;
				this.requestor.acceptType(
					this.acceptedClasses[i][0],
					this.acceptedClasses[i][1],
					this.acceptedClassesModifiers[i],
					false,
					null,
					this.actualSelectionStart,
					this.actualSelectionEnd);
			}
			this.acceptedClasses = null;
			this.acceptedClassesModifiers = null;
			this.acceptedClassesCount = 0;
		}
		if(this.acceptedInterfaces != null){
			this.acceptedAnswer = true;
			for (int i = 0; i < this.acceptedInterfacesCount; i++) {
				this.noProposal = false;
				this.requestor.acceptType(
					this.acceptedInterfaces[i][0],
					this.acceptedInterfaces[i][1],
					this.acceptedInterfacesModifiers[i],
					false,
					null,
					this.actualSelectionStart,
					this.actualSelectionEnd);
			}
			this.acceptedInterfaces = null;
			this.acceptedInterfacesModifiers = null;
			this.acceptedInterfacesCount = 0;
		}
		if(this.acceptedAnnotations != null){
			this.acceptedAnswer = true;
			for (int i = 0; i < this.acceptedAnnotationsCount; i++) {
				this.noProposal = false;
				this.requestor.acceptType(
					this.acceptedAnnotations[i][0],
					this.acceptedAnnotations[i][1],
					this.acceptedAnnotationsModifiers[i],
					false,
					null,
					this.actualSelectionStart,
					this.actualSelectionEnd);
			}
			this.acceptedAnnotations = null;
			this.acceptedAnnotationsModifiers = null;
			this.acceptedAnnotationsCount = 0;
		}
		if(this.acceptedEnums != null){
			this.acceptedAnswer = true;
			for (int i = 0; i < this.acceptedEnumsCount; i++) {
				this.noProposal = false;
				this.requestor.acceptType(
					this.acceptedEnums[i][0],
					this.acceptedEnums[i][1],
					this.acceptedEnumsModifiers[i],
					false,
					null,
					this.actualSelectionStart,
					this.actualSelectionEnd);
			}
			this.acceptedEnums = null;
			this.acceptedEnumsModifiers = null;
			this.acceptedEnumsCount = 0;
		}
	}
	private boolean checkSelection(
			char[] source,
			int selectionStart,
			int selectionEnd,
			boolean isModuleInfo) {

		Scanner scanner =
			new Scanner(
				false /*comment*/,
				false /*whitespace*/,
				false /*nls*/,
				this.compilerOptions.sourceLevel,
				this.compilerOptions.complianceLevel,
				null/*taskTag*/,
				null/*taskPriorities*/,
				true /*taskCaseSensitive*/,
				this.compilerOptions.enablePreviewFeatures);
		scanner.setSource(source);

		int lastIdentifierStart = -1;
		int lastIdentifierEnd = -1;
		char[] lastIdentifier = null;
		int token;

		if(selectionStart > selectionEnd){
			int end = source.length - 1;

			// compute start position of current line
			int currentPosition = selectionStart - 1;
			int nextCharacterPosition = selectionStart;
			char currentCharacter = ' ';
			try {
				lineLoop: while(currentPosition > 0){

					if(source[currentPosition] == '\\' && source[currentPosition+1] == 'u') {
						int pos = currentPosition + 2;
						int c1 = 0, c2 = 0, c3 = 0, c4 = 0;
						while (source[pos] == 'u') {
							pos++;
						}

						int endOfUnicode = pos + 3;
						if (end < endOfUnicode) {
							if (endOfUnicode < source.length) {
								end = endOfUnicode;
							} else {
								return false; // not enough characters to decode an unicode
							}
						}

						if ((c1 = ScannerHelper.getHexadecimalValue(source[pos++])) > 15
							|| c1 < 0
							|| (c2 = ScannerHelper.getHexadecimalValue(source[pos++])) > 15
							|| c2 < 0
							|| (c3 = ScannerHelper.getHexadecimalValue(source[pos++])) > 15
							|| c3 < 0
							|| (c4 = ScannerHelper.getHexadecimalValue(source[pos++])) > 15
							|| c4 < 0) {
							return false;
						} else {
							currentCharacter = (char) (((c1 * 16 + c2) * 16 + c3) * 16 + c4);
							nextCharacterPosition = pos;
						}
					} else {
						currentCharacter = source[currentPosition];
						nextCharacterPosition = currentPosition+1;
					}

					switch(currentCharacter) {
						case '\r':
						case '\n':
						case '/':
						case '"':
						case '\'':
							break lineLoop;
						case '-':
							if (source[nextCharacterPosition] == '>') {
								nextCharacterPosition--; // nextCharacterPosition = currentPosition
								break lineLoop;
							}
							break;
						case ':':
							if (source[nextCharacterPosition] == ':') {
								nextCharacterPosition--; // nextCharacterPosition = currentPosition
								break lineLoop;
							}
					}
					currentPosition--;
				}
			} catch (ArrayIndexOutOfBoundsException e) {
				return false;
			}

			// compute start and end of the last token
			scanner.resetTo(nextCharacterPosition, end, isModuleInfo);
			isolateLastName: do {
				try {
					token = scanner.getNextToken();
				} catch (InvalidInputException e) {
					return false;
				}
				switch (token) {
					case TerminalTokens.TokenNamethis:
					case TerminalTokens.TokenNamesuper:
					case TerminalTokens.TokenNamenew:
					case TerminalTokens.TokenNameIdentifier:
						if (scanner.startPosition <= selectionStart && selectionStart <= scanner.currentPosition) {
							if (scanner.currentPosition == scanner.eofPosition) {
								int temp = scanner.eofPosition;
								scanner.eofPosition = scanner.source.length;
							 	while(scanner.getNextCharAsJavaIdentifierPart()){/*empty*/}
							 	scanner.eofPosition = temp;
							}
							lastIdentifierStart = scanner.startPosition;
							lastIdentifierEnd = scanner.currentPosition - 1;
							lastIdentifier = scanner.getCurrentTokenSource();
							break isolateLastName;
						}
						break;
					case TerminalTokens.TokenNameARROW:
					case TerminalTokens.TokenNameCOLON_COLON:
						if (scanner.startPosition <= selectionStart && selectionStart <= scanner.currentPosition) {
							lastIdentifierStart = scanner.startPosition;
							lastIdentifierEnd = scanner.currentPosition - 1;
							lastIdentifier = scanner.getCurrentTokenSource();
							break isolateLastName;
						}
						break;
				}
			} while (token != TerminalTokens.TokenNameEOF);
		} else {
			if (selectionStart == selectionEnd) { // Widen the selection to scan -> || :: if needed. No unicode handling for now.
				if (selectionStart > 0 && selectionEnd < source.length - 1) {
					if ((source[selectionStart] == '>' && source[selectionStart - 1] == '-') ||
							source[selectionStart] == ':' && source[selectionStart - 1] == ':') {
						selectionStart--;
					} else {
						if ((source[selectionStart] == '-' && source[selectionEnd + 1] == '>') ||
								source[selectionStart] == ':' && source[selectionEnd + 1] == ':') {
							selectionEnd++;
						}
					}
				}
			} // there could be some innocuous widening, shouldn't matter.
			scanner.resetTo(selectionStart, selectionEnd, isModuleInfo);

			boolean expectingIdentifier = true;
			do {
				try {
					token = scanner.getNextToken();
				} catch (InvalidInputException e) {
					return false;
				}
				switch (token) {
					case TerminalTokens.TokenNamethis :
					case TerminalTokens.TokenNamesuper :
					case TerminalTokens.TokenNamenew :
					case TerminalTokens.TokenNameIdentifier :
						if (!expectingIdentifier)
							return false;
						lastIdentifier = scanner.getCurrentTokenSource();
						lastIdentifierStart = scanner.startPosition;
						lastIdentifierEnd = scanner.currentPosition - 1;
						if(lastIdentifierEnd > selectionEnd) {
							lastIdentifierEnd = selectionEnd;
							lastIdentifier = CharOperation.subarray(lastIdentifier, 0,lastIdentifierEnd - lastIdentifierStart + 1);
						}
						expectingIdentifier = false;
						break;
					case TerminalTokens.TokenNameCOLON_COLON:
						if (selectionStart >= scanner.startPosition && selectionEnd < scanner.currentPosition) {
							this.actualSelectionStart = selectionStart;
							this.actualSelectionEnd = selectionEnd;
							this.selectedIdentifier = CharOperation.NO_CHAR;
							return true;
						}
						//$FALL-THROUGH$
					case TerminalTokens.TokenNameDOT :
						if (expectingIdentifier)
							return false;
						expectingIdentifier = true;
						break;
					case TerminalTokens.TokenNameEOF :
						if (expectingIdentifier)
							return false;
						break;
					case TerminalTokens.TokenNameLESS :
						if(!checkTypeArgument(scanner))
							return false;
						break;
					case TerminalTokens.TokenNameAT:
						if(scanner.startPosition != scanner.initialPosition)
							return false;
						break;
					case TerminalTokens.TokenNameARROW:
						this.actualSelectionStart = selectionStart;
						this.actualSelectionEnd = selectionEnd;
						this.selectedIdentifier = CharOperation.NO_CHAR;
						return true;
					case TerminalTokens.TokenNameLPAREN:
					case TerminalTokens.TokenNameRPAREN:
						break;
					default :
						return false;
				}
			} while (token != TerminalTokens.TokenNameEOF);
		}
		if (lastIdentifierStart > 0) {
			this.actualSelectionStart = lastIdentifierStart;
			this.actualSelectionEnd = lastIdentifierEnd;
			this.selectedIdentifier = lastIdentifier;
			return true;
		}
		return false;
	}
	private boolean checkTypeArgument(Scanner scanner) {
		int depth = 1;
		int token;
		StringBuilder buffer = new StringBuilder();
		do {
			try {
				token = scanner.getNextToken();
			} catch (InvalidInputException e) {
				return false;
			}
			switch(token) {
				case TerminalTokens.TokenNameLESS :
					depth++;
					buffer.append(scanner.getCurrentTokenSource());
					break;
				case TerminalTokens.TokenNameGREATER :
					depth--;
					buffer.append(scanner.getCurrentTokenSource());
					break;
				case TerminalTokens.TokenNameRIGHT_SHIFT :
					depth-=2;
					buffer.append(scanner.getCurrentTokenSource());
					break;
				case TerminalTokens.TokenNameUNSIGNED_RIGHT_SHIFT :
					depth-=3;
					buffer.append(scanner.getCurrentTokenSource());
					break;
				case TerminalTokens.TokenNameextends :
				case TerminalTokens.TokenNamesuper :
					buffer.append(' ');
					buffer.append(scanner.getCurrentTokenSource());
					buffer.append(' ');
					break;
				case TerminalTokens.TokenNameCOMMA :
					if(depth == 1) {
						int length = buffer.length();
						char[] typeRef = new char[length];
						buffer.getChars(0, length, typeRef, 0);
						try {
							Signature.createTypeSignature(typeRef, true);
							buffer = new StringBuilder();
						} catch(IllegalArgumentException e) {
							return false;
						}
					}
					break;
				default :
					buffer.append(scanner.getCurrentTokenSource());
					break;

			}
			if(depth < 0) {
				return false;
			}
		} while (depth != 0 && token != TerminalTokens.TokenNameEOF);

		if(depth == 0) {
			int length = buffer.length() - 1;
			char[] typeRef = new char[length];
			buffer.getChars(0, length, typeRef, 0);
			try {
				Signature.createTypeSignature(typeRef, true);
				return true;
			} catch(IllegalArgumentException e) {
				return false;
			}
		}

		return false;
	}

	/*
	 * find all types outside the project scope
	 */
	private void findAllTypes(char[] prefix) {
		try {
			IProgressMonitor progressMonitor = new IProgressMonitor() {
				boolean isCanceled = false;
				@Override
				public void beginTask(String name, int totalWork) {
					// implements interface method
				}
				@Override
				public void done() {
					// implements interface method
				}
				@Override
				public void internalWorked(double work) {
					// implements interface method
				}
				@Override
				public boolean isCanceled() {
					return this.isCanceled;
				}
				@Override
				public void setCanceled(boolean value) {
					this.isCanceled = value;
				}
				@Override
				public void setTaskName(String name) {
					// implements interface method
				}
				@Override
				public void subTask(String name) {
					// implements interface method
				}
				@Override
				public void worked(int work) {
					// implements interface method
				}
			};

			TypeNameMatchRequestor typeNameMatchRequestor = new TypeNameMatchRequestor() {
				@Override
				public void acceptTypeNameMatch(TypeNameMatch match) {
					if (SelectionEngine.this.requestor instanceof SelectionRequestor) {
						SelectionEngine.this.noProposal = false;
						((SelectionRequestor)SelectionEngine.this.requestor).acceptType(match.getType());
					}
				}
			};

			IJavaSearchScope scope = BasicSearchEngine.createWorkspaceScope();

			SelectionTypeNameMatchRequestorWrapper requestorWrapper =
				new SelectionTypeNameMatchRequestorWrapper(
						typeNameMatchRequestor,
						scope,
						this.unitScope == null ? null : this.unitScope.referenceContext.imports);

			org.eclipse.jdt.core.ICompilationUnit[] workingCopies = this.owner == null ? null : JavaModelManager.getJavaModelManager().getWorkingCopies(this.owner, true/*add primary WCs*/);

			try {
				new BasicSearchEngine(workingCopies).searchAllTypeNames(
					null,
					SearchPattern.R_EXACT_MATCH,
					prefix,
					SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE,
					IJavaSearchConstants.TYPE,
					scope,
					requestorWrapper,
					IJavaSearchConstants.CANCEL_IF_NOT_READY_TO_SEARCH,
					progressMonitor);
			} catch (OperationCanceledException e) {
				// do nothing
			}
			requestorWrapper.acceptNotImported();
		} catch (JavaModelException e) {
			// do nothing
		}
	}

	@Override
	public AssistParser getParser() {
		return this.parser;
	}

	/*
	 * Returns whether the given binding is a local/anonymous reference binding, or if its declaring class is
	 * local.
	 */
	private boolean isLocal(ReferenceBinding binding) {
		if(binding instanceof ParameterizedTypeBinding) {
			return isLocal(((ParameterizedTypeBinding)binding).genericType());
		}
		if (!(binding instanceof SourceTypeBinding)) return false;
		if (binding instanceof LocalTypeBinding) return true;
		if (binding instanceof MemberTypeBinding) {
			return isLocal(((MemberTypeBinding)binding).enclosingType);
		}
		return false;
	}

	/**
	 * Ask the engine to compute the selection at the specified position
	 * of the given compilation unit.

	 *  @param sourceUnit org.eclipse.jdt.internal.compiler.env.ICompilationUnit
	 *      the source of the current compilation unit.
	 *
	 *  @param selectionSourceStart int
	 *  @param selectionSourceEnd int
	 *      a range in the source where the selection is.
	 */
	public void select(
		ICompilationUnit sourceUnit,
		int selectionSourceStart,
		int selectionSourceEnd) {

		char[] source = sourceUnit.getContents();

		if(DEBUG) {
			System.out.print("SELECTION IN "); //$NON-NLS-1$
			System.out.print(sourceUnit.getFileName());
			System.out.print(" FROM "); //$NON-NLS-1$
			System.out.print(selectionSourceStart);
			System.out.print(" TO "); //$NON-NLS-1$
			System.out.println(selectionSourceEnd);
			System.out.println("SELECTION - Source :"); //$NON-NLS-1$
			System.out.println(source);
		}
		boolean isModuleInfo = CharOperation.endsWith(sourceUnit.getFileName(), TypeConstants.MODULE_INFO_FILE_NAME);
		if (!checkSelection(source, selectionSourceStart, selectionSourceEnd, isModuleInfo)) {
			return;
		}
		if (DEBUG) {
			System.out.print("SELECTION - Checked : \""); //$NON-NLS-1$
			System.out.print(new String(source, this.actualSelectionStart, this.actualSelectionEnd-this.actualSelectionStart+1));
			System.out.println('"');
		}
		try {
			this.acceptedAnswer = false;
			CompilationResult result = new CompilationResult(sourceUnit, 1, 1, this.compilerOptions.maxProblemsPerUnit);
			CompilationUnitDeclaration parsedUnit =
				this.parser.dietParse(sourceUnit, result, this.actualSelectionStart, this.actualSelectionEnd);

			if (parsedUnit != null) {
				if(DEBUG) {
					System.out.println("SELECTION - Diet AST :"); //$NON-NLS-1$
					System.out.println(parsedUnit.toString());
				}

				// scan the package & import statements first
				if (parsedUnit.currentPackage instanceof SelectionOnPackageReference) {
					char[][] tokens =
						((SelectionOnPackageReference) parsedUnit.currentPackage).tokens;
					this.noProposal = false;
					this.requestor.acceptPackage(CharOperation.concatWith(tokens, '.'));
					return;
				}
				ImportReference[] imports = parsedUnit.imports;
				if (imports != null) {
					for (int i = 0, length = imports.length; i < length; i++) {
						ImportReference importReference = imports[i];
						if (importReference instanceof SelectionOnImportReference) {
							char[][] tokens = ((SelectionOnImportReference) importReference).tokens;
							this.noProposal = false;
							this.requestor.acceptPackage(CharOperation.concatWith(tokens, '.'));
							this.nameEnvironment.findTypes(CharOperation.concatWith(tokens, '.'), false, false, IJavaSearchConstants.TYPE, this);

							this.lookupEnvironment.buildTypeBindings(parsedUnit, null /*no access restriction*/);
							if ((this.unitScope = parsedUnit.scope) != null) {
								int tokenCount = tokens.length;
								char[] lastToken = tokens[tokenCount - 1];
								char[][] qualifierTokens = CharOperation.subarray(tokens, 0, tokenCount - 1);

								if(qualifierTokens != null && qualifierTokens.length > 0) {
									Binding binding = this.unitScope.getTypeOrPackage(qualifierTokens);
									if(binding != null && binding instanceof ReferenceBinding) {
										ReferenceBinding ref = (ReferenceBinding) binding;
										selectMemberTypeFromImport(parsedUnit, lastToken, ref, importReference.isStatic());
										if(importReference.isStatic()) {
											selectStaticFieldFromStaticImport(parsedUnit, lastToken, ref);
											selectStaticMethodFromStaticImport(parsedUnit, lastToken, ref);
										}
									}
								}
							}

							// accept qualified types only if no unqualified type was accepted
							if(!this.acceptedAnswer) {
								acceptQualifiedTypes();
								if (!this.acceptedAnswer) {
									this.nameEnvironment.findTypes(this.selectedIdentifier, false, false, IJavaSearchConstants.TYPE, this);
									// try with simple type name
									if(!this.acceptedAnswer) {
										acceptQualifiedTypes();
									}
								}
							}
							if(this.noProposal && this.problem != null) {
								this.requestor.acceptError(this.problem);
							}
							return;
						}
					}
				}
				try {
					if (parsedUnit.isModuleInfo() && parsedUnit.moduleDeclaration != null) {
						ModuleDeclaration module = parsedUnit.moduleDeclaration;
						this.lookupEnvironment.buildTypeBindings(parsedUnit, null /*no access restriction*/);
						if ((this.unitScope = parsedUnit.scope)  != null) {
							this.lookupEnvironment.completeTypeBindings(parsedUnit, true);
						}
						module.resolveModuleDirectives(parsedUnit.scope);
						module.resolvePackageDirectives(parsedUnit.scope);
						module.resolveTypeDirectives(parsedUnit.scope);
						acceptPackageVisibilityStatements(module.exports, parsedUnit.scope);
						acceptPackageVisibilityStatements(module.opens, parsedUnit.scope);
					} else if (parsedUnit.types != null || parsedUnit.isPackageInfo()) {
						if(selectDeclaration(parsedUnit))
							return;
						this.lookupEnvironment.buildTypeBindings(parsedUnit, null /*no access restriction*/);
						if ((this.unitScope = parsedUnit.scope)  != null) {
							this.lookupEnvironment.completeTypeBindings(parsedUnit, true);
							CompilationUnitDeclaration previousUnitBeingCompleted = this.lookupEnvironment.unitBeingCompleted;
							this.lookupEnvironment.unitBeingCompleted = parsedUnit;
							parsedUnit.scope.faultInTypes();
							this.lookupEnvironment.unitBeingCompleted = previousUnitBeingCompleted;
							ASTNode node = null;
							if (parsedUnit.types != null)
								node = parseBlockStatements(parsedUnit, selectionSourceStart);
							if(DEBUG) {
								System.out.println("SELECTION - AST :"); //$NON-NLS-1$
								System.out.println(parsedUnit.toString());
							}
							parsedUnit.resolve();
							if (node != null) {
								selectLocalDeclaration(node);
							}
						}
					}
				} catch (SelectionNodeFound e) {
					if (e.binding != null) {
						if(DEBUG) {
							System.out.println("SELECTION - Selection binding:"); //$NON-NLS-1$
							System.out.println(e.binding.toString());
						}
						// if null then we found a problem in the selection node
						selectFrom(e.binding, parsedUnit, sourceUnit, e.isDeclaration);
					}
				}
			}
			// only reaches here if no selection could be derived from the parsed tree
			// thus use the selected source and perform a textual type search
			if (!this.acceptedAnswer) {
				this.nameEnvironment.findTypes(this.selectedIdentifier, false, false, IJavaSearchConstants.TYPE, this);

				// accept qualified types only if no unqualified type was accepted
				if(!this.acceptedAnswer) {
					acceptQualifiedTypes();

					// accept types from all the workspace only if no type was found in the project scope
					if (this.noProposal) {
						findAllTypes(this.selectedIdentifier);
					}
				}
			}
			if(this.noProposal && this.problem != null) {
				this.requestor.acceptError(this.problem);
			}
		} catch (IndexOutOfBoundsException | AbortCompilation e) { // ignore this exception for now since it typically means we cannot find java.lang.Object
			if(DEBUG) {
				System.out.println("Exception caught by SelectionEngine:"); //$NON-NLS-1$
				e.printStackTrace(System.out);
			}
		} finally {
			reset(true);
		}
	}

	private void acceptPackageVisibilityStatements(PackageVisibilityStatement[] pvs, Scope scope) {
		if (pvs != null) {
			for (PackageVisibilityStatement pv : pvs) {
				if (pv.pkgRef instanceof SelectionOnPackageVisibilityReference) {
					this.noProposal = false;
					this.requestor.acceptPackage(CharOperation.concatWith(((SelectionOnPackageVisibilityReference) pv.pkgRef).tokens, '.'));
				}
			}
		}
	}
	private void selectMemberTypeFromImport(CompilationUnitDeclaration parsedUnit, char[] lastToken, ReferenceBinding ref, boolean staticOnly) {
		int fieldLength = lastToken.length;
		ReferenceBinding[] memberTypes = ref.memberTypes();
		next : for (int j = 0; j < memberTypes.length; j++) {
			ReferenceBinding memberType = memberTypes[j];

			if (fieldLength > memberType.sourceName.length)
				continue next;

			if (staticOnly && !memberType.isStatic())
				continue next;

			if (!CharOperation.equals(lastToken, memberType.sourceName, true))
				continue next;

			selectFrom(memberType, parsedUnit, false);
		}
	}

	private void selectStaticFieldFromStaticImport(CompilationUnitDeclaration parsedUnit, char[] lastToken, ReferenceBinding ref) {
		int fieldLength = lastToken.length;
		FieldBinding[] fields = ref.availableFields();
		next : for (int j = 0; j < fields.length; j++) {
			FieldBinding field = fields[j];

			if (fieldLength > field.name.length)
				continue next;

			if (field.isSynthetic())
				continue next;

			if (!field.isStatic())
				continue next;

			if (!CharOperation.equals(lastToken, field.name, true))
				continue next;

			selectFrom(field, parsedUnit, false);
		}
	}

	private void selectStaticMethodFromStaticImport(CompilationUnitDeclaration parsedUnit, char[] lastToken, ReferenceBinding ref) {
		int methodLength = lastToken.length;
		MethodBinding[] methods = ref.availableMethods();
		next : for (int j = 0; j < methods.length; j++) {
			MethodBinding method = methods[j];

			if (method.isSynthetic()) continue next;

			if (method.isDefaultAbstract())	continue next;

			if (method.isConstructor()) continue next;

			if (!method.isStatic()) continue next;

			if (methodLength > method.selector.length)
				continue next;

			if (!CharOperation.equals(lastToken, method.selector, true))
				continue next;

			selectFrom(method, parsedUnit, false);
		}
	}

	private void selectFrom(Binding binding, CompilationUnitDeclaration parsedUnit, boolean isDeclaration) {
		selectFrom(binding, parsedUnit, null, isDeclaration);
	}
	private void selectFrom(Binding binding, CompilationUnitDeclaration parsedUnit, ICompilationUnit unit, boolean isDeclaration) {
		if(binding instanceof TypeVariableBinding) {
			TypeVariableBinding typeVariableBinding = (TypeVariableBinding) binding;
			Binding enclosingElement = typeVariableBinding.declaringElement;
			this.noProposal = false;

			if(enclosingElement instanceof SourceTypeBinding) {
				SourceTypeBinding enclosingType = (SourceTypeBinding) enclosingElement;
				if (isLocal(enclosingType) && this.requestor instanceof SelectionRequestor) {
					((SelectionRequestor)this.requestor).acceptLocalTypeParameter(typeVariableBinding);
				} else {
					this.requestor.acceptTypeParameter(
						enclosingType.qualifiedPackageName(),
						enclosingType.qualifiedSourceName(),
						typeVariableBinding.sourceName(),
						false,
						this.actualSelectionStart,
						this.actualSelectionEnd);
				}
			} else if(enclosingElement instanceof MethodBinding) {
				MethodBinding enclosingMethod = (MethodBinding) enclosingElement;
				if (isLocal(enclosingMethod.declaringClass) && this.requestor instanceof SelectionRequestor) {
					((SelectionRequestor)this.requestor).acceptLocalMethodTypeParameter(typeVariableBinding);
				} else {
					this.requestor.acceptMethodTypeParameter(
						enclosingMethod.declaringClass.qualifiedPackageName(),
						enclosingMethod.declaringClass.qualifiedSourceName(),
						enclosingMethod.isConstructor()
								? enclosingMethod.declaringClass.sourceName()
								: enclosingMethod.selector,
						enclosingMethod.sourceStart(),
						enclosingMethod.sourceEnd(),
						typeVariableBinding.sourceName(),
						false,
						this.actualSelectionStart,
						this.actualSelectionEnd);
				}
			}
			this.acceptedAnswer = true;
		} else if (binding instanceof ReferenceBinding) {
			ReferenceBinding typeBinding = (ReferenceBinding) binding;
			if(typeBinding instanceof ProblemReferenceBinding) {
				TypeBinding closestMatch = typeBinding.closestMatch();
				if (closestMatch instanceof ReferenceBinding) {
					typeBinding = (ReferenceBinding) closestMatch;
				} else {
					typeBinding = null;
				}
			}
			if (typeBinding == null) return;
			if (isLocal(typeBinding) && this.requestor instanceof SelectionRequestor) {
				this.noProposal = false;
				((SelectionRequestor)this.requestor).acceptLocalType(typeBinding);
			} else if (binding instanceof IntersectionTypeBinding18) {
				IntersectionTypeBinding18 intersection = (IntersectionTypeBinding18) binding;
				ReferenceBinding[] intersectingTypes = intersection.intersectingTypes;
				for (ReferenceBinding referenceBinding : intersectingTypes) {
					selectFrom(referenceBinding, parsedUnit, isDeclaration);
				}
			} else {
				this.noProposal = false;

				this.requestor.acceptType(
					typeBinding.qualifiedPackageName(),
					typeBinding.qualifiedSourceName(),
					typeBinding.modifiers,
					false,
					typeBinding.computeUniqueKey(),
					this.actualSelectionStart,
					this.actualSelectionEnd);
			}
			this.acceptedAnswer = true;
		} else if (binding instanceof MethodBinding) {
			this.noProposal = false;

			// when a full lambda expression is selected we will find the paser.assistedNode to represent that
			// lambda expression. So we use that state to differentiate hover over "->" vs full lambda expression selection.
			if(this.parser.assistNode instanceof LambdaExpression) {
				LambdaExpression lambdaExpr = (LambdaExpression) this.parser.assistNode;
				SyntheticMethodBinding methodBinding = new SyntheticMethodBinding(lambdaExpr,
						CharOperation.concat(TypeConstants.ANONYMOUS_METHOD, Integer.toString(lambdaExpr.ordinal).toCharArray()), (SourceTypeBinding)lambdaExpr.binding.declaringClass);
				if(this.requestor instanceof SelectionRequestor) {
					((SelectionRequestor)this.requestor).acceptLambdaMethod(methodBinding, unit);
					this.acceptedAnswer = true;
					return;
				}
			}

			MethodBinding methodBinding = getCorrectMethodBinding((MethodBinding) binding);

			boolean isValuesOrValueOf = false;
			if(binding instanceof SyntheticMethodBinding) {
				SyntheticMethodBinding syntheticMethodBinding = (SyntheticMethodBinding) binding;
				if(syntheticMethodBinding.purpose  == SyntheticMethodBinding.EnumValues
						|| syntheticMethodBinding.purpose  == SyntheticMethodBinding.EnumValueOf) {
					isValuesOrValueOf =  true;
				}
			}

			if(!isValuesOrValueOf && !methodBinding.isSynthetic()) {
				TypeBinding[] parameterTypes = methodBinding.original().parameters;
				int length = parameterTypes.length;
				char[][] parameterPackageNames = new char[length][];
				char[][] parameterTypeNames = new char[length][];
				String[] parameterSignatures = new String[length];
				for (int i = 0; i < length; i++) {
					parameterPackageNames[i] = parameterTypes[i].qualifiedPackageName();
					parameterTypeNames[i] = parameterTypes[i].qualifiedSourceName();
					parameterSignatures[i] = new String(getSignature(parameterTypes[i])).replace('/', '.');
				}

				TypeVariableBinding[] typeVariables = methodBinding.original().typeVariables;
				length = typeVariables == null ? 0 : typeVariables.length;
				char[][] typeParameterNames = new char[length][];
				char[][][] typeParameterBoundNames = new char[length][][];
				for (int i = 0; i < length; i++) {
					TypeVariableBinding typeVariable = typeVariables[i];
					typeParameterNames[i] = typeVariable.sourceName;
					if (typeVariable.firstBound == null) {
						typeParameterBoundNames[i] = new char[0][];
					} else if (TypeBinding.equalsEquals(typeVariable.firstBound, typeVariable.superclass)) {
						int boundCount = 1 + (typeVariable.superInterfaces == null ? 0 : typeVariable.superInterfaces.length);
						typeParameterBoundNames[i] = new char[boundCount][];
						typeParameterBoundNames[i][0] = typeVariable.superclass.sourceName;
						for (int j = 1; j < boundCount; j++) {
							typeParameterBoundNames[i][j] = typeVariables[i].superInterfaces[j - 1].sourceName;
						}
					} else {
						int boundCount = typeVariable.superInterfaces == null ? 0 : typeVariable.superInterfaces.length;
						typeParameterBoundNames[i] = new char[boundCount][];
						for (int j = 0; j < boundCount; j++) {
							typeParameterBoundNames[i][j] = typeVariables[i].superInterfaces[j].sourceName;
						}
					}
				}

				ReferenceBinding declaringClass = methodBinding.declaringClass;
				if (isLocal(declaringClass) && this.requestor instanceof SelectionRequestor) {
					((SelectionRequestor)this.requestor).acceptLocalMethod(methodBinding);
				} else {
					this.requestor.acceptMethod(
						declaringClass.qualifiedPackageName(),
						declaringClass.qualifiedSourceName(),
						declaringClass.enclosingType() == null ? null : new String(getSignature(declaringClass.enclosingType())),
						methodBinding.isConstructor()
							? declaringClass.sourceName()
							: methodBinding.selector,
						parameterPackageNames,
						parameterTypeNames,
						parameterSignatures,
						typeParameterNames,
						typeParameterBoundNames,
						methodBinding.isConstructor(),
						isDeclaration,
						methodBinding.computeUniqueKey(),
						this.actualSelectionStart,
						this.actualSelectionEnd);
				}
			}
			this.acceptedAnswer = true;
		} else if (binding instanceof FieldBinding) {
			FieldBinding fieldBinding = (FieldBinding) binding;
			ReferenceBinding declaringClass = fieldBinding.declaringClass;
			if (declaringClass != null) { // arraylength
				this.noProposal = false;
				if (isLocal(declaringClass) && this.requestor instanceof SelectionRequestor) {
					((SelectionRequestor)this.requestor).acceptLocalField(fieldBinding);
				} else {
					// if the binding is a problem field binding, we want to make sure
					// we can retrieve the closestMatch if the problem reason is NotVisible
					FieldBinding currentFieldBinding = fieldBinding;
					while (currentFieldBinding instanceof ProblemFieldBinding) {
						ProblemFieldBinding problemFieldBinding = (ProblemFieldBinding) currentFieldBinding;
						if (problemFieldBinding.problemId() == ProblemReasons.NotVisible) {
							currentFieldBinding = problemFieldBinding.closestMatch;
						} else {
							currentFieldBinding = null;
						}
					}
					char[] fieldName = null;
					char[] key = null;
					if (currentFieldBinding != null) {
						fieldName = currentFieldBinding.name;
						key = currentFieldBinding.computeUniqueKey();
					} else {
						fieldName = fieldBinding.name;
						key = fieldBinding.computeUniqueKey();
					}
					this.requestor.acceptField(
						declaringClass.qualifiedPackageName(),
						declaringClass.qualifiedSourceName(),
						fieldName,
						false,
						key,
						this.actualSelectionStart,
						this.actualSelectionEnd);
				}
				this.acceptedAnswer = true;
			}
		} else if (binding instanceof LocalVariableBinding) {
			if (this.requestor instanceof SelectionRequestor) {
				((SelectionRequestor)this.requestor).acceptLocalVariable((LocalVariableBinding)binding, unit);
				this.acceptedAnswer = true;
			} else {
				// open on the type of the variable
				selectFrom(((LocalVariableBinding) binding).type, parsedUnit, false);
			}
		} else if (binding instanceof ArrayBinding) {
			selectFrom(((ArrayBinding) binding).leafComponentType, parsedUnit, false);
			// open on the type of the array
		} else if (binding instanceof PackageBinding) {
			PackageBinding packageBinding = (PackageBinding) binding;
			this.noProposal = false;
			this.requestor.acceptPackage(packageBinding.readableName());
			this.acceptedAnswer = true;
		} else if(binding instanceof BaseTypeBinding) {
			this.acceptedAnswer = true;
		} else if (binding instanceof ModuleBinding) {
			this.noProposal = false;
			ModuleBinding moduleBinding = (ModuleBinding) binding;
			this.requestor.acceptModule(
					moduleBinding.moduleName,
					moduleBinding.computeUniqueKey(),
					this.actualSelectionStart,
					this.actualSelectionEnd);
			this.acceptedAnswer = true;
		}
	}
	/*
	 * Checks if a local declaration got selected in this method/initializer/field.
	 */
	private void selectLocalDeclaration(ASTNode node) {
		// the selected identifier is not identical to the parser one (equals but not identical),
		// for traversing the parse tree, the parser assist identifier is necessary for identitiy checks
		final char[] assistIdentifier = getParser().assistIdentifier();
		if (assistIdentifier == null) return;
		final BlockScope nodeScope;
		if (node instanceof AbstractMethodDeclaration) {
			nodeScope = ((AbstractMethodDeclaration)node).scope;
		} else {
			nodeScope = null;
		}

		class Visitor extends ASTVisitor {
			@Override
			public boolean visit(ConstructorDeclaration constructorDeclaration, ClassScope scope) {
				if (constructorDeclaration.selector == assistIdentifier){
					if (constructorDeclaration.binding != null) {
						throw new SelectionNodeFound(constructorDeclaration.binding);
					} else {
						if (constructorDeclaration.scope != null) {
							throw new SelectionNodeFound(new MethodBinding(constructorDeclaration.modifiers, constructorDeclaration.selector, null, null, null, constructorDeclaration.scope.referenceType().binding));
						}
					}
				}
				return true;
			}
			@Override
			public boolean visit(
		    		LocalDeclaration localDeclaration, BlockScope scope) {
				if(localDeclaration instanceof SelectionOnLocalName) {
					localDeclaration.resolve(scope);
				}
				if (localDeclaration.type instanceof SingleTypeReference && ((SingleTypeReference)localDeclaration.type).token == assistIdentifier) {
					if(localDeclaration.binding != null) {
						throw new SelectionNodeFound(localDeclaration.binding.type);
					} else {
						throw new SelectionNodeFound();
					}
				}
				return true; // do nothing by default, keep traversing
			}
			@Override
			public boolean visit(FieldDeclaration fieldDeclaration, MethodScope scope) {
				if (fieldDeclaration.name == assistIdentifier){
					throw new SelectionNodeFound(fieldDeclaration.binding);
				}
				return true;
			}
			@Override
			public boolean visit(TypeDeclaration localTypeDeclaration, BlockScope scope) {
				if (localTypeDeclaration.name == assistIdentifier) {
					throw new SelectionNodeFound(localTypeDeclaration.binding);
				}
				return true;
			}
			@Override
			public boolean visit(TypeDeclaration memberTypeDeclaration, ClassScope scope) {
				if (memberTypeDeclaration.name == assistIdentifier) {
					throw new SelectionNodeFound(memberTypeDeclaration.binding);
				}
				return true;
			}
			@Override
			public boolean visit(MethodDeclaration methodDeclaration, ClassScope scope) {
				if (methodDeclaration.selector == assistIdentifier){
					if (methodDeclaration.binding != null) {
						throw new SelectionNodeFound(methodDeclaration.binding);
					} else {
						if (methodDeclaration.scope != null) {
							throw new SelectionNodeFound(new MethodBinding(methodDeclaration.modifiers, methodDeclaration.selector, null, null, null, methodDeclaration.scope.referenceType().binding));
						}
					}
				}
				return true;
			}
			@Override
			public boolean visit(MessageSend messageSend, BlockScope scope) {
				if (messageSend.selector == assistIdentifier && messageSend instanceof SelectionOnMessageSend) {
					if (scope == null) {
						scope = nodeScope;
					}
					if (scope != null) {
						messageSend.resolve(scope);
						throw new SelectionNodeFound(messageSend.binding);
					}
				}
				return true;
			}
			@Override
			public boolean visit(
		    		Argument argument, BlockScope scope) {
				if (argument.type instanceof SingleTypeReference && ((SingleTypeReference)argument.type).token == assistIdentifier)
					throw new SelectionNodeFound(argument.binding.type);
				return true; // do nothing by default, keep traversing
			}
			@Override
			public boolean visit(TypeDeclaration typeDeclaration, CompilationUnitScope scope) {
				if (typeDeclaration.name == assistIdentifier) {
					throw new SelectionNodeFound(typeDeclaration.binding);
				}
				return true;
			}
			@Override
			public boolean visit(TypeParameter typeParameter, BlockScope scope) {
				if (typeParameter.name == assistIdentifier) {
					throw new SelectionNodeFound(typeParameter.binding);
				}
				return true;
			}
			@Override
			public boolean visit(TypeParameter typeParameter, ClassScope scope) {
				if (typeParameter.name == assistIdentifier) {
					throw new SelectionNodeFound(typeParameter.binding);
				}
				return true;
			}
		}

		if (node instanceof AbstractMethodDeclaration) {
			((AbstractMethodDeclaration)node).traverse(new Visitor(), (ClassScope)null);
		} else {
			((FieldDeclaration)node).traverse(new Visitor(), (MethodScope)null);
		}
	}

	/**
	 * Asks the engine to compute the selection of the given type
	 * from the given context
	 *
	 *  @param typeName char[]
	 *      a type name which is to be resolved in the context of a compilation unit.
	 *		NOTE: the type name is supposed to be correctly reduced (no whitespaces, no unicodes left)
	 *
	 *  @param context org.eclipse.jdt.core.IType
	 *      the context in which code assist is invoked.
	 */
	public void selectType(char[] typeName, IType context) throws JavaModelException {
		try {
			this.acceptedAnswer = false;

			// only the type erasure are returned by IType.resolvedType(...)
			if (CharOperation.indexOf('<', typeName) != -1) {
				char[] typeSig = Signature.createCharArrayTypeSignature(typeName, false/*not resolved*/);
				typeSig = Signature.getTypeErasure(typeSig);
				typeName = Signature.toCharArray(typeSig);
			}

			CompilationUnitDeclaration parsedUnit = null;
			TypeDeclaration typeDeclaration = null;
			org.eclipse.jdt.core.ICompilationUnit cu = context.getCompilationUnit();
			if (cu != null) {
			 	IType[] topLevelTypes = cu.getTypes();
			 	int length = topLevelTypes.length;
			 	SourceTypeElementInfo[] topLevelInfos = new SourceTypeElementInfo[length];
			 	for (int i = 0; i < length; i++) {
					topLevelInfos[i] = (SourceTypeElementInfo) ((SourceType)topLevelTypes[i]).getElementInfo();
				}
				CompilationResult result = new CompilationResult((org.eclipse.jdt.internal.compiler.env.ICompilationUnit) cu, 1, 1, this.compilerOptions.maxProblemsPerUnit);
				int flags = SourceTypeConverter.FIELD_AND_METHOD | SourceTypeConverter.MEMBER_TYPE;
				if (context.isAnonymous() || context.isLocal())
					flags |= SourceTypeConverter.LOCAL_TYPE;
				parsedUnit =
					SourceTypeConverter.buildCompilationUnit(
							topLevelInfos,
							flags,
							this.parser.problemReporter(),
							result);
				if (parsedUnit != null && parsedUnit.types != null) {
					if(DEBUG) {
						System.out.println("SELECTION - Diet AST :"); //$NON-NLS-1$
						System.out.println(parsedUnit.toString());
					}
					// find the type declaration that corresponds to the original source type
					while (context.isLambda() && context.getParent() != null) {
						// It is easier to find the first enclosing proper type than the corresponding
						// lambda expression ast to add the selection node to.
						context = (IType) context.getParent().getAncestor(IJavaElement.TYPE);
					}
					typeDeclaration = new ASTNodeFinder(parsedUnit).findType(context);
				}
			} else { // binary type
				IOrdinaryClassFile iClassFile = context.getClassFile();
				if (iClassFile instanceof ClassFile) {
					ClassFile classFile = (ClassFile) iClassFile;
					ClassFileReader reader = null;
					if (classFile.getPackageFragmentRoot() instanceof JrtPackageFragmentRoot) {
						IBinaryType binaryTypeInfo = classFile.getBinaryTypeInfo();
						if (binaryTypeInfo instanceof ClassFileReader) {
							reader = (ClassFileReader) binaryTypeInfo;
						}
					} else {
						BinaryTypeDescriptor descriptor = BinaryTypeFactory.createDescriptor(classFile);
						try {
							reader = BinaryTypeFactory.rawReadType(descriptor, false/*don't fully initialize so as to keep constant pool (used below)*/);
						} catch (ClassFormatException e) {
							if (JavaCore.getPlugin().isDebugging()) {
								e.printStackTrace(System.err);
							}
						}
					}
					if (reader == null) {
						throw classFile.newNotPresentException();
					}
					CompilationResult result = new CompilationResult(reader.getFileName(), 1, 1, this.compilerOptions.maxProblemsPerUnit);
					parsedUnit = new CompilationUnitDeclaration(this.parser.problemReporter(), result, 0);
					HashSetOfCharArrayArray typeNames = new HashSetOfCharArrayArray();

					BinaryTypeConverter converter = new BinaryTypeConverter(this.parser.problemReporter(), result, typeNames);
					typeDeclaration = converter.buildTypeDeclaration(context, parsedUnit);
					parsedUnit.imports = converter.buildImports(reader);
				}
			}

			if (typeDeclaration != null) {

				// add fake field with the type we're looking for
				// note: since we didn't ask for fields above, there is no field defined yet
				FieldDeclaration field = new FieldDeclaration();
				int dot;
				if ((dot = CharOperation.lastIndexOf('.', typeName)) == -1) {
					this.selectedIdentifier = typeName;
					field.type = new SelectionOnSingleTypeReference(typeName, -1);
					// position not used
				} else {
					char[][] previousIdentifiers = CharOperation.splitOn('.', typeName, 0, dot);
					char[] selectionIdentifier =
						CharOperation.subarray(typeName, dot + 1, typeName.length);
					this.selectedIdentifier = selectionIdentifier;
					field.type =
						new SelectionOnQualifiedTypeReference(
							previousIdentifiers,
							selectionIdentifier,
							new long[previousIdentifiers.length + 1]);
				}
				field.name = "<fakeField>".toCharArray(); //$NON-NLS-1$
				typeDeclaration.fields = new FieldDeclaration[] { field };

				// build bindings
				this.lookupEnvironment.buildTypeBindings(parsedUnit, null /*no access restriction*/);
				if ((this.unitScope = parsedUnit.scope) != null) {
					try {
						// build fields
						// note: this builds fields only in the parsed unit (the buildFieldsAndMethods flag is not passed along)
						this.lookupEnvironment.completeTypeBindings(parsedUnit, true);

						// resolve
						parsedUnit.scope.faultInTypes();
						parsedUnit.resolve();
					} catch (SelectionNodeFound e) {
						if (e.binding != null) {
							if(DEBUG) {
								System.out.println("SELECTION - Selection binding :"); //$NON-NLS-1$
								System.out.println(e.binding.toString());
							}
							// if null then we found a problem in the selection node
							selectFrom(e.binding, parsedUnit, e.isDeclaration);
						}
					}
				}
			}
			if(this.noProposal && this.problem != null) {
				this.requestor.acceptError(this.problem);
			}
		} catch (AbortCompilation e) { // ignore this exception for now since it typically means we cannot find java.lang.Object
		} finally {
			reset(true);
		}
	}

	// Check if a declaration got selected in this unit
	private boolean selectDeclaration(CompilationUnitDeclaration compilationUnit){

		// the selected identifier is not identical to the parser one (equals but not identical),
		// for traversing the parse tree, the parser assist identifier is necessary for identitiy checks
		char[] assistIdentifier = getParser().assistIdentifier();
		if (assistIdentifier == null) return false;

		ImportReference currentPackage = compilationUnit.currentPackage;
		char[] packageName = currentPackage == null ? CharOperation.NO_CHAR : CharOperation.concatWith(currentPackage.tokens, '.');
		// iterate over the types
		TypeDeclaration[] types = compilationUnit.types;
		for (int i = 0, length = types == null ? 0 : types.length; i < length; i++){
			if(selectDeclaration(types[i], assistIdentifier, packageName))
				return true;
		}
		return false;
	}

	// Check if a declaration got selected in this type
	private boolean selectDeclaration(TypeDeclaration typeDeclaration, char[] assistIdentifier, char[] packageName){

		if (typeDeclaration.name == assistIdentifier){
			char[] qualifiedSourceName = null;

			TypeDeclaration enclosingType = typeDeclaration;
			while(enclosingType != null) {
				qualifiedSourceName = CharOperation.concat(enclosingType.name, qualifiedSourceName, '.');
				enclosingType = enclosingType.enclosingType;
			}
			char[] uniqueKey = typeDeclaration.binding != null ? typeDeclaration.binding.computeUniqueKey() : null;

			this.requestor.acceptType(
				packageName,
				qualifiedSourceName,
				typeDeclaration.modifiers,
				true,
				uniqueKey,
				this.actualSelectionStart,
				this.actualSelectionEnd);

			this.noProposal = false;
			return true;
		}
		TypeDeclaration[] memberTypes = typeDeclaration.memberTypes;
		for (int i = 0, length = memberTypes == null ? 0 : memberTypes.length; i < length; i++){
			if(selectDeclaration(memberTypes[i], assistIdentifier, packageName))
				return true;
		}
		FieldDeclaration[] fields = typeDeclaration.fields;
		for (int i = 0, length = fields == null ? 0 : fields.length; i < length; i++){
			if (fields[i].name == assistIdentifier){
				char[] qualifiedSourceName = null;

				TypeDeclaration enclosingType = typeDeclaration;
				while(enclosingType != null) {
					qualifiedSourceName = CharOperation.concat(enclosingType.name, qualifiedSourceName, '.');
					enclosingType = enclosingType.enclosingType;
				}
				FieldDeclaration field = fields[i];
				this.requestor.acceptField(
					packageName,
					qualifiedSourceName,
					field.name,
					true,
					field.binding != null ? field.binding.computeUniqueKey() : null,
					this.actualSelectionStart,
					this.actualSelectionEnd);

				this.noProposal = false;
				return true;
			}
		}
		AbstractMethodDeclaration[] methods = typeDeclaration.methods;
		for (int i = 0, length = methods == null ? 0 : methods.length; i < length; i++){
			AbstractMethodDeclaration method = methods[i];

			if (method.selector == assistIdentifier){
				char[] qualifiedSourceName = null;

				TypeDeclaration enclosingType = typeDeclaration;
				while(enclosingType != null) {
					qualifiedSourceName = CharOperation.concat(enclosingType.name, qualifiedSourceName, '.');
					enclosingType = enclosingType.enclosingType;
				}

				this.requestor.acceptMethod(
					packageName,
					qualifiedSourceName,
					null, // SelectionRequestor does not need of declaring type signature for method declaration
					method.selector,
					null, // SelectionRequestor does not need of parameters type for method declaration
					null, // SelectionRequestor does not need of parameters type for method declaration
					null, // SelectionRequestor does not need of parameters type for method declaration
					null, // SelectionRequestor does not need of type parameters name for method declaration
					null, // SelectionRequestor does not need of type parameters bounds for method declaration
					method.isConstructor(),
					true,
					method.binding != null ? method.binding.computeUniqueKey() : null,
					this.actualSelectionStart,
					this.actualSelectionEnd);

				this.noProposal = false;
				return true;
			}

			TypeParameter[] methodTypeParameters = method.typeParameters();
			for (int j = 0, length2 = methodTypeParameters == null ? 0 : methodTypeParameters.length; j < length2; j++){
				TypeParameter methodTypeParameter = methodTypeParameters[j];

				if(methodTypeParameter.name == assistIdentifier) {
					char[] qualifiedSourceName = null;

					TypeDeclaration enclosingType = typeDeclaration;
					while(enclosingType != null) {
						qualifiedSourceName = CharOperation.concat(enclosingType.name, qualifiedSourceName, '.');
						enclosingType = enclosingType.enclosingType;
					}

					this.requestor.acceptMethodTypeParameter(
						packageName,
						qualifiedSourceName,
						method.selector,
						method.sourceStart,
						method.sourceEnd,
						methodTypeParameter.name,
						true,
						this.actualSelectionStart,
						this.actualSelectionEnd);

					this.noProposal = false;
					return true;
				}
			}
		}

		TypeParameter[] typeParameters = typeDeclaration.typeParameters;
		for (int i = 0, length = typeParameters == null ? 0 : typeParameters.length; i < length; i++){
			TypeParameter typeParameter = typeParameters[i];
			if(typeParameter.name == assistIdentifier) {
				char[] qualifiedSourceName = null;

				TypeDeclaration enclosingType = typeDeclaration;
				while(enclosingType != null) {
					qualifiedSourceName = CharOperation.concat(enclosingType.name, qualifiedSourceName, '.');
					enclosingType = enclosingType.enclosingType;
				}

				this.requestor.acceptTypeParameter(
					packageName,
					qualifiedSourceName,
					typeParameter.name,
					true,
					this.actualSelectionStart,
					this.actualSelectionEnd);

				this.noProposal = false;
				return true;
			}
		}

		return false;
	}

	/*
	 * Returns the correct method binding according to whether the selection is on the method declaration
	 * or on the inheritDoc tag in its javadoc.
	 */
	private MethodBinding getCorrectMethodBinding(MethodBinding binding) {
		if (this.parser.javadocParser instanceof SelectionJavadocParser) {
			if (((SelectionJavadocParser)this.parser.javadocParser).inheritDocTagSelected){
				try {
					Object res = findMethodWithAttachedDocInHierarchy(binding);
					if (res instanceof MethodBinding) {
						return (MethodBinding) res;
					}
				} catch (JavaModelException e) {
					return null;
				}
			}
		}
		return binding;
	}

	protected MethodBinding findOverriddenMethodInType(ReferenceBinding overriddenType, MethodBinding overriding) throws JavaModelException {
		if (overriddenType == null)
			return null;
		MethodBinding[] overriddenMethods= overriddenType.availableMethods();
		LookupEnvironment lookupEnv = this.lookupEnvironment;
		if (lookupEnv != null && overriddenMethods != null) {
			for (int i= 0; i < overriddenMethods.length; i++) {
				if (lookupEnv.methodVerifier().isMethodSubsignature(overriding, overriddenMethods[i])) {
					return overriddenMethods[i];
				}
			}
		}
		return null;
	}

	private Object findMethodWithAttachedDocInHierarchy(final MethodBinding method) throws JavaModelException {
		ReferenceBinding type= method.declaringClass;
		final SelectionRequestor requestor1 = (SelectionRequestor) this.requestor;
		return new InheritDocVisitor() {
			@Override
			public Object visit(ReferenceBinding currType) throws JavaModelException {
				MethodBinding overridden =  findOverriddenMethodInType(currType, method);
				if (overridden == null)
					return InheritDocVisitor.CONTINUE;
				TypeBinding args[] = overridden.parameters;
				String names[] = new String[args.length];
				for (int i = 0; i < args.length; i++) {
					names[i] = Signature.createTypeSignature(args[i].sourceName(), false);
				}
				IMember member = (IMember) requestor1.findMethodFromBinding(overridden, names, overridden.declaringClass);
				if (member == null)
					return InheritDocVisitor.CONTINUE;
				if (member.getAttachedJavadoc(null) != null ) {
					// for binary methods with attached javadoc and no source attached
					return overridden;
				}
				IOpenable openable = member.getOpenable();
				if (openable == null)
					return InheritDocVisitor.CONTINUE;
				IBuffer buf= openable.getBuffer();
				if (buf == null) {
					// no source attachment found. This method maybe the one. Stop.
					return InheritDocVisitor.STOP_BRANCH;
				}

				ISourceRange javadocRange= member.getJavadocRange();
				if (javadocRange == null)
					return InheritDocVisitor.CONTINUE;	// this method doesn't have javadoc, continue to look.
				String rawJavadoc= buf.getText(javadocRange.getOffset(), javadocRange.getLength());
				if (rawJavadoc != null) {
					return overridden;
				}
				return InheritDocVisitor.CONTINUE;
			}
		}.visitInheritDoc(type);
	}

	/**
	 * Implements the "Algorithm for Inheriting Method Comments" as specified for
	 * <a href="http://download.oracle.com/javase/6/docs/technotes/tools/windows/javadoc.html#inheritingcomments">1.6</a>.
	 *
	 * <p>
	 * Unfortunately, the implementation is broken in Javadoc implementations since 1.5, see
	 * <a href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6376959">Sun's bug</a>.
	 * </p>
	 *
	 * <p>
	 * We adhere to the spec.
	 * </p>
	 */
	static abstract class InheritDocVisitor {
		public static final Object STOP_BRANCH= new Object() {
			@Override
			public String toString() { return "STOP_BRANCH"; } //$NON-NLS-1$
		};
		public static final Object CONTINUE= new Object() {
			@Override
			public String toString() { return "CONTINUE"; } //$NON-NLS-1$
		};

		/**
		 * Visits a type and decides how the visitor should proceed.
		 *
		 * @param currType the current type
		 * @return <ul>
		 *         <li>{@link #STOP_BRANCH} to indicate that no Javadoc has been found and visiting
		 *         super types should stop here</li>
		 *         <li>{@link #CONTINUE} to indicate that no Javadoc has been found and visiting
		 *         super types should continue</li>
		 *         <li>an {@link Object} or <code>null</code>, to indicate that visiting should be
		 *         cancelled immediately. The returned value is the result of
		 *         {@link #visitInheritDoc(ReferenceBinding)}</li>
		 *         </ul>
		 * @throws JavaModelException unexpected problem
		 * @see #visitInheritDoc(ReferenceBinding)
		 */
		public abstract Object visit(ReferenceBinding currType) throws JavaModelException;

		/**
		 * Visits the super types of the given <code>currentType</code>.
		 *
		 * @param currentType the starting type
		 * @return the result from a call to {@link #visit(ReferenceBinding)}, or <code>null</code> if none of
		 *         the calls returned a result
		 * @throws JavaModelException unexpected problem
		 */
		public Object visitInheritDoc(ReferenceBinding currentType) throws JavaModelException {
			ArrayList visited= new ArrayList();
			visited.add(currentType);
			Object result= visitInheritDocInterfaces(visited, currentType);
			if (result != InheritDocVisitor.CONTINUE)
				return result;

			ReferenceBinding superClass= currentType.superclass();

			while (superClass != null && ! visited.contains(superClass)) {
				result= visit(superClass);
				if (result == InheritDocVisitor.STOP_BRANCH) {
					return null;
				} else if (result == InheritDocVisitor.CONTINUE) {
					visited.add(superClass);
					result= visitInheritDocInterfaces(visited, superClass);
					if (result != InheritDocVisitor.CONTINUE)
						return result;
					else
						superClass= superClass.superclass();
				} else {
					return result;
				}
			}

			return null;
		}

		/**
		 * Visits the super interfaces of the given type in the given hierarchy, thereby skipping already visited types.
		 *
		 * @param visited set of visited types
		 * @param currentType type whose super interfaces should be visited
		 * @return the result, or {@link #CONTINUE} if no result has been found
		 * @throws JavaModelException unexpected problem
		 */
		private Object visitInheritDocInterfaces(ArrayList visited, ReferenceBinding currentType) throws JavaModelException {
			ArrayList toVisitChildren= new ArrayList();
			ReferenceBinding[] superInterfaces= currentType.superInterfaces();
			for (int i= 0; i < superInterfaces.length; i++) {
				ReferenceBinding superInterface= superInterfaces[i];
				if (visited.contains(superInterface))
					continue;
				visited.add(superInterface);
				Object result= visit(superInterface);
				if (result == InheritDocVisitor.STOP_BRANCH) {
					//skip
				} else if (result == InheritDocVisitor.CONTINUE) {
					toVisitChildren.add(superInterface);
				} else {
					return result;
				}
			}
			for (Iterator iter= toVisitChildren.iterator(); iter.hasNext(); ) {
				ReferenceBinding child= (ReferenceBinding) iter.next();
				Object result= visitInheritDocInterfaces(visited, child);
				if (result != InheritDocVisitor.CONTINUE)
					return result;
			}
			return InheritDocVisitor.CONTINUE;
		}
	}

	@Override
	public void acceptModule(char[] moduleName) {
		// TODO Auto-generated method stub

	}
}
