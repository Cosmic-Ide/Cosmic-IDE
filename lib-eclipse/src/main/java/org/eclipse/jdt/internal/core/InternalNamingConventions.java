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
package org.eclipse.jdt.internal.core;

import java.util.Map;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.eclipse.jdt.internal.codeassist.impl.AssistOptions;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.parser.Scanner;
import org.eclipse.jdt.internal.compiler.parser.ScannerHelper;
import org.eclipse.jdt.internal.compiler.parser.TerminalTokens;
import org.eclipse.jdt.internal.compiler.util.SimpleSetOfCharArray;

public class InternalNamingConventions {
	private static final char[] DEFAULT_NAME = "name".toCharArray(); //$NON-NLS-1$

	private static Scanner getNameScanner(CompilerOptions compilerOptions) {
		return
			new Scanner(
				false /*comment*/,
				false /*whitespace*/,
				false /*nls*/,
				compilerOptions.sourceLevel /*sourceLevel*/,
				null /*taskTags*/,
				null/*taskPriorities*/,
				true/*taskCaseSensitive*/,
				compilerOptions.enablePreviewFeatures);
	}

	private static void acceptName(
		char[] name,
		char[] prefix,
		char[] suffix,
		boolean isFirstPrefix,
		boolean isFirstSuffix,
		int reusedCharacters,
		INamingRequestor requestor) {
		if(prefix.length > 0 && suffix.length > 0) {
			requestor.acceptNameWithPrefixAndSuffix(name, isFirstPrefix, isFirstSuffix, reusedCharacters);
		} else if(prefix.length > 0){
			requestor.acceptNameWithPrefix(name, isFirstPrefix, reusedCharacters);
		} else if(suffix.length > 0){
			requestor.acceptNameWithSuffix(name, isFirstSuffix, reusedCharacters);
		} else {
			requestor.acceptNameWithoutPrefixAndSuffix(name, reusedCharacters);
		}
	}

	private static char[][] computeBaseTypeNames(char[] typeName, boolean isConstantField, char[][] excludedNames){
		if (isConstantField) {
			return computeNonBaseTypeNames(typeName, isConstantField, false);
		} else {
			char[] name = computeBaseTypeNames(typeName[0], excludedNames);
			if(name != null) {
				return new char[][]{name};
			} else {
				// compute variable name like from non base type
				return computeNonBaseTypeNames(typeName, isConstantField, false);
			}
		}
	}
	private static char[] computeBaseTypeNames(char firstName, char[][] excludedNames){
		char[] name = new char[]{firstName};

		for(int i = 0 ; i < excludedNames.length ; i++){
			if(CharOperation.equals(name, excludedNames[i], false)) {
				name[0]++;
				if(name[0] > 'z')
					name[0] = 'a';
				if(name[0] == firstName)
					return null;
				i = 0;
			}
		}

		return name;
	}

