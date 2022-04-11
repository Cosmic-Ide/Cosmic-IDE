/*******************************************************************************
 * Copyright (c) 2015, 2019 GK Software AG.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Stephan Herrmann - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.core.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.internal.compiler.classfmt.ExternalAnnotationProvider;
import org.eclipse.jdt.internal.compiler.codegen.ConstantPool;
import org.eclipse.jdt.internal.compiler.lookup.SignatureWrapper;
import org.eclipse.jdt.internal.core.ClasspathEntry;
import org.eclipse.jdt.internal.core.util.KeyToSignature;

/**
 * Utilities for accessing and manipulating text files that externally define annotations for a given Java type.
 * Files are assumed to be in ".eea format", a textual representation of annotated signatures of members of a given type.
 *
 * @since 3.11
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public final class ExternalAnnotationUtil {

	/** Representation of a 'nullable' annotation, independent of the concrete annotation name used in Java sources. */
	public static final char NULLABLE = ExternalAnnotationProvider.NULLABLE;

	/** Representation of a 'nonnull' annotation, independent of the concrete annotation name used in Java sources. */
	public static final char NONNULL = ExternalAnnotationProvider.NONNULL;

	/**
	 * Represents absence of a null annotation. Useful for removing an existing null annotation.
	 * This character is used only internally, it is not part of the Eclipse External Annotation file format.
	 */
	public static final char NO_ANNOTATION = ExternalAnnotationProvider.NO_ANNOTATION;

	/** Strategy for merging a new signature with an existing (possibly annotated) signature. */
	public static enum MergeStrategy {
		/** Unconditionally replace the signature. */
		REPLACE_SIGNATURE,
		/** Override existing annotations, keeping old annotations in locations that are not annotated in the new signature. */
		OVERWRITE_ANNOTATIONS,
		/** Only add new annotations, never remove or overwrite existing annotations. */
		ADD_ANNOTATIONS
	}

	private static final int POSITION_RETURN_TYPE = -1;
	private static final int POSITION_FULL_SIGNATURE = -2;
	private static final int POSITION_TYPE_PARAMETER = -3;

	/**
	 * Answer the give method's signature in class file format.
	 * @param methodBinding binding representing a method
	 * @return a signature in class file format
	 */
	public static String extractGenericSignature(IMethodBinding methodBinding) {
		// Note that IMethodBinding.binding is not accessible, hence we need to recover the signature from the key:
		KeyToSignature parser = new KeyToSignature(methodBinding.getKey(), KeyToSignature.SIGNATURE, true);
		parser.parse();
		return parser.toString();
	}

	/**
	 * Answer the given types's signature in class file format.
	 * @param type binding representing a type
	 * @return a signature in class file format
	 */
	public static String extractGenericTypeSignature(ITypeBinding type) {
		KeyToSignature parser = new KeyToSignature(type.getKey(), KeyToSignature.SIGNATURE, true);
		parser.parse();
		return parser.toString();
	}

	/**
	 * Answer the signature of all type parameters of this type in class file format.
	 * @param type binding representing a type, required to have type parameters
	 * @return a signature in class file format
	 * @since 3.19
	 */
	public static String extractGenericTypeParametersSignature(ITypeBinding type) {
		StringBuilder signature = new StringBuilder().append('<');
		for (ITypeBinding typeParameter : type.getTypeParameters()) {
			signature.append(typeParameter.getName());
			// superclass (if relevant, else empty ':'):
			signature.append(':');
			ITypeBinding superclass = typeParameter.getSuperclass();
			if (superclass != null) {
				String superclassSignature = extractGenericTypeSignature(superclass);
				boolean superIsRelevant = typeParameter.getInterfaces().length == 0;
				superIsRelevant |= !superclassSignature.equals(new String(ConstantPool.JavaLangObjectSignature));
				if (superIsRelevant) {
					signature.append(superclassSignature);
				}
			}
			// superinterfaces:
			for (ITypeBinding superInterface : typeParameter.getInterfaces()) {
				signature.append(':').append(extractGenericTypeSignature(superInterface));
			}
		}
		signature.append('>');
		return signature.toString();
	}

	/**
	 * Insert an encoded annotation into the given methodSignature affecting its return type.
	 * <p>
	 * This method is suitable for declaration annotations.
	 * </p>
	 * @param methodSignature a method signature in class file format
	 * @param annotation one of {@link #NULLABLE} and {@link #NONNULL}.
	 * @param mergeStrategy when passing {@link MergeStrategy#ADD_ANNOTATIONS} this method will
	 * 	refuse to overwrite any existing annotation in the specified location
	 * @return the modified method signature, or the original signature if modification would
	 *	conflict with the given merge strategy.
	 * @throws IllegalArgumentException if the method signature is malformed or its return type is not a reference type.
	 */
	public static String insertReturnAnnotation(String methodSignature, char annotation, MergeStrategy mergeStrategy) {
		int close = methodSignature.indexOf(')');
		if (close == -1 || close > methodSignature.length()-4)
			throw new IllegalArgumentException("Malformed method signature"); //$NON-NLS-1$
		switch (methodSignature.charAt(close+1)) {
			case 'L': case 'T': case '[':
				return insertAt(methodSignature, close+2, annotation, mergeStrategy);
		}
		throw new IllegalArgumentException("Return type is not a reference type"); //$NON-NLS-1$
	}

	/**
	 * Insert an encoded annotation into the given methodSignature affecting one of its parameters.
	 * <p>
	 * This method is suitable for declaration annotations.
	 * </p>
	 * @param methodSignature a method signature in class file format
	 * @param paramIdx 0-based index of the parameter to which the annotation should be attached
	 * @param annotation one of {@link #NULLABLE} and {@link #NONNULL}.
	 * @param mergeStrategy when passing {@link MergeStrategy#ADD_ANNOTATIONS} this method will
	 * 	refuse to overwrite any existing annotation in the specified location
	 * @return the modified method signature, or the original signature if modification would
	 *	conflict with the given merge strategy.
	 * @throws IllegalArgumentException if the method signature is malformed or its specified parameter type is not a reference type.
	 */
	public static String insertParameterAnnotation(String methodSignature, int paramIdx, char annotation, MergeStrategy mergeStrategy)
	{
		SignatureWrapper wrapper = new SignatureWrapper(methodSignature.toCharArray());
		wrapper.start = 1;
		for (int i = 0; i < paramIdx; i++)
			wrapper.start = wrapper.computeEnd() + 1;
		int start = wrapper.start;
		switch (methodSignature.charAt(start)) {
			case 'L': case 'T': case '[':
				return insertAt(methodSignature, start+1, annotation, mergeStrategy);
		}
		throw new IllegalArgumentException("Paramter type is not a reference type"); //$NON-NLS-1$
	}

	/**
	 * Answer the external annotation file corresponding to the given type as seen from the given project.
	 * Note that manipulation of external annotations is only supported for annotation files in the workspace,
	 * and only in directory layout, not from zip files.
	 * @param project current project that references the given type from a jar file.
	 * @param type the type for which external annotations are sought
	 * @param monitor progress monitor to be passed through into file operations
	 * @return a file assumed (but not checked) to be in .eea format. The file may not "exist".
	 * 	Can be null if the given type is not contained in a jar file for which an external annotation path
	 *  has been defined in the context of the given project.
	 * @throws CoreException Signals a problem in accessing any of the relevant elements: the project, the type,
	 * the containing jar file and finally the sought annotation file.
	 */
	public static IFile getAnnotationFile(IJavaProject project, ITypeBinding type, IProgressMonitor monitor) throws CoreException {

		IType targetType = project.findType(type.getErasure().getQualifiedName());
		if (!targetType.exists())
			return null;

		String binaryTypeName = targetType.getFullyQualifiedName('$').replace('.', '/');

		IPackageFragmentRoot packageRoot = (IPackageFragmentRoot) targetType.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
		IClasspathEntry entry = packageRoot.getResolvedClasspathEntry();
		IPath annotationPath = ClasspathEntry.getExternalAnnotationPath(entry, project.getProject(), false);

		if (annotationPath == null)
			return null;

		IWorkspaceRoot workspaceRoot = project.getProject().getWorkspace().getRoot();

		if (annotationPath.segmentCount() > 1) {
			IFile annotationZip = workspaceRoot.getFile(annotationPath);
			if (annotationZip.exists())
				return null;
		}

		annotationPath = annotationPath.append(binaryTypeName).addFileExtension(ExternalAnnotationProvider.ANNOTATION_FILE_EXTENSION);
		return workspaceRoot.getFile(annotationPath);
	}

	/**
	 * Update the given external annotation file with details regarding annotations of one specific method or field.
	 * If the specified member already has external annotations, old and new annotations will be merged,
	 * with priorities controlled by the parameter 'mergeStrategy'.
	 * <p>
	 * This method is suitable for declaration annotations and type use annotations.
	 * </p>
	 * @param typeName binary name (slash separated) of the type being annotated
	 * @param file a file assumed to be in .eea format, will be created if it doesn't exist.
	 * @param selector selector of the method or field
	 * @param originalSignature unannotated signature of the member, used for identification
	 * @param annotatedSignature new signatures whose annotations should be superimposed on the member
	 * @param mergeStrategy controls how old and new signatures should be merged
	 * @param monitor progress monitor to be passed through into file operations, or null if no reporting is desired
	 * @throws CoreException if access to the file fails
	 * @throws IOException if reading file content fails
	 */
	public static void annotateMember(String typeName, IFile file, String selector, String originalSignature, String annotatedSignature,
										MergeStrategy mergeStrategy, IProgressMonitor monitor)
			throws CoreException, IOException
	{
		annotateMember(typeName, file, selector, originalSignature, annotatedSignature, POSITION_FULL_SIGNATURE, mergeStrategy, monitor);
	}

	/**
	 * Update the given external annotation file with details regarding annotations of a type parameter of the type itself.
	 * If the type already has external annotations, old and new annotations will be merged,
	 * with priorities controlled by the parameter 'mergeStrategy'.
	 * <p>
	 * This method is suitable only for type use annotations.
	 * </p>
	 * @param typeName binary name (slash separated) of the type being annotated
	 * @param file a file assumed to be in .eea format, will be created if it doesn't exist.
	 * @param originalSignature unannotated signature of all type parameters of the type
	 * @param annotatedTypeParameter signature of the new type parameter whose annotations should be superimposed on the method
	 * @param rank rank of the type parameter to be annotated
	 * @param mergeStrategy controls how old and new signatures should be merged
	 * @param monitor progress monitor to be passed through into file operations, or null if no reporting is desired
	 * @throws CoreException if access to the file fails
	 * @throws IOException if reading file content fails
	 * @throws IllegalArgumentException if the annotatedTypeParameter does not structurally match to originalSignature
	 * @since 3.19
	 */
	public static void annotateTypeTypeParameter(String typeName, IFile file, String originalSignature, String annotatedTypeParameter,
			int rank, MergeStrategy mergeStrategy, IProgressMonitor monitor)
			throws CoreException, IOException, IllegalArgumentException
	{
		annotateMember(typeName, file, null, originalSignature, annotatedTypeParameter, POSITION_TYPE_PARAMETER-rank, mergeStrategy, monitor);
	}

	/**
	 * Update the given external annotation file with details regarding annotations of a type parameter of a given method.
	 * If the specified method already has external annotations, old and new annotations will be merged,
	 * with priorities controlled by the parameter 'mergeStrategy'.
	 * <p>
	 * This method is suitable only for type use annotations.
	 * </p>
	 * @param typeName binary name (slash separated) of the type being annotated
	 * @param file a file assumed to be in .eea format, will be created if it doesn't exist.
	 * @param selector selector of the method
	 * @param originalSignature unannotated signature of the member, used for identification
	 * @param annotatedTypeParameter signature of the new type parameter whose annotations should be superimposed on the method
	 * @param rank rank of the type parameter to be annotated
	 * @param mergeStrategy controls how old and new signatures should be merged
	 * @param monitor progress monitor to be passed through into file operations, or null if no reporting is desired
	 * @throws CoreException if access to the file fails
	 * @throws IOException if reading file content fails
	 * @throws IllegalArgumentException if the annotatedTypeParameter does not structurally match to originalSignature
	 * @since 3.19
	 */
	public static void annotateMethodTypeParameter(String typeName, IFile file, String selector, String originalSignature, String annotatedTypeParameter,
			int rank, MergeStrategy mergeStrategy, IProgressMonitor monitor)
			throws CoreException, IOException, IllegalArgumentException
	{
		annotateMember(typeName, file, selector, originalSignature, annotatedTypeParameter, POSITION_TYPE_PARAMETER-rank, mergeStrategy, monitor);
	}

	/**
	 * Update the given external annotation file with details regarding annotations of the return type of a given method.
	 * If the specified method already has external annotations, old and new annotations will be merged,
	 * with priorities controlled by the parameter 'mergeStrategy'.
	 * <p>
	 * This method is suitable for declaration annotations and type use annotations.
	 * </p>
	 * @param typeName binary name (slash separated) of the type being annotated
	 * @param file a file assumed to be in .eea format, will be created if it doesn't exist.
	 * @param selector selector of the method
	 * @param originalSignature unannotated signature of the member, used for identification
	 * @param annotatedReturnType signature of the new return type whose annotations should be superimposed on the method
	 * @param mergeStrategy controls how old and new signatures should be merged
	 * @param monitor progress monitor to be passed through into file operations, or null if no reporting is desired
	 * @throws CoreException if access to the file fails
	 * @throws IOException if reading file content fails
	 * @throws IllegalArgumentException if the annotatedReturnType does not structurally match to originalSignature
	 */
	public static void annotateMethodReturnType(String typeName, IFile file, String selector, String originalSignature,
										String annotatedReturnType, MergeStrategy mergeStrategy, IProgressMonitor monitor)
			throws CoreException, IOException, IllegalArgumentException
	{
		annotateMember(typeName, file, selector, originalSignature, annotatedReturnType, POSITION_RETURN_TYPE, mergeStrategy, monitor);
	}

	/**
	 * Update the given external annotation file with details regarding annotations of a parameter type of a given method.
	 * If the specified method already has external annotations, old and new annotations will be merged,
	 * with priorities controlled by the parameter 'mergeStrategy'.
	 * <p>
	 * This method is suitable for declaration annotations and type use annotations.
	 * </p>
	 * @param typeName binary name (slash separated) of the type being annotated
	 * @param file a file assumed to be in .eea format, will be created if it doesn't exist.
	 * @param selector selector of the method
	 * @param originalSignature unannotated signature of the member, used for identification
	 * @param annotatedParameterType signature of the new parameter type whose annotations should be superimposed on the method
	 * @param paramIdx 0-based index of the parameter to which the annotation should be attached
	 * @param mergeStrategy controls how old and new signatures should be merged
	 * @param monitor progress monitor to be passed through into file operations, or null if no reporting is desired
	 * @throws CoreException if access to the file fails
	 * @throws IOException if reading file content fails
	 * @throws IllegalArgumentException if the annotatedParameterType does not structurally match to originalSignature
	 */
	public static void annotateMethodParameterType(String typeName, IFile file, String selector, String originalSignature,
										String annotatedParameterType, int paramIdx, MergeStrategy mergeStrategy, IProgressMonitor monitor)
			throws CoreException, IOException, IllegalArgumentException
	{
		annotateMember(typeName, file, selector, originalSignature, annotatedParameterType, paramIdx, mergeStrategy, monitor);
	}

	private static void annotateMember(String typeName, IFile file, String selector, String originalSignature, String annotatedSignature,
										int updatePosition, MergeStrategy mergeStrategy, IProgressMonitor monitor)
			throws CoreException, IOException, IllegalArgumentException
	{

		if (!file.exists()) {
			// assemble full annotatedSignature (don't bother merging since no previous signature exists):
			annotatedSignature = updateSignature(originalSignature, annotatedSignature, updatePosition, MergeStrategy.REPLACE_SIGNATURE);

			StringBuffer newContent= new StringBuffer();
			// header:
			newContent.append(ExternalAnnotationProvider.CLASS_PREFIX);
			newContent.append(typeName).append('\n');
			// new entry:
			if (selector != null) { // otherwise we are annotating a class type parameter
				newContent.append(selector).append('\n');
			}
			newContent.append(' ').append(originalSignature).append('\n');
			newContent.append(' ').append(annotatedSignature).append('\n');

			createNewFile(file, newContent.toString(), monitor);
		} else {
			BufferedReader reader = new BufferedReader(new InputStreamReader(file.getContents()));
			StringBuffer newContent = new StringBuffer();
			try {
				// type references get the previous signature from the existing type binding:
				String previousSignature = originalSignature;
				newContent.append(reader.readLine()).append('\n'); // skip class name
				String line;
				while ((line = reader.readLine()) != null) {
					if (selector == null) { // annotating a type parameter?
						if (line.trim().startsWith("<")) { //$NON-NLS-1$
							line = reader.readLine();
							assert line.trim().startsWith("<"); // reading old annotated type parameters //$NON-NLS-1$
							// bindings of type parameters (being declarations) don't include annotations in their signature,
							// so we need to preserve the previous signature from the .eea:
							previousSignature = line.trim();
							line = reader.readLine();
						}
						break;
					}
					if (line.isEmpty()) {
						newContent.append('\n');
						continue;
					}
					if (!Character.isJavaIdentifierStart(line.charAt(0)) && line.charAt(0) != '<') {
						newContent.append(line).append('\n');
						continue;
					}
					// compare selectors:
					int relation = line.compareTo(selector);
					if (relation > 0) { // past the insertion point
						break;
					}
					if (relation < 0) {
						newContent.append(line).append('\n');
						continue;
					}
					if (relation == 0) {
						StringBuffer pending = new StringBuffer(line).append('\n');
						pending.append(line = reader.readLine());
						if (line == null) {
							break; // found only the selector at EOF, append right here, ignoring 'pending'
						}
						// compare original signatures:
						relation = line.trim().compareTo(originalSignature);
						if (relation > 0) { // past the insertion point
							// add new entry (below)
							line = pending.toString(); // push back
							break;
						}
						newContent.append(pending).append('\n');
						if (relation < 0)
							continue;
						if (relation == 0) {
							// update existing entry:
							String annotationLine = reader.readLine();
							String nextLine = null;
							if (annotationLine == null || annotationLine.isEmpty() || !annotationLine.startsWith(" ")) { //$NON-NLS-1$
								nextLine = annotationLine; // push back, since not a signature line
								annotationLine = line; // no annotated line yet, use unannotated line instead
							}
							if (annotationLine.startsWith(" ")) { //$NON-NLS-1$
								switch (mergeStrategy) {
									case REPLACE_SIGNATURE:
										break; // unconditionally use annotatedSignature
									case OVERWRITE_ANNOTATIONS:
									case ADD_ANNOTATIONS:
										annotatedSignature = updateSignature(annotationLine.trim(), annotatedSignature, updatePosition, mergeStrategy);
										break;
									default:
										JavaCore.getJavaCore().getLog().log(new Status(IStatus.ERROR, JavaCore.PLUGIN_ID,
																				"Unexpected value for enum MergeStrategy")); //$NON-NLS-1$
								}
							}
							writeFile(file, newContent, annotatedSignature, nextLine, reader, monitor);
							return;
						}
					}
				}
				// add new entry:
				if (selector != null)
					newContent.append(selector).append('\n');
				newContent.append(' ').append(originalSignature).append('\n');
				annotatedSignature = updateSignature(previousSignature, annotatedSignature, updatePosition, mergeStrategy);
				writeFile(file, newContent, annotatedSignature, line, reader, monitor);
			} finally {
				reader.close();
			}
		}
	}

	private static String updateSignature(String originalSignature, String annotatedSignature, int updatePosition, MergeStrategy mergeStrategy) {
		StringBuffer buf = new StringBuffer();
		String signatureToReplace;
		String postfix = null;
		if (updatePosition <= POSITION_TYPE_PARAMETER) {
			// '<' [Annot] Identifier ClassBound {InterfaceBound} ... '>'
			assert originalSignature.charAt(0) ==  '<': "generic signature must start with '<'"; //$NON-NLS-1$
			SignatureWrapper wrapper = new SignatureWrapper(originalSignature.toCharArray(), true, true); // may already contain annotations
			wrapper.start = 1; // skip '<'
			// skip preceding type parameters:
			for (int i = 0; i < (-updatePosition+POSITION_TYPE_PARAMETER); i++) {
				wrapper.skipTypeParameter();
			}
			int start = wrapper.start;
			// copy entire prefix:
			buf.append(originalSignature, 0, start);
			// process selected type parameter:
			int end = wrapper.skipTypeParameter();
			signatureToReplace = originalSignature.substring(start, end);
			updateTypeParameter(buf, signatureToReplace.toCharArray(), annotatedSignature.toCharArray(), mergeStrategy);
			// extract postfix:
			postfix = originalSignature.substring(end, originalSignature.length());
		} else {
			switch (updatePosition) {
				case POSITION_FULL_SIGNATURE:
					signatureToReplace = originalSignature;
					break;
				case POSITION_RETURN_TYPE:
					assert originalSignature.charAt(0) == '(' || originalSignature.charAt(0) == '<': "signature must start with '(' or '<'"; //$NON-NLS-1$
					int close = originalSignature.indexOf(')');
					buf.append(originalSignature, 0, close+1);
					signatureToReplace = originalSignature.substring(close+1);
					break;
				default: // parameter
					SignatureWrapper wrapper = new SignatureWrapper(originalSignature.toCharArray(), true, true); // may already contain annotations
					wrapper.start = CharOperation.indexOf('(', wrapper.signature) + 1; // possibly skipping type parameters
					for (int i = 0; i < updatePosition; i++)
						wrapper.start = wrapper.skipAngleContents(wrapper.computeEnd()) + 1;
					int start = wrapper.start;
					int end = wrapper.skipAngleContents(wrapper.computeEnd());
					buf.append(originalSignature, 0, start);
					signatureToReplace = originalSignature.substring(start, end+1);
					postfix = originalSignature.substring(end+1, originalSignature.length());
			}
			updateType(buf, signatureToReplace.toCharArray(), annotatedSignature.toCharArray(), mergeStrategy);
		}
		if (postfix != null)
			buf.append(postfix);
		return buf.toString();
	}

	/**
	 * Insert that given annotation at the given position into the given signature.
	 * @param mergeStrategy if set to {@link MergeStrategy#ADD_ANNOTATIONS}, refuse to
	 *   overwrite any existing annotation in the specified location.
	 */
	private static String insertAt(String signature, int position, char annotation, MergeStrategy mergeStrategy) {
		StringBuilder result = new StringBuilder();
		result.append(signature, 0, position);
		result.append(annotation);
		char next = signature.charAt(position);
		switch (next) {
			case NULLABLE: case NONNULL:
				if (mergeStrategy == MergeStrategy.ADD_ANNOTATIONS)
					return signature; // refuse any change
				position++; // skip old annotation
		}
		result.append(signature, position, signature.length());
		return result.toString();
	}

	/**
	 * Update 'oldType' with annotations from 'newType' guided by 'mergeStrategy'.
	 * The result is written into 'buf' as we go.
	 */
	private static boolean updateType(StringBuffer buf, char[] oldType, char[] newType, MergeStrategy mergeStrategy) {
		if (mergeStrategy == MergeStrategy.REPLACE_SIGNATURE) {
			buf.append(newType);
			return false;
		}
		try {
			SignatureWrapper oWrap = new SignatureWrapper(oldType, true, true); // may already contain annotations
			SignatureWrapper nWrap = new SignatureWrapper(newType, true, true); // may already contain annotations
			if (match(buf, oWrap, nWrap, 'L', false)
				|| match(buf, oWrap, nWrap, 'T', false))
			{
				mergeAnnotation(buf, oWrap, nWrap, mergeStrategy);
				buf.append(oWrap.nextName());
				nWrap.nextName(); // skip
				if (match(buf, oWrap, nWrap, '<', false)) {
					do {
						int oStart = oWrap.start;
						int nStart = nWrap.start;
						oWrap.computeEnd();
						nWrap.computeEnd();
						if (updateType(buf, oWrap.getFrom(oStart), nWrap.getFrom(nStart), mergeStrategy))
							mergeAnnotation(buf, oWrap, nWrap, mergeStrategy);
					} while (!match(buf, oWrap, nWrap, '>', false));
				}
				match(buf, oWrap, nWrap, ';', true);
			} else if (match(buf, oWrap, nWrap, '[', false)) {
				mergeAnnotation(buf, oWrap, nWrap, mergeStrategy);
				updateType(buf, oWrap.tail(), nWrap.tail(), mergeStrategy);
			} else if (match(buf, oWrap, nWrap, '*', false)
					|| match(buf, oWrap, nWrap, '+', false)
					|| match(buf, oWrap, nWrap, '-', false))
			{
				return true; // annotation allowed after this (not included in oldType / newType)
			} else {
				buf.append(oldType);
			}
		} catch (ArrayIndexOutOfBoundsException aioobe) { // from several locations inside match() or mergeAnnotation().
			StringBuilder msg = new StringBuilder("Structural mismatch between ").append(oldType).append(" and ").append(newType); //$NON-NLS-1$ //$NON-NLS-2$
			throw new IllegalArgumentException(msg.toString(), aioobe);
		}
		return false;
	}
	/**
	 * similar to updateType() but for type parameters, syntax:
	 * 		[Annot] Identifier ClassBound {InterfaceBound}
	 */
	private static boolean updateTypeParameter(StringBuffer buf, char[] oldType, char[] newType, MergeStrategy mergeStrategy) {
		if (mergeStrategy == MergeStrategy.REPLACE_SIGNATURE) {
			buf.append(newType);
			return false;
		}
		try {
			SignatureWrapper oWrap = new SignatureWrapper(oldType, true, true); // may already contain annotations
			SignatureWrapper nWrap = new SignatureWrapper(newType, true, true); // may already contain annotations
			// [Annot]
			mergeAnnotation(buf, oWrap, nWrap, mergeStrategy);
			// Identifier:
			char[] oName = oWrap.wordUntil(':');
			char[] nName = nWrap.wordUntil(':');
			if (!CharOperation.equals(oName, nName)) {
				StringBuilder msg = new StringBuilder("Structural mismatch between type parameters ").append(oName).append(" and ").append(nName); //$NON-NLS-1$ //$NON-NLS-2$
				throw new IllegalArgumentException(msg.toString());
			}
			buf.append(oName);
			// one or more bounds each starting with ':'
			while (match(buf, oWrap, nWrap, ':', false)) {
				int oStart = oWrap.start;
				int nStart = nWrap.start;
				nWrap.skipAngleContents(nWrap.computeEnd());
				char[] nType = nWrap.getFrom(nStart);
				char[] oType;
				if (oWrap.charAtStart() == ':') {
					// old bound is empty!
					if (CharOperation.equals(nType, new char[]{':'})) {
						nWrap.start--; // unget second ':'
						continue; // both bounds empty
					}
					if (CharOperation.equals(nType, ConstantPool.JavaLangObjectSignature)) {
						// new: j.l.Object => skip
						continue;
					}
					// new is not exactly Ljava/lang/Object; (perhaps already annotated?) => replace old with j.l.Object for decoration:
					oType = ConstantPool.JavaLangObjectSignature;
				} else {
					oWrap.skipAngleContents(oWrap.computeEnd());
					oType = oWrap.getFrom(oStart);
				}
				if (updateType(buf, oType, nType, mergeStrategy))
					mergeAnnotation(buf, oWrap, nWrap, mergeStrategy);
				if (oWrap.atEnd() || nWrap.atEnd())
					return false;
			}
		} catch (ArrayIndexOutOfBoundsException aioobe) { // from several locations inside match() or mergeAnnotation().
			StringBuilder msg = new StringBuilder("Structural mismatch between ").append(oldType).append(" and ").append(newType); //$NON-NLS-1$ //$NON-NLS-2$
			throw new IllegalArgumentException(msg.toString(), aioobe);
		}
		return false;
	}
	/**
	 * Does the current char at both given signatures match the 'expected' char?
	 * If yes, print it into 'buf' and answer true.
	 * If no, if 'force' raise an exception, else quietly answer false without updating 'buf'.
	 */
	private static boolean match(StringBuffer buf, SignatureWrapper sig1, SignatureWrapper sig2, char expected, boolean force) {
		boolean match1 = sig1.signature[sig1.start] == expected;
		boolean match2 = sig2.signature[sig2.start] == expected;
		if (match1 != match2) {
			StringBuilder msg = new StringBuilder("Mismatching type structures ") //$NON-NLS-1$
									.append(sig1.signature).append(" vs ").append(sig2.signature); //$NON-NLS-1$
			throw new IllegalArgumentException(msg.toString());
		}
		if (match1) {
			buf.append(expected);
			sig1.start++;
			sig2.start++;
			return true;
		} else if (force) {
			throw new IllegalArgumentException("Expected char "+expected+" not found in "+new String(sig1.signature)); //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			return false;
		}
	}

	/**
	 * If a current char of 'oldS' and/or 'newS' represents a null annotation, insert it into 'buf' guided by 'mergeStrategy'.
	 * If the new char is NO_ANNOTATION and strategy is OVERWRITE_ANNOTATIONS, silently skip over any null annotations in 'oldS'.
	 */
	private static void mergeAnnotation(StringBuffer buf, SignatureWrapper oldS, SignatureWrapper newS, MergeStrategy mergeStrategy) {
		 // if atEnd use a char that's different from NULLABLE, NONNULL and NO_ANNOTATION:
		char oldAnn = !oldS.atEnd() ? oldS.signature[oldS.start] : '\0';
		char newAnn = !newS.atEnd() ? newS.signature[newS.start] : '\0';
		switch (mergeStrategy) {
			case ADD_ANNOTATIONS:
				switch (oldAnn) {
					case NULLABLE: case NONNULL:
						oldS.start++;
						buf.append(oldAnn); // old exists, so it remains
						switch (newAnn) { case NULLABLE: case NONNULL: newS.start++; } // just skip
						return;
				}
				//$FALL-THROUGH$
			case OVERWRITE_ANNOTATIONS:
				switch (newAnn) {
					case NULLABLE: case NONNULL:
						newS.start++;
						buf.append(newAnn); // new exists and is not suppressed by "ADD & old exists"
						switch (oldAnn) { case NULLABLE: case NONNULL: oldS.start++; } // just skip
						break;
					case NO_ANNOTATION:
						newS.start++; // don't insert
						switch (oldAnn) { case NULLABLE: case NONNULL: oldS.start++; } // just skip
						break;
					default:
						switch (oldAnn) {
							case NULLABLE: case NONNULL:
								oldS.start++;
								buf.append(oldAnn); // keep
						}
				}
				break;
			default:
				throw new IllegalArgumentException("Unexpected merge strategy"); // REPLACE_SIGNATURE does not reach this point, see initial check in updateType() //$NON-NLS-1$
		}
	}

	/**
	 * Write back the given annotationFile, with the following content:
	 * - head (assumed to include a member and its original signature
	 * - annotatedSignature
	 * - nextLines (optionally, may be null)
	 * - the still unconsumed content of tailReader
	 */
	private static void writeFile(IFile annotationFile, StringBuffer head, String annotatedSignature,
									String nextLines, BufferedReader tailReader, IProgressMonitor monitor)
			throws CoreException, IOException
	{
		head.append(' ').append(annotatedSignature).append('\n');
		if (nextLines != null)
			head.append(nextLines).append('\n');
		String line;
		while ((line = tailReader.readLine()) != null)
			head.append(line).append('\n');
		ByteArrayInputStream newContent = new ByteArrayInputStream(head.toString().getBytes("UTF-8")); //$NON-NLS-1$
		annotationFile.setContents(newContent, IResource.KEEP_HISTORY, monitor);
	}

	private static void createNewFile(IFile file, String newContent, IProgressMonitor monitor) throws CoreException {
		ensureExists(file.getParent(), monitor);

		try {
			file.create(new ByteArrayInputStream(newContent.getBytes("UTF-8")), false, monitor); //$NON-NLS-1$
		} catch (UnsupportedEncodingException e) {
			throw new CoreException(new Status(IStatus.ERROR, JavaCore.PLUGIN_ID, e.getMessage(), e));
		}
	}

	private static void ensureExists(IContainer container, IProgressMonitor monitor) throws CoreException {
		if (container.exists()) return;
		if (!(container instanceof IFolder)) throw new CoreException(new Status(IStatus.ERROR, JavaCore.PLUGIN_ID, "not a folder: "+container)); //$NON-NLS-1$
		IContainer parent= container.getParent();
		if (parent instanceof IFolder) {
			ensureExists(parent, monitor);
		}
		((IFolder) container).create(false, true, monitor);
	}

	/**
	 * Retrieve the annotated signature of a specified member as found in the given external annotation file, if any.
	 * @param typeName fully qualified slash-separated name of the type for which the file defines external annotations
	 * @param file a file assumed to be in .eea format, must not be null, but may not exist
	 * @param selector name of the member whose annotation we are looking for, or null when annotating the type's type parameters
	 * @param originalSignature the unannotated signature by which the member is identified
	 * @return the annotated signature as found in the file, or null.
	 */
	public static String getAnnotatedSignature(String typeName, IFile file, String selector, String originalSignature) {
		if (file.exists()) {
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getContents()))) {
				ExternalAnnotationProvider.assertClassHeader(reader.readLine(), typeName);
				while (true) {
					String line = reader.readLine();
					// selector:
					if (selector != null) {
						if (selector.equals(line)) {
							// original signature:
							line = reader.readLine();
							if (originalSignature.equals(ExternalAnnotationProvider.extractSignature(line))) {
								// annotated signature:
								return ExternalAnnotationProvider.extractSignature(reader.readLine());
							}
						}
					} else if (line != null && line.trim().startsWith("<")) { //$NON-NLS-1$
						// original signature:
						if (originalSignature.equals(ExternalAnnotationProvider.extractSignature(line))) {
							// annotated signature:
							return ExternalAnnotationProvider.extractSignature(reader.readLine());
						}
					}
					if (line == null)
						break;
				}
			} catch (IOException | CoreException e) {
				return null;
			}
		}
		return null;
	}

	/**
	 * Apply the specified changes on the given type.
	 * This method can be used as a dry run without modifying an annotation file.
	 *
	 * @param originalSignature the original type signature, may be annotated already
	 * @param annotatedType a type signature with additional annotations (incl. {@link #NO_ANNOTATION}).
	 * @param mergeStrategy controls how old and new signatures should be merged
	 * @return an array of length four: <ul>
	 * <li>prefix up-to the changed type</li>
	 * <li>original type</li>
	 * <li>changed type</li>
	 * <li>postfix after the changed type <em>(here: empty string)</em></li>
	 * </ul>
	 */
	public static String[] annotateType(String originalSignature, String annotatedType, MergeStrategy mergeStrategy)
	{
		String[] result = new String[4]; // prefix, orig, replacement, postfix
		StringBuffer buf;
		result[0] = ""; //$NON-NLS-1$
		buf = new StringBuffer();
		result[1] = originalSignature;
		updateType(buf, originalSignature.toCharArray(), annotatedType.toCharArray(), mergeStrategy);
		result[2] = buf.toString();
		result[3] = ""; //$NON-NLS-1$
		return result;
	}

	/**
	 * Apply the specified changes on the return type of the given signature.
	 * This method can be used as a dry run without modifying an annotation file.
	 *
	 * @param originalSignature the original full signature, may be annotated already
	 * @param annotatedType a type signature with additional annotations (incl. {@link #NO_ANNOTATION}).
	 * @param mergeStrategy controls how old and new signatures should be merged
	 * @return an array of length four: <ul>
	 * <li>prefix up-to the changed type</li>
	 * <li>original type</li>
	 * <li>changed type</li>
	 * <li>postfix after the changed type <em>(here: empty string)</em></li>
	 * </ul>
	 */
	public static String[] annotateReturnType(String originalSignature, String annotatedType, MergeStrategy mergeStrategy)
	{
		String[] result = new String[4]; // prefix, orig, replacement, postfix
		StringBuffer buf;
		assert originalSignature.charAt(0) == '(' || originalSignature.charAt(0) == '<': "signature must start with '(' or '<'"; //$NON-NLS-1$
		int close = originalSignature.indexOf(')');
		result[0] = originalSignature.substring(0, close+1);
		buf = new StringBuffer();
		result[1] = originalSignature.substring(close+1);
		updateType(buf, result[1].toCharArray(), annotatedType.toCharArray(), mergeStrategy);
		result[2] = buf.toString();
		result[3] = ""; //$NON-NLS-1$
		return result;
	}


	/**
	 * Apply the specified changes on a parameter within the given signature.
	 * This method can be used as a dry run without modifying an annotation file.
	 *
	 * @param originalSignature the original full signature, may be annotated already
	 * @param annotatedType a type signature with additional annotations (incl. {@link #NO_ANNOTATION}).
	 * @param paramIdx the index of a parameter to annotate
	 * @param mergeStrategy controls how old and new signatures should be merged
	 * @return an array of length four: <ul>
	 * <li>prefix up-to the changed type</li>
	 * <li>original type</li>
	 * <li>changed type</li>
	 * <li>postfix after the changed type</li>
	 * </ul>
	 */
	public static String[] annotateParameterType(String originalSignature, String annotatedType, int paramIdx, MergeStrategy mergeStrategy)
	{
		String[] result = new String[4]; // prefix, orig, replacement, postfix
		StringBuffer buf;
		SignatureWrapper wrapper = new SignatureWrapper(originalSignature.toCharArray(), true, true); // may already contain annotations
		wrapper.start = CharOperation.indexOf('(', wrapper.signature) + 1; // possibly skip type parameters
		for (int i = 0; i < paramIdx; i++)
			wrapper.start = wrapper.skipAngleContents(wrapper.computeEnd()) + 1;
		int start = wrapper.start;
		int end = wrapper.skipAngleContents(wrapper.computeEnd());
		result[0] = originalSignature.substring(0, start);
		buf = new StringBuffer();
		result[1] = originalSignature.substring(start, end+1);
		updateType(buf, result[1].toCharArray(), annotatedType.toCharArray(), mergeStrategy);
		result[2] = buf.toString();
		result[3] = originalSignature.substring(end+1, originalSignature.length());
		return result;
	}

	/**
	 * Apply the specified changes on a type parameter within the given signature.
	 * This method can be used as a dry run without modifying an annotation file.
	 *
	 * @param originalSignature the original full signature, may be annotated already
	 * @param annotatedType a type signature with additional annotations (incl. {@link #NO_ANNOTATION}).
	 * @param rank the index of a type parameter to annotate
	 * @param mergeStrategy controls how old and new signatures should be merged
	 * @return an array of length four: <ul>
	 * <li>prefix up-to the changed type</li>
	 * <li>original type</li>
	 * <li>changed type</li>
	 * <li>postfix after the changed type</li>
	 * </ul>
	 * @since 3.19
	 */
	public static String[] annotateTypeParameter(String originalSignature, String annotatedType, int rank, MergeStrategy mergeStrategy)
	{
		String[] result = new String[4]; // prefix, orig, replacement, postfix
		StringBuffer buf = new StringBuffer();
		SignatureWrapper wrapper = new SignatureWrapper(originalSignature.toCharArray(), true, true); // may already contain annotations
		wrapper.start = 1; // skip '<'
		// prefix:
		for (int i = 0; i < rank; i++) {
			wrapper.skipTypeParameter();
		}
		int start = wrapper.start;
		result[0] = originalSignature.substring(0, start);
		// orig:
		int end = wrapper.skipTypeParameter();
		result[1] = originalSignature.substring(start, end);
		// replacement:
		SignatureWrapper oWrap = new SignatureWrapper(result[1].toCharArray());
		SignatureWrapper nWrap = new SignatureWrapper(annotatedType.toCharArray());
		mergeAnnotation(buf, oWrap, nWrap, mergeStrategy);
		updateTypeParameter(buf, oWrap.tail(), nWrap.tail(), mergeStrategy);
		result[2] = buf.toString();
		// postfix:
		result[3] = originalSignature.substring(end, originalSignature.length());
		return result;
	}
}
