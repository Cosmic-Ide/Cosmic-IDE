/*******************************************************************************
 * Copyright (c) 2004, 2021 IBM Corporation and others.
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
 *     Microsoft Corporation - read formatting options from the compilation unit
 *******************************************************************************/
package org.eclipse.jdt.core.dom.rewrite;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.ChildPropertyDescriptor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.SimplePropertyDescriptor;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.internal.compiler.parser.RecoveryScannerData;
import org.eclipse.jdt.internal.core.dom.rewrite.ASTRewriteAnalyzer;
import org.eclipse.jdt.internal.core.dom.rewrite.LineInformation;
import org.eclipse.jdt.internal.core.dom.rewrite.NodeInfoStore;
import org.eclipse.jdt.internal.core.dom.rewrite.NodeRewriteEvent;
import org.eclipse.jdt.internal.core.dom.rewrite.RewriteEventStore;
import org.eclipse.jdt.internal.core.dom.rewrite.TrackedNodePosition;
import org.eclipse.jdt.internal.core.dom.rewrite.RewriteEventStore.CopySourceInfo;
import org.eclipse.jdt.internal.core.dom.rewrite.RewriteEventStore.PropertyLocation;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;

/**
 * Infrastructure for modifying code by describing changes to AST nodes.
 * The AST rewriter collects descriptions of modifications to nodes and
 * translates these descriptions into text edits that can then be applied to
 * the original source. The key thing is that this is all done without actually
 * modifying the original AST, which has the virtue of allowing one to entertain
 * several alternate sets of changes on the same AST (e.g., for calculating multiple
 * quick fix proposals). The rewrite infrastructure tries to generate minimal
 * text changes, preserve existing comments and indentation, and follow code
 * formatter settings.
 * <p>
 * The following code snippet illustrated usage of this class:
 * </p>
 * <pre>
 * Document document = new Document("import java.util.List;\nclass X {}\n");
 * ASTParser parser = ASTParser.newParser(AST.JLS3);
 * parser.setSource(document.get().toCharArray());
 * CompilationUnit cu = (CompilationUnit) parser.createAST(null);
 * AST ast = cu.getAST();
 * ImportDeclaration id = ast.newImportDeclaration();
 * id.setName(ast.newName(new String[] {"java", "util", "Set"}));
 * ASTRewrite rewriter = ASTRewrite.create(ast);
 * TypeDeclaration td = (TypeDeclaration) cu.types().get(0);
 * ITrackedNodePosition tdLocation = rewriter.track(td);
 * ListRewrite lrw = rewriter.getListRewrite(cu, CompilationUnit.IMPORTS_PROPERTY);
 * lrw.insertLast(id, null);
 * TextEdit edits = rewriter.rewriteAST(document, null);
 * UndoEdit undo = null;
 * try {
 *     undo = edits.apply(document);
 * } catch(MalformedTreeException e) {
 *     e.printStackTrace();
 * } catch(BadLocationException e) {
 *     e.printStackTrace();
 * }
 * assert "import java.util.List;\nimport java.util.Set;\nclass X {}\n".equals(document.get());
 * // tdLocation.getStartPosition() and tdLocation.getLength()
 * // are new source range for "class X {}" in document.get()
 * </pre>
 * <p>
 * If you are sure you never have to explore multiple alternate changes and you never need
 * to create {@link #createCopyTarget(ASTNode) copies} or {@link #createStringPlaceholder(String, int) string placeholders},
 * then you can also try {@link CompilationUnit#recordModifications()} instead.
 * </p>
 * <p>
 * <code>ASTRewrite</code> cannot rewrite (non-Javadoc) comments from
 * {@link CompilationUnit#getCommentList()}, since those comment nodes
 * are not part of the normal node hierarchy.
 * </p>
 * <p>
 * This class is not intended to be subclassed.
 * </p>
 * @since 3.0
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @noextend This class is not intended to be subclassed by clients.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class ASTRewrite {
	/** root node for the rewrite: Only nodes under this root are accepted */
	private final AST ast;

	private final RewriteEventStore eventStore;
	private final NodeInfoStore nodeStore;

	/**
	 * Target source range computer; null means uninitialized;
	 * lazy initialized to <code>new TargetSourceRangeComputer()</code>.
	 * @since 3.1
	 */
	private TargetSourceRangeComputer targetSourceRangeComputer = null;

	/**
	 * Primary field used in representing rewrite properties efficiently.
	 * If <code>null</code>, this rewrite has no properties.
	 * If a {@link String}, this is the name of this rewrite's sole property,
	 * and <code>property2</code> contains its value.
	 * If a {@link Map}, this is the table of property name-value
	 * mappings.
	 * Initially <code>null</code>.
	 *
	 * @see #property2
	 */
	private Object property1 = null;

	/**
	 * Auxiliary field used in representing rewrite properties efficiently.
	 *
	 * @see #property1
	 */
	private Object property2 = null;

	/**
	 * Creates a new instance for describing manipulations of
	 * the given AST.
	 *
	 * @param ast the AST whose nodes will be rewritten
	 * @return the new rewriter instance
	 */
	public static ASTRewrite create(AST ast) {
		return new ASTRewrite(ast);
	}

	/**
	 * Internal constructor. Creates a new instance for the given AST.
	 * Clients should use {@link #create(AST)} to create instances.
	 *
	 * @param ast the AST being rewritten
	 */
	protected ASTRewrite(AST ast) {
		this.ast= ast;
		this.eventStore= new RewriteEventStore();
		this.nodeStore= new NodeInfoStore(ast);
	}

	/**
	 * Returns the AST the rewrite was set up on.
	 *
	 * @return the AST the rewrite was set up on
	 */
	public final AST getAST() {
		return this.ast;
	}

	/**
	 * Internal method. Returns the internal event store.
	 * Clients should not use.
	 * @return Returns the internal event store. Clients should not use.
	 */
	protected final RewriteEventStore getRewriteEventStore() {
		return this.eventStore;
	}

	/**
	 * Internal method. Returns the internal node info store.
	 * Clients should not use.
	 * @return Returns the internal info store. Clients should not use.
	 */
	protected final NodeInfoStore getNodeStore() {
		return this.nodeStore;
	}

	/**
	 * Converts all modifications recorded by this rewriter
	 * into an object representing the corresponding text
	 * edits to the given document containing the original source
	 * code. The document itself is not modified.
	 * <p>
	 * For nodes in the original that are being replaced or deleted,
	 * this rewriter computes the adjusted source ranges
	 * by calling {@link TargetSourceRangeComputer#computeSourceRange(ASTNode) getExtendedSourceRangeComputer().computeSourceRange(node)}.
	 * </p>
	 * <p>
	 * Calling this methods does not discard the modifications
	 * on record. Subsequence modifications are added to the ones
	 * already on record. If this method is called again later,
	 * the resulting text edit object will accurately reflect
	 * the net cumulative effect of all those changes.
	 * </p>
	 *
	 * @param document original document containing source code
	 * @param options the table of formatter options
	 * (key type: <code>String</code>; value type: <code>String</code>);
	 * or <code>null</code> to use the standard global options
	 * {@link JavaCore#getOptions() JavaCore.getOptions()}
	 * @return text edit object describing the changes to the
	 * document corresponding to the changes recorded by this rewriter
	 * @throws IllegalArgumentException An <code>IllegalArgumentException</code>
	 * is thrown if the document passed does not correspond to the AST that is rewritten.
	 */
	public TextEdit rewriteAST(IDocument document, Map options) throws IllegalArgumentException {
		if (document == null) {
			throw new IllegalArgumentException();
		}

		ASTNode rootNode= getRootNode();
		if (rootNode == null) {
			return new MultiTextEdit(); // no changes
		}

		char[] content= document.get().toCharArray();
		LineInformation lineInfo= LineInformation.create(document);
		String lineDelim= TextUtilities.getDefaultLineDelimiter(document);

		ASTNode astRoot= rootNode.getRoot();
		List commentNodes= astRoot instanceof CompilationUnit ? ((CompilationUnit) astRoot).getCommentList() : null;
		Map currentOptions = options == null ? JavaCore.getOptions() : options;
		return internalRewriteAST(content, lineInfo, lineDelim, commentNodes, currentOptions, rootNode, (RecoveryScannerData)((CompilationUnit) astRoot).getStatementsRecoveryData());
	}

	/**
	 * Converts all modifications recorded by this rewriter into an object representing the the corresponding text
	 * edits to the source of a {@link ITypeRoot} from which the AST was created from.
	 * The type root's source itself is not modified by this method call.
	 * <p>
	 * Important: This API can only be used if the modified AST has been created from a
	 * {@link ITypeRoot} with source. That means {@link ASTParser#setSource(ICompilationUnit)},
	 * {@link ASTParser#setSource(IClassFile)} or {@link ASTParser#setSource(ITypeRoot)}
	 * has been used when initializing the {@link ASTParser}. A {@link IllegalArgumentException} is thrown
	 * otherwise. An {@link IllegalArgumentException} is also thrown when the type roots buffer does not correspond
	 * anymore to the AST. Use {@link #rewriteAST(IDocument, Map)} for all ASTs created from other content.
	 * </p>
	 * <p>
	 * For nodes in the original that are being replaced or deleted,
	 * this rewriter computes the adjusted source ranges
	 * by calling {@link TargetSourceRangeComputer#computeSourceRange(ASTNode) getExtendedSourceRangeComputer().computeSourceRange(node)}.
	 * </p>
	 * <p>
	 * Calling this methods does not discard the modifications
	 * on record. Subsequence modifications are added to the ones
	 * already on record. If this method is called again later,
	 * the resulting text edit object will accurately reflect
	 * the net cumulative effect of all those changes.
	 * </p>
	 *
	 * @return text edit object describing the changes to the
	 * document corresponding to the changes recorded by this rewriter
	 * @throws JavaModelException A {@link JavaModelException} is thrown when
	 * the underlying compilation units buffer could not be accessed.
	 * @throws IllegalArgumentException An {@link IllegalArgumentException}
	 * is thrown if the document passed does not correspond to the AST that is rewritten.
	 *
	 * @since 3.2
	 */
	public TextEdit rewriteAST() throws JavaModelException, IllegalArgumentException {
		ASTNode rootNode= getRootNode();
		if (rootNode == null) {
			return new MultiTextEdit(); // no changes
		}

		ASTNode root= rootNode.getRoot();
		if (!(root instanceof CompilationUnit)) {
			throw new IllegalArgumentException("This API can only be used if the AST is created from a compilation unit or class file"); //$NON-NLS-1$
		}
		CompilationUnit astRoot= (CompilationUnit) root;
		ITypeRoot typeRoot = astRoot.getTypeRoot();
		if (typeRoot == null || typeRoot.getBuffer() == null) {
			throw new IllegalArgumentException("This API can only be used if the AST is created from a compilation unit or class file"); //$NON-NLS-1$
		}

		char[] content= typeRoot.getBuffer().getCharacters();
		LineInformation lineInfo= LineInformation.create(astRoot);
		String lineDelim= typeRoot.findRecommendedLineSeparator();
		Map options= typeRoot instanceof ICompilationUnit ? ((ICompilationUnit) typeRoot).getOptions(true) :
			typeRoot.getJavaProject().getOptions(true);

		return internalRewriteAST(content, lineInfo, lineDelim, astRoot.getCommentList(), options, rootNode, (RecoveryScannerData)astRoot.getStatementsRecoveryData());
	}

	private TextEdit internalRewriteAST(char[] content, LineInformation lineInfo, String lineDelim, List commentNodes, Map options, ASTNode rootNode, RecoveryScannerData recoveryScannerData) {
		TextEdit result= new MultiTextEdit();
		//validateASTNotModified(rootNode);

		TargetSourceRangeComputer sourceRangeComputer= getExtendedSourceRangeComputer();
		this.eventStore.prepareMovedNodes(sourceRangeComputer);

		ASTRewriteAnalyzer visitor= new ASTRewriteAnalyzer(content, lineInfo, lineDelim, result, this.eventStore, this.nodeStore, commentNodes, options, sourceRangeComputer, recoveryScannerData);
		rootNode.accept(visitor); // throws IllegalArgumentException

		this.eventStore.revertMovedNodes();
		return result;
	}

	private ASTNode getRootNode() {
		ASTNode node= null;
		int start= -1;
		int end= -1;

		for (Iterator iter= getRewriteEventStore().getChangeRootIterator(); iter.hasNext();) {
			ASTNode curr= (ASTNode) iter.next();
			if (!RewriteEventStore.isNewNode(curr)) {
				int currStart= curr.getStartPosition();
				int currEnd= currStart + curr.getLength();
				if (node == null || currStart < start && currEnd > end) {
					start= currStart;
					end= currEnd;
					node= curr;
				} else if (currStart < start) {
					start= currStart;
				} else if (currEnd > end) {
					end= currEnd;
				}
			}
		}
		if (node != null) {
			int currStart= node.getStartPosition();
			int currEnd= currStart + node.getLength();
			while (start < currStart || end > currEnd) { // go up until a node covers all
				node= node.getParent();
				currStart= node.getStartPosition();
				currEnd= currStart + node.getLength();
			}
			ASTNode parent= node.getParent(); // go up until a parent has different range
			while (parent != null && parent.getStartPosition() == node.getStartPosition() && parent.getLength() == node.getLength()) {
				node= parent;
				parent= node.getParent();
			}
		}
		return node;
	}

	/*
	private void validateASTNotModified(ASTNode root) throws IllegalArgumentException {
		GenericVisitor isModifiedVisitor= new GenericVisitor() {
			protected boolean visitNode(ASTNode node) {
				if ((node.getFlags() & ASTNode.ORIGINAL) == 0) {
					throw new IllegalArgumentException("The AST that is rewritten must not be modified."); //$NON-NLS-1$
				}
				return true;
			}
		};
		root.accept(isModifiedVisitor);
	}
	*/

	/**
	 * Removes the given node from its parent in this rewriter. The AST itself
     * is not actually modified in any way; rather, the rewriter just records
     * a note that this node should not be there.
	 *
	 * @param node the node being removed. The node can either be an original node in the AST
	 * or (since 3.4) a new node already inserted or used as replacement in this AST rewriter.
	 * @param editGroup the edit group in which to collect the corresponding
	 * text edits, or <code>null</code> if ungrouped
	 * @throws IllegalArgumentException if the node is null, or if the node is not
	 * part of this rewriter's AST, or if the described modification is invalid
	 * (such as removing a required node)
	 */
	public final void remove(ASTNode node, TextEditGroup editGroup) {
		if (node == null) {
			throw new IllegalArgumentException();
		}

		StructuralPropertyDescriptor property;
		ASTNode parent;
		if (RewriteEventStore.isNewNode(node)) { // remove a new node, bug 164862
			PropertyLocation location= this.eventStore.getPropertyLocation(node, RewriteEventStore.NEW);
			if (location != null) {
				property= location.getProperty();
				parent= location.getParent();
			} else {
				throw new IllegalArgumentException("Node is not part of the rewriter's AST"); //$NON-NLS-1$
			}
		} else {
			property= node.getLocationInParent();
			parent= node.getParent();
		}

		if (property.isChildListProperty()) {
			getListRewrite(parent, (ChildListPropertyDescriptor) property).remove(node, editGroup);
		} else {
			set(parent, property, null, editGroup);
		}
	}

	/**
	 * Replaces the given node in this rewriter. The replacement node
	 * must either be brand new (not part of the original AST) or a placeholder
	 * node (for example, one created by {@link #createCopyTarget(ASTNode)}
	 * or {@link #createStringPlaceholder(String, int)}). The AST itself
     * is not actually modified in any way; rather, the rewriter just records
     * a note that this node has been replaced.
	 *
	 * @param node the node being replaced. The node can either be an original node in the AST
	 * or (since 3.4) a new node already inserted or used as replacement in this AST rewriter.
	 * @param replacement the replacement node, or <code>null</code> if no
	 * replacement
	 * @param editGroup the edit group in which to collect the corresponding
	 * text edits, or <code>null</code> if ungrouped
	 * @throws IllegalArgumentException if the node is null, or if the node is not part
	 * of this rewriter's AST, or if the replacement node is not a new node (or
     * placeholder), or if the described modification is otherwise invalid
	 */
	public final void replace(ASTNode node, ASTNode replacement, TextEditGroup editGroup) {
		if (node == null) {
			throw new IllegalArgumentException();
		}

		StructuralPropertyDescriptor property;
		ASTNode parent;
		if (RewriteEventStore.isNewNode(node)) { // replace a new node, bug 164862
			PropertyLocation location= this.eventStore.getPropertyLocation(node, RewriteEventStore.NEW);
			if (location != null) {
				property= location.getProperty();
				parent= location.getParent();
			} else {
				throw new IllegalArgumentException("Node is not part of the rewriter's AST"); //$NON-NLS-1$
			}
		} else {
			property= node.getLocationInParent();
			parent= node.getParent();
		}

		if (property.isChildListProperty()) {
			getListRewrite(parent, (ChildListPropertyDescriptor) property).replace(node, replacement, editGroup);
		} else {
			set(parent, property, replacement, editGroup);
		}
	}

	/**
	 * Sets the given property of the given node. If the given property is a child
     * property, the value must be a replacement node that is either be brand new
     * (not part of the original AST) or a placeholder node (for example, one
     * created by {@link #createCopyTarget(ASTNode)}
	 * or {@link #createStringPlaceholder(String, int)}); or it must be
	 * <code>null</code>, indicating that the child should be deleted.
	 * If the given property is a simple property, the value must be the new
	 * value (primitive types must be boxed) or <code>null</code>.
     * The AST itself is not actually modified in any way; rather, the rewriter
     * just records a note that this node has been changed in the specified way.
	 *
	 * @param node the node
	 * @param property the node's property; either a simple property or a child property
	 * @param value the replacement child or new value, or <code>null</code> if none
	 * @param editGroup the edit group in which to collect the corresponding
	 * text edits, or <code>null</code> if ungrouped
	 * @throws IllegalArgumentException if the node or property is null, or if the node
	 * is not part of this rewriter's AST, or if the property is not a node property,
	 * or if the described modification is invalid
	 */
	public final void set(ASTNode node, StructuralPropertyDescriptor property, Object value, TextEditGroup editGroup) {
		if (node == null || property == null) {
			throw new IllegalArgumentException();
		}
		validateIsCorrectAST(node);
		validatePropertyType(property, value);
		validateIsPropertyOfNode(property, node);

		NodeRewriteEvent nodeEvent= this.eventStore.getNodeEvent(node, property, true);
		nodeEvent.setNewValue(value);
		if (editGroup != null) {
			this.eventStore.setEventEditGroup(nodeEvent, editGroup);
		}
	}

	/**
	 * Returns the value of the given property as managed by this rewriter. If the property
	 * has been removed, <code>null</code> is returned. If it has been replaced, the replacing value
	 * is returned. If the property has not been changed yet, the original value is returned.
	 * <p>
	 * For child list properties use {@link ListRewrite#getRewrittenList()} to get access to the
	 * rewritten nodes in a list. </p>
	 *
	 * @param node the node
	 * @param property the node's property
	 * @return the value of the given property as managed by this rewriter
	 *
	 * @since 3.2
	 */
	public Object get(ASTNode node, StructuralPropertyDescriptor property) {
		if (node == null || property == null) {
			throw new IllegalArgumentException();
		}
		if (property.isChildListProperty()) {
			throw new IllegalArgumentException("Use the list rewriter to access nodes in a list"); //$NON-NLS-1$
		}
		return this.eventStore.getNewValue(node, property);
	}

	/**
	 * Creates and returns a new rewriter for describing modifications to the
	 * given list property of the given node.
	 *
	 * @param node the node
	 * @param property the node's property; the child list property
	 * @return a new list rewriter object
	 * @throws IllegalArgumentException if the node or property is null, or if the node
	 * is not part of this rewriter's AST, or if the property is not a node property,
	 * or if the described modification is invalid
	 */
	public final ListRewrite getListRewrite(ASTNode node, ChildListPropertyDescriptor property) {
		if (node == null || property == null) {
			throw new IllegalArgumentException();
		}

		validateIsCorrectAST(node);
		validateIsListProperty(property);
		validateIsPropertyOfNode(property, node);

		return new ListRewrite(this, node, property);
	}

	/**
	 * Returns the value of the named property of this rewrite, or <code>null</code> if none.
	 *
	 * @param propertyName the property name
	 * @return the property value, or <code>null</code> if none
	 * @see #setProperty(String,Object)
	 * @throws IllegalArgumentException if the given property name is <code>null</code>
	 * @since 3.7
	 */
	public final Object getProperty(String propertyName) {
		if (propertyName == null) {
			throw new IllegalArgumentException();
		}
		if (this.property1 == null) {
			// rewrite has no properties at all
			return null;
		}
		if (this.property1 instanceof String) {
			// rewrite has only a single property
			if (propertyName.equals(this.property1)) {
				return this.property2;
			} else {
				return null;
			}
		}
		// otherwise rewrite has table of properties
		Map m = (Map) this.property1;
		return m.get(propertyName);
	}

	/**
	 * Returns an object that tracks the source range of the given node
	 * across the rewrite to its AST. Upon return, the result object reflects
	 * the given node's current source range in the AST. After
	 * <code>rewrite</code> is called, the result object is updated to
	 * reflect the given node's source range in the rewritten AST.
	 *
	 * @param node the node to track
	 * @return an object that tracks the source range of <code>node</code>
	 * @throws IllegalArgumentException if the node is null, or if the node
	 * is not part of this rewriter's AST, or if the node is already being
	 * tracked
	 */
	public final ITrackedNodePosition track(ASTNode node) {
		if (node == null) {
			throw new IllegalArgumentException();
		}
		TextEditGroup group= this.eventStore.getTrackedNodeData(node);
		if (group == null) {
			group= new TextEditGroup("internal"); //$NON-NLS-1$
			this.eventStore.setTrackedNodeData(node, group);
		}
		return new TrackedNodePosition(group, node);
	}

	private void validateIsExistingNode(ASTNode node) {
		if (node.getStartPosition() == -1) {
			throw new IllegalArgumentException("Node is not an existing node"); //$NON-NLS-1$
		}
	}

	private void validateIsCorrectAST(ASTNode node) {
		if (node.getAST() != getAST()) {
			throw new IllegalArgumentException("Node is not inside the AST"); //$NON-NLS-1$
		}
	}

	private void validateIsListProperty(StructuralPropertyDescriptor property) {
		if (!property.isChildListProperty()) {
			String message= property.getId() + " is not a list property"; //$NON-NLS-1$
			throw new IllegalArgumentException(message);
		}
	}

	private void validateIsPropertyOfNode(StructuralPropertyDescriptor property, ASTNode node) {
		if (!property.getNodeClass().isInstance(node)) {
			String message= property.getId() + " is not a property of type " + node.getClass().getName(); //$NON-NLS-1$
			throw new IllegalArgumentException(message);
		}
	}

	private void validatePropertyType(StructuralPropertyDescriptor prop, Object value) {
		if (prop.isChildListProperty()) {
			String message = "Can not modify a list property, use getListRewrite()"; //$NON-NLS-1$
			throw new IllegalArgumentException(message);
		}
		if (!RewriteEventStore.DEBUG) {
			return;
		}

		if (value == null) {
			if (prop.isSimpleProperty() && ((SimplePropertyDescriptor) prop).isMandatory()
					|| prop.isChildProperty() && ((ChildPropertyDescriptor) prop).isMandatory()) {
				String message = "Can not remove property " + prop.getId(); //$NON-NLS-1$
				throw new IllegalArgumentException(message);
			}

		} else {
			Class valueType;
			if (prop.isSimpleProperty()) {
				SimplePropertyDescriptor p = (SimplePropertyDescriptor) prop;
				valueType = p.getValueType();
				if (valueType == int.class) {
					valueType = Integer.class;
				} else if (valueType == boolean.class) {
					valueType = Boolean.class;
				}
			} else {
				ChildPropertyDescriptor p = (ChildPropertyDescriptor) prop;
				valueType = p.getChildType();
			}
			if (!valueType.isAssignableFrom(value.getClass())) {
				String message = value.getClass().getName() + " is not a valid type for " + prop.getNodeClass().getName() //$NON-NLS-1$
						+ " property '" + prop.getId() + '\''; //$NON-NLS-1$
				throw new IllegalArgumentException(message);
			}
		}
	}

	/**
	 * Creates and returns a placeholder node for a source string that is to be inserted into
	 * the output document at the position corresponding to the placeholder.
	 * The string will be inserted without being reformatted beyond correcting
	 * the indentation level. The placeholder node can either be inserted as new or
	 * used to replace an existing node.
	 *
	 * @param code the string to be inserted; lines should should not have extra indentation
	 * @param nodeType the ASTNode type that corresponds to the passed code.
	 * @return the new placeholder node
	 * @throws IllegalArgumentException if the code is null, or if the node
	 * type is invalid
	 */
	public final ASTNode createStringPlaceholder(String code, int nodeType) {
		if (code == null) {
			throw new IllegalArgumentException();
		}
		ASTNode placeholder= getNodeStore().newPlaceholderNode(nodeType);
		if (placeholder == null) {
			throw new IllegalArgumentException("String placeholder is not supported for type" + nodeType); //$NON-NLS-1$
		}

		getNodeStore().markAsStringPlaceholder(placeholder, code);
		return placeholder;
	}

	/**
	 * Creates and returns a node that represents a sequence of nodes.
	 * Each of the given nodes must be either be brand new (not part of the original AST), or
	 * a placeholder node (for example, one created by {@link #createCopyTarget(ASTNode)}
	 * or {@link #createStringPlaceholder(String, int)}), or another group node.
	 * The type of the returned node is unspecified. The returned node can be used
	 * to replace an existing node (or as an element of another group node).
	 * When the document is rewritten, the source code for each of the given nodes is
	 * inserted, in order, into the output document at the position corresponding to the
	 * group (indentation is adjusted).
	 *
	 * @param targetNodes the nodes to go in the group
	 * @return the new group node
	 * @throws IllegalArgumentException if the targetNodes is <code>null</code> or empty
	 * @since 3.1
	 */
	public final ASTNode createGroupNode(ASTNode[] targetNodes) {
		if (targetNodes == null || targetNodes.length == 0) {
			throw new IllegalArgumentException();
		}
		Block res= getNodeStore().createCollapsePlaceholder();
		ListRewrite listRewrite= getListRewrite(res, Block.STATEMENTS_PROPERTY);
		for (int i= 0; i < targetNodes.length; i++) {
			listRewrite.insertLast(targetNodes[i], null);
		}
		return res;
	}


	private ASTNode createTargetNode(ASTNode node, boolean isMove) {
		if (node == null) {
			throw new IllegalArgumentException();
		}
		validateIsExistingNode(node);
		validateIsCorrectAST(node);
		CopySourceInfo info= getRewriteEventStore().markAsCopySource(node.getParent(), node.getLocationInParent(), node, isMove);

		ASTNode placeholder= getNodeStore().newPlaceholderNode(node.getNodeType());
		if (placeholder == null) {
			throw new IllegalArgumentException("Creating a target node is not supported for nodes of type" + node.getClass().getName()); //$NON-NLS-1$
		}
		getNodeStore().markAsCopyTarget(placeholder, info);

		return placeholder;
	}

	/**
	 * Creates and returns a placeholder node for a true copy of the given node.
	 * The placeholder node can either be inserted as new or used to replace an
	 * existing node. When the document is rewritten, a copy of the source code
	 * for the given node is inserted into the output document at the position
	 * corresponding to the placeholder (indentation is adjusted).
	 *
	 * @param node the node to create a copy placeholder for
	 * @return the new placeholder node
	 * @throws IllegalArgumentException if the node is null, or if the node
	 * is not part of this rewriter's AST
	 */
	public final ASTNode createCopyTarget(ASTNode node) {
		return createTargetNode(node, false);
	}

	/**
	 * Creates and returns a placeholder node for the new locations of the given node.
	 * After obtaining a placeholder, the node must be removed or replaced.
	 * The placeholder node can either be inserted as new or used to replace an
	 * existing node. The placeholder must be used somewhere in the AST and must not be dropped
	 * by subsequent modifications. When the document is rewritten, the source code for the given
	 * node is inserted into the output document at the position corresponding to the
	 * placeholder (indentation is adjusted).
	 *
	 * @param node the node to create a move placeholder for
	 * @return the new placeholder node
	 * @throws IllegalArgumentException if the node is null, or if the node
	 * is not part of this rewriter's AST
	 */
	public final ASTNode createMoveTarget(ASTNode node) {
		return createTargetNode(node, true);
	}

	/**
	 * Returns the extended source range computer for this AST rewriter.
	 * The default value is a <code>new TargetSourceRangeComputer()</code>.
	 *
	 * @return an extended source range computer
	 * @since 3.1
	 * @see #setTargetSourceRangeComputer(TargetSourceRangeComputer)
	 */
	public final TargetSourceRangeComputer getExtendedSourceRangeComputer() {
		if (this.targetSourceRangeComputer == null) {
			// lazy initialize
			this.targetSourceRangeComputer = new TargetSourceRangeComputer();
		}
		return this.targetSourceRangeComputer;
	}

	/**
	 * Sets the named property of this rewrite to the given value,
	 * or to <code>null</code> to clear it.
	 * <p>
	 * Clients should employ property names that are sufficiently unique
	 * to avoid inadvertent conflicts with other clients that might also be
	 * setting properties on the same rewrite.
	 * </p>
	 * <p>
	 * Note that modifying a property is not considered a modification to the
	 * AST itself. This is to allow clients to decorate existing rewrites with
	 * their own properties without jeopardizing certain things (like the
	 * validity of bindings), which rely on the underlying tree remaining static.
	 * </p>
	 *
	 * @param propertyName the property name
	 * @param data the new property value, or <code>null</code> if none
	 * @see #getProperty(String)
	 * @throws IllegalArgumentException if the given property name is <code>null</code>
	 * @since 3.7
	 */
	public final void setProperty(String propertyName, Object data) {
		if (propertyName == null) {
			throw new IllegalArgumentException();
		}
		if (this.property1 == null) {
			// rewrite has no properties at all
			if (data == null) {
				// rewrite already knows this
				return;
			}
			// rewrite gets its fist property
			this.property1 = propertyName;
			this.property2 = data;
			return;
		}
		if (this.property1 instanceof String) {
			// rewrite has only a single property
			if (propertyName.equals(this.property1)) {
				// we're in luck
				if (data == null) {
					// just delete last property
					this.property1 = null;
					this.property2 = null;
				} else {
					this.property2 = data;
				}
				return;
			}
			if (data == null) {
				// we already know this
				return;
			}
			// rewrite already has one property - getting its second
			// convert to more flexible representation
			Map m = new HashMap(3);
			m.put(this.property1, this.property2);
			m.put(propertyName, data);
			this.property1 = m;
			this.property2 = null;
			return;
		}
		// rewrite has two or more properties
		Map m = (Map) this.property1;
		if (data == null) {
			m.remove(propertyName);
			// check for just one property left
			if (m.size() == 1) {
				// convert to more efficient representation
				Map.Entry[] entries = (Map.Entry[]) m.entrySet().toArray(new Map.Entry[1]);
				this.property1 = entries[0].getKey();
				this.property2 = entries[0].getValue();
			}
			return;
		} else {
			m.put(propertyName, data);
			// still has two or more properties
			return;
		}
	}

	/**
	 * Sets a custom target source range computer for this AST rewriter. This is advanced feature to modify how
	 * comments are associated with nodes, which should be done only in special cases.
	 *
	 * @param computer a target source range computer,
	 * or <code>null</code> to restore the default value of
	 * <code>new TargetSourceRangeComputer()</code>
	 * @since 3.1
	 * @see #getExtendedSourceRangeComputer()
	 */
	public final void setTargetSourceRangeComputer(TargetSourceRangeComputer computer) {
		// if computer==null, rely on lazy init code in getExtendedSourceRangeComputer()
		this.targetSourceRangeComputer = computer;
	}

	/**
	 * Returns a string suitable for debugging purposes (only).
	 *
	 * @return a debug string
	 */
	@Override
	public String toString() {
		StringBuilder buf= new StringBuilder();
		buf.append("Events:\n"); //$NON-NLS-1$
		// be extra careful of uninitialized or mangled instances
		if (this.eventStore != null) {
			buf.append(this.eventStore.toString());
		}
		return buf.toString();
	}
}
