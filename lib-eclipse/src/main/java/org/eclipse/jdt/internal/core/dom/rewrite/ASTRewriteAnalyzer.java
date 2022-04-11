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
package org.eclipse.jdt.internal.core.dom.rewrite;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.TargetSourceRangeComputer;
import org.eclipse.jdt.core.dom.rewrite.TargetSourceRangeComputer.SourceRange;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.jdt.core.formatter.IndentManipulation;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.parser.RecoveryScanner;
import org.eclipse.jdt.internal.compiler.parser.RecoveryScannerData;
import org.eclipse.jdt.internal.compiler.parser.Scanner;
import org.eclipse.jdt.internal.compiler.parser.ScannerHelper;
import org.eclipse.jdt.internal.compiler.parser.TerminalTokens;
import org.eclipse.jdt.internal.compiler.util.Util;
import org.eclipse.jdt.internal.core.dom.rewrite.ASTRewriteFormatter.BlockContext;
import org.eclipse.jdt.internal.core.dom.rewrite.ASTRewriteFormatter.NodeMarker;
import org.eclipse.jdt.internal.core.dom.rewrite.ASTRewriteFormatter.Prefix;
import org.eclipse.jdt.internal.core.dom.rewrite.NodeInfoStore.CopyPlaceholderData;
import org.eclipse.jdt.internal.core.dom.rewrite.NodeInfoStore.StringPlaceholderData;
import org.eclipse.jdt.internal.core.dom.rewrite.RewriteEventStore.CopySourceInfo;
import org.eclipse.jdt.internal.core.dom.util.DOMASTUtil;
import org.eclipse.text.edits.CopySourceEdit;
import org.eclipse.text.edits.CopyTargetEdit;
import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.MoveSourceEdit;
import org.eclipse.text.edits.MoveTargetEdit;
import org.eclipse.text.edits.RangeMarker;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;


