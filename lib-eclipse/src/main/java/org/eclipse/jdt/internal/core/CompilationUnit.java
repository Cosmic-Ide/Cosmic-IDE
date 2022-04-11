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
 *     Alex Smirnoff (alexsmr@sympatico.ca) - part of the changes to support Java-like extension
 *                                                            (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=71460)
 *     Microsoft Corporation - support custom options at compilation unit level
 *******************************************************************************/
package org.eclipse.jdt.internal.core;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.compiler.*;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.internal.compiler.IProblemFactory;
import org.eclipse.jdt.internal.compiler.SourceElementParser;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.lookup.TypeConstants;
import org.eclipse.jdt.internal.compiler.problem.AbortCompilationUnit;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblemFactory;
import org.eclipse.jdt.internal.compiler.util.SuffixConstants;
import org.eclipse.jdt.internal.core.util.MementoTokenizer;
import org.eclipse.jdt.internal.core.util.Messages;
import org.eclipse.jdt.internal.core.util.Util;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.UndoEdit;

/**
 * @see ICompilationUnit
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class CompilationUnit extends Openable implements ICompilationUnit, org.eclipse.jdt.internal.compiler.env.ICompilationUnit, SuffixConstants {
	/**
	 * Internal synonym for deprecated constant AST.JSL2
	 * to alleviate deprecation warnings.
	 * @deprecated
	 */
	/*package*/ static final int JLS2_INTERNAL = AST.JLS2;

	private static final IImportDeclaration[] NO_IMPORTS = new IImportDeclaration[0];

	protected String name;
	public WorkingCopyOwner owner;

/**
 * Constructs a handle to a compilation unit with the given name in the
 * specified package for the specified owner
 */
public CompilationUnit(PackageFragment parent, String name, WorkingCopyOwner owner) {
	super(parent);
	this.name = name;
	this.owner = owner;
}


@Override
public UndoEdit applyTextEdit(TextEdit edit, IProgressMonitor monitor) throws JavaModelException {
	IBuffer buffer = getBuffer();
	if (buffer instanceof IBuffer.ITextEditCapability) {
		return ((IBuffer.ITextEditCapability) buffer).applyTextEdit(edit, monitor);
	} else if (buffer != null) {
		IDocument document = buffer instanceof IDocument ? (IDocument) buffer : new DocumentAdapter(buffer);
		try {
			UndoEdit undoEdit= edit.apply(document);
			return undoEdit;
		} catch (MalformedTreeException | BadLocationException e) {
			throw new JavaModelException(e, IJavaModelStatusConstants.BAD_TEXT_EDIT_LOCATION);
		}
	}
	return null; // can not happen, there are no compilation units without buffer
}

@Override
public void becomeWorkingCopy(IProblemRequestor problemRequestor, IProgressMonitor monitor) throws JavaModelException {
	JavaModelManager manager = JavaModelManager.getJavaModelManager();
	JavaModelManager.PerWorkingCopyInfo perWorkingCopyInfo = manager.getPerWorkingCopyInfo(this, false/*don't create*/, true /*record usage*/, null/*no problem requestor needed*/);
	if (perWorkingCopyInfo == null) {
		// close cu and its children
		close();

		BecomeWorkingCopyOperation operation = new BecomeWorkingCopyOperation(this, problemRequestor);
		operation.runOperation(monitor);
	}
}

@Override
public void becomeWorkingCopy(IProgressMonitor monitor) throws JavaModelException {
	IProblemRequestor requestor = this.owner == null ? null : this.owner.getProblemRequestor(this);
	becomeWorkingCopy(requestor, monitor);
}
@Override
protected boolean buildStructure(OpenableElementInfo info, final IProgressMonitor pm, Map newElements, IResource underlyingResource) throws JavaModelException {
	CompilationUnitElementInfo unitInfo = (CompilationUnitElementInfo) info;

	// ensure buffer is opened
	IBuffer buffer = getBufferManager().getBuffer(CompilationUnit.this);
	if (buffer == null) {
		openBuffer(pm, unitInfo); // open buffer independently from the info, since we are building the info
	}

	// generate structure and compute syntax problems if needed
	CompilationUnitStructureRequestor requestor = new CompilationUnitStructureRequestor(this, unitInfo, newElements);
	JavaModelManager.PerWorkingCopyInfo perWorkingCopyInfo = getPerWorkingCopyInfo();
	IJavaProject project = getJavaProject();

	boolean createAST;
	boolean resolveBindings;
	int reconcileFlags;
	HashMap problems;
	if (info instanceof ASTHolderCUInfo) {
		ASTHolderCUInfo astHolder = (ASTHolderCUInfo) info;
		createAST = astHolder.astLevel != NO_AST;
		resolveBindings = astHolder.resolveBindings;
		reconcileFlags = astHolder.reconcileFlags;
		problems = astHolder.problems;
	} else {
		createAST = false;
		resolveBindings = false;
		reconcileFlags = 0;
		problems = null;
	}

	boolean computeProblems = perWorkingCopyInfo != null && perWorkingCopyInfo.isActive() && project != null && JavaProject.hasJavaNature(project.getProject());
	IProblemFactory problemFactory = new DefaultProblemFactory();
	Map options = this.getOptions(true);
	if (!computeProblems) {
		// disable task tags checking to speed up parsing
		options.put(JavaCore.COMPILER_TASK_TAGS, ""); //$NON-NLS-1$
	}
	CompilerOptions compilerOptions = new CompilerOptions(options);
	compilerOptions.ignoreMethodBodies = (reconcileFlags & ICompilationUnit.IGNORE_METHOD_BODIES) != 0;
	SourceElementParser parser = new SourceElementParser(
		requestor,
		problemFactory,
		compilerOptions,
		true/*report local declarations*/,
		!createAST /*optimize string literals only if not creating a DOM AST*/);
	parser.reportOnlyOneSyntaxError = !computeProblems;
	parser.setMethodsFullRecovery(true);
	parser.setStatementsRecovery((reconcileFlags & ICompilationUnit.ENABLE_STATEMENTS_RECOVERY) != 0);

	if (!computeProblems && !resolveBindings && !createAST) // disable javadoc parsing if not computing problems, not resolving and not creating ast
		parser.javadocParser.checkDocComment = false;
	requestor.parser = parser;

	// update timestamp (might be IResource.NULL_STAMP if original does not exist)
	if (underlyingResource == null) {
		underlyingResource = getResource();
	}
	// underlying resource is null in the case of a working copy on a class file in a jar
	if (underlyingResource != null)
		unitInfo.timestamp = ((IFile)underlyingResource).getModificationStamp();

	// compute other problems if needed
	CompilationUnitDeclaration compilationUnitDeclaration = null;
	CompilationUnit source = cloneCachingContents();
	try {
		if (computeProblems) {
			if (problems == null) {
				// report problems to the problem requestor
				problems = new HashMap();
				compilationUnitDeclaration = CompilationUnitProblemFinder.process(source, parser, this.owner, problems, createAST, reconcileFlags, pm);
				try {
					perWorkingCopyInfo.beginReporting();
					for (Iterator iteraror = problems.values().iterator(); iteraror.hasNext();) {
						CategorizedProblem[] categorizedProblems = (CategorizedProblem[]) iteraror.next();
						if (categorizedProblems == null) continue;
						for (int i = 0, length = categorizedProblems.length; i < length; i++) {
							perWorkingCopyInfo.acceptProblem(categorizedProblems[i]);
						}
					}
				} finally {
					perWorkingCopyInfo.endReporting();
				}
			} else {
				// collect problems
				compilationUnitDeclaration = CompilationUnitProblemFinder.process(source, parser, this.owner, problems, createAST, reconcileFlags, pm);
			}
		} else {
			compilationUnitDeclaration = parser.parseCompilationUnit(source, true /*full parse to find local elements*/, pm);
		}

		if (createAST) {
			int astLevel = ((ASTHolderCUInfo) info).astLevel;
			org.eclipse.jdt.core.dom.CompilationUnit cu = AST.convertCompilationUnit(astLevel, compilationUnitDeclaration, options, computeProblems, source, reconcileFlags, pm);
			((ASTHolderCUInfo) info).ast = cu;
		}
	} finally {
	    if (compilationUnitDeclaration != null) {
	    	unitInfo.hasFunctionalTypes = compilationUnitDeclaration.hasFunctionalTypes();
	        compilationUnitDeclaration.cleanUp();
	    }
	}

	return unitInfo.isStructureKnown();
}
/*
 * Clone this handle so that it caches its contents in memory.
 * DO NOT PASS TO CLIENTS
 */
