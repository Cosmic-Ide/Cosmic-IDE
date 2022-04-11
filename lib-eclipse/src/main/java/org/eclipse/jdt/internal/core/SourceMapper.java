/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
 *     Kelly Campbell <kellyc@google.com> - Hangs in SourceMapper during java proposals - https://bugs.eclipse.org/bugs/show_bug.cgi?id=281575
 *     Stephan Herrmann - Contribution for Bug 380048 - error popup when navigating to source files
 *******************************************************************************/
package org.eclipse.jdt.internal.core;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IModuleDescription;
import org.eclipse.jdt.core.IOrdinaryClassFile;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeParameter;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.SourceRange;
import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.codeassist.impl.Keywords;
import org.eclipse.jdt.internal.compiler.IProblemFactory;
import org.eclipse.jdt.internal.compiler.ISourceElementRequestor;
import org.eclipse.jdt.internal.compiler.SourceElementParser;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.ImportReference;
import org.eclipse.jdt.internal.compiler.env.IBinaryType;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.lookup.TypeConstants;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblemFactory;
import org.eclipse.jdt.internal.compiler.util.JRTUtil;
import org.eclipse.jdt.internal.compiler.util.SuffixConstants;
import org.eclipse.jdt.internal.compiler.util.Util;
import org.eclipse.jdt.internal.core.util.ReferenceInfoAdapter;

