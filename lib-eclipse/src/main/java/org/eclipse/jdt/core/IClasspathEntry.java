/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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

import org.eclipse.core.runtime.IPath;

/**
 * An entry on a Java project classpath identifying one or more package fragment
 * roots. A classpath entry has a content kind (either source,
 * {@link IPackageFragmentRoot#K_SOURCE}, or binary, {@link IPackageFragmentRoot#K_BINARY}), which is inherited
 * by each package fragment root and package fragment associated with the entry.
 * <p>
 * A classpath entry can refer to any of the following:<ul>
 *	<li>Source code in the current project. In this case, the entry identifies a
 *		root folder in the current project containing package fragments and
 *		source files with one of the {@link JavaCore#getJavaLikeExtensions()
 *		Java-like extensions}. The root folder itself represents a default
 *		package, subfolders represent package fragments, and files with a
 *     Java-like extension (e.g. <code>.java</code> files)
 *		represent compilation units. All compilation units will be compiled when
 * 		the project is built. The classpath entry must specify the
 *		absolute path to the root folder. Entries of this kind are
 *		associated with the {@link #CPE_SOURCE} constant.
 *      Source classpath entries can carry inclusion and exclusion patterns for
 *      selecting which source files appear as compilation
 *      units and get compiled when the project is built.
 *  </li>
 *
 *	<li>A binary library in the current project, in another project, or in the external
 *		file system. In this case the entry identifies a JAR (or root folder) containing
 *		package fragments and <code>.class</code> files.  The classpath entry
 *		must specify the absolute path to the JAR (or root folder), and in case it refers
 *		to an external JAR, then there is no associated resource in the workbench. Entries
 *		of this kind are associated with the {@link #CPE_LIBRARY} constant.</li>
 *
 *	<li>A required project. In this case the entry identifies another project in
 *		the workspace. The required project is used as a binary library when compiling
 *		(that is, the builder looks in the output location of the required project
 *		for required <code>.class</code> files when building). When performing other
 *		"development" operations - such as code assist, code resolve, type hierarchy
 *		creation, etc. - the source code of the project is referred to. Thus, development
 *		is performed against a required project's source code, and compilation is
 *		performed against a required project's last built state.  The
 *		classpath entry must specify the absolute path to the
 *		project. Entries of this kind are  associated with the {@link #CPE_PROJECT}
 *		constant.
 * 		Note: referencing a required project with a classpath entry refers to the source
 *     code or associated <code>.class</code> files located in its output location.
 *     It will also automatically include any other libraries or projects that the required project's classpath
 *     refers to, iff the corresponding classpath entries are tagged as being exported
 *     ({@link IClasspathEntry#isExported}).
 *    Unless exporting some classpath entries, classpaths are not chained by default -
 *    each project must specify its own classpath in its entirety.</li>
 *
 *  <li> A path beginning in a classpath variable defined globally to the workspace.
 *		Entries of this kind are  associated with the {@link #CPE_VARIABLE} constant.
 *      Classpath variables are created using {@link JavaCore#setClasspathVariable(String, IPath, org.eclipse.core.runtime.IProgressMonitor)},
 * 		and gets resolved, to either a project or library entry, using
 *      {@link JavaCore#getResolvedClasspathEntry(IClasspathEntry)}.
 *		It is also possible to register an automatic initializer ({@link ClasspathVariableInitializer}),
 * 	which will be invoked through the extension point "org.eclipse.jdt.core.classpathVariableInitializer".
 * 	After resolution, a classpath variable entry may either correspond to a project or a library entry. </li>
 *
 *  <li> A named classpath container identified by its container path.
 *     A classpath container provides a way to indirectly reference a set of classpath entries through
 *     a classpath entry of kind {@link #CPE_CONTAINER}. Typically, a classpath container can
 *     be used to describe a complex library composed of multiple JARs, projects or classpath variables,
 *     considering also that containers can be mapped differently on each project. Several projects can
 *     reference the same generic container path, but have each of them actually bound to a different
 *     container object.
 *     The container path is a formed by a first ID segment followed with extra segments,
 *     which can be used as additional hints for resolving this container reference. If no container was ever
 *     recorded for this container path onto this project (using {@link JavaCore#setClasspathContainer},
 * 	then a {@link ClasspathContainerInitializer} will be activated if any was registered for this
 * 	container ID onto the extension point "org.eclipse.jdt.core.classpathContainerInitializer".
 * 	A classpath container entry can be resolved explicitly using {@link JavaCore#getClasspathContainer}
 * 	and the resulting container entries can contain any non-container entry. In particular, it may contain variable
 *     entries, which in turn needs to be resolved before being directly used.
 * 	<br> Also note that the container resolution APIs include an IJavaProject argument, so as to allow the same
 * 	container path to be interpreted in different ways for different projects. </li>
 * </ul>
 * The result of {@link IJavaProject#getResolvedClasspath} will have all entries of type
 * {@link #CPE_VARIABLE} and {@link #CPE_CONTAINER} resolved to a set of
 * {@link #CPE_SOURCE}, {@link #CPE_LIBRARY} or {@link #CPE_PROJECT}
 * classpath entries.
 * <p>
 * Any classpath entry other than a source folder (kind {@link #CPE_SOURCE}) can
 * be marked as being exported. Exported entries are automatically contributed to
 * dependent projects, along with the project's default output folder, which is
 * implicitly exported, and any auxiliary output folders specified on source
 * classpath entries. The project's output folder(s) are always listed first,
 * followed by the any exported entries.
 * <p>
 * Classpath entries can be created via methods on {@link JavaCore}.
 * </p>
 *
 * @see JavaCore#newLibraryEntry(org.eclipse.core.runtime.IPath, org.eclipse.core.runtime.IPath, org.eclipse.core.runtime.IPath)
 * @see JavaCore#newProjectEntry(org.eclipse.core.runtime.IPath)
 * @see JavaCore#newSourceEntry(org.eclipse.core.runtime.IPath)
 * @see JavaCore#newVariableEntry(org.eclipse.core.runtime.IPath, org.eclipse.core.runtime.IPath, org.eclipse.core.runtime.IPath)
 * @see JavaCore#newContainerEntry(org.eclipse.core.runtime.IPath)
 * @see ClasspathVariableInitializer
 * @see ClasspathContainerInitializer
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IClasspathEntry {

	/**
	 * Entry kind constant describing a classpath entry identifying a
	 * library. A library is a folder or JAR containing package
	 * fragments consisting of pre-compiled binaries.
	 */
	int CPE_LIBRARY = 1;

	/**
	 * Entry kind constant describing a classpath entry identifying a
	 * required project.
	 */
	int CPE_PROJECT = 2;

	/**
	 * Entry kind constant describing a classpath entry identifying a
	 * folder containing package fragments with source code
	 * to be compiled.
	 */
	int CPE_SOURCE = 3;

	/**
	 * Entry kind constant describing a classpath entry defined using
	 * a path that begins with a classpath variable reference.
	 */
	int CPE_VARIABLE = 4;

	/**
	 * Entry kind constant describing a classpath entry representing
	 * a name classpath container.
	 *
	 * @since 2.0
	 */
	int CPE_CONTAINER = 5;

	/**
	 * Returns whether the access rules of the project's exported entries should be combined with this entry's access rules.
	 * Returns true for container entries.
	 * Returns false otherwise.
	 *
	 * @return whether the access rules of the project's exported entries should be combined with this entry's access rules
	 * @since 3.1
	 */
	boolean combineAccessRules();

	/**
	 * Returns the possibly empty list of access rules for this entry.
	 *
	 * @return the possibly empty list of access rules for this entry
	 * @since 3.1
	 */
	IAccessRule[] getAccessRules();
	/**
	 * Returns the kind of files found in the package fragments identified by this
	 * classpath entry.
	 *
	 * @return {@link IPackageFragmentRoot#K_SOURCE} for files containing
	 *   source code, and {@link IPackageFragmentRoot#K_BINARY} for binary
	 *   class files.
	 *   There is no specified value for an entry denoting a variable ({@link #CPE_VARIABLE})
	 *   or a classpath container ({@link #CPE_CONTAINER}).
	 */
	int getContentKind();

	/**
	 * Returns the kind of this classpath entry.
	 *
	 * @return one of:
	 * <ul>
	 * <li>{@link #CPE_SOURCE} - this entry describes a source root in
	 		its project
	 * <li>{@link #CPE_LIBRARY} - this entry describes a folder or JAR
	 		containing binaries
	 * <li>{@link #CPE_PROJECT} - this entry describes another project
	 *
	 * <li>{@link #CPE_VARIABLE} - this entry describes a project or library
	 *  	indirectly via a classpath variable in the first segment of the path
	 * *
	 * <li>{@link #CPE_CONTAINER} - this entry describes set of entries
	 *  	referenced indirectly via a classpath container
	 * </ul>
	 */
	int getEntryKind();

	/**
	 * Returns the set of patterns used to exclude resources or classes associated with
	 * this classpath entry.
	 * <p>
	 * For source classpath entries,
	 * exclusion patterns allow specified portions of the resource tree rooted
	 * at this source entry's path to be filtered out. If no exclusion patterns
	 * are specified, this source entry includes all relevent files. Each path
	 * specified must be a relative path, and will be interpreted relative
	 * to this source entry's path. File patterns are case-sensitive. A file
	 * matched by one or more of these patterns is excluded from the
	 * corresponding package fragment root.
	 * Exclusion patterns have higher precedence than inclusion patterns;
	 * in other words, exclusion patterns can remove files for the ones that
	 * are to be included, not the other way around.
	 * </p>
	 * <p>
	 * Note that there is no need to supply a pattern to exclude ".class" files
	 * because a source entry filters these out automatically.
	 * </p>
	 * <p>
	 * The pattern mechanism is similar to Ant's. Each pattern is represented as
	 * a relative path. The path segments can be regular file or folder names or simple patterns
	 * involving standard wildcard characters.
	 * </p>
	 * <p>
	 * '*' matches 0 or more characters within a segment. So
	 * <code>*.java</code> matches <code>.java</code>, <code>a.java</code>
	 * and <code>Foo.java</code>, but not <code>Foo.properties</code>
	 * (does not end with <code>.java</code>).
	 * </p>
	 * <p>
	 * '?' matches 1 character within a segment. So <code>?.java</code>
	 * matches <code>a.java</code>, <code>A.java</code>,
	 * but not <code>.java</code> or <code>xyz.java</code> (neither have
	 * just one character before <code>.java</code>).
	 * </p>
	 * <p>
	 * Combinations of *'s and ?'s are allowed.
	 * </p>
	 * <p>
	 * The special pattern '**' matches zero or more segments. In a source entry,
	 * a path like <code>tests/</code> that ends in a trailing separator is interpreted
	 * as <code>tests/&#42;&#42;</code>, and would match everything under
	 * the folder named <code>tests</code>.
	 * </p>
	 * <p>
	 * Example patterns in source entries (assuming that "java" is the only {@link JavaCore#getJavaLikeExtensions() Java-like extension}):
	 * <ul>
	 * <li>
	 * <code>tests/&#42;&#42;</code> (or simply <code>tests/</code>)
	 * matches all files under a root folder
	 * named <code>tests</code>. This includes <code>tests/Foo.java</code>
	 * and <code>tests/com/example/Foo.java</code>, but not
	 * <code>com/example/tests/Foo.java</code> (not under a root folder named
	 * <code>tests</code>).
	 * </li>
	 * <li>
	 * <code>tests/&#42;</code> matches all files directly below a root
	 * folder named <code>tests</code>. This includes <code>tests/Foo.java</code>
	 * and <code>tests/FooHelp.java</code>
	 * but not <code>tests/com/example/Foo.java</code> (not directly under
	 * a folder named <code>tests</code>) or
	 * <code>com/Foo.java</code> (not under a folder named <code>tests</code>).
	 * </li>
	 * <li>
	 * <code>&#42;&#42;/tests/&#42;&#42;</code> matches all files under any
	 * folder named <code>tests</code>. This includes <code>tests/Foo.java</code>,
	 * <code>com/examples/tests/Foo.java</code>, and
	 * <code>com/examples/tests/unit/Foo.java</code>, but not
	 * <code>com/example/Foo.java</code> (not under a folder named
	 * <code>tests</code>).
	 * </li>
	 * </ul>
	 *
	 * @return the possibly empty list of resource exclusion patterns
	 *   associated with this classpath entry, or <code>null</code> if this kind
	 *   of classpath entry does not support exclusion patterns
	 * @since 2.1
	 */
	IPath[] getExclusionPatterns();

	/**
	 * Returns the extra classpath attributes for this classpath entry. Returns an empty array if this entry
	 * has no extra attributes.
	 *
	 * @return the possibly empty list of extra classpath attributes for this classpath entry
	 * @since 3.1
	 */
	IClasspathAttribute[] getExtraAttributes();

	/**
	 * Returns the set of patterns used to explicitly define resources or classes
	 * to be included with this classpath entry.
	 * <p>
	 * For source classpath entries,
	 * when no inclusion patterns are specified, the source entry includes all
	 * relevent files in the resource tree rooted at this source entry's path.
	 * Specifying one or more inclusion patterns means that only the specified
	 * portions of the resource tree are to be included. Each path specified
	 * must be a relative path, and will be interpreted relative to this source
	 * entry's path. File patterns are case-sensitive. A file matched by one or
	 * more of these patterns is included in the corresponding package fragment
	 * root unless it is excluded by one or more of this entrie's exclusion
	 * patterns. Exclusion patterns have higher precedence than inclusion
	 * patterns; in other words, exclusion patterns can remove files for the
	 * ones that are to be included, not the other way around.
	 * </p>
	 * <p>
	 * See {@link #getExclusionPatterns()} for a discussion of the syntax and
	 * semantics of path patterns. The absence of any inclusion patterns is
	 * semantically equivalent to the explicit inclusion pattern
	 * <code>&#42;&#42;</code>.
	 * </p>
	 * <p>
	 * Example patterns in source entries:
	 * <ul>
	 * <li>
	 * The inclusion pattern <code>src/&#42;&#42;</code> by itself includes all
	 * files under a root folder named <code>src</code>.
	 * </li>
	 * <li>
	 * The inclusion patterns <code>src/&#42;&#42;</code> and
	 * <code>tests/&#42;&#42;</code> includes all files under the root folders
	 * named <code>src</code> and <code>tests</code>.
	 * </li>
	 * <li>
	 * The inclusion pattern <code>src/&#42;&#42;</code> together with the
	 * exclusion pattern <code>src/&#42;&#42;/Foo.java</code> includes all
	 * files under a root folder named <code>src</code> except for ones
	 * named <code>Foo.java</code>.
	 * </li>
	 * </ul>
	 *
	 * @return the possibly empty list of resource inclusion patterns
	 *   associated with this classpath entry, or <code>null</code> if this kind
	 *   of classpath entry does not support inclusion patterns
	 * @since 3.0
	 */
	IPath[] getInclusionPatterns();

	/**
	 * Returns the full path to the specific location where the builder writes
	 * <code>.class</code> files generated for this source entry
	 * (entry kind {@link #CPE_SOURCE}).
	 * <p>
	 * Source entries can optionally be associated with a specific output location.
	 * If none is provided, the source entry will be implicitly associated with its project
	 * default output location (see {@link IJavaProject#getOutputLocation}).
	 * </p><p>
	 * NOTE: A specific output location cannot coincidate with another source/library entry.
	 * </p>
	 *
	 * @return the full path to the specific location where the builder writes
	 * <code>.class</code> files for this source entry, or <code>null</code>
	 * if using default output folder
	 * @since 2.1
	 */
	IPath getOutputLocation();

	/**
	 * Returns the path of this classpath entry.
	 *
	 * The meaning of the path of a classpath entry depends on its entry kind:<ul>
	 *	<li>Source code in the current project ({@link #CPE_SOURCE}) -
	 *      The path associated with this entry is the absolute path to the root folder. </li>
	 *	<li>A binary library in the current project ({@link #CPE_LIBRARY}) - the path
	 *		associated with this entry is the absolute path to the JAR (or root folder), and
	 *		in case it refers to an external library, then there is no associated resource in
	 *		the workbench.
	 *	<li>A required project ({@link #CPE_PROJECT}) - the path of the entry denotes the
	 *		path to the corresponding project resource.</li>
	 *  <li>A variable entry ({@link #CPE_VARIABLE}) - the first segment of the path
	 *      is the name of a classpath variable. If this classpath variable
	 *		is bound to the path <i>P</i>, the path of the corresponding classpath entry
	 *		is computed by appending to <i>P</i> the segments of the returned
	 *		path without the variable.</li>
	 *  <li> A container entry ({@link #CPE_CONTAINER}) - the path of the entry
	 * 	is the name of the classpath container, which can be bound indirectly to a set of classpath
	 * 	entries after resolution. The containerPath is a formed by a first ID segment followed with
	 *     extra segments that can be used as additional hints for resolving this container
	 * 	reference (also see {@link IClasspathContainer}).
	 * </li>
	 * </ul>
	 *
	 * @return the path of this classpath entry
	 */
	IPath getPath();

	/**
	 * Returns the path to the source archive or folder associated with this
	 * classpath entry, or <code>null</code> if this classpath entry has no
	 * source attachment.
	 * <p>
	 * Only library and variable classpath entries may have source attachments.
	 * For library classpath entries, the result path (if present) locates a source
	 * archive or folder. This archive or folder can be located in a project of the
	 * workspace or outside the workspace. For variable classpath entries, the
	 * result path (if present) has an analogous form and meaning as the
	 * variable path, namely the first segment is the name of a classpath variable.
	 * </p>
	 *
	 * @return the path to the source archive or folder, or <code>null</code> if none
	 */
	IPath getSourceAttachmentPath();

	/**
	 * Returns the path within the source archive or folder where package fragments
	 * are located. An empty path indicates that packages are located at
	 * the root of the source archive or folder. Returns a non-<code>null</code> value
	 * if and only if {@link #getSourceAttachmentPath} returns
	 * a non-<code>null</code> value.
	 *
	 * @return the path within the source archive or folder, or <code>null</code> if
	 *    not applicable
	 */
	IPath getSourceAttachmentRootPath();


	/**
	 * Returns the classpath entry that is making a reference to this classpath entry. For entry kinds
	 * {@link #CPE_LIBRARY}, the return value is the entry that is representing the JAR that includes
	 * <code>this</code> in the MANIFEST.MF file's Class-Path section. For entry kinds other than
	 * {@link #CPE_LIBRARY}, this returns <code>null</code>. For those entries that are on the raw classpath already,
	 * this returns <code>null</code>.
	 * <p>
	 * It is possible that multiple library entries refer to the same entry
	 * via the MANIFEST.MF file. In those cases, this method returns the first classpath entry
	 * that appears in the raw classpath. However, this does not mean that the other referencing
	 * entries do not relate to their referenced entries.
	 * See {@link JavaCore#getReferencedClasspathEntries(IClasspathEntry, IJavaProject)} for
	 * more details.
	 * </p>
	 *
	 * @return the classpath entry that is referencing this entry or <code>null</code> if
	 * 		not applicable.
	 * @since 3.6
	 */
	IClasspathEntry getReferencingEntry();

	/**
	 * Returns whether this entry is exported to dependent projects.
	 * Always returns <code>false</code> for source entries (kind
	 * {@link #CPE_SOURCE}), which cannot be exported.
	 *
	 * @return <code>true</code> if exported, and <code>false</code> otherwise
	 * @since 2.0
	 */
	boolean isExported();

	/**
	 * This is a helper method, which returns the resolved classpath entry denoted
	 * by an entry (if it is a variable entry). It is obtained by resolving the variable
	 * reference in the first segment. Returns <code>null</code> if unable to resolve using
	 * the following algorithm:
	 * <ul>
	 * <li> if variable segment cannot be resolved, returns <code>null</code></li>
	 * <li> finds a project, JAR or binary folder in the workspace at the resolved path location</li>
	 * <li> if none finds an external JAR file or folder outside the workspace at the resolved path location </li>
	 * <li> if none returns <code>null</code></li>
	 * </ul>
	 * <p>
	 * Variable source attachment is also resolved and recorded in the resulting classpath entry.
	 * <p>
	 * @return the resolved library or project classpath entry, or <code>null</code>
	 *   if the given path could not be resolved to a classpath entry
	 *	<p>
	 * Note that this deprecated API doesn't handle CPE_CONTAINER entries.
	 *
	 * @deprecated Use {@link JavaCore#getResolvedClasspathEntry(IClasspathEntry)} instead
	 */
	IClasspathEntry getResolvedEntry();

	/**
	 * This is a convience method, that returns <code>true</code> if the extra attributes contain an attribute whose name
	 * is {@link IClasspathAttribute#TEST} and whose value is 'true'.
	 *
	 * @see #getExtraAttributes()
	 * @see IClasspathAttribute#TEST
	 * @return <code>true</code>, if if the extra attributes contain a attribute whose name is
	 *         {@link IClasspathAttribute#TEST} and whose value is 'true'.
	 * @since 3.14
	 */
	default public boolean isTest() {
		for (IClasspathAttribute attribute : getExtraAttributes()) {
			if (IClasspathAttribute.TEST.equals(attribute.getName()) && "true".equals(attribute.getValue())) //$NON-NLS-1$
				return true;
		}
		return false;
	}

	/**
	 * This is a convience method, that returns <code>true</code> if the extra attributes contain an attribute whose name
	 * is {@link IClasspathAttribute#WITHOUT_TEST_CODE} and whose value is 'true'.
	 *
	 * @see #getExtraAttributes()
	 * @see IClasspathAttribute#WITHOUT_TEST_CODE
	 * @return <code>true</code>, if if the extra attributes contain a attribute whose name is
	 *         {@link IClasspathAttribute#WITHOUT_TEST_CODE} and whose value is 'true'.
	 * @since 3.14
	 */
	default public boolean isWithoutTestCode() {
		for (IClasspathAttribute attribute : getExtraAttributes()) {
			if (IClasspathAttribute.WITHOUT_TEST_CODE.equals(attribute.getName()) && "true".equals(attribute.getValue())) //$NON-NLS-1$
				return true;
		}
		return false;
	}
}
