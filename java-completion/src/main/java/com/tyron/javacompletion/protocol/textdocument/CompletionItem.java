package com.tyron.javacompletion.protocol.textdocument;

import com.google.gson.JsonElement;
import com.tyron.javacompletion.protocol.TextEdit;

import androidx.annotation.Nullable;
import java.net.URI;
import java.util.List;

public class CompletionItem {
    /**
     * The label of this completion item. By default also the text that is inserted when selecting
     * this completion.
     */
    public String label;

    /** The kind of this completion item. Based of the kind an icon is chosen by the editor. */
    @Nullable
    public CompletionItemKind kind;

    /**
     * A human-readable string with additional information about this item, like type or symbol
     * information.
     */
    @Nullable public String detail;

    /** A human-readable string that represents a doc-comment. */
    @Nullable public String documentation;

    /**
     * A string that shoud be used when comparing this item with other items. When {@code null} the
     * label is used.
     */
    @Nullable public String sortText;

    /**
     * A string that should be used when filtering a set of completion items. When {@code null} the
     * label is used.
     */
    @Nullable public String filterText;

    /**
     * A string that should be inserted a document when selecting this completion. When {@code null}
     * the label is used.
     */
    @Nullable public String insertText;

    /**
     * The format of the insert text. The format applies to both the {@link #insertText} property and
     * the {@link #newText} property of a provided {@link #textEdit}.
     */
    @Nullable public InsertTextFormat insertTextFormat;

    /**
     * An edit which is applied to a document when selecting this completion. When an edit is provided
     * the value of {@link #insertText} is ignored.
     *
     * <p><b>Note:</b> The range of the edit must be a single line range and it must contain the
     * position at which completion has been requested.
     */
    @Nullable public TextEdit textEdit;

    /**
     * An optional array of additional text edits that are applied when selecting this completion.
     * Edits must not overlap with the main edit nor with themselves.
     */
    @Nullable public List<TextEdit> additionalTextEdits;

    /**
     * An optional command that is executed <b>after</b> inserting this completion. <b>Note</b> that
     * additional modifications to the current document should be described with the
     * additionalTextEdits-property.
     */
  //  @Nullable public Command command;

    /**
     * An data entry field that is preserved on a completion item between a completion and a
     * completion resolve request.
     */
    @Nullable public List<ResolveData> data;

    /**
     * Defines whether the insert text in a completion item should be interpreted as plain text or a
     * snippet.
     */
    public enum InsertTextFormat {
        UNKNOWN,

        /** The primary text to be inserted is treated as a plain string. */
        PLAINTEXT,

        /**
         * The primary text to be inserted is treated as a snippet.
         *
         * <p>A snippet can define tab stops and placeholders with {@code $1}, {@code $2} and {@code
         * ${3:foo}}. {@code $0} defines the final tab stop, it defaults to the end of the snippet.
         * Placeholders with equal identifiers are linked, that is typing in one will update others too.
         *
         * <p>See also:
         * https://github.com/Microsoft/vscode/blob/master/src/vs/editor/contrib/snippet/common/snippet.md
         */
        SNIPPET,
    }

    /** The kind of a completion entry. */
    public enum CompletionItemKind {
        UNKNOWN,
        TEXT,
        METHOD,
        FUNCTION,
        CONSTRUCTOR,
        FIELD,
        VARIABLE,
        CLASS,
        INTERFACE,
        MODULE,
        PROPERTY,
        UNIT,
        VALUE,
        ENUM,
        KEYWORD,
        SNIPPET,
        COLOR,
        FILE,
        REFERENCE,
    }

    public static class ResolveData {
        /** The action for resolving the completion item. */
        public ResolveAction action;
        /** Action-specific data. */
        public JsonElement params;
    }

    /** Actions for resolving a completion item. */
    public enum ResolveAction {
        /** Fill additionalTextEdits with adding import statements. */
        ADD_IMPORT_TEXT_EDIT,
        /**
         * Convert the documentation from JavaDoc format to Markdown format.
         *
         * <p>We defer the conversion from completion time to resolve time to reduce unnecessary cost
         * and latency.
         */
        FORMAT_JAVADOC,
    }

    /** Marker interface for the actual type of {@link ResolveData#data}. */
    public interface ResolveActionParams {}

    /** Resolve data for ADD_IMPORT_TEXT_EDIT action. */
    public static class ResolveAddImportTextEditsParams implements ResolveActionParams {
        /** The text document's URI. */
        public URI uri;
        /** The full name of the class to be imported. */
        public String classFullName;
    }

    /** Resolve data for FORMAT_JAVADOC action. */
    public static class ResolveFormatJavadocParams implements ResolveActionParams {
        /** The javadoc to be converted. */
        public String javadoc;

        public ResolveFormatJavadocParams(String javadoc) {
            this.javadoc = javadoc;
        }
    }
}