/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
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
 *     Tim Hanson <thanson@bea.com> - fix for https://bugs.eclipse.org/bugs/show_bug.cgi?id=137634
 *     Sebastian Zarnekow - Contribution for
 *								Bug 545491 - Poor performance of ReferenceCollection with many source files
 *******************************************************************************/
package org.eclipse.jdt.internal.core.builder;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.lookup.CompilationUnitScope;
import org.eclipse.jdt.internal.compiler.lookup.TypeConstants;
import org.eclipse.jdt.internal.compiler.util.SortedCharArrays;

public class ReferenceCollection {

// contains no simple names as in just 'a' which is kept in simpleNameReferences instead
// TODO after #addDependencies, it will contain simple names, though. See ReferenceCollectionTest
char[][][] qualifiedNameReferences;
char[][] simpleNameReferences;
char[][] rootReferences;

protected ReferenceCollection(char[][][] qualifiedNameReferences, char[][] simpleNameReferences, char[][] rootReferences) {
	this.qualifiedNameReferences = internQualifiedNames(qualifiedNameReferences, false);
	this.simpleNameReferences = internSimpleNames(simpleNameReferences, true);
	this.rootReferences = internSimpleNames(rootReferences, false);
}

/**
 * Add the given fully qualified names to this reference collection.
 * Subsequent queries of {@link #includes(char[][][], char[][], char[][])} will report true
 * if the given names intersect with one of the added type name dependencies.
 *
 * @see CompilationUnitScope#recordQualifiedReference
 */
public void addDependencies(String[] typeNameDependencies) {
	// if each qualified type name is already known then all of its subNames can be skipped
	// and its expected that very few qualified names in typeNameDependencies need to be added
	// but could always take 'p1.p2.p3.X' and make all qualified names 'p1' 'p1.p2' 'p1.p2.p3' 'p1.p2.p3.X', then intern
	next: for(String typeNameDependency: typeNameDependencies) {
		char[][] qualifiedTypeName = CharOperation.splitOn('.', typeNameDependency.toCharArray());
		if (!isWellKnownQualifiedName(qualifiedTypeName)) {
			int qLength = qualifiedTypeName.length;
			QualifiedNameSet internedNames = InternedQualifiedNames[qLength <= MaxQualifiedNames ? qLength - 1 : 0];
			qualifiedTypeName = internSimpleNames(qualifiedTypeName, false, false);
			qualifiedTypeName = internedNames.add(qualifiedTypeName);
			int idx;
			while ((idx = Arrays.binarySearch(this.qualifiedNameReferences, qualifiedTypeName, SortedCharArrays.CHAR_CHAR_ARR_COMPARATOR)) < 0) {
				this.simpleNameReferences = ensureContainedInSortedOrder(this.simpleNameReferences, qualifiedTypeName[qualifiedTypeName.length - 1]);
				this.rootReferences = ensureContainedInSortedOrder(this.rootReferences, qualifiedTypeName[0]);

				int length = this.qualifiedNameReferences.length;
				idx = -(idx+1);
				this.qualifiedNameReferences = SortedCharArrays.insertIntoArray(this.qualifiedNameReferences, new char[length + 1][][], qualifiedTypeName, idx, this.qualifiedNameReferences.length);

				qualifiedTypeName = CharOperation.subarray(qualifiedTypeName, 0, qualifiedTypeName.length - 1);
				char[][][] temp = internQualifiedNames(new char[][][] {qualifiedTypeName}, false);
				if (temp == EmptyQualifiedNames)
					continue next; // qualifiedTypeName is a well known name
				qualifiedTypeName = temp[0];
			}
		}
	}
}

public boolean includes(char[] simpleName) {
	boolean result = sortedArrayContains(this.simpleNameReferences, simpleName, SortedCharArrays.CHAR_ARR_COMPARATOR);
	if (REFERENCE_COLLECTION_DEBUG) {
		assertIncludes(result, simpleName);
	}
	return result;
}

public boolean includes(char[][] qualifiedName) {
	boolean result = sortedArrayContains(this.qualifiedNameReferences, qualifiedName, SortedCharArrays.CHAR_CHAR_ARR_COMPARATOR);
	if (REFERENCE_COLLECTION_DEBUG) {
		assertIncludes(result, qualifiedName);
	}
	return result;
}

private static String qualifiedNamesToString(char[][][] qualifiedNames) {
	if (qualifiedNames == null)
		return "null"; //$NON-NLS-1$
	return Arrays.stream(qualifiedNames).map(CharOperation::toString).collect(Collectors.joining(",")); //$NON-NLS-1$
}

/**
 * @deprecated
 */
public boolean includes(char[][][] qualifiedNames, char[][] simpleNames) {
	return includes(qualifiedNames, simpleNames, null);
}

public boolean includes(char[][][] qualifiedNames, char[][] simpleNames, char[][] rootNames) {
	boolean result = doIncludes(qualifiedNames, simpleNames, rootNames);
	if (REFERENCE_COLLECTION_DEBUG) {
		assertIncludes(result, qualifiedNames, simpleNames, rootNames);
	}
	return result;
}

private boolean doIncludes(char[][][] qualifiedNames, char[][] simpleNames, char[][] rootNames) {
	if (rootNames != null) {
		if (!includesRootName(rootNames))
			return false;
	}
	// if either collection of names is null, it means it contained a well known name so we know it already has a match
	if (simpleNames == null || qualifiedNames == null) {
		if (simpleNames == null && qualifiedNames == null) {
			if (JavaBuilder.DEBUG)
				System.out.println("Found well known match"); //$NON-NLS-1$
			return true;
		} else if (qualifiedNames == null) {
			return includesSimpleName(simpleNames);
		}
		return includesQualifiedName(qualifiedNames);
	}

	if (simpleNames.length <= qualifiedNames.length) {
		return includesSimpleName(simpleNames) && includesQualifiedName(qualifiedNames);
	} else {
		return includesQualifiedName(qualifiedNames) && includesSimpleName(simpleNames);
	}
}

public boolean insideRoot(char[] rootName) {
	boolean result = sortedArrayContains(this.rootReferences, rootName, SortedCharArrays.CHAR_ARR_COMPARATOR);
	if (REFERENCE_COLLECTION_DEBUG) {
		if (result != debugIncludes(rootName)) {
			String message = "Mismatch: " + String.valueOf(rootName) + (result ? " should not " : " should ") + " be included in "  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
					+ Arrays.asList(CharOperation.toStrings(this.rootReferences));
			throw new IllegalStateException(message);
		}
	}
	return result;
}

private static <T> boolean sortedArrayContains(T[] array, T element, Comparator<? super T> comparator) {
	int l = array.length;
	if (l < SortedCharArrays.BINARY_SEARCH_THRESHOLD) {
		for (int i = 0; i < l; i++)
			if (element == array[i]) return true;
		return false;
	}
	return Arrays.binarySearch(array, element, comparator) >= 0;
}

private boolean includesSimpleName(char[][] simpleNames) {
	return intersects(simpleNames, this.simpleNameReferences, SortedCharArrays.CHAR_ARR_COMPARATOR);
}

private boolean includesQualifiedName(char[][][] qualifiedNames) {
	if (intersects(qualifiedNames, this.qualifiedNameReferences, SortedCharArrays.CHAR_CHAR_ARR_COMPARATOR)) {
		return true;
	}
	char[][] maybeSimpleName;
	for(int i = qualifiedNames.length - 1; i >= 0 && (maybeSimpleName = qualifiedNames[i]).length == 1; i--) {
		if (includes(maybeSimpleName[0])) {
			return true;
		}
	}
	return false;
}

private boolean includesRootName(char[][] rootNames) {
	return intersects(rootNames, this.rootReferences, SortedCharArrays.CHAR_ARR_COMPARATOR);
}

private static <T> boolean intersects(T[] firstSortedArr, T[] secondSortedArr, Comparator<? super T> comparator) {
	/*
	 * Both arrays are sorted, so we can walk them in pairs.
	 * Using binary search for the remaining array elements to figure the next
	 * interesting index can greatly reduce the runtime cost for arrays that do
	 * have more than a few elements.
	 */
	for(int i = 0, l = firstSortedArr.length, j = 0, k = secondSortedArr.length; i < l && j < k;) {
		T firstElement = firstSortedArr[i];
		T secondElement = secondSortedArr[j];
		int compare = comparator.compare(firstElement, secondElement);
		if (compare == 0) {
			return true;
		} else if (compare < 0) {
			/*
			 * left side is smaller than the right side, but not exactly the right side.
			 * Take the next element from the left and proceed.
			 *
			 * If the number of remaining elements in the first array is sufficiently big,
			 * attempt a binary search for the second element to possibly skip a few elements.
			 */
			i++;
			if (l - i > SortedCharArrays.BINARY_SEARCH_THRESHOLD) {
				i = Arrays.binarySearch(firstSortedArr, i, l, secondElement, comparator);
				if (i >= 0) {
					return true;
				}
				i = -(i + 1);
			}
		} else {
			/*
			 * the inverse logic is applied here
			 */
			j++;
			if (k - j > SortedCharArrays.BINARY_SEARCH_THRESHOLD) {
				j = Arrays.binarySearch(secondSortedArr, j, k, firstElement, comparator);
				if (j >= 0) {
					return true;
				}
				j = -(j + 1);
			}
		}
	}
	return false;
}

private static char[][] ensureContainedInSortedOrder(char[][] sortedArray, char[] entry) {
	int idx = Arrays.binarySearch(sortedArray, entry, SortedCharArrays.CHAR_ARR_COMPARATOR);
	if (idx < 0) {
		idx = -(idx + 1);
		char[][] result = SortedCharArrays.insertIntoArray(sortedArray, new char[sortedArray.length + 1][], entry, idx, sortedArray.length);
		return result;
	}
	return sortedArray;
}

private static boolean isWellKnownQualifiedName(char[][] qualifiedName) {
	for (int i = 0, m = WellKnownQualifiedNames.length, qLength = qualifiedName.length; i < m; i++) {
		char[][] wellKnownName = WellKnownQualifiedNames[i];
		if (qLength > wellKnownName.length)
			break; // all remaining well known names are shorter
		if (CharOperation.equals(qualifiedName, wellKnownName)) {
			return true;
		}
	}
	return false;
}

// When any type is compiled, its methods are verified for certain problems
// the MethodVerifier requests 3 well known types which end up in the reference collection
// having WellKnownQualifiedNames & WellKnownSimpleNames, saves every type 40 bytes
// NOTE: These collections are sorted by length
static final char[][][] WellKnownQualifiedNames = new char[][][] {
	TypeConstants.JAVA_LANG_RUNTIMEEXCEPTION,
	TypeConstants.JAVA_LANG_THROWABLE,
	TypeConstants.JAVA_LANG_OBJECT,
	TypeConstants.JAVA_LANG,
	new char[][] {TypeConstants.JAVA},
	new char[][] {new char[] {'o', 'r', 'g'}},
	new char[][] {new char[] {'c', 'o', 'm'}},
	CharOperation.NO_CHAR_CHAR}; // default package
static final char[][] WellKnownSimpleNames = new char[][] {
	TypeConstants.JAVA_LANG_RUNTIMEEXCEPTION[2],
	TypeConstants.JAVA_LANG_THROWABLE[2],
	TypeConstants.JAVA_LANG_OBJECT[2],
	TypeConstants.JAVA,
	TypeConstants.LANG,
	new char[] {'o', 'r', 'g'},
	new char[] {'c', 'o', 'm'}};

static final char[][][] EmptyQualifiedNames = new char[0][][];
static final char[][] EmptySimpleNames = CharOperation.NO_CHAR_CHAR;

// each array contains qualified char[][], one for size 2, 3, 4, 5, 6, 7 & the rest
static final int MaxQualifiedNames = 7;
static QualifiedNameSet[] InternedQualifiedNames = new QualifiedNameSet[MaxQualifiedNames];
// each array contains simple char[], one for size 1 to 29 & the rest
static final int MaxSimpleNames = 30;
static NameSet[] InternedSimpleNames = new NameSet[MaxSimpleNames];
static {
	for (int i = 0; i < MaxQualifiedNames; i++)
		InternedQualifiedNames[i] = new QualifiedNameSet(37);
	for (int i = 0; i < MaxSimpleNames; i++)
		InternedSimpleNames[i] = new NameSet(37);
}

//TODO: remove once ReferenceCollection.internQualifiedNames(StringSet) is adapted to use java.util.Set, so that git history is preserved
public static char[][][] internQualifiedNames(Set<String> qualifiedStrings) {
	if (qualifiedStrings == null) return EmptyQualifiedNames;
	int length = qualifiedStrings.size();
	if (length == 0) return EmptyQualifiedNames;

	char[][][] result = new char[length][][];
	for (String qualifiedString : qualifiedStrings)
		if (qualifiedString != null)
			result[--length] = CharOperation.splitOn('/', qualifiedString.toCharArray());
	return internQualifiedNames(result, false);
}

//TODO: remove once PDE API Tools has been adapted to also use java.util.Set, so that git history is preserved
public static char[][][] internQualifiedNames(StringSet qualifiedStrings) {
	if (qualifiedStrings == null) return EmptyQualifiedNames;
	int length = qualifiedStrings.elementSize;
	if (length == 0) return EmptyQualifiedNames;

	char[][][] result = new char[length][][];
	String[] strings = qualifiedStrings.values;
	for (int i = 0, l = strings.length; i < l; i++)
		if (strings[i] != null)
			result[--length] = CharOperation.splitOn('/', strings[i].toCharArray());
	return internQualifiedNames(result, false);
}

/**
 * <strong>Note</strong>: this method may change order of the result data, the new array is always sorted.
 */
public static char[][][] internQualifiedNames(char[][][] qualifiedNames) {
	return internQualifiedNames(qualifiedNames, false);
}

/**
 * Use a flyweight cache for the char arrays to avoid duplicated arrays with the same contents.
 * After calling this method, identity comparison on the array contents of the resulting array
 * will work for arrays with equal content.
 * <p>
 * <strong>Note</strong>: this method may change order of the result data, the new array is always sorted.
 * <p>
 * Optionally drops very common qualified names from the array to spare some bytes.
 *
 * @return a new array with interned elements.
 */
public static char[][][] internQualifiedNames(char[][][] qualifiedNames, boolean keepWellKnown) {
	return internQualifiedNames(qualifiedNames, keepWellKnown, true);
}

static char[][][] internQualifiedNames(char[][][] qualifiedNames, boolean keepWellKnown, boolean doSort) {
	if (qualifiedNames == null) return EmptyQualifiedNames;
	int length = qualifiedNames.length;
	if (length == 0) return EmptyQualifiedNames;

	char[][][] keepers = new char[length][][];
	char[][] prev = null;
	boolean isSorted = true;
	int index = 0;

	next : for (int i = 0; i < length; i++) {
		char[][] qualifiedName = qualifiedNames[i];
		int qLength = qualifiedName.length;
		for (int j = 0, m = WellKnownQualifiedNames.length; j < m; j++) {
			char[][] wellKnownName = WellKnownQualifiedNames[j];
			if (qLength > wellKnownName.length)
				break; // all remaining well known names are shorter
			if (CharOperation.equals(qualifiedName, wellKnownName)) {
				if (keepWellKnown) {
					// This code is duplicated to encourage the JIT to inline more stuff
					if (doSort && isSorted) {
						if (prev != null && SortedCharArrays.compareCharCharArray(prev, qualifiedName) > 0) {
							isSorted = false;
						}
						prev = qualifiedName;
					}
					keepers[index++] = wellKnownName;
				}
				continue next;
			}
		}

		// InternedQualifiedNames[0] is for the rest (> 7 & 1)
		// InternedQualifiedNames[1] is for size 2...
		// InternedQualifiedNames[6] is for size 7
		QualifiedNameSet internedNames = InternedQualifiedNames[qLength <= MaxQualifiedNames ? qLength - 1 : 0];
		qualifiedName = internSimpleNames(qualifiedName, false, false);
		// This code is duplicated to encourage the JIT to inline more stuff
		if (doSort && isSorted) {
			if (prev != null && SortedCharArrays.compareCharCharArray(prev, qualifiedName) > 0) {
				isSorted = false;
			}
			prev = qualifiedName;
		}
		keepers[index++] = internedNames.add(qualifiedName);
	}
	if (length > index) {
		if (index == 0) return EmptyQualifiedNames;
		System.arraycopy(keepers, 0, keepers = new char[index][][], 0, index);
	}
	if (doSort && !isSorted) {
		Arrays.sort(keepers, SortedCharArrays.CHAR_CHAR_ARR_COMPARATOR);
	}
	return keepers;
}

/**
 * @deprecated
 */
public static char[][] internSimpleNames(Set<String> simpleStrings) {
	return internSimpleNames(simpleStrings, true);
}

// TODO: remove once ReferenceCollection.internSimpleNames(StringSet, boolean) is adapted to use java.util.Set, so that git history is preserved
public static char[][] internSimpleNames(Set<String> simpleStrings, boolean removeWellKnown) {
	if (simpleStrings == null) return EmptySimpleNames;
	int length = simpleStrings.size();
	if (length == 0) return EmptySimpleNames;

	char[][] result = new char[length][];
	for (String simpleString : simpleStrings)
		if (simpleString != null)
			result[--length] = simpleString.toCharArray();
	return internSimpleNames(result, removeWellKnown);
}

//TODO: adjust to use java.util.Set once PDE API Tools have been adapted to use the set version, so that git history is preserved
public static char[][] internSimpleNames(StringSet simpleStrings, boolean removeWellKnown) {
	if (simpleStrings == null) return EmptySimpleNames;
	int length = simpleStrings.elementSize;
	if (length == 0) return EmptySimpleNames;

	char[][] result = new char[length][];
	String[] strings = simpleStrings.values;
	for (int i = 0, l = strings.length; i < l; i++)
		if (strings[i] != null)
			result[--length] = strings[i].toCharArray();
	return internSimpleNames(result, removeWellKnown);
}
/**
 * Use a flyweight cache for the char arrays to avoid duplicated arrays with the same contents.
 * After calling this method, identity comparison on the array contents of the resulting array
 * will work for arrays with equal content.
 * <p>
 * <strong>Note</strong>: this method may change order of the result data, the new array is always sorted.
 * <p>
 * Optionally drops very common qualified names from the array to spare some bytes.
 *
 * @return a new array with interned elements.
 */
public static char[][] internSimpleNames(char[][] simpleNames, boolean removeWellKnown) {
	return internSimpleNames(simpleNames, removeWellKnown, true);
}
static char[][] internSimpleNames(char[][] simpleNames, boolean removeWellKnown, boolean doSort) {
	if (simpleNames == null) return EmptySimpleNames;
	int length = simpleNames.length;
	if (length == 0) return EmptySimpleNames;

	char[][] keepers = new char[length][];
	char[] prev = null;
	boolean isSorted = true;
	int index = 0;
	next : for (int i = 0; i < length; i++) {
		char[] name = simpleNames[i];
		int sLength = name.length;
		for (int j = 0, m = WellKnownSimpleNames.length; j < m; j++) {
			char[] wellKnownName = WellKnownSimpleNames[j];
			if (sLength > wellKnownName.length)
				break; // all remaining well known names are shorter
			if (CharOperation.equals(name, wellKnownName)) {
				if (!removeWellKnown) {
					keepers[index++] = wellKnownName;
					// This code is duplicated to encourage the JIT to inline more stuff
					if (doSort && isSorted) {
						if (prev != null && SortedCharArrays.compareCharArray(prev, name) > 0) {
							isSorted = false;
						}
						prev = name;
					}
				}
				continue next;
			}
		}

		// InternedSimpleNames[0] is for the rest (> 29)
		// InternedSimpleNames[1] is for size 1...
		// InternedSimpleNames[29] is for size 29
		NameSet internedNames = InternedSimpleNames[sLength < MaxSimpleNames ? sLength : 0];
		keepers[index++] = internedNames.add(name);
		// This code is duplicated to encourage the JIT to inline more stuff
		if (doSort && isSorted) {
			if (prev != null && SortedCharArrays.compareCharArray(prev, name) > 0) {
				isSorted = false;
			}
			prev = name;
		}
	}
	if (length > index) {
		if (index == 0) return EmptySimpleNames;
		System.arraycopy(keepers, 0, keepers = new char[index][], 0, index);
	}
	if (doSort && !isSorted) {
		Arrays.sort(keepers, SortedCharArrays.CHAR_ARR_COMPARATOR);
	}
	return keepers;
}

// DEBUG code below
public static boolean REFERENCE_COLLECTION_DEBUG = false;

private void assertIncludes(boolean expectation, char[] simpleName) {
	if (expectation != debugIncludes(simpleName)) {
		String message = "Mismatch: " + String.valueOf(simpleName) + (expectation ? " should not " : " should ") + " be included in "  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				+ Arrays.asList(CharOperation.toStrings(this.simpleNameReferences));
		throw new IllegalStateException(message);
	}
}

private void assertIncludes(boolean expectation, char[][] qualifiedName) {
	if (expectation != debugIncludes(qualifiedName)) {
		String message = "Mismatch: " + CharOperation.toString(qualifiedName) + (expectation ? " should not " : " should ") + " be included in "  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				+ qualifiedNamesToString(this.qualifiedNameReferences);
		throw new IllegalStateException(message);
	}
}

private void assertIncludes(boolean expectation, char[][][] qualifiedNames, char[][] simpleNames, char[][] rootNames) {
	if (expectation != debugIncludes(qualifiedNames, simpleNames, rootNames)) {
		String message = String.format("Mismatched includes(..): ReferenceCollection([%s], %s, %s).includes([%s], %s, %s)", //$NON-NLS-1$
				qualifiedNamesToString(this.qualifiedNameReferences),
				Arrays.toString(CharOperation.toStrings(this.simpleNameReferences)),
				Arrays.toString(CharOperation.toStrings(this.rootReferences)),
				qualifiedNamesToString(qualifiedNames),
				Arrays.toString(CharOperation.toStrings(simpleNames)),
				Arrays.toString(CharOperation.toStrings(rootNames))
		);
		throw new IllegalStateException(message);
	}
}

private boolean debugIncludes(char[][][] qualifiedNames, char[][] simpleNames, char[][] rootNames) {
	// if either collection of names is null, it means it contained a well known name so we know it already has a match
	if (rootNames != null) {
		boolean foundRoot = false;
		for (int i = 0, l = rootNames.length; !foundRoot && i < l; i++)
			foundRoot = debugInsideRoot(rootNames[i]);
		if (!foundRoot)
			return false;
	}
	if (simpleNames == null || qualifiedNames == null) {
		if (simpleNames == null && qualifiedNames == null) {
			if (JavaBuilder.DEBUG)
				System.out.println("Found well known match"); //$NON-NLS-1$
			return true;
		} else if (qualifiedNames == null) {
			for (int i = 0, l = simpleNames.length; i < l; i++) {
				if (debugIncludes(simpleNames[i])) {
					if (JavaBuilder.DEBUG)
						System.out.println("Found match in well known package to " + new String(simpleNames[i])); //$NON-NLS-1$
					return true;
				}
			}
		} else {
			for (int i = 0, l = qualifiedNames.length; i < l; i++) {
				char[][] qualifiedName = qualifiedNames[i];
				if (qualifiedName.length == 1 ? debugIncludes(qualifiedName[0]) : debugIncludes(qualifiedName)) {
					if (JavaBuilder.DEBUG)
						System.out.println("Found well known match in " + CharOperation.toString(qualifiedName)); //$NON-NLS-1$
					return true;
				}
			}
		}
		return false;
	}

	int sLength = simpleNames.length;
	int qLength = qualifiedNames.length;
	if (sLength <= qLength) {
		for (int i = 0; i < sLength; i++) {
			if (debugIncludes(simpleNames[i])) {
				for (int j = 0; j < qLength; j++) {
					char[][] qualifiedName = qualifiedNames[j];
					if (qualifiedName.length == 1 ? debugIncludes(qualifiedName[0]) : debugIncludes(qualifiedName)) {
						if (JavaBuilder.DEBUG)
							System.out.println("Found match in " + CharOperation.toString(qualifiedName) //$NON-NLS-1$
								+ " to " + new String(simpleNames[i])); //$NON-NLS-1$
						return true;
					}
				}
				return false;
			}
		}
	} else {
		for (int i = 0; i < qLength; i++) {
			char[][] qualifiedName = qualifiedNames[i];
			if (qualifiedName.length == 1 ? debugIncludes(qualifiedName[0]) : debugIncludes(qualifiedName)) {
				for (int j = 0; j < sLength; j++) {
					if (debugIncludes(simpleNames[j])) {
						if (JavaBuilder.DEBUG)
							System.out.println("Found match in " + CharOperation.toString(qualifiedName) //$NON-NLS-1$
								+ " to " + new String(simpleNames[j])); //$NON-NLS-1$
						return true;
					}
				}
				return false;
			}
		}
	}
	return false;
}

private boolean debugInsideRoot(char[] rootName) {
	for (int i = 0, l = this.rootReferences.length; i < l; i++)
		if (rootName == this.rootReferences[i]) return true;
	return false;
}

private boolean debugIncludes(char[] simpleName) {
	for (int i = 0, l = this.simpleNameReferences.length; i < l; i++)
		if (simpleName == this.simpleNameReferences[i]) return true;
	return false;
}

private boolean debugIncludes(char[][] qualifiedName) {
	for (int i = 0, l = this.qualifiedNameReferences.length; i < l; i++)
		if (qualifiedName == this.qualifiedNameReferences[i]) return true;
	return false;
}

@Override
public int hashCode() {
	return System.identityHashCode(this);
}

@Override
public boolean equals(Object obj) {
	if (this == obj) {
		return true;
	}
	if (!(obj instanceof ReferenceCollection)) {
		return false;
	}
	ReferenceCollection other = (ReferenceCollection) obj;
	return Arrays.deepEquals(this.qualifiedNameReferences, other.qualifiedNameReferences)
			&& Arrays.deepEquals(this.rootReferences, other.rootReferences)
			&& Arrays.deepEquals(this.simpleNameReferences, other.simpleNameReferences);
}

}
