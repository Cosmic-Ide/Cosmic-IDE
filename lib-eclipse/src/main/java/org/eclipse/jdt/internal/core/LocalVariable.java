/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
 *								Bug 466308 - [hovering] Javadoc header for parameter is wrong with annotation-based null analysis
 *******************************************************************************/
package org.eclipse.jdt.internal.core;

import java.util.HashMap;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.ArrayInitializer;
import org.eclipse.jdt.internal.compiler.ast.ClassLiteralAccess;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.Literal;
import org.eclipse.jdt.internal.compiler.ast.NullLiteral;
import org.eclipse.jdt.internal.compiler.ast.OperatorIds;
import org.eclipse.jdt.internal.compiler.ast.QualifiedNameReference;
import org.eclipse.jdt.internal.compiler.ast.SingleNameReference;
import org.eclipse.jdt.internal.compiler.ast.UnaryExpression;
import org.eclipse.jdt.internal.compiler.lookup.ExtraCompilerModifiers;
import org.eclipse.jdt.internal.compiler.parser.RecoveryScanner;
import org.eclipse.jdt.internal.core.util.MementoTokenizer;
import org.eclipse.jdt.internal.core.util.Util;


public class LocalVariable extends SourceRefElement implements ILocalVariable {

	public static final ILocalVariable[] NO_LOCAL_VARIABLES = new ILocalVariable[0];

	String name;
	public int declarationSourceStart, declarationSourceEnd;
	public int nameStart, nameEnd;
	String typeSignature;
	public IAnnotation[] annotations;
	private int flags;
	private boolean isParameter;
	public IAnnotation[][] annotationsOnDimensions;

	public LocalVariable(
			JavaElement parent,
			String name,
			int declarationSourceStart,
			int declarationSourceEnd,
			int nameStart,
			int nameEnd,
			String typeSignature,
			org.eclipse.jdt.internal.compiler.ast.Annotation[] astAnnotations,
			int flags,
			boolean isParameter) {

		super(parent);
		this.name = name;
		this.declarationSourceStart = declarationSourceStart;
		this.declarationSourceEnd = declarationSourceEnd;
		this.nameStart = nameStart;
		this.nameEnd = nameEnd;
		this.typeSignature = typeSignature;
		this.annotations = getAnnotations(astAnnotations);
		this.flags = flags;
		this.isParameter = isParameter;
	}
	public LocalVariable(
			JavaElement parent,
			String name,
			int declarationSourceStart,
			int declarationSourceEnd,
			int nameStart,
			int nameEnd,
			String typeSignature,
			org.eclipse.jdt.internal.compiler.ast.Annotation[] astAnnotations,
			int flags,
			boolean isParameter,
		org.eclipse.jdt.internal.compiler.ast.Annotation[][] astAnnotationsOnDimensions) {

		this(parent, name, declarationSourceStart, declarationSourceEnd, nameStart,
				nameEnd, typeSignature, astAnnotations, flags, isParameter);

		int noOfDimensions = astAnnotationsOnDimensions == null ? 0 : astAnnotationsOnDimensions.length;
		if (noOfDimensions > 0) {
			this.annotationsOnDimensions = new IAnnotation[noOfDimensions][];
			for (int i = 0; i < noOfDimensions; ++i) {
				this.annotationsOnDimensions[i] = getAnnotations(astAnnotationsOnDimensions[i]);
			}
		}
	}

	@Override
	protected void closing(Object info) {
		// a local variable has no info
	}

