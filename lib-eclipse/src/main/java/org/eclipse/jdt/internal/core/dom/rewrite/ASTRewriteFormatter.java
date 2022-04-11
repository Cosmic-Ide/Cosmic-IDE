/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.core.dom.rewrite;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.jdt.core.formatter.IndentManipulation;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.DefaultPositionUpdater;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.Position;
import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

@SuppressWarnings({"rawtypes", "unchecked"})
public final class ASTRewriteFormatter {

	public static class NodeMarker extends Position {
		public Object data;
	}

	private class ExtendedFlattener extends ASTRewriteFlattener {

		private ArrayList positions;

		public ExtendedFlattener(RewriteEventStore store) {
			super(store);
			this.positions= new ArrayList();
		}

		@Override
		public void preVisit(ASTNode node) {
			Object trackData= getEventStore().getTrackedNodeData(node);
			if (trackData != null) {
				addMarker(trackData, this.result.length(), 0);
			}
			Object placeholderData= getPlaceholders().getPlaceholderData(node);
			if (placeholderData != null) {
				addMarker(placeholderData, this.result.length(), 0);
			}
		}

		@Override
		public void postVisit(ASTNode node) {
			Object placeholderData= getPlaceholders().getPlaceholderData(node);
			if (placeholderData != null) {
				fixupLength(placeholderData, this.result.length());
			}
			Object trackData= getEventStore().getTrackedNodeData(node);
			if (trackData != null) {
				fixupLength(trackData, this.result.length());
			}
		}

		@Override
		public boolean visit(Block node) {
			if (getPlaceholders().isCollapsed(node)) {
				visitList(node, Block.STATEMENTS_PROPERTY, null);
				return false;
			}
			return super.visit(node);
		}

		private NodeMarker addMarker(Object annotation, int startOffset, int length) {
			NodeMarker marker= new NodeMarker();
			marker.offset= startOffset;
			marker.length= length;
			marker.data= annotation;
			this.positions.add(marker);
			return marker;
		}

		private void fixupLength(Object data, int endOffset) {
			for (int i= this.positions.size()-1; i >= 0 ; i--) {
				NodeMarker marker= (NodeMarker) this.positions.get(i);
				if (marker.data == data) {
					marker.length= endOffset - marker.offset;
					return;
				}
			}
		}

		public NodeMarker[] getMarkers() {
			return (NodeMarker[]) this.positions.toArray(new NodeMarker[this.positions.size()]);
		}
	}

	private final String lineDelimiter;
	private final int tabWidth;
	private final int indentWidth;

	private final NodeInfoStore placeholders;
	private final RewriteEventStore eventStore;

	private final Map options;


	public ASTRewriteFormatter(NodeInfoStore placeholders, RewriteEventStore eventStore, Map options, String lineDelimiter) {
		this.placeholders= placeholders;
		this.eventStore= eventStore;

		this.options= options == null ? JavaCore.getOptions() : (Map) new HashMap(options);
		this.options.put(
				DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_RESOURCES_IN_TRY,
				DefaultCodeFormatterConstants.createAlignmentValue(true, DefaultCodeFormatterConstants.WRAP_NEXT_PER_LINE, DefaultCodeFormatterConstants.INDENT_DEFAULT));

		this.lineDelimiter= lineDelimiter;

		this.tabWidth= IndentManipulation.getTabWidth(options);
		this.indentWidth= IndentManipulation.getIndentWidth(options);
	}



	public NodeInfoStore getPlaceholders() {
		return this.placeholders;
	}

	public RewriteEventStore getEventStore() {
		return this.eventStore;
	}

	public int getTabWidth() {
		return this.tabWidth;
	}

	public int getIndentWidth() {
		return this.indentWidth;
	}

	public String getLineDelimiter() {
		return this.lineDelimiter;
	}

