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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.core.BufferManager;
import org.eclipse.jdt.internal.core.CompilationUnit;
import org.eclipse.jdt.internal.core.DefaultWorkingCopyOwner;
import org.eclipse.jdt.internal.core.ExternalJavaProject;
import org.eclipse.jdt.internal.core.PackageFragment;
import org.eclipse.jdt.internal.core.PackageFragmentRoot;

/**
 * The owner of an {@link ICompilationUnit} handle in working copy mode.
 * An owner is used to identify a working copy and to create its buffer.
 * <p>
 * Clients should subclass this class to instantiate a working copy owner that is specific to their need and that
 * they can pass in to various APIs (e.g. {@link IType#resolveType(String, WorkingCopyOwner)}.
 * Clients can also override the default implementation of {@link #createBuffer(ICompilationUnit)}.
 * </p><p>
 * Note: even though this class has no abstract method, which means that it provides functional default behavior,
 * it is still an abstract class, as clients are intended to own their owner implementation.
 * </p>
 * @see ICompilationUnit#becomeWorkingCopy(org.eclipse.core.runtime.IProgressMonitor)
 * @see ICompilationUnit#discardWorkingCopy()
 * @see ICompilationUnit#getWorkingCopy(org.eclipse.core.runtime.IProgressMonitor)
 * @since 3.0
 */
public abstract class WorkingCopyOwner {

	/**
	 * Sets the buffer provider of the primary working copy owner. Note that even if the
	 * buffer provider is a working copy owner, only its <code>createBuffer(ICompilationUnit)</code>
	 * method is used by the primary working copy owner. It doesn't replace the internal primary
	 * working owner.
 	 * <p>
	 * This method is for internal use by the jdt-related plug-ins.
	 * Clients outside of the jdt should not reference this method.
	 * </p>
	 *
	 * @param primaryBufferProvider the primary buffer provider
	 */
	public static void setPrimaryBufferProvider(WorkingCopyOwner primaryBufferProvider) {
		DefaultWorkingCopyOwner.PRIMARY.primaryBufferProvider = primaryBufferProvider;
	}

	/**
	 * Creates a buffer for the given working copy.
	 * The new buffer will be initialized with the contents of the underlying file
	 * if and only if it was not already initialized by the compilation owner (a buffer is
	 * uninitialized if its content is <code>null</code>).
	 * <p>
	 * Note: This buffer will be associated to the working copy for its entire life-cycle. Another
	 * working copy on same unit but owned by a different owner would not share the same buffer
	 * unless its owner decided to implement such a sharing behaviour.
	 * </p>
	 *
	 * @param workingCopy the working copy of the buffer
	 * @return IBuffer the created buffer for the given working copy
	 * @see IBuffer
	 */
	public IBuffer createBuffer(ICompilationUnit workingCopy) {

		return BufferManager.createBuffer(workingCopy);
	}

	/**
	 * Returns the problem requestor used by a working copy of this working copy owner.
	 * <p>
	 * By default, no problem requestor is configured. Clients can override this
	 * method to provide a requestor.
	 * </p>
	 *
	 * @param workingCopy The problem requestor used for the given working copy.
	 * @return the problem requestor to be used by working copies of this working
	 * copy owner or <code>null</code> if no problem requestor is configured.
	 *
	 * @since 3.3
	 */
	public IProblemRequestor getProblemRequestor(ICompilationUnit workingCopy) {
		return null;
	}

	/**
	 * Returns the source of the compilation unit that defines the given type in
	 * the given package, or <code>null</code> if the type is unknown to this
	 * owner.
	 * <p>This method is called before the normal lookup (i.e. before looking
	 * at the project's classpath and before looking at the working copies of this
	 * owner.)</p>
	 * <p>This allows to provide types that are not normally available, or to hide
	 * types that would normally be available by returning an empty source for
	 * the given type and package.</p>
	 * <p>Example of use:
	 * <pre>
	 * WorkingCopyOwner owner = new WorkingCopyOwner() {
	 *   public String findSource(String typeName, String packageName) {
	 *     if ("to.be".equals(packageName) && "Generated".equals(typeName)) {
	 *       return
	 *         "package to.be;\n" +
	 *         "public class Generated {\n" +
	 *         "}";
	 *     }
	 *     return super.findSource(typeName, packageName);
	 *   }
	 *   public boolean isPackage(String[] pkg) {
	 *     switch (pkg.length) {
	 *     case 1:
	 *       return "to".equals(pkg[0]);
	 *     case 2:
	 *       return "to".equals(pkg[0]) && "be".equals(pkg[1]);
	 *     }
	 *     return false;
	 *   }
	 * };
	 * // Working copy on X.java with the following contents:
	 * //    public class X extends to.be.Generated {
	 * //    }
	 * ICompilationUnit workingCopy = ...
	 * ASTParser parser = ASTParser.newParser(AST.JLS3);
	 * parser.setSource(workingCopy);
	 * parser.setResolveBindings(true);
	 * parser.setWorkingCopyOwner(owner);
	 * CompilationUnit cu = (CompilationUnit) parser.createAST(null);
	 * assert cu.getProblems().length == 0;
	 * </pre>
	 *
	 * @param typeName the simple name of the type to lookup
	 * @param packageName the dot-separated name of the package of type
	 * @return the source of the compilation unit that defines the given type in
	 * the given package, or <code>null</code> if the type is unknown
	 * @see #isPackage(String[])
	 * @since 3.5
	 */
	public String findSource(String typeName, String packageName) {
		return null;
	}

