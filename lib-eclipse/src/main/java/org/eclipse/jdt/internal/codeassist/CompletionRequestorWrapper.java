/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
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
package org.eclipse.jdt.internal.codeassist;

import org.eclipse.jdt.core.CompletionProposal;
import org.eclipse.jdt.core.CompletionRequestor;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.compiler.IProblem;

/**
 * This CompletionRequetor wrap the old requestor ICOmpletionRequestor
 * @since 3.1
 * @deprecated
 */
public class CompletionRequestorWrapper extends CompletionRequestor {
	private static boolean DECODE_SIGNATURE = false;

	private org.eclipse.jdt.core.ICompletionRequestor requestor;
	public CompletionRequestorWrapper(org.eclipse.jdt.core.ICompletionRequestor requestor) {
		this.requestor = requestor;
	}

	@Override
	public void accept(CompletionProposal proposal) {
		InternalCompletionProposal internalCompletionProposal = (InternalCompletionProposal) proposal;
		switch(internalCompletionProposal.getKind()) {
			case CompletionProposal.KEYWORD:
				this.requestor.acceptKeyword(
						internalCompletionProposal.getName(),
						internalCompletionProposal.getReplaceStart(),
						internalCompletionProposal.getReplaceEnd(),
						internalCompletionProposal.getRelevance());
				break;
			case CompletionProposal.PACKAGE_REF:
				if(DECODE_SIGNATURE) {
					this.requestor.acceptPackage(
							internalCompletionProposal.getDeclarationSignature(),
							internalCompletionProposal.getCompletion(),
							internalCompletionProposal.getReplaceStart(),
							internalCompletionProposal.getReplaceEnd(),
							internalCompletionProposal.getRelevance());
				} else {
					this.requestor.acceptPackage(
							internalCompletionProposal.getPackageName(),
							internalCompletionProposal.getCompletion(),
							internalCompletionProposal.getReplaceStart(),
							internalCompletionProposal.getReplaceEnd(),
							internalCompletionProposal.getRelevance());
				}
				break;
			case CompletionProposal.TYPE_REF:
				if((internalCompletionProposal.getFlags() & Flags.AccEnum) != 0) {
					// does not exist for old requestor
				} else if((internalCompletionProposal.getFlags() & Flags.AccInterface) != 0) {
					if(DECODE_SIGNATURE) {
						this.requestor.acceptInterface(
								internalCompletionProposal.getDeclarationSignature(),
								Signature.getSignatureSimpleName(internalCompletionProposal.getSignature()),
								internalCompletionProposal.getCompletion(),
								internalCompletionProposal.getFlags() & ~Flags.AccInterface,
								internalCompletionProposal.getReplaceStart(),
								internalCompletionProposal.getReplaceEnd(),
								internalCompletionProposal.getRelevance());
					} else {
						this.requestor.acceptInterface(
								internalCompletionProposal.getPackageName() == null ? CharOperation.NO_CHAR : internalCompletionProposal.getPackageName(),
								internalCompletionProposal.getTypeName(),
								internalCompletionProposal.getCompletion(),
								internalCompletionProposal.getFlags() & ~Flags.AccInterface,
								internalCompletionProposal.getReplaceStart(),
								internalCompletionProposal.getReplaceEnd(),
								internalCompletionProposal.getRelevance());
					}
				} else {
					if(DECODE_SIGNATURE) {
						this.requestor.acceptClass(
								internalCompletionProposal.getDeclarationSignature(),
								Signature.getSignatureSimpleName(internalCompletionProposal.getSignature()),
								internalCompletionProposal.getCompletion(),
								internalCompletionProposal.getFlags(),
								internalCompletionProposal.getReplaceStart(),
								internalCompletionProposal.getReplaceEnd(),
								internalCompletionProposal.getRelevance());
					} else {
						this.requestor.acceptClass(
								internalCompletionProposal.getPackageName() == null ? CharOperation.NO_CHAR : internalCompletionProposal.getPackageName(),
								internalCompletionProposal.getTypeName(),
								internalCompletionProposal.getCompletion(),
								internalCompletionProposal.getFlags(),
								internalCompletionProposal.getReplaceStart(),
								internalCompletionProposal.getReplaceEnd(),
								internalCompletionProposal.getRelevance());
					}
				}
				break;
			case CompletionProposal.FIELD_REF:
				if(DECODE_SIGNATURE) {
					this.requestor.acceptField(
							Signature.getSignatureQualifier(internalCompletionProposal.getDeclarationSignature()),
							Signature.getSignatureSimpleName(internalCompletionProposal.getDeclarationSignature()),
							internalCompletionProposal.getName(),
							Signature.getSignatureQualifier(internalCompletionProposal.getSignature()),
							Signature.getSignatureSimpleName(internalCompletionProposal.getSignature()),
							internalCompletionProposal.getCompletion(),
							internalCompletionProposal.getFlags(),
							internalCompletionProposal.getReplaceStart(),
							internalCompletionProposal.getReplaceEnd(),
							internalCompletionProposal.getRelevance()
					);
				} else {
					this.requestor.acceptField(
							internalCompletionProposal.getDeclarationPackageName() == null ? CharOperation.NO_CHAR : internalCompletionProposal.getDeclarationPackageName(),
							internalCompletionProposal.getDeclarationTypeName() == null ? CharOperation.NO_CHAR : internalCompletionProposal.getDeclarationTypeName(),
							internalCompletionProposal.getName(),
							internalCompletionProposal.getPackageName() == null ? CharOperation.NO_CHAR : internalCompletionProposal.getPackageName(),
							internalCompletionProposal.getTypeName() == null ? CharOperation.NO_CHAR : internalCompletionProposal.getTypeName(),
							internalCompletionProposal.getCompletion(),
							internalCompletionProposal.getFlags(),
							internalCompletionProposal.getReplaceStart(),
							internalCompletionProposal.getReplaceEnd(),
							internalCompletionProposal.getRelevance()
					);
				}
				break;
			case CompletionProposal.METHOD_REF:
				if(DECODE_SIGNATURE) {
					this.requestor.acceptMethod(
							Signature.getSignatureQualifier(internalCompletionProposal.getDeclarationSignature()),
							Signature.getSignatureSimpleName(internalCompletionProposal.getDeclarationSignature()),
							internalCompletionProposal.getName(),
							getParameterPackages(internalCompletionProposal.getSignature()),
							getParameterTypes(internalCompletionProposal.getSignature()),
							internalCompletionProposal.findParameterNames(null) == null ? CharOperation.NO_CHAR_CHAR : internalCompletionProposal.findParameterNames(null),
							Signature.getSignatureQualifier(Signature.getReturnType(internalCompletionProposal.getSignature())),
							Signature.getSignatureSimpleName(Signature.getReturnType(internalCompletionProposal.getSignature())),
							internalCompletionProposal.getCompletion(),
							internalCompletionProposal.getFlags(),
							internalCompletionProposal.getReplaceStart(),
							internalCompletionProposal.getReplaceEnd(),
							internalCompletionProposal.getRelevance()
						);
				} else {
					this.requestor.acceptMethod(
							internalCompletionProposal.getDeclarationPackageName() == null ? CharOperation.NO_CHAR : internalCompletionProposal.getDeclarationPackageName(),
							internalCompletionProposal.getDeclarationTypeName() == null ? CharOperation.NO_CHAR : internalCompletionProposal.getDeclarationTypeName(),
							internalCompletionProposal.getName(),
							internalCompletionProposal.getParameterPackageNames() == null ? CharOperation.NO_CHAR_CHAR : internalCompletionProposal.getParameterPackageNames(),
							internalCompletionProposal.getParameterTypeNames() == null ? CharOperation.NO_CHAR_CHAR : internalCompletionProposal.getParameterTypeNames(),
							internalCompletionProposal.findParameterNames(null) == null ? CharOperation.NO_CHAR_CHAR : internalCompletionProposal.findParameterNames(null),
							internalCompletionProposal.getPackageName() == null ? CharOperation.NO_CHAR : internalCompletionProposal.getPackageName(),
							internalCompletionProposal.getTypeName() == null ? CharOperation.NO_CHAR : internalCompletionProposal.getTypeName(),
							internalCompletionProposal.getCompletion(),
							internalCompletionProposal.getFlags(),
							internalCompletionProposal.getReplaceStart(),
							internalCompletionProposal.getReplaceEnd(),
							internalCompletionProposal.getRelevance()
					);
				}
				break;
			case CompletionProposal.METHOD_DECLARATION:
				if(DECODE_SIGNATURE) {
					this.requestor.acceptMethodDeclaration(
							Signature.getSignatureQualifier(internalCompletionProposal.getDeclarationSignature()),
							Signature.getSignatureSimpleName(internalCompletionProposal.getDeclarationSignature()),
							internalCompletionProposal.getName(),
							getParameterPackages(internalCompletionProposal.getSignature()),
							getParameterTypes(internalCompletionProposal.getSignature()),
							internalCompletionProposal.findParameterNames(null) == null ? CharOperation.NO_CHAR_CHAR : internalCompletionProposal.findParameterNames(null),
							Signature.getSignatureQualifier(Signature.getReturnType(internalCompletionProposal.getSignature())),
							Signature.getSignatureSimpleName(Signature.getReturnType(internalCompletionProposal.getSignature())),
							internalCompletionProposal.getCompletion(),
							internalCompletionProposal.getFlags(),
							internalCompletionProposal.getReplaceStart(),
							internalCompletionProposal.getReplaceEnd(),
							internalCompletionProposal.getRelevance()
					);
				} else {
					this.requestor.acceptMethodDeclaration(
							internalCompletionProposal.getDeclarationPackageName(),
							internalCompletionProposal.getDeclarationTypeName(),
							internalCompletionProposal.getName(),
							internalCompletionProposal.getParameterPackageNames() == null ? CharOperation.NO_CHAR_CHAR : internalCompletionProposal.getParameterPackageNames(),
							internalCompletionProposal.getParameterTypeNames() == null ? CharOperation.NO_CHAR_CHAR : internalCompletionProposal.getParameterTypeNames(),
							internalCompletionProposal.findParameterNames(null) == null ? CharOperation.NO_CHAR_CHAR : internalCompletionProposal.findParameterNames(null),
							internalCompletionProposal.getPackageName(),
							internalCompletionProposal.getTypeName(),
							internalCompletionProposal.getCompletion(),
							internalCompletionProposal.getFlags(),
							internalCompletionProposal.getReplaceStart(),
							internalCompletionProposal.getReplaceEnd(),
							internalCompletionProposal.getRelevance()
					);
				}
				break;
			case CompletionProposal.ANONYMOUS_CLASS_DECLARATION:
				if(DECODE_SIGNATURE) {
					this.requestor.acceptAnonymousType(
							Signature.getSignatureQualifier(internalCompletionProposal.getDeclarationSignature()),
							Signature.getSignatureSimpleName(internalCompletionProposal.getDeclarationSignature()),
							getParameterPackages(internalCompletionProposal.getSignature()),
							getParameterTypes(internalCompletionProposal.getSignature()),
							internalCompletionProposal.findParameterNames(null) == null ? CharOperation.NO_CHAR_CHAR : internalCompletionProposal.findParameterNames(null),
							internalCompletionProposal.getCompletion(),
							internalCompletionProposal.getFlags(),
							internalCompletionProposal.getReplaceStart(),
							internalCompletionProposal.getReplaceEnd(),
							internalCompletionProposal.getRelevance()
						);
				} else {
					this.requestor.acceptAnonymousType(
						internalCompletionProposal.getDeclarationPackageName(),
						internalCompletionProposal.getDeclarationTypeName(),
						internalCompletionProposal.getParameterPackageNames() == null ? CharOperation.NO_CHAR_CHAR : internalCompletionProposal.getParameterPackageNames(),
						internalCompletionProposal.getParameterTypeNames() == null ? CharOperation.NO_CHAR_CHAR : internalCompletionProposal.getParameterTypeNames(),
						internalCompletionProposal.findParameterNames(null) == null ? CharOperation.NO_CHAR_CHAR : internalCompletionProposal.findParameterNames(null),
						internalCompletionProposal.getCompletion(),
						internalCompletionProposal.getFlags(),
						internalCompletionProposal.getReplaceStart(),
						internalCompletionProposal.getReplaceEnd(),
						internalCompletionProposal.getRelevance()
					);
				}
				break;
			case CompletionProposal.LABEL_REF :
				this.requestor.acceptLabel(
					internalCompletionProposal.getCompletion(),
					internalCompletionProposal.getReplaceStart(),
					internalCompletionProposal.getReplaceEnd(),
					internalCompletionProposal.getRelevance()
				);
				break;
			case CompletionProposal.LOCAL_VARIABLE_REF:
				if(DECODE_SIGNATURE) {
					this.requestor.acceptLocalVariable(
							internalCompletionProposal.getCompletion(),
							Signature.getSignatureQualifier(internalCompletionProposal.getSignature()),
							Signature.getSignatureSimpleName(internalCompletionProposal.getSignature()),
							internalCompletionProposal.getFlags(),
							internalCompletionProposal.getReplaceStart(),
							internalCompletionProposal.getReplaceEnd(),
							internalCompletionProposal.getRelevance()
					);
				} else {
					this.requestor.acceptLocalVariable(
							internalCompletionProposal.getCompletion(),
							internalCompletionProposal.getPackageName() == null ? CharOperation.NO_CHAR : internalCompletionProposal.getPackageName(),
							internalCompletionProposal.getTypeName(),
							internalCompletionProposal.getFlags(),
							internalCompletionProposal.getReplaceStart(),
							internalCompletionProposal.getReplaceEnd(),
							internalCompletionProposal.getRelevance()
					);
				}
				break;
			case CompletionProposal.VARIABLE_DECLARATION:
				if(DECODE_SIGNATURE) {
					this.requestor.acceptLocalVariable(
							internalCompletionProposal.getCompletion(),
							Signature.getSignatureQualifier(internalCompletionProposal.getSignature()),
							Signature.getSignatureSimpleName(internalCompletionProposal.getSignature()),
							internalCompletionProposal.getFlags(),
							internalCompletionProposal.getReplaceStart(),
							internalCompletionProposal.getReplaceEnd(),
							internalCompletionProposal.getRelevance()
						);
				} else {
					this.requestor.acceptLocalVariable(
						internalCompletionProposal.getCompletion(),
						internalCompletionProposal.getPackageName(),
						internalCompletionProposal.getTypeName(),
						internalCompletionProposal.getFlags(),
						internalCompletionProposal.getReplaceStart(),
						internalCompletionProposal.getReplaceEnd(),
						internalCompletionProposal.getRelevance()
					);
				}
				break;
			case CompletionProposal.POTENTIAL_METHOD_DECLARATION:
				if(this.requestor instanceof IExtendedCompletionRequestor) {
					IExtendedCompletionRequestor r = (IExtendedCompletionRequestor) this.requestor;
					if(DECODE_SIGNATURE) {
						r.acceptPotentialMethodDeclaration(
								Signature.getSignatureQualifier(internalCompletionProposal.getDeclarationSignature()),
								Signature.getSignatureSimpleName(internalCompletionProposal.getDeclarationSignature()),
								internalCompletionProposal.getName(),
								internalCompletionProposal.getReplaceStart(),
								internalCompletionProposal.getReplaceEnd(),
								internalCompletionProposal.getRelevance()
						);
					} else {
						r.acceptPotentialMethodDeclaration(
								internalCompletionProposal.getDeclarationPackageName(),
								internalCompletionProposal.getDeclarationTypeName(),
								internalCompletionProposal.getName(),
								internalCompletionProposal.getReplaceStart(),
								internalCompletionProposal.getReplaceEnd(),
								internalCompletionProposal.getRelevance()
						);
					}
				}
				break;

		}
	}

	@Override
	public void completionFailure(IProblem problem) {
		this.requestor.acceptError(problem);
	}

	private char[][] getParameterPackages(char[] methodSignature) {
		char[][] parameterQualifiedTypes = Signature.getParameterTypes(methodSignature);
		int length = parameterQualifiedTypes == null ? 0 : parameterQualifiedTypes.length;
		char[][] parameterPackages = new char[length][];
		for(int i = 0; i < length; i++) {
			parameterPackages[i] = Signature.getSignatureQualifier(parameterQualifiedTypes[i]);
		}

		return parameterPackages;
	}

	private char[][] getParameterTypes(char[] methodSignature) {
		char[][] parameterQualifiedTypes = Signature.getParameterTypes(methodSignature);
		int length = parameterQualifiedTypes == null ? 0 : parameterQualifiedTypes.length;
		char[][] parameterPackages = new char[length][];
		for(int i = 0; i < length; i++) {
			parameterPackages[i] = Signature.getSignatureSimpleName(parameterQualifiedTypes[i]);
		}

		return parameterPackages;
	}
}