	/**
	 * Returns the string accumulated in the visit formatted using the default formatter.
	 * Updates the existing node's positions.
	 *
	 * @param node The node to flatten.
	 * @param initialIndentationLevel The initial indentation level.
	 * @param resultingMarkers Resulting the updated NodeMarkers.
	 * @return Returns the serialized and formatted code.
	 */
	public String getFormattedResult(ASTNode node, int initialIndentationLevel, Collection resultingMarkers) {

		ExtendedFlattener flattener= new ExtendedFlattener(this.eventStore);
		node.accept(flattener);

		NodeMarker[] markers= flattener.getMarkers();
		Collections.addAll(resultingMarkers, markers);

		String unformatted= flattener.getResult();
		TextEdit edit= formatNode(node, unformatted, initialIndentationLevel);
		if (edit == null) {
		    if (initialIndentationLevel > 0) {
		        // at least correct the indent
		        String indentString = createIndentString(initialIndentationLevel);
				ReplaceEdit[] edits = IndentManipulation.getChangeIndentEdits(unformatted, 0, this.tabWidth, this.indentWidth, indentString);
				edit= new MultiTextEdit();
				edit.addChild(new InsertEdit(0, indentString));
				edit.addChildren(edits);
		    } else {
		       return unformatted;
		    }
		}
		return evaluateFormatterEdit(unformatted, edit, markers);
	}

    public String createIndentString(int indentationUnits) {
    	return ToolFactory.createCodeFormatter(this.options).createIndentationString(indentationUnits);
    }

	public String getIndentString(String currentLine) {
		return IndentManipulation.extractIndentString(currentLine, this.tabWidth, this.indentWidth);
	}

	public String changeIndent(String code, int codeIndentLevel, String newIndent) {
		return IndentManipulation.changeIndent(code, codeIndentLevel, this.tabWidth, this.indentWidth, newIndent, this.lineDelimiter);
	}

	public int computeIndentUnits(String line) {
		return IndentManipulation.measureIndentUnits(line, this.tabWidth, this.indentWidth);
	}

	/**
	 * Evaluates the edit on the given string.
	 * @param string The string to format
	 * @param edit The edit resulted from the code formatter
	 * @param positions Positions to update or <code>null</code>.
	 * @return The formatted string
	 * @throws IllegalArgumentException If the positions are not inside the string, a
	 *  IllegalArgumentException is thrown.
	 */
	public static String evaluateFormatterEdit(String string, TextEdit edit, Position[] positions) {
		try {
			Document doc= createDocument(string, positions);
			edit.apply(doc, 0);
			if (positions != null) {
				for (int i= 0; i < positions.length; i++) {
					Assert.isTrue(!positions[i].isDeleted, "Position got deleted"); //$NON-NLS-1$
				}
			}
			return doc.get();
		} catch (BadLocationException e) {
			//JavaPlugin.log(e); // bug in the formatter
			Assert.isTrue(false, "Fromatter created edits with wrong positions: " + e.getMessage()); //$NON-NLS-1$
		}
		return null;
	}

	public TextEdit formatString(int kind, String string, int offset, int length, int indentationLevel) {
		return ToolFactory.createCodeFormatter(this.options).format(kind, string, offset, length, indentationLevel, this.lineDelimiter);
	}

