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
 *     IBM Corporation - added getOption(String, boolean), getOptions(boolean) and setOptions(Map)
 *     IBM Corporation - deprecated getPackageFragmentRoots(IClasspathEntry) and
 *                               added findPackageFragmentRoots(IClasspathEntry)
 *     IBM Corporation - added isOnClasspath(IResource)
 *     IBM Corporation - added setOption(String, String)
 *     IBM Corporation - added forceClasspathReload(IProgressMonitor)
 *******************************************************************************/
package org.eclipse.jdt.core;

import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IPath;

import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IPackageBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.eval.IEvaluationContext;

/**
 * A Java project represents a view of a project resource in terms of Java
 * elements such as package fragments, types, methods and fields.
 * A project may contain several package roots, which contain package fragments.
 * A package root corresponds to an underlying folder or JAR.
 * <p>
 * Each Java project has a classpath, defining which folders contain source code and
 * where required libraries are located. Each Java project also has an output location,
 * defining where the builder writes <code>.class</code> files. A project that
 * references packages in another project can access the packages by including
 * the required project in a classpath entry. The Java model will present the
 * source elements in the required project; when building, the compiler will use
 * the corresponding generated class files from the required project's output
 * location(s)). The classpath format is a sequence of classpath entries
 * describing the location and contents of package fragment roots.
 * <p>
 * Java project elements need to be opened before they can be navigated or manipulated.
 * The children of a Java project are the package fragment roots that are
 * defined by the classpath and contained in this project (in other words, it
 * does not include package fragment roots for other projects). The children
 * (i.e. the package fragment roots) appear in the order they are defined by
 * the classpath.
 * <p>
 * An instance of one of these handles can be created via
 * <code>JavaCore.create(project)</code>.
 * </p>
 *
 * @see JavaCore#create(org.eclipse.core.resources.IProject)
 * @see IClasspathEntry
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IJavaProject extends IParent, IJavaElement, IOpenable {

	/**
	 * Path of the file containing the project's classpath relative to the project's root.
	 *
	 * <p>The file is a child of the project folder.</p>
	 * <p>The format of this file is unspecified and it is not meant to be modified.
	 * Its contents is modified by using the <code>IJavaProject#setRawClasspath(..)</code> methods.</p>
	 *
	 * @see #setRawClasspath(IClasspathEntry[], IProgressMonitor)
	 * @see #setRawClasspath(IClasspathEntry[], boolean, IProgressMonitor)
	 * @see #setRawClasspath(IClasspathEntry[], IPath, IProgressMonitor)
	 * @see #setRawClasspath(IClasspathEntry[], IClasspathEntry[], IPath, IProgressMonitor)
	 * @see #setRawClasspath(IClasspathEntry[], IPath, boolean, IProgressMonitor)
	 * @since 3.7
	 */
	String CLASSPATH_FILE_NAME = ".classpath"; //$NON-NLS-1$

	/**
	 * Decodes the classpath entry that has been encoded in the given string
	 * in the context of this project.
	 * Returns null if the encoded entry is malformed.
	 *
	 * @param encodedEntry the encoded classpath entry
	 * @return the decoded classpath entry, or <code>null</code> if unable to decode it
	 * @since 3.2
	 */
	IClasspathEntry decodeClasspathEntry(String encodedEntry);

	/**
	 * Encodes the given classpath entry into a string in the context of this project.
	 *
	 * @param classpathEntry the classpath entry to encode
	 * @return the encoded classpath entry
	 * @since 3.2
	 */
	String encodeClasspathEntry(IClasspathEntry classpathEntry);

	/**
	 * Returns the <code>IJavaElement</code> corresponding to the given
	 * classpath-relative path, or <code>null</code> if no such
	 * <code>IJavaElement</code> is found. The result is one of an
	 * <code>ICompilationUnit</code>, <code>IClassFile</code>, or
	 * <code>IPackageFragment</code>.
	 * <p>
	 * When looking for a package fragment, there might be several potential
	 * matches; only one of them is returned.
	 *
	 * <p>For example, the path "java/lang/Object.java", would result in the
	 * <code>ICompilationUnit</code> or <code>IClassFile</code> corresponding to
	 * "java.lang.Object". The path "java/lang" would result in the
	 * <code>IPackageFragment</code> for "java.lang".
	 * @param path the given classpath-relative path
	 * @exception JavaModelException if the given path is <code>null</code>
	 *  or absolute
	 * @return the <code>IJavaElement</code> corresponding to the given
	 * classpath-relative path, or <code>null</code> if no such
	 * <code>IJavaElement</code> is found
	 */
	IJavaElement findElement(IPath path) throws JavaModelException;

	/**
	 * Returns the <code>IJavaElement</code> corresponding to the given
	 * classpath-relative path, or <code>null</code> if no such
	 * <code>IJavaElement</code> is found. The result is one of an
	 * <code>ICompilationUnit</code>, <code>IClassFile</code>, or
	 * <code>IPackageFragment</code>. If it is an <code>ICompilationUnit</code>,
	 * its owner is the given owner.
	 * <p>
	 * When looking for a package fragment, there might be several potential
	 * matches; only one of them is returned.
	 *
	 * <p>For example, the path "java/lang/Object.java", would result in the
	 * <code>ICompilationUnit</code> or <code>IClassFile</code> corresponding to
	 * "java.lang.Object". The path "java/lang" would result in the
	 * <code>IPackageFragment</code> for "java.lang".
	 * @param path the given classpath-relative path
	 * @param owner the owner of the returned compilation unit, ignored if it is
	 *   not a compilation unit.
	 * @exception JavaModelException if the given path is <code>null</code>
	 *  or absolute
	 * @return the <code>IJavaElement</code> corresponding to the given
	 * classpath-relative path, or <code>null</code> if no such
	 * <code>IJavaElement</code> is found
	 * @since 3.0
	 */
	IJavaElement findElement(IPath path, WorkingCopyOwner owner) throws JavaModelException;

	/**
	 * Finds the Java element corresponding to the given binding key if any,
	 * else returns <code>null</code>. Elements are looked up using this
	 * project's classpath. The first element corresponding to
	 * the given key on this project's classpath is returned.
	 * <p>Possible elements are:
	 * <ul>
	 * <li>{@link IPackageFragment} for a binding key from an
	 * 		{@link IPackageBinding}</li>
	 * <li>{@link IType} for a binding key from an {@link ITypeBinding}</li>
	 * <li>{@link IMethod} for a binding key from an {@link IMethodBinding}</li>
	 * <li>{@link IField} for a binding key from an {@link IVariableBinding}
	 * 		representing a {@link IVariableBinding#isField() field}</li>
	 * <li>{@link ITypeParameter} for a binding key from an {@link ITypeBinding}
	 * 		representing a {@link ITypeBinding#isTypeVariable() type
	 * 		variable}</li>
	 * <li>{@link IAnnotation} for a binding key from an
	 * 		{@link IAnnotationBinding}</li>
	 * </ul>
	 * <p>Note: if two methods correspond to the binding key because their
	 * parameter types' simple names are the same, then the first one is returned.
	 * For example, if a class defines two methods <code>foo(p1.Y, String)</code>
	 * and <code>foo(p2.Y, String)</code>, in both cases the parameter type's
	 * simple names  are <code>{"Y", "String"}</code>. Thus
	 * <code>foo(p1.Y, String)</code> is returned.</p>
	 *
	 * @param bindingKey the given binding key
	 * @param owner the owner of the returned element's compilation unit,
	 * 		or <code>null</code> if the default working copy owner must be
	 * 		used
	 * @exception JavaModelException if this project does not exist or if an
	 *		exception occurs while accessing its corresponding resource
	 * @return the Java element corresponding to the given key,
	 * 		or <code>null</code> if no such Java element is found
	 * @since 3.4
	 */
	IJavaElement findElement(String bindingKey, WorkingCopyOwner owner) throws JavaModelException;

	/**
	 * Returns the first existing package fragment on this project's classpath
	 * whose path matches the given (absolute) path, or <code>null</code> if none
	 * exist.
	 * The path can be:
	 * 	- internal to the workbench: "/Project/src"
	 *  - external to the workbench: "c:/jdk/classes.zip/java/lang"
	 * @param path the given absolute path
	 * @exception JavaModelException if this project does not exist or if an
	 *		exception occurs while accessing its corresponding resource
	 * @return the first existing package fragment on this project's classpath
	 * whose path matches the given (absolute) path, or <code>null</code> if none
	 * exist
	 */
	IPackageFragment findPackageFragment(IPath path) throws JavaModelException;

	/**
	 * Returns the existing package fragment root on this project's classpath
	 * whose path matches the given (absolute) path, or <code>null</code> if
	 * one does not exist.
	 * The path can be:
	 *	- internal to the workbench: "/Compiler/src"
	 *	- external to the workbench: "c:/jdk/classes.zip"
	 * @param path the given absolute path
	 * @exception JavaModelException if this project does not exist or if an
	 *		exception occurs while accessing its corresponding resource
	 * @return the existing package fragment root on this project's classpath
	 * whose path matches the given (absolute) path, or <code>null</code> if
	 * one does not exist
	 */
	IPackageFragmentRoot findPackageFragmentRoot(IPath path)
		throws JavaModelException;
	/**
	 * Returns the existing package fragment roots identified by the given entry.
	 * A classpath entry within the current project identifies a single root.
	 * <p>
	 * If the classpath entry denotes a variable, it will be resolved and return
	 * the roots of the target entry (empty if not resolvable).
	 * <p>
	 * If the classpath entry denotes a container, it will be resolved and return
	 * the roots corresponding to the set of container entries (empty if not resolvable).
	 * <p>
	 * The result does not include package fragment roots in other projects
	 * referenced on this project's classpath.
	 *
	 * @param entry the given entry
	 * @return the existing package fragment roots identified by the given entry
	 * @see IClasspathContainer
	 * @since 2.1
	 */
	IPackageFragmentRoot[] findPackageFragmentRoots(IClasspathEntry entry);

	/**
	 * In a Java 9 project, a classpath entry can be filtered using a {@link IClasspathAttribute#LIMIT_MODULES} attribute,
	 * otherwise for an unnamed module a default set of roots is used as defined in JEP 261.
	 * In both cases {@link IJavaProject#findPackageFragmentRoots(IClasspathEntry)} will not contain all roots physically
	 * present in the container.
	 * <p>
	 * This API can be used to bypass any filter and get really all roots to which the given entry is resolved.
	 * </p>
	 *
	 * @param entry a classpath entry of this Java project
	 * @return the unfiltered array of package fragment roots to which the classpath entry resolves
	 * @see #findPackageFragmentRoots(IClasspathEntry)
	 * @since 3.14
	 */
	IPackageFragmentRoot[] findUnfilteredPackageFragmentRoots(IClasspathEntry entry);

	/**
	 * Returns the first type (excluding secondary types) found following this project's
	 * classpath with the given fully qualified name or <code>null</code> if none is found.
	 * The fully qualified name is a dot-separated name. For example,
	 * a class B defined as a member type of a class A in package x.y should have a
	 * the fully qualified name "x.y.A.B".
	 *
	 * Note that in order to be found, a type name (or its top level enclosing
	 * type name) must match its corresponding compilation unit name. As a
	 * consequence, secondary types cannot be found using this functionality.
	 * To find secondary types use {@link #findType(String, IProgressMonitor)} instead.
	 *
	 * @param fullyQualifiedName the given fully qualified name
	 * @exception JavaModelException if this project does not exist or if an
	 *		exception occurs while accessing its corresponding resource
	 * @return the first type found following this project's classpath
	 * with the given fully qualified name or <code>null</code> if none is found
	 * @see IType#getFullyQualifiedName(char)
	 * @since 2.0
	 */
	IType findType(String fullyQualifiedName) throws JavaModelException;
	/**
	 * Same functionality as {@link #findType(String)} but also looks for secondary
	 * types if the given name does not match a compilation unit name.
	 *
	 * @param fullyQualifiedName the given fully qualified name
	 * @param progressMonitor the progress monitor to report progress to,
	 * 	or <code>null</code> if no progress monitor is provided
	 * @exception JavaModelException if this project does not exist or if an
	 *		exception occurs while accessing its corresponding resource
	 * @return the first type found following this project's classpath
	 * with the given fully qualified name or <code>null</code> if none is found
	 * @see IType#getFullyQualifiedName(char)
	 * @since 3.2
	 */
	IType findType(String fullyQualifiedName, IProgressMonitor progressMonitor) throws JavaModelException;
	/**
	 * Returns the first type (excluding secondary types) found following this project's
	 * classpath with the given fully qualified name or <code>null</code> if none is found.
	 * The fully qualified name is a dot-separated name. For example,
	 * a class B defined as a member type of a class A in package x.y should have a
	 * the fully qualified name "x.y.A.B".
	 * If the returned type is part of a compilation unit, its owner is the given
	 * owner.
	 *
	 * Note that in order to be found, a type name (or its top level enclosing
	 * type name) must match its corresponding compilation unit name. As a
	 * consequence, secondary types cannot be found using this functionality.
	 * To find secondary types use {@link #findType(String, WorkingCopyOwner, IProgressMonitor)}
	 * instead.
	 *
	 * @param fullyQualifiedName the given fully qualified name
	 * @param owner the owner of the returned type's compilation unit
	 * @exception JavaModelException if this project does not exist or if an
	 *		exception occurs while accessing its corresponding resource
	 * @return the first type found following this project's classpath
	 * with the given fully qualified name or <code>null</code> if none is found
	 * @see IType#getFullyQualifiedName(char)
	 * @since 3.0
	 */
	IType findType(String fullyQualifiedName, WorkingCopyOwner owner) throws JavaModelException;
	/**
	 * Same functionality as {@link #findType(String, WorkingCopyOwner)}
	 * but also looks for secondary types if the given name does not match
	 * a compilation unit name.
	 *
	 * @param fullyQualifiedName the given fully qualified name
	 * @param owner the owner of the returned type's compilation unit
	 * @param progressMonitor the progress monitor to report progress to,
	 * 	or <code>null</code> if no progress monitor is provided
	 * @exception JavaModelException if this project does not exist or if an
	 *		exception occurs while accessing its corresponding resource
	 * @return the first type found following this project's classpath
	 * with the given fully qualified name or <code>null</code> if none is found
	 * @see IType#getFullyQualifiedName(char)
	 * @since 3.2
	 */
	IType findType(String fullyQualifiedName, WorkingCopyOwner owner, IProgressMonitor progressMonitor) throws JavaModelException;
	/**
	 * Returns the first type (excluding secondary types) found following this
	 * project's classpath with the given package name and type qualified name
	 * or <code>null</code> if none is found.
	 * The package name is a dot-separated name.
	 * The type qualified name is also a dot-separated name. For example,
	 * a class B defined as a member type of a class A should have the
	 * type qualified name "A.B".
	 *
	 * Note that in order to be found, a type name (or its top level enclosing
	 * type name) must match its corresponding compilation unit name. As a
	 * consequence, secondary types cannot be found using this functionality.
	 * To find secondary types use {@link #findType(String, String, IProgressMonitor)}
	 * instead.
	 *
	 * @param packageName the given package name
	 * @param typeQualifiedName the given type qualified name
	 * @exception JavaModelException if this project does not exist or if an
	 *		exception occurs while accessing its corresponding resource
	 * @return the first type found following this project's classpath
	 * with the given package name and type qualified name
	 * or <code>null</code> if none is found
	 * @see IType#getTypeQualifiedName(char)
	 * @since 2.0
	 */
	IType findType(String packageName, String typeQualifiedName) throws JavaModelException;
	/**
	 * Same functionality as {@link #findType(String, String)} but also looks for
	 * secondary types if the given name does not match a compilation unit name.
	 *
	 * @param packageName the given package name
	 * @param typeQualifiedName the given type qualified name
	 * @param progressMonitor the progress monitor to report progress to,
	 * 	or <code>null</code> if no progress monitor is provided
	 * @exception JavaModelException if this project does not exist or if an
	 *		exception occurs while accessing its corresponding resource
	 * @return the first type found following this project's classpath
	 * with the given fully qualified name or <code>null</code> if none is found
	 * @see IType#getFullyQualifiedName(char)
	 * @since 3.2
	 */
	IType findType(String packageName, String typeQualifiedName, IProgressMonitor progressMonitor) throws JavaModelException;
	/**
	 * Returns the first type (excluding secondary types) found following this
	 * project's classpath with the given package name and type qualified name
	 * or <code>null</code> if none is found.
	 * The package name is a dot-separated name.
	 * The type qualified name is also a dot-separated name. For example,
	 * a class B defined as a member type of a class A should have the
	 * type qualified name "A.B".
	 * If the returned type is part of a compilation unit, its owner is the given
	 * owner.
	 *
	 * Note that in order to be found, a type name (or its top level enclosing
	 * type name) must match its corresponding compilation unit name. As a
	 * consequence, secondary types cannot be found using this functionality.
	 * To find secondary types use {@link #findType(String, String, WorkingCopyOwner, IProgressMonitor)}
	 * instead.
	 *
	 * @param packageName the given package name
	 * @param typeQualifiedName the given type qualified name
	 * @param owner the owner of the returned type's compilation unit
	 * @exception JavaModelException if this project does not exist or if an
	 *		exception occurs while accessing its corresponding resource
	 * @return the first type found following this project's classpath
	 * with the given package name and type qualified name
	 * or <code>null</code> if none is found
	 * @see IType#getTypeQualifiedName(char)
	 * @since 3.0
	 */
	IType findType(String packageName, String typeQualifiedName, WorkingCopyOwner owner) throws JavaModelException;
	/**
	 * Same functionality as {@link #findType(String, String, WorkingCopyOwner)}
	 * but also looks for secondary types if the given name does not match a compilation unit name.
	 *
	 * @param packageName the given package name
	 * @param typeQualifiedName the given type qualified name
	 * @param owner the owner of the returned type's compilation unit
	 * @param progressMonitor the progress monitor to report progress to,
	 * 	or <code>null</code> if no progress monitor is provided
	 * @exception JavaModelException if this project does not exist or if an
	 *		exception occurs while accessing its corresponding resource
	 * @return the first type found following this project's classpath
	 * with the given fully qualified name or <code>null</code> if none is found
	 * @see IType#getFullyQualifiedName(char)
	 * @since 3.2
	 */
	IType findType(String packageName, String typeQualifiedName, WorkingCopyOwner owner, IProgressMonitor progressMonitor) throws JavaModelException;

	/**
	 * Finds the first module with the given name found following this project's module path.
	 * If the returned module descriptor is part of a compilation unit, its owner is the given owner.
	 * @param moduleName the given module name
	 * @param owner the owner of the returned module descriptor's compilation unit
	 *
	 * @exception JavaModelException if this project does not exist or if an
	 *		exception occurs while accessing its corresponding resource
	 * @return the first module found following this project's module path
	 * with the given name or <code>null</code> if none is found
	 * @since 3.14
	 */
	IModuleDescription findModule(String moduleName, WorkingCopyOwner owner) throws JavaModelException;

	/**
	 * Returns all of the existing package fragment roots that exist
	 * on the classpath, in the order they are defined by the classpath.
	 *
	 * @return all of the existing package fragment roots that exist
	 * on the classpath
	 * @exception JavaModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource
	 */
	IPackageFragmentRoot[] getAllPackageFragmentRoots() throws JavaModelException;

	/**
	 * Returns an array of non-Java resources directly contained in this project.
	 * It does not transitively answer non-Java resources contained in folders;
	 * these would have to be explicitly iterated over.
	 * <p>
	 * Non-Java resources includes other files and folders located in the
	 * project not accounted for by any of it source or binary package fragment
	 * roots. If the project is a source folder itself, resources excluded from the
	 * corresponding source classpath entry by one or more exclusion patterns
	 * are considered non-Java resources and will appear in the result
	 * (possibly in a folder)
	 * </p>
	 *
	 * @return an array of non-Java resources (<code>IFile</code>s and/or
	 *              <code>IFolder</code>s) directly contained in this project
	 * @exception JavaModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource
	 */
	Object[] getNonJavaResources() throws JavaModelException;

	/**
	 * Helper method for returning one option value only. Equivalent to <code>(String)this.getOptions(inheritJavaCoreOptions).get(optionName)</code>
	 * Note that it may answer <code>null</code> if this option does not exist, or if there is no custom value for it.
	 * <p>
	 * For a complete description of the configurable options, see <code>JavaCore#getDefaultOptions</code>.
	 * </p>
	 *
	 * @param optionName the name of an option
	 * @param inheritJavaCoreOptions - boolean indicating whether JavaCore options should be inherited as well
	 * @return the String value of a given option
	 * @see JavaCore#getDefaultOptions()
	 * @since 2.1
	 */
	String getOption(String optionName, boolean inheritJavaCoreOptions);

	/**
	 * Returns the table of the current custom options for this project. Projects remember their custom options,
	 * in other words, only the options different from the the JavaCore global options for the workspace.
	 * A boolean argument allows to directly merge the project options with global ones from <code>JavaCore</code>.
	 * <p>
	 * For a complete description of the configurable options, see <code>JavaCore#getDefaultOptions</code>.
	 * </p>
	 *
	 * @param inheritJavaCoreOptions - boolean indicating whether JavaCore options should be inherited as well
	 * @return table of current settings of all options
	 *   (key type: <code>String</code>; value type: <code>String</code>)
	 * @see JavaCore#getDefaultOptions()
	 * @since 2.1
	 */
	Map<String, String> getOptions(boolean inheritJavaCoreOptions);

	/**
	 * Returns the default output location for this project as a workspace-
	 * relative absolute path.
	 * <p>
	 * The default output location is where class files are ordinarily generated
	 * (and resource files, copied). Each source classpath entry can also
	 * specify an output location for the generated class files (and copied
	 * resource files) corresponding to compilation units under that source
	 * folder. This makes it possible to arrange generated class files for
	 * different source folders in different output folders, and not
	 * necessarily the default output folder. This means that the generated
	 * class files for the project may end up scattered across several folders,
	 * rather than all in the default output folder (which is more standard).
	 * </p>
	 *
	 * @return the workspace-relative absolute path of the default output folder
	 * @exception JavaModelException if this element does not exist
	 * @see #setOutputLocation(org.eclipse.core.runtime.IPath, IProgressMonitor)
	 * @see IClasspathEntry#getOutputLocation()
	 */
	IPath getOutputLocation() throws JavaModelException;

	/**
	 * Returns a package fragment root for an external library
	 * (a ZIP archive - e.g. a <code>.jar</code>, a <code>.zip</code> file, etc. -
	 * or - since 3.4 - a class folder) at the specified file system path.
	 * This is a handle-only method.  The underlying <code>java.io.File</code>
	 * may or may not exist. No resource is associated with this local library
	 * package fragment root.
	 *
	 * @param externalLibraryPath the library's file system path
	 * @return a package fragment root for the external library at the specified file system path
	 */
	IPackageFragmentRoot getPackageFragmentRoot(String externalLibraryPath);

	/**
	 * Returns a package fragment root for the given resource, which
	 * must either be a folder representing the top of a package hierarchy,
	 * or a ZIP archive (e.g. a <code>.jar</code>, a <code>.zip</code> file, etc.)
	 * This is a handle-only method.  The underlying resource may or may not exist.
	 *
	 * @param resource the given resource
	 * @return a package fragment root for the given resource, which
	 * must either be a folder representing the top of a package hierarchy,
	 * or a ZIP archive (e.g. a <code>.jar</code>, a <code>.zip</code> file, etc.)
	 */
	IPackageFragmentRoot getPackageFragmentRoot(IResource resource);

	/**
	 * Returns all of the  package fragment roots contained in this
	 * project, identified on this project's resolved classpath. The result
	 * does not include package fragment roots in other projects referenced
	 * on this project's classpath. The package fragment roots appear in the
	 * order they are defined by the classpath.
	 *
	 * <p>NOTE: This is equivalent to <code>getChildren()</code>.
	 *
	 * @return all of the  package fragment roots contained in this
	 * project, identified on this project's resolved classpath
	 * @exception JavaModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource
	 */
	IPackageFragmentRoot[] getPackageFragmentRoots() throws JavaModelException;

	/**
	 * Returns the existing package fragment roots identified by the given entry.
	 * A classpath entry within the current project identifies a single root.
	 * <p>
	 * If the classpath entry denotes a variable, it will be resolved and return
	 * the roots of the target entry (empty if not resolvable).
	 * <p>
	 * If the classpath entry denotes a container, it will be resolved and return
	 * the roots corresponding to the set of container entries (empty if not resolvable).
	 * <p>
	 * The result does not include package fragment roots in other projects
	 * referenced on this project's classpath.
	 *
	 * @param entry the given entry
	 * @return the existing package fragment roots identified by the given entry
	 * @see IClasspathContainer
	 * @deprecated Use {@link IJavaProject#findPackageFragmentRoots(IClasspathEntry)} instead
	 */
	IPackageFragmentRoot[] getPackageFragmentRoots(IClasspathEntry entry);

	/**
	 * Returns all package fragments in all package fragment roots contained
	 * in this project. This is a convenience method.
	 *
	 * Note that the package fragment roots corresponds to the resolved
	 * classpath of the project.
	 *
	 * @return all package fragments in all package fragment roots contained
	 * in this project
	 * @exception JavaModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource
	 */
	IPackageFragment[] getPackageFragments() throws JavaModelException;

	/**
	 * Returns the <code>IProject</code> on which this <code>IJavaProject</code>
	 * was created. This is handle-only method.
	 *
	 * @return the <code>IProject</code> on which this <code>IJavaProject</code>
	 * was created
	 */
	IProject getProject();

	/**
	 * Returns the {@link IModuleDescription} this project represents or
	 * null if the Java project doesn't represent any named module. A Java
	 * project is said to represent a module if any of its source package
	 * fragment roots (see {@link IPackageFragmentRoot#K_SOURCE}) contains a
	 * valid Java module descriptor, or if one of its classpath entries
	 * has a valid {@link IClasspathAttribute#PATCH_MODULE} attribute
	 * affecting the current project.
	 * In the latter case the corresponding module description of the
	 * location referenced by that classpath entry is returned.
	 *
	 * @return the {@link IModuleDescription} this project represents.
	 * @exception JavaModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource
	 * @since 3.14
	 */
	IModuleDescription getModuleDescription() throws JavaModelException;

	/**
	 * Returns the <code>IModuleDescription</code> owned by this project or
	 * null if the Java project doesn't own a valid Java module descriptor.
	 * This method considers only module descriptions contained in any of the
	 * project's source package fragment roots (see {@link IPackageFragmentRoot#K_SOURCE}).
	 * In particular any {@link IClasspathAttribute#PATCH_MODULE} attribute
	 * is not considered.
	 *
	 * @return the {@link IModuleDescription} this project owns.
	 * @exception JavaModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource
	 * @throws JavaModelException
	 * @since 3.20
	 */
	IModuleDescription getOwnModuleDescription() throws JavaModelException;

	/**
	 * Returns the raw classpath for the project, as a list of classpath
	 * entries. This corresponds to the exact set of entries which were assigned
	 * using <code>setRawClasspath</code>, in particular such a classpath may
	 * contain classpath variable and classpath container entries. Classpath
	 * variable and classpath container entries can be resolved using the
	 * helper method <code>getResolvedClasspath</code>; classpath variable
	 * entries also can be resolved individually using
	 * <code>JavaCore#getClasspathVariable</code>).
	 * <p>
	 * Both classpath containers and classpath variables provides a level of
	 * indirection that can make the <code>.classpath</code> file stable across
	 * workspaces.
	 * As an example, classpath variables allow a classpath to no longer refer
	 * directly to external JARs located in some user specific location.
	 * The classpath can simply refer to some variables defining the proper
	 * locations of these external JARs. Similarly, classpath containers
	 * allows classpath entries to be computed dynamically by the plug-in that
	 * defines that kind of classpath container.
	 * </p>
	 * <p>
	 * Note that in case the project isn't yet opened, the classpath will
	 * be read directly from the associated <code>.classpath</code> file.
	 * </p>
	 *
	 * @return the raw classpath for the project, as a list of classpath entries
	 * @exception JavaModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource
	 * @see IClasspathEntry
	 */
	IClasspathEntry[] getRawClasspath() throws JavaModelException;

	/**
	 * Returns the names of the projects that are directly required by this
	 * project. A project is required if it is in its classpath.
	 * <p>
	 * The project names are returned in the order they appear on the classpath.
	 *
	 * @return the names of the projects that are directly required by this
	 * project in classpath order
	 * @exception JavaModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource
	 */
	String[] getRequiredProjectNames() throws JavaModelException;

	/**
	 * This is a helper method returning the resolved classpath for the project
	 * as a list of simple (non-variable, non-container) classpath entries.
	 * All classpath variable and classpath container entries in the project's
	 * raw classpath will be replaced by the simple classpath entries they
	 * resolve to.
	 * <p>
	 * The resulting resolved classpath is accurate for the given point in time.
	 * If the project's raw classpath is later modified, or if classpath
	 * variables are changed, the resolved classpath can become out of date.
	 * Because of this, hanging on resolved classpath is not recommended.
	 * </p>
	 * <p>
	 * Note that if the resolution creates duplicate entries
	 * (i.e. {@link IClasspathEntry entries} which are {@link Object#equals(Object)}),
	 * only the first one is added to the resolved classpath.
	 * </p>
	 *
	 * @param ignoreUnresolvedEntry indicates how to handle unresolvable
	 * variables and containers; <code>true</code> indicates that missing
	 * variables and unresolvable classpath containers should be silently
	 * ignored, and that the resulting list should consist only of the
	 * entries that could be successfully resolved; <code>false</code> indicates
	 * that a <code>JavaModelException</code> should be thrown for the first
	 * unresolved variable or container
	 * @return the resolved classpath for the project as a list of simple
	 * classpath entries, where all classpath variable and container entries
	 * have been resolved and substituted with their final target entries
	 * @exception JavaModelException in one of the corresponding situation:
	 * <ul>
	 *    <li>this element does not exist</li>
	 *    <li>an exception occurs while accessing its corresponding resource</li>
	 *    <li>a classpath variable or classpath container was not resolvable
	 *    and <code>ignoreUnresolvedEntry</code> is <code>false</code>.</li>
	 * </ul>
	 * @see IClasspathEntry
	 */
	IClasspathEntry[] getResolvedClasspath(boolean ignoreUnresolvedEntry)
	     throws JavaModelException;

	/**
	 * Returns whether this project has been built at least once and thus whether it has a build state.
	 * @return true if this project has been built at least once, false otherwise
	 */
	boolean hasBuildState();

	/**
	 * Returns whether setting this project's classpath to the given classpath entries
	 * would result in a cycle.
	 *
	 * If the set of entries contains some variables, those are resolved in order to determine
	 * cycles.
	 *
	 * @param entries the given classpath entries
	 * @return true if the given classpath entries would result in a cycle, false otherwise
	 */
	boolean hasClasspathCycle(IClasspathEntry[] entries);
	/**
	 * Returns whether the given element is on the classpath of this project,
	 * that is, referenced from a classpath entry and not explicitly excluded
	 * using an exclusion pattern.
	 *
	 * @param element the given element
	 * @return <code>true</code> if the given element is on the classpath of
	 * this project, <code>false</code> otherwise
	 * @see IClasspathEntry#getInclusionPatterns()
	 * @see IClasspathEntry#getExclusionPatterns()
	 * @since 2.0
	 */
	boolean isOnClasspath(IJavaElement element);
	/**
	 * Returns whether the given resource is on the classpath of this project,
	 * that is, referenced from a classpath entry and not explicitly excluded
	 * using an exclusion pattern.
	 *
	 * @param resource the given resource
	 * @return <code>true</code> if the given resource is on the classpath of
	 * this project, <code>false</code> otherwise
	 * @see IClasspathEntry#getInclusionPatterns()
	 * @see IClasspathEntry#getExclusionPatterns()
	 * @since 2.1
	 */
	boolean isOnClasspath(IResource resource);

	/**
	 * Returns the class path entry which contains the given resource and not explicitly excluded using an exclusion
	 * pattern, or null otherwise.
	 *
	 * @param resource
	 *            the resource which may or may not on one of the class path entries.
	 * @return the class path entry which contains the given resource, or null, if it's not in any of the classpath
	 *         entries, or the resource is null.
	 * @since 3.28
	 */
	IClasspathEntry findContainingClasspathEntry(IResource resource);

	/**
	 * Creates a new evaluation context.
	 * @return a new evaluation context.
	 */
	IEvaluationContext newEvaluationContext();

	/**
	 * Creates and returns a type hierarchy for all types in the given
	 * region, considering subtypes within that region.
	 *
	 * @param monitor the given progress monitor
	 * @param region the given region
	 * @exception JavaModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource
	 * @exception IllegalArgumentException if region is <code>null</code>
	 * @return a type hierarchy for all types in the given
	 * region, considering subtypes within that region
	 */
	ITypeHierarchy newTypeHierarchy(IRegion region, IProgressMonitor monitor)
		throws JavaModelException;

	/**
	 * Creates and returns a type hierarchy for all types in the given
	 * region, considering subtypes within that region and considering types in the
	 * working copies with the given owner.
	 * In other words, the owner's working copies will take
	 * precedence over their original compilation units in the workspace.
	 * <p>
	 * Note that if a working copy is empty, it will be as if the original compilation
	 * unit had been deleted.
	 * <p>
	 *
	 * @param monitor the given progress monitor
	 * @param region the given region
	 * @param owner the owner of working copies that take precedence over their original compilation units
	 * @exception JavaModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource
	 * @exception IllegalArgumentException if region is <code>null</code>
	 * @return a type hierarchy for all types in the given
	 * region, considering subtypes within that region
	 * @since 3.0
	 */
	ITypeHierarchy newTypeHierarchy(IRegion region, WorkingCopyOwner owner, IProgressMonitor monitor)
		throws JavaModelException;

	/**
	 * Creates and returns a type hierarchy for the given type considering
	 * subtypes in the specified region.
	 *
	 * @param type the given type
	 * @param region the given region
	 * @param monitor the given monitor
	 *
	 * @exception JavaModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource
	 *
	 * @exception IllegalArgumentException if type or region is <code>null</code>
	 * @return a type hierarchy for the given type considering
	 * subtypes in the specified region
	 */
	ITypeHierarchy newTypeHierarchy(
		IType type,
		IRegion region,
		IProgressMonitor monitor)
		throws JavaModelException;

	/**
	 * Creates and returns a type hierarchy for the given type considering
	 * subtypes in the specified region and considering types in the
	 * working copies with the given owner.
	 * In other words, the owner's working copies will take
	 * precedence over their original compilation units in the workspace.
	 * <p>
	 * Note that if a working copy is empty, it will be as if the original compilation
	 * unit had been deleted.
	 * <p>
	 *
	 * @param type the given type
	 * @param region the given region
	 * @param monitor the given monitor
	 * @param owner the owner of working copies that take precedence over their original compilation units
	 *
	 * @exception JavaModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource
	 *
	 * @exception IllegalArgumentException if type or region is <code>null</code>
	 * @return a type hierarchy for the given type considering
	 * subtypes in the specified region
	 * @since 3.0
	 */
	ITypeHierarchy newTypeHierarchy(
		IType type,
		IRegion region,
		WorkingCopyOwner owner,
		IProgressMonitor monitor)
		throws JavaModelException;

	/**
	 * Returns the default output location for the project as defined by its <code>.classpath</code> file from disk, or <code>null</code>
	 * if unable to read the file.
	 * <p>
	 * This output location may differ from the in-memory one returned by <code>getOutputLocation</code>, in case the
	 * automatic reconciliation mechanism has not been performed yet. Usually, any change to the <code>.classpath</code> file
	 * is automatically noticed and reconciled at the next resource change notification event.
	 * However, if the file is modified within an operation, where this change needs to be taken into account before the
	 * operation ends, then the output location from disk can be read using this method, and further assigned to the project
	 * using <code>setRawClasspath(...)</code>.
	 * <p>
	 * The default output location is where class files are ordinarily generated
	 * (and resource files, copied). Each source classpath entry can also
	 * specify an output location for the generated class files (and copied
	 * resource files) corresponding to compilation units under that source
	 * folder. This makes it possible to arrange generated class files for
	 * different source folders in different output folders, and not
	 * necessarily the default output folder. This means that the generated
	 * class files for the project may end up scattered across several folders,
	 * rather than all in the default output folder (which is more standard).
	 * <p>
	 * In order to manually force a project classpath refresh, one can simply assign the project classpath using the result of this
	 * method, as follows:
	 * <code>proj.setRawClasspath(proj.readRawClasspath(), proj.readOutputLocation(), monitor)</code>
	 * (note that the <code>readRawClasspath/readOutputLocation</code> methods could return <code>null</code>).
	 * <p>
	 * @return the workspace-relative absolute path of the default output folder
	 * @see #getOutputLocation()
	 * @since 3.0
	 */
	IPath readOutputLocation();

	/**
	 * Returns the raw classpath for the project as defined by its
	 * <code>.classpath</code> file from disk, or <code>null</code>
	 * if unable to read the file.
	 * <p>
	 * This classpath may differ from the in-memory classpath returned by
	 * <code>getRawClasspath</code>, in case the automatic reconciliation
	 * mechanism has not been performed yet. Usually, any change to the
	 * <code>.classpath</code> file is automatically noticed and reconciled at
	 * the next resource change notification event. However, if the file is
	 * modified within an operation, where this change needs to be taken into
	 * account before the operation ends, then the classpath from disk can be
	 * read using this method, and further assigned to the project using
	 * <code>setRawClasspath(...)</code>.
	 * </p>
	 * <p>
	 * Classpath variable and classpath container entries can be resolved using
	 * the helper method <code>getResolvedClasspath</code>; classpath variable
	 * entries also can be resolved individually using
	 * <code>JavaCore#getClasspathVariable</code>).
	 * </p>
	 * <p>
	 * Note that no check is performed whether the project has the Java nature
	 * set, allowing an existing <code>.classpath</code> file to be considered
	 * independantly (unlike <code>getRawClasspath</code> which requires the
	 * Java nature to be associated with the project).
	 * </p>
	 * <p>
	 * In order to manually force a project classpath refresh, one can simply
	 * assign the project classpath using the result of this method, as follows:
	 * <code>proj.setRawClasspath(proj.readRawClasspath(), proj.readOutputLocation(), monitor)</code>
	 * (note that the <code>readRawClasspath/readOutputLocation</code> methods
	 * could return <code>null</code>).
	 * </p>
	 *
	 * @return the raw classpath from disk for the project, as a list of
	 * classpath entries
	 * @see #getRawClasspath()
	 * @see IClasspathEntry
	 * @since 3.0
	 */
	IClasspathEntry[] readRawClasspath();

	/**
	 * Helper method for setting one option value only.
	 *<p>
	 * Equivalent to:
	 * <pre>
	 * 	Map options = this.getOptions(false);
	 * 	map.put(optionName, optionValue);
	 * 	this.setOptions(map)
	 *  </pre>
	 * <p>
	 * For a complete description of the configurable options, see <code>JavaCore#getDefaultOptions</code>.
	 * </p>
	 *
	 * @param optionName the name of an option
	 * @param optionValue the value of the option to set. If <code>null</code>, then the option
	 * 	is removed from project preferences.
	 * @throws NullPointerException if <code>optionName</code> is <code>null</code>
	 * 	(see {@link org.osgi.service.prefs.Preferences#put(String, String)}).
	 * @see JavaCore#getDefaultOptions()
	 * @since 3.0
	 */
	void setOption(String optionName, String optionValue);

	/**
	 * Sets the project custom options. All and only the options explicitly included in the given table
	 * are remembered; all previous option settings are forgotten, including ones not explicitly
	 * mentioned.
	 * <p>
	 * For a complete description of the configurable options, see <code>JavaCore#getDefaultOptions</code>.
	 * </p>
	 *
	 * @param newOptions the new options (key type: <code>String</code>; value type: <code>String</code>),
	 *   or <code>null</code> to flush all custom options (clients will automatically get the global JavaCore options).
	 * @see JavaCore#getDefaultOptions()
	 * @since 2.1
	 */
	void setOptions(Map<String, String> newOptions);

	/**
	 * Sets the default output location of this project to the location
	 * described by the given workspace-relative absolute path.
	 * <p>
	 * The default output location is where class files are ordinarily generated
	 * (and resource files, copied). Each source classpath entries can also
	 * specify an output location for the generated class files (and copied
	 * resource files) corresponding to compilation units under that source
	 * folder. This makes it possible to arrange that generated class files for
	 * different source folders to end up in different output folders, and not
	 * necessarily the default output folder. This means that the generated
	 * class files for the project may end up scattered across several folders,
	 * rather than all in the default output folder (which is more standard).
	 * </p>
	 *
	 * @param path the workspace-relative absolute path of the default output
	 * folder
	 * @param monitor the progress monitor
	 *
	 * @exception JavaModelException if the classpath could not be set. Reasons include:
	 * <ul>
	 *  <li> This Java element does not exist (ELEMENT_DOES_NOT_EXIST)</li>
	 *  <li> The path refers to a location not contained in this project (<code>PATH_OUTSIDE_PROJECT</code>)
	 *  <li> The path is not an absolute path (<code>RELATIVE_PATH</code>)
	 *  <li> The path is nested inside a package fragment root of this project (<code>INVALID_PATH</code>)
	 *  <li> The output location is being modified during resource change event notification (CORE_EXCEPTION)
	 * </ul>
	 * @see #getOutputLocation()
     * @see IClasspathEntry#getOutputLocation()
	 */
	void setOutputLocation(IPath path, IProgressMonitor monitor)
		throws JavaModelException;

	/**
	 * Sets both the classpath of this project and its default output
	 * location at once. The classpath is defined using a list of classpath
	 * entries. In particular such a classpath may contain classpath variable entries.
	 * Classpath variable entries can be resolved individually ({@link JavaCore#getClasspathVariable(String)}),
	 * or the full classpath can be resolved at once using the helper method {@link #getResolvedClasspath(boolean)}.
	 * <p>
	 * A classpath variable provides an indirection level for better sharing a classpath. As an example, it allows
	 * a classpath to no longer refer directly to external JARs located in some user specific location. The classpath
	 * can simply refer to some variables defining the proper locations of these external JARs.
	 * </p><p>
	 * If it is specified that this operation cannot modify resources, the .classpath file will not be written to disk
	 * and no error marker will be generated. To synchronize the .classpath with the in-memory classpath,
	 * one can use <code>setRawClasspath(readRawClasspath(), true, monitor)</code>.
	 * </p><p>
	 * Setting the classpath to <code>null</code> specifies a default classpath
	 * (the project root). Setting the classpath to an empty array specifies an
	 * empty classpath.
	 * </p><p>
	 * If a cycle is detected while setting this classpath (and if resources can be modified), an error marker will be added
	 * to the project closing the cycle.
	 * To avoid this problem, use {@link #hasClasspathCycle(IClasspathEntry[])}
	 * before setting the classpath.
	 * <p>
	 * This operation acquires a lock on the workspace's root.
	 *
	 * @param entries a list of classpath entries
	 * @param outputLocation the default output location
	 * @param canModifyResources whether resources should be written to disk if needed
	 * @param monitor the given progress monitor
	 * @exception JavaModelException if the classpath could not be set. Reasons include:
	 * <ul>
	 * <li> This Java element does not exist (ELEMENT_DOES_NOT_EXIST)</li>
	 * <li> The classpath is being modified during resource change event notification (CORE_EXCEPTION)
	 * <li> The classpath failed the validation check as defined by {@link JavaConventions#validateClasspath(IJavaProject, IClasspathEntry[], IPath)}
	 * </ul>
	 * @see IClasspathEntry
	 * @since 3.2
	 */
	void setRawClasspath(IClasspathEntry[] entries, IPath outputLocation, boolean canModifyResources, IProgressMonitor monitor) throws JavaModelException;

	/**
	 * Sets the classpath of this project using a list of classpath entries. In particular such a classpath may contain
	 * classpath variable entries. Classpath variable entries can be resolved individually ({@link JavaCore#getClasspathVariable(String)}),
	 * or the full classpath can be resolved at once using the helper method {@link #getResolvedClasspath(boolean)}.
	 * <p>
	 * A classpath variable provides an indirection level for better sharing a classpath. As an example, it allows
	 * a classpath to no longer refer directly to external JARs located in some user specific location. The classpath
	 * can simply refer to some variables defining the proper locations of these external JARs.
	 * </p><p>
	 * If it is specified that this operation cannot modify resources, the .classpath file will not be written to disk
	 * and no error marker will be generated. To synchronize the .classpath with the in-memory classpath,
	 * one can use <code>setRawClasspath(readRawClasspath(), true, monitor)</code>.
	 * </p><p>
	 * Setting the classpath to <code>null</code> specifies a default classpath
	 * (the project root). Setting the classpath to an empty array specifies an
	 * empty classpath.
	 * </p><p>
	 * If a cycle is detected while setting this classpath (and if resources can be modified), an error marker will be added
	 * to the project closing the cycle.
	 * To avoid this problem, use {@link #hasClasspathCycle(IClasspathEntry[])}
	 * before setting the classpath.
	 * <p>
	 * This operation acquires a lock on the workspace's root.
	 *
	 * @param entries a list of classpath entries
	 * @param canModifyResources whether resources should be written to disk if needed
	 * @param monitor the given progress monitor
	 * @exception JavaModelException if the classpath could not be set. Reasons include:
	 * <ul>
	 * <li> This Java element does not exist (ELEMENT_DOES_NOT_EXIST)</li>
	 * <li> The classpath is being modified during resource change event notification (CORE_EXCEPTION)
	 * <li> The classpath failed the validation check as defined by {@link JavaConventions#validateClasspath(IJavaProject, IClasspathEntry[], IPath)}
	 * </ul>
	 * @see IClasspathEntry
	 * @since 3.2
	 */
	void setRawClasspath(IClasspathEntry[] entries, boolean canModifyResources, IProgressMonitor monitor) throws JavaModelException;

	/**
	 * Works similar to {@link #setRawClasspath(IClasspathEntry[], IPath, IProgressMonitor)} and
	 * additionally allows persisting the given array of referenced entries for this project.
	 * The referenced entries and their attributes are stored in the .classpath file of this
	 * project. For details on referenced entries, see
	 * {@link JavaCore#getReferencedClasspathEntries(IClasspathEntry, IJavaProject)}
	 * and {@link IClasspathEntry#getReferencingEntry()}.
	 * <p>
	 * Since the referenced entries are stored in the .classpath file, clients can store additional
	 * information that belong to these entries and retrieve them across sessions, though the referenced
	 * entries themselves may not be present in the raw classpath. By passing a <code>null</code>
	 * referencedEntries, clients can choose not to modify the already persisted referenced entries,
	 * which is fully equivalent to {@link #setRawClasspath(IClasspathEntry[], IPath, IProgressMonitor)}.
	 * If an empty array is passed as referencedEntries, the already persisted referenced entries,
	 * if any, will be cleared.
	 * </p> <p>
	 * If there are duplicates of a referenced entry or if any of the <code>referencedEntries</code>
	 * is already present in the raw classpath(<code>entries</code>) those referenced entries will
	 * be excluded and not be persisted.
	 *</p>
	 * @param entries a list of classpath entries
	 * @param referencedEntries the list of referenced classpath entries to be persisted
	 * @param outputLocation the default output location
	 * @param monitor the given progress monitor
	 * @exception JavaModelException if the classpath could not be set. Reasons include:
	 * <ul>
	 * <li> This Java element does not exist (ELEMENT_DOES_NOT_EXIST)</li>
	 * <li> The classpath is being modified during resource change event notification (CORE_EXCEPTION)
	 * <li> The classpath failed the validation check as defined by {@link JavaConventions#validateClasspath(IJavaProject, IClasspathEntry[], IPath)}
	 * </ul>
	 * @see IClasspathEntry
	 * @see #getReferencedClasspathEntries()
	 * @since 3.6
	 */
	void setRawClasspath(IClasspathEntry[] entries, IClasspathEntry[] referencedEntries, IPath outputLocation,
			IProgressMonitor monitor) throws JavaModelException;

	/**
	 * Returns the list of referenced classpath entries stored in the .classpath file of <code>this</code>
	 * java project. Clients can store the referenced classpath entries using
	 * {@link #setRawClasspath(IClasspathEntry[], IClasspathEntry[], IPath, IProgressMonitor)}
	 * If the client has not stored any referenced entries for this project, an empty array is returned.
	 *
	 * @throws JavaModelException
	 * @return an array of referenced classpath entries stored for this java project or an empty array if none
	 * 			stored earlier.
	 * @since 3.6
	 */
	IClasspathEntry[] getReferencedClasspathEntries() throws JavaModelException;

	/**
	 * Sets the classpath of this project using a list of classpath entries. In particular such a classpath may contain
	 * classpath variable entries. Classpath variable entries can be resolved individually ({@link JavaCore#getClasspathVariable(String)}),
	 * or the full classpath can be resolved at once using the helper method {@link #getResolvedClasspath(boolean)}.
	 * <p>
	 * A classpath variable provides an indirection level for better sharing a classpath. As an example, it allows
	 * a classpath to no longer refer directly to external JARs located in some user specific location. The classpath
	 * can simply refer to some variables defining the proper locations of these external JARs.
	 * <p>
	 * Setting the classpath to <code>null</code> specifies a default classpath
	 * (the project root). Setting the classpath to an empty array specifies an
	 * empty classpath.
	 * <p>
	 * If a cycle is detected while setting this classpath, an error marker will be added
	 * to the project closing the cycle.
	 * To avoid this problem, use {@link #hasClasspathCycle(IClasspathEntry[])}
	 * before setting the classpath.
	 * <p>
	 * This operation acquires a lock on the workspace's root.
	 *
	 * @param entries a list of classpath entries
	 * @param monitor the given progress monitor
	 * @exception JavaModelException if the classpath could not be set. Reasons include:
	 * <ul>
	 * <li> This Java element does not exist (ELEMENT_DOES_NOT_EXIST)</li>
	 * <li> The classpath is being modified during resource change event notification (CORE_EXCEPTION)
	 * <li> The classpath failed the validation check as defined by {@link JavaConventions#validateClasspath(IJavaProject, IClasspathEntry[], IPath)}
	 * </ul>
	 * @see IClasspathEntry
	 */
	void setRawClasspath(IClasspathEntry[] entries, IProgressMonitor monitor)
		throws JavaModelException;

	/**
	 * Sets the both the classpath of this project and its default output
	 * location at once. The classpath is defined using a list of classpath
	 * entries. In particular, such a classpath may contain classpath variable
	 * entries. Classpath variable entries can be resolved individually (see
	 * ({@link JavaCore#getClasspathVariable(String)}), or the full classpath can be
	 * resolved at once using the helper method
	 * {@link #getResolvedClasspath(boolean)}.
	 * <p>
	 * A classpath variable provides an indirection level for better sharing a
	 * classpath. As an example, it allows a classpath to no longer refer
	 * directly to external JARs located in some user specific location. The
	 * classpath can simply refer to some variables defining the proper
	 * locations of these external JARs.
	 * </p>
	 * <p>
	 * Setting the classpath to <code>null</code> specifies a default classpath
	 * (the project root). Setting the classpath to an empty array specifies an
	 * empty classpath.
	 * </p>
	 * <p>
	 * If a cycle is detected while setting this classpath, an error marker will
	 * be added to the project closing the cycle. To avoid this problem, use
	 * {@link #hasClasspathCycle(IClasspathEntry[])} before setting
	 * the classpath.
	 * </p>
	 * <p>
	 * This operation acquires a lock on the workspace's root.
	 * </p>
	 *
	 * @param entries a list of classpath entries
	 * @param monitor the progress monitor
	 * @param outputLocation the default output location
	 * @exception JavaModelException if the classpath could not be set. Reasons
	 * include:
	 * <ul>
	 * <li> This Java element does not exist (ELEMENT_DOES_NOT_EXIST)</li>
	 * <li> Two or more entries specify source roots with the same or overlapping paths (NAME_COLLISION)
	 * <li> A entry of kind <code>CPE_PROJECT</code> refers to this project (INVALID_PATH)
	 *  <li>This Java element does not exist (ELEMENT_DOES_NOT_EXIST)</li>
	 *	<li>The output location path refers to a location not contained in this project (<code>PATH_OUTSIDE_PROJECT</code>)
	 *	<li>The output location path is not an absolute path (<code>RELATIVE_PATH</code>)
	 *  <li>The output location path is nested inside a package fragment root of this project (<code>INVALID_PATH</code>)
	 * <li> The classpath is being modified during resource change event notification (CORE_EXCEPTION)
	 * </ul>
	 * @see IClasspathEntry
	 * @since 2.0
	 */
	void setRawClasspath(IClasspathEntry[] entries, IPath outputLocation, IProgressMonitor monitor)
		throws JavaModelException;

	/**
	 * Returns the classpath entry that refers to the given path or <code>null</code> if there is no reference to the
	 * path.
	 *
	 * @param path
	 *            IPath
	 * @return the classpath entry or <code>null</code>.
	 * @throws JavaModelException
	 * @since 3.14
	 */
	IClasspathEntry getClasspathEntryFor(IPath path) throws JavaModelException;

	/**
	 * When compiling test code in a modular project that has non-source classpath entries which don't have the
	 * {@link IClasspathAttribute#MODULE} set, the module is assumed to read the unnamed module (which is useful for
	 * test-only dependencies that should not be mentioned in the module-info.java). When executing test code that was
	 * compiled like this, corresponding "--add-reads" options need to be passed to the java runtime. This method
	 * returns the list of modules on the project's classpath for which this is the case.
	 *
	 * @return the set of module names
	 * @throws JavaModelException
	 *             when access to the classpath or module description of the given project fails.
	 * @since 3.14
	 */
	Set<String> determineModulesOfProjectsWithNonEmptyClasspath() throws JavaModelException;
}
