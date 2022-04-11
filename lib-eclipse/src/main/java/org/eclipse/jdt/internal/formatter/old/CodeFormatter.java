/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
package org.eclipse.jdt.internal.formatter.old;

import java.util.Map;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.jdt.internal.compiler.parser.TerminalTokens;
import org.eclipse.jdt.internal.formatter.DefaultCodeFormatter;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

/** <h2>How to format a piece of code ?</h2>
 * <ul><li>Create an instance of <code>CodeFormatter</code>
 * <li>Use the method <code>void format(aString)</code>
 * on this instance to format <code>aString</code>.
 * It will return the formatted string.</ul>
 * @deprecated
*/
@SuppressWarnings({ "rawtypes", "unchecked" })
public class CodeFormatter implements TerminalTokens, org.eclipse.jdt.core.ICodeFormatter {

	private Map options;

	public CodeFormatter(Map options) {
		if (options == null) {
			this.options = JavaCore.getOptions();
		} else {
			this.options = options;
		}
	}

	@Override
	public String format(String string, int indentLevel, int[] positions, String lineSeparator) {
		// initialize the new formatter with old options
		Map newOptions = DefaultCodeFormatterConstants.getEclipse21Settings();

		Object formatterNewLineOpeningBrace = this.options.get(JavaCore.FORMATTER_NEWLINE_OPENING_BRACE);
		if (formatterNewLineOpeningBrace != null) {
			if (JavaCore.INSERT.equals(formatterNewLineOpeningBrace)) {
				newOptions.put(DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_ANONYMOUS_TYPE_DECLARATION, DefaultCodeFormatterConstants.NEXT_LINE);
				newOptions.put(DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_BLOCK, DefaultCodeFormatterConstants.NEXT_LINE);
				newOptions.put(DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_CONSTRUCTOR_DECLARATION, DefaultCodeFormatterConstants.NEXT_LINE);
				newOptions.put(DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_METHOD_DECLARATION, DefaultCodeFormatterConstants.NEXT_LINE);
				newOptions.put(DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_SWITCH, DefaultCodeFormatterConstants.NEXT_LINE);
				newOptions.put(DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_TYPE_DECLARATION, DefaultCodeFormatterConstants.NEXT_LINE);
			} else {
				newOptions.put(DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_ANONYMOUS_TYPE_DECLARATION, DefaultCodeFormatterConstants.END_OF_LINE);
				newOptions.put(DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_BLOCK, DefaultCodeFormatterConstants.END_OF_LINE);
				newOptions.put(DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_CONSTRUCTOR_DECLARATION, DefaultCodeFormatterConstants.END_OF_LINE);
				newOptions.put(DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_METHOD_DECLARATION, DefaultCodeFormatterConstants.END_OF_LINE);
				newOptions.put(DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_SWITCH, DefaultCodeFormatterConstants.END_OF_LINE);
				newOptions.put(DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_TYPE_DECLARATION, DefaultCodeFormatterConstants.END_OF_LINE);
			}
		}
		Object formatterNewLineControl = this.options.get(JavaCore.FORMATTER_NEWLINE_CONTROL);
		if (formatterNewLineControl != null) {
			if (JavaCore.INSERT.equals(formatterNewLineControl)) {
				newOptions.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_BEFORE_CATCH_IN_TRY_STATEMENT, JavaCore.INSERT);
				newOptions.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_BEFORE_ELSE_IN_IF_STATEMENT, JavaCore.INSERT);
				newOptions.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_BEFORE_FINALLY_IN_TRY_STATEMENT, JavaCore.INSERT);
				newOptions.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_BEFORE_WHILE_IN_DO_STATEMENT, JavaCore.INSERT);
			} else {
				newOptions.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_BEFORE_CATCH_IN_TRY_STATEMENT, JavaCore.DO_NOT_INSERT);
				newOptions.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_BEFORE_ELSE_IN_IF_STATEMENT, JavaCore.DO_NOT_INSERT);
				newOptions.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_BEFORE_FINALLY_IN_TRY_STATEMENT, JavaCore.DO_NOT_INSERT);
				newOptions.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_BEFORE_WHILE_IN_DO_STATEMENT, JavaCore.DO_NOT_INSERT);
			}
		}
		Object formatterClearBlankLines = this.options.get(JavaCore.FORMATTER_CLEAR_BLANK_LINES);
		if (formatterClearBlankLines != null) {
			if (JavaCore.PRESERVE_ONE.equals(formatterClearBlankLines)) {
				newOptions.put(DefaultCodeFormatterConstants.FORMATTER_NUMBER_OF_EMPTY_LINES_TO_PRESERVE, "1"); //$NON-NLS-1$
			} else {
				newOptions.put(DefaultCodeFormatterConstants.FORMATTER_NUMBER_OF_EMPTY_LINES_TO_PRESERVE, "0"); //$NON-NLS-1$
			}
		}
		Object formatterNewLineElseIf = this.options.get(JavaCore.FORMATTER_NEWLINE_ELSE_IF);
		if (formatterNewLineElseIf != null) {
			if (JavaCore.INSERT.equals(formatterNewLineElseIf)) {
				newOptions.put(DefaultCodeFormatterConstants.FORMATTER_COMPACT_ELSE_IF, DefaultCodeFormatterConstants.FALSE);
			} else {
				newOptions.put(DefaultCodeFormatterConstants.FORMATTER_COMPACT_ELSE_IF, DefaultCodeFormatterConstants.TRUE);
			}
		}
		Object formatterNewLineEmptyBlock = this.options.get(JavaCore.FORMATTER_NEWLINE_EMPTY_BLOCK);
		if (formatterNewLineEmptyBlock != null) {
			if (JavaCore.INSERT.equals(formatterNewLineEmptyBlock)) {
				newOptions.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_IN_EMPTY_BLOCK, JavaCore.INSERT);
			} else {
				newOptions.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_IN_EMPTY_BLOCK, JavaCore.DO_NOT_INSERT);
			}
		}
		Object formatterCompactAssignment = this.options.get(JavaCore.FORMATTER_COMPACT_ASSIGNMENT);
		if (formatterCompactAssignment != null) {
			if (JavaCore.COMPACT.equals(formatterCompactAssignment)) {
				newOptions.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_ASSIGNMENT_OPERATOR, JavaCore.DO_NOT_INSERT);
			} else {
				newOptions.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_ASSIGNMENT_OPERATOR, JavaCore.INSERT);
			}
		}
		if (this.options.get(JavaCore.FORMATTER_SPACE_CASTEXPRESSION) != null) {
			if (JavaCore.INSERT.equals(this.options.get(JavaCore.FORMATTER_SPACE_CASTEXPRESSION))) {
				newOptions.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_CLOSING_PAREN_IN_CAST, JavaCore.INSERT);
			} else {
				newOptions.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_CLOSING_PAREN_IN_CAST, JavaCore.DO_NOT_INSERT);
			}
		}
		newOptions.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, this.options.get(JavaCore.FORMATTER_TAB_CHAR));
		newOptions.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, this.options.get(JavaCore.FORMATTER_TAB_SIZE));
		newOptions.put(DefaultCodeFormatterConstants.FORMATTER_LINE_SPLIT, this.options.get(JavaCore.FORMATTER_LINE_SPLIT));
		newOptions.put(DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_ARRAY_INITIALIZER, DefaultCodeFormatterConstants.END_OF_LINE);
		newOptions.put(DefaultCodeFormatterConstants.FORMATTER_CONTINUATION_INDENTATION, "1");//$NON-NLS-1$
		newOptions.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_PARAMETERS_IN_METHOD_DECLARATION, DefaultCodeFormatterConstants.createAlignmentValue(false, DefaultCodeFormatterConstants.WRAP_ONE_PER_LINE, DefaultCodeFormatterConstants.INDENT_BY_ONE));
		newOptions.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_PARAMETERS_IN_CONSTRUCTOR_DECLARATION, DefaultCodeFormatterConstants.createAlignmentValue(false, DefaultCodeFormatterConstants.WRAP_ONE_PER_LINE, DefaultCodeFormatterConstants.INDENT_BY_ONE));
		newOptions.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ARGUMENTS_IN_ALLOCATION_EXPRESSION, DefaultCodeFormatterConstants.createAlignmentValue(false, DefaultCodeFormatterConstants.WRAP_ONE_PER_LINE, DefaultCodeFormatterConstants.INDENT_BY_ONE));
		newOptions.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ARGUMENTS_IN_EXPLICIT_CONSTRUCTOR_CALL, DefaultCodeFormatterConstants.createAlignmentValue(false, DefaultCodeFormatterConstants.WRAP_ONE_PER_LINE, DefaultCodeFormatterConstants.INDENT_BY_ONE));
		newOptions.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ARGUMENTS_IN_METHOD_INVOCATION, DefaultCodeFormatterConstants.createAlignmentValue(false, DefaultCodeFormatterConstants.WRAP_ONE_PER_LINE, DefaultCodeFormatterConstants.INDENT_BY_ONE));
		newOptions.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ARGUMENTS_IN_QUALIFIED_ALLOCATION_EXPRESSION, DefaultCodeFormatterConstants.createAlignmentValue(false, DefaultCodeFormatterConstants.WRAP_ONE_PER_LINE, DefaultCodeFormatterConstants.INDENT_BY_ONE));
		newOptions.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_THROWS_CLAUSE_IN_METHOD_DECLARATION, DefaultCodeFormatterConstants.createAlignmentValue(false, DefaultCodeFormatterConstants.WRAP_ONE_PER_LINE, DefaultCodeFormatterConstants.INDENT_BY_ONE));
		newOptions.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_MULTIPLE_FIELDS, DefaultCodeFormatterConstants.createAlignmentValue(false, DefaultCodeFormatterConstants.WRAP_ONE_PER_LINE, DefaultCodeFormatterConstants.INDENT_BY_ONE));
		newOptions.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_BINARY_EXPRESSION, DefaultCodeFormatterConstants.createAlignmentValue(false, DefaultCodeFormatterConstants.WRAP_ONE_PER_LINE, DefaultCodeFormatterConstants.INDENT_BY_ONE));
		newOptions.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_ARRAY_INITIALIZER, JavaCore.INSERT);
		newOptions.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_BRACE_IN_ARRAY_INITIALIZER, JavaCore.INSERT);
		newOptions.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_BRACE_IN_ARRAY_INITIALIZER, JavaCore.INSERT);

		DefaultCodeFormatter defaultCodeFormatter = new DefaultCodeFormatter(newOptions);
		TextEdit textEdit = defaultCodeFormatter.format(org.eclipse.jdt.core.formatter.CodeFormatter.K_UNKNOWN, string, 0, string.length(), indentLevel, lineSeparator);
		if (positions != null && textEdit != null) {
			// update positions
			TextEdit[] edits = textEdit.getChildren();
			int textEditSize = edits.length;
			int editsIndex = 0;
			int delta = 0;
			int originalSourceLength = string.length() - 1;
			if (textEditSize != 0) {
				for (int i = 0, max = positions.length; i < max; i++) {
					int currentPosition = positions[i];
					if (currentPosition > originalSourceLength) {
						currentPosition = originalSourceLength;
					}
					ReplaceEdit currentEdit = (ReplaceEdit) edits[editsIndex];
					while (currentEdit.getOffset() <= currentPosition) {
						delta += currentEdit.getText().length() - currentEdit.getLength();
						editsIndex++;
						if (editsIndex < textEditSize) {
							currentEdit = (ReplaceEdit) edits[editsIndex];
						} else {
							break;
						}
					}
					positions[i] = currentPosition + delta;
				}
			}
		}
		return org.eclipse.jdt.internal.core.util.Util.editedString(string, textEdit);
	}
}
