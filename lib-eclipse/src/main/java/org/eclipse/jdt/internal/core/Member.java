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
 *******************************************************************************/
package org.eclipse.jdt.internal.core;

import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.eclipse.jdt.internal.compiler.impl.Constant;
import org.eclipse.jdt.internal.compiler.lookup.TypeIds;
import org.eclipse.jdt.internal.core.util.MementoTokenizer;

/**
 * @see IMember
 */

@SuppressWarnings({"rawtypes", "unchecked"})
public abstract class Member extends SourceRefElement implements IMember {

protected Member(JavaElement parent) {
	super(parent);
}
protected static boolean areSimilarMethods(
	String name1, String[] params1,
	String name2, String[] params2,
	String[] simpleNames1) {

	if (name1.equals(name2)) {
		int params1Length = params1.length;
		if (params1Length == params2.length) {
			for (int i = 0; i < params1Length; i++) {
				String simpleName1 =
					simpleNames1 == null ?
						Signature.getSimpleName(Signature.toString(Signature.getTypeErasure(params1[i]))) :
						simpleNames1[i];
				String simpleName2 = Signature.getSimpleName(Signature.toString(Signature.getTypeErasure(params2[i])));
				if (!simpleName1.equals(simpleName2)) {
					return false;
				}
			}
			return true;
		}
	}
	return false;
}
/**
 * Converts a field constant from the compiler's representation
 * to the Java Model constant representation (Number or String).
 */
protected static Object convertConstant(Constant constant) {
	if (constant == null)
		return null;
	if (constant == Constant.NotAConstant) {
		return null;
	}
	switch (constant.typeID()) {
		case TypeIds.T_boolean :
			return constant.booleanValue() ? Boolean.TRUE : Boolean.FALSE;
		case TypeIds.T_byte :
			return Byte.valueOf(constant.byteValue());
		case TypeIds.T_char :
			return Character.valueOf(constant.charValue());
		case TypeIds.T_double :
			return Double.valueOf(constant.doubleValue());
		case TypeIds.T_float :
			return Float.valueOf(constant.floatValue());
		case TypeIds.T_int :
			return Integer.valueOf(constant.intValue());
		case TypeIds.T_long :
			return Long.valueOf(constant.longValue());
		case TypeIds.T_short :
			return Short.valueOf(constant.shortValue());
		case TypeIds.T_JavaLangString :
			return constant.stringValue();
		default :
			return null;
	}
}
/*
 * Helper method for SourceType.findMethods and BinaryType.findMethods
 */
public static IMethod[] findMethods(IMethod method, IMethod[] methods) {
	String elementName = method.getElementName();
	String[] parameters = method.getParameterTypes();
	int paramLength = parameters.length;
	String[] simpleNames = new String[paramLength];
	for (int i = 0; i < paramLength; i++) {
		String erasure = Signature.getTypeErasure(parameters[i]);
		simpleNames[i] = Signature.getSimpleName(Signature.toString(erasure));
	}
	ArrayList list = new ArrayList();
	for (int i = 0, length = methods.length; i < length; i++) {
		IMethod existingMethod = methods[i];
		if (areSimilarMethods(
				elementName,
				parameters,
				existingMethod.getElementName(),
				existingMethod.getParameterTypes(),
				simpleNames)) {
			list.add(existingMethod);
		}
	}
	int size = list.size();
	if (size == 0) {
		return null;
	} else {
		IMethod[] result = new IMethod[size];
		list.toArray(result);
		return result;
	}
}
@Override
public String[] getCategories() throws JavaModelException {
	IType type = (IType) getAncestor(IJavaElement.TYPE);
	if (type == null) return CharOperation.NO_STRINGS;
	if (type.isBinary()) {
		return CharOperation.NO_STRINGS;
	} else {
		SourceTypeElementInfo info = (SourceTypeElementInfo) ((SourceType) type).getElementInfo();
		HashMap map = info.getCategories();
		if (map == null) return CharOperation.NO_STRINGS;
		String[] categories = (String[]) map.get(this);
		if (categories == null) return CharOperation.NO_STRINGS;
		return categories;
	}
}
/**
 * @see IMember
 */
@Override
public IClassFile getClassFile() {
	JavaElement element = getParent();
	while (element instanceof Member) {
		element= element.getParent();
	}
	if (element instanceof IClassFile) {
		return (IClassFile) element;
	}
	return null;
}
/**
 * @see IMember
 */
@Override
public IType getDeclaringType() {
	JavaElement parentElement = getParent();
	if (parentElement.getElementType() == TYPE) {
		return (IType) parentElement;
	}
	return null;
}
/**
 * @see IMember
 */
@Override
public int getFlags() throws JavaModelException {
	MemberElementInfo info = (MemberElementInfo) getElementInfo();
	return info.getModifiers();
}
/*
 * @see JavaElement
 */
@Override
public IJavaElement getHandleFromMemento(String token, MementoTokenizer memento, WorkingCopyOwner workingCopyOwner) {
	switch (token.charAt(0)) {
		case JEM_COUNT:
			return getHandleUpdatingCountFromMemento(memento, workingCopyOwner);
		case JEM_LAMBDA_EXPRESSION:
			if (!memento.hasMoreTokens() || memento.nextToken() != MementoTokenizer.STRING)
				return this;
			if (!memento.hasMoreTokens()) return this;
			String interphase = memento.nextToken();
			if (!memento.hasMoreTokens() || memento.nextToken() != MementoTokenizer.COUNT)
				return this;
			int sourceStart = Integer.parseInt(memento.nextToken());
			if (!memento.hasMoreTokens() || memento.nextToken() != MementoTokenizer.COUNT)
				return this;
			int sourceEnd = Integer.parseInt(memento.nextToken());
			if (!memento.hasMoreTokens() || memento.nextToken() != MementoTokenizer.COUNT)
				return this;
			int arrowPosition = Integer.parseInt(memento.nextToken());
			LambdaExpression expression = LambdaFactory.createLambdaExpression(this, interphase, sourceStart, sourceEnd, arrowPosition);
			if (!memento.hasMoreTokens() || (token = memento.nextToken()) != MementoTokenizer.LAMBDA_METHOD)
				return expression;
			return expression.getHandleFromMemento(token, memento, workingCopyOwner);
		case JEM_TYPE:
			String typeName;
			if (memento.hasMoreTokens()) {
				typeName = memento.nextToken();
				char firstChar = typeName.charAt(0);
				if (firstChar == JEM_FIELD || firstChar == JEM_INITIALIZER || firstChar == JEM_METHOD || firstChar == JEM_TYPE || firstChar == JEM_COUNT) {
					token = typeName;
					typeName = ""; //$NON-NLS-1$
				} else {
					token = null;
				}
			} else {
				typeName = ""; //$NON-NLS-1$
				token = null;
			}
			JavaElement type = (JavaElement)getType(typeName, 1);
			if (token == null) {
				return type.getHandleFromMemento(memento, workingCopyOwner);
			} else {
				return type.getHandleFromMemento(token, memento, workingCopyOwner);
			}
		case JEM_LOCALVARIABLE:
			if (!memento.hasMoreTokens()) return this;
			String varName = memento.nextToken();
			if (!memento.hasMoreTokens()) return this;
			memento.nextToken(); // JEM_COUNT
			if (!memento.hasMoreTokens()) return this;
			int declarationStart = Integer.parseInt(memento.nextToken());
			if (!memento.hasMoreTokens()) return this;
			memento.nextToken(); // JEM_COUNT
			if (!memento.hasMoreTokens()) return this;
			int declarationEnd = Integer.parseInt(memento.nextToken());
			if (!memento.hasMoreTokens()) return this;
			memento.nextToken(); // JEM_COUNT
			if (!memento.hasMoreTokens()) return this;
			int nameStart = Integer.parseInt(memento.nextToken());
			if (!memento.hasMoreTokens()) return this;
			memento.nextToken(); // JEM_COUNT
			if (!memento.hasMoreTokens()) return this;
			int nameEnd = Integer.parseInt(memento.nextToken());
			if (!memento.hasMoreTokens()) return this;
			memento.nextToken(); // JEM_COUNT
			if (!memento.hasMoreTokens()) return this;
			String typeSignature = memento.nextToken();
			memento.nextToken(); // JEM_COUNT
			if (!memento.hasMoreTokens()) return this;
			int flags = Integer.parseInt(memento.nextToken());
			memento.nextToken(); // JEM_COUNT
			if (!memento.hasMoreTokens()) return this;
			boolean isParameter = Boolean.parseBoolean(memento.nextToken());
			return new LocalVariable(this, varName, declarationStart, declarationEnd, nameStart, nameEnd, typeSignature, null, flags, isParameter);
		case JEM_TYPE_PARAMETER:
			if (!memento.hasMoreTokens()) return this;
			String typeParameterName = memento.nextToken();
			JavaElement typeParameter = new TypeParameter(this, typeParameterName);
			return typeParameter.getHandleFromMemento(memento, workingCopyOwner);
		case JEM_ANNOTATION:
			if (!memento.hasMoreTokens()) return this;
			String annotationName = memento.nextToken();
			JavaElement annotation = new Annotation(this, annotationName);
			return annotation.getHandleFromMemento(memento, workingCopyOwner);
	}
	return null;
}
/**
 * @see JavaElement#getHandleMemento()
 */
@Override
protected char getHandleMementoDelimiter() {
	return JavaElement.JEM_TYPE;
}
/*
 * Returns the outermost context defining a local element. Per construction, it can only be a
 * method/field/initializarer member; thus, returns null if this member is already a top-level type or member type.
 * e.g for X.java/X/Y/foo()/Z/bar()/T, it will return X.java/X/Y/foo()
 */
public Member getOuterMostLocalContext() {
	IJavaElement current = this;
	Member lastLocalContext = null;
	parentLoop: while (true) {
		switch (current.getElementType()) {
			case CLASS_FILE:
			case COMPILATION_UNIT:
				break parentLoop; // done recursing
			case TYPE:
				// cannot be a local context
				break;
			case INITIALIZER:
			case FIELD:
			case METHOD:
				 // these elements can define local members
				lastLocalContext = (Member) current;
				break;
		}
		current = current.getParent();
	}
	return lastLocalContext;
}
@Override
public ISourceRange getJavadocRange() throws JavaModelException {
	ISourceRange range= getSourceRange();
	if (range == null) return null;
	IBuffer buf= null;
	if (isBinary()) {
		buf = getClassFile().getBuffer();
	} else {
		ICompilationUnit compilationUnit = getCompilationUnit();
		if (!compilationUnit.isConsistent()) {
			return null;
		}
		buf = compilationUnit.getBuffer();
	}
	final int start= range.getOffset();
	final int length= range.getLength();
	if (length > 0 && buf.getChar(start) == '/') {
		IScanner scanner= ToolFactory.createScanner(true, false, false, false);
		try {
			scanner.setSource(buf.getText(start, length).toCharArray());
			int docOffset= -1;
			int docEnd= -1;

			int terminal= scanner.getNextToken();
			loop: while (true) {
				switch(terminal) {
					case ITerminalSymbols.TokenNameCOMMENT_JAVADOC :
						docOffset= scanner.getCurrentTokenStartPosition();
						docEnd= scanner.getCurrentTokenEndPosition() + 1;
						terminal= scanner.getNextToken();
						break;
					case ITerminalSymbols.TokenNameCOMMENT_LINE :
					case ITerminalSymbols.TokenNameCOMMENT_BLOCK :
					case ITerminalSymbols.TokenNameCOMMA:
						terminal= scanner.getNextToken();
						continue loop;
					default :
						break loop;
				}
			}
			if (docOffset != -1) {
				return new SourceRange(docOffset + start, docEnd - docOffset);
			}
		} catch (InvalidInputException | IndexOutOfBoundsException e) {
			// https://bugs.eclipse.org/bugs/show_bug.cgi?id=305001
		}
	}
	return null;
}
/**
 * @see IMember
 */
@Override
public ISourceRange getNameRange() throws JavaModelException {
	MemberElementInfo info= (MemberElementInfo)getElementInfo();
	return new SourceRange(info.getNameSourceStart(), info.getNameSourceEnd() - info.getNameSourceStart() + 1);
}
/**
 * @see IMember
 */
@Override
public IType getType(String typeName, int count) {
	if (isBinary()) {
		throw new IllegalArgumentException("Not a source member " + toStringWithAncestors()); //$NON-NLS-1$
	} else {
		SourceType type = new SourceType(this, typeName);
		type.occurrenceCount = count;
		return type;
	}
}
/**
 * @see IMember#getTypeRoot()
 */
@Override
public ITypeRoot getTypeRoot() {
	IJavaElement element = getParent();
	while (element instanceof IMember) {
		element= element.getParent();
	}
	return (ITypeRoot) element;
}
/**
 * @see IMember
 */
@Override
public boolean isBinary() {
	return false;
}
protected boolean isMainMethod(IMethod method) throws JavaModelException {
	if ("main".equals(method.getElementName()) && Signature.SIG_VOID.equals(method.getReturnType())) { //$NON-NLS-1$
		int flags= method.getFlags();
		IType declaringType = null;
		if (Flags.isStatic(flags) &&
				(Flags.isPublic(flags) ||
						((declaringType = getDeclaringType()) != null && declaringType.isInterface()))) {
			String[] paramTypes= method.getParameterTypes();
			if (paramTypes.length == 1) {
				String typeSignature=  Signature.toString(paramTypes[0]);
				return "String[]".equals(Signature.getSimpleName(typeSignature)); //$NON-NLS-1$
			}
		}
	}
	return false;
}
/**
 * @see IJavaElement
 */
@Override
public boolean isReadOnly() {
	return getClassFile() != null;
}
/**
 */
@Override
public String readableName() {

	IJavaElement declaringType = getDeclaringType();
	if (declaringType != null) {
		String declaringName = ((JavaElement) getDeclaringType()).readableName();
		StringBuilder buffer = new StringBuilder(declaringName);
		buffer.append('.');
		buffer.append(getElementName());
		return buffer.toString();
	} else {
		return super.readableName();
	}
}
}