public CompilationUnit cloneCachingContents() {
	return new CompilationUnit((PackageFragment) this.getParent(), this.name, this.owner) {
		private char[] cachedContents;
		@Override
		public char[] getContents() {
			if (this.cachedContents == null)
				this.cachedContents = CompilationUnit.this.getContents();
			return this.cachedContents;
		}
		@Override
		public CompilationUnit originalFromClone() {
			return CompilationUnit.this;
		}
	};
}

@Override
public boolean canBeRemovedFromCache() {
	if (getPerWorkingCopyInfo() != null) return false; // working copies should remain in the cache until they are destroyed
	return super.canBeRemovedFromCache();
}

@Override
public boolean canBufferBeRemovedFromCache(IBuffer buffer) {
	if (getPerWorkingCopyInfo() != null) return false; // working copy buffers should remain in the cache until working copy is destroyed
	return super.canBufferBeRemovedFromCache(buffer);
}

@Override
public void close() throws JavaModelException {
	if (getPerWorkingCopyInfo() != null) return; // a working copy must remain opened until it is discarded
	super.close();
}

@Override
protected void closing(Object info) {
	if (getPerWorkingCopyInfo() == null) {
		super.closing(info);
	} // else the buffer of a working copy must remain open for the lifetime of the working copy
}
/**
 * @see ICodeAssist#codeComplete(int, ICompletionRequestor)
 * @deprecated
 */
@Override
public void codeComplete(int offset, ICompletionRequestor requestor) throws JavaModelException {
	codeComplete(offset, requestor, DefaultWorkingCopyOwner.PRIMARY);
}
/**
 * @see ICodeAssist#codeComplete(int, ICompletionRequestor, WorkingCopyOwner)
 * @deprecated
 */
@Override
public void codeComplete(int offset, ICompletionRequestor requestor, WorkingCopyOwner workingCopyOwner) throws JavaModelException {
	if (requestor == null) {
		throw new IllegalArgumentException("Completion requestor cannot be null"); //$NON-NLS-1$
	}
	codeComplete(offset, new org.eclipse.jdt.internal.codeassist.CompletionRequestorWrapper(requestor), workingCopyOwner);
}
/**
 * @see ICodeAssist#codeComplete(int, ICodeCompletionRequestor)
 * @deprecated - use codeComplete(int, ICompletionRequestor)
 */
@Override
public void codeComplete(int offset, final ICodeCompletionRequestor requestor) throws JavaModelException {

	if (requestor == null){
		codeComplete(offset, (ICompletionRequestor)null);
		return;
	}
	codeComplete(
		offset,
		new ICompletionRequestor(){
			@Override
			public void acceptAnonymousType(char[] superTypePackageName,char[] superTypeName,char[][] parameterPackageNames,char[][] parameterTypeNames,char[][] parameterNames,char[] completionName,int modifiers,int completionStart,int completionEnd, int relevance){
				// ignore
			}
			@Override
			public void acceptClass(char[] packageName, char[] className, char[] completionName, int modifiers, int completionStart, int completionEnd, int relevance) {
				requestor.acceptClass(packageName, className, completionName, modifiers, completionStart, completionEnd);
			}
			@Override
			public void acceptError(IProblem error) {
				// was disabled in 1.0
			}
			@Override
			public void acceptField(char[] declaringTypePackageName, char[] declaringTypeName, char[] fieldName, char[] typePackageName, char[] typeName, char[] completionName, int modifiers, int completionStart, int completionEnd, int relevance) {
				requestor.acceptField(declaringTypePackageName, declaringTypeName, fieldName, typePackageName, typeName, completionName, modifiers, completionStart, completionEnd);
			}
			@Override
			public void acceptInterface(char[] packageName,char[] interfaceName,char[] completionName,int modifiers,int completionStart,int completionEnd, int relevance) {
				requestor.acceptInterface(packageName, interfaceName, completionName, modifiers, completionStart, completionEnd);
			}
			@Override
			public void acceptKeyword(char[] keywordName,int completionStart,int completionEnd, int relevance){
				requestor.acceptKeyword(keywordName, completionStart, completionEnd);
			}
			@Override
			public void acceptLabel(char[] labelName,int completionStart,int completionEnd, int relevance){
				requestor.acceptLabel(labelName, completionStart, completionEnd);
			}
			@Override
			public void acceptLocalVariable(char[] localVarName,char[] typePackageName,char[] typeName,int modifiers,int completionStart,int completionEnd, int relevance){
				// ignore
			}
			@Override
			public void acceptMethod(char[] declaringTypePackageName,char[] declaringTypeName,char[] selector,char[][] parameterPackageNames,char[][] parameterTypeNames,char[][] parameterNames,char[] returnTypePackageName,char[] returnTypeName,char[] completionName,int modifiers,int completionStart,int completionEnd, int relevance){
				// skip parameter names
				requestor.acceptMethod(declaringTypePackageName, declaringTypeName, selector, parameterPackageNames, parameterTypeNames, returnTypePackageName, returnTypeName, completionName, modifiers, completionStart, completionEnd);
			}
			@Override
			public void acceptMethodDeclaration(char[] declaringTypePackageName,char[] declaringTypeName,char[] selector,char[][] parameterPackageNames,char[][] parameterTypeNames,char[][] parameterNames,char[] returnTypePackageName,char[] returnTypeName,char[] completionName,int modifiers,int completionStart,int completionEnd, int relevance){
				// ignore
			}
			@Override
			public void acceptModifier(char[] modifierName,int completionStart,int completionEnd, int relevance){
				requestor.acceptModifier(modifierName, completionStart, completionEnd);
			}
			@Override
			public void acceptPackage(char[] packageName,char[] completionName,int completionStart,int completionEnd, int relevance){
				requestor.acceptPackage(packageName, completionName, completionStart, completionEnd);
			}
			@Override
			public void acceptType(char[] packageName,char[] typeName,char[] completionName,int completionStart,int completionEnd, int relevance){
				requestor.acceptType(packageName, typeName, completionName, completionStart, completionEnd);
			}
			@Override
			public void acceptVariableName(char[] typePackageName,char[] typeName,char[] varName,char[] completionName,int completionStart,int completionEnd, int relevance){
				// ignore
			}
		});
}

