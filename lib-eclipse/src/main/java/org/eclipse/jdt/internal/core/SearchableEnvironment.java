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
 *     Stephan Herrmann - contribution for bug 337868 - [compiler][model] incomplete support for package-info.java when using SearchableEnvironment
 *     Microsoft Corporation - contribution for bug 575562 - improve completion search performance
 *******************************************************************************/
package org.eclipse.jdt.internal.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.search.*;
import org.eclipse.jdt.internal.codeassist.ISearchRequestor;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.classfmt.ExternalAnnotationDecorator;
import org.eclipse.jdt.internal.compiler.classfmt.ExternalAnnotationProvider;
import org.eclipse.jdt.internal.compiler.env.AccessRestriction;
import org.eclipse.jdt.internal.compiler.env.IBinaryType;
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.jdt.internal.compiler.env.IModule;
import org.eclipse.jdt.internal.compiler.env.IModule.IModuleReference;
import org.eclipse.jdt.internal.compiler.env.IModule.IPackageExport;
import org.eclipse.jdt.internal.compiler.env.IModuleAwareNameEnvironment;
import org.eclipse.jdt.internal.compiler.env.ISourceType;
import org.eclipse.jdt.internal.compiler.env.IUpdatableModule;
import org.eclipse.jdt.internal.compiler.env.IUpdatableModule.UpdateKind;
import org.eclipse.jdt.internal.compiler.env.NameEnvironmentAnswer;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.lookup.BinaryTypeBinding.ExternalAnnotationStatus;
import org.eclipse.jdt.internal.compiler.lookup.ModuleBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeConstants;
import org.eclipse.jdt.internal.core.NameLookup.Answer;
import org.eclipse.jdt.internal.core.search.BasicSearchEngine;
import org.eclipse.jdt.internal.core.search.IRestrictedAccessConstructorRequestor;
import org.eclipse.jdt.internal.core.search.IRestrictedAccessTypeRequestor;
import org.eclipse.jdt.internal.core.search.indexing.IndexManager;
import org.eclipse.jdt.internal.core.search.processing.IJob;
import org.eclipse.jdt.internal.core.util.Util;

/**
 *	This class provides a <code>SearchableBuilderEnvironment</code> for code assist which
 *	uses the Java model as a search tool.
 */
