/*******************************************************************************
 * Copyright (c) 2000, 2022 IBM Corporation and others.
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
 *     Brock Janiczak - Contribution for bug 150741
 *     Ray V. (voidstar@gmail.com) - Contribution for bug 282988
 *     Jesper S Moller - Contribution for bug 402173
 *     Mateusz Matela <mateusz.matela@gmail.com> - [formatter] Formatter does not format Java code correctly, especially when max line width is set - https://bugs.eclipse.org/303519
 *     Lars Vogel <Lars.Vogel@vogella.com> - Contributions for
 *     						Bug 473178
 *******************************************************************************/
package org.eclipse.jdt.internal.formatter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.jdt.internal.compiler.util.Util;

/**
 * This is still subject to changes before 3.0.
 * @since 3.0
 */

public class DefaultCodeFormatterOptions {

	/** Internal constants related to wrapping alignment settings */
	public static class Alignment {

		/*
		 * Alignment modes
		 */
		public static final int M_FORCE = 1; // if bit set, then alignment will be non-optional (default is optional)
		public static final int M_INDENT_ON_COLUMN = 2; // if bit set, broken fragments will be aligned on current location column (default is to break at current indentation level)
		public static final int	M_INDENT_BY_ONE = 4; // if bit set, broken fragments will be indented one level below current (not using continuation indentation)
		public static final int M_INDENT_DEFAULT = 0;
		public static final int M_INDENT_PRESERVE = 8;

		// split modes can be combined either with M_FORCE or M_INDENT_ON_COLUMN

		/** foobar(#fragment1, #fragment2, <ul>
		 *  <li>    #fragment3, #fragment4 </li>
		 * </ul>
		 */
		public static final int M_COMPACT_SPLIT = 16; // fill each line with all possible fragments

		/** foobar(<ul>
		 * <li>    #fragment1, #fragment2,  </li>
		 * <li>     #fragment3, #fragment4, </li>
		 * </ul>
		 */
		public static final int M_COMPACT_FIRST_BREAK_SPLIT = 32; //  compact mode, but will first try to break before first fragment

		/** foobar(<ul>
		 * <li>     #fragment1,  </li>
		 * <li>     #fragment2,  </li>
		 * <li>     #fragment3 </li>
		 * <li>     #fragment4,  </li>
		 * </ul>
		 */
		public static final int M_ONE_PER_LINE_SPLIT = 32+16; // one fragment per line

		/**
		 * foobar(<ul>
		 * <li>     #fragment1,  </li>
		 * <li>        #fragment2,  </li>
		 * <li>        #fragment3 </li>
		 * <li>        #fragment4,  </li>
		 * </ul>
		 */
		public static final int M_NEXT_SHIFTED_SPLIT = 64; // one fragment per line, subsequent are indented further

		/** foobar(#fragment1, <ul>
		 * <li>      #fragment2,  </li>
		 * <li>      #fragment3 </li>
		 * <li>      #fragment4,  </li>
		 * </ul>
		 */
		public static final int M_NEXT_PER_LINE_SPLIT = 64+16; // one per line, except first fragment (if possible)

		public static final int M_NO_ALIGNMENT = 0;

		public static final int SPLIT_MASK = M_ONE_PER_LINE_SPLIT | M_NEXT_SHIFTED_SPLIT | M_COMPACT_SPLIT | M_COMPACT_FIRST_BREAK_SPLIT | M_NEXT_PER_LINE_SPLIT;
	}


	public static final int TAB = 1;
	public static final int SPACE = 2;
	public static final int MIXED = 4;

	public static DefaultCodeFormatterOptions getDefaultSettings() {
		DefaultCodeFormatterOptions options = new DefaultCodeFormatterOptions();
		options.setDefaultSettings();
		return options;
	}

	public static DefaultCodeFormatterOptions getEclipseDefaultSettings() {
		DefaultCodeFormatterOptions options = new DefaultCodeFormatterOptions();
		options.setEclipseDefaultSettings();
		return options;
	}

	public static DefaultCodeFormatterOptions getJavaConventionsSettings() {
		DefaultCodeFormatterOptions options = new DefaultCodeFormatterOptions();
		options.setJavaConventionsSettings();
		return options;
	}

	public int alignment_for_annotations_on_type;
	public int alignment_for_type_annotations;
	public int alignment_for_annotations_on_enum_constant;
	public int alignment_for_annotations_on_field;
	public int alignment_for_annotations_on_method;
	public int alignment_for_annotations_on_package;
	public int alignment_for_annotations_on_parameter;
	public int alignment_for_annotations_on_local_variable;
	public int alignment_for_arguments_in_allocation_expression;
	public int alignment_for_arguments_in_annotation;
	public int alignment_for_arguments_in_enum_constant;
	public int alignment_for_arguments_in_explicit_constructor_call;
	public int alignment_for_arguments_in_method_invocation;
	public int alignment_for_arguments_in_qualified_allocation_expression;
	public int alignment_for_assertion_message;
	public int alignment_for_assignment;
	public int alignment_for_multiplicative_operator;
	public int alignment_for_additive_operator;
	public int alignment_for_string_concatenation;
	public int alignment_for_shift_operator;
	public int alignment_for_relational_operator;
	public int alignment_for_bitwise_operator;
	public int alignment_for_logical_operator;
	public int alignment_for_compact_if;
	public int alignment_for_compact_loop;
	public int alignment_for_conditional_expression;
	public int alignment_for_conditional_expression_chain;
	public int alignment_for_enum_constants;
	public int alignment_for_expressions_in_array_initializer;
	public int alignment_for_expressions_in_for_loop_header;
	public int alignment_for_expressions_in_switch_case_with_arrow;
	public int alignment_for_expressions_in_switch_case_with_colon;
	public int alignment_for_method_declaration;
	public int alignment_for_module_statements;
	// TODO following option cannot be set in preferences dialog (but it's used by old.CodeFormatter)
	public int alignment_for_multiple_fields;
	public int alignment_for_parameterized_type_references;
	public int alignment_for_parameters_in_constructor_declaration;
	public int alignment_for_parameters_in_method_declaration;
	public int alignment_for_record_components;
	public int alignment_for_selector_in_method_invocation;
	public int alignment_for_superclass_in_type_declaration;
	public int alignment_for_superinterfaces_in_enum_declaration;
	public int alignment_for_superinterfaces_in_record_declaration;
	public int alignment_for_superinterfaces_in_type_declaration;
	public int alignment_for_switch_case_with_arrow;
	public int alignment_for_throws_clause_in_constructor_declaration;
	public int alignment_for_throws_clause_in_method_declaration;
	public int alignment_for_type_arguments;
	public int alignment_for_type_parameters;
	public int alignment_for_resources_in_try;
	public int alignment_for_union_type_in_multicatch;
	public boolean align_selector_in_method_invocation_on_expression_first_line;

	public boolean align_type_members_on_columns;
	public boolean align_variable_declarations_on_columns;
	public boolean align_assignment_statements_on_columns;
	public boolean align_with_spaces;
	public int align_fields_grouping_blank_lines;

	public String brace_position_for_annotation_type_declaration;
	public String brace_position_for_anonymous_type_declaration;
	public String brace_position_for_array_initializer;
	public String brace_position_for_block;
	public String brace_position_for_block_in_case;
	public String brace_position_for_constructor_declaration;
	public String brace_position_for_enum_constant;
	public String brace_position_for_enum_declaration;
	public String brace_position_for_lambda_body;
	public String brace_position_for_method_declaration;
	public String brace_position_for_type_declaration;
	public String brace_position_for_record_constructor;
	public String brace_position_for_record_declaration;
	public String brace_position_for_switch;

	public String parenthesis_positions_in_method_declaration;
	public String parenthesis_positions_in_method_invocation;
	public String parenthesis_positions_in_enum_constant_declaration;
	public String parenthesis_positions_in_record_declaration;
	public String parenthesis_positions_in_if_while_statement;
	public String parenthesis_positions_in_for_statement;
	public String parenthesis_positions_in_switch_statement;
	public String parenthesis_positions_in_try_clause;
	public String parenthesis_positions_in_catch_clause;
	public String parenthesis_positions_in_annotation;
	public String parenthesis_positions_in_lambda_declaration;

	public int continuation_indentation;
	public int continuation_indentation_for_array_initializer;

	public int blank_lines_after_imports;
	public int blank_lines_after_package;
	public int blank_lines_before_field;
	public int blank_lines_before_first_class_body_declaration;
	public int blank_lines_after_last_class_body_declaration;
	public int blank_lines_before_imports;
	public int blank_lines_before_member_type;
	public int blank_lines_before_abstract_method;
	public int blank_lines_before_method;
	public int blank_lines_before_new_chunk;
	public int blank_lines_before_package;
	public int blank_lines_between_import_groups;
	public int blank_lines_between_type_declarations;
	public int blank_lines_at_beginning_of_method_body;
	public int blank_lines_at_end_of_method_body;
	public int blank_lines_at_beginning_of_code_block;
	public int blank_lines_at_end_of_code_block;
	public int blank_lines_before_code_block;
	public int blank_lines_after_code_block;
	public int blank_lines_between_statement_groups_in_switch;

	public boolean comment_clear_blank_lines_in_javadoc_comment;
	public boolean comment_clear_blank_lines_in_block_comment;
	public boolean comment_new_lines_at_block_boundaries;
	public boolean comment_new_lines_at_javadoc_boundaries;
	public boolean comment_format_javadoc_comment;
	public boolean comment_format_line_comment;
	public boolean comment_format_line_comment_starting_on_first_column;
	public boolean comment_format_block_comment;
	public boolean comment_format_header;
	public boolean comment_format_html;
	public boolean comment_format_source;
	public boolean comment_indent_parameter_description;
	public boolean comment_indent_tag_description;
	public boolean comment_indent_root_tags;
	public boolean comment_align_tags_names_descriptions;
	public boolean comment_align_tags_descriptions_grouped;
	public boolean comment_insert_empty_line_before_root_tags;
	public boolean comment_insert_empty_line_between_different_tags;
	public boolean comment_insert_new_line_for_parameter;
	public boolean comment_preserve_white_space_between_code_and_line_comments;
	public int comment_line_length;
	public boolean comment_count_line_length_from_starting_position;

	public boolean use_tags;
	public char[] disabling_tag;
	public char[] enabling_tag;
	private final static char[] DEFAULT_DISABLING_TAG = "@formatter:off".toCharArray(); //$NON-NLS-1$
	private final static char[] DEFAULT_ENABLING_TAG = "@formatter:on".toCharArray(); //$NON-NLS-1$

	public boolean indent_statements_compare_to_block;
	public boolean indent_statements_compare_to_body;
	public boolean indent_body_declarations_compare_to_annotation_declaration_header;
	public boolean indent_body_declarations_compare_to_enum_constant_header;
	public boolean indent_body_declarations_compare_to_enum_declaration_header;
	public boolean indent_body_declarations_compare_to_record_header;
	public boolean indent_body_declarations_compare_to_type_header;
	public boolean indent_breaks_compare_to_cases;
	public boolean indent_empty_lines;
	public boolean indent_switchstatements_compare_to_cases;
	public boolean indent_switchstatements_compare_to_switch;
	public int indentation_size;

	public boolean insert_new_line_after_annotation_on_type;
	public boolean insert_new_line_after_type_annotation;
	public boolean insert_new_line_after_annotation_on_enum_constant;
	public boolean insert_new_line_after_annotation_on_field;
	public boolean insert_new_line_after_annotation_on_method;
	public boolean insert_new_line_after_annotation_on_package;
	public boolean insert_new_line_after_annotation_on_parameter;
	public boolean insert_new_line_after_annotation_on_local_variable;
	public boolean insert_new_line_after_label;
	public boolean insert_new_line_after_opening_brace_in_array_initializer;
	public boolean insert_new_line_at_end_of_file_if_missing;
	public boolean insert_new_line_before_catch_in_try_statement;
	public boolean insert_new_line_before_closing_brace_in_array_initializer;
	public boolean insert_new_line_before_else_in_if_statement;
	public boolean insert_new_line_before_finally_in_try_statement;
	public boolean insert_new_line_before_while_in_do_statement;

	public String keep_loop_body_block_on_one_line;
	public String keep_if_then_body_block_on_one_line;
	public String keep_switch_body_block_on_one_line;
	public String keep_switch_case_with_arrow_on_one_line;
	public String keep_code_block_on_one_line;
	public String keep_lambda_body_block_on_one_line;
	public String keep_method_body_on_one_line;
	public String keep_type_declaration_on_one_line;
	public String keep_anonymous_type_declaration_on_one_line;
	public String keep_enum_declaration_on_one_line;
	public String keep_enum_constant_declaration_on_one_line;
	public String keep_annotation_declaration_on_one_line;
	public String keep_record_declaration_on_one_line;
	public String keep_record_constructor_on_one_line;
	public boolean keep_simple_getter_setter_on_one_line;

	public boolean insert_space_after_and_in_type_parameter;
	public boolean insert_space_after_arrow_in_switch_case;
	public boolean insert_space_after_arrow_in_switch_default;
	public boolean insert_space_after_assignment_operator;
	public boolean insert_space_after_at_in_annotation;
	public boolean insert_space_after_at_in_annotation_type_declaration;
	public boolean insert_space_after_multiplicative_operator;
	public boolean insert_space_after_additive_operator;
	public boolean insert_space_after_string_concatenation;
	public boolean insert_space_after_shift_operator;
	public boolean insert_space_after_relational_operator;
	public boolean insert_space_after_bitwise_operator;
	public boolean insert_space_after_logical_operator;
	public boolean insert_space_after_closing_angle_bracket_in_type_arguments;
	public boolean insert_space_after_closing_angle_bracket_in_type_parameters;
	public boolean insert_space_after_closing_paren_in_cast;
	public boolean insert_space_after_closing_brace_in_block;
	public boolean insert_space_after_colon_in_assert;
	//TODO field is never used
	public boolean insert_space_after_colon_in_case;
	public boolean insert_space_after_colon_in_conditional;
	public boolean insert_space_after_colon_in_for;
	public boolean insert_space_after_colon_in_labeled_statement;
	public boolean insert_space_after_comma_in_allocation_expression;
	public boolean insert_space_after_comma_in_annotation;
	public boolean insert_space_after_comma_in_array_initializer;
	public boolean insert_space_after_comma_in_constructor_declaration_parameters;
	public boolean insert_space_after_comma_in_constructor_declaration_throws;
	public boolean insert_space_after_comma_in_enum_constant_arguments;
	public boolean insert_space_after_comma_in_enum_declarations;
	public boolean insert_space_after_comma_in_explicit_constructor_call_arguments;
	public boolean insert_space_after_comma_in_for_increments;
	public boolean insert_space_after_comma_in_for_inits;
	public boolean insert_space_after_comma_in_method_invocation_arguments;
	public boolean insert_space_after_comma_in_method_declaration_parameters;
	public boolean insert_space_after_comma_in_method_declaration_throws;
	public boolean insert_space_after_comma_in_multiple_field_declarations;
	public boolean insert_space_after_comma_in_multiple_local_declarations;
	public boolean insert_space_after_comma_in_parameterized_type_reference;
	public boolean insert_space_after_comma_in_permitted_types;
	public boolean insert_space_after_comma_in_record_components;
	public boolean insert_space_after_comma_in_superinterfaces;
	public boolean insert_space_after_comma_in_switch_case_expressions;
	public boolean insert_space_after_comma_in_type_arguments;
	public boolean insert_space_after_comma_in_type_parameters;
	public boolean insert_space_after_ellipsis;
	public boolean insert_space_after_lambda_arrow;
	public boolean insert_space_after_not_operator;
	public boolean insert_space_after_opening_angle_bracket_in_parameterized_type_reference;
	public boolean insert_space_after_opening_angle_bracket_in_type_arguments;
	public boolean insert_space_after_opening_angle_bracket_in_type_parameters;
	public boolean insert_space_after_opening_bracket_in_array_allocation_expression;
	public boolean insert_space_after_opening_bracket_in_array_reference;
	public boolean insert_space_after_opening_brace_in_array_initializer;
	public boolean insert_space_after_opening_paren_in_annotation;
	public boolean insert_space_after_opening_paren_in_cast;
	public boolean insert_space_after_opening_paren_in_catch;
	public boolean insert_space_after_opening_paren_in_constructor_declaration;
	public boolean insert_space_after_opening_paren_in_enum_constant;
	public boolean insert_space_after_opening_paren_in_for;
	public boolean insert_space_after_opening_paren_in_if;
	public boolean insert_space_after_opening_paren_in_method_declaration;
	public boolean insert_space_after_opening_paren_in_method_invocation;
	public boolean insert_space_after_opening_paren_in_parenthesized_expression;
	public boolean insert_space_after_opening_paren_in_record_declaration;
	public boolean insert_space_after_opening_paren_in_switch;
	public boolean insert_space_after_opening_paren_in_synchronized;
	public boolean insert_space_after_opening_paren_in_try;
	public boolean insert_space_after_opening_paren_in_while;
	public boolean insert_space_after_postfix_operator;
	public boolean insert_space_after_prefix_operator;
	public boolean insert_space_after_question_in_conditional;
	public boolean insert_space_after_question_in_wilcard;
	public boolean insert_space_after_semicolon_in_for;
	public boolean insert_space_after_semicolon_in_try_resources;
	public boolean insert_space_after_unary_operator;
	public boolean insert_space_before_and_in_type_parameter;
	public boolean insert_space_before_arrow_in_switch_case;
	public boolean insert_space_before_arrow_in_switch_default;
	public boolean insert_space_before_at_in_annotation_type_declaration;
	public boolean insert_space_before_assignment_operator;
	public boolean insert_space_before_multiplicative_operator;
	public boolean insert_space_before_additive_operator;
	public boolean insert_space_before_string_concatenation;
	public boolean insert_space_before_shift_operator;
	public boolean insert_space_before_relational_operator;
	public boolean insert_space_before_bitwise_operator;
	public boolean insert_space_before_logical_operator;
	public boolean insert_space_before_closing_angle_bracket_in_parameterized_type_reference;
	public boolean insert_space_before_closing_angle_bracket_in_type_arguments;
	public boolean insert_space_before_closing_angle_bracket_in_type_parameters;
	public boolean insert_space_before_closing_brace_in_array_initializer;
	public boolean insert_space_before_closing_bracket_in_array_allocation_expression;
	public boolean insert_space_before_closing_bracket_in_array_reference;
	public boolean insert_space_before_closing_paren_in_annotation;
	public boolean insert_space_before_closing_paren_in_cast;
	public boolean insert_space_before_closing_paren_in_catch;
	public boolean insert_space_before_closing_paren_in_constructor_declaration;
	public boolean insert_space_before_closing_paren_in_enum_constant;
	public boolean insert_space_before_closing_paren_in_for;
	public boolean insert_space_before_closing_paren_in_if;
	public boolean insert_space_before_closing_paren_in_method_declaration;
	public boolean insert_space_before_closing_paren_in_method_invocation;
	public boolean insert_space_before_closing_paren_in_parenthesized_expression;
	public boolean insert_space_before_closing_paren_in_record_declaration;
	public boolean insert_space_before_closing_paren_in_switch;
	public boolean insert_space_before_closing_paren_in_synchronized;
	public boolean insert_space_before_closing_paren_in_try;
	public boolean insert_space_before_closing_paren_in_while;
	public boolean insert_space_before_colon_in_assert;
	public boolean insert_space_before_colon_in_case;
	public boolean insert_space_before_colon_in_conditional;
	public boolean insert_space_before_colon_in_default;
	public boolean insert_space_before_colon_in_for;
	public boolean insert_space_before_colon_in_labeled_statement;
	public boolean insert_space_before_comma_in_allocation_expression;
	public boolean insert_space_before_comma_in_annotation;
	public boolean insert_space_before_comma_in_array_initializer;
	public boolean insert_space_before_comma_in_constructor_declaration_parameters;
	public boolean insert_space_before_comma_in_constructor_declaration_throws;
	public boolean insert_space_before_comma_in_enum_constant_arguments;
	public boolean insert_space_before_comma_in_enum_declarations;
	public boolean insert_space_before_comma_in_explicit_constructor_call_arguments;
	public boolean insert_space_before_comma_in_for_increments;
	public boolean insert_space_before_comma_in_for_inits;
	public boolean insert_space_before_comma_in_method_invocation_arguments;
	public boolean insert_space_before_comma_in_method_declaration_parameters;
	public boolean insert_space_before_comma_in_method_declaration_throws;
	public boolean insert_space_before_comma_in_multiple_field_declarations;
	public boolean insert_space_before_comma_in_multiple_local_declarations;
	public boolean insert_space_before_comma_in_parameterized_type_reference;
	public boolean insert_space_before_comma_in_permitted_types;
	public boolean insert_space_before_comma_in_record_components;
	public boolean insert_space_before_comma_in_superinterfaces;
	public boolean insert_space_before_comma_in_switch_case_expressions;
	public boolean insert_space_before_comma_in_type_arguments;
	public boolean insert_space_before_comma_in_type_parameters;
	public boolean insert_space_before_ellipsis;
	public boolean insert_space_before_lambda_arrow;
	public boolean insert_space_before_parenthesized_expression_in_return;
	public boolean insert_space_before_parenthesized_expression_in_throw;
	public boolean insert_space_before_question_in_wilcard;
	public boolean insert_space_before_opening_angle_bracket_in_parameterized_type_reference;
	public boolean insert_space_before_opening_angle_bracket_in_type_arguments;
	public boolean insert_space_before_opening_angle_bracket_in_type_parameters;
	public boolean insert_space_before_opening_brace_in_annotation_type_declaration;
	public boolean insert_space_before_opening_brace_in_anonymous_type_declaration;
	public boolean insert_space_before_opening_brace_in_array_initializer;
	public boolean insert_space_before_opening_brace_in_block;
	public boolean insert_space_before_opening_brace_in_constructor_declaration;
	public boolean insert_space_before_opening_brace_in_enum_constant;
	public boolean insert_space_before_opening_brace_in_enum_declaration;
	public boolean insert_space_before_opening_brace_in_method_declaration;
	public boolean insert_space_before_opening_brace_in_record_constructor;
	public boolean insert_space_before_opening_brace_in_record_declaration;
	public boolean insert_space_before_opening_brace_in_type_declaration;
	public boolean insert_space_before_opening_bracket_in_array_allocation_expression;
	public boolean insert_space_before_opening_bracket_in_array_reference;
	public boolean insert_space_before_opening_bracket_in_array_type_reference;
	public boolean insert_space_before_opening_paren_in_annotation;
	public boolean insert_space_before_opening_paren_in_annotation_type_member_declaration;
	public boolean insert_space_before_opening_paren_in_catch;
	public boolean insert_space_before_opening_paren_in_constructor_declaration;
	public boolean insert_space_before_opening_paren_in_enum_constant;
	public boolean insert_space_before_opening_paren_in_for;
	public boolean insert_space_before_opening_paren_in_if;
	public boolean insert_space_before_opening_paren_in_method_invocation;
	public boolean insert_space_before_opening_paren_in_method_declaration;
	public boolean insert_space_before_opening_paren_in_record_declaration;
	public boolean insert_space_before_opening_paren_in_switch;
	public boolean insert_space_before_opening_paren_in_try;
	public boolean insert_space_before_opening_brace_in_switch;
	public boolean insert_space_before_opening_paren_in_synchronized;
	public boolean insert_space_before_opening_paren_in_parenthesized_expression;
	public boolean insert_space_before_opening_paren_in_while;
	public boolean insert_space_before_postfix_operator;
	public boolean insert_space_before_prefix_operator;
	public boolean insert_space_before_question_in_conditional;
	public boolean insert_space_before_semicolon;
	public boolean insert_space_before_semicolon_in_for;
	public boolean insert_space_before_semicolon_in_try_resources;
	public boolean insert_space_before_unary_operator;
	public boolean insert_space_between_brackets_in_array_type_reference;
	public boolean insert_space_between_empty_braces_in_array_initializer;
	public boolean insert_space_between_empty_brackets_in_array_allocation_expression;
	public boolean insert_space_between_empty_parens_in_annotation_type_member_declaration;
	public boolean insert_space_between_empty_parens_in_constructor_declaration;
	public boolean insert_space_between_empty_parens_in_enum_constant;
	public boolean insert_space_between_empty_parens_in_method_declaration;
	public boolean insert_space_between_empty_parens_in_method_invocation;
	public boolean compact_else_if;
	public boolean keep_guardian_clause_on_one_line;
	public boolean keep_else_statement_on_same_line;
	public boolean keep_empty_array_initializer_on_one_line;
	public boolean keep_simple_if_on_one_line;
	public boolean keep_then_statement_on_same_line;
	public boolean keep_simple_for_body_on_same_line;
	public boolean keep_simple_while_body_on_same_line;
	public boolean keep_simple_do_while_body_on_same_line;
	public boolean never_indent_block_comments_on_first_column;
	public boolean never_indent_line_comments_on_first_column;
	public int number_of_empty_lines_to_preserve;
	public boolean join_wrapped_lines;
	public boolean join_lines_in_comments;
	public boolean put_empty_statement_on_new_line;
	public int tab_size;
	public int page_width;
	public int tab_char;
	public boolean use_tabs_only_for_leading_indentations;
	public int text_block_indentation;
	public boolean wrap_before_multiplicative_operator;
	public boolean wrap_before_additive_operator;
	public boolean wrap_before_assertion_message_operator;
	public boolean wrap_before_string_concatenation;
	public boolean wrap_before_shift_operator;
	public boolean wrap_before_relational_operator;
	public boolean wrap_before_bitwise_operator;
	public boolean wrap_before_logical_operator;
	public boolean wrap_before_or_operator_multicatch;
	public boolean wrap_before_conditional_operator;
	public boolean wrap_before_assignment_operator;
	public boolean wrap_before_switch_case_arrow_operator;
	public boolean wrap_outer_expressions_when_nested;

	public int initial_indentation_level;
	public String line_separator;

	private final static List<String> KEEP_ON_ONE_LINE_VALUES = Arrays.asList(
			DefaultCodeFormatterConstants.ONE_LINE_NEVER,
			DefaultCodeFormatterConstants.ONE_LINE_IF_EMPTY,
			DefaultCodeFormatterConstants.ONE_LINE_IF_SINGLE_ITEM,
			DefaultCodeFormatterConstants.ONE_LINE_ALWAYS,
			DefaultCodeFormatterConstants.ONE_LINE_PRESERVE);

	private final static List<String> BRACE_POSITION_VALUES = Arrays.asList(
			DefaultCodeFormatterConstants.END_OF_LINE,
			DefaultCodeFormatterConstants.NEXT_LINE,
			DefaultCodeFormatterConstants.NEXT_LINE_SHIFTED,
			DefaultCodeFormatterConstants.NEXT_LINE_ON_WRAP);

	private final static List<String> PARENTHESIS_POSITION_VALUES = Arrays.asList(
			DefaultCodeFormatterConstants.COMMON_LINES,
			DefaultCodeFormatterConstants.SEPARATE_LINES_IF_NOT_EMPTY,
			DefaultCodeFormatterConstants.SEPARATE_LINES_IF_WRAPPED,
			DefaultCodeFormatterConstants.SEPARATE_LINES,
			DefaultCodeFormatterConstants.SEPARATE_LINES,
			DefaultCodeFormatterConstants.PRESERVE_POSITIONS);

	private DefaultCodeFormatterOptions() {
		// cannot be instantiated
	}

	public DefaultCodeFormatterOptions(Map<String, String> settings) {
		setDefaultSettings();
		if (settings == null) return;
		set(settings);
	}

	private String getAlignment(int alignment) {
		return Integer.toString(alignment);
	}

