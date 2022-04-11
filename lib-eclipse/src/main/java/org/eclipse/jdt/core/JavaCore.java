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
 *     IBM Corporation - added the following constants:
 *                                 COMPILER_PB_DEPRECATION_IN_DEPRECATED_CODE
 *                                 COMPILER_PB_STATIC_ACCESS_RECEIVER
 *                                 COMPILER_TASK_TAGS
 *                                 CORE_CIRCULAR_CLASSPATH
 *                                 CORE_INCOMPLETE_CLASSPATH
 *     IBM Corporation - added run(IWorkspaceRunnable, IProgressMonitor)
 *     IBM Corporation - added exclusion patterns to source classpath entries
 *     IBM Corporation - added specific output location to source classpath entries
 *     IBM Corporation - added the following constants:
 *                                 CORE_JAVA_BUILD_CLEAN_OUTPUT_FOLDER
 *                                 CORE_JAVA_BUILD_RECREATE_MODIFIED_CLASS_FILES_IN_OUTPUT_FOLDER
 *                                 CLEAN
 *     IBM Corporation - added getClasspathContainerInitializer(String)
 *     IBM Corporation - added the following constants:
 *                                 CODEASSIST_ARGUMENT_PREFIXES
 *                                 CODEASSIST_ARGUMENT_SUFFIXES
 *                                 CODEASSIST_FIELD_PREFIXES
 *                                 CODEASSIST_FIELD_SUFFIXES
 *                                 CODEASSIST_LOCAL_PREFIXES
 *                                 CODEASSIST_LOCAL_SUFFIXES
 *                                 CODEASSIST_STATIC_FIELD_PREFIXES
 *                                 CODEASSIST_STATIC_FIELD_SUFFIXES
 *                                 COMPILER_PB_CHAR_ARRAY_IN_STRING_CONCATENATION
 *     IBM Corporation - added the following constants:
 *                                 COMPILER_PB_LOCAL_VARIABLE_HIDING
 *                                 COMPILER_PB_SPECIAL_PARAMETER_HIDING_FIELD
 *                                 COMPILER_PB_FIELD_HIDING
 *                                 COMPILER_PB_POSSIBLE_ACCIDENTAL_BOOLEAN_ASSIGNMENT
 *                                 CORE_INCOMPATIBLE_JDK_LEVEL
 *                                 VERSION_1_5
 *                                 COMPILER_PB_EMPTY_STATEMENT
 *     IBM Corporation - added the following constants:
 *                                 COMPILER_PB_INDIRECT_STATIC_ACCESS
 *                                 COMPILER_PB_BOOLEAN_METHOD_THROWING_EXCEPTION
 *                                 COMPILER_PB_UNNECESSARY_CAST
 *     IBM Corporation - added the following constants:
 *                                 COMPILER_PB_INVALID_JAVADOC
 *                                 COMPILER_PB_INVALID_JAVADOC_TAGS
 *                                 COMPILER_PB_INVALID_JAVADOC_TAGS_VISIBILITY
 *                                 COMPILER_PB_MISSING_JAVADOC_TAGS
 *                                 COMPILER_PB_MISSING_JAVADOC_TAGS_VISIBILITY
 *                                 COMPILER_PB_MISSING_JAVADOC_TAGS_OVERRIDING
 *                                 COMPILER_PB_MISSING_JAVADOC_COMMENTS
 *                                 COMPILER_PB_MISSING_JAVADOC_COMMENTS_VISIBILITY
 *                                 COMPILER_PB_MISSING_JAVADOC_COMMENTS_OVERRIDING
 *                                 COMPILER_PB_DEPRECATION_WHEN_OVERRIDING_DEPRECATED_METHOD
 *                                 COMPILER_PB_UNUSED_DECLARED_THROWN_EXCEPTION_WHEN_OVERRIDING
 *     IBM Corporation - added the following constants:
 *                                 TIMEOUT_FOR_PARAMETER_NAME_FROM_ATTACHED_JAVADOC
 *     IBM Corporation - added the following constants:
 *                                 COMPILER_PB_FALLTHROUGH_CASE
 *                                 COMPILER_PB_PARAMETER_ASSIGNMENT
 *                                 COMPILER_PB_NULL_REFERENCE
 *     IBM Corporation - added the following constants:
 *                                 CODEASSIST_DEPRECATION_CHECK
 *     IBM Corporation - added the following constants:
 *                                 COMPILER_PB_POTENTIAL_NULL_REFERENCE
 *                                 COMPILER_PB_REDUNDANT_NULL_CHECK
 *     IBM Corporation - added the following constants:
 *                                 COMPILER_PB_UNUSED_PARAMETER_INCLUDE_DOC_COMMENT_REFERENCE
 *     IBM Corporation - added the following constants:
 *                                 COMPILER_PB_UNUSED_DECLARED_THROWN_EXCEPTION_INCLUDE_DOC_COMMENT_REFERENCE
 *     IBM Corporation - added the following constants:
 *                                 COMPILER_PB_MISSING_JAVADOC_TAG_DESCRIPTION
 *								   COMPILER_PB_MISSING_JAVADOC_TAG_DESCRIPTION_NO_TAG
 *								   COMPILER_PB_MISSING_JAVADOC_TAG_DESCRIPTION_RETURN_TAG
 *								   COMPILER_PB_MISSING_JAVADOC_TAG_DESCRIPTION_ALL_TAGS
 *     IBM Corporation - added the following constants:
 *                                 COMPILER_PB_REDUNDANT_SUPERINTERFACE
 *     IBM Corporation - added the following constant:
 *                                 COMPILER_PB_UNUSED_DECLARED_THROWN_EXCEPTION_EXEMPT_EXCEPTION_AND_THROWABLE
 *     IBM Corporation - added getOptionForConfigurableSeverity(int)
 *     Benjamin Muskalla - added COMPILER_PB_MISSING_SYNCHRONIZED_ON_INHERITED_METHOD
 *     Stephan Herrmann  - added COMPILER_PB_UNUSED_OBJECT_ALLOCATION
 *     Stephan Herrmann  - added COMPILER_PB_SUPPRESS_OPTIONAL_ERRORS
 *     Stephan Herrmann  - added the following constants:
 *     								COMPILER_PB_UNCLOSED_CLOSEABLE,
 *     								COMPILER_PB_POTENTIALLY_UNCLOSED_CLOSEABLE
 *     								COMPILER_PB_EXPLICITLY_CLOSED_AUTOCLOSEABLE
 *     								COMPILER_ANNOTATION_NULL_ANALYSIS
 *     								COMPILER_NULLABLE_ANNOTATION_NAME
 *     								COMPILER_NONNULL_ANNOTATION_NAME
 *     								COMPILER_PB_NULL_SPECIFICATION_VIOLATION
 *     								COMPILER_PB_POTENTIAL_NULL_SPECIFICATION_VIOLATION
 *     								COMPILER_PB_NULL_SPECIFICATION_INSUFFICIENT_INFO
 *									COMPILER_PB_MISSING_ENUM_CASE_DESPITE_DEFAULT
 *									COMPILER_PB_SWITCH_MISSING_DEFAULT_CASE
 *									COMPILER_INHERIT_NULL_ANNOTATIONS
 *									COMPILER_PB_NONNULL_PARAMETER_ANNOTATION_DROPPED
 *									COMPILER_PB_SYNTACTIC_NULL_ANALYSIS_FOR_FIELDS
 *									COMPILER_PB_UNLIKELY_COLLECTION_METHOD_ARGUMENT_TYPE
 *									COMPILER_PB_UNLIKELY_EQUALS_ARGUMENT_TYPE
 *     Jesper S Moller   - Contributions for bug 381345 : [1.8] Take care of the Java 8 major version
 *                       - added the following constants:
 *									COMPILER_CODEGEN_METHOD_PARAMETERS_ATTR
 *     Harry Terkelsen (het@google.com) - Bug 449262 - Allow the use of third-party Java formatters
 *     Gábor Kövesdán - Contribution for Bug 350000 - [content assist] Include non-prefix matches in auto-complete suggestions
 *     Karsten Thoms - Bug 532505 - Reduce memory footprint of ClasspathAccessRule
 *
 *******************************************************************************/

package org.eclipse.jdt.core;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipFile;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.TypeNameRequestor;
import org.eclipse.jdt.core.util.IAttributeNamesConstants;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFormatException;
import org.eclipse.jdt.internal.compiler.env.AutomaticModuleNaming;
import org.eclipse.jdt.internal.compiler.env.IModule;
import org.eclipse.jdt.internal.compiler.env.IModule.IModuleReference;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.problem.ProblemReporter;
import org.eclipse.jdt.internal.core.BatchOperation;
import org.eclipse.jdt.internal.core.BufferManager;
import org.eclipse.jdt.internal.core.ClasspathAttribute;
import org.eclipse.jdt.internal.core.ClasspathEntry;
import org.eclipse.jdt.internal.core.ClasspathValidation;
import org.eclipse.jdt.internal.core.CreateTypeHierarchyOperation;
import org.eclipse.jdt.internal.core.DefaultWorkingCopyOwner;
import org.eclipse.jdt.internal.core.ExternalFoldersManager;
import org.eclipse.jdt.internal.core.JavaCorePreferenceInitializer;
import org.eclipse.jdt.internal.core.JavaModel;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.jdt.internal.core.JavaProject;
import org.eclipse.jdt.internal.core.PackageFragmentRoot;
import org.eclipse.jdt.internal.core.Region;
import org.eclipse.jdt.internal.core.SetContainerOperation;
import org.eclipse.jdt.internal.core.SetVariablesOperation;
import org.eclipse.jdt.internal.core.builder.JavaBuilder;
import org.eclipse.jdt.internal.core.builder.ModuleInfoBuilder;
import org.eclipse.jdt.internal.core.builder.State;
import org.eclipse.jdt.internal.core.search.indexing.IndexManager;
import org.eclipse.jdt.internal.core.util.MementoTokenizer;
import org.eclipse.jdt.internal.core.util.Messages;
import org.eclipse.jdt.internal.core.util.ModuleUtil;
import org.eclipse.jdt.internal.core.util.Util;
import org.osgi.framework.BundleContext;

