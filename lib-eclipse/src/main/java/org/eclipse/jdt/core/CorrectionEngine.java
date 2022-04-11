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
package org.eclipse.jdt.core;

import java.util.Hashtable;
import java.util.Map;

import org.eclipse.core.resources.*;
import org.eclipse.jdt.core.compiler.*;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.parser.*;
import org.eclipse.jdt.internal.compiler.problem.ProblemReporter;
import org.eclipse.jdt.internal.core.util.Messages;
import org.eclipse.jdt.internal.core.util.Util;

/**
 * This class is the entry point for source corrections.
 *
 * This class is intended to be instantiated by clients.
 *
 * @since 2.0
 * @noextend This class is not intended to be subclassed by clients.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class CorrectionEngine {

	/**
	 * This field is not intended to be used by client.
	 */
	protected int correctionStart;
	/**
	 * This field is not intended to be used by client.
	 */
	protected int correctionEnd;
	/**
	 * This field is not intended to be used by client.
	 */
	protected int prefixLength;
	/**
	 * This field is not intended to be used by client.
	 */
	protected ICompilationUnit compilationUnit;
	/**
	 * This field is not intended to be used by client.
	 */
	protected ICorrectionRequestor correctionRequestor;
	/**
	 * This field is not intended to be used by client.
	 */
	protected static final int CLASSES = 0x00000001;
	/**
	 * This field is not intended to be used by client.
	 */
	protected static final int INTERFACES = 0x00000002;
	/**
	 * This field is not intended to be used by client.
	 */
	protected static final int IMPORT = 0x00000004;
	/**
	 * This field is not intended to be used by client.
	 */
	protected static final int METHOD = 0x00000008;
	/**
	 * This field is not intended to be used by client.
	 */
	protected static final int FIELD = 0x00000010;
	/**
	 * This field is not intended to be used by client.
	 */
	protected static final int LOCAL = 0x00000020;
	/**
	 * This field is not intended to be used by client.
	 */
	protected int filter;

	/**
	 * The CorrectionEngine is responsible for computing problem corrections.
	 *
	 *  @param setting java.util.Map
	 *		set of options used to configure the code correction engine.
	 * 		CURRENTLY THERE IS NO CORRECTION SPECIFIC SETTINGS.
	 */
	public CorrectionEngine(Map setting) {
		// settings ignored for now
	}

	/**
	 * Performs code correction for the given marker,
	 * reporting results to the given correction requestor.
	 *
	 * Correction results are answered through a requestor.
	 *
	 * @param marker
	 * 		the marker which describe the problem to correct.
	 * @param targetUnit
	 * 		replace the compilation unit given by the marker. Ignored if null.
	 * @param positionOffset
	 * 		the offset of position given by the marker.
	 * @param requestor
	 * 		the given correction requestor
	 * @exception IllegalArgumentException if <code>requestor</code> is <code>null</code>
	 * @exception JavaModelException currently this exception is never thrown, but the opportunity to thrown an exception
	 * 	when the correction failed is kept for later.
	 * @since 2.0
	 */
	public void computeCorrections(IMarker marker, ICompilationUnit targetUnit, int positionOffset, ICorrectionRequestor requestor) throws JavaModelException {

		IJavaElement element = targetUnit == null ? JavaCore.create(marker.getResource()) : targetUnit;

		if(!(element instanceof ICompilationUnit))
			return;

		ICompilationUnit unit = (ICompilationUnit) element;

		int id = marker.getAttribute(IJavaModelMarker.ID, -1);
		String[] args = Util.getProblemArgumentsFromMarker(marker.getAttribute(IJavaModelMarker.ARGUMENTS, "")); //$NON-NLS-1$
		int start = marker.getAttribute(IMarker.CHAR_START, -1);
		int end = marker.getAttribute(IMarker.CHAR_END, -1);

		computeCorrections(unit, id, start + positionOffset, end + positionOffset, args, requestor);
	}

	/**
	 * Performs code correction for the given IProblem,
	 * reporting results to the given correction requestor.
	 *
	 * Correction results are answered through a requestor.
	 *
	 * @param problem
	 * 		the problem which describe the problem to correct.
	 * @param targetUnit
	 * 		denote the compilation unit in which correction occurs. Cannot be null.
	 * @param requestor
	 * 		the given correction requestor
	 * @exception IllegalArgumentException if <code>targetUnit</code> or <code>requestor</code> is <code>null</code>
	 * @exception JavaModelException currently this exception is never thrown, but the opportunity to thrown an exception
	 * 	when the correction failed is kept for later.
	 * @since 2.0
	 */
	public void computeCorrections(IProblem problem, ICompilationUnit targetUnit, ICorrectionRequestor requestor) throws JavaModelException {
		if (requestor == null) {
			throw new IllegalArgumentException(Messages.correction_nullUnit);
		}
		this.computeCorrections(
			targetUnit, problem.getID(),
			problem.getSourceStart(),
			problem.getSourceEnd(),
			problem.getArguments(),
			requestor);
	}

	/*
	 * Ask the engine to compute a correction for the specified problem
	 * of the given compilation unit.
	 * Correction results are answered through a requestor.
	 *
	 *  @param unit org.eclipse.jdt.internal.core.ICompilationUnit
	 *      the compilation unit.
	 *
	 * 	@param id int
	 * 		the id of the problem.
	 *
	 * 	@param start int
	 * 		a position in the source where the error begin.
	 *
	 *  @param end int
	 *      a position in the source where the error finish.
	 *
	 * 	@param arguments String[]
	 * 		arguments of the problem.
	 *
	 * @exception IllegalArgumentException if <code>requestor</code> is <code>null</code>
	 * @exception JavaModelException currently this exception is never thrown, but the opportunity to thrown an exception
	 * 	when the correction failed is kept for later.
	 * @since 2.0
	 */
	private void computeCorrections(ICompilationUnit unit, int id, int start, int end, String[] arguments, ICorrectionRequestor requestor) {

		if(id == -1 || arguments == null || start == -1 || end == -1)
			return;
		if (requestor == null) {
			throw new IllegalArgumentException(Messages.correction_nullRequestor);
		}

		this.correctionRequestor = requestor;
		this.correctionStart = start;
		this.correctionEnd = end;
		this.compilationUnit = unit;

		String argument = null;
		try {
			switch (id) {
				// Type correction
				case IProblem.ImportNotFound :
					this.filter = IMPORT;
					argument = arguments[0];
					break;
				case IProblem.UndefinedType :
					this.filter = CLASSES | INTERFACES;
					argument = arguments[0];
					break;

				// Method correction
				case IProblem.UndefinedMethod :
					this.filter = METHOD;
					argument = arguments[1];
					break;

				// Field and local variable correction
				case IProblem.UndefinedField :
					this.filter = FIELD;
					argument = arguments[0];
					break;
				case IProblem.UndefinedName :
				case IProblem.UnresolvedVariable :
					this.filter = FIELD | LOCAL;
					argument = arguments[0];
					break;
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			return;
		}
		if(argument != null) {
			correct(argument.toCharArray());
		}
	}

	private void correct(char[] argument) {
		try {
			String source = this.compilationUnit.getSource();
			Map<String,String> currentProjectOptions = this.compilationUnit.getJavaProject().getOptions(true);
			long sourceLevel = CompilerOptions.versionToJdkLevel(currentProjectOptions.get(JavaCore.COMPILER_SOURCE));
			long complianceLevel = CompilerOptions.versionToJdkLevel(currentProjectOptions.get(JavaCore.COMPILER_COMPLIANCE));

			Scanner scanner =
				new Scanner(
					false /*comment*/,
					false /*whitespace*/,
					false /*nls*/,
					sourceLevel,
					complianceLevel,
					null/*taskTag*/,
					null/*taskPriorities*/,
					true /*taskCaseSensitive*/,
					JavaCore.ENABLED.equals(currentProjectOptions.get(JavaCore.COMPILER_PB_ENABLE_PREVIEW_FEATURES)));
			scanner.setSource(source.toCharArray());

			scanner.resetTo(this.correctionStart, this.correctionEnd);
			int token = 0;
			char[] argumentSource = CharOperation.NO_CHAR;

			// search last segment position
			while(true) {
				token = scanner.getNextToken();
				if (token == TerminalTokens.TokenNameEOF) return;

				char[] tokenSource = scanner.getCurrentTokenSource();

				argumentSource = CharOperation.concat(argumentSource, tokenSource);
				if(!CharOperation.prefixEquals(argumentSource, argument))
					return;

				if(CharOperation.equals(argument, argumentSource)) {
					this.correctionStart = scanner.startPosition;
					this.correctionEnd = scanner.currentPosition;
					this.prefixLength = CharOperation.lastIndexOf('.', argument) + 1;
					break;
				}

			}

			// search completion position
			int completionPosition = this.correctionStart;
			scanner.resetTo(completionPosition, this.correctionEnd);
			int position = completionPosition;

			for (int i = 0; i < 4; i++) {
				if(scanner.getNextCharAsJavaIdentifierPart()) {
					completionPosition = position;
					position = scanner.currentPosition;
				} else {
					break;
				}
			}
			Hashtable oldOptions = JavaCore.getOptions();
			try {
				Hashtable options = new Hashtable(oldOptions);
				options.put(JavaCore.CODEASSIST_CAMEL_CASE_MATCH, JavaCore.DISABLED);
				options.put(JavaCore.CODEASSIST_SUBWORD_MATCH, JavaCore.DISABLED);
				JavaCore.setOptions(options);

				this.compilationUnit.codeComplete(
					completionPosition,
					this.completionRequestor
				);
			} finally {
				JavaCore.setOptions(oldOptions);
			}
		} catch (JavaModelException | InvalidInputException e) {
			return;
		}
	}

	/**
	 * This field is not intended to be used by client.
	 */
	protected CompletionRequestor completionRequestor = new CompletionRequestor() {
		@Override
		public void accept(CompletionProposal proposal) {
			switch (proposal.getKind()) {
				case CompletionProposal.TYPE_REF:
					int flags = proposal.getFlags();
					if (!(Flags.isEnum(flags) || Flags.isAnnotation(flags))) {
						if((CorrectionEngine.this.filter & (CLASSES | INTERFACES)) != 0) {
							char[] completionName = proposal.getCompletion();
							CorrectionEngine.this.correctionRequestor.acceptClass(
								proposal.getDeclarationSignature(),
								Signature.getSignatureSimpleName(proposal.getSignature()),
								CharOperation.subarray(completionName, CorrectionEngine.this.prefixLength, completionName.length),
								proposal.getFlags(),
								CorrectionEngine.this.correctionStart,
								CorrectionEngine.this.correctionEnd);
						} else if((CorrectionEngine.this.filter & IMPORT) != 0) {
							char[] packageName = proposal.getDeclarationSignature();
							char[] className = Signature.getSignatureSimpleName(proposal.getSignature());
							char[] fullName = CharOperation.concat(packageName, className, '.');
							CorrectionEngine.this.correctionRequestor.acceptClass(
								packageName,
								className,
								CharOperation.subarray(fullName, CorrectionEngine.this.prefixLength, fullName.length),
								proposal.getFlags(),
								CorrectionEngine.this.correctionStart,
								CorrectionEngine.this.correctionEnd);
						}
					}
					break;
				case CompletionProposal.FIELD_REF:
					if((CorrectionEngine.this.filter & FIELD) != 0) {
						char[] declaringSignature = proposal.getDeclarationSignature();
						char[] signature = proposal.getSignature();
						CorrectionEngine.this.correctionRequestor.acceptField(
							Signature.getSignatureQualifier(declaringSignature),
							Signature.getSignatureSimpleName(declaringSignature),
							proposal.getName(),
							Signature.getSignatureQualifier(signature),
							Signature.getSignatureSimpleName(signature),
							proposal.getName(),
							proposal.getFlags(),
							CorrectionEngine.this.correctionStart,
							CorrectionEngine.this.correctionEnd);
					}
					break;
				case CompletionProposal.LOCAL_VARIABLE_REF:
					if((CorrectionEngine.this.filter & LOCAL) != 0) {
						char[] signature = proposal.getSignature();
						CorrectionEngine.this.correctionRequestor.acceptLocalVariable(
							proposal.getName(),
							Signature.getSignatureQualifier(signature),
							Signature.getSignatureSimpleName(signature),
							proposal.getFlags(),
							CorrectionEngine.this.correctionStart,
							CorrectionEngine.this.correctionEnd);
					}
					break;
				case CompletionProposal.METHOD_REF:
					if((CorrectionEngine.this.filter & METHOD) != 0) {
						char[] declaringSignature = proposal.getDeclarationSignature();
						char[] signature = proposal.getSignature();
						char[][] parameterTypeSignatures = Signature.getParameterTypes(signature);
						int length = parameterTypeSignatures.length;
						char[][] parameterPackageNames = new char[length][];
						char[][] parameterTypeNames = new char[length][];
						for (int i = 0; i < length; i++) {
							parameterPackageNames[i] = Signature.getSignatureQualifier(parameterTypeSignatures[i]);
							parameterTypeNames[i] = Signature.getSignatureSimpleName(parameterTypeSignatures[i]);
						}
						char[] returnTypeSignature = Signature.getReturnType(signature);
						CorrectionEngine.this.correctionRequestor.acceptMethod(
							Signature.getSignatureQualifier(declaringSignature),
							Signature.getSignatureSimpleName(declaringSignature),
							proposal.getName(),
							parameterPackageNames,
							parameterTypeNames,
							proposal.findParameterNames(null),
							Signature.getSignatureQualifier(returnTypeSignature),
							Signature.getSignatureSimpleName(returnTypeSignature),
							proposal.getName(),
							proposal.getFlags(),
							CorrectionEngine.this.correctionStart,
							CorrectionEngine.this.correctionEnd);
					}
					break;
				case CompletionProposal.PACKAGE_REF:
					if((CorrectionEngine.this.filter & (CLASSES | INTERFACES | IMPORT)) != 0) {
						char[] packageName = proposal.getDeclarationSignature();
						CorrectionEngine.this.correctionRequestor.acceptPackage(
							packageName,
							CharOperation.subarray(packageName, CorrectionEngine.this.prefixLength, packageName.length),
							CorrectionEngine.this.correctionStart,
							CorrectionEngine.this.correctionEnd);
					}
					break;
			}
		}
	};


	/**
	 * Return an array of strings which contains one entry per warning token
	 * accepted by the <code>@SuppressWarnings</code> annotation. This array is
	 * neither null nor empty, it contains at least the String <code>all</code>.
	 * It should not be modified by the caller (please take a copy if modifications
	 * are needed).<br>
	 * <b>Note:</b> The tokens returned are not necessarily standardized across Java
	 * compilers. If you were to use one of these tokens in a <code>@SuppressWarnings</code>
	 * annotation in the Java source code, the effects (if any) may vary from
	 * compiler to compiler.
	 *
	 * @return an array of strings which contains one entry per warning token
	 * 			accepted by the <code>@SuppressWarnings</code> annotation.
	 * @since 3.2
	 */
	public static String[] getAllWarningTokens() {
		return CompilerOptions.warningTokens;
	}

	/**
	 * Helper method for decoding problem marker attributes. Returns an array of String arguments
	 * extracted from the problem marker "arguments" attribute, or <code>null</code> if the marker
	 * "arguments" attribute is missing or ill-formed.
	 *
	 * @param problemMarker
	 * 		the problem marker to decode arguments from.
	 * @return an array of String arguments, or <code>null</code> if unable to extract arguments
	 * @since 2.1
	 */
	public static String[] getProblemArguments(IMarker problemMarker){
		String argumentsString = problemMarker.getAttribute(IJavaModelMarker.ARGUMENTS, null);
		return Util.getProblemArgumentsFromMarker(argumentsString);
	}

	/**
	 * Returns a token which can be used to suppress a given warning using
	 * <code>@SuppressWarnings</code> annotation, for a given problem ID
	 * ({@link IProblem }). If a particular problem is not suppressable,
	 * <code>null</code> will be returned.
	 * <p>
	 * <b>Note:</b> <code>@SuppressWarnings</code> can only suppress warnings,
	 * which means that if some problems got promoted to ERROR using custom compiler
	 * settings ({@link IJavaProject#setOption(String, String)}), the
	 * <code>@SuppressWarnings</code> annotation will be ineffective.
	 * </p>
	 * <p>
	 * <b>Note:</b> <code>@SuppressWarnings</code> can be argumented with
	 * <code>"all"</code> so as to suppress all possible warnings at once.
	 * </p>
	 * <p>
	 * <b>Note:</b> The tokens returned are not necessarily standardized across Java
	 * compilers. If you were to use one of these tokens in an @SuppressWarnings
	 * annotation in the Java source code, the effects (if any) may vary from
	 * compiler to compiler.
	 * </p>
	 * @param problemID
	 *         the ID of a given warning to suppress
	 * @return a String which can be used in <code>@SuppressWarnings</code> annotation,
	 * or <code>null</code> if unable to suppress this warning.
	 * @since 3.1
	 */
	public static String getWarningToken(int problemID){
		int irritant = ProblemReporter.getIrritant(problemID);
		if (irritant != 0) {
			return CompilerOptions.warningTokenFromIrritant(irritant);
		}
		return null;
	}
}