public class SearchableEnvironment
	implements IModuleAwareNameEnvironment, IJavaSearchConstants {

	public NameLookup nameLookup;
	protected ICompilationUnit unitToSkip;
	protected org.eclipse.jdt.core.ICompilationUnit[] workingCopies;
	protected WorkingCopyOwner owner;

	protected JavaProject project;
	protected IJavaSearchScope searchScope;

	protected boolean checkAccessRestrictions;
	// moduleName -> IPackageFragmentRoot[](lazily populated)
	private Map<String,IPackageFragmentRoot[]> knownModuleLocations; // null indicates: not using JPMS
	private boolean excludeTestCode;

	private ModuleUpdater moduleUpdater;
	private Map<IPackageFragmentRoot,IModuleDescription> rootToModule;

	private long timeSpentInGetModulesDeclaringPackage;
	private long timeSpentInFindTypes;

	@Deprecated
	public SearchableEnvironment(JavaProject project, org.eclipse.jdt.core.ICompilationUnit[] workingCopies) throws JavaModelException {
		this(project, workingCopies, false);
	}
	/**
	 * Creates a SearchableEnvironment on the given project
	 */
	public SearchableEnvironment(JavaProject project, org.eclipse.jdt.core.ICompilationUnit[] workingCopies, boolean excludeTestCode) throws JavaModelException {
		this.project = project;
		this.excludeTestCode = excludeTestCode;
		this.checkAccessRestrictions =
			!JavaCore.IGNORE.equals(project.getOption(JavaCore.COMPILER_PB_FORBIDDEN_REFERENCE, true))
			|| !JavaCore.IGNORE.equals(project.getOption(JavaCore.COMPILER_PB_DISCOURAGED_REFERENCE, true));
		this.workingCopies = workingCopies;
		this.nameLookup = project.newNameLookup(workingCopies, excludeTestCode);
		if (CompilerOptions.versionToJdkLevel(project.getOption(JavaCore.COMPILER_COMPLIANCE, true)) >= ClassFileConstants.JDK9) {
			this.knownModuleLocations = new HashMap<>();
		}
		if (CompilerOptions.versionToJdkLevel(project.getOption(JavaCore.COMPILER_COMPLIANCE, true)) >= ClassFileConstants.JDK9) {
			this.moduleUpdater = new ModuleUpdater(project);
			if (!excludeTestCode) {
				IClasspathEntry[] expandedClasspath = project.getExpandedClasspath();
				if(Arrays.stream(expandedClasspath).anyMatch(e -> e.isTest())) {
					this.moduleUpdater.addReadUnnamedForNonEmptyClasspath(project, expandedClasspath);
				}
			}
			for (IClasspathEntry entry : project.getRawClasspath())
				if(!excludeTestCode || !entry.isTest())
					this.moduleUpdater.computeModuleUpdates(entry);
		}
	}

	/**
	 * Note: this is required for (abandoned) Scala-IDE
	 */
	@Deprecated
	public SearchableEnvironment(JavaProject project, WorkingCopyOwner owner) throws JavaModelException {
		this(project, owner, false);
	}

	/**
	 * Creates a SearchableEnvironment on the given project
	 */
	public SearchableEnvironment(JavaProject project, WorkingCopyOwner owner, boolean excludeTestCode) throws JavaModelException {
		this(project, owner == null ? null : JavaModelManager.getJavaModelManager().getWorkingCopies(owner, true/*add primary WCs*/), excludeTestCode);
		this.owner = owner;
	}

	private static int convertSearchFilterToModelFilter(int searchFilter) {
		switch (searchFilter) {
			case IJavaSearchConstants.CLASS:
				return NameLookup.ACCEPT_CLASSES;
			case IJavaSearchConstants.INTERFACE:
				return NameLookup.ACCEPT_INTERFACES;
			case IJavaSearchConstants.ENUM:
				return NameLookup.ACCEPT_ENUMS;
			case IJavaSearchConstants.ANNOTATION_TYPE:
				return NameLookup.ACCEPT_ANNOTATIONS;
			case IJavaSearchConstants.CLASS_AND_ENUM:
				return NameLookup.ACCEPT_CLASSES | NameLookup.ACCEPT_ENUMS;
			case IJavaSearchConstants.CLASS_AND_INTERFACE:
				return NameLookup.ACCEPT_CLASSES | NameLookup.ACCEPT_INTERFACES;
			default:
				return NameLookup.ACCEPT_ALL;
		}
	}
	/**
	 * Returns the given type in the the given package if it exists,
	 * otherwise <code>null</code>.
	 */
	protected NameEnvironmentAnswer find(String typeName, String packageName, IPackageFragmentRoot[] moduleContext) {
		if (packageName == null)
			packageName = IPackageFragment.DEFAULT_PACKAGE_NAME;
		if (this.owner != null) {
			String source = this.owner.findSource(typeName, packageName);
			if (source != null) {
				IJavaElement moduleElement = (moduleContext != null && moduleContext.length > 0) ? moduleContext[0] : null;
				ICompilationUnit cu = new BasicCompilationUnit(
						source.toCharArray(),
						CharOperation.splitOn('.', packageName.toCharArray()),
						typeName + Util.defaultJavaExtension(),
						moduleElement);
				return new NameEnvironmentAnswer(cu, null);
			}
		}
		NameLookup.Answer answer =
			this.nameLookup.findType(
				typeName,
				packageName,
				false/*exact match*/,
				NameLookup.ACCEPT_ALL,
				this.checkAccessRestrictions,
				moduleContext);
		if (answer != null) {
			// construct name env answer
			if (answer.type instanceof BinaryType) { // BinaryType
				return createAnswer(answer, packageName, typeName, (BinaryType) answer.type);
			} else { //SourceType
				try {
					// retrieve the requested type
					SourceTypeElementInfo sourceType = (SourceTypeElementInfo)((SourceType) answer.type).getElementInfo();
					ISourceType topLevelType = sourceType;
					while (topLevelType.getEnclosingType() != null) {
						topLevelType = topLevelType.getEnclosingType();
					}
					// find all siblings (other types declared in same unit, since may be used for name resolution)
					IType[] types = sourceType.getHandle().getCompilationUnit().getTypes();
					ISourceType[] sourceTypes = new ISourceType[types.length];

					// in the resulting collection, ensure the requested type is the first one
					sourceTypes[0] = sourceType;
					int length = types.length;
					for (int i = 0, index = 1; i < length; i++) {
						ISourceType otherType =
							(ISourceType) ((JavaElement) types[i]).getElementInfo();
						if (!otherType.equals(topLevelType) && index < length) // check that the index is in bounds (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=62861)
							sourceTypes[index++] = otherType;
					}
					char[] moduleName = answer.module != null ? answer.module.getElementName().toCharArray() : null;
					return new NameEnvironmentAnswer(sourceTypes, answer.restriction, getExternalAnnotationPath(answer.entry), moduleName);
				} catch (JavaModelException jme) {
					if (jme.isDoesNotExist() && String.valueOf(TypeConstants.PACKAGE_INFO_NAME).equals(typeName)) {
						// in case of package-info.java the type doesn't exist in the model,
						// but the CU may still help in order to fetch package level annotations.
						return new NameEnvironmentAnswer((ICompilationUnit)answer.type.getParent(), answer.restriction);
					}
					// no usable answer
				}
			}
		}
		return null;
	}

	private String getExternalAnnotationPath(IClasspathEntry entry) {
		if (entry == null)
			return null;
		IPath path = ClasspathEntry.getExternalAnnotationPath(entry, this.project.getProject(), true);
		if (path == null)
			return null;
		return path.toOSString();
	}

	private NameEnvironmentAnswer createAnswer(Answer lookupAnswer, String packageName, String typeName, BinaryType binaryType) {
		char[] moduleName = lookupAnswer.module != null ? lookupAnswer.module.getElementName().toCharArray() : null;
		try {
			IBinaryType iBinaryType = (IBinaryType) binaryType.getElementInfo();
			if (iBinaryType.getExternalAnnotationStatus() == ExternalAnnotationStatus.NOT_EEA_CONFIGURED
					&& JavaCore.ENABLED.equals(this.project.getOption(JavaCore.CORE_JAVA_BUILD_EXTERNAL_ANNOTATIONS_FROM_ALL_LOCATIONS, true)))
			{
				String soughtName = typeName+ExternalAnnotationProvider.ANNOTATION_FILE_SUFFIX;
				boolean isAnnotated = false;
				IPackageFragment[] packageFragments = this.nameLookup.findPackageFragments(packageName, false);
				if (packageFragments != null) {
					String packageNameSlash = packageName.replace('.', '/');
					for (IPackageFragment fragment : packageFragments) {
						if (fragment.exists()) {
							for (Object rc : fragment.getNonJavaResources()) {
								if (rc instanceof IStorage && soughtName.equals(((IStorage) rc).getName())) {
									if (isAnnotated) {
										// TODO: if merging at method granularity should be supported, this is where to implement it.
										// Otherwise we could raise/log a warning?
										break;
									}
									try {
										iBinaryType = new ExternalAnnotationDecorator(iBinaryType,
												new ExternalAnnotationProvider(((IStorage) rc).getContents(), packageNameSlash+'/'+typeName));
										isAnnotated = true;
										break;
									} catch (IOException | CoreException e) {
										// ignore
									}
								}
							}
						}
					}
					if (!isAnnotated) {
						// project is configured to globally consider external annotations, but no .eea found => decorate in order to answer NO_EEA_FILE:
						iBinaryType = new ExternalAnnotationDecorator(iBinaryType, null);
					}
				}
			}
			return new NameEnvironmentAnswer(iBinaryType, lookupAnswer.restriction, moduleName);
		} catch (JavaModelException e) {
			// fallback to null
		}
		return null;
	}

	/**
	 * Find the modules that start with the given prefix.
	 * A valid prefix is a qualified name separated by periods
	 * (ex. java.util).
	 * The packages found are passed to:
	 *    ISearchRequestor.acceptModule(char[][] moduleName)
	 */
	public void findModules(char[] prefix, ISearchRequestor requestor, IJavaProject javaProject) {
		this.nameLookup.seekModule(prefix, true, new SearchableEnvironmentRequestor(requestor));
	}

	/**
	 * Find the packages that start with the given prefix.
	 * A valid prefix is a qualified name separated by periods
	 * (ex. java.util).
	 * The packages found are passed to:
	 *    ISearchRequestor.acceptPackage(char[][] packageName)
	 */
	public void findPackages(char[] prefix, ISearchRequestor requestor) {
		this.nameLookup.seekPackageFragments(
			new String(prefix),
			true,
			new SearchableEnvironmentRequestor(requestor));
	}

	/**
	 * Find the packages that start with the given prefix and belong to the given module.
	 * A valid prefix is a qualified name separated by periods
	 * (ex. java.util).
	 * The packages found are passed to:
	 *    ISearchRequestor.acceptPackage(char[][] packageName)
	 */
	public void findPackages(char[] prefix, ISearchRequestor requestor, IPackageFragmentRoot[] moduleContext, boolean followRequires) {
		this.nameLookup.seekPackageFragments(
			new String(prefix),
			true,
			new SearchableEnvironmentRequestor(requestor), moduleContext);
	if (followRequires && this.knownModuleLocations != null) {
		try {
			boolean isMatchAllPrefix = CharOperation.equals(CharOperation.ALL_PREFIX, prefix);
			Set<IModuleDescription> modDescs = new HashSet<>();
			for (IPackageFragmentRoot root : moduleContext) {
				IModuleDescription desc = root.getJavaProject().getModuleDescription();
				if (desc instanceof AbstractModule)
					modDescs.add(desc);
			}
			for (IModuleDescription md : modDescs) {
				IModuleReference[] reqModules = ((AbstractModule) md).getRequiredModules();
				char[] modName = md.getElementName().toCharArray();
				for (IModuleReference moduleReference : reqModules) {
					findPackagesFromRequires(prefix, isMatchAllPrefix, requestor, moduleReference, modName);
				}
			}
		} catch (JavaModelException e) {
			// silent
		}
	}
}

private void findPackagesFromRequires(char[] prefix, boolean isMatchAllPrefix, ISearchRequestor requestor, IModuleReference moduleReference, char[] clientModuleName) {
	IPackageFragmentRoot[] fragmentRoots = findModuleContext(moduleReference.name());
	if (fragmentRoots == null) return;
	for (IPackageFragmentRoot root : fragmentRoots) {
		IJavaProject requiredProject = root.getJavaProject();
		try {
			IModuleDescription module = requiredProject.getModuleDescription();
			if (module instanceof AbstractModule) {
				AbstractModule requiredModule = (AbstractModule) module;
				for (IPackageExport packageExport : requiredModule.getExportedPackages()) {
					if (!packageExport.isQualified() || CharOperation.containsEqual(packageExport.targets(), clientModuleName)) {
						char[] exportName = packageExport.name();
						if (isMatchAllPrefix || CharOperation.prefixEquals(prefix, exportName))
							requestor.acceptPackage(exportName);
					}
				}
				for (IModuleReference moduleRef2 : requiredModule.getRequiredModules()) {
					if (moduleRef2.isTransitive())
						findPackagesFromRequires(prefix, isMatchAllPrefix, requestor, moduleRef2, clientModuleName);
				}
			}
		} catch (JavaModelException e) {
			// silent
		}
	}
}
	/**
	 * Find the top-level types that are defined
	 * in the current environment and whose simple name matches the given name.
	 *
	 * The types found are passed to one of the following methods (if additional
	 * information is known about the types):
	 *    ISearchRequestor.acceptType(char[][] packageName, char[] typeName)
	 *    ISearchRequestor.acceptClass(char[][] packageName, char[] typeName, int modifiers)
	 *    ISearchRequestor.acceptInterface(char[][] packageName, char[] typeName, int modifiers)
	 *
	 * This method can not be used to find member types... member
	 * types are found relative to their enclosing type.
	 */
	public void findExactTypes(char[] name, final boolean findMembers, int searchFor, final ISearchRequestor storage) {

		try {
			final String excludePath;
			if (this.unitToSkip != null) {
				if (!(this.unitToSkip instanceof IJavaElement)) {
					// revert to model investigation
					findExactTypes(
						new String(name),
						storage,
						convertSearchFilterToModelFilter(searchFor));
					return;
				}
				excludePath = ((IJavaElement) this.unitToSkip).getPath().toString();
			} else {
				excludePath = null;
			}

			IProgressMonitor progressMonitor = new IProgressMonitor() {
				boolean isCanceled = false;
				@Override
				public void beginTask(String n, int totalWork) {
					// implements interface method
				}
				@Override
				public void done() {
					// implements interface method
				}
				@Override
				public void internalWorked(double work) {
					// implements interface method
				}
				@Override
				public boolean isCanceled() {
					return this.isCanceled;
				}
				@Override
				public void setCanceled(boolean value) {
					this.isCanceled = value;
				}
				@Override
				public void setTaskName(String n) {
					// implements interface method
				}
				@Override
				public void subTask(String n) {
					// implements interface method
				}
				@Override
				public void worked(int work) {
					// implements interface method
				}
			};
			IRestrictedAccessTypeRequestor typeRequestor = new IRestrictedAccessTypeRequestor() {
				@Override
				public void acceptType(int modifiers, char[] packageName, char[] simpleTypeName, char[][] enclosingTypeNames, String path, AccessRestriction access) {
					if (excludePath != null && excludePath.equals(path))
						return;
					if (!findMembers && enclosingTypeNames != null && enclosingTypeNames.length > 0)
						return; // accept only top level types
					storage.acceptType(packageName, simpleTypeName, enclosingTypeNames, modifiers, access);
				}
			};
			try {
				new BasicSearchEngine(this.workingCopies).searchAllTypeNames(
					null,
					SearchPattern.R_EXACT_MATCH,
					name,
					SearchPattern.R_EXACT_MATCH,
					searchFor,
					getSearchScope(),
					typeRequestor,
					CANCEL_IF_NOT_READY_TO_SEARCH,
					progressMonitor);
			} catch (OperationCanceledException e) {
				findExactTypes(
					new String(name),
					storage,
					convertSearchFilterToModelFilter(searchFor));
			}
		} catch (JavaModelException e) {
			findExactTypes(
				new String(name),
				storage,
				convertSearchFilterToModelFilter(searchFor));
		}
	}

	/**
	 * Returns all types whose simple name matches with the given <code>name</code>.
	 */
	private void findExactTypes(String name, ISearchRequestor storage, int type) {
		SearchableEnvironmentRequestor requestor =
			new SearchableEnvironmentRequestor(storage, this.unitToSkip, this.project, this.nameLookup);
		this.nameLookup.seekTypes(name, null, false, type, requestor);
	}

	/**
	 * Find a type in the given module or any module read by it.
	 * Does not check accessibility / unique visibility, but returns the first observable type found.
	 * @param compoundTypeName name of the sought type
	 * @param module start into the module graph
	 * @return the answer :)
	 */
	public NameEnvironmentAnswer findTypeInModules(char[][] compoundTypeName, ModuleBinding module) {
		char[] nameForLookup = module.nameForLookup();
		NameEnvironmentAnswer answer = findType(compoundTypeName, nameForLookup);
		if (answer != null)
			return answer;
		if (LookupStrategy.get(nameForLookup) == LookupStrategy.Named) {
			for (ModuleBinding required : module.getAllRequiredModules()) {
				answer = findType(compoundTypeName, required.nameForLookup());
				if (answer != null)
					return answer;
			}
		}
		return null;
	}

	/**
	 * @see org.eclipse.jdt.internal.compiler.env.IModuleAwareNameEnvironment#findType(char[][],char[])
	 */
	@Override
	public NameEnvironmentAnswer findType(char[][] compoundTypeName, char[] moduleName) {
		if (compoundTypeName == null) return null;

		boolean isNamedStrategy = LookupStrategy.get(moduleName) == LookupStrategy.Named;
		IPackageFragmentRoot[] moduleLocations = isNamedStrategy ? findModuleContext(moduleName) : null;

		int length = compoundTypeName.length;
		if (length <= 1) {
			if (length == 0) return null;
			return find(new String(compoundTypeName[0]), null, moduleLocations);
		}

		int lengthM1 = length - 1;
		char[][] packageName = new char[lengthM1][];
		System.arraycopy(compoundTypeName, 0, packageName, 0, lengthM1);

		return find(
			new String(compoundTypeName[lengthM1]),
			CharOperation.toString(packageName),
			moduleLocations);
	}

	/**
	 * @see org.eclipse.jdt.internal.compiler.env.IModuleAwareNameEnvironment#findType(char[],char[][],char[])
	 */
	@Override
	public NameEnvironmentAnswer findType(char[] name, char[][] packageName, char[] moduleName) {
		if (name == null) return null;

		boolean isNamedStrategy = LookupStrategy.get(moduleName) == LookupStrategy.Named;
		IPackageFragmentRoot[] moduleLocations = isNamedStrategy ? findModuleContext(moduleName) : null;
		return find(
			new String(name),
			packageName == null || packageName.length == 0 ? null : CharOperation.toString(packageName),
			moduleLocations);
	}

	/**
	 * Find the top-level types that are defined
	 * in the current environment and whose name starts with the
	 * given prefix. The prefix is a qualified name separated by periods
	 * or a simple name (ex. java.util.V or V).
	 *
	 * The types found are passed to one of the following methods (if additional
	 * information is known about the types):
	 *    ISearchRequestor.acceptType(char[][] packageName, char[] typeName)
	 *    ISearchRequestor.acceptClass(char[][] packageName, char[] typeName, int modifiers)
	 *    ISearchRequestor.acceptInterface(char[][] packageName, char[] typeName, int modifiers)
	 *
	 * This method can not be used to find member types... member
	 * types are found relative to their enclosing type.
	 */
	public void findTypes(char[] prefix, final boolean findMembers, boolean camelCaseMatch, int searchFor, final ISearchRequestor storage) {
		findTypes(prefix, findMembers, camelCaseMatch ? SearchPattern.R_PREFIX_MATCH | SearchPattern.R_CAMELCASE_MATCH : SearchPattern.R_PREFIX_MATCH, searchFor, storage, null);
	}
	/**
	 * Must be used only by CompletionEngine.
	 * The progress monitor is used to be able to cancel completion operations
	 *
	 * Find the top-level types that are defined
	 * in the current environment and whose name starts with the
	 * given prefix. The prefix is a qualified name separated by periods
	 * or a simple name (ex. java.util.V or V).
	 *
	 * The types found are passed to one of the following methods (if additional
	 * information is known about the types):
	 *    ISearchRequestor.acceptType(char[][] packageName, char[] typeName)
	 *    ISearchRequestor.acceptClass(char[][] packageName, char[] typeName, int modifiers)
	 *    ISearchRequestor.acceptInterface(char[][] packageName, char[] typeName, int modifiers)
	 *
	 * This method can not be used to find member types... member
	 * types are found relative to their enclosing type.
	 */
	public void findTypes(char[] prefix, final boolean findMembers, int matchRule, int searchFor, final ISearchRequestor storage, IProgressMonitor monitor) {
		findTypes(prefix, findMembers, matchRule, searchFor, true, storage, monitor);
	}

	/**
	 * Must be used only by CompletionEngine.
	 * The progress monitor is used to be able to cancel completion operations
	 *
	 * Find the top-level types that are defined
	 * in the current environment and whose name starts with the
	 * given prefix. The prefix is a qualified name separated by periods
	 * or a simple name (ex. java.util.V or V).
	 *
	 * The types found are passed to one of the following methods (if additional
	 * information is known about the types):
	 *    ISearchRequestor.acceptType(char[][] packageName, char[] typeName)
	 *    ISearchRequestor.acceptClass(char[][] packageName, char[] typeName, int modifiers)
	 *    ISearchRequestor.acceptInterface(char[][] packageName, char[] typeName, int modifiers)
	 *
	 * This method can not be used to find member types... member
	 * types are found relative to their enclosing type.
	 */
	public void findTypes(char[] prefix, final boolean findMembers, int matchRule, int searchFor, final boolean resolveDocumentName, final ISearchRequestor storage, IProgressMonitor monitor) {
		long start = -1;
		if (NameLookup.VERBOSE)
			start = System.currentTimeMillis();

		boolean camelCaseMatch = (matchRule & SearchPattern.R_CAMELCASE_MATCH) != 0;
		/*
			if (true){
				findTypes(new String(prefix), storage, NameLookup.ACCEPT_CLASSES | NameLookup.ACCEPT_INTERFACES);
				return;
			}
		*/
		try {
			final String excludePath;
			if (this.unitToSkip != null) {
				if (!(this.unitToSkip instanceof IJavaElement)) {
					// revert to model investigation
					findTypes(
						new String(prefix),
						storage,
						convertSearchFilterToModelFilter(searchFor));
					return;
				}
				excludePath = ((IJavaElement) this.unitToSkip).getPath().toString();
			} else {
				excludePath = null;
			}
			int lastDotIndex = CharOperation.lastIndexOf('.', prefix);
			char[] qualification, simpleName;
			if (lastDotIndex < 0) {
				qualification = null;
				if (camelCaseMatch) {
					simpleName = prefix;
				} else {
					simpleName = CharOperation.toLowerCase(prefix);
				}
			} else {
				qualification = CharOperation.subarray(prefix, 0, lastDotIndex);
				if (camelCaseMatch) {
					simpleName = CharOperation.subarray(prefix, lastDotIndex + 1, prefix.length);
				} else {
					simpleName =
						CharOperation.toLowerCase(
							CharOperation.subarray(prefix, lastDotIndex + 1, prefix.length));
				}
			}

			IProgressMonitor progressMonitor = new IProgressMonitor() {
				boolean isCanceled = false;
				@Override
				public void beginTask(String name, int totalWork) {
					// implements interface method
				}
				@Override
				public void done() {
					// implements interface method
				}
				@Override
				public void internalWorked(double work) {
					// implements interface method
				}
				@Override
				public boolean isCanceled() {
					return this.isCanceled;
				}
				@Override
				public void setCanceled(boolean value) {
					this.isCanceled = value;
				}
				@Override
				public void setTaskName(String name) {
					// implements interface method
				}
				@Override
				public void subTask(String name) {
					// implements interface method
				}
				@Override
				public void worked(int work) {
					// implements interface method
				}
			};
			IRestrictedAccessTypeRequestor typeRequestor = new IRestrictedAccessTypeRequestor() {
				@Override
				public void acceptType(int modifiers, char[] packageName, char[] simpleTypeName, char[][] enclosingTypeNames, String path, AccessRestriction access) {
					if (excludePath != null && excludePath.equals(path))
						return;
					if (!findMembers && enclosingTypeNames != null && enclosingTypeNames.length > 0)
						return; // accept only top level types
					storage.acceptType(packageName, simpleTypeName, enclosingTypeNames, modifiers, access);
				}
			};

			if (monitor != null) {
				IndexManager indexManager = JavaModelManager.getIndexManager();
				if (indexManager.awaitingJobsCount() == 0) {
					// indexes were already there, so perform an immediate search to avoid any index rebuilt
					new BasicSearchEngine(this.workingCopies).searchAllTypeNames(
						qualification,
						SearchPattern.R_EXACT_MATCH,
						simpleName,
						matchRule, // not case sensitive
						searchFor,
						getSearchScope(),
						resolveDocumentName,
						typeRequestor,
						FORCE_IMMEDIATE_SEARCH,
						progressMonitor);
				} else {
					// indexes were not ready, give the indexing a chance to finish small jobs by sleeping 100ms...
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						// Do nothing
					}
					if (monitor.isCanceled()) {
						throw new OperationCanceledException();
					}
					if (indexManager.awaitingJobsCount() == 0) {
						// indexes are now ready, so perform an immediate search to avoid any index rebuilt
						new BasicSearchEngine(this.workingCopies).searchAllTypeNames(
							qualification,
							SearchPattern.R_EXACT_MATCH,
							simpleName,
							matchRule,
							searchFor,
							getSearchScope(),
							resolveDocumentName,
							typeRequestor,
							FORCE_IMMEDIATE_SEARCH,
							progressMonitor);
					} else {
						// Indexes are still not ready, so look for types in the model instead of a search request
						findTypes(
							new String(prefix),
							storage,
							convertSearchFilterToModelFilter(searchFor));
					}
				}
			} else {
				try {
					new BasicSearchEngine(this.workingCopies).searchAllTypeNames(
						qualification,
						SearchPattern.R_EXACT_MATCH,
						simpleName,
						matchRule, // not case sensitive
						searchFor,
						getSearchScope(),
						resolveDocumentName,
						typeRequestor,
						CANCEL_IF_NOT_READY_TO_SEARCH,
						progressMonitor);
				} catch (OperationCanceledException e) {
					findTypes(
						new String(prefix),
						storage,
						convertSearchFilterToModelFilter(searchFor));
				}
			}
		} catch (JavaModelException e) {
			findTypes(
				new String(prefix),
				storage,
				convertSearchFilterToModelFilter(searchFor));
		} finally {
			if (NameLookup.VERBOSE)
				this.timeSpentInFindTypes += System.currentTimeMillis()-start;
		}
	}

	/**
	 * Must be used only by CompletionEngine.
	 * The progress monitor is used to be able to cancel completion operations
	 *
	 * Find constructor declarations that are defined
	 * in the current environment and whose name starts with the
	 * given prefix. The prefix is a qualified name separated by periods
	 * or a simple name (ex. java.util.V or V).
	 *
	 * The constructors found are passed to one of the following methods:
	 *    ISearchRequestor.acceptConstructor(...)
	 */
	public void findConstructorDeclarations(char[] prefix, int matchRule, final boolean resolveDocumentName, final ISearchRequestor storage, IProgressMonitor monitor) {
		try {
			final String excludePath;
			if (this.unitToSkip != null && this.unitToSkip instanceof IJavaElement) {
				excludePath = ((IJavaElement) this.unitToSkip).getPath().toString();
			} else {
				excludePath = null;
			}

			int lastDotIndex = CharOperation.lastIndexOf('.', prefix);
			boolean camelCaseMatch = (matchRule & SearchPattern.R_CAMELCASE_MATCH) != 0;
			char[] qualification, simpleName;
			if (lastDotIndex < 0) {
				qualification = null;
				if (camelCaseMatch) {
					simpleName = prefix;
				} else {
					simpleName = CharOperation.toLowerCase(prefix);
				}
			} else {
				qualification = CharOperation.subarray(prefix, 0, lastDotIndex);
				if (camelCaseMatch) {
					simpleName = CharOperation.subarray(prefix, lastDotIndex + 1, prefix.length);
				} else {
					simpleName =
						CharOperation.toLowerCase(
							CharOperation.subarray(prefix, lastDotIndex + 1, prefix.length));
				}
			}

			IProgressMonitor progressMonitor = new IProgressMonitor() {
				boolean isCanceled = false;
				@Override
				public void beginTask(String name, int totalWork) {
					// implements interface method
				}
				@Override
				public void done() {
					// implements interface method
				}
				@Override
				public void internalWorked(double work) {
					// implements interface method
				}
				@Override
				public boolean isCanceled() {
					return this.isCanceled;
				}
				@Override
				public void setCanceled(boolean value) {
					this.isCanceled = value;
				}
				@Override
				public void setTaskName(String name) {
					// implements interface method
				}
				@Override
				public void subTask(String name) {
					// implements interface method
				}
				@Override
				public void worked(int work) {
					// implements interface method
				}
			};

			IRestrictedAccessConstructorRequestor constructorRequestor = new IRestrictedAccessConstructorRequestor() {
				@Override
				public void acceptConstructor(
						int modifiers,
						char[] simpleTypeName,
						int parameterCount,
						char[] signature,
						char[][] parameterTypes,
						char[][] parameterNames,
						int typeModifiers,
						char[] packageName,
						int extraFlags,
						String path,
						AccessRestriction access) {
					if (excludePath != null && excludePath.equals(path))
						return;

					storage.acceptConstructor(
							modifiers,
							simpleTypeName,
							parameterCount,
							signature,
							parameterTypes,
							parameterNames,
							typeModifiers,
							packageName,
							extraFlags,
							path,
							access);
				}
			};

			if (monitor != null) {
				IndexManager indexManager = JavaModelManager.getIndexManager();
				// Wait for the end of indexing or a cancel
				indexManager.performConcurrentJob(new IJob() {
					@Override
					public boolean belongsTo(String jobFamily) {
						return true;
					}

					@Override
					public void cancel() {
						// job is cancelled through progress
					}

					@Override
					public void ensureReadyToRun() {
						// always ready
					}

					@Override
					public boolean execute(IProgressMonitor progress) {
						return progress == null || !progress.isCanceled();
					}

					@Override
					public String getJobFamily() {
						return ""; //$NON-NLS-1$
					}

				}, IJob.WaitUntilReady, monitor);
				new BasicSearchEngine(this.workingCopies).searchAllConstructorDeclarations(
						qualification,
						simpleName,
						matchRule,
						getSearchScope(),
						resolveDocumentName,
						constructorRequestor,
						FORCE_IMMEDIATE_SEARCH,
						progressMonitor);
			} else {
				try {
					new BasicSearchEngine(this.workingCopies).searchAllConstructorDeclarations(
							qualification,
							simpleName,
							matchRule,
							getSearchScope(),
							resolveDocumentName,
							constructorRequestor,
							CANCEL_IF_NOT_READY_TO_SEARCH,
							progressMonitor);
				} catch (OperationCanceledException e) {
					// Do nothing
				}
			}
		} catch (JavaModelException e) {
			// Do nothing
		}
	}

	/**
	 * Returns all types whose name starts with the given (qualified) <code>prefix</code>.
	 *
	 * If the <code>prefix</code> is unqualified, all types whose simple name matches
	 * the <code>prefix</code> are returned.
	 */
	private void findTypes(String prefix, ISearchRequestor storage, int type) {
		//TODO (david) should add camel case support
		SearchableEnvironmentRequestor requestor =
			new SearchableEnvironmentRequestor(storage, this.unitToSkip, this.project, this.nameLookup);
		int index = prefix.lastIndexOf('.');
		if (index == -1) {
			this.nameLookup.seekTypes(prefix, null, true, type, requestor);
		} else {
			String packageName = prefix.substring(0, index);
			JavaElementRequestor elementRequestor = new JavaElementRequestor();
			this.nameLookup.seekPackageFragments(packageName, false, elementRequestor);
			IPackageFragment[] fragments = elementRequestor.getPackageFragments();
			if (fragments != null) {
				String className = prefix.substring(index + 1);
				for (int i = 0, length = fragments.length; i < length; i++)
					if (fragments[i] != null)
						this.nameLookup.seekTypes(className, fragments[i], true, type, requestor);
			}
		}
	}

	private IJavaSearchScope getSearchScope() {
		if (this.searchScope == null) {
			// Create search scope with visible entry on the project's classpath
			if(this.checkAccessRestrictions) {
				this.searchScope = BasicSearchEngine.createJavaSearchScope(this.excludeTestCode, new IJavaElement[] {this.project});
			} else {
				this.searchScope = BasicSearchEngine.createJavaSearchScope(this.excludeTestCode, this.nameLookup.packageFragmentRoots);
			}
		}
		return this.searchScope;
	}

	/**
	 * @see org.eclipse.jdt.internal.compiler.env.IModuleAwareNameEnvironment#getModulesDeclaringPackage(char[][], char[])
	 */
	@Override
	public char[][] getModulesDeclaringPackage(char[][] packageName, char[] moduleName) {
		long start = -1;
		if (NameLookup.VERBOSE)
			start = System.currentTimeMillis();
		try {
			String[] pkgName = Arrays.stream(packageName).map(String::new).toArray(String[]::new);
			LookupStrategy strategy = LookupStrategy.get(moduleName);
			switch (strategy) {
				case Named:
					if (this.knownModuleLocations != null) {
						IPackageFragmentRoot[] moduleContext = findModuleContext(moduleName);
						if (moduleContext != null) {
							// (this.owner != null && this.owner.isPackage(pkgName)) // TODO(SHMOD) see old isPackage
							if (this.nameLookup.isPackage(pkgName, moduleContext)) {
								return new char[][] { moduleName };
							}
						}
					}
					return null;
				case Unnamed:
				case Any:
					// if in pre-9 mode we may still search the unnamed module
					if (this.knownModuleLocations == null) {
						if ((this.owner != null && this.owner.isPackage(pkgName))
								|| this.nameLookup.isPackage(pkgName))
							return new char[][] { ModuleBinding.UNNAMED };
						return null;
					}
					//$FALL-THROUGH$
				case AnyNamed:
					char[][] names = CharOperation.NO_CHAR_CHAR;
					// narrow down candidates of roots (https://bugs.eclipse.org/566498)
					IPackageFragmentRoot[] matchingRoots = this.nameLookup.findPackageFragementRoots(pkgName);
					if(matchingRoots != null) {
						boolean containsUnnamed = false;
						for (IPackageFragmentRoot packageRoot : matchingRoots) {
							IPackageFragmentRoot[] singleton = { packageRoot };
							if (strategy.matches(singleton, locs -> locs[0] instanceof JrtPackageFragmentRoot || getModuleDescription(locs) != null)) {
								if (this.nameLookup.isPackage(pkgName, singleton)) {
									IModuleDescription moduleDescription = getModuleDescription(singleton);
									char[] aName;
									if (moduleDescription != null) {
										aName = moduleDescription.getElementName().toCharArray();
									} else {
										if (containsUnnamed)
											continue;
										containsUnnamed = true;
										aName = ModuleBinding.UNNAMED;
									}
									names = CharOperation.arrayConcat(names, aName);
								}
							}
						}
					}
					return names == CharOperation.NO_CHAR_CHAR ? null : names;
				default:
					throw new IllegalArgumentException("Unexpected LookupStrategy "+strategy); //$NON-NLS-1$
			}
		} finally {
			if (NameLookup.VERBOSE)
				this.timeSpentInGetModulesDeclaringPackage += System.currentTimeMillis()-start;
		}
	}
	@Override
	public boolean hasCompilationUnit(char[][] pkgName, char[] moduleName, boolean checkCUs) {
		LookupStrategy strategy = LookupStrategy.get(moduleName);
		switch (strategy) {
			case Named:
				if (this.knownModuleLocations != null) {
					IPackageFragmentRoot[] moduleContext = findModuleContext(moduleName);
					if (moduleContext != null) {
						// (this.owner != null && this.owner.isPackage(pkgName)) // TODO(SHMOD) see old isPackage
						if (this.nameLookup.hasCompilationUnit(pkgName, moduleContext))
							return true;
					}
				}
				return false;
			case Unnamed:
			case Any:
				// if in pre-9 mode we may still search the unnamed module
				if (this.knownModuleLocations == null) {
					if (this.nameLookup.hasCompilationUnit(pkgName, null))
						return true;
				}
				//$FALL-THROUGH$
			case AnyNamed:
				// narrow down candidates of roots (https://bugs.eclipse.org/566498)
				String[] splittedName = Util.toStrings(pkgName);
				IPackageFragmentRoot[] packageRoots = this.nameLookup.findPackageFragementRoots(splittedName);
				if(packageRoots != null) {
					for (IPackageFragmentRoot packageRoot : packageRoots) {
						IPackageFragmentRoot[] singleton = { packageRoot };
						if (strategy.matches(singleton, locs -> locs[0] instanceof JrtPackageFragmentRoot || getModuleDescription(locs) != null)) {
							if (this.nameLookup.hasCompilationUnit(pkgName, singleton))
								return true;
						}
					}
				}
				return false;
			default:
				throw new IllegalArgumentException("Unexpected LookupStrategy "+strategy); //$NON-NLS-1$
		}
	}

	private IModuleDescription getModuleDescription(IPackageFragmentRoot[] roots) {
		if (this.rootToModule == null) {
			this.rootToModule = new HashMap<>();
		}
		for (IPackageFragmentRoot root : roots) {
			IModuleDescription moduleDescription = NameLookup.getModuleDescription(this.project, root, this.rootToModule, this.nameLookup.rootToResolvedEntries::get);
			if (moduleDescription != null)
				return moduleDescription;
		}
		return null;
	}

	private IPackageFragmentRoot[] findModuleContext(char[] moduleName) {
		IPackageFragmentRoot[] moduleContext = null;
		if (this.knownModuleLocations != null && moduleName != null && moduleName.length > 0) {
			moduleContext = this.knownModuleLocations.get(String.valueOf(moduleName));
			if (moduleContext == null) {
				Answer moduleAnswer = this.nameLookup.findModule(moduleName);
				if (moduleAnswer != null) {
					IProject currentProject = moduleAnswer.module.getJavaProject().getProject();
					IJavaElement current = moduleAnswer.module.getParent();
					while (moduleContext == null && current != null) {
						switch (current.getElementType()) {
							case IJavaElement.PACKAGE_FRAGMENT_ROOT:
								if (!((IPackageFragmentRoot) current).isExternal() && !(current instanceof JarPackageFragmentRoot)) {
									current = current.getJavaProject();
								} else {
									moduleContext = new IPackageFragmentRoot[] { (IPackageFragmentRoot) current }; // TODO: validate
									break;
								}
								//$FALL-THROUGH$
							case IJavaElement.JAVA_PROJECT:
								try {
									moduleContext = getOwnedPackageFragmentRoots((IJavaProject) current);
								} catch (JavaModelException e) {
									// silent?
								}
								break;
							default:
								current = current.getParent();
								if (current != null) {
									try {
										// detect when an element refers to a resource owned by another project:
										IResource resource = current.getUnderlyingResource();
										if (resource != null) {
											IProject otherProject = resource.getProject();
											if (otherProject != null && !otherProject.equals(currentProject)) {
												IJavaProject otherJavaProject = JavaCore.create(otherProject);
												if (otherJavaProject.exists())
													moduleContext = getRootsForOutputLocation(otherJavaProject, resource);
											}
										}
									} catch (JavaModelException e) {
										Util.log(e, "Failed to find package fragment root for " + current); //$NON-NLS-1$
									}
								}
						}
					}
					this.knownModuleLocations.put(String.valueOf(moduleName), moduleContext);
				}
			}
		}
		return moduleContext;
	}

	/**
	 * Returns a printable string for the array.
	 */
	protected String toStringChar(char[] name) {
		return "["  //$NON-NLS-1$
		+ new String(name) + "]" ; //$NON-NLS-1$
	}

	/**
	 * Returns a printable string for the array.
	 */
	protected String toStringCharChar(char[][] names) {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < names.length; i++) {
			result.append(toStringChar(names[i]));
		}
		return result.toString();
	}

	@Override
	public void cleanup() {
		// nothing to do
	}

	@Override
	public org.eclipse.jdt.internal.compiler.env.IModule getModule(char[] name) {
		NameLookup.Answer answer = this.nameLookup.findModule(name);
		IModule module = null;
		if (answer != null) {
			module = NameLookup.getModuleDescriptionInfo(answer.module);
		}
		return module;
	}

	@Override
	public char[][] getAllAutomaticModules() {
		return CharOperation.NO_CHAR_CHAR;
	}

	@Override
	public void applyModuleUpdates(IUpdatableModule module, UpdateKind kind) {
		if (this.moduleUpdater != null)
			this.moduleUpdater.applyModuleUpdates(module, kind);
	}

	private IPackageFragmentRoot[] getRootsForOutputLocation(IJavaProject otherJavaProject, IResource outputLocation) throws JavaModelException {
		IPath outputPath = outputLocation.getFullPath();
		List<IPackageFragmentRoot> result = new ArrayList<>();
		if (outputPath.equals(otherJavaProject.getOutputLocation())) {
			// collect roots reporting to the default output location:
			for (IClasspathEntry classpathEntry : otherJavaProject.getRawClasspath()) {
				if (classpathEntry.getOutputLocation() == null) {
					for (IPackageFragmentRoot root : otherJavaProject.findPackageFragmentRoots(classpathEntry)) {
						IResource rootResource = root.getResource();
						if (rootResource == null || !rootResource.getProject().equals(otherJavaProject.getProject()))
							continue; // outside this project
						result.add(root);
					}
				}
			}
		}
		if (!result.isEmpty())
			return result.toArray(new IPackageFragmentRoot[result.size()]);
		// search an entry that specifically (and exclusively) reports to the output location:
		for (IClasspathEntry classpathEntry : otherJavaProject.getRawClasspath()) {
			if (outputPath.equals(classpathEntry.getOutputLocation()))
				return otherJavaProject.findPackageFragmentRoots(classpathEntry);
		}
		return null;
	}

	public static IPackageFragmentRoot[] getOwnedPackageFragmentRoots(IJavaProject javaProject) throws JavaModelException {
		IPackageFragmentRoot[] allRoots = javaProject.getPackageFragmentRoots();
		IPackageFragmentRoot[] sourceRoots = Arrays.copyOf(allRoots, allRoots.length);
		int count = 0;
		for (int i = 0; i < allRoots.length; i++) {
			IPackageFragmentRoot root = allRoots[i];
			if (root.getKind() == IPackageFragmentRoot.K_BINARY) {
				if(root instanceof JarPackageFragmentRoot) {
					// don't treat jars in a project as part of the project's module
					continue;
				}
				IResource resource = root.getResource();
				if (resource == null || !resource.getProject().equals(javaProject.getProject()))
					continue; // outside this project
			}
			sourceRoots[count++] = root;
		}
		if (count < allRoots.length)
			return Arrays.copyOf(sourceRoots, count);
		return sourceRoots;
	}

	@Override
	public char[][] listPackages(char[] moduleName) {
		switch (LookupStrategy.get(moduleName)) {
			case Named:
				IPackageFragmentRoot[] packageRoots = findModuleContext(moduleName);
				Set<String> packages = new HashSet<>();
				if (packageRoots != null) {
					for (IPackageFragmentRoot packageRoot : packageRoots) {
						try {
							for (IJavaElement javaElement : packageRoot.getChildren()) {
								if (javaElement instanceof IPackageFragment && !((IPackageFragment) javaElement).isDefaultPackage())
									packages.add(javaElement.getElementName());
							}
						} catch (JavaModelException e) {
							Util.log(e, "Failed to retrieve packages from " + packageRoot); //$NON-NLS-1$
						}
					}
				}
				return packages.stream().map(String::toCharArray).toArray(char[][]::new);
			default:
				throw new UnsupportedOperationException("can list packages only of a named module"); //$NON-NLS-1$
		}
	}

	public void printTimeSpent() {
		if(!NameLookup.VERBOSE)
			return;

		Util.verbose(" TIME SPENT SearchableEnvironment");  //$NON-NLS-1$
		Util.verbose(" -> getModulesDeclaringPackage..." +  this.timeSpentInGetModulesDeclaringPackage + "ms");  //$NON-NLS-1$ //$NON-NLS-2$
		Util.verbose(" -> findTypes...................." +  this.timeSpentInFindTypes + "ms");  //$NON-NLS-1$ //$NON-NLS-2$

		this.nameLookup.printTimeSpent();
	}
}