@Override
public void codeComplete(int offset, CompletionRequestor requestor) throws JavaModelException {
	codeComplete(offset, requestor, DefaultWorkingCopyOwner.PRIMARY);
}

@Override
public void codeComplete(int offset, CompletionRequestor requestor, IProgressMonitor monitor) throws JavaModelException {
	codeComplete(offset, requestor, DefaultWorkingCopyOwner.PRIMARY, monitor);
}

@Override
public void codeComplete(int offset, CompletionRequestor requestor, WorkingCopyOwner workingCopyOwner) throws JavaModelException {
	codeComplete(offset, requestor, workingCopyOwner, null);
}

@Override
public void codeComplete(int offset, CompletionRequestor requestor, WorkingCopyOwner workingCopyOwner, IProgressMonitor monitor) throws JavaModelException {
	codeComplete(
			this,
			isWorkingCopy() ? (org.eclipse.jdt.internal.compiler.env.ICompilationUnit) getOriginalElement() : this,
			offset,
			requestor,
			workingCopyOwner,
			this,
			monitor);
}

/**
 * @see ICodeAssist#codeSelect(int, int)
 */
@Override
public IJavaElement[] codeSelect(int offset, int length) throws JavaModelException {
	return codeSelect(offset, length, DefaultWorkingCopyOwner.PRIMARY);
}
/**
 * @see ICodeAssist#codeSelect(int, int, WorkingCopyOwner)
 */
@Override
public IJavaElement[] codeSelect(int offset, int length, WorkingCopyOwner workingCopyOwner) throws JavaModelException {
	return super.codeSelect(this, offset, length, workingCopyOwner);
}
/**
 * @see IWorkingCopy#commit(boolean, IProgressMonitor)
 * @deprecated
 */
@Override
public void commit(boolean force, IProgressMonitor monitor) throws JavaModelException {
	commitWorkingCopy(force, monitor);
}
/**
 * @see ICompilationUnit#commitWorkingCopy(boolean, IProgressMonitor)
 */
@Override
public void commitWorkingCopy(boolean force, IProgressMonitor monitor) throws JavaModelException {
	CommitWorkingCopyOperation op= new CommitWorkingCopyOperation(this, force);
	op.runOperation(monitor);
}
/**
 * @see ISourceManipulation#copy(IJavaElement, IJavaElement, String, boolean, IProgressMonitor)
 */
@Override
public void copy(IJavaElement container, IJavaElement sibling, String rename, boolean force, IProgressMonitor monitor) throws JavaModelException {
	if (container == null) {
		throw new IllegalArgumentException(Messages.operation_nullContainer);
	}
	IJavaElement[] elements = new IJavaElement[] {this};
	IJavaElement[] containers = new IJavaElement[] {container};
	String[] renamings = null;
	if (rename != null) {
		renamings = new String[] {rename};
	}
	getJavaModel().copy(elements, containers, null, renamings, force, monitor);
}
/**
 * Returns a new element info for this element.
 */
@Override
protected Object createElementInfo() {
	return new CompilationUnitElementInfo();
}
/**
 * @see ICompilationUnit#createImport(String, IJavaElement, IProgressMonitor)
 */
@Override
public IImportDeclaration createImport(String importName, IJavaElement sibling, IProgressMonitor monitor) throws JavaModelException {
	return createImport(importName, sibling, Flags.AccDefault, monitor);
}

/**
 * @see ICompilationUnit#createImport(String, IJavaElement, int, IProgressMonitor)
 * @since 3.0
 */
@Override
public IImportDeclaration createImport(String importName, IJavaElement sibling, int flags, IProgressMonitor monitor) throws JavaModelException {
	CreateImportOperation op = new CreateImportOperation(importName, this, flags);
	if (sibling != null) {
		op.createBefore(sibling);
	}
	op.runOperation(monitor);
	return getImport(importName);
}

/**
 * @see ICompilationUnit#createPackageDeclaration(String, IProgressMonitor)
 */
@Override
public IPackageDeclaration createPackageDeclaration(String pkg, IProgressMonitor monitor) throws JavaModelException {

	CreatePackageDeclarationOperation op= new CreatePackageDeclarationOperation(pkg, this);
	op.runOperation(monitor);
	return getPackageDeclaration(pkg);
}
/**
 * @see ICompilationUnit#createType(String, IJavaElement, boolean, IProgressMonitor)
 */
@Override
public IType createType(String content, IJavaElement sibling, boolean force, IProgressMonitor monitor) throws JavaModelException {
	if (!exists()) {
		//autogenerate this compilation unit
		IPackageFragment pkg = (IPackageFragment) getParent();
		String source = ""; //$NON-NLS-1$
		if (!pkg.isDefaultPackage()) {
			//not the default package...add the package declaration
			String lineSeparator = Util.getLineSeparator(null/*no existing source*/, getJavaProject());
			source = "package " + pkg.getElementName() + ";"  + lineSeparator + lineSeparator; //$NON-NLS-1$ //$NON-NLS-2$
		}
		CreateCompilationUnitOperation op = new CreateCompilationUnitOperation(pkg, this.name, source, force);
		op.runOperation(monitor);
	}
	CreateTypeOperation op = new CreateTypeOperation(this, content, force);
	if (sibling != null) {
		op.createBefore(sibling);
	}
	op.runOperation(monitor);
	return (IType) op.getResultElements()[0];
}
/**
 * @see ISourceManipulation#delete(boolean, IProgressMonitor)
 */
@Override
public void delete(boolean force, IProgressMonitor monitor) throws JavaModelException {
	IJavaElement[] elements= new IJavaElement[] {this};
	getJavaModel().delete(elements, force, monitor);
}
/**
 * @see IWorkingCopy#destroy()
 * @deprecated
 */
