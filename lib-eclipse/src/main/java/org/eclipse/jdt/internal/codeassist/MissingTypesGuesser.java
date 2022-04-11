/*******************************************************************************
 * Copyright (c) 2006, 2013 IBM Corporation and others.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.ast.*;
import org.eclipse.jdt.internal.compiler.env.AccessRestriction;
import org.eclipse.jdt.internal.compiler.lookup.Binding;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.ProblemReasons;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.Scope;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.jdt.internal.compiler.util.HashtableOfObjectToInt;
import org.eclipse.jdt.internal.core.SearchableEnvironment;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class MissingTypesGuesser extends ASTVisitor {
	public static interface GuessedTypeRequestor {
		public void accept(
				TypeBinding guessedType,
				Binding[] missingElements,
				int[] missingElementsStarts,
				int[] missingElementsEnds,
				boolean hasProblems);

	}

	private static class ResolutionCleaner extends ASTVisitor {
		private HashtableOfObjectToInt bitsMap = new HashtableOfObjectToInt();
		private boolean firstCall = true;

		public ResolutionCleaner(){
			super();
		}

		private void cleanUp(TypeReference typeReference) {
			if (this.firstCall) {
				this.bitsMap.put(typeReference, typeReference.bits);
			} else {
				typeReference.bits = this.bitsMap.get(typeReference);
			}
			typeReference.resolvedType = null;
		}

		private void cleanUp(ParameterizedSingleTypeReference typeReference) {
			this.cleanUp((TypeReference)typeReference);
			typeReference.bits &= ~ASTNode.DidResolve;
		}

		private void cleanUp(ParameterizedQualifiedTypeReference typeReference) {
			this.cleanUp((TypeReference)typeReference);
			typeReference.bits &= ~ASTNode.DidResolve;
		}

		public void cleanUp(TypeReference convertedType, BlockScope scope) {
			convertedType.traverse(this, scope);
			this.firstCall = false;
		}

		public void cleanUp(TypeReference convertedType, ClassScope scope) {
			convertedType.traverse(this, scope);
			this.firstCall = false;
		}

		@Override
		public boolean visit(SingleTypeReference singleTypeReference, BlockScope scope) {
			this.cleanUp(singleTypeReference);
			return true;
		}

		@Override
		public boolean visit(SingleTypeReference singleTypeReference, ClassScope scope) {
			this.cleanUp(singleTypeReference);
			return true;
		}

		@Override
		public boolean visit(Wildcard wildcard, BlockScope scope) {
			this.cleanUp(wildcard);
			return true;
		}

		@Override
		public boolean visit(Wildcard wildcard, ClassScope scope) {
			this.cleanUp(wildcard);
			return true;
		}

		@Override
		public boolean visit(ArrayTypeReference arrayTypeReference, BlockScope scope) {
			this.cleanUp(arrayTypeReference);
			return true;
		}

		@Override
		public boolean visit(ArrayTypeReference arrayTypeReference, ClassScope scope) {
			this.cleanUp(arrayTypeReference);
			return true;
		}

		@Override
		public boolean visit(ParameterizedSingleTypeReference parameterizedSingleTypeReference, BlockScope scope) {
			this.cleanUp(parameterizedSingleTypeReference);
			return true;
		}

		@Override
		public boolean visit(ParameterizedSingleTypeReference parameterizedSingleTypeReference, ClassScope scope) {
			this.cleanUp(parameterizedSingleTypeReference);
			return true;
		}

		@Override
		public boolean visit(QualifiedTypeReference qualifiedTypeReference, BlockScope scope) {
			this.cleanUp(qualifiedTypeReference);
			return true;
		}

		@Override
		public boolean visit(QualifiedTypeReference qualifiedTypeReference, ClassScope scope) {
			this.cleanUp(qualifiedTypeReference);
			return true;
		}

		@Override
		public boolean visit(ArrayQualifiedTypeReference arrayQualifiedTypeReference, BlockScope scope) {
			this.cleanUp(arrayQualifiedTypeReference);
			return true;
		}

		@Override
		public boolean visit(ArrayQualifiedTypeReference arrayQualifiedTypeReference, ClassScope scope) {
			this.cleanUp(arrayQualifiedTypeReference);
			return true;
		}

		@Override
		public boolean visit(ParameterizedQualifiedTypeReference parameterizedQualifiedTypeReference, BlockScope scope) {
			this.cleanUp(parameterizedQualifiedTypeReference);
			return true;
		}

		@Override
		public boolean visit(ParameterizedQualifiedTypeReference parameterizedQualifiedTypeReference, ClassScope scope) {
			this.cleanUp(parameterizedQualifiedTypeReference);
			return true;
		}

	}

	private CompletionEngine.CompletionProblemFactory problemFactory ;
	private  SearchableEnvironment nameEnvironment;

	private HashMap substituedTypes;
	private HashMap originalTypes;
	private int combinationsCount;

	public MissingTypesGuesser(CompletionEngine completionEngine) {
		this.problemFactory = completionEngine.problemFactory;
		this.nameEnvironment = completionEngine.nameEnvironment;
	}

	private boolean computeMissingElements(
			QualifiedTypeReference[] substituedTypeNodes,
			char[][][] originalTypeNames,
			Binding[] missingElements,
			int[] missingElementsStarts,
			int[] missingElementsEnds) {
		int length = substituedTypeNodes.length;

		for (int i = 0; i < length; i++) {
			TypeReference substituedType = substituedTypeNodes[i];
			if (substituedType.resolvedType == null) return false;
			ReferenceBinding erasure = (ReferenceBinding)substituedType.resolvedType.leafComponentType().erasure();
			Binding missingElement;
			int depthToRemove = originalTypeNames[i].length - 1 ;
			if (depthToRemove == 0) {
				missingElement = erasure;
			} else {
				int depth = erasure.depth() + 1;

				if (depth > depthToRemove) {
					missingElement = erasure.enclosingTypeAt(depthToRemove);
				} else {
					return false;
					///////////////////////////////////////////////////////////
					//// Uncomment the following code to return missing package
					///////////////////////////////////////////////////////////
					//depthToRemove -= depth;
					//PackageBinding packageBinding = erasure.getPackage();
					//while(depthToRemove > 0) {
					//	packageBinding = packageBinding.parent;
					//	depthToRemove--;
					//}
					//missingElement = packageBinding;
				}
			}

			missingElements[i] = missingElement;
			missingElementsStarts[i] = substituedType.sourceStart;
			missingElementsEnds[i] = substituedType.sourceEnd + 1;

		}

		return true;
	}

	private TypeReference convert(ArrayQualifiedTypeReference typeRef) {
		if (typeRef.resolvedType != null) {
			if (typeRef.resolvedType.isValidBinding()) {
				ArrayQualifiedTypeReference convertedType =
					new ArrayQualifiedTypeReference(
							typeRef.tokens,
							typeRef.dimensions(),
							typeRef.sourcePositions);
				convertedType.sourceStart = typeRef.sourceStart;
				convertedType.sourceEnd = typeRef.sourceEnd;
				return convertedType;
			} else if((typeRef.resolvedType.problemId() & ProblemReasons.NotFound) != 0) {
				// only the first token must be resolved
				if(((ReferenceBinding)typeRef.resolvedType.leafComponentType()).compoundName.length != 1) return null;

				char[][] typeName = typeRef.getTypeName();
				char[][][] typeNames = findTypeNames(typeName);
				if(typeNames == null || typeNames.length == 0) return null;
				ArrayQualifiedTypeReference convertedType =
					new ArrayQualifiedTypeReference(
							typeNames[0],
							typeRef.dimensions(),
							new long[typeNames[0].length]);
				convertedType.sourceStart = typeRef.sourceStart;
				convertedType.sourceEnd = (int)(typeRef.sourcePositions[0] & 0x00000000FFFFFFFFL);
				this.substituedTypes.put(convertedType, typeNames);
				this.originalTypes.put(convertedType, typeName);
				this.combinationsCount *= typeNames.length;
				return convertedType;
			}
		}
		return null;
	}

	private TypeReference convert(ArrayTypeReference typeRef) {
		if (typeRef.resolvedType != null) {
			if (typeRef.resolvedType.isValidBinding()) {
				ArrayTypeReference convertedType =
					new ArrayTypeReference(
							typeRef.token,
							typeRef.dimensions,
							0);
				convertedType.sourceStart = typeRef.sourceStart;
				convertedType.sourceEnd = typeRef.originalSourceEnd;
				return convertedType;
			} else if((typeRef.resolvedType.problemId() & ProblemReasons.NotFound) != 0) {
				char[][] typeName = typeRef.getTypeName();
				char[][][] typeNames = findTypeNames(typeName);
				if(typeNames == null || typeNames.length == 0) return null;
				ArrayQualifiedTypeReference convertedType =
					new ArrayQualifiedTypeReference(
							typeNames[0],
							typeRef.dimensions,
							new long[typeNames[0].length]);
				convertedType.sourceStart = typeRef.sourceStart;
				convertedType.sourceEnd = typeRef.originalSourceEnd;
				this.substituedTypes.put(convertedType, typeNames);
				this.originalTypes.put(convertedType, typeName);
				this.combinationsCount *= typeNames.length;
				return convertedType;
			}
		}
		return null;
	}

	private TypeReference convert(ParameterizedQualifiedTypeReference typeRef) {
		if (typeRef.resolvedType != null) {
			TypeReference[][] typeArguments = typeRef.typeArguments;
			int length = typeArguments.length;
			TypeReference[][] convertedTypeArguments = new TypeReference[length][];
			next : for (int i = 0; i < length; i++) {
				if (typeArguments[i] == null) continue next;
				int length2 = typeArguments[i].length;
				convertedTypeArguments[i] = new TypeReference[length2];
				for (int j = 0; j < length2; j++) {
					convertedTypeArguments[i][j] = convert(typeArguments[i][j]);
					if (convertedTypeArguments[i][j] == null) return null;
				}
			}

			if (typeRef.resolvedType.isValidBinding()) {
				ParameterizedQualifiedTypeReference convertedType =
					new ParameterizedQualifiedTypeReference(
							typeRef.tokens,
							convertedTypeArguments,
							typeRef.dimensions(),
							new long[typeRef.tokens.length]);
				convertedType.sourceStart = typeRef.sourceStart;
				convertedType.sourceEnd = typeRef.sourceEnd;
				return convertedType;
			} else if((typeRef.resolvedType.problemId() & ProblemReasons.NotFound) != 0) {
				// only the first token must be resolved
				if(((ReferenceBinding)typeRef.resolvedType.leafComponentType()).compoundName.length != 1) return null;

				char[][] typeName = typeRef.getTypeName();
				char[][][] typeNames = findTypeNames(typeName);
				if(typeNames == null || typeNames.length == 0) return null;

				TypeReference[][] newConvertedTypeArguments = new TypeReference[typeNames[0].length][];
				for (int k = newConvertedTypeArguments.length - 1, l = convertedTypeArguments.length -1; k > -1 && l > -1;) {
					newConvertedTypeArguments[k] = convertedTypeArguments[l];
					k--;
					l--;
				}

				ParameterizedQualifiedTypeReference convertedType =
					new ParameterizedQualifiedTypeReference(
							typeNames[0],
							newConvertedTypeArguments,
							typeRef.dimensions(),
							new long[typeNames[0].length]);
				convertedType.sourceStart = typeRef.sourceStart;
				convertedType.sourceEnd = (int)(typeRef.sourcePositions[0] & 0x00000000FFFFFFFFL);
				this.substituedTypes.put(convertedType, typeNames);
				this.originalTypes.put(convertedType, typeName);
				this.combinationsCount *= typeNames.length;
				return convertedType;
			}
		}
		return null;
	}

	private TypeReference convert(ParameterizedSingleTypeReference typeRef) {
		if (typeRef.resolvedType != null) {
			TypeReference[] typeArguments = typeRef.typeArguments;
			int length = typeArguments.length;
			TypeReference[] convertedTypeArguments = new TypeReference[length];
			for (int i = 0; i < length; i++) {
				convertedTypeArguments[i] = convert(typeArguments[i]);
				if(convertedTypeArguments[i] == null) return null;
			}

			if (typeRef.resolvedType.isValidBinding()) {
				ParameterizedSingleTypeReference convertedType =
					new ParameterizedSingleTypeReference(
							typeRef.token,
							convertedTypeArguments,
							typeRef.dimensions,
							0);
				convertedType.sourceStart = typeRef.sourceStart;
				convertedType.sourceEnd = typeRef.sourceEnd;
				return convertedType;
			} else if((typeRef.resolvedType.problemId() & ProblemReasons.NotFound) != 0) {
				char[][] typeName = typeRef.getTypeName();
				char[][][] typeNames = findTypeNames(typeName);
				if(typeNames == null || typeNames.length == 0) return null;

				TypeReference[][] allConvertedTypeArguments = new TypeReference[typeNames[0].length][];
				allConvertedTypeArguments[allConvertedTypeArguments.length - 1] = convertedTypeArguments;

				ParameterizedQualifiedTypeReference convertedType =
					new ParameterizedQualifiedTypeReference(
							typeNames[0],
							allConvertedTypeArguments,
							typeRef.dimensions,
							new long[typeNames[0].length]);
				convertedType.sourceStart = typeRef.sourceStart;
				convertedType.sourceEnd = typeRef.sourceEnd;
				this.substituedTypes.put(convertedType, typeNames);
				this.originalTypes.put(convertedType, typeName);
				this.combinationsCount *= typeNames.length;
				return convertedType;
			}
		}
		return null;
	}

	private TypeReference convert(QualifiedTypeReference typeRef) {
		if (typeRef.resolvedType != null) {
			if (typeRef.resolvedType.isValidBinding()) {
				QualifiedTypeReference convertedType = new QualifiedTypeReference(typeRef.tokens, typeRef.sourcePositions);
				convertedType.sourceStart = typeRef.sourceStart;
				convertedType.sourceEnd = typeRef.sourceEnd;
				return convertedType;
			} else if((typeRef.resolvedType.problemId() & ProblemReasons.NotFound) != 0) {
				// only the first token must be resolved
				if(((ReferenceBinding)typeRef.resolvedType).compoundName.length != 1) return null;

				char[][] typeName = typeRef.getTypeName();
				char[][][] typeNames = findTypeNames(typeName);
				if(typeNames == null || typeNames.length == 0) return null;
				QualifiedTypeReference convertedType = new QualifiedTypeReference(typeNames[0], new long[typeNames[0].length]);
				convertedType.sourceStart = typeRef.sourceStart;
				convertedType.sourceEnd = (int)(typeRef.sourcePositions[0] & 0x00000000FFFFFFFFL);
				this.substituedTypes.put(convertedType, typeNames);
				this.originalTypes.put(convertedType, typeName);
				this.combinationsCount *= typeNames.length;
				return convertedType;
			}
		}
		return null;
	}

	private TypeReference convert(SingleTypeReference typeRef) {
		if (typeRef.resolvedType != null) {
			if (typeRef.resolvedType.isValidBinding()) {
				SingleTypeReference convertedType = new SingleTypeReference(typeRef.token, 0);
				convertedType.sourceStart = typeRef.sourceStart;
				convertedType.sourceEnd = typeRef.sourceEnd;
				return convertedType;
			} else if((typeRef.resolvedType.problemId() & ProblemReasons.NotFound) != 0) {
				char[][] typeName = typeRef.getTypeName();
				char[][][] typeNames = findTypeNames(typeName);
				if(typeNames == null || typeNames.length == 0) return null;
				QualifiedTypeReference convertedType = new QualifiedTypeReference(typeNames[0], new long[typeNames[0].length]);
				convertedType.sourceStart = typeRef.sourceStart;
				convertedType.sourceEnd = typeRef.sourceEnd;
				this.substituedTypes.put(convertedType, typeNames);
				this.originalTypes.put(convertedType, typeName);
				this.combinationsCount *= typeNames.length;
				return convertedType;
			}
		}
		return null;
	}

	private TypeReference convert(TypeReference typeRef) {
		if (typeRef instanceof ParameterizedSingleTypeReference) {
			return convert((ParameterizedSingleTypeReference)typeRef);
		} else if(typeRef instanceof ParameterizedQualifiedTypeReference) {
			return convert((ParameterizedQualifiedTypeReference)typeRef);
		} else if (typeRef instanceof ArrayTypeReference) {
			return convert((ArrayTypeReference)typeRef);
		} else if(typeRef instanceof ArrayQualifiedTypeReference) {
			return convert((ArrayQualifiedTypeReference)typeRef);
		} else if(typeRef instanceof Wildcard) {
			return convert((Wildcard)typeRef);
		} else if (typeRef instanceof SingleTypeReference) {
			return convert((SingleTypeReference)typeRef);
		} else if (typeRef instanceof QualifiedTypeReference) {
			return convert((QualifiedTypeReference)typeRef);
		}
		return null;
	}

	private TypeReference convert(Wildcard typeRef) {
		TypeReference bound = typeRef.bound;
		TypeReference convertedBound = null;
		if (bound != null) {
			convertedBound = convert(bound);
			if (convertedBound == null) return null;
		}
		Wildcard convertedType = new Wildcard(typeRef.kind);
		convertedType.bound = convertedBound;
		convertedType.sourceStart = typeRef.sourceStart;
		convertedType.sourceEnd = typeRef.sourceEnd;
		return convertedType;
	}

	private char[][][] findTypeNames(char[][] missingTypeName) {
		char[] missingSimpleName = missingTypeName[missingTypeName.length - 1];
		final boolean isQualified = missingTypeName.length > 1;
		final char[] missingFullyQualifiedName =
			isQualified ? CharOperation.concatWith(missingTypeName, '.') : null;
		final ArrayList results = new ArrayList();
		ISearchRequestor storage = new ISearchRequestor() {
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
			public void acceptModule(char[] moduleName) {
				// TODO Auto-generated method stub
			}
			@Override
			public void acceptPackage(char[] packageName) {
				// package aren't searched
			}
			@Override
			public void acceptType(
					char[] packageName,
					char[] typeName,
					char[][] enclosingTypeNames,
					int modifiers,
					AccessRestriction accessRestriction) {
				char[] fullyQualifiedName = CharOperation.concat(packageName, CharOperation.concat(CharOperation.concatWith(enclosingTypeNames, '.'), typeName, '.'), '.');
				if (isQualified && !CharOperation.endsWith(fullyQualifiedName, missingFullyQualifiedName)) return;
				char[][] compoundName = CharOperation.splitOn('.', fullyQualifiedName);
				results.add(compoundName);
			}

		};
		this.nameEnvironment.findExactTypes(missingSimpleName, true, IJavaSearchConstants.TYPE, storage);
		if(results.size() == 0) return null;
		return (char[][][])results.toArray(new char[results.size()][0][0]);
	}

	private char[][] getOriginal(TypeReference typeRef) {
		return (char[][])this.originalTypes.get(typeRef);
	}

	private QualifiedTypeReference[] getSubstituedTypes() {
		Set types = this.substituedTypes.keySet();
		return (QualifiedTypeReference[]) types.toArray(new QualifiedTypeReference[types.size()]);
	}

	private char[][][] getSubstitution(TypeReference typeRef) {
		return (char[][][])this.substituedTypes.get(typeRef);
	}

	public void guess(TypeReference typeRef, Scope scope, GuessedTypeRequestor requestor) {
		this.substituedTypes = new HashMap();
		this.originalTypes = new HashMap();
		this.combinationsCount = 1;

		TypeReference convertedType = convert(typeRef);

		if(convertedType == null) return;

		QualifiedTypeReference[] substituedTypeNodes = getSubstituedTypes();
		int length = substituedTypeNodes.length;

		int[] substitutionsIndexes = new int[substituedTypeNodes.length];
		char[][][][] subtitutions = new char[substituedTypeNodes.length][][][];
		char[][][] originalTypeNames = new char[substituedTypeNodes.length][][];
		for (int i = 0; i < substituedTypeNodes.length; i++) {
			subtitutions[i] = getSubstitution(substituedTypeNodes[i]);
			originalTypeNames[i] = getOriginal(substituedTypeNodes[i]);
		}

		ResolutionCleaner resolutionCleaner = new ResolutionCleaner();
		for (int i = 0; i < this.combinationsCount; i++) {

			nextSubstitution(substituedTypeNodes, subtitutions, substitutionsIndexes);


			this.problemFactory.startCheckingProblems();
			TypeBinding guessedType = null;
			switch (scope.kind) {
				case Scope.METHOD_SCOPE :
				case Scope.BLOCK_SCOPE :
					resolutionCleaner.cleanUp(convertedType, (BlockScope)scope);
					guessedType = convertedType.resolveType((BlockScope)scope);
					break;
				case Scope.CLASS_SCOPE :
					resolutionCleaner.cleanUp(convertedType, (ClassScope)scope);
					guessedType = convertedType.resolveType((ClassScope)scope);
					break;
			}
			this.problemFactory.stopCheckingProblems();
			if (!this.problemFactory.hasForbiddenProblems) {
				if (guessedType != null) {
					Binding[] missingElements = new Binding[length];
					int[] missingElementsStarts = new int[length];
					int[] missingElementsEnds = new int[length];

					if(computeMissingElements(
							substituedTypeNodes,
							originalTypeNames,
							missingElements,
							missingElementsStarts,
							missingElementsEnds)) {
						requestor.accept(
								guessedType.capture(scope, typeRef.sourceStart, typeRef.sourceEnd),
								missingElements,
								missingElementsStarts,
								missingElementsEnds,
								this.problemFactory.hasAllowedProblems);
					}
				}
			}
		}
	}
	private void nextSubstitution(
			QualifiedTypeReference[] substituedTypeNodes,
			char[][][][] subtitutions,
			int[] substitutionsIndexes) {
		int length = substituedTypeNodes.length;

		done : for (int i = 0; i < length; i++) {
			if(substitutionsIndexes[i] < subtitutions[i].length - 1) {
				substitutionsIndexes[i]++;
				break done;
			} else {
				substitutionsIndexes[i] = 0;
			}
		}

		for (int i = 0; i < length; i++) {
			QualifiedTypeReference qualifiedTypeReference = substituedTypeNodes[i];
			qualifiedTypeReference.tokens = subtitutions[i][substitutionsIndexes[i]];
			qualifiedTypeReference.sourcePositions = new long[qualifiedTypeReference.tokens.length];
			if(qualifiedTypeReference instanceof ParameterizedQualifiedTypeReference) {
				ParameterizedQualifiedTypeReference parameterizedQualifiedTypeReference =
					(ParameterizedQualifiedTypeReference)qualifiedTypeReference;
				TypeReference[][] typeArguments = parameterizedQualifiedTypeReference.typeArguments;
				TypeReference[][] newTypeArguments = new TypeReference[qualifiedTypeReference.tokens.length][];
				for (int j = newTypeArguments.length - 1, k = typeArguments.length -1; j > -1 && k > -1;) {
					newTypeArguments[j] = typeArguments[k];
					j--;
					k--;
				}
			}
		}
	}
}