	/**
	 * Creates edits that describe how to format the given string. Returns <code>null</code> if the code could not be formatted for the given kind.
	 * @param node Node describing the type of the string
	 * @param str The unformatted string
	 * @param indentationLevel
	 * @return Returns the edit representing the result of the formatter
	 * @throws IllegalArgumentException If the offset and length are not inside the string, a
	 *  IllegalArgumentException is thrown.
	 */
	private TextEdit formatNode(ASTNode node, String str, int indentationLevel) {
		int code;
		String prefix= ""; //$NON-NLS-1$
		String suffix= ""; //$NON-NLS-1$
		if (node instanceof Statement) {
			code= CodeFormatter.K_STATEMENTS;
			if (node.getNodeType() == ASTNode.SWITCH_CASE) {
				prefix= "switch(1) {"; //$NON-NLS-1$
				suffix= "}"; //$NON-NLS-1$
				code= CodeFormatter.K_STATEMENTS;
			}
		} else if (node instanceof Expression && node.getNodeType() != ASTNode.VARIABLE_DECLARATION_EXPRESSION) {
			if (node instanceof Annotation) {
				suffix= "\nclass A {}"; //$NON-NLS-1$
				code= CodeFormatter.K_COMPILATION_UNIT;
			} else {
				code= CodeFormatter.K_EXPRESSION;
			}
		} else if (node instanceof BodyDeclaration) {
			code= CodeFormatter.K_CLASS_BODY_DECLARATIONS;
		} else {
			switch (node.getNodeType()) {
				case ASTNode.ARRAY_TYPE:
				case ASTNode.PARAMETERIZED_TYPE:
				case ASTNode.PRIMITIVE_TYPE:
				case ASTNode.QUALIFIED_TYPE:
				case ASTNode.SIMPLE_TYPE:
					prefix= "void m(final "; //$NON-NLS-1$
					suffix= " x);"; //$NON-NLS-1$
					code= CodeFormatter.K_CLASS_BODY_DECLARATIONS;
					break;
				case ASTNode.WILDCARD_TYPE:
					prefix= "A<"; //$NON-NLS-1$
					suffix= "> x;"; //$NON-NLS-1$
					code= CodeFormatter.K_CLASS_BODY_DECLARATIONS;
					break;
				case ASTNode.COMPILATION_UNIT:
					code= CodeFormatter.K_COMPILATION_UNIT;
					break;
				case ASTNode.MODULE_DECLARATION:
					code= CodeFormatter.K_MODULE_INFO;
					break;
				case ASTNode.VARIABLE_DECLARATION_EXPRESSION:
					suffix= ";"; //$NON-NLS-1$
					code= CodeFormatter.K_STATEMENTS;
					break;
				case ASTNode.SINGLE_VARIABLE_DECLARATION:
					prefix= "void m("; //$NON-NLS-1$
					suffix= ");"; //$NON-NLS-1$
					code= CodeFormatter.K_CLASS_BODY_DECLARATIONS;
					break;
				case ASTNode.VARIABLE_DECLARATION_FRAGMENT:
					prefix= "A "; //$NON-NLS-1$
					suffix= ";"; //$NON-NLS-1$
					code= CodeFormatter.K_STATEMENTS;
					break;
				case ASTNode.PACKAGE_DECLARATION:
				case ASTNode.IMPORT_DECLARATION:
					suffix= "\nclass A {}"; //$NON-NLS-1$
					code= CodeFormatter.K_COMPILATION_UNIT;
					break;
				case ASTNode.JAVADOC:
					suffix= "\nclass A {}"; //$NON-NLS-1$
					code= CodeFormatter.K_COMPILATION_UNIT;
					break;
				case ASTNode.CATCH_CLAUSE:
					prefix= "try {}"; //$NON-NLS-1$
					code= CodeFormatter.K_STATEMENTS;
					break;
				case ASTNode.ANONYMOUS_CLASS_DECLARATION:
					prefix= "new A()"; //$NON-NLS-1$
					suffix= ";"; //$NON-NLS-1$
					code= CodeFormatter.K_STATEMENTS;
					break;
				case ASTNode.MEMBER_VALUE_PAIR:
					prefix= "@Author("; //$NON-NLS-1$
					suffix= ") class x {}"; //$NON-NLS-1$
					code= CodeFormatter.K_COMPILATION_UNIT;
					break;
				case ASTNode.MODIFIER:
					suffix= " class x {}"; //$NON-NLS-1$
					code= CodeFormatter.K_COMPILATION_UNIT;
					break;
				case ASTNode.MODULE_MODIFIER:
					code= CodeFormatter.K_MODULE_INFO;
					break;
				case ASTNode.TYPE_PARAMETER:
					prefix= "class X<"; //$NON-NLS-1$
					suffix= "> {}"; //$NON-NLS-1$
					code= CodeFormatter.K_COMPILATION_UNIT;
					break;
				case ASTNode.MEMBER_REF:
				case ASTNode.METHOD_REF:
				case ASTNode.METHOD_REF_PARAMETER:
				case ASTNode.TAG_ELEMENT:
				case ASTNode.TEXT_ELEMENT:
					// javadoc formatting disabled due to bug 93644
					return null;

//				wiat for bug 93644
//				case ASTNode.MEMBER_REF:
//				case ASTNode.METHOD_REF:
//					prefix= "/**\n * @see ";
//					suffix= "\n*/";
//					code= CodeFormatter.K_JAVA_DOC;
//					break;
//				case ASTNode.METHOD_REF_PARAMETER:
//					prefix= "/**\n * @see A#foo(";
//					suffix= ")\n*/";
//					code= CodeFormatter.K_JAVA_DOC;
//					break;
//				case ASTNode.TAG_ELEMENT:
//				case ASTNode.TEXT_ELEMENT:
//					prefix= "/**\n * ";
//					suffix= "\n*/";
//					code= CodeFormatter.K_JAVA_DOC;
//					break;
				default:
					//Assert.isTrue(false, "Node type not covered: " + node.getClass().getName());
					return null;
			}
		}

		String concatStr= prefix + str + suffix;
		TextEdit edit= formatString(code, concatStr, prefix.length(), str.length(), indentationLevel);

		if (prefix.length() > 0) {
			edit= shifEdit(edit, prefix.length());
		}
		return edit;
	}

