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
 *     Stephan Herrmann - Contribution for
 *								Bug 458577 - IClassFile.getWorkingCopy() may lead to NPE in BecomeWorkingCopyOperation
 *******************************************************************************/
package org.eclipse.jdt.internal.core.util;

import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.WildcardType;
import org.eclipse.jdt.core.util.IClassFileAttribute;
import org.eclipse.jdt.core.util.IClassFileReader;
import org.eclipse.jdt.core.util.ICodeAttribute;
import org.eclipse.jdt.core.util.IComponentInfo;
import org.eclipse.jdt.core.util.IFieldInfo;
import org.eclipse.jdt.core.util.IMethodInfo;
import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.AnnotationMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Argument;
import org.eclipse.jdt.internal.compiler.ast.IntersectionCastTypeReference;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.eclipse.jdt.internal.compiler.ast.UnionTypeReference;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFormatException;
import org.eclipse.jdt.internal.compiler.env.ClassSignature;
import org.eclipse.jdt.internal.compiler.env.EnumConstantSignature;
import org.eclipse.jdt.internal.compiler.env.IBinaryAnnotation;
import org.eclipse.jdt.internal.compiler.env.IDependent;
import org.eclipse.jdt.internal.compiler.impl.Constant;
import org.eclipse.jdt.internal.compiler.lookup.Binding;
import org.eclipse.jdt.internal.compiler.lookup.FieldBinding;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.RecordComponentBinding;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeIds;
import org.eclipse.jdt.internal.compiler.parser.ScannerHelper;
import org.eclipse.jdt.internal.compiler.util.SuffixConstants;
import org.eclipse.jdt.internal.core.Annotation;
import org.eclipse.jdt.internal.core.ClassFile;
import org.eclipse.jdt.internal.core.JavaElement;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.jdt.internal.core.Member;
import org.eclipse.jdt.internal.core.MemberValuePair;
import org.eclipse.jdt.internal.core.PackageFragment;
import org.eclipse.jdt.internal.core.PackageFragmentRoot;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

