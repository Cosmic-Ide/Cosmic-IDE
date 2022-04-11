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
 *     Stephan Herrmann - Contribution for
 *								Bug 186342 - [compiler][null] Using annotations for null checking
 *								Bug 440474 - [null] textual encoding of external null annotations
 *     Andy Clement (GoPivotal, Inc) aclement@gopivotal.com - Contributions for
 *         Bug 407191 - [1.8] Binary access support for type annotations
 *******************************************************************************/
package org.eclipse.jdt.internal.eval;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.env.IBinaryAnnotation;
import org.eclipse.jdt.internal.compiler.env.IBinaryField;
import org.eclipse.jdt.internal.compiler.env.IBinaryMethod;
import org.eclipse.jdt.internal.compiler.env.IBinaryNestedType;
import org.eclipse.jdt.internal.compiler.env.IBinaryType;
import org.eclipse.jdt.internal.compiler.env.IBinaryTypeAnnotation;
import org.eclipse.jdt.internal.compiler.env.IRecordComponent;
import org.eclipse.jdt.internal.compiler.env.ITypeAnnotationWalker;
import org.eclipse.jdt.internal.compiler.lookup.LookupEnvironment;
import org.eclipse.jdt.internal.compiler.lookup.BinaryTypeBinding.ExternalAnnotationStatus;
import org.eclipse.jdt.internal.core.util.Util;

/**
 * The skeleton of the class 'org.eclipse.jdt.internal.eval.target.CodeSnippet'
 * used at compile time. Note that the method run() is declared to
 * throw Throwable so that the user can write a code snipet that
 * throws checked exceptio without having to catch those.
 */
public class CodeSnippetSkeleton implements IBinaryType, EvaluationConstants {
	public static class BinaryMethodSkeleton implements IBinaryMethod {
		char[][] exceptionTypeNames;
		char[] methodDescriptor;
		char[] selector;
		boolean isConstructor;

		public BinaryMethodSkeleton(char[] selector, char[] methodDescriptor, char[][] exceptionTypeNames, boolean isConstructor) {
			this.selector = selector;
			this.methodDescriptor = methodDescriptor;
			this.exceptionTypeNames = exceptionTypeNames;
			this.isConstructor = isConstructor;
		}
		@Override
		public IBinaryAnnotation[] getAnnotations() {
			return null;
		}
		@Override
		public char[][] getArgumentNames() {
			return null;
		}
		@Override
		public Object getDefaultValue() {
			return null;
		}
		@Override
		public char[][] getExceptionTypeNames() {
			return this.exceptionTypeNames;
		}
		@Override
		public char[] getGenericSignature() {
			return null;
		}
		@Override
		public char[] getMethodDescriptor() {
			return this.methodDescriptor;
		}
		@Override
		public int getModifiers() {
			return ClassFileConstants.AccPublic;
		}
		@Override
		public IBinaryAnnotation[] getParameterAnnotations(int index, char[] classFileName) {
			return null;
		}
		@Override
		public int getAnnotatedParametersCount() {
			return 0;
		}
		@Override
		public IBinaryTypeAnnotation[] getTypeAnnotations() {
			return null;
		}
		@Override
		public char[] getSelector() {
			return this.selector;
		}
		@Override
		public long getTagBits() {
			return 0;
		}
		@Override
		public boolean isClinit() {
			return false;
		}
		@Override
		public boolean isConstructor() {
			return this.isConstructor;
		}
}

	IBinaryMethod[] methods = new IBinaryMethod[] {
		new BinaryMethodSkeleton(
			"<init>".toCharArray(), //$NON-NLS-1$
			"()V".toCharArray(), //$NON-NLS-1$
			new char[][] {},
			true
		),
		new BinaryMethodSkeleton(
			"run".toCharArray(), //$NON-NLS-1$
			"()V".toCharArray(), //$NON-NLS-1$
			new char[][] {"java/lang/Throwable".toCharArray()}, //$NON-NLS-1$
			false
		),
		new BinaryMethodSkeleton(
			"setResult".toCharArray(), //$NON-NLS-1$
			"(Ljava/lang/Object;Ljava/lang/Class;)V".toCharArray(), //$NON-NLS-1$
			new char[][] {},
			false
		)
	};

/**
 * CodeSnippetSkeleton constructor comment.
 */
public CodeSnippetSkeleton() {
	super();
}
@Override
public IBinaryAnnotation[] getAnnotations() {
	return null;
}
@Override
public IBinaryTypeAnnotation[] getTypeAnnotations() {
	return null;
}
@Override
public char[] getEnclosingMethod() {
	return null;
}
@Override
public char[] getEnclosingTypeName() {
	return null;
}
@Override
public IBinaryField[] getFields() {
	return null;
}
/**
 * @see org.eclipse.jdt.internal.compiler.env.IDependent#getFileName()
 */
@Override
public char[] getFileName() {
	return CharOperation.concat(CODE_SNIPPET_NAME, Util.defaultJavaExtension().toCharArray());
}
@Override
public char[] getGenericSignature() {
	return null;
}
@Override
public char[][] getInterfaceNames() {
	return null;
}
public String getJavadocContents() {
	return null;
}
public String getJavadocContents(IProgressMonitor monitor, String defaultEncoding) throws JavaModelException {
	return null;
}
@Override
public IBinaryNestedType[] getMemberTypes() {
	return null;
}
@Override
public IBinaryMethod[] getMethods() {
	return this.methods;
}
@Override
public int getModifiers() {
	return ClassFileConstants.AccPublic;
}
@Override
public char[][][] getMissingTypeNames() {
	return null;
}
@Override
public char[] getName() {
	return CODE_SNIPPET_NAME;
}
@Override
public char[] getSourceName() {
	return ROOT_CLASS_NAME;
}
@Override
public char[] getSuperclassName() {
	return null;
}
@Override
public long getTagBits() {
	return 0;
}
public String getURLContents(String docUrlValue, String defaultEncoding) {
	return null;
}
@Override
public boolean isAnonymous() {
	return false;
}
@Override
public boolean isBinaryType() {
	return true;
}
@Override
public boolean isLocal() {
	return false;
}
@Override
public boolean isRecord() {
	return false;
}
@Override
public boolean isMember() {
	return false;
}
@Override
public char[] sourceFileName() {
	return null;
}
@Override
public ITypeAnnotationWalker enrichWithExternalAnnotationsFor(ITypeAnnotationWalker walker, Object member, LookupEnvironment environment) {
	return walker;
}
@Override
public char[] getModule() {
	// TODO Java 9 Auto-generated method stub
	return null;
}
@Override
public ExternalAnnotationStatus getExternalAnnotationStatus() {
	return ExternalAnnotationStatus.NOT_EEA_CONFIGURED;
}
@Override
public IRecordComponent[] getRecordComponents() {
	return null;
}
}