	private static TextEdit shifEdit(TextEdit oldEdit, int diff) {
		TextEdit newEdit;
		if (oldEdit instanceof ReplaceEdit) {
			ReplaceEdit edit= (ReplaceEdit) oldEdit;
			newEdit= new ReplaceEdit(edit.getOffset() - diff, edit.getLength(), edit.getText());
		} else if (oldEdit instanceof InsertEdit) {
			InsertEdit edit= (InsertEdit) oldEdit;
			newEdit= new InsertEdit(edit.getOffset() - diff,  edit.getText());
		} else if (oldEdit instanceof DeleteEdit) {
			DeleteEdit edit= (DeleteEdit) oldEdit;
			newEdit= new DeleteEdit(edit.getOffset() - diff,  edit.getLength());
		} else if (oldEdit instanceof MultiTextEdit) {
			newEdit= new MultiTextEdit();
		} else {
			return null; // not supported
		}
		TextEdit[] children= oldEdit.getChildren();
		for (int i= 0; i < children.length; i++) {
			TextEdit shifted= shifEdit(children[i], diff);
			if (shifted != null) {
				newEdit.addChild(shifted);
			}
		}
		return newEdit;
	}

	private static Document createDocument(String string, Position[] positions) throws IllegalArgumentException {
		Document doc= new Document(string);
		try {
			if (positions != null) {
				final String POS_CATEGORY= "myCategory"; //$NON-NLS-1$

				doc.addPositionCategory(POS_CATEGORY);
				doc.addPositionUpdater(new DefaultPositionUpdater(POS_CATEGORY) {
					@Override
					protected boolean notDeleted() {
						int start= this.fOffset;
						int end= start + this.fLength;
						if (start < this.fPosition.offset && (this.fPosition.offset + this.fPosition.length < end)) {
							this.fPosition.offset= end; // deleted positions: set to end of remove
							return false;
						}
						return true;
					}
				});
				for (int i= 0; i < positions.length; i++) {
					try {
						doc.addPosition(POS_CATEGORY, positions[i]);
					} catch (BadLocationException e) {
						throw new IllegalArgumentException("Position outside of string. offset: " + positions[i].offset + ", length: " + positions[i].length + ", string size: " + string.length(), e);   //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
					}
				}
			}
		} catch (BadPositionCategoryException cannotHappen) {
			// can not happen: category is correctly set up
		}
		return doc;
	}



    public static interface Prefix {
		String getPrefix(int indent);
	}

	public static interface BlockContext {
		String[] getPrefixAndSuffix(int indent, ASTNode node, RewriteEventStore events);
	}

	public static class ConstPrefix implements Prefix {
		private String prefix;

		public ConstPrefix(String prefix) {
			this.prefix= prefix;
		}

		@Override
		public String getPrefix(int indent) {
			return this.prefix;
		}
	}

	private class FormattingPrefix implements Prefix {
		private int kind;
		private String string;
		private int start;
		private int length;