	private static char[][] computeNonBaseTypeNames(char[] sourceName, boolean isConstantField, boolean onlyLongest){
		int length = sourceName.length;

		if (length == 0) {
			return CharOperation.NO_CHAR_CHAR;
		}

		if (length == 1) {
			if (isConstantField) {
				return generateConstantName(new char[][]{CharOperation.toLowerCase(sourceName)}, 0, onlyLongest);
			} else {
				return generateNonConstantName(new char[][]{CharOperation.toLowerCase(sourceName)}, 0, onlyLongest);
			}
		}

		char[][] nameParts = new char[length][];
		int namePartsPtr = -1;

		int endIndex = length;
		char c = sourceName[length - 1];

		final int IS_LOWER_CASE = 1;
		final int IS_UPPER_CASE = 2;
		final int IS_UNDERSCORE = 3;
		final int IS_OTHER = 4;

		int previousCharKind =
			ScannerHelper.isLowerCase(c) ? IS_LOWER_CASE :
				ScannerHelper.isUpperCase(c) ? IS_UPPER_CASE :
					c == '_' ? IS_UNDERSCORE : IS_OTHER;

		for(int i = length - 1 ; i >= 0 ; i--){
			c = sourceName[i];

			int charKind =
				ScannerHelper.isLowerCase(c) ? IS_LOWER_CASE :
					ScannerHelper.isUpperCase(c) ? IS_UPPER_CASE :
						c == '_' ? IS_UNDERSCORE : IS_OTHER;

			switch (charKind) {
				case IS_LOWER_CASE:
					if (previousCharKind == IS_UPPER_CASE) {
						nameParts[++namePartsPtr] = CharOperation.subarray(sourceName, i + 1, endIndex);
						endIndex = i + 1;
					}
					previousCharKind = IS_LOWER_CASE;
					break;
				case IS_UPPER_CASE:
					if (previousCharKind == IS_LOWER_CASE) {
						nameParts[++namePartsPtr] = CharOperation.subarray(sourceName, i, endIndex);
						if (i > 0) {
							char pc = sourceName[i - 1];
							previousCharKind =
								ScannerHelper.isLowerCase(pc) ? IS_LOWER_CASE :
									ScannerHelper.isUpperCase(pc) ? IS_UPPER_CASE :
										pc == '_' ? IS_UNDERSCORE : IS_OTHER;
						}
						endIndex = i;
					} else {
						previousCharKind = IS_UPPER_CASE;
					}
					break;
				case IS_UNDERSCORE:
					switch (previousCharKind) {
						case IS_UNDERSCORE:
							// https://bugs.eclipse.org/bugs/show_bug.cgi?id=283539
							// Process consecutive underscores only for constant types
							if (isConstantField) {
								if (i > 0) {
									char pc = sourceName[i - 1];
									previousCharKind =
										ScannerHelper.isLowerCase(pc) ? IS_LOWER_CASE :
											ScannerHelper.isUpperCase(pc) ? IS_UPPER_CASE :
												pc == '_' ? IS_UNDERSCORE : IS_OTHER;
								}
								endIndex = i;
							}
							break;
						case IS_LOWER_CASE:
						case IS_UPPER_CASE:
							nameParts[++namePartsPtr] = CharOperation.subarray(sourceName, i + 1, endIndex);
							if (i > 0) {
								char pc = sourceName[i - 1];
								previousCharKind =
									ScannerHelper.isLowerCase(pc) ? IS_LOWER_CASE :
										ScannerHelper.isUpperCase(pc) ? IS_UPPER_CASE :
											pc == '_' ? IS_UNDERSCORE : IS_OTHER;
							}
							// Include the '_' also. E.g. My_word -> "My_" and "word".
							endIndex = i+1;
							break;
						default:
							previousCharKind = IS_UNDERSCORE;
							break;
					}
					break;
				default:
					previousCharKind = IS_OTHER;
					break;
			}
		}
		if (endIndex > 0) {
			nameParts[++namePartsPtr] = CharOperation.subarray(sourceName, 0, endIndex);
		}
		if (namePartsPtr == -1) {
			return new char[][] { sourceName };
		}

		if (isConstantField) {
			return generateConstantName(nameParts, namePartsPtr, onlyLongest);
		} else {
			return generateNonConstantName(nameParts, namePartsPtr, onlyLongest);
		}
	}



	private static char[] excludeNames(
		char[] suffixName,
		char[] prefixName,
		char[] suffix,
		char[][] excludedNames) {
		int count = 2;
		int m = 0;
		while (m < excludedNames.length) {
			if(CharOperation.equals(suffixName, excludedNames[m], false)) {
				suffixName = CharOperation.concat(
					prefixName,
					String.valueOf(count++).toCharArray(),
					suffix
				);
				m = 0;
			} else {
				m++;
			}
		}
		return suffixName;
	}

