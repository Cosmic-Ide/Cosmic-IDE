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
 *								Bug 377883 - NPE on open Call Hierarchy
 *     Microsoft Corporation - Contribution for bug 575562 - improve completion search performance
 *******************************************************************************/
package org.eclipse.jdt.internal.core.search.matching;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IAnnotatable;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModelStatusConstants;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IModuleDescription;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.IOrdinaryClassFile;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.compiler.*;
import org.eclipse.jdt.core.search.*;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.DefaultErrorHandlingPolicies;
import org.eclipse.jdt.internal.compiler.ast.*;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFormatException;
import org.eclipse.jdt.internal.compiler.env.*;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.impl.ITypeRequestor;
import org.eclipse.jdt.internal.compiler.lookup.*;
import org.eclipse.jdt.internal.compiler.parser.*;
import org.eclipse.jdt.internal.compiler.problem.*;
import org.eclipse.jdt.internal.compiler.util.HashtableOfIntValues;
import org.eclipse.jdt.internal.compiler.util.Messages;
import org.eclipse.jdt.internal.compiler.util.SimpleLookupTable;
import org.eclipse.jdt.internal.compiler.util.SimpleSet;
import org.eclipse.jdt.internal.compiler.util.SuffixConstants;
import org.eclipse.jdt.internal.core.hierarchy.HierarchyResolver;
import org.eclipse.jdt.internal.core.AbstractModule;
import org.eclipse.jdt.internal.core.BinaryMember;
import org.eclipse.jdt.internal.core.BinaryMethod;
import org.eclipse.jdt.internal.core.BinaryType;
import org.eclipse.jdt.internal.core.ClassFile;
import org.eclipse.jdt.internal.core.CompilationUnit;
import org.eclipse.jdt.internal.core.JarPackageFragmentRoot;
import org.eclipse.jdt.internal.core.JavaElement;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.jdt.internal.core.JavaProject;
import org.eclipse.jdt.internal.core.LambdaFactory;
import org.eclipse.jdt.internal.core.LocalVariable;
import org.eclipse.jdt.internal.core.ModularClassFile;
import org.eclipse.jdt.internal.core.NameLookup;
import org.eclipse.jdt.internal.core.Openable;
import org.eclipse.jdt.internal.core.PackageFragment;
import org.eclipse.jdt.internal.core.PackageFragmentRoot;
import org.eclipse.jdt.internal.core.SearchableEnvironment;
import org.eclipse.jdt.internal.core.SourceMapper;
import org.eclipse.jdt.internal.core.SourceMethod;
import org.eclipse.jdt.internal.core.SourceType;
import org.eclipse.jdt.internal.core.SourceTypeElementInfo;
import org.eclipse.jdt.internal.core.index.Index;
import org.eclipse.jdt.internal.core.search.*;
import org.eclipse.jdt.internal.core.search.indexing.QualifierQuery;
import org.eclipse.jdt.internal.core.util.ASTNodeFinder;
import org.eclipse.jdt.internal.core.util.HandleFactory;
import org.eclipse.jdt.internal.core.util.Util;