/**
 * Provides convenient utility methods to other types in this package.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class Util {

	public interface Comparable {
		/**
		 * Returns 0 if this and c are equal, >0 if this is greater than c,
		 * or <0 if this is less than c.
		 */
		int compareTo(Comparable c);
	}

	public interface Comparer {
		/**
		 * Returns 0 if a and b are equal, >0 if a is greater than b,
		 * or <0 if a is less than b.
		 */
		int compare(Object a, Object b);
	}

	public static interface BindingsToNodesMap {
		public org.eclipse.jdt.internal.compiler.ast.ASTNode get(Binding binding);
	}

	private static final char ARGUMENTS_DELIMITER = '#';

	private static final String EMPTY_ARGUMENT = "   "; //$NON-NLS-1$

	private static char[][] JAVA_LIKE_EXTENSIONS;

	private static final char[] BOOLEAN = "boolean".toCharArray(); //$NON-NLS-1$
	private static final char[] BYTE = "byte".toCharArray(); //$NON-NLS-1$
	private static final char[] CHAR = "char".toCharArray(); //$NON-NLS-1$
	private static final char[] DOUBLE = "double".toCharArray(); //$NON-NLS-1$
	private static final char[] FLOAT = "float".toCharArray(); //$NON-NLS-1$
	private static final char[] INT = "int".toCharArray(); //$NON-NLS-1$
	private static final char[] LONG = "long".toCharArray(); //$NON-NLS-1$
	private static final char[] SHORT = "short".toCharArray(); //$NON-NLS-1$
	private static final char[] VOID = "void".toCharArray(); //$NON-NLS-1$
	private static final char[] INIT = "<init>".toCharArray(); //$NON-NLS-1$

	private static final String TASK_PRIORITIES_PROBLEM = "TASK_PRIORITIES_PB"; //$NON-NLS-1$
	private static List fgRepeatedMessages= new ArrayList(5);

	private Util() {
		// cannot be instantiated
	}

	/**
	 * Returns a new array adding the second array at the end of first array.
	 * It answers null if the first and second are null.
	 * If the first array is null or if it is empty, then a new array is created with second.
	 * If the second array is null, then the first array is returned.
	 * <br>
	 * <br>
	 * For example:
	 * <ol>
	 * <li><pre>
	 *    first = null
	 *    second = "a"
	 *    => result = {"a"}
	 * </pre>
	 * <li><pre>
	 *    first = {"a"}
	 *    second = null
	 *    => result = {"a"}
	 * </pre>
	 * </li>
	 * <li><pre>
	 *    first = {"a"}
	 *    second = {"b"}
	 *    => result = {"a", "b"}
	 * </pre>
	 * </li>
	 * </ol>
	 *
	 * @param first the first array to concatenate
	 * @param second the array to add at the end of the first array
	 * @return a new array adding the second array at the end of first array, or null if the two arrays are null.
	 */
	public static final String[] arrayConcat(String[] first, String second) {
		if (second == null)
			return first;
		if (first == null)
			return new String[] {second};

		int length = first.length;
		if (first.length == 0) {
			return new String[] {second};
		}

		String[] result = new String[length + 1];
		System.arraycopy(first, 0, result, 0, length);
		result[length] = second;
		return result;
	}

	/**
	 * Checks the type signature in String sig,
	 * starting at start and ending before end (end is not included).
	 * Returns the index of the character immediately after the signature if valid,
	 * or -1 if not valid.
	 */
	private static int checkTypeSignature(String sig, int start, int end, boolean allowVoid) {
		if (start >= end) return -1;
		int i = start;
		char c = sig.charAt(i++);
		int nestingDepth = 0;
		while (c == '[') {
			++nestingDepth;
			if (i >= end) return -1;
			c = sig.charAt(i++);
		}
		switch (c) {
			case 'B':
			case 'C':
			case 'D':
			case 'F':
			case 'I':
			case 'J':
			case 'S':
			case 'Z':
				break;
			case 'V':
				if (!allowVoid) return -1;
				// array of void is not allowed
				if (nestingDepth != 0) return -1;
				break;
			case 'L':
				int semicolon = sig.indexOf(';', i);
				// Must have at least one character between L and ;
				if (semicolon <= i || semicolon >= end) return -1;
				i = semicolon + 1;
				break;
			default:
				return -1;
		}
		return i;
	}

	/**
	 * Combines two hash codes to make a new one.
	 */
	public static int combineHashCodes(int hashCode1, int hashCode2) {
		return hashCode1 * 17 + hashCode2;
	}

	/**
	 * Compares two byte arrays.
	 * Returns <0 if a byte in a is less than the corresponding byte in b, or if a is shorter, or if a is null.
	 * Returns >0 if a byte in a is greater than the corresponding byte in b, or if a is longer, or if b is null.
	 * Returns 0 if they are equal or both null.
	 */
	public static int compare(byte[] a, byte[] b) {
		if (a == b)
			return 0;
		if (a == null)
			return -1;
		if (b == null)
			return 1;
		int len = Math.min(a.length, b.length);
		for (int i = 0; i < len; ++i) {
			int diff = a[i] - b[i];
			if (diff != 0)
				return diff;
		}
		if (a.length > len)
			return 1;
		if (b.length > len)
			return -1;
		return 0;
	}
	/**
	 * Compares two strings lexicographically.
	 * The comparison is based on the Unicode value of each character in
	 * the strings.
	 *
	 * @return  the value <code>0</code> if the str1 is equal to str2;
	 *          a value less than <code>0</code> if str1
	 *          is lexicographically less than str2;
	 *          and a value greater than <code>0</code> if str1 is
	 *          lexicographically greater than str2.
	 */
	public static int compare(char[] str1, char[] str2) {
		int len1= str1.length;
		int len2= str2.length;
		int n= Math.min(len1, len2);
		int i= 0;
		while (n-- != 0) {
			char c1= str1[i];
			char c2= str2[i++];
			if (c1 != c2) {
				return c1 - c2;
			}
		}
		return len1 - len2;
	}
	/**
	 * Concatenate a String[] compound name to a continuous char[].
	 */
	public static char[] concatCompoundNameToCharArray(String[] compoundName) {
		if (compoundName == null) return null;
		int length = compoundName.length;
		if (length == 0) return new char[0];
		int size = 0;
		for (int i=0; i<length; i++) {
			size += compoundName[i].length();
		}
		char[] compoundChars = new char[size+length-1];
		int pos = 0;
		for (int i=0; i<length; i++) {
			String name = compoundName[i];
			if (i > 0) compoundChars[pos++] = '.';
			int nameLength = name.length();
			name.getChars(0, nameLength, compoundChars, pos);
			pos += nameLength;
		}
		return compoundChars;
	}
	public static String concatenateName(String name1, String name2, char separator) {
		StringBuilder buf= new StringBuilder();
		if (name1 != null && name1.length() > 0) {
			buf.append(name1);
		}
		if (name2 != null && name2.length() > 0) {
			if (buf.length() > 0) {
				buf.append(separator);
			}
			buf.append(name2);
		}
		return buf.toString();
	}
	/**
	 * Returns the concatenation of the given array parts using the given separator between each part.
	 * <br>
	 * <br>
	 * For example:<br>
	 * <ol>
	 * <li><pre>
	 *    array = {"a", "b"}
	 *    separator = '.'
	 *    => result = "a.b"
	 * </pre>
	 * </li>
	 * <li><pre>
	 *    array = {}
	 *    separator = '.'
	 *    => result = ""
	 * </pre></li>
	 * </ol>
	 *
	 * @param array the given array
	 * @param separator the given separator
	 * @return the concatenation of the given array parts using the given separator between each part
	 */
	public static final String concatWith(String[] array, char separator) {
		StringBuilder buffer = new StringBuilder();
		for (int i = 0, length = array.length; i < length; i++) {
			buffer.append(array[i]);
			if (i < length - 1)
				buffer.append(separator);
		}
		return buffer.toString();
	}

	/**
	 * Returns the concatenation of the given array parts using the given separator between each
	 * part and appending the given name at the end.
	 * <br>
	 * <br>
	 * For example:<br>
	 * <ol>
	 * <li><pre>
	 *    name = "c"
	 *    array = { "a", "b" }
	 *    separator = '.'
	 *    => result = "a.b.c"
	 * </pre>
	 * </li>
	 * <li><pre>
	 *    name = null
	 *    array = { "a", "b" }
	 *    separator = '.'
	 *    => result = "a.b"
	 * </pre></li>
	 * <li><pre>
	 *    name = " c"
	 *    array = null
	 *    separator = '.'
	 *    => result = "c"
	 * </pre></li>
	 * </ol>
	 *
	 * @param array the given array
	 * @param name the given name
	 * @param separator the given separator
	 * @return the concatenation of the given array parts using the given separator between each
	 * part and appending the given name at the end
	 */
	public static final String concatWith(
		String[] array,
		String name,
		char separator) {

		if (array == null || array.length == 0) return name;
		if (name == null || name.length() == 0) return concatWith(array, separator);
		StringBuilder buffer = new StringBuilder();
		for (int i = 0, length = array.length; i < length; i++) {
			buffer.append(array[i]);
			buffer.append(separator);
		}
		buffer.append(name);
		return buffer.toString();

	}

	public static final char[] concat(char[] first, char[] second) {
		if (first == null)
			return second;
		if (second == null)
			return first;

		int length1 = first.length;
		int length2 = second.length;
		char[] result = new char[length1 + length2];
		System.arraycopy(first, 0, result, 0, length1);
		System.arraycopy(second, 0, result, length1, length2);
		return result;
	}

	/**
	 * Converts a type signature from the IBinaryType representation to the DC representation.
	 */
	public static String convertTypeSignature(char[] sig, int start, int length) {
		return new String(sig, start, length).replace('/', '.');
	}

	/*
	 * Returns the default java extension (".java").
	 * To be used when the extension is not known.
	 */
	public static String defaultJavaExtension() {
		return SuffixConstants.SUFFIX_STRING_java;
	}

	/**
	 * Apply the given edit on the given string and return the updated string.
	 * Return the given string if anything wrong happen while applying the edit.
	 *
	 * @param original the given string
	 * @param edit the given edit
	 *
	 * @return the updated string
	 */
	public final static String editedString(String original, TextEdit edit) {
		if (edit == null) {
			return original;
		}
		SimpleDocument document = new SimpleDocument(original);
		try {
			edit.apply(document, TextEdit.NONE);
			return document.get();
		} catch (MalformedTreeException | BadLocationException e) {
			e.printStackTrace();
		}
		return original;
	}

	/**
	 * Returns true iff str.toLowerCase().endsWith(end.toLowerCase())
	 * implementation is not creating extra strings.
	 */
	public final static boolean endsWithIgnoreCase(String str, String end) {

		int strLength = str == null ? 0 : str.length();
		int endLength = end == null ? 0 : end.length();

		// return false if the string is smaller than the end.
		if(endLength > strLength)
			return false;

		// return false if any character of the end are
		// not the same in lower case.
		for(int i = 1 ; i <= endLength; i++){
			if(ScannerHelper.toLowerCase(end.charAt(endLength - i)) != ScannerHelper.toLowerCase(str.charAt(strLength - i)))
				return false;
		}

		return true;
	}

	/**
	 * Compares two arrays using equals() on the elements.
	 * Neither can be null. Only the first len elements are compared.
	 * Return false if either array is shorter than len.
	 */
	public static boolean equalArrays(Object[] a, Object[] b, int len) {
		if (a == b)	return true;
		if (a.length < len || b.length < len) return false;
		for (int i = 0; i < len; ++i) {
			if (a[i] == null) {
				if (b[i] != null) return false;
			} else {
				if (!a[i].equals(b[i])) return false;
			}
		}
		return true;
	}

	/**
	 * Compares two arrays using equals() on the elements.
	 * Either or both arrays may be null.
	 * Returns true if both are null.
	 * Returns false if only one is null.
	 * If both are arrays, returns true iff they have the same length and
	 * all elements are equal.
	 */
	public static boolean equalArraysOrNull(int[] a, int[] b) {
		if (a == b)
			return true;
		if (a == null || b == null)
			return false;
		int len = a.length;
		if (len != b.length)
			return false;
		for (int i = 0; i < len; ++i) {
			if (a[i] != b[i])
				return false;
		}
		return true;
	}

	/**
	 * Compares two arrays using equals() on the elements.
	 * Either or both arrays may be null.
	 * Returns true if both are null.
	 * Returns false if only one is null.
	 * If both are arrays, returns true iff they have the same length and
	 * all elements compare true with equals.
	 */
	public static boolean equalArraysOrNull(Object[] a, Object[] b) {
		if (a == b)	return true;
		if (a == null || b == null) return false;

		int len = a.length;
		if (len != b.length) return false;
		// walk array from end to beginning as this optimizes package name cases
		// where the first part is always the same (e.g. org.eclipse.jdt)
		for (int i = len-1; i >= 0; i--) {
			if (a[i] == null) {
				if (b[i] != null) return false;
			} else {
				if (!a[i].equals(b[i])) return false;
			}
		}
		return true;
	}

	/**
	 * Compares two arrays using equals() on the elements.
	 * The arrays are first sorted.
	 * Either or both arrays may be null.
	 * Returns true if both are null.
	 * Returns false if only one is null.
	 * If both are arrays, returns true iff they have the same length and
	 * iff, after sorting both arrays, all elements compare true with equals.
	 * The original arrays are left untouched.
	 */
	public static boolean equalArraysOrNullSortFirst(Comparable[] a, Comparable[] b) {
		if (a == b)	return true;
		if (a == null || b == null) return false;
		int len = a.length;
		if (len != b.length) return false;
		if (len >= 2) {  // only need to sort if more than two items
			a = sortCopy(a);
			b = sortCopy(b);
		}
		for (int i = 0; i < len; ++i) {
			if (!a[i].equals(b[i])) return false;
		}
		return true;
	}

	/**
	 * Compares two String arrays using equals() on the elements.
	 * The arrays are first sorted.
	 * Either or both arrays may be null.
	 * Returns true if both are null.
	 * Returns false if only one is null.
	 * If both are arrays, returns true iff they have the same length and
	 * iff, after sorting both arrays, all elements compare true with equals.
	 * The original arrays are left untouched.
	 */
	public static boolean equalArraysOrNullSortFirst(String[] a, String[] b) {
		if (a == b)	return true;
		if (a == null || b == null) return false;
		int len = a.length;
		if (len != b.length) return false;
		if (len >= 2) {  // only need to sort if more than two items
			a = sortCopy(a);
			b = sortCopy(b);
		}
		for (int i = 0; i < len; ++i) {
			if (!a[i].equals(b[i])) return false;
		}
		return true;
	}

	/**
	 * Compares two objects using equals().
	 * Either or both array may be null.
	 * Returns true if both are null.
	 * Returns false if only one is null.
	 * Otherwise, return the result of comparing with equals().
	 */
	public static boolean equalOrNull(Object a, Object b) {
		if (a == b) {
			return true;
		}
		if (a == null || b == null) {
			return false;
		}
		return a.equals(b);
	}

	/*
	 * Returns whether the given file name equals to the given string ignoring the java like extension
	 * of the file name.
	 * Returns false if it is not a java like file name.
	 */
	public static boolean equalsIgnoreJavaLikeExtension(String fileName, String string) {
		int fileNameLength = fileName.length();
		int stringLength = string.length();
		if (fileNameLength < stringLength) return false;
		for (int i = 0; i < stringLength; i ++) {
			if (fileName.charAt(i) != string.charAt(i)) {
				return false;
			}
		}
		char[][] javaLikeExtensions = getJavaLikeExtensions();
		suffixes: for (int i = 0, length = javaLikeExtensions.length; i < length; i++) {
			char[] suffix = javaLikeExtensions[i];
			int extensionStart = stringLength+1;
			if (extensionStart + suffix.length != fileNameLength) continue;
			if (fileName.charAt(stringLength) != '.') continue;
			for (int j = extensionStart; j < fileNameLength; j++) {
				if (fileName.charAt(j) != suffix[j-extensionStart])
					continue suffixes;
			}
			return true;
		}
		return false;
	}

	/**
	 * Given a qualified name, extract the last component.
	 * If the input is not qualified, the same string is answered.
	 */
	public static String extractLastName(String qualifiedName) {
		int i = qualifiedName.lastIndexOf('.');
		if (i == -1) return qualifiedName;
		return qualifiedName.substring(i+1);
	}

	/**
	 * Extracts the parameter types from a method signature.
	 */
	public static String[] extractParameterTypes(char[] sig) {
		int count = getParameterCount(sig);
		String[] result = new String[count];
		if (count == 0)
			return result;
		int i = CharOperation.indexOf('(', sig) + 1;
		count = 0;
		int len = sig.length;
		int start = i;
		for (;;) {
			if (i == len)
				break;
			char c = sig[i];
			if (c == ')')
				break;
			if (c == '[') {
				++i;
			} else
				if (c == 'L') {
					i = CharOperation.indexOf(';', sig, i + 1) + 1;
					Assert.isTrue(i != 0);
					result[count++] = convertTypeSignature(sig, start, i - start);
					start = i;
				} else {
					++i;
					result[count++] = convertTypeSignature(sig, start, i - start);
					start = i;
				}
		}
		return result;
	}

	/**
	 * Extracts the return type from a method signature.
	 */
	public static String extractReturnType(String sig) {
		int i = sig.lastIndexOf(')');
		Assert.isTrue(i != -1);
		return sig.substring(i+1);
	}
	private static IFile findFirstClassFile(IFolder folder) {
		try {
			IResource[] members = folder.members();
			for (int i = 0, max = members.length; i < max; i++) {
				IResource member = members[i];
				if (member.getType() == IResource.FOLDER) {
					return findFirstClassFile((IFolder)member);
				} else if (org.eclipse.jdt.internal.compiler.util.Util.isClassFileName(member.getName())) {
					return (IFile) member;
				}
			}
		} catch (CoreException e) {
			// ignore
		}
		return null;
	}

	/**
	 * Finds the first line separator used by the given text.
	 *
	 * @return </code>"\n"</code> or </code>"\r"</code> or  </code>"\r\n"</code>,
	 *			or <code>null</code> if none found
	 */
	public static String findLineSeparator(char[] text) {
		// find the first line separator
		int length = text.length;
		if (length > 0) {
			char nextChar = text[0];
			for (int i = 0; i < length; i++) {
				char currentChar = nextChar;
				nextChar = i < length-1 ? text[i+1] : ' ';
				switch (currentChar) {
					case '\n': return "\n"; //$NON-NLS-1$
					case '\r': return nextChar == '\n' ? "\r\n" : "\r"; //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
		}
		// not found
		return null;
	}

	public static IClassFileAttribute getAttribute(IClassFileReader classFileReader, char[] attributeName) {
		IClassFileAttribute[] attributes = classFileReader.getAttributes();
		for (int i = 0, max = attributes.length; i < max; i++) {
			if (CharOperation.equals(attributes[i].getAttributeName(), attributeName)) {
				return attributes[i];
			}
		}
		return null;
	}

	public static IClassFileAttribute getAttribute(ICodeAttribute codeAttribute, char[] attributeName) {
		IClassFileAttribute[] attributes = codeAttribute.getAttributes();
		for (int i = 0, max = attributes.length; i < max; i++) {
			if (CharOperation.equals(attributes[i].getAttributeName(), attributeName)) {
				return attributes[i];
			}
		}
		return null;
	}

	public static IClassFileAttribute getAttribute(IFieldInfo fieldInfo, char[] attributeName) {
		IClassFileAttribute[] attributes = fieldInfo.getAttributes();
		for (int i = 0, max = attributes.length; i < max; i++) {
			if (CharOperation.equals(attributes[i].getAttributeName(), attributeName)) {
				return attributes[i];
			}
		}
		return null;
	}
	public static IClassFileAttribute getAttribute(IComponentInfo componentInfo, char[] attributeName) {
		IClassFileAttribute[] attributes = componentInfo.getAttributes();
		for (int i = 0, max = attributes.length; i < max; i++) {
			if (CharOperation.equals(attributes[i].getAttributeName(), attributeName)) {
				return attributes[i];
			}
		}
		return null;
	}

	public static IClassFileAttribute getAttribute(IMethodInfo methodInfo, char[] attributeName) {
		IClassFileAttribute[] attributes = methodInfo.getAttributes();
		for (int i = 0, max = attributes.length; i < max; i++) {
			if (CharOperation.equals(attributes[i].getAttributeName(), attributeName)) {
				return attributes[i];
			}
		}
		return null;
	}

	private static IClassFile getClassFile(char[] fileName) {
		int jarSeparator = CharOperation.indexOf(IDependent.JAR_FILE_ENTRY_SEPARATOR, fileName);
		int pkgEnd = CharOperation.lastIndexOf('/', fileName); // pkgEnd is exclusive
		if (pkgEnd == -1)
			pkgEnd = CharOperation.lastIndexOf(File.separatorChar, fileName);
		if (jarSeparator != -1 && pkgEnd < jarSeparator) // if in a jar and no slash, it is a default package -> pkgEnd should be equal to jarSeparator
			pkgEnd = jarSeparator;
		if (pkgEnd == -1)
			return null;
		IPackageFragment pkg = getPackageFragment(fileName, pkgEnd, jarSeparator);
		if (pkg == null) return null;
		int start;
		return pkg.getClassFile(new String(fileName, start = pkgEnd + 1, fileName.length - start));
	}

	private static ICompilationUnit getCompilationUnit(char[] fileName, WorkingCopyOwner workingCopyOwner) {
		char[] slashSeparatedFileName = CharOperation.replaceOnCopy(fileName, File.separatorChar, '/');
		int pkgEnd = CharOperation.lastIndexOf('/', slashSeparatedFileName); // pkgEnd is exclusive
		if (pkgEnd == -1)
			return null;
		IPackageFragment pkg = getPackageFragment(slashSeparatedFileName, pkgEnd, -1/*no jar separator for .java files*/);
		if (pkg != null) {
			int start;
			ICompilationUnit cu = pkg.getCompilationUnit(new String(slashSeparatedFileName, start =  pkgEnd+1, slashSeparatedFileName.length - start));
			if (workingCopyOwner != null) {
				ICompilationUnit workingCopy = cu.findWorkingCopy(workingCopyOwner);
				if (workingCopy != null)
					return workingCopy;
			}
			return cu;
		}
		IWorkspaceRoot wsRoot = ResourcesPlugin.getWorkspace().getRoot();
		IFile file = wsRoot.getFile(new Path(String.valueOf(fileName)));
		if (file.exists()) {
			// this approach works if file exists but is not on the project's build path:
			return JavaCore.createCompilationUnitFrom(file);
		}
		return null;
	}

	/**
	 * Returns the registered Java like extensions.
	 */
	public static char[][] getJavaLikeExtensions() {
		if (JAVA_LIKE_EXTENSIONS == null) {
			IContentType javaContentType = Platform.getContentTypeManager().getContentType(JavaCore.JAVA_SOURCE_CONTENT_TYPE);
			HashSet fileExtensions = new HashSet();
			// content types derived from java content type should be included (https://bugs.eclipse.org/bugs/show_bug.cgi?id=121715)
			IContentType[] contentTypes = Platform.getContentTypeManager().getAllContentTypes();
			for (int i = 0, length = contentTypes.length; i < length; i++) {
				if (contentTypes[i].isKindOf(javaContentType)) { // note that javaContentType.isKindOf(javaContentType) == true
					String[] fileExtension = contentTypes[i].getFileSpecs(IContentType.FILE_EXTENSION_SPEC);
					for (int j = 0, length2 = fileExtension.length; j < length2; j++) {
						fileExtensions.add(fileExtension[j]);
					}
				}
			}
			int length = fileExtensions.size();
			// note that file extensions contains "java" as it is defined in JDT Core's plugin.xml
			char[][] extensions = new char[length][];
			extensions[0] = SuffixConstants.EXTENSION_java.toCharArray(); // ensure that "java" is first
			int index = 1;
			Iterator iterator = fileExtensions.iterator();
			while (iterator.hasNext()) {
				String fileExtension = (String) iterator.next();
				if (SuffixConstants.EXTENSION_java.equals(fileExtension))
					continue;
				extensions[index++] = fileExtension.toCharArray();
			}
			JAVA_LIKE_EXTENSIONS = extensions;
		}
		return JAVA_LIKE_EXTENSIONS;
	}
	/**
	 * Get the jdk level of this root.
	 * The value can be:
	 * <ul>
	 * <li>major<<16 + minor : see predefined constants on ClassFileConstants </li>
	 * <li><code>0</null> if the root is a source package fragment root or if a Java model exception occured</li>
	 * </ul>
	 * Returns the jdk level
	 */
	public static long getJdkLevel(Object targetLibrary) {
		try {
			ClassFileReader reader = null;
			if (targetLibrary instanceof IFolder) {
				IFile classFile = findFirstClassFile((IFolder) targetLibrary); // only internal classfolders are allowed
				if (classFile != null)
					reader = Util.newClassFileReader(classFile);
			} else {
				// root is a jar file or a zip file
				ZipFile jar = null;
				try {
					IPath path = null;
					if (targetLibrary instanceof IResource) {
						path = ((IResource)targetLibrary).getFullPath();
					} else if (targetLibrary instanceof File){
						File f = (File) targetLibrary;
						if (!f.isDirectory()) {
							path = new Path(((File)targetLibrary).getPath());
						}
					}
					if (path != null) {
						if (JavaModelManager.isJrt(path)) {
							return ClassFileConstants.JDK9;
						} else {
							jar = JavaModelManager.getJavaModelManager().getZipFile(path);
							for (Enumeration e= jar.entries(); e.hasMoreElements();) {
								ZipEntry member= (ZipEntry) e.nextElement();
								String entryName= member.getName();
								if (org.eclipse.jdt.internal.compiler.util.Util.isClassFileName(entryName)) {
									reader = ClassFileReader.read(jar, entryName);
									break;
								}
							}
						}
					}
				} catch (CoreException e) {
					// ignore
				} finally {
					JavaModelManager.getJavaModelManager().closeZipFile(jar);
				}
			}
			if (reader != null) {
				return reader.getVersion();
			}
		} catch(CoreException | ClassFormatException | IOException e) {
			// ignore
		}
		return 0;
	}

	/**
	 * Returns the substring of the given file name, ending at the start of a
	 * Java like extension. The entire file name is returned if it doesn't end
	 * with a Java like extension.
	 */
	public static String getNameWithoutJavaLikeExtension(String fileName) {
		int index = indexOfJavaLikeExtension(fileName);
		if (index == -1)
			return fileName;
		return fileName.substring(0, index);
	}

	/**
	 * Returns the line separator found in the given text.
	 * If it is null, or not found return the line delimiter for the given project.
	 * If the project is null, returns the line separator for the workspace.
	 * If still null, return the system line separator.
	 */
	public static String getLineSeparator(String text, IJavaProject project) {
		String lineSeparator = null;

		// line delimiter in given text
		if (text != null && text.length() != 0) {
			lineSeparator = findLineSeparator(text.toCharArray());
			if (lineSeparator != null)
				return lineSeparator;
		}

		if (Platform.isRunning()) {
			// line delimiter in project preference
			IScopeContext[] scopeContext;
			if (project != null) {
				scopeContext= new IScopeContext[] { new ProjectScope(project.getProject()) };
				lineSeparator= Platform.getPreferencesService().getString(Platform.PI_RUNTIME, Platform.PREF_LINE_SEPARATOR, null, scopeContext);
				if (lineSeparator != null)
					return lineSeparator;
			}

			// line delimiter in workspace preference
			scopeContext= new IScopeContext[] { InstanceScope.INSTANCE };
			lineSeparator = Platform.getPreferencesService().getString(Platform.PI_RUNTIME, Platform.PREF_LINE_SEPARATOR, null, scopeContext);
			if (lineSeparator != null)
				return lineSeparator;
		}

		// system line delimiter
		return org.eclipse.jdt.internal.compiler.util.Util.LINE_SEPARATOR;
	}

	/**
	 * Returns the line separator used by the given buffer.
	 * Uses the given text if none found.
	 *
	 * @return </code>"\n"</code> or </code>"\r"</code> or  </code>"\r\n"</code>
	 */
	private static String getLineSeparator(char[] text, char[] buffer) {
		// search in this buffer's contents first
		String lineSeparator = findLineSeparator(buffer);
		if (lineSeparator == null) {
			// search in the given text
			lineSeparator = findLineSeparator(text);
			if (lineSeparator == null) {
				// default to system line separator
				return getLineSeparator((String) null, (IJavaProject) null);
			}
		}
		return lineSeparator;
	}

	public static IPackageFragment getPackageFragment(char[] fileName, int pkgEnd, int jarSeparator) {
		if (jarSeparator != -1) {
			String jarMemento = new String(fileName, 0, jarSeparator);
			PackageFragmentRoot root = (PackageFragmentRoot) JavaCore.create(jarMemento);
			if (pkgEnd == jarSeparator)
				return root.getPackageFragment(CharOperation.NO_STRINGS);
			char[] pkgName = CharOperation.subarray(fileName, jarSeparator+1, pkgEnd);
			char[][] compoundName = CharOperation.splitOn('/', pkgName);
			return root.getPackageFragment(CharOperation.toStrings(compoundName));
		} else {
			Path path = new Path(new String(fileName, 0, pkgEnd));
			IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
			IContainer folder = path.segmentCount() == 1 ? workspaceRoot.getProject(path.lastSegment()) : (IContainer) workspaceRoot.getFolder(path);
			IJavaElement element = JavaCore.create(folder);
			if (element == null) return null;
			switch (element.getElementType()) {
				case IJavaElement.PACKAGE_FRAGMENT:
					return (IPackageFragment) element;
				case IJavaElement.PACKAGE_FRAGMENT_ROOT:
					return ((PackageFragmentRoot) element).getPackageFragment(CharOperation.NO_STRINGS);
				case IJavaElement.JAVA_PROJECT:
					PackageFragmentRoot root = (PackageFragmentRoot) ((IJavaProject) element).getPackageFragmentRoot(folder);
					if (root == null) return null;
					return root.getPackageFragment(CharOperation.NO_STRINGS);
			}
			return null;
		}
	}

	/**
	 * Returns the number of parameter types in a method signature.
	 */
	public static int getParameterCount(char[] sig) {
		int i = CharOperation.indexOf('(', sig) + 1;
		Assert.isTrue(i != 0);
		int count = 0;
		int len = sig.length;
		for (;;) {
			if (i == len)
				break;
			char c = sig[i];
			if (c == ')')
				break;
			if (c == '[') {
				++i;
			} else
				if (c == 'L') {
					++count;
					i = CharOperation.indexOf(';', sig, i + 1) + 1;
					Assert.isTrue(i != 0);
				} else {
					++count;
					++i;
				}
		}
		return count;
	}

	/**
	 * Put all the arguments in one String.
	 */
	public static String getProblemArgumentsForMarker(String[] arguments){
		StringBuffer args = new StringBuffer(10);

		args.append(arguments.length);
		args.append(':');


		for (int j = 0; j < arguments.length; j++) {
			if(j != 0)
				args.append(ARGUMENTS_DELIMITER);

			if(arguments[j].length() == 0) {
				args.append(EMPTY_ARGUMENT);
			} else {
				encodeArgument(arguments[j], args);
			}
		}

		return args.toString();
	}

	/**
	 * Encode the argument by doubling the '#' if present into the argument value.
	 *
	 * <p>This stores the encoded argument into the given buffer.</p>
	 *
	 * @param argument the given argument
	 * @param buffer the buffer in which the encoded argument is stored
	 */
	private static void encodeArgument(String argument, StringBuffer buffer) {
		for (int i = 0, max = argument.length(); i < max; i++) {
			char charAt = argument.charAt(i);
			switch(charAt) {
				case ARGUMENTS_DELIMITER :
					buffer.append(ARGUMENTS_DELIMITER).append(ARGUMENTS_DELIMITER);
					break;
				default:
					buffer.append(charAt);
			}
		}
	}

	/**
	 * Separate all the arguments of a String made by getProblemArgumentsForMarker
	 */
	public static String[] getProblemArgumentsFromMarker(String argumentsString){
		if (argumentsString == null) {
			return null;
		}
		int index = argumentsString.indexOf(':');
		if(index == -1)
			return null;

		int length = argumentsString.length();
		int numberOfArg = 0;
		try{
			numberOfArg = Integer.parseInt(argumentsString.substring(0 , index));
		} catch (NumberFormatException e) {
			return null;
		}
		argumentsString = argumentsString.substring(index + 1, length);

		return decodeArgumentString(numberOfArg, argumentsString);
	}

	private static String[] decodeArgumentString(int length, String argumentsString) {
		// decode the argumentString knowing that '#' is doubled if part of the argument value
		if (length == 0) {
			if (argumentsString.length() != 0) {
				return null;
			}
			return CharOperation.NO_STRINGS;
		}
		String[] result = new String[length];
		int count = 0;
		StringBuffer buffer = new StringBuffer();
		for (int i = 0, max = argumentsString.length(); i < max; i++) {
			char current = argumentsString.charAt(i);
			switch(current) {
				case ARGUMENTS_DELIMITER :
					/* check the next character. If this is also ARGUMENTS_DELIMITER then only put one into the
					 * decoded argument and proceed with the next character
					 */
					if ((i + 1) == max) {
						return null;
					}
					char next = argumentsString.charAt(i + 1);
					if (next == ARGUMENTS_DELIMITER) {
						buffer.append(ARGUMENTS_DELIMITER);
						i++; // proceed with the next character
					} else {
						// this means the current argument is over
						String currentArgumentContents = String.valueOf(buffer);
						if (EMPTY_ARGUMENT.equals(currentArgumentContents)) {
							currentArgumentContents = org.eclipse.jdt.internal.compiler.util.Util.EMPTY_STRING;
						}
						result[count++] = currentArgumentContents;
						if (count > length) {
							// too many elements - ill-formed
							return null;
						}
						buffer.delete(0, buffer.length());
					}
					break;
				default :
					buffer.append(current);
			}
		}
		// process last argument
		String currentArgumentContents = String.valueOf(buffer);
		if (EMPTY_ARGUMENT.equals(currentArgumentContents)) {
			currentArgumentContents = org.eclipse.jdt.internal.compiler.util.Util.EMPTY_STRING;
		}
		result[count++] = currentArgumentContents;
		if (count > length) {
			// too many elements - ill-formed
			return null;
		}
		buffer.delete(0, buffer.length());
		return result;
	}

	/**
	 * Returns the given file's contents as a byte array.
	 */
	public static byte[] getResourceContentsAsByteArray(IFile file) throws JavaModelException {
		InputStream stream= null;
		try {
			stream = file.getContents(true);
		} catch (CoreException e) {
			throw new JavaModelException(e);
		}
		try {
			return org.eclipse.jdt.internal.compiler.util.Util.getInputStreamAsByteArray(stream);
		} catch (IOException e) {
			throw new JavaModelException(e, IJavaModelStatusConstants.IO_EXCEPTION);
		} finally {
			try {
				stream.close();
			} catch (IOException e) {
				// ignore
			}
		}
	}

	/**
	 * Returns the given file's contents as a character array.
	 */
	public static char[] getResourceContentsAsCharArray(IFile file) throws JavaModelException {
		// Get encoding from file
		String encoding;
		try {
			encoding = file.getCharset();
		} catch(CoreException ce) {
			// do not use any encoding
			encoding = null;
		}
		return getResourceContentsAsCharArray(file, encoding);
	}

	public static char[] getResourceContentsAsCharArray(IFile file, String encoding) throws JavaModelException {
		// Get resource contents
		InputStream stream= null;
		try {
			stream = file.getContents(true);
		} catch (CoreException e) {
			throw new JavaModelException(e, IJavaModelStatusConstants.ELEMENT_DOES_NOT_EXIST);
		}
		try {
			return org.eclipse.jdt.internal.compiler.util.Util.getInputStreamAsCharArray(stream, encoding);
		} catch (IOException e) {
			throw new JavaModelException(e, IJavaModelStatusConstants.IO_EXCEPTION);
		} finally {
			try {
				stream.close();
			} catch (IOException e) {
				// ignore
			}
		}
	}

	/*
	 * Returns the signature of the given type.
	 */
	public static String getSignature(Type type) {
		StringBuffer buffer = new StringBuffer();
		getFullyQualifiedName(type, buffer);
		return Signature.createTypeSignature(buffer.toString(), false/*not resolved in source*/);
	}

	/*
	 * Returns the source attachment property for this package fragment root's path
	 */
	public static String getSourceAttachmentProperty(IPath path) throws JavaModelException {
		Map rootPathToAttachments = JavaModelManager.getJavaModelManager().rootPathToAttachments;
		String property = (String) rootPathToAttachments.get(path);
		if (property == null) {
			try {
				property = ResourcesPlugin.getWorkspace().getRoot().getPersistentProperty(getSourceAttachmentPropertyName(path));
				if (property == null) {
					rootPathToAttachments.put(path, PackageFragmentRoot.NO_SOURCE_ATTACHMENT);
					return null;
				}
				rootPathToAttachments.put(path, property);
				return property;
			} catch (CoreException e)  {
				throw new JavaModelException(e);
			}
		} else if (property.equals(PackageFragmentRoot.NO_SOURCE_ATTACHMENT)) {
			return null;
		} else
			return property;
	}

	private static QualifiedName getSourceAttachmentPropertyName(IPath path) {
		return new QualifiedName(JavaCore.PLUGIN_ID, "sourceattachment: " + path.toOSString()); //$NON-NLS-1$
	}

	public static void setSourceAttachmentProperty(IPath path, String property) {
		if (property == null) {
			JavaModelManager.getJavaModelManager().rootPathToAttachments.put(path, PackageFragmentRoot.NO_SOURCE_ATTACHMENT);
		} else {
			JavaModelManager.getJavaModelManager().rootPathToAttachments.put(path, property);
		}
		try {
			ResourcesPlugin.getWorkspace().getRoot().setPersistentProperty(getSourceAttachmentPropertyName(path), property);
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	/*
	 * Returns the declaring type signature of the element represented by the given binding key.
	 * Returns the signature of the element if it is a type.
	 *
	 * @return the declaring type signature
	 */
	public static String getDeclaringTypeSignature(String key) {
		KeyToSignature keyToSignature = new KeyToSignature(key, KeyToSignature.DECLARING_TYPE);
		keyToSignature.parse();
		return keyToSignature.signature.toString();
	}

	/*
	 * Appends to the given buffer the fully qualified name (as it appears in the source) of the given type
	 */
	private static void getFullyQualifiedName(Type type, StringBuffer buffer) {
		switch (type.getNodeType()) {
			case ASTNode.ARRAY_TYPE:
				ArrayType arrayType = (ArrayType) type;
				getFullyQualifiedName(arrayType.getElementType(), buffer);
				for (int i = 0, length = arrayType.getDimensions(); i < length; i++) {
					buffer.append('[');
					buffer.append(']');
				}
				break;
			case ASTNode.PARAMETERIZED_TYPE:
				ParameterizedType parameterizedType = (ParameterizedType) type;
				getFullyQualifiedName(parameterizedType.getType(), buffer);
				buffer.append('<');
				Iterator iterator = parameterizedType.typeArguments().iterator();
				boolean isFirst = true;
				while (iterator.hasNext()) {
					if (!isFirst)
						buffer.append(',');
					else
						isFirst = false;
					Type typeArgument = (Type) iterator.next();
					getFullyQualifiedName(typeArgument, buffer);
				}
				buffer.append('>');
				break;
			case ASTNode.PRIMITIVE_TYPE:
				buffer.append(((PrimitiveType) type).getPrimitiveTypeCode().toString());
				break;
			case ASTNode.QUALIFIED_TYPE:
				buffer.append(((QualifiedType) type).getName().getFullyQualifiedName());
				break;
			case ASTNode.SIMPLE_TYPE:
				buffer.append(((SimpleType) type).getName().getFullyQualifiedName());
				break;
			case ASTNode.WILDCARD_TYPE:
				buffer.append('?');
				WildcardType wildcardType = (WildcardType) type;
				Type bound = wildcardType.getBound();
				if (bound == null) return;
				if (wildcardType.isUpperBound()) {
					buffer.append(" extends "); //$NON-NLS-1$
				} else {
					buffer.append(" super "); //$NON-NLS-1$
				}
				getFullyQualifiedName(bound, buffer);
				break;
		}
	}

	/**
	 * Returns a trimmed version the simples names returned by Signature.
	 */
	public static String[] getTrimmedSimpleNames(String name) {
		String[] result = Signature.getSimpleNames(name);
		for (int i = 0, length = result.length; i < length; i++) {
			result[i] = result[i].trim();
		}
		return result;
	}

	/**
	 * Return the java element corresponding to the given compiler binding.
	 */
	public static JavaElement getUnresolvedJavaElement(FieldBinding binding, WorkingCopyOwner workingCopyOwner, BindingsToNodesMap bindingsToNodes) {
		if (binding.declaringClass == null) return null; // array length
		JavaElement unresolvedJavaElement = getUnresolvedJavaElement(binding.declaringClass, workingCopyOwner, bindingsToNodes);
		if (unresolvedJavaElement == null || unresolvedJavaElement.getElementType() != IJavaElement.TYPE) {
			return null;
		}
		return (JavaElement) ((IType) unresolvedJavaElement).getField(String.valueOf(binding.name));
	}

	/**
	 * Return the java element corresponding to the given compiler binding.
	 */
	public static JavaElement getUnresolvedJavaElement(RecordComponentBinding binding, WorkingCopyOwner workingCopyOwner, BindingsToNodesMap bindingsToNodes) {
		if (binding.declaringRecord == null) return null; // array length
		JavaElement unresolvedJavaElement = getUnresolvedJavaElement(binding.declaringRecord, workingCopyOwner, bindingsToNodes);
		if (unresolvedJavaElement == null || unresolvedJavaElement.getElementType() != IJavaElement.TYPE) {
			return null;
		}
		return (JavaElement) ((IType) unresolvedJavaElement).getField(String.valueOf(binding.name));
	}

	/**
	 * Returns the IInitializer that contains the given local variable in the given type
	 */
	public static JavaElement getUnresolvedJavaElement(int localSourceStart, int localSourceEnd, JavaElement type) {
		try {
			if (!(type instanceof IType))
				return null;
			IInitializer[] initializers = ((IType) type).getInitializers();
			for (int i = 0; i < initializers.length; i++) {
				IInitializer initializer = initializers[i];
				ISourceRange sourceRange = initializer.getSourceRange();
				if (sourceRange != null) {
					int initializerStart = sourceRange.getOffset();
					int initializerEnd = initializerStart + sourceRange.getLength();
					if (initializerStart <= localSourceStart && localSourceEnd <= initializerEnd) {
						return (JavaElement) initializer;
					}
				}
			}
			return null;
		} catch (JavaModelException e) {
			return null;
		}
	}

	/**
	 * Return the java element corresponding to the given compiler binding.
	 */
	public static JavaElement getUnresolvedJavaElement(MethodBinding methodBinding, WorkingCopyOwner workingCopyOwner, BindingsToNodesMap bindingsToNodes) {
		JavaElement unresolvedJavaElement = getUnresolvedJavaElement(methodBinding.declaringClass, workingCopyOwner, bindingsToNodes);
		if (unresolvedJavaElement == null || unresolvedJavaElement.getElementType() != IJavaElement.TYPE) {
			return null;
		}
		IType declaringType = (IType) unresolvedJavaElement;

		org.eclipse.jdt.internal.compiler.ast.ASTNode node = bindingsToNodes == null ? null : bindingsToNodes.get(methodBinding);
		if (node != null && !declaringType.isBinary()) {
			if (node instanceof AnnotationMethodDeclaration) {
				// node is an AnnotationMethodDeclaration
				AnnotationMethodDeclaration typeMemberDeclaration = (AnnotationMethodDeclaration) node;
				return (JavaElement) declaringType.getMethod(String.valueOf(typeMemberDeclaration.selector), CharOperation.NO_STRINGS); // annotation type members don't have parameters
			} else {
				// node is an MethodDeclaration
				MethodDeclaration methodDeclaration = (MethodDeclaration) node;

				Argument[] arguments = methodDeclaration.arguments;
				String[] parameterSignatures;
				if (arguments != null) {
					parameterSignatures = new String[arguments.length];
					for (int i = 0; i < arguments.length; i++) {
						Argument argument = arguments[i];
						TypeReference typeReference = argument.type;
						int arrayDim = typeReference.dimensions();

						String typeSig =
							Signature.createTypeSignature(
									CharOperation.concatWith(
											typeReference.getTypeName(), '.'), false);
						if (arrayDim > 0) {
							typeSig = Signature.createArraySignature(typeSig, arrayDim);
						}
						parameterSignatures[i] = typeSig;

					}
				} else {
					parameterSignatures = CharOperation.NO_STRINGS;
				}
				return (JavaElement) declaringType.getMethod(String.valueOf(methodDeclaration.selector), parameterSignatures);
			}
		} else {
			// case of method not in the created AST, or a binary method
			org.eclipse.jdt.internal.compiler.lookup.MethodBinding original = methodBinding.original();
			String selector = original.isConstructor() ? declaringType.getElementName() : new String(original.selector);
			boolean isBinary = declaringType.isBinary();
			ReferenceBinding enclosingType = original.declaringClass.enclosingType();
			// Static inner types' constructors don't get receivers (https://bugs.eclipse.org/bugs/show_bug.cgi?id=388137)
			boolean isInnerBinaryTypeConstructor = isBinary && original.isConstructor() && !original.declaringClass.isStatic() && enclosingType != null;
			TypeBinding[] parameters = original.parameters;
			int length = parameters == null ? 0 : parameters.length;
			int declaringIndex = isInnerBinaryTypeConstructor ? 1 : 0;
			String[] parameterSignatures = new String[declaringIndex + length];
			if (isInnerBinaryTypeConstructor)
				parameterSignatures[0] = new String(enclosingType.genericTypeSignature()).replace('/', '.');
			for (int i = 0;  i < length; i++) {
				char[] signature = parameters[i].genericTypeSignature();
				if (isBinary) {
					signature = CharOperation.replaceOnCopy(signature, '/', '.');
				} else {
					signature = toUnresolvedTypeSignature(signature);
				}
				parameterSignatures[declaringIndex + i] = new String(signature);
			}
			IMethod result = declaringType.getMethod(selector, parameterSignatures);
			if (isBinary)
				return (JavaElement) result;
			if (result.exists()) // if perfect match (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=249567 )
				return (JavaElement) result;
			IMethod[] methods = null;
			try {
				methods = declaringType.getMethods();
			} catch (JavaModelException e) {
				// declaring type doesn't exist
				return null;
			}
			IMethod[] candidates = Member.findMethods(result, methods);
			if (candidates == null || candidates.length == 0)
				return null;
			return (JavaElement) candidates[0];
		}
	}

	/**
	 * Return the java element corresponding to the given compiler binding.
	 */
	public static JavaElement getUnresolvedJavaElement(TypeBinding typeBinding, WorkingCopyOwner workingCopyOwner, BindingsToNodesMap bindingsToNodes) {
		if (typeBinding == null)
			return null;
		switch (typeBinding.kind()) {
			case Binding.ARRAY_TYPE :
				typeBinding = ((org.eclipse.jdt.internal.compiler.lookup.ArrayBinding) typeBinding).leafComponentType();
				return getUnresolvedJavaElement(typeBinding, workingCopyOwner, bindingsToNodes);
			case Binding.BASE_TYPE :
			case Binding.WILDCARD_TYPE :
			case Binding.INTERSECTION_TYPE:
				return null;
			default :
				if (typeBinding.isCapture())
					return null;
		}
		ReferenceBinding referenceBinding;
		if (typeBinding.isParameterizedType() || typeBinding.isRawType())
			referenceBinding = (ReferenceBinding) typeBinding.erasure();
		else
			referenceBinding = (ReferenceBinding) typeBinding;
		char[] fileName = referenceBinding.getFileName();
		if (referenceBinding.isLocalType() || referenceBinding.isAnonymousType()) {
			// local or anonymous type
			if (org.eclipse.jdt.internal.compiler.util.Util.isClassFileName(fileName)) {
				int jarSeparator = CharOperation.indexOf(IDependent.JAR_FILE_ENTRY_SEPARATOR, fileName);
				int pkgEnd = CharOperation.lastIndexOf('/', fileName); // pkgEnd is exclusive
				if (pkgEnd == -1)
					pkgEnd = CharOperation.lastIndexOf(File.separatorChar, fileName);
				if (jarSeparator != -1 && pkgEnd < jarSeparator) // if in a jar and no slash, it is a default package -> pkgEnd should be equal to jarSeparator
					pkgEnd = jarSeparator;
				if (pkgEnd == -1)
					return null;
				IPackageFragment pkg = getPackageFragment(fileName, pkgEnd, jarSeparator);
				char[] constantPoolName = referenceBinding.constantPoolName();
				if (constantPoolName == null) {
					ClassFile classFile = (ClassFile) getClassFile(fileName);
					return classFile == null ? null : (JavaElement) classFile.getType();
				}
				pkgEnd = CharOperation.lastIndexOf('/', constantPoolName);
				char[] classFileName = CharOperation.subarray(constantPoolName, pkgEnd+1, constantPoolName.length);
				ClassFile classFile = (ClassFile) pkg.getClassFile(new String(classFileName) + SuffixConstants.SUFFIX_STRING_class);
				return (JavaElement) classFile.getType();
			}
			ICompilationUnit cu = getCompilationUnit(fileName, workingCopyOwner);
			if (cu == null) return null;
			// must use getElementAt(...) as there is no back pointer to the defining method (scope is null after resolution has ended)
			try {
				int sourceStart = ((org.eclipse.jdt.internal.compiler.lookup.LocalTypeBinding) referenceBinding).sourceStart;
				return (JavaElement) cu.getElementAt(sourceStart);
			} catch (JavaModelException e) {
				// does not exist
				return null;
			}
		} else if (referenceBinding.isTypeVariable()) {
			// type parameter
			final String typeVariableName = new String(referenceBinding.sourceName());
			org.eclipse.jdt.internal.compiler.lookup.Binding declaringElement = ((org.eclipse.jdt.internal.compiler.lookup.TypeVariableBinding) referenceBinding).declaringElement;
			if (declaringElement instanceof MethodBinding) {
				IMethod declaringMethod = (IMethod) getUnresolvedJavaElement((MethodBinding) declaringElement, workingCopyOwner, bindingsToNodes);
				return (JavaElement) declaringMethod.getTypeParameter(typeVariableName);
			} else {
				IType declaringType = (IType) getUnresolvedJavaElement((TypeBinding) declaringElement, workingCopyOwner, bindingsToNodes);
				if (declaringType == null)
					return null;
				return (JavaElement) declaringType.getTypeParameter(typeVariableName);
			}
		} else {
			if (fileName == null) return null; // case of a WilCardBinding that doesn't have a corresponding Java element
			// member or top level type
			TypeBinding declaringTypeBinding = typeBinding.enclosingType();
			if (declaringTypeBinding == null) {
				// top level type
				if (org.eclipse.jdt.internal.compiler.util.Util.isClassFileName(fileName)) {
					ClassFile classFile = (ClassFile) getClassFile(fileName);
					if (classFile == null) return null;
					return (JavaElement) classFile.getType();
				}
				ICompilationUnit cu = getCompilationUnit(fileName, workingCopyOwner);
				if (cu == null) return null;
				return (JavaElement) cu.getType(new String(referenceBinding.sourceName()));
			} else {
				// member type
				IType declaringType = (IType) getUnresolvedJavaElement(declaringTypeBinding, workingCopyOwner, bindingsToNodes);
				if (declaringType == null) return null;
				return (JavaElement) declaringType.getType(new String(referenceBinding.sourceName()));
			}
		}
	}

	/*
	 * Returns the index of the most specific argument paths which is strictly enclosing the path to check
	 */
	public static int indexOfEnclosingPath(IPath checkedPath, IPath[] paths, int pathCount) {

	    int bestMatch = -1, bestLength = -1;
		for (int i = 0; i < pathCount; i++){
			if (paths[i].equals(checkedPath)) continue;
			if (paths[i].isPrefixOf(checkedPath)) {
			    int currentLength = paths[i].segmentCount();
			    if (currentLength > bestLength) {
			        bestLength = currentLength;
			        bestMatch = i;
			    }
			}
		}
		return bestMatch;
	}

	/*
	 * Returns the index of the Java like extension of the given file name
	 * or -1 if it doesn't end with a known Java like extension.
	 * Note this is the index of the '.' even if it is not considered part of the extension.
	 */
	public static int indexOfJavaLikeExtension(String fileName) {
		int fileNameLength = fileName.length();
		char[][] javaLikeExtensions = getJavaLikeExtensions();
		extensions: for (int i = 0, length = javaLikeExtensions.length; i < length; i++) {
			char[] extension = javaLikeExtensions[i];
			int extensionLength = extension.length;
			int extensionStart = fileNameLength - extensionLength;
			int dotIndex = extensionStart - 1;
			if (dotIndex < 0) continue;
			if (fileName.charAt(dotIndex) != '.') continue;
			for (int j = 0; j < extensionLength; j++) {
				if (fileName.charAt(extensionStart + j) != extension[j])
					continue extensions;
			}
			return dotIndex;
		}
		return -1;
	}

	/*
	 * Returns the index of the first argument paths which is equal to the path to check
	 */
	public static int indexOfMatchingPath(IPath checkedPath, IPath[] paths, int pathCount) {

		for (int i = 0; i < pathCount; i++){
			if (paths[i].equals(checkedPath)) return i;
		}
		return -1;
	}

	/*
	 * Returns the index of the first argument paths which is strictly nested inside the path to check
	 */
	public static int indexOfNestedPath(IPath checkedPath, IPath[] paths, int pathCount) {

		for (int i = 0; i < pathCount; i++){
			if (checkedPath.equals(paths[i])) continue;
			if (checkedPath.isPrefixOf(paths[i])) return i;
		}
		return -1;
	}
	/**
	 * Returns whether the local file system supports accessing and modifying
	 * the given attribute.
	 */
	protected static boolean isAttributeSupported(int attribute) {
		return (EFS.getLocalFileSystem().attributes() & attribute) != 0;
	}

	/**
	 * Returns whether the given resource is read-only or not.
	 * @param resource
	 * @return <code>true</code> if the resource is read-only, <code>false</code> if it is not or
	 * 	if the file system does not support the read-only attribute.
	 */
	public static boolean isReadOnly(IResource resource) {
		if (isReadOnlySupported()) {
			ResourceAttributes resourceAttributes = resource.getResourceAttributes();
			if (resourceAttributes == null) return false; // not supported on this platform for this resource
			return resourceAttributes.isReadOnly();
		}
		return false;
	}

	/**
	 * Returns whether the local file system supports accessing and modifying
	 * the read only flag.
	 */
	public static boolean isReadOnlySupported() {
		return isAttributeSupported(EFS.ATTRIBUTE_READ_ONLY);
	}

	/*
	 * Returns whether the given java element is exluded from its root's classpath.
	 * It doesn't check whether the root itself is on the classpath or not
	 */
	public static final boolean isExcluded(IJavaElement element) {
		int elementType = element.getElementType();
		switch (elementType) {
			case IJavaElement.JAVA_MODEL:
			case IJavaElement.JAVA_PROJECT:
			case IJavaElement.PACKAGE_FRAGMENT_ROOT:
				return false;

			case IJavaElement.PACKAGE_FRAGMENT:
				PackageFragmentRoot root = (PackageFragmentRoot) element.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
				IResource resource = ((PackageFragment) element).resource();
				return resource != null && isExcluded(resource, root.fullInclusionPatternChars(), root.fullExclusionPatternChars());

			case IJavaElement.COMPILATION_UNIT:
				root = (PackageFragmentRoot) element.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
				resource = element.getResource();
				if (resource == null)
					return false;
				if (isExcluded(resource, root.fullInclusionPatternChars(), root.fullExclusionPatternChars()))
					return true;
				return isExcluded(element.getParent());

			default:
				IJavaElement cu = element.getAncestor(IJavaElement.COMPILATION_UNIT);
				return cu != null && isExcluded(cu);
		}
	}
	/*
	 * Returns whether the given resource path matches one of the inclusion/exclusion
	 * patterns.
	 * NOTE: should not be asked directly using pkg root pathes
	 * @see IClasspathEntry#getInclusionPatterns
	 * @see IClasspathEntry#getExclusionPatterns
	 */
	public final static boolean isExcluded(IPath resourcePath, char[][] inclusionPatterns, char[][] exclusionPatterns, boolean isFolderPath) {
		if (inclusionPatterns == null && exclusionPatterns == null) return false;
		return org.eclipse.jdt.internal.compiler.util.Util.isExcluded(resourcePath.toString().toCharArray(), inclusionPatterns, exclusionPatterns, isFolderPath);
	}

	/*
	 * Returns whether the given resource matches one of the exclusion patterns.
	 * NOTE: should not be asked directly using pkg root pathes
	 * @see IClasspathEntry#getExclusionPatterns
	 */
	public final static boolean isExcluded(IResource resource, char[][] inclusionPatterns, char[][] exclusionPatterns) {
		IPath path = resource.getFullPath();
		// ensure that folders are only excluded if all of their children are excluded
		int resourceType = resource.getType();
		return isExcluded(path, inclusionPatterns, exclusionPatterns, resourceType == IResource.FOLDER || resourceType == IResource.PROJECT);
	}


	/**
	 * Validate the given .class file name.
	 * A .class file name must obey the following rules:
	 * <ul>
	 * <li> it must not be null
	 * <li> it must include the <code>".class"</code> suffix
	 * <li> its prefix must be a valid identifier
	 * </ul>
	 * </p>
	 * @param name the name of a .class file
	 * @param sourceLevel the source level
	 * @param complianceLevel the compliance level
	 * @return a status object with code <code>IStatus.OK</code> if
	 *		the given name is valid as a .class file name, otherwise a status
	 *		object indicating what is wrong with the name
	 */
	public static boolean isValidClassFileName(String name, String sourceLevel, String complianceLevel) {
		return JavaConventions.validateClassFileName(name, sourceLevel, complianceLevel).getSeverity() != IStatus.ERROR;
	}


	/**
	 * Validate the given compilation unit name.
	 * A compilation unit name must obey the following rules:
	 * <ul>
	 * <li> it must not be null
	 * <li> it must include the <code>".java"</code> suffix
	 * <li> its prefix must be a valid identifier
	 * </ul>
	 * </p>
	 * @param name the name of a compilation unit
	 * @param sourceLevel the source level
	 * @param complianceLevel the compliance level
	 * @return a status object with code <code>IStatus.OK</code> if
	 *		the given name is valid as a compilation unit name, otherwise a status
	 *		object indicating what is wrong with the name
	 */
	public static boolean isValidCompilationUnitName(String name, String sourceLevel, String complianceLevel) {
		return JavaConventions.validateCompilationUnitName(name, sourceLevel, complianceLevel).getSeverity() != IStatus.ERROR;
	}

	/**
	 * Returns true if the given folder name is valid for a package,
	 * false if it is not.
	 * @param folderName the name of the folder
	 * @param sourceLevel the source level
	 * @param complianceLevel the compliance level
	 */
	public static boolean isValidFolderNameForPackage(String folderName, String sourceLevel, String complianceLevel) {
		return JavaConventions.validateIdentifier(folderName, sourceLevel, complianceLevel).getSeverity() != IStatus.ERROR;
	}

	/**
	 * Returns true if the given method signature is valid,
	 * false if it is not.
	 */
	public static boolean isValidMethodSignature(String sig) {
		int len = sig.length();
		if (len == 0) return false;
		int i = 0;
		char c = sig.charAt(i++);
		if (c != '(') return false;
		if (i >= len) return false;
		while (sig.charAt(i) != ')') {
			// Void is not allowed as a parameter type.
			i = checkTypeSignature(sig, i, len, false);
			if (i == -1) return false;
			if (i >= len) return false;
		}
		++i;
		i = checkTypeSignature(sig, i, len, true);
		return i == len;
	}

	/**
	 * Returns true if the given type signature is valid,
	 * false if it is not.
	 */
	public static boolean isValidTypeSignature(String sig, boolean allowVoid) {
		int len = sig.length();
		return checkTypeSignature(sig, 0, len, allowVoid) == len;
	}

	/*
	 * Returns the simple name of a local type from the given binary type name.
	 * The last '$' is at lastDollar. The last character of the type name is at end-1.
	 */
	public static String localTypeName(String binaryTypeName, int lastDollar, int end) {
		if (lastDollar > 0 && binaryTypeName.charAt(lastDollar-1) == '$')
			// local name starts with a dollar sign
			// (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=103466)
			return binaryTypeName;
		int nameStart = lastDollar+1;
		while (nameStart < end && Character.isDigit(binaryTypeName.charAt(nameStart)))
			nameStart++;
		return binaryTypeName.substring(nameStart, end);
	}

	/*
	 * Add a log entry
	 */
	public static void log(Throwable e, String message) {
		Throwable nestedException;
		if (e instanceof JavaModelException
				&& (nestedException = ((JavaModelException)e).getException()) != null) {
			e = nestedException;
		}
		log(new Status(
				IStatus.ERROR,
				JavaCore.PLUGIN_ID,
				IStatus.ERROR,
				message,
				e));
	}

	/**
	 * Log a message that is potentially repeated in the same session.
	 * The first time this method is called with a given exception, the
	 * exception stack trace is written to the log.
	 * <p>Only intended for use in debug statements.</p>
	 *
	 * @param key the given key
	 * @param e the given exception
	 * @throws IllegalArgumentException if the given key is null
	 */
	public static void logRepeatedMessage(String key, Exception e) {
		if (key == null) {
			throw new IllegalArgumentException("key cannot be null"); //$NON-NLS-1$
		}
		if (fgRepeatedMessages.contains(key)) {
			return;
		}
		fgRepeatedMessages.add(key);
		log(e);
	}

	public static void logRepeatedMessage(String key, int statusErrorID, String message) {
		if (key == null) {
			throw new IllegalArgumentException("key cannot be null"); //$NON-NLS-1$
		}
		if (fgRepeatedMessages.contains(key)) {
			return;
		}
		fgRepeatedMessages.add(key);
		log(statusErrorID, message);
	}

	/*
	 * Add a log entry
	 */
	public static void log(int statusErrorID, String message) {
		log(new Status(
				statusErrorID,
				JavaCore.PLUGIN_ID,
				message));
	}

	/*
	 * Add a log entry
	 */
	public static void log(IStatus status) {
		Plugin plugin = JavaCore.getPlugin();
		if (plugin == null) {
			System.err.println(status.toString());
		} else {
			plugin.getLog().log(status);
		}
	}

	public static void log(Throwable e) {
		if (e instanceof CoreException) {
			log(((CoreException)e).getStatus());
		} else {
			log(new Status(IStatus.ERROR, JavaCore.PLUGIN_ID, Messages.internal_error, e));
		}
	}

	public static ClassFileReader newClassFileReader(IResource resource) throws CoreException, ClassFormatException, IOException {
		try (InputStream in = ((IFile) resource).getContents(true)) {
			return ClassFileReader.read(in, resource.getFullPath().toString());
		}
	}

	/**
	 * Normalizes the cariage returns in the given text.
	 * They are all changed  to use the given buffer's line separator.
	 */
	public static char[] normalizeCRs(char[] text, char[] buffer) {
		CharArrayBuffer result = new CharArrayBuffer();
		int lineStart = 0;
		int length = text.length;
		if (length == 0) return text;
		String lineSeparator = getLineSeparator(text, buffer);
		char nextChar = text[0];
		for (int i = 0; i < length; i++) {
			char currentChar = nextChar;
			nextChar = i < length-1 ? text[i+1] : ' ';
			switch (currentChar) {
				case '\n':
					int lineLength = i-lineStart;
					char[] line = new char[lineLength];
					System.arraycopy(text, lineStart, line, 0, lineLength);
					result.append(line);
					result.append(lineSeparator);
					lineStart = i+1;
					break;
				case '\r':
					lineLength = i-lineStart;
					if (lineLength >= 0) {
						line = new char[lineLength];
						System.arraycopy(text, lineStart, line, 0, lineLength);
						result.append(line);
						result.append(lineSeparator);
						if (nextChar == '\n') {
							nextChar = ' ';
							lineStart = i+2;
						} else {
							// when line separator are mixed in the same file
							// \r might not be followed by a \n. If not, we should increment
							// lineStart by one and not by two.
							lineStart = i+1;
						}
					} else {
						// when line separator are mixed in the same file
						// we need to prevent NegativeArraySizeException
						lineStart = i+1;
					}
					break;
			}
		}
		char[] lastLine;
		if (lineStart > 0) {
			int lastLineLength = length-lineStart;
			if (lastLineLength > 0) {
				lastLine = new char[lastLineLength];
				System.arraycopy(text, lineStart, lastLine, 0, lastLineLength);
				result.append(lastLine);
			}
			return result.getContents();
		}
		return text;
	}

	/**
	 * Normalizes the carriage returns in the given text.
	 * They are all changed to use given buffer's line separator.
	 */
	public static String normalizeCRs(String text, String buffer) {
		return new String(normalizeCRs(text.toCharArray(), buffer.toCharArray()));
	}

	/**
	 * Converts the given relative path into a package name.
	 * Returns null if the path is not a valid package name.
	 * @param pkgPath the package path
	 * @param sourceLevel the source level
	 * @param complianceLevel the compliance level
	 */
	public static String packageName(IPath pkgPath, String sourceLevel, String complianceLevel) {
		StringBuilder pkgName = new StringBuilder(IPackageFragment.DEFAULT_PACKAGE_NAME);
		for (int j = 0, max = pkgPath.segmentCount(); j < max; j++) {
			String segment = pkgPath.segment(j);
			if (!isValidFolderNameForPackage(segment, sourceLevel, complianceLevel)) {
				return null;
			}
			pkgName.append(segment);
			if (j < pkgPath.segmentCount() - 1) {
				pkgName.append("." ); //$NON-NLS-1$
			}
		}
		return pkgName.toString();
	}

	/**
	 * Returns the length of the common prefix between s1 and s2.
	 */
	public static int prefixLength(char[] s1, char[] s2) {
		int len= 0;
		int max= Math.min(s1.length, s2.length);
		for (int i= 0; i < max && s1[i] == s2[i]; ++i)
			++len;
		return len;
	}
	/**
	 * Returns the length of the common prefix between s1 and s2.
	 */
	public static int prefixLength(String s1, String s2) {
		int len= 0;
		int max= Math.min(s1.length(), s2.length());
		for (int i= 0; i < max && s1.charAt(i) == s2.charAt(i); ++i)
			++len;
		return len;
	}
	private static void quickSort(char[][] list, int left, int right) {
		int original_left= left;
		int original_right= right;
		char[] mid= list[left + (right - left) / 2];
		do {
			while (compare(list[left], mid) < 0) {
				left++;
			}
			while (compare(mid, list[right]) < 0) {
				right--;
			}
			if (left <= right) {
				char[] tmp= list[left];
				list[left]= list[right];
				list[right]= tmp;
				left++;
				right--;
			}
		} while (left <= right);
		if (original_left < right) {
			quickSort(list, original_left, right);
		}
		if (left < original_right) {
			quickSort(list, left, original_right);
		}
	}

	/**
	 * Sort the comparable objects in the given collection.
	 */
	private static void quickSort(Comparable[] sortedCollection, int left, int right) {
		int original_left = left;
		int original_right = right;
		Comparable mid = sortedCollection[ left + (right - left) / 2];
		do {
			while (sortedCollection[left].compareTo(mid) < 0) {
				left++;
			}
			while (mid.compareTo(sortedCollection[right]) < 0) {
				right--;
			}
			if (left <= right) {
				Comparable tmp = sortedCollection[left];
				sortedCollection[left] = sortedCollection[right];
				sortedCollection[right] = tmp;
				left++;
				right--;
			}
		} while (left <= right);
		if (original_left < right) {
			quickSort(sortedCollection, original_left, right);
		}
		if (left < original_right) {
			quickSort(sortedCollection, left, original_right);
		}
	}
	private static void quickSort(int[] list, int left, int right) {
		int original_left= left;
		int original_right= right;
		int mid= list[left + (right - left) / 2];
		do {
			while (list[left] < mid) {
				left++;
			}
			while (mid < list[right]) {
				right--;
			}
			if (left <= right) {
				int tmp= list[left];
				list[left]= list[right];
				list[right]= tmp;
				left++;
				right--;
			}
		} while (left <= right);
		if (original_left < right) {
			quickSort(list, original_left, right);
		}
		if (left < original_right) {
			quickSort(list, left, original_right);
		}
	}

	/**
	 * Sort the objects in the given collection using the given comparer.
	 */
	private static void quickSort(Object[] sortedCollection, int left, int right, Comparer comparer) {
		int original_left = left;
		int original_right = right;
		Object mid = sortedCollection[ left + (right - left) / 2];
		do {
			while (comparer.compare(sortedCollection[left], mid) < 0) {
				left++;
			}
			while (comparer.compare(mid, sortedCollection[right]) < 0) {
				right--;
			}
			if (left <= right) {
				Object tmp = sortedCollection[left];
				sortedCollection[left] = sortedCollection[right];
				sortedCollection[right] = tmp;
				left++;
				right--;
			}
		} while (left <= right);
		if (original_left < right) {
			quickSort(sortedCollection, original_left, right, comparer);
		}
		if (left < original_right) {
			quickSort(sortedCollection, left, original_right, comparer);
		}
	}

	/**
	 * Sort the strings in the given collection.
	 */
	private static void quickSort(String[] sortedCollection, int left, int right) {
		int original_left = left;
		int original_right = right;
		String mid = sortedCollection[ left + (right - left) / 2];
		do {
			while (sortedCollection[left].compareTo(mid) < 0) {
				left++;
			}
			while (mid.compareTo(sortedCollection[right]) < 0) {
				right--;
			}
			if (left <= right) {
				String tmp = sortedCollection[left];
				sortedCollection[left] = sortedCollection[right];
				sortedCollection[right] = tmp;
				left++;
				right--;
			}
		} while (left <= right);
		if (original_left < right) {
			quickSort(sortedCollection, original_left, right);
		}
		if (left < original_right) {
			quickSort(sortedCollection, left, original_right);
		}
	}

	/**
	 * Returns the toString() of the given full path minus the first given number of segments.
	 * The returned string is always a relative path (it has no leading slash)
	 */
	public static String relativePath(IPath fullPath, int skipSegmentCount) {
		boolean hasTrailingSeparator = fullPath.hasTrailingSeparator();
		String[] segments = fullPath.segments();

		// compute length
		int length = 0;
		int max = segments.length;
		if (max > skipSegmentCount) {
			for (int i1 = skipSegmentCount; i1 < max; i1++) {
				length += segments[i1].length();
			}
			//add the separator lengths
			length += max - skipSegmentCount - 1;
		}
		if (hasTrailingSeparator)
			length++;

		char[] result = new char[length];
		int offset = 0;
		int len = segments.length - 1;
		if (len >= skipSegmentCount) {
			//append all but the last segment, with separators
			for (int i = skipSegmentCount; i < len; i++) {
				int size = segments[i].length();
				segments[i].getChars(0, size, result, offset);
				offset += size;
				result[offset++] = '/';
			}
			//append the last segment
			int size = segments[len].length();
			segments[len].getChars(0, size, result, offset);
			offset += size;
		}
		if (hasTrailingSeparator)
			result[offset++] = '/';
		return new String(result);
	}

	/*
	 * Resets the list of Java-like extensions after a change in content-type.
	 */
	public static void resetJavaLikeExtensions() {
		JAVA_LIKE_EXTENSIONS = null;
	}

	/**
	 * Scans the given string for a type signature starting at the given index
	 * and returns the index of the last character.
	 * <pre>
	 * TypeSignature:
	 *  |  BaseTypeSignature
	 *  |  ArrayTypeSignature
	 *  |  ClassTypeSignature
	 *  |  TypeVariableSignature
	 * </pre>
	 *
	 * @param string the signature string
	 * @param start the 0-based character index of the first character
	 * @return the 0-based character index of the last character
	 * @exception IllegalArgumentException if this is not a type signature
	 */
	public static int scanTypeSignature(char[] string, int start) {
		// this method is used in jdt.debug
		return org.eclipse.jdt.internal.compiler.util.Util.scanTypeSignature(string, start);
	}
	/**
	 * Return a new array which is the split of the given string using the given divider. The given end
	 * is exclusive and the given start is inclusive.
	 * <br>
	 * <br>
	 * For example:
	 * <ol>
	 * <li><pre>
	 *    divider = 'b'
	 *    string = "abbaba"
	 *    start = 2
	 *    end = 5
	 *    result => { "", "a", "" }
	 * </pre>
	 * </li>
	 * </ol>
	 *
	 * @param divider the given divider
	 * @param string the given string
	 * @param start the given starting index
	 * @param end the given ending index
	 * @return a new array which is the split of the given string using the given divider
	 * @throws ArrayIndexOutOfBoundsException if start is lower than 0 or end is greater than the array length
	 */
	public static final String[] splitOn(
		char divider,
		String string,
		int start,
		int end) {
		int length = string == null ? 0 : string.length();
		if (length == 0 || start > end)
			return CharOperation.NO_STRINGS;

		int wordCount = 1;
		for (int i = start; i < end; i++)
			if (string.charAt(i) == divider)
				wordCount++;
		String[] split = new String[wordCount];
		int last = start, currentWord = 0;
		for (int i = start; i < end; i++) {
			if (string.charAt(i) == divider) {
				split[currentWord++] = string.substring(last, i);
				last = i + 1;
			}
		}
		split[currentWord] = string.substring(last, end);
		return split;
	}
	/**
	 * Sets or unsets the given resource as read-only in the file system.
	 * It's a no-op if the file system does not support the read-only attribute.
	 *
	 * @param resource The resource to set as read-only
	 * @param readOnly <code>true</code> to set it to read-only,
	 *		<code>false</code> to unset
	 */
	public static void setReadOnly(IResource resource, boolean readOnly) {
		if (isReadOnlySupported()) {
			ResourceAttributes resourceAttributes = resource.getResourceAttributes();
			if (resourceAttributes == null) return; // not supported on this platform for this resource
			resourceAttributes.setReadOnly(readOnly);
			try {
				resource.setResourceAttributes(resourceAttributes);
			} catch (CoreException e) {
				// ignore
			}
		}
	}
	public static void sort(char[][] list) {
		if (list.length > 1)
			quickSort(list, 0, list.length - 1);
	}

	/**
	 * Sorts an array of Comparable objects in place.
	 */
	public static void sort(Comparable[] objects) {
		if (objects.length > 1)
			quickSort(objects, 0, objects.length - 1);
	}
	public static void sort(int[] list) {
		if (list.length > 1)
			quickSort(list, 0, list.length - 1);
	}

	/**
	 * Sorts an array of objects in place.
	 * The given comparer compares pairs of items.
	 */
	public static void sort(Object[] objects, Comparer comparer) {
		if (objects.length > 1)
			quickSort(objects, 0, objects.length - 1, comparer);
	}

	/**
	 * Sorts an array of strings in place using quicksort.
	 */
	public static void sort(String[] strings) {
		if (strings.length > 1)
			quickSort(strings, 0, strings.length - 1);
	}

	/**
	 * Sorts an array of Comparable objects, returning a new array
	 * with the sorted items.  The original array is left untouched.
	 */
	public static Comparable[] sortCopy(Comparable[] objects) {
		int len = objects.length;
		Comparable[] copy = new Comparable[len];
		System.arraycopy(objects, 0, copy, 0, len);
		sort(copy);
		return copy;
	}

	/**
	 * Sorts an array of Java elements based on their toStringWithAncestors(),
	 * returning a new array with the sorted items.
	 * The original array is left untouched.
	 */
	public static IJavaElement[] sortCopy(IJavaElement[] elements) {
		int len = elements.length;
		IJavaElement[] copy = new IJavaElement[len];
		System.arraycopy(elements, 0, copy, 0, len);
		sort(copy, new Comparer() {
			@Override
			public int compare(Object a, Object b) {
				return ((JavaElement) a).toStringWithAncestors().compareTo(((JavaElement) b).toStringWithAncestors());
			}
		});
		return copy;
	}

	/**
	 * Sorts an array of Strings, returning a new array
	 * with the sorted items.  The original array is left untouched.
	 */
	public static Object[] sortCopy(Object[] objects, Comparer comparer) {
		int len = objects.length;
		Object[] copy = new Object[len];
		System.arraycopy(objects, 0, copy, 0, len);
		sort(copy, comparer);
		return copy;
	}

	/**
	 * Sorts an array of Strings, returning a new array
	 * with the sorted items.  The original array is left untouched.
	 */
	public static String[] sortCopy(String[] objects) {
		int len = objects.length;
		String[] copy = new String[len];
		System.arraycopy(objects, 0, copy, 0, len);
		sort(copy);
		return copy;
	}

	/*
	 * Returns whether the given compound name starts with the given prefix.
	 * Returns true if the n first elements of the prefix are equals and the last element of the
	 * prefix is a prefix of the corresponding element in the compound name.
	 */
	public static boolean startsWithIgnoreCase(String[] compoundName, String[] prefix, boolean partialMatch) {
		int prefixLength = prefix.length;
		int nameLength = compoundName.length;
		if (prefixLength > nameLength) return false;
		for (int i = 0; i < prefixLength - 1; i++) {
			if (!compoundName[i].equalsIgnoreCase(prefix[i]))
				return false;
		}
		return (partialMatch || prefixLength == nameLength) && compoundName[prefixLength-1].toLowerCase().startsWith(prefix[prefixLength-1].toLowerCase());
	}

	/**
	 * Converts a String[] to char[][].
	 */
	public static char[][] toCharArrays(String[] a) {
		int len = a.length;
		if (len == 0) return CharOperation.NO_CHAR_CHAR;
		char[][] result = new char[len][];
		for (int i = 0; i < len; ++i) {
			result[i] = a[i].toCharArray();
		}
		return result;
	}

	/**
	 * Converts a String to char[][], where segments are separate by '.'.
	 */
	public static char[][] toCompoundChars(String s) {
		int len = s.length();
		if (len == 0) {
			return CharOperation.NO_CHAR_CHAR;
		}
		int segCount = 1;
		for (int off = s.indexOf('.'); off != -1; off = s.indexOf('.', off + 1)) {
			++segCount;
		}
		char[][] segs = new char[segCount][];
		int start = 0;
		for (int i = 0; i < segCount; ++i) {
			int dot = s.indexOf('.', start);
			int end = (dot == -1 ? s.length() : dot);
			segs[i] = new char[end - start];
			s.getChars(start, end, segs[i], 0);
			start = end + 1;
		}
		return segs;
	}

	/*
	 * Converts the given URI to a local file. Use the existing file if the uri is on the local file system.
	 * Otherwise fetch it.
	 * Returns null if unable to fetch it.
	 */
	public static File toLocalFile(URI uri, IProgressMonitor monitor) throws CoreException {
		IFileStore fileStore = EFS.getStore(uri);
		File localFile = fileStore.toLocalFile(EFS.NONE, monitor);
		if (localFile ==null)
			// non local file system
			localFile= fileStore.toLocalFile(EFS.CACHE, monitor);
		return localFile;
	}
	/**
	 * Converts a char[][] to String, where segments are separated by '.'.
	 */
	public static String toString(char[][] c) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0, max = c.length; i < max; ++i) {
			if (i != 0) sb.append('.');
			sb.append(c[i]);
		}
		return sb.toString();
	}

	/**
	 * Converts a char[][] and a char[] to String, where segments are separated by '.'.
	 */
	public static String toString(char[][] c, char[] d) {
		if (c == null) return new String(d);
		StringBuilder sb = new StringBuilder();
		for (int i = 0, max = c.length; i < max; ++i) {
			sb.append(c[i]);
			sb.append('.');
		}
		sb.append(d);
		return sb.toString();
	}

	/*
	 * Converts a char[][] to String[].
	 */
	public static String[] toStrings(char[][] a) {
		int len = a.length;
		String[] result = new String[len];
		for (int i = 0; i < len; ++i) {
			result[i] = new String(a[i]);
		}
		return result;
	}
	private static char[] toUnresolvedTypeSignature(char[] signature) {
		int length = signature.length;
		if (length <= 1)
			return signature;
		StringBuffer buffer = new StringBuffer(length);
		toUnresolvedTypeSignature(signature, 0, length, buffer);
		int bufferLength = buffer.length();
		char[] result = new char[bufferLength];
		buffer.getChars(0, bufferLength, result, 0);
		return result;
	}

	private static int toUnresolvedTypeSignature(char[] signature, int start, int length, StringBuffer buffer) {
		if (signature[start] == Signature.C_RESOLVED)
			buffer.append(Signature.C_UNRESOLVED);
		else
			buffer.append(signature[start]);
		for (int i = start+1; i < length; i++) {
			char c = signature[i];
			switch (c) {
			case '/':
			case Signature.C_DOLLAR:
				buffer.append(Signature.C_DOT);
				break;
			case Signature.C_GENERIC_START:
				buffer.append(Signature.C_GENERIC_START);
				i = toUnresolvedTypeSignature(signature, i+1, length, buffer);
				break;
			case Signature.C_GENERIC_END:
				buffer.append(Signature.C_GENERIC_END);
				return i;
			default:
				buffer.append(c);
				break;
			}
		}
		return length;
	}
	private static void appendArrayTypeSignature(char[] string, int start, StringBuffer buffer, boolean compact) {
		int length = string.length;
		// need a minimum 2 char
		if (start >= length - 1) {
			throw newIllegalArgumentException(string, start);
		}
		char c = string[start];
		if (c != Signature.C_ARRAY) {
			throw newUnexpectedCharacterException(string, start, c);
		}

		int index = start;
		c = string[++index];
		while(c == Signature.C_ARRAY) {
			// need a minimum 2 char
			if (index >= length - 1) {
				throw newIllegalArgumentException(string, start);
			}
			c = string[++index];
		}

		appendTypeSignature(string, index, buffer, compact);

		for(int i = 0, dims = index - start; i < dims; i++) {
			buffer.append('[').append(']');
		}
	}
	private static void appendClassTypeSignature(char[] string, int start, StringBuffer buffer, boolean compact) {
		char c = string[start];
		if (c != Signature.C_RESOLVED) {
			return;
		}
		int p = start + 1;
		int checkpoint = buffer.length();
		while (true) {
			c = string[p];
			switch(c) {
				case Signature.C_SEMICOLON :
					// all done
					return;
				case Signature.C_DOT :
				case '/' :
					// erase package prefix
					if (compact) {
						buffer.setLength(checkpoint);
					} else {
						buffer.append('.');
					}
					break;
				 case Signature.C_DOLLAR :
					/**
					 * Convert '$' in resolved type signatures into '.'.
					 * NOTE: This assumes that the type signature is an inner type
					 * signature. This is true in most cases, but someone can define a
					 * non-inner type name containing a '$'.
					 */
					buffer.append('.');
				 	break;
				 default :
					buffer.append(c);
			}
			p++;
		}
	}
	static void appendTypeSignature(char[] string, int start, StringBuffer buffer, boolean compact) {
		char c = string[start];
		switch (c) {
			case Signature.C_ARRAY :
				appendArrayTypeSignature(string, start, buffer, compact);
				break;
			case Signature.C_RESOLVED :
				appendClassTypeSignature(string, start, buffer, compact);
				break;
			case Signature.C_TYPE_VARIABLE :
				int e = org.eclipse.jdt.internal.compiler.util.Util.scanTypeVariableSignature(string, start);
				buffer.append(string, start + 1, e - start - 1);
				break;
			case Signature.C_BOOLEAN :
				buffer.append(BOOLEAN);
				break;
			case Signature.C_BYTE :
				buffer.append(BYTE);
				break;
			case Signature.C_CHAR :
				buffer.append(CHAR);
				break;
			case Signature.C_DOUBLE :
				buffer.append(DOUBLE);
				break;
			case Signature.C_FLOAT :
				buffer.append(FLOAT);
				break;
			case Signature.C_INT :
				buffer.append(INT);
				break;
			case Signature.C_LONG :
				buffer.append(LONG);
				break;
			case Signature.C_SHORT :
				buffer.append(SHORT);
				break;
			case Signature.C_VOID :
				buffer.append(VOID);
				break;
		}
	}
	public static String toString(char[] declaringClass, char[] methodName, char[] methodSignature, boolean includeReturnType, boolean compact) {
		final boolean isConstructor = CharOperation.equals(methodName, INIT);
		int firstParen = CharOperation.indexOf(Signature.C_PARAM_START, methodSignature);
		if (firstParen == -1) {
			return ""; //$NON-NLS-1$
		}

		StringBuffer buffer = new StringBuffer(methodSignature.length + 10);

		// decode declaring class name
		// it can be either an array signature or a type signature
		if (declaringClass != null && declaringClass.length > 0) {
			char[] declaringClassSignature = null;
			if (declaringClass[0] == Signature.C_ARRAY) {
				CharOperation.replace(declaringClass, '/', '.');
				declaringClassSignature = Signature.toCharArray(declaringClass);
			} else {
				CharOperation.replace(declaringClass, '/', '.');
				declaringClassSignature = declaringClass;
			}
			int lastIndexOfSlash = CharOperation.lastIndexOf('.', declaringClassSignature);
			if (compact && lastIndexOfSlash != -1) {
				buffer.append(declaringClassSignature, lastIndexOfSlash + 1, declaringClassSignature.length - lastIndexOfSlash - 1);
			} else {
				buffer.append(declaringClassSignature);
			}
			if (!isConstructor) {
				buffer.append('.');
			}
		}

		// selector
		if (!isConstructor && methodName != null) {
			buffer.append(methodName);
		}

		// parameters
		buffer.append('(');
		char[][] pts = Signature.getParameterTypes(methodSignature);
		for (int i = 0, max = pts.length; i < max; i++) {
			appendTypeSignature(pts[i], 0 , buffer, compact);
			if (i != pts.length - 1) {
				buffer.append(',');
				buffer.append(' ');
			}
		}
		buffer.append(')');

		if (!isConstructor) {
			buffer.append(" : "); //$NON-NLS-1$
			// return type
			if (includeReturnType) {
				char[] rts = Signature.getReturnType(methodSignature);
				appendTypeSignature(rts, 0 , buffer, compact);
			}
		}
		return String.valueOf(buffer);
	}
	/*
	 * Returns the unresolved type parameter signatures of the given method
	 * e.g. {"QString;", "[int", "[[Qjava.util.Vector;"}
	 */
	public static String[] typeParameterSignatures(AbstractMethodDeclaration method) {
		Argument[] args = method.arguments;
		if (args != null) {
			int length = args.length;
			String[] signatures = new String[length];
			for (int i = 0; i < args.length; i++) {
				Argument arg = args[i];
				signatures[i] = typeSignature(arg.type);
			}
			return signatures;
		}
		return CharOperation.NO_STRINGS;
	}

	/*
	 * Returns the unresolved type signature of the given type reference,
	 * e.g. "QString;", "[int", "[[Qjava.util.Vector;"
	 */
	public static String typeSignature(TypeReference type) {
		String signature = null;
		if ((type.bits & org.eclipse.jdt.internal.compiler.ast.ASTNode.IsUnionType) != 0) {
			// special treatment for union type reference
			UnionTypeReference unionTypeReference = (UnionTypeReference) type;
			TypeReference[] typeReferences = unionTypeReference.typeReferences;
			String[] typeSignatures = typeSignatures(typeReferences);
			signature = Signature.createIntersectionTypeSignature(typeSignatures);
		} else if (type instanceof IntersectionCastTypeReference) {
			IntersectionCastTypeReference intersection = (IntersectionCastTypeReference) type;
			TypeReference[] typeReferences = intersection.typeReferences;
			String[] typeSignatures = typeSignatures(typeReferences);
			signature = Signature.createUnionTypeSignature(typeSignatures);
		} else {
			char[][] compoundName = type.getParameterizedTypeName();
			char[] typeName =CharOperation.concatWith(compoundName, '.');
			signature = Signature.createTypeSignature(typeName, false/*don't resolve*/);
		}
		return signature;
	}

	private static String[] typeSignatures(TypeReference[] types) {
		int length = types.length;
		String[] typeSignatures = new String[length];
		for(int i = 0; i < length; i++) {
			char[][] compoundName = types[i].getParameterizedTypeName();
			char[] typeName = CharOperation.concatWith(compoundName, '.');
			typeSignatures[i] = Signature.createTypeSignature(typeName, false/*don't resolve*/);
		}
		return typeSignatures;
	}
	/**
	 * Asserts that the given method signature is valid.
	 */
	public static void validateMethodSignature(String sig) {
		Assert.isTrue(isValidMethodSignature(sig));
	}

	/**
	 * Asserts that the given type signature is valid.
	 */
	public static void validateTypeSignature(String sig, boolean allowVoid) {
		Assert.isTrue(isValidTypeSignature(sig, allowVoid));
	}
	public static void verbose(String log) {
		verbose(log, System.out);
	}
	public static synchronized void verbose(String log, PrintStream printStream) {
		int start = 0;
		do {
			int end = log.indexOf('\n', start);
			printStream.print(Thread.currentThread());
			printStream.print(" "); //$NON-NLS-1$
			printStream.print(log.substring(start, end == -1 ? log.length() : end+1));
			start = end+1;
		} while (start != 0);
		printStream.println();
	}

	/**
	 * Returns true if the given name ends with one of the known java like extension.
	 * (implementation is not creating extra strings)
	 */
	public final static boolean isJavaLikeFileName(String name) {
		if (name == null) return false;
		return indexOfJavaLikeExtension(name) != -1;
	}

	/**
	 * Returns true if the given name ends with one of the known java like extension.
	 * (implementation is not creating extra strings)
	 */
	public final static boolean isJavaLikeFileName(char[] fileName) {
		if (fileName == null) return false;
		int fileNameLength = fileName.length;
		char[][] javaLikeExtensions = getJavaLikeExtensions();
		extensions: for (int i = 0, length = javaLikeExtensions.length; i < length; i++) {
			char[] extension = javaLikeExtensions[i];
			int extensionLength = extension.length;
			int extensionStart = fileNameLength - extensionLength;
			if (extensionStart-1 < 0) continue;
			if (fileName[extensionStart-1] != '.') continue;
			for (int j = 0; j < extensionLength; j++) {
				if (fileName[extensionStart + j] != extension[j])
					continue extensions;
			}
			return true;
		}
		return false;
	}

	/**
	 * Get all type arguments from an array of signatures.
	 *
	 * Example:
	 * 	For following type X<Y<Z>,V<W>,U>.A<B> signatures is:
	 * 	[
	 * 		['L','X','<','L','Y','<','L','Z',';'>',';','L','V','<','L','W',';'>',';','L','U',';',>',';'],
	 * 		['L','A','<','L','B',';','>',';']
	 * 	]
	 * 	@see #splitTypeLevelsSignature(String)
	 * 	Then, this method returns:
	 * 	[
	 * 		[
	 * 			['L','Y','<','L','Z',';'>',';'],
	 * 			['L','V','<','L','W',';'>',';'],
	 * 			['L','U',';']
	 * 		],
	 * 		[
	 * 			['L','B',';']
	 * 		]
	 * 	]
	 *
	 * @param typeSignatures Array of signatures (one per each type levels)
	 * @throws IllegalArgumentException If one of provided signature is malformed
	 * @return char[][][] Array of type arguments for each signature
	 */
	public final static char[][][] getAllTypeArguments(char[][] typeSignatures) {
		if (typeSignatures == null) return null;
		int length = typeSignatures.length;
		char[][][] typeArguments = new char[length][][];
		for (int i=0; i<length; i++){
			typeArguments[i] = Signature.getTypeArguments(typeSignatures[i]);
		}
		return typeArguments;
	}
	public static IAnnotation getAnnotation(JavaElement parent, IBinaryAnnotation binaryAnnotation, String memberValuePairName) {
		char[] typeName = org.eclipse.jdt.core.Signature.toCharArray(CharOperation.replaceOnCopy(binaryAnnotation.getTypeName(), '/', '.'));
		return new Annotation(parent, new String(typeName), memberValuePairName);
	}

	public static Object getAnnotationMemberValue(JavaElement parent, MemberValuePair memberValuePair, Object binaryValue) {
		if (binaryValue instanceof Constant) {
			return getAnnotationMemberValue(memberValuePair, (Constant) binaryValue);
		} else if (binaryValue instanceof IBinaryAnnotation) {
			memberValuePair.valueKind = IMemberValuePair.K_ANNOTATION;
			return getAnnotation(parent, (IBinaryAnnotation) binaryValue, memberValuePair.getMemberName());
		} else if (binaryValue instanceof ClassSignature) {
			memberValuePair.valueKind = IMemberValuePair.K_CLASS;
			char[] className = Signature.toCharArray(CharOperation.replaceOnCopy(((ClassSignature) binaryValue).getTypeName(), '/', '.'));
			return new String(className);
		} else if (binaryValue instanceof EnumConstantSignature) {
			memberValuePair.valueKind = IMemberValuePair.K_QUALIFIED_NAME;
			EnumConstantSignature enumConstant = (EnumConstantSignature) binaryValue;
			char[] enumName = Signature.toCharArray(CharOperation.replaceOnCopy(enumConstant.getTypeName(), '/', '.'));
			char[] qualifiedName = CharOperation.concat(enumName, enumConstant.getEnumConstantName(), '.');
			return new String(qualifiedName);
		} else if (binaryValue instanceof Object[]) {
			memberValuePair.valueKind = -1; // modified below by the first call to getMemberValue(...)
			Object[] binaryValues = (Object[]) binaryValue;
			int length = binaryValues.length;
			Object[] values = new Object[length];
			for (int i = 0; i < length; i++) {
				int previousValueKind = memberValuePair.valueKind;
				Object value = getAnnotationMemberValue(parent, memberValuePair, binaryValues[i]);
				if (previousValueKind != -1 && memberValuePair.valueKind != previousValueKind) {
					// values are heterogeneous, value kind is thus unknown
					memberValuePair.valueKind = IMemberValuePair.K_UNKNOWN;
				}
				if (value instanceof Annotation) {
					Annotation annotation = (Annotation) value;
					for (int j = 0; j < i; j++) {
						if (annotation.equals(values[j])) {
							annotation.occurrenceCount++;
						}
					}
				}
				values[i] = value;
			}
			if (memberValuePair.valueKind == -1)
				memberValuePair.valueKind = IMemberValuePair.K_UNKNOWN;
			return values;
		} else {
			memberValuePair.valueKind = IMemberValuePair.K_UNKNOWN;
			return null;
		}
	}

	/*
	 * Creates a member value from the given constant, and sets the valueKind on the given memberValuePair
	 */
	public static Object getAnnotationMemberValue(MemberValuePair memberValuePair, Constant constant) {
		if (constant == null) {
			memberValuePair.valueKind = IMemberValuePair.K_UNKNOWN;
			return null;
		}
		switch (constant.typeID()) {
			case TypeIds.T_int :
				memberValuePair.valueKind = IMemberValuePair.K_INT;
				return Integer.valueOf(constant.intValue());
			case TypeIds.T_byte :
				memberValuePair.valueKind = IMemberValuePair.K_BYTE;
				return Byte.valueOf(constant.byteValue());
			case TypeIds.T_short :
				memberValuePair.valueKind = IMemberValuePair.K_SHORT;
				return Short.valueOf(constant.shortValue());
			case TypeIds.T_char :
				memberValuePair.valueKind = IMemberValuePair.K_CHAR;
				return Character.valueOf(constant.charValue());
			case TypeIds.T_float :
				memberValuePair.valueKind = IMemberValuePair.K_FLOAT;
				return Float.valueOf(constant.floatValue());
			case TypeIds.T_double :
				memberValuePair.valueKind = IMemberValuePair.K_DOUBLE;
				return Double.valueOf(constant.doubleValue());
			case TypeIds.T_boolean :
				memberValuePair.valueKind = IMemberValuePair.K_BOOLEAN;
				return Boolean.valueOf(constant.booleanValue());
			case TypeIds.T_long :
				memberValuePair.valueKind = IMemberValuePair.K_LONG;
				return Long.valueOf(constant.longValue());
			case TypeIds.T_JavaLangString :
				memberValuePair.valueKind = IMemberValuePair.K_STRING;
				return constant.stringValue();
			default:
				memberValuePair.valueKind = IMemberValuePair.K_UNKNOWN;
				return null;
		}
	}

	/*
	 * Creates a member value from the given constant in case of negative numerals,
	 * and sets the valueKind on the given memberValuePair
	 */
	public static Object getNegativeAnnotationMemberValue(MemberValuePair memberValuePair, Constant constant) {
		if (constant == null) {
			memberValuePair.valueKind = IMemberValuePair.K_UNKNOWN;
			return null;
		}
		switch (constant.typeID()) {
			case TypeIds.T_int :
				memberValuePair.valueKind = IMemberValuePair.K_INT;
				return Integer.valueOf(constant.intValue() * -1);
			case TypeIds.T_float :
				memberValuePair.valueKind = IMemberValuePair.K_FLOAT;
				return Float.valueOf(constant.floatValue() * -1.0f);
			case TypeIds.T_double :
				memberValuePair.valueKind = IMemberValuePair.K_DOUBLE;
				return Double.valueOf(constant.doubleValue() * -1.0);
			case TypeIds.T_long :
				memberValuePair.valueKind = IMemberValuePair.K_LONG;
				return Long.valueOf(constant.longValue() * -1L);
			default:
				memberValuePair.valueKind = IMemberValuePair.K_UNKNOWN;
				return null;
		}
	}
	/**
	 * Split signatures of all levels  from a type unique key.
	 *
	 * Example:
	 * 	For following type X<Y<Z>,V<W>,U>.A<B>, unique key is:
	 * 	"LX<LY<LZ;>;LV<LW;>;LU;>.LA<LB;>;"
	 *
	 * 	The return splitted signatures array is:
	 * 	[
	 * 		['L','X','<','L','Y','<','L','Z',';'>',';','L','V','<','L','W',';'>',';','L','U','>',';'],
	 * 		['L','A','<','L','B',';','>',';']
	 *
	 * @param typeSignature ParameterizedSourceType type signature
	 * @return char[][] Array of signatures for each level of given unique key
	 */
	public final static char[][] splitTypeLevelsSignature(String typeSignature) {
		// In case of IJavaElement signature, replace '$' by '.'
		char[] source = Signature.removeCapture(typeSignature.toCharArray());
		CharOperation.replace(source, '$', '.');

		// Init counters and arrays
		char[][] signatures = new char[10][];
		int signaturesCount = 0;
//		int[] lengthes = new int [10];
		int paramOpening = 0;

		// Scan each signature character
		for (int idx=0, ln = source.length; idx < ln; idx++) {
			switch (source[idx]) {
				case '>':
					paramOpening--;
					if (paramOpening == 0)  {
						if (signaturesCount == signatures.length) {
							System.arraycopy(signatures, 0, signatures = new char[signaturesCount+10][], 0, signaturesCount);
						}
					}
					break;
				case '<':
					paramOpening++;
					break;
				case '.':
					if (paramOpening == 0)  {
						if (signaturesCount == signatures.length) {
							System.arraycopy(signatures, 0, signatures = new char[signaturesCount+10][], 0, signaturesCount);
						}
						signatures[signaturesCount] = new char[idx+1];
						System.arraycopy(source, 0, signatures[signaturesCount], 0, idx);
						signatures[signaturesCount][idx] = Signature.C_SEMICOLON;
						signaturesCount++;
					}
					break;
				case '/':
					source[idx] = '.';
					break;
			}
		}

		// Resize signatures array
		char[][] typeSignatures = new char[signaturesCount+1][];
		typeSignatures[0] = source;
		for (int i=1, j=signaturesCount-1; i<=signaturesCount; i++, j--){
			typeSignatures[i] = signatures[j];
		}
		return typeSignatures;
	}

	/*
	 * Can throw IllegalArgumentException or ArrayIndexOutOfBoundsException
	 */
	public static String toAnchor(int startingIndex, char[] methodSignature, String methodName, boolean isVarArgs) {
		try {
			return new String(toAnchor(startingIndex, methodSignature, methodName.toCharArray(), isVarArgs));
		} catch(IllegalArgumentException e) {
			return null;
		}
	}
	public static char[] toAnchor(int startingIndex, char[] methodSignature, char[] methodName, boolean isVargArgs) {
		int firstParen = CharOperation.indexOf(Signature.C_PARAM_START, methodSignature);
		if (firstParen == -1) {
			throw new IllegalArgumentException(String.valueOf(methodSignature));
		}

		StringBuffer buffer = new StringBuffer(methodSignature.length + 10);

		// selector
		if (methodName != null) {
			buffer.append(methodName);
		}

		// parameters
		buffer.append('(');
		char[][] pts = Signature.getParameterTypes(methodSignature);
		for (int i = startingIndex, max = pts.length; i < max; i++) {
			if (i == max - 1) {
				appendTypeSignatureForAnchor(pts[i], 0 , buffer, isVargArgs);
			} else {
				appendTypeSignatureForAnchor(pts[i], 0 , buffer, false);
			}
			if (i != pts.length - 1) {
				buffer.append(',');
				buffer.append(' ');
			}
		}
		buffer.append(')');
		char[] result = new char[buffer.length()];
		buffer.getChars(0, buffer.length(), result, 0);
		return result;
	}

	private static int appendTypeSignatureForAnchor(char[] string, int start, StringBuffer buffer, boolean isVarArgs) {
		// need a minimum 1 char
		if (start >= string.length) {
			throw newIllegalArgumentException(string, start);
		}
		char c = string[start];
		if (isVarArgs) {
			switch (c) {
				case Signature.C_ARRAY :
					return appendArrayTypeSignatureForAnchor(string, start, buffer, true);
				case Signature.C_RESOLVED :
				case Signature.C_TYPE_VARIABLE :
				case Signature.C_BOOLEAN :
				case Signature.C_BYTE :
				case Signature.C_CHAR :
				case Signature.C_DOUBLE :
				case Signature.C_FLOAT :
				case Signature.C_INT :
				case Signature.C_LONG :
				case Signature.C_SHORT :
				case Signature.C_VOID :
				case Signature.C_STAR:
				case Signature.C_EXTENDS:
				case Signature.C_SUPER:
				case Signature.C_CAPTURE:
				default:
					// a var args is an array type
					throw newUnexpectedCharacterException(string, start, c);
			}
		} else {
			switch (c) {
				case Signature.C_ARRAY :
					return appendArrayTypeSignatureForAnchor(string, start, buffer, false);
				case Signature.C_RESOLVED :
					return appendClassTypeSignatureForAnchor(string, start, buffer);
				case Signature.C_TYPE_VARIABLE :
					int e = org.eclipse.jdt.internal.compiler.util.Util.scanTypeVariableSignature(string, start);
					buffer.append(string, start + 1, e - start - 1);
					return e;
				case Signature.C_BOOLEAN :
					buffer.append(BOOLEAN);
					return start;
				case Signature.C_BYTE :
					buffer.append(BYTE);
					return start;
				case Signature.C_CHAR :
					buffer.append(CHAR);
					return start;
				case Signature.C_DOUBLE :
					buffer.append(DOUBLE);
					return start;
				case Signature.C_FLOAT :
					buffer.append(FLOAT);
					return start;
				case Signature.C_INT :
					buffer.append(INT);
					return start;
				case Signature.C_LONG :
					buffer.append(LONG);
					return start;
				case Signature.C_SHORT :
					buffer.append(SHORT);
					return start;
				case Signature.C_VOID :
					buffer.append(VOID);
					return start;
				case Signature.C_CAPTURE :
					return appendCaptureTypeSignatureForAnchor(string, start, buffer);
				case Signature.C_STAR:
				case Signature.C_EXTENDS:
				case Signature.C_SUPER:
					return appendTypeArgumentSignatureForAnchor(string, start, buffer);
				default :
					throw newIllegalArgumentException(string, start);
			}
		}
	}

	private static int appendTypeArgumentSignatureForAnchor(char[] string, int start, StringBuffer buffer) {
		// need a minimum 1 char
		if (start >= string.length) {
			throw newIllegalArgumentException(string, start);
		}
		char c = string[start];
		switch(c) {
			case Signature.C_STAR :
				return start;
			case Signature.C_EXTENDS :
				return appendTypeSignatureForAnchor(string, start + 1, buffer, false);
			case Signature.C_SUPER :
				return appendTypeSignatureForAnchor(string, start + 1, buffer, false);
			default :
				return appendTypeSignatureForAnchor(string, start, buffer, false);
		}
	}
	private static int appendCaptureTypeSignatureForAnchor(char[] string, int start, StringBuffer buffer) {
		// need a minimum 2 char
		if (start >= string.length - 1) {
			throw newIllegalArgumentException(string, start);
		}
		char c = string[start];
		if (c != Signature.C_CAPTURE) {
			throw newUnexpectedCharacterException(string, start, c);
		}
		return appendTypeArgumentSignatureForAnchor(string, start + 1, buffer);
	}
	private static int appendArrayTypeSignatureForAnchor(char[] string, int start, StringBuffer buffer, boolean isVarArgs) {
		int length = string.length;
		// need a minimum 2 char
		if (start >= length - 1) {
			throw newIllegalArgumentException(string, start);
		}
		char c = string[start];
		if (c != Signature.C_ARRAY) {
			throw newUnexpectedCharacterException(string, start, c);
		}

		int index = start;
		c = string[++index];
		while(c == Signature.C_ARRAY) {
			// need a minimum 2 char
			if (index >= length - 1) {
				throw newIllegalArgumentException(string, start);
			}
			c = string[++index];
		}

		int e = appendTypeSignatureForAnchor(string, index, buffer, false);

		for(int i = 1, dims = index - start; i < dims; i++) {
			buffer.append('[').append(']');
		}

		if (isVarArgs) {
			buffer.append('.').append('.').append('.');
		} else {
			buffer.append('[').append(']');
		}
		return e;
	}
	private static int appendClassTypeSignatureForAnchor(char[] string, int start, StringBuffer buffer) {
		// need a minimum 3 chars "Lx;"
		if (start >= string.length - 2) {
			throw newIllegalArgumentException(string, start);
		}
		// must start in "L" or "Q"
		char c = string[start];
		if (c != Signature.C_RESOLVED && c != Signature.C_UNRESOLVED) {
			throw newUnexpectedCharacterException(string, start, c);
		}
		int p = start + 1;
		while (true) {
			if (p >= string.length) {
				throw newIllegalArgumentException(string, start);
			}
			c = string[p];
			switch(c) {
				case Signature.C_SEMICOLON :
					// all done
					return p;
				case Signature.C_GENERIC_START :
					int e = scanGenericEnd(string, p + 1);
					// once we hit type arguments there are no more package prefixes
					p = e;
					break;
				case Signature.C_DOT :
					buffer.append('.');
					break;
				 case '/' :
					buffer.append('/');
					break;
				 case Signature.C_DOLLAR :
					// once we hit "$" there are no more package prefixes
					/**
					 * Convert '$' in resolved type signatures into '.'.
					 * NOTE: This assumes that the type signature is an inner type
					 * signature. This is true in most cases, but someone can define a
					 * non-inner type name containing a '$'.
					 */
					buffer.append('.');
				 	break;
				 default :
					buffer.append(c);
			}
			p++;
		}
	}

	private static IllegalArgumentException newIllegalArgumentException(char[] string, int index) {
		return new IllegalArgumentException("\"" + String.valueOf(string) + "\" at " + index); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private static IllegalArgumentException newUnexpectedCharacterException(char[] string, int start, char unexpected) {
		return new IllegalArgumentException("Unexpected '" + unexpected + "' in \"" + String.valueOf(string) + "\" starting at " + start); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	private static int scanGenericEnd(char[] string, int start) {
		if (string[start] == Signature.C_GENERIC_END) {
			return start;
		}
		int length = string.length;
		int balance = 1;
		start++;
		while (start <= length) {
			switch(string[start]) {
				case Signature.C_GENERIC_END :
					balance--;
					if (balance == 0) {
						return start;
					}
					break;
				case Signature.C_GENERIC_START :
					balance++;
					break;
			}
			start++;
		}
		return start;
	}

	/*
	 * This method adjusts the task tags and task priorities so that they have the same size
	 */
	public static void fixTaskTags(Map defaultOptionsMap) {
		Object taskTagsValue = defaultOptionsMap.get(JavaCore.COMPILER_TASK_TAGS);
		char[][] taskTags = null;
		if (taskTagsValue instanceof String) {
			taskTags = CharOperation.splitAndTrimOn(',', ((String) taskTagsValue).toCharArray());
		}
		Object taskPrioritiesValue = defaultOptionsMap.get(JavaCore.COMPILER_TASK_PRIORITIES);
		char[][] taskPriorities = null;
		if (taskPrioritiesValue instanceof String) {
			taskPriorities = CharOperation.splitAndTrimOn(',', ((String) taskPrioritiesValue).toCharArray());
		}
		if (taskPriorities == null) {
			if (taskTags != null) {
				Util.logRepeatedMessage(TASK_PRIORITIES_PROBLEM, IStatus.ERROR, "Inconsistent values for taskTags (not null) and task priorities (null)"); //$NON-NLS-1$
				defaultOptionsMap.remove(JavaCore.COMPILER_TASK_TAGS);
			}
			return;
		} else if (taskTags == null) {
			Util.logRepeatedMessage(TASK_PRIORITIES_PROBLEM, IStatus.ERROR, "Inconsistent values for taskTags (null) and task priorities (not null)"); //$NON-NLS-1$
			defaultOptionsMap.remove(JavaCore.COMPILER_TASK_PRIORITIES);
			return;
		}
		int taskTagsLength = taskTags.length;
		int taskPrioritiesLength = taskPriorities.length;
		if (taskTagsLength != taskPrioritiesLength) {
			Util.logRepeatedMessage(TASK_PRIORITIES_PROBLEM, IStatus.ERROR, "Inconsistent values for taskTags and task priorities : length is different"); //$NON-NLS-1$
			if (taskTagsLength > taskPrioritiesLength) {
				System.arraycopy(taskTags, 0, (taskTags = new char[taskPrioritiesLength][]), 0, taskPrioritiesLength);
				defaultOptionsMap.put(JavaCore.COMPILER_TASK_TAGS, new String(CharOperation.concatWith(taskTags,',')));
			} else {
				System.arraycopy(taskPriorities, 0, (taskPriorities = new char[taskTagsLength][]), 0, taskTagsLength);
				defaultOptionsMap.put(JavaCore.COMPILER_TASK_PRIORITIES, new String(CharOperation.concatWith(taskPriorities,',')));
			}
		}
	}
	/**
	 * Finds the IMethod element corresponding to the given selector,
	 * without creating a new dummy instance of a binary method.
	 * @param type the type in which the method is declared
	 * @param selector the method name
	 * @param paramTypeSignatures the type signatures of the method arguments
	 * @param isConstructor whether we're looking for a constructor
	 * @return an IMethod if found, otherwise null
	 * @throws JavaModelException
	 */
	public static IMethod findMethod(IType type, char[] selector, String[] paramTypeSignatures, boolean isConstructor) throws JavaModelException {
		IMethod method = null;
		int startingIndex = 0;
		String[] args;
		IType enclosingType = type.getDeclaringType();
		// If the method is a constructor of a non-static inner type, add the enclosing type as an
		// additional parameter to the constructor
		if (enclosingType != null
				&& isConstructor
				&& !Flags.isStatic(type.getFlags())) {
			args = new String[paramTypeSignatures.length+1];
			startingIndex = 1;
			args[0] = Signature.createTypeSignature(enclosingType.getFullyQualifiedName(), true);
		} else {
			args = new String[paramTypeSignatures.length];
		}
		int length = args.length;
		for(int i = startingIndex;	i< length ; i++){
			args[i] = paramTypeSignatures[i-startingIndex];
		}
		method = type.getMethod(new String(selector), args);

		IMethod[] methods = type.findMethods(method);
		if (methods != null && methods.length > 0) {
			method = methods[0];
		}
		return method;
	}
}