	private static char[][] generateNonConstantName(char[][] nameParts, int namePartsPtr, boolean onlyLongest) {
		char[][] names;
		if (onlyLongest) {
			names = new char[1][];
		} else {
			names = new char[namePartsPtr + 1][];
		}

		char[] namePart = nameParts[0];

		char[] name = CharOperation.toLowerCase(namePart);

		if (!onlyLongest) {
			names[namePartsPtr] = name;
		}

		char[] nameSuffix = namePart;

		for (int i = 1; i <= namePartsPtr; i++) {
			namePart = nameParts[i];

			name = CharOperation.concat(CharOperation.toLowerCase(namePart), nameSuffix);
			// https://bugs.eclipse.org/bugs/show_bug.cgi?id=283539
			// Only the first word is converted to lower case and the rest of them are not changed for non-constants

			if (!onlyLongest) {
				names[namePartsPtr - i] = name;
			}

			nameSuffix = CharOperation.concat(namePart, nameSuffix);
		}
		if (onlyLongest) {
			names[0] = name;
		}
		return names;
	}

	private static char[][] generateConstantName(char[][] nameParts, int namePartsPtr, boolean onlyLongest) {
		char[][] names;
		if (onlyLongest) {
			names = new char[1][];
		} else {
			names = new char[namePartsPtr + 1][];
		}

		char[] namePart = CharOperation.toUpperCase(nameParts[0]);
		int namePartLength = namePart.length;
		System.arraycopy(namePart, 0, namePart, 0, namePartLength);

		char[] name = namePart;

		if (!onlyLongest) {
			names[namePartsPtr] = name;
		}

		for (int i = 1; i <= namePartsPtr; i++) {
			namePart = CharOperation.toUpperCase(nameParts[i]);
			namePartLength = namePart.length;
			if (namePart[namePartLength - 1] != '_') {
				name = CharOperation.concat(namePart, name, '_');
			} else {
				name = CharOperation.concat(namePart, name);
			}

			if (!onlyLongest) {
				names[namePartsPtr - i] = name;
			}
		}
		if (onlyLongest) {
			names[0] = name;
		}
		return names;
	}

	public static char[] getBaseName(
			int variableKind,
			IJavaProject javaProject,
			char[] name,
			boolean updateFirstCharacter) {

		AssistOptions assistOptions;
		if (javaProject != null) {
			assistOptions = new AssistOptions(javaProject.getOptions(true));
		} else {
			assistOptions = new AssistOptions(JavaCore.getOptions());
		}

		char[][] prefixes = null;
		char[][] suffixes = null;
		switch (variableKind) {
			case VK_INSTANCE_FIELD:
				prefixes = assistOptions.fieldPrefixes;
				suffixes = assistOptions.fieldSuffixes;
				break;
			case VK_STATIC_FIELD:
				prefixes = assistOptions.staticFieldPrefixes;
				suffixes = assistOptions.staticFieldSuffixes;
				break;
			case VK_STATIC_FINAL_FIELD:
				prefixes = assistOptions.staticFinalFieldPrefixes;
				suffixes = assistOptions.staticFinalFieldSuffixes;
				break;
			case VK_LOCAL:
				prefixes = assistOptions.localPrefixes;
				suffixes = assistOptions.localSuffixes;
				break;
			case VK_PARAMETER:
				prefixes = assistOptions.argumentPrefixes;
				suffixes = assistOptions.argumentSuffixes;
				break;
		}


		return getBaseName(name, prefixes, suffixes, variableKind == VK_STATIC_FINAL_FIELD, updateFirstCharacter);
	}

	private static char[] getBaseName(char[] name, char[][] prefixes, char[][] suffixes, boolean isConstant, boolean updateFirstCharacter) {
		char[] nameWithoutPrefixAndSiffix = removeVariablePrefixAndSuffix(name, prefixes, suffixes, updateFirstCharacter);

		char[] baseName;
		if (isConstant) {
			int length = nameWithoutPrefixAndSiffix.length;
			baseName = new char[length];
			int baseNamePtr = -1;

			boolean previousIsUnderscore = false;
			for (int i = 0; i < length; i++) {
				char c = nameWithoutPrefixAndSiffix[i];
				if (c != '_') {
					if (previousIsUnderscore) {
						baseName[++baseNamePtr] = ScannerHelper.toUpperCase(c);
						previousIsUnderscore = false;
					} else {
						baseName[++baseNamePtr] = ScannerHelper.toLowerCase(c);
					}
				} else {
					previousIsUnderscore = true;
				}
			}
			System.arraycopy(baseName, 0, baseName = new char[baseNamePtr + 1], 0, baseNamePtr + 1);
		} else {
			baseName = nameWithoutPrefixAndSiffix;
		}

		return baseName;
	}

