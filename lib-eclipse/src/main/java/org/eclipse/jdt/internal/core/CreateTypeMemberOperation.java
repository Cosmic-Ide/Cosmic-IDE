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
 *******************************************************************************/
package org.eclipse.jdt.internal.core;

import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModelStatus;
import org.eclipse.jdt.core.IJavaModelStatusConstants;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.RecordDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.formatter.IndentManipulation;
import org.eclipse.jdt.internal.compiler.parser.ScannerHelper;

/**
 * Implements functionality common to
 * operations that create type members.
 */
@SuppressWarnings("rawtypes")
public abstract class CreateTypeMemberOperation extends CreateElementInCUOperation {
	/**
	 * The source code for the new member.
	 */
	protected String source = null;
	/**
	 * The name of the <code>ASTNode</code> that may be used to
	 * create this new element.
	 * Used by the <code>CopyElementsOperation</code> for renaming
	 */
	protected String alteredName;
	/**
	 * The AST node representing the element that
	 * this operation created.
	 */
	 protected ASTNode createdNode;
/**
 * When executed, this operation will create a type member
 * in the given parent element with the specified source.
 */
public CreateTypeMemberOperation(IJavaElement parentElement, String source, boolean force) {
	super(parentElement);
	this.source = source;
	this.force = force;
}
@Override
protected StructuralPropertyDescriptor getChildPropertyDescriptor(ASTNode parent) {
	switch (parent.getNodeType()) {
		case ASTNode.COMPILATION_UNIT:
			return CompilationUnit.TYPES_PROPERTY;
		case ASTNode.ENUM_DECLARATION:
			return EnumDeclaration.BODY_DECLARATIONS_PROPERTY;
		case ASTNode.ANNOTATION_TYPE_DECLARATION:
			return AnnotationTypeDeclaration.BODY_DECLARATIONS_PROPERTY;
		case ASTNode.RECORD_DECLARATION:
			return RecordDeclaration.BODY_DECLARATIONS_PROPERTY;
		default:
			return TypeDeclaration.BODY_DECLARATIONS_PROPERTY;
	}
}
@Override
protected ASTNode generateElementAST(ASTRewrite rewriter, ICompilationUnit cu) throws JavaModelException {
	if (this.createdNode == null) {
		this.source = removeIndentAndNewLines(this.source, cu);
		ASTParser parser = ASTParser.newParser(getLatestASTLevel());
		parser.setSource(this.source.toCharArray());
		parser.setProject(getCompilationUnit().getJavaProject());
		parser.setKind(ASTParser.K_CLASS_BODY_DECLARATIONS);
		ASTNode node = parser.createAST(this.progressMonitor);
		String createdNodeSource;
		if (node.getNodeType() != ASTNode.TYPE_DECLARATION) {
			createdNodeSource = generateSyntaxIncorrectAST();
			if (this.createdNode == null)
				throw new JavaModelException(new JavaModelStatus(IJavaModelStatusConstants.INVALID_CONTENTS));
		} else {
			TypeDeclaration typeDeclaration = (TypeDeclaration) node;
			if ((typeDeclaration.getFlags() & ASTNode.MALFORMED) != 0) {
				createdNodeSource = generateSyntaxIncorrectAST();
				if (this.createdNode == null)
					throw new JavaModelException(new JavaModelStatus(IJavaModelStatusConstants.INVALID_CONTENTS));
			} else {
				List bodyDeclarations = typeDeclaration.bodyDeclarations();
				if (bodyDeclarations.size() == 0) {
					throw new JavaModelException(new JavaModelStatus(IJavaModelStatusConstants.INVALID_CONTENTS));
				}
				this.createdNode = (ASTNode) bodyDeclarations.iterator().next();
				createdNodeSource = this.source;
			}
		}
		if (this.alteredName != null) {
			SimpleName newName = this.createdNode.getAST().newSimpleName(this.alteredName);
			SimpleName oldName = rename(this.createdNode, newName);
			int nameStart = oldName.getStartPosition();
			int nameEnd = nameStart + oldName.getLength();
			StringBuilder newSource = new StringBuilder();
			if (this.source.equals(createdNodeSource)) {
				newSource.append(createdNodeSource.substring(0, nameStart));
				newSource.append(this.alteredName);
				newSource.append(createdNodeSource.substring(nameEnd));
			} else {
				// syntactically incorrect source
				int createdNodeStart = this.createdNode.getStartPosition();
				int createdNodeEnd = createdNodeStart + this.createdNode.getLength();
				newSource.append(createdNodeSource.substring(createdNodeStart, nameStart));
				newSource.append(this.alteredName);
				newSource.append(createdNodeSource.substring(nameEnd, createdNodeEnd));

			}
			this.source = newSource.toString();
		}
	}
	if (rewriter == null) return this.createdNode;
	// return a string place holder (instead of the created node) so has to not lose comments and formatting
	return rewriter.createStringPlaceholder(this.source, this.createdNode.getNodeType());
}
private String removeIndentAndNewLines(String code, ICompilationUnit cu) throws JavaModelException {
	IJavaProject project = cu.getJavaProject();
	Map options = project.getOptions(true/*inherit JavaCore options*/);
	int tabWidth = IndentManipulation.getTabWidth(options);
	int indentWidth = IndentManipulation.getIndentWidth(options);
	int indent = IndentManipulation.measureIndentUnits(code, tabWidth, indentWidth);
	int firstNonWhiteSpace = -1;
	int length = code.length();
	while (firstNonWhiteSpace < length-1)
		if (!ScannerHelper.isWhitespace(code.charAt(++firstNonWhiteSpace)))
			break;
	int lastNonWhiteSpace = length;
	while (lastNonWhiteSpace > 0)
		if (!ScannerHelper.isWhitespace(code.charAt(--lastNonWhiteSpace)))
			break;
	String lineDelimiter = cu.findRecommendedLineSeparator();
	return IndentManipulation.changeIndent(code.substring(firstNonWhiteSpace, lastNonWhiteSpace+1), indent, tabWidth, indentWidth, "", lineDelimiter); //$NON-NLS-1$
}
/*
 * Renames the given node to the given name.
 * Returns the old name.
 */
protected abstract SimpleName rename(ASTNode node, SimpleName newName);
/**
 * Generates an <code>ASTNode</code> based on the source of this operation
 * when there is likely a syntax error in the source.
 * Returns the source used to generate this node.
 */
protected String generateSyntaxIncorrectAST() {
	//create some dummy source to generate an ast node
	StringBuilder buff = new StringBuilder();
	IType type = getType();
	String lineSeparator = org.eclipse.jdt.internal.core.util.Util.getLineSeparator(this.source, type == null ? null : type.getJavaProject());
	buff.append(lineSeparator + " public class A {" + lineSeparator); //$NON-NLS-1$
	buff.append(this.source);
	buff.append(lineSeparator).append('}');
	ASTParser parser = ASTParser.newParser(getLatestASTLevel());
	parser.setSource(buff.toString().toCharArray());
	CompilationUnit compilationUnit = (CompilationUnit) parser.createAST(null);
	TypeDeclaration typeDeclaration = (TypeDeclaration) compilationUnit.types().iterator().next();
	List bodyDeclarations = typeDeclaration.bodyDeclarations();
	if (bodyDeclarations.size() != 0)
		this.createdNode = (ASTNode) bodyDeclarations.iterator().next();
	return buff.toString();
}
/**
 * Returns the IType the member is to be created in.
 */
protected IType getType() {
	return (IType)getParentElement();
}
/**
 * Sets the name of the <code>ASTNode</code> that will be used to
 * create this new element.
 * Used by the <code>CopyElementsOperation</code> for renaming
 */
@Override
protected void setAlteredName(String newName) {
	this.alteredName = newName;
}
/**
 * Possible failures: <ul>
 *  <li>NO_ELEMENTS_TO_PROCESS - the parent element supplied to the operation is
 * 		<code>null</code>.
 *	<li>INVALID_CONTENTS - The source is <code>null</code> or has serious syntax errors.
  *	<li>NAME_COLLISION - A name collision occurred in the destination
 * </ul>
 */
@Override
public IJavaModelStatus verify() {
	IJavaModelStatus status = super.verify();
	if (!status.isOK()) {
		return status;
	}
	if (this.source == null) {
		return new JavaModelStatus(IJavaModelStatusConstants.INVALID_CONTENTS);
	}
	if (!this.force) {
		//check for name collisions
		try {
			ICompilationUnit cu = getCompilationUnit();
			generateElementAST(null, cu);
		} catch (JavaModelException jme) {
			return jme.getJavaModelStatus();
		}
		return verifyNameCollision();
	}

	return JavaModelStatus.VERIFIED_OK;
}
/**
 * Verify for a name collision in the destination container.
 */
protected IJavaModelStatus verifyNameCollision() {
	return JavaModelStatus.VERIFIED_OK;
}
}