@Override
public void destroy() {
	try {
		discardWorkingCopy();
	} catch (JavaModelException e) {
		if (JavaModelManager.VERBOSE)
			e.printStackTrace();
	}
}

@Override
public void discardWorkingCopy() throws JavaModelException {
	// discard working copy and its children
	DiscardWorkingCopyOperation op = new DiscardWorkingCopyOperation(this);
	op.runOperation(null);
}
/**
 * Returns true if this handle represents the same Java element
 * as the given handle.
 *
 * @see Object#equals(java.lang.Object)
 */
@Override
public boolean equals(Object obj) {
	if (!(obj instanceof CompilationUnit)) return false;
	CompilationUnit other = (CompilationUnit)obj;
	return this.owner.equals(other.owner) && super.equals(obj);
}
/**
 * @see ICompilationUnit#findElements(IJavaElement)
 */
@Override
public IJavaElement[] findElements(IJavaElement element) {
	if (element instanceof IType && ((IType) element).isLambda()) {
		return null;
	}
	ArrayList children = new ArrayList();
	while (element != null && element.getElementType() != IJavaElement.COMPILATION_UNIT) {
		children.add(element);
		element = element.getParent();
	}
	if (element == null) return null;
	IJavaElement currentElement = this;
	for (int i = children.size()-1; i >= 0; i--) {
		SourceRefElement child = (SourceRefElement)children.get(i);
		switch (child.getElementType()) {
			case IJavaElement.PACKAGE_DECLARATION:
				currentElement = ((ICompilationUnit)currentElement).getPackageDeclaration(child.getElementName());
				break;
			case IJavaElement.IMPORT_CONTAINER:
				currentElement = ((ICompilationUnit)currentElement).getImportContainer();
				break;
			case IJavaElement.IMPORT_DECLARATION:
				currentElement = ((IImportContainer)currentElement).getImport(child.getElementName());
				break;
			case IJavaElement.TYPE:
				switch (currentElement.getElementType()) {
					case IJavaElement.COMPILATION_UNIT:
						currentElement = ((ICompilationUnit)currentElement).getType(child.getElementName());
						break;
					case IJavaElement.TYPE:
						currentElement = ((IType)currentElement).getType(child.getElementName());
						break;
					case IJavaElement.FIELD:
					case IJavaElement.INITIALIZER:
					case IJavaElement.METHOD:
						currentElement =  ((IMember)currentElement).getType(child.getElementName(), child.occurrenceCount);
						break;
				}
				break;
			case IJavaElement.INITIALIZER:
				currentElement = ((IType)currentElement).getInitializer(child.occurrenceCount);
				break;
			case IJavaElement.FIELD:
				currentElement = ((IType)currentElement).getField(child.getElementName());
				break;
			case IJavaElement.METHOD:
				currentElement = ((IType)currentElement).getMethod(child.getElementName(), ((IMethod)child).getParameterTypes());
				break;
		}

	}
	if (currentElement != null && currentElement.exists()) {
		return new IJavaElement[] {currentElement};
	} else {
		return null;
	}
}
/**
 * @see ICompilationUnit#findPrimaryType()
 */
@Override
public IType findPrimaryType() {
	String typeName = Util.getNameWithoutJavaLikeExtension(getElementName());
	IType primaryType= getType(typeName);
	if (primaryType.exists()) {
		return primaryType;
	}
	return null;
}

/**
 * @see IWorkingCopy#findSharedWorkingCopy(IBufferFactory)
 * @deprecated
 */
@Override
public IJavaElement findSharedWorkingCopy(IBufferFactory factory) {

	// if factory is null, default factory must be used
	if (factory == null) factory = getBufferManager().getDefaultBufferFactory();

	return findWorkingCopy(BufferFactoryWrapper.create(factory));
}

/**
 * @see ICompilationUnit#findWorkingCopy(WorkingCopyOwner)
 */
@Override
public ICompilationUnit findWorkingCopy(WorkingCopyOwner workingCopyOwner) {
	CompilationUnit cu = new CompilationUnit((PackageFragment)this.getParent(), getElementName(), workingCopyOwner);
	if (workingCopyOwner == DefaultWorkingCopyOwner.PRIMARY) {
		return cu;
	} else {
		// must be a working copy
		JavaModelManager.PerWorkingCopyInfo perWorkingCopyInfo = cu.getPerWorkingCopyInfo();
		if (perWorkingCopyInfo != null) {
			return perWorkingCopyInfo.getWorkingCopy();
		} else {
			return null;
		}
	}
}
/**
 * @see ICompilationUnit#getAllTypes()
 */
@Override
public IType[] getAllTypes() throws JavaModelException {
	IJavaElement[] types = getTypes();
	ArrayList allTypes = new ArrayList(types.length);
	ArrayList typesToTraverse = new ArrayList(types.length);
	Collections.addAll(typesToTraverse, types);
	while (!typesToTraverse.isEmpty()) {
		IType type = (IType) typesToTraverse.get(0);
		typesToTraverse.remove(type);
		allTypes.add(type);
		types = type.getTypes();
		Collections.addAll(typesToTraverse, types);
	}
	IType[] arrayOfAllTypes = new IType[allTypes.size()];
	allTypes.toArray(arrayOfAllTypes);
	return arrayOfAllTypes;
}
/**
 * @see IMember#getCompilationUnit()
 */
@Override
public CompilationUnit getCompilationUnit() {
	return this;
}
/**
 * @see org.eclipse.jdt.internal.compiler.env.ICompilationUnit#getContents()
 */
@Override
public char[] getContents() {
	IBuffer buffer = getBufferManager().getBuffer(this);
	char[] contents = null;
	if (buffer != null) {
		contents = buffer.getCharacters();
	}
	if (buffer == null || (!isWorkingCopy() && contents == null && buffer.isClosed())) {
		// no need to force opening of CU to get the content
		// also this cannot be a working copy, as its buffer is never closed while the working copy is alive
		IFile file = (IFile) getResource();
		// Get encoding from file
		String encoding;
		try {
			encoding = file.getCharset();
		} catch(CoreException ce) {
			// do not use any encoding
			encoding = null;
		}
		try {
			return Util.getResourceContentsAsCharArray(file, encoding);
		} catch (JavaModelException e) {
			if (JavaModelManager.getJavaModelManager().abortOnMissingSource.get() == Boolean.TRUE) {
				IOException ioException =
					e.getJavaModelStatus().getCode() == IJavaModelStatusConstants.IO_EXCEPTION ?
						(IOException)e.getException() :
						new IOException(e.getMessage());
				throw new AbortCompilationUnit(null, ioException, encoding);
			} else {
				Util.log(e);
			}
			return CharOperation.NO_CHAR;
		}
	}
	if (contents == null) { // see https://bugs.eclipse.org/bugs/show_bug.cgi?id=129814
		if (JavaModelManager.getJavaModelManager().abortOnMissingSource.get() == Boolean.TRUE) {
			IOException ioException = new IOException(Messages.buffer_closed);
			IFile file = (IFile) getResource();
			// Get encoding from file
			String encoding;
			try {
				encoding = file.getCharset();
			} catch(CoreException ce) {
				// do not use any encoding
				encoding = null;
			}
			throw new AbortCompilationUnit(null, ioException, encoding);
		}
		return CharOperation.NO_CHAR;
	}
	return contents;
}
/**
 * A compilation unit has a corresponding resource unless it is contained
 * in a jar.
 *
 * @see IJavaElement#getCorrespondingResource()
 */