	public static char[] removeVariablePrefixAndSuffix(
			int variableKind,
			IJavaProject javaProject,
			char[] name) {
		AssistOptions assistOptions;
		if (javaProject != null) {
			assistOptions = new AssistOptions(javaProject.getOptions(true));
		} else {
			assistOptions = new AssistOptions(JavaCore.getOptions());
		}

		char[][] prefixes = null;
		char[][] suffixes = null;
		switch (variableKind) {
			case VK_INSTANCE_FIELD:
				prefixes = assistOptions.fieldPrefixes;
				suffixes = assistOptions.fieldSuffixes;
				break;
			case VK_STATIC_FIELD:
				prefixes = assistOptions.staticFieldPrefixes;
				suffixes = assistOptions.staticFieldSuffixes;
				break;
			case VK_STATIC_FINAL_FIELD:
				prefixes = assistOptions.staticFinalFieldPrefixes;
				suffixes = assistOptions.staticFinalFieldSuffixes;
				break;
			case VK_LOCAL:
				prefixes = assistOptions.localPrefixes;
				suffixes = assistOptions.localSuffixes;
				break;
			case VK_PARAMETER:
				prefixes = assistOptions.argumentPrefixes;
				suffixes = assistOptions.argumentSuffixes;
				break;
		}

		return InternalNamingConventions.removeVariablePrefixAndSuffix(name, prefixes, suffixes, true);
	}

	private static char[] removeVariablePrefixAndSuffix(char[] name, char[][] prefixes, char[][] suffixes, boolean updateFirstCharacter) {
		// remove longer prefix
		char[] withoutPrefixName = name;
		if (prefixes != null) {
			int bestLength = 0;
			for (int i= 0; i < prefixes.length; i++) {
				char[] prefix = prefixes[i];
				if (CharOperation.prefixEquals(prefix, name)) {
					int currLen = prefix.length;
					boolean lastCharIsLetter = ScannerHelper.isLetter(prefix[currLen - 1]);
					if(!lastCharIsLetter || (lastCharIsLetter && name.length > currLen && ScannerHelper.isUpperCase(name[currLen]))) {
						if (bestLength < currLen && name.length != currLen) {
							withoutPrefixName = CharOperation.subarray(name, currLen, name.length);
							bestLength = currLen;
						}
					}
				}
			}
		}

		// remove longer suffix
		char[] withoutSuffixName = withoutPrefixName;
		if(suffixes != null) {
			int bestLength = 0;
			for (int i = 0; i < suffixes.length; i++) {
				char[] suffix = suffixes[i];
				if(CharOperation.endsWith(withoutPrefixName, suffix)) {
					int currLen = suffix.length;
					if(bestLength < currLen && withoutPrefixName.length != currLen) {
						withoutSuffixName = CharOperation.subarray(withoutPrefixName, 0, withoutPrefixName.length - currLen);
						bestLength = currLen;
					}
				}
			}
		}

		if (updateFirstCharacter) withoutSuffixName[0] = ScannerHelper.toLowerCase(withoutSuffixName[0]);
		return withoutSuffixName;
	}