/**
 * Infrastructure to support code modifications. Existing code must stay untouched, new code
 * added with correct formatting, moved code left with the user's formatting / comments.
 * Idea:
 * - Get the AST for existing code
 * - Describe changes
 * - This visitor analyzes the changes or annotations and generates text edits
 * (text manipulation API) that describe the required code changes.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public final class ASTRewriteAnalyzer extends ASTVisitor {

	/** @deprecated using deprecated code */
	private static final ChildPropertyDescriptor INTERNAL_ARRAY_COMPONENT_TYPE_PROPERTY = ArrayType.COMPONENT_TYPE_PROPERTY;

	/** @deprecated using deprecated code */
	private static final SimplePropertyDescriptor INTERNAL_FIELD_MODIFIERS_PROPERTY = FieldDeclaration.MODIFIERS_PROPERTY;

	/** @deprecated using deprecated code */
	private static final SimplePropertyDescriptor INTERNAL_INITIALIZER_MODIFIERS_PROPERTY3 = Initializer.MODIFIERS_PROPERTY;

	/** @deprecated using deprecated code */
	private static final SimplePropertyDescriptor INTERNAL_METHOD_MODIFIERS_PROPERTY2 = MethodDeclaration.MODIFIERS_PROPERTY;

	/** @deprecated using deprecated code */
	private static final ChildPropertyDescriptor INTERNAL_METHOD_RETURN_TYPE_PROPERTY = MethodDeclaration.RETURN_TYPE_PROPERTY;

	/** @deprecated using deprecated code */
	private static final SimplePropertyDescriptor INTERNAL_METHOD_EXTRA_DIMENSIONS_PROPERTY = MethodDeclaration.EXTRA_DIMENSIONS_PROPERTY;

	/** @deprecated using deprecated code */
	private static final ChildListPropertyDescriptor INTERNAL_METHOD_THROWN_EXCEPTIONS_PROPERTY = MethodDeclaration.THROWN_EXCEPTIONS_PROPERTY;

	/** @deprecated using deprecated code */
	private static final SimplePropertyDescriptor INTERNAL_TYPE_MODIFIERS_PROPERTY = TypeDeclaration.MODIFIERS_PROPERTY;

	/** @deprecated using deprecated code */
	private static final ChildPropertyDescriptor INTERNAL_TYPE_SUPERCLASS_PROPERTY = TypeDeclaration.SUPERCLASS_PROPERTY;

	/** @deprecated using deprecated code */
	private static final ChildListPropertyDescriptor INTERNAL_TYPE_SUPER_INTERFACES_PROPERTY = TypeDeclaration.SUPER_INTERFACES_PROPERTY;

	/** @deprecated using deprecated code */
	private static final ChildPropertyDescriptor INTERNAL_CIC_NAME_PROPERTY = ClassInstanceCreation.NAME_PROPERTY;

	/** @deprecated using deprecated code */
	private static final SimplePropertyDescriptor INTERNAL_FRAGMENT_EXTRA_DIMENSIONS_PROPERTY = VariableDeclarationFragment.EXTRA_DIMENSIONS_PROPERTY;

	/** @deprecated using deprecated code */
	private static final ChildPropertyDescriptor INTERNAL_TDS_TYPE_DECLARATION_PROPERTY = TypeDeclarationStatement.TYPE_DECLARATION_PROPERTY;

	/** @deprecated using deprecated code */
	private static final SimplePropertyDescriptor INTERNAL_VARIABLE_MODIFIERS_PROPERTY = SingleVariableDeclaration.MODIFIERS_PROPERTY;

	/** @deprecated using deprecated code */
	private static final SimplePropertyDescriptor INTERNAL_VARIABLE_EXTRA_DIMENSIONS_PROPERTY = SingleVariableDeclaration.EXTRA_DIMENSIONS_PROPERTY;

	/** @deprecated using deprecated code */
	private static final SimplePropertyDescriptor INTERNAL_VDE_MODIFIERS_PROPERTY = VariableDeclarationExpression.MODIFIERS_PROPERTY;

	/** @deprecated using deprecated code */
	private static final SimplePropertyDescriptor INTERNAL_VDS_MODIFIERS_PROPERTY = VariableDeclarationStatement.MODIFIERS_PROPERTY;

	/** @deprecated using deprecated code */
	private static final ChildListPropertyDescriptor INTERNAL_TRY_STATEMENT_RESOURCES_PROPERTY = TryStatement.RESOURCES_PROPERTY;

	/** @deprecated using deprecated code */
	private static final ChildPropertyDescriptor INTERNAL_SWITCH_EXPRESSION_PROPERTY = SwitchCase.EXPRESSION_PROPERTY;

	/** @deprecated using deprecated code */
	private static final int JLS2_INTERNAL = AST.JLS2;

	/** @deprecated using deprecated code */
	private static final int JLS3_INTERNAL = AST.JLS3;

	/** @deprecated using deprecated code */
	private static final int JLS4_INTERNAL = AST.JLS4;

	/** @deprecated using deprecated code */
	private static final int JLS8_INTERNAL = AST.JLS8;

	/** @deprecated using deprecated code */
	private static final int JLS9_INTERNAL = AST.JLS9;

	/** @deprecated using deprecated code */
	private static final int JLS14_INTERNAL = AST.JLS14;

	TextEdit currentEdit;
	final RewriteEventStore eventStore; // used from inner classes

	private TokenScanner tokenScanner; // shared scanner

	private final Map sourceCopyInfoToEdit;
	private final Stack sourceCopyEndNodes;

	private final char[] content;
	private final LineInformation lineInfo;
	private final ASTRewriteFormatter formatter;
	private final NodeInfoStore nodeInfos;
	private final TargetSourceRangeComputer extendedSourceRangeComputer;
	private final LineCommentEndOffsets lineCommentEndOffsets;

	private int beforeRequiredSpaceIndex = -1;

	Map options;

	private RecoveryScannerData recoveryScannerData;

	/**
	 * Constructor for ASTRewriteAnalyzer.
	 * <p>The given options cannot be null.</p>
	 *
	 * @param content the content of the compilation unit to rewrite.
	 * @param lineInfo line information for the content of the compilation unit to rewrite.
	 * @param rootEdit the edit to add all generated edits to
	 * @param eventStore the event store containing the description of changes
	 * @param nodeInfos annotations to nodes, such as if a node is a string placeholder or a copy target
	 * @param comments list of comments of the compilation unit to rewrite (elements of type <code>Comment</code>) or <code>null</code>.
	 * @param options the current jdt.core options (formatting/compliance)
	 * @param extendedSourceRangeComputer the source range computer to use
	 * @param recoveryScannerData internal data used by {@link RecoveryScanner}
	 */
	public ASTRewriteAnalyzer(
			char[] content,
			LineInformation lineInfo,
			String lineDelim,
			TextEdit rootEdit,
			RewriteEventStore eventStore,
			NodeInfoStore nodeInfos,
			List comments,
			Map options,
			TargetSourceRangeComputer extendedSourceRangeComputer,
			RecoveryScannerData recoveryScannerData) {
		this.eventStore= eventStore;
		this.content= content;
		this.lineInfo= lineInfo;
		this.nodeInfos= nodeInfos;
		this.tokenScanner= null;
		this.currentEdit= rootEdit;
		this.sourceCopyInfoToEdit= new IdentityHashMap();
		this.sourceCopyEndNodes= new Stack();

		this.formatter= new ASTRewriteFormatter(nodeInfos, eventStore, options, lineDelim);

		this.extendedSourceRangeComputer = extendedSourceRangeComputer;
		this.lineCommentEndOffsets= new LineCommentEndOffsets(comments);

		this.options = options;

		this.recoveryScannerData = recoveryScannerData;
	}

	final TokenScanner getScanner() {
		if (this.tokenScanner == null) {
			CompilerOptions compilerOptions = new CompilerOptions(this.options);
			Scanner scanner;
			if (this.recoveryScannerData == null) {
				scanner =
					new Scanner(
							true,/*tokenizeComments*/
							false,/*tokenizeWhiteSpace*/
							false,/*checkNonExternalizedStringLiterals*/
							compilerOptions.sourceLevel,
							compilerOptions.complianceLevel,
							null/*taskTags*/,
							null/*taskPriorities*/,
							true/*taskCaseSensitive*/,
							compilerOptions.enablePreviewFeatures/*isPreviewEnabled*/);
			} else {
				scanner =
					new RecoveryScanner(
							false,/*tokenizeWhiteSpace*/
							false,/*checkNonExternalizedStringLiterals*/
							compilerOptions.sourceLevel,
							compilerOptions.complianceLevel,
							null/*taskTags*/,
							null/*taskPriorities*/,
							true/*taskCaseSensitive*/,
							compilerOptions.enablePreviewFeatures/*isPreviewEnabled*/,
							this.recoveryScannerData);
			}
			scanner.setSource(this.content);
			this.tokenScanner= new TokenScanner(scanner);
		}
		return this.tokenScanner;
	}

	final char[] getContent() {
		return this.content;
	}

	final LineInformation getLineInformation() {
		return this.lineInfo;
	}

	final LineCommentEndOffsets getLineCommentEndOffsets() {
		return this.lineCommentEndOffsets;
	}
	/**
	 * Returns the extended source range for a node.
	 *
	 * @return an extended source range (never null)
	 * @since 3.1
	 */
	final SourceRange getExtendedRange(ASTNode node) {
		if (this.eventStore.isRangeCopyPlaceholder(node)) {
			return new SourceRange(node.getStartPosition(), node.getLength());
		}
		return this.extendedSourceRangeComputer.computeSourceRange(node);
	}

	final int getExtendedOffset(ASTNode node) {
		return getExtendedRange(node).getStartPosition();
	}

	final int getExtendedEnd(ASTNode node) {
		TargetSourceRangeComputer.SourceRange range= getExtendedRange(node);
		return range.getStartPosition() + range.getLength();
	}

	final TextEdit getCopySourceEdit(CopySourceInfo info) {
		TextEdit edit= (TextEdit) this.sourceCopyInfoToEdit.get(info);
		if (edit == null) {
			SourceRange range= getExtendedRange(info.getNode());
			int start= range.getStartPosition();
			int end= start + range.getLength();
			if (info.isMove) {
				MoveSourceEdit moveSourceEdit= new MoveSourceEdit(start, end - start);
				moveSourceEdit.setTargetEdit(new MoveTargetEdit(0));
				edit= moveSourceEdit;
			} else {
				CopySourceEdit copySourceEdit= new CopySourceEdit(start, end - start);
				copySourceEdit.setTargetEdit(new CopyTargetEdit(0));
				edit= copySourceEdit;
			}
			this.sourceCopyInfoToEdit.put(info, edit);
		}
		return edit;
	}

	private final int getChangeKind(ASTNode node, StructuralPropertyDescriptor property) {
		RewriteEvent event= getEvent(node, property);
		if (event != null) {
			return event.getChangeKind();
		}
		return RewriteEvent.UNCHANGED;
	}

	private final boolean hasChildrenChanges(ASTNode node) {
		return this.eventStore.hasChangedProperties(node);
	}

	private final boolean isChanged(ASTNode node, StructuralPropertyDescriptor property) {
		RewriteEvent event= getEvent(node, property);
		if (event != null) {
			return event.getChangeKind() != RewriteEvent.UNCHANGED;
		}
		return false;
	}

	private final boolean isCollapsed(ASTNode node) {
		return this.nodeInfos.isCollapsed(node);
	}

	final boolean isInsertBoundToPrevious(ASTNode node) {
		return this.eventStore.isInsertBoundToPrevious(node);
	}

	private final TextEditGroup getEditGroup(ASTNode parent, StructuralPropertyDescriptor property) {
		RewriteEvent event= getEvent(parent, property);
		if (event != null) {
			return getEditGroup(event);
		}
		return null;
	}

	final RewriteEvent getEvent(ASTNode parent, StructuralPropertyDescriptor property) {
		return this.eventStore.getEvent(parent, property);
	}

	final TextEditGroup getEditGroup(RewriteEvent change) {
		return this.eventStore.getEventEditGroup(change);
	}

	private final Object getOriginalValue(ASTNode parent, StructuralPropertyDescriptor property) {
		return this.eventStore.getOriginalValue(parent, property);
	}

	private final Object getNewValue(ASTNode parent, StructuralPropertyDescriptor property) {
		return this.eventStore.getNewValue(parent, property);
	}

	final void addEdit(TextEdit edit) {
		this.currentEdit.addChild(edit);
	}

	final String getLineDelimiter() {
		return this.formatter.getLineDelimiter();
	}

	final String createIndentString(int indent) {
	    return this.formatter.createIndentString(indent);
	}

	final private String getIndentOfLine(int pos) {
		int line= getLineInformation().getLineOfOffset(pos);
		if (line >= 0) {
			char[] cont= getContent();
			int lineStart= getLineInformation().getLineOffset(line);
		    int i= lineStart;
			while (i < cont.length && IndentManipulation.isIndentChar(this.content[i])) {
			    i++;
			}
			return new String(cont, lineStart, i - lineStart);
		}
		return Util.EMPTY_STRING;
	}


	final String getIndentAtOffset(int pos) {
		return this.formatter.getIndentString(getIndentOfLine(pos));
	}

	final void doTextInsert(int offset, String insertString, TextEditGroup editGroup) {
		if (insertString.length() > 0) {
			// bug fix for 95839: problem with inserting at the end of a line comment
			if (this.lineCommentEndOffsets.isEndOfLineComment(offset, this.content)) {
				if (!insertString.startsWith(getLineDelimiter())) {
					TextEdit edit= new InsertEdit(offset, getLineDelimiter());  // add a line delimiter
					addEdit(edit);
					if (editGroup != null) {
						addEditGroup(editGroup, edit);
					}
				}
				this.lineCommentEndOffsets.remove(offset); // only one line delimiter per line comment required
			}
			TextEdit edit= new InsertEdit(offset, insertString);
			addEdit(edit);
			if (editGroup != null) {
				addEditGroup(editGroup, edit);
			}
		}
	}

	final void addEditGroup(TextEditGroup editGroup, TextEdit edit) {
		editGroup.addTextEdit(edit);
	}

	final TextEdit doTextRemove(int offset, int len, TextEditGroup editGroup) {
		if (len == 0) {
			return null;
		}
		TextEdit edit= new DeleteEdit(offset, len);
		addEdit(edit);
		if (editGroup != null) {
			addEditGroup(editGroup, edit);
		}
		return edit;
	}

	final void doTextRemoveAndVisit(int offset, int len, ASTNode node, TextEditGroup editGroup) {
		TextEdit edit= doTextRemove(offset, len, editGroup);
		if (edit != null) {
			this.currentEdit= edit;
			voidVisit(node);
			this.currentEdit= edit.getParent();
		} else {
			voidVisit(node);
		}
	}

	final int doVisit(ASTNode node) {
		node.accept(this);
		return getExtendedEnd(node);
	}

	private final int doVisit(ASTNode parent, StructuralPropertyDescriptor property, int offset) {
		Object node= getOriginalValue(parent, property);
		if (property.isChildProperty() && node != null) {
			return doVisit((ASTNode) node);
		} else if (property.isChildListProperty()) {
			return doVisitList((List) node, offset);
		}
		return offset;
	}

	private int doVisitList(List list, int offset) {
		int endPos= offset;
		for (Iterator iter= list.iterator(); iter.hasNext();) {
			ASTNode curr= ((ASTNode) iter.next());
			endPos= doVisit(curr);
		}
		return endPos;
	}

	final void voidVisit(ASTNode node) {
		node.accept(this);
	}

	private final void voidVisit(ASTNode parent, StructuralPropertyDescriptor property) {
		Object node= getOriginalValue(parent, property);
		if (property.isChildProperty() && node != null) {
			voidVisit((ASTNode) node);
		} else if (property.isChildListProperty()) {
			voidVisitList((List) node);
		}
	}

	private void voidVisitList(List list) {
		for (Iterator iter= list.iterator(); iter.hasNext();) {
			doVisit(((ASTNode) iter.next()));
		}
	}

	private final boolean doVisitUnchangedChildren(ASTNode parent) {
		List properties= parent.structuralPropertiesForType();
		for (int i= 0; i < properties.size(); i++) {
			voidVisit(parent, (StructuralPropertyDescriptor) properties.get(i));
		}
		return false;
	}


	private final void doTextReplace(int offset, int len, String insertString, TextEditGroup editGroup) {
		if (len > 0 || insertString.length() > 0) {
			TextEdit edit= new ReplaceEdit(offset, len, insertString);
			addEdit(edit);
			if (editGroup != null) {
				addEditGroup(editGroup, edit);
			}
		}
	}

	private final TextEdit doTextCopy(TextEdit sourceEdit, int destOffset, int sourceIndentLevel, String destIndentString, TextEditGroup editGroup) {
		TextEdit targetEdit;
		SourceModifier modifier= new SourceModifier(sourceIndentLevel, destIndentString, this.formatter.getTabWidth(), this.formatter.getIndentWidth());

		if (sourceEdit instanceof MoveSourceEdit) {
			MoveSourceEdit moveEdit= (MoveSourceEdit) sourceEdit;
			moveEdit.setSourceModifier(modifier);

			targetEdit= new MoveTargetEdit(destOffset, moveEdit);
			addEdit(targetEdit);
		} else {
			CopySourceEdit copyEdit= (CopySourceEdit) sourceEdit;
			copyEdit.setSourceModifier(modifier);

			targetEdit= new CopyTargetEdit(destOffset, copyEdit);
			addEdit(targetEdit);
		}

		if (editGroup != null) {
			addEditGroup(editGroup, sourceEdit);
			addEditGroup(editGroup, targetEdit);
		}
		return targetEdit;

	}

	private void changeNotSupported(ASTNode node) {
		Assert.isTrue(false, "Change not supported in " + node.getClass().getName()); //$NON-NLS-1$
	}


	class ListRewriter {
		protected String constantSeparator;
		protected int startPos;

		protected RewriteEvent[] list;

		protected final ASTNode getOriginalNode(int index) {
			return (ASTNode) this.list[index].getOriginalValue();
		}

		protected final ASTNode getNewNode(int index) {
			return (ASTNode) this.list[index].getNewValue();
		}

		protected String getSeparatorString(int nodeIndex) {
			return this.constantSeparator;
		}

		protected int getInitialIndent() {
			return getIndent(this.startPos);
		}

		protected int getNodeIndent(int nodeIndex) {
			ASTNode node= getOriginalNode(nodeIndex);
			if (node == null) {
				for (int i= nodeIndex - 1; i>= 0; i--) {
					ASTNode curr= getOriginalNode(i);
					if (curr != null) {
						return getIndent(curr.getStartPosition());
					}
				}
				return getInitialIndent();
			}
			return getIndent(node.getStartPosition());
		}

		protected int getStartOfNextNode(int nextIndex, int defaultPos) {
			for (int i= nextIndex; i < this.list.length; i++) {
				RewriteEvent elem= this.list[i];
				if (elem.getChangeKind() != RewriteEvent.INSERTED) {
					ASTNode node= (ASTNode) elem.getOriginalValue();
					return getExtendedOffset(node);
				}
			}
			return defaultPos;
		}

		protected int getEndOfNode(ASTNode node) {
			return getExtendedEnd(node);
		}

		public final int rewriteList(ASTNode parent, StructuralPropertyDescriptor property, int offset, String keyword, String separator) {
			this.constantSeparator= separator;
			return rewriteList(parent, property, keyword, null, offset);
		}

		private boolean insertAfterSeparator(ASTNode node) {
			return !isInsertBoundToPrevious(node);
		}

		private boolean lineCommentSwallowsActualCode(int prevEnd) {
			if (ASTRewriteAnalyzer.this.getLineCommentEndOffsets().isEndOfLineComment(prevEnd)) {
				int lastEndOffset = getEndOfNode((ASTNode) this.list[this.list.length - 1].getOriginalValue());
				LineInformation lInfo = ASTRewriteAnalyzer.this.getLineInformation();
				try {
					return lInfo.getLineOfOffset(lastEndOffset) == lInfo.getLineOfOffset(getScanner().getNextStartOffset(lastEndOffset, false));
				} catch (CoreException e) {
					// ignore
				}
			}
			return false;
		}

		protected boolean mustRemoveSeparator(int originalOffset, int nodeIndex) {
			return true;
		}

		private int rewriteList(
				ASTNode parent,
				StructuralPropertyDescriptor property,
				String keyword,
				String endKeyword,
				int offset) {
			this.startPos= offset;
			this.list= getEvent(parent, property).getChildren();

			int total= this.list.length;
			if (total == 0) {
				return this.startPos;
			}

			int currPos= -1;

			int lastNonInsert= -1;
			int lastNonDelete= -1;

			for (int i= 0; i < total; i++) {
				int currMark= this.list[i].getChangeKind();

				if (currMark != RewriteEvent.INSERTED) {
					lastNonInsert= i;
					if (currPos == -1) {
						ASTNode elem= (ASTNode) this.list[i].getOriginalValue();
						currPos= getExtendedOffset(elem);
					}
				}
				if (currMark != RewriteEvent.REMOVED) {
					lastNonDelete= i;
				}
			}

			boolean insertNew = currPos == -1;
			if (insertNew) { // only inserts
				if (keyword.length() > 0) {  // creating a new list -> insert keyword first (e.g. " throws ")
					TextEditGroup editGroup= getEditGroup(this.list[0]); // first node is insert
					doTextInsert(offset, keyword, editGroup);
				}
				currPos= offset;
			}
			if (lastNonDelete == -1) { // all removed, set back to start so the keyword is removed as well
				currPos= offset;
			}

			int prevEnd= currPos;
			int prevMark= RewriteEvent.UNCHANGED;

			final int NONE= 0, NEW= 1, EXISTING= 2;
			int separatorState= NEW;

			for (int i= 0; i < total; i++) {
				RewriteEvent currEvent= this.list[i];
				int currMark= currEvent.getChangeKind();
				int nextIndex= i + 1;

				if (currMark == RewriteEvent.INSERTED) {
					TextEditGroup editGroup= getEditGroup(currEvent);
					ASTNode node= (ASTNode) currEvent.getNewValue();

					if (separatorState == NONE) { // element after last existing element (but not first)
						doTextInsert(currPos, getSeparatorString(i - 1), editGroup); // insert separator
						separatorState= NEW;
					}
					if (separatorState == NEW || insertAfterSeparator(node)) {
						if (separatorState == EXISTING) {
							updateIndent(prevMark, currPos, i, editGroup);
						}

						doTextInsert(currPos, node, getNodeIndent(i), true, editGroup); // insert node

						separatorState= NEW;
						if (i != lastNonDelete) {
							if (this.list[nextIndex].getChangeKind() != RewriteEvent.INSERTED) {
								doTextInsert(currPos, getSeparatorString(i), editGroup); // insert separator
							} else {
								separatorState= NONE;
							}
						}
					} else { // EXISTING && insert before separator
						doTextInsert(prevEnd, getSeparatorString(i - 1), editGroup);
						doTextInsert(prevEnd, node, getNodeIndent(i), true, editGroup);
					}
					if (insertNew && i == lastNonDelete) {
						if (endKeyword != null && endKeyword.length() > 0) {
							doTextInsert(currPos, endKeyword, editGroup);
						}
					}
				} else if (currMark == RewriteEvent.REMOVED) {
					ASTNode node= (ASTNode) currEvent.getOriginalValue();
					TextEditGroup editGroup= getEditGroup(currEvent);
					int currEnd= getEndOfNode(node);
					// https://bugs.eclipse.org/bugs/show_bug.cgi?id=306524
					// Check for leading comments that are not part of extended range, and prevent them
					// from getting removed.
					try {
						TokenScanner scanner = getScanner();
						int newOffset = prevEnd;
						int extendedOffset = getExtendedOffset(node);
						// Try to find the end of the last comment which is not part of extended source
						// range of the node.
						while (TokenScanner.isComment(scanner.readNext(newOffset, false))) {
							int tempOffset = scanner.getNextEndOffset(newOffset, false);
							// check whether the comment is part of extended source range of the node.
							// If it is then we need to stop.
							if (tempOffset < extendedOffset) {
								newOffset = tempOffset;
							} else {
								break;
							}
						}
						if (currPos < newOffset) {
							currPos = extendedOffset;
						}
						prevEnd = newOffset;
					} catch (CoreException e) {
						// ignore
					}
					if (i > lastNonDelete && separatorState == EXISTING) {
						// is last, remove previous separator: split delete to allow range copies
						doTextRemove(prevEnd, currPos - prevEnd, editGroup); // remove separator
						doTextRemoveAndVisit(currPos, currEnd - currPos, node, editGroup); // remove node
						if (lineCommentSwallowsActualCode(prevEnd)) doTextInsert(currEnd, getLineDelimiter(), editGroup);
						currPos= currEnd;
						prevEnd= currEnd;
					} else {
						if (i < lastNonDelete) {
							updateIndent(prevMark, currPos, i, editGroup);
						}

						// remove element and next separator
						int end= getStartOfNextNode(nextIndex, currEnd); // start of next
						// https://bugs.eclipse.org/bugs/show_bug.cgi?id=306524
						// Check for trailing comments that are not part of extended range, and prevent them
						// from getting removed.
						try {
							TokenScanner scanner = getScanner();
							int nextToken= scanner.readNext(currEnd, false);
							if (TokenScanner.isComment(nextToken)) {
								// the separator also has comments that are not part of extended
								// source range of this node or the next node. So dont remove the separator
								if (end != scanner.getNextStartOffset(currEnd, false)) {
									// If this condition were true, comments just found as part of the separator would've basically been
									// part of the extended source range of the next node. So 'end' wud've safely been set to the correct position
									// and no change is needed.
									end = currEnd;
								}
							}
						} catch (CoreException e) {
							// ignore
						}
						doTextRemoveAndVisit(currPos, currEnd - currPos, node, getEditGroup(currEvent)); // remove node
						if (mustRemoveSeparator(currPos, i)) {
							doTextRemove(currEnd, end - currEnd, editGroup); // remove separator
						}
						currPos= end;
						prevEnd= currEnd;
						separatorState= NEW;
					}
				} else { // replaced or unchanged
					if (currMark == RewriteEvent.REPLACED) {
						ASTNode node= (ASTNode) currEvent.getOriginalValue();
						int currEnd= getEndOfNode(node);

						TextEditGroup editGroup= getEditGroup(currEvent);
						ASTNode changed= (ASTNode) currEvent.getNewValue();

						updateIndent(prevMark, currPos, i, editGroup);
						// make sure that comments between last modified source position and extended starting position of
						// node to be replaced are not touched
						try {
							TokenScanner scanner = getScanner();
							int newOffset = prevEnd;
							int extendedOffset = getExtendedOffset(node);
							// Try to find the end of the last comment which is not part of extended source
							// range of the node.
							while (TokenScanner.isComment(scanner.readNext(newOffset, false))) {
								int tempOffset = scanner.getNextEndOffset(newOffset, false);
								// check whether the comment is part of extended source range of the node.
								// If it is then we need to stop.
								if (tempOffset < extendedOffset) {
									newOffset = tempOffset;
								} else {
									break;
								}
							}
							if (currPos < newOffset) {
								currPos = extendedOffset;
							}
						} catch (CoreException e) {
							// ignore
						}
						doTextRemoveAndVisit(currPos, currEnd - currPos, node, editGroup);
						doTextInsert(currPos, changed, getNodeIndent(i), true, editGroup);

						prevEnd= currEnd;
					} else { // is unchanged
						ASTNode node= (ASTNode) currEvent.getOriginalValue();
						voidVisit(node);
					}
					if (i == lastNonInsert) { // last node or next nodes are all inserts
						separatorState= NONE;
						if (currMark == RewriteEvent.UNCHANGED) {
							ASTNode node= (ASTNode) currEvent.getOriginalValue();
							prevEnd= getEndOfNode(node);
						}
						currPos= prevEnd;
					} else if (this.list[nextIndex].getChangeKind() != RewriteEvent.UNCHANGED) {
						// no updates needed while nodes are unchanged
						if (currMark == RewriteEvent.UNCHANGED) {
							ASTNode node= (ASTNode) currEvent.getOriginalValue();
							prevEnd= getEndOfNode(node);
						}
						currPos= getStartOfNextNode(nextIndex, prevEnd); // start of next
						separatorState= EXISTING;
					}
				}

				prevMark = currMark;
			}
			return currPos;
		}

		public final int rewriteList(ASTNode parent, StructuralPropertyDescriptor property, int offset, String keyword) {
			return rewriteList(parent, property, keyword, null, offset);
		}

		protected void updateIndent(int prevMark, int originalOffset, int nodeIndex, TextEditGroup editGroup) {
			// Do nothing.
		}

		public final int rewriteList(ASTNode parent, StructuralPropertyDescriptor property, int offset, String keyword, String endKeyword, String separator) {
			this.constantSeparator= separator;
			return rewriteList(parent, property, keyword, endKeyword, offset);
		}
	}

	private int rewriteRequiredNode(ASTNode parent, StructuralPropertyDescriptor property) {
		RewriteEvent event= getEvent(parent, property);
		if (event != null && event.getChangeKind() == RewriteEvent.REPLACED) {
			ASTNode node= (ASTNode) event.getOriginalValue();
			TextEditGroup editGroup= getEditGroup(event);
			SourceRange range= getExtendedRange(node);
			int offset= range.getStartPosition();
			int length= range.getLength();
			doTextRemoveAndVisit(offset, length, node, editGroup);
			doTextInsert(offset, (ASTNode) event.getNewValue(), getIndent(offset), true, editGroup);
			return offset + length;
		}
		return doVisit(parent, property, 0);
	}

	private int rewriteNode(ASTNode parent, StructuralPropertyDescriptor property, int offset, Prefix prefix) {
		RewriteEvent event= getEvent(parent, property);
		if (event != null) {
			switch (event.getChangeKind()) {
				case RewriteEvent.INSERTED: {
					ASTNode node= (ASTNode) event.getNewValue();
					TextEditGroup editGroup= getEditGroup(event);
					int indent= getIndent(offset);
					doTextInsert(offset, prefix.getPrefix(indent), editGroup);
					doTextInsert(offset, node, indent, true, editGroup);
					return offset;
				}
				case RewriteEvent.REMOVED: {
					ASTNode node= (ASTNode) event.getOriginalValue();
					TextEditGroup editGroup= getEditGroup(event);

					// if there is a prefix, remove the prefix as well
					int nodeEnd;
					int len;
					if (offset == 0) {
						SourceRange range= getExtendedRange(node);
						offset= range.getStartPosition();
						len= range.getLength();
						nodeEnd= offset+len;
					} else {
						nodeEnd= getExtendedEnd(node);
						len= nodeEnd-offset;
					}
					doTextRemoveAndVisit(offset, len, node, editGroup);
					return nodeEnd;
				}
				case RewriteEvent.REPLACED: {
					ASTNode node= (ASTNode) event.getOriginalValue();
					TextEditGroup editGroup= getEditGroup(event);
					SourceRange range= getExtendedRange(node);
					int nodeOffset= range.getStartPosition();
					int nodeLen= range.getLength();
					doTextRemoveAndVisit(nodeOffset, nodeLen, node, editGroup);
					doTextInsert(nodeOffset, (ASTNode) event.getNewValue(), getIndent(offset), true, editGroup);
					return nodeOffset + nodeLen;
				}
			}
		}
		return doVisit(parent, property, offset);
	}

	private int rewriteJavadoc(ASTNode node, StructuralPropertyDescriptor property) {
		int pos= rewriteNode(node, property, node.getStartPosition(), ASTRewriteFormatter.NONE);
		int changeKind= getChangeKind(node, property);
		if (changeKind == RewriteEvent.INSERTED) {
			String indent= getLineDelimiter() + getIndentAtOffset(pos);
			doTextInsert(pos, indent, getEditGroup(node, property));
		} else if (changeKind == RewriteEvent.REMOVED) {
			try {
				getScanner().readNext(pos, false);
				doTextRemove(pos, getScanner().getCurrentStartOffset() - pos, getEditGroup(node, property));
				pos= getScanner().getCurrentStartOffset();
			} catch (CoreException e) {
				handleException(e);
			}
		}
		return pos;
	}


	/*
	 * endpos can be -1 -> use the end pos of the body
	 */
	private int rewriteBodyNode(ASTNode parent, StructuralPropertyDescriptor property, int offset, int endPos, int indent, BlockContext context) {
		RewriteEvent event= getEvent(parent, property);
		if (event != null) {
			switch (event.getChangeKind()) {
				case RewriteEvent.INSERTED: {
					ASTNode node= (ASTNode) event.getNewValue();
					TextEditGroup editGroup= getEditGroup(event);

					String[] strings= context.getPrefixAndSuffix(indent, node, this.eventStore);

					doTextInsert(offset, strings[0], editGroup);
					doTextInsert(offset, node, indent, true, editGroup);
					doTextInsert(offset, strings[1], editGroup);
					return offset;
				}
				case RewriteEvent.REMOVED: {
					ASTNode node= (ASTNode) event.getOriginalValue();
					if (endPos == -1) {
						endPos= getExtendedEnd(node);
					}

					TextEditGroup editGroup= getEditGroup(event);
					// if there is a prefix, remove the prefix as well
					int len= endPos - offset;
					doTextRemoveAndVisit(offset, len, node, editGroup);
					return endPos;
				}
				case RewriteEvent.REPLACED: {
					ASTNode node= (ASTNode) event.getOriginalValue();
					boolean insertNewLine = false;
					if (endPos == -1) {
						int previousEnd = node.getStartPosition() + node.getLength();
						endPos= getExtendedEnd(node);
						if (endPos != previousEnd) {
							// check if the end is a comment
							int token = TokenScanner.END_OF_FILE;
							try {
								token = getScanner().readNext(previousEnd, false);
							} catch(CoreException e) {
								// ignore
							}
							if (token == TerminalTokens.TokenNameCOMMENT_LINE) {
								insertNewLine = true;
							}
						}
					}
					TextEditGroup editGroup= getEditGroup(event);
					int nodeLen= endPos - offset;

					ASTNode replacingNode= (ASTNode) event.getNewValue();
					String[] strings= context.getPrefixAndSuffix(indent, replacingNode, this.eventStore);
					doTextRemoveAndVisit(offset, nodeLen, node, editGroup);

					String prefix= strings[0];
					String insertedPrefix = prefix;
					if (insertNewLine) {
						insertedPrefix = getLineDelimiter() + this.formatter.createIndentString(indent) + insertedPrefix.trim() + ' ';
					}
					doTextInsert(offset, insertedPrefix, editGroup);
					int lineStart= getCurrentLineStart(prefix, prefix.length());
					if (lineStart != 0) {
						// prefix contains a new line: update the indent to the one used in the prefix
						indent= this.formatter.computeIndentUnits(prefix.substring(lineStart));
					}
					doTextInsert(offset, replacingNode, indent, true, editGroup);
					doTextInsert(offset, strings[1], editGroup);
					return endPos;
				}
			}
		}
		int pos= doVisit(parent, property, offset);
		if (endPos != -1) {
			return endPos;
		}
		return pos;
	}
	private int rewriteOptionalQualifier(ASTNode parent, StructuralPropertyDescriptor property, int startPos) {
		RewriteEvent event= getEvent(parent, property);
		if (event != null) {
			switch (event.getChangeKind()) {
				case RewriteEvent.INSERTED: {
					ASTNode node= (ASTNode) event.getNewValue();
					TextEditGroup editGroup= getEditGroup(event);
					doTextInsert(startPos, node, getIndent(startPos), true, editGroup);
					doTextInsert(startPos, ".", editGroup); //$NON-NLS-1$
					return startPos;
				}
				case RewriteEvent.REMOVED: {
					try {
						ASTNode node= (ASTNode) event.getOriginalValue();
						TextEditGroup editGroup= getEditGroup(event);
						int dotEnd= getScanner().getTokenEndOffset(TerminalTokens.TokenNameDOT, node.getStartPosition() + node.getLength());
						doTextRemoveAndVisit(startPos, dotEnd - startPos, node, editGroup);
						return dotEnd;
					} catch (CoreException e) {
						handleException(e);
					}
					break;
				}
				case RewriteEvent.REPLACED: {
					ASTNode node= (ASTNode) event.getOriginalValue();
					TextEditGroup editGroup= getEditGroup(event);
					SourceRange range= getExtendedRange(node);
					int offset= range.getStartPosition();
					int length= range.getLength();

					doTextRemoveAndVisit(offset, length, node, editGroup);
					doTextInsert(offset, (ASTNode) event.getNewValue(), getIndent(startPos), true, editGroup);
					try {
						return getScanner().getTokenEndOffset(TerminalTokens.TokenNameDOT, offset + length);
					} catch (CoreException e) {
						handleException(e);
					}
					break;
				}
			}
		}
		Object node= getOriginalValue(parent, property);
		if (node == null) {
			return startPos;
		}
		int pos= doVisit((ASTNode) node);
		try {
			return getScanner().getTokenEndOffset(TerminalTokens.TokenNameDOT, pos);
		} catch (CoreException e) {
			handleException(e);
		}
		return pos;
	}

	private int rewriteExpressionOptionalQualifier(SwitchCase parent, StructuralPropertyDescriptor property, int startPos) {
		RewriteEvent event= getEvent(parent, property);
		if (event != null) {
			switch (event.getChangeKind()) {
				case RewriteEvent.INSERTED: {
					ASTNode node= (ASTNode) event.getNewValue();
					TextEditGroup editGroup= getEditGroup(event);
					doTextInsert(startPos, node, getIndent(startPos), true, editGroup);
					doTextInsert(startPos, ".", editGroup); //$NON-NLS-1$
					return startPos;
				}
				case RewriteEvent.REMOVED: {
					try {
						ASTNode node= (ASTNode) event.getOriginalValue();
						TextEditGroup editGroup= getEditGroup(event);
						int dotEnd= getScanner().getTokenEndOffset(TerminalTokens.TokenNameCOLON, node.getStartPosition() + node.getLength());
						doTextRemoveAndVisit(startPos, dotEnd - startPos, node, editGroup);
						return dotEnd;
					} catch (CoreException e) {
						handleException(e);
					}
					break;
				}
				case RewriteEvent.REPLACED: {
					ASTNode node= (ASTNode) event.getOriginalValue();
					TextEditGroup editGroup= getEditGroup(event);
					SourceRange range= getExtendedRange(node);
					int offset= range.getStartPosition();
					int length= range.getLength();

					doTextRemoveAndVisit(offset, length, node, editGroup);
					doTextInsert(offset, (ASTNode) event.getNewValue(), getIndent(startPos), true, editGroup);
					try {
						return getScanner().getTokenEndOffset(TerminalTokens.TokenNameCOLON, offset + length);
					} catch (CoreException e) {
						handleException(e);
					}
					break;
				}
			}
		}
		Object node= getOriginalValue(parent, property);
		if (node == null) {
			return startPos;
		}
		int pos= doVisit((ASTNode) node);
		try {
			return getScanner().getTokenEndOffset(TerminalTokens.TokenNameCOLON, pos);
		} catch (CoreException e) {
			handleException(e);
		}
		return pos;
	}

	class ParagraphListRewriter extends ListRewriter {

		public final static int DEFAULT_SPACING= 1;

		private int initialIndent;
		private int separatorLines;

		public ParagraphListRewriter(int initialIndent, int separator) {
			this.initialIndent= initialIndent;
			this.separatorLines= separator;
		}

		@Override
		protected int getInitialIndent() {
			return this.initialIndent;
		}

		@Override
		protected String getSeparatorString(int nodeIndex) {
			return getSeparatorString(nodeIndex, nodeIndex + 1);
		}

		protected String getSeparatorString(int nodeIndex, int nextNodeIndex) {
			int newLines= this.separatorLines == -1 ? getNewLines(nodeIndex) : this.separatorLines;

			String lineDelim= getLineDelimiter();
			StringBuilder buf= new StringBuilder(lineDelim);
			for (int i= 0; i < newLines; i++) {
				buf.append(lineDelim);
			}
			buf.append(createIndentString(getNodeIndent(nextNodeIndex)));
			return buf.toString();
		}

		protected ASTNode getNode(int nodeIndex) {
			ASTNode elem= (ASTNode) this.list[nodeIndex].getOriginalValue();
			if (elem == null) {
				elem= (ASTNode) this.list[nodeIndex].getNewValue();
			}
			return elem;
		}

		private int getNewLines(int nodeIndex) {
			ASTNode curr= getNode(nodeIndex);
			ASTNode next= getNode(nodeIndex + 1);

			int currKind= curr.getNodeType();
			int nextKind= next.getNodeType();

			ASTNode last= null;
			ASTNode secondLast= null;
			for (int i= 0; i < this.list.length; i++) {
				ASTNode elem= (ASTNode) this.list[i].getOriginalValue();
				if (elem != null) {
					if (last != null) {
						if (elem.getNodeType() == nextKind && last.getNodeType() == currKind) {
							return countEmptyLines(last);
						}
						secondLast= last;
					}
					last= elem;
				}
			}
			if (currKind == ASTNode.FIELD_DECLARATION && nextKind == ASTNode.FIELD_DECLARATION ) {
				return 0;
			}
			if (secondLast != null) {
				return countEmptyLines(secondLast);
			}
			return DEFAULT_SPACING;
		}

		private int countEmptyLines(ASTNode last) {
			LineInformation lineInformation= getLineInformation();
			int lastLine= lineInformation.getLineOfOffset(getExtendedEnd(last));
			if (lastLine >= 0) {
				int startLine= lastLine + 1;
				int start= lineInformation.getLineOffset(startLine);
				if (start < 0) {
					return 0;
				}
				char[] cont= getContent();
				int i= start;
				while (i < cont.length && ScannerHelper.isWhitespace(cont[i])) {
					i++;
				}
				if (i > start) {
					lastLine= lineInformation.getLineOfOffset(i);
					if (lastLine > startLine) {
						return lastLine - startLine;
					}
				}
			}
			return 0;
		}

		@Override
		protected boolean mustRemoveSeparator(int originalOffset, int nodeIndex) {
			// Do not remove separator if the previous non removed node is on the same line and the next node is on another line
			int previousNonRemovedNodeIndex = nodeIndex - 1;
			while (previousNonRemovedNodeIndex >= 0 && this.list[previousNonRemovedNodeIndex].getChangeKind() == RewriteEvent.REMOVED) {
				previousNonRemovedNodeIndex--;
			}

			if (previousNonRemovedNodeIndex > -1) {
				LineInformation lineInformation = getLineInformation();

				RewriteEvent prevEvent = this.list[previousNonRemovedNodeIndex];
				int prevKind = prevEvent.getChangeKind();
				if (prevKind == RewriteEvent.UNCHANGED || prevKind == RewriteEvent.REPLACED) {
					ASTNode prevNode = (ASTNode) this.list[previousNonRemovedNodeIndex].getOriginalValue();
					int prevEndPosition = prevNode.getStartPosition() + prevNode.getLength();
					int prevLine = lineInformation.getLineOfOffset(prevEndPosition);
					int line = lineInformation.getLineOfOffset(originalOffset);

					if (prevLine == line && nodeIndex + 1 < this.list.length) {
						RewriteEvent nextEvent = this.list[nodeIndex + 1];
						int nextKind = nextEvent.getChangeKind();

						if (nextKind == RewriteEvent.UNCHANGED || prevKind == RewriteEvent.REPLACED) {
							ASTNode nextNode = (ASTNode) nextEvent.getOriginalValue();
							int nextStartPosition = nextNode.getStartPosition();
							int nextLine = lineInformation.getLineOfOffset(nextStartPosition);

							return nextLine == line;
						}
						return false;
					}
				}
			}

			return true;
		}
	}

	private int rewriteParagraphList(ASTNode parent, StructuralPropertyDescriptor property, int insertPos, int insertIndent, int separator, int lead) {
		RewriteEvent event= getEvent(parent, property);
		if (event == null || event.getChangeKind() == RewriteEvent.UNCHANGED) {
			return doVisit(parent, property, insertPos);
		}

		RewriteEvent[] events= event.getChildren();
		ParagraphListRewriter listRewriter= new ParagraphListRewriter(insertIndent, separator);
		StringBuilder leadString= new StringBuilder();
		if (isAllOfKind(events, RewriteEvent.INSERTED)) {
			for (int i= 0; i < lead; i++) {
				leadString.append(getLineDelimiter());
			}
			leadString.append(createIndentString(insertIndent));
		}
		return listRewriter.rewriteList(parent, property, insertPos, leadString.toString());
	}

	private int rewriteOptionalTypeParameters(ASTNode parent, StructuralPropertyDescriptor property, int offset, String keyword, boolean adjustOnNext, boolean needsSpaceOnRemoveAll) {
		int pos= offset;
		RewriteEvent event= getEvent(parent, property);
		if (event != null && event.getChangeKind() != RewriteEvent.UNCHANGED) {
			RewriteEvent[] children= event.getChildren();
			try {
				boolean isAllInserted= isAllOfKind(children, RewriteEvent.INSERTED);
				if (isAllInserted && adjustOnNext) {
					pos= getScanner().getNextStartOffset(pos, false); // adjust on next element
				}
				boolean isAllRemoved= !isAllInserted && isAllOfKind(children, RewriteEvent.REMOVED);
				if (isAllRemoved) { // all removed: set start to left bracket
					int posBeforeOpenBracket= getScanner().getTokenStartOffset(TerminalTokens.TokenNameLESS, pos);
					if (posBeforeOpenBracket != pos) {
						needsSpaceOnRemoveAll= false;
					}
					pos= posBeforeOpenBracket;
				}
				pos= new ListRewriter().rewriteList(parent, property, pos, String.valueOf('<'), ", "); //$NON-NLS-1$
				if (isAllRemoved) { // all removed: remove right and space up to next element
					int endPos= getScanner().getTokenEndOffset(TerminalTokens.TokenNameGREATER, pos); // set pos to '>'
					endPos= getScanner().getNextStartOffset(endPos, false);
					String replacement= needsSpaceOnRemoveAll ? String.valueOf(' ') : Util.EMPTY_STRING;
					doTextReplace(pos, endPos - pos, replacement, getEditGroup(children[children.length - 1]));
					return endPos;
				} else if (isAllInserted) {
					doTextInsert(pos, String.valueOf('>' + keyword), getEditGroup(children[children.length - 1]));
					return pos;
				}
			} catch (CoreException e) {
				handleException(e);
			}
		} else {
			pos= doVisit(parent, property, pos);
		}
		if (pos != offset) { // list contained some type -> parse after closing bracket
			try {
				return getScanner().getTokenEndOffset(TerminalTokens.TokenNameGREATER, pos);
			} catch (CoreException e) {
				handleException(e);
			}
		}
		return pos;
	}

	private boolean isAllOfKind(RewriteEvent[] children, int kind) {
		for (int i= 0; i < children.length; i++) {
			if (children[i].getChangeKind() != kind) {
				return false;
			}
		}
		return true;
	}

	private int rewriteNodeList(ASTNode parent, StructuralPropertyDescriptor property, int pos, String keyword, String endKeyword, String separator) {
		RewriteEvent event= getEvent(parent, property);
		if (event != null && event.getChangeKind() != RewriteEvent.UNCHANGED) {
			return new ListRewriter().rewriteList(parent, property, pos, keyword, endKeyword, separator);
		}
		return doVisit(parent, property, pos);
	}

	private int rewriteNodeList(ASTNode parent, StructuralPropertyDescriptor property, int pos, String keyword, String separator) {
		RewriteEvent event= getEvent(parent, property);
		if (event != null && event.getChangeKind() != RewriteEvent.UNCHANGED) {
			return new ListRewriter().rewriteList(parent, property, pos, keyword, separator);
		}
		return doVisit(parent, property, pos);
	}

	private void rewriteMethodBody(MethodDeclaration parent, int startPos) {
		RewriteEvent event= getEvent(parent, MethodDeclaration.BODY_PROPERTY);
		if (event != null) {
			switch (event.getChangeKind()) {
				case RewriteEvent.INSERTED: {
					int endPos= parent.getStartPosition() + parent.getLength();
					TextEditGroup editGroup= getEditGroup(event);
					ASTNode body= (ASTNode) event.getNewValue();
					doTextRemove(startPos, endPos - startPos, editGroup);
					int indent= getIndent(parent.getStartPosition());
					String prefix= this.formatter.METHOD_BODY.getPrefix(indent);
					doTextInsert(startPos, prefix, editGroup);
					doTextInsert(startPos, body, indent, true, editGroup);
					return;
				}
				case RewriteEvent.REMOVED: {
					TextEditGroup editGroup= getEditGroup(event);
					ASTNode body= (ASTNode) event.getOriginalValue();
					int endPos= parent.getStartPosition() + parent.getLength();
					doTextRemoveAndVisit(startPos, endPos - startPos, body, editGroup);
					doTextInsert(startPos, ";", editGroup); //$NON-NLS-1$
					return;
				}
				case RewriteEvent.REPLACED: {
					TextEditGroup editGroup= getEditGroup(event);
					ASTNode body= (ASTNode) event.getOriginalValue();
					doTextRemoveAndVisit(body.getStartPosition(), body.getLength(), body, editGroup);
					doTextInsert(body.getStartPosition(), (ASTNode) event.getNewValue(), getIndent(body.getStartPosition()), true, editGroup);
					return;
				}
			}
		}
		voidVisit(parent, MethodDeclaration.BODY_PROPERTY);
	}

	protected int rewriteExtraDimensionsInfo(ASTNode node, int pos, ChildListPropertyDescriptor property) {
		return rewriteNodeList(node, property, pos, " ", ""); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private int rewriteExtraDimensions(ASTNode parent, StructuralPropertyDescriptor property, int pos) {
		RewriteEvent event= getEvent(parent, property);
		if (event == null || event.getChangeKind() == RewriteEvent.UNCHANGED) {
			return ((Integer) getOriginalValue(parent, property)).intValue();
		}
		int oldDim= ((Integer) event.getOriginalValue()).intValue();
		int newDim= ((Integer) event.getNewValue()).intValue();

		if (oldDim != newDim) {
			TextEditGroup editGroup= getEditGroup(event);
			rewriteExtraDimensions(oldDim, newDim, pos, editGroup);
		}
		return oldDim;
	}

	private void rewriteExtraDimensions(int oldDim, int newDim, int pos, TextEditGroup editGroup) {

		if (oldDim < newDim) {
			for (int i= oldDim; i < newDim; i++) {
				doTextInsert(pos, "[]", editGroup); //$NON-NLS-1$
			}
		} else if (newDim < oldDim) {
			try {
				getScanner().setOffset(pos);
				for (int i= newDim; i < oldDim; i++) {
					getScanner().readToToken(TerminalTokens.TokenNameRBRACKET);
				}
				doTextRemove(pos, getScanner().getCurrentEndOffset() - pos, editGroup);
			} catch (CoreException e) {
				handleException(e);
			}
		}
	}

	private int getPosAfterToken(int pos, int token) {
		try {
			int nextToken= getScanner().readNext(pos, true);
			if (nextToken == token) {
				return getScanner().getCurrentEndOffset();
			}
		} catch (CoreException e) {
			handleException(e);
		}
		return pos;
	}
	/*
	 * Next token is a left brace. Returns the offset after the brace. For incomplete code, return the start offset.
	 */
	private int getPosAfterLeftBrace(int pos) {
		try {
			return getPosAfterToken(pos, TerminalTokens.TokenNameLBRACE);
		} catch (IllegalArgumentException e) {
			return pos;
		}
	}

	/*
	 * Next token is a right parenthesis. Returns the offset after the parenthesis. For incomplete code, return the start offset.
	 */
	private int getPosAfterRightParenthesis(int pos) {
		try {
			return getPosAfterToken(pos, TerminalTokens.TokenNameRPAREN);
		} catch (IllegalArgumentException e) {
			return pos;
		}
	}

	/*
	 * Next token is try keyword. Returns the offset after 'try' keyword. For incomplete code, return the start offset.
	 */
	private int getPosAfterTry(int pos) {
		try {
			int nextToken= getScanner().readNext(pos, true);
			if (nextToken == TerminalTokens.TokenNametry) {
				return getScanner().getCurrentEndOffset();
			}
		} catch (CoreException e) {
			handleException(e);
		}
		return pos;
	}

	final int getIndent(int offset) {
		return this.formatter.computeIndentUnits(getIndentOfLine(offset));
	}

	final void doTextInsert(int insertOffset, ASTNode node, int initialIndentLevel, boolean removeLeadingIndent, TextEditGroup editGroup) {
		ArrayList markers= new ArrayList();
		String formatted= this.formatter.getFormattedResult(node, initialIndentLevel, markers);


		int currPos= 0;
		if (removeLeadingIndent) {
			while (currPos < formatted.length() && ScannerHelper.isWhitespace(formatted.charAt(currPos))) {
				currPos++;
			}
		}
		for (int i= 0; i < markers.size(); i++) { // markers.size can change!
			NodeMarker curr= (NodeMarker) markers.get(i);

			int offset= curr.offset;
			if (offset >= currPos) {
				String insertStr= formatted.substring(currPos, offset);
				doTextInsert(insertOffset, insertStr, editGroup); // insert until the marker's begin
			} else {
				// already processed
				continue;
			}

			Object data= curr.data;
			if (data instanceof TextEditGroup) { // tracking a node
				// need to split and create 2 edits as tracking node can surround replaced node.
				TextEdit edit= new RangeMarker(insertOffset, 0);
				addEditGroup((TextEditGroup) data, edit);
				addEdit(edit);
				if (curr.length != 0) {
					int end= offset + curr.length;
					int k= i + 1;
					while (k < markers.size() && ((NodeMarker) markers.get(k)).offset < end) {
						k++;
					}
					curr.offset= end;
					curr.length= 0;
					markers.add(k, curr); // add again for end position
				}
				currPos= offset;
			} else {
				// If in the first line, there are cases (eg: catch clause) where the line will not be prefixed with
				// proper indentation - see https://bugs.eclipse.org/bugs/show_bug.cgi?id=350285
				int lineOffset = getCurrentLineStart(formatted, offset);
				String destIndentString = (lineOffset == 0)
						? this.formatter.createIndentString(initialIndentLevel)
						: this.formatter.getIndentString(formatted.substring(lineOffset, offset));
				if (data instanceof CopyPlaceholderData) { // replace with a copy/move target
					CopySourceInfo copySource= ((CopyPlaceholderData) data).copySource;
					int srcIndentLevel= getIndent(copySource.getNode().getStartPosition());
					TextEdit sourceEdit= getCopySourceEdit(copySource);
					doTextCopy(sourceEdit, insertOffset, srcIndentLevel, destIndentString, editGroup);
					currPos= offset + curr.length; // continue to insert after the replaced string
					if (needsNewLineForLineComment(copySource.getNode(), formatted, currPos)) {
						doTextInsert(insertOffset, getLineDelimiter(), editGroup);
					}
				} else if (data instanceof StringPlaceholderData) { // replace with a placeholder
					String code= ((StringPlaceholderData) data).code;
					String str= this.formatter.changeIndent(code, 0, destIndentString);
					doTextInsert(insertOffset, str, editGroup);
					currPos= offset + curr.length; // continue to insert after the replaced string
				}
			}

		}
		if (currPos < formatted.length()) {
			String insertStr= formatted.substring(currPos);
			doTextInsert(insertOffset, insertStr, editGroup);
		}
	}

	private boolean needsNewLineForLineComment(ASTNode node, String formatted, int offset) {
		if (!this.lineCommentEndOffsets.isEndOfLineComment(getExtendedEnd(node), this.content)) {
			return false;
		}
		// copied code ends with a line comment, but doesn't contain the new line
		return offset < formatted.length() && !IndentManipulation.isLineDelimiterChar(formatted.charAt(offset));
	}

	private int getCurrentLineStart(String str, int pos) {
		for (int i= pos - 1; i>= 0; i--) {
			char ch= str.charAt(i);
			if (IndentManipulation.isLineDelimiterChar(ch)) {
				return i+1;
			}
		}
		return 0;
	}

	private void rewriteModifiers(ASTNode parent, StructuralPropertyDescriptor property, int offset) {
		RewriteEvent event= getEvent(parent, property);
		if (event == null || event.getChangeKind() != RewriteEvent.REPLACED) {
			return;
		}
		try {
			int oldModifiers= ((Integer) event.getOriginalValue()).intValue();
			int newModifiers= ((Integer) event.getNewValue()).intValue();
			TextEditGroup editGroup= getEditGroup(event);

			TokenScanner scanner= getScanner();

			int tok= scanner.readNext(offset, false);
			int startPos= scanner.getCurrentStartOffset();
			int nextStart= startPos;
			loop: while (true) {
				if (TokenScanner.isComment(tok)) {
					tok= scanner.readNext(true); // next non-comment token
				}
				boolean keep= true;
				switch (tok) {
					case TerminalTokens.TokenNamepublic: keep= Modifier.isPublic(newModifiers); break;
					case TerminalTokens.TokenNameprotected: keep= Modifier.isProtected(newModifiers); break;
					case TerminalTokens.TokenNameprivate: keep= Modifier.isPrivate(newModifiers); break;
					case TerminalTokens.TokenNamestatic: keep= Modifier.isStatic(newModifiers); break;
					case TerminalTokens.TokenNamefinal: keep= Modifier.isFinal(newModifiers); break;
					case TerminalTokens.TokenNameabstract: keep= Modifier.isAbstract(newModifiers); break;
					case TerminalTokens.TokenNamenative: keep= Modifier.isNative(newModifiers); break;
					case TerminalTokens.TokenNamevolatile: keep= Modifier.isVolatile(newModifiers); break;
					case TerminalTokens.TokenNamestrictfp: keep= Modifier.isStrictfp(newModifiers); break;
					case TerminalTokens.TokenNametransient: keep= Modifier.isTransient(newModifiers); break;
					case TerminalTokens.TokenNamesynchronized: keep= Modifier.isSynchronized(newModifiers); break;
					case TerminalTokens.TokenNameRestrictedIdentifiersealed: keep= Modifier.isSealed(newModifiers); break;
					case TerminalTokens.TokenNamenon_sealed: keep= Modifier.isNonSealed(newModifiers); break;
					default:
						break loop;
				}
				tok= getScanner().readNext(false); // include comments
				int currPos= nextStart;
				nextStart= getScanner().getCurrentStartOffset();
				if (!keep) {
					doTextRemove(currPos, nextStart - currPos, editGroup);
				}
			}
			int addedModifiers= newModifiers & ~oldModifiers;
			if (addedModifiers != 0) {
				if (startPos != nextStart) {
					int visibilityModifiers= addedModifiers & (Modifier.PUBLIC | Modifier.PRIVATE | Modifier.PROTECTED);
					if (visibilityModifiers != 0) {
						StringBuffer buf= new StringBuffer();
						ASTRewriteFlattener.printModifiers(visibilityModifiers, buf);
						doTextInsert(startPos, buf.toString(), editGroup);
						addedModifiers &= ~visibilityModifiers;
					}
				}
				StringBuffer buf= new StringBuffer();
				ASTRewriteFlattener.printModifiers(addedModifiers, buf);
				doTextInsert(nextStart, buf.toString(), editGroup);
			}
		} catch (CoreException e) {
			handleException(e);
		}
	}

	class ModifierRewriter extends ListRewriter {

		private final Prefix annotationSeparation;

		public ModifierRewriter(Prefix annotationSeparation) {
			this.annotationSeparation= annotationSeparation;
		}

		@Override
		protected String getSeparatorString(int nodeIndex) {
			ASTNode curr= getNewNode(nodeIndex);
			if (curr instanceof Annotation) {
				return this.annotationSeparation.getPrefix(getNodeIndent(nodeIndex + 1));
			}
			return super.getSeparatorString(nodeIndex);
		}
	}

//	private int rewriteExpression2(ASTNode node, ChildListPropertyDescriptor property, int pos) {
//		RewriteEvent event= getEvent(node, property);
//		if (event == null || event.getChangeKind() == RewriteEvent.UNCHANGED) {
//			return doVisit(node, property, pos);
//		}
//		RewriteEvent[] children= event.getChildren();
//		boolean isAllInsert= isAllOfKind(children, RewriteEvent.INSERTED);
//		boolean isAllRemove= isAllOfKind(children, RewriteEvent.REMOVED);
//		String keyword= Util.EMPTY_STRING;
//		if (((SwitchCase)node).isSwitchLabeledRule()) {
//			keyword = "->"; //$NON-NLS-1$
//		} else {
//			keyword = ":"; //$NON-NLS-1$
//		}
//
//		Prefix formatterPrefix = this.formatter.CASE_SEPARATION;
//
//		int endPos= new ModifierRewriter(formatterPrefix).rewriteList(node, property, pos, keyword, " "); //$NON-NLS-1$
//
//		try {
//			int nextPos= getScanner().getNextStartOffset(endPos, false);
//			RewriteEvent lastChild = children[children.length - 1];
//			boolean lastUnchanged= lastChild.getChangeKind() != RewriteEvent.UNCHANGED;
//
//			if (isAllRemove) {
//				doTextRemove(endPos, nextPos - endPos, getEditGroup(lastChild));
//				return nextPos;
//			} else if (isAllInsert || (nextPos == endPos && lastUnchanged)){
//				String separator;
//				if (lastChild.getNewValue() instanceof Annotation) {
//					separator= formatterPrefix.getPrefix(getIndent(pos));
//				} else {
//					separator= String.valueOf(' ');
//				}
//				doTextInsert(endPos, separator, getEditGroup(lastChild));
//			}
//		} catch (CoreException e) {
//			handleException(e);
//		}
//		return endPos;
//	}

	private int rewriteModifiers2(ASTNode node, ChildListPropertyDescriptor property, int pos) {
		RewriteEvent event= getEvent(node, property);
		if (event == null || event.getChangeKind() == RewriteEvent.UNCHANGED) {
			return doVisit(node, property, pos);
		}
		RewriteEvent[] children= event.getChildren();
		boolean isAllInsert= isAllOfKind(children, RewriteEvent.INSERTED);
		boolean isAllRemove= isAllOfKind(children, RewriteEvent.REMOVED);
		String keyword= Util.EMPTY_STRING;
		boolean isVarargsAnnotationsProperty = property == SingleVariableDeclaration.VARARGS_ANNOTATIONS_PROPERTY;
		if (isVarargsAnnotationsProperty) {
			keyword= " "; //$NON-NLS-1$
		} else if (isAllInsert || isAllRemove) {
			// update pos
			try {
				pos= getScanner().getNextStartOffset(pos, false);
			} catch (CoreException e) {
				handleException(e);
			}
		}

		boolean isAnnotationsProperty = isVarargsAnnotationsProperty
				|| node instanceof AnnotatableType && property == ((AnnotatableType) node).getAnnotationsProperty();
		Prefix formatterPrefix;
		if (property == SingleVariableDeclaration.MODIFIERS2_PROPERTY ||
				property == VariableDeclarationExpression.MODIFIERS2_PROPERTY ||
				property == VariableDeclarationStatement.MODIFIERS2_PROPERTY ||
				property == TypeParameter.MODIFIERS_PROPERTY || isAnnotationsProperty) {
			ASTNode parent = node.getParent();
			if (parent instanceof MethodDeclaration)
				formatterPrefix= this.formatter.PARAM_ANNOTATION_SEPARATION;
			else if (parent instanceof Block || parent instanceof TryStatement || parent instanceof ForStatement)
				formatterPrefix= this.formatter.LOCAL_ANNOTATION_SEPARATION;
			else
				formatterPrefix= this.formatter.TYPE_ANNOTATION_SEPARATION;
		} else {
			formatterPrefix= this.formatter.ANNOTATION_SEPARATION;
		}

		int endPos= new ModifierRewriter(formatterPrefix).rewriteList(node, property, pos, keyword, " "); //$NON-NLS-1$

		try {
			int nextPos= getScanner().getNextStartOffset(endPos, false);
			RewriteEvent lastChild = children[children.length - 1];
			boolean lastUnchanged= lastChild.getChangeKind() != RewriteEvent.UNCHANGED;

			if (isAllRemove) {
				doTextRemove(endPos, nextPos - endPos, getEditGroup(lastChild));
				return nextPos;
			} else if ((isAllInsert || (nextPos == endPos && lastUnchanged)) // see bug 165654
					&& !isVarargsAnnotationsProperty) {
				String separator;
				if (lastChild.getNewValue() instanceof Annotation) {
					separator= formatterPrefix.getPrefix(getIndent(pos));
				} else {
					separator= String.valueOf(' ');
				}
				doTextInsert(endPos, separator, getEditGroup(lastChild));
			}
		} catch (CoreException e) {
			handleException(e);
		}
		return endPos;
	}

	private int rewriteTypeAnnotations(ASTNode node, ChildListPropertyDescriptor property, int pos) {
		return rewriteModifiers2(node, property, pos);
	}

	private int rewriteVarargsAnnotations(ASTNode node, ChildListPropertyDescriptor property, int pos) {
		return rewriteModifiers2(node, property, pos);
	}

	private void replaceOperation(int posBeforeOperation, String newOperation, TextEditGroup editGroup) {
		try {
			getScanner().readNext(posBeforeOperation, true);
			doTextReplace(getScanner().getCurrentStartOffset(), getScanner().getCurrentLength(), newOperation, editGroup);
		} catch (CoreException e) {
			handleException(e);
		}
	}

	private void rewriteOperation(ASTNode parent, StructuralPropertyDescriptor property, int posBeforeOperation) {
		RewriteEvent event= getEvent(parent, property);
		if (event != null && event.getChangeKind() != RewriteEvent.UNCHANGED) {
			try {
				String newOperation= event.getNewValue().toString();
				TextEditGroup editGroup= getEditGroup(event);
				getScanner().readNext(posBeforeOperation, true);
				doTextReplace(getScanner().getCurrentStartOffset(), getScanner().getCurrentLength(), newOperation, editGroup);
			} catch (CoreException e) {
				handleException(e);
			}
		}
	}

	@Override
	public void postVisit(ASTNode node) {
		TextEditGroup editGroup= this.eventStore.getTrackedNodeData(node);
		if (editGroup != null) {
			this.currentEdit= this.currentEdit.getParent();
		}
		// remove copy source edits
		doCopySourcePostVisit(node, this.sourceCopyEndNodes);
	}

	@Override
	public void preVisit(ASTNode node) {
		// copies, then range marker

		CopySourceInfo[] infos= this.eventStore.getNodeCopySources(node);
		doCopySourcePreVisit(infos, this.sourceCopyEndNodes);

		TextEditGroup editGroup= this.eventStore.getTrackedNodeData(node);
		if (editGroup != null) {
			SourceRange range= getExtendedRange(node);
			int offset= range.getStartPosition();
			int length= range.getLength();
			TextEdit edit= new RangeMarker(offset, length);
			addEditGroup(editGroup, edit);
			addEdit(edit);
			this.currentEdit= edit;
		}

		ensureSpaceBeforeReplace(node);
	}

	final void doCopySourcePreVisit(CopySourceInfo[] infos, Stack nodeEndStack) {
		if (infos != null) {
			for (int i= 0; i < infos.length; i++) {
				CopySourceInfo curr= infos[i];
				TextEdit edit= getCopySourceEdit(curr);
				addEdit(edit);
				this.currentEdit= edit;
				nodeEndStack.push(curr.getNode());
			}
		}
	}

	final void doCopySourcePostVisit(ASTNode node, Stack nodeEndStack) {
		while (!nodeEndStack.isEmpty() && nodeEndStack.peek() == node) {
			nodeEndStack.pop();
			this.currentEdit= this.currentEdit.getParent();
		}
	}

	@Override
	public boolean visit(CompilationUnit node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}
		int startPos = 0;
		boolean isModuleInfo = node.getAST().apiLevel() >= JLS9_INTERNAL && node.getModule() != null;

		if (!isModuleInfo) {
			startPos = rewriteNode(node, CompilationUnit.PACKAGE_PROPERTY, 0, ASTRewriteFormatter.NONE);

			if (getChangeKind(node, CompilationUnit.PACKAGE_PROPERTY) == RewriteEvent.INSERTED) {
				doTextInsert(0, getLineDelimiter(), getEditGroup(node, CompilationUnit.PACKAGE_PROPERTY));
			}
		}

		startPos = rewriteParagraphList(node, CompilationUnit.IMPORTS_PROPERTY, startPos, 0, 0, 2);
		if (isModuleInfo) {
			rewriteNode(node, CompilationUnit.MODULE_PROPERTY, startPos, ASTRewriteFormatter.NONE);
		} else {
			rewriteParagraphList(node, CompilationUnit.TYPES_PROPERTY, startPos, 0, -1, 2);
		}
		return false;
	}

	@Override
	public boolean visit(TypeDeclaration node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}
		int apiLevel= node.getAST().apiLevel();

		int pos= rewriteJavadoc(node, TypeDeclaration.JAVADOC_PROPERTY);

		boolean isJLS2 = apiLevel == JLS2_INTERNAL;
		if (isJLS2) {
			rewriteModifiers(node, INTERNAL_TYPE_MODIFIERS_PROPERTY, pos);
		} else {
			rewriteModifiers2(node, TypeDeclaration.MODIFIERS2_PROPERTY, pos);
		}

		boolean isInterface= ((Boolean) getOriginalValue(node, TypeDeclaration.INTERFACE_PROPERTY)).booleanValue();
		// modifiers & class/interface
		boolean invertType= isChanged(node, TypeDeclaration.INTERFACE_PROPERTY);
		if (invertType) {
			try {
				int typeToken= isInterface ? TerminalTokens.TokenNameinterface : TerminalTokens.TokenNameclass;
				int startPosition = node.getStartPosition();
				if (!isJLS2) {
					List modifiers = node.modifiers();
					final int size = modifiers.size();
					if (size != 0) {
						ASTNode modifierNode = (ASTNode) modifiers.get(size - 1);
						startPosition = modifierNode.getStartPosition() + modifierNode.getLength();
					}
				}
				getScanner().readToToken(typeToken, startPosition);

				String str= isInterface ? "class" : "interface"; //$NON-NLS-1$ //$NON-NLS-2$
				int start= getScanner().getCurrentStartOffset();
				int end= getScanner().getCurrentEndOffset();

				doTextReplace(start, end - start, str, getEditGroup(node, TypeDeclaration.INTERFACE_PROPERTY));
			} catch (CoreException e) {
				// ignore
			}
		}

		// name
		pos= rewriteRequiredNode(node, TypeDeclaration.NAME_PROPERTY);

		if (!isJLS2) {
			pos= rewriteOptionalTypeParameters(node, TypeDeclaration.TYPE_PARAMETERS_PROPERTY, pos, Util.EMPTY_STRING, false, true);
		}

		// superclass
		if (!isInterface || invertType) {
			ChildPropertyDescriptor superClassProperty= isJLS2 ? INTERNAL_TYPE_SUPERCLASS_PROPERTY : TypeDeclaration.SUPERCLASS_TYPE_PROPERTY;

			RewriteEvent superClassEvent= getEvent(node, superClassProperty);

			int changeKind= superClassEvent != null ? superClassEvent.getChangeKind() : RewriteEvent.UNCHANGED;
			switch (changeKind) {
				case RewriteEvent.INSERTED: {
					doTextInsert(pos, " extends ", getEditGroup(superClassEvent)); //$NON-NLS-1$
					doTextInsert(pos, (ASTNode) superClassEvent.getNewValue(), 0, false, getEditGroup(superClassEvent));
					break;
				}
				case RewriteEvent.REMOVED: {
					ASTNode superClass= (ASTNode) superClassEvent.getOriginalValue();
					int endPos= getExtendedEnd(superClass);
					doTextRemoveAndVisit(pos, endPos - pos, superClass, getEditGroup(superClassEvent));
					pos= endPos;
					break;
				}
				case RewriteEvent.REPLACED: {
					ASTNode superClass= (ASTNode) superClassEvent.getOriginalValue();
					SourceRange range= getExtendedRange(superClass);
					int offset= range.getStartPosition();
					int length= range.getLength();
					doTextRemoveAndVisit(offset, length, superClass, getEditGroup(superClassEvent));
					doTextInsert(offset, (ASTNode) superClassEvent.getNewValue(), 0, false, getEditGroup(superClassEvent));
					pos= offset + length;
					break;
				}
				case RewriteEvent.UNCHANGED: {
					pos= doVisit(node, superClassProperty, pos);
				}
			}
		}
		// extended interfaces
		ChildListPropertyDescriptor superInterfaceProperty= isJLS2 ? INTERNAL_TYPE_SUPER_INTERFACES_PROPERTY : TypeDeclaration.SUPER_INTERFACE_TYPES_PROPERTY;

		RewriteEvent interfaceEvent= getEvent(node, superInterfaceProperty);
		if (interfaceEvent == null || interfaceEvent.getChangeKind() == RewriteEvent.UNCHANGED) {
			if (invertType) {
				List originalNodes= (List) getOriginalValue(node, superInterfaceProperty);
				if (!originalNodes.isEmpty()) {
					String keyword= isInterface ? " implements " : " extends "; //$NON-NLS-1$ //$NON-NLS-2$
					ASTNode firstNode= (ASTNode) originalNodes.get(0);
					doTextReplace(pos, firstNode.getStartPosition() - pos, keyword, getEditGroup(node, TypeDeclaration.INTERFACE_PROPERTY));
				}
			}
			pos= doVisit(node, superInterfaceProperty, pos);
		} else {
			String keyword= (isInterface == invertType) ? " implements " : " extends "; //$NON-NLS-1$ //$NON-NLS-2$
			if (invertType) {
				List newNodes= (List) interfaceEvent.getNewValue();
				if (!newNodes.isEmpty()) {
					List origNodes= (List) interfaceEvent.getOriginalValue();
					int firstStart= pos;
					if (!origNodes.isEmpty()) {
						firstStart= ((ASTNode) origNodes.get(0)).getStartPosition();
					}
					doTextReplace(pos, firstStart - pos, keyword, getEditGroup(node, TypeDeclaration.INTERFACE_PROPERTY));
					keyword= Util.EMPTY_STRING;
					pos= firstStart;
				}
			}
			pos= rewriteNodeList(node, superInterfaceProperty, pos, keyword, ", "); //$NON-NLS-1$
		}

		if (DOMASTUtil.isFeatureSupportedinAST(node.getAST(), Modifier.SEALED)) {
			// permits for sealed
			ChildListPropertyDescriptor permitsProperty= TypeDeclaration.PERMITS_TYPES_PROPERTY;

			RewriteEvent permitsEvent= getEvent(node, permitsProperty);
			if (permitsEvent == null || permitsEvent.getChangeKind() == RewriteEvent.UNCHANGED) {
				pos= doVisit(node, permitsProperty, pos);
			} else {
				pos= rewriteNodeList(node, permitsProperty, pos, " permits ", ", "); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}

		// type members
		// startPos : find position after left brace of type, be aware that bracket might be missing
		int startIndent= getIndent(node.getStartPosition()) + 1;
		int startPos= getPosAfterLeftBrace(pos);
		rewriteParagraphList(node, TypeDeclaration.BODY_DECLARATIONS_PROPERTY, startPos, startIndent, -1, 2);
		return false;
	}

	private void rewriteReturnType(MethodDeclaration node, boolean isConstructor, boolean isConstructorChange) {
		ChildPropertyDescriptor property= (node.getAST().apiLevel() == JLS2_INTERNAL) ? INTERNAL_METHOD_RETURN_TYPE_PROPERTY : MethodDeclaration.RETURN_TYPE2_PROPERTY;

		// weakness in the AST: return type can exist, even if missing in source
		ASTNode originalReturnType= (ASTNode) getOriginalValue(node, property);
		boolean returnTypeExists=  originalReturnType != null && originalReturnType.getStartPosition() != -1;
		if (!isConstructorChange && returnTypeExists) {
			rewriteRequiredNode(node, property);
			ensureSpaceAfterReplace(node, property);
			return;
		}
		// difficult cases: return type insert or remove
		ASTNode newReturnType= (ASTNode) getNewValue(node, property);
		if (isConstructorChange || !returnTypeExists && newReturnType != originalReturnType) {
			// use the start offset of the method name to insert
			ASTNode originalMethodName= (ASTNode) getOriginalValue(node, MethodDeclaration.NAME_PROPERTY);
			int nextStart= originalMethodName.getStartPosition(); // see bug 84049: can't use extended offset
			TextEditGroup editGroup= getEditGroup(node, property);
			if (isConstructor || !returnTypeExists) { // insert
				doTextInsert(nextStart, newReturnType, getIndent(nextStart), true, editGroup);
				doTextInsert(nextStart, " ", editGroup); //$NON-NLS-1$
			} else { // remove up to the method name
				int offset= getExtendedOffset(originalReturnType);
				doTextRemoveAndVisit(offset, nextStart - offset, originalReturnType, editGroup);
			}
		}
	}

	private int rewriteMethodReceiver(MethodDeclaration method, int offset) throws CoreException {
		offset= getScanner().getTokenEndOffset(TerminalTokens.TokenNameLPAREN, offset);
		if (method.getAST().apiLevel() < JLS8_INTERNAL) {
			return offset;
		}

		int newParamCount = ((List) getNewValue(method, MethodDeclaration.PARAMETERS_PROPERTY)).size();
		int oldParamCount = method.parameters().size();
		RewriteEvent event = getEvent(method, MethodDeclaration.RECEIVER_TYPE_PROPERTY);
		RewriteEvent qualEvent = getEvent(method, MethodDeclaration.RECEIVER_QUALIFIER_PROPERTY);

		boolean rewriteQualifier = false;
		ASTNode newQual = null;
		ASTNode oldQual = null;
		if (qualEvent != null) {
			newQual = (ASTNode) qualEvent.getNewValue();
			oldQual = (ASTNode) qualEvent.getOriginalValue();
		}

		TextEditGroup editGroup= getEditGroup(event);
		if (event != null && event.getChangeKind() != RewriteEvent.UNCHANGED) {
			int changeKind= event.getChangeKind();
			if (changeKind == RewriteEvent.INSERTED) {
				 doTextInsert(offset, (ASTNode) event.getNewValue(), 0, false, editGroup);
				doTextInsert(offset, " ", editGroup); //$NON-NLS-1$
				if (newQual != null ) {
					doTextInsert(offset, newQual, 0, false, getEditGroup(qualEvent));
					doTextInsert(offset, ".", editGroup); //$NON-NLS-1$
				}
				doTextInsert(offset, "this", editGroup); //$NON-NLS-1$
				if (newParamCount > 0) {
					doTextInsert(offset, ", ", editGroup); //$NON-NLS-1$
				}
			} else {
				ASTNode elem= (ASTNode) event.getOriginalValue();
				SourceRange range= getExtendedRange(elem);
				int elemOffset= range.getStartPosition();
				int elemLength= range.getLength();
				int elemEnd= elemOffset + elemLength;
				if (changeKind == RewriteEvent.REMOVED) {
					editGroup= getEditGroup(event);
					int endPos;
					if (oldParamCount == 0) {
						endPos= getScanner().getTokenStartOffset(TerminalTokens.TokenNameRPAREN, elemEnd);
					} else {
						endPos= getScanner().getTokenEndOffset(TerminalTokens.TokenNameCOMMA, elemEnd);
					}
					doTextRemoveAndVisit(offset, endPos - offset, elem, editGroup);
					return endPos;
				} else if (changeKind == RewriteEvent.REPLACED) {
					editGroup= getEditGroup(event);
					doTextRemoveAndVisit(elemOffset, elemLength, elem, editGroup);
					doTextInsert(elemOffset, (ASTNode) event.getNewValue(), 0, false, editGroup);
					rewriteQualifier = true;
				}
			}
		} else {
			rewriteRequiredNode(method, MethodDeclaration.RECEIVER_TYPE_PROPERTY);
			if (method.getReceiverType() != null) {
				rewriteQualifier = true;
			}
		}
		if (rewriteQualifier) {
			if (qualEvent != null) {
				int qualChangeKind = qualEvent.getChangeKind();
				TextEditGroup qualGroup = getEditGroup(qualEvent);
				if (qualChangeKind == RewriteEvent.INSERTED) {
					int pos= getScanner().getTokenStartOffset(TerminalTokens.TokenNamethis, offset);
					doTextInsert(pos, (ASTNode) qualEvent.getNewValue(), 0, false, qualGroup);
					doTextInsert(pos, ".", qualGroup); //$NON-NLS-1$
				} else if (qualChangeKind == RewriteEvent.REMOVED) {
					int qualOffset = oldQual.getStartPosition();
					int endPos= getScanner().getTokenEndOffset(TerminalTokens.TokenNameDOT, qualOffset);
					doTextRemove(qualOffset, endPos - qualOffset, qualGroup);
				} else if (qualChangeKind == RewriteEvent.REPLACED) {
					SourceRange range= getExtendedRange(oldQual);
					int elemOffset= range.getStartPosition();
					int elemLength= range.getLength();
					doTextRemoveAndVisit(elemOffset, elemLength, oldQual, qualGroup);
					doTextInsert(elemOffset, newQual, 0, false, qualGroup);
				}
			}
			offset=  getScanner().getTokenEndOffset(TerminalTokens.TokenNamethis, offset);
			if (newParamCount > 0 && oldParamCount == 0) {
				doTextInsert(offset, ", ", editGroup); //$NON-NLS-1$
			}
		}
		return offset;
	}

	private ASTNode getPreviousNode(ASTNode node) {
		ArrayType arrayType = (ArrayType) node.getParent();
		ASTNode prev = arrayType.getElementType();
		List l = (List) node.getParent().getStructuralProperty(node.getLocationInParent());
		int size = l.size();
		int i = 0;
		for (; i < size; i++) {
			ASTNode currNode = (ASTNode) l.get(i);
			if (node == currNode) break;
			prev = currNode;
		}
		return i < size ? prev : null;
	}

	private int getPreviousTokenEndOffset(ASTNode node, int token) {
		int offset = -1;
		ASTNode prev = getPreviousNode(node);
		if (prev != null) {
			offset = prev.getStartPosition() + prev.getLength();
			try {
				offset = getScanner().getPreviousTokenEndOffset(token, offset);
			} catch (CoreException e1) {
				e1.printStackTrace();
			}
		}
		return offset;
	}

	@Override
	public boolean visit(Dimension node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}

		boolean keywordSpace = true;
		ASTNode parent = node.getParent();
		if (parent.getNodeType() == ASTNode.ARRAY_TYPE) {
			List oldAnnotations = (List) getOriginalValue(node, Dimension.ANNOTATIONS_PROPERTY);
			List newAnnotations = (List) getNewValue(node, Dimension.ANNOTATIONS_PROPERTY);
			int oldAnnotationSize = oldAnnotations.size();
			int newAnnotationSize = newAnnotations.size();
			if (oldAnnotationSize > 0 && newAnnotationSize == 0) { /* removed all annotations */
				int delStart = getPreviousTokenEndOffset(node, TerminalTokens.TokenNameAT);
				int delEnd = ((ASTNode) oldAnnotations.get(0)).getStartPosition();
				if (delStart >= 0 && delEnd > delStart) {
					doTextRemove(delStart, delEnd - delStart, null); /* remove spaces before the annotation */
				}

				ASTNode currNode = (ASTNode) oldAnnotations.get(oldAnnotationSize - 1);
				delStart = currNode.getStartPosition() + currNode.getLength();
				try {
					delEnd = getScanner().getNextStartOffset(delStart, false);
					doTextRemove(delStart, delEnd - delStart, null); /* remove spaces after the annotation */
				} catch (CoreException e) {
					e.printStackTrace();
				}
			} else if (oldAnnotationSize == 0 && newAnnotationSize > 0) { /* inserting first annotation */
				if (ScannerHelper.isWhitespace(this.content[node.getStartPosition() - 1])) {
					keywordSpace = false; /* space exists already */
				}
			}
		}
		rewriteNodeList(node, Dimension.ANNOTATIONS_PROPERTY, node.getStartPosition(), keywordSpace ? String.valueOf(' ') : Util.EMPTY_STRING, String.valueOf(' '), String.valueOf(' '));
		return false;
	}

	@Override
	public boolean visit(MethodDeclaration node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}
		int pos= rewriteJavadoc(node, MethodDeclaration.JAVADOC_PROPERTY);
		int apiLevel= node.getAST().apiLevel();
		if (apiLevel == JLS2_INTERNAL) {
			rewriteModifiers(node, INTERNAL_METHOD_MODIFIERS_PROPERTY2, pos);
		} else {
			pos= rewriteModifiers2(node, MethodDeclaration.MODIFIERS2_PROPERTY, pos);
			pos= rewriteOptionalTypeParameters(node, MethodDeclaration.TYPE_PARAMETERS_PROPERTY, pos, " ", true, pos != node.getStartPosition()); //$NON-NLS-1$
		}

		boolean isConstructorChange= isChanged(node, MethodDeclaration.CONSTRUCTOR_PROPERTY);
		boolean isConstructor= ((Boolean) getOriginalValue(node, MethodDeclaration.CONSTRUCTOR_PROPERTY)).booleanValue();
		if (!isConstructor || isConstructorChange) {
			rewriteReturnType(node, isConstructor, isConstructorChange);
		}
		// method name
		pos= rewriteRequiredNode(node, MethodDeclaration.NAME_PROPERTY);

		// parameters
		try {
			pos= rewriteMethodReceiver(node, pos);
			pos= rewriteNodeList(node, MethodDeclaration.PARAMETERS_PROPERTY, pos, Util.EMPTY_STRING, ", "); //$NON-NLS-1$

			pos= getScanner().getTokenEndOffset(TerminalTokens.TokenNameRPAREN, pos);
			ChildListPropertyDescriptor exceptionsProperty = apiLevel < JLS8_INTERNAL ? INTERNAL_METHOD_THROWN_EXCEPTIONS_PROPERTY : MethodDeclaration.THROWN_EXCEPTION_TYPES_PROPERTY;

			if (apiLevel < JLS8_INTERNAL) {
				int extraDims= rewriteExtraDimensions(node, INTERNAL_METHOD_EXTRA_DIMENSIONS_PROPERTY, pos);

				boolean hasExceptionChanges= isChanged(node, exceptionsProperty);

				int bodyChangeKind= getChangeKind(node, MethodDeclaration.BODY_PROPERTY);

				if ((extraDims > 0) && (hasExceptionChanges || bodyChangeKind == RewriteEvent.INSERTED || bodyChangeKind == RewriteEvent.REMOVED)) {
					int dim= ((Integer) getOriginalValue(node, INTERNAL_METHOD_EXTRA_DIMENSIONS_PROPERTY)).intValue();
					while (dim > 0) {
						pos= getScanner().getTokenEndOffset(TerminalTokens.TokenNameRBRACKET, pos);
						dim--;
					}
				}
			} else {
				pos= rewriteExtraDimensionsInfo(node, pos, MethodDeclaration.EXTRA_DIMENSIONS2_PROPERTY);
			}

			pos= rewriteNodeList(node, exceptionsProperty, pos, " throws ", ", "); //$NON-NLS-1$ //$NON-NLS-2$

			rewriteMethodBody(node, pos);
		} catch (CoreException e) {
			// ignore
		}
		return false;
	}

	@Override
	public boolean visit(ModuleDeclaration node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}
		int pos= rewriteJavadoc(node, ModuleDeclaration.JAVADOC_PROPERTY);
		pos= rewriteModifiers2(node, ModuleDeclaration.ANNOTATIONS_PROPERTY, pos);

		RewriteEvent event= getEvent(node, ModuleDeclaration.OPEN_PROPERTY);
		if (event != null && event.getChangeKind() != RewriteEvent.UNCHANGED) {
			boolean fakeInModule = getScanner().getScanner().fakeInModule;
			try {
				boolean wasOpen= ((Boolean) event.getOriginalValue()).booleanValue();
				if (wasOpen) {
					this.tokenScanner.getScanner().fakeInModule = true;
					int endPos= getScanner().getTokenStartOffset(TerminalTokens.TokenNamemodule, pos);
					doTextRemove(pos, endPos - pos, getEditGroup(event));
				} else {
					doTextInsert(pos, "open ", getEditGroup(event)); //$NON-NLS-1$
				}
			} catch (CoreException e) {
				handleException(e);
			} finally {
				this.tokenScanner.getScanner().fakeInModule = fakeInModule;
			}
		}

		pos= rewriteRequiredNode(node, ModuleDeclaration.NAME_PROPERTY);
		int startPos = getPosAfterLeftBrace(pos);
		int startIndent= getIndent(node.getStartPosition()) + 1;
		boolean fakeInModule = this.tokenScanner.getScanner().fakeInModule;
		this.tokenScanner.getScanner().fakeInModule = true;
		rewriteParagraphList(node, ModuleDeclaration.MODULE_DIRECTIVES_PROPERTY, startPos, startIndent, 0, 1);
		this.tokenScanner.getScanner().fakeInModule = fakeInModule;
		return false;
	}

	@Override
	public boolean visit(Block node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}

		int startPos;
		if (isCollapsed(node)) {
			startPos= node.getStartPosition();
		} else {
			startPos= getPosAfterLeftBrace(node.getStartPosition());
		}
		int startIndent= getIndent(node.getStartPosition()) + 1;
		rewriteParagraphList(node, Block.STATEMENTS_PROPERTY, startPos, startIndent, 0, 1);
		return false;
	}

	@Override
	public boolean visit(RecordDeclaration node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}
		int pos= rewriteJavadoc(node, RecordDeclaration.JAVADOC_PROPERTY);

		rewriteModifiers2(node, RecordDeclaration.MODIFIERS2_PROPERTY, pos);

		// name
		pos= rewriteRequiredNode(node, RecordDeclaration.NAME_PROPERTY);

		pos= rewriteOptionalTypeParameters(node, RecordDeclaration.TYPE_PARAMETERS_PROPERTY, pos, Util.EMPTY_STRING, false, true);

		try {
			pos= getScanner().getTokenEndOffset(TerminalTokens.TokenNameLPAREN, pos);
		}catch(CoreException ex) {
				//ignore
		}


		pos= rewriteNodeList(node, RecordDeclaration.RECORD_COMPONENTS_PROPERTY, pos, Util.EMPTY_STRING, ", "); //$NON-NLS-1$

		try {
		pos= getScanner().getTokenEndOffset(TerminalTokens.TokenNameRPAREN, pos);
		}catch(CoreException ex) {
			//ignore
		}

		// extended interfaces
		ChildListPropertyDescriptor superInterfaceProperty= RecordDeclaration.SUPER_INTERFACE_TYPES_PROPERTY;

		RewriteEvent interfaceEvent= getEvent(node, superInterfaceProperty);
		if (interfaceEvent == null || interfaceEvent.getChangeKind() == RewriteEvent.UNCHANGED) {
			pos= doVisit(node, superInterfaceProperty, pos);
		} else {
			String keyword=  " implements "; //$NON-NLS-1$
			pos= rewriteNodeList(node, superInterfaceProperty, pos, keyword, ", "); //$NON-NLS-1$
		}

		// type members
		// startPos : find position after left brace of type, be aware that bracket might be missing

		int startIndent= getIndent(node.getStartPosition()) + 1;
		pos = getPosAfterRightParenthesis(pos);
		pos= getPosAfterLeftBrace(pos);
		rewriteParagraphList(node, RecordDeclaration.BODY_DECLARATIONS_PROPERTY, pos, startIndent, -1, 2);
		return false;
	}

	@Override
	public boolean visit(ReturnStatement node) {
		try {
			this.beforeRequiredSpaceIndex = getScanner().getTokenEndOffset(TerminalTokens.TokenNamereturn, node.getStartPosition());

			if (!hasChildrenChanges(node)) {
				return doVisitUnchangedChildren(node);
			}

			ensureSpaceBeforeReplace(node);

			rewriteNode(node, ReturnStatement.EXPRESSION_PROPERTY, this.beforeRequiredSpaceIndex, ASTRewriteFormatter.SPACE);
		} catch (CoreException e) {
			handleException(e);
		}
		return false;
	}

	@Override
	public boolean visit(RequiresDirective node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}
		int pos = getPosAfterToken(node.getStartPosition(), TerminalTokens.TokenNamerequires);
		rewriteNodeList(node, RequiresDirective.MODIFIERS_PROPERTY, pos, String.valueOf(' '), String.valueOf(' '));
		rewriteRequiredNode(node, RequiresDirective.NAME_PROPERTY);
		return false;
	}

	@Override
	public boolean visit(AnonymousClassDeclaration node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}

		int startPos= getPosAfterLeftBrace(node.getStartPosition());
		int startIndent= getIndent(node.getStartPosition()) + 1;
		rewriteParagraphList(node, AnonymousClassDeclaration.BODY_DECLARATIONS_PROPERTY, startPos, startIndent, -1, 2);
		return false;
	}

	@Override
	public boolean visit(ArrayAccess node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}

		rewriteRequiredNode(node, ArrayAccess.ARRAY_PROPERTY);
		rewriteRequiredNode(node, ArrayAccess.INDEX_PROPERTY);
		return false;
	}

	@Override
	public boolean visit(ArrayCreation node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}

		ArrayType arrayType= (ArrayType) getOriginalValue(node, ArrayCreation.TYPE_PROPERTY);
		ArrayType replacingType= arrayType;
		int nOldBrackets= getDimensions(arrayType); // number of total brackets
		boolean astLevelGTE8 = node.getAST().apiLevel() >= JLS8_INTERNAL;
		boolean typeReplaced = false;

		TextEditGroup editGroup= null;
		RewriteEvent typeEvent= getEvent(node, ArrayCreation.TYPE_PROPERTY);
		if (typeEvent != null && typeEvent.getChangeKind() == RewriteEvent.REPLACED) { // changed arraytype can have different dimension or type name
			typeReplaced = true;
			replacingType= (ArrayType) typeEvent.getNewValue();
			editGroup= getEditGroup(typeEvent);
			Type newType= replacingType.getElementType();
			Type oldType= getElementType(arrayType);
			if (!newType.equals(oldType)) {
				SourceRange range= getExtendedRange(oldType);
				int offset= range.getStartPosition();
				int length= range.getLength();
				doTextRemove(offset, length, editGroup);
				doTextInsert(offset, newType, 0, false, editGroup);
			}
		}

		try {
			// dimension node with expressions and/or annotations
			RewriteEvent dimEvent= getEvent(node, ArrayCreation.DIMENSIONS_PROPERTY);
			boolean hasDimensionChanges= (dimEvent != null && dimEvent.getChangeKind() != RewriteEvent.UNCHANGED);
			RewriteEvent[] events= hasDimensionChanges ? dimEvent.getChildren() : null;
			ArrayType currentLevel = astLevelGTE8 ? null : (ArrayType) replacingType.getElementType().getParent();
			int replacingTypeDimensions = replacingType.getDimensions();
			int i=0, dimSize= (events == null) ? 0 : events.length;
			Type elementType= arrayType.getElementType();
			int offset= elementType.getStartPosition() + elementType.getLength();
			editGroup = null;
			while(currentLevel != null || astLevelGTE8) {
				if (i < dimSize) {
					if (astLevelGTE8)
						rewriteAnnotationsOnDimension(arrayType, replacingType, i, offset, typeReplaced, editGroup);
					offset= getScanner().getTokenEndOffset(TerminalTokens.TokenNameLBRACKET, offset);
					if (hasDimensionChanges) {
						RewriteEvent event= events[i];
						int changeKind= event.getChangeKind();
						if (changeKind == RewriteEvent.INSERTED) { // insert new dimension
							editGroup= getEditGroup(event);
							int endPos= getScanner().getTokenStartOffset(TerminalTokens.TokenNameRBRACKET, offset);
							doTextRemove(offset, endPos - offset, editGroup);
							doTextInsert(offset, (ASTNode) event.getNewValue(), 0, false, editGroup);
						} else {
							ASTNode elem= (ASTNode) event.getOriginalValue();
							int elemEnd= elem.getStartPosition() + elem.getLength();
							int endPos= getScanner().getTokenStartOffset(TerminalTokens.TokenNameRBRACKET, elemEnd);
							if (changeKind == RewriteEvent.REMOVED) {
								editGroup= getEditGroup(event);
								doTextRemoveAndVisit(offset, endPos - offset, elem, editGroup);
							} else if (changeKind == RewriteEvent.REPLACED) {
								editGroup= getEditGroup(event);
								SourceRange range= getExtendedRange(elem);
								int elemOffset= range.getStartPosition();
								int elemLength= range.getLength();
								doTextRemoveAndVisit(elemOffset, elemLength, elem, editGroup);
								doTextInsert(elemOffset, (ASTNode) event.getNewValue(), 0, false, editGroup);
							} else {
								voidVisit(elem);
							}
						}
						offset= retrieveRightBracketEndPosition(offset, 1, true);
					} else {
						ASTNode elem= (ASTNode) node.dimensions().get(i);
						voidVisit(elem);
						offset= retrieveRightBracketEndPosition(offset, 1, true);
					}
				} else if (i < nOldBrackets) {
					if (astLevelGTE8)
						rewriteAnnotationsOnDimension(arrayType, replacingType, i, offset, typeReplaced, editGroup);
					offset= retrieveRightBracketEndPosition(offset, 1, false);
				} else {
					insertAnnotationsOnDimension(replacingType, i, offset, editGroup, astLevelGTE8);
					doTextInsert(offset, "[]", editGroup); //$NON-NLS-1$
				}
				i++;
				if (astLevelGTE8) {
					if (i == replacingTypeDimensions) break;
				} else {
					if (currentLevel == replacingType) break;
					currentLevel= (ArrayType) currentLevel.getParent();
				}
			}
			if (i < nOldBrackets) {
				int endPos= retrieveRightBracketEndPosition(offset, nOldBrackets - i, false);
				doTextRemove(offset, endPos - offset, editGroup);
			}

			int kind= getChangeKind(node, ArrayCreation.INITIALIZER_PROPERTY);
			if (kind == RewriteEvent.REMOVED) {
				offset= getScanner().getPreviousTokenEndOffset(TerminalTokens.TokenNameLBRACE, offset);
			} else {
				offset= node.getStartPosition() + node.getLength(); // insert pos
			}
			rewriteNode(node, ArrayCreation.INITIALIZER_PROPERTY, offset, ASTRewriteFormatter.SPACE);
		} catch (CoreException e) {
			handleException(e);
		}
		return false;
	}

	private void insertAnnotationsOnDimension(ArrayType replacingType, int index, int pos, TextEditGroup editGroup,
			boolean astLevelGTE8) {
		if (astLevelGTE8) {
			Dimension dim = (Dimension) replacingType.dimensions().get(index);
			List annotations = dim.annotations();
			if (annotations != null) {
				int size = annotations.size();
				if (size > 0) {
					doTextInsert(pos, " ", editGroup); //$NON-NLS-1$
					for (int j = 0; j < size; j++) {
						Annotation annotation = (Annotation) annotations.get(j);
						doTextInsert(pos, annotation.toString() + " ", editGroup); //$NON-NLS-1$
					}
				}
			}
		}
	}

	private void rewriteAnnotationsOnDimension(ArrayType oldArrayType, ArrayType replacingType, int index, int pos, boolean typeReplaced, TextEditGroup editGroup) throws CoreException {
		if (typeReplaced) {
			List dimensions = oldArrayType.dimensions();
			Dimension oldDim = index < dimensions.size() ? (Dimension) dimensions.get(index) : null;
			if (oldDim != null) {
				List oldAnnotations = oldDim.annotations();
				int size = oldAnnotations.size();
				if (size > 0) {
					ASTNode prev = getPreviousNode(oldDim);
					Annotation annotation = (Annotation) oldAnnotations.get(0);
					int start = prev != null ? prev.getStartPosition() + prev.getLength() : annotation.getStartPosition();
					annotation = (Annotation) oldAnnotations.get(size - 1);
					int end = annotation.getStartPosition() + annotation.getLength();
					end = getScanner().getTokenEndOffset(TerminalTokens.TokenNameLBRACKET, end) - 1;
					doTextRemove(start, end - start, editGroup);
				}
			}
			insertAnnotationsOnDimension(replacingType, index, pos, editGroup, true);
		} else {
			Dimension dim = (Dimension) replacingType.dimensions().get(index);
			rewriteNodeList(dim, Dimension.ANNOTATIONS_PROPERTY, pos, String.valueOf(' '), String.valueOf(' '), String.valueOf(' '));
		}
	}

	/**
	 * This method is used to retrieve the position of the right bracket.
	 * @return int the dimension found, -1 if none
	 */
	protected int retrieveRightBracketEndPosition(int offset, int count, boolean isLeftRead) throws CoreException {
		TokenScanner scanner= getScanner();
		int token;
		int balance= 0;
		if (isLeftRead) balance++;
		scanner.setOffset(offset);
		while ((token= scanner.readNext(true)) != TerminalTokens.TokenNameEOF) {
			switch(token) {
				case TerminalTokens.TokenNameLBRACKET :
					balance++;
					break;
				case TerminalTokens.TokenNameRBRACKET :
					balance--;
					if (balance == 0) {
						if (--count == 0) {
							return scanner.getCurrentEndOffset();
						}
					}
					break;
			}
		}
		return -1;
	}

	private Type getElementType(ArrayType parent) {
		if (parent.getAST().apiLevel() >= JLS8_INTERNAL) {
			return (Type) getOriginalValue(parent, ArrayType.ELEMENT_TYPE_PROPERTY);
		}
		Type t = (Type) getOriginalValue(parent, INTERNAL_ARRAY_COMPONENT_TYPE_PROPERTY);
		while (t.isArrayType()) {
			t = (Type) getOriginalValue(t, INTERNAL_ARRAY_COMPONENT_TYPE_PROPERTY);
		}
		return t;
	}

	private int getDimensions(ArrayType parent) {
		if (parent.getAST().apiLevel() >= JLS8_INTERNAL) {
			return ((List) getOriginalValue(parent, ArrayType.DIMENSIONS_PROPERTY)).size();
		}
		Type t = (Type) getOriginalValue(parent, INTERNAL_ARRAY_COMPONENT_TYPE_PROPERTY);
		int dimensions = 1; // always include this array type
		while (t.isArrayType()) {
			dimensions++;
			t = (Type) getOriginalValue(t, INTERNAL_ARRAY_COMPONENT_TYPE_PROPERTY);
		}
		return dimensions;
	}

	@Override
	public boolean visit(ArrayInitializer node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}

		int startPos= getPosAfterLeftBrace(node.getStartPosition());
		rewriteNodeList(node, ArrayInitializer.EXPRESSIONS_PROPERTY, startPos, Util.EMPTY_STRING, ", "); //$NON-NLS-1$
		return false;
	}

	private int rewriteArrayTypeDimensions(ArrayType node, int pos) {
		return rewriteNodeList(node, ArrayType.DIMENSIONS_PROPERTY, pos, Util.EMPTY_STRING, ""); //$NON-NLS-1$
	}

	@Override
	public boolean visit(ArrayType node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}
		if (node.getAST().apiLevel() < JLS8_INTERNAL) {
			rewriteRequiredNode(node, INTERNAL_ARRAY_COMPONENT_TYPE_PROPERTY);
		} else {
			int pos = rewriteRequiredNode(node, ArrayType.ELEMENT_TYPE_PROPERTY);
			rewriteArrayTypeDimensions(node, pos);
		}
		return false;
	}

	@Override
	public boolean visit(AssertStatement node) {
		try {
			this.beforeRequiredSpaceIndex = getScanner().getNextEndOffset(node.getStartPosition(), true);

			if (!hasChildrenChanges(node)) {
				return doVisitUnchangedChildren(node);
			}

			ensureSpaceBeforeReplace(node);

			int offset= rewriteRequiredNode(node, AssertStatement.EXPRESSION_PROPERTY);
			rewriteNode(node, AssertStatement.MESSAGE_PROPERTY, offset, ASTRewriteFormatter.ASSERT_COMMENT);
		} catch (CoreException e) {
			handleException(e);
		}
		return false;
	}

	@Override
	public boolean visit(Assignment node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}

		int pos= rewriteRequiredNode(node, Assignment.LEFT_HAND_SIDE_PROPERTY);
		rewriteOperation(node, Assignment.OPERATOR_PROPERTY, pos);
		rewriteRequiredNode(node, Assignment.RIGHT_HAND_SIDE_PROPERTY);
		return false;
	}

	@Override
	public boolean visit(BooleanLiteral node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}

		Boolean newLiteral= (Boolean) getNewValue(node, BooleanLiteral.BOOLEAN_VALUE_PROPERTY);
		TextEditGroup group = getEditGroup(node, BooleanLiteral.BOOLEAN_VALUE_PROPERTY);
		doTextReplace(node.getStartPosition(), node.getLength(), newLiteral.toString(), group);
		return false;
	}

	@Override
	public boolean visit(BreakStatement node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}

		try {
			int offset= getScanner().getTokenEndOffset(TerminalTokens.TokenNamebreak, node.getStartPosition());
			rewriteNode(node, BreakStatement.LABEL_PROPERTY, offset, ASTRewriteFormatter.SPACE); // space between break and label
		} catch (CoreException e) {
			handleException(e);
		}
		return false;
	}

	@Override
	public boolean visit(CaseDefaultExpression node) {
		if (!DOMASTUtil.isPatternSupported(node.getAST())) {
			return false;
		}
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}
		changeNotSupported(node); // no modification possible
		return false;
	}

	@Override
	public boolean visit(CastExpression node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}

		rewriteRequiredNode(node, CastExpression.TYPE_PROPERTY);
		rewriteRequiredNode(node, CastExpression.EXPRESSION_PROPERTY);
		return false;
	}

	@Override
	public boolean visit(CatchClause node) { // catch (Exception) Block
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}

		rewriteRequiredNode(node, CatchClause.EXCEPTION_PROPERTY);
		rewriteRequiredNode(node, CatchClause.BODY_PROPERTY);
		return false;
	}

	@Override
	public boolean visit(CharacterLiteral node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}

		String escapedSeq= (String) getNewValue(node, CharacterLiteral.ESCAPED_VALUE_PROPERTY);
		TextEditGroup group = getEditGroup(node, CharacterLiteral.ESCAPED_VALUE_PROPERTY);
		doTextReplace(node.getStartPosition(), node.getLength(), escapedSeq, group);
		return false;
	}

	@Override
	public boolean visit(ClassInstanceCreation node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}

		int pos= rewriteOptionalQualifier(node, ClassInstanceCreation.EXPRESSION_PROPERTY, node.getStartPosition());
		if (node.getAST().apiLevel() == JLS2_INTERNAL) {
			pos= rewriteRequiredNode(node, INTERNAL_CIC_NAME_PROPERTY);
		} else {
			if (isChanged(node, ClassInstanceCreation.TYPE_ARGUMENTS_PROPERTY)) {
				try {
					pos= getScanner().getTokenEndOffset(TerminalTokens.TokenNamenew, pos); //after 'new'
					rewriteOptionalTypeParameters(node, ClassInstanceCreation.TYPE_ARGUMENTS_PROPERTY, pos, " ", true, true); //$NON-NLS-1$
				} catch (CoreException e) {
					handleException(e);
				}
			} else {
				voidVisit(node, ClassInstanceCreation.TYPE_ARGUMENTS_PROPERTY);
			}
			pos= rewriteRequiredNode(node, ClassInstanceCreation.TYPE_PROPERTY);
		}

		if (isChanged(node, ClassInstanceCreation.ARGUMENTS_PROPERTY)) {
			try {
				int startpos= getScanner().getTokenEndOffset(TerminalTokens.TokenNameLPAREN, pos);
				rewriteNodeList(node, ClassInstanceCreation.ARGUMENTS_PROPERTY, startpos, Util.EMPTY_STRING, ", "); //$NON-NLS-1$
			} catch (CoreException e) {
				handleException(e);
			}
		} else {
			voidVisit(node, ClassInstanceCreation.ARGUMENTS_PROPERTY);
		}

		int kind= getChangeKind(node, ClassInstanceCreation.ANONYMOUS_CLASS_DECLARATION_PROPERTY);
		if (kind == RewriteEvent.REMOVED) {
			try {
				pos= getScanner().getPreviousTokenEndOffset(TerminalTokens.TokenNameLBRACE, pos);
			} catch (CoreException e) {
				handleException(e);
			}
		} else {
			pos= node.getStartPosition() + node.getLength(); // insert pos
		}
		rewriteNode(node, ClassInstanceCreation.ANONYMOUS_CLASS_DECLARATION_PROPERTY, pos, ASTRewriteFormatter.SPACE);
		return false;
	}

	@Override
	public boolean visit(ConditionalExpression node) { // expression ? thenExpression : elseExpression
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}

		rewriteRequiredNode(node, ConditionalExpression.EXPRESSION_PROPERTY);
		rewriteRequiredNode(node, ConditionalExpression.THEN_EXPRESSION_PROPERTY);
		rewriteRequiredNode(node, ConditionalExpression.ELSE_EXPRESSION_PROPERTY);
		return false;
	}

	@Override
	public boolean visit(ConstructorInvocation node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}
		int pos= node.getStartPosition();
		if (node.getAST().apiLevel() >= JLS3_INTERNAL) {
			pos= rewriteOptionalTypeParameters(node, ConstructorInvocation.TYPE_ARGUMENTS_PROPERTY, pos, Util.EMPTY_STRING, false, false);
		}
		try {
			pos= getScanner().getTokenEndOffset(TerminalTokens.TokenNameLPAREN, pos);
			rewriteNodeList(node, ConstructorInvocation.ARGUMENTS_PROPERTY, pos, Util.EMPTY_STRING, ", "); //$NON-NLS-1$
		} catch (CoreException e) {
			handleException(e);
		}
		return false;
	}

	@Override
	public boolean visit(ContinueStatement node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}

		try {
			int offset= getScanner().getTokenEndOffset(TerminalTokens.TokenNamecontinue, node.getStartPosition());
			rewriteNode(node, ContinueStatement.LABEL_PROPERTY, offset, ASTRewriteFormatter.SPACE); // space between continue and label
		} catch (CoreException e) {
			handleException(e);
		}
		return false;
	}

	private void visitReferenceTypeArguments(ASTNode node, StructuralPropertyDescriptor childProperty, int pos) {
		if (isChanged(node, childProperty)) {
			try {
				pos = getScanner().getTokenEndOffset(TerminalTokens.TokenNameCOLON_COLON, pos);
				rewriteOptionalTypeParameters(node, childProperty, pos, Util.EMPTY_STRING, false, false);
			} catch (CoreException e) {
				handleException(e);
			}
		}
	}

	@Override
	public boolean visit(CreationReference node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}
		int pos= rewriteRequiredNode(node, CreationReference.TYPE_PROPERTY);
		visitReferenceTypeArguments(node, CreationReference.TYPE_ARGUMENTS_PROPERTY, pos);
		return false;
	}

	@Override
	public boolean visit(DoStatement node) { // do statement while expression
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}

		int pos= node.getStartPosition();
		try {
			RewriteEvent event= getEvent(node, DoStatement.BODY_PROPERTY);
			if (event != null && event.getChangeKind() == RewriteEvent.REPLACED) {
				int startOffset= getScanner().getTokenEndOffset(TerminalTokens.TokenNamedo, pos);
				ASTNode body= (ASTNode) event.getOriginalValue();
				int bodyEnd= body.getStartPosition() + body.getLength();
				int endPos= getScanner().getTokenStartOffset(TerminalTokens.TokenNamewhile, bodyEnd);
				rewriteBodyNode(node, DoStatement.BODY_PROPERTY, startOffset, endPos, getIndent(node.getStartPosition()), this.formatter.DO_BLOCK); // body
			} else {
				voidVisit(node, DoStatement.BODY_PROPERTY);
			}
		} catch (CoreException e) {
			handleException(e);
		}

		rewriteRequiredNode(node, DoStatement.EXPRESSION_PROPERTY);
		return false;
	}

	@Override
	public boolean visit(EmptyStatement node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}

		changeNotSupported(node); // no modification possible
		return false;
	}

	@Override
	public boolean visit(ExportsDirective node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}
		int pos = rewriteRequiredNode(node, ExportsDirective.NAME_PROPERTY);
		rewriteNodeList(node, ExportsDirective.MODULES_PROPERTY, pos, "to ", ", "); //$NON-NLS-1$ //$NON-NLS-2$

		return false;
	}

	@Override
	public boolean visit(ExpressionStatement node) { // expression
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}

		rewriteRequiredNode(node, ExpressionStatement.EXPRESSION_PROPERTY);
		return false;
	}

	@Override
	public boolean visit(FieldAccess node) { // expression.name
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}

		rewriteRequiredNode(node, FieldAccess.EXPRESSION_PROPERTY); // expression
		rewriteRequiredNode(node, FieldAccess.NAME_PROPERTY); // name
		return false;
	}

	@Override
	public boolean visit(FieldDeclaration node) { //{ Modifier } Type VariableDeclarationFragment { ',' VariableDeclarationFragment } ';'
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}
		int pos= rewriteJavadoc(node, FieldDeclaration.JAVADOC_PROPERTY);

		if (node.getAST().apiLevel() == JLS2_INTERNAL) {
			rewriteModifiers(node, INTERNAL_FIELD_MODIFIERS_PROPERTY, pos);
		} else {
			rewriteModifiers2(node, FieldDeclaration.MODIFIERS2_PROPERTY, pos);
		}

		pos= rewriteRequiredNode(node, FieldDeclaration.TYPE_PROPERTY);
		ensureSpaceAfterReplace(node, FieldDeclaration.TYPE_PROPERTY);
		rewriteNodeList(node, FieldDeclaration.FRAGMENTS_PROPERTY, pos, Util.EMPTY_STRING, ", "); //$NON-NLS-1$
		return false;
	}

	@Override
	public boolean visit(ForStatement node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}

		try {
			int pos= node.getStartPosition();

			if (isChanged(node, ForStatement.INITIALIZERS_PROPERTY)) {
				// position after opening parent
				int startOffset= getScanner().getTokenEndOffset(TerminalTokens.TokenNameLPAREN, pos);
				pos= rewriteNodeList(node, ForStatement.INITIALIZERS_PROPERTY, startOffset, Util.EMPTY_STRING, ", "); //$NON-NLS-1$
			} else {
				pos= doVisit(node, ForStatement.INITIALIZERS_PROPERTY, pos);
			}

			// position after first semicolon
			pos= getScanner().getTokenEndOffset(TerminalTokens.TokenNameSEMICOLON, pos);

			pos= rewriteNode(node, ForStatement.EXPRESSION_PROPERTY, pos, ASTRewriteFormatter.NONE);

			if (isChanged(node, ForStatement.UPDATERS_PROPERTY)) {
				int startOffset= getScanner().getTokenEndOffset(TerminalTokens.TokenNameSEMICOLON, pos);
				pos= rewriteNodeList(node, ForStatement.UPDATERS_PROPERTY, startOffset, Util.EMPTY_STRING, ", "); //$NON-NLS-1$
			} else {
				pos= doVisit(node, ForStatement.UPDATERS_PROPERTY, pos);
			}

			RewriteEvent bodyEvent= getEvent(node, ForStatement.BODY_PROPERTY);
			if (bodyEvent != null && bodyEvent.getChangeKind() == RewriteEvent.REPLACED) {
				int startOffset= getScanner().getTokenEndOffset(TerminalTokens.TokenNameRPAREN, pos);
				rewriteBodyNode(node, ForStatement.BODY_PROPERTY, startOffset, -1, getIndent(node.getStartPosition()), this.formatter.FOR_BLOCK); // body
			} else {
				voidVisit(node, ForStatement.BODY_PROPERTY);
			}

		} catch (CoreException e) {
			handleException(e);
		}


		return false;
	}

	@Override
	public boolean visit(GuardedPattern node) {
		if (!DOMASTUtil.isPatternSupported(node.getAST())) {
			return false;
		}
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}

		rewriteRequiredNode(node, GuardedPattern.PATTERN_PROPERTY);
		rewriteRequiredNode(node, GuardedPattern.EXPRESSION_PROPERTY);

		return false;
	}

	@Override
	public boolean visit(IfStatement node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}

		int pos= rewriteRequiredNode(node, IfStatement.EXPRESSION_PROPERTY); // statement

		RewriteEvent thenEvent= getEvent(node, IfStatement.THEN_STATEMENT_PROPERTY);
		int elseChange= getChangeKind(node, IfStatement.ELSE_STATEMENT_PROPERTY);

		if (thenEvent != null && thenEvent.getChangeKind() != RewriteEvent.UNCHANGED) {
			try {
				int tok= getScanner().readNext(pos, true); // after the closing parent
				pos= (tok == TerminalTokens.TokenNameRPAREN) ? getScanner().getCurrentEndOffset() : getScanner().getCurrentStartOffset();

				int indent= getIndent(node.getStartPosition());

				int endPos= -1;
				Object elseStatement= getOriginalValue(node, IfStatement.ELSE_STATEMENT_PROPERTY);
				if (elseStatement != null) {
					ASTNode thenStatement = (ASTNode) thenEvent.getOriginalValue();
					endPos= getScanner().getTokenStartOffset(TerminalTokens.TokenNameelse, thenStatement.getStartPosition() + thenStatement.getLength()); // else keyword
				}
				if (elseStatement == null || elseChange != RewriteEvent.UNCHANGED) {
					pos= rewriteBodyNode(node, IfStatement.THEN_STATEMENT_PROPERTY, pos, endPos, indent, this.formatter.IF_BLOCK_NO_ELSE);
				} else {
					pos= rewriteBodyNode(node, IfStatement.THEN_STATEMENT_PROPERTY, pos, endPos, indent, this.formatter.IF_BLOCK_WITH_ELSE);
				}
			} catch (CoreException e) {
				handleException(e);
			}
		} else {
			pos= doVisit(node, IfStatement.THEN_STATEMENT_PROPERTY, pos);
		}

		if (elseChange != RewriteEvent.UNCHANGED) {
			int indent= getIndent(node.getStartPosition());
			Object newThen= getNewValue(node, IfStatement.THEN_STATEMENT_PROPERTY);
			if (newThen instanceof Block) {
				rewriteBodyNode(node, IfStatement.ELSE_STATEMENT_PROPERTY, pos, -1, indent, this.formatter.ELSE_AFTER_BLOCK);
			} else {
				rewriteBodyNode(node, IfStatement.ELSE_STATEMENT_PROPERTY, pos, -1, indent, this.formatter.ELSE_AFTER_STATEMENT);
			}
		} else {
			pos= doVisit(node, IfStatement.ELSE_STATEMENT_PROPERTY, pos);
		}
		return false;
	}

	@Override
	public boolean visit(ImportDeclaration node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}
		if (node.getAST().apiLevel() >= JLS3_INTERNAL) {
			RewriteEvent event= getEvent(node, ImportDeclaration.STATIC_PROPERTY);
			if (event != null && event.getChangeKind() != RewriteEvent.UNCHANGED) {
				try {
					int pos= getScanner().getTokenEndOffset(TerminalTokens.TokenNameimport, node.getStartPosition());
					boolean wasStatic= ((Boolean) event.getOriginalValue()).booleanValue();
					if (wasStatic) {
						int endPos= getScanner().getTokenEndOffset(TerminalTokens.TokenNamestatic, pos);
						doTextRemove(pos, endPos - pos, getEditGroup(event));
					} else {
						doTextInsert(pos, " static", getEditGroup(event)); //$NON-NLS-1$
					}
				} catch (CoreException e) {
					handleException(e);
				}
			}
		}

		int pos= rewriteRequiredNode(node, ImportDeclaration.NAME_PROPERTY);

		RewriteEvent event= getEvent(node, ImportDeclaration.ON_DEMAND_PROPERTY);
		if (event != null && event.getChangeKind() != RewriteEvent.UNCHANGED) {
			boolean isOnDemand= ((Boolean) event.getOriginalValue()).booleanValue();
			if (!isOnDemand) {
				doTextInsert(pos, ".*", getEditGroup(event)); //$NON-NLS-1$
			} else {
				try {
					int endPos= getScanner().getTokenStartOffset(TerminalTokens.TokenNameSEMICOLON, pos);
					doTextRemove(pos, endPos - pos, getEditGroup(event));
				} catch (CoreException e) {
					handleException(e);
				}
			}
		}
		return false;
	}

	@Override
	public boolean visit(InfixExpression node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}

		Expression right= node.getRightOperand();
		int pos;

		RewriteEvent leftEvent= getEvent(node, InfixExpression.LEFT_OPERAND_PROPERTY);
		boolean removeLeft = leftEvent != null && leftEvent.getChangeKind() == RewriteEvent.REMOVED;
		RewriteEvent rightEvent= getEvent(node, InfixExpression.RIGHT_OPERAND_PROPERTY);
		boolean removeRight = rightEvent != null && rightEvent.getChangeKind() == RewriteEvent.REMOVED;

		if (removeLeft) {
			Expression left= node.getLeftOperand();
			int leftStart= getExtendedOffset(left);
			pos= getExtendedOffset(right);
			TextEditGroup editGroup= getEditGroup(leftEvent);
			doTextRemoveAndVisit(leftStart, pos - leftStart, left, editGroup);
		} else {
			pos = rewriteRequiredNode(node, InfixExpression.LEFT_OPERAND_PROPERTY);
		}

		boolean needsNewOperation= isChanged(node, InfixExpression.OPERATOR_PROPERTY);
		String operation= getNewValue(node, InfixExpression.OPERATOR_PROPERTY).toString();
		if (needsNewOperation && !removeLeft && !removeRight) {
			replaceOperation(pos, operation, getEditGroup(node, InfixExpression.OPERATOR_PROPERTY));
		}

		if (removeRight) {
			int end;
			if (removeLeft && node.extendedOperands().size() > 0) {
				end= getExtendedOffset((Expression) node.extendedOperands().get(0));
			} else {
				end= getExtendedEnd(right);
			}
			TextEditGroup editGroup= getEditGroup(rightEvent);
			doTextRemoveAndVisit(pos, end - pos, right, editGroup);
			pos= end;
		} else {
			pos= rewriteRequiredNode(node, InfixExpression.RIGHT_OPERAND_PROPERTY);
		}

		RewriteEvent event= getEvent(node, InfixExpression.EXTENDED_OPERANDS_PROPERTY);
		String prefixString= ' ' + operation + ' ';

		if (needsNewOperation) {
			int startPos= pos;
			TextEditGroup editGroup= getEditGroup(node, InfixExpression.OPERATOR_PROPERTY);

			if (event != null && event.getChangeKind() != RewriteEvent.UNCHANGED) {
				RewriteEvent[] extendedOperands= event.getChildren();
				for (int i= 0; i < extendedOperands.length; i++) {
					RewriteEvent curr= extendedOperands[i];
					ASTNode elem= (ASTNode) curr.getOriginalValue();
					if (elem != null) {
						if (curr.getChangeKind() != RewriteEvent.REPLACED) {
							replaceOperation(startPos, operation, editGroup);
						}
						startPos= elem.getStartPosition() + elem.getLength();
					}
				}
			} else {
				List extendedOperands= (List) getOriginalValue(node, InfixExpression.EXTENDED_OPERANDS_PROPERTY);
				for (int i= 0; i < extendedOperands.size(); i++) {
					ASTNode elem= (ASTNode) extendedOperands.get(i);
					replaceOperation(startPos, operation, editGroup);
					startPos= elem.getStartPosition() + elem.getLength();
				}
			}
		}
		rewriteNodeList(node, InfixExpression.EXTENDED_OPERANDS_PROPERTY, pos, prefixString, prefixString);
		return false;
	}

	@Override
	public boolean visit(Initializer node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}
		int pos= rewriteJavadoc(node, Initializer.JAVADOC_PROPERTY);
		if (node.getAST().apiLevel() == JLS2_INTERNAL) {
			rewriteModifiers(node, INTERNAL_INITIALIZER_MODIFIERS_PROPERTY3, pos);
		} else {
			rewriteModifiers2(node, Initializer.MODIFIERS2_PROPERTY, pos);
		}
		rewriteRequiredNode(node, Initializer.BODY_PROPERTY);
		return false;
	}

	@Override
	public boolean visit(InstanceofExpression node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}

		rewriteRequiredNode(node, InstanceofExpression.LEFT_OPERAND_PROPERTY);
		ensureSpaceAfterReplace(node, InstanceofExpression.LEFT_OPERAND_PROPERTY);
		rewriteRequiredNode(node, InstanceofExpression.RIGHT_OPERAND_PROPERTY);
		return false;
	}

	@Override
	public boolean visit(PatternInstanceofExpression node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}

		rewriteRequiredNode(node, PatternInstanceofExpression.LEFT_OPERAND_PROPERTY);
		ensureSpaceAfterReplace(node, PatternInstanceofExpression.LEFT_OPERAND_PROPERTY);
		rewriteRequiredNode(node, PatternInstanceofExpression.RIGHT_OPERAND_PROPERTY);
		return false;
	}

	private void ensureSpaceAfterReplace(ASTNode node, ChildPropertyDescriptor desc) {
		if (getChangeKind(node, desc) == RewriteEvent.REPLACED) {
			int leftOperandEnd= getExtendedEnd((ASTNode) getOriginalValue(node, desc));
			try {
				int offset= getScanner().getNextStartOffset(leftOperandEnd, true); // instanceof

				if (offset == leftOperandEnd) {
					doTextInsert(offset, String.valueOf(' '), getEditGroup(node, desc));
				}
			} catch (CoreException e) {
				handleException(e);
			}
		}
	}

	private void ensureSpaceBeforeReplace(ASTNode node) {
		if (this.beforeRequiredSpaceIndex  == -1) return;

		List events = this.eventStore.getChangedPropertieEvents(node);

		for (Iterator iterator = events.iterator(); iterator.hasNext();) {
			RewriteEvent event = (RewriteEvent) iterator.next();
			if (event.getChangeKind() == RewriteEvent.REPLACED && event.getOriginalValue() instanceof ASTNode) {
				if (this.beforeRequiredSpaceIndex  == getExtendedOffset((ASTNode) event.getOriginalValue())) {
					doTextInsert(this.beforeRequiredSpaceIndex , String.valueOf(' '), getEditGroup(event));
					this.beforeRequiredSpaceIndex  = -1;
					return;
				}
			}
		}

		if (this.beforeRequiredSpaceIndex  < getExtendedOffset(node)) {
			this.beforeRequiredSpaceIndex  = -1;
		}
	}

	@Override
	public boolean visit(IntersectionType node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}
		rewriteNodeList(node, IntersectionType.TYPES_PROPERTY, node.getStartPosition(), Util.EMPTY_STRING, " & "); //$NON-NLS-1$
		return false;
	}

	@Override
	public boolean visit(Javadoc node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}
		int startPos= node.getStartPosition() + 3;
		String separator= getLineDelimiter() + getIndentAtOffset(node.getStartPosition())  + " * "; //$NON-NLS-1$

		rewriteNodeList(node, Javadoc.TAGS_PROPERTY, startPos, separator, separator);
		return false;
	}

	@Override
	public boolean visit(LabeledStatement node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}

		rewriteRequiredNode(node, LabeledStatement.LABEL_PROPERTY);
		rewriteRequiredNode(node, LabeledStatement.BODY_PROPERTY);
		return false;
	}

	@Override
	public boolean visit(LambdaExpression node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}
		Boolean newValue = (Boolean) getNewValue(node, LambdaExpression.PARENTHESES_PROPERTY);
		boolean hasParentheses = newValue.equals(Boolean.TRUE);
		if (!hasParentheses) {// Parentheses can be absent if and only if there is one and only one type elided parameter.
			List parameters = (List) getNewValue(node, LambdaExpression.PARAMETERS_PROPERTY);
			hasParentheses = !(parameters.size() == 1 && parameters.get(0) instanceof VariableDeclarationFragment);
		}

		boolean deleteParentheses = false;
		boolean insertParentheses = false;
		TextEditGroup editGroup = null;

		boolean oldHasParentheses = getOriginalValue(node, LambdaExpression.PARENTHESES_PROPERTY).equals(Boolean.TRUE);
		RewriteEvent event = getEvent(node, LambdaExpression.PARENTHESES_PROPERTY);
		if (event != null) {
			editGroup = getEditGroup(event);
			if (event.getChangeKind() == RewriteEvent.REPLACED) {
				if (newValue != Boolean.FALSE) {
					insertParentheses = true;
				} else {// apply the stricter check for parentheses deletion
					deleteParentheses = !hasParentheses;
				}
			}
		} else if (!oldHasParentheses && hasParentheses) {// parameter property changed to effect parentheses insertion
			if ((event = getEvent(node, LambdaExpression.PARAMETERS_PROPERTY)) != null) {// a null check though event cannot be null here
				editGroup = getEditGroup(event);
				insertParentheses = true;
			}
		}

		int pos = node.getStartPosition();
		if (insertParentheses) {
			doTextInsert(pos, "(", editGroup); //$NON-NLS-1$
		} else if (deleteParentheses) {
			try {
				int lparensEnd = getScanner().getTokenEndOffset(TerminalTokens.TokenNameLPAREN, pos);
				doTextRemove(pos, lparensEnd - pos, editGroup);
				pos = lparensEnd;
			} catch (CoreException e) {
				handleException(e);
			}
		}

		if (isChanged(node, LambdaExpression.PARAMETERS_PROPERTY)) {
			try {
				pos = oldHasParentheses ? getScanner().getTokenEndOffset(TerminalTokens.TokenNameLPAREN, pos) : pos;
				pos = rewriteNodeList(node, LambdaExpression.PARAMETERS_PROPERTY, pos, Util.EMPTY_STRING, ", "); //$NON-NLS-1$
			} catch (CoreException e) {
				handleException(e);
			}
		} else {
			pos = doVisit(node, LambdaExpression.PARAMETERS_PROPERTY, pos);
		}

		if (insertParentheses) {
			doTextInsert(pos, ")", editGroup); //$NON-NLS-1$
		} else if (deleteParentheses) {
			try {
				doTextRemove(pos, getScanner().getTokenEndOffset(TerminalTokens.TokenNameRPAREN, pos) - pos, editGroup);
			} catch (CoreException e) {
				handleException(e);
			}
		}
		rewriteRequiredNode(node, LambdaExpression.BODY_PROPERTY);

		return false;
	}

	@Override
	public boolean visit(MethodInvocation node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}

		int pos= rewriteOptionalQualifier(node, MethodInvocation.EXPRESSION_PROPERTY, node.getStartPosition());
		if (node.getAST().apiLevel() >= JLS3_INTERNAL) {
			pos= rewriteOptionalTypeParameters(node, MethodInvocation.TYPE_ARGUMENTS_PROPERTY, pos, Util.EMPTY_STRING, false, false);
		}

		pos= rewriteRequiredNode(node, MethodInvocation.NAME_PROPERTY);

		if (isChanged(node, MethodInvocation.ARGUMENTS_PROPERTY)) {
			// eval position after opening parent
			try {
				int startOffset= getScanner().getTokenEndOffset(TerminalTokens.TokenNameLPAREN, pos);
				rewriteNodeList(node, MethodInvocation.ARGUMENTS_PROPERTY, startOffset, Util.EMPTY_STRING, ", "); //$NON-NLS-1$
			} catch (CoreException e) {
				handleException(e);
			}
		} else {
			voidVisit(node, MethodInvocation.ARGUMENTS_PROPERTY);
		}
		return false;
	}

	@Override
	public boolean visit(NullLiteral node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}

		changeNotSupported(node); // no modification possible
		return false;
	}

	@Override
	public boolean visit(NullPattern node) {
		if (!DOMASTUtil.isPatternSupported(node.getAST())) {
			return false;
		}
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}

		changeNotSupported(node); // no modification possible
		return false;
	}

	@Override
	public boolean visit(NumberLiteral node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}
		String newLiteral= (String) getNewValue(node, NumberLiteral.TOKEN_PROPERTY);
		TextEditGroup group = getEditGroup(node, NumberLiteral.TOKEN_PROPERTY);
		doTextReplace(node.getStartPosition(), node.getLength(), newLiteral, group);
		return false;
	}

	@Override
	public boolean visit(PackageDeclaration node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}
		if (node.getAST().apiLevel() >= JLS3_INTERNAL) {
			int pos= rewriteJavadoc(node, PackageDeclaration.JAVADOC_PROPERTY);
			rewriteModifiers2(node, PackageDeclaration.ANNOTATIONS_PROPERTY, pos);
		}

		rewriteRequiredNode(node, PackageDeclaration.NAME_PROPERTY);
		return false;
	}

	@Override
	public boolean visit(ParenthesizedExpression node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}

		rewriteRequiredNode(node, ParenthesizedExpression.EXPRESSION_PROPERTY);
		return false;
	}

	@Override
	public boolean visit(PostfixExpression node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}

		int pos= rewriteRequiredNode(node, PostfixExpression.OPERAND_PROPERTY);
		rewriteOperation(node, PostfixExpression.OPERATOR_PROPERTY, pos);
		return false;
	}

	@Override
	public boolean visit(PrefixExpression node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}

		rewriteOperation(node, PrefixExpression.OPERATOR_PROPERTY, node.getStartPosition());
		rewriteRequiredNode(node, PrefixExpression.OPERAND_PROPERTY);
		return false;
	}

	@Override
	public boolean visit(PrimitiveType node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}
		if (node.getAST().apiLevel() >= JLS8_INTERNAL) {
			rewriteTypeAnnotations(node, PrimitiveType.ANNOTATIONS_PROPERTY, node.getStartPosition());
		}
		PrimitiveType.Code newCode= (PrimitiveType.Code) getNewValue(node, PrimitiveType.PRIMITIVE_TYPE_CODE_PROPERTY);
		TextEditGroup group = getEditGroup(node, PrimitiveType.PRIMITIVE_TYPE_CODE_PROPERTY);
		doTextReplace(node.getStartPosition(), node.getLength(), newCode.toString(), group);
		return false;
	}

	@Override
	public boolean visit(ProvidesDirective node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}
		int pos = rewriteRequiredNode(node, ProvidesDirective.NAME_PROPERTY);
		pos= rewriteNodeList(node, ProvidesDirective.IMPLEMENTATIONS_PROPERTY, pos, " with ", ", "); //$NON-NLS-1$ //$NON-NLS-2$
		return false;
	}

	@Override
	public boolean visit(QualifiedName node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}

		rewriteRequiredNode(node, QualifiedName.QUALIFIER_PROPERTY);
		rewriteRequiredNode(node, QualifiedName.NAME_PROPERTY);
		return false;
	}

	@Override
	public boolean visit(SimpleName node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}
		String newString= (String) getNewValue(node, SimpleName.IDENTIFIER_PROPERTY);
		TextEditGroup group = getEditGroup(node, SimpleName.IDENTIFIER_PROPERTY);
		doTextReplace(node.getStartPosition(), node.getLength(), newString, group);
		return false;
	}

	@Override
	public boolean visit(SimpleType node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}
		if (node.getAST().apiLevel() >= JLS8_INTERNAL) {
			rewriteTypeAnnotations(node, SimpleType.ANNOTATIONS_PROPERTY, node.getStartPosition());
		}
		rewriteRequiredNode(node, SimpleType.NAME_PROPERTY);
		return false;
	}

	@Override
	public boolean visit(SingleVariableDeclaration node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}
		int pos= node.getStartPosition();
		int apiLevel= node.getAST().apiLevel();
		if (apiLevel == JLS2_INTERNAL) {
			rewriteModifiers(node, INTERNAL_VARIABLE_MODIFIERS_PROPERTY, pos);
		} else {
			rewriteModifiers2(node, SingleVariableDeclaration.MODIFIERS2_PROPERTY, pos);
		}
		pos= rewriteRequiredNode(node, SingleVariableDeclaration.TYPE_PROPERTY);
		if (apiLevel >= JLS3_INTERNAL) {
			if (isChanged(node, SingleVariableDeclaration.VARARGS_PROPERTY)) {
				TextEditGroup editGroup = getEditGroup(node, SingleVariableDeclaration.VARARGS_PROPERTY);
				if (getNewValue(node, SingleVariableDeclaration.VARARGS_PROPERTY).equals(Boolean.TRUE)) {
					if (apiLevel >= JLS8_INTERNAL) {
						pos= rewriteVarargsAnnotations(node, SingleVariableDeclaration.VARARGS_ANNOTATIONS_PROPERTY, pos);
					}
					int indent= getIndent(node.getStartPosition());
					String prefix= this.formatter.VARARGS.getPrefix(indent);
					doTextInsert(pos, prefix, editGroup);
					doTextInsert(pos, "...", editGroup); //$NON-NLS-1$
				} else {
					try {
						int ellipsisEnd;
						int noOfAnnotations = apiLevel >= JLS8_INTERNAL ? node.varargsAnnotations().size() : 0;
						if (noOfAnnotations > 0) {
							Annotation annotation= (Annotation) node.varargsAnnotations().get(noOfAnnotations - 1);
							int annotationEndPosition= annotation.getStartPosition() + annotation.getLength();
							ellipsisEnd= getScanner().getNextEndOffset(annotationEndPosition, true);
						} else {
							ellipsisEnd= getScanner().getNextEndOffset(pos, true);
						}
						doTextRemove(pos, ellipsisEnd - pos, editGroup);
					} catch (CoreException e) {
						handleException(e);
					}
				}
			} else {
				if (apiLevel >= JLS8_INTERNAL && node.isVarargs()) {
					pos = rewriteVarargsAnnotations(node, SingleVariableDeclaration.VARARGS_ANNOTATIONS_PROPERTY, pos);
				}
			}
			if (!node.isVarargs()) {
				ensureSpaceAfterReplace(node, SingleVariableDeclaration.TYPE_PROPERTY);
			}
		} else {
			ensureSpaceAfterReplace(node, SingleVariableDeclaration.TYPE_PROPERTY);
		}

		pos= rewriteRequiredNode(node, SingleVariableDeclaration.NAME_PROPERTY);
		if (apiLevel < JLS8_INTERNAL) {
			int extraDims= rewriteExtraDimensions(node, INTERNAL_VARIABLE_EXTRA_DIMENSIONS_PROPERTY, pos);

			if (extraDims > 0) {
				int kind= getChangeKind(node, SingleVariableDeclaration.INITIALIZER_PROPERTY);
				if (kind == RewriteEvent.REMOVED) {
					try {
						pos= getScanner().getPreviousTokenEndOffset(TerminalTokens.TokenNameEQUAL, pos);
					} catch (CoreException e) {
						handleException(e);
					}
				} else {
					pos= node.getStartPosition() + node.getLength(); // insert pos
				}
			}
		} else {
			pos = rewriteExtraDimensionsInfo(node, pos, SingleVariableDeclaration.EXTRA_DIMENSIONS2_PROPERTY);
		}

		rewriteNode(node, SingleVariableDeclaration.INITIALIZER_PROPERTY, pos, this.formatter.VAR_INITIALIZER);
		return false;
	}

	@Override
	public boolean visit(StringLiteral node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}
		String escapedSeq= (String) getNewValue(node, StringLiteral.ESCAPED_VALUE_PROPERTY);
		TextEditGroup group = getEditGroup(node, StringLiteral.ESCAPED_VALUE_PROPERTY);
		doTextReplace(node.getStartPosition(), node.getLength(), escapedSeq, group);

		return false;
	}

	@Override
	public boolean visit(SuperConstructorInvocation node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}

		int pos= rewriteOptionalQualifier(node, SuperConstructorInvocation.EXPRESSION_PROPERTY, node.getStartPosition());

		if (node.getAST().apiLevel() >= JLS3_INTERNAL) {
			pos= rewriteOptionalTypeParameters(node, SuperConstructorInvocation.TYPE_ARGUMENTS_PROPERTY, pos, Util.EMPTY_STRING, false, false);
		}

		if (isChanged(node, SuperConstructorInvocation.ARGUMENTS_PROPERTY)) {
			// eval position after opening parent
			try {
				pos= getScanner().getTokenEndOffset(TerminalTokens.TokenNameLPAREN, pos);
				rewriteNodeList(node, SuperConstructorInvocation.ARGUMENTS_PROPERTY, pos, Util.EMPTY_STRING, ", "); //$NON-NLS-1$
			} catch (CoreException e) {
				handleException(e);
			}
		} else {
			voidVisit(node, SuperConstructorInvocation.ARGUMENTS_PROPERTY);
		}
		return false;
	}

	@Override
	public boolean visit(SuperFieldAccess node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}

		rewriteOptionalQualifier(node, SuperFieldAccess.QUALIFIER_PROPERTY, node.getStartPosition());
		rewriteRequiredNode(node, SuperFieldAccess.NAME_PROPERTY);
		return false;
	}

	@Override
	public boolean visit(SuperMethodInvocation node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}

		int pos= rewriteOptionalQualifier(node, SuperMethodInvocation.QUALIFIER_PROPERTY, node.getStartPosition());

		if (node.getAST().apiLevel() >= JLS3_INTERNAL) {
			if (isChanged(node, SuperMethodInvocation.TYPE_ARGUMENTS_PROPERTY)) {
				try {
					pos= getScanner().getTokenEndOffset(TerminalTokens.TokenNameDOT, pos);
					rewriteOptionalTypeParameters(node, SuperMethodInvocation.TYPE_ARGUMENTS_PROPERTY, pos, Util.EMPTY_STRING, false, false);
				} catch (CoreException e) {
					handleException(e);
				}
			}
		}

		pos= rewriteRequiredNode(node, SuperMethodInvocation.NAME_PROPERTY);

		if (isChanged(node, SuperMethodInvocation.ARGUMENTS_PROPERTY)) {
			// eval position after opening parent
			try {
				pos= getScanner().getTokenEndOffset(TerminalTokens.TokenNameLPAREN, pos);
				rewriteNodeList(node, SuperMethodInvocation.ARGUMENTS_PROPERTY, pos, Util.EMPTY_STRING, ", "); //$NON-NLS-1$
			} catch (CoreException e) {
				handleException(e);
			}
		} else {
			voidVisit(node, SuperMethodInvocation.ARGUMENTS_PROPERTY);
		}
		return false;
	}

	@Override
	public boolean visit(SwitchCase node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}

		// dont allow switching from case to default or back. New statements should be created.
		if (node.getAST().apiLevel() >= JLS14_INTERNAL) {
			int pos = node.expressions().size() == 0 ? node.getStartPosition() :
					rewriteNodeList(node, SwitchCase.EXPRESSIONS2_PROPERTY, node.getStartPosition(), Util.EMPTY_STRING, ", "); //$NON-NLS-1$
			if (isChanged(node, SwitchCase.SWITCH_LABELED_RULE_PROPERTY)) {
				TextEditGroup editGroup = getEditGroup(node, SwitchCase.SWITCH_LABELED_RULE_PROPERTY);
				try {
					int tokenEnd, oldToken;
					String newVal;
					if (getNewValue(node, SwitchCase.SWITCH_LABELED_RULE_PROPERTY).equals(Boolean.TRUE)) {
						oldToken = TerminalTokens.TokenNameCOLON;
						newVal = "->"; //$NON-NLS-1$
					} else {
						oldToken = TerminalTokens.TokenNameARROW;
						newVal = ":"; //$NON-NLS-1$
					}
					pos = getScanner().getTokenStartOffset(oldToken, pos);
					tokenEnd = getScanner().getTokenEndOffset(oldToken, pos);
					doTextRemove(pos, tokenEnd - pos, editGroup);
					doTextInsert(pos, newVal, editGroup);
					pos = tokenEnd;
				} catch (CoreException e) {
					handleException(e);
				}
			}
		} else {
			rewriteExpressionOptionalQualifier(node, INTERNAL_SWITCH_EXPRESSION_PROPERTY, node.getStartPosition());
		}
		return false;
	}

	class SwitchListLabeledRuleRewriter extends SwitchListRewriter {

		public SwitchListLabeledRuleRewriter(int initialIndent) {
			super(initialIndent);
		}

		@Override
		protected String getSeparatorString(int nodeIndex, int nextNodeIndex) {
		    boolean isSwitchLabelRule = isSwitchLabeledRule(nodeIndex, nextNodeIndex);
		    String spaceDelim = JavaCore.INSERT.equals(ASTRewriteAnalyzer.this.options.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_ARROW_IN_SWITCH_CASE))? " ":""; //$NON-NLS-1$ //$NON-NLS-2$
		    String lineDelim = isSwitchLabelRule ? spaceDelim : getLineDelimiter();

			StringBuilder buf= new StringBuilder(lineDelim);
			buf.append(createIndentString(getNodeIndent(nextNodeIndex)));
			return buf.toString();
		}

		@Override
		protected int getNodeIndent(int nodeIndex) {
			int indent= getInitialIndent();
			if (this.indentSwitchStatementsCompareToCases) {
				RewriteEvent event = this.list[nodeIndex];
				int changeKind = event.getChangeKind();
				ASTNode node;
				if (changeKind == RewriteEvent.INSERTED || changeKind == RewriteEvent.REPLACED) {
					node= (ASTNode)event.getNewValue();
				} else {
					node= (ASTNode)event.getOriginalValue();
				}
				if (node.getNodeType() != ASTNode.SWITCH_CASE) {
					ASTNode prevNode = getNode(nodeIndex -1);
					if (prevNode.getNodeType() == ASTNode.SWITCH_CASE && ((SwitchCase)prevNode).isSwitchLabeledRule()) {
						return 0;
					} else {
						indent++;
					}
				}
			}
			return indent;
		}

		private boolean isSwitchLabeledRule(int nodeIndex, int nextNodeIndex) {
			ASTNode curr= getNode(nodeIndex);
			ASTNode next= getNode(nodeIndex +1);
			int currKind= curr.getNodeType();
		    if (currKind == ASTNode.SWITCH_CASE && next instanceof Statement &&   ((SwitchCase)curr).isSwitchLabeledRule()) {
					return true;
			}
		    return false;
		}
	}

	class SwitchListRewriter extends ParagraphListRewriter {

		protected boolean indentSwitchStatementsCompareToCases;

		public SwitchListRewriter(int initialIndent) {
			super(initialIndent, 0);
			this.indentSwitchStatementsCompareToCases =
				DefaultCodeFormatterConstants.TRUE.equals(ASTRewriteAnalyzer.this.options.get(DefaultCodeFormatterConstants.FORMATTER_INDENT_SWITCHSTATEMENTS_COMPARE_TO_CASES));
		}

		@Override
		protected int getNodeIndent(int nodeIndex) {
			int indent= getInitialIndent();

			if (this.indentSwitchStatementsCompareToCases) {
				RewriteEvent event = this.list[nodeIndex];
				int changeKind = event.getChangeKind();

				ASTNode node;
				if (changeKind == RewriteEvent.INSERTED || changeKind == RewriteEvent.REPLACED) {
					node= (ASTNode)event.getNewValue();
				} else {
					node= (ASTNode)event.getOriginalValue();
				}

				if (node.getNodeType() != ASTNode.SWITCH_CASE) {
					indent++;
				}
			}
			return indent;
		}

		@Override
		protected String getSeparatorString(int nodeIndex) {
			int total = this.list.length;

			int nextNodeIndex = nodeIndex + 1;
			while (nextNodeIndex < total && this.list[nextNodeIndex].getChangeKind() == RewriteEvent.REMOVED) {
				nextNodeIndex++;
			}
			if (nextNodeIndex == total) {
				return super.getSeparatorString(nodeIndex);
			}
			return getSeparatorString(nodeIndex, nextNodeIndex);
		}

		@Override
		protected void updateIndent(int prevMark, int originalOffset, int nodeIndex, TextEditGroup editGroup) {
			if (prevMark != RewriteEvent.UNCHANGED && prevMark != RewriteEvent.REPLACED) return;

			// Do not change indent if the previous non removed node is on the same line
			int previousNonRemovedNodeIndex = nodeIndex - 1;
			while (previousNonRemovedNodeIndex >= 0 && this.list[previousNonRemovedNodeIndex].getChangeKind() == RewriteEvent.REMOVED) {
				previousNonRemovedNodeIndex--;
			}

			if (previousNonRemovedNodeIndex > -1) {
				LineInformation lineInformation = getLineInformation();

				RewriteEvent prevEvent = this.list[previousNonRemovedNodeIndex];
				int prevKind = prevEvent.getChangeKind();
				if (prevKind == RewriteEvent.UNCHANGED || prevKind == RewriteEvent.REPLACED) {
					ASTNode prevNode = (ASTNode) this.list[previousNonRemovedNodeIndex].getOriginalValue();
					int prevEndPosition = prevNode.getStartPosition() + prevNode.getLength();
					int prevLine = lineInformation.getLineOfOffset(prevEndPosition);
					int line = lineInformation.getLineOfOffset(originalOffset);

					if (prevLine == line) {
						return;
					}
				}
			}

			int total = this.list.length;
			while (nodeIndex < total && this.list[nodeIndex].getChangeKind() == RewriteEvent.REMOVED) {
				nodeIndex++;
			}

			int originalIndent = getIndent(originalOffset);
			int newIndent = getNodeIndent(nodeIndex);

			if (originalIndent != newIndent) {

				int line= getLineInformation().getLineOfOffset(originalOffset);
				if (line >= 0) {
					int lineStart= getLineInformation().getLineOffset(line);

					doTextRemove(lineStart, originalOffset - lineStart, editGroup); // remove previous indentation
					doTextInsert(lineStart, createIndentString(newIndent), editGroup); // add new indentation
				}
			}
		}
	}

	@Override
	public boolean visit(SwitchExpression node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}

		int pos= rewriteRequiredNode(node, SwitchExpression.EXPRESSION_PROPERTY);

		ChildListPropertyDescriptor property= SwitchExpression.STATEMENTS_PROPERTY;
		if (getChangeKind(node, property) != RewriteEvent.UNCHANGED) {
			try {
				pos= getScanner().getTokenEndOffset(TerminalTokens.TokenNameLBRACE, pos);
				int insertIndent= getIndent(node.getStartPosition());
				if (DefaultCodeFormatterConstants.TRUE.equals(this.options.get(DefaultCodeFormatterConstants.FORMATTER_INDENT_SWITCHSTATEMENTS_COMPARE_TO_SWITCH))) {
					insertIndent++;
				}

				ParagraphListRewriter listRewriter= new SwitchListLabeledRuleRewriter(insertIndent);
				StringBuilder leadString= new StringBuilder();
				leadString.append(getLineDelimiter());
				leadString.append(createIndentString(insertIndent));
				listRewriter.rewriteList(node, property, pos, leadString.toString());
			} catch (CoreException e) {
				handleException(e);
			}
		} else {
			voidVisit(node, SwitchExpression.STATEMENTS_PROPERTY);
		}
		return false;
	}

	@Override
	public boolean visit(SwitchStatement node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}

		int pos= rewriteRequiredNode(node, SwitchStatement.EXPRESSION_PROPERTY);

		ChildListPropertyDescriptor property= SwitchStatement.STATEMENTS_PROPERTY;
		if (getChangeKind(node, property) != RewriteEvent.UNCHANGED) {
			try {
				pos= getScanner().getTokenEndOffset(TerminalTokens.TokenNameLBRACE, pos);
				int insertIndent= getIndent(node.getStartPosition());
				if (DefaultCodeFormatterConstants.TRUE.equals(this.options.get(DefaultCodeFormatterConstants.FORMATTER_INDENT_SWITCHSTATEMENTS_COMPARE_TO_SWITCH))) {
					insertIndent++;
				}
				ParagraphListRewriter listRewriter;
				if ((node.getAST().apiLevel() >= JLS14_INTERNAL)) {
					listRewriter= new SwitchListLabeledRuleRewriter(insertIndent);
				} else {
					listRewriter= new SwitchListRewriter(insertIndent);
				}
				StringBuilder leadString= new StringBuilder();
				leadString.append(getLineDelimiter());
				leadString.append(createIndentString(insertIndent));
				listRewriter.rewriteList(node, property, pos, leadString.toString());
			} catch (CoreException e) {
				handleException(e);
			}
		} else {
			voidVisit(node, SwitchStatement.STATEMENTS_PROPERTY);
		}
		return false;
	}

	@Override
	public boolean visit(SynchronizedStatement node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}

		rewriteRequiredNode(node, SynchronizedStatement.EXPRESSION_PROPERTY);
		rewriteRequiredNode(node, SynchronizedStatement.BODY_PROPERTY);
		return false;
	}

	@Override
	public boolean visit(ThisExpression node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}

		rewriteOptionalQualifier(node, ThisExpression.QUALIFIER_PROPERTY, node.getStartPosition());
		return false;
	}

	@Override
	public boolean visit(ThrowStatement node) {
		try {
			this.beforeRequiredSpaceIndex = getScanner().getTokenEndOffset(TerminalTokens.TokenNamethrow, node.getStartPosition());

			if (!hasChildrenChanges(node)) {
				return doVisitUnchangedChildren(node);
			}

			ensureSpaceBeforeReplace(node);

			rewriteRequiredNode(node, ThrowStatement.EXPRESSION_PROPERTY);
		} catch (CoreException e) {
			handleException(e);
		}
		return false;
	}

	@Override
	public boolean visit(TryStatement node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}
		int pos= node.getStartPosition();
		int level = node.getAST().apiLevel();
		if (level >= JLS4_INTERNAL) {
			StructuralPropertyDescriptor desc = level < JLS9_INTERNAL ? INTERNAL_TRY_STATEMENT_RESOURCES_PROPERTY : TryStatement.RESOURCES2_PROPERTY;
			if (isChanged(node, desc)) {
				int indent= getIndent(node.getStartPosition());
				String prefix= this.formatter.TRY_RESOURCES.getPrefix(indent);
				String newParen = this.formatter.TRY_RESOURCES_PAREN.getPrefix(indent) + "("; //$NON-NLS-1$
				pos= rewriteNodeList(node, desc, getPosAfterTry(pos), newParen, ")", ";" + prefix); //$NON-NLS-1$ //$NON-NLS-2$

			} else {
				pos= doVisit(node, desc, pos);
			}
		}

		pos= rewriteRequiredNode(node, TryStatement.BODY_PROPERTY);

		if (isChanged(node, TryStatement.CATCH_CLAUSES_PROPERTY)) {
			int indent= getIndent(node.getStartPosition());
			String prefix= this.formatter.CATCH_BLOCK.getPrefix(indent);
			pos= rewriteNodeList(node, TryStatement.CATCH_CLAUSES_PROPERTY, pos, prefix, prefix);
		} else {
			pos= doVisit(node, TryStatement.CATCH_CLAUSES_PROPERTY, pos);
		}
		rewriteNode(node, TryStatement.FINALLY_PROPERTY, pos, this.formatter.FINALLY_BLOCK);
		return false;
	}

	@Override
	public boolean visit(TypeDeclarationStatement node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}
		if (node.getAST().apiLevel() == JLS2_INTERNAL) {
			rewriteRequiredNode(node, INTERNAL_TDS_TYPE_DECLARATION_PROPERTY);
		} else {
			rewriteRequiredNode(node, TypeDeclarationStatement.DECLARATION_PROPERTY);
		}
		return false;
	}

	@Override
	public boolean visit(TypeLiteral node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}

		rewriteRequiredNode(node, TypeLiteral.TYPE_PROPERTY);
		return false;
	}

	@Override
	public boolean visit(UnionType node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}
		rewriteNodeList(node, UnionType.TYPES_PROPERTY, node.getStartPosition(), Util.EMPTY_STRING, " | "); //$NON-NLS-1$
		return false;
	}

	@Override
	public boolean visit(UsesDirective node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}
		rewriteRequiredNode(node,UsesDirective.NAME_PROPERTY);
		return false;
	}

	@Override
	public boolean visit(VariableDeclarationExpression node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}

		// same code as FieldDeclaration
		int pos= node.getStartPosition();
		if (node.getAST().apiLevel() == JLS2_INTERNAL) {
			rewriteModifiers(node, INTERNAL_VDE_MODIFIERS_PROPERTY, pos);
		} else {
			rewriteModifiers2(node, VariableDeclarationExpression.MODIFIERS2_PROPERTY, pos);
		}
		pos= rewriteRequiredNode(node, VariableDeclarationExpression.TYPE_PROPERTY);
		rewriteNodeList(node, VariableDeclarationExpression.FRAGMENTS_PROPERTY, pos, Util.EMPTY_STRING, ", "); //$NON-NLS-1$
		return false;
	}

	@Override
	public boolean visit(VariableDeclarationFragment node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}

		int pos= rewriteRequiredNode(node, VariableDeclarationFragment.NAME_PROPERTY);

		if (node.getAST().apiLevel() < JLS8_INTERNAL) {
			int extraDims= rewriteExtraDimensions(node, INTERNAL_FRAGMENT_EXTRA_DIMENSIONS_PROPERTY, pos);
			if (extraDims > 0) {
				int kind= getChangeKind(node, VariableDeclarationFragment.INITIALIZER_PROPERTY);
				if (kind == RewriteEvent.REMOVED) {
					try {
						pos= getScanner().getPreviousTokenEndOffset(TerminalTokens.TokenNameEQUAL, pos);
					} catch (CoreException e) {
						handleException(e);
					}
				} else {
					pos= node.getStartPosition() + node.getLength(); // insert pos
				}
			}
		} else {
			pos = rewriteExtraDimensionsInfo(node, pos, VariableDeclarationFragment.EXTRA_DIMENSIONS2_PROPERTY);
		}
		rewriteNode(node, VariableDeclarationFragment.INITIALIZER_PROPERTY, pos, this.formatter.VAR_INITIALIZER);
		return false;
	}

	@Override
	public boolean visit(VariableDeclarationStatement node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}

		// same code as FieldDeclaration
		int pos= node.getStartPosition();
		if (node.getAST().apiLevel() == JLS2_INTERNAL) {
			rewriteModifiers(node, INTERNAL_VDS_MODIFIERS_PROPERTY, pos);
		} else {
			rewriteModifiers2(node, VariableDeclarationStatement.MODIFIERS2_PROPERTY, pos);
		}
		pos= rewriteRequiredNode(node, VariableDeclarationStatement.TYPE_PROPERTY);

		rewriteNodeList(node, VariableDeclarationStatement.FRAGMENTS_PROPERTY, pos, Util.EMPTY_STRING, ", "); //$NON-NLS-1$
		return false;
	}

	@Override
	public boolean visit(WhileStatement node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}

		int pos= rewriteRequiredNode(node, WhileStatement.EXPRESSION_PROPERTY);

		try {
			if (isChanged(node, WhileStatement.BODY_PROPERTY)) {
				int startOffset= getScanner().getTokenEndOffset(TerminalTokens.TokenNameRPAREN, pos);
				rewriteBodyNode(node, WhileStatement.BODY_PROPERTY, startOffset, -1, getIndent(node.getStartPosition()), this.formatter.WHILE_BLOCK); // body
			} else {
				voidVisit(node, WhileStatement.BODY_PROPERTY);
			}
		} catch (CoreException e) {
			handleException(e);
		}
		return false;
	}

	@Override
	public boolean visit(MemberRef node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}
		rewriteNode(node, MemberRef.QUALIFIER_PROPERTY, node.getStartPosition(), ASTRewriteFormatter.NONE);

		rewriteRequiredNode(node, MemberRef.NAME_PROPERTY);
		return false;
	}

	@Override
	public boolean visit(MethodRef node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}
		rewriteNode(node, MethodRef.QUALIFIER_PROPERTY, node.getStartPosition(), ASTRewriteFormatter.NONE);

		int pos= rewriteRequiredNode(node, MethodRef.NAME_PROPERTY);

		if (isChanged(node, MethodRef.PARAMETERS_PROPERTY)) {
			// eval position after opening parent
			try {
				int startOffset= getScanner().getTokenEndOffset(TerminalTokens.TokenNameLPAREN, pos);
				rewriteNodeList(node, MethodRef.PARAMETERS_PROPERTY, startOffset, Util.EMPTY_STRING, ", "); //$NON-NLS-1$
			} catch (CoreException e) {
				handleException(e);
			}
		} else {
			voidVisit(node, MethodRef.PARAMETERS_PROPERTY);
		}
		return false;
	}

	@Override
	public boolean visit(MethodRefParameter node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}
		int pos= rewriteRequiredNode(node, MethodRefParameter.TYPE_PROPERTY);
		if (node.getAST().apiLevel() >= JLS3_INTERNAL) {
			if (isChanged(node, MethodRefParameter.VARARGS_PROPERTY)) {
				if (getNewValue(node, MethodRefParameter.VARARGS_PROPERTY).equals(Boolean.TRUE)) {
					doTextInsert(pos, "...", getEditGroup(node, MethodRefParameter.VARARGS_PROPERTY)); //$NON-NLS-1$
				} else {
					try {
						int ellipsisEnd= getScanner().getNextEndOffset(pos, true);
						doTextRemove(pos, ellipsisEnd - pos, getEditGroup(node, MethodRefParameter.VARARGS_PROPERTY));
					} catch (CoreException e) {
						handleException(e);
					}
				}
			}
		}
		rewriteNode(node, MethodRefParameter.NAME_PROPERTY, pos, ASTRewriteFormatter.SPACE);
		return false;
	}

	@Override
	public boolean visit(TagElement node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}

		int changeKind= getChangeKind(node, TagElement.TAG_NAME_PROPERTY);
		switch (changeKind) {
			case RewriteEvent.INSERTED: {
			    String newTagName= (String) getNewValue(node, TagElement.TAG_NAME_PROPERTY);
				doTextInsert(node.getStartPosition(), newTagName, getEditGroup(node, TagElement.TAG_NAME_PROPERTY));
				break;
			}
			case RewriteEvent.REMOVED: {
			    doTextRemove(node.getStartPosition(), findTagNameEnd(node) - node.getStartPosition(), getEditGroup(node, TagElement.TAG_NAME_PROPERTY));
			    break;
			}
			case RewriteEvent.REPLACED: {
			    String newTagName= (String) getNewValue(node, TagElement.TAG_NAME_PROPERTY);
		    	doTextReplace(node.getStartPosition(), findTagNameEnd(node) - node.getStartPosition(), newTagName, getEditGroup(node, TagElement.TAG_NAME_PROPERTY));
			    break;
			}
		}

		if (isChanged(node, TagElement.FRAGMENTS_PROPERTY)) {
			// eval position after name
			int endOffset= findTagNameEnd(node);
            rewriteNodeList(node, TagElement.FRAGMENTS_PROPERTY, endOffset, " ", " ");  //$NON-NLS-1$//$NON-NLS-2$
		} else {
			voidVisit(node, TagElement.FRAGMENTS_PROPERTY);
		}
		return false;
	}

	private int findTagNameEnd(TagElement tagNode) {
		if (tagNode.getTagName() != null) {
			char[] cont= getContent();
		    int len= cont.length;
			int i= tagNode.getStartPosition();
			while (i < len && !IndentManipulation.isIndentChar(cont[i])) {
			    i++;
			}
			return i;
		}
	    return tagNode.getStartPosition();
	}

	@Override
	public boolean visit(TextBlock node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}
		String escapedSeq= (String) getNewValue(node, TextBlock.ESCAPED_VALUE_PROPERTY);
		TextEditGroup group = getEditGroup(node, TextBlock.ESCAPED_VALUE_PROPERTY);
		doTextReplace(node.getStartPosition(), node.getLength(), escapedSeq, group);

		return false;
	}

	@Override
	public boolean visit(TextElement node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}
		String newText= (String) getNewValue(node, TextElement.TEXT_PROPERTY);
		TextEditGroup group = getEditGroup(node, TextElement.TEXT_PROPERTY);
		doTextReplace(node.getStartPosition(), node.getLength(), newText, group);
		return false;
	}

	@Override
	public boolean visit(AnnotationTypeDeclaration node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}
		int pos= rewriteJavadoc(node, AnnotationTypeDeclaration.JAVADOC_PROPERTY);
		rewriteModifiers2(node, AnnotationTypeDeclaration.MODIFIERS2_PROPERTY, pos);
		pos= rewriteRequiredNode(node, AnnotationTypeDeclaration.NAME_PROPERTY);

		int startIndent= getIndent(node.getStartPosition()) + 1;
		int startPos= getPosAfterLeftBrace(pos);
		rewriteParagraphList(node, AnnotationTypeDeclaration.BODY_DECLARATIONS_PROPERTY, startPos, startIndent, -1, 2);
		return false;
	}

	@Override
	public boolean visit(AnnotationTypeMemberDeclaration node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}
		int pos= rewriteJavadoc(node, AnnotationTypeMemberDeclaration.JAVADOC_PROPERTY);
		rewriteModifiers2(node, AnnotationTypeMemberDeclaration.MODIFIERS2_PROPERTY, pos);
		rewriteRequiredNode(node, AnnotationTypeMemberDeclaration.TYPE_PROPERTY);
		pos= rewriteRequiredNode(node, AnnotationTypeMemberDeclaration.NAME_PROPERTY);

		try {
			int changeKind= getChangeKind(node, AnnotationTypeMemberDeclaration.DEFAULT_PROPERTY);
			if (changeKind == RewriteEvent.INSERTED || changeKind == RewriteEvent.REMOVED) {
				pos= getScanner().getTokenEndOffset(TerminalTokens.TokenNameRPAREN, pos);
			}
			rewriteNode(node, AnnotationTypeMemberDeclaration.DEFAULT_PROPERTY, pos, this.formatter.ANNOT_MEMBER_DEFAULT);
		} catch (CoreException e) {
			handleException(e);
		}
		return false;
	}

	@Override
	public boolean visit(EnhancedForStatement node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}
		rewriteRequiredNode(node, EnhancedForStatement.PARAMETER_PROPERTY);
		int pos= rewriteRequiredNode(node, EnhancedForStatement.EXPRESSION_PROPERTY);

		RewriteEvent bodyEvent= getEvent(node, EnhancedForStatement.BODY_PROPERTY);
		if (bodyEvent != null && bodyEvent.getChangeKind() == RewriteEvent.REPLACED) {
			int startOffset;
			try {
				startOffset= getScanner().getTokenEndOffset(TerminalTokens.TokenNameRPAREN, pos);
				rewriteBodyNode(node, EnhancedForStatement.BODY_PROPERTY, startOffset, -1, getIndent(node.getStartPosition()), this.formatter.FOR_BLOCK); // body
			} catch (CoreException e) {
				handleException(e);
			}
		} else {
			voidVisit(node, EnhancedForStatement.BODY_PROPERTY);
		}
		return false;
	}

	@Override
	public boolean visit(EnumConstantDeclaration node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}
		int pos= rewriteJavadoc(node, EnumConstantDeclaration.JAVADOC_PROPERTY);
		rewriteModifiers2(node, EnumConstantDeclaration.MODIFIERS2_PROPERTY, pos);
		pos= rewriteRequiredNode(node, EnumConstantDeclaration.NAME_PROPERTY);
		RewriteEvent argsEvent= getEvent(node, EnumConstantDeclaration.ARGUMENTS_PROPERTY);
		if (argsEvent != null && argsEvent.getChangeKind() != RewriteEvent.UNCHANGED) {
			RewriteEvent[] children= argsEvent.getChildren();
			try {
				int nextTok= getScanner().readNext(pos, true);
				boolean hasParents= (nextTok == TerminalTokens.TokenNameLPAREN);
				boolean isAllRemoved= hasParents && isAllOfKind(children, RewriteEvent.REMOVED);
				String prefix= Util.EMPTY_STRING;
				if (!hasParents) {
					prefix= "("; //$NON-NLS-1$
				} else if (!isAllRemoved) {
					pos= getScanner().getCurrentEndOffset();
				}
				pos= rewriteNodeList(node, EnumConstantDeclaration.ARGUMENTS_PROPERTY, pos, prefix, ", "); //$NON-NLS-1$

				if (!hasParents) {
					doTextInsert(pos, ")", getEditGroup(children[children.length - 1])); //$NON-NLS-1$
				} else if (isAllRemoved) {
					int afterClosing= getScanner().getNextEndOffset(pos, true);
					doTextRemove(pos, afterClosing - pos, getEditGroup(children[children.length - 1]));
					pos= afterClosing;
				}
			} catch (CoreException e) {
				handleException(e);
			}
		} else {
			pos= doVisit(node, EnumConstantDeclaration.ARGUMENTS_PROPERTY, pos);
		}

		if (isChanged(node, EnumConstantDeclaration.ANONYMOUS_CLASS_DECLARATION_PROPERTY)) {
			int kind= getChangeKind(node, EnumConstantDeclaration.ANONYMOUS_CLASS_DECLARATION_PROPERTY);
			if (kind == RewriteEvent.REMOVED) {
				try {
					// 'pos' can be before brace
					pos= getScanner().getPreviousTokenEndOffset(TerminalTokens.TokenNameLBRACE, pos);
				} catch (CoreException e) {
					handleException(e);
				}
			} else {
				pos= node.getStartPosition() + node.getLength(); // insert pos
			}
			rewriteNode(node, EnumConstantDeclaration.ANONYMOUS_CLASS_DECLARATION_PROPERTY, pos, ASTRewriteFormatter.SPACE);
		}
		return false;
	}

	@Override
	public boolean visit(EnumDeclaration node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}
		int pos= rewriteJavadoc(node, EnumDeclaration.JAVADOC_PROPERTY);
		rewriteModifiers2(node, EnumDeclaration.MODIFIERS2_PROPERTY, pos);
		pos= rewriteRequiredNode(node, EnumDeclaration.NAME_PROPERTY);
		pos= rewriteNodeList(node, EnumDeclaration.SUPER_INTERFACE_TYPES_PROPERTY, pos, " implements ", ", "); //$NON-NLS-1$ //$NON-NLS-2$

		pos= getPosAfterLeftBrace(pos);

		String leadString= Util.EMPTY_STRING;
		RewriteEvent constEvent= getEvent(node, EnumDeclaration.ENUM_CONSTANTS_PROPERTY);

		if (constEvent != null && constEvent.getChangeKind() != RewriteEvent.UNCHANGED) {
			RewriteEvent[] events= constEvent.getChildren();
			if (isAllOfKind(events, RewriteEvent.INSERTED)) {
				leadString= this.formatter.FIRST_ENUM_CONST.getPrefix(getIndent(node.getStartPosition()));
			}
		}
		pos= rewriteNodeList(node, EnumDeclaration.ENUM_CONSTANTS_PROPERTY, pos, leadString, ", "); //$NON-NLS-1$

		RewriteEvent bodyEvent= getEvent(node, EnumDeclaration.BODY_DECLARATIONS_PROPERTY);
		int indent= 0;
		if (bodyEvent != null && bodyEvent.getChangeKind() != RewriteEvent.UNCHANGED) {
			boolean hasConstants= !((List) getNewValue(node, EnumDeclaration.ENUM_CONSTANTS_PROPERTY)).isEmpty();

			RewriteEvent[] children= bodyEvent.getChildren();
			try {
				if (hasConstants) {
					indent= getIndent(pos);
				} else {
					indent= getIndent(node.getStartPosition()) + 1;
				}
				int token= getScanner().readNext(pos, true);
				boolean hasSemicolon= token == TerminalTokens.TokenNameSEMICOLON;
				if (!hasSemicolon && isAllOfKind(children, RewriteEvent.INSERTED)) {
					if (!hasConstants) {
						String str= this.formatter.FIRST_ENUM_CONST.getPrefix(indent - 1);
						doTextInsert(pos, str, getEditGroup(children[0]));
					}
					if (token == TerminalTokens.TokenNameCOMMA) {
						// a comma is at the end of the enum constant before a potential semicolon
						int endPos= getScanner().getCurrentEndOffset();
						int nextToken= getScanner().readNext(endPos, true);
						if (nextToken != TerminalTokens.TokenNameSEMICOLON) {
							doTextInsert(endPos, ";", getEditGroup(children[0])); //$NON-NLS-1$
						} else {
							endPos= getScanner().getCurrentEndOffset();
							if (isAllOfKind(children, RewriteEvent.REMOVED)) {
								doTextRemove(pos, endPos - pos, getEditGroup(children[0]));
							}
						}
						pos= endPos;
					} else {
						doTextInsert(pos, ";", getEditGroup(children[0])); //$NON-NLS-1$
					}
				} else if (hasSemicolon) {
					int endPos= getScanner().getCurrentEndOffset();
					if (isAllOfKind(children, RewriteEvent.REMOVED)) {
						doTextRemove(pos, endPos - pos, getEditGroup(children[0]));
					}
					pos= endPos;
				}
			} catch (CoreException e) {
				handleException(e);
			}
		}
		rewriteParagraphList(node, EnumDeclaration.BODY_DECLARATIONS_PROPERTY, pos, indent, -1, 2);
		return false;
	}

	@Override
	public boolean visit(ExpressionMethodReference node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}
		int pos= rewriteRequiredNode(node, ExpressionMethodReference.EXPRESSION_PROPERTY);
		visitReferenceTypeArguments(node, ExpressionMethodReference.TYPE_ARGUMENTS_PROPERTY, pos);
		rewriteRequiredNode(node, ExpressionMethodReference.NAME_PROPERTY);
		return false;
	}

	@Override
	public boolean visit(MarkerAnnotation node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}
		rewriteRequiredNode(node, MarkerAnnotation.TYPE_NAME_PROPERTY);
		return false;
	}

	@Override
	public boolean visit(MemberValuePair node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}
		rewriteRequiredNode(node, MemberValuePair.NAME_PROPERTY);
		rewriteRequiredNode(node, MemberValuePair.VALUE_PROPERTY);

		return false;
	}

	@Override
	public boolean visit(Modifier node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}
		String newText= getNewValue(node, Modifier.KEYWORD_PROPERTY).toString(); // type Modifier.ModifierKeyword
		TextEditGroup group = getEditGroup(node, Modifier.KEYWORD_PROPERTY);
		doTextReplace(node.getStartPosition(), node.getLength(), newText, group);
		return false;
	}

	@Override
	public boolean visit(ModuleModifier node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}
		String newText= getNewValue(node, ModuleModifier.KEYWORD_PROPERTY).toString(); // type ModuleModifier.ModuleModifierKeyword
		TextEditGroup group = getEditGroup(node, ModuleModifier.KEYWORD_PROPERTY);
		doTextReplace(node.getStartPosition(), node.getLength(), newText, group);
		return false;
	}

	@Override
	public boolean visit(NormalAnnotation node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}
		int pos= rewriteRequiredNode(node, NormalAnnotation.TYPE_NAME_PROPERTY);
		if (isChanged(node, NormalAnnotation.VALUES_PROPERTY)) {
			// eval position after opening parent
			try {
				int startOffset= getScanner().getTokenEndOffset(TerminalTokens.TokenNameLPAREN, pos);
				rewriteNodeList(node, NormalAnnotation.VALUES_PROPERTY, startOffset, Util.EMPTY_STRING, ", "); //$NON-NLS-1$
			} catch (CoreException e) {
				handleException(e);
			}
		} else {
			voidVisit(node, NormalAnnotation.VALUES_PROPERTY);
		}
		return false;
	}

	@Override
	public boolean visit(NameQualifiedType node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}
		int pos = rewriteRequiredNode(node, NameQualifiedType.QUALIFIER_PROPERTY);
		try {
			pos = getScanner().getTokenEndOffset(TerminalTokens.TokenNameDOT, pos);
			rewriteTypeAnnotations(node, NameQualifiedType.ANNOTATIONS_PROPERTY, pos);
		} catch (CoreException e) {
			handleException(e);
		}
		rewriteRequiredNode(node, NameQualifiedType.NAME_PROPERTY);
		return false;
	}

	@Override
	public boolean visit(ParameterizedType node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}
		int pos= rewriteRequiredNode(node, ParameterizedType.TYPE_PROPERTY);
		if (isChanged(node, ParameterizedType.TYPE_ARGUMENTS_PROPERTY)) {
			// eval position after opening parent
			try {
				int startOffset= getScanner().getTokenEndOffset(TerminalTokens.TokenNameLESS, pos);
				rewriteNodeList(node, ParameterizedType.TYPE_ARGUMENTS_PROPERTY, startOffset, Util.EMPTY_STRING, ", "); //$NON-NLS-1$
			} catch (CoreException e) {
				handleException(e);
			}
		} else {
			voidVisit(node, ParameterizedType.TYPE_ARGUMENTS_PROPERTY);
		}
		return false;
	}

	@Override
	public boolean visit(QualifiedType node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}
		int pos = rewriteRequiredNode(node, QualifiedType.QUALIFIER_PROPERTY);
		if (node.getAST().apiLevel() >= JLS8_INTERNAL) {
			try {
				pos = getScanner().getTokenEndOffset(TerminalTokens.TokenNameDOT, pos);
				rewriteTypeAnnotations(node, QualifiedType.ANNOTATIONS_PROPERTY, pos);
			} catch (CoreException e) {
				handleException(e);
			}
		}
		rewriteRequiredNode(node, QualifiedType.NAME_PROPERTY);
		return false;
	}

	@Override
	public boolean visit(SingleMemberAnnotation node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}
		rewriteRequiredNode(node, SingleMemberAnnotation.TYPE_NAME_PROPERTY);
		rewriteRequiredNode(node, SingleMemberAnnotation.VALUE_PROPERTY);
		return false;
	}

	@Override
	public boolean visit(SuperMethodReference node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}
		int pos = rewriteOptionalQualifier(node, SuperMethodReference.QUALIFIER_PROPERTY, node.getStartPosition());
		visitReferenceTypeArguments(node, SuperMethodReference.TYPE_ARGUMENTS_PROPERTY, pos);
		rewriteRequiredNode(node, SuperMethodReference.NAME_PROPERTY);
		return false;
	}

	@Override
	public boolean visit(TypeMethodReference node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}
		int pos= rewriteRequiredNode(node, TypeMethodReference.TYPE_PROPERTY);
		visitReferenceTypeArguments(node, TypeMethodReference.TYPE_ARGUMENTS_PROPERTY, pos);
		rewriteRequiredNode(node, TypeMethodReference.NAME_PROPERTY);
		return false;
	}

	@Override
	public boolean visit(TypeParameter node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}
		int pos;
		if (node.getAST().apiLevel() >= JLS8_INTERNAL) {
			pos = rewriteModifiers2(node, TypeParameter.MODIFIERS_PROPERTY, node.getStartPosition());
		}
		pos= rewriteRequiredNode(node, TypeParameter.NAME_PROPERTY);
		rewriteNodeList(node, TypeParameter.TYPE_BOUNDS_PROPERTY, pos, " extends ", " & "); //$NON-NLS-1$ //$NON-NLS-2$
		return false;
	}

	@Override
	public boolean visit(TypePattern node) {
		if (!DOMASTUtil.isPatternSupported(node.getAST())) {
			return false;
		}
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}

		rewriteRequiredNode(node, TypePattern.PATTERN_VARIABLE_PROPERTY);
		return false;
	}

	@Override
	public boolean visit(WildcardType node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}
		if (node.getAST().apiLevel() >= JLS8_INTERNAL) {
			rewriteTypeAnnotations(node, WildcardType.ANNOTATIONS_PROPERTY, node.getStartPosition());
		}
		try {
			int pos= getScanner().getNextEndOffset(node.getStartPosition(), true); // pos after question mark

			Prefix prefix;
			if (Boolean.TRUE.equals(getNewValue(node, WildcardType.UPPER_BOUND_PROPERTY))) {
				prefix= this.formatter.WILDCARD_EXTENDS;
			} else {
				prefix= this.formatter.WILDCARD_SUPER;
			}

			int boundKindChange= getChangeKind(node, WildcardType.UPPER_BOUND_PROPERTY);
			if (boundKindChange != RewriteEvent.UNCHANGED) {
				int boundTypeChange= getChangeKind(node, WildcardType.BOUND_PROPERTY);
				if (boundTypeChange != RewriteEvent.INSERTED && boundTypeChange != RewriteEvent.REMOVED) {
					ASTNode type= (ASTNode) getOriginalValue(node, WildcardType.BOUND_PROPERTY);
					String str= prefix.getPrefix(0);
					doTextReplace(pos, type.getStartPosition() - pos, str, getEditGroup(node, WildcardType.BOUND_PROPERTY));
				}
			}
			rewriteNode(node, WildcardType.BOUND_PROPERTY, pos, prefix);
		} catch (CoreException e) {
			handleException(e);
		}
		return false;
	}

	@Override
	public boolean visit(YieldStatement node) {
		if (!hasChildrenChanges(node)) {
			return doVisitUnchangedChildren(node);
		}

		try {
			int offset= node.isImplicit() ? node.getStartPosition() : getScanner().getTokenEndOffset(TerminalTokens.TokenNamebreak, node.getStartPosition());
			if ((node.getAST().apiLevel() >= JLS14_INTERNAL)) {
				rewriteNode(node, YieldStatement.EXPRESSION_PROPERTY, offset, ASTRewriteFormatter.SPACE); // space between yield and label
			}
		} catch (CoreException e) {
			handleException(e);
		}
		return false;
	}

	final void handleException(Throwable e) {
		throw new IllegalArgumentException("Document does not match the AST", e); //$NON-NLS-1$
	}

}