@Override
public IResource getCorrespondingResource() throws JavaModelException {
	PackageFragmentRoot root = getPackageFragmentRoot();
	if (root == null || root.isArchive()) {
		return null;
	} else {
		return getUnderlyingResource();
	}
}
/**
 * @see ICompilationUnit#getElementAt(int)
 */
@Override
public IJavaElement getElementAt(int position) throws JavaModelException {

	IJavaElement e= getSourceElementAt(position);
	if (e == this) {
		return null;
	} else {
		return e;
	}
}
@Override
public String getElementName() {
	return this.name;
}
/**
 * @see IJavaElement
 */
@Override
public int getElementType() {
	return COMPILATION_UNIT;
}
/**
 * @see org.eclipse.jdt.internal.compiler.env.IDependent#getFileName()
 */
@Override
public char[] getFileName(){
	return getPath().toString().toCharArray();
}

@Override
public IJavaElement getHandleFromMemento(String token, MementoTokenizer memento, WorkingCopyOwner workingCopyOwner) {
	switch (token.charAt(0)) {
		case JEM_IMPORTDECLARATION:
			JavaElement container = getImportContainer();
			return container.getHandleFromMemento(token, memento, workingCopyOwner);
		case JEM_PACKAGEDECLARATION:
			if (!memento.hasMoreTokens()) return this;
			String pkgName = memento.nextToken();
			JavaElement pkgDecl = getPackageDeclaration(pkgName);
			return pkgDecl.getHandleFromMemento(memento, workingCopyOwner);
		case JEM_TYPE:
			if (!memento.hasMoreTokens()) return this;
			String typeName = memento.nextToken();
			JavaElement type = (JavaElement)getType(typeName);
			return type.getHandleFromMemento(memento, workingCopyOwner);
		case JEM_MODULE:
			if (!memento.hasMoreTokens()) return this;
			String modName = memento.nextToken();
			JavaElement mod = new SourceModule(this, modName);
			return mod.getHandleFromMemento(memento, workingCopyOwner);
	}
	return null;
}

/**
 * @see JavaElement#getHandleMementoDelimiter()
 */
@Override
protected char getHandleMementoDelimiter() {
	return JavaElement.JEM_COMPILATIONUNIT;
}
/**
 * @see ICompilationUnit#getImport(String)
 */
@Override
public ImportDeclaration getImport(String importName) {
	return getImportContainer().getImport(importName);
}
/**
 * @see ICompilationUnit#getImportContainer()
 */
@Override
public ImportContainer getImportContainer() {
	return new ImportContainer(this);
}


/**
 * @see ICompilationUnit#getImports()
 */
@Override
public IImportDeclaration[] getImports() throws JavaModelException {
	IImportContainer container= getImportContainer();
	JavaModelManager manager = JavaModelManager.getJavaModelManager();
	Object info = manager.getInfo(container);
	if (info == null) {
		if (manager.getInfo(this) != null)
			// CU was opened, but no import container, then no imports
			return NO_IMPORTS;
		else {
			open(null); // force opening of CU
			info = manager.getInfo(container);
			if (info == null)
				// after opening, if no import container, then no imports
				return NO_IMPORTS;
		}
	}
	IJavaElement[] elements = ((ImportContainerInfo) info).children;
	int length = elements.length;
	IImportDeclaration[] imports = new IImportDeclaration[length];
	System.arraycopy(elements, 0, imports, 0, length);
	return imports;
}
/**
 * @see IMember#getTypeRoot()
 */
public ITypeRoot getTypeRoot() {
	return this;
}
/**
 * @see org.eclipse.jdt.internal.compiler.env.ICompilationUnit#getMainTypeName()
 */
@Override
public char[] getMainTypeName(){
	return Util.getNameWithoutJavaLikeExtension(getElementName()).toCharArray();
}
/**
 * @see IWorkingCopy#getOriginal(IJavaElement)
 * @deprecated
 */
@Override
public IJavaElement getOriginal(IJavaElement workingCopyElement) {
	// backward compatibility
	if (!isWorkingCopy()) return null;
	CompilationUnit cu = (CompilationUnit)workingCopyElement.getAncestor(COMPILATION_UNIT);
	if (cu == null || !this.owner.equals(cu.owner)) {
		return null;
	}

	return workingCopyElement.getPrimaryElement();
}
/**
 * @see IWorkingCopy#getOriginalElement()
 * @deprecated
 */
@Override
public IJavaElement getOriginalElement() {
	// backward compatibility
	if (!isWorkingCopy()) return null;

	return getPrimaryElement();
}

@Override
public WorkingCopyOwner getOwner() {
	return isPrimary() || !isWorkingCopy() ? null : this.owner;
}
/**
 * @see ICompilationUnit#getPackageDeclaration(String)
 */
@Override
public PackageDeclaration getPackageDeclaration(String pkg) {
	return new PackageDeclaration(this, pkg);
}
/**
 * @see ICompilationUnit#getPackageDeclarations()
 */
@Override
public IPackageDeclaration[] getPackageDeclarations() throws JavaModelException {
	ArrayList list = getChildrenOfType(PACKAGE_DECLARATION);
	IPackageDeclaration[] array= new IPackageDeclaration[list.size()];
	list.toArray(array);
	return array;
}
/**
 * @see org.eclipse.jdt.internal.compiler.env.ICompilationUnit#getPackageName()
 */
@Override
public char[][] getPackageName() {
	PackageFragment packageFragment = (PackageFragment) getParent();
	if (packageFragment == null) return CharOperation.NO_CHAR_CHAR;
	return Util.toCharArrays(packageFragment.names);
}

/**
 * @see IJavaElement#getPath()
 */
@Override
public IPath getPath() {
	PackageFragmentRoot root = getPackageFragmentRoot();
	if (root == null) return new Path(getElementName()); // working copy not in workspace
	if (root.isArchive()) {
		return root.getPath();
	} else {
		return getParent().getPath().append(getElementName());
	}
}
/*
 * Returns the per working copy info for the receiver, or null if none exist.
 * Note: the use count of the per working copy info is NOT incremented.
 */