	public Map<String, String> getMap() {
		Map<String, String> options = new HashMap<>();
		options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ANNOTATIONS_ON_TYPE, getAlignment(this.alignment_for_annotations_on_type));
		options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_TYPE_ANNOTATIONS, getAlignment(this.alignment_for_type_annotations));
		options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ANNOTATIONS_ON_ENUM_CONSTANT, getAlignment(this.alignment_for_annotations_on_enum_constant));
		options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ANNOTATIONS_ON_FIELD, getAlignment(this.alignment_for_annotations_on_field));
		options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ANNOTATIONS_ON_METHOD, getAlignment(this.alignment_for_annotations_on_method));
		options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ANNOTATIONS_ON_PACKAGE, getAlignment(this.alignment_for_annotations_on_package));
		options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ANNOTATIONS_ON_PARAMETER, getAlignment(this.alignment_for_annotations_on_parameter));
		options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ANNOTATIONS_ON_LOCAL_VARIABLE, getAlignment(this.alignment_for_annotations_on_local_variable));
		options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ARGUMENTS_IN_ALLOCATION_EXPRESSION, getAlignment(this.alignment_for_arguments_in_allocation_expression));
		options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ARGUMENTS_IN_ANNOTATION, getAlignment(this.alignment_for_arguments_in_annotation));
		options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ARGUMENTS_IN_ENUM_CONSTANT, getAlignment(this.alignment_for_arguments_in_enum_constant));
		options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ARGUMENTS_IN_EXPLICIT_CONSTRUCTOR_CALL, getAlignment(this.alignment_for_arguments_in_explicit_constructor_call));
		options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ARGUMENTS_IN_METHOD_INVOCATION, getAlignment(this.alignment_for_arguments_in_method_invocation));
		options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ARGUMENTS_IN_QUALIFIED_ALLOCATION_EXPRESSION, getAlignment(this.alignment_for_arguments_in_qualified_allocation_expression));
		options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ASSERTION_MESSAGE, getAlignment(this.alignment_for_assertion_message));
		options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ASSIGNMENT, getAlignment(this.alignment_for_assignment));
		options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_MULTIPLICATIVE_OPERATOR ,getAlignment(this.alignment_for_multiplicative_operator));
		options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ADDITIVE_OPERATOR ,getAlignment(this.alignment_for_additive_operator));
		options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_STRING_CONCATENATION ,getAlignment(this.alignment_for_string_concatenation));
		options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_SHIFT_OPERATOR ,getAlignment(this.alignment_for_shift_operator));
		options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_RELATIONAL_OPERATOR ,getAlignment(this.alignment_for_relational_operator));
		options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_BITWISE_OPERATOR ,getAlignment(this.alignment_for_bitwise_operator));
		options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_LOGICAL_OPERATOR ,getAlignment(this.alignment_for_logical_operator));
		options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_COMPACT_IF, getAlignment(this.alignment_for_compact_if));
		options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_COMPACT_LOOP, getAlignment(this.alignment_for_compact_loop));
		options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_CONDITIONAL_EXPRESSION, getAlignment(this.alignment_for_conditional_expression));
		options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_CONDITIONAL_EXPRESSION_CHAIN, getAlignment(this.alignment_for_conditional_expression_chain));
		options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ENUM_CONSTANTS, getAlignment(this.alignment_for_enum_constants));
		options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_EXPRESSIONS_IN_ARRAY_INITIALIZER, getAlignment(this.alignment_for_expressions_in_array_initializer));
		options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_EXPRESSIONS_IN_FOR_LOOP_HEADER, getAlignment(this.alignment_for_expressions_in_for_loop_header));
		options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_EXPRESSIONS_IN_SWITCH_CASE_WITH_ARROW, getAlignment(this.alignment_for_expressions_in_switch_case_with_arrow));
		options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_EXPRESSIONS_IN_SWITCH_CASE_WITH_COLON, getAlignment(this.alignment_for_expressions_in_switch_case_with_colon));
		options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_METHOD_DECLARATION, getAlignment(this.alignment_for_method_declaration));
		options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_MODULE_STATEMENTS, getAlignment(this.alignment_for_module_statements));
		options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_MULTIPLE_FIELDS, getAlignment(this.alignment_for_multiple_fields));
		options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_PARAMETERIZED_TYPE_REFERENCES, getAlignment(this.alignment_for_parameterized_type_references));
		options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_PARAMETERS_IN_CONSTRUCTOR_DECLARATION, getAlignment(this.alignment_for_parameters_in_constructor_declaration));
		options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_PARAMETERS_IN_METHOD_DECLARATION, getAlignment(this.alignment_for_parameters_in_method_declaration));
		options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_RECORD_COMPONENTS, getAlignment(this.alignment_for_record_components));
		options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_RESOURCES_IN_TRY, getAlignment(this.alignment_for_resources_in_try));
		options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_SELECTOR_IN_METHOD_INVOCATION, getAlignment(this.alignment_for_selector_in_method_invocation));
		options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_SUPERCLASS_IN_TYPE_DECLARATION, getAlignment(this.alignment_for_superclass_in_type_declaration));
		options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_SUPERINTERFACES_IN_ENUM_DECLARATION, getAlignment(this.alignment_for_superinterfaces_in_enum_declaration));
		options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_SUPERINTERFACES_IN_RECORD_DECLARATION, getAlignment(this.alignment_for_superinterfaces_in_record_declaration));
		options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_SUPERINTERFACES_IN_TYPE_DECLARATION, getAlignment(this.alignment_for_superinterfaces_in_type_declaration));
		options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_SWITCH_CASE_WITH_ARROW, getAlignment(this.alignment_for_switch_case_with_arrow));
		options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_THROWS_CLAUSE_IN_CONSTRUCTOR_DECLARATION, getAlignment(this.alignment_for_throws_clause_in_constructor_declaration));
		options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_THROWS_CLAUSE_IN_METHOD_DECLARATION, getAlignment(this.alignment_for_throws_clause_in_method_declaration));
		options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_TYPE_ARGUMENTS, getAlignment(this.alignment_for_type_arguments));
		options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_TYPE_PARAMETERS, getAlignment(this.alignment_for_type_parameters));
		options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_UNION_TYPE_IN_MULTICATCH, getAlignment(this.alignment_for_union_type_in_multicatch));
		options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGN_SELECTOR_IN_METHOD_INVOCATION_ON_EXPRESSION_FIRST_LINE, this.align_selector_in_method_invocation_on_expression_first_line ? DefaultCodeFormatterConstants.TRUE : DefaultCodeFormatterConstants.FALSE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGN_TYPE_MEMBERS_ON_COLUMNS, this.align_type_members_on_columns ? DefaultCodeFormatterConstants.TRUE : DefaultCodeFormatterConstants.FALSE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGN_VARIABLE_DECLARATIONS_ON_COLUMNS, this.align_variable_declarations_on_columns ? DefaultCodeFormatterConstants.TRUE : DefaultCodeFormatterConstants.FALSE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGN_ASSIGNMENT_STATEMENTS_ON_COLUMNS, this.align_assignment_statements_on_columns ? DefaultCodeFormatterConstants.TRUE : DefaultCodeFormatterConstants.FALSE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGN_FIELDS_GROUPING_BLANK_LINES, Integer.toString(this.align_fields_grouping_blank_lines));
		options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGN_WITH_SPACES, this.align_with_spaces ? DefaultCodeFormatterConstants.TRUE : DefaultCodeFormatterConstants.FALSE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_ANNOTATION_TYPE_DECLARATION, this.brace_position_for_annotation_type_declaration);
		options.put(DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_ANONYMOUS_TYPE_DECLARATION, this.brace_position_for_anonymous_type_declaration);
		options.put(DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_ARRAY_INITIALIZER, this.brace_position_for_array_initializer);
		options.put(DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_BLOCK, this.brace_position_for_block);
		options.put(DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_BLOCK_IN_CASE, this.brace_position_for_block_in_case);
		options.put(DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_CONSTRUCTOR_DECLARATION, this.brace_position_for_constructor_declaration);
		options.put(DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_ENUM_CONSTANT, this.brace_position_for_enum_constant);
		options.put(DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_ENUM_DECLARATION, this.brace_position_for_enum_declaration);
		options.put(DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_METHOD_DECLARATION, this.brace_position_for_method_declaration);
		options.put(DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_TYPE_DECLARATION, this.brace_position_for_type_declaration);
		options.put(DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_LAMBDA_BODY, this.brace_position_for_lambda_body);
		options.put(DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_RECORD_CONSTRUCTOR, this.brace_position_for_record_constructor);
		options.put(DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_RECORD_DECLARATION, this.brace_position_for_record_declaration);
		options.put(DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_SWITCH, this.brace_position_for_switch);
		options.put(DefaultCodeFormatterConstants.FORMATTER_PARENTHESES_POSITIONS_IN_METHOD_DECLARATION, this.parenthesis_positions_in_method_declaration);
		options.put(DefaultCodeFormatterConstants.FORMATTER_PARENTHESES_POSITIONS_IN_METHOD_INVOCATION, this.parenthesis_positions_in_method_invocation);
		options.put(DefaultCodeFormatterConstants.FORMATTER_PARENTHESES_POSITIONS_IN_ENUM_CONSTANT_DECLARATION, this.parenthesis_positions_in_enum_constant_declaration);
		options.put(DefaultCodeFormatterConstants.FORMATTER_PARENTHESES_POSITIONS_IN_RECORD_DECLARATION, this.parenthesis_positions_in_record_declaration);
		options.put(DefaultCodeFormatterConstants.FORMATTER_PARENTHESES_POSITIONS_IN_IF_WHILE_STATEMENT, this.parenthesis_positions_in_if_while_statement);
		options.put(DefaultCodeFormatterConstants.FORMATTER_PARENTHESES_POSITIONS_IN_FOR_STATEMENT, this.parenthesis_positions_in_for_statement);
		options.put(DefaultCodeFormatterConstants.FORMATTER_PARENTHESES_POSITIONS_IN_SWITCH_STATEMENT, this.parenthesis_positions_in_switch_statement);
		options.put(DefaultCodeFormatterConstants.FORMATTER_PARENTHESES_POSITIONS_IN_TRY_CLAUSE, this.parenthesis_positions_in_try_clause);
		options.put(DefaultCodeFormatterConstants.FORMATTER_PARENTHESES_POSITIONS_IN_CATCH_CLAUSE, this.parenthesis_positions_in_catch_clause);
		options.put(DefaultCodeFormatterConstants.FORMATTER_PARENTHESES_POSITIONS_IN_ANNOTATION, this.parenthesis_positions_in_annotation);
		options.put(DefaultCodeFormatterConstants.FORMATTER_PARENTHESES_POSITIONS_IN_LAMBDA_DECLARATION, this.parenthesis_positions_in_lambda_declaration);
		options.put(DefaultCodeFormatterConstants.FORMATTER_COMMENT_CLEAR_BLANK_LINES_IN_BLOCK_COMMENT, this.comment_clear_blank_lines_in_block_comment ? DefaultCodeFormatterConstants.TRUE : DefaultCodeFormatterConstants.FALSE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_COMMENT_CLEAR_BLANK_LINES_IN_JAVADOC_COMMENT, this.comment_clear_blank_lines_in_javadoc_comment ? DefaultCodeFormatterConstants.TRUE : DefaultCodeFormatterConstants.FALSE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_COMMENT_NEW_LINES_AT_BLOCK_BOUNDARIES, this.comment_new_lines_at_block_boundaries ? DefaultCodeFormatterConstants.TRUE : DefaultCodeFormatterConstants.FALSE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_COMMENT_NEW_LINES_AT_JAVADOC_BOUNDARIES, this.comment_new_lines_at_javadoc_boundaries ? DefaultCodeFormatterConstants.TRUE : DefaultCodeFormatterConstants.FALSE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_BLOCK_COMMENT, this.comment_format_block_comment ? DefaultCodeFormatterConstants.TRUE : DefaultCodeFormatterConstants.FALSE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_HEADER, this.comment_format_header ? DefaultCodeFormatterConstants.TRUE : DefaultCodeFormatterConstants.FALSE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_HTML, this.comment_format_html ? DefaultCodeFormatterConstants.TRUE : DefaultCodeFormatterConstants.FALSE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_JAVADOC_COMMENT, this.comment_format_javadoc_comment ? DefaultCodeFormatterConstants.TRUE : DefaultCodeFormatterConstants.FALSE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_LINE_COMMENT, this.comment_format_line_comment ? DefaultCodeFormatterConstants.TRUE : DefaultCodeFormatterConstants.FALSE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_LINE_COMMENT_STARTING_ON_FIRST_COLUMN, this.comment_format_line_comment_starting_on_first_column ? DefaultCodeFormatterConstants.TRUE : DefaultCodeFormatterConstants.FALSE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_SOURCE, this.comment_format_source ? DefaultCodeFormatterConstants.TRUE : DefaultCodeFormatterConstants.FALSE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_COMMENT_INDENT_PARAMETER_DESCRIPTION, this.comment_indent_parameter_description ? DefaultCodeFormatterConstants.TRUE : DefaultCodeFormatterConstants.FALSE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_COMMENT_INDENT_TAG_DESCRIPTION, this.comment_indent_tag_description ? DefaultCodeFormatterConstants.TRUE : DefaultCodeFormatterConstants.FALSE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_COMMENT_INDENT_ROOT_TAGS, this.comment_indent_root_tags ? DefaultCodeFormatterConstants.TRUE : DefaultCodeFormatterConstants.FALSE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_COMMENT_ALIGN_TAGS_NAMES_DESCRIPTIONS, this.comment_align_tags_names_descriptions ? DefaultCodeFormatterConstants.TRUE : DefaultCodeFormatterConstants.FALSE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_COMMENT_ALIGN_TAGS_DESCREIPTIONS_GROUPED, this.comment_align_tags_descriptions_grouped ? DefaultCodeFormatterConstants.TRUE : DefaultCodeFormatterConstants.FALSE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_COMMENT_INSERT_EMPTY_LINE_BEFORE_ROOT_TAGS, this.comment_insert_empty_line_before_root_tags ? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_COMMENT_INSERT_EMPTY_LINE_BETWEEN_DIFFERENT_TAGS, this.comment_insert_empty_line_between_different_tags ? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_COMMENT_INSERT_NEW_LINE_FOR_PARAMETER, this.comment_insert_new_line_for_parameter ? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_COMMENT_PRESERVE_WHITE_SPACE_BETWEEN_CODE_AND_LINE_COMMENT, this.comment_preserve_white_space_between_code_and_line_comments ? DefaultCodeFormatterConstants.TRUE : DefaultCodeFormatterConstants.FALSE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_COMMENT_LINE_LENGTH, Integer.toString(this.comment_line_length));
		options.put(DefaultCodeFormatterConstants.FORMATTER_COMMENT_COUNT_LINE_LENGTH_FROM_STARTING_POSITION, this.comment_count_line_length_from_starting_position ? DefaultCodeFormatterConstants.TRUE : DefaultCodeFormatterConstants.FALSE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_CONTINUATION_INDENTATION, Integer.toString(this.continuation_indentation));
		options.put(DefaultCodeFormatterConstants.FORMATTER_CONTINUATION_INDENTATION_FOR_ARRAY_INITIALIZER, Integer.toString(this.continuation_indentation_for_array_initializer));
		options.put(DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_AFTER_IMPORTS, Integer.toString(this.blank_lines_after_imports));
		options.put(DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_AFTER_PACKAGE, Integer.toString(this.blank_lines_after_package));
		options.put(DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_FIELD, Integer.toString(this.blank_lines_before_field));
		options.put(DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_FIRST_CLASS_BODY_DECLARATION, Integer.toString(this.blank_lines_before_first_class_body_declaration));
		options.put(DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_AFTER_LAST_CLASS_BODY_DECLARATION, Integer.toString(this.blank_lines_after_last_class_body_declaration));
		options.put(DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_IMPORTS, Integer.toString(this.blank_lines_before_imports));
		options.put(DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_MEMBER_TYPE, Integer.toString(this.blank_lines_before_member_type));
		options.put(DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_ABSTRACT_METHOD, Integer.toString(this.blank_lines_before_abstract_method));
		options.put(DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_METHOD, Integer.toString(this.blank_lines_before_method));
		options.put(DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_NEW_CHUNK, Integer.toString(this.blank_lines_before_new_chunk));
		options.put(DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_PACKAGE, Integer.toString(this.blank_lines_before_package));
		options.put(DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BETWEEN_IMPORT_GROUPS, Integer.toString(this.blank_lines_between_import_groups));
		options.put(DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BETWEEN_TYPE_DECLARATIONS, Integer.toString(this.blank_lines_between_type_declarations));
		options.put(DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_AT_BEGINNING_OF_METHOD_BODY, Integer.toString(this.blank_lines_at_beginning_of_method_body));
		options.put(DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_AT_END_OF_METHOD_BODY, Integer.toString(this.blank_lines_at_end_of_method_body));
		options.put(DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_AT_BEGINNING_OF_CODE_BLOCK, Integer.toString(this.blank_lines_at_beginning_of_code_block));
		options.put(DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_AT_END_OF_CODE_BLOCK, Integer.toString(this.blank_lines_at_end_of_code_block));
		options.put(DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_CODE_BLOCK, Integer.toString(this.blank_lines_before_code_block));
		options.put(DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_AFTER_CODE_BLOCK, Integer.toString(this.blank_lines_after_code_block));
		options.put(DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BETWEEN_STATEMENT_GROUPS_IN_SWITCH, Integer.toString(this.blank_lines_between_statement_groups_in_switch));
		options.put(DefaultCodeFormatterConstants.FORMATTER_INDENT_STATEMENTS_COMPARE_TO_BLOCK, this.indent_statements_compare_to_block ? DefaultCodeFormatterConstants.TRUE : DefaultCodeFormatterConstants.FALSE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INDENT_STATEMENTS_COMPARE_TO_BODY, this.indent_statements_compare_to_body ? DefaultCodeFormatterConstants.TRUE : DefaultCodeFormatterConstants.FALSE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INDENT_BODY_DECLARATIONS_COMPARE_TO_ANNOTATION_DECLARATION_HEADER, this.indent_body_declarations_compare_to_annotation_declaration_header ? DefaultCodeFormatterConstants.TRUE : DefaultCodeFormatterConstants.FALSE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INDENT_BODY_DECLARATIONS_COMPARE_TO_ENUM_CONSTANT_HEADER, this.indent_body_declarations_compare_to_enum_constant_header ? DefaultCodeFormatterConstants.TRUE : DefaultCodeFormatterConstants.FALSE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INDENT_BODY_DECLARATIONS_COMPARE_TO_ENUM_DECLARATION_HEADER, this.indent_body_declarations_compare_to_enum_declaration_header ? DefaultCodeFormatterConstants.TRUE : DefaultCodeFormatterConstants.FALSE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INDENT_BODY_DECLARATIONS_COMPARE_TO_RECORD_HEADER, this.indent_body_declarations_compare_to_record_header ? DefaultCodeFormatterConstants.TRUE : DefaultCodeFormatterConstants.FALSE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INDENT_BODY_DECLARATIONS_COMPARE_TO_TYPE_HEADER, this.indent_body_declarations_compare_to_type_header ? DefaultCodeFormatterConstants.TRUE : DefaultCodeFormatterConstants.FALSE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INDENT_BREAKS_COMPARE_TO_CASES, this.indent_breaks_compare_to_cases ? DefaultCodeFormatterConstants.TRUE : DefaultCodeFormatterConstants.FALSE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INDENT_EMPTY_LINES, this.indent_empty_lines ? DefaultCodeFormatterConstants.TRUE : DefaultCodeFormatterConstants.FALSE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INDENT_SWITCHSTATEMENTS_COMPARE_TO_CASES, this.indent_switchstatements_compare_to_cases ? DefaultCodeFormatterConstants.TRUE : DefaultCodeFormatterConstants.FALSE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INDENT_SWITCHSTATEMENTS_COMPARE_TO_SWITCH, this.indent_switchstatements_compare_to_switch ? DefaultCodeFormatterConstants.TRUE : DefaultCodeFormatterConstants.FALSE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INDENTATION_SIZE, Integer.toString(this.tab_char == MIXED ? this.indentation_size : this.tab_size)); // reverse values swapping performed by IndentationTabPage
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION_ON_TYPE, this.insert_new_line_after_annotation_on_type ? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_TYPE_ANNOTATION, this.insert_new_line_after_type_annotation ? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION_ON_ENUM_CONSTANT, this.insert_new_line_after_annotation_on_enum_constant ? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION_ON_FIELD, this.insert_new_line_after_annotation_on_field ? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION_ON_METHOD, this.insert_new_line_after_annotation_on_method ? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION_ON_PACKAGE, this.insert_new_line_after_annotation_on_package ? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION_ON_PARAMETER, this.insert_new_line_after_annotation_on_parameter ? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION_ON_LOCAL_VARIABLE, this.insert_new_line_after_annotation_on_local_variable ? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_OPENING_BRACE_IN_ARRAY_INITIALIZER, this.insert_new_line_after_opening_brace_in_array_initializer? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AT_END_OF_FILE_IF_MISSING, this.insert_new_line_at_end_of_file_if_missing ? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_BEFORE_CATCH_IN_TRY_STATEMENT, this.insert_new_line_before_catch_in_try_statement? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_BEFORE_CLOSING_BRACE_IN_ARRAY_INITIALIZER, this.insert_new_line_before_closing_brace_in_array_initializer? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_BEFORE_ELSE_IN_IF_STATEMENT, this.insert_new_line_before_else_in_if_statement? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_BEFORE_FINALLY_IN_TRY_STATEMENT, this.insert_new_line_before_finally_in_try_statement? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_BEFORE_WHILE_IN_DO_STATEMENT, this.insert_new_line_before_while_in_do_statement? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_KEEP_ANNOTATION_DECLARATION_ON_ONE_LINE, this.keep_annotation_declaration_on_one_line);
		options.put(DefaultCodeFormatterConstants.FORMATTER_KEEP_ANONYMOUS_TYPE_DECLARATION_ON_ONE_LINE, this.keep_anonymous_type_declaration_on_one_line);
		options.put(DefaultCodeFormatterConstants.FORMATTER_KEEP_IF_THEN_BODY_BLOCK_ON_ONE_LINE, this.keep_if_then_body_block_on_one_line);
		options.put(DefaultCodeFormatterConstants.FORMATTER_KEEP_SWITCH_BODY_BLOCK_ON_ONE_LINE, this.keep_switch_body_block_on_one_line);
		options.put(DefaultCodeFormatterConstants.FORMATTER_KEEP_SWITCH_CASE_WITH_ARROW_ON_ONE_LINE, this.keep_switch_case_with_arrow_on_one_line);
		options.put(DefaultCodeFormatterConstants.FORMATTER_KEEP_LAMBDA_BODY_BLOCK_ON_ONE_LINE, this.keep_lambda_body_block_on_one_line);
		options.put(DefaultCodeFormatterConstants.FORMATTER_KEEP_LOOP_BODY_BLOCK_ON_ONE_LINE, this.keep_loop_body_block_on_one_line);
		options.put(DefaultCodeFormatterConstants.FORMATTER_KEEP_CODE_BLOCK_ON_ONE_LINE, this.keep_code_block_on_one_line);
		options.put(DefaultCodeFormatterConstants.FORMATTER_KEEP_ENUM_CONSTANT_DECLARATION_ON_ONE_LINE, this.keep_enum_constant_declaration_on_one_line);
		options.put(DefaultCodeFormatterConstants.FORMATTER_KEEP_ENUM_DECLARATION_ON_ONE_LINE, this.keep_enum_declaration_on_one_line);
		options.put(DefaultCodeFormatterConstants.FORMATTER_KEEP_METHOD_BODY_ON_ONE_LINE, this.keep_method_body_on_one_line);
		options.put(DefaultCodeFormatterConstants.FORMATTER_KEEP_TYPE_DECLARATION_ON_ONE_LINE, this.keep_type_declaration_on_one_line);
		options.put(DefaultCodeFormatterConstants.FORMATTER_KEEP_RECORD_DECLARATION_ON_ONE_LINE, this.keep_record_declaration_on_one_line);
		options.put(DefaultCodeFormatterConstants.FORMATTER_KEEP_RECORD_CONSTRUCTOR_ON_ONE_LINE, this.keep_record_constructor_on_one_line);
		options.put(DefaultCodeFormatterConstants.FORMATTER_KEEP_SIMPLE_GETTER_SETTER_ON_ONE_LINE, this.keep_simple_getter_setter_on_one_line? DefaultCodeFormatterConstants.TRUE : DefaultCodeFormatterConstants.FALSE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_LABEL, this.insert_new_line_after_label? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_AND_IN_TYPE_PARAMETER, this.insert_space_after_and_in_type_parameter? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_ARROW_IN_SWITCH_CASE, this.insert_space_after_arrow_in_switch_case? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_ARROW_IN_SWITCH_DEFAULT, this.insert_space_after_arrow_in_switch_default? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_ASSIGNMENT_OPERATOR, this.insert_space_after_assignment_operator? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_AT_IN_ANNOTATION, this.insert_space_after_at_in_annotation? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_AT_IN_ANNOTATION_TYPE_DECLARATION, this.insert_space_after_at_in_annotation_type_declaration ? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_MULTIPLICATIVE_OPERATOR, this.insert_space_after_multiplicative_operator ? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_ADDITIVE_OPERATOR, this.insert_space_after_additive_operator ? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_STRING_CONCATENATION, this.insert_space_after_string_concatenation ? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_SHIFT_OPERATOR, this.insert_space_after_shift_operator ? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_RELATIONAL_OPERATOR, this.insert_space_after_relational_operator ? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_BITWISE_OPERATOR, this.insert_space_after_bitwise_operator ? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_LOGICAL_OPERATOR, this.insert_space_after_logical_operator ? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_CLOSING_ANGLE_BRACKET_IN_TYPE_ARGUMENTS, this.insert_space_after_closing_angle_bracket_in_type_arguments ? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_CLOSING_ANGLE_BRACKET_IN_TYPE_PARAMETERS, this.insert_space_after_closing_angle_bracket_in_type_parameters ? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_CLOSING_PAREN_IN_CAST, this.insert_space_after_closing_paren_in_cast? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_CLOSING_BRACE_IN_BLOCK, this.insert_space_after_closing_brace_in_block? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COLON_IN_ASSERT, this.insert_space_after_colon_in_assert ? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COLON_IN_CASE, this.insert_space_after_colon_in_case ? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COLON_IN_CONDITIONAL, this.insert_space_after_colon_in_conditional ? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COLON_IN_FOR, this.insert_space_after_colon_in_for ? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COLON_IN_LABELED_STATEMENT, this.insert_space_after_colon_in_labeled_statement? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_ALLOCATION_EXPRESSION, this.insert_space_after_comma_in_allocation_expression? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_ANNOTATION, this.insert_space_after_comma_in_annotation? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_ARRAY_INITIALIZER, this.insert_space_after_comma_in_array_initializer? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_CONSTRUCTOR_DECLARATION_PARAMETERS, this.insert_space_after_comma_in_constructor_declaration_parameters? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_CONSTRUCTOR_DECLARATION_THROWS, this.insert_space_after_comma_in_constructor_declaration_throws? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_ENUM_CONSTANT_ARGUMENTS, this.insert_space_after_comma_in_enum_constant_arguments ? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_ENUM_DECLARATIONS, this.insert_space_after_comma_in_enum_declarations ? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_EXPLICIT_CONSTRUCTOR_CALL_ARGUMENTS, this.insert_space_after_comma_in_explicit_constructor_call_arguments? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_FOR_INCREMENTS, this.insert_space_after_comma_in_for_increments? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_FOR_INITS, this.insert_space_after_comma_in_for_inits? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_METHOD_INVOCATION_ARGUMENTS, this.insert_space_after_comma_in_method_invocation_arguments? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_METHOD_DECLARATION_PARAMETERS, this.insert_space_after_comma_in_method_declaration_parameters? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_METHOD_DECLARATION_THROWS, this.insert_space_after_comma_in_method_declaration_throws? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_MULTIPLE_FIELD_DECLARATIONS, this.insert_space_after_comma_in_multiple_field_declarations? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_MULTIPLE_LOCAL_DECLARATIONS, this.insert_space_after_comma_in_multiple_local_declarations? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_PARAMETERIZED_TYPE_REFERENCE, this.insert_space_after_comma_in_parameterized_type_reference? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_PERMITTED_TYPES, this.insert_space_after_comma_in_permitted_types? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_RECORD_COMPONENTS, this.insert_space_after_comma_in_record_components? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_SUPERINTERFACES, this.insert_space_after_comma_in_superinterfaces? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_SWITCH_CASE_EXPRESSIONS, this.insert_space_after_comma_in_switch_case_expressions ? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_TYPE_ARGUMENTS, this.insert_space_after_comma_in_type_arguments ? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_TYPE_PARAMETERS, this.insert_space_after_comma_in_type_parameters ? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_BRACKET_IN_ARRAY_ALLOCATION_EXPRESSION, this.insert_space_after_opening_bracket_in_array_allocation_expression? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_ELLIPSIS, this.insert_space_after_ellipsis ? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_LAMBDA_ARROW, this.insert_space_after_lambda_arrow ? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_NOT_OPERATOR, this.insert_space_after_not_operator? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_ANGLE_BRACKET_IN_PARAMETERIZED_TYPE_REFERENCE, this.insert_space_after_opening_angle_bracket_in_parameterized_type_reference? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_ANGLE_BRACKET_IN_TYPE_ARGUMENTS, this.insert_space_after_opening_angle_bracket_in_type_arguments? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_ANGLE_BRACKET_IN_TYPE_PARAMETERS, this.insert_space_after_opening_angle_bracket_in_type_parameters? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_BRACKET_IN_ARRAY_REFERENCE, this.insert_space_after_opening_bracket_in_array_reference? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_BRACE_IN_ARRAY_INITIALIZER, this.insert_space_after_opening_brace_in_array_initializer? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_ANNOTATION, this.insert_space_after_opening_paren_in_annotation? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_CAST, this.insert_space_after_opening_paren_in_cast? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_CATCH, this.insert_space_after_opening_paren_in_catch? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_CONSTRUCTOR_DECLARATION, this.insert_space_after_opening_paren_in_constructor_declaration? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_ENUM_CONSTANT, this.insert_space_after_opening_paren_in_enum_constant? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_FOR, this.insert_space_after_opening_paren_in_for? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_IF, this.insert_space_after_opening_paren_in_if? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_METHOD_DECLARATION, this.insert_space_after_opening_paren_in_method_declaration? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_METHOD_INVOCATION, this.insert_space_after_opening_paren_in_method_invocation? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_PARENTHESIZED_EXPRESSION, this.insert_space_after_opening_paren_in_parenthesized_expression? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_RECORD_DECLARATION, this.insert_space_after_opening_paren_in_record_declaration? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_SWITCH, this.insert_space_after_opening_paren_in_switch? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_SYNCHRONIZED, this.insert_space_after_opening_paren_in_synchronized? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_TRY, this.insert_space_after_opening_paren_in_try? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_WHILE, this.insert_space_after_opening_paren_in_while? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_POSTFIX_OPERATOR, this.insert_space_after_postfix_operator? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_PREFIX_OPERATOR, this.insert_space_after_prefix_operator? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_QUESTION_IN_CONDITIONAL, this.insert_space_after_question_in_conditional? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_QUESTION_IN_WILDCARD, this.insert_space_after_question_in_wilcard? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_SEMICOLON_IN_FOR, this.insert_space_after_semicolon_in_for? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_SEMICOLON_IN_TRY_RESOURCES, this.insert_space_after_semicolon_in_try_resources? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_UNARY_OPERATOR, this.insert_space_after_unary_operator? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_AND_IN_TYPE_PARAMETER, this.insert_space_before_and_in_type_parameter ? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_ARROW_IN_SWITCH_CASE, this.insert_space_before_arrow_in_switch_case ? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_ARROW_IN_SWITCH_DEFAULT, this.insert_space_before_arrow_in_switch_default ? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_AT_IN_ANNOTATION_TYPE_DECLARATION, this.insert_space_before_at_in_annotation_type_declaration ? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_ASSIGNMENT_OPERATOR, this.insert_space_before_assignment_operator? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_MULTIPLICATIVE_OPERATOR, this.insert_space_before_multiplicative_operator ? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_ADDITIVE_OPERATOR, this.insert_space_before_additive_operator ? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_STRING_CONCATENATION, this.insert_space_before_string_concatenation ? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_SHIFT_OPERATOR, this.insert_space_before_shift_operator ? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_RELATIONAL_OPERATOR, this.insert_space_before_relational_operator ? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_BITWISE_OPERATOR, this.insert_space_before_bitwise_operator ? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_LOGICAL_OPERATOR, this.insert_space_before_logical_operator ? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_ANGLE_BRACKET_IN_PARAMETERIZED_TYPE_REFERENCE, this.insert_space_before_closing_angle_bracket_in_parameterized_type_reference? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_ANGLE_BRACKET_IN_TYPE_ARGUMENTS, this.insert_space_before_closing_angle_bracket_in_type_arguments? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_ANGLE_BRACKET_IN_TYPE_PARAMETERS, this.insert_space_before_closing_angle_bracket_in_type_parameters? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_BRACE_IN_ARRAY_INITIALIZER, this.insert_space_before_closing_brace_in_array_initializer? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_BRACKET_IN_ARRAY_ALLOCATION_EXPRESSION, this.insert_space_before_closing_bracket_in_array_allocation_expression? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_BRACKET_IN_ARRAY_REFERENCE, this.insert_space_before_closing_bracket_in_array_reference? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_ANNOTATION, this.insert_space_before_closing_paren_in_annotation? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_CAST, this.insert_space_before_closing_paren_in_cast? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_CATCH, this.insert_space_before_closing_paren_in_catch? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_CONSTRUCTOR_DECLARATION, this.insert_space_before_closing_paren_in_constructor_declaration? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_ENUM_CONSTANT, this.insert_space_before_closing_paren_in_enum_constant? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_FOR, this.insert_space_before_closing_paren_in_for? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_IF, this.insert_space_before_closing_paren_in_if? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_METHOD_DECLARATION, this.insert_space_before_closing_paren_in_method_declaration? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_METHOD_INVOCATION, this.insert_space_before_closing_paren_in_method_invocation? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_PARENTHESIZED_EXPRESSION, this.insert_space_before_closing_paren_in_parenthesized_expression? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_RECORD_DECLARATION, this.insert_space_before_closing_paren_in_record_declaration? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_SWITCH, this.insert_space_before_closing_paren_in_switch? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_SYNCHRONIZED, this.insert_space_before_closing_paren_in_synchronized? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_TRY, this.insert_space_before_closing_paren_in_try? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_WHILE, this.insert_space_before_closing_paren_in_while? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COLON_IN_ASSERT, this.insert_space_before_colon_in_assert? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COLON_IN_CASE, this.insert_space_before_colon_in_case? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COLON_IN_CONDITIONAL, this.insert_space_before_colon_in_conditional? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COLON_IN_DEFAULT, this.insert_space_before_colon_in_default? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COLON_IN_FOR, this.insert_space_before_colon_in_for ? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COLON_IN_LABELED_STATEMENT, this.insert_space_before_colon_in_labeled_statement? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_ALLOCATION_EXPRESSION, this.insert_space_before_comma_in_allocation_expression? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_ANNOTATION, this.insert_space_before_comma_in_annotation? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_ARRAY_INITIALIZER, this.insert_space_before_comma_in_array_initializer? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_CONSTRUCTOR_DECLARATION_PARAMETERS, this.insert_space_before_comma_in_constructor_declaration_parameters? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_CONSTRUCTOR_DECLARATION_THROWS, this.insert_space_before_comma_in_constructor_declaration_throws? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_ENUM_CONSTANT_ARGUMENTS, this.insert_space_before_comma_in_enum_constant_arguments? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_ENUM_DECLARATIONS, this.insert_space_before_comma_in_enum_declarations? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_EXPLICIT_CONSTRUCTOR_CALL_ARGUMENTS, this.insert_space_before_comma_in_explicit_constructor_call_arguments? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_FOR_INCREMENTS, this.insert_space_before_comma_in_for_increments? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_FOR_INITS, this.insert_space_before_comma_in_for_inits? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_METHOD_INVOCATION_ARGUMENTS, this.insert_space_before_comma_in_method_invocation_arguments? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_METHOD_DECLARATION_PARAMETERS, this.insert_space_before_comma_in_method_declaration_parameters? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_METHOD_DECLARATION_THROWS, this.insert_space_before_comma_in_method_declaration_throws? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_MULTIPLE_FIELD_DECLARATIONS, this.insert_space_before_comma_in_multiple_field_declarations? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_MULTIPLE_LOCAL_DECLARATIONS, this.insert_space_before_comma_in_multiple_local_declarations? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_PERMITTED_TYPES, this.insert_space_before_comma_in_permitted_types? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_RECORD_COMPONENTS, this.insert_space_before_comma_in_record_components? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_SUPERINTERFACES, this.insert_space_before_comma_in_superinterfaces? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_SWITCH_CASE_EXPRESSIONS, this.insert_space_before_comma_in_switch_case_expressions? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_TYPE_ARGUMENTS, this.insert_space_before_comma_in_type_arguments ? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_TYPE_PARAMETERS, this.insert_space_before_comma_in_type_parameters? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_PARAMETERIZED_TYPE_REFERENCE, this.insert_space_before_comma_in_parameterized_type_reference? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_ELLIPSIS, this.insert_space_before_ellipsis ? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_LAMBDA_ARROW, this.insert_space_before_lambda_arrow ? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_ANGLE_BRACKET_IN_PARAMETERIZED_TYPE_REFERENCE, this.insert_space_before_opening_angle_bracket_in_parameterized_type_reference? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_ANGLE_BRACKET_IN_TYPE_ARGUMENTS, this.insert_space_before_opening_angle_bracket_in_type_arguments? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_ANGLE_BRACKET_IN_TYPE_PARAMETERS, this.insert_space_before_opening_angle_bracket_in_type_parameters? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_ANNOTATION_TYPE_DECLARATION, this.insert_space_before_opening_brace_in_annotation_type_declaration ? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_ANONYMOUS_TYPE_DECLARATION, this.insert_space_before_opening_brace_in_anonymous_type_declaration? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_ARRAY_INITIALIZER, this.insert_space_before_opening_brace_in_array_initializer? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_BLOCK, this.insert_space_before_opening_brace_in_block? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_CONSTRUCTOR_DECLARATION, this.insert_space_before_opening_brace_in_constructor_declaration? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_ENUM_CONSTANT, this.insert_space_before_opening_brace_in_enum_constant? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_ENUM_DECLARATION, this.insert_space_before_opening_brace_in_enum_declaration? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_METHOD_DECLARATION, this.insert_space_before_opening_brace_in_method_declaration? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_RECORD_CONSTRUCTOR, this.insert_space_before_opening_brace_in_record_constructor? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_RECORD_DECLARATION, this.insert_space_before_opening_brace_in_record_declaration? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_TYPE_DECLARATION, this.insert_space_before_opening_brace_in_type_declaration? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACKET_IN_ARRAY_ALLOCATION_EXPRESSION, this.insert_space_before_opening_bracket_in_array_allocation_expression ? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACKET_IN_ARRAY_REFERENCE, this.insert_space_before_opening_bracket_in_array_reference? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACKET_IN_ARRAY_TYPE_REFERENCE, this.insert_space_before_opening_bracket_in_array_type_reference? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_ANNOTATION, this.insert_space_before_opening_paren_in_annotation? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_ANNOTATION_TYPE_MEMBER_DECLARATION, this.insert_space_before_opening_paren_in_annotation_type_member_declaration ? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_CATCH, this.insert_space_before_opening_paren_in_catch? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_CONSTRUCTOR_DECLARATION, this.insert_space_before_opening_paren_in_constructor_declaration? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_ENUM_CONSTANT, this.insert_space_before_opening_paren_in_enum_constant? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_FOR, this.insert_space_before_opening_paren_in_for? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_IF, this.insert_space_before_opening_paren_in_if? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_METHOD_INVOCATION, this.insert_space_before_opening_paren_in_method_invocation? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_METHOD_DECLARATION, this.insert_space_before_opening_paren_in_method_declaration? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_RECORD_DECLARATION, this.insert_space_before_opening_paren_in_record_declaration? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_SWITCH, this.insert_space_before_opening_paren_in_switch? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_SWITCH, this.insert_space_before_opening_brace_in_switch? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_SYNCHRONIZED, this.insert_space_before_opening_paren_in_synchronized? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_TRY, this.insert_space_before_opening_paren_in_try? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_PARENTHESIZED_EXPRESSION, this.insert_space_before_opening_paren_in_parenthesized_expression? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_WHILE, this.insert_space_before_opening_paren_in_while? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_PARENTHESIZED_EXPRESSION_IN_RETURN, this.insert_space_before_parenthesized_expression_in_return ? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_PARENTHESIZED_EXPRESSION_IN_THROW, this.insert_space_before_parenthesized_expression_in_throw ? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_POSTFIX_OPERATOR, this.insert_space_before_postfix_operator? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_PREFIX_OPERATOR, this.insert_space_before_prefix_operator? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_QUESTION_IN_CONDITIONAL, this.insert_space_before_question_in_conditional? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_QUESTION_IN_WILDCARD, this.insert_space_before_question_in_wilcard? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_SEMICOLON, this.insert_space_before_semicolon? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_SEMICOLON_IN_FOR, this.insert_space_before_semicolon_in_for? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_SEMICOLON_IN_TRY_RESOURCES, this.insert_space_before_semicolon_in_try_resources? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_UNARY_OPERATOR, this.insert_space_before_unary_operator? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_BRACKETS_IN_ARRAY_TYPE_REFERENCE, this.insert_space_between_brackets_in_array_type_reference? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_BRACES_IN_ARRAY_INITIALIZER, this.insert_space_between_empty_braces_in_array_initializer? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_BRACKETS_IN_ARRAY_ALLOCATION_EXPRESSION, this.insert_space_between_empty_brackets_in_array_allocation_expression? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_PARENS_IN_ANNOTATION_TYPE_MEMBER_DECLARATION, this.insert_space_between_empty_parens_in_annotation_type_member_declaration? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_PARENS_IN_CONSTRUCTOR_DECLARATION, this.insert_space_between_empty_parens_in_constructor_declaration? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_PARENS_IN_ENUM_CONSTANT, this.insert_space_between_empty_parens_in_enum_constant? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_PARENS_IN_METHOD_DECLARATION, this.insert_space_between_empty_parens_in_method_declaration? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_PARENS_IN_METHOD_INVOCATION, this.insert_space_between_empty_parens_in_method_invocation? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_COMPACT_ELSE_IF, this.compact_else_if ? DefaultCodeFormatterConstants.TRUE : DefaultCodeFormatterConstants.FALSE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_KEEP_GUARDIAN_CLAUSE_ON_ONE_LINE, this.keep_guardian_clause_on_one_line ? DefaultCodeFormatterConstants.TRUE : DefaultCodeFormatterConstants.FALSE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_KEEP_ELSE_STATEMENT_ON_SAME_LINE, this.keep_else_statement_on_same_line ? DefaultCodeFormatterConstants.TRUE : DefaultCodeFormatterConstants.FALSE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_KEEP_EMPTY_ARRAY_INITIALIZER_ON_ONE_LINE, this.keep_empty_array_initializer_on_one_line ? DefaultCodeFormatterConstants.TRUE : DefaultCodeFormatterConstants.FALSE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_KEEP_SIMPLE_IF_ON_ONE_LINE, this.keep_simple_if_on_one_line ? DefaultCodeFormatterConstants.TRUE : DefaultCodeFormatterConstants.FALSE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_KEEP_THEN_STATEMENT_ON_SAME_LINE, this.keep_then_statement_on_same_line ? DefaultCodeFormatterConstants.TRUE : DefaultCodeFormatterConstants.FALSE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_KEEP_SIMPLE_FOR_BODY_ON_SAME_LINE, this.keep_simple_for_body_on_same_line ? DefaultCodeFormatterConstants.TRUE : DefaultCodeFormatterConstants.FALSE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_KEEP_SIMPLE_WHILE_BODY_ON_SAME_LINE, this.keep_simple_while_body_on_same_line ? DefaultCodeFormatterConstants.TRUE : DefaultCodeFormatterConstants.FALSE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_KEEP_SIMPLE_DO_WHILE_BODY_ON_SAME_LINE, this.keep_simple_do_while_body_on_same_line ? DefaultCodeFormatterConstants.TRUE : DefaultCodeFormatterConstants.FALSE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_NEVER_INDENT_BLOCK_COMMENTS_ON_FIRST_COLUMN, this.never_indent_block_comments_on_first_column ? DefaultCodeFormatterConstants.TRUE : DefaultCodeFormatterConstants.FALSE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_NEVER_INDENT_LINE_COMMENTS_ON_FIRST_COLUMN, this.never_indent_line_comments_on_first_column ? DefaultCodeFormatterConstants.TRUE : DefaultCodeFormatterConstants.FALSE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_NUMBER_OF_EMPTY_LINES_TO_PRESERVE, Integer.toString(this.number_of_empty_lines_to_preserve));
		options.put(DefaultCodeFormatterConstants.FORMATTER_JOIN_WRAPPED_LINES, this.join_wrapped_lines ? DefaultCodeFormatterConstants.TRUE : DefaultCodeFormatterConstants.FALSE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_JOIN_LINES_IN_COMMENTS, this.join_lines_in_comments ? DefaultCodeFormatterConstants.TRUE : DefaultCodeFormatterConstants.FALSE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_PUT_EMPTY_STATEMENT_ON_NEW_LINE, this.put_empty_statement_on_new_line ? DefaultCodeFormatterConstants.TRUE : DefaultCodeFormatterConstants.FALSE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_LINE_SPLIT, Integer.toString(this.page_width));
		switch(this.tab_char) {
			case SPACE :
				options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
				break;
			case TAB :
				options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.TAB);
				break;
			case MIXED :
				options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, DefaultCodeFormatterConstants.MIXED);
				break;
		}
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, Integer.toString(this.tab_char == SPACE ? this.indentation_size : this.tab_size)); // reverse values swapping performed by IndentationTabPage
		options.put(DefaultCodeFormatterConstants.FORMATTER_USE_TABS_ONLY_FOR_LEADING_INDENTATIONS, this.use_tabs_only_for_leading_indentations ?  DefaultCodeFormatterConstants.TRUE : DefaultCodeFormatterConstants.FALSE);

		int textBlockIndentation;
		switch (this.text_block_indentation) {
			case Alignment.M_INDENT_PRESERVE:
				textBlockIndentation = DefaultCodeFormatterConstants.INDENT_PRESERVE;
				break;
			case Alignment.M_INDENT_BY_ONE:
				textBlockIndentation = DefaultCodeFormatterConstants.INDENT_BY_ONE;
				break;
			case Alignment.M_INDENT_DEFAULT:
				textBlockIndentation = DefaultCodeFormatterConstants.INDENT_DEFAULT;
				break;
			case Alignment.M_INDENT_ON_COLUMN:
				textBlockIndentation = DefaultCodeFormatterConstants.INDENT_ON_COLUMN;
				break;
			default:
				throw new IllegalArgumentException("Invalid text block indentation: " + this.text_block_indentation); //$NON-NLS-1$
		}
		options.put(DefaultCodeFormatterConstants.FORMATTER_TEXT_BLOCK_INDENTATION, Integer.toString(textBlockIndentation));

		options.put(DefaultCodeFormatterConstants.FORMATTER_WRAP_BEFORE_MULTIPLICATIVE_OPERATOR, this.wrap_before_multiplicative_operator ? DefaultCodeFormatterConstants.TRUE : DefaultCodeFormatterConstants.FALSE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_WRAP_BEFORE_ADDITIVE_OPERATOR, this.wrap_before_additive_operator ? DefaultCodeFormatterConstants.TRUE : DefaultCodeFormatterConstants.FALSE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_WRAP_BEFORE_ASSERTION_MESSAGE_OPERATOR, this.wrap_before_assertion_message_operator ? DefaultCodeFormatterConstants.TRUE : DefaultCodeFormatterConstants.FALSE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_WRAP_BEFORE_STRING_CONCATENATION, this.wrap_before_string_concatenation ? DefaultCodeFormatterConstants.TRUE : DefaultCodeFormatterConstants.FALSE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_WRAP_BEFORE_SHIFT_OPERATOR, this.wrap_before_shift_operator ? DefaultCodeFormatterConstants.TRUE : DefaultCodeFormatterConstants.FALSE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_WRAP_BEFORE_RELATIONAL_OPERATOR, this.wrap_before_relational_operator ? DefaultCodeFormatterConstants.TRUE : DefaultCodeFormatterConstants.FALSE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_WRAP_BEFORE_BITWISE_OPERATOR, this.wrap_before_bitwise_operator ? DefaultCodeFormatterConstants.TRUE : DefaultCodeFormatterConstants.FALSE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_WRAP_BEFORE_LOGICAL_OPERATOR, this.wrap_before_logical_operator ? DefaultCodeFormatterConstants.TRUE : DefaultCodeFormatterConstants.FALSE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_WRAP_BEFORE_OR_OPERATOR_MULTICATCH, this.wrap_before_or_operator_multicatch ? DefaultCodeFormatterConstants.TRUE : DefaultCodeFormatterConstants.FALSE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_WRAP_BEFORE_CONDITIONAL_OPERATOR, this.wrap_before_conditional_operator ? DefaultCodeFormatterConstants.TRUE : DefaultCodeFormatterConstants.FALSE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_WRAP_BEFORE_ASSIGNMENT_OPERATOR, this.wrap_before_assignment_operator ? DefaultCodeFormatterConstants.TRUE : DefaultCodeFormatterConstants.FALSE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_WRAP_BEFORE_SWITCH_CASE_ARROW_OPERATOR, this.wrap_before_switch_case_arrow_operator ? DefaultCodeFormatterConstants.TRUE : DefaultCodeFormatterConstants.FALSE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_DISABLING_TAG, this.disabling_tag == null ? Util.EMPTY_STRING : new String(this.disabling_tag));
		options.put(DefaultCodeFormatterConstants.FORMATTER_ENABLING_TAG, this.enabling_tag == null ? Util.EMPTY_STRING : new String(this.enabling_tag));
		options.put(DefaultCodeFormatterConstants.FORMATTER_USE_ON_OFF_TAGS, this.use_tags ? DefaultCodeFormatterConstants.TRUE : DefaultCodeFormatterConstants.FALSE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_WRAP_OUTER_EXPRESSIONS_WHEN_NESTED, this.wrap_outer_expressions_when_nested ? DefaultCodeFormatterConstants.TRUE : DefaultCodeFormatterConstants.FALSE);
		return options;
	}

	public void set(Map<String, String> settings) {
		setInt(settings, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ANNOTATIONS_ON_TYPE,
				v -> this.alignment_for_annotations_on_type = v);
		setInt(settings, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_TYPE_ANNOTATIONS,
				v -> this.alignment_for_type_annotations = v);
		setInt(settings, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ANNOTATIONS_ON_ENUM_CONSTANT,
				v -> this.alignment_for_annotations_on_enum_constant = v);
		setInt(settings, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ANNOTATIONS_ON_FIELD,
				v -> this.alignment_for_annotations_on_field = v);
		setInt(settings, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ANNOTATIONS_ON_METHOD,
				v -> this.alignment_for_annotations_on_method = v);
		setInt(settings, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ANNOTATIONS_ON_PACKAGE,
				v -> this.alignment_for_annotations_on_package = v);
		setInt(settings, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ANNOTATIONS_ON_PARAMETER,
				v -> this.alignment_for_annotations_on_parameter = v);
		setInt(settings, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ANNOTATIONS_ON_LOCAL_VARIABLE,
				v -> this.alignment_for_annotations_on_local_variable = v);
		final Object alignmentForArgumentsInAllocationExpressionOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ARGUMENTS_IN_ALLOCATION_EXPRESSION);
		if (alignmentForArgumentsInAllocationExpressionOption != null) {
			try {
				this.alignment_for_arguments_in_allocation_expression = Integer.parseInt((String) alignmentForArgumentsInAllocationExpressionOption);
			} catch (NumberFormatException | ClassCastException e) {
				this.alignment_for_arguments_in_allocation_expression = Alignment.M_COMPACT_SPLIT;
			}
		}
		final Object alignmentForArgumentsInAnnotationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ARGUMENTS_IN_ANNOTATION);
		if (alignmentForArgumentsInAnnotationOption != null) {
			try {
				this.alignment_for_arguments_in_annotation = Integer.parseInt((String) alignmentForArgumentsInAnnotationOption);
			} catch (NumberFormatException | ClassCastException e) {
				this.alignment_for_arguments_in_annotation = Alignment.M_NO_ALIGNMENT;
			}
		}
		final Object alignmentForArgumentsInEnumConstantOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ARGUMENTS_IN_ENUM_CONSTANT);
		if (alignmentForArgumentsInEnumConstantOption != null) {
			try {
				this.alignment_for_arguments_in_enum_constant = Integer.parseInt((String) alignmentForArgumentsInEnumConstantOption);
			} catch (NumberFormatException | ClassCastException e) {
				this.alignment_for_arguments_in_enum_constant = Alignment.M_COMPACT_SPLIT;
			}
		}
		final Object alignmentForArgumentsInExplicitConstructorCallOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ARGUMENTS_IN_EXPLICIT_CONSTRUCTOR_CALL);
		if (alignmentForArgumentsInExplicitConstructorCallOption != null) {
			try {
				this.alignment_for_arguments_in_explicit_constructor_call = Integer.parseInt((String) alignmentForArgumentsInExplicitConstructorCallOption);
			} catch (NumberFormatException | ClassCastException e) {
				this.alignment_for_arguments_in_explicit_constructor_call = Alignment.M_COMPACT_SPLIT;
			}
		}
		final Object alignmentForArgumentsInMethodInvocationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ARGUMENTS_IN_METHOD_INVOCATION);
		if (alignmentForArgumentsInMethodInvocationOption != null) {
			try {
				this.alignment_for_arguments_in_method_invocation = Integer.parseInt((String) alignmentForArgumentsInMethodInvocationOption);
			} catch (NumberFormatException | ClassCastException e) {
				this.alignment_for_arguments_in_method_invocation = Alignment.M_COMPACT_SPLIT;
			}
		}
		final Object alignmentForArgumentsInQualifiedAllocationExpressionOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ARGUMENTS_IN_QUALIFIED_ALLOCATION_EXPRESSION);
		if (alignmentForArgumentsInQualifiedAllocationExpressionOption != null) {
			try {
				this.alignment_for_arguments_in_qualified_allocation_expression = Integer.parseInt((String) alignmentForArgumentsInQualifiedAllocationExpressionOption);
			} catch (NumberFormatException | ClassCastException e) {
				this.alignment_for_arguments_in_qualified_allocation_expression = Alignment.M_COMPACT_SPLIT;
			}
		}
		setInt(settings, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ASSERTION_MESSAGE,
				v -> this.alignment_for_assertion_message = v);
		final Object alignmentForAssignmentOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ASSIGNMENT);
		if (alignmentForAssignmentOption != null) {
			try {
				this.alignment_for_assignment = Integer.parseInt((String) alignmentForAssignmentOption);
			} catch (NumberFormatException | ClassCastException e) {
				this.alignment_for_assignment =  Alignment.M_ONE_PER_LINE_SPLIT;
			}
		}
		setInt(settings, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_MULTIPLICATIVE_OPERATOR,
				v-> this.alignment_for_multiplicative_operator = v);
		setInt(settings, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ADDITIVE_OPERATOR,
				v -> this.alignment_for_additive_operator = v);
		setInt(settings, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_STRING_CONCATENATION,
				v -> this.alignment_for_string_concatenation = v);
		setInt(settings, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_SHIFT_OPERATOR,
				v -> this.alignment_for_shift_operator = v);
		setInt(settings, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_RELATIONAL_OPERATOR,
				v -> this.alignment_for_relational_operator = v);
		setInt(settings, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_BITWISE_OPERATOR,
				v -> this.alignment_for_bitwise_operator = v);
		setInt(settings, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_LOGICAL_OPERATOR,
				v -> this.alignment_for_logical_operator = v);
		final Object alignmentForCompactIfOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_COMPACT_IF);
		if (alignmentForCompactIfOption != null) {
			try {
				this.alignment_for_compact_if = Integer.parseInt((String) alignmentForCompactIfOption);
			} catch (NumberFormatException | ClassCastException e) {
				this.alignment_for_compact_if = Alignment.M_ONE_PER_LINE_SPLIT | Alignment.M_INDENT_BY_ONE;
			}
		}
		final Object alignmentForCompactLoopOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_COMPACT_LOOP);
		if (alignmentForCompactLoopOption != null)
			this.alignment_for_compact_loop = toInt(alignmentForCompactLoopOption, Alignment.M_ONE_PER_LINE_SPLIT | Alignment.M_INDENT_BY_ONE);

		final Object alignmentForConditionalExpressionOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_CONDITIONAL_EXPRESSION);
		if (alignmentForConditionalExpressionOption != null) {
			try {
				this.alignment_for_conditional_expression = Integer.parseInt((String) alignmentForConditionalExpressionOption);
			} catch (NumberFormatException | ClassCastException e) {
				this.alignment_for_conditional_expression = Alignment.M_ONE_PER_LINE_SPLIT;
			}
		}
		setInt(settings, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_CONDITIONAL_EXPRESSION_CHAIN,
				v -> this.alignment_for_conditional_expression_chain = v);
		final Object alignmentForEnumConstantsOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ENUM_CONSTANTS);
		if (alignmentForEnumConstantsOption != null) {
			try {
				this.alignment_for_enum_constants = Integer.parseInt((String) alignmentForEnumConstantsOption);
			} catch (NumberFormatException | ClassCastException e) {
				this.alignment_for_enum_constants = Alignment.M_NO_ALIGNMENT;
			}
		}
		final Object alignmentForExpressionsInArrayInitializerOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_EXPRESSIONS_IN_ARRAY_INITIALIZER);
		if (alignmentForExpressionsInArrayInitializerOption != null) {
			try {
				this.alignment_for_expressions_in_array_initializer = Integer.parseInt((String) alignmentForExpressionsInArrayInitializerOption);
			} catch (NumberFormatException | ClassCastException e) {
				this.alignment_for_expressions_in_array_initializer = Alignment.M_COMPACT_SPLIT;
			}
		}
		final Object alignmentForExpressionsInForLoopOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_EXPRESSIONS_IN_FOR_LOOP_HEADER);
		if (alignmentForExpressionsInForLoopOption != null)
			this.alignment_for_expressions_in_for_loop_header = toInt(alignmentForExpressionsInForLoopOption, Alignment.M_NO_ALIGNMENT);
		setInt(settings, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_EXPRESSIONS_IN_SWITCH_CASE_WITH_ARROW,
				v -> this.alignment_for_expressions_in_switch_case_with_arrow = v);
		setInt(settings, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_EXPRESSIONS_IN_SWITCH_CASE_WITH_COLON,
				v -> this.alignment_for_expressions_in_switch_case_with_colon = v);

		final Object alignmentForMethodDeclarationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_METHOD_DECLARATION);
		if (alignmentForMethodDeclarationOption != null) {
			try {
				this.alignment_for_method_declaration = Integer.parseInt((String) alignmentForMethodDeclarationOption);
			} catch(NumberFormatException | ClassCastException e) {
				this.alignment_for_method_declaration = Alignment.M_COMPACT_SPLIT;
			}
		}
		final Object alignmentForModuleStatementsOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_MODULE_STATEMENTS);
		if (alignmentForModuleStatementsOption != null)
			this.alignment_for_module_statements = toInt(alignmentForModuleStatementsOption, Alignment.M_COMPACT_SPLIT);

		final Object alignmentForMultipleFieldsOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_MULTIPLE_FIELDS);
		if (alignmentForMultipleFieldsOption != null) {
			try {
				this.alignment_for_multiple_fields = Integer.parseInt((String) alignmentForMultipleFieldsOption);
			} catch (NumberFormatException | ClassCastException e) {
				this.alignment_for_multiple_fields = Alignment.M_COMPACT_SPLIT;
			}
		}
		final Object alignmentForParameterizeddTypeReferencesOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_PARAMETERIZED_TYPE_REFERENCES);
		if (alignmentForParameterizeddTypeReferencesOption != null) {
			this.alignment_for_parameterized_type_references = toInt(alignmentForParameterizeddTypeReferencesOption, Alignment.M_NO_ALIGNMENT);
		}
		final Object alignmentForParametersInConstructorDeclarationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_PARAMETERS_IN_CONSTRUCTOR_DECLARATION);
		if (alignmentForParametersInConstructorDeclarationOption != null) {
			try {
				this.alignment_for_parameters_in_constructor_declaration = Integer.parseInt((String) alignmentForParametersInConstructorDeclarationOption);
			} catch (NumberFormatException | ClassCastException e) {
				this.alignment_for_parameters_in_constructor_declaration = Alignment.M_COMPACT_SPLIT;
			}
		}
		final Object alignmentForParametersInMethodDeclarationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_PARAMETERS_IN_METHOD_DECLARATION);
		if (alignmentForParametersInMethodDeclarationOption != null) {
			try {
				this.alignment_for_parameters_in_method_declaration = Integer.parseInt((String) alignmentForParametersInMethodDeclarationOption);
			} catch(NumberFormatException | ClassCastException e) {
				this.alignment_for_parameters_in_method_declaration = Alignment.M_COMPACT_SPLIT;
			}
		}
		setInt(settings, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_RECORD_COMPONENTS,
				v -> this.alignment_for_record_components = v);
		final Object alignmentForResourcesInTry = settings.get(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_RESOURCES_IN_TRY);
		if (alignmentForResourcesInTry != null) {
			try {
				this.alignment_for_resources_in_try = Integer.parseInt((String) alignmentForResourcesInTry);
			} catch(NumberFormatException | ClassCastException e) {
				this.alignment_for_resources_in_try = Alignment.M_NEXT_PER_LINE_SPLIT;
			}
		}
		final Object alignmentForSelectorInMethodInvocationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_SELECTOR_IN_METHOD_INVOCATION);
		if (alignmentForSelectorInMethodInvocationOption != null) {
			try {
				this.alignment_for_selector_in_method_invocation = Integer.parseInt((String) alignmentForSelectorInMethodInvocationOption);
			} catch(NumberFormatException | ClassCastException e) {
				this.alignment_for_selector_in_method_invocation = Alignment.M_COMPACT_SPLIT;
			}
		}
		final Object alignmentForSuperclassInTypeDeclarationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_SUPERCLASS_IN_TYPE_DECLARATION);
		if (alignmentForSuperclassInTypeDeclarationOption != null) {
			try {
				this.alignment_for_superclass_in_type_declaration = Integer.parseInt((String) alignmentForSuperclassInTypeDeclarationOption);
			} catch(NumberFormatException | ClassCastException e) {
				this.alignment_for_superclass_in_type_declaration = Alignment.M_NEXT_SHIFTED_SPLIT;
			}
		}
		final Object alignmentForSuperinterfacesInEnumDeclarationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_SUPERINTERFACES_IN_ENUM_DECLARATION);
		if (alignmentForSuperinterfacesInEnumDeclarationOption != null) {
			try {
				this.alignment_for_superinterfaces_in_enum_declaration = Integer.parseInt((String) alignmentForSuperinterfacesInEnumDeclarationOption);
			} catch(NumberFormatException | ClassCastException e) {
				this.alignment_for_superinterfaces_in_enum_declaration = Alignment.M_NEXT_SHIFTED_SPLIT;
			}
		}
		setInt(settings, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_SUPERINTERFACES_IN_RECORD_DECLARATION,
				v -> this.alignment_for_superinterfaces_in_record_declaration = v);
		final Object alignmentForSuperinterfacesInTypeDeclarationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_SUPERINTERFACES_IN_TYPE_DECLARATION);
		if (alignmentForSuperinterfacesInTypeDeclarationOption != null) {
			try {
				this.alignment_for_superinterfaces_in_type_declaration = Integer.parseInt((String) alignmentForSuperinterfacesInTypeDeclarationOption);
			} catch(NumberFormatException | ClassCastException e) {
				this.alignment_for_superinterfaces_in_type_declaration = Alignment.M_NEXT_SHIFTED_SPLIT;
			}
		}
		setInt(settings, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_SWITCH_CASE_WITH_ARROW,
				v -> this.alignment_for_switch_case_with_arrow = v);
		final Object alignmentForThrowsClauseInConstructorDeclarationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_THROWS_CLAUSE_IN_CONSTRUCTOR_DECLARATION);
		if (alignmentForThrowsClauseInConstructorDeclarationOption != null) {
			try {
				this.alignment_for_throws_clause_in_constructor_declaration = Integer.parseInt((String) alignmentForThrowsClauseInConstructorDeclarationOption);
			} catch(NumberFormatException | ClassCastException e) {
				this.alignment_for_throws_clause_in_constructor_declaration = Alignment.M_COMPACT_SPLIT;
			}
		}
		final Object alignmentForThrowsClauseInMethodDeclarationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_THROWS_CLAUSE_IN_METHOD_DECLARATION);
		if (alignmentForThrowsClauseInMethodDeclarationOption != null) {
			try {
				this.alignment_for_throws_clause_in_method_declaration = Integer.parseInt((String) alignmentForThrowsClauseInMethodDeclarationOption);
			} catch(NumberFormatException | ClassCastException e) {
				this.alignment_for_throws_clause_in_method_declaration = Alignment.M_COMPACT_SPLIT;
			}
		}
		final Object alignmentForTypeArguments = settings.get(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_TYPE_ARGUMENTS);
		if (alignmentForTypeArguments != null) {
			this.alignment_for_type_arguments = toInt(alignmentForTypeArguments, Alignment.M_NO_ALIGNMENT);
		}
		final Object alignmentForTypeParameters = settings.get(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_TYPE_PARAMETERS);
		if (alignmentForTypeParameters != null) {
			this.alignment_for_type_parameters = toInt(alignmentForTypeParameters, Alignment.M_NO_ALIGNMENT);
		}
		final Object alignmentForUnionTypeInMulticatch = settings.get(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_UNION_TYPE_IN_MULTICATCH);
		if (alignmentForUnionTypeInMulticatch != null) {
			try {
				this.alignment_for_union_type_in_multicatch = Integer.parseInt((String) alignmentForUnionTypeInMulticatch);
			} catch(NumberFormatException | ClassCastException e) {
				this.alignment_for_union_type_in_multicatch = Alignment.M_COMPACT_SPLIT;
			}
		}
		setBoolean(settings, DefaultCodeFormatterConstants.FORMATTER_ALIGN_SELECTOR_IN_METHOD_INVOCATION_ON_EXPRESSION_FIRST_LINE, DefaultCodeFormatterConstants.TRUE,
				v -> this.align_selector_in_method_invocation_on_expression_first_line = v);
		final Object alignTypeMembersOnColumnsOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_ALIGN_TYPE_MEMBERS_ON_COLUMNS);
		if (alignTypeMembersOnColumnsOption != null) {
			this.align_type_members_on_columns = DefaultCodeFormatterConstants.TRUE.equals(alignTypeMembersOnColumnsOption);
		}
		final Object alignVariableDeclarationsOnColumnsOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_ALIGN_VARIABLE_DECLARATIONS_ON_COLUMNS);
		if (alignVariableDeclarationsOnColumnsOption != null) {
			this.align_variable_declarations_on_columns = DefaultCodeFormatterConstants.TRUE.equals(alignVariableDeclarationsOnColumnsOption);
		}
		final Object alignAssignmentStatementsOnColumnsOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_ALIGN_ASSIGNMENT_STATEMENTS_ON_COLUMNS);
		if (alignAssignmentStatementsOnColumnsOption != null) {
			this.align_assignment_statements_on_columns = DefaultCodeFormatterConstants.TRUE.equals(alignAssignmentStatementsOnColumnsOption);
		}
		final Object alignGroupSepartionBlankLinesOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_ALIGN_FIELDS_GROUPING_BLANK_LINES);
		if (alignTypeMembersOnColumnsOption != null) {
			try {
				this.align_fields_grouping_blank_lines = Integer.parseInt((String) alignGroupSepartionBlankLinesOption);
			} catch(NumberFormatException | ClassCastException e) {
				this.align_fields_grouping_blank_lines = Integer.MAX_VALUE;
			}
		}
		final Object alignWithSpaces = settings.get(DefaultCodeFormatterConstants.FORMATTER_ALIGN_WITH_SPACES);
		if (alignWithSpaces != null) {
			this.align_with_spaces = DefaultCodeFormatterConstants.TRUE.equals(alignWithSpaces);
		}
		final Object bracePositionForAnnotationTypeDeclarationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_ANNOTATION_TYPE_DECLARATION);
		if (bracePositionForAnnotationTypeDeclarationOption != null) {
			try {
				this.brace_position_for_annotation_type_declaration = (String) bracePositionForAnnotationTypeDeclarationOption;
			} catch(ClassCastException e) {
				this.brace_position_for_annotation_type_declaration = DefaultCodeFormatterConstants.END_OF_LINE;
			}
		}
		final Object bracePositionForAnonymousTypeDeclarationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_ANONYMOUS_TYPE_DECLARATION);
		if (bracePositionForAnonymousTypeDeclarationOption != null) {
			try {
				this.brace_position_for_anonymous_type_declaration = (String) bracePositionForAnonymousTypeDeclarationOption;
			} catch(ClassCastException e) {
				this.brace_position_for_anonymous_type_declaration = DefaultCodeFormatterConstants.END_OF_LINE;
			}
		}
		final Object bracePositionForArrayInitializerOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_ARRAY_INITIALIZER);
		if (bracePositionForArrayInitializerOption != null) {
			try {
				this.brace_position_for_array_initializer = (String) bracePositionForArrayInitializerOption;
			} catch(ClassCastException e) {
				this.brace_position_for_array_initializer = DefaultCodeFormatterConstants.END_OF_LINE;
			}
		}
		final Object bracePositionForBlockOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_BLOCK);
		if (bracePositionForBlockOption != null) {
			try {
				this.brace_position_for_block = (String) bracePositionForBlockOption;
			} catch(ClassCastException e) {
				this.brace_position_for_block = DefaultCodeFormatterConstants.END_OF_LINE;
			}
		}
		final Object bracePositionForBlockInCaseOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_BLOCK_IN_CASE);
		if (bracePositionForBlockInCaseOption != null) {
			try {
				this.brace_position_for_block_in_case = (String) bracePositionForBlockInCaseOption;
			} catch(ClassCastException e) {
				this.brace_position_for_block_in_case = DefaultCodeFormatterConstants.END_OF_LINE;
			}
		}
		final Object bracePositionForConstructorDeclarationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_CONSTRUCTOR_DECLARATION);
		if (bracePositionForConstructorDeclarationOption != null) {
			try {
				this.brace_position_for_constructor_declaration = (String) bracePositionForConstructorDeclarationOption;
			} catch(ClassCastException e) {
				this.brace_position_for_constructor_declaration = DefaultCodeFormatterConstants.END_OF_LINE;
			}
		}
		final Object bracePositionForEnumConstantOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_ENUM_CONSTANT);
		if (bracePositionForEnumConstantOption != null) {
			try {
				this.brace_position_for_enum_constant = (String) bracePositionForEnumConstantOption;
			} catch(ClassCastException e) {
				this.brace_position_for_enum_constant = DefaultCodeFormatterConstants.END_OF_LINE;
			}
		}
		final Object bracePositionForEnumDeclarationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_ENUM_DECLARATION);
		if (bracePositionForEnumDeclarationOption != null) {
			try {
				this.brace_position_for_enum_declaration = (String) bracePositionForEnumDeclarationOption;
			} catch(ClassCastException e) {
				this.brace_position_for_enum_declaration = DefaultCodeFormatterConstants.END_OF_LINE;
			}
		}
		final Object bracePositionForLambdaDeclarationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_LAMBDA_BODY);
		if (bracePositionForLambdaDeclarationOption != null) {
			try {
				this.brace_position_for_lambda_body = (String) bracePositionForLambdaDeclarationOption;
			} catch(ClassCastException e) {
				this.brace_position_for_lambda_body = DefaultCodeFormatterConstants.END_OF_LINE;
			}
		}
		final Object bracePositionForMethodDeclarationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_METHOD_DECLARATION);
		if (bracePositionForMethodDeclarationOption != null) {
			try {
				this.brace_position_for_method_declaration = (String) bracePositionForMethodDeclarationOption;
			} catch(ClassCastException e) {
				this.brace_position_for_method_declaration = DefaultCodeFormatterConstants.END_OF_LINE;
			}
		}
		setString(settings, DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_RECORD_CONSTRUCTOR, BRACE_POSITION_VALUES,
				v -> this.brace_position_for_record_constructor = v);
		setString(settings, DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_RECORD_DECLARATION, BRACE_POSITION_VALUES,
				v -> this.brace_position_for_record_declaration = v);
		final Object bracePositionForSwitchOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_SWITCH);
		if (bracePositionForSwitchOption != null) {
			try {
				this.brace_position_for_switch = (String) bracePositionForSwitchOption;
			} catch(ClassCastException e) {
				this.brace_position_for_switch = DefaultCodeFormatterConstants.END_OF_LINE;
			}
		}
		final Object bracePositionForTypeDeclarationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_TYPE_DECLARATION);
		if (bracePositionForTypeDeclarationOption != null) {
			try {
				this.brace_position_for_type_declaration = (String) bracePositionForTypeDeclarationOption;
			} catch(ClassCastException e) {
				this.brace_position_for_type_declaration = DefaultCodeFormatterConstants.END_OF_LINE;
			}
		}

		final Object closingParenPositionInMethodDeclaration = settings.get(DefaultCodeFormatterConstants.FORMATTER_PARENTHESES_POSITIONS_IN_METHOD_DECLARATION);
		if (closingParenPositionInMethodDeclaration != null) {
			this.parenthesis_positions_in_method_declaration = toString(closingParenPositionInMethodDeclaration, DefaultCodeFormatterConstants.COMMON_LINES);
		}
		final Object closingParenPositionInMethodInvocation = settings.get(DefaultCodeFormatterConstants.FORMATTER_PARENTHESES_POSITIONS_IN_METHOD_INVOCATION);
		if (closingParenPositionInMethodInvocation != null) {
			this.parenthesis_positions_in_method_invocation = toString(closingParenPositionInMethodInvocation, DefaultCodeFormatterConstants.COMMON_LINES);
		}
		final Object closingParenPositionInEnumConstantDeclaration = settings.get(DefaultCodeFormatterConstants.FORMATTER_PARENTHESES_POSITIONS_IN_ENUM_CONSTANT_DECLARATION);
		if (closingParenPositionInEnumConstantDeclaration != null) {
			this.parenthesis_positions_in_enum_constant_declaration = toString(closingParenPositionInEnumConstantDeclaration, DefaultCodeFormatterConstants.COMMON_LINES);
		}
		setString(settings, DefaultCodeFormatterConstants.FORMATTER_PARENTHESES_POSITIONS_IN_RECORD_DECLARATION, PARENTHESIS_POSITION_VALUES,
			v -> this.parenthesis_positions_in_record_declaration = v);
		final Object closingParenPositionInIfWhileStatement = settings.get(DefaultCodeFormatterConstants.FORMATTER_PARENTHESES_POSITIONS_IN_IF_WHILE_STATEMENT);
		if (closingParenPositionInIfWhileStatement != null) {
			this.parenthesis_positions_in_if_while_statement = toString(closingParenPositionInIfWhileStatement, DefaultCodeFormatterConstants.COMMON_LINES);
		}
		final Object closingParenPositionInForStatement = settings.get(DefaultCodeFormatterConstants.FORMATTER_PARENTHESES_POSITIONS_IN_FOR_STATEMENT);
		if (closingParenPositionInForStatement != null) {
			this.parenthesis_positions_in_for_statement = toString(closingParenPositionInForStatement, DefaultCodeFormatterConstants.COMMON_LINES);
		}
		final Object closingParenPositionInSwitchStatement = settings.get(DefaultCodeFormatterConstants.FORMATTER_PARENTHESES_POSITIONS_IN_SWITCH_STATEMENT);
		if (closingParenPositionInSwitchStatement != null) {
			this.parenthesis_positions_in_switch_statement = toString(closingParenPositionInSwitchStatement, DefaultCodeFormatterConstants.COMMON_LINES);
		}
		final Object closingParenPositionInTryClause = settings.get(DefaultCodeFormatterConstants.FORMATTER_PARENTHESES_POSITIONS_IN_TRY_CLAUSE);
		if (closingParenPositionInTryClause != null) {
			this.parenthesis_positions_in_try_clause = toString(closingParenPositionInTryClause, DefaultCodeFormatterConstants.COMMON_LINES);
		}
		final Object closingParenPositionInCatchClause = settings.get(DefaultCodeFormatterConstants.FORMATTER_PARENTHESES_POSITIONS_IN_CATCH_CLAUSE);
		if (closingParenPositionInCatchClause != null) {
			this.parenthesis_positions_in_catch_clause = toString(closingParenPositionInCatchClause, DefaultCodeFormatterConstants.COMMON_LINES);
		}
		final Object closingParenPositionInAnnotation = settings.get(DefaultCodeFormatterConstants.FORMATTER_PARENTHESES_POSITIONS_IN_ANNOTATION);
		if (closingParenPositionInAnnotation != null) {
			this.parenthesis_positions_in_annotation = toString(closingParenPositionInAnnotation, DefaultCodeFormatterConstants.COMMON_LINES);
		}
		final Object closingParenPositionInLambdaDeclaration = settings.get(DefaultCodeFormatterConstants.FORMATTER_PARENTHESES_POSITIONS_IN_LAMBDA_DECLARATION);
		if (closingParenPositionInLambdaDeclaration != null) {
			this.parenthesis_positions_in_lambda_declaration = toString(closingParenPositionInLambdaDeclaration, DefaultCodeFormatterConstants.COMMON_LINES);
		}

		final Object continuationIndentationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_CONTINUATION_INDENTATION);
		if (continuationIndentationOption != null) {
			try {
				this.continuation_indentation = Integer.parseInt((String) continuationIndentationOption);
			} catch(NumberFormatException | ClassCastException e) {
				this.continuation_indentation = 2;
			}
		}
		final Object continuationIndentationForArrayInitializerOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_CONTINUATION_INDENTATION_FOR_ARRAY_INITIALIZER);
		if (continuationIndentationForArrayInitializerOption != null) {
			try {
				this.continuation_indentation_for_array_initializer = Integer.parseInt((String) continuationIndentationForArrayInitializerOption);
			} catch(NumberFormatException | ClassCastException e) {
				this.continuation_indentation_for_array_initializer = 2;
			}
		}
		final Object blankLinesAfterImportsOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_AFTER_IMPORTS);
		if (blankLinesAfterImportsOption != null) {
			try {
				this.blank_lines_after_imports = Integer.parseInt((String) blankLinesAfterImportsOption);
			} catch(NumberFormatException | ClassCastException e) {
				this.blank_lines_after_imports = 0;
			}
		}
		final Object blankLinesAfterPackageOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_AFTER_PACKAGE);
		if (blankLinesAfterPackageOption != null) {
			try {
				this.blank_lines_after_package = Integer.parseInt((String) blankLinesAfterPackageOption);
			} catch(NumberFormatException | ClassCastException e) {
				this.blank_lines_after_package = 0;
			}
		}
		final Object blankLinesBeforeFieldOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_FIELD);
		if (blankLinesBeforeFieldOption != null) {
			try {
				this.blank_lines_before_field = Integer.parseInt((String) blankLinesBeforeFieldOption);
			} catch(NumberFormatException | ClassCastException e) {
				this.blank_lines_before_field = 0;
			}
		}
		final Object blankLinesBeforeFirstClassBodyDeclarationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_FIRST_CLASS_BODY_DECLARATION);
		if (blankLinesBeforeFirstClassBodyDeclarationOption != null) {
			try {
				this.blank_lines_before_first_class_body_declaration = Integer.parseInt((String) blankLinesBeforeFirstClassBodyDeclarationOption);
			} catch(NumberFormatException | ClassCastException e) {
				this.blank_lines_before_first_class_body_declaration = 0;
			}
		}
		setInt(settings, DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_AFTER_LAST_CLASS_BODY_DECLARATION,
				v -> this.blank_lines_after_last_class_body_declaration = v);
		final Object blankLinesBeforeImportsOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_IMPORTS);
		if (blankLinesBeforeImportsOption != null) {
			try {
				this.blank_lines_before_imports = Integer.parseInt((String) blankLinesBeforeImportsOption);
			} catch(NumberFormatException | ClassCastException e) {
				this.blank_lines_before_imports = 0;
			}
		}
		final Object blankLinesBeforeMemberTypeOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_MEMBER_TYPE);
		if (blankLinesBeforeMemberTypeOption != null) {
			try {
				this.blank_lines_before_member_type = Integer.parseInt((String) blankLinesBeforeMemberTypeOption);
			} catch(NumberFormatException | ClassCastException e) {
				this.blank_lines_before_member_type = 0;
			}
		}
		setInt(settings, DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_ABSTRACT_METHOD,
				v -> this.blank_lines_before_abstract_method = v);
		final Object blankLinesBeforeMethodOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_METHOD);
		if (blankLinesBeforeMethodOption != null) {
			try {
				this.blank_lines_before_method = Integer.parseInt((String) blankLinesBeforeMethodOption);
			} catch(NumberFormatException | ClassCastException e) {
				this.blank_lines_before_method = 0;
			}
		}
		final Object blankLinesBeforeNewChunkOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_NEW_CHUNK);
		if (blankLinesBeforeNewChunkOption != null) {
			try {
				this.blank_lines_before_new_chunk = Integer.parseInt((String) blankLinesBeforeNewChunkOption);
			} catch(NumberFormatException | ClassCastException e) {
				this.blank_lines_before_new_chunk = 0;
			}
		}
		final Object blankLinesBeforePackageOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_PACKAGE);
		if (blankLinesBeforePackageOption != null) {
			try {
				this.blank_lines_before_package = Integer.parseInt((String) blankLinesBeforePackageOption);
			} catch(NumberFormatException | ClassCastException e) {
				this.blank_lines_before_package = 0;
			}
		}
		final Object blankLinesBetweenImportGroupsOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BETWEEN_IMPORT_GROUPS);
		if (blankLinesBetweenImportGroupsOption != null) {
			try {
				this.blank_lines_between_import_groups = Integer.parseInt((String) blankLinesBetweenImportGroupsOption);
			} catch(NumberFormatException | ClassCastException e) {
				this.blank_lines_between_import_groups = 1;
			}
		}
		final Object blankLinesBetweenTypeDeclarationsOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BETWEEN_TYPE_DECLARATIONS);
		if (blankLinesBetweenTypeDeclarationsOption != null) {
			try {
				this.blank_lines_between_type_declarations = Integer.parseInt((String) blankLinesBetweenTypeDeclarationsOption);
			} catch(NumberFormatException | ClassCastException e) {
				this.blank_lines_between_type_declarations = 0;
			}
		}
		final Object blankLinesAtBeginningOfMethodBodyOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_AT_BEGINNING_OF_METHOD_BODY);
		if (blankLinesAtBeginningOfMethodBodyOption != null) {
			try {
				this.blank_lines_at_beginning_of_method_body = Integer.parseInt((String) blankLinesAtBeginningOfMethodBodyOption);
			} catch(NumberFormatException | ClassCastException e) {
				this.blank_lines_at_beginning_of_method_body = 0;
			}
		}
		setInt(settings, DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_AT_END_OF_METHOD_BODY,
				v -> this.blank_lines_at_end_of_method_body = v);
		setInt(settings, DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_AT_BEGINNING_OF_CODE_BLOCK,
				v -> this.blank_lines_at_beginning_of_code_block = v);
		setInt(settings, DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_AT_END_OF_CODE_BLOCK,
				v -> this.blank_lines_at_end_of_code_block = v);
		setInt(settings, DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_CODE_BLOCK,
				v -> this.blank_lines_before_code_block = v);
		setInt(settings, DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_AFTER_CODE_BLOCK,
				v -> this.blank_lines_after_code_block = v);
		setInt(settings, DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BETWEEN_STATEMENT_GROUPS_IN_SWITCH,
				v -> this.blank_lines_between_statement_groups_in_switch = v);
		final Object insertNewLineAfterTypeAnnotationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_TYPE_ANNOTATION);
		if (insertNewLineAfterTypeAnnotationOption != null) {
			this.insert_new_line_after_type_annotation = JavaCore.INSERT.equals(insertNewLineAfterTypeAnnotationOption);
		}
		setDeprecatedOptions(settings);
		final Object commentFormatJavadocCommentOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_JAVADOC_COMMENT);
		if (commentFormatJavadocCommentOption != null) {
			this.comment_format_javadoc_comment = DefaultCodeFormatterConstants.TRUE.equals(commentFormatJavadocCommentOption);
		}
		final Object commentFormatBlockCommentOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_BLOCK_COMMENT);
		if (commentFormatBlockCommentOption != null) {
			this.comment_format_block_comment = DefaultCodeFormatterConstants.TRUE.equals(commentFormatBlockCommentOption);
		}
		final Object commentFormatLineCommentOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_LINE_COMMENT);
		if (commentFormatLineCommentOption != null) {
			this.comment_format_line_comment = DefaultCodeFormatterConstants.TRUE.equals(commentFormatLineCommentOption);
		}
		final Object formatLineCommentStartingOnFirstColumnOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_LINE_COMMENT_STARTING_ON_FIRST_COLUMN);
		if (formatLineCommentStartingOnFirstColumnOption != null) {
			this.comment_format_line_comment_starting_on_first_column = DefaultCodeFormatterConstants.TRUE.equals(formatLineCommentStartingOnFirstColumnOption);
		}
		final Object commentFormatHeaderOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_HEADER);
		if (commentFormatHeaderOption != null) {
			this.comment_format_header = DefaultCodeFormatterConstants.TRUE.equals(commentFormatHeaderOption);
		}
		final Object commentFormatHtmlOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_HTML);
		if (commentFormatHtmlOption != null) {
			this.comment_format_html = DefaultCodeFormatterConstants.TRUE.equals(commentFormatHtmlOption);
		}
		final Object commentFormatSourceOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_SOURCE);
		if (commentFormatSourceOption != null) {
			this.comment_format_source = DefaultCodeFormatterConstants.TRUE.equals(commentFormatSourceOption);
		}
		final Object commentIndentParameterDescriptionOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_COMMENT_INDENT_PARAMETER_DESCRIPTION);
		if (commentIndentParameterDescriptionOption != null) {
			this.comment_indent_parameter_description = DefaultCodeFormatterConstants.TRUE.equals(commentIndentParameterDescriptionOption);
		}
		setBoolean(settings, DefaultCodeFormatterConstants.FORMATTER_COMMENT_INDENT_TAG_DESCRIPTION, DefaultCodeFormatterConstants.TRUE,
				v -> this.comment_indent_tag_description = v);
		final Object commentIndentRootTagsOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_COMMENT_INDENT_ROOT_TAGS);
		if (commentIndentRootTagsOption != null) {
			this.comment_indent_root_tags = DefaultCodeFormatterConstants.TRUE.equals(commentIndentRootTagsOption);
		}
		final Object commentAlignTagsDescriptionsOption= settings.get(DefaultCodeFormatterConstants.FORMATTER_COMMENT_ALIGN_TAGS_NAMES_DESCRIPTIONS);
		if (commentAlignTagsDescriptionsOption != null) {
			this.comment_align_tags_names_descriptions = DefaultCodeFormatterConstants.TRUE.equals(commentAlignTagsDescriptionsOption);
		}
		final Object commentAlignTagsGroupedOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_COMMENT_ALIGN_TAGS_DESCREIPTIONS_GROUPED);
		if (commentAlignTagsGroupedOption != null) {
			this.comment_align_tags_descriptions_grouped = DefaultCodeFormatterConstants.TRUE.equals(commentAlignTagsGroupedOption);
		}
		final Object commentInsertEmptyLineBeforeRootTagsOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_COMMENT_INSERT_EMPTY_LINE_BEFORE_ROOT_TAGS);
		if (commentInsertEmptyLineBeforeRootTagsOption != null) {
			this.comment_insert_empty_line_before_root_tags = JavaCore.INSERT.equals(commentInsertEmptyLineBeforeRootTagsOption);
		}
		setBoolean(settings, DefaultCodeFormatterConstants.FORMATTER_COMMENT_INSERT_EMPTY_LINE_BETWEEN_DIFFERENT_TAGS, JavaCore.INSERT,
				v -> this.comment_insert_empty_line_between_different_tags = v);
		final Object commentInsertNewLineForParameterOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_COMMENT_INSERT_NEW_LINE_FOR_PARAMETER);
		if (commentInsertNewLineForParameterOption != null) {
			this.comment_insert_new_line_for_parameter = JavaCore.INSERT.equals(commentInsertNewLineForParameterOption);
		}
		final Object commentPreserveWhiteSpaceBetweenCodeAndLineCommentsOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_COMMENT_PRESERVE_WHITE_SPACE_BETWEEN_CODE_AND_LINE_COMMENT);
		if (commentPreserveWhiteSpaceBetweenCodeAndLineCommentsOption != null) {
			this.comment_preserve_white_space_between_code_and_line_comments = DefaultCodeFormatterConstants.TRUE.equals(commentPreserveWhiteSpaceBetweenCodeAndLineCommentsOption);
		}
		final Object commentLineLengthOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_COMMENT_LINE_LENGTH);
		if (commentLineLengthOption != null) {
			try {
				this.comment_line_length = Integer.parseInt((String) commentLineLengthOption);
			} catch(NumberFormatException | ClassCastException e) {
				this.comment_line_length = 80;
			}
		}
		final Object commentCountLineLengthFromStartingPositionOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_COMMENT_COUNT_LINE_LENGTH_FROM_STARTING_POSITION);
		if (commentCountLineLengthFromStartingPositionOption != null) {
			this.comment_count_line_length_from_starting_position = DefaultCodeFormatterConstants.TRUE.equals(commentCountLineLengthFromStartingPositionOption);
		}
		final Object commentNewLinesAtBlockBoundariesOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_COMMENT_NEW_LINES_AT_BLOCK_BOUNDARIES);
		if (commentNewLinesAtBlockBoundariesOption != null) {
			this.comment_new_lines_at_block_boundaries = DefaultCodeFormatterConstants.TRUE.equals(commentNewLinesAtBlockBoundariesOption);
		}
		final Object commentNewLinesAtJavadocBoundariesOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_COMMENT_NEW_LINES_AT_JAVADOC_BOUNDARIES);
		if (commentNewLinesAtJavadocBoundariesOption != null) {
			this.comment_new_lines_at_javadoc_boundaries = DefaultCodeFormatterConstants.TRUE.equals(commentNewLinesAtJavadocBoundariesOption);
		}
		final Object indentStatementsCompareToBlockOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INDENT_STATEMENTS_COMPARE_TO_BLOCK);
		if (indentStatementsCompareToBlockOption != null) {
			this.indent_statements_compare_to_block = DefaultCodeFormatterConstants.TRUE.equals(indentStatementsCompareToBlockOption);
		}
		final Object indentStatementsCompareToBodyOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INDENT_STATEMENTS_COMPARE_TO_BODY);
		if (indentStatementsCompareToBodyOption != null) {
			this.indent_statements_compare_to_body = DefaultCodeFormatterConstants.TRUE.equals(indentStatementsCompareToBodyOption);
		}
		setBoolean(settings, DefaultCodeFormatterConstants.FORMATTER_INDENT_BODY_DECLARATIONS_COMPARE_TO_RECORD_HEADER, DefaultCodeFormatterConstants.TRUE,
			v -> this.indent_body_declarations_compare_to_record_header = v);
		final Object indentBodyDeclarationsCompareToAnnotationDeclarationHeaderOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INDENT_BODY_DECLARATIONS_COMPARE_TO_ANNOTATION_DECLARATION_HEADER);
		if (indentBodyDeclarationsCompareToAnnotationDeclarationHeaderOption != null) {
			this.indent_body_declarations_compare_to_annotation_declaration_header = DefaultCodeFormatterConstants.TRUE.equals(indentBodyDeclarationsCompareToAnnotationDeclarationHeaderOption);
		}
		final Object indentBodyDeclarationsCompareToEnumConstantHeaderOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INDENT_BODY_DECLARATIONS_COMPARE_TO_ENUM_CONSTANT_HEADER);
		if (indentBodyDeclarationsCompareToEnumConstantHeaderOption != null) {
			this.indent_body_declarations_compare_to_enum_constant_header = DefaultCodeFormatterConstants.TRUE.equals(indentBodyDeclarationsCompareToEnumConstantHeaderOption);
		}
		final Object indentBodyDeclarationsCompareToEnumDeclarationHeaderOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INDENT_BODY_DECLARATIONS_COMPARE_TO_ENUM_DECLARATION_HEADER);
		if (indentBodyDeclarationsCompareToEnumDeclarationHeaderOption != null) {
			this.indent_body_declarations_compare_to_enum_declaration_header = DefaultCodeFormatterConstants.TRUE.equals(indentBodyDeclarationsCompareToEnumDeclarationHeaderOption);
		}
		final Object indentBodyDeclarationsCompareToTypeHeaderOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INDENT_BODY_DECLARATIONS_COMPARE_TO_TYPE_HEADER);
		if (indentBodyDeclarationsCompareToTypeHeaderOption != null) {
			this.indent_body_declarations_compare_to_type_header = DefaultCodeFormatterConstants.TRUE.equals(indentBodyDeclarationsCompareToTypeHeaderOption);
		}
		final Object indentBreaksCompareToCasesOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INDENT_BREAKS_COMPARE_TO_CASES);
		if (indentBreaksCompareToCasesOption != null) {
			this.indent_breaks_compare_to_cases = DefaultCodeFormatterConstants.TRUE.equals(indentBreaksCompareToCasesOption);
		}
		final Object indentEmptyLinesOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INDENT_EMPTY_LINES);
		if (indentEmptyLinesOption != null) {
			this.indent_empty_lines = DefaultCodeFormatterConstants.TRUE.equals(indentEmptyLinesOption);
		}
		final Object indentSwitchstatementsCompareToCasesOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INDENT_SWITCHSTATEMENTS_COMPARE_TO_CASES);
		if (indentSwitchstatementsCompareToCasesOption != null) {
			this.indent_switchstatements_compare_to_cases = DefaultCodeFormatterConstants.TRUE.equals(indentSwitchstatementsCompareToCasesOption);
		}
		final Object indentSwitchstatementsCompareToSwitchOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INDENT_SWITCHSTATEMENTS_COMPARE_TO_SWITCH);
		if (indentSwitchstatementsCompareToSwitchOption != null) {
			this.indent_switchstatements_compare_to_switch = DefaultCodeFormatterConstants.TRUE.equals(indentSwitchstatementsCompareToSwitchOption);
		}
		final Object indentationSizeOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INDENTATION_SIZE);
		if (indentationSizeOption != null) {
			int indentationSize = 4;
			try {
				indentationSize = Integer.parseInt((String) indentationSizeOption);
			} catch(NumberFormatException | ClassCastException e) {
				// keep default
			}
			// reverse values swapping performed by IndentationTabPage
			if (DefaultCodeFormatterConstants.MIXED.equals(settings.get(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR)))
				this.indentation_size = indentationSize;
			else if (JavaCore.SPACE.equals(settings.get(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR)))
				this.tab_size = indentationSize;
		}
		final Object insertNewLineAfterOpeningBraceInArrayInitializerOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_OPENING_BRACE_IN_ARRAY_INITIALIZER);
		if (insertNewLineAfterOpeningBraceInArrayInitializerOption != null) {
			this.insert_new_line_after_opening_brace_in_array_initializer = JavaCore.INSERT.equals(insertNewLineAfterOpeningBraceInArrayInitializerOption);
		}
		final Object insertNewLineAtEndOfFileIfMissingOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AT_END_OF_FILE_IF_MISSING);
		if (insertNewLineAtEndOfFileIfMissingOption != null) {
			this.insert_new_line_at_end_of_file_if_missing = JavaCore.INSERT.equals(insertNewLineAtEndOfFileIfMissingOption);
		}
		final Object insertNewLineBeforeCatchInTryStatementOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_BEFORE_CATCH_IN_TRY_STATEMENT);
		if (insertNewLineBeforeCatchInTryStatementOption != null) {
			this.insert_new_line_before_catch_in_try_statement = JavaCore.INSERT.equals(insertNewLineBeforeCatchInTryStatementOption);
		}
		final Object insertNewLineBeforeClosingBraceInArrayInitializerOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_BEFORE_CLOSING_BRACE_IN_ARRAY_INITIALIZER);
		if (insertNewLineBeforeClosingBraceInArrayInitializerOption != null) {
			this.insert_new_line_before_closing_brace_in_array_initializer = JavaCore.INSERT.equals(insertNewLineBeforeClosingBraceInArrayInitializerOption);
		}
		final Object insertNewLineBeforeElseInIfStatementOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_BEFORE_ELSE_IN_IF_STATEMENT);
		if (insertNewLineBeforeElseInIfStatementOption != null) {
			this.insert_new_line_before_else_in_if_statement = JavaCore.INSERT.equals(insertNewLineBeforeElseInIfStatementOption);
		}
		final Object insertNewLineBeforeFinallyInTryStatementOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_BEFORE_FINALLY_IN_TRY_STATEMENT);
		if (insertNewLineBeforeFinallyInTryStatementOption != null) {
			this.insert_new_line_before_finally_in_try_statement = JavaCore.INSERT.equals(insertNewLineBeforeFinallyInTryStatementOption);
		}
		final Object insertNewLineBeforeWhileInDoStatementOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_BEFORE_WHILE_IN_DO_STATEMENT);
		if (insertNewLineBeforeWhileInDoStatementOption != null) {
			this.insert_new_line_before_while_in_do_statement = JavaCore.INSERT.equals(insertNewLineBeforeWhileInDoStatementOption);
		}

		setString(settings, DefaultCodeFormatterConstants.FORMATTER_KEEP_ANNOTATION_DECLARATION_ON_ONE_LINE, KEEP_ON_ONE_LINE_VALUES,
				v -> this.keep_annotation_declaration_on_one_line = v);
		setString(settings, DefaultCodeFormatterConstants.FORMATTER_KEEP_ANONYMOUS_TYPE_DECLARATION_ON_ONE_LINE, KEEP_ON_ONE_LINE_VALUES,
				v -> this.keep_anonymous_type_declaration_on_one_line = v);
		setString(settings, DefaultCodeFormatterConstants.FORMATTER_KEEP_IF_THEN_BODY_BLOCK_ON_ONE_LINE, KEEP_ON_ONE_LINE_VALUES,
				v -> this.keep_if_then_body_block_on_one_line = v);
		setString(settings, DefaultCodeFormatterConstants.FORMATTER_KEEP_SWITCH_BODY_BLOCK_ON_ONE_LINE, KEEP_ON_ONE_LINE_VALUES,
				v -> this.keep_switch_body_block_on_one_line = v);
		setString(settings, DefaultCodeFormatterConstants.FORMATTER_KEEP_SWITCH_CASE_WITH_ARROW_ON_ONE_LINE, KEEP_ON_ONE_LINE_VALUES,
				v -> this.keep_switch_case_with_arrow_on_one_line = v);
		setString(settings, DefaultCodeFormatterConstants.FORMATTER_KEEP_LOOP_BODY_BLOCK_ON_ONE_LINE, KEEP_ON_ONE_LINE_VALUES,
				v -> this.keep_loop_body_block_on_one_line = v);
		setString(settings, DefaultCodeFormatterConstants.FORMATTER_KEEP_LAMBDA_BODY_BLOCK_ON_ONE_LINE, KEEP_ON_ONE_LINE_VALUES,
				v -> this.keep_lambda_body_block_on_one_line = v);
		setString(settings, DefaultCodeFormatterConstants.FORMATTER_KEEP_CODE_BLOCK_ON_ONE_LINE,
				Arrays.asList(DefaultCodeFormatterConstants.ONE_LINE_NEVER, DefaultCodeFormatterConstants.ONE_LINE_IF_EMPTY),
				v -> this.keep_code_block_on_one_line = v);
		setString(settings, DefaultCodeFormatterConstants.FORMATTER_KEEP_ENUM_CONSTANT_DECLARATION_ON_ONE_LINE, KEEP_ON_ONE_LINE_VALUES,
				v -> this.keep_enum_constant_declaration_on_one_line = v);
		setString(settings, DefaultCodeFormatterConstants.FORMATTER_KEEP_ENUM_DECLARATION_ON_ONE_LINE, KEEP_ON_ONE_LINE_VALUES,
				v -> this.keep_enum_declaration_on_one_line = v);
		setString(settings, DefaultCodeFormatterConstants.FORMATTER_KEEP_METHOD_BODY_ON_ONE_LINE, KEEP_ON_ONE_LINE_VALUES,
				v -> this.keep_method_body_on_one_line = v);
		setString(settings, DefaultCodeFormatterConstants.FORMATTER_KEEP_TYPE_DECLARATION_ON_ONE_LINE, KEEP_ON_ONE_LINE_VALUES,
				v -> this.keep_type_declaration_on_one_line = v);
		setString(settings, DefaultCodeFormatterConstants.FORMATTER_KEEP_RECORD_DECLARATION_ON_ONE_LINE, KEEP_ON_ONE_LINE_VALUES,
				v -> this.keep_record_declaration_on_one_line = v);
		setString(settings, DefaultCodeFormatterConstants.FORMATTER_KEEP_RECORD_CONSTRUCTOR_ON_ONE_LINE, KEEP_ON_ONE_LINE_VALUES,
				v -> this.keep_record_constructor_on_one_line = v);
		setBoolean(settings, DefaultCodeFormatterConstants.FORMATTER_KEEP_SIMPLE_GETTER_SETTER_ON_ONE_LINE, DefaultCodeFormatterConstants.TRUE,
				v -> this.keep_simple_getter_setter_on_one_line = v);

		final Object insertNewLineAfterLabelOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_LABEL);
		if (insertNewLineAfterLabelOption != null) {
			this.insert_new_line_after_label = JavaCore.INSERT.equals(insertNewLineAfterLabelOption);
		}
		final Object insertSpaceAfterAndInWildcardOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_AND_IN_TYPE_PARAMETER);
		if (insertSpaceAfterAndInWildcardOption != null) {
			this.insert_space_after_and_in_type_parameter = JavaCore.INSERT.equals(insertSpaceAfterAndInWildcardOption);
		}
		setBoolean(settings, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_ARROW_IN_SWITCH_CASE, JavaCore.INSERT,
				v -> this.insert_space_after_arrow_in_switch_case = v);
		setBoolean(settings, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_ARROW_IN_SWITCH_DEFAULT, JavaCore.INSERT,
				v -> this.insert_space_after_arrow_in_switch_default = v);
		final Object insertSpaceAfterAssignmentOperatorOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_ASSIGNMENT_OPERATOR);
		if (insertSpaceAfterAssignmentOperatorOption != null) {
			this.insert_space_after_assignment_operator = JavaCore.INSERT.equals(insertSpaceAfterAssignmentOperatorOption);
		}
		final Object insertSpaceAfterAtInAnnotationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_AT_IN_ANNOTATION);
		if (insertSpaceAfterAtInAnnotationOption != null) {
			this.insert_space_after_at_in_annotation = JavaCore.INSERT.equals(insertSpaceAfterAtInAnnotationOption);
		}
		final Object insertSpaceAfterAtInAnnotationTypeDeclarationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_AT_IN_ANNOTATION_TYPE_DECLARATION);
		if (insertSpaceAfterAtInAnnotationTypeDeclarationOption != null) {
			this.insert_space_after_at_in_annotation_type_declaration = JavaCore.INSERT.equals(insertSpaceAfterAtInAnnotationTypeDeclarationOption);
		}
		setBoolean(settings, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_MULTIPLICATIVE_OPERATOR, JavaCore.INSERT,
				v -> this.insert_space_after_multiplicative_operator = v);
		setBoolean(settings, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_ADDITIVE_OPERATOR, JavaCore.INSERT,
				v -> this.insert_space_after_additive_operator = v);
		setBoolean(settings, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_STRING_CONCATENATION, JavaCore.INSERT,
				v -> this.insert_space_after_string_concatenation = v);
		setBoolean(settings, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_SHIFT_OPERATOR, JavaCore.INSERT,
				v -> this.insert_space_after_shift_operator = v);
		setBoolean(settings, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_RELATIONAL_OPERATOR, JavaCore.INSERT,
				v -> this.insert_space_after_relational_operator = v);
		setBoolean(settings, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_BITWISE_OPERATOR, JavaCore.INSERT,
				v -> this.insert_space_after_bitwise_operator = v);
		setBoolean(settings, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_LOGICAL_OPERATOR, JavaCore.INSERT,
				v -> this.insert_space_after_logical_operator = v);
		final Object insertSpaceAfterClosingAngleBracketInTypeArgumentsOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_CLOSING_ANGLE_BRACKET_IN_TYPE_ARGUMENTS);
		if (insertSpaceAfterClosingAngleBracketInTypeArgumentsOption != null) {
			this.insert_space_after_closing_angle_bracket_in_type_arguments = JavaCore.INSERT.equals(insertSpaceAfterClosingAngleBracketInTypeArgumentsOption);
		}
		final Object insertSpaceAfterClosingAngleBracketInTypeParametersOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_CLOSING_ANGLE_BRACKET_IN_TYPE_PARAMETERS);
		if (insertSpaceAfterClosingAngleBracketInTypeParametersOption != null) {
			this.insert_space_after_closing_angle_bracket_in_type_parameters = JavaCore.INSERT.equals(insertSpaceAfterClosingAngleBracketInTypeParametersOption);
		}
		final Object insertSpaceAfterClosingParenInCastOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_CLOSING_PAREN_IN_CAST);
		if (insertSpaceAfterClosingParenInCastOption != null) {
			this.insert_space_after_closing_paren_in_cast = JavaCore.INSERT.equals(insertSpaceAfterClosingParenInCastOption);
		}
		final Object insertSpaceAfterClosingBraceInBlockOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_CLOSING_BRACE_IN_BLOCK);
		if (insertSpaceAfterClosingBraceInBlockOption != null) {
			this.insert_space_after_closing_brace_in_block = JavaCore.INSERT.equals(insertSpaceAfterClosingBraceInBlockOption);
		}
		final Object insertSpaceAfterColonInAssertOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COLON_IN_ASSERT);
		if (insertSpaceAfterColonInAssertOption != null) {
			this.insert_space_after_colon_in_assert = JavaCore.INSERT.equals(insertSpaceAfterColonInAssertOption);
		}
		final Object insertSpaceAfterColonInCaseOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COLON_IN_CASE);
		if (insertSpaceAfterColonInCaseOption != null) {
			this.insert_space_after_colon_in_case = JavaCore.INSERT.equals(insertSpaceAfterColonInCaseOption);
		}
		final Object insertSpaceAfterColonInConditionalOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COLON_IN_CONDITIONAL);
		if (insertSpaceAfterColonInConditionalOption != null) {
			this.insert_space_after_colon_in_conditional = JavaCore.INSERT.equals(insertSpaceAfterColonInConditionalOption);
		}
		final Object insertSpaceAfterColonInForOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COLON_IN_FOR);
		if (insertSpaceAfterColonInForOption != null) {
			this.insert_space_after_colon_in_for = JavaCore.INSERT.equals(insertSpaceAfterColonInForOption);
		}
		final Object insertSpaceAfterColonInLabeledStatementOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COLON_IN_LABELED_STATEMENT);
		if (insertSpaceAfterColonInLabeledStatementOption != null) {
			this.insert_space_after_colon_in_labeled_statement = JavaCore.INSERT.equals(insertSpaceAfterColonInLabeledStatementOption);
		}
		final Object insertSpaceAfterCommaInAllocationExpressionOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_ALLOCATION_EXPRESSION);
		if (insertSpaceAfterCommaInAllocationExpressionOption != null) {
			this.insert_space_after_comma_in_allocation_expression = JavaCore.INSERT.equals(insertSpaceAfterCommaInAllocationExpressionOption);
		}
		final Object insertSpaceAfterCommaInAnnotationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_ANNOTATION);
		if (insertSpaceAfterCommaInAnnotationOption != null) {
			this.insert_space_after_comma_in_annotation = JavaCore.INSERT.equals(insertSpaceAfterCommaInAnnotationOption);
		}
		final Object insertSpaceAfterCommaInArrayInitializerOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_ARRAY_INITIALIZER);
		if (insertSpaceAfterCommaInArrayInitializerOption != null) {
			this.insert_space_after_comma_in_array_initializer = JavaCore.INSERT.equals(insertSpaceAfterCommaInArrayInitializerOption);
		}
		final Object insertSpaceAfterCommaInConstructorDeclarationParametersOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_CONSTRUCTOR_DECLARATION_PARAMETERS);
		if (insertSpaceAfterCommaInConstructorDeclarationParametersOption != null) {
			this.insert_space_after_comma_in_constructor_declaration_parameters = JavaCore.INSERT.equals(insertSpaceAfterCommaInConstructorDeclarationParametersOption);
		}
		final Object insertSpaceAfterCommaInConstructorDeclarationThrowsOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_CONSTRUCTOR_DECLARATION_THROWS);
		if (insertSpaceAfterCommaInConstructorDeclarationThrowsOption != null) {
			this.insert_space_after_comma_in_constructor_declaration_throws = JavaCore.INSERT.equals(insertSpaceAfterCommaInConstructorDeclarationThrowsOption);
		}
		final Object insertSpaceAfterCommaInEnumConstantArgumentsOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_ENUM_CONSTANT_ARGUMENTS);
		if (insertSpaceAfterCommaInEnumConstantArgumentsOption != null) {
			this.insert_space_after_comma_in_enum_constant_arguments = JavaCore.INSERT.equals(insertSpaceAfterCommaInEnumConstantArgumentsOption);
		}
		final Object insertSpaceAfterCommaInEnumDeclarationsOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_ENUM_DECLARATIONS);
		if (insertSpaceAfterCommaInEnumDeclarationsOption != null) {
			this.insert_space_after_comma_in_enum_declarations = JavaCore.INSERT.equals(insertSpaceAfterCommaInEnumDeclarationsOption);
		}
		final Object insertSpaceAfterCommaInExplicitConstructorCallArgumentsOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_EXPLICIT_CONSTRUCTOR_CALL_ARGUMENTS);
		if (insertSpaceAfterCommaInExplicitConstructorCallArgumentsOption != null) {
			this.insert_space_after_comma_in_explicit_constructor_call_arguments = JavaCore.INSERT.equals(insertSpaceAfterCommaInExplicitConstructorCallArgumentsOption);
		}
		final Object insertSpaceAfterCommaInForIncrementsOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_FOR_INCREMENTS);
		if (insertSpaceAfterCommaInForIncrementsOption != null) {
			this.insert_space_after_comma_in_for_increments = JavaCore.INSERT.equals(insertSpaceAfterCommaInForIncrementsOption);
		}
		final Object insertSpaceAfterCommaInForInitsOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_FOR_INITS);
		if (insertSpaceAfterCommaInForInitsOption != null) {
			this.insert_space_after_comma_in_for_inits = JavaCore.INSERT.equals(insertSpaceAfterCommaInForInitsOption);
		}
		final Object insertSpaceAfterCommaInMethodInvocationArgumentsOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_METHOD_INVOCATION_ARGUMENTS);
		if (insertSpaceAfterCommaInMethodInvocationArgumentsOption != null) {
			this.insert_space_after_comma_in_method_invocation_arguments = JavaCore.INSERT.equals(insertSpaceAfterCommaInMethodInvocationArgumentsOption);
		}
		final Object insertSpaceAfterCommaInMethodDeclarationParametersOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_METHOD_DECLARATION_PARAMETERS);
		if (insertSpaceAfterCommaInMethodDeclarationParametersOption != null) {
			this.insert_space_after_comma_in_method_declaration_parameters = JavaCore.INSERT.equals(insertSpaceAfterCommaInMethodDeclarationParametersOption);
		}
		final Object insertSpaceAfterCommaInMethodDeclarationThrowsOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_METHOD_DECLARATION_THROWS);
		if (insertSpaceAfterCommaInMethodDeclarationThrowsOption != null) {
			this.insert_space_after_comma_in_method_declaration_throws = JavaCore.INSERT.equals(insertSpaceAfterCommaInMethodDeclarationThrowsOption);
		}
		final Object insertSpaceAfterCommaInMultipleFieldDeclarationsOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_MULTIPLE_FIELD_DECLARATIONS);
		if (insertSpaceAfterCommaInMultipleFieldDeclarationsOption != null) {
			this.insert_space_after_comma_in_multiple_field_declarations = JavaCore.INSERT.equals(insertSpaceAfterCommaInMultipleFieldDeclarationsOption);
		}
		final Object insertSpaceAfterCommaInMultipleLocalDeclarationsOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_MULTIPLE_LOCAL_DECLARATIONS);
		if (insertSpaceAfterCommaInMultipleLocalDeclarationsOption != null) {
			this.insert_space_after_comma_in_multiple_local_declarations = JavaCore.INSERT.equals(insertSpaceAfterCommaInMultipleLocalDeclarationsOption);
		}
		final Object insertSpaceAfterCommaInParameterizedTypeReferenceOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_PARAMETERIZED_TYPE_REFERENCE);
		if (insertSpaceAfterCommaInParameterizedTypeReferenceOption != null) {
			this.insert_space_after_comma_in_parameterized_type_reference = JavaCore.INSERT.equals(insertSpaceAfterCommaInParameterizedTypeReferenceOption);
		}
		setBoolean(settings, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_PERMITTED_TYPES, JavaCore.INSERT,
				v -> this.insert_space_after_comma_in_permitted_types = v);
		setBoolean(settings, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_RECORD_COMPONENTS, JavaCore.INSERT,
				v -> this.insert_space_after_comma_in_record_components = v);
		final Object insertSpaceAfterCommaInSuperinterfacesOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_SUPERINTERFACES);
		if (insertSpaceAfterCommaInSuperinterfacesOption != null) {
			this.insert_space_after_comma_in_superinterfaces = JavaCore.INSERT.equals(insertSpaceAfterCommaInSuperinterfacesOption);
		}
		setBoolean(settings, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_SWITCH_CASE_EXPRESSIONS, JavaCore.INSERT,
				v -> this.insert_space_after_comma_in_switch_case_expressions = v);
		final Object insertSpaceAfterCommaInTypeArgumentsOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_TYPE_ARGUMENTS);
		if (insertSpaceAfterCommaInTypeArgumentsOption != null) {
			this.insert_space_after_comma_in_type_arguments = JavaCore.INSERT.equals(insertSpaceAfterCommaInTypeArgumentsOption);
		}
		final Object insertSpaceAfterCommaInTypeParametersOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_TYPE_PARAMETERS);
		if (insertSpaceAfterCommaInTypeParametersOption != null) {
			this.insert_space_after_comma_in_type_parameters = JavaCore.INSERT.equals(insertSpaceAfterCommaInTypeParametersOption);
		}
		final Object insertSpaceAfterEllipsisOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_ELLIPSIS);
		if (insertSpaceAfterEllipsisOption != null) {
			this.insert_space_after_ellipsis = JavaCore.INSERT.equals(insertSpaceAfterEllipsisOption);
		}
		final Object insertSpaceAfterLambdaArrowOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_LAMBDA_ARROW);
		if (insertSpaceAfterLambdaArrowOption != null) {
			this.insert_space_after_lambda_arrow = JavaCore.INSERT.equals(insertSpaceAfterLambdaArrowOption);
		}
		setBoolean(settings, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_NOT_OPERATOR, JavaCore.INSERT,
				v -> this.insert_space_after_not_operator = v);
		final Object insertSpaceAfterOpeningAngleBracketInParameterizedTypeReferenceOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_ANGLE_BRACKET_IN_PARAMETERIZED_TYPE_REFERENCE);
		if (insertSpaceAfterOpeningAngleBracketInParameterizedTypeReferenceOption != null) {
			this.insert_space_after_opening_angle_bracket_in_parameterized_type_reference = JavaCore.INSERT.equals(insertSpaceAfterOpeningAngleBracketInParameterizedTypeReferenceOption);
		}
		final Object insertSpaceAfterOpeningAngleBracketInTypeArgumentsOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_ANGLE_BRACKET_IN_TYPE_ARGUMENTS);
		if (insertSpaceAfterOpeningAngleBracketInTypeArgumentsOption != null) {
			this.insert_space_after_opening_angle_bracket_in_type_arguments = JavaCore.INSERT.equals(insertSpaceAfterOpeningAngleBracketInTypeArgumentsOption);
		}
		final Object insertSpaceAfterOpeningAngleBracketInTypeParametersOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_ANGLE_BRACKET_IN_TYPE_PARAMETERS);
		if (insertSpaceAfterOpeningAngleBracketInTypeParametersOption != null) {
			this.insert_space_after_opening_angle_bracket_in_type_parameters = JavaCore.INSERT.equals(insertSpaceAfterOpeningAngleBracketInTypeParametersOption);
		}
		final Object insertSpaceAfterOpeningBracketInArrayAllocationExpressionOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_BRACKET_IN_ARRAY_ALLOCATION_EXPRESSION);
		if (insertSpaceAfterOpeningBracketInArrayAllocationExpressionOption != null) {
			this.insert_space_after_opening_bracket_in_array_allocation_expression = JavaCore.INSERT.equals(insertSpaceAfterOpeningBracketInArrayAllocationExpressionOption);
		}
		final Object insertSpaceAfterOpeningBracketInArrayReferenceOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_BRACKET_IN_ARRAY_REFERENCE);
		if (insertSpaceAfterOpeningBracketInArrayReferenceOption != null) {
			this.insert_space_after_opening_bracket_in_array_reference = JavaCore.INSERT.equals(insertSpaceAfterOpeningBracketInArrayReferenceOption);
		}
		final Object insertSpaceAfterOpeningBraceInArrayInitializerOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_BRACE_IN_ARRAY_INITIALIZER);
		if (insertSpaceAfterOpeningBraceInArrayInitializerOption != null) {
			this.insert_space_after_opening_brace_in_array_initializer = JavaCore.INSERT.equals(insertSpaceAfterOpeningBraceInArrayInitializerOption);
		}
		final Object insertSpaceAfterOpeningParenInAnnotationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_ANNOTATION);
		if (insertSpaceAfterOpeningParenInAnnotationOption != null) {
			this.insert_space_after_opening_paren_in_annotation = JavaCore.INSERT.equals(insertSpaceAfterOpeningParenInAnnotationOption);
		}
		final Object insertSpaceAfterOpeningParenInCastOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_CAST);
		if (insertSpaceAfterOpeningParenInCastOption != null) {
			this.insert_space_after_opening_paren_in_cast = JavaCore.INSERT.equals(insertSpaceAfterOpeningParenInCastOption);
		}
		final Object insertSpaceAfterOpeningParenInCatchOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_CATCH);
		if (insertSpaceAfterOpeningParenInCatchOption != null) {
			this.insert_space_after_opening_paren_in_catch = JavaCore.INSERT.equals(insertSpaceAfterOpeningParenInCatchOption);
		}
		final Object insertSpaceAfterOpeningParenInConstructorDeclarationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_CONSTRUCTOR_DECLARATION);
		if (insertSpaceAfterOpeningParenInConstructorDeclarationOption != null) {
			this.insert_space_after_opening_paren_in_constructor_declaration = JavaCore.INSERT.equals(insertSpaceAfterOpeningParenInConstructorDeclarationOption);
		}
		final Object insertSpaceAfterOpeningParenInEnumConstantOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_ENUM_CONSTANT);
		if (insertSpaceAfterOpeningParenInEnumConstantOption != null) {
			this.insert_space_after_opening_paren_in_enum_constant = JavaCore.INSERT.equals(insertSpaceAfterOpeningParenInEnumConstantOption);
		}
		final Object insertSpaceAfterOpeningParenInForOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_FOR);
		if (insertSpaceAfterOpeningParenInForOption != null) {
			this.insert_space_after_opening_paren_in_for = JavaCore.INSERT.equals(insertSpaceAfterOpeningParenInForOption);
		}
		final Object insertSpaceAfterOpeningParenInIfOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_IF);
		if (insertSpaceAfterOpeningParenInIfOption != null) {
			this.insert_space_after_opening_paren_in_if = JavaCore.INSERT.equals(insertSpaceAfterOpeningParenInIfOption);
		}
		final Object insertSpaceAfterOpeningParenInMethodDeclarationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_METHOD_DECLARATION);
		if (insertSpaceAfterOpeningParenInMethodDeclarationOption != null) {
			this.insert_space_after_opening_paren_in_method_declaration = JavaCore.INSERT.equals(insertSpaceAfterOpeningParenInMethodDeclarationOption);
		}
		final Object insertSpaceAfterOpeningParenInMethodInvocationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_METHOD_INVOCATION);
		if (insertSpaceAfterOpeningParenInMethodInvocationOption != null) {
			this.insert_space_after_opening_paren_in_method_invocation = JavaCore.INSERT.equals(insertSpaceAfterOpeningParenInMethodInvocationOption);
		}
		final Object insertSpaceAfterOpeningParenInParenthesizedExpressionOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_PARENTHESIZED_EXPRESSION);
		if (insertSpaceAfterOpeningParenInParenthesizedExpressionOption != null) {
			this.insert_space_after_opening_paren_in_parenthesized_expression = JavaCore.INSERT.equals(insertSpaceAfterOpeningParenInParenthesizedExpressionOption);
		}
		setBoolean(settings, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_RECORD_DECLARATION, JavaCore.INSERT,
			v -> this.insert_space_after_opening_paren_in_record_declaration = v);
		final Object insertSpaceAfterOpeningParenInSwitchOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_SWITCH);
		if (insertSpaceAfterOpeningParenInSwitchOption != null) {
			this.insert_space_after_opening_paren_in_switch = JavaCore.INSERT.equals(insertSpaceAfterOpeningParenInSwitchOption);
		}
		final Object insertSpaceAfterOpeningParenInSynchronizedOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_SYNCHRONIZED);
		if (insertSpaceAfterOpeningParenInSynchronizedOption != null) {
			this.insert_space_after_opening_paren_in_synchronized = JavaCore.INSERT.equals(insertSpaceAfterOpeningParenInSynchronizedOption);
		}
		final Object insertSpaceAfterOpeningParenInTryOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_TRY);
		if (insertSpaceAfterOpeningParenInTryOption != null) {
			this.insert_space_after_opening_paren_in_try = JavaCore.INSERT.equals(insertSpaceAfterOpeningParenInTryOption);
		}
		final Object insertSpaceAfterOpeningParenInWhileOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_WHILE);
		if (insertSpaceAfterOpeningParenInWhileOption != null) {
			this.insert_space_after_opening_paren_in_while = JavaCore.INSERT.equals(insertSpaceAfterOpeningParenInWhileOption);
		}
		final Object insertSpaceAfterPostfixOperatorOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_POSTFIX_OPERATOR);
		if (insertSpaceAfterPostfixOperatorOption != null) {
			this.insert_space_after_postfix_operator = JavaCore.INSERT.equals(insertSpaceAfterPostfixOperatorOption);
		}
		final Object insertSpaceAfterPrefixOperatorOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_PREFIX_OPERATOR);
		if (insertSpaceAfterPrefixOperatorOption != null) {
			this.insert_space_after_prefix_operator = JavaCore.INSERT.equals(insertSpaceAfterPrefixOperatorOption);
		}
		final Object insertSpaceAfterQuestionInConditionalOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_QUESTION_IN_CONDITIONAL);
		if (insertSpaceAfterQuestionInConditionalOption != null) {
			this.insert_space_after_question_in_conditional = JavaCore.INSERT.equals(insertSpaceAfterQuestionInConditionalOption);
		}
		final Object insertSpaceAfterQuestionInWildcardOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_QUESTION_IN_WILDCARD);
		if (insertSpaceAfterQuestionInWildcardOption != null) {
			this.insert_space_after_question_in_wilcard = JavaCore.INSERT.equals(insertSpaceAfterQuestionInWildcardOption);
		}
		final Object insertSpaceAfterSemicolonInForOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_SEMICOLON_IN_FOR);
		if (insertSpaceAfterSemicolonInForOption != null) {
			this.insert_space_after_semicolon_in_for = JavaCore.INSERT.equals(insertSpaceAfterSemicolonInForOption);
		}
		final Object insertSpaceAfterSemicolonInTryOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_SEMICOLON_IN_TRY_RESOURCES);
		if (insertSpaceAfterSemicolonInTryOption != null) {
			this.insert_space_after_semicolon_in_try_resources = JavaCore.INSERT.equals(insertSpaceAfterSemicolonInTryOption);
		}
		final Object insertSpaceAfterUnaryOperatorOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_UNARY_OPERATOR);
		if (insertSpaceAfterUnaryOperatorOption != null) {
			this.insert_space_after_unary_operator = JavaCore.INSERT.equals(insertSpaceAfterUnaryOperatorOption);
		}
		final Object insertSpaceBeforeAndInWildcardOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_AND_IN_TYPE_PARAMETER);
		if (insertSpaceBeforeAndInWildcardOption != null) {
			this.insert_space_before_and_in_type_parameter = JavaCore.INSERT.equals(insertSpaceBeforeAndInWildcardOption);
		}
		setBoolean(settings, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_ARROW_IN_SWITCH_CASE, JavaCore.INSERT,
				v -> this.insert_space_before_arrow_in_switch_case = v);
		setBoolean(settings, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_ARROW_IN_SWITCH_DEFAULT, JavaCore.INSERT,
				v -> this.insert_space_before_arrow_in_switch_default = v);
		final Object insertSpaceBeforeAtInAnnotationTypeDeclarationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_AT_IN_ANNOTATION_TYPE_DECLARATION);
		if (insertSpaceBeforeAtInAnnotationTypeDeclarationOption != null) {
			this.insert_space_before_at_in_annotation_type_declaration = JavaCore.INSERT.equals(insertSpaceBeforeAtInAnnotationTypeDeclarationOption);
		}
		final Object insertSpaceBeforeAssignmentOperatorOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_ASSIGNMENT_OPERATOR);
		if (insertSpaceBeforeAssignmentOperatorOption != null) {
			this.insert_space_before_assignment_operator = JavaCore.INSERT.equals(insertSpaceBeforeAssignmentOperatorOption);
		}
		setBoolean(settings, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_MULTIPLICATIVE_OPERATOR, JavaCore.INSERT,
				v -> this.insert_space_before_multiplicative_operator = v);
		setBoolean(settings, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_ADDITIVE_OPERATOR, JavaCore.INSERT,
				v -> this.insert_space_before_additive_operator = v);
		setBoolean(settings, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_STRING_CONCATENATION, JavaCore.INSERT,
				v -> this.insert_space_before_string_concatenation = v);
		setBoolean(settings, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_SHIFT_OPERATOR, JavaCore.INSERT,
				v -> this.insert_space_before_shift_operator = v);
		setBoolean(settings, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_RELATIONAL_OPERATOR, JavaCore.INSERT,
				v -> this.insert_space_before_relational_operator = v);
		setBoolean(settings, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_BITWISE_OPERATOR, JavaCore.INSERT,
				v -> this.insert_space_before_bitwise_operator = v);
		setBoolean(settings, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_LOGICAL_OPERATOR, JavaCore.INSERT,
				v -> this.insert_space_before_logical_operator = v);
		final Object insertSpaceBeforeClosingAngleBracketInParameterizedTypeReferenceOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_ANGLE_BRACKET_IN_PARAMETERIZED_TYPE_REFERENCE);
		if (insertSpaceBeforeClosingAngleBracketInParameterizedTypeReferenceOption != null) {
			this.insert_space_before_closing_angle_bracket_in_parameterized_type_reference = JavaCore.INSERT.equals(insertSpaceBeforeClosingAngleBracketInParameterizedTypeReferenceOption);
		}
		final Object insertSpaceBeforeClosingAngleBracketInTypeArgumentsOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_ANGLE_BRACKET_IN_TYPE_ARGUMENTS);
		if (insertSpaceBeforeClosingAngleBracketInTypeArgumentsOption != null) {
			this.insert_space_before_closing_angle_bracket_in_type_arguments = JavaCore.INSERT.equals(insertSpaceBeforeClosingAngleBracketInTypeArgumentsOption);
		}
		final Object insertSpaceBeforeClosingAngleBracketInTypeParametersOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_ANGLE_BRACKET_IN_TYPE_PARAMETERS);
		if (insertSpaceBeforeClosingAngleBracketInTypeParametersOption != null) {
			this.insert_space_before_closing_angle_bracket_in_type_parameters = JavaCore.INSERT.equals(insertSpaceBeforeClosingAngleBracketInTypeParametersOption);
		}
		final Object insertSpaceBeforeClosingBraceInArrayInitializerOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_BRACE_IN_ARRAY_INITIALIZER);
		if (insertSpaceBeforeClosingBraceInArrayInitializerOption != null) {
			this.insert_space_before_closing_brace_in_array_initializer = JavaCore.INSERT.equals(insertSpaceBeforeClosingBraceInArrayInitializerOption);
		}
		final Object insertSpaceBeforeClosingBracketInArrayAllocationExpressionOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_BRACKET_IN_ARRAY_ALLOCATION_EXPRESSION);
		if (insertSpaceBeforeClosingBracketInArrayAllocationExpressionOption != null) {
			this.insert_space_before_closing_bracket_in_array_allocation_expression = JavaCore.INSERT.equals(insertSpaceBeforeClosingBracketInArrayAllocationExpressionOption);
		}
		final Object insertSpaceBeforeClosingBracketInArrayReferenceOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_BRACKET_IN_ARRAY_REFERENCE);
		if (insertSpaceBeforeClosingBracketInArrayReferenceOption != null) {
			this.insert_space_before_closing_bracket_in_array_reference = JavaCore.INSERT.equals(insertSpaceBeforeClosingBracketInArrayReferenceOption);
		}
		final Object insertSpaceBeforeClosingParenInAnnotationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_ANNOTATION);
		if (insertSpaceBeforeClosingParenInAnnotationOption != null) {
			this.insert_space_before_closing_paren_in_annotation = JavaCore.INSERT.equals(insertSpaceBeforeClosingParenInAnnotationOption);
		}
		final Object insertSpaceBeforeClosingParenInCastOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_CAST);
		if (insertSpaceBeforeClosingParenInCastOption != null) {
			this.insert_space_before_closing_paren_in_cast = JavaCore.INSERT.equals(insertSpaceBeforeClosingParenInCastOption);
		}
		final Object insertSpaceBeforeClosingParenInCatchOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_CATCH);
		if (insertSpaceBeforeClosingParenInCatchOption != null) {
			this.insert_space_before_closing_paren_in_catch = JavaCore.INSERT.equals(insertSpaceBeforeClosingParenInCatchOption);
		}
		final Object insertSpaceBeforeClosingParenInConstructorDeclarationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_CONSTRUCTOR_DECLARATION);
		if (insertSpaceBeforeClosingParenInConstructorDeclarationOption != null) {
			this.insert_space_before_closing_paren_in_constructor_declaration = JavaCore.INSERT.equals(insertSpaceBeforeClosingParenInConstructorDeclarationOption);
		}
		final Object insertSpaceBeforeClosingParenInEnumConstantOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_ENUM_CONSTANT);
		if (insertSpaceBeforeClosingParenInEnumConstantOption != null) {
			this.insert_space_before_closing_paren_in_enum_constant = JavaCore.INSERT.equals(insertSpaceBeforeClosingParenInEnumConstantOption);
		}
		final Object insertSpaceBeforeClosingParenInForOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_FOR);
		if (insertSpaceBeforeClosingParenInForOption != null) {
			this.insert_space_before_closing_paren_in_for = JavaCore.INSERT.equals(insertSpaceBeforeClosingParenInForOption);
		}
		final Object insertSpaceBeforeClosingParenInIfOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_IF);
		if (insertSpaceBeforeClosingParenInIfOption != null) {
			this.insert_space_before_closing_paren_in_if = JavaCore.INSERT.equals(insertSpaceBeforeClosingParenInIfOption);
		}
		final Object insertSpaceBeforeClosingParenInMethodDeclarationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_METHOD_DECLARATION);
		if (insertSpaceBeforeClosingParenInMethodDeclarationOption != null) {
			this.insert_space_before_closing_paren_in_method_declaration = JavaCore.INSERT.equals(insertSpaceBeforeClosingParenInMethodDeclarationOption);
		}
		final Object insertSpaceBeforeClosingParenInMethodInvocationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_METHOD_INVOCATION);
		if (insertSpaceBeforeClosingParenInMethodInvocationOption != null) {
			this.insert_space_before_closing_paren_in_method_invocation = JavaCore.INSERT.equals(insertSpaceBeforeClosingParenInMethodInvocationOption);
		}
		final Object insertSpaceBeforeClosingParenInParenthesizedExpressionOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_PARENTHESIZED_EXPRESSION);
		if (insertSpaceBeforeClosingParenInParenthesizedExpressionOption != null) {
			this.insert_space_before_closing_paren_in_parenthesized_expression = JavaCore.INSERT.equals(insertSpaceBeforeClosingParenInParenthesizedExpressionOption);
		}
		setBoolean(settings, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_RECORD_DECLARATION, JavaCore.INSERT,
			v -> this.insert_space_before_closing_paren_in_record_declaration = v);
		final Object insertSpaceBeforeClosingParenInSwitchOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_SWITCH);
		if (insertSpaceBeforeClosingParenInSwitchOption != null) {
			this.insert_space_before_closing_paren_in_switch = JavaCore.INSERT.equals(insertSpaceBeforeClosingParenInSwitchOption);
		}
		final Object insertSpaceBeforeClosingParenInSynchronizedOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_SYNCHRONIZED);
		if (insertSpaceBeforeClosingParenInSynchronizedOption != null) {
			this.insert_space_before_closing_paren_in_synchronized = JavaCore.INSERT.equals(insertSpaceBeforeClosingParenInSynchronizedOption);
		}
		final Object insertSpaceBeforeClosingParenInTryOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_TRY);
		if (insertSpaceBeforeClosingParenInTryOption != null) {
			this.insert_space_before_closing_paren_in_try = JavaCore.INSERT.equals(insertSpaceBeforeClosingParenInTryOption);
		}
		final Object insertSpaceBeforeClosingParenInWhileOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_WHILE);
		if (insertSpaceBeforeClosingParenInWhileOption != null) {
			this.insert_space_before_closing_paren_in_while = JavaCore.INSERT.equals(insertSpaceBeforeClosingParenInWhileOption);
		}
		final Object insertSpaceBeforeColonInAssertOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COLON_IN_ASSERT);
		if (insertSpaceBeforeColonInAssertOption != null) {
			this.insert_space_before_colon_in_assert = JavaCore.INSERT.equals(insertSpaceBeforeColonInAssertOption);
		}
		final Object insertSpaceBeforeColonInCaseOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COLON_IN_CASE);
		if (insertSpaceBeforeColonInCaseOption != null) {
			this.insert_space_before_colon_in_case = JavaCore.INSERT.equals(insertSpaceBeforeColonInCaseOption);
		}
		final Object insertSpaceBeforeColonInConditionalOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COLON_IN_CONDITIONAL);
		if (insertSpaceBeforeColonInConditionalOption != null) {
			this.insert_space_before_colon_in_conditional = JavaCore.INSERT.equals(insertSpaceBeforeColonInConditionalOption);
		}
		final Object insertSpaceBeforeColonInDefaultOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COLON_IN_DEFAULT);
		if (insertSpaceBeforeColonInDefaultOption != null) {
			this.insert_space_before_colon_in_default = JavaCore.INSERT.equals(insertSpaceBeforeColonInDefaultOption);
		}
		final Object insertSpaceBeforeColonInForOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COLON_IN_FOR);
		if (insertSpaceBeforeColonInForOption != null) {
			this.insert_space_before_colon_in_for = JavaCore.INSERT.equals(insertSpaceBeforeColonInForOption);
		}
		final Object insertSpaceBeforeColonInLabeledStatementOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COLON_IN_LABELED_STATEMENT);
		if (insertSpaceBeforeColonInLabeledStatementOption != null) {
			this.insert_space_before_colon_in_labeled_statement = JavaCore.INSERT.equals(insertSpaceBeforeColonInLabeledStatementOption);
		}
		final Object insertSpaceBeforeCommaInAllocationExpressionOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_ALLOCATION_EXPRESSION);
		if (insertSpaceBeforeCommaInAllocationExpressionOption != null) {
			this.insert_space_before_comma_in_allocation_expression = JavaCore.INSERT.equals(insertSpaceBeforeCommaInAllocationExpressionOption);
		}
		final Object insertSpaceBeforeCommaInAnnotationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_ANNOTATION);
		if (insertSpaceBeforeCommaInAnnotationOption != null) {
			this.insert_space_before_comma_in_annotation = JavaCore.INSERT.equals(insertSpaceBeforeCommaInAnnotationOption);
		}
		final Object insertSpaceBeforeCommaInArrayInitializerOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_ARRAY_INITIALIZER);
		if (insertSpaceBeforeCommaInArrayInitializerOption != null) {
			this.insert_space_before_comma_in_array_initializer = JavaCore.INSERT.equals(insertSpaceBeforeCommaInArrayInitializerOption);
		}
		final Object insertSpaceBeforeCommaInConstructorDeclarationParametersOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_CONSTRUCTOR_DECLARATION_PARAMETERS);
		if (insertSpaceBeforeCommaInConstructorDeclarationParametersOption != null) {
			this.insert_space_before_comma_in_constructor_declaration_parameters = JavaCore.INSERT.equals(insertSpaceBeforeCommaInConstructorDeclarationParametersOption);
		}
		final Object insertSpaceBeforeCommaInConstructorDeclarationThrowsOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_CONSTRUCTOR_DECLARATION_THROWS);
		if (insertSpaceBeforeCommaInConstructorDeclarationThrowsOption != null) {
			this.insert_space_before_comma_in_constructor_declaration_throws = JavaCore.INSERT.equals(insertSpaceBeforeCommaInConstructorDeclarationThrowsOption);
		}
		final Object insertSpaceBeforeCommaInEnumConstantArgumentsOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_ENUM_CONSTANT_ARGUMENTS);
		if (insertSpaceBeforeCommaInEnumConstantArgumentsOption != null) {
			this.insert_space_before_comma_in_enum_constant_arguments = JavaCore.INSERT.equals(insertSpaceBeforeCommaInEnumConstantArgumentsOption);
		}
		final Object insertSpaceBeforeCommaInEnumDeclarationsOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_ENUM_DECLARATIONS);
		if (insertSpaceBeforeCommaInEnumDeclarationsOption != null) {
			this.insert_space_before_comma_in_enum_declarations = JavaCore.INSERT.equals(insertSpaceBeforeCommaInEnumDeclarationsOption);
		}
		final Object insertSpaceBeforeCommaInExplicitConstructorCallArgumentsOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_EXPLICIT_CONSTRUCTOR_CALL_ARGUMENTS);
		if (insertSpaceBeforeCommaInExplicitConstructorCallArgumentsOption != null) {
			this.insert_space_before_comma_in_explicit_constructor_call_arguments = JavaCore.INSERT.equals(insertSpaceBeforeCommaInExplicitConstructorCallArgumentsOption);
		}
		final Object insertSpaceBeforeCommaInForIncrementsOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_FOR_INCREMENTS);
		if (insertSpaceBeforeCommaInForIncrementsOption != null) {
			this.insert_space_before_comma_in_for_increments = JavaCore.INSERT.equals(insertSpaceBeforeCommaInForIncrementsOption);
		}
		final Object insertSpaceBeforeCommaInForInitsOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_FOR_INITS);
		if (insertSpaceBeforeCommaInForInitsOption != null) {
			this.insert_space_before_comma_in_for_inits = JavaCore.INSERT.equals(insertSpaceBeforeCommaInForInitsOption);
		}
		final Object insertSpaceBeforeCommaInMethodInvocationArgumentsOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_METHOD_INVOCATION_ARGUMENTS);
		if (insertSpaceBeforeCommaInMethodInvocationArgumentsOption != null) {
			this.insert_space_before_comma_in_method_invocation_arguments = JavaCore.INSERT.equals(insertSpaceBeforeCommaInMethodInvocationArgumentsOption);
		}
		final Object insertSpaceBeforeCommaInMethodDeclarationParametersOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_METHOD_DECLARATION_PARAMETERS);
		if (insertSpaceBeforeCommaInMethodDeclarationParametersOption != null) {
			this.insert_space_before_comma_in_method_declaration_parameters = JavaCore.INSERT.equals(insertSpaceBeforeCommaInMethodDeclarationParametersOption);
		}
		final Object insertSpaceBeforeCommaInMethodDeclarationThrowsOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_METHOD_DECLARATION_THROWS);
		if (insertSpaceBeforeCommaInMethodDeclarationThrowsOption != null) {
			this.insert_space_before_comma_in_method_declaration_throws = JavaCore.INSERT.equals(insertSpaceBeforeCommaInMethodDeclarationThrowsOption);
		}
		final Object insertSpaceBeforeCommaInMultipleFieldDeclarationsOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_MULTIPLE_FIELD_DECLARATIONS);
		if (insertSpaceBeforeCommaInMultipleFieldDeclarationsOption != null) {
			this.insert_space_before_comma_in_multiple_field_declarations = JavaCore.INSERT.equals(insertSpaceBeforeCommaInMultipleFieldDeclarationsOption);
		}
		final Object insertSpaceBeforeCommaInMultipleLocalDeclarationsOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_MULTIPLE_LOCAL_DECLARATIONS);
		if (insertSpaceBeforeCommaInMultipleLocalDeclarationsOption != null) {
			this.insert_space_before_comma_in_multiple_local_declarations = JavaCore.INSERT.equals(insertSpaceBeforeCommaInMultipleLocalDeclarationsOption);
		}
		final Object insertSpaceBeforeCommaInParameterizedTypeReferenceOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_PARAMETERIZED_TYPE_REFERENCE);
		if (insertSpaceBeforeCommaInParameterizedTypeReferenceOption != null) {
			this.insert_space_before_comma_in_parameterized_type_reference = JavaCore.INSERT.equals(insertSpaceBeforeCommaInParameterizedTypeReferenceOption);
		}
		setBoolean(settings, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_PERMITTED_TYPES, JavaCore.INSERT,
				v -> this.insert_space_before_comma_in_permitted_types = v);
		final Object insertSpaceBeforeCommaInSuperinterfacesOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_SUPERINTERFACES);
		setBoolean(settings, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_RECORD_COMPONENTS, JavaCore.INSERT,
				v -> this.insert_space_before_comma_in_record_components = v);
		if (insertSpaceBeforeCommaInSuperinterfacesOption != null) {
			this.insert_space_before_comma_in_superinterfaces = JavaCore.INSERT.equals(insertSpaceBeforeCommaInSuperinterfacesOption);
		}
		setBoolean(settings, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_SWITCH_CASE_EXPRESSIONS, JavaCore.INSERT,
				v -> this.insert_space_before_comma_in_switch_case_expressions = v);
		final Object insertSpaceBeforeCommaInTypeArgumentsOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_TYPE_ARGUMENTS);
		if (insertSpaceBeforeCommaInTypeArgumentsOption != null) {
			this.insert_space_before_comma_in_type_arguments = JavaCore.INSERT.equals(insertSpaceBeforeCommaInTypeArgumentsOption);
		}
		final Object insertSpaceBeforeCommaInTypeParametersOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_TYPE_PARAMETERS);
		if (insertSpaceBeforeCommaInTypeParametersOption != null) {
			this.insert_space_before_comma_in_type_parameters = JavaCore.INSERT.equals(insertSpaceBeforeCommaInTypeParametersOption);
		}
		final Object insertSpaceBeforeEllipsisOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_ELLIPSIS);
		if (insertSpaceBeforeEllipsisOption != null) {
			this.insert_space_before_ellipsis = JavaCore.INSERT.equals(insertSpaceBeforeEllipsisOption);
		}
		final Object insertSpaceBeforeLambdaArrowOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_LAMBDA_ARROW);
		if (insertSpaceBeforeLambdaArrowOption != null) {
			this.insert_space_before_lambda_arrow = JavaCore.INSERT.equals(insertSpaceBeforeLambdaArrowOption);
		}
		final Object insertSpaceBeforeOpeningAngleBrackerInParameterizedTypeReferenceOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_ANGLE_BRACKET_IN_PARAMETERIZED_TYPE_REFERENCE);
		if (insertSpaceBeforeOpeningAngleBrackerInParameterizedTypeReferenceOption != null) {
			this.insert_space_before_opening_angle_bracket_in_parameterized_type_reference = JavaCore.INSERT.equals(insertSpaceBeforeOpeningAngleBrackerInParameterizedTypeReferenceOption);
		}
		final Object insertSpaceBeforeOpeningAngleBrackerInTypeArgumentsOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_ANGLE_BRACKET_IN_TYPE_ARGUMENTS);
		if (insertSpaceBeforeOpeningAngleBrackerInTypeArgumentsOption != null) {
			this.insert_space_before_opening_angle_bracket_in_type_arguments = JavaCore.INSERT.equals(insertSpaceBeforeOpeningAngleBrackerInTypeArgumentsOption);
		}
		final Object insertSpaceBeforeOpeningAngleBrackerInTypeParametersOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_ANGLE_BRACKET_IN_TYPE_PARAMETERS);
		if (insertSpaceBeforeOpeningAngleBrackerInTypeParametersOption != null) {
			this.insert_space_before_opening_angle_bracket_in_type_parameters = JavaCore.INSERT.equals(insertSpaceBeforeOpeningAngleBrackerInTypeParametersOption);
		}
		final Object insertSpaceBeforeOpeningBraceInAnnotationTypeDeclarationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_ANNOTATION_TYPE_DECLARATION);
		if (insertSpaceBeforeOpeningBraceInAnnotationTypeDeclarationOption != null) {
			this.insert_space_before_opening_brace_in_annotation_type_declaration = JavaCore.INSERT.equals(insertSpaceBeforeOpeningBraceInAnnotationTypeDeclarationOption);
		}
		final Object insertSpaceBeforeOpeningBraceInAnonymousTypeDeclarationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_ANONYMOUS_TYPE_DECLARATION);
		if (insertSpaceBeforeOpeningBraceInAnonymousTypeDeclarationOption != null) {
			this.insert_space_before_opening_brace_in_anonymous_type_declaration = JavaCore.INSERT.equals(insertSpaceBeforeOpeningBraceInAnonymousTypeDeclarationOption);
		}
		final Object insertSpaceBeforeOpeningBraceInArrayInitializerOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_ARRAY_INITIALIZER);
		if (insertSpaceBeforeOpeningBraceInArrayInitializerOption != null) {
			this.insert_space_before_opening_brace_in_array_initializer = JavaCore.INSERT.equals(insertSpaceBeforeOpeningBraceInArrayInitializerOption);
		}
		final Object insertSpaceBeforeOpeningBraceInBlockOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_BLOCK);
		if (insertSpaceBeforeOpeningBraceInBlockOption != null) {
			this.insert_space_before_opening_brace_in_block = JavaCore.INSERT.equals(insertSpaceBeforeOpeningBraceInBlockOption);
		}
		final Object insertSpaceBeforeOpeningBraceInConstructorDeclarationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_CONSTRUCTOR_DECLARATION);
		if (insertSpaceBeforeOpeningBraceInConstructorDeclarationOption != null) {
			this.insert_space_before_opening_brace_in_constructor_declaration = JavaCore.INSERT.equals(insertSpaceBeforeOpeningBraceInConstructorDeclarationOption);
		}
		final Object insertSpaceBeforeOpeningBraceInEnumDeclarationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_ENUM_DECLARATION);
		if (insertSpaceBeforeOpeningBraceInEnumDeclarationOption != null) {
			this.insert_space_before_opening_brace_in_enum_declaration = JavaCore.INSERT.equals(insertSpaceBeforeOpeningBraceInEnumDeclarationOption);
		}
		final Object insertSpaceBeforeOpeningBraceInEnumConstantOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_ENUM_CONSTANT);
		if (insertSpaceBeforeOpeningBraceInEnumConstantOption != null) {
			this.insert_space_before_opening_brace_in_enum_constant = JavaCore.INSERT.equals(insertSpaceBeforeOpeningBraceInEnumConstantOption);
		}
		final Object insertSpaceBeforeOpeningBraceInMethodDeclarationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_METHOD_DECLARATION);
		if (insertSpaceBeforeOpeningBraceInMethodDeclarationOption != null) {
			this.insert_space_before_opening_brace_in_method_declaration = JavaCore.INSERT.equals(insertSpaceBeforeOpeningBraceInMethodDeclarationOption);
		}
		setBoolean(settings, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_RECORD_CONSTRUCTOR, JavaCore.INSERT,
				v -> this.insert_space_before_opening_brace_in_record_constructor = v);
		setBoolean(settings, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_RECORD_DECLARATION, JavaCore.INSERT,
			v -> this.insert_space_before_opening_brace_in_record_declaration = v);
		final Object insertSpaceBeforeOpeningBraceInTypeDeclarationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_TYPE_DECLARATION);
		if (insertSpaceBeforeOpeningBraceInTypeDeclarationOption != null) {
			this.insert_space_before_opening_brace_in_type_declaration = JavaCore.INSERT.equals(insertSpaceBeforeOpeningBraceInTypeDeclarationOption);
		}
		final Object insertSpaceBeforeOpeningBracketInArrayAllocationExpressionOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACKET_IN_ARRAY_ALLOCATION_EXPRESSION);
		if (insertSpaceBeforeOpeningBracketInArrayAllocationExpressionOption != null) {
			this.insert_space_before_opening_bracket_in_array_allocation_expression = JavaCore.INSERT.equals(insertSpaceBeforeOpeningBracketInArrayAllocationExpressionOption);
		}
		final Object insertSpaceBeforeOpeningBracketInArrayReferenceOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACKET_IN_ARRAY_REFERENCE);
		if (insertSpaceBeforeOpeningBracketInArrayReferenceOption != null) {
			this.insert_space_before_opening_bracket_in_array_reference = JavaCore.INSERT.equals(insertSpaceBeforeOpeningBracketInArrayReferenceOption);
		}
		final Object insertSpaceBeforeOpeningBracketInArrayTypeReferenceOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACKET_IN_ARRAY_TYPE_REFERENCE);
		if (insertSpaceBeforeOpeningBracketInArrayTypeReferenceOption != null) {
			this.insert_space_before_opening_bracket_in_array_type_reference = JavaCore.INSERT.equals(insertSpaceBeforeOpeningBracketInArrayTypeReferenceOption);
		}
		final Object insertSpaceBeforeOpeningParenInAnnotationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_ANNOTATION);
		if (insertSpaceBeforeOpeningParenInAnnotationOption != null) {
			this.insert_space_before_opening_paren_in_annotation = JavaCore.INSERT.equals(insertSpaceBeforeOpeningParenInAnnotationOption);
		}
		final Object insertSpaceBeforeOpeningParenInAnnotationTypeMemberDeclarationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_ANNOTATION_TYPE_MEMBER_DECLARATION);
		if (insertSpaceBeforeOpeningParenInAnnotationTypeMemberDeclarationOption != null) {
			this.insert_space_before_opening_paren_in_annotation_type_member_declaration = JavaCore.INSERT.equals(insertSpaceBeforeOpeningParenInAnnotationTypeMemberDeclarationOption);
		}
		final Object insertSpaceBeforeOpeningParenInCatchOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_CATCH);
		if (insertSpaceBeforeOpeningParenInCatchOption != null) {
			this.insert_space_before_opening_paren_in_catch = JavaCore.INSERT.equals(insertSpaceBeforeOpeningParenInCatchOption);
		}
		final Object insertSpaceBeforeOpeningParenInConstructorDeclarationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_CONSTRUCTOR_DECLARATION);
		if (insertSpaceBeforeOpeningParenInConstructorDeclarationOption != null) {
			this.insert_space_before_opening_paren_in_constructor_declaration = JavaCore.INSERT.equals(insertSpaceBeforeOpeningParenInConstructorDeclarationOption);
		}
		final Object insertSpaceBeforeOpeningParenInEnumConstantOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_ENUM_CONSTANT);
		if (insertSpaceBeforeOpeningParenInEnumConstantOption != null) {
			this.insert_space_before_opening_paren_in_enum_constant = JavaCore.INSERT.equals(insertSpaceBeforeOpeningParenInEnumConstantOption);
		}
		final Object insertSpaceBeforeOpeningParenInForOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_FOR);
		if (insertSpaceBeforeOpeningParenInForOption != null) {
			this.insert_space_before_opening_paren_in_for = JavaCore.INSERT.equals(insertSpaceBeforeOpeningParenInForOption);
		}
		final Object insertSpaceBeforeOpeningParenInIfOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_IF);
		if (insertSpaceBeforeOpeningParenInIfOption != null) {
			this.insert_space_before_opening_paren_in_if = JavaCore.INSERT.equals(insertSpaceBeforeOpeningParenInIfOption);
		}
		final Object insertSpaceBeforeOpeningParenInMethodInvocationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_METHOD_INVOCATION);
		if (insertSpaceBeforeOpeningParenInMethodInvocationOption != null) {
			this.insert_space_before_opening_paren_in_method_invocation = JavaCore.INSERT.equals(insertSpaceBeforeOpeningParenInMethodInvocationOption);
		}
		final Object insertSpaceBeforeOpeningParenInMethodDeclarationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_METHOD_DECLARATION);
		if (insertSpaceBeforeOpeningParenInMethodDeclarationOption != null) {
			this.insert_space_before_opening_paren_in_method_declaration = JavaCore.INSERT.equals(insertSpaceBeforeOpeningParenInMethodDeclarationOption);
		}
		setBoolean(settings, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_RECORD_DECLARATION, JavaCore.INSERT,
				v -> this.insert_space_before_opening_paren_in_record_declaration = v);
		final Object insertSpaceBeforeOpeningParenInSwitchOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_SWITCH);
		if (insertSpaceBeforeOpeningParenInSwitchOption != null) {
			this.insert_space_before_opening_paren_in_switch = JavaCore.INSERT.equals(insertSpaceBeforeOpeningParenInSwitchOption);
		}
		final Object insertSpaceBeforeOpeningBraceInSwitchOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_SWITCH);
		if (insertSpaceBeforeOpeningBraceInSwitchOption != null) {
			this.insert_space_before_opening_brace_in_switch = JavaCore.INSERT.equals(insertSpaceBeforeOpeningBraceInSwitchOption);
		}
		final Object insertSpaceBeforeOpeningParenInSynchronizedOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_SYNCHRONIZED);
		if (insertSpaceBeforeOpeningParenInSynchronizedOption != null) {
			this.insert_space_before_opening_paren_in_synchronized = JavaCore.INSERT.equals(insertSpaceBeforeOpeningParenInSynchronizedOption);
		}
		final Object insertSpaceBeforeOpeningParenInTryOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_TRY);
		if (insertSpaceBeforeOpeningParenInTryOption != null) {
			this.insert_space_before_opening_paren_in_try = JavaCore.INSERT.equals(insertSpaceBeforeOpeningParenInTryOption);
		}
		final Object insertSpaceBeforeOpeningParenInParenthesizedExpressionOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_PARENTHESIZED_EXPRESSION);
		if (insertSpaceBeforeOpeningParenInParenthesizedExpressionOption != null) {
			this.insert_space_before_opening_paren_in_parenthesized_expression = JavaCore.INSERT.equals(insertSpaceBeforeOpeningParenInParenthesizedExpressionOption);
		}
		final Object insertSpaceBeforeOpeningParenInWhileOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_WHILE);
		if (insertSpaceBeforeOpeningParenInWhileOption != null) {
			this.insert_space_before_opening_paren_in_while = JavaCore.INSERT.equals(insertSpaceBeforeOpeningParenInWhileOption);
		}
		final Object insertSpaceBeforeParenthesizedExpressionInReturnOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_PARENTHESIZED_EXPRESSION_IN_RETURN);
		if (insertSpaceBeforeParenthesizedExpressionInReturnOption != null) {
			this.insert_space_before_parenthesized_expression_in_return = JavaCore.INSERT.equals(insertSpaceBeforeParenthesizedExpressionInReturnOption);
		}
		final Object insertSpaceBeforeParenthesizedExpressionInThrowOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_PARENTHESIZED_EXPRESSION_IN_THROW);
		if (insertSpaceBeforeParenthesizedExpressionInThrowOption != null) {
			this.insert_space_before_parenthesized_expression_in_throw = JavaCore.INSERT.equals(insertSpaceBeforeParenthesizedExpressionInThrowOption);
		}
		final Object insertSpaceBeforePostfixOperatorOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_POSTFIX_OPERATOR);
		if (insertSpaceBeforePostfixOperatorOption != null) {
			this.insert_space_before_postfix_operator = JavaCore.INSERT.equals(insertSpaceBeforePostfixOperatorOption);
		}
		final Object insertSpaceBeforePrefixOperatorOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_PREFIX_OPERATOR);
		if (insertSpaceBeforePrefixOperatorOption != null) {
			this.insert_space_before_prefix_operator = JavaCore.INSERT.equals(insertSpaceBeforePrefixOperatorOption);
		}
		final Object insertSpaceBeforeQuestionInConditionalOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_QUESTION_IN_CONDITIONAL);
		if (insertSpaceBeforeQuestionInConditionalOption != null) {
			this.insert_space_before_question_in_conditional = JavaCore.INSERT.equals(insertSpaceBeforeQuestionInConditionalOption);
		}
		final Object insertSpaceBeforeQuestionInWildcardOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_QUESTION_IN_WILDCARD);
		if (insertSpaceBeforeQuestionInWildcardOption != null) {
			this.insert_space_before_question_in_wilcard = JavaCore.INSERT.equals(insertSpaceBeforeQuestionInWildcardOption);
		}
		final Object insertSpaceBeforeSemicolonOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_SEMICOLON);
		if (insertSpaceBeforeSemicolonOption != null) {
			this.insert_space_before_semicolon = JavaCore.INSERT.equals(insertSpaceBeforeSemicolonOption);
		}
		final Object insertSpaceBeforeSemicolonInForOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_SEMICOLON_IN_FOR);
		if (insertSpaceBeforeSemicolonInForOption != null) {
			this.insert_space_before_semicolon_in_for = JavaCore.INSERT.equals(insertSpaceBeforeSemicolonInForOption);
		}
		final Object insertSpaceBeforeSemicolonInTryOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_SEMICOLON_IN_TRY_RESOURCES);
		if (insertSpaceBeforeSemicolonInTryOption != null) {
			this.insert_space_before_semicolon_in_try_resources = JavaCore.INSERT.equals(insertSpaceBeforeSemicolonInTryOption);
		}
		final Object insertSpaceBeforeUnaryOperatorOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_UNARY_OPERATOR);
		if (insertSpaceBeforeUnaryOperatorOption != null) {
			this.insert_space_before_unary_operator = JavaCore.INSERT.equals(insertSpaceBeforeUnaryOperatorOption);
		}
		final Object insertSpaceBetweenBracketsInArrayTypeReferenceOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_BRACKETS_IN_ARRAY_TYPE_REFERENCE);
		if (insertSpaceBetweenBracketsInArrayTypeReferenceOption != null) {
			this.insert_space_between_brackets_in_array_type_reference = JavaCore.INSERT.equals(insertSpaceBetweenBracketsInArrayTypeReferenceOption);
		}
		final Object insertSpaceBetweenEmptyBracesInArrayInitializerOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_BRACES_IN_ARRAY_INITIALIZER);
		if (insertSpaceBetweenEmptyBracesInArrayInitializerOption != null) {
			this.insert_space_between_empty_braces_in_array_initializer = JavaCore.INSERT.equals(insertSpaceBetweenEmptyBracesInArrayInitializerOption);
		}
		final Object insertSpaceBetweenEmptyBracketsInArrayAllocationExpressionOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_BRACKETS_IN_ARRAY_ALLOCATION_EXPRESSION);
		if (insertSpaceBetweenEmptyBracketsInArrayAllocationExpressionOption != null) {
			this.insert_space_between_empty_brackets_in_array_allocation_expression = JavaCore.INSERT.equals(insertSpaceBetweenEmptyBracketsInArrayAllocationExpressionOption);
		}
		final Object insertSpaceBetweenEmptyParensInConstructorDeclarationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_PARENS_IN_CONSTRUCTOR_DECLARATION);
		if (insertSpaceBetweenEmptyParensInConstructorDeclarationOption != null) {
			this.insert_space_between_empty_parens_in_constructor_declaration = JavaCore.INSERT.equals(insertSpaceBetweenEmptyParensInConstructorDeclarationOption);
		}
		final Object insertSpaceBetweenEmptyParensInAnnotationTypeMemberDeclarationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_PARENS_IN_ANNOTATION_TYPE_MEMBER_DECLARATION);
		if (insertSpaceBetweenEmptyParensInAnnotationTypeMemberDeclarationOption != null) {
			this.insert_space_between_empty_parens_in_annotation_type_member_declaration = JavaCore.INSERT.equals(insertSpaceBetweenEmptyParensInAnnotationTypeMemberDeclarationOption);
		}
		final Object insertSpaceBetweenEmptyParensInEnumConstantOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_PARENS_IN_ENUM_CONSTANT);
		if (insertSpaceBetweenEmptyParensInEnumConstantOption != null) {
			this.insert_space_between_empty_parens_in_enum_constant = JavaCore.INSERT.equals(insertSpaceBetweenEmptyParensInEnumConstantOption);
		}
		final Object insertSpaceBetweenEmptyParensInMethodDeclarationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_PARENS_IN_METHOD_DECLARATION);
		if (insertSpaceBetweenEmptyParensInMethodDeclarationOption != null) {
			this.insert_space_between_empty_parens_in_method_declaration = JavaCore.INSERT.equals(insertSpaceBetweenEmptyParensInMethodDeclarationOption);
		}
		final Object insertSpaceBetweenEmptyParensInMethodInvocationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_PARENS_IN_METHOD_INVOCATION);
		if (insertSpaceBetweenEmptyParensInMethodInvocationOption != null) {
			this.insert_space_between_empty_parens_in_method_invocation = JavaCore.INSERT.equals(insertSpaceBetweenEmptyParensInMethodInvocationOption);
		}
		final Object compactElseIfOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_COMPACT_ELSE_IF);
		if (compactElseIfOption != null) {
			this.compact_else_if = DefaultCodeFormatterConstants.TRUE.equals(compactElseIfOption);
		}
		final Object keepGuardianClauseOnOneLineOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_KEEP_GUARDIAN_CLAUSE_ON_ONE_LINE);
		if (keepGuardianClauseOnOneLineOption != null) {
			this.keep_guardian_clause_on_one_line = DefaultCodeFormatterConstants.TRUE.equals(keepGuardianClauseOnOneLineOption);
		}
		final Object keepElseStatementOnSameLineOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_KEEP_ELSE_STATEMENT_ON_SAME_LINE);
		if (keepElseStatementOnSameLineOption != null) {
			this.keep_else_statement_on_same_line = DefaultCodeFormatterConstants.TRUE.equals(keepElseStatementOnSameLineOption);
		}
		final Object keepEmptyArrayInitializerOnOneLineOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_KEEP_EMPTY_ARRAY_INITIALIZER_ON_ONE_LINE);
		if (keepEmptyArrayInitializerOnOneLineOption != null) {
			this.keep_empty_array_initializer_on_one_line = DefaultCodeFormatterConstants.TRUE.equals(keepEmptyArrayInitializerOnOneLineOption);
		}
		final Object keepSimpleIfOnOneLineOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_KEEP_SIMPLE_IF_ON_ONE_LINE);
		if (keepSimpleIfOnOneLineOption != null) {
			this.keep_simple_if_on_one_line = DefaultCodeFormatterConstants.TRUE.equals(keepSimpleIfOnOneLineOption);
		}
		final Object keepThenStatementOnSameLineOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_KEEP_THEN_STATEMENT_ON_SAME_LINE);
		if (keepThenStatementOnSameLineOption != null) {
			this.keep_then_statement_on_same_line = DefaultCodeFormatterConstants.TRUE.equals(keepThenStatementOnSameLineOption);
		}
		final Object keepSimpleForBodyOnSameLineOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_KEEP_SIMPLE_FOR_BODY_ON_SAME_LINE);
		if (keepSimpleForBodyOnSameLineOption != null) {
			this.keep_simple_for_body_on_same_line = DefaultCodeFormatterConstants.TRUE.equals(keepSimpleForBodyOnSameLineOption);
		}
		final Object keepSimpleWhileBodyOnSameLineOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_KEEP_SIMPLE_WHILE_BODY_ON_SAME_LINE);
		if (keepSimpleWhileBodyOnSameLineOption != null) {
			this.keep_simple_while_body_on_same_line = DefaultCodeFormatterConstants.TRUE.equals(keepSimpleWhileBodyOnSameLineOption);
		}
		final Object keepSimpleDoWhileBodyOnSameLineOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_KEEP_SIMPLE_DO_WHILE_BODY_ON_SAME_LINE);
		if (keepSimpleDoWhileBodyOnSameLineOption != null) {
			this.keep_simple_do_while_body_on_same_line = DefaultCodeFormatterConstants.TRUE.equals(keepSimpleDoWhileBodyOnSameLineOption);
		}
		final Object neverIndentBlockCommentOnFirstColumnOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_NEVER_INDENT_BLOCK_COMMENTS_ON_FIRST_COLUMN);
		if (neverIndentBlockCommentOnFirstColumnOption != null) {
			this.never_indent_block_comments_on_first_column = DefaultCodeFormatterConstants.TRUE.equals(neverIndentBlockCommentOnFirstColumnOption);
		}
		final Object neverIndentLineCommentOnFirstColumnOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_NEVER_INDENT_LINE_COMMENTS_ON_FIRST_COLUMN);
		if (neverIndentLineCommentOnFirstColumnOption != null) {
			this.never_indent_line_comments_on_first_column = DefaultCodeFormatterConstants.TRUE.equals(neverIndentLineCommentOnFirstColumnOption);
		}
		final Object numberOfEmptyLinesToPreserveOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_NUMBER_OF_EMPTY_LINES_TO_PRESERVE);
		if (numberOfEmptyLinesToPreserveOption != null) {
			try {
				this.number_of_empty_lines_to_preserve = Integer.parseInt((String) numberOfEmptyLinesToPreserveOption);
			} catch(NumberFormatException | ClassCastException e) {
				this.number_of_empty_lines_to_preserve = 0;
			}
		}
		final Object joinLinesInCommentsOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_JOIN_LINES_IN_COMMENTS);
		if (joinLinesInCommentsOption != null) {
			this.join_lines_in_comments = DefaultCodeFormatterConstants.TRUE.equals(joinLinesInCommentsOption);
		}
		final Object joinWrappedLinesOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_JOIN_WRAPPED_LINES);
		if (joinWrappedLinesOption != null) {
			this.join_wrapped_lines = DefaultCodeFormatterConstants.TRUE.equals(joinWrappedLinesOption);
		}
		final Object putEmptyStatementOnNewLineOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_PUT_EMPTY_STATEMENT_ON_NEW_LINE);
		if (putEmptyStatementOnNewLineOption != null) {
			this.put_empty_statement_on_new_line = DefaultCodeFormatterConstants.TRUE.equals(putEmptyStatementOnNewLineOption);
		}
		final Object tabSizeOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE);
		if (tabSizeOption != null) {
			int tabSize = 4;
			try {
				tabSize = Integer.parseInt((String) tabSizeOption);
			} catch(NumberFormatException | ClassCastException e) {
				// keep default
			}
			// reverse values swapping performed by IndentationTabPage
			if (!JavaCore.SPACE.equals(settings.get(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR)))
				this.tab_size = tabSize;
			if (!DefaultCodeFormatterConstants.MIXED.equals(settings.get(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR)))
				this.indentation_size = tabSize;
		}
		final Object useTabsOnlyForLeadingIndentationsOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_USE_TABS_ONLY_FOR_LEADING_INDENTATIONS);
		if (useTabsOnlyForLeadingIndentationsOption != null) {
			this.use_tabs_only_for_leading_indentations = DefaultCodeFormatterConstants.TRUE.equals(useTabsOnlyForLeadingIndentationsOption);
		}
		setInt(settings, DefaultCodeFormatterConstants.FORMATTER_TEXT_BLOCK_INDENTATION, v -> {
			if (DefaultCodeFormatterConstants.INDENT_PRESERVE == v) {
				this.text_block_indentation = Alignment.M_INDENT_PRESERVE;
			} else if (DefaultCodeFormatterConstants.INDENT_BY_ONE == v) {
				this.text_block_indentation = Alignment.M_INDENT_BY_ONE;
			} else if (DefaultCodeFormatterConstants.INDENT_DEFAULT == v) {
				this.text_block_indentation = Alignment.M_INDENT_DEFAULT;
			} else if (DefaultCodeFormatterConstants.INDENT_ON_COLUMN == v) {
				this.text_block_indentation = Alignment.M_INDENT_ON_COLUMN;
			} else {
				throw new IllegalArgumentException("invalid text block setting: " + v); //$NON-NLS-1$
			}
		});
		final Object pageWidthOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_LINE_SPLIT);
		if (pageWidthOption != null) {
			try {
				this.page_width = Integer.parseInt((String) pageWidthOption);
			} catch(NumberFormatException | ClassCastException e) {
				this.page_width = 120;
			}
		}
		final Object useTabOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR);
		if (useTabOption != null) {
			if (JavaCore.TAB.equals(useTabOption)) {
				this.tab_char = TAB;
			} else if (JavaCore.SPACE.equals(useTabOption)) {
				this.tab_char = SPACE;
			} else {
				this.tab_char = MIXED;
			}
		}
		setBoolean(settings, DefaultCodeFormatterConstants.FORMATTER_WRAP_BEFORE_MULTIPLICATIVE_OPERATOR, DefaultCodeFormatterConstants.TRUE,
				v -> this.wrap_before_multiplicative_operator = v);
		setBoolean(settings, DefaultCodeFormatterConstants.FORMATTER_WRAP_BEFORE_ASSERTION_MESSAGE_OPERATOR, DefaultCodeFormatterConstants.TRUE,
				v -> this.wrap_before_assertion_message_operator = v);
		setBoolean(settings, DefaultCodeFormatterConstants.FORMATTER_WRAP_BEFORE_ADDITIVE_OPERATOR, DefaultCodeFormatterConstants.TRUE,
				v -> this.wrap_before_additive_operator = v);
		setBoolean(settings, DefaultCodeFormatterConstants.FORMATTER_WRAP_BEFORE_STRING_CONCATENATION, DefaultCodeFormatterConstants.TRUE,
				v -> this.wrap_before_string_concatenation = v);
		setBoolean(settings, DefaultCodeFormatterConstants.FORMATTER_WRAP_BEFORE_SHIFT_OPERATOR, DefaultCodeFormatterConstants.TRUE,
				v -> this.wrap_before_shift_operator = v);
		setBoolean(settings, DefaultCodeFormatterConstants.FORMATTER_WRAP_BEFORE_RELATIONAL_OPERATOR, DefaultCodeFormatterConstants.TRUE,
				v -> this.wrap_before_relational_operator = v);
		setBoolean(settings, DefaultCodeFormatterConstants.FORMATTER_WRAP_BEFORE_BITWISE_OPERATOR, DefaultCodeFormatterConstants.TRUE,
				v -> this.wrap_before_bitwise_operator = v);
		setBoolean(settings, DefaultCodeFormatterConstants.FORMATTER_WRAP_BEFORE_LOGICAL_OPERATOR, DefaultCodeFormatterConstants.TRUE,
				v -> this.wrap_before_logical_operator = v);
		setBoolean(settings, DefaultCodeFormatterConstants.FORMATTER_WRAP_BEFORE_OR_OPERATOR_MULTICATCH, DefaultCodeFormatterConstants.TRUE,
				v -> this.wrap_before_or_operator_multicatch = v);
		setBoolean(settings, DefaultCodeFormatterConstants.FORMATTER_WRAP_BEFORE_CONDITIONAL_OPERATOR, DefaultCodeFormatterConstants.TRUE,
				v -> this.wrap_before_conditional_operator = v);
		setBoolean(settings, DefaultCodeFormatterConstants.FORMATTER_WRAP_BEFORE_ASSIGNMENT_OPERATOR, DefaultCodeFormatterConstants.TRUE,
				v -> this.wrap_before_assignment_operator = v);
		setBoolean(settings, DefaultCodeFormatterConstants.FORMATTER_WRAP_BEFORE_SWITCH_CASE_ARROW_OPERATOR, DefaultCodeFormatterConstants.TRUE,
				v -> this.wrap_before_switch_case_arrow_operator = v);

		final Object useTags = settings.get(DefaultCodeFormatterConstants.FORMATTER_USE_ON_OFF_TAGS);
		if (useTags != null) {
			this.use_tags = DefaultCodeFormatterConstants.TRUE.equals(useTags);
		}
		final Object disableTagOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_DISABLING_TAG);
		if (disableTagOption != null) {
			if (disableTagOption instanceof String) {
				String stringValue = (String) disableTagOption;
				int idx = stringValue.indexOf('\n');
				if (idx == 0) {
					this.disabling_tag = null;
				} else {
					String tag = idx < 0 ? stringValue.trim() : stringValue.substring(0, idx).trim();
					if (tag.length() == 0) {
						this.disabling_tag = null;
					} else {
						this.disabling_tag = tag.toCharArray();
					}
				}
			}
		}
		final Object enableTagOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_ENABLING_TAG);
		if (enableTagOption != null) {
			if (enableTagOption instanceof String) {
				String stringValue = (String) enableTagOption;
				int idx = stringValue.indexOf('\n');
				if (idx == 0) {
					this.enabling_tag = null;
				} else {
					String tag = idx < 0 ? stringValue.trim() : stringValue.substring(0, idx).trim();
					if (tag.length() == 0) {
						this.enabling_tag = null;
					} else {
						this.enabling_tag = tag.toCharArray();
					}
				}
			}
		}
		final Object wrapWrapOuterExpressionsWhenNestedOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_WRAP_OUTER_EXPRESSIONS_WHEN_NESTED);
		if (wrapWrapOuterExpressionsWhenNestedOption != null) {
			this.wrap_outer_expressions_when_nested = DefaultCodeFormatterConstants.TRUE.equals(wrapWrapOuterExpressionsWhenNestedOption);
		}

		setDerivableOptions(settings);
	}

	private int toInt(Object value, int defaultValue) {
		if (value instanceof String) {
			try {
				return Integer.parseInt((String) value);
			} catch (NumberFormatException e) {
				return defaultValue;
			}
		}
		return defaultValue;
	}

	private String toString(Object value, String defaultValue) {
		if (value instanceof String)
			return (String) value;
		return defaultValue;
	}

	private void setInt(Map<String, String> settings, String key, IntConsumer setter) {
		String value = settings.get(key);
		if (value != null) {
			try {
				setter.accept(Integer.parseInt(value));
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("Expected integer for setting " + key + ", got: " + value); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	}

	private void setString(Map<String, String> settings, String key, List<String> allowedValues, Consumer<String> setter) {
		Object value = settings.get(key);
		if (value != null) {
			if (!allowedValues.contains(value))
				throw new IllegalArgumentException("Unrecognized value for setting " + key + ": " + value); //$NON-NLS-1$ //$NON-NLS-2$
			setter.accept((String) value);
		}
	}

	private void setBoolean(Map<String, String> settings, String key, String trueValue, Consumer<Boolean> setter) {
		Object value = settings.get(key);
		if (value != null)
			setter.accept(trueValue.equals(value));
	}

	/**
	 * This method is used to handle deprecated preferences which might be replaced by
	 * one or more preferences.
	 * Depending on deprecated option handling policy, set the new formatting option(s).
	 * <p>
	 * Note: Also add deprecated preference keys in {@link org.eclipse.jdt.internal.core.JavaCorePreferenceInitializer#initializeDeprecatedOptions}
	 * so that the formatter recognizes those deprecated options when used with project specific formatter profiles.
	 * (see <a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=544776">Bug 544776</a>)
	 * </p>
	 *
	 * @param settings the given map
	 * @deprecated
	 */
	private void setDeprecatedOptions(Map<String, String> settings) {
		// backward compatibility code
		final Object commentClearBlankLinesOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_COMMENT_CLEAR_BLANK_LINES);
		if (commentClearBlankLinesOption != null) {
			this.comment_clear_blank_lines_in_javadoc_comment = DefaultCodeFormatterConstants.TRUE.equals(commentClearBlankLinesOption);
			this.comment_clear_blank_lines_in_block_comment = DefaultCodeFormatterConstants.TRUE.equals(commentClearBlankLinesOption);
		} else {
			final Object commentClearBlankLinesInJavadocCommentOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_COMMENT_CLEAR_BLANK_LINES_IN_JAVADOC_COMMENT);
			if (commentClearBlankLinesInJavadocCommentOption != null) {
				this.comment_clear_blank_lines_in_javadoc_comment = DefaultCodeFormatterConstants.TRUE.equals(commentClearBlankLinesInJavadocCommentOption);
			}
			final Object commentClearBlankLinesInBlockCommentOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_COMMENT_CLEAR_BLANK_LINES_IN_BLOCK_COMMENT);
			if (commentClearBlankLinesInBlockCommentOption != null) {
				this.comment_clear_blank_lines_in_block_comment = DefaultCodeFormatterConstants.TRUE.equals(commentClearBlankLinesInBlockCommentOption);
			}
		}

		// New line after annotations
		final Object insertNewLineAfterAnnotationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION);

		final Object insertNewLineAfterAnnotationOnMemberOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION_ON_MEMBER);
		final Object insertNewLineAfterAnnotationOnTypeOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION_ON_TYPE);
		final Object insertNewLineAfterAnnotationOnEnumConstantOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION_ON_ENUM_CONSTANT);
		final Object insertNewLineAfterAnnotationOnFieldOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION_ON_FIELD);
		final Object insertNewLineAfterAnnotationOnMethodOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION_ON_METHOD);
		final Object insertNewLineAfterAnnotationOnPackageOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION_ON_PACKAGE);

		final Object insertNewLineAfterAnnotationOnParameterOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION_ON_PARAMETER);
		final Object insertNewLineAfterAnnotationOnLocalVariableOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION_ON_LOCAL_VARIABLE);

		if (insertNewLineAfterAnnotationOnTypeOption == null
				&& insertNewLineAfterAnnotationOnEnumConstantOption == null
				&& insertNewLineAfterAnnotationOnFieldOption == null
				&& insertNewLineAfterAnnotationOnMethodOption == null
				&& insertNewLineAfterAnnotationOnPackageOption == null) {
			// if none of the new 3.7 options is used, fall back to the deprecated 3.4 option
			if (insertNewLineAfterAnnotationOnMemberOption != null) {
				boolean insert = JavaCore.INSERT.equals(insertNewLineAfterAnnotationOnMemberOption);
				this.insert_new_line_after_annotation_on_type = insert;
				this.insert_new_line_after_annotation_on_enum_constant = insert;
				this.insert_new_line_after_annotation_on_field = insert;
				this.insert_new_line_after_annotation_on_method = insert;
				this.insert_new_line_after_annotation_on_package = insert;

				// and use the other 3.4 options if available
				if (insertNewLineAfterAnnotationOnParameterOption != null) {
					this.insert_new_line_after_annotation_on_parameter = JavaCore.INSERT.equals(insertNewLineAfterAnnotationOnParameterOption);
				}
				if (insertNewLineAfterAnnotationOnLocalVariableOption != null) {
					this.insert_new_line_after_annotation_on_local_variable = JavaCore.INSERT.equals(insertNewLineAfterAnnotationOnLocalVariableOption);
				}
			} else if (insertNewLineAfterAnnotationOnParameterOption == null
					&& insertNewLineAfterAnnotationOnLocalVariableOption == null) {
				// if none of the new 3.4 options is used, fall back to the deprecated 3.1 option
				if (insertNewLineAfterAnnotationOption != null) {
					boolean insert = JavaCore.INSERT.equals(insertNewLineAfterAnnotationOption);
					this.insert_new_line_after_annotation_on_type = insert;
					this.insert_new_line_after_annotation_on_enum_constant = insert;
					this.insert_new_line_after_annotation_on_field = insert;
					this.insert_new_line_after_annotation_on_method = insert;
					this.insert_new_line_after_annotation_on_package = insert;
					this.insert_new_line_after_annotation_on_parameter = insert;
					this.insert_new_line_after_annotation_on_local_variable = insert;
					int alignment = insert ? Alignment.M_FORCE | Alignment.M_ONE_PER_LINE_SPLIT
							: Alignment.M_NO_ALIGNMENT;
					this.alignment_for_annotations_on_type = alignment;
					this.alignment_for_annotations_on_enum_constant = alignment;
					this.alignment_for_annotations_on_field = alignment;
					this.alignment_for_annotations_on_method = alignment;
					this.alignment_for_annotations_on_package = alignment;
					this.alignment_for_annotations_on_parameter = alignment;
					this.alignment_for_annotations_on_local_variable = alignment;
				}
			}
		} else { // otherwise use new 3.7 options if available
			if (insertNewLineAfterAnnotationOnTypeOption != null) {
				this.insert_new_line_after_annotation_on_type = JavaCore.INSERT.equals(insertNewLineAfterAnnotationOnTypeOption);
			}
			if (insertNewLineAfterAnnotationOnEnumConstantOption != null) {
				this.insert_new_line_after_annotation_on_enum_constant = JavaCore.INSERT.equals(insertNewLineAfterAnnotationOnEnumConstantOption);
			}
			if (insertNewLineAfterAnnotationOnFieldOption != null) {
				this.insert_new_line_after_annotation_on_field = JavaCore.INSERT.equals(insertNewLineAfterAnnotationOnFieldOption);
			}
			if (insertNewLineAfterAnnotationOnMethodOption != null) {
				this.insert_new_line_after_annotation_on_method = JavaCore.INSERT.equals(insertNewLineAfterAnnotationOnMethodOption);
			}
			if (insertNewLineAfterAnnotationOnPackageOption != null) {
				this.insert_new_line_after_annotation_on_package = JavaCore.INSERT.equals(insertNewLineAfterAnnotationOnPackageOption);
			}
			// and the other 3.4 options if available
			if (insertNewLineAfterAnnotationOnParameterOption != null) {
				this.insert_new_line_after_annotation_on_parameter = JavaCore.INSERT.equals(insertNewLineAfterAnnotationOnParameterOption);
			}
			if (insertNewLineAfterAnnotationOnLocalVariableOption != null) {
				this.insert_new_line_after_annotation_on_local_variable = JavaCore.INSERT.equals(insertNewLineAfterAnnotationOnLocalVariableOption);
			}
		}

		// insert new line between empty braces -> keep braced code on one line
		HashMap<Boolean, String> insertToOneLine = new HashMap<>();
		insertToOneLine.put(true, DefaultCodeFormatterConstants.ONE_LINE_NEVER);
		insertToOneLine.put(false, DefaultCodeFormatterConstants.ONE_LINE_IF_EMPTY);
		if (settings.get(DefaultCodeFormatterConstants.FORMATTER_KEEP_ANNOTATION_DECLARATION_ON_ONE_LINE) == null) {
			setBoolean(settings, DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_IN_EMPTY_ANNOTATION_DECLARATION, JavaCore.INSERT,
					v -> this.keep_annotation_declaration_on_one_line = insertToOneLine.get(v));
		}
		if (settings.get(DefaultCodeFormatterConstants.FORMATTER_KEEP_ANONYMOUS_TYPE_DECLARATION_ON_ONE_LINE) == null) {
			setBoolean(settings, DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_IN_EMPTY_ANONYMOUS_TYPE_DECLARATION, JavaCore.INSERT,
					v -> this.keep_anonymous_type_declaration_on_one_line = insertToOneLine.get(v));
		}
		if (settings.get(DefaultCodeFormatterConstants.FORMATTER_KEEP_IF_THEN_BODY_BLOCK_ON_ONE_LINE) == null) {
			setBoolean(settings, DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_IN_EMPTY_BLOCK, JavaCore.INSERT,
					v -> this.keep_if_then_body_block_on_one_line = insertToOneLine.get(v));
		}
		if (settings.get(DefaultCodeFormatterConstants.FORMATTER_KEEP_SWITCH_CASE_WITH_ARROW_ON_ONE_LINE) == null) {
			setBoolean(settings, DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_IN_EMPTY_BLOCK, JavaCore.INSERT,
					v -> this.keep_switch_case_with_arrow_on_one_line = insertToOneLine.get(v));
		}
		if (settings.get(DefaultCodeFormatterConstants.FORMATTER_KEEP_LOOP_BODY_BLOCK_ON_ONE_LINE) == null) {
			setBoolean(settings, DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_IN_EMPTY_BLOCK, JavaCore.INSERT,
					v -> this.keep_loop_body_block_on_one_line = insertToOneLine.get(v));
		}
		if (settings.get(DefaultCodeFormatterConstants.FORMATTER_KEEP_LAMBDA_BODY_BLOCK_ON_ONE_LINE) == null) {
			setBoolean(settings, DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_IN_EMPTY_BLOCK, JavaCore.INSERT,
					v -> this.keep_lambda_body_block_on_one_line = insertToOneLine.get(v));
		}
		if (settings.get(DefaultCodeFormatterConstants.FORMATTER_KEEP_CODE_BLOCK_ON_ONE_LINE) == null) {
			setBoolean(settings, DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_IN_EMPTY_BLOCK, JavaCore.INSERT,
					v -> this.keep_code_block_on_one_line = insertToOneLine.get(v));
		}
		if (settings.get(DefaultCodeFormatterConstants.FORMATTER_KEEP_ENUM_CONSTANT_DECLARATION_ON_ONE_LINE) == null) {
			setBoolean(settings, DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_IN_EMPTY_ENUM_CONSTANT, JavaCore.INSERT,
					v -> this.keep_enum_constant_declaration_on_one_line = insertToOneLine.get(v));
		}
		if (settings.get(DefaultCodeFormatterConstants.FORMATTER_KEEP_ENUM_DECLARATION_ON_ONE_LINE) == null) {
			setBoolean(settings, DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_IN_EMPTY_ENUM_DECLARATION, JavaCore.INSERT,
					v -> this.keep_enum_declaration_on_one_line = insertToOneLine.get(v));
		}
		if (settings.get(DefaultCodeFormatterConstants.FORMATTER_KEEP_METHOD_BODY_ON_ONE_LINE) == null) {
			setBoolean(settings, DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_IN_EMPTY_METHOD_BODY, JavaCore.INSERT,
					v -> this.keep_method_body_on_one_line = insertToOneLine.get(v));
		}
		if (settings.get(DefaultCodeFormatterConstants.FORMATTER_KEEP_TYPE_DECLARATION_ON_ONE_LINE) == null) {
			setBoolean(settings, DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_IN_EMPTY_TYPE_DECLARATION, JavaCore.INSERT,
					v -> this.keep_type_declaration_on_one_line = insertToOneLine.get(v));
		}

		// alignment for binary expressions -> more granular settings
		if (settings.get(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_MULTIPLICATIVE_OPERATOR) == null) {
			setInt(settings, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_BINARY_EXPRESSION,
					v-> this.alignment_for_multiplicative_operator = v);
		}
		if (settings.get(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ADDITIVE_OPERATOR) == null) {
			setInt(settings, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_BINARY_EXPRESSION,
					v -> this.alignment_for_additive_operator = v);
		}
		if (settings.get(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_STRING_CONCATENATION) == null) {
			setInt(settings, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_BINARY_EXPRESSION,
					v -> this.alignment_for_string_concatenation = v);
		}
		if (settings.get(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_BITWISE_OPERATOR) == null) {
			setInt(settings, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_BINARY_EXPRESSION,
					v -> this.alignment_for_bitwise_operator = v);
		}
		if (settings.get(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_LOGICAL_OPERATOR) == null) {
			setInt(settings, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_BINARY_EXPRESSION,
					v -> this.alignment_for_logical_operator = v);
		}
		if (settings.get(DefaultCodeFormatterConstants.FORMATTER_WRAP_BEFORE_MULTIPLICATIVE_OPERATOR) == null) {
			setBoolean(settings, DefaultCodeFormatterConstants.FORMATTER_WRAP_BEFORE_BINARY_OPERATOR, DefaultCodeFormatterConstants.TRUE,
					v -> this.wrap_before_multiplicative_operator = v);
		}
		if (settings.get(DefaultCodeFormatterConstants.FORMATTER_WRAP_BEFORE_ADDITIVE_OPERATOR) == null) {
			setBoolean(settings, DefaultCodeFormatterConstants.FORMATTER_WRAP_BEFORE_BINARY_OPERATOR, DefaultCodeFormatterConstants.TRUE,
					v -> this.wrap_before_additive_operator = v);
		}
		if (settings.get(DefaultCodeFormatterConstants.FORMATTER_WRAP_BEFORE_STRING_CONCATENATION) == null) {
			setBoolean(settings, DefaultCodeFormatterConstants.FORMATTER_WRAP_BEFORE_BINARY_OPERATOR, DefaultCodeFormatterConstants.TRUE,
					v -> this.wrap_before_string_concatenation = v);
		}
		if (settings.get(DefaultCodeFormatterConstants.FORMATTER_WRAP_BEFORE_BITWISE_OPERATOR) == null) {
			setBoolean(settings, DefaultCodeFormatterConstants.FORMATTER_WRAP_BEFORE_BINARY_OPERATOR, DefaultCodeFormatterConstants.TRUE,
					v -> this.wrap_before_bitwise_operator = v);
		}
		if (settings.get(DefaultCodeFormatterConstants.FORMATTER_WRAP_BEFORE_LOGICAL_OPERATOR) == null) {
			setBoolean(settings, DefaultCodeFormatterConstants.FORMATTER_WRAP_BEFORE_BINARY_OPERATOR, DefaultCodeFormatterConstants.TRUE,
					v -> this.wrap_before_logical_operator = v);
		}

		// add space before and after binary operator -> more granular settings
		if (settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_MULTIPLICATIVE_OPERATOR) == null) {
			setBoolean(settings, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_BINARY_OPERATOR, JavaCore.INSERT,
					v -> this.insert_space_before_multiplicative_operator = v);
		}
		if (settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_ADDITIVE_OPERATOR) == null) {
			setBoolean(settings, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_BINARY_OPERATOR, JavaCore.INSERT,
					v -> this.insert_space_before_additive_operator = v);
		}
		if (settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_STRING_CONCATENATION) == null) {
			setBoolean(settings, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_BINARY_OPERATOR, JavaCore.INSERT,
					v -> this.insert_space_before_string_concatenation = v);
		}
		if (settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_SHIFT_OPERATOR) == null) {
			setBoolean(settings, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_BINARY_OPERATOR, JavaCore.INSERT,
					v -> this.insert_space_before_shift_operator = v);
		}
		if (settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_RELATIONAL_OPERATOR) == null) {
			setBoolean(settings, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_BINARY_OPERATOR, JavaCore.INSERT,
					v -> this.insert_space_before_relational_operator = v);
		}
		if (settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_BITWISE_OPERATOR) == null) {
			setBoolean(settings, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_BINARY_OPERATOR, JavaCore.INSERT,
					v -> this.insert_space_before_bitwise_operator = v);
		}
		if (settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_LOGICAL_OPERATOR) == null) {
			setBoolean(settings, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_BINARY_OPERATOR, JavaCore.INSERT,
					v -> this.insert_space_before_logical_operator = v);
		}
		if (settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_MULTIPLICATIVE_OPERATOR) == null) {
			setBoolean(settings, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_BINARY_OPERATOR, JavaCore.INSERT,
					v -> this.insert_space_after_multiplicative_operator = v);
		}
		if (settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_ADDITIVE_OPERATOR) == null) {
			setBoolean(settings, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_BINARY_OPERATOR, JavaCore.INSERT,
					v -> this.insert_space_after_additive_operator = v);
		}
		if (settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_STRING_CONCATENATION) == null) {
			setBoolean(settings, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_BINARY_OPERATOR, JavaCore.INSERT,
					v -> this.insert_space_after_string_concatenation = v);
		}
		if (settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_SHIFT_OPERATOR) == null) {
			setBoolean(settings, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_BINARY_OPERATOR, JavaCore.INSERT,
					v -> this.insert_space_after_shift_operator = v);
		}
		if (settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_RELATIONAL_OPERATOR) == null) {
			setBoolean(settings, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_BINARY_OPERATOR, JavaCore.INSERT,
					v -> this.insert_space_after_relational_operator = v);
		}
		if (settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_BITWISE_OPERATOR) == null) {
			setBoolean(settings, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_BINARY_OPERATOR, JavaCore.INSERT,
					v -> this.insert_space_after_bitwise_operator = v);
		}
		if (settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_LOGICAL_OPERATOR) == null) {
			setBoolean(settings, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_BINARY_OPERATOR, JavaCore.INSERT,
					v -> this.insert_space_after_logical_operator = v);
		}
	}

	/**
	 * Handles new settings which may not be defined in an older profile, but are can be easily derived from other
	 * settings to keep the behavior consistent with previous versions.
	 */
	private void setDerivableOptions(Map<String, String> settings) {
		if (!settings.containsKey(DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_ABSTRACT_METHOD)) {
			setInt(settings, DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_METHOD,
					v -> this.blank_lines_before_abstract_method = v);
		}
		if (!settings.containsKey(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_NOT_OPERATOR)) {
			setBoolean(settings, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_UNARY_OPERATOR, JavaCore.INSERT,
					v -> this.insert_space_after_not_operator = v);
		}
	}

	public void setDefaultSettings() {
		this.alignment_for_annotations_on_type = Alignment.M_FORCE | Alignment.M_ONE_PER_LINE_SPLIT;
		this.alignment_for_type_annotations = Alignment.M_NO_ALIGNMENT;
		this.alignment_for_annotations_on_enum_constant = Alignment.M_FORCE | Alignment.M_ONE_PER_LINE_SPLIT;
		this.alignment_for_annotations_on_field = Alignment.M_FORCE | Alignment.M_ONE_PER_LINE_SPLIT;
		this.alignment_for_annotations_on_method = Alignment.M_FORCE | Alignment.M_ONE_PER_LINE_SPLIT;
		this.alignment_for_annotations_on_package = Alignment.M_FORCE | Alignment.M_ONE_PER_LINE_SPLIT;
		this.alignment_for_annotations_on_parameter = Alignment.M_NO_ALIGNMENT;
		this.alignment_for_annotations_on_local_variable = Alignment.M_FORCE | Alignment.M_ONE_PER_LINE_SPLIT;
		this.alignment_for_arguments_in_allocation_expression = Alignment.M_COMPACT_SPLIT;
		this.alignment_for_arguments_in_annotation = Alignment.M_NO_ALIGNMENT;
		this.alignment_for_arguments_in_enum_constant = Alignment.M_COMPACT_SPLIT;
		this.alignment_for_arguments_in_explicit_constructor_call = Alignment.M_COMPACT_SPLIT;
		this.alignment_for_arguments_in_method_invocation = Alignment.M_COMPACT_SPLIT;
		this.alignment_for_arguments_in_qualified_allocation_expression = Alignment.M_COMPACT_SPLIT;
		this.alignment_for_assertion_message = Alignment.M_COMPACT_SPLIT;
		this.alignment_for_assignment = Alignment.M_NO_ALIGNMENT;
		this.alignment_for_multiplicative_operator = Alignment.M_COMPACT_SPLIT;
		this.alignment_for_additive_operator = Alignment.M_COMPACT_SPLIT;
		this.alignment_for_string_concatenation = Alignment.M_COMPACT_SPLIT;
		this.alignment_for_shift_operator = Alignment.M_NO_ALIGNMENT;
		this.alignment_for_relational_operator = Alignment.M_NO_ALIGNMENT;
		this.alignment_for_bitwise_operator = Alignment.M_COMPACT_SPLIT;
		this.alignment_for_logical_operator = Alignment.M_COMPACT_SPLIT;
		this.alignment_for_compact_if = Alignment.M_ONE_PER_LINE_SPLIT | Alignment.M_INDENT_BY_ONE;
		this.alignment_for_compact_loop = Alignment.M_ONE_PER_LINE_SPLIT | Alignment.M_INDENT_BY_ONE;
		this.alignment_for_conditional_expression = Alignment.M_ONE_PER_LINE_SPLIT;
		this.alignment_for_conditional_expression_chain = Alignment.M_NO_ALIGNMENT;
		this.alignment_for_enum_constants = Alignment.M_NO_ALIGNMENT;
		this.alignment_for_expressions_in_array_initializer = Alignment.M_COMPACT_SPLIT;
		this.alignment_for_expressions_in_for_loop_header = Alignment.M_NO_ALIGNMENT;
		this.alignment_for_expressions_in_switch_case_with_arrow = Alignment.M_COMPACT_SPLIT;
		this.alignment_for_expressions_in_switch_case_with_colon = Alignment.M_COMPACT_SPLIT;
		this.alignment_for_method_declaration = Alignment.M_NO_ALIGNMENT;
		this.alignment_for_module_statements = Alignment.M_COMPACT_SPLIT;
		this.alignment_for_multiple_fields = Alignment.M_COMPACT_SPLIT;
		this.alignment_for_parameterized_type_references = Alignment.M_NO_ALIGNMENT;
		this.alignment_for_parameters_in_constructor_declaration = Alignment.M_COMPACT_SPLIT;
		this.alignment_for_parameters_in_method_declaration = Alignment.M_COMPACT_SPLIT;
		this.alignment_for_record_components = Alignment.M_COMPACT_SPLIT;
		this.alignment_for_resources_in_try = Alignment.M_NEXT_PER_LINE_SPLIT;
		this.alignment_for_selector_in_method_invocation = Alignment.M_COMPACT_SPLIT;
		this.alignment_for_superclass_in_type_declaration = Alignment.M_NEXT_SHIFTED_SPLIT;
		this.alignment_for_superinterfaces_in_enum_declaration = Alignment.M_NEXT_SHIFTED_SPLIT;
		this.alignment_for_superinterfaces_in_record_declaration = Alignment.M_NEXT_SHIFTED_SPLIT;
		this.alignment_for_superinterfaces_in_type_declaration = Alignment.M_NEXT_SHIFTED_SPLIT;
		this.alignment_for_switch_case_with_arrow = Alignment.M_COMPACT_SPLIT | Alignment.M_INDENT_BY_ONE;
		this.alignment_for_throws_clause_in_constructor_declaration = Alignment.M_COMPACT_SPLIT;
		this.alignment_for_throws_clause_in_method_declaration = Alignment.M_COMPACT_SPLIT;
		this.alignment_for_type_arguments = Alignment.M_NO_ALIGNMENT;
		this.alignment_for_type_parameters = Alignment.M_NO_ALIGNMENT;
		this.alignment_for_union_type_in_multicatch = Alignment.M_COMPACT_SPLIT;
		this.align_selector_in_method_invocation_on_expression_first_line = true;
		this.align_type_members_on_columns = false;
		this.align_variable_declarations_on_columns = false;
		this.align_assignment_statements_on_columns = false;
		this.align_with_spaces = false;
		this.align_fields_grouping_blank_lines = Integer.MAX_VALUE;
		this.brace_position_for_annotation_type_declaration = DefaultCodeFormatterConstants.END_OF_LINE;
		this.brace_position_for_anonymous_type_declaration = DefaultCodeFormatterConstants.END_OF_LINE;
		this.brace_position_for_array_initializer = DefaultCodeFormatterConstants.END_OF_LINE;
		this.brace_position_for_block = DefaultCodeFormatterConstants.END_OF_LINE;
		this.brace_position_for_block_in_case = DefaultCodeFormatterConstants.END_OF_LINE;
		this.brace_position_for_constructor_declaration = DefaultCodeFormatterConstants.END_OF_LINE;
		this.brace_position_for_enum_constant = DefaultCodeFormatterConstants.END_OF_LINE;
		this.brace_position_for_enum_declaration = DefaultCodeFormatterConstants.END_OF_LINE;
		this.brace_position_for_lambda_body = DefaultCodeFormatterConstants.END_OF_LINE;
		this.brace_position_for_method_declaration = DefaultCodeFormatterConstants.END_OF_LINE;
		this.brace_position_for_type_declaration = DefaultCodeFormatterConstants.END_OF_LINE;
		this.brace_position_for_record_constructor = DefaultCodeFormatterConstants.END_OF_LINE;
		this.brace_position_for_record_declaration = DefaultCodeFormatterConstants.END_OF_LINE;
		this.brace_position_for_switch = DefaultCodeFormatterConstants.END_OF_LINE;
		this.parenthesis_positions_in_method_declaration = DefaultCodeFormatterConstants.COMMON_LINES;
		this.parenthesis_positions_in_method_invocation = DefaultCodeFormatterConstants.COMMON_LINES;
		this.parenthesis_positions_in_enum_constant_declaration = DefaultCodeFormatterConstants.COMMON_LINES;
		this.parenthesis_positions_in_record_declaration = DefaultCodeFormatterConstants.COMMON_LINES;
		this.parenthesis_positions_in_if_while_statement = DefaultCodeFormatterConstants.COMMON_LINES;
		this.parenthesis_positions_in_for_statement = DefaultCodeFormatterConstants.COMMON_LINES;
		this.parenthesis_positions_in_switch_statement = DefaultCodeFormatterConstants.COMMON_LINES;
		this.parenthesis_positions_in_try_clause = DefaultCodeFormatterConstants.COMMON_LINES;
		this.parenthesis_positions_in_catch_clause = DefaultCodeFormatterConstants.COMMON_LINES;
		this.parenthesis_positions_in_annotation = DefaultCodeFormatterConstants.COMMON_LINES;
		this.parenthesis_positions_in_lambda_declaration = DefaultCodeFormatterConstants.COMMON_LINES;
		this.comment_clear_blank_lines_in_block_comment = false;
		this.comment_clear_blank_lines_in_javadoc_comment = false;
		this.comment_format_block_comment = true;
		this.comment_format_javadoc_comment = true;
		this.comment_format_line_comment = true;
		this.comment_format_line_comment_starting_on_first_column = true;
		this.comment_format_header = false;
		this.comment_format_html = true;
		this.comment_format_source = true;
		this.comment_indent_parameter_description = true;
		this.comment_indent_tag_description = false;
		this.comment_indent_root_tags = true;
		this.comment_align_tags_names_descriptions = false;
		this.comment_align_tags_descriptions_grouped = false;
		this.comment_insert_empty_line_before_root_tags = true;
		this.comment_insert_empty_line_between_different_tags = false;
		this.comment_insert_new_line_for_parameter = true;
		this.comment_new_lines_at_block_boundaries = true;
		this.comment_new_lines_at_javadoc_boundaries = true;
		this.comment_line_length = 80;
		this.comment_count_line_length_from_starting_position = true;
		this.comment_preserve_white_space_between_code_and_line_comments= false;
		this.continuation_indentation = 2;
		this.continuation_indentation_for_array_initializer = 2;
		this.blank_lines_after_imports = 0;
		this.blank_lines_after_package = 0;
		this.blank_lines_before_field = 0;
		this.blank_lines_before_first_class_body_declaration = 0;
		this.blank_lines_after_last_class_body_declaration = 0;
		this.blank_lines_before_imports = 0;
		this.blank_lines_before_member_type = 0;
		this.blank_lines_before_abstract_method = 0;
		this.blank_lines_before_method = 0;
		this.blank_lines_before_new_chunk = 0;
		this.blank_lines_before_package = 0;
		this.blank_lines_between_import_groups = 1;
		this.blank_lines_between_type_declarations = 0;
		this.blank_lines_at_beginning_of_method_body = 0;
		this.blank_lines_at_end_of_method_body = 0;
		this.blank_lines_at_beginning_of_code_block = 0;
		this.blank_lines_at_end_of_code_block = 0;
		this.blank_lines_before_code_block = 0;
		this.blank_lines_after_code_block = 0;
		this.blank_lines_between_statement_groups_in_switch = 0;
		this.indent_statements_compare_to_block = true;
		this.indent_statements_compare_to_body = true;
		this.indent_body_declarations_compare_to_annotation_declaration_header = true;
		this.indent_body_declarations_compare_to_enum_constant_header = true;
		this.indent_body_declarations_compare_to_enum_declaration_header = true;
		this.indent_body_declarations_compare_to_record_header = true;
		this.indent_body_declarations_compare_to_type_header = true;
		this.indent_breaks_compare_to_cases = true;
		this.indent_empty_lines = false;
		this.indent_switchstatements_compare_to_cases = true;
		this.indent_switchstatements_compare_to_switch = true;
		this.indentation_size = 4;
		this.insert_new_line_after_annotation_on_type = true;
		this.insert_new_line_after_type_annotation = false;
		this.insert_new_line_after_annotation_on_enum_constant = true;
		this.insert_new_line_after_annotation_on_field = true;
		this.insert_new_line_after_annotation_on_method = true;
		this.insert_new_line_after_annotation_on_package = true;
		this.insert_new_line_after_annotation_on_parameter = false;
		this.insert_new_line_after_annotation_on_local_variable = true;
		this.insert_new_line_after_opening_brace_in_array_initializer = false;
		this.insert_new_line_at_end_of_file_if_missing = false;
		this.insert_new_line_before_catch_in_try_statement = false;
		this.insert_new_line_before_closing_brace_in_array_initializer = false;
		this.insert_new_line_before_else_in_if_statement = false;
		this.insert_new_line_before_finally_in_try_statement = false;
		this.insert_new_line_before_while_in_do_statement = false;
		this.keep_annotation_declaration_on_one_line = DefaultCodeFormatterConstants.ONE_LINE_NEVER;
		this.keep_anonymous_type_declaration_on_one_line = DefaultCodeFormatterConstants.ONE_LINE_NEVER;
		this.keep_if_then_body_block_on_one_line = DefaultCodeFormatterConstants.ONE_LINE_NEVER;
		this.keep_switch_body_block_on_one_line = DefaultCodeFormatterConstants.ONE_LINE_NEVER;
		this.keep_switch_case_with_arrow_on_one_line = DefaultCodeFormatterConstants.ONE_LINE_NEVER;
		this.keep_lambda_body_block_on_one_line = DefaultCodeFormatterConstants.ONE_LINE_NEVER;
		this.keep_loop_body_block_on_one_line = DefaultCodeFormatterConstants.ONE_LINE_NEVER;
		this.keep_code_block_on_one_line = DefaultCodeFormatterConstants.ONE_LINE_NEVER;
		this.keep_enum_constant_declaration_on_one_line = DefaultCodeFormatterConstants.ONE_LINE_NEVER;
		this.keep_enum_declaration_on_one_line = DefaultCodeFormatterConstants.ONE_LINE_NEVER;
		this.keep_method_body_on_one_line = DefaultCodeFormatterConstants.ONE_LINE_NEVER;
		this.keep_type_declaration_on_one_line = DefaultCodeFormatterConstants.ONE_LINE_NEVER;
		this.keep_record_declaration_on_one_line = DefaultCodeFormatterConstants.ONE_LINE_NEVER;
		this.keep_record_constructor_on_one_line = DefaultCodeFormatterConstants.ONE_LINE_NEVER;
		this.keep_simple_getter_setter_on_one_line = false;
		this.insert_space_after_and_in_type_parameter = true;
		this.insert_space_after_arrow_in_switch_case = true;
		this.insert_space_after_arrow_in_switch_default = true;
		this.insert_space_after_assignment_operator = true;
		this.insert_space_after_at_in_annotation = false;
		this.insert_space_after_at_in_annotation_type_declaration = false;
		this.insert_space_after_multiplicative_operator = true;
		this.insert_space_after_additive_operator = true;
		this.insert_space_after_string_concatenation = true;
		this.insert_space_after_shift_operator = true;
		this.insert_space_after_relational_operator = true;
		this.insert_space_after_bitwise_operator = true;
		this.insert_space_after_logical_operator = true;
		this.insert_space_after_closing_angle_bracket_in_type_arguments = false;
		this.insert_space_after_closing_angle_bracket_in_type_parameters = true;
		this.insert_space_after_closing_paren_in_cast = true;
		this.insert_space_after_closing_brace_in_block = true;
		this.insert_space_after_colon_in_assert = true;
		this.insert_space_after_colon_in_case = true;
		this.insert_space_after_colon_in_conditional = true;
		this.insert_space_after_colon_in_for = true;
		this.insert_space_after_colon_in_labeled_statement = true;
		this.insert_space_after_comma_in_allocation_expression = true;
		this.insert_space_after_comma_in_annotation = true;
		this.insert_space_after_comma_in_array_initializer = true;
		this.insert_space_after_comma_in_constructor_declaration_parameters = true;
		this.insert_space_after_comma_in_constructor_declaration_throws = true;
		this.insert_space_after_comma_in_enum_constant_arguments = true;
		this.insert_space_after_comma_in_enum_declarations = true;
		this.insert_space_after_comma_in_explicit_constructor_call_arguments = true;
		this.insert_space_after_comma_in_for_increments = true;
		this.insert_space_after_comma_in_for_inits = true;
		this.insert_space_after_comma_in_method_invocation_arguments = true;
		this.insert_space_after_comma_in_method_declaration_parameters = true;
		this.insert_space_after_comma_in_method_declaration_throws = true;
		this.insert_space_after_comma_in_multiple_field_declarations = true;
		this.insert_space_after_comma_in_multiple_local_declarations = true;
		this.insert_space_after_comma_in_parameterized_type_reference = true;
		this.insert_space_after_comma_in_permitted_types = true;
		this.insert_space_after_comma_in_record_components = true;
		this.insert_space_after_comma_in_superinterfaces = true;
		this.insert_space_after_comma_in_switch_case_expressions = true;
		this.insert_space_after_comma_in_type_arguments = true;
		this.insert_space_after_comma_in_type_parameters = true;
		this.insert_space_after_ellipsis = true;
		this.insert_space_after_lambda_arrow = true;
		this.insert_space_after_not_operator = false;
		this.insert_space_after_opening_angle_bracket_in_parameterized_type_reference = false;
		this.insert_space_after_opening_angle_bracket_in_type_arguments = false;
		this.insert_space_after_opening_angle_bracket_in_type_parameters = false;
		this.insert_space_after_opening_bracket_in_array_allocation_expression = false;
		this.insert_space_after_opening_bracket_in_array_reference = false;
		this.insert_space_after_opening_brace_in_array_initializer = false;
		this.insert_space_after_opening_paren_in_annotation = false;
		this.insert_space_after_opening_paren_in_cast = false;
		this.insert_space_after_opening_paren_in_catch = false;
		this.insert_space_after_opening_paren_in_constructor_declaration = false;
		this.insert_space_after_opening_paren_in_enum_constant = false;
		this.insert_space_after_opening_paren_in_for = false;
		this.insert_space_after_opening_paren_in_if = false;
		this.insert_space_after_opening_paren_in_method_declaration = false;
		this.insert_space_after_opening_paren_in_method_invocation = false;
		this.insert_space_after_opening_paren_in_parenthesized_expression = false;
		this.insert_space_after_opening_paren_in_record_declaration = false;
		this.insert_space_after_opening_paren_in_switch = false;
		this.insert_space_after_opening_paren_in_synchronized = false;
		this.insert_space_after_opening_paren_in_try = false;
		this.insert_space_after_opening_paren_in_while = false;
		this.insert_space_after_postfix_operator = false;
		this.insert_space_after_prefix_operator = false;
		this.insert_space_after_question_in_conditional = true;
		this.insert_space_after_question_in_wilcard = false;
		this.insert_space_after_semicolon_in_for = true;
		this.insert_space_after_semicolon_in_try_resources = true;
		this.insert_space_after_unary_operator = false;
		this.insert_space_before_and_in_type_parameter = true;
		this.insert_space_before_arrow_in_switch_case = true;
		this.insert_space_before_arrow_in_switch_default = true;
		this.insert_space_before_at_in_annotation_type_declaration = true;
		this.insert_space_before_assignment_operator = true;
		this.insert_space_before_multiplicative_operator = true;
		this.insert_space_before_additive_operator = true;
		this.insert_space_before_string_concatenation = true;
		this.insert_space_before_shift_operator = true;
		this.insert_space_before_relational_operator = true;
		this.insert_space_before_bitwise_operator = true;
		this.insert_space_before_logical_operator = true;
		this.insert_space_before_closing_angle_bracket_in_parameterized_type_reference = false;
		this.insert_space_before_closing_angle_bracket_in_type_arguments = false;
		this.insert_space_before_closing_angle_bracket_in_type_parameters = false;
		this.insert_space_before_closing_brace_in_array_initializer = false;
		this.insert_space_before_closing_bracket_in_array_allocation_expression = false;
		this.insert_space_before_closing_bracket_in_array_reference = false;
		this.insert_space_before_closing_paren_in_annotation = false;
		this.insert_space_before_closing_paren_in_cast = false;
		this.insert_space_before_closing_paren_in_catch = false;
		this.insert_space_before_closing_paren_in_constructor_declaration = false;
		this.insert_space_before_closing_paren_in_enum_constant = false;
		this.insert_space_before_closing_paren_in_for = false;
		this.insert_space_before_closing_paren_in_if = false;
		this.insert_space_before_closing_paren_in_method_declaration = false;
		this.insert_space_before_closing_paren_in_method_invocation = false;
		this.insert_space_before_closing_paren_in_parenthesized_expression = false;
		this.insert_space_before_closing_paren_in_record_declaration = false;
		this.insert_space_before_closing_paren_in_switch = false;
		this.insert_space_before_closing_paren_in_synchronized = false;
		this.insert_space_before_closing_paren_in_try = false;
		this.insert_space_before_closing_paren_in_while = false;
		this.insert_space_before_colon_in_assert = true;
		this.insert_space_before_colon_in_case = true;
		this.insert_space_before_colon_in_conditional = true;
		this.insert_space_before_colon_in_default = true;
		this.insert_space_before_colon_in_for = true;
		this.insert_space_before_colon_in_labeled_statement = true;
		this.insert_space_before_comma_in_allocation_expression = false;
		this.insert_space_before_comma_in_array_initializer = false;
		this.insert_space_before_comma_in_constructor_declaration_parameters = false;
		this.insert_space_before_comma_in_constructor_declaration_throws = false;
		this.insert_space_before_comma_in_enum_constant_arguments = false;
		this.insert_space_before_comma_in_enum_declarations = false;
		this.insert_space_before_comma_in_explicit_constructor_call_arguments = false;
		this.insert_space_before_comma_in_for_increments = false;
		this.insert_space_before_comma_in_for_inits = false;
		this.insert_space_before_comma_in_method_invocation_arguments = false;
		this.insert_space_before_comma_in_method_declaration_parameters = false;
		this.insert_space_before_comma_in_method_declaration_throws = false;
		this.insert_space_before_comma_in_multiple_field_declarations = false;
		this.insert_space_before_comma_in_multiple_local_declarations = false;
		this.insert_space_before_comma_in_parameterized_type_reference = false;
		this.insert_space_before_comma_in_permitted_types = false;
		this.insert_space_before_comma_in_record_components = false;
		this.insert_space_before_comma_in_superinterfaces = false;
		this.insert_space_before_comma_in_switch_case_expressions = false;
		this.insert_space_before_comma_in_type_arguments = false;
		this.insert_space_before_comma_in_type_parameters = false;
		this.insert_space_before_ellipsis = false;
		this.insert_space_before_lambda_arrow = true;
		this.insert_space_before_parenthesized_expression_in_return = true;
		this.insert_space_before_parenthesized_expression_in_throw = true;
		this.insert_space_before_opening_angle_bracket_in_parameterized_type_reference = false;
		this.insert_space_before_opening_angle_bracket_in_type_arguments = false;
		this.insert_space_before_opening_angle_bracket_in_type_parameters = false;
		this.insert_space_before_opening_brace_in_annotation_type_declaration = true;
		this.insert_space_before_opening_brace_in_anonymous_type_declaration = true;
		this.insert_space_before_opening_brace_in_array_initializer = false;
		this.insert_space_before_opening_brace_in_block = true;
		this.insert_space_before_opening_brace_in_constructor_declaration = true;
		this.insert_space_before_opening_brace_in_enum_constant = true;
		this.insert_space_before_opening_brace_in_enum_declaration = true;
		this.insert_space_before_opening_brace_in_method_declaration = true;
		this.insert_space_before_opening_brace_in_record_constructor = true;
		this.insert_space_before_opening_brace_in_record_declaration = true;
		this.insert_space_before_opening_brace_in_switch = true;
		this.insert_space_before_opening_brace_in_type_declaration = true;
		this.insert_space_before_opening_bracket_in_array_allocation_expression = false;
		this.insert_space_before_opening_bracket_in_array_reference = false;
		this.insert_space_before_opening_bracket_in_array_type_reference = false;
		this.insert_space_before_opening_paren_in_annotation = false;
		this.insert_space_before_opening_paren_in_annotation_type_member_declaration = false;
		this.insert_space_before_opening_paren_in_catch = true;
		this.insert_space_before_opening_paren_in_constructor_declaration = false;
		this.insert_space_before_opening_paren_in_enum_constant = false;
		this.insert_space_before_opening_paren_in_for = true;
		this.insert_space_before_opening_paren_in_if = true;
		this.insert_space_before_opening_paren_in_method_invocation = false;
		this.insert_space_before_opening_paren_in_method_declaration = false;
		this.insert_space_before_opening_paren_in_record_declaration = false;
		this.insert_space_before_opening_paren_in_switch = true;
		this.insert_space_before_opening_paren_in_synchronized = true;
		this.insert_space_before_opening_paren_in_try = true;
		this.insert_space_before_opening_paren_in_parenthesized_expression = false;
		this.insert_space_before_opening_paren_in_while = true;
		this.insert_space_before_postfix_operator = false;
		this.insert_space_before_prefix_operator = false;
		this.insert_space_before_question_in_conditional = true;
		this.insert_space_before_question_in_wilcard = false;
		this.insert_space_before_semicolon = false;
		this.insert_space_before_semicolon_in_for = false;
		this.insert_space_before_semicolon_in_try_resources = false;
		this.insert_space_before_unary_operator = false;
		this.insert_space_between_brackets_in_array_type_reference = false;
		this.insert_space_between_empty_braces_in_array_initializer = false;
		this.insert_space_between_empty_brackets_in_array_allocation_expression = false;
		this.insert_space_between_empty_parens_in_annotation_type_member_declaration = false;
		this.insert_space_between_empty_parens_in_constructor_declaration = false;
		this.insert_space_between_empty_parens_in_enum_constant = false;
		this.insert_space_between_empty_parens_in_method_declaration = false;
		this.insert_space_between_empty_parens_in_method_invocation = false;
		this.compact_else_if = true;
		this.keep_guardian_clause_on_one_line = false;
		this.keep_else_statement_on_same_line = false;
		this.keep_empty_array_initializer_on_one_line = false;
		this.keep_simple_if_on_one_line = false;
		this.keep_then_statement_on_same_line = false;
		this.keep_simple_for_body_on_same_line = false;
		this.keep_simple_while_body_on_same_line = false;
		this.keep_simple_do_while_body_on_same_line = false;
		this.never_indent_block_comments_on_first_column = false;
		this.never_indent_line_comments_on_first_column = false;
		this.number_of_empty_lines_to_preserve = 1;
		this.join_lines_in_comments = true;
		this.join_wrapped_lines = true;
		this.put_empty_statement_on_new_line = false;
		this.tab_size = 4;
		this.page_width = 120;
		this.tab_char = TAB; // see https://bugs.eclipse.org/bugs/show_bug.cgi?id=49081
		this.use_tabs_only_for_leading_indentations = false;
		this.text_block_indentation = Alignment.M_INDENT_DEFAULT;
		this.wrap_before_multiplicative_operator = true;
		this.wrap_before_additive_operator = true;
		this.wrap_before_assertion_message_operator = true;
		this.wrap_before_string_concatenation = true;
		this.wrap_before_shift_operator = true;
		this.wrap_before_relational_operator = true;
		this.wrap_before_bitwise_operator = true;
		this.wrap_before_logical_operator = true;
		this.wrap_before_or_operator_multicatch = true;
		this.wrap_before_conditional_operator = true;
		this.wrap_before_assignment_operator = false;
		this.wrap_before_switch_case_arrow_operator = false;
		this.use_tags = true;
		this.disabling_tag = DEFAULT_DISABLING_TAG;
		this.enabling_tag = DEFAULT_ENABLING_TAG;
		this.wrap_outer_expressions_when_nested = true;
	}

	public void setEclipseDefaultSettings() {
		setJavaConventionsSettings();
		this.tab_char = TAB;
		this.tab_size = 4;
	}

	public void setJavaConventionsSettings() {
		this.alignment_for_annotations_on_type = Alignment.M_FORCE | Alignment.M_ONE_PER_LINE_SPLIT;
		this.alignment_for_type_annotations = Alignment.M_NO_ALIGNMENT;
		this.alignment_for_annotations_on_enum_constant = Alignment.M_FORCE | Alignment.M_ONE_PER_LINE_SPLIT;
		this.alignment_for_annotations_on_field = Alignment.M_FORCE | Alignment.M_ONE_PER_LINE_SPLIT;
		this.alignment_for_annotations_on_method = Alignment.M_FORCE | Alignment.M_ONE_PER_LINE_SPLIT;
		this.alignment_for_annotations_on_package = Alignment.M_FORCE | Alignment.M_ONE_PER_LINE_SPLIT;
		this.alignment_for_annotations_on_parameter = Alignment.M_NO_ALIGNMENT;
		this.alignment_for_annotations_on_local_variable = Alignment.M_FORCE | Alignment.M_ONE_PER_LINE_SPLIT;
		this.alignment_for_arguments_in_allocation_expression = Alignment.M_COMPACT_SPLIT;
		this.alignment_for_arguments_in_annotation = Alignment.M_NO_ALIGNMENT;
		this.alignment_for_arguments_in_enum_constant = Alignment.M_COMPACT_SPLIT;
		this.alignment_for_arguments_in_explicit_constructor_call = Alignment.M_COMPACT_SPLIT;
		this.alignment_for_arguments_in_method_invocation = Alignment.M_COMPACT_SPLIT;
		this.alignment_for_arguments_in_qualified_allocation_expression = Alignment.M_COMPACT_SPLIT;
		this.alignment_for_assertion_message = Alignment.M_COMPACT_SPLIT;
		this.alignment_for_assignment = Alignment.M_NO_ALIGNMENT;
		this.alignment_for_multiplicative_operator = Alignment.M_COMPACT_SPLIT;
		this.alignment_for_additive_operator = Alignment.M_COMPACT_SPLIT;
		this.alignment_for_string_concatenation = Alignment.M_COMPACT_SPLIT;
		this.alignment_for_shift_operator = Alignment.M_NO_ALIGNMENT;
		this.alignment_for_relational_operator = Alignment.M_NO_ALIGNMENT;
		this.alignment_for_bitwise_operator = Alignment.M_COMPACT_SPLIT;
		this.alignment_for_logical_operator = Alignment.M_COMPACT_SPLIT;
		this.alignment_for_compact_if = Alignment.M_COMPACT_SPLIT;
		this.alignment_for_compact_loop = Alignment.M_COMPACT_SPLIT;
		this.alignment_for_conditional_expression = Alignment.M_NEXT_PER_LINE_SPLIT;
		this.alignment_for_conditional_expression_chain = Alignment.M_NO_ALIGNMENT;
		this.alignment_for_enum_constants = Alignment.M_COMPACT_SPLIT;
		this.alignment_for_expressions_in_array_initializer = Alignment.M_COMPACT_SPLIT;
		this.alignment_for_expressions_in_for_loop_header = Alignment.M_NO_ALIGNMENT;
		this.alignment_for_expressions_in_switch_case_with_arrow = Alignment.M_COMPACT_SPLIT;
		this.alignment_for_expressions_in_switch_case_with_colon = Alignment.M_COMPACT_SPLIT;
		this.alignment_for_method_declaration = Alignment.M_NO_ALIGNMENT;
		this.alignment_for_module_statements = Alignment.M_COMPACT_SPLIT;
		this.alignment_for_multiple_fields = Alignment.M_COMPACT_SPLIT;
		this.alignment_for_parameterized_type_references = Alignment.M_NO_ALIGNMENT;
		this.alignment_for_parameters_in_constructor_declaration = Alignment.M_COMPACT_SPLIT;
		this.alignment_for_parameters_in_method_declaration = Alignment.M_COMPACT_SPLIT;
		this.alignment_for_record_components = Alignment.M_COMPACT_SPLIT;
		this.alignment_for_resources_in_try = Alignment.M_NEXT_PER_LINE_SPLIT;
		this.alignment_for_selector_in_method_invocation = Alignment.M_COMPACT_SPLIT;
		this.alignment_for_superclass_in_type_declaration = Alignment.M_COMPACT_SPLIT;
		this.alignment_for_superinterfaces_in_enum_declaration = Alignment.M_COMPACT_SPLIT;
		this.alignment_for_superinterfaces_in_record_declaration = Alignment.M_COMPACT_SPLIT;
		this.alignment_for_superinterfaces_in_type_declaration = Alignment.M_COMPACT_SPLIT;
		this.alignment_for_switch_case_with_arrow = Alignment.M_COMPACT_SPLIT | Alignment.M_INDENT_BY_ONE;
		this.alignment_for_throws_clause_in_constructor_declaration = Alignment.M_COMPACT_SPLIT;
		this.alignment_for_throws_clause_in_method_declaration = Alignment.M_COMPACT_SPLIT;
		this.alignment_for_type_arguments = Alignment.M_NO_ALIGNMENT;
		this.alignment_for_type_parameters = Alignment.M_NO_ALIGNMENT;
		this.alignment_for_union_type_in_multicatch = Alignment.M_COMPACT_SPLIT;
		this.align_selector_in_method_invocation_on_expression_first_line = true;
		this.align_type_members_on_columns = false;
		this.align_variable_declarations_on_columns = false;
		this.align_assignment_statements_on_columns = false;
		this.align_with_spaces = false;
		this.align_fields_grouping_blank_lines = Integer.MAX_VALUE;
		this.brace_position_for_annotation_type_declaration = DefaultCodeFormatterConstants.END_OF_LINE;
		this.brace_position_for_anonymous_type_declaration = DefaultCodeFormatterConstants.END_OF_LINE;
		this.brace_position_for_array_initializer = DefaultCodeFormatterConstants.END_OF_LINE;
		this.brace_position_for_block = DefaultCodeFormatterConstants.END_OF_LINE;
		this.brace_position_for_block_in_case = DefaultCodeFormatterConstants.END_OF_LINE;
		this.brace_position_for_constructor_declaration = DefaultCodeFormatterConstants.END_OF_LINE;
		this.brace_position_for_enum_constant = DefaultCodeFormatterConstants.END_OF_LINE;
		this.brace_position_for_enum_declaration = DefaultCodeFormatterConstants.END_OF_LINE;
		this.brace_position_for_lambda_body = DefaultCodeFormatterConstants.END_OF_LINE;
		this.brace_position_for_method_declaration = DefaultCodeFormatterConstants.END_OF_LINE;
		this.brace_position_for_type_declaration = DefaultCodeFormatterConstants.END_OF_LINE;
		this.brace_position_for_record_constructor = DefaultCodeFormatterConstants.END_OF_LINE;
		this.brace_position_for_record_declaration = DefaultCodeFormatterConstants.END_OF_LINE;
		this.brace_position_for_switch = DefaultCodeFormatterConstants.END_OF_LINE;
		this.parenthesis_positions_in_method_declaration = DefaultCodeFormatterConstants.COMMON_LINES;
		this.parenthesis_positions_in_method_invocation = DefaultCodeFormatterConstants.COMMON_LINES;
		this.parenthesis_positions_in_enum_constant_declaration = DefaultCodeFormatterConstants.COMMON_LINES;
		this.parenthesis_positions_in_record_declaration = DefaultCodeFormatterConstants.COMMON_LINES;
		this.parenthesis_positions_in_if_while_statement = DefaultCodeFormatterConstants.COMMON_LINES;
		this.parenthesis_positions_in_for_statement = DefaultCodeFormatterConstants.COMMON_LINES;
		this.parenthesis_positions_in_switch_statement = DefaultCodeFormatterConstants.COMMON_LINES;
		this.parenthesis_positions_in_try_clause = DefaultCodeFormatterConstants.COMMON_LINES;
		this.parenthesis_positions_in_catch_clause = DefaultCodeFormatterConstants.COMMON_LINES;
		this.parenthesis_positions_in_annotation = DefaultCodeFormatterConstants.COMMON_LINES;
		this.parenthesis_positions_in_lambda_declaration = DefaultCodeFormatterConstants.COMMON_LINES;
		this.comment_clear_blank_lines_in_block_comment = false;
		this.comment_clear_blank_lines_in_javadoc_comment = false;
		this.comment_format_block_comment = true;
		this.comment_format_javadoc_comment = true;
		this.comment_format_line_comment = true;
		this.comment_format_line_comment_starting_on_first_column = false;
		this.comment_format_header = false;
		this.comment_format_html = true;
		this.comment_format_source = true;
		this.comment_indent_parameter_description = false;
		this.comment_indent_tag_description = false;
		this.comment_indent_root_tags = false;
		this.comment_align_tags_names_descriptions = false;
		this.comment_align_tags_descriptions_grouped = true;
		this.comment_insert_empty_line_before_root_tags = true;
		this.comment_insert_empty_line_between_different_tags = false;
		this.comment_insert_new_line_for_parameter = false;
		this.comment_new_lines_at_block_boundaries = true;
		this.comment_new_lines_at_javadoc_boundaries = true;
		this.comment_line_length = 80;
		this.comment_count_line_length_from_starting_position = true;
		this.comment_preserve_white_space_between_code_and_line_comments= false;
		this.continuation_indentation = 2;
		this.continuation_indentation_for_array_initializer = 2;
		this.blank_lines_after_imports = 1;
		this.blank_lines_after_package = 1;
		this.blank_lines_before_field = 0;
		this.blank_lines_before_first_class_body_declaration = 0;
		this.blank_lines_after_last_class_body_declaration = 0;
		this.blank_lines_before_imports = 1;
		this.blank_lines_before_member_type = 1;
		this.blank_lines_before_abstract_method = 1;
		this.blank_lines_before_method = 1;
		this.blank_lines_before_new_chunk = 1;
		this.blank_lines_before_package = 0;
		this.blank_lines_between_import_groups = 1;
		this.blank_lines_between_type_declarations = 1;
		this.blank_lines_at_beginning_of_method_body = 0;
		this.blank_lines_at_end_of_method_body = 0;
		this.blank_lines_at_beginning_of_code_block = 0;
		this.blank_lines_at_end_of_code_block = 0;
		this.blank_lines_before_code_block = 0;
		this.blank_lines_after_code_block = 0;
		this.blank_lines_between_statement_groups_in_switch = 0;
		this.indent_statements_compare_to_block = true;
		this.indent_statements_compare_to_body = true;
		this.indent_body_declarations_compare_to_annotation_declaration_header = true;
		this.indent_body_declarations_compare_to_enum_constant_header = true;
		this.indent_body_declarations_compare_to_enum_declaration_header = true;
		this.indent_body_declarations_compare_to_record_header = true;
		this.indent_body_declarations_compare_to_type_header = true;
		this.indent_breaks_compare_to_cases = true;
		this.indent_empty_lines = false;
		this.indent_switchstatements_compare_to_cases = true;
		this.indent_switchstatements_compare_to_switch = false;
		this.indentation_size = 4;
		this.insert_new_line_after_annotation_on_type = true;
		this.insert_new_line_after_type_annotation = false;
		this.insert_new_line_after_annotation_on_enum_constant = true;
		this.insert_new_line_after_annotation_on_field = true;
		this.insert_new_line_after_annotation_on_method = true;
		this.insert_new_line_after_annotation_on_package = true;
		this.insert_new_line_after_annotation_on_parameter = false;
		this.insert_new_line_after_annotation_on_local_variable = true;
		this.insert_new_line_after_opening_brace_in_array_initializer = false;
		this.insert_new_line_at_end_of_file_if_missing = false;
		this.insert_new_line_before_catch_in_try_statement = false;
		this.insert_new_line_before_closing_brace_in_array_initializer = false;
		this.insert_new_line_before_else_in_if_statement = false;
		this.insert_new_line_before_finally_in_try_statement = false;
		this.insert_new_line_before_while_in_do_statement = false;
		this.keep_annotation_declaration_on_one_line = DefaultCodeFormatterConstants.ONE_LINE_NEVER;
		this.keep_anonymous_type_declaration_on_one_line = DefaultCodeFormatterConstants.ONE_LINE_NEVER;
		this.keep_if_then_body_block_on_one_line = DefaultCodeFormatterConstants.ONE_LINE_NEVER;
		this.keep_switch_body_block_on_one_line = DefaultCodeFormatterConstants.ONE_LINE_NEVER;
		this.keep_switch_case_with_arrow_on_one_line = DefaultCodeFormatterConstants.ONE_LINE_NEVER;
		this.keep_lambda_body_block_on_one_line = DefaultCodeFormatterConstants.ONE_LINE_NEVER;
		this.keep_loop_body_block_on_one_line = DefaultCodeFormatterConstants.ONE_LINE_NEVER;
		this.keep_code_block_on_one_line = DefaultCodeFormatterConstants.ONE_LINE_NEVER;
		this.keep_enum_constant_declaration_on_one_line = DefaultCodeFormatterConstants.ONE_LINE_NEVER;
		this.keep_enum_declaration_on_one_line = DefaultCodeFormatterConstants.ONE_LINE_NEVER;
		this.keep_method_body_on_one_line = DefaultCodeFormatterConstants.ONE_LINE_NEVER;
		this.keep_type_declaration_on_one_line = DefaultCodeFormatterConstants.ONE_LINE_NEVER;
		this.keep_record_declaration_on_one_line = DefaultCodeFormatterConstants.ONE_LINE_NEVER;
		this.keep_record_constructor_on_one_line = DefaultCodeFormatterConstants.ONE_LINE_NEVER;
		this.keep_simple_getter_setter_on_one_line = false;
		this.insert_space_after_and_in_type_parameter = true;
		this.insert_space_after_arrow_in_switch_case = true;
		this.insert_space_after_arrow_in_switch_default = true;
		this.insert_space_after_assignment_operator = true;
		this.insert_space_after_at_in_annotation = false;
		this.insert_space_after_at_in_annotation_type_declaration = false;
		this.insert_space_after_multiplicative_operator = true;
		this.insert_space_after_additive_operator = true;
		this.insert_space_after_string_concatenation = true;
		this.insert_space_after_shift_operator = true;
		this.insert_space_after_relational_operator = true;
		this.insert_space_after_bitwise_operator = true;
		this.insert_space_after_logical_operator = true;
		this.insert_space_after_closing_angle_bracket_in_type_arguments = false;
		this.insert_space_after_closing_angle_bracket_in_type_parameters = true;
		this.insert_space_after_closing_paren_in_cast = true;
		this.insert_space_after_closing_brace_in_block = true;
		this.insert_space_after_colon_in_assert = true;
		this.insert_space_after_colon_in_case = true;
		this.insert_space_after_colon_in_conditional = true;
		this.insert_space_after_colon_in_for = true;
		this.insert_space_after_colon_in_labeled_statement = true;
		this.insert_space_after_comma_in_allocation_expression = true;
		this.insert_space_after_comma_in_annotation = true;
		this.insert_space_after_comma_in_array_initializer = true;
		this.insert_space_after_comma_in_constructor_declaration_parameters = true;
		this.insert_space_after_comma_in_constructor_declaration_throws = true;
		this.insert_space_after_comma_in_enum_constant_arguments = true;
		this.insert_space_after_comma_in_enum_declarations = true;
		this.insert_space_after_comma_in_explicit_constructor_call_arguments = true;
		this.insert_space_after_comma_in_for_increments = true;
		this.insert_space_after_comma_in_for_inits = true;
		this.insert_space_after_comma_in_method_invocation_arguments = true;
		this.insert_space_after_comma_in_method_declaration_parameters = true;
		this.insert_space_after_comma_in_method_declaration_throws = true;
		this.insert_space_after_comma_in_multiple_field_declarations = true;
		this.insert_space_after_comma_in_multiple_local_declarations = true;
		this.insert_space_after_comma_in_parameterized_type_reference = true;
		this.insert_space_after_comma_in_permitted_types = true;
		this.insert_space_after_comma_in_record_components = true;
		this.insert_space_after_comma_in_superinterfaces = true;
		this.insert_space_after_comma_in_switch_case_expressions = true;
		this.insert_space_after_comma_in_type_arguments = true;
		this.insert_space_after_comma_in_type_parameters = true;
		this.insert_space_after_ellipsis = true;
		this.insert_space_after_lambda_arrow = true;
		this.insert_space_after_not_operator = false;
		this.insert_space_after_opening_angle_bracket_in_parameterized_type_reference = false;
		this.insert_space_after_opening_angle_bracket_in_type_arguments = false;
		this.insert_space_after_opening_angle_bracket_in_type_parameters = false;
		this.insert_space_after_opening_bracket_in_array_allocation_expression = false;
		this.insert_space_after_opening_bracket_in_array_reference = false;
		this.insert_space_after_opening_brace_in_array_initializer = true;
		this.insert_space_after_opening_paren_in_annotation = false;
		this.insert_space_after_opening_paren_in_cast = false;
		this.insert_space_after_opening_paren_in_catch = false;
		this.insert_space_after_opening_paren_in_constructor_declaration = false;
		this.insert_space_after_opening_paren_in_enum_constant = false;
		this.insert_space_after_opening_paren_in_for = false;
		this.insert_space_after_opening_paren_in_if = false;
		this.insert_space_after_opening_paren_in_method_declaration = false;
		this.insert_space_after_opening_paren_in_method_invocation = false;
		this.insert_space_after_opening_paren_in_parenthesized_expression = false;
		this.insert_space_after_opening_paren_in_record_declaration = false;
		this.insert_space_after_opening_paren_in_switch = false;
		this.insert_space_after_opening_paren_in_synchronized = false;
		this.insert_space_after_opening_paren_in_try = false;
		this.insert_space_after_opening_paren_in_while = false;
		this.insert_space_after_postfix_operator = false;
		this.insert_space_after_prefix_operator = false;
		this.insert_space_after_question_in_conditional = true;
		this.insert_space_after_question_in_wilcard = false;
		this.insert_space_after_semicolon_in_for = true;
		this.insert_space_after_semicolon_in_try_resources = true;
		this.insert_space_after_unary_operator = false;
		this.insert_space_before_and_in_type_parameter = true;
		this.insert_space_before_arrow_in_switch_case = true;
		this.insert_space_before_arrow_in_switch_default = true;
		this.insert_space_before_at_in_annotation_type_declaration = true;
		this.insert_space_before_assignment_operator = true;
		this.insert_space_before_multiplicative_operator = true;
		this.insert_space_before_additive_operator = true;
		this.insert_space_before_string_concatenation = true;
		this.insert_space_before_shift_operator = true;
		this.insert_space_before_relational_operator = true;
		this.insert_space_before_bitwise_operator = true;
		this.insert_space_before_logical_operator = true;
		this.insert_space_before_closing_angle_bracket_in_parameterized_type_reference = false;
		this.insert_space_before_closing_angle_bracket_in_type_arguments = false;
		this.insert_space_before_closing_angle_bracket_in_type_parameters = false;
		this.insert_space_before_closing_brace_in_array_initializer = true;
		this.insert_space_before_closing_bracket_in_array_allocation_expression = false;
		this.insert_space_before_closing_bracket_in_array_reference = false;
		this.insert_space_before_closing_paren_in_annotation = false;
		this.insert_space_before_closing_paren_in_cast = false;
		this.insert_space_before_closing_paren_in_catch = false;
		this.insert_space_before_closing_paren_in_constructor_declaration = false;
		this.insert_space_before_closing_paren_in_enum_constant = false;
		this.insert_space_before_closing_paren_in_for = false;
		this.insert_space_before_closing_paren_in_if = false;
		this.insert_space_before_closing_paren_in_method_declaration = false;
		this.insert_space_before_closing_paren_in_method_invocation = false;
		this.insert_space_before_closing_paren_in_parenthesized_expression = false;
		this.insert_space_before_closing_paren_in_record_declaration = false;
		this.insert_space_before_closing_paren_in_switch = false;
		this.insert_space_before_closing_paren_in_synchronized = false;
		this.insert_space_before_closing_paren_in_try = false;
		this.insert_space_before_closing_paren_in_while = false;
		this.insert_space_before_colon_in_assert = true;
		this.insert_space_before_colon_in_case = false;
		this.insert_space_before_colon_in_conditional = true;
		this.insert_space_before_colon_in_default = false;
		this.insert_space_before_colon_in_for = true;
		this.insert_space_before_colon_in_labeled_statement = false;
		this.insert_space_before_comma_in_allocation_expression = false;
		this.insert_space_before_comma_in_array_initializer = false;
		this.insert_space_before_comma_in_constructor_declaration_parameters = false;
		this.insert_space_before_comma_in_constructor_declaration_throws = false;
		this.insert_space_before_comma_in_enum_constant_arguments = false;
		this.insert_space_before_comma_in_enum_declarations = false;
		this.insert_space_before_comma_in_explicit_constructor_call_arguments = false;
		this.insert_space_before_comma_in_for_increments = false;
		this.insert_space_before_comma_in_for_inits = false;
		this.insert_space_before_comma_in_method_invocation_arguments = false;
		this.insert_space_before_comma_in_method_declaration_parameters = false;
		this.insert_space_before_comma_in_method_declaration_throws = false;
		this.insert_space_before_comma_in_multiple_field_declarations = false;
		this.insert_space_before_comma_in_multiple_local_declarations = false;
		this.insert_space_before_comma_in_parameterized_type_reference = false;
		this.insert_space_before_comma_in_permitted_types = false;
		this.insert_space_before_comma_in_record_components = false;
		this.insert_space_before_comma_in_superinterfaces = false;
		this.insert_space_before_comma_in_switch_case_expressions = false;
		this.insert_space_before_comma_in_type_arguments = false;
		this.insert_space_before_comma_in_type_parameters = false;
		this.insert_space_before_ellipsis = false;
		this.insert_space_before_lambda_arrow = true;
		this.insert_space_before_parenthesized_expression_in_return = true;
		this.insert_space_before_parenthesized_expression_in_throw = true;
		this.insert_space_before_opening_angle_bracket_in_parameterized_type_reference = false;
		this.insert_space_before_opening_angle_bracket_in_type_arguments = false;
		this.insert_space_before_opening_angle_bracket_in_type_parameters = false;
		this.insert_space_before_opening_brace_in_annotation_type_declaration = true;
		this.insert_space_before_opening_brace_in_anonymous_type_declaration = true;
		this.insert_space_before_opening_brace_in_array_initializer = true;
		this.insert_space_before_opening_brace_in_block = true;
		this.insert_space_before_opening_brace_in_constructor_declaration = true;
		this.insert_space_before_opening_brace_in_enum_constant = true;
		this.insert_space_before_opening_brace_in_enum_declaration = true;
		this.insert_space_before_opening_brace_in_method_declaration = true;
		this.insert_space_before_opening_brace_in_record_constructor = true;
		this.insert_space_before_opening_brace_in_record_declaration = true;
		this.insert_space_before_opening_brace_in_switch = true;
		this.insert_space_before_opening_brace_in_type_declaration = true;
		this.insert_space_before_opening_bracket_in_array_allocation_expression = false;
		this.insert_space_before_opening_bracket_in_array_reference = false;
		this.insert_space_before_opening_bracket_in_array_type_reference = false;
		this.insert_space_before_opening_paren_in_annotation = false;
		this.insert_space_before_opening_paren_in_annotation_type_member_declaration = false;
		this.insert_space_before_opening_paren_in_catch = true;
		this.insert_space_before_opening_paren_in_constructor_declaration = false;
		this.insert_space_before_opening_paren_in_enum_constant = false;
		this.insert_space_before_opening_paren_in_for = true;
		this.insert_space_before_opening_paren_in_if = true;
		this.insert_space_before_opening_paren_in_method_invocation = false;
		this.insert_space_before_opening_paren_in_method_declaration = false;
		this.insert_space_before_opening_paren_in_record_declaration = false;
		this.insert_space_before_opening_paren_in_switch = true;
		this.insert_space_before_opening_paren_in_synchronized = true;
		this.insert_space_before_opening_paren_in_try = true;
		this.insert_space_before_opening_paren_in_parenthesized_expression = false;
		this.insert_space_before_opening_paren_in_while = true;
		this.insert_space_before_postfix_operator = false;
		this.insert_space_before_prefix_operator = false;
		this.insert_space_before_question_in_conditional = true;
		this.insert_space_before_question_in_wilcard = false;
		this.insert_space_before_semicolon = false;
		this.insert_space_before_semicolon_in_for = false;
		this.insert_space_before_semicolon_in_try_resources = false;
		this.insert_space_before_unary_operator = false;
		this.insert_space_between_brackets_in_array_type_reference = false;
		this.insert_space_between_empty_braces_in_array_initializer = false;
		this.insert_space_between_empty_brackets_in_array_allocation_expression = false;
		this.insert_space_between_empty_parens_in_annotation_type_member_declaration = false;
		this.insert_space_between_empty_parens_in_constructor_declaration = false;
		this.insert_space_between_empty_parens_in_enum_constant = false;
		this.insert_space_between_empty_parens_in_method_declaration = false;
		this.insert_space_between_empty_parens_in_method_invocation = false;
		this.compact_else_if = true;
		this.keep_guardian_clause_on_one_line = false;
		this.keep_else_statement_on_same_line = false;
		this.keep_empty_array_initializer_on_one_line = false;
		this.keep_simple_if_on_one_line = false;
		this.keep_then_statement_on_same_line = false;
		this.keep_simple_for_body_on_same_line = false;
		this.keep_simple_while_body_on_same_line = false;
		this.keep_simple_do_while_body_on_same_line = false;
		this.never_indent_block_comments_on_first_column = false;
		this.never_indent_line_comments_on_first_column = false;
		this.number_of_empty_lines_to_preserve = 1;
		this.join_lines_in_comments = true;
		this.join_wrapped_lines = true;
		this.put_empty_statement_on_new_line = true;
		this.tab_size = 8;
		this.page_width = 120;
		this.tab_char = MIXED;
		this.use_tabs_only_for_leading_indentations = false;
		this.text_block_indentation = Alignment.M_INDENT_DEFAULT;
		this.wrap_before_multiplicative_operator = true;
		this.wrap_before_additive_operator = true;
		this.wrap_before_assertion_message_operator = true;
		this.wrap_before_string_concatenation = true;
		this.wrap_before_shift_operator = true;
		this.wrap_before_relational_operator = true;
		this.wrap_before_bitwise_operator = true;
		this.wrap_before_logical_operator = true;
		this.wrap_before_or_operator_multicatch = true;
		this.wrap_before_conditional_operator = true;
		this.wrap_before_assignment_operator = false;
		this.wrap_before_switch_case_arrow_operator = false;
		this.use_tags = true;
		this.disabling_tag = DEFAULT_DISABLING_TAG;
		this.enabling_tag = DEFAULT_ENABLING_TAG;
		this.wrap_outer_expressions_when_nested = true;
	}
}