	@Override
	protected Object createElementInfo() {
		// a local variable has no info
		return null;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof LocalVariable)) return false;
		LocalVariable other = (LocalVariable)o;
		return
			this.declarationSourceStart == other.declarationSourceStart
			&& this.declarationSourceEnd == other.declarationSourceEnd
			&& this.nameStart == other.nameStart
			&& this.nameEnd == other.nameEnd
			&& super.equals(o);
	}

	@Override
	public boolean exists() {
		return this.getParent().exists(); // see https://bugs.eclipse.org/bugs/show_bug.cgi?id=46192
	}

	@Override
	protected void generateInfos(Object info, HashMap newElements, IProgressMonitor pm) {
		// a local variable has no info
	}

	@Override
	public IAnnotation getAnnotation(String annotationName) {
		for (int i = 0, length = this.annotations.length; i < length; i++) {
			IAnnotation annotation = this.annotations[i];
			if (annotation.getElementName().equals(annotationName))
				return annotation;
		}
		return super.getAnnotation(annotationName);
	}

	@Override
	public IAnnotation[] getAnnotations() throws JavaModelException {
		return this.annotations;
	}

	private IAnnotation[] getAnnotations(org.eclipse.jdt.internal.compiler.ast.Annotation[] astAnnotations) {
		int length;
		if (astAnnotations == null || (length = astAnnotations.length) == 0)
			return Annotation.NO_ANNOTATIONS;
		IAnnotation[] result = new IAnnotation[length];
		for (int i = 0; i < length; i++) {
			result[i] = getAnnotation(astAnnotations[i], this);
		}
		return result;
	}

	private IAnnotation getAnnotation(final org.eclipse.jdt.internal.compiler.ast.Annotation annotation, JavaElement parentElement) {
		final int typeStart = annotation.type.sourceStart();
		final int typeEnd = annotation.type.sourceEnd();
		final int sourceStart = annotation.sourceStart();
		final int sourceEnd = annotation.declarationSourceEnd;
		class LocalVarAnnotation extends Annotation {
			IMemberValuePair[] memberValuePairs;
			public LocalVarAnnotation(JavaElement localVar, String elementName) {
				super(localVar, elementName);
			}
			@Override
			public IMemberValuePair[] getMemberValuePairs() throws JavaModelException {
				return this.memberValuePairs;
			}
			@Override
			public ISourceRange getNameRange() throws JavaModelException {
				return new SourceRange(typeStart, typeEnd - typeStart + 1);
			}
			@Override
			public ISourceRange getSourceRange() throws JavaModelException {
				return new SourceRange(sourceStart, sourceEnd - sourceStart + 1);
			}
			@Override
			public boolean exists() {
				return this.getParent().exists();
			}
		}
		String annotationName = new String(CharOperation.concatWith(annotation.type.getTypeName(), '.'));
		LocalVarAnnotation localVarAnnotation = new LocalVarAnnotation(parentElement, annotationName);
		org.eclipse.jdt.internal.compiler.ast.MemberValuePair[] astMemberValuePairs = annotation.memberValuePairs();
		int length;
		IMemberValuePair[] memberValuePairs;
		if (astMemberValuePairs == null || (length = astMemberValuePairs.length) == 0) {
			memberValuePairs = Annotation.NO_MEMBER_VALUE_PAIRS;
		} else {
			memberValuePairs = new IMemberValuePair[length];
			for (int i = 0; i < length; i++) {
				org.eclipse.jdt.internal.compiler.ast.MemberValuePair astMemberValuePair = astMemberValuePairs[i];
				MemberValuePair memberValuePair = new MemberValuePair(new String(astMemberValuePair.name));
				memberValuePair.value = getAnnotationMemberValue(memberValuePair, astMemberValuePair.value, localVarAnnotation);
				memberValuePairs[i] = memberValuePair;
			}
		}
		localVarAnnotation.memberValuePairs = memberValuePairs;
		return localVarAnnotation;
	}

	/*
	 * Creates the value wrapper from the given expression, and sets the valueKind on the given memberValuePair
	 */
	private Object getAnnotationMemberValue(MemberValuePair memberValuePair, Expression expression, JavaElement parentElement) {
		if (expression instanceof NullLiteral) {
			return null;
		} else if (expression instanceof Literal) {
			((Literal) expression).computeConstant();
			return Util.getAnnotationMemberValue(memberValuePair, expression.constant);
		} else if (expression instanceof org.eclipse.jdt.internal.compiler.ast.Annotation) {
			memberValuePair.valueKind = IMemberValuePair.K_ANNOTATION;
			return getAnnotation((org.eclipse.jdt.internal.compiler.ast.Annotation) expression, parentElement);
		} else if (expression instanceof ClassLiteralAccess) {
			ClassLiteralAccess classLiteral = (ClassLiteralAccess) expression;
			char[] typeName = CharOperation.concatWith(classLiteral.type.getTypeName(), '.');
			memberValuePair.valueKind = IMemberValuePair.K_CLASS;
			return new String(typeName);
		} else if (expression instanceof QualifiedNameReference) {
			char[] qualifiedName = CharOperation.concatWith(((QualifiedNameReference) expression).tokens, '.');
			memberValuePair.valueKind = IMemberValuePair.K_QUALIFIED_NAME;
			return new String(qualifiedName);
		} else if (expression instanceof SingleNameReference) {
			char[] simpleName = ((SingleNameReference) expression).token;
			if (simpleName == RecoveryScanner.FAKE_IDENTIFIER) {
				memberValuePair.valueKind = IMemberValuePair.K_UNKNOWN;
				return null;
			}
			memberValuePair.valueKind = IMemberValuePair.K_SIMPLE_NAME;
			return new String(simpleName);
		} else if (expression instanceof ArrayInitializer) {
			memberValuePair.valueKind = -1; // modified below by the first call to getMemberValue(...)
			Expression[] expressions = ((ArrayInitializer) expression).expressions;
			int length = expressions == null ? 0 : expressions.length;
			Object[] values = new Object[length];
			for (int i = 0; i < length; i++) {
				int previousValueKind = memberValuePair.valueKind;
				Object value = getAnnotationMemberValue(memberValuePair, expressions[i], parentElement);
				if (previousValueKind != -1 && memberValuePair.valueKind != previousValueKind) {
					// values are heterogeneous, value kind is thus unknown
					memberValuePair.valueKind = IMemberValuePair.K_UNKNOWN;
				}
				values[i] = value;
			}
			if (memberValuePair.valueKind == -1)
				memberValuePair.valueKind = IMemberValuePair.K_UNKNOWN;
			return values;
		} else if (expression instanceof UnaryExpression) {			//to deal with negative numerals (see bug - 248312)
			UnaryExpression unaryExpression = (UnaryExpression) expression;
			if ((unaryExpression.bits & ASTNode.OperatorMASK) >> ASTNode.OperatorSHIFT == OperatorIds.MINUS) {
				if (unaryExpression.expression instanceof Literal) {
					Literal subExpression = (Literal) unaryExpression.expression;
					subExpression.computeConstant();
					return Util.getNegativeAnnotationMemberValue(memberValuePair, subExpression.constant);
				}
			}
			memberValuePair.valueKind = IMemberValuePair.K_UNKNOWN;
			return null;
		} else {
			memberValuePair.valueKind = IMemberValuePair.K_UNKNOWN;
			return null;
		}
	}

	@Override
	public IJavaElement getHandleFromMemento(String token, MementoTokenizer memento, WorkingCopyOwner owner) {
		switch (token.charAt(0)) {
			case JEM_COUNT:
				return getHandleUpdatingCountFromMemento(memento, owner);
		}
		return this;
	}

	@Override
	protected void getHandleMemento(StringBuffer buff) {
		getHandleMemento(buff, true);
	}

	protected void getHandleMemento(StringBuffer buff, boolean memoizeParent) {
		if (memoizeParent)
			getParent().getHandleMemento(buff);
		buff.append(getHandleMementoDelimiter());
		buff.append(this.name);
		buff.append(JEM_COUNT);
		buff.append(this.declarationSourceStart);
		buff.append(JEM_COUNT);
		buff.append(this.declarationSourceEnd);
		buff.append(JEM_COUNT);
		buff.append(this.nameStart);
		buff.append(JEM_COUNT);
		buff.append(this.nameEnd);
		buff.append(JEM_COUNT);
		escapeMementoName(buff, this.typeSignature);
		buff.append(JEM_COUNT);
		buff.append(this.flags);
		buff.append(JEM_COUNT);
		buff.append(this.isParameter);
		if (this.occurrenceCount > 1) {
			buff.append(JEM_COUNT);
			buff.append(this.occurrenceCount);
		}
	}

	@Override
	protected char getHandleMementoDelimiter() {
		return JavaElement.JEM_LOCALVARIABLE;
	}

	@Override
	public IResource getCorrespondingResource() {
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @since 3.7
	 */
	@Override
	public IMember getDeclaringMember() {
		return (IMember) this.getParent();
	}

	@Override
	public String getElementName() {
		return this.name;
	}

	@Override
	public int getElementType() {
		return LOCAL_VARIABLE;
	}

	/**
	 * {@inheritDoc}
	 * @since 3.7
	 */
	@Override
	public int getFlags() {
		if (this.flags == -1) {
			SourceMapper mapper= getSourceMapper();
			if (mapper != null) {
				try {
					// ensure the class file's buffer is open so that source ranges are computed
					IClassFile classFile = getClassFile();
					if (classFile != null) {
						classFile.getBuffer();
						return mapper.getFlags(this);
					}
				} catch(JavaModelException e) {
					// ignore
				}
			}
			return 0;
		}
		return this.flags & ExtraCompilerModifiers.AccJustFlag;
	}

	/**
	 * @see IMember#getClassFile()
	 */
	@Override
	public ClassFile getClassFile() {
		JavaElement element = getParent();
		while (element instanceof Member) {
			element= element.getParent();
		}
		if (element instanceof ClassFile) {
			return (ClassFile) element;
		}
		return null;
	}
	/**
	 * {@inheritDoc}
	 * @since 3.7
	 */
	@Override
	public ISourceRange getNameRange() {
		if (this.nameEnd == -1) {
			SourceMapper mapper= getSourceMapper();
			if (mapper != null) {
				try {
					// ensure the class file's buffer is open so that source ranges are computed
					IClassFile classFile = getClassFile();
					if (classFile != null) {
						classFile.getBuffer();
						return mapper.getNameRange(this);
					}
				} catch(JavaModelException e) {
					// ignore
				}
			}
			return SourceMapper.UNKNOWN_RANGE;
		}
		return new SourceRange(this.nameStart, this.nameEnd-this.nameStart+1);
	}

	@Override
	public IPath getPath() {
		return this.getParent().getPath();
	}

	@Override
	public IResource resource() {
		return this.getParent().resource();
	}

	/**
	 * @see ISourceReference
	 */
	@Override
	public String getSource() throws JavaModelException {
		IOpenable openable = this.getParent().getOpenableParent();
		IBuffer buffer = openable.getBuffer();
		if (buffer == null) {
			return null;
		}
		ISourceRange range = getSourceRange();
		int offset = range.getOffset();
		int length = range.getLength();
		if (offset == -1 || length == 0 ) {
			return null;
		}
		try {
			return buffer.getText(offset, length);
		} catch(RuntimeException e) {
			return null;
		}
	}

	/**
	 * {@inheritDoc}
	 * @since 3.7
	 */
	@Override
	public ISourceRange getSourceRange() throws JavaModelException {
		if (this.declarationSourceEnd == -1) {
			SourceMapper mapper= getSourceMapper();
			if (mapper != null) {
				// ensure the class file's buffer is open so that source ranges are computed
				IClassFile classFile = getClassFile();
				if (classFile != null) {
					classFile.getBuffer();
					return mapper.getSourceRange(this);
				}
			}
			return SourceMapper.UNKNOWN_RANGE;
		}
		return new SourceRange(this.declarationSourceStart, this.declarationSourceEnd-this.declarationSourceStart+1);
	}

	/**
	 * {@inheritDoc}
	 * @since 3.7
	 */
	@Override
	public ITypeRoot getTypeRoot() {
		return this.getDeclaringMember().getTypeRoot();
	}

	@Override
	public String getTypeSignature() {
		return this.typeSignature;
	}

	@Override
	public IResource getUnderlyingResource() throws JavaModelException {
		return this.getParent().getUnderlyingResource();
	}

	@Override
	public int hashCode() {
		return Util.combineHashCodes(this.getParent().hashCode(), this.nameStart);
	}

	/**
	 * {@inheritDoc}
	 * @since 3.7
	 */
	@Override
	public boolean isParameter() {
		return this.isParameter;
	}

	@Override
	public boolean isStructureKnown() throws JavaModelException {
		return true;
	}

	/**
	 * @see org.eclipse.jdt.internal.compiler.lookup.Binding#computeUniqueKey()
	 */
	public String getKey(boolean forceOpen) throws JavaModelException {
		if (this.getParent().getElementType() == IJavaElement.METHOD) {
			StringBuilder buf = new StringBuilder();
			if (this.getParent() instanceof BinaryMethod)
				buf.append(((BinaryMethod) this.getParent()).getKey(forceOpen));
			else
				buf.append(((IMethod)this.getParent()).getKey());
			buf.append('#');
			buf.append(this.name);
			if (this.isParameter) {
				ILocalVariable[] parameters = ((IMethod) this.getParent()).getParameters();
				for (int i = 0; i < parameters.length; i++) {
					if (this.equals(parameters[i])) {
						buf.append("#0#").append(i); // always first occurrence, followed by parameter rank //$NON-NLS-1$
						break;
					}
				}
			}
			return buf.toString();
		}
		return null;
	}

	@Override
	protected void toStringInfo(int tab, StringBuffer buffer, Object info, boolean showResolvedInfo) {
		buffer.append(tabString(tab));
		if (info != NO_INFO) {
			buffer.append(Signature.toString(getTypeSignature()));
			buffer.append(" "); //$NON-NLS-1$
		}
		toStringName(buffer);
	}

}