/**
 * A SourceMapper maps source code in a ZIP file to binary types or
 * binary modules in a JAR. The SourceMapper uses the fuzzy parser
 * to identify source fragments in a .java file, and attempts to match
 * the source code with children in a binary type.
 * Since a module has no children in the Java Model no such matching
 * happens in that case.
 * A SourceMapper is associated with a JarPackageFragment by an AttachSourceOperation.
 *
 * @see org.eclipse.jdt.internal.core.JarPackageFragment
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class SourceMapper
	extends ReferenceInfoAdapter
	implements ISourceElementRequestor, SuffixConstants {

	public static class LocalVariableElementKey {
		String parent;
		String name;

		public LocalVariableElementKey(IJavaElement method, String name) {
			StringBuffer buffer = new StringBuffer();
			buffer
				.append(method.getParent().getHandleIdentifier())
				.append('#')
				.append(method.getElementName())
				.append('(');
			if (method.getElementType() == IJavaElement.METHOD) {
				String[] parameterTypes = ((IMethod) method).getParameterTypes();
				for (int i = 0, max = parameterTypes.length; i < max; i++) {
					if (i > 0) {
						buffer.append(',');
					}
					buffer.append(Signature.getSignatureSimpleName(parameterTypes[i]));
				}
			}
			buffer.append(')');
			this.parent = String.valueOf(buffer);
			this.name = name;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((this.name == null) ? 0 : this.name.hashCode());
			result = prime * result + ((this.parent == null) ? 0 : this.parent.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			LocalVariableElementKey other = (LocalVariableElementKey) obj;
			if (this.name == null) {
				if (other.name != null)
					return false;
			} else if (!this.name.equals(other.name))
				return false;
			if (this.parent == null) {
				if (other.parent != null)
					return false;
			} else if (!this.parent.equals(other.parent))
				return false;
			return true;
		}
		@Override
		public String toString() {
			StringBuffer buffer = new StringBuffer();
			buffer.append('(').append(this.parent).append('.').append(this.name).append(')');
			return String.valueOf(buffer);
		}
	}

	public static boolean VERBOSE = false;
	/**
	 * Specifies the location of the package fragment roots within
	 * the zip (empty specifies the default root). <code>null</code> is
	 * not a valid root path.
	 */
	protected ArrayList rootPaths;

	/**
	 * The binary type or module source is being mapped for
	 */
	protected NamedMember binaryTypeOrModule;

	/**
	 * The location of the zip file containing source.
	 */
	protected IPath sourcePath;
	/**
	 * Specifies the location of the package fragment root within
	 * the zip (empty specifies the default root). <code>null</code> is
	 * not a valid root path.
	 */
	protected String rootPath = ""; //$NON-NLS-1$

	/**
	 * Table that maps a binary method to its parameter names.
	 * Keys are the method handles, entries are <code>char[][]</code>.
	 */
	protected HashMap parameterNames;

	/**
	 * Table that maps a binary element to its <code>SourceRange</code>s.
	 * Keys are the element handles, entries are <code>SourceRange[]</code> which
	 * is a two element array; the first being source range, the second
	 * being name range.
	 */
	protected HashMap sourceRanges;

	/*
	 * A map from IJavaElement to String[]
	 */
	protected HashMap categories;

	/**
	 * Table that contains all source ranges for local variables.
	 * Keys are the special local variable elements, entries are <code>char[][]</code>.
	 */
	protected HashMap parametersRanges;

	/**
	 * Set that contains all final local variables.
	 */
	protected HashSet finalParameters;

	/**
	 * The unknown source range {-1, 0}
	 */
	public static final SourceRange UNKNOWN_RANGE = new SourceRange(-1, 0);

	/**
	 * The position within the source of the start of the
	 * current member element, or -1 if we are outside a member.
	 */
	protected int[] memberDeclarationStart;
	/**
	 * The <code>SourceRange</code> of the name of the current member element.
	 */
	protected SourceRange[] memberNameRange;
	/**
	 * The name of the current member element.
	 */
	protected String[] memberName;

	/**
	 * The parameter names for the current member method element.
	 */
	protected char[][][] methodParameterNames;

	/**
	 * The parameter types for the current member method element.
	 */
	protected char[][][] methodParameterTypes;


	/**
	 * The element searched for
	 */
	protected IJavaElement searchedElement;

	/**
	 * imports references (keyed by binaryTypeOrModule)
	 */
	private HashMap importsTable;
	private HashMap importsCounterTable;

	/**
	 * Enclosing type information
	 */
	IType[] types;
	int[] typeDeclarationStarts;
	SourceRange[] typeNameRanges;
	int[] typeModifiers;
	int typeDepth;

	/**
	 * Module information
	 */
	SourceRange moduleNameRange;
	int moduleDeclarationStart;
	int moduleModifiers;

	/**
	 *  Anonymous counter in case we want to map the source of an anonymous class.
	 */
	int anonymousCounter;
	int anonymousClassName;

	String encoding;
	String defaultEncoding;
	/**
	 *Options to be used
	 */
	Map options;

	/**
	 * Use to handle root paths inference
	 */
	private boolean areRootPathsComputed;

	public SourceMapper() {
		this.areRootPathsComputed = false;
	}

	public SourceMapper(IPath sourcePath, String rootPath, Map options) {
		this(sourcePath, rootPath, options, null);
	}
	/**
	 * Creates a <code>SourceMapper</code> that locates source in the zip file
	 * at the given location in the specified package fragment root.
	 */
	public SourceMapper(IPath sourcePath, String rootPath, Map options, String encoding) {
		this.areRootPathsComputed = false;
		this.options = options;
		this.encoding = encoding;
		try {
			this.defaultEncoding = ResourcesPlugin.getWorkspace().getRoot().getDefaultCharset();
		} catch (CoreException e) {
			// use no encoding
		}
		if (rootPath != null) {
			this.rootPath = rootPath;
			this.rootPaths = new ArrayList();
			this.rootPaths.add(rootPath);
		}
		this.sourcePath = sourcePath;
		this.sourceRanges = new HashMap();
		this.parametersRanges = new HashMap();
		this.parameterNames = new HashMap();
		this.importsTable = new HashMap();
		this.importsCounterTable = new HashMap();
	}

	/**
	 * @see ISourceElementRequestor
	 */
	@Override
	public void acceptImport(
			int declarationStart,
			int declarationEnd,
			int nameStart,
			int nameEnd,
			char[][] tokens,
			boolean onDemand,
			int modifiers) {
		char[][] imports = (char[][]) this.importsTable.get(this.binaryTypeOrModule);
		int importsCounter;
		if (imports == null) {
			imports = new char[5][];
			importsCounter = 0;
		} else {
			importsCounter = ((Integer) this.importsCounterTable.get(this.binaryTypeOrModule)).intValue();
		}
		if (imports.length == importsCounter) {
			System.arraycopy(
				imports,
				0,
				(imports = new char[importsCounter * 2][]),
				0,
				importsCounter);
		}
		char[] name = CharOperation.concatWith(tokens, '.');
		if (onDemand) {
			int nameLength = name.length;
			System.arraycopy(name, 0, (name = new char[nameLength + 2]), 0, nameLength);
			name[nameLength] = '.';
			name[nameLength + 1] = '*';
		}
		if(Flags.isStatic(modifiers)) {
			name = CharOperation.concatAll(Keywords.STATIC, name, ' ');
		}
		imports[importsCounter++] = name;
		this.importsTable.put(this.binaryTypeOrModule, imports);
		this.importsCounterTable.put(this.binaryTypeOrModule, Integer.valueOf(importsCounter));
	}

	/**
	 * @see ISourceElementRequestor
	 */
	@Override
	public void acceptLineSeparatorPositions(int[] positions) {
		//do nothing
	}

	/**
	 * @see ISourceElementRequestor
	 */
	@Override
	public void acceptPackage(ImportReference importReference) {
		//do nothing
	}

	/**
	 * @see ISourceElementRequestor
	 */
	@Override
	public void acceptProblem(CategorizedProblem problem) {
		//do nothing
	}

	private void addCategories(IJavaElement element, char[][] elementCategories) {
		if (elementCategories == null) return;
		if (this.categories == null)
			this.categories = new HashMap();
		this.categories.put(element, CharOperation.toStrings(elementCategories));
	}

	/**
	 * Closes this <code>SourceMapper</code>'s zip file. Once this is done, this
	 * <code>SourceMapper</code> cannot be used again.
	 */
	public void close() {
		this.sourceRanges = null;
		this.parameterNames = null;
		this.parametersRanges = null;
		this.finalParameters = null;
	}

	/**
	 * NOT API, public only for access by Unit tests.
	 * Converts these type names to unqualified signatures. This needs to be done in order to be consistent
	 * with the way the source range is retrieved.
	 * @see SourceMapper#getUnqualifiedMethodHandle
	 * @see Signature
	 */
	public String[] convertTypeNamesToSigs(char[][] typeNames) {
		if (typeNames == null)
			return CharOperation.NO_STRINGS;
		int n = typeNames.length;
		if (n == 0)
			return CharOperation.NO_STRINGS;
		String[] typeSigs = new String[n];
		for (int i = 0; i < n; ++i) {
			char[] typeSig = Signature.createCharArrayTypeSignature(typeNames[i], false);

			// transforms signatures that contains a qualification into unqualified signatures
			// e.g. "QX<+QMap.Entry;>;" becomes "QX<+QEntry;>;"
			StringBuffer simpleTypeSig = null;
			int start = 0;
			int dot = -1;
			int length = typeSig.length;
			for (int j = 0; j < length; j++) {
				switch (typeSig[j]) {
					case Signature.C_UNRESOLVED:
						if (simpleTypeSig != null)
							simpleTypeSig.append(typeSig, start, j-start);
						start = j;
						break;
					case Signature.C_DOT:
						dot = j;
						break;
					case Signature.C_GENERIC_START:
						int matchingEnd = findMatchingGenericEnd(typeSig, j+1);
						if (matchingEnd > 0 && matchingEnd+1 < length && typeSig[matchingEnd+1] == Signature.C_DOT) {
							// found Head<Param>.Tail -> discard everything except Tail
							if (simpleTypeSig == null)
								simpleTypeSig = new StringBuffer().append(typeSig, 0, start);
							simpleTypeSig.append(Signature.C_UNRESOLVED);
							start = j = matchingEnd+2;
							break;
						}
						//$FALL-THROUGH$
					case Signature.C_NAME_END:
						if (dot > start) {
							if (simpleTypeSig == null)
								simpleTypeSig = new StringBuffer().append(typeSig, 0, start);
							simpleTypeSig.append(Signature.C_UNRESOLVED);
							simpleTypeSig.append(typeSig, dot+1, j-dot-1);
							start = j;
						}
						break;
				}
			}
			if (simpleTypeSig == null) {
				typeSigs[i] = new String(typeSig);
			} else {
				simpleTypeSig.append(typeSig, start, length-start);
				typeSigs[i] = simpleTypeSig.toString();
			}
		}
		return typeSigs;
	}

	private int findMatchingGenericEnd(char[] sig, int start) {
		int nesting = 0;
		int length = sig.length;
		for (int i=start; i < length; i++) {
			switch (sig[i]) {
				case Signature.C_GENERIC_START:
					nesting++;
					break;
				case Signature.C_GENERIC_END:
					if (nesting == 0)
						return i;
					nesting--;
					break;
			}
		}
		return -1;
	}

	static class JrtPackageNamesAdderVisitor implements JRTUtil.JrtFileVisitor<java.nio.file.Path> {

		public final HashSet firstLevelPackageNames;
		final IPackageFragmentRoot root;
		public String sourceLevel = null;
		public String complianceLevel = null;
		public boolean containsADefaultPackage;
		public boolean containsJavaSource;

		JrtPackageNamesAdderVisitor(HashSet firstLevelPackageNames, String sourceLevel, String complianceLevel,
				boolean containsADefaultPackage, boolean containsJavaSource, IPackageFragmentRoot root) {
			this.firstLevelPackageNames = firstLevelPackageNames;
			this.root = root;
			this.sourceLevel = sourceLevel;
			this.complianceLevel = complianceLevel;
			this.containsADefaultPackage = containsADefaultPackage;
			this.containsJavaSource = containsJavaSource;
		}

		@Override
		public FileVisitResult visitPackage(java.nio.file.Path dir, java.nio.file.Path mod, BasicFileAttributes attrs) throws IOException {
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFile(java.nio.file.Path file, java.nio.file.Path mod, BasicFileAttributes attrs) throws IOException {
			String entryName = file.toString();
			if (Util.isClassFileName(entryName)) {
				int index = entryName.indexOf('/');
				if (index != -1) {
					String firstLevelPackageName = entryName.substring(0, index);
					if (!this.firstLevelPackageNames.contains(firstLevelPackageName)) {
						if (this.sourceLevel == null) {
							IJavaProject project = this.root.getJavaProject();
							this.sourceLevel = project.getOption(JavaCore.COMPILER_SOURCE, true);
							this.complianceLevel = project.getOption(JavaCore.COMPILER_COMPLIANCE, true);
						}
						IStatus status = JavaConventions.validatePackageName(firstLevelPackageName, this.sourceLevel, this.complianceLevel);
						if (status.isOK() || status.getSeverity() == IStatus.WARNING) {
							this.firstLevelPackageNames.add(firstLevelPackageName);
						}
					}
				} else {
					this.containsADefaultPackage = true;
				}
			} else if (!this.containsJavaSource && org.eclipse.jdt.internal.core.util.Util.isJavaLikeFileName(entryName)) {
				this.containsJavaSource = true;
			}
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitModule(java.nio.file.Path path, String name) throws IOException {
			return FileVisitResult.CONTINUE;
		}
	}
	private synchronized void computeAllRootPaths(IJavaElement typeOrModule) {
		if (this.areRootPathsComputed) {
			return;
		}
		IPackageFragmentRoot root = (IPackageFragmentRoot) typeOrModule.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
		IPath pkgFragmentRootPath = root.getPath();
		final HashSet tempRoots = new HashSet();
		long time = 0;
		if (VERBOSE) {
			System.out.println("compute all root paths for " + root.getElementName()); //$NON-NLS-1$
			time = System.currentTimeMillis();
		}
		final HashSet firstLevelPackageNames = new HashSet();
		boolean containsADefaultPackage = false;
		boolean containsJavaSource = !pkgFragmentRootPath.equals(this.sourcePath); // used to optimize zip file reading only if source path and root path are equals, otherwise assume that attachment contains Java source

		String sourceLevel = null;
		String complianceLevel = null;
		if (Util.isJrt(pkgFragmentRootPath.toOSString())) {
			try {
				JrtPackageNamesAdderVisitor jrtPackageNamesAdderVisitor = new JrtPackageNamesAdderVisitor(firstLevelPackageNames,
						sourceLevel, complianceLevel, containsADefaultPackage, containsJavaSource, root);
				org.eclipse.jdt.internal.compiler.util.JRTUtil.walkModuleImage(root.getPath().toFile(), jrtPackageNamesAdderVisitor, JRTUtil.NOTIFY_FILES);
				sourceLevel = jrtPackageNamesAdderVisitor.sourceLevel;
				complianceLevel = jrtPackageNamesAdderVisitor.complianceLevel;
				containsADefaultPackage = jrtPackageNamesAdderVisitor.containsADefaultPackage;
				containsJavaSource = jrtPackageNamesAdderVisitor.containsJavaSource;
			} catch (IOException e) {
				// We are not reading any specific file, so, move on for now
				if (VERBOSE) {
					e.printStackTrace();
				}
			}
		} else if (root.isArchive()) {
			JavaModelManager manager = JavaModelManager.getJavaModelManager();
			ZipFile zip = null;
			try {
				zip = manager.getZipFile(pkgFragmentRootPath);
				for (Enumeration entries = zip.entries(); entries.hasMoreElements(); ) {
					ZipEntry entry = (ZipEntry) entries.nextElement();
					String entryName = entry.getName();
					if (!entry.isDirectory()) {
						if (Util.isClassFileName(entryName)) {
							int index = entryName.indexOf('/');
							if (index != -1) {
								String firstLevelPackageName = entryName.substring(0, index);
								if (!firstLevelPackageNames.contains(firstLevelPackageName)) {
									if (sourceLevel == null) {
										IJavaProject project = root.getJavaProject();
										sourceLevel = project.getOption(JavaCore.COMPILER_SOURCE, true);
										complianceLevel = project.getOption(JavaCore.COMPILER_COMPLIANCE, true);
									}
									IStatus status = JavaConventions.validatePackageName(firstLevelPackageName, sourceLevel, complianceLevel);
									if (status.isOK() || status.getSeverity() == IStatus.WARNING) {
										firstLevelPackageNames.add(firstLevelPackageName);
									}
								}
							} else {
								containsADefaultPackage = true;
							}
						} else if (!containsJavaSource && org.eclipse.jdt.internal.core.util.Util.isJavaLikeFileName(entryName)) {
							containsJavaSource = true;
						}
					}
				}
			} catch (CoreException e) {
				// ignore
			} finally {
				manager.closeZipFile(zip); // handle null case
			}
		} else {
			Object target = JavaModel.getTarget(root.getPath(), true);
			if (target instanceof IResource) {
				IResource resource = (IResource) target;
				if (resource instanceof IContainer) {
					try {
						IResource[] members = ((IContainer) resource).members();
						for (int i = 0, max = members.length; i < max; i++) {
							IResource member = members[i];
							String resourceName = member.getName();
							if (member.getType() == IResource.FOLDER) {
								if (sourceLevel == null) {
									IJavaProject project = root.getJavaProject();
									sourceLevel = project.getOption(JavaCore.COMPILER_SOURCE, true);
									complianceLevel = project.getOption(JavaCore.COMPILER_COMPLIANCE, true);
								}
								IStatus status = JavaConventions.validatePackageName(resourceName, sourceLevel, complianceLevel);
								if (status.isOK() || status.getSeverity() == IStatus.WARNING) {
									firstLevelPackageNames.add(resourceName);
								}
							} else if (Util.isClassFileName(resourceName)) {
								containsADefaultPackage = true;
							} else if (!containsJavaSource && org.eclipse.jdt.internal.core.util.Util.isJavaLikeFileName(resourceName)) {
								containsJavaSource = true;
							}
						}
					} catch (CoreException e) {
						// ignore
					}
				}
			}
		}

		if (containsJavaSource) { // no need to read source attachment if it contains no Java source (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=190840 )
			Object target = JavaModel.getTarget(this.sourcePath, true);
			if (target instanceof IContainer) {
				IContainer folder = (IContainer)target;
				computeRootPath(folder, firstLevelPackageNames, containsADefaultPackage, tempRoots, folder.getFullPath().segmentCount()/*if external folder, this is the linked folder path*/);
			} else {
				JavaModelManager manager = JavaModelManager.getJavaModelManager();
				ZipFile zip = null;
				try {
					zip = manager.getZipFile(this.sourcePath);
					for (Enumeration entries = zip.entries(); entries.hasMoreElements(); ) {
						ZipEntry entry = (ZipEntry) entries.nextElement();
						String entryName;
						if (!entry.isDirectory() && org.eclipse.jdt.internal.core.util.Util.isJavaLikeFileName(entryName = entry.getName())) {
							IPath path = new Path(entryName);
							int segmentCount = path.segmentCount();
							if (segmentCount > 1) {
								for (int i = 0, max = path.segmentCount() - 1; i < max; i++) {
									if (firstLevelPackageNames.contains(path.segment(i))) {
										tempRoots.add(path.uptoSegment(i));
										// don't break here as this path could contain other first level package names (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=74014)
									}
									if (i == max - 1 && containsADefaultPackage) {
										tempRoots.add(path.uptoSegment(max));
									}
								}
							} else if (containsADefaultPackage) {
								tempRoots.add(new Path("")); //$NON-NLS-1$
							}
						}
					}
				} catch (CoreException e) {
					// ignore
				} finally {
					manager.closeZipFile(zip); // handle null case
				}
			}
		}
		int size = tempRoots.size();
		if (this.rootPaths != null) {
			for (Iterator iterator = this.rootPaths.iterator(); iterator.hasNext(); ) {
				tempRoots.add(new Path((String) iterator.next()));
			}
			this.rootPaths.clear();
		} else {
			this.rootPaths = new ArrayList(size);
		}
		size = tempRoots.size();
		if (size > 0) {
			ArrayList sortedRoots = new ArrayList(tempRoots);
			if (size > 1) {
				Collections.sort(sortedRoots, new Comparator() {
					@Override
					public int compare(Object o1, Object o2) {
						IPath path1 = (IPath) o1;
						IPath path2 = (IPath) o2;
						return path1.segmentCount() - path2.segmentCount();
					}
				});
			}
			for (Iterator iter = sortedRoots.iterator(); iter.hasNext();) {
				IPath path = (IPath) iter.next();
				this.rootPaths.add(path.toString());
			}
		}
		this.areRootPathsComputed = true;
		if (VERBOSE) {
			System.out.println("Spent " + (System.currentTimeMillis() - time) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
			System.out.println("Found " + size + " root paths");	//$NON-NLS-1$ //$NON-NLS-2$
			int i = 0;
			for (Iterator iterator = this.rootPaths.iterator(); iterator.hasNext();) {
				System.out.println("root[" + i + "]=" + ((String) iterator.next()));//$NON-NLS-1$ //$NON-NLS-2$
				i++;
			}
		}
	}

	private void computeRootPath(IContainer container, HashSet firstLevelPackageNames, boolean hasDefaultPackage, Set set, int sourcePathSegmentCount) {
		try {
			IResource[] resources = container.members();
			for (int i = 0, max = resources.length; i < max; i++) {
				IResource resource = resources[i];
				if (resource.getType() == IResource.FOLDER) {
					if (firstLevelPackageNames.contains(resource.getName())) {
						IPath fullPath = container.getFullPath();
						IPath rootPathEntry = fullPath.removeFirstSegments(sourcePathSegmentCount).setDevice(null);
						if (rootPathEntry.segmentCount() >= 1) {
							set.add(rootPathEntry);
						}
						computeRootPath((IFolder) resource, firstLevelPackageNames, hasDefaultPackage, set, sourcePathSegmentCount);
					} else {
						computeRootPath((IFolder) resource, firstLevelPackageNames, hasDefaultPackage, set, sourcePathSegmentCount);
					}
				}
				if (i == max - 1 && hasDefaultPackage) {
					// check if one member is a .java file
					boolean hasJavaSourceFile = false;
					for (int j = 0; j < max; j++) {
						if (org.eclipse.jdt.internal.core.util.Util.isJavaLikeFileName(resources[i].getName())) {
							hasJavaSourceFile = true;
							break;
						}
					}
					if (hasJavaSourceFile) {
						IPath fullPath = container.getFullPath();
						IPath rootPathEntry = fullPath.removeFirstSegments(sourcePathSegmentCount).setDevice(null);
						set.add(rootPathEntry);
					}
				}
			}
		} catch (CoreException e) {
			// ignore
			e.printStackTrace();
		}
	}

	/**
	 * @see ISourceElementRequestor
	 */
	@Override
	public void enterType(TypeInfo typeInfo) {

		this.typeDepth++;
		if (this.typeDepth == this.types.length) { // need to grow
			System.arraycopy(
				this.types,
				0,
				this.types = new IType[this.typeDepth * 2],
				0,
				this.typeDepth);
			System.arraycopy(
				this.typeNameRanges,
				0,
				this.typeNameRanges = new SourceRange[this.typeDepth * 2],
				0,
				this.typeDepth);
			System.arraycopy(
				this.typeDeclarationStarts,
				0,
				this.typeDeclarationStarts = new int[this.typeDepth * 2],
				0,
				this.typeDepth);
			System.arraycopy(
				this.memberName,
				0,
				this.memberName = new String[this.typeDepth * 2],
				0,
				this.typeDepth);
			System.arraycopy(
				this.memberDeclarationStart,
				0,
				this.memberDeclarationStart = new int[this.typeDepth * 2],
				0,
				this.typeDepth);
			System.arraycopy(
				this.memberNameRange,
				0,
				this.memberNameRange = new SourceRange[this.typeDepth * 2],
				0,
				this.typeDepth);
			System.arraycopy(
				this.methodParameterTypes,
				0,
				this.methodParameterTypes = new char[this.typeDepth * 2][][],
				0,
				this.typeDepth);
			System.arraycopy(
				this.methodParameterNames,
				0,
				this.methodParameterNames = new char[this.typeDepth * 2][][],
				0,
				this.typeDepth);
			System.arraycopy(
				this.typeModifiers,
				0,
				this.typeModifiers = new int[this.typeDepth * 2],
				0,
				this.typeDepth);
		}
		if (typeInfo.name.length == 0) {
			this.anonymousCounter++;
			if (this.anonymousCounter == this.anonymousClassName) {
				this.types[this.typeDepth] = getType(this.binaryTypeOrModule.getElementName());
			} else {
				this.types[this.typeDepth] = getType(new String(typeInfo.name));
			}
		} else {
			this.types[this.typeDepth] = getType(new String(typeInfo.name));
		}
		this.typeNameRanges[this.typeDepth] =
			new SourceRange(typeInfo.nameSourceStart, typeInfo.nameSourceEnd - typeInfo.nameSourceStart + 1);
		this.typeDeclarationStarts[this.typeDepth] = typeInfo.declarationStart;

		IType currentType = this.types[this.typeDepth];

		// type parameters
		if (typeInfo.typeParameters != null) {
			for (int i = 0, length = typeInfo.typeParameters.length; i < length; i++) {
				TypeParameterInfo typeParameterInfo = typeInfo.typeParameters[i];
				ITypeParameter typeParameter = currentType.getTypeParameter(new String(typeParameterInfo.name));
				setSourceRange(
					typeParameter,
					new SourceRange(
						typeParameterInfo.declarationStart,
						typeParameterInfo.declarationEnd - typeParameterInfo.declarationStart + 1),
					new SourceRange(
						typeParameterInfo.nameSourceStart,
						typeParameterInfo.nameSourceEnd - typeParameterInfo.nameSourceStart + 1));
			}
		}

		// type modifiers
		this.typeModifiers[this.typeDepth] = typeInfo.modifiers;

		// categories
		addCategories(currentType, typeInfo.categories);
	}

	@Override
	public void enterModule(ModuleInfo moduleInfo) {
		this.moduleNameRange =
			new SourceRange(moduleInfo.nameSourceStart, moduleInfo.nameSourceEnd - moduleInfo.nameSourceStart + 1);
		this.moduleDeclarationStart = moduleInfo.declarationStart;


		// module type modifiers
		this.moduleModifiers = moduleInfo.modifiers;

		if (this.binaryTypeOrModule instanceof IModuleDescription) {
			// categories
			addCategories(this.binaryTypeOrModule, moduleInfo.categories);
		}
	}

	@Override
	public void exitModule(int declarationEnd) {
		setSourceRange(
			this.binaryTypeOrModule,
			new SourceRange(
				this.moduleDeclarationStart,
				declarationEnd - this.moduleDeclarationStart + 1),
			this.moduleNameRange);
	}

	/**
	 * @see ISourceElementRequestor
	 */
	@Override
	public void enterCompilationUnit() {
		// do nothing
	}

	/**
	 * @see ISourceElementRequestor
	 */
	@Override
	public void enterConstructor(MethodInfo methodInfo) {
		enterAbstractMethod(methodInfo);
	}

	/**
	 * @see ISourceElementRequestor
	 */
	@Override
	public void enterField(FieldInfo fieldInfo) {
		if (this.typeDepth >= 0) {
			this.memberDeclarationStart[this.typeDepth] = fieldInfo.declarationStart;
			this.memberNameRange[this.typeDepth] =
				new SourceRange(fieldInfo.nameSourceStart, fieldInfo.nameSourceEnd - fieldInfo.nameSourceStart + 1);
			String fieldName = new String(fieldInfo.name);
			this.memberName[this.typeDepth] = fieldName;

			// categories
			IType currentType = this.types[this.typeDepth];
			IField field = currentType.getField(fieldName);
			addCategories(field, fieldInfo.categories);
		}
	}

	/**
	 * @see ISourceElementRequestor
	 */
	@Override
	public void enterInitializer(
		int declarationSourceStart,
		int modifiers) {
		//do nothing
	}

	/**
	 * @see ISourceElementRequestor
	 */
	@Override
	public void enterMethod(MethodInfo methodInfo) {
		enterAbstractMethod(methodInfo);
	}
	private void enterAbstractMethod(MethodInfo methodInfo) {
		if (this.typeDepth >= 0) {
			this.memberName[this.typeDepth] = new String(methodInfo.name);
			this.memberNameRange[this.typeDepth] =
				new SourceRange(methodInfo.nameSourceStart, methodInfo.nameSourceEnd - methodInfo.nameSourceStart + 1);
			this.memberDeclarationStart[this.typeDepth] = methodInfo.declarationStart;
			IType currentType = this.types[this.typeDepth];
			int currenTypeModifiers = this.typeModifiers[this.typeDepth];
			char[][] parameterTypes = methodInfo.parameterTypes;
			if (methodInfo.isConstructor && currentType.getDeclaringType() != null && !Flags.isStatic(currenTypeModifiers)) {
				IType declaringType = currentType.getDeclaringType();
				String declaringTypeName = declaringType.getElementName();
				if (declaringTypeName.length() == 0) {
					IOrdinaryClassFile classFile = declaringType.getClassFile();
					int length = parameterTypes != null ? parameterTypes.length : 0;
					char[][] newParameterTypes = new char[length+1][];
					declaringTypeName = classFile.getElementName();
					declaringTypeName = declaringTypeName.substring(0, declaringTypeName.indexOf('.'));
					newParameterTypes[0] = declaringTypeName.toCharArray();
					if (length != 0) {
						System.arraycopy(parameterTypes, 0, newParameterTypes, 1, length);
					}
					this.methodParameterTypes[this.typeDepth] = newParameterTypes;
				} else {
					int length = parameterTypes != null ? parameterTypes.length : 0;
					char[][] newParameterTypes = new char[length+1][];
					newParameterTypes[0] = declaringTypeName.toCharArray();
					if (length != 0) {
						System.arraycopy(parameterTypes, 0, newParameterTypes, 1, length);
					}
					this.methodParameterTypes[this.typeDepth] = newParameterTypes;
				}
			} else {
				this.methodParameterTypes[this.typeDepth] = parameterTypes;
			}
			this.methodParameterNames[this.typeDepth] = methodInfo.parameterNames;

			IMethod method = currentType.getMethod(
					this.memberName[this.typeDepth],
					convertTypeNamesToSigs(this.methodParameterTypes[this.typeDepth]));

			// type parameters
			if (methodInfo.typeParameters != null) {
				for (int i = 0, length = methodInfo.typeParameters.length; i < length; i++) {
					TypeParameterInfo typeParameterInfo = methodInfo.typeParameters[i];
					ITypeParameter typeParameter = method.getTypeParameter(new String(typeParameterInfo.name));
					setSourceRange(
						typeParameter,
						new SourceRange(
							typeParameterInfo.declarationStart,
							typeParameterInfo.declarationEnd - typeParameterInfo.declarationStart + 1),
						new SourceRange(
							typeParameterInfo.nameSourceStart,
							typeParameterInfo.nameSourceEnd - typeParameterInfo.nameSourceStart + 1));
				}
			}
			// parameters infos
			if (methodInfo.parameterInfos != null) {
				for (int i = 0, length = methodInfo.parameterInfos.length; i < length; i++) {
					ParameterInfo parameterInfo = methodInfo.parameterInfos[i];
					LocalVariableElementKey key = new LocalVariableElementKey(method, new String(parameterInfo.name));
					SourceRange[] allRanges = new SourceRange[] {
						new SourceRange(
							parameterInfo.declarationStart,
							parameterInfo.declarationEnd - parameterInfo.declarationStart + 1),
						new SourceRange(
							parameterInfo.nameSourceStart,
							parameterInfo.nameSourceEnd - parameterInfo.nameSourceStart + 1)
					};
					this.parametersRanges.put(
						key,
						allRanges);
					if (parameterInfo.modifiers != 0) {
						if (this.finalParameters == null) {
							this.finalParameters = new HashSet();
						}
						this.finalParameters.add(key);
					}
				}
			}

			// categories
			addCategories(method, methodInfo.categories);
		}
	}

	/**
	 * @see ISourceElementRequestor
	 */
	@Override
	public void exitType(int declarationEnd) {
		if (this.typeDepth >= 0) {
			IType currentType = this.types[this.typeDepth];
			setSourceRange(
				currentType,
				new SourceRange(
					this.typeDeclarationStarts[this.typeDepth],
					declarationEnd - this.typeDeclarationStarts[this.typeDepth] + 1),
				this.typeNameRanges[this.typeDepth]);
			this.typeDepth--;
		}
	}

	/**
	 * @see ISourceElementRequestor
	 */
	@Override
	public void exitCompilationUnit(int declarationEnd) {
		//do nothing
	}

	/**
	 * @see ISourceElementRequestor
	 */
	@Override
	public void exitConstructor(int declarationEnd) {
		exitAbstractMethod(declarationEnd);
	}

	/**
	 * @see ISourceElementRequestor
	 */
	@Override
	public void exitField(int initializationStart, int declarationEnd, int declarationSourceEnd) {
		if (this.typeDepth >= 0) {
			IType currentType = this.types[this.typeDepth];
			setSourceRange(
				currentType.getField(this.memberName[this.typeDepth]),
				new SourceRange(
					this.memberDeclarationStart[this.typeDepth],
					declarationEnd - this.memberDeclarationStart[this.typeDepth] + 1),
				this.memberNameRange[this.typeDepth]);
		}
	}
	/**
	 * @see ISourceElementRequestor
	 */
	@Override
	public void exitRecordComponent(int declarationEnd, int declarationSourceEnd) {
		if (this.typeDepth >= 0) {
			IType currentType = this.types[this.typeDepth];
			setSourceRange(
				currentType.getRecordComponent(this.memberName[this.typeDepth]),
				new SourceRange(
					this.memberDeclarationStart[this.typeDepth],
					declarationEnd - this.memberDeclarationStart[this.typeDepth] + 1),
				this.memberNameRange[this.typeDepth]);
		}
	}

	/**
	 * @see ISourceElementRequestor
	 */
	@Override
	public void exitInitializer(int declarationEnd) {
		// implements abstract method
	}

	/**
	 * @see ISourceElementRequestor
	 */
	@Override
	public void exitMethod(int declarationEnd, Expression defaultValue) {
		exitAbstractMethod(declarationEnd);
	}
	private void exitAbstractMethod(int declarationEnd) {
		if (this.typeDepth >= 0) {
			IType currentType = this.types[this.typeDepth];
			SourceRange sourceRange =
				new SourceRange(
					this.memberDeclarationStart[this.typeDepth],
					declarationEnd - this.memberDeclarationStart[this.typeDepth] + 1);
			IMethod method = currentType.getMethod(
					this.memberName[this.typeDepth],
					convertTypeNamesToSigs(this.methodParameterTypes[this.typeDepth]));
			setSourceRange(
				method,
				sourceRange,
				this.memberNameRange[this.typeDepth]);
			setMethodParameterNames(
				method,
				this.methodParameterNames[this.typeDepth]);
		}
	}

	/**
	 * Locates and returns source code for the given (binary) type, in this
	 * SourceMapper's ZIP file, or returns <code>null</code> if source
	 * code cannot be found.
	 */
	public char[] findSource(IType type, IBinaryType info) {
		if (!type.isBinary()) {
			return null;
		}
		String simpleSourceFileName = ((BinaryType) type).getSourceFileName(info);
		if (simpleSourceFileName == null) {
			return null;
		}
		return findSource(type, simpleSourceFileName);
	}

	/**
	 * Locates and returns source code for the given (binary) type, in this
	 * SourceMapper's ZIP file, or returns <code>null</code> if source
	 * code cannot be found.
	 * The given simpleSourceFileName is the .java file name (without the enclosing
	 * folder) used to create the given type (e.g. "A.java" for x/y/A$Inner.class)
	 */
	public char[] findSource(IType type, String simpleSourceFileName) {
		PackageFragment pkgFrag = (PackageFragment) type.getPackageFragment();
		String name = org.eclipse.jdt.internal.core.util.Util.concatWith(pkgFrag.names, simpleSourceFileName, '/');
		return internalFindSource((NamedMember) type, name);
	}

	/**
	 * Locates and returns source code for the given (binary) module, in this
	 * SourceMapper's ZIP file, or returns <code>null</code> if source
	 * code cannot be found.
	 */
	public char[] findSource(IModuleDescription module) {
		if (!module.isBinary()) {
			return null;
		}
		return internalFindSource((NamedMember) module, TypeConstants.MODULE_INFO_FILE_NAME_STRING);
	}

	private char[] internalFindSource(NamedMember typeOrModule, String name) {
		long time = 0;
		if (VERBOSE) {
			time = System.currentTimeMillis();
		}

		char[] source = null;

		JavaModelManager javaModelManager = JavaModelManager.getJavaModelManager();
		try {
			javaModelManager.cacheZipFiles(this); // Cache any zip files we open during this operation

			if (this.rootPath != null) {
				source = getSourceForRootPath(this.rootPath, name);
				if (source == null) {
					source = getSourceForRootPath("", name); //$NON-NLS-1$
				}
			}

			if (source == null) { // proceed with automatic root path detection ...
				// ... but not for multi-module roots
				if (!(typeOrModule.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT) instanceof JrtPackageFragmentRoot)) {
					computeAllRootPaths(typeOrModule);
					if (this.rootPaths != null) {
						loop: for (Iterator iterator = this.rootPaths.iterator(); iterator.hasNext(); ) {
							String currentRootPath = (String) iterator.next();
							if (!currentRootPath.equals(this.rootPath)) {
								source = getSourceForRootPath(currentRootPath, name);
								if (source != null) {
									// remember right root path
									this.rootPath = currentRootPath;
									break loop;
								}
							}
						}
					}
				}
			}
		} finally {
			javaModelManager.flushZipFiles(this); // clean up cached zip files.
		}
		if (VERBOSE) {
			System.out.println("spent " + (System.currentTimeMillis() - time) + "ms for " + typeOrModule.getElementName()); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return source;
	}

	private char[] getSourceForRootPath(String currentRootPath, String name) {
		String newFullName;
		if (!currentRootPath.equals(IPackageFragmentRoot.DEFAULT_PACKAGEROOT_PATH)) {
			if (currentRootPath.endsWith("/")) { //$NON-NLS-1$
				newFullName = currentRootPath + name;
			} else {
				newFullName = currentRootPath + '/' + name;
			}
		} else {
			newFullName = name;
		}
		return this.findSource(newFullName);
	}

	public char[] findSource(String fullName) {
		char[] source = null;
		Object target = JavaModel.getTarget(this.sourcePath, true);
		String charSet = null;
		if (target instanceof IContainer) {
			IResource res = ((IContainer)target).findMember(fullName);
			if (res instanceof IFile) {
				try {
					// Order of preference: charSet supplied, this.encoding or this.defaultEncoding in that order
					try {
						// Use the implicit encoding only when the source attachment's encoding hasn't been explicitly set.
						charSet = ((IFile) res).getCharset(this.encoding == null);
					} catch (CoreException e) {
						// Ignore
					}
					source = org.eclipse.jdt.internal.core.util.Util.getResourceContentsAsCharArray((IFile) res,
									charSet == null ? (this.encoding == null ? this.defaultEncoding : this.encoding) : charSet);
				} catch (JavaModelException e) {
					// Ignore
				}
			}
		} else {
			try {
				// https://bugs.eclipse.org/bugs/show_bug.cgi?id=303511
				// For a resource inside the workspace, use the encoding set on the resource
				if (target instanceof IFile)
					charSet = ((IFile)target).getCharset(this.encoding == null);
			} catch (CoreException e) {
				// Ignore
			}

			// try to get the entry
			ZipEntry entry = null;
			ZipFile zip = null;
			JavaModelManager manager = JavaModelManager.getJavaModelManager();
			try {
				zip = manager.getZipFile(this.sourcePath);
				entry = zip.getEntry(fullName);
				if (entry != null) {
					// now read the source code
					source = readSource(entry, zip, charSet);
				}
			} catch (CoreException e) {
				return null;
			} finally {
				manager.closeZipFile(zip); // handle null case
			}
		}
		return source;
	}


	public int getFlags(IJavaElement element) {
		switch(element.getElementType()) {
			case IJavaElement.LOCAL_VARIABLE :
				LocalVariableElementKey key = new LocalVariableElementKey(element.getParent(), element.getElementName());
				if (this.finalParameters != null && this.finalParameters.contains(key)) {
					return Flags.AccFinal;
				}
		}
		return 0;
	}

	/**
	 * Returns the SourceRange for the name of the given element, or
	 * {-1, -1} if no source range is known for the name of the element.
	 */
	public SourceRange getNameRange(IJavaElement element) {
		switch(element.getElementType()) {
			case IJavaElement.METHOD :
				if (((IMember) element).isBinary()) {
					IJavaElement[] el = getUnqualifiedMethodHandle((IMethod) element, false);
					if(el[1] != null && this.sourceRanges.get(el[0]) == null) {
						element = getUnqualifiedMethodHandle((IMethod) element, true)[0];
					} else {
						element = el[0];
					}
				}
				break;
			case IJavaElement.TYPE_PARAMETER :
				IJavaElement parent = element.getParent();
				if (parent.getElementType() == IJavaElement.METHOD) {
					IMethod method = (IMethod) parent;
					if (method.isBinary()) {
						IJavaElement[] el = getUnqualifiedMethodHandle(method, false);
						if(el[1] != null && this.sourceRanges.get(el[0]) == null) {
							method = (IMethod) getUnqualifiedMethodHandle(method, true)[0];
						} else {
							method = (IMethod) el[0];
						}
						element = method.getTypeParameter(element.getElementName());
					}
				}
				break;
			case IJavaElement.LOCAL_VARIABLE :
				LocalVariableElementKey key = new LocalVariableElementKey(element.getParent(), element.getElementName());
				SourceRange[] ranges = (SourceRange[]) this.parametersRanges.get(key);
				if (ranges == null) {
					return UNKNOWN_RANGE;
				} else {
					return ranges[1];
				}
		}
		SourceRange[] ranges = (SourceRange[]) this.sourceRanges.get(element);
		if (ranges == null) {
			return UNKNOWN_RANGE;
		} else {
			return ranges[1];
		}
	}

	/**
	 * Returns parameters names for the given method, or
	 * null if no parameter names are known for the method.
	 */
	public char[][] getMethodParameterNames(IMethod method) {
		if (method.isBinary()) {
			IJavaElement[] el = getUnqualifiedMethodHandle(method, false);
			if(el[1] != null && this.parameterNames.get(el[0]) == null) {
				method = (IMethod) getUnqualifiedMethodHandle(method, true)[0];
			} else {
				method = (IMethod) el[0];
			}
		}
		char[][] parameters = (char[][]) this.parameterNames.get(method);
		if (parameters == null) {
			return null;
		} else {
			return parameters;
		}
	}

	/**
	 * Returns the <code>SourceRange</code> for the given element, or
	 * {-1, -1} if no source range is known for the element.
	 */
	public SourceRange getSourceRange(IJavaElement element) {
		switch(element.getElementType()) {
			case IJavaElement.METHOD :
				if (((IMember) element).isBinary()) {
					IJavaElement[] el = getUnqualifiedMethodHandle((IMethod) element, false);
					if(el[1] != null && this.sourceRanges.get(el[0]) == null) {
						element = getUnqualifiedMethodHandle((IMethod) element, true)[0];
					} else {
						element = el[0];
					}
				}
				break;
			case IJavaElement.TYPE_PARAMETER :
				IJavaElement parent = element.getParent();
				if (parent.getElementType() == IJavaElement.METHOD) {
					IMethod method = (IMethod) parent;
					if (method.isBinary()) {
						IJavaElement[] el = getUnqualifiedMethodHandle(method, false);
						if(el[1] != null && this.sourceRanges.get(el[0]) == null) {
							method = (IMethod) getUnqualifiedMethodHandle(method, true)[0];
						} else {
							method = (IMethod) el[0];
						}
						element = method.getTypeParameter(element.getElementName());
					}
				}
				break;
			case IJavaElement.LOCAL_VARIABLE :
				LocalVariableElementKey key = new LocalVariableElementKey(element.getParent(), element.getElementName());
				SourceRange[] ranges = (SourceRange[]) this.parametersRanges.get(key);
				if (ranges == null) {
					return UNKNOWN_RANGE;
				} else {
					return ranges[0];
				}
		}
		SourceRange[] ranges = (SourceRange[]) this.sourceRanges.get(element);
		if (ranges == null) {
			return UNKNOWN_RANGE;
		} else {
			return ranges[0];
		}
	}

	/**
	 * Returns the type with the given <code>typeName</code>.  Returns inner classes
	 * as well.
	 */
	protected IType getType(String typeName) {
		if (!(this.binaryTypeOrModule instanceof IType))
			return null;
		IType type = (IType) this.binaryTypeOrModule;
		if (typeName.length() == 0) {
			IJavaElement classFile = type.getParent();
			String classFileName = classFile.getElementName();
			StringBuilder newClassFileName = new StringBuilder();
			int lastDollar = classFileName.lastIndexOf('$');
			for (int i = 0; i <= lastDollar; i++)
				newClassFileName.append(classFileName.charAt(i));
			newClassFileName.append(Integer.toString(this.anonymousCounter));
			PackageFragment pkg = (PackageFragment) classFile.getParent();
			return new BinaryType(new ClassFile(pkg, newClassFileName.toString()), typeName);
		} else if (type.getElementName().equals(typeName))
			return type;
		else
			return ((this.typeDepth <= 1) ? type : this.types[this.typeDepth - 1]).getType(typeName);
	}

	/**
	 * Creates a handle that has parameter types that are not
	 * fully qualified so that the correct source is found.
	 */
	protected IJavaElement[] getUnqualifiedMethodHandle(IMethod method, boolean noDollar) {
		boolean hasDollar = false;
		String[] qualifiedParameterTypes = method.getParameterTypes();
		String[] unqualifiedParameterTypes = new String[qualifiedParameterTypes.length];
		for (int i = 0; i < qualifiedParameterTypes.length; i++) {
			StringBuffer unqualifiedTypeSig = new StringBuffer();
			getUnqualifiedTypeSignature(qualifiedParameterTypes[i], 0/*start*/, qualifiedParameterTypes[i].length(), unqualifiedTypeSig, noDollar);
			unqualifiedParameterTypes[i] = unqualifiedTypeSig.toString();
			hasDollar |= unqualifiedParameterTypes[i].lastIndexOf('$') != -1;
		}

		IJavaElement[] result = new IJavaElement[2];
		result[0] = ((IType) method.getParent()).getMethod(
			method.getElementName(),
			unqualifiedParameterTypes);
		if(hasDollar) {
			result[1] = result[0];
		}
		return result;
	}

	private int getUnqualifiedTypeSignature(String qualifiedTypeSig, int start, int length, StringBuffer unqualifiedTypeSig, boolean noDollar) {
		char firstChar = qualifiedTypeSig.charAt(start);
		int end = start + 1;
		boolean sigStart = false;
		firstPass: for (int i = start; i < length; i++) {
			char current = qualifiedTypeSig.charAt(i);
			switch (current) {
				case Signature.C_ARRAY :
				case Signature.C_SUPER:
				case Signature.C_EXTENDS:
					unqualifiedTypeSig.append(current);
					start = i + 1;
					end = start + 1;
					firstChar = qualifiedTypeSig.charAt(start);
					break;
				case Signature.C_RESOLVED :
				case Signature.C_UNRESOLVED :
				case Signature.C_TYPE_VARIABLE :
					if (!sigStart) {
						start = ++i;
						sigStart = true;
					}
					break;
				case Signature.C_NAME_END:
				case Signature.C_GENERIC_START :
					end = i;
					break firstPass;
				case Signature.C_STAR :
					unqualifiedTypeSig.append(current);
					start = i + 1;
					end = start + 1;
					firstChar = qualifiedTypeSig.charAt(start);
					break;
				case Signature.C_GENERIC_END :
					return i;
				case Signature.C_DOT:
					start = ++i;
					break;
				case Signature.C_BOOLEAN :
				case Signature.C_BYTE :
				case Signature.C_CHAR :
				case Signature.C_DOUBLE :
				case Signature.C_FLOAT :
				case Signature.C_INT :
				case Signature.C_LONG :
				case Signature.C_SHORT :
					if (!sigStart) {
						unqualifiedTypeSig.append(current);
						return i+1;
					}
			}
		}
		switch (firstChar) {
			case Signature.C_RESOLVED :
			case Signature.C_UNRESOLVED :
			case Signature.C_TYPE_VARIABLE :
				unqualifiedTypeSig.append(Signature.C_UNRESOLVED);
				if (noDollar) {
					int lastDollar = qualifiedTypeSig.lastIndexOf('$', end);
					if (lastDollar > start)
						start = lastDollar + 1;
				}
				for (int i = start; i < length; i++) {
					char current = qualifiedTypeSig.charAt(i);
					switch (current) {
						case Signature.C_GENERIC_START:
							unqualifiedTypeSig.append(current);
							i++;
							do {
								i = getUnqualifiedTypeSignature(qualifiedTypeSig, i, length, unqualifiedTypeSig, noDollar);
							} while (qualifiedTypeSig.charAt(i) != Signature.C_GENERIC_END);
							unqualifiedTypeSig.append(Signature.C_GENERIC_END);
							break;
						case Signature.C_NAME_END:
							unqualifiedTypeSig.append(current);
							return i + 1;
						default:
							unqualifiedTypeSig.append(current);
							break;
					}
				}
				return length;
			default :
				// primitive type or wildcard
				unqualifiedTypeSig.append(qualifiedTypeSig.substring(start, end));
				return end;
		}
	}

	/**
	 * Maps the given source code to the given binary type or module and its children.
	 */
	public void mapSource(NamedMember typeOrModule, char[] contents, IBinaryType info) {
		this.mapSource(typeOrModule, contents, info, null);
	}

	/**
	 * Maps the given source code to the given binary type and its children.
	 * If a non-null java element is passed, finds the name range for the
	 * given java element without storing it.
	 */
	public synchronized ISourceRange mapSource(
		NamedMember typeOrModule,
		char[] contents,
		IBinaryType info, // null for modules
		IJavaElement elementToFind) {

		this.binaryTypeOrModule = typeOrModule;

		// check whether it is already mapped
		if (this.sourceRanges.get(typeOrModule) != null) return (elementToFind != null) ? getNameRange(elementToFind) : null;

		this.importsTable.remove(this.binaryTypeOrModule);
		this.importsCounterTable.remove(this.binaryTypeOrModule);
		this.searchedElement = elementToFind;
		this.types = new IType[1];
		this.typeDeclarationStarts = new int[1];
		this.typeNameRanges = new SourceRange[1];
		this.typeModifiers = new int[1];
		this.typeDepth = -1;
		this.memberDeclarationStart = new int[1];
		this.memberName = new String[1];
		this.memberNameRange = new SourceRange[1];
		this.methodParameterTypes = new char[1][][];
		this.methodParameterNames = new char[1][][];
		this.anonymousCounter = 0;

		HashMap oldSourceRanges = null;
		if (elementToFind != null) {
			oldSourceRanges = (HashMap) this.sourceRanges.clone();
		}
		try {
			IProblemFactory factory = new DefaultProblemFactory();
			SourceElementParser parser = null;
			boolean doFullParse = false;
			this.anonymousClassName = 0;
			String sourceFileName;
			if (this.binaryTypeOrModule instanceof BinaryType) {
				if (info == null) {
					try {
						info = (IBinaryType) this.binaryTypeOrModule.getElementInfo();
					} catch(JavaModelException e) {
						return null;
					}
				}
				sourceFileName = ((BinaryType) this.binaryTypeOrModule).sourceFileName(info);
				boolean isAnonymousClass = info.isAnonymous();

				char[] fullName = info.getName();
				if (isAnonymousClass) {
					String eltName = this.binaryTypeOrModule.getParent().getElementName();
					eltName = eltName.substring(eltName.lastIndexOf('$') + 1, eltName.length());
					try {
						this.anonymousClassName = Integer.parseInt(eltName);
					} catch(NumberFormatException e) {
						// ignore
					}
				}
				doFullParse = hasToRetrieveSourceRangesForLocalClass(fullName);
			} else {
				sourceFileName = TypeConstants.MODULE_INFO_CLASS_NAME_STRING;
			}
			parser = new SourceElementParser(this, factory, new CompilerOptions(this.options), doFullParse, true/*optimize string literals*/);
			parser.javadocParser.checkDocComment = false; // disable javadoc parsing
			IJavaElement javaElement = this.binaryTypeOrModule.getCompilationUnit();
			if (javaElement == null) javaElement = this.binaryTypeOrModule.getParent();
			parser.parseCompilationUnit(
				new BasicCompilationUnit(contents, null, sourceFileName, javaElement),
				doFullParse,
				null/*no progress*/);
			if (elementToFind != null) {
				ISourceRange range = getNameRange(elementToFind);
				return range;
			} else {
				return null;
			}
		} finally {
			if (elementToFind != null) {
				this.sourceRanges = oldSourceRanges;
			}
			this.binaryTypeOrModule = null;
			this.searchedElement = null;
			this.types = null;
			this.typeDeclarationStarts = null;
			this.typeNameRanges = null;
			this.typeDepth = -1;
		}
	}
	private char[] readSource(ZipEntry entry, ZipFile zip, String charSet) {
		try {
			byte[] bytes = Util.getZipEntryByteContent(entry, zip);
			if (bytes != null) {
				// Order of preference: charSet supplied, this.encoding or this.defaultEncoding in that order
				return Util.bytesToChar(bytes, charSet == null ? (this.encoding == null ? this.defaultEncoding : this.encoding) : charSet);
			}
		} catch (IOException e) {
			// ignore
		}
		return null;
	}

	/**
	 * Sets the mapping for this method to its parameter names.
	 *
	 * @see #parameterNames
	 */
	protected void setMethodParameterNames(
		IMethod method,
		char[][] parameterNames) {
		if (parameterNames == null) {
			parameterNames = CharOperation.NO_CHAR_CHAR;
		}
		this.parameterNames.put(method, parameterNames);
	}

	/**
	 * Sets the mapping for this element to its source ranges for its source range
	 * and name range.
	 *
	 * @see #sourceRanges
	 */
	protected void setSourceRange(
		IJavaElement element,
		SourceRange sourceRange,
		SourceRange nameRange) {
		this.sourceRanges.put(element, new SourceRange[] { sourceRange, nameRange });
	}

	/**
	 * Return a char[][] array containing the imports of the attached source for the binary type
	 */
	public char[][] getImports(Member typeOrModule) {
		char[][] imports = (char[][]) this.importsTable.get(typeOrModule);
		if (imports != null) {
			int importsCounter = ((Integer) this.importsCounterTable.get(typeOrModule)).intValue();
			if (imports.length != importsCounter) {
				System.arraycopy(
					imports,
					0,
					(imports = new char[importsCounter][]),
					0,
					importsCounter);
			}
			this.importsTable.put(typeOrModule, imports);
		}
		return imports;
	}

	private boolean hasToRetrieveSourceRangesForLocalClass(char[] eltName) {
		/*
		 * A$1$B$2 : true
		 * A$B$B$2 : true
		 * A$C$B$D : false
		 * A$F$B$D$1$F : true
		 * A$F$B$D$1F : true
		 * A$1 : true
		 * A$B : false
		 */
		if (eltName == null) return false;
		int length = eltName.length;
		int dollarIndex = CharOperation.indexOf('$', eltName, 0);
		while (dollarIndex != -1) {
			int nameStart = dollarIndex+1;
			if (nameStart == length) return false;
			if (Character.isDigit(eltName[nameStart]))
				return true;
			dollarIndex = CharOperation.indexOf('$', eltName, nameStart);
		}
		return false;
	}

}
