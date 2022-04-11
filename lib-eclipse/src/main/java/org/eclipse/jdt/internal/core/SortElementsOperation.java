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
 *     Alex Blewitt - alex_blewitt@yahoo.com https://bugs.eclipse.org/bugs/show_bug.cgi?id=171066
 *     Microsoft Corporation - read formatting options from the compilation unit
 *******************************************************************************/
package org.eclipse.jdt.internal.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModelStatus;
import org.eclipse.jdt.core.IJavaModelStatusConstants;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.util.CompilationUnitSorter;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.core.util.Messages;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.RangeMarker;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;

/**
 * This operation is used to sort elements in a compilation unit according to
 * certain criteria.
 *
 * @since 2.1
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class SortElementsOperation extends JavaModelOperation {
	public static final String CONTAINS_MALFORMED_NODES = "malformed"; //$NON-NLS-1$

	Comparator comparator;
	int[] positions;
    int apiLevel;

	/**
	 * Constructor for SortElementsOperation.
     *
     * @param level the AST API level; one of the AST LEVEL constants
	 * @param elements
	 * @param positions
	 * @param comparator
	 */
	public SortElementsOperation(int level, IJavaElement[] elements, int[] positions, Comparator comparator) {
		super(elements);
		this.comparator = comparator;
        this.positions = positions;
        this.apiLevel = level;
	}

	/**
	 * Returns the amount of work for the main task of this operation for
	 * progress reporting.
	 */
	protected int getMainAmountOfWork(){
		return this.elementsToProcess.length;
	}

	boolean checkMalformedNodes(ASTNode node) {
		Object property = node.getProperty(CONTAINS_MALFORMED_NODES);
		if (property == null) return false;
		return ((Boolean) property).booleanValue();
	}

	protected boolean isMalformed(ASTNode node) {
		return (node.getFlags() & ASTNode.MALFORMED) != 0;
	}

	/**
	 * @see org.eclipse.jdt.internal.core.JavaModelOperation#executeOperation()
	 */
	@Override
	protected void executeOperation() throws JavaModelException {
		try {
			beginTask(Messages.operation_sortelements, getMainAmountOfWork());
			CompilationUnit copy = (CompilationUnit) this.elementsToProcess[0];
			ICompilationUnit unit = copy.getPrimary();
			IBuffer buffer = copy.getBuffer();
			if (buffer  == null) {
				return;
			}
			char[] bufferContents = buffer.getCharacters();
			String result = processElement(unit, bufferContents);
			if (!CharOperation.equals(result.toCharArray(), bufferContents)) {
				copy.getBuffer().setContents(result);
			}
			worked(1);
		} finally {
			done();
		}
	}

	/**
	 * Calculates the required text edits to sort the <code>unit</code>
	 * @param group
	 * @return the edit or null if no sorting is required
	 */
	public TextEdit calculateEdit(org.eclipse.jdt.core.dom.CompilationUnit unit, TextEditGroup group) throws JavaModelException {
		if (this.elementsToProcess.length != 1)
			throw new JavaModelException(new JavaModelStatus(IJavaModelStatusConstants.NO_ELEMENTS_TO_PROCESS));

		if (!(this.elementsToProcess[0] instanceof ICompilationUnit))
			throw new JavaModelException(new JavaModelStatus(IJavaModelStatusConstants.INVALID_ELEMENT_TYPES, this.elementsToProcess[0]));

		try {
			beginTask(Messages.operation_sortelements, getMainAmountOfWork());

			ICompilationUnit cu= (ICompilationUnit)this.elementsToProcess[0];
			String content= cu.getBuffer().getContents();
			ASTRewrite rewrite= sortCompilationUnit(unit, group);
			if (rewrite == null) {
				return null;
			}

			Document document= new Document(content);
			return rewrite.rewriteAST(document, cu.getOptions(true));
		} finally {
			done();
		}
	}

	/**
	 * Method processElement.
	 * @param unit
	 * @param source
	 */
	private String processElement(ICompilationUnit unit, char[] source) {
		Document document = new Document(new String(source));
		CompilerOptions options = new CompilerOptions(unit.getJavaProject().getOptions(true));
		ASTParser parser = ASTParser.newParser(this.apiLevel);
		parser.setCompilerOptions(options.getMap());
		parser.setSource(source);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setResolveBindings(false);
		org.eclipse.jdt.core.dom.CompilationUnit ast = (org.eclipse.jdt.core.dom.CompilationUnit) parser.createAST(null);

		ASTRewrite rewriter= sortCompilationUnit(ast, null);
		if (rewriter == null)
			return document.get();

		TextEdit edits = rewriter.rewriteAST(document, unit.getJavaProject().getOptions(true));

		RangeMarker[] markers = null;
		if (this.positions != null) {
			markers = new RangeMarker[this.positions.length];
			for (int i = 0, max = this.positions.length; i < max; i++) {
				markers[i]= new RangeMarker(this.positions[i], 0);
				insert(edits, markers[i]);
			}
		}
		try {
			edits.apply(document, TextEdit.UPDATE_REGIONS);
			if (this.positions != null) {
				for (int i= 0, max = markers.length; i < max; i++) {
					this.positions[i]= markers[i].getOffset();
				}
			}
		} catch (BadLocationException e) {
			// ignore
		}
		return document.get();
	}


	private ASTRewrite sortCompilationUnit(org.eclipse.jdt.core.dom.CompilationUnit ast, final TextEditGroup group) {
		ast.accept(new ASTVisitor() {
			@Override
			public boolean visit(org.eclipse.jdt.core.dom.CompilationUnit compilationUnit) {
				List types = compilationUnit.types();
				boolean contains_malformed_nodes = false;
				for (Iterator iter = types.iterator(); iter.hasNext();) {
					AbstractTypeDeclaration typeDeclaration = (AbstractTypeDeclaration) iter.next();
					typeDeclaration.setProperty(CompilationUnitSorter.RELATIVE_ORDER, Integer.valueOf(typeDeclaration.getStartPosition()));
					contains_malformed_nodes |= isMalformed(typeDeclaration);
				}
				compilationUnit.setProperty(CONTAINS_MALFORMED_NODES, contains_malformed_nodes);
				return true;
			}
			@Override
			public boolean visit(AnnotationTypeDeclaration annotationTypeDeclaration) {
				List bodyDeclarations = annotationTypeDeclaration.bodyDeclarations();
				boolean contains_malformed_nodes = false;
				for (Iterator iter = bodyDeclarations.iterator(); iter.hasNext();) {
					BodyDeclaration bodyDeclaration = (BodyDeclaration) iter.next();
					bodyDeclaration.setProperty(CompilationUnitSorter.RELATIVE_ORDER, Integer.valueOf(bodyDeclaration.getStartPosition()));
					contains_malformed_nodes |= isMalformed(bodyDeclaration);
				}
				annotationTypeDeclaration.setProperty(CONTAINS_MALFORMED_NODES, contains_malformed_nodes);
				return true;
			}

			@Override
			public boolean visit(AnonymousClassDeclaration anonymousClassDeclaration) {
				List bodyDeclarations = anonymousClassDeclaration.bodyDeclarations();
				boolean contains_malformed_nodes = false;
				for (Iterator iter = bodyDeclarations.iterator(); iter.hasNext();) {
					BodyDeclaration bodyDeclaration = (BodyDeclaration) iter.next();
					bodyDeclaration.setProperty(CompilationUnitSorter.RELATIVE_ORDER, Integer.valueOf(bodyDeclaration.getStartPosition()));
					contains_malformed_nodes |= isMalformed(bodyDeclaration);
				}
				anonymousClassDeclaration.setProperty(CONTAINS_MALFORMED_NODES, contains_malformed_nodes);
				return true;
			}

			@Override
			public boolean visit(TypeDeclaration typeDeclaration) {
				List bodyDeclarations = typeDeclaration.bodyDeclarations();
				boolean contains_malformed_nodes = false;
				for (Iterator iter = bodyDeclarations.iterator(); iter.hasNext();) {
					BodyDeclaration bodyDeclaration = (BodyDeclaration) iter.next();
					bodyDeclaration.setProperty(CompilationUnitSorter.RELATIVE_ORDER, Integer.valueOf(bodyDeclaration.getStartPosition()));
					contains_malformed_nodes |= isMalformed(bodyDeclaration);
				}
				typeDeclaration.setProperty(CONTAINS_MALFORMED_NODES, contains_malformed_nodes);
				return true;
			}

			@Override
			public boolean visit(EnumDeclaration enumDeclaration) {
				List bodyDeclarations = enumDeclaration.bodyDeclarations();
				boolean contains_malformed_nodes = false;
				for (Iterator iter = bodyDeclarations.iterator(); iter.hasNext();) {
					BodyDeclaration bodyDeclaration = (BodyDeclaration) iter.next();
					bodyDeclaration.setProperty(CompilationUnitSorter.RELATIVE_ORDER, Integer.valueOf(bodyDeclaration.getStartPosition()));
					contains_malformed_nodes |= isMalformed(bodyDeclaration);
				}
				List enumConstants = enumDeclaration.enumConstants();
				for (Iterator iter = enumConstants.iterator(); iter.hasNext();) {
					EnumConstantDeclaration enumConstantDeclaration = (EnumConstantDeclaration) iter.next();
					enumConstantDeclaration.setProperty(CompilationUnitSorter.RELATIVE_ORDER, Integer.valueOf(enumConstantDeclaration.getStartPosition()));
					contains_malformed_nodes |= isMalformed(enumConstantDeclaration);
				}
				enumDeclaration.setProperty(CONTAINS_MALFORMED_NODES, contains_malformed_nodes);
				return true;
			}
		});

		final ASTRewrite rewriter= ASTRewrite.create(ast.getAST());
		final boolean[] hasChanges= new boolean[] {false};

		ast.accept(new ASTVisitor() {

			private void sortElements(List elements, ListRewrite listRewrite) {
				if (elements.size() == 0)
					return;

				final List myCopy = new ArrayList();
				myCopy.addAll(elements);
				Collections.sort(myCopy, SortElementsOperation.this.comparator);

				for (int i = 0; i < elements.size(); i++) {
					ASTNode oldNode= (ASTNode) elements.get(i);
					ASTNode newNode= (ASTNode) myCopy.get(i);
					if (oldNode != newNode) {
						listRewrite.replace(oldNode, rewriter.createMoveTarget(newNode), group);
						hasChanges[0]= true;
					}
				}
			}

			@Override
			public boolean visit(org.eclipse.jdt.core.dom.CompilationUnit compilationUnit) {
				if (checkMalformedNodes(compilationUnit)) {
					return true; // abort sorting of current element
				}

				sortElements(compilationUnit.types(), rewriter.getListRewrite(compilationUnit, org.eclipse.jdt.core.dom.CompilationUnit.TYPES_PROPERTY));
				return true;
			}

			@Override
			public boolean visit(AnnotationTypeDeclaration annotationTypeDeclaration) {
				if (checkMalformedNodes(annotationTypeDeclaration)) {
					return true; // abort sorting of current element
				}

				sortElements(annotationTypeDeclaration.bodyDeclarations(), rewriter.getListRewrite(annotationTypeDeclaration, AnnotationTypeDeclaration.BODY_DECLARATIONS_PROPERTY));
				return true;
			}

			@Override
			public boolean visit(AnonymousClassDeclaration anonymousClassDeclaration) {
				if (checkMalformedNodes(anonymousClassDeclaration)) {
					return true; // abort sorting of current element
				}

				sortElements(anonymousClassDeclaration.bodyDeclarations(), rewriter.getListRewrite(anonymousClassDeclaration, AnonymousClassDeclaration.BODY_DECLARATIONS_PROPERTY));
				return true;
			}

			@Override
			public boolean visit(TypeDeclaration typeDeclaration) {
				if (checkMalformedNodes(typeDeclaration)) {
					return true; // abort sorting of current element
				}

				sortElements(typeDeclaration.bodyDeclarations(), rewriter.getListRewrite(typeDeclaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY));
				return true;
			}

			@Override
			public boolean visit(EnumDeclaration enumDeclaration) {
				if (checkMalformedNodes(enumDeclaration)) {
					return true; // abort sorting of current element
				}

				sortElements(enumDeclaration.bodyDeclarations(), rewriter.getListRewrite(enumDeclaration, EnumDeclaration.BODY_DECLARATIONS_PROPERTY));
				sortElements(enumDeclaration.enumConstants(), rewriter.getListRewrite(enumDeclaration, EnumDeclaration.ENUM_CONSTANTS_PROPERTY));
				return true;
			}
		});

		if (!hasChanges[0])
			return null;

		return rewriter;
	}

	/**
	 * Possible failures:
	 * <ul>
	 *  <li>NO_ELEMENTS_TO_PROCESS - the compilation unit supplied to the operation is <code>null</code></li>.
	 *  <li>INVALID_ELEMENT_TYPES - the supplied elements are not an instance of IWorkingCopy</li>.
	 * </ul>
	 * @return IJavaModelStatus
	 */
	@Override
	public IJavaModelStatus verify() {
		if (this.elementsToProcess.length != 1) {
			return new JavaModelStatus(IJavaModelStatusConstants.NO_ELEMENTS_TO_PROCESS);
		}
		if (this.elementsToProcess[0] == null) {
			return new JavaModelStatus(IJavaModelStatusConstants.NO_ELEMENTS_TO_PROCESS);
		}
		if (!(this.elementsToProcess[0] instanceof ICompilationUnit) || !((ICompilationUnit) this.elementsToProcess[0]).isWorkingCopy()) {
			return new JavaModelStatus(IJavaModelStatusConstants.INVALID_ELEMENT_TYPES, this.elementsToProcess[0]);
		}
		return JavaModelStatus.VERIFIED_OK;
	}

	public static void insert(TextEdit parent, TextEdit edit) {
		if (!parent.hasChildren()) {
			parent.addChild(edit);
			return;
		}
		TextEdit[] children= parent.getChildren();
		// First dive down to find the right parent.
		for (int i= 0; i < children.length; i++) {
			TextEdit child= children[i];
			if (covers(child, edit)) {
				insert(child, edit);
				return;
			}
		}
		// We have the right parent. Now check if some of the children have to
		// be moved under the new edit since it is covering it.
		for (int i= children.length - 1; i >= 0; i--) {
			TextEdit child= children[i];
			if (covers(edit, child)) {
				parent.removeChild(i);
				edit.addChild(child);
			}
		}
		parent.addChild(edit);
	}

	private static boolean covers(TextEdit thisEdit, TextEdit otherEdit) {
		if (thisEdit.getLength() == 0) {
			return false;
		}

		int thisOffset= thisEdit.getOffset();
		int thisEnd= thisEdit.getExclusiveEnd();
		if (otherEdit.getLength() == 0) {
			int otherOffset= otherEdit.getOffset();
			return thisOffset <= otherOffset && otherOffset < thisEnd;
		} else {
			int otherOffset= otherEdit.getOffset();
			int otherEnd= otherEdit.getExclusiveEnd();
			return thisOffset <= otherOffset && otherEnd <= thisEnd;
		}
	}
}