@SuppressWarnings({"rawtypes", "unchecked"})
public class MatchLocator implements ITypeRequestor {

public static final int MAX_AT_ONCE;
static {
	long maxMemory = Runtime.getRuntime().maxMemory();
	int ratio = (int) Math.round(((double) maxMemory) / (64 * 0x100000));
	switch (ratio) {
		case 0:
		case 1:
			MAX_AT_ONCE = 100;
			break;
		case 2:
			MAX_AT_ONCE = 200;
			break;
		case 3:
			MAX_AT_ONCE = 300;
			break;
		default:
			MAX_AT_ONCE = 400;
			break;
	}
}

// permanent state
public SearchPattern pattern;
public PatternLocator patternLocator;
public int matchContainer;
public SearchRequestor requestor;
public IJavaSearchScope scope;
public IProgressMonitor progressMonitor;
private IJavaSearchScope subScope = null;

public org.eclipse.jdt.core.ICompilationUnit[] workingCopies;
public HandleFactory handleFactory;

// cache of all super type names if scope is hierarchy scope
public char[][][] allSuperTypeNames;

// the following is valid for the current project
public MatchLocatorParser parser;
private Parser basicParser;
public INameEnvironment nameEnvironment;
public NameLookup nameLookup;
public LookupEnvironment lookupEnvironment;
public HierarchyResolver hierarchyResolver;

public CompilerOptions options;

// management of PossibleMatch to be processed
public int numberOfMatches; // (numberOfMatches - 1) is the last unit in matchesToProcess
public PossibleMatch[] matchesToProcess;
public PossibleMatch currentPossibleMatch;

/* package */HashMap<SearchMatch, Binding> matchBinding = new HashMap<>();
/*
 * Time spent in the IJavaSearchResultCollector
 */
public long resultCollectorTime = 0;

// Progress information
int progressStep;
int progressWorked;

// Binding resolution and cache
CompilationUnitScope unitScope;
SimpleLookupTable bindings;

HashtableOfIntValues inTypeOccurrencesCounts = new HashtableOfIntValues();
// Cache for method handles
HashSet methodHandles;
private TypeBinding unitScopeTypeBinding = null; // cached

private final boolean searchPackageDeclaration;
private int sourceStartOfMethodToRetain;
private int sourceEndOfMethodToRetain;

public static class WorkingCopyDocument extends JavaSearchDocument {
	public org.eclipse.jdt.core.ICompilationUnit workingCopy;
	WorkingCopyDocument(org.eclipse.jdt.core.ICompilationUnit workingCopy, SearchParticipant participant) {
		super(workingCopy.getPath().toString(), participant);
		this.charContents = ((CompilationUnit)workingCopy).getContents();
		this.workingCopy = workingCopy;
	}
	@Override
	public String toString() {
		return "WorkingCopyDocument for " + getPath(); //$NON-NLS-1$
	}
}

public static class WrappedCoreException extends RuntimeException {
	private static final long serialVersionUID = 8354329870126121212L; // backward compatible
	public CoreException coreException;
	public WrappedCoreException(CoreException coreException) {
		this.coreException = coreException;
	}
}

public static SearchDocument[] addWorkingCopies(SearchPattern pattern, SearchDocument[] indexMatches, org.eclipse.jdt.core.ICompilationUnit[] copies, SearchParticipant participant) {
	if (copies == null) return indexMatches;
	// working copies take precedence over corresponding compilation units
	HashMap workingCopyDocuments = workingCopiesThatCanSeeFocus(copies, pattern, participant);
	if (workingCopyDocuments.size() == 0) return indexMatches;
	SearchDocument[] matches = null;
	int length = indexMatches.length;
	for (int i = 0; i < length; i++) {
		SearchDocument searchDocument = indexMatches[i];
		if (searchDocument.getParticipant() == participant) {
			SearchDocument workingCopyDocument = (SearchDocument) workingCopyDocuments.remove(searchDocument.getPath());
			if (workingCopyDocument != null) {
				if (matches == null) {
					System.arraycopy(indexMatches, 0, matches = new SearchDocument[length], 0, length);
				}
				matches[i] = workingCopyDocument;
			}
		}
	}
	if (matches == null) { // no working copy
		matches = indexMatches;
	}
	int remainingWorkingCopiesSize = workingCopyDocuments.size();
	if (remainingWorkingCopiesSize != 0) {
		System.arraycopy(matches, 0, matches = new SearchDocument[length+remainingWorkingCopiesSize], 0, length);
		Iterator iterator = workingCopyDocuments.values().iterator();
		int index = length;
		while (iterator.hasNext()) {
			matches[index++] = (SearchDocument) iterator.next();
		}
	}
	return matches;
}

public static void setFocus(SearchPattern pattern, IJavaElement focus) {
	pattern.focus = focus;
}

/**
 * Sets the qualifier queries into pattern.
 * @see QualifierQuery#encodeQuery(org.eclipse.jdt.internal.core.search.indexing.QualifierQuery.QueryCategory[], char[], char[])
 */
public static void setIndexQualifierQuery(SearchPattern pattern, char[] queries) {
	pattern.indexQualifierQuery = queries;
}

/*
 * Returns the working copies that can see the given focus.
 */
private static HashMap workingCopiesThatCanSeeFocus(org.eclipse.jdt.core.ICompilationUnit[] copies, SearchPattern pattern, SearchParticipant participant) {
	if (copies == null) return new HashMap();
	HashMap result = new HashMap();
	for (int i=0, length = copies.length; i<length; i++) {
		org.eclipse.jdt.core.ICompilationUnit workingCopy = copies[i];
		IPath projectOrJar = MatchLocator.getProjectOrJar(workingCopy).getPath();
		if (pattern.focus == null || IndexSelector.canSeeFocus(pattern, projectOrJar) != IndexSelector.PROJECT_CAN_NOT_SEE_FOCUS) {
			result.put(
				workingCopy.getPath().toString(),
				new WorkingCopyDocument(workingCopy, participant)
			);
		}
	}
	return result;
}

public static IBinaryType classFileReader(IType type) {
	IOrdinaryClassFile classFile = type.getClassFile();
	JavaModelManager manager = JavaModelManager.getJavaModelManager();
	if (classFile.isOpen())
		return (IBinaryType)manager.getInfo(type);

	PackageFragment pkg = (PackageFragment) type.getPackageFragment();
	IPackageFragmentRoot root = (IPackageFragmentRoot) pkg.getParent();
	try {
		if (!root.isArchive())
			return Util.newClassFileReader(((JavaElement) type).resource());

		String rootPath = root.getPath().toOSString();
		if (org.eclipse.jdt.internal.compiler.util.Util.isJrt(rootPath)) {
			String classFileName = classFile.getElementName();
			String path = Util.concatWith(pkg.names, classFileName, '/');
			return ClassFileReader.readFromJrt(new File(rootPath), null, path);
		} else {
			ZipFile zipFile = null;
			try {
				IPath zipPath = root.getPath();
				if (JavaModelManager.ZIP_ACCESS_VERBOSE)
					System.out.println("(" + Thread.currentThread() + ") [MatchLocator.classFileReader()] Creating ZipFile on " + zipPath); //$NON-NLS-1$	//$NON-NLS-2$
				zipFile = manager.getZipFile(zipPath);
				String classFileName = classFile.getElementName();
				String path = Util.concatWith(pkg.names, classFileName, '/');
				return ClassFileReader.read(zipFile, path);
			} finally {
				manager.closeZipFile(zipFile);
			}
		}
	} catch (ClassFormatException | CoreException | IOException e) {
		// invalid class file: return null
	}
	return null;
}

/**
 * Query a given index for matching entries. Assumes the sender has opened the index and will close when finished.
 */
public static void findIndexMatches(SearchPattern pattern, Index index, IndexQueryRequestor requestor, SearchParticipant participant, IJavaSearchScope scope, IProgressMonitor monitor) throws IOException {
	pattern.findIndexMatches(index, requestor, participant, scope, monitor);
}

/**
 * Query a given index for matching entries. Assumes the sender has opened the index and will close when finished.
 */
public static void findIndexMatches(SearchPattern pattern, Index index, IndexQueryRequestor requestor, SearchParticipant participant, IJavaSearchScope scope, boolean resolveDocumentName, IProgressMonitor monitor) throws IOException {
	pattern.findIndexMatches(index, requestor, participant, scope, resolveDocumentName, monitor);
}

public static IJavaElement getProjectOrJar(IJavaElement element) {
	while (!(element instanceof IJavaProject) && !(element instanceof JarPackageFragmentRoot)) {
		element = element.getParent();
	}
	return element;
}

public static IJavaElement projectOrJarFocus(SearchPattern pattern) {
	return pattern == null || pattern.focus == null ? null : getProjectOrJar(pattern.focus);
}

public MatchLocator(
	SearchPattern pattern,
	SearchRequestor requestor,
	IJavaSearchScope scope,
	IProgressMonitor progressMonitor) {

	this.pattern = pattern;
	this.patternLocator = PatternLocator.patternLocator(this.pattern);
	this.matchContainer = this.patternLocator == null ? 0 : this.patternLocator.matchContainer();
	this.requestor = requestor;
	this.scope = scope;
	this.progressMonitor = progressMonitor;
	if (pattern instanceof PackageDeclarationPattern) {
		this.searchPackageDeclaration = true;
	} else if (pattern instanceof OrPattern) {
		this.searchPackageDeclaration = ((OrPattern)pattern).hasPackageDeclaration();
	} else {
		this.searchPackageDeclaration = false;
	}
	if (pattern instanceof MethodPattern) {
	    IType type = ((MethodPattern) pattern).declaringType;
	    if (type != null && !type.isBinary()) {
	    	SourceType sourceType = (SourceType) type;
	    	IMember local = sourceType.getOuterMostLocalContext();
	    	if (local instanceof IMethod) { // remember this method's range so we don't purge its statements.
	    		try {
	    			ISourceRange range = local.getSourceRange();
	    			this.sourceStartOfMethodToRetain  = range.getOffset();
	    			this.sourceEndOfMethodToRetain = this.sourceStartOfMethodToRetain + range.getLength() - 1; // offset is 0 based.
	    		} catch (JavaModelException e) {
	    			// drop silently.
	    		}
	    	}
	    }
	}
}
/**
 * Add an additional binary type
 */
@Override
public void accept(IBinaryType binaryType, PackageBinding packageBinding, AccessRestriction accessRestriction) {
	this.lookupEnvironment.createBinaryTypeFrom(binaryType, packageBinding, accessRestriction);
}
/**
 * Add an additional compilation unit into the loop
 *  ->  build compilation unit declarations, their bindings and record their results.
 */
@Override
public void accept(ICompilationUnit sourceUnit, AccessRestriction accessRestriction) {
	// Switch the current policy and compilation result for this unit to the requested one.
	CompilationResult unitResult = new CompilationResult(sourceUnit, 1, 1, this.options.maxProblemsPerUnit);
	try {
		CompilationUnitDeclaration parsedUnit = basicParser().dietParse(sourceUnit, unitResult);
		this.lookupEnvironment.buildTypeBindings(parsedUnit, accessRestriction);
		this.lookupEnvironment.completeTypeBindings(parsedUnit, true);
	} catch (AbortCompilationUnit e) {
		// at this point, currentCompilationUnitResult may not be sourceUnit, but some other
		// one requested further along to resolve sourceUnit.
		if (unitResult.compilationUnit == sourceUnit) { // only report once
			//requestor.acceptResult(unitResult.tagAsAccepted());
		} else {
			throw e; // want to abort enclosing request to compile
		}
	}
	// Display unit error in debug mode
	if (BasicSearchEngine.VERBOSE) {
		if (unitResult.problemCount > 0) {
			System.out.println(unitResult);
		}
	}
}
/**
 * Add additional source types
 */
@Override
public void accept(ISourceType[] sourceTypes, PackageBinding packageBinding, AccessRestriction accessRestriction) {
	// case of SearchableEnvironment of an IJavaProject is used
	ISourceType sourceType = sourceTypes[0];
	while (sourceType.getEnclosingType() != null)
		sourceType = sourceType.getEnclosingType();
	if (sourceType instanceof SourceTypeElementInfo) {
		// get source
		SourceTypeElementInfo elementInfo = (SourceTypeElementInfo) sourceType;
		IType type = elementInfo.getHandle();
		ICompilationUnit sourceUnit = (ICompilationUnit) type.getCompilationUnit();
		accept(sourceUnit, accessRestriction);
	} else {
		CompilationResult result = new CompilationResult(sourceType.getFileName(), 1, 1, 0);
		CompilationUnitDeclaration unit =
			SourceTypeConverter.buildCompilationUnit(
				sourceTypes,
				SourceTypeConverter.FIELD_AND_METHOD // need field and methods
				| SourceTypeConverter.MEMBER_TYPE, // need member types
				// no need for field initialization
				this.lookupEnvironment.problemReporter,
				result);
		if(unit != null) {
			this.lookupEnvironment.buildTypeBindings(unit, accessRestriction);
			this.lookupEnvironment.completeTypeBindings(unit, true);
		}
	}
}
protected Parser basicParser() {
	if (this.basicParser == null) {
		ProblemReporter problemReporter =
			new ProblemReporter(
				DefaultErrorHandlingPolicies.proceedWithAllProblems(),
				this.options,
				new DefaultProblemFactory());
		this.basicParser = new Parser(problemReporter, false);
		this.basicParser.reportOnlyOneSyntaxError = true;
	}
	return this.basicParser;
}
/*
 * Caches the given binary type in the lookup environment and returns it.
 * Returns the existing one if already cached.
 * Returns null if source type binding was cached.
 */
protected BinaryTypeBinding cacheBinaryType(IType type, IBinaryType binaryType) throws JavaModelException {
	IType enclosingType = type.getDeclaringType();
	if (enclosingType != null)
		cacheBinaryType(enclosingType, null); // cache enclosing types first, so that binary type can be found in lookup enviroment
	if (binaryType == null) {
		ClassFile classFile = (ClassFile) type.getClassFile();
		try {
			binaryType = getBinaryInfo(classFile, classFile.resource());
		} catch (CoreException e) {
			if (e instanceof JavaModelException) {
				throw (JavaModelException) e;
			} else {
				throw new JavaModelException(e);
			}
		}
	}
	BinaryTypeBinding binding = this.lookupEnvironment.cacheBinaryType(binaryType, null /*no access restriction*/);
	if (binding == null) { // it was already cached as a result of a previous query
		char[][] compoundName = CharOperation.splitOn('.', type.getFullyQualifiedName().toCharArray());
		ReferenceBinding referenceBinding = this.lookupEnvironment.getCachedType(compoundName);
		if (referenceBinding != null && (referenceBinding instanceof BinaryTypeBinding))
			binding = (BinaryTypeBinding) referenceBinding; // if the binding could be found and if it comes from a binary type
	}
	return binding;
}
/*
 * Computes the super type names of the focus type if any.
 */
protected char[][][] computeSuperTypeNames(IType focusType) {
	String fullyQualifiedName = focusType.getFullyQualifiedName();
	int lastDot = fullyQualifiedName.lastIndexOf('.');
	char[] qualification = lastDot == -1 ? CharOperation.NO_CHAR : fullyQualifiedName.substring(0, lastDot).toCharArray();
	char[] simpleName = focusType.getElementName().toCharArray();

	SuperTypeNamesCollector superTypeNamesCollector =
		new SuperTypeNamesCollector(
			this.pattern,
			simpleName,
			qualification,
			new MatchLocator(this.pattern, this.requestor, this.scope, this.progressMonitor), // clone MatchLocator so that it has no side effect
			focusType,
			this.progressMonitor);
	try {
		this.allSuperTypeNames = superTypeNamesCollector.collect();
	} catch (JavaModelException e) {
		// problem collecting super type names: leave it null
	}
	return this.allSuperTypeNames;
}
/**
 * Creates an IMethod from the given lambda declaration and type.
 */
protected IJavaElement createHandle(LambdaExpression lambdaExpression, IJavaElement parent) {
	org.eclipse.jdt.internal.core.LambdaExpression lambdaElement = LambdaFactory.createLambdaExpression((JavaElement) parent, lambdaExpression);
	IMethod lambdaMethodElement = lambdaElement.getMethod();
	this.methodHandles.add(lambdaMethodElement);
	return lambdaMethodElement;
}
/**
 * Creates an IMethod from the given method declaration and type.
 */
protected IJavaElement createHandle(AbstractMethodDeclaration method, IJavaElement parent) {
	if (!(parent instanceof IType)) return parent;

	IType type = (IType) parent;
	Argument[] arguments = method.arguments;
	int argCount = arguments == null ? 0 : arguments.length;
	if (type.isBinary()) {
		// don't cache the methods of the binary type
		// fall thru if its a constructor with a synthetic argument... find it the slower way
		IBinaryType reader = classFileReader(type);
		if (reader != null) {
			// build arguments names
			boolean firstIsSynthetic = false;
			if (reader.isMember() && method.isConstructor() && !Flags.isStatic(reader.getModifiers())) { // see https://bugs.eclipse.org/bugs/show_bug.cgi?id=48261
				firstIsSynthetic = true;
				argCount++;
			}
			char[][] argumentTypeNames = new char[argCount][];
			for (int i = 0; i < argCount; i++) {
				char[] typeName = null;
				if (i == 0 && firstIsSynthetic) {
					typeName = type.getDeclaringType().getFullyQualifiedName().toCharArray();
				} else if (arguments != null) {
					TypeReference typeRef = arguments[firstIsSynthetic ? i - 1 : i].type;
					typeName = CharOperation.concatWith(typeRef.getTypeName(), '.');
					for (int k = 0, dim = typeRef.dimensions(); k < dim; k++)
						typeName = CharOperation.concat(typeName, new char[] {'[', ']'});
				}
				if (typeName == null) {
					// invalid type name
					return null;
				}
				argumentTypeNames[i] = typeName;
			}
			// return binary method
			IMethod binaryMethod = createBinaryMethodHandle(type, method.selector, argumentTypeNames);
			if (binaryMethod == null) {
				// when first attempt fails, try with similar matches if any...
				PossibleMatch similarMatch = this.currentPossibleMatch.getSimilarMatch();
				while (similarMatch != null) {
					type = ((ClassFile)similarMatch.openable).getType();
					binaryMethod = createBinaryMethodHandle(type, method.selector, argumentTypeNames);
					if (binaryMethod != null) {
						return binaryMethod;
					}
					similarMatch = similarMatch.getSimilarMatch();
				}
			}
			return binaryMethod;
		}
		if (BasicSearchEngine.VERBOSE) {
			System.out.println("Not able to createHandle for the method " + //$NON-NLS-1$
					CharOperation.charToString(method.selector) + " May miss some results");  //$NON-NLS-1$
		}
		return null;
	}

	String[] parameterTypeSignatures = new String[argCount];
	if (arguments != null) {
		for (int i = 0; i < argCount; i++) {
			TypeReference typeRef = arguments[i].type;
			char[] typeName = CharOperation.concatWith(typeRef.getParameterizedTypeName(), '.');
			parameterTypeSignatures[i] = Signature.createTypeSignature(typeName, false);
		}
	}

	return createMethodHandle(type, new String(method.selector), parameterTypeSignatures);
}
/*
 * Create binary method handle
 */
IMethod createBinaryMethodHandle(IType type, char[] methodSelector, char[][] argumentTypeNames) {
	IBinaryType reader = MatchLocator.classFileReader(type);
	if (reader != null) {
		IBinaryMethod[] methods = reader.getMethods();
		if (methods != null) {
			int argCount = argumentTypeNames == null ? 0 : argumentTypeNames.length;
			nextMethod : for (int i = 0, methodsLength = methods.length; i < methodsLength; i++) {
				IBinaryMethod binaryMethod = methods[i];
				char[] selector = binaryMethod.isConstructor() ? type.getElementName().toCharArray() : binaryMethod.getSelector();
				if (CharOperation.equals(selector, methodSelector)) {
					char[] signature = binaryMethod.getGenericSignature();
					if (signature == null) signature = binaryMethod.getMethodDescriptor();
					char[][] parameterTypes = Signature.getParameterTypes(signature);
					if (argCount != parameterTypes.length) continue nextMethod;
					if (argumentTypeNames != null) {
						for (int j = 0; j < argCount; j++) {
							char[] parameterTypeName = ClassFileMatchLocator.convertClassFileFormat(parameterTypes[j]);
							if (!CharOperation.endsWith(Signature.toCharArray(Signature.getTypeErasure(parameterTypeName)), CharOperation.replaceOnCopy(argumentTypeNames[j], '$', '.')))
								continue nextMethod;
							parameterTypes[j] = parameterTypeName;
						}
					}
					return (IMethod) createMethodHandle(type, new String(selector), CharOperation.toStrings(parameterTypes));
				}
			}
		}
	}
	return null;
}
/*
 * Create method handle.
 * Store occurrences for create handle to retrieve possible duplicate ones.
 */
private IJavaElement createMethodHandle(IType type, String methodName, String[] parameterTypeSignatures) {
	IMethod methodHandle = type.getMethod(methodName, parameterTypeSignatures);
	if (methodHandle instanceof SourceMethod) {
		while (this.methodHandles.contains(methodHandle)) {
			((SourceMethod) methodHandle).occurrenceCount++;
		}
	}
	this.methodHandles.add(methodHandle);
	return methodHandle;
}
/**
 * Creates an IField from the given field declaration and type.
 */
protected IJavaElement createHandle(FieldDeclaration fieldDeclaration, TypeDeclaration typeDeclaration, IJavaElement parent) {
	if (!(parent instanceof IType)) return parent;
	IType type = (IType) parent;

	switch (fieldDeclaration.getKind()) {
		case AbstractVariableDeclaration.FIELD :
		case AbstractVariableDeclaration.ENUM_CONSTANT :
			return ((IType) parent).getField(new String(fieldDeclaration.name));
	}
	if (type.isBinary()) {
		// do not return initializer for binary types
		// see bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=98378
		return type;
	}
	// find occurrence count of the given initializer in its type declaration
	int occurrenceCount = 0;
	FieldDeclaration[] fields = typeDeclaration.fields;
	int length = fields == null ? 0 : fields.length;
	for (int i = 0; i < length; i++) {
		if (fields[i].getKind() == AbstractVariableDeclaration.INITIALIZER) {
			occurrenceCount++;
			if (fields[i].equals(fieldDeclaration)) break;
		}
	}
	return ((IType) parent).getInitializer(occurrenceCount);
}
/**
 * Create an handle for a local variable declaration (may be a local variable or type parameter).
 */
protected IJavaElement createHandle(AbstractVariableDeclaration variableDeclaration, IJavaElement parent) {
	boolean isParameter = true;
	switch (variableDeclaration.getKind()) {
		case AbstractVariableDeclaration.LOCAL_VARIABLE:
			isParameter = false;
			//$FALL-THROUGH$
		case AbstractVariableDeclaration.PARAMETER:
			if (variableDeclaration.type.resolvedType != null) {
				return new LocalVariable((JavaElement)parent,
					new String(variableDeclaration.name),
					variableDeclaration.declarationSourceStart,
					variableDeclaration.declarationSourceEnd,
					variableDeclaration.sourceStart,
					variableDeclaration.sourceEnd,
					new String(variableDeclaration.type.resolvedType.signature()),
					variableDeclaration.annotations,
					variableDeclaration.modifiers,
					isParameter,
					variableDeclaration.type.getAnnotationsOnDimensions()
				);
			}
			break;
		case AbstractVariableDeclaration.TYPE_PARAMETER:
			return new org.eclipse.jdt.internal.core.TypeParameter((JavaElement)parent, new String(variableDeclaration.name));
	}
	return null;
}
/**
 * Create an handle for a local variable declaration (may be a local variable or type parameter).
 */
protected IJavaElement createHandle(Annotation annotation, IAnnotatable parent) {
	if (parent == null) return null;
	TypeReference typeRef = annotation.type;
	char[][] typeName = typeRef.getTypeName();
	String name = new String(typeName[typeName.length-1]);
	try {
		IAnnotation[] annotations = parent.getAnnotations();
		int length = annotations == null ? 0 : annotations.length;
		for (int i=0; i<length; i++) {
			if (annotations[i].getElementName().equals(name)) {
				return annotations[i];
			}
		}
		if (parent instanceof LocalVariable) {
			LocalVariable localVariable = (LocalVariable) parent;
			IAnnotation[][] annotationsOnDimensions = localVariable.annotationsOnDimensions;
			int noOfDimensions = annotationsOnDimensions == null ? 0 : annotationsOnDimensions.length;
			for (int i = 0; i < noOfDimensions; ++i) {
				IAnnotation[] dimAnnotations = annotationsOnDimensions[i];
				int noOfAnnotations = dimAnnotations.length;
				for (int j = 0; j < noOfAnnotations; ++j) {
					if (dimAnnotations[j].getElementName().equals(name))
						return dimAnnotations[j];
				}
			}
		}
	}
	catch (JavaModelException jme) {
		// skip
	}
	return null;
}
/*
 * Create handles for a list of fields
 */
private IJavaElement[] createHandles(FieldDeclaration[] fields, TypeDeclaration type, IJavaElement parent) {
	IJavaElement[] otherElements = null;
	if (fields != null) {
		int length = fields.length;
		int size = 0;
		while (size<length && fields[size] != null) {
			size++;
		}
		otherElements = new IJavaElement[size];
		for (int j=0; j<size; j++) {
			otherElements[j] = createHandle(fields[j], type, parent);
		}
	}
	return otherElements;
}
/*
 * Creates hierarchy resolver if needed.
 * Returns whether focus is visible.
 */
protected boolean createHierarchyResolver(IType focusType, PossibleMatch[] possibleMatches) {
	// cache focus type if not a possible match
	char[][] compoundName = CharOperation.splitOn('.', focusType.getFullyQualifiedName().toCharArray());
	boolean isPossibleMatch = false;
	for (int i = 0, length = possibleMatches.length; i < length; i++) {
		if (CharOperation.equals(possibleMatches[i].compoundName, compoundName)) {
			isPossibleMatch = true;
			break;
		}
	}
	if (!isPossibleMatch) {
		if (focusType.isBinary()) {
			try {
				cacheBinaryType(focusType, null);
			} catch (JavaModelException e) {
				return false;
			}
		} else {
			// cache all types in the focus' compilation unit (even secondary types)
			accept((ICompilationUnit) focusType.getCompilationUnit(), null /*TODO no access restriction*/);
		}
	}

	// resolve focus type
	this.hierarchyResolver = new HierarchyResolver(this.lookupEnvironment, null/*hierarchy is not going to be computed*/);
	ReferenceBinding binding = this.hierarchyResolver.setFocusType(compoundName);
	return binding != null && binding.isValidBinding() && (binding.tagBits & TagBits.HierarchyHasProblems) == 0;
}
/**
 * Creates an IImportDeclaration from the given import statement
 */
protected IJavaElement createImportHandle(ImportReference importRef) {
	char[] importName = CharOperation.concatWith(importRef.getImportName(), '.');
	if ((importRef.bits & ASTNode.OnDemand) != 0)
		importName = CharOperation.concat(importName, ".*" .toCharArray()); //$NON-NLS-1$
	Openable openable = this.currentPossibleMatch.openable;
	if (openable instanceof CompilationUnit)
		return ((CompilationUnit) openable).getImport(new String(importName));

	if (openable instanceof ModularClassFile)
		return openable;
	// binary types do not contain import statements so just answer the top-level type as the element
	IType binaryType = ((ClassFile) openable).getType();
	String typeName = binaryType.getElementName();
	int lastDollar = typeName.lastIndexOf('$');
	if (lastDollar == -1) return binaryType;
	return createTypeHandle(typeName.substring(0, lastDollar));
}
/**
 * Creates an IImportDeclaration from the given import statement
 */
protected IJavaElement createPackageDeclarationHandle(CompilationUnitDeclaration unit) {
	if (unit.isPackageInfo()) {
		char[] packName = CharOperation.concatWith(unit.currentPackage.getImportName(), '.');
		Openable openable = this.currentPossibleMatch.openable;
		if (openable instanceof CompilationUnit) {
			return ((CompilationUnit) openable).getPackageDeclaration(new String(packName));
		}
	}
	return createTypeHandle(new String(unit.getMainTypeName()));
}
/**
 * Creates an IType from the given simple top level type name.
 */
protected IType createTypeHandle(String simpleTypeName) {
	Openable openable = this.currentPossibleMatch.openable;
	if (openable instanceof CompilationUnit)
		return ((CompilationUnit) openable).getType(simpleTypeName);

	IType binaryType = ((ClassFile) openable).getType();
	String binaryTypeQualifiedName = binaryType.getTypeQualifiedName();
	if (simpleTypeName.equals(binaryTypeQualifiedName))
		return binaryType; // answer only top-level types, sometimes the classFile is for a member/local type

	// type name may be null for anonymous (see bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=164791)
	String classFileName = simpleTypeName.length() == 0 ? binaryTypeQualifiedName : simpleTypeName;
	IOrdinaryClassFile classFile = binaryType.getPackageFragment().getOrdinaryClassFile(classFileName + SuffixConstants.SUFFIX_STRING_class);
	return classFile.getType();
}
protected boolean encloses(IJavaElement element) {
	if (element != null) {
		if (this.scope instanceof HierarchyScope)
			return ((HierarchyScope)this.scope).encloses(element, this.progressMonitor);
		else
			return this.scope.encloses(element);
	}
	return false;
}
private boolean filterEnum(SearchMatch match) {

	// filter org.apache.commons.lang.enum package for projects above 1.5
	// https://bugs.eclipse.org/bugs/show_bug.cgi?id=317264
	IJavaElement element = (IJavaElement)match.getElement();
	PackageFragment pkg = (PackageFragment)element.getAncestor(IJavaElement.PACKAGE_FRAGMENT);
	if (pkg != null) {
		// enum was found in org.apache.commons.lang.enum at index 5
		if (pkg.names.length == 5 && pkg.names[4].equals("enum")) {  //$NON-NLS-1$
			if (this.options == null) {
				IJavaProject proj = (IJavaProject)pkg.getAncestor(IJavaElement.JAVA_PROJECT);
				String complianceStr = proj.getOption(CompilerOptions.OPTION_Source, true);
				if (CompilerOptions.versionToJdkLevel(complianceStr) >= ClassFileConstants.JDK1_5)
					return true;
			} else if (this.options.sourceLevel >= ClassFileConstants.JDK1_5) {
				return true;
			}
		}
	}
	return false;
}

/* (non-Javadoc)
 * Return info about last type argument of a parameterized type reference.
 * These info are made of concatenation of 2 int values which are respectively
 *  depth and end position of the last type argument.
 * For example, this method will return 0x300000020 for type ref List<List<List<String>>>
 * if end position of type reference "String" equals 32.
 */
private long findLastTypeArgumentInfo(TypeReference typeRef) {
	// Get last list of type arguments for parameterized qualified type reference
	TypeReference lastTypeArgument = typeRef;
	int depth = 0;
	while (true) {
		TypeReference[] lastTypeArguments = null;
		if (lastTypeArgument instanceof ParameterizedQualifiedTypeReference) {
			ParameterizedQualifiedTypeReference pqtRef = (ParameterizedQualifiedTypeReference) lastTypeArgument;
			for (int i=pqtRef.typeArguments.length-1; i>=0 && lastTypeArguments==null; i--) {
				lastTypeArguments = pqtRef.typeArguments[i];
			}
		}
		// Get last type argument for single type reference of last list of argument of parameterized qualified type reference
		TypeReference last = null;
		if (lastTypeArgument instanceof ParameterizedSingleTypeReference || lastTypeArguments != null) {
			if (lastTypeArguments == null) {
				lastTypeArguments = ((ParameterizedSingleTypeReference)lastTypeArgument).typeArguments;
			}
			if (lastTypeArguments != null) {
				for (int i=lastTypeArguments.length-1; i>=0 && last==null; i++) {
					last = lastTypeArguments[i];
				}
			}
		}
		if (last == null) break;
		depth++;
		lastTypeArgument = last;
	}
	// Current type reference is not parameterized. So, it is the last type argument
	return (((long) depth) << 32) + lastTypeArgument.sourceEnd;
}
protected IBinaryType getBinaryInfo(ClassFile classFile, IResource resource) throws CoreException {
	BinaryType binaryType = (BinaryType) classFile.getType();
	if (classFile.isOpen())
		return (IBinaryType) binaryType.getElementInfo(); // reuse the info from the java model cache

	// create a temporary info
	IBinaryType info;
	try {
		PackageFragment pkg = (PackageFragment) classFile.getParent();
		PackageFragmentRoot root = (PackageFragmentRoot) pkg.getParent();
		if (root.isArchive()) {
			// class file in a jar
			String classFileName = classFile.getElementName();
			String classFilePath = Util.concatWith(pkg.names, classFileName, '/');
			ZipFile zipFile = null;
			try {
				zipFile = ((JarPackageFragmentRoot) root).getJar();
				info = ClassFileReader.read(zipFile, classFilePath);
			} finally {
				JavaModelManager.getJavaModelManager().closeZipFile(zipFile);
			}
		} else {
			// class file in a directory
			info = Util.newClassFileReader(resource);
		}
		if (info == null) throw binaryType.newNotPresentException();
		return info;
	} catch (ClassFormatException e) {
		//e.printStackTrace();
		return null;
	} catch (java.io.IOException e) {
		throw new JavaModelException(e, IJavaModelStatusConstants.IO_EXCEPTION);
	}
}
protected IType getFocusType() {
	return this.scope instanceof HierarchyScope ? ((HierarchyScope) this.scope).focusType : null;
}
protected void getMethodBodies(CompilationUnitDeclaration unit, MatchingNodeSet nodeSet) {
	if (unit.ignoreMethodBodies) {
		unit.ignoreFurtherInvestigation = true;
		return; // if initial diet parse did not work, no need to dig into method bodies.
	}

	// save existing values to restore them at the end of the parsing process
	// see bug 47079 for more details
	int[] oldLineEnds = this.parser.scanner.lineEnds;
	int oldLinePtr = this.parser.scanner.linePtr;

	try {
		CompilationResult compilationResult = unit.compilationResult;
		this.parser.scanner.setSource(compilationResult);

		if (this.parser.javadocParser.checkDocComment) {
			char[] contents = compilationResult.compilationUnit.getContents();
			this.parser.javadocParser.scanner.setSource(contents);
		}
		this.parser.nodeSet = nodeSet;
		this.parser.parseBodies(unit);
	} finally {
		this.parser.nodeSet = null;
		// this is done to prevent any side effects on the compilation unit result
		// line separator positions array.
		this.parser.scanner.lineEnds = oldLineEnds;
		this.parser.scanner.linePtr = oldLinePtr;
	}
}
protected TypeBinding getType(Object typeKey, char[] typeName) {
	if (this.unitScope == null || typeName == null || typeName.length == 0) return null;
	// Try to get binding from cache
	Binding binding = (Binding) this.bindings.get(typeKey);
	if (binding != null) {
		if (binding instanceof TypeBinding && binding.isValidBinding())
			return (TypeBinding) binding;
		return null;
	}
	// Get binding from unit scope
	char[][] compoundName = CharOperation.splitOn('.', typeName);
	TypeBinding typeBinding = this.unitScope.getType(compoundName, compoundName.length);
	this.unitScopeTypeBinding = typeBinding; //cache.
	if (typeBinding == null || !typeBinding.isValidBinding()) {
		typeBinding = this.lookupEnvironment.getType(compoundName, this.unitScope.module());
	}
	this.bindings.put(typeKey, typeBinding);
	return typeBinding != null && typeBinding.isValidBinding() ? typeBinding : null;
}
public MethodBinding getMethodBinding(MethodPattern methodPattern) {
	this.unitScopeTypeBinding = null;
    MethodBinding methodBinding = getMethodBinding0(methodPattern);
    if (methodBinding != null)
    	return methodBinding; // known to be valid.
    // special handling for methods of anonymous/local types. Since these cannot be looked up in the environment the usual way ...
    if (methodPattern.focus instanceof SourceMethod) {
    	char[] typeName = PatternLocator.qualifiedPattern(methodPattern.declaringSimpleName, methodPattern.declaringQualification);
    	if (typeName != null) {
    		IType type = methodPattern.declaringType;
    		IType enclosingType = type.getDeclaringType();
    		while (enclosingType != null) {
    			type = enclosingType;
    			enclosingType = type.getDeclaringType();
    		}
    		typeName = type.getFullyQualifiedName().toCharArray();
    		TypeBinding declaringTypeBinding = getType(typeName, typeName);
    		if (declaringTypeBinding instanceof SourceTypeBinding) {
    			SourceTypeBinding sourceTypeBinding = ((SourceTypeBinding) declaringTypeBinding);
    			ClassScope skope = sourceTypeBinding.scope;
    			if (skope != null) {
    				CompilationUnitDeclaration unit = skope.referenceCompilationUnit();
    				if (unit != null) {
    					AbstractMethodDeclaration amd = new ASTNodeFinder(unit).findMethod((IMethod) methodPattern.focus);
    					if (amd != null && amd.binding != null && amd.binding.isValidBinding()) {
    						this.bindings.put(methodPattern, amd.binding);
    						return amd.binding;
    					}
    				}
    			}
    		}
    	}
    } else if (methodPattern.focus instanceof BinaryMethod &&
    		methodPattern.declaringType instanceof BinaryType &&
    		this.unitScopeTypeBinding instanceof ProblemReferenceBinding) {//Get binding from unit scope for non-visible member of binary type
    	char[] typeName = PatternLocator.qualifiedPattern(methodPattern.declaringSimpleName, methodPattern.declaringQualification);
    	if (typeName != null) {
    		IType type = methodPattern.declaringType;
    		IType enclosingType = type.getDeclaringType();
    		while (enclosingType != null) {
    			type = enclosingType;
    			enclosingType = type.getDeclaringType();
    		}
    		typeName = type.getFullyQualifiedName().toCharArray();
    		TypeBinding typeBinding = this.unitScopeTypeBinding;
    		if (typeBinding instanceof ProblemReferenceBinding) {
    			ProblemReferenceBinding problemReferenceBinding = (ProblemReferenceBinding) this.unitScopeTypeBinding;
    			ReferenceBinding closestMatch = (problemReferenceBinding.problemId() == ProblemReasons.NotVisible) ?
    					problemReferenceBinding.closestReferenceMatch() : null;
    					return closestMatch != null ?  getMethodBinding(methodPattern, closestMatch) : null;
    		}
    	}
    }
	return null;
}

private List<String> getInverseFullName(char[] qualifier, char[] simpleName) {
	List <String> result = new ArrayList<String>();
	if (qualifier != null && qualifier.length > 0) {
		result.addAll(Arrays.asList(new String(qualifier).split("\\.")));//$NON-NLS-1$
		Collections.reverse(result);
	}
	if (simpleName != null) result.add(0, new String(simpleName));
	return result;
}

/** returns the row index which has the highest column entry.
 * TODO: rewrite this code with list when (if) we move to 1.8 [with FP constructs].
 */
private int  getMaxResult(int[][] resultsMap) {
	int rows = resultsMap.length;
	int cols = resultsMap[0].length;
	List <Integer> candidates = new ArrayList<Integer>();
	candidates.add(0); //default row

	for (int j = 0; j < cols; ++j) {
		int current = resultsMap[0][j];
		for (int i = 1; i < rows; ++i) {
			int tmp = resultsMap[i][j];
			if (tmp < current) continue;
			if (tmp > current)  {
				current = tmp;
				candidates.clear();
			}
			candidates.add(i);// there is atleast one element always.
		}
		if (candidates.size() <= 1) break; // found
	}
	return candidates.get(0);
}

/** apply the function to map the parameter full name to an index
 */
private int mapParameter(List <String> patternParameterFullName, List <String> methodParameterFullName) {
	int patternLen = patternParameterFullName.size();
	int methodLen = methodParameterFullName.size();
	int size = patternLen < methodLen ? patternLen : methodLen;
	int result = -1;
	for (int i = 0; i < size; i++) {
		if (!patternParameterFullName.get(i).equals(methodParameterFullName.get(i))) break;
		++result;
	}
	return patternLen == methodLen && result + 1 == patternLen ? Integer.MAX_VALUE : result;
}
/**
 * returns an array of integers whose elements are matching indices.
 * As a special case, full match would have max value as the index.
 */
private int[] getResultMap(Map<Integer, List<String>> patternMap, Map<Integer, List<String>> methodMap) {
	int paramLength = methodMap.size();
	int[] result = new int[paramLength];
	for (int p = 0; p < paramLength; p++) {
		result[p] = mapParameter(patternMap.get(p), methodMap.get(p));
	}
	return result;
}

private Map<Integer, List<String>> getSplitNames(char[][] qualifiedNames, char[][] simpleNames) {
	int paramLength = simpleNames.length;
	Map <Integer, List<String>> result = new HashMap<Integer, List<String>>();
	for (int p = 0; p < paramLength; p++) result.put(p, getInverseFullName(qualifiedNames[p], simpleNames[p]));
	return result;
}

private Map<Integer, List<String>> getSplitNames(MethodBinding method) {
	TypeBinding[] methodParameters = method.parameters;
	int paramLength = methodParameters == null ? 0 : methodParameters.length;
	Map <Integer, List<String>> result = new HashMap<Integer, List<String>>();
	for (int p = 0; p < paramLength; p++) result.put(p, getInverseFullName(methodParameters[p].qualifiedSourceName(), null)); // source is part of qualifiedSourceName here);
	return result;
}

/**
 * Selects the most applicable method (though similar but not to be confused with its namesake in jls)
 * All this machinery for that elusive uncommon case referred in bug 431357.
 */
private MethodBinding getMostApplicableMethod(List<MethodBinding> possibleMethods, MethodPattern methodPattern) {
	int size = possibleMethods.size();
	MethodBinding result = size != 0 ? possibleMethods.get(0) : null;
	if (size > 1) {
		// can cache but may not be worth since this is not a common case
		Map<Integer, List<String>> methodPatternReverseNames = getSplitNames(methodPattern.parameterQualifications, methodPattern.parameterSimpleNames);
		int len = possibleMethods.size();
		int[][] resultMaps = new int[len][];
		for (int i = 0; i < len; ++i) resultMaps[i] = getResultMap(methodPatternReverseNames, getSplitNames(possibleMethods.get(i)));
		result = possibleMethods.get(getMaxResult(resultMaps));
	}
	return result;
}

private MethodBinding getMethodBinding0(MethodPattern methodPattern) {
	if (this.unitScope == null) return null;
	// Try to get binding from cache
	Binding binding = (Binding) this.bindings.get(methodPattern);
	if (binding != null) {
		if (binding instanceof MethodBinding && binding.isValidBinding())
			return (MethodBinding) binding;
	}
	//	Get binding from unit scope
	char[] typeName = PatternLocator.qualifiedPattern(methodPattern.declaringSimpleName, methodPattern.declaringQualification);
	if (typeName == null) {
		if (methodPattern.declaringType == null) return null;
		typeName = methodPattern.declaringType.getFullyQualifiedName().toCharArray();
	}
	TypeBinding declaringTypeBinding = getType(typeName, typeName);
	MethodBinding result = null;
	if (declaringTypeBinding != null) {
		if (declaringTypeBinding.isArrayType()) {
			declaringTypeBinding = declaringTypeBinding.leafComponentType();
		}
		if (!declaringTypeBinding.isBaseType()) {
			result = getMethodBinding(methodPattern, declaringTypeBinding);
		}
	}
	this.bindings.put(methodPattern, result != null ? result : new ProblemMethodBinding(methodPattern.selector, null, ProblemReasons.NotFound));
	return result;
}
private boolean matchParams(MethodPattern methodPattern, int index, TypeBinding binding) {
	char[] qualifier = CharOperation.concat(methodPattern.parameterQualifications[index], methodPattern.parameterSimpleNames[index], '.');
	int offset = (qualifier.length > 0 && qualifier[0] == '*') ? 1 : 0;
	String s1 = new String(qualifier, offset, qualifier.length - offset);
	char[] s2 = CharOperation.concat(binding.qualifiedPackageName(), binding.qualifiedSourceName(), '.');
	return new String(s2).endsWith(s1);
}

private MethodBinding getMethodBinding(MethodPattern methodPattern, TypeBinding declaringTypeBinding) {
	MethodBinding result;
	char[][] parameterTypes = methodPattern.parameterSimpleNames;
	if (parameterTypes == null) return null;
	int paramTypeslength = parameterTypes.length;
	ReferenceBinding referenceBinding = (ReferenceBinding) declaringTypeBinding;
	MethodBinding[] methods = referenceBinding.getMethods(methodPattern.selector);
	int methodsLength = methods.length;
	TypeVariableBinding[] refTypeVariables = referenceBinding.typeVariables();
	int typeVarLength = refTypeVariables==null ? 0 : refTypeVariables.length;
	List <MethodBinding> possibleMethods = new ArrayList<MethodBinding>(methodsLength);
	for (int i=0; i<methodsLength; i++) {
		TypeBinding[] methodParameters = methods[i].parameters;
		int paramLength = methodParameters==null ? 0 : methodParameters.length;
		TypeVariableBinding[] methodTypeVariables = methods[i].typeVariables;
		int methTypeVarLength = methodTypeVariables==null ? 0 : methodTypeVariables.length;
		boolean found = false;
		if (methodParameters != null && paramLength == paramTypeslength) {
			for (int p=0; p<paramLength; p++) {
				TypeBinding parameter = methodParameters[p];
				if (matchParams(methodPattern, p, parameter)) {
					// param erasure match
					found = true;
				} else {
					// type variable
					found = false;
					if (refTypeVariables != null) {
						for (int v=0; v<typeVarLength; v++) {
							if (!CharOperation.equals(refTypeVariables[v].sourceName, parameterTypes[p])) {
								found = false;
								break;
							}
							found = true;
						}
					}
					if (!found && methodTypeVariables != null) {
						for (int v=0; v<methTypeVarLength; v++) {
							if (!CharOperation.equals(methodTypeVariables[v].sourceName, parameterTypes[p])) {
								found = false;
								break;
							}
							found = true;
						}
					}
					if (!found) break;
				}
			}
		}
		if (found) {
			possibleMethods.add(methods[i]);
		}
	}
	result =  getMostApplicableMethod(possibleMethods, methodPattern);
	return result;
}
protected boolean hasAlreadyDefinedType(CompilationUnitDeclaration parsedUnit) {
	CompilationResult result = parsedUnit.compilationResult;
	if (result == null) return false;
	for (int i = 0; i < result.problemCount; i++)
		if (result.problems[i].getID() == IProblem.DuplicateTypes)
			return true;
	return false;
}
/**
 * Create a new parser for the given project, as well as a lookup environment.
 */
public void initialize(JavaProject project, int possibleMatchSize) throws JavaModelException {
	// clean up name environment only if there are several possible match as it is reused
	// when only one possible match (bug 58581)
	if (this.nameEnvironment != null && possibleMatchSize != 1) {
		this.nameEnvironment.cleanup();
		this.unitScope = null; // don't leak a reference to the cleaned-up name environment
	}

	SearchableEnvironment searchableEnvironment = project.newSearchableNameEnvironment(this.workingCopies);

	List<IJavaProject> projects = new ArrayList<>();
	projects.add(project);
	if (this.pattern.focus != null) {
		IJavaProject focusProject = this.pattern.focus.getJavaProject();
		if (!project.equals(focusProject)) {
			projects.add(focusProject);
		}
	}
	this.nameEnvironment = IndexBasedJavaSearchEnvironment.create(projects, this.workingCopies);

	// create lookup environment
	Map map = project.getOptions(true);
	map.put(CompilerOptions.OPTION_TaskTags, org.eclipse.jdt.internal.compiler.util.Util.EMPTY_STRING);
	this.options = new CompilerOptions(map);
	ProblemReporter problemReporter =
		new ProblemReporter(
			DefaultErrorHandlingPolicies.proceedWithAllProblems(),
			this.options,
			new DefaultProblemFactory());
	this.lookupEnvironment = new LookupEnvironment(this, this.options, problemReporter, this.nameEnvironment);
	this.lookupEnvironment.mayTolerateMissingType = true;
	this.parser = MatchLocatorParser.createParser(problemReporter, this);
	this.bindings = new SimpleLookupTable(); // For every LE

	// basic parser needs also to be reset as project options may have changed
	// see bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=163072
	this.basicParser = null;

	// remember project's name lookup
	this.nameLookup = searchableEnvironment.nameLookup;

	// initialize queue of units
	this.numberOfMatches = 0;
	this.matchesToProcess = new PossibleMatch[possibleMatchSize];

	this.lookupEnvironment.addResolutionListener(this.patternLocator);
}
private boolean skipMatch(JavaProject javaProject, PossibleMatch possibleMatch) {
	if (this.options.sourceLevel >= ClassFileConstants.JDK9) {
		char[] pModuleName = possibleMatch.getModuleName();
		if (pModuleName != null && this.lookupEnvironment.getModule(pModuleName) == null)
			return true;
	}
	return false;
}
protected void locateMatches(JavaProject javaProject, PossibleMatch[] possibleMatches, int start, int length) throws CoreException {
	initialize(javaProject, length);

	// create and resolve binding (equivalent to beginCompilation() in Compiler)
	boolean mustResolvePattern = this.pattern.mustResolve;
	boolean mustResolve = mustResolvePattern;
	this.patternLocator.mayBeGeneric = this.options.sourceLevel >= ClassFileConstants.JDK1_5;
	boolean bindingsWereCreated = mustResolve;
	try {
		for (int i = start, maxUnits = start + length; i < maxUnits; i++) {
			PossibleMatch possibleMatch = possibleMatches[i];
			if (skipMatch(javaProject, possibleMatch)) continue;
			try {
				if (!parseAndBuildBindings(possibleMatch, mustResolvePattern)) continue;
				// Currently we only need to resolve over pattern flag if there's potential parameterized types
				if (this.patternLocator.mayBeGeneric) {
					// If pattern does not resolve then rely on possible match node set resolution
					// which may have been modified while locator was adding possible matches to it
					if (!mustResolvePattern && !mustResolve) {
						mustResolve = possibleMatch.nodeSet.mustResolve;
						bindingsWereCreated = mustResolve;
					}
				} else {
					// Reset matching node resolution with pattern one if there's no potential parameterized type
					// to minimize side effect on previous search behavior
					possibleMatch.nodeSet.mustResolve = mustResolvePattern;
				}
				// possible match node resolution has been merged with pattern one, so rely on it to know
				// whether we need to process compilation unit now or later
				if (!possibleMatch.nodeSet.mustResolve) {
					if (this.progressMonitor != null) {
						this.progressWorked++;
						if ((this.progressWorked%this.progressStep)==0) this.progressMonitor.worked(this.progressStep);
					}
					process(possibleMatch, bindingsWereCreated);
					if (this.numberOfMatches>0 && this.matchesToProcess[this.numberOfMatches-1] == possibleMatch) {
						// forget last possible match as it was processed
						this.numberOfMatches--;
					}
				}
			} finally {
				if (possibleMatch.hasSimilarMatch()) {
					// If there is similar match, then also process it
					// see bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=211872
					possibleMatches[i] = possibleMatch.getSimilarMatch();
					i--;
				}
				if (!possibleMatch.nodeSet.mustResolve)
					possibleMatch.cleanUp();
			}
		}
		if (mustResolve)
			this.lookupEnvironment.completeTypeBindings();

		// create hierarchy resolver if needed
		IType focusType = getFocusType();
		if (focusType == null) {
			this.hierarchyResolver = null;
		} else if (!createHierarchyResolver(focusType, possibleMatches)) {
			// focus type is not visible, use the super type names instead of the bindings
			if (computeSuperTypeNames(focusType) == null) return;
		}
	} catch (AbortCompilation e) {
		bindingsWereCreated = false;
	}

	if (!mustResolve) {
		return;
	}

	// possible match resolution
	for (int i = 0; i < this.numberOfMatches; i++) {
		if (this.progressMonitor != null && this.progressMonitor.isCanceled())
			throw new OperationCanceledException();
		PossibleMatch possibleMatch = this.matchesToProcess[i];
		this.matchesToProcess[i] = null; // release reference to processed possible match
		try {
			process(possibleMatch, bindingsWereCreated);
		} catch (AbortCompilation | JavaModelException e) {
			// problem with class path: it could not find base classes
			// continue and try next matching openable reporting inaccurate matches (since bindings will be null)
			bindingsWereCreated = false;
		} finally {
			if (this.progressMonitor != null) {
				this.progressWorked++;
				if ((this.progressWorked%this.progressStep)==0) this.progressMonitor.worked(this.progressStep);
			}
			if (this.options.verbose)
				System.out.println(
					Messages.bind(Messages.compilation_done,
						new String[] {
							String.valueOf(i + 1),
							String.valueOf(this.numberOfMatches),
							new String(possibleMatch.parsedUnit.getFileName())
						}));
			// cleanup compilation unit result
			possibleMatch.cleanUp();
		}
	}
}
/**
 * Locate the matches amongst the possible matches.
 */
protected void locateMatches(JavaProject javaProject, PossibleMatchSet matchSet, int expected) throws CoreException {
	PossibleMatch[] possibleMatches = matchSet.getPossibleMatches(javaProject.getPackageFragmentRoots());
	int length = possibleMatches.length;
	// increase progress from duplicate matches not stored in matchSet while adding...
	if (this.progressMonitor != null && expected>length) {
		this.progressWorked += expected-length;
		this.progressMonitor.worked( expected-length);
	}
	// locate matches (processed matches are limited to avoid problem while using VM default memory heap size)
	for (int index = 0; index < length;) {
		int max = Math.min(MAX_AT_ONCE, length - index);
		locateMatches(javaProject, possibleMatches, index, max);
		index += max;
	}
	this.patternLocator.clear();
}
/**
 * Locate the matches in the given files and report them using the search requestor.
 */
public void locateMatches(SearchDocument[] searchDocuments) throws CoreException {
	if (this.patternLocator == null) return;
	int docsLength = searchDocuments.length;
	int progressLength = docsLength;
	if (BasicSearchEngine.VERBOSE) {
		System.out.println("Locating matches in documents ["); //$NON-NLS-1$
		for (int i = 0; i < docsLength; i++)
			System.out.println("\t" + searchDocuments[i]); //$NON-NLS-1$
		System.out.println("]"); //$NON-NLS-1$
	}
	IJavaProject[] javaModelProjects = null;
	if (this.searchPackageDeclaration) {
		javaModelProjects = JavaModelManager.getJavaModelManager().getJavaModel().getJavaProjects();
		progressLength += javaModelProjects.length;
	}

	// init infos for progress increasing
	int n = progressLength<1000 ? Math.min(Math.max(progressLength/200+1, 2),4) : 5 *(progressLength/1000);
	this.progressStep = progressLength < n ? 1 : progressLength / n; // step should not be 0
	this.progressWorked = 0;

	// extract working copies
	ArrayList copies = new ArrayList();
	for (int i = 0; i < docsLength; i++) {
		SearchDocument document = searchDocuments[i];
		if (document instanceof WorkingCopyDocument) {
			copies.add(((WorkingCopyDocument)document).workingCopy);
		}
	}
	int copiesLength = copies.size();
	this.workingCopies = new org.eclipse.jdt.core.ICompilationUnit[copiesLength];
	copies.toArray(this.workingCopies);

	JavaModelManager manager = JavaModelManager.getJavaModelManager();
	this.bindings = new SimpleLookupTable();
	try {
		// optimize access to zip files during search operation
		manager.cacheZipFiles(this);

		// initialize handle factory (used as a cache of handles so as to optimize space)
		if (this.handleFactory == null)
			this.handleFactory = new HandleFactory();

		if (this.progressMonitor != null) {
			this.progressMonitor.beginTask("", searchDocuments.length); //$NON-NLS-1$
		}

		// initialize pattern for polymorphic search (i.e. method reference pattern)
		this.patternLocator.initializePolymorphicSearch(this);

		JavaProject previousJavaProject = null;
		PossibleMatchSet matchSet = new PossibleMatchSet();
		Util.sort(searchDocuments, new Util.Comparer() {
			@Override
			public int compare(Object a, Object b) {
				return ((SearchDocument)a).getPath().compareTo(((SearchDocument)b).getPath());
			}
		});
		int displayed = 0; // progress worked displayed
		String previousPath = null;
		SearchParticipant searchParticipant = null;
		for (int i = 0; i < docsLength; i++) {
			if (this.progressMonitor != null && this.progressMonitor.isCanceled()) {
				throw new OperationCanceledException();
			}

			// skip duplicate paths
			SearchDocument searchDocument = searchDocuments[i];
			if (searchParticipant == null) {
				searchParticipant = searchDocument.getParticipant();
			}
			searchDocuments[i] = null; // free current document
			String pathString = searchDocument.getPath();
			if (i > 0 && pathString.equals(previousPath)) {
				if (this.progressMonitor != null) {
					this.progressWorked++;
					if ((this.progressWorked%this.progressStep)==0) this.progressMonitor.worked(this.progressStep);
				}
				displayed++;
				continue;
			}
			previousPath = pathString;

			Openable openable;
			org.eclipse.jdt.core.ICompilationUnit workingCopy = null;
			if (searchDocument instanceof WorkingCopyDocument) {
				workingCopy = ((WorkingCopyDocument)searchDocument).workingCopy;
				openable = (Openable) workingCopy;
			} else {
				openable = this.handleFactory.createOpenable(pathString, this.scope);
			}
			if (openable == null) {
				if (this.progressMonitor != null) {
					this.progressWorked++;
					if ((this.progressWorked%this.progressStep)==0) this.progressMonitor.worked(this.progressStep);
				}
				displayed++;
				continue; // match is outside classpath
			}

			// create new parser and lookup environment if this is a new project
			IResource resource = null;
			openable = getCloserOpenable(openable, pathString);
			JavaProject javaProject = openable.getJavaProject();
			resource = workingCopy != null ? workingCopy.getResource() : openable.getResource();
			if (resource == null)
				resource = javaProject.getProject(); // case of a file in an external jar or external folder
			if (!javaProject.equals(previousJavaProject)) {
				// locate matches in previous project
				if (previousJavaProject != null) {
					try {
						locateMatches(previousJavaProject, matchSet, i-displayed);
						displayed = i;
					} catch (JavaModelException e) {
						// problem with classpath in this project -> skip it
					}
					matchSet.reset();
				}
				previousJavaProject = javaProject;
			}
			PossibleMatch possibleMatch = new PossibleMatch(this, resource, openable, searchDocument,this.pattern.mustResolve);
			matchSet.add(possibleMatch);
			if (pathString.endsWith(TypeConstants.AUTOMATIC_MODULE_NAME)) {
				IPath path = resource.getFullPath();
				String s = (pathString.contains(path.lastSegment())) ?
						JavaModelManager.getLocalFile(path).toPath().toAbsolutePath().toString() :
						pathString.split(Pattern.quote("|"))[0]; //$NON-NLS-1$
				possibleMatch.autoModuleName = new String(AutomaticModuleNaming.determineAutomaticModuleName(s));
			}
		}

		// last project
		if (previousJavaProject != null) {
			try {
				locateMatches(previousJavaProject, matchSet, docsLength-displayed);
			} catch (JavaModelException e) {
				// problem with classpath in last project -> ignore
			}
		}

		if (this.searchPackageDeclaration) {
			locatePackageDeclarations(searchParticipant, javaModelProjects);
		}

	} finally {
		if (this.progressMonitor != null)
			this.progressMonitor.done();
		if (this.nameEnvironment != null)
			this.nameEnvironment.cleanup();
		this.unitScope = null;
		manager.flushZipFiles(this);
		this.bindings = null;
	}
}
private IJavaSearchScope getSubScope(String optionString, long value, boolean ref) {
	if (this.subScope != null)
		return this.subScope;
	IPath[] enclosingProjectsAndJars = this.scope.enclosingProjectsAndJars();
	JavaModelManager manager = JavaModelManager.getJavaModelManager();
	HashSet<IJavaProject> set = new HashSet<>();
	for (int i = 0, l = enclosingProjectsAndJars.length; i < l; i++) {
		IPath path = enclosingProjectsAndJars[i];
		if (path.segmentCount() == 1) {
			IJavaProject p = manager.getJavaModel().getJavaProject(path.segment(0));
			if (p == null) continue;
			if (CompilerOptions.versionToJdkLevel(p.getOption(optionString, true)) >= value) {
				set.add(p);
			}
		}
	}
	return this.subScope = BasicSearchEngine.createJavaSearchScope(set.toArray(new IJavaProject[0]), ref);
}
private Openable getCloserOpenable(Openable openable, String pathString) {
	if (this.pattern instanceof TypeDeclarationPattern &&
			((TypeDeclarationPattern) this.pattern).moduleNames != null) {
		JavaProject javaProject = openable.getJavaProject();
		PackageFragmentRoot root = openable.getPackageFragmentRoot();
		if (root instanceof JarPackageFragmentRoot) {
			JarPackageFragmentRoot jpkf = (JarPackageFragmentRoot) root;
			if (jpkf.getModuleDescription() != null &&
					CompilerOptions.versionToJdkLevel(javaProject.getOption(JavaCore.COMPILER_COMPLIANCE, true)) <
					ClassFileConstants.JDK9) {
				openable = this.handleFactory.createOpenable(pathString,
						getSubScope(JavaCore.COMPILER_COMPLIANCE, ClassFileConstants.JDK9, false));
			}
		}
	}
	return openable;
}

/**
 * Locates the package declarations corresponding to this locator's pattern.
 */
protected void locatePackageDeclarations(SearchParticipant participant, IJavaProject[] projects) throws CoreException {
	locatePackageDeclarations(this.pattern, participant, projects);
}
/**
 * Locates the package declarations corresponding to the search pattern.
 */
protected void locatePackageDeclarations(SearchPattern searchPattern, SearchParticipant participant, IJavaProject[] projects) throws CoreException {
	if (this.progressMonitor != null && this.progressMonitor.isCanceled()) {
		throw new OperationCanceledException();
	}
	if (searchPattern instanceof OrPattern) {
		SearchPattern[] patterns = ((OrPattern) searchPattern).patterns;
		for (int i = 0, length = patterns.length; i < length; i++) {
			locatePackageDeclarations(patterns[i], participant, projects);
		}
	} else if (searchPattern instanceof PackageDeclarationPattern) {
		IJavaElement focus = searchPattern.focus;
		if (focus != null) {
			if (encloses(focus)) {
				SearchMatch match = new PackageDeclarationMatch(focus.getAncestor(IJavaElement.PACKAGE_FRAGMENT), SearchMatch.A_ACCURATE, -1, -1, participant, focus.getResource());
				report(match);
			}
			return;
		}
		PackageDeclarationPattern pkgPattern = (PackageDeclarationPattern) searchPattern;
		boolean isWorkspaceScope = this.scope == JavaModelManager.getJavaModelManager().getWorkspaceScope();
		IPath[] scopeProjectsAndJars =  isWorkspaceScope ? null : this.scope.enclosingProjectsAndJars();
		int scopeLength = isWorkspaceScope ? 0 : scopeProjectsAndJars.length;
		SimpleSet packages = new SimpleSet();
		for (int i = 0, length = projects.length; i < length; i++) {
			IJavaProject javaProject = projects[i];
			if (this.progressMonitor != null) {
				if (this.progressMonitor.isCanceled()) throw new OperationCanceledException();
				this.progressWorked++;
				if ((this.progressWorked%this.progressStep)==0) this.progressMonitor.worked(this.progressStep);
			}
			// Verify that project belongs to the scope
			if (!isWorkspaceScope) {
				boolean found = false;
				for (int j=0; j<scopeLength; j++) {
					if (javaProject.getPath().equals(scopeProjectsAndJars[j])) {
						found = true;
						break;
					}
				}
				if (!found) continue;
			}
			// Get all project package fragment names
			this.nameLookup = ((JavaProject) projects[i]).newNameLookup(this.workingCopies);
			IPackageFragment[] packageFragments = this.nameLookup.findPackageFragments(new String(pkgPattern.pkgName), false, true);
			int pLength = packageFragments == null ? 0 : packageFragments.length;
			// Report matches avoiding duplicate names
			for (int p=0; p<pLength; p++) {
				IPackageFragment fragment = packageFragments[p];
				if (packages.addIfNotIncluded(fragment) == null) continue;
				if (encloses(fragment)) {
					IResource resource = fragment.getResource();
					if (resource == null) // case of a file in an external jar
						resource = javaProject.getProject();
					try {
						if (encloses(fragment)) {
							SearchMatch match = new PackageDeclarationMatch(fragment, SearchMatch.A_ACCURATE, -1, -1, participant, resource);
							report(match);
						}
					} catch (JavaModelException e) {
						throw e;
					} catch (CoreException e) {
						throw new JavaModelException(e);
					}
				}
			}
		}
	}
}
//*/
protected IType lookupType(ReferenceBinding typeBinding) {
	if (typeBinding == null || !typeBinding.isValidBinding()) return null;

	char[] packageName = typeBinding.qualifiedPackageName();
	IPackageFragment[] pkgs = this.nameLookup.findPackageFragments(
		(packageName == null || packageName.length == 0)
			? IPackageFragment.DEFAULT_PACKAGE_NAME
			: new String(packageName),
		false);

	// iterate type lookup in each package fragment
	char[] sourceName = typeBinding.qualifiedSourceName();
	String typeName = new String(sourceName);
	int acceptFlag = 0;
	if (typeBinding.isAnnotationType()) {
		acceptFlag = NameLookup.ACCEPT_ANNOTATIONS;
	} else if (typeBinding.isEnum()) {
		acceptFlag = NameLookup.ACCEPT_ENUMS;
	} else if (typeBinding.isInterface()) {
		acceptFlag = NameLookup.ACCEPT_INTERFACES;
	} else if (typeBinding.isRecord()) {
		acceptFlag = NameLookup.ACCEPT_RECORDS;
	} else if (typeBinding.isClass()) {
		acceptFlag = NameLookup.ACCEPT_CLASSES;
	}
	if (pkgs != null) {
		for (int i = 0, length = pkgs.length; i < length; i++) {
			IType type = this.nameLookup.findType(typeName, pkgs[i],  false,  acceptFlag, false, true/*consider secondary types*/);
			if (type != null) return type;
		}
	}

	// search inside enclosing element
	char[][] qualifiedName = CharOperation.splitOn('.', sourceName);
	int length = qualifiedName.length;
	if (length == 0) return null;

	IType type = createTypeHandle(new String(qualifiedName[0])); // find the top-level type
	if (type == null) return null;

	for (int i = 1; i < length; i++) {
		type = type.getType(new String(qualifiedName[i]));
		if (type == null) return null;
	}
	if (type.exists()) return type;
	return null;
}
public SearchMatch newDeclarationMatch(
		IJavaElement element,
		Binding binding,
		int accuracy,
		int offset,
		int length) {
	SearchParticipant participant = getParticipant();
	IResource resource = this.currentPossibleMatch.resource;
	return newDeclarationMatch(element, binding, accuracy, offset, length, participant, resource);
}

public SearchMatch newDeclarationMatch(
		IJavaElement element,
		Binding binding,
		int accuracy,
		int offset,
		int length,
		SearchParticipant participant,
		IResource resource) {
	switch (element.getElementType()) {
		case IJavaElement.PACKAGE_FRAGMENT:
			return new PackageDeclarationMatch(element, accuracy, offset, length, participant, resource);
		case IJavaElement.TYPE:
			return new TypeDeclarationMatch(binding == null ? element : ((JavaElement) element).resolved(binding), accuracy, offset, length, participant, resource);
		case IJavaElement.FIELD:
			return new FieldDeclarationMatch(binding == null ? element : ((JavaElement) element).resolved(binding), accuracy, offset, length, participant, resource);
		case IJavaElement.METHOD:
			return new MethodDeclarationMatch(binding == null ? element : ((JavaElement) element).resolved(binding), accuracy, offset, length, participant, resource);
		case IJavaElement.LOCAL_VARIABLE:
			return new LocalVariableDeclarationMatch(element, accuracy, offset, length, participant, resource);
		case IJavaElement.PACKAGE_DECLARATION:
			return new PackageDeclarationMatch(element, accuracy, offset, length, participant, resource);
		case IJavaElement.TYPE_PARAMETER:
			return new TypeParameterDeclarationMatch(element, accuracy, offset, length, participant, resource);
		case IJavaElement.JAVA_MODULE:
			ModuleDeclarationMatch match = new ModuleDeclarationMatch(binding == null ? element : ((JavaElement) element).resolved(binding), accuracy, offset, length, participant, resource);
			this.matchBinding.put(match, binding);
			return match;
		default:
			return null;
	}
}

public FieldReferenceMatch newFieldReferenceMatch(
		IJavaElement enclosingElement,
		IJavaElement localElement,
		Binding enclosingBinding,
		int accuracy,
		int offset,
		int length, ASTNode reference) {
	int bits = reference.bits;
	boolean isCompoundAssigned = (bits & ASTNode.IsCompoundAssigned) != 0;
	boolean isReadAccess = isCompoundAssigned || (bits & ASTNode.IsStrictlyAssigned) == 0;
	boolean isWriteAccess = isCompoundAssigned || (bits & ASTNode.IsStrictlyAssigned) != 0;
	if (isWriteAccess) {
		if (reference instanceof QualifiedNameReference) {
			char[][] tokens = ((QualifiedNameReference)reference).tokens;
			char[] lastToken = tokens[tokens.length-1];
			if (this.pattern instanceof OrPattern) {
				SearchPattern[] patterns = ((OrPattern) this.pattern).patterns;
				for (int i = 0, pLength = patterns.length; i < pLength; i++) {
					if (!this.patternLocator.matchesName(((VariablePattern)patterns[i]).name, lastToken)) {
			        	isWriteAccess = false;
			        	isReadAccess = true;
					}
				}
			} else if (!this.patternLocator.matchesName(((VariablePattern)this.pattern).name, lastToken)) {
	        	isWriteAccess = false;
	        	isReadAccess = true;
			}
        }
	}
	boolean insideDocComment = (bits & ASTNode.InsideJavadoc) != 0;
	SearchParticipant participant = getParticipant();
	IResource resource = this.currentPossibleMatch.resource;
	if (enclosingBinding != null) {
		enclosingElement = ((JavaElement) enclosingElement).resolved(enclosingBinding);
	}
	FieldReferenceMatch match = new FieldReferenceMatch(enclosingElement, accuracy, offset, length, isReadAccess, isWriteAccess, insideDocComment, participant, resource);
	match.setLocalElement(localElement);
	return match;
}

public SearchMatch newLocalVariableReferenceMatch(
		IJavaElement enclosingElement,
		int accuracy,
		int offset,
		int length,
		ASTNode reference) {
	int bits = reference.bits;
	boolean isCompoundAssigned = (bits & ASTNode.IsCompoundAssigned) != 0;
	boolean isReadAccess = isCompoundAssigned || (bits & ASTNode.IsStrictlyAssigned) == 0;
	boolean isWriteAccess = isCompoundAssigned || (bits & ASTNode.IsStrictlyAssigned) != 0;
	if (isWriteAccess) {
		if (reference instanceof QualifiedNameReference) {
			char[][] tokens = ((QualifiedNameReference)reference).tokens;
			char[] lastToken = tokens[tokens.length-1];
			if (this.pattern instanceof OrPattern) {
				SearchPattern[] patterns = ((OrPattern) this.pattern).patterns;
				for (int i = 0, pLength = patterns.length; i < pLength; i++) {
					if (!this.patternLocator.matchesName(((VariablePattern)patterns[i]).name, lastToken)) {
			        	isWriteAccess = false;
			        	isReadAccess = true;
					}
				}
			} else if (!this.patternLocator.matchesName(((VariablePattern)this.pattern).name, lastToken)) {
	        	isWriteAccess = false;
	        	isReadAccess = true;
			}
        }
	}
	boolean insideDocComment = (bits & ASTNode.InsideJavadoc) != 0;
	SearchParticipant participant = getParticipant();
	IResource resource = this.currentPossibleMatch.resource;
	return new LocalVariableReferenceMatch(enclosingElement, accuracy, offset, length, isReadAccess, isWriteAccess, insideDocComment, participant, resource);
}

public MethodReferenceMatch newMethodReferenceMatch(
		IJavaElement enclosingElement,
		Binding enclosingBinding,
		int accuracy,
		int offset,
		int length,
		boolean isConstructor,
		boolean isSynthetic,
		ASTNode reference) {
	SearchParticipant participant = getParticipant();
	IResource resource = this.currentPossibleMatch.resource;
	boolean insideDocComment = (reference.bits & ASTNode.InsideJavadoc) != 0;
	if (enclosingBinding != null)
		enclosingElement = ((JavaElement) enclosingElement).resolved(enclosingBinding);
	boolean isOverridden = (accuracy & PatternLocator.SUPER_INVOCATION_FLAVOR) != 0;
	return new MethodReferenceMatch(enclosingElement, accuracy, offset, length, isConstructor, isSynthetic, isOverridden, insideDocComment, participant, resource);
}

public PackageReferenceMatch newPackageReferenceMatch(
		IJavaElement enclosingElement,
		int accuracy,
		int offset,
		int length,
		ASTNode reference) {
	SearchParticipant participant = getParticipant();
	IResource resource = this.currentPossibleMatch.resource;
	boolean insideDocComment = reference != null && (reference.bits & ASTNode.InsideJavadoc) != 0;
	return new PackageReferenceMatch(enclosingElement, accuracy, offset, length, insideDocComment, participant, resource);
}

public SearchMatch newTypeParameterReferenceMatch(
		IJavaElement enclosingElement,
		int accuracy,
		int offset,
		int length,
		ASTNode reference) {
	int bits = reference.bits;
	boolean insideDocComment = (bits & ASTNode.InsideJavadoc) != 0;
	SearchParticipant participant = getParticipant();
	IResource resource = this.currentPossibleMatch.resource;
	return new TypeParameterReferenceMatch(enclosingElement, accuracy, offset, length, insideDocComment, participant, resource);
}

public TypeReferenceMatch newTypeReferenceMatch(
		IJavaElement enclosingElement,
		Binding enclosingBinding,
		int accuracy,
		int offset,
		int length,
		ASTNode reference) {
	SearchParticipant participant = getParticipant();
	IResource resource = this.currentPossibleMatch.resource;
	boolean insideDocComment = reference != null && (reference.bits & ASTNode.InsideJavadoc) != 0;
	if (enclosingBinding != null)
		enclosingElement = ((JavaElement) enclosingElement).resolved(enclosingBinding);
	return new TypeReferenceMatch(enclosingElement, accuracy, offset, length, insideDocComment, participant, resource);
}

public TypeReferenceMatch newTypeReferenceMatch(
		IJavaElement enclosingElement,
		Binding enclosingBinding,
		int accuracy,
		ASTNode reference) {
	return newTypeReferenceMatch(enclosingElement, enclosingBinding, accuracy, reference.sourceStart, reference.sourceEnd-reference.sourceStart+1, reference);
}

public ModuleReferenceMatch newModuleReferenceMatch(
		IJavaElement enclosingElement,
		Binding enclosingBinding,
		int accuracy,
		int offset,
		int length,
		ASTNode reference) {
	SearchParticipant participant = getParticipant();
	IResource resource = this.currentPossibleMatch.resource;
	boolean insideDocComment = reference != null ? (reference.bits & ASTNode.InsideJavadoc) != 0 : false;
	if (enclosingBinding != null)
		enclosingElement = ((JavaElement) enclosingElement).resolved(enclosingBinding);
	return new ModuleReferenceMatch(enclosingElement, accuracy, offset, length, insideDocComment, participant, resource);
}

public ModuleReferenceMatch newModuleReferenceMatch(
		IJavaElement enclosingElement,
		Binding enclosingBinding,
		int accuracy,
		ASTNode reference) {
	return newModuleReferenceMatch(enclosingElement, enclosingBinding, accuracy, reference.sourceStart, reference.sourceEnd-reference.sourceStart+1, reference);
}
/**
 * Add the possibleMatch to the loop
 *  ->  build compilation unit declarations, their bindings and record their results.
 */
protected boolean parseAndBuildBindings(PossibleMatch possibleMatch, boolean mustResolve) throws CoreException {
	if (this.progressMonitor != null && this.progressMonitor.isCanceled())
		throw new OperationCanceledException();

	try {
		if (BasicSearchEngine.VERBOSE)
			System.out.println("Parsing " + possibleMatch.openable.toStringWithAncestors()); //$NON-NLS-1$

		this.parser.nodeSet = possibleMatch.nodeSet;
		CompilationResult unitResult = new CompilationResult(possibleMatch, 1, 1, this.options.maxProblemsPerUnit);
		CompilationUnitDeclaration parsedUnit = this.parser.dietParse(possibleMatch, unitResult);
		if (parsedUnit != null) {
			if (parsedUnit.isModuleInfo()) {
				if (mustResolve) {
					this.lookupEnvironment.buildTypeBindings(parsedUnit, null /*no access restriction*/);
				}
			} else if (!parsedUnit.isEmpty()) {
				if (mustResolve) {
					this.lookupEnvironment.buildTypeBindings(parsedUnit, null /*no access restriction*/);
				}
				if (hasAlreadyDefinedType(parsedUnit)) return false; // skip type has it is hidden so not visible
				getMethodBodies(parsedUnit, possibleMatch.nodeSet);
				if (this.patternLocator.mayBeGeneric && !mustResolve && possibleMatch.nodeSet.mustResolve) {
					// special case: possible match node set force resolution although pattern does not
					// => we need to build types for this compilation unit
					this.lookupEnvironment.buildTypeBindings(parsedUnit, null /*no access restriction*/);
				}
			}

			// add the possibleMatch with its parsedUnit to matchesToProcess
			possibleMatch.parsedUnit = parsedUnit;
			int size = this.matchesToProcess.length;
			if (this.numberOfMatches == size)
				System.arraycopy(this.matchesToProcess, 0, this.matchesToProcess = new PossibleMatch[size == 0 ? 1 : size * 2], 0, this.numberOfMatches);
			this.matchesToProcess[this.numberOfMatches++] = possibleMatch;
		}
	} finally {
		this.parser.nodeSet = null;
	}
	return true;
}
/*
 * Process a compilation unit already parsed and build.
 */
protected void process(PossibleMatch possibleMatch, boolean bindingsWereCreated) throws CoreException {
	this.currentPossibleMatch = possibleMatch;
	CompilationUnitDeclaration unit = possibleMatch.parsedUnit;
	try {
		if (unit.isEmpty()) {
			if (this.currentPossibleMatch.openable instanceof ClassFile) {
				ClassFile classFile = (ClassFile) this.currentPossibleMatch.openable;
				IBinaryType info = null;
				try {
					info = getBinaryInfo(classFile, classFile.resource());
				}
				catch (CoreException ce) {
					// Do nothing
				}
				if (info != null) {
					boolean mayBeGeneric = this.patternLocator.mayBeGeneric;
					this.patternLocator.mayBeGeneric = false; // there's no longer generic in class files
					try {
						new ClassFileMatchLocator().locateMatches(this, classFile, info);
					}
					finally {
						this.patternLocator.mayBeGeneric = mayBeGeneric;
					}
				}
			} else if (this.currentPossibleMatch.openable instanceof ModularClassFile &&
					unit.moduleDeclaration == null) { // no source
				boolean mayBeGeneric = this.patternLocator.mayBeGeneric;
				this.patternLocator.mayBeGeneric = false; // there's no longer generic in class files
				try {
					new ModularClassFileMatchLocator().locateMatches(this, (ModularClassFile) this.currentPossibleMatch.openable);
				}
				finally {
					this.patternLocator.mayBeGeneric = mayBeGeneric;
				}
				return;
			}
			if (!unit.isModuleInfo())
				return;
		}
		if (hasAlreadyDefinedType(unit)) return; // skip type has it is hidden so not visible

		// Move getMethodBodies to #parseAndBuildings(...) method to allow possible match resolution management
		//getMethodBodies(unit);

		boolean mustResolve = (this.pattern.mustResolve || possibleMatch.nodeSet.mustResolve);
		if (bindingsWereCreated && mustResolve) {
			if (unit.types != null) {
				if (BasicSearchEngine.VERBOSE)
					System.out.println("Resolving " + this.currentPossibleMatch.openable.toStringWithAncestors()); //$NON-NLS-1$

				this.lookupEnvironment.unitBeingCompleted = unit;
				reduceParseTree(unit);

				if (unit.scope != null) {
					// fault in fields & methods
					unit.scope.faultInTypes();
				}
				unit.resolve();
			} else if (unit.isPackageInfo()) {
				if (BasicSearchEngine.VERBOSE)
					System.out.println("Resolving " + this.currentPossibleMatch.openable.toStringWithAncestors()); //$NON-NLS-1$
				unit.resolve();
			} else if (unit.isModuleInfo()) {
				if (BasicSearchEngine.VERBOSE)
					System.out.println("Resolving " + this.currentPossibleMatch.openable.toStringWithAncestors()); //$NON-NLS-1$
				this.lookupEnvironment.unitBeingCompleted = unit;
				if (unit.scope != null && unit.moduleDeclaration != null) {
					unit.moduleDeclaration.resolveTypeDirectives(unit.scope);
				}
			}
		}
		reportMatching(unit, mustResolve);
	} catch (AbortCompilation e) {
		if (BasicSearchEngine.VERBOSE) {
			System.out.println("AbortCompilation while resolving unit " + String.valueOf(unit.getFileName())); //$NON-NLS-1$
			e.printStackTrace();
		}
		// could not resolve: report inaccurate matches
		reportMatching(unit, false); // do not resolve when cu has errors
		if (!(e instanceof AbortCompilationUnit)) {
			// problem with class path
			throw e;
		}
	} finally {
		this.lookupEnvironment.unitBeingCompleted = null;
		this.currentPossibleMatch = null;
	}
}
protected void purgeMethodStatements(TypeDeclaration type, boolean checkEachMethod) {
	checkEachMethod = checkEachMethod
		&& this.currentPossibleMatch.nodeSet.hasPossibleNodes(type.declarationSourceStart, type.declarationSourceEnd);
	AbstractMethodDeclaration[] methods = type.methods;
	if (methods != null) {
		if (checkEachMethod) {
			for (int j = 0, length = methods.length; j < length; j++) {
				AbstractMethodDeclaration method = methods[j];
				if (!this.currentPossibleMatch.nodeSet.hasPossibleNodes(method.declarationSourceStart, method.declarationSourceEnd)) {
					if (this.sourceStartOfMethodToRetain != method.declarationSourceStart || this.sourceEndOfMethodToRetain != method.declarationSourceEnd) { // approximate, but no big deal
						method.statements = null;
						method.javadoc = null;
					}
				}
			}
		} else {
			for (int j = 0, length = methods.length; j < length; j++) {
				AbstractMethodDeclaration method = methods[j];
				if (this.sourceStartOfMethodToRetain != method.declarationSourceStart || this.sourceEndOfMethodToRetain != method.declarationSourceEnd) { // approximate, but no big deal
					method.statements = null;
					method.javadoc = null;
				}
			}
		}
	}

	TypeDeclaration[] memberTypes = type.memberTypes;
	if (memberTypes != null)
		for (int i = 0, l = memberTypes.length; i < l; i++)
			purgeMethodStatements(memberTypes[i], checkEachMethod);
}
/**
 * Called prior to the unit being resolved. Reduce the parse tree where possible.
 */
protected void reduceParseTree(CompilationUnitDeclaration unit) {
	// remove statements from methods that have no possible matching nodes
	TypeDeclaration[] types = unit.types;
	for (int i = 0, l = types.length; i < l; i++)
		purgeMethodStatements(types[i], true);
}
public SearchParticipant getParticipant() {
	return this.currentPossibleMatch.document.getParticipant();
}

protected void report(SearchMatch match) throws CoreException {
	if (match == null) {
		if (BasicSearchEngine.VERBOSE) {
			System.out.println("Cannot report a null match!!!"); //$NON-NLS-1$
		}
		return;
	}
	if (filterEnum(match)){
		if (BasicSearchEngine.VERBOSE) {
			System.out.println("Filtered package with name enum"); //$NON-NLS-1$
		}
		return;
	}
	long start = -1;
	if (BasicSearchEngine.VERBOSE) {
		start = System.currentTimeMillis();
		System.out.println("Reporting match"); //$NON-NLS-1$
		System.out.println("\tResource: " + match.getResource());//$NON-NLS-1$
		System.out.println("\tPositions: [offset=" + match.getOffset() + ", length=" + match.getLength() + "]"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		try {
			if (this.parser != null && match.getOffset() > 0 && match.getLength() > 0 && !(match.getElement() instanceof BinaryMember)) {
				String selection = new String(this.parser.scanner.source, match.getOffset(), match.getLength());
				System.out.println("\tSelection: -->" + selection + "<--"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		} catch (Exception e) {
			// it's just for debug purposes... ignore all exceptions in this area
		}
		try {
			JavaElement javaElement = (JavaElement)match.getElement();
			System.out.println("\tJava element: "+ javaElement.toStringWithAncestors()); //$NON-NLS-1$
			if (!javaElement.exists()) {
				System.out.println("\t\tWARNING: this element does NOT exist!"); //$NON-NLS-1$
			}
		} catch (Exception e) {
			// it's just for debug purposes... ignore all exceptions in this area
		}
		if (match instanceof ReferenceMatch) {
			try {
				ReferenceMatch refMatch = (ReferenceMatch) match;
				JavaElement local = (JavaElement) refMatch.getLocalElement();
				if (local != null) {
					System.out.println("\tLocal element: "+ local.toStringWithAncestors()); //$NON-NLS-1$
				}
				if (match instanceof TypeReferenceMatch) {
					IJavaElement[] others = ((TypeReferenceMatch) refMatch).getOtherElements();
					if (others != null) {
						int length = others.length;
						if (length > 0) {
							System.out.println("\tOther elements:"); //$NON-NLS-1$
							for (int i=0; i<length; i++) {
								JavaElement other = (JavaElement) others[i];
								System.out.println("\t\t- "+ other.toStringWithAncestors()); //$NON-NLS-1$
							}
						}
					}
				}
			} catch (Exception e) {
				// it's just for debug purposes... ignore all exceptions in this area
			}
		}
		System.out.println(match.getAccuracy() == SearchMatch.A_ACCURATE
			? "\tAccuracy: EXACT_MATCH" //$NON-NLS-1$
			: "\tAccuracy: POTENTIAL_MATCH"); //$NON-NLS-1$
		System.out.print("\tRule: "); //$NON-NLS-1$
		if (match.isExact()) {
			System.out.print("EXACT"); //$NON-NLS-1$
		} else if (match.isEquivalent()) {
			System.out.print("EQUIVALENT"); //$NON-NLS-1$
		} else if (match.isErasure()) {
			System.out.print("ERASURE"); //$NON-NLS-1$
		} else {
			System.out.print("INVALID RULE"); //$NON-NLS-1$
		}
		if (match instanceof MethodReferenceMatch) {
			MethodReferenceMatch methodReferenceMatch = (MethodReferenceMatch) match;
			if (methodReferenceMatch.isSuperInvocation()) {
				System.out.print("+SUPER INVOCATION"); //$NON-NLS-1$
			}
			if (methodReferenceMatch.isImplicit()) {
				System.out.print("+IMPLICIT"); //$NON-NLS-1$
			}
			if (methodReferenceMatch.isSynthetic()) {
				System.out.print("+SYNTHETIC"); //$NON-NLS-1$
			}
		}
		System.out.println("\n\tRaw: "+match.isRaw()); //$NON-NLS-1$
	}
	this.requestor.acceptSearchMatch(match);
	if (BasicSearchEngine.VERBOSE)
		this.resultCollectorTime += System.currentTimeMillis()-start;
}
/**
 * Finds the accurate positions of the sequence of tokens given by qualifiedName
 * in the source and reports a reference to this this qualified name
 * to the search requestor.
 */
protected void reportAccurateTypeReference(SearchMatch match, ASTNode typeRef, char[] name) throws CoreException {
	if (match.getRule() == 0) return;
	if (!encloses((IJavaElement)match.getElement())) return;

	int sourceStart = typeRef.sourceStart;
	int sourceEnd = typeRef.sourceEnd;

	// Compute source positions of the qualified reference
	if (name != null) {
		Scanner scanner = this.parser.scanner;
		scanner.setSource(this.currentPossibleMatch.getContents());
		scanner.resetTo(sourceStart, sourceEnd);

		int token = -1;
		int currentPosition;
		do {
			currentPosition = scanner.currentPosition;
			try {
				token = scanner.getNextToken();
			} catch (InvalidInputException e) {
				// ignore
			}
			if (token == TerminalTokens.TokenNameIdentifier && this.pattern.matchesName(name, scanner.getCurrentTokenSource())) {
				int length = scanner.currentPosition-currentPosition;
				match.setOffset(currentPosition);
				match.setLength(length);
				report(match);
				return;
			}
		} while (token != TerminalTokens.TokenNameEOF);
	}

	//	Report match
	match.setOffset(sourceStart);
	match.setLength(sourceEnd-sourceStart+1);
	report(match);
}

/**
 * Finds the accurate positions of the sequence of tokens given by qualifiedName
 * in the source and reports a reference to this parameterized type name
 * to the search requestor.
 * @since 3.1
 */
protected void reportAccurateParameterizedMethodReference(SearchMatch match, ASTNode statement, TypeReference[] typeArguments) throws CoreException {
	if (match.getRule() == 0) return;
	if (!encloses((IJavaElement)match.getElement())) return;

	// If there's type arguments, look for end (i.e. char '>') of last one.
	int start = match.getOffset();
	if (typeArguments != null && typeArguments.length > 0) {
		boolean isErasureMatch= (this.pattern instanceof OrPattern) ? ((OrPattern)this.pattern).isErasureMatch() : ((JavaSearchPattern)this.pattern).isErasureMatch();
		if (!isErasureMatch) {

			// Initialize scanner
			Scanner scanner = this.parser.scanner;
			char[] source = this.currentPossibleMatch.getContents();
			scanner.setSource(source);

			// Search previous opening '<'
			start = typeArguments[0].sourceStart;
			int end = statement.sourceEnd;
			scanner.resetTo(start, end);
			int lineStart = start;
			try {
				linesUp: while (true) {
					while (scanner.source[scanner.currentPosition] != '\n') {
						scanner.currentPosition--;
						if (scanner.currentPosition == 0) break linesUp;
					}
					lineStart = scanner.currentPosition+1;
					scanner.resetTo(lineStart, end);
					while (!scanner.atEnd()) {
						if (scanner.getNextToken() == TerminalTokens.TokenNameLESS) {
							start = scanner.getCurrentTokenStartPosition();
							break linesUp;
						}
					}
					end = lineStart - 2;
					scanner.currentPosition = end;
				}
			}
			catch (InvalidInputException ex) {
				// give up
			}
	 	}
	}

	// Report match
	match.setOffset(start);
	match.setLength(statement.sourceEnd-start+1);
	report(match);
}

/**
 * Finds the accurate positions of the sequence of tokens given by qualifiedName
 * in the source and reports a reference to this parameterized type name
 * to the search requestor.
 * @since 3.1
 */
protected void reportAccurateParameterizedTypeReference(SearchMatch match, TypeReference typeRef, int index, TypeReference[] typeArguments) throws CoreException {
	if (match.getRule() == 0) return;
	if (!encloses((IJavaElement)match.getElement())) return;

	// If there's type arguments, look for end (i.e. char '>') of last one.
	int end = typeRef.sourceEnd;
	if (typeArguments != null) {

		boolean shouldMatchErasure= (this.pattern instanceof OrPattern) ? ((OrPattern)this.pattern).isErasureMatch() : ((JavaSearchPattern)this.pattern).isErasureMatch();
		boolean hasSignatures = (this.pattern instanceof OrPattern) ? ((OrPattern)this.pattern).hasSignatures() : ((JavaSearchPattern)this.pattern).hasSignatures();
		if (shouldMatchErasure || !hasSignatures) {
			// if pattern is erasure only, then select the end of the reference
			if (typeRef instanceof QualifiedTypeReference && index >= 0) {
				long[] positions = ((QualifiedTypeReference) typeRef).sourcePositions;
				end = (int) positions[index];
			} else if (typeRef instanceof ArrayTypeReference) {
				end = ((ArrayTypeReference) typeRef).originalSourceEnd;
			}
		}  else {
			// Initialize scanner
			Scanner scanner = this.parser.scanner;
			char[] source = this.currentPossibleMatch.getContents();
			scanner.setSource(source);

			// Set scanner position at end of last type argument
			scanner.resetTo(end, source.length-1);
			int depth = 0;
			for (int i=typeArguments.length-1; i>=0; i--) {
				if (typeArguments[i] != null) {
					long lastTypeArgInfo = findLastTypeArgumentInfo(typeArguments[i]);
					depth = (int) (lastTypeArgInfo >>> 32)+1;
					scanner.resetTo(((int)lastTypeArgInfo)+1, scanner.eofPosition-1);
					break;
				}
			}

			// Now, scan to search next closing '>'
			while (depth-- > 0) {
				while (!scanner.atEnd()) {
					if (scanner.getNextChar() == '>') {
						end = scanner.currentPosition - 1;
						break;
					}
				}
			}
	 	}
	}

	// Report match
	match.setLength(end-match.getOffset()+1);
	report(match);
}
/**
 * Finds the accurate positions of each valid token in the source and
 * reports a reference to this token to the search requestor.
 * A token is valid if it has an accuracy which is not -1.
 */
protected void reportAccurateEnumConstructorReference(SearchMatch match, FieldDeclaration field, AllocationExpression allocation) throws CoreException {
	// Verify that field declaration is really an enum constant
	if (allocation == null || allocation.enumConstant == null) {
		report(match);
		return;
	}

	// Get scan area
	int sourceStart = match.getOffset()+match.getLength();
	if (allocation.arguments != null && allocation.arguments.length > 0) {
		sourceStart = allocation.arguments[allocation.arguments.length-1].sourceEnd+1;
	}
	int sourceEnd = field.declarationSourceEnd;
	if (allocation instanceof QualifiedAllocationExpression) {
		QualifiedAllocationExpression qualifiedAllocation = (QualifiedAllocationExpression) allocation;
		if (qualifiedAllocation.anonymousType != null) {
			sourceEnd = qualifiedAllocation.anonymousType.sourceStart - 1;
		}
	}

	// Scan to find last closing parenthesis
	Scanner scanner = this.parser.scanner;
	scanner.setSource(this.currentPossibleMatch.getContents());
	scanner.resetTo(sourceStart, sourceEnd);
	try {
		int token = scanner.getNextToken();
		while (token != TerminalTokens.TokenNameEOF) {
			if (token == TerminalTokens.TokenNameRPAREN) {
				sourceEnd = scanner.getCurrentTokenEndPosition();
			}
			token = scanner.getNextToken();
		}
	}
	catch (InvalidInputException iie) {
		// give up
	}

	// Report match
	match.setLength(sourceEnd-match.getOffset()+1);
	report(match);
}
/**
 * Finds the accurate positions of each valid token in the source and
 * reports a reference to this token to the search requestor.
 * A token is valid if it has an accuracy which is not -1.
 */
protected void reportAccurateFieldReference(SearchMatch[] matches, QualifiedNameReference qNameRef) throws CoreException {
	if (matches == null) return; // there's nothing to accurate in this case
	int matchesLength = matches.length;

	int sourceStart = qNameRef.sourceStart;
	int sourceEnd = qNameRef.sourceEnd;
	char[][] tokens = qNameRef.tokens;

	// compute source positions of the qualified reference
	Scanner scanner = this.parser.scanner;
	scanner.setSource(this.currentPossibleMatch.getContents());
	scanner.resetTo(sourceStart, sourceEnd);
	int sourceLength = sourceEnd-sourceStart+1;

	int refSourceStart = -1, refSourceEnd = -1;
	int length = tokens.length;
	int token = -1;
	int previousValid = -1;
	int i = 0;
	int index = 0;
	do {
		int currentPosition = scanner.currentPosition;
		// read token
		try {
			token = scanner.getNextToken();
		} catch (InvalidInputException e) {
			//ignore
		}
		if (token != TerminalTokens.TokenNameEOF) {
			char[] currentTokenSource = scanner.getCurrentTokenSource();
			boolean equals = false;
			while (i < length && !(equals = this.pattern.matchesName(tokens[i++], currentTokenSource))){/*empty*/}
			if (equals && (previousValid == -1 || previousValid == i - 2)) {
				previousValid = i - 1;
				if (refSourceStart == -1)
					refSourceStart = currentPosition;
				refSourceEnd = scanner.currentPosition - 1;
			} else {
				i = 0;
				refSourceStart = -1;
				previousValid = -1;
			}
			// read '.'
			try {
				token = scanner.getNextToken();
			} catch (InvalidInputException e) {
				// ignore
			}
		}
		SearchMatch match = matches[index];
		if (match != null && match.getRule() != 0) {
			if (!encloses((IJavaElement)match.getElement())) return;
			// accept reference
			if (refSourceStart != -1) {
				match.setOffset(refSourceStart);
				match.setLength(refSourceEnd-refSourceStart+1);
				report(match);
			} else {
				match.setOffset(sourceStart);
				match.setLength(sourceLength);
				report(match);
			}
			i = 0;
		}
		refSourceStart = -1;
		previousValid = -1;
		if (index < matchesLength - 1) {
			index++;
		}
	} while (token != TerminalTokens.TokenNameEOF);

}
protected void reportBinaryMemberDeclaration(IResource resource, IMember binaryMember, Binding binaryMemberBinding, IBinaryType info, int accuracy) throws CoreException {
	ClassFile classFile = (ClassFile) binaryMember.getClassFile();
	ISourceRange range = classFile.isOpen() ? binaryMember.getNameRange() : SourceMapper.UNKNOWN_RANGE;
	if (range.getOffset() == -1) {
		BinaryType type = (BinaryType) classFile.getType();
		String sourceFileName = type.sourceFileName(info);
		if (sourceFileName != null) {
			SourceMapper mapper = classFile.getSourceMapper();
			if (mapper != null) {
				char[] contents = mapper.findSource(type, sourceFileName);
				if (contents != null)
					range = mapper.mapSource(type, contents, info, binaryMember);
			}
		}
	}
	if (resource == null) resource =  this.currentPossibleMatch.resource;
	SearchMatch match = newDeclarationMatch(binaryMember, binaryMemberBinding, accuracy, range.getOffset(), range.getLength(), getParticipant(), resource);
	report(match);
}
protected void reportMatching(LambdaExpression lambdaExpression,  IJavaElement parent, int accuracy, MatchingNodeSet nodeSet, boolean typeInHierarchy) throws CoreException {
	IJavaElement enclosingElement = null;
	// Report the lambda declaration itself.
	if (accuracy > -1) {
		enclosingElement = createHandle(lambdaExpression, parent);
		if (enclosingElement != null) { // skip if unable to find method
			// compute source positions of the selector
			int nameSourceStart = lambdaExpression.sourceStart;
			if (encloses(enclosingElement)) {
				SearchMatch match = null;
				int length = lambdaExpression.arrowPosition() + 1 - nameSourceStart;
				match = this.patternLocator.newDeclarationMatch(lambdaExpression, enclosingElement, null, accuracy, length, this);
				if (match != null) {
					report(match);
				}
			}
		}
	}
	if (enclosingElement == null) {
		enclosingElement = createHandle(lambdaExpression, parent);
	}
	// Traverse the lambda declaration to report matches inside, these matches if any should see the present lambda as the parent model element.
	ASTNode[] nodes = typeInHierarchy ? nodeSet.matchingNodes(lambdaExpression.sourceStart, lambdaExpression.sourceEnd) : null;
	boolean report = (this.matchContainer & PatternLocator.METHOD_CONTAINER) != 0 && encloses(enclosingElement);
	MemberDeclarationVisitor declarationVisitor = new MemberDeclarationVisitor(enclosingElement, report ? nodes : null, nodeSet, this, typeInHierarchy);

	if (lambdaExpression.arguments != null) {
		int argumentsLength = lambdaExpression.arguments.length;
		for (int i = 0; i < argumentsLength; i++)
			lambdaExpression.arguments[i].traverse(declarationVisitor, (BlockScope) null);
	}

	if (lambdaExpression.body != null) {
		lambdaExpression.body.traverse(declarationVisitor, (BlockScope) null);
	}

	// Report all nodes and remove them
	if (nodes != null) {
		int length = nodes.length;
		for (int i = 0; i < length; i++) {
			Integer level = (Integer) nodeSet.matchingNodes.removeKey(nodes[i]);
			if (report && level != null) {
				this.patternLocator.matchReportReference(nodes[i], enclosingElement, declarationVisitor.getLocalElement(i), declarationVisitor.getOtherElements(i), lambdaExpression.binding, level.intValue(), this);
			}
		}
	}
}
/**
 * Visit the given method declaration and report the nodes that match exactly the
 * search pattern (i.e. the ones in the matching nodes set)
 * Note that the method declaration has already been checked.
 */
protected void reportMatching(AbstractMethodDeclaration method, TypeDeclaration type, IJavaElement parent, int accuracy, boolean typeInHierarchy, MatchingNodeSet nodeSet) throws CoreException {
	IJavaElement enclosingElement = null;

	// report method declaration itself
	if (accuracy > -1) {
		enclosingElement = createHandle(method, parent);
		if (enclosingElement != null) { // skip if unable to find method
			// compute source positions of the selector
			Scanner scanner = this.parser.scanner;
			int nameSourceStart = method.sourceStart;
			scanner.setSource(this.currentPossibleMatch.getContents());
			scanner.resetTo(nameSourceStart, method.sourceEnd);
			try {
				scanner.getNextToken();
			} catch (InvalidInputException e) {
				// ignore
			}
			if (encloses(enclosingElement)) {
				SearchMatch match = null;
				if (method.isDefaultConstructor()) {
					// Use type for match associated element as default constructor does not exist in source
					int offset = type.sourceStart;
					match = this.patternLocator.newDeclarationMatch(type, parent, type.binding, accuracy, type.sourceEnd-offset+1, this);
				} else {
					int length = scanner.currentPosition - nameSourceStart;
					match = this.patternLocator.newDeclarationMatch(method, enclosingElement, method.binding, accuracy, length, this);
				}
				if (match != null) {
					report(match);
				}
			}
		}
	}

	// handle nodes for the local type first
	if ((method.bits & ASTNode.HasLocalType) != 0) {
		if (enclosingElement == null) {
			enclosingElement = createHandle(method, parent);
		}
		if (enclosingElement != null) {
			// Traverse method declaration to report matches both in local types declaration
			// and in local variables declaration
			ASTNode[] nodes = typeInHierarchy ? nodeSet.matchingNodes(method.declarationSourceStart, method.declarationSourceEnd) : null;
			boolean report = (this.matchContainer & PatternLocator.METHOD_CONTAINER) != 0 && encloses(enclosingElement);
			MemberDeclarationVisitor declarationVisitor = new MemberDeclarationVisitor(enclosingElement, report ? nodes : null, nodeSet, this, typeInHierarchy);
			try {
				method.traverse(declarationVisitor, (ClassScope) null);
			} catch (WrappedCoreException e) {
				throw e.coreException;
			}
			// Report all nodes and remove them
			if (nodes != null) {
				int length = nodes.length;
				for (int i = 0; i < length; i++) {
					Integer level = (Integer) nodeSet.matchingNodes.removeKey(nodes[i]);
					if (report && level != null) {
						this.patternLocator.matchReportReference(nodes[i], enclosingElement, declarationVisitor.getLocalElement(i), declarationVisitor.getOtherElements(i), method.binding, level.intValue(), this);
					}
				}
			}
		}
	}

	// report the type parameters
	TypeParameter[] typeParameters = method.typeParameters();
	if (typeParameters != null) {
		if (enclosingElement == null) {
			enclosingElement = createHandle(method, parent);
		}
		if (enclosingElement != null) {
			reportMatching(typeParameters, enclosingElement, parent, method.binding, nodeSet);
		}
	}

	// report annotations
	if (method.annotations != null) {
		if (enclosingElement == null) {
			enclosingElement = createHandle(method, parent);
		}
		if (enclosingElement != null) {
			reportMatching(method.annotations, enclosingElement, null, method.binding, nodeSet, true, true);
		}
	}

	// references in this method
	if (typeInHierarchy) {
		ASTNode[] nodes = nodeSet.matchingNodes(method.declarationSourceStart, method.declarationSourceEnd);
		if (nodes != null) {
			if ((this.matchContainer & PatternLocator.METHOD_CONTAINER) != 0) {
				if (enclosingElement == null) {
					enclosingElement = createHandle(method, parent);
				}
				if (encloses(enclosingElement)) {
					if (this.pattern.mustResolve) {
						// Visit only if the pattern must resolve
						MemberDeclarationVisitor declarationVisitor = new MemberDeclarationVisitor(enclosingElement, nodes, nodeSet, this, typeInHierarchy);
						method.traverse(declarationVisitor, (ClassScope) null);
						int length = nodes.length;
						for (int i = 0; i < length; i++) {
							Integer level = (Integer) nodeSet.matchingNodes.removeKey(nodes[i]);
							if (level != null) { // ensure that the reference has not been already reported while visiting
				    	        this.patternLocator.matchReportReference(nodes[i], enclosingElement, declarationVisitor.getLocalElement(i), declarationVisitor.getOtherElements(i), method.binding, level.intValue(), this);
							}
						}
					} else {
						for (int i = 0, l = nodes.length; i < l; i++) {
							ASTNode node = nodes[i];
							Integer level = (Integer) nodeSet.matchingNodes.removeKey(node);
							if (level != null) { // ensure that the reference has not been already reported while visiting
								this.patternLocator.matchReportReference(node, enclosingElement, null, null, method.binding, level.intValue(), this);
							}
						}
					}
					return;
				}
			}
			// Remove all remaining nodes
			for (int i = 0, l = nodes.length; i < l; i++) {
				nodeSet.matchingNodes.removeKey(nodes[i]);
			}
		}
	}
}
/**
 * Report matching in annotations.
 * @param otherElements TODO
 */
protected void reportMatching(Annotation[] annotations, IJavaElement enclosingElement, IJavaElement[] otherElements, Binding elementBinding, MatchingNodeSet nodeSet, boolean matchedContainer, boolean enclosesElement) throws CoreException {
	if (annotations == null)
		return;
	for (int i=0, al=annotations.length; i<al; i++) {
		Annotation annotationType = annotations[i];
		IJavaElement localAnnotation = null;
		IJavaElement[] otherAnnotations = null;
		int length = otherElements == null ? 0 : otherElements.length;
		boolean handlesCreated = false;

		// Look for annotation type ref
		TypeReference typeRef = annotationType.type;
		Integer level = (Integer) nodeSet.matchingNodes.removeKey(typeRef);
		if (level != null && enclosesElement && matchedContainer) {
			localAnnotation = createHandle(annotationType, (IAnnotatable) enclosingElement);
			if (length > 0) {
				otherAnnotations = new IJavaElement[length];
				for (int o=0; o<length; o++) {
					otherAnnotations[o] = createHandle(annotationType, (IAnnotatable) otherElements[o]);
				}
			}
			handlesCreated = true;
			this.patternLocator.matchReportReference(typeRef, enclosingElement, localAnnotation, otherAnnotations, elementBinding, level.intValue(), this);
		}

		// Look for attribute ref
		MemberValuePair[] pairs = annotationType.memberValuePairs();
		for (int j = 0, pl = pairs.length; j < pl; j++) {
			MemberValuePair pair = pairs[j];
			level = (Integer) nodeSet.matchingNodes.removeKey(pair);
			if (level != null && enclosesElement) {
				ASTNode reference = (annotationType instanceof SingleMemberAnnotation) ? (ASTNode) annotationType: pair;
				if (!handlesCreated) {
					localAnnotation = createHandle(annotationType, (IAnnotatable) enclosingElement);
					if (length > 0) {
						otherAnnotations = new IJavaElement[length];
						for (int o=0; o<length; o++) {
							otherAnnotations[o] = createHandle(annotationType, (IAnnotatable) otherElements[o]);
						}
					}
					handlesCreated = true;
				}
				this.patternLocator.matchReportReference(reference, enclosingElement, localAnnotation, otherAnnotations, pair.binding, level.intValue(), this);
			}
		}

		// Look for reference inside annotation
		ASTNode[] nodes = nodeSet.matchingNodes(annotationType.sourceStart, annotationType.declarationSourceEnd);
		if (nodes != null) {
			if (!matchedContainer) {
				for (int j = 0, nl = nodes.length; j < nl; j++) {
					nodeSet.matchingNodes.removeKey(nodes[j]);
				}
			} else {
				for (int j = 0, nl = nodes.length; j < nl; j++) {
					ASTNode node = nodes[j];
					level = (Integer) nodeSet.matchingNodes.removeKey(node);
					if (enclosesElement) {
						if (!handlesCreated) {
							localAnnotation = createHandle(annotationType, (IAnnotatable) enclosingElement);
							if (length > 0) {
								otherAnnotations = new IJavaElement[length];
								for (int o=0; o<length; o++) {
									otherAnnotations[o] = createHandle(annotationType, (IAnnotatable) otherElements[o]);
								}
							}
							handlesCreated = true;
						}
						this.patternLocator.matchReportReference(node, enclosingElement, localAnnotation, otherAnnotations, elementBinding, level.intValue(), this);
					}
				}
			}
		}
	}
}
private void reportMatching(Annotation[][] annotationsList, IJavaElement enclosingElement, Binding binding,
		MatchingNodeSet nodeSet, boolean matchedClassContainer) throws CoreException {
	if (annotationsList != null) {
		for (int i = 0, length = annotationsList.length; i < length; ++i) {
			Annotation[] annotations = annotationsList[i];
			if (annotations != null)
				reportMatching(annotations, enclosingElement, null, binding, nodeSet, matchedClassContainer, encloses(enclosingElement));
		}
	}
}
/**
 * Visit the given resolved parse tree and report the nodes that match the search pattern.
 */
protected void reportMatching(CompilationUnitDeclaration unit, boolean mustResolve) throws CoreException {
	MatchingNodeSet nodeSet = this.currentPossibleMatch.nodeSet;
	boolean locatorMustResolve = this.patternLocator.mustResolve;
	if (nodeSet.mustResolve) this.patternLocator.mustResolve = true;
	if (BasicSearchEngine.VERBOSE) {
		System.out.println("Report matching: "); //$NON-NLS-1$
		int size = nodeSet.matchingNodes==null ? 0 : nodeSet.matchingNodes.elementSize;
		System.out.print("	- node set: accurate="+ size); //$NON-NLS-1$
		size = nodeSet.possibleMatchingNodesSet==null ? 0 : nodeSet.possibleMatchingNodesSet.elementSize;
		System.out.println(", possible="+size); //$NON-NLS-1$
		System.out.print("	- must resolve: "+mustResolve); //$NON-NLS-1$
		System.out.print(" (locator: "+this.patternLocator.mustResolve); //$NON-NLS-1$
		System.out.println(", nodeSet: "+nodeSet.mustResolve+')'); //$NON-NLS-1$
		System.out.println("	- fine grain flags="+ JavaSearchPattern.getFineGrainFlagString(this.patternLocator.fineGrain())); //$NON-NLS-1$
	}
	if (mustResolve) {
		this.unitScope= unit.scope.compilationUnitScope();
		// move the possible matching nodes that exactly match the search pattern to the matching nodes set
		Object[] nodes = nodeSet.possibleMatchingNodesSet.values;
		for (int i = 0, l = nodes.length; i < l; i++) {
			ASTNode node = (ASTNode) nodes[i];
			if (node == null) continue;
			if (node instanceof ImportReference) {
				// special case for import refs: they don't know their binding
				// import ref cannot be in the hierarchy of a type
				if (this.hierarchyResolver != null) continue;

				ImportReference importRef = (ImportReference) node;
				boolean inModule = (importRef.bits & ASTNode.inModule) != 0;
				boolean getOnDemand = (importRef.bits & ASTNode.OnDemand) != 0 || inModule;
				Binding binding = getOnDemand
					? this.unitScope.getImport(CharOperation.subarray(importRef.tokens, 0, importRef.tokens.length), true, importRef.isStatic())
					: this.unitScope.getImport(importRef.tokens, false, importRef.isStatic());
				if (inModule) {
					nodeSet.addMatch(node, this.patternLocator.resolveLevel(binding)); // report all module-info together
				} else {
					this.patternLocator.matchLevelAndReportImportRef(importRef, binding, this);
				}
			} else {
				nodeSet.addMatch(node, this.patternLocator.resolveLevel(node));
			}
		}
		nodeSet.possibleMatchingNodesSet = new SimpleSet(3);
		if (BasicSearchEngine.VERBOSE) {
			int size = nodeSet.matchingNodes==null ? 0 : nodeSet.matchingNodes.elementSize;
			System.out.print("	- node set: accurate="+size); //$NON-NLS-1$
			size = nodeSet.possibleMatchingNodesSet==null ? 0 : nodeSet.possibleMatchingNodesSet.elementSize;
			System.out.println(", possible="+size); //$NON-NLS-1$
		}
	} else {
		this.unitScope = null;
	}

	if (nodeSet.matchingNodes.elementSize == 0) return; // no matching nodes were found
	this.methodHandles = new HashSet();

	boolean matchedUnitContainer = (this.matchContainer & PatternLocator.COMPILATION_UNIT_CONTAINER) != 0;

	// report references in javadoc
	if (unit.javadoc != null) {
		ASTNode[] nodes = nodeSet.matchingNodes(unit.javadoc.sourceStart, unit.javadoc.sourceEnd);
		if (nodes != null) {
			if (!matchedUnitContainer) {
				for (int i = 0, l = nodes.length; i < l; i++)
					nodeSet.matchingNodes.removeKey(nodes[i]);
			} else {
				IJavaElement element = createPackageDeclarationHandle(unit);
				for (int i = 0, l = nodes.length; i < l; i++) {
					ASTNode node = nodes[i];
					Integer level = (Integer) nodeSet.matchingNodes.removeKey(node);
					if (encloses(element)) {
						this.patternLocator.matchReportReference(node, element, null, null, null/*no binding*/, level.intValue(), this);
					}
				}
			}
		}
	}

	if (matchedUnitContainer) {
		ImportReference pkg = unit.currentPackage;
		if (pkg != null && pkg.annotations != null) {
			IJavaElement element = createPackageDeclarationHandle(unit);
			if (element != null) {
				reportMatching(pkg.annotations, element, null, null, nodeSet, true, encloses(element));
			}
		}

		ImportReference[] imports = unit.imports;
		if (imports != null) {
			for (int i = 0, l = imports.length; i < l; i++) {
				ImportReference importRef = imports[i];
				Integer level = (Integer) nodeSet.matchingNodes.removeKey(importRef);
				if (level != null) {
					this.patternLocator.matchReportImportRef(importRef, null /*no binding*/, createImportHandle(importRef), level.intValue(), this);
				}
			}
		}
	}

	TypeDeclaration[] types = unit.types;
	if (types != null) {
		for (int i = 0, l = types.length; i < l; i++) {
			if (nodeSet.matchingNodes.elementSize == 0) return; // reported all the matching nodes
			TypeDeclaration type = types[i];
			Integer level = (Integer) nodeSet.matchingNodes.removeKey(type);
			int accuracy = (level != null && matchedUnitContainer) ? level.intValue() : -1;
			this.inTypeOccurrencesCounts = new HashtableOfIntValues();
			reportMatching(type, null, accuracy, nodeSet, 1);
		}
	} else if (unit.moduleDeclaration != null) {
		ModuleDeclaration mod = unit.moduleDeclaration;
		Integer level = (Integer) nodeSet.matchingNodes.removeKey(mod);
		int accuracy = (level != null && matchedUnitContainer) ? level.intValue() : -1;
		reportMatching(mod, null, accuracy, nodeSet, 1);
	}

	// Clear handle cache
	this.methodHandles = null;
	this.bindings.removeKey(this.pattern);
	this.patternLocator.mustResolve = locatorMustResolve;
}
/**
 * Visit the given field declaration and report the nodes that match exactly the
 * search pattern (i.e. the ones in the matching nodes set)
 */
protected void reportMatching(FieldDeclaration field, FieldDeclaration[] otherFields, TypeDeclaration type, IJavaElement parent, int accuracy, boolean typeInHierarchy, MatchingNodeSet nodeSet) throws CoreException {
	IJavaElement enclosingElement = null;
	if (accuracy > -1) {
		enclosingElement = createHandle(field, type, parent);
		if (encloses(enclosingElement)) {
			int offset = field.sourceStart;
			SearchMatch match = newDeclarationMatch(enclosingElement, field.binding, accuracy, offset, field.sourceEnd-offset+1);
			if (field.initialization instanceof AllocationExpression) {
				reportAccurateEnumConstructorReference(match, field, (AllocationExpression) field.initialization);
			} else {
				report(match);
			}
		}
	}

	// handle the nodes for the local type first
	if ((field.bits & ASTNode.HasLocalType) != 0) {
		if (enclosingElement == null) {
			enclosingElement = createHandle(field, type, parent);
		}
		// Traverse field declaration(s) to report matches both in local types declaration
		// and in local variables declaration
		int fieldEnd = field.endPart2Position == 0 ? field.declarationSourceEnd : field.endPart2Position;
		ASTNode[] nodes = typeInHierarchy ? nodeSet.matchingNodes(field.sourceStart, fieldEnd) : null;
		boolean report = (this.matchContainer & PatternLocator.FIELD_CONTAINER) != 0 && encloses(enclosingElement);
		MemberDeclarationVisitor declarationVisitor = new MemberDeclarationVisitor(enclosingElement, report ? nodes : null, nodeSet, this, typeInHierarchy);
		try {
			field.traverse(declarationVisitor, (MethodScope) null);
		} catch (WrappedCoreException e) {
			throw e.coreException;
		}
		// Report all nodes and remove them
		if (nodes != null) {
			int length = nodes.length;
			for (int i = 0; i < length; i++) {
				ASTNode node = nodes[i];
				Integer level = (Integer) nodeSet.matchingNodes.removeKey(node);
				if (report && level != null) {
					if (node instanceof TypeDeclaration) {
						// use field declaration to report match (see bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=88174)
						AllocationExpression allocation = ((TypeDeclaration)node).allocation;
						if (allocation != null && allocation.enumConstant != null) {
							node = field;
						}
					}
	    	        this.patternLocator.matchReportReference(node, enclosingElement, declarationVisitor.getLocalElement(i), declarationVisitor.getOtherElements(i), field.binding, level.intValue(), this);
				}
			}
		}
	}

	// report annotations
	IJavaElement[] otherElements = null;
	if (field.annotations != null) {
		if (enclosingElement == null) {
			enclosingElement = createHandle(field, type, parent);
		}
		if (otherFields != null) {
			otherElements = createHandles(otherFields, type, parent);
		}
		reportMatching(field.annotations, enclosingElement, otherElements, field.binding, nodeSet, true, true);
	}

	if (typeInHierarchy) {
		// Look at field declaration
		if (field.endPart1Position != 0) { // not necessary if field is an initializer
			ASTNode[] nodes = nodeSet.matchingNodes(field.declarationSourceStart, field.endPart1Position);
			if (nodes != null) {
				if ((this.matchContainer & PatternLocator.FIELD_CONTAINER) == 0) {
					for (int i = 0, l = nodes.length; i < l; i++)
						nodeSet.matchingNodes.removeKey(nodes[i]);
				} else {
					if (enclosingElement == null)
						enclosingElement = createHandle(field, type, parent);
					if (encloses(enclosingElement)) {
						for (int i = 0, l = nodes.length; i < l; i++) {
							ASTNode node = nodes[i];
							Integer level = (Integer) nodeSet.matchingNodes.removeKey(node);
							if (otherFields != null && otherElements == null) {
								otherElements = createHandles(otherFields, type, parent);
							}
							this.patternLocator.matchReportReference(node, enclosingElement, null, otherElements, field.binding, level.intValue(), this);
						}
					}
				}
			}
		}

		// Look in initializer
		int fieldEnd = field.endPart2Position == 0 ? field.declarationSourceEnd : field.endPart2Position;
		ASTNode[] nodes = nodeSet.matchingNodes(field.sourceStart, fieldEnd);
		if (nodes != null) {
			if ((this.matchContainer & PatternLocator.FIELD_CONTAINER) == 0) {
				for (int i = 0, l = nodes.length; i < l; i++) {
					nodeSet.matchingNodes.removeKey(nodes[i]);
				}
			} else {
				if (enclosingElement == null) {
					enclosingElement = createHandle(field, type, parent);
				}
				if (encloses(enclosingElement)) {
					MemberDeclarationVisitor declarationVisitor = new MemberDeclarationVisitor(enclosingElement, nodes, nodeSet, this, typeInHierarchy);
					field.traverse(declarationVisitor, (MethodScope) null);
					int length = nodes.length;
					for (int i = 0; i < length; i++) {
						ASTNode node = nodes[i];
						Integer level = (Integer) nodeSet.matchingNodes.removeKey(node);
						if (level != null) { // ensure that the reference has not been already reported while visiting
							if (node instanceof TypeDeclaration) {
								// use field declaration to report match (see bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=88174)
								AllocationExpression allocation = ((TypeDeclaration)node).allocation;
								if (allocation != null && allocation.enumConstant != null) {
									node = field;
								}
							}
			    	        this.patternLocator.matchReportReference(node, enclosingElement, declarationVisitor.getLocalElement(i), declarationVisitor.getOtherElements(i), field.binding, level.intValue(), this);
						}
					}
					return;
				}
			}
		}
	}
}
/**
 * Visit the given module declaration and report the nodes that match exactly the
 * search pattern (i.e. the ones in the matching nodes set)
 */
protected void reportMatching(ModuleDeclaration module, IJavaElement parent, int accuracy, MatchingNodeSet nodeSet, int occurrenceCount) throws CoreException {
	if (this.currentPossibleMatch.autoModuleName != null && accuracy > -1) {
		reportMatchingAutoModule(module, parent, accuracy);
		return;
	}
	IModuleDescription moduleDesc =  null;
	Openable openable = this.currentPossibleMatch.openable;
	if (openable instanceof ITypeRoot) {
		ITypeRoot typeRoot = (ITypeRoot) openable;
		try {
			moduleDesc =  typeRoot.getModule();
		} catch (JavaModelException e) {
			// do nothing
		}
	}
	if (moduleDesc == null) // could theoretically happen if openable is ICompilationUnit, but logically having a module should prevent this from happening
		return;
	reportMatching(module.annotations, moduleDesc, null, module.binding, nodeSet, true, true);
	if (accuracy > -1) { // report module declaration
		SearchMatch match = this.patternLocator.newDeclarationMatch(module, moduleDesc, module.binding, accuracy, module.moduleName.length, this);
		report(match);
	}
	reportMatching(module.requires, module,  nodeSet, moduleDesc);
	reportMatching(module.exports, nodeSet, moduleDesc);
	reportMatching(module.opens, nodeSet, moduleDesc);
	reportMatching(module.services, module, nodeSet, moduleDesc);
	reportMatching(module.uses, module, nodeSet, moduleDesc);
}
private void reportMatchingAutoModule(ModuleDeclaration module, IJavaElement parent, int accuracy) throws CoreException {
	IModuleDescription autoModule = new AbstractModule.AutoModule( this.currentPossibleMatch.openable, this.currentPossibleMatch.autoModuleName, true);
	SearchMatch match = this.patternLocator.newDeclarationMatch(module, autoModule, module.binding, accuracy, module.moduleName.length, this);
	report(match);
}

private void reportMatching(RequiresStatement[] reqs, ModuleDeclaration module, MatchingNodeSet nodeSet, IModuleDescription moduleDesc) {
	if (reqs == null || reqs.length == 0)
		return;
	try {
		for (RequiresStatement req : reqs) {
			Integer level = (Integer) nodeSet.matchingNodes.removeKey(req.module);
			if (level != null) {
				this.patternLocator.matchReportReference(req.module, moduleDesc, req.resolvedBinding, level.intValue(), this);
			}
		}
	} catch (CoreException e) {
		// do nothing
	}
}

private void reportMatching(PackageVisibilityStatement[] psvs, MatchingNodeSet nodeSet, IModuleDescription moduleDesc)
		throws JavaModelException, CoreException {
	if (psvs != null && psvs.length > 0) {
		for (PackageVisibilityStatement psv : psvs) {
			ImportReference importRef = psv.pkgRef;
			Integer level = (Integer) nodeSet.matchingNodes.removeKey(importRef);
			if (level != null) {
				Binding binding = this.unitScope.getImport(CharOperation.subarray(importRef.tokens, 0, importRef.tokens.length), true, false);
				this.patternLocator.matchReportImportRef(importRef, binding, moduleDesc, level.intValue(), this);
			}
			ModuleReference[] tgts = psv.targets;
			if (tgts == null || tgts.length == 0) continue;
			for (ModuleReference tgt : tgts) {
				level = (Integer) nodeSet.matchingNodes.removeKey(tgt);
				if (level != null) {
					this.patternLocator.matchReportReference(tgt, moduleDesc, tgt.resolve(this.unitScope), level.intValue(), this);
				}
			}
		}
	}
}
private void reportMatching(ProvidesStatement[] provides, ModuleDeclaration module, MatchingNodeSet nodeSet, IModuleDescription moduleDesc) throws JavaModelException, CoreException {
	if (provides != null && provides.length > 0) {
		for (ProvidesStatement service : provides) {
			TypeReference intf = service.serviceInterface;
			if (intf != null) {
				Integer level = (Integer) nodeSet.matchingNodes.removeKey(intf);
				if (level != null)
					this.patternLocator.matchReportReference(intf, moduleDesc, null, null, module.binding, level.intValue(), this);
			}
			TypeReference[] impls = service.implementations;
			for (TypeReference impl : impls) {
				if (impl != null) {
					Integer level = (Integer) nodeSet.matchingNodes.removeKey(impl);
					if (level != null)
						this.patternLocator.matchReportReference(impl, moduleDesc, null, null, module.binding, level.intValue(), this);
				}
			}
		}
	}
}
private void reportMatching(UsesStatement[] uses, ModuleDeclaration module, MatchingNodeSet nodeSet, IModuleDescription moduleDesc) {
	if (uses != null && uses.length > 0) {
		try {
			for (UsesStatement service : uses) {
				TypeReference intf = service.serviceInterface;
				if (intf != null) {
					Integer level = (Integer) nodeSet.matchingNodes.removeKey(intf);
					if (level != null) {
						this.patternLocator.matchReportReference(intf, moduleDesc, null, null, module.binding, level.intValue(), this);
					}
				}
			}
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

/**
 * Visit the given type declaration and report the nodes that match exactly the
 * search pattern (i.e. the ones in the matching nodes set)
 */
protected void reportMatching(TypeDeclaration type, IJavaElement parent, int accuracy, MatchingNodeSet nodeSet, int occurrenceCount) throws CoreException {
	// create type handle
	IJavaElement enclosingElement = parent;
	if (enclosingElement == null) {
		enclosingElement = createTypeHandle(new String(type.name));
	} else if (enclosingElement instanceof IType) {
		enclosingElement = ((IType) parent).getType(new String(type.name));
	} else if (enclosingElement instanceof IMember) {
	    IMember member = (IMember) parent;
		if (member.isBinary())  {
			IOpenable openable = enclosingElement.getOpenable();
			IJavaElement anonType = null;
			if (openable instanceof ClassFile) {
				BinaryType binaryType = (BinaryType)((ClassFile) openable).getType();
				String fileName = binaryType.getPath().toOSString();
				if ((type.bits & ASTNode.IsAnonymousType) != 0) {
					if (fileName != null) {
						if (fileName.endsWith("jar") || fileName.endsWith(SuffixConstants.SUFFIX_STRING_class)) { //$NON-NLS-1$
							IOrdinaryClassFile classFile= binaryType.getPackageFragment().getOrdinaryClassFile(binaryType.getTypeQualifiedName() +
									"$" + Integer.toString(occurrenceCount) + SuffixConstants.SUFFIX_STRING_class);//$NON-NLS-1$
							anonType =  classFile.getType();
						}
					} else {
						// TODO: JAVA 9 - JIMAGE to be included later - currently assuming that only .class files will be dealt here.
					}
				}
			}
			enclosingElement = anonType != null ? anonType : ((IOrdinaryClassFile)this.currentPossibleMatch.openable).getType() ;
		} else {
			enclosingElement = member.getType(new String(type.name), occurrenceCount);
		}
	}
	if (enclosingElement == null) return;
	boolean enclosesElement = encloses(enclosingElement);

	// report the type declaration
	if (accuracy > -1 && enclosesElement) {
		int offset = type.sourceStart;
		SearchMatch match = this.patternLocator.newDeclarationMatch(type, enclosingElement, type.binding, accuracy, type.sourceEnd-offset+1, this);
		report(match);
	}

	boolean matchedClassContainer = (this.matchContainer & PatternLocator.CLASS_CONTAINER) != 0;

	// report the type parameters
	if (type.typeParameters != null) {
		reportMatching(type.typeParameters, enclosingElement, parent, type.binding, nodeSet);
	}

	// report annotations
	if (type.annotations != null) {
		reportMatching(type.annotations, enclosingElement, null, type.binding, nodeSet, matchedClassContainer, enclosesElement);
	}

	// report references in javadoc
	if (type.javadoc != null) {
		ASTNode[] nodes = nodeSet.matchingNodes(type.declarationSourceStart, type.sourceStart);
		if (nodes != null) {
			if (!matchedClassContainer) {
				for (int i = 0, l = nodes.length; i < l; i++)
					nodeSet.matchingNodes.removeKey(nodes[i]);
			} else {
				for (int i = 0, l = nodes.length; i < l; i++) {
					ASTNode node = nodes[i];
					Integer level = (Integer) nodeSet.matchingNodes.removeKey(node);
					if (enclosesElement) {
						this.patternLocator.matchReportReference(node, enclosingElement, null, null, type.binding, level.intValue(), this);
					}
				}
			}
		}
	}

	// super types
	if ((type.bits & ASTNode.IsAnonymousType) != 0) {
		TypeReference superType = type.allocation.type;
		if (superType != null) {
			Integer level = (Integer) nodeSet.matchingNodes.removeKey(superType);
			if (level != null && matchedClassContainer)
				this.patternLocator.matchReportReference(superType, enclosingElement, null, null, type.binding, level.intValue(), this);
		}
	} else {
		TypeReference superClass = type.superclass;
		if (superClass != null) {
			reportMatchingSuperOrPermit(superClass, enclosingElement, type.binding, nodeSet, matchedClassContainer);
			for (int i = 0, length = superClass.annotations == null ? 0 : superClass.annotations.length; i < length; i++) {
				Annotation[] annotations = superClass.annotations[i];
				if (annotations == null) continue;
				reportMatching(annotations, enclosingElement, null, type.binding, nodeSet, matchedClassContainer, enclosesElement);
			}
		}
		TypeReference[] superInterfaces = type.superInterfaces;
		if (superInterfaces != null) {
			for (int i = 0, l = superInterfaces.length; i < l; i++) {
				reportMatchingSuperOrPermit(superInterfaces[i], enclosingElement, type.binding, nodeSet, matchedClassContainer);
				TypeReference typeReference  = type.superInterfaces[i];
				Annotation[][] annotations = typeReference != null ? typeReference.annotations : null;
				if (annotations != null) {
					for (int j = 0, length = annotations.length; j < length; j++) {
						if (annotations[j] == null) continue;
						reportMatching(annotations[j], enclosingElement, null, type.binding, nodeSet, matchedClassContainer, enclosesElement);
					}
				}
			}
		}
		TypeReference[] permittedTypes = type.permittedTypes;
		if (permittedTypes != null) {
			for (int i = 0, l = permittedTypes.length; i < l; i++) {
				reportMatchingSuperOrPermit(permittedTypes[i], enclosingElement, type.binding, nodeSet, matchedClassContainer);
				TypeReference typeReference  = type.permittedTypes[i];
				Annotation[][] annotations = typeReference != null ? typeReference.annotations : null;
				if (annotations != null) {
					for (int j = 0, length = annotations.length; j < length; j++) {
						if (annotations[j] == null) continue;
						reportMatching(annotations[j], enclosingElement, null, type.binding, nodeSet, matchedClassContainer, enclosesElement);
					}
				}
			}
		}

	}

	// filter out element not in hierarchy scope
	boolean typeInHierarchy = type.binding == null || typeInHierarchy(type.binding);
	matchedClassContainer = matchedClassContainer && typeInHierarchy;

	// Visit fields
	FieldDeclaration[] fields = type.fields;
	if (fields != null) {
		if (nodeSet.matchingNodes.elementSize == 0) return;	// end as all matching nodes were reported
		FieldDeclaration[] otherFields = null;
		int first = -1;
		int length = fields.length;
		for (int i = 0; i < length; i++) {
			FieldDeclaration field = fields[i];
			boolean last = field.endPart2Position == 0 || field.declarationEnd == field.endPart2Position;
			// Store first index of multiple field declaration
			if (!last) {
				if (first == -1) {
					first = i;
				}
			}
			if (first >= 0) {
				// Store all multiple fields but first one for other elements
				if (i > first) {
					if (otherFields == null) {
						otherFields = new FieldDeclaration[length-i];
					}
					otherFields[i-1-first] = field;
				}
				// On last field, report match with all other elements
				if (last) {
					for (int j=first; j<=i; j++) {
						Integer level = (Integer) nodeSet.matchingNodes.removeKey(fields[j]);
						int value = (level != null && matchedClassContainer) ? level.intValue() : -1;
						reportMatching(fields[j], otherFields, type, enclosingElement, value, typeInHierarchy, nodeSet);
					}
					first = -1;
					otherFields = null;
				}
			} else {
				// Single field, report normally
				Integer level = (Integer) nodeSet.matchingNodes.removeKey(field);
				int value = (level != null && matchedClassContainer) ? level.intValue() : -1;
				reportMatching(field, null, type, enclosingElement, value, typeInHierarchy, nodeSet);
			}
		}
	}

	// Visit methods
	AbstractMethodDeclaration[] methods = type.methods;
	if (methods != null) {
		if (nodeSet.matchingNodes.elementSize == 0) return;	// end as all matching nodes were reported
		for (int i = 0, l = methods.length; i < l; i++) {
			AbstractMethodDeclaration method = methods[i];
			Integer level = (Integer) nodeSet.matchingNodes.removeKey(method);
			int value = (level != null && matchedClassContainer) ? level.intValue() : -1;
			reportMatching(method, type, enclosingElement, value, typeInHierarchy, nodeSet);
		}
	}

	// Visit types
	TypeDeclaration[] memberTypes = type.memberTypes;
	if (memberTypes != null) {
		for (int i = 0, l = memberTypes.length; i < l; i++) {
			if (nodeSet.matchingNodes.elementSize == 0) return;	// end as all matching nodes were reported
			TypeDeclaration memberType = memberTypes[i];
			Integer level = (Integer) nodeSet.matchingNodes.removeKey(memberType);
			int value = (level != null && matchedClassContainer) ? level.intValue() : -1;
			reportMatching(memberType, enclosingElement, value, nodeSet, 1);
		}
	}
}
/**
 * Report matches in type parameters.
 */
protected void reportMatching(TypeParameter[] typeParameters, IJavaElement enclosingElement, IJavaElement parent, Binding binding, MatchingNodeSet nodeSet) throws CoreException {
	if (typeParameters == null) return;
	for (int i=0, l=typeParameters.length; i<l; i++) {
		TypeParameter typeParameter = typeParameters[i];
		if (typeParameter != null) {
			Integer level = (Integer) nodeSet.matchingNodes.removeKey(typeParameter);
			if (level != null) {
				if (level.intValue() > -1 && encloses(enclosingElement)) {
					int offset = typeParameter.sourceStart;
					SearchMatch match = this.patternLocator.newDeclarationMatch(typeParameter, enclosingElement, binding, level.intValue(), typeParameter.sourceEnd-offset+1, this);
					report(match);
				}
			}
			boolean matchedClassContainer = (this.matchContainer & PatternLocator.ALL_CONTAINER) != 0;
			if (typeParameter.annotations != null) {
				reportMatching(typeParameter.annotations, enclosingElement, null, typeParameter.binding, nodeSet, matchedClassContainer, encloses(enclosingElement));
			}
			if (typeParameter.type != null) {
				reportMatching(typeParameter.type.annotations, enclosingElement, typeParameter.binding, nodeSet, matchedClassContainer);
				level = (Integer) nodeSet.matchingNodes.removeKey(typeParameter.type);
				if (level != null) {
					IJavaElement localElement = createHandle(typeParameter, enclosingElement);
					this.patternLocator.matchReportReference(typeParameter.type, enclosingElement, localElement, null, binding, level.intValue(), this);
				}
				if (typeParameter.type instanceof ParameterizedSingleTypeReference) {
                    ParameterizedSingleTypeReference paramSTR = (ParameterizedSingleTypeReference) typeParameter.type;
                    if (paramSTR.typeArguments != null) {
                    	int length = paramSTR.typeArguments.length;
                    	for (int k=0; k<length; k++) {
							TypeReference typeArgument = paramSTR.typeArguments[k];
							reportMatching(typeArgument.annotations, enclosingElement, typeArgument.resolvedType, nodeSet, matchedClassContainer);
							level = (Integer) nodeSet.matchingNodes.removeKey(typeArgument);
							if (level != null) {
								IJavaElement localElement = createHandle(typeParameter, enclosingElement);
								this.patternLocator.matchReportReference(typeArgument, enclosingElement, localElement, null, binding, level.intValue(), this);
							}
							if (typeArgument instanceof Wildcard) {
	                            TypeReference wildcardBound = ((Wildcard) typeArgument).bound;
	                            if (wildcardBound != null) {
		            				reportMatching(wildcardBound.annotations, enclosingElement, wildcardBound.resolvedType, nodeSet, matchedClassContainer);
									level = (Integer) nodeSet.matchingNodes.removeKey(wildcardBound);
									if (level != null) {
										IJavaElement localElement = createHandle(typeParameter, enclosingElement);
										this.patternLocator.matchReportReference(wildcardBound, enclosingElement, localElement, null, binding, level.intValue(), this);
									}
	                            }
                            }
                    	}
                    }
				}
			}
			if (typeParameter.bounds != null) {
				for (int j=0, b=typeParameter.bounds.length; j<b; j++) {
					TypeReference typeParameterBound = typeParameter.bounds[j];
					if (typeParameterBound.annotations != null) {
						reportMatching(typeParameterBound.annotations, enclosingElement, binding,nodeSet, matchedClassContainer);
					}
					level = (Integer) nodeSet.matchingNodes.removeKey(typeParameterBound);
					if (level != null) {
						IJavaElement localElement = createHandle(typeParameter, enclosingElement);
						this.patternLocator.matchReportReference(typeParameterBound, enclosingElement, localElement, null, binding, level.intValue(), this);
					}
					if (typeParameterBound instanceof ParameterizedSingleTypeReference) {
	                    ParameterizedSingleTypeReference paramSTR = (ParameterizedSingleTypeReference) typeParameterBound;
	                    if (paramSTR.typeArguments != null) {
	                    	int length = paramSTR.typeArguments.length;
	                    	for (int k=0; k<length; k++) {
								TypeReference typeArgument = paramSTR.typeArguments[k];
								if (typeArgument.annotations != null) {
									reportMatching(typeArgument.annotations, enclosingElement, binding,nodeSet, matchedClassContainer);
								}
								level = (Integer) nodeSet.matchingNodes.removeKey(typeArgument);
								if (level != null) {
									IJavaElement localElement = createHandle(typeParameter, enclosingElement);
									this.patternLocator.matchReportReference(typeArgument, enclosingElement, localElement, null, binding, level.intValue(), this);
								}
								if (typeArgument instanceof Wildcard) {
		                            TypeReference wildcardBound = ((Wildcard) typeArgument).bound;
		                            if (wildcardBound != null) {
		            					if (wildcardBound.annotations != null) {
		            						reportMatching(wildcardBound.annotations, enclosingElement, binding,nodeSet, matchedClassContainer);
		            					}
										level = (Integer) nodeSet.matchingNodes.removeKey(wildcardBound);
										if (level != null) {
											IJavaElement localElement = createHandle(typeParameter, enclosingElement);
											this.patternLocator.matchReportReference(wildcardBound, enclosingElement, localElement, null, binding, level.intValue(), this);
										}
		                            }
	                            }
	                    	}
	                    }
                    }
				}
			}
		}
	}
}
protected void reportMatchingSuperOrPermit(TypeReference superReference, IJavaElement enclosingElement, Binding elementBinding, MatchingNodeSet nodeSet, boolean matchedClassContainer) throws CoreException {
	ASTNode[] nodes = null;
	if (superReference instanceof ParameterizedSingleTypeReference || superReference instanceof ParameterizedQualifiedTypeReference) {
		long lastTypeArgumentInfo = findLastTypeArgumentInfo(superReference);
		nodes = nodeSet.matchingNodes(superReference.sourceStart, (int)lastTypeArgumentInfo);
	}
	if (nodes != null) {
		if ((this.matchContainer & PatternLocator.CLASS_CONTAINER) == 0) {
			for (int i = 0, l = nodes.length; i < l; i++)
				nodeSet.matchingNodes.removeKey(nodes[i]);
		} else {
			if (encloses(enclosingElement))
				for (int i = 0, l = nodes.length; i < l; i++) {
					ASTNode node = nodes[i];
					Integer level = (Integer) nodeSet.matchingNodes.removeKey(node);
					this.patternLocator.matchReportReference(node, enclosingElement, null, null, elementBinding, level.intValue(), this);
				}
		}
	} else if (encloses(enclosingElement)) {
		Integer level = (Integer) nodeSet.matchingNodes.removeKey(superReference);
		if (level != null && matchedClassContainer)
			this.patternLocator.matchReportReference(superReference, enclosingElement, null, null, elementBinding, level.intValue(), this);
	}
}
protected boolean typeInHierarchy(ReferenceBinding binding) {
	if (this.hierarchyResolver == null) return true; // not a hierarchy scope
	if (this.hierarchyResolver.subOrSuperOfFocus(binding)) return true;

	if (this.allSuperTypeNames != null) {
		char[][] compoundName = binding.compoundName;
		for (int i = 0, length = this.allSuperTypeNames.length; i < length; i++)
			if (CharOperation.equals(compoundName, this.allSuperTypeNames[i]))
				return true;
	}
	return false;
}
}