	private static char[] removePrefix(char[] name, char[][] prefixes) {
		// remove longer prefix
		char[] withoutPrefixName = name;
		if (prefixes != null) {
			int bestLength = 0;
			int nameLength = name.length;
			for (int i= 0; i < prefixes.length; i++) {
				char[] prefix = prefixes[i];

				int prefixLength = prefix.length;
				if(prefixLength <= nameLength) {
					if(CharOperation.prefixEquals(prefix, name, false)) {
						if (prefixLength > bestLength) {
							bestLength = prefixLength;
						}
					}
				} else {
					int currLen = 0;
					for (; currLen < nameLength; currLen++) {
						if(ScannerHelper.toLowerCase(prefix[currLen]) != ScannerHelper.toLowerCase(name[currLen])) {
							if (currLen > bestLength) {
								bestLength = currLen;
							}
							break;
						}
					}
					if(currLen == nameLength && currLen > bestLength) {
						bestLength = currLen;
					}
				}
			}
			if(bestLength > 0) {
				if(bestLength == nameLength) {
					withoutPrefixName = CharOperation.NO_CHAR;
				} else {
					withoutPrefixName = CharOperation.subarray(name, bestLength, nameLength);
				}
			}
		}
//
//
//		// remove longer prefix
//		char[] withoutPrefixName = name;
//		if (prefixes != null) {
//			int bestLength = 0;
//			for (int i= 0; i < prefixes.length; i++) {
//				char[] prefix = prefixes[i];
//				int max = prefix.length < name.length ? prefix.length : name.length;
//				int currLen = 0;
//				for (; currLen < max; currLen++) {
//					if(Character.toLowerCase(prefix[currLen]) != Character.toLowerCase(name[currLen])) {
//						if (currLen > bestLength) {
//							bestLength = currLen;
//						}
//						break;
//					}
//				}
//				if(currLen == max && currLen > bestLength) {
//					bestLength = max;
//				}
//			}
//			if(bestLength > 0) {
//				if(bestLength == name.length) {
//					withoutPrefixName = CharOperation.NO_CHAR;
//				} else {
//					withoutPrefixName = CharOperation.subarray(name, bestLength, name.length);
//				}
//			}
//		}

		return withoutPrefixName;
	}

	public static final int VK_STATIC_FIELD = 1;
	public static final int VK_INSTANCE_FIELD = 2;
	public static final int VK_STATIC_FINAL_FIELD = 3;
	public static final int VK_PARAMETER = 4;
	public static final int VK_LOCAL = 5;

	public static final int BK_SIMPLE_NAME = 1;
	public static final int BK_SIMPLE_TYPE_NAME = 2;