		public FormattingPrefix(String string, String sub, int kind) {
			this.start= string.indexOf(sub);
			this.length= sub.length();
			this.string= string;
			this.kind= kind;
		}

		@Override
		public String getPrefix(int indent) {
			Position pos= new Position(this.start, this.length);
			String str= this.string;
			TextEdit res= formatString(this.kind, str, 0, str.length(), indent);
			if (res != null) {
				str= evaluateFormatterEdit(str, res, new Position[] { pos });
			}
			return str.substring(pos.offset + 1, pos.offset + pos.length - 1);
		}
	}

	private class BlockFormattingPrefix implements BlockContext {
		private String prefix;
		private int start;

		public BlockFormattingPrefix(String prefix, int start) {
			this.start= start;
			this.prefix= prefix;
		}

		@Override
		public String[] getPrefixAndSuffix(int indent, ASTNode node, RewriteEventStore events) {
			String nodeString= ASTRewriteFlattener.asString(node, events);
			String str= this.prefix + nodeString;
			Position pos= new Position(this.start, this.prefix.length() + 1 - this.start);

			TextEdit res= formatString(CodeFormatter.K_STATEMENTS, str, 0, str.length(), indent);
			if (res != null) {
				str= evaluateFormatterEdit(str, res, new Position[] { pos });
			}
			return new String[] { str.substring(pos.offset + 1, pos.offset + pos.length - 1), ""}; //$NON-NLS-1$
		}
	}

	private class BlockFormattingPrefixSuffix implements BlockContext {
		private String prefix;
		private String suffix;
		private int start;

		public BlockFormattingPrefixSuffix(String prefix, String suffix, int start) {
			this.start= start;
			this.suffix= suffix;
			this.prefix= prefix;
		}

		@Override
		public String[] getPrefixAndSuffix(int indent, ASTNode node, RewriteEventStore events) {
			String nodeString= ASTRewriteFlattener.asString(node, events);
			int nodeStart= this.prefix.length();
			int nodeEnd= nodeStart + nodeString.length() - 1;

			String str= this.prefix + nodeString + this.suffix;

			Position pos1= new Position(this.start, nodeStart + 1 - this.start);
			Position pos2= new Position(nodeEnd, 2);

			TextEdit res= formatString(CodeFormatter.K_STATEMENTS, str, 0, str.length(), indent);
			if (res != null) {
				str= evaluateFormatterEdit(str, res, new Position[] { pos1, pos2 });
			}
			return new String[] {
				str.substring(pos1.offset + 1, pos1.offset + pos1.length - 1),
				str.substring(pos2.offset + 1, pos2.offset + pos2.length - 1)
			};
		}
	}

	public final static Prefix NONE= new ConstPrefix(""); //$NON-NLS-1$
	public final static Prefix SPACE= new ConstPrefix(" "); //$NON-NLS-1$
	public final static Prefix ASSERT_COMMENT= new ConstPrefix(" : "); //$NON-NLS-1$

	public final Prefix VAR_INITIALIZER= new FormattingPrefix("A a={};", "a={" , CodeFormatter.K_STATEMENTS); //$NON-NLS-1$ //$NON-NLS-2$
	public final Prefix METHOD_BODY= new FormattingPrefix("void a() {}", ") {" , CodeFormatter.K_CLASS_BODY_DECLARATIONS); //$NON-NLS-1$ //$NON-NLS-2$
	public final Prefix FINALLY_BLOCK= new FormattingPrefix("try {} finally {}", "} finally {", CodeFormatter.K_STATEMENTS); //$NON-NLS-1$ //$NON-NLS-2$
	public final Prefix CATCH_BLOCK= new FormattingPrefix("try {} catch(Exception e) {}", "} c" , CodeFormatter.K_STATEMENTS); //$NON-NLS-1$ //$NON-NLS-2$
	public final Prefix ANNOT_MEMBER_DEFAULT= new FormattingPrefix("String value() default 1;", ") default 1" , CodeFormatter.K_CLASS_BODY_DECLARATIONS); //$NON-NLS-1$ //$NON-NLS-2$
	public final Prefix ENUM_BODY_START= new FormattingPrefix("enum E { A(){void foo(){}} }", "){v" , CodeFormatter.K_COMPILATION_UNIT); //$NON-NLS-1$ //$NON-NLS-2$
	public final Prefix ENUM_BODY_END= new FormattingPrefix("enum E { A(){void foo(){ }}, B}", "}}," , CodeFormatter.K_COMPILATION_UNIT); //$NON-NLS-1$ //$NON-NLS-2$
	public final Prefix WILDCARD_EXTENDS= new FormattingPrefix("A<? extends B> a;", "? extends B" , CodeFormatter.K_CLASS_BODY_DECLARATIONS); //$NON-NLS-1$ //$NON-NLS-2$
	public final Prefix WILDCARD_SUPER= new FormattingPrefix("A<? super B> a;", "? super B" , CodeFormatter.K_CLASS_BODY_DECLARATIONS); //$NON-NLS-1$ //$NON-NLS-2$

