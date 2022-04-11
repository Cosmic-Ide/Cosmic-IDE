/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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

package org.eclipse.jdt.core.dom;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.internal.compiler.parser.Scanner;
import org.eclipse.jdt.internal.compiler.util.Util;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.TextEdit;

/**
 * Java compilation unit AST node type. This is the type of the root of an AST.
 * In JLS9 and later, this node can also contain a ModuleDeclaration (with a completely different grammar).
 * <p>
 * The source range for this type of node is ordinarily the entire source file,
 * including leading and trailing whitespace and comments.
 * </p>
 * <pre>
 * CompilationUnit:
 *     OrdinaryCompilationUnit
 *     ModularCompilationUnit
 *
 * OrdinaryCompilationUnit:
 *     [ PackageDeclaration ]
 *         { ImportDeclaration }
 *         { TypeDeclaration | EnumDeclaration | AnnotationTypeDeclaration | <b>;</b> }
 *
 * ModularCompilationUnit:
 *     {ImportDeclaration}
 *         ModuleDeclaration
 * </pre>
 *
 * @since 2.0
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @noextend This class is not intended to be subclassed by clients.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class CompilationUnit extends ASTNode {

	/**
	 * Canonical empty list of messages.
	 */
	private static final Message[] EMPTY_MESSAGES = new Message[0];

	/**
	 * Canonical empty list of problems.
	 */
	private static final IProblem[] EMPTY_PROBLEMS = new IProblem[0];

	/**
	 * The "imports" structural property of this node type (element type: {@link ImportDeclaration}).
	 *
	 * @since 3.0
	 */
	public static final ChildListPropertyDescriptor IMPORTS_PROPERTY =
		new ChildListPropertyDescriptor(CompilationUnit.class, "imports", ImportDeclaration.class, NO_CYCLE_RISK); //$NON-NLS-1$

	/**
	 * The "package" structural property of this node type (child type: {@link PackageDeclaration}).
	 *
	 * @since 3.0
	 */
	public static final ChildPropertyDescriptor PACKAGE_PROPERTY =
		new ChildPropertyDescriptor(CompilationUnit.class, "package", PackageDeclaration.class, OPTIONAL, NO_CYCLE_RISK); //$NON-NLS-1$

	/**
	 * The "module" structural property of this node type (child type: {@link ModuleDeclaration}) (added in JLS9 API).
	 *
	 * @since 3.14
	 */
	public static final ChildPropertyDescriptor MODULE_PROPERTY =
		new ChildPropertyDescriptor(CompilationUnit.class, "module", ModuleDeclaration.class, OPTIONAL, NO_CYCLE_RISK); //$NON-NLS-1$

	/**
	 * A list of property descriptors (element type:
	 * {@link StructuralPropertyDescriptor}),
	 * or null if uninitialized.
	 * @since 3.0
	 */
	private static final List PROPERTY_DESCRIPTORS;

	/**
	 * A list of property descriptors (element type:
	 * {@link StructuralPropertyDescriptor}),
	 * or null if uninitialized.
	 * @since 3.14
	 */
	private static final List PROPERTY_DESCRIPTORS_9_0;

	/**
	 * The "types" structural property of this node type (element type: {@link AbstractTypeDeclaration}).
	 *
	 * @since 3.0
	 */
	public static final ChildListPropertyDescriptor TYPES_PROPERTY =
		new ChildListPropertyDescriptor(CompilationUnit.class, "types", AbstractTypeDeclaration.class, CYCLE_RISK); //$NON-NLS-1$

	static {
		List properyList = new ArrayList(4);
		createPropertyList(CompilationUnit.class, properyList);
		addProperty(PACKAGE_PROPERTY, properyList);
		addProperty(IMPORTS_PROPERTY, properyList);
		addProperty(TYPES_PROPERTY, properyList);
		PROPERTY_DESCRIPTORS = reapPropertyList(properyList);

		properyList = new ArrayList(5);
		createPropertyList(CompilationUnit.class, properyList);
		addProperty(PACKAGE_PROPERTY, properyList);
		addProperty(IMPORTS_PROPERTY, properyList);
		addProperty(TYPES_PROPERTY, properyList);
		addProperty(MODULE_PROPERTY, properyList);
		PROPERTY_DESCRIPTORS_9_0 = reapPropertyList(properyList);
	}

	/**
	 * Returns a list of structural property descriptors for this node type.
	 * Clients must not modify the result.
	 *
	 * @param apiLevel the API level; one of the
	 * <code>AST.JLS*</code> constants

	 * @return a list of property descriptors (element type:
	 * {@link StructuralPropertyDescriptor})
	 * @since 3.0
	 */
	public static List propertyDescriptors(int apiLevel) {
		if (apiLevel < AST.JLS9_INTERNAL)
			return PROPERTY_DESCRIPTORS;
		else
			return PROPERTY_DESCRIPTORS_9_0;
	}

	/**
	 * The comment mapper, or <code>null</code> if none;
	 * initially <code>null</code>.
	 * @since 3.0
	 */
	private DefaultCommentMapper commentMapper = null;

	/**
	 * The Java type root (an <code>org.eclipse.jdt.core.ICompilationUnit</code> or an <code>org.eclipse.jdt.core.IClassFile</code>)
	 * this compilation unit was created from, or <code>null</code> if it was not created from a Java type root.
	 */
	private ITypeRoot typeRoot = null;

	/**
	 * The list of import declarations in textual order order;
	 * initially none (elementType: <code>ImportDeclaration</code>).
	 */
	private ASTNode.NodeList imports =
		new ASTNode.NodeList(IMPORTS_PROPERTY);

	/**
	 * Line end table. If <code>lineEndTable[i] == p</code> then the
	 * line number <code>i+1</code> ends at character position
	 * <code>p</code>. Except for the last line, the positions are that
	 * of the last character of the line delimiter.
	 * For example, the source string <code>A\nB\nC</code> has
	 * line end table {1, 3} (if \n is one character).
	 */
	private int[] lineEndTable = Util.EMPTY_INT_ARRAY;

	/**
	 * Messages reported by the compiler during parsing or name resolution.
	 */
	private Message[] messages;

	/**
	 * The comment list (element type: {@link Comment},
	 * or <code>null</code> if none; initially <code>null</code>.
	 * @since 3.0
	 */
	private List optionalCommentList = null;

	/**
	 * The comment table, or <code>null</code> if none; initially
	 * <code>null</code>. This array is the storage underlying
	 * the <code>optionalCommentList</code> ArrayList.
	 * @since 3.0
	 */
	Comment[] optionalCommentTable = null;

	/**
	 * The package declaration, or <code>null</code> if none; initially
	 * <code>null</code>.
	 */
	private PackageDeclaration optionalPackageDeclaration = null;

	/**
	 * The module declaration, or <code>null</code> if none; initially
	 * <code>null</code>.
	 */
	private ModuleDeclaration module = null;
	/**
	 * Problems reported by the compiler during parsing or name resolution.
	 */
	private IProblem[] problems = EMPTY_PROBLEMS;

	/**
	 * Internal data used to perform statements recovery.
	 */
	private Object statementsRecoveryData;

	/**
	 * The list of type declarations in textual order order;
	 * initially none (elementType: <code>AbstractTypeDeclaration</code>)
	 */
	private ASTNode.NodeList types =
		new ASTNode.NodeList(TYPES_PROPERTY);

	/**
	 * Creates a new AST node for a compilation owned by the given AST.
	 * The compilation unit initially has no package declaration, no
	 * import declarations, and no type declarations.
	 * <p>
	 * N.B. This constructor is package-private; all subclasses must be
	 * declared in the same package; clients are unable to declare
	 * additional subclasses.
	 * </p>
	 *
	 * @param ast the AST that is to own this node
	 */
	CompilationUnit(AST ast) {
		super(ast);
	}

	@Override
	void accept0(ASTVisitor visitor) {
		boolean visitChildren = visitor.visit(this);
		if (visitChildren) {
			// visit children in normal left to right reading order
			if (this.ast.apiLevel >= AST.JLS9_INTERNAL) {
				acceptChild(visitor, getModule());
			}
			acceptChild(visitor, getPackage());
			acceptChildren(visitor, this.imports);
			acceptChildren(visitor, this.types);
		}
		visitor.endVisit(this);
	}

	@Override
	ASTNode clone0(AST target) {
		CompilationUnit result = new CompilationUnit(target);
		// n.b do not copy line number table or messages
		result.setSourceRange(getStartPosition(), getLength());
		if (this.ast.apiLevel >= AST.JLS9_INTERNAL) {
			result.setModule((ModuleDeclaration) ASTNode.copySubtree(target, getModule()));
		}
		result.setPackage(
			(PackageDeclaration) ASTNode.copySubtree(target, getPackage()));
		result.imports().addAll(ASTNode.copySubtrees(target, imports()));
		result.types().addAll(ASTNode.copySubtrees(target, types()));
		return result;
	}


	/**
	 * Returns the column number corresponding to the given source character
	 * position in the original source string. Column number are zero-based.
	 * Return <code>-1</code> if it is beyond the valid range or <code>-2</code>
	 * if the column number information is unknown.
	 *
	 * @param position a 0-based character position, possibly
	 *   negative or out of range
	 * @return the 0-based column number, or <code>-1</code> if the character
	 *    position does not correspond to a source line in the original
	 *    source file or <code>-2</code> if column number information is unknown for this
	 *    compilation unit
	 * @see ASTParser
	 * @since 3.2
	 */
	public int getColumnNumber(final int position) {
		if (this.lineEndTable == null) return -2;
		final int line = getLineNumber(position);
		if (line == -1) {
			return -1;
		}
		if (line == 1) {
			if (position >= getStartPosition() + getLength()) return -1;
			return position;
		}
		// length is different from 0
		int length = this.lineEndTable.length;
		// -1 to for one-based to zero-based conversion.
		// -1, again, to get previous line.
		final int previousLineOffset = this.lineEndTable[line - 2];
		 // previousLineOffset + 1 is the first character of the current line
		final int offsetForLine = previousLineOffset + 1;
		final int currentLineEnd = line == length + 1 ? getStartPosition() + getLength() - 1 :	this.lineEndTable[line - 1];
		if (offsetForLine > currentLineEnd) {
			return -1;
		} else {
			return position - offsetForLine;
		}
	}

	/**
	 * Finds the corresponding AST node in the given compilation unit from
	 * which the given binding originated. Returns <code>null</code> if the
	 * binding does not correspond to any node in this compilation unit.
	 * This method always returns <code>null</code> if bindings were not requested
	 * when this AST was built.
	 * <p>
	 * The following table indicates the expected node type for the various
	 * different kinds of bindings:
	 * <ul>
	 * <li>package - a <code>PackageDeclaration</code></li>
	 * <li>class or interface - a <code>TypeDeclaration</code> or a
	 *    <code>AnonymousClassDeclaration</code> (for anonymous classes)</li>
	 * <li>primitive type - none</li>
	 * <li>array type - none</li>
	 * <li>field - a <code>VariableDeclarationFragment</code> in a
	 *    <code>FieldDeclaration</code> </li>
	 * <li>local variable - a <code>SingleVariableDeclaration</code>, or
	 *    a <code>VariableDeclarationFragment</code> in a
	 *    <code>VariableDeclarationStatement</code> or
	 *    <code>VariableDeclarationExpression</code></li>
	 * <li>method - a <code>MethodDeclaration</code> </li>
	 * <li>constructor - a <code>MethodDeclaration</code> </li>
     * <li>annotation type - an <code>AnnotationTypeDeclaration</code></li>
     * <li>annotation type member - an <code>AnnotationTypeMemberDeclaration</code></li>
     * <li>enum type - an <code>EnumDeclaration</code></li>
     * <li>enum constant - an <code>EnumConstantDeclaration</code></li>
     * <li>type variable - a <code>TypeParameter</code></li>
     * <li>capture binding - none</li>
     * <li>annotation binding - an <code>Annotation</code></li>
     * <li>member value pair binding - an <code>MemberValuePair</code>,
     *      or <code>null</code> if it represents a default value or a single member value</li>
	 * </ul>
	 * <p>
     * For parameterized or raw type bindings, the declaring node is
     * that of the corresponding generic type. And for parameterized or raw
     * method bindings, the declaring node is that of the corresponding
     * generic method.
	 * </p>
	 * <p>
	 * Each call to {@link ASTParser#createAST(org.eclipse.core.runtime.IProgressMonitor)} with a request for bindings
	 * gives rise to separate universe of binding objects. This method always returns
	 * <code>null</code> when the binding object comes from a different AST.
	 * Use <code>findDeclaringNode(binding.getKey())</code> when the binding comes
	 * from a different AST.
	 * </p>
	 *
	 * @param binding the binding
	 * @return the corresponding node where the given binding is declared,
	 * or <code>null</code> if the binding does not correspond to a node in this
	 * compilation unit or if bindings were not requested when this AST was built
	 * @see #findDeclaringNode(String)
	 */
	public ASTNode findDeclaringNode(IBinding binding) {
		return this.ast.getBindingResolver().findDeclaringNode(binding);
	}

	/**
	 * Finds the corresponding AST node in the given compilation unit from
	 * which the binding with the given key originated. Returns
	 * <code>null</code> if the corresponding node cannot be determined.
	 * This method always returns <code>null</code> if bindings were not requested
	 * when this AST was built.
	 * <p>
	 * The following table indicates the expected node type for the various
	 * different kinds of binding keys:
	 * <ul>
	 * <li></li>
	 * <li>package - a <code>PackageDeclaration</code></li>
	 * <li>class or interface - a <code>TypeDeclaration</code> or a
	 *    <code>AnonymousClassDeclaration</code> (for anonymous classes)</li>
	 * <li>primitive type - none</li>
	 * <li>array type - none</li>
	 * <li>field - a <code>VariableDeclarationFragment</code> in a
	 *    <code>FieldDeclaration</code> </li>
	 * <li>local variable - a <code>SingleVariableDeclaration</code>, or
	 *    a <code>VariableDeclarationFragment</code> in a
	 *    <code>VariableDeclarationStatement</code> or
	 *    <code>VariableDeclarationExpression</code></li>
	 * <li>method - a <code>MethodDeclaration</code> </li>
	 * <li>constructor - a <code>MethodDeclaration</code> </li>
     * <li>annotation type - an <code>AnnotationTypeDeclaration</code></li>
     * <li>annotation type member - an <code>AnnotationTypeMemberDeclaration</code></li>
     * <li>enum type - an <code>EnumDeclaration</code></li>
     * <li>enum constant - an <code>EnumConstantDeclaration</code></li>
	 * <li>type variable - a <code>TypeParameter</code></li>
     * <li>capture binding - none</li>
	 * </ul>
     * For parameterized or raw type bindings, the declaring node is
     * that of the corresponding generic type. And for parameterized or raw
     * method bindings, the declaring node is that of the corresponding
     * generic method.
	 *
	 * @param key the binding key, or <code>null</code>
	 * @return the corresponding node where a binding with the given
	 * key is declared, or <code>null</code> if the key is <code>null</code>
	 * or if the key does not correspond to a node in this compilation unit
	 * or if bindings were not requested when this AST was built
	 * @see IBinding#getKey()
	 * @since 2.1
	 */
	public ASTNode findDeclaringNode(String key) {
		return this.ast.getBindingResolver().findDeclaringNode(key);
	}

	/**
	 * Returns a list of the comments encountered while parsing
	 * this compilation unit.
	 * <p>
	 * Since the Java language allows comments to appear most anywhere
	 * in the source text, it is problematic to locate comments in relation
	 * to the structure of an AST. The one exception is doc comments
	 * which, by convention, immediately precede type, field, and
	 * method declarations; these comments are located in the AST
	 * by {@link  BodyDeclaration#getJavadoc BodyDeclaration.getJavadoc}.
	 * Other comments do not show up in the AST. The table of comments
	 * is provided for clients that need to find the source ranges of
	 * all comments in the original source string. It includes entries
	 * for comments of all kinds (line, block, and doc), arranged in order
	 * of increasing source position.
	 * </p>
	 * <p>
	 * Note on comment parenting: The {@link ASTNode#getParent() getParent()}
	 * of a doc comment associated with a body declaration is the body
	 * declaration node; for these comment nodes
	 * {@link ASTNode#getRoot() getRoot()} will return the compilation unit
	 * (assuming an unmodified AST) reflecting the fact that these nodes
	 * are property located in the AST for the compilation unit.
	 * However, for other comment nodes, {@link ASTNode#getParent() getParent()}
	 * will return <code>null</code>, and {@link ASTNode#getRoot() getRoot()}
	 * will return the comment node itself, indicating that these comment nodes
	 * are not directly connected to the AST for the compilation unit. The
	 * {@link Comment#getAlternateRoot Comment.getAlternateRoot}
	 * method provides a way to navigate from a comment to its compilation
	 * unit.
	 * </p>
	 * <p>
	 * A note on visitors: The only comment nodes that will be visited when
	 * visiting a compilation unit are the doc comments parented by body
	 * declarations. To visit all comments in normal reading order, iterate
	 * over the comment table and call {@link ASTNode#accept(ASTVisitor) accept}
	 * on each element.
	 * </p>
	 * <p>
	 * Clients cannot modify the resulting list.
	 * </p>
	 *
	 * @return an unmodifiable list of comments in increasing order of source
	 * start position (element type: {@link Comment}, or <code>null</code> if comment information
	 * for this compilation unit is not available
	 * @see ASTParser
	 * @since 3.0
	 */
	public List getCommentList() {
		return this.optionalCommentList;
	}

	/**
	 * Returns the internal comment mapper.
	 *
	 * @return the comment mapper, or <code>null</code> if none.
	 * @since 3.0
	 */
	DefaultCommentMapper getCommentMapper() {
		return this.commentMapper;
	}

	/**
	 * Returns the extended source length of the given node. Unlike
	 * {@link ASTNode#getStartPosition()} and {@link ASTNode#getLength()},
	 * the extended source range may include comments and whitespace
	 * immediately before or after the normal source range for the node.
	 *
	 * @param node the node
	 * @return a (possibly 0) length, or <code>0</code>
	 *    if no source position information is recorded for this node
	 * @see #getExtendedStartPosition(ASTNode)
	 * @since 3.0
	 */
	public int getExtendedLength(ASTNode node) {
		if (node == null) {
			throw new IllegalArgumentException();
		}
		if (this.commentMapper == null || node.getAST() != getAST()) {
			// fall back: use best info available
			return node.getLength();
		} else {
			return this.commentMapper.getExtendedLength(node);
		}
	}

	/**
	 * Returns the extended start position of the given node. Unlike
	 * {@link ASTNode#getStartPosition()} and {@link ASTNode#getLength()},
	 * the extended source range may include comments and whitespace
	 * immediately before or after the normal source range for the node.
	 *
	 * @param node the node
	 * @return the 0-based character index, or <code>-1</code>
	 *    if no source position information is recorded for this node
	 * @see #getExtendedLength(ASTNode)
	 * @since 3.0
	 */
	public int getExtendedStartPosition(ASTNode node) {
		if (node == null) {
			throw new IllegalArgumentException();
		}
		if (this.commentMapper == null || node.getAST() != getAST()) {
			// fall back: use best info available
			return node.getStartPosition();
		} else {
			return this.commentMapper.getExtendedStartPosition(node);
		}
	}

	/**
	 * The Java element (an <code>org.eclipse.jdt.core.ICompilationUnit</code> or an <code>org.eclipse.jdt.core.IClassFile</code>)
	 * this compilation unit was created from, or <code>null</code> if it was not created from a Java element.
	 *
	 * @return the Java element this compilation unit was created from, or <code>null</code> if none
	 * @since 3.1
	 * @see #getTypeRoot()
	 */
	public IJavaElement getJavaElement() {
		return this.typeRoot;
	}

	/**
	 * Returns the list of messages reported by the compiler during the parsing
	 * or the type checking of this compilation unit. This list might be a subset of
	 * errors detected and reported by a Java compiler.
	 * <p>
	 * This list of messages is suitable for simple clients that do little
	 * more than log the messages or display them to the user. Clients that
	 * need further details should call <code>getProblems</code> to get
	 * compiler problem objects.
	 * </p>
	 *
	 * @return the list of messages, possibly empty
	 * @see #getProblems()
	 * @see ASTParser
	 */
	public Message[] getMessages() {
		if (this.messages == null) {
			int problemLength = this.problems.length;
			if (problemLength == 0) {
				this.messages = EMPTY_MESSAGES;
			} else {
				this.messages = new Message[problemLength];
				for (int i = 0; i < problemLength; i++) {
					IProblem problem = this.problems[i];
					int start = problem.getSourceStart();
					int end = problem.getSourceEnd();
					this.messages[i] = new Message(problem.getMessage(), start, end - start + 1);
				}
			}
		}
		return this.messages;
	}

	@Override
	final int getNodeType0() {
		return COMPILATION_UNIT;
	}

	/**
	 * Returns the node for the module declaration of this compilation
	 * unit, or <code>null</code> if this compilation unit is not a module info.
	 *
	 * @return the module declaration node, or <code>null</code> if none
	 * @exception UnsupportedOperationException if this operation is used below JLS9
	 * @since 3.14
	 */
	public ModuleDeclaration getModule() {
		unsupportedBelow9();
		return this.module;
	}

	/**
	 * Returns the node for the package declaration of this compilation
	 * unit, or <code>null</code> if this compilation unit is in the
	 * default package.
	 *
	 * @return the package declaration node, or <code>null</code> if none
	 */
	public PackageDeclaration getPackage() {
		return this.optionalPackageDeclaration;
	}

	/**
	 * Given a line number and column number, returns the corresponding
	 * position in the original source string.
	 * Returns -2 if no line number information is available for this
	 * compilation unit.
	 * Returns the total size of the source string if <code>line</code>
	 * is greater than the actual number lines in the unit.
	 * Returns -1 if <code>column</code> is less than 0,
	 * or the position of the last character of the line if <code>column</code>
	 * is beyond the legal range, or the given line number is less than one.
	 *
	 * @param line the one-based line number
	 * @param column the zero-based column number
	 * @return the 0-based character position in the source string;
	 * <code>-2</code> if line/column number information is not known
	 * for this compilation unit or <code>-1</code> the inputs are not valid
	 * @since 3.2
	 */
	 public int getPosition(int line, int column) {
		if (this.lineEndTable == null) return -2;
		if (line < 1 || column < 0) return -1;
		int length;
		if ((length = this.lineEndTable.length) == 0) {
			if (line != 1) return -1;
			return column >= getStartPosition() + getLength() ? -1 : column;
		}
		if (line == 1) {
			final int endOfLine = this.lineEndTable[0];
			return column > endOfLine ? -1 : column;
		} else if( line > length + 1 ) {
			// greater than the number of lines in the source string.
			return -1;
		}
		// -1 to for one-based to zero-based conversion.
		// -1, again, to get previous line.
		final int previousLineOffset = this.lineEndTable[line - 2];
		 // previousLineOffset + 1 is the first character of the current line
		final int offsetForLine = previousLineOffset + 1;
		final int currentLineEnd = line == length + 1 ? getStartPosition() + getLength() - 1 : this.lineEndTable[line-1];
		if ((offsetForLine + column) > currentLineEnd) {
			return -1;
		} else {
			return offsetForLine + column;
		}
	}

	/**
	 * Returns the list of detailed problem reports noted by the compiler
	 * during the parsing or the type checking of this compilation unit. This
	 * list might be a subset of errors detected and reported by a Java
	 * compiler.
	 * <p>
	 * Simple clients that do little more than log the messages or display
	 * them to the user should probably call <code>getMessages</code> instead.
	 * </p>
	 *
	 * @return the list of detailed problem objects, possibly empty
	 * @see #getMessages()
	 * @see ASTParser
	 * @since 2.1
	 */
	public IProblem[] getProblems() {
		return this.problems;
	}

	/**
	 * Internal method
	 *
	 * This method return internal data used to perform statements recovery.
	 *
	 * @return internal data used to perform statements recovery.
	 *
	 * @noreference This method is not intended to be referenced by clients.
	 * @since 3.5
	 */
	public Object getStatementsRecoveryData() {
		return this.statementsRecoveryData;
	}

	/**
	 * The Java type root (a {@link org.eclipse.jdt.core.ICompilationUnit compilation unit} or a {@link org.eclipse.jdt.core.IClassFile class file})
	 * this compilation unit was created from, or <code>null</code> if it was not created from a Java type root.
	 *
	 * @return the Java type root this compilation unit was created from, or <code>null</code> if none
	 * @since 3.3
	 */
	public ITypeRoot getTypeRoot() {
		return this.typeRoot;
	}

	/**
	 * Returns the live list of nodes for the import declarations of this
	 * compilation unit, in order of appearance.
	 *
	 * @return the live list of import declaration nodes
	 *    (elementType: {@link ImportDeclaration})
	 */
	public List imports() {
		return this.imports;
	}

	/**
	 * Return the index in the whole comments list {@link #getCommentList() }
	 * of the first leading comments associated with the given node.
	 *
	 * @param node the node
	 * @return 0-based index of first leading comment or -1 if node has no associated
	 * 	comment before its start position.
	 * @since 3.2
	 */
	public int firstLeadingCommentIndex(ASTNode node) {
		if (node == null) {
			throw new IllegalArgumentException();
		}
		if (this.commentMapper == null || node.getAST() != getAST()) {
			return -1;
		}
		return this.commentMapper.firstLeadingCommentIndex(node);
	}

	/**
	 * Return the index in the whole comments list {@link #getCommentList() }
	 * of the last trailing comments associated with the given node.
	 *
	 * @param node the node
	 * @return 0-based index of last trailing comment or -1 if node has no
	 * 	associated comment after its end position.
	 * @since 3.2
	 */
	public int lastTrailingCommentIndex(ASTNode node) {
		if (node == null) {
			throw new IllegalArgumentException();
		}
		if (this.commentMapper == null || node.getAST() != getAST()) {
			return -1;
		}
		return this.commentMapper.lastTrailingCommentIndex(node);
	}

	/**
	 * Initializes the internal comment mapper with the given
	 * scanner.
	 *
	 * @param scanner the scanner
	 * @since 3.0
	 */
	void initCommentMapper(Scanner scanner) {
		this.commentMapper = new DefaultCommentMapper(this.optionalCommentTable);
		this.commentMapper.initialize(this, scanner);
	}

	@Override
	final List internalGetChildListProperty(ChildListPropertyDescriptor property) {
		if (property == IMPORTS_PROPERTY) {
			return imports();
		}
		if (property == TYPES_PROPERTY) {
			return types();
		}
		// allow default implementation to flag the error
		return super.internalGetChildListProperty(property);
	}

	@Override
	final ASTNode internalGetSetChildProperty(ChildPropertyDescriptor property, boolean get, ASTNode child) {
		if (property == MODULE_PROPERTY) {
			if (get) {
				return getModule();
			} else {
				setModule((ModuleDeclaration) child);
				return null;
			}
		}
		if (property == PACKAGE_PROPERTY) {
			if (get) {
				return getPackage();
			} else {
				setPackage((PackageDeclaration) child);
				return null;
			}
		}
		// allow default implementation to flag the error
		return super.internalGetSetChildProperty(property, get, child);
	}

	@Override
	final List internalStructuralPropertiesForType(int apiLevel) {
		return propertyDescriptors(apiLevel);
	}

	/**
	 * Returns the line number corresponding to the given source character
	 * position in the original source string. The initial line of the
	 * compilation unit is numbered 1, and each line extends through the
	 * last character of the end-of-line delimiter. The very last line extends
	 * through the end of the source string and has no line delimiter.
	 * For example, the source string <code>class A\n{\n}</code> has 3 lines
	 * corresponding to inclusive character ranges [0,7], [8,9], and [10,10].
	 * Returns 1 for a character position that does not correspond to any
	 * source line, or if no line number information is available for this
	 * compilation unit.
	 *
	 * @param position a 0-based character position, possibly
	 *   negative or out of range
	 * @return the 1-based line number, or <code>1</code> if the character
	 *    position does not correspond to a source line in the original
	 *    source file or if line number information is not known for this
	 *    compilation unit
	 * @deprecated Use getLineNumber(int) instead. Be careful to handle the negative values.
	 * @see ASTParser
	 * @see #getLineNumber(int)
	 */
	public int lineNumber(int position) {
		int lineNumber = getLineNumber(position);
		return lineNumber < 1 ? 1 : lineNumber;
	}

	/**
	 * Returns the line number corresponding to the given source character
	 * position in the original source string. The initial line of the
	 * compilation unit is numbered 1, and each line extends through the
	 * last character of the end-of-line delimiter. The very last line extends
	 * through the end of the source string and has no line delimiter.
	 * For example, the source string <code>class A\n{\n}</code> has 3 lines
	 * corresponding to inclusive character ranges [0,7], [8,9], and [10,10].
	 * Returns -1 for a character position that does not correspond to any
	 * source line, or -2 if no line number information is available for this
	 * compilation unit.
	 *
	 * @param position a 0-based character position, possibly
	 *   negative or out of range
	 * @return the 1-based line number, or <code>-1</code> if the character
	 *    position does not correspond to a source line in the original
	 *    source file or <code>-2</code> if line number information is not known for this
	 *    compilation unit
	 * @see ASTParser
	 * @since 3.2
	 */
	public int getLineNumber(int position) {
		if (this.lineEndTable == null) return -2;
		int length;
		if ((length = this.lineEndTable.length) == 0) {
			if (position >= getStartPosition() + getLength()) {
				return -1;
			}
			return 1;
		}
		int low = 0;
		if (position < 0) {
			// position illegal
			return -1;
		}
		if (position <= this.lineEndTable[low]) {
			// before the first line delimiter
			return 1;
		}
		// assert position > lineEndTable[low+1]  && low == 0
		int hi = length - 1;
		if (position > this.lineEndTable[hi]) {
			// position beyond the last line separator
			if (position >= getStartPosition() + getLength()) {
				// this is beyond the end of the source length
				return -1;
			} else {
				return length + 1;
			}
		}
		// assert lineEndTable[low]  < position <= lineEndTable[hi]
		// && low == 0 && hi == length - 1 && low < hi

		// binary search line end table
		while (true) {
			// invariant lineEndTable[low] < position <= lineEndTable[hi]
			// && 0 <= low < hi <= length - 1
			// reducing measure hi - low
			if (low + 1 == hi) {
				// assert lineEndTable[low] < position <= lineEndTable[low+1]
				// position is on line low+1 (line number is low+2)
				return low + 2;
			}
			// assert hi - low >= 2, so average is truly in between
			int mid = low + (hi - low) / 2;
			// assert 0 <= low < mid < hi <= length - 1
			if (position <= this.lineEndTable[mid]) {
				// assert lineEndTable[low] < position <= lineEndTable[mid]
				// && 0 <= low < mid < hi <= length - 1
				hi = mid;
			} else {
				// position > lineEndTable[mid]
				// assert lineEndTable[mid] < position <= lineEndTable[hi]
				// && 0 <= low < mid < hi <= length - 1
				low = mid;
			}
			// in both cases, invariant reachieved with reduced measure
		}
	}

	@Override
	int memSize() {
		int size = BASE_NODE_SIZE + 8 * 4;
		if (this.lineEndTable != null) {
			size += HEADERS + 4 * this.lineEndTable.length;
		}
		if (this.optionalCommentTable != null) {
			size += HEADERS + 4 * this.optionalCommentTable.length;
		}
		// ignore the space taken up by optionalCommentList
		return size;
	}

	/**
	 * Enables the recording of changes to this compilation
	 * unit and its descendants. The compilation unit must have
	 * been created by {@link ASTParser} and still be in
	 * its original state. Once recording is on,
	 * arbitrary changes to the subtree rooted at this compilation
	 * unit are recorded internally. Once the modification has
	 * been completed, call {@link #rewrite(IDocument, Map)} to get an object
	 * representing the corresponding edits to the original
	 * source code string.
	 * <p>
	 * Note that this way of manipulating an AST only allows a single line of modifications,
	 * and it lacks important functionality like
	 * {@link ASTRewrite#createStringPlaceholder(String, int) string placeholders} and support for
	 * {@link ASTRewrite#createCopyTarget(ASTNode) copying} nodes including comments and formatting.
	 * </p>
	 * <p>
	 * To future-proof your code, <em>consider using an external {@link ASTRewrite} instead</em>,
	 * which doesn't suffer from these limitations and allows to modify an AST in a non-destructive way.
	 * </p>
	 *
	 * @exception IllegalArgumentException if this compilation unit is
	 * marked as unmodifiable, or if this compilation unit has already
	 * been tampered with, or recording has already been enabled
	 * @see ASTRewrite
	 * @since 3.0
	 */
	public void recordModifications() {
		getAST().recordModifications(this);
	}

	/**
	 * Converts all modifications recorded for this compilation
	 * unit into an object representing the corresponding text
	 * edits to the given document containing the original source
	 * code for this compilation unit.
	 * <p>
	 * The compilation unit must have been created by
	 * {@link ASTParser} from the source code string in the
	 * given document, and recording must have been turned
	 * on with a prior call to {@link #recordModifications()}
	 * while the AST was still in its original state.
	 * </p>
	 * <p>
	 * Calling this methods does not discard the modifications
	 * on record. Subsequence modifications made to the AST
	 * are added to the ones already on record. If this method
	 * is called again later, the resulting text edit object will
	 * accurately reflect the net cumulative effect of all those
	 * changes.
	 * </p>
	 *
	 * @param document original document containing source code
	 * for this compilation unit
	 * @param options the table of formatter options
	 * (key type: <code>String</code>; value type: <code>String</code>);
	 * or <code>null</code> to use the standard global options
	 * {@link org.eclipse.jdt.core.JavaCore#getOptions() JavaCore.getOptions()}.
	 * @return text edit object describing the changes to the
	 * document corresponding to the recorded AST modifications
	 * @exception IllegalArgumentException if the document passed is
	 * <code>null</code> or does not correspond to this AST
	 * @exception IllegalStateException if <code>recordModifications</code>
	 * was not called to enable recording
	 * @see #recordModifications()
	 * @since 3.0
	 */
	public TextEdit rewrite(IDocument document, Map options) {
		return getAST().rewrite(document, options);
	}

	/**
	 * Sets the list of the comments encountered while parsing
	 * this compilation unit.
	 *
	 * @param commentTable a list of comments in increasing order
	 * of source start position, or <code>null</code> if comment
	 * information for this compilation unit is not available
	 * @exception IllegalArgumentException if the comment table is
	 * not in increasing order of source position
	 * @see #getCommentList()
	 * @see ASTParser
	 * @since 3.0
	 */
	void setCommentTable(Comment[] commentTable) {
		// double check table to ensure that all comments have
		// source positions and are in strictly increasing order
		if (commentTable == null) {
			this.optionalCommentList = null;
			this.optionalCommentTable = null;
		} else {
			int nextAvailablePosition = 0;
			for (int i = 0; i < commentTable.length; i++) {
				Comment comment = commentTable[i];
				if (comment == null) {
					throw new IllegalArgumentException();
				}
				int start = comment.getStartPosition();
				int length = comment.getLength();
				if (start < 0 || length < 0 || start < nextAvailablePosition) {
					throw new IllegalArgumentException();
				}
				nextAvailablePosition = comment.getStartPosition() + comment.getLength();
			}
			this.optionalCommentTable = commentTable;
			List commentList = Arrays.asList(commentTable);
			// protect the list from further modification
			this.optionalCommentList = Collections.unmodifiableList(commentList);
		}
	}

	/**
	 * Sets the Java type root (a {@link org.eclipse.jdt.core.ICompilationUnit compilation unit} or a {@link org.eclipse.jdt.core.IClassFile class file})
	 * this compilation unit was created from, or <code>null</code> if it was not created from a Java type root.
	 *
	 * @param typeRoot the Java type root this compilation unit was created from
	 */
	void setTypeRoot(ITypeRoot typeRoot) {
		this.typeRoot = typeRoot;
	}

	/**
	 * Sets the line end table for this compilation unit.
	 * If <code>lineEndTable[i] == p</code> then line number <code>i+1</code>
	 * ends at character position <code>p</code>. Except for the last line, the
	 * positions are that of (the last character of) the line delimiter.
	 * For example, the source string <code>A\nB\nC</code> has
	 * line end table {1, 3, 4}.
	 *
	 * @param lineEndTable the line end table
	 */
	void setLineEndTable(int[] lineEndTable) {
		if (lineEndTable == null) {
			throw new NullPointerException();
		}
		// alternate root is *not* considered a structural property
		// but we protect them nevertheless
		checkModifiable();
		this.lineEndTable = lineEndTable;
	}

	/**
	 * Sets or clears the module declaration of this compilation unit
	 * node to the given module declaration node.
	 *
	 * @param module the new module declaration node, or
	 *   <code>null</code> if this compilation unit does not have a module
	 * @exception IllegalArgumentException if:
	 * <ul>
	 * <li>the node belongs to a different AST</li>
	 * <li>the node already has a parent</li>
	 * </ul>
	 * @exception UnsupportedOperationException if this operation is used below JLS9
	 * @since 3.14
	 */
	public void setModule(ModuleDeclaration module) {
		unsupportedBelow9();
		ASTNode oldChild = this.module;
		preReplaceChild(oldChild, module, MODULE_PROPERTY);
		this.module = module;
		postReplaceChild(oldChild, module, MODULE_PROPERTY);
	}

	/**
	 * Sets or clears the package declaration of this compilation unit
	 * node to the given package declaration node.
	 *
	 * @param pkgDecl the new package declaration node, or
	 *   <code>null</code> if this compilation unit does not have a package
	 *   declaration (that is in the default package)
	 * @exception IllegalArgumentException if:
	 * <ul>
	 * <li>the node belongs to a different AST</li>
	 * <li>the node already has a parent</li>
	 * </ul>
	 */
	public void setPackage(PackageDeclaration pkgDecl) {
		ASTNode oldChild = this.optionalPackageDeclaration;
		preReplaceChild(oldChild, pkgDecl, PACKAGE_PROPERTY);
		this.optionalPackageDeclaration = pkgDecl;
		postReplaceChild(oldChild, pkgDecl, PACKAGE_PROPERTY);
	}


	/**
	 * Sets the array of problems reported by the compiler during the parsing or
	 * name resolution of this compilation unit.
	 *
	 * @param problems the list of problems
	 */
	void setProblems(IProblem[] problems) {
		if (problems == null) {
			throw new IllegalArgumentException();
		}
		this.problems = problems;
	}

	/**
	 * Internal method
	 *
	 * Sets internal data used to perform statements recovery.
	 * @param data
	 *
	 * @since 3.5
	 */
	void setStatementsRecoveryData(Object data) {
		this.statementsRecoveryData = data;
	}

	@Override
	final boolean subtreeMatch0(ASTMatcher matcher, Object other) {
		// dispatch to correct overloaded match method
		return matcher.match(this, other);
	}

	@Override
	int treeSize() {
		int size = memSize();
		if (this.module != null) {
			size += getModule().treeSize();
		}
		if (this.optionalPackageDeclaration != null) {
			size += getPackage().treeSize();
		}
		size += this.imports.listSize();
		size += this.types.listSize();
		// include disconnected comments
		if (this.optionalCommentList != null) {
			for (int i = 0; i < this.optionalCommentList.size(); i++) {
				Comment comment = (Comment) this.optionalCommentList.get(i);
				if (comment != null && comment.getParent() == null) {
					size += comment.treeSize();
				}
			}
		}
		return size;
	}

	/**
	 * Returns the live list of nodes for the top-level type declarations of this
	 * compilation unit, in order of appearance.
     * <p>
     * Note that in JLS3, the types may include both enum declarations
     * and annotation type declarations introduced in J2SE 5.
     * For JLS2, the elements are always <code>TypeDeclaration</code>.
     * </p>
	 *
	 * @return the live list of top-level type declaration
	 *    nodes (element type: {@link AbstractTypeDeclaration})
	 */
	public List types() {
		return this.types;
	}
}

