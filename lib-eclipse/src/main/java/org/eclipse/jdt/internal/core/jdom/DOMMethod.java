/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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
package org.eclipse.jdt.internal.core.jdom;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.jdom.*;
import org.eclipse.jdt.internal.core.util.Messages;
import org.eclipse.jdt.internal.core.util.CharArrayBuffer;
import org.eclipse.jdt.internal.core.util.Util;
/**
 * DOMMethod provides an implementation of IDOMMethod.
 *
 * @see IDOMMethod
 * @see DOMNode
 * @deprecated The JDOM was made obsolete by the addition in 2.0 of the more
 * powerful, fine-grained DOM/AST API found in the
 * org.eclipse.jdt.core.dom package.
 */
// TODO (jerome) - add implementation support for 1.5 features
class DOMMethod extends DOMMember implements IDOMMethod {

	/**
	 * Contains the return type of the method when the
	 * return type has been altered from the contents
	 * in the document, otherwise <code>null</code>.
	 */
	protected String fReturnType;

	/**
	 * The original inclusive source range of the
	 * method's return type in the document, or -1's
	 * if no return type is present in the document.
	 * If the return type of this method is qualified with
	 * '[]' following the parameter list, this array has
	 * four entries. In this case, the last two entries
	 * of the array are the inclusive source range of
	 * the array qualifiers.
	 */
	protected int[]  fReturnTypeRange;

	/**
	 * Contains the textual representation of the method's
	 * parameter list, including open and closing parentheses
	 * when the parameters had been altered from the contents
	 * in the document, otherwise <code>null</code>.
	 */
	protected char[] fParameterList;

	/**
	 * The original inclusive source range of the
	 * method's parameter list in the document.
	 */
	protected int[]  fParameterRange;

	/**
	 * Contains the textual representation of the method's
	 * exception list when the exceptions had been altered
	 * from the contents in the document, otherwise
	 * <code>null</code>. The exception list is a comment
	 * delimited list of exceptions, not including the "throws"
	 * keyword.
	 */
	protected char[] fExceptionList;

	/**
	 * The original inclusive source range of the
	 * method's exception list in the document.
	 */
	protected int[]  fExceptionRange;

	/**
	 * Contains the method's body when the body has
	 * been altered from the contents in the document,
	 * otherwise <code>null</code>. The body includes everything
	 * between and including the enclosing braces, and trailing
	 * whitespace.
	 */
	protected String fBody;

	/**
	 * The original inclusive source range of the
	 * method's body.
	 */
	protected int[]  fBodyRange;


	/**
	 * Names of parameters in the method parameter list,
	 * or <code>null</code> if the method has no parameters.
	 */
	protected String[] fParameterNames;

	/**
	 * Types of parameters in the method parameter list,
	 * or <code>null</code> if the method has no parameters.
	 */
	protected String[] fParameterTypes;

	/**
	 * The exceptions the method throws, or <code>null</code>
	 * if the method throws no exceptions.
	 */
	protected String[] fExceptions;

	/**
	 * The formal type parameters.
	 * @since 3.0
	 */
	protected String[] fTypeParameters = CharOperation.NO_STRINGS;

