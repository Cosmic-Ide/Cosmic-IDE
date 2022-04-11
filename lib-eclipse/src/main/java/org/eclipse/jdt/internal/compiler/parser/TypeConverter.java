/*******************************************************************************
 * Copyright (c) 2008, 2020 IBM Corporation and others.
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
package org.eclipse.jdt.internal.compiler.parser;

import java.util.ArrayList;

import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.ArrayQualifiedTypeReference;
import org.eclipse.jdt.internal.compiler.ast.ArrayTypeReference;
import org.eclipse.jdt.internal.compiler.ast.ImportReference;
import org.eclipse.jdt.internal.compiler.ast.ParameterizedQualifiedTypeReference;
import org.eclipse.jdt.internal.compiler.ast.ParameterizedSingleTypeReference;
import org.eclipse.jdt.internal.compiler.ast.QualifiedTypeReference;
import org.eclipse.jdt.internal.compiler.ast.SingleTypeReference;
import org.eclipse.jdt.internal.compiler.ast.TypeParameter;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.eclipse.jdt.internal.compiler.ast.Wildcard;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeConstants;
import org.eclipse.jdt.internal.compiler.problem.ProblemReporter;

@SuppressWarnings({ "rawtypes", "unchecked" })
public abstract class TypeConverter {

	int namePos;

	protected ProblemReporter problemReporter;
	protected boolean has1_5Compliance;
	protected boolean has14_Compliance;
	private char memberTypeSeparator;

	protected TypeConverter(ProblemReporter problemReporter, char memberTypeSeparator) {
		this.problemReporter = problemReporter;
		this.has1_5Compliance = problemReporter.options.originalComplianceLevel >= ClassFileConstants.JDK1_5;
		this.has14_Compliance = problemReporter.options.originalComplianceLevel >= ClassFileConstants.JDK14;
		this.memberTypeSeparator = memberTypeSeparator;
	}

	private void addIdentifiers(String typeSignature, int start, int endExclusive, int identCount, ArrayList fragments) {
		if (identCount == 1) {
			char[] identifier;
			typeSignature.getChars(start, endExclusive, identifier = new char[endExclusive-start], 0);
			fragments.add(identifier);
		} else
			fragments.add(extractIdentifiers(typeSignature, start, endExclusive-1, identCount));
	}

	/*
	 * Build an import reference from an import name, e.g. java.lang.*
	 */
	protected ImportReference createImportReference(
		String[] importName,
		int start,
		int end,
		boolean onDemand,
		int modifiers) {

		int length = importName.length;
		long[] positions = new long[length];
		long position = ((long) start << 32) + end;
		char[][] qImportName = new char[length][];
		for (int i = 0; i < length; i++) {
			qImportName[i] = importName[i].toCharArray();
			positions[i] = position; // dummy positions
		}
		return new ImportReference(
			qImportName,
			positions,
			onDemand,
			modifiers);
	}

	protected TypeParameter createTypeParameter(char[] typeParameterName, char[][] typeParameterBounds, int start, int end) {

		TypeParameter parameter = new TypeParameter();
		parameter.name = typeParameterName;
		parameter.sourceStart = start;
		parameter.sourceEnd = end;
		if (typeParameterBounds != null) {
			int length = typeParameterBounds.length;
			if (length > 0) {
				parameter.type = createTypeReference(typeParameterBounds[0], start, end);
				if (length > 1) {
					parameter.bounds = new TypeReference[length-1];
					for (int i = 1; i < length; i++) {
						TypeReference bound = createTypeReference(typeParameterBounds[i], start, end);
						bound.bits |= ASTNode.IsSuperType;
						parameter.bounds[i-1] = bound;
					}
				}
			}
		}
		return parameter;
	}

	/*
	 * Build a type reference from a readable name, e.g. java.lang.Object[][]
	 */
	protected TypeReference createTypeReference(
		char[] typeName,
		int start,
		int end,
		boolean includeGenericsAnyway) {

		int length = typeName.length;
		this.namePos = 0;
		return decodeType2(typeName, length, start, end, true);
	}

	/*
	 * Build a type reference from a readable name, e.g. java.lang.Object[][]
	 */
	protected TypeReference createTypeReference(
		char[] typeName,
		int start,
		int end) {

		int length = typeName.length;
		this.namePos = 0;
		return decodeType2(typeName, length, start, end, false);
	}

	/*
	 * Build a type reference from a type signature, e.g. Ljava.lang.Object;
	 */
	protected TypeReference createTypeReference(
			String typeSignature,
			int start,
			int end) {

		int length = typeSignature.length();
		this.namePos = 0;
		return decodeType(typeSignature, length, start, end);
	}

	private TypeReference decodeType(String typeSignature, int length, int start, int end) {
		int identCount = 1;
		int dim = 0;
		int nameFragmentStart = this.namePos, nameFragmentEnd = -1;
		boolean nameStarted = false;
		ArrayList fragments = null;
		typeLoop: while (this.namePos < length) {
			char currentChar = typeSignature.charAt(this.namePos);
			switch (currentChar) {
				case Signature.C_BOOLEAN :
					if (!nameStarted) {
						this.namePos++;
						if (dim == 0)
							return new SingleTypeReference(TypeBinding.BOOLEAN.simpleName, ((long) start << 32) + end);
						else
							return new ArrayTypeReference(TypeBinding.BOOLEAN.simpleName, dim, ((long) start << 32) + end);
					}
					break;
				case Signature.C_BYTE :
					if (!nameStarted) {
						this.namePos++;
						if (dim == 0)
							return new SingleTypeReference(TypeBinding.BYTE.simpleName, ((long) start << 32) + end);
						else
							return new ArrayTypeReference(TypeBinding.BYTE.simpleName, dim, ((long) start << 32) + end);
					}
					break;
				case Signature.C_CHAR :
					if (!nameStarted) {
						this.namePos++;
						if (dim == 0)
							return new SingleTypeReference(TypeBinding.CHAR.simpleName, ((long) start << 32) + end);
						else
							return new ArrayTypeReference(TypeBinding.CHAR.simpleName, dim, ((long) start << 32) + end);
					}
					break;
				case Signature.C_DOUBLE :
					if (!nameStarted) {
						this.namePos++;
						if (dim == 0)
							return new SingleTypeReference(TypeBinding.DOUBLE.simpleName, ((long) start << 32) + end);
						else
							return new ArrayTypeReference(TypeBinding.DOUBLE.simpleName, dim, ((long) start << 32) + end);
					}
					break;
				case Signature.C_FLOAT :
					if (!nameStarted) {
						this.namePos++;
						if (dim == 0)
							return new SingleTypeReference(TypeBinding.FLOAT.simpleName, ((long) start << 32) + end);
						else
							return new ArrayTypeReference(TypeBinding.FLOAT.simpleName, dim, ((long) start << 32) + end);
					}
					break;
				case Signature.C_INT :
					if (!nameStarted) {
						this.namePos++;
						if (dim == 0)
							return new SingleTypeReference(TypeBinding.INT.simpleName, ((long) start << 32) + end);
						else
							return new ArrayTypeReference(TypeBinding.INT.simpleName, dim, ((long) start << 32) + end);
					}
					break;
				case Signature.C_LONG :
					if (!nameStarted) {
						this.namePos++;
						if (dim == 0)
							return new SingleTypeReference(TypeBinding.LONG.simpleName, ((long) start << 32) + end);
						else
							return new ArrayTypeReference(TypeBinding.LONG.simpleName, dim, ((long) start << 32) + end);
					}
					break;
				case Signature.C_SHORT :
					if (!nameStarted) {
						this.namePos++;
						if (dim == 0)
							return new SingleTypeReference(TypeBinding.SHORT.simpleName, ((long) start << 32) + end);
						else
							return new ArrayTypeReference(TypeBinding.SHORT.simpleName, dim, ((long) start << 32) + end);
					}
					break;
				case Signature.C_VOID :
					if (!nameStarted) {
						this.namePos++;
						return new SingleTypeReference(TypeBinding.VOID.simpleName, ((long) start << 32) + end);
					}
					break;
				case Signature.C_RESOLVED :
				case Signature.C_UNRESOLVED :
				case Signature.C_TYPE_VARIABLE :
					if (!nameStarted) {
						nameFragmentStart = this.namePos+1;
						nameStarted = true;
					}
					break;
				case Signature.C_STAR:
					this.namePos++;
					Wildcard result = new Wildcard(Wildcard.UNBOUND);
					result.sourceStart = start;
					result.sourceEnd = end;
					return result;
				case Signature.C_EXTENDS:
					this.namePos++;
					result = new Wildcard(Wildcard.EXTENDS);
					result.bound = decodeType(typeSignature, length, start, end);
					result.sourceStart = start;
					result.sourceEnd = end;
					return result;
				case Signature.C_SUPER:
					this.namePos++;
					result = new Wildcard(Wildcard.SUPER);
					result.bound = decodeType(typeSignature, length, start, end);
					result.sourceStart = start;
					result.sourceEnd = end;
					return result;
				case Signature.C_ARRAY :
					dim++;
					break;
				case Signature.C_GENERIC_END :
				case Signature.C_SEMICOLON :
					nameFragmentEnd = this.namePos-1;
					this.namePos++;
					break typeLoop;
				case Signature.C_DOLLAR:
					if (this.memberTypeSeparator != Signature.C_DOLLAR)
						break;
					// $FALL-THROUGH$
				case Signature.C_DOT :
					if (!nameStarted) {
						nameFragmentStart = this.namePos+1;
						nameStarted = true;
					} else if (this.namePos > nameFragmentStart) // handle name starting with a $ (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=91709)
						identCount ++;
					break;
				case Signature.C_GENERIC_START :
					nameFragmentEnd = this.namePos-1;
					// convert 1.5 specific constructs only if compliance is 1.5 or above
					if (!this.has1_5Compliance)
						break typeLoop;
					if (fragments == null) fragments = new ArrayList(2);
					addIdentifiers(typeSignature, nameFragmentStart, nameFragmentEnd + 1, identCount, fragments);
					this.namePos++; // skip '<'
					TypeReference[] arguments = decodeTypeArguments(typeSignature, length, start, end); // positionned on '>' at end
					fragments.add(arguments);
					identCount = 1;
					nameStarted = false;
					// next increment will skip '>'
					break;
			}
			this.namePos++;
		}
		if (fragments == null) { // non parameterized
			/* rebuild identifiers and dimensions */
			if (identCount == 1) { // simple type reference
				if (dim == 0) {
					char[] nameFragment = new char[nameFragmentEnd - nameFragmentStart + 1];
					typeSignature.getChars(nameFragmentStart, nameFragmentEnd +1, nameFragment, 0);
					return new SingleTypeReference(nameFragment, ((long) start << 32) + end);
				} else {
					char[] nameFragment = new char[nameFragmentEnd - nameFragmentStart + 1];
					typeSignature.getChars(nameFragmentStart, nameFragmentEnd +1, nameFragment, 0);
					return new ArrayTypeReference(nameFragment, dim, ((long) start << 32) + end);
				}
			} else { // qualified type reference
				long[] positions = new long[identCount];
				long pos = ((long) start << 32) + end;
				for (int i = 0; i < identCount; i++) {
					positions[i] = pos;
				}
				char[][] identifiers = extractIdentifiers(typeSignature, nameFragmentStart, nameFragmentEnd, identCount);
				if (dim == 0) {
					return new QualifiedTypeReference(identifiers, positions);
				} else {
					return new ArrayQualifiedTypeReference(identifiers, dim, positions);
				}
			}
		} else { // parameterized
			// rebuild type reference from available fragments: char[][], arguments, char[][], arguments...
			// check trailing qualified name
			if (nameStarted) {
				addIdentifiers(typeSignature, nameFragmentStart, nameFragmentEnd + 1, identCount, fragments);
			}
			int fragmentLength = fragments.size();
			if (fragmentLength == 2) {
				Object firstFragment = fragments.get(0);
				if (firstFragment instanceof char[]) {
					// parameterized single type
					return new ParameterizedSingleTypeReference((char[]) firstFragment, (TypeReference[]) fragments.get(1), dim, ((long) start << 32) + end);
				}
			}
			// parameterized qualified type
			identCount = 0;
			for (int i = 0; i < fragmentLength; i ++) {
				Object element = fragments.get(i);
				if (element instanceof char[][]) {
					identCount += ((char[][])element).length;
				} else if (element instanceof char[])
					identCount++;
			}
			char[][] tokens = new char[identCount][];
			TypeReference[][] arguments = new TypeReference[identCount][];
			int index = 0;
			for (int i = 0; i < fragmentLength; i ++) {
				Object element = fragments.get(i);
				if (element instanceof char[][]) {
					char[][] fragmentTokens = (char[][]) element;
					int fragmentTokenLength = fragmentTokens.length;
					System.arraycopy(fragmentTokens, 0, tokens, index, fragmentTokenLength);
					index += fragmentTokenLength;
				} else if (element instanceof char[]) {
					tokens[index++] = (char[]) element;
				} else {
					arguments[index-1] = (TypeReference[]) element;
				}
			}
			long[] positions = new long[identCount];
			long pos = ((long) start << 32) + end;
			for (int i = 0; i < identCount; i++) {
				positions[i] = pos;
			}
			return new ParameterizedQualifiedTypeReference(tokens, arguments, dim, positions);
		}
	}

	private TypeReference decodeType2(char[] typeName, int length, int start, int end, boolean includeGenericsAnyway) {
		int identCount = 1;
		int dim = 0;
		int nameFragmentStart = this.namePos, nameFragmentEnd = -1;
		ArrayList fragments = null;
		typeLoop: while (this.namePos < length) {
			char currentChar = typeName[this.namePos];
			switch (currentChar) {
				case '?' :
					this.namePos++; // skip '?'
					while (typeName[this.namePos] == ' ') this.namePos++;
					switch(typeName[this.namePos]) {
						case 's' :
							checkSuper: {
								int max = TypeConstants.WILDCARD_SUPER.length-1;
								for (int ahead = 1; ahead < max; ahead++) {
									if (typeName[this.namePos+ahead] != TypeConstants.WILDCARD_SUPER[ahead+1]) {
										break checkSuper;
									}
								}
								this.namePos += max;
								Wildcard result = new Wildcard(Wildcard.SUPER);
								result.bound = decodeType2(typeName, length, start, end, includeGenericsAnyway);
								result.sourceStart = start;
								result.sourceEnd = end;
								return result;
							}
							break;
						case 'e' :
							checkExtends: {
								int max = TypeConstants.WILDCARD_EXTENDS.length-1;
								for (int ahead = 1; ahead < max; ahead++) {
									if (typeName[this.namePos+ahead] != TypeConstants.WILDCARD_EXTENDS[ahead+1]) {
										break checkExtends;
									}
								}
								this.namePos += max;
								Wildcard result = new Wildcard(Wildcard.EXTENDS);
								result.bound = decodeType2(typeName, length, start, end, includeGenericsAnyway);
								result.sourceStart = start;
								result.sourceEnd = end;
								return result;
							}
							break;
					}
					Wildcard result = new Wildcard(Wildcard.UNBOUND);
					result.sourceStart = start;
					result.sourceEnd = end;
					return result;
				case '[' :
					if (dim == 0 && nameFragmentEnd < 0) nameFragmentEnd = this.namePos-1;
					dim++;
					break;
				case ']' :
					break;
				case '>' :
				case ',' :
					break typeLoop;
				case '.' :
					if (nameFragmentStart < 0) nameFragmentStart = this.namePos+1; // member type name
					identCount ++;
					break;
				case '<' :
					/* We need to convert and preserve 1.5 specific constructs either if compliance is 1.5 or above,
					   or the caller has explicitly requested generics to be included. The parameter includeGenericsAnyway
					   should be used by the caller to signal that in the calling context generics information must be
					   internalized even when the requesting project is 1.4. But in all cases, we must skip over them to
					   see if there are any applicable type fragments after the type parameters: i.e we just aren't done
					   having seen a '<' in 1.4 mode.

					   Because of the way type signatures are encoded, TypeConverter.decodeType(String, int, int, int) is immune
					   to this problem. See https://bugs.eclipse.org/bugs/show_bug.cgi?id=325633
					 */
					if (this.has1_5Compliance || includeGenericsAnyway) {
						if (fragments == null) fragments = new ArrayList(2);
					}
					nameFragmentEnd = this.namePos-1;
					if (this.has1_5Compliance || includeGenericsAnyway) {
						char[][] identifiers = CharOperation.splitOn('.', typeName, nameFragmentStart, this.namePos);
						fragments.add(identifiers);
					}
					this.namePos++; // skip '<'
					TypeReference[] arguments = decodeTypeArguments(typeName, length, start, end, includeGenericsAnyway); // positionned on '>' at end
					if (this.has1_5Compliance || includeGenericsAnyway) {
						fragments.add(arguments);
						identCount = 0;
						nameFragmentStart = -1;
						nameFragmentEnd = -1;
					}
					// next increment will skip '>'
					break;
			}
			this.namePos++;
		}
		return decodeType3(typeName, length, start, end, identCount, dim, nameFragmentStart, nameFragmentEnd,
				fragments);
	}

	/*
	 * Method should be inlined.
	 *
	 * Only extracted to work around https://bugs.eclipse.org/471835 :
	 * Random crashes in PhaseIdealLoop::build_loop_late_post when C2 JIT tries to compile TypeConverter::decodeType
	 */
	private TypeReference decodeType3(char[] typeName, int length, int start, int end, int identCount, int dim,
			int nameFragmentStart, int nameFragmentEnd, ArrayList fragments) {
		if (nameFragmentEnd < 0) nameFragmentEnd = this.namePos-1;
		if (fragments == null) { // non parameterized
			/* rebuild identifiers and dimensions */
			if (identCount == 1) { // simple type reference
				if (dim == 0) {
					char[] nameFragment;
					if (nameFragmentStart != 0 || nameFragmentEnd >= 0) {
						int nameFragmentLength = nameFragmentEnd - nameFragmentStart + 1;
						System.arraycopy(typeName, nameFragmentStart, nameFragment = new char[nameFragmentLength], 0, nameFragmentLength);
					} else {
						nameFragment = typeName;
					}
					return new SingleTypeReference(nameFragment, ((long) start << 32) + end);
				} else {
					int nameFragmentLength = nameFragmentEnd - nameFragmentStart + 1;
					char[] nameFragment = new char[nameFragmentLength];
					System.arraycopy(typeName, nameFragmentStart, nameFragment, 0, nameFragmentLength);
					return new ArrayTypeReference(nameFragment, dim, ((long) start << 32) + end);
				}
			} else { // qualified type reference
				long[] positions = new long[identCount];
				long pos = ((long) start << 32) + end;
				for (int i = 0; i < identCount; i++) {
					positions[i] = pos;
				}
				char[][] identifiers = CharOperation.splitOn('.', typeName, nameFragmentStart, nameFragmentEnd+1);
				if (dim == 0) {
					return new QualifiedTypeReference(identifiers, positions);
				} else {
					return new ArrayQualifiedTypeReference(identifiers, dim, positions);
				}
			}
		} else { // parameterized
			// rebuild type reference from available fragments: char[][], arguments, char[][], arguments...
			// check trailing qualified name
			if (nameFragmentStart > 0 && nameFragmentStart < length) {
				char[][] identifiers = CharOperation.splitOn('.', typeName, nameFragmentStart, nameFragmentEnd+1);
				fragments.add(identifiers);
			}
			int fragmentLength = fragments.size();
			if (fragmentLength == 2) {
				char[][] firstFragment = (char[][]) fragments.get(0);
				if (firstFragment.length == 1) {
					// parameterized single type
					return new ParameterizedSingleTypeReference(firstFragment[0], (TypeReference[]) fragments.get(1), dim, ((long) start << 32) + end);
				}
			}
			// parameterized qualified type
			identCount = 0;
			for (int i = 0; i < fragmentLength; i ++) {
				Object element = fragments.get(i);
				if (element instanceof char[][]) {
					identCount += ((char[][])element).length;
				}
			}
			char[][] tokens = new char[identCount][];
			TypeReference[][] arguments = new TypeReference[identCount][];
			int index = 0;
			for (int i = 0; i < fragmentLength; i ++) {
				Object element = fragments.get(i);
				if (element instanceof char[][]) {
					char[][] fragmentTokens = (char[][]) element;
					int fragmentTokenLength = fragmentTokens.length;
					System.arraycopy(fragmentTokens, 0, tokens, index, fragmentTokenLength);
					index += fragmentTokenLength;
				} else {
					arguments[index-1] = (TypeReference[]) element;
				}
			}
			long[] positions = new long[identCount];
			long pos = ((long) start << 32) + end;
			for (int i = 0; i < identCount; i++) {
				positions[i] = pos;
			}
			return new ParameterizedQualifiedTypeReference(tokens, arguments, dim, positions);
		}
	}

	private TypeReference[] decodeTypeArguments(char[] typeName, int length, int start, int end, boolean includeGenericsAnyway) {
		ArrayList argumentList = new ArrayList(1);
		int count = 0;
		argumentsLoop: while (this.namePos < length) {
			TypeReference argument = decodeType2(typeName, length, start, end, includeGenericsAnyway);
			count++;
			argumentList.add(argument);
			if (this.namePos >= length) break argumentsLoop;
			if (typeName[this.namePos] == '>') {
				break argumentsLoop;
			}
			this.namePos++; // skip ','
		}
		TypeReference[] typeArguments = new TypeReference[count];
		argumentList.toArray(typeArguments);
		return typeArguments;
	}

	private TypeReference[] decodeTypeArguments(String typeSignature, int length, int start, int end) {
		ArrayList argumentList = new ArrayList(1);
		int count = 0;
		argumentsLoop: while (this.namePos < length) {
			TypeReference argument = decodeType(typeSignature, length, start, end);
			count++;
			argumentList.add(argument);
			if (this.namePos >= length) break argumentsLoop;
			if (typeSignature.charAt(this.namePos) == Signature.C_GENERIC_END) {
				break argumentsLoop;
			}
		}
		TypeReference[] typeArguments = new TypeReference[count];
		argumentList.toArray(typeArguments);
		return typeArguments;
	}

	private char[][] extractIdentifiers(String typeSignature, int start, int endInclusive, int identCount) {
		char[][] result = new char[identCount][];
		int charIndex = start;
		int i = 0;
		while (charIndex < endInclusive) {
			char currentChar;
			if ((currentChar = typeSignature.charAt(charIndex)) == this.memberTypeSeparator || currentChar == Signature.C_DOT) {
				typeSignature.getChars(start, charIndex, result[i++] = new char[charIndex - start], 0);
				start = ++charIndex;
			} else
				charIndex++;
		}
		typeSignature.getChars(start, charIndex + 1, result[i++] = new char[charIndex - start + 1], 0);
		return result;
	}
}