/**
 * The plug-in runtime class for the Java model plug-in containing the core
 * (UI-free) support for Java projects.
 * <p>
 * Like all plug-in runtime classes (subclasses of <code>Plugin</code>), this
 * class is automatically instantiated by the platform when the plug-in gets
 * activated. Clients must not attempt to instantiate plug-in runtime classes
 * directly.
 * </p>
 * <p>
 * The single instance of this class can be accessed from any plug-in declaring
 * the Java model plug-in as a prerequisite via
 * <code>JavaCore.getJavaCore()</code>. The Java model plug-in will be activated
 * automatically if not already active.
 * </p>
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public final class JavaCore extends Plugin {

	private static final IResource[] NO_GENERATED_RESOURCES = new IResource[0];

	private static Plugin JAVA_CORE_PLUGIN = null;
	/**
	 * The plug-in identifier of the Java core support
	 * (value <code>"org.eclipse.jdt.core"</code>).
	 */
	public static final String PLUGIN_ID = "org.eclipse.jdt.core" ; //$NON-NLS-1$

	/**
	 * The identifier for the Java builder
	 * (value <code>"org.eclipse.jdt.core.javabuilder"</code>).
	 */
	public static final String BUILDER_ID = PLUGIN_ID + ".javabuilder" ; //$NON-NLS-1$

	/**
	 * The identifier for the Java model
	 * (value <code>"org.eclipse.jdt.core.javamodel"</code>).
	 */
	public static final String MODEL_ID = PLUGIN_ID + ".javamodel" ; //$NON-NLS-1$

	/**
	 * The identifier for the Java nature
	 * (value <code>"org.eclipse.jdt.core.javanature"</code>).
	 * The presence of this nature on a project indicates that it is
	 * Java-capable.
	 *
	 * @see org.eclipse.core.resources.IProject#hasNature(java.lang.String)
	 */
	public static final String NATURE_ID = PLUGIN_ID + ".javanature" ; //$NON-NLS-1$

	/**
	 * Name of the handle id attribute in a Java marker.
	 */
	protected static final String ATT_HANDLE_ID =
		"org.eclipse.jdt.internal.core.JavaModelManager.handleId" ; //$NON-NLS-1$

	/**
	 * Name of the User Library Container id.
	 * @since 3.0
	 */
	public static final String USER_LIBRARY_CONTAINER_ID= "org.eclipse.jdt.USER_LIBRARY"; //$NON-NLS-1$

	/**
	 * @since 3.14
	 */
	public static final String MODULE_PATH_CONTAINER_ID = "org.eclipse.jdt.MODULE_PATH"; //$NON-NLS-1$

	// Begin configurable option IDs {

	/**
	 * Compiler option ID: Generating Local Variable Debug Attribute.
	 * <p>When generated, this attribute will enable local variable names
	 *    to be displayed in debugger, only in place where variables are
	 *    definitely assigned (.class file is then bigger).</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.debug.localVariable"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "generate", "do not generate" }</code></dd>
	 * <dt>Default:</dt><dd><code>"generate"</code></dd>
	 * </dl>
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_LOCAL_VARIABLE_ATTR = PLUGIN_ID + ".compiler.debug.localVariable"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Generating Line Number Debug Attribute.
	 * <p>When generated, this attribute will enable source code highlighting in debugger
	 *    (.class file is then bigger).</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.debug.lineNumber"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "generate", "do not generate" }</code></dd>
	 * <dt>Default:</dt><dd><code>"generate"</code></dd>
	 * </dl>
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_LINE_NUMBER_ATTR = PLUGIN_ID + ".compiler.debug.lineNumber"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Generating Source Debug Attribute.
	 * <p>When generated, this attribute will enable the debugger to present the
	 *    corresponding source code.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.debug.sourceFile"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "generate", "do not generate" }</code></dd>
	 * <dt>Default:</dt><dd><code>"generate"</code></dd>
	 * </dl>
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_SOURCE_FILE_ATTR = PLUGIN_ID + ".compiler.debug.sourceFile"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Preserving Unused Local Variables.
	 * <p>Unless requested to preserve unused local variables (that is, never read), the
	 *    compiler will optimize them out, potentially altering debugging.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.codegen.unusedLocal"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "preserve", "optimize out" }</code></dd>
	 * <dt>Default:</dt><dd><code>"preserve"</code></dd>
	 * </dl>
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_CODEGEN_UNUSED_LOCAL = PLUGIN_ID + ".compiler.codegen.unusedLocal"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Generating Method Parameters Attribute.
	 * <p>When generated, this attribute will enable information about the formal parameters of a method
	 * (such as their names) to be accessed from reflection libraries, annotation processing,
	 * code weaving, and in the debugger, from platform target level 1.8 and later.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.codegen.methodParameters"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "generate", "do not generate" }</code></dd>
	 * <dt>Default:</dt><dd><code>"do not generate"</code></dd>
	 * </dl>
	 * @since 3.10
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_CODEGEN_METHOD_PARAMETERS_ATTR = PLUGIN_ID + ".compiler.codegen.methodParameters"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Defining Target Java Platform.
	 * <p>For binary compatibility reasons, .class files are tagged with a minimal required VM version.</p>
	 * <p>Note that <code>"1.4"</code> and higher target versions require the compliance mode to be at least as high
	 *    as the target version. Usually, compliance, target, and source versions are set to the same values.</p>
	 * <p><code>"cldc1.1"</code> requires the source version to be <code>"1.3"</code> and the compliance version to be <code>"1.4"</code> or lower.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.codegen.targetPlatform"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "1.1", "cldc1.1", "1.2", "1.3", "1.4", "1.5", "1.6", "1.7", "1.8", "9", "10", "11" }</code></dd>
	 * <dt>Default:</dt><dd><code>"1.2"</code></dd>
	 * </dl>
	 * @category CompilerOptionID
	 * @see #COMPILER_COMPLIANCE
	 * @see #COMPILER_SOURCE
	 * @see #setComplianceOptions(String, Map)
	 */
	public static final String COMPILER_CODEGEN_TARGET_PLATFORM = PLUGIN_ID + ".compiler.codegen.targetPlatform"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Inline JSR Bytecode Instruction.
	 * <p>When enabled, the compiler will no longer generate JSR instructions, but rather inline corresponding
	 *    subroutine code sequences (mostly corresponding to try finally blocks). The generated code will thus
	 *    get bigger, but will load faster on virtual machines since the verification process is then much simpler.</p>
	 * <p>This mode is anticipating support for the Java Specification Request 202.</p>
	 * <p>Note that JSR inlining is optional only for target platform lesser than 1.5. From 1.5 on, the JSR
	 *    inlining is mandatory (also see related setting {@link #COMPILER_CODEGEN_TARGET_PLATFORM}).</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.codegen.inlineJsrBytecode"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "enabled", "disabled" }</code></dd>
	 * <dt>Default:</dt><dd><code>"disabled"</code></dd>
	 * </dl>
	 * @since 3.0
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_CODEGEN_INLINE_JSR_BYTECODE = PLUGIN_ID + ".compiler.codegen.inlineJsrBytecode"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Javadoc Comment Support.
	 * <p>When this support is disabled, the compiler will ignore all javadoc problems options settings
	 *    and will not report any javadoc problem. It will also not find any reference in javadoc comment and
	 *    DOM AST Javadoc node will be only a flat text instead of having structured tag elements.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.doc.comment.support"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "enabled", "disabled" }</code></dd>
	 * <dt>Default:</dt><dd><code>"enabled"</code></dd>
	 * </dl>
	 * @since 3.0
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_DOC_COMMENT_SUPPORT = PLUGIN_ID + ".compiler.doc.comment.support"; //$NON-NLS-1$
	/**
	 * @deprecated Discontinued since turning off would violate language specs.
	 * @category DeprecatedOptionID
	 */
	public static final String COMPILER_PB_UNREACHABLE_CODE = PLUGIN_ID + ".compiler.problem.unreachableCode"; //$NON-NLS-1$
	/**
	 * @deprecated Discontinued since turning off would violate language specs.
	 * @category DeprecatedOptionID
	 */
	public static final String COMPILER_PB_INVALID_IMPORT = PLUGIN_ID + ".compiler.problem.invalidImport"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting Attempt to Override Package Visible Method.
	 * <p>A package visible method, which is any method that is not explicitly
	 *    declared as public, protected or private, is not visible from other
	 *    packages, and thus cannot be overridden from another package.
	 *    Attempting to override a package visible method from another package
	 *    introduces a new method that is unrelated to the original one. When
	 *    enabling this option, the compiler will signal such situations as an
	 *    error or a warning.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.overridingPackageDefaultMethod"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning", "info", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"warning"</code></dd>
	 * </dl>
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_OVERRIDING_PACKAGE_DEFAULT_METHOD = PLUGIN_ID + ".compiler.problem.overridingPackageDefaultMethod"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting Method With Constructor Name.
	 * <p>Naming a method with a constructor name is generally considered poor
	 *    style programming. When enabling this option, the compiler will signal such
	 *    scenario either as an error or a warning.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.methodWithConstructorName"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning", "info", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"warning"</code></dd>
	 * </dl>
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_METHOD_WITH_CONSTRUCTOR_NAME = PLUGIN_ID + ".compiler.problem.methodWithConstructorName"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting Deprecation.
	 * <p>When enabled, the compiler will signal use of deprecated API either as an
	 *    error or a warning.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.deprecation"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning", "info", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"warning"</code></dd>
	 * </dl>
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_DEPRECATION = PLUGIN_ID + ".compiler.problem.deprecation"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting Terminal Deprecation.
	 * <p>When enabled, the compiler will signal use of terminally deprecated API either as an
	 *    error or a warning.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.terminalDeprecation"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning", "info", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"warning"</code></dd>
	 * </dl>
	 * @since 3.14
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_TERMINAL_DEPRECATION = PLUGIN_ID + ".compiler.problem.terminalDeprecation"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting Deprecation Inside Deprecated Code.
	 * <p>When enabled, the compiler will signal use of deprecated API inside deprecated code.</p>
	 * <p>The severity of the problem is controlled with option {@link #COMPILER_PB_DEPRECATION}.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.deprecationInDeprecatedCode"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "enabled", "disabled" }</code></dd>
	 * <dt>Default:</dt><dd><code>"disabled"</code></dd>
	 * </dl>
	 * @since 2.1
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_DEPRECATION_IN_DEPRECATED_CODE = PLUGIN_ID + ".compiler.problem.deprecationInDeprecatedCode"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting Deprecation When Overriding Deprecated Method.
	 * <p>When enabled, the compiler will signal the declaration of a method overriding a deprecated one.</p>
	 * <p>The severity of the problem is controlled with option {@link #COMPILER_PB_DEPRECATION}.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.deprecationWhenOverridingDeprecatedMethod"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "enabled", "disabled" }</code></dd>
	 * <dt>Default:</dt><dd><code>"disabled"</code></dd>
	 * </dl>
	 * @since 3.0
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_DEPRECATION_WHEN_OVERRIDING_DEPRECATED_METHOD = "org.eclipse.jdt.core.compiler.problem.deprecationWhenOverridingDeprecatedMethod"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting Hidden Catch Block.
	 * <p>Locally to a try statement, some catch blocks may hide others. For example,</p>
	 *    <pre>
	 *      try {  throw new java.io.CharConversionException();
	 *      } catch (java.io.CharConversionException e) {
	 *      } catch (java.io.IOException e) {}.
	 *    </pre>
	 * <p>When enabling this option, the compiler will issue an error or a warning for hidden
	 *    catch blocks corresponding to checked exceptions.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.hiddenCatchBlock"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning", "info", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"warning"</code></dd>
	 * </dl>
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_HIDDEN_CATCH_BLOCK = PLUGIN_ID + ".compiler.problem.hiddenCatchBlock"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting Unused Local.
	 * <p>When enabled, the compiler will issue an error or a warning for unused local
	 *    variables (that is, variables never read from).</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.unusedLocal"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning", "info", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"warning"</code></dd>
	 * </dl>
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_UNUSED_LOCAL = PLUGIN_ID + ".compiler.problem.unusedLocal"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting Unused Parameter.
	 * <p>When enabled, the compiler will issue an error or a warning for unused method
	 *    parameters (that is, parameters never read from).</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.unusedParameter"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning", "info", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"ignore"</code></dd>
	 * </dl>
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_UNUSED_PARAMETER = PLUGIN_ID + ".compiler.problem.unusedParameter"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting Unused Exception Parameter.
	 * <p>When enabled, the compiler will issue an error or a warning for unused exception
	 *    parameters (that is, the thrown exception is never read from).</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.unusedExceptionParameter"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning", "info", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"ignore"</code></dd>
	 * </dl>
	 * @category CompilerOptionID
	 * @since 3.11
	 */
	public static final String COMPILER_PB_UNUSED_EXCEPTION_PARAMETER = PLUGIN_ID + ".compiler.problem.unusedExceptionParameter"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting Unused Parameter if Implementing Abstract Method.
	 * <p>When enabled, the compiler will signal unused parameters in abstract method implementations.</p>
	 * <p>The severity of the problem is controlled with option {@link #COMPILER_PB_UNUSED_PARAMETER}.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.unusedParameterWhenImplementingAbstract"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "enabled", "disabled" }</code></dd>
	 * <dt>Default:</dt><dd><code>"disabled"</code></dd>
	 * </dl>
	 * @since 2.1
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_UNUSED_PARAMETER_WHEN_IMPLEMENTING_ABSTRACT = PLUGIN_ID + ".compiler.problem.unusedParameterWhenImplementingAbstract"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting Unused Parameter if Overriding Concrete Method.
	 * <p>When enabled, the compiler will signal unused parameters in methods overriding concrete ones.</p>
	 * <p>The severity of the problem is controlled with option {@link #COMPILER_PB_UNUSED_PARAMETER}.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.unusedParameterWhenOverridingConcrete"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "enabled", "disabled" }</code></dd>
	 * <dt>Default:</dt><dd><code>"disabled"</code></dd>
	 * </dl>
	 * @since 2.1
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_UNUSED_PARAMETER_WHEN_OVERRIDING_CONCRETE = PLUGIN_ID + ".compiler.problem.unusedParameterWhenOverridingConcrete"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Consider Reference in Doc Comment for Unused Parameter Check.
	 * <p>When enabled, the compiler will consider doc comment references to parameters (i.e. <code>@param</code> clauses) for the unused
	 *    parameter check. Thus, documented parameters will be considered as mandated as per doc contract.</p>
	 * <p>The severity of the unused parameter problem is controlled with option {@link #COMPILER_PB_UNUSED_PARAMETER}.</p>
	 * <p>Note: this option has no effect until the doc comment support is enabled according to the
	 *    option {@link #COMPILER_DOC_COMMENT_SUPPORT}.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.unusedParameterIncludeDocCommentReference"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "enabled", "disabled" }</code></dd>
	 * <dt>Default:</dt><dd><code>"enabled"</code></dd>
	 * </dl>
	 * @since 3.3
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_UNUSED_PARAMETER_INCLUDE_DOC_COMMENT_REFERENCE = PLUGIN_ID + ".compiler.problem.unusedParameterIncludeDocCommentReference"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting Unused Import.
	 * <p>When enabled, the compiler will issue an error or a warning for unused import
	 *    reference.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.unusedImport"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning", "info", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"warning"</code></dd>
	 * </dl>
	 * @since 2.0
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_UNUSED_IMPORT = PLUGIN_ID + ".compiler.problem.unusedImport"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting Presence of Type Arguments for a Non-Generic Method Invocation.
	 * <p>When enabled, the compiler will issue an error or a warning whenever type arguments are encountered for a
	 *    non-generic method invocation. Note that prior to compliance level is <code>"1.7"</code>, this situation would automatically result
	 *    in an error. From Java7 on, unused type arguments are being tolerated, and optionally warned against.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.unusedTypeArgumentsForMethodInvocation"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning", "info", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"warning"</code></dd>
	 * </dl>
	 * @since 3.4
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_UNUSED_TYPE_ARGUMENTS_FOR_METHOD_INVOCATION = PLUGIN_ID + ".compiler.problem.unusedTypeArgumentsForMethodInvocation"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting Synthetic Access Emulation.
	 * <p>When enabled, the compiler will issue an error or a warning whenever it emulates
	 *    access to a non-accessible member of an enclosing type. Such access can have
	 *    performance implications.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.syntheticAccessEmulation"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning", "info", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"ignore"</code></dd>
	 * </dl>
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_SYNTHETIC_ACCESS_EMULATION = PLUGIN_ID + ".compiler.problem.syntheticAccessEmulation"; //$NON-NLS-1$

	/**
	 * Compiler option ID: Reporting Unused Type Parameter.
	 * <p>When enabled, the compiler will issue an error or a warning whenever it encounters an
	 * unused type parameter. </p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.unusedTypeParameter"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning", "info", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"ignore"</code></dd>
	 * </dl>
	 * @since 3.9
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_UNUSED_TYPE_PARAMETER = PLUGIN_ID + ".compiler.problem.unusedTypeParameter"; //$NON-NLS-1$

	/**
	 * Compiler option ID: Reporting Non-Externalized String Literal.
	 * <p>When enabled, the compiler will issue an error or a warning for non externalized
	 *    String literal (that is, not tagged with <code>//$NON-NLS-&lt;n&gt;$</code>).</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.nonExternalizedStringLiteral"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning", "info", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"ignore"</code></dd>
	 * </dl>
	 * @since 2.0
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_NON_NLS_STRING_LITERAL = PLUGIN_ID + ".compiler.problem.nonExternalizedStringLiteral"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting Usage of <code>'assert'</code> Identifier.
	 * <p>When enabled, the compiler will issue an error or a warning whenever <code>'assert'</code> is
	 *    used as an identifier (reserved keyword in 1.4).</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.assertIdentifier"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning", "info", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"warning"</code></dd>
	 * </dl>
	 * @since 2.0
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_ASSERT_IDENTIFIER = PLUGIN_ID + ".compiler.problem.assertIdentifier"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting Usage of <code>'enum'</code> Identifier.
	 * <p>When enabled, the compiler will issue an error or a warning whenever <code>'enum'</code> is
	 *    used as an identifier (reserved keyword in 1.5).</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.enumIdentifier"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning", "info", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"warning"</code></dd>
	 * </dl>
	 * @since 3.1
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_ENUM_IDENTIFIER = PLUGIN_ID + ".compiler.problem.enumIdentifier"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting Non-Static Reference to a Static Member.
	 * <p>When enabled, the compiler will issue an error or a warning whenever a static field
	 *    or method is accessed with an expression receiver. A reference to a static member should
	 *    be qualified with a type name.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.staticAccessReceiver"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning", "info", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"warning"</code></dd>
	 * </dl>
	 * @since 2.1
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_STATIC_ACCESS_RECEIVER = PLUGIN_ID + ".compiler.problem.staticAccessReceiver"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting Indirect Reference to a Static Member.
	 * <p>When enabled, the compiler will issue an error or a warning whenever a static field
	 *    or method is accessed in an indirect way. A reference to a static member should
	 *    preferably be qualified with its declaring type name.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.indirectStaticAccess"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning", "info", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"ignore"</code></dd>
	 * </dl>
	 * @since 3.0
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_INDIRECT_STATIC_ACCESS = PLUGIN_ID + ".compiler.problem.indirectStaticAccess"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting Assignment with no Effect.
	 * <p>When enabled, the compiler will issue an error or a warning whenever an assignment
	 *    has no effect (e.g <code>'x = x'</code>).</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.noEffectAssignment"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning", "info", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"warning"</code></dd>
	 * </dl>
	 * @since 2.1
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_NO_EFFECT_ASSIGNMENT = PLUGIN_ID + ".compiler.problem.noEffectAssignment"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting Interface Method not Compatible with non-Inherited Methods.
	 * <p>When enabled, the compiler will issue an error or a warning whenever an interface
	 *    defines a method incompatible with a non-inherited <code>Object</code> method. Until this conflict
	 *    is resolved, such an interface cannot be implemented. For example,</p>
	 *    <pre>
	 *      interface I {
	 *         int clone();
	 *      }
	 *    </pre>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.incompatibleNonInheritedInterfaceMethod"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning", "info", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"warning"</code></dd>
	 * </dl>
	 * @since 2.1
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_INCOMPATIBLE_NON_INHERITED_INTERFACE_METHOD = PLUGIN_ID + ".compiler.problem.incompatibleNonInheritedInterfaceMethod"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting Unused Private Members.
	 * <p>When enabled, the compiler will issue an error or a warning whenever a private
	 *    method or field is declared but never used within the same unit.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.unusedPrivateMember"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning", "info", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"warning"</code></dd>
	 * </dl>
	 * @since 2.1
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_UNUSED_PRIVATE_MEMBER = PLUGIN_ID + ".compiler.problem.unusedPrivateMember"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting Local Variable Declaration Hiding another Variable.
	 * <p>When enabled, the compiler will issue an error or a warning whenever a local variable
	 *    declaration is hiding some field or local variable (either locally, inherited or defined in enclosing type).</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.localVariableHiding"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning", "info", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"ignore"</code></dd>
	 * </dl>
	 * @since 3.0
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_LOCAL_VARIABLE_HIDING = PLUGIN_ID + ".compiler.problem.localVariableHiding"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting Special Parameter Hiding another Field.
	 * <p>When enabled, the compiler will signal cases where a constructor or setter method parameter declaration
	 *    is hiding some field (either locally, inherited or defined in enclosing type).</p>
	 * <p>The severity of the problem is controlled with option {@link #COMPILER_PB_LOCAL_VARIABLE_HIDING}.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.specialParameterHidingField"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "enabled", "disabled" }</code></dd>
	 * <dt>Default:</dt><dd><code>"disabled"</code></dd>
	 * </dl>
	 * @since 3.0
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_SPECIAL_PARAMETER_HIDING_FIELD = PLUGIN_ID + ".compiler.problem.specialParameterHidingField"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting Field Declaration Hiding another Variable.
	 * <p>When enabled, the compiler will issue an error or a warning whenever a field
	 *    declaration is hiding some field or local variable (either locally, inherited or defined in enclosing type).</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.fieldHiding"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning", "info", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"ignore"</code></dd>
	 * </dl>
	 * @since 3.0
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_FIELD_HIDING = PLUGIN_ID + ".compiler.problem.fieldHiding"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting Type Declaration Hiding another Type.
	 * <p>When enabled, the compiler will issue an error or a warning in situations where a type parameter
	 *    declaration is hiding some type, when a nested type is hiding some type parameter, or when
	 *    a nested type is hiding another nested type defined in same unit.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.typeParameterHiding"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning", "info", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"warning"</code></dd>
	 * </dl>
	 * @since 3.1
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_TYPE_PARAMETER_HIDING = PLUGIN_ID + ".compiler.problem.typeParameterHiding"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting Possible Accidental Boolean Assignment.
	 * <p>When enabled, the compiler will issue an error or a warning if a boolean assignment is acting as the condition
	 *    of a control statement  (where it probably was meant to be a boolean comparison).</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.possibleAccidentalBooleanAssignment"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning", "info", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"ignore"</code></dd>
	 * </dl>
	 * @since 3.0
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_POSSIBLE_ACCIDENTAL_BOOLEAN_ASSIGNMENT = PLUGIN_ID + ".compiler.problem.possibleAccidentalBooleanAssignment"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting Switch Fall-Through Case.
	 * <p>When enabled, the compiler will issue an error or a warning if a case may be
	 *    entered by falling through previous case. Empty cases are allowed.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.fallthroughCase"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning", "info", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"ignore"</code></dd>
	 * </dl>
	 * @since 3.2
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_FALLTHROUGH_CASE = PLUGIN_ID + ".compiler.problem.fallthroughCase"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting Empty Statements and Unnecessary Semicolons.
	 * <p>When enabled, the compiler will issue an error or a warning if an empty statement or a
	 *    unnecessary semicolon is encountered.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.emptyStatement"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning", "info", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"ignore"</code></dd>
	 * </dl>
	 * @since 3.0
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_EMPTY_STATEMENT = PLUGIN_ID + ".compiler.problem.emptyStatement"; //$NON-NLS-1$
	/**
	 * Compiler option ID.
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.booleanMethodThrowingException"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning", "info", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"ignore"</code></dd>
	 * </dl>
	 * @since 3.0
	 * @category CompilerOptionID
	 * @deprecated - this option has no effect
	 */
	public static final String COMPILER_PB_BOOLEAN_METHOD_THROWING_EXCEPTION = PLUGIN_ID + ".compiler.problem.booleanMethodThrowingException"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting Unnecessary Type Check.
	 * <p>When enabled, the compiler will issue an error or a warning when a cast or an <code>instanceof</code> operation
	 *    is unnecessary.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.unnecessaryTypeCheck"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning", "info", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"ignore"</code></dd>
	 * </dl>
	 * @since 3.0
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_UNNECESSARY_TYPE_CHECK = PLUGIN_ID + ".compiler.problem.unnecessaryTypeCheck"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting Unnecessary Else.
	 * <p>When enabled, the compiler will issue an error or a warning when a statement is unnecessarily
	 *    nested within an <code>else</code> clause (in situation where then clause is not completing normally).</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.unnecessaryElse"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning", "info", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"ignore"</code></dd>
	 * </dl>
	 * @since 3.0
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_UNNECESSARY_ELSE = PLUGIN_ID + ".compiler.problem.unnecessaryElse"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting Undocumented Empty Block.
	 * <p>When enabled, the compiler will issue an error or a warning when an empty block is detected and it is not
	 *    documented with any comment.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.undocumentedEmptyBlock"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning", "info", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"ignore"</code></dd>
	 * </dl>
	 * @since 3.0
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_UNDOCUMENTED_EMPTY_BLOCK = PLUGIN_ID + ".compiler.problem.undocumentedEmptyBlock"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting Finally Blocks Not Completing Normally.
	 * <p>When enabled, the compiler will issue an error or a warning when a finally block does not complete normally.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.finallyBlockNotCompletingNormally"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning", "info", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"warning"</code></dd>
	 * </dl>
	 * @since 3.0
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_FINALLY_BLOCK_NOT_COMPLETING = PLUGIN_ID + ".compiler.problem.finallyBlockNotCompletingNormally"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting Unused Declared Thrown Exception.
	 * <p>When enabled, the compiler will issue an error or a warning when a
	 *    method or a constructor is declaring a checked exception as thrown,
	 *    but its body actually raises neither that exception, nor any other
	 *    exception extending it.</p>
	 * <p>This diagnostic is further tuned by options
	 *    {@link #COMPILER_PB_UNUSED_DECLARED_THROWN_EXCEPTION_INCLUDE_DOC_COMMENT_REFERENCE},
	 *    {@link #COMPILER_PB_UNUSED_DECLARED_THROWN_EXCEPTION_EXEMPT_EXCEPTION_AND_THROWABLE},
	 *    and {@link #COMPILER_PB_UNUSED_DECLARED_THROWN_EXCEPTION_WHEN_OVERRIDING}.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.unusedDeclaredThrownException"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning", "info", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"ignore"</code></dd>
	 * </dl>
	 * @since 3.0
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_UNUSED_DECLARED_THROWN_EXCEPTION = PLUGIN_ID + ".compiler.problem.unusedDeclaredThrownException"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting Unused Declared Thrown Exception in Overriding Method.
	 * <p>When disabled, the compiler will report unused declared thrown
	 *    exceptions neither on overriding methods nor on implementing methods.</p>
	 * <p>The severity of the unused declared thrown exception problem is
	 *    controlled with option {@link #COMPILER_PB_UNUSED_DECLARED_THROWN_EXCEPTION}.</p>
	 * <p>This diagnostic is further tuned by options
	 *    {@link #COMPILER_PB_UNUSED_DECLARED_THROWN_EXCEPTION_INCLUDE_DOC_COMMENT_REFERENCE} and
	 *    {@link #COMPILER_PB_UNUSED_DECLARED_THROWN_EXCEPTION_EXEMPT_EXCEPTION_AND_THROWABLE}.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.unusedDeclaredThrownExceptionWhenOverriding"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "enabled", "disabled" }</code></dd>
	 * <dt>Default:</dt><dd><code>"disabled"</code></dd>
	 * </dl>
	 * @since 3.0
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_UNUSED_DECLARED_THROWN_EXCEPTION_WHEN_OVERRIDING = PLUGIN_ID + ".compiler.problem.unusedDeclaredThrownExceptionWhenOverriding"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Consider Reference in Doc Comment for Unused Declared Thrown Exception Check.
	 * <p>When enabled, the compiler will consider doc comment references to
	 *    exceptions (i.e. <code>@throws</code> clauses) for the unused
	 *    declared thrown exception check. Thus, documented exceptions will be
	 *    considered as mandated as per doc contract.</p>
	 * <p>The severity of the unused declared thrown exception problem is controlled with option {@link #COMPILER_PB_UNUSED_DECLARED_THROWN_EXCEPTION}.</p>
	 * <p>Note: this option has no effect until the doc comment support is enabled according to the
	 *    option {@link #COMPILER_DOC_COMMENT_SUPPORT}.</p>
	 * <p>This diagnostic is further tuned by options
	 *    {@link #COMPILER_PB_UNUSED_DECLARED_THROWN_EXCEPTION_EXEMPT_EXCEPTION_AND_THROWABLE}
	 *    and {@link #COMPILER_PB_UNUSED_DECLARED_THROWN_EXCEPTION_WHEN_OVERRIDING}.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.unusedDeclaredThrownExceptionIncludeDocCommentReference"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "enabled", "disabled" }</code></dd>
	 * <dt>Default:</dt><dd><code>"enabled"</code></dd>
	 * </dl>
	 * @since 3.4
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_UNUSED_DECLARED_THROWN_EXCEPTION_INCLUDE_DOC_COMMENT_REFERENCE = PLUGIN_ID + ".compiler.problem.unusedDeclaredThrownExceptionIncludeDocCommentReference"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting Unused Declared Thrown Exception Exempts Exception And Throwable.
	 * <p>When enabled, the compiler will issue an error or a warning when a
	 *    method or a constructor is declaring a checked exception else than
	 *    {@link java.lang.Throwable} or {@link java.lang.Exception} as thrown,
	 *    but its body actually raises neither that exception, nor any other
	 *    exception extending it. When disabled, the compiler will issue an
	 *    error or a warning when a method or a constructor is declaring a
	 *    checked exception (including {@link java.lang.Throwable} and
	 *    {@link java.lang.Exception}) as thrown, but its body actually raises
	 *    neither that exception, nor any other exception extending it.</p>
	 * <p>The severity of the unused declared thrown exception problem is
	 *    controlled with option
	 *    {@link #COMPILER_PB_UNUSED_DECLARED_THROWN_EXCEPTION}.</p>
	 * <p>This diagnostic is further tuned by options
	 *    {@link #COMPILER_PB_UNUSED_DECLARED_THROWN_EXCEPTION_INCLUDE_DOC_COMMENT_REFERENCE}
	 *    and {@link #COMPILER_PB_UNUSED_DECLARED_THROWN_EXCEPTION_WHEN_OVERRIDING}.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.unusedDeclaredThrownExceptionExemptExceptionAndThrowable"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "enabled", "disabled" }</code></dd>
	 * <dt>Default:</dt><dd><code>"enabled"</code></dd>
	 * </dl>
	 * @since 3.4
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_UNUSED_DECLARED_THROWN_EXCEPTION_EXEMPT_EXCEPTION_AND_THROWABLE = PLUGIN_ID + ".compiler.problem.unusedDeclaredThrownExceptionExemptExceptionAndThrowable"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting Unqualified Access to Field.
	 * <p>When enabled, the compiler will issue an error or a warning when a field is access without any qualification.
	 *    In order to improve code readability, it should be qualified, e.g. <code>'x'</code> should rather be written <code>'this.x'</code>.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.unqualifiedFieldAccess"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning", "info", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"ignore"</code></dd>
	 * </dl>
	 * @since 3.0
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_UNQUALIFIED_FIELD_ACCESS = PLUGIN_ID + ".compiler.problem.unqualifiedFieldAccess"; //$NON-NLS-1$
	/**
	 * @deprecated Use {@link #COMPILER_PB_UNCHECKED_TYPE_OPERATION} instead.
	 * @since 3.1
	 * @category DeprecatedOptionID
	 */
	public static final String COMPILER_PB_UNSAFE_TYPE_OPERATION = PLUGIN_ID + ".compiler.problem.uncheckedTypeOperation"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting Unchecked Type Operation.
	 * <p>When enabled, the compiler will issue an error or a warning whenever an operation involves generic types, and potentially
	 *    invalidates type safety since involving raw types (e.g. invoking <code>#foo(X&lt;String&gt;)</code> with arguments <code>(X)</code>).</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.uncheckedTypeOperation"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning", "info", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"warning"</code></dd>
	 * </dl>
	 * @since 3.1
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_UNCHECKED_TYPE_OPERATION = PLUGIN_ID + ".compiler.problem.uncheckedTypeOperation"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting Raw Type Reference.
	 * <p>When enabled, the compiler will issue an error or a warning when detecting references to raw types. Raw types are
	 *    discouraged, and are intended to help interfacing with legacy code. In the future, the language specification may
	 *    reject raw references to generic types.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.rawTypeReference"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning", "info", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"warning"</code></dd>
	 * </dl>
	 * @since 3.2
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_RAW_TYPE_REFERENCE = PLUGIN_ID + ".compiler.problem.rawTypeReference"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting of Unavoidable Generic Type Problems due to raw APIs.
	 * <p> When enabled, the compiler will issue an error or warning even when it detects a generics-related type problem
	 *     that could not have been avoided by the programmer, because a referenced API already contains raw types.
	 *     As an example, a type may be forced to use raw types
	 *     in its method signatures and return types because the methods it overrides from a super type are declared to
	 *     use raw types in the first place.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.unavoidableGenericTypeProblems"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "enabled", "disabled" }</code></dd>
	 * <dt>Default:</dt><dd><code>"enabled"</code></dd>
	 * </dl>
	 * @since 3.7
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_UNAVOIDABLE_GENERIC_TYPE_PROBLEMS = PLUGIN_ID + ".compiler.problem.unavoidableGenericTypeProblems"; //$NON-NLS-1$

	/**
	 * Compiler option ID: Reporting final Bound for Type Parameter.
	 * <p>When enabled, the compiler will issue an error or a warning whenever a generic type parameter is associated with a
	 *    bound corresponding to a final type; since final types cannot be further extended, the parameter is pretty useless.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.finalParameterBound"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning", "info", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"warning"</code></dd>
	 * </dl>
	 * @since 3.1
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_FINAL_PARAMETER_BOUND = PLUGIN_ID + ".compiler.problem.finalParameterBound"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting Missing Declaration of serialVersionUID Field on Serializable Class.
	 * <p>When enabled, the compiler will issue an error or a warning whenever a serializable class is missing a local declaration
	 *    of a <code>serialVersionUID</code> field. This field must be declared as static final and be of type <code>long</code>.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.missingSerialVersion"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning", "info", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"warning"</code></dd>
	 * </dl>
	 * @since 3.1
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_MISSING_SERIAL_VERSION = PLUGIN_ID + ".compiler.problem.missingSerialVersion"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting Varargs Argument Needing a Cast in Method/Constructor Invocation.
	 * <p>When enabled, the compiler will issue an error or a warning whenever a varargs arguments should be cast
	 *    when passed to a method/constructor invocation. (e.g. <code>Class.getMethod(String name, Class ... args )</code>
	 *    invoked with arguments <code>("foo", null)</code>).</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.varargsArgumentNeedCast"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning", "info", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"warning"</code></dd>
	 * </dl>
	 * @since 3.1
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_VARARGS_ARGUMENT_NEED_CAST = PLUGIN_ID + ".compiler.problem.varargsArgumentNeedCast"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting Boxing/Unboxing Conversion.
	 * <p>When enabled, the compiler will issue an error or a warning whenever a boxing or an unboxing
	 *    conversion is performed.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.autoboxing"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning", "info", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"ignore"</code></dd>
	 * </dl>
	 * @since 3.1
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_AUTOBOXING = PLUGIN_ID + ".compiler.problem.autoboxing"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting Use of Annotation Type as Super Interface.
	 * <p>When enabled, the compiler will issue an error or a warning whenever an annotation type is used
	 *    as a super-interface. Though legal, this is discouraged.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.annotationSuperInterface"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning", "info", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"warning"</code></dd>
	 * </dl>
	 * @since 3.1
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_ANNOTATION_SUPER_INTERFACE = PLUGIN_ID + ".compiler.problem.annotationSuperInterface"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting Missing <code>@Override</code> Annotation.
	 * <p>When enabled, the compiler will issue an error or a warning whenever encountering a method
	 *    declaration which overrides a superclass method but has no <code>@Override</code> annotation.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.missingOverrideAnnotation"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning", "info", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"ignore"</code></dd>
	 * </dl>
	 * @since 3.1
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_MISSING_OVERRIDE_ANNOTATION = PLUGIN_ID + ".compiler.problem.missingOverrideAnnotation"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting Missing <code>@Override</code> Annotation for interface method implementation.
	 * <p>When enabled, the compiler will issue an error or a warning whenever encountering a method
	 *    declaration which overrides or implements a superinterface method but has no <code>@Override</code> annotation.</p>
	 * <p>This option only has an effect if the compiler compliance is 1.6 or greater.</p>
	 * <p>The severity of the problem is controlled with option {@link #COMPILER_PB_MISSING_OVERRIDE_ANNOTATION}.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.missingOverrideAnnotationForInterfaceMethodImplementation"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "enabled", "disabled" }</code></dd>
	 * <dt>Default:</dt><dd><code>"enabled"</code></dd>
	 * </dl>
	 * @since 3.6
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_MISSING_OVERRIDE_ANNOTATION_FOR_INTERFACE_METHOD_IMPLEMENTATION = PLUGIN_ID + ".compiler.problem.missingOverrideAnnotationForInterfaceMethodImplementation"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting Missing <code>@Deprecated</code> Annotation.
	 * <p>When enabled, the compiler will issue an error or a warning whenever encountering a declaration
	 *    carrying a <code>@deprecated</code> doc tag but having no corresponding <code>@Deprecated</code> annotation.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.missingDeprecatedAnnotation"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning", "info", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"ignore"</code></dd>
	 * </dl>
	 * @since 3.1
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_MISSING_DEPRECATED_ANNOTATION = PLUGIN_ID + ".compiler.problem.missingDeprecatedAnnotation"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting Missing HashCode Method.
	 * <p>When enabled, the compiler will issue an error or a warning if a type
	 * overrides Object.equals(Object) but does not override hashCode().</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.missingHashCodeMethod"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning", "info", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"ignore"</code></dd>
	 * </dl>
	 * @since 3.5
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_MISSING_HASHCODE_METHOD = PLUGIN_ID + ".compiler.problem.missingHashCodeMethod"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting Dead Code.
	 * <p>When enabled, the compiler will issue an error or a warning if some non fatal dead code is detected. For instance, <code>if (false) foo();</code>
	 * is not reported as truly unreachable code by the Java Language Specification. If this diagnostic is enabled, then the invocation of <code>foo()</code> is
	 * going to be signaled as being dead code.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.deadCode"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning", "info", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"warning"</code></dd>
	 * </dl>
	 * @since 3.5
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_DEAD_CODE = PLUGIN_ID + ".compiler.problem.deadCode"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting Dead Code Inside Trivial If Statement.
	 * <p>When enabled, the compiler will signal presence of dead code inside trivial IF statement, e.g. <code>if (DEBUG)...</code>.</p>
	 * <p>The severity of the problem is controlled with option {@link #COMPILER_PB_DEAD_CODE}.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.deadCodeInTrivialIfStatement"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "enabled", "disabled" }</code></dd>
	 * <dt>Default:</dt><dd><code>"disabled"</code></dd>
	 * </dl>
	 * @since 3.5
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_DEAD_CODE_IN_TRIVIAL_IF_STATEMENT = PLUGIN_ID + ".compiler.problem.deadCodeInTrivialIfStatement"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting Incomplete Enum Switch.
	 * <p>When enabled, the compiler will issue an error or a warning
	 * 		regarding each enum constant for which a corresponding case label is lacking.
	 * 		Reporting is further controlled by the option {@link #COMPILER_PB_MISSING_ENUM_CASE_DESPITE_DEFAULT}.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.incompleteEnumSwitch"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning", "info", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"warning"</code></dd>
	 * </dl>
	 * @since 3.1
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_INCOMPLETE_ENUM_SWITCH = PLUGIN_ID + ".compiler.problem.incompleteEnumSwitch"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting Missing Enum Case In Switch Despite An Existing Default Case.
	 * <p>This option further controls the option {@link #COMPILER_PB_INCOMPLETE_ENUM_SWITCH}:</p>
	 * 	<ul>
	 * 	<li>If enabled the compiler will report problems about missing enum constants even if a default case exists
	 * 		in the same switch statement.</li>
	 *  <li>If disabled existence of a default case is considered as sufficient to make a switch statement complete.</li>
	 *  </ul>
	 *  This option has no effect if {@link #COMPILER_PB_INCOMPLETE_ENUM_SWITCH} is set to <code>"ignore"</code>.
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.missingEnumCaseDespiteDefault"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "enabled", "disabled" }</code></dd>
	 * <dt>Default:</dt><dd><code>"disabled"</code></dd>
	 * </dl>
	 * @since 3.8
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_MISSING_ENUM_CASE_DESPITE_DEFAULT = PLUGIN_ID + ".compiler.problem.missingEnumCaseDespiteDefault"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting Missing Default Case In Switch.
	 * <p>When enabled, the compiler will issue an error or a warning
	 * 		against each switch statement that lacks a default case.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.missingDefaultCase"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning", "info", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"ignore"</code></dd>
	 * </dl>
	 * @since 3.8
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_SWITCH_MISSING_DEFAULT_CASE = PLUGIN_ID + ".compiler.problem.missingDefaultCase"; //$NON-NLS-1$
	/**
	 * @since 3.1
	 * @deprecated Use {@link #COMPILER_PB_NULL_REFERENCE} instead.
	 * @category DeprecatedOptionID
	 */
	public static final String COMPILER_PB_INCONSISTENT_NULL_CHECK = PLUGIN_ID + ".compiler.problem.inconsistentNullCheck"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting Unreferenced Label.
	 * <p>When enabled, the compiler will issue an error or a warning when encountering a labeled statement which label
	 *    is never explicitly referenced. A label is considered to be referenced if its name explicitly appears behind a break
	 *    or continue statement; for instance the following label would be considered unreferenced:</p>
	 *    <code>LABEL: { break; }</code>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.unusedLabel"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning", "info", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"warning"</code></dd>
	 * </dl>
	 * @since 3.2
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_UNUSED_LABEL = PLUGIN_ID + ".compiler.problem.unusedLabel"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting Invalid Javadoc Comment.
	 * <p>This is the generic control for the severity of Javadoc problems.
	 *    When enabled, the compiler will issue an error or a warning for a problem in Javadoc.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.invalidJavadoc"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning", "info", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"ignore"</code></dd>
	 * </dl>
	 * @since 3.0
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_INVALID_JAVADOC = PLUGIN_ID + ".compiler.problem.invalidJavadoc"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting Invalid Javadoc Tags.
	 * <p>When enabled, the compiler will signal unbound or unexpected reference tags in Javadoc.
	 *    A <code>@throws</code> tag referencing an undeclared exception would be considered as unexpected.</p>
	 * <p>Note that this diagnosis can be enabled based on the visibility of the construct associated with the Javadoc;
	 *    also see the setting {@link #COMPILER_PB_INVALID_JAVADOC_TAGS_VISIBILITY}.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.invalidJavadocTags"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "enabled", "disabled" }</code></dd>
	 * <dt>Default:</dt><dd><code>"disabled"</code></dd>
	 * </dl>
	 * @since 3.0
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_INVALID_JAVADOC_TAGS = PLUGIN_ID + ".compiler.problem.invalidJavadocTags"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting Invalid Javadoc Tags with Deprecated References.
	 * <p>Specify whether the compiler will report deprecated references used in Javadoc tags.</p>
	 * <p>Note that this diagnosis can be enabled based on the visibility of the construct associated with the Javadoc;
	 *    also see the setting {@link #COMPILER_PB_INVALID_JAVADOC_TAGS_VISIBILITY}.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.invalidJavadocTagsDeprecatedRef"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "enabled", "disabled" }</code></dd>
	 * <dt>Default:</dt><dd><code>"disabled"</code></dd>
	 * </dl>
	 * @since 3.1
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_INVALID_JAVADOC_TAGS__DEPRECATED_REF = PLUGIN_ID + ".compiler.problem.invalidJavadocTagsDeprecatedRef"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting Invalid Javadoc Tags with Not Visible References.
	 * <p>Specify whether the compiler will report non-visible references used in Javadoc tags.</p>
	 * <p>Note that this diagnosis can be enabled based on the visibility of the construct associated with the Javadoc;
	 *    also see the setting {@link #COMPILER_PB_INVALID_JAVADOC_TAGS_VISIBILITY}.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.invalidJavadocTagsNotVisibleRef"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "enabled", "disabled" }</code></dd>
	 * <dt>Default:</dt><dd><code>"disabled"</code></dd>
	 * </dl>
	 * @since 3.1
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_INVALID_JAVADOC_TAGS__NOT_VISIBLE_REF = PLUGIN_ID + ".compiler.problem.invalidJavadocTagsNotVisibleRef"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Visibility Level For Invalid Javadoc Tags.
	 * <p>Set the minimum visibility level for Javadoc tag problems. Below this level problems will be ignored.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.invalidJavadocTagsVisibility"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "public", "protected", "default", "private" }</code></dd>
	 * <dt>Default:</dt><dd><code>"public"</code></dd>
	 * </dl>
	 * @since 3.0
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_INVALID_JAVADOC_TAGS_VISIBILITY = PLUGIN_ID + ".compiler.problem.invalidJavadocTagsVisibility"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting missing tag description.
	 * <p>When enabled, the compiler will report a warning or an error for any Javadoc tag missing a required description.</p>
	 * <p>The severity of the problem is controlled with option {@link #COMPILER_PB_INVALID_JAVADOC}.</p>
	 * <p>It does not depend on option {@link #COMPILER_PB_INVALID_JAVADOC_TAGS}.</p>
	 * <p>When this option is valued to {@link #COMPILER_PB_MISSING_JAVADOC_TAG_DESCRIPTION_ALL_STANDARD_TAGS},
	 *       a subset of the standard <a href="http://download.oracle.com/javase/6/docs/technotes/tools/windows/javadoc.html#javadoctags">Javadoc tags</a>
	 *       that have a description, text or label are checked. While this set may grow in the future, note that user-defined tags are not and will not be checked.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.missingJavadocTagDescription"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "return_tag", "all_standard_tags", "no_tag" }</code></dd>
	 * <dt>Default:</dt><dd><code>"return_tag"</code></dd>
	 * </dl>
	 * @since 3.4
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_MISSING_JAVADOC_TAG_DESCRIPTION = PLUGIN_ID + ".compiler.problem.missingJavadocTagDescription"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting Missing Javadoc Tags.
	 * <p>This is the generic control for the severity of Javadoc missing tag problems.
	 *    When enabled, the compiler will issue an error or a warning when tags are missing in Javadoc comments.</p>
	 * <p>Note that this diagnosis can be enabled based on the visibility of the construct associated with the Javadoc;
	 *    also see the setting {@link #COMPILER_PB_MISSING_JAVADOC_TAGS_VISIBILITY}.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.missingJavadocTags"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning", "info", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"ignore"</code></dd>
	 * </dl>
	 * @since 3.0
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_MISSING_JAVADOC_TAGS = PLUGIN_ID + ".compiler.problem.missingJavadocTags"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Visibility Level For Missing Javadoc Tags.
	 * <p>Set the minimum visibility level for Javadoc missing tag problems. Below this level problems will be ignored.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.missingJavadocTagsVisibility"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "public", "protected", "default", "private" }</code></dd>
	 * <dt>Default:</dt><dd><code>"public"</code></dd>
	 * </dl>
	 * @since 3.0
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_MISSING_JAVADOC_TAGS_VISIBILITY = PLUGIN_ID + ".compiler.problem.missingJavadocTagsVisibility"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting Missing Javadoc Tags on Overriding Methods.
	 * <p>Specify whether the compiler will verify overriding methods in order to report Javadoc missing tag problems.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.missingJavadocTagsOverriding"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "enabled", "disabled" }</code></dd>
	 * <dt>Default:</dt><dd><code>"disabled"</code></dd>
	 * </dl>
	 * @since 3.0
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_MISSING_JAVADOC_TAGS_OVERRIDING = PLUGIN_ID + ".compiler.problem.missingJavadocTagsOverriding"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting Missing Javadoc Tags for Method Type Parameters.
	 * <p>Specify whether a missing <code>@param</code> for a type parameter in a method declaration should be reported.
	 *    When enabled, the compiler will issue a missing Javadoc tag error or warning for a type parameter without a
	 *    corresponding <code>@param</code> tag.</p>
	 * <p>This option only has an effect if the compiler compliance is 1.5 or greater.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.missingJavadocTagsMethodTypeParameters"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "enabled", "disabled" }</code></dd>
	 * <dt>Default:</dt><dd><code>"disabled"</code></dd>
	 * </dl>
	 * @since 3.7
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_MISSING_JAVADOC_TAGS_METHOD_TYPE_PARAMETERS = PLUGIN_ID + ".compiler.problem.missingJavadocTagsMethodTypeParameters"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting Missing Javadoc Comments.
	 * <p>This is the generic control for the severity of missing Javadoc comment problems.
	 *    When enabled, the compiler will issue an error or a warning when Javadoc comments are missing.</p>
	 * <p>Note that this diagnosis can be enabled based on the visibility of the construct associated with the expected Javadoc;
	 *    also see the setting {@link #COMPILER_PB_MISSING_JAVADOC_COMMENTS_VISIBILITY}.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.missingJavadocComments"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning", "info", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"ignore"</code></dd>
	 * </dl>
	 * @since 3.0
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_MISSING_JAVADOC_COMMENTS = PLUGIN_ID + ".compiler.problem.missingJavadocComments"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Visibility Level For Missing Javadoc Comments.
	 * <p>Set the minimum visibility level for missing Javadoc problems. Below this level problems will be ignored.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.missingJavadocCommentsVisibility"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "public", "protected", "default", "private" }</code></dd>
	 * <dt>Default:</dt><dd><code>"public"</code></dd>
	 * </dl>
	 * @since 3.0
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_MISSING_JAVADOC_COMMENTS_VISIBILITY = PLUGIN_ID + ".compiler.problem.missingJavadocCommentsVisibility"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting Missing Javadoc Comments on Overriding Methods.
	 * <p>Specify whether the compiler will verify overriding methods in order to report missing Javadoc comment problems.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.missingJavadocCommentsOverriding"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "enabled", "disabled" }</code></dd>
	 * <dt>Default:</dt><dd><code>"disabled"</code></dd>
	 * </dl>
	 * @since 3.0
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_MISSING_JAVADOC_COMMENTS_OVERRIDING = PLUGIN_ID + ".compiler.problem.missingJavadocCommentsOverriding"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting Usage of <code>char[]</code> Expressions in String Concatenations.
	 * <p>When enabled, the compiler will issue an error or a warning whenever a <code>char[]</code> expression
	 *    is used in String concatenations (for example, <code>"hello" + new char[]{'w','o','r','l','d'}</code>).</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.noImplicitStringConversion"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning", "info", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"warning"</code></dd>
	 * </dl>
	 * @since 2.1
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_CHAR_ARRAY_IN_STRING_CONCATENATION = PLUGIN_ID + ".compiler.problem.noImplicitStringConversion"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Maximum Number of Problems Reported per Compilation Unit.
	 * <p>Specify the maximum number of problems reported on each compilation unit.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.maxProblemPerUnit"</code></dd>
	 * <dt>Possible values:</dt><dd><code>"&lt;n&gt;"</code> where <code>&lt;n&gt;</code> is zero or a positive integer (if zero then all problems are reported).</dd>
	 * <dt>Default:</dt><dd><code>"100"</code></dd>
	 * </dl>
	 * @since 2.0
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_MAX_PER_UNIT = PLUGIN_ID + ".compiler.maxProblemPerUnit"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Treating Optional Error as Fatal.
	 * <p>When enabled, optional errors (i.e. optional problems which severity is set to <code>"error"</code>) will be treated as standard
	 *    compiler errors, yielding problem methods/types preventing from running offending code until the issue got resolved.</p>
	 * <p>When disabled, optional errors are only considered as warnings, still carrying an error indication to make them more
	 *    severe. Note that by default, optional errors are not fatal. Non-optional errors are
	 *    always fatal.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.fatalOptionalError"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "enabled", "disabled" }</code></dd>
	 * <dt>Default:</dt><dd><code>"disabled"</code></dd>
	 * </dl>
	 * @since 3.2
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_FATAL_OPTIONAL_ERROR = PLUGIN_ID + ".compiler.problem.fatalOptionalError"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting Parameter Assignment.
	 * <p>When enabled, the compiler will issue an error or a warning if a parameter is
	 *    assigned to.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.parameterAssignment"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning", "info", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"ignore"</code></dd>
	 * </dl>
	 * @since 3.2
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_PARAMETER_ASSIGNMENT = PLUGIN_ID + ".compiler.problem.parameterAssignment"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting a method that qualifies as static, but not declared static.
	 * <p>When enabled, the compiler will issue an error or a warning if a method has
	 *    not been declared as <code>static</code>, even though it qualifies as one.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.reportMethodCanBeStatic"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning", "info", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"ignore"</code></dd>
	 * </dl>
	 * @since 3.7
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_MISSING_STATIC_ON_METHOD = PLUGIN_ID + ".compiler.problem.reportMethodCanBeStatic"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting a method that may qualify as static, but not declared static.
	 * <p>When enabled, the compiler will issue an error or a warning if a method has
	 *    not been declared as <code>static</code>, even though it may qualify as one,
	 *    when another method doesn't override it.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.reportMethodCanBePotentiallyStatic"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning", "info", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"ignore"</code></dd>
	 * </dl>
	 * @since 3.7
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_POTENTIALLY_MISSING_STATIC_ON_METHOD = PLUGIN_ID + ".compiler.problem.reportMethodCanBePotentiallyStatic"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting a resource that is not closed properly.
	 * <p>When enabled, the compiler will issue an error or a warning if
	 *    a local variable holds a value of type <code>java.lang.AutoCloseable</code> (compliance>=1.7)
	 *    or a value of type <code>java.io.Closeable</code> (compliance<=1.6) and if
	 *    flow analysis shows that the method <code>close()</code> is not invoked locally on that value.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.unclosedCloseable"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning", "info", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"warning"</code></dd>
	 * </dl>
	 * @since 3.8
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_UNCLOSED_CLOSEABLE = PLUGIN_ID + ".compiler.problem.unclosedCloseable"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting a resource that may not be closed properly.
	 * <p>When enabled, the compiler will issue an error or a warning if
	 *    a local variable holds a value of type <code>java.lang.AutoCloseable</code> (compliance>=1.7)
	 *    or a value of type <code>java.io.Closeable</code> (compliance<=1.6) and if
	 *    flow analysis shows that the method <code>close()</code> is
	 *    not invoked locally on that value for all execution paths.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.potentiallyUnclosedCloseable"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning", "info", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"ignore"</code></dd>
	 * </dl>
	 * @since 3.8
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_POTENTIALLY_UNCLOSED_CLOSEABLE = PLUGIN_ID + ".compiler.problem.potentiallyUnclosedCloseable"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting a resource that is not managed by try-with-resources.
	 * <p>When enabled, the compiler will issue an error or a warning if a local variable
	 * 	  holds a value of type <code>java.lang.AutoCloseable</code>, and if the method
	 *    <code>close()</code> is explicitly invoked on that resource, but the resource is
	 *    not managed by a try-with-resources block.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.explicitlyClosedAutoCloseable"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning", "info", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"ignore"</code></dd>
	 * </dl>
	 * @since 3.8
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_EXPLICITLY_CLOSED_AUTOCLOSEABLE = PLUGIN_ID + ".compiler.problem.explicitlyClosedAutoCloseable"; //$NON-NLS-1$

	/**
	 * Compiler option ID: Reporting a method invocation providing an argument of an unlikely type.
	 * <p>When enabled, the compiler will issue an error or warning when certain well-known Collection methods
	 *    that take an 'Object', like e.g. {@link Map#get(Object)}, are used with an argument type
	 *    that seems to be not related to the corresponding type argument of the Collection.</p>
	 * <p>By default, this analysis will apply some heuristics to determine whether or not two
	 *    types may or may not be related, which can be changed via option
	 *    {@link #COMPILER_PB_UNLIKELY_COLLECTION_METHOD_ARGUMENT_TYPE_STRICT}.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.unlikelyCollectionMethodArgumentType"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning", "info", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"warning"</code></dd>
	 * </dl>
	 * @since 3.13
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_UNLIKELY_COLLECTION_METHOD_ARGUMENT_TYPE = PLUGIN_ID + ".compiler.problem.unlikelyCollectionMethodArgumentType"; //$NON-NLS-1$

	/**
	 * Compiler option ID: Perform strict analysis against the expected type of collection methods.
	 * <p>This is a sub-option of {@link #COMPILER_PB_UNLIKELY_COLLECTION_METHOD_ARGUMENT_TYPE},
	 *    which will replace the heuristics with strict compatibility checks,
	 *    i.e., each argument that is not strictly compatible with the expected type will trigger an error or warning.</p>
	 * <p>This option has no effect if {@link #COMPILER_PB_UNLIKELY_COLLECTION_METHOD_ARGUMENT_TYPE} is set to <code>"ignore"</code>.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.unlikelyCollectionMethodArgumentTypeStrict"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "enabled", "disabled" }</code></dd>
	 * <dt>Default:</dt><dd><code>"disabled"</code></dd>
	 * </dl>
	 * @since 3.13
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_UNLIKELY_COLLECTION_METHOD_ARGUMENT_TYPE_STRICT = PLUGIN_ID + ".compiler.problem.unlikelyCollectionMethodArgumentTypeStrict"; //$NON-NLS-1$

	/**
	 * Compiler option ID: Reporting a method invocation providing an argument of an unlikely type to method 'equals'.
	 * <p>
	 * When enabled, the compiler will issue an error or warning when {@link java.lang.Object#equals(Object)} is used with an argument type
	 * that seems to be not related to the receiver's type, or correspondingly when the arguments of {@link java.util.Objects#equals(Object, Object)}
	 * have types that seem to be not related to each other.
	 * </p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.unlikelyEqualsArgumentType"</code></dd>
	 * <dt>Possible values:</dt>
	 * <dd><code>{ "error", "warning", "info", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"info"</code></dd>
	 * </dl>
	 *
	 * @since 3.13
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_UNLIKELY_EQUALS_ARGUMENT_TYPE = PLUGIN_ID + ".compiler.problem.unlikelyEqualsArgumentType"; //$NON-NLS-1$

	/**
	 * Compiler option ID: Reporting when public API uses a non-API type.
	 * <p>
	 * This option is relevant only when compiling code in a named module (at compliance 9 or greater).
	 * <p>
	 * When enabled, the compiler will issue an error or warning when public API mentions a type that is not
	 * accessible to clients. Here, public API refers to signatures of public fields and methods declared
	 * by a public type in an exported package.
	 * In these positions types are complained against that are either not public or not in an exported package.
	 * Export qualification is not taken into account.
	 * If a type in one of these positions is declared in another module that is required by the current module,
	 * but without the {@code transitive} modifier, this is reported as a problem, too.
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.APILeak"</code></dd>
	 * <dt>Possible values:</dt>
	 * <dd><code>{ "error", "warning", "info", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"warning"</code></dd>
	 * </dl>
	 *
	 * @since 3.14
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_API_LEAKS = PLUGIN_ID + ".compiler.problem.APILeak"; //$NON-NLS-1$

	/**
	 * Compiler option ID: Reporting when a module requires an auto module with an unstable name.
	 * <p>
	 * The name of an auto module name is considered unstable when it is derived from a file name rather than
	 * being declared in the module's MANIFEST.MF.
	 * <p>
	 * When enabled, the compiler will issue an error or warning when a module references an auto module
	 * with an unstable name in its 'requires' clause.
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.unstableAutoModuleName"</code></dd>
	 * <dt>Possible values:</dt>
	 * <dd><code>{ "error", "warning", "info", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"warning"</code></dd>
	 * </dl>
	 *
	 * @since 3.14
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_UNSTABLE_AUTO_MODULE_NAME = PLUGIN_ID + ".compiler.problem.unstableAutoModuleName"; //$NON-NLS-1$

	/**
	 * Compiler option ID: Reporting when a {@code @SuppressWarnings} annotation might be unused, but exact information is not available.
	 * <p>
	 * This issue occurs when a suppress warnings token (like, e.g., {@code "unused"}) represents a group of problems,
	 * and some of the problems in that group are currently disabled (configured as "ignore").
	 * In this situation the compiler may not know if none of the problems in that group could be found within the
	 * annotated code section.
	 * <p>
	 * When enabled, the compiler will issue an error, warning or info when a {@code @SuppressWarnings} annotation
	 * was not observed to be necessary, but analysis of the suppressed group of problems was incomplete.
	 *
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.suppressWarningsNotFullyAnalysed"</code></dd>
	 * <dt>Possible values:</dt>
	 * <dd><code>{ "error", "warning", "info", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"info"</code></dd>
	 * </dl>
	 *
	 * @since 3.20
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_SUPPRESS_WARNINGS_NOT_FULLY_ANALYSED = PLUGIN_ID + ".compiler.problem.suppressWarningsNotFullyAnalysed"; //$NON-NLS-1$

	/**
	 * Compiler option ID: Annotation-based Null Analysis.
	 * <p>This option controls whether the compiler will use null annotations for
	 *    improved analysis of (potential) null references.</p>
	 * <p>When enabled, the compiler will interpret the annotation types defined using
	 *    {@link #COMPILER_NONNULL_ANNOTATION_NAME} and {@link #COMPILER_NULLABLE_ANNOTATION_NAME}
	 *    as specifying whether or not a given type includes the value <code>null</code>.</p>
	 * <p>The effect of these analyses is further controlled by the options
	 *    {@link #COMPILER_PB_NULL_SPECIFICATION_VIOLATION},
	 *    {@link #COMPILER_PB_NULL_ANNOTATION_INFERENCE_CONFLICT} and
	 *    {@link #COMPILER_PB_NULL_UNCHECKED_CONVERSION}.
	 * </p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.annotation.nullanalysis"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "disabled", "enabled" }</code></dd>
	 * <dt>Default:</dt><dd><code>"disabled"</code></dd>
	 * </dl>
	 * @since 3.8
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_ANNOTATION_NULL_ANALYSIS = PLUGIN_ID + ".compiler.annotation.nullanalysis"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Name of Annotation Type for Nullable Types.
	 * <p>This option defines a fully qualified Java type name that the compiler may use
	 *    to perform special null analysis.</p>
	 * <p>If the annotation specified by this option is applied to a type in a method
	 *    signature or variable declaration, this will be interpreted as a specification
	 *    that <code>null</code> is a legal value in that position. Currently supported
	 *    positions are: method parameters, method return type, fields and local variables.</p>
	 * <p>If a value whose type
	 *    is annotated with this annotation is dereferenced without checking for null,
	 *    the compiler will trigger a diagnostic as further controlled by
	 *    {@link #COMPILER_PB_POTENTIAL_NULL_REFERENCE}.</p>
	 * <p>The compiler may furthermore check adherence to the null specification as
	 *    further controlled by {@link #COMPILER_PB_NULL_SPECIFICATION_VIOLATION},
	 *    {@link #COMPILER_PB_NULL_ANNOTATION_INFERENCE_CONFLICT} and
	 *    {@link #COMPILER_PB_NULL_UNCHECKED_CONVERSION}.</p>
	 * <p>This option only has an effect if the option {@link #COMPILER_ANNOTATION_NULL_ANALYSIS} is enabled.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.annotation.nullable"</code></dd>
	 * <dt>Possible values:</dt><dd>any legal, fully qualified Java type name; must resolve to an annotation type.</dd>
	 * <dt>Default:</dt><dd><code>"org.eclipse.jdt.annotation.Nullable"</code></dd>
	 * </dl>
	 * @since 3.8
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_NULLABLE_ANNOTATION_NAME = PLUGIN_ID + ".compiler.annotation.nullable"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Names of Secondary Annotation Types for Nullable Types.
	 * <p>This option defines a comma-separated list of fully qualified Java type names
	 *    that the compiler may use to perform special null analysis.</p>
	 * <p>The annotation types identified by the names in this list are interpreted in the same way
	 *    as the annotation identified by {@link #COMPILER_NULLABLE_ANNOTATION_NAME}.
	 *    The intention is to support libraries using different sets of null annotations,
	 *    in addition to those used by the current project. Secondary null annotations should not be
	 *    used in the project's own source code.</p>
	 * <p>JDT will never actively use any secondary annotation names from this list,
	 *    i.e., inferred null annotations and content assist proposals mentioning null annotations
	 *    are always rendered using the primary name from {@link #COMPILER_NULLABLE_ANNOTATION_NAME}.</p>
	 * <p>This option only has an effect if the option {@link #COMPILER_ANNOTATION_NULL_ANALYSIS} is enabled.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.annotation.nullable.secondary"</code></dd>
	 * <dt>Possible values:</dt><dd>a comma-separated list of legal, fully qualified Java type names;
	 *     each name in the list must resolve to an annotation type.</dd>
	 * <dt>Default:</dt><dd><code>""</code></dd>
	 * </dl>
	 * @since 3.12
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_NULLABLE_ANNOTATION_SECONDARY_NAMES = PLUGIN_ID + ".compiler.annotation.nullable.secondary"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Name of Annotation Type for Non-Null Types.
	 * <p>This option defines a fully qualified Java type name that the compiler may use
	 *    to perform special null analysis.</p>
	 * <p>If the annotation specified by this option is applied to a type in a method
	 *    signature or variable declaration, this will be interpreted as a specification
	 *    that <code>null</code> is <b>not</b> a legal value in that position. Currently
	 *    supported positions are: method parameters, method return type, fields and local variables.</p>
	 * <p>For values declared with this annotation, the compiler will never trigger a null
	 *    reference diagnostic (as controlled by {@link #COMPILER_PB_POTENTIAL_NULL_REFERENCE}
	 *    and {@link #COMPILER_PB_NULL_REFERENCE}), because the assumption is made that null
	 *    will never occur at runtime in these positions.</p>
	 * <p>The compiler may furthermore check adherence to the null specification as further
	 *    controlled by {@link #COMPILER_PB_NULL_SPECIFICATION_VIOLATION},
	 *    {@link #COMPILER_PB_NULL_ANNOTATION_INFERENCE_CONFLICT} and
	 *    {@link #COMPILER_PB_NULL_UNCHECKED_CONVERSION}.</p>
	 * <p>This option only has an effect if the option {@link #COMPILER_ANNOTATION_NULL_ANALYSIS} is enabled.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.annotation.nonnull"</code></dd>
	 * <dt>Possible values:</dt><dd>any legal, fully qualified Java type name; must resolve to an annotation type.</dd>
	 * <dt>Default:</dt><dd><code>"org.eclipse.jdt.annotation.NonNull"</code></dd>
	 * </dl>
	 * @since 3.8
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_NONNULL_ANNOTATION_NAME = PLUGIN_ID + ".compiler.annotation.nonnull"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Names of Secondary Annotation Types for Non-Null Types.
	 * <p>This option defines a comma-separated list of fully qualified Java type names
	 *    that the compiler may use to perform special null analysis.</p>
	 * <p>The annotation types identified by the names in this list are interpreted in the same way
	 *    as the annotation identified by {@link #COMPILER_NONNULL_ANNOTATION_NAME}.
	 *    The intention is to support libraries using different sets of null annotations,
	 *    in addition to those used by the current project. Secondary null annotations should not be
	 *    used in the project's own source code.</p>
	 * <p>JDT will never actively use any secondary annotation names from this list,
	 *    i.e., inferred null annotations and content assist proposals mentioning null annotations
	 *    are always rendered using the primary name from {@link #COMPILER_NONNULL_ANNOTATION_NAME}.</p>
	 * <p>This option only has an effect if the option {@link #COMPILER_ANNOTATION_NULL_ANALYSIS} is enabled.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.annotation.nonnull.secondary"</code></dd>
	 * <dt>Possible values:</dt><dd>a comma-separated list of legal, fully qualified Java type names;
	 *     each name in the list must resolve to an annotation type.</dd>
	 * <dt>Default:</dt><dd><code>""</code></dd>
	 * </dl>
	 * @since 3.12
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_NONNULL_ANNOTATION_SECONDARY_NAMES = PLUGIN_ID + ".compiler.annotation.nonnull.secondary"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Name of Annotation Type to specify a nullness default for unannotated types.
	 * <p>This option defines a fully qualified Java type name that the compiler may use
	 *    to perform special null analysis.</p>
	 * <p>If the annotation is applied without an argument, all unannotated types in method signatures
	 *    and field declarations within the annotated element will be treated as if they were specified
	 *    with the non-null annotation (see {@link #COMPILER_NONNULL_ANNOTATION_NAME}).</p>
	 * <p>If the annotation is applied with the constant <code>false</code> as its argument
	 *    all corresponding defaults at outer scopes will be canceled for the annotated element.</p>
	 * <p>This option only has an effect if the option {@link #COMPILER_ANNOTATION_NULL_ANALYSIS} is enabled.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.annotation.nonnullbydefault"</code></dd>
	 * <dt>Possible values:</dt><dd>any legal, fully qualified Java type name; must resolve to an annotation type.
	 *     That annotation type should have exactly one boolean parameter.</dd>
	 * <dt>Default:</dt><dd><code>"org.eclipse.jdt.annotation.NonNullByDefault"</code></dd>
	 * </dl>
	 * @since 3.8
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_NONNULL_BY_DEFAULT_ANNOTATION_NAME = PLUGIN_ID + ".compiler.annotation.nonnullbydefault"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Names of Secondary Annotation Types to specify a nullness default for unannotated types.
	 * <p>This option defines a comma-separated list of fully qualified Java type names
	 *    that the compiler may use to perform special null analysis.</p>
	 * <p>The annotation types identified by the names in this list are interpreted in the same way
	 *    as the annotation identified by {@link #COMPILER_NONNULL_BY_DEFAULT_ANNOTATION_NAME}.
	 *    The intention is to support libraries using different sets of null annotations,
	 *    in addition to those used by the current project. Secondary null annotations should not be
	 *    used in the project's own source code.</p>
	 * <p>This option only has an effect if the option {@link #COMPILER_ANNOTATION_NULL_ANALYSIS} is enabled.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.annotation.nonnullbydefault.secondary"</code></dd>
	 * <dt>Possible values:</dt><dd>a comma-separated list of legal, fully qualified Java type names;
	 *     each name in the list must resolve to an annotation type.</dd>
	 * <dt>Default:</dt><dd><code>""</code></dd>
	 * </dl>
	 * @since 3.12
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_NONNULL_BY_DEFAULT_ANNOTATION_SECONDARY_NAMES = PLUGIN_ID + ".compiler.annotation.nonnullbydefault.secondary"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting missing default nullness annotation.
	 * <p>When enabled, the compiler will issue an error or a warning in the following cases:</p>
	 * <ul>
	 * <li> When a package does not contain a default nullness annotation, as a result of missing package-info.java
	 * or missing default nullness annotation in package-info.java.</li>
	 * <li> When a type inside a default package does not contain a default nullness annotation.</li>
	 * </ul>
	 * <p>This option only has an effect if the option {@link #COMPILER_ANNOTATION_NULL_ANALYSIS} is enabled.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.annotation.missingNonNullByDefaultAnnotation"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning", "info", "ignore" }</code>.</dd>
	 * <dt>Default:</dt><dd><code>"ignore"</code></dd>
	 * </dl>
	 * @since 3.8
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_MISSING_NONNULL_BY_DEFAULT_ANNOTATION = PLUGIN_ID + ".compiler.annotation.missingNonNullByDefaultAnnotation"; //$NON-NLS-1$
	/**
	 * Core option ID: Read external annotations from all build path entries.
	 * <p>This option controls where the compiler will look for external annotations for enhanced null analysis</p>
	 * <p>When enabled, the compiler will search all buildpath entries of a given project to locate external annotation files
	 * 		({@code .eea}) in order to superimpose null annotations over classes read from dependencies.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.builder.annotationPath.allLocations"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "disabled", "enabled" }</code></dd>
	 * <dt>Default:</dt><dd><code>"disabled"</code></dd>
	 * </dl>
	 * @since 3.27
	 * @category CoreOptionID
	 */
	public static final String CORE_JAVA_BUILD_EXTERNAL_ANNOTATIONS_FROM_ALL_LOCATIONS = PLUGIN_ID + ".builder.annotationPath.allLocations"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting Violations of Null Specifications.
	 * <p>Depending on this option, the compiler will issue either an error or a warning
	 *    whenever one of the following situations is detected:</p>
	 *    <ol>
	 *    <li>A method declared with a nonnull annotation returns a
	 *        <em>nullable</em> expression.</li>
	 *    <li>A <em>nullable</em> expression is passed
     *        as an argument in a method call where the corresponding parameter of the called
     *        method is declared with a nonnull annotation.</li>
	 *    <li>A <em>nullable</em> expression is assigned
     *        to a local variable that is declared with a nonnull annotation.</li>
	 *    <li>A method that overrides an inherited method declared with a nonnull annotation
	 *        tries to relax that contract by specifying a nullable annotation
	 *        (prohibition of contravariant return).</li>
	 *    <li>A method that overrides an inherited method which has a nullable declaration
	 *        for at least one of its parameters, tries to tighten that null contract by
	 *        specifying a nonnull annotation for its corresponding parameter
	 *        (prohibition of covariant parameters).</li>
	 *    <li>A non-static field with a nonnull annotation is not definitely assigned at
	 *        the end of each constructor.</li>
	 *    <li>A static field with a nonnull annotation is not definitely assigned in static initializers.</li>
	 *    </ol>
	 *    In the above an expression is considered as <em>nullable</em> if
	 *    either it is statically known to evaluate to the value <code>null</code>, or if it is
	 *    declared with a nullable annotation.
	 * <p>The compiler options {@link #COMPILER_NONNULL_ANNOTATION_NAME} and
	 *    {@link #COMPILER_NULLABLE_ANNOTATION_NAME} control which annotations the compiler
	 *    shall interpret as nonnull or nullable annotations, respectively.
	 * </p>
	 * <p>This option only has an effect if the option {@link #COMPILER_ANNOTATION_NULL_ANALYSIS} is enabled.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.nullSpecViolation"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning" }</code></dd>
	 * <dt>Default:</dt><dd><code>"error"</code></dd>
	 * </dl>
	 * @since 3.8
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_NULL_SPECIFICATION_VIOLATION = PLUGIN_ID + ".compiler.problem.nullSpecViolation"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting conflicts between declared null annotation and inferred null value
	 * <p>When enabled, the compiler will issue an error or a warning whenever one of the
	 *    following situations is detected:</p>
	 *    <ol>
	 *    <li>A method declared with a nonnull annotation returns an expression that is
	 *          statically known to evaluate to a null value on some flow.</li>
	 *    <li>An expression that is statically known to evaluate to a null value on some flow
	 *        is passed as an argument in a method call where the corresponding parameter of
	 *        the called method is declared with a nonnull annotation.</li>
	 *    <li>An expression that is statically known to evaluate to a null value on some flow
	 *        is assigned to a local variable that is declared with a nonnull annotation.</li>
	 *    </ol>
	 * <p>The compiler options {@link #COMPILER_NONNULL_ANNOTATION_NAME} and
	 *    {@link #COMPILER_NULLABLE_ANNOTATION_NAME} control which annotations the compiler
	 *    shall interpret as nonnull or nullable annotations, respectively.
	 * </p>
	 * <p>This option only has an effect if the option {@link #COMPILER_ANNOTATION_NULL_ANALYSIS} is enabled.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.nullAnnotationInferenceConflict"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning", "info", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"error"</code></dd>
	 * </dl>
	 * @since 3.8
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_NULL_ANNOTATION_INFERENCE_CONFLICT = PLUGIN_ID + ".compiler.problem.nullAnnotationInferenceConflict"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting unchecked conversion from a type with unknown nullness to a null annotated type
	 * <p>When enabled, the compiler will issue an error or a warning whenever one of the
	 *    following situations is detected:</p>
	 *    <ol>
	 *    <li>A method declared with a nonnull annotation returns an expression for which
	 *        insufficient nullness information is available for statically proving that no
	 *        flow will pass a null value at runtime.</li>
	 *    <li>An expression for which insufficient nullness information is available for
	 *        statically proving that it will never evaluate to a null value at runtime
	 *        is passed as an argument in a method call where the corresponding parameter of
	 *        the called method is declared with a nonnull annotation.</li>
	 *    <li>An expression for which insufficient nullness information is available for
	 *        statically proving that it will never evaluate to a null value at runtime
	 *        is assigned to a local variable that is declared with a nonnull annotation.</li>
	 *    </ol>
	 * <p>Unchecked null conversion is usually a consequence of using other unannotated
	 *    variables or methods.</p>
	 * <p>The compiler options {@link #COMPILER_NONNULL_ANNOTATION_NAME} and
	 *    {@link #COMPILER_NULLABLE_ANNOTATION_NAME} control which annotations the compiler
	 *    shall interpret as nonnull or nullable annotations, respectively.
	 * </p>
	 * <p>This option only has an effect if the option {@link #COMPILER_ANNOTATION_NULL_ANALYSIS} is enabled.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.nullUncheckedConversion"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning", "info", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"warning"</code></dd>
	 * </dl>
	 * @since 3.8
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_NULL_UNCHECKED_CONVERSION = PLUGIN_ID + ".compiler.problem.nullUncheckedConversion"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting problems detected by pessimistic null analysis for free type variables.
	 * <p>Unless set to <code>"ignore"</code>, type variables not affected by any explicit null annotation are pessimistically analyzed
	 * in two directions: When reading a value of this type, it is assumed to be nullable. When this type appears as the required type
	 * (i.e., at the left hand side of an assignment or variable initialization, or as the method return type against which a return statement
	 * is being checked) the type is considered to require the nonnull property.</p>
	 * <p>Problems reported due to this pessimistic analysis are reported with the level given in this option.</p>
	 * @since 3.12
	 * @category CompilerOptionID
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.pessimisticNullAnalysisForFreeTypeVariables"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning", "info", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"warning"</code></dd>
	 * </dl>
	 */
	public static final String COMPILER_PB_PESSIMISTIC_NULL_ANALYSIS_FOR_FREE_TYPE_VARIABLES = PLUGIN_ID + ".compiler.problem.pessimisticNullAnalysisForFreeTypeVariables"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting Redundant Null Annotations.
	 * <p>When enabled, the compiler will issue an error or a warning when a non-null annotation
	 *    (see {@link #COMPILER_NONNULL_ANNOTATION_NAME})
	 *    is applied although the same effect is already achieved by a default applicable at the
	 *    current location. Such a default may be set by using the annotation specified by the option
	 *    {@link #COMPILER_NONNULL_BY_DEFAULT_ANNOTATION_NAME}.
	 * </p>
	 * <p>This option only has an effect if the option {@link #COMPILER_ANNOTATION_NULL_ANALYSIS} is enabled.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.redundantNullAnnotation"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning", "info", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"warning"</code></dd>
	 * </dl>
	 * @since 3.8
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_REDUNDANT_NULL_ANNOTATION = PLUGIN_ID + ".compiler.problem.redundantNullAnnotation"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Perform syntactic null analysis for fields.
	 * <p>When enabled, the compiler will detect certain syntactic constellations where a null
	 *	  related warning against a field reference would normally be raised but can be suppressed
	 *    at low risk given that the same field reference was known to be non-null immediately before.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.syntacticNullAnalysisForFields"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "disabled", "enabled" }</code></dd>
	 * <dt>Default:</dt><dd><code>"disabled"</code></dd>
	 * </dl>
	 * @since 3.9
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_SYNTACTIC_NULL_ANALYSIS_FOR_FIELDS = JavaCore.PLUGIN_ID+".compiler.problem.syntacticNullAnalysisForFields"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Inheritance of null annotations.
	 * <p>When enabled, the compiler will check for each method without any explicit null annotations:
	 *    If it overrides a method which has null annotations, it will treat the
	 *    current method as if it had the same annotations as the overridden method.</p>
	 * <p>Annotation inheritance will use the <em>effective</em> nullness of the overridden method
	 *    after transitively applying inheritance and after applying any default nullness
	 *    (see {@link #COMPILER_NONNULL_BY_DEFAULT_ANNOTATION_NAME}) at the site of the overridden method.</p>
	 * <p>If different implicit null annotations (from a nonnull default and/or overridden methods) are applicable
	 *    to the same type in a method signature, this is flagged as an error
	 *    and an explicit null annotation must be used to disambiguate.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.annotation.inheritNullAnnotations"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "disabled", "enabled" }</code></dd>
	 * <dt>Default:</dt><dd><code>"disabled"</code></dd>
	 * </dl>
	 * @since 3.9
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_INHERIT_NULL_ANNOTATIONS = JavaCore.PLUGIN_ID+".compiler.annotation.inheritNullAnnotations"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting Dropped Nonnull Parameter Annotations.
	 * <p>When enabled, the compiler will issue an error or a warning against a parameter of
	 *    a method that overrides an inherited method
	 *    if all of the following hold:</p>
	 * <ul>
	 *    <li>The overridden method declares the corresponding parameter as non-null (see {@link #COMPILER_NONNULL_ANNOTATION_NAME}).</li>
	 *    <li>The parameter in the overriding method has no null annotation.</li>
	 *    <li>The overriding method is not affected by a nullness default (see {@link #COMPILER_NONNULL_BY_DEFAULT_ANNOTATION_NAME}).</li>
	 *    <li>Inheritance of null annotations is disabled (see {@link #COMPILER_INHERIT_NULL_ANNOTATIONS}).</li>
	 * </ul>
	 * <p>This particular situation bears the same inherent risk as any unannotated method parameter,
	 *    because the compiler's null ananysis cannot decide wither <code>null</code> is or is not a legal value for this parameter.
	 *    However, the annotation in the overridden method <em>suggests</em> that the parameter should also be annotated as non-null.
	 *    If that is not intended or possible, it is recommended to annotate the parameter as nullable,
	 *    in order to make this (legal) change of contract explicit.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.nonnullParameterAnnotationDropped"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning", "info", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"warning"</code></dd>
	 * </dl>
	 * @since 3.9
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_NONNULL_PARAMETER_ANNOTATION_DROPPED = JavaCore.PLUGIN_ID+".compiler.problem.nonnullParameterAnnotationDropped"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting Unsafe NonNull Interpretation Of Type Variables.
	 * <p>When enabled, the compiler will issue an error or a warning against a method call
	 *    if all of the following hold:</p>
	 * <ul>
	 *    <li>The method's declared return type is a type variable without any null annotation.</li>
	 *    <li>For the given invocation this type variable is substituted with a nonnull type.</li>
	 *    <li>The type declaring the method is provided by a third-party library.</li>
	 *    <li>No null annotations exist for this library type, neither in its class file nor using external annotations.</li>
	 * </ul>
	 * <p>This particular situation leverages the option to consistently substitute all occurrences of a type variable
	 *  with a nonnull type, but it bears the risk that the library type may not be aware of null annotations thus lacking
	 *  a necessary <code>@Nullable</code> annotation for a particular occurrence of a type variable.</p>
	 * <p>This option only has an effect if the option {@link #COMPILER_ANNOTATION_NULL_ANALYSIS} is enabled and when
	 *  the configured set of null annotations declares the target <code>TYPE_USE</code></p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.nonnullTypeVariableFromLegacyInvocation"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning", "info", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"warning"</code></dd>
	 * </dl>
	 * @since 3.12
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_NONNULL_TYPEVAR_FROM_LEGACY_INVOCATION = JavaCore.PLUGIN_ID+".compiler.problem.nonnullTypeVariableFromLegacyInvocation"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting Unsafe Conversion To Unannotated Type Argument.
	 * <p>When enabled, the compiler will issue an error, warning or info when a value of a parameterized type
	 * with annotated type arguments is assigned to a variable / bound to a method argument, where the corresponding
	 * type argument is unannotated.</p>
	 * <p>This situation is problematic because it will enable using the less-annotated type to manipulate the given
	 * objects in ways that may violate contracts of the more-annotated type.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.annotatedTypeArgumentToUnannotated"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning", "info", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"info"</code></dd>
	 * </dl>
	 * @since 3.21
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_ANNOTATED_TYPE_ARGUMENT_TO_UNANNOTATED = JavaCore.PLUGIN_ID+".compiler.problem.annotatedTypeArgumentToUnannotated"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Setting Source Compatibility Mode.
	 * <p>Specify whether which source level compatibility is used. From 1.4 on, <code>'assert'</code> is a keyword
	 *    reserved for assertion support. Also note, than when toggling to 1.4 mode, the target VM
	 *    level should be set to <code>"1.4"</code> and the compliance mode should be <code>"1.4"</code>.</p>
	 * <p>Source level 1.5 is necessary to enable generics, autoboxing, covariance, annotations, enumerations
	 *    enhanced for loop, static imports and varargs.</p>
	 * <p>In source levels <code>"1.5"</code> and higher, the compliance and target settings should be
	 *    set to the same version as the source level.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.source"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "1.3", "1.4", "1.5", "1.6", "1.7", "1.8", "9", "10", "11" }</code></dd>
	 * <dt>Default:</dt><dd><code>"1.3"</code></dd>
	 * </dl>
	 * @since 2.0
	 * @category CompilerOptionID
	 * @see #COMPILER_COMPLIANCE
	 * @see #COMPILER_CODEGEN_TARGET_PLATFORM
	 * @see #setComplianceOptions(String, Map)
	 */
	public static final String COMPILER_SOURCE = PLUGIN_ID + ".compiler.source"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Setting Compliance Level.
	 * <p>Select the compliance level for the compiler.
	 *    {@link #COMPILER_SOURCE} and {@link #COMPILER_CODEGEN_TARGET_PLATFORM} settings cannot be
	 *    higher than the compiler compliance level. In <code>"1.5"</code> and higher compliance, source and target settings
	 *    should match the compliance setting.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.compliance"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "1.3", "1.4", "1.5", "1.6", "1.7", "1.8", "9", "10", "11" }</code></dd>
	 * <dt>Default:</dt><dd><code>"1.4"</code></dd>
	 * </dl>
	 * @since 2.0
	 * @category CompilerOptionID
	 * @see #COMPILER_SOURCE
	 * @see #COMPILER_CODEGEN_TARGET_PLATFORM
	 * @see #setComplianceOptions(String, Map)
	 */
	public static final String COMPILER_COMPLIANCE = PLUGIN_ID + ".compiler.compliance"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Use system libraries from release.
	 * <p>When enabled, the compiler will compile against the system libraries from release
	 * of the specified compliance level</p>
	 * <p>Setting this option sets the {@link #COMPILER_CODEGEN_TARGET_PLATFORM}) and {@link #COMPILER_SOURCE} to
	 * the same level as the compiler compliance. This option is available to a project only when a supporting
	 * JDK is found in the project's build path</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.release"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "enabled", "disabled" }</code></dd>
	 * <dt>Default:</dt><dd><code>"disabled"</code></dd>
	 * </dl>
	 * @since 3.14
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_RELEASE = PLUGIN_ID + ".compiler.release"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Defining the Automatic Task Priorities.
	 * <p>In parallel with the Automatic Task Tags, this list defines the priorities (high, normal or low)
	 *    of the task markers issued by the compiler.
	 *    If the default is specified, the priority of each task marker is <code>"NORMAL"</code>.</p>
	 * <p>Task Priorities and task tags must have the same length. If task priorities are set, then task tags should also
	 * be set.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.taskPriorities"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "&lt;priority&gt;[,&lt;priority&gt;]*" }</code> where <code>&lt;priority&gt;</code> is one of <code>"HIGH"</code>, <code>"NORMAL"</code> or <code>"LOW"</code></dd>
	 * <dt>Default:</dt><dd><code>"NORMAL,HIGH,NORMAL"</code></dd>
	 * </dl>
	 * @since 2.1
	 * @category CompilerOptionID
	 * @see #COMPILER_TASK_TAGS
	 */
	public static final String COMPILER_TASK_PRIORITIES = PLUGIN_ID + ".compiler.taskPriorities"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Defining the Automatic Task Tags.
	 * <p>When the tag list is not empty, the compiler will issue a task marker whenever it encounters
	 *    one of the corresponding tags inside any comment in Java source code.</p>
	 * <p>Generated task messages will start with the tag, and range until the next line separator,
	 *    comment ending, or tag.</p>
	 * <p>When a given line of code bears multiple tags, each tag will be reported separately.
	 *    Moreover, a tag immediately followed by another tag will be reported using the contents of the
	 *    next non-empty tag of the line, if any.</p>
	 * <p>Note that tasks messages are trimmed. If a tag is starting with a letter or digit, then it cannot be leaded by
	 *    another letter or digit to be recognized (<code>"fooToDo"</code> will not be recognized as a task for tag <code>"ToDo"</code>, but <code>"foo#ToDo"</code>
	 *    will be detected for either tag <code>"ToDo"</code> or <code>"#ToDo"</code>). Respectively, a tag ending with a letter or digit cannot be followed
	 *    by a letter or digit to be recognized (<code>"ToDofoo"</code> will not be recognized as a task for tag <code>"ToDo"</code>, but <code>"ToDo:foo"</code> will
	 *    be detected either for tag <code>"ToDo"</code> or <code>"ToDo:"</code>).</p>
	 * <p>Task Priorities and task tags must have the same length. If task tags are set, then task priorities should also
	 * be set.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.taskTags"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "&lt;tag&gt;[,&lt;tag&gt;]*" }</code> where <code>&lt;tag&gt;</code> is a String without any wild-card or leading/trailing spaces</dd>
	 * <dt>Default:</dt><dd><code>"TODO,FIXME,XXX"</code></dd>
	 * </dl>
	 * @since 2.1
	 * @category CompilerOptionID
	 * @see #COMPILER_TASK_PRIORITIES
	 */
	public static final String COMPILER_TASK_TAGS = PLUGIN_ID + ".compiler.taskTags"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Determining whether task tags are case-sensitive.
	 * <p>When enabled, task tags are considered in a case-sensitive way.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.taskCaseSensitive"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "enabled", "disabled" }</code></dd>
	 * <dt>Default:</dt><dd><code>"enabled"</code></dd>
	 * </dl>
	 * @since 3.0
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_TASK_CASE_SENSITIVE = PLUGIN_ID + ".compiler.taskCaseSensitive"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting Forbidden Reference to Type with Restricted Access.
	 * <p>When enabled, the compiler will issue an error or a warning when referring to a type that is non accessible, as defined according
	 *    to the access rule specifications.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.forbiddenReference"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning", "info", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"error"</code></dd>
	 * </dl>
	 * @since 3.1
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_FORBIDDEN_REFERENCE = PLUGIN_ID + ".compiler.problem.forbiddenReference"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting Discouraged Reference to Type with Restricted Access.
	 * <p>When enabled, the compiler will issue an error or a warning when referring to a type with discouraged access, as defined according
	 *    to the access rule specifications.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.discouragedReference"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning", "info", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"warning"</code></dd>
	 * </dl>
	 * @since 3.1
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_DISCOURAGED_REFERENCE = PLUGIN_ID + ".compiler.problem.discouragedReference"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Determining Effect of <code>@SuppressWarnings</code>.
	 * <p>When enabled, the <code>@SuppressWarnings</code> annotation can be used to suppress some compiler warnings.</p>
	 * <p>When disabled, all <code>@SupressWarnings</code> annotations are ignored; i.e., warnings are reported.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.suppressWarnings"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "enabled", "disabled" }</code></dd>
	 * <dt>Default:</dt><dd><code>"enabled"</code></dd>
	 * </dl>
	 * @since 3.1
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_SUPPRESS_WARNINGS = PLUGIN_ID + ".compiler.problem.suppressWarnings"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Raise null related errors or warnings arising because of assert statements.
	 * <p>When enabled, the compiler will flag all null related errors or warnings that have been enabled by the user,
	 *    irrespective of whether a variable occurred in an assert statement.</p>
	 * <p>When disabled, the compiler will not flag null related errors or warnings on variables that got marked as maybe or definitely
	 *    <code>null</code> in an assert statement upstream.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.includeNullInfoFromAsserts"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "enabled", "disabled" }</code></dd>
	 * <dt>Default:</dt><dd><code>"disabled"</code></dd>
	 * </dl>
	 * @since 3.7
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_INCLUDE_ASSERTS_IN_NULL_ANALYSIS = PLUGIN_ID + ".compiler.problem.includeNullInfoFromAsserts"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Further Determining the Effect of <code>@SuppressWarnings</code> if also
	 * {@link #COMPILER_PB_SUPPRESS_WARNINGS} is enabled.
	 * <p>When enabled, the <code>@SuppressWarnings</code> annotation can additionally be used to suppress
	 * optional compiler diagnostics that have been configured as {@link #ERROR}.</p>
	 * <p>When disabled, all <code>@SuppressWarnings</code> annotations only affects warnings.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.suppressOptionalErrors"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "enabled", "disabled" }</code></dd>
	 * <dt>Default:</dt><dd><code>"disabled"</code></dd>
	 * </dl>
	 * @since 3.6
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_SUPPRESS_OPTIONAL_ERRORS = PLUGIN_ID + ".compiler.problem.suppressOptionalErrors"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting Unhandled Warning Token for <code>@SuppressWarnings</code>.
	 * <p>When enabled, the compiler will issue an error or a warning when encountering a token
	 *    it cannot handle inside a <code>@SuppressWarnings</code> annotation.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.unhandledWarningToken"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning", "info", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"warning"</code></dd>
	 * </dl>
	 * @since 3.1
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_UNHANDLED_WARNING_TOKEN = PLUGIN_ID + ".compiler.problem.unhandledWarningToken"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting Unnecessary <code>@SuppressWarnings</code>.
	 * <p>When enabled, the compiler will issue an error or a warning when encountering <code>@SuppressWarnings</code> annotation
	 *    for which no corresponding warning got detected in the code. This diagnostic is provided to help developers to get
	 *    rid of transient <code>@SuppressWarnings</code> no longer needed. Note that <code>@SuppressWarnings("all")</code> is still
	 *    silencing the warning for unnecessary <code>@SuppressWarnings</code>, as it is the master switch to silence ALL warnings.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.unusedWarningToken"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning", "info", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"warning"</code></dd>
	 * </dl>
	 * @since 3.4
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_UNUSED_WARNING_TOKEN = PLUGIN_ID + ".compiler.problem.unusedWarningToken"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting Null Dereference.
	 * <p>When enabled, the compiler will issue an error or a warning whenever a
	 *    variable that is statically known to hold a null value is used to
	 *    access a field or method.</p>
	 * <p>Assert statements are ignored unless {@link #COMPILER_PB_INCLUDE_ASSERTS_IN_NULL_ANALYSIS}
	 *    is enabled.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.nullReference"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning", "info", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"warning"</code></dd>
	 * </dl>
	 * @since 3.2
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_NULL_REFERENCE = PLUGIN_ID + ".compiler.problem.nullReference"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting Potential Null Dereference.
	 * <p>When enabled, the compiler will issue an error or a warning whenever a
	 *    variable that has formerly been tested against null but is not (no more)
	 *    statically known to hold a non-null value is used to access a field or
	 *    method.</p>
	 * <p>Assert statements are ignored unless {@link #COMPILER_PB_INCLUDE_ASSERTS_IN_NULL_ANALYSIS}
	 *    is enabled.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.potentialNullReference"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning", "info", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"ignore"</code></dd>
	 * </dl>
	 * @since 3.3
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_POTENTIAL_NULL_REFERENCE = PLUGIN_ID + ".compiler.problem.potentialNullReference"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting Redundant Null Check.
	 * <p>When enabled, the compiler will issue an error or a warning whenever a
	 *    variable that is statically known to hold a null or a non-null value
	 *    is tested against null.</p>
	 * <p>Assert statements are ignored unless {@link #COMPILER_PB_INCLUDE_ASSERTS_IN_NULL_ANALYSIS}
	 *    is enabled.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.redundantNullCheck"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning", "info", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"ignore"</code></dd>
	 * </dl>
	 * @since 3.3
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_REDUNDANT_NULL_CHECK = PLUGIN_ID + ".compiler.problem.redundantNullCheck"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting Overriding method that doesn't call the super method invocation.
	 * <p>When enabled, the compiler will issue an error or a warning if a method is overriding a method without calling
	 *    the super invocation.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.overridingMethodWithoutSuperInvocation"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning", "info", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"ignore"</code></dd>
	 * </dl>
	 * @since 3.3
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_OVERRIDING_METHOD_WITHOUT_SUPER_INVOCATION = PLUGIN_ID + ".compiler.problem.overridingMethodWithoutSuperInvocation"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting Redundant Superinterface.
	 * <p>When enabled, the compiler will issue an error or a warning if a type
	 *    explicitly implements an interface that is already implemented by any
	 *    of its supertypes.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.redundantSuperinterface"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning", "info", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"ignore"</code></dd>
	 * </dl>
	 * @since 3.4
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_REDUNDANT_SUPERINTERFACE = PLUGIN_ID + ".compiler.problem.redundantSuperinterface"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting Comparison of Identical Expressions.
	 * <p>When enabled, the compiler will issue an error or a warning if a comparison
	 * is involving identical operands (e.g <code>'x == x'</code>).</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.comparingIdentical"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning", "info", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"warning"</code></dd>
	 * </dl>
	 * @since 3.5
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_COMPARING_IDENTICAL = PLUGIN_ID + ".compiler.problem.comparingIdentical"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting Missing Synchronized Modifier On Inherited Method.
	 * <p>When enabled, the compiler will issue an error or a warning if a method
	 * overrides a synchronized method without having a synchronized modifier.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.missingSynchronizedOnInheritedMethod"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning", "info", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"ignore"</code></dd>
	 * </dl>
	 * @since 3.5
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_MISSING_SYNCHRONIZED_ON_INHERITED_METHOD = PLUGIN_ID + ".compiler.problem.missingSynchronizedOnInheritedMethod"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting Allocation of an Unused Object.
	 * <p>When enabled, the compiler will issue an error or a warning if an object is allocated but never used,
	 * neither by holding a reference nor by invoking one of the object's methods.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.unusedObjectAllocation"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning", "info", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"ignore"</code></dd>
	 * </dl>
	 * @since 3.6
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_UNUSED_OBJECT_ALLOCATION = PLUGIN_ID + ".compiler.problem.unusedObjectAllocation";  //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting redundant specification of type arguments in class instance creation expressions.
	 * <p>When enabled, the compiler will issue an error or a warning if type arguments are used in a class instance creation,
	 * when the '&lt;&gt;' operator can be used instead.</p>
	 * <p>This option only has an effect if the compiler compliance is 1.7 or greater.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.redundantSpecificationOfTypeArguments"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning", "info", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"ignore"</code></dd>
	 * </dl>
	 * @since 3.7.1
	 * @category CompilerOptionID
	 */
	public static final String COMPILER_PB_REDUNDANT_TYPE_ARGUMENTS = PLUGIN_ID + ".compiler.problem.redundantSpecificationOfTypeArguments";  //$NON-NLS-1$
	/**
	 * Core option ID: Computing Project Build Order.
	 * <p>Indicate whether JavaCore should enforce the project build order to be based on
	 *    the classpath prerequisite chain. When requesting to compute, this takes over
	 *    the platform default order (based on project references).</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.computeJavaBuildOrder"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "compute", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"ignore"</code></dd>
	 * </dl>
	 * @category CoreOptionID
	 */
	public static final String CORE_JAVA_BUILD_ORDER = PLUGIN_ID + ".computeJavaBuildOrder"; //$NON-NLS-1$
	/**
	 * Core option ID: Specifying Filters for Resource Copying Control.
	 * <p>Allow to specify some filters to control the resource copy process.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.builder.resourceCopyExclusionFilter"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "&lt;name&gt;[,&lt;name&gt;]* }</code> where <code>&lt;name&gt;</code> is a file name pattern (* and ? wild-cards allowed)
	 *	   or the name of a folder which ends with <code>'/'</code></dd>
	 * <dt>Default:</dt><dd><code>""</code></dd>
	 * </dl>
	 * @since 2.0
	 * @category CoreOptionID
	 */
	public static final String CORE_JAVA_BUILD_RESOURCE_COPY_FILTER = PLUGIN_ID + ".builder.resourceCopyExclusionFilter"; //$NON-NLS-1$
	/**
	 * Core option ID: Reporting Duplicate Resources.
	 * <p>Indicate the severity of the problem reported when more than one occurrence
	 *    of a resource is to be copied into the output location.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.builder.duplicateResourceTask"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning" }</code></dd>
	 * <dt>Default:</dt><dd><code>"warning"</code></dd>
	 * </dl>
	 * @since 2.1
	 * @category CoreOptionID
	 */
	public static final String CORE_JAVA_BUILD_DUPLICATE_RESOURCE = PLUGIN_ID + ".builder.duplicateResourceTask"; //$NON-NLS-1$
	/**
	 * Core option ID: Cleaning Output Folder(s).
	 * <p>Indicate whether the JavaBuilder is allowed to clean the output folders
	 *    when performing full build operations.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.builder.cleanOutputFolder"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "clean", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"clean"</code></dd>
	 * </dl>
	 * @since 2.1
	 * @category CoreOptionID
	 */
	public static final String CORE_JAVA_BUILD_CLEAN_OUTPUT_FOLDER = PLUGIN_ID + ".builder.cleanOutputFolder"; //$NON-NLS-1$
	/**
	 * Core option ID: Recreate Modified class files in Output Folder.
	 * <p>Indicate whether the JavaBuilder should check for any changes to .class files
	 *    in the output folders while performing incremental build operations. If changes
	 *    are detected to managed .class files, then a full build is performed, otherwise
	 *    the changes are left as is. Tools further altering generated .class files, like optimizers,
	 *    should ensure this option remains set in its default state of ignore.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.builder.recreateModifiedClassFileInOutputFolder"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "enabled", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"ignore"</code></dd>
	 * </dl>
	 * @since 3.2
	 * @category CoreOptionID
	 */
	public static final String CORE_JAVA_BUILD_RECREATE_MODIFIED_CLASS_FILES_IN_OUTPUT_FOLDER = PLUGIN_ID + ".builder.recreateModifiedClassFileInOutputFolder"; //$NON-NLS-1$
	/**
	 * Core option ID: Reporting Incomplete Classpath.
	 * <p>Indicate the severity of the problem reported when an entry on the classpath does not exist,
	 *    is not legitimate or is not visible (for example, a referenced project is closed).</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.incompleteClasspath"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning"}</code></dd>
	 * <dt>Default:</dt><dd><code>"error"</code></dd>
	 * </dl>
	 * @since 2.1
	 * @category CoreOptionID
	 */
	public static final String CORE_INCOMPLETE_CLASSPATH = PLUGIN_ID + ".incompleteClasspath"; //$NON-NLS-1$
	/**
	 * Core option ID: Reporting Classpath Cycle.
	 * <p>Indicate the severity of the problem reported when a project is involved in a cycle.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.circularClasspath"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning" }</code></dd>
	 * <dt>Default:</dt><dd><code>"error"</code></dd>
	 * </dl>
	 * @since 2.1
	 * @category CoreOptionID
	 */
	public static final String CORE_CIRCULAR_CLASSPATH = PLUGIN_ID + ".circularClasspath"; //$NON-NLS-1$
	/**
	 * Core option ID: Reporting Incompatible JDK Level for Required Binaries.
	 * <p>Indicate the severity of the problem reported when a project prerequisites another project
	 *    or library with an incompatible target JDK level (e.g. project targeting 1.1 vm, but compiled against 1.4 libraries).</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.incompatibleJDKLevel"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"ignore"</code></dd>
	 * </dl>
	 * @since 3.0
	 * @category CoreOptionID
	 */
	public static final String CORE_INCOMPATIBLE_JDK_LEVEL = PLUGIN_ID + ".incompatibleJDKLevel"; //$NON-NLS-1$
	/**
	 * Core option ID: Abort if Invalid Classpath.
	 * <p>Allow to toggle the builder to abort if the classpath is invalid.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.builder.invalidClasspath"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "abort", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"abort"</code></dd>
	 * </dl>
	 * @since 2.0
	 * @category CoreOptionID
	 */
	public static final String CORE_JAVA_BUILD_INVALID_CLASSPATH = PLUGIN_ID + ".builder.invalidClasspath"; //$NON-NLS-1$
	/**
	 * Core option ID: Default Source Encoding Format.
	 * <p>Get the default encoding format of source files. This value is
	 *    immutable and preset to the result of <code>ResourcesPlugin.getEncoding()</code>.</p>
	 * <p>It is offered as a convenience shortcut only.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.encoding"</code></dd>
	 * <dt>value:</dt><dd><code>&lt;immutable, platform default value&gt;</code></dd>
	 * </dl>
	 * @since 2.0
	 * @category CoreOptionID
	 */
	public static final String CORE_ENCODING = PLUGIN_ID + ".encoding"; //$NON-NLS-1$
	/**
	 * Core option ID: Enabling Usage of Classpath Exclusion Patterns.
	 * <p>When disabled, no entry on a project classpath can be associated with
	 *    an exclusion pattern.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.classpath.exclusionPatterns"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "enabled", "disabled" }</code></dd>
	 * <dt>Default:</dt><dd><code>"enabled"</code></dd>
	 * </dl>
	 * @since 2.1
	 * @category CoreOptionID
	 */
	public static final String CORE_ENABLE_CLASSPATH_EXCLUSION_PATTERNS = PLUGIN_ID + ".classpath.exclusionPatterns"; //$NON-NLS-1$
	/**
	 * Core option ID: Enabling Usage of Classpath Multiple Output Locations.
	 * <p>When disabled, no entry on a project classpath can be associated with
	 *    a specific output location, preventing thus usage of multiple output locations.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.classpath.multipleOutputLocations"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "enabled", "disabled" }</code></dd>
	 * <dt>Default:</dt><dd><code>"enabled"</code></dd>
	 * </dl>
	 * @since 2.1
	 * @category CoreOptionID
	 */
	public static final String CORE_ENABLE_CLASSPATH_MULTIPLE_OUTPUT_LOCATIONS = PLUGIN_ID + ".classpath.multipleOutputLocations"; //$NON-NLS-1$
	/**
	 * Core option ID: Reporting an output location overlapping another source location.
	 * <p> Indicate the severity of the problem reported when a source entry's output location overlaps another
	 * source entry.</p>
	 *
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.classpath.outputOverlappingAnotherSource"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "warning", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"error"</code></dd>
	 * </dl>
	 * @since 3.6.4
	 */
	public static final String CORE_OUTPUT_LOCATION_OVERLAPPING_ANOTHER_SOURCE = PLUGIN_ID + ".classpath.outputOverlappingAnotherSource";  //$NON-NLS-1$

	/**
	 * Core option ID: Reporting if a project which has only main sources depends on a project with only test sources.
	 * <p> Indicate the severity of the problem reported when a project that has one or more main source folders but
	 * no test source folders has a project on its build path that only has one or more test source folders, but no main source folders.</p>
	 *
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.classpath.mainOnlyProjectHasTestOnlyDependency"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "error", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"error"</code></dd>
	 * </dl>
	 * @since 3.16
	 */
	public static final String CORE_MAIN_ONLY_PROJECT_HAS_TEST_ONLY_DEPENDENCY = PLUGIN_ID + ".classpath.mainOnlyProjectHasTestOnlyDependency";  //$NON-NLS-1$

	/**
	 * Compiler option ID: Enabling support for preview language features.
	 * <p>When enabled, the compiler will activate the preview language features of this Java version.</p>
	 *
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.enablePreviewFeatures"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "enabled", "disabled" }</code></dd>
	 * <dt>Default:</dt><dd><code>"disabled"</code></dd>
	 * </dl>
	 * @category CompilerOptionID
	 * @since 3.18
	 */
	public static final String COMPILER_PB_ENABLE_PREVIEW_FEATURES = PLUGIN_ID + ".compiler.problem.enablePreviewFeatures"; //$NON-NLS-1$
	/**
	 * Compiler option ID: Reporting Preview features.
	 * <p>When enabled, the compiler will issue a warning when a preview feature is used.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.compiler.problem.reportPreviewFeatures"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "warning", "info", "ignore" }</code></dd>
	 * <dt>Default:</dt><dd><code>"warning"</code></dd>
	 * </dl>
	 * @category CompilerOptionID
	 * @since 3.18
	 */
	public static final String COMPILER_PB_REPORT_PREVIEW_FEATURES = PLUGIN_ID + ".compiler.problem.reportPreviewFeatures"; //$NON-NLS-1$
	/**
	 * Core option ID: Set the timeout value for retrieving the method's parameter names from javadoc.
	 * <p>Timeout in milliseconds to retrieve the method's parameter names from javadoc.</p>
	 * <p>If the value is <code>0</code>, the parameter names are not fetched and the raw names are returned.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.timeoutForParameterNameFromAttachedJavadoc"</code></dd>
	 * <dt>Possible values:</dt><dd><code>"&lt;n&gt;"</code>, where <code>n</code> is an integer greater than or equal to <code>0</code></dd>
	 * <dt>Default:</dt><dd><code>"50"</code></dd>
	 * </dl>
	 * @since 3.2
	 * @category CoreOptionID
	 */
	public static final String TIMEOUT_FOR_PARAMETER_NAME_FROM_ATTACHED_JAVADOC = PLUGIN_ID + ".timeoutForParameterNameFromAttachedJavadoc"; //$NON-NLS-1$

	/**
	 * Core option ID: The ID of the formatter to use in formatting operations.
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.javaFormatter"</code></dd>
	 * <dt>Default:</dt><dd><code>"org.eclipse.jdt.core.defaultJavaFormatter"</code></dd>
	 * </dl>
	 * @see #DEFAULT_JAVA_FORMATTER
	 * @see #JAVA_FORMATTER_EXTENSION_POINT_ID
	 * @since 3.11
	 * @category CoreOptionID
	 */
	public static final String JAVA_FORMATTER = PLUGIN_ID + ".javaFormatter"; //$NON-NLS-1$

	/**
	 * @since 2.0
	 * @deprecated Use {@link org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants#FORMATTER_BRACE_POSITION_FOR_ANONYMOUS_TYPE_DECLARATION},
	 * {@link org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants#FORMATTER_BRACE_POSITION_FOR_BLOCK} ,
	 * {@link org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants#FORMATTER_BRACE_POSITION_FOR_CONSTRUCTOR_DECLARATION},
	 * {@link org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants#FORMATTER_BRACE_POSITION_FOR_METHOD_DECLARATION},
	 * {@link org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants#FORMATTER_BRACE_POSITION_FOR_SWITCH},
	 * {@link org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants#FORMATTER_BRACE_POSITION_FOR_TYPE_DECLARATION} instead.
	 * @category DeprecatedOptionID
	 */
	public static final String FORMATTER_NEWLINE_OPENING_BRACE = PLUGIN_ID + ".formatter.newline.openingBrace"; //$NON-NLS-1$
	/**
	 * @since 2.0
	 * @deprecated Use {@link org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants#FORMATTER_INSERT_NEW_LINE_BEFORE_CATCH_IN_TRY_STATEMENT},
	 *  {@link org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants#FORMATTER_INSERT_NEW_LINE_BEFORE_ELSE_IN_IF_STATEMENT},
	 *  {@link org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants#FORMATTER_INSERT_NEW_LINE_BEFORE_FINALLY_IN_TRY_STATEMENT},
	 *  {@link org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants#FORMATTER_INSERT_NEW_LINE_BEFORE_WHILE_IN_DO_STATEMENT} instead.
	 * @category DeprecatedOptionID
	 */
	public static final String FORMATTER_NEWLINE_CONTROL = PLUGIN_ID + ".formatter.newline.controlStatement"; //$NON-NLS-1$
	/**
	 * @since 2.0
	 * @deprecated Use {@link org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants#FORMATTER_COMPACT_ELSE_IF} instead.
	 * @category DeprecatedOptionID
	 */
	public static final String FORMATTER_NEWLINE_ELSE_IF = PLUGIN_ID + ".formatter.newline.elseIf"; //$NON-NLS-1$
	/**
	 * @since 2.0
	 * @deprecated Use {@link org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants#FORMATTER_INSERT_NEW_LINE_IN_EMPTY_BLOCK} instead.
	 * @category DeprecatedOptionID
	 */
	public static final String FORMATTER_NEWLINE_EMPTY_BLOCK = PLUGIN_ID + ".formatter.newline.emptyBlock"; //$NON-NLS-1$
	/**
	 * @since 2.0
	 * @deprecated Use {@link org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants#FORMATTER_NUMBER_OF_EMPTY_LINES_TO_PRESERVE} instead.
	 * @category DeprecatedOptionID
	 */
	public static final String FORMATTER_CLEAR_BLANK_LINES = PLUGIN_ID + ".formatter.newline.clearAll"; //$NON-NLS-1$
	/**
	 * @since 2.0
	 * @deprecated Use {@link org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants#FORMATTER_LINE_SPLIT} instead
	 * @category DeprecatedOptionID
	 */
	public static final String FORMATTER_LINE_SPLIT = PLUGIN_ID + ".formatter.lineSplit"; //$NON-NLS-1$
	/**
	 * @since 2.0
	 * @deprecated Use {@link org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants#FORMATTER_INSERT_SPACE_BEFORE_ASSIGNMENT_OPERATOR} instead.
	 * @category DeprecatedOptionID
	 */
	public static final String FORMATTER_COMPACT_ASSIGNMENT = PLUGIN_ID + ".formatter.style.assignment"; //$NON-NLS-1$
	/**
	 * @since 2.0
	 * @deprecated Use {@link org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants#FORMATTER_TAB_CHAR} instead.
	 * @category DeprecatedOptionID
	 */
	public static final String FORMATTER_TAB_CHAR = PLUGIN_ID + ".formatter.tabulation.char"; //$NON-NLS-1$
	/**
	 * @since 2.0
	 * @deprecated Use {@link org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants#FORMATTER_TAB_SIZE} instead.
	 * @category DeprecatedOptionID
	 */
	public static final String FORMATTER_TAB_SIZE = PLUGIN_ID + ".formatter.tabulation.size"; //$NON-NLS-1$
	/**
	 * @since 2.1
	 * @deprecated Use {@link org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants#FORMATTER_INSERT_SPACE_AFTER_CLOSING_PAREN_IN_CAST} instead.
	 * @category DeprecatedOptionID
	 */
	public static final String FORMATTER_SPACE_CASTEXPRESSION = PLUGIN_ID + ".formatter.space.castexpression"; //$NON-NLS-1$
	/**
	 * Code assist option ID: Activate Visibility Sensitive Completion.
	 * <p>When active, completion doesn't show that you can not see
	 *    (for example, you can not see private methods of a super class).</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.codeComplete.visibilityCheck"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "enabled", "disabled" }</code></dd>
	 * <dt>Default:</dt><dd><code>"disabled"</code></dd>
	 * </dl>
	 * @since 2.0
	 * @category CodeAssistOptionID
	 */
	public static final String CODEASSIST_VISIBILITY_CHECK = PLUGIN_ID + ".codeComplete.visibilityCheck"; //$NON-NLS-1$
	/**
	 * Code assist option ID: Activate Deprecation Sensitive Completion.
	 * <p>When enabled, completion doesn't propose deprecated members and types.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.codeComplete.deprecationCheck"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "enabled", "disabled" }</code></dd>
	 * <dt>Default:</dt><dd><code>"disabled"</code></dd>
	 * </dl>
	 * @since 3.2
	 * @category CodeAssistOptionID
	 */
	public static final String CODEASSIST_DEPRECATION_CHECK = PLUGIN_ID + ".codeComplete.deprecationCheck"; //$NON-NLS-1$
	/**
	 * Code assist option ID: Activate Camel Case Sensitive Completion.
	 * <p>When enabled, completion shows proposals whose name match the CamelCase
	 *    pattern.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.codeComplete.camelCaseMatch"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "enabled", "disabled" }</code></dd>
	 * <dt>Default:</dt><dd><code>"enabled"</code></dd>
	 * </dl>
	 * @since 3.2
	 * @category CodeAssistOptionID
	 */
	public static final String CODEASSIST_CAMEL_CASE_MATCH = PLUGIN_ID + ".codeComplete.camelCaseMatch"; //$NON-NLS-1$
	/**
	 * Code assist option ID: Activate Substring Code Completion.
	 * <p>When enabled, completion shows proposals in which the pattern can
	 *    be found as a substring in a case-insensitive way.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.codeComplete.substringMatch"</code></dd>
	 * </dl>
	 * @since 3.12
	 * @deprecated - this option has no effect
	 * @category DeprecatedOptionID
	 */
	public static final String CODEASSIST_SUBSTRING_MATCH = PLUGIN_ID + ".codeComplete.substringMatch"; //$NON-NLS-1$
	/**
	 * Code assist option ID: Activate Subword Code Completion.
	 * <p>When enabled, completion shows proposals in which the pattern can
	 *    be found as a subword in a case-insensitive way.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.codeComplete.subwordMatch"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "enabled", "disabled" }</code></dd>
	 * <dt>Default:</dt><dd><code>"enabled"</code></dd>
	 * </dl>
	 * @since 3.21
	 * @category CodeAssistOptionID
	 */
	public static final String CODEASSIST_SUBWORD_MATCH = PLUGIN_ID + ".codeComplete.subwordMatch"; //$NON-NLS-1$
	/**
	 * Code assist option ID: Automatic Qualification of Implicit Members.
	 * <p>When active, completion automatically qualifies completion on implicit
	 *    field references and message expressions.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.codeComplete.forceImplicitQualification"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "enabled", "disabled" }</code></dd>
	 * <dt>Default:</dt><dd><code>"disabled"</code></dd>
	 * </dl>
	 * @since 2.0
	 * @category CodeAssistOptionID
	 */
	public static final String CODEASSIST_IMPLICIT_QUALIFICATION = PLUGIN_ID + ".codeComplete.forceImplicitQualification"; //$NON-NLS-1$
	/**
	 * Code assist option ID: Define the Prefixes for Field Name.
	 * <p>When the prefixes is non empty, completion for field name will begin with
	 *    one of the proposed prefixes.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.codeComplete.fieldPrefixes"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "&lt;prefix&gt;[,&lt;prefix&gt;]*" }</code> where <code>&lt;prefix&gt;</code> is a String without any wild-card</dd>
	 * <dt>Default:</dt><dd><code>""</code></dd>
	 * </dl>
	 * @since 2.1
	 * @category CodeAssistOptionID
	 */
	public static final String CODEASSIST_FIELD_PREFIXES = PLUGIN_ID + ".codeComplete.fieldPrefixes"; //$NON-NLS-1$
	/**
	 * Code assist option ID: Define the Prefixes for Static Field Name.
	 * <p>When the prefixes is non empty, completion for static field name will begin with
	 *    one of the proposed prefixes.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.codeComplete.staticFieldPrefixes"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "&lt;prefix&gt;[,&lt;prefix&gt;]*" }</code> where <code>&lt;prefix&gt;</code> is a String without any wild-card</dd>
	 * <dt>Default:</dt><dd><code>""</code></dd>
	 * </dl>
	 * @since 2.1
	 * @category CodeAssistOptionID
	 */
	public static final String CODEASSIST_STATIC_FIELD_PREFIXES = PLUGIN_ID + ".codeComplete.staticFieldPrefixes"; //$NON-NLS-1$
	/**
	 * Code assist option ID: Define the Prefixes for Static Final Field Name.
	 * <p>When the prefixes is non empty, completion for static final field name will begin with
	 *    one of the proposed prefixes.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.codeComplete.staticFinalFieldPrefixes"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "&lt;prefix&gt;[,&lt;prefix&gt;]*" }</code> where <code>&lt;prefix&gt;</code> is a String without any wild-card</dd>
	 * <dt>Default:</dt><dd><code>""</code></dd>
	 * </dl>
	 * @since 3.5
	 * @category CodeAssistOptionID
	 */
	public static final String CODEASSIST_STATIC_FINAL_FIELD_PREFIXES = PLUGIN_ID + ".codeComplete.staticFinalFieldPrefixes"; //$NON-NLS-1$
	/**
	 * Code assist option ID: Define the Prefixes for Local Variable Name.
	 * <p>When the prefixes is non empty, completion for local variable name will begin with
	 *    one of the proposed prefixes.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.codeComplete.localPrefixes"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "&lt;prefix&gt;[,&lt;prefix&gt;]*" }</code> where <code>&lt;prefix&gt;</code> is a String without any wild-card</dd>
	 * <dt>Default:</dt><dd><code>""</code></dd>
	 * </dl>
	 * @since 2.1
	 * @category CodeAssistOptionID
	 */
	public static final String CODEASSIST_LOCAL_PREFIXES = PLUGIN_ID + ".codeComplete.localPrefixes"; //$NON-NLS-1$
	/**
	 * Code assist option ID: Define the Prefixes for Argument Name.
	 * <p>When the prefixes is non empty, completion for argument name will begin with
	 *    one of the proposed prefixes.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.codeComplete.argumentPrefixes"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "&lt;prefix&gt;[,&lt;prefix&gt;]*" }</code> where <code>&lt;prefix&gt;</code> is a String without any wild-card</dd>
	 * <dt>Default:</dt><dd><code>""</code></dd>
	 * </dl>
	 * @since 2.1
	 * @category CodeAssistOptionID
	 */
	public static final String CODEASSIST_ARGUMENT_PREFIXES = PLUGIN_ID + ".codeComplete.argumentPrefixes"; //$NON-NLS-1$
	/**
	 * Code assist option ID: Define the Suffixes for Field Name.
	 * <p>When the suffixes is non empty, completion for field name will end with
	 *    one of the proposed suffixes.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.codeComplete.fieldSuffixes"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "&lt;suffix&gt;[,&lt;suffix&gt;]*" }</code> where <code>&lt;suffix&gt;</code> is a String without any wild-card</dd>
	 * <dt>Default:</dt><dd><code>""</code></dd>
	 * </dl>
	 * @since 2.1
	 * @category CodeAssistOptionID
	 */
	public static final String CODEASSIST_FIELD_SUFFIXES = PLUGIN_ID + ".codeComplete.fieldSuffixes"; //$NON-NLS-1$
	/**
	 * Code assist option ID: Define the Suffixes for Static Field Name.
	 * <p>When the suffixes is non empty, completion for static field name will end with
	 *    one of the proposed suffixes.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.codeComplete.staticFieldSuffixes"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "&lt;suffix&gt;[,&lt;suffix&gt;]*" }</code>< where <code>&lt;suffix&gt;</code> is a String without any wild-card</dd>
	 * <dt>Default:</dt><dd><code>""</code></dd>
	 * </dl>
	 * @since 2.1
	 * @category CodeAssistOptionID
	 */
	public static final String CODEASSIST_STATIC_FIELD_SUFFIXES = PLUGIN_ID + ".codeComplete.staticFieldSuffixes"; //$NON-NLS-1$
	/**
	 * Code assist option ID: Define the Suffixes for Static Final Field Name.
	 * <p>When the suffixes is non empty, completion for static final field name will end with
	 *    one of the proposed suffixes.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.codeComplete.staticFinalFieldSuffixes"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "&lt;suffix&gt;[,&lt;suffix&gt;]*" }</code>< where <code>&lt;suffix&gt;</code> is a String without any wild-card</dd>
	 * <dt>Default:</dt><dd><code>""</code></dd>
	 * </dl>
	 * @since 3.5
	 * @category CodeAssistOptionID
	 */
	public static final String CODEASSIST_STATIC_FINAL_FIELD_SUFFIXES = PLUGIN_ID + ".codeComplete.staticFinalFieldSuffixes"; //$NON-NLS-1$
	/**
	 * Code assist option ID: Define the Suffixes for Local Variable Name.
	 * <p>When the suffixes is non empty, completion for local variable name will end with
	 *    one of the proposed suffixes.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.codeComplete.localSuffixes"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "&lt;suffix&gt;[,&lt;suffix&gt;]*" }</code> where <code>&lt;suffix&gt;</code> is a String without any wild-card</dd>
	 * <dt>Default:</dt><dd><code>""</code></dd>
	 * </dl>
	 * @since 2.1
	 * @category CodeAssistOptionID
	 */
	public static final String CODEASSIST_LOCAL_SUFFIXES = PLUGIN_ID + ".codeComplete.localSuffixes"; //$NON-NLS-1$
	/**
	 * Code assist option ID: Define the Suffixes for Argument Name.
	 * <p>When the suffixes is non empty, completion for argument name will end with
	 *    one of the proposed suffixes.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.codeComplete.argumentSuffixes"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "&lt;suffix&gt;[,&lt;suffix&gt;]*" }</code> where <code>&lt;suffix&gt;</code> is a String without any wild-card</dd>
	 * <dt>Default:</dt><dd><code>""</code></dd>
	 * </dl>
	 * @since 2.1
	 * @category CodeAssistOptionID
	 */
	public static final String CODEASSIST_ARGUMENT_SUFFIXES = PLUGIN_ID + ".codeComplete.argumentSuffixes"; //$NON-NLS-1$
	/**
	 * Code assist option ID: Activate Forbidden Reference Sensitive Completion.
	 * <p>When enabled, completion doesn't propose elements which match a
	 *    forbidden reference rule.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.codeComplete.forbiddenReferenceCheck"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "enabled", "disabled" }</code></dd>
	 * <dt>Default:</dt><dd><code>"enabled"</code></dd>
	 * </dl>
	 * @since 3.1
	 * @category CodeAssistOptionID
	 */
	public static final String CODEASSIST_FORBIDDEN_REFERENCE_CHECK= PLUGIN_ID + ".codeComplete.forbiddenReferenceCheck"; //$NON-NLS-1$
	/**
	 * Code assist option ID: Activate Discouraged Reference Sensitive Completion.
	 * <p>When enabled, completion doesn't propose elements which match a
	 *    discouraged reference rule.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.codeComplete.discouragedReferenceCheck"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "enabled", "disabled" }</code></dd>
	 * <dt>Default:</dt><dd><code>"disabled"</code></dd>
	 * </dl>
	 * @since 3.1
	 * @category CodeAssistOptionID
	 */
	public static final String CODEASSIST_DISCOURAGED_REFERENCE_CHECK= PLUGIN_ID + ".codeComplete.discouragedReferenceCheck"; //$NON-NLS-1$
	/**
	 * Code assist option ID: Activate Suggestion of Static Import.
	 * <p>When enabled, completion proposals can contain static import
	 *    pattern.</p>
	 * <dl>
	 * <dt>Option id:</dt><dd><code>"org.eclipse.jdt.core.codeComplete.suggestStaticImports"</code></dd>
	 * <dt>Possible values:</dt><dd><code>{ "enabled", "disabled" }</code></dd>
	 * <dt>Default:</dt><dd><code>"enabled"</code></dd>
	 * </dl>
	 * @since 3.3
	 * @category CodeAssistOptionID
	 */
	public static final String CODEASSIST_SUGGEST_STATIC_IMPORTS= PLUGIN_ID + ".codeComplete.suggestStaticImports"; //$NON-NLS-1$
	// end configurable option IDs }
	// Begin configurable option values {
	/**
	 * @deprecated Use {@link #DEFAULT_TASK_TAGS} instead.
	 * @since 2.1
	 * @category DeprecatedOptionValue
	 */
	public static final String DEFAULT_TASK_TAG = "TODO"; //$NON-NLS-1$
	/**
	 * @deprecated Use {@link #DEFAULT_TASK_PRIORITIES} instead.
	 * @since 2.1
	 * @category DeprecatedOptionValue
	 */
	public static final String DEFAULT_TASK_PRIORITY = "NORMAL"; //$NON-NLS-1$
	/**
	 * Configurable option value: {@value}.
	 * @since 3.0
	 * @category OptionValue
	 */
	public static final String DEFAULT_TASK_TAGS = "TODO,FIXME,XXX"; //$NON-NLS-1$
	/**
	 * Configurable option value: {@value}.
	 * @since 3.0
	 * @category OptionValue
	 */
	public static final String DEFAULT_TASK_PRIORITIES = "NORMAL,HIGH,NORMAL"; //$NON-NLS-1$
	/**
	 * Configurable option value: {@value}.
	 * @category OptionValue
	 */
	public static final String GENERATE = "generate"; //$NON-NLS-1$
	/**
	 * Configurable option value: {@value}.
	 * @category OptionValue
	 */
	public static final String DO_NOT_GENERATE = "do not generate"; //$NON-NLS-1$
	/**
	 * Configurable option value: {@value}.
	 * @category OptionValue
	 */
	public static final String PRESERVE = "preserve"; //$NON-NLS-1$
	/**
	 * Configurable option value: {@value}.
	 * @category OptionValue
	 */
	public static final String OPTIMIZE_OUT = "optimize out"; //$NON-NLS-1$
	/**
	 * Configurable option value for {@link #COMPILER_TASK_PRIORITIES}: {@value}.
	 * @since 2.1
	 * @category OptionValue
	 */
	public static final String COMPILER_TASK_PRIORITY_HIGH = "HIGH"; //$NON-NLS-1$
	/**
	 * Configurable option value for {@link #COMPILER_TASK_PRIORITIES}: {@value}.
	 * @since 2.1
	 * @category OptionValue
	 */
	public static final String COMPILER_TASK_PRIORITY_LOW = "LOW"; //$NON-NLS-1$
	/**
	 * Configurable option value for {@link #COMPILER_TASK_PRIORITIES}: {@value}.
	 * @since 2.1
	 * @category OptionValue
	 */
	public static final String COMPILER_TASK_PRIORITY_NORMAL = "NORMAL"; //$NON-NLS-1$
	/**
	 * Configurable option value: {@value}.
	 * @category OptionValue
	 */
	public static final String VERSION_1_1 = "1.1"; //$NON-NLS-1$
	/**
	 * Configurable option value: {@value}.
	 * @category OptionValue
	 */
	public static final String VERSION_1_2 = "1.2"; //$NON-NLS-1$
	/**
	 * Configurable option value: {@value}.
	 * @since 2.0
	 * @category OptionValue
	 */
	public static final String VERSION_1_3 = "1.3"; //$NON-NLS-1$
	/**
	 * Configurable option value: {@value}.
	 * @since 2.0
	 * @category OptionValue
	 */
	public static final String VERSION_1_4 = "1.4"; //$NON-NLS-1$
	/**
	 * Configurable option value: {@value}.
	 * @since 3.0
	 * @category OptionValue
	 */
	public static final String VERSION_1_5 = "1.5"; //$NON-NLS-1$
	/**
	 * Configurable option value: {@value}.
	 * @since 3.2
	 * @category OptionValue
	 */
	public static final String VERSION_1_6 = "1.6"; //$NON-NLS-1$
	/**
	 * Configurable option value: {@value}.
	 * @since 3.3
	 * @category OptionValue
	 */
	public static final String VERSION_1_7 = "1.7"; //$NON-NLS-1$
	/**
	 * Configurable option value: {@value}.
	 * @since 3.10
	 * @category OptionValue
	 */
	public static final String VERSION_1_8 = "1.8"; //$NON-NLS-1$
	/**
	 * Configurable option value: {@value}.
	 * @since 3.14
	 * @category OptionValue
	 */
	public static final String VERSION_9 = "9"; //$NON-NLS-1$
	/**
	 * Configurable option value: {@value}.
	 * @since 3.14
	 * @category OptionValue
	 */
	public static final String VERSION_10 = "10"; //$NON-NLS-1$
	/**
	 * Configurable option value: {@value}.
	 * @since 3.16
	 * @category OptionValue
	 */
	public static final String VERSION_11 = "11"; //$NON-NLS-1$
	/**
	 * Configurable option value: {@value}.
	 * @since 3.18
	 * @category OptionValue
	 */
	public static final String VERSION_12 = "12"; //$NON-NLS-1$
	/**
	 * Configurable option value: {@value}.
	 * @since 3.20
	 * @category OptionValue
	 */
	public static final String VERSION_13 = "13"; //$NON-NLS-1$
	/**
	 * Configurable option value: {@value}.
	 * @since 3.22
	 * @category OptionValue
	 */
	public static final String VERSION_14 = "14"; //$NON-NLS-1$
	/**
	 * Configurable option value: {@value}.
	 * @since 3.24
	 * @category OptionValue
	 */
	public static final String VERSION_15 = "15"; //$NON-NLS-1$
	/**
	 * Configurable option value: {@value}.
	 * @since 3.26
	 * @category OptionValue
	 */
	public static final String VERSION_16 = "16"; //$NON-NLS-1$
	/**
	 * Configurable option value: {@value}.
	 * @since 3.28
	 * @category OptionValue
	 */
	public static final String VERSION_17 = "17"; //$NON-NLS-1$
	/**
	 * Configurable option value: {@value}.
	 * @since 3.4
	 * @category OptionValue
	 */
	public static final String VERSION_CLDC_1_1 = "cldc1.1"; //$NON-NLS-1$
	private static List<String> allVersions = Collections.unmodifiableList(Arrays.asList(VERSION_CLDC_1_1, VERSION_1_1, VERSION_1_2, VERSION_1_3, VERSION_1_4, VERSION_1_5,
			VERSION_1_6, VERSION_1_7, VERSION_1_8, VERSION_9, VERSION_10, VERSION_11, VERSION_12, VERSION_13, VERSION_14, VERSION_15, VERSION_16, VERSION_17));

	/**
	 * Returns all {@link JavaCore}{@code #VERSION_*} levels in the order of their
	 * introduction. For e.g., {@link JavaCore#VERSION_1_8} appears before {@link JavaCore#VERSION_10}
	 *
	 * @return all available versions
	 * @since 3.14
	 */
	public static List<String> getAllVersions() {
		return allVersions;
	}

	/**
	 * Returns whether the given version of Java or Java Runtime is supported
	 * by the Java Development Toolkit.
	 *
	 * A true indicates that the given version is supported. For e.g., if the argument
	 * is <code>11.0.1</code> and {@link #getAllVersions()} contains <code>11</code>,
	 * the method returns <code>true</code>.
	 *
	 * @return a boolean indicating support for the given version of Java or Java Runtime.
	 * @since 3.16
	 */
	public static boolean isSupportedJavaVersion(String version) {
		return CompilerOptions.versionToJdkLevel(version, false) > 0;
	}

	/**
	 * Configurable option value: {@value}.
	 * @since 2.0
	 * @category OptionValue
	 */
	public static final String ABORT = "abort"; //$NON-NLS-1$
	/**
	 * Configurable option value: {@value}.
	 * @category OptionValue
	 */
	public static final String ERROR = "error"; //$NON-NLS-1$
	/**
	 * Configurable option value: {@value}.
	 * @category OptionValue
	 */
	public static final String WARNING = "warning"; //$NON-NLS-1$
	/**
	 * Configurable option value: {@value}.
	 * @category OptionValue
	 */
	public static final String IGNORE = "ignore"; //$NON-NLS-1$
	/**
	 * Configurable option value: {@value}.
	 * @category OptionValue
	 * @since 3.12
	 */
	public static final String INFO = "info"; //$NON-NLS-1$
	/**
	 * Configurable option value: {@value}.
	 * @category OptionValue
	 */
	public static final String COMPUTE = "compute"; //$NON-NLS-1$
	/**
	 * Configurable option value: {@value}.
	 * @since 2.0
	 * @category OptionValue
	 */
	public static final String INSERT = "insert"; //$NON-NLS-1$
	/**
	 * Configurable option value: {@value}.
	 * @since 2.0
	 * @category OptionValue
	 */
	public static final String DO_NOT_INSERT = "do not insert"; //$NON-NLS-1$
	/**
	 * Configurable option value: {@value}.
	 * @since 2.0
	 * @category OptionValue
	 */
	public static final String PRESERVE_ONE = "preserve one"; //$NON-NLS-1$
	/**
	 * Configurable option value: {@value}.
	 * @since 2.0
	 * @category OptionValue
	 */
	public static final String CLEAR_ALL = "clear all"; //$NON-NLS-1$
	/**
	 * Configurable option value: {@value}.
	 * @since 2.0
	 * @category OptionValue
	 */
	public static final String NORMAL = "normal"; //$NON-NLS-1$
	/**
	 * Configurable option value: {@value}.
	 * @since 2.0
	 * @category OptionValue
	 */
	public static final String COMPACT = "compact"; //$NON-NLS-1$
	/**
	 * Configurable option value: {@value}.
	 * @since 2.0
	 * @category OptionValue
	 */
	public static final String TAB = "tab"; //$NON-NLS-1$
	/**
	 * Configurable option value: {@value}.
	 * @since 2.0
	 * @category OptionValue
	 */
	public static final String SPACE = "space"; //$NON-NLS-1$
	/**
	 * Configurable option value: {@value}.
	 * @since 2.0
	 * @category OptionValue
	 */
	public static final String ENABLED = "enabled"; //$NON-NLS-1$
	/**
	 * Configurable option value: {@value}.
	 * @since 2.0
	 * @category OptionValue
	 */
	public static final String DISABLED = "disabled"; //$NON-NLS-1$
	/**
	 * Configurable option value: {@value}.
	 * @since 2.1
	 * @category OptionValue
	 */
	public static final String CLEAN = "clean"; //$NON-NLS-1$
	/**
	 * Configurable option value: {@value}.
	 * @since 3.0
	 * @category OptionValue
	 */
	public static final String PUBLIC = "public"; //$NON-NLS-1$
	/**
	 * Configurable option value: {@value}.
	 * @since 3.0
	 * @category OptionValue
	 */
	public static final String PROTECTED = "protected"; //$NON-NLS-1$
	/**
	 * Configurable option value: {@value}.
	 * @since 3.0
	 * @category OptionValue
	 */
	public static final String DEFAULT = "default"; //$NON-NLS-1$
	/**
	 * Configurable option value: {@value}.
	 * @since 3.0
	 * @category OptionValue
	 */
	public static final String PRIVATE = "private"; //$NON-NLS-1$
	/**
	 * Configurable option value: {@value}.
	 * @since 3.1
	 * @category OptionValue
	 */
	public static final String NEVER = "never"; //$NON-NLS-1$
	/**
	 * Configurable option value: {@value}.
	 * @since 3.4
	 * @category OptionValue
	 */
	public static final String COMPILER_PB_MISSING_JAVADOC_TAG_DESCRIPTION_NO_TAG = CompilerOptions.NO_TAG;
	/**
	 * Configurable option value: {@value}.
	 * @since 3.4
	 * @category OptionValue
	 */
	public static final String COMPILER_PB_MISSING_JAVADOC_TAG_DESCRIPTION_RETURN_TAG = CompilerOptions.RETURN_TAG;
	/**
	 * Configurable option value: {@value}.
	 * @since 3.4
	 * @category OptionValue
	 */
	public static final String COMPILER_PB_MISSING_JAVADOC_TAG_DESCRIPTION_ALL_STANDARD_TAGS = CompilerOptions.ALL_STANDARD_TAGS;
	// end configurable option values }

	/**
	 * Value of the content-type for Java source files. Use this value to retrieve the Java content type
	 * from the content type manager, and to add new Java-like extensions to this content type.
	 *
	 * @see org.eclipse.core.runtime.content.IContentTypeManager#getContentType(String)
	 * @see #getJavaLikeExtensions()
	 * @since 3.2
	 */
	public static final String JAVA_SOURCE_CONTENT_TYPE = JavaCore.PLUGIN_ID+".javaSource" ; //$NON-NLS-1$

	/**
	 * The ID of the Eclipse built-in formatter.
	 *
	 * @see #JAVA_FORMATTER
	 * @see #JAVA_FORMATTER_EXTENSION_POINT_ID
	 * @since 3.11
	 */
	public static final String DEFAULT_JAVA_FORMATTER = PLUGIN_ID + ".defaultJavaFormatter"; //$NON-NLS-1$

	/**
	 * Name of the extension point for contributing a source code formatter
	 * @see #JAVA_FORMATTER
	 * @see #DEFAULT_JAVA_FORMATTER
	 * @since 3.11
	 */
	public static final String JAVA_FORMATTER_EXTENSION_POINT_ID = "javaFormatter" ;  //$NON-NLS-1$

	/**
	 * Creates the Java core plug-in.
	 * <p>
	 * The plug-in instance is created automatically by the
	 * Eclipse platform. Clients must not call.
	 * </p>
	 *
	 * @since 3.0
	 */
	public JavaCore() {
		super();
		JAVA_CORE_PLUGIN = this;
	}

	/**
	 * Adds the given listener for changes to Java elements.
	 * Has no effect if an identical listener is already registered.
	 * <p>
	 * This listener will only be notified during the POST_CHANGE resource change notification
	 * and any reconcile operation (POST_RECONCILE).
	 * </p>
	 * <p>
	 * For finer control of the notification, use <code>addElementChangedListener(IElementChangedListener,int)</code>,
	 * which allows to specify a different eventMask.
	 * </p>
	 *
	 * @param listener the listener
	 * @see ElementChangedEvent
	 */
	public static void addElementChangedListener(IElementChangedListener listener) {
		addElementChangedListener(listener, ElementChangedEvent.POST_CHANGE | ElementChangedEvent.POST_RECONCILE);
	}

	/**
	 * Adds the given listener for changes to Java elements.
	 * Has no effect if an identical listener is already registered.
	 * After completion of this method, the given listener will be registered for exactly
	 * the specified events.  If they were previously registered for other events, they
	 * will be deregistered.
	 * <p>
	 * Once registered, a listener starts receiving notification of changes to
	 * java elements in the model. The listener continues to receive
	 * notifications until it is replaced or removed.
	 * </p>
	 * <p>
	 * Listeners can listen for several types of event as defined in <code>ElementChangeEvent</code>.
	 * Clients are free to register for any number of event types however if they register
	 * for more than one, it is their responsibility to ensure they correctly handle the
	 * case where the same java element change shows up in multiple notifications.
	 * Clients are guaranteed to receive only the events for which they are registered.
	 * </p>
	 *
	 * @param listener the listener
	 * @param eventMask the bit-wise OR of all event types of interest to the listener
	 * @see IElementChangedListener
	 * @see ElementChangedEvent
	 * @see #removeElementChangedListener(IElementChangedListener)
	 * @since 2.0
	 */
	public static void addElementChangedListener(IElementChangedListener listener, int eventMask) {
		JavaModelManager.getDeltaState().addElementChangedListener(listener, eventMask);
	}

	/**
	 * Configures the given marker attribute map for the given Java element.
	 * Used for markers, which denote a Java element rather than a resource.
	 *
	 * @param attributes the mutable marker attribute map (key type: <code>String</code>,
	 *   value type: <code>String</code>)
	 * @param element the Java element for which the marker needs to be configured
	 */
	public static void addJavaElementMarkerAttributes(
		Map attributes,
		IJavaElement element) {
		if (element instanceof IMember)
			element = ((IMember) element).getClassFile();
		if (attributes != null && element != null)
			attributes.put(ATT_HANDLE_ID, element.getHandleIdentifier());
	}

	private static void addNonJavaResources(Object[] nonJavaResources,
			IContainer container,
			int rootPathSegmentCounts,
			ArrayList collector) {
		for (int i = 0, max = nonJavaResources.length; i < max; i++) {
			Object nonJavaResource = nonJavaResources[i];
			if (nonJavaResource instanceof IFile) {
				IFile file = (IFile) nonJavaResource;
				IPath path = file.getFullPath().removeFirstSegments(rootPathSegmentCounts);
				IResource member = container.findMember(path);
				if (member != null && member.exists()) {
					collector.add(member);
				}
			} else if (nonJavaResource instanceof IFolder) {
				IFolder folder = (IFolder) nonJavaResource;
				IResource[] members = null;
				try {
					members = folder.members();
				} catch (CoreException e) {
					// ignore
				}
				if (members != null) {
					addNonJavaResources(members, container, rootPathSegmentCounts, collector);
				}
			}
		}
	}

	/**
	 * Adds the given listener for POST_CHANGE resource change events to the Java core.
	 * The listener is guaranteed to be notified of the POST_CHANGE resource change event before
	 * the Java core starts processing the resource change event itself.
	 * <p>
	 * Has no effect if an identical listener is already registered.
	 * </p>
	 *
	 * @param listener the listener
	 * @see #removePreProcessingResourceChangedListener(IResourceChangeListener)
	 * @since 3.0
	 * @deprecated use addPreProcessingResourceChangedListener(listener, IResourceChangeEvent.POST_CHANGE) instead
	 */
	public static void addPreProcessingResourceChangedListener(IResourceChangeListener listener) {
		addPreProcessingResourceChangedListener(listener, IResourceChangeEvent.POST_CHANGE);
	}

	/**
	 * Adds the given listener for resource change events of the given types to the Java core.
	 * The listener is guaranteed to be notified of the resource change event before
	 * the Java core starts processing the resource change event itself.
	 * <p>
	 * If an identical listener is already registered, the given event types are added to the event types
	 * of interest to the listener.
	 * </p>
	 * <p>
	 * Supported event types are:
	 * </p>
	 * <ul>
	 * <li>{@link IResourceChangeEvent#PRE_BUILD}</li>
	 * <li>{@link IResourceChangeEvent#POST_BUILD}</li>
	 * <li>{@link IResourceChangeEvent#POST_CHANGE}</li>
	 * <li>{@link IResourceChangeEvent#PRE_DELETE}</li>
	 * <li>{@link IResourceChangeEvent#PRE_CLOSE}</li>
	 * </ul>
	 * This list may increase in the future.
	 *
	 * @param listener the listener
	 * @param eventMask the bit-wise OR of all event types of interest to the
	 * listener
	 * @see #removePreProcessingResourceChangedListener(IResourceChangeListener)
	 * @see IResourceChangeEvent
	 * @since 3.2
	 */
	public static void addPreProcessingResourceChangedListener(IResourceChangeListener listener, int eventMask) {
		JavaModelManager.getDeltaState().addPreResourceChangedListener(listener, eventMask);
	}

	/**
	 * Configures the given marker for the given Java element.
	 * Used for markers, which denote a Java element rather than a resource.
	 *
	 * @param marker the marker to be configured
	 * @param element the Java element for which the marker needs to be configured
	 * @exception CoreException if the <code>IMarker.setAttribute</code> on the marker fails
	 */
	public void configureJavaElementMarker(IMarker marker, IJavaElement element)
		throws CoreException {
		if (element instanceof IMember)
			element = ((IMember) element).getClassFile();
		if (marker != null && element != null)
			marker.setAttribute(ATT_HANDLE_ID, element.getHandleIdentifier());
	}

	/**
	 * Returns the Java model element corresponding to the given handle identifier
	 * generated by <code>IJavaElement.getHandleIdentifier()</code>, or
	 * <code>null</code> if unable to create the associated element.
	 *
	 * @param handleIdentifier the given handle identifier
	 * @return the Java element corresponding to the handle identifier
	 */
	public static IJavaElement create(String handleIdentifier) {
		return create(handleIdentifier, DefaultWorkingCopyOwner.PRIMARY);
	}

	/**
	 * Returns the Java model element corresponding to the given handle identifier
	 * generated by <code>IJavaElement.getHandleIdentifier()</code>, or
	 * <code>null</code> if unable to create the associated element.
	 * If the returned Java element is an <code>ICompilationUnit</code> or an element
	 * inside a compilation unit, the compilation unit's owner is the given owner if such a
	 * working copy exists, otherwise the compilation unit is a primary compilation unit.
	 *
	 * @param handleIdentifier the given handle identifier
	 * @param owner the owner of the returned compilation unit, ignored if the returned
	 *   element is not a compilation unit, or an element inside a compilation unit
	 * @return the Java element corresponding to the handle identifier
	 * @since 3.0
	 */
	public static IJavaElement create(String handleIdentifier, WorkingCopyOwner owner) {
		if (handleIdentifier == null) {
			return null;
		}
		if (owner == null)
			owner = DefaultWorkingCopyOwner.PRIMARY;
		MementoTokenizer memento = new MementoTokenizer(handleIdentifier);
		JavaModel model = JavaModelManager.getJavaModelManager().getJavaModel();
		return model.getHandleFromMemento(memento, owner);
	}

	/**
	 * Returns the Java element corresponding to the given file, or
	 * <code>null</code> if unable to associate the given file
	 * with a Java element.
	 *
	 * <p>The file must be one of:</p>
	 *	<ul>
	 *	<li>a file with one of the {@link JavaCore#getJavaLikeExtensions()
	 *      Java-like extensions} - the element returned is the corresponding <code>ICompilationUnit</code></li>
	 *	<li>a <code>.class</code> file - the element returned is the corresponding <code>IClassFile</code></li>
	 *	<li>a ZIP archive (e.g. a <code>.jar</code>, a <code>.zip</code> file, etc.) - the element returned is the corresponding <code>IPackageFragmentRoot</code></li>
	 *	</ul>
	 * <p>
	 * Creating a Java element has the side effect of creating and opening all of the
	 * element's parents if they are not yet open.
	 * </p>
	 *
	 * @param file the given file
	 * @return the Java element corresponding to the given file, or
	 * <code>null</code> if unable to associate the given file
	 * with a Java element
	 */
	public static IJavaElement create(IFile file) {
		return JavaModelManager.create(file, null/*unknown java project*/);
	}
	/**
	 * Returns the package fragment or package fragment root corresponding to the given folder, or
	 * <code>null</code> if unable to associate the given folder with a Java element.
	 * <p>
	 * Note that a package fragment root is returned rather than a default package.
	 * </p>
	 * <p>
	 * Creating a Java element has the side effect of creating and opening all of the
	 * element's parents if they are not yet open.
	 * </p>
	 *
	 * @param folder the given folder
	 * @return the package fragment or package fragment root corresponding to the given folder, or
	 * <code>null</code> if unable to associate the given folder with a Java element
	 */
	public static IJavaElement create(IFolder folder) {
		return JavaModelManager.create(folder, null/*unknown java project*/);
	}
	/**
	 * Returns the Java project corresponding to the given project.
	 * <p>
	 * Creating a Java Project has the side effect of creating and opening all of the
	 * project's parents if they are not yet open.
	 * </p>
	 * <p>
	 * Note that no check is done at this time on the existence or the java nature of this project.
	 * </p>
	 *
	 * @param project the given project
	 * @return the Java project corresponding to the given project, null if the given project is null
	 */
	public static IJavaProject create(IProject project) {
		if (project == null) {
			return null;
		}
		JavaModel javaModel = JavaModelManager.getJavaModelManager().getJavaModel();
		return javaModel.getJavaProject(project);
	}
	/**
	 * Returns the Java element corresponding to the given resource, or
	 * <code>null</code> if unable to associate the given resource
	 * with a Java element.
	 * <p>
	 * The resource must be one of:
	 * </p>
	 *	<ul>
	 *	<li>a project - the element returned is the corresponding <code>IJavaProject</code></li>
	 *	<li>a file with one of the {@link JavaCore#getJavaLikeExtensions()
	 *      Java-like extensions} - the element returned is the corresponding <code>ICompilationUnit</code></li>
	 *	<li>a <code>.class</code> file - the element returned is the corresponding <code>IClassFile</code></li>
	 *	<li>a ZIP archive (e.g. a <code>.jar</code>, a <code>.zip</code> file, etc.) - the element returned is the corresponding <code>IPackageFragmentRoot</code></li>
	 *  <li>a folder - the element returned is the corresponding <code>IPackageFragmentRoot</code>
	 *    	or <code>IPackageFragment</code></li>
	 *  <li>the workspace root resource - the element returned is the <code>IJavaModel</code></li>
	 *	</ul>
	 * <p>
	 * Creating a Java element has the side effect of creating and opening all of the
	 * element's parents if they are not yet open.
	 * </p>
	 *
	 * @param resource the given resource
	 * @return the Java element corresponding to the given resource, or
	 * <code>null</code> if unable to associate the given resource
	 * with a Java element
	 */
	public static IJavaElement create(IResource resource) {
		return JavaModelManager.create(resource, null/*unknown java project*/);
	}
	/**
	 * Returns the Java element corresponding to the given file, its project being the given
	 * project. Returns <code>null</code> if unable to associate the given resource
	 * with a Java element.
	 * <p>
	 * The resource must be one of:
	 * </p>
	 *	<ul>
	 *	<li>a project - the element returned is the corresponding <code>IJavaProject</code></li>
	 *	<li>a file with one of the {@link JavaCore#getJavaLikeExtensions()
	 *      Java-like extensions} - the element returned is the corresponding <code>ICompilationUnit</code></li>
	 *	<li>a <code>.class</code> file - the element returned is the corresponding <code>IClassFile</code></li>
	 *	<li>a ZIP archive (e.g. a <code>.jar</code>, a <code>.zip</code> file, etc.) - the element returned is the corresponding <code>IPackageFragmentRoot</code></li>
	 *  <li>a folder - the element returned is the corresponding <code>IPackageFragmentRoot</code>
	 *    	or <code>IPackageFragment</code></li>
	 *  <li>the workspace root resource - the element returned is the <code>IJavaModel</code></li>
	 *	</ul>
	 * <p>
	 * Creating a Java element has the side effect of creating and opening all of the
	 * element's parents if they are not yet open.
	 * </p>
	 *
	 * @param resource the given resource
	 * @return the Java element corresponding to the given file, or
	 * <code>null</code> if unable to associate the given file
	 * with a Java element
	 * @since 3.3
	 */
	public static IJavaElement create(IResource resource, IJavaProject project) {
		return JavaModelManager.create(resource, project);
	}
	/**
	 * Returns the Java model.
	 *
	 * @param root the given root
	 * @return the Java model, or <code>null</code> if the root is null
	 */
	public static IJavaModel create(IWorkspaceRoot root) {
		if (root == null) {
			return null;
		}
		return JavaModelManager.getJavaModelManager().getJavaModel();
	}
	/**
	 * Creates and returns a class file element for
	 * the given <code>.class</code> file. Returns <code>null</code> if unable
	 * to recognize the class file.
	 *
	 * @param file the given <code>.class</code> file
	 * @return a class file element for the given <code>.class</code> file, or <code>null</code> if unable
	 * to recognize the class file
	 */
	public static IClassFile createClassFileFrom(IFile file) {
		return JavaModelManager.createClassFileFrom(file, null);
	}
	/**
	 * Creates and returns a compilation unit element for
	 * the given source file (i.e. a file with one of the {@link JavaCore#getJavaLikeExtensions()
	 * Java-like extensions}). Returns <code>null</code> if unable
	 * to recognize the compilation unit.
	 *
	 * @param file the given source file
	 * @return a compilation unit element for the given source file, or <code>null</code> if unable
	 * to recognize the compilation unit
	 */
	public static ICompilationUnit createCompilationUnitFrom(IFile file) {
		return JavaModelManager.createCompilationUnitFrom(file, null/*unknown java project*/);
	}
	/**
	 * Creates and returns a handle for the given JAR file.
	 * The Java model associated with the JAR's project may be
	 * created as a side effect.
	 *
	 * @param file the given JAR file
	 * @return a handle for the given JAR file, or <code>null</code> if unable to create a JAR package fragment root.
	 * (for example, if the JAR file represents a non-Java resource)
	 */
	public static IPackageFragmentRoot createJarPackageFragmentRootFrom(IFile file) {
		return JavaModelManager.createJarPackageFragmentRootFrom(file, null/*unknown java project*/);
	}

	/**
	 * Answers the project specific value for a given classpath container.
	 * In case this container path could not be resolved, then will answer <code>null</code>.
	 * Both the container path and the project context are supposed to be non-null.
	 * <p>
	 * The containerPath is a formed by a first ID segment followed with extra segments, which can be
	 * used as additional hints for resolution. If no container was ever recorded for this container path
	 * onto this project (using <code>setClasspathContainer</code>, then a
	 * <code>ClasspathContainerInitializer</code> will be activated if any was registered for this container
	 * ID onto the extension point "org.eclipse.jdt.core.classpathContainerInitializer".
	 * </p>
	 * <p>
	 * There is no assumption that the returned container must answer the exact same containerPath
	 * when requested <code>IClasspathContainer#getPath</code>.
	 * Indeed, the containerPath is just an indication for resolving it to an actual container object.
	 * </p>
	 * <p>
	 * Classpath container values are persisted locally to the workspace, but
	 * are not preserved from a session to another. It is thus highly recommended to register a
	 * <code>ClasspathContainerInitializer</code> for each referenced container
	 * (through the extension point "org.eclipse.jdt.core.ClasspathContainerInitializer").
	 * </p>
	 *
	 * @param containerPath the name of the container, which needs to be resolved
	 * @param project a specific project in which the container is being resolved
	 * @return the corresponding classpath container or <code>null</code> if unable to find one.
	 *
	 * @exception JavaModelException if an exception occurred while resolving the container, or if the resolved container
	 *   contains illegal entries (contains CPE_CONTAINER entries or null entries).
	 *
	 * @see ClasspathContainerInitializer
	 * @see IClasspathContainer
	 * @see #setClasspathContainer(IPath, IJavaProject[], IClasspathContainer[], IProgressMonitor)
	 * @since 2.0
	 */
	public static IClasspathContainer getClasspathContainer(IPath containerPath, IJavaProject project) throws JavaModelException {

	    JavaModelManager manager = JavaModelManager.getJavaModelManager();
		IClasspathContainer container = manager.getClasspathContainer(containerPath, project);
		if (container == JavaModelManager.CONTAINER_INITIALIZATION_IN_PROGRESS) {
		    return manager.getPreviousSessionContainer(containerPath, project);
		}
		return container;
	}

	/**
	 * Helper method finding the classpath container initializer registered for a given classpath container ID
	 * or <code>null</code> if none was found while iterating over the contributions to extension point to
	 * the extension point "org.eclipse.jdt.core.classpathContainerInitializer".
	 * <p>
	 * A containerID is the first segment of any container path, used to identify the registered container initializer.
	 * </p>
	 * @param containerID - a containerID identifying a registered initializer
	 * @return ClasspathContainerInitializer - the registered classpath container initializer or <code>null</code> if
	 * none was found.
	 * @since 2.1
	 */
	public static ClasspathContainerInitializer getClasspathContainerInitializer(String containerID) {
		Hashtable containerInitializersCache = JavaModelManager.getJavaModelManager().containerInitializersCache;
		ClasspathContainerInitializer initializer = (ClasspathContainerInitializer) containerInitializersCache.get(containerID);
		if (initializer == null) {
			initializer = computeClasspathContainerInitializer(containerID);
			if (initializer == null)
				return null;
			containerInitializersCache.put(containerID, initializer);
		}
		return initializer;
	}

	private static ClasspathContainerInitializer computeClasspathContainerInitializer(String containerID) {
		Plugin jdtCorePlugin = JavaCore.getPlugin();
		if (jdtCorePlugin == null) return null;

		IExtensionPoint extension = Platform.getExtensionRegistry().getExtensionPoint(JavaCore.PLUGIN_ID, JavaModelManager.CPCONTAINER_INITIALIZER_EXTPOINT_ID);
		if (extension != null) {
			IExtension[] extensions =  extension.getExtensions();
			for(int i = 0; i < extensions.length; i++){
				IConfigurationElement [] configElements = extensions[i].getConfigurationElements();
				for(int j = 0; j < configElements.length; j++){
					IConfigurationElement configurationElement = configElements[j];
					String initializerID = configurationElement.getAttribute("id"); //$NON-NLS-1$
					if (initializerID != null && initializerID.equals(containerID)){
						if (JavaModelManager.CP_RESOLVE_VERBOSE_ADVANCED)
							verbose_found_container_initializer(containerID, configurationElement);
						try {
							Object execExt = configurationElement.createExecutableExtension("class"); //$NON-NLS-1$
							if (execExt instanceof ClasspathContainerInitializer){
								return (ClasspathContainerInitializer)execExt;
							}
						} catch(CoreException e) {
							// executable extension could not be created: ignore this initializer
							if (JavaModelManager.CP_RESOLVE_VERBOSE || JavaModelManager.CP_RESOLVE_VERBOSE_FAILURE) {
								verbose_failed_to_instanciate_container_initializer(containerID, configurationElement);
								e.printStackTrace();
							}
						}
					}
				}
			}
		}
		return null;
	}

	private static void verbose_failed_to_instanciate_container_initializer(String containerID, IConfigurationElement configurationElement) {
		Util.verbose(
			"CPContainer INIT - failed to instanciate initializer\n" + //$NON-NLS-1$
			"	container ID: " + containerID + '\n' + //$NON-NLS-1$
			"	class: " + configurationElement.getAttribute("class"), //$NON-NLS-1$ //$NON-NLS-2$
			System.err);
	}

	private static void verbose_found_container_initializer(String containerID, IConfigurationElement configurationElement) {
		Util.verbose(
			"CPContainer INIT - found initializer\n" + //$NON-NLS-1$
			"	container ID: " + containerID + '\n' + //$NON-NLS-1$
			"	class: " + configurationElement.getAttribute("class")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Returns the path held in the given classpath variable.
	 * Returns <code>null</code> if unable to bind.
	 * <p>
	 * Classpath variable values are persisted locally to the workspace, and
	 * are preserved from session to session.
	 * </p>
	 * <p>
	 * Note that classpath variables can be contributed registered initializers for,
	 * using the extension point "org.eclipse.jdt.core.classpathVariableInitializer".
	 * If an initializer is registered for a variable, its persisted value will be ignored:
	 * its initializer will thus get the opportunity to rebind the variable differently on
	 * each session.
	 * </p>
	 *
	 * @param variableName the name of the classpath variable
	 * @return the path, or <code>null</code> if none
	 * @see #setClasspathVariable(String, IPath)
	 */
	public static IPath getClasspathVariable(final String variableName) {

	    JavaModelManager manager = JavaModelManager.getJavaModelManager();
		IPath variablePath = manager.variableGet(variableName);
		if (variablePath == JavaModelManager.VARIABLE_INITIALIZATION_IN_PROGRESS){
		    return manager.getPreviousSessionVariable(variableName);
		}

		if (variablePath != null) {
			if (variablePath == JavaModelManager.CP_ENTRY_IGNORE_PATH)
				return null;
			return variablePath;
		}

		// even if persisted value exists, initializer is given priority, only if no initializer is found the persisted value is reused
		final ClasspathVariableInitializer initializer = JavaCore.getClasspathVariableInitializer(variableName);
		if (initializer != null){
			if (JavaModelManager.CP_RESOLVE_VERBOSE)
				verbose_triggering_variable_initialization(variableName, initializer);
			if (JavaModelManager.CP_RESOLVE_VERBOSE_ADVANCED)
				verbose_triggering_variable_initialization_invocation_trace();
			manager.variablePut(variableName, JavaModelManager.VARIABLE_INITIALIZATION_IN_PROGRESS); // avoid initialization cycles
			boolean ok = false;
			try {
				// let OperationCanceledException go through
				// (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=59363)
				initializer.initialize(variableName);

				variablePath = manager.variableGet(variableName); // initializer should have performed side-effect
				if (variablePath == JavaModelManager.VARIABLE_INITIALIZATION_IN_PROGRESS) return null; // break cycle (initializer did not init or reentering call)
				if (JavaModelManager.CP_RESOLVE_VERBOSE_ADVANCED)
					verbose_variable_value_after_initialization(variableName, variablePath);
				manager.variablesWithInitializer.add(variableName);
				ok = true;
			} catch (RuntimeException | Error e) {
				if (JavaModelManager.CP_RESOLVE_VERBOSE || JavaModelManager.CP_RESOLVE_VERBOSE_FAILURE)
					e.printStackTrace();
				throw e;
			} finally {
				if (!ok) JavaModelManager.getJavaModelManager().variablePut(variableName, null); // flush cache
			}
		} else {
			if (JavaModelManager.CP_RESOLVE_VERBOSE_ADVANCED || JavaModelManager.CP_RESOLVE_VERBOSE_FAILURE)
				verbose_no_variable_initializer_found(variableName);
		}
		return variablePath;
	}

	private static void verbose_no_variable_initializer_found(String variableName) {
		Util.verbose(
			"CPVariable INIT - no initializer found\n" + //$NON-NLS-1$
			"	variable: " + variableName); //$NON-NLS-1$
	}

	private static void verbose_variable_value_after_initialization(String variableName, IPath variablePath) {
		Util.verbose(
			"CPVariable INIT - after initialization\n" + //$NON-NLS-1$
			"	variable: " + variableName +'\n' + //$NON-NLS-1$
			"	variable path: " + variablePath); //$NON-NLS-1$
	}

	private static void verbose_triggering_variable_initialization(String variableName, ClasspathVariableInitializer initializer) {
		Util.verbose(
			"CPVariable INIT - triggering initialization\n" + //$NON-NLS-1$
			"	variable: " + variableName + '\n' + //$NON-NLS-1$
			"	initializer: " + initializer); //$NON-NLS-1$
	}

	private static void verbose_triggering_variable_initialization_invocation_trace() {
		Util.verbose(
			"CPVariable INIT - triggering initialization\n" + //$NON-NLS-1$
			"	invocation trace:"); //$NON-NLS-1$
		new Exception("<Fake exception>").printStackTrace(System.out); //$NON-NLS-1$
	}

	/**
	 * Returns deprecation message of a given classpath variable.
	 *
	 * @param variableName
	 * @return A string if the classpath variable is deprecated, <code>null</code> otherwise.
	 * @since 3.3
	 */
	public static String getClasspathVariableDeprecationMessage(String variableName) {
	    JavaModelManager manager = JavaModelManager.getJavaModelManager();

		// Returns the stored deprecation message
		String message = manager.deprecatedVariables.get(variableName);
		if (message != null) {
		    return message;
		}

	    // If the variable has been already initialized, then there's no deprecation message
		IPath variablePath = manager.variableGet(variableName);
		if (variablePath != null && variablePath != JavaModelManager.VARIABLE_INITIALIZATION_IN_PROGRESS) {
			return null;
		}

		// Search for extension point to get the possible deprecation message
		Plugin jdtCorePlugin = JavaCore.getPlugin();
		if (jdtCorePlugin == null) return null;

		IExtensionPoint extension = Platform.getExtensionRegistry().getExtensionPoint(JavaCore.PLUGIN_ID, JavaModelManager.CPVARIABLE_INITIALIZER_EXTPOINT_ID);
		if (extension != null) {
			IExtension[] extensions =  extension.getExtensions();
			for(int i = 0; i < extensions.length; i++){
				IConfigurationElement [] configElements = extensions[i].getConfigurationElements();
				for(int j = 0; j < configElements.length; j++){
					IConfigurationElement configElement = configElements[j];
					String varAttribute = configElement.getAttribute("variable"); //$NON-NLS-1$
					if (variableName.equals(varAttribute)) {
						String deprecatedAttribute = configElement.getAttribute("deprecated"); //$NON-NLS-1$
						if (deprecatedAttribute != null) {
							return deprecatedAttribute;
						}
					}
				}
			}
		}
		return null;
	}

	/**
	 * Helper method finding the classpath variable initializer registered for a given classpath variable name
	 * or <code>null</code> if none was found while iterating over the contributions to extension point to
	 * the extension point "org.eclipse.jdt.core.classpathVariableInitializer".
	 *
 	 * @param variable the given variable
 	 * @return ClasspathVariableInitializer - the registered classpath variable initializer or <code>null</code> if
	 * none was found.
	 * @since 2.1
 	 */
	public static ClasspathVariableInitializer getClasspathVariableInitializer(String variable){

		Plugin jdtCorePlugin = JavaCore.getPlugin();
		if (jdtCorePlugin == null) return null;

		IExtensionPoint extension = Platform.getExtensionRegistry().getExtensionPoint(JavaCore.PLUGIN_ID, JavaModelManager.CPVARIABLE_INITIALIZER_EXTPOINT_ID);
		if (extension != null) {
			IExtension[] extensions =  extension.getExtensions();
			for(int i = 0; i < extensions.length; i++){
				IConfigurationElement [] configElements = extensions[i].getConfigurationElements();
				for(int j = 0; j < configElements.length; j++){
					IConfigurationElement configElement = configElements[j];
					try {
						String varAttribute = configElement.getAttribute("variable"); //$NON-NLS-1$
						if (variable.equals(varAttribute)) {
							if (JavaModelManager.CP_RESOLVE_VERBOSE_ADVANCED)
								verbose_found_variable_initializer(variable, configElement);
							Object execExt = configElement.createExecutableExtension("class"); //$NON-NLS-1$
							if (execExt instanceof ClasspathVariableInitializer){
								ClasspathVariableInitializer initializer = (ClasspathVariableInitializer)execExt;
								String deprecatedAttribute = configElement.getAttribute("deprecated"); //$NON-NLS-1$
								if (deprecatedAttribute != null) {
									JavaModelManager.getJavaModelManager().deprecatedVariables.put(variable, deprecatedAttribute);
								}
								String readOnlyAttribute = configElement.getAttribute("readOnly"); //$NON-NLS-1$
								if (JavaModelManager.TRUE.equals(readOnlyAttribute)) {
									JavaModelManager.getJavaModelManager().readOnlyVariables.add(variable);
								}
								return initializer;
							}
						}
					} catch(CoreException e){
						// executable extension could not be created: ignore this initializer
						if (JavaModelManager.CP_RESOLVE_VERBOSE || JavaModelManager.CP_RESOLVE_VERBOSE_FAILURE) {
							verbose_failed_to_instanciate_variable_initializer(variable, configElement);
							e.printStackTrace();
						}
					}
				}
			}
		}
		return null;
	}

	private static void verbose_failed_to_instanciate_variable_initializer(String variable, IConfigurationElement configElement) {
		Util.verbose(
			"CPContainer INIT - failed to instanciate initializer\n" + //$NON-NLS-1$
			"	variable: " + variable + '\n' + //$NON-NLS-1$
			"	class: " + configElement.getAttribute("class"), //$NON-NLS-1$ //$NON-NLS-2$
			System.err);
	}

	private static void verbose_found_variable_initializer(String variable, IConfigurationElement configElement) {
		Util.verbose(
			"CPVariable INIT - found initializer\n" + //$NON-NLS-1$
			"	variable: " + variable + '\n' + //$NON-NLS-1$
			"	class: " + configElement.getAttribute("class")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Returns the names of all known classpath variables.
	 * <p>
	 * Classpath variable values are persisted locally to the workspace, and
	 * are preserved from session to session.
	 * </p>
	 *
	 * @return the list of classpath variable names
	 * @see #setClasspathVariable(String, IPath)
	 */
	public static String[] getClasspathVariableNames() {
		return JavaModelManager.getJavaModelManager().variableNames();
	}

	/**
	 * Returns a table of all known configurable options with their default values.
	 * These options allow to configure the behaviour of the underlying components.
	 * The client may safely use the result as a template that they can modify and
	 * then pass to <code>setOptions</code>.
	 * <p>
	 * Helper constants have been defined on JavaCore for each of the option IDs
	 * (categorized in Code assist option ID, Compiler option ID and Core option ID)
	 * and some of their acceptable values (categorized in Option value). Some
	 * options accept open value sets beyond the documented constant values.
	 * </p>
	 * <p>
	 * Note: each release may add new options.
	 * </p>
	 *
	 * @return a table of all known configurable options with their default values
	 */
	public static Hashtable<String, String> getDefaultOptions(){
		return JavaModelManager.getJavaModelManager().getDefaultOptions();
	}

	/**
	 * Returns the workspace root default charset encoding.
	 *
	 * @return the name of the default charset encoding for workspace root.
	 * @see IContainer#getDefaultCharset()
	 * @see ResourcesPlugin#getEncoding()
	 * @since 3.0
	 */
	public static String getEncoding() {
		try {
			return ResourcesPlugin.getWorkspace().getRoot().getDefaultCharset();
		}
		catch (IllegalStateException ise) {
			// happen when there's no workspace (see bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=216817)
			// or when it is shutting down (see bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=60687)
			return System.getProperty("file.encoding"); //$NON-NLS-1$
		}
		catch (CoreException ce) {
			// fails silently and return plugin global encoding if core exception occurs
		}
		return ResourcesPlugin.getEncoding();
	}

	/**
	 * Returns an array that contains the resources generated by the Java builder when building the
	 * compilation units contained in the given region.
	 * <p>The contents of the array is accurate only if the elements of the given region have been built.</p>
	 * <p>The given region can contain instances of:</p>
	 * <ul>
	 * <li><code>org.eclipse.jdt.core.ICompilationUnit</code></li>
	 * <li><code>org.eclipse.jdt.core.IPackageFragment</code></li>
	 * <li><code>org.eclipse.jdt.core.IPackageFragmentRoot</code></li>
	 * <li><code>org.eclipse.jdt.core.IJavaProject</code></li>
	 * </ul>
	 * <p>All other types of <code>org.eclipse.jdt.core.IJavaElement</code> are ignored.</p>
	 *
	 * @param region the given region
	 * @param includesNonJavaResources a flag that indicates if non-java resources should be included
	 *
	 * @return an array that contains the resources generated by the Java builder when building the
	 * compilation units contained in the given region, an empty array if none
	 * @exception IllegalArgumentException if the given region is <code>null</code>
	 * @since 3.3
	 */
	public static IResource[] getGeneratedResources(IRegion region, boolean includesNonJavaResources) {
		if (region == null) throw new IllegalArgumentException("region cannot be null"); //$NON-NLS-1$
		IJavaElement[] elements = region.getElements();
		HashMap projectsStates = new HashMap();
		ArrayList collector = new ArrayList();
		for (int i = 0, max = elements.length; i < max; i++) {
			// collect all the java project
			IJavaElement element = elements[i];
			IJavaProject javaProject = element.getJavaProject();
			IProject project = javaProject.getProject();
			State state = null;
			State currentState = (State) projectsStates.get(project);
			if (currentState != null) {
				state = currentState;
			} else {
				state = (State) JavaModelManager.getJavaModelManager().getLastBuiltState(project, null);
				if (state != null) {
					projectsStates.put(project, state);
				}
			}
			if (state == null) continue;
			if (element.getElementType() == IJavaElement.JAVA_PROJECT) {
				IPackageFragmentRoot[] roots = null;
				try {
					roots = javaProject.getPackageFragmentRoots();
				} catch (JavaModelException e) {
					// ignore
				}
				if (roots == null) continue;
				IRegion region2 = JavaCore.newRegion();
				for (int j = 0; j < roots.length; j++) {
					region2.add(roots[j]);
				}
				IResource[] res = getGeneratedResources(region2, includesNonJavaResources);
				for (int j = 0, max2 = res.length; j < max2; j++) {
					collector.add(res[j]);
				}
				continue;
			}
			IPath outputLocation = null;
			try {
				outputLocation = javaProject.getOutputLocation();
			} catch (JavaModelException e) {
				// ignore
			}
			IJavaElement root = element;
			while (root != null && root.getElementType() != IJavaElement.PACKAGE_FRAGMENT_ROOT) {
				root = root.getParent();
			}
			if (root == null) continue;
			IPackageFragmentRoot packageFragmentRoot = (IPackageFragmentRoot) root;
			int rootPathSegmentCounts = packageFragmentRoot.getPath().segmentCount();
			try {
				IClasspathEntry entry = packageFragmentRoot.getRawClasspathEntry();
				IPath entryOutputLocation = entry.getOutputLocation();
				if (entryOutputLocation != null) {
					outputLocation = entryOutputLocation;
				}
			} catch (JavaModelException e) {
				e.printStackTrace();
			}
			if (outputLocation == null) continue;
			IContainer container = (IContainer) project.getWorkspace().getRoot().findMember(outputLocation);
			switch(element.getElementType()) {
				case IJavaElement.COMPILATION_UNIT :
					// get the .class files generated when this element was built
					ICompilationUnit unit = (ICompilationUnit) element;
					getGeneratedResource(unit, container, state, rootPathSegmentCounts, collector);
					break;
				case IJavaElement.PACKAGE_FRAGMENT :
					// collect all the .class files generated when all the units in this package were built
					IPackageFragment fragment = (IPackageFragment) element;
					ICompilationUnit[] compilationUnits = null;
					try {
						compilationUnits = fragment.getCompilationUnits();
					} catch (JavaModelException e) {
						// ignore
					}
					if (compilationUnits == null) continue;
					for (int j = 0, max2 = compilationUnits.length; j < max2; j++) {
						getGeneratedResource(compilationUnits[j], container, state, rootPathSegmentCounts, collector);
					}
					if (includesNonJavaResources) {
						// retrieve all non-java resources from the output location using the package fragment path
						Object[] nonJavaResources = null;
						try {
							nonJavaResources = fragment.getNonJavaResources();
						} catch (JavaModelException e) {
							// ignore
						}
						if (nonJavaResources != null) {
							addNonJavaResources(nonJavaResources, container, rootPathSegmentCounts, collector);
						}
					}
					break;
				case IJavaElement.PACKAGE_FRAGMENT_ROOT :
					// collect all the .class files generated when all the units in this package were built
					IPackageFragmentRoot fragmentRoot = (IPackageFragmentRoot) element;
					if (fragmentRoot.isArchive()) continue;
					IJavaElement[] children = null;
					try {
						children = fragmentRoot.getChildren();
					} catch (JavaModelException e) {
						// ignore
					}
					if (children == null) continue;
					for (int j = 0, max2 = children.length; j < max2; j++) {
						fragment = (IPackageFragment) children[j];
						ICompilationUnit[] units = null;
						try {
							units = fragment.getCompilationUnits();
						} catch (JavaModelException e) {
							// ignore
						}
						if (units == null) continue;
						for (int n = 0, max3 = units.length; n < max3; n++) {
							getGeneratedResource(units[n], container, state, rootPathSegmentCounts, collector);
						}
						if (includesNonJavaResources) {
							// retrieve all non-java resources from the output location using the package fragment path
							Object[] nonJavaResources = null;
							try {
								nonJavaResources = fragment.getNonJavaResources();
							} catch (JavaModelException e) {
								// ignore
							}
							if (nonJavaResources != null) {
								addNonJavaResources(nonJavaResources, container, rootPathSegmentCounts, collector);
							}
						}
					}
					break;
			}
		}
		int size = collector.size();
		if (size != 0) {
			IResource[] result = new IResource[size];
			collector.toArray(result);
			return result;
		}
		return NO_GENERATED_RESOURCES;
	}

	private static void getGeneratedResource(ICompilationUnit unit,
			IContainer container,
			State state,
			int rootPathSegmentCounts,
			ArrayList collector) {
		IResource resource = unit.getResource();
		char[][] typeNames = state.getDefinedTypeNamesFor(resource.getProjectRelativePath().toString());
		if (typeNames != null) {
			IPath path = unit.getPath().removeFirstSegments(rootPathSegmentCounts).removeLastSegments(1);
			for (int j = 0, max2 = typeNames.length; j < max2; j++) {
				IPath localPath = path.append(new String(typeNames[j]) + ".class"); //$NON-NLS-1$
				IResource member = container.findMember(localPath);
				if (member != null && member.exists()) {
					collector.add(member);
				}
			}
		} else {
			IPath path = unit.getPath().removeFirstSegments(rootPathSegmentCounts).removeLastSegments(1);
			path = path.append(Util.getNameWithoutJavaLikeExtension(unit.getElementName()) + ".class"); //$NON-NLS-1$
			IResource member = container.findMember(path);
			if (member != null && member.exists()) {
				collector.add(member);
			}
		}
	}

	/**
	 * Returns the single instance of the Java core plug-in runtime class.
	 * Equivalent to <code>(JavaCore) getPlugin()</code>.
	 *
	 * @return the single instance of the Java core plug-in runtime class
	 */
	public static JavaCore getJavaCore() {
		return (JavaCore) getPlugin();
	}

	/**
	 * Returns the list of known Java-like extensions.
	 * Java like extension are defined in the {@link org.eclipse.core.runtime.Platform#getContentTypeManager()
	 * content type manager} for the {@link #JAVA_SOURCE_CONTENT_TYPE}.
	 * <p>
	 * Note that a Java-like extension doesn't include the leading dot ('.').
	 * Also note that the "java" extension is always defined as a Java-like extension.
	 * </p>
	 *
	 * @return the list of known Java-like extensions.
	 * @since 3.2
	 */
	public static String[] getJavaLikeExtensions() {
		return CharOperation.toStrings(Util.getJavaLikeExtensions());
	}

	/**
	 * Helper method for returning one option value only. Equivalent to <code>(String)JavaCore.getOptions().get(optionName)</code>
	 * Note that it may answer <code>null</code> if this option does not exist.
	 * <p>
	 * Helper constants have been defined on JavaCore for each of the option IDs
	 * (categorized in Code assist option ID, Compiler option ID and Core option ID)
	 * and some of their acceptable values (categorized in Option value). Some
	 * options accept open value sets beyond the documented constant values.
	 * </p>
	 * <p>
	 * Note: each release may add new options.
	 * </p>
	 *
	 * @param optionName the name of an option
	 * @return the String value of a given option
	 * @see JavaCore#getDefaultOptions()
	 * @see JavaCorePreferenceInitializer for changing default settings
	 * @since 2.0
	 */
	public static String getOption(String optionName) {
		return JavaModelManager.getJavaModelManager().getOption(optionName);
	}

	/**
	 * Returns the option that can be used to configure the severity of the
	 * compiler problem identified by <code>problemID</code> if any,
	 * <code>null</code> otherwise. Non-null return values are taken from the
	 * constants defined by this class whose names start with
	 * <code>COMPILER_PB</code> and for which the possible values of the
	 * option are defined by <code>{ "error", "warning", "info", "ignore" }</code>. A
	 * null return value means that the provided problem ID is unknown or that
	 * it matches a problem whose severity cannot be configured.
	 * @param problemID one of the problem IDs defined by {@link IProblem}
	 * @return the option that can be used to configure the severity of the
	 *         compiler problem identified by <code>problemID</code> if any,
	 *         <code>null</code> otherwise
	 * @since 3.4
	 */
	public static String getOptionForConfigurableSeverity(int problemID) {
		return CompilerOptions.optionKeyFromIrritant(ProblemReporter.getIrritant(problemID));
	}

	/**
	 * Returns the option that can be used to configure the severity of the
	 * compiler build path problem identified by <code>id</code> if any,
	 * <code>null</code> otherwise. Non-null return values are taken from the
	 * constants defined by this class whose names start with
	 * <code>CORE_</code> and for which the possible values of the
	 * option are defined by <code>{ "error", "warning", "info", "ignore" }</code>. A
	 * null return value means that the provided id is unknown or that
	 * it matches a problem whose severity cannot be configured.
	 * @param id one of the build path problems defined in IJavaModelStatusConstants
	 * @return the option that can be used to configure the severity of the
	 *         compiler problem identified by <code>id</code> if any,
	 *         <code>null</code> otherwise
	 * @since 3.16
	 */
	public static String getOptionForConfigurableBuildPathProblemSeverity(int id) {
		switch (id) {
			case IJavaModelStatusConstants.CLASSPATH_CYCLE:
				return JavaCore.CORE_CIRCULAR_CLASSPATH;
			case IJavaModelStatusConstants.INCOMPATIBLE_JDK_LEVEL:
				return JavaCore.CORE_INCOMPATIBLE_JDK_LEVEL;
			case IJavaModelStatusConstants.OUTPUT_LOCATION_OVERLAPPING_ANOTHER_SOURCE:
				return JavaCore.CORE_OUTPUT_LOCATION_OVERLAPPING_ANOTHER_SOURCE;
			case IJavaModelStatusConstants.MAIN_ONLY_PROJECT_DEPENDS_ON_TEST_ONLY_PROJECT:
				return JavaCore.CORE_MAIN_ONLY_PROJECT_HAS_TEST_ONLY_DEPENDENCY;
			case IJavaModelStatusConstants.INVALID_CLASSPATH:
				return JavaCore.CORE_INCOMPLETE_CLASSPATH;
		}
		return null;
	}

	/**
	 * Returns the table of the current options. Initially, all options have their default values,
	 * and this method returns a table that includes all known options.
	 * <p>
	 * Helper constants have been defined on JavaCore for each of the option IDs
	 * (categorized in Code assist option ID, Compiler option ID and Core option ID)
	 * and some of their acceptable values (categorized in Option value). Some
	 * options accept open value sets beyond the documented constant values.
	 * </p>
	 * <p>
	 * Note: each release may add new options.
	 * </p>
	 * <p>Returns a default set of options even if the platform is not running.</p>
	 *
	 * @return table of current settings of all options
	 *   (key type: <code>String</code>; value type: <code>String</code>)
	 * @see #getDefaultOptions()
	 * @see JavaCorePreferenceInitializer for changing default settings
	 */
	public static Hashtable<String, String> getOptions() {
		return JavaModelManager.getJavaModelManager().getOptions();
	}

	/**
	 * Returns the single instance of the Java core plug-in runtime class.
	 *
	 * @return the single instance of the Java core plug-in runtime class
	 */
	public static Plugin getPlugin() {
		return JAVA_CORE_PLUGIN;
	}

	/**
	 * This is a helper method, which returns the resolved classpath entry denoted
	 * by a given entry (if it is a variable entry). It is obtained by resolving the variable
	 * reference in the first segment. Returns <code>null</code> if unable to resolve using
	 * the following algorithm:
	 * <ul>
	 * <li> if variable segment cannot be resolved, returns <code>null</code></li>
	 * <li> finds a project, JAR or binary folder in the workspace at the resolved path location</li>
	 * <li> if none finds an external JAR file or folder outside the workspace at the resolved path location </li>
	 * <li> if none returns <code>null</code></li>
	 * </ul>
	 * <p>
	 * Variable source attachment path and root path are also resolved and recorded in the resulting classpath entry.
	 * </p>
	 * <p>
	 * NOTE: This helper method does not handle classpath containers, for which should rather be used
	 * <code>JavaCore#getClasspathContainer(IPath, IJavaProject)</code>.
	 * </p>
	 *
	 * @param entry the given variable entry
	 * @return the resolved library or project classpath entry, or <code>null</code>
	 *   if the given variable entry could not be resolved to a valid classpath entry
	 */
	public static IClasspathEntry getResolvedClasspathEntry(IClasspathEntry entry) {
		return JavaModelManager.getJavaModelManager().resolveVariableEntry(entry, false/*don't use previous session value*/);
	}


	/**
	 * Resolve a variable path (helper method).
	 *
	 * @param variablePath the given variable path
	 * @return the resolved variable path or <code>null</code> if none
	 */
	public static IPath getResolvedVariablePath(IPath variablePath) {
		return JavaModelManager.getJavaModelManager().getResolvedVariablePath(variablePath, false/*don't use previous session value*/);
	}

	/**
	 * Answers the shared working copies currently registered for this buffer factory.
	 * Working copies can be shared by several clients using the same buffer factory,see
	 * <code>IWorkingCopy.getSharedWorkingCopy</code>.
	 *
	 * @param factory the given buffer factory
	 * @return the list of shared working copies for a given buffer factory
	 * @since 2.0
	 * @deprecated Use {@link #getWorkingCopies(WorkingCopyOwner)} instead
	 */
	public static IWorkingCopy[] getSharedWorkingCopies(IBufferFactory factory){

		// if factory is null, default factory must be used
		if (factory == null) factory = BufferManager.getDefaultBufferManager().getDefaultBufferFactory();

		return getWorkingCopies(org.eclipse.jdt.internal.core.BufferFactoryWrapper.create(factory));
	}

	/**
	 * Returns the names of all defined user libraries. The corresponding classpath container path
	 * is the name appended to the USER_LIBRARY_CONTAINER_ID.
	 * @return Return an array containing the names of all known user defined.
	 * @since 3.0
	 */
	public static String[] getUserLibraryNames() {
		 return JavaModelManager.getUserLibraryManager().getUserLibraryNames();
	}

	/**
	 * Returns the working copies that have the given owner.
	 * Only compilation units in working copy mode are returned.
	 * If the owner is <code>null</code>, primary working copies are returned.
	 *
	 * @param owner the given working copy owner or <code>null</code> for primary working copy owner
	 * @return the list of working copies for a given owner
	 * @since 3.0
	 */
	public static ICompilationUnit[] getWorkingCopies(WorkingCopyOwner owner){

		JavaModelManager manager = JavaModelManager.getJavaModelManager();
		if (owner == null) owner = DefaultWorkingCopyOwner.PRIMARY;
		ICompilationUnit[] result = manager.getWorkingCopies(owner, false/*don't add primary WCs*/);
		if (result == null) return JavaModelManager.NO_WORKING_COPY;
		return result;
	}

	/**
	 * Initializes JavaCore internal structures to allow subsequent operations (such
	 * as the ones that need a resolved classpath) to run full speed. A client may
	 * choose to call this method in a background thread early after the workspace
	 * has started so that the initialization is transparent to the user.
	 * <p>
	 * However calling this method is optional. Services will lazily perform
	 * initialization when invoked. This is only a way to reduce initialization
	 * overhead on user actions, if it can be performed before at some
	 * appropriate moment.
	 * </p><p>
	 * This initialization runs across all Java projects in the workspace. Thus the
	 * workspace root scheduling rule is used during this operation.
	 * </p><p>
	 * This method may return before the initialization is complete. The
	 * initialization will then continue in a background thread.
	 * </p><p>
	 * This method can be called concurrently.
	 * </p>
	 *
	 * @param monitor a progress monitor, or <code>null</code> if progress
	 *    reporting and cancellation are not desired
	 * @exception CoreException if the initialization fails,
	 * 		the status of the exception indicates the reason of the failure
	 * @since 3.1
	 */
	public static void initializeAfterLoad(IProgressMonitor monitor) throws CoreException {
		SubMonitor mainMonitor = SubMonitor.convert(monitor, Messages.javamodel_initialization, 100);
		mainMonitor.subTask(Messages.javamodel_configuring_classpath_containers);

		// initialize all containers and variables
		JavaModelManager manager = JavaModelManager.getJavaModelManager();
		try {
			SubMonitor subMonitor = mainMonitor.split(50).setWorkRemaining(100); // 50% of the time is spent in initializing containers and variables
			subMonitor.split(5); // give feedback to the user that something is happening
			manager.batchContainerInitializationsProgress.initializeAfterLoadMonitor.set(subMonitor);
			if (manager.forceBatchInitializations(true/*initAfterLoad*/)) { // if no other thread has started the batch container initializations
				manager.getClasspathContainer(Path.EMPTY, null); // force the batch initialization
			} else { // else wait for the batch initialization to finish
				while (manager.batchContainerInitializations == JavaModelManager.BATCH_INITIALIZATION_IN_PROGRESS) {
					subMonitor.subTask(manager.batchContainerInitializationsProgress.subTaskName);
					subMonitor.split(manager.batchContainerInitializationsProgress.getWorked());
					synchronized(manager) {
						try {
							manager.wait(100);
						} catch (InterruptedException e) {
							// continue
						}
					}
				}
			}
		} finally {
			manager.batchContainerInitializationsProgress.initializeAfterLoadMonitor.remove();
		}

		// avoid leaking source attachment properties (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=183413 )
		// and recreate links for external folders if needed
		mainMonitor.subTask(Messages.javamodel_resetting_source_attachment_properties);
		final IJavaProject[] projects = manager.getJavaModel().getJavaProjects();
		HashSet visitedPaths = new HashSet();
		ExternalFoldersManager externalFoldersManager = JavaModelManager.getExternalManager();
		for (int i = 0, length = projects.length; i < length; i++) {
			JavaProject javaProject = (JavaProject) projects[i];
			IClasspathEntry[] classpath;
			try {
				classpath = javaProject.getResolvedClasspath();
			} catch (JavaModelException e) {
				// project no longer exist: ignore
				continue;
			}
			if (classpath != null) {
				for (int j = 0, length2 = classpath.length; j < length2; j++) {
					IClasspathEntry entry = classpath[j];
					if (entry.getSourceAttachmentPath() != null) {
						IPath entryPath = entry.getPath();
						if (visitedPaths.add(entryPath)) {
							Util.setSourceAttachmentProperty(entryPath, null);
						}
					}
					// else source might have been attached by IPackageFragmentRoot#attachSource(...), we keep it
					if (entry.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
						IPath entryPath = entry.getPath();
						if (ExternalFoldersManager.isExternalFolderPath(entryPath) && externalFoldersManager.getFolder(entryPath) == null) {
							externalFoldersManager.addFolder(entryPath, true);
						}
					}
				}
			}
		}
		try {
			externalFoldersManager.createPendingFolders(mainMonitor.split(1));
		}
		catch(JavaModelException jme) {
			// Creation of external folder project failed. Log it and continue;
			Util.log(jme, "Error while processing external folders"); //$NON-NLS-1$
		}

		// ensure external jars are refreshed (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=93668)
		// before search is initialized (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=405051)
		final JavaModel model = manager.getJavaModel();
		try {
			mainMonitor.subTask(Messages.javamodel_refreshing_external_jars);
			model.refreshExternalArchives(
				null/*refresh all projects*/,
				mainMonitor.split(1) // 1% of the time is spent in jar refresh
			);
		} catch (JavaModelException e) {
			// refreshing failed: ignore
		}

		// initialize delta state
		mainMonitor.subTask(Messages.javamodel_initializing_delta_state);
		manager.deltaState.rootsAreStale = true; // in case it was already initialized before we cleaned up the source attachment properties
		manager.deltaState.initializeRoots(true/*initAfteLoad*/);

		// dummy query for waiting until the indexes are ready
		mainMonitor.subTask(Messages.javamodel_configuring_searchengine);
		// 47% of the time is spent in the dummy search
		updateLegacyIndex(mainMonitor.split(47));

		// check if the build state version number has changed since last session
		// (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=98969)
		mainMonitor.subTask(Messages.javamodel_getting_build_state_number);
		QualifiedName qName = new QualifiedName(JavaCore.PLUGIN_ID, "stateVersionNumber"); //$NON-NLS-1$
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		String versionNumber = null;
		try {
			versionNumber = root.getPersistentProperty(qName);
		} catch (CoreException e) {
			// could not read version number: consider it is new
		}
		String newVersionNumber = Byte.toString(State.VERSION);
		if (!newVersionNumber.equals(versionNumber)) {
			// build state version number has changed: touch every projects to force a rebuild
			if (JavaBuilder.DEBUG)
				System.out.println("Build state version number has changed"); //$NON-NLS-1$
			IWorkspaceRunnable runnable = new IWorkspaceRunnable() {
				@Override
				public void run(IProgressMonitor progressMonitor2) throws CoreException {
					for (int i = 0, length = projects.length; i < length; i++) {
						IJavaProject project = projects[i];
						try {
							if (JavaBuilder.DEBUG)
								System.out.println("Touching " + project.getElementName()); //$NON-NLS-1$
							new ClasspathValidation((JavaProject) project).validate(); // https://bugs.eclipse.org/bugs/show_bug.cgi?id=287164
							project.getProject().touch(progressMonitor2);
						} catch (CoreException e) {
							// could not touch this project: ignore
						}
					}
				}
			};
			mainMonitor.subTask(Messages.javamodel_building_after_upgrade);
			try {
				ResourcesPlugin.getWorkspace().run(runnable, mainMonitor.split(1));
			} catch (CoreException e) {
				// could not touch all projects
			}
			try {
				root.setPersistentProperty(qName, newVersionNumber);
			} catch (CoreException e) {
				Util.log(e, "Could not persist build state version number"); //$NON-NLS-1$
			}
		}
	}

	private static void updateLegacyIndex(IProgressMonitor monitor) {
		SearchEngine engine = new SearchEngine();
		IJavaSearchScope scope = SearchEngine.createWorkspaceScope();
		try {
			engine.searchAllTypeNames(
				null,
				SearchPattern.R_EXACT_MATCH,
				"!@$#!@".toCharArray(), //$NON-NLS-1$
				SearchPattern.R_PATTERN_MATCH | SearchPattern.R_CASE_SENSITIVE,
				IJavaSearchConstants.CLASS,
				scope,
				new TypeNameRequestor() {
					@Override
					public void acceptType(
						int modifiers,
						char[] packageName,
						char[] simpleTypeName,
						char[][] enclosingTypeNames,
						String path) {
						// no type to accept
					}
				},
				// will not activate index query caches if indexes are not ready, since it would take to long
				// to wait until indexes are fully rebuild
				IJavaSearchConstants.CANCEL_IF_NOT_READY_TO_SEARCH,
				monitor
			);
		} catch (JavaModelException e) {
			// /search failed: ignore
		} catch (OperationCanceledException e) {
			if (monitor.isCanceled())
				throw e;
			// else indexes were not ready: catch the exception so that jars are still refreshed
		}
	}

	/**
	 * Returns whether a given classpath variable is read-only or not.
	 *
	 * @param variableName
	 * @return <code>true</code> if the classpath variable is read-only,
	 * 	<code>false</code> otherwise.
	 * @since 3.3
	 */
	public static boolean isClasspathVariableReadOnly(String variableName) {
	    return JavaModelManager.getJavaModelManager().readOnlyVariables.contains(variableName);
	}

	/**
	 * Returns whether the given file name's extension is a Java-like extension.
	 *
	 * @return whether the given file name's extension is a Java-like extension
	 * @see #getJavaLikeExtensions()
	 * @since 3.2
	 */
	public static boolean isJavaLikeFileName(String fileName) {
		return Util.isJavaLikeFileName(fileName);
	}

	/**
	 * Returns whether the given marker references the given Java element.
	 * Used for markers, which denote a Java element rather than a resource.
	 *
	 * @param element the element
	 * @param marker the marker
	 * @return <code>true</code> if the marker references the element, false otherwise
	 * @exception CoreException if the <code>IMarker.getAttribute</code> on the marker fails
	 */
	public static boolean isReferencedBy(IJavaElement element, IMarker marker) throws CoreException {

		// only match units or classfiles
		if (element instanceof IMember){
			IMember member = (IMember) element;
			if (member.isBinary()){
				element = member.getClassFile();
			} else {
				element = member.getCompilationUnit();
			}
		}
		if (element == null) return false;
		if (marker == null) return false;

		String markerHandleId = (String)marker.getAttribute(ATT_HANDLE_ID);
		if (markerHandleId == null) return false;

		IJavaElement markerElement = JavaCore.create(markerHandleId);
		while (true){
			if (element.equals(markerElement)) return true; // external elements may still be equal with different handleIDs.

			// cycle through enclosing types in case marker is associated with a classfile (15568)
			if (markerElement instanceof IOrdinaryClassFile){
				IType enclosingType = ((IOrdinaryClassFile)markerElement).getType().getDeclaringType();
				if (enclosingType != null){
					markerElement = enclosingType.getClassFile(); // retry with immediate enclosing classfile
					continue;
				}
			}
			break;
		}
		return false;
	}

	/**
	 * Returns whether the given marker delta references the given Java element.
	 * Used for markers deltas, which denote a Java element rather than a resource.
	 *
	 * @param element the element
	 * @param markerDelta the marker delta
	 * @return <code>true</code> if the marker delta references the element
	 * @exception CoreException if the  <code>IMarkerDelta.getAttribute</code> on the marker delta fails
	 */
	public static boolean isReferencedBy(IJavaElement element, IMarkerDelta markerDelta) throws CoreException {

		// only match units or classfiles
		if (element instanceof IMember){
			IMember member = (IMember) element;
			if (member.isBinary()){
				element = member.getClassFile();
			} else {
				element = member.getCompilationUnit();
			}
		}
		if (element == null) return false;
		if (markerDelta == null) return false;

		String markerDeltarHandleId = (String)markerDelta.getAttribute(ATT_HANDLE_ID);
		if (markerDeltarHandleId == null) return false;

		IJavaElement markerElement = JavaCore.create(markerDeltarHandleId);
		while (true){
			if (element.equals(markerElement)) return true; // external elements may still be equal with different handleIDs.

			// cycle through enclosing types in case marker is associated with a classfile (15568)
			if (markerElement instanceof IOrdinaryClassFile){
				IType enclosingType = ((IOrdinaryClassFile)markerElement).getType().getDeclaringType();
				if (enclosingType != null){
					markerElement = enclosingType.getClassFile(); // retry with immediate enclosing classfile
					continue;
				}
			}
			break;
		}
		return false;
	}

	/**
	 * Creates and returns a new access rule with the given file pattern and kind.
	 * <p>
	 * The rule kind is one of {@link IAccessRule#K_ACCESSIBLE}, {@link IAccessRule#K_DISCOURAGED},
	 * or {@link IAccessRule#K_NON_ACCESSIBLE}, optionally combined with {@link IAccessRule#IGNORE_IF_BETTER},
	 * e.g. <code>IAccessRule.K_NON_ACCESSIBLE | IAccessRule.IGNORE_IF_BETTER</code>.
	 * </p>
	 *
	 * @param filePattern the file pattern this access rule should match
	 * @param kind one of {@link IAccessRule#K_ACCESSIBLE}, {@link IAccessRule#K_DISCOURAGED},
	 *                     or {@link IAccessRule#K_NON_ACCESSIBLE}, optionally combined with
	 *                     {@link IAccessRule#IGNORE_IF_BETTER}
	 * @return a new access rule
	 * @since 3.1
	 *
	 * @see IClasspathEntry#getExclusionPatterns()
	 */
	public static IAccessRule newAccessRule(IPath filePattern, int kind) {
		return JavaModelManager.getJavaModelManager().getAccessRule(filePattern, kind);
	}

	/**
	 * Creates and returns a new classpath attribute with the given name and the given value.
	 *
	 * @return a new classpath attribute
	 * @since 3.1
	 */
	public static IClasspathAttribute newClasspathAttribute(String name, String value) {
		return new ClasspathAttribute(name, value);
	}

	/**
	 * Creates and returns a new classpath entry of kind <code>CPE_CONTAINER</code>
	 * for the given path. This method is fully equivalent to calling
	 * {@link #newContainerEntry(IPath, IAccessRule[], IClasspathAttribute[], boolean)
	 * newContainerEntry(containerPath, new IAccessRule[0], new IClasspathAttribute[0], false)}.
	 *
	 * @param containerPath the path identifying the container, it must be formed of two
	 * 	segments
	 * @return a new container classpath entry
	 *
	 * @see JavaCore#getClasspathContainer(IPath, IJavaProject)
	 * @since 2.0
	 */
	public static IClasspathEntry newContainerEntry(IPath containerPath) {
		return newContainerEntry(
		containerPath,
		ClasspathEntry.NO_ACCESS_RULES,
		ClasspathEntry.NO_EXTRA_ATTRIBUTES,
		false/*not exported*/);
	}

	/**
	 * Creates and returns a new classpath entry of kind <code>CPE_CONTAINER</code>
	 * for the given path. This method is fully equivalent to calling
	 * {@link #newContainerEntry(IPath, IAccessRule[], IClasspathAttribute[], boolean)
	 * newContainerEntry(containerPath, new IAccessRule[0], new IClasspathAttribute[0], isExported)}.
	 *
	 * @param containerPath the path identifying the container, it must be formed of at least
	 * 	one segment (ID+hints)
	 * @param isExported a boolean indicating whether this entry is contributed to dependent
	 *    projects in addition to the output location
	 * @return a new container classpath entry
	 *
	 * @see JavaCore#getClasspathContainer(IPath, IJavaProject)
	 * @see JavaCore#setClasspathContainer(IPath, IJavaProject[], IClasspathContainer[], IProgressMonitor)
	 * @since 2.0
	 */
	public static IClasspathEntry newContainerEntry(IPath containerPath, boolean isExported) {
		return newContainerEntry(
			containerPath,
			ClasspathEntry.NO_ACCESS_RULES,
			ClasspathEntry.NO_EXTRA_ATTRIBUTES,
			isExported);
	}

	/**
	 * Creates and returns a new classpath entry of kind <code>CPE_CONTAINER</code>
	 * for the given path. The path of the container will be used during resolution so as to map this
	 * container entry to a set of other classpath entries the container is acting for.
	 * <p>
	 * A container entry allows to express indirect references to a set of libraries, projects and variable entries,
	 * which can be interpreted differently for each Java project where it is used.
	 * A classpath container entry can be resolved using <code>JavaCore.getResolvedClasspathContainer</code>,
	 * and updated with <code>JavaCore.classpathContainerChanged</code>
	 * </p>
	 * <p>
	 * A container is exclusively resolved by a <code>ClasspathContainerInitializer</code> registered onto the
	 * extension point "org.eclipse.jdt.core.classpathContainerInitializer".
	 * </p>
	 * <p>
	 * A container path must be formed of at least one segment, where:
	 * </p>
	 * <ul>
	 * <li> the first segment is a unique ID identifying the target container, there must be a container initializer registered
	 * 	onto this ID through the extension point  "org.eclipse.jdt.core.classpathContainerInitializer". </li>
	 * <li> the remaining segments will be passed onto the initializer, and can be used as additional
	 * 	hints during the initialization phase. </li>
	 * </ul>
	 * <p>
	 * Example of an ClasspathContainerInitializer for a classpath container denoting a default JDK container:
	 * </p>
	 * <pre>
	 * containerEntry = JavaCore.newContainerEntry(new Path("MyProvidedJDK/default"));
	 *
	 * &lt;extension
	 *    point="org.eclipse.jdt.core.classpathContainerInitializer"&gt;
	 *    &lt;containerInitializer
	 *       id="MyProvidedJDK"
	 *       class="com.example.MyInitializer"/&gt;
	 * </pre>
	 * <p>
	 * The access rules determine the set of accessible source and class files
	 * in the container. If the list of access rules is empty, then all files
	 * in this container are accessible.
	 * See {@link IAccessRule} for a detailed description of access
	 * rules. Note that if an entry defined by the container defines access rules,
	 * then these access rules are combined with the given access rules.
	 * The given access rules are considered first, then the entry's access rules are
	 * considered.
	 * </p>
	 * <p>
	 * The <code>extraAttributes</code> list contains name/value pairs that must be persisted with
	 * this entry. If no extra attributes are provided, an empty array must be passed in.<br>
	 * Note that this list should not contain any duplicate name.
	 * </p>
	 * <p>
	 * The <code>isExported</code> flag indicates whether this entry is contributed to dependent
	 * projects. If not exported, dependent projects will not see any of the classes from this entry.
	 * If exported, dependent projects will concatenate the accessible files patterns of this entry with the
	 * accessible files patterns of the projects, and they will concatenate the non accessible files patterns of this entry
	 * with the non accessible files patterns of the project.
	 * </p>
	 * <p>
	 * Note that this operation does not attempt to validate classpath containers
	 * or access the resources at the given paths.
	 * </p>
	 *
	 * @param containerPath the path identifying the container, it must be formed of at least
	 * 	one segment (ID+hints)
	 * @param accessRules the possibly empty list of access rules for this entry
	 * @param extraAttributes the possibly empty list of extra attributes to persist with this entry
	 * @param isExported a boolean indicating whether this entry is contributed to dependent
	 *    projects in addition to the output location
	 * @return a new container classpath entry
	 *
	 * @see JavaCore#getClasspathContainer(IPath, IJavaProject)
	 * @see JavaCore#setClasspathContainer(IPath, IJavaProject[], IClasspathContainer[], IProgressMonitor)
	 * @see JavaCore#newContainerEntry(IPath, boolean)
	 * @see JavaCore#newAccessRule(IPath, int)
	 * @since 3.1
	 */
	public static IClasspathEntry newContainerEntry(
			IPath containerPath,
			IAccessRule[] accessRules,
			IClasspathAttribute[] extraAttributes,
			boolean isExported) {

		if (containerPath == null) {
			throw new ClasspathEntry.AssertionFailedException("Container path cannot be null"); //$NON-NLS-1$
		} else if (containerPath.segmentCount() < 1) {
			throw new ClasspathEntry.AssertionFailedException("Illegal classpath container path: \'" + containerPath.makeRelative().toString() + "\', must have at least one segment (containerID+hints)"); //$NON-NLS-1$//$NON-NLS-2$
		}
		if (accessRules == null || accessRules.length == 0) {
			accessRules = ClasspathEntry.NO_ACCESS_RULES;
		}
		if (extraAttributes == null || extraAttributes.length == 0) {
			extraAttributes = ClasspathEntry.NO_EXTRA_ATTRIBUTES;
		}
		return new ClasspathEntry(
			IPackageFragmentRoot.K_SOURCE,
			IClasspathEntry.CPE_CONTAINER,
			containerPath,
			ClasspathEntry.INCLUDE_ALL, // inclusion patterns
			ClasspathEntry.EXCLUDE_NONE, // exclusion patterns
			null, // source attachment
			null, // source attachment root
			null, // specific output folder
			isExported,
			accessRules,
			true, // combine access rules
			extraAttributes);
	}

	/**
	 * Creates and returns a type hierarchy for all types in the given
	 * region, considering subtypes within that region and considering types in the
	 * working copies with the given owner.
	 * In other words, the owner's working copies will take
	 * precedence over their original compilation units in the workspace.
	 * <p>
	 * Note that if a working copy is empty, it will be as if the original compilation
	 * unit had been deleted.
	 * </p>
	 *
	 * @param monitor the given progress monitor
	 * @param region the given region
	 * @param owner the owner of working copies that take precedence over their original compilation units,
	 *   or <code>null</code> if the primary working copy owner should be used
	 * @exception JavaModelException if an element in the region does not exist or if an
	 *		exception occurs while accessing its corresponding resource
	 * @exception IllegalArgumentException if region is <code>null</code>
	 * @return a type hierarchy for all types in the given
	 * region, considering subtypes within that region
	 * @since 3.1
	 */
	public static ITypeHierarchy newTypeHierarchy(IRegion region, WorkingCopyOwner owner, IProgressMonitor monitor) throws JavaModelException {
		if (region == null) {
			throw new IllegalArgumentException(Messages.hierarchy_nullRegion);
		}
		ICompilationUnit[] workingCopies = JavaModelManager.getJavaModelManager().getWorkingCopies(owner, true/*add primary working copies*/);
		CreateTypeHierarchyOperation op =
			new CreateTypeHierarchyOperation(region, workingCopies, null, true/*compute subtypes*/);
		op.runOperation(monitor);
		return op.getResult();
	}

	/**
	 * Creates and returns a new non-exported classpath entry of kind <code>CPE_LIBRARY</code> for the
	 * JAR or folder identified by the given absolute path. This specifies that all package fragments
	 * within the root will have children of type <code>IClassFile</code>.
	 * This method is fully equivalent to calling
	 * {@link #newLibraryEntry(IPath, IPath, IPath, IAccessRule[], IClasspathAttribute[], boolean)
	 * newLibraryEntry(path, sourceAttachmentPath, sourceAttachmentRootPath, new IAccessRule[0], new IClasspathAttribute[0], false)}.
	 *
	 * @param path the path to the library
	 * @param sourceAttachmentPath the absolute path of the corresponding source archive or folder,
	 *    or <code>null</code> if none. Note, since 3.0, an empty path is allowed to denote no source attachment.
	 *    Since 3.4, this path can also denote a path external to the workspace.
	 *   and will be automatically converted to <code>null</code>.
	 * @param sourceAttachmentRootPath the location of the root of the source files within the source archive or folder
	 *    or <code>null</code> if this location should be automatically detected.
	 * @return a new library classpath entry
	 */
	public static IClasspathEntry newLibraryEntry(
		IPath path,
		IPath sourceAttachmentPath,
		IPath sourceAttachmentRootPath) {

		return newLibraryEntry(
			path,
			sourceAttachmentPath,
			sourceAttachmentRootPath,
			ClasspathEntry.NO_ACCESS_RULES,
			ClasspathEntry.NO_EXTRA_ATTRIBUTES,
			false/*not exported*/);
	}

	/**
	 * Creates and returns a new classpath entry of kind <code>CPE_LIBRARY</code> for the JAR or folder
	 * identified by the given absolute path. This specifies that all package fragments within the root
	 * will have children of type <code>IClassFile</code>.
	 * <p>
	 * This method is fully equivalent to calling
	 * {@link #newLibraryEntry(IPath, IPath, IPath, IAccessRule[], IClasspathAttribute[], boolean)
	 * newLibraryEntry(path, sourceAttachmentPath, sourceAttachmentRootPath, new IAccessRule[0], new IClasspathAttribute[0], isExported)}.
	 * </p>
	 *
	 * @param path the path to the library
	 * @param sourceAttachmentPath the absolute path of the corresponding source archive or folder,
	 *    or <code>null</code> if none. Note, since 3.0, an empty path is allowed to denote no source attachment.
	 *   and will be automatically converted to <code>null</code>. Since 3.4, this path can also denote a path external
	 *   to the workspace.
	 * @param sourceAttachmentRootPath the location of the root of the source files within the source archive or folder
	 *    or <code>null</code> if this location should be automatically detected.
	 * @param isExported indicates whether this entry is contributed to dependent
	 * 	  projects in addition to the output location
	 * @return a new library classpath entry
	 * @since 2.0
	 */
	public static IClasspathEntry newLibraryEntry(
		IPath path,
		IPath sourceAttachmentPath,
		IPath sourceAttachmentRootPath,
		boolean isExported) {

		return newLibraryEntry(
			path,
			sourceAttachmentPath,
			sourceAttachmentRootPath,
			ClasspathEntry.NO_ACCESS_RULES,
			ClasspathEntry.NO_EXTRA_ATTRIBUTES,
			isExported);
	}

	/**
	 * Creates and returns a new classpath entry of kind <code>CPE_LIBRARY</code> for the JAR or folder
	 * identified by the given absolute path. This specifies that all package fragments within the root
	 * will have children of type <code>IClassFile</code>.
	 * <p>
	 * A library entry is used to denote a prerequisite JAR or root folder containing binaries.
	 * The target JAR can either be defined internally to the workspace (absolute path relative
	 * to the workspace root), or externally to the workspace (absolute path in the file system).
	 * The target root folder can also be defined internally to the workspace (absolute path relative
	 * to the workspace root), or - since 3.4 - externally to the workspace (absolute path in the file system).
	 * Since 3.5, the path to the library can also be relative to the project using ".." as the first segment.
	 * </p>
	 * <p>
	 * e.g. Here are some examples of binary path usage
	 * </p>
	 *	<ul>
	 *	<li><code> "c:\jdk1.2.2\jre\lib\rt.jar" </code> - reference to an external JAR on Windows</li>
	 *	<li><code> "/Project/someLib.jar" </code> - reference to an internal JAR on Windows or Linux</li>
	 *	<li><code> "/Project/classes/" </code> - reference to an internal binary folder on Windows or Linux</li>
	 *	<li><code> "/home/usr/classes" </code> - reference to an external binary folder on Linux</li>
	 *	<li><code> "../../lib/someLib.jar" </code> - reference to an external JAR that is a sibling of the workspace on either platform</li>
	 * </ul>
	 * Note that on non-Windows platform, a path <code>"/some/lib.jar"</code> is ambiguous.
	 * It can be a path to an external JAR (its file system path being <code>"/some/lib.jar"</code>)
	 * or it can be a path to an internal JAR (<code>"some"</code> being a project in the workspace).
	 * Such an ambiguity is solved when the classpath entry is used (e.g. in {@link IJavaProject#getPackageFragmentRoots()}).
	 * If the resource <code>"lib.jar"</code> exists in project <code>"some"</code>, then it is considered an
	 * internal JAR. Otherwise it is an external JAR.
	 * <p>Also note that this operation does not attempt to validate or access the
	 * resources at the given paths.
	 * </p><p>
	 * The access rules determine the set of accessible class files
	 * in the library. If the list of access rules is empty then all files
	 * in this library are accessible.
	 * See {@link IAccessRule} for a detailed description of access
	 * rules.
	 * </p>
	 * <p>
	 * The <code>extraAttributes</code> list contains name/value pairs that must be persisted with
	 * this entry. If no extra attributes are provided, an empty array must be passed in.<br>
	 * Note that this list should not contain any duplicate name.
	 * </p>
	 * <p>
	 * The <code>isExported</code> flag indicates whether this entry is contributed to dependent
	 * projects. If not exported, dependent projects will not see any of the classes from this entry.
	 * If exported, dependent projects will concatenate the accessible files patterns of this entry with the
	 * accessible files patterns of the projects, and they will concatenate the non accessible files patterns of this entry
	 * with the non accessible files patterns of the project.
	 * </p>
	 * <p>
	 * Since 3.5, if the library is a ZIP archive, the "Class-Path" clause (if any) in the "META-INF/MANIFEST.MF" is read
	 * and referenced ZIP archives are added to the {@link IJavaProject#getResolvedClasspath(boolean) resolved classpath}.
	 * </p>
	 *
	 * @param path the path to the library
	 * @param sourceAttachmentPath the absolute path of the corresponding source archive or folder,
	 *    or <code>null</code> if none. Note, since 3.0, an empty path is allowed to denote no source attachment.
	 *   and will be automatically converted to <code>null</code>. Since 3.4, this path can also denote a path external
	 *   to the workspace.
	 * @param sourceAttachmentRootPath the location of the root of the source files within the source archive or folder
	 *    or <code>null</code> if this location should be automatically detected.
	 * @param accessRules the possibly empty list of access rules for this entry
	 * @param extraAttributes the possibly empty list of extra attributes to persist with this entry
	 * @param isExported indicates whether this entry is contributed to dependent
	 * 	  projects in addition to the output location
	 * @return a new library classpath entry
	 * @since 3.1
	 */
	public static IClasspathEntry newLibraryEntry(
			IPath path,
			IPath sourceAttachmentPath,
			IPath sourceAttachmentRootPath,
			IAccessRule[] accessRules,
			IClasspathAttribute[] extraAttributes,
			boolean isExported) {

		if (path == null) throw new ClasspathEntry.AssertionFailedException("Library path cannot be null"); //$NON-NLS-1$
		if (accessRules == null || accessRules.length==0) {
			accessRules = ClasspathEntry.NO_ACCESS_RULES;
		}
		if (extraAttributes == null || extraAttributes.length==0) {
			extraAttributes = ClasspathEntry.NO_EXTRA_ATTRIBUTES;
		}
		boolean hasDotDot = ClasspathEntry.hasDotDot(path);
		if (!hasDotDot && !path.isAbsolute()) throw new ClasspathEntry.AssertionFailedException("Path for IClasspathEntry must be absolute: " + path); //$NON-NLS-1$
		if (sourceAttachmentPath != null) {
			if (sourceAttachmentPath.isEmpty()) {
				sourceAttachmentPath = null; // treat empty path as none
			} else if (!sourceAttachmentPath.isAbsolute()) {
				throw new ClasspathEntry.AssertionFailedException("Source attachment path '" //$NON-NLS-1$
						+ sourceAttachmentPath
						+ "' for IClasspathEntry must be absolute"); //$NON-NLS-1$
			}
		}
		return new ClasspathEntry(
			IPackageFragmentRoot.K_BINARY,
			IClasspathEntry.CPE_LIBRARY,
			hasDotDot ? path : JavaProject.createPackageFragementKey(path),
			ClasspathEntry.INCLUDE_ALL, // inclusion patterns
			ClasspathEntry.EXCLUDE_NONE, // exclusion patterns
			sourceAttachmentPath,
			sourceAttachmentRootPath,
			null, // specific output folder
			isExported,
			accessRules,
			false, // no access rules to combine
			extraAttributes);
	}

	/**
	 * Creates and returns a new non-exported classpath entry of kind <code>CPE_PROJECT</code>
	 * for the project identified by the given absolute path.
	 * <p>
	 * This method is fully equivalent to calling
	 * {@link #newProjectEntry(IPath, IAccessRule[], boolean, IClasspathAttribute[], boolean)
	 * newProjectEntry(path, new IAccessRule[0], true, new IClasspathAttribute[0], false)}.
	 * </p>
	 *
	 * @param path the absolute path of the binary archive
	 * @return a new project classpath entry
	 */
	public static IClasspathEntry newProjectEntry(IPath path) {
		return newProjectEntry(path, false);
	}

	/**
	 * Creates and returns a new classpath entry of kind <code>CPE_PROJECT</code>
	 * for the project identified by the given absolute path.
	 * <p>
	 * This method is fully equivalent to calling
	 * {@link #newProjectEntry(IPath, IAccessRule[], boolean, IClasspathAttribute[], boolean)
	 * newProjectEntry(path, new IAccessRule[0], true, new IClasspathAttribute[0], isExported)}.
	 * </p>
	 *
	 * @param path the absolute path of the prerequisite project
	 * @param isExported indicates whether this entry is contributed to dependent
	 * 	  projects in addition to the output location
	 * @return a new project classpath entry
	 * @since 2.0
	 */
	public static IClasspathEntry newProjectEntry(IPath path, boolean isExported) {

		if (!path.isAbsolute()) throw new ClasspathEntry.AssertionFailedException("Path for IClasspathEntry must be absolute"); //$NON-NLS-1$

		return newProjectEntry(
			path,
			ClasspathEntry.NO_ACCESS_RULES,
			true,
			ClasspathEntry.NO_EXTRA_ATTRIBUTES,
			isExported);
	}

	/**
	 * Creates and returns a new classpath entry of kind <code>CPE_PROJECT</code>
	 * for the project identified by the given absolute path.
	 * <p>
	 * A project entry is used to denote a prerequisite project on a classpath.
	 * The referenced project will be contributed as a whole, either as sources (in the Java Model, it
	 * contributes all its package fragment roots) or as binaries (when building, it contributes its
	 * whole output location).
	 * </p>
	 * <p>
	 * A project reference allows to indirect through another project, independently from its internal layout.
	 * </p><p>
	 * The prerequisite project is referred to using an absolute path relative to the workspace root.
	 * </p>
	 * <p>
	 * The access rules determine the set of accessible class files
	 * in the project. If the list of access rules is empty then all files
	 * in this project are accessible.
	 * See {@link IAccessRule} for a detailed description of access rules.
	 * </p>
	 * <p>
	 * The <code>combineAccessRules</code> flag indicates whether access rules of one (or more)
	 * exported entry of the project should be combined with the given access rules. If they should
	 * be combined, the given access rules are considered first, then the entry's access rules are
	 * considered.
	 * </p>
	 * <p>
	 * The <code>extraAttributes</code> list contains name/value pairs that must be persisted with
	 * this entry. If no extra attributes are provided, an empty array must be passed in.<br>
	 * Note that this list should not contain any duplicate name.
	 * </p>
	 * <p>
	 * The <code>isExported</code> flag indicates whether this entry is contributed to dependent
	 * projects. If not exported, dependent projects will not see any of the classes from this entry.
	 * If exported, dependent projects will concatenate the accessible files patterns of this entry with the
	 * accessible files patterns of the projects, and they will concatenate the non accessible files patterns of this entry
	 * with the non accessible files patterns of the project.
	 * </p>
	 *
	 * @param path the absolute path of the prerequisite project
	 * @param accessRules the possibly empty list of access rules for this entry
	 * @param combineAccessRules whether the access rules of the project's exported entries should be combined with the given access rules
	 * @param extraAttributes the possibly empty list of extra attributes to persist with this entry
	 * @param isExported indicates whether this entry is contributed to dependent
	 * 	  projects in addition to the output location
	 * @return a new project classpath entry
	 * @since 3.1
	 */
	public static IClasspathEntry newProjectEntry(
			IPath path,
			IAccessRule[] accessRules,
			boolean combineAccessRules,
			IClasspathAttribute[] extraAttributes,
			boolean isExported) {

		if (!path.isAbsolute()) throw new ClasspathEntry.AssertionFailedException("Path for IClasspathEntry must be absolute"); //$NON-NLS-1$
		if (accessRules == null || accessRules.length == 0) {
			accessRules = ClasspathEntry.NO_ACCESS_RULES;
		}
		if (extraAttributes == null || extraAttributes.length == 0) {
			extraAttributes = ClasspathEntry.NO_EXTRA_ATTRIBUTES;
		}
		return new ClasspathEntry(
			IPackageFragmentRoot.K_SOURCE,
			IClasspathEntry.CPE_PROJECT,
			path,
			ClasspathEntry.INCLUDE_ALL, // inclusion patterns
			ClasspathEntry.EXCLUDE_NONE, // exclusion patterns
			null, // source attachment
			null, // source attachment root
			null, // specific output folder
			isExported,
			accessRules,
			combineAccessRules,
			extraAttributes);
	}

	/**
	 * Returns a new empty region.
	 *
	 * @return a new empty region
	 */
	public static IRegion newRegion() {
		return new Region();
	}

	/**
	 * Creates and returns a new classpath entry of kind <code>CPE_SOURCE</code>
	 * for all files in the project's source folder identified by the given
	 * absolute workspace-relative path.
	 * <p>
	 * The convenience method is fully equivalent to:
	 * </p>
	 * <pre>
	 * newSourceEntry(path, new IPath[] {}, new IPath[] {}, null);
	 * </pre>
	 *
	 * @param path the absolute workspace-relative path of a source folder
	 * @return a new source classpath entry
	 * @see #newSourceEntry(IPath, IPath[], IPath[], IPath)
	 */
	public static IClasspathEntry newSourceEntry(IPath path) {

		return newSourceEntry(path, ClasspathEntry.INCLUDE_ALL, ClasspathEntry.EXCLUDE_NONE, null /*output location*/);
	}

	/**
	 * Creates and returns a new classpath entry of kind <code>CPE_SOURCE</code>
	 * for the project's source folder identified by the given absolute
	 * workspace-relative path but excluding all source files with paths
	 * matching any of the given patterns.
	 * <p>
	 * The convenience method is fully equivalent to:
	 * </p>
	 * <pre>
	 * newSourceEntry(path, new IPath[] {}, exclusionPatterns, null);
	 * </pre>
	 *
	 * @param path the absolute workspace-relative path of a source folder
	 * @param exclusionPatterns the possibly empty list of exclusion patterns
	 *    represented as relative paths
	 * @return a new source classpath entry
	 * @see #newSourceEntry(IPath, IPath[], IPath[], IPath)
	 * @since 2.1
	 */
	public static IClasspathEntry newSourceEntry(IPath path, IPath[] exclusionPatterns) {

		return newSourceEntry(path, ClasspathEntry.INCLUDE_ALL, exclusionPatterns, null /*output location*/);
	}

	/**
	 * Creates and returns a new classpath entry of kind <code>CPE_SOURCE</code>
	 * for the project's source folder identified by the given absolute
	 * workspace-relative path but excluding all source files with paths
	 * matching any of the given patterns, and associated with a specific output location
	 * (that is, ".class" files are not going to the project default output location).
	 * <p>
	 * The convenience method is fully equivalent to:
	 * </p>
	 * <pre>
	 * newSourceEntry(path, new IPath[] {}, exclusionPatterns, specificOutputLocation);
	 * </pre>
	 *
	 * @param path the absolute workspace-relative path of a source folder
	 * @param exclusionPatterns the possibly empty list of exclusion patterns
	 *    represented as relative paths
	 * @param specificOutputLocation the specific output location for this source entry (<code>null</code> if using project default output location)
	 * @return a new source classpath entry
	 * @see #newSourceEntry(IPath, IPath[], IPath[], IPath)
	 * @since 2.1
	 */
	public static IClasspathEntry newSourceEntry(IPath path, IPath[] exclusionPatterns, IPath specificOutputLocation) {

	    return newSourceEntry(path, ClasspathEntry.INCLUDE_ALL, exclusionPatterns, specificOutputLocation);
	}

	/**
	 * Creates and returns a new classpath entry of kind <code>CPE_SOURCE</code>
	 * for the project's source folder identified by the given absolute
	 * workspace-relative path but excluding all source files with paths
	 * matching any of the given patterns, and associated with a specific output location
	 * (that is, ".class" files are not going to the project default output location).
	 * <p>
	 * The convenience method is fully equivalent to:
	 * </p>
	 * <pre>
	 * newSourceEntry(path, new IPath[] {}, exclusionPatterns, specificOutputLocation, new IClasspathAttribute[] {});
	 * </pre>
	 *
	 * @param path the absolute workspace-relative path of a source folder
	 * @param inclusionPatterns the possibly empty list of inclusion patterns
	 *    represented as relative paths
	 * @param exclusionPatterns the possibly empty list of exclusion patterns
	 *    represented as relative paths
	 * @param specificOutputLocation the specific output location for this source entry (<code>null</code> if using project default output location)
	 * @return a new source classpath entry
	 * @see #newSourceEntry(IPath, IPath[], IPath[], IPath, IClasspathAttribute[])
	 * @since 3.0
	 */
	public static IClasspathEntry newSourceEntry(IPath path, IPath[] inclusionPatterns, IPath[] exclusionPatterns, IPath specificOutputLocation) {
		return newSourceEntry(path, inclusionPatterns, exclusionPatterns, specificOutputLocation, ClasspathEntry.NO_EXTRA_ATTRIBUTES);
	}

	/**
	 * Creates and returns a new classpath entry of kind <code>CPE_SOURCE</code>
	 * for the project's source folder identified by the given absolute
	 * workspace-relative path using the given inclusion and exclusion patterns
	 * to determine which source files are included, and the given output path
	 * to control the output location of generated files.
	 * <p>
	 * The source folder is referred to using an absolute path relative to the
	 * workspace root, e.g. <code>/Project/src</code>. A project's source
	 * folders are located with that project. That is, a source classpath
	 * entry specifying the path <code>/P1/src</code> is only usable for
	 * project <code>P1</code>.
	 * </p>
	 * <p>
	 * The inclusion patterns determines the initial set of source files that
	 * are to be included; the exclusion patterns are then used to reduce this
	 * set. When no inclusion patterns are specified, the initial file set
	 * includes all relevant files in the resource tree rooted at the source
	 * entry's path. On the other hand, specifying one or more inclusion
	 * patterns means that all <b>and only</b> files matching at least one of
	 * the specified patterns are to be included. If exclusion patterns are
	 * specified, the initial set of files is then reduced by eliminating files
	 * matched by at least one of the exclusion patterns. Inclusion and
	 * exclusion patterns look like relative file paths with wildcards and are
	 * interpreted relative to the source entry's path. File patterns are
	 * case-sensitive can contain '**', '*' or '?' wildcards (see
	 * {@link IClasspathEntry#getExclusionPatterns()} for the full description
	 * of their syntax and semantics). The resulting set of files are included
	 * in the corresponding package fragment root; all package fragments within
	 * the root will have children of type <code>ICompilationUnit</code>.
	 * </p>
	 * <p>
	 * For example, if the source folder path is
	 * <code>/Project/src</code>, there are no inclusion filters, and the
	 * exclusion pattern is
	 * <code>com/xyz/tests/&#42;&#42;</code>, then source files
	 * like <code>/Project/src/com/xyz/Foo.java</code>
	 * and <code>/Project/src/com/xyz/utils/Bar.java</code> would be included,
	 * whereas <code>/Project/src/com/xyz/tests/T1.java</code>
	 * and <code>/Project/src/com/xyz/tests/quick/T2.java</code> would be
	 * excluded.
	 * </p>
	 * <p>
	 * Additionally, a source entry can be associated with a specific output location.
	 * By doing so, the Java builder will ensure that the generated ".class" files will
	 * be issued inside this output location, as opposed to be generated into the
	 * project default output location (when output location is <code>null</code>).
	 * Note that multiple source entries may target the same output location.
	 * The output location is referred to using an absolute path relative to the
	 * workspace root, e.g. <code>"/Project/bin"</code>, it must be located inside
	 * the same project as the source folder.
	 * </p>
	 * <p>
	 * Also note that all sources/binaries inside a project are contributed as
	 * a whole through a project entry
	 * (see <code>JavaCore.newProjectEntry</code>). Particular source entries
	 * cannot be selectively exported.
	 * </p>
	 * <p>
	 * The <code>extraAttributes</code> list contains name/value pairs that must be persisted with
	 * this entry. If no extra attributes are provided, an empty array must be passed in.<br>
	 * Note that this list should not contain any duplicate name.
	 * </p>
	 *
	 * @param path the absolute workspace-relative path of a source folder
	 * @param inclusionPatterns the possibly empty list of inclusion patterns
	 *    represented as relative paths
	 * @param exclusionPatterns the possibly empty list of exclusion patterns
	 *    represented as relative paths
	 * @param specificOutputLocation the specific output location for this source entry (<code>null</code> if using project default ouput location)
	 * @param extraAttributes the possibly empty list of extra attributes to persist with this entry
	 * @return a new source classpath entry with the given exclusion patterns
	 * @see IClasspathEntry#getInclusionPatterns()
	 * @see IClasspathEntry#getExclusionPatterns()
	 * @see IClasspathEntry#getOutputLocation()
	 * @since 3.1
	 */
	public static IClasspathEntry newSourceEntry(IPath path, IPath[] inclusionPatterns, IPath[] exclusionPatterns, IPath specificOutputLocation, IClasspathAttribute[] extraAttributes) {

		if (path == null) throw new ClasspathEntry.AssertionFailedException("Source path cannot be null"); //$NON-NLS-1$
		if (!path.isAbsolute()) throw new ClasspathEntry.AssertionFailedException("Path for IClasspathEntry must be absolute"); //$NON-NLS-1$
		if (exclusionPatterns == null) {
			exclusionPatterns = ClasspathEntry.EXCLUDE_NONE;
		}
		if (inclusionPatterns == null) {
			inclusionPatterns = ClasspathEntry.INCLUDE_ALL;
		}
		if (extraAttributes == null) {
			extraAttributes = ClasspathEntry.NO_EXTRA_ATTRIBUTES;
		}
		return new ClasspathEntry(
			IPackageFragmentRoot.K_SOURCE,
			IClasspathEntry.CPE_SOURCE,
			path,
			inclusionPatterns,
			exclusionPatterns,
			null, // source attachment
			null, // source attachment root
			specificOutputLocation, // custom output location
			false,
			null,
			false, // no access rules to combine
			extraAttributes);
	}

	/**
	 * Creates and returns a new non-exported classpath entry of kind <code>CPE_VARIABLE</code>
	 * for the given path. This method is fully equivalent to calling
	 * {@link #newVariableEntry(IPath, IPath, IPath, IAccessRule[], IClasspathAttribute[], boolean)
	 * newVariableEntry(variablePath, variableSourceAttachmentPath, sourceAttachmentRootPath, new IAccessRule[0], new IClasspathAttribute[0], false)}.
	 *
	 * @param variablePath the path of the binary archive; first segment is the
	 *   name of a classpath variable
	 * @param variableSourceAttachmentPath the path of the corresponding source archive,
	 *    or <code>null</code> if none; if present, the first segment is the
	 *    name of a classpath variable (not necessarily the same variable
	 *    as the one that begins <code>variablePath</code>)
	 * @param sourceAttachmentRootPath the location of the root of the source files within the source archive
	 *    or <code>null</code> if <code>variableSourceAttachmentPath</code> is also <code>null</code>
	 * @return a new library classpath entry
	 */
	public static IClasspathEntry newVariableEntry(
		IPath variablePath,
		IPath variableSourceAttachmentPath,
		IPath sourceAttachmentRootPath) {

		return newVariableEntry(variablePath, variableSourceAttachmentPath, sourceAttachmentRootPath, false);
	}

	/**
	 * Creates and returns a new classpath entry of kind <code>CPE_VARIABLE</code>
	 * for the given path. This method is fully equivalent to calling
	 * {@link #newVariableEntry(IPath, IPath, IPath, IAccessRule[], IClasspathAttribute[], boolean)
	 * newVariableEntry(variablePath, variableSourceAttachmentPath, sourceAttachmentRootPath, new IAccessRule[0], new IClasspathAttribute[0], isExported)}.
	 *
	 * @param variablePath the path of the binary archive; first segment is the
	 *   name of a classpath variable
	 * @param variableSourceAttachmentPath the path of the corresponding source archive,
	 *    or <code>null</code> if none; if present, the first segment is the
	 *    name of a classpath variable (not necessarily the same variable
	 *    as the one that begins <code>variablePath</code>)
	 * @param variableSourceAttachmentRootPath the location of the root of the source files within the source archive
	 *    or <code>null</code> if <code>variableSourceAttachmentPath</code> is also <code>null</code>
	 * @param isExported indicates whether this entry is contributed to dependent
	 * 	  projects in addition to the output location
	 * @return a new variable classpath entry
	 * @since 2.0
	 */
	public static IClasspathEntry newVariableEntry(
			IPath variablePath,
			IPath variableSourceAttachmentPath,
			IPath variableSourceAttachmentRootPath,
			boolean isExported) {

		return newVariableEntry(
			variablePath,
			variableSourceAttachmentPath,
			variableSourceAttachmentRootPath,
			ClasspathEntry.NO_ACCESS_RULES,
			ClasspathEntry.NO_EXTRA_ATTRIBUTES,
			isExported);
	}

	/**
	 * Creates and returns a new classpath entry of kind <code>CPE_VARIABLE</code>
	 * for the given path. The first segment of the path is the name of a classpath variable.
	 * The trailing segments of the path will be appended to resolved variable path.
	 * <p>
	 * A variable entry allows to express indirect references on a classpath to other projects or libraries,
	 * depending on what the classpath variable is referring.
	 * </p>
	 * <p>
	 * It is possible to register an automatic initializer (<code>ClasspathVariableInitializer</code>),
	 * which will be invoked through the extension point "org.eclipse.jdt.core.classpathVariableInitializer".
	 * After resolution, a classpath variable entry may either correspond to a project or a library entry.
	 * </p>
	 * <p>
	 * e.g. Here are some examples of variable path usage
	 * </p>
	 * <ul>
	 * <li> "JDTCORE" where variable <code>JDTCORE</code> is
	 *		bound to "c:/jars/jdtcore.jar". The resolved classpath entry is denoting the library "c:\jars\jdtcore.jar"</li>
	 * <li> "JDTCORE" where variable <code>JDTCORE</code> is
	 *		bound to "/Project_JDTCORE". The resolved classpath entry is denoting the project "/Project_JDTCORE"</li>
	 * <li> "PLUGINS/com.example/example.jar" where variable <code>PLUGINS</code>
	 *      is bound to "c:/eclipse/plugins". The resolved classpath entry is denoting the library "c:\eclipse\plugins\com.example\example.jar"</li>
	 * </ul>
	 * <p>
	 * The access rules determine the set of accessible class files
	 * in the project or library. If the list of access rules is empty then all files
	 * in this project or library are accessible.
	 * See {@link IAccessRule} for a detailed description of access rules.
	 * </p>
	 * <p>
	 * The <code>extraAttributes</code> list contains name/value pairs that must be persisted with
	 * this entry. If no extra attributes are provided, an empty array must be passed in.<br>
	 * Note that this list should not contain any duplicate name.
	 * </p>
	 * <p>
	 * The <code>isExported</code> flag indicates whether this entry is contributed to dependent
	 * projects. If not exported, dependent projects will not see any of the classes from this entry.
	 * If exported, dependent projects will concatenate the accessible files patterns of this entry with the
	 * accessible files patterns of the projects, and they will concatenate the non accessible files patterns of this entry
	 * with the non accessible files patterns of the project.
	 * </p>
	 * <p>
	 * Note that this operation does not attempt to validate classpath variables
	 * or access the resources at the given paths.
	 * </p>
	 *
	 * @param variablePath the path of the binary archive; first segment is the
	 *   name of a classpath variable
	 * @param variableSourceAttachmentPath the path of the corresponding source archive,
	 *    or <code>null</code> if none; if present, the first segment is the
	 *    name of a classpath variable (not necessarily the same variable
	 *    as the one that begins <code>variablePath</code>)
	 * @param variableSourceAttachmentRootPath the location of the root of the source files within the source archive
	 *    or <code>null</code> if <code>variableSourceAttachmentPath</code> is also <code>null</code>
	 * @param accessRules the possibly empty list of access rules for this entry
	 * @param extraAttributes the possibly empty list of extra attributes to persist with this entry
	 * @param isExported indicates whether this entry is contributed to dependent
	 * 	  projects in addition to the output location
	 * @return a new variable classpath entry
	 * @since 3.1
	 */
	public static IClasspathEntry newVariableEntry(
			IPath variablePath,
			IPath variableSourceAttachmentPath,
			IPath variableSourceAttachmentRootPath,
			IAccessRule[] accessRules,
			IClasspathAttribute[] extraAttributes,
			boolean isExported) {

		if (variablePath == null) throw new ClasspathEntry.AssertionFailedException("Variable path cannot be null"); //$NON-NLS-1$
		if (variablePath.segmentCount() < 1) {
			throw new ClasspathEntry.AssertionFailedException("Illegal classpath variable path: \'" + variablePath.makeRelative().toString() + "\', must have at least one segment"); //$NON-NLS-1$//$NON-NLS-2$
		}
		if (accessRules == null || accessRules.length == 0) {
			accessRules = ClasspathEntry.NO_ACCESS_RULES;
		}
		if (extraAttributes == null || extraAttributes.length == 0) {
			extraAttributes = ClasspathEntry.NO_EXTRA_ATTRIBUTES;
		}

		return new ClasspathEntry(
			IPackageFragmentRoot.K_SOURCE,
			IClasspathEntry.CPE_VARIABLE,
			variablePath,
			ClasspathEntry.INCLUDE_ALL, // inclusion patterns
			ClasspathEntry.EXCLUDE_NONE, // exclusion patterns
			variableSourceAttachmentPath, // source attachment
			variableSourceAttachmentRootPath, // source attachment root
			null, // specific output folder
			isExported,
			accessRules,
			false, // no access rules to combine
			extraAttributes);
	}

	/**
	 * Returns an array of classpath entries that are referenced directly or indirectly
	 * by a given classpath entry. For the entry kind {@link IClasspathEntry#CPE_LIBRARY},
	 * the method returns the libraries that are included in the Class-Path section of
	 * the MANIFEST.MF file. If a referenced JAR file has further references to other library
	 * entries, they are processed recursively and added to the list. For entry kinds other
	 * than {@link IClasspathEntry#CPE_LIBRARY}, this method returns an empty array.
	 * <p>
	 * When a non-null project is passed, any additional attributes that may have been stored
	 * previously in the project's .classpath file are retrieved and populated in the
	 * corresponding referenced entry. If the project is <code>null</code>, the raw referenced
	 * entries are returned without any persisted attributes.
	 * For more details on storing referenced entries, see
	 * {@link IJavaProject#setRawClasspath(IClasspathEntry[], IClasspathEntry[], IPath,
	 * IProgressMonitor)}.
	 * </p>
	 *
	 * @param libraryEntry the library entry whose referenced entries are sought
	 * @param project project where the persisted referenced entries to be retrieved from. If <code>null</code>
	 * 			persisted attributes are not attempted to be retrieved.
	 * @return an array of classpath entries that are referenced directly or indirectly by the given entry.
	 * 			If not applicable, returns an empty array.
	 * @since 3.6
	 */
	public static IClasspathEntry[] getReferencedClasspathEntries(IClasspathEntry libraryEntry, IJavaProject project) {
		JavaModelManager manager = JavaModelManager.getJavaModelManager();
		return manager.getReferencedClasspathEntries(libraryEntry, project);
	}

	/**
	 * Removed the given classpath variable. Does nothing if no value was
	 * set for this classpath variable.
	 * <p>
	 * This functionality cannot be used while the resource tree is locked.
	 * </p>
	 * <p>
	 * Classpath variable values are persisted locally to the workspace, and
	 * are preserved from session to session.
	 * </p>
	 *
	 * @param variableName the name of the classpath variable
	 * @see #setClasspathVariable(String, IPath)
	 *
	 * @deprecated Use {@link #removeClasspathVariable(String, IProgressMonitor)} instead
	 */
	public static void removeClasspathVariable(String variableName) {
		removeClasspathVariable(variableName, null);
	}

	/**
	 * Removed the given classpath variable. Does nothing if no value was
	 * set for this classpath variable.
	 * <p>
	 * This functionality cannot be used while the resource tree is locked.
	 * </p>
	 * <p>
	 * Classpath variable values are persisted locally to the workspace, and
	 * are preserved from session to session.
	 * </p>
	 *
	 * @param variableName the name of the classpath variable
	 * @param monitor the progress monitor to report progress
	 * @see #setClasspathVariable(String, IPath)
	 */
	public static void removeClasspathVariable(String variableName, IProgressMonitor monitor) {
		try {
			SetVariablesOperation operation = new SetVariablesOperation(new String[]{ variableName}, new IPath[]{ null }, true/*update preferences*/);
			operation.runOperation(monitor);
		} catch (JavaModelException e) {
			Util.log(e, "Exception while removing variable " + variableName); //$NON-NLS-1$
		}
	}

	/**
	 * Removes the given element changed listener.
	 * Has no effect if an identical listener is not registered.
	 *
	 * @param listener the listener
	 */
	public static void removeElementChangedListener(IElementChangedListener listener) {
		JavaModelManager.getDeltaState().removeElementChangedListener(listener);
	}

	/**
	 * Removes the file extension from the given file name, if it has a Java-like file
	 * extension. Otherwise the file name itself is returned.
	 * Note this removes the dot ('.') before the extension as well.
	 *
	 * @param fileName the name of a file
	 * @return the fileName without the Java-like extension
	 * @since 3.2
	 */
	public static String removeJavaLikeExtension(String fileName) {
		return Util.getNameWithoutJavaLikeExtension(fileName);
	}

	/**
	 * Removes the given pre-processing resource changed listener.
	 * <p>
	 * Has no effect if an identical listener is not registered.
	 * </p>
	 *
	 * @param listener the listener
	 * @since 3.0
	 */
	public static void removePreProcessingResourceChangedListener(IResourceChangeListener listener) {
		JavaModelManager.getDeltaState().removePreResourceChangedListener(listener);
	}

	/**
	 * Deletes the index, then rebuilds any portions of the index that are
	 * currently needed by the workspace.
	 *
	 * @param monitor a progress monitor, or <code>null</code> if progress
	 *    reporting and cancellation are not desired
	 * @throws CoreException
	 * @since 3.13
	 */
	public static void rebuildIndex(IProgressMonitor monitor) throws CoreException {
		SubMonitor subMonitor = SubMonitor.convert(monitor, 100);
		IndexManager manager = JavaModelManager.getIndexManager();
		manager.deleteIndexFiles(subMonitor.split(1));
		manager.reset();
		updateLegacyIndex(subMonitor.split(4));
	}

	/**
	 * Runs the given action as an atomic Java model operation.
	 * <p>
	 * After running a method that modifies java elements,
	 * registered listeners receive after-the-fact notification of
	 * what just transpired, in the form of a element changed event.
	 * This method allows clients to call a number of
	 * methods that modify java elements and only have element
	 * changed event notifications reported at the end of the entire
	 * batch.
	 * </p>
	 * <p>
	 * If this method is called outside the dynamic scope of another such
	 * call, this method runs the action and then reports a single
	 * element changed event describing the net effect of all changes
	 * done to java elements by the action.
	 * </p>
	 * <p>
	 * If this method is called in the dynamic scope of another such
	 * call, this method simply runs the action.
	 * </p>
	 *
	 * @param action the action to perform
	 * @param monitor a progress monitor, or <code>null</code> if progress
	 *    reporting and cancellation are not desired
	 * @exception CoreException if the operation failed.
	 * @since 2.1
	 */
	public static void run(IWorkspaceRunnable action, IProgressMonitor monitor) throws CoreException {
		run(action, ResourcesPlugin.getWorkspace().getRoot(), monitor);
	}
	/**
	 * Runs the given action as an atomic Java model operation.
	 * <p>
	 * After running a method that modifies java elements,
	 * registered listeners receive after-the-fact notification of
	 * what just transpired, in the form of a element changed event.
	 * This method allows clients to call a number of
	 * methods that modify java elements and only have element
	 * changed event notifications reported at the end of the entire
	 * batch.
	 * </p>
	 * <p>
	 * If this method is called outside the dynamic scope of another such
	 * call, this method runs the action and then reports a single
	 * element changed event describing the net effect of all changes
	 * done to java elements by the action.
	 * </p>
	 * <p>
	 * If this method is called in the dynamic scope of another such
	 * call, this method simply runs the action.
	 * </p>
	 * <p>
 	 * The supplied scheduling rule is used to determine whether this operation can be
	 * run simultaneously with workspace changes in other threads. See
	 * <code>IWorkspace.run(...)</code> for more details.
 	 * </p>
	 *
	 * @param action the action to perform
	 * @param rule the scheduling rule to use when running this operation, or
	 * <code>null</code> if there are no scheduling restrictions for this operation.
	 * @param monitor a progress monitor, or <code>null</code> if progress
	 *    reporting and cancellation are not desired
	 * @exception CoreException if the operation failed.
	 * @since 3.0
	 */
	public static void run(IWorkspaceRunnable action, ISchedulingRule rule, IProgressMonitor monitor) throws CoreException {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		if (workspace.isTreeLocked()) {
			new BatchOperation(action).run(monitor);
		} else {
			// use IWorkspace.run(...) to ensure that a build will be done in autobuild mode
			workspace.run(new BatchOperation(action), rule, IWorkspace.AVOID_UPDATE, monitor);
		}
	}
	/**
	 * Bind a container reference path to some actual containers (<code>IClasspathContainer</code>).
	 * This API must be invoked whenever changes in container need to be reflected onto the JavaModel.
	 * Containers can have distinct values in different projects, therefore this API considers a
	 * set of projects with their respective containers.
	 * <p>
	 * <code>containerPath</code> is the path under which these values can be referenced through
	 * container classpath entries (<code>IClasspathEntry#CPE_CONTAINER</code>). A container path
	 * is formed by a first ID segment followed with extra segments, which can be used as additional hints
	 * for the resolution. The container ID is used to identify a <code>ClasspathContainerInitializer</code>
	 * registered on the extension point "org.eclipse.jdt.core.classpathContainerInitializer".
	 * </p>
	 * <p>
	 * There is no assumption that each individual container value passed in argument
	 * (<code>respectiveContainers</code>) must answer the exact same path when requested
	 * <code>IClasspathContainer#getPath</code>.
	 * Indeed, the containerPath is just an indication for resolving it to an actual container object. It can be
	 * delegated to a <code>ClasspathContainerInitializer</code>, which can be activated through the extension
	 * point "org.eclipse.jdt.core.ClasspathContainerInitializer").
	 * </p>
	 * <p>
	 * In reaction to changing container values, the JavaModel will be updated to reflect the new
	 * state of the updated container. A combined Java element delta will be notified to describe the corresponding
	 * classpath changes resulting from the container update. This operation is batched, and automatically eliminates
	 * unnecessary updates (new container is same as old one). This operation acquires a lock on the workspace's root.
	 * </p>
	 * <p>
	 * This functionality cannot be used while the workspace is locked, since
	 * it may create/remove some resource markers.
	 * </p>
	 * <p>
	 * Classpath container values are persisted locally to the workspace, but
	 * are not preserved from a session to another. It is thus highly recommended to register a
	 * <code>ClasspathContainerInitializer</code> for each referenced container
	 * (through the extension point "org.eclipse.jdt.core.ClasspathContainerInitializer").
	 * </p>
	 * <p>
	 * Note: setting a container to <code>null</code> will cause it to be lazily resolved again whenever
	 * its value is required. In particular, this will cause a registered initializer to be invoked
	 * again.
	 * </p>
	 * @param containerPath - the name of the container reference, which is being updated
	 * @param affectedProjects - the set of projects for which this container is being bound
	 * @param respectiveContainers - the set of respective containers for the affected projects
	 * @param monitor a monitor to report progress
	 * @throws JavaModelException
	 * @see ClasspathContainerInitializer
	 * @see #getClasspathContainer(IPath, IJavaProject)
	 * @see IClasspathContainer
	 * @since 2.0
	 */
	public static void setClasspathContainer(IPath containerPath, IJavaProject[] affectedProjects, IClasspathContainer[] respectiveContainers, IProgressMonitor monitor) throws JavaModelException {
		if (affectedProjects.length != respectiveContainers.length)
			throw new ClasspathEntry.AssertionFailedException("Projects and containers collections should have the same size"); //$NON-NLS-1$
		if (affectedProjects.length == 1) {
			IClasspathContainer container = respectiveContainers[0];
			if (container != null) {
				JavaModelManager manager = JavaModelManager.getJavaModelManager();
				IJavaProject project = affectedProjects[0];
				IClasspathContainer existingCointainer = manager.containerGet(project, containerPath);
				if (existingCointainer == JavaModelManager.CONTAINER_INITIALIZATION_IN_PROGRESS) {
					manager.containerBeingInitializedPut(project, containerPath, container);
					return;
				}
			}
		}
		SetContainerOperation operation = new SetContainerOperation(containerPath, affectedProjects, respectiveContainers);
		operation.runOperation(monitor);
	}

	/**
	 * Sets the value of the given classpath variable.
	 * The path must have at least one segment.
	 * <p>
	 * This functionality cannot be used while the resource tree is locked.
	 * </p>
	 * <p>
	 * Classpath variable values are persisted locally to the workspace, and
	 * are preserved from session to session.
	 * </p>
	 *
	 * @param variableName the name of the classpath variable
	 * @param path the path
	 * @throws JavaModelException
	 * @see #getClasspathVariable(String)
	 *
	 * @deprecated Use {@link #setClasspathVariable(String, IPath, IProgressMonitor)} instead
	 */
	public static void setClasspathVariable(String variableName, IPath path)
		throws JavaModelException {

		setClasspathVariable(variableName, path, null);
	}

	/**
	 * Sets the value of the given classpath variable.
	 * The path must not be null.
	 * Since 3.5, the path to a library can also be relative to the project using ".." as the first segment.
	 * <p>
	 * This functionality cannot be used while the resource tree is locked.
	 * </p>
	 * <p>
	 * Classpath variable values are persisted locally to the workspace, and
	 * are preserved from session to session.
	 * </p>
	 * <p>
	 * Updating a variable with the same value has no effect.
	 * </p>
	 *
	 * @param variableName the name of the classpath variable
	 * @param path the path
	 * @param monitor a monitor to report progress
	 * @throws JavaModelException
	 * @see #getClasspathVariable(String)
	 */
	public static void setClasspathVariable(
		String variableName,
		IPath path,
		IProgressMonitor monitor)
		throws JavaModelException {

		if (path == null) throw new ClasspathEntry.AssertionFailedException("Variable path cannot be null"); //$NON-NLS-1$
		setClasspathVariables(new String[]{variableName}, new IPath[]{ path }, monitor);
	}

	/**
	 * Sets the values of all the given classpath variables at once.
	 * Null paths can be used to request corresponding variable removal.
	 * Since 3.5, the path to a library can also be relative to the project using ".." as the first segment.
	 * <p>
	 * A combined Java element delta will be notified to describe the corresponding
	 * classpath changes resulting from the variables update. This operation is batched,
	 * and automatically eliminates unnecessary updates (new variable is same as old one).
	 * This operation acquires a lock on the workspace's root.
	 * </p>
	 * <p>
	 * This functionality cannot be used while the workspace is locked, since
	 * it may create/remove some resource markers.
	 * </p>
	 * <p>
	 * Classpath variable values are persisted locally to the workspace, and
	 * are preserved from session to session.
	 * </p>
	 * <p>
	 * Updating a variable with the same value has no effect.
	 * </p>
	 *
	 * @param variableNames an array of names for the updated classpath variables
	 * @param paths an array of path updates for the modified classpath variables (null
	 *       meaning that the corresponding value will be removed
	 * @param monitor a monitor to report progress
	 * @throws JavaModelException
	 * @see #getClasspathVariable(String)
	 * @since 2.0
	 */
	public static void setClasspathVariables(
		String[] variableNames,
		IPath[] paths,
		IProgressMonitor monitor)
		throws JavaModelException {

		if (variableNames.length != paths.length)	throw new ClasspathEntry.AssertionFailedException("Variable names and paths collections should have the same size"); //$NON-NLS-1$
		SetVariablesOperation operation = new SetVariablesOperation(variableNames, paths, true/*update preferences*/);
		operation.runOperation(monitor);
	}

	/**
	 * Sets the default compiler options inside the given options map according
	 * to the given compliance.
	 *
	 * <p>The given compliance must be one of those supported by the compiler,
	 * that is one of the acceptable values for option {@link #COMPILER_COMPLIANCE}.</p>
	 *
	 * <p>The list of modified options is currently:</p>
	 * <ul>
	 * <li>{@link #COMPILER_COMPLIANCE}</li>
	 * <li>{@link #COMPILER_SOURCE}</li>
	 * <li>{@link #COMPILER_CODEGEN_TARGET_PLATFORM}</li>
	 * <li>{@link #COMPILER_PB_ASSERT_IDENTIFIER}</li>
	 * <li>{@link #COMPILER_PB_ENUM_IDENTIFIER}</li>
	 * <li>{@link #COMPILER_CODEGEN_INLINE_JSR_BYTECODE} for compliance levels 1.5 and greater</li>
	 * <li>{@link #COMPILER_PB_ENABLE_PREVIEW_FEATURES} for compliance levels 11 and greater</li>
	 * <li>{@link #COMPILER_PB_REPORT_PREVIEW_FEATURES} for compliance levels 11 and greater</li>
	 * </ul>
	 *
	 * <p>If the given compliance is unknown, the given map is unmodified.</p>
	 *
	 * @param compliance the given {@link #COMPILER_COMPLIANCE compliance}
	 * @param options the given options map
	 * @since 3.3
	 */
	public static void setComplianceOptions(String compliance, Map options) {
		long jdkLevel = CompilerOptions.versionToJdkLevel(compliance);
		int major = (int) (jdkLevel >>> 16);
		switch(major) {
			case ClassFileConstants.MAJOR_VERSION_1_3:
				options.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_3);
				options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_3);
				options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_1);
				options.put(JavaCore.COMPILER_PB_ASSERT_IDENTIFIER, JavaCore.IGNORE);
				options.put(JavaCore.COMPILER_PB_ENUM_IDENTIFIER, JavaCore.IGNORE);
				break;
			case ClassFileConstants.MAJOR_VERSION_1_4:
				options.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_4);
				options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_3);
				options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_2);
				options.put(JavaCore.COMPILER_PB_ASSERT_IDENTIFIER, JavaCore.WARNING);
				options.put(JavaCore.COMPILER_PB_ENUM_IDENTIFIER, JavaCore.WARNING);
				break;
			case ClassFileConstants.MAJOR_VERSION_1_5:
				options.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_5);
				options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_5);
				options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_5);
				options.put(JavaCore.COMPILER_PB_ASSERT_IDENTIFIER, JavaCore.ERROR);
				options.put(JavaCore.COMPILER_PB_ENUM_IDENTIFIER, JavaCore.ERROR);
				options.put(JavaCore.COMPILER_CODEGEN_INLINE_JSR_BYTECODE, JavaCore.ENABLED);
				break;
			case ClassFileConstants.MAJOR_VERSION_1_6:
				options.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_6);
				options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_6);
				options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_6);
				options.put(JavaCore.COMPILER_PB_ASSERT_IDENTIFIER, JavaCore.ERROR);
				options.put(JavaCore.COMPILER_PB_ENUM_IDENTIFIER, JavaCore.ERROR);
				options.put(JavaCore.COMPILER_CODEGEN_INLINE_JSR_BYTECODE, JavaCore.ENABLED);
				break;
			case ClassFileConstants.MAJOR_VERSION_1_7:
				options.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_7);
				options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_7);
				options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_7);
				options.put(JavaCore.COMPILER_PB_ASSERT_IDENTIFIER, JavaCore.ERROR);
				options.put(JavaCore.COMPILER_PB_ENUM_IDENTIFIER, JavaCore.ERROR);
				options.put(JavaCore.COMPILER_CODEGEN_INLINE_JSR_BYTECODE, JavaCore.ENABLED);
				break;
			case ClassFileConstants.MAJOR_VERSION_1_8:
				options.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_8);
				options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_8);
				options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_8);
				options.put(JavaCore.COMPILER_PB_ASSERT_IDENTIFIER, JavaCore.ERROR);
				options.put(JavaCore.COMPILER_PB_ENUM_IDENTIFIER, JavaCore.ERROR);
				options.put(JavaCore.COMPILER_CODEGEN_INLINE_JSR_BYTECODE, JavaCore.ENABLED);
				break;
			case ClassFileConstants.MAJOR_VERSION_9:
				options.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_9);
				options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_9);
				options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_9);
				options.put(JavaCore.COMPILER_PB_ASSERT_IDENTIFIER, JavaCore.ERROR);
				options.put(JavaCore.COMPILER_PB_ENUM_IDENTIFIER, JavaCore.ERROR);
				options.put(JavaCore.COMPILER_CODEGEN_INLINE_JSR_BYTECODE, JavaCore.ENABLED);
				options.put(JavaCore.COMPILER_RELEASE, JavaCore.ENABLED);
				break;
			case ClassFileConstants.MAJOR_VERSION_10:
				options.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_10);
				options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_10);
				options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_10);
				options.put(JavaCore.COMPILER_PB_ASSERT_IDENTIFIER, JavaCore.ERROR);
				options.put(JavaCore.COMPILER_PB_ENUM_IDENTIFIER, JavaCore.ERROR);
				options.put(JavaCore.COMPILER_CODEGEN_INLINE_JSR_BYTECODE, JavaCore.ENABLED);
				options.put(JavaCore.COMPILER_RELEASE, JavaCore.ENABLED);
				break;
			default:
				if(major > ClassFileConstants.MAJOR_VERSION_10) {
					String version = CompilerOptions.versionFromJdkLevel(jdkLevel);
					options.put(JavaCore.COMPILER_COMPLIANCE, version);
					options.put(JavaCore.COMPILER_SOURCE, version);
					options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, version);
					options.put(JavaCore.COMPILER_PB_ASSERT_IDENTIFIER, JavaCore.ERROR);
					options.put(JavaCore.COMPILER_PB_ENUM_IDENTIFIER, JavaCore.ERROR);
					options.put(JavaCore.COMPILER_CODEGEN_INLINE_JSR_BYTECODE, JavaCore.ENABLED);
					options.put(JavaCore.COMPILER_RELEASE, JavaCore.ENABLED);
					options.put(JavaCore.COMPILER_PB_ENABLE_PREVIEW_FEATURES, JavaCore.DISABLED);
					options.put(JavaCore.COMPILER_PB_REPORT_PREVIEW_FEATURES, JavaCore.WARNING);
				}
				break;
		}
	}

	/**
	 * Sets the current table of options. All and only the options explicitly
	 * included in the given table are remembered; all previous option settings
	 * are forgotten, including ones not explicitly mentioned.
	 * <p>
	 * Helper constants have been defined on JavaCore for each of the option IDs
	 * (categorized in Code assist option ID, Compiler option ID and Core option ID)
	 * and some of their acceptable values (categorized in Option value). Some
	 * options accept open value sets beyond the documented constant values.
	 * </p>
	 * Note: each release may add new options.
	 *
	 * @param newOptions
	 *            the new options (key type: <code>String</code>; value type:
	 *            <code>String</code>), or <code>null</code> to reset all
	 *            options to their default values
	 * @see JavaCore#getDefaultOptions()
	 * @see JavaCorePreferenceInitializer for changing default settings
	 */
	public static void setOptions(Hashtable<String, String> newOptions) {
		JavaModelManager.getJavaModelManager().setOptions(newOptions);
	}

	/**
	 * Returns the latest version of Java supported by the Java Model. This is usually the last entry
	 * from {@link JavaCore#getAllVersions()}.
	 *
	 * @since 3.16
	 * @return the latest Java version support by Java Model
	 */
	public static String latestSupportedJavaVersion() {
		return allVersions.get(allVersions.size() - 1);
	}
	/**
	 * Compares two given versions of the Java platform. The versions being compared must both be
	 * one of the supported values mentioned in
	 * {@link #COMPILER_CODEGEN_TARGET_PLATFORM COMPILER_CODEGEN_TARGET_PLATFORM},
	 * both values from {@link #COMPILER_COMPLIANCE},  or both values from {@link #COMPILER_SOURCE}.
	 *
	 * @param first first version to be compared
	 * @param second second version to be compared
	 * @return the value {@code 0} if both versions are the same;
	 * 			a value less than {@code 0} if <code>first</code> is smaller than <code>second</code>; and
	 * 			a value greater than {@code 0} if <code>first</code> is higher than <code>second</code>
	 * @since 3.12
	 */
	public static int compareJavaVersions(String first, String second) {
		return Long.compare(CompilerOptions.versionToJdkLevel(first), CompilerOptions.versionToJdkLevel(second));
	}
	/**
	 * Returns an array of module names referenced by this project indirectly.
	 * This is a helper method that can be used to construct a Java module
	 * description of an existing project. The referenced modules can either be
	 * system modules or user modules found in project build path in the form of
	 * libraries.
	 * The prerequisites for this to be effective are:
	 * <ul>
	 * <li>the project is already in compliance level 9 or above.
	 * <li>the system library on the build path of the project is a modularized Java Runtime.
	 * </ul>
	 *
	 * @param project
	 *            the project whose referenced modules to be computed
	 * @return an array of String containing module names
	 * @throws CoreException
	 * @since 3.14
	 */
	public static String[] getReferencedModules(IJavaProject project) throws CoreException {
		return ModuleUtil.getReferencedModules(project);
	}

	/**
	 * Returns the <code>IModuleDescription</code> that the given java element contains
	 * when regarded as an automatic module. The element must be an <code>IPackageFragmentRoot</code>
	 * or an <code>IJavaProject</code>.
	 *
	 * <p>The returned module descriptor has a name (<code>getElementName()</code>) following
	 * the specification of <code>java.lang.module.ModuleFinder.of(Path...)</code>, but it
	 * contains no other useful information.</p>
	 *
	 * @return the <code>IModuleDescription</code> representing this java element as an automatic module,
	 * 		never <code>null</code>.
	 * @throws JavaModelException
	 * @throws IllegalArgumentException if the provided element is neither <code>IPackageFragmentRoot</code>
	 * 	nor <code>IJavaProject</code>
	 * @since 3.14
	 */
	public static IModuleDescription getAutomaticModuleDescription(IJavaElement element) throws JavaModelException, IllegalArgumentException {
		switch (element.getElementType()) {
			case IJavaElement.JAVA_PROJECT:
				return ((JavaProject) element).getAutomaticModuleDescription();
			case IJavaElement.PACKAGE_FRAGMENT_ROOT:
				return ((PackageFragmentRoot) element).getAutomaticModuleDescription();
			default:
				throw new IllegalArgumentException("Illegal kind of java element: "+element.getElementType()); //$NON-NLS-1$
		}
	}

	/**
	 * Filter the given set of system roots by the rules for root modules from JEP 261.
	 * @param allSystemRoots all physically available system modules, represented by their package fragment roots
	 * @return the list of names of default root modules
	 * @since 3.14
	 */
	public static List<String> defaultRootModules(Iterable<IPackageFragmentRoot> allSystemRoots) {
		return JavaProject.defaultRootModules(allSystemRoots);
	}

	/**
	 * Compile the given module description in the context of its enclosing Java project
	 * and add class file attributes using the given map of attribute values.
	 * <p>In this map, the following keys are supported</p>
	 * <dl>
	 * <dt>{@link IAttributeNamesConstants#MODULE_MAIN_CLASS}</dt>
	 * <dd>The associated value will be used for the <code>ModuleMainClass</code> attribute.</dd>
	 * <dt>{@link IAttributeNamesConstants#MODULE_PACKAGES}</dt>
	 * <dd>If the associated value is an empty string, then the compiler will generate a
	 * <code>ModulePackages</code> attribute with a list of packages that is computed from
	 * <ul>
	 * <li>all <code>exports</code> directives
	 * <li>all <code>opens</code> directives
	 * <li>the implementation classes of all <code>provides</code> directives.
	 * </ul>
	 * If the associated value is not empty, it must be a comma-separated list of package names,
	 * which will be added to the computed list.
	 * </dl>
	 * <p>No other keys are supported in this version, but more keys may be added in the future.</p>
	 *
	 * @param module handle for the <code>module-info.java</code> file to be compiled.
	 * @param classFileAttributes map of attribute names and values to be used during class file generation
	 * @return the compiled byte code
	 *
	 * @throws JavaModelException
	 * @throws IllegalArgumentException if the map of classFileAttributes contains an unsupported key.
	 * @since 3.14
	 */
	public static byte[] compileWithAttributes(IModuleDescription module, Map<String,String> classFileAttributes)
			throws JavaModelException, IllegalArgumentException
	{
		return new ModuleInfoBuilder().compileWithAttributes(module, classFileAttributes);
	}

	/**
	 * Returns the module name computed for a jar. If the file is a jar and contains a module-info.class, the name
	 * specified in it is used, otherwise, the algorithm for automatic module naming is used, which first looks for a
	 * module name in the Manifest.MF and as last resort computes it from the file name.
	 *
	 * @param file the jar to examine
	 * @return null if file is not a file, otherwise the module name.
	 * @since 3.14
	 */
	public static String getModuleNameFromJar(File file) {
		if (!file.isFile()) {
			return null;
		}

		char[] moduleName = null;
		try (ZipFile zipFile = new ZipFile(file)) {
			IModule module = null;
			ClassFileReader reader = ClassFileReader.read(zipFile, IModule.MODULE_INFO_CLASS);
			if (reader != null) {
				module = reader.getModuleDeclaration();
				if (module != null) {
					moduleName = module.name();
				}
			}
		} catch (ClassFormatException | IOException ex) {
			Util.log(ex);
		}
		if (moduleName == null) {
			moduleName = AutomaticModuleNaming.determineAutomaticModuleName(file.getAbsolutePath());
		}
		return new String(moduleName);
	}

	/**
	 * Returns the names of the modules required by the module-info.class in the jar. If the file is not jar or a jar
	 * that has no module-info.class is present, the empty set is returned.
	 *
	 * @param file the jar to examine
	 * @return set of module names.
	 * @since 3.14
	 */
	public static Set<String> getRequiredModulesFromJar(File file) {
		if (!file.isFile()) {
			return Collections.emptySet();
		}
		try (ZipFile zipFile = new ZipFile(file)) {
			IModule module = null;
			ClassFileReader reader = ClassFileReader.read(zipFile, IModule.MODULE_INFO_CLASS);
			if (reader != null) {
				module = reader.getModuleDeclaration();
				if (module != null) {
					IModuleReference[] moduleRefs = module.requires();
					if (moduleRefs != null) {
						return Stream.of(moduleRefs).map(m -> new String(m.name()))
								.collect(Collectors.toCollection(LinkedHashSet::new));
					}
				}
			}
		} catch (ClassFormatException | IOException ex) {
			Util.log(ex);
		}
		return Collections.emptySet();
	}


	/* (non-Javadoc)
	 * Shutdown the JavaCore plug-in.
	 * <p>
	 * De-registers the JavaModelManager as a resource changed listener and save participant.
	 * </p>
	 * @see org.eclipse.core.runtime.Plugin#stop(BundleContext)
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		try {
			JavaModelManager.unregisterDebugOptionsListener();
			JavaModelManager.getJavaModelManager().shutdown();
		} finally {
			// ensure we call super.stop as the last thing
			super.stop(context);
		}
	}

	/* (non-Javadoc)
	 * Startup the JavaCore plug-in.
	 * <p>
	 * Registers the JavaModelManager as a resource changed listener and save participant.
	 * Starts the background indexing, and restore saved classpath variable values.
	 * </p>
	 * @throws Exception
	 * @see org.eclipse.core.runtime.Plugin#start(BundleContext)
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		JavaModelManager.registerDebugOptionsListener(context);
		JavaModelManager.getJavaModelManager().startup();
		// New index is disabled, see bug 544898
		// Indexer.getInstance().rescanAll();
	}
}