	public final Prefix FIRST_ENUM_CONST= new FormattingPrefix("enum E { X;}", "{ X" , CodeFormatter.K_COMPILATION_UNIT); //$NON-NLS-1$ //$NON-NLS-2$
	public final Prefix ANNOTATION_SEPARATION= new FormattingPrefix("@A @B class C {}", "A @" , CodeFormatter.K_COMPILATION_UNIT); //$NON-NLS-1$ //$NON-NLS-2$
	public final Prefix PARAM_ANNOTATION_SEPARATION= new FormattingPrefix("void foo(@A @B C p) { }", "A @" , CodeFormatter.K_CLASS_BODY_DECLARATIONS); //$NON-NLS-1$ //$NON-NLS-2$
	public final Prefix LOCAL_ANNOTATION_SEPARATION= new FormattingPrefix("@A @B C p;", "A @" , CodeFormatter.K_STATEMENTS); //$NON-NLS-1$ //$NON-NLS-2$
	public final Prefix TYPE_ANNOTATION_SEPARATION= new FormattingPrefix("C<@A @B D> l;", "A @" , CodeFormatter.K_STATEMENTS); //$NON-NLS-1$ //$NON-NLS-2$
	public final Prefix VARARGS= new FormattingPrefix("void foo(A ... a) { }", "A ." , CodeFormatter.K_CLASS_BODY_DECLARATIONS); //$NON-NLS-1$ //$NON-NLS-2$
	public final Prefix TRY_RESOURCES = new FormattingPrefix("try (A a = new A(); B b = new B()) {}", "; B" , CodeFormatter.K_STATEMENTS); //$NON-NLS-1$ //$NON-NLS-2$
	public final Prefix TRY_RESOURCES_PAREN = new FormattingPrefix("try (A a = new A(); B b = new B()) {}", "y (" , CodeFormatter.K_STATEMENTS); //$NON-NLS-1$ //$NON-NLS-2$

	public final BlockContext IF_BLOCK_WITH_ELSE= new BlockFormattingPrefixSuffix("if (true)", "else{}", 8); //$NON-NLS-1$ //$NON-NLS-2$
	public final BlockContext IF_BLOCK_NO_ELSE= new BlockFormattingPrefix("if (true)", 8); //$NON-NLS-1$
	public final BlockContext ELSE_AFTER_STATEMENT= new BlockFormattingPrefix("if (true) foo();else ", 15); //$NON-NLS-1$
	public final BlockContext ELSE_AFTER_BLOCK= new BlockFormattingPrefix("if (true) {}else ", 11); //$NON-NLS-1$

	public final Prefix CASE_SEPARATION= new FormattingPrefix("case A, B", "A" , CodeFormatter.K_EXPRESSION); //$NON-NLS-1$ //$NON-NLS-2$

	public final BlockContext FOR_BLOCK= new BlockFormattingPrefix("for (;;) ", 7); //$NON-NLS-1$
	public final BlockContext WHILE_BLOCK= new BlockFormattingPrefix("while (true)", 11); //$NON-NLS-1$
	public final BlockContext DO_BLOCK= new BlockFormattingPrefixSuffix("do ", "while (true);", 1); //$NON-NLS-1$ //$NON-NLS-2$

}
