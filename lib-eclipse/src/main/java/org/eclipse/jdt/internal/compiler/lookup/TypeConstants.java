/*******************************************************************************
 * Copyright (c) 2000, 2022 IBM Corporation and others.
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
 *     Stephan Herrmann - Contributions for
 *								bug 349326 - [1.7] new warning for missing try-with-resources
 *								bug 358903 - Filter practically unimportant resource leak warnings
 *								bug 381445 - [compiler][resource] Can the resource leak check be made aware of Closeables.closeQuietly?
 *								bug 400421 - [compiler] Null analysis for fields does not take @com.google.inject.Inject into account
 *								bug 382069 - [null] Make the null analysis consider JUnit's assertNotNull similarly to assertions
 *								Bug 405569 - Resource leak check false positive when using DbUtils.closeQuietly
 *								Bug 427199 - [1.8][resource] avoid resource leak warnings on Streams that have no resource
 *								Bug 425183 - [1.8][inference] make CaptureBinding18 safe
 *								Bug 429958 - [1.8][null] evaluate new DefaultLocation attribute of @NonNullByDefault
 *								Bug 410218 - Optional warning for arguments of "unexpected" types to Map#get(Object), Collection#remove(Object) et al.
 *    Jesper S Moller - Contributions for
 *								Bug 405066 - [1.8][compiler][codegen] Implement code generation infrastructure for JSR335
 *								Bug 412153 - [1.8][compiler] Check validity of annotations which may be repeatable
 *								bug 527554 - [18.3] Compiler support for JEP 286 Local-Variable Type
 *    Andy Clement (GoPivotal, Inc) aclement@gopivotal.com - Contributions for
 *                              Bug 405104 - [1.8][compiler][codegen] Implement support for serializeable lambdas
 *    Ulrich Grave <ulrich.grave@gmx.de> - Contributions for
 *                              bug 386692 - Missing "unused" warning on "autowired" fields
 *    Pierre-Yves B. <pyvesdev@gmail.com> - Contribution for
 *                              bug 542520 - [JUnit 5] Warning The method xxx from the type X is never used locally is shown when using MethodSource
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.lookup;

import org.eclipse.jdt.core.compiler.CharOperation;

// TODO should rename into TypeNames (once extracted last non name constants)
public interface TypeConstants {

	char[] JAVA = "java".toCharArray(); //$NON-NLS-1$
	char[] JAVAC = "javac".toCharArray(); //$NON-NLS-1$
	char[] JAVAX = "javax".toCharArray(); //$NON-NLS-1$
	char[] LANG = "lang".toCharArray(); //$NON-NLS-1$
	char[] IO = "io".toCharArray(); //$NON-NLS-1$
	char[] NIO = "nio".toCharArray(); //$NON-NLS-1$
	char[] UTIL = "util".toCharArray(); //$NON-NLS-1$
	char[] ZIP = "zip".toCharArray(); //$NON-NLS-1$
	char[] JDK = "jdk".toCharArray(); //$NON-NLS-1$
	char[] ANNOTATION = "annotation".toCharArray(); //$NON-NLS-1$
	char[] REFLECT = "reflect".toCharArray(); //$NON-NLS-1$
	char[] LENGTH = "length".toCharArray(); //$NON-NLS-1$
	char[] CLONE = "clone".toCharArray(); //$NON-NLS-1$
	char[] EQUALS = "equals".toCharArray(); //$NON-NLS-1$
	char[] GETCLASS = "getClass".toCharArray(); //$NON-NLS-1$
	char[] HASHCODE = "hashCode".toCharArray(); //$NON-NLS-1$
	char[] TOSTRING = "toString".toCharArray(); //$NON-NLS-1$
	char[] OBJECT = "Object".toCharArray(); //$NON-NLS-1$
	char[] MAIN = "main".toCharArray(); //$NON-NLS-1$
	char[] SERIALVERSIONUID = "serialVersionUID".toCharArray(); //$NON-NLS-1$
	char[] SERIALPERSISTENTFIELDS = "serialPersistentFields".toCharArray(); //$NON-NLS-1$
	char[] READRESOLVE = "readResolve".toCharArray(); //$NON-NLS-1$
	char[] WRITEREPLACE = "writeReplace".toCharArray(); //$NON-NLS-1$
	char[] READOBJECT = "readObject".toCharArray(); //$NON-NLS-1$
	char[] WRITEOBJECT = "writeObject".toCharArray(); //$NON-NLS-1$
	char[] CharArray_JAVA_LANG_OBJECT = "java.lang.Object".toCharArray(); //$NON-NLS-1$
	char[] CharArray_JAVA_LANG_ENUM = "java.lang.Enum".toCharArray(); //$NON-NLS-1$
	char[] CharArray_JAVA_LANG_RECORD = "java.lang.Record".toCharArray(); //$NON-NLS-1$
	char[] CharArray_JAVA_LANG_RECORD_SLASH = "java/lang/Record".toCharArray(); //$NON-NLS-1$
	char[] CharArray_JAVA_LANG_ANNOTATION_ANNOTATION = "java.lang.annotation.Annotation".toCharArray(); //$NON-NLS-1$
	char[] CharArray_JAVA_IO_OBJECTINPUTSTREAM = "java.io.ObjectInputStream".toCharArray(); //$NON-NLS-1$
	char[] CharArray_JAVA_IO_OBJECTOUTPUTSTREAM = "java.io.ObjectOutputStream".toCharArray(); //$NON-NLS-1$
	char[] CharArray_JAVA_IO_OBJECTSTREAMFIELD = "java.io.ObjectStreamField".toCharArray(); //$NON-NLS-1$
	char[] ANONYM_PREFIX = "new ".toCharArray(); //$NON-NLS-1$
	char[] ANONYM_SUFFIX = "(){}".toCharArray(); //$NON-NLS-1$
    char[] WILDCARD_NAME = { '?' };
    char[] WILDCARD_SUPER = " super ".toCharArray(); //$NON-NLS-1$
    char[] WILDCARD_EXTENDS = " extends ".toCharArray(); //$NON-NLS-1$
    char[] WILDCARD_MINUS = { '-' };
    char[] WILDCARD_STAR = { '*' };
    char[] WILDCARD_PLUS = { '+' };
    char[] WILDCARD_CAPTURE_NAME_PREFIX = "capture#".toCharArray(); //$NON-NLS-1$
    char[] WILDCARD_CAPTURE_NAME_SUFFIX = "-of ".toCharArray(); //$NON-NLS-1$
    char[] WILDCARD_CAPTURE_SIGNABLE_NAME_SUFFIX = "capture-of ".toCharArray(); //$NON-NLS-1$
	char[] WILDCARD_CAPTURE = { '!' };
	char[] CAPTURE18 = { '^' };
	char[] BYTE = "byte".toCharArray(); //$NON-NLS-1$
	char[] SHORT = "short".toCharArray(); //$NON-NLS-1$
	char[] INT = "int".toCharArray(); //$NON-NLS-1$
	char[] LONG = "long".toCharArray(); //$NON-NLS-1$
	char[] FLOAT = "float".toCharArray(); //$NON-NLS-1$
	char[] DOUBLE = "double".toCharArray(); //$NON-NLS-1$
	char[] CHAR = "char".toCharArray(); //$NON-NLS-1$
	char[] BOOLEAN = "boolean".toCharArray(); //$NON-NLS-1$
	char[] NULL = "null".toCharArray(); //$NON-NLS-1$
	char[] VOID = "void".toCharArray(); //$NON-NLS-1$
    char[] VALUE = "value".toCharArray(); //$NON-NLS-1$
    char[] VALUES = "values".toCharArray(); //$NON-NLS-1$
    char[] VALUEOF = "valueOf".toCharArray(); //$NON-NLS-1$
    char[] UPPER_SOURCE = "SOURCE".toCharArray(); //$NON-NLS-1$
    char[] UPPER_CLASS = "CLASS".toCharArray(); //$NON-NLS-1$
    char[] UPPER_RUNTIME = "RUNTIME".toCharArray(); //$NON-NLS-1$
	char[] ANNOTATION_PREFIX = "@".toCharArray(); //$NON-NLS-1$
	char[] ANNOTATION_SUFFIX = "()".toCharArray(); //$NON-NLS-1$
    char[] TYPE = "TYPE".toCharArray(); //$NON-NLS-1$
    char[] UPPER_FIELD = "FIELD".toCharArray(); //$NON-NLS-1$
    char[] UPPER_METHOD = "METHOD".toCharArray(); //$NON-NLS-1$
    char[] UPPER_PARAMETER = "PARAMETER".toCharArray(); //$NON-NLS-1$
    char[] UPPER_CONSTRUCTOR = "CONSTRUCTOR".toCharArray(); //$NON-NLS-1$
    char[] UPPER_LOCAL_VARIABLE = "LOCAL_VARIABLE".toCharArray(); //$NON-NLS-1$
    char[] UPPER_ANNOTATION_TYPE = "ANNOTATION_TYPE".toCharArray(); //$NON-NLS-1$
    char[] UPPER_PACKAGE = "PACKAGE".toCharArray(); //$NON-NLS-1$
    char[] ANONYMOUS_METHOD = "lambda$".toCharArray(); //$NON-NLS-1$
    char[] DESERIALIZE_LAMBDA = "$deserializeLambda$".toCharArray(); //$NON-NLS-1$
    char[] LAMBDA_TYPE = "<lambda>".toCharArray(); //$NON-NLS-1$
    char[] UPPER_MODULE = "MODULE".toCharArray(); //$NON-NLS-1$
    char[] UPPER_RECORD_COMPONENT = "RECORD_COMPONENT".toCharArray(); //$NON-NLS-1$
    char[] YIELD = "yield".toCharArray(); //$NON-NLS-1$

    // JEP 286
	char[] VAR = "var".toCharArray(); //$NON-NLS-1$
    // JEP 359 Records
	char[] RECORD_RESTRICTED_IDENTIFIER = "record".toCharArray(); //$NON-NLS-1$
	char[] RECORD_CLASS = "Record".toCharArray(); //$NON-NLS-1$
	// JEP 360 Sealed
	char[] PERMITS = "permits".toCharArray(); //$NON-NLS-1$
	char[] SEALED = "sealed".toCharArray(); //$NON-NLS-1$
	String KEYWORD_EXTENDS = "extends"; //$NON-NLS-1$
	String IMPLEMENTS = "implements"; //$NON-NLS-1$

    // jsr308
	char[] TYPE_USE_TARGET  = "TYPE_USE".toCharArray(); //$NON-NLS-1$
	char[] TYPE_PARAMETER_TARGET = "TYPE_PARAMETER".toCharArray(); //$NON-NLS-1$

    // common 3rd party package components:
    char[] ORG = "org".toCharArray(); //$NON-NLS-1$
    char[] ECLIPSE = "eclipse".toCharArray(); //$NON-NLS-1$
    char[] CORE = "core".toCharArray(); //$NON-NLS-1$
    char[] RUNTIME = "runtime".toCharArray(); //$NON-NLS-1$
    char[] APACHE = "apache".toCharArray(); //$NON-NLS-1$
    char[] COMMONS = "commons".toCharArray(); //$NON-NLS-1$
    char[] LANG3 = "lang3".toCharArray(); //$NON-NLS-1$
    char[] COM = "com".toCharArray(); //$NON-NLS-1$
    char[] GOOGLE = "google".toCharArray(); //$NON-NLS-1$
    char[] JDT = "jdt".toCharArray(); //$NON-NLS-1$
    char[] INTERNAL = "internal".toCharArray(); //$NON-NLS-1$
    char[] COMPILER = "compiler".toCharArray(); //$NON-NLS-1$
    char[] LOOKUP = "lookup".toCharArray(); //$NON-NLS-1$
    char[] TYPEBINDING = "TypeBinding".toCharArray(); //$NON-NLS-1$
    char[] DOM = "dom".toCharArray(); //$NON-NLS-1$
    char[] ITYPEBINDING = "ITypeBinding".toCharArray(); //$NON-NLS-1$
    char[] SPRING = "springframework".toCharArray(); //$NON-NLS-1$

	// Constant compound names
	char[][] JAVA_LANG = {JAVA, LANG};
	char[][] JAVA_IO = {JAVA, IO};
	char[][] JAVA_LANG_ANNOTATION = {JAVA, LANG, ANNOTATION};
	char[][] JAVA_LANG_ANNOTATION_ANNOTATION = {JAVA, LANG, ANNOTATION, "Annotation".toCharArray()}; //$NON-NLS-1$
	char[][] JAVA_LANG_ASSERTIONERROR = {JAVA, LANG, "AssertionError".toCharArray()}; //$NON-NLS-1$
	char[][] JAVA_LANG_CLASS = {JAVA, LANG, "Class".toCharArray()}; //$NON-NLS-1$
	char[][] JAVA_LANG_CLASSNOTFOUNDEXCEPTION = {JAVA, LANG, "ClassNotFoundException".toCharArray()}; //$NON-NLS-1$
	char[][] JAVA_LANG_NOSUCHFIELDERROR = {JAVA, LANG, "NoSuchFieldError".toCharArray()}; //$NON-NLS-1$
	char[][] JAVA_LANG_CLONEABLE = {JAVA, LANG, "Cloneable".toCharArray()}; //$NON-NLS-1$
	char[][] JAVA_LANG_ENUM = {JAVA, LANG, "Enum".toCharArray()}; //$NON-NLS-1$
	char[][] JAVA_LANG_EXCEPTION = {JAVA, LANG, "Exception".toCharArray()}; //$NON-NLS-1$
	char[][] JAVA_LANG_ERROR = {JAVA, LANG, "Error".toCharArray()}; //$NON-NLS-1$
	char[][] JAVA_LANG_ILLEGALARGUMENTEXCEPTION = {JAVA, LANG, "IllegalArgumentException".toCharArray()}; //$NON-NLS-1$
	char[][] JAVA_LANG_INCOMPATIBLECLASSCHANGEERROR = {JAVA, LANG, "IncompatibleClassChangeError".toCharArray()}; //$NON-NLS-1$
	char[][] JAVA_LANG_ITERABLE = {JAVA, LANG, "Iterable".toCharArray()}; //$NON-NLS-1$
	char[][] JAVA_LANG_NOCLASSDEFERROR = {JAVA, LANG, "NoClassDefError".toCharArray()}; //$NON-NLS-1$
	char[][] JAVA_LANG_OBJECT = {JAVA, LANG, OBJECT};
	char[][] JAVA_LANG_RECORD = {JAVA, LANG, RECORD_CLASS};
	char[][] JAVA_LANG_STRING = {JAVA, LANG, "String".toCharArray()}; //$NON-NLS-1$
	char[][] JAVA_LANG_STRINGBUFFER = {JAVA, LANG, "StringBuffer".toCharArray()}; //$NON-NLS-1$
	char[][] JAVA_LANG_STRINGBUILDER = {JAVA, LANG, "StringBuilder".toCharArray()}; //$NON-NLS-1$
	char[][] JAVA_LANG_SYSTEM = {JAVA, LANG, "System".toCharArray()}; //$NON-NLS-1$
	char[][] JAVA_LANG_RUNTIMEEXCEPTION = {JAVA, LANG, "RuntimeException".toCharArray()}; //$NON-NLS-1$
	char[][] JAVA_LANG_THROWABLE = {JAVA, LANG, "Throwable".toCharArray()}; //$NON-NLS-1$
	char[][] JAVA_LANG_REFLECT_CONSTRUCTOR = {JAVA, LANG, REFLECT, "Constructor".toCharArray()}; //$NON-NLS-1$
	char[][] JAVA_IO_PRINTSTREAM = {JAVA, IO, "PrintStream".toCharArray()}; //$NON-NLS-1$
	char[][] JAVA_IO_SERIALIZABLE = {JAVA, IO, "Serializable".toCharArray()}; //$NON-NLS-1$
	char[][] JAVA_LANG_BYTE = {JAVA, LANG, "Byte".toCharArray()}; //$NON-NLS-1$
	char[][] JAVA_LANG_SHORT = {JAVA, LANG, "Short".toCharArray()}; //$NON-NLS-1$
	char[][] JAVA_LANG_CHARACTER = {JAVA, LANG, "Character".toCharArray()}; //$NON-NLS-1$
	char[][] JAVA_LANG_INTEGER = {JAVA, LANG, "Integer".toCharArray()}; //$NON-NLS-1$
	char[][] JAVA_LANG_LONG = {JAVA, LANG, "Long".toCharArray()}; //$NON-NLS-1$
	char[][] JAVA_LANG_FLOAT = {JAVA, LANG, "Float".toCharArray()}; //$NON-NLS-1$
	char[][] JAVA_LANG_DOUBLE = {JAVA, LANG, "Double".toCharArray()}; //$NON-NLS-1$
	char[][] JAVA_LANG_BOOLEAN = {JAVA, LANG, "Boolean".toCharArray()}; //$NON-NLS-1$
	char[][] JAVA_LANG_VOID = {JAVA, LANG, "Void".toCharArray()}; //$NON-NLS-1$
	char[][] JAVA_UTIL_COLLECTION = {JAVA, UTIL, "Collection".toCharArray()}; //$NON-NLS-1$
	char[][] JAVA_UTIL_ITERATOR = {JAVA, UTIL, "Iterator".toCharArray()}; //$NON-NLS-1$
	char[] NEXT = "next".toCharArray(); //$NON-NLS-1$
	char[][] JAVA_UTIL_OBJECTS = {JAVA, UTIL, "Objects".toCharArray()}; //$NON-NLS-1$
	char[][] JAVA_UTIL_LIST = {JAVA, UTIL, "List".toCharArray()}; //$NON-NLS-1$
	char[][] JAVA_UTIL_ARRAYS = {JAVA, UTIL, "Arrays".toCharArray()}; //$NON-NLS-1$
	char[][] JAVA_LANG_DEPRECATED = {JAVA, LANG, "Deprecated".toCharArray()}; //$NON-NLS-1$
	char[] FOR_REMOVAL = "forRemoval".toCharArray(); //$NON-NLS-1$
	char[] SINCE = "since".toCharArray(); //$NON-NLS-1$
	char[] ESSENTIAL_API = "essentialAPI".toCharArray(); //$NON-NLS-1$
	char[][] JAVA_LANG_ANNOTATION_DOCUMENTED = {JAVA, LANG, ANNOTATION, "Documented".toCharArray()}; //$NON-NLS-1$
	char[][] JAVA_LANG_ANNOTATION_INHERITED = {JAVA, LANG, ANNOTATION, "Inherited".toCharArray()}; //$NON-NLS-1$
	char[][] JAVA_LANG_ANNOTATION_REPEATABLE = {JAVA, LANG, ANNOTATION, "Repeatable".toCharArray()}; //$NON-NLS-1$
	char[][] JAVA_LANG_OVERRIDE = {JAVA, LANG, "Override".toCharArray()}; //$NON-NLS-1$
	char[][] JAVA_LANG_FUNCTIONAL_INTERFACE = {JAVA, LANG, "FunctionalInterface".toCharArray()}; //$NON-NLS-1$
	char[][] JAVA_LANG_ANNOTATION_RETENTION = {JAVA, LANG, ANNOTATION, "Retention".toCharArray()}; //$NON-NLS-1$
	char[][] JAVA_LANG_SUPPRESSWARNINGS = {JAVA, LANG, "SuppressWarnings".toCharArray()}; //$NON-NLS-1$
	char[][] JAVA_LANG_ANNOTATION_TARGET = {JAVA, LANG, ANNOTATION, "Target".toCharArray()}; //$NON-NLS-1$
	char[][] JAVA_LANG_ANNOTATION_RETENTIONPOLICY = {JAVA, LANG, ANNOTATION, "RetentionPolicy".toCharArray()}; //$NON-NLS-1$
	char[][] JAVA_LANG_ANNOTATION_ELEMENTTYPE = {JAVA, LANG, ANNOTATION, "ElementType".toCharArray()}; //$NON-NLS-1$
	char[][] JDK_INTERNAL_PREVIEW_FEATURE = {JDK, INTERNAL, "PreviewFeature".toCharArray()}; //$NON-NLS-1$
	char[][] JDK_INTERNAL_JAVAC_PREVIEW_FEATURE = {JDK, INTERNAL, JAVAC, "PreviewFeature".toCharArray()}; //$NON-NLS-1$
	char[][] JAVA_LANG_REFLECT_FIELD = new char[][] {JAVA, LANG, REFLECT, "Field".toCharArray()}; //$NON-NLS-1$
	char[][] JAVA_LANG_REFLECT_METHOD = new char[][] {JAVA, LANG, REFLECT, "Method".toCharArray()}; //$NON-NLS-1$
	char[][] JAVA_IO_CLOSEABLE = new char[][] { JAVA, IO, "Closeable".toCharArray()};//$NON-NLS-1$
	char[][] JAVA_IO_OBJECTSTREAMEXCEPTION = new char[][] { JAVA, IO, "ObjectStreamException".toCharArray()};//$NON-NLS-1$
	char[][] JAVA_IO_EXTERNALIZABLE = {JAVA, IO, "Externalizable".toCharArray()}; //$NON-NLS-1$
	char[][] JAVA_IO_IOEXCEPTION = new char[][] { JAVA, IO, "IOException".toCharArray()};//$NON-NLS-1$
	char[][] JAVA_IO_OBJECTOUTPUTSTREAM = new char[][] { JAVA, IO, "ObjectOutputStream".toCharArray()}; //$NON-NLS-1$
	char[][] JAVA_IO_OBJECTINPUTSTREAM = new char[][] { JAVA, IO, "ObjectInputStream".toCharArray()}; //$NON-NLS-1$
	char[][] JAVA_NIO_FILE_FILES = new char[][] { JAVA, "nio".toCharArray(), "file".toCharArray(), "Files".toCharArray() };   //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
	// javax.rmi.CORBA.Stub
	char[][] JAVAX_RMI_CORBA_STUB = new char[][] {
			JAVAX,
			"rmi".toCharArray(), //$NON-NLS-1$
			"CORBA".toCharArray(), //$NON-NLS-1$
			"Stub".toCharArray(), //$NON-NLS-1$
	};
	char[][] JAVA_LANG_SAFEVARARGS =  {JAVA, LANG, "SafeVarargs".toCharArray()}; //$NON-NLS-1$
	char[] INVOKE = "invoke".toCharArray(); //$NON-NLS-1$
	char[][] JAVA_LANG_INVOKE_METHODHANDLE_POLYMORPHICSIGNATURE = { // Signature while parsing binary file
			JAVA,
			LANG,
			INVOKE,
			"MethodHandle".toCharArray(), //$NON-NLS-1$
			"PolymorphicSignature".toCharArray() //$NON-NLS-1$
	};
	char[][] JAVA_LANG_INVOKE_METHODHANDLE_$_POLYMORPHICSIGNATURE = { // Signature while parsing source file
			JAVA,
			LANG,
			INVOKE,
			"MethodHandle$PolymorphicSignature".toCharArray() //$NON-NLS-1$
	};
	char[][] JAVA_LANG_INVOKE_LAMBDAMETAFACTORY = {JAVA, LANG, INVOKE, "LambdaMetafactory".toCharArray()}; //$NON-NLS-1$
	char[][] JAVA_LANG_INVOKE_SERIALIZEDLAMBDA = {JAVA, LANG, INVOKE, "SerializedLambda".toCharArray()}; //$NON-NLS-1$
	char[][] JAVA_LANG_INVOKE_METHODHANDLES = {JAVA, LANG, INVOKE, "MethodHandles".toCharArray()}; //$NON-NLS-1$
	char[][] JAVA_LANG_AUTOCLOSEABLE =  {JAVA, LANG, "AutoCloseable".toCharArray()}; //$NON-NLS-1$
	char[] CLOSE = "close".toCharArray(); //$NON-NLS-1$
	char[][] JAVA_LANG_RUNTIME_OBJECTMETHODS = {JAVA, LANG, RUNTIME, "ObjectMethods".toCharArray()}; //$NON-NLS-1$
	char[][] JAVA_LANG_RUNTIME_SWITCHBOOTSTRAPS = {JAVA, LANG, RUNTIME, "SwitchBootstraps".toCharArray()}; //$NON-NLS-1$
	// known helper functions for closing a Closeable (all receive a Closeable as their first argument):
	public static class CloseMethodRecord {
		public char[][] typeName;
		public char[] selector;
		public int numCloseableArgs;
		public CloseMethodRecord(char[][] typeName, char[] selector, int num) {
			this.typeName = typeName;
			this.selector = selector;
			this.numCloseableArgs = num;
		}
	}
	char[][] GUAVA_CLOSEABLES = { COM, GOOGLE, "common".toCharArray(), IO, "Closeables".toCharArray() }; //$NON-NLS-1$ //$NON-NLS-2$
	char[][] APACHE_IOUTILS = { ORG, APACHE, COMMONS, IO, "IOUtils".toCharArray() }; //$NON-NLS-1$
	char[][] APACHE_DBUTILS = { ORG, APACHE, COMMONS, "dbutils".toCharArray(), "DbUtils".toCharArray() }; //$NON-NLS-1$ //$NON-NLS-2$
	char[] CLOSE_QUIETLY = "closeQuietly".toCharArray(); //$NON-NLS-1$
	CloseMethodRecord[] closeMethods = new CloseMethodRecord[] {
		new CloseMethodRecord(GUAVA_CLOSEABLES, CLOSE_QUIETLY, 1),
		new CloseMethodRecord(GUAVA_CLOSEABLES, CLOSE, 1),
		new CloseMethodRecord(APACHE_IOUTILS, CLOSE_QUIETLY, 1),
		new CloseMethodRecord(APACHE_DBUTILS, CLOSE, 1),
		new CloseMethodRecord(APACHE_DBUTILS, CLOSE_QUIETLY, 3), // closeQuietly(Connection,Statement,ResultSet)
		new CloseMethodRecord(APACHE_DBUTILS, "commitAndClose".toCharArray(), 1), //$NON-NLS-1$
		new CloseMethodRecord(APACHE_DBUTILS, "commitAndCloseQuietly".toCharArray(), 1), //$NON-NLS-1$
		new CloseMethodRecord(APACHE_DBUTILS, "rollbackAndClose".toCharArray(), 1), //$NON-NLS-1$
		new CloseMethodRecord(APACHE_DBUTILS, "rollbackAndCloseQuietly".toCharArray(), 1), //$NON-NLS-1$
	};
	// white lists of closeables:
	char[][] JAVA_IO_WRAPPER_CLOSEABLES = new char[][] {
		"BufferedInputStream".toCharArray(), //$NON-NLS-1$
		"BufferedOutputStream".toCharArray(), //$NON-NLS-1$
		"BufferedReader".toCharArray(), //$NON-NLS-1$
		"BufferedWriter".toCharArray(), //$NON-NLS-1$
		"InputStreamReader".toCharArray(), //$NON-NLS-1$
		"PrintWriter".toCharArray(),  //$NON-NLS-1$
		"LineNumberReader".toCharArray(), //$NON-NLS-1$
		"DataInputStream".toCharArray(), //$NON-NLS-1$
		"DataOutputStream".toCharArray(), //$NON-NLS-1$
		"ObjectInputStream".toCharArray(), //$NON-NLS-1$
		"ObjectOutputStream".toCharArray(), //$NON-NLS-1$
		"FilterInputStream".toCharArray(), //$NON-NLS-1$
		"FilterOutputStream".toCharArray(), //$NON-NLS-1$
		"DataInputStream".toCharArray(), //$NON-NLS-1$
		"DataOutputStream".toCharArray(), //$NON-NLS-1$
		"PushbackInputStream".toCharArray(), //$NON-NLS-1$
		"SequenceInputStream".toCharArray(), //$NON-NLS-1$
		"PrintStream".toCharArray(), //$NON-NLS-1$
		"PushbackReader".toCharArray(), //$NON-NLS-1$
		"OutputStreamWriter".toCharArray(), //$NON-NLS-1$
	};
	char[][] JAVA_UTIL_ZIP_WRAPPER_CLOSEABLES = new char[][] {
		"GZIPInputStream".toCharArray(), //$NON-NLS-1$
		"InflaterInputStream".toCharArray(), //$NON-NLS-1$
		"DeflaterInputStream".toCharArray(), //$NON-NLS-1$
		"CheckedInputStream".toCharArray(), //$NON-NLS-1$
		"ZipInputStream".toCharArray(), //$NON-NLS-1$
		"JarInputStream".toCharArray(), //$NON-NLS-1$
		"GZIPOutputStream".toCharArray(), //$NON-NLS-1$
		"InflaterOutputStream".toCharArray(), //$NON-NLS-1$
		"DeflaterOutputStream".toCharArray(), //$NON-NLS-1$
		"CheckedOutputStream".toCharArray(), //$NON-NLS-1$
		"ZipOutputStream".toCharArray(), //$NON-NLS-1$
		"JarOutputStream".toCharArray(), //$NON-NLS-1$
	};
	char[][][] OTHER_WRAPPER_CLOSEABLES = new char[][][] {
		{JAVA, "security".toCharArray(), "DigestInputStream".toCharArray()}, //$NON-NLS-1$ //$NON-NLS-2$
		{JAVA, "security".toCharArray(), "DigestOutputStream".toCharArray()}, //$NON-NLS-1$ //$NON-NLS-2$
		{JAVA, "beans".toCharArray(), "XMLEncoder".toCharArray()}, //$NON-NLS-1$ //$NON-NLS-2$
		{JAVA, "beans".toCharArray(), "XMLDecoder".toCharArray()}, //$NON-NLS-1$ //$NON-NLS-2$
		{JAVAX, "sound".toCharArray(), "sampled".toCharArray(), "AudioInputStream".toCharArray()}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	};
	char[][] JAVA_IO_RESOURCE_FREE_CLOSEABLES = new char[][] {
		"StringReader".toCharArray(), //$NON-NLS-1$
		"StringWriter".toCharArray(), //$NON-NLS-1$
		"ByteArrayInputStream".toCharArray(), //$NON-NLS-1$
		"ByteArrayOutputStream".toCharArray(), //$NON-NLS-1$
		"CharArrayReader".toCharArray(), //$NON-NLS-1$
		"CharArrayWriter".toCharArray(), //$NON-NLS-1$
		"StringBufferInputStream".toCharArray(), //$NON-NLS-1$
	};
	char[][] JAVA_UTIL_STREAM = { JAVA, UTIL, "stream".toCharArray() }; //$NON-NLS-1$
	char[][] RESOURCE_FREE_CLOSEABLE_J_U_STREAMS = {
		"Stream".toCharArray(), //$NON-NLS-1$
		"DoubleStream".toCharArray(), //$NON-NLS-1$
		"LongStream".toCharArray(), //$NON-NLS-1$
		"IntStream".toCharArray() //$NON-NLS-1$
	};
	char[][] ONE_UTIL_STREAMEX = { "one".toCharArray(), UTIL, "streamex".toCharArray() }; //$NON-NLS-1$ //$NON-NLS-2$
	char[][] RESOURCE_FREE_CLOSEABLE_STREAMEX = {
		"StreamEx".toCharArray(), //$NON-NLS-1$
		"IntStreamEx".toCharArray(), //$NON-NLS-1$
		"DoubleStreamEx".toCharArray(), //$NON-NLS-1$
		"LongStreamEx".toCharArray(), //$NON-NLS-1$
		"EntryStream".toCharArray() //$NON-NLS-1$
	};

	// closeable classes containing one or more 'fluent' methods (returning 'this'):
	char[] CHANNELS = "channels".toCharArray(); //$NON-NLS-1$
	char[][][] FLUENT_RESOURCE_CLASSES = {
		new char[][] {JAVA, IO, "CharArrayWriter".toCharArray() }, //$NON-NLS-1$
		new char[][] {JAVA, IO, "Console".toCharArray() }, //$NON-NLS-1$
		new char[][] {JAVA, IO, "PrintStream".toCharArray() }, //$NON-NLS-1$
		new char[][] {JAVA, IO, "PrintWriter".toCharArray() }, //$NON-NLS-1$
		new char[][] {JAVA, IO, "StringWriter".toCharArray() }, //$NON-NLS-1$
		new char[][] {JAVA, IO, "Writer".toCharArray() }, //$NON-NLS-1$
		new char[][] {JAVA, NIO, CHANNELS, "AsynchronousFileChannel".toCharArray() }, //$NON-NLS-1$
		new char[][] {JAVA, NIO, CHANNELS, "AsynchronousServerSocketChannel".toCharArray() }, //$NON-NLS-1$
		new char[][] {JAVA, NIO, CHANNELS, "FileChannel".toCharArray() }, //$NON-NLS-1$
		new char[][] {JAVA, NIO, CHANNELS, "NetworkChannel".toCharArray() }, //$NON-NLS-1$
		new char[][] {JAVA, NIO, CHANNELS, "SeekableByteChannel".toCharArray() }, //$NON-NLS-1$
		new char[][] {JAVA, NIO, CHANNELS, "SelectableChannel".toCharArray() }, //$NON-NLS-1$
		new char[][] {JAVA, NIO, CHANNELS, "Selector".toCharArray() }, //$NON-NLS-1$
		new char[][] {JAVA, NIO, CHANNELS, "ServerSocketChannel".toCharArray() }, //$NON-NLS-1$
		new char[][] {JAVA, UTIL, "Formatter".toCharArray() }, //$NON-NLS-1$
		new char[][] {JAVA, UTIL, "Scanner".toCharArray() }, //$NON-NLS-1$
	};

	// different assertion utilities:
	char[] ASSERT_CLASS = "Assert".toCharArray(); //$NON-NLS-1$
	char[] ASSERTIONS_CLASS = "Assertions".toCharArray(); //$NON-NLS-1$
	char[][] ORG_ECLIPSE_CORE_RUNTIME_ASSERT = new char[][] { ORG, ECLIPSE, CORE, RUNTIME, ASSERT_CLASS };
	// ... methods:
	char[] IS_NOTNULL = "isNotNull".toCharArray(); //$NON-NLS-1$

	char[] JUNIT = "junit".toCharArray(); //$NON-NLS-1$
	char[] FRAMEWORK = "framework".toCharArray(); //$NON-NLS-1$
	char[] JUPITER = "jupiter".toCharArray(); //$NON-NLS-1$
	char[] PARAMS = "params".toCharArray(); //$NON-NLS-1$
	char[] PROVIDER = "provider".toCharArray(); //$NON-NLS-1$
	char[] API = "api".toCharArray(); //$NON-NLS-1$
	char[][] JUNIT_FRAMEWORK_ASSERT = new char[][] { JUNIT, FRAMEWORK, ASSERT_CLASS };
	char[][] ORG_JUNIT_ASSERT = new char[][] { ORG, JUNIT, ASSERT_CLASS };
	char[][] ORG_JUNIT_JUPITER_API_ASSERTIONS = new char[][] { ORG, JUNIT, JUPITER, API, ASSERTIONS_CLASS };
	// ... methods:
	char[] ASSERT_NULL = "assertNull".toCharArray(); //$NON-NLS-1$
	char[] ASSERT_NOTNULL = "assertNotNull".toCharArray(); //$NON-NLS-1$
	char[] ASSERT_TRUE = "assertTrue".toCharArray(); //$NON-NLS-1$
	char[] ASSERT_FALSE = "assertFalse".toCharArray(); //$NON-NLS-1$
	// ... annotations:
	char[] METHOD_SOURCE = "MethodSource".toCharArray(); //$NON-NLS-1$
	char[][] ORG_JUNIT_METHOD_SOURCE = new char[][] { ORG, JUNIT, JUPITER, PARAMS, PROVIDER, METHOD_SOURCE };

	char[] VALIDATE_CLASS = "Validate".toCharArray(); //$NON-NLS-1$
	char[][] ORG_APACHE_COMMONS_LANG_VALIDATE = new char[][] { ORG, APACHE, COMMONS, LANG, VALIDATE_CLASS };
	char[][] ORG_APACHE_COMMONS_LANG3_VALIDATE = new char[][] { ORG, APACHE, COMMONS, LANG3, VALIDATE_CLASS };
	char[][] ORG_ECLIPSE_JDT_INTERNAL_COMPILER_LOOKUP_TYPEBINDING = new char[][] { ORG, ECLIPSE, JDT, INTERNAL, COMPILER, LOOKUP, TYPEBINDING };
	char[][] ORG_ECLIPSE_JDT_CORE_DOM_ITYPEBINDING = new char[][] { ORG, ECLIPSE, JDT, CORE, DOM, ITYPEBINDING };

	// ... methods:
	char[] IS_TRUE = "isTrue".toCharArray(); //$NON-NLS-1$
	char[] NOT_NULL = "notNull".toCharArray(); //$NON-NLS-1$

	char[][] COM_GOOGLE_COMMON_BASE_PRECONDITIONS = new char[][] {
			COM, GOOGLE, "common".toCharArray(), "base".toCharArray(), "Preconditions".toCharArray() }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	// ... methods:
	char[] CHECK_NOT_NULL = "checkNotNull".toCharArray(); //$NON-NLS-1$
	char[] CHECK_ARGUMENT = "checkArgument".toCharArray(); //$NON-NLS-1$
	char[] CHECK_STATE = "checkState".toCharArray(); //$NON-NLS-1$

	// ... methods in java.util.Objects:
	char[] REQUIRE_NON_NULL = "requireNonNull".toCharArray(); //$NON-NLS-1$

	// different @Inject annotations are relevant for @NonNull fields
	char[] INJECT_PACKAGE = "inject".toCharArray(); //$NON-NLS-1$
	char[] INJECT_TYPE = "Inject".toCharArray(); //$NON-NLS-1$
	char[][] JAVAX_ANNOTATION_INJECT_INJECT = new char[][] { JAVAX, INJECT_PACKAGE, INJECT_TYPE };
	char[][] COM_GOOGLE_INJECT_INJECT = new char[][] {COM, GOOGLE, INJECT_PACKAGE, INJECT_TYPE };
	//    detail for the above:
	char[] OPTIONAL = "optional".toCharArray(); //$NON-NLS-1$

	// utils that ensure argument is nonnull if result is true
	// java.lang.Class:
	char[] IS_INSTANCE = "isInstance".toCharArray(); //$NON-NLS-1$
	// java.lang.Objects:
	char[] NON_NULL = "nonNull".toCharArray();   //$NON-NLS-1$
	char[] IS_NULL = "isNull".toCharArray();   //$NON-NLS-1$

	// well-known methods with "dangerous" signatures:
	char[][] JAVA_UTIL_MAP = new char[][] { JAVA, UTIL, "Map".toCharArray() }; //$NON-NLS-1$
	char[] GET = "get".toCharArray(); //$NON-NLS-1$
	char[] REMOVE = "remove".toCharArray(); //$NON-NLS-1$
	char[] REMOVE_ALL = "removeAll".toCharArray(); //$NON-NLS-1$
	char[] CONTAINS_ALL = "containsAll".toCharArray(); //$NON-NLS-1$
	char[] RETAIN_ALL = "retainAll".toCharArray(); //$NON-NLS-1$
	char[] CONTAINS_KEY = "containsKey".toCharArray(); //$NON-NLS-1$
	char[] CONTAINS_VALUE = "containsValue".toCharArray(); //$NON-NLS-1$
	// for Collection.contains:
	char[] CONTAINS = "contains".toCharArray(); //$NON-NLS-1$
	// for List.*indexOf:
	char[] INDEX_OF = "indexOf".toCharArray(); //$NON-NLS-1$
	char[] LAST_INDEX_OF = "lastIndexOf".toCharArray(); //$NON-NLS-1$
	enum DangerousMethod {
		// Collection:
		Contains, Remove, RemoveAll, ContainsAll, RetainAll,
		// Map:
		Get, ContainsKey, ContainsValue,
		// List:
		IndexOf, LastIndexOf,
		// Object:
		Equals;

		public static DangerousMethod detectSelector(char[] selector) {
			switch (selector[0]) {
				case 'r':
					if (CharOperation.prefixEquals(TypeConstants.REMOVE, selector)) {
						if (CharOperation.equals(selector, TypeConstants.REMOVE))
							return DangerousMethod.Remove;
						else if (CharOperation.equals(selector, TypeConstants.REMOVE_ALL))
							return DangerousMethod.RemoveAll;
					} else if (CharOperation.equals(selector, TypeConstants.RETAIN_ALL)) {
						return DangerousMethod.RetainAll;
					}
					break;
				case 'c':
					if (CharOperation.prefixEquals(TypeConstants.CONTAINS, selector)) {
						if (CharOperation.equals(selector, TypeConstants.CONTAINS))
							return DangerousMethod.Contains;
						else if (CharOperation.equals(selector, TypeConstants.CONTAINS_ALL))
							return DangerousMethod.ContainsAll;
						else if (CharOperation.equals(selector, TypeConstants.CONTAINS_KEY))
							return DangerousMethod.ContainsKey;
						else if (CharOperation.equals(selector, TypeConstants.CONTAINS_VALUE))
							return DangerousMethod.ContainsValue;
					}
					break;
				case 'g':
					if (CharOperation.equals(selector, TypeConstants.GET))
						return DangerousMethod.Get;
					break;
				case 'i':
					if (CharOperation.equals(selector, TypeConstants.INDEX_OF))
						return DangerousMethod.IndexOf;
					break;
				case 'l':
					if (CharOperation.equals(selector, TypeConstants.LAST_INDEX_OF))
						return DangerousMethod.LastIndexOf;
					break;
				case 'e':
					if (CharOperation.equals(selector, TypeConstants.EQUALS))
						return DangerousMethod.Equals;
					break;
			}
			return null;
		}
	}

	// Spring @Autowired annotation
	char [] AUTOWIRED = "Autowired".toCharArray();  //$NON-NLS-1$
	char [] BEANS = "beans".toCharArray();  //$NON-NLS-1$
	char [] FACTORY = "factory".toCharArray(); //$NON-NLS-1$
	char[][] ORG_SPRING_AUTOWIRED = new char[][] {ORG, SPRING, BEANS, FACTORY, ANNOTATION, AUTOWIRED};
	char[] REQUIRED = "required".toCharArray(); //$NON-NLS-1$

	// Constraints for generic type argument inference
	int CONSTRAINT_EQUAL = 0;		// Actual = Formal
	int CONSTRAINT_EXTENDS = 1;	// Actual << Formal
	int CONSTRAINT_SUPER = 2;		// Actual >> Formal

	// status of bound checks
	public static enum BoundCheckStatus {
		OK, NULL_PROBLEM, UNCHECKED, MISMATCH;
		/** true if no problem or only a null problem. */
		boolean isOKbyJLS() {
			switch (this) {
				case OK:
				case NULL_PROBLEM:
					return true;
				default:
					return false;
			}
		}
		public BoundCheckStatus betterOf(BoundCheckStatus other) {
			if (this.ordinal() < other.ordinal())
				return this;
			return other;
		}
	}

	// Synthetics
	char[] INIT = "<init>".toCharArray(); //$NON-NLS-1$
	char[] CLINIT = "<clinit>".toCharArray(); //$NON-NLS-1$
	char[] SYNTHETIC_SWITCH_ENUM_TABLE = "$SWITCH_TABLE$".toCharArray(); //$NON-NLS-1$
	char[] SYNTHETIC_ENUM_VALUES = "ENUM$VALUES".toCharArray(); //$NON-NLS-1$
	char[] SYNTHETIC_ASSERT_DISABLED = "$assertionsDisabled".toCharArray(); //$NON-NLS-1$
	char[] SYNTHETIC_CLASS = "class$".toCharArray(); //$NON-NLS-1$
	char[] SYNTHETIC_OUTER_LOCAL_PREFIX = "val$".toCharArray(); //$NON-NLS-1$
	char[] SYNTHETIC_ENCLOSING_INSTANCE_PREFIX = "this$".toCharArray(); //$NON-NLS-1$
	char[] SYNTHETIC_ACCESS_METHOD_PREFIX =  "access$".toCharArray(); //$NON-NLS-1$
	char[] SYNTHETIC_ENUM_CONSTANT_INITIALIZATION_METHOD_PREFIX =  " enum constant initialization$".toCharArray(); //$NON-NLS-1$
	char[] SYNTHETIC_STATIC_FACTORY =  "<factory>".toCharArray(); //$NON-NLS-1$

	// constants in enum org.eclipse.jdt.annotation.DefaultLocation:
	char[] DEFAULT_LOCATION__PARAMETER = "PARAMETER".toCharArray(); //$NON-NLS-1$
	char[] DEFAULT_LOCATION__RETURN_TYPE = "RETURN_TYPE".toCharArray(); //$NON-NLS-1$
	char[] DEFAULT_LOCATION__FIELD = "FIELD".toCharArray(); //$NON-NLS-1$
	char[] DEFAULT_LOCATION__TYPE_ARGUMENT = "TYPE_ARGUMENT".toCharArray(); //$NON-NLS-1$
	char[] DEFAULT_LOCATION__TYPE_PARAMETER = "TYPE_PARAMETER".toCharArray(); //$NON-NLS-1$
	char[] DEFAULT_LOCATION__TYPE_BOUND = "TYPE_BOUND".toCharArray(); //$NON-NLS-1$
	char[] DEFAULT_LOCATION__ARRAY_CONTENTS = "ARRAY_CONTENTS".toCharArray(); //$NON-NLS-1$


	// synthetic package-info name
	public static final char[] PACKAGE_INFO_NAME = "package-info".toCharArray(); //$NON-NLS-1$
	public static final char[] MODULE_INFO_NAME = "module-info".toCharArray(); //$NON-NLS-1$
	public static final String MODULE_INFO_NAME_STRING = "module-info"; //$NON-NLS-1$
	public static final char[] MODULE_INFO_FILE_NAME = "module-info.java".toCharArray(); //$NON-NLS-1$
	public static final char[] MODULE_INFO_CLASS_NAME = "module-info.class".toCharArray(); //$NON-NLS-1$
	public static final String MODULE_INFO_FILE_NAME_STRING = "module-info.java"; //$NON-NLS-1$
	public static final String MODULE_INFO_CLASS_NAME_STRING = "module-info.class"; //$NON-NLS-1$
	// java.base module name
	char[] JAVA_BASE = "java.base".toCharArray(); //$NON-NLS-1$
	String META_INF_MANIFEST_MF = "META-INF/MANIFEST.MF"; //$NON-NLS-1$
	String AUTOMATIC_MODULE_NAME = "Automatic-Module-Name";  //$NON-NLS-1$
	char[][] JDK_INTERNAL_VALUEBASED = {"jdk".toCharArray(), "internal".toCharArray(), "ValueBased".toCharArray()}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
}