	public static void suggestVariableNames(
			int variableKind,
			int baseNameKind,
			char[] baseName,
			IJavaProject javaProject,
			int dim,
			char[] internalPrefix,
			char[][] excluded,
			boolean evaluateDefault,
			INamingRequestor requestor) {

		if(baseName == null || baseName.length == 0)
			return;

		Map<String, String> options;
		if (javaProject != null) {
			options = javaProject.getOptions(true);
		} else {
			options = JavaCore.getOptions();
		}
		CompilerOptions compilerOptions = new CompilerOptions(options);
		AssistOptions assistOptions = new AssistOptions(options);

		boolean isConstantField = false;

		char[][] prefixes = null;
		char[][] suffixes = null;
		switch (variableKind) {
			case VK_INSTANCE_FIELD:
				prefixes = assistOptions.fieldPrefixes;
				suffixes = assistOptions.fieldSuffixes;
				break;
			case VK_STATIC_FIELD:
				prefixes = assistOptions.staticFieldPrefixes;
				suffixes = assistOptions.staticFieldSuffixes;
				break;
			case VK_STATIC_FINAL_FIELD:
				isConstantField = true;
				prefixes = assistOptions.staticFinalFieldPrefixes;
				suffixes = assistOptions.staticFinalFieldSuffixes;
				break;
			case VK_LOCAL:
				prefixes = assistOptions.localPrefixes;
				suffixes = assistOptions.localSuffixes;
				break;
			case VK_PARAMETER:
				prefixes = assistOptions.argumentPrefixes;
				suffixes = assistOptions.argumentSuffixes;
				break;
		}

		if(prefixes == null || prefixes.length == 0) {
			prefixes = new char[1][0];
		} else {
			int length = prefixes.length;
			System.arraycopy(prefixes, 0, prefixes = new char[length+1][], 0, length);
			prefixes[length] = CharOperation.NO_CHAR;
		}

		if(suffixes == null || suffixes.length == 0) {
			suffixes = new char[1][0];
		} else {
			int length = suffixes.length;
			System.arraycopy(suffixes, 0, suffixes = new char[length+1][], 0, length);
			suffixes[length] = CharOperation.NO_CHAR;
		}

		if(internalPrefix == null) {
			internalPrefix = CharOperation.NO_CHAR;
		} else {
			internalPrefix = removePrefix(internalPrefix, prefixes);
		}

		char[][] tempNames = null;

		Scanner nameScanner = getNameScanner(compilerOptions);
		if (baseNameKind == BK_SIMPLE_TYPE_NAME) {
			boolean isBaseType = false;

			try{
				nameScanner.setSource(baseName);
				switch (nameScanner.getNextToken()) {
					case TerminalTokens.TokenNameint :
					case TerminalTokens.TokenNamebyte :
					case TerminalTokens.TokenNameshort :
					case TerminalTokens.TokenNamechar :
					case TerminalTokens.TokenNamelong :
					case TerminalTokens.TokenNamefloat :
					case TerminalTokens.TokenNamedouble :
					case TerminalTokens.TokenNameboolean :
						isBaseType = true;
						break;
				}
			} catch(InvalidInputException e){
				// ignore
			}
			if (isBaseType) {
				// compute variable name from base type
				if (internalPrefix.length > 0) return;

				tempNames = computeBaseTypeNames(baseName, isConstantField, excluded);
			} else {
				// compute variable name for non base type
				tempNames = computeNonBaseTypeNames(baseName, isConstantField, false);
			}
		} else {
			tempNames = computeNonBaseTypeNames(baseName, isConstantField, true);
		}

		boolean acceptDefaultName = true;
		SimpleSetOfCharArray foundNames = new SimpleSetOfCharArray();

		for (int i = 0; i < tempNames.length; i++) {
			char[] tempName = tempNames[i];

			// add English plural form is necessary
			if(dim > 0) {
				int length = tempName.length;

				if (isConstantField) {
					if (tempName[length-1] == 'S'){
						if(tempName.length > 1 && tempName[length-2] == 'S') {
							System.arraycopy(tempName, 0, tempName = new char[length + 2], 0, length);
							tempName[length] = 'E';
							tempName[length+1] = 'S';
						}
					} else if(tempName[length-1] == 'Y') {
						boolean precededByAVowel = false;
						if(tempName.length > 1) {
							switch (tempName[length-2]) {
								case 'A':
								case 'E':
								case 'I':
								case 'O':
								case 'U':
									precededByAVowel = true;
									break;
							}
						}
						if (precededByAVowel) {
							System.arraycopy(tempName, 0, tempName = new char[length + 1], 0, length);
							tempName[length] = 'S';
						} else {
							System.arraycopy(tempName, 0, tempName = new char[length + 2], 0, length);
							tempName[length-1] = 'I';
							tempName[length] = 'E';
							tempName[length+1] = 'S';
						}
					} else {
						System.arraycopy(tempName, 0, tempName = new char[length + 1], 0, length);
						tempName[length] = 'S';
					}
				} else {
					if (tempName[length-1] == 's'){
						if(tempName.length > 1 && tempName[length-2] == 's') {
							System.arraycopy(tempName, 0, tempName = new char[length + 2], 0, length);
							tempName[length] = 'e';
							tempName[length+1] = 's';
						}
					} else if(tempName[length-1] == 'y') {
						boolean precededByAVowel = false;
						if(tempName.length > 1) {
							switch (tempName[length-2]) {
								case 'a':
								case 'e':
								case 'i':
								case 'o':
								case 'u':
									precededByAVowel = true;
									break;
							}
						}
						if (precededByAVowel) {
							System.arraycopy(tempName, 0, tempName = new char[length + 1], 0, length);
							tempName[length] = 's';
						} else {
							System.arraycopy(tempName, 0, tempName = new char[length + 2], 0, length);
							tempName[length-1] = 'i';
							tempName[length] = 'e';
							tempName[length+1] = 's';
						}
					} else {
						System.arraycopy(tempName, 0, tempName = new char[length + 1], 0, length);
						tempName[length] = 's';
					}
				}
			}

			char[] unprefixedName = tempName;

			int matchingIndex = -1;
			if (!isConstantField) {
				unprefixedName[0] = ScannerHelper.toUpperCase(unprefixedName[0]);

				done : for (int j = 0; j <= internalPrefix.length; j++) {
					if(j == internalPrefix.length ||
							CharOperation.prefixEquals(CharOperation.subarray(internalPrefix, j, -1), unprefixedName, j != 0 /*do not check case when there is no prefix*/)) {
						matchingIndex = j;
						break done;
					}
				}
			} else {
				done : for (int j = 0; j <= internalPrefix.length; j++) {
					if(j == internalPrefix.length) {
						matchingIndex = j;
						break done;
					} else if(CharOperation.prefixEquals(CharOperation.subarray(internalPrefix, j, -1), unprefixedName, j != 0 /*do not check case when there is no prefix*/)) {
						if (j == 0 || internalPrefix[j - 1] == '_') {
							matchingIndex = j;
							break done;
						}

					}
				}
			}

			if(matchingIndex > -1) {
				if (!isConstantField) {
					tempName = CharOperation.concat(CharOperation.subarray(internalPrefix, 0, matchingIndex), unprefixedName);
					if(matchingIndex == 0) tempName[0] = ScannerHelper.toLowerCase(tempName[0]);
				} else {
					if(matchingIndex != 0 && tempName[0] != '_' && internalPrefix[matchingIndex - 1] != '_') {
						tempName = CharOperation.concat(CharOperation.subarray(CharOperation.toUpperCase(internalPrefix), 0, matchingIndex), unprefixedName, '_');
					} else {
						tempName = CharOperation.concat(CharOperation.subarray(CharOperation.toUpperCase(internalPrefix), 0, matchingIndex), unprefixedName);
					}
				}

				for (int k = 0; k < prefixes.length; k++) {
					if (!isConstantField) {
						if(prefixes[k].length > 0
							&& ScannerHelper.isLetterOrDigit(prefixes[k][prefixes[k].length - 1])) {
							tempName[0] = ScannerHelper.toUpperCase(tempName[0]);
						} else {
							tempName[0] = ScannerHelper.toLowerCase(tempName[0]);
						}
					}
					char[] prefixName = CharOperation.concat(prefixes[k], tempName);
					for (int l = 0; l < suffixes.length; l++) {
						char[] suffixName = CharOperation.concat(prefixName, suffixes[l]);
						suffixName =
							excludeNames(
								suffixName,
								prefixName,
								suffixes[l],
								excluded);
						try{
							nameScanner.setSource(suffixName);
							switch (nameScanner.getNextToken()) {
								case TerminalTokens.TokenNameIdentifier :
									int token = nameScanner.getNextToken();
									if (token == TerminalTokens.TokenNameEOF && nameScanner.startPosition == suffixName.length) {
										if (!foundNames.includes(suffixName)) {
											acceptName(suffixName, prefixes[k], suffixes[l],  k == 0, l == 0, internalPrefix.length - matchingIndex, requestor);
											foundNames.add(suffixName);
											acceptDefaultName = false;
										}
									}
									break;
								default:
									suffixName = CharOperation.concat(
										prefixName,
										String.valueOf(1).toCharArray(),
										suffixes[l]
									);
									suffixName =
										excludeNames(
											suffixName,
											prefixName,
											suffixes[l],
											excluded);
									nameScanner.setSource(suffixName);
									switch (nameScanner.getNextToken()) {
										case TerminalTokens.TokenNameIdentifier :
											token = nameScanner.getNextToken();
											if (token == TerminalTokens.TokenNameEOF && nameScanner.startPosition == suffixName.length) {
												if (!foundNames.includes(suffixName)) {
													acceptName(suffixName, prefixes[k], suffixes[l], k == 0, l == 0, internalPrefix.length - matchingIndex, requestor);
													foundNames.add(suffixName);
													acceptDefaultName = false;
												}
											}
									}
							}
						} catch(InvalidInputException e){
							// ignore
						}
					}
				}
			}
		}
		// if no names were found
		if(evaluateDefault && acceptDefaultName) {
			char[] name = excludeNames(DEFAULT_NAME, DEFAULT_NAME, CharOperation.NO_CHAR, excluded);
			requestor.acceptNameWithoutPrefixAndSuffix(name, 0);
		}
	}
}