public JavaModelManager.PerWorkingCopyInfo getPerWorkingCopyInfo() {
	return JavaModelManager.getJavaModelManager().getPerWorkingCopyInfo(this, false/*don't create*/, false/*don't record usage*/, null/*no problem requestor needed*/);
}

@Override
public ICompilationUnit getPrimary() {
	return (ICompilationUnit)getPrimaryElement(true);
}

@Override
public JavaElement getPrimaryElement(boolean checkOwner) {
	if (checkOwner && isPrimary()) return this;
	return new CompilationUnit((PackageFragment)getParent(), getElementName(), DefaultWorkingCopyOwner.PRIMARY);
}

@Override
public IResource resource(PackageFragmentRoot root) {
	if (root == null) return null; // working copy not in workspace
	return ((IContainer) ((Openable) this.getParent()).resource(root)).getFile(new Path(getElementName()));
}
/**
 * @see ISourceReference#getSource()
 */
@Override
public String getSource() throws JavaModelException {
	IBuffer buffer = getBuffer();
	if (buffer == null) return ""; //$NON-NLS-1$
	return buffer.getContents();
}
/**
 * @see ISourceReference#getSourceRange()
 */
@Override
public ISourceRange getSourceRange() throws JavaModelException {
	return ((CompilationUnitElementInfo) getElementInfo()).getSourceRange();
}
/**
 * @see ICompilationUnit#getType(String)
 */
@Override
public IType getType(String typeName) {
	return new SourceType(this, typeName);
}
/**
 * @see ICompilationUnit#getTypes()
 */
@Override
public IType[] getTypes() throws JavaModelException {
	ArrayList list = getChildrenOfType(TYPE);
	IType[] array= new IType[list.size()];
	list.toArray(array);
	return array;
}
/**
 * @see IJavaElement
 */
@Override
public IResource getUnderlyingResource() throws JavaModelException {
	if (isWorkingCopy() && !isPrimary()) return null;
	return super.getUnderlyingResource();
}
/**
 * @see IWorkingCopy#getSharedWorkingCopy(IProgressMonitor, IBufferFactory, IProblemRequestor)
 * @deprecated
 */
@Override
public IJavaElement getSharedWorkingCopy(IProgressMonitor pm, IBufferFactory factory, IProblemRequestor problemRequestor) throws JavaModelException {

	// if factory is null, default factory must be used
	if (factory == null) factory = getBufferManager().getDefaultBufferFactory();

	return getWorkingCopy(BufferFactoryWrapper.create(factory), problemRequestor, pm);
}
/**
 * @see IWorkingCopy#getWorkingCopy()
 * @deprecated
 */
@Override
public IJavaElement getWorkingCopy() throws JavaModelException {
	return getWorkingCopy(null);
}
/**
 * @see ICompilationUnit#getWorkingCopy(IProgressMonitor)
 */
@Override
public ICompilationUnit getWorkingCopy(IProgressMonitor monitor) throws JavaModelException {
	return getWorkingCopy(new WorkingCopyOwner() {/*non shared working copy*/}, null/*no problem requestor*/, monitor);
}
/**
 * @see ITypeRoot#getWorkingCopy(WorkingCopyOwner, IProgressMonitor)
 */
@Override
public ICompilationUnit getWorkingCopy(WorkingCopyOwner workingCopyOwner, IProgressMonitor monitor) throws JavaModelException {
	return getWorkingCopy(workingCopyOwner, null, monitor);
}
/**
 * @see IWorkingCopy#getWorkingCopy(IProgressMonitor, IBufferFactory, IProblemRequestor)
 * @deprecated
 */
@Override
public IJavaElement getWorkingCopy(IProgressMonitor monitor, IBufferFactory factory, IProblemRequestor problemRequestor) throws JavaModelException {
	return getWorkingCopy(BufferFactoryWrapper.create(factory), problemRequestor, monitor);
}
/**
 * @see ICompilationUnit#getWorkingCopy(WorkingCopyOwner, IProblemRequestor, IProgressMonitor)
 * @deprecated
 */
@Override
public ICompilationUnit getWorkingCopy(WorkingCopyOwner workingCopyOwner, IProblemRequestor problemRequestor, IProgressMonitor monitor) throws JavaModelException {
	if (!isPrimary()) return this;

	JavaModelManager manager = JavaModelManager.getJavaModelManager();

	CompilationUnit workingCopy = new CompilationUnit((PackageFragment)getParent(), getElementName(), workingCopyOwner);
	JavaModelManager.PerWorkingCopyInfo perWorkingCopyInfo =
		manager.getPerWorkingCopyInfo(workingCopy, false/*don't create*/, true/*record usage*/, null/*not used since don't create*/);
	if (perWorkingCopyInfo != null) {
		return perWorkingCopyInfo.getWorkingCopy(); // return existing handle instead of the one created above
	}
	BecomeWorkingCopyOperation op = new BecomeWorkingCopyOperation(workingCopy, problemRequestor);
	op.runOperation(monitor);
	return workingCopy;
}
/**
 * @see Openable#hasBuffer()
 */
@Override
protected boolean hasBuffer() {
	return true;
}

@Override
public boolean hasResourceChanged() {
	if (!isWorkingCopy()) return false;

	// if resource got deleted, then #getModificationStamp() will answer IResource.NULL_STAMP, which is always different from the cached
	// timestamp
	Object info = JavaModelManager.getJavaModelManager().getInfo(this);
	if (info == null) return false;
	IResource resource = getResource();
	if (resource == null) return false;
	return ((CompilationUnitElementInfo)info).timestamp != resource.getModificationStamp();
}
@Override
public boolean ignoreOptionalProblems() {
	return getPackageFragmentRoot().ignoreOptionalProblems();
}
/**
 * @see IWorkingCopy#isBasedOn(IResource)
 * @deprecated
 */
@Override
public boolean isBasedOn(IResource resource) {
	if (!isWorkingCopy()) return false;
	if (!getResource().equals(resource)) return false;
	return !hasResourceChanged();
}
/**
 * @see IOpenable#isConsistent()
 */
@Override
public boolean isConsistent() {
	return !JavaModelManager.getJavaModelManager().getElementsOutOfSynchWithBuffers().contains(this);
}
public boolean isPrimary() {
	return this.owner == DefaultWorkingCopyOwner.PRIMARY;
}
/**
 * @see Openable#isSourceElement()
 */