	/**
	 * Default value for this attotation type member (only),
	 * or <code>null</code> if none.
	 * @since 3.0
	 */
	protected String fDefaultValue = null;

/**
 * Constructs an empty method node.
 */
DOMMethod() {
	// Constructs an empty method node
}
/**
 * Creates a new detailed METHOD document fragment on the given range of the document.
 *
 * @param document - the document containing this node's original contents
 * @param sourceRange - a two element array of integers describing the
 *		entire inclusive source range of this node within its document.
 * 		Contents start on and include the character at the first position.
 *		Contents end on and include the character at the last position.
 *		An array of -1's indicates this node's contents do not exist
 *		in the document.
 * @param name - the identifier portion of the name of this node, or
 *		<code>null</code> if this node does not have a name
 * @param nameRange - a two element array of integers describing the
 *		entire inclusive source range of this node's name within its document,
 *		including any array qualifiers that might immediately follow the name
 *		or -1's if this node does not have a name.
 * @param commentRange - a two element array describing the comments that precede
 *		the member declaration. The first matches the start of this node's
 *		sourceRange, and the second is the new-line or first non-whitespace
 *		character following the last comment. If no comments are present,
 *		this array contains two -1's.
 * @param flags - an integer representing the modifiers for this member. The
 *		integer can be analyzed with org.eclipse.jdt.core.Flags
 * @param modifierRange - a two element array describing the location of
 *		modifiers for this member within its source range. The first integer
 *		is the first character of the first modifier for this member, and
 *		the second integer is the last whitespace character preceeding the
 *		next part of this member declaration. If there are no modifiers present
 *		in this node's source code (that is, package default visibility), this array
 *		contains two -1's.
 * @param isConstructor - true if the method is a contructor, otherwise false
 * @param returnType - the normalized return type of this method
 * @param returnTypeRange - a two element array describing the location of the
 *		return type within the method's source range. The first integer is is
 *		the position of the first character in the return type, and the second
 *		integer is the position of the last character in the return type.
 *		For constructors, the contents of this array are -1's.
 * 		If the return type of this method is qualified with '[]' following the
 *		parameter list, this array has four entries. In this case, the last
 *		two entries of the array are the inclusive source range of the array
 *		qualifiers.
 * @param parameterTypes - an array of parameter types in the method declaration
 * 		or <code>null</code> if the method has no parameters
 * @param parameterNames - an array of parameter names in the method declaration
 * 		or <code>null</code> if the method has no parameters
 * @param parameterRange - a two element array describing the location of the
 * 		parameter list in the method. The first integer is the location of the
 *		open parenthesis and the second integer is the location of the closing
 *		parenthesis.
 * @param exceptions - an array of the names of exceptions thrown by this method
 *		or <code>null</code> if the method throws no exceptions
 * @param exceptionRange - a two element array describing the location of the
 * 		exception list in the method declaration. The first integer is the position
 * 		of the first character in the first exception the method throws, and the
 *		second integer is the position of the last character of the last exception
 *		this method throws.
 * @param bodyRange - a two element array describing the location of the method's body.
 *		The first integer is the first character following the method's
 *		parameter list, or exception list (if present). The second integer is the location
 * 		of the last character in the method's source range.
 */
DOMMethod(char[] document, int[] sourceRange, String name, int[] nameRange, int[] commentRange, int flags, int[] modifierRange, boolean isConstructor, String returnType, int[] returnTypeRange, String[] parameterTypes, String[] parameterNames, int[] parameterRange, String[] exceptions, int[] exceptionRange, int[] bodyRange) {
	super(document, sourceRange, name, nameRange, commentRange, flags, modifierRange);

	setMask(MASK_IS_CONSTRUCTOR, isConstructor);
	this.fReturnType= returnType;
	this.fReturnTypeRange= returnTypeRange;
	this.fParameterTypes= parameterTypes;
	this.fParameterNames= parameterNames;
	this.fParameterRange= parameterRange;
	this.fExceptionRange= exceptionRange;
	this.fExceptions= exceptions;
	setHasBody(true);
	this.fBodyRange= bodyRange;
	setMask(MASK_DETAILED_SOURCE_INDEXES, true);

}
/**
 * Creates a new simple METHOD document fragment on the given range of the document.
 *
 * @param document - the document containing this node's original contents
 * @param sourceRange - a two element array of integers describing the
 *		entire inclusive source range of this node within its document.
 * 		Contents start on and include the character at the first position.
 *		Contents end on and include the character at the last position.
 *		An array of -1's indicates this node's contents do not exist
 *		in the document.
 * @param name - the identifier portion of the name of this node, or
 *		<code>null</code> if this node does not have a name
 * @param nameRange - a two element array of integers describing the
 *		entire inclusive source range of this node's name within its document,
 *		including any array qualifiers that might immediately follow the name
 *		or -1's if this node does not have a name.
 * @param flags - an integer representing the modifiers for this member. The
 *		integer can be analyzed with org.eclipse.jdt.core.Flags
 * @param isConstructor - true if the method is a contructor, otherwise false
 * @param returnType - the normalized return type of this method
 * @param parameterTypes - an array of parameter types in the method declaration
 * 		or <code>null</code> if the method has no parameters
 * @param parameterNames - an array of parameter names in the method declaration
 * 		or <code>null</code> if the method has no parameters
 * @param exceptions - an array of the names of exceptions thrown by this method
 *		or <code>null</code> if the method throws no exceptions
 */
DOMMethod(char[] document, int[] sourceRange, String name, int[] nameRange, int flags, boolean isConstructor, String returnType, String[] parameterTypes, String[] parameterNames, String[] exceptions) {
	this(document, sourceRange, name, nameRange, new int[] {-1, -1}, flags, new int[] {-1, -1}, isConstructor, returnType, new int[] {-1, -1}, parameterTypes, parameterNames, new int[] {-1, -1}, exceptions, new int[] {-1, -1}, new int[] {-1, -1});
	setMask(MASK_DETAILED_SOURCE_INDEXES, false);
}
/**
 * @see IDOMMethod#addException(String)
 */
@Override
public void addException(String name) throws IllegalArgumentException {
	if (name == null) {
		throw new IllegalArgumentException(Messages.dom_nullExceptionType);
	}
	if (this.fExceptions == null) {
		this.fExceptions= new String[1];
		this.fExceptions[0]= name;
	} else {
		this.fExceptions= appendString(this.fExceptions, name);
	}
	setExceptions(this.fExceptions);
}
/**
 * @see IDOMMethod#addParameter(String, String)
 */
@Override
public void addParameter(String type, String name) throws IllegalArgumentException {
	if (type == null) {
		throw new IllegalArgumentException(Messages.dom_nullTypeParameter);
	}
	if (name == null) {
		throw new IllegalArgumentException(Messages.dom_nullNameParameter);
	}
	if (this.fParameterNames == null) {
		this.fParameterNames= new String[1];
		this.fParameterNames[0]= name;
	} else {
		this.fParameterNames= appendString(this.fParameterNames, name);
	}
	if (this.fParameterTypes == null) {
		this.fParameterTypes= new String[1];
		this.fParameterTypes[0]= type;
	} else {
		this.fParameterTypes= appendString(this.fParameterTypes, type);
	}
	setParameters(this.fParameterTypes, this.fParameterNames);
}
/**
 * @see DOMMember#appendMemberBodyContents(CharArrayBuffer)
 */
@Override
protected void appendMemberBodyContents(CharArrayBuffer buffer) {
	if (this.fBody != null) {
		buffer.append(this.fBody);
	} else {
		buffer.append(this.fDocument, this.fBodyRange[0], this.fBodyRange[1] + 1 - this.fBodyRange[0]);
	}
}
/**
 * @see DOMMember#appendMemberDeclarationContents(CharArrayBuffer)
 */
@Override
protected void appendMemberDeclarationContents(CharArrayBuffer buffer) {

	if (isConstructor()) {
		buffer
			.append(getConstructorName())
			.append(this.fDocument, this.fNameRange[1] + 1, this.fParameterRange[0] - this.fNameRange[1] - 1);
	} else {
		buffer.append(getReturnTypeContents());
		if (this.fReturnTypeRange[0] >= 0) {
			buffer.append(this.fDocument, this.fReturnTypeRange[1] + 1, this.fNameRange[0] - this.fReturnTypeRange[1] - 1);
		} else {
			buffer.append(' ');
		}
		buffer
			.append(getNameContents())
			.append(this.fDocument, this.fNameRange[1] + 1, this.fParameterRange[0] - this.fNameRange[1] - 1);
	}
	if (this.fParameterList != null) {
		buffer.append(this.fParameterList);
	} else {
		buffer.append(this.fDocument, this.fParameterRange[0], this.fParameterRange[1] + 1 - this.fParameterRange[0]);
	}
	int start;
	if (hasTrailingArrayQualifier() && isReturnTypeAltered()) {
		start= this.fReturnTypeRange[3] + 1;
	} else {
		start= this.fParameterRange[1] + 1;
	}
	if (this.fExceptions != null) {
		// add 'throws' keyword
		if (this.fExceptionRange[0] >= 0) {
			buffer.append(this.fDocument, start, this.fExceptionRange[0] - start);
		} else {
			buffer.append(" throws "); //$NON-NLS-1$
		}
		// add exception list
		if (this.fExceptionList != null) {
			buffer.append(this.fExceptionList);
			// add space before body
			if (this.fExceptionRange[0] >= 0) {
				buffer.append(this.fDocument, this.fExceptionRange[1] + 1, this.fBodyRange[0] - this.fExceptionRange[1] - 1);
			} else {
				buffer.append(this.fDocument, this.fParameterRange[1] + 1, this.fBodyRange[0] - this.fParameterRange[1] - 1);
			}
		} else {
			// add list and space before body
			buffer.append(this.fDocument, this.fExceptionRange[0], this.fBodyRange[0] - this.fExceptionRange[0]);
		}
	} else {
		// add space before body
		if (this.fExceptionRange[0] >= 0) {
			buffer.append(this.fDocument, this.fExceptionRange[1] + 1, this.fBodyRange[0] - this.fExceptionRange[1] - 1);
		} else {
			buffer.append(this.fDocument, start, this.fBodyRange[0] - start);
		}
	}

}
/**
 * @see DOMMember#appendSimpleContents(CharArrayBuffer)
 */
@Override
protected void appendSimpleContents(CharArrayBuffer buffer) {
	// append eveything before my name
	buffer.append(this.fDocument, this.fSourceRange[0], this.fNameRange[0] - this.fSourceRange[0]);
	// append my name
	if (isConstructor()) {
		buffer.append(getConstructorName());
	} else {
		buffer.append(this.fName);
	}
	// append everything after my name
	buffer.append(this.fDocument, this.fNameRange[1] + 1, this.fSourceRange[1] - this.fNameRange[1]);
}
/**
 * @see IDOMMethod#getBody()
 */
@Override
public String getBody() {
	becomeDetailed();
	if (hasBody()) {
		if (this.fBody != null) {
			return this.fBody;
		} else {
			return new String(this.fDocument, this.fBodyRange[0], this.fBodyRange[1] + 1 - this.fBodyRange[0]);
		}
	} else {
		return null;
	}
}
/**
 * Returns the simple name of the enclsoing type for this constructor.
 * If the constuctor is not currently enclosed in a type, the original
 * name of the constructor as found in the documnent is returned.
 */
protected String getConstructorName() {

	if (isConstructor()) {
		if (getParent() != null) {
			return getParent().getName();
		} else {
			// If there is no parent use the original name
			return new String(getNameContents());
		}
	} else {
		return null;
	}

}
/**
 * @see DOMNode#getDetailedNode()
 */
@Override
protected DOMNode getDetailedNode() {
	return (DOMNode)getFactory().createMethod(getContents());
}
/**
 * @see IDOMMethod#getExceptions()
 */
@Override
public String[] getExceptions() {
	return this.fExceptions;
}
@Override
protected char[] generateFlags() {
	char[] flags= Flags.toString(getFlags() & ~Flags.AccVarargs).toCharArray();
	if (flags.length == 0) {
		return flags;
	} else {
		return CharOperation.concat(flags, new char[] {' '});
	}
}/**
 * @see IDOMNode#getJavaElement
 */
@Override
public IJavaElement getJavaElement(IJavaElement parent) throws IllegalArgumentException {
	if (parent.getElementType() == IJavaElement.TYPE) {
		// translate parameter types to signatures
		String[] sigs= null;
		if (this.fParameterTypes != null) {
			sigs= new String[this.fParameterTypes.length];
			int i;
			for (i= 0; i < this.fParameterTypes.length; i++) {
				sigs[i]= Signature.createTypeSignature(this.fParameterTypes[i].toCharArray(), false);
			}
		}
		String name= null;
		if (isConstructor()) {
			name= getConstructorName();
		} else {
			name= getName();
		}
		return ((IType)parent).getMethod(name, sigs);
	} else {
		throw new IllegalArgumentException(Messages.element_illegalParent);
	}
}
/**
 * @see DOMMember#getMemberDeclarationStartPosition()
 */
@Override
protected int getMemberDeclarationStartPosition() {
	if (this.fReturnTypeRange[0] >= 0) {
		return this.fReturnTypeRange[0];
	} else {
		return this.fNameRange[0];
	}
}
/**
 * @see IDOMNode#getName()
 */
@Override
public String getName() {
	if (isConstructor()) {
		return null;
	} else {
		return super.getName();
	}
}
/**
 * @see IDOMNode#getNodeType()
 */
@Override
public int getNodeType() {
	return IDOMNode.METHOD;
}
/**
 * @see IDOMMethod#getParameterNames()
 */
@Override
public String[] getParameterNames() {
	return this.fParameterNames;
}
/**
 * @see IDOMMethod#getParameterTypes()
 */
@Override
public String[] getParameterTypes() {
	return this.fParameterTypes;
}
/**
 * @see IDOMMethod#getReturnType()
 */
@Override
public String getReturnType() {
	if (isConstructor()) {
		return null;
	} else {
		return this.fReturnType;
	}
}
/**
 * Returns the source code to be used for this method's return type
 */
protected char[] getReturnTypeContents() {
	if (isConstructor()) {
		return null;
	} else {
		if (isReturnTypeAltered()) {
			return this.fReturnType.toCharArray();
		} else {
			return CharOperation.subarray(this.fDocument, this.fReturnTypeRange[0], this.fReturnTypeRange[1] + 1);
		}

	}
}
/**
 * Returns true if this method's return type has
 * array qualifiers ('[]') following the parameter list.
 */
protected boolean hasTrailingArrayQualifier() {
	return this.fReturnTypeRange.length > 2;
}
/**
 * @see IDOMMethod#isConstructor()
 */
@Override
public boolean isConstructor() {
	return getMask(MASK_IS_CONSTRUCTOR);
}
/**
 * Returns true if this method's return type has been altered
 * from the original document contents.
 */
protected boolean isReturnTypeAltered() {
	return getMask(MASK_RETURN_TYPE_ALTERED);
}
/**
 * @see IDOMNode#isSignatureEqual(IDOMNode)
 *
 * <p>Two methods have equal signatures if there names are the same
 * and their parameter types are the same.
 */
@Override
public boolean isSignatureEqual(IDOMNode node) {
	boolean ok= node.getNodeType() == getNodeType();
	if (ok) {
		IDOMMethod method= (IDOMMethod)node;
		ok = (isConstructor() && method.isConstructor()) ||
			(!isConstructor() && !method.isConstructor());
		if (ok && !isConstructor()) {
			ok= getName().equals(method.getName());
		}
		if (!ok) {
			return false;
		}

		String[] types= method.getParameterTypes();
		if (this.fParameterTypes == null || this.fParameterTypes.length == 0) {
			// this method has no parameters
			if (types == null || types.length == 0) {
				// the other method has no parameters either
				return true;
			}
		} else {
			// this method has parameters
			if (types == null || types.length == 0) {
				// the other method has no parameters
				return false;
			}
			if (this.fParameterTypes.length != types.length) {
				// the methods have a different number of parameters
				return false;
			}
			int i;
			for (i= 0; i < types.length; i++) {
				if (!this.fParameterTypes[i].equals(types[i])) {
					return false;
				}
			}
			return true;
		}
	}
	return false;

}
/**
 * @see DOMNode
 */
@Override
protected DOMNode newDOMNode() {
	return new DOMMethod();
}
/**
 * Offsets all the source indexes in this node by the given amount.
 */
@Override
protected void offset(int offset) {
	super.offset(offset);
	offsetRange(this.fBodyRange, offset);
	offsetRange(this.fExceptionRange, offset);
	offsetRange(this.fParameterRange, offset);
	offsetRange(this.fReturnTypeRange, offset);
}
/**
 * @see IDOMMethod#setBody
 */
@Override
public void setBody(String body) {
	becomeDetailed();
	fragment();
	this.fBody= body;
	setHasBody(body != null);
	if (!hasBody()) {
		this.fBody= ";" + Util.getLineSeparator(body, null); //$NON-NLS-1$
	}
}
/**
 * Sets the end of the body range
 */
void setBodyRangeEnd(int end) {
	this.fBodyRange[1] = end;
}
/**
 * @see IDOMMethod#setConstructor(boolean)
 */
@Override
public void setConstructor(boolean b) {
	becomeDetailed();
	setMask(MASK_IS_CONSTRUCTOR, b);
	fragment();
}
/**
 * @see IDOMMethod#setExceptions(String[])
 */
@Override
public void setExceptions(String[] names) {
	becomeDetailed();
	if (names == null || names.length == 0) {
		this.fExceptions= null;
	} else {
		this.fExceptions= names;
		CharArrayBuffer buffer = new CharArrayBuffer();
		char[] comma = new char[] {',', ' '};
		for (int i = 0, length = names.length; i < length; i++) {
			if (i > 0)
				buffer.append(comma);
			buffer.append(names[i]);
		}
		this.fExceptionList= buffer.getContents();
	}
	fragment();
}
/**
 * @see IDOMMethod#setName
 */
@Override
public void setName(String name) {
	if (name == null) {
		throw new IllegalArgumentException(Messages.element_nullName);
	} else {
		super.setName(name);
	}
}
/**
 * @see IDOMMethod#setParameters(String[], String[])
 */
@Override
public void setParameters(String[] types, String[] names) throws IllegalArgumentException {
	becomeDetailed();
	if (types== null || names == null) {
		if (types == null && names == null) {
			this.fParameterTypes= null;
			this.fParameterNames= null;
			this.fParameterList= new char[] {'(',')'};
		} else {
			throw new IllegalArgumentException(Messages.dom_mismatchArgNamesAndTypes);
		}
	} else if (names.length != types.length) {
		throw new IllegalArgumentException(Messages.dom_mismatchArgNamesAndTypes);
	} else if (names.length == 0) {
		setParameters(null, null);
	} else {
		this.fParameterNames= names;
		this.fParameterTypes= types;
		CharArrayBuffer parametersBuffer = new CharArrayBuffer();
		parametersBuffer.append("("); //$NON-NLS-1$
		char[] comma = new char[] {',', ' '};
		for (int i = 0; i < names.length; i++) {
			if (i > 0) {
				parametersBuffer.append(comma);
			}
			parametersBuffer
				.append(types[i])
				.append(' ')
				.append(names[i]);
		}
		parametersBuffer.append(')');
		this.fParameterList= parametersBuffer.getContents();
	}
	fragment();
}
/**
 * @see IDOMMethod#setReturnType(String)
 */
@Override
public void setReturnType(String name) throws IllegalArgumentException {
	if (name == null) {
		throw new IllegalArgumentException(Messages.dom_nullReturnType);
	}
	becomeDetailed();
	fragment();
	setReturnTypeAltered(true);
	this.fReturnType= name;
}
/**
 * Sets the state of this method declaration as having
 * the return type altered from the original document.
 */
protected void setReturnTypeAltered(boolean typeAltered) {
	setMask(MASK_RETURN_TYPE_ALTERED, typeAltered);
}
/**
 */
@Override
protected void setSourceRangeEnd(int end) {
	super.setSourceRangeEnd(end);
	this.fBodyRange[1]= end;
}
/**
 * @see DOMNode#shareContents(DOMNode)
 */
@Override
protected void shareContents(DOMNode node) {
	super.shareContents(node);
	DOMMethod method= (DOMMethod)node;
	this.fBody= method.fBody;
	this.fBodyRange= rangeCopy(method.fBodyRange);
	this.fExceptionList= method.fExceptionList;
	this.fExceptionRange= rangeCopy(method.fExceptionRange);
	this.fExceptions= method.fExceptions;
	this.fParameterList= method.fParameterList;
	this.fParameterNames= method.fParameterNames;
	this.fParameterRange= rangeCopy(method.fParameterRange);
	this.fParameterTypes= method.fParameterTypes;
	this.fReturnType= method.fReturnType;
	this.fReturnTypeRange= rangeCopy(method.fReturnTypeRange);
}
/**
 * @see IDOMNode#toString()
 */
@Override
public String toString() {
	if (isConstructor()) {
		return "CONSTRUCTOR"; //$NON-NLS-1$
	} else {
		return "METHOD: " + getName(); //$NON-NLS-1$
	}
}

/**
 * @see IDOMMethod#setDefault(java.lang.String)
 * @since 3.0
 */
@Override
public void setDefault(String defaultValue) {
	this.fDefaultValue =  defaultValue;
}

/**
 * @see IDOMMethod#getDefault()
 * @since 3.0
 */
@Override
public String getDefault() {
	return this.fDefaultValue;
}

/**
 * @see IDOMMethod#getTypeParameters()
 * @since 3.0
 */
@Override
public String[] getTypeParameters() {
	return this.fTypeParameters;
}

/**
 * @see IDOMMethod#setTypeParameters(java.lang.String[])
 * @since 3.0
 */
@Override
public void setTypeParameters(String[] typeParameters) {
	this.fTypeParameters = typeParameters;
}
}