	/**
	 * Returns whether the given package segments represent a package.
	 * <p>This method is called before the normal lookup (i.e. before looking
	 * at the project's classpath and before looking at the working copies of this
	 * owner.)</p>
	 * <p>This allows to provide packages that are not normally available.</p>
	 * <p>If <code>false</code> is returned, then normal lookup is used on
	 * this package.</p>
	 *
	 * @param pkg the segments of a package to lookup
	 * @return whether the given package segments represent a package.
	 * @see #findSource(String, String)
	 * @since 3.5
	 */
	public boolean isPackage(String[] pkg) {
		return false;
	}

	/**
	 * Returns a new working copy with the given name using this working copy owner to
	 * create its buffer.
	 * <p>
	 * This working copy always belongs to the default package in a package
	 * fragment root that corresponds to its Java project, and this Java project never exists.
	 * However this Java project has the given classpath that is used when resolving names
	 * in this working copy.
	 * </p><p>
	 * A DOM AST created using this working copy will have bindings resolved using the given
	 * classpath, and problem are reported to the given problem requestor.
	 * </p><p>
	 * <code>JavaCore#getOptions()</code> is used to create the DOM AST as it is not
	 * possible to set the options on the non-existing Java project.
	 * </p><p>
	 * When the working copy instance is created, an {@link IJavaElementDelta#ADDED added delta} is
	 * reported on this working copy.
	 * </p><p>
	 * Once done with the working copy, users of this method must discard it using
	 * {@link ICompilationUnit#discardWorkingCopy()}.
	 * </p><p>
	 * Note that when such working copy is committed, only its buffer is saved (see
	 * {@link IBuffer#save(IProgressMonitor, boolean)}) but no resource is created.
	 * </p><p>
	 * This method is not intended to be overriden by clients.
	 * </p>
	 *
	 * @param name the name of the working copy (e.g. "X.java")
	 * @param classpath the classpath used to resolve names in this working copy
	 * @param problemRequestor a requestor which will get notified of problems detected during
	 * 	reconciling as they are discovered. The requestor can be set to <code>null</code> indicating
	 * 	that the client is not interested in problems.
	 * @param monitor a progress monitor used to report progress while opening the working copy
	 * 	or <code>null</code> if no progress should be reported
	 * @throws JavaModelException if the contents of this working copy can
	 *   not be determined.
	 * @return a new working copy
	 * @see ICompilationUnit#becomeWorkingCopy(IProblemRequestor, IProgressMonitor)
	 * @since 3.2
	 *
	 * @deprecated Use {@link #newWorkingCopy(String, IClasspathEntry[], IProgressMonitor)} instead.
	 * 	Note that if this deprecated method is used, problems may be reported twice
	 * 	if the given requestor is not the same as the current working copy owner one.
	 */
	public final ICompilationUnit newWorkingCopy(String name, IClasspathEntry[] classpath, IProblemRequestor problemRequestor, IProgressMonitor monitor) throws JavaModelException {
		ExternalJavaProject project = new ExternalJavaProject(classpath);
		IPackageFragment parent = ((PackageFragmentRoot) project.getPackageFragmentRoot(project.getProject())).getPackageFragment(CharOperation.NO_STRINGS);
		CompilationUnit result = new CompilationUnit((PackageFragment) parent, name, this);
		result.becomeWorkingCopy(problemRequestor, monitor);
		return result;
	}

	/**
	 * Returns a new working copy with the given name using this working copy owner to
	 * create its buffer.
	 * <p>
	 * This working copy always belongs to the default package in a package
	 * fragment root that corresponds to its Java project, and this Java project never exists.
	 * However this Java project has the given classpath that is used when resolving names
	 * in this working copy.
	 * </p><p>
	 * If a DOM AST is created using this working copy, then given classpath will be used
	 *  if bindings need to be resolved. Problems will be reported to the problem requestor
	 * of the current working copy owner problem if it is not <code>null</code>.
	 * </p><p>
	 * Options used to create the DOM AST are got from {@link JavaCore#getOptions()}
	 * as it is not possible to set the options on a non-existing Java project.
	 * </p><p>
	 * When the working copy instance is created, an {@link IJavaElementDelta#ADDED added delta} is
	 * reported on this working copy.
	 * </p><p>
	 * Once done with the working copy, users of this method must discard it using
	 * {@link ICompilationUnit#discardWorkingCopy()}.
	 * </p><p>
	 * Note that when such working copy is committed, only its buffer is saved (see
	 * {@link IBuffer#save(IProgressMonitor, boolean)}) but no resource is created.
	 * </p><p>
	 * This method is not intended to be overriden by clients.
	 * </p>
	 *
	 * @param name the name of the working copy (e.g. "X.java")
	 * @param classpath the classpath used to resolve names in this working copy
	 * @param monitor a progress monitor used to report progress while opening the working copy
	 * 	or <code>null</code> if no progress should be reported
	 * @throws JavaModelException if the contents of this working copy can
	 *   not be determined.
	 * @return a new working copy
	 * @see ICompilationUnit#becomeWorkingCopy(IProgressMonitor)
	 *
	 * @since 3.3
	 */
	public final ICompilationUnit newWorkingCopy(String name, IClasspathEntry[] classpath, IProgressMonitor monitor) throws JavaModelException {
		ExternalJavaProject project = new ExternalJavaProject(classpath);
		IPackageFragment parent = ((PackageFragmentRoot) project.getPackageFragmentRoot(project.getProject())).getPackageFragment(CharOperation.NO_STRINGS);
		CompilationUnit result = new CompilationUnit((PackageFragment) parent, name, this);
		result.becomeWorkingCopy(getProblemRequestor(result), monitor);
		return result;
	}

}