@Override
protected boolean isSourceElement() {
	return true;
}
protected IStatus validateCompilationUnit(IResource resource) {
	IPackageFragmentRoot root = getPackageFragmentRoot();
	// root never null as validation is not done for working copies
	try {
		if (root.getKind() != IPackageFragmentRoot.K_SOURCE)
			return new JavaModelStatus(IJavaModelStatusConstants.INVALID_ELEMENT_TYPES, root);
	} catch (JavaModelException e) {
		return e.getJavaModelStatus();
	}
	if (resource != null) {
		char[][] inclusionPatterns = ((PackageFragmentRoot)root).fullInclusionPatternChars();
		char[][] exclusionPatterns = ((PackageFragmentRoot)root).fullExclusionPatternChars();
		if (Util.isExcluded(resource, inclusionPatterns, exclusionPatterns))
			return new JavaModelStatus(IJavaModelStatusConstants.ELEMENT_NOT_ON_CLASSPATH, this);
		if (!resource.isAccessible())
			return new JavaModelStatus(IJavaModelStatusConstants.ELEMENT_DOES_NOT_EXIST, this);
	}
	IJavaProject project = getJavaProject();
	return JavaConventions.validateCompilationUnitName(getElementName(),project.getOption(JavaCore.COMPILER_SOURCE, true), project.getOption(JavaCore.COMPILER_COMPLIANCE, true));
}

@Override
public boolean isWorkingCopy() {
	// For backward compatibility, non primary working copies are always returning true; in removal
	// delta, clients can still check that element was a working copy before being discarded.
	return !isPrimary() || getPerWorkingCopyInfo() != null;
}
/**
 * @see IOpenable#makeConsistent(IProgressMonitor)
 */
@Override
public void makeConsistent(IProgressMonitor monitor) throws JavaModelException {
	makeConsistent(NO_AST, false/*don't resolve bindings*/, 0 /* don't perform statements recovery */, null/*don't collect problems but report them*/, monitor);
}
public org.eclipse.jdt.core.dom.CompilationUnit makeConsistent(int astLevel, boolean resolveBindings, int reconcileFlags, HashMap problems, IProgressMonitor monitor) throws JavaModelException {
	if (isConsistent()) return null;

	try {
		JavaModelManager.getJavaModelManager().abortOnMissingSource.set(Boolean.TRUE);
		// create a new info and make it the current info
		// (this will remove the info and its children just before storing the new infos)
		if (astLevel != NO_AST || problems != null) {
			ASTHolderCUInfo info = new ASTHolderCUInfo();
			info.astLevel = astLevel;
			info.resolveBindings = resolveBindings;
			info.reconcileFlags = reconcileFlags;
			info.problems = problems;
			openWhenClosed(info, true, monitor);
			org.eclipse.jdt.core.dom.CompilationUnit result = info.ast;
			info.ast = null;
			return result;
		} else {
			openWhenClosed(createElementInfo(), true, monitor);
			return null;
		}
	} finally {
		JavaModelManager.getJavaModelManager().abortOnMissingSource.remove();
	}
}
/**
 * @see ISourceManipulation#move(IJavaElement, IJavaElement, String, boolean, IProgressMonitor)
 */
@Override
public void move(IJavaElement container, IJavaElement sibling, String rename, boolean force, IProgressMonitor monitor) throws JavaModelException {
	if (container == null) {
		throw new IllegalArgumentException(Messages.operation_nullContainer);
	}
	IJavaElement[] elements= new IJavaElement[] {this};
	IJavaElement[] containers= new IJavaElement[] {container};

	String[] renamings= null;
	if (rename != null) {
		renamings= new String[] {rename};
	}
	getJavaModel().move(elements, containers, null, renamings, force, monitor);
}

/**
 * @see Openable#openBuffer(IProgressMonitor, Object)
 */
@Override
protected IBuffer openBuffer(IProgressMonitor pm, Object info) throws JavaModelException {

	// create buffer
	BufferManager bufManager = getBufferManager();
	boolean isWorkingCopy = isWorkingCopy();
	IBuffer buffer =
		isWorkingCopy
			? this.owner.createBuffer(this)
			: BufferManager.createBuffer(this);
	if (buffer == null) return null;

	ICompilationUnit original = null;
	boolean mustSetToOriginalContent = false;
	if (isWorkingCopy) {
		// ensure that isOpen() is called outside the bufManager synchronized block
		// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=237772
		mustSetToOriginalContent = !isPrimary() && (original = new CompilationUnit((PackageFragment)getParent(), getElementName(), DefaultWorkingCopyOwner.PRIMARY)).isOpen() ;
	}

	// synchronize to ensure that 2 threads are not putting 2 different buffers at the same time
	// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=146331
	IBuffer existingBuffer;
	synchronized(bufManager) {
		existingBuffer = bufManager.getBuffer(this);
		if (existingBuffer == null) {
			// set the buffer source
			if (buffer.getCharacters() == null) {
				if (isWorkingCopy) {
					if (mustSetToOriginalContent) {
						buffer.setContents(original.getSource());
					} else {
						IFile file = (IFile)getResource();
						if (file == null || !file.exists()) {
							// initialize buffer with empty contents
							buffer.setContents(CharOperation.NO_CHAR);
						} else {
							buffer.setContents(Util.getResourceContentsAsCharArray(file));
						}
					}
				} else {
					IFile file = (IFile)getResource();
					if (file == null || !file.exists()) throw newNotPresentException();
					buffer.setContents(Util.getResourceContentsAsCharArray(file));
				}
			}

			// add buffer to buffer cache
			// note this may cause existing buffers to be removed from the buffer cache, but only primary compilation unit's buffer
			// can be closed, thus no call to a client's IBuffer#close() can be done in this synchronized block.
			bufManager.addBuffer(buffer);

			// listen to buffer changes
			buffer.addBufferChangedListener(this);
		}
	}
	if(existingBuffer != null) {
		buffer.close();
		return existingBuffer;
	}
	return buffer;
}
@Override
protected void openAncestors(HashMap newElements, IProgressMonitor monitor) throws JavaModelException {
	if (!isWorkingCopy()) {
		super.openAncestors(newElements, monitor);
	}
	// else don't open ancestors for a working copy to speed up the first becomeWorkingCopy
	// (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=89411)
}
/*
 * @see #cloneCachingContents()
 */
public CompilationUnit originalFromClone() {
	return this;
}
/**
 * @see ICompilationUnit#reconcile()
 * @deprecated
 */
@Override
public IMarker[] reconcile() throws JavaModelException {
	reconcile(NO_AST, false/*don't force problem detection*/, false, null/*use primary owner*/, null/*no progress monitor*/);
	return null;
}
/**
 * @see ICompilationUnit#reconcile(int, boolean, WorkingCopyOwner, IProgressMonitor)
 */
@Override
public void reconcile(boolean forceProblemDetection, IProgressMonitor monitor) throws JavaModelException {
	reconcile(NO_AST, forceProblemDetection? ICompilationUnit.FORCE_PROBLEM_DETECTION : 0, null/*use primary owner*/, monitor);
}

/**
 * @see ICompilationUnit#reconcile(int, boolean, WorkingCopyOwner, IProgressMonitor)
 * @since 3.0
 */
@Override
public org.eclipse.jdt.core.dom.CompilationUnit reconcile(
		int astLevel,
		boolean forceProblemDetection,
		WorkingCopyOwner workingCopyOwner,
		IProgressMonitor monitor) throws JavaModelException {
	return reconcile(astLevel, forceProblemDetection? ICompilationUnit.FORCE_PROBLEM_DETECTION : 0, workingCopyOwner, monitor);
}

/**
 * @see ICompilationUnit#reconcile(int, boolean, WorkingCopyOwner, IProgressMonitor)
 * @since 3.0
 */
@Override
public org.eclipse.jdt.core.dom.CompilationUnit reconcile(
		int astLevel,
		boolean forceProblemDetection,
		boolean enableStatementsRecovery,
		WorkingCopyOwner workingCopyOwner,
		IProgressMonitor monitor) throws JavaModelException {
	int flags = 0;
	if (forceProblemDetection) flags |= ICompilationUnit.FORCE_PROBLEM_DETECTION;
	if (enableStatementsRecovery) flags |= ICompilationUnit.ENABLE_STATEMENTS_RECOVERY;
	return reconcile(astLevel, flags, workingCopyOwner, monitor);
}

@Override
public org.eclipse.jdt.core.dom.CompilationUnit reconcile(
		int astLevel,
		int reconcileFlags,
		WorkingCopyOwner workingCopyOwner,
		IProgressMonitor monitor)
		throws JavaModelException {

	if (!isWorkingCopy()) return null; // Reconciling is not supported on non working copies
	if (workingCopyOwner == null) workingCopyOwner = DefaultWorkingCopyOwner.PRIMARY;


	PerformanceStats stats = null;
	if(ReconcileWorkingCopyOperation.PERF) {
		stats = PerformanceStats.getStats(JavaModelManager.RECONCILE_PERF, this);
		stats.startRun(new String(getFileName()));
	}
	ReconcileWorkingCopyOperation op = new ReconcileWorkingCopyOperation(this, astLevel, reconcileFlags, workingCopyOwner);
	JavaModelManager manager = JavaModelManager.getJavaModelManager();
	try {
		manager.cacheZipFiles(this); // cache zip files for performance (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=134172)
		op.runOperation(monitor);
	} finally {
		manager.flushZipFiles(this);
	}
	if(ReconcileWorkingCopyOperation.PERF) {
		stats.endRun();
	}
	return op.ast;
}

/**
 * @see ISourceManipulation#rename(String, boolean, IProgressMonitor)
 */
@Override
public void rename(String newName, boolean force, IProgressMonitor monitor) throws JavaModelException {
	if (newName == null) {
		throw new IllegalArgumentException(Messages.operation_nullName);
	}
	IJavaElement[] elements= new IJavaElement[] {this};
	IJavaElement[] dests= new IJavaElement[] {getParent()};
	String[] renamings= new String[] {newName};
	getJavaModel().rename(elements, dests, renamings, force, monitor);
}

@Override
public void restore() throws JavaModelException {

	if (!isWorkingCopy()) return;

	CompilationUnit original = (CompilationUnit) getOriginalElement();
	IBuffer buffer = getBuffer();
	if (buffer == null) return;
	buffer.setContents(original.getContents());
	updateTimeStamp(original);
	makeConsistent(null);
}
/**
 * @see IOpenable
 */
@Override
public void save(IProgressMonitor pm, boolean force) throws JavaModelException {
	if (isWorkingCopy()) {
		// no need to save the buffer for a working copy (this is a noop)
		reconcile();   // not simply makeConsistent, also computes fine-grain deltas
								// in case the working copy is being reconciled already (if not it would miss
								// one iteration of deltas).
	} else {
		super.save(pm, force);
	}
}
/**
 * Debugging purposes
 */
@Override
protected void toStringInfo(int tab, StringBuffer buffer, Object info, boolean showResolvedInfo) {
	if (!isPrimary()) {
		buffer.append(tabString(tab));
		buffer.append("[Working copy] "); //$NON-NLS-1$
		toStringName(buffer);
	} else {
		if (isWorkingCopy()) {
			buffer.append(tabString(tab));
			buffer.append("[Working copy] "); //$NON-NLS-1$
			toStringName(buffer);
			if (info == null) {
				buffer.append(" (not open)"); //$NON-NLS-1$
			}
		} else {
			super.toStringInfo(tab, buffer, info, showResolvedInfo);
		}
	}
}
/*
 * Assume that this is a working copy
 */
protected void updateTimeStamp(CompilationUnit original) throws JavaModelException {
	long timeStamp =
		((IFile) original.getResource()).getModificationStamp();
	if (timeStamp == IResource.NULL_STAMP) {
		throw new JavaModelException(
			new JavaModelStatus(IJavaModelStatusConstants.INVALID_RESOURCE));
	}
	((CompilationUnitElementInfo) getElementInfo()).timestamp = timeStamp;
}

@Override
protected IStatus validateExistence(IResource underlyingResource) {
	// check if this compilation unit can be opened
	if (!isWorkingCopy()) { // no check is done on root kind or exclusion pattern for working copies
		IStatus status = validateCompilationUnit(underlyingResource);
		if (!status.isOK())
			return status;
	}

	// prevents reopening of non-primary working copies (they are closed when they are discarded and should not be reopened)
	if (!isPrimary() && getPerWorkingCopyInfo() == null) {
		return newDoesNotExistStatus();
	}

	return JavaModelStatus.VERIFIED_OK;
}

@Override
public ISourceRange getNameRange() {
	return null;
}


@Override
public IModuleDescription getModule() throws JavaModelException {
	if (TypeConstants.MODULE_INFO_FILE_NAME_STRING.equals(getElementName()))
		return ((CompilationUnitElementInfo) getElementInfo()).getModule();
	return null;
}

@Override
public char[] getModuleName() {
	try {
		IModuleDescription module = getModule();
		if (module == null) {
			JavaProject project = (JavaProject) getAncestor(IJavaElement.JAVA_PROJECT);
			module = project.getModuleDescription();
		}
		if (module != null)
			return module.getElementName().toCharArray();
	} catch (JavaModelException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	return null;
}

@Override
public void setOptions(Map<String, String> newOptions) {
	Map<String, String> customOptions = newOptions == null ? null : new ConcurrentHashMap<String, String>(newOptions);
	try {
		this.getCompilationUnitElementInfo().setCustomOptions(customOptions);
	} catch (JavaModelException e) {
		// do nothing
	}
}

@Override
public Map<String, String> getCustomOptions() {
	try {
		Map<String, String> customOptions = this.getCompilationUnitElementInfo().getCustomOptions();
		return customOptions == null ? Collections.emptyMap() : customOptions;
	} catch (JavaModelException e) {
		// do nothing
	}

	return Collections.emptyMap();
}

private CompilationUnitElementInfo getCompilationUnitElementInfo() throws JavaModelException {
	return (CompilationUnitElementInfo) this.getElementInfo();
}
}
